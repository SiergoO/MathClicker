package com.sdomashchuk.mathclicker.core.database.dao

import com.sdomashchuk.mathclicker.core.database.local.TargetsQueries
import com.sdomashchuk.mathclicker.core.model.Target
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.sdomashchuk.mathclicker.core.database.local.Targets as LocalTargets

class TargetsDao(
    private val queries: TargetsQueries,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend fun updateTargets(targets: List<Target>) =
        withContext(dispatcher) {
            queries.transaction { targets.forEach { queries.update(it) } }
        }

    // Delete and insert must land in one SQL transaction: run as two separate calls, process death
    // between them commits the delete without the insert, leaving an open field with zero targets.
    suspend fun refreshTargets(targets: List<Target>) =
        withContext(dispatcher) {
            queries.transaction {
                queries.deleteTargets()
                targets.forEach { queries.insert(it) }
            }
        }

    suspend fun getTargets(): List<Target> =
        withContext(dispatcher) { queries.getTargets().executeAsList().map { it.toDomainModel() } }
}

private fun TargetsQueries.insert(target: Target) =
    insertTarget(
        id = target.id.toLong(),
        relatedFieldId = target.relatedFieldId.toLong(),
        columnId = target.columnId.toLong(),
        value_ = target.value.toLong(),
        position = target.position.toLong(),
        appearanceDelayMs = target.appearanceDelayMs.toLong(),
        lifetimeMs = target.lifetimeMs.toLong(),
        isProfitable = target.isProfitable,
        isVisible = target.isVisible,
        isActive = target.isActive,
    )

private fun TargetsQueries.update(target: Target) =
    updateTarget(
        relatedFieldId = target.relatedFieldId.toLong(),
        columnId = target.columnId.toLong(),
        value_ = target.value.toLong(),
        position = target.position.toLong(),
        appearanceDelayMs = target.appearanceDelayMs.toLong(),
        lifetimeMs = target.lifetimeMs.toLong(),
        isProfitable = target.isProfitable,
        isVisible = target.isVisible,
        isActive = target.isActive,
        id = target.id.toLong(),
    )

private fun LocalTargets.toDomainModel() =
    Target(
        id = id.toInt(),
        relatedFieldId = relatedFieldId.toInt(),
        columnId = columnId.toInt(),
        value = value_.toInt(),
        position = position.toInt(),
        appearanceDelayMs = appearanceDelayMs.toInt(),
        lifetimeMs = lifetimeMs.toInt(),
        isProfitable = isProfitable,
        isVisible = isVisible,
        isActive = isActive,
    )
