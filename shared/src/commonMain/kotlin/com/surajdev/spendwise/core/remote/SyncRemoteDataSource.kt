package com.surajdev.spendwise.core.remote

/**
 * The cloud copy of a user's data. Implemented with Firestore on Android and iOS; a no-op
 * implementation is used when Firebase isn't configured (and on the JVM test target).
 */
interface SyncRemoteDataSource {
    suspend fun pushExpenses(uid: String, records: List<ExpenseRecord>)

    /** Changes with server timestamp >= [sinceMillis], oldest first, at most [limit]. */
    suspend fun pullExpenses(uid: String, sinceMillis: Long, limit: Int): List<RemoteChange<ExpenseRecord>>

    suspend fun pushCategories(uid: String, records: List<CategoryRecord>)

    suspend fun pullCategories(uid: String, sinceMillis: Long, limit: Int): List<RemoteChange<CategoryRecord>>

    /** Deletes every expense and category stored for [uid]. Must fail, not silently skip, when offline. */
    suspend fun deleteAllData(uid: String)
}

data class RemoteChange<T>(val record: T, val serverUpdatedAtMillis: Long)

/** Platform-neutral sync payload for an expense (tombstones included). */
data class ExpenseRecord(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val note: String,
    val occurredAt: Long,
    val timeZoneId: String,
    val paymentMethod: String?,
    val source: String,
    val externalRef: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val lastModifiedBy: String,
)

data class CategoryRecord(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val lastModifiedBy: String,
)

class NoOpSyncRemoteDataSource : SyncRemoteDataSource {
    override suspend fun pushExpenses(uid: String, records: List<ExpenseRecord>) = Unit
    override suspend fun pullExpenses(uid: String, sinceMillis: Long, limit: Int) = emptyList<RemoteChange<ExpenseRecord>>()
    override suspend fun pushCategories(uid: String, records: List<CategoryRecord>) = Unit
    override suspend fun pullCategories(uid: String, sinceMillis: Long, limit: Int) = emptyList<RemoteChange<CategoryRecord>>()
    override suspend fun deleteAllData(uid: String) = Unit
}
