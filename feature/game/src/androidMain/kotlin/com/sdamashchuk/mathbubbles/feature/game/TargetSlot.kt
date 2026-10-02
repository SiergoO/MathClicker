package com.sdamashchuk.mathbubbles.feature.game

import android.util.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.feature.game.model.TargetScreenPosition

@Composable
internal fun TargetSlot(
    id: Int,
    target: Target,
    gameColumnSize: Size,
    isReady: Boolean,
    gameTimeMsProvider: () -> Long,
    realTimeMsProvider: () -> Long,
    icePickArmed: Boolean,
    isFading: Boolean,
    onTargetClicked: (id: Int) -> Unit,
    onTargetPositioned: (id: Int, position: TargetScreenPosition) -> Unit,
    onFadeFinished: () -> Unit,
) {
    val fadeProgress = remember(id) { Animatable(0f) }
    LaunchedEffect(isFading) {
        if (isFading) {
            fadeProgress.animateTo(1f, tween(DisappearFade.DURATION_MS))
            onFadeFinished()
        }
    }
    TargetButton(
        target = target,
        gameColumnSize = gameColumnSize,
        isReady = isReady,
        gameTimeMsProvider = gameTimeMsProvider,
        realTimeMsProvider = realTimeMsProvider,
        onTargetClicked = if (isFading) { _: Int -> } else onTargetClicked,
        onTargetPositioned = if (isFading) { _, _ -> } else onTargetPositioned,
        icePickArmed = icePickArmed,
        interactive = !isFading,
        fadeProvider = { fadeProgress.value },
    )
}
