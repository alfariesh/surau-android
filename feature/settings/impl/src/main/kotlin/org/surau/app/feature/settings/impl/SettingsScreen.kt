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

package org.surau.app.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.core.data.util.QuranDownloadState
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauButtonGroup
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauOutlinedButton
import org.surau.app.core.designsystem.component.SurauSurface
import org.surau.app.core.designsystem.component.SurauSwitch
import org.surau.app.core.designsystem.component.SurauTextButton
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.supportsDynamicTheming
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemeStyle
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.core.ui.TrackScreenViewEvent
import org.surau.app.feature.settings.impl.SettingsUiState.Loading
import org.surau.app.feature.settings.impl.SettingsUiState.Success

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onManageAccountClick: () -> Unit,
    appVersionName: String,
    modifier: Modifier = Modifier,
    currentLanguageTag: String = "",
    onChangeLanguage: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        appVersionName = appVersionName,
        currentLanguageTag = currentLanguageTag,
        onChangeLanguage = onChangeLanguage,
        onBackClick = onBackClick,
        onSignInClick = onSignInClick,
        onManageAccountClick = onManageAccountClick,
        onLogout = viewModel::logout,
        onChangeDynamicColorPreference = viewModel::updateDynamicColorPreference,
        onChangeDarkThemeConfig = viewModel::updateDarkThemeConfig,
        onChangeThemeSeed = viewModel::updateThemeSeed,
        onChangeThemeStyle = viewModel::updateThemeStyle,
        onChangeThemeContrast = viewModel::updateThemeContrast,
        onChangeMeshGradient = viewModel::updateMeshGradient,
        onChangeReaderMode = viewModel::updateReaderMode,
        onChangeTranslationSource = viewModel::updateTranslationSource,
        onChangeRecitation = viewModel::updateRecitation,
        onChangeArabicFontScale = viewModel::updateArabicFontScale,
        onChangeShowTransliteration = viewModel::updateShowTransliteration,
        onChangeShowTranslation = viewModel::updateShowTranslation,
        onChangeArabicLineSpacing = viewModel::updateArabicLineSpacing,
        onChangeTranslationScale = viewModel::updateTranslationScale,
        onChangeKeepScreenOn = viewModel::updateKeepScreenOn,
        onStartQuranDownload = viewModel::startQuranDownload,
        onCancelQuranDownload = viewModel::cancelQuranDownload,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    appVersionName: String,
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onManageAccountClick: () -> Unit,
    onLogout: () -> Unit,
    onChangeDynamicColorPreference: (Boolean) -> Unit,
    onChangeDarkThemeConfig: (DarkThemeConfig) -> Unit,
    onChangeReaderMode: (ReaderMode) -> Unit,
    onChangeTranslationSource: (String) -> Unit,
    onChangeArabicFontScale: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onChangeThemeSeed: (Long) -> Unit = {},
    onChangeThemeStyle: (ThemeStyle) -> Unit = {},
    onChangeThemeContrast: (ThemeContrast) -> Unit = {},
    onChangeMeshGradient: (Boolean) -> Unit = {},
    onChangeRecitation: (String) -> Unit = {},
    onChangeShowTransliteration: (Boolean) -> Unit = {},
    onChangeShowTranslation: (Boolean) -> Unit = {},
    onChangeArabicLineSpacing: (Float) -> Unit = {},
    onChangeTranslationScale: (Float) -> Unit = {},
    onChangeKeepScreenOn: (Boolean) -> Unit = {},
    onStartQuranDownload: () -> Unit = {},
    onCancelQuranDownload: () -> Unit = {},
    currentLanguageTag: String = "",
    onChangeLanguage: (String) -> Unit = {},
    supportDynamicColor: Boolean = supportsDynamicTheming(),
) {
    TrackScreenViewEvent(screenName = "Settings")

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.testTag("settings:back")) {
                Icon(
                    imageVector = SurauIcons.ArrowBack,
                    contentDescription = stringResource(R.string.feature_settings_impl_back),
                )
            }
            Text(
                text = stringResource(R.string.feature_settings_impl_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        when (uiState) {
            Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SurauLoadingWheel(
                    contentDesc = stringResource(R.string.feature_settings_impl_loading),
                )
            }

            is Success -> Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                AccountSection(
                    authState = uiState.authState,
                    onSignInClick = onSignInClick,
                    onManageAccountClick = onManageAccountClick,
                    onLogout = onLogout,
                )

                SectionTitle(stringResource(R.string.feature_settings_impl_section_appearance))
                DarkModeChooser(
                    darkThemeConfig = uiState.settings.darkThemeConfig,
                    onChangeDarkThemeConfig = onChangeDarkThemeConfig,
                )
                if (supportDynamicColor) {
                    DynamicColorChooser(
                        useDynamicColor = uiState.settings.useDynamicColor,
                        onChangeDynamicColorPreference = onChangeDynamicColorPreference,
                    )
                }
                ThemeColorChooser(
                    settings = uiState.settings,
                    onChangeThemeSeed = onChangeThemeSeed,
                    onChangeThemeStyle = onChangeThemeStyle,
                    onChangeThemeContrast = onChangeThemeContrast,
                    onChangeMeshGradient = onChangeMeshGradient,
                )

                SectionTitle(stringResource(R.string.feature_settings_impl_section_language))
                LanguageChooser(
                    currentLanguageTag = currentLanguageTag,
                    onChangeLanguage = onChangeLanguage,
                )

                SectionTitle(stringResource(R.string.feature_settings_impl_section_reader))
                ReaderModeChooser(
                    readerMode = uiState.settings.readerMode,
                    onChangeReaderMode = onChangeReaderMode,
                )
                ArabicFontScaleChooser(
                    scale = uiState.settings.arabicFontScale,
                    onChangeArabicFontScale = onChangeArabicFontScale,
                )
                ReaderDisplayChooser(
                    settings = uiState.settings,
                    onChangeShowTransliteration = onChangeShowTransliteration,
                    onChangeShowTranslation = onChangeShowTranslation,
                    onChangeArabicLineSpacing = onChangeArabicLineSpacing,
                    onChangeTranslationScale = onChangeTranslationScale,
                    onChangeKeepScreenOn = onChangeKeepScreenOn,
                )
                if (uiState.translationSources.isNotEmpty()) {
                    TranslationSourceChooser(
                        sources = uiState.translationSources,
                        selectedId = uiState.settings.translationSourceId,
                        onChangeTranslationSource = onChangeTranslationSource,
                    )
                }
                if (uiState.recitations.isNotEmpty()) {
                    RecitationChooser(
                        recitations = uiState.recitations,
                        selectedId = uiState.settings.recitationId,
                        onChangeRecitation = onChangeRecitation,
                    )
                }

                SectionTitle(stringResource(R.string.feature_settings_impl_section_offline))
                QuranDownloadSection(
                    state = uiState.quranDownloadState,
                    onStartDownload = onStartQuranDownload,
                    onCancelDownload = onCancelQuranDownload,
                )

                SectionTitle(stringResource(R.string.feature_settings_impl_section_about))
                Text(
                    text = stringResource(R.string.feature_settings_impl_version, appVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun AccountSection(
    authState: AuthState,
    onSignInClick: () -> Unit,
    onManageAccountClick: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    SectionTitle(stringResource(R.string.feature_settings_impl_section_account))
    when (authState) {
        is AuthState.Authenticated -> {
            Text(
                text = stringResource(R.string.feature_settings_impl_signed_in_as),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = authState.session.displayName ?: authState.session.email,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.size(12.dp))
            SurauButton(
                onClick = onManageAccountClick,
                modifier = Modifier.testTag("settings:manageAccount"),
            ) {
                Text(stringResource(R.string.feature_settings_impl_manage_account))
            }
            Spacer(modifier = Modifier.size(8.dp))
            SurauOutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.testTag("settings:logout"),
            ) {
                Text(stringResource(R.string.feature_settings_impl_logout))
            }
        }

        else -> {
            Text(
                text = stringResource(R.string.feature_settings_impl_guest),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.feature_settings_impl_guest_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(12.dp))
            SurauButton(
                onClick = onSignInClick,
                modifier = Modifier.testTag("settings:signIn"),
            ) {
                Text(stringResource(R.string.feature_settings_impl_sign_in))
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.feature_settings_impl_logout_confirm_title)) },
            text = { Text(stringResource(R.string.feature_settings_impl_logout_confirm_body)) },
            confirmButton = {
                SurauTextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    modifier = Modifier.testTag("settings:logoutConfirm"),
                ) {
                    Text(stringResource(R.string.feature_settings_impl_logout))
                }
            },
            dismissButton = {
                SurauTextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.feature_settings_impl_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun DarkModeChooser(
    darkThemeConfig: DarkThemeConfig,
    onChangeDarkThemeConfig: (DarkThemeConfig) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_dark_mode_preference),
        style = MaterialTheme.typography.bodyLarge,
    )
    Column(Modifier.selectableGroup()) {
        SettingsChooserRow(
            text = stringResource(R.string.feature_settings_impl_dark_mode_config_system_default),
            selected = darkThemeConfig == DarkThemeConfig.FOLLOW_SYSTEM,
            onClick = { onChangeDarkThemeConfig(DarkThemeConfig.FOLLOW_SYSTEM) },
        )
        SettingsChooserRow(
            text = stringResource(R.string.feature_settings_impl_dark_mode_config_light),
            selected = darkThemeConfig == DarkThemeConfig.LIGHT,
            onClick = { onChangeDarkThemeConfig(DarkThemeConfig.LIGHT) },
        )
        SettingsChooserRow(
            text = stringResource(R.string.feature_settings_impl_dark_mode_config_dark),
            selected = darkThemeConfig == DarkThemeConfig.DARK,
            onClick = { onChangeDarkThemeConfig(DarkThemeConfig.DARK) },
        )
    }
}

@Composable
private fun DynamicColorChooser(
    useDynamicColor: Boolean,
    onChangeDynamicColorPreference: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_dynamic_color_preference),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Column(Modifier.selectableGroup()) {
        SettingsChooserRow(
            text = stringResource(R.string.feature_settings_impl_dynamic_color_yes),
            selected = useDynamicColor,
            onClick = { onChangeDynamicColorPreference(true) },
        )
        SettingsChooserRow(
            text = stringResource(R.string.feature_settings_impl_dynamic_color_no),
            selected = !useDynamicColor,
            onClick = { onChangeDynamicColorPreference(false) },
        )
    }
}

private data class ThemePreset(
    val name: String,
    /** Packed ARGB seed, or 0 for the default (Zamrud) scheme. */
    val argb: Long,
    /** The color shown on the swatch chip. */
    val swatch: Color,
)

// Curated, spiritually-named presets. Zamrud (argb 0) keeps the default hardcoded HeroUI scheme; the
// rest drive the generated scheme. Display names are brand proper-nouns, identical across locales.
private val themePresets = listOf(
    ThemePreset("Zamrud", 0L, Color(0xFF006D4AL)),
    ThemePreset("Mihrab", 0xFF0E7C86L, Color(0xFF0E7C86L)),
    ThemePreset("Lazuardi", 0xFF2D5BD0L, Color(0xFF2D5BD0L)),
    ThemePreset("Senja", 0xFFC2562EL, Color(0xFFC2562EL)),
    ThemePreset("Kurma", 0xFF8A5A2BL, Color(0xFF8A5A2BL)),
    ThemePreset("Nila", 0xFF3B3F8FL, Color(0xFF3B3F8FL)),
    ThemePreset("Salju", 0xFF5B6B61L, Color(0xFF5B6B61L)),
    ThemePreset("Monokrom", 0xFF3A3A3AL, Color(0xFF3A3A3AL)),
)

@Composable
private fun ThemeColorChooser(
    settings: UserEditableSettings,
    onChangeThemeSeed: (Long) -> Unit,
    onChangeThemeStyle: (ThemeStyle) -> Unit,
    onChangeThemeContrast: (ThemeContrast) -> Unit,
    onChangeMeshGradient: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_theme_color),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp),
    )

    // The preview uses the currently-applied theme, which re-skins instantly when a preset is tapped.
    ThemePreviewCard()

    // Choosing a preset is mutually exclusive with wallpaper dynamic color.
    val activeSeed = if (settings.useDynamicColor) -1L else settings.seedColorArgb
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        themePresets.forEach { preset ->
            ThemeSwatch(
                preset = preset,
                selected = preset.argb == activeSeed,
                onClick = { onChangeThemeSeed(preset.argb) },
            )
        }
    }

    var showPicker by rememberSaveable { mutableStateOf(false) }
    SurauTextButton(
        onClick = { showPicker = true },
        modifier = Modifier
            .padding(top = 4.dp)
            .testTag("settings:themeCustom"),
    ) {
        Text(stringResource(R.string.feature_settings_impl_theme_custom))
    }
    if (showPicker) {
        val initial = settings.seedColorArgb.takeIf { it != 0L }?.let { Color(it) }
            ?: Color(0xFF0E7C86L)
        ThemeColorPickerSheet(
            initial = initial,
            style = settings.themeStyle.toSeedPaletteStyle(),
            contrast = settings.themeContrast.toContrastLevel(),
            onApply = {
                onChangeThemeSeed(it.toThemeArgb())
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    val customActive = !settings.useDynamicColor && settings.seedColorArgb != 0L
    if (customActive) {
        val styles = listOf(
            ThemeStyle.TONAL_SPOT to
                stringResource(R.string.feature_settings_impl_theme_style_standard),
            ThemeStyle.VIBRANT to
                stringResource(R.string.feature_settings_impl_theme_style_vibrant),
            ThemeStyle.EXPRESSIVE to
                stringResource(R.string.feature_settings_impl_theme_style_expressive),
            ThemeStyle.NEUTRAL to
                stringResource(R.string.feature_settings_impl_theme_style_neutral),
        )
        Text(
            text = stringResource(R.string.feature_settings_impl_theme_style),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.size(8.dp))
        SurauButtonGroup(
            options = styles.map { it.second },
            selectedIndex = styles.indexOfFirst { it.first == settings.themeStyle }
                .coerceAtLeast(0),
            onOptionSelected = { index -> onChangeThemeStyle(styles[index].first) },
        )

        val contrasts = listOf(
            ThemeContrast.STANDARD to
                stringResource(R.string.feature_settings_impl_theme_contrast_standard),
            ThemeContrast.MEDIUM to
                stringResource(R.string.feature_settings_impl_theme_contrast_medium),
            ThemeContrast.HIGH to
                stringResource(R.string.feature_settings_impl_theme_contrast_high),
        )
        Text(
            text = stringResource(R.string.feature_settings_impl_theme_contrast),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.size(8.dp))
        SurauButtonGroup(
            options = contrasts.map { it.second },
            selectedIndex = contrasts.indexOfFirst { it.first == settings.themeContrast }
                .coerceAtLeast(0),
            onOptionSelected = { index -> onChangeThemeContrast(contrasts[index].first) },
        )

        SurauTextButton(
            onClick = { onChangeThemeSeed(0L) },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.feature_settings_impl_theme_reset))
        }
    }

    SettingsSwitchRow(
        text = stringResource(R.string.feature_settings_impl_theme_mesh),
        checked = settings.useMeshGradient,
        onCheckedChange = onChangeMeshGradient,
    )
    Text(
        text = stringResource(R.string.feature_settings_impl_theme_mesh_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ThemePreviewCard() {
    SurauSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SurauButton(onClick = {}) {
                    Text(stringResource(R.string.feature_settings_impl_theme_preview_action))
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feature_settings_impl_theme_preview_chip),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            AyahText(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ThemeSwatch(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(preset.swatch)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    },
                ),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ReaderModeChooser(
    readerMode: ReaderMode,
    onChangeReaderMode: (ReaderMode) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_reader_mode),
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.size(8.dp))
    val modes = listOf(
        ReaderMode.ARABIC_TRANSLATION to
            stringResource(R.string.feature_settings_impl_reader_mode_arabic_translation),
        ReaderMode.TRANSLATION_ONLY to
            stringResource(R.string.feature_settings_impl_reader_mode_translation),
        ReaderMode.ARABIC_ONLY to
            stringResource(R.string.feature_settings_impl_reader_mode_arabic),
    )
    SurauButtonGroup(
        options = modes.map { it.second },
        selectedIndex = modes.indexOfFirst { it.first == readerMode }.coerceAtLeast(0),
        onOptionSelected = { index -> onChangeReaderMode(modes[index].first) },
    )
}

@Composable
private fun ArabicFontScaleChooser(
    scale: Float,
    onChangeArabicFontScale: (Float) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_arabic_font_scale),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp),
    )
    Slider(
        value = scale,
        onValueChange = onChangeArabicFontScale,
        valueRange = 0.8f..1.8f,
        steps = 4,
    )
    AyahText(
        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
        fontScale = scale,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ReaderDisplayChooser(
    settings: UserEditableSettings,
    onChangeShowTransliteration: (Boolean) -> Unit,
    onChangeShowTranslation: (Boolean) -> Unit,
    onChangeArabicLineSpacing: (Float) -> Unit,
    onChangeTranslationScale: (Float) -> Unit,
    onChangeKeepScreenOn: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_reader_line_spacing),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp),
    )
    Slider(
        value = settings.arabicLineSpacing,
        onValueChange = onChangeArabicLineSpacing,
        valueRange = 1f..2f,
        steps = 3,
    )

    Text(
        text = stringResource(R.string.feature_settings_impl_reader_translation_size),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp),
    )
    Slider(
        value = settings.translationScale,
        onValueChange = onChangeTranslationScale,
        valueRange = 0.8f..1.6f,
        steps = 3,
    )

    SettingsSwitchRow(
        text = stringResource(R.string.feature_settings_impl_reader_transliteration),
        checked = settings.showTransliteration,
        onCheckedChange = onChangeShowTransliteration,
    )
    SettingsSwitchRow(
        text = stringResource(R.string.feature_settings_impl_reader_show_translation),
        checked = settings.showTranslation,
        onCheckedChange = onChangeShowTranslation,
    )
    SettingsSwitchRow(
        text = stringResource(R.string.feature_settings_impl_reader_keep_screen_on),
        checked = settings.keepScreenOn,
        onCheckedChange = onChangeKeepScreenOn,
    )
}

@Composable
private fun SettingsSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        SurauSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TranslationSourceChooser(
    sources: List<TranslationSource>,
    selectedId: String?,
    onChangeTranslationSource: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_translation_source),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp),
    )
    val effectiveSelected = selectedId ?: sources.firstOrNull { it.isDefault }?.id
    Column(Modifier.selectableGroup()) {
        sources.forEach { source ->
            ListItem(
                headlineContent = { Text(source.name) },
                supportingContent = source.translator?.let { { Text(it) } },
                leadingContent = {
                    RadioButton(
                        selected = source.id == effectiveSelected,
                        onClick = null,
                    )
                },
                modifier = Modifier.selectable(
                    selected = source.id == effectiveSelected,
                    role = Role.RadioButton,
                    onClick = { onChangeTranslationSource(source.id) },
                ),
            )
        }
    }
}

@Composable
private fun RecitationChooser(
    recitations: List<Recitation>,
    selectedId: String?,
    onChangeRecitation: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_qari),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp),
    )
    val effectiveSelected = selectedId ?: recitations.firstOrNull { it.isDefault }?.id
    Column(Modifier.selectableGroup()) {
        recitations.forEach { recitation ->
            ListItem(
                headlineContent = { Text(recitation.displayName) },
                supportingContent = recitation.style?.let { { Text(it) } },
                leadingContent = {
                    RadioButton(
                        selected = recitation.id == effectiveSelected,
                        onClick = null,
                    )
                },
                modifier = Modifier.selectable(
                    selected = recitation.id == effectiveSelected,
                    role = Role.RadioButton,
                    onClick = { onChangeRecitation(recitation.id) },
                ),
            )
        }
    }
}

@Composable
private fun LanguageChooser(
    currentLanguageTag: String,
    onChangeLanguage: (String) -> Unit,
) {
    // Endonyms (Bahasa Indonesia / English) stay in their own language so users can always find
    // their language; only the "follow system" label is localised.
    val options = listOf(
        "" to stringResource(R.string.feature_settings_impl_language_system),
        "id" to stringResource(R.string.feature_settings_impl_language_id),
        "en" to stringResource(R.string.feature_settings_impl_language_en),
    )
    Column(Modifier.selectableGroup()) {
        options.forEach { (tag, label) ->
            SettingsChooserRow(
                text = label,
                selected = currentLanguageTag.substringBefore('-') == tag,
                onClick = { onChangeLanguage(tag) },
            )
        }
    }
}

@Composable
private fun QuranDownloadSection(
    state: QuranDownloadState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_download_quran_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.size(12.dp))
    when (state) {
        is QuranDownloadState.Running -> {
            LinearProgressIndicator(
                progress = { state.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings:downloadProgress"),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.feature_settings_impl_download_progress,
                        state.percent,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                SurauOutlinedButton(
                    onClick = onCancelDownload,
                    modifier = Modifier.testTag("settings:downloadCancel"),
                ) {
                    Text(stringResource(R.string.feature_settings_impl_download_cancel))
                }
            }
        }

        QuranDownloadState.Completed -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = SurauIcons.DownloadDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.feature_settings_impl_download_done),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                SurauTextButton(onClick = onStartDownload) {
                    Text(stringResource(R.string.feature_settings_impl_download_redownload))
                }
            }
        }

        QuranDownloadState.Failed -> {
            Text(
                text = stringResource(R.string.feature_settings_impl_download_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.size(8.dp))
            SurauButton(
                onClick = onStartDownload,
                modifier = Modifier.testTag("settings:downloadRetry"),
            ) {
                Text(stringResource(R.string.feature_settings_impl_download_retry))
            }
        }

        QuranDownloadState.Idle -> {
            SurauButton(
                onClick = onStartDownload,
                modifier = Modifier.testTag("settings:downloadQuran"),
            ) {
                Icon(imageVector = SurauIcons.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.feature_settings_impl_download_quran))
            }
        }
    }
}

@Composable
private fun SettingsChooserRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
