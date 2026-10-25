package com.surajdev.spendwise.feature.common

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.daysUntil

/** Locale-independent English date/time labels (the app is English-only for v1). */
object Formats {

    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date.daysUntil(today)) {
        0 -> "Today"
        1 -> "Yesterday"
        else -> shortDate(date, includeYear = date.year != today.year)
    }

    /** "Fri, 24 Oct" or "Fri, 24 Oct 2025". */
    fun shortDate(date: LocalDate, includeYear: Boolean = false): String {
        val base = "${weekdayShort(date.dayOfWeek)}, ${date.day} ${monthShort(date.month)}"
        return if (includeYear) "$base ${date.year}" else base
    }

    /** "Friday, 24 October". */
    fun longDate(date: LocalDate): String =
        "${weekdayLong(date.dayOfWeek)}, ${date.day} ${monthLong(date.month)}"

    /** "24 Oct". */
    fun dayMonth(date: LocalDate): String = "${date.day} ${monthShort(date.month)}"

    /** "9:30 PM". */
    fun time(time: LocalTime): String {
        val hour12 = when (val h = time.hour % 12) { 0 -> 12; else -> h }
        val minute = time.minute.toString().padStart(2, '0')
        val suffix = if (time.hour < 12) "AM" else "PM"
        return "$hour12:$minute $suffix"
    }

    /** Compact hour label for charts: "12a", "6a", "12p", "6p". */
    fun hourShort(hour: Int): String {
        val h = when (val value = hour % 12) { 0 -> 12; else -> value }
        return "$h${if (hour < 12) "a" else "p"}"
    }

    fun weekdayShort(day: DayOfWeek): String = weekdayLong(day).take(3)

    fun weekdayLong(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
    }

    fun monthShort(month: Month): String = monthLong(month).take(3)

    fun monthLong(month: Month): String = when (month) {
        Month.JANUARY -> "January"
        Month.FEBRUARY -> "February"
        Month.MARCH -> "March"
        Month.APRIL -> "April"
        Month.MAY -> "May"
        Month.JUNE -> "June"
        Month.JULY -> "July"
        Month.AUGUST -> "August"
        Month.SEPTEMBER -> "September"
        Month.OCTOBER -> "October"
        Month.NOVEMBER -> "November"
        Month.DECEMBER -> "December"
    }

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
