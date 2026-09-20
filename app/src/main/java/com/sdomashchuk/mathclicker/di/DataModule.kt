package com.sdomashchuk.mathclicker.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sdomashchuk.mathclicker.data.database.dao.FieldDao
import com.sdomashchuk.mathclicker.data.database.dao.TargetsDao
import com.sdomashchuk.mathclicker.data.database.local.FieldQueries
import com.sdomashchuk.mathclicker.data.database.local.MathClickerDatabase
import com.sdomashchuk.mathclicker.data.database.local.TargetsQueries
import com.sdomashchuk.mathclicker.data.repository.GameRepositoryImpl
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule =
    module {
        // "Database" is the file name Room opened this database under on device. Keeping it is what
        // lets SQLDelight open an existing install's file in place instead of creating a new one.
        single<SqlDriver> { AndroidSqliteDriver(MathClickerDatabase.Schema, get<Context>(), "Database") }
        single<MathClickerDatabase> { MathClickerDatabase(get()) }
        single<FieldQueries> { get<MathClickerDatabase>().fieldQueries }
        single<TargetsQueries> { get<MathClickerDatabase>().targetsQueries }
        single { FieldDao(get()) }
        single { TargetsDao(get()) }
        single { GameRepositoryImpl(get(), get()) } bind GameRepository::class
    }
