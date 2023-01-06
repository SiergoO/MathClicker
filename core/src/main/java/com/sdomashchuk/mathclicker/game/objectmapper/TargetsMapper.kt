package com.sdomashchuk.mathclicker.game.objectmapper

import com.sdomashchuk.mathclicker.model.OperationSign
import com.sdomashchuk.mathclicker.model.Target

internal fun List<Target>.changeVisibility(id: Int, isVisible: Boolean): List<Target> = this.map {
    if (id == it.id) {
        it.copy(
            isVisible = isVisible
        )
    } else it
}

internal fun List<Target>.changeActiveness(id: Int, isActive: Boolean): List<Target> = this.map {
    if (id == it.id) {
        it.copy(
            isActive = isActive
        )
    } else it
}

internal fun List<Target>.decrementValue(id: Int, decrement: Int): List<Target> = this.map {
    if (id == it.id) {
        val targetValue = it.value - decrement
        it.copy(
            value = targetValue
        )
    } else it
}

internal fun List<Target>.ensureVisible(): List<Target> = this.map {
    it.copy(
        isVisible = it.value > 0 && it.isVisible
    )
}

internal fun List<Target>.ensureVisible(id: Int): List<Target> = this.map {
    if (id == it.id) {
        it.copy(
            isVisible = it.value > 0 && it.isVisible
        )
    } else it
}

internal fun List<Target>.ensureAlive(): List<Target> = this.map {
    it.copy(
        isActive = it.value > 0 && it.isActive
    )
}

internal fun List<Target>.ensureAlive(id: Int): List<Target> = this.map {
    if (id == it.id) {
        it.copy(
            isActive = it.value > 0 && it.isActive
        )
    } else it
}

internal fun List<Target>.updateTargetPositioning(id: Int, position: Int, gameColumnHeightPx: Int): List<Target> =
    this.map {
        if (id == it.id) {
            val progressInPercents = 1f - position.toFloat() / gameColumnHeightPx.toFloat()
            val updatedLifetimeMs = (it.lifetimeMs * progressInPercents).toInt()
            val updatedAppearance = if (position > 0) 0 else (it.appearanceDelayMs * progressInPercents).toInt()
            it.copy(
                position = position,
                lifetimeMs = updatedLifetimeMs,
                appearanceDelayMs = updatedAppearance
            )
        } else it
    }

internal fun List<Target>.shortenAppearanceDelay(): List<Target> {
    val nonVisibleAliveTargets =
        this.filter { it.isActive && !it.isVisible }.sortedBy { it.appearanceDelayMs }
    return if (nonVisibleAliveTargets.isNotEmpty()) {
        val closestTargetsToReveal = nonVisibleAliveTargets.take((1..4).random())
        this.map { target ->
            if (closestTargetsToReveal.contains(target)) {
                target.copy(appearanceDelayMs = 0)
            } else if (nonVisibleAliveTargets.contains(target)) {
                target.copy(appearanceDelayMs = target.appearanceDelayMs - closestTargetsToReveal.last().appearanceDelayMs)
            } else {
                target
            }
        }
    } else this
}

internal fun List<Target>.performOperation(
    currentOperationSign: OperationSign,
    currentOperationDigit: Int
): Pair<List<Target>, Int> {
    var multiplier = 0
    var totalScore = 0
    val updatedList = this.map { target ->
        if (target.isActive && target.isVisible) {
            var isProfitable = target.isProfitable
            val nextValue = run {
                if (currentOperationSign == OperationSign.DIVISION) {
                    val remainder = target.value % currentOperationDigit
                    if (remainder == 0) {
                        val result = target.value / currentOperationDigit
                        totalScore += if (target.isProfitable) target.value - result else 0
                        multiplier++
                        result
                    } else {
                        isProfitable = false
                        multiplier--
                        target.value * currentOperationDigit
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
                isProfitable = isProfitable
            )
        } else target
    }
    val finalScore = totalScore * multiplier
    return Pair(updatedList, finalScore)
}