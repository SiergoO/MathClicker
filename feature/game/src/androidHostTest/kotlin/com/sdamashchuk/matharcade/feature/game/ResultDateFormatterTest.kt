package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultDateFormatterTest {
    // M4: a null date treated as zero rather than unknown. Epoch zero itself is exercised
    // separately below so a regression to "null coerced to 0L" can't pass by coincidence.
    @Test
    fun `formatResultDate renders a null finishedAt as the no-date placeholder, not epoch zero`() {
        assertEquals("—", formatResultDate(epochMillis = null, noDateText = "—"))
    }

    @Test
    fun `formatResultDate renders epoch zero as an actual date, not the placeholder`() {
        assertEquals(expected(0L), formatResultDate(epochMillis = 0L, noDateText = "—"))
    }

    @Test
    fun `formatResultDate carries a time of day, so two runs on the same day are told apart`() {
        val morning = 1_790_208_000_000L
        val elevenMinutesLater = morning + 11 * 60 * 1000L
        assertNotEquals(
            formatResultDate(morning, noDateText = "—"),
            formatResultDate(elevenMinutesLater, noDateText = "—"),
        )
    }

    @Test
    fun `formatResultDate agrees with the device's own clock`() {
        // A host running in UTC cannot tell this from the old UTC-pinned format; the zone is
        // carried by the absent setTimeZone call, not proven here.
        val instant = 1_790_208_000_000L
        assertEquals(expected(instant), formatResultDate(instant, noDateText = "—"))
    }

    private fun expected(epochMillis: Long): String =
        SimpleDateFormat("dd.MM.yy HH:mm", Locale.US).format(Date(epochMillis))
}
