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
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.surau.app.core.designsystem.theme.LocalSurauColors
import kotlin.math.abs

/**
 * A scrolling "wheel" picker: a vertical list that snaps to the item under the centre band, with
 * off-centre rows progressively dimmed and shrunk for depth — the idiomatic Compose equivalent of
 * the HeroUI Native wheel picker (built on [LazyColumn] + snapping fling, no external library).
 *
 * State is hoisted: the caller owns [selectedIndex] and reacts to [onSelectedIndexChange], which
 * fires for every item that settles under the centre band (including while the user is dragging).
 *
 * @param items The values to choose from.
 * @param selectedIndex Index into [items] of the centred (selected) value.
 * @param onSelectedIndexChange Called with the index that has scrolled under the centre band.
 * @param modifier Modifier applied to the picker.
 * @param visibleCount How many rows are visible at once. Must be odd so a row sits dead centre.
 * @param itemHeight Height of a single row.
 * @param enabled When `false`, scrolling is disabled and the picker is dimmed.
 * @param hapticFeedback When `true`, emits a small haptic tick as the centred value changes.
 * @param showIndicator Draws the centre selection band and the top/bottom fade. Set `false` when a
 *        parent (e.g. [SurauWheelTimePicker]) draws a shared band/fade across several columns.
 * @param label Maps an item to its display string.
 */
@Composable
fun <T> SurauWheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = SurauWheelPickerDefaults.VISIBLE_COUNT,
    itemHeight: Dp = SurauWheelPickerDefaults.ItemHeight,
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    showIndicator: Boolean = true,
    label: (T) -> String = { it.toString() },
) {
    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        if (showIndicator) {
            SurauWheelSelectionBand(itemHeight = itemHeight)
        }
        WheelColumn(
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            visibleCount = visibleCount,
            itemHeight = itemHeight,
            enabled = enabled,
            hapticFeedback = hapticFeedback,
            label = label,
        )
        if (showIndicator) {
            SurauWheelEdgeFade(visibleCount = visibleCount, itemHeight = itemHeight)
        }
    }
}

/** The rounded `default`-coloured band marking the selected (centre) row (HeroUI `rounded-2xl`). */
@Composable
internal fun SurauWheelSelectionBand(itemHeight: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(LocalSurauColors.current.default),
    )
}

/**
 * A non-interactive top/bottom fade towards the surface colour. Drawn as an overlay with no pointer
 * input so it never intercepts the wheel's scroll gestures.
 */
@Composable
internal fun SurauWheelEdgeFade(
    visibleCount: Int,
    itemHeight: Dp = SurauWheelPickerDefaults.ItemHeight,
    modifier: Modifier = Modifier,
) {
    val fade = MaterialTheme.colorScheme.surface
    // HeroUI fades one row at each end (mask half-height = itemHeight).
    val edgeFraction = 1f / visibleCount
    Box(
        modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to fade,
                        edgeFraction to Color.Transparent,
                        1f - edgeFraction to Color.Transparent,
                        1f to fade,
                    ),
                )
            },
    )
}

/**
 * The bare scrolling column with the per-row depth effect. Shared by [SurauWheelPicker] and the
 * time-picker columns so the snap/scale/dim behaviour stays identical.
 */
@Composable
internal fun <T> WheelColumn(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = SurauWheelPickerDefaults.VISIBLE_COUNT,
    itemHeight: Dp = SurauWheelPickerDefaults.ItemHeight,
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    label: (T) -> String = { it.toString() },
) {
    require(visibleCount % 2 == 1) { "visibleCount must be odd, was $visibleCount" }
    val edge = visibleCount / 2
    val accentColor = LocalSurauColors.current.accent
    val baseColor = LocalContentColor.current
    val haptic = LocalHapticFeedback.current

    val safeSelected = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val state = rememberLazyListState(initialFirstVisibleItemIndex = safeSelected)
    // The report effect below is keyed only on (state, items.size), so a column whose size is
    // constant across selections (e.g. day-within-month, hours, minutes) never restarts it. Read the
    // live selected index through rememberUpdatedState so the long-lived collector — and the
    // derivedStateOf fallback — compare against the current value, not the one captured at launch.
    val currentSelected by rememberUpdatedState(safeSelected)

    // The index whose centre is nearest the viewport centre.
    val centredIndex by remember(state, items) {
        derivedStateOf {
            val info = state.layoutInfo
            val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2f) - viewportCentre) }
                ?.index
                ?: currentSelected
        }
    }

    // Report user-driven changes (also drives the haptic tick).
    val onChange = rememberUpdatedState(onSelectedIndexChange)
    LaunchedEffect(state, items.size) {
        snapshotFlow { centredIndex }
            .distinctUntilChanged()
            .filter { it in items.indices }
            .collect { idx ->
                if (idx != currentSelected) {
                    if (hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onChange.value(idx)
                }
            }
    }
    // Sync external value changes back into the scroll position.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && selectedIndex != centredIndex) {
            state.animateScrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        modifier = modifier
            .height(itemHeight * visibleCount)
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        state = state,
        contentPadding = PaddingValues(vertical = itemHeight * edge),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        userScrollEnabled = enabled,
    ) {
        items(count = items.size) { index ->
            val isCentred = index == centredIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                        val viewportCentre =
                            (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2f
                        val itemCentre = info?.let { it.offset + it.size / 2f } ?: viewportCentre
                        val distanceRows = abs(itemCentre - viewportCentre) / itemHeight.toPx()
                        // HeroUI interpolates opacity 0.5->1 and scale 0.85->1 over two rows.
                        val proximity = 1f - (distanceRows / 2f).coerceIn(0f, 1f)
                        alpha = lerp(0.5f, 1f, proximity)
                        val s = lerp(0.85f, 1f, proximity)
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(items[index]),
                    style = textStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = if (isCentred) accentColor else baseColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

object SurauWheelPickerDefaults {
    val ItemHeight = 44.dp
    const val VISIBLE_COUNT = 5
}
