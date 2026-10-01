package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.BoosterDropContext
import com.sdamashchuk.mathbubbles.core.game.model.BoosterDropResult
import com.sdamashchuk.mathbubbles.core.model.Booster
import kotlin.random.Random

// The owner targets one booster per 10-15 draws; retune these on the device, together.
private const val SILENT_DRAWS = 6
private const val GUARANTEED_DRAW = 20
private const val CHANCE_STEP_PER_DRAW = 0.01

private const val BASE_FREEZE_WEIGHT = 3
private const val BASE_REWIND_WEIGHT = 2
private const val BASE_ICE_PICK_WEIGHT = 2
private const val BASE_SHIELD_WEIGHT = 1
private const val TELEGRAPH_WEIGHT_MULTIPLIER = 2
private const val LAST_LIFE_SHIELD_MULTIPLIER = 3

/**
 * Decides once per fire whether the next action is a booster; stateless, the caller owns the counter.
 */
internal class BoosterDropRule(
    private val random: Random,
) {
    // The 6 silent draws happen once per session: repeating them after every drop lifts the mean
    // interval from 11.8 to over 16.
    fun roll(
        counter: Int,
        hasDroppedBefore: Boolean,
        stashFull: Boolean,
        context: BoosterDropContext,
    ): BoosterDropResult {
        if (stashFull) return BoosterDropResult(booster = null, counter = counter)
        val silentDraws = if (hasDroppedBefore) 0 else SILENT_DRAWS
        val drawNumber = counter + 1
        val drops =
            when {
                drawNumber <= silentDraws -> false
                drawNumber >= GUARANTEED_DRAW -> true
                else -> random.nextDouble() < (drawNumber - silentDraws) * CHANCE_STEP_PER_DRAW
            }
        return if (drops) {
            BoosterDropResult(booster = pickBooster(context), counter = 0)
        } else {
            BoosterDropResult(booster = null, counter = drawNumber)
        }
    }

    private fun pickBooster(context: BoosterDropContext): Booster {
        val weights = weightsFor(context)
        val total = weights.values.sum()
        var remaining = random.nextInt(total)
        for ((booster, weight) in weights) {
            if (remaining < weight) return booster
            remaining -= weight
        }
        error("booster weights ($weights) summed to $total but left $remaining unclaimed")
    }

    private fun weightsFor(context: BoosterDropContext): Map<Booster, Int> {
        val weights =
            linkedMapOf(
                Booster.FREEZE to BASE_FREEZE_WEIGHT,
                Booster.REWIND to BASE_REWIND_WEIGHT,
                Booster.ICE_PICK to BASE_ICE_PICK_WEIGHT,
                Booster.SHIELD to BASE_SHIELD_WEIGHT,
            )
        if (context.anyVisibleBeyondTelegraph) {
            weights[Booster.FREEZE] = BASE_FREEZE_WEIGHT * TELEGRAPH_WEIGHT_MULTIPLIER
            weights[Booster.ICE_PICK] = BASE_ICE_PICK_WEIGHT * TELEGRAPH_WEIGHT_MULTIPLIER
        }
        if (context.oneLifeLeft) {
            weights[Booster.SHIELD] = BASE_SHIELD_WEIGHT * LAST_LIFE_SHIELD_MULTIPLIER
        }
        if (context.noVisibleBubble || context.icePickArmed) {
            weights.remove(Booster.ICE_PICK)
        }
        if (context.shieldActive) {
            weights.remove(Booster.SHIELD)
        }
        return weights
    }
}
