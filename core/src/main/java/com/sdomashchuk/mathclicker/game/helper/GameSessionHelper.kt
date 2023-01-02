package com.sdomashchuk.mathclicker.game.helper

import com.sdomashchuk.mathclicker.model.OperationSign

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
    fun getTargetAppearanceDelayMsById(id: Int): Int
    fun getTargetAmountByLevel(level: Int): Int
    fun getOperationDigitByLevel(operationSign: OperationSign, level: Int): Int
    fun getDivisionDigitByLevel(level: Int): Int
    fun getSubtractionDigitByLevel(level: Int): Int
}