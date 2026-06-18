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

package org.surau.app.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.surau.app.core.common.network.Dispatcher
import org.surau.app.core.common.network.SurauDispatchers.IO
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.sync.initializers.DownloadConstraints
import org.surau.app.sync.initializers.downloadForegroundInfo
import java.io.IOException

/**
 * Caches every surah's full text (and offline-search index) for offline reading. Iterates the 114
 * surahs through [QuranRepository.ensureSurahCached], which skips surahs already fresh — so a retry
 * after a dropped connection resumes cheaply. Progress is reported via [setProgress] for the
 * Settings UI.
 */
@HiltWorker
internal class DownloadQuranWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val quranRepository: QuranRepository,
    private val userDataRepository: UserDataRepository,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.downloadForegroundInfo(progress = 0)

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        traceAsync("DownloadQuran", 0) {
            val sourceId = quranRepository.resolveTranslationSourceId(
                userDataRepository.userData.first().translationSourceId,
            )
            // Publish the target source up front so the UI can tell which translation source this
            // download is for (and show "not downloaded" once the user switches to another source).
            setProgress(workDataOf(PROGRESS_KEY to 0, SOURCE_KEY to sourceId))

            for (surahId in 1..SURAH_COUNT) {
                if (isStopped) return@traceAsync Result.failure()
                try {
                    quranRepository.ensureSurahCached(surahId, sourceId, allowStaleOnError = false)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: IOException) {
                    // Connection dropped mid-download: resume when the network constraint is met
                    // again. A bounded retry avoids looping on a persistent failure.
                    return@traceAsync if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
                } catch (_: Exception) {
                    return@traceAsync if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
                }
                setProgress(workDataOf(PROGRESS_KEY to (surahId * 100 / SURAH_COUNT), SOURCE_KEY to sourceId))
            }
            // Record the fully-downloaded source so a later source switch reads as "not downloaded".
            Result.success(workDataOf(SOURCE_KEY to sourceId))
        }
    }

    companion object {
        /** Progress percent (0..100) published via [setProgress]; read by the download manager. */
        const val PROGRESS_KEY = "download_progress"

        /**
         * The translation source this download targets, published in both progress and the success
         * output so the download manager can tell a finished download apart from the active source.
         */
        const val SOURCE_KEY = "download_source_id"

        /** The Qur'an always has 114 surahs. */
        private const val SURAH_COUNT = 114

        /** Cap on automatic retries before the download is reported as failed. */
        private const val MAX_ATTEMPTS = 5

        fun downloadWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setConstraints(DownloadConstraints)
            .setInputData(DownloadQuranWorker::class.delegatedData())
            .build()
    }
}
