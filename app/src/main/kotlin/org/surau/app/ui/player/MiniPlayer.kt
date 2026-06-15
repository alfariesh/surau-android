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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.surau.app.R
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.media.PlayerUiState

/** Height of the mini-player card (excludes the nav bar it sits above). */
val MiniPlayerHeight = 64.dp

private const val SKIP_DRAG_THRESHOLD_PX = 120f

/**
 * The collapsed face of the expandable player: a sticky card that sits directly above the bottom
 * navigation bar while a recitation is loaded. Tap it (or its title) to expand to the full Flow
 * player; fling horizontally to skip to the previous/next ayah.
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onExpand)
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                        onDragEnd = {
                            if (total <= -SKIP_DRAG_THRESHOLD_PX) {
                                onNext()
                            } else if (total >= SKIP_DRAG_THRESHOLD_PX) {
                                onPrevious()
                            }
                        },
                    )
                },
        ) {
            // Thin progress line along the very top edge of the card.
            val progress = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.surahName.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = buildList {
                        state.currentAyahNumber?.let {
                            add(stringResource(R.string.player_ayah, it))
                        }
                        state.recitationName?.let { add(it) }
                    }.joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = SurauIcons.SkipPrevious,
                        contentDescription = stringResource(R.string.player_previous),
                    )
                }
                FilledTonalIconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (state.isPlaying) SurauIcons.Pause else SurauIcons.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.player_pause else R.string.player_play,
                        ),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = SurauIcons.SkipNext,
                        contentDescription = stringResource(R.string.player_next),
                    )
                }
            }
        }
    }
}
