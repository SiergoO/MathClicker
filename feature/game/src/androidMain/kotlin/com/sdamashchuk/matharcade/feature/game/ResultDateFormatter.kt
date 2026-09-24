package com.sdamashchuk.matharcade.feature.game

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// UTC, not the device zone: two devices in different zones must read the same stored run the same
// way, and there is nothing in `field` that records which zone finishedAt was captured in.
private val dateFormat =
    SimpleDateFormat("dd.MM.yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

// null is a pre-MC-53 run with no recorded date, not epoch zero (M4): formatting it as
// 01.01.1970 would read as a real, if very old, date instead of the honest "unknown" it is.
internal fun formatResultDate(
    epochMillis: Long?,
    noDateText: String,
): String = epochMillis?.let { dateFormat.format(Date(it)) } ?: noDateText
