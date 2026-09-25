package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.model.OperationSign

interface SessionHelper {
    val levelRange: IntRange
    val initialTargetValueRange: IntRange

    // MC-73: replaces initialTargetLifetimeMsRange - same shape (level 1's own randomized range),
    // named for what the value now is: how long a target spends falling, not a countdown it owns.
    val initialTargetFlightTimeMsRange: IntRange
    val initialTargetAmountRange: IntRange
    val initialDivisionValueRange: IntRange
    val initialSubtractionValueRange: IntRange

    // operationDigit is the divisor the value is generated against - MC-60: preparation cost is a
    // relationship between a target and the digit the player is about to press, not a property of
    // the target alone, so the generator needs to know which digit that is. Division-armed boards
    // only (MC-70): the remainder this shapes is meaningless against subtraction, which is not
    // modular - see getSubtractionTargetValueByLevel below.
    fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int

    // MC-70: subtraction succeeds whenever value >= digit, a magnitude check with no remainder to
    // shift - reusing getTargetValueByLevel's residue math here would shape nothing real. Defaults to
    // delegating there so a fake that never overrides this keeps compiling and behaving exactly as it
    // always has.
    fun getSubtractionTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int = getTargetValueByLevel(level, operationDigit)

    // MC-73: the authoring knob - one deterministic number per level, no random draw. Fraction of a
    // fall covered per millisecond; getTargetFlightTimeMs is its inverse, with the per-target spread
    // applied to speed rather than to time (see that function).
    fun getTargetSpeedByLevel(level: Int): Float

    // The only randomized draw left in target scheduling: how long this one target is visible for,
    // in [base, base / 0.8] - a 0..-20% draw against the level's speed, landing as 0..+25% against
    // its flight time. Everything else a target is scheduled against (finishesAtMs, via
    // getFinishSpacingMsByLevel/getOpeningOffsetMsByLevel) is deterministic.
    fun getTargetFlightTimeMs(level: Int): Int

    // Deterministic, never random - see the MC-73 spec's "что из этого следует бесплатно". This is
    // the number that used to be erasable by a draw; it no longer can be, because it is not built
    // from one. finishesAtMs(i) - finishesAtMs(i-1) is exactly this value by construction.
    fun getFinishSpacingMsByLevel(level: Int): Int

    // At least the level's own maximum possible flight time, so the very first target's appearsAtMs
    // is never before Field.gameTimeMs - see the MC-73 spec's note on why zero-clamping would be
    // wrong (a target starting mid-fall) rather than the honest floor this is.
    fun getOpeningOffsetMsByLevel(level: Int): Int

    fun getTargetAmountByLevel(level: Int): Int

    fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ): Int

    fun getDivisionDigitByLevel(level: Int): Int

    fun getSubtractionDigitByLevel(level: Int): Int

    fun failedGrowthCap(level: Int): Int
}
