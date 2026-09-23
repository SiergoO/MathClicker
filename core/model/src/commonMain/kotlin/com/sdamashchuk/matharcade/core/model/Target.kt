package com.sdamashchuk.matharcade.core.model

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
}
