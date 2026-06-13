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

package org.surau.app.core.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.Surah
import javax.inject.Inject

/**
 * Everything the surah reader needs: the surah, its ayahs with translations from the user's
 * effective translation source, and the user's reader presentation preferences.
 *
 * Triggers the per-surah lazy cache fetch ([QuranRepository.ensureSurahCached]) and re-resolves
 * automatically when the user changes translation source or reader settings.
 */
class GetReaderContentUseCase @Inject constructor(
    private val quranRepository: QuranRepository,
    private val userDataRepository: UserDataRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(surahId: Int): Flow<ReaderContent> =
        userDataRepository.userData
            .map { ReaderPrefs(it.readerMode, it.translationSourceId, it.arabicFontScale) }
            .distinctUntilChanged()
            .flatMapLatest { prefs ->
                flow {
                    val sourceId =
                        quranRepository.resolveTranslationSourceId(prefs.translationSourceId)
                    // Fetch (or revalidate) the surah content before observing the cache.
                    quranRepository.ensureSurahCached(surahId, sourceId)
                    emitAll(
                        combine(
                            quranRepository.observeSurah(surahId),
                            quranRepository.observeAyahs(surahId, sourceId),
                        ) { surah, ayahs ->
                            ReaderContent(
                                surah = surah,
                                ayahs = ayahs,
                                readerMode = prefs.readerMode,
                                arabicFontScale = prefs.arabicFontScale,
                                translationSourceId = sourceId,
                            )
                        },
                    )
                }
            }

    private data class ReaderPrefs(
        val readerMode: ReaderMode,
        val translationSourceId: String?,
        val arabicFontScale: Float,
    )
}

data class ReaderContent(
    val surah: Surah?,
    val ayahs: List<PopulatedAyah>,
    val readerMode: ReaderMode,
    val arabicFontScale: Float,
    val translationSourceId: String,
)
