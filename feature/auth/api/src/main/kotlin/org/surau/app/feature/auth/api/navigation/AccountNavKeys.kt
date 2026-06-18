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

package org.surau.app.feature.auth.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.surau.app.core.navigation.Navigator

/**
 * "Kelola Akun" hub, reached from Settings for signed-in users. Lists the account actions, each of
 * which opens its own focused screen.
 */
@Serializable
object AccountNavKey : NavKey

@Serializable
object EditProfileNavKey : NavKey

@Serializable
object ChangePasswordNavKey : NavKey

/**
 * Email change: a single screen that walks request → OTP verification internally. When reached from
 * the emailed change-email link, [token] auto-verifies the change (requires an active session).
 */
@Serializable
data class ChangeEmailNavKey(val token: String? = null) : NavKey

@Serializable
object SessionsNavKey : NavKey

@Serializable
object EmailPreferencesNavKey : NavKey

@Serializable
object DeleteAccountNavKey : NavKey

fun Navigator.navigateToManageAccount() = navigate(AccountNavKey)

fun Navigator.navigateToEditProfile() = navigate(EditProfileNavKey)

fun Navigator.navigateToChangePassword() = navigate(ChangePasswordNavKey)

fun Navigator.navigateToChangeEmail() = navigate(ChangeEmailNavKey())

fun Navigator.navigateToSessions() = navigate(SessionsNavKey)

fun Navigator.navigateToEmailPreferences() = navigate(EmailPreferencesNavKey)

fun Navigator.navigateToDeleteAccount() = navigate(DeleteAccountNavKey)
