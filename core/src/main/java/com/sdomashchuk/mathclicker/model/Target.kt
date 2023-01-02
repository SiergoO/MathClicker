package com.sdomashchuk.mathclicker.model

data class Target(
    val id: Int,
    val relatedFieldId: Int,
    val columnId: Int,
    val value: Int,
    val position: Int,
    val appearanceDelayMs: Int,
    val lifetimeMs: Int,
    val isProfitable: Boolean = true,
    val isVisible: Boolean = false,
    val isActive: Boolean = true
)
