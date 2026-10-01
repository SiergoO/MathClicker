package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.mathbubbles.core.model.OperationSign

internal fun Field.decrementLifeCount(decrement: Int): Field {
    val lifeCount = this.lifeCount - decrement
    return this.copy(
        lifeCount = lifeCount,
    )
}

// stepMs is already clamped to MAX_TICK_MS in Game.tick - do not clamp it again here.
internal fun Field.advanceClock(stepMs: Int): Field = copy(gameTimeMs = gameTimeMs + stepMs)

// isClosed, finishedAt and lifeCount are deliberately untouched: they are owned state, not derived
// from the clock, so winding it back cannot reopen a closed field or hand back a spent life.
internal fun Field.rewind(byMs: Long): Field = copy(gameTimeMs = (gameTimeMs - byMs).coerceAtLeast(0L))

// nowMs is injected so two engines seeded alike play out identically, wall-clock included. isClosed is
// checked, not just lifeCount, so a later call cannot overwrite an already-stamped finishedAt.
internal fun Field.closeIfNecessary(nowMs: Long): Field {
    val newlyClosed = !isClosed && lifeCount <= 0
    return this.copy(
        isClosed = isClosed || lifeCount <= 0,
        finishedAt = if (newlyClosed) nowMs else finishedAt,
    )
}

// Mirrors SessionHelperImpl's LEVEL_MAX, enforced here because this is the only place a level is
// ever incremented.
private const val LEVEL_MAX = 999

// Capped at the starting lifeCount: the life HUD has exactly that many slots.
private const val LIFE_BONUS_CAP = INITIAL_LIFE_COUNT

internal fun Field.updateLevel(maxLevel: Int = LEVEL_MAX): Field = copy(level = (level + 1).coerceAtMost(maxLevel))

// coerceAtMost rather than refusing the call, so a no-op grant is still a well-defined Field;
// Game.grantLife is what turns a no-op into no event.
internal fun Field.grantLife(cap: Int = LIFE_BONUS_CAP): Field = copy(lifeCount = (lifeCount + 1).coerceAtMost(cap))

// Summed in Long and clamped: Int overflow wraps negative, and the floor at zero would then turn the
// best run in the game into a score of 0.
internal fun Field.updateScore(scoreToAdd: Int): Field =
    this.copy(
        score = (this.score.toLong() + scoreToAdd).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
    )

internal fun Field.updateActionButtons(
    nextOperationSign: OperationSign,
    nextOperationDigit: Int,
    nextBooster: Booster? = null,
    boosterDropCounter: Int = this.boosterDropCounter,
): Field =
    this.copy(
        currentOperationSign = this.nextOperationSign,
        currentOperationDigit = this.nextOperationDigit,
        currentBooster = this.nextBooster,
        nextOperationSign = nextOperationSign,
        nextOperationDigit = nextOperationDigit,
        nextBooster = nextBooster,
        boosterDropCounter = boosterDropCounter,
        hasDroppedBoosterThisSession = hasDroppedBoosterThisSession || nextBooster != null,
    )

/**
 * Replaces the multiplier with this press's own count, rather than accumulating across presses.
 */
internal fun Field.applyCombo(scored: Int): Field = this.copy(bonusMultiplier = scored)

internal fun Field.resetStreak(): Field = this.copy(bonusMultiplier = 0)
