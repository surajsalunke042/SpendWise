package com.surajdev.spendwise.feature.addexpense

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.surajdev.spendwise.core.designsystem.component.appTextFieldColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.CategoryBadge
import com.surajdev.spendwise.core.designsystem.component.PrimaryButton
import com.surajdev.spendwise.core.designsystem.component.SectionLabel
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.designsystem.theme.categoryColor
import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.Currency
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.feature.common.DatePickerSheet
import com.surajdev.spendwise.feature.common.Formats
import com.surajdev.spendwise.feature.common.TimePickerSheet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddExpenseRoute(
    expenseId: String?,
    onClose: () -> Unit,
    onManageCategories: () -> Unit,
    viewModel: AddExpenseViewModel = koinViewModel(key = "add-expense-$expenseId") { parametersOf(AddExpenseArgs(expenseId)) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddExpenseEffect.Saved, AddExpenseEffect.Deleted, AddExpenseEffect.NotFound -> onClose()
            }
        }
    }
    AddExpenseScreen(state, viewModel::onIntent, onClose, onManageCategories)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    state: AddExpenseUiState,
    onIntent: (AddExpenseIntent) -> Unit,
    onClose: () -> Unit,
    onManageCategories: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && !state.isEditing) amountFocus.requestFocus()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit expense" else "New expense", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onClose) { AppIcon(AppIcons.Close, contentDescription = "Close") }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { confirmDelete = true }) {
                            AppIcon(AppIcons.Delete, contentDescription = "Delete expense", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            Surface(color = Color.Transparent) {
                PrimaryButton(
                    text = if (state.isEditing) "Save changes" else "Save expense",
                    onClick = { onIntent(AddExpenseIntent.Save) },
                    enabled = state.canSave,
                    loading = state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = Spacing.screen, vertical = Spacing.md),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen),
        ) {
            AmountField(
                text = state.amountText,
                error = state.amountError,
                onChange = { onIntent(AddExpenseIntent.AmountChanged(it)) },
                modifier = Modifier.focusRequester(amountFocus),
            )

            Spacer(Modifier.height(Spacing.xl))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Category", Modifier.weight(1f))
                TextButton(onClick = onManageCategories) { Text("Manage") }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                state.categories.forEach { category ->
                    CategoryChip(category, selected = category.id == state.selectedCategoryId) {
                        onIntent(AddExpenseIntent.CategorySelected(category.id))
                    }
                }
            }
            state.categoryError?.let { ErrorText(it) }

            Spacer(Modifier.height(Spacing.xl))
            SectionLabel("Note")
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = state.note,
                onValueChange = { onIntent(AddExpenseIntent.NoteChanged(it)) },
                placeholder = { Text("What was it for? (optional)") },
                singleLine = true,
                isError = state.noteError != null,
                supportingText = state.noteError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (state.canSave) onIntent(AddExpenseIntent.Save) }),
                shape = MaterialTheme.shapes.small,
                colors = appTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.lg))
            SectionLabel("When")
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PickerChip(AppIcons.Calendar, Formats.dayLabel(state.date, state.today)) { showDatePicker = true }
                PickerChip(AppIcons.Time, Formats.time(state.time)) { showTimePicker = true }
            }
            state.dateError?.let { ErrorText(it) }

            Spacer(Modifier.height(Spacing.xl))
            SectionLabel("Paid with (optional)")
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = state.paymentMethod == method,
                        onClick = { onIntent(AddExpenseIntent.PaymentMethodToggled(method)) },
                        label = { Text(method.label) },
                        leadingIcon = { AppIcon(AppIcons.forKey(method.iconKey), contentDescription = null, size = 18.dp) },
                        shape = MaterialTheme.shapes.small,
                        colors = chipColors(),
                        border = chipBorder(state.paymentMethod == method),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initial = state.date,
            maxDate = state.today,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onIntent(AddExpenseIntent.DateChanged(it))
                showDatePicker = false
            },
        )
    }
    if (showTimePicker) {
        TimePickerSheet(
            initial = state.time,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                onIntent(AddExpenseIntent.TimeChanged(it))
                showTimePicker = false
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this expense?") },
            text = { Text("It will be removed from your reports on all your devices.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onIntent(AddExpenseIntent.Delete)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AmountField(text: String, error: String?, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(Modifier.fillMaxWidth().padding(top = Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
        SectionLabel("Amount")
        Spacer(Modifier.height(Spacing.sm))
        val style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        // The ₹ and the digits stay together, centered as one unit.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text(Currency.Default.symbol, style = style, color = muted)
            Spacer(Modifier.width(Spacing.xs))
            BasicTextField(
                value = text,
                onValueChange = onChange,
                textStyle = style,
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = modifier.width(IntrinsicSize.Min).widthIn(min = 32.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("0", style = style, color = muted.copy(alpha = 0.5f))
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .height(if (error != null) 2.dp else 1.dp)
                .background(if (error != null) MaterialTheme.colorScheme.error else SpendWiseTheme.extended.hairline),
        )
        error?.let { ErrorText(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val color = categoryColor(category.colorArgb, SpendWiseTheme.extended.isDark)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) color else SpendWiseTheme.extended.hairline),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.xs, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(category.iconKey, category.colorArgb, size = 30.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(category.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PickerChip(icon: org.jetbrains.compose.resources.DrawableResource, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SpendWiseTheme.extended.hairline),
    ) {
        Row(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon, contentDescription = null, size = 18.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.sm))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

@Composable
internal fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = if (SpendWiseTheme.extended.vivid) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
internal fun chipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = SpendWiseTheme.extended.hairline,
    selectedBorderColor = MaterialTheme.colorScheme.primary,
)
