package com.surajdev.spendwise.core.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.surajdev.spendwise.core.data.local.AppPreferences
import com.surajdev.spendwise.core.data.local.createPreferencesDataStore
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.remote.CategoryRecord
import com.surajdev.spendwise.core.remote.ExpenseRecord
import com.surajdev.spendwise.core.remote.RemoteChange
import com.surajdev.spendwise.core.remote.SyncRemoteDataSource
import com.surajdev.spendwise.testing.SequentialIds
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files

fun inMemoryDatabase(): SpendWiseDatabase =
    Room.inMemoryDatabaseBuilder<SpendWiseDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

fun tempPreferences(deviceIdPrefix: String = "device"): AppPreferences {
    val dir = Files.createTempDirectory("spendwise-prefs").toFile()
    return AppPreferences(createPreferencesDataStore(File(dir, "test.preferences_pb").absolutePath), SequentialIds(deviceIdPrefix))
}

/**
 * In-memory stand-in for Firestore, keyed by user. Each push gets a strictly increasing
 * "server" timestamp, shared by every record in the batch, like a Firestore batch commit.
 */
class FakeRemote : SyncRemoteDataSource {
    private var serverClock = 1_000L
    private val expenses = mutableMapOf<String, MutableMap<String, RemoteChange<ExpenseRecord>>>()
    private val categories = mutableMapOf<String, MutableMap<String, RemoteChange<CategoryRecord>>>()
    var failNextPush = false

    fun expensesOf(uid: String): List<ExpenseRecord> = expenses[uid].orEmpty().values.map { it.record }

    override suspend fun pushExpenses(uid: String, records: List<ExpenseRecord>) {
        if (failNextPush) {
            failNextPush = false
            error("Network unavailable")
        }
        val stamp = ++serverClock
        val store = expenses.getOrPut(uid) { mutableMapOf() }
        records.forEach { store[it.id] = RemoteChange(it, stamp) }
    }

    override suspend fun pullExpenses(uid: String, sinceMillis: Long, limit: Int) =
        expenses[uid].orEmpty().values.filter { it.serverUpdatedAtMillis >= sinceMillis }
            .sortedBy { it.serverUpdatedAtMillis }.take(limit)

    override suspend fun pushCategories(uid: String, records: List<CategoryRecord>) {
        val stamp = ++serverClock
        val store = categories.getOrPut(uid) { mutableMapOf() }
        records.forEach { store[it.id] = RemoteChange(it, stamp) }
    }

    override suspend fun pullCategories(uid: String, sinceMillis: Long, limit: Int) =
        categories[uid].orEmpty().values.filter { it.serverUpdatedAtMillis >= sinceMillis }
            .sortedBy { it.serverUpdatedAtMillis }.take(limit)
}
