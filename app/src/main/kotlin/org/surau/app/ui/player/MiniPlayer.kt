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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.surau.app.R
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurahNameFontFamily
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.designsystem.theme.surahNameGlyphCode
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
    hazeState: HazeState,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = SurauTheme.colors.surface
    val border = SurauTheme.colors.border
    // Frosted-glass face: blur the content scrolling behind the card (Haze samples the
    // `hazeSource` in the app shell) and lay a translucent surface tint over it. Blur is real on
    // API 31+; older devices fall back to the flat tint. Square top edge (no corner rounding) with
    // a hairline divider drawn along the top to separate it from the scrolling content above.
    Box(
        modifier
            .fillMaxSize()
            .hazeEffect(state = hazeState) {
                backgroundColor = surface
                tints = listOf(HazeTint(surface.copy(alpha = 0.6f)))
                blurRadius = 24.dp
                noiseFactor = 0f
            }
            .drawBehind {
                val stroke = 0.5.dp.toPx()
                drawLine(
                    color = border,
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke,
                )
            }
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
        // Playback progress drives the wavy ring wrapped around the play/pause button below.
        val progress = if (state.durationMs > 0L) {
            (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Calligraphic surah-name ornament: the Surah Name font substitutes the `surahNNN`
            // ligature for the decorative glyph. Decorative only — the title row below already
            // announces the name, so it's hidden from TalkBack.
            state.surahId?.let { surahId ->
                Text(
                    text = surahNameGlyphCode(surahId),
                    fontFamily = SurahNameFontFamily,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    color = SurauTheme.colors.accent,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clearAndSetSemantics {},
                )
            }
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
            // Playback progress shows as an M3 wavy ring hugging the play/pause button (48dp
            // tonal button inside a 52dp wavy track, leaving a hairline gap). The wave depth is
            // scaled to half the M3 default for a subtler ripple.
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    color = SurauTheme.colors.accent,
                    trackColor = SurauTheme.colors.default,
                    amplitude = { fraction ->
                        WavyProgressIndicatorDefaults.indicatorAmplitude(fraction) * 0.5f
                    },
                    modifier = Modifier.size(52.dp),
                )
                FilledTonalIconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (state.isPlaying) SurauIcons.Pause else SurauIcons.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.player_pause else R.string.player_play,
                        ),
                    )
                }
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
