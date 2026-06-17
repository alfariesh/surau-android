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

package org.surau.app.core.data.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.surau.app.core.datastore.AuthSession
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.auth.UserSession
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.network.model.auth.ForgotPasswordRequestDto
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
import org.surau.app.core.network.retrofit.SurauAuthApi
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the single-flight rotating-refresh logic — the most data-loss-sensitive code in M1 auth.
 * The rotated refresh token is single-use server-side, so losing it (or refreshing twice) signs the
 * user out. These tests drive the real [SessionTokenProvider] against a real [AuthSessionDataSource]
 * backed by an in-memory store, with a fake refresh API.
 */
class SessionTokenProviderTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val authSession = AuthSessionDataSource(InMemoryDataStore(AuthSession.getDefaultInstance()))
    private val api = FakeRefreshApi()
    private val subject = SessionTokenProvider(authSession, api)

    @Test
    fun concurrentRefresh_collapsesToSingleNetworkCall() = testScope.runTest {
        seedSession(access = "stale", refresh = "r0")
        api.refreshDelayMs = 100 // hold the lock so the other callers must queue
        api.nextPair = TokenPairDto(accessToken = "a1", refreshToken = "r1", expiresInSeconds = 900)

        val results = (1..4).map { async { subject.refreshTokens("stale") } }.awaitAll()

        assertEquals(1, api.refreshCallCount, "concurrent 401s must trigger exactly one refresh")
        assertTrue(results.all { it == "a1" }, "every caller reuses the freshly rotated access token")
        assertEquals("a1", authSession.currentAccessToken())
        assertEquals("r1", authSession.currentRefreshToken())
    }

    @Test
    fun rotatedRefreshToken_isPersisted_andUsedOnNextRefresh() = testScope.runTest {
        seedSession(access = "stale", refresh = "r0")

        api.nextPair = TokenPairDto(accessToken = "a1", refreshToken = "r1")
        assertEquals("a1", subject.refreshTokens("stale"))
        assertEquals("r0", api.lastRefreshTokenSent)
        assertEquals("r1", authSession.currentRefreshToken())

        // The access token "a1" is now itself stale; the next refresh must send the rotated r1.
        api.nextPair = TokenPairDto(accessToken = "a2", refreshToken = "r2")
        assertEquals("a2", subject.refreshTokens("a1"))
        assertEquals("r1", api.lastRefreshTokenSent)
        assertEquals("r2", authSession.currentRefreshToken())
    }

    @Test
    fun refreshRejectedWith401_clearsSession() = testScope.runTest {
        seedSession(access = "stale", refresh = "r0")
        api.error = SurauApiException(
            httpStatus = 401,
            code = SurauApiException.CODE_INVALID_CREDENTIALS,
            message = "invalid refresh token",
        )

        val result = subject.refreshTokens("stale")

        assertNull(result)
        assertNull(authSession.currentAccessToken(), "a rejected refresh token signs the user out")
        assertNull(authSession.currentRefreshToken())
    }

    @Test
    fun refreshFailsWithIoError_keepsSession() = testScope.runTest {
        seedSession(access = "stale", refresh = "r0")
        api.error = IOException("offline")

        val result = subject.refreshTokens("stale")

        assertNull(result)
        // A transient connectivity error must not clear the session.
        assertEquals("r0", authSession.currentRefreshToken())
        assertEquals("stale", authSession.currentAccessToken())
    }

    @Test
    fun refreshRateLimited_keepsSession_andSkipsFurtherRefreshDuringCooldown() = testScope.runTest {
        seedSession(access = "stale", refresh = "r0")
        api.error = SurauApiException(
            httpStatus = 429,
            code = SurauApiException.CODE_RATE_LIMITED,
            message = "rate limited",
            retryAfterSeconds = 60,
        )

        assertNull(subject.refreshTokens("stale"))
        assertEquals("r0", authSession.currentRefreshToken(), "rate limit must not clear the session")
        assertEquals(1, api.refreshCallCount)

        // Within the cool-down window a second refresh must NOT hit the network again.
        api.error = null
        api.nextPair = TokenPairDto(accessToken = "a1", refreshToken = "r1")
        assertNull(subject.refreshTokens("stale"))
        assertEquals(1, api.refreshCallCount, "refresh is suppressed during the Retry-After window")
    }

    @Test
    fun refreshWithNoSession_returnsNull_withoutCallingApi() = testScope.runTest {
        val result = subject.refreshTokens("stale")

        assertNull(result)
        assertEquals(0, api.refreshCallCount)
    }

    private suspend fun seedSession(access: String, refresh: String) {
        authSession.setSession(
            session = UserSession(
                userId = "user-1",
                email = "user@surau.org",
                displayName = null,
                sessionId = "session-1",
                emailVerified = true,
            ),
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochSeconds = 0,
        )
    }
}

private class FakeRefreshApi : SurauAuthApi {
    var nextPair: TokenPairDto = TokenPairDto(accessToken = "a1", refreshToken = "r1")
    var error: Exception? = null
    var refreshDelayMs: Long = 0
    var refreshCallCount = 0
    var lastRefreshTokenSent: String? = null

    override suspend fun refresh(body: RefreshRequestDto): TokenPairDto {
        refreshCallCount++
        lastRefreshTokenSent = body.refreshToken
        if (refreshDelayMs > 0) delay(refreshDelayMs)
        error?.let { throw it }
        return nextPair
    }

    override suspend fun register(body: RegisterRequestDto): RegisteredUserDto =
        throw NotImplementedError()

    override suspend fun login(body: LoginRequestDto): TokenPairDto = throw NotImplementedError()

    override suspend fun logout(body: LogoutRequestDto) = throw NotImplementedError()

    override suspend fun verifyEmail(body: VerifyEmailRequestDto): VerifyEmailResponseDto =
        throw NotImplementedError()

    override suspend fun resendVerification(body: ResendVerificationRequestDto) =
        throw NotImplementedError()

    override suspend fun forgotPassword(body: ForgotPasswordRequestDto) = throw NotImplementedError()

    override suspend fun resetPassword(body: ResetPasswordRequestDto) = throw NotImplementedError()
}
