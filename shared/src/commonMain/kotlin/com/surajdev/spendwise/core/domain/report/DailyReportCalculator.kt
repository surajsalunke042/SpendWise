package com.surajdev.spendwise.core.domain.report

import com.surajdev.spendwise.core.domain.model.Category
import com.surajdev.spendwise.core.domain.model.CategorySpend
import com.surajdev.spendwise.core.domain.model.Currency
import com.surajdev.spendwise.core.domain.model.DailyReport
import com.surajdev.spendwise.core.domain.model.DefaultCategories
import com.surajdev.spendwise.core.domain.model.Expense
import com.surajdev.spendwise.core.domain.model.ExpenseItem
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.sum
import com.surajdev.spendwise.core.domain.time.DayMath.minusDays
import com.surajdev.spendwise.core.domain.time.DayMath.plusDays
import com.surajdev.spendwise.core.domain.time.DayMath.previousDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Builds a [DailyReport]. Pure function of its inputs, so it is fully unit-testable. */
object DailyReportCalculator {

    const val AVERAGE_WINDOW_DAYS = 30

    /**
     * @param expenses expenses covering at least [AVERAGE_WINDOW_DAYS] days before [date] through [date].
     * Extra expenses outside that range are ignored.
     * @param earliestExpenseDate local date of the user's first-ever expense. Days before it don't count
     * toward the average, so a new user's average isn't diluted by days before they installed the app.
     */
    fun calculate(
        date: LocalDate,
        expenses: List<Expense>,
        categories: Map<String, Category>,
        earliestExpenseDate: LocalDate?,
        currency: Currency = Currency.Default,
    ): DailyReport {
        val byDate = expenses.groupBy { it.localDate }
        val dayExpenses = byDate[date].orEmpty().sortedBy { it.occurredAt }
        val items = dayExpenses.map { ExpenseItem(it, categories.resolve(it.categoryId)) }
        val total = dayExpenses.map { it.amount }.sum(currency)

        val breakdown = items
            .groupBy { it.category.id }
            .map { (_, group) ->
                val categoryTotal = group.map { it.expense.amount }.sum(currency)
                CategorySpend(
                    category = group.first().category,
                    total = categoryTotal,
                    count = group.size,
                    share = if (total.isZero) 0f else categoryTotal.minorUnits.toFloat() / total.minorUnits,
                )
            }
            .sortedWith(compareByDescending<CategorySpend> { it.total.minorUnits }.thenBy { it.category.sortOrder })

        val hourly = MutableList(24) { Money.zero(currency) }
        dayExpenses.forEach { expense ->
            val hour = expense.localDateTime.hour
            hourly[hour] = hourly[hour] + expense.amount
        }

        fun totalOn(day: LocalDate): Money = byDate[day].orEmpty().map { it.amount }.sum(currency)

        return DailyReport(
            date = date,
            total = total,
            entries = items,
            categoryBreakdown = breakdown,
            hourlyTotals = hourly,
            largestExpense = items.maxByOrNull { it.expense.amount.minorUnits },
            previousDayTotal = totalOn(date.previousDay()),
            trailingAverage = trailingAverage(date, earliestExpenseDate, ::totalOn, currency),
            lastSevenDays = (6 downTo 0).map { offset ->
                val day = date.minusDays(offset)
                day to totalOn(day)
            },
        )
    }

    private fun trailingAverage(
        date: LocalDate,
        earliestExpenseDate: LocalDate?,
        totalOn: (LocalDate) -> Money,
        currency: Currency,
    ): Money? {
        if (earliestExpenseDate == null || earliestExpenseDate >= date) return null
        val windowStart = maxOf(date.minusDays(AVERAGE_WINDOW_DAYS), earliestExpenseDate)
        val days = windowStart.daysUntil(date)
        if (days <= 0) return null
        val sum = (0 until days).map { totalOn(windowStart.plusDays(it)) }.sum(currency)
        return sum / days
    }

    private fun Map<String, Category>.resolve(id: String): Category =
        this[id] ?: this[DefaultCategories.OTHER_ID] ?: DefaultCategories.all.last()
}
