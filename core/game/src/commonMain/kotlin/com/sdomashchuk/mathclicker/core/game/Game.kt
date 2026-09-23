package com.sdomashchuk.mathclicker.core.game

import com.sdomashchuk.mathclicker.core.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.core.game.objectmapper.changeActiveness
import com.sdomashchuk.mathclicker.core.game.objectmapper.changeVisibility
import com.sdomashchuk.mathclicker.core.game.objectmapper.closeIfNecessary
import com.sdomashchuk.mathclicker.core.game.objectmapper.decrementLifeCount
import com.sdomashchuk.mathclicker.core.game.objectmapper.decrementValue
import com.sdomashchuk.mathclicker.core.game.objectmapper.ensureAlive
import com.sdomashchuk.mathclicker.core.game.objectmapper.ensureVisible
import com.sdomashchuk.mathclicker.core.game.objectmapper.performOperation
import com.sdomashchuk.mathclicker.core.game.objectmapper.shortenAppearanceDelay
import com.sdomashchuk.mathclicker.core.game.objectmapper.updateActionButtons
import com.sdomashchuk.mathclicker.core.game.objectmapper.updateGameColumnSize
import com.sdomashchuk.mathclicker.core.game.objectmapper.updateLevel
import com.sdomashchuk.mathclicker.core.game.objectmapper.updateScore
import com.sdomashchuk.mathclicker.core.game.objectmapper.updateTargetPositioning
import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.OperationSign
import com.sdomashchuk.mathclicker.core.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class Game(
    private val sessionHelper: SessionHelper,
    private val scope: CoroutineScope,
    private val random: Random = Random.Default,
) {
    private val _targetsFlow: MutableStateFlow<List<Target>> = MutableStateFlow(listOf())
    val targetsFlow: StateFlow<List<Target>> = _targetsFlow.asStateFlow()

    private val _fieldFlow: MutableStateFlow<Field> = MutableStateFlow(Field())
    val fieldFlow: StateFlow<Field> = _fieldFlow.asStateFlow()

    // Every read-then-write of _targetsFlow/_fieldFlow - including the pairs that read one and
    // write both - happens inside this lock, so a caller (Main) and the collector below (Default)
    // can never interleave mid-transaction. It is not reentrant: the locked functions below call
    // the private *Locked helpers directly rather than each other, so no coroutine ever tries to
    // acquire it twice.
    private val mutex = Mutex()

    private var collectorJob: Job? = null

    fun start() {
        collectorJob?.cancel()
        collectorJob =
            scope.launch {
                _targetsFlow.collect { targets ->
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
            _fieldFlow.value = recreateField(id)
        }

    suspend fun createTargets() = mutex.withLock { createTargetsLocked() }

    suspend fun fieldRestored(field: Field) =
        mutex.withLock {
            _fieldFlow.value = field
        }

    suspend fun targetsRestored(targets: List<Target>) =
        mutex.withLock {
            _targetsFlow.value = targets
        }

    suspend fun targetClicked(id: Int) =
        mutex.withLock {
            val updatedTargets =
                targetsFlow.value
                    .decrementValue(id, 1)
                    .ensureAlive(id)
                    .ensureVisible(id)
            _targetsFlow.value = updatedTargets
            if (updatedTargets.first { it.id == id }.isProfitable) {
                _fieldFlow.value = fieldFlow.value.updateScore(1)
            }
        }

    suspend fun targetRevealed(id: Int) =
        mutex.withLock {
            _targetsFlow.value = targetsFlow.value.changeVisibility(id, true)
        }

    suspend fun targetDidBreakout(id: Int) =
        mutex.withLock {
            // A target can only ever cost one life: if it is already inactive (a previous breakout,
            // or the UI re-firing for the same fall) this is a no-op rather than a second decrement.
            val target = targetsFlow.value.firstOrNull { it.id == id } ?: return@withLock
            if (!target.isActive) return@withLock
            val updatedTargets =
                targetsFlow.value
                    .changeActiveness(id, false)
                    .changeVisibility(id, false)
            val updatedField =
                fieldFlow.value
                    .decrementLifeCount(1)
                    .closeIfNecessary()
            _targetsFlow.value = updatedTargets
            _fieldFlow.value = updatedField
        }

    suspend fun targetShouldBeSaved(
        id: Int,
        position: Int,
        gameColumnHeightPx: Int,
    ) = mutex.withLock {
        _targetsFlow.value = targetsFlow.value.updateTargetPositioning(id, position, gameColumnHeightPx)
    }

    suspend fun fireButtonClicked() =
        mutex.withLock {
            val (nextOperationSign, nextOperationDigit) = getNextSignAndDigit()
            val (afterOperationButtons, resultingScore) =
                targetsFlow.value.performOperation(
                    fieldFlow.value.currentOperationSign,
                    fieldFlow.value.currentOperationDigit,
                )
            val updatedTargets =
                afterOperationButtons
                    .ensureAlive()
                    .ensureVisible()
            _targetsFlow.value = updatedTargets
            val updatedField =
                fieldFlow.value
                    .updateActionButtons(nextOperationSign, nextOperationDigit)
                    .updateScore(resultingScore)
            _fieldFlow.value = updatedField
        }

    suspend fun gameColumnSizeMeasured(
        width: Int,
        height: Int,
    ) = mutex.withLock {
        _fieldFlow.value = fieldFlow.value.updateGameColumnSize(width, height)
    }

    private suspend fun activeTargetsAbsent() =
        mutex.withLock {
            // Re-check under the lock: the collector's own condition (targets.none { it.isActive })
            // was evaluated outside it, and a concurrent createField()/createTargets() - a restart -
            // can repopulate active targets in the window between that check and this coroutine
            // actually acquiring the mutex. Without this, a stale trigger would level up a board
            // that is no longer empty.
            if (_targetsFlow.value.none { it.isActive }) {
                _fieldFlow.value = fieldFlow.value.updateLevel()
                createTargetsLocked()
            }
        }

    private suspend fun visibleTargetsAbsent() =
        mutex.withLock {
            _targetsFlow.value = targetsFlow.value.shortenAppearanceDelay(random)
        }

    // Only ever called from inside a mutex.withLock block above - never acquires the lock itself,
    // so createTargets() and activeTargetsAbsent() can both call it without a non-reentrant Mutex
    // deadlocking on its own coroutine.
    private fun createTargetsLocked() {
        val amount = sessionHelper.getTargetAmountByLevel(fieldFlow.value.level)
        _targetsFlow.value = recreateTargets(amount)
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

    private fun recreateTargets(amount: Int): List<Target> =
        List(amount) { id ->
            Target(
                id + 1,
                fieldFlow.value.id,
                id.toGameColumnId(),
                sessionHelper.getTargetValueByLevel(fieldFlow.value.level),
                0,
                sessionHelper.getTargetAppearanceDelayMsById(id),
                sessionHelper.getTargetLifetimeMsByLevel(fieldFlow.value.level),
            )
        }

    private fun getNextSignAndDigit(): Pair<OperationSign, Int> {
        val nextOperationSign = OperationSign.values().random(random)
        val nextOperationDigit = sessionHelper.getOperationDigitByLevel(nextOperationSign, fieldFlow.value.level)
        return Pair(nextOperationSign, nextOperationDigit)
    }
}
