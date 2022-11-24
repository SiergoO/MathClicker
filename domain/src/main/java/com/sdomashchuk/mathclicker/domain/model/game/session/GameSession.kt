package com.sdomashchuk.mathclicker.domain.model.game.session

data class GameSession(
    val gameField: GameField,
    val targetParamsList: List<TargetParams>
)