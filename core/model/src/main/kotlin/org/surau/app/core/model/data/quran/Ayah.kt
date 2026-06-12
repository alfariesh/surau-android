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
 * A single Quran ayah (verse).
 *
 * @property textQpcHafs Unicode Hafs text authored for the KFGQPC Uthmanic HAFS font.
 */
data class Ayah(
    val surahId: Int,
    val ayahNumber: Int,
    val textQpcHafs: String,
    val pageNumber: Int? = null,
    val juzNumber: Int? = null,
    val hizbNumber: Int? = null,
) {
    val ayahKey: AyahKey get() = AyahKey.of(surahId, ayahNumber)
}

/**
 * A translation of an ayah from a specific [TranslationSource].
 */
data class Translation(
    val sourceId: String,
    val lang: String,
    val text: String,
)

/**
 * A Latin transliteration of an ayah.
 */
data class Transliteration(
    val sourceId: String,
    val lang: String,
    val text: String,
)

/**
 * An ayah combined with its translation and transliteration for reading.
 */
data class PopulatedAyah(
    val ayah: Ayah,
    val translation: Translation? = null,
    val transliteration: Transliteration? = null,
)
