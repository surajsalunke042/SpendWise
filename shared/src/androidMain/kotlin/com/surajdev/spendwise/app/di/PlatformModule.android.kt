package com.surajdev.spendwise.app.di

import android.content.Context
import androidx.room3.Room
import com.surajdev.spendwise.core.data.local.PREFERENCES_FILE_NAME
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.domain.platform.BackgroundSyncScheduler
import com.surajdev.spendwise.core.domain.platform.ReminderScheduler
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.core.platform.AndroidBackgroundSyncScheduler
import com.surajdev.spendwise.core.platform.AndroidReminderScheduler
import com.surajdev.spendwise.core.remote.NoOpSyncRemoteDataSource
import com.surajdev.spendwise.core.remote.SyncRemoteDataSource
import com.surajdev.spendwise.core.remote.UnavailableAuthRepository
import com.surajdev.spendwise.core.remote.firebase.FirebaseAuthRepository
import com.surajdev.spendwise.core.remote.firebase.FirestoreSyncRemoteDataSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.apps
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single {
        val context = androidContext()
        Room.databaseBuilder<SpendWiseDatabase>(context, context.getDatabasePath(SpendWiseDatabase.FILE_NAME).absolutePath)
    }
    single { PlatformPaths(preferencesFile = androidContext().filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single<BackgroundSyncScheduler> { AndroidBackgroundSyncScheduler(androidContext()) }

    // Firebase is only initialized when google-services.json was present at build time.
    single<AuthRepository> {
        if (isFirebaseConfigured(androidContext())) FirebaseAuthRepository() else UnavailableAuthRepository()
    }
    single<SyncRemoteDataSource> {
        if (isFirebaseConfigured(androidContext())) FirestoreSyncRemoteDataSource() else NoOpSyncRemoteDataSource()
    }
}

private fun isFirebaseConfigured(context: Context): Boolean =
    runCatching { Firebase.apps(context).isNotEmpty() }.getOrDefault(false)
