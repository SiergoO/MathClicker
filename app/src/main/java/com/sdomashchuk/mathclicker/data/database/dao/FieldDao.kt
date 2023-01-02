package com.sdomashchuk.mathclicker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sdomashchuk.mathclicker.data.database.entity.FieldDataModel

@Dao
interface FieldDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: FieldDataModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateField(field: FieldDataModel)

    @Transaction
    @Query("SELECT * FROM field ORDER BY id DESC")
    suspend fun getAllFields(): List<FieldDataModel>

    @Transaction
    @Query("SELECT * FROM field WHERE isClosed=0")
    suspend fun getUnfinishedField(): FieldDataModel?

    @Transaction
    @Query("SELECT * FROM field WHERE id=:sessionId")
    suspend fun getFieldById(sessionId: Int): FieldDataModel

    @Query("SELECT COUNT(id) FROM field")
    suspend fun getFieldCount(): Int
}