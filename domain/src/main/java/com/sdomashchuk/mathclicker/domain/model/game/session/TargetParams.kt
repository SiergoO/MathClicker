package com.sdomashchuk.mathclicker.domain.model.game.session

data class TargetParams(
    val id: Int,
    val relatedGameFieldId: Int,
    val columnId: Int,
    val value: Int,
    val position: Int,
    val animationDelayMs: Int,
    val animationDurationMs: Int,
    val isVisible: Boolean = false,
    val isAlive: Boolean = true
)
