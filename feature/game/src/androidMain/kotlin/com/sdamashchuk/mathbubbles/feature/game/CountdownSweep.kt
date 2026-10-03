package com.sdamashchuk.mathbubbles.feature.game

private const val FULL_CIRCLE_DEGREES = 360f

internal fun countdownSweepDegrees(fraction: Float): Float = FULL_CIRCLE_DEGREES * fraction.coerceIn(0f, 1f)
