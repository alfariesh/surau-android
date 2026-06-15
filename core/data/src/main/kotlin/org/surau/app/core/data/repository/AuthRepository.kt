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
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.EmailPreferences

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

    // --- Account management (M5; all require an active session) ---

    /**
     * Updates the server profile (display name + country; timezone is filled automatically) and
     * refreshes the locally stored display name.
     */
    suspend fun updateProfile(displayName: String?, countryCode: String?)

    /**
     * Changes the password. The backend rotates the whole session and revokes every other one;
     * the returned token pair is persisted so this device stays signed in.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String)

    /** Step 1 of an email change: sends an OTP to [newEmail] after re-checking the password. */
    suspend fun requestEmailChange(currentPassword: String, newEmail: String)

    /**
     * Step 2 of an email change: confirms [otp]; the rotated token pair is persisted and the stored
     * email updated to [newEmail].
     */
    suspend fun verifyEmailChange(newEmail: String, otp: String)

    /** Lists the account's active sessions/devices. */
    suspend fun listSessions(): List<AccountSession>

    /** Revokes a single other session by id. */
    suspend fun revokeSession(sessionId: String)

    /** Signs out of every device (this one included) and clears the local session. */
    suspend fun logoutAllDevices()

    /**
     * Deletes (anonymizes) the account after re-checking the password, then clears the local
     * session. Local reading data is kept — the user continues as a guest.
     */
    suspend fun deleteAccount(currentPassword: String)

    /** Reads the email subscription preferences. */
    suspend fun emailPreferences(): EmailPreferences

    /** Updates the marketing opt-in and returns the new preferences. */
    suspend fun updateEmailPreferences(marketingOptIn: Boolean): EmailPreferences
}
