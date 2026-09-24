package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.core.ui.theme.DarkGray
import com.sdamashchuk.matharcade.core.ui.theme.Red200

// MC-54: telegraphs the final 15% of a fall (Target.isTelegraphingBreakout) so a breakout is never
// a surprise. Deliberately not colour-coded - isProfitable already owns that channel below, and a
// second meaning on the same channel would make both unreadable - so the cue is a size pulse plus a
// ring, both legible independent of hue. Verified on device: see the MC-54 task report.
private const val TELEGRAPH_PULSE_SCALE = 1.15f
private const val TELEGRAPH_PULSE_MS = 300
private const val TELEGRAPH_BORDER_WIDTH_DP = 3

@Composable
fun TargetButton(
    target: Target,
    gameColumnSize: Size,
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
        Button(
            modifier =
                Modifier
                    .width((gameColumnSize.width * 0.8).dp)
                    .height((gameColumnSize.width * 0.8).dp)
                    .offset(0.dp, targetButtonYOffset.dp)
                    .scale(telegraphScale)
                    .clip(CircleShape),
            colors =
                ButtonDefaults.buttonColors(
                    backgroundColor = if (target.isProfitable) Red200 else Color.LightGray,
                ),
            border = if (target.isTelegraphingBreakout) BorderStroke(TELEGRAPH_BORDER_WIDTH_DP.dp, DarkGray) else null,
            onClick = { onTargetClicked.invoke(target.id) },
        ) {
            Text(text = target.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
}
