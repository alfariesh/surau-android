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

package org.surau.app.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.UserSession
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.auth.ForgotPasswordRequestDto
import org.surau.app.core.network.model.auth.LoginRequestDto
import org.surau.app.core.network.model.auth.LogoutRequestDto
import org.surau.app.core.network.model.auth.RegisterRequestDto
import org.surau.app.core.network.model.auth.ResendVerificationRequestDto
import org.surau.app.core.network.model.auth.ResetPasswordRequestDto
import org.surau.app.core.network.model.auth.VerifyEmailRequestDto
import org.surau.app.core.network.retrofit.SurauAuthApi
import org.surau.app.core.network.retrofit.SurauUserApi
import javax.inject.Inject

internal class DefaultAuthRepository @Inject constructor(
    private val authApi: SurauAuthApi,
    private val userApi: SurauUserApi,
    private val authSessionDataSource: AuthSessionDataSource,
) : AuthRepository {

    override val authState: Flow<AuthState> = authSessionDataSource.authState

    override suspend fun login(email: String, password: String) {
        val pair = apiCall { authApi.login(LoginRequestDto(email = email, password = password)) }

        // Persist the tokens first so the authenticated client can resolve the identity.
        authSessionDataSource.setSession(
            session = UserSession(
                userId = "",
                email = email,
                displayName = null,
                sessionId = pair.sessionId,
                emailVerified = true,
            ),
            accessToken = pair.accessToken,
            refreshToken = pair.refreshToken,
            expiresAtEpochSeconds = Clock.System.now().epochSeconds + pair.expiresInSeconds,
        )

        // Best effort: enrich the stored identity. Failure here must not fail the login.
        try {
            val identity = apiCall { userApi.introspect() }
            authSessionDataSource.setSession(
                session = UserSession(
                    userId = identity.userId,
                    email = email,
                    displayName = identity.username,
                    sessionId = identity.sessionId ?: pair.sessionId,
                    emailVerified = true,
                ),
                accessToken = pair.accessToken,
                refreshToken = pair.refreshToken,
                expiresAtEpochSeconds = Clock.System.now().epochSeconds + pair.expiresInSeconds,
            )
        } catch (_: Exception) {
            // Keep the minimal session; identity will refresh on the next profile fetch.
        }
    }

    override suspend fun register(email: String, password: String, displayName: String?) {
        apiCall {
            authApi.register(
                RegisterRequestDto(
                    email = email,
                    password = password,
                    displayName = displayName?.takeIf { it.isNotBlank() },
                ),
            )
        }
    }

    override suspend fun verifyEmail(email: String, otp: String) {
        apiCall { authApi.verifyEmail(VerifyEmailRequestDto(email = email, otp = otp)) }
    }

    override suspend fun resendVerification(email: String) {
        apiCall { authApi.resendVerification(ResendVerificationRequestDto(email = email)) }
    }

    override suspend fun forgotPassword(email: String) {
        apiCall { authApi.forgotPassword(ForgotPasswordRequestDto(email = email)) }
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        apiCall { authApi.resetPassword(ResetPasswordRequestDto(token = token, password = newPassword)) }
    }

    override suspend fun logout() {
        val refreshToken = authSessionDataSource.currentRefreshToken()
        try {
            if (refreshToken != null) {
                apiCall { authApi.logout(LogoutRequestDto(refreshToken)) }
            }
        } catch (_: Exception) {
            // Best effort — the local session is cleared regardless.
        } finally {
            authSessionDataSource.clear()
        }
    }
}
