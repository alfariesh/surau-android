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

package org.surau.app.core.network.model.me

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuranProgressDto(
    @SerialName("surah_id") val surahId: Int,
    @SerialName("ayah_number") val ayahNumber: Int,
    @SerialName("ayah_key") val ayahKey: String,
    @SerialName("position_percent") val positionPercent: Double? = null,
    @SerialName("page_number") val pageNumber: Int? = null,
    @SerialName("juz_number") val juzNumber: Int? = null,
    @SerialName("observed_at") val observedAt: Instant? = null,
    @SerialName("updated_at") val updatedAt: Instant? = null,
)

@Serializable
data class PutQuranProgressRequestDto(
    @SerialName("ayah_key") val ayahKey: String,
    @SerialName("client_observed_at") val clientObservedAt: Instant? = null,
)

/**
 * A saved item ("bookmark"). Milestone 3 only creates `quran_ayah` items, but a [surahId]/[ayahKey]
 * may be absent for other item types, so they are nullable for defensive deserialization.
 */
@Serializable
data class SavedItemDto(
    @SerialName("id") val id: String,
    @SerialName("item_type") val itemType: String,
    @SerialName("surah_id") val surahId: Int? = null,
    @SerialName("ayah_key") val ayahKey: String? = null,
    @SerialName("label") val label: String? = null,
    @SerialName("note") val note: String? = null,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("created_at") val createdAt: Instant? = null,
    @SerialName("updated_at") val updatedAt: Instant? = null,
)

@Serializable
data class SavedItemsResponseDto(
    @SerialName("items") val items: List<SavedItemDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class SavedItemsTagsResponseDto(
    @SerialName("items") val items: List<String> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

/**
 * Body for `POST /me/saved-items` (upsert by target). Absent metadata fields never clear stored
 * values, so the nullable fields carry `null` defaults — under the app's `Json` (encodeDefaults
 * off) a `null` value equals its default and is omitted from the request entirely.
 */
@Serializable
data class CreateSavedItemRequestDto(
    @SerialName("item_type") val itemType: String,
    @SerialName("surah_id") val surahId: Int,
    @SerialName("ayah_key") val ayahKey: String,
    @SerialName("label") val label: String? = null,
    @SerialName("note") val note: String? = null,
    @SerialName("tags") val tags: List<String>? = null,
)

/**
 * Body for `PATCH /me/saved-items/{id}` (the only way to clear a field). An explicit `null` clears;
 * an absent field is left unchanged. The fields deliberately have **no defaults** so that, under the
 * app's `Json` (explicitNulls on), every declared field is always serialized — `null` is emitted as
 * `"note": null` rather than omitted. Milestone 3 edits only `note` + `tags`, so `label` is not
 * declared here and is never touched.
 */
@Serializable
data class PatchSavedItemRequestDto(
    @SerialName("note") val note: String?,
    @SerialName("tags") val tags: List<String>?,
)
