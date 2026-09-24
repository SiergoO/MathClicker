package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.model.OperationSign

internal class FakeSessionHelper(
    private val targetAmount: Int = 1,
    private val targetValue: Int = 10,
    private val appearanceDelayMs: Int = 0,
    private val appearanceDelayMsById: ((Int) -> Int)? = null,
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    // Offset by (level - 1) so every value matches its old constant at level 1 - the level every
    // existing test runs at - while still diverging at any other level, which is what makes a level
    // argument silently swapped for a literal (createTargets, getNextSignAndDigit, recreateField)
    // observable.
    override fun getTargetValueByLevel(level: Int) = targetValue + (level - 1)

    override fun getTargetLifetimeMsByLevel(level: Int) = 1000 + (level - 1)

    override fun getTargetAppearanceDelayMsById(id: Int) = appearanceDelayMsById?.invoke(id) ?: appearanceDelayMs

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
