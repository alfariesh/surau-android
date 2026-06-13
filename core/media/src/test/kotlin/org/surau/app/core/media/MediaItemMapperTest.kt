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

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.surau.app.core.model.data.quran.AudioTrack
import org.surau.app.core.model.data.quran.SurahAudioManifest
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MediaItemMapperTest {

    @Test
    fun toMediaItems_skipsMissingAndNonAyahTracks_andMapsMetadata() {
        val manifest = SurahAudioManifest(
            surahId = 1,
            recitationId = "afasy",
            recitationName = "Mishari Rashid Al-Afasy",
            mode = "ayah",
            tracks = listOf(
                track(ayahKey = "1:1", ayahNumber = 1, url = "https://cdn/1.mp3"),
                track(ayahKey = "1:2", ayahNumber = 2, url = "https://cdn/2.mp3"),
                track(ayahKey = "1:3", ayahNumber = 3, url = ""), // missing audio → dropped
                track(ayahKey = "bismillah", ayahNumber = null, url = "https://cdn/b.mp3"), // non-ayah → dropped
            ),
            missingAyahKeys = listOf("1:3"),
        )

        val items = manifest.toMediaItems(surahName = "Al-Fatihah")

        assertEquals(listOf("1:1", "1:2"), items.map { it.mediaId })
        assertEquals("Al-Fatihah · 1", items[0].mediaMetadata.title.toString())
        assertEquals("Mishari Rashid Al-Afasy", items[0].mediaMetadata.artist.toString())
        assertEquals("https://cdn/1.mp3", items[0].localConfiguration?.uri.toString())
        assertEquals(1, items[0].mediaMetadata.extras?.getInt(KEY_SURAH_ID))
        assertEquals(2, items[1].mediaMetadata.extras?.getInt(KEY_AYAH_NUMBER))
    }

    private fun track(ayahKey: String, ayahNumber: Int?, url: String) = AudioTrack(
        ayahKey = ayahKey,
        ayahNumber = ayahNumber,
        url = url,
        durationMs = 3000L,
        segments = emptyList(),
    )
}
