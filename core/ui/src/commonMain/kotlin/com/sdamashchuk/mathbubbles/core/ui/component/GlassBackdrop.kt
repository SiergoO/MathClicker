package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterMote

private const val MOTE_COUNT = 9
private const val MIN_RADIUS_FRACTION = 0.03f
private const val MAX_RADIUS_FRACTION = 0.09f
private const val MIN_ALPHA = 0.05f
private const val MAX_ALPHA = 0.16f

private const val SALT_X = 1
private const val SALT_Y = 2
private const val SALT_RADIUS = 3
private const val SALT_ALPHA = 4

@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.drawWithCache { onDrawBehind { drawMotes() } }, content = content)
}

private fun DrawScope.drawMotes() {
    repeat(MOTE_COUNT) { index ->
        val radiusFraction =
            MIN_RADIUS_FRACTION + (MAX_RADIUS_FRACTION - MIN_RADIUS_FRACTION) * moteScatter(index, SALT_RADIUS)
        val alpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * moteScatter(index, SALT_ALPHA)
        val center =
            Offset(
                x = size.width * moteScatter(index, SALT_X),
                y = size.height * moteScatter(index, SALT_Y),
            )
        drawCircle(color = WaterMote, radius = size.minDimension * radiusFraction, center = center, alpha = alpha)
    }
}
