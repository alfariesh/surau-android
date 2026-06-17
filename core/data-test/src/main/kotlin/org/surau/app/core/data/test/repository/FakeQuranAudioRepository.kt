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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.surau.app.core.data.repository.QuranAudioRepository
import org.surau.app.core.data.test.QuranTestData
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.SurahAudioManifest
import javax.inject.Inject

/**
 * Fake [QuranAudioRepository] backed by [QuranTestData] (Mishari/Abdul Basit recitations and the
 * Al-Fatihah manifest). Set [manifestError] to drive the offline/error paths.
 */
class FakeQuranAudioRepository @Inject constructor() : QuranAudioRepository {

    val recitations = MutableStateFlow(QuranTestData.recitations)

    /** Manifest returned by [audioManifest]; override per test. */
    var manifest: SurahAudioManifest = QuranTestData.fatihahAudioManifest

    /** When non-null, [audioManifest] throws this instead of returning [manifest]. */
    var manifestError: Exception? = null

    /** Optional suspension before [audioManifest] returns, to exercise load-cancellation races. */
    var manifestDelayMs: Long = 0

    /** Records the surahId of every [audioManifest] call, in order. */
    val audioManifestCalls = mutableListOf<Int>()

    override fun observeRecitations(): Flow<List<Recitation>> = recitations

    override suspend fun resolveRecitationId(preferredId: String?): String? =
        preferredId
            ?: recitations.value.firstOrNull { it.isDefault }?.id
            ?: recitations.value.firstOrNull()?.id

    override suspend fun resolveRecitationId(preferredId: String?, requiredMode: String): String? {
        val matching = recitations.value.filter { it.mode == requiredMode }
        return matching.firstOrNull { it.id == preferredId }?.id
            ?: matching.firstOrNull { it.isDefault }?.id
            ?: matching.firstOrNull()?.id
    }

    override suspend fun audioManifest(surahId: Int, recitationId: String?): SurahAudioManifest {
        audioManifestCalls += surahId
        if (manifestDelayMs > 0) delay(manifestDelayMs)
        manifestError?.let { throw it }
        return manifest
    }
}
