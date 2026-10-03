package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sdamashchuk.mathbubbles.core.ui.component.model.BubbleStyle

private const val HIGHLIGHT_CORE_RADIUS_FRACTION = 0.15f
private const val HIGHLIGHT_GLOW_RADIUS_FRACTION = 0.30f

// Upright, not tilted. The reference art's blob leans; on a falling object that lean reads as the
// bubble being askew, so the owner picked the straight ellipse from the design system instead.
private const val HIGHLIGHT_ECCENTRICITY = 1.2f

private const val CONVEX_LIGHT_ALPHA = 0.16f
private const val CONVEX_SHADE_ALPHA = 0.22f

fun Modifier.bubbleSurface(
    style: BubbleStyle,
    highlightOffsetProvider: (() -> Offset)? = null,
) = drawWithCache {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
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
    val highlightCenter =
        Offset(
            center.x + (highlight?.offsetXFraction ?: 0f) * radius,
            center.y + (highlight?.offsetYFraction ?: 0f) * radius,
        )
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
        if (highlight != null && glowBrush != null && coreBrush != null) {
            val dynamicOffset = highlightOffsetProvider?.invoke() ?: Offset.Zero
            // Both highlight layers share one transform: the fit puts them on the same ellipse, and
            // stretching y is what makes that ellipse upright rather than round.
            withTransform({
                translate(dynamicOffset.x * radius, dynamicOffset.y * radius)
                rotate(highlight.rotationDegrees, pivot = highlightCenter)
                scale(scaleX = 1f, scaleY = HIGHLIGHT_ECCENTRICITY, pivot = highlightCenter)
            }) {
                drawCircle(glowBrush, radius = radius * HIGHLIGHT_GLOW_RADIUS_FRACTION, center = highlightCenter)
                drawCircle(coreBrush, radius = radius * HIGHLIGHT_CORE_RADIUS_FRACTION, center = highlightCenter)
            }
        }
        drawCircle(style.rimColor, radius = radius - rimWidthPx / 2f, center = center, style = Stroke(rimWidthPx))
    }
}
