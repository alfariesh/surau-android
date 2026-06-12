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

package org.surau.app.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.surau.app.core.database.dao.AyahDao
import org.surau.app.core.database.dao.JuzDao
import org.surau.app.core.database.dao.PopulatedAyahRow
import org.surau.app.core.database.dao.SurahDao
import org.surau.app.core.database.dao.TranslationSourceDao
import org.surau.app.core.database.model.AyahEntity
import org.surau.app.core.database.model.AyahFetchMetadataEntity
import org.surau.app.core.database.model.JuzEntity
import org.surau.app.core.database.model.SurahEntity
import org.surau.app.core.database.model.TranslationEntity
import org.surau.app.core.database.model.TranslationSourceEntity
import org.surau.app.core.database.model.TransliterationEntity
import org.surau.app.core.database.model.asExternalModel
import org.surau.app.core.model.data.quran.Ayah
import org.surau.app.core.model.data.quran.JuzSegment
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.Surah
import org.surau.app.core.model.data.quran.Translation
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.core.model.data.quran.Transliteration
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.quran.AyahDto
import org.surau.app.core.network.retrofit.SurauQuranApi
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Room is the source of truth; surah metadata refreshes weekly, per-surah ayah content monthly.
 * Quran text is immutable in practice, so stale reads are always acceptable offline.
 */
internal class OfflineFirstQuranRepository @Inject constructor(
    private val quranApi: SurauQuranApi,
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao,
    private val juzDao: JuzDao,
    private val translationSourceDao: TranslationSourceDao,
) : QuranRepository {

    private val surahRefreshMutex = Mutex()

    override fun observeSurahs(): Flow<List<Surah>> =
        surahDao.observeAll()
            .onStart { refreshSurahsIfNeeded() }
            .map { surahs -> surahs.map(SurahEntity::asExternalModel) }

    override fun observeSurah(surahId: Int): Flow<Surah?> =
        surahDao.observeSurah(surahId).map { it?.asExternalModel() }

    override fun observeJuzList(): Flow<List<JuzSegment>> =
        juzDao.observeAll()
            .onStart { refreshJuzIfNeeded() }
            .map { juz -> juz.map(JuzEntity::asExternalModel) }

    override fun observeTranslationSources(): Flow<List<TranslationSource>> =
        translationSourceDao.observeAll()
            .onStart { refreshTranslationSourcesIfNeeded() }
            .map { sources -> sources.map(TranslationSourceEntity::asExternalModel) }

    override fun observeAyahs(
        surahId: Int,
        translationSourceId: String,
    ): Flow<List<PopulatedAyah>> =
        ayahDao.observePopulatedAyahs(surahId, translationSourceId)
            .map { rows -> rows.map(PopulatedAyahRow::asExternalModel) }

    override suspend fun ensureSurahCached(surahId: Int, translationSourceId: String) {
        val metadata = ayahDao.fetchMetadata(surahId, translationSourceId)
        val isFresh = metadata != null &&
            metadata.fetchedAt > Clock.System.now() - AYAH_CACHE_TTL
        if (isFresh) return

        val response = try {
            apiCall {
                quranApi.ayahs(
                    surahId = surahId,
                    translationSource = translationSourceId,
                )
            }
        } catch (exception: IOException) {
            // Offline: serve whatever is cached, only fail when there is nothing at all.
            if (metadata != null) return else throw exception
        }

        val now = Clock.System.now()
        ayahDao.upsertSurahContent(
            ayahs = response.items.map { it.asAyahEntity() },
            translations = response.items.mapNotNull { it.asTranslationEntity(translationSourceId) },
            transliterations = response.items.mapNotNull { it.asTransliterationEntity() },
            metadata = AyahFetchMetadataEntity(
                surahId = surahId,
                translationSourceId = translationSourceId,
                fetchedAt = now,
            ),
        )
    }

    override suspend fun resolveTranslationSourceId(preferredId: String?): String {
        if (preferredId != null) return preferredId
        refreshTranslationSourcesIfNeeded()
        val sources = translationSourceDao.observeAll().first()
        return sources.firstOrNull { it.isDefault }?.id
            ?: sources.firstOrNull()?.id
            ?: DEFAULT_TRANSLATION_SOURCE_ID
    }

    override suspend fun search(query: String): List<QuranSearchResult> =
        apiCall { quranApi.search(query = query) }
            .items
            .map { result ->
                QuranSearchResult(
                    ayah = PopulatedAyah(
                        ayah = result.ayah.asExternalModel(),
                        translation = result.ayah.translation?.let { translation ->
                            Translation(
                                sourceId = translation.sourceId.orEmpty(),
                                lang = translation.lang.orEmpty(),
                                text = translation.text,
                            )
                        },
                    ),
                    score = result.score,
                )
            }

    private suspend fun refreshSurahsIfNeeded() {
        surahRefreshMutex.withLock {
            val oldest = surahDao.oldestFetchedAt()
            val isFresh = surahDao.count() > 0 &&
                oldest != null &&
                Instant.fromEpochMilliseconds(oldest) > Clock.System.now() - SURAH_CACHE_TTL
            if (isFresh) return

            val surahs = try {
                apiCall { quranApi.surahs() }.items
            } catch (_: Exception) {
                // Offline-first: keep serving the cache (or an empty list on first launch).
                return
            }

            val now = Clock.System.now()
            surahDao.upsertAll(
                surahs.map { dto ->
                    SurahEntity(
                        surahId = dto.surahId,
                        nameArabic = dto.nameArabic,
                        nameLatin = dto.nameLatin,
                        nameTranslation = dto.nameTranslation,
                        revelationType = dto.revelationType,
                        ayahCount = dto.ayahCount,
                        info = dto.info,
                        fetchedAt = now,
                    )
                },
            )
        }
    }

    private suspend fun refreshJuzIfNeeded() {
        if (juzDao.count() > 0) return
        val juz = try {
            apiCall { quranApi.juz() }.items
        } catch (_: Exception) {
            return
        }
        juzDao.upsertAll(
            juz.map { dto ->
                JuzEntity(
                    number = dto.number,
                    ayahCount = dto.ayahCount,
                    startSurahId = dto.start.surahId,
                    startAyahNumber = dto.start.ayahNumber,
                    startSurahName = dto.start.surahName,
                    endSurahId = dto.end.surahId,
                    endAyahNumber = dto.end.ayahNumber,
                    endSurahName = dto.end.surahName,
                )
            },
        )
    }

    private suspend fun refreshTranslationSourcesIfNeeded() {
        if (translationSourceDao.count() > 0) return
        val sources = try {
            apiCall { quranApi.translationSources() }.items
        } catch (_: Exception) {
            return
        }
        val now = Clock.System.now()
        translationSourceDao.upsertAll(
            sources.map { dto ->
                TranslationSourceEntity(
                    id = dto.id,
                    lang = dto.lang,
                    name = dto.name,
                    translator = dto.translator,
                    coveragePercent = dto.coverage?.percent,
                    isDefault = dto.isDefault,
                    fetchedAt = now,
                )
            },
        )
    }

    companion object {
        private val SURAH_CACHE_TTL = 7.days
        private val AYAH_CACHE_TTL = 30.days

        /** Kemenag Indonesian translation — sane fallback when the backend is unreachable. */
        private const val DEFAULT_TRANSLATION_SOURCE_ID = "kemenag-id-translation"
    }
}

private fun AyahDto.asExternalModel() = Ayah(
    surahId = surahId,
    ayahNumber = ayahNumber,
    textQpcHafs = textQpcHafs,
    pageNumber = pageNumber,
    juzNumber = juzNumber,
    hizbNumber = hizbNumber,
)

private fun AyahDto.asAyahEntity() = AyahEntity(
    surahId = surahId,
    ayahNumber = ayahNumber,
    ayahKey = ayahKey,
    textQpcHafs = textQpcHafs,
    pageNumber = pageNumber,
    juzNumber = juzNumber,
    hizbNumber = hizbNumber,
)

private fun AyahDto.asTranslationEntity(requestedSourceId: String): TranslationEntity? =
    translation?.let { translation ->
        TranslationEntity(
            surahId = surahId,
            ayahNumber = ayahNumber,
            // reader_minimal omits source_id; attribute to the requested source.
            sourceId = translation.sourceId ?: requestedSourceId,
            lang = translation.lang.orEmpty(),
            text = translation.text,
        )
    }

private fun AyahDto.asTransliterationEntity(): TransliterationEntity? =
    transliteration?.let { transliteration ->
        TransliterationEntity(
            surahId = surahId,
            ayahNumber = ayahNumber,
            sourceId = transliteration.sourceId.orEmpty(),
            lang = transliteration.lang.orEmpty(),
            text = transliteration.text,
        )
    }

private fun PopulatedAyahRow.asExternalModel() = PopulatedAyah(
    ayah = Ayah(
        surahId = surahId,
        ayahNumber = ayahNumber,
        textQpcHafs = textQpcHafs,
        pageNumber = pageNumber,
        juzNumber = juzNumber,
        hizbNumber = hizbNumber,
    ),
    translation = translationText?.let {
        Translation(
            sourceId = translationSourceId.orEmpty(),
            lang = translationLang.orEmpty(),
            text = it,
        )
    },
    transliteration = transliterationText?.let {
        Transliteration(
            sourceId = transliterationSourceId.orEmpty(),
            lang = transliterationLang.orEmpty(),
            text = it,
        )
    },
)
