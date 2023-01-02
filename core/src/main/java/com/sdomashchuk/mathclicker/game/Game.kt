package com.sdomashchuk.mathclicker.game

import com.sdomashchuk.mathclicker.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.game.objectmapper.changeActiveness
import com.sdomashchuk.mathclicker.game.objectmapper.changeVisibility
import com.sdomashchuk.mathclicker.game.objectmapper.closeIfNecessary
import com.sdomashchuk.mathclicker.game.objectmapper.decrementLifeCount
import com.sdomashchuk.mathclicker.game.objectmapper.decrementValue
import com.sdomashchuk.mathclicker.game.objectmapper.ensureAlive
import com.sdomashchuk.mathclicker.game.objectmapper.ensureVisible
import com.sdomashchuk.mathclicker.game.objectmapper.performOperation
import com.sdomashchuk.mathclicker.game.objectmapper.shortenAppearanceDelay
import com.sdomashchuk.mathclicker.game.objectmapper.updateActionButtons
import com.sdomashchuk.mathclicker.game.objectmapper.updateGameColumnSize
import com.sdomashchuk.mathclicker.game.objectmapper.updateLevel
import com.sdomashchuk.mathclicker.game.objectmapper.updateScore
import com.sdomashchuk.mathclicker.game.objectmapper.updateTargetPositioning
import com.sdomashchuk.mathclicker.model.Field
import com.sdomashchuk.mathclicker.model.OperationSign
import com.sdomashchuk.mathclicker.model.Target
import com.sdomashchuk.mathclicker.toGameColumnId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class Game @Inject constructor(
    private val sessionHelper: SessionHelper
) {

    private val gameScope = CoroutineScope(Dispatchers.IO)

    private val _targetsFlow: MutableStateFlow<List<Target>> = MutableStateFlow(listOf())
    val targetsFlow: StateFlow<List<Target>> = _targetsFlow.asStateFlow()

    private val _fieldFlow: MutableStateFlow<Field> = MutableStateFlow(Field())
    val fieldFlow: StateFlow<Field> = _fieldFlow.asStateFlow()

    init {
        gameScope.launch {
            targetsFlow.collect { targets ->
                if (targets.isNotEmpty() && targets.none { it.isVisible }) {
                    visibleTargetsAbsent()
                }
                if (targets.isNotEmpty() && targets.none { it.isActive }) {
                    activeTargetsAbsent()
                }
            }
        }
    }

    fun createField(id: Int) {
        val newField = recreateField(id)
        _fieldFlow.value = newField
    }

    fun createTargets() {
        val amount = sessionHelper.getTargetAmountByLevel(fieldFlow.value.level)
        val newTargets = recreateTargets(amount)
        _targetsFlow.value = newTargets
    }

    fun fieldRestored(field: Field) {
        _fieldFlow.value = field
    }

    fun targetsRestored(targets: List<Target>) {
        _targetsFlow.value = targets
    }

    fun targetClicked(id: Int) {
        val updatedTargets = targetsFlow.value.decrementValue(id, 1)
            .ensureAlive(id)
            .ensureVisible(id)
        _targetsFlow.value = updatedTargets
        if (updatedTargets.first { it.id == id }.isProfitable) {
            _fieldFlow.value = fieldFlow.value.updateScore(1)
        }
    }

    fun targetRevealed(id: Int) {
        val updatedTargets = targetsFlow.value.changeVisibility(id, true)
        _targetsFlow.value = updatedTargets
    }

    fun targetDidBreakout(id: Int) {
        val updatedTargets = targetsFlow.value.changeActiveness(id, false)
            .changeVisibility(id, false)
        val updatedField = fieldFlow.value.decrementLifeCount(1)
            .closeIfNecessary()
        _targetsFlow.value = updatedTargets
        _fieldFlow.value = updatedField
    }

    fun targetShouldBeSaved(id: Int, position: Int, gameColumnHeightPx: Int) {
        val updatedTargets = targetsFlow.value.updateTargetPositioning(id, position, gameColumnHeightPx)
        _targetsFlow.value = updatedTargets
    }

    fun fireButtonClicked() {
        val (nextOperationSign, nextOperationDigit) = getNextSignAndDigit()
        val (afterOperationButtons, resultingScore) = targetsFlow.value.performOperation(
            fieldFlow.value.currentOperationSign, fieldFlow.value.currentOperationDigit
        )
        val updatedTargets = afterOperationButtons
            .ensureAlive()
            .ensureVisible()
        _targetsFlow.value = updatedTargets
        val updatedField = fieldFlow.value.updateActionButtons(nextOperationSign, nextOperationDigit)
            .updateScore(resultingScore)
        _fieldFlow.value = updatedField
    }

    fun gameColumnSizeMeasured(width: Int, height: Int) {
        val updatedField = fieldFlow.value.updateGameColumnSize(width, height)
        _fieldFlow.value = updatedField
    }

    private fun activeTargetsAbsent() {
        val updatedField = fieldFlow.value.updateLevel()
        _fieldFlow.value = updatedField
        createTargets()
    }

    private fun visibleTargetsAbsent() {
        val updatedTargets = targetsFlow.value.shortenAppearanceDelay()
        _targetsFlow.value = updatedTargets
    }

    private fun recreateField(id: Int): Field {
        val currentOperationSign = OperationSign.values().random()
        val currentOperationDigit = sessionHelper.getOperationDigitByLevel(currentOperationSign, 1)
        val nextOperationSign = OperationSign.values().random()
        val nextOperationDigit = sessionHelper.getOperationDigitByLevel(nextOperationSign, 1)
        return Field(
            id = id,
            currentOperationSign = currentOperationSign,
            currentOperationDigit = currentOperationDigit,
            nextOperationSign = nextOperationSign,
            nextOperationDigit = nextOperationDigit,
        )
    }

    private fun recreateTargets(amount: Int): List<Target> {
        return List(amount) { id ->
            Target(
                id + 1,
                fieldFlow.value.id,
                id.toGameColumnId(),
                sessionHelper.getTargetValueByLevel(fieldFlow.value.level),
                0,
                sessionHelper.getTargetAppearanceDelayMsById(id),
                sessionHelper.getTargetLifetimeMsByLevel(fieldFlow.value.level)
            )
        }
    }

    private fun getNextSignAndDigit(): Pair<OperationSign, Int> {
        val nextOperationSign = OperationSign.values().random()
        val nextOperationDigit = sessionHelper.getOperationDigitByLevel(nextOperationSign, fieldFlow.value.level)
        return Pair(nextOperationSign, nextOperationDigit)
    }
}