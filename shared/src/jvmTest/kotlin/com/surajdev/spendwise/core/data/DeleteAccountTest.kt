package com.surajdev.spendwise.core.data

import com.surajdev.spendwise.core.data.repository.CategoryRepositoryImpl
import com.surajdev.spendwise.core.data.repository.ExpenseRepositoryImpl
import com.surajdev.spendwise.core.data.sync.SyncEngine
import com.surajdev.spendwise.core.data.sync.SyncRepositoryImpl
import com.surajdev.spendwise.core.domain.model.AuthProvider
import com.surajdev.spendwise.core.domain.model.AuthUser
import com.surajdev.spendwise.core.domain.repository.AuthError
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.testing.FixedClock
import com.surajdev.spendwise.testing.at
import com.surajdev.spendwise.testing.expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuth(uid: String) : AuthRepository {
    val user = MutableStateFlow<AuthUser?>(AuthUser(uid = uid, email = "a@b.com", displayName = null, providers = setOf(AuthProvider.EMAIL)))
    var deleteError: AuthError? = null

    override val isAvailable = true
    override val currentUser: Flow<AuthUser?> = user
    override fun currentUserId() = user.value?.uid
    override suspend fun signInWithEmail(email: String, password: String): AuthError? = null
    override suspend fun signUpWithEmail(email: String, password: String): AuthError? = null
    override suspend fun sendPasswordReset(email: String): AuthError? = null
    override suspend fun signInWithGoogle(idToken: String, accessToken: String?): AuthError? = null
    override suspend fun signInWithApple(idToken: String, rawNonce: String): AuthError? = null
    override suspend fun signOut() {
        user.value = null
    }
    override suspend fun reauthenticateWithEmail(password: String): AuthError? = null
    override suspend fun reauthenticateWithGoogle(idToken: String, accessToken: String?): AuthError? = null
    override suspend fun reauthenticateWithApple(idToken: String, rawNonce: String): AuthError? = null
    override suspend fun deleteCurrentUser(): AuthError? = deleteError ?: run {
        user.value = null
        null
    }
}

class DeleteAccountTest {
    private val uid = "user-1"
    private val remote = FakeRemote()
    private val auth = FakeAuth(uid)
    private val clock = FixedClock(at("2026-10-24T09:00"))
    private val db = inMemoryDatabase()
    private val prefs = tempPreferences()
    private val expenses = ExpenseRepositoryImpl(db.expenseDao(), prefs, clock)
    private val categories = CategoryRepositoryImpl(db.categoryDao(), prefs, clock)
    private val engine = SyncEngine(db, remote, prefs, clock)

    private fun TestScope.repository() =
        SyncRepositoryImpl(auth, engine, remote, db, categories, prefs, clock, backgroundScope)

    @Test
    fun deletesCloudDataAccountAndLocalCopy() = runTest {
        val sync = repository()
        categories.ensureDefaults()
        expenses.save(expense("e1", 120, "2026-10-24T08:00"))
        engine.sync(uid)
        assertEquals(1, remote.expensesOf(uid).size)

        assertNull(sync.deleteAccount())

        assertTrue(remote.expensesOf(uid).isEmpty())
        assertTrue(remote.categoriesOf(uid).isEmpty())
        assertNull(auth.user.value)
        assertTrue(expenses.observeExpenses().first().isEmpty())
        // The app keeps working offline with fresh default categories.
        assertTrue(categories.observeCategories().first().isNotEmpty())
        assertNull(prefs.lastSyncedUid())
    }

    @Test
    fun keepsEverythingWhenCloudDeletionFails() = runTest {
        val sync = repository()
        expenses.save(expense("e1", 120, "2026-10-24T08:00"))
        engine.sync(uid)
        remote.failNextDelete = true

        assertIs<AuthError.Unknown>(sync.deleteAccount())

        assertNotNull(auth.user.value)
        assertEquals(1, remote.expensesOf(uid).size)
        assertEquals(listOf("e1"), expenses.observeExpenses().first().map { it.id })
    }

    @Test
    fun keepsLocalDataAndReuploadsWhenAccountDeletionFails() = runTest {
        val sync = repository()
        expenses.save(expense("e1", 120, "2026-10-24T08:00"))
        engine.sync(uid)
        auth.deleteError = AuthError.RequiresRecentLogin

        assertEquals(AuthError.RequiresRecentLogin, sync.deleteAccount())

        assertNotNull(auth.user.value)
        assertEquals(listOf("e1"), expenses.observeExpenses().first().map { it.id })
        // The cloud copy was already wiped, so the next sync must restore it from this device.
        assertTrue(remote.expensesOf(uid).isEmpty())
        engine.sync(uid)
        assertEquals(listOf("e1"), remote.expensesOf(uid).map { it.id })
    }
}
