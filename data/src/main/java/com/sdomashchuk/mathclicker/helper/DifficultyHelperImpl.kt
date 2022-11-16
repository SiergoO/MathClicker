package com.sdomashchuk.mathclicker.helper

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign
import com.sdomashchuk.mathclicker.util.random

class DifficultyHelperImpl: DifficultyHelper {

    override val sessionLevelRange = IntRange(1, 1000)
    override val initialTargetValueRange = IntRange(1, 20)
    override val initialTargetAnimationDurationMsRange = IntRange(27000, 54000)
    override val initialLevelTargetAmountRange = 4
    override val initialDivisionValueRange = IntRange(2, 5)
    override val initialSubtractionValueRange = IntRange(1, 3)

    override fun getTargetValueByLevel(level: Int): Int {
        val minThreshold = initialTargetValueRange.first
        val maxThreshold = if (level in 980..sessionLevelRange.last) 999 else initialTargetValueRange.last + level
        return IntRange(minThreshold, maxThreshold).random()
    }

    override fun getTargetAnimationDurationMsByLevel(level: Int): Int {
        val minThreshold = initialTargetAnimationDurationMsRange.first - level * 25
        val maxThreshold = initialTargetAnimationDurationMsRange.last - level * 50
        return IntRange(minThreshold, maxThreshold).random()
    }

    override fun getOperationValueByLevel(operationSign: OperationSign, level: Int): Int =
        if (operationSign == OperationSign.DIVISION) getDivisionValueByLevel(level) else getSubtractionValueByLevel(level)

    override fun getDivisionValueByLevel(level: Int): Int {
        val minThreshold = initialDivisionValueRange.first + level / 20
        val maxThreshold = initialDivisionValueRange.last + level / 5
        return IntRange(minThreshold, maxThreshold).random()
    }

    override fun getSubtractionValueByLevel(level: Int): Int {
        val minThreshold = initialSubtractionValueRange.first + level / 20
        val maxThreshold = initialSubtractionValueRange.last + level / 3
        return IntRange(minThreshold, maxThreshold).random()
    }
}