package com.surajdev.spendwise.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.surajdev.spendwise.core.database.entity.CategoryEntity
import com.surajdev.spendwise.core.database.entity.SyncCursorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query(
        """
        SELECT * FROM categories
        WHERE deleted_at IS NULL AND (:includeArchived OR archived = 0)
        ORDER BY sort_order, name
        """,
    )
    fun observe(includeArchived: Boolean): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Upsert
    suspend fun upsert(entity: CategoryEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CategoryEntity>)

    /** Used for seeding defaults: existing rows (possibly edited by the user) are left alone. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(entities: List<CategoryEntity>)

    @Query(
        """
        UPDATE categories SET archived = :archived, updated_at = :now, sync_state = 1, last_modified_by = :deviceId
        WHERE id = :id
        """,
    )
    suspend fun setArchived(id: String, archived: Boolean, now: Long, deviceId: String)

    // --- Sync ---

    @Query("SELECT * FROM categories WHERE sync_state = 1 ORDER BY updated_at LIMIT :limit")
    suspend fun getPending(limit: Int): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories WHERE sync_state = 1")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE categories SET sync_state = 0 WHERE id = :id AND updated_at = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("UPDATE categories SET sync_state = 1")
    suspend fun markAllPending()

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface SyncCursorDao {
    @Query("SELECT last_server_millis FROM sync_cursors WHERE collection = :collection")
    suspend fun get(collection: String): Long?

    @Upsert
    suspend fun upsert(entity: SyncCursorEntity)

    @Query("DELETE FROM sync_cursors")
    suspend fun clear()
}
