package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.model.GameState
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

// Level-independent and unkillable, so a divergence can only come from the race itself. This also
// collapses every seed-sensitive output (sign, digit, score) to the same value regardless of seed.
private class RaceSessionHelper(
    private val amount: Int = 1,
    // Total opening offset for id 0, mirroring FakeSessionHelper's own shape: default 0 auto-floors
    // to the flight time below (100ms), so id 0 appears immediately unless overridden.
    private val openingOffsetMs: Int = 0,
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
    ) = UNKILLABLE_TARGET_VALUE

    override fun getTargetSpeedByLevel(level: Int) = 1f / FLIGHT_TIME_MS

    // Well under MAX_TICK_MS: a single BREAKOUT_TICK_MS tick both falls and breaks the target out in
    // one locked call, the same one-call atomicity the old single targetDidBreakout() call gave this
    // race - a flight time that needed several ticks to exhaust would spread the breakout across more
    // than one mutex acquisition and change what is being raced against fire.
    override fun getTargetFlightTimeMs(level: Int) = FLIGHT_TIME_MS

    override fun getFinishSpacingMsByLevel(level: Int) = finishSpacingMs

    override fun getOpeningOffsetMsByLevel(level: Int) = openingOffsetMs.coerceAtLeast(FLIGHT_TIME_MS)

    override fun getTargetAmountByLevel(level: Int) = amount

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = 1

    override fun getDivisionDigitByLevel(level: Int) = 1

    override fun getSubtractionDigitByLevel(level: Int) = 1

    override fun failedGrowthCap(level: Int) = UNKILLABLE_TARGET_VALUE
}

// Corrupted trials never reach the settled state polled for below, so they always burn the full
// timeout. Kept short so a red run stays fast: correct trials settle in low single-digit
// milliseconds (hundreds of times under this), so there is no realistic risk of a green trial
// timing out here.
private const val SETTLE_TIMEOUT_MS = 300L

private const val UNKILLABLE_TARGET_VALUE = 1_000_000

private const val FLIGHT_TIME_MS = 100

// Set by the weakest reader: a split write shows up on ~0.1% of level-up trials, so 2000 trials
// catch a torn build about 86% of the time on one target and about 98% across allTests' two.
private const val TEAR_TRIALS = 2000

// Comfortably above RaceSessionHelper's 100ms lifetime (and under MAX_TICK_MS), so the single tick
// call this drives always completes the fall in the same locked step it starts in.
private const val BREAKOUT_TICK_MS = 200

// A real Dispatchers.Default scope, not runTest's virtual time: the collector and the two racing
// calls must genuinely run on different threads. Settles by polling, since the timing is not fixed.
private suspend fun raceLevelUpAgainstFire(seed: Long): Pair<Field, List<Target>> {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        val game = Game(RaceSessionHelper(), scope, Random(seed))
        game.start()
        game.createField(1)
        game.createTargets()
        // Reveals the sole target (delay 0) without falling far enough to break out on its own
        // (lifetimeMs 100), so fire has something visible to act on when the race below starts.
        game.tick(1)
        val startingLevel = game.stateFlow.value.field.level

        scope.launch { game.tick(BREAKOUT_TICK_MS) }
        scope.launch { game.fireButtonClicked() }

        // A corrupted run never reaches the settled shape and is read as whatever it held at the timeout.
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (
                game.stateFlow.value.field.level != startingLevel + 1 ||
                game.stateFlow.value.targets.size != 1 ||
                !game.stateFlow.value.targets
                    .all { it.isActive }
            ) {
                yield()
            }
        }
        return game.stateFlow.value.field to game.stateFlow.value.targets
    } finally {
        scope.cancel()
    }
}

// A busy-polling reader on a second real thread must only ever see the target's active flag and the
// field's closed flag agree. The delayed second target keeps tick() from recreating the set instead.
private suspend fun readerSeesTornStateOnLastLifeBreakout(seed: Long): Boolean {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        // A huge finish spacing (rather than a per-id delay, no longer expressible) pushes
        // the second target's own appearsAtMs well past this test's window.
        val game =
            Game(
                RaceSessionHelper(amount = 2, finishSpacingMs = 999_999),
                scope,
                Random(seed),
            )
        game.createField(1)
        game.createTargets()
        val targetId =
            game.stateFlow.value.targets
                .first()
                .id
        game.tick(1)
        game.fieldRestored(
            game.stateFlow.value.field
                .copy(lifeCount = 1),
        )

        var torn = false
        val reader =
            scope.launch {
                while (isActive) {
                    val snapshot: GameState = game.stateFlow.value
                    val targetInactive = !snapshot.targets.first { it.id == targetId }.isActive
                    if (targetInactive != snapshot.field.isClosed) {
                        torn = true
                        break
                    }
                }
            }

        game.tick(BREAKOUT_TICK_MS)
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (!game.stateFlow.value.field.isClosed) yield()
        }
        reader.cancelAndJoin()
        return torn
    } finally {
        scope.cancel()
    }
}

// Invariant: once the level has advanced, a target set must exist to play, so a level above 1 with
// nothing active never holds inside the lock.
private suspend fun readerSeesTornStateOnLevelUp(seed: Long): Boolean {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        val game = Game(RaceSessionHelper(), scope, Random(seed))
        game.createField(1)
        game.createTargets()

        var torn = false
        val reader =
            scope.launch {
                while (isActive) {
                    val snapshot: GameState = game.stateFlow.value
                    if (snapshot.field.level > 1 && snapshot.targets.none { it.isActive }) {
                        torn = true
                        break
                    }
                }
            }

        game.tick(BREAKOUT_TICK_MS)
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (game.stateFlow.value.field.level < 2) yield()
        }
        reader.cancelAndJoin()
        return torn
    } finally {
        scope.cancel()
    }
}

// Sign and digit are pinned through fieldRestored, so every trial makes the same transition:
// 1_000_000 to 999_999 and score 0 to 1. Either both have moved or neither has.
private suspend fun readerSeesTornStateOnFirePress(seed: Long): Boolean {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        val game = Game(RaceSessionHelper(), scope, Random(seed))
        game.createField(1)
        game.createTargets()
        val targetId =
            game.stateFlow.value.targets
                .first()
                .id
        // Reveals the sole target (delay 0) without falling far enough to break out on its own
        // (lifetimeMs 100), so fire has something visible to act on.
        game.tick(1)
        game.fieldRestored(
            game.stateFlow.value.field.copy(
                score = 0,
                currentOperationSign = OperationSign.SUBTRACTION,
                currentOperationDigit = 1,
            ),
        )

        var torn = false
        val reader =
            scope.launch {
                while (isActive) {
                    val snapshot: GameState = game.stateFlow.value
                    val valueUntouched = snapshot.targets.first { it.id == targetId }.value == UNKILLABLE_TARGET_VALUE
                    if (valueUntouched != (snapshot.field.score == 0)) {
                        torn = true
                        break
                    }
                }
            }

        game.fireButtonClicked()
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (game.stateFlow.value.targets
                    .first { it.id == targetId }
                    .value == UNKILLABLE_TARGET_VALUE
            ) {
                yield()
            }
        }
        reader.cancelAndJoin()
        return torn
    } finally {
        scope.cancel()
    }
}

class GameConcurrencyTest {
    // tick() can fall a target, break it out and level up in one locked step; racing a fire press against
    // it has exactly one correct outcome - the level advances by one and the set is the fresh one.
    @Test
    fun `a tick racing a fire press never corrupts the level or loses the target set`() =
        runBlocking {
            val trials = 400
            // A corrupted trial burns the full timeout, and the assertion already fails once one appears, so the
            // loop stops early. The green path never corrupts and still runs all 400.
            val maxCorruptionsBeforeStopping = 5
            var corrupted = 0
            var ran = 0
            for (trial in 0 until trials) {
                ran++
                val (field, targets) = raceLevelUpAgainstFire(seed = trial.toLong())
                val ok = field.level == 2 && targets.size == 1 && targets.all { it.isActive }
                if (!ok) corrupted++
                if (corrupted >= maxCorruptionsBeforeStopping) break
            }
            assertEquals(0, corrupted, "tick vs fire race corrupted $corrupted/$ran trials")
        }

    // A second, independently-timed sample of the same race: it flags a mismatch if either run in a pair
    // lands away from the one valid outcome. Seeded determinism is GameTest's own end-to-end pin.
    @Test
    fun `two independent races settle on the same level and target set every time`() =
        runBlocking {
            val pairs = 20
            var diverged = 0
            repeat(pairs) { trial ->
                val seed = trial.toLong()
                val (firstField, firstTargets) = raceLevelUpAgainstFire(seed)
                val (secondField, secondTargets) = raceLevelUpAgainstFire(seed)
                val agree =
                    firstField.level == secondField.level &&
                        firstTargets.map { it.id to it.isActive } == secondTargets.map { it.id to it.isActive }
                if (!agree) diverged++
            }
            assertEquals(0, diverged, "$diverged/$pairs pairs landed on different outcomes")
        }

    // The direct proof for this task: before MC-42, targetDidBreakout published the target list and
    // the field as two separate StateFlow writes, so a reader outside the lock could catch the target
    // already deactivated while the field still reported the life as un-lost (or vice versa). One
    // GameState published in a single assignment closes that window.
    @Test
    fun `a reader never observes the target and field halves of a breakout half-applied`() =
        runBlocking {
            val trials = TEAR_TRIALS
            val maxTornBeforeStopping = 5
            var torn = 0
            var ran = 0
            for (trial in 0 until trials) {
                ran++
                if (readerSeesTornStateOnLastLifeBreakout(seed = trial.toLong())) torn++
                if (torn >= maxTornBeforeStopping) break
            }
            assertEquals(0, torn, "reader observed a torn field/target pairing in $torn/$ran trials")
        }

    @Test
    fun `a reader never observes a level-up half-applied`() =
        runBlocking {
            var torn = 0
            var ran = 0
            for (trial in 0 until TEAR_TRIALS) {
                ran++
                if (readerSeesTornStateOnLevelUp(seed = trial.toLong())) torn++
                if (torn >= 5) break
            }
            assertEquals(0, torn, "reader observed a level above 1 against a cleared board in $torn/$ran trials")
        }

    @Test
    fun `a reader never observes a fire press half-applied`() =
        runBlocking {
            var torn = 0
            var ran = 0
            for (trial in 0 until TEAR_TRIALS) {
                ran++
                if (readerSeesTornStateOnFirePress(seed = trial.toLong())) torn++
                if (torn >= 5) break
            }
            assertEquals(0, torn, "reader observed a rescored field against untouched targets in $torn/$ran trials")
        }
}
