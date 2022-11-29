package com.sdomashchuk.mathclicker.domain.model.game.session

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign

data class GameField(
    val id: Int = 1,
    val level: Int = 1,
    val score: Int = 0,
    val lifeCount: Int = 3,
    val bonusMultiplier: Int = 0,
    val currentOperationSign: OperationSign = OperationSign.DIVISION,
    val currentOperationDigit: Int = 0,
    val nextOperationSign: OperationSign = OperationSign.DIVISION,
    val nextOperationDigit: Int = 0,
    val isClosed: Boolean = false
)