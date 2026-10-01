package com.sdamashchuk.mathbubbles.core.game.model

import com.sdamashchuk.mathbubbles.core.model.Booster

/**
 * Effects running in [com.sdamashchuk.mathbubbles.core.game.Game]; [remainingFraction] falls from
 * 1 to 0 over the timed one at a constant rate, [intensity] is the current booster's own eased
 * envelope, and [freezeTintIntensity] is Freeze's alone, carried continuously through a crossover
 * so the water tint never pops when Rewind takes over.
 */
data class ActiveEffects(
    val timedBooster: Booster? = null,
    val remainingRealMs: Int = 0,
    val remainingFraction: Float = 0f,
    val intensity: Float = 0f,
    val freezeTintIntensity: Float = 0f,
    val icePickArmedFrom: IcePickSource? = null,
    val shieldActive: Boolean = false,
) {
    val icePickArmed: Boolean get() = icePickArmedFrom != null
}
