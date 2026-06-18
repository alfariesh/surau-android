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

package org.surau.app.ui.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.QuranAudioRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.QuranSearchResult
import org.surau.app.core.data.repository.UserRepository
import org.surau.app.core.data.test.repository.FakeQuranDownloadManager
import org.surau.app.core.data.util.QuranDownloadManager
import org.surau.app.core.model.data.ThemePalette
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.EmailPreferences
import org.surau.app.core.model.data.quran.JuzSegment
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.Surah
import org.surau.app.core.model.data.quran.SurahAudioManifest
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.core.testing.repository.TestUserDataRepository
import org.surau.app.core.testing.util.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updateThemePalette_clearsCustomSeedAndDynamicColor() = runTest {
        val userDataRepository = TestUserDataRepository()
        userDataRepository.setDynamicColorPreference(true)
        userDataRepository.setSeedColor(0xFF0E7C86L)
        val viewModel = profileViewModel(userDataRepository)

        viewModel.updateThemePalette(ThemePalette.AIRBNB)
        advanceUntilIdle()

        val userData = userDataRepository.userData.first()
        assertEquals(ThemePalette.AIRBNB, userData.themePalette)
        assertEquals(0L, userData.seedColorArgb)
        assertFalse(userData.useDynamicColor)
    }

    @Test
    fun startQuranDownload_delegatesToManager() = runTest {
        val downloadManager = FakeQuranDownloadManager()
        val viewModel = profileViewModel(TestUserDataRepository(), downloadManager)

        viewModel.startQuranDownload()

        assertEquals(1, downloadManager.startCount)
    }

    @Test
    fun cancelQuranDownload_delegatesToManager() = runTest {
        val downloadManager = FakeQuranDownloadManager()
        val viewModel = profileViewModel(TestUserDataRepository(), downloadManager)

        viewModel.cancelQuranDownload()

        assertEquals(1, downloadManager.cancelCount)
    }

    private fun profileViewModel(
        userDataRepository: TestUserDataRepository,
        quranDownloadManager: QuranDownloadManager = FakeQuranDownloadManager(),
    ) = ProfileViewModel(
        userDataRepository = userDataRepository,
        authRepository = FakeAuthRepository(),
        userRepository = FakeUserRepository(),
        quranDownloadManager = quranDownloadManager,
        quranRepository = FakeQuranRepository(),
        quranAudioRepository = FakeQuranAudioRepository(),
    )
}

private class FakeAuthRepository : AuthRepository {
    override val authState: Flow<AuthState> = flowOf(AuthState.Guest)
    override suspend fun login(email: String, password: String) = Unit
    override suspend fun register(email: String, password: String, displayName: String?) = Unit
    override suspend fun verifyEmail(email: String, otp: String) = Unit
    override suspend fun resendVerification(email: String) = Unit
    override suspend fun forgotPassword(email: String) = Unit
    override suspend fun resetPassword(token: String, newPassword: String) = Unit
    override suspend fun logout() = Unit
    override suspend fun updateProfile(displayName: String?, countryCode: String?) = Unit
    override suspend fun changePassword(currentPassword: String, newPassword: String) = Unit
    override suspend fun requestEmailChange(currentPassword: String, newEmail: String) = Unit
    override suspend fun verifyEmailChange(newEmail: String, otp: String) = Unit
    override suspend fun verifyEmailWithToken(token: String) = Unit
    override suspend fun verifyEmailChangeWithToken(token: String) = Unit
    override suspend fun listSessions(): List<AccountSession> = emptyList()
    override suspend fun revokeSession(sessionId: String) = Unit
    override suspend fun logoutAllDevices() = Unit
    override suspend fun deleteAccount(currentPassword: String) = Unit
    override suspend fun emailPreferences(): EmailPreferences = EmailPreferences(marketingOptIn = false)
    override suspend fun updateEmailPreferences(marketingOptIn: Boolean): EmailPreferences =
        EmailPreferences(marketingOptIn)
}

private class FakeUserRepository : UserRepository {
    override suspend fun pullPreferencesIntoSettings() = Unit
    override suspend fun pushReaderPreferences() = Unit
    override suspend fun completeOnboardingIfNeeded() = Unit
}

private class FakeQuranRepository : QuranRepository {
    override fun observeSurahs(): Flow<List<Surah>> = flowOf(emptyList())
    override fun observeSurah(surahId: Int): Flow<Surah?> = flowOf(null)
    override fun observeJuzList(): Flow<List<JuzSegment>> = flowOf(emptyList())
    override fun observeTranslationSources(): Flow<List<TranslationSource>> = flowOf(emptyList())
    override fun observeAyahs(
        surahId: Int,
        translationSourceId: String,
    ): Flow<List<PopulatedAyah>> = flowOf(emptyList())

    override suspend fun ensureSurahCached(
        surahId: Int,
        translationSourceId: String,
        allowStaleOnError: Boolean,
    ) = Unit
    override suspend fun resolveTranslationSourceId(preferredId: String?): String = preferredId ?: "default"
    override suspend fun search(query: String, translationSourceId: String?): List<QuranSearchResult> = emptyList()
}

private class FakeQuranAudioRepository : QuranAudioRepository {
    override fun observeRecitations(): Flow<List<Recitation>> = flowOf(emptyList())
    override suspend fun resolveRecitationId(preferredId: String?): String? = preferredId
    override suspend fun resolveRecitationId(preferredId: String?, requiredMode: String): String? = preferredId
    override suspend fun audioManifest(surahId: Int, recitationId: String?): SurahAudioManifest =
        error("Not used in ProfileViewModelTest")
}
