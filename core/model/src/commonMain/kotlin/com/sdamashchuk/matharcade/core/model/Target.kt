package com.sdamashchuk.matharcade.core.model

// The final 15% of a fall, where the telegraph in TargetButton switches on - the same 0.85
// threshold against position that MC-54 asked for, kept as a constant rather than a literal on the
// property below so the two can't drift apart.
private const val BREAKOUT_TELEGRAPH_THRESHOLD = 0.85f

// MC-72: a target's schedule is two absolute moments on Field.gameTimeMs, not a delay/lifetime pair
// counted down independently - see the MC-71 design doc. finishesAtMs is assigned directly rather
// than derived as appearsAtMs + a separately-rolled lifetime, which is what closed the bug this
// task exists for: two independent random draws summing to a finish is what let an intra-wave
// stagger be cancelled by a lifetime spread.
data class Target(
    val id: Int,
    val relatedFieldId: Int,
    val columnId: Int,
    val value: Int,
    val appearsAtMs: Long,
    val finishesAtMs: Long,
    val isProfitable: Boolean = true,
    val isActive: Boolean = true,
) {
    // Reads the clock rather than owning one - Target has no way to advance time itself, only
    // Field.gameTimeMs does (see Game.tick). The span <= 0 guard closes the same NaN/divide-by-zero
    // path a corrupt restored row (finishesAtMs <= appearsAtMs) opened before this task, when the
    // guard was against lifetimeMs <= 0 instead.
    fun position(gameTimeMs: Long): Float {
        val span = finishesAtMs - appearsAtMs
        return if (span <= 0) 0f else ((gameTimeMs - appearsAtMs).toFloat() / span).coerceIn(0f, 1f)
    }

    fun isVisible(gameTimeMs: Long): Boolean = gameTimeMs >= appearsAtMs

    fun hasBrokenOut(gameTimeMs: Long): Boolean = gameTimeMs >= finishesAtMs

    // Reuses position's own span <= 0 guard rather than repeating it: a corrupt zero/negative span
    // already floors position at 0f, which is below the threshold, so no target in that state is
    // ever telegraphed.
    fun isTelegraphingBreakout(gameTimeMs: Long): Boolean = position(gameTimeMs) >= BREAKOUT_TELEGRAPH_THRESHOLD
}
