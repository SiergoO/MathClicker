package com.sdomashchuk.mathclicker.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams

@Entity(tableName = "targetParams")
class TargetParamsDataModel(
    @PrimaryKey(autoGenerate = true) var id: Int = 1,
    @ColumnInfo(name = "relatedGameFieldId") var relatedGameFieldId: Int = 0,
    @ColumnInfo(name = "columnId") var columnId: Int = 1,
    @ColumnInfo(name = "value") var value: Int = 0,
    @ColumnInfo(name = "position") var position: Int = 0,
    @ColumnInfo(name = "animationDelayMs") var animationDelayMs: Int = 0,
    @ColumnInfo(name = "animationDurationMs") var animationDurationMs: Int = 0,
    @ColumnInfo(name = "isVisible") var isVisible: Boolean = false,
    @ColumnInfo(name = "isAlive") var isAlive: Boolean = false
)

fun TargetParamsDataModel.toDomainModel() = TargetParams(
    id = id,
    relatedGameFieldId = relatedGameFieldId,
    columnId = columnId,
    value = value,
    position = position,
    animationDelayMs = animationDelayMs,
    animationDurationMs = animationDurationMs,
    isVisible = isVisible,
    isAlive = isAlive
)

fun TargetParams.toDataModel() = TargetParamsDataModel(
    id = id,
    relatedGameFieldId = relatedGameFieldId,
    columnId = columnId,
    value = value,
    position = position,
    animationDelayMs = animationDelayMs,
    animationDurationMs = animationDurationMs,
    isVisible = isVisible,
    isAlive = isAlive
)