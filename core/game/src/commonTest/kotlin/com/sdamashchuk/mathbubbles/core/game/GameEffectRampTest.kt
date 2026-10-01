package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.model.Booster
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Local copies of Game's own private tunables, not imports, so a change to either shows up here as
// a failure rather than silently re-deriving the windows these tests probe.
private const val RAMP_MS = 200
private const val FREEZE_MS = 3_000
private const val REWIND_MS = 2_000
private const val STEP_MS = 20

@OptIn(ExperimentalCoroutinesApi::class)
class GameEffectRampTest {
    @Test
    fun `freeze's clock advance during the ramp-in is strictly between 0 and full speed and never increases`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE),
            )
            game.fireButtonClicked()

            val deltas = mutableListOf<Long>()
            var previousGameTimeMs = game.stateFlow.value.field.gameTimeMs
            repeat(RAMP_MS / STEP_MS) {
                game.tick(STEP_MS)
                val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                deltas.add(gameTimeMs - previousGameTimeMs)
                previousGameTimeMs = gameTimeMs
            }

            deltas.forEach { delta ->
                assertTrue(
                    delta in 1 until STEP_MS.toLong(),
                    "expected a ramped step strictly between 0 and $STEP_MS, got $delta",
                )
            }
            deltas.zipWithNext().forEach { (earlier, later) ->
                assertTrue(later < earlier, "expected a strictly slowing clock, got $earlier then $later")
            }
        }

    @Test
    fun `freeze's clock advance during the ramp-out stays strictly between 0 and full speed and hands off cleanly`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE),
            )
            game.fireButtonClicked()

            val deltas = mutableListOf<Long>()
            var previousGameTimeMs = game.stateFlow.value.field.gameTimeMs
            repeat(FREEZE_MS / STEP_MS + 1) {
                game.tick(STEP_MS)
                val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                deltas.add(gameTimeMs - previousGameTimeMs)
                previousGameTimeMs = gameTimeMs
            }

            val rampOutDeltas = deltas.subList(deltas.size - 1 - RAMP_MS / STEP_MS, deltas.size - 1)
            rampOutDeltas.forEach { delta ->
                assertTrue(
                    delta in 1 until STEP_MS.toLong(),
                    "expected a ramped step strictly between 0 and $STEP_MS, got $delta",
                )
            }
            rampOutDeltas.zipWithNext().forEach { (earlier, later) ->
                assertTrue(later > earlier, "expected a strictly speeding-up clock, got $earlier then $later")
            }
            assertEquals(STEP_MS.toLong(), deltas.last(), "expected a clean hand-off to full speed")
        }

    @Test
    fun `freeze advances the clock by exactly 0 once the ramp-in has settled into its hold`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE),
            )
            game.fireButtonClicked()

            repeat(RAMP_MS / STEP_MS) { game.tick(STEP_MS) } // spends the ramp-in
            val gameTimeMsAfterRamp = game.stateFlow.value.field.gameTimeMs

            game.tick(STEP_MS)
            assertEquals(
                gameTimeMsAfterRamp,
                game.stateFlow.value.field.gameTimeMs,
                "the hold must advance the clock by 0",
            )
        }

    @Test
    fun `rewind's rate passes through 0 without a sign jump`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 5_000, currentBooster = Booster.REWIND),
            )
            game.fireButtonClicked()

            val deltas = mutableListOf<Long>()
            var previousGameTimeMs = game.stateFlow.value.field.gameTimeMs
            repeat(RAMP_MS / STEP_MS) {
                game.tick(STEP_MS)
                val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                deltas.add(gameTimeMs - previousGameTimeMs)
                previousGameTimeMs = gameTimeMs
            }

            deltas.zipWithNext().forEach { (earlier, later) ->
                assertTrue(
                    later <= earlier,
                    "a sign jump would show up as a step larger than its predecessor: $earlier then $later",
                )
            }
            assertTrue(deltas.any { it > 0 }, "the rate must start on the forward side of 0")
            assertTrue(deltas.any { it < 0 }, "the rate must end up rewinding past 0")
        }

    @Test
    fun `a rewind cut by freeze crosses over through the ramp instead of jumping`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    gameTimeMs = 5_000,
                    currentBooster = Booster.REWIND,
                    nextBooster = Booster.FREEZE,
                ),
            )
            game.fireButtonClicked() // rewind
            repeat(RAMP_MS / STEP_MS) { game.tick(STEP_MS) } // settles the rewind into its -1 hold

            game.fireButtonClicked() // freeze cuts the rewind short
            val deltas = mutableListOf<Long>()
            var previousGameTimeMs = game.stateFlow.value.field.gameTimeMs
            repeat(RAMP_MS / STEP_MS) {
                game.tick(STEP_MS)
                val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                deltas.add(gameTimeMs - previousGameTimeMs)
                previousGameTimeMs = gameTimeMs
            }

            // A jump back to Freeze's own neutral rate would step forward on this first tick;
            // continuing from Rewind's held -1 instead lands within 1ms of -19 on a 20ms step.
            assertTrue(
                deltas.first() in -20L..-18L,
                "expected the first crossover step within 1ms of -19, got ${deltas.first()}",
            )
            deltas.zipWithNext().forEach { (earlier, later) ->
                assertTrue(later > earlier, "expected a strictly easing clock, got $earlier then $later")
            }
        }

    @Test
    fun `freeze cut by rewind eases the water tint out instead of popping it to 0`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    gameTimeMs = 5_000,
                    currentBooster = Booster.FREEZE,
                    nextBooster = Booster.REWIND,
                ),
            )
            game.fireButtonClicked() // freeze
            repeat(RAMP_MS / STEP_MS) { game.tick(STEP_MS) } // settles fully frozen
            assertEquals(1f, game.stateFlow.value.effects.freezeTintIntensity)

            game.fireButtonClicked() // rewind cuts the freeze short
            val tints = mutableListOf<Float>()
            repeat(RAMP_MS / STEP_MS) {
                game.tick(STEP_MS)
                tints.add(game.stateFlow.value.effects.freezeTintIntensity)
            }

            assertTrue(tints.first() > 0.7f, "expected the tint to still read mostly full right after the swap")
            tints.zipWithNext().forEach { (earlier, later) ->
                assertTrue(later <= earlier, "expected a monotonically easing tint, got $earlier then $later")
            }
            assertTrue(tints.last() < 1e-6f, "expected the tint to fully fade by the end of the ramp")
        }

    @Test
    fun `a plain rewind never shows any freeze tint`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 5_000, currentBooster = Booster.REWIND),
            )
            game.fireButtonClicked()

            repeat(REWIND_MS / STEP_MS + 1) {
                game.tick(STEP_MS)
                assertEquals(
                    0f,
                    game.stateFlow.value.effects.freezeTintIntensity,
                    "the rate crossing 0 must not read as a flash of Freeze",
                )
            }
        }

    @Test
    fun `a natural freeze end fades the water tint out over the same ramp as the clock`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 5_000, currentBooster = Booster.FREEZE),
            )
            game.fireButtonClicked()

            repeat((FREEZE_MS - RAMP_MS / 2) / STEP_MS) { game.tick(STEP_MS) }
            val midRampOut = game.stateFlow.value.effects.freezeTintIntensity
            assertTrue(midRampOut in 0.35f..0.65f, "expected about half tint mid ramp-out, got $midRampOut")

            repeat(RAMP_MS / 2 / STEP_MS) { game.tick(STEP_MS) }
            assertNull(game.stateFlow.value.effects.timedBooster)
            assertTrue(
                game.stateFlow.value.effects.freezeTintIntensity <= 0.1f,
                "expected the tint gone when the freeze ends, got ${game.stateFlow.value.effects.freezeTintIntensity}",
            )
        }

    @Test
    fun `rewind cut by freeze eases the water tint in instead of leaving it at 0`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    gameTimeMs = 5_000,
                    currentBooster = Booster.REWIND,
                    nextBooster = Booster.FREEZE,
                ),
            )
            game.fireButtonClicked() // rewind
            repeat(RAMP_MS / STEP_MS) { game.tick(STEP_MS) } // settles into its -1 hold
            assertEquals(0f, game.stateFlow.value.effects.freezeTintIntensity)

            game.fireButtonClicked() // freeze cuts the rewind short
            val tints = mutableListOf<Float>()
            repeat(RAMP_MS / STEP_MS) {
                game.tick(STEP_MS)
                tints.add(game.stateFlow.value.effects.freezeTintIntensity)
            }

            tints.zipWithNext().forEach { (earlier, later) ->
                assertTrue(later >= earlier, "expected a monotonically easing-in tint, got $earlier then $later")
            }
            assertTrue(tints.last() > 1f - 1e-6f, "expected the tint to fully ease in by the end of the ramp")
        }

    @Test
    fun `re-applying freeze while it is already holding keeps the envelope at full intensity throughout`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE, nextBooster = Booster.FREEZE),
            )
            game.fireButtonClicked() // first freeze
            repeat(RAMP_MS / STEP_MS) { game.tick(STEP_MS) } // fully ramped in
            assertEquals(1f, game.stateFlow.value.effects.intensity)

            game.fireButtonClicked() // restarts the hold
            game.tick(STEP_MS)

            assertEquals(
                1f,
                game.stateFlow.value.effects.intensity,
                "a re-ramp would have dipped the intensity below 1",
            )
        }

    @Test
    fun `rewind clamped at the top edge still ramps out smoothly to a clean hand-off at full speed`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 50, currentBooster = Booster.REWIND),
            )
            game.fireButtonClicked()

            val deltas = mutableListOf<Long>()
            var previousGameTimeMs = game.stateFlow.value.field.gameTimeMs
            while (game.stateFlow.value.effects.timedBooster != null) {
                game.tick(STEP_MS)
                val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                deltas.add(gameTimeMs - previousGameTimeMs)
                previousGameTimeMs = gameTimeMs
            }
            game.tick(STEP_MS)
            val postExpiryDelta = game.stateFlow.value.field.gameTimeMs - previousGameTimeMs

            // The clamp legitimately holds the clock at 0 while the rate is still negative, so
            // leading zeros are dropped before checking the climb once movement resumes.
            val resumedMovement = deltas.takeLast(RAMP_MS / STEP_MS).dropWhile { it <= 0 }
            assertTrue(resumedMovement.isNotEmpty(), "expected the clock to resume moving before expiry")
            resumedMovement.zipWithNext().forEach { (earlier, later) ->
                assertTrue(later > earlier, "expected a strictly speeding-up clock, got $earlier then $later")
            }
            assertEquals(STEP_MS.toLong(), postExpiryDelta, "expected a clean hand-off to full speed")
        }

    @Test
    fun `freeze cutting a rewind held at -1 never pushes the newest visible bubble past its own appearance`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, appearanceDelayMs = 20, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    gameTimeMs = 70,
                    currentBooster = Booster.REWIND,
                    nextBooster = Booster.FREEZE,
                ),
            )
            game.fireButtonClicked() // rewind
            repeat(RAMP_MS / STEP_MS) { game.tick(STEP_MS) } // settles into its -1 hold, back at 70
            game.tick(STEP_MS) // one more hold tick: 70 -> 50, 30ms ahead of the bubble's appearance at 20

            game.fireButtonClicked() // freeze carries the held -1 across the crossover
            var elapsedSinceFreezeMs = 0
            var previousGameTimeMs = game.stateFlow.value.field.gameTimeMs
            while (game.stateFlow.value.effects.timedBooster != null) {
                game.tick(STEP_MS)
                elapsedSinceFreezeMs += STEP_MS
                val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                assertTrue(
                    game.stateFlow.value.targets
                        .single()
                        .isVisible(gameTimeMs),
                    "the bubble was pushed back past its own appearance",
                )
                if (elapsedSinceFreezeMs == FREEZE_MS / 2) {
                    assertEquals(0L, gameTimeMs - previousGameTimeMs, "expected the clock still held mid-budget")
                }
                previousGameTimeMs = gameTimeMs
            }
            assertTrue(
                elapsedSinceFreezeMs in FREEZE_MS - 20..FREEZE_MS + 20,
                "expected freeze to keep its full ~${FREEZE_MS}ms budget, got $elapsedSinceFreezeMs",
            )
        }

    @Test
    fun `freeze fired on the exact tick the rewind clamp would hit still respects the clamp`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, appearanceDelayMs = 20, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    gameTimeMs = 70,
                    currentBooster = Booster.REWIND,
                    nextBooster = Booster.FREEZE,
                ),
            )
            game.fireButtonClicked() // rewind
            repeat(RAMP_MS / STEP_MS) { game.tick(STEP_MS) } // settles into its -1 hold, back at 70
            repeat(3) { game.tick(STEP_MS) } // 70 -> 50 -> 30 -> 10, the next tick alone would clamp

            // Fired here, before that next tick, so the clamp hits on the first tick under FREEZE.
            game.fireButtonClicked()
            var elapsedSinceFreezeMs = 0
            var previousGameTimeMs = game.stateFlow.value.field.gameTimeMs
            while (game.stateFlow.value.effects.timedBooster != null) {
                game.tick(STEP_MS)
                elapsedSinceFreezeMs += STEP_MS
                val gameTimeMs = game.stateFlow.value.field.gameTimeMs
                assertTrue(
                    game.stateFlow.value.targets
                        .single()
                        .isVisible(gameTimeMs),
                    "the bubble was pushed back past its own appearance",
                )
                if (elapsedSinceFreezeMs == FREEZE_MS / 2) {
                    assertEquals(0L, gameTimeMs - previousGameTimeMs, "expected the clock still held mid-budget")
                }
                previousGameTimeMs = gameTimeMs
            }
            assertTrue(
                elapsedSinceFreezeMs in FREEZE_MS - 20..FREEZE_MS + 20,
                "expected freeze to keep its full ~${FREEZE_MS}ms budget, got $elapsedSinceFreezeMs",
            )
        }

    @Test
    fun `rewind clamped at the top edge never pushes the newest visible bubble past its own appearance`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 50, currentBooster = Booster.REWIND),
            )
            game.fireButtonClicked()

            repeat(20) {
                game.tick(STEP_MS)
                assertTrue(
                    game.stateFlow.value.targets
                        .single()
                        .isVisible(game.stateFlow.value.field.gameTimeMs),
                    "the bubble was pushed back past its own appearance",
                )
            }
            // Not exactly 0: each tick's step truncates to a whole ms, so a few ms of slack
            // accumulate near the clamp rather than landing on it exactly.
            assertTrue(game.stateFlow.value.field.gameTimeMs in 0..10, "expected the clamp to hold near 0")
        }
}
