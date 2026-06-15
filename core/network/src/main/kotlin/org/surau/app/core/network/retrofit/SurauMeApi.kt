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
import org.surau.app.core.network.model.me.KhatamCycleDto
import org.surau.app.core.network.model.me.KhatamHistoryResponseDto
import org.surau.app.core.network.model.me.PatchSavedItemRequestDto
import org.surau.app.core.network.model.me.PutQuranProgressRequestDto
import org.surau.app.core.network.model.me.QuranProgressDto
import org.surau.app.core.network.model.me.QuranProgressListResponseDto
import org.surau.app.core.network.model.me.ReadingActivitySummaryDto
import org.surau.app.core.network.model.me.ReadingStreakDto
import org.surau.app.core.network.model.me.SavedItemDto
import org.surau.app.core.network.model.me.SavedItemsResponseDto
import org.surau.app.core.network.model.me.SavedItemsTagsResponseDto
import org.surau.app.core.network.model.me.StartKhatamRequestDto
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

    // --- Khatam (Quran completion cycle) ---

    /** The active (uncompleted) cycle. Returns 404 when the user has no active cycle. */
    @GET("me/quran/khatam")
    suspend fun activeKhatam(): KhatamCycleDto

    /** Starts a new cycle. 409 if one is already active. */
    @POST("me/quran/khatam")
    suspend fun startKhatam(@Body body: StartKhatamRequestDto): KhatamCycleDto

    /** Marks one juz (1–30) as completed; idempotent. Returns the updated cycle. */
    @PUT("me/quran/khatam/juz/{juz}")
    suspend fun markKhatamJuz(@Path("juz") juz: Int): KhatamCycleDto

    /** Removes one juz mark (1–30); idempotent. Returns the updated cycle. */
    @DELETE("me/quran/khatam/juz/{juz}")
    suspend fun unmarkKhatamJuz(@Path("juz") juz: Int): KhatamCycleDto

    /** Completes the active cycle. 409 unless all 30 juz are marked. */
    @POST("me/quran/khatam/complete")
    suspend fun completeKhatam(): KhatamCycleDto

    @GET("me/quran/khatam/history")
    suspend fun khatamHistory(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): KhatamHistoryResponseDto

    // --- Reading activity & streak ---

    /** Daily activity buckets over `[from, to]` (each `YYYY-MM-DD` in the device's local date). */
    @GET("me/activity")
    suspend fun activity(
        @Query("from") from: String,
        @Query("to") to: String,
    ): ReadingActivitySummaryDto

    /** The reading streak; pass [today] as the device's local `YYYY-MM-DD` for correct day boundaries. */
    @GET("me/activity/streak")
    suspend fun streak(@Query("today") today: String): ReadingStreakDto

    /** Per-surah resume positions (for thin progress badges on the surah list). */
    @GET("me/quran/progress/surahs")
    suspend fun surahProgress(): QuranProgressListResponseDto
}
