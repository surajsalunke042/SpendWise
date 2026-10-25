package com.surajdev.spendwise.core.data.sync

import androidx.room3.withWriteTransaction
import com.surajdev.spendwise.core.data.local.AppPreferences
import com.surajdev.spendwise.core.database.SpendWiseDatabase
import com.surajdev.spendwise.core.domain.model.SyncStatus
import com.surajdev.spendwise.core.domain.repository.AuthRepository
import com.surajdev.spendwise.core.domain.repository.CategoryRepository
import com.surajdev.spendwise.core.domain.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Owns when sync runs: shortly after local changes, right after sign-in, when the app comes to the
 * foreground (see App.kt), and every few minutes while the app is open. Android also schedules a
 * periodic background sync through WorkManager.
 */
class SyncRepositoryImpl(
    private val auth: AuthRepository,
    private val engine: SyncEngine,
    private val database: SpendWiseDatabase,
    private val categories: CategoryRepository,
    private val preferences: AppPreferences,
    private val clock: Clock,
    private val appScope: CoroutineScope,
) : SyncRepository {

    private val mutex = Mutex()
    private val running = MutableStateFlow(false)
    private val lastError = MutableStateFlow<String?>(null)
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var started = false

    private val pendingCount: Flow<Int> = combine(
        database.expenseDao().observePendingCount(),
        database.categoryDao().observePendingCount(),
    ) { expenses, categories -> expenses + categories }.distinctUntilChanged()

    override val status: Flow<SyncStatus> =
        if (!auth.isAvailable) {
            flowOf(SyncStatus.Unavailable)
        } else {
            combine(auth.currentUser, running, lastError, pendingCount, preferences.lastSyncedAt) { user, isRunning, error, pending, lastSynced ->
                when {
                    user == null -> SyncStatus.SignedOut
                    isRunning -> SyncStatus.Syncing
                    error != null -> SyncStatus.Failed(error, pending)
                    else -> SyncStatus.Idle(lastSynced, pending)
                }
            }.distinctUntilChanged()
        }

    @OptIn(FlowPreview::class)
    fun start() {
        if (started || !auth.isAvailable) return
        started = true
        appScope.launch {
            requests.debounce(DEBOUNCE).collect { syncNow() }
        }
        appScope.launch {
            auth.currentUser.map { it?.uid }.distinctUntilChanged().filterNotNull().collect { requestSync() }
        }
        appScope.launch {
            pendingCount.filter { it > 0 }.collect { requestSync() }
        }
        appScope.launch {
            while (isActive) {
                delay(if (lastError.value != null) RETRY_INTERVAL else FOREGROUND_INTERVAL)
                requestSync()
            }
        }
    }

    override suspend fun syncNow(): Boolean = mutex.withLock {
        val uid = auth.currentUserId() ?: return false
        running.value = true
        try {
            engine.sync(uid)
            lastError.value = null
            preferences.setLastSyncedAt(clock.now())
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastError.value = e.message ?: "Sync failed"
            false
        } finally {
            running.value = false
        }
    }

    override fun requestSync() {
        requests.tryEmit(Unit)
    }

    override suspend fun signOut(keepLocalData: Boolean) = mutex.withLock {
        auth.signOut()
        if (!keepLocalData) {
            database.withWriteTransaction {
                database.expenseDao().deleteAll()
                database.categoryDao().deleteAll()
                database.syncCursorDao().clear()
            }
            categories.ensureDefaults()
            preferences.setLastSyncedUid(null)
        }
        preferences.setLastSyncedAt(null)
        lastError.value = null
    }

    private companion object {
        val DEBOUNCE = 2.seconds
        val FOREGROUND_INTERVAL = 15.minutes
        val RETRY_INTERVAL = 2.minutes
    }
}
