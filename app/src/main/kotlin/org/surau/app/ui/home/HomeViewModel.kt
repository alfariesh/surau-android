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

package org.surau.app.ui.home

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.surau.app.core.data.repository.ActivityRepository
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.KhatamRepository
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.model.data.activity.ReadingStreak
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.KhatamCycle
import javax.inject.Inject

/**
 * Backs the Home dashboard: continue-reading (always available) plus reading streak and khatam
 * (loaded only when signed in; failures fall back to `null` so the dashboard still renders).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val activityRepository: ActivityRepository,
    private val khatamRepository: KhatamRepository,
    quranProgressRepository: QuranProgressRepository,
    private val quranRepository: QuranRepository,
) : ViewModel() {

    private val continueReading: Flow<ContinueReading?> =
        quranProgressRepository.observePosition().flatMapLatest { position ->
            if (position == null) {
                flowOf(null)
            } else {
                quranRepository.observeSurah(position.surahId).map { surah ->
                    surah?.let {
                        ContinueReading(
                            surahId = position.surahId,
                            surahName = it.nameLatin,
                            ayahNumber = position.ayahNumber,
                        )
                    }
                }
            }
        }

    private val progress: Flow<Pair<ReadingStreak?, KhatamCycle?>> =
        authRepository.authState.flatMapLatest { state ->
            if (state is AuthState.Authenticated) {
                flow {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    val streak = runCatching { activityRepository.getStreak(today) }.getOrNull()
                    val khatam = runCatching { khatamRepository.getActiveCycle() }.getOrNull()
                    emit(streak to khatam)
                }
            } else {
                flowOf<Pair<ReadingStreak?, KhatamCycle?>>(null to null)
            }
        }

    val uiState: StateFlow<HomeUiState> =
        combine(authRepository.authState, continueReading, progress) { auth, resume, (streak, khatam) ->
            HomeUiState(
                signedIn = auth is AuthState.Authenticated,
                continueReading = resume,
                streak = streak,
                khatam = khatam,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

data class HomeUiState(
    val signedIn: Boolean = false,
    val continueReading: ContinueReading? = null,
    val streak: ReadingStreak? = null,
    val khatam: KhatamCycle? = null,
)

data class ContinueReading(
    val surahId: Int,
    val surahName: String,
    val ayahNumber: Int,
)
