package com.sdamashchuk.mathbubbles.core.ui.component.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
data class GlassButtonStyle(
    val fillColor: Color,
    val rimTopColor: Color,
    val rimBottomColor: Color,
    val rimWidth: Dp,
    val cornerRadius: Dp,
)
