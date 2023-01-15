package com.sdomashchuk.mathclicker.di

import android.content.Context
import androidx.room.Room
import com.sdomashchuk.mathclicker.data.database.dao.FieldDao
import com.sdomashchuk.mathclicker.data.database.MathClickerDatabase
import com.sdomashchuk.mathclicker.data.database.dao.TargetsDao
import com.sdomashchuk.mathclicker.data.repository.GameRepositoryImpl
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
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
            "Database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideFieldDao(database: MathClickerDatabase): FieldDao {
        return database.fieldDao()
    }

    @Provides
    @Singleton
    fun provideTargetsDao(database: MathClickerDatabase): TargetsDao {
        return database.targetsDao()
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        FieldDao: FieldDao,
        targetsDao: TargetsDao
    ): GameRepository =
        GameRepositoryImpl(
            FieldDao,
            targetsDao,
        )
}

