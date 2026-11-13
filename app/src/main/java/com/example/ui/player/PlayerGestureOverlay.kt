package com.example.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import kotlin.math.abs

private enum class DragAxis { NONE, VERTICAL_LEFT, VERTICAL_RIGHT, HORIZONTAL_SEEK, PINCH_ZOOM }

@Composable
fun PlayerGestureOverlay(
    isLocked: Boolean,
    durationMs: Long,
    currentPositionMs: Long,
    currentBrightnessFraction: () -> Float,
    currentVolumeFraction: () -> Float,
    onSetBrightness: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onSeekPreview: (targetMs: Long, deltaMs: Long) -> Unit,
    onCommitSeek: (targetMs: Long) -> Unit,
    onClearGestureHud: () -> Unit,
    onSingleTap: () -> Unit,
    onLockedTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onZoomPan: (scaleDelta: Float, panX: Float, panY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var startBrightness by remember { mutableFloatStateOf(0.5f) }
    var startVolume by remember { mutableFloatStateOf(0.5f) }
    var startSeekPosMs by remember { mutableLongStateOf(0L) }
    var pendingSeekTargetMs by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_gesture_layer")
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = {
                        if (isLocked) onLockedTap() else onSingleTap()
                    },
                    onDoubleTap = { offset ->
                        if (isLocked) {
                            onLockedTap()
                        } else {
                            val ratio = offset.x / size.width.coerceAtLeast(1)
                            when {
                                ratio < 0.34f -> onDoubleTapLeft()
                                ratio > 0.66f -> onDoubleTapRight()
                                else -> onDoubleTapCenter()
                            }
                        }
                    }
                )
            }
            .pointerInput(isLocked, durationMs) {
                if (isLocked) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val touchSlop = viewConfiguration.touchSlop
                    var dragAxis = DragAxis.NONE
                    var cumulativeDx = 0f
                    var cumulativeDy = 0f
                    val startX = down.position.x
                    val isLeftSide = startX < size.width / 2f

                    startBrightness = currentBrightnessFraction()
                    startVolume = currentVolumeFraction()
                    startSeekPosMs = currentPositionMs
                    pendingSeekTargetMs = currentPositionMs

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.isEmpty()) break

                        if (pressedChanges.size >= 2) {
                            dragAxis = DragAxis.PINCH_ZOOM
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (zoomChange != 1f || panChange.x != 0f || panChange.y != 0f) {
                                onZoomPan(zoomChange, panChange.x, panChange.y)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                            continue
                        }

                        if (dragAxis == DragAxis.PINCH_ZOOM) {
                            continue
                        }

                        val change = pressedChanges.first()
                        val delta = change.position - change.previousPosition
                        cumulativeDx += delta.x
                        cumulativeDy += delta.y

                        if (dragAxis == DragAxis.NONE) {
                            if (abs(cumulativeDx) > touchSlop || abs(cumulativeDy) > touchSlop) {
                                dragAxis = if (abs(cumulativeDx) > abs(cumulativeDy)) {
                                    DragAxis.HORIZONTAL_SEEK
                                } else if (isLeftSide) {
                                    DragAxis.VERTICAL_LEFT
                                } else {
                                    DragAxis.VERTICAL_RIGHT
                                }
                            }
                        }

                        when (dragAxis) {
                            DragAxis.VERTICAL_LEFT -> {
                                change.consume()
                                val heightPx = size.height.coerceAtLeast(1).toFloat()
                                val fractionDelta = (-cumulativeDy) / (heightPx * 0.65f)
                                onSetBrightness((startBrightness + fractionDelta).coerceIn(0.02f, 1f))
                            }
                            DragAxis.VERTICAL_RIGHT -> {
                                change.consume()
                                val heightPx = size.height.coerceAtLeast(1).toFloat()
                                val fractionDelta = (-cumulativeDy) / (heightPx * 0.65f)
                                onSetVolume((startVolume + fractionDelta).coerceIn(0f, 1f))
                            }
                            DragAxis.HORIZONTAL_SEEK -> {
                                change.consume()
                                val widthPx = size.width.coerceAtLeast(1).toFloat()
                                // Full horizontal swipe maps to max 120 seconds (or full duration if shorter)
                                val spanMs = if (durationMs in 1L..120_000L) durationMs else 120_000L
                                val seekDeltaMs = ((cumulativeDx / widthPx) * spanMs).toLong()
                                val target = if (durationMs > 0L) {
                                    (startSeekPosMs + seekDeltaMs).coerceIn(0L, durationMs)
                                } else {
                                    (startSeekPosMs + seekDeltaMs).coerceAtLeast(0L)
                                }
                                pendingSeekTargetMs = target
                                onSeekPreview(target, target - startSeekPosMs)
                            }
                            DragAxis.NONE, DragAxis.PINCH_ZOOM -> Unit
                        }
                    }

                    if (dragAxis == DragAxis.HORIZONTAL_SEEK) {
                        onCommitSeek(pendingSeekTargetMs)
                        onClearGestureHud()
                    } else if (dragAxis != DragAxis.NONE) {
                        onClearGestureHud()
                    }
                }
            }
    )
}
