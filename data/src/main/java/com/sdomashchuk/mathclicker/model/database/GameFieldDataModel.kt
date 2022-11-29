package com.sdomashchuk.mathclicker.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sdomashchuk.mathclicker.domain.model.game.session.GameField
import com.sdomashchuk.mathclicker.util.toOperationSign
import com.sdomashchuk.mathclicker.util.toSymbol

@Entity(tableName = "gameField")
class GameFieldDataModel(
    @PrimaryKey(autoGenerate = true) var id: Int = 1,
    @ColumnInfo(name = "level") var level: Int = 1,
    @ColumnInfo(name = "score") var score: Int = 0,
    @ColumnInfo(name = "lifeCount") var lifeCount: Int = 3,
    @ColumnInfo(name = "bonusMultiplier") var bonusMultiplier: Int = 0,
    @ColumnInfo(name = "currentOperationSign") var currentOperationSign: String = "",
    @ColumnInfo(name = "currentOperationDigit") var currentOperationDigit: Int = 0,
    @ColumnInfo(name = "nextOperationSign") var nextOperationSign: String = "",
    @ColumnInfo(name = "nextOperationDigit") var nextOperationDigit: Int = 0,
    @ColumnInfo(name = "isClosed") var isClosed: Boolean = false
)

fun GameFieldDataModel.toDomainModel() = GameField(
    id = id,
    level = level,
    score = score,
    lifeCount = lifeCount,
    bonusMultiplier = bonusMultiplier,
    currentOperationSign = currentOperationSign.toOperationSign(),
    currentOperationDigit = currentOperationDigit,
    nextOperationSign = nextOperationSign.toOperationSign(),
    nextOperationDigit = nextOperationDigit,
    isClosed = isClosed
)

fun GameField.toDataModel() = GameFieldDataModel(
    id = id,
    level = level,
    score = score,
    lifeCount = lifeCount,
    bonusMultiplier = bonusMultiplier,
    currentOperationSign = currentOperationSign.toSymbol(),
    currentOperationDigit = currentOperationDigit,
    nextOperationSign = nextOperationSign.toSymbol(),
    nextOperationDigit = nextOperationDigit,
    isClosed = isClosed
)