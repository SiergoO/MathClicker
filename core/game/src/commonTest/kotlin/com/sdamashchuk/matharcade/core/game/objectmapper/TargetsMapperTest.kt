package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.game.scheduledTarget
import com.sdamashchuk.matharcade.core.model.Target
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun target(
    id: Int,
    appearsAtMs: Long,
    finishesAtMs: Long,
    isProfitable: Boolean = true,
    isActive: Boolean = true,
) = Target(
    id = id,
    relatedFieldId = 0,
    columnId = 0,
    value = 5,
    appearsAtMs = appearsAtMs,
    finishesAtMs = finishesAtMs,
    isProfitable = isProfitable,
    isActive = isActive,
)

class TargetsMapperTest {
    @Test
    fun `a target hits zero and is retired by the follow-up ensureAlive pass`() {
        val targets = listOf(target(id = 1, appearsAtMs = 0, finishesAtMs = 1000))

        val decremented = targets.decrementValue(id = 1, decrement = 5)
        val retired = decremented.ensureAlive()

        assertEquals(0, retired.first().value)
        assertFalse(retired.first().isActive)
    }

    // The properties themselves live on :core:model's Target, not this mapper - covered from here
    // because :core:model has no test source set.
    @Test
    fun `position clamps to 0 before appearsAtMs and to 1 at or past finishesAtMs`() {
        val target = target(id = 1, appearsAtMs = 1000, finishesAtMs = 2000)

        assertEquals(0f, target.position(500))
        assertEquals(0f, target.position(1000))
        assertEquals(0.5f, target.position(1500))
        assertEquals(1f, target.position(2000))
        assertEquals(1f, target.position(3000))
    }

    @Test
    fun `position is 0 rather than dividing by zero for a zero or inverted span`() {
        val zeroSpan = target(id = 1, appearsAtMs = 1000, finishesAtMs = 1000)
        val invertedSpan = target(id = 2, appearsAtMs = 1000, finishesAtMs = 500)

        assertEquals(0f, zeroSpan.position(1000))
        assertEquals(0f, invertedSpan.position(1000))
    }

    @Test
    fun `isVisible flips from false to true exactly at appearsAtMs`() {
        val target = target(id = 1, appearsAtMs = 1000, finishesAtMs = 2000)

        assertFalse(target.isVisible(999))
        assertTrue(target.isVisible(1000))
    }

    @Test
    fun `hasBrokenOut flips from false to true exactly at finishesAtMs`() {
        val target = target(id = 1, appearsAtMs = 1000, finishesAtMs = 2000)

        assertFalse(target.hasBrokenOut(1999))
        assertTrue(target.hasBrokenOut(2000))
    }

    @Test
    fun `isTelegraphingBreakout flips on at the final 15 percent of the fall`() {
        val target = target(id = 1, appearsAtMs = 0, finishesAtMs = 1000)

        assertFalse(target.isTelegraphingBreakout(849))
        assertTrue(target.isTelegraphingBreakout(850))
        assertFalse(target.isTelegraphingBreakout(200))
        assertTrue(target.isTelegraphingBreakout(1000))
    }

    @Test
    fun `isTelegraphingBreakout is false rather than dividing by zero for a zero or negative span`() {
        val zeroSpan = target(id = 1, appearsAtMs = 500, finishesAtMs = 500)
        val invertedSpan = target(id = 2, appearsAtMs = 500, finishesAtMs = 400)

        assertFalse(zeroSpan.isTelegraphingBreakout(500))
        assertFalse(invertedSpan.isTelegraphingBreakout(500))
    }

    // MC-72's translation of the pre-MC-72 delay-shortening pass: pulls the closest 1..4 waiting
    // targets to right now and shifts every other waiting target's whole schedule earlier by the
    // same amount, preserving each one's own flight time exactly.
    @Test
    fun `shortenAppearanceDelay reveals the closest waiting targets now and preserves their flight time`() {
        val targets =
            listOf(
                scheduledTarget(id = 1, value = 5, appearanceDelayMs = 100, lifetimeMs = 900),
                scheduledTarget(id = 2, value = 5, appearanceDelayMs = 5000, lifetimeMs = 900),
            )

        val shortened = targets.shortenAppearanceDelay(gameTimeMs = 0, random = Random(1))

        val revealed = shortened.first { it.id == 1 }
        assertEquals(0L, revealed.appearsAtMs)
        assertEquals(900L, revealed.finishesAtMs - revealed.appearsAtMs)
    }

    // Five candidates so at least one is always left waiting regardless of how many the (1..4)
    // draw reveals: k is read back from the result itself rather than pinned, since which of 1..4
    // it lands on is Random(1)'s own business, not this test's.
    @Test
    fun `shortenAppearanceDelay shifts every target it does not reveal earlier by the same amount`() {
        val targets =
            (1..5).map { id ->
                scheduledTarget(id = id, value = 5, appearanceDelayMs = 100L * id, lifetimeMs = 900)
            }

        val shortened = targets.shortenAppearanceDelay(gameTimeMs = 0, random = Random(1))

        val revealed = shortened.filter { it.appearsAtMs == 0L }.sortedBy { it.id }
        val stillWaiting = shortened.filterNot { it.appearsAtMs == 0L }.sortedBy { it.id }
        assertTrue(revealed.isNotEmpty() && revealed.size < targets.size)
        val revealedCount = revealed.size
        val shiftMs = 100L * revealedCount
        stillWaiting.forEach { target ->
            assertEquals(100L * target.id - shiftMs, target.appearsAtMs)
            assertEquals(900L, target.finishesAtMs - target.appearsAtMs)
        }
        revealed.forEach { target -> assertEquals(900L, target.finishesAtMs - target.appearsAtMs) }
    }

    @Test
    fun `shortenAppearanceDelay is a no-op once every target is already visible`() {
        val targets = listOf(scheduledTarget(id = 1, value = 5))

        val shortened = targets.shortenAppearanceDelay(gameTimeMs = 0, random = Random(1))

        assertEquals(targets, shortened)
    }
}
