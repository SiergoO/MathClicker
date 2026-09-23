package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Target
import kotlin.random.Random

internal fun List<Target>.changeVisibility(
    id: Int,
    isVisible: Boolean,
): List<Target> =
    this.map {
        if (id == it.id) {
            it.copy(
                isVisible = isVisible,
            )
        } else {
            it
        }
    }

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

internal fun List<Target>.ensureVisible(): List<Target> =
    this.map {
        it.copy(
            isVisible = it.value > 0 && it.isVisible,
        )
    }

internal fun List<Target>.ensureVisible(id: Int): List<Target> =
    this.map {
        if (id == it.id) {
            it.copy(
                isVisible = it.value > 0 && it.isVisible,
            )
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

internal fun List<Target>.updateTargetPositioning(
    id: Int,
    position: Int,
    gameColumnHeightPx: Int,
): List<Target> =
    this.map {
        if (id == it.id) {
            // lifetimeMs must stay the fall's original total duration: it is the divisor the UI
            // uses to derive the remaining animation time from the saved position on every resume.
            // Shrinking it here used to compound across repeated pause/resume cycles until the
            // remaining duration collapsed to ~0 while the target was nowhere near the bottom,
            // causing a spurious breakout (MC-27).
            val progressInPercents = 1f - position.toFloat() / gameColumnHeightPx.toFloat()
            val updatedAppearance = if (position > 0) 0 else (it.appearanceDelayMs * progressInPercents).toInt()
            it.copy(
                fallenMs = (fallenFraction(position, gameColumnHeightPx) * it.lifetimeMs).toInt(),
                appearanceDelayMs = updatedAppearance,
            )
        } else {
            it
        }
    }

// Clamped so a corrupt or overshooting saved position can't push fallenMs outside 0..lifetimeMs.
private fun fallenFraction(
    position: Int,
    gameColumnHeightPx: Int,
): Float =
    if (gameColumnHeightPx > 0) {
        (position.toFloat() / gameColumnHeightPx).coerceIn(0f, 1f)
    } else {
        0f
    }

internal fun List<Target>.shortenAppearanceDelay(random: Random = Random.Default): List<Target> {
    val nonVisibleAliveTargets =
        this.filter { it.isActive && !it.isVisible }.sortedBy { it.appearanceDelayMs }
    return if (nonVisibleAliveTargets.isNotEmpty()) {
        val closestTargetsToReveal = nonVisibleAliveTargets.take((1..4).random(random))
        this.map { target ->
            if (closestTargetsToReveal.contains(target)) {
                target.copy(appearanceDelayMs = 0)
            } else if (nonVisibleAliveTargets.contains(target)) {
                target.copy(
                    appearanceDelayMs =
                        target.appearanceDelayMs - closestTargetsToReveal.last().appearanceDelayMs,
                )
            } else {
                target
            }
        }
    } else {
        this
    }
}
