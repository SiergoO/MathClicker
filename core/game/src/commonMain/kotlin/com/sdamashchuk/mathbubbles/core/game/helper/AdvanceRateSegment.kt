package com.sdamashchuk.mathbubbles.core.game.helper

import kotlin.math.abs

// Split at the moment the rate reaches [target] mid-segment, or a short segment inside a long
// tick either over- or undershoots the average.
internal fun advanceRateSegment(
    rate: Double,
    target: Double,
    speedPerMs: Double,
    durationMs: Int,
): Pair<Double, Double> {
    val delta = target - rate
    return if (durationMs <= 0 || speedPerMs <= 0.0 || delta == 0.0) {
        rate to rate * durationMs.coerceAtLeast(0)
    } else {
        val msToTarget = (abs(delta) / speedPerMs).coerceAtMost(durationMs.toDouble())
        val rateAtTarget = rate + (if (delta > 0) speedPerMs else -speedPerMs) * msToTarget
        val rampStepMs = (rate + rateAtTarget) / 2.0 * msToTarget
        val holdMs = durationMs - msToTarget
        rateAtTarget to rampStepMs + rateAtTarget * holdMs
    }
}
