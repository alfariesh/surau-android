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

package org.surau.app.core.network.model

import org.junit.Test
import org.surau.app.core.network.di.NetworkModule
import org.surau.app.core.network.model.auth.ChangePasswordRequestDto
import org.surau.app.core.network.model.auth.SessionsResponseDto
import org.surau.app.core.network.model.user.ProfilePatchRequestDto
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks down the account-management contract: the profile PATCH must omit absent fields (so a
 * partial update never wipes the rest), and the sessions list envelope parses fully.
 */
class AccountDtoSerializationTest {

    // The PRODUCTION Json (encodeDefaults=false, explicitNulls=true) so this test actually guards the
    // partial-PATCH contract — flipping the DI config would make these tests fail, not silently pass.
    private val json = NetworkModule.providesNetworkJson()

    @Test
    fun profilePatch_omitsAbsentFields() {
        val body = ProfilePatchRequestDto(displayName = "Aisyah")
        val encoded = json.encodeToString(ProfilePatchRequestDto.serializer(), body)

        assertEquals("""{"display_name":"Aisyah"}""", encoded)
        assertFalse(encoded.contains("timezone"), "absent timezone must be omitted")
        assertFalse(encoded.contains("country_code"))
        assertFalse(encoded.contains("personalization_enabled"))
    }

    @Test
    fun profilePatch_includesProvidedFields() {
        val body = ProfilePatchRequestDto(
            displayName = "Umar",
            timezone = "Asia/Jakarta",
            countryCode = "ID",
        )
        val encoded = json.encodeToString(ProfilePatchRequestDto.serializer(), body)

        assertTrue(encoded.contains("\"display_name\":\"Umar\""))
        assertTrue(encoded.contains("\"timezone\":\"Asia/Jakarta\""))
        assertTrue(encoded.contains("\"country_code\":\"ID\""))
    }

    @Test
    fun profilePatch_allNull_encodesEmptyObject() {
        // A fully-empty patch must serialize to "{}" so a one-field edit can never wipe the rest.
        val encoded = json.encodeToString(ProfilePatchRequestDto.serializer(), ProfilePatchRequestDto())

        assertEquals("{}", encoded)
    }

    @Test
    fun profilePatch_encodesExplicitFalsePersonalization() {
        // `false` is distinct from the (omitted) default, so turning personalization OFF must be sent.
        val body = ProfilePatchRequestDto(personalizationEnabled = false)
        val encoded = json.encodeToString(ProfilePatchRequestDto.serializer(), body)

        assertEquals("""{"personalization_enabled":false}""", encoded)
    }

    @Test
    fun changePasswordRequest_usesSnakeCaseFields() {
        val body = ChangePasswordRequestDto(currentPassword = "old12345", newPassword = "new12345")
        val encoded = json.encodeToString(ChangePasswordRequestDto.serializer(), body)

        assertEquals("""{"current_password":"old12345","new_password":"new12345"}""", encoded)
    }

    @Test
    fun sessionsResponse_parsesListEnvelope() {
        val response = json.decodeFromString(
            SessionsResponseDto.serializer(),
            """
            {
              "items": [
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "user_agent": "Mozilla/5.0",
                  "client_ip": "203.0.113.42",
                  "created_at": "2026-01-01T00:00:00Z",
                  "last_used_at": "2026-01-02T08:30:00Z",
                  "expires_at": "2026-02-01T00:00:00Z",
                  "is_current": true
                }
              ],
              "total": 1
            }
            """.trimIndent(),
        )

        assertEquals(1, response.total)
        val session = response.items.single()
        assertEquals("203.0.113.42", session.clientIp)
        assertTrue(session.isCurrent)
    }
}
