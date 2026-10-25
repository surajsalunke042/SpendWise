package com.surajdev.spendwise.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.surajdev.spendwise.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /**
     * Search across note and category name, with optional time window and category filter.
     * Pass `categoryCount = 0` to skip the category filter.
     */
    @Query(
        """
        SELECT e.* FROM expenses e
        LEFT JOIN categories c ON c.id = e.category_id
        WHERE e.deleted_at IS NULL
          AND (:fromMillis IS NULL OR e.occurred_at >= :fromMillis)
          AND (:toMillis IS NULL OR e.occurred_at < :toMillis)
          AND (:query = '' OR e.note LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
          AND (:categoryCount = 0 OR e.category_id IN (:categoryIds))
        ORDER BY e.occurred_at DESC
        """,
    )
    fun observe(
        fromMillis: Long?,
        toMillis: Long?,
        query: String,
        categoryIds: List<String>,
        categoryCount: Int,
    ): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id AND deleted_at IS NULL")
    fun observeById(id: String): Flow<ExpenseEntity?>

    /** Includes soft-deleted rows; used by sync and restore. */
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT MIN(occurred_at) FROM expenses WHERE deleted_at IS NULL")
    fun observeEarliestOccurredAt(): Flow<Long?>

    @Query(
        """
        SELECT COALESCE(SUM(amount_minor), 0) FROM expenses
        WHERE deleted_at IS NULL AND occurred_at >= :fromMillis AND occurred_at < :toMillis
        """,
    )
    suspend fun sumBetween(fromMillis: Long, toMillis: Long): Long

    @Query(
        """
        SELECT COUNT(*) FROM expenses
        WHERE deleted_at IS NULL AND occurred_at >= :fromMillis AND occurred_at < :toMillis
        """,
    )
    suspend fun countBetween(fromMillis: Long, toMillis: Long): Int

    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    @Query(
        """
        UPDATE expenses SET deleted_at = :now, updated_at = :now, sync_state = 1, last_modified_by = :deviceId
        WHERE id = :id
        """,
    )
    suspend fun markDeleted(id: String, now: Long, deviceId: String)

    @Query(
        """
        UPDATE expenses SET deleted_at = NULL, updated_at = :now, sync_state = 1, last_modified_by = :deviceId
        WHERE id = :id
        """,
    )
    suspend fun restore(id: String, now: Long, deviceId: String)

    // --- Sync ---

    @Query("SELECT * FROM expenses WHERE sync_state = 1 ORDER BY updated_at LIMIT :limit")
    suspend fun getPending(limit: Int): List<ExpenseEntity>

    @Query("SELECT COUNT(*) FROM expenses WHERE sync_state = 1")
    fun observePendingCount(): Flow<Int>

    /** Clears the pending flag only if the row wasn't edited again while the push was in flight. */
    @Query("UPDATE expenses SET sync_state = 0 WHERE id = :id AND updated_at = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("UPDATE expenses SET sync_state = 1")
    suspend fun markAllPending()

    @Query("DELETE FROM expenses WHERE deleted_at IS NOT NULL AND deleted_at < :before AND sync_state = 0")
    suspend fun purgeTombstones(before: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
