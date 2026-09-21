package com.sdomashchuk.mathclicker.core.game.objectmapper

import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.OperationSign

internal fun Field.incrementLifeCount(increment: Int): Field {
    val lifeCount = this.lifeCount + increment
    return this.copy(
        lifeCount = lifeCount,
    )
}

internal fun Field.decrementLifeCount(decrement: Int): Field {
    val lifeCount = this.lifeCount - decrement
    return this.copy(
        lifeCount = lifeCount,
    )
}

internal fun Field.closeIfNecessary(): Field =
    this.copy(
        isClosed = lifeCount <= 0,
    )

internal fun Field.updateGameColumnSize(
    width: Int,
    height: Int,
): Field =
    this.copy(
        gameColumnWidthPx = width,
        gameColumnHeightPx = height,
    )

internal fun Field.updateLevel(): Field =
    this.copy(
        level = level + 1,
    )

internal fun Field.updateScore(scoreToAdd: Int): Field {
    val finalScore = (this.score + scoreToAdd).let { if (it < 0) 0 else it }
    return this.copy(
        score = finalScore,
    )
}

internal fun Field.updateActionButtons(
    nextOperationSign: OperationSign,
    nextOperationDigit: Int,
): Field =
    this.copy(
        currentOperationSign = this.nextOperationSign,
        currentOperationDigit = this.nextOperationDigit,
        nextOperationSign = nextOperationSign,
        nextOperationDigit = nextOperationDigit,
    )
