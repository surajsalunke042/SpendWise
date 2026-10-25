package com.surajdev.spendwise.app.di

import androidx.room3.Room
import com.surajdev.spendwise.core.data.local.PREFERENCES_FILE_NAME
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.domain.platform.BackgroundSyncScheduler
import com.surajdev.spendwise.core.domain.platform.ReminderScheduler
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.core.platform.IosReminderScheduler
import com.surajdev.spendwise.core.remote.NoOpSyncRemoteDataSource
import com.surajdev.spendwise.core.remote.SyncRemoteDataSource
import com.surajdev.spendwise.core.remote.UnavailableAuthRepository
import com.surajdev.spendwise.core.remote.firebase.FirebaseAuthRepository
import com.surajdev.spendwise.core.remote.firebase.FirestoreSyncRemoteDataSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.apps
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val platformModule = module {
    single { Room.databaseBuilder<SpendWiseDatabase>(name = "${documentDirectory()}/${SpendWiseDatabase.FILE_NAME}") }
    single { PlatformPaths(preferencesFile = "${documentDirectory()}/$PREFERENCES_FILE_NAME") }
    single<ReminderScheduler> { IosReminderScheduler() }
    single<BackgroundSyncScheduler> {
        // iOS syncs while the app is open (on launch, on foreground and after changes).
        object : BackgroundSyncScheduler {
            override fun schedulePeriodic() = Unit
            override fun cancel() = Unit
        }
    }

    // iOSApp.swift only calls FirebaseApp.configure() when GoogleService-Info.plist is bundled.
    single<AuthRepository> { if (isFirebaseConfigured()) FirebaseAuthRepository() else UnavailableAuthRepository() }
    single<SyncRemoteDataSource> {
        if (isFirebaseConfigured()) FirestoreSyncRemoteDataSource() else NoOpSyncRemoteDataSource()
    }
}

private fun isFirebaseConfigured(): Boolean = runCatching { Firebase.apps().isNotEmpty() }.getOrDefault(false)

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "Documents directory unavailable" }
}
