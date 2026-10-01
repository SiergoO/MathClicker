package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.model.isReadyFor

/**
 * The single flag TargetButton renders - [isReadyFor]'s rule gated by profitability (grey always
 * wins: a target that failed an operation never looks ready, whatever its number says) and by
 * [hintsEnabled], the policy seam a future difficulty/mod supplies. TargetButton never sees why
 * the result is false.
 */
internal fun shouldShowReadinessHint(
    target: Target,
    sign: OperationSign,
    digit: Int,
    hintsEnabled: Boolean,
): Boolean = hintsEnabled && target.isProfitable && isReadyFor(target, sign, digit)
