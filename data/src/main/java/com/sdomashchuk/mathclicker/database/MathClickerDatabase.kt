package com.sdomashchuk.mathclicker.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sdomashchuk.mathclicker.model.database.GameFieldDataModel
import com.sdomashchuk.mathclicker.model.database.TargetParamsDataModel

@Database(entities = [GameFieldDataModel::class, TargetParamsDataModel::class], version = 1, exportSchema = false)
abstract class MathClickerDatabase : RoomDatabase() {

    abstract fun gameSessionDao(): GameSessionDao
    abstract fun targetParamsDao(): TargetParamsDao
}