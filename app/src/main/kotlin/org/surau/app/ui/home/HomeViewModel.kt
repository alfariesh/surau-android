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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.data.repository.QuranRepository
import javax.inject.Inject

/**
 * Backs the Home dashboard header: the continue-reading card. The reading-activity content shown
 * below it (streak, khatam, heatmap, …) is owned by [org.surau.app.feature.activity.impl.ActivityPane]
 * and its own view model.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    quranProgressRepository: QuranProgressRepository,
    private val quranRepository: QuranRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
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
            .map { HomeUiState(continueReading = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

data class HomeUiState(
    val continueReading: ContinueReading? = null,
)

data class ContinueReading(
    val surahId: Int,
    val surahName: String,
    val ayahNumber: Int,
)
