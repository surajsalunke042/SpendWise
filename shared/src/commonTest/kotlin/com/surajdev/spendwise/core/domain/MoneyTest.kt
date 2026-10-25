package com.surajdev.spendwise.core.domain

import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.MoneyFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {

    @Test
    fun parsesWholeAndFractionalAmounts() {
        assertEquals(Money(12000), Money.parse("120"))
        assertEquals(Money(12050), Money.parse("120.5"))
        assertEquals(Money(12005), Money.parse("120.05"))
        assertEquals(Money(50), Money.parse(".5"))
        assertEquals(Money(12000), Money.parse("120."))
    }

    @Test
    fun parsesGroupedInputWithSymbol() {
        assertEquals(Money(1_20_000_75), Money.parse("₹1,20,000.75"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1.2.3"))
        assertNull(Money.parse("10.999"))
        assertNull(Money.parse("-5"))
    }

    @Test
    fun formatsWithIndianGrouping() {
        assertEquals("₹0", MoneyFormatter.format(Money(0)))
        assertEquals("₹999", MoneyFormatter.format(Money(999_00)))
        assertEquals("₹1,000", MoneyFormatter.format(Money(1_000_00)))
        assertEquals("₹1,23,456", MoneyFormatter.format(Money(1_23_456_00)))
        assertEquals("₹1,23,45,678.50", MoneyFormatter.format(Money(1_23_45_678_50)))
        assertEquals("-₹12,345", MoneyFormatter.format(Money(-12_345_00)))
    }

    @Test
    fun fractionDisplayOptions() {
        assertEquals("₹120.50", MoneyFormatter.format(Money(12050)))
        assertEquals("₹121", MoneyFormatter.format(Money(12050), showFraction = false))
        assertEquals("₹120.00", MoneyFormatter.format(Money(12000), showFraction = true))
        assertEquals("1,200", MoneyFormatter.format(Money(1_200_00), withSymbol = false))
    }

    @Test
    fun compactFormat() {
        assertEquals("₹950", MoneyFormatter.formatCompact(Money(950_00)))
        assertEquals("₹1.2K", MoneyFormatter.formatCompact(Money(1_234_00)))
        assertEquals("₹3.5L", MoneyFormatter.formatCompact(Money(3_45_000_00)))
        assertEquals("₹1Cr", MoneyFormatter.formatCompact(Money(1_00_00_000_00)))
    }

    @Test
    fun inputTextRoundTrips() {
        assertEquals("120", MoneyFormatter.toInputText(Money(12000)))
        assertEquals("120.5", MoneyFormatter.toInputText(Money(12050)))
        assertEquals("120.05", MoneyFormatter.toInputText(Money(12005)))
        listOf(1L, 50L, 12000L, 12345L).forEach { minor ->
            assertEquals(Money(minor), Money.parse(MoneyFormatter.toInputText(Money(minor))))
        }
    }
}
