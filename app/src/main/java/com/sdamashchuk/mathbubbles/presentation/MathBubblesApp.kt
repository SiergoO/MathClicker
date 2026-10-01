package com.sdamashchuk.mathbubbles.presentation

import android.app.Application
import com.sdamashchuk.mathbubbles.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MathBubblesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MathBubblesApp)
            modules(appModules)
        }
    }
}
