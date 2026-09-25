package com.sdamashchuk.matharcade.core.game

// The freeze/slow axis effects from MC-71's design doc: a coefficient on tick()'s own step, 0 for a
// full freeze, a fraction to slow. Folded in before the MAX_TICK_MS clamp, not after - a scale
// above 1.0 (a future speed-up) must never let a step through larger than an unscaled tick already
// could, or the clamp guarding a backgrounded app against teleporting targets stops being a
// ceiling. Applying the clamp last also means clockScale's default of 1.0 reproduces tick()'s
// pre-MC-74 arithmetic exactly: (elapsedMs * 1.0).toInt() is elapsedMs itself for every value this
// engine ever ticks with.
internal fun scaleTickStep(
    elapsedMs: Int,
    clockScale: Double,
    maxTickMs: Int,
): Int = (elapsedMs * clockScale).toInt().coerceIn(0, maxTickMs)
