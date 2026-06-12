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
import kotlinx.datetime.Instant

/**
 * Marks that a surah's ayahs (with translations from [translationSourceId]) have been fetched
 * and cached, enabling per-surah lazy caching with freshness checks.
 */
@Entity(
    tableName = "ayah_fetch_metadata",
    primaryKeys = ["surah_id", "translation_source_id"],
)
data class AyahFetchMetadataEntity(
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "translation_source_id")
    val translationSourceId: String,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Instant,
)
