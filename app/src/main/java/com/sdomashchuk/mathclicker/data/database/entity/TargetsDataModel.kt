package com.sdomashchuk.mathclicker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sdomashchuk.mathclicker.domain.model.game.session.Target

@Entity(tableName = "targets")
class TargetsDataModel(
    @PrimaryKey(autoGenerate = true) var id: Int = 1,
    @ColumnInfo(name = "relatedFieldId") var relatedFieldId: Int = 0,
    @ColumnInfo(name = "columnId") var columnId: Int = 1,
    @ColumnInfo(name = "value") var value: Int = 0,
    @ColumnInfo(name = "position") var position: Int = 0,
    @ColumnInfo(name = "appearanceDelayMs") var appearanceDelayMs: Int = 0,
    @ColumnInfo(name = "lifetimeMs") var lifetimeMs: Int = 0,
    @ColumnInfo(name = "isProfitable") var isProfitable: Boolean = true,
    @ColumnInfo(name = "isVisible") var isVisible: Boolean = false,
    @ColumnInfo(name = "isActive") var isActive: Boolean = false
)

fun TargetsDataModel.toDomainModel() = Target(
    id = id,
    relatedFieldId = relatedFieldId,
    columnId = columnId,
    value = value,
    position = position,
    appearanceDelayMs = appearanceDelayMs,
    lifetimeMs = lifetimeMs,
    isProfitable = isProfitable,
    isVisible = isVisible,
    isActive = isActive
)

fun Target.toDataModel() = TargetsDataModel(
    id = id,
    relatedFieldId = relatedFieldId,
    columnId = columnId,
    value = value,
    position = position,
    appearanceDelayMs = appearanceDelayMs,
    lifetimeMs = lifetimeMs,
    isProfitable = isProfitable,
    isVisible = isVisible,
    isActive = isActive
)