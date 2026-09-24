package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign

internal fun Field.decrementLifeCount(decrement: Int): Field {
    val lifeCount = this.lifeCount - decrement
    return this.copy(
        lifeCount = lifeCount,
    )
}

// The only place gameTimeMs ever moves. stepMs is Game.tick()'s own step, already clamped to
// MAX_TICK_MS there - clamping again here would be the same rule in two places, which is exactly
// what the design doc asks not to do.
internal fun Field.advanceClock(stepMs: Int): Field = copy(gameTimeMs = gameTimeMs + stepMs)

// nowMs comes from Game's own injected Clock rather than being read here, the same reason
// getNextSignAndDigit takes Game's Random instead of drawing its own - two engines seeded alike
// (GameSimulationTest, GameTest) must play out identically, wall-clock time included. isClosed is
// checked, not just lifeCount <= 0: resolveBreakouts calls this on every breakout, and a field that
// was already closed (lifeCount already <= 0 from an earlier call) must keep its original
// finishedAt rather than have a later, unrelated call overwrite it.
internal fun Field.closeIfNecessary(nowMs: Long): Field {
    val newlyClosed = !isClosed && lifeCount <= 0
    return this.copy(
        isClosed = isClosed || lifeCount <= 0,
        finishedAt = if (newlyClosed) nowMs else finishedAt,
    )
}

// Mirrors SessionHelperImpl's LEVEL_MAX. MC-52 floored the lifetime and wave-gap curves, so neither
// can cross into an empty range at any level any more; this cap is now needed only for the value and
// target-amount curves, which still climb without a ceiling of their own. Enforced here, not on the
// helper, because updateLevel is the only place a level is ever incremented.
private const val LEVEL_MAX = 999

// Starting point, not a tuned value (MC-54): three lives with no way to earn one back is a hard
// ceiling, but the owner hasn't picked a pace for lifting it. 5 is chosen so a bonus is reachable
// well inside a run - MC-52 already put a no-input level-1 session at ~12s, so this needs real
// playtesting data, not a guess, before it is treated as final.
private const val LIFE_BONUS_INTERVAL_LEVELS = 5

// Capped at Field's own starting lifeCount (see Field.kt) rather than an unbounded bank: the grant
// is a recovery mechanic, not a second growth curve, and staying at or below the start keeps the
// existing 3-slot life HUD (Field.kt in :feature:game) correct with no further change. Also a
// starting point, pending the same tuning as the interval above.
private const val LIFE_BONUS_CAP = 3

// The only place a level is ever incremented (see LEVEL_MAX above), so it is also the only place
// the every-N-levels life bonus can be granted - restoring a session never calls this, which is
// what keeps a qualifying level from granting a second time across a restore.
internal fun Field.updateLevel(maxLevel: Int = LEVEL_MAX): Field {
    val leveled = copy(level = (level + 1).coerceAtMost(maxLevel))
    return if (leveled.level % LIFE_BONUS_INTERVAL_LEVELS == 0) {
        leveled.copy(lifeCount = (leveled.lifeCount + 1).coerceAtMost(LIFE_BONUS_CAP))
    } else {
        leveled
    }
}

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
