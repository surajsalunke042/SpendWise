package com.surajdev.spendwise.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.domain.model.AuthUser
import com.surajdev.spendwise.core.domain.model.SyncStatus
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.domain.model.UserSettings
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val authAvailable: Boolean = false,
    val user: AuthUser? = null,
    val syncStatus: SyncStatus = SyncStatus.Unavailable,
)

sealed interface SettingsIntent {
    data class ReminderToggled(val enabled: Boolean) : SettingsIntent
    data class ReminderTimeChanged(val time: LocalTime) : SettingsIntent
    data class ThemeChanged(val mode: ThemeMode) : SettingsIntent
    data object SyncNow : SettingsIntent
    data class SignOut(val keepLocalData: Boolean) : SettingsIntent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    auth: AuthRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        auth.currentUser,
        syncRepository.status,
    ) { settings, user, sync ->
        SettingsUiState(settings, auth.isAvailable, user, sync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(authAvailable = auth.isAvailable))

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            val current = state.value.settings
            when (intent) {
                is SettingsIntent.ReminderToggled -> settingsRepository.setReminder(intent.enabled, current.reminderTime)
                is SettingsIntent.ReminderTimeChanged -> settingsRepository.setReminder(true, intent.time)
                is SettingsIntent.ThemeChanged -> settingsRepository.setThemeMode(intent.mode)
                SettingsIntent.SyncNow -> syncRepository.syncNow()
                is SettingsIntent.SignOut -> syncRepository.signOut(intent.keepLocalData)
            }
        }
    }
}
