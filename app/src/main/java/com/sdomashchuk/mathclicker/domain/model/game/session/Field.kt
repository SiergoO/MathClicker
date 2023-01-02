package com.sdomashchuk.mathclicker.domain.model.game.session

import com.sdomashchuk.mathclicker.model.OperationSign

typealias GameField = com.sdomashchuk.mathclicker.model.Field
typealias DomainField = Field

data class Field(
    val id: Int = 1,
    val level: Int = 1,
    val score: Int = 0,
    val lifeCount: Int = 3,
    val bonusMultiplier: Int = 0,
    val currentOperationSign: OperationSign = OperationSign.DIVISION,
    val currentOperationDigit: Int = 0,
    val nextOperationSign: OperationSign = OperationSign.DIVISION,
    val nextOperationDigit: Int = 0,
    val gameColumnWidthPx: Int = 0,
    val gameColumnHeightPx: Int = 0,
    val isClosed: Boolean = false
)

fun DomainField.toGameModel() = GameField(
    id,
    level,
    score,
    lifeCount,
    bonusMultiplier,
    currentOperationSign,
    currentOperationDigit,
    nextOperationSign,
    nextOperationDigit,
    gameColumnWidthPx,
    gameColumnHeightPx,
    isClosed
)

fun GameField.toDomainModel() = DomainField(
    id,
    level,
    score,
    lifeCount,
    bonusMultiplier,
    currentOperationSign,
    currentOperationDigit,
    nextOperationSign,
    nextOperationDigit,
    gameColumnWidthPx,
    gameColumnHeightPx,
    isClosed
)