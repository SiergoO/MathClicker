package com.sdamashchuk.mathbubbles.core.game.model

import com.sdamashchuk.mathbubbles.core.model.Booster

/**
 * [booster] is null on a draw that stayed an operation; [counter] is always the value
 * [com.sdamashchuk.mathbubbles.core.game.Game]'s own drop counter should hold next, win or not.
 */
data class BoosterDropResult(
    val booster: Booster?,
    val counter: Int,
)
