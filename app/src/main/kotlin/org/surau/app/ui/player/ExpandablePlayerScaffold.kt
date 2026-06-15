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

package org.surau.app.ui.player

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Hosts the expandable player. A single [PlayerSheetState] offset drives every animation, exactly
 * like RetroMusic's `BottomSheetBehavior.onSlide`:
 * - the mini card cross-fades out over the first 20% of the drag,
 * - the full Flow player cross-fades in between 20%–40%.
 *
 * The bottom navigation bar (a sibling in the app shell) reads the same [PlayerSheetState.progress]
 * to slide down and fade as the sheet expands.
 *
 * @param peekPx the collapsed peek height (mini card + nav bar) in px — anchors the mini above the
 * nav bar.
 * @param miniHeight the mini card's own height (above the nav bar).
 */
@Composable
fun ExpandablePlayerScaffold(
    sheetState: PlayerSheetState,
    peekPx: Float,
    miniHeight: Dp,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerHeightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(containerHeightPx, peekPx) {
            sheetState.updateLayout(containerHeightPx, peekPx)
        }

        // Only compose the (heavy) Flow player once the sheet starts opening; only hit-test the mini
        // while it is the visible face. Both flip on a threshold via derivedStateOf, so the per-frame
        // offset changes never trigger recomposition here.
        val showExpanded by remember {
            derivedStateOf { sheetState.progress() > 0.001f || sheetState.isExpanding }
        }
        val showMini by remember {
            derivedStateOf { sheetState.progress() < 0.5f }
        }
        val nestedScrollConnection = remember(sheetState) { sheetState.nestedScrollConnection() }
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(sheetState.draggable)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, sheetState.offsetPx.roundToInt()) }
                .anchoredDraggable(
                    state = sheetState.draggable,
                    orientation = Orientation.Vertical,
                    flingBehavior = flingBehavior,
                )
                .nestedScroll(nestedScrollConnection),
        ) {
            if (showExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = ((sheetState.progress() - 0.2f) / 0.2f).coerceIn(0f, 1f)
                        },
                ) {
                    expandedContent()
                }
            }
            if (showMini) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(miniHeight)
                        .align(Alignment.TopStart)
                        .graphicsLayer {
                            alpha = (1f - sheetState.progress() / 0.2f).coerceIn(0f, 1f)
                        },
                ) {
                    collapsedContent()
                }
            }
        }
    }
}
