package com.sdamashchuk.matharcade.presentation

import android.app.Application
import com.sdamashchuk.matharcade.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MathArcadeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MathArcadeApp)
            modules(appModules)
        }
    }
}
