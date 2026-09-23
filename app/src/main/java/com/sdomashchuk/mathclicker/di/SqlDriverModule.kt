package com.sdomashchuk.mathclicker.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sdomashchuk.mathclicker.core.database.local.MathClickerDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val sqlDriverModule =
    module {
        // "Database" is the file name Room opened this database under on device. Keeping it is what
        // lets SQLDelight open an existing install's file in place instead of creating a new one.
        // No callback argument needed: the default AndroidSqliteDriver.Callback already delegates
        // onUpgrade to MathClickerDatabase.Schema.migrate whenever the on-disk `user_version` is
        // below Schema.version, so a version bump alone (adding a .sqm) is enough to migrate every
        // existing install on next open — this construction never needed to change for that.
        single<SqlDriver> { AndroidSqliteDriver(MathClickerDatabase.Schema, get<Context>(), "Database") }
        // Dispatchers.IO, not Default: the game loop owns Default, and a blocking SQLite write
        // on a pool sized to the core count is what IO exists to keep out of it.
        single<CoroutineDispatcher> { Dispatchers.IO }
    }
