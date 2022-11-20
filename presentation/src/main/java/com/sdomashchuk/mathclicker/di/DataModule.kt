package com.sdomashchuk.mathclicker.di

import android.content.Context
import androidx.room.Room
import com.sdomashchuk.mathclicker.database.GameSessionDao
import com.sdomashchuk.mathclicker.database.MathClickerDatabase
import com.sdomashchuk.mathclicker.database.TargetParamsDao
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.helper.DifficultyHelper
import com.sdomashchuk.mathclicker.helper.DifficultyHelperImpl
import com.sdomashchuk.mathclicker.repository.GameRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DataModule {

    @Provides
    @Singleton
    fun provideMathClickerDatabase(@ApplicationContext context: Context): MathClickerDatabase {
        return Room.databaseBuilder(
            context,
            MathClickerDatabase::class.java,
            "MathClicker"
        ).build()
    }

    @Provides
    @Singleton
    fun provideGameSessionDao(database: MathClickerDatabase): GameSessionDao {
        return database.gameSessionDao()
    }

    @Provides
    @Singleton
    fun provideTargetParamsDao(database: MathClickerDatabase): TargetParamsDao {
        return database.targetParamsDao()
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        gameSessionDao: GameSessionDao,
        targetParamsDao: TargetParamsDao,
        difficultyHelper: DifficultyHelper
    ): GameRepository =
        GameRepositoryImpl(
            gameSessionDao,
            targetParamsDao,
            difficultyHelper
        )

    @Provides
    @Singleton
    fun provideDifficultyHelper(): DifficultyHelper = DifficultyHelperImpl()
}

