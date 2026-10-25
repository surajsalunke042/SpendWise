package com.surajdev.spendwise.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surajdev.spendwise.core.designsystem.chart.BarChart
import com.surajdev.spendwise.core.designsystem.component.AppBackground
import com.surajdev.spendwise.core.designsystem.component.AppFab
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.HeroCard
import com.surajdev.spendwise.core.designsystem.component.LocalBottomBarClearance
import com.surajdev.spendwise.core.designsystem.component.ChangeIndicator
import com.surajdev.spendwise.core.designsystem.component.SurfaceCard
import com.surajdev.spendwise.core.designsystem.component.HairlineDivider
import com.surajdev.spendwise.core.designsystem.component.SectionLabel
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.domain.model.DailyReport
import com.surajdev.spendwise.core.domain.model.DefaultCategories
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.ExpenseItem
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.MoneyFormatter
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.model.SyncStatus
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.platform.rememberNotificationPermissionRequester
import com.surajdev.spendwise.feature.common.ExpenseRow
import com.surajdev.spendwise.feature.common.Formats
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun HomeRoute(
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(HomeIntent.Refresh)
        onPauseOrDispose { }
    }
    val requestPermission = rememberNotificationPermissionRequester { granted ->
        if (granted) viewModel.onIntent(HomeIntent.EnableReminder) else viewModel.onIntent(HomeIntent.DismissReminderPrompt)
    }
    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onEnableReminder = requestPermission,
        onAddExpense = onAddExpense,
        onOpenExpense = onOpenExpense,
        onOpenReport = onOpenReport,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onEnableReminder: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val bottomClearance = LocalBottomBarClearance.current
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            AppFab(
                onClick = onAddExpense,
                icon = AppIcons.Add,
                contentDescription = null,
                text = "Add expense",
                modifier = Modifier.padding(bottom = bottomClearance),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = bottomClearance + 96.dp),
        ) {
            item {
                Header(state, onOpenSettings)
            }
            item {
                TodayCard(state.report, onOpenReport, Modifier.padding(horizontal = Spacing.screen))
            }
            if (state.showReminderPrompt) {
                item {
                    ReminderPrompt(
                        timeLabel = state.reminderTimeLabel,
                        onEnable = onEnableReminder,
                        onDismiss = { onIntent(HomeIntent.DismissReminderPrompt) },
                        modifier = Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.md),
                    )
                }
            }
            state.report?.let { report ->
                item {
                    WeekCard(report, Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.md))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.sm, top = Spacing.xl),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("Today's entries", Modifier.weight(1f))
                        if (report.entries.isNotEmpty()) {
                            TextButton(onClick = onOpenReport) { Text("Daily report") }
                        }
                    }
                }
                if (report.entries.isEmpty()) {
                    item {
                        Text(
                            "Nothing logged yet today. Tap \"Add expense\" when you spend.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md),
                        )
                    }
                } else {
                    item {
                        SurfaceCard(
                            modifier = Modifier.padding(horizontal = Spacing.screen).fillMaxWidth(),
                            contentPadding = 0.dp,
                        ) {
                            val entries = report.entries.sortedByDescending { it.expense.occurredAt }
                            entries.forEachIndexed { index, item ->
                                ExpenseRow(item, onClick = { onOpenExpense(item.expense.id) })
                                if (index < entries.lastIndex) HairlineDivider(Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: HomeUiState, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.sm, top = Spacing.lg, bottom = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(state.greeting, style = MaterialTheme.typography.headlineMedium)
            Text(
                Formats.longDate(state.today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SyncIndicator(state.syncStatus, onOpenSettings)
    }
}

@Composable
private fun SyncIndicator(status: SyncStatus, onClick: () -> Unit) {
    val (icon, description) = when (status) {
        SyncStatus.Unavailable -> return
        SyncStatus.SignedOut -> AppIcons.CloudOff to "Not backed up. Sign in to sync."
        SyncStatus.Syncing -> AppIcons.Sync to "Syncing"
        is SyncStatus.Failed -> AppIcons.CloudOff to "Sync paused, will retry"
        is SyncStatus.Idle -> if (status.pendingChanges > 0) AppIcons.Sync to "Changes waiting to sync" else AppIcons.CloudDone to "All changes synced"
    }
    IconButton(onClick = onClick) {
        AppIcon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodayCard(report: DailyReport?, onOpenReport: () -> Unit, modifier: Modifier = Modifier) {
    val extended = SpendWiseTheme.extended
    HeroCard(modifier = modifier.fillMaxWidth(), onClick = onOpenReport) {
        SectionLabel("Spent today", color = extended.onHeroMuted)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = report?.let { MoneyFormatter.format(it.total, showFraction = null) } ?: "—",
            style = MaterialTheme.typography.displayLarge,
            color = extended.onHero,
        )
        Spacer(Modifier.height(Spacing.sm))
        if (report != null) {
            val count = report.entries.size
            Text(
                text = if (count == 0) "No expenses yet" else "$count ${if (count == 1) "expense" else "expenses"}",
                style = MaterialTheme.typography.bodyMedium,
                color = extended.onHeroMuted,
            )
            // Comparing an empty day with the average isn't useful, so wait until something is logged.
            if (report.trailingAverage != null && !report.total.isZero) {
                Spacer(Modifier.height(Spacing.sm))
                ChangeIndicator(report.changeVsAverage, "your 30-day average", onHero = true)
            }
        }
    }
}

@Composable
private fun WeekCard(report: DailyReport, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Last 7 days", Modifier.weight(1f))
            val weekTotal = report.lastSevenDays.fold(0L) { acc, (_, money) -> acc + money.minorUnits }
            Text(
                MoneyFormatter.format(report.total.copy(minorUnits = weekTotal), showFraction = false),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        BarChart(
            values = report.lastSevenDays.map { it.second.minorUnits.toFloat() },
            labels = report.lastSevenDays.map { Formats.weekdayShort(it.first.dayOfWeek).take(1) },
            valueLabel = { index -> MoneyFormatter.format(report.lastSevenDays[index].second, showFraction = false) },
            contentDescription = "Spending over the last 7 days: " + report.lastSevenDays.joinToString { (date, money) ->
                "${Formats.weekdayLong(date.dayOfWeek)} ${MoneyFormatter.format(money, showFraction = false)}"
            },
            highlightIndex = report.lastSevenDays.lastIndex,
            height = 120.dp,
        )
    }
}

@Composable
private fun ReminderPrompt(
    timeLabel: String,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            AppIcon(AppIcons.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("A gentle nudge at $timeLabel", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Get one reminder each evening to log the day's spending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextButton(onClick = onDismiss) { Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = onEnable) { Text("Turn on") }
            }
        }
    }
}

private val PreviewDate = LocalDate(2026, 10, 24)

private fun previewExpense(id: String, rupees: Long, categoryIndex: Int, note: String, time: String, method: PaymentMethod): ExpenseItem {
    val at = Instant.parse("${PreviewDate}T$time:00+05:30")
    val category = DefaultCategories.all[categoryIndex]
    return ExpenseItem(
        expense = Expense(
            id = id,
            amount = Money(rupees * 100),
            categoryId = category.id,
            note = note,
            occurredAt = at,
            timeZoneId = "Asia/Kolkata",
            paymentMethod = method,
            createdAt = at,
            updatedAt = at,
        ),
        category = category,
    )
}

private fun previewReport(entries: List<ExpenseItem>, weekRupees: List<Long>, averageRupees: Long?) = DailyReport(
    date = PreviewDate,
    total = Money(entries.sumOf { it.expense.amount.minorUnits }),
    entries = entries,
    categoryBreakdown = emptyList(),
    hourlyTotals = List(24) { Money.zero() },
    largestExpense = entries.maxByOrNull { it.expense.amount.minorUnits },
    previousDayTotal = Money(weekRupees[weekRupees.lastIndex - 1] * 100),
    trailingAverage = averageRupees?.let { Money(it * 100) },
    lastSevenDays = weekRupees.mapIndexed { i, rupees ->
        PreviewDate.minus(DatePeriod(days = weekRupees.lastIndex - i)) to Money(rupees * 100)
    },
)

private val PopulatedPreviewState = HomeUiState(
    today = PreviewDate,
    greeting = "Good evening",
    report = previewReport(
        entries = listOf(
            previewExpense("1", 120, 0, "Breakfast", "08:40", PaymentMethod.UPI),
            previewExpense("2", 250, 1, "Auto to office", "09:15", PaymentMethod.CASH),
            previewExpense("3", 1_499, 2, "Running shoes", "18:30", PaymentMethod.CARD),
        ),
        weekRupees = listOf(420, 980, 310, 0, 1_250, 560, 1_869),
        averageRupees = 650,
    ),
    syncStatus = SyncStatus.Idle(lastSyncedAt = null, pendingChanges = 0),
)

private val EmptyPreviewState = HomeUiState(
    today = PreviewDate,
    greeting = "Good morning",
    report = previewReport(entries = emptyList(), weekRupees = List(7) { 0L }, averageRupees = null),
    showReminderPrompt = true,
    reminderTimeLabel = "9:00 PM",
    syncStatus = SyncStatus.SignedOut,
)

@Composable
private fun HomeScreenPreview(state: HomeUiState, themeMode: ThemeMode) {
    SpendWiseTheme(themeMode) {
        Box {
            AppBackground(Modifier.matchParentSize())
            HomeScreen(
                state = state,
                onIntent = {},
                onEnableReminder = {},
                onAddExpense = {},
                onOpenExpense = {},
                onOpenReport = {},
                onOpenSettings = {},
            )
        }
    }
}

@Preview(name = "Home - light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenLightPreview() = HomeScreenPreview(PopulatedPreviewState, ThemeMode.LIGHT)

@Preview(name = "Home - dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenDarkPreview() = HomeScreenPreview(PopulatedPreviewState, ThemeMode.DARK)

@Preview(name = "Home - empty day", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenEmptyPreview() = HomeScreenPreview(EmptyPreviewState, ThemeMode.LIGHT)
