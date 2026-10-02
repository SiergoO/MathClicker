package com.sdamashchuk.mathbubbles.feature.game

internal object DisappearFade {
    const val DURATION_MS = 200
    private const val SCALE_END = 0.85f

    fun alpha(progress: Float): Float = 1f - progress

    fun scale(progress: Float): Float = 1f - (1f - SCALE_END) * progress
}
