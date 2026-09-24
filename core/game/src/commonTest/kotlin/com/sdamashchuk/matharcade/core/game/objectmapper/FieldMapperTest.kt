package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FieldMapperTest {
    @Test
    fun `closeIfNecessary closes the field once lifeCount drops to zero or below`() {
        assertTrue(Field(lifeCount = 0).closeIfNecessary().isClosed)
        assertTrue(Field(lifeCount = -1).closeIfNecessary().isClosed)
        assertFalse(Field(lifeCount = 1).closeIfNecessary().isClosed)
    }

    @Test
    fun `updateScore clamps the running total at zero instead of going negative`() {
        assertEquals(0, Field(score = 5).updateScore(-10).score)
        assertEquals(8, Field(score = 5).updateScore(3).score)
    }

    @Test
    fun `updateLevel increments normally below the clamp`() {
        assertEquals(2, Field(level = 1).updateLevel().level)
    }

    @Test
    fun `updateLevel clamps at 999 and does not advance past it`() {
        // Level 999 is SessionHelperImpl's own declared levelRange.last.
        var field = Field(level = 999)

        repeat(500) { field = field.updateLevel() }

        assertEquals(999, field.level)
    }

    @Test
    fun `advanceStreak increments a clean press and resets a failed one`() {
        assertEquals(1, Field(bonusMultiplier = 0).advanceStreak(pressFailed = false).bonusMultiplier)
        assertEquals(6, Field(bonusMultiplier = 5).advanceStreak(pressFailed = false).bonusMultiplier)
        assertEquals(0, Field(bonusMultiplier = 5).advanceStreak(pressFailed = true).bonusMultiplier)
    }

    @Test
    fun `advanceStreak has no ceiling`() {
        assertEquals(10, Field(bonusMultiplier = 9).advanceStreak(pressFailed = false).bonusMultiplier)
        assertEquals(11, Field(bonusMultiplier = 10).advanceStreak(pressFailed = false).bonusMultiplier)
        assertEquals(1001, Field(bonusMultiplier = 1000).advanceStreak(pressFailed = false).bonusMultiplier)
    }

    @Test
    fun `updateScore saturates instead of wrapping negative`() {
        // The reason the streak can be uncapped at all. Wrapping would land on a negative sum, which
        // the floor below then turns into a score of 0 - erasing the run of the only player good
        // enough to get there.
        assertEquals(Int.MAX_VALUE, Field(score = Int.MAX_VALUE - 1).updateScore(100).score)
        assertEquals(Int.MAX_VALUE, Field(score = Int.MAX_VALUE).updateScore(Int.MAX_VALUE).score)
        assertEquals(0, Field(score = 10).updateScore(-100).score)
    }

    @Test
    fun `resetStreak zeroes the streak regardless of its current value`() {
        assertEquals(0, Field(bonusMultiplier = 7).resetStreak().bonusMultiplier)
    }

    @Test
    fun `appliedMultiplier floors a resting streak at one but passes a live streak through`() {
        // The property itself lives on :core:model's Field, not this mapper - covered from here
        // because :core:model has no test source set, the same as Target.position.
        assertEquals(1, Field(bonusMultiplier = 0).appliedMultiplier)
        assertEquals(4, Field(bonusMultiplier = 4).appliedMultiplier)
    }

    @Test
    fun `updateActionButtons promotes the pending operation and queues a new one`() {
        val field =
            Field(
                currentOperationSign = OperationSign.DIVISION,
                currentOperationDigit = 0,
                nextOperationSign = OperationSign.SUBTRACTION,
                nextOperationDigit = 7,
            )

        val updated = field.updateActionButtons(OperationSign.DIVISION, 4)

        assertEquals(OperationSign.SUBTRACTION, updated.currentOperationSign)
        assertEquals(7, updated.currentOperationDigit)
        assertEquals(OperationSign.DIVISION, updated.nextOperationSign)
        assertEquals(4, updated.nextOperationDigit)
    }
}
