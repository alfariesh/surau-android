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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.surau.app.core.designsystem.theme.LocalSurauColors
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Status colour family for a [SurauToast]. Tints only the title (the container stays `surface`). */
enum class SurauToastVariant { Default, Accent, Success, Warning, Danger }

/** Where [SurauToastHost] anchors its toast. */
enum class SurauToastPlacement { Top, Bottom }

/** How long a toast stays before auto-dismissing. */
enum class SurauToastDuration(val millis: Long) {
    Short(4_000),
    Long(8_000),
    Indefinite(kotlin.Long.MAX_VALUE),
}

/** Outcome of a [SurauToastState.show] call. */
enum class SurauToastResult { ActionPerformed, Dismissed }

/** The data backing a visible toast, with controls to resolve it. */
@Stable
interface SurauToastData {
    val message: String
    val description: String?
    val variant: SurauToastVariant
    val actionLabel: String?
    val duration: SurauToastDuration

    fun performAction()
    fun dismiss()
}

/**
 * Host state for toasts, modelled on `SnackbarHostState`: [show] suspends until the toast is
 * dismissed (or its action is tapped), serialising concurrent calls so only one toast is visible.
 */
@Stable
class SurauToastState {
    private val mutex = Mutex()

    var currentData: SurauToastData? by mutableStateOf(null)
        private set

    /**
     * Shows a toast and suspends until it is dismissed or its action is performed. A second call
     * waits for the current toast to resolve first.
     */
    suspend fun show(
        message: String,
        description: String? = null,
        variant: SurauToastVariant = SurauToastVariant.Default,
        actionLabel: String? = null,
        duration: SurauToastDuration = SurauToastDuration.Short,
    ): SurauToastResult = mutex.withLock {
        try {
            suspendCancellableCoroutine { continuation ->
                currentData = SurauToastDataImpl(
                    message = message,
                    description = description,
                    variant = variant,
                    actionLabel = actionLabel,
                    duration = duration,
                    continuation = continuation,
                )
            }
        } finally {
            currentData = null
        }
    }
}

private class SurauToastDataImpl(
    override val message: String,
    override val description: String?,
    override val variant: SurauToastVariant,
    override val actionLabel: String?,
    override val duration: SurauToastDuration,
    private val continuation: CancellableContinuation<SurauToastResult>,
) : SurauToastData {
    override fun performAction() {
        if (continuation.isActive) continuation.resume(SurauToastResult.ActionPerformed)
    }

    override fun dismiss() {
        if (continuation.isActive) continuation.resume(SurauToastResult.Dismissed)
    }
}

/** Remembers a [SurauToastState] for the lifetime of the composition. */
@Composable
fun rememberSurauToastState(): SurauToastState = remember { SurauToastState() }

/**
 * Renders the current toast from [state], anchored to [placement]. Matches HeroUI's motion: it
 * slides in from the placement edge on a heavy spring (no fade-in), and exits over 150ms with a
 * downward/upward slide + slight scale-down. Auto-dismisses after the toast's duration and supports
 * swipe-to-dismiss (>50dp in the placement direction). Place it in a `Box` over your screen content.
 *
 * @param state The host state. Call [SurauToastState.show] (from a coroutine) to display a toast.
 * @param modifier Modifier applied to the host container.
 * @param placement Which edge the toast slides from / anchors to (default top).
 * @param toast Slot for rendering the toast; defaults to [SurauToast].
 */
@Composable
fun SurauToastHost(
    state: SurauToastState,
    modifier: Modifier = Modifier,
    placement: SurauToastPlacement = SurauToastPlacement.Top,
    toast: @Composable (SurauToastData) -> Unit = { SurauToast(it) },
) {
    val data = state.currentData
    LaunchedEffect(data) {
        if (data != null && data.duration != SurauToastDuration.Indefinite) {
            delay(data.duration.millis)
            data.dismiss()
        }
    }
    val fromTop = placement == SurauToastPlacement.Top
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = if (fromTop) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = data != null,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow),
            ) { full -> if (fromTop) -full else full },
            exit = slideOutVertically(tween(150)) { full -> if (fromTop) -full else full } +
                scaleOut(tween(150), targetScale = 0.97f) +
                fadeOut(tween(150)),
        ) {
            // Keep the last data around so the exit transition still has something to render.
            val lastData = remember { mutableStateOf(data) }
            if (data != null) lastData.value = data
            lastData.value?.let { shown ->
                key(shown) {
                    ToastSwipeBox(data = shown, placement = placement) { toast(shown) }
                }
            }
        }
    }
}

@Composable
private fun ToastSwipeBox(
    data: SurauToastData,
    placement: SurauToastPlacement,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { offsetY.snapTo(offsetY.value + delta) }
                },
                onDragStopped = {
                    val threshold = with(density) { 50.dp.toPx() }
                    val dismiss = if (placement == SurauToastPlacement.Top) {
                        offsetY.value < -threshold
                    } else {
                        offsetY.value > threshold
                    }
                    if (dismiss) data.dismiss() else offsetY.animateTo(0f)
                },
            ),
    ) {
        content()
    }
}

/** Renders a [SurauToastData] (delegates to the explicit-parameter overload). */
@Composable
fun SurauToast(data: SurauToastData, modifier: Modifier = Modifier) {
    SurauToast(
        message = data.message,
        modifier = modifier,
        description = data.description,
        variant = data.variant,
        actionLabel = data.actionLabel,
        onAction = { data.performAction() },
        onClose = { data.dismiss() },
    )
}

/**
 * The toast card — HeroUI Native's `Toast`. The container is always `surface` (24dp rounded, 16dp
 * padding, soft shadow); [variant] tints only the title. There is **no** default leading icon (pass
 * one via [icon]); the close button shows only when [onClose] is provided.
 *
 * @param message The primary line.
 * @param modifier Modifier applied to the card.
 * @param description Optional secondary line.
 * @param variant Title colour family.
 * @param icon Optional leading content (HeroUI has no default icon).
 * @param actionLabel Optional action text; shown only when [onAction] is provided.
 * @param onAction Called when the action is tapped.
 * @param onClose Called when the close button is tapped; hidden when `null`.
 */
@Composable
fun SurauToast(
    message: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    variant: SurauToastVariant = SurauToastVariant.Default,
    icon: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val colors = LocalSurauColors.current
    val titleColor = variant.titleColor()
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(colors.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) {
            Box { icon() }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                maxLines = 2,
            )
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = colors.muted,
                    maxLines = 3,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (onClose != null) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Tutup",
                tint = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClose)
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun SurauToastVariant.titleColor(): Color {
    val colors = LocalSurauColors.current
    return when (this) {
        SurauToastVariant.Default -> MaterialTheme.colorScheme.onSurface
        SurauToastVariant.Accent -> colors.accent
        SurauToastVariant.Success -> colors.success
        SurauToastVariant.Warning -> colors.warning
        SurauToastVariant.Danger -> colors.danger
    }
}
