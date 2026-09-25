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

// A level-independent, unkillable target: amount and value never change with level, and the value
// is far past anything a single subtract-1-or-divide-by-1 press can zero out. That isolates the
// one interleaving MC-30 finding 1 measured (breakout-triggered level-up vs a fire press) from
// every other source of state change, so a divergence can only come from the race itself. Note
// this also makes every seed-sensitive output (OperationSign, digit, score) collapse to the same
// value regardless of seed - fine for the race test below, but see the second test's comment.
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

// Per-trial detection is not uniform across the three readers: injecting a split write measures
// roughly 100% for the breakout reader, 9% for the fire reader and 0.1% for the level-up one. That
// last figure is what sets this number - at 0.1% per trial, 2000 trials catch a torn build about
// 86% of the time on one target and about 98% across allTests' two. Raising it further is cheap if
// a tear ever slips through, but the honest claim is "very likely", not "certain".
private const val TEAR_TRIALS = 2000

// Comfortably above RaceSessionHelper's 100ms lifetime (and under MAX_TICK_MS), so the single tick
// call this drives always completes the fall in the same locked step it starts in.
private const val BREAKOUT_TICK_MS = 200

// Runs the single-target-tick-vs-fire race on a real Dispatchers.Default scope - the collector
// launched by start() and the two racing calls genuinely run on different threads, unlike GameTest's
// virtual-time runTest. Settles by polling rather than a fixed delay: the correct end state is
// always "one fresh, fully active target", however long the collector takes to get there.
//
// Breakout now originates inside tick() under the same mutex fire uses, so the breakout-vs-fire race
// this test reproduced pre-MC-38 is structurally impossible: whichever of tick()/fireButtonClicked()
// acquires the lock first fully completes before the other starts. What remains genuinely contended
// is tick() itself (mutating targets, field, and potentially leveling up) against a concurrent fire
// press - MC-32's race in its current form.
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

        // field and targets publish as one GameState now, so there is no torn instant left to sample
        // - but the two launched coroutines can still legitimately take a few dispatch cycles to
        // reach the settled shape depending on which wins the race. Wait for it instead of a fixed
        // delay; a genuinely corrupted run (stuck, or advanced more than once) never reaches this and
        // is read as whatever it settled on at the timeout.
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

// Isolates the reader/writer tear MC-42 exists for from the writer/writer race above: a single
// tick() call that costs the last life writes both the deactivated target and the closed field
// under one mutex.withLock. A concurrent reader busy-polling stateFlow.value from a second real
// thread - no suspension point in the loop, so nothing yields the window away - must only ever see
// the target's active flag and the field's closed flag agree; either both still false (untouched) or
// both true (fully applied), never one ahead of the other. No start(): the level-up this would
// otherwise trigger is a different transition, already covered above. A second target, delayed well
// past this test's window, keeps the board non-empty so the breakout below doesn't also satisfy
// tick()'s own all-inactive check - that recreates the target set (id 1 included) as a fresh, active
// one in the same locked step regardless of start()/isClosed, which would confuse the invariant being
// checked here with a legitimate, unrelated state change.
private suspend fun readerSeesTornStateOnLastLifeBreakout(seed: Long): Boolean {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        // MC-73: a huge finish spacing (rather than a per-id delay, no longer expressible) pushes
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

// The level-up half of the same tear. tick() publishes the bumped field and the regenerated target
// set in one assignment when the board it just broke out empties - before MC-42 the pre-tick
// collector path wrote them as two, so a reader could catch level 2 against the cleared level-1
// board, the "new level against the previous target set" the task exists to close. No start(): with
// breakout and the level-up it can cause both decided inside tick() itself, the collector has
// nothing left to drive here. Invariant: once the level has advanced, a target set must exist to
// play, so a level above 1 with nothing active is a state that never holds inside the lock.
// RaceSessionHelper's targets are unkillable and never broken out again, so level 2 with an
// all-inactive board cannot arise legitimately after the single level-up this drives.
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

// The fire-press half. fireButtonClicked publishes the operated-on targets and the rescored field
// together; split, a reader sees one ahead of the other. The operation sign and digit are pinned
// through fieldRestored rather than left to the seed, so the transition is the same on every trial:
// subtracting 1 from RaceSessionHelper's 1_000_000 takes the value to 999_999 and the score from 0
// to 1 in one step. Either both have moved or neither has.
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
    // Reproduces MC-30 finding 1 in its post-MC-38 form: breakout-vs-fire is now structurally
    // impossible (breakout only ever originates inside tick(), under the same mutex fire uses), so
    // the contended pair is tick() itself - which can fall a target, break it out, and level up, all
    // in one locked step - racing a single fire press from a second coroutine, both on
    // Dispatchers.Default. Correct behaviour has exactly one outcome regardless of which side wins
    // the race: the level advances by exactly one and the target set is the fresh one-target set for
    // the new level, never a torn mix of the two, never applied twice.
    @Test
    fun `a tick racing a fire press never corrupts the level or loses the target set`() =
        runBlocking {
            val trials = 400
            // A corrupted trial always burns the full SETTLE_TIMEOUT_MS, so on unlocked code this
            // loop used to cost minutes per platform (150.6s JVM, 44.3s iOS at the old 2000ms
            // timeout and no cap). The assertion below already fails once corrupted > 0, so once a
            // handful of trials have shown the race, running the rest of the 400 only adds wall
            // clock without adding information. The green path is unaffected - it never corrupts,
            // so it never stops early and still runs all 400.
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

    // A second, pairwise angle on the same race - not a reproducibility check, despite the name
    // this test originally had. RaceSessionHelper's amount/value/delay/lifetime are level-independent
    // constants, and OperationSign/digit/score are excluded from the comparison below because they
    // legitimately depend on whether the fire press lands before or after the breakout in real time
    // (an ordinary race between two independent player actions, not corruption). Between those two
    // exclusions every seed produces the same level and the same target id/active set, so comparing
    // two runs on the SAME seed is no more informative here than comparing two runs on different
    // seeds would be - review caught that the original name and comment claimed otherwise. What this
    // test still adds over the one above: it flags a mismatch if either run in a pair lands away
    // from the one valid outcome, a second, independently-timed sample of the same race. The
    // engine's seeded-determinism evidence is GameTest.kt's
    // "seeded Game reproduces the same session with the real session helper", which pins exact
    // seed-99 values end to end without racing anything.
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
