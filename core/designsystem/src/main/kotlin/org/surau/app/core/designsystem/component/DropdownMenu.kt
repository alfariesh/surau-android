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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * A dropdown menu — HeroUI Native's `Menu` (popover presentation). Anchored below the composable
 * it's placed beside, in an `overlay` card (24dp rounded, 6dp/12dp padding, soft shadow). Provide
 * [SurauMenuItem]s as content.
 *
 * @param expanded Whether the menu is shown.
 * @param onDismissRequest Called on outside tap / dismissal.
 * @param modifier Modifier applied to the menu content.
 * @param content The menu items, in a [ColumnScope].
 */
@Composable
fun SurauDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SurauAnchoredPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        SurauOverlayCard(horizontalPadding = 6.dp, verticalPadding = 12.dp, modifier = modifier) {
            Column(modifier = Modifier.width(IntrinsicSize.Max), content = content)
        }
    }
}

/**
 * A menu row for [SurauDropdownMenu] — a label with optional [leading]/[trailing] content. The
 * `danger` variant tints the label with the danger colour.
 *
 * @param text The item label.
 * @param onClick Called when tapped.
 * @param modifier Modifier applied to the item.
 * @param leading Optional leading content (icon).
 * @param trailing Optional trailing content (icon / shortcut / check).
 * @param danger When `true`, the label uses the danger colour.
 * @param enabled When `false`, the item is dimmed and not clickable.
 */
@Composable
fun SurauMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalSurauColors.current
    val textColor = when {
        !enabled -> colors.muted
        danger -> colors.danger
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leading != null) {
            leading()
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        }
    }
}
