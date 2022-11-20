package com.sdomashchuk.mathclicker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams
import com.sdomashchuk.mathclicker.model.database.GameFieldDataModel
import com.sdomashchuk.mathclicker.model.database.GameSessionDataModel
import com.sdomashchuk.mathclicker.model.database.TargetParamsDataModel

@Dao
interface TargetParamsDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTargetParams(targetParams: TargetParamsDataModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTargetParams(targetParams: TargetParamsDataModel)

    @Query("DELETE FROM targetParams")
    suspend fun deleteTargetParams()

    @Query("SELECT * FROM targetParams ORDER BY id DESC")
    suspend fun getTargetParams(): List<TargetParamsDataModel>
}