package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.model.Field
import kotlin.test.Test
import kotlin.test.assertEquals

class ComboMapperTest {
    @Test
    fun `applyCombo raises the multiplier one step when two or more targets changed`() {
        assertEquals(1, Field(bonusMultiplier = 0).applyCombo(changedTargetCount = 2).bonusMultiplier)
        assertEquals(1, Field(bonusMultiplier = 0).applyCombo(changedTargetCount = 12).bonusMultiplier)
        assertEquals(3, Field(bonusMultiplier = 2).applyCombo(changedTargetCount = 2).bonusMultiplier)
    }

    @Test
    fun `applyCombo resets the multiplier when zero or one target changed`() {
        assertEquals(0, Field(bonusMultiplier = 3).applyCombo(changedTargetCount = 1).bonusMultiplier)
        assertEquals(0, Field(bonusMultiplier = 3).applyCombo(changedTargetCount = 0).bonusMultiplier)
    }

    @Test
    fun `applyCombo stops raising the multiplier once appliedMultiplier reaches the x5 cap`() {
        var field = Field(bonusMultiplier = 0)

        repeat(50) { field = field.applyCombo(changedTargetCount = 2) }

        assertEquals(MAX_COMBO_MULTIPLIER, field.appliedMultiplier)
        assertEquals(MAX_COMBO_MULTIPLIER - 1, field.bonusMultiplier)
    }

    @Test
    fun `resetStreak zeroes the streak regardless of its current value`() {
        assertEquals(0, Field(bonusMultiplier = 7).resetStreak().bonusMultiplier)
    }
}
