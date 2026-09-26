package com.sdamashchuk.matharcade.feature.game

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.US)

// null is a pre-MC-53 run with no recorded date, not epoch zero: 01.01.1970 would read as a real,
// if very old, date instead of the unknown it is.
internal fun formatResultDate(
    epochMillis: Long?,
    noDateText: String,
): String = epochMillis?.let { dateFormat.format(Date(it)) } ?: noDateText
