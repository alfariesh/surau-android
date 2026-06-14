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

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.testing.util.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun invalidPassword() = SurauApiException(
    httpStatus = 401,
    code = SurauApiException.CODE_INVALID_CREDENTIALS,
    message = "invalid credentials",
)

class ChangePasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val authRepository = FakeAccountAuthRepository()

    @Test
    fun success_emitsSuccess() = runTest {
        val viewModel = ChangePasswordViewModel(authRepository)
        viewModel.changePassword("oldsecret1", "newsecret1")
        runCurrent()
        assertEquals(AuthSubmitState.Success, viewModel.submitState.value)
    }

    @Test
    fun wrongPassword_showsInvalidPassword() = runTest {
        authRepository.error = invalidPassword()
        val viewModel = ChangePasswordViewModel(authRepository)
        viewModel.changePassword("wrong", "newsecret1")
        runCurrent()
        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.INVALID_PASSWORD),
            viewModel.submitState.value,
        )
    }

    @Test
    fun rateLimited_countsDown() = runTest {
        authRepository.error = SurauApiException(
            httpStatus = 429,
            code = SurauApiException.CODE_RATE_LIMITED,
            message = "rate limited",
            retryAfterSeconds = 3,
        )
        val viewModel = ChangePasswordViewModel(authRepository)
        viewModel.changePassword("oldsecret1", "newsecret1")
        runCurrent()
        assertEquals(3, assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value).secondsLeft)
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(2, assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value).secondsLeft)
    }
}

class ChangeEmailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val authRepository = FakeAccountAuthRepository()

    @Test
    fun request_advancesToOtpPhase() = runTest {
        val viewModel = ChangeEmailViewModel(authRepository)
        viewModel.requestChange("secret123", " new@surau.org ")
        runCurrent()
        assertTrue(viewModel.awaitingOtp.value)
        assertEquals("new@surau.org", viewModel.newEmail.value)
        assertEquals(AuthSubmitState.Idle, viewModel.submitState.value)
    }

    @Test
    fun request_emailExists_showsEmailExists() = runTest {
        authRepository.error = SurauApiException(httpStatus = 409, code = null, message = "user already exists")
        val viewModel = ChangeEmailViewModel(authRepository)
        viewModel.requestChange("secret123", "taken@surau.org")
        runCurrent()
        assertEquals(AuthSubmitState.Error(AuthErrorKind.EMAIL_EXISTS), viewModel.submitState.value)
        assertTrue(!viewModel.awaitingOtp.value)
    }

    @Test
    fun verify_usesPendingEmail_andSucceeds() = runTest {
        val viewModel = ChangeEmailViewModel(authRepository)
        viewModel.requestChange("secret123", "new@surau.org")
        runCurrent()
        viewModel.verify("123456")
        runCurrent()
        assertEquals(AuthSubmitState.Success, viewModel.submitState.value)
        assertEquals("new@surau.org", authRepository.lastVerifiedEmail)
    }

    @Test
    fun request_emailDeliveryFailed_showsDeliveryError() = runTest {
        authRepository.error = SurauApiException(httpStatus = 503, code = null, message = "email delivery failed")
        val viewModel = ChangeEmailViewModel(authRepository)
        viewModel.requestChange("secret123", "new@surau.org")
        runCurrent()
        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.EMAIL_DELIVERY_FAILED),
            viewModel.submitState.value,
        )
        assertTrue(!viewModel.awaitingOtp.value)
    }

    @Test
    fun verify_wrongCode_showsInvalidCode() = runTest {
        val viewModel = ChangeEmailViewModel(authRepository)
        viewModel.requestChange("secret123", "new@surau.org")
        runCurrent()
        authRepository.error = SurauApiException(httpStatus = 400, code = null, message = "invalid verification token")
        viewModel.verify("000000")
        runCurrent()
        assertEquals(AuthSubmitState.Error(AuthErrorKind.INVALID_CODE), viewModel.submitState.value)
    }

    @Test
    fun restart_returnsToRequestForm() = runTest {
        val viewModel = ChangeEmailViewModel(authRepository)
        viewModel.requestChange("secret123", "new@surau.org")
        runCurrent()
        assertTrue(viewModel.awaitingOtp.value)
        viewModel.restartEmailChange()
        assertTrue(!viewModel.awaitingOtp.value)
        assertEquals(AuthSubmitState.Idle, viewModel.submitState.value)
    }
}

class DeleteAccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val authRepository = FakeAccountAuthRepository()

    @Test
    fun success_emitsSuccess() = runTest {
        val viewModel = DeleteAccountViewModel(authRepository)
        viewModel.deleteAccount("secret123")
        runCurrent()
        assertEquals(AuthSubmitState.Success, viewModel.submitState.value)
    }

    @Test
    fun wrongPassword_showsInvalidPassword() = runTest {
        authRepository.error = invalidPassword()
        val viewModel = DeleteAccountViewModel(authRepository)
        viewModel.deleteAccount("wrong")
        runCurrent()
        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.INVALID_PASSWORD),
            viewModel.submitState.value,
        )
    }
}

class SessionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val authRepository = FakeAccountAuthRepository()

    private val sampleSessions = listOf(
        AccountSession("s1", "Pixel 8", "203.0.113.1", null, null, null, isCurrent = true),
        AccountSession("s2", "Chrome", "203.0.113.2", null, null, null, isCurrent = false),
    )

    @Test
    fun load_success_rendersSessions() = runTest {
        authRepository.sessions = sampleSessions
        val viewModel = SessionsViewModel(authRepository)
        runCurrent()
        assertEquals(sampleSessions, assertIs<SessionsUiState.Success>(viewModel.uiState.value).sessions)
    }

    @Test
    fun load_error_showsError() = runTest {
        authRepository.error = SurauApiException(httpStatus = 500, code = null, message = "boom")
        val viewModel = SessionsViewModel(authRepository)
        runCurrent()
        assertEquals(SessionsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun revoke_callsRepository() = runTest {
        authRepository.sessions = sampleSessions
        val viewModel = SessionsViewModel(authRepository)
        runCurrent()
        viewModel.revoke("s2")
        runCurrent()
        assertEquals(listOf("s2"), authRepository.revokedSessionIds)
    }
}

class EmailPreferencesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val authRepository = FakeAccountAuthRepository()

    @Test
    fun load_reflectsBackendValue() = runTest {
        authRepository.marketingOptIn = true
        val viewModel = EmailPreferencesViewModel(authRepository)
        runCurrent()
        assertTrue(assertIs<EmailPrefsUiState.Success>(viewModel.uiState.value).marketingOptIn)
    }

    @Test
    fun toggle_persistsOptIn() = runTest {
        val viewModel = EmailPreferencesViewModel(authRepository)
        runCurrent()
        viewModel.setMarketingOptIn(true)
        runCurrent()
        assertTrue(assertIs<EmailPrefsUiState.Success>(viewModel.uiState.value).marketingOptIn)
        assertTrue(authRepository.marketingOptIn)
    }
}

class AccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val authRepository = FakeAccountAuthRepository()

    @Test
    fun logoutAllDevices_signsOut() = runTest {
        val viewModel = AccountViewModel(authRepository)
        viewModel.logoutAllDevices()
        runCurrent()
        assertTrue(viewModel.signedOut.value)
        assertEquals(1, authRepository.logoutAllCalls)
    }
}

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val authRepository = FakeAccountAuthRepository()

    @Test
    fun save_sendsTrimmedValues_andSucceeds() = runTest {
        val viewModel = ProfileViewModel(authRepository)
        viewModel.save(" Umar ", " ID ")
        runCurrent()
        assertEquals(AuthSubmitState.Success, viewModel.submitState.value)
        assertEquals("Umar" to "ID", authRepository.lastProfile)
    }
}
