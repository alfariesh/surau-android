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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
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
) : QuranDownloadManager {

    override val downloadState: Flow<QuranDownloadState> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(DOWNLOAD_WORK_NAME)
            .map { infos -> infos.firstOrNull().toDownloadState() }
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

private fun WorkInfo?.toDownloadState(): QuranDownloadState = when (this?.state) {
    null, WorkInfo.State.CANCELLED -> QuranDownloadState.Idle
    WorkInfo.State.SUCCEEDED -> QuranDownloadState.Completed
    WorkInfo.State.FAILED -> QuranDownloadState.Failed
    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED ->
        QuranDownloadState.Running(progress.getInt(DownloadQuranWorker.PROGRESS_KEY, 0))
}
