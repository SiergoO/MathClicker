package com.sdamashchuk.mathbubbles.feature.game

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val UTC: ZoneId = ZoneId.of("UTC")
private val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")
private val EXPECTED_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d HH:mm:ss", Locale.ENGLISH)

class ResultDateFormatterTest {
    private val defaultLocale: Locale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `formatResultDate renders a null finishedAt as the no-date placeholder, not epoch zero`() {
        assertEquals("—", formatResultDate(epochMillis = null, noDateText = "—", zoneId = UTC))
    }

    @Test
    fun `formatResultDate renders epoch zero as an actual date, not the placeholder`() {
        assertEquals(expected(0L, UTC), formatResultDate(epochMillis = 0L, noDateText = "—", zoneId = UTC))
    }

    @Test
    fun `formatResultDate carries a time of day, so two runs on the same day are told apart`() {
        val morning = 1_790_208_000_000L
        val elevenMinutesLater = morning + 11 * 60 * 1000L
        assertNotEquals(
            formatResultDate(morning, noDateText = "—", zoneId = UTC),
            formatResultDate(elevenMinutesLater, noDateText = "—", zoneId = UTC),
        )
    }

    @Test
    fun `formatResultDate renders the month abbreviation, day, and 24h time with seconds`() {
        val instant = Instant.parse("2026-09-07T10:42:21Z").toEpochMilli()

        assertEquals("Sep 7 10:42:21", formatResultDate(instant, noDateText = "—", zoneId = UTC))
    }

    @Test
    fun `formatResultDate keeps the month in English when the device locale is not`() {
        Locale.setDefault(Locale.FRANCE)
        val instant = Instant.parse("2026-09-07T10:42:21Z").toEpochMilli()

        assertEquals("Sep 7 10:42:21", formatResultDate(instant, noDateText = "—", zoneId = UTC))
    }

    @Test
    fun `formatResultDate follows the injected time zone, not a fixed one`() {
        val instant = 1_790_208_000_000L

        val inUtc = formatResultDate(instant, noDateText = "—", zoneId = UTC)
        val inTokyo = formatResultDate(instant, noDateText = "—", zoneId = TOKYO)

        assertNotEquals(inUtc, inTokyo)
    }

    private fun expected(
        epochMillis: Long,
        zoneId: ZoneId,
    ): String = EXPECTED_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))
}
