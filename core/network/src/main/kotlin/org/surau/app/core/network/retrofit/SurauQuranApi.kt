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

import org.surau.app.core.network.model.PagedResponseDto
import org.surau.app.core.network.model.quran.AyahDto
import org.surau.app.core.network.model.quran.JuzDto
import org.surau.app.core.network.model.quran.QuranSearchResultDto
import org.surau.app.core.network.model.quran.RecitationDto
import org.surau.app.core.network.model.quran.SurahAudioManifestDto
import org.surau.app.core.network.model.quran.SurahDto
import org.surau.app.core.network.model.quran.TranslationSourceDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Public Quran content endpoints. Responses carry ETag/Cache-Control headers which the public
 * OkHttp client caches on disk.
 */
interface SurauQuranApi {

    @GET("quran/surahs")
    suspend fun surahs(
        @Query("lang") lang: String = "id",
    ): PagedResponseDto<SurahDto>

    @GET("quran/surahs/{surahId}/ayahs")
    suspend fun ayahs(
        @Path("surahId") surahId: Int,
        @Query("lang") lang: String = "id",
        @Query("translation_source") translationSource: String? = null,
        @Query("include_translation") includeTranslation: Boolean = true,
        @Query("include_audio") includeAudio: Boolean = false,
        @Query("view") view: String = "reader_minimal",
    ): PagedResponseDto<AyahDto>

    @GET("quran/juz")
    suspend fun juz(): PagedResponseDto<JuzDto>

    @GET("quran/translation-sources")
    suspend fun translationSources(
        @Query("lang") lang: String? = null,
    ): PagedResponseDto<TranslationSourceDto>

    @GET("quran/recitations")
    suspend fun recitations(): PagedResponseDto<RecitationDto>

    @GET("quran/surahs/{surahId}/audio")
    suspend fun surahAudio(
        @Path("surahId") surahId: Int,
        @Query("recitation_id") recitationId: String? = null,
    ): SurahAudioManifestDto

    @GET("quran/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("lang") lang: String = "id",
        @Query("translation_source") translationSource: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): PagedResponseDto<QuranSearchResultDto>
}
