package com.surajdev.spendwise.core.data.repository

import com.surajdev.spendwise.core.data.local.AppPreferences
import com.surajdev.spendwise.core.data.mapper.toDomain
import com.surajdev.spendwise.core.data.mapper.toEntity
import com.surajdev.spendwise.core.database.dao.CategoryDao
import com.surajdev.spendwise.core.database.dao.ExpenseDao
import com.surajdev.spendwise.core.database.entity.SyncState
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.DefaultCategories
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.domain.model.UserSettings
import com.surajdev.spendwise.core.domain.platform.ReminderScheduler
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.ExpenseRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import kotlin.time.Clock
import kotlin.time.Instant

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao,
    private val preferences: AppPreferences,
    private val clock: Clock,
) : ExpenseRepository {

    override fun observeExpenses(
        window: ClosedRange<Instant>?,
        query: String,
        categoryIds: Set<String>,
    ): Flow<List<Expense>> = dao.observe(
        fromMillis = window?.start?.toEpochMilliseconds(),
        toMillis = window?.endInclusive?.toEpochMilliseconds(),
        query = query,
        categoryIds = categoryIds.toList(),
        categoryCount = categoryIds.size,
    ).map { list -> list.map { it.toDomain() } }

    override fun observeExpense(id: String): Flow<Expense?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun getExpense(id: String): Expense? = dao.getById(id)?.takeIf { it.deletedAt == null }?.toDomain()

    override fun observeEarliestExpenseTime(): Flow<Instant?> =
        dao.observeEarliestOccurredAt().map { it?.let(Instant::fromEpochMilliseconds) }

    override suspend fun save(expense: Expense) {
        dao.upsert(expense.toEntity(preferences.deviceId()))
    }

    override suspend fun delete(id: String) {
        dao.markDeleted(id, clock.now().toEpochMilliseconds(), preferences.deviceId())
    }

    override suspend fun restore(id: String) {
        dao.restore(id, clock.now().toEpochMilliseconds(), preferences.deviceId())
    }
}

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
    private val preferences: AppPreferences,
    private val clock: Clock,
) : CategoryRepository {

    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
        dao.observe(includeArchived).map { list -> list.map { it.toDomain() } }

    override suspend fun getCategory(id: String): Category? =
        dao.getById(id)?.takeIf { it.deletedAt == null }?.toDomain()

    override suspend fun save(category: Category) {
        val now = clock.now().toEpochMilliseconds()
        val createdAt = dao.getById(category.id)?.createdAt ?: now
        dao.upsert(category.toEntity(createdAt, now, SyncState.PENDING, preferences.deviceId()))
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        dao.setArchived(id, archived, clock.now().toEpochMilliseconds(), preferences.deviceId())
    }

    override suspend fun ensureDefaults() {
        // Seeds are stamped with time 0 and marked synced, so they are never pushed unless the user
        // edits them, and any edited copy from another device always wins.
        val seeds = DefaultCategories.all.map {
            it.toEntity(createdAt = 0, updatedAt = 0, syncState = SyncState.SYNCED, deviceId = SEED_DEVICE)
        }
        dao.insertIfMissing(seeds)
    }

    private companion object {
        const val SEED_DEVICE = "seed"
    }
}

class SettingsRepositoryImpl(
    private val preferences: AppPreferences,
    private val reminderScheduler: ReminderScheduler,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = preferences.settings

    override suspend fun setReminder(enabled: Boolean, time: LocalTime) {
        preferences.setReminder(enabled, time)
        if (enabled) reminderScheduler.schedule(time) else reminderScheduler.cancel()
    }

    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    override suspend fun dismissReminderPrompt() = preferences.dismissReminderPrompt()

    override suspend fun rememberLastUsed(categoryId: String, paymentMethod: PaymentMethod?) =
        preferences.rememberLastUsed(categoryId, paymentMethod)
}
