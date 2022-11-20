package com.sdomashchuk.mathclicker.helper

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign

interface DifficultyHelper {

    val sessionLevelRange: IntRange
    val initialTargetValueRange: IntRange
    val initialTargetAnimationDurationMsRange: IntRange
    val initialTargetAnimationDelayMsRange: IntRange
    val initialTargetAmountRange: IntRange
    val initialDivisionValueRange: IntRange
    val initialSubtractionValueRange: IntRange

    fun getTargetValueByLevel(level: Int): Int
    fun getTargetAnimationDurationMsByLevel(level: Int): Int
    fun getTargetAnimationDelayMsById(id: Int): Int
    fun getTargetAmountByLevel(level: Int): Int
    fun getOperationValueByLevel(operationSign: OperationSign, level: Int): Int
    fun getDivisionValueByLevel(level: Int): Int
    fun getSubtractionValueByLevel(level: Int): Int
}