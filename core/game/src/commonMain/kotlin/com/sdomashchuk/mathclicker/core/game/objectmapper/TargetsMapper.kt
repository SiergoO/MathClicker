package com.sdomashchuk.mathclicker.core.game.objectmapper

import com.sdomashchuk.mathclicker.core.model.OperationSign
import com.sdomashchuk.mathclicker.core.model.Target
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
                position = position,
                appearanceDelayMs = updatedAppearance,
            )
        } else {
            it
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

// A failed division can raise a target's value, but never past this ceiling. Ordinary play hits it
// often, not rarely: simulating the real SessionHelperImpl ranges with alternating division and
// subtraction, level 1 reaches it in 1549/2000 runs within 100 presses (median 59, earliest 16);
// level 30 reaches it in every run (median 18, earliest 6). That is a gameplay change, not only an
// arithmetic fix: a target that used to wrap negative and vanish for free now survives at
// 1,000,000, needs roughly twenty successful halvings inside a 20-40s fall, and costs a life on
// breakout instead. Value stays far below Int.MAX_VALUE so the pre-clamp multiplication (computed
// in Long) can never wrap.
private const val FAILED_DIVISION_VALUE_CAP = 1_000_000

internal fun List<Target>.performOperation(
    currentOperationSign: OperationSign,
    currentOperationDigit: Int,
): Pair<List<Target>, Int> {
    var multiplier = 0
    var totalScore = 0
    val updatedList =
        this.map { target ->
            if (target.isActive && target.isVisible) {
                var isProfitable = target.isProfitable
                val nextValue =
                    run {
                        if (currentOperationSign == OperationSign.DIVISION) {
                            if (currentOperationDigit == 0) {
                                // Field()'s default digit before a real one is assigned. Treat it as a
                                // failed split rather than dividing by zero: no penalty multiplication,
                                // no crash, the target simply survives unchanged.
                                isProfitable = false
                                multiplier--
                                target.value
                            } else {
                                val remainder = target.value % currentOperationDigit
                                if (remainder == 0) {
                                    val result = target.value / currentOperationDigit
                                    totalScore += if (target.isProfitable) target.value - result else 0
                                    multiplier++
                                    result
                                } else {
                                    isProfitable = false
                                    multiplier--
                                    (target.value.toLong() * currentOperationDigit)
                                        .coerceAtMost(FAILED_DIVISION_VALUE_CAP.toLong())
                                        .toInt()
                                }
                            }
                        } else {
                            val result = target.value - currentOperationDigit
                            return@run when {
                                result > 0 -> {
                                    totalScore += if (target.isProfitable) currentOperationDigit else 0
                                    multiplier = if (multiplier == 0) 1 else multiplier
                                    result
                                }

                                result == 0 -> {
                                    totalScore += if (target.isProfitable) currentOperationDigit else 0
                                    multiplier++
                                    0
                                }

                                else -> {
                                    isProfitable = false
                                    multiplier--
                                    target.value + currentOperationDigit
                                }
                            }
                        }
                    }
                target.copy(
                    value = nextValue,
                    isProfitable = isProfitable,
                )
            } else {
                target
            }
        }
    val finalScore = totalScore * multiplier
    return Pair(updatedList, finalScore)
}
