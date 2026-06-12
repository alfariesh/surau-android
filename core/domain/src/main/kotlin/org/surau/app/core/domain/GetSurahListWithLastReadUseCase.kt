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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.model.data.quran.QuranReadingPosition
import org.surau.app.core.model.data.quran.Surah
import javax.inject.Inject

/**
 * The Quran home content: every surah plus the user's resume point (with its surah resolved for
 * display on the "last read" card).
 */
class GetSurahListWithLastReadUseCase @Inject constructor(
    private val quranRepository: QuranRepository,
    private val quranProgressRepository: QuranProgressRepository,
) {
    operator fun invoke(): Flow<SurahListWithLastRead> =
        combine(
            quranRepository.observeSurahs(),
            quranProgressRepository.observePosition(),
        ) { surahs, position ->
            SurahListWithLastRead(
                surahs = surahs,
                lastRead = position?.let { p ->
                    surahs.firstOrNull { it.surahId == p.surahId }?.let { surah ->
                        LastRead(position = p, surah = surah)
                    }
                },
            )
        }
}

data class SurahListWithLastRead(
    val surahs: List<Surah>,
    val lastRead: LastRead?,
)

data class LastRead(
    val position: QuranReadingPosition,
    val surah: Surah,
)
