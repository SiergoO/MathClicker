package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.model.OperationSign

internal class FakeSessionHelper(
    private val targetAmount: Int = 1,
    private val targetValue: Int = 10,
    private val flightTimeMs: Int = 1000,
    private val finishSpacingMs: Int = 2000,
    private val openingOffsetMs: Int = 0,
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetFlightTimeMsRange = 20000..40000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    // Offset by (level - 1) so every value matches its old constant at level 1 - the level every
    // existing test runs at - while still diverging at any other level, which is what makes a level
    // argument silently swapped for a literal (createTargets, getNextSignAndDigit, recreateField)
    // observable.
    override fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ) = targetValue + (level - 1)

    override fun getTargetSpeedByLevel(level: Int) = 1f / (flightTimeMs + (level - 1))

    override fun getTargetFlightTimeMs(level: Int) = flightTimeMs + (level - 1)

    // finishSpacingMs comfortably above flightTimeMs (the fake's own default: 2000 vs 1000) so a
    // fresh target's own appearsAtMs never lands before the previous one's finishesAtMs - keeping
    // this fake's board honest about the invariant production code now enforces, not just its shape.
    override fun getFinishSpacingMsByLevel(level: Int) = finishSpacingMs

    override fun getOpeningOffsetMsByLevel(level: Int) = openingOffsetMs.coerceAtLeast(getTargetFlightTimeMs(level))

    override fun getTargetAmountByLevel(level: Int) = targetAmount + (level - 1)

    // Sign-dependent digits so the reproducibility test can notice a sign that came out different.
    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = if (operationSign == OperationSign.DIVISION) 2 + (level - 1) else 3 + (level - 1)

    override fun getDivisionDigitByLevel(level: Int) = 2 + (level - 1)

    override fun getSubtractionDigitByLevel(level: Int) = 3 + (level - 1)

    override fun failedGrowthCap(level: Int) = 1_000_000 + (level - 1)
}
