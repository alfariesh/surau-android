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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.surau.app.R
import org.surau.app.core.designsystem.component.SurauBackground
import org.surau.app.core.designsystem.component.SurauNavigationBar
import org.surau.app.core.designsystem.component.SurauNavigationBarItem
import org.surau.app.core.designsystem.icon.SurauIcons
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
import org.surau.app.feature.quran.api.navigation.SurahFlowNavKey
import org.surau.app.feature.quran.api.navigation.navigateToSurahReader
import org.surau.app.feature.quran.impl.EmbeddedSurahFlowPlayer
import org.surau.app.feature.quran.impl.navigation.quranBookmarksEntry
import org.surau.app.feature.quran.impl.navigation.quranHomeEntry
import org.surau.app.feature.quran.impl.navigation.quranSearchEntry
import org.surau.app.feature.quran.impl.navigation.surahReaderEntry
import org.surau.app.feature.settings.api.navigation.navigateToSettings
import org.surau.app.feature.settings.impl.navigation.settingsEntry
import org.surau.app.navigation.HadithNavKey
import org.surau.app.navigation.HomeNavKey
import org.surau.app.navigation.KitabsNavKey
import org.surau.app.navigation.ProfileNavKey
import org.surau.app.navigation.TOP_LEVEL_NAV_ITEMS
import org.surau.app.ui.player.AppPlayerViewModel
import org.surau.app.ui.player.ExpandablePlayerScaffold
import org.surau.app.ui.player.MiniPlayer
import org.surau.app.ui.player.MiniPlayerHeight
import org.surau.app.ui.player.PlayerAnchor
import org.surau.app.ui.player.rememberPlayerSheetState
import org.surau.app.ui.home.HomeScreen
import org.surau.app.ui.profile.ProfileScreen
import org.surau.app.ui.screens.ComingSoonScreen
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
        val scope = rememberCoroutineScope()

        // First launch: offer guest/sign-in once. Never blocks later launches. A pending
        // reset-password deep link takes precedence and suppresses the welcome detour.
        LaunchedEffect(shouldShowWelcome, resetPasswordToken) {
            if (resetPasswordToken == null &&
                shouldShowWelcome &&
                appState.navigationState.currentKey == HomeNavKey
            ) {
                navigator.navigate(WelcomeNavKey)
            }
        }

        // Reset-password deep link: route straight to the reset screen with the emailed token.
        LaunchedEffect(resetPasswordToken) {
            if (resetPasswordToken != null &&
                appState.navigationState.currentKey == HomeNavKey
            ) {
                navigator.navigate(ResetPasswordNavKey(resetPasswordToken))
            }
        }

        // --- Expandable player state -------------------------------------------------------------
        val playerViewModel: AppPlayerViewModel = hiltViewModel()
        val playerState by playerViewModel.state.collectAsStateWithLifecycle()
        val sheetState = rememberPlayerSheetState()

        // A pending request to open the Flow player for a surah that may not be playing yet (the
        // embedded Flow view-model auto-starts surah-mode playback on mount). `expandRequest` makes
        // each "Flow" tap distinct so the sheet re-expands even when a session already exists.
        var pendingFlow by remember { mutableStateOf<SurahFlowNavKey?>(null) }
        var expandRequest by remember { mutableIntStateOf(0) }
        var handledExpand by remember { mutableIntStateOf(0) }

        val activeSurahId = pendingFlow?.surahId ?: playerState.surahId
        val hasSession = activeSurahId != null

        // Drop the pending request once real playback has caught up to it.
        LaunchedEffect(playerState.surahId, pendingFlow) {
            val request = pendingFlow
            if (request != null && playerState.surahId == request.surahId) {
                pendingFlow = null
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
                    .consumeWindowInsets(padding),
            ) {
                val context = LocalContext.current
                val density = LocalDensity.current

                // The bottom navigation bar owns the bottom system inset; we measure its full height
                // (content + inset) so content reserves space above it and the player anchors line up.
                var navBarHeightPx by remember { mutableIntStateOf(0) }
                val navBarHeightDp = with(density) { navBarHeightPx.toDp() }
                val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }
                val peekPx = navBarHeightPx + miniPlayerHeightPx

                // Drive the sheet: hide when no session, expand on a Flow request, otherwise peek.
                LaunchedEffect(hasSession, expandRequest, sheetState.isLaidOut) {
                    if (!sheetState.isLaidOut) return@LaunchedEffect
                    when {
                        !hasSession -> sheetState.hide()
                        expandRequest != handledExpand -> {
                            handledExpand = expandRequest
                            sheetState.expand()
                        }
                        sheetState.draggable.settledValue == PlayerAnchor.Hidden -> sheetState.show()
                    }
                }

                val entryProvider = entryProvider<NavKey> {
                    entry<HomeNavKey> {
                        HomeScreen(
                            onContinueReading = { surahId, ayahNumber ->
                                navigator.navigateToSurahReader(surahId, ayahNumber)
                            },
                            onSeeActivity = navigator::navigateToActivity,
                            onSignIn = navigator::navigateToLogin,
                        )
                    }
                    quranHomeEntry(
                        navigator = navigator,
                        onSettingsClick = navigator::navigateToSettings,
                        onActivityClick = navigator::navigateToActivity,
                    )
                    surahReaderEntry(
                        navigator = navigator,
                        onFlowClick = { surahId, ayahNumber ->
                            pendingFlow = SurahFlowNavKey(surahId, ayahNumber)
                            expandRequest++
                        },
                    )
                    quranSearchEntry(navigator)
                    quranBookmarksEntry(navigator)
                    entry<HadithNavKey> {
                        ComingSoonScreen(
                            title = stringResource(R.string.tab_hadith),
                            icon = SurauIcons.AutoStories,
                            body = stringResource(R.string.coming_soon_body),
                        )
                    }
                    entry<KitabsNavKey> {
                        ComingSoonScreen(
                            title = stringResource(R.string.tab_kitabs),
                            icon = SurauIcons.LibraryBooks,
                            body = stringResource(R.string.coming_soon_body),
                        )
                    }
                    entry<ProfileNavKey> {
                        ProfileScreen(
                            appVersionName = appVersionName,
                            onManageAccount = navigator::navigateToManageAccount,
                            onSettings = navigator::navigateToSettings,
                            onActivity = navigator::navigateToActivity,
                            onSignIn = navigator::navigateToLogin,
                        )
                    }
                    activityEntry(
                        navigator = navigator,
                        onLoginClick = navigator::navigateToLogin,
                    )
                    authEntries(
                        navigator = navigator,
                        onAuthFlowDone = { navigator.navigate(HomeNavKey) },
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

                // Content. Top & horizontal safe insets are handled here; the bottom is reserved for
                // the navigation bar (and the mini-player when a session is active). imePadding keeps
                // text fields visible when the keyboard opens.
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                            ),
                        )
                        .padding(
                            bottom = navBarHeightDp + if (hasSession) MiniPlayerHeight else 0.dp,
                        )
                        .imePadding(),
                ) {
                    val sceneStrategy = rememberListDetailSceneStrategy<NavKey>()
                    NavDisplay(
                        entries = appState.navigationState.toEntries(entryProvider),
                        sceneStrategies = listOf(sceneStrategy),
                        onBack = { navigator.goBack() },
                    )
                }

                // Expandable player overlay (mini card ↔ full Flow). Persists across tabs because it
                // reads the process-singleton player state and lives above the nav host.
                if (hasSession) {
                    ExpandablePlayerScaffold(
                        sheetState = sheetState,
                        peekPx = peekPx,
                        miniHeight = MiniPlayerHeight,
                        collapsedContent = {
                            MiniPlayer(
                                state = playerState,
                                onExpand = { scope.launch { sheetState.expand() } },
                                onPlayPause = playerViewModel::playPause,
                                onPrevious = playerViewModel::previous,
                                onNext = playerViewModel::next,
                            )
                        },
                        expandedContent = {
                            val surahId = activeSurahId
                            if (surahId != null) {
                                EmbeddedSurahFlowPlayer(
                                    surahId = surahId,
                                    initialAyah = pendingFlow?.ayahNumber,
                                    onCollapse = { scope.launch { sheetState.collapse() } },
                                )
                            }
                        },
                    )

                    // Back collapses an expanded player before falling through to navigation.
                    BackHandler(enabled = sheetState.isExpanding) {
                        scope.launch { sheetState.collapse() }
                    }
                }

                // Bottom navigation: 5 tabs, animated label (selected shows label, others icon-only).
                // Slides down + fades as the player expands, mirroring the sheet's progress.
                SurauBottomBar(
                    currentTopLevelKey = appState.navigationState.currentTopLevelKey,
                    onTabSelected = { key -> navigator.navigate(key) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { navBarHeightPx = it.height }
                        .graphicsLayer {
                            val progress = sheetState.progress()
                            translationY = progress * navBarHeightPx.toFloat()
                            alpha = 1f - progress
                        },
                )
            }
        }
    }
}

/**
 * The app's bottom navigation bar. The selected tab reveals its text label (sliding up from the
 * bottom); unselected tabs show only their icon — `alwaysShowLabel = false` gives Material 3's
 * built-in label animation.
 */
@Composable
private fun SurauBottomBar(
    currentTopLevelKey: NavKey,
    onTabSelected: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    SurauNavigationBar(modifier = modifier) {
        TOP_LEVEL_NAV_ITEMS.forEach { (key, item) ->
            val label = stringResource(item.iconTextId)
            SurauNavigationBarItem(
                selected = currentTopLevelKey == key,
                onClick = { onTabSelected(key) },
                alwaysShowLabel = false,
                icon = {
                    Icon(imageVector = item.unselectedIcon, contentDescription = label)
                },
                selectedIcon = {
                    Icon(imageVector = item.selectedIcon, contentDescription = label)
                },
                label = { Text(label) },
            )
        }
    }
}
