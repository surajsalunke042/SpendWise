package com.surajdev.spendwise.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Sync state stored on every syncable row. */
object SyncState {
    const val SYNCED = 0
    const val PENDING = 1
}

/**
 * There is deliberately no foreign key to categories: sync can deliver an expense before its
 * category, and categories are archived rather than deleted.
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index("occurred_at"),
        Index("category_id"),
        Index("sync_state"),
        Index("source", "external_ref"),
    ],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "note") val note: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    @ColumnInfo(name = "payment_method") val paymentMethod: String?,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "external_ref") val externalRef: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "sync_state") val syncState: Int,
    @ColumnInfo(name = "last_modified_by") val lastModifiedBy: String,
)

@Entity(tableName = "categories", indices = [Index("sync_state")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_argb") val colorArgb: Long,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "archived") val archived: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "sync_state") val syncState: Int,
    @ColumnInfo(name = "last_modified_by") val lastModifiedBy: String,
)

/** Last server timestamp pulled per remote collection. */
@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey val collection: String,
    @ColumnInfo(name = "last_server_millis") val lastServerMillis: Long,
)
