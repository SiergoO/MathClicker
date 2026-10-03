package com.sdamashchuk.mathbubbles.core.game.firespam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun TestScope.runFireSpamSeeds(pressesPerSecond: Int): List<FireSpamRunResult> {
    val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
    return FIRE_SPAM_SEEDS.map { runFireSpamSimulation(it, pressesPerSecond, scope) }
}
