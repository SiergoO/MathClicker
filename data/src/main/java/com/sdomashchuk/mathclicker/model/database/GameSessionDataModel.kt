package com.sdomashchuk.mathclicker.model.database

import androidx.room.Embedded
import androidx.room.Relation
import com.sdomashchuk.mathclicker.domain.model.game.session.GameSession

data class GameSessionDataModel(
    @Embedded val gameField: GameFieldDataModel,
    @Relation(
        parentColumn = "id",
        entityColumn = "relatedGameFieldId"
    )
    val targetParams: List<TargetParamsDataModel>?
)

fun GameSessionDataModel.toDomainModel() = GameSession(
    gameField = gameField.toDomainModel(),
    targetParamsList = targetParams?.map { it.toDomainModel() }?: listOf()
)