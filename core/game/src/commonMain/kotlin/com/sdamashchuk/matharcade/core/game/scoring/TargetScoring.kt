package com.sdamashchuk.matharcade.core.game.scoring

import com.sdamashchuk.matharcade.core.game.model.PressOutcome
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target

// failedGrowthCap is level-scaled (see SessionHelperImpl, next to getTargetValueByLevel, for the
// simulation that verifies it) rather than a flat constant here, so the caller passes in the
// ceiling for the target's current level on every press.
internal fun List<Target>.performOperation(
    currentOperationSign: OperationSign,
    currentOperationDigit: Int,
    failedGrowthCap: Int,
    gameTimeMs: Long,
): PressOutcome {
    var totalScore = 0
    var scored = 0
    var failedCount = 0
    val updatedList =
        this.map { target ->
            if (target.isActive && target.isVisible(gameTimeMs)) {
                var isProfitable = target.isProfitable
                val nextValue =
                    run {
                        if (currentOperationSign == OperationSign.DIVISION) {
                            if (currentOperationDigit == 0) {
                                // Field()'s default digit before a real one is assigned. Treat it as a
                                // failed split rather than dividing by zero: no crash, the target
                                // simply survives unchanged.
                                isProfitable = false
                                failedCount++
                                target.value
                            } else {
                                val remainder = target.value % currentOperationDigit
                                if (remainder == 0) {
                                    val result = target.value / currentOperationDigit
                                    if (target.isProfitable) {
                                        totalScore += target.value - result
                                        scored++
                                    }
                                    result
                                } else {
                                    isProfitable = false
                                    failedCount++
                                    (target.value.toLong() * currentOperationDigit)
                                        .coerceAtMost(failedGrowthCap.toLong())
                                        .toInt()
                                }
                            }
                        } else {
                            val result = target.value - currentOperationDigit
                            return@run if (result >= 0) {
                                if (target.isProfitable) {
                                    totalScore += currentOperationDigit
                                    scored++
                                }
                                result
                            } else {
                                isProfitable = false
                                failedCount++
                                target.value + currentOperationDigit
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
    return PressOutcome(updatedList, totalScore, scored, failedCount)
}
