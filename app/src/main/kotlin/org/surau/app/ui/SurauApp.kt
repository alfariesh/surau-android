/*
 * Copyright 2022 The Android Open Source Project
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

package org.surau.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.surau.app.R
import org.surau.app.core.designsystem.component.SurauBackground
import org.surau.app.core.navigation.Navigator
import org.surau.app.core.navigation.toEntries
import org.surau.app.feature.activity.api.navigation.navigateToActivity
import org.surau.app.feature.activity.impl.navigation.activityEntry
import org.surau.app.feature.auth.api.navigation.ResetPasswordNavKey
import org.surau.app.feature.auth.api.navigation.WelcomeNavKey
import org.surau.app.feature.auth.api.navigation.navigateToLogin
import org.surau.app.feature.auth.api.navigation.navigateToManageAccount
import org.surau.app.feature.auth.impl.navigation.accountEntries
import org.surau.app.feature.auth.impl.navigation.authEntries
import org.surau.app.feature.quran.api.navigation.QuranHomeNavKey
import org.surau.app.feature.quran.impl.navigation.quranBookmarksEntry
import org.surau.app.feature.quran.impl.navigation.quranHomeEntry
import org.surau.app.feature.quran.impl.navigation.quranSearchEntry
import org.surau.app.feature.quran.impl.navigation.surahFlowEntry
import org.surau.app.feature.quran.impl.navigation.surahReaderEntry
import org.surau.app.feature.settings.api.navigation.navigateToSettings
import org.surau.app.feature.settings.impl.navigation.settingsEntry
import org.surau.app.util.AppLanguage

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SurauApp(
    appState: SurauAppState,
    shouldShowWelcome: Boolean,
    appVersionName: String,
    modifier: Modifier = Modifier,
    resetPasswordToken: String? = null,
) {
    SurauBackground(modifier = modifier) {
        val snackbarHostState = remember { SnackbarHostState() }

        val isOffline by appState.isOffline.collectAsStateWithLifecycle()

        // If user is not connected to the internet show a snack bar to inform them.
        val notConnectedMessage = stringResource(R.string.not_connected)
        LaunchedEffect(isOffline) {
            if (isOffline) {
                snackbarHostState.showSnackbar(
                    message = notConnectedMessage,
                    duration = Indefinite,
                )
            }
        }

        val navigator = remember { Navigator(appState.navigationState) }

        // First launch: offer guest/sign-in once. Never blocks later launches. A pending
        // reset-password deep link takes precedence and suppresses the welcome detour.
        LaunchedEffect(shouldShowWelcome, resetPasswordToken) {
            if (resetPasswordToken == null &&
                shouldShowWelcome &&
                appState.navigationState.currentKey == QuranHomeNavKey
            ) {
                navigator.navigate(WelcomeNavKey)
            }
        }

        // Reset-password deep link: route straight to the reset screen with the emailed token.
        LaunchedEffect(resetPasswordToken) {
            if (resetPasswordToken != null &&
                appState.navigationState.currentKey == QuranHomeNavKey
            ) {
                navigator.navigate(ResetPasswordNavKey(resetPasswordToken))
            }
        }

        Scaffold(
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(
                            WindowInsets.ime,
                        ),
                    ),
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                val context = LocalContext.current
                val entryProvider = entryProvider<NavKey> {
                    quranHomeEntry(
                        navigator = navigator,
                        onSettingsClick = navigator::navigateToSettings,
                        onActivityClick = navigator::navigateToActivity,
                    )
                    surahReaderEntry(navigator)
                    surahFlowEntry(navigator)
                    quranSearchEntry(navigator)
                    quranBookmarksEntry(navigator)
                    activityEntry(
                        navigator = navigator,
                        onLoginClick = navigator::navigateToLogin,
                    )
                    authEntries(
                        navigator = navigator,
                        onAuthFlowDone = { navigator.navigate(QuranHomeNavKey) },
                    )
                    accountEntries(
                        navigator = navigator,
                        onSignedOut = { navigator.navigate(WelcomeNavKey) },
                    )
                    settingsEntry(
                        navigator = navigator,
                        onSignInClick = navigator::navigateToLogin,
                        onManageAccountClick = navigator::navigateToManageAccount,
                        appVersionName = appVersionName,
                        currentLanguageTag = AppLanguage.current(context),
                        onChangeLanguage = { tag -> AppLanguage.apply(context, tag) },
                    )
                }

                // Two-pane list-detail (QuranHome | SurahReader) on expanded widths; the strategy
                // collapses to a single pane on compact, and ignores entries without pane metadata,
                // so every other screen keeps its current single-pane behaviour.
                val sceneStrategy = rememberListDetailSceneStrategy<NavKey>()
                NavDisplay(
                    entries = appState.navigationState.toEntries(entryProvider),
                    sceneStrategies = listOf(sceneStrategy),
                    onBack = { navigator.goBack() },
                )
            }
        }
    }
}
