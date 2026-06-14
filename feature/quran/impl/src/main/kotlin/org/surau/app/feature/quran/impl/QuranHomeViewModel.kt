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

package org.surau.app.feature.quran.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.surau.app.core.common.result.Result
import org.surau.app.core.common.result.asResult
import org.surau.app.core.data.repository.ActivityRepository
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.domain.GetSurahListWithLastReadUseCase
import org.surau.app.core.domain.LastRead
import org.surau.app.core.model.data.activity.ReadingStreak
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.JuzSegment
import org.surau.app.core.model.data.quran.Surah
import javax.inject.Inject

@HiltViewModel
class QuranHomeViewModel @Inject constructor(
    getSurahListWithLastRead: GetSurahListWithLastReadUseCase,
    quranRepository: QuranRepository,
    authRepository: AuthRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {

    /**
     * The signed-in user's reading streak for the home header chip. Authenticated-only and
     * failure-tolerant: emits `null` for guests and on any error, and an immediate `null` first so
     * it never delays the (offline-first) surah list.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val streak: Flow<ReadingStreak?> =
        authRepository.authState.flatMapLatest { state ->
            if (state is AuthState.Authenticated) {
                flow {
                    emit(null)
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    emit(runCatching { activityRepository.getStreak(today) }.getOrNull())
                }
            } else {
                flowOf(null)
            }
        }

    val uiState: StateFlow<QuranHomeUiState> =
        combine(
            combine(
                getSurahListWithLastRead(),
                quranRepository.observeJuzList(),
            ) { surahsWithLastRead, juzList ->
                QuranHomeData(
                    surahs = surahsWithLastRead.surahs,
                    juzList = juzList,
                    lastRead = surahsWithLastRead.lastRead,
                    progressBySurah = surahsWithLastRead.progressBySurah,
                )
            }.asResult(),
            streak,
        ) { result, streakValue ->
            when (result) {
                is Result.Loading -> QuranHomeUiState.Loading
                is Result.Error -> QuranHomeUiState.Error
                is Result.Success ->
                    if (result.data.surahs.isEmpty()) {
                        // First launch while offline: nothing cached yet.
                        QuranHomeUiState.Error
                    } else {
                        QuranHomeUiState.Success(
                            surahs = result.data.surahs,
                            juzList = result.data.juzList,
                            lastRead = result.data.lastRead,
                            progressBySurah = result.data.progressBySurah,
                            streak = streakValue,
                        )
                    }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = QuranHomeUiState.Loading,
            )

    private data class QuranHomeData(
        val surahs: List<Surah>,
        val juzList: List<JuzSegment>,
        val lastRead: LastRead?,
        val progressBySurah: Map<Int, Float>,
    )
}

sealed interface QuranHomeUiState {
    data object Loading : QuranHomeUiState

    data object Error : QuranHomeUiState

    data class Success(
        val surahs: List<Surah>,
        val juzList: List<JuzSegment>,
        val lastRead: LastRead?,
        val progressBySurah: Map<Int, Float> = emptyMap(),
        val streak: ReadingStreak? = null,
    ) : QuranHomeUiState
}
