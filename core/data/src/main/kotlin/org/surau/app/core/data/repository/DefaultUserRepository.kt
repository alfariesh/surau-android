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

import kotlinx.coroutines.flow.first
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.user.OnboardingRequestDto
import org.surau.app.core.network.model.user.PreferencesPatchRequestDto
import org.surau.app.core.network.retrofit.SurauUserApi
import javax.inject.Inject

internal class DefaultUserRepository @Inject constructor(
    private val userApi: SurauUserApi,
    private val userDataRepository: UserDataRepository,
) : UserRepository {

    override suspend fun pullPreferencesIntoSettings() {
        val account = try {
            apiCall { userApi.profile() }
        } catch (_: Exception) {
            return
        }

        val local = userDataRepository.userData.first()

        if (local.translationSourceId == null) {
            account.preferences.quranTranslationSourceId?.let {
                userDataRepository.setTranslationSourceId(it)
            }
        }
        if (local.recitationId == null) {
            account.preferences.quranRecitationId?.let {
                userDataRepository.setRecitationId(it)
            }
        }
        account.preferences.readerMode?.toReaderModeOrNull()?.let {
            userDataRepository.setReaderMode(it)
        }
    }

    override suspend fun pushReaderPreferences() {
        val local = userDataRepository.userData.first()
        try {
            apiCall {
                userApi.patchPreferences(
                    PreferencesPatchRequestDto(
                        readerMode = local.readerMode.asBackendValue(),
                        quranTranslationSourceId = local.translationSourceId,
                        quranRecitationId = local.recitationId,
                    ),
                )
            }
        } catch (_: Exception) {
            // Local settings stay authoritative on this device.
        }
    }

    override suspend fun completeOnboardingIfNeeded() {
        val account = try {
            apiCall { userApi.profile() }
        } catch (_: Exception) {
            return
        }
        if (!account.onboardingRequired) return

        val local = userDataRepository.userData.first()
        try {
            apiCall {
                userApi.completeOnboarding(
                    OnboardingRequestDto(
                        preferredUiLang = "id",
                        preferredContentLang = "id",
                        readerMode = local.readerMode.asBackendValue(),
                        quranTranslationSourceId = local.translationSourceId,
                        quranRecitationId = local.recitationId,
                    ),
                )
            }
        } catch (_: Exception) {
            // Retried on a later login; onboarding is non-blocking in milestone 1.
        }
    }
}

internal fun ReaderMode.asBackendValue(): String = when (this) {
    ReaderMode.ARABIC_TRANSLATION -> "arabic_translation"
    ReaderMode.TRANSLATION_ONLY -> "translation_only"
    ReaderMode.ARABIC_ONLY -> "arabic_only"
}

internal fun String.toReaderModeOrNull(): ReaderMode? = when (this) {
    "arabic_translation" -> ReaderMode.ARABIC_TRANSLATION
    "translation_only" -> ReaderMode.TRANSLATION_ONLY
    "arabic_only" -> ReaderMode.ARABIC_ONLY
    else -> null
}
