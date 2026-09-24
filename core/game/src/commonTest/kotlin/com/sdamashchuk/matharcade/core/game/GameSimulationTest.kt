package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.helper.SessionHelperImpl
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TICK_MS = 16

// Generous relative to any real session (targets live at most tens of seconds): a bug that stops
// the field from ever closing hangs the test rather than the suite.
private const val MAX_SIMULATION_TICKS = 200_000

private class SimulationSessionHelper(
    private val targetAmount: Int = 1,
    private val targetValue: Int = 10,
    private val lifetimeMs: Int = 1_000_000,
    private val appearanceDelayMsById: (Int) -> Int = { 0 },
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(level: Int) = targetValue

    override fun getTargetLifetimeMsByLevel(level: Int) = lifetimeMs

    override fun getTargetAppearanceDelayMsById(id: Int) = appearanceDelayMsById(id)

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
    // The definition of done for MC-38: before this commit the only way a test could close a field
    // was to call targetDidBreakout directly - simulating the thing the engine is now supposed to
    // know on its own. This test never touches breakout, reveal or level-up; tick() alone plays the
    // session out. The elapsed time is pinned, not ranged: a range assertion recomputes the
    // production formula and cannot fail (SessionHelperImplTest's "the starting difficulty..." is
    // the cautionary precedent this repo already has for that mistake).
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
            // Random(99) through SessionHelperImpl and Game's own draws, measured once and pinned:
            // a change to either the difficulty curve or the clock arithmetic moves this number.
            assertEquals(30640, elapsedMs)
        }

    // The MC-27 bug class as a JVM assertion for the first time: a target's accumulated fall must
    // survive a gap where tick() is simply never called (a paused/backgrounded composition), rather
    // than the pre-MC-38 UI re-deriving position from a saved fraction on every resume. 250ms steps
    // divide the 1000ms lifetime evenly, so the tick that reaches fallenMs == lifetimeMs is exact.
    @Test
    fun `pausing mid-fall and resuming keeps the accumulated fall`() =
        runTest {
            val game = Game(SimulationSessionHelper(lifetimeMs = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.createTargets()

            game.tick(250)
            game.tick(250)
            assertEquals(
                500,
                game.stateFlow.value.targets
                    .first()
                    .fallenMs,
            )
            assertEquals(3, game.stateFlow.value.field.lifeCount)

            // "Paused": no tick() call happens here at all, for any stretch of real or notional
            // frames - there is nothing to represent, which is the point. Resuming is just calling
            // tick() again against the state left behind above.

            game.tick(250)
            assertEquals(
                750,
                game.stateFlow.value.targets
                    .first()
                    .fallenMs,
            )
            assertEquals(3, game.stateFlow.value.field.lifeCount)

            game.tick(250)
            assertEquals(2, game.stateFlow.value.field.lifeCount) // breakout on exactly this tick
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
            assertEquals(MAX_TICK_MS, target.fallenMs)
            assertTrue(target.isActive)
            assertEquals(3, game.stateFlow.value.field.lifeCount)
        }

    // The thing the audit called inexpressible: exactly the targets whose individual delay has
    // elapsed by a fixed point in time are visible, and no others.
    @Test
    fun `ten seconds of ticks reveals exactly the targets whose delay has elapsed`() =
        runTest {
            val delaysMs = listOf(1000, 5000, 9000, 11000)
            val game =
                Game(
                    SimulationSessionHelper(
                        targetAmount = delaysMs.size,
                        lifetimeMs = 1_000_000,
                        appearanceDelayMsById = { delaysMs[it] },
                    ),
                    backgroundScope,
                    Random(3),
                )
            game.createField(1)
            game.createTargets()

            repeat(40) { game.tick(250) } // 10_000ms total

            val visibility =
                game.stateFlow.value.targets
                    .sortedBy { it.id }
                    .map { it.isVisible }
            assertEquals(listOf(true, true, true, false), visibility)
        }

    // Two engines seeded alike must play out alike under nothing but their own clocks - the same
    // guarantee GameTest's "seeded Game reproduces..." makes for the UI-driven path, now made for
    // tick() alone.
    @Test
    fun `two identical seeded simulations produce identical state and identical elapsed time`() =
        runTest {
            val seed = 55L

            fun newGame() = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))

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

    // The determinism trap named in the design doc: shortenAppearanceDelay must fire on the edge (a
    // visible active target existed and then didn't), not on the level (no visible active target
    // right now), or its draw count - and therefore the whole session's state - becomes a function
    // of frame rate. Every target here starts with a non-zero delay, so nothing is ever visible
    // before its own delay naturally elapses; a level-triggered shorten would fire on every one of
    // the many ticks before that first reveal, and it would fire a different number of times at 8ms
    // steps than at 16ms steps for the same elapsed time.
    @Test
    fun `the same seed driven at two step sizes over the same elapsed time produces identical state`() =
        runTest {
            val totalElapsedMs = 8000
            val seed = 11L

            fun newGame() =
                Game(
                    SimulationSessionHelper(
                        targetAmount = 8,
                        lifetimeMs = 1_000_000,
                        appearanceDelayMsById = { 2000 + it * 1000 },
                    ),
                    backgroundScope,
                    Random(seed),
                )

            val fast = newGame()
            fast.createField(1)
            fast.createTargets()
            repeat(totalElapsedMs / 16) { fast.tick(16) }

            val slow = newGame()
            slow.createField(1)
            slow.createTargets()
            repeat(totalElapsedMs / 8) { slow.tick(8) }

            assertEquals(fast.stateFlow.value.field, slow.stateFlow.value.field)
            assertEquals(fast.stateFlow.value.targets, slow.stateFlow.value.targets)
        }
}
