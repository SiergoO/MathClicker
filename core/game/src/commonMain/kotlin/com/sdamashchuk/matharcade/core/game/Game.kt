package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.model.GameEvent
import com.sdamashchuk.matharcade.core.game.model.GameState
import com.sdamashchuk.matharcade.core.game.objectmapper.advanceClock
import com.sdamashchuk.matharcade.core.game.objectmapper.advanceStreak
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

// A frame gap this large (a resumed app, a dropped composition) is treated as a single 250ms step
// rather than replayed in full, so a stalled clock can't teleport every target straight to the
// floor the moment it resumes. Read from Field's withFrameNanos loop, the sole driver of tick().
internal const val MAX_TICK_MS = 250

// getNextSignAndDigit redraws rather than offers a press that cannot succeed against anything
// visible (MC-50). Bounded rather than looped unconditionally: a board every candidate digit range
// genuinely cannot touch (pathological, but not provably impossible) must still return in finite
// time. 20 is generous against what that loop actually costs - each attempt is one Random draw and
// a scan of the visible targets - and the fallback on exhaustion is simply the last draw made, dud
// or not, which is exactly what every press already tolerated before this task.
private const val MAX_OPERATION_DRAW_ATTEMPTS = 20

class Game(
    private val sessionHelper: SessionHelper,
    private val scope: CoroutineScope,
    private val random: Random = Random.Default,
    // Injected the same way random is: two engines seeded alike must reach identical state,
    // finishedAt included, which reading Clock.System directly inside closeIfNecessary could not
    // guarantee across two separate calls a real clock advances between.
    private val clock: Clock = Clock.System,
) {
    private val _stateFlow: MutableStateFlow<GameState> = MutableStateFlow(GameState(Field(), listOf()))
    val stateFlow: StateFlow<GameState> = _stateFlow.asStateFlow()

    // StateFlow conflates, so a UI collector provably drops intermediate events (two targets
    // zeroed in the same frame would look identical to one). tryEmit is used at every call site
    // below, never emit: emit suspends, and tick runs inside the mutex on the frame path where
    // blocking on a slow subscriber is exactly what this buffer exists to avoid. DROP_OLDEST over
    // SUSPEND is the same reasoning from the other side - a full buffer must never stall tick.
    private val _events =
        MutableSharedFlow<GameEvent>(
            replay = 0,
            extraBufferCapacity = 32,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    // Every read-then-write of _stateFlow - including the mutators that only touch one half of it -
    // happens inside this lock, so a caller (Main) and the collector below (Default) can never
    // interleave mid-transaction. Each locked function below assigns _stateFlow.value exactly once,
    // with the untouched half copied through unchanged: that is what makes the published state
    // indivisible rather than just the write to it serialized (MC-42; MC-32 only serialized writes).
    // The lock itself is not reentrant: the locked functions call the private *Locked helpers
    // directly rather than each other, so no coroutine ever tries to acquire it twice.
    private val mutex = Mutex()

    private var collectorJob: Job? = null

    fun start() {
        collectorJob?.cancel()
        collectorJob =
            scope.launch {
                // Filtered to target changes only, same as the pre-MC-42 _targetsFlow.collect: a
                // combined state also emits on field-only changes (gameColumnSizeMeasured, a
                // non-scoring targetClicked, ...) that this collector's guard below has no reason
                // to re-evaluate. Without this filter a field-only change would replay the same
                // (unchanged) target list through activeTargetsAbsent(), which is at best a wasted
                // lock acquisition.
                stateFlow
                    .map { it.targets }
                    .distinctUntilChanged()
                    .collect { targets ->
                        // start() registers this collector asynchronously on the injected scope, so its
                        // first delivery races updateSession()'s own createField()/createTargets() calls:
                        // gating on fieldFlow.value.id != 0 instead of targets.isNotEmpty() let that first,
                        // still-empty delivery see a live field id and level up with no player input.
                        // GameViewModel.updateSession() now recreates a restored empty target list itself
                        // (targetsRestored(emptyList()) is otherwise a no-op MutableStateFlow never emits
                        // a value equal to its current one), so nothing in production ever hands this
                        // collector an empty list to recover from. Keep the cheap guard.
                        //
                        // The visibleTargetsAbsent() branch that used to sit here is gone: tick() now
                        // runs every frame, and while no target is visible - true for the whole board at
                        // the start of every level - distinctUntilChanged above would pass on every
                        // single frame, replaying shortenAppearanceDelay() (and its Random draw) 60-120
                        // times a second instead of once per game event. tick()'s own stillWaiting
                        // computation does this correctly, fired on the edge instead of the level.
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
            // Drawn against updatedTargets, not the pre-press board: this is the freshest state
            // available before the draw is promoted to "current" by updateActionButtons below, so a
            // target this same press just cleared or retired can't be the reason the next offer is a
            // dud (MC-50). The board can still move again before the draw is actually used - tick()
            // runs between now and the next press - but that gap is unavoidable without validating
            // inside performOperation itself, which is deliberately out of scope: a press that turns
            // out to fail must still cost what it costs.
            val (nextOperationSign, nextOperationDigit) =
                getNextSignAndDigit(updatedTargets, current.field.level, current.field.gameTimeMs)
            val streakedField = current.field.advanceStreak(pressOutcome.failed)
            // In Long: totalScore is bounded by the board, but appliedMultiplier is not, so the
            // product is the first place an uncapped streak can overflow.
            val gained =
                (pressOutcome.totalScore.toLong() * streakedField.appliedMultiplier)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            val updatedField =
                streakedField
                    .updateActionButtons(nextOperationSign, nextOperationDigit)
                    .updateScore(gained)
            _stateFlow.value = GameState(updatedField, updatedTargets)
            _events.tryEmit(GameEvent.OperationResolved(gained, streakedField.bonusMultiplier))
        }

    // The engine's own clock: advances gameTimeMs by elapsedMs and resolves whatever that step
    // causes against each target's fixed schedule - reveal, breakout, level-up - in this one locked
    // step. Targets are never rewritten here (MC-72: their schedule is stable, only the clock they
    // are read against moves); this is now the only way a target is revealed or breaks out, there
    // is no equivalent UI-driven call left.
    //
    // clockScale is MC-74's freeze/slow seam: a coefficient on this call's own step, defaulted to
    // 1.0 so every existing caller (there is no consumer of a non-default value yet) reproduces
    // today's arithmetic exactly. It is a parameter, not state Game or Field carries between calls -
    // Field is persisted, and a freeze that survives killing and relaunching the app is a bug, not a
    // feature. A future caller drives it every frame, the same way the UI already drives elapsedMs.
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

        // Fired on the edge - a visible active target existed before this step and does not
        // after - never on the level, or the draw count this makes would depend on frame rate
        // instead of game events (see shortenAppearanceDelay's own determinism guarantee).
        val stillWaiting =
            hadVisibleActiveTarget &&
                targetsAfterBreakout.none { it.isActive && it.isVisible(advancedField.gameTimeMs) }
        val targetsAfterShorten =
            if (stillWaiting) {
                targetsAfterBreakout.shortenAppearanceDelay(advancedField.gameTimeMs)
            } else {
                targetsAfterBreakout
            }

        // fieldAfterBreakout can already be closed here - the breakout that emptied the board
        // is also the one that cost the last life. A closed field must not level up or get a
        // fresh target set for a session that is already over (MC-59), so the board is instead
        // published exactly as resolveBreakouts left it: the just-broken-out targets, inactive,
        // not a recreated set.
        val leveledUp = targetsAfterShorten.none { it.isActive } && !fieldAfterBreakout.isClosed
        _stateFlow.value =
            if (leveledUp) {
                val leveledField = fieldAfterBreakout.updateLevel()
                GameState(leveledField, recreateTargets(leveledField))
            } else {
                GameState(fieldAfterBreakout, targetsAfterShorten)
            }

        // lifeCount is decremented by exactly one per id, in this same order, inside
        // resolveBreakouts - so the i-th id's own post-breakout count is derivable here without
        // resolveBreakouts having to thread it back out as extra return state.
        brokenOutIds.forEachIndexed { index, id ->
            _events.tryEmit(GameEvent.TargetBrokeOut(id, current.field.lifeCount - (index + 1)))
        }
        if (leveledUp) {
            _events.tryEmit(GameEvent.LevelUp(_stateFlow.value.field.level))
        }
        // current.field.isClosed already returned this call early above, so a closed field here
        // is always a fresh transition, not a repeat of one already reported.
        if (_stateFlow.value.field.isClosed) {
            _events.tryEmit(GameEvent.GameOver)
        }
    }

    // No caller today - MC-76 removed the every-N-levels trigger MC-54 wired through updateLevel,
    // but kept the grant itself as the mechanism a future random event calls. Mutex-guarded like
    // every other public entry point above; a call that would exceed the cap is a pure no-op and
    // must not emit LifeGranted, or the feedback layer would tell a full-lives player they gained one.
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
            // Re-check under the lock: the collector's own condition (targets.none { it.isActive })
            // was evaluated outside it, and a concurrent createField()/createTargets() - a restart -
            // can repopulate active targets in the window between that check and this coroutine
            // actually acquiring the mutex. Without this, a stale trigger would level up a board
            // that is no longer empty.
            val current = _stateFlow.value
            // A closed field must not level up here either (MC-59): tick() can close the field on
            // a step that leaves other targets still active (not every breakout empties the board),
            // and one of those can then reach zero through targetClicked - reaching this branch for
            // a session that is already over. Leave the state exactly as it is; there is nothing to
            // recreate for a dead session.
            if (current.targets.none { it.isActive } && !current.field.isClosed) {
                // The level bump and the regenerated targets are published in the one assignment
                // below, not two: a reader between separate writes here is exactly the torn state
                // MC-42 exists to close (level+1 against the still-inactive target set).
                val updatedField = current.field.updateLevel()
                _stateFlow.value = GameState(updatedField, recreateTargets(updatedField))
                _events.tryEmit(GameEvent.LevelUp(updatedField.level))
            }
        }

    // Only ever called from inside a mutex.withLock block above - never acquires the lock itself,
    // so createTargets() can call it without a non-reentrant Mutex deadlocking on its own coroutine.
    // activeTargetsAbsent() no longer routes through here: it has to publish the bumped field and the
    // regenerated targets in one assignment, which this cannot express.
    private fun createTargetsLocked() {
        _stateFlow.value = _stateFlow.value.copy(targets = recreateTargets(_stateFlow.value.field))
    }

    // The body of the old targetDidBreakout, minus its "already inactive" guard: that guard is now
    // structural, since tick only ever offers an id here once - the same locked step that finds
    // hasBrokenOut() true is the one that deactivates it, leaving nothing for a repeat to find.
    // isVisible no longer needs a matching changeVisibility() call: it is derived from the clock
    // (MC-72), and every consumer already gates on isActive first, which this does set to false.
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

    // Takes the field explicitly rather than reading _stateFlow.value.field, so activeTargetsAbsent
    // can regenerate targets against the just-bumped level in the same assignment as the level bump
    // itself, instead of a second read-then-write of _stateFlow.
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
            // MC-70: the armed sign picks which shaping the value gets - getTargetValueByLevel's
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

    // Never offers a sign/digit that would fail against every visible target (MC-50): an empty or
    // fully-hidden board (no visible active target at all) has no dud to avoid, so the first draw is
    // taken unconditionally rather than looping. Otherwise redraws until one succeeds against at
    // least one visible active target, or MAX_OPERATION_DRAW_ATTEMPTS is spent.
    private fun getNextSignAndDigit(
        targets: List<Target>,
        level: Int,
        gameTimeMs: Long,
    ): Pair<OperationSign, Int> {
        val visibleActiveTargets = targets.filter { it.isActive && it.isVisible(gameTimeMs) }
        var sign = OperationSign.values().random(random)
        var digit = sessionHelper.getOperationDigitByLevel(sign, level)
        var attempts = 1
        while (
            visibleActiveTargets.isNotEmpty() &&
            visibleActiveTargets.none { it.succeedsAgainst(sign, digit) } &&
            attempts < MAX_OPERATION_DRAW_ATTEMPTS
        ) {
            sign = OperationSign.values().random(random)
            digit = sessionHelper.getOperationDigitByLevel(sign, level)
            attempts++
        }
        if (visibleActiveTargets.isNotEmpty() && visibleActiveTargets.none { it.succeedsAgainst(sign, digit) }) {
            // Exhausting the bound used to mean handing over the last draw, dud or not. MC-60 made
            // that reachable: shaping values toward a residue of the current digit puts lone small
            // values on the board, and a lone 1 is below every division digit and every subtraction
            // digit past level 9. Subtracting the smallest visible value always succeeds - it zeroes
            // that target exactly, and a target's value is never below 1 - so the dud stops being
            // improbable and becomes impossible while anything is on screen. Off the level's own
            // digit curve, deliberately: this fires in roughly 3 draws in 10 000, and a player with
            // no move at all is worse than one handed a generous one.
            sign = OperationSign.SUBTRACTION
            digit = visibleActiveTargets.minOf { it.value }
        }
        return Pair(sign, digit)
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
