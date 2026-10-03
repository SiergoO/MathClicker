package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val persisted = Field(id = 1, gameTimeMs = 10_000)

class ShouldPersistFieldTest {
    @Test
    fun `an identical field is not persisted again`() {
        assertFalse(shouldPersistField(persisted, persisted.copy()))
    }

    @Test
    fun `a clock advance below the interval alone is not persisted`() {
        assertFalse(shouldPersistField(persisted, persisted.copy(gameTimeMs = 12_999)))
    }

    @Test
    fun `a clock advance of the full interval is persisted`() {
        assertTrue(shouldPersistField(persisted, persisted.copy(gameTimeMs = 13_000)))
    }

    @Test
    fun `a clock moved back by the full interval is persisted`() {
        assertTrue(shouldPersistField(persisted, persisted.copy(gameTimeMs = 7_000)))
    }

    @Test
    fun `a clock moved back below the interval alone is not persisted`() {
        assertFalse(shouldPersistField(persisted, persisted.copy(gameTimeMs = 7_001)))
    }

    @Test
    fun `a running effect's countdown and tint alone are not persisted`() {
        val running = persisted.copy(timedEffectBooster = Booster.FREEZE, timedEffectRemainingMs = 5_000)
        val later =
            running.copy(
                gameTimeMs = 10_500,
                timedEffectRemainingMs = 4_500,
                timedEffectRate = 0.5,
                freezeTintEnvelope = 0.4,
            )
        assertFalse(shouldPersistField(running, later))
    }

    @Test
    fun `each discrete change is persisted even with the clock unchanged`() {
        val changes =
            listOf(
                persisted.copy(score = 5),
                persisted.copy(lifeCount = persisted.lifeCount - 1),
                persisted.copy(level = 2),
                persisted.copy(bonusMultiplier = 1),
                persisted.copy(currentOperationSign = OperationSign.SUBTRACTION),
                persisted.copy(nextOperationDigit = 7),
                persisted.copy(currentBooster = Booster.SHIELD),
                persisted.copy(boosterStash = listOf(Booster.SHIELD)),
                persisted.copy(timedEffectBooster = Booster.FREEZE),
                persisted.copy(icePickArmedFireButton = true),
                persisted.copy(shieldActive = true),
                persisted.copy(isClosed = true, finishedAt = 1L),
            )
        changes.forEach { assertTrue("$it", shouldPersistField(persisted, it)) }
    }
}
