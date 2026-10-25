package com.surajdev.spendwise

import androidx.compose.ui.window.ComposeUIViewController
import com.surajdev.spendwise.app.App
import com.surajdev.spendwise.app.AppInitializer
import com.surajdev.spendwise.app.di.initKoin
import com.surajdev.spendwise.core.platform.NativeSignInProvider
import org.koin.dsl.module
import platform.UIKit.UIViewController

/** Called once from iOSApp.swift, after FirebaseApp.configure(). Swift: `IosEntryKt.startSpendWise(...)`. */
fun startSpendWise(nativeSignIn: NativeSignInProvider) {
    initKoin {
        modules(module { single<NativeSignInProvider> { nativeSignIn } })
    }
    AppInitializer.start()
}

/** Swift: `IosEntryKt.MainViewController()`. */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
