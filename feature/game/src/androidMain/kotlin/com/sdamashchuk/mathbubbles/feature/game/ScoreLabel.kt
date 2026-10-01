package com.sdamashchuk.mathbubbles.feature.game

internal fun scoreLabel(
    appliedMultiplier: Int,
    plainScore: String,
    comboScore: String,
): String = if (appliedMultiplier > 1) comboScore else plainScore
