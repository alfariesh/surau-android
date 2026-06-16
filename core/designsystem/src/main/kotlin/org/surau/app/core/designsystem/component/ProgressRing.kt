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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * A circular progress ring — the idiomatic Compose equivalent of HeroUI's `ProgressChart` (a single
 * donut arc). Draws a `default`-coloured track and an `accent` arc sweeping clockwise from 12
 * o'clock, with rounded caps. Put a label (e.g. "57%" or "17/30 juz") in [content].
 *
 * @param progress Completion in `0f..1f`.
 * @param modifier Modifier applied to the ring.
 * @param ringSize Overall diameter.
 * @param strokeWidth Arc thickness.
 * @param trackColor Unfilled track colour (defaults to the `default` token).
 * @param progressColor Filled arc colour (defaults to the `accent` token).
 * @param content Centre content, laid out in a [BoxScope].
 */
@Composable
fun SurauProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 128.dp,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = Color.Unspecified,
    progressColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = LocalSurauColors.current
    val track = if (trackColor.isSpecified) trackColor else colors.default
    val arc = if (progressColor.isSpecified) progressColor else colors.accent
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "SurauProgressRing",
    )

    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0f) {
                drawArc(
                    color = arc,
                    startAngle = -90f,
                    sweepAngle = animated * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
