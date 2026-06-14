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
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.model.data.activity.ReadingActivity
import org.surau.app.core.model.data.activity.ReadingActivityDay
import org.surau.app.core.model.data.activity.ReadingStreak
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.me.ReadingActivitySummaryDto
import org.surau.app.core.network.model.me.ReadingStreakDto
import org.surau.app.core.network.retrofit.SurauMeApi
import javax.inject.Inject

/**
 * Network-first activity repository (no HTTP caching — the auth client is uncached and the data is
 * small and personal). See [ActivityRepository].
 */
internal class DefaultActivityRepository @Inject constructor(
    private val meApi: SurauMeApi,
    private val authSessionDataSource: AuthSessionDataSource,
) : ActivityRepository {

    override suspend fun getActivity(from: LocalDate, to: LocalDate): ReadingActivity =
        apiCall { meApi.activity(from = from.toString(), to = to.toString()) }.asExternalModel()

    override suspend fun getStreak(today: LocalDate): ReadingStreak =
        apiCall { meApi.streak(today = today.toString()) }.asExternalModel()

    override fun observeSurahProgress(): Flow<Map<Int, Float>> = flow {
        emit(emptyMap())
        if (authSessionDataSource.authState.first() !is AuthState.Authenticated) return@flow
        val map = runCatching { apiCall { meApi.surahProgress() } }
            .getOrNull()
            ?.items
            ?.associate { it.surahId to ((it.positionPercent ?: 0.0) / 100.0).toFloat() }
        if (map != null) emit(map)
    }
}

private fun ReadingStreakDto.asExternalModel() = ReadingStreak(
    currentStreakDays = currentStreakDays,
    longestStreakDays = longestStreakDays,
    totalActiveDays = totalActiveDays,
    lastActiveDate = lastActiveDate.toLocalDateOrNull(),
    activeToday = activeToday,
)

private fun ReadingActivitySummaryDto.asExternalModel() = ReadingActivity(
    from = from.toLocalDateOrNull(),
    to = to.toLocalDateOrNull(),
    activeDays = activeDays,
    totalQuranAyahs = quranAyahsRead,
    days = days.mapNotNull { day ->
        day.date.toLocalDateOrNull()?.let {
            ReadingActivityDay(
                date = it,
                quranAyahsRead = day.quranAyahsRead,
                kitabPagesRead = day.kitabPagesRead,
            )
        }
    },
)

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
