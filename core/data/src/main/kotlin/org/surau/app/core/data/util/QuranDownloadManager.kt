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

package org.surau.app.core.data.util

import kotlinx.coroutines.flow.Flow

/**
 * Drives the "download the whole Qur'an for offline use" job. Implemented over WorkManager in
 * `sync:work`; the UI talks only to this interface so it never touches WorkManager directly.
 */
interface QuranDownloadManager {
    /** The current state of the (single, unique) download job. */
    val downloadState: Flow<QuranDownloadState>

    /** Enqueues the download (no-op if one is already running). */
    fun startDownload()

    /** Cancels an in-progress download. Already-cached surahs stay cached. */
    fun cancelDownload()
}

/**
 * Observable state of the offline Qur'an download.
 */
sealed interface QuranDownloadState {
    /** No download has run, or the last one was cancelled. */
    data object Idle : QuranDownloadState

    /** A download is enqueued or running; [percent] is 0..100. */
    data class Running(val percent: Int) : QuranDownloadState

    /** The whole Qur'an is downloaded. */
    data object Completed : QuranDownloadState

    /** The download failed (e.g. exhausted retries). */
    data object Failed : QuranDownloadState
}
