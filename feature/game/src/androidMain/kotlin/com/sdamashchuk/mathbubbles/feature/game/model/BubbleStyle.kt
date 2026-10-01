package com.sdamashchuk.mathbubbles.feature.game.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
data class BubbleStyle(
    val fillColor: Color,
    val fillAlpha: Float,
    val rimColor: Color,
    val rimWidth: Dp,
    val highlight: BubbleHighlight? = null,
)
