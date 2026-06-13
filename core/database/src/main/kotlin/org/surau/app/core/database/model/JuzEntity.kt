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
import org.surau.app.core.model.data.quran.AyahReference
import org.surau.app.core.model.data.quran.JuzSegment

/**
 * One of the 30 juz segments, cached from `GET /quran/juz`.
 */
@Entity(tableName = "juz")
data class JuzEntity(
    @PrimaryKey
    val number: Int,
    @ColumnInfo(name = "ayah_count")
    val ayahCount: Int,
    @ColumnInfo(name = "start_surah_id")
    val startSurahId: Int,
    @ColumnInfo(name = "start_ayah_number")
    val startAyahNumber: Int,
    @ColumnInfo(name = "start_surah_name")
    val startSurahName: String,
    @ColumnInfo(name = "end_surah_id")
    val endSurahId: Int,
    @ColumnInfo(name = "end_ayah_number")
    val endAyahNumber: Int,
    @ColumnInfo(name = "end_surah_name")
    val endSurahName: String,
)

fun JuzEntity.asExternalModel() = JuzSegment(
    number = number,
    ayahCount = ayahCount,
    start = AyahReference(startSurahId, startAyahNumber, startSurahName),
    end = AyahReference(endSurahId, endAyahNumber, endSurahName),
)
