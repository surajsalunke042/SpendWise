package com.surajdev.spendwise.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.common.TimeZoneProvider
import com.surajdev.spendwise.core.domain.model.DailyReport
import com.surajdev.spendwise.core.domain.time.DayMath
import com.surajdev.spendwise.core.domain.time.DayMath.nextDay
import com.surajdev.spendwise.core.domain.time.DayMath.previousDay
import com.surajdev.spendwise.core.domain.usecase.ObserveDailyReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

data class ReportArgs(val initialDate: LocalDate?)

data class ReportUiState(
    val date: LocalDate,
    val today: LocalDate,
    val report: DailyReport? = null,
) {
    val canGoForward: Boolean get() = date < today
}

sealed interface ReportIntent {
    data object PreviousDay : ReportIntent
    data object NextDay : ReportIntent
    data class DateSelected(val date: LocalDate) : ReportIntent
    data object JumpToToday : ReportIntent
}

class ReportViewModel(
    args: ReportArgs,
    private val observeDailyReport: ObserveDailyReportUseCase,
    private val clock: Clock,
    private val timeZones: TimeZoneProvider,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(args.initialDate ?: today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ReportUiState> = selectedDate
        .flatMapLatest { date -> observeDailyReport(date).map { ReportUiState(date, today(), it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState(selectedDate.value, today()))

    fun onIntent(intent: ReportIntent) {
        val today = today()
        when (intent) {
            ReportIntent.PreviousDay -> selectedDate.update { it.previousDay() }
            ReportIntent.NextDay -> selectedDate.update { if (it < today) it.nextDay() else it }
            is ReportIntent.DateSelected -> selectedDate.value = minOf(intent.date, today)
            ReportIntent.JumpToToday -> selectedDate.value = today
        }
    }

    private fun today(): LocalDate = DayMath.today(clock, timeZones.current())
}
