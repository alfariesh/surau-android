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

package org.surau.app.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import org.surau.app.core.model.data.quran.Ayah

/**
 * A Quran ayah, cached per surah from `GET /quran/surahs/{id}/ayahs`.
 */
@Entity(
    tableName = "ayahs",
    primaryKeys = ["surah_id", "ayah_number"],
    indices = [Index(value = ["ayah_key"], unique = true)],
)
data class AyahEntity(
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "ayah_key")
    val ayahKey: String,
    @ColumnInfo(name = "text_qpc_hafs")
    val textQpcHafs: String,
    @ColumnInfo(name = "page_number")
    val pageNumber: Int?,
    @ColumnInfo(name = "juz_number")
    val juzNumber: Int?,
    @ColumnInfo(name = "hizb_number")
    val hizbNumber: Int?,
    /**
     * Simplified Imlaei Arabic text. Only populated by the full download path (`view=full`);
     * the lazy reader path (`view=reader_minimal`) leaves it null. Indexed for offline search.
     */
    @ColumnInfo(name = "text_imlaei_simple")
    val textImlaeiSimple: String? = null,
    /**
     * Server-normalised, diacritic-stripped Arabic search text. Preferred FTS index source when
     * present; like [textImlaeiSimple] it is only filled by the full download path.
     */
    @ColumnInfo(name = "search_text")
    val searchText: String? = null,
)

fun AyahEntity.asExternalModel() = Ayah(
    surahId = surahId,
    ayahNumber = ayahNumber,
    textQpcHafs = textQpcHafs,
    pageNumber = pageNumber,
    juzNumber = juzNumber,
    hizbNumber = hizbNumber,
)
