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

    fun getTargetValueByLevel(level: Int): Int

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
