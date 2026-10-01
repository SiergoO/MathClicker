package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import kotlin.test.Test
import kotlin.test.assertEquals

// Field.currentAction/nextAction have no test source set of their own (:core:model has none - see
// FieldMapperTest's own appliedMultiplier test for the same arrangement).
class FieldActionTest {
    @Test
    fun `currentAction and nextAction read the operation pair when no booster is drawn`() {
        val field =
            Field(
                currentOperationSign = OperationSign.DIVISION,
                currentOperationDigit = 4,
                nextOperationSign = OperationSign.SUBTRACTION,
                nextOperationDigit = 7,
            )

        assertEquals(FieldAction.Operation(OperationSign.DIVISION, 4), field.currentAction)
        assertEquals(FieldAction.Operation(OperationSign.SUBTRACTION, 7), field.nextAction)
    }

    @Test
    fun `currentAction and nextAction read the booster over the stale operation pair once one is drawn`() {
        val field =
            Field(
                currentOperationSign = OperationSign.DIVISION,
                currentOperationDigit = 4,
                currentBooster = Booster.FREEZE,
                nextOperationSign = OperationSign.SUBTRACTION,
                nextOperationDigit = 7,
                nextBooster = Booster.SHIELD,
            )

        assertEquals(FieldAction.BoosterAction(Booster.FREEZE), field.currentAction)
        assertEquals(FieldAction.BoosterAction(Booster.SHIELD), field.nextAction)
    }

    @Test
    fun `a fresh field carries no boosters and an empty stash`() {
        val field = Field()

        assertEquals(FieldAction.Operation(OperationSign.DIVISION, 0), field.currentAction)
        assertEquals(emptyList(), field.boosterStash)
    }
}
