package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.domain.usecase.UseCase
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateScoreUseCase
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateScoreUseCaseParam
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateScoreUseCaseResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object UseCaseModule {

    @Provides
    @Singleton
    fun provideUpdateScoreUseCase(gameRepository: GameRepository): UseCase<UpdateScoreUseCaseParam, UpdateScoreUseCaseResult> {
        return UpdateScoreUseCase(gameRepository)
    }
}
