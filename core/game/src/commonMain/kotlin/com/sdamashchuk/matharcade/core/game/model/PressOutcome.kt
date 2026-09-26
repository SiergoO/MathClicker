package com.sdamashchuk.matharcade.core.game.model

import com.sdamashchuk.matharcade.core.model.Target

/**
 * The result of one fire-button press against the current target list: [targets] with each
 * affected member updated, the raw (unmultiplied) [totalScore] it earned, and how many affected
 * targets the operation [scored] on versus [failedCount] it came out unprofitable against.
 *
 * [scored] is the combo (MC-95): one press is broadcast to every visible target, so the interesting
 * number is how many of them a single operation closed, not whether the press was flawless.
 */
data class PressOutcome(
    val targets: List<Target>,
    val totalScore: Int,
    val scored: Int,
    val failedCount: Int,
) {
    val failed: Boolean get() = failedCount > 0
}
