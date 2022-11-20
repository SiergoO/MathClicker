package com.sdomashchuk.mathclicker.util

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign

fun String.toOperationSign(): OperationSign = when (this) {
    "÷" -> OperationSign.DIVISION
    else -> OperationSign.SUBTRACTION
}

fun OperationSign.toSymbol(): String = when (this) {
    OperationSign.DIVISION -> "÷"
    OperationSign.SUBTRACTION -> "–"
}

fun Int.toColumnId(): Int = if (this !in 0..3) (this + 1) % 4 else this