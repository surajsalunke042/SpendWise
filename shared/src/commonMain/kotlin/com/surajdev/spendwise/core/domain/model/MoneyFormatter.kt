package com.surajdev.spendwise.core.domain.model

import kotlin.math.abs

/**
 * Formats money with Indian digit grouping (₹1,23,45,678.50).
 * Pure Kotlin so it behaves identically on Android and iOS regardless of device locale.
 */
object MoneyFormatter {

    /**
     * @param showFraction when false, the paise are dropped (rounded half up) for compact display.
     * When null, paise are shown only if non-zero.
     */
    fun format(money: Money, showFraction: Boolean? = null, withSymbol: Boolean = true): String {
        val currency = money.currency
        val negative = money.minorUnits < 0
        val absMinor = abs(money.minorUnits)
        val fractionPart = absMinor % currency.minorPerMajor
        val includeFraction = showFraction ?: (fractionPart != 0L)

        val wholePart = if (includeFraction) {
            absMinor / currency.minorPerMajor
        } else {
            (absMinor + currency.minorPerMajor / 2) / currency.minorPerMajor
        }

        val builder = StringBuilder()
        if (negative) builder.append('-')
        if (withSymbol) builder.append(currency.symbol)
        builder.append(groupIndian(wholePart.toString()))
        if (includeFraction && currency.fractionDigits > 0) {
            builder.append('.')
            builder.append(fractionPart.toString().padStart(currency.fractionDigits, '0'))
        }
        return builder.toString()
    }

    /** Compact form for chart labels: ₹950, ₹1.2K, ₹3.4L, ₹1.1Cr. */
    fun formatCompact(money: Money): String {
        val major = abs(money.minorUnits) / money.currency.minorPerMajor
        val sign = if (money.minorUnits < 0) "-" else ""
        val symbol = money.currency.symbol
        return when {
            major >= 1_00_00_000 -> "$sign$symbol${oneDecimal(major, 1_00_00_000)}Cr"
            major >= 1_00_000 -> "$sign$symbol${oneDecimal(major, 1_00_000)}L"
            major >= 1_000 -> "$sign$symbol${oneDecimal(major, 1_000)}K"
            else -> "$sign$symbol$major"
        }
    }

    /** Plain editable text for an amount field, e.g. 12050 paise -> "120.5". */
    fun toInputText(money: Money): String {
        val currency = money.currency
        val whole = money.minorUnits / currency.minorPerMajor
        val fraction = money.minorUnits % currency.minorPerMajor
        if (fraction == 0L) return whole.toString()
        return "$whole.${fraction.toString().padStart(currency.fractionDigits, '0').trimEnd('0')}"
    }

    internal fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val groups = rest.reversed().chunked(2).map { it.reversed() }.reversed()
        return groups.joinToString(",") + "," + lastThree
    }

    private fun oneDecimal(value: Long, unit: Long): String {
        val tenths = (value * 10 + unit / 2) / unit
        val whole = tenths / 10
        val decimal = tenths % 10
        return if (decimal == 0L) whole.toString() else "$whole.$decimal"
    }
}
