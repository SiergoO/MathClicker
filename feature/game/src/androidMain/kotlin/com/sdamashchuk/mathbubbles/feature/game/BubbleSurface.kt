package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterSurface
import com.sdamashchuk.mathbubbles.feature.game.model.BubbleStyle

private const val HIGHLIGHT_X_FRACTION = -0.34f
private const val HIGHLIGHT_Y_FRACTION = -0.33f
private const val HIGHLIGHT_CORE_RADIUS_FRACTION = 0.20f
private const val HIGHLIGHT_GLOW_RADIUS_FRACTION = 0.38f

// Upright, not tilted. The reference art's blob leans; on a falling object that lean reads as the
// bubble being askew, so the owner picked the straight ellipse from the design system instead.
private const val HIGHLIGHT_ECCENTRICITY = 1.2f

// The dock bubbles - the fire button, the preview and the stash slots - share one look: the
// targets' ready-state radial depth shading, a shade lighter than the water, with no specular spot.
private const val DOCK_FILL_ALPHA = 0.62f
private const val DOCK_RIM_WIDTH_FRACTION = 0.027f
private const val DOCK_CONVEXITY = 1f

private const val CONVEX_LIGHT_ALPHA = 0.16f
private const val CONVEX_SHADE_ALPHA = 0.22f

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

@Composable
fun BubbleSurface(
    style: BubbleStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.bubbleSurface(style),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

internal fun Modifier.bubbleSurface(style: BubbleStyle) =
    drawWithCache {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val highlightCenter =
            Offset(
                center.x + HIGHLIGHT_X_FRACTION * radius,
                center.y + HIGHLIGHT_Y_FRACTION * radius,
            )
        val rimWidthPx = style.rimWidth.toPx()

        // The stop positions are the fitted falloff (1 - d/R)^0.6 sampled at six radii, not a guess.
        val bodyBrush =
            Brush.radialGradient(
                0.00f to style.fillColor.copy(alpha = style.fillAlpha),
                0.25f to style.fillColor.copy(alpha = style.fillAlpha * 0.84f),
                0.50f to style.fillColor.copy(alpha = style.fillAlpha * 0.66f),
                0.75f to style.fillColor.copy(alpha = style.fillAlpha * 0.44f),
                0.90f to style.fillColor.copy(alpha = style.fillAlpha * 0.25f),
                1.00f to style.fillColor.copy(alpha = 0f),
                center = center,
                radius = radius,
            )
        val convexity = style.convexity
        val convexBrush =
            convexity?.let {
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = CONVEX_LIGHT_ALPHA * it),
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = CONVEX_SHADE_ALPHA * it),
                )
            }
        val highlight = style.highlight
        val glowBrush =
            highlight?.let {
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = it.glowAlpha),
                    1f to Color.White.copy(alpha = 0f),
                    center = highlightCenter,
                    radius = radius * HIGHLIGHT_GLOW_RADIUS_FRACTION,
                )
            }
        val coreBrush =
            highlight?.let {
                Brush.radialGradient(
                    0.00f to Color.White.copy(alpha = it.coreAlpha),
                    0.34f to Color.White.copy(alpha = it.coreAlpha),
                    0.62f to Color.White.copy(alpha = it.coreAlpha * 0.55f),
                    1.00f to Color.White.copy(alpha = 0f),
                    center = highlightCenter,
                    radius = radius * HIGHLIGHT_CORE_RADIUS_FRACTION,
                )
            }

        onDrawBehind {
            drawCircle(bodyBrush, radius = radius, center = center)
            if (convexBrush != null) {
                drawCircle(convexBrush, radius = radius, center = center)
            }
            // Both highlight layers share one transform: the fit puts them on the same ellipse, and
            // stretching y is what makes that ellipse upright rather than round.
            if (glowBrush != null && coreBrush != null) {
                withTransform({ scale(scaleX = 1f, scaleY = HIGHLIGHT_ECCENTRICITY, pivot = highlightCenter) }) {
                    drawCircle(glowBrush, radius = radius * HIGHLIGHT_GLOW_RADIUS_FRACTION, center = highlightCenter)
                    drawCircle(coreBrush, radius = radius * HIGHLIGHT_CORE_RADIUS_FRACTION, center = highlightCenter)
                }
            }
            drawCircle(style.rimColor, radius = radius - rimWidthPx / 2f, center = center, style = Stroke(rimWidthPx))
        }
    }
