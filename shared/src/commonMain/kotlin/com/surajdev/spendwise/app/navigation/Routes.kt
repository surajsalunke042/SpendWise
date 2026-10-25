package com.surajdev.spendwise.app.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ExpensesRoute
@Serializable data class ReportRoute(val epochDay: Long? = null)
@Serializable data object SettingsRoute
@Serializable data class AddExpenseRoute(val expenseId: String? = null)
@Serializable data object CategoriesRoute
@Serializable data object AuthRoute

/** Requests coming from outside the UI, e.g. tapping the daily reminder notification. */
object DeepLinks {
    enum class Target { TODAY_REPORT }

    private val _events = MutableSharedFlow<Target>(replay = 1, extraBufferCapacity = 1)
    val events: SharedFlow<Target> = _events

    fun open(target: Target) {
        _events.tryEmit(target)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun consumed() = _events.resetReplayCache()
}
