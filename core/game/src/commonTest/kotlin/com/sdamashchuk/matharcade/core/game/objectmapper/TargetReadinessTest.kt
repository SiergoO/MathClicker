package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.game.scheduledTarget
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.isReadyFor
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun target(value: Int) = scheduledTarget(id = 1, relatedFieldId = 0, value = value)

// isReadyFor lives on :core:model - covered from here because :core:model has no test source set,
// the same precedent as Target.isTelegraphingBreakout above and Field.appliedMultiplier in
// FieldMapperTest.
class TargetReadinessTest {
    @Test
    fun `division is ready exactly when the remainder is zero`() {
        assertTrue(isReadyFor(target(value = 10), OperationSign.DIVISION, digit = 2))
        assertFalse(isReadyFor(target(value = 7), OperationSign.DIVISION, digit = 2))
    }

    @Test
    fun `division boundary is pinned at value 16 versus value 17 against digit 2`() {
        assertTrue(isReadyFor(target(value = 16), OperationSign.DIVISION, digit = 2))
        assertFalse(isReadyFor(target(value = 17), OperationSign.DIVISION, digit = 2))
    }

    @Test
    fun `division by a zero digit is never ready and never throws`() {
        assertFalse(isReadyFor(target(value = 10), OperationSign.DIVISION, digit = 0))
    }

    @Test
    fun `subtraction is ready when the result lands on zero but not when it goes negative`() {
        assertTrue(isReadyFor(target(value = 5), OperationSign.SUBTRACTION, digit = 5))
        assertFalse(isReadyFor(target(value = 4), OperationSign.SUBTRACTION, digit = 5))
    }

    @Test
    fun `subtraction is ready with room to spare above zero`() {
        assertTrue(isReadyFor(target(value = 10), OperationSign.SUBTRACTION, digit = 5))
    }

    @Test
    fun `a fresh Field's default digit of zero is never ready`() {
        // Field()'s default currentOperationSign is DIVISION and currentOperationDigit is 0 - this
        // is the state a new session's first frame is in before a real digit is assigned.
        assertFalse(isReadyFor(target(value = 10), OperationSign.DIVISION, digit = 0))
    }
}
