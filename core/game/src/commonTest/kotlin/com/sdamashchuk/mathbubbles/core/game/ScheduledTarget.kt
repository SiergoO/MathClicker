package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.model.Target

// MC-72 turned Target's own delay/lifetime state into two absolute moments on the clock, which
// pre-MC-72 tests expressed directly (fallenMs, appearanceDelayMs, lifetimeMs, isVisible). This
// keeps those same fixtures readable across the whole module: referenceGameTimeMs is "now" for the
// purposes of fallenMs/appearanceDelayMs (0 unless the field under test starts elsewhere), and
// isVisible is gone entirely - every fixture already kept exactly one of fallenMs/appearanceDelayMs
// non-zero, which is already what determines visibility once translated (appearsAtMs <= "now" or not).
internal fun scheduledTarget(
    id: Int,
    relatedFieldId: Int = 1,
    columnId: Int = 0,
    value: Int,
    fallenMs: Long = 0,
    appearanceDelayMs: Long = 0,
    lifetimeMs: Long = 100_000,
    referenceGameTimeMs: Long = 0,
    isProfitable: Boolean = true,
    isActive: Boolean = true,
): Target {
    val appearsAtMs = referenceGameTimeMs + appearanceDelayMs - fallenMs
    return Target(
        id = id,
        relatedFieldId = relatedFieldId,
        columnId = columnId,
        value = value,
        appearsAtMs = appearsAtMs,
        finishesAtMs = appearsAtMs + lifetimeMs,
        isProfitable = isProfitable,
        isActive = isActive,
    )
}
