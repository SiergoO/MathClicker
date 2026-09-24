package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.model.OperationSign

interface SessionHelper {
    val levelRange: IntRange
    val initialTargetValueRange: IntRange
    val initialTargetLifetimeMsRange: IntRange
    val initialTargetAppearanceDelayMsRange: IntRange
    val initialTargetAmountRange: IntRange
    val initialDivisionValueRange: IntRange
    val initialSubtractionValueRange: IntRange

    // operationDigit is the divisor the value is generated against - MC-60: preparation cost is a
    // relationship between a target and the digit the player is about to press, not a property of
    // the target alone, so the generator needs to know which digit that is.
    fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int

    fun getTargetLifetimeMsByLevel(level: Int): Int

    fun getTargetAppearanceDelayMsByIdAndLevel(
        id: Int,
        level: Int,
    ): Int

    fun getTargetAmountByLevel(level: Int): Int

    fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ): Int

    fun getDivisionDigitByLevel(level: Int): Int

    fun getSubtractionDigitByLevel(level: Int): Int

    fun failedGrowthCap(level: Int): Int
}
