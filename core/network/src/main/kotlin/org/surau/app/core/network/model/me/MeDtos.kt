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
