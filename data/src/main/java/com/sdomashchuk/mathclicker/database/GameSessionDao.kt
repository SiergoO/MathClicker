package com.sdomashchuk.mathclicker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sdomashchuk.mathclicker.model.database.GameFieldDataModel
import com.sdomashchuk.mathclicker.model.database.GameSessionDataModel

@Dao
interface GameSessionDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGameField(gameField: GameFieldDataModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateGameField(gameField: GameFieldDataModel)

    @Transaction
    @Query("SELECT * FROM gameField ORDER BY id DESC")
    suspend fun getAllSessions(): List<GameSessionDataModel>

    @Transaction
    @Query("SELECT * FROM gameField WHERE isClosed=0")
    suspend fun getUnfinishedSession(): GameSessionDataModel?

    @Transaction
    @Query("SELECT * FROM gameField WHERE id=:gameSessionId")
    suspend fun getSessionById(gameSessionId: Int): GameSessionDataModel

    @Query("UPDATE gameField SET level=:level")
    suspend fun updateLevel(level: Int)

    @Query("UPDATE gameField SET score=:score")
    suspend fun updateScore(score: Int)
}