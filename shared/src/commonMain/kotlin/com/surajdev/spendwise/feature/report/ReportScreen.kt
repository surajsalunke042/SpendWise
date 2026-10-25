package com.surajdev.spendwise.feature.report

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surajdev.spendwise.core.designsystem.chart.BarChart
import com.surajdev.spendwise.core.designsystem.chart.DonutChart
import com.surajdev.spendwise.core.designsystem.chart.DonutSlice
import com.surajdev.spendwise.core.designsystem.component.AmountText
import com.surajdev.spendwise.core.designsystem.component.AppIcon
import com.surajdev.spendwise.core.designsystem.component.CategoryBadge
import com.surajdev.spendwise.core.designsystem.component.ChangeIndicator
import com.surajdev.spendwise.core.designsystem.component.EmptyState
import com.surajdev.spendwise.core.designsystem.component.SurfaceCard
import com.surajdev.spendwise.core.designsystem.component.HeroCard
import com.surajdev.spendwise.core.designsystem.component.LocalBottomBarClearance
import com.surajdev.spendwise.core.designsystem.component.HairlineDivider
import com.surajdev.spendwise.core.designsystem.component.SectionLabel
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.designsystem.theme.categoryColor
import com.surajdev.spendwise.core.designsystem.theme.tabular
import com.surajdev.spendwise.core.domain.model.CategorySpend
import com.surajdev.spendwise.core.domain.model.DailyReport
import com.surajdev.spendwise.core.domain.model.MoneyFormatter
import com.surajdev.spendwise.feature.common.DatePickerSheet
import com.surajdev.spendwise.feature.common.ExpenseRow
import com.surajdev.spendwise.feature.common.Formats
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

@Composable
fun ReportRoute(
    initialDate: LocalDate?,
    onOpenExpense: (String) -> Unit,
    viewModel: ReportViewModel = koinViewModel(key = "report-$initialDate") { parametersOf(ReportArgs(initialDate)) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportScreen(state, viewModel::onIntent, onOpenExpense)
}

@Composable
fun ReportScreen(
    state: ReportUiState,
    onIntent: (ReportIntent) -> Unit,
    onOpenExpense: (String) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = LocalBottomBarClearance.current + Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                DateNavigator(state, onIntent, onPickDate = { showPicker = true })
            }
            val report = state.report ?: return@LazyColumn
            item { Summary(report, Modifier.padding(horizontal = Spacing.screen)) }
            if (report.entries.isEmpty()) {
                item {
                    EmptyState(
                        icon = AppIcons.Report,
                        title = "No spending on this day",
                        message = "A quiet day. Your report fills in as you log expenses.",
                    )
                }
            } else {
                item { BreakdownCard(report, Modifier.padding(horizontal = Spacing.screen)) }
                item { HourlyCard(report, Modifier.padding(horizontal = Spacing.screen)) }
                item { HighlightsCard(report, onOpenExpense, Modifier.padding(horizontal = Spacing.screen)) }
            }
            item { WeekCard(report, Modifier.padding(horizontal = Spacing.screen)) }
            if (report.entries.isNotEmpty()) {
                item { EntriesCard(report, onOpenExpense, Modifier.padding(horizontal = Spacing.screen)) }
            }
        }
    }
    if (showPicker) {
        DatePickerSheet(
            initial = state.date,
            maxDate = state.today,
            onDismiss = { showPicker = false },
            onConfirm = {
                onIntent(ReportIntent.DateSelected(it))
                showPicker = false
            },
        )
    }
}

@Composable
private fun DateNavigator(state: ReportUiState, onIntent: (ReportIntent) -> Unit, onPickDate: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = Spacing.lg)) {
        Text(
            "Daily report",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = Spacing.screen).semantics { heading() },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onIntent(ReportIntent.PreviousDay) }) {
                AppIcon(AppIcons.ChevronLeft, contentDescription = "Previous day")
            }
            TextButton(onClick = onPickDate, modifier = Modifier.weight(1f)) {
                Text(
                    Formats.dayLabel(state.date, state.today).let { label ->
                        if (label == "Today" || label == "Yesterday") "$label · ${Formats.dayMonth(state.date)}" else label
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            IconButton(onClick = { onIntent(ReportIntent.NextDay) }, enabled = state.canGoForward) {
                AppIcon(
                    AppIcons.ChevronRight,
                    contentDescription = "Next day",
                    tint = if (state.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun Summary(report: DailyReport, modifier: Modifier = Modifier) {
    val extended = SpendWiseTheme.extended
    HeroCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel("Total spent", color = extended.onHeroMuted)
        Spacer(Modifier.height(Spacing.sm))
        Text(MoneyFormatter.format(report.total), style = MaterialTheme.typography.displayLarge, color = extended.onHero)
        Text(
            "${report.entries.size} ${if (report.entries.size == 1) "expense" else "expenses"}",
            style = MaterialTheme.typography.bodyMedium,
            color = extended.onHeroMuted,
        )
        Spacer(Modifier.height(Spacing.md))
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (extended.vivid) Color.White.copy(alpha = 0.22f) else extended.hairline))
        Spacer(Modifier.height(Spacing.md))
        ComparisonRow("Previous day", MoneyFormatter.format(report.previousDayTotal, showFraction = false)) {
            ChangeIndicator(report.changeVsPreviousDay, "the previous day", onHero = true)
        }
        Spacer(Modifier.height(Spacing.md))
        ComparisonRow(
            "30-day average",
            report.trailingAverage?.let { MoneyFormatter.format(it, showFraction = false) } ?: "Not enough history",
        ) {
            if (report.trailingAverage != null) ChangeIndicator(report.changeVsAverage, "your average", onHero = true)
        }
    }
}

@Composable
private fun ComparisonRow(label: String, value: String, indicator: @Composable () -> Unit) {
    val extended = SpendWiseTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = extended.onHeroMuted, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall.tabular(), color = extended.onHero)
        }
        indicator()
    }
}

@Composable
private fun BreakdownCard(report: DailyReport, modifier: Modifier = Modifier) {
    val dark = SpendWiseTheme.extended.isDark
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel("By category")
        Spacer(Modifier.height(Spacing.lg))
        DonutChart(
            slices = report.categoryBreakdown.map { DonutSlice(it.total.minorUnits.toFloat(), categoryColor(it.category.colorArgb, dark)) },
            contentDescription = "Spending by category: " + report.categoryBreakdown.joinToString { spend ->
                "${spend.category.name} ${MoneyFormatter.format(spend.total, showFraction = false)}, ${(spend.share * 100).roundToInt()} percent"
            },
            modifier = Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    MoneyFormatter.format(report.total, showFraction = false),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "${report.categoryBreakdown.size} ${if (report.categoryBreakdown.size == 1) "category" else "categories"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        report.categoryBreakdown.forEach { spend -> LegendRow(spend) }
    }
}

@Composable
private fun LegendRow(spend: CategorySpend) {
    val color = categoryColor(spend.category.colorArgb, SpendWiseTheme.extended.isDark)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(Spacing.md))
        Text(spend.category.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "${(spend.share * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.md))
        AmountText(spend.total, style = MaterialTheme.typography.titleSmall, showFraction = false)
    }
}

@Composable
private fun HourlyCard(report: DailyReport, modifier: Modifier = Modifier) {
    val busiest = report.hourlyTotals.withIndex().maxByOrNull { it.value.minorUnits }
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel("Through the day")
        if (busiest != null && !busiest.value.isZero) {
            Text(
                "Most spent around ${Formats.time(kotlinx.datetime.LocalTime(busiest.index, 0))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        BarChart(
            values = report.hourlyTotals.map { it.minorUnits.toFloat() },
            labels = (0 until 24).map { hour -> if (hour % 6 == 0) Formats.hourShort(hour) else null },
            valueLabel = { hour -> "${Formats.hourShort(hour)} · ${MoneyFormatter.format(report.hourlyTotals[hour], showFraction = false)}" },
            contentDescription = "Spending by hour: " + report.hourlyTotals.withIndex()
                .filter { !it.value.isZero }
                .joinToString { "${Formats.time(kotlinx.datetime.LocalTime(it.index, 0))} ${MoneyFormatter.format(it.value, showFraction = false)}" },
            highlightIndex = busiest?.index,
            maxBarWidth = 8.dp,
        )
    }
}

@Composable
private fun HighlightsCard(report: DailyReport, onOpenExpense: (String) -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 0.dp) {
        Column(Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg)) {
            SectionLabel("Top categories")
            Spacer(Modifier.height(Spacing.sm))
            report.topCategories.forEachIndexed { index, spend ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.width(28.dp),
                    )
                    CategoryBadge(spend.category.iconKey, spend.category.colorArgb, size = 32.dp)
                    Spacer(Modifier.width(Spacing.md))
                    Text(spend.category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    AmountText(spend.total, style = MaterialTheme.typography.titleSmall, showFraction = false)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            SectionLabel("Largest expense")
        }
        report.largestExpense?.let { item ->
            ExpenseRow(item, onClick = { onOpenExpense(item.expense.id) })
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun WeekCard(report: DailyReport, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel("7 days to ${Formats.dayMonth(report.date)}")
        Spacer(Modifier.height(Spacing.md))
        BarChart(
            values = report.lastSevenDays.map { it.second.minorUnits.toFloat() },
            labels = report.lastSevenDays.map { Formats.weekdayShort(it.first.dayOfWeek) },
            valueLabel = { index -> MoneyFormatter.format(report.lastSevenDays[index].second, showFraction = false) },
            contentDescription = "Daily totals for the week: " + report.lastSevenDays.joinToString { (date, money) ->
                "${Formats.weekdayLong(date.dayOfWeek)} ${MoneyFormatter.format(money, showFraction = false)}"
            },
            highlightIndex = report.lastSevenDays.lastIndex,
            height = 140.dp,
        )
    }
}

@Composable
private fun EntriesCard(report: DailyReport, onOpenExpense: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionLabel("All entries", Modifier.padding(vertical = Spacing.sm))
        SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
            report.entries.forEachIndexed { index, item ->
                ExpenseRow(item, onClick = { onOpenExpense(item.expense.id) })
                if (index < report.entries.lastIndex) HairlineDivider(Modifier.padding(start = 68.dp))
            }
        }
    }
}
