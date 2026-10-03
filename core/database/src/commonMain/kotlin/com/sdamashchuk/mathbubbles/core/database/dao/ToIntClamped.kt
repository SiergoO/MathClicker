package com.sdamashchuk.mathbubbles.core.database.dao

internal fun Long.toIntClamped(): Int = coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
