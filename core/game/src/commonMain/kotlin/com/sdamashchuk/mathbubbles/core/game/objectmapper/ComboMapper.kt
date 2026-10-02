package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.model.Field

private const val COMBO_RAISE_THRESHOLD = 2
internal const val MAX_COMBO_MULTIPLIER = 5

internal fun Field.applyCombo(changedTargetCount: Int): Field =
    if (changedTargetCount >= COMBO_RAISE_THRESHOLD) {
        copy(bonusMultiplier = (bonusMultiplier + 1).coerceAtMost(MAX_COMBO_MULTIPLIER - 1))
    } else {
        resetStreak()
    }

internal fun Field.resetStreak(): Field = copy(bonusMultiplier = 0)
