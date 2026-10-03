package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelper
import com.sdamashchuk.mathbubbles.core.model.OperationSign

class SteadySessionHelper : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetFlightTimeMsRange = 20000..40000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ) = 10

    override fun getTargetSpeedByLevel(level: Int) = 1f / 1000

    override fun getTargetFlightTimeMs(level: Int) = 1000

    override fun getFinishSpacingMsByLevel(level: Int) = 2000

    override fun getOpeningOffsetMsByLevel(level: Int) = 1000

    override fun getTargetAmountByLevel(level: Int) = level + 1

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = 2

    override fun getDivisionDigitByLevel(level: Int) = 2

    override fun getSubtractionDigitByLevel(level: Int) = 3

    override fun failedGrowthCap(level: Int) = 1_000_000
}
