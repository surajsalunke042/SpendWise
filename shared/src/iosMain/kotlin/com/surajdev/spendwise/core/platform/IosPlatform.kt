package com.surajdev.spendwise.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.surajdev.spendwise.core.domain.platform.ReminderScheduler
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalTime
import org.koin.compose.koinInject
import platform.Foundation.NSDateComponents
import platform.UIKit.UIDevice
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

actual val platformName: String =
    UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion

/**
 * A repeating local notification. iOS can't run code at delivery time, so unlike Android the text
 * can't include today's total.
 */
class IosReminderScheduler : ReminderScheduler {
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(time: LocalTime) {
        val content = UNMutableNotificationContent().apply {
            title = "Time to log today's expenses"
            body = "Add anything you spent today, then check your daily report."
            sound = UNNotificationSound.defaultSound
        }
        val components = NSDateComponents().apply {
            hour = time.hour.toLong()
            minute = time.minute.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
        val request = UNNotificationRequest.requestWithIdentifier(REMINDER_ID, content, trigger)
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_ID))
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_ID))
    }

    private companion object {
        const val REMINDER_ID = "daily_reminder"
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(onResult: (granted: Boolean) -> Unit): () -> Unit =
    remember(onResult) {
        {
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ ->
                dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
            }
        }
    }

/** iOS manages status bar contrast from the root view controller; nothing to do here. */
@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) = Unit

/** Result passed back from Swift. Tokens are null when [cancelled] or [errorMessage] is set. */
class NativeSignInResult(
    val idToken: String?,
    /** Google: access token. Apple: the raw (unhashed) nonce used for the request. */
    val secondaryToken: String?,
    val errorMessage: String?,
    val cancelled: Boolean,
)

/** Implemented in Swift (iosApp/iosApp/NativeSignIn.swift) and passed in at startup. */
interface NativeSignInProvider {
    val supportsGoogle: Boolean

    fun signInWithGoogle(completion: (NativeSignInResult) -> Unit)

    fun signInWithApple(completion: (NativeSignInResult) -> Unit)
}

@Composable
actual fun rememberSocialSignInLauncher(): SocialSignInLauncher {
    val provider = koinInject<NativeSignInProvider>()
    return remember(provider) { IosSocialSignInLauncher(provider) }
}

private class IosSocialSignInLauncher(private val provider: NativeSignInProvider) : SocialSignInLauncher {
    override val supportsGoogle: Boolean get() = provider.supportsGoogle
    override val supportsApple: Boolean = true

    override suspend fun signInWithGoogle(): SocialSignInResult = suspendCancellableCoroutine { cont ->
        provider.signInWithGoogle { result ->
            cont.resume(result.toSocialResult { id, secondary -> SocialSignInResult.Google(id, secondary) })
        }
    }

    override suspend fun signInWithApple(): SocialSignInResult = suspendCancellableCoroutine { cont ->
        provider.signInWithApple { result ->
            cont.resume(
                result.toSocialResult { id, nonce ->
                    if (nonce == null) SocialSignInResult.Failed("Missing nonce") else SocialSignInResult.Apple(id, nonce)
                },
            )
        }
    }

    private inline fun NativeSignInResult.toSocialResult(
        success: (String, String?) -> SocialSignInResult,
    ): SocialSignInResult = when {
        cancelled -> SocialSignInResult.Cancelled
        errorMessage != null -> SocialSignInResult.Failed(errorMessage)
        idToken != null -> success(idToken, secondaryToken)
        else -> SocialSignInResult.Failed("Sign-in failed")
    }
}
