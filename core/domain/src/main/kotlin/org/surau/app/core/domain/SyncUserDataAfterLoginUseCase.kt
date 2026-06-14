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

package org.surau.app.core.domain

import org.surau.app.core.data.repository.BookmarkRepository
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.data.repository.UserRepository
import javax.inject.Inject

/**
 * Runs the post-login data reconciliation:
 * 1. adopt server reader preferences where local ones are unset,
 * 2. merge guest reading progress with the server position (newest wins),
 * 3. merge guest bookmarks with the server's saved items (newest wins),
 * 4. silently complete backend onboarding (milestone 1 has no onboarding UI).
 *
 * Each step is best-effort and independent; a failing step never blocks login.
 */
class SyncUserDataAfterLoginUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val quranProgressRepository: QuranProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
) {
    suspend operator fun invoke() {
        userRepository.pullPreferencesIntoSettings()
        quranProgressRepository.reconcile()
        bookmarkRepository.reconcile()
        userRepository.completeOnboardingIfNeeded()
    }
}
