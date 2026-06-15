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

import org.surau.app.core.network.model.auth.ChangeEmailRequestDto
import org.surau.app.core.network.model.auth.ChangeEmailVerifyRequestDto
import org.surau.app.core.network.model.auth.ChangePasswordRequestDto
import org.surau.app.core.network.model.auth.DeleteAccountRequestDto
import org.surau.app.core.network.model.auth.SessionsResponseDto
import org.surau.app.core.network.model.auth.TokenPairDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Authenticated account-management endpoints, served by the auth client (Bearer + token refresh).
 * Unlike the public [SurauAuthApi] these require a signed-in session.
 *
 * Change-password and verify-email-change rotate the whole session and return a fresh
 * [TokenPairDto] (all other sessions are revoked); callers must persist it or the user is
 * immediately logged out.
 */
interface SurauAccountApi {

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): TokenPairDto

    @POST("auth/change-email/request")
    suspend fun requestEmailChange(@Body body: ChangeEmailRequestDto)

    @POST("auth/change-email/verify")
    suspend fun verifyEmailChange(@Body body: ChangeEmailVerifyRequestDto): TokenPairDto

    @GET("auth/sessions")
    suspend fun listSessions(): SessionsResponseDto

    @DELETE("auth/sessions/{id}")
    suspend fun revokeSession(@Path("id") id: String)

    @POST("auth/logout-all")
    suspend fun logoutAll()

    @POST("auth/delete-account")
    suspend fun deleteAccount(@Body body: DeleteAccountRequestDto)
}
