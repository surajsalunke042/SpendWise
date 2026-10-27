package com.surajdev.spendwise.core.domain.repository

import com.surajdev.spendwise.core.domain.model.AuthUser
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.model.SyncStatus
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** Expenses, stored locally first. Writes are picked up by sync automatically. */
interface ExpenseRepository {
    /** Non-deleted expenses with `occurredAt` in [window], newest first. Optional text and category filters. */
    fun observeExpenses(
        window: ClosedRange<Instant>? = null,
        query: String = "",
        categoryIds: Set<String> = emptySet(),
    ): Flow<List<Expense>>

    fun observeExpense(id: String): Flow<Expense?>

    suspend fun getExpense(id: String): Expense?

    /** Time of the earliest non-deleted expense, or null when there are none. */
    fun observeEarliestExpenseTime(): Flow<Instant?>

    suspend fun save(expense: Expense)

    /** Soft-deletes so the deletion can sync and be undone. */
    suspend fun delete(id: String)

    suspend fun restore(id: String)
}

interface CategoryRepository {
    fun observeCategories(includeArchived: Boolean = false): Flow<List<Category>>

    suspend fun getCategory(id: String): Category?

    suspend fun save(category: Category)

    suspend fun setArchived(id: String, archived: Boolean)

    /** Inserts the built-in categories if they're missing. Safe to call on every start. */
    suspend fun ensureDefaults()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setReminder(enabled: Boolean, time: LocalTime)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun dismissReminderPrompt()

    suspend fun rememberLastUsed(categoryId: String, paymentMethod: PaymentMethod?)
}

sealed interface AuthError {
    data object InvalidCredentials : AuthError
    data object EmailAlreadyInUse : AuthError
    data object WeakPassword : AuthError
    data object InvalidEmail : AuthError
    data object Network : AuthError
    data object Cancelled : AuthError
    data object Unavailable : AuthError
    /** Firebase wants a fresh sign-in before a sensitive action such as deleting the account. */
    data object RequiresRecentLogin : AuthError
    data class Unknown(val message: String?) : AuthError
}

interface AuthRepository {
    /** False when Firebase isn't configured in this build; the app then runs offline-only. */
    val isAvailable: Boolean

    val currentUser: Flow<AuthUser?>

    fun currentUserId(): String?

    suspend fun signInWithEmail(email: String, password: String): AuthError?

    suspend fun signUpWithEmail(email: String, password: String): AuthError?

    suspend fun sendPasswordReset(email: String): AuthError?

    suspend fun signInWithGoogle(idToken: String, accessToken: String?): AuthError?

    suspend fun signInWithApple(idToken: String, rawNonce: String): AuthError?

    suspend fun signOut()

    /** Confirms the signed-in user's identity again; Firebase needs this right before deleting an account. */
    suspend fun reauthenticateWithEmail(password: String): AuthError?

    suspend fun reauthenticateWithGoogle(idToken: String, accessToken: String?): AuthError?

    suspend fun reauthenticateWithApple(idToken: String, rawNonce: String): AuthError?

    /** Deletes the signed-in user. Call [SyncRepository.deleteAccount] instead, which removes their data first. */
    suspend fun deleteCurrentUser(): AuthError?
}

interface SyncRepository {
    val status: Flow<SyncStatus>

    /** Runs a sync now. Safe to call concurrently; overlapping calls are serialized. */
    suspend fun syncNow(): Boolean

    /** Asks for a sync soon (debounced). */
    fun requestSync()

    /** Signs out, optionally deleting this device's local copy of the data. */
    suspend fun signOut(keepLocalData: Boolean)

    /**
     * Permanently deletes the user's cloud data, their account, and this device's copy of the data.
     * Re-authenticate just before calling this. Returns null on success.
     */
    suspend fun deleteAccount(): AuthError?
}
