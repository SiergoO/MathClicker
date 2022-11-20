package com.sdomashchuk.mathclicker.domain.repository

import com.sdomashchuk.mathclicker.domain.model.game.session.GameField
import com.sdomashchuk.mathclicker.domain.model.game.session.GameSession
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams

interface GameRepository {
    suspend fun initGameField(): GameField
    suspend fun updateGameField(gameField: GameField)
    suspend fun updateLevel(level: Int)
    suspend fun updateScore(score: Int)

    suspend fun initTargetParams(gameField: GameField)
    suspend fun updateTargetParams(targetParams: TargetParams)
    suspend fun deleteTargetParams()
    suspend fun getTargetParams(level: Int): List<TargetParams>

    suspend fun getSessionById(id: Int): GameSession
    suspend fun getUnfinishedSession(): GameSession?
}