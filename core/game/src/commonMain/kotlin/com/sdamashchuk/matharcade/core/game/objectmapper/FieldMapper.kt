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

// ASK-10: combo is a streak over presses, not targets - a ceiling this far out is an
// implementation call rather than the owner's, named here so it is one edit to change.
private const val STREAK_CAP = 10

internal fun Field.advanceStreak(pressFailed: Boolean): Field =
    this.copy(bonusMultiplier = if (pressFailed) 0 else (bonusMultiplier + 1).coerceAtMost(STREAK_CAP))

internal fun Field.resetStreak(): Field = this.copy(bonusMultiplier = 0)
