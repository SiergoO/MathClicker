package com.sdamashchuk.mathbubbles.core.game.objectmapper

import com.sdamashchuk.mathbubbles.core.game.scheduledTarget
import com.sdamashchuk.mathbubbles.core.model.Target
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

    // A single uniform shift of the whole waiting schedule - shiftMs is fixed by the closest
    // waiting target alone, then every waiting target's appearsAtMs and finishesAtMs move by exactly
    // that constant. No Random draw: see this function's own doc comment for why the old per-target
    // reveal (which needed one) reintroduced the bug this whole epic exists to kill.
    @Test
    fun `shortenAppearanceDelay brings the closest waiting target to now and preserves its flight time`() {
        val targets =
            listOf(
                scheduledTarget(id = 1, value = 5, appearanceDelayMs = 100, lifetimeMs = 900),
                scheduledTarget(id = 2, value = 5, appearanceDelayMs = 5000, lifetimeMs = 900),
            )

        val shortened = targets.shortenAppearanceDelay(gameTimeMs = 0)

        val closest = shortened.first { it.id == 1 }
        assertEquals(0L, closest.appearsAtMs)
        assertEquals(900L, closest.finishesAtMs - closest.appearsAtMs)
    }

    @Test
    fun `shortenAppearanceDelay shifts every waiting target by the same amount - the closest one's own delay`() {
        val targets =
            (1..5).map { id ->
                scheduledTarget(id = id, value = 5, appearanceDelayMs = 100L * id, lifetimeMs = 900)
            }

        val shortened = targets.shortenAppearanceDelay(gameTimeMs = 0)

        // id 1 is the closest (delay 100), so shiftMs is exactly 100 - every target, including id 1
        // itself, moves earlier by that one constant.
        shortened.sortedBy { it.id }.forEach { target ->
            assertEquals(100L * target.id - 100L, target.appearsAtMs)
            assertEquals(900L, target.finishesAtMs - target.appearsAtMs)
        }
    }

    // The direct test for MC-73's fix to this function: a uniform shift subtracts one constant from
    // both ends of every interval, which cannot change any interval's length - so no shift can ever
    // recreate the triple-death bug this whole epic exists to kill.
    @Test
    fun `shortenAppearanceDelay preserves the pairwise spacing of every finish it shifts`() {
        val targets =
            (1..5).map { id ->
                scheduledTarget(id = id, value = 5, appearanceDelayMs = 100L * id, lifetimeMs = 900)
            }
        val originalFinishes = targets.associate { it.id to it.finishesAtMs }

        val shortened = targets.shortenAppearanceDelay(gameTimeMs = 0)

        for (a in targets) {
            for (b in targets) {
                val originalGap = originalFinishes.getValue(b.id) - originalFinishes.getValue(a.id)
                val shiftedGap =
                    shortened.first { it.id == b.id }.finishesAtMs -
                        shortened.first { it.id == a.id }.finishesAtMs
                assertEquals(originalGap, shiftedGap, "gap between id ${a.id} and id ${b.id} changed")
            }
        }
    }

    @Test
    fun `shortenAppearanceDelay is a no-op once every target is already visible`() {
        val targets = listOf(scheduledTarget(id = 1, value = 5))

        val shortened = targets.shortenAppearanceDelay(gameTimeMs = 0)

        assertEquals(targets, shortened)
    }

    @Test
    fun `shiftFinish holds a target by adding to finishesAtMs without moving appearsAtMs`() {
        val targets = listOf(target(id = 1, appearsAtMs = 0, finishesAtMs = 1000))

        val held = targets.shiftFinish(id = 1, byMs = 500, gameTimeMs = 0)

        assertEquals(0L, held.first().appearsAtMs)
        assertEquals(1500L, held.first().finishesAtMs)
    }

    @Test
    fun `shiftFinish speeds a target up by subtracting from finishesAtMs`() {
        val targets = listOf(target(id = 1, appearsAtMs = 0, finishesAtMs = 1000))

        val sped = targets.shiftFinish(id = 1, byMs = -400, gameTimeMs = 0)

        assertEquals(600L, sped.first().finishesAtMs)
    }

    // M4: shifting appearsAtMs alongside finishesAtMs would slide the whole flight instead of
    // holding or hurrying it - this pins appearsAtMs as untouched regardless of byMs's sign.
    @Test
    fun `shiftFinish never changes appearsAtMs`() {
        val targets = listOf(target(id = 1, appearsAtMs = 200, finishesAtMs = 1000))

        val held = targets.shiftFinish(id = 1, byMs = 5000, gameTimeMs = 0)
        val sped = targets.shiftFinish(id = 1, byMs = -5000, gameTimeMs = 0)

        assertEquals(200L, held.first().appearsAtMs)
        assertEquals(200L, sped.first().appearsAtMs)
    }

    // M5: removing the coerceAtLeast floor would let a large enough speed-up push finishesAtMs at
    // or below appearsAtMs, reopening Target.position's own zero-span guard.
    @Test
    fun `shiftFinish clamps a large speed-up so the span never inverts`() {
        val targets = listOf(target(id = 1, appearsAtMs = 1000, finishesAtMs = 1200))

        val sped = targets.shiftFinish(id = 1, byMs = -10_000, gameTimeMs = 0)

        assertTrue(sped.first().finishesAtMs > sped.first().appearsAtMs)
    }

    @Test
    fun `shiftFinish only ever touches the target with the matching id`() {
        val targets =
            listOf(
                target(id = 1, appearsAtMs = 0, finishesAtMs = 1000),
                target(id = 2, appearsAtMs = 0, finishesAtMs = 1000),
            )

        val held = targets.shiftFinish(id = 1, byMs = 500, gameTimeMs = 0)

        assertEquals(1500L, held.first { it.id == 1 }.finishesAtMs)
        assertEquals(1000L, held.first { it.id == 2 }.finishesAtMs)
    }

    @Test
    fun `shiftFinish is a no-op on an inactive target`() {
        val targets = listOf(target(id = 1, appearsAtMs = 0, finishesAtMs = 1000, isActive = false))

        val held = targets.shiftFinish(id = 1, byMs = 500, gameTimeMs = 0)

        assertEquals(targets, held)
    }

    @Test
    fun `shiftFinish is a no-op on a target that has already broken out`() {
        val targets = listOf(target(id = 1, appearsAtMs = 0, finishesAtMs = 1000))

        val held = targets.shiftFinish(id = 1, byMs = 500, gameTimeMs = 1000)

        assertEquals(targets, held)
    }
}
