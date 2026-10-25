package com.surajdev.spendwise.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.common.TimeZoneProvider
import com.surajdev.spendwise.core.domain.model.DailyReport
import com.surajdev.spendwise.core.domain.model.SyncStatus
import com.surajdev.spendwise.core.domain.model.UserSettings
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import com.surajdev.spendwise.core.domain.time.DayMath
import com.surajdev.spendwise.core.domain.usecase.ObserveDailyReportUseCase
import com.surajdev.spendwise.feature.common.Formats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class HomeUiState(
    val today: LocalDate,
    val greeting: String,
    val report: DailyReport? = null,
    val showReminderPrompt: Boolean = false,
    val reminderTimeLabel: String = "",
    val syncStatus: SyncStatus = SyncStatus.Unavailable,
)

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object EnableReminder : HomeIntent
    data object DismissReminderPrompt : HomeIntent
}

class HomeViewModel(
    private val observeDailyReport: ObserveDailyReportUseCase,
    private val settingsRepository: SettingsRepository,
    syncRepository: SyncRepository,
    private val clock: Clock,
    private val timeZones: TimeZoneProvider,
) : ViewModel() {

    private val today = MutableStateFlow(currentDate())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = combine(
        today.flatMapLatest { date -> observeDailyReport(date) },
        settingsRepository.settings,
        syncRepository.status,
    ) { report, settings, sync ->
        HomeUiState(
            today = report.date,
            greeting = greeting(),
            report = report,
            showReminderPrompt = !settings.reminderEnabled && !settings.reminderPromptDismissed,
            reminderTimeLabel = Formats.time(settings.reminderTime),
            syncStatus = sync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(today.value, greeting()))

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            // Picks up a new day if the app stayed open past midnight.
            HomeIntent.Refresh -> today.value = currentDate()
            HomeIntent.EnableReminder -> viewModelScope.launch {
                settingsRepository.setReminder(enabled = true, time = UserSettings.DEFAULT_REMINDER_TIME)
            }
            HomeIntent.DismissReminderPrompt -> viewModelScope.launch { settingsRepository.dismissReminderPrompt() }
        }
    }

    private fun currentDate(): LocalDate = DayMath.today(clock, timeZones.current())

    private fun greeting(): String =
        Formats.greeting(clock.now().toLocalDateTime(timeZones.current()).hour)
}
