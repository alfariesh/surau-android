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

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.surau.app.core.datastore.AuthSession
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.network.model.auth.ForgotPasswordRequestDto
import org.surau.app.core.network.model.auth.IntrospectDto
import org.surau.app.core.network.model.auth.LoginRequestDto
import org.surau.app.core.network.model.auth.LogoutRequestDto
import org.surau.app.core.network.model.auth.RefreshRequestDto
import org.surau.app.core.network.model.auth.RegisterRequestDto
import org.surau.app.core.network.model.auth.RegisteredUserDto
import org.surau.app.core.network.model.auth.ResendVerificationRequestDto
import org.surau.app.core.network.model.auth.ResetPasswordRequestDto
import org.surau.app.core.network.model.auth.TokenPairDto
import org.surau.app.core.network.model.auth.VerifyEmailRequestDto
import org.surau.app.core.network.model.auth.VerifyEmailResponseDto
import org.surau.app.core.network.model.user.OnboardingRequestDto
import org.surau.app.core.network.model.user.PreferencesPatchRequestDto
import org.surau.app.core.network.model.user.UserAccountDto
import org.surau.app.core.network.retrofit.SurauAuthApi
import org.surau.app.core.network.retrofit.SurauUserApi
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultAuthRepositoryTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var authApi: FakeSurauAuthApi
    private lateinit var userApi: FakeSurauUserApi
    private lateinit var authSession: AuthSessionDataSource
    private lateinit var subject: DefaultAuthRepository

    @Before
    fun setup() {
        authApi = FakeSurauAuthApi()
        userApi = FakeSurauUserApi()
        authSession = AuthSessionDataSource(InMemoryDataStore(AuthSession.getDefaultInstance()))
        subject = DefaultAuthRepository(authApi, userApi, authSession)
    }

    @Test
    fun login_persistsSession_thenEnrichesFromIntrospect() = testScope.runTest {
        authApi.loginResponse = TokenPairDto(
            accessToken = "access",
            refreshToken = "refresh",
            sessionId = "login-session",
            expiresInSeconds = 900,
        )
        userApi.introspectResponse = IntrospectDto(
            userId = "user-1",
            username = "Aisyah",
            sessionId = "introspect-session",
        )

        subject.login("user@surau.org", "secret")

        val state = subject.authState.first()
        assertIs<AuthState.Authenticated>(state)
        assertEquals("user-1", state.session.userId)
        assertEquals("Aisyah", state.session.displayName)
        assertEquals("introspect-session", state.session.sessionId)
        assertEquals("user@surau.org", state.session.email)
        assertEquals("access", authSession.currentAccessToken())
        assertEquals("refresh", authSession.currentRefreshToken())
    }

    @Test
    fun login_whenIntrospectFails_keepsMinimalSession() = testScope.runTest {
        authApi.loginResponse = TokenPairDto(
            accessToken = "access",
            refreshToken = "refresh",
            sessionId = "login-session",
        )
        userApi.introspectError = IOException("offline")

        subject.login("user@surau.org", "secret")

        val state = subject.authState.first()
        assertIs<AuthState.Authenticated>(state)
        // Minimal session retained from the token response; identity not enriched.
        assertEquals("", state.session.userId)
        assertNull(state.session.displayName)
        assertEquals("login-session", state.session.sessionId)
        assertEquals("user@surau.org", state.session.email)
        assertEquals("refresh", authSession.currentRefreshToken())
    }

    @Test
    fun logout_clearsLocalSession_evenWhenApiFails() = testScope.runTest {
        authApi.loginResponse = TokenPairDto(accessToken = "access", refreshToken = "refresh")
        subject.login("user@surau.org", "secret")
        authApi.logoutError = IOException("offline")

        subject.logout()

        // The API was attempted with the stored refresh token, yet the session is cleared anyway.
        assertEquals(listOf("refresh"), authApi.loggedOutRefreshTokens)
        assertEquals(AuthState.Guest, subject.authState.first())
        assertNull(authSession.currentAccessToken())
    }

    @Test
    fun logout_whenGuest_doesNotCallApi() = testScope.runTest {
        subject.logout()

        assertTrue(authApi.loggedOutRefreshTokens.isEmpty())
        assertEquals(AuthState.Guest, subject.authState.first())
    }
}

private class FakeSurauAuthApi : SurauAuthApi {
    var loginResponse: TokenPairDto = TokenPairDto(accessToken = "access", refreshToken = "refresh")
    var logoutError: Exception? = null
    val loggedOutRefreshTokens = mutableListOf<String>()

    override suspend fun login(body: LoginRequestDto): TokenPairDto = loginResponse

    override suspend fun logout(body: LogoutRequestDto) {
        // Record the attempt before failing, so tests can assert the call was made.
        loggedOutRefreshTokens += body.refreshToken
        logoutError?.let { throw it }
    }

    override suspend fun register(body: RegisterRequestDto): RegisteredUserDto =
        throw NotImplementedError()

    override suspend fun refresh(body: RefreshRequestDto): TokenPairDto = throw NotImplementedError()

    override suspend fun verifyEmail(body: VerifyEmailRequestDto): VerifyEmailResponseDto =
        throw NotImplementedError()

    override suspend fun resendVerification(body: ResendVerificationRequestDto) =
        throw NotImplementedError()

    override suspend fun forgotPassword(body: ForgotPasswordRequestDto) = throw NotImplementedError()

    override suspend fun resetPassword(body: ResetPasswordRequestDto) = throw NotImplementedError()
}

private class FakeSurauUserApi : SurauUserApi {
    var introspectResponse: IntrospectDto = IntrospectDto(userId = "user-1")
    var introspectError: Exception? = null

    override suspend fun introspect(): IntrospectDto =
        introspectError?.let { throw it } ?: introspectResponse

    override suspend fun profile(): UserAccountDto = throw NotImplementedError()

    override suspend fun completeOnboarding(body: OnboardingRequestDto): UserAccountDto =
        throw NotImplementedError()

    override suspend fun patchPreferences(body: PreferencesPatchRequestDto): UserAccountDto =
        throw NotImplementedError()
}
