package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.model.OperationSign

interface SessionHelper {
    val levelRange: IntRange
    val initialTargetValueRange: IntRange

    val initialTargetFlightTimeMsRange: IntRange
    val initialTargetAmountRange: IntRange
    val initialDivisionValueRange: IntRange
    val initialSubtractionValueRange: IntRange

    // The divisor the value is shaped against. Division-armed boards only: the remainder this shapes
    // is meaningless against subtraction, which is not modular.
    fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int

    fun getSubtractionTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int = getTargetValueByLevel(level, operationDigit)

    // Fraction of a fall covered per millisecond; getTargetFlightTimeMs is its inverse.
    fun getTargetSpeedByLevel(level: Int): Float

    // The only randomized draw in target scheduling - everything else a target is scheduled against
    // is deterministic.
    fun getTargetFlightTimeMs(level: Int): Int

    // Deterministic: finishesAtMs(i) - finishesAtMs(i-1) is exactly this, by construction.
    fun getFinishSpacingMsByLevel(level: Int): Int

    // At least the level's maximum flight time, so the first target's appearsAtMs is never before
    // Field.gameTimeMs.
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
