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

package org.surau.app.core.model.data.quran

/**
 * A surah's audio recitation manifest from `GET /quran/surahs/{id}/audio`. Track URLs point at the
 * CDN and may expire, so this is never persisted — it is fetched fresh per playback session.
 */
data class SurahAudioManifest(
    val surahId: Int,
    val recitationId: String,
    val recitationName: String,
    val mode: String,
    val tracks: List<AudioTrack>,
    val missingAyahKeys: List<String>,
)

/**
 * A single playable unit. In `mode == "ayah"` (the common case) one track is one ayah, identified
 * by [ayahKey] (e.g. `"73:4"`).
 */
data class AudioTrack(
    val ayahKey: String,
    val ayahNumber: Int?,
    val url: String,
    val durationMs: Long?,
    val segments: List<AudioSegment>,
)

/**
 * Ayah-level timing within a [AudioTrack] — used to highlight individual ayahs inside a
 * surah-level audio file.
 */
data class AudioSegment(
    val segmentIndex: Int,
    val ayahKey: String,
    val timestampFromMs: Long,
    val timestampToMs: Long,
    val durationMs: Long?,
)
