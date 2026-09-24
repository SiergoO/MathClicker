package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ResultDateFormatterTest {
    // M4: a null date treated as zero rather than unknown. Epoch zero itself is exercised
    // separately below so a regression to "null coerced to 0L" can't pass by coincidence.
    @Test
    fun `formatResultDate renders a null finishedAt as the no-date placeholder, not epoch zero`() {
        assertEquals("—", formatResultDate(epochMillis = null, noDateText = "—"))
    }

    @Test
    fun `formatResultDate renders epoch zero as an actual date, not the placeholder`() {
        assertEquals("01.01.1970", formatResultDate(epochMillis = 0L, noDateText = "—"))
    }

    @Test
    fun `formatResultDate renders a real timestamp in UTC regardless of the host time zone`() {
        // 2026-09-24T00:00:00Z.
        assertEquals("24.09.2026", formatResultDate(epochMillis = 1_790_208_000_000L, noDateText = "—"))
    }
}
