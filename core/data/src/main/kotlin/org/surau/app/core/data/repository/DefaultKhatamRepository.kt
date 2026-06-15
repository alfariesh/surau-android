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

import org.surau.app.core.model.data.quran.KhatamCycle
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.me.KhatamCycleDto
import org.surau.app.core.network.model.me.StartKhatamRequestDto
import org.surau.app.core.network.retrofit.SurauMeApi
import javax.inject.Inject

/**
 * Network-first khatam repository. Data is small and personal, so nothing is cached (the ViewModel
 * holds the loaded cycle and mutates it optimistically). See [KhatamRepository].
 */
internal class DefaultKhatamRepository @Inject constructor(
    private val meApi: SurauMeApi,
) : KhatamRepository {

    override suspend fun getActiveCycle(): KhatamCycle? =
        try {
            apiCall { meApi.activeKhatam() }.asExternalModel()
        } catch (e: SurauApiException) {
            // The backend returns 404 when there is no active cycle — a normal "empty" state.
            if (e.httpStatus == 404) null else throw e
        }

    override suspend fun startCycle(notes: String?): KhatamCycle =
        apiCall { meApi.startKhatam(StartKhatamRequestDto(notes = notes?.ifBlank { null })) }
            .asExternalModel()

    override suspend fun markJuz(juz: Int): KhatamCycle =
        apiCall { meApi.markKhatamJuz(juz) }.asExternalModel()

    override suspend fun unmarkJuz(juz: Int): KhatamCycle =
        apiCall { meApi.unmarkKhatamJuz(juz) }.asExternalModel()

    override suspend fun complete(): KhatamCycle =
        apiCall { meApi.completeKhatam() }.asExternalModel()

    override suspend fun history(limit: Int, offset: Int): List<KhatamCycle> =
        apiCall { meApi.khatamHistory(limit = limit, offset = offset) }
            .items.map(KhatamCycleDto::asExternalModel)
}

internal fun KhatamCycleDto.asExternalModel() = KhatamCycle(
    id = id,
    startedAt = startedAt,
    completedAt = completedAt,
    notes = notes,
    completedJuz = completedJuz.toSet(),
    juzCount = juzCount,
    percent = percent,
)
