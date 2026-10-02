package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.game.scheduledTarget
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.isDivisibleByCurrentAction
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun target(value: Int) = scheduledTarget(id = 1, relatedFieldId = 0, value = value)

// isDivisibleByCurrentAction lives on :core:model - covered from here because :core:model has no
// test source set, the same precedent as TargetReadinessTest above it.
class TargetDivisibilityTest {
    @Test
    fun `division with a zero remainder is divisible`() {
        assertTrue(isDivisibleByCurrentAction(target(value = 10), OperationSign.DIVISION, digit = 2))
    }

    @Test
    fun `division with a non-zero remainder is not divisible`() {
        assertFalse(isDivisibleByCurrentAction(target(value = 7), OperationSign.DIVISION, digit = 2))
    }

    @Test
    fun `division by a zero digit is never divisible`() {
        assertFalse(isDivisibleByCurrentAction(target(value = 10), OperationSign.DIVISION, digit = 0))
    }

    @Test
    fun `subtraction is never divisible even when it would clear the target`() {
        assertFalse(isDivisibleByCurrentAction(target(value = 10), OperationSign.SUBTRACTION, digit = 5))
    }
}
