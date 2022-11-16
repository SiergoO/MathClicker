package com.sdomashchuk.mathclicker.util

import com.sdomashchuk.mathclicker.domain.model.game.OperationSign

fun OperationSign.toSymbol() = when (this) {
    OperationSign.DIVISION -> "÷"
    OperationSign.SUBTRACTION -> "–"
}