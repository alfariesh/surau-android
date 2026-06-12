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

package org.surau.app.core.network.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("onboarding_version") val onboardingVersion: Int = 0,
    @SerialName("personalization_enabled") val personalizationEnabled: Boolean = false,
)

@Serializable
data class UserPreferencesDto(
    @SerialName("preferred_ui_lang") val preferredUiLang: String = "id",
    @SerialName("preferred_content_lang") val preferredContentLang: String = "id",
    @SerialName("reader_mode") val readerMode: String? = null,
    @SerialName("quran_translation_source_id") val quranTranslationSourceId: String? = null,
    @SerialName("quran_recitation_id") val quranRecitationId: String? = null,
)

/**
 * `GET /user/profile` — the user's identity flattened together with nested profile and
 * preferences.
 */
@Serializable
data class UserAccountDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String? = null,
    @SerialName("email") val email: String = "",
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("profile") val profile: UserProfileDto = UserProfileDto(),
    @SerialName("preferences") val preferences: UserPreferencesDto = UserPreferencesDto(),
    @SerialName("onboarding_required") val onboardingRequired: Boolean = false,
)

@Serializable
data class OnboardingRequestDto(
    @SerialName("preferred_ui_lang") val preferredUiLang: String? = null,
    @SerialName("preferred_content_lang") val preferredContentLang: String? = null,
    @SerialName("reader_mode") val readerMode: String? = null,
    @SerialName("quran_translation_source_id") val quranTranslationSourceId: String? = null,
    @SerialName("quran_recitation_id") val quranRecitationId: String? = null,
)

@Serializable
data class PreferencesPatchRequestDto(
    @SerialName("reader_mode") val readerMode: String? = null,
    @SerialName("quran_translation_source_id") val quranTranslationSourceId: String? = null,
    @SerialName("quran_recitation_id") val quranRecitationId: String? = null,
)
