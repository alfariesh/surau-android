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

package org.surau.app.feature.auth.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.surau.app.core.navigation.Navigator
import org.surau.app.feature.auth.api.navigation.ForgotPasswordNavKey
import org.surau.app.feature.auth.api.navigation.LoginNavKey
import org.surau.app.feature.auth.api.navigation.RegisterNavKey
import org.surau.app.feature.auth.api.navigation.ResetPasswordNavKey
import org.surau.app.feature.auth.api.navigation.VerifyEmailNavKey
import org.surau.app.feature.auth.api.navigation.WelcomeNavKey
import org.surau.app.feature.auth.api.navigation.navigateToForgotPassword
import org.surau.app.feature.auth.api.navigation.navigateToLogin
import org.surau.app.feature.auth.api.navigation.navigateToRegister
import org.surau.app.feature.auth.api.navigation.navigateToResetPassword
import org.surau.app.feature.auth.api.navigation.navigateToVerifyEmail
import org.surau.app.feature.auth.impl.ForgotPasswordScreen
import org.surau.app.feature.auth.impl.LoginScreen
import org.surau.app.feature.auth.impl.RegisterScreen
import org.surau.app.feature.auth.impl.ResetPasswordScreen
import org.surau.app.feature.auth.impl.VerifyEmailScreen
import org.surau.app.feature.auth.impl.WelcomeScreen

/**
 * Registers every auth destination.
 *
 * @param onAuthFlowDone exits the auth sub-flow back to the app's home destination (used after
 * login succeeds or the user continues as guest).
 */
fun EntryProviderScope<NavKey>.authEntries(
    navigator: Navigator,
    onAuthFlowDone: () -> Unit,
) {
    entry<WelcomeNavKey> {
        WelcomeScreen(
            onContinueAsGuest = onAuthFlowDone,
            onSignIn = navigator::navigateToLogin,
        )
    }

    entry<LoginNavKey> {
        LoginScreen(
            onBackClick = navigator::goBack,
            onLoggedIn = onAuthFlowDone,
            onNeedsVerification = navigator::navigateToVerifyEmail,
            onRegisterClick = navigator::navigateToRegister,
            onForgotPasswordClick = navigator::navigateToForgotPassword,
        )
    }

    entry<RegisterNavKey> {
        RegisterScreen(
            onBackClick = navigator::goBack,
            onNeedsVerification = navigator::navigateToVerifyEmail,
            onSignInClick = navigator::navigateToLogin,
        )
    }

    entry<VerifyEmailNavKey> { navKey ->
        VerifyEmailScreen(
            email = navKey.email,
            onBackClick = navigator::goBack,
            onVerified = navigator::navigateToLogin,
        )
    }

    entry<ForgotPasswordNavKey> {
        ForgotPasswordScreen(
            onBackClick = navigator::goBack,
            onContinueToReset = navigator::navigateToResetPassword,
        )
    }

    entry<ResetPasswordNavKey> { navKey ->
        ResetPasswordScreen(
            initialToken = navKey.token,
            onBackClick = navigator::goBack,
            onResetDone = navigator::navigateToLogin,
        )
    }
}
