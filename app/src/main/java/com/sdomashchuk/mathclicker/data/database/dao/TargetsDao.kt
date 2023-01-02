package com.sdomashchuk.mathclicker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sdomashchuk.mathclicker.data.database.entity.TargetsDataModel

@Dao
interface TargetsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: TargetsDataModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<TargetsDataModel>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTarget(target: TargetsDataModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTargets(targets: List<TargetsDataModel>)

    @Query("DELETE FROM targets")
    suspend fun deleteTargets()

    @Query("SELECT * FROM targets ORDER BY id DESC")
    suspend fun getTargets(): List<TargetsDataModel>
}