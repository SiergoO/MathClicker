package com.sdamashchuk.mathbubbles.feature.game

internal fun shardAngle(
    index: Int,
    shardCount: Int,
): Float = (2f * Math.PI.toFloat() / shardCount) * index
