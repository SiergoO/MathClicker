package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun target(
    value: Int,
    isProfitable: Boolean = true,
) = Target(
    id = 1,
    relatedFieldId = 0,
    columnId = 0,
    value = value,
    appearsAtMs = 0,
    finishesAtMs = 0,
    isProfitable = isProfitable,
)

class DivisionHintTest {
    @Test
    fun `a divisible profitable target shows the hint when the policy allows it`() {
        assertTrue(
            shouldShowDivisionHint(target(value = 10), OperationSign.DIVISION, digit = 2, hintsEnabled = true),
        )
    }

    @Test
    fun `a non-divisible profitable target does not show the hint`() {
        assertFalse(
            shouldShowDivisionHint(target(value = 7), OperationSign.DIVISION, digit = 2, hintsEnabled = true),
        )
    }

    @Test
    fun `subtraction never shows the hint even when it would clear the target`() {
        assertFalse(
            shouldShowDivisionHint(target(value = 10), OperationSign.SUBTRACTION, digit = 5, hintsEnabled = true),
        )
    }

    @Test
    fun `an unprofitable divisible target never shows the hint`() {
        assertFalse(
            shouldShowDivisionHint(
                target(value = 10, isProfitable = false),
                OperationSign.DIVISION,
                digit = 2,
                hintsEnabled = true,
            ),
        )
    }

    @Test
    fun `a disabled policy hides the hint for an otherwise divisible profitable target`() {
        assertFalse(
            shouldShowDivisionHint(target(value = 10), OperationSign.DIVISION, digit = 2, hintsEnabled = false),
        )
    }
}
