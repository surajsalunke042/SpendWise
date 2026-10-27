package com.surajdev.spendwise.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.domain.model.AuthUser
import com.surajdev.spendwise.core.domain.model.SyncStatus
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.domain.model.UserSettings
import com.surajdev.spendwise.core.domain.repository.AuthError
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import com.surajdev.spendwise.core.platform.SocialSignInResult
import com.surajdev.spendwise.feature.auth.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val authAvailable: Boolean = false,
    val user: AuthUser? = null,
    val syncStatus: SyncStatus = SyncStatus.Unavailable,
    val deleteAccount: DeleteAccountState = DeleteAccountState(),
)

data class DeleteAccountState(
    val isBusy: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false,
)

sealed interface SettingsIntent {
    data class ReminderToggled(val enabled: Boolean) : SettingsIntent
    data class ReminderTimeChanged(val time: LocalTime) : SettingsIntent
    data class ThemeChanged(val mode: ThemeMode) : SettingsIntent
    data object SyncNow : SettingsIntent
    data class SignOut(val keepLocalData: Boolean) : SettingsIntent

    /** Account deletion: confirm identity with a password or the Google/Apple sheet, then delete. */
    data class DeleteAccountWithPassword(val password: String) : SettingsIntent
    data object DeleteAccountSocialStarted : SettingsIntent
    data class DeleteAccountWithSocial(val result: SocialSignInResult) : SettingsIntent
    /** For accounts whose provider can't sign in on this platform; works only if the sign-in is recent. */
    data object DeleteAccountWithoutReauth : SettingsIntent
    data object DeleteAccountDismissed : SettingsIntent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val deleteAccount = MutableStateFlow(DeleteAccountState())

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        auth.currentUser,
        syncRepository.status,
        deleteAccount,
    ) { settings, user, sync, deletion ->
        SettingsUiState(settings, auth.isAvailable, user, sync, deletion)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(authAvailable = auth.isAvailable))

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.DeleteAccountWithPassword -> {
                if (intent.password.isEmpty()) {
                    deleteAccount.update { it.copy(error = "Enter your password") }
                } else {
                    deleteAfter { auth.reauthenticateWithEmail(intent.password) }
                }
            }
            SettingsIntent.DeleteAccountSocialStarted -> deleteAccount.update { it.copy(isBusy = true, error = null) }
            is SettingsIntent.DeleteAccountWithSocial -> when (val result = intent.result) {
                is SocialSignInResult.Google -> deleteAfter { auth.reauthenticateWithGoogle(result.idToken, result.accessToken) }
                is SocialSignInResult.Apple -> deleteAfter { auth.reauthenticateWithApple(result.idToken, result.rawNonce) }
                SocialSignInResult.Cancelled -> deleteAccount.update { it.copy(isBusy = false) }
                is SocialSignInResult.Failed -> deleteAccount.update { it.copy(isBusy = false, error = result.message) }
            }
            SettingsIntent.DeleteAccountWithoutReauth -> deleteAfter { null }
            SettingsIntent.DeleteAccountDismissed -> deleteAccount.value = DeleteAccountState()
            else -> viewModelScope.launch {
                val current = state.value.settings
                when (intent) {
                    is SettingsIntent.ReminderToggled -> settingsRepository.setReminder(intent.enabled, current.reminderTime)
                    is SettingsIntent.ReminderTimeChanged -> settingsRepository.setReminder(true, intent.time)
                    is SettingsIntent.ThemeChanged -> settingsRepository.setThemeMode(intent.mode)
                    SettingsIntent.SyncNow -> syncRepository.syncNow()
                    is SettingsIntent.SignOut -> syncRepository.signOut(intent.keepLocalData)
                    else -> Unit
                }
            }
        }
    }

    /** Re-authenticates with [reauthenticate], then deletes the account and all its data. */
    private fun deleteAfter(reauthenticate: suspend () -> AuthError?) {
        viewModelScope.launch {
            deleteAccount.update { it.copy(isBusy = true, error = null) }
            val error = when (val reauthError = reauthenticate()) {
                null -> syncRepository.deleteAccount()
                AuthError.InvalidCredentials -> WrongAccount
                else -> reauthError
            }
            deleteAccount.value = when (error) {
                null -> DeleteAccountState(completed = true)
                AuthError.Cancelled -> DeleteAccountState()
                else -> DeleteAccountState(error = error.userMessage())
            }
        }
    }

    private companion object {
        val WrongAccount = AuthError.Unknown("That didn't match the account you're signed in with.")
    }
}
