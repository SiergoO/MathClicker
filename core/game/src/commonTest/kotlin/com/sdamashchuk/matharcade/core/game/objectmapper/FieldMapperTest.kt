package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FieldMapperTest {
    @Test
    fun `closeIfNecessary closes the field once lifeCount drops to zero or below`() {
        assertTrue(Field(lifeCount = 0).closeIfNecessary(nowMs = 1L).isClosed)
        assertTrue(Field(lifeCount = -1).closeIfNecessary(nowMs = 1L).isClosed)
        assertFalse(Field(lifeCount = 1).closeIfNecessary(nowMs = 1L).isClosed)
    }

    @Test
    fun `closeIfNecessary stamps finishedAt only when it actually closes the field`() {
        assertEquals(1234L, Field(lifeCount = 0).closeIfNecessary(nowMs = 1234L).finishedAt)
        assertNull(Field(lifeCount = 1).closeIfNecessary(nowMs = 1234L).finishedAt)
    }

    // resolveBreakouts calls this on every breakout, not just the one that empties the last life -
    // an already-closed field must never have its recorded date overwritten by a later, unrelated call.
    @Test
    fun `closeIfNecessary never overwrites a finishedAt the field already carries`() {
        val alreadyClosed = Field(lifeCount = 0, isClosed = true, finishedAt = 1000L)

        assertEquals(1000L, alreadyClosed.closeIfNecessary(nowMs = 5000L).finishedAt)
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

    // UpdateLevel no longer touches lifeCount at all - the every-N-levels grant MC-54 wired
    // through here is gone, levels 5 and 10 included, which used to qualify.
    @Test
    fun `updateLevel never changes lifeCount including on a former qualifying level`() {
        (1..11).forEach { startingLevel ->
            val before = Field(level = startingLevel, lifeCount = 1)
            val after = before.updateLevel()

            assertEquals(1, after.lifeCount, "level ${after.level} should not have changed lifeCount")
        }
    }

    // 3 is FieldMapper's own LIFE_BONUS_CAP, used as a literal here the same way LEVEL_MAX's 999
    // already is above - both are private to FieldMapper.
    @Test
    fun `grantLife adds one life below the cap but is a no-op at it`() {
        assertEquals(2, Field(lifeCount = 1).grantLife().lifeCount)
        assertEquals(3, Field(lifeCount = 2).grantLife().lifeCount)
        assertEquals(3, Field(lifeCount = 3).grantLife().lifeCount)
    }

    @Test
    fun `repeated grantLife calls never push lifeCount past the cap`() {
        var field = Field(lifeCount = 1)

        repeat(5) { field = field.grantLife() }

        assertEquals(3, field.lifeCount)
    }

    @Test
    fun `applyCombo is the number of targets the press scored on - not a running streak`() {
        assertEquals(3, Field(bonusMultiplier = 0).applyCombo(3).bonusMultiplier)
        assertEquals(1, Field(bonusMultiplier = 7).applyCombo(1).bonusMultiplier)
        assertEquals(0, Field(bonusMultiplier = 7).applyCombo(0).bonusMultiplier)
    }

    @Test
    fun `applyCombo has no ceiling of its own`() {
        assertEquals(10, Field(bonusMultiplier = 0).applyCombo(10).bonusMultiplier)
        assertEquals(1000, Field(bonusMultiplier = 0).applyCombo(1000).bonusMultiplier)
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
    fun `advanceClock adds stepMs onto whatever gameTimeMs already carries`() {
        assertEquals(250L, Field(gameTimeMs = 0).advanceClock(250).gameTimeMs)
        assertEquals(5250L, Field(gameTimeMs = 5000).advanceClock(250).gameTimeMs)
    }

    @Test
    fun `rewind subtracts byMs from gameTimeMs when it does not cross zero`() {
        assertEquals(4750L, Field(gameTimeMs = 5000).rewind(250).gameTimeMs)
    }

    // M1: removing the coerceAtLeast(0L) clamp lets this go negative instead.
    @Test
    fun `rewind clamps at zero rather than letting the clock go negative`() {
        assertEquals(0L, Field(gameTimeMs = 100).rewind(500).gameTimeMs)
    }

    // M2: a rewind that "helpfully" restores a spent life or reopens a closed field once gameTimeMs
    // winds back before finishedAt would be exactly the resurrection this operation must never
    // cause - isClosed, lifeCount and finishedAt are real, owned state, never derived from the
    // clock, so rewind must leave every one of them exactly as it found them.
    @Test
    fun `rewind never restores lifeCount or reopens an already closed field`() {
        val closed = Field(gameTimeMs = 5000, lifeCount = 0, isClosed = true, finishedAt = 1000L)

        val rewound = closed.rewind(10_000)

        assertEquals(0L, rewound.gameTimeMs)
        assertTrue(rewound.isClosed)
        assertEquals(0, rewound.lifeCount)
        assertEquals(1000L, rewound.finishedAt)
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
