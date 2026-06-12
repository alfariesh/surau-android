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

/**
 * Supplies access tokens to the authenticated OkHttp client.
 *
 * Implemented in `core:data`, which owns token storage and the refresh policy. The implementation
 * must make [refreshTokens] single-flight (one refresh at a time) and must persist the rotated
 * refresh token before returning — the backend invalidates the old refresh token on use.
 */
interface AuthTokenProvider {

    /** The current access token, or `null` when signed out. */
    suspend fun accessToken(): String?

    /**
     * Refreshes the session because a request using [failedAccessToken] got a 401.
     *
     * Returns the new access token, or the current one if another caller already refreshed.
     * Returns `null` when the session cannot be refreshed (signed out / refresh rejected) —
     * the implementation is expected to clear the session in that case.
     */
    suspend fun refreshTokens(failedAccessToken: String?): String?
}
