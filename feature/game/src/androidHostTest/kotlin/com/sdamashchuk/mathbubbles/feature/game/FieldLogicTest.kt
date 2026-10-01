package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.mathbubbles.core.model.INITIAL_LIFE_COUNT
import org.junit.Assert.assertEquals
import org.junit.Test

class FieldLogicTest {
    @Test
    fun `calculateLifeSlotCount draws the starting cap's worth of slots below the cap`() {
        assertEquals(INITIAL_LIFE_COUNT, calculateLifeSlotCount(lifeCount = 1))
        assertEquals(INITIAL_LIFE_COUNT, calculateLifeSlotCount(lifeCount = INITIAL_LIFE_COUNT))
    }

    @Test
    fun `calculateLifeSlotCount grows past the cap so a bonus life above it stays visible`() {
        assertEquals(INITIAL_LIFE_COUNT + 1, calculateLifeSlotCount(lifeCount = INITIAL_LIFE_COUNT + 1))
    }

    @Test
    fun `calculateGameColumnWidth splits the measured width across four columns`() {
        // Both expectations are literals on purpose. GAME_COLUMN_COUNT is a const val, so deriving
        // the expected width from it compiles to the same bytecode as the production expression -
        // changing the constant moves both sides together and the test can never catch the drift it
        // exists to catch. Pinning the count makes whoever changes it come here; pinning the width
        // then fails if this call site did not move with it.
        assertEquals(4, GAME_COLUMN_COUNT)
        assertEquals(100, calculateGameColumnWidth(400))
    }

    @Test
    fun `scoreLabel shows the plain score at a multiplier of 1`() {
        assertEquals(
            "plain",
            scoreLabel(appliedMultiplier = 1, plainScore = "plain", comboScore = "x1 plain"),
        )
    }

    @Test
    fun `scoreLabel shows the combo prefix once the multiplier rises above 1`() {
        assertEquals(
            "x2 combo",
            scoreLabel(appliedMultiplier = 2, plainScore = "plain", comboScore = "x2 combo"),
        )
    }
}
