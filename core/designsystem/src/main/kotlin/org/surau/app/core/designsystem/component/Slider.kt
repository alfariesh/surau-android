/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.surau.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import kotlin.math.roundToInt

/**
 * A slider — HeroUI Native's `Slider`. A 20dp-tall `default` track with an `accent` fill and a
 * 28×20 `accent` thumb wrapping a white/`accent-foreground` knob that scales to 0.9 while dragged
 * (spring: damping 15, stiffness 200, mass 0.5). Supports dragging the thumb and tapping the track.
 *
 * @param value Current value (within [valueRange]).
 * @param onValueChange Called continuously as the value changes.
 * @param modifier Modifier applied to the slider.
 * @param valueRange The inclusive value range.
 * @param step Snap increment; `0` for continuous.
 * @param enabled When `false`, the slider is dimmed and not interactive.
 * @param onValueChangeFinished Called when an interaction (drag or tap) completes.
 */
@Composable
fun SurauSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    step: Float = 1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = LocalSurauColors.current
    val density = LocalDensity.current
    val min = valueRange.start
    val max = valueRange.endInclusive
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)

    var pressed by remember { mutableStateOf(false) }
    val knobScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f),
        label = "SurauSliderKnob",
    )

    var widthPx by remember { mutableFloatStateOf(0f) }
    val thumbPx = with(density) { SurauSliderDefaults.ThumbWidth.toPx() }

    fun emit(x: Float) {
        val effective = (widthPx - thumbPx).coerceAtLeast(1f)
        val fraction = ((x - thumbPx / 2f) / effective).coerceIn(0f, 1f)
        var next = min + fraction * (max - min)
        if (step > 0f) next = min + (((next - min) / step).roundToInt()) * step
        onChange(next.coerceIn(min, max))
    }

    val fraction = if (max > min) ((value - min) / (max - min)).coerceIn(0f, 1f) else 0f
    val effective = (widthPx - thumbPx).coerceAtLeast(0f)
    val thumbX = fraction * effective
    val fillWidth = with(density) { (thumbX + thumbPx).toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SurauSliderDefaults.TrackHeight)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    emit(offset.x)
                    onFinished?.invoke()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        pressed = true
                        emit(offset.x)
                    },
                    onDragEnd = {
                        pressed = false
                        onFinished?.invoke()
                    },
                    onDragCancel = { pressed = false },
                ) { change, _ -> emit(change.position.x) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track (unfilled)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SurauSliderDefaults.TrackHeight)
                .clip(RoundedCornerShape(SurauSliderDefaults.Radius))
                .background(colors.default),
        )
        // Active fill
        Box(
            modifier = Modifier
                .width(fillWidth)
                .height(SurauSliderDefaults.TrackHeight)
                .clip(RoundedCornerShape(SurauSliderDefaults.Radius))
                .background(colors.accent),
        )
        // Thumb (accent container + scaling knob)
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbX.roundToInt(), 0) }
                .size(width = SurauSliderDefaults.ThumbWidth, height = SurauSliderDefaults.ThumbHeight)
                .clip(RoundedCornerShape(SurauSliderDefaults.Radius))
                .background(colors.accent)
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = knobScale
                        scaleY = knobScale
                    }
                    .clip(RoundedCornerShape(SurauSliderDefaults.Radius))
                    .background(colors.onAccent),
            )
        }
    }
}

object SurauSliderDefaults {
    val TrackHeight = 20.dp
    val ThumbWidth = 28.dp
    val ThumbHeight = 20.dp
    val Radius: Dp = 12.dp
}
