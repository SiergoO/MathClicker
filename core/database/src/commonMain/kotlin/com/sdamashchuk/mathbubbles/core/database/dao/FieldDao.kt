package com.sdamashchuk.mathbubbles.core.database.dao

import com.sdamashchuk.mathbubbles.core.database.local.FieldQueries
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.sdamashchuk.mathbubbles.core.database.local.Field_ as LocalField

private const val BOOSTER_STASH_SEPARATOR = ","

class FieldDao(
    private val queries: FieldQueries,
    private val dispatcher: CoroutineDispatcher,
) {
    // id == 0 is the domain model's "not yet persisted" default. Room bound NULL for it on an
    // autoGenerate primary key so SQLite would assign the next rowid; the generated insertField
    // takes a nullable id for the same reason, so the mapping here is a direct translation.
    suspend fun insertField(field: Field) =
        withContext(dispatcher) {
            queries.insertField(
                id = field.id.takeIf { it != 0 }?.toLong(),
                level = field.level.toLong(),
                score = field.score.toLong(),
                lifeCount = field.lifeCount.toLong(),
                bonusMultiplier = field.bonusMultiplier.toLong(),
                currentOperationSign = field.currentOperationSign.sign,
                currentOperationDigit = field.currentOperationDigit.toLong(),
                nextOperationSign = field.nextOperationSign.sign,
                nextOperationDigit = field.nextOperationDigit.toLong(),
                isClosed = field.isClosed,
                finishedAt = field.finishedAt,
                gameTimeMs = field.gameTimeMs,
                currentBooster = field.currentBooster?.name,
                nextBooster = field.nextBooster?.name,
                boosterStash = field.boosterStash.joinToString(BOOSTER_STASH_SEPARATOR) { it.name },
                boosterDropCounter = field.boosterDropCounter.toLong(),
                hasDroppedBoosterThisSession = field.hasDroppedBoosterThisSession,
            )
        }

    suspend fun updateField(field: Field) =
        withContext(dispatcher) {
            queries.updateField(
                level = field.level.toLong(),
                score = field.score.toLong(),
                lifeCount = field.lifeCount.toLong(),
                bonusMultiplier = field.bonusMultiplier.toLong(),
                currentOperationSign = field.currentOperationSign.sign,
                currentOperationDigit = field.currentOperationDigit.toLong(),
                nextOperationSign = field.nextOperationSign.sign,
                nextOperationDigit = field.nextOperationDigit.toLong(),
                isClosed = field.isClosed,
                finishedAt = field.finishedAt,
                gameTimeMs = field.gameTimeMs,
                currentBooster = field.currentBooster?.name,
                nextBooster = field.nextBooster?.name,
                boosterStash = field.boosterStash.joinToString(BOOSTER_STASH_SEPARATOR) { it.name },
                boosterDropCounter = field.boosterDropCounter.toLong(),
                hasDroppedBoosterThisSession = field.hasDroppedBoosterThisSession,
                id = field.id.toLong(),
            )
        }

    suspend fun getFieldById(id: Int): Field =
        withContext(dispatcher) { queries.getFieldById(id.toLong()).executeAsOne().toDomainModel() }

    suspend fun getUnfinishedField(): Field? =
        withContext(dispatcher) { queries.getUnfinishedField().executeAsOneOrNull()?.toDomainModel() }

    suspend fun getFieldCount(): Int = withContext(dispatcher) { queries.getFieldCount().executeAsOne().toInt() }

    // Newest first, capped at 7 (see Field.sq); the best among them is a separate query below
    // because it has to read past this window, not just this page.
    suspend fun getRecentClosedFields(): List<Field> =
        withContext(dispatcher) { queries.getRecentClosedFields().executeAsList().map { it.toDomainModel() } }

    suspend fun getBestClosedField(): Field? =
        withContext(dispatcher) { queries.getBestClosedField().executeAsOneOrNull()?.toDomainModel() }
}

private fun LocalField.toDomainModel() =
    Field(
        id = id.toInt(),
        level = level.toInt(),
        score = score.toInt(),
        lifeCount = lifeCount.toInt(),
        bonusMultiplier = bonusMultiplier.toInt(),
        currentOperationSign = OperationSign.values().first { it.sign == currentOperationSign },
        currentOperationDigit = currentOperationDigit.toInt(),
        nextOperationSign = OperationSign.values().first { it.sign == nextOperationSign },
        nextOperationDigit = nextOperationDigit.toInt(),
        isClosed = isClosed,
        finishedAt = finishedAt,
        gameTimeMs = gameTimeMs,
        currentBooster = currentBooster?.let { Booster.valueOf(it) },
        nextBooster = nextBooster?.let { Booster.valueOf(it) },
        boosterStash =
            boosterStash.split(BOOSTER_STASH_SEPARATOR).filter { it.isNotEmpty() }.map { Booster.valueOf(it) },
        boosterDropCounter = boosterDropCounter.toInt(),
        hasDroppedBoosterThisSession = hasDroppedBoosterThisSession,
    )
