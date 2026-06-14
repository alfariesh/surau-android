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

package org.surau.app.core.network.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class ChangeEmailRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_email") val newEmail: String,
)

@Serializable
data class ChangeEmailVerifyRequestDto(
    @SerialName("otp") val otp: String? = null,
    @SerialName("token") val token: String? = null,
)

@Serializable
data class DeleteAccountRequestDto(
    @SerialName("current_password") val currentPassword: String,
)

/**
 * One active session/device returned by `GET /auth/sessions`. Timestamps are ISO-8601 strings,
 * parsed lazily where the UI needs them.
 */
@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("user_agent") val userAgent: String = "",
    @SerialName("client_ip") val clientIp: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_current") val isCurrent: Boolean = false,
)

@Serializable
data class SessionsResponseDto(
    @SerialName("items") val items: List<SessionDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
)
