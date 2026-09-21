package com.sdomashchuk.mathclicker.data.database.dao

import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.OperationSign
import com.sdomashchuk.mathclicker.data.database.local.FieldQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sdomashchuk.mathclicker.data.database.local.Field_ as LocalField

class FieldDao(
    private val queries: FieldQueries,
) {
    // id == 0 is the domain model's "not yet persisted" default. Room bound NULL for it on an
    // autoGenerate primary key so SQLite would assign the next rowid; the generated insertField
    // takes a nullable id for the same reason, so the mapping here is a direct translation.
    suspend fun insertField(field: Field) =
        withContext(Dispatchers.IO) {
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
                gameColumnWidthPx = field.gameColumnWidthPx.toLong(),
                gameColumnHeightPx = field.gameColumnHeightPx.toLong(),
                isClosed = field.isClosed,
            )
        }

    suspend fun updateField(field: Field) =
        withContext(Dispatchers.IO) {
            queries.updateField(
                level = field.level.toLong(),
                score = field.score.toLong(),
                lifeCount = field.lifeCount.toLong(),
                bonusMultiplier = field.bonusMultiplier.toLong(),
                currentOperationSign = field.currentOperationSign.sign,
                currentOperationDigit = field.currentOperationDigit.toLong(),
                nextOperationSign = field.nextOperationSign.sign,
                nextOperationDigit = field.nextOperationDigit.toLong(),
                gameColumnWidthPx = field.gameColumnWidthPx.toLong(),
                gameColumnHeightPx = field.gameColumnHeightPx.toLong(),
                isClosed = field.isClosed,
                id = field.id.toLong(),
            )
        }

    suspend fun getFieldById(id: Int): Field =
        withContext(Dispatchers.IO) { queries.getFieldById(id.toLong()).executeAsOne().toDomainModel() }

    suspend fun getUnfinishedField(): Field? =
        withContext(Dispatchers.IO) { queries.getUnfinishedField().executeAsOneOrNull()?.toDomainModel() }

    suspend fun getFieldCount(): Int = withContext(Dispatchers.IO) { queries.getFieldCount().executeAsOne().toInt() }
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
        gameColumnWidthPx = gameColumnWidthPx.toInt(),
        gameColumnHeightPx = gameColumnHeightPx.toInt(),
        isClosed = isClosed,
    )
