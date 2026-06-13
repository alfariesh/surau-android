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

package org.surau.app.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.SurahAudioManifest

/**
 * Audio murottal data: the catalog of recitations (cached offline-first) and per-surah audio
 * manifests (network-only — CDN URLs expire, so they are never persisted).
 */
interface QuranAudioRepository {

    /** Available recitations, refreshing from the network when empty or stale (7-day TTL). */
    fun observeRecitations(): Flow<List<Recitation>>

    /**
     * The effective recitation id: [preferredId] if set, otherwise the cached default, otherwise
     * the first available. Returns `null` when nothing is cached and the network is unreachable —
     * callers should then omit the id so the backend applies its own default.
     */
    suspend fun resolveRecitationId(preferredId: String?): String?

    /**
     * Like [resolveRecitationId] but restricted to recitations whose [Recitation.mode] equals
     * [requiredMode] (e.g. `"surah"` for the immersive Flow reader). [preferredId] is honoured only
     * when its recitation also matches [requiredMode]. Returns `null` if none is cached.
     */
    suspend fun resolveRecitationId(preferredId: String?, requiredMode: String): String?

    /**
     * The audio manifest for [surahId] using [recitationId] (or the backend default when `null`).
     * Always network-only.
     *
     * @throws org.surau.app.core.network.model.SurauApiException on backend errors
     * @throws java.io.IOException when offline
     */
    suspend fun audioManifest(surahId: Int, recitationId: String?): SurahAudioManifest
}
