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
import org.surau.app.core.common.coroutines.runCatchingExceptCancellation
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
            .map {
                ReaderPrefs(
                    readerMode = it.readerMode,
                    translationSourceId = it.translationSourceId,
                    arabicFontScale = it.arabicFontScale,
                    showTransliteration = it.readerShowTransliteration,
                    showTranslation = it.readerShowTranslation,
                    arabicLineSpacing = it.readerArabicLineSpacing,
                    translationScale = it.readerTranslationScale,
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { prefs ->
                flow {
                    val sourceId =
                        quranRepository.resolveTranslationSourceId(prefs.translationSourceId)
                    // Best-effort refresh: a failure (e.g. offline, or switching to an uncached
                    // source) must NOT terminate the flow and collapse the reader to a full-screen
                    // error. The cache below still serves what it has; the ViewModel decides between
                    // content and an empty/error state from the observed data, and the flow stays
                    // alive so a later cache write recovers automatically.
                    runCatchingExceptCancellation { quranRepository.ensureSurahCached(surahId, sourceId) }
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
                                showTransliteration = prefs.showTransliteration,
                                showTranslation = prefs.showTranslation,
                                arabicLineSpacing = prefs.arabicLineSpacing,
                                translationScale = prefs.translationScale,
                            )
                        },
                    )
                }
            }

    private data class ReaderPrefs(
        val readerMode: ReaderMode,
        val translationSourceId: String?,
        val arabicFontScale: Float,
        val showTransliteration: Boolean,
        val showTranslation: Boolean,
        val arabicLineSpacing: Float,
        val translationScale: Float,
    )
}

data class ReaderContent(
    val surah: Surah?,
    val ayahs: List<PopulatedAyah>,
    val readerMode: ReaderMode,
    val arabicFontScale: Float,
    val translationSourceId: String,
    val showTransliteration: Boolean = false,
    val showTranslation: Boolean = true,
    val arabicLineSpacing: Float = 1f,
    val translationScale: Float = 1f,
)
