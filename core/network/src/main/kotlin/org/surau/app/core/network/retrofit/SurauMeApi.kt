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

import org.surau.app.core.network.model.me.CreateSavedItemRequestDto
import org.surau.app.core.network.model.me.PatchSavedItemRequestDto
import org.surau.app.core.network.model.me.PutQuranProgressRequestDto
import org.surau.app.core.network.model.me.QuranProgressDto
import org.surau.app.core.network.model.me.SavedItemDto
import org.surau.app.core.network.model.me.SavedItemsResponseDto
import org.surau.app.core.network.model.me.SavedItemsTagsResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Authenticated personal-data endpoints, served by the auth client (Bearer + token refresh).
 */
interface SurauMeApi {

    @GET("me/quran/progress")
    suspend fun quranProgress(): QuranProgressDto

    @PUT("me/quran/progress")
    suspend fun putQuranProgress(@Body body: PutQuranProgressRequestDto): QuranProgressDto

    @GET("me/saved-items")
    suspend fun savedItems(
        @Query("item_type") itemType: String = "quran_ayah",
        @Query("surah_id") surahId: Int? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SavedItemsResponseDto

    @GET("me/saved-items/tags")
    suspend fun savedItemTags(): SavedItemsTagsResponseDto

    @POST("me/saved-items")
    suspend fun createSavedItem(@Body body: CreateSavedItemRequestDto): SavedItemDto

    @PATCH("me/saved-items/{id}")
    suspend fun patchSavedItem(
        @Path("id") id: String,
        @Body body: PatchSavedItemRequestDto,
    ): SavedItemDto

    @DELETE("me/saved-items/{id}")
    suspend fun deleteSavedItem(@Path("id") id: String)
}
