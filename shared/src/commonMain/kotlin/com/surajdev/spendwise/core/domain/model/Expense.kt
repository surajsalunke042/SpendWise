package com.surajdev.spendwise.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A single expense.
 *
 * @property occurredAt the moment the money was spent.
 * @property timeZoneId the zone the expense was logged in, so it stays on the right day
 * even if the user later travels.
 * @property source where the record came from. Phase 2 adds automatic sources.
 * @property externalRef an ID from the source (e.g. a UPI reference), used to detect duplicates.
 */
data class Expense(
    val id: String,
    val amount: Money,
    val categoryId: String,
    val note: String,
    val occurredAt: Instant,
    val timeZoneId: String,
    val paymentMethod: PaymentMethod?,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val externalRef: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val timeZone: TimeZone get() = timeZoneOrDefault(timeZoneId)

    val localDateTime: LocalDateTime get() = occurredAt.toLocalDateTime(timeZone)

    val localDate: LocalDate get() = localDateTime.date
}

enum class PaymentMethod(val label: String, val iconKey: String) {
    CASH("Cash", "payments"),
    UPI("UPI", "qr_code"),
    CARD("Card", "credit_card"),
    NET_BANKING("Net banking", "account_balance"),
    WALLET("Wallet", "account_balance_wallet"),
    OTHER("Other", "more_horiz");

    companion object {
        fun fromNameOrNull(name: String?): PaymentMethod? = entries.firstOrNull { it.name == name }
    }
}

enum class ExpenseSource {
    MANUAL,
    SMS,
    NOTIFICATION,
    EMAIL,
    BANK_API,
    IMPORT;

    companion object {
        fun fromName(name: String): ExpenseSource = entries.firstOrNull { it.name == name } ?: MANUAL
    }
}

/** An expense together with its category, ready for display. */
data class ExpenseItem(
    val expense: Expense,
    val category: Category,
)

fun timeZoneOrDefault(id: String): TimeZone =
    runCatching { TimeZone.of(id) }.getOrElse { TimeZone.currentSystemDefault() }
