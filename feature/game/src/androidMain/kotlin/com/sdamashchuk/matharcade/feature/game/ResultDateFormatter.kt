package com.sdamashchuk.matharcade.feature.game

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val RESULT_DATE_PATTERN = "dd.MM.yy HH:mm"

internal fun formatResultDate(
    epochMillis: Long?,
    noDateText: String,
): String = epochMillis?.let { SimpleDateFormat(RESULT_DATE_PATTERN, Locale.US).format(Date(it)) } ?: noDateText
