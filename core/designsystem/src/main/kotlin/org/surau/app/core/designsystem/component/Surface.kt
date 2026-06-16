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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/** Background level for [SurauSurface], mapping to the HeroUI surface tokens. */
enum class SurauSurfaceVariant { Default, Secondary, Tertiary, Background, Transparent }

/**
 * A themed container surface — HeroUI Native's `Surface`. Matches the source exactly: `rounded-3xl`
 * (24dp) corners, a baked-in `p-4` (16dp) content padding, `shadow-surface` (a subtle drop shadow),
 * and `overflow-hidden`. [variant] selects the background token; `Transparent` drops the shadow.
 *
 * @param modifier Modifier applied to the surface.
 * @param variant Which surface token to use as the background.
 * @param shape The surface shape (default 24dp rounded).
 * @param contentPadding Inner padding (HeroUI bakes in 16dp).
 * @param border Optional border stroke (HeroUI surfaces have none by default).
 * @param shadowElevation Drop-shadow elevation; ignored for [SurauSurfaceVariant.Transparent].
 * @param content Surface content.
 */
@Composable
fun SurauSurface(
    modifier: Modifier = Modifier,
    variant: SurauSurfaceVariant = SurauSurfaceVariant.Default,
    shape: Shape = SurauSurfaceDefaults.Shape,
    contentPadding: PaddingValues = SurauSurfaceDefaults.ContentPadding,
    border: BorderStroke? = null,
    shadowElevation: Dp = SurauSurfaceDefaults.ShadowElevation,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = variant.color(),
        contentColor = variant.contentColor(),
        border = border,
        shadowElevation = if (variant == SurauSurfaceVariant.Transparent) 0.dp else shadowElevation,
    ) {
        Box(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

/**
 * Clickable variant of [SurauSurface]. Adds ripple feedback and the appropriate accessibility role.
 *
 * @param onClick Called when the surface is clicked.
 * @param enabled When `false`, the surface is not clickable.
 */
@Composable
fun SurauSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: SurauSurfaceVariant = SurauSurfaceVariant.Default,
    shape: Shape = SurauSurfaceDefaults.Shape,
    contentPadding: PaddingValues = SurauSurfaceDefaults.ContentPadding,
    border: BorderStroke? = null,
    shadowElevation: Dp = SurauSurfaceDefaults.ShadowElevation,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = variant.color(),
        contentColor = variant.contentColor(),
        border = border,
        shadowElevation = if (variant == SurauSurfaceVariant.Transparent) 0.dp else shadowElevation,
    ) {
        Box(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

@Composable
private fun SurauSurfaceVariant.color(): Color {
    val colors = LocalSurauColors.current
    return when (this) {
        SurauSurfaceVariant.Default -> colors.surface
        SurauSurfaceVariant.Secondary -> colors.surfaceSecondary
        SurauSurfaceVariant.Tertiary -> colors.surfaceTertiary
        SurauSurfaceVariant.Background -> colors.background
        SurauSurfaceVariant.Transparent -> Color.Transparent
    }
}

@Composable
private fun SurauSurfaceVariant.contentColor(): Color = when (this) {
    SurauSurfaceVariant.Transparent -> LocalContentColor.current
    else -> MaterialTheme.colorScheme.onSurface
}

object SurauSurfaceDefaults {
    val Shape = RoundedCornerShape(24.dp)
    val ContentPadding = PaddingValues(16.dp)
    val ShadowElevation = 1.dp
}
