package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sdamashchuk.mathbubbles.core.ui.component.model.AmbientBubbleStyle
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterMote

fun DrawScope.drawAmbientBubbles(
    style: AmbientBubbleStyle,
    timeMs: Long,
) {
    repeat(style.count) { index ->
        val progress = ambientBubbleProgress(style, index, timeMs)
        val alpha = ambientBubbleAlpha(style, index, progress)
        if (alpha <= 0f) return@repeat
        drawCircle(
            color = WaterMote.copy(alpha = alpha),
            radius = size.minDimension * ambientBubbleRadiusFraction(style, index),
            center =
                Offset(
                    x = size.width * ambientBubbleCenterXFraction(index, timeMs),
                    y = size.height * (1f - progress),
                ),
        )
    }
}
