package com.sdomashchuk.mathclicker.core.game

fun Int.toGameColumnId(): Int = if (this !in 0..3) (this + 1) % 4 else this
