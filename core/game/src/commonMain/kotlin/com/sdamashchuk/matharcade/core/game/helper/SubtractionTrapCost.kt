package com.sdamashchuk.matharcade.core.game.helper

import kotlin.random.Random

// MC-80: subtraction's own axis - trap share, not division's four tap-cost buckets borrowed
// backwards (PreparationCost.kt's cost-as-value collapse made low levels the trap-heaviest and
// piled traps onto the digit 1). Trap share rises with level per ASK-27 ("смягчить: уменьшить
// долю ловушек на низких уровнях"): a gentle start teaches the mechanic, and traps become a bigger
// share of the board only as the player's pace - not this curve alone - is what makes the level hard.
private val SUBTRACTION_TRAP_PROFILE_LEVEL_1 = SubtractionTrapProfile(trapPercent = 20)
private val SUBTRACTION_TRAP_PROFILE_LEVEL_10 = SubtractionTrapProfile(trapPercent = 35)
private val SUBTRACTION_TRAP_PROFILE_LEVEL_30 = SubtractionTrapProfile(trapPercent = 50)
private val SUBTRACTION_TRAP_PROFILE_LEVEL_50 = SubtractionTrapProfile(trapPercent = 65)

private const val PROFILE_BREAKPOINT_10 = 10
private const val PROFILE_BREAKPOINT_30 = 30
private const val PROFILE_BREAKPOINT_50 = 50

// Null means "not a trap - generate ready". A trap's value is drawn uniformly from every value it
// can occupy (1 until operationDigit) rather than weighted toward the low end, so the value itself
// doesn't repeat PreparationCost's bug at one remove. At operationDigit == 2 the only value below
// the digit is 1 - a constraint of the digit, not a collapse in this curve.
internal fun desiredSubtractionTrapValue(
    level: Int,
    operationDigit: Int,
    random: Random,
): Int? {
    val profile = subtractionTrapProfileByLevel(level)
    val roll = random.nextInt(SubtractionTrapProfile.PERCENT_TOTAL)
    if (roll >= profile.trapPercent) return null
    return IntRange(1, operationDigit - 1).random(random)
}

// Step table lookup, same shape as PreparationCost's: the highest named breakpoint at or below
// level wins, so levels between two named rows inherit the row below them.
private fun subtractionTrapProfileByLevel(level: Int): SubtractionTrapProfile =
    when {
        level < PROFILE_BREAKPOINT_10 -> SUBTRACTION_TRAP_PROFILE_LEVEL_1
        level < PROFILE_BREAKPOINT_30 -> SUBTRACTION_TRAP_PROFILE_LEVEL_10
        level < PROFILE_BREAKPOINT_50 -> SUBTRACTION_TRAP_PROFILE_LEVEL_30
        else -> SUBTRACTION_TRAP_PROFILE_LEVEL_50
    }
