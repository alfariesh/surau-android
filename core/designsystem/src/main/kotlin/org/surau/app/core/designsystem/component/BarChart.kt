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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import kotlin.math.min

/** A single bar for [SurauBarChart]. */
data class SurauBarEntry(val value: Float, val label: String = "")

/**
 * A vertical bar chart — the idiomatic Compose port of HeroUI/RN `BarChart`, drawn on a [Canvas].
 * Each bar has a faint full-height `default` track and an `accent` value bar with rounded corners;
 * optional x-axis labels render beneath, centred under their bars.
 *
 * @param entries The bars, in order.
 * @param modifier Modifier applied to the chart.
 * @param height Plot height (excluding labels).
 * @param barColor Value-bar colour (defaults to the `accent` token).
 * @param maxValue Y scale max; defaults to the largest entry value (min 1).
 * @param showTrack Whether to draw the faint full-height track behind each bar.
 * @param showLabels Whether to render the x-axis labels row.
 */
@Composable
fun SurauBarChart(
    entries: List<SurauBarEntry>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    barColor: Color = Color.Unspecified,
    maxValue: Float? = null,
    showTrack: Boolean = true,
    showLabels: Boolean = true,
) {
    val colors = LocalSurauColors.current
    val bar = if (barColor.isSpecified) barColor else colors.accent
    val track = colors.default
    val max = (maxValue ?: entries.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val n = entries.size
            if (n == 0) return@Canvas
            val gap = (size.width * 0.18f / n).coerceIn(2.dp.toPx(), 12.dp.toPx())
            val barWidth = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
            val radius = CornerRadius(min(barWidth / 2f, 8.dp.toPx()))
            entries.forEachIndexed { index, entry ->
                val x = index * (barWidth + gap)
                if (showTrack) {
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = radius,
                    )
                }
                val barHeight = (entry.value / max * size.height).coerceIn(0f, size.height)
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = bar,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius,
                    )
                }
            }
        }
        if (showLabels && entries.any { it.label.isNotEmpty() }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                entries.forEach { entry ->
                    Text(
                        text = entry.label,
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
