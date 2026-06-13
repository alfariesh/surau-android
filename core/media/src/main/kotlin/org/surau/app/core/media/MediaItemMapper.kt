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

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.surau.app.core.model.data.quran.SurahAudioManifest

internal const val KEY_SURAH_ID = "org.surau.app.media.SURAH_ID"
internal const val KEY_AYAH_NUMBER = "org.surau.app.media.AYAH_NUMBER"

/**
 * Maps a [SurahAudioManifest] to one [MediaItem] per ayah, in order. Tracks without an ayah number
 * (e.g. bismillah or whole-surah audio) or without a playable URL (e.g. a missing ayah) are
 * dropped. The `mediaId` is the ayah key (`"surah:ayah"`), and the surah/ayah are also stored in
 * the metadata extras so the controller can recover them on every item transition.
 */
internal fun SurahAudioManifest.toMediaItems(surahName: String): List<MediaItem> =
    tracks
        .filter { it.ayahNumber != null && it.url.isNotBlank() }
        .map { track ->
            val ayahNumber = requireNotNull(track.ayahNumber)
            val extras = Bundle().apply {
                putInt(KEY_SURAH_ID, surahId)
                putInt(KEY_AYAH_NUMBER, ayahNumber)
            }
            val metadata = MediaMetadata.Builder()
                .setTitle("$surahName · $ayahNumber")
                .setArtist(recitationName)
                .setExtras(extras)
                .build()
            MediaItem.Builder()
                .setMediaId(track.ayahKey)
                .setUri(track.url)
                .setMediaMetadata(metadata)
                .build()
        }
