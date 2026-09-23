package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign

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

// Mirrors SessionHelperImpl's LEVEL_MAX: past level 1334 getTargetLifetimeMsByLevel's min/max
// thresholds cross and IntRange.random() throws on an empty range. Enforced here, not on the
// helper, because updateLevel is the only place a level is ever incremented.
private const val LEVEL_MAX = 999

internal fun Field.updateLevel(maxLevel: Int = LEVEL_MAX): Field =
    this.copy(
        level = (level + 1).coerceAtMost(maxLevel),
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
