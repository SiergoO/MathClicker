package com.sdamashchuk.mathbubbles.core.game.firespam

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FireSpamReportTest {
    @Test
    fun `prints the survival report for both press rates and the baseline`() =
        runTest {
            val baseline = runFireSpamSeeds(PRESS_RATE_DO_NOTHING_PER_SECOND)
            val casual = runFireSpamSeeds(PRESS_RATE_CASUAL_PER_SECOND)
            val frantic = runFireSpamSeeds(PRESS_RATE_FRANTIC_PER_SECOND)
            println(baseline.toMarkdownTable("Baseline: no presses"))
            println(casual.toMarkdownTable("Spam at $PRESS_RATE_CASUAL_PER_SECOND presses per second"))
            println(frantic.toMarkdownTable("Spam at $PRESS_RATE_FRANTIC_PER_SECOND presses per second"))
            assertEquals(FIRE_SPAM_SEEDS, baseline.map { it.seed })
        }
}
