package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target

internal class FadeContext {
    val gameTimeMs = HashMap<Int, Long>()
    val realTimeMs = HashMap<Int, Long>()
    val isReady = HashMap<Int, Boolean>()
    private val lastSeenTargets = HashMap<Int, Target>()
    private val previousVisibleIds = mutableSetOf<Int>()

    // Must run in composition, not an effect: a dropped target would otherwise vanish for a frame
    // and remount with fresh animation state instead of fading.
    fun sync(
        visibleTargets: List<Target>,
        fadingTargets: MutableMap<Int, Target>,
        operationSign: OperationSign,
        operationDigit: Int,
        divisionHintsEnabled: Boolean,
        gameTimeMsProvider: () -> Long,
        realTimeMsProvider: () -> Long,
    ) {
        val currentVisibleIds = visibleTargets.mapTo(mutableSetOf()) { it.id }
        if (currentVisibleIds != previousVisibleIds) {
            for (id in previousVisibleIds - currentVisibleIds) {
                val target = lastSeenTargets[id] ?: continue
                gameTimeMs[id] = gameTimeMsProvider()
                realTimeMs[id] = realTimeMsProvider()
                isReady[id] = shouldShowDivisionHint(target, operationSign, operationDigit, divisionHintsEnabled)
                fadingTargets[id] = target
            }
            previousVisibleIds.clear()
            previousVisibleIds += currentVisibleIds
        }
        lastSeenTargets.keys.retainAll(currentVisibleIds)
        visibleTargets.forEach { lastSeenTargets[it.id] = it }
    }

    fun remove(id: Int) {
        gameTimeMs.remove(id)
        realTimeMs.remove(id)
        isReady.remove(id)
    }
}
