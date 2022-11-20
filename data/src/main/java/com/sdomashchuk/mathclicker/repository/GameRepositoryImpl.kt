package com.sdomashchuk.mathclicker.repository

import com.sdomashchuk.mathclicker.database.GameSessionDao
import com.sdomashchuk.mathclicker.database.TargetParamsDao
import com.sdomashchuk.mathclicker.domain.model.game.OperationSign
import com.sdomashchuk.mathclicker.domain.model.game.session.GameField
import com.sdomashchuk.mathclicker.domain.model.game.session.GameSession
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.helper.DifficultyHelper
import com.sdomashchuk.mathclicker.model.database.GameFieldDataModel
import com.sdomashchuk.mathclicker.model.database.toDataModel
import com.sdomashchuk.mathclicker.model.database.toDomainModel
import com.sdomashchuk.mathclicker.util.random
import com.sdomashchuk.mathclicker.util.toColumnId
import com.sdomashchuk.mathclicker.util.toSymbol

class GameRepositoryImpl(
    private val gameSessionDao: GameSessionDao,
    private val targetParamsDao: TargetParamsDao,
    private val difficultyHelper: DifficultyHelper
) : GameRepository {

    override suspend fun initGameField(): GameField {
        val currentOperationSign = OperationSign.values().random()
        val currentOperationDigit = difficultyHelper.getOperationValueByLevel(currentOperationSign, 1)
        val nextOperationSign = OperationSign.values().random()
        val nextOperationDigit = difficultyHelper.getOperationValueByLevel(nextOperationSign, 1)
        val gameField = GameFieldDataModel(
            currentOperationSign = currentOperationSign.toSymbol(),
            currentOperationDigit = currentOperationDigit,
            nextOperationSign = nextOperationSign.toSymbol(),
            nextOperationDigit = nextOperationDigit
        )
        gameSessionDao.insertGameField(gameField)
        return gameField.toDomainModel()
    }

    override suspend fun updateGameField(gameField: GameField) {
        gameSessionDao.updateGameField(gameField.toDataModel())
    }

    override suspend fun updateLevel(level: Int) {
        gameSessionDao.updateLevel(level)
    }

    override suspend fun updateScore(score: Int) {
        gameSessionDao.updateLevel(score)
    }

    override suspend fun initTargetParams(gameField: GameField) {
        val amount = difficultyHelper.getTargetAmountByLevel(gameField.level)
        repeat(amount) { id ->
            val tp = TargetParams(
                id + 1,
                gameField.id,
                id.toColumnId(),
                difficultyHelper.getTargetValueByLevel(gameField.level),
                0,
                difficultyHelper.getTargetAnimationDelayMsById(id),
                difficultyHelper.getTargetAnimationDurationMsByLevel(gameField.level)
            ).toDataModel()
            targetParamsDao.insertTargetParams(tp)
        }
    }

    override suspend fun updateTargetParams(targetParams: TargetParams) {
        targetParamsDao.updateTargetParams(targetParams.toDataModel())
    }

    override suspend fun deleteTargetParams() {
        targetParamsDao.deleteTargetParams()
    }

    override suspend fun getTargetParams(level: Int): List<TargetParams> {
        return targetParamsDao.getTargetParams().map { it.toDomainModel() }
    }

    override suspend fun getSessionById(id: Int): GameSession {
        return gameSessionDao.getSessionById(id).toDomainModel()
    }

    override suspend fun getUnfinishedSession(): GameSession? {
        return gameSessionDao.getUnfinishedSession()?.toDomainModel()
    }
}
