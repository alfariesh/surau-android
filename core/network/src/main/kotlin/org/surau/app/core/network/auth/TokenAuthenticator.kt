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

package org.surau.app.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reacts to 401s on the authenticated client by refreshing the session through
 * [AuthTokenProvider] and retrying with the new access token.
 *
 * Never installed on the public client, and the refresh call itself goes through a bare client —
 * both guards prevent refresh recursion. The prior-response chain is additionally capped so a
 * misbehaving backend can't cause an infinite retry loop.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponseCount() >= MAX_RETRIES) return null

        val failedAccessToken = response.request
            .header("Authorization")
            ?.removePrefix("Bearer ")

        val newAccessToken = runBlocking {
            tokenProvider.refreshTokens(failedAccessToken)
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun Response.priorResponseCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        private const val MAX_RETRIES = 2
    }
}
