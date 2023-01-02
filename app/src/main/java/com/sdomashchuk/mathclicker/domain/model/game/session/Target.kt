package com.sdomashchuk.mathclicker.domain.model.game.session

import kotlinx.collections.immutable.toImmutableList

typealias GameTarget = com.sdomashchuk.mathclicker.model.Target
typealias DomainTarget = Target

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

fun DomainTarget.toGameModel() = GameTarget(
    id,
    relatedFieldId,
    columnId,
    value,
    position,
    appearanceDelayMs,
    lifetimeMs,
    isProfitable,
    isVisible,
    isActive
)

fun GameTarget.toDomainModel() = DomainTarget(
    id,
    relatedFieldId,
    columnId,
    value,
    position,
    appearanceDelayMs,
    lifetimeMs,
    isProfitable,
    isVisible,
    isActive
)

fun List<DomainTarget>.toGameList() = this.map { it.toGameModel() }

fun List<GameTarget>.toDomainList() = this.map { it.toDomainModel() }

fun List<GameTarget>.toImmutableDomainList() = this.map { it.toDomainModel() }.toImmutableList()
