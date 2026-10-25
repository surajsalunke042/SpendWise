package com.surajdev.spendwise.testing

import com.surajdev.spendwise.core.common.IdGenerator
import com.surajdev.spendwise.core.common.TimeZoneProvider
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.DefaultCategories
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.domain.model.UserSettings
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.ExpenseRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Instant

val IST: TimeZone = TimeZone.of("Asia/Kolkata")

class FixedClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

class FixedTimeZone(private val zone: TimeZone = IST) : TimeZoneProvider {
    override fun current(): TimeZone = zone
}

class SequentialIds(private val prefix: String = "id") : IdGenerator {
    private var next = 1
    override fun newId(): String = "$prefix-${next++}"
}

fun at(dateTime: String, zone: TimeZone = IST): Instant = LocalDateTime.parse(dateTime).toInstant(zone)

fun expense(
    id: String,
    rupees: Long,
    at: String,
    categoryId: String = "cat_food",
    note: String = "",
    zone: TimeZone = IST,
): Expense {
    val instant = at(at, zone)
    return Expense(
        id = id,
        amount = Money(rupees * 100),
        categoryId = categoryId,
        note = note,
        occurredAt = instant,
        timeZoneId = zone.id,
        paymentMethod = null,
        createdAt = instant,
        updatedAt = instant,
    )
}

val defaultCategoryMap: Map<String, Category> = DefaultCategories.all.associateBy { it.id }

class FakeExpenseRepository : ExpenseRepository {
    val items = MutableStateFlow<Map<String, Expense>>(emptyMap())
    private val deleted = mutableMapOf<String, Expense>()

    override fun observeExpenses(window: ClosedRange<Instant>?, query: String, categoryIds: Set<String>): Flow<List<Expense>> =
        items.map { map ->
            map.values
                .filter { window == null || (it.occurredAt >= window.start && it.occurredAt < window.endInclusive) }
                .filter { query.isBlank() || it.note.contains(query, ignoreCase = true) }
                .filter { categoryIds.isEmpty() || it.categoryId in categoryIds }
                .sortedByDescending { it.occurredAt }
        }

    override fun observeExpense(id: String): Flow<Expense?> = items.map { it[id] }
    override suspend fun getExpense(id: String): Expense? = items.value[id]
    override fun observeEarliestExpenseTime(): Flow<Instant?> = items.map { map -> map.values.minOfOrNull { it.occurredAt } }
    override suspend fun save(expense: Expense) = items.update { it + (expense.id to expense) }

    override suspend fun delete(id: String) {
        items.value[id]?.let { deleted[id] = it }
        items.update { it - id }
    }

    override suspend fun restore(id: String) {
        deleted.remove(id)?.let { save(it) }
    }
}

class FakeCategoryRepository(initial: List<Category> = DefaultCategories.all) : CategoryRepository {
    val items = MutableStateFlow(initial.associateBy { it.id })

    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
        items.map { map -> map.values.filter { includeArchived || !it.isArchived }.sortedBy { it.sortOrder } }

    override suspend fun getCategory(id: String): Category? = items.value[id]
    override suspend fun save(category: Category) = items.update { it + (category.id to category) }
    override suspend fun setArchived(id: String, archived: Boolean) =
        items.update { map -> map[id]?.let { map + (id to it.copy(isArchived = archived)) } ?: map }

    override suspend fun ensureDefaults() = Unit
}

class FakeSettingsRepository : SettingsRepository {
    val state = MutableStateFlow(UserSettings())
    override val settings: Flow<UserSettings> = state
    override suspend fun setReminder(enabled: Boolean, time: LocalTime) = state.update { it.copy(reminderEnabled = enabled, reminderTime = time) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun dismissReminderPrompt() = state.update { it.copy(reminderPromptDismissed = true) }
    override suspend fun rememberLastUsed(categoryId: String, paymentMethod: PaymentMethod?) =
        state.update { it.copy(lastUsedCategoryId = categoryId, lastUsedPaymentMethod = paymentMethod) }
}
