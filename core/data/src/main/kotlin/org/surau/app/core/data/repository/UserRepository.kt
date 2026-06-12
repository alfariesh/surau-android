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

/**
 * Server-side user profile and preferences for signed-in users.
 */
interface UserRepository {

    /**
     * Adopts the server's reader preferences (translation source, reader mode, recitation) into
     * local settings where the local value is still unset. Called once after login.
     */
    suspend fun pullPreferencesIntoSettings()

    /**
     * Pushes the local reader preferences to the backend. Fire-and-forget; failures are ignored
     * (local settings remain authoritative on this device).
     */
    suspend fun pushReaderPreferences()

    /**
     * Marks onboarding complete on the backend if the account still requires it, sending the
     * current local reader preferences. Milestone 1 has no onboarding UI.
     */
    suspend fun completeOnboardingIfNeeded()
}
