package com.sdomashchuk.mathclicker.game.objectmapper

import com.sdomashchuk.mathclicker.model.Field
import com.sdomashchuk.mathclicker.model.OperationSign
import com.sdomashchuk.mathclicker.model.Target

internal fun Field.incrementLifeCount(increment: Int): Field {
    val lifeCount = this.lifeCount + increment
    return this.copy(
        lifeCount = lifeCount
    )
}

internal fun Field.decrementLifeCount(decrement: Int): Field {
    val lifeCount = this.lifeCount - decrement
    return this.copy(
        lifeCount = lifeCount
    )
}

internal fun Field.closeIfNecessary(): Field {
    return this.copy(
        isClosed = lifeCount <= 0
    )
}

internal fun Field.updateGameColumnSize(width: Int, height: Int): Field {
    return this.copy(
        gameColumnWidthPx = width,
        gameColumnHeightPx = height
    )
}

internal fun Field.updateLevel(): Field {
    return this.copy(
        level = level + 1
    )
}

internal fun Field.updateScore(scoreToAdd: Int): Field {
    val finalScore = (this.score + scoreToAdd).let { if (it < 0) 0 else it }
    return this.copy(
        score = finalScore
    )
}

internal fun Field.updateActionButtons(
    nextOperationSign: OperationSign,
    nextOperationDigit: Int
): Field {
    return this.copy(
        currentOperationSign = this.nextOperationSign,
        currentOperationDigit = this.nextOperationDigit,
        nextOperationSign = nextOperationSign,
        nextOperationDigit = nextOperationDigit
    )
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
