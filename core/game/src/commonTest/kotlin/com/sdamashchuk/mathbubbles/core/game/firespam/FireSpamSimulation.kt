package com.sdamashchuk.mathbubbles.core.game.firespam

import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import kotlinx.coroutines.CoroutineScope
import kotlin.random.Random

const val PRESS_RATE_CASUAL_PER_SECOND = 3
const val PRESS_RATE_FRANTIC_PER_SECOND = 8
const val PRESS_RATE_DO_NOTHING_PER_SECOND = 0
const val FIRE_SPAM_GAME_TIME_CAP_MS = 600_000L
const val FIRE_SPAM_REAL_TIME_CAP_MS = 2 * FIRE_SPAM_GAME_TIME_CAP_MS

private const val SIMULATION_TICK_MS = 16
private const val MS_PER_SECOND = 1000

/**
 * Plays one seeded session; [scope] must run its collectors eagerly (unconfined) so level-ups after a press land before the next tick.
 */
suspend fun runFireSpamSimulation(
    seed: Long,
    pressesPerSecond: Int,
    scope: CoroutineScope,
): FireSpamRunResult {
    val game = Game(SessionHelperImpl(Random(seed)), scope, Random(seed), boostersEnabled = true)
    game.start()
    game.createField(1)
    game.createTargets()

    val pressIntervalMs = if (pressesPerSecond > 0) MS_PER_SECOND / pressesPerSecond else 0
    var pressAccumulatorMs = 0
    var pressesMade = 0
    var realTimeMs = 0L
    var peakMultiplier = game.stateFlow.value.field.appliedMultiplier

    while (!game.stateFlow.value.field.isClosed && game.stateFlow.value.field.gameTimeMs < FIRE_SPAM_GAME_TIME_CAP_MS &&
        realTimeMs < FIRE_SPAM_REAL_TIME_CAP_MS
    ) {
        game.tick(SIMULATION_TICK_MS)
        realTimeMs += SIMULATION_TICK_MS
        peakMultiplier = maxOf(peakMultiplier, game.stateFlow.value.field.appliedMultiplier)
        if (pressesPerSecond > 0 && !game.stateFlow.value.field.isClosed) {
            pressAccumulatorMs += SIMULATION_TICK_MS
            while (pressAccumulatorMs >= pressIntervalMs) {
                game.fireButtonClicked()
                pressesMade++
                pressAccumulatorMs -= pressIntervalMs
                peakMultiplier = maxOf(peakMultiplier, game.stateFlow.value.field.appliedMultiplier)
            }
        }
    }
    game.stop()

    val finalField = game.stateFlow.value.field
    return FireSpamRunResult(
        seed = seed,
        survivalGameTimeMs = finalField.gameTimeMs,
        survivalRealTimeMs = realTimeMs,
        levelReached = finalField.level,
        score = finalField.score,
        peakMultiplier = peakMultiplier,
        pressesMade = pressesMade,
        endReason = if (finalField.isClosed) RunEndReason.GAME_OVER else RunEndReason.TIME_CAP,
    )
}
