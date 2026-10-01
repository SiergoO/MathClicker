package com.sdamashchuk.mathbubbles.core.game.model

import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target

/**
 * One indivisible snapshot of [com.sdamashchuk.mathbubbles.core.game.Game]'s state - every mutator
 * publishes exactly one of these under its lock, so a reader can never observe a field and target
 * list that were never true together.
 */
data class GameState(
    val field: Field,
    val targets: List<Target>,
    val effects: ActiveEffects = ActiveEffects(),
)
