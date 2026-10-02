package com.sdamashchuk.mathbubbles.core.database.dao

import com.sdamashchuk.mathbubbles.core.database.local.TargetsQueries
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.sdamashchuk.mathbubbles.core.database.local.Targets as LocalTargets

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

// fallenMs, appearanceDelayMs, lifetimeMs and isVisible are MC-72's dead columns: nothing on
// Target carries them any more, so every write pins them to 0/false. They stay in the schema
// because minSdk 24's SQLite has no DROP COLUMN (see 5.sqm).
internal fun TargetsQueries.insert(target: Target) =
    insertTarget(
        id = target.id.toLong(),
        relatedFieldId = target.relatedFieldId.toLong(),
        columnId = target.columnId.toLong(),
        value_ = target.value.toLong(),
        fallenMs = 0,
        appearanceDelayMs = 0,
        lifetimeMs = 0,
        isProfitable = target.isProfitable,
        isVisible = false,
        isActive = target.isActive,
        appearsAtMs = target.appearsAtMs,
        finishesAtMs = target.finishesAtMs,
    )

internal fun TargetsQueries.update(target: Target) =
    updateTarget(
        relatedFieldId = target.relatedFieldId.toLong(),
        columnId = target.columnId.toLong(),
        value_ = target.value.toLong(),
        fallenMs = 0,
        appearanceDelayMs = 0,
        lifetimeMs = 0,
        isProfitable = target.isProfitable,
        isVisible = false,
        isActive = target.isActive,
        appearsAtMs = target.appearsAtMs,
        finishesAtMs = target.finishesAtMs,
        id = target.id.toLong(),
    )

private fun LocalTargets.toDomainModel() =
    Target(
        id = id.toInt(),
        relatedFieldId = relatedFieldId.toInt(),
        columnId = columnId.toInt(),
        value = value_.toInt(),
        appearsAtMs = appearsAtMs,
        finishesAtMs = finishesAtMs,
        isProfitable = isProfitable,
        isActive = isActive,
    )
