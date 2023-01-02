package com.sdomashchuk.mathclicker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sdomashchuk.mathclicker.domain.model.game.session.Field
import com.sdomashchuk.mathclicker.model.OperationSign

@Entity(tableName = "field")
class FieldDataModel(
    @PrimaryKey(autoGenerate = true) var id: Int = 1,
    @ColumnInfo(name = "level") var level: Int = 1,
    @ColumnInfo(name = "score") var score: Int = 0,
    @ColumnInfo(name = "lifeCount") var lifeCount: Int = 3,
    @ColumnInfo(name = "bonusMultiplier") var bonusMultiplier: Int = 0,
    @ColumnInfo(name = "currentOperationSign") var currentOperationSign: String = "",
    @ColumnInfo(name = "currentOperationDigit") var currentOperationDigit: Int = 0,
    @ColumnInfo(name = "nextOperationSign") var nextOperationSign: String = "",
    @ColumnInfo(name = "nextOperationDigit") var nextOperationDigit: Int = 0,
    @ColumnInfo(name = "gameColumnWidthPx") var gameColumnWidthPx: Int = 0,
    @ColumnInfo(name = "gameColumnHeightPx") var gameColumnHeightPx: Int = 0,
    @ColumnInfo(name = "isClosed") var isClosed: Boolean = false
)

fun FieldDataModel.toDomainModel() = Field(
    id = id,
    level = level,
    score = score,
    lifeCount = lifeCount,
    bonusMultiplier = bonusMultiplier,
    currentOperationSign = OperationSign.values().first { it.sign == currentOperationSign },
    currentOperationDigit = currentOperationDigit,
    nextOperationSign = OperationSign.values().first { it.sign == nextOperationSign },
    nextOperationDigit = nextOperationDigit,
    gameColumnWidthPx = gameColumnWidthPx,
    gameColumnHeightPx = gameColumnHeightPx,
    isClosed = isClosed
)

fun Field.toDataModel() = FieldDataModel(
    id = id,
    level = level,
    score = score,
    lifeCount = lifeCount,
    bonusMultiplier = bonusMultiplier,
    currentOperationSign = currentOperationSign.sign,
    currentOperationDigit = currentOperationDigit,
    nextOperationSign = nextOperationSign.sign,
    nextOperationDigit = nextOperationDigit,
    gameColumnWidthPx = gameColumnWidthPx,
    gameColumnHeightPx = gameColumnHeightPx,
    isClosed = isClosed
)