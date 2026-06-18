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
import org.surau.app.core.datastore.crypto.AuthCrypto
import org.surau.app.core.datastore.crypto.PlaintextAuthCrypto
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.UserSession
import javax.inject.Inject

/**
 * Persists the signed-in session (tokens + identity).
 *
 * The token fields are encrypted at rest with Keystore-backed AEAD ([crypto]) and the backing
 * DataStore file is excluded from Android Auto Backup, so tokens never leave the device in the
 * clear. Identity fields stay plaintext; combined with 15-minute access tokens and rotating refresh
 * tokens this keeps storage inside the app sandbox safe.
 */
class AuthSessionDataSource @Inject constructor(
    private val authSession: DataStore<AuthSession>,
    private val crypto: AuthCrypto = PlaintextAuthCrypto,
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
                this.accessToken = crypto.encrypt(accessToken)
                this.refreshToken = crypto.encrypt(refreshToken)
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
                this.accessToken = crypto.encrypt(accessToken)
                this.refreshToken = crypto.encrypt(refreshToken)
                this.expiresAtEpochSeconds = expiresAtEpochSeconds
                if (sessionId != null) this.sessionId = sessionId
            }
        }
    }

    /**
     * Atomically persists a rotated token pair AND the new email after a confirmed email change.
     * Folding both writes into one [DataStore.updateData] transform means a crash or cancellation
     * can't leave the new tokens stored against the stale email.
     */
    suspend fun updateTokensAndEmail(
        accessToken: String,
        refreshToken: String,
        expiresAtEpochSeconds: Long,
        email: String,
        sessionId: String? = null,
    ) {
        authSession.updateData {
            it.copy {
                this.accessToken = crypto.encrypt(accessToken)
                this.refreshToken = crypto.encrypt(refreshToken)
                this.expiresAtEpochSeconds = expiresAtEpochSeconds
                this.email = email
                if (sessionId != null) this.sessionId = sessionId
            }
        }
    }

    /**
     * Updates the stored email after a confirmed email change reached via the emailed link (where
     * the new address is fetched from the server rather than supplied with the tokens).
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

    suspend fun currentAccessToken(): String? = decryptStoredToken { it.accessToken }

    suspend fun currentRefreshToken(): String? = decryptStoredToken { it.refreshToken }

    /**
     * Decrypts a stored token. A non-empty stored value that fails to decrypt (returns null) means
     * the Keystore keyset was invalidated (e.g. a lock-screen credential or biometric change) and the
     * token is permanently unrecoverable. We [clear] the session so the app self-heals to a clean
     * Guest state and prompts a fresh sign-in, rather than stranding the UI as "authenticated" (the
     * stored ciphertext is non-empty, so [toAuthState] would otherwise keep reporting Authenticated)
     * while every authenticated request silently sends no token and 401s.
     */
    private suspend fun decryptStoredToken(select: (AuthSession) -> String): String? {
        val stored = select(authSession.data.first()).ifEmpty { return null }
        val decrypted = crypto.decrypt(stored)
        if (decrypted == null) clear()
        return decrypted
    }
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
