package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.domain.usecase.UseCase
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateLevelUseCase
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateLevelUseCaseParam
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateLevelUseCaseResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
object UseCaseModule {

    @Provides
    fun provideUpdateLevelUseCase(gameRepository: GameRepository): UseCase<UpdateLevelUseCaseParam, UpdateLevelUseCaseResult> {
        return UpdateLevelUseCase(gameRepository)
    }
}
