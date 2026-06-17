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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.network.auth.AuthTokenProvider
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.auth.RefreshRequestDto
import org.surau.app.core.network.retrofit.SurauAuthApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the OkHttp auth machinery to the persisted session.
 *
 * Refreshes are single-flight: concurrent 401s collapse into one `POST /auth/refresh`, and the
 * rotated pair is persisted before the lock is released — the backend invalidates the previous
 * refresh token on use, so losing the rotated value would sign the user out.
 */
@Singleton
class SessionTokenProvider @Inject constructor(
    private val authSessionDataSource: AuthSessionDataSource,
    private val authApi: SurauAuthApi,
) : AuthTokenProvider {

    private val refreshMutex = Mutex()

    /** Epoch-ms until which refreshing is paused after a 429, honouring the server's Retry-After. */
    private var refreshBlockedUntilMs = 0L

    override suspend fun accessToken(): String? = authSessionDataSource.currentAccessToken()

    override suspend fun refreshTokens(failedAccessToken: String?): String? =
        refreshMutex.withLock {
            // Another caller may have refreshed while we waited on the lock.
            val current = authSessionDataSource.currentAccessToken()
            if (current != null && current != failedAccessToken) {
                return current
            }

            // Respect a server-imposed rate-limit cool-down instead of hammering /auth/refresh.
            if (Clock.System.now().toEpochMilliseconds() < refreshBlockedUntilMs) {
                return null
            }

            val refreshToken = authSessionDataSource.currentRefreshToken() ?: return null

            try {
                val pair = apiCall { authApi.refresh(RefreshRequestDto(refreshToken)) }
                authSessionDataSource.updateTokens(
                    accessToken = pair.accessToken,
                    refreshToken = pair.refreshToken,
                    expiresAtEpochSeconds = Clock.System.now().epochSeconds +
                        pair.expiresInSeconds,
                )
                pair.accessToken
            } catch (exception: SurauApiException) {
                when {
                    // Rate-limited: keep the session and back off for the server's window.
                    exception.isRateLimited -> {
                        val backoffSeconds =
                            exception.retryAfterSeconds ?: DEFAULT_REFRESH_BACKOFF_SECONDS
                        refreshBlockedUntilMs = Clock.System.now().toEpochMilliseconds() +
                            backoffSeconds * MILLIS_PER_SECOND
                        null
                    }
                    // The refresh token was rejected — the session is gone for good.
                    exception.httpStatus in 400..403 -> {
                        authSessionDataSource.clear()
                        null
                    }
                    else -> null
                }
            } catch (_: IOException) {
                // Connectivity problem: keep the session, fail just this request.
                null
            }
        }

    private companion object {
        const val DEFAULT_REFRESH_BACKOFF_SECONDS = 60L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
