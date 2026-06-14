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

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.test.QuranTestData
import org.surau.app.core.data.test.repository.FakeQuranAudioRepository
import org.surau.app.core.data.test.repository.FakeQuranRepository
import org.surau.app.core.data.test.repository.FakeUserDataRepository
import org.surau.app.core.datastore.SurauPreferencesDataSource
import org.surau.app.core.datastore.UserPreferences
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.domain.GetReaderContentUseCase
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.media.RepeatScope
import org.surau.app.core.media.SleepTimerOption
import org.surau.app.core.testing.util.MainDispatcherRule
import org.surau.app.feature.quran.api.navigation.SurahFlowNavKey
import java.io.IOException
import kotlin.test.assertEquals

class SurahFlowViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val quranRepository = FakeQuranRepository()
    private val audioRepository = FakeQuranAudioRepository()
    private val playerController = FakeSurauPlayerController()
    private val userDataRepository = FakeUserDataRepository(
        SurauPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance())),
    )

    private fun viewModel(navKey: SurahFlowNavKey) = SurahFlowViewModel(
        getReaderContent = GetReaderContentUseCase(
            quranRepository = quranRepository,
            userDataRepository = userDataRepository,
        ),
        quranAudioRepository = audioRepository,
        userDataRepository = userDataRepository,
        playerController = playerController,
        navKey = navKey,
    )

    @Test
    fun init_autoplaysSurahModeManifestAtStartAyah() = runTest {
        audioRepository.manifest = QuranTestData.fatihahSurahModeManifest

        val viewModel = viewModel(SurahFlowNavKey(surahId = 1, ayahNumber = 3))
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(1, playerController.playCalls.size)
        val call = playerController.playCalls.first()
        assertEquals("surah", call.manifest.mode)
        assertEquals(3, call.startAyah)
    }

    @Test
    fun init_whenManifestUnavailable_doesNotPlay() = runTest {
        audioRepository.manifestError = IOException("offline")

        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(0, playerController.playCalls.size)
    }

    @Test
    fun init_doesNotReplayActiveSurahSession() = runTest {
        playerController.playerState.value = PlayerUiState(surahId = 1)

        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(0, playerController.playCalls.size)
    }

    @Test
    fun playingAyah_gatedToThisSurah() = runTest {
        playerController.playerState.value = PlayerUiState(surahId = 1, currentAyahNumber = 4)

        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))

        assertEquals(4, viewModel.playingAyah.first { it != null })
    }

    @Test
    fun setFontScale_persists() = runTest {
        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))

        viewModel.setFontScale(1.6f)
        advanceUntilIdle()

        assertEquals(1.6f, userDataRepository.userData.first().flowArabicFontScale, 0.001f)
    }

    @Test
    fun surahEnds_autoContinuesToNextSurah() = runTest {
        audioRepository.manifest = QuranTestData.fatihahSurahModeManifest
        viewModel(SurahFlowNavKey(surahId = 1))
        advanceUntilIdle() // init plays surah 1

        playerController.surahCompletionEvents.tryEmit(1)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), audioRepository.audioManifestCalls)
        assertEquals(2, playerController.playCalls.size)
        assertEquals(1, playerController.playCalls[1].startAyah)
    }

    @Test
    fun surahEnds_doesNotAdvance_whenAutoContinueOff() = runTest {
        userDataRepository.setFlowAutoContinue(false)
        viewModel(SurahFlowNavKey(surahId = 1))
        advanceUntilIdle()

        playerController.surahCompletionEvents.tryEmit(1)
        advanceUntilIdle()

        assertEquals(listOf(1), audioRepository.audioManifestCalls)
    }

    @Test
    fun surahEnds_doesNotAdvance_pastLastSurah() = runTest {
        viewModel(SurahFlowNavKey(surahId = 114))
        advanceUntilIdle()

        playerController.surahCompletionEvents.tryEmit(114)
        advanceUntilIdle()

        assertEquals(listOf(114), audioRepository.audioManifestCalls)
    }

    @Test
    fun surahEnds_ignoresCompletionForADifferentSurah() = runTest {
        viewModel(SurahFlowNavKey(surahId = 1))
        advanceUntilIdle()

        playerController.surahCompletionEvents.tryEmit(73) // not the shown surah
        advanceUntilIdle()

        assertEquals(listOf(1), audioRepository.audioManifestCalls)
    }

    @Test
    fun toggleAutoContinue_persists() = runTest {
        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))

        viewModel.toggleAutoContinue() // default is on → turns off
        advanceUntilIdle()

        assertEquals(false, userDataRepository.userData.first().flowAutoContinue)
    }

    @Test
    fun onSetRepeat_forwardsToController() = runTest {
        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))

        viewModel.onSetRepeat(RepeatScope.AYAH, count = 3)

        assertEquals(
            FakeSurauPlayerController.RepeatCall(RepeatScope.AYAH, 3),
            playerController.repeatCalls.single(),
        )
    }

    @Test
    fun onSetSleepTimer_forwardsToController() = runTest {
        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))

        viewModel.onSetSleepTimer(SleepTimerOption.EndOfSurah)

        assertEquals(SleepTimerOption.EndOfSurah, playerController.sleepTimerCalls.single())
    }

    @Test
    fun onSetSpeed_forwardsToController() = runTest {
        val viewModel = viewModel(SurahFlowNavKey(surahId = 1))

        viewModel.onSetSpeed(1.25f)

        assertEquals(1.25f, playerController.speedCalls.single())
    }
}
