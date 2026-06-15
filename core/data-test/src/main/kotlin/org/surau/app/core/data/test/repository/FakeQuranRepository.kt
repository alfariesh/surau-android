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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.QuranSearchResult
import org.surau.app.core.data.test.QuranTestData
import org.surau.app.core.model.data.quran.JuzSegment
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.Surah
import org.surau.app.core.model.data.quran.TranslationSource
import javax.inject.Inject

/**
 * Fake [QuranRepository] backed by [QuranTestData] (Al-Fatihah + Al-Muzzammil excerpts).
 */
class FakeQuranRepository @Inject constructor() : QuranRepository {

    private val surahs = MutableStateFlow(QuranTestData.surahs)

    override fun observeSurahs(): Flow<List<Surah>> = surahs

    override fun observeSurah(surahId: Int): Flow<Surah?> =
        surahs.map { all -> all.firstOrNull { it.surahId == surahId } }

    override fun observeJuzList(): Flow<List<JuzSegment>> = MutableStateFlow(QuranTestData.juz)

    override fun observeTranslationSources(): Flow<List<TranslationSource>> =
        MutableStateFlow(QuranTestData.translationSources)

    override fun observeAyahs(
        surahId: Int,
        translationSourceId: String,
    ): Flow<List<PopulatedAyah>> =
        MutableStateFlow(QuranTestData.ayahsBySurah[surahId].orEmpty())

    override suspend fun ensureSurahCached(surahId: Int, translationSourceId: String) = Unit

    override suspend fun resolveTranslationSourceId(preferredId: String?): String =
        preferredId ?: QuranTestData.translationSources.first().id

    override suspend fun search(
        query: String,
        translationSourceId: String?,
    ): List<QuranSearchResult> =
        QuranTestData.ayahsBySurah.values.flatten()
            .filter { it.translation?.text?.contains(query, ignoreCase = true) == true }
            .map { QuranSearchResult(ayah = it, score = 1.0) }
}
