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
import org.surau.app.core.model.data.auth.AuthState

/**
 * Account/session operations against the Surau backend.
 *
 * All suspend functions throw [org.surau.app.core.network.model.SurauApiException] on backend
 * errors (check `isEmailNotVerified` / `isRateLimited` for flow-control cases) and
 * [java.io.IOException] on connectivity failures.
 */
interface AuthRepository {

    /** The persisted sign-in state. */
    val authState: Flow<AuthState>

    /** Logs in and persists the session. 403 AUTH_EMAIL_NOT_VERIFIED means verify first. */
    suspend fun login(email: String, password: String)

    /** Registers a new account; the user must then verify their email before logging in. */
    suspend fun register(email: String, password: String, displayName: String?)

    /** Confirms an email with the 6-digit OTP (or an emailed token). */
    suspend fun verifyEmail(email: String, otp: String)

    suspend fun resendVerification(email: String)

    suspend fun forgotPassword(email: String)

    suspend fun resetPassword(token: String, newPassword: String)

    /** Revokes the session server-side (best effort) and always clears it locally. */
    suspend fun logout()
}
