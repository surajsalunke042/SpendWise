package com.surajdev.spendwise.feature.expenselist

import kotlinx.datetime.LocalDate
import com.surajdev.spendwise.core.domain.model.DaySection
import com.surajdev.spendwise.core.designsystem.component.SurfaceCard
import com.surajdev.spendwise.core.designsystem.component.LocalBottomBarClearance
import com.surajdev.spendwise.core.designsystem.component.AppFab
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.surajdev.spendwise.core.designsystem.component.appTextFieldColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surajdev.spendwise.core.designsystem.component.AmountText
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.EmptyState
import com.surajdev.spendwise.core.designsystem.component.HairlineDivider
import com.surajdev.spendwise.core.designsystem.component.SectionLabel
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.domain.model.ExpenseItem
import com.surajdev.spendwise.feature.addexpense.chipBorder
import com.surajdev.spendwise.feature.addexpense.chipColors
import com.surajdev.spendwise.feature.common.DateRangePickerSheet
import com.surajdev.spendwise.feature.common.ExpenseRow
import com.surajdev.spendwise.feature.common.Formats
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpenseListRoute(
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    viewModel: ExpenseListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExpenseListEffect.Deleted -> launch {
                    val result = snackbar.showSnackbar("Expense deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onIntent(ExpenseListIntent.UndoDelete(effect.id))
                }
            }
        }
    }
    ExpenseListScreen(state, snackbar, viewModel::onIntent, onAddExpense, onOpenExpense)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    state: ExpenseListUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ExpenseListIntent) -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
) {
    var showRangePicker by rememberSaveable { mutableStateOf(false) }
    val bottomClearance = LocalBottomBarClearance.current

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.padding(bottom = bottomClearance)) },
        floatingActionButton = {
            AppFab(
                onClick = onAddExpense,
                icon = AppIcons.Add,
                contentDescription = "Add expense",
                modifier = Modifier.padding(bottom = bottomClearance),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Expenses",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.lg),
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(ExpenseListIntent.QueryChanged(it)) },
                placeholder = { Text("Search notes and categories") },
                leadingIcon = { AppIcon(AppIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = if (state.query.isNotEmpty()) {
                    { IconButton(onClick = { onIntent(ExpenseListIntent.QueryChanged("")) }) { AppIcon(AppIcons.Close, "Clear search") } }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = MaterialTheme.shapes.small,
                colors = appTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.md),
            )
            FilterRow(state, onIntent, onCustomRange = { showRangePicker = true })

            when {
                state.isLoading -> Unit
                state.sections.isEmpty() && state.hasFilters -> EmptyState(
                    icon = AppIcons.Search,
                    title = "No matches",
                    message = "Try a different search or clear the filters.",
                    action = { TextButton(onClick = { onIntent(ExpenseListIntent.ClearFilters) }) { Text("Clear filters") } },
                )
                state.sections.isEmpty() -> EmptyState(
                    icon = AppIcons.Receipt,
                    title = "No expenses yet",
                    message = "Everything you log will appear here, grouped by day.",
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = bottomClearance + 96.dp)) {
                    state.sections.forEach { section ->
                        stickyHeader(key = "header-${section.date}") {
                            DayHeader(section, state.today)
                        }
                        item(key = "day-${section.date}") {
                            SurfaceCard(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(bottom = Spacing.sm),
                                contentPadding = 0.dp,
                            ) {
                                section.items.forEachIndexed { index, item ->
                                    key(item.expense.id) {
                                        SwipeToDeleteRow(item, onDelete = { onIntent(ExpenseListIntent.Delete(item.expense.id)) }) {
                                            onOpenExpense(item.expense.id)
                                        }
                                    }
                                    if (index < section.items.lastIndex) HairlineDivider(Modifier.padding(start = 68.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRangePicker) {
        DateRangePickerSheet(
            initial = state.customRange,
            maxDate = state.today,
            onDismiss = { showRangePicker = false },
            onConfirm = {
                onIntent(ExpenseListIntent.CustomRangeSelected(it))
                showRangePicker = false
            },
        )
    }
}

@Composable
private fun FilterRow(state: ExpenseListUiState, onIntent: (ExpenseListIntent) -> Unit, onCustomRange: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .padding(bottom = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box {
            val dateLabel = when (state.datePreset) {
                DatePreset.CUSTOM -> state.customRange?.let { "${Formats.dayMonth(it.start)} – ${Formats.dayMonth(it.endInclusive)}" } ?: "Custom"
                else -> state.datePreset.label
            }
            FilterChip(
                selected = state.datePreset != DatePreset.ALL,
                onClick = { menuOpen = true },
                label = { Text(dateLabel) },
                leadingIcon = { AppIcon(AppIcons.Calendar, null, size = 18.dp) },
                trailingIcon = { AppIcon(AppIcons.ExpandMore, null, size = 18.dp) },
                shape = MaterialTheme.shapes.small,
                colors = chipColors(),
                border = chipBorder(state.datePreset != DatePreset.ALL),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DatePreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(if (preset == DatePreset.CUSTOM) "Custom range…" else preset.label) },
                        onClick = {
                            menuOpen = false
                            if (preset == DatePreset.CUSTOM) onCustomRange() else onIntent(ExpenseListIntent.PresetSelected(preset))
                        },
                    )
                }
            }
        }
        state.categories.forEach { category ->
            val selected = category.id in state.selectedCategoryIds
            FilterChip(
                selected = selected,
                onClick = { onIntent(ExpenseListIntent.CategoryToggled(category.id)) },
                label = { Text(category.name) },
                leadingIcon = { AppIcon(AppIcons.forKey(category.iconKey), null, size = 18.dp) },
                shape = MaterialTheme.shapes.small,
                colors = chipColors(),
                border = chipBorder(selected),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
    }
}

@Composable
private fun SwipeToDeleteRow(item: ExpenseItem, onDelete: () -> Unit, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                scope.launch { dismissState.reset() }
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = Spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AppIcon(AppIcons.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        // Opaque so the delete background only shows while swiping.
        ExpenseRow(item, modifier = Modifier.background(MaterialTheme.colorScheme.surface), onClick = onClick)
    }
}

/** Sticky day label, drawn as a small frosted pill so it reads over the cards scrolling beneath it. */
@Composable
private fun DayHeader(section: DaySection, today: LocalDate) {
    val extended = SpendWiseTheme.extended
    Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm)) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (extended.vivid) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, extended.cardBorder, CircleShape)
                .padding(horizontal = Spacing.md, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionLabel(Formats.dayLabel(section.date, today), color = MaterialTheme.colorScheme.onSurface)
            AmountText(
                section.total,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                showFraction = false,
            )
        }
    }
}
