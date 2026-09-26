package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.INITIAL_LIFE_COUNT
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

// The whole-axis rewind effect from MC-71's design doc: gameTimeMs -= x, with every target's
// derived position/visibility/breakout rising back up on its own - no target is touched. Clamped at
// 0, the clock's own origin, rather than allowed negative. isClosed, finishedAt and lifeCount are
// deliberately untouched: they are real, owned state written once by closeIfNecessary and
// decrementLifeCount, never derived from gameTimeMs, so winding the clock back cannot reopen a
// closed field or hand back a spent life - the resurrection this operation must never cause.
internal fun Field.rewind(byMs: Long): Field = copy(gameTimeMs = (gameTimeMs - byMs).coerceAtLeast(0L))

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

// Capped at Field's own starting lifeCount (see Field.kt) rather than an unbounded bank: the grant
// is a recovery mechanic, not a second growth curve, and staying at or below the start keeps the
// existing 3-slot life HUD (Field.kt in :feature:game) correct with no further change.
private const val LIFE_BONUS_CAP = INITIAL_LIFE_COUNT

// The only place a level is ever incremented (see LEVEL_MAX above). Restoring a session never calls
// this. MC-54's every-N-levels life bonus lived here too until MC-76 removed the trigger - leveling
// up is now purely a level change, and grantLife below is the only way lifeCount recovers.
internal fun Field.updateLevel(maxLevel: Int = LEVEL_MAX): Field = copy(level = (level + 1).coerceAtMost(maxLevel))

// The mechanism MC-54's automatic grant used to drive, kept after MC-76 removed the every-N-levels
// trigger: the owner wants the ability to hand back a life for whatever random event eventually
// wants one, without re-deciding the cap semantics. coerceAtMost, not a guard that refuses the call
// outright, so a no-op grant is still a well-defined Field - Game.grantLife is what turns "no-op"
// into "no event emitted".
internal fun Field.grantLife(cap: Int = LIFE_BONUS_CAP): Field = copy(lifeCount = (lifeCount + 1).coerceAtMost(cap))

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

/**
 * Replaces the multiplier with this press's own count, rather than accumulating across presses.
 */
internal fun Field.applyCombo(scored: Int): Field = this.copy(bonusMultiplier = scored)

internal fun Field.resetStreak(): Field = this.copy(bonusMultiplier = 0)
