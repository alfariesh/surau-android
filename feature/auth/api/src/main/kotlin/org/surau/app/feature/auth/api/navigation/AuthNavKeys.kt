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
 * First-launch welcome: continue as guest or sign in. Pushed once over the home screen and
 * never blocks subsequent launches.
 */
@Serializable
object WelcomeNavKey : NavKey

@Serializable
object LoginNavKey : NavKey

@Serializable
object RegisterNavKey : NavKey

/**
 * Email verification with a 6-digit OTP, reached after registration or a 403
 * AUTH_EMAIL_NOT_VERIFIED login.
 */
@Serializable
data class VerifyEmailNavKey(val email: String) : NavKey

@Serializable
object ForgotPasswordNavKey : NavKey

/**
 * Password reset. [token] is prefilled when arriving via deep link (deferred); the user can also
 * paste the emailed token manually.
 */
@Serializable
data class ResetPasswordNavKey(val token: String? = null) : NavKey

fun Navigator.navigateToLogin() = navigate(LoginNavKey)

fun Navigator.navigateToRegister() = navigate(RegisterNavKey)

fun Navigator.navigateToVerifyEmail(email: String) = navigate(VerifyEmailNavKey(email))

fun Navigator.navigateToForgotPassword() = navigate(ForgotPasswordNavKey)

fun Navigator.navigateToResetPassword(token: String? = null) = navigate(ResetPasswordNavKey(token))
