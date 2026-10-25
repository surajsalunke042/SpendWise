package com.surajdev.spendwise.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppViewModel(
    settings: SettingsRepository,
    private val sync: SyncRepository,
) : ViewModel() {

    /** Null until settings load, so the first frame doesn't flash the wrong theme. */
    val themeMode: StateFlow<ThemeMode?> = settings.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onForeground() = sync.requestSync()
}
