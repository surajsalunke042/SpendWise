package com.surajdev.spendwise.feature

import com.surajdev.spendwise.feature.addexpense.AddExpenseViewModel
import com.surajdev.spendwise.feature.common.Formats
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddExpenseInputTest {

    @Test
    fun amountInputAcceptsDecimalsUpToTwoPlaces() {
        assertEquals("120", AddExpenseViewModel.sanitizeAmount("120"))
        assertEquals("120.5", AddExpenseViewModel.sanitizeAmount("120.5"))
        assertEquals("120.50", AddExpenseViewModel.sanitizeAmount("120,50"))
        assertEquals("1200", AddExpenseViewModel.sanitizeAmount("1 200"))
        assertNull(AddExpenseViewModel.sanitizeAmount("120.555"))
        assertNull(AddExpenseViewModel.sanitizeAmount("1.2.3"))
        assertNull(AddExpenseViewModel.sanitizeAmount("1234567890"))
    }

    @Test
    fun dateAndTimeLabels() {
        val today = LocalDate(2026, 10, 24)
        assertEquals("Today", Formats.dayLabel(today, today))
        assertEquals("Yesterday", Formats.dayLabel(LocalDate(2026, 10, 23), today))
        assertEquals("Thu, 22 Oct", Formats.dayLabel(LocalDate(2026, 10, 22), today))
        assertEquals("Wed, 31 Dec 2025", Formats.dayLabel(LocalDate(2025, 12, 31), today))
        assertEquals("9:30 PM", Formats.time(LocalTime(21, 30)))
        assertEquals("12:05 AM", Formats.time(LocalTime(0, 5)))
        assertEquals("12:00 PM", Formats.time(LocalTime(12, 0)))
        assertEquals("6p", Formats.hourShort(18))
    }
}
