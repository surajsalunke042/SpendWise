package com.surajdev.spendwise

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.surajdev.spendwise.app.App
import com.surajdev.spendwise.app.navigation.DeepLinks
import com.surajdev.spendwise.core.platform.EXTRA_OPEN_REPORT

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_REPORT, false) == true) {
            intent.removeExtra(EXTRA_OPEN_REPORT)
            DeepLinks.open(DeepLinks.Target.TODAY_REPORT)
        }
    }
}
