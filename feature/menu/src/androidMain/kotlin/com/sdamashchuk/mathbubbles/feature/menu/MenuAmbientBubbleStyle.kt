package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.ui.component.model.AmbientBubbleStyle

internal val MenuAmbientBubbleStyle =
    AmbientBubbleStyle(
        count = 22,
        slowestRiseMs = 22_000f,
        fastestRiseMs = 11_000f,
        smallestRadiusFraction = 0.012f,
        largestRadiusFraction = 0.045f,
        minAlpha = 0.12f,
        maxAlpha = 0.34f,
    )
