package com.sdamashchuk.mathbubbles.core.game.model

import com.sdamashchuk.mathbubbles.core.model.Target

/**
 * One press against the whole board: [scored] and [failedCount] count the targets it resolved and
 * the ones it left unprofitable, and [totalScore] is raw - the combo multiplier is applied by the
 * caller.
 */
data class PressOutcome(
    val targets: List<Target>,
    val totalScore: Int,
    val scored: Int,
    val failedCount: Int,
) {
    val failed: Boolean get() = failedCount > 0
}
