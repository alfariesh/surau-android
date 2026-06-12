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
import org.surau.app.core.model.data.quran.Translation
import org.surau.app.core.model.data.quran.Transliteration

/**
 * A translation of one ayah from one translation source.
 */
@Entity(
    tableName = "translations",
    primaryKeys = ["surah_id", "ayah_number", "source_id"],
)
data class TranslationEntity(
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    val lang: String,
    val text: String,
)

fun TranslationEntity.asExternalModel() = Translation(
    sourceId = sourceId,
    lang = lang,
    text = text,
)

/**
 * A Latin transliteration of one ayah.
 */
@Entity(
    tableName = "transliterations",
    primaryKeys = ["surah_id", "ayah_number"],
)
data class TransliterationEntity(
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    val lang: String,
    val text: String,
)

fun TransliterationEntity.asExternalModel() = Transliteration(
    sourceId = sourceId,
    lang = lang,
    text = text,
)
