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

package org.surau.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.metrics.performance.JankStats
import androidx.tracing.trace
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.surau.app.MainActivityUiState.Loading
import org.surau.app.core.analytics.AnalyticsHelper
import org.surau.app.core.analytics.LocalAnalyticsHelper
import org.surau.app.core.data.util.NetworkMonitor
import org.surau.app.core.data.util.TimeZoneMonitor
import org.surau.app.core.designsystem.theme.HeroPalette
import org.surau.app.core.designsystem.theme.SeedPaletteStyle
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemePalette
import org.surau.app.core.model.data.ThemeStyle
import org.surau.app.core.ui.LocalTimeZone
import org.surau.app.ui.SurauApp
import org.surau.app.ui.rememberSurauAppState
import org.surau.app.util.AppLanguage
import org.surau.app.util.isSystemInDarkTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Applies the persisted per-app language on API < 33 (no-op on 33+).
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    /**
     * Lazily inject [JankStats], which is used to track jank throughout the app.
     */
    @Inject
    lateinit var lazyStats: dagger.Lazy<JankStats>

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var timeZoneMonitor: TimeZoneMonitor

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    private val viewModel: MainActivityViewModel by viewModels()

    /**
     * Reset-password token captured from a `https://surau.org/reset-password?token=…` deep link.
     * Tracked as Compose state so that a warm-start [onNewIntent] also routes into the reset flow.
     */
    private var deepLinkResetToken by mutableStateOf<String?>(null)

    /** Requests the Android 13+ notification permission so media/lock-screen controls can show. */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        deepLinkResetToken = extractResetToken(intent)
        requestNotificationPermissionIfNeeded()

        // We keep this as a mutable state, so that we can track changes inside the composition.
        // This allows us to react to dark/light mode changes.
        var themeSettings by mutableStateOf(
            ThemeSettings(
                darkTheme = resources.configuration.isSystemInDarkTheme,
                disableDynamicTheming = Loading.shouldDisableDynamicTheming,
                seedColorArgb = Loading.seedColorArgb,
                themeStyle = Loading.themeStyle,
                themeContrast = Loading.themeContrast,
                themePalette = Loading.themePalette,
                useMeshGradient = Loading.useMeshGradient,
            ),
        )

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    isSystemInDarkTheme(),
                    viewModel.uiState,
                ) { systemDark, uiState ->
                    ThemeSettings(
                        darkTheme = uiState.shouldUseDarkTheme(systemDark),
                        disableDynamicTheming = uiState.shouldDisableDynamicTheming,
                        seedColorArgb = uiState.seedColorArgb,
                        themeStyle = uiState.themeStyle,
                        themeContrast = uiState.themeContrast,
                        themePalette = uiState.themePalette,
                        useMeshGradient = uiState.useMeshGradient && meshRuntimeAllowed(),
                    )
                }
                    .onEach { themeSettings = it }
                    .map { it.darkTheme }
                    .distinctUntilChanged()
                    .collect { darkTheme ->
                        trace("surauEdgeToEdge") {
                            // Turn off the decor fitting system windows, which allows us to handle insets,
                            // including IME animations, and go edge-to-edge.
                            // This is the same parameters as the default enableEdgeToEdge call, but we manually
                            // resolve whether or not to show dark theme using uiState, since it can be different
                            // than the configuration's dark theme value based on the user preference.
                            enableEdgeToEdge(
                                statusBarStyle = SystemBarStyle.auto(
                                    lightScrim = android.graphics.Color.TRANSPARENT,
                                    darkScrim = android.graphics.Color.TRANSPARENT,
                                ) { darkTheme },
                                navigationBarStyle = SystemBarStyle.auto(
                                    lightScrim = lightScrim,
                                    darkScrim = darkScrim,
                                ) { darkTheme },
                            )
                        }
                    }
            }
        }

        // Keep the splash screen on-screen until the UI state is loaded. This condition is
        // evaluated each time the app needs to be redrawn so it should be fast to avoid blocking
        // the UI.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.shouldKeepSplashScreen() }

        setContent {
            val appState = rememberSurauAppState(
                networkMonitor = networkMonitor,
                timeZoneMonitor = timeZoneMonitor,
            )

            val currentTimeZone by appState.currentTimeZone.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CompositionLocalProvider(
                LocalAnalyticsHelper provides analyticsHelper,
                LocalTimeZone provides currentTimeZone,
            ) {
                SurauTheme(
                    darkTheme = themeSettings.darkTheme,
                    disableDynamicTheming = themeSettings.disableDynamicTheming,
                    seedColor = themeSettings.seedColorArgb
                        .takeIf { it != 0L }
                        ?.let { Color(it) }
                        ?: Color.Unspecified,
                    seedStyle = themeSettings.themeStyle.toSeedPaletteStyle(),
                    seedContrast = themeSettings.themeContrast.toContrastLevel(),
                    heroPalette = themeSettings.themePalette.toHeroPalette(),
                    meshGradientEnabled = themeSettings.useMeshGradient,
                ) {
                    SurauApp(
                        appState = appState,
                        shouldShowWelcome = uiState.shouldShowWelcome,
                        resetPasswordToken = deepLinkResetToken,
                        onResetPasswordTokenConsumed = { deepLinkResetToken = null },
                        appVersionName = BuildConfig.VERSION_NAME,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        lazyStats.get().isTrackingEnabled = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractResetToken(intent)?.let { deepLinkResetToken = it }
    }

    /**
     * Runtime gates for the decorative mesh gradient: suppressed under battery saver or when the user
     * has turned animations off (reduced motion), regardless of the stored preference.
     */
    private fun meshRuntimeAllowed(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager?.isPowerSaveMode == true) return false
        val animatorScale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        return animatorScale != 0f
    }
}

/**
 * Pulls the reset-password token out of a `VIEW` deep link such as
 * `https://surau.org/reset-password?token=abc`. Returns null for any other intent.
 */
private fun extractResetToken(intent: Intent?): String? =
    intent
        ?.takeIf { it.action == Intent.ACTION_VIEW }
        ?.data
        ?.takeIf { it.path?.startsWith("/reset-password") == true }
        ?.getQueryParameter("token")
        ?.takeIf { it.isNotBlank() }

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * Class for the system theme settings.
 * This wrapping class allows us to combine all the changes and prevent unnecessary recompositions.
 */
data class ThemeSettings(
    val darkTheme: Boolean,
    val disableDynamicTheming: Boolean,
    val seedColorArgb: Long = 0L,
    val themeStyle: ThemeStyle = ThemeStyle.TONAL_SPOT,
    val themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    val themePalette: ThemePalette = ThemePalette.SURAU_BASE,
    val useMeshGradient: Boolean = false,
)

/** Maps the persisted domain [ThemePalette] to the design-system palette. */
private fun ThemePalette.toHeroPalette(): HeroPalette = when (this) {
    ThemePalette.SURAU_BASE -> HeroPalette.SURAU_BASE
    ThemePalette.DEFAULT -> HeroPalette.DEFAULT
    ThemePalette.MOUVE -> HeroPalette.MOUVE
    ThemePalette.SKY -> HeroPalette.SKY
    ThemePalette.MINT -> HeroPalette.MINT
    ThemePalette.DISCORD -> HeroPalette.DISCORD
    ThemePalette.UBER -> HeroPalette.UBER
    ThemePalette.AIRBNB -> HeroPalette.AIRBNB
}

/** Maps the persisted domain [ThemeStyle] to the design-system's generator style. */
private fun ThemeStyle.toSeedPaletteStyle(): SeedPaletteStyle = when (this) {
    ThemeStyle.TONAL_SPOT -> SeedPaletteStyle.TONAL_SPOT
    ThemeStyle.VIBRANT -> SeedPaletteStyle.VIBRANT
    ThemeStyle.EXPRESSIVE -> SeedPaletteStyle.EXPRESSIVE
    ThemeStyle.NEUTRAL -> SeedPaletteStyle.NEUTRAL
}

/** Maps the persisted domain [ThemeContrast] to the generator's 0f..1f contrast level. */
private fun ThemeContrast.toContrastLevel(): Float = when (this) {
    ThemeContrast.STANDARD -> 0f
    ThemeContrast.MEDIUM -> 0.5f
    ThemeContrast.HIGH -> 1f
}
