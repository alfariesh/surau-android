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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * Pill switch matching HeroUI Native's `Switch` exactly: a **48×24** track whose background animates
 * `default → accent` (175ms, cubic-bezier 0.25/0.1/0.25/1), and a **28×20** capsule thumb that
 * springs across (`damping 120, stiffness 1600, mass 2` ≈ no bounce) while its fill animates
 * `white → accent-foreground`. The whole control scales to **0.96** while pressed (150ms).
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Called when toggled. Pass `null` to render a read-only switch.
 * @param modifier Modifier applied to the switch.
 * @param enabled When `false`, the switch is dimmed (opacity 0.5) and not toggleable.
 * @param thumbContent Optional content centred inside the thumb.
 */
@Composable
fun SurauSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: @Composable (() -> Unit)? = null,
) {
    val colors = LocalSurauColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val easing = remember { CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f) }

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "SurauSwitchScale",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.default,
        animationSpec = tween(durationMillis = 175, easing = easing),
        label = "SurauSwitchTrack",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.onAccent else Color.White,
        animationSpec = tween(durationMillis = 175, easing = easing),
        label = "SurauSwitchThumb",
    )
    // left: 2dp (off) -> trackWidth - thumbWidth - 2dp = 48 - 28 - 2 = 18dp (on).
    val thumbLeft by animateDpAsState(
        targetValue = if (checked) {
            SurauSwitchDefaults.TrackWidth - SurauSwitchDefaults.ThumbWidth -
                SurauSwitchDefaults.ThumbInset
        } else {
            SurauSwitchDefaults.ThumbInset
        },
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "SurauSwitchThumbLeft",
    )

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            indication = null,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .then(toggleModifier)
            .size(width = SurauSwitchDefaults.TrackWidth, height = SurauSwitchDefaults.TrackHeight)
            .clip(CircleShape)
            .background(trackColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbLeft.roundToPx(), 0) }
                .size(width = SurauSwitchDefaults.ThumbWidth, height = SurauSwitchDefaults.ThumbHeight)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center,
            content = { thumbContent?.invoke() },
        )
    }
}

object SurauSwitchDefaults {
    val TrackWidth = 48.dp
    val TrackHeight = 24.dp
    val ThumbWidth = 28.dp
    val ThumbHeight = 20.dp
    val ThumbInset = 2.dp
}
