package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelper
import com.sdamashchuk.mathbubbles.core.game.model.BoosterDropContext
import com.sdamashchuk.mathbubbles.core.game.model.BoosterDropResult
import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.game.model.GameState
import com.sdamashchuk.mathbubbles.core.game.model.TimedBoosterEffect
import com.sdamashchuk.mathbubbles.core.game.objectmapper.advanceClock
import com.sdamashchuk.mathbubbles.core.game.objectmapper.applyCombo
import com.sdamashchuk.mathbubbles.core.game.objectmapper.changeActiveness
import com.sdamashchuk.mathbubbles.core.game.objectmapper.closeIfNecessary
import com.sdamashchuk.mathbubbles.core.game.objectmapper.decrementLifeCount
import com.sdamashchuk.mathbubbles.core.game.objectmapper.decrementValue
import com.sdamashchuk.mathbubbles.core.game.objectmapper.ensureAlive
import com.sdamashchuk.mathbubbles.core.game.objectmapper.freeStashSlot
import com.sdamashchuk.mathbubbles.core.game.objectmapper.grantLife
import com.sdamashchuk.mathbubbles.core.game.objectmapper.resetStreak
import com.sdamashchuk.mathbubbles.core.game.objectmapper.rewind
import com.sdamashchuk.mathbubbles.core.game.objectmapper.shortenAppearanceDelay
import com.sdamashchuk.mathbubbles.core.game.objectmapper.stashCurrentBooster
import com.sdamashchuk.mathbubbles.core.game.objectmapper.updateActionButtons
import com.sdamashchuk.mathbubbles.core.game.objectmapper.updateLevel
import com.sdamashchuk.mathbubbles.core.game.objectmapper.updateScore
import com.sdamashchuk.mathbubbles.core.game.scoring.performOperation
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
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

private const val BOOSTER_STASH_CAPACITY = 3

private const val FREEZE_DURATION_MS = 3_000
private const val REWIND_DURATION_MS = 2_000

// 13 of the 33 are Game's own public contract; splitting the file would only scatter private
// helpers, not shrink it. Suppressed here, not in a baseline, so further growth still fails.
@Suppress("TooManyFunctions")
class Game(
    private val sessionHelper: SessionHelper,
    private val scope: CoroutineScope,
    private val random: Random = Random.Default,
    // Injected, not read from Clock.System: two engines seeded alike must reach identical state,
    // finishedAt included.
    private val clock: Clock = Clock.System,
    private val boostersEnabled: Boolean = false,
) {
    private val _stateFlow: MutableStateFlow<GameState> = MutableStateFlow(GameState(Field(), listOf()))
    val stateFlow: StateFlow<GameState> = _stateFlow.asStateFlow()

    private val boosterDropRule = BoosterDropRule(random)

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

    // TODO(MC-114): persist with the field; until then a restored session restarts the drop curve.
    private var boosterDropCounter = 0

    private var hasDroppedBoosterThisSession = false

    private var activeTimedEffect: TimedBoosterEffect? = null
    private var icePickArmed = false
    private var shieldActive = false

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
            boosterDropCounter = 0
            hasDroppedBoosterThisSession = false
            activeTimedEffect = null
            icePickArmed = false
            shieldActive = false
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
            if (icePickArmed) {
                val target = current.targets.firstOrNull { it.id == id }
                if (target == null || !target.isActive || !target.isVisible(current.field.gameTimeMs)) {
                    return@withLock
                }
                icePickArmed = false
                val awarded = if (target.isProfitable) target.value else 0
                val updatedTargets =
                    current.targets
                        .decrementValue(id, target.value)
                        .ensureAlive(id)
                val updatedField = current.field.updateScore(awarded)
                _stateFlow.value = GameState(updatedField, updatedTargets)
                _events.tryEmit(GameEvent.TargetZeroed(id, awarded))
                return@withLock
            }
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
            when (val action = current.field.currentAction) {
                is FieldAction.Operation -> fireOperationLocked(current, action)
                is FieldAction.BoosterAction -> fireBoosterLocked(current)
            }
        }

    private fun fireOperationLocked(
        current: GameState,
        action: FieldAction.Operation,
    ) {
        val pressOutcome =
            current.targets.performOperation(
                action.sign,
                action.digit,
                sessionHelper.failedGrowthCap(current.field.level),
                current.field.gameTimeMs,
            )
        val updatedTargets =
            pressOutcome.targets
                .ensureAlive()
        val drop = rollBoosterDrop(current.field, updatedTargets)
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
        boosterDropCounter = drop.counter
        val updatedField =
            comboField
                .updateActionButtons(nextOperationSign, nextOperationDigit, drop.booster)
                .updateScore(gained)
        _stateFlow.value = GameState(updatedField, updatedTargets)
        _events.tryEmit(GameEvent.OperationResolved(gained, comboField.bonusMultiplier))
    }

    private fun fireBoosterLocked(current: GameState) {
        current.field.currentBooster?.let(::applyBoosterEffect)
        val drop = rollBoosterDrop(current.field, current.targets)
        val (nextOperationSign, nextOperationDigit) =
            getNextSignAndDigit(current.targets, current.field.level, current.field.gameTimeMs)
        boosterDropCounter = drop.counter
        val updatedField = current.field.updateActionButtons(nextOperationSign, nextOperationDigit, drop.booster)
        _stateFlow.value = GameState(updatedField, current.targets)
    }

    // A swipe on an operation or into a full stash is a UI slip, so it is ignored rather than reported.
    suspend fun stashBooster() =
        mutex.withLock {
            val current = _stateFlow.value
            val booster = current.field.currentBooster ?: return@withLock
            if (current.field.boosterStash.size >= BOOSTER_STASH_CAPACITY) return@withLock
            val stashedField = current.field.stashCurrentBooster(booster)
            val drop = rollBoosterDrop(stashedField, current.targets)
            val (nextOperationSign, nextOperationDigit) =
                getNextSignAndDigit(current.targets, stashedField.level, stashedField.gameTimeMs)
            boosterDropCounter = drop.counter
            val promotedField = stashedField.updateActionButtons(nextOperationSign, nextOperationDigit, drop.booster)
            _stateFlow.value = GameState(promotedField, current.targets)
        }

    suspend fun applyBoosterFromStash(slotIndex: Int) =
        mutex.withLock {
            val current = _stateFlow.value
            val booster = current.field.boosterStash.getOrNull(slotIndex) ?: return@withLock
            applyBoosterEffect(booster)
            _stateFlow.value = current.copy(field = current.field.freeStashSlot(slotIndex))
        }

    suspend fun disarmIcePick() =
        mutex.withLock {
            if (!icePickArmed) return@withLock
            val current = _stateFlow.value
            if (current.field.boosterStash.size >= BOOSTER_STASH_CAPACITY) return@withLock
            icePickArmed = false
            _stateFlow.value = current.copy(field = current.field.stashCurrentBooster(Booster.ICE_PICK))
        }

    private fun applyBoosterEffect(booster: Booster) {
        when (booster) {
            Booster.FREEZE -> activeTimedEffect = TimedBoosterEffect(Booster.FREEZE, FREEZE_DURATION_MS)
            Booster.REWIND -> activeTimedEffect = TimedBoosterEffect(Booster.REWIND, REWIND_DURATION_MS)
            Booster.ICE_PICK -> icePickArmed = true
            Booster.SHIELD -> shieldActive = true
        }
    }

    suspend fun tick(
        elapsedMs: Int,
        clockScale: Double = 1.0,
    ) = mutex.withLock {
        val current = _stateFlow.value
        if (current.field.isClosed || current.targets.none { it.isActive }) return@withLock
        val hadVisibleActiveTarget = current.targets.any { it.isActive && it.isVisible(current.field.gameTimeMs) }
        // Captured before advancing: the effect can expire inside advanceEffectField below, and
        // shortenAppearanceDelay must stay off for the tick that ran under it either way.
        val effectActiveBeforeTick = activeTimedEffect != null

        val advancedField = advanceEffectField(current, elapsedMs, clockScale)
        val brokenOutIds =
            current.targets.filter { it.isActive && it.hasBrokenOut(advancedField.gameTimeMs) }.map { it.id }
        val (fieldAfterBreakout, targetsAfterBreakout, shieldConsumed) =
            resolveBreakouts(current.targets, brokenOutIds, advancedField)

        val targetsAfterShorten =
            shortenIfStillWaiting(targetsAfterBreakout, advancedField, hadVisibleActiveTarget, effectActiveBeforeTick)

        val leveledUp = applyTickOutcome(fieldAfterBreakout, targetsAfterShorten)
        emitTickEvents(current.field.lifeCount, brokenOutIds, shieldConsumed, leveledUp)
    }

    private fun advanceEffectField(
        current: GameState,
        elapsedMs: Int,
        clockScale: Double,
    ): Field =
        when (activeTimedEffect?.booster) {
            Booster.FREEZE -> advanceFrozenField(current, elapsedMs)
            Booster.REWIND -> advanceRewindingField(current, elapsedMs)
            else -> current.field.advanceClock(scaleTickStep(elapsedMs, clockScale, MAX_TICK_MS))
        }

    // On the edge, never on the level: otherwise the draw count would follow frame rate, not game events.
    private fun shortenIfStillWaiting(
        targets: List<Target>,
        advancedField: Field,
        hadVisibleActiveTarget: Boolean,
        effectActiveBeforeTick: Boolean,
    ): List<Target> {
        val stillWaiting =
            hadVisibleActiveTarget &&
                !effectActiveBeforeTick &&
                targets.none { it.isActive && it.isVisible(advancedField.gameTimeMs) }
        return if (stillWaiting) targets.shortenAppearanceDelay(advancedField.gameTimeMs) else targets
    }

    // A closed field must not level up or get a fresh target set.
    private fun applyTickOutcome(
        field: Field,
        targets: List<Target>,
    ): Boolean {
        val leveledUp = targets.none { it.isActive } && !field.isClosed
        _stateFlow.value =
            if (leveledUp) {
                val leveledField = field.updateLevel()
                GameState(leveledField, recreateTargets(leveledField))
            } else {
                GameState(field, targets)
            }
        return leveledUp
    }

    private fun emitTickEvents(
        startingLives: Int,
        brokenOutIds: List<Int>,
        shieldConsumed: Boolean,
        leveledUp: Boolean,
    ) {
        var livesLeft = startingLives
        brokenOutIds.forEachIndexed { index, id ->
            if (!(shieldConsumed && index == 0)) livesLeft -= 1
            _events.tryEmit(GameEvent.TargetBrokeOut(id, livesLeft))
        }
        if (leveledUp) {
            _events.tryEmit(GameEvent.LevelUp(_stateFlow.value.field.level))
        }
        if (_stateFlow.value.field.isClosed) {
            _events.tryEmit(GameEvent.GameOver)
        }
    }

    private fun advanceFrozenField(
        current: GameState,
        elapsedMs: Int,
    ): Field {
        val effect = activeTimedEffect ?: return current.field
        val step = scaleTickStep(elapsedMs, 1.0, MAX_TICK_MS)
        val remaining = effect.remainingRealMs - step
        activeTimedEffect = if (remaining > 0) effect.copy(remainingRealMs = remaining) else null
        return current.field.advanceClock(0)
    }

    // Never wound past the newest visible bubble's appearsAtMs, or it would vanish off the top;
    // reaching that clamp ends the effect early.
    private fun advanceRewindingField(
        current: GameState,
        elapsedMs: Int,
    ): Field {
        val effect = activeTimedEffect ?: return current.field
        val step = scaleTickStep(elapsedMs, 1.0, MAX_TICK_MS)
        val topBoundaryMs =
            current.targets
                .filter { it.isActive && it.isVisible(current.field.gameTimeMs) }
                .maxOfOrNull { it.appearsAtMs }
        val maxRewindMs = topBoundaryMs?.let { (current.field.gameTimeMs - it).coerceAtLeast(0L) } ?: Long.MAX_VALUE
        val actualStepMs = step.toLong().coerceAtMost(maxRewindMs)
        val hitTopBoundary = actualStepMs < step
        val remaining = effect.remainingRealMs - step
        activeTimedEffect = if (hitTopBoundary || remaining <= 0) null else effect.copy(remainingRealMs = remaining)
        return current.field.rewind(actualStepMs)
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

    // The Boolean says a shield absorbed one breakout, which the per-id livesLeft events must reflect.
    private fun resolveBreakouts(
        targets: List<Target>,
        brokenOutIds: List<Int>,
        field: Field,
    ): Triple<Field, List<Target>, Boolean> {
        val updatedTargets =
            brokenOutIds.fold(targets) { acc, id -> acc.changeActiveness(id, false) }
        val shieldConsumed = shieldActive && brokenOutIds.isNotEmpty()
        if (shieldConsumed) shieldActive = false
        val livesLost = brokenOutIds.size - (if (shieldConsumed) 1 else 0)
        val fieldAfterLifeLoss = field.decrementLifeCount(livesLost)
        val updatedField =
            (if (brokenOutIds.isNotEmpty()) fieldAfterLifeLoss.resetStreak() else fieldAfterLifeLoss)
                .closeIfNecessary(clock.now().toEpochMilliseconds())
        return Triple(updatedField, updatedTargets, shieldConsumed)
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

    private fun rollBoosterDrop(
        field: Field,
        targets: List<Target>,
    ): BoosterDropResult {
        if (!boostersEnabled) return BoosterDropResult(booster = null, counter = boosterDropCounter)
        val visibleActiveTargets = targets.filter { it.isActive && it.isVisible(field.gameTimeMs) }
        val context =
            BoosterDropContext(
                anyVisibleBeyondTelegraph = visibleActiveTargets.any { it.isTelegraphingBreakout(field.gameTimeMs) },
                noVisibleBubble = visibleActiveTargets.isEmpty(),
                oneLifeLeft = field.lifeCount == 1,
                icePickArmed = icePickArmed,
                shieldActive = shieldActive,
            )
        val result =
            boosterDropRule.roll(
                counter = boosterDropCounter,
                hasDroppedBefore = hasDroppedBoosterThisSession,
                stashFull = field.boosterStash.size >= BOOSTER_STASH_CAPACITY,
                context = context,
            )
        if (result.booster != null) hasDroppedBoosterThisSession = true
        return result
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
