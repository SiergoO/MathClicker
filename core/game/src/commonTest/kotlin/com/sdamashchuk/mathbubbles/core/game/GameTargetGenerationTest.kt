package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val TESTED_SEEDS = listOf(1L, 42L, 99L, 12345L)
private val TESTED_LEVELS = listOf(1, 10, 30, 100, 999)

// MC-73's generator properties, checked directly against Game.recreateTargets through the real
// SessionHelperImpl rather than inferred from a simulation: these are the two claims the whole task
// is built to make true by construction, not by luck of any one seed.
@OptIn(ExperimentalCoroutinesApi::class)
class GameTargetGenerationTest {
    // The assertion that makes the bug this epic exists for unreproducible: the gap between two
    // consecutive finishesAtMs is exactly the level's own finish spacing, regardless of what the
    // per-target flight-time draw lands on. No draw can touch it, because it is never built from one.
    @Test
    fun `consecutive generated targets differ by exactly the finish spacing - across seeds and levels`() =
        runTest {
            for (seed in TESTED_SEEDS) {
                for (level in TESTED_LEVELS) {
                    val sessionHelper = SessionHelperImpl(random = Random(seed))
                    val game = Game(sessionHelper, backgroundScope, Random(seed))
                    game.createField(1)
                    game.fieldRestored(
                        game.stateFlow.value.field
                            .copy(level = level),
                    )
                    game.createTargets()

                    val expectedSpacing = sessionHelper.getFinishSpacingMsByLevel(level).toLong()
                    val finishes =
                        game.stateFlow.value.targets
                            .sortedBy { it.id }
                            .map { it.finishesAtMs }
                    finishes.zipWithNext().forEach { (earlier, later) ->
                        assertEquals(expectedSpacing, later - earlier, "seed $seed level $level")
                    }
                }
            }
        }

    // MC-73's guard against a target starting mid-fall: getOpeningOffsetMsByLevel is at least the
    // level's own worst-case flight time, so no generated target's appearsAtMs can land before the
    // field's own gameTimeMs at creation.
    @Test
    fun `every target appearsAtMs is never before the field gameTimeMs - across seeds and levels`() =
        runTest {
            for (seed in TESTED_SEEDS) {
                for (level in TESTED_LEVELS) {
                    val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))
                    game.createField(1)
                    game.fieldRestored(
                        game.stateFlow.value.field
                            .copy(level = level),
                    )
                    game.createTargets()

                    val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                    game.stateFlow.value.targets.forEach { target ->
                        assertTrue(
                            target.appearsAtMs >= gameTimeMs,
                            "seed $seed level $level: target ${target.id} appearsAtMs ${target.appearsAtMs} is before gameTimeMs $gameTimeMs",
                        )
                    }
                }
            }
        }
}
