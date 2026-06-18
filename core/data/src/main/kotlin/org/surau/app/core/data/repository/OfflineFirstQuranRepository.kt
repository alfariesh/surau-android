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
import org.surau.app.core.common.coroutines.runCatchingExceptCancellation
import org.surau.app.core.database.dao.AyahDao
import org.surau.app.core.database.dao.JuzDao
import org.surau.app.core.database.dao.PopulatedAyahRow
import org.surau.app.core.database.dao.SurahDao
import org.surau.app.core.database.dao.TranslationSourceDao
import org.surau.app.core.database.model.AyahEntity
import org.surau.app.core.database.model.AyahFetchMetadataEntity
import org.surau.app.core.database.model.AyahFtsEntity
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

    override suspend fun ensureSurahCached(
        surahId: Int,
        translationSourceId: String,
        allowStaleOnError: Boolean,
    ) {
        val metadata = ayahDao.fetchMetadata(surahId, translationSourceId)
        val isFresh = metadata != null &&
            metadata.fetchedAt > Clock.System.now() - AYAH_CACHE_TTL
        if (isFresh) return

        val response = try {
            apiCall {
                quranApi.ayahs(
                    surahId = surahId,
                    translationSource = translationSourceId,
                    // `full` carries `text_imlaei_simple`/`search_text`, so every cached surah is
                    // offline-searchable — whether reached by reading or by the bulk download.
                    view = VIEW_FULL,
                )
            }
        } catch (exception: IOException) {
            // Offline. Reader path: serve whatever is cached. Bulk-download path
            // (allowStaleOnError == false): a stale surah must not be silently skipped — rethrow so
            // the worker retries it instead of reporting success with a gap in the offline index.
            if (metadata != null && allowStaleOnError) return else throw exception
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
            ftsRows = response.items.map { it.asFtsEntity(translationSourceId) },
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

    override suspend fun search(
        query: String,
        translationSourceId: String?,
    ): List<QuranSearchResult> = try {
        apiCall { quranApi.search(query = query, translationSource = translationSourceId) }
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
    } catch (_: IOException) {
        searchLocal(query, translationSourceId)
    }

    /**
     * Offline fallback over the downloaded FTS index. Returns nothing (rather than throwing) when
     * the query has no searchable tokens or nothing is downloaded for the resolved source.
     */
    private suspend fun searchLocal(
        query: String,
        translationSourceId: String?,
    ): List<QuranSearchResult> {
        val match = buildFtsMatchExpression(query) ?: return emptyList()
        val sourceId = resolveTranslationSourceId(translationSourceId)
        return ayahDao.searchAyahsFts(sourceId, match, LOCAL_SEARCH_LIMIT)
            .map { row -> QuranSearchResult(ayah = row.asExternalModel(), score = 0.0, isOffline = true) }
    }

    private suspend fun refreshSurahsIfNeeded() {
        surahRefreshMutex.withLock {
            val oldest = surahDao.oldestFetchedAt()
            val isFresh = surahDao.count() > 0 &&
                oldest != null &&
                Instant.fromEpochMilliseconds(oldest) > Clock.System.now() - SURAH_CACHE_TTL
            if (isFresh) return

            val surahs = runCatchingExceptCancellation {
                apiCall { quranApi.surahs() }.items
            }.getOrElse {
                // Offline-first: keep serving the cache (or an empty list on first launch).
                return
            }

            val now = Clock.System.now()
            surahDao.upsertAll(
                surahs.map { dto ->
                    SurahEntity(
                        surahId = dto.surahId,
                        nameArabic = dto.nameArabic.orEmpty(),
                        nameLatin = dto.nameLatin ?: dto.nameArabic.orEmpty(),
                        nameTranslation = dto.nameTranslation,
                        revelationType = dto.revelationType,
                        ayahCount = dto.ayahCount,
                        info = dto.info?.textHtml?.ifBlank { null },
                        fetchedAt = now,
                    )
                },
            )
        }
    }

    private suspend fun refreshJuzIfNeeded() {
        if (juzDao.count() > 0) return
        val juz = runCatchingExceptCancellation {
            apiCall { quranApi.juz() }.items
        }.getOrElse { return }
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
        val sources = runCatchingExceptCancellation {
            apiCall { quranApi.translationSources() }.items
        }.getOrElse { return }
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

        /** Full ayah view: includes `text_imlaei_simple`/`search_text` needed by offline search. */
        private const val VIEW_FULL = "full"

        /** Cap on offline FTS hits; the online endpoint defaults to 20, so mirror it. */
        private const val LOCAL_SEARCH_LIMIT = 50
    }
}

/**
 * Turns a free-text query into a safe FTS4 MATCH expression: split on whitespace, strip the FTS
 * operator characters, and prefix-match each surviving token (implicit AND). Returns null when no
 * usable token remains so the caller can skip the local query.
 */
internal fun buildFtsMatchExpression(query: String): String? {
    val tokens = query
        .split(Regex("\\s+"))
        .mapNotNull { raw ->
            val cleaned = raw.filterNot { it in FTS_RESERVED_CHARS }
            cleaned.ifBlank { null }
        }
    if (tokens.isEmpty()) return null
    return tokens.joinToString(" ") { "$it*" }
}

/** Characters with special meaning in an FTS4 MATCH expression; stripped from user tokens. */
private const val FTS_RESERVED_CHARS = "\"*():^-"

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
    textImlaeiSimple = textImlaeiSimple,
    searchText = searchText,
)

/**
 * The FTS row for one ayah under [requestedSourceId]. Indexes the server-normalised search text
 * (falling back through imlaei to the Hafs text) plus this source's translation.
 */
private fun AyahDto.asFtsEntity(requestedSourceId: String) = AyahFtsEntity(
    ayahKey = ayahKey,
    sourceId = requestedSourceId,
    arabic = searchText ?: textImlaeiSimple ?: textQpcHafs,
    translation = translation?.text.orEmpty(),
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
