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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.surau.app.core.network.model.SurauApiException
import java.io.IOException

class AuthSubmitStateMappingTest {

    @Test
    fun conflict409_mapsToEmailExists() {
        // Registering an already-used email: backend replies 409 user_already_exists.
        val exception = SurauApiException(
            httpStatus = 409,
            code = "user_already_exists",
            message = "user already exists",
        )

        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.EMAIL_EXISTS),
            exception.toAuthSubmitState(),
        )
    }

    @Test
    fun rateLimited_mapsToRateLimitedWithRetryAfter() {
        val exception = SurauApiException(
            httpStatus = 429,
            code = "AUTH_RATE_LIMITED",
            message = "too many requests",
            retryAfterSeconds = 30L,
        )

        assertEquals(AuthSubmitState.RateLimited(30L), exception.toAuthSubmitState())
    }

    @Test
    fun emailNotVerified_withEmail_mapsToRequiresVerification() {
        val exception = SurauApiException(
            httpStatus = 403,
            code = "AUTH_EMAIL_NOT_VERIFIED",
            message = "email not verified",
        )

        assertEquals(
            AuthSubmitState.RequiresVerification("user@surau.org"),
            exception.toAuthSubmitState(email = "user@surau.org"),
        )
    }

    @Test
    fun ioException_mapsToOffline() {
        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.OFFLINE),
            IOException().toAuthSubmitState(),
        )
    }

    @Test
    fun unmappedServerError_mapsToGeneric() {
        val exception = SurauApiException(
            httpStatus = 500,
            code = "internal_error",
            message = "boom",
        )

        assertEquals(
            AuthSubmitState.Error(AuthErrorKind.GENERIC),
            exception.toAuthSubmitState(),
        )
    }
}
