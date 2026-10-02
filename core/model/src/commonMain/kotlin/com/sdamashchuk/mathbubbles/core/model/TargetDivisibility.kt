package com.sdamashchuk.mathbubbles.core.model

/**
 * Whether [sign]/[digit] would divide [target] outright - division with no remainder only.
 * Subtraction never qualifies, however close it comes to clearing the target: [isReadyFor]
 * covers that case, this one does not.
 */
fun isDivisibleByCurrentAction(
    target: Target,
    sign: OperationSign,
    digit: Int,
): Boolean = sign == OperationSign.DIVISION && digit != 0 && target.value % digit == 0
