package com.sdomashchuk.mathclicker.domain.repository

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign
import com.sdomashchuk.mathclicker.domain.model.game.session.GameField
import com.sdomashchuk.mathclicker.domain.model.game.session.GameSession
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams

interface GameRepository {
    suspend fun initGameField(): GameField
    suspend fun updateGameField(gameField: GameField)
    suspend fun getNextSignAndDigit(level: Int): Pair<OperationSign, Int>
    suspend fun updateSignAndDigit(nextOperationSign: OperationSign, nextOperationDigit: Int)
    suspend fun updateLevel(level: Int)
    suspend fun updateScore(score: Int)

    suspend fun initTargetParams(gameField: GameField)
    suspend fun refreshTargetParams(gameField: GameField)
    suspend fun updateTargetParams(targetParams: TargetParams)
    suspend fun updateTargetParamsList(targetParamsList: List<TargetParams>)
    suspend fun deleteTargetParams()
    suspend fun getTargetParams(gameFieldId: Int): List<TargetParams>

    suspend fun getSessionById(id: Int): GameSession
    suspend fun getUnfinishedSession(): GameSession?
}