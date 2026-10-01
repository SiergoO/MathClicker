package com.sdamashchuk.mathbubbles.core.game.model

import com.sdamashchuk.mathbubbles.core.model.Booster

/**
 * The one timed slot Freeze or Rewind share; applying either while one is active replaces it
 * outright, which is what gives same-kind restart and cross-kind cancellation for free.
 */
internal data class TimedBoosterEffect(
    val booster: Booster,
    val remainingRealMs: Int,
)
