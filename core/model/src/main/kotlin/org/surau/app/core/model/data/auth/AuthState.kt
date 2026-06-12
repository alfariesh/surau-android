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

package org.surau.app.core.model.data.auth

/**
 * Whether the user is signed in. Surau is guest-first: the Quran is readable in [Guest] state and
 * signing in only adds personal sync.
 */
sealed interface AuthState {

    /** Auth state has not been loaded from disk yet. */
    data object Unknown : AuthState

    /** No active session — reading as a guest. */
    data object Guest : AuthState

    /** Signed in with an active session. */
    data class Authenticated(val session: UserSession) : AuthState
}

/**
 * The signed-in user's identity, persisted alongside the session tokens.
 */
data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val sessionId: String,
    val emailVerified: Boolean = true,
)
