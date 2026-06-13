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
import kotlinx.datetime.Clock
import org.surau.app.core.database.dao.ReadingProgressDao
import org.surau.app.core.database.model.ReadingProgressEntity
import org.surau.app.core.database.model.asExternalModel
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.QuranReadingPosition
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.me.PutQuranProgressRequestDto
import org.surau.app.core.network.retrofit.SurauMeApi
import javax.inject.Inject

internal class OfflineFirstQuranProgressRepository @Inject constructor(
    private val readingProgressDao: ReadingProgressDao,
    private val meApi: SurauMeApi,
    private val authSessionDataSource: AuthSessionDataSource,
) : QuranProgressRepository {

    override fun observePosition(): Flow<QuranReadingPosition?> =
        readingProgressDao.observeProgress().map { it?.asExternalModel() }

    override suspend fun savePosition(ayahKey: AyahKey) {
        readingProgressDao.upsert(
            ReadingProgressEntity(
                ayahKey = ayahKey.value,
                updatedAt = Clock.System.now(),
                pendingSync = true,
            ),
        )
    }

    override suspend fun pushPendingPosition() {
        if (!isAuthenticated()) return
        val local = readingProgressDao.progress() ?: return
        if (!local.pendingSync) return

        try {
            apiCall {
                meApi.putQuranProgress(
                    PutQuranProgressRequestDto(
                        ayahKey = local.ayahKey,
                        clientObservedAt = local.updatedAt,
                    ),
                )
            }
            readingProgressDao.markSynced(local.ayahKey)
        } catch (_: Exception) {
            // Keep pending; the sync worker or next push retries.
        }
    }

    override suspend fun reconcile() {
        if (!isAuthenticated()) return

        val remote = try {
            apiCall { meApi.quranProgress() }
        } catch (exception: SurauApiException) {
            if (exception.httpStatus == 404) null else return
        } catch (_: Exception) {
            return
        }

        val local = readingProgressDao.progress()

        val remoteObservedAt = remote?.observedAt ?: remote?.updatedAt

        when {
            remote == null && local != null -> pushPendingPosition()

            remote != null && (
                local == null ||
                    (remoteObservedAt != null && remoteObservedAt > local.updatedAt)
                ) -> {
                // Remote is newer (or there is no local position): adopt it.
                readingProgressDao.upsert(
                    ReadingProgressEntity(
                        ayahKey = remote.ayahKey,
                        updatedAt = remoteObservedAt ?: Clock.System.now(),
                        pendingSync = false,
                    ),
                )
            }

            local != null && local.pendingSync -> pushPendingPosition()
        }
    }

    private suspend fun isAuthenticated(): Boolean =
        authSessionDataSource.authState.first() is AuthState.Authenticated
}
