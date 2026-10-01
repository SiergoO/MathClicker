package com.sdamashchuk.mathbubbles.feature.game

import android.util.Size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentSoft
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterDeep
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterMote
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterSurface
import com.sdamashchuk.mathbubbles.feature.game.model.TargetScreenPosition
import com.sdamashchuk.mathbubbles.feature.game.model.TargetZeroedBurst
import com.sdamashchuk.mathbubbles.feature.game.model.TargetZeroedSignal
import androidx.compose.ui.geometry.Size as GeometrySize

private const val PLAY_AREA_HEIGHT_FRACTION = 0.75f

// Four falling-target columns over the water. The reference art has no lane separators at
// all: the bubbles carry the columns on their own, and a hairline grid over water reads as a
// spreadsheet.
@Composable
fun PlayArea(
    gameState: State<GameViewModel.State>,
    onTargetClicked: (id: Int) -> Unit,
    targetZeroedSignal: TargetZeroedSignal?,
    readinessHintsEnabled: Boolean,
) {
    var gameColumnSize by remember { mutableStateOf(Size(0, 0)) }
    val localDensity = LocalDensity.current

    // The clock reaches the draw and layout phases through this and is never read during
    // composition. GameViewModel.State is a new object on every tick because gameTimeMs moved, so
    // a single `gameState.value` read anywhere in this composable's body would have put the entire
    // board back through composition sixty times a second.
    val gameTimeMsProvider = remember { { gameState.value.field.gameTimeMs } }

    // Each of these recomputes per tick and is cheap, but only notifies a reader when its own value
    // actually changes - which is what keeps this composable out of the per-frame path.
    val level by remember { derivedStateOf { gameState.value.field.level } }
    val operationSign by remember { derivedStateOf { gameState.value.field.currentOperationSign } }
    val operationDigit by remember { derivedStateOf { gameState.value.field.currentOperationDigit } }
    val visibleTargets by
        remember {
            derivedStateOf {
                val gameTimeMs = gameState.value.field.gameTimeMs
                gameState.value.targetList.filter { it.isActive && it.isVisible(gameTimeMs) }
            }
        }
    val isFrozen by remember { derivedStateOf { gameState.value.effects.timedBooster == Booster.FREEZE } }
    val icePickArmed by remember { derivedStateOf { gameState.value.effects.icePickArmed } }

    val depthFactor = waterDepthFactor(level)
    val surface = tintIfFrozen(sink(WaterSurface, depthFactor), isFrozen)
    val deep = tintIfFrozen(sink(WaterDeep, depthFactor), isFrozen)

    // A plain map, not mutableStateMapOf. Nothing composable reads it - the burst below reads it
    // from a coroutine - so snapshot machinery bought nothing and cost a state record per write,
    // once per target per frame.
    val targetPositions = remember { HashMap<Int, TargetScreenPosition>() }
    val bursts = remember { mutableStateListOf<TargetZeroedBurst>() }
    LaunchedEffect(targetZeroedSignal) {
        val signal = targetZeroedSignal ?: return@LaunchedEffect
        val position = targetPositions.remove(signal.targetId) ?: return@LaunchedEffect
        bursts.add(TargetZeroedBurst(signal.sequence, position))
    }
    LaunchedEffect(visibleTargets) {
        targetPositions.keys.retainAll(visibleTargets.mapTo(mutableSetOf()) { it.id })
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(PLAY_AREA_HEIGHT_FRACTION)
                .drawWithCache {
                    // The gradient is rasterised once into a one-pixel-wide strip and stretched
                    // across the field, not evaluated per pixel per frame. Measured on this
                    // emulator, whose GL is SwiftShader - a software rasteriser - a full-screen
                    // vertical gradient cost about 7ms of every frame; the strip is 4KB and each
                    // output row samples a single texel.
                    val strip = verticalGradientStrip(surface, deep, size.height.toInt())
                    val fieldSize = IntSize(size.width.toInt(), size.height.toInt())
                    onDrawBehind {
                        drawImage(
                            image = strip,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(1, strip.height),
                            dstOffset = IntOffset.Zero,
                            dstSize = fieldSize,
                        )
                        drawAmbientBubbles(gameTimeMsProvider())
                    }
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        val measuredWidthDp =
                            with(localDensity) {
                                coordinates.size.width
                                    .toDp()
                                    .value
                                    .toInt()
                            }
                        val gameColumnWidth = calculateGameColumnWidth(measuredWidthDp)
                        val gameColumnHeight =
                            with(localDensity) {
                                coordinates.size.height
                                    .toDp()
                                    .value
                                    .toInt() - (gameColumnWidth * 0.8).toInt()
                            }
                        gameColumnSize = Size(gameColumnWidth, gameColumnHeight)
                    },
        ) {
            repeat(GAME_COLUMN_COUNT) { columnId ->
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(gameColumnSize.width.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    visibleTargets.filter { target -> target.columnId == columnId }.forEach { target ->
                        key(target.id) {
                            TargetButton(
                                target,
                                gameColumnSize,
                                isReady =
                                    shouldShowReadinessHint(
                                        target,
                                        operationSign,
                                        operationDigit,
                                        hintsEnabled = readinessHintsEnabled,
                                    ),
                                gameTimeMsProvider = gameTimeMsProvider,
                                onTargetClicked = onTargetClicked,
                                onTargetPositioned = { id, position -> targetPositions[id] = position },
                                icePickArmed = icePickArmed,
                            )
                        }
                    }
                }
            }
        }
        // Drawn last, on top of the columns: a burst marks where a target was, not another lane
        // occupant.
        bursts.forEach { burst ->
            key(burst.id) {
                BurstRing(position = burst.position, onFinished = { bursts.remove(burst) })
            }
        }
    }
}

private const val FROZEN_TINT_FRACTION = 0.25f

private fun tintIfFrozen(
    color: Color,
    isFrozen: Boolean,
): Color =
    if (isFrozen) {
        Color(
            red = color.red + (AccentSoft.red - color.red) * FROZEN_TINT_FRACTION,
            green = color.green + (AccentSoft.green - color.green) * FROZEN_TINT_FRACTION,
            blue = color.blue + (AccentSoft.blue - color.blue) * FROZEN_TINT_FRACTION,
            alpha = color.alpha,
        )
    } else {
        color
    }

// An even split. The subtraction this used to carry reserved width for 1dp lane separators; MC-81
// moved those into an overlay and MC-84 removed them, so reserving for them left the row 3dp short
// and every column fractionally left of where it belongs.
internal fun calculateGameColumnWidth(measuredWidthDp: Int): Int = measuredWidthDp / GAME_COLUMN_COUNT

// Darkens one water stop toward the abyss. Multiplying every channel by the same factor
// keeps the hue and only removes light, which is what water actually does with depth.
private fun sink(
    color: Color,
    factor: Float,
): Color = Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)

private fun verticalGradientStrip(
    top: Color,
    bottom: Color,
    heightPx: Int,
): ImageBitmap {
    val height = heightPx.coerceAtLeast(1)
    val bitmap = ImageBitmap(1, height)
    CanvasDrawScope().draw(
        density =
            androidx.compose.ui.unit
                .Density(1f),
        layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = GeometrySize(1f, height.toFloat()),
    ) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
    }
    return bitmap
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientBubbles(gameTimeMs: Long) {
    repeat(AMBIENT_BUBBLE_COUNT) { index ->
        val progress = ambientBubbleProgress(index, gameTimeMs)
        val alpha = ambientBubbleAlpha(index, progress)
        if (alpha <= 0f) return@repeat
        val radius = size.minDimension * ambientBubbleRadiusFraction(index)
        drawCircle(
            color = WaterMote.copy(alpha = alpha),
            radius = radius,
            center =
                Offset(
                    x = size.width * ambientBubbleCenterXFraction(index, gameTimeMs),
                    y = size.height * (1f - progress),
                ),
        )
    }
}
