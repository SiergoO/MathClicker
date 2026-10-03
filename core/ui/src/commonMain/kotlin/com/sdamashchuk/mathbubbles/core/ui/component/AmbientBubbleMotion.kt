package com.sdamashchuk.mathbubbles.core.ui.component

import com.sdamashchuk.mathbubbles.core.ui.component.model.AmbientBubbleStyle

private const val SWAY_FRACTION = 0.035f
private const val SWAY_PERIOD_MS = 3_700f
private const val FULL_TURN_RADIANS = 2f * kotlin.math.PI.toFloat()
private const val EDGE_FADE_STEEPNESS = 4f

// One salt per independent property, so a bubble's speed tells you nothing about its lane.
private const val SALT_RISE = 1
private const val SALT_PHASE = 2
private const val SALT_LANE = 3
private const val SALT_SWAY = 4
private const val SALT_RADIUS = 5
private const val SALT_ALPHA = 6

/**
 * How far up its run a bubble is: 0 just off the bottom, 1 as it leaves the top.
 */
fun ambientBubbleProgress(
    style: AmbientBubbleStyle,
    index: Int,
    timeMs: Long,
): Float {
    val riseMs = style.slowestRiseMs + (style.fastestRiseMs - style.slowestRiseMs) * moteScatter(index, SALT_RISE)
    val phase = moteScatter(index, SALT_PHASE)
    return ((timeMs / riseMs) + phase).mod(1f)
}

fun ambientBubbleCenterXFraction(
    index: Int,
    timeMs: Long,
): Float {
    val lane = moteScatter(index, SALT_LANE)
    val swayPhase = moteScatter(index, SALT_SWAY)
    val sway = kotlin.math.sin(((timeMs / SWAY_PERIOD_MS) + swayPhase) * FULL_TURN_RADIANS)
    return (lane + sway * SWAY_FRACTION).coerceIn(0f, 1f)
}

fun ambientBubbleRadiusFraction(
    style: AmbientBubbleStyle,
    index: Int,
): Float =
    style.smallestRadiusFraction +
        (style.largestRadiusFraction - style.smallestRadiusFraction) * moteScatter(index, SALT_RADIUS)

// Fades in off the bottom and out at the top, so nothing pops into or out of existence at an edge.
fun ambientBubbleAlpha(
    style: AmbientBubbleStyle,
    index: Int,
    progress: Float,
): Float {
    val edgeFade =
        (progress * EDGE_FADE_STEEPNESS).coerceAtMost(1f) *
            ((1f - progress) * EDGE_FADE_STEEPNESS).coerceAtMost(1f)
    return (style.minAlpha + (style.maxAlpha - style.minAlpha) * moteScatter(index, SALT_ALPHA)) * edgeFade
}
