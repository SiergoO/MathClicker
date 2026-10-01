package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import com.sdamashchuk.mathbubbles.core.game.objectmapper.rewind
import com.sdamashchuk.mathbubbles.core.game.objectmapper.shiftFinish
import com.sdamashchuk.mathbubbles.core.model.Field
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// The closing property MC-74's design doc asks for directly: no axis operation may desynchronise
// two targets, because none of them carries its own time any more. Proven against a
// generated board rather than a single fixture, and against the two operations composed together,
// not just each in isolation.
private fun generatedBoard() =
    (1..5).map { id ->
        scheduledTarget(id = id, value = 5, appearanceDelayMs = 100L * id, lifetimeMs = 900L + 50L * id)
    }

// Finishes well after the clock the composition test runs at, so shiftFinish's no-op guard does not
// swallow the hold and leave the assertion vacuous.
private fun liveBoard() =
    (1..5).map { id ->
        scheduledTarget(id = id, value = 5, appearanceDelayMs = 100L * id, lifetimeMs = 9000L + 50L * id)
    }

@OptIn(ExperimentalCoroutinesApi::class)
class TimeAxisTest {
    // Every target's remaining time-to-finish is measured against the rewound clock, so the gaps
    // compared here are the ones the engine will actually read - the earlier version of this test
    // compared the untouched target list against a snapshot of itself and would have stayed green
    // with the rewind call deleted outright.
    @Test
    fun `rewind preserves every pairwise gap in remaining time-to-finish across a generated board`() {
        val targets = generatedBoard()
        val field = Field(gameTimeMs = 5000)
        val remainingBefore = targets.associate { it.id to it.finishesAtMs - field.gameTimeMs }

        val rewound = field.rewind(2000)

        val remainingAfter = targets.associate { it.id to it.finishesAtMs - rewound.gameTimeMs }
        assertNotEquals(remainingBefore, remainingAfter, "the rewind did not move the clock at all")
        for (a in targets) {
            for (b in targets) {
                assertEquals(
                    remainingBefore.getValue(b.id) - remainingBefore.getValue(a.id),
                    remainingAfter.getValue(b.id) - remainingAfter.getValue(a.id),
                    "gap between id ${a.id} and id ${b.id} changed",
                )
            }
        }
    }

    // The substantive content behind the interval check above: winding gameTimeMs back by byMs
    // hands every target the exact same byMs of remaining time-to-finish back, uniformly - not just
    // "the numbers didn't change" but "the whole board rises together".
    @Test
    fun `rewind gives every target on the board the same extra remaining time-to-finish`() {
        val targets = generatedBoard()
        val field = Field(gameTimeMs = 5000)
        val byMs = 2000L

        val rewoundField = field.rewind(byMs)

        targets.forEach { target ->
            val remainingBefore = target.finishesAtMs - field.gameTimeMs
            val remainingAfter = target.finishesAtMs - rewoundField.gameTimeMs
            assertEquals(remainingBefore + byMs, remainingAfter, "target ${target.id} did not gain the full rewind")
        }
    }

    @Test
    fun `shiftFinish moves exactly one target's schedule on a generated board`() {
        val targets = generatedBoard()

        val held = targets.shiftFinish(id = 3, byMs = 400, gameTimeMs = 0)

        targets.zip(held).forEach { (before, after) ->
            if (before.id == 3) {
                assertEquals(before.appearsAtMs, after.appearsAtMs)
                assertEquals(before.finishesAtMs + 400, after.finishesAtMs)
            } else {
                assertEquals(before, after)
            }
        }
    }

    // The rewound clock is threaded into the hold rather than a constant being handed to both
    // sides: shiftFinish's own no-op guard reads gameTimeMs, so passing the same fixed value to
    // both orders made the earlier version of this test pass with both sides no-opping. The board
    // here deliberately finishes well after the clock, so the hold genuinely applies.
    @Test
    fun `rewind and shiftFinish commute with the rewound clock threaded into the hold`() {
        val field = Field(gameTimeMs = 5000)
        val targets = liveBoard()

        val rewoundFirst = field.rewind(1000)
        val heldAfterRewind = targets.shiftFinish(id = 2, byMs = 300, gameTimeMs = rewoundFirst.gameTimeMs)

        val heldFirst = targets.shiftFinish(id = 2, byMs = 300, gameTimeMs = field.gameTimeMs)
        val rewoundAfterHold = field.rewind(1000)

        assertNotEquals(targets, heldAfterRewind, "the hold no-opped - the board is not live at this clock")
        assertEquals(rewoundFirst, rewoundAfterHold)
        assertEquals(heldAfterRewind, heldFirst)
    }

    // The safety property rewind exists to not break, proven through the engine rather than on
    // Field alone: winding the clock back past a target's own finishesAtMs must not charge a second
    // life for the same breakout. It holds because resolveBreakouts deactivates the target and
    // tick()'s breakout filter requires isActive - a chain across three files that nothing pinned
    // before, and that any future freeze or rewind perk rests on.
    @Test
    fun `rewinding past a breakout does not charge a second life for it`() =
        runTest {
            val seed = 99L
            val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))
            game.createField(1)
            game.createTargets()

            val startingLives = game.stateFlow.value.field.lifeCount
            while (game.stateFlow.value.field.lifeCount == startingLives) {
                game.tick(16)
            }
            val afterFirstLoss = game.stateFlow.value.field.lifeCount
            val clockAtLoss = game.stateFlow.value.field.gameTimeMs
            val brokenOutId =
                game.stateFlow.value.targets
                    .first { !it.isActive }
                    .id

            game.fieldRestored(
                game.stateFlow.value.field
                    .rewind(3000),
            )
            assertTrue(game.stateFlow.value.field.gameTimeMs < clockAtLoss)
            while (game.stateFlow.value.field.gameTimeMs < clockAtLoss) {
                game.tick(16)
            }

            assertEquals(afterFirstLoss, game.stateFlow.value.field.lifeCount)
            assertFalse(
                game.stateFlow.value.targets
                    .first { it.id == brokenOutId }
                    .isActive,
            )
        }
}
