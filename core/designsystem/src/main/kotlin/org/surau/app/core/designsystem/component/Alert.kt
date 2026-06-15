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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/** Status family for [SurauAlert]. Tints the title + icon; the container stays `surface`. */
enum class SurauAlertVariant { Default, Accent, Success, Warning, Danger }

/**
 * An inline alert — HeroUI Native's `Alert`. A `surface` container (12dp padding, 24dp rounded,
 * soft shadow, no border) with a leading status icon, a title and an optional description. Per the
 * source, [variant] changes only the title + icon colour, not the background.
 *
 * @param title The alert title.
 * @param modifier Modifier applied to the alert.
 * @param description Optional secondary line (muted).
 * @param variant Status family controlling the title/icon colour.
 * @param icon Optional leading icon override; defaults to a per-variant status icon.
 * @param action Optional trailing content (e.g. a button or close affordance).
 */
@Composable
fun SurauAlert(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    variant: SurauAlertVariant = SurauAlertVariant.Default,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = LocalSurauColors.current
    val statusColor = variant.statusColor()
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .shadow(1.dp, shape)
            .clip(shape)
            .background(colors.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.padding(top = 3.5.dp)) {
            if (icon != null) {
                icon()
            } else {
                Icon(
                    imageVector = variant.statusIcon(),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor,
            )
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = colors.muted,
                )
            }
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
private fun SurauAlertVariant.statusColor(): Color {
    val colors = LocalSurauColors.current
    return when (this) {
        SurauAlertVariant.Default -> MaterialTheme.colorScheme.onSurface
        SurauAlertVariant.Accent -> colors.accent
        SurauAlertVariant.Success -> colors.success
        SurauAlertVariant.Warning -> colors.warning
        SurauAlertVariant.Danger -> colors.danger
    }
}

private fun SurauAlertVariant.statusIcon(): ImageVector = when (this) {
    SurauAlertVariant.Success -> Icons.Rounded.CheckCircle
    SurauAlertVariant.Warning -> Icons.Rounded.Warning
    SurauAlertVariant.Danger -> Icons.Rounded.Error
    else -> Icons.Rounded.Info
}
