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
data class RegisterRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class RegisteredUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String? = null,
    @SerialName("email") val email: String,
    @SerialName("role") val role: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
)

@Serializable
data class LoginRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

/**
 * Token pair issued by login/refresh/change-password.
 */
@Serializable
data class TokenPairDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresInSeconds: Long = 900,
    @SerialName("session_id") val sessionId: String = "",
)

@Serializable
data class VerifyEmailRequestDto(
    @SerialName("email") val email: String? = null,
    @SerialName("otp") val otp: String? = null,
    @SerialName("token") val token: String? = null,
)

@Serializable
data class VerifyEmailResponseDto(
    @SerialName("email_verified") val emailVerified: Boolean = false,
)

@Serializable
data class ResendVerificationRequestDto(
    @SerialName("email") val email: String,
)

@Serializable
data class ForgotPasswordRequestDto(
    @SerialName("email") val email: String,
)

@Serializable
data class ResetPasswordRequestDto(
    @SerialName("token") val token: String,
    @SerialName("password") val password: String,
)

@Serializable
data class IntrospectDto(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
)
