package com.sdamashchuk.matharcade.feature.game

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// MC-92: date and time of day, in the device's own zone. It was a bare dd.MM.yyyy in UTC, and a
// history where every row reads "26.09.2026" tells the player nothing about which run is which -
// that, not the column widths, is what made the table read as a pile. The zone is the device's for
// the same reason: this is one device's own history, and a run the player finished at 22:38 must say
// 22:38, not whatever that instant is in UTC. Two-digit year to keep the cell narrow enough that the
// numeric columns still have room.
private val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.US)

// null is a pre-MC-53 run with no recorded date, not epoch zero (M4): formatting it as
// 01.01.1970 would read as a real, if very old, date instead of the honest "unknown" it is.
internal fun formatResultDate(
    epochMillis: Long?,
    noDateText: String,
): String = epochMillis?.let { dateFormat.format(Date(it)) } ?: noDateText
