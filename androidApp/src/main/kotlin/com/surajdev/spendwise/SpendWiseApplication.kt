package com.surajdev.spendwise

import android.app.Application
import com.surajdev.spendwise.app.AppInitializer
import com.surajdev.spendwise.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class SpendWiseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@SpendWiseApplication)
        }
        AppInitializer.start()
    }
}
