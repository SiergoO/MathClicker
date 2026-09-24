package com.sdamashchuk.matharcade.core.game.model

import com.sdamashchuk.matharcade.core.model.Target

/**
 * The result of one fire-button press against the current target list: [targets] with each
 * affected member updated, the raw (unmultiplied) [totalScore] it earned, and whether [failed] -
 * any affected target came out unprofitable, which is what resets the combo streak.
 */
data class PressOutcome(
    val targets: List<Target>,
    val totalScore: Int,
    val failed: Boolean,
)
