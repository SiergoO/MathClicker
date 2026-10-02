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
import androidx.compose.runtime.mutableStateMapOf
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
import com.sdamashchuk.mathbubbles.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.mathbubbles.core.model.Target
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
    divisionHintsEnabled: Boolean,
    realTimeMsProvider: () -> Long = { 0L },
) {
    var gameColumnSize by remember { mutableStateOf(Size(0, 0)) }
    val localDensity = LocalDensity.current

    // The clock reaches the draw and layout phases through this and is never read during
    // composition. GameViewModel.State is a new object on every tick because gameTimeMs moved, so
    // a single `gameState.value` read anywhere in this composable's body would have put the entire
    // board back through composition sixty times a second.
    val gameTimeMsProvider = remember { { gameState.value.field.gameTimeMs } }

    // Freeze's own envelope, not gated on timedBooster == FREEZE: a crossover into Rewind must
    // still ease this back to 0 instead of popping it the instant the booster swaps.
    val freezeIntensityProvider = remember { { gameState.value.effects.freezeTintIntensity } }

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
    val icePickArmed by remember { derivedStateOf { gameState.value.effects.icePickArmed } }

    val depthFactor = waterDepthFactor(level)
    val surface = sink(WaterSurface, depthFactor)
    val deep = sink(WaterDeep, depthFactor)

    // A plain map, not mutableStateMapOf. Nothing composable reads it - the burst below reads it
    // from a coroutine - so snapshot machinery bought nothing and cost a state record per write,
    // once per target per frame.
    val targetPositions = remember { HashMap<Int, TargetScreenPosition>() }
    val bursts = remember { mutableStateListOf<TargetZeroedBurst>() }

    val fadingTargets = remember { mutableStateMapOf<Int, Target>() }
    val fadeContext = remember { FadeContext() }
    fadeContext.sync(
        visibleTargets = visibleTargets,
        fadingTargets = fadingTargets,
        operationSign = operationSign,
        operationDigit = operationDigit,
        divisionHintsEnabled = divisionHintsEnabled,
        gameTimeMsProvider = gameTimeMsProvider,
        realTimeMsProvider = realTimeMsProvider,
    )

    LaunchedEffect(targetZeroedSignal) {
        val signal = targetZeroedSignal ?: return@LaunchedEffect
        val position = targetPositions[signal.targetId] ?: return@LaunchedEffect
        bursts.add(TargetZeroedBurst(signal.sequence, position, signal.viaIcePick))
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
                        // A flat overlay, not a re-rasterised strip: rebuilding the gradient bitmap
                        // at this rate would reintroduce the cost the comment above moved out.
                        val freezeIntensity = freezeIntensityProvider()
                        if (freezeIntensity > 0f) {
                            drawRect(AccentSoft.copy(alpha = FROZEN_TINT_ALPHA * freezeIntensity))
                        }
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
                    columnTargetIds(columnId, visibleTargets, fadingTargets).forEach { id ->
                        key(id) {
                            val isFading = fadingTargets.containsKey(id)
                            val target =
                                if (isFading) fadingTargets.getValue(id) else visibleTargets.first { it.id == id }
                            TargetSlot(
                                id = id,
                                target = target,
                                gameColumnSize = gameColumnSize,
                                isReady =
                                    if (isFading) {
                                        fadeContext.isReady.getOrElse(id) { false }
                                    } else {
                                        shouldShowDivisionHint(
                                            target,
                                            operationSign,
                                            operationDigit,
                                            divisionHintsEnabled,
                                        )
                                    },
                                gameTimeMsProvider =
                                    if (isFading) {
                                        { fadeContext.gameTimeMs.getOrElse(id) { gameTimeMsProvider() } }
                                    } else {
                                        gameTimeMsProvider
                                    },
                                realTimeMsProvider =
                                    if (isFading) {
                                        { fadeContext.realTimeMs.getOrElse(id) { realTimeMsProvider() } }
                                    } else {
                                        realTimeMsProvider
                                    },
                                icePickArmed = icePickArmed,
                                isFading = isFading,
                                onTargetClicked = onTargetClicked,
                                onTargetPositioned = { tid, position -> targetPositions[tid] = position },
                                onFadeFinished = {
                                    fadeContext.remove(id)
                                    fadingTargets.remove(id)
                                },
                            )
                        }
                    }
                }
            }
        }
        // Drawn last, on top of the columns: a burst marks where a target was, not another lane
        // occupant.
        TargetZeroedBurstsLayer(bursts)
    }
}

private fun columnTargetIds(
    columnId: Int,
    visibleTargets: List<Target>,
    fadingTargets: Map<Int, Target>,
): List<Int> {
    val liveIds = visibleTargets.filter { it.columnId == columnId }.map { it.id }
    val fadingIds = fadingTargets.filterValues { it.columnId == columnId }.keys
    return liveIds + (fadingIds - liveIds.toSet())
}

private const val FROZEN_TINT_ALPHA = 0.55f

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
