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

package org.surau.app.feature.auth.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.BookmarkRepository
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.data.repository.UserRepository
import org.surau.app.core.data.test.repository.FakeBookmarkRepository
import org.surau.app.core.data.test.repository.FakeQuranProgressRepository
import org.surau.app.core.data.test.repository.FakeUserDataRepository
import org.surau.app.core.data.test.repository.FakeUserRepository
import org.surau.app.core.datastore.SurauPreferencesDataSource
import org.surau.app.core.datastore.UserPreferences
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.domain.SyncUserDataAfterLoginUseCase
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.EmailPreferences
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.testing.util.MainDispatcherRule
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Scriptable [AuthRepository] for ViewModel tests. */
private class ScriptedAuthRepository : AuthRepository {
    var loginError: Exception? = null
    var loginCalls = 0

    override val authState: Flow<AuthState> = MutableStateFlow(AuthState.Guest)

    override suspend fun login(email: String, password: String) {
        loginCalls++
        loginError?.let { throw it }
    }

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
    override suspend fun emailPreferences() = EmailPreferences(marketingOptIn = false)
    override suspend fun updateEmailPreferences(marketingOptIn: Boolean) =
        EmailPreferences(marketingOptIn = marketingOptIn)
}

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = ScriptedAuthRepository()
    private val progressRepository: QuranProgressRepository = FakeQuranProgressRepository()
    private val bookmarkRepository: BookmarkRepository = FakeBookmarkRepository()
    private val userRepository: UserRepository = FakeUserRepository()
    private val userDataRepository = FakeUserDataRepository(
        SurauPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance())),
    )

    private fun viewModel() = LoginViewModel(
        authRepository = authRepository,
        syncUserDataAfterLogin = SyncUserDataAfterLoginUseCase(
            userRepository = userRepository,
            quranProgressRepository = progressRepository,
            bookmarkRepository = bookmarkRepository,
        ),
        userDataRepository = userDataRepository,
    )

    @Test
    fun login_success_marksWelcomeShownAndSucceeds() = runTest {
        val viewModel = viewModel()

        viewModel.login("user@surau.org", "password123")
        runCurrent()

        assertEquals(AuthSubmitState.Success, viewModel.submitState.value)
        assertTrue(userDataRepository.userData.first().welcomeShown)
        assertEquals(1, authRepository.loginCalls)
    }

    @Test
    fun login_unverifiedEmail_requiresVerification() = runTest {
        authRepository.loginError = SurauApiException(
            httpStatus = 403,
            code = SurauApiException.CODE_EMAIL_NOT_VERIFIED,
            message = "email not verified",
        )
        val viewModel = viewModel()

        viewModel.login("user@surau.org", "password123")
        runCurrent()

        assertEquals(
            AuthSubmitState.RequiresVerification("user@surau.org"),
            viewModel.submitState.value,
        )
    }

    @Test
    fun login_invalidCredentials_showsError() = runTest {
        authRepository.loginError = SurauApiException(
            httpStatus = 401,
            code = SurauApiException.CODE_INVALID_CREDENTIALS,
            message = "invalid credentials",
        )
        val viewModel = viewModel()

        viewModel.login("user@surau.org", "password123")
        runCurrent()

        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.INVALID_CREDENTIALS),
            viewModel.submitState.value,
        )
    }

    @Test
    fun login_offline_showsOfflineError() = runTest {
        authRepository.loginError = IOException("no network")
        val viewModel = viewModel()

        viewModel.login("user@surau.org", "password123")
        runCurrent()

        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.OFFLINE),
            viewModel.submitState.value,
        )
    }

    @Test
    fun login_rateLimited_countsDownThenIdle() = runTest {
        authRepository.loginError = SurauApiException(
            httpStatus = 429,
            code = SurauApiException.CODE_RATE_LIMITED,
            message = "rate limited",
            retryAfterSeconds = 3,
        )
        val viewModel = viewModel()

        viewModel.login("user@surau.org", "password123")
        runCurrent()

        val limited = assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value)
        assertEquals(3, limited.secondsLeft)

        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(2, assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value).secondsLeft)

        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(AuthSubmitState.Idle, viewModel.submitState.value)
    }
}
