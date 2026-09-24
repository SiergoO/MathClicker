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

// Mirrors SessionHelperImpl's LEVEL_MAX. MC-52 floored the lifetime and wave-gap curves, so neither
// can cross into an empty range at any level any more; this cap is now needed only for the value and
// target-amount curves, which still climb without a ceiling of their own. Enforced here, not on the
// helper, because updateLevel is the only place a level is ever incremented.
private const val LEVEL_MAX = 999

internal fun Field.updateLevel(maxLevel: Int = LEVEL_MAX): Field =
    this.copy(
        level = (level + 1).coerceAtMost(maxLevel),
    )

// Saturating, not wrapping. With the streak uncapped (ASK-18) the per-press award grows without a
// bound of its own, and Int arithmetic that overflows wraps negative - which the old floor at zero
// would then have turned into a score of 0. Silently erasing the run of the one player good enough
// to reach the ceiling is the worst possible failure for a score chase, so the sum is taken in Long
// and clamped. The floor at zero stays for a negative award, which nothing produces today.
internal fun Field.updateScore(scoreToAdd: Int): Field =
    this.copy(
        score = (this.score.toLong() + scoreToAdd).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
    )

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

// Uncapped by decision (ASK-18): a long clean run is meant to be worth chasing, and a ceiling is
// exactly the point at which a score chase stops rewarding the players who are best at it. The
// arithmetic that an unbounded streak would otherwise overflow is saturated in updateScore.
internal fun Field.advanceStreak(pressFailed: Boolean): Field =
    this.copy(bonusMultiplier = if (pressFailed) 0 else bonusMultiplier + 1)

internal fun Field.resetStreak(): Field = this.copy(bonusMultiplier = 0)
