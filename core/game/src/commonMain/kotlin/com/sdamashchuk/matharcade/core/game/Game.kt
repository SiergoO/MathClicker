package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.model.GameEvent
import com.sdamashchuk.matharcade.core.game.model.GameState
import com.sdamashchuk.matharcade.core.game.objectmapper.advanceClock
import com.sdamashchuk.matharcade.core.game.objectmapper.applyCombo
import com.sdamashchuk.matharcade.core.game.objectmapper.changeActiveness
import com.sdamashchuk.matharcade.core.game.objectmapper.closeIfNecessary
import com.sdamashchuk.matharcade.core.game.objectmapper.decrementLifeCount
import com.sdamashchuk.matharcade.core.game.objectmapper.decrementValue
import com.sdamashchuk.matharcade.core.game.objectmapper.ensureAlive
import com.sdamashchuk.matharcade.core.game.objectmapper.grantLife
import com.sdamashchuk.matharcade.core.game.objectmapper.resetStreak
import com.sdamashchuk.matharcade.core.game.objectmapper.shortenAppearanceDelay
import com.sdamashchuk.matharcade.core.game.objectmapper.updateActionButtons
import com.sdamashchuk.matharcade.core.game.objectmapper.updateLevel
import com.sdamashchuk.matharcade.core.game.objectmapper.updateScore
import com.sdamashchuk.matharcade.core.game.scoring.performOperation
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Clock

// A resumed app or a dropped composition is clamped to one step rather than replayed in full,
// so a stalled clock cannot teleport every target to the floor the moment it resumes.
internal const val MAX_TICK_MS = 250

// Termination bound: a board no candidate digit can touch must still return in finite time.
private const val MAX_OPERATION_DRAW_ATTEMPTS = 20

// Ten of the nineteen are the engine's public contract, so the threshold is unreachable without
// collapsing it. Suppressed here rather than in a baseline so anything new still fails.
@Suppress("TooManyFunctions")
class Game(
    private val sessionHelper: SessionHelper,
    private val scope: CoroutineScope,
    private val random: Random = Random.Default,
    // Injected, not read from Clock.System: two engines seeded alike must reach identical state,
    // finishedAt included.
    private val clock: Clock = Clock.System,
) {
    private val _stateFlow: MutableStateFlow<GameState> = MutableStateFlow(GameState(Field(), listOf()))
    val stateFlow: StateFlow<GameState> = _stateFlow.asStateFlow()

    // tryEmit, never emit: emit suspends, and tick runs inside the mutex on the frame path.
    // DROP_OLDEST for the same reason - a full buffer must never stall tick.
    private val _events =
        MutableSharedFlow<GameEvent>(
            replay = 0,
            extraBufferCapacity = 32,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    // Every read-then-write of _stateFlow happens under this lock, and each locked function assigns
    // _stateFlow.value exactly once. Not reentrant: locked functions call the *Locked helpers, never each other.
    private val mutex = Mutex()

    private var collectorJob: Job? = null

    // Set by createField, consumed by that session's first fireButtonClicked - which is what makes
    // recreateField's unvalidated next draw safe to promote.
    private var pendingOpeningPromotionCheck = false

    fun start() {
        collectorJob?.cancel()
        collectorJob =
            scope.launch {
                stateFlow
                    .map { it.targets }
                    .distinctUntilChanged()
                    .collect { targets ->
                        // Do not restore the visibleTargetsAbsent branch that used to sit here: no target is visible at the
                        // start of every level, so it replayed a Random draw every frame instead of once per event.
                        if (targets.isNotEmpty() && targets.none { it.isActive }) {
                            activeTargetsAbsent()
                        }
                    }
            }
    }

    fun stop() {
        collectorJob?.cancel()
        collectorJob = null
    }

    suspend fun createField(id: Int) =
        mutex.withLock {
            _stateFlow.value = _stateFlow.value.copy(field = recreateField(id))
            pendingOpeningPromotionCheck = true
        }

    suspend fun createTargets() = mutex.withLock { createTargetsLocked() }

    suspend fun fieldRestored(field: Field) =
        mutex.withLock {
            _stateFlow.value = _stateFlow.value.copy(field = field)
        }

    suspend fun targetsRestored(targets: List<Target>) =
        mutex.withLock {
            _stateFlow.value = _stateFlow.value.copy(targets = targets)
        }

    suspend fun targetClicked(id: Int) =
        mutex.withLock {
            val current = _stateFlow.value
            val previousValue = current.targets.first { it.id == id }.value
            val updatedTargets =
                current.targets
                    .decrementValue(id, 1)
                    .ensureAlive(id)
            val updatedTarget = updatedTargets.first { it.id == id }
            val awarded = if (updatedTarget.isProfitable) 1 else 0
            val updatedField = if (updatedTarget.isProfitable) current.field.updateScore(1) else current.field
            _stateFlow.value = GameState(updatedField, updatedTargets)
            if (previousValue > 0 && updatedTarget.value == 0) {
                _events.tryEmit(GameEvent.TargetZeroed(id, awarded))
            }
        }

    suspend fun fireButtonClicked() =
        mutex.withLock {
            val current = _stateFlow.value
            val pressOutcome =
                current.targets.performOperation(
                    current.field.currentOperationSign,
                    current.field.currentOperationDigit,
                    sessionHelper.failedGrowthCap(current.field.level),
                    current.field.gameTimeMs,
                )
            val updatedTargets =
                pressOutcome.targets
                    .ensureAlive()
            val (nextOperationSign, nextOperationDigit) =
                getNextSignAndDigit(updatedTargets, current.field.level, current.field.gameTimeMs)
            val comboField =
                current.field
                    .applyCombo(pressOutcome.scored)
                    .let { field ->
                        if (pendingOpeningPromotionCheck) {
                            pendingOpeningPromotionCheck = false
                            ensurePromotedOperationSucceeds(field, updatedTargets)
                        } else {
                            field
                        }
                    }
            // The product is where an unbounded multiplier would overflow Int.
            val gained =
                (pressOutcome.totalScore.toLong() * comboField.appliedMultiplier)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            val updatedField =
                comboField
                    .updateActionButtons(nextOperationSign, nextOperationDigit)
                    .updateScore(gained)
            _stateFlow.value = GameState(updatedField, updatedTargets)
            _events.tryEmit(GameEvent.OperationResolved(gained, comboField.bonusMultiplier))
        }

    suspend fun tick(
        elapsedMs: Int,
        clockScale: Double = 1.0,
    ) = mutex.withLock {
        val current = _stateFlow.value
        if (current.field.isClosed || current.targets.none { it.isActive }) return@withLock
        val step = scaleTickStep(elapsedMs, clockScale, MAX_TICK_MS)
        val hadVisibleActiveTarget = current.targets.any { it.isActive && it.isVisible(current.field.gameTimeMs) }

        val advancedField = current.field.advanceClock(step)
        val brokenOutIds =
            current.targets.filter { it.isActive && it.hasBrokenOut(advancedField.gameTimeMs) }.map { it.id }
        val (fieldAfterBreakout, targetsAfterBreakout) =
            resolveBreakouts(current.targets, brokenOutIds, advancedField)

        // On the edge, never on the level: otherwise the draw count would follow frame rate, not game events.
        val stillWaiting =
            hadVisibleActiveTarget &&
                targetsAfterBreakout.none { it.isActive && it.isVisible(advancedField.gameTimeMs) }
        val targetsAfterShorten =
            if (stillWaiting) {
                targetsAfterBreakout.shortenAppearanceDelay(advancedField.gameTimeMs)
            } else {
                targetsAfterBreakout
            }

        // A closed field must not level up or get a fresh target set.
        val leveledUp = targetsAfterShorten.none { it.isActive } && !fieldAfterBreakout.isClosed
        _stateFlow.value =
            if (leveledUp) {
                val leveledField = fieldAfterBreakout.updateLevel()
                GameState(leveledField, recreateTargets(leveledField))
            } else {
                GameState(fieldAfterBreakout, targetsAfterShorten)
            }

        brokenOutIds.forEachIndexed { index, id ->
            _events.tryEmit(GameEvent.TargetBrokeOut(id, current.field.lifeCount - (index + 1)))
        }
        if (leveledUp) {
            _events.tryEmit(GameEvent.LevelUp(_stateFlow.value.field.level))
        }
        if (_stateFlow.value.field.isClosed) {
            _events.tryEmit(GameEvent.GameOver)
        }
    }

    // No caller yet, kept as the mechanism a future random event calls. A grant that would exceed the
    // cap must not emit LifeGranted, or a full-lives player is told they gained one.
    suspend fun grantLife() =
        mutex.withLock {
            val current = _stateFlow.value.field
            val granted = current.grantLife()
            if (granted.lifeCount > current.lifeCount) {
                _stateFlow.value = _stateFlow.value.copy(field = granted)
                _events.tryEmit(GameEvent.LifeGranted(granted.lifeCount))
            }
        }

    private suspend fun activeTargetsAbsent() =
        mutex.withLock {
            // Re-checked under the lock: a concurrent restart can repopulate active targets between the
            // collector's own check and this coroutine acquiring the mutex.
            val current = _stateFlow.value
            // A closed field must not level up: tick can close it on a step that leaves other targets active.
            if (current.targets.none { it.isActive } && !current.field.isClosed) {
                val updatedField = current.field.updateLevel()
                _stateFlow.value = GameState(updatedField, recreateTargets(updatedField))
                _events.tryEmit(GameEvent.LevelUp(updatedField.level))
            }
        }

    // Called only from inside withLock, and never acquires the lock itself - the Mutex is not reentrant.
    private fun createTargetsLocked() {
        val field = _stateFlow.value.field
        val targets = recreateTargets(field)
        _stateFlow.value =
            _stateFlow.value.copy(field = ensureOpeningOperationSucceeds(field, targets), targets = targets)
    }

    // Validated against the moment the targets first become visible, not against now: nothing is visible
    // at creation, so filtering on gameTimeMs would find an empty board and validate nothing.
    private fun ensureOpeningOperationSucceeds(
        field: Field,
        targets: List<Target>,
    ): Field {
        if (targets.isEmpty()) return field
        val firstVisibleAtMs = targets.minOf { it.appearsAtMs }
        val openingWave = targets.filter { it.isActive && it.isVisible(firstVisibleAtMs) }
        val alreadySucceeds =
            openingWave.any { it.succeedsAgainst(field.currentOperationSign, field.currentOperationDigit) }
        return if (openingWave.isEmpty() || alreadySucceeds) {
            field
        } else {
            val (sign, digit) = drawSignAndDigit(openingWave, field.level)
            field.copy(currentOperationSign = sign, currentOperationDigit = digit)
        }
    }

    private fun resolveBreakouts(
        targets: List<Target>,
        brokenOutIds: List<Int>,
        field: Field,
    ): Pair<Field, List<Target>> {
        val updatedTargets =
            brokenOutIds.fold(targets) { acc, id -> acc.changeActiveness(id, false) }
        val fieldAfterLifeLoss = brokenOutIds.fold(field) { acc, _ -> acc.decrementLifeCount(1) }
        val updatedField =
            (if (brokenOutIds.isNotEmpty()) fieldAfterLifeLoss.resetStreak() else fieldAfterLifeLoss)
                .closeIfNecessary(clock.now().toEpochMilliseconds())
        return updatedField to updatedTargets
    }

    private fun recreateField(id: Int): Field {
        val currentOperationSign = OperationSign.values().random(random)
        val currentOperationDigit = sessionHelper.getOperationDigitByLevel(currentOperationSign, 1)
        val nextOperationSign = OperationSign.values().random(random)
        val nextOperationDigit = sessionHelper.getOperationDigitByLevel(nextOperationSign, 1)
        return Field(
            id = id,
            currentOperationSign = currentOperationSign,
            currentOperationDigit = currentOperationDigit,
            nextOperationSign = nextOperationSign,
            nextOperationDigit = nextOperationDigit,
        )
    }

    // Takes the field explicitly so activeTargetsAbsent can regenerate against the just-bumped level in
    // the same assignment as the bump.
    private fun recreateTargets(field: Field): List<Target> {
        val amount = sessionHelper.getTargetAmountByLevel(field.level)
        // Assigned once per field, not per target: both are deterministic (no Random draw), so
        // there is nothing to desync by hoisting them out of the loop below - see MC-73's spec for
        // why finishesAtMs is assigned directly rather than derived as appearsAtMs + a rolled
        // lifetime (the bug this task exists for).
        val openingOffsetMs = sessionHelper.getOpeningOffsetMsByLevel(field.level)
        val finishSpacingMs = sessionHelper.getFinishSpacingMsByLevel(field.level)
        return List(amount) { id ->
            // The two remaining sessionHelper draws must stay in this order (value, then flight
            // time): they share one seeded Random, and Kotlin evaluates constructor/call arguments
            // left-to-right by call-site position - reordering them silently desyncs every draw
            // after the first (the same discipline MC-72's version of this comment documented for
            // its own three draws).
            //
            // The armed sign picks which shaping the value gets - getTargetValueByLevel's
            // residue is meaningless against subtraction, which is not modular (see
            // getSubtractionTargetValueByLevel). Both still draw from the same shared Random, just a
            // different number of times per call, so this branch can move the draw count from here on
            // without desyncing the order above.
            val value =
                when (field.currentOperationSign) {
                    OperationSign.DIVISION -> {
                        sessionHelper.getTargetValueByLevel(field.level, field.currentOperationDigit)
                    }

                    OperationSign.SUBTRACTION -> {
                        sessionHelper.getSubtractionTargetValueByLevel(field.level, field.currentOperationDigit)
                    }
                }
            val flightTimeMs = sessionHelper.getTargetFlightTimeMs(field.level)
            val finishesAtMs = field.gameTimeMs + openingOffsetMs + id * finishSpacingMs
            val appearsAtMs = finishesAtMs - flightTimeMs
            Target(
                id = id + 1,
                relatedFieldId = field.id,
                columnId = id.toGameColumnId(),
                value = value,
                appearsAtMs = appearsAtMs,
                finishesAtMs = finishesAtMs,
            )
        }
    }

    // Never offers a sign/digit that would fail against every visible target: an empty or
    // fully-hidden board (no visible active target at all) has no dud to avoid, so the first draw is
    // taken unconditionally rather than looping. Otherwise redraws until one succeeds against at
    // least one visible active target, or MAX_OPERATION_DRAW_ATTEMPTS is spent.
    private fun getNextSignAndDigit(
        targets: List<Target>,
        level: Int,
        gameTimeMs: Long,
    ): Pair<OperationSign, Int> {
        val visibleActiveTargets = targets.filter { it.isActive && it.isVisible(gameTimeMs) }
        return drawSignAndDigit(visibleActiveTargets, level)
    }

    // The promotion-time counterpart to ensureOpeningOperationSucceeds above - called once, by
    // fireButtonClicked, only for the press that consumes pendingOpeningPromotionCheck. Filters
    // updatedTargets the same way getNextSignAndDigit does, against the field's own (unadvanced -
    // fireButtonClicked never ticks the clock) gameTimeMs, so the check sees exactly the board the new
    // next draw was just validated against, not the stale one recreateField drew against.
    private fun ensurePromotedOperationSucceeds(
        field: Field,
        updatedTargets: List<Target>,
    ): Field {
        val visibleActiveTargets = updatedTargets.filter { it.isActive && it.isVisible(field.gameTimeMs) }
        val alreadySucceeds =
            visibleActiveTargets.any { it.succeedsAgainst(field.nextOperationSign, field.nextOperationDigit) }
        return if (visibleActiveTargets.isEmpty() || alreadySucceeds) {
            field
        } else {
            val (sign, digit) = drawSignAndDigit(visibleActiveTargets, field.level)
            field.copy(nextOperationSign = sign, nextOperationDigit = digit)
        }
    }

    // The redraw loop itself, shared with ensureOpeningOperationSucceeds above: the only thing that
    // differs between "the next press" and "the session's opening press" is which targets count as
    // visible, never this arithmetic.
    private fun drawSignAndDigit(
        visibleActiveTargets: List<Target>,
        level: Int,
    ): Pair<OperationSign, Int> {
        val drawnSign = OperationSign.values().random(random)
        val otherSign = OperationSign.values().first { it != drawnSign }
        for (sign in listOf(drawnSign, otherSign)) {
            val digit = digitThatSucceeds(sign, visibleActiveTargets, level)
            if (digit != null) return Pair(sign, digit)
        }

        // Off the level's own digit curve, deliberately: subtracting the smallest visible
        // value zeroes that target exactly, so a board no drawn digit satisfies still never hands
        // over a dead press.
        return Pair(OperationSign.SUBTRACTION, visibleActiveTargets.minOf { it.value })
    }

    /**
     * Null when every draw of this sign's digit curve is a dud; on an empty board the first draw is
     * taken unconditionally, which is what keeps the caller's minOf fallback off an empty list.
     */
    private fun digitThatSucceeds(
        sign: OperationSign,
        visibleActiveTargets: List<Target>,
        level: Int,
    ): Int? {
        repeat(MAX_OPERATION_DRAW_ATTEMPTS) {
            val digit = sessionHelper.getOperationDigitByLevel(sign, level)
            if (visibleActiveTargets.isEmpty() || visibleActiveTargets.any { it.succeedsAgainst(sign, digit) }) {
                return digit
            }
        }
        return null
    }
}

private fun Target.succeedsAgainst(
    sign: OperationSign,
    digit: Int,
): Boolean =
    when (sign) {
        OperationSign.DIVISION -> digit != 0 && value % digit == 0
        OperationSign.SUBTRACTION -> value - digit >= 0
    }
