package com.surajdev.spendwise.core.domain.time

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Day boundaries are local midnight. Each expense belongs to the day in the zone it was logged in,
 * so queries use a window padded by a day on each side and the caller filters by local date.
 */
object DayMath {

    /** Instant window that safely contains every expense whose local date is in [start]..[endInclusive]. */
    fun queryWindow(start: LocalDate, endInclusive: LocalDate, zone: TimeZone): ClosedRange<Instant> {
        val from = start.minus(DatePeriod(days = 1)).atStartOfDayIn(zone)
        val to = endInclusive.plus(DatePeriod(days = 2)).atStartOfDayIn(zone)
        return from..to
    }

    fun today(clock: Clock, zone: TimeZone): LocalDate = clock.todayIn(zone)

    fun LocalDate.previousDay(): LocalDate = minus(DatePeriod(days = 1))

    fun LocalDate.nextDay(): LocalDate = plus(DatePeriod(days = 1))

    fun LocalDate.minusDays(days: Int): LocalDate = minus(DatePeriod(days = days))

    fun LocalDate.plusDays(days: Int): LocalDate = plus(DatePeriod(days = days))
}
