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
import org.surau.app.core.network.model.auth.RefreshRequestDto
import org.surau.app.core.network.retrofit.SurauAuthApi
import retrofit2.HttpException
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

    override suspend fun accessToken(): String? = authSessionDataSource.currentAccessToken()

    override suspend fun refreshTokens(failedAccessToken: String?): String? =
        refreshMutex.withLock {
            // Another caller may have refreshed while we waited on the lock.
            val current = authSessionDataSource.currentAccessToken()
            if (current != null && current != failedAccessToken) {
                return current
            }

            val refreshToken = authSessionDataSource.currentRefreshToken() ?: return null

            try {
                val pair = authApi.refresh(RefreshRequestDto(refreshToken))
                authSessionDataSource.updateTokens(
                    accessToken = pair.accessToken,
                    refreshToken = pair.refreshToken,
                    expiresAtEpochSeconds = Clock.System.now().epochSeconds +
                        pair.expiresInSeconds,
                )
                pair.accessToken
            } catch (exception: HttpException) {
                if (exception.code() in 400..403) {
                    // The refresh token was rejected — the session is gone for good.
                    authSessionDataSource.clear()
                }
                null
            } catch (_: IOException) {
                // Connectivity problem: keep the session, fail just this request.
                null
            }
        }
}
