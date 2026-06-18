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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.EmailPreferences
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.testing.util.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Regression coverage for the secondary auth forms (verify-email, forgot-password, reset-password):
 * a 429 must produce a ticking [AuthSubmitState.RateLimited] that counts down to Idle — previously
 * only login/register did this, leaving these forms with a frozen message and an enabled button.
 */
class AuthSecondaryFormRateLimitTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = RateLimitScriptedAuthRepository()

    private fun rateLimited(seconds: Long) = SurauApiException(
        httpStatus = 429,
        code = SurauApiException.CODE_RATE_LIMITED,
        message = "rate limited",
        retryAfterSeconds = seconds,
    )

    @Test
    fun verifyEmail_rateLimited_countsDownThenIdle() = runTest {
        authRepository.error = rateLimited(3)
        val viewModel = VerifyEmailViewModel(authRepository, email = "user@surau.org")

        viewModel.verify("123456")
        runCurrent()

        assertEquals(3, assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value).secondsLeft)
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(2, assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value).secondsLeft)
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(AuthSubmitState.Idle, viewModel.submitState.value)
    }

    @Test
    fun verifyEmail_badCode_mapsToInvalidCode() = runTest {
        authRepository.error = SurauApiException(
            httpStatus = 400,
            code = "AUTH_INVALID_OTP",
            message = "invalid otp",
        )
        val viewModel = VerifyEmailViewModel(authRepository, email = "user@surau.org")

        viewModel.verify("000000")
        runCurrent()

        assertEquals(AuthSubmitState.Error(AuthErrorKind.INVALID_CODE), viewModel.submitState.value)
    }

    @Test
    fun forgotPassword_rateLimited_countsDownThenIdle() = runTest {
        authRepository.error = rateLimited(2)
        val viewModel = ForgotPasswordViewModel(authRepository)

        viewModel.sendResetEmail("user@surau.org")
        runCurrent()

        assertEquals(2, assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value).secondsLeft)
        advanceTimeBy(2_100)
        runCurrent()
        assertEquals(AuthSubmitState.Idle, viewModel.submitState.value)
    }

    @Test
    fun resetPassword_rateLimited_countsDownThenIdle() = runTest {
        authRepository.error = rateLimited(2)
        val viewModel = ResetPasswordViewModel(authRepository)

        viewModel.resetPassword("token", "newpassword1")
        runCurrent()

        assertEquals(2, assertIs<AuthSubmitState.RateLimited>(viewModel.submitState.value).secondsLeft)
        advanceTimeBy(2_100)
        runCurrent()
        assertEquals(AuthSubmitState.Idle, viewModel.submitState.value)
    }
}

/** Scriptable [AuthRepository] that throws [error] from the secondary-form actions under test. */
private class RateLimitScriptedAuthRepository : AuthRepository {
    var error: Exception? = null

    override val authState: Flow<AuthState> = MutableStateFlow(AuthState.Guest)

    override suspend fun verifyEmail(email: String, otp: String) {
        error?.let { throw it }
    }

    override suspend fun forgotPassword(email: String) {
        error?.let { throw it }
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        error?.let { throw it }
    }

    override suspend fun login(email: String, password: String) = Unit
    override suspend fun register(email: String, password: String, displayName: String?) = Unit
    override suspend fun resendVerification(email: String) = Unit
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
