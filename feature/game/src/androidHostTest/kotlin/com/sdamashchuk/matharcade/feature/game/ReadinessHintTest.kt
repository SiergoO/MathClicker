package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
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
    fallenMs = 0,
    appearanceDelayMs = 0,
    lifetimeMs = 0,
    isProfitable = isProfitable,
)

class ReadinessHintTest {
    @Test
    fun `a ready profitable target shows the hint when the policy allows it`() {
        assertTrue(
            shouldShowReadinessHint(target(value = 10), OperationSign.DIVISION, digit = 2, hintsEnabled = true),
        )
    }

    @Test
    fun `an unprofitable target never shows the hint even when the rule would say ready`() {
        assertFalse(
            shouldShowReadinessHint(
                target(value = 10, isProfitable = false),
                OperationSign.DIVISION,
                digit = 2,
                hintsEnabled = true,
            ),
        )
    }

    @Test
    fun `a disabled policy hides the hint for an otherwise ready profitable target`() {
        assertFalse(
            shouldShowReadinessHint(target(value = 10), OperationSign.DIVISION, digit = 2, hintsEnabled = false),
        )
    }

    @Test
    fun `a not-yet-ready profitable target does not show the hint`() {
        assertFalse(
            shouldShowReadinessHint(target(value = 7), OperationSign.DIVISION, digit = 2, hintsEnabled = true),
        )
    }
}
