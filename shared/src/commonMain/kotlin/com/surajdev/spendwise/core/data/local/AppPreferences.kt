package com.surajdev.spendwise.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.surajdev.spendwise.core.common.IdGenerator
import com.surajdev.spendwise.core.domain.model.PaymentMethod
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalTime
import okio.Path.Companion.toPath
import kotlin.time.Instant

const val PREFERENCES_FILE_NAME = "spendwise.preferences_pb"

fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

/** Key-value storage for user settings and internal sync bookkeeping. */
class AppPreferences(
    private val dataStore: DataStore<Preferences>,
    private val idGenerator: IdGenerator,
) {
    private val deviceIdLock = Mutex()
    private var cachedDeviceId: String? = null

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            reminderEnabled = prefs[REMINDER_ENABLED] ?: false,
            reminderTime = prefs[REMINDER_MINUTES]?.let { LocalTime(it / 60, it % 60) }
                ?: UserSettings.DEFAULT_REMINDER_TIME,
            themeMode = prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM,
            reminderPromptDismissed = prefs[REMINDER_PROMPT_DISMISSED] ?: false,
            lastUsedCategoryId = prefs[LAST_CATEGORY],
            lastUsedPaymentMethod = PaymentMethod.fromNameOrNull(prefs[LAST_PAYMENT_METHOD]),
        )
    }

    suspend fun setReminder(enabled: Boolean, time: LocalTime) {
        dataStore.edit {
            it[REMINDER_ENABLED] = enabled
            it[REMINDER_MINUTES] = time.hour * 60 + time.minute
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun dismissReminderPrompt() {
        dataStore.edit { it[REMINDER_PROMPT_DISMISSED] = true }
    }

    suspend fun rememberLastUsed(categoryId: String, paymentMethod: PaymentMethod?) {
        dataStore.edit {
            it[LAST_CATEGORY] = categoryId
            if (paymentMethod != null) it[LAST_PAYMENT_METHOD] = paymentMethod.name else it.remove(LAST_PAYMENT_METHOD)
        }
    }

    /** Stable random ID for this installation; used as a tiebreaker in conflict resolution. */
    suspend fun deviceId(): String = cachedDeviceId ?: deviceIdLock.withLock {
        cachedDeviceId ?: run {
            val stored = dataStore.data.first()[DEVICE_ID]
            val id = stored ?: idGenerator.newId().also { newId -> dataStore.edit { it[DEVICE_ID] = newId } }
            cachedDeviceId = id
            id
        }
    }

    suspend fun lastSyncedUid(): String? = dataStore.data.first()[LAST_SYNCED_UID]

    suspend fun setLastSyncedUid(uid: String?) {
        dataStore.edit {
            if (uid != null) it[LAST_SYNCED_UID] = uid else it.remove(LAST_SYNCED_UID)
        }
    }

    val lastSyncedAt: Flow<Instant?> = dataStore.data.map { prefs ->
        prefs[LAST_SYNCED_AT]?.let(Instant::fromEpochMilliseconds)
    }

    suspend fun setLastSyncedAt(instant: Instant?) {
        dataStore.edit {
            if (instant != null) it[LAST_SYNCED_AT] = instant.toEpochMilliseconds() else it.remove(LAST_SYNCED_AT)
        }
    }

    private companion object {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REMINDER_PROMPT_DISMISSED = booleanPreferencesKey("reminder_prompt_dismissed")
        val LAST_CATEGORY = stringPreferencesKey("last_category")
        val LAST_PAYMENT_METHOD = stringPreferencesKey("last_payment_method")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val LAST_SYNCED_UID = stringPreferencesKey("last_synced_uid")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }
}
