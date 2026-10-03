package com.sdamashchuk.mathbubbles.core.ui.component.model

import androidx.compose.runtime.Immutable

@Immutable
data class AmbientBubbleStyle(
    val count: Int,
    val slowestRiseMs: Float,
    val fastestRiseMs: Float,
    val smallestRadiusFraction: Float,
    val largestRadiusFraction: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
)
