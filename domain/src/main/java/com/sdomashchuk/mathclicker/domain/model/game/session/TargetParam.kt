package com.sdomashchuk.mathclicker.domain.model.game.session

data class TargetParam(
    val id: Int,
    val value: Int,
    val animationDurationMs: Int,
    val isAlive: Boolean = true
)
