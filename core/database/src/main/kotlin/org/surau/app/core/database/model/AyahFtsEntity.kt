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
import androidx.room.Fts4

/**
 * A full-text-search row for one downloaded ayah, scoped to one translation source.
 *
 * This is a **standalone** FTS4 table (not content-linked): the searchable text spans two base
 * tables (`ayahs.text_imlaei_simple` and `translations.text`), so there is no single content table
 * to mirror. It is populated only by the offline download path and queried as a fallback when the
 * server search is unreachable. [arabic] and [translation] are indexed; [ayahKey] and [sourceId]
 * are stored but excluded from the index — they exist purely to join back to the base tables and to
 * scope a query/delete to one translation source.
 */
@Entity(tableName = "ayahs_fts")
@Fts4(notIndexed = ["ayah_key", "source_id"])
data class AyahFtsEntity(
    @ColumnInfo(name = "ayah_key")
    val ayahKey: String,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    /** Normalised Arabic search text (`search_text` ?: `text_imlaei_simple`). */
    val arabic: String,
    /** Translation text for [sourceId]. */
    val translation: String,
)
