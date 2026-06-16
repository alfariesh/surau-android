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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * A line chart — the idiomatic Compose port of HeroUI/RN `LineChart`, drawn on a [Canvas]. Plots a
 * single series as a smoothed (bezier) `accent` line over a transparent gradient fill, with dot
 * markers and optional x-axis labels. The y-axis is anchored at zero (fromZero). Best for trends
 * over many points (e.g. a 30-day reading trend) where a bar chart would be too dense.
 *
 * @param values The series values, in order.
 * @param modifier Modifier applied to the chart.
 * @param height Plot height (excluding labels).
 * @param lineColor Line/dot/fill colour (defaults to the `accent` token).
 * @param showGradient Whether to draw the gradient area under the line.
 * @param showDots Whether to draw a dot at each point.
 * @param bezier Whether to smooth the line with cubic beziers (vs straight segments).
 * @param labels Optional x-axis labels, rendered beneath and evenly distributed.
 */
@Composable
fun SurauLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    lineColor: Color = Color.Unspecified,
    showGradient: Boolean = true,
    showDots: Boolean = true,
    bezier: Boolean = true,
    labels: List<String> = emptyList(),
) {
    val colors = LocalSurauColors.current
    val line = if (lineColor.isSpecified) lineColor else colors.accent
    val dotCenter = MaterialTheme.colorScheme.surface
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val n = values.size
            if (n == 0) return@Canvas
            val inset = 4.dp.toPx() // keep dots/strokes inside bounds
            val w = size.width
            val h = size.height - inset * 2
            val stepX = if (n > 1) w / (n - 1) else 0f
            val points = values.mapIndexed { i, v ->
                Offset(x = i * stepX, y = inset + (h - (v / max) * h))
            }

            val linePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until n) {
                    val prev = points[i - 1]
                    val cur = points[i]
                    if (bezier) {
                        val midX = prev.x + (cur.x - prev.x) / 2f
                        cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                    } else {
                        lineTo(cur.x, cur.y)
                    }
                }
            }

            if (showGradient && n > 1) {
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(line.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = size.height,
                    ),
                )
            }
            drawPath(
                path = linePath,
                color = line,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            if (showDots) {
                points.forEach { p ->
                    drawCircle(color = line, radius = 4.dp.toPx(), center = p)
                    drawCircle(color = dotCenter, radius = 2.dp.toPx(), center = p)
                }
            }
        }
        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}
