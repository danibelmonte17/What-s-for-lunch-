package com.whatsforlunch.app

import android.app.Application
import com.whatsforlunch.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WhatsForLunchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WhatsForLunchApplication)
            modules(appModule)
        }
    }
}
