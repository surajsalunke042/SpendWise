package com.surajdev.spendwise.core.data.sync

import androidx.room3.withWriteTransaction
import com.surajdev.spendwise.core.data.local.AppPreferences
import com.surajdev.spendwise.core.data.mapper.toEntity
import com.surajdev.spendwise.core.data.mapper.toRecord
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.database.entity.CategoryEntity
import com.surajdev.spendwise.core.database.entity.ExpenseEntity
import com.surajdev.spendwise.core.database.entity.SyncCursorEntity
import com.surajdev.spendwise.core.remote.SyncRemoteDataSource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Two-way sync between Room (source of truth) and the remote store.
 *
 * Order: push local changes, then pull remote changes since the last cursor. Pull uses server
 * timestamps, so device clock differences can't cause missed changes; conflicts are decided by
 * [ConflictResolver] using client timestamps, which reflect what the user did last.
 */
class SyncEngine(
    private val database: SpendWiseDatabase,
    private val remote: SyncRemoteDataSource,
    private val preferences: AppPreferences,
    private val clock: Clock,
) {
    private val expenseDao = database.expenseDao()
    private val categoryDao = database.categoryDao()
    private val cursorDao = database.syncCursorDao()

    suspend fun sync(uid: String) {
        prepareForUser(uid)
        pushCategories(uid)
        pushExpenses(uid)
        pullCategories(uid)
        pullExpenses(uid)
        expenseDao.purgeTombstones(before = (clock.now() - TOMBSTONE_RETENTION).toEpochMilliseconds())
    }

    /**
     * When a different account (or the first account) syncs from this device, upload everything
     * local and pull the account's full history.
     */
    private suspend fun prepareForUser(uid: String) {
        if (preferences.lastSyncedUid() == uid) return
        database.withWriteTransaction {
            expenseDao.markAllPending()
            categoryDao.markAllPending()
            cursorDao.clear()
        }
        preferences.setLastSyncedUid(uid)
    }

    private suspend fun pushExpenses(uid: String) {
        while (true) {
            val pending = expenseDao.getPending(PUSH_BATCH)
            if (pending.isEmpty()) return
            remote.pushExpenses(uid, pending.map { it.toRecord() })
            database.withWriteTransaction {
                pending.forEach { expenseDao.markSynced(it.id, it.updatedAt) }
            }
            if (pending.size < PUSH_BATCH) return
        }
    }

    private suspend fun pushCategories(uid: String) {
        while (true) {
            val pending = categoryDao.getPending(PUSH_BATCH)
            if (pending.isEmpty()) return
            remote.pushCategories(uid, pending.map { it.toRecord() })
            database.withWriteTransaction {
                pending.forEach { categoryDao.markSynced(it.id, it.updatedAt) }
            }
            if (pending.size < PUSH_BATCH) return
        }
    }

    private suspend fun pullExpenses(uid: String) {
        var cursor = cursorDao.get(EXPENSES) ?: 0L
        while (true) {
            val previous = cursor
            val changes = remote.pullExpenses(uid, cursor, PULL_PAGE)
            if (changes.isEmpty()) return
            database.withWriteTransaction {
                changes.forEach { change ->
                    val incoming = change.record.toEntity()
                    val local = expenseDao.getById(incoming.id)
                    if (ConflictResolver.shouldApplyRemote(local?.stamp(), incoming.stamp())) {
                        expenseDao.upsert(incoming)
                    }
                }
                cursor = maxOf(cursor, changes.maxOf { it.serverUpdatedAtMillis })
                cursorDao.upsert(SyncCursorEntity(EXPENSES, cursor))
            }
            // Pages are fetched with ">= cursor", so a short page means we've caught up.
            // A full page that didn't move the cursor would loop forever; stop instead.
            if (changes.size < PULL_PAGE || cursor == previous) return
        }
    }

    private suspend fun pullCategories(uid: String) {
        var cursor = cursorDao.get(CATEGORIES) ?: 0L
        while (true) {
            val previous = cursor
            val changes = remote.pullCategories(uid, cursor, PULL_PAGE)
            if (changes.isEmpty()) return
            database.withWriteTransaction {
                changes.forEach { change ->
                    val incoming = change.record.toEntity()
                    val local = categoryDao.getById(incoming.id)
                    if (ConflictResolver.shouldApplyRemote(local?.stamp(), incoming.stamp())) {
                        categoryDao.upsert(incoming)
                    }
                }
                cursor = maxOf(cursor, changes.maxOf { it.serverUpdatedAtMillis })
                cursorDao.upsert(SyncCursorEntity(CATEGORIES, cursor))
            }
            if (changes.size < PULL_PAGE || cursor == previous) return
        }
    }

    private fun ExpenseEntity.stamp() = VersionStamp(updatedAt, deletedAt != null, lastModifiedBy)

    private fun CategoryEntity.stamp() = VersionStamp(updatedAt, deletedAt != null, lastModifiedBy)

    companion object {
        const val EXPENSES = "expenses"
        const val CATEGORIES = "categories"

        /** Firestore allows 500 writes per batch; pushes stay below that. */
        const val PUSH_BATCH = 400

        /** Must be larger than [PUSH_BATCH]: one batch shares a server timestamp. */
        const val PULL_PAGE = 500

        val TOMBSTONE_RETENTION = 30.days
    }
}
