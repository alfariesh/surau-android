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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.UserSession
import javax.inject.Inject

/**
 * Fake [AuthRepository]: starts as guest, logs in instantly without a backend.
 */
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val state = MutableStateFlow<AuthState>(AuthState.Guest)

    override val authState: Flow<AuthState> = state

    override suspend fun login(email: String, password: String) {
        state.value = AuthState.Authenticated(
            UserSession(
                userId = "fake-user",
                email = email,
                displayName = null,
                sessionId = "fake-session",
            ),
        )
    }

    override suspend fun register(email: String, password: String, displayName: String?) = Unit

    override suspend fun verifyEmail(email: String, otp: String) = Unit

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun forgotPassword(email: String) = Unit

    override suspend fun resetPassword(token: String, newPassword: String) = Unit

    override suspend fun logout() {
        state.value = AuthState.Guest
    }
}
