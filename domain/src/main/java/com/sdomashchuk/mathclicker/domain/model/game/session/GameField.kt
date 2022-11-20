package com.sdomashchuk.mathclicker.domain.model.game.session

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign

data class GameField(
    val id: Int = 0,
    val level: Int = 0,
    val score: Int = 0,
    val bonusMultiplier: Int = 0,
    val currentOperationSign: OperationSign = OperationSign.DIVISION,
    val currentOperationDigit: Int = 0,
    val nextOperationSign: OperationSign = OperationSign.DIVISION,
    val nextOperationDigit: Int = 0
)