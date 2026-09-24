package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.core.ui.theme.DarkGray
import com.sdamashchuk.matharcade.core.ui.theme.Red200
import com.sdamashchuk.matharcade.core.ui.theme.Red500

// MC-54: telegraphs the final 15% of a fall (Target.isTelegraphingBreakout) so a breakout is never
// a surprise. Deliberately not colour-coded - isProfitable already owns that channel below, and a
// second meaning on the same channel would make both unreadable - so the cue is a size pulse plus a
// ring, both legible independent of hue. Verified on device: see the MC-54 task report.
private const val TELEGRAPH_PULSE_SCALE = 1.15f
private const val TELEGRAPH_PULSE_MS = 300
private const val TELEGRAPH_BORDER_WIDTH_DP = 3

// MC-61: readiness hint. isReady is already the policy's own decision (see shouldShowReadinessHint)
// - this composable only animates the colour, Red200 to Red500 over ~200ms rather than an instant
// swap, which on a 4-26 target board reads as a flicker whenever the divisor changes and half the
// board flips at once. Grey (isProfitable) still wins outright: the lerp only ever runs inside the
// profitable branch below, so an unprofitable target is never mid-fade toward red.
private const val READINESS_TRANSITION_MS = 200

@Composable
fun TargetButton(
    target: Target,
    gameColumnSize: Size,
    isReady: Boolean,
    onTargetClicked: (id: Int) -> Unit,
) {
    val targetButtonYOffset = target.position * gameColumnSize.height
    if (target.isActive && targetButtonYOffset.dp > 0.dp) {
        val telegraphTransition = rememberInfiniteTransition(label = "breakoutTelegraph")
        val telegraphScale by
            telegraphTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (target.isTelegraphingBreakout) TELEGRAPH_PULSE_SCALE else 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(TELEGRAPH_PULSE_MS, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "breakoutTelegraphScale",
            )
        val readinessFraction by
            animateFloatAsState(
                targetValue = if (isReady) 1f else 0f,
                animationSpec = tween(READINESS_TRANSITION_MS),
                label = "readinessFraction",
            )
        val backgroundColor =
            if (target.isProfitable) lerp(Red200, Red500, readinessFraction) else Color.LightGray
        Button(
            modifier =
                Modifier
                    .width((gameColumnSize.width * 0.8).dp)
                    .height((gameColumnSize.width * 0.8).dp)
                    .offset(0.dp, targetButtonYOffset.dp)
                    .scale(telegraphScale)
                    .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor),
            border = if (target.isTelegraphingBreakout) BorderStroke(TELEGRAPH_BORDER_WIDTH_DP.dp, DarkGray) else null,
            onClick = { onTargetClicked.invoke(target.id) },
        ) {
            Text(text = target.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
}
