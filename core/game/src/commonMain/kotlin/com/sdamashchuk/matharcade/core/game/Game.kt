package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.model.GameState
import com.sdamashchuk.matharcade.core.game.objectmapper.advance
import com.sdamashchuk.matharcade.core.game.objectmapper.changeActiveness
import com.sdamashchuk.matharcade.core.game.objectmapper.changeVisibility
import com.sdamashchuk.matharcade.core.game.objectmapper.closeIfNecessary
import com.sdamashchuk.matharcade.core.game.objectmapper.decrementLifeCount
import com.sdamashchuk.matharcade.core.game.objectmapper.decrementValue
import com.sdamashchuk.matharcade.core.game.objectmapper.ensureAlive
import com.sdamashchuk.matharcade.core.game.objectmapper.ensureVisible
import com.sdamashchuk.matharcade.core.game.objectmapper.shortenAppearanceDelay
import com.sdamashchuk.matharcade.core.game.objectmapper.updateActionButtons
import com.sdamashchuk.matharcade.core.game.objectmapper.updateLevel
import com.sdamashchuk.matharcade.core.game.objectmapper.updateScore
import com.sdamashchuk.matharcade.core.game.objectmapper.updateTargetPositioning
import com.sdamashchuk.matharcade.core.game.scoring.performOperation
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

// A frame gap this large (a resumed app, a dropped composition) is treated as a single 250ms step
// rather than replayed in full, so a stalled clock can't teleport every target straight to the
// floor the moment it resumes. Not yet read by any caller - the UI still drives the fall - wired
// up in the commit that replaces that driver with tick().
internal const val MAX_TICK_MS = 250

class Game(
    private val sessionHelper: SessionHelper,
    private val scope: CoroutineScope,
    private val random: Random = Random.Default,
) {
    private val _stateFlow: MutableStateFlow<GameState> = MutableStateFlow(GameState(Field(), listOf()))
    val stateFlow: StateFlow<GameState> = _stateFlow.asStateFlow()

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
                // non-scoring targetClicked, ...) that this collector's guards below have no reason
                // to re-evaluate. Without this filter a field-only change would replay the same
                // (unchanged) target list through visibleTargetsAbsent()/activeTargetsAbsent(), which
                // is at best a wasted lock acquisition and at worst a second shortenAppearanceDelay
                // pass over targets that already appeared.
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
                        if (targets.isNotEmpty() && targets.none { it.isVisible }) {
                            visibleTargetsAbsent()
                        }
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
            val updatedTargets =
                current.targets
                    .decrementValue(id, 1)
                    .ensureAlive(id)
                    .ensureVisible(id)
            val updatedField =
                if (updatedTargets.first { it.id == id }.isProfitable) {
                    current.field.updateScore(1)
                } else {
                    current.field
                }
            _stateFlow.value = GameState(updatedField, updatedTargets)
        }

    suspend fun targetRevealed(id: Int) =
        mutex.withLock {
            _stateFlow.value = _stateFlow.value.copy(targets = _stateFlow.value.targets.changeVisibility(id, true))
        }

    suspend fun targetDidBreakout(id: Int) =
        mutex.withLock {
            val current = _stateFlow.value
            // A target can only ever cost one life: if it is already inactive (a previous breakout,
            // or the UI re-firing for the same fall) this is a no-op rather than a second decrement.
            val target = current.targets.firstOrNull { it.id == id } ?: return@withLock
            if (!target.isActive) return@withLock
            val updatedTargets =
                current.targets
                    .changeActiveness(id, false)
                    .changeVisibility(id, false)
            val updatedField =
                current.field
                    .decrementLifeCount(1)
                    .closeIfNecessary()
            _stateFlow.value = GameState(updatedField, updatedTargets)
        }

    suspend fun targetShouldBeSaved(
        id: Int,
        position: Int,
        gameColumnHeightPx: Int,
    ) = mutex.withLock {
        _stateFlow.value =
            _stateFlow.value.copy(
                targets = _stateFlow.value.targets.updateTargetPositioning(id, position, gameColumnHeightPx),
            )
    }

    suspend fun fireButtonClicked() =
        mutex.withLock {
            val current = _stateFlow.value
            val (nextOperationSign, nextOperationDigit) = getNextSignAndDigit()
            val (afterOperationButtons, resultingScore) =
                current.targets.performOperation(
                    current.field.currentOperationSign,
                    current.field.currentOperationDigit,
                )
            val updatedTargets =
                afterOperationButtons
                    .ensureAlive()
                    .ensureVisible()
            val updatedField =
                current.field
                    .updateActionButtons(nextOperationSign, nextOperationDigit)
                    .updateScore(resultingScore)
            _stateFlow.value = GameState(updatedField, updatedTargets)
        }

    // The engine's own clock: advances every active target by elapsedMs and resolves whatever that
    // step causes - reveal, breakout, level-up - in this one locked step, so a caller never needs to
    // follow it up with targetRevealed/targetDidBreakout the way the UI-driven path still does.
    suspend fun tick(elapsedMs: Int) =
        mutex.withLock {
            val current = _stateFlow.value
            if (current.field.isClosed || current.targets.none { it.isActive }) return@withLock
            val step = elapsedMs.coerceIn(0, MAX_TICK_MS)
            val hadVisibleActiveTarget = current.targets.any { it.isActive && it.isVisible }

            val advanced = current.targets.advance(step)
            val brokenOutIds = advanced.filter { it.isActive && it.fallenMs >= it.lifetimeMs }.map { it.id }
            val (fieldAfterBreakout, targetsAfterBreakout) = resolveBreakouts(advanced, brokenOutIds, current.field)

            // Fired on the edge - a visible active target existed before this step and does not
            // after - never on the level, or the draw count this makes would depend on frame rate
            // instead of game events (see shortenAppearanceDelay's own determinism guarantee).
            val stillWaiting = hadVisibleActiveTarget && targetsAfterBreakout.none { it.isActive && it.isVisible }
            val targetsAfterShorten =
                if (stillWaiting) targetsAfterBreakout.shortenAppearanceDelay(random) else targetsAfterBreakout

            _stateFlow.value =
                if (targetsAfterShorten.none { it.isActive }) {
                    val leveledField = fieldAfterBreakout.updateLevel()
                    GameState(leveledField, recreateTargets(leveledField))
                } else {
                    GameState(fieldAfterBreakout, targetsAfterShorten)
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
            if (current.targets.none { it.isActive }) {
                // The level bump and the regenerated targets are published in the one assignment
                // below, not two: a reader between separate writes here is exactly the torn state
                // MC-42 exists to close (level+1 against the still-inactive target set).
                val updatedField = current.field.updateLevel()
                _stateFlow.value = GameState(updatedField, recreateTargets(updatedField))
            }
        }

    private suspend fun visibleTargetsAbsent() =
        mutex.withLock {
            _stateFlow.value = _stateFlow.value.copy(targets = _stateFlow.value.targets.shortenAppearanceDelay(random))
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
    // fallenMs >= lifetimeMs is the one that deactivates it, leaving nothing for a repeat to find.
    private fun resolveBreakouts(
        targets: List<Target>,
        brokenOutIds: List<Int>,
        field: Field,
    ): Pair<Field, List<Target>> {
        val updatedTargets =
            brokenOutIds.fold(targets) { acc, id -> acc.changeActiveness(id, false).changeVisibility(id, false) }
        val updatedField =
            brokenOutIds.fold(field) { acc, _ -> acc.decrementLifeCount(1) }.closeIfNecessary()
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
        return List(amount) { id ->
            Target(
                id = id + 1,
                relatedFieldId = field.id,
                columnId = id.toGameColumnId(),
                value = sessionHelper.getTargetValueByLevel(field.level),
                fallenMs = 0,
                appearanceDelayMs = sessionHelper.getTargetAppearanceDelayMsById(id),
                lifetimeMs = sessionHelper.getTargetLifetimeMsByLevel(field.level),
            )
        }
    }

    private fun getNextSignAndDigit(): Pair<OperationSign, Int> {
        val nextOperationSign = OperationSign.values().random(random)
        val nextOperationDigit =
            sessionHelper.getOperationDigitByLevel(nextOperationSign, _stateFlow.value.field.level)
        return Pair(nextOperationSign, nextOperationDigit)
    }
}
