package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object ShakeMotion {
    private const val SEED_MULTIPLIER = 2_654_435_761L
    private const val ANGLE_MODULUS = 360L
    private const val DEGREES_TO_RADIANS = (PI / 180.0).toFloat()

    fun angleDegrees(seed: Int): Float = ((seed * SEED_MULTIPLIER) % ANGLE_MODULUS).toFloat()

    fun direction(seed: Int): Offset {
        val radians = angleDegrees(seed) * DEGREES_TO_RADIANS
        return Offset(cos(radians), sin(radians))
    }

    fun offset(
        seed: Int,
        amplitudePx: Float,
    ): Offset = direction(seed) * amplitudePx

    fun keyframes(
        seed: Int,
        amplitudePx: Float,
    ): List<Offset> {
        val peak = offset(seed, amplitudePx)
        return listOf(-peak, peak, Offset.Zero)
    }
}
