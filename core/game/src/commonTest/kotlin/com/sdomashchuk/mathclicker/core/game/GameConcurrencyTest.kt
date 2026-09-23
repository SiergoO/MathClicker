package com.sdomashchuk.mathclicker.core.game

import com.sdomashchuk.mathclicker.core.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.OperationSign
import com.sdomashchuk.mathclicker.core.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    override fun getTargetValueByLevel(level: Int) = 1_000_000

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
            game.targetsFlow.value
                .first()
                .id
        game.targetRevealed(targetId)
        val startingLevel = game.fieldFlow.value.level

        scope.launch { game.targetDidBreakout(targetId) }
        scope.launch { game.fireButtonClicked() }

        // fieldFlow and targetsFlow are two separate StateFlows, so even a correct run has a real,
        // benign instant between activeTargetsAbsent's two writes where a reader outside the lock
        // sees the bumped level against the not-yet-replaced target list. Polling on the level alone
        // would sample inside that window and misreport it as corruption. Wait for both flows to
        // agree on the fully-settled shape instead; a genuinely corrupted run (stuck, or advanced
        // more than once) never reaches this and is read as whatever it settled on at the timeout.
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (
                game.fieldFlow.value.level != startingLevel + 1 ||
                game.targetsFlow.value.size != 1 ||
                !game.targetsFlow.value.all { it.isActive }
            ) {
                yield()
            }
        }
        return game.fieldFlow.value to game.targetsFlow.value
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
}
