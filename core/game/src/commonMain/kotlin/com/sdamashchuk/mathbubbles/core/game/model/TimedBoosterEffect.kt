package com.sdamashchuk.mathbubbles.core.game.model

import com.sdamashchuk.mathbubbles.core.model.Booster

/**
 * The one timed slot Freeze or Rewind share, replaced outright by applying either while one is
 * active; [rate] is the eased clock multiplier (1 = untouched, Freeze's floor 0, Rewind's -1).
 */
internal data class TimedBoosterEffect(
    val booster: Booster,
    val remainingRealMs: Int,
    val rate: Double = 1.0,
)
