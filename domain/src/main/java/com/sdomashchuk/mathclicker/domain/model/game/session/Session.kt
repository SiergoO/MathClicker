package com.sdomashchuk.mathclicker.domain.model.game.session

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign

data class Session(
    val level: Int = 1,
    val score: Int = 0,
    val bonusMultiplier: Int = 0,
    val currentOperationDigit: Int = 2,
    val currentOperationSign: OperationSign = OperationSign.DIVISION,
    val nextOperationDigit: Int = 3,
    val nextOperationSign: OperationSign = OperationSign.SUBTRACTION
)
