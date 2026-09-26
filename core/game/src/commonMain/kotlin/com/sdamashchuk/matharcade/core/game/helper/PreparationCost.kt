package com.sdamashchuk.matharcade.core.game.helper

import kotlin.random.Random

// Step table, not a curve - each row is a level bracket's cost profile, looked up by the
// highest breakpoint at or below the level. Levels between two named rows (the spec only names
// 1-3, 10, 30, 50+) inherit the row below them; this is what "one edit per row" means in practice.
private val PREPARATION_PROFILE_LEVEL_1 = PreparationProfile(25, 65, 10, 0)
private val PREPARATION_PROFILE_LEVEL_10 = PreparationProfile(25, 45, 30, 0)
private val PREPARATION_PROFILE_LEVEL_30 = PreparationProfile(30, 30, 30, 10)
private val PREPARATION_PROFILE_LEVEL_50 = PreparationProfile(35, 25, 25, 15)

private const val PROFILE_BREAKPOINT_10 = 10
private const val PROFILE_BREAKPOINT_30 = 30
private const val PROFILE_BREAKPOINT_50 = 50

private const val ONE_TAP_COST = 1
private const val TWO_OR_THREE_TAPS_MIN = 2
private const val TWO_OR_THREE_TAPS_MAX = 3
private const val FOUR_PLUS_TAPS_MIN = 4

// Picks the number of taps a generated target should need before operationDigit succeeds, from the
// level's profile. A bucket that operationDigit is too small to realize (e.g. "2-3 taps" against a
// divisor of 2, whose only possible costs are 0 and 1) collapses to the largest cost that digit can
// actually produce, rather than picking an unreachable one.
internal fun desiredPreparationCost(
    level: Int,
    operationDigit: Int,
    random: Random,
): Int {
    val profile = preparationProfileByLevel(level)
    val maxCost = operationDigit - 1
    val roll = random.nextInt(PreparationProfile.PERCENT_TOTAL)
    return when {
        roll < profile.readyNowPercent -> {
            0
        }

        roll < profile.readyNowPercent + profile.oneTapPercent -> {
            minOf(ONE_TAP_COST, maxCost)
        }

        roll < profile.readyNowPercent + profile.oneTapPercent + profile.twoOrThreeTapsPercent -> {
            costInClampedRange(TWO_OR_THREE_TAPS_MIN, TWO_OR_THREE_TAPS_MAX, maxCost, random)
        }

        else -> {
            costInClampedRange(FOUR_PLUS_TAPS_MIN, maxCost, maxCost, random)
        }
    }
}

// Step table lookup: the highest named breakpoint at or below level wins, so levels between two
// named rows (the spec only names 1-3, 10, 30, 50+) inherit the row below them.
private fun preparationProfileByLevel(level: Int): PreparationProfile =
    when {
        level < PROFILE_BREAKPOINT_10 -> PREPARATION_PROFILE_LEVEL_1
        level < PROFILE_BREAKPOINT_30 -> PREPARATION_PROFILE_LEVEL_10
        level < PROFILE_BREAKPOINT_50 -> PREPARATION_PROFILE_LEVEL_30
        else -> PREPARATION_PROFILE_LEVEL_50
    }

private fun costInClampedRange(
    lo: Int,
    hi: Int,
    maxCost: Int,
    random: Random,
): Int {
    val clampedHi = minOf(hi, maxCost)
    return if (clampedHi < lo) maxCost else IntRange(lo, clampedHi).random(random)
}
