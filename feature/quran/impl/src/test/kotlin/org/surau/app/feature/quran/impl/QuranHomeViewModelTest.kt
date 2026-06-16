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

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.test.QuranTestData
import org.surau.app.core.data.test.repository.FakeActivityRepository
import org.surau.app.core.data.test.repository.FakeQuranProgressRepository
import org.surau.app.core.data.test.repository.FakeQuranRepository
import org.surau.app.core.domain.GetSurahListWithLastReadUseCase
import org.surau.app.core.testing.util.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Skips the conflatable Loading emission when the dispatcher races past it. */
private suspend fun TurbineTestContext<QuranHomeUiState>.awaitNonLoading(): QuranHomeUiState {
    var item = awaitItem()
    if (item is QuranHomeUiState.Loading) item = awaitItem()
    return item
}

class QuranHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val quranRepository = FakeQuranRepository()
    private val progressRepository = FakeQuranProgressRepository()
    private val activityRepository = FakeActivityRepository()

    private fun viewModel() = QuranHomeViewModel(
        getSurahListWithLastRead = GetSurahListWithLastReadUseCase(
            quranRepository = quranRepository,
            quranProgressRepository = progressRepository,
            activityRepository = activityRepository,
        ),
        quranRepository = quranRepository,
    )

    @Test
    fun uiState_startsLoading_thenShowsSurahs() = runTest {
        viewModel().uiState.test {
            val success = assertIs<QuranHomeUiState.Success>(awaitNonLoading())
            assertEquals(QuranTestData.surahs, success.surahs)
            assertEquals(QuranTestData.juz, success.juzList)
        }
    }

    @Test
    fun uiState_mergesPerSurahProgress() = runTest {
        activityRepository.setSurahProgress(mapOf(73 to 0.5f))

        viewModel().uiState.test {
            var state = awaitNonLoading()
            while (state is QuranHomeUiState.Success && state.progressBySurah.isEmpty()) {
                state = awaitItem()
            }
            val success = assertIs<QuranHomeUiState.Success>(state)
            assertEquals(0.5f, success.progressBySurah[73])
        }
    }
}
