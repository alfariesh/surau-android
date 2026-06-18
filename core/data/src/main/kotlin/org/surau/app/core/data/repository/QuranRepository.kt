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
import org.surau.app.core.model.data.quran.JuzSegment
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.Surah
import org.surau.app.core.model.data.quran.TranslationSource

/**
 * Offline-first access to Quran content. Room is the source of truth; the backend is consulted
 * lazily (per surah) and on freshness expiry.
 */
interface QuranRepository {

    /** All 114 surahs, refreshing from the network when empty or stale. */
    fun observeSurahs(): Flow<List<Surah>>

    /** A single surah's metadata. */
    fun observeSurah(surahId: Int): Flow<Surah?>

    /** The 30 juz segments. */
    fun observeJuzList(): Flow<List<JuzSegment>>

    /** Available translation sources. */
    fun observeTranslationSources(): Flow<List<TranslationSource>>

    /** Ayahs of [surahId] with translations from [translationSourceId], from the local cache. */
    fun observeAyahs(surahId: Int, translationSourceId: String): Flow<List<PopulatedAyah>>

    /**
     * Ensures [surahId]'s ayahs (with [translationSourceId] translations) are cached locally,
     * fetching from the network if missing or stale.
     *
     * On a network error: when [allowStaleOnError] is true (the on-demand reader path) an existing
     * cache is served and the error swallowed; when false (the bulk-download path) the IOException
     * is rethrown so the caller can retry rather than silently skip a stale surah.
     *
     * @throws org.surau.app.core.network.model.SurauApiException on backend errors
     * @throws java.io.IOException offline with no usable cache
     */
    suspend fun ensureSurahCached(
        surahId: Int,
        translationSourceId: String,
        allowStaleOnError: Boolean = true,
    )

    /**
     * The user's effective translation source: their preference if set, otherwise the backend
     * default.
     */
    suspend fun resolveTranslationSourceId(preferredId: String?): String

    /**
     * Full-text search. Tries the server first; when offline, falls back to a local FTS index over
     * downloaded surahs scoped to [translationSourceId] (the user's effective source — null lets the
     * repository resolve the default). Offline hits are flagged via [QuranSearchResult.isOffline].
     */
    suspend fun search(
        query: String,
        translationSourceId: String? = null,
    ): List<QuranSearchResult>
}

/**
 * One search hit: the matching ayah with its translation. [isOffline] is true when the result came
 * from the local index because the server was unreachable.
 */
data class QuranSearchResult(
    val ayah: PopulatedAyah,
    val score: Double,
    val isOffline: Boolean = false,
)
