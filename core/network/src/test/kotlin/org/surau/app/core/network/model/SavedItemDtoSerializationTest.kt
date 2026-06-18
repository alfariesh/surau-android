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

import org.junit.Test
import org.surau.app.core.network.di.NetworkModule
import org.surau.app.core.network.model.me.CreateSavedItemRequestDto
import org.surau.app.core.network.model.me.PatchSavedItemRequestDto
import org.surau.app.core.network.model.me.SavedItemsResponseDto
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks down the trickiest saved-items contract detail: POST must NOT clear absent metadata
 * (omit null), while PATCH MUST clear via an explicit null. Both rely on the app's network [Json]
 * configuration (`encodeDefaults` off, `explicitNulls` on); this uses the REAL production instance
 * so the test breaks if that config ever drifts.
 */
class SavedItemDtoSerializationTest {

    private val json = NetworkModule.providesNetworkJson()

    @Test
    fun createRequest_omitsNullMetadata_soUpsertNeverClears() {
        val body = CreateSavedItemRequestDto(itemType = "quran_ayah", surahId = 73, ayahKey = "73:4")
        val encoded = json.encodeToString(CreateSavedItemRequestDto.serializer(), body)

        assertTrue(encoded.contains("\"item_type\":\"quran_ayah\""))
        assertTrue(encoded.contains("\"surah_id\":73"))
        assertTrue(encoded.contains("\"ayah_key\":\"73:4\""))
        assertFalse(encoded.contains("note"), "note must be omitted, not null")
        assertFalse(encoded.contains("tags"), "tags must be omitted, not null")
        assertFalse(encoded.contains("label"))
    }

    @Test
    fun createRequest_includesProvidedMetadata() {
        val body = CreateSavedItemRequestDto(
            itemType = "quran_ayah",
            surahId = 73,
            ayahKey = "73:4",
            note = "renungan",
            tags = listOf("tafsir"),
        )
        val encoded = json.encodeToString(CreateSavedItemRequestDto.serializer(), body)

        assertTrue(encoded.contains("\"note\":\"renungan\""))
        assertTrue(encoded.contains("\"tags\":[\"tafsir\"]"))
    }

    @Test
    fun patchRequest_emitsExplicitNull_soFieldClears() {
        val body = PatchSavedItemRequestDto(note = null, tags = listOf("tafsir"))
        val encoded = json.encodeToString(PatchSavedItemRequestDto.serializer(), body)

        assertEquals("""{"note":null,"tags":["tafsir"]}""", encoded)
    }

    @Test
    fun patchRequest_emptyTags_clearsTags() {
        val body = PatchSavedItemRequestDto(note = "keep", tags = emptyList())
        val encoded = json.encodeToString(PatchSavedItemRequestDto.serializer(), body)

        assertEquals("""{"note":"keep","tags":[]}""", encoded)
    }

    @Test
    fun savedItemsResponse_parsesListEnvelope() {
        val response = json.decodeFromString(
            SavedItemsResponseDto.serializer(),
            """
            {
              "items": [
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "item_type": "quran_ayah",
                  "surah_id": 73,
                  "ayah_key": "73:4",
                  "note": "renungan",
                  "tags": ["tafsir", "favorit"],
                  "created_at": "2026-01-01T00:00:00Z",
                  "updated_at": "2026-01-02T00:00:00Z"
                }
              ],
              "total": 1
            }
            """.trimIndent(),
        )

        assertEquals(1, response.total)
        val item = response.items.single()
        assertEquals("73:4", item.ayahKey)
        assertEquals(listOf("tafsir", "favorit"), item.tags)
    }
}
