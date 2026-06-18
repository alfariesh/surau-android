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

package org.surau.app.sync.status

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.data.util.QuranDownloadManager
import org.surau.app.core.data.util.QuranDownloadState
import org.surau.app.sync.initializers.DOWNLOAD_WORK_NAME
import org.surau.app.sync.workers.DownloadQuranWorker
import javax.inject.Inject

/**
 * [QuranDownloadManager] backed by [WorkInfo] from [WorkManager].
 */
internal class WorkManagerQuranDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    userDataRepository: UserDataRepository,
    private val quranRepository: QuranRepository,
) : QuranDownloadManager {

    /** The active translation source, resolved (pref → default → first); re-resolved only on change. */
    private val currentSourceId: Flow<String> =
        userDataRepository.userData
            .map { it.translationSourceId }
            .distinctUntilChanged()
            .map { quranRepository.resolveTranslationSourceId(it) }

    override val downloadState: Flow<QuranDownloadState> =
        combine(
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(DOWNLOAD_WORK_NAME),
            currentSourceId,
        ) { infos, sourceId -> infos.firstOrNull().toDownloadState(sourceId) }
            .conflate()

    override fun startDownload() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            DOWNLOAD_WORK_NAME,
            // KEEP: a running download isn't restarted; a finished one is replaced on the next tap.
            ExistingWorkPolicy.KEEP,
            DownloadQuranWorker.downloadWork(),
        )
    }

    override fun cancelDownload() {
        WorkManager.getInstance(context).cancelUniqueWork(DOWNLOAD_WORK_NAME)
    }
}

private fun WorkInfo?.toDownloadState(currentSourceId: String): QuranDownloadState {
    this ?: return resolveDownloadState(null, null, currentSourceId, 0)
    // The target source is in the output (when finished) or the latest progress (while running).
    val workSourceId = outputData.getString(DownloadQuranWorker.SOURCE_KEY)
        ?: progress.getString(DownloadQuranWorker.SOURCE_KEY)
    return resolveDownloadState(
        state = state,
        workSourceId = workSourceId,
        currentSourceId = currentSourceId,
        percent = progress.getInt(DownloadQuranWorker.PROGRESS_KEY, 0),
    )
}

/**
 * Maps a download job's WorkManager [state] to a [QuranDownloadState] for the **active** translation
 * source. A job that finished/runs for a *different* source than the one now selected reads as [Idle]
 * (the active source has no offline index yet) so the UI prompts a fresh download instead of falsely
 * reporting "downloaded". A null [workSourceId] (legacy job / not yet published) is treated as a match.
 */
internal fun resolveDownloadState(
    state: WorkInfo.State?,
    workSourceId: String?,
    currentSourceId: String,
    percent: Int,
): QuranDownloadState = when (state) {
    null, WorkInfo.State.CANCELLED -> QuranDownloadState.Idle
    WorkInfo.State.SUCCEEDED ->
        if (workSourceId == null || workSourceId == currentSourceId) {
            QuranDownloadState.Completed
        } else {
            QuranDownloadState.Idle
        }
    WorkInfo.State.FAILED -> QuranDownloadState.Failed
    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED ->
        if (workSourceId != null && workSourceId != currentSourceId) {
            QuranDownloadState.Idle
        } else {
            QuranDownloadState.Running(percent)
        }
}
