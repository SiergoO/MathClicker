package com.sdamashchuk.mathbubbles.feature.menu

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object MenuLogoMotion {
    const val BREATH_PERIOD_MS = 4_000L
    const val BOB_PERIOD_MS = 6_000L
    const val BREATH_PEAK_SCALE = 1.03f
    const val BOB_AMPLITUDE_DP = 4f

    private const val HALF = 0.5f
    private const val FULL_TURN = 2f * PI.toFloat()

    fun breathScale(timeMs: Long): Float {
        val phase = (timeMs % BREATH_PERIOD_MS).toFloat() / BREATH_PERIOD_MS
        return 1f + (BREATH_PEAK_SCALE - 1f) * HALF * (1f - cos(FULL_TURN * phase))
    }

    fun bobFraction(timeMs: Long): Float {
        val phase = (timeMs % BOB_PERIOD_MS).toFloat() / BOB_PERIOD_MS
        return sin(FULL_TURN * phase)
    }
}
