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

package org.surau.app.core.network.model

import kotlinx.serialization.json.Json
import org.junit.Test
import org.surau.app.core.network.model.quran.AyahDto
import org.surau.app.core.network.model.quran.JuzDto
import org.surau.app.core.network.model.quran.SurahAudioManifestDto
import org.surau.app.core.network.model.quran.SurahDto
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the deserialization contract for the core M1 (surah/ayah/juz) and M2 (audio manifest)
 * endpoints against the real backend payload shapes. Uses the same [Json] configuration as the
 * production network module (`ignoreUnknownKeys = true`, nothing else) so a contract drift —
 * especially a backend `omitempty` field that the DTO declares as required — fails here instead of
 * silently breaking a foundational screen in production.
 */
class QuranDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun surahList_decodesEvenWhenOmitemptyNameKeysAreAbsent() {
        // The backend marshals name_arabic/name_latin as *string,omitempty, so a surah with a NULL
        // name omits the key entirely. The whole list must still decode (no MissingFieldException).
        val response = json.decodeFromString(
            PagedResponseDto.serializer(SurahDto.serializer()),
            """
            {
              "items": [
                { "surah_id": 1, "name_arabic": "الفاتحة", "name_latin": "Al-Fatihah",
                  "name_translation": "Pembukaan", "revelation_type": "makkah", "ayah_count": 7 },
                { "surah_id": 2, "ayah_count": 286, "revelation_type": "madinah" }
              ],
              "total": 2
            }
            """.trimIndent(),
        )

        assertEquals(2, response.total)
        val full = response.items.first()
        assertEquals("الفاتحة", full.nameArabic)
        assertEquals("Al-Fatihah", full.nameLatin)
        assertEquals(7, full.ayahCount)

        val missingNames = response.items[1]
        assertEquals(2, missingNames.surahId)
        assertNull(missingNames.nameArabic, "absent name_arabic must decode to null, not throw")
        assertNull(missingNames.nameLatin, "absent name_latin must decode to null, not throw")
    }

    @Test
    fun ayah_readerMinimalView_decodesWithoutTranslationOrTransliteration() {
        val response = json.decodeFromString(
            PagedResponseDto.serializer(AyahDto.serializer()),
            """
            {
              "items": [
                { "surah_id": 1, "ayah_number": 1, "ayah_key": "1:1",
                  "text_qpc_hafs": "بِسْمِ ٱللَّهِ", "page_number": 1, "juz_number": 1 }
              ],
              "total": 1
            }
            """.trimIndent(),
        )

        val ayah = response.items.single()
        assertEquals("1:1", ayah.ayahKey)
        assertEquals("بِسْمِ ٱللَّهِ", ayah.textQpcHafs)
        assertNull(ayah.translation)
        assertNull(ayah.transliteration)
        assertNull(ayah.textImlaeiSimple)
    }

    @Test
    fun ayah_fullView_decodesNestedTranslationAndTransliteration() {
        val ayah = json.decodeFromString(
            AyahDto.serializer(),
            """
            {
              "surah_id": 1, "ayah_number": 2, "ayah_key": "1:2",
              "text_qpc_hafs": "ٱلْحَمْدُ لِلَّهِ", "text_imlaei_simple": "الحمد لله",
              "search_text": "alhamdulillah", "page_number": 1, "juz_number": 1, "hizb_number": 1,
              "translation": { "source_id": "kemenag", "lang": "id", "text": "Segala puji bagi Allah" },
              "transliteration": { "source_id": "lat", "lang": "id", "text": "Al-hamdu lillah" }
            }
            """.trimIndent(),
        )

        assertEquals("1:2", ayah.ayahKey)
        assertEquals("الحمد لله", ayah.textImlaeiSimple)
        assertEquals("Segala puji bagi Allah", ayah.translation?.text)
        assertEquals("Al-hamdu lillah", ayah.transliteration?.text)
    }

    @Test
    fun juzList_decodesStartAndEndReferences() {
        val response = json.decodeFromString(
            PagedResponseDto.serializer(JuzDto.serializer()),
            """
            {
              "items": [
                { "kind": "juz", "number": 1, "ayah_count": 148,
                  "start": { "surah_id": 1, "ayah_number": 1, "ayah_key": "1:1", "surah_name": "Al-Fatihah" },
                  "end": { "surah_id": 2, "ayah_number": 141, "ayah_key": "2:141", "surah_name": "Al-Baqarah" } }
              ],
              "total": 1
            }
            """.trimIndent(),
        )

        val juz = response.items.single()
        assertEquals(1, juz.number)
        assertEquals("1:1", juz.start.ayahKey)
        assertEquals("2:141", juz.end.ayahKey)
    }

    @Test
    fun audioManifest_decodesTracksSegmentsAndMissingAyahKeys() {
        val manifest = json.decodeFromString(
            SurahAudioManifestDto.serializer(),
            """
            {
              "surah_id": 1,
              "recitation": { "id": "yasser", "display_name": "Yasser Al-Dosari",
                "reciter_name": "Yasser Al-Dosari", "mode": "surah", "is_default": true },
              "mode": "surah",
              "tracks": [
                { "recitation_id": "yasser", "track_type": "surah", "track_key": "1",
                  "surah_id": 1, "url": "https://cdn.surau.org/1.mp3", "duration_ms": 95000,
                  "duration_seconds": 95,
                  "segments": [
                    { "segment_index": 0, "ayah_key": "1:1", "timestamp_from_ms": 0, "timestamp_to_ms": 4000 },
                    { "segment_index": 1, "ayah_key": "1:2", "timestamp_from_ms": 4000, "timestamp_to_ms": 9000 }
                  ] }
              ],
              "missing_ayah_keys": ["1:7"]
            }
            """.trimIndent(),
        )

        assertEquals(1, manifest.surahId)
        assertEquals("surah", manifest.mode)
        val track = manifest.tracks.single()
        assertEquals("https://cdn.surau.org/1.mp3", track.url)
        assertEquals(95000L, track.durationMs)
        assertEquals(2, track.segments.size)
        assertEquals("1:2", track.segments[1].ayahKey)
        assertEquals(listOf("1:7"), manifest.missingAyahKeys)
    }

    @Test
    fun audioManifest_decodesWhenOptionalArraysAbsent() {
        // tracks / missing_ayah_keys are absent on a degraded manifest; defaults must apply.
        val manifest = json.decodeFromString(
            SurahAudioManifestDto.serializer(),
            """
            {
              "surah_id": 112,
              "recitation": { "id": "alafasy", "mode": "ayah" }
            }
            """.trimIndent(),
        )

        assertEquals(112, manifest.surahId)
        assertTrue(manifest.tracks.isEmpty())
        assertTrue(manifest.missingAyahKeys.isEmpty())
    }
}
