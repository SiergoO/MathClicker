package com.sdamashchuk.mathbubbles.feature.game

// The board sinks as the player climbs. Every water stop is multiplied by this factor, so
// the whole column darkens together and the shaft of light keeps its shape.
private const val DEPTH_DARKENING_PER_LEVEL = 0.018f

// Not cosmetic. Below roughly this point an idle bubble - whose rim is only #6B8695 - stops
// separating from the water at all, and the board becomes unreadable rather than atmospheric.
private const val DEEPEST_FACTOR = 0.45f

/**
 * Brightness multiplier for the water at a given level: 1 at level 1, falling to a hard floor.
 */
internal fun waterDepthFactor(level: Int): Float = maxOf(DEEPEST_FACTOR, 1f - (level - 1) * DEPTH_DARKENING_PER_LEVEL)
