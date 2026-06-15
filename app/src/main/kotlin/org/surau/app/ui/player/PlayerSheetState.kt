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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/** Drag anchors for the expandable player sheet. */
enum class PlayerAnchor { Hidden, Collapsed, Expanded }

/** Snap animation used when a drag/fling settles to an anchor. */
private val SnapSpec: AnimationSpec<Float> = spring(stiffness = 400f)

/**
 * State for the expandable player. Wraps an [AnchoredDraggableState] whose offset (px, measured from
 * the top of the overlay) drives every animation: `Expanded` sits at `0`, `Collapsed` peeks at the
 * bottom, and `Hidden` is off-screen. [progress] maps the live offset to `0f` (collapsed) .. `1f`
 * (expanded) — the single value the mini/full cross-fade and the nav-bar slide read each frame,
 * exactly like RetroMusic's `BottomSheetBehavior.onSlide`.
 */
@Stable
class PlayerSheetState(
    val draggable: AnchoredDraggableState<PlayerAnchor>,
) {
    /**
     * Spring driving programmatic expand/collapse and the drag-hand-off settle. Defaults to a gentle
     * non-bouncy spring; the host overwrites it with the Material 3 [androidx.compose.material3.
     * MotionScheme] spatial spec so the sheet shares the app's motion language.
     */
    var motionSpec: AnimationSpec<Float> = SnapSpec

    /** Offset of the [PlayerAnchor.Collapsed] anchor (px); `NaN` until the layout is measured. */
    private var collapsedOffsetPx by mutableFloatStateOf(Float.NaN)

    /** Container height (px); the [PlayerAnchor.Hidden] anchor sits here. */
    private var containerHeightPx by mutableFloatStateOf(Float.NaN)

    /** Recomputes anchors from a fresh measurement, preserving the current target. */
    fun updateLayout(containerHeightPx: Float, peekPx: Float) {
        if (containerHeightPx <= 0f) return
        this.containerHeightPx = containerHeightPx
        collapsedOffsetPx = (containerHeightPx - peekPx).coerceAtLeast(0f)
        draggable.updateAnchors(
            DraggableAnchors {
                PlayerAnchor.Expanded at 0f
                PlayerAnchor.Collapsed at collapsedOffsetPx
                PlayerAnchor.Hidden at containerHeightPx
            },
            draggable.targetValue,
        )
    }

    /** True once anchors have been measured at least once. */
    val isLaidOut: Boolean get() = !collapsedOffsetPx.isNaN()

    /** Live sheet offset in px, falling back to the collapsed/hidden position before first drag. */
    val offsetPx: Float
        get() {
            val raw = draggable.offset
            if (!raw.isNaN()) return raw
            return if (collapsedOffsetPx.isNaN()) 0f else collapsedOffsetPx
        }

    /** `0f` collapsed → `1f` expanded; `0f` until measured. Read inside draw-phase lambdas. */
    fun progress(): Float {
        val collapsed = collapsedOffsetPx
        if (collapsed.isNaN() || collapsed <= 0f) return 0f
        val raw = draggable.offset
        if (raw.isNaN()) return 0f
        // Expanded anchor is 0, so the span is exactly [0, collapsed].
        return ((collapsed - raw) / collapsed).coerceIn(0f, 1f)
    }

    /** Whether the sheet is settling toward (or rests at) the fully-expanded state. */
    val isExpanding: Boolean get() = draggable.targetValue == PlayerAnchor.Expanded

    suspend fun expand() = draggable.animateTo(PlayerAnchor.Expanded, motionSpec)

    suspend fun collapse() = draggable.animateTo(PlayerAnchor.Collapsed, motionSpec)

    suspend fun show() = draggable.animateTo(PlayerAnchor.Collapsed, motionSpec)

    suspend fun hide() = draggable.animateTo(PlayerAnchor.Hidden, motionSpec)

    /**
     * Bridges the expanded Flow list's scroll to the sheet drag, so dragging the content down once
     * it is scrolled to the top collapses the sheet (and dragging up finishes expanding it) — the
     * RetroMusic hand-off. The [PlayerAnchor.Expanded] anchor sits at offset `0`, so any positive
     * offset means there is room to collapse.
     */
    fun nestedScrollConnection(): NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            return if (delta < 0f && source == NestedScrollSource.UserInput) {
                Offset(0f, draggable.dispatchRawDelta(delta))
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset = if (source == NestedScrollSource.UserInput) {
            Offset(0f, draggable.dispatchRawDelta(available.y))
        } else {
            Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return if (available.y < 0f && draggable.offset > 0f) {
                draggable.settle(motionSpec)
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            draggable.settle(motionSpec)
            return available
        }
    }
}

@Composable
fun rememberPlayerSheetState(): PlayerSheetState {
    val draggable = remember { AnchoredDraggableState(PlayerAnchor.Hidden) }
    return remember { PlayerSheetState(draggable) }
}
