package com.sdamashchuk.matharcade.core.model

/**
 * Whether [sign]/[digit] would clear [target] outright - division with no remainder, or
 * subtraction that does not go negative. A zero [digit] (Field()'s default before a real one is
 * assigned) is never ready rather than dividing by zero, the same treatment performOperation
 * already gives a failed split.
 */
fun isReadyFor(
    target: Target,
    sign: OperationSign,
    digit: Int,
): Boolean =
    when (sign) {
        OperationSign.DIVISION -> digit != 0 && target.value % digit == 0
        OperationSign.SUBTRACTION -> target.value - digit >= 0
    }
