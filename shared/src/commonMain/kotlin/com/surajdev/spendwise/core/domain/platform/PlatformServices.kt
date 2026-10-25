package com.surajdev.spendwise.core.domain.platform

import kotlinx.datetime.LocalTime

/** Schedules the daily "log your expenses" reminder. Implemented per platform. */
interface ReminderScheduler {
    fun schedule(time: LocalTime)

    fun cancel()
}

/** Background sync scheduling. On Android this uses WorkManager; on iOS it is a no-op. */
interface BackgroundSyncScheduler {
    fun schedulePeriodic()

    fun cancel()
}
