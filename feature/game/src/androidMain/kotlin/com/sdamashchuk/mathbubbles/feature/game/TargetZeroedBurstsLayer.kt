package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.sdamashchuk.mathbubbles.feature.game.model.BurstPhase
import com.sdamashchuk.mathbubbles.feature.game.model.TargetZeroedBurst
import com.sdamashchuk.mathbubbles.feature.game.model.initialBurstPhase

@Composable
fun TargetZeroedBurstsLayer(bursts: SnapshotStateList<TargetZeroedBurst>) {
    bursts.forEach { burst ->
        key(burst.id) {
            var phase by remember(burst.id) { mutableStateOf(initialBurstPhase(burst.viaIcePick)) }
            when (phase) {
                BurstPhase.Shatter -> {
                    IcePickShatter(position = burst.position, onFinished = { phase = BurstPhase.Burst })
                }

                BurstPhase.Burst -> {
                    BurstRing(position = burst.position, onFinished = { bursts.remove(burst) })
                }
            }
        }
    }
}
