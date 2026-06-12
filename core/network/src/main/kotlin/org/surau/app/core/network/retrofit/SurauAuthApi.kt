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

package org.surau.app.core.network.retrofit

import org.surau.app.core.network.model.auth.ForgotPasswordRequestDto
import org.surau.app.core.network.model.auth.LoginRequestDto
import org.surau.app.core.network.model.auth.LogoutRequestDto
import org.surau.app.core.network.model.auth.RefreshRequestDto
import org.surau.app.core.network.model.auth.RegisterRequestDto
import org.surau.app.core.network.model.auth.RegisteredUserDto
import org.surau.app.core.network.model.auth.ResendVerificationRequestDto
import org.surau.app.core.network.model.auth.ResetPasswordRequestDto
import org.surau.app.core.network.model.auth.TokenPairDto
import org.surau.app.core.network.model.auth.VerifyEmailRequestDto
import org.surau.app.core.network.model.auth.VerifyEmailResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Public auth endpoints. Served by the unauthenticated client — never attach the
 * token authenticator here.
 */
interface SurauAuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): RegisteredUserDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenPairDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenPairDto

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto)

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequestDto): VerifyEmailResponseDto

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body body: ResendVerificationRequestDto)

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto)

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto)
}
