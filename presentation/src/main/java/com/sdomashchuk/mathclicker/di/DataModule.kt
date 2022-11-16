package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.helper.DifficultyHelper
import com.sdomashchuk.mathclicker.helper.DifficultyHelperImpl
import com.sdomashchuk.mathclicker.repository.game.GameRepositoryImpl
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DataModule {

    @Provides
    @Singleton
    fun provideGameRepository(difficultyHelper: DifficultyHelper): GameRepository = GameRepositoryImpl(difficultyHelper)

    @Provides
    @Singleton
    fun provideDifficultyHelper(): DifficultyHelper = DifficultyHelperImpl()
}

