/*
 * Copyright 2022 The Android Open Source Project
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

package org.surau.app.feature.settings.impl

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.test.repository.FakeAuthRepository
import org.surau.app.core.data.test.repository.FakeQuranRepository
import org.surau.app.core.data.test.repository.FakeUserDataRepository
import org.surau.app.core.data.test.repository.FakeUserRepository
import org.surau.app.core.datastore.SurauPreferencesDataSource
import org.surau.app.core.datastore.UserPreferences
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.DarkThemeConfig.DARK
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.testing.util.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userDataRepository = FakeUserDataRepository(
        SurauPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance())),
    )
    private val authRepository = FakeAuthRepository()
    private val quranRepository = FakeQuranRepository()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        viewModel = SettingsViewModel(
            userDataRepository = userDataRepository,
            authRepository = authRepository,
            userRepository = FakeUserRepository(),
            quranRepository = quranRepository,
        )
    }

    @Test
    fun stateIsInitiallyLoading() = runTest {
        assertEquals(SettingsUiState.Loading, viewModel.settingsUiState.value)
    }

    @Test
    fun stateIsSuccessAfterUserDataLoaded() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.settingsUiState.collect() }

        userDataRepository.setDarkThemeConfig(DARK)
        userDataRepository.setReaderMode(ReaderMode.ARABIC_ONLY)

        val state = assertIs<SettingsUiState.Success>(viewModel.settingsUiState.value)
        assertEquals(DARK, state.settings.darkThemeConfig)
        assertEquals(ReaderMode.ARABIC_ONLY, state.settings.readerMode)
        assertEquals(AuthState.Guest, state.authState)
    }

    @Test
    fun logout_returnsToGuest() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.settingsUiState.collect() }
        authRepository.login("user@surau.org", "password123")
        userDataRepository.setDarkThemeConfig(DARK)

        var state = assertIs<SettingsUiState.Success>(viewModel.settingsUiState.value)
        assertIs<AuthState.Authenticated>(state.authState)

        viewModel.logout()

        state = assertIs<SettingsUiState.Success>(viewModel.settingsUiState.value)
        assertEquals(AuthState.Guest, state.authState)
    }
}
