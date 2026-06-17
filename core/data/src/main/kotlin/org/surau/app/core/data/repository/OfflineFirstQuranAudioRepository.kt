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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.surau.app.core.common.coroutines.runCatchingExceptCancellation
import org.surau.app.core.database.dao.RecitationDao
import org.surau.app.core.database.model.RecitationEntity
import org.surau.app.core.database.model.asExternalModel
import org.surau.app.core.model.data.quran.AudioSegment
import org.surau.app.core.model.data.quran.AudioTrack
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.SurahAudioManifest
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.quran.AyahAudioSegmentDto
import org.surau.app.core.network.model.quran.SurahAudioManifestDto
import org.surau.app.core.network.model.quran.SurahAudioTrackDto
import org.surau.app.core.network.retrofit.SurauQuranApi
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Room caches the recitation catalog (refreshed weekly); audio manifests are fetched fresh per
 * playback session and never cached, since CDN URLs can expire.
 */
internal class OfflineFirstQuranAudioRepository @Inject constructor(
    private val quranApi: SurauQuranApi,
    private val recitationDao: RecitationDao,
) : QuranAudioRepository {

    private val recitationRefreshMutex = Mutex()

    override fun observeRecitations(): Flow<List<Recitation>> =
        recitationDao.observeAll()
            .onStart { refreshRecitationsIfNeeded() }
            .map { recitations -> recitations.map(RecitationEntity::asExternalModel) }

    override suspend fun resolveRecitationId(preferredId: String?): String? {
        if (preferredId != null) return preferredId
        refreshRecitationsIfNeeded()
        val recitations = recitationDao.observeAll().first()
        return recitations.firstOrNull { it.isDefault }?.id
            ?: recitations.firstOrNull()?.id
    }

    override suspend fun resolveRecitationId(preferredId: String?, requiredMode: String): String? {
        refreshRecitationsIfNeeded()
        val recitations = recitationDao.observeAll().first()
            .map(RecitationEntity::asExternalModel)
            .filter { it.mode == requiredMode }
        return recitations.firstOrNull { it.id == preferredId }?.id
            ?: recitations.firstOrNull { it.isDefault }?.id
            ?: recitations.firstOrNull()?.id
    }

    override suspend fun audioManifest(surahId: Int, recitationId: String?): SurahAudioManifest =
        apiCall { quranApi.surahAudio(surahId = surahId, recitationId = recitationId) }
            .asExternalModel()

    private suspend fun refreshRecitationsIfNeeded() {
        recitationRefreshMutex.withLock {
            val oldest = recitationDao.oldestFetchedAt()
            val isFresh = recitationDao.count() > 0 &&
                oldest != null &&
                Instant.fromEpochMilliseconds(oldest) > Clock.System.now() - RECITATION_CACHE_TTL
            if (isFresh) return

            val recitations = runCatchingExceptCancellation {
                apiCall { quranApi.recitations() }.items
            }.getOrElse {
                // Offline-first: keep serving the cache (or an empty list on first launch).
                // Cancellation is rethrown by the helper, not swallowed here.
                return
            }

            val now = Clock.System.now()
            recitationDao.upsertAll(
                recitations.map { dto ->
                    RecitationEntity(
                        id = dto.id,
                        displayName = dto.displayName,
                        reciterName = dto.reciterName,
                        style = dto.style,
                        mode = dto.mode,
                        isDefault = dto.isDefault,
                        fetchedAt = now,
                    )
                },
            )
        }
    }

    companion object {
        private val RECITATION_CACHE_TTL = 7.days
    }
}

private fun SurahAudioManifestDto.asExternalModel() = SurahAudioManifest(
    surahId = surahId,
    recitationId = recitation.id,
    recitationName = recitation.displayName.ifEmpty { recitation.reciterName },
    mode = mode,
    tracks = tracks.map(SurahAudioTrackDto::asExternalModel),
    missingAyahKeys = missingAyahKeys,
)

private fun SurahAudioTrackDto.asExternalModel() = AudioTrack(
    ayahKey = trackKey,
    ayahNumber = ayahNumber,
    url = url,
    durationMs = durationMs,
    segments = segments.map(AyahAudioSegmentDto::asExternalModel),
)

private fun AyahAudioSegmentDto.asExternalModel() = AudioSegment(
    segmentIndex = segmentIndex,
    ayahKey = ayahKey,
    timestampFromMs = timestampFromMs,
    timestampToMs = timestampToMs,
    durationMs = durationMs,
)
