package com.surajdev.spendwise.core.data

import com.surajdev.spendwise.core.data.repository.CategoryRepositoryImpl
import com.surajdev.spendwise.core.data.repository.ExpenseRepositoryImpl
import com.surajdev.spendwise.core.database.entity.SyncState
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.DefaultCategories
import com.surajdev.spendwise.testing.FixedClock
import com.surajdev.spendwise.testing.at
import com.surajdev.spendwise.testing.expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpenseRepositoryTest {
    private val db = inMemoryDatabase()
    private val prefs = tempPreferences()
    private val clock = FixedClock(at("2026-10-24T20:00"))
    private val expenses = ExpenseRepositoryImpl(db.expenseDao(), prefs, clock)
    private val categories = CategoryRepositoryImpl(db.categoryDao(), prefs, clock)

    @AfterTest
    fun close() = db.close()

    @Test
    fun savesAndReadsBack() = runTest {
        val e = expense("e1", 250, "2026-10-24T13:00", note = "Lunch")
        expenses.save(e)

        assertEquals(e, expenses.getExpense("e1"))
        assertEquals(SyncState.PENDING, db.expenseDao().getById("e1")?.syncState)
    }

    @Test
    fun searchMatchesNoteAndCategoryName() = runTest {
        categories.ensureDefaults()
        expenses.save(expense("a", 100, "2026-10-24T09:00", "cat_food", note = "Masala chai"))
        expenses.save(expense("b", 200, "2026-10-24T10:00", "cat_transport", note = "Metro card"))

        assertEquals(listOf("a"), expenses.observeExpenses(query = "chai").first().map { it.id })
        assertEquals(listOf("b"), expenses.observeExpenses(query = "transport").first().map { it.id })
        assertEquals(listOf("b"), expenses.observeExpenses(categoryIds = setOf("cat_transport")).first().map { it.id })
        assertEquals(listOf("b", "a"), expenses.observeExpenses().first().map { it.id })
    }

    @Test
    fun timeWindowIsHalfOpen() = runTest {
        expenses.save(expense("start", 1, "2026-10-24T00:00"))
        expenses.save(expense("end", 1, "2026-10-25T00:00"))

        val window = at("2026-10-24T00:00")..at("2026-10-25T00:00")
        assertEquals(listOf("start"), expenses.observeExpenses(window).first().map { it.id })
    }

    @Test
    fun softDeleteHidesAndRestoreBringsBack() = runTest {
        expenses.save(expense("e1", 100, "2026-10-24T09:00"))
        db.expenseDao().markSynced("e1", db.expenseDao().getById("e1")!!.updatedAt)

        expenses.delete("e1")
        assertNull(expenses.getExpense("e1"))
        assertEquals(emptyList(), expenses.observeExpenses().first())
        val tombstone = db.expenseDao().getById("e1")!!
        assertEquals(SyncState.PENDING, tombstone.syncState)
        assertEquals(clock.now.toEpochMilliseconds(), tombstone.deletedAt)

        expenses.restore("e1")
        assertEquals("e1", expenses.getExpense("e1")?.id)
    }

    @Test
    fun markSyncedIgnoresRowsEditedSincePush() = runTest {
        expenses.save(expense("e1", 100, "2026-10-24T09:00"))
        val pushedVersion = db.expenseDao().getById("e1")!!.updatedAt
        expenses.save(expense("e1", 150, "2026-10-24T09:00").copy(updatedAt = at("2026-10-24T21:00")))

        db.expenseDao().markSynced("e1", pushedVersion)

        assertEquals(SyncState.PENDING, db.expenseDao().getById("e1")?.syncState)
    }

    @Test
    fun defaultsAreSeededOnceAndUserEditsSurvive() = runTest {
        categories.ensureDefaults()
        categories.save(DefaultCategories.all.first().copy(name = "Food & dining"))
        categories.ensureDefaults()

        val all = categories.observeCategories().first()
        assertEquals(DefaultCategories.all.size, all.size)
        assertEquals("Food & dining", all.first { it.id == "cat_food" }.name)
    }

    @Test
    fun archivedCategoriesAreHiddenByDefault() = runTest {
        categories.save(Category("gym", "Gym", "fitness_center", 0xFF3F6B5C, isDefault = false, sortOrder = 9))
        categories.setArchived("gym", true)

        assertEquals(emptyList(), categories.observeCategories().first().map { it.id })
        assertEquals(listOf("gym"), categories.observeCategories(includeArchived = true).first().map { it.id })
    }
}
