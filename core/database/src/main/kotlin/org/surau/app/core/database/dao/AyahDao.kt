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

package org.surau.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.surau.app.core.database.model.AyahEntity
import org.surau.app.core.database.model.AyahFetchMetadataEntity
import org.surau.app.core.database.model.AyahFtsEntity
import org.surau.app.core.database.model.TranslationEntity
import org.surau.app.core.database.model.TransliterationEntity

/**
 * A row combining an ayah with its translation/transliteration for a given source, produced by
 * [AyahDao.observePopulatedAyahs].
 */
data class PopulatedAyahRow(
    val surahId: Int,
    val ayahNumber: Int,
    val textQpcHafs: String,
    val pageNumber: Int?,
    val juzNumber: Int?,
    val hizbNumber: Int?,
    val translationSourceId: String?,
    val translationLang: String?,
    val translationText: String?,
    val transliterationSourceId: String?,
    val transliterationLang: String?,
    val transliterationText: String?,
)

@Dao
interface AyahDao {

    @Query(
        """
        SELECT
            a.surah_id AS surahId,
            a.ayah_number AS ayahNumber,
            a.text_qpc_hafs AS textQpcHafs,
            a.page_number AS pageNumber,
            a.juz_number AS juzNumber,
            a.hizb_number AS hizbNumber,
            t.source_id AS translationSourceId,
            t.lang AS translationLang,
            t.text AS translationText,
            tr.source_id AS transliterationSourceId,
            tr.lang AS transliterationLang,
            tr.text AS transliterationText
        FROM ayahs a
        LEFT JOIN translations t
            ON t.surah_id = a.surah_id AND t.ayah_number = a.ayah_number
                AND t.source_id = :translationSourceId
        LEFT JOIN transliterations tr
            ON tr.surah_id = a.surah_id AND tr.ayah_number = a.ayah_number
        WHERE a.surah_id = :surahId
        ORDER BY a.ayah_number
        """,
    )
    fun observePopulatedAyahs(
        surahId: Int,
        translationSourceId: String,
    ): Flow<List<PopulatedAyahRow>>

    @Upsert
    suspend fun upsertAyahs(ayahs: List<AyahEntity>)

    @Upsert
    suspend fun upsertTranslations(translations: List<TranslationEntity>)

    @Upsert
    suspend fun upsertTransliterations(transliterations: List<TransliterationEntity>)

    @Upsert
    suspend fun upsertFetchMetadata(metadata: AyahFetchMetadataEntity)

    @Insert
    suspend fun insertFtsRows(rows: List<AyahFtsEntity>)

    /**
     * Clears this surah's FTS rows for one source. Scoped by `ayah_key LIKE 'surahId:%'` (string
     * comparison — FTS columns have no type affinity, so binding the integer directly would never
     * match the stored text). Called before re-inserting so a re-download stays idempotent.
     */
    @Query("DELETE FROM ayahs_fts WHERE source_id = :sourceId AND ayah_key LIKE :surahId || ':%'")
    suspend fun deleteFtsForSurah(surahId: Int, sourceId: String)

    @Query(
        """
        SELECT * FROM ayah_fetch_metadata
        WHERE surah_id = :surahId AND translation_source_id = :translationSourceId
        """,
    )
    suspend fun fetchMetadata(
        surahId: Int,
        translationSourceId: String,
    ): AyahFetchMetadataEntity?

    /**
     * Offline full-text search over downloaded ayahs for one translation source. [match] is a
     * sanitised FTS4 MATCH expression (see the repository); results join back to the base tables so
     * the hit carries the same populated shape as the reader.
     */
    @Query(
        """
        SELECT
            a.surah_id AS surahId,
            a.ayah_number AS ayahNumber,
            a.text_qpc_hafs AS textQpcHafs,
            a.page_number AS pageNumber,
            a.juz_number AS juzNumber,
            a.hizb_number AS hizbNumber,
            t.source_id AS translationSourceId,
            t.lang AS translationLang,
            t.text AS translationText,
            tr.source_id AS transliterationSourceId,
            tr.lang AS transliterationLang,
            tr.text AS transliterationText
        FROM ayahs_fts f
        JOIN ayahs a ON a.ayah_key = f.ayah_key
        LEFT JOIN translations t
            ON t.surah_id = a.surah_id AND t.ayah_number = a.ayah_number
                AND t.source_id = :translationSourceId
        LEFT JOIN transliterations tr
            ON tr.surah_id = a.surah_id AND tr.ayah_number = a.ayah_number
        WHERE f.source_id = :translationSourceId AND ayahs_fts MATCH :match
        ORDER BY a.surah_id, a.ayah_number
        LIMIT :limit
        """,
    )
    suspend fun searchAyahsFts(
        translationSourceId: String,
        match: String,
        limit: Int,
    ): List<PopulatedAyahRow>

    /**
     * Atomically stores a fetched surah: ayahs, translations, transliterations, the cache marker,
     * and (when [ftsRows] is non-empty, i.e. a `view=full` fetch) the offline-search index for this
     * surah+source.
     */
    @Transaction
    suspend fun upsertSurahContent(
        ayahs: List<AyahEntity>,
        translations: List<TranslationEntity>,
        transliterations: List<TransliterationEntity>,
        metadata: AyahFetchMetadataEntity,
        ftsRows: List<AyahFtsEntity> = emptyList(),
    ) {
        upsertAyahs(ayahs)
        upsertTranslations(translations)
        upsertTransliterations(transliterations)
        upsertFetchMetadata(metadata)
        if (ftsRows.isNotEmpty()) {
            deleteFtsForSurah(metadata.surahId, metadata.translationSourceId)
            insertFtsRows(ftsRows)
        }
    }
}
