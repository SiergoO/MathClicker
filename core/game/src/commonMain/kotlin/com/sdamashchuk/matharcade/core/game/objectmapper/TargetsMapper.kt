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

// Advances every active target by stepMs. A target still waiting out its appearance delay burns
// the step against appearanceDelayMs first; when the delay is consumed partway through the step,
// the leftover carries straight into fallenMs instead of being dropped, so a target loses none of
// its fall at the moment it appears.
internal fun List<Target>.advance(stepMs: Int): List<Target> =
    this.map { target ->
        if (!target.isActive) {
            target
        } else {
            val remainingDelay = target.appearanceDelayMs - stepMs
            if (remainingDelay > 0) {
                target.copy(appearanceDelayMs = remainingDelay)
            } else {
                target.copy(
                    appearanceDelayMs = 0,
                    isVisible = true,
                    fallenMs = target.fallenMs - remainingDelay,
                )
            }
        }
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
