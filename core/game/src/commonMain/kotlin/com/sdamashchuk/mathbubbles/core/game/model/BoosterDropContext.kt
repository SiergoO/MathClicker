package com.sdamashchuk.mathbubbles.core.game.model

/**
 * The board facts [com.sdamashchuk.mathbubbles.core.game.BoosterDropRule] weighs a drop against.
 */
data class BoosterDropContext(
    val anyVisibleBeyondTelegraph: Boolean,
    val noVisibleBubble: Boolean,
    val oneLifeLeft: Boolean,
    val icePickArmed: Boolean,
    val shieldActive: Boolean,
)
