package com.sdamashchuk.mathbubbles.core.game.model

/**
 * The board facts [com.sdamashchuk.mathbubbles.core.game.BoosterDropRule] weighs a drop against;
 * [icePickArmed] and [shieldActive] have no source yet - effects land in a later task - so callers
 * pass `false` until then.
 */
data class BoosterDropContext(
    val anyVisibleBeyondTelegraph: Boolean,
    val noVisibleBubble: Boolean,
    val oneLifeLeft: Boolean,
    val icePickArmed: Boolean,
    val shieldActive: Boolean,
)
