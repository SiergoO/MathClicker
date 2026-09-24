package com.sdamashchuk.matharcade.core.model

// The final 15% of a fall, where the telegraph in TargetButton switches on - the same 0.85
// threshold against position that MC-54 asked for, kept as a constant rather than a literal on the
// property below so the two can't drift apart.
private const val BREAKOUT_TELEGRAPH_THRESHOLD = 0.85f

data class Target(
    val id: Int,
    val relatedFieldId: Int,
    val columnId: Int,
    val value: Int,
    val fallenMs: Int,
    val appearanceDelayMs: Int,
    val lifetimeMs: Int,
    val isProfitable: Boolean = true,
    val isVisible: Boolean = false,
    val isActive: Boolean = true,
) {
    // Computed rather than stored, so it stays out of equals/copy. The lifetimeMs <= 0 guard
    // closes a NaN path a corrupt restored row (lifetimeMs 0) would otherwise open.
    val position: Float
        get() = if (lifetimeMs <= 0) 0f else (fallenMs.toFloat() / lifetimeMs).coerceIn(0f, 1f)

    // Reuses position's own lifetimeMs <= 0 guard rather than repeating it: a corrupt zero/negative
    // lifetime already floors position at 0f, which is below the threshold, so no target in that
    // state is ever telegraphed.
    val isTelegraphingBreakout: Boolean
        get() = position >= BREAKOUT_TELEGRAPH_THRESHOLD
}
