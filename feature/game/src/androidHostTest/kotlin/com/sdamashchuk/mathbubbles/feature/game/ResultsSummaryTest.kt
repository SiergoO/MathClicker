package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.feature.game.model.ResultsSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultsSummaryTest {
    // Gate: the empty-history case must render (no crash) and must not show a delta to nothing.
    @Test
    fun `resultsSummaryOf reports no history rather than a fabricated record when there is no best`() {
        assertEquals(ResultsSummary.NoHistory(score = 120), resultsSummaryOf(Field(score = 120), best = null))
    }

    @Test
    fun `resultsSummaryOf reports a new record when the current run matches or beats the best`() {
        val best = Field(score = 500, level = 4, isClosed = true)

        assertEquals(ResultsSummary.NewRecord(500), resultsSummaryOf(Field(score = 500), best))
        assertEquals(ResultsSummary.NewRecord(600), resultsSummaryOf(Field(score = 600), best))
    }

    // M5: the delta computed against the most recent run rather than the best. The function only
    // ever sees `best`, never a separate "most recent run" value, so there is nothing here for that
    // mutation to substitute - this pins the number the delta must produce against a best that
    // differs from the current run either way.
    @Test
    fun `resultsSummaryOf computes the delta to the best, not to the current run's own margin`() {
        val best = Field(score = 500, level = 4, isClosed = true)

        assertEquals(
            ResultsSummary.ShortOfBest(score = 300, deltaToBest = 200),
            resultsSummaryOf(Field(score = 300), best),
        )
    }
}
