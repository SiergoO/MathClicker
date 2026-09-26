package com.sdamashchuk.matharcade.feature.game.model

/**
 * A target's screen-space center in dp, reported by TargetButton on every active frame so a burst
 * can still find where a target was after it has left the composition.
 */
data class TargetScreenPosition(
    val xDp: Float,
    val yDp: Float,
)
