package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.ui.component.model.AmbientBubbleStyle

internal val FieldAmbientBubbleStyle =
    AmbientBubbleStyle(
        count = 14,
        slowestRiseMs = 14_000f,
        fastestRiseMs = 6_000f,
        smallestRadiusFraction = 0.0035f,
        largestRadiusFraction = 0.013f,
        minAlpha = 0.05f,
        maxAlpha = 0.20f,
    )
