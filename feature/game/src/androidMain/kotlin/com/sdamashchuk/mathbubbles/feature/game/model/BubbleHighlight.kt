package com.sdamashchuk.mathbubbles.feature.game.model

import androidx.compose.runtime.Immutable

@Immutable
data class BubbleHighlight(
    val coreAlpha: Float,
    val glowAlpha: Float,
    val offsetXFraction: Float,
    val offsetYFraction: Float,
    val rotationDegrees: Float = 0f,
)
