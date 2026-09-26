package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Target

internal fun List<Target>.changeActiveness(
    id: Int,
    isActive: Boolean,
): List<Target> =
    this.map {
        if (id == it.id) {
            it.copy(
                isActive = isActive,
            )
        } else {
            it
        }
    }

internal fun List<Target>.decrementValue(
    id: Int,
    decrement: Int,
): List<Target> =
    this.map {
        if (id == it.id) {
            val targetValue = it.value - decrement
            it.copy(
                value = targetValue,
            )
        } else {
            it
        }
    }

// Strictly after appearsAtMs, never equal, so the result cannot reopen Target.position's zero-span guard.
private const val MIN_TARGET_SPAN_MS = 1L

// Positive byMs holds the target, negative speeds it up; appearsAtMs never moves, or the whole flight
// would slide instead. coerceAtLeast stops a large speed-up from inverting the span.
internal fun List<Target>.shiftFinish(
    id: Int,
    byMs: Long,
    gameTimeMs: Long,
): List<Target> =
    this.map {
        if (id == it.id && it.isActive && !it.hasBrokenOut(gameTimeMs)) {
            it.copy(finishesAtMs = (it.finishesAtMs + byMs).coerceAtLeast(it.appearsAtMs + MIN_TARGET_SPAN_MS))
        } else {
            it
        }
    }

internal fun List<Target>.ensureAlive(): List<Target> =
    this.map {
        it.copy(
            isActive = it.value > 0 && it.isActive,
        )
    }

internal fun List<Target>.ensureAlive(id: Int): List<Target> =
    this.map {
        if (id == it.id) {
            it.copy(
                isActive = it.value > 0 && it.isActive,
            )
        } else {
            it
        }
    }

// One uniform shift of the whole remaining schedule, never a per-target snap to gameTimeMs: subtracting
// one constant from both ends preserves every gap, while snapping collapses the finishes together.
internal fun List<Target>.shortenAppearanceDelay(gameTimeMs: Long): List<Target> {
    val waitingTargets = this.filter { it.isActive && !it.isVisible(gameTimeMs) }
    val closestAppearsAtMs = waitingTargets.minOfOrNull { it.appearsAtMs } ?: return this
    val shiftMs = closestAppearsAtMs - gameTimeMs
    return this.map { target ->
        if (waitingTargets.contains(target)) {
            target.copy(
                appearsAtMs = target.appearsAtMs - shiftMs,
                finishesAtMs = target.finishesAtMs - shiftMs,
            )
        } else {
            target
        }
    }
}
