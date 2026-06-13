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
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import org.surau.app.core.model.data.quran.RevelationType
import org.surau.app.core.model.data.quran.Surah

/**
 * One of the 114 Quran surahs, cached from `GET /quran/surahs`.
 */
@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "name_arabic")
    val nameArabic: String,
    @ColumnInfo(name = "name_latin")
    val nameLatin: String,
    @ColumnInfo(name = "name_translation")
    val nameTranslation: String,
    @ColumnInfo(name = "revelation_type")
    val revelationType: String,
    @ColumnInfo(name = "ayah_count")
    val ayahCount: Int,
    val info: String?,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Instant,
)

fun SurahEntity.asExternalModel() = Surah(
    surahId = surahId,
    nameArabic = nameArabic,
    nameLatin = nameLatin,
    nameTranslation = nameTranslation,
    // The backend sends "madinah"/"makkah" (some sources use "madaniyah"/"makkiyah").
    revelationType = if (revelationType.startsWith("madin", ignoreCase = true) ||
        revelationType.startsWith("madan", ignoreCase = true)
    ) {
        RevelationType.MADANIYAH
    } else {
        RevelationType.MAKKIYAH
    },
    ayahCount = ayahCount,
    info = info,
)
