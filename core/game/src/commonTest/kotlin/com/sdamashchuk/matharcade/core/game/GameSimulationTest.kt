package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.helper.SessionHelperImpl
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private const val TICK_MS = 16

// Generous relative to any real session (targets live at most tens of seconds): a bug that stops
// the field from ever closing hangs the test rather than the suite.
private const val MAX_SIMULATION_TICKS = 200_000

private class SimulationSessionHelper(
    private val targetAmount: Int = 1,
    private val targetValue: Int = 10,
    private val lifetimeMs: Int = 1_000_000,
    // Total opening offset for id 0, mirroring FakeSessionHelper's own shape: null auto-floors to
    // the flight time (lifetimeMs above), so id 0 appears immediately unless overridden.
    private val openingOffsetMs: Int? = null,
    private val finishSpacingMs: Int = 1,
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetFlightTimeMsRange = 20000..40000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ) = targetValue

    override fun getTargetSpeedByLevel(level: Int) = 1f / lifetimeMs

    override fun getTargetFlightTimeMs(level: Int) = lifetimeMs

    override fun getFinishSpacingMsByLevel(level: Int) = finishSpacingMs

    override fun getOpeningOffsetMsByLevel(level: Int) = (openingOffsetMs ?: lifetimeMs).coerceAtLeast(lifetimeMs)

    override fun getTargetAmountByLevel(level: Int) = targetAmount

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = 1

    override fun getDivisionDigitByLevel(level: Int) = 1

    override fun getSubtractionDigitByLevel(level: Int) = 1

    override fun failedGrowthCap(level: Int) = targetValue
}

// Drives tick() in fixed steps until the field closes, failing loudly instead of hanging if a
// regression stops the clock from ever ending the session.
private suspend fun Game.tickToGameOver(stepMs: Int = TICK_MS): Int {
    var elapsedMs = 0
    var ticks = 0
    while (!stateFlow.value.field.isClosed) {
        tick(stepMs)
        elapsedMs += stepMs
        ticks++
        check(ticks < MAX_SIMULATION_TICKS) { "field never closed within $MAX_SIMULATION_TICKS ticks" }
    }
    return elapsedMs
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameSimulationTest {
    // tick() alone plays the session out - nothing here touches breakout, reveal or level-up. The elapsed
    // time is pinned, not ranged: a range assertion recomputes the production formula and cannot fail.
    @Test
    fun `a full session runs to game over on ticks alone`() =
        runTest {
            val seed = 99L
            val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))
            game.createField(1)
            game.createTargets()

            val elapsedMs = game.tickToGameOver()

            assertTrue(game.stateFlow.value.field.isClosed)
            assertEquals(0, game.stateFlow.value.field.lifeCount)
            // Measured once and pinned: any change to the difficulty curve or the clock arithmetic moves it.
            assertEquals(15104, elapsedMs)
        }

    // A target's fall must survive a gap where tick() is never called - a paused composition. 250ms steps
    // divide the 1000ms lifetime evenly, so the tick that reaches position 1 is exact.
    @Test
    fun `pausing mid-fall and resuming keeps the accumulated fall`() =
        runTest {
            val game = Game(SimulationSessionHelper(lifetimeMs = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.createTargets()

            game.tick(250)
            game.tick(250)
            val target =
                game.stateFlow.value.targets
                    .first()
            assertEquals(0.5f, target.position(game.stateFlow.value.field.gameTimeMs))
            assertEquals(3, game.stateFlow.value.field.lifeCount)

            // "Paused": no tick() call happens here at all, for any stretch of real or notional
            // frames - there is nothing to represent, which is the point. Resuming is just calling
            // tick() again against the state left behind above.

            game.tick(250)
            assertEquals(0.75f, target.position(game.stateFlow.value.field.gameTimeMs))
            assertEquals(3, game.stateFlow.value.field.lifeCount)

            game.tick(250)
            assertEquals(2, game.stateFlow.value.field.lifeCount) // breakout on exactly this tick
        }

    // A session with no player input must outlast a single target's own fall. Ticking to exactly the first
    // target's lifetimeMs and finding the field open proves the wave is staggered, not stacked.
    @Test
    fun `a no-input session survives longer than its own first target's lifetime`() =
        runTest {
            val seed = 99L
            val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))
            game.createField(1)
            game.createTargets()
            val firstTarget =
                game.stateFlow.value.targets
                    .first { it.id == 1 }
            val firstTargetLifetimeMs = firstTarget.finishesAtMs - firstTarget.appearsAtMs

            var elapsedMs = 0
            while (elapsedMs < firstTargetLifetimeMs) {
                game.tick(TICK_MS)
                elapsedMs += TICK_MS
            }

            assertFalse(game.stateFlow.value.field.isClosed)
        }

    // MC-68's acceptance criterion: a no-input session's life losses must be spread out, not
    // clustered within a couple of milliseconds of each other - the device bug this epic closes.
    @Test
    fun `a no-input session spreads its three life losses out - across several levels`() =
        runTest {
            for (level in listOf(1, 10, 30, 100, 999)) {
                val seed = level.toLong()
                val sessionHelper = SessionHelperImpl(random = Random(seed))
                val game = Game(sessionHelper, backgroundScope, Random(seed))
                game.createField(1)
                game.fieldRestored(
                    game.stateFlow.value.field
                        .copy(level = level),
                )
                game.createTargets()

                val lifeLossTimes = mutableListOf<Long>()
                var previousLifeCount = game.stateFlow.value.field.lifeCount
                while (!game.stateFlow.value.field.isClosed) {
                    game.tick(TICK_MS)
                    val current = game.stateFlow.value.field
                    if (current.lifeCount < previousLifeCount) {
                        repeat(previousLifeCount - current.lifeCount) { lifeLossTimes.add(current.gameTimeMs) }
                        previousLifeCount = current.lifeCount
                    }
                }

                assertTrue(lifeLossTimes.size >= 2, "level $level: fewer than two life losses recorded")
                val interval = lifeLossTimes.last() - lifeLossTimes.first()
                // Less one tick: a breakout is detected on the first tick at or after its
                // scheduled finish, so the recorded interval can sit one tick short of the schedule.
                val expectedSpanMs =
                    (lifeLossTimes.size - 1).toLong() * sessionHelper.getFinishSpacingMsByLevel(level) - TICK_MS
                assertTrue(
                    interval >= expectedSpanMs,
                    "level $level: first-to-last life-loss interval ${interval}ms is under ${expectedSpanMs}ms",
                )
            }
        }

    // MAX_TICK_MS is the belt to the structural brace (a paused composition no longer feeds tick()
    // at all): one huge step must still not walk a target further than the clamp allows.
    @Test
    fun `a single oversized tick advances at most MAX_TICK_MS`() =
        runTest {
            val game = Game(SimulationSessionHelper(lifetimeMs = 20_000), backgroundScope, Random(2))
            game.createField(1)
            game.createTargets()

            game.tick(30_000)

            val target =
                game.stateFlow.value.targets
                    .first()
            assertEquals(MAX_TICK_MS.toLong(), game.stateFlow.value.field.gameTimeMs)
            assertTrue(target.isActive)
            assertEquals(3, game.stateFlow.value.field.lifeCount)
        }

    // GameTimeMs moves by the exact same clamped step targets.advance() consumes, not the
    // raw elapsedMs passed in - proven over many small ticks and one oversized one in the same run,
    // so a mutant that clamped only one of the two would fail here.
    @Test
    fun `gameTimeMs accumulates by exactly the clamped step over many ticks including an oversized one`() =
        runTest {
            val game = Game(SimulationSessionHelper(lifetimeMs = 1_000_000), backgroundScope, Random(6))
            game.createField(1)
            game.createTargets()

            repeat(10) { game.tick(TICK_MS) }
            assertEquals((TICK_MS * 10).toLong(), game.stateFlow.value.field.gameTimeMs)

            game.tick(30_000) // far past MAX_TICK_MS

            assertEquals((TICK_MS * 10 + MAX_TICK_MS).toLong(), game.stateFlow.value.field.gameTimeMs)
        }

    // Needs a fourth target still active and mid-fall when the other three's simultaneous breakout empties
    // lifeCount - the case an active-target check alone would still miss.
    @Test
    fun `gameTimeMs does not advance once the field is closed`() =
        runTest {
            // Applied identically to both games below, so the fast/slow comparison is unaffected by the fixture.
            val game = Game(SimulationSessionHelper(), backgroundScope, Random(7))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 10, appearanceDelayMs = 0, lifetimeMs = 1000),
                    scheduledTarget(id = 2, value = 10, appearanceDelayMs = 0, lifetimeMs = 1000),
                    scheduledTarget(id = 3, value = 10, appearanceDelayMs = 0, lifetimeMs = 1000),
                    scheduledTarget(id = 4, value = 10, appearanceDelayMs = 50_000, lifetimeMs = 1000),
                ),
            )
            val stillDelayedId =
                game.stateFlow.value.targets
                    .first { it.appearsAtMs > game.stateFlow.value.field.gameTimeMs }
                    .id

            // 1000ms: the three undelayed targets break out together, closing the field.
            repeat(4) { game.tick(250) }
            assertTrue(game.stateFlow.value.field.isClosed)
            assertTrue(
                game.stateFlow.value.targets
                    .first { it.id == stillDelayedId }
                    .isActive,
            )
            val gameTimeMsAtClose = game.stateFlow.value.field.gameTimeMs

            game.tick(TICK_MS)

            assertEquals(gameTimeMsAtClose, game.stateFlow.value.field.gameTimeMs)
        }

    // The thing the audit called inexpressible: exactly the targets whose individual delay has
    // elapsed by a fixed point in time are visible, and no others.
    @Test
    fun `ten seconds of ticks reveals exactly the targets whose delay has elapsed`() =
        runTest {
            // Applied identically to both games below, so the fast/slow comparison is unaffected by the fixture.
            val delaysMs = listOf(1000L, 5000L, 9000L, 11000L)
            val game = Game(SimulationSessionHelper(), backgroundScope, Random(3))
            game.createField(1)
            game.targetsRestored(
                delaysMs.mapIndexed { index, delayMs ->
                    scheduledTarget(id = index + 1, value = 10, appearanceDelayMs = delayMs, lifetimeMs = 1_000_000)
                },
            )

            repeat(40) { game.tick(250) } // 10_000ms total

            val gameTimeMs = game.stateFlow.value.field.gameTimeMs
            val visibility =
                game.stateFlow.value.targets
                    .sortedBy { it.id }
                    .map { it.isVisible(gameTimeMs) }
            assertEquals(listOf(true, true, true, false), visibility)
        }

    // Two engines seeded alike must play out alike under nothing but their own clocks - the same
    // guarantee GameTest's "seeded Game reproduces..." makes for the UI-driven path, now made for
    // tick() alone.
    @Test
    fun `two identical seeded simulations produce identical state and identical elapsed time`() =
        runTest {
            val seed = 55L
            // A real Clock.System advances between the two calls below by however long the first
            // simulation took to run, which would put a different finishedAt on each field - the
            // fixed instant is what keeps "identical seed, identical state" true of that column too.
            val fixedClock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(0)
                }

            fun newGame() = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed), fixedClock)

            val first = newGame()
            first.createField(1)
            first.createTargets()
            val firstElapsedMs = first.tickToGameOver()

            val second = newGame()
            second.createField(1)
            second.createTargets()
            val secondElapsedMs = second.tickToGameOver()

            assertEquals(firstElapsedMs, secondElapsedMs)
            assertEquals(first.stateFlow.value.field, second.stateFlow.value.field)
            assertEquals(first.stateFlow.value.targets, second.stateFlow.value.targets)
        }

    // shortenAppearanceDelay must fire on the edge, not on the level, or its draw count becomes a function
    // of frame rate - it would fire a different number of times at 8ms steps than at 16ms.
    @Test
    fun `the same seed driven at two step sizes over the same elapsed time produces identical state`() =
        runTest {
            val totalElapsedMs = 8000
            val seed = 11L

            // Applied identically to both games below, so the fast/slow comparison is unaffected by the fixture.
            fun targetFixture() =
                (0 until 8).map { index ->
                    scheduledTarget(
                        id = index + 1,
                        columnId = index.toGameColumnId(),
                        value = 10,
                        appearanceDelayMs = (2000 + index * 1000).toLong(),
                        lifetimeMs = 1_000_000,
                    )
                }

            fun newGame() = Game(SimulationSessionHelper(), backgroundScope, Random(seed))

            val fast = newGame()
            fast.createField(1)
            fast.targetsRestored(targetFixture())
            repeat(totalElapsedMs / 16) { fast.tick(16) }

            val slow = newGame()
            slow.createField(1)
            slow.targetsRestored(targetFixture())
            repeat(totalElapsedMs / 8) { slow.tick(8) }

            assertEquals(fast.stateFlow.value.field, slow.stateFlow.value.field)
            assertEquals(fast.stateFlow.value.targets, slow.stateFlow.value.targets)
        }
}
