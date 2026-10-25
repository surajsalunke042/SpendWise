package com.surajdev.spendwise.core.domain

import app.cash.turbine.test
import com.surajdev.spendwise.core.common.Outcome
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.DateRange
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.ExpenseFilter
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.usecase.CategoryDraft
import com.surajdev.spendwise.core.domain.usecase.CategoryValidationError
import com.surajdev.spendwise.core.domain.usecase.ExpenseDraft
import com.surajdev.spendwise.core.domain.usecase.ExpenseValidationError
import com.surajdev.spendwise.core.domain.usecase.ObserveDailyReportUseCase
import com.surajdev.spendwise.core.domain.usecase.ObserveExpenseSectionsUseCase
import com.surajdev.spendwise.core.domain.usecase.SaveCategoryUseCase
import com.surajdev.spendwise.core.domain.usecase.SaveExpenseUseCase
import com.surajdev.spendwise.testing.FakeCategoryRepository
import com.surajdev.spendwise.testing.FakeExpenseRepository
import com.surajdev.spendwise.testing.FakeSettingsRepository
import com.surajdev.spendwise.testing.FixedClock
import com.surajdev.spendwise.testing.FixedTimeZone
import com.surajdev.spendwise.testing.IST
import com.surajdev.spendwise.testing.SequentialIds
import com.surajdev.spendwise.testing.at
import com.surajdev.spendwise.testing.expense
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SaveExpenseUseCaseTest {
    private val expenses = FakeExpenseRepository()
    private val categories = FakeCategoryRepository()
    private val settings = FakeSettingsRepository()
    private val clock = FixedClock(at("2026-10-24T20:00"))
    private val useCase = SaveExpenseUseCase(expenses, categories, settings, clock, SequentialIds(), FixedTimeZone())

    private fun draft(
        amount: Money? = Money(250_00),
        categoryId: String? = "cat_food",
        note: String = "Lunch",
        at: String = "2026-10-24T13:00",
    ) = ExpenseDraft(amount = amount, categoryId = categoryId, note = note, occurredAt = LocalDateTime.parse(at), paymentMethod = PaymentMethod.UPI)

    @Test
    fun savesNewExpenseWithDeviceZoneAndRemembersChoices() = runTest {
        val result = useCase(draft(note = "  Lunch  "))

        val saved = assertIs<Outcome.Success<Expense>>(result).value
        assertEquals("id-1", saved.id)
        assertEquals("Lunch", saved.note)
        assertEquals(IST.id, saved.timeZoneId)
        assertEquals(at("2026-10-24T13:00"), saved.occurredAt)
        assertEquals(clock.now, saved.createdAt)
        assertEquals(saved, expenses.getExpense("id-1"))
        assertEquals("cat_food", settings.state.value.lastUsedCategoryId)
        assertEquals(PaymentMethod.UPI, settings.state.value.lastUsedPaymentMethod)
    }

    @Test
    fun editKeepsIdentityAndCreationTime() = runTest {
        expenses.save(expense("e1", 100, "2026-10-20T10:00"))
        clock.now = at("2026-10-24T21:00")

        val result = useCase(draft().copy(id = "e1", amount = Money(150_00)))

        val saved = (result as Outcome.Success).value
        assertEquals("e1", saved.id)
        assertEquals(Money(150_00), saved.amount)
        assertEquals(at("2026-10-20T10:00"), saved.createdAt)
        assertEquals(at("2026-10-24T21:00"), saved.updatedAt)
    }

    @Test
    fun validation() = runTest {
        assertEquals(Outcome.Failure(ExpenseValidationError.AMOUNT_MISSING), useCase(draft(amount = null)))
        assertEquals(Outcome.Failure(ExpenseValidationError.AMOUNT_NOT_POSITIVE), useCase(draft(amount = Money(0))))
        assertEquals(Outcome.Failure(ExpenseValidationError.AMOUNT_TOO_LARGE), useCase(draft(amount = Money(SaveExpenseUseCase.MAX_AMOUNT_MINOR + 1))))
        assertEquals(Outcome.Failure(ExpenseValidationError.CATEGORY_MISSING), useCase(draft(categoryId = null)))
        assertEquals(Outcome.Failure(ExpenseValidationError.CATEGORY_MISSING), useCase(draft(categoryId = "nope")))
        assertEquals(Outcome.Failure(ExpenseValidationError.NOTE_TOO_LONG), useCase(draft(note = "x".repeat(201))))
        assertEquals(Outcome.Failure(ExpenseValidationError.DATE_IN_FUTURE), useCase(draft(at = "2026-10-26T09:00")))
        assertEquals(Outcome.Failure(ExpenseValidationError.NOT_FOUND), useCase(draft().copy(id = "missing")))
        assertEquals(emptyMap(), expenses.items.value)
    }
}

class SaveCategoryUseCaseTest {
    private val categories = FakeCategoryRepository()
    private val useCase = SaveCategoryUseCase(categories, SequentialIds("cat"))

    @Test
    fun createsCategoryAfterExistingOnes() = runTest {
        val result = useCase(CategoryDraft(name = " Gym ", iconKey = "fitness_center", colorArgb = 0xFF3F6B5C))

        val created = (result as Outcome.Success<Category>).value
        assertEquals("cat-1", created.id)
        assertEquals("Gym", created.name)
        assertEquals(false, created.isDefault)
        assertEquals(7, created.sortOrder)
    }

    @Test
    fun rejectsDuplicateNamesIgnoringCase() = runTest {
        assertEquals(Outcome.Failure(CategoryValidationError.NAME_TAKEN), useCase(CategoryDraft(name = "food", iconKey = "x", colorArgb = 0)))
    }

    @Test
    fun allowsRenamingToOwnName() = runTest {
        val result = useCase(CategoryDraft(id = "cat_food", name = "Food", iconKey = "local_cafe", colorArgb = 0xFFB07D4F))
        assertEquals("local_cafe", (result as Outcome.Success).value.iconKey)
    }

    @Test
    fun rejectsBlankAndLongNames() = runTest {
        assertEquals(Outcome.Failure(CategoryValidationError.NAME_MISSING), useCase(CategoryDraft(name = "  ", iconKey = "x", colorArgb = 0)))
        assertEquals(Outcome.Failure(CategoryValidationError.NAME_TOO_LONG), useCase(CategoryDraft(name = "x".repeat(31), iconKey = "x", colorArgb = 0)))
    }
}

class ObserveUseCasesTest {
    private val expenses = FakeExpenseRepository()
    private val categories = FakeCategoryRepository()

    @Test
    fun sectionsAreGroupedByDayNewestFirst() = runTest {
        listOf(
            expense("a", 100, "2026-10-23T09:00"),
            expense("b", 200, "2026-10-24T08:00"),
            expense("c", 300, "2026-10-24T19:00"),
        ).forEach { expenses.save(it) }

        ObserveExpenseSectionsUseCase(expenses, categories, FixedTimeZone())(ExpenseFilter()).test {
            val sections = awaitItem()
            assertEquals(listOf(LocalDate(2026, 10, 24), LocalDate(2026, 10, 23)), sections.map { it.date })
            assertEquals(listOf("c", "b"), sections[0].items.map { it.expense.id })
            assertEquals(Money(500_00), sections[0].total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dateRangeFilterUsesLocalDates() = runTest {
        listOf(
            expense("in", 100, "2026-10-24T00:10"),
            expense("out", 100, "2026-10-23T23:50"),
        ).forEach { expenses.save(it) }

        val filter = ExpenseFilter(dateRange = DateRange(LocalDate(2026, 10, 24), LocalDate(2026, 10, 24)))
        ObserveExpenseSectionsUseCase(expenses, categories, FixedTimeZone())(filter).test {
            assertEquals(listOf("in"), awaitItem().flatMap { s -> s.items.map { it.expense.id } })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dailyReportUpdatesWhenExpensesChange() = runTest {
        ObserveDailyReportUseCase(expenses, categories, FixedTimeZone())(LocalDate(2026, 10, 24)).test {
            assertEquals(Money(0), awaitItem().total)
            expenses.save(expense("a", 120, "2026-10-24T10:00"))
            assertEquals(Money(120_00), awaitItem().total)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
