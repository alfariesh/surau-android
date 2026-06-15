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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.surau.app.core.designsystem.theme.LocalSurauColors

/**
 * Positions a popup below its anchor, horizontally centred, with a 9dp gap (HeroUI's default
 * `offset`), flipping above the anchor when there isn't room, and clamping within 12dp screen insets.
 */
private class SurauPopoverPositionProvider(
    private val offsetPx: Int,
    private val insetPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - insetPx).coerceAtLeast(insetPx)
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(insetPx, maxX)
        val below = anchorBounds.bottom + offsetPx
        val y = if (below + popupContentSize.height > windowSize.height - insetPx) {
            val above = anchorBounds.top - popupContentSize.height - offsetPx
            if (above >= insetPx) above else below
        } else {
            below
        }
        return IntOffset(x, y.coerceAtLeast(insetPx))
    }
}

/**
 * Shared anchored-popup scaffold for [SurauPopover], [SurauSelect] and [SurauDropdownMenu]: a
 * Compose [Popup] positioned below the composable it's placed next to, with HeroUI's open motion
 * (fade 0.25→1 + scale 0.97→1 over 200ms, reverse over 150ms). The caller styles the surface card.
 */
@Composable
internal fun SurauAnchoredPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val offsetPx = with(density) { 9.dp.roundToPx() }
    val insetPx = with(density) { 12.dp.roundToPx() }
    val provider = remember(offsetPx, insetPx) { SurauPopoverPositionProvider(offsetPx, insetPx) }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        val visibleState = remember { MutableTransitionState(false) }.apply { targetState = true }
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = modifier,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.97f, animationSpec = tween(200)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.97f, animationSpec = tween(150)),
            content = { content() },
        )
    }
}

/** The HeroUI `overlay`-styled popup card: rounded-3xl (24dp), soft shadow, [contentPaddingValues]. */
@Composable
internal fun SurauOverlayCard(
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .shadow(8.dp, shape)
            .clip(shape)
            .background(LocalSurauColors.current.overlay)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        content()
    }
}

/**
 * A popover — HeroUI Native's `Popover`. Anchored below the composable it's placed beside, in an
 * `overlay`-coloured card (24dp rounded, 16dp/12dp padding, soft shadow). Dismisses on outside tap.
 *
 * @param expanded Whether the popover is shown.
 * @param onDismissRequest Called when the user taps outside / requests dismissal.
 * @param modifier Modifier applied to the popover content.
 * @param content The popover body.
 */
@Composable
fun SurauPopover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SurauAnchoredPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        SurauOverlayCard(horizontalPadding = 16.dp, verticalPadding = 12.dp, modifier = modifier) {
            content()
        }
    }
}
