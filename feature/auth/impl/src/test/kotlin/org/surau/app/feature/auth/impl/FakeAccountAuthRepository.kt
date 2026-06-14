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

package org.surau.app.feature.auth.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.EmailPreferences
import org.surau.app.core.model.data.auth.UserSession

/**
 * Scriptable [AuthRepository] for account-management ViewModel tests. Set [error] before a call to
 * make the next account action throw; the auth (login/register/…) surface is unused here.
 */
internal class FakeAccountAuthRepository : AuthRepository {

    val authStateFlow = MutableStateFlow<AuthState>(
        AuthState.Authenticated(
            UserSession(
                userId = "user-1",
                email = "user@surau.org",
                displayName = "Aisyah",
                sessionId = "session-1",
            ),
        ),
    )
    override val authState: Flow<AuthState> = authStateFlow

    /** Thrown by the next account mutation/query when non-null. */
    var error: Exception? = null
    var sessions: List<AccountSession> = emptyList()
    var marketingOptIn: Boolean = false

    val revokedSessionIds = mutableListOf<String>()
    var logoutAllCalls = 0
    var lastProfile: Pair<String?, String?>? = null
    var lastVerifiedEmail: String? = null

    private fun raise() = error?.let { throw it }

    override suspend fun updateProfile(displayName: String?, countryCode: String?) {
        raise()
        lastProfile = displayName to countryCode
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) = raise() ?: Unit

    override suspend fun requestEmailChange(currentPassword: String, newEmail: String) = raise() ?: Unit

    override suspend fun verifyEmailChange(newEmail: String, otp: String) {
        raise()
        lastVerifiedEmail = newEmail
    }

    override suspend fun listSessions(): List<AccountSession> {
        raise()
        return sessions
    }

    override suspend fun revokeSession(sessionId: String) {
        raise()
        revokedSessionIds += sessionId
    }

    override suspend fun logoutAllDevices() {
        raise()
        logoutAllCalls++
        authStateFlow.value = AuthState.Guest
    }

    override suspend fun deleteAccount(currentPassword: String) {
        raise()
        authStateFlow.value = AuthState.Guest
    }

    override suspend fun emailPreferences(): EmailPreferences {
        raise()
        return EmailPreferences(marketingOptIn)
    }

    override suspend fun updateEmailPreferences(marketingOptIn: Boolean): EmailPreferences {
        raise()
        this.marketingOptIn = marketingOptIn
        return EmailPreferences(marketingOptIn)
    }

    // Unused auth flows.
    override suspend fun login(email: String, password: String) = Unit
    override suspend fun register(email: String, password: String, displayName: String?) = Unit
    override suspend fun verifyEmail(email: String, otp: String) = Unit
    override suspend fun resendVerification(email: String) = Unit
    override suspend fun forgotPassword(email: String) = Unit
    override suspend fun resetPassword(token: String, newPassword: String) = Unit
    override suspend fun logout() = Unit
}
