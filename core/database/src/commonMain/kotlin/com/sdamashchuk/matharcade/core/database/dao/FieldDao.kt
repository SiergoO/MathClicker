package com.sdamashchuk.matharcade.core.database.dao

import com.sdamashchuk.matharcade.core.database.local.FieldQueries
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.sdamashchuk.matharcade.core.database.local.Field_ as LocalField

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
                id = field.id.toLong(),
            )
        }

    suspend fun getFieldById(id: Int): Field =
        withContext(dispatcher) { queries.getFieldById(id.toLong()).executeAsOne().toDomainModel() }

    suspend fun getUnfinishedField(): Field? =
        withContext(dispatcher) { queries.getUnfinishedField().executeAsOneOrNull()?.toDomainModel() }

    suspend fun getFieldCount(): Int = withContext(dispatcher) { queries.getFieldCount().executeAsOne().toInt() }

    // Newest first, capped at 10 (see Field.sq); the best among them is a separate query below
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
    )
