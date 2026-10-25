package com.surajdev.spendwise.core.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.MoneyFormatter
import com.surajdev.spendwise.core.domain.platform.BackgroundSyncScheduler
import com.surajdev.spendwise.core.domain.platform.ReminderScheduler
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import com.surajdev.spendwise.shared.R
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/** Intent extra telling MainActivity to open today's report. */
const val EXTRA_OPEN_REPORT = "com.surajdev.spendwise.OPEN_REPORT"

private const val REMINDER_WORK = "daily_reminder"
private const val SYNC_WORK = "periodic_sync"
private const val REMINDER_CHANNEL = "daily_reminder"
private const val REMINDER_NOTIFICATION_ID = 1001
private const val KEY_HOUR = "hour"
private const val KEY_MINUTE = "minute"

/**
 * Schedules a one-off job for the next reminder time; the job re-schedules itself after firing.
 * WorkManager's timing is inexact (a few minutes), which is fine for a reminder and needs no
 * exact-alarm permission.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    override fun schedule(time: LocalTime) {
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(millisUntilNext(time), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_HOUR to time.hour, KEY_MINUTE to time.minute))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(REMINDER_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK)
    }

    private fun millisUntilNext(time: LocalTime): Long {
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = Clock.System.todayIn(zone)
        var next = LocalDateTime(today, time).toInstant(zone)
        if (next <= now) next = LocalDateTime(today.plus(DatePeriod(days = 1)), time).toInstant(zone)
        return (next - now).inWholeMilliseconds
    }
}

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val database: SpendWiseDatabase by inject()
    private val settings: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        val current = settings.settings.first()
        if (!current.reminderEnabled) return Result.success()

        showNotification()
        // Schedule tomorrow's reminder.
        AndroidReminderScheduler(applicationContext).schedule(current.reminderTime)
        return Result.success()
    }

    private suspend fun showNotification() {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        val from = today.atStartOfDayIn(zone).toEpochMilliseconds()
        val to = today.plus(DatePeriod(days = 1)).atStartOfDayIn(zone).toEpochMilliseconds()
        val dao = database.expenseDao()
        val count = dao.countBetween(from, to)
        val total = Money(dao.sumBetween(from, to))

        val (title, text) = if (count == 0) {
            "Nothing logged today" to "Spent anything today? Add it before you forget."
        } else {
            "You spent ${MoneyFormatter.format(total, showFraction = false)} today" to
                "$count ${if (count == 1) "expense" else "expenses"} logged. Tap to see your daily report."
        }

        ensureChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REPORT, true)
        }
        val pendingIntent = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(REMINDER_CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "End-of-day reminder to log your expenses"
        }
        manager.createNotificationChannel(channel)
    }
}

class AndroidBackgroundSyncScheduler(private val context: Context) : BackgroundSyncScheduler {

    override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(SYNC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK)
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val sync: SyncRepository by inject()

    // Failures are retried by the next periodic run or the next app launch.
    override suspend fun doWork(): Result {
        sync.syncNow()
        return Result.success()
    }
}
