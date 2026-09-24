package com.sdamashchuk.matharcade.core.game.scoring

import com.sdamashchuk.matharcade.core.game.model.PressOutcome
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target

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
): PressOutcome {
    var totalScore = 0
    var pressFailed = false
    val updatedList =
        this.map { target ->
            if (target.isActive && target.isVisible) {
                var isProfitable = target.isProfitable
                val nextValue =
                    run {
                        if (currentOperationSign == OperationSign.DIVISION) {
                            if (currentOperationDigit == 0) {
                                // Field()'s default digit before a real one is assigned. Treat it as a
                                // failed split rather than dividing by zero: no crash, the target
                                // simply survives unchanged.
                                isProfitable = false
                                pressFailed = true
                                target.value
                            } else {
                                val remainder = target.value % currentOperationDigit
                                if (remainder == 0) {
                                    val result = target.value / currentOperationDigit
                                    totalScore += if (target.isProfitable) target.value - result else 0
                                    result
                                } else {
                                    isProfitable = false
                                    pressFailed = true
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
                                    result
                                }

                                result == 0 -> {
                                    totalScore += if (target.isProfitable) currentOperationDigit else 0
                                    0
                                }

                                else -> {
                                    isProfitable = false
                                    pressFailed = true
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
    return PressOutcome(updatedList, totalScore, pressFailed)
}
