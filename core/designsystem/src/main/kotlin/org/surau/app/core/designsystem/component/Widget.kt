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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * A dashboard "widget" card — HeroUI Native Pro's `Widget`, ported with slot lambdas. A neutral
 * `surface-secondary` shell (16dp rounded, 6dp padding) wraps an optional header (title +
 * description on the left, [legend] on the right), an elevated `surface` content card (12dp
 * rounded, 16dp padding, soft shadow), and an optional footer. Header/footer use the source's exact
 * asymmetric padding (header 2dp top / 6dp bottom, footer mirrored).
 *
 * @param modifier Modifier applied to the widget shell.
 * @param shellColor Background of the outer shell (and header/footer band). Defaults to
 *        `surface-secondary`.
 * @param containerVariant Surface token for the elevated content card. Defaults to `surface`.
 * @param title Optional heading. Rendered with the uniform ~20sp medium widget-title style.
 * @param description Optional sub-heading. Rendered 14sp muted, beneath the title.
 * @param legend Optional trailing header content, laid out in a [RowScope]. Pair with
 *        [SurauWidgetLegendItem].
 * @param footer Optional footer content beneath the content card.
 * @param content The widget body, laid out in a [ColumnScope] inside the elevated content card.
 */
@Composable
fun SurauWidget(
    modifier: Modifier = Modifier,
    shellColor: Color = LocalSurauColors.current.surfaceSecondary,
    containerVariant: SurauSurfaceVariant = SurauSurfaceVariant.Default,
    title: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    legend: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalSurauColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(shellColor)
            .padding(6.dp),
    ) {
        if (title != null || description != null || legend != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    if (title != null) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                        ) {
                            // Uniform widget heading: ~20sp (medium), description 14sp.
                            ProvideTextStyle(
                                MaterialTheme.typography.titleSmall.copy(fontSize = 20.sp, lineHeight = 26.sp),
                                title,
                            )
                        }
                    }
                    if (description != null) {
                        CompositionLocalProvider(LocalContentColor provides colors.muted) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium, description)
                        }
                    }
                }
                if (legend != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = legend,
                    )
                }
            }
        }

        SurauSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = containerVariant,
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(content = content)
        }

        if (footer != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                footer()
            }
        }
    }
}

/**
 * A legend entry for a [SurauWidget] header — a 10dp coloured dot followed by a 12sp muted label.
 *
 * @param label The series label.
 * @param color The dot colour (typically a chart series colour).
 * @param modifier Modifier applied to the legend item.
 */
@Composable
fun SurauWidgetLegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalSurauColors.current.muted,
            maxLines = 1,
        )
    }
}
