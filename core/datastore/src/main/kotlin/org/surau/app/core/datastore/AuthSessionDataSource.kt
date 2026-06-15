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

package org.surau.app.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.UserSession
import javax.inject.Inject

/**
 * Persists the signed-in session (tokens + identity).
 *
 * The backing DataStore file is excluded from Android Auto Backup so tokens never leave the
 * device; combined with 15-minute access tokens and rotating refresh tokens this keeps plaintext
 * storage inside the app sandbox acceptable for milestone 1.
 */
class AuthSessionDataSource @Inject constructor(
    private val authSession: DataStore<AuthSession>,
) {
    /**
     * The current [AuthState]. Never emits [AuthState.Unknown]; that state only exists for UI
     * consumers awaiting the first emission.
     */
    val authState: Flow<AuthState> = authSession.data.map { it.toAuthState() }

    /**
     * Stores a freshly issued session after login/registration.
     */
    suspend fun setSession(
        session: UserSession,
        accessToken: String,
        refreshToken: String,
        expiresAtEpochSeconds: Long,
    ) {
        authSession.updateData {
            it.copy {
                this.accessToken = accessToken
                this.refreshToken = refreshToken
                this.expiresAtEpochSeconds = expiresAtEpochSeconds
                this.sessionId = session.sessionId
                this.userId = session.userId
                this.email = session.email
                this.displayName = session.displayName.orEmpty()
                this.emailVerified = session.emailVerified
            }
        }
    }

    /**
     * Persists a rotated token pair, keeping the stored identity. Pass [sessionId] when the whole
     * session was rotated server-side (change-password / change-email), so the stored id stays in
     * sync; the silent refresh path leaves it null to keep the existing id.
     */
    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
        expiresAtEpochSeconds: Long,
        sessionId: String? = null,
    ) {
        authSession.updateData {
            it.copy {
                this.accessToken = accessToken
                this.refreshToken = refreshToken
                this.expiresAtEpochSeconds = expiresAtEpochSeconds
                if (sessionId != null) this.sessionId = sessionId
            }
        }
    }

    /**
     * Updates the stored email after a confirmed email change.
     */
    suspend fun updateEmail(email: String) {
        authSession.updateData { it.copy { this.email = email } }
    }

    /**
     * Updates the stored display name after a profile edit.
     */
    suspend fun updateDisplayName(displayName: String?) {
        authSession.updateData { it.copy { this.displayName = displayName.orEmpty() } }
    }

    /**
     * Clears the session, returning the user to guest state.
     */
    suspend fun clear() {
        authSession.updateData { AuthSession.getDefaultInstance() }
    }

    suspend fun currentAccessToken(): String? =
        authSession.data.first().accessToken.ifEmpty { null }

    suspend fun currentRefreshToken(): String? =
        authSession.data.first().refreshToken.ifEmpty { null }
}

private fun AuthSession.toAuthState(): AuthState =
    if (accessToken.isEmpty()) {
        AuthState.Guest
    } else {
        AuthState.Authenticated(
            UserSession(
                userId = userId,
                email = email,
                displayName = displayName.ifEmpty { null },
                sessionId = sessionId,
                emailVerified = emailVerified,
            ),
        )
    }
