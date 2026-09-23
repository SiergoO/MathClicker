package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT

fun Int.toGameColumnId(): Int = if (this !in 0 until GAME_COLUMN_COUNT) (this + 1) % GAME_COLUMN_COUNT else this
