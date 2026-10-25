package com.surajdev.spendwise.core.remote.firebase

import com.surajdev.spendwise.core.remote.CategoryRecord
import com.surajdev.spendwise.core.remote.ExpenseRecord
import com.surajdev.spendwise.core.remote.RemoteChange
import com.surajdev.spendwise.core.remote.SyncRemoteDataSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.BaseTimestampSerializer
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.memoryCacheSettings
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.serialization.Serializable

/**
 * Firestore layout: `users/{uid}/expenses/{id}` and `users/{uid}/categories/{id}`.
 * Every write stamps `serverUpdatedAt` with the server time, which the pull cursor uses.
 */
class FirestoreSyncRemoteDataSource : SyncRemoteDataSource {

    private val firestore: FirebaseFirestore by lazy {
        Firebase.firestore.apply {
            // Room is the only local cache; Firestore's own disk cache would just duplicate it.
            settings = firestoreSettings { cacheSettings = memoryCacheSettings { } }
        }
    }

    private fun userCollection(uid: String, name: String): CollectionReference =
        firestore.collection("users").document(uid).collection(name)

    override suspend fun pushExpenses(uid: String, records: List<ExpenseRecord>) {
        if (records.isEmpty()) return
        val collection = userCollection(uid, "expenses")
        val batch = firestore.batch()
        records.forEach { batch.set(collection.document(it.id), ExpenseDocument.from(it)) }
        batch.commit()
    }

    override suspend fun pullExpenses(uid: String, sinceMillis: Long, limit: Int): List<RemoteChange<ExpenseRecord>> =
        userCollection(uid, "expenses")
            .where { SERVER_UPDATED_AT greaterThanOrEqualTo Timestamp.fromMilliseconds(sinceMillis.toDouble()) }
            .orderBy(SERVER_UPDATED_AT)
            .limit(limit)
            .get()
            .documents
            .map { snapshot ->
                val doc = snapshot.data<ExpenseDocument>(ServerTimestampBehavior.ESTIMATE)
                RemoteChange(doc.toRecord(), doc.serverUpdatedAt.millis())
            }

    override suspend fun pushCategories(uid: String, records: List<CategoryRecord>) {
        if (records.isEmpty()) return
        val collection = userCollection(uid, "categories")
        val batch = firestore.batch()
        records.forEach { batch.set(collection.document(it.id), CategoryDocument.from(it)) }
        batch.commit()
    }

    override suspend fun pullCategories(uid: String, sinceMillis: Long, limit: Int): List<RemoteChange<CategoryRecord>> =
        userCollection(uid, "categories")
            .where { SERVER_UPDATED_AT greaterThanOrEqualTo Timestamp.fromMilliseconds(sinceMillis.toDouble()) }
            .orderBy(SERVER_UPDATED_AT)
            .limit(limit)
            .get()
            .documents
            .map { snapshot ->
                val doc = snapshot.data<CategoryDocument>(ServerTimestampBehavior.ESTIMATE)
                RemoteChange(doc.toRecord(), doc.serverUpdatedAt.millis())
            }

    private fun BaseTimestamp.millis(): Long = when (this) {
        is Timestamp -> toMilliseconds().toLong()
        else -> 0L
    }

    private companion object {
        const val SERVER_UPDATED_AT = "serverUpdatedAt"
    }
}

@Serializable
internal data class ExpenseDocument(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val note: String,
    val occurredAt: Long,
    val timeZoneId: String,
    val paymentMethod: String? = null,
    val source: String,
    val externalRef: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val lastModifiedBy: String,
    @Serializable(with = BaseTimestampSerializer::class)
    val serverUpdatedAt: BaseTimestamp = Timestamp.ServerTimestamp,
) {
    fun toRecord() = ExpenseRecord(
        id, amountMinor, currency, categoryId, note, occurredAt, timeZoneId, paymentMethod,
        source, externalRef, createdAt, updatedAt, deletedAt, lastModifiedBy,
    )

    companion object {
        fun from(r: ExpenseRecord) = ExpenseDocument(
            r.id, r.amountMinor, r.currency, r.categoryId, r.note, r.occurredAt, r.timeZoneId, r.paymentMethod,
            r.source, r.externalRef, r.createdAt, r.updatedAt, r.deletedAt, r.lastModifiedBy,
        )
    }
}

@Serializable
internal data class CategoryDocument(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val lastModifiedBy: String,
    @Serializable(with = BaseTimestampSerializer::class)
    val serverUpdatedAt: BaseTimestamp = Timestamp.ServerTimestamp,
) {
    fun toRecord() = CategoryRecord(
        id, name, iconKey, colorArgb, isDefault, sortOrder, archived, createdAt, updatedAt, deletedAt, lastModifiedBy,
    )

    companion object {
        fun from(r: CategoryRecord) = CategoryDocument(
            r.id, r.name, r.iconKey, r.colorArgb, r.isDefault, r.sortOrder, r.archived,
            r.createdAt, r.updatedAt, r.deletedAt, r.lastModifiedBy,
        )
    }
}
