package com.sdamashchuk.mathbubbles.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val sqlDriverModule =
    module {
        // Opened on first query, which the DAOs run on the IO dispatcher, not on the thread that builds the graph.
        single<SqlDriver> {
            val context = get<Context>()
            val logger = get<Logger>()
            LazySqlDriver { createRecoveringSqlDriver(context, logger) }
        }
        // Dispatchers.IO, not Default: the game loop owns Default, and a blocking SQLite write
        // on a pool sized to the core count is what IO exists to keep out of it.
        single<CoroutineDispatcher> { Dispatchers.IO }
    }
