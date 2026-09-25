package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Target
import kotlin.random.Random

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

// MC-72: pulls a target's whole schedule earlier rather than shortening a standalone delay field -
// there is no delay left to shorten, only appearsAtMs/finishesAtMs. Each shifted target keeps its
// own flight time (finishesAtMs - appearsAtMs) exactly, the same guarantee the pre-MC-72 version
// gave by leaving lifetimeMs untouched.
internal fun List<Target>.shortenAppearanceDelay(
    gameTimeMs: Long,
    random: Random = Random.Default,
): List<Target> {
    val nonVisibleAliveTargets =
        this.filter { it.isActive && !it.isVisible(gameTimeMs) }.sortedBy { it.appearsAtMs }
    return if (nonVisibleAliveTargets.isNotEmpty()) {
        val closestTargetsToReveal = nonVisibleAliveTargets.take((1..4).random(random))
        val shiftMs = closestTargetsToReveal.last().appearsAtMs - gameTimeMs
        this.map { target ->
            if (closestTargetsToReveal.contains(target)) {
                target.copy(
                    appearsAtMs = gameTimeMs,
                    finishesAtMs = gameTimeMs + (target.finishesAtMs - target.appearsAtMs),
                )
            } else if (nonVisibleAliveTargets.contains(target)) {
                target.copy(
                    appearsAtMs = target.appearsAtMs - shiftMs,
                    finishesAtMs = target.finishesAtMs - shiftMs,
                )
            } else {
                target
            }
        }
    } else {
        this
    }
}
