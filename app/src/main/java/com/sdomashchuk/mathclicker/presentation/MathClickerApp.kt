package com.sdomashchuk.mathclicker.presentation

import android.app.Application
import com.sdomashchuk.mathclicker.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MathClickerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MathClickerApp)
            modules(appModules)
        }
    }
}
