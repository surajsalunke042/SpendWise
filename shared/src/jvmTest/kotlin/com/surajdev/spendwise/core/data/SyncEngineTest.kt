package com.surajdev.spendwise.core.data

import com.surajdev.spendwise.core.data.repository.CategoryRepositoryImpl
import com.surajdev.spendwise.core.data.repository.ExpenseRepositoryImpl
import com.surajdev.spendwise.core.data.sync.SyncEngine
import com.surajdev.spendwise.core.database.entity.SyncState
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.testing.FixedClock
import com.surajdev.spendwise.testing.at
import com.surajdev.spendwise.testing.expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/** A simulated phone: its own database, preferences and clock, sharing one [FakeRemote]. */
private class Device(name: String, remote: FakeRemote, val clock: FixedClock = FixedClock(at("2026-10-24T09:00"))) {
    val db = inMemoryDatabase()
    val prefs = tempPreferences(name)
    val expenses = ExpenseRepositoryImpl(db.expenseDao(), prefs, clock)
    val categories = CategoryRepositoryImpl(db.categoryDao(), prefs, clock)
    val engine = SyncEngine(db, remote, prefs, clock)

    suspend fun ids() = expenses.observeExpenses().first().map { it.id }.sorted()
}

class SyncEngineTest {
    private val remote = FakeRemote()
    private val uid = "user-1"

    @Test
    fun pushesPendingChangesAndMarksThemSynced() = runTest {
        val phone = Device("phone", remote)
        phone.expenses.save(expense("e1", 120, "2026-10-24T08:00"))

        phone.engine.sync(uid)

        assertEquals(listOf("e1"), remote.expensesOf(uid).map { it.id })
        assertEquals(SyncState.SYNCED, phone.db.expenseDao().getById("e1")?.syncState)
    }

    @Test
    fun newDevicePullsEverything() = runTest {
        val phone = Device("phone", remote)
        phone.expenses.save(expense("e1", 120, "2026-10-24T08:00"))
        phone.expenses.save(expense("e2", 80, "2026-10-24T08:30"))
        phone.engine.sync(uid)

        val tablet = Device("tablet", remote)
        tablet.engine.sync(uid)

        assertEquals(listOf("e1", "e2"), tablet.ids())
    }

    @Test
    fun localDataIsUploadedOnFirstSignIn() = runTest {
        val phone = Device("phone", remote)
        phone.expenses.save(expense("offline", 50, "2026-10-20T10:00"))
        // Pretend it had synced before sign-in was possible.
        phone.db.expenseDao().markSynced("offline", phone.db.expenseDao().getById("offline")!!.updatedAt)

        phone.engine.sync(uid)

        assertEquals(listOf("offline"), remote.expensesOf(uid).map { it.id })
    }

    @Test
    fun laterEditWinsOnBothDevices() = runTest {
        val phone = Device("phone", remote)
        val tablet = Device("tablet", remote)
        phone.expenses.save(expense("e1", 100, "2026-10-24T08:00"))
        phone.engine.sync(uid)
        tablet.engine.sync(uid)

        // Both edit offline; the tablet's edit happens later.
        val base = tablet.expenses.getExpense("e1")!!
        phone.expenses.save(base.copy(amount = Money(150_00), updatedAt = at("2026-10-24T10:00")))
        tablet.expenses.save(base.copy(amount = Money(175_00), updatedAt = at("2026-10-24T10:05")))

        phone.engine.sync(uid)
        tablet.engine.sync(uid)
        phone.engine.sync(uid)

        assertEquals(Money(175_00), phone.expenses.getExpense("e1")?.amount)
        assertEquals(Money(175_00), tablet.expenses.getExpense("e1")?.amount)
    }

    @Test
    fun deletionPropagates() = runTest {
        val phone = Device("phone", remote)
        val tablet = Device("tablet", remote)
        phone.expenses.save(expense("e1", 100, "2026-10-24T08:00"))
        phone.engine.sync(uid)
        tablet.engine.sync(uid)

        phone.clock.now = at("2026-10-24T11:00")
        phone.expenses.delete("e1")
        phone.engine.sync(uid)
        tablet.engine.sync(uid)

        assertNull(tablet.expenses.getExpense("e1"))
        assertEquals(emptyList(), tablet.ids())
    }

    @Test
    fun failedPushKeepsChangesPendingForNextTime() = runTest {
        val phone = Device("phone", remote)
        phone.expenses.save(expense("e1", 100, "2026-10-24T08:00"))
        remote.failNextPush = true

        assertFailsWith<IllegalStateException> { phone.engine.sync(uid) }
        assertEquals(SyncState.PENDING, phone.db.expenseDao().getById("e1")?.syncState)

        phone.engine.sync(uid)
        assertEquals(listOf("e1"), remote.expensesOf(uid).map { it.id })
    }

    @Test
    fun differentAccountsStaySeparate() = runTest {
        val phone = Device("phone", remote)
        phone.expenses.save(expense("mine", 100, "2026-10-24T08:00"))
        phone.engine.sync(uid)

        val friend = Device("friend", remote)
        friend.engine.sync("user-2")

        assertEquals(emptyList(), friend.ids())
    }

    @Test
    fun tombstonesArePurgedAfterRetention() = runTest {
        val phone = Device("phone", remote)
        phone.expenses.save(expense("e1", 100, "2026-10-24T08:00"))
        phone.expenses.delete("e1")
        phone.engine.sync(uid)

        phone.clock.now = phone.clock.now + SyncEngine.TOMBSTONE_RETENTION + 1.minutes
        phone.engine.sync(uid)

        assertNull(phone.db.expenseDao().getById("e1"))
    }
}
