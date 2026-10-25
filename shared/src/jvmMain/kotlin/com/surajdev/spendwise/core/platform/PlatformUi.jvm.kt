package com.surajdev.spendwise.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual val platformName: String = "JVM ${System.getProperty("java.version")}"

@Composable
actual fun rememberSocialSignInLauncher(): SocialSignInLauncher = remember {
    object : SocialSignInLauncher {
        override val supportsGoogle = false
        override val supportsApple = false
        override suspend fun signInWithGoogle() = SocialSignInResult.Failed("Not supported on desktop")
        override suspend fun signInWithApple() = SocialSignInResult.Failed("Not supported on desktop")
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(onResult: (granted: Boolean) -> Unit): () -> Unit =
    remember(onResult) { { onResult(true) } }

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) = Unit
