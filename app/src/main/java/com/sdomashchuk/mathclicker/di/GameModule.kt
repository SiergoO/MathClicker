package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.game.Game
import com.sdomashchuk.mathclicker.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.game.helper.SessionHelperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [UseCaseModule::class])
object GameModule {
    @Provides
    @Singleton
    fun provideGame(sessionHelper: SessionHelper): Game = Game(sessionHelper)

    @Provides
    @Singleton
    fun provideSessionHelper(): SessionHelper = SessionHelperImpl()
}