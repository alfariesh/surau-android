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

package org.surau.app.core.media

import androidx.media3.common.MediaItem

/**
 * Pure derivation of [PlayerUiState] from a player snapshot. The surah/ayah identity comes from the
 * current [MediaItem] (extras + `mediaId`). [PlayerUiState.positionMs] is tracked separately by a
 * polling loop, so it is preserved from [base] here.
 */
internal fun reducePlayerState(
    base: PlayerUiState,
    isPlaying: Boolean,
    currentItem: MediaItem?,
    durationMs: Long,
): PlayerUiState = base.copy(
    isPlaying = isPlaying,
    surahId = currentItem?.mediaMetadata?.extras?.getInt(KEY_SURAH_ID, 0)?.takeIf { it > 0 },
    currentAyahNumber = currentItem?.mediaId?.substringAfter(':', "")?.toIntOrNull(),
    recitationName = currentItem?.mediaMetadata?.artist?.toString(),
    durationMs = durationMs.coerceAtLeast(0L),
)
