package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.sdamashchuk.mathbubbles.core.ui.component.model.BubbleStyle
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterSurface

// The dock bubbles - the fire button, the preview and the stash slots - share one look: the
// targets' ready-state radial depth shading, a shade lighter than the water, with no specular spot.
private const val DOCK_FILL_ALPHA = 0.62f
private const val DOCK_RIM_WIDTH_FRACTION = 0.027f
private const val DOCK_CONVEXITY = 1f

internal fun dockBubbleStyle(
    rimColor: Color,
    diameter: Dp,
): BubbleStyle =
    BubbleStyle(
        fillColor = WaterSurface,
        fillAlpha = DOCK_FILL_ALPHA,
        rimColor = rimColor,
        rimWidth = diameter * DOCK_RIM_WIDTH_FRACTION,
        convexity = DOCK_CONVEXITY,
    )
