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

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.test.QuranTestData
import org.surau.app.core.data.test.repository.FakeQuranProgressRepository
import org.surau.app.core.data.test.repository.FakeQuranRepository
import org.surau.app.core.datastore.SurauPreferencesDataSource
import org.surau.app.core.datastore.UserPreferences
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.data.test.repository.FakeUserDataRepository
import org.surau.app.core.domain.GetReaderContentUseCase
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.testing.util.MainDispatcherRule
import org.surau.app.feature.quran.api.navigation.SurahReaderNavKey
import app.cash.turbine.TurbineTestContext
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Skips the conflatable Loading emission when the dispatcher races past it. */
private suspend fun TurbineTestContext<ReaderUiState>.awaitNonLoading(): ReaderUiState {
    var item = awaitItem()
    if (item is ReaderUiState.Loading) item = awaitItem()
    return item
}

class SurahReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val quranRepository = FakeQuranRepository()
    private val progressRepository = FakeQuranProgressRepository()
    private val userDataRepository = FakeUserDataRepository(
        SurauPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance())),
    )

    private fun viewModel(navKey: SurahReaderNavKey) = SurahReaderViewModel(
        getReaderContent = GetReaderContentUseCase(
            quranRepository = quranRepository,
            userDataRepository = userDataRepository,
        ),
        quranRepository = quranRepository,
        quranProgressRepository = progressRepository,
        userDataRepository = userDataRepository,
        navKey = navKey,
    )

    @Test
    fun uiState_loadsSurahContent() = runTest {
        viewModel(SurahReaderNavKey(surahId = 1)).uiState.test {
            val success = assertIs<ReaderUiState.Success>(awaitNonLoading())
            assertEquals(1, success.content.surah?.surahId)
            assertEquals(7, success.content.ayahs.size)
            assertEquals(ReaderMode.ARABIC_TRANSLATION, success.content.readerMode)
        }
    }

    @Test
    fun uiState_carriesInitialAyahForResume() = runTest {
        viewModel(SurahReaderNavKey(surahId = 1, ayahNumber = 5)).uiState.test {
            val success = assertIs<ReaderUiState.Success>(awaitNonLoading())
            assertEquals(5, success.initialAyahNumber)
        }
    }

    @Test
    fun onAyahVisible_savesPositionAfterDebounce() = runTest {
        val viewModel = viewModel(SurahReaderNavKey(surahId = 73))

        viewModel.onAyahVisible(2)
        runCurrent()
        // Before the debounce window nothing is saved.
        assertEquals(null, progressRepository.observePosition().first())

        advanceTimeBy(2_000)
        runCurrent()

        assertEquals("73:2", progressRepository.observePosition().first()?.ayahKey?.value)
    }

    @Test
    fun setReaderMode_updatesUserData() = runTest {
        val viewModel = viewModel(SurahReaderNavKey(surahId = 1))

        viewModel.setReaderMode(ReaderMode.ARABIC_ONLY)
        runCurrent()

        assertEquals(ReaderMode.ARABIC_ONLY, userDataRepository.userData.first().readerMode)
    }
}
