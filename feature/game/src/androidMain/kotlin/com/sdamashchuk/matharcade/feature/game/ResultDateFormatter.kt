package com.sdamashchuk.matharcade.feature.game

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val RESULT_DATE_PATTERN = "dd.MM.yy HH:mm"

// null is a pre-MC-53 run with no recorded date, not epoch zero: 01.01.1970 would read as a real,
// if very old, date instead of the unknown it is.
internal fun formatResultDate(
    epochMillis: Long?,
    noDateText: String,
): String = epochMillis?.let { SimpleDateFormat(RESULT_DATE_PATTERN, Locale.US).format(Date(it)) } ?: noDateText
