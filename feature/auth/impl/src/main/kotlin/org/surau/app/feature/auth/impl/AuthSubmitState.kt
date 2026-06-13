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

import org.surau.app.core.network.model.SurauApiException
import java.io.IOException

/**
 * Submission state shared by every auth form.
 */
sealed interface AuthSubmitState {
    data object Idle : AuthSubmitState

    data object Submitting : AuthSubmitState

    /** The action completed; the screen decides where to navigate. */
    data object Success : AuthSubmitState

    /** Login hit a 403 AUTH_EMAIL_NOT_VERIFIED — continue in the verification flow. */
    data class RequiresVerification(val email: String) : AuthSubmitState

    /** 429/lockout: the submit button stays disabled while [secondsLeft] ticks down. */
    data class RateLimited(val secondsLeft: Long) : AuthSubmitState

    data class Error(val kind: AuthErrorKind) : AuthSubmitState
}

enum class AuthErrorKind {
    INVALID_CREDENTIALS,
    EMAIL_EXISTS,
    OFFLINE,
    GENERIC,
}

/**
 * Maps an auth API failure to UI state. Rate limits become a ticking [AuthSubmitState.RateLimited]
 * handled by the caller.
 */
internal fun Throwable.toAuthSubmitState(email: String? = null): AuthSubmitState = when {
    this is SurauApiException && isEmailNotVerified && email != null ->
        AuthSubmitState.RequiresVerification(email)

    this is SurauApiException && isRateLimited ->
        AuthSubmitState.RateLimited(retryAfterSeconds ?: DEFAULT_RETRY_AFTER_SECONDS)

    // Register returns 409 when the email already has an account.
    this is SurauApiException && httpStatus == HTTP_CONFLICT ->
        AuthSubmitState.Error(AuthErrorKind.EMAIL_EXISTS)

    this is SurauApiException && isInvalidCredentials ->
        AuthSubmitState.Error(AuthErrorKind.INVALID_CREDENTIALS)

    this is IOException -> AuthSubmitState.Error(AuthErrorKind.OFFLINE)

    else -> AuthSubmitState.Error(AuthErrorKind.GENERIC)
}

private const val DEFAULT_RETRY_AFTER_SECONDS = 60L
private const val HTTP_CONFLICT = 409
