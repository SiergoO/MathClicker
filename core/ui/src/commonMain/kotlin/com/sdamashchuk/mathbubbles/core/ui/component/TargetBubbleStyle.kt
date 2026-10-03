package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import com.sdamashchuk.mathbubbles.core.ui.component.model.BubbleHighlight
import com.sdamashchuk.mathbubbles.core.ui.component.model.BubbleStyle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillReady
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady

// Fitted numerically against the reference art, not eyeballed - mean absolute channel error 3.4%.
// The highlight is two layers, not one: with a single blob the fit will not go below 10.7/255.
private const val FILL_ALPHA_IDLE = 0.28f
private const val FILL_ALPHA_READY = 0.62f
private const val RIM_WIDTH_FRACTION_IDLE = 0.031f
private const val RIM_WIDTH_FRACTION_READY = 0.027f
private const val HIGHLIGHT_CORE_ALPHA_IDLE = 0.30f
private const val HIGHLIGHT_CORE_ALPHA_READY = 0.90f
private const val HIGHLIGHT_GLOW_ALPHA_IDLE = 0.14f
private const val HIGHLIGHT_GLOW_ALPHA_READY = 0.36f

// The standard look every profitable bubble now holds steady at: the old readiness blend's
// not-yet-ready floor, baked in rather than animated toward the READY constants above.
private const val STANDARD_LIVELINESS = 0.62f
private val STANDARD_FILL_COLOR = lerp(BubbleFillIdle, BubbleFillReady, STANDARD_LIVELINESS)
private val STANDARD_RIM_COLOR = lerp(BubbleRimIdle, BubbleRimReady, STANDARD_LIVELINESS)
private const val STANDARD_FILL_ALPHA = FILL_ALPHA_IDLE + (FILL_ALPHA_READY - FILL_ALPHA_IDLE) * STANDARD_LIVELINESS
private const val STANDARD_RIM_WIDTH_FRACTION =
    RIM_WIDTH_FRACTION_IDLE + (RIM_WIDTH_FRACTION_READY - RIM_WIDTH_FRACTION_IDLE) * STANDARD_LIVELINESS
private const val STANDARD_CORE_ALPHA =
    HIGHLIGHT_CORE_ALPHA_IDLE + (HIGHLIGHT_CORE_ALPHA_READY - HIGHLIGHT_CORE_ALPHA_IDLE) * STANDARD_LIVELINESS
private const val STANDARD_GLOW_ALPHA =
    HIGHLIGHT_GLOW_ALPHA_IDLE + (HIGHLIGHT_GLOW_ALPHA_READY - HIGHLIGHT_GLOW_ALPHA_IDLE) * STANDARD_LIVELINESS

private const val UNREACHABLE_CORE_ALPHA = 0.06f
private const val UNREACHABLE_FILL_ALPHA = 0.16f

// Every target shares one standard look; only a target that would bring no points
// (isProfitable false) drops to the dimmer, coreless unreachable look.
fun targetBubbleStyle(
    diameter: Dp,
    isProfitable: Boolean,
    horizontalFraction: Float,
): BubbleStyle {
    val baseOffset = TargetHighlightTilt.baseOffsetFraction(horizontalFraction)
    val highlight =
        BubbleHighlight(
            coreAlpha = if (isProfitable) STANDARD_CORE_ALPHA else UNREACHABLE_CORE_ALPHA,
            glowAlpha = if (isProfitable) STANDARD_GLOW_ALPHA else HIGHLIGHT_GLOW_ALPHA_IDLE,
            offsetXFraction = baseOffset.x,
            offsetYFraction = baseOffset.y,
            rotationDegrees = TargetHighlightTilt.angleDegrees(horizontalFraction),
        )
    return if (isProfitable) {
        BubbleStyle(
            fillColor = STANDARD_FILL_COLOR,
            fillAlpha = STANDARD_FILL_ALPHA,
            rimColor = STANDARD_RIM_COLOR,
            rimWidth = diameter * STANDARD_RIM_WIDTH_FRACTION,
            highlight = highlight,
        )
    } else {
        BubbleStyle(
            fillColor = BubbleFillIdle,
            fillAlpha = UNREACHABLE_FILL_ALPHA,
            rimColor = BubbleRimIdle,
            rimWidth = diameter * RIM_WIDTH_FRACTION_IDLE,
            highlight = highlight,
        )
    }
}
