package com.sdamashchuk.mathbubbles.core.game.firespam

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class FireSpamPinningTest {
    @Test
    fun `fire presses at 3 per second end every one of 10 seeded runs in game over within 4x the idle survival time`() =
        runTest { assertSpamLoses(PRESS_RATE_CASUAL_PER_SECOND) }

    private suspend fun TestScope.assertSpamLoses(pressesPerSecond: Int) {
        val idleSurvivalMs = runFireSpamSeeds(PRESS_RATE_DO_NOTHING_PER_SECOND).maxOf { it.survivalGameTimeMs }
        val boundMs = idleSurvivalMs * SPAM_SURVIVAL_BOUND_FACTOR
        val survivors =
            runFireSpamSeeds(pressesPerSecond).filter {
                it.endReason != RunEndReason.GAME_OVER || it.survivalGameTimeMs > boundMs
            }
        assertTrue(survivors.isEmpty(), "bound $boundMs ms game time, survivors: $survivors")
    }
}

private const val SPAM_SURVIVAL_BOUND_FACTOR = 4
