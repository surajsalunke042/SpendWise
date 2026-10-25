package com.surajdev.spendwise.app

import com.surajdev.spendwise.app.di.AppScope
import com.surajdev.spendwise.core.data.sync.SyncRepositoryImpl
import com.surajdev.spendwise.core.domain.platform.BackgroundSyncScheduler
import com.surajdev.spendwise.core.domain.platform.ReminderScheduler
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/** One-time startup work, called by each platform's entry point after Koin starts. */
object AppInitializer : KoinComponent {
    private var started = false

    fun start() {
        if (started) return
        started = true
        val scope = get<AppScope>()
        scope.launch {
            get<CategoryRepository>().ensureDefaults()
            val settings = get<SettingsRepository>().settings.first()
            if (settings.reminderEnabled) get<ReminderScheduler>().schedule(settings.reminderTime)
        }
        get<SyncRepositoryImpl>().start()
        get<BackgroundSyncScheduler>().schedulePeriodic()
    }
}
