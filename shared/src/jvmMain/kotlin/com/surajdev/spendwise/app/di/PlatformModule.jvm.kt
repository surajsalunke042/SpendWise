package com.surajdev.spendwise.app.di

import androidx.room3.Room
import com.surajdev.spendwise.core.data.local.PREFERENCES_FILE_NAME
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.domain.platform.BackgroundSyncScheduler
import com.surajdev.spendwise.core.domain.platform.ReminderScheduler
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.core.remote.NoOpSyncRemoteDataSource
import com.surajdev.spendwise.core.remote.SyncRemoteDataSource
import com.surajdev.spendwise.core.remote.UnavailableAuthRepository
import kotlinx.datetime.LocalTime
import org.koin.dsl.module
import java.io.File

/** JVM target: used for fast tests and local previews. No Firebase, no notifications. */
actual val platformModule = module {
    single {
        val dir = File(System.getProperty("user.home"), ".spendwise").apply { mkdirs() }
        Room.databaseBuilder<SpendWiseDatabase>(name = File(dir, SpendWiseDatabase.FILE_NAME).absolutePath)
    }
    single {
        val dir = File(System.getProperty("user.home"), ".spendwise").apply { mkdirs() }
        PlatformPaths(preferencesFile = File(dir, PREFERENCES_FILE_NAME).absolutePath)
    }
    single<ReminderScheduler> {
        object : ReminderScheduler {
            override fun schedule(time: LocalTime) = Unit
            override fun cancel() = Unit
        }
    }
    single<BackgroundSyncScheduler> {
        object : BackgroundSyncScheduler {
            override fun schedulePeriodic() = Unit
            override fun cancel() = Unit
        }
    }
    single<AuthRepository> { UnavailableAuthRepository() }
    single<SyncRemoteDataSource> { NoOpSyncRemoteDataSource() }
}
