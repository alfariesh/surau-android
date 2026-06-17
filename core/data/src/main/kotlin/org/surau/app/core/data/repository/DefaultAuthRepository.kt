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
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.surau.app.core.common.coroutines.runCatchingExceptCancellation
import org.surau.app.core.data.util.TimeZoneMonitor
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.EmailPreferences
import org.surau.app.core.model.data.auth.UserSession
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.auth.ChangeEmailRequestDto
import org.surau.app.core.network.model.auth.ChangeEmailVerifyRequestDto
import org.surau.app.core.network.model.auth.ChangePasswordRequestDto
import org.surau.app.core.network.model.auth.DeleteAccountRequestDto
import org.surau.app.core.network.model.auth.ForgotPasswordRequestDto
import org.surau.app.core.network.model.auth.LoginRequestDto
import org.surau.app.core.network.model.auth.LogoutRequestDto
import org.surau.app.core.network.model.auth.RegisterRequestDto
import org.surau.app.core.network.model.auth.ResendVerificationRequestDto
import org.surau.app.core.network.model.auth.ResetPasswordRequestDto
import org.surau.app.core.network.model.auth.SessionDto
import org.surau.app.core.network.model.auth.TokenPairDto
import org.surau.app.core.network.model.auth.VerifyEmailRequestDto
import org.surau.app.core.network.model.user.EmailPreferencesDto
import org.surau.app.core.network.model.user.EmailPreferencesPatchRequestDto
import org.surau.app.core.network.model.user.ProfilePatchRequestDto
import org.surau.app.core.network.retrofit.SurauAccountApi
import org.surau.app.core.network.retrofit.SurauAuthApi
import org.surau.app.core.network.retrofit.SurauUserApi
import javax.inject.Inject

internal class DefaultAuthRepository @Inject constructor(
    private val authApi: SurauAuthApi,
    private val userApi: SurauUserApi,
    private val accountApi: SurauAccountApi,
    private val authSessionDataSource: AuthSessionDataSource,
    private val timeZoneMonitor: TimeZoneMonitor,
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

        // Best effort: enrich the stored identity. Failure here must not fail the login, but
        // cancellation must still propagate (runCatchingExceptCancellation rethrows it).
        runCatchingExceptCancellation {
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
            // Best effort — the local session is cleared regardless (finally). Cancellation still
            // propagates so a cancelled logout doesn't look like a completed one.
            runCatchingExceptCancellation {
                if (refreshToken != null) {
                    apiCall { authApi.logout(LogoutRequestDto(refreshToken)) }
                }
            }
        } finally {
            authSessionDataSource.clear()
        }
    }

    override suspend fun updateProfile(displayName: String?, countryCode: String?) {
        val timezone = timeZoneMonitor.currentTimeZone.first().id
        val account = apiCall {
            userApi.updateProfile(
                ProfilePatchRequestDto(
                    displayName = displayName?.takeIf { it.isNotBlank() },
                    timezone = timezone,
                    countryCode = countryCode?.takeIf { it.isNotBlank() },
                ),
            )
        }
        authSessionDataSource.updateDisplayName(account.profile.displayName)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        val pair = apiCall {
            accountApi.changePassword(
                ChangePasswordRequestDto(currentPassword = currentPassword, newPassword = newPassword),
            )
        }
        persistRotatedSession(pair)
    }

    override suspend fun requestEmailChange(currentPassword: String, newEmail: String) {
        apiCall {
            accountApi.requestEmailChange(
                ChangeEmailRequestDto(currentPassword = currentPassword, newEmail = newEmail),
            )
        }
    }

    override suspend fun verifyEmailChange(newEmail: String, otp: String) {
        val pair = apiCall {
            accountApi.verifyEmailChange(ChangeEmailVerifyRequestDto(otp = otp))
        }
        persistRotatedSession(pair)
        authSessionDataSource.updateEmail(newEmail)
    }

    override suspend fun listSessions(): List<AccountSession> =
        apiCall { accountApi.listSessions() }.items.map { it.toAccountSession() }

    override suspend fun revokeSession(sessionId: String) {
        apiCall { accountApi.revokeSession(sessionId) }
    }

    override suspend fun logoutAllDevices() {
        // The current session is revoked too — only clear locally once the call succeeds.
        apiCall { accountApi.logoutAll() }
        authSessionDataSource.clear()
    }

    override suspend fun deleteAccount(currentPassword: String) {
        // A wrong password is a 401 the caller can retry, so clear only on success.
        apiCall { accountApi.deleteAccount(DeleteAccountRequestDto(currentPassword = currentPassword)) }
        authSessionDataSource.clear()
    }

    override suspend fun emailPreferences(): EmailPreferences =
        apiCall { userApi.emailPreferences() }.toEmailPreferences()

    override suspend fun updateEmailPreferences(marketingOptIn: Boolean): EmailPreferences =
        apiCall {
            userApi.updateEmailPreferences(
                EmailPreferencesPatchRequestDto(marketingOptIn = marketingOptIn),
            )
        }.toEmailPreferences()

    /** Persists a token pair from a session-rotating endpoint, keeping the rotated session id. */
    private suspend fun persistRotatedSession(pair: TokenPairDto) {
        authSessionDataSource.updateTokens(
            accessToken = pair.accessToken,
            refreshToken = pair.refreshToken,
            expiresAtEpochSeconds = Clock.System.now().epochSeconds + pair.expiresInSeconds,
            sessionId = pair.sessionId.ifEmpty { null },
        )
    }
}

private fun SessionDto.toAccountSession(): AccountSession = AccountSession(
    id = id,
    userAgent = userAgent,
    clientIp = clientIp,
    createdAt = createdAt.toInstantOrNull(),
    lastUsedAt = lastUsedAt.toInstantOrNull(),
    expiresAt = expiresAt.toInstantOrNull(),
    isCurrent = isCurrent,
)

private fun EmailPreferencesDto.toEmailPreferences(): EmailPreferences =
    EmailPreferences(marketingOptIn = marketingOptIn)

private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }
