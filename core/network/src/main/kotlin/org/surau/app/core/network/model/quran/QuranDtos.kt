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

package org.surau.app.core.network.model.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SurahDto(
    @SerialName("surah_id") val surahId: Int,
    @SerialName("name_arabic") val nameArabic: String,
    @SerialName("name_latin") val nameLatin: String,
    @SerialName("name_translation") val nameTranslation: String = "",
    @SerialName("revelation_type") val revelationType: String = "makkiyah",
    @SerialName("ayah_count") val ayahCount: Int,
    @SerialName("info") val info: String? = null,
)

@Serializable
data class AyahTranslationDto(
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("lang") val lang: String? = null,
    @SerialName("text") val text: String = "",
)

@Serializable
data class AyahTransliterationDto(
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("lang") val lang: String? = null,
    @SerialName("text") val text: String = "",
)

@Serializable
data class AyahDto(
    @SerialName("surah_id") val surahId: Int,
    @SerialName("ayah_number") val ayahNumber: Int,
    @SerialName("ayah_key") val ayahKey: String,
    @SerialName("text_qpc_hafs") val textQpcHafs: String = "",
    @SerialName("page_number") val pageNumber: Int? = null,
    @SerialName("juz_number") val juzNumber: Int? = null,
    @SerialName("hizb_number") val hizbNumber: Int? = null,
    @SerialName("translation") val translation: AyahTranslationDto? = null,
    @SerialName("transliteration") val transliteration: AyahTransliterationDto? = null,
)

@Serializable
data class AyahReferenceDto(
    @SerialName("surah_id") val surahId: Int,
    @SerialName("ayah_number") val ayahNumber: Int,
    @SerialName("ayah_key") val ayahKey: String = "",
    @SerialName("surah_name") val surahName: String = "",
)

@Serializable
data class JuzDto(
    @SerialName("kind") val kind: String = "juz",
    @SerialName("number") val number: Int,
    @SerialName("ayah_count") val ayahCount: Int = 0,
    @SerialName("start") val start: AyahReferenceDto,
    @SerialName("end") val end: AyahReferenceDto,
)

@Serializable
data class TranslationSourceCoverageDto(
    @SerialName("percent") val percent: Double? = null,
)

@Serializable
data class TranslationSourceDto(
    @SerialName("id") val id: String,
    @SerialName("lang") val lang: String,
    @SerialName("name") val name: String,
    @SerialName("translator") val translator: String? = null,
    @SerialName("coverage") val coverage: TranslationSourceCoverageDto? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable
data class RecitationDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("reciter_name") val reciterName: String = "",
    @SerialName("style") val style: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable
data class QuranSearchResultDto(
    @SerialName("ayah") val ayah: AyahDto,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("matched_lang") val matchedLang: String? = null,
    @SerialName("matched_source_id") val matchedSourceId: String? = null,
    @SerialName("matched_field") val matchedField: String? = null,
)
