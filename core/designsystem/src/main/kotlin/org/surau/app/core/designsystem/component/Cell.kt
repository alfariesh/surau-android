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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * A grouped list of row cells — HeroUI Native's `ListGroup`. A rounded `surface` card (24dp, soft
 * shadow, clipped) that stacks [SurauCell]s flush, with no dividers (matching the source — add your
 * own [androidx.compose.material3.HorizontalDivider] between cells if you want them).
 *
 * @param modifier Modifier applied to the group.
 * @param variant Surface background level (default/secondary/tertiary/transparent).
 * @param content The cells, stacked in a [ColumnScope].
 */
@Composable
fun SurauListGroup(
    modifier: Modifier = Modifier,
    variant: SurauSurfaceVariant = SurauSurfaceVariant.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    SurauSurface(
        modifier = modifier,
        variant = variant,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(content = content)
    }
}

/**
 * A single row "cell" for [SurauListGroup] — a 16dp-padded row with optional [leading] content, a
 * title + optional [description], and optional [trailing] content. When [onClick] is set and no
 * [trailing] is given, a muted chevron is shown.
 *
 * @param title The primary label.
 * @param modifier Modifier applied to the cell.
 * @param description Optional secondary line (muted).
 * @param leading Optional leading content (icon/avatar).
 * @param trailing Optional trailing content; overrides the default chevron.
 * @param onClick Optional click handler; makes the row pressable.
 * @param showChevron Whether to show the default trailing chevron (defaults to `onClick != null`).
 */
@Composable
fun SurauCell(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
) {
    val colors = LocalSurauColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = colors.muted,
                )
            }
        }
        when {
            trailing != null -> trailing()
            showChevron -> Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
