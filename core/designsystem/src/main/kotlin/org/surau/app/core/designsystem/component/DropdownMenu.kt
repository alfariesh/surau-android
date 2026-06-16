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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * A dropdown menu — HeroUI Native's `Menu`, built on Material 3 [DropdownMenu] so it gets keyboard
 * navigation, menu accessibility semantics and collision-aware positioning for free, restyled to
 * the HeroUI `overlay` token (rounded container, soft shadow). Provide [SurauMenuItem]s as content.
 *
 * @param expanded Whether the menu is shown.
 * @param onDismissRequest Called on outside tap / dismissal.
 * @param modifier Modifier applied to the menu.
 * @param content The menu items, in a [ColumnScope].
 */
@Composable
fun SurauDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(SurauMenuDefaults.ContainerRadius),
        containerColor = LocalSurauColors.current.overlay,
        shadowElevation = SurauMenuDefaults.ShadowElevation,
        content = content,
    )
}

/**
 * A menu row for [SurauDropdownMenu], wrapping Material 3 [DropdownMenuItem]. The `danger` variant
 * tints the label with the danger colour.
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
    DropdownMenuItem(
        text = {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leading,
        trailingIcon = trailing,
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            textColor = if (danger) colors.danger else MaterialTheme.colorScheme.onSurface,
            leadingIconColor = if (danger) colors.danger else colors.muted,
            trailingIconColor = colors.muted,
            disabledTextColor = colors.muted,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    )
}

object SurauMenuDefaults {
    val ContainerRadius = 16.dp
    val ShadowElevation = 8.dp
}
