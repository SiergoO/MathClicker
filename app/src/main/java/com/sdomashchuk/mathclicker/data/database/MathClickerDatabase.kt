package com.sdomashchuk.mathclicker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sdomashchuk.mathclicker.data.database.dao.FieldDao
import com.sdomashchuk.mathclicker.data.database.dao.TargetsDao
import com.sdomashchuk.mathclicker.data.database.entity.FieldDataModel
import com.sdomashchuk.mathclicker.data.database.entity.TargetsDataModel

@Database(entities = [FieldDataModel::class, TargetsDataModel::class], version = 1, exportSchema = false)
abstract class MathClickerDatabase : RoomDatabase() {

    abstract fun fieldDao(): FieldDao
    abstract fun targetsDao(): TargetsDao
}