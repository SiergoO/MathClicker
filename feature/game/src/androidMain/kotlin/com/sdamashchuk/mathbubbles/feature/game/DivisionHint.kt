package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.model.isDivisibleByCurrentAction

/**
 * The single flag TargetButton shakes on - [isDivisibleByCurrentAction]'s rule gated by
 * profitability (grey always wins: a target that failed an operation never hints, whatever its
 * number says) and by [hintsEnabled], the policy seam a future difficulty/mod supplies.
 * TargetButton never sees why the result is false.
 */
internal fun shouldShowDivisionHint(
    target: Target,
    sign: OperationSign,
    digit: Int,
    hintsEnabled: Boolean,
): Boolean = hintsEnabled && target.isProfitable && isDivisibleByCurrentAction(target, sign, digit)
