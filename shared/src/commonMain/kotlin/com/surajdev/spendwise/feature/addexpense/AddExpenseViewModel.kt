package com.surajdev.spendwise.feature.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surajdev.spendwise.core.common.Outcome
import com.surajdev.spendwise.core.common.TimeZoneProvider
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.MoneyFormatter
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.ExpenseRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.usecase.ExpenseDraft
import com.surajdev.spendwise.core.domain.usecase.ExpenseValidationError
import com.surajdev.spendwise.core.domain.usecase.SaveExpenseUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Navigation argument; null [expenseId] means a new expense. */
data class AddExpenseArgs(val expenseId: String?)

data class AddExpenseUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val amountText: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val note: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val today: LocalDate,
    val paymentMethod: PaymentMethod? = null,
    val amountError: String? = null,
    val categoryError: String? = null,
    val noteError: String? = null,
    val dateError: String? = null,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && !isSaving && amountText.isNotBlank() && selectedCategoryId != null
}

sealed interface AddExpenseIntent {
    data class AmountChanged(val text: String) : AddExpenseIntent
    data class CategorySelected(val id: String) : AddExpenseIntent
    data class NoteChanged(val text: String) : AddExpenseIntent
    data class DateChanged(val date: LocalDate) : AddExpenseIntent
    data class TimeChanged(val time: LocalTime) : AddExpenseIntent
    data class PaymentMethodToggled(val method: PaymentMethod) : AddExpenseIntent
    data object Save : AddExpenseIntent
    data object Delete : AddExpenseIntent
}

sealed interface AddExpenseEffect {
    data object Saved : AddExpenseEffect
    data object Deleted : AddExpenseEffect
    data object NotFound : AddExpenseEffect
}

class AddExpenseViewModel(
    private val args: AddExpenseArgs,
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val saveExpense: SaveExpenseUseCase,
    clock: Clock,
    timeZones: TimeZoneProvider,
) : ViewModel() {

    private val now = clock.now().toLocalDateTime(timeZones.current())
    private val _state = MutableStateFlow(
        AddExpenseUiState(isEditing = args.expenseId != null, date = now.date, time = LocalTime(now.hour, now.minute), today = now.date),
    )
    val state: StateFlow<AddExpenseUiState> = _state.asStateFlow()

    private val _effects = Channel<AddExpenseEffect>(Channel.BUFFERED)
    val effects: Flow<AddExpenseEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch {
            categories.observeCategories().collect { list -> _state.update { it.copy(categories = list) } }
        }
    }

    private suspend fun load() {
        val id = args.expenseId
        if (id != null) {
            val expense = expenses.getExpense(id)
            if (expense == null) {
                _effects.send(AddExpenseEffect.NotFound)
                return
            }
            val local = expense.localDateTime
            _state.update {
                it.copy(
                    isLoading = false,
                    amountText = MoneyFormatter.toInputText(expense.amount),
                    selectedCategoryId = expense.categoryId,
                    note = expense.note,
                    date = local.date,
                    time = LocalTime(local.hour, local.minute),
                    paymentMethod = expense.paymentMethod,
                )
            }
        } else {
            val prefs = settings.settings.first()
            val active = categories.observeCategories().first()
            val defaultCategory = prefs.lastUsedCategoryId?.takeIf { last -> active.any { it.id == last } }
                ?: active.firstOrNull()?.id
            _state.update {
                it.copy(isLoading = false, selectedCategoryId = defaultCategory, paymentMethod = prefs.lastUsedPaymentMethod)
            }
        }
    }

    fun onIntent(intent: AddExpenseIntent) {
        when (intent) {
            is AddExpenseIntent.AmountChanged -> sanitizeAmount(intent.text)?.let { text ->
                _state.update { it.copy(amountText = text, amountError = null) }
            }
            is AddExpenseIntent.CategorySelected -> _state.update { it.copy(selectedCategoryId = intent.id, categoryError = null) }
            is AddExpenseIntent.NoteChanged -> _state.update { it.copy(note = intent.text, noteError = null) }
            is AddExpenseIntent.DateChanged -> _state.update { it.copy(date = intent.date, dateError = null) }
            is AddExpenseIntent.TimeChanged -> _state.update { it.copy(time = intent.time, dateError = null) }
            is AddExpenseIntent.PaymentMethodToggled -> _state.update {
                it.copy(paymentMethod = if (it.paymentMethod == intent.method) null else intent.method)
            }
            AddExpenseIntent.Save -> save()
            AddExpenseIntent.Delete -> delete()
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val draft = ExpenseDraft(
                id = args.expenseId,
                amount = Money.parse(current.amountText),
                categoryId = current.selectedCategoryId,
                note = current.note,
                occurredAt = LocalDateTime(current.date, current.time),
                paymentMethod = current.paymentMethod,
            )
            when (val result = saveExpense(draft)) {
                is Outcome.Success -> _effects.send(AddExpenseEffect.Saved)
                is Outcome.Failure -> _state.update { it.withError(result.error) }
            }
            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun delete() {
        val id = args.expenseId ?: return
        viewModelScope.launch {
            expenses.delete(id)
            _effects.send(AddExpenseEffect.Deleted)
        }
    }

    private fun AddExpenseUiState.withError(error: ExpenseValidationError) = when (error) {
        ExpenseValidationError.AMOUNT_MISSING -> copy(amountError = "Enter an amount")
        ExpenseValidationError.AMOUNT_NOT_POSITIVE -> copy(amountError = "Amount must be more than zero")
        ExpenseValidationError.AMOUNT_TOO_LARGE -> copy(amountError = "That's more than ₹1 crore. Check the amount.")
        ExpenseValidationError.CATEGORY_MISSING -> copy(categoryError = "Choose a category")
        ExpenseValidationError.NOTE_TOO_LONG -> copy(noteError = "Keep the note under ${SaveExpenseUseCase.MAX_NOTE_LENGTH} characters")
        ExpenseValidationError.DATE_IN_FUTURE -> copy(dateError = "The date can't be in the future")
        ExpenseValidationError.NOT_FOUND -> copy(amountError = "This expense no longer exists")
    }

    companion object {
        /** Allows digits with at most one decimal point and two decimals. Returns null to reject the edit. */
        fun sanitizeAmount(input: String): String? {
            val text = input.replace(',', '.').filter { it.isDigit() || it == '.' }
            if (text.count { it == '.' } > 1) return null
            val whole = text.substringBefore('.')
            val fraction = text.substringAfter('.', missingDelimiterValue = "")
            if (whole.length > 9 || fraction.length > 2) return null
            return text
        }
    }
}
