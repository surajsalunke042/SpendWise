package com.surajdev.spendwise.core.platform

import androidx.compose.runtime.Composable

sealed interface SocialSignInResult {
    data class Google(val idToken: String, val accessToken: String?) : SocialSignInResult
    data class Apple(val idToken: String, val rawNonce: String) : SocialSignInResult
    data object Cancelled : SocialSignInResult
    data class Failed(val message: String) : SocialSignInResult
}

/** Platform sign-in sheets (Credential Manager on Android, GoogleSignIn / AuthenticationServices on iOS). */
interface SocialSignInLauncher {
    val supportsGoogle: Boolean
    val supportsApple: Boolean

    suspend fun signInWithGoogle(): SocialSignInResult

    suspend fun signInWithApple(): SocialSignInResult
}

@Composable
expect fun rememberSocialSignInLauncher(): SocialSignInLauncher

/**
 * Returns a function that asks for notification permission (Android 13+ / iOS) and reports
 * whether it was granted. On platforms without a runtime permission it reports true.
 */
@Composable
expect fun rememberNotificationPermissionRequester(onResult: (granted: Boolean) -> Unit): () -> Unit

/** Keeps status/navigation bar icons readable for the in-app theme. */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)

/** Platform name shown in Settings → About. */
expect val platformName: String
