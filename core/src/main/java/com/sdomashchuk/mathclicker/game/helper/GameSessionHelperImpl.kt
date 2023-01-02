package com.sdomashchuk.mathclicker.game.helper

import com.sdomashchuk.mathclicker.model.OperationSign

class SessionHelperImpl: SessionHelper {

    companion object {
        private const val LEVEL_MIN = 1
        private const val LEVEL_MAX = 999

        private const val INITIAL_TARGET_VALUE_MIN = 1
        private const val INITIAL_TARGET_VALUE_MAX = 20

        private const val INITIAL_TARGET_LIFETIME_MS_MIN = 20000
        private const val INITIAL_TARGET_LIFETIME_MS_MAX = 40000

        private const val INITIAL_TARGET_APPEARANCE_DELAY_MS_MIN = 10000
        private const val INITIAL_TARGET_APPEARANCE_DELAY_MS_MAX = 20000

        private const val INITIAL_TARGET_AMOUNT_MIN = 6
        private const val INITIAL_TARGET_AMOUNT_MAX = 10

        private const val INITIAL_DIVISION_VALUE_MIN = 2
        private const val INITIAL_DIVISION_VALUE_MAX = 5

        private const val INITIAL_SUBTRACTION_VALUE_MIN = 1
        private const val INITIAL_SUBTRACTION_VALUE_MAX = 3

        private const val GAME_COLUMN_AMOUNT = 4
    }

    override val levelRange = IntRange(LEVEL_MIN, LEVEL_MAX)
    override val initialTargetValueRange = IntRange(INITIAL_TARGET_VALUE_MIN, INITIAL_TARGET_VALUE_MAX)
    override val initialTargetLifetimeMsRange = IntRange(INITIAL_TARGET_LIFETIME_MS_MIN, INITIAL_TARGET_LIFETIME_MS_MAX)
    override val initialTargetAppearanceDelayMsRange = IntRange(INITIAL_TARGET_APPEARANCE_DELAY_MS_MIN, INITIAL_TARGET_APPEARANCE_DELAY_MS_MAX)
    override val initialTargetAmountRange = IntRange(INITIAL_TARGET_AMOUNT_MIN, INITIAL_TARGET_AMOUNT_MAX)
    override val initialDivisionValueRange = IntRange(INITIAL_DIVISION_VALUE_MIN, INITIAL_DIVISION_VALUE_MAX)
    override val initialSubtractionValueRange = IntRange(INITIAL_SUBTRACTION_VALUE_MIN, INITIAL_SUBTRACTION_VALUE_MAX)

    /**
     * Calculates the target lifetime depending on the level. The higher the level, the less time the target should be
     * visible during the level.
     * @param level
     * @return random target lifetime in a certain range of values.
     */
    override fun getTargetLifetimeMsByLevel(level: Int): Int {
        val minThreshold = initialTargetLifetimeMsRange.first - level * 15
        val maxThreshold = initialTargetLifetimeMsRange.last - level * 30
        return IntRange(minThreshold, maxThreshold).random()
    }

    /**
     * Calculates the target appearance delay depending on the level. Resulting value depends on the number of
     * game columns for the sequential appearance of the targets in groups.
     * @param level
     * @return random target appearance delay in a certain range of values.
     */
    override fun getTargetAppearanceDelayMsById(id: Int): Int {
        val minThreshold = initialTargetAppearanceDelayMsRange.first * (id / GAME_COLUMN_AMOUNT)
        val maxThreshold = initialTargetAppearanceDelayMsRange.last * (id / GAME_COLUMN_AMOUNT)
        return IntRange(minThreshold, maxThreshold).random()
    }

    /**
     * Calculates the target value depending on the level. The higher the level, the higher target value should be.
     * @param level
     * @return random target value in a certain range of values.
     */
    override fun getTargetValueByLevel(level: Int): Int {
        val minThreshold = initialTargetValueRange.first + level / 10
        val maxThreshold = initialTargetValueRange.last + level * 3
        return IntRange(minThreshold, maxThreshold).random()
    }

    /**
     * Calculates the target amount should be displayed during the level depending on the level. The higher the level,
     * the more targets should appear during the level.
     * @param level
     * @return random target amount value in a certain range of values.
     */
    override fun getTargetAmountByLevel(level: Int): Int {
        val minThreshold = initialTargetAmountRange.first + level / 5
        val maxThreshold = initialTargetAmountRange.last + level / 3
        return IntRange(minThreshold, maxThreshold).random()
    }

    /**
     * Calculates the operation digit with known sign passed via parameters.
     * @param operationSign can be either division or subtraction. The calculation of operation digit depends on the
     * sign digit passed via params.
     * @param level
     * @return random division or subtraction digit in a certain range of values.
     */
    override fun getOperationDigitByLevel(operationSign: OperationSign, level: Int): Int =
        if (operationSign == OperationSign.DIVISION) getDivisionDigitByLevel(level) else getSubtractionDigitByLevel(level)

    /**
     * Calculates the division digit depending on the level.
     * @param level
     * @return random division digit in a certain range of values.
     */
    override fun getDivisionDigitByLevel(level: Int): Int {
        val minThreshold = initialDivisionValueRange.first + level / 10
        val maxThreshold = initialDivisionValueRange.last + level / 5
        return IntRange(minThreshold, maxThreshold).random()
    }

    /**
     * Calculates the subtraction digit depending on the level.
     * @param level
     * @return random subtraction digit in a certain range of values.
     */
    override fun getSubtractionDigitByLevel(level: Int): Int {
        val minThreshold = initialSubtractionValueRange.first + level / 10
        val maxThreshold = initialSubtractionValueRange.last + level / 3
        return IntRange(minThreshold, maxThreshold).random()
    }
}