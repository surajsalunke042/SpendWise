package com.surajdev.spendwise.core.domain.usecase

import com.surajdev.spendwise.core.common.IdGenerator
import com.surajdev.spendwise.core.common.Outcome
import com.surajdev.spendwise.core.common.TimeZoneProvider
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.DailyReport
import com.surajdev.spendwise.core.domain.model.DaySection
import com.surajdev.spendwise.core.domain.model.DefaultCategories
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.ExpenseFilter
import com.surajdev.spendwise.core.domain.model.ExpenseItem
import com.surajdev.spendwise.core.domain.model.ExpenseSource
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.model.sum
import com.surajdev.spendwise.core.domain.report.DailyReportCalculator
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.ExpenseRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.time.DayMath
import com.surajdev.spendwise.core.domain.time.DayMath.minusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** What the user entered on the Add/Edit screen. */
data class ExpenseDraft(
    val id: String? = null,
    val amount: Money?,
    val categoryId: String?,
    val note: String,
    val occurredAt: LocalDateTime,
    val paymentMethod: PaymentMethod?,
)

enum class ExpenseValidationError {
    AMOUNT_MISSING,
    AMOUNT_NOT_POSITIVE,
    AMOUNT_TOO_LARGE,
    CATEGORY_MISSING,
    NOTE_TOO_LONG,
    DATE_IN_FUTURE,
    NOT_FOUND,
}

/** Validates a draft and saves it as a new or updated expense. */
class SaveExpenseUseCase(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val timeZones: TimeZoneProvider,
) {
    suspend operator fun invoke(draft: ExpenseDraft): Outcome<Expense, ExpenseValidationError> {
        val amount = draft.amount ?: return Outcome.Failure(ExpenseValidationError.AMOUNT_MISSING)
        if (amount.minorUnits <= 0) return Outcome.Failure(ExpenseValidationError.AMOUNT_NOT_POSITIVE)
        if (amount.minorUnits > MAX_AMOUNT_MINOR) return Outcome.Failure(ExpenseValidationError.AMOUNT_TOO_LARGE)
        val categoryId = draft.categoryId ?: return Outcome.Failure(ExpenseValidationError.CATEGORY_MISSING)
        if (categories.getCategory(categoryId) == null) {
            return Outcome.Failure(ExpenseValidationError.CATEGORY_MISSING)
        }
        val note = draft.note.trim()
        if (note.length > MAX_NOTE_LENGTH) return Outcome.Failure(ExpenseValidationError.NOTE_TOO_LONG)

        val now = clock.now()
        val existing = draft.id?.let { expenses.getExpense(it) ?: return Outcome.Failure(ExpenseValidationError.NOT_FOUND) }
        // Editing keeps the zone the expense was logged in; new expenses use the device's zone.
        val zone = existing?.timeZone ?: timeZones.current()
        val occurredAt = draft.occurredAt.toInstant(zone)
        if (occurredAt > now + 1.days) return Outcome.Failure(ExpenseValidationError.DATE_IN_FUTURE)

        val expense = Expense(
            id = existing?.id ?: idGenerator.newId(),
            amount = amount,
            categoryId = categoryId,
            note = note,
            occurredAt = occurredAt,
            timeZoneId = existing?.timeZoneId ?: zone.id,
            paymentMethod = draft.paymentMethod,
            source = existing?.source ?: ExpenseSource.MANUAL,
            externalRef = existing?.externalRef,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        expenses.save(expense)
        settings.rememberLastUsed(categoryId, draft.paymentMethod)
        return Outcome.Success(expense)
    }

    companion object {
        const val MAX_NOTE_LENGTH = 200
        /** ₹1 crore in paise; guards against typos like an extra five zeros. */
        const val MAX_AMOUNT_MINOR = 1_00_00_000_00L
    }
}

/** Expenses matching a filter, grouped by local day (newest first). */
class ObserveExpenseSectionsUseCase(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val timeZones: TimeZoneProvider,
) {
    operator fun invoke(filter: ExpenseFilter): Flow<List<DaySection>> {
        val window = filter.dateRange?.let { DayMath.queryWindow(it.start, it.endInclusive, timeZones.current()) }
        return combine(
            expenses.observeExpenses(window, filter.query.trim(), filter.categoryIds),
            categories.observeCategories(includeArchived = true),
        ) { list, categoryList ->
            val byId = categoryList.associateBy { it.id }
            list
                .filter { filter.dateRange == null || it.localDate in filter.dateRange }
                .groupBy { it.localDate }
                .entries
                .sortedByDescending { it.key }
                .map { (date, dayExpenses) ->
                    val sorted = dayExpenses.sortedByDescending { it.occurredAt }
                    DaySection(
                        date = date,
                        total = sorted.map { it.amount }.sum(),
                        items = sorted.map { ExpenseItem(it, byId.resolve(it.categoryId)) },
                    )
                }
        }
    }
}

/** Live daily report for a date; updates as expenses change. */
class ObserveDailyReportUseCase(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val timeZones: TimeZoneProvider,
) {
    operator fun invoke(date: LocalDate): Flow<DailyReport> {
        val zone = timeZones.current()
        val window = DayMath.queryWindow(date.minusDays(DailyReportCalculator.AVERAGE_WINDOW_DAYS), date, zone)
        return combine(
            expenses.observeExpenses(window),
            categories.observeCategories(includeArchived = true),
            expenses.observeEarliestExpenseTime().map { it?.toLocalDateTime(zone)?.date }.distinctUntilChanged(),
        ) { list, categoryList, earliest ->
            DailyReportCalculator.calculate(
                date = date,
                expenses = list,
                categories = categoryList.associateBy { it.id },
                earliestExpenseDate = earliest,
            )
        }
    }
}

/** Today's total for the Home header. */
class ObserveDayTotalUseCase(
    private val expenses: ExpenseRepository,
    private val timeZones: TimeZoneProvider,
) {
    operator fun invoke(date: LocalDate): Flow<Money> {
        val zone: TimeZone = timeZones.current()
        return expenses.observeExpenses(DayMath.queryWindow(date, date, zone))
            .map { list -> list.filter { it.localDate == date }.map { it.amount }.sum() }
            .distinctUntilChanged()
    }
}

internal fun Map<String, Category>.resolve(id: String): Category =
    this[id] ?: this[DefaultCategories.OTHER_ID] ?: DefaultCategories.all.last()
