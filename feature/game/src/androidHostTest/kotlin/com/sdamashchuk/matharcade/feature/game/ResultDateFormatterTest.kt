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
        // The whole point of MC-92's timestamp: a history of ten rows that all read 26.09.26 says
        // nothing about which run is which. Two instants eleven minutes apart must not render alike.
        val morning = 1_790_208_000_000L
        val elevenMinutesLater = morning + 11 * 60 * 1000L
        assertNotEquals(
            formatResultDate(morning, noDateText = "—"),
            formatResultDate(elevenMinutesLater, noDateText = "—"),
        )
    }

    @Test
    fun `formatResultDate agrees with the device's own clock`() {
        // Derived rather than a literal: this is one device's own history, so the assertion is "what
        // this device's clock calls that instant", which a hardcoded string cannot state without also
        // hardcoding a zone - the exact thing the format stopped doing. On a host that happens to run
        // in UTC this cannot tell the current format from the old UTC-pinned one; the zone itself is
        // carried by the absence of a setTimeZone call in ResultDateFormatter, not proven here.
        val instant = 1_790_208_000_000L
        assertEquals(expected(instant), formatResultDate(instant, noDateText = "—"))
    }

    private fun expected(epochMillis: Long): String =
        SimpleDateFormat("dd.MM.yy HH:mm", Locale.US).format(Date(epochMillis))
}
