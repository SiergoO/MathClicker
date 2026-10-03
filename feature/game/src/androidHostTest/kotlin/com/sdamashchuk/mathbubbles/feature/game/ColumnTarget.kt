package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.Target

fun columnTarget(
    id: Int,
    column: Int,
    value: Int,
    finishesAtMs: Long = 100_000L,
): Target =
    Target(
        id = id,
        relatedFieldId = 1,
        columnId = column,
        value = value,
        appearsAtMs = 0,
        finishesAtMs = finishesAtMs,
    )
