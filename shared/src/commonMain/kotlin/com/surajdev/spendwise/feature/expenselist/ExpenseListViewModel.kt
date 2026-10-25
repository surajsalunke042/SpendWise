package com.surajdev.spendwise.feature.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.common.TimeZoneProvider
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.DateRange
import com.surajdev.spendwise.core.domain.model.DaySection
import com.surajdev.spendwise.core.domain.model.ExpenseFilter
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.ExpenseRepository
import com.surajdev.spendwise.core.domain.time.DayMath
import com.surajdev.spendwise.core.domain.time.DayMath.minusDays
import com.surajdev.spendwise.core.domain.usecase.ObserveExpenseSectionsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

enum class DatePreset(val label: String) { ALL("All time"), WEEK("This week"), MONTH("This month"), LAST_30("Last 30 days"), CUSTOM("Custom") }

data class ExpenseListUiState(
    val query: String = "",
    val selectedCategoryIds: Set<String> = emptySet(),
    val datePreset: DatePreset = DatePreset.ALL,
    val customRange: DateRange? = null,
    val categories: List<Category> = emptyList(),
    val sections: List<DaySection> = emptyList(),
    val isLoading: Boolean = true,
    val today: LocalDate,
) {
    val hasFilters: Boolean get() = query.isNotBlank() || selectedCategoryIds.isNotEmpty() || datePreset != DatePreset.ALL
}

sealed interface ExpenseListIntent {
    data class QueryChanged(val query: String) : ExpenseListIntent
    data class CategoryToggled(val id: String) : ExpenseListIntent
    data class PresetSelected(val preset: DatePreset) : ExpenseListIntent
    data class CustomRangeSelected(val range: DateRange) : ExpenseListIntent
    data object ClearFilters : ExpenseListIntent
    data class Delete(val id: String) : ExpenseListIntent
    data class UndoDelete(val id: String) : ExpenseListIntent
}

sealed interface ExpenseListEffect {
    data class Deleted(val id: String) : ExpenseListEffect
}

private data class Filters(
    val query: String = "",
    val categoryIds: Set<String> = emptySet(),
    val preset: DatePreset = DatePreset.ALL,
    val customRange: DateRange? = null,
)

class ExpenseListViewModel(
    private val observeSections: ObserveExpenseSectionsUseCase,
    private val expenses: ExpenseRepository,
    categories: CategoryRepository,
    private val clock: Clock,
    private val timeZones: TimeZoneProvider,
) : ViewModel() {

    private val filters = MutableStateFlow(Filters())
    private val _effects = Channel<ExpenseListEffect>(Channel.BUFFERED)
    val effects: Flow<ExpenseListEffect> = _effects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val sections: Flow<List<DaySection>> = filters
        .debounce(150.milliseconds)
        .map { it.toExpenseFilter() }
        .distinctUntilChanged()
        .flatMapLatest { observeSections(it) }

    val state: StateFlow<ExpenseListUiState> = combine(
        filters,
        categories.observeCategories(),
        sections,
    ) { f, categoryList, sectionList ->
        ExpenseListUiState(
            query = f.query,
            selectedCategoryIds = f.categoryIds,
            datePreset = f.preset,
            customRange = f.customRange,
            categories = categoryList,
            sections = sectionList,
            isLoading = false,
            today = today(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseListUiState(today = today()))

    fun onIntent(intent: ExpenseListIntent) {
        when (intent) {
            is ExpenseListIntent.QueryChanged -> filters.update { it.copy(query = intent.query) }
            is ExpenseListIntent.CategoryToggled -> filters.update {
                it.copy(categoryIds = if (intent.id in it.categoryIds) it.categoryIds - intent.id else it.categoryIds + intent.id)
            }
            is ExpenseListIntent.PresetSelected -> filters.update { it.copy(preset = intent.preset) }
            is ExpenseListIntent.CustomRangeSelected -> filters.update { it.copy(preset = DatePreset.CUSTOM, customRange = intent.range) }
            ExpenseListIntent.ClearFilters -> filters.value = Filters()
            is ExpenseListIntent.Delete -> viewModelScope.launch {
                expenses.delete(intent.id)
                _effects.send(ExpenseListEffect.Deleted(intent.id))
            }
            is ExpenseListIntent.UndoDelete -> viewModelScope.launch { expenses.restore(intent.id) }
        }
    }

    private fun Filters.toExpenseFilter(): ExpenseFilter {
        val today = today()
        val range = when (preset) {
            DatePreset.ALL -> null
            DatePreset.WEEK -> DateRange(today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber)), today)
            DatePreset.MONTH -> DateRange(LocalDate(today.year, today.month, 1), today)
            DatePreset.LAST_30 -> DateRange(today.minusDays(29), today)
            DatePreset.CUSTOM -> customRange
        }
        return ExpenseFilter(query = query, categoryIds = categoryIds, dateRange = range)
    }

    private fun today(): LocalDate = DayMath.today(clock, timeZones.current())
}
