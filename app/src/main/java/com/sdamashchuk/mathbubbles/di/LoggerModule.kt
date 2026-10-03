package com.sdamashchuk.mathbubbles.di

import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import org.koin.dsl.module

val loggerModule =
    module {
        single<Logger> { AndroidLogger() }
    }
