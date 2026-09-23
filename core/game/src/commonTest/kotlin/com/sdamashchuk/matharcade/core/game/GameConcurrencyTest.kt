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
private class RaceSessionHelper : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(level: Int) = UNKILLABLE_TARGET_VALUE

    override fun getTargetLifetimeMsByLevel(level: Int) = 1000

    override fun getTargetAppearanceDelayMsById(id: Int) = 0

    override fun getTargetAmountByLevel(level: Int) = 1

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = 1

    override fun getDivisionDigitByLevel(level: Int) = 1

    override fun getSubtractionDigitByLevel(level: Int) = 1
}

// Corrupted trials never reach the settled state polled for below, so they always burn the full
// timeout. Kept short so a red run stays fast: correct trials settle in low single-digit
// milliseconds (hundreds of times under this), so there is no realistic risk of a green trial
// timing out here.
private const val SETTLE_TIMEOUT_MS = 300L

private const val UNKILLABLE_TARGET_VALUE = 1_000_000

// The measured tear rate is under 1% per trial, so 400 trials leave roughly a 2% chance of a
// genuinely torn build going green. 2000 puts that below 1e-8 and costs a fraction of a second.
private const val TEAR_TRIALS = 2000

// Runs the single-target-breakout-vs-fire race on a real Dispatchers.Default scope - the collector
// launched by start() and the two racing calls genuinely run on different threads, unlike GameTest's
// virtual-time runTest. Settles by polling rather than a fixed delay: the correct end state is
// always "one fresh, fully active target", however long the collector takes to get there.
private suspend fun raceLevelUpAgainstFire(seed: Long): Pair<Field, List<Target>> {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        val game = Game(RaceSessionHelper(), scope, Random(seed))
        game.start()
        game.createField(1)
        game.createTargets()
        val targetId =
            game.stateFlow.value.targets
                .first()
                .id
        game.targetRevealed(targetId)
        val startingLevel = game.stateFlow.value.field.level

        scope.launch { game.targetDidBreakout(targetId) }
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

// Isolates the reader/writer tear MC-42 exists for from the writer/writer race above: a single call
// to targetDidBreakout that costs the last life writes both the deactivated target and the closed
// field under one mutex.withLock. A concurrent reader busy-polling stateFlow.value from a second real
// thread - no suspension point in the loop, so nothing yields the window away - must only ever see
// the target's active flag and the field's closed flag agree; either both still false (untouched) or
// both true (fully applied), never one ahead of the other. No start(): the level-up this would
// otherwise trigger is a different transition, already covered above, and would recreate this exact
// target id and confuse the invariant being checked here.
private suspend fun readerSeesTornStateOnLastLifeBreakout(seed: Long): Boolean {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        val game = Game(RaceSessionHelper(), scope, Random(seed))
        game.createField(1)
        game.createTargets()
        val targetId =
            game.stateFlow.value.targets
                .first()
                .id
        game.targetRevealed(targetId)
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

        game.targetDidBreakout(targetId)
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (!game.stateFlow.value.field.isClosed) yield()
        }
        reader.cancelAndJoin()
        return torn
    } finally {
        scope.cancel()
    }
}

// The level-up half of the same tear. activeTargetsAbsent() publishes the bumped field and the
// regenerated target set; before MC-42 it wrote them as two assignments, so a reader could catch
// level 2 against the cleared level-1 board - the "new level against the previous target set" the
// task exists to close. Needs start(), because the collector is what drives the level-up.
// Invariant: once the level has advanced, a target set must exist to play, so a level above 1 with
// nothing active is a state that never holds inside the lock. RaceSessionHelper's targets are
// unkillable and never broken out again, so level 2 with an all-inactive board cannot arise
// legitimately after the single level-up this drives.
private suspend fun readerSeesTornStateOnLevelUp(seed: Long): Boolean {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    try {
        val game = Game(RaceSessionHelper(), scope, Random(seed))
        game.start()
        game.createField(1)
        game.createTargets()
        val targetId =
            game.stateFlow.value.targets
                .first()
                .id

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

        game.targetDidBreakout(targetId)
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
        game.targetRevealed(targetId)
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
    // Reproduces MC-30 finding 1 directly: the last active target breaking out - a level-up driven
    // by Game's own collector coroutine - racing a single fire press from a second coroutine, both
    // on Dispatchers.Default. Correct behaviour has exactly one outcome regardless of which side
    // wins the race: the level advances by exactly one and the target set is the fresh one-target
    // set for the new level, never a torn mix of the two, never applied twice.
    @Test
    fun `a level-up racing a fire press never corrupts the level or loses the target set`() =
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
            assertEquals(0, corrupted, "level-up vs fire race corrupted $corrupted/$ran trials")
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
