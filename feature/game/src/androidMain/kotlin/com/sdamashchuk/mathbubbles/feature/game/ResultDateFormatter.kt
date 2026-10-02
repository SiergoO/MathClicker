package com.sdamashchuk.mathbubbles.feature.game

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RESULT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d HH:mm:ss", Locale.ENGLISH)

internal fun formatResultDate(
    epochMillis: Long?,
    noDateText: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String =
    epochMillis?.let {
        RESULT_DATE_FORMATTER.format(Instant.ofEpochMilli(it).atZone(zoneId))
    } ?: noDateText
