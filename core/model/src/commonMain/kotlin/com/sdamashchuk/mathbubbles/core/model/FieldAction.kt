package com.sdamashchuk.mathbubbles.core.model

/**
 * What the action stream drew into a slot on [Field] - a math operation or a booster. Replaces a
 * bare sign/digit pair for anything that reads [Field.currentAction] or [Field.nextAction].
 */
sealed interface FieldAction {
    data class Operation(
        val sign: OperationSign,
        val digit: Int,
    ) : FieldAction

    data class BoosterAction(
        val booster: Booster,
    ) : FieldAction
}
