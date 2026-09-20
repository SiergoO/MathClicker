package com.sdomashchuk.mathclicker.di

import android.content.Context
import androidx.room.Room
import com.sdomashchuk.mathclicker.data.database.MathClickerDatabase
import com.sdomashchuk.mathclicker.data.database.dao.FieldDao
import com.sdomashchuk.mathclicker.data.database.dao.TargetsDao
import com.sdomashchuk.mathclicker.data.repository.GameRepositoryImpl
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule =
    module {
        single {
            Room
                .databaseBuilder(
                    get<Context>(),
                    MathClickerDatabase::class.java,
                    "Database",
                ).build()
        }
        single<FieldDao> { get<MathClickerDatabase>().fieldDao() }
        single<TargetsDao> { get<MathClickerDatabase>().targetsDao() }
        single { GameRepositoryImpl(get(), get()) } bind GameRepository::class
    }
