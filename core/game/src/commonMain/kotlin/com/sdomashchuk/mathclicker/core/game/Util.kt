package com.sdomashchuk.mathclicker.core.game

import com.sdomashchuk.mathclicker.core.model.GAME_COLUMN_COUNT

fun Int.toGameColumnId(): Int = if (this !in 0 until GAME_COLUMN_COUNT) (this + 1) % GAME_COLUMN_COUNT else this
