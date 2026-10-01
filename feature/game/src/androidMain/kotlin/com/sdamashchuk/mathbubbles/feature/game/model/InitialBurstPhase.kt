package com.sdamashchuk.mathbubbles.feature.game.model

fun initialBurstPhase(viaIcePick: Boolean): BurstPhase = if (viaIcePick) BurstPhase.Shatter else BurstPhase.Burst
