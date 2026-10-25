package com.surajdev.spendwise.core.domain

import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.report.DailyReportCalculator
import com.surajdev.spendwise.testing.defaultCategoryMap
import com.surajdev.spendwise.testing.expense
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyReportCalculatorTest {

    private val day = LocalDate(2026, 10, 24)

    @Test
    fun totalsBreakdownAndLargestExpense() {
        val expenses = listOf(
            expense("a", 200, "2026-10-24T09:15", "cat_food"),
            expense("b", 300, "2026-10-24T13:40", "cat_food"),
            expense("c", 500, "2026-10-24T18:05", "cat_transport"),
            expense("other-day", 999, "2026-10-23T12:00", "cat_bills"),
        )

        val report = DailyReportCalculator.calculate(day, expenses, defaultCategoryMap, earliestExpenseDate = LocalDate(2026, 10, 23))

        assertEquals(Money(1000_00), report.total)
        assertEquals(3, report.entries.size)
        assertEquals(listOf("cat_food", "cat_transport"), report.categoryBreakdown.map { it.category.id })
        assertEquals(Money(500_00), report.categoryBreakdown[0].total)
        assertEquals(0.5f, report.categoryBreakdown[0].share)
        assertEquals("c", report.largestExpense?.expense?.id)
        assertEquals(Money(999_00), report.previousDayTotal)
    }

    @Test
    fun hourlyBucketsUseLocalTime() {
        val expenses = listOf(
            expense("a", 100, "2026-10-24T00:30"),
            expense("b", 100, "2026-10-24T09:00"),
            expense("c", 150, "2026-10-24T09:59"),
            expense("d", 100, "2026-10-24T23:59"),
        )

        val report = DailyReportCalculator.calculate(day, expenses, defaultCategoryMap, earliestExpenseDate = day)

        assertEquals(24, report.hourlyTotals.size)
        assertEquals(Money(100_00), report.hourlyTotals[0])
        assertEquals(Money(250_00), report.hourlyTotals[9])
        assertEquals(Money(100_00), report.hourlyTotals[23])
    }

    @Test
    fun expenseBelongsToTheDayInTheZoneItWasLogged() {
        // 23:30 in New York on the 24th is already the 25th in India.
        val newYork = TimeZone.of("America/New_York")
        val expenses = listOf(expense("travel", 400, "2026-10-24T23:30", zone = newYork))

        val report = DailyReportCalculator.calculate(day, expenses, defaultCategoryMap, earliestExpenseDate = day)

        assertEquals(Money(400_00), report.total)
    }

    @Test
    fun averageCountsZeroSpendDaysOverThirtyDays() {
        // ₹3,000 spent once, 10 days ago; history goes back more than 30 days.
        val expenses = listOf(
            expense("old", 3000, "2026-10-14T12:00"),
            expense("today", 100, "2026-10-24T12:00"),
        )

        val report = DailyReportCalculator.calculate(day, expenses, defaultCategoryMap, earliestExpenseDate = LocalDate(2026, 1, 1))

        assertEquals(Money(100_00), report.trailingAverage) // 3000 / 30 days
        assertEquals(0.0, report.changeVsAverage)
    }

    @Test
    fun averageOnlyCountsDaysSinceFirstExpense() {
        val expenses = listOf(
            expense("first", 300, "2026-10-21T12:00"),
            expense("today", 100, "2026-10-24T12:00"),
        )

        val report = DailyReportCalculator.calculate(day, expenses, defaultCategoryMap, earliestExpenseDate = LocalDate(2026, 10, 21))

        // Window is the 21st, 22nd and 23rd: ₹300 over 3 days.
        assertEquals(Money(100_00), report.trailingAverage)
    }

    @Test
    fun noAverageWithoutHistory() {
        val report = DailyReportCalculator.calculate(
            day,
            listOf(expense("today", 100, "2026-10-24T12:00")),
            defaultCategoryMap,
            earliestExpenseDate = day,
        )

        assertNull(report.trailingAverage)
        assertNull(report.changeVsPreviousDay)
    }

    @Test
    fun changeVsPreviousDay() {
        val expenses = listOf(
            expense("y", 200, "2026-10-23T12:00"),
            expense("t", 300, "2026-10-24T12:00"),
        )

        val report = DailyReportCalculator.calculate(day, expenses, defaultCategoryMap, earliestExpenseDate = LocalDate(2026, 10, 23))

        assertEquals(0.5, report.changeVsPreviousDay)
    }

    @Test
    fun lastSevenDaysIncludesEmptyDaysOldestFirst() {
        val expenses = listOf(
            expense("a", 100, "2026-10-18T12:00"),
            expense("b", 200, "2026-10-24T12:00"),
        )

        val report = DailyReportCalculator.calculate(day, expenses, defaultCategoryMap, earliestExpenseDate = LocalDate(2026, 10, 18))

        assertEquals(7, report.lastSevenDays.size)
        assertEquals(LocalDate(2026, 10, 18) to Money(100_00), report.lastSevenDays.first())
        assertEquals(LocalDate(2026, 10, 24) to Money(200_00), report.lastSevenDays.last())
        assertEquals(Money(0), report.lastSevenDays[3].second)
    }

    @Test
    fun unknownCategoryFallsBackToOther() {
        val report = DailyReportCalculator.calculate(
            day,
            listOf(expense("x", 50, "2026-10-24T12:00", categoryId = "deleted-category")),
            defaultCategoryMap,
            earliestExpenseDate = day,
        )

        assertEquals("cat_other", report.categoryBreakdown.single().category.id)
    }
}
