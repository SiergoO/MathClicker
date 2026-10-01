package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.ui.component.moteScatter

// The water is not still. Everything here is a pure function of the engine clock and a
// bubble's index, so the drift needs no animation objects, no remembered state and no
// recomposition - the play area's draw phase reads the clock and asks for a position.
internal const val AMBIENT_BUBBLE_COUNT = 14

private const val SLOWEST_RISE_MS = 14_000f
private const val FASTEST_RISE_MS = 6_000f
private const val SMALLEST_RADIUS_FRACTION = 0.0035f
private const val LARGEST_RADIUS_FRACTION = 0.013f
private const val MIN_ALPHA = 0.05f
private const val MAX_ALPHA = 0.20f

// A drift of at most this fraction of the width, so a bubble wanders as it rises instead of
// travelling a ruler-straight line.
private const val SWAY_FRACTION = 0.035f
private const val SWAY_PERIOD_MS = 3_700f
private const val FULL_TURN_RADIANS = 2f * kotlin.math.PI.toFloat()

// A bubble reaches full opacity a quarter of the way up and starts fading a quarter from the top.
private const val EDGE_FADE_STEEPNESS = 4f

// One salt per independent property, so a bubble's speed tells you nothing about its lane.
private const val SALT_RISE = 1
private const val SALT_PHASE = 2
private const val SALT_LANE = 3
private const val SALT_SWAY = 4
private const val SALT_RADIUS = 5
private const val SALT_ALPHA = 6

/** How far up its run a bubble is: 0 just off the bottom, 1 as it leaves the top. */
internal fun ambientBubbleProgress(
    index: Int,
    gameTimeMs: Long,
): Float {
    val riseMs = SLOWEST_RISE_MS + (FASTEST_RISE_MS - SLOWEST_RISE_MS) * moteScatter(index, SALT_RISE)
    val phase = moteScatter(index, SALT_PHASE)
    return ((gameTimeMs / riseMs) + phase).mod(1f)
}

internal fun ambientBubbleCenterXFraction(
    index: Int,
    gameTimeMs: Long,
): Float {
    val lane = moteScatter(index, SALT_LANE)
    val swayPhase = moteScatter(index, SALT_SWAY)
    val sway = kotlin.math.sin(((gameTimeMs / SWAY_PERIOD_MS) + swayPhase) * FULL_TURN_RADIANS)
    return (lane + sway * SWAY_FRACTION).coerceIn(0f, 1f)
}

internal fun ambientBubbleRadiusFraction(index: Int): Float =
    SMALLEST_RADIUS_FRACTION + (LARGEST_RADIUS_FRACTION - SMALLEST_RADIUS_FRACTION) * moteScatter(index, SALT_RADIUS)

// Fades in off the bottom and out at the top, so nothing ever pops into or out of existence at a
// screen edge.
internal fun ambientBubbleAlpha(
    index: Int,
    progress: Float,
): Float {
    val edgeFade =
        (progress * EDGE_FADE_STEEPNESS).coerceAtMost(1f) *
            ((1f - progress) * EDGE_FADE_STEEPNESS).coerceAtMost(1f)
    return (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * moteScatter(index, SALT_ALPHA)) * edgeFade
}
