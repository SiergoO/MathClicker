package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.component.model.GlassButtonStyle
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentDeep
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.core.ui.theme.LargeCornerRadius
import com.sdamashchuk.mathbubbles.core.ui.theme.Shapes

private val ButtonHeight = 56.dp
private const val FILL_ALPHA = 0.22f
private const val FILL_ALPHA_PRESSED = 0.32f
private const val HIGHLIGHT_ALPHA = 0.30f
private const val HIGHLIGHT_ALPHA_PRESSED = 0.44f
private const val HIGHLIGHT_HEIGHT_FRACTION = 0.5f
private const val RIM_WIDTH_DP = 1.5f
private const val PRESSED_SCALE = 0.97f

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedState = interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(if (pressedState.value) PRESSED_SCALE else 1f, label = "glassButtonScale")
    val style =
        remember {
            GlassButtonStyle(
                fillColor = Accent,
                rimTopColor = BubbleRimReady,
                rimBottomColor = AccentDeep,
                rimWidth = RIM_WIDTH_DP.dp,
                cornerRadius = LargeCornerRadius,
            )
        }
    val surface = remember(style) { Modifier.glassSurface(style, pressedState) }

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = Shapes.large,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
        elevation =
            ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .height(ButtonHeight)
                .graphicsLayer {
                    scaleX = scaleState.value
                    scaleY = scaleState.value
                }.clip(Shapes.large)
                .then(surface),
    ) {
        Text(text = text, style = MaterialTheme.typography.h2, color = Ink)
    }
}

private fun Modifier.glassSurface(
    style: GlassButtonStyle,
    pressedState: State<Boolean>,
) = drawWithCache {
    val rimWidthPx = style.rimWidth.toPx()
    val cornerRadiusPx = style.cornerRadius.toPx()
    val rimCornerRadius = CornerRadius((cornerRadiusPx - rimWidthPx / 2f).coerceAtLeast(0f))

    fun fillBrush(alpha: Float) =
        Brush.verticalGradient(
            0f to style.fillColor.copy(alpha = alpha * 1.1f),
            1f to style.fillColor.copy(alpha = alpha * 0.8f),
        )

    fun highlightBrush(alpha: Float) =
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = alpha),
            HIGHLIGHT_HEIGHT_FRACTION to Color.White.copy(alpha = 0f),
            1f to Color.White.copy(alpha = 0f),
        )

    val fillBrushNormal = fillBrush(FILL_ALPHA)
    val fillBrushPressed = fillBrush(FILL_ALPHA_PRESSED)
    val highlightBrushNormal = highlightBrush(HIGHLIGHT_ALPHA)
    val highlightBrushPressed = highlightBrush(HIGHLIGHT_ALPHA_PRESSED)
    val rimBrush =
        Brush.verticalGradient(
            0f to style.rimTopColor,
            1f to style.rimBottomColor,
        )

    onDrawBehind {
        val pressed = pressedState.value
        drawRect(if (pressed) fillBrushPressed else fillBrushNormal)
        drawRect(if (pressed) highlightBrushPressed else highlightBrushNormal)
        drawRoundRect(
            brush = rimBrush,
            topLeft = Offset(rimWidthPx / 2f, rimWidthPx / 2f),
            size = Size(size.width - rimWidthPx, size.height - rimWidthPx),
            cornerRadius = rimCornerRadius,
            style = Stroke(rimWidthPx),
        )
    }
}
