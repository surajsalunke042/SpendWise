package com.surajdev.spendwise.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** Inclusive range of calendar days. */
data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(start <= endInclusive) { "start must not be after end" }
    }

    operator fun contains(date: LocalDate): Boolean = date in start..endInclusive
}

data class ExpenseFilter(
    val query: String = "",
    val categoryIds: Set<String> = emptySet(),
    val dateRange: DateRange? = null,
) {
    val isActive: Boolean get() = query.isNotBlank() || categoryIds.isNotEmpty() || dateRange != null
}

/** Expenses of one day, newest first. */
data class DaySection(
    val date: LocalDate,
    val total: Money,
    val items: List<ExpenseItem>,
)

data class CategorySpend(
    val category: Category,
    val total: Money,
    val count: Int,
    /** Fraction of the day's total, 0..1. */
    val share: Float,
)

data class DailyReport(
    val date: LocalDate,
    val total: Money,
    val entries: List<ExpenseItem>,
    /** Sorted by total, largest first. */
    val categoryBreakdown: List<CategorySpend>,
    /** 24 buckets, index = hour of day. */
    val hourlyTotals: List<Money>,
    val largestExpense: ExpenseItem?,
    val previousDayTotal: Money,
    /** Average over up to 30 days before [date], counting zero-spend days. Null if there's no history. */
    val trailingAverage: Money?,
    /** Daily totals for the 7 days ending on [date], oldest first. */
    val lastSevenDays: List<Pair<LocalDate, Money>>,
) {
    val topCategories: List<CategorySpend> get() = categoryBreakdown.take(3)

    /** Change vs previous day as a fraction (0.12 = +12%). Null when yesterday was zero. */
    val changeVsPreviousDay: Double? get() = change(total, previousDayTotal)

    val changeVsAverage: Double? get() = trailingAverage?.let { change(total, it) }

    private fun change(current: Money, baseline: Money): Double? =
        if (baseline.minorUnits == 0L) null
        else (current.minorUnits - baseline.minorUnits).toDouble() / baseline.minorUnits
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reminderPromptDismissed: Boolean = false,
    val lastUsedCategoryId: String? = null,
    val lastUsedPaymentMethod: PaymentMethod? = null,
) {
    companion object {
        val DEFAULT_REMINDER_TIME = LocalTime(21, 30)
    }
}

enum class AuthProvider { GOOGLE, APPLE, EMAIL, OTHER }

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val providers: Set<AuthProvider>,
)

sealed interface SyncStatus {
    /** Firebase isn't configured in this build. */
    data object Unavailable : SyncStatus

    /** Not signed in: data stays on this device only. */
    data object SignedOut : SyncStatus

    data object Syncing : SyncStatus

    data class Idle(val lastSyncedAt: Instant?, val pendingChanges: Int) : SyncStatus

    data class Failed(val message: String, val pendingChanges: Int) : SyncStatus
}
