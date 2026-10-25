package com.surajdev.spendwise.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.surajdev.spendwise.core.database.dao.CategoryDao
import com.surajdev.spendwise.core.database.dao.ExpenseDao
import com.surajdev.spendwise.core.database.dao.SyncCursorDao
import com.surajdev.spendwise.core.database.entity.CategoryEntity
import com.surajdev.spendwise.core.database.entity.ExpenseEntity
import com.surajdev.spendwise.core.database.entity.SyncCursorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Local source of truth. Schema JSON is exported to `shared/schemas` on every build; commit it,
 * because migration tests compare against it. See README "Database migrations".
 */
@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, SyncCursorEntity::class],
    version = SpendWiseDatabase.VERSION,
    exportSchema = true,
)
@ConstructedBy(SpendWiseDatabaseConstructor::class)
abstract class SpendWiseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun syncCursorDao(): SyncCursorDao

    companion object {
        const val VERSION = 1
        const val FILE_NAME = "spendwise.db"

        /** Hand-written migrations, oldest first. Prefer @AutoMigration for simple column additions. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}

// Room generates the actual implementations for each platform.
@Suppress("KotlinNoActualForExpect")
expect object SpendWiseDatabaseConstructor : RoomDatabaseConstructor<SpendWiseDatabase> {
    override fun initialize(): SpendWiseDatabase
}

fun RoomDatabase.Builder<SpendWiseDatabase>.buildSpendWiseDatabase(): SpendWiseDatabase =
    addMigrations(*SpendWiseDatabase.MIGRATIONS)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
