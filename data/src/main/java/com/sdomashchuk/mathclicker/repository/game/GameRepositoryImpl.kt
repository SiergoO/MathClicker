package com.sdomashchuk.mathclicker.repository.game

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign
import com.sdomashchuk.mathclicker.domain.model.game.session.Session
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParam
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.helper.DifficultyHelper
import com.sdomashchuk.mathclicker.util.random

class GameRepositoryImpl(
    private val difficultyHelper: DifficultyHelper
) : GameRepository {
    override suspend fun getSession(): Session {
        val currentOperationSign = OperationSign.values().random()
        val currentOperationDigit = difficultyHelper.getOperationValueByLevel(currentOperationSign, 1)
        val nextOperationSign = OperationSign.values().random()
        val nextOperationDigit = difficultyHelper.getOperationValueByLevel(nextOperationSign, 1)
        return Session(
            level = 1,
            score = 0,
            bonusMultiplier = 0,
            currentOperationDigit = currentOperationDigit,
            currentOperationSign = currentOperationSign,
            nextOperationDigit = nextOperationDigit,
            nextOperationSign = nextOperationSign
        )
    }

    override suspend fun getTargetParams(level: Int): List<TargetParam> {
        return List(4) {
            TargetParam(
                it,
                difficultyHelper.getTargetValueByLevel(level),
                difficultyHelper.getTargetAnimationDurationMsByLevel(level)
            )
        }
    }

    override suspend fun updateSession(session: Session) {
        // TODO("Not yet implemented")
    }

    override suspend fun updateScore(score: Int) {
        // TODO("Not yet implemented")
    }
}