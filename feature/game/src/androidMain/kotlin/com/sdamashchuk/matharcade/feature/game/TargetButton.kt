package com.sdamashchuk.matharcade.feature.game

import android.util.Size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.core.ui.theme.Red200
import kotlin.math.roundToInt

@Composable
fun TargetButton(
    target: Target,
    gameColumnSize: Size,
    onTargetClicked: (id: Int) -> Unit,
) {
    val targetButtonYOffset = target.position * gameColumnSize.height
    if (target.isActive && targetButtonYOffset.dp > 0.dp) {
        Button(
            modifier =
                Modifier
                    .width((gameColumnSize.width * 0.8).dp)
                    .height((gameColumnSize.width * 0.8).dp)
                    .offset(0.dp, targetButtonYOffset.dp)
                    .clip(CircleShape),
            colors =
                ButtonDefaults.buttonColors(
                    backgroundColor = if (target.isProfitable) Red200 else Color.LightGray,
                ),
            onClick = { onTargetClicked.invoke(target.id) },
        ) {
            Text(text = target.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
}

// target.lifetimeMs is the fall's original total duration (see updateTargetPositioning); the
// remaining distance from the last saved fallenMs is animated over the matching remaining share
// of it, so a pause/resume restart can't shrink the fall's real duration (MC-27).
// internal, not private: it is otherwise a pure function of its inputs, and TargetButtonLogicTest
// exercises it directly rather than through Compose.
internal fun remainingFallDurationMs(target: Target): Int = (target.lifetimeMs - target.fallenMs).coerceAtLeast(0)

internal fun shouldReveal(
    targetButtonYOffset: Float,
    isVisible: Boolean,
    hasRevealedThisActivation: Boolean,
): Boolean = targetButtonYOffset > 0f && !isVisible && !hasRevealedThisActivation

internal fun shouldBreakout(
    targetButtonYOffset: Float,
    gameColumnHeightPx: Int,
    hasBrokenOutThisActivation: Boolean,
): Boolean =
    gameColumnHeightPx != 0 &&
        targetButtonYOffset.roundToInt() + 1 >= gameColumnHeightPx &&
        !hasBrokenOutThisActivation
