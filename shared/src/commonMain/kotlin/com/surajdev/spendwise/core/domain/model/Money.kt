package com.surajdev.spendwise.core.domain.model

/**
 * Supported currencies. Only INR is enabled for v1, but every amount carries its currency
 * so more can be added without a data migration.
 */
enum class Currency(val code: String, val symbol: String, val fractionDigits: Int) {
    INR(code = "INR", symbol = "₹", fractionDigits = 2);

    val minorPerMajor: Long = pow10(fractionDigits)

    companion object {
        val Default = INR

        fun fromCode(code: String): Currency = entries.firstOrNull { it.code == code } ?: Default
    }
}

/**
 * An amount of money in minor units (paise for INR). Amounts are never stored as floating point,
 * which avoids rounding errors in totals.
 */
data class Money(val minorUnits: Long, val currency: Currency = Currency.Default) : Comparable<Money> {

    val isZero: Boolean get() = minorUnits == 0L

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits - other.minorUnits)
    }

    operator fun div(divisor: Int): Money = copy(minorUnits = minorUnits / divisor)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
    }

    companion object {
        fun zero(currency: Currency = Currency.Default) = Money(0, currency)

        /**
         * Parses user input such as "120", "120.5", "1,20,000.75" into money.
         * Returns null for anything that isn't a valid non-negative amount.
         */
        fun parse(input: String, currency: Currency = Currency.Default): Money? {
            val cleaned = input.trim().replace(",", "").replace(currency.symbol, "").trim()
            if (cleaned.isEmpty()) return null
            val parts = cleaned.split('.')
            if (parts.size > 2) return null
            val whole = parts[0].ifEmpty { "0" }
            val fraction = parts.getOrElse(1) { "" }
            if (!whole.all(Char::isDigit) || !fraction.all(Char::isDigit)) return null
            if (fraction.length > currency.fractionDigits) return null
            if (whole.length > 12) return null
            val minor = whole.toLong() * currency.minorPerMajor +
                fraction.padEnd(currency.fractionDigits, '0').ifEmpty { "0" }.toLong()
            return Money(minor, currency)
        }
    }
}

fun Iterable<Money>.sum(currency: Currency = Currency.Default): Money =
    fold(Money.zero(currency)) { acc, money -> acc + money }

private fun pow10(n: Int): Long {
    var result = 1L
    repeat(n) { result *= 10 }
    return result
}
