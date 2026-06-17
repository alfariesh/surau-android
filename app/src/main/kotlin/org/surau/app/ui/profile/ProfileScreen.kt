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

package org.surau.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LinearProgressIndicator
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
import org.surau.app.R
import org.surau.app.core.data.util.QuranDownloadState
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauButtonGroup
import org.surau.app.core.designsystem.component.SurauCell
import org.surau.app.core.designsystem.component.SurauListGroup
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauOutlinedButton
import org.surau.app.core.designsystem.component.SurauSelect
import org.surau.app.core.designsystem.component.SurauSurface
import org.surau.app.core.designsystem.component.SurauSurfaceVariant
import org.surau.app.core.designsystem.component.SurauSwitch
import org.surau.app.core.designsystem.component.SurauTextButton
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.designsystem.theme.supportsDynamicTheming
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemePalette
import org.surau.app.core.model.data.ThemeStyle
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.core.ui.TrackScreenViewEvent
import org.surau.app.ui.profile.ProfileUiState.Loading
import org.surau.app.ui.profile.ProfileUiState.Success

/**
 * The Profile hub — the single home for the account header, account actions, and every
 * user-editable setting, each grouped into its own card section.
 */
@Composable
fun ProfileScreen(
    appVersionName: String,
    onManageAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    currentLanguageTag: String = "",
    onChangeLanguage: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        appVersionName = appVersionName,
        currentLanguageTag = currentLanguageTag,
        onChangeLanguage = onChangeLanguage,
        onManageAccount = onManageAccount,
        onSignIn = onSignIn,
        onLogout = viewModel::logout,
        onChangeDynamicColorPreference = viewModel::updateDynamicColorPreference,
        onChangeDarkThemeConfig = viewModel::updateDarkThemeConfig,
        onChangeThemeSeed = viewModel::updateThemeSeed,
        onChangeThemeStyle = viewModel::updateThemeStyle,
        onChangeThemeContrast = viewModel::updateThemeContrast,
        onChangeThemePalette = viewModel::updateThemePalette,
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
internal fun ProfileScreen(
    uiState: ProfileUiState,
    appVersionName: String,
    onManageAccount: () -> Unit,
    onSignIn: () -> Unit,
    onLogout: () -> Unit,
    onChangeDarkThemeConfig: (DarkThemeConfig) -> Unit,
    onChangeReaderMode: (ReaderMode) -> Unit,
    onChangeTranslationSource: (String) -> Unit,
    onChangeRecitation: (String) -> Unit,
    onChangeArabicFontScale: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onChangeDynamicColorPreference: (Boolean) -> Unit = {},
    onChangeThemeSeed: (Long) -> Unit = {},
    onChangeThemeStyle: (ThemeStyle) -> Unit = {},
    onChangeThemeContrast: (ThemeContrast) -> Unit = {},
    onChangeThemePalette: (ThemePalette) -> Unit = {},
    onChangeMeshGradient: (Boolean) -> Unit = {},
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
    TrackScreenViewEvent(screenName = "Profile")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        when (uiState) {
            Loading -> {
                AccountHeaderCard(
                    name = stringResource(R.string.profile_guest),
                    subtitle = stringResource(R.string.profile_guest_body),
                )
                Spacer(Modifier.size(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SurauLoadingWheel(
                        contentDesc = stringResource(R.string.feature_settings_impl_loading),
                    )
                }
            }

            is Success -> {
                val settings = uiState.settings
                val signedIn = uiState.authState is AuthState.Authenticated
                val session = (uiState.authState as? AuthState.Authenticated)?.session

                AccountHeaderCard(
                    name = session?.displayName?.takeIf { it.isNotBlank() }
                        ?: session?.email
                        ?: stringResource(R.string.profile_guest),
                    subtitle = if (signedIn) {
                        session?.email.orEmpty()
                    } else {
                        stringResource(R.string.profile_guest_body)
                    },
                )

                if (!signedIn) {
                    Spacer(Modifier.size(16.dp))
                    SurauButton(
                        onClick = onSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile:signIn"),
                        text = { Text(stringResource(R.string.feature_settings_impl_sign_in)) },
                    )
                }

                if (signedIn) {
                    Spacer(Modifier.size(16.dp))
                    AccountActionsCard(
                        onManageAccount = onManageAccount,
                        onLogout = onLogout,
                    )
                }

                ProfileSection(stringResource(R.string.feature_settings_impl_section_appearance)) {
                    DarkModeChooser(
                        darkThemeConfig = settings.darkThemeConfig,
                        onChangeDarkThemeConfig = onChangeDarkThemeConfig,
                    )
                    if (supportDynamicColor) {
                        DynamicColorChooser(
                            useDynamicColor = settings.useDynamicColor,
                            onChangeDynamicColorPreference = onChangeDynamicColorPreference,
                        )
                    }
                    ThemeColorChooser(
                        settings = settings,
                        onChangeThemeSeed = onChangeThemeSeed,
                        onChangeThemeStyle = onChangeThemeStyle,
                        onChangeThemeContrast = onChangeThemeContrast,
                        onChangeThemePalette = onChangeThemePalette,
                        onChangeMeshGradient = onChangeMeshGradient,
                    )
                }

                ProfileSection(stringResource(R.string.feature_settings_impl_section_language)) {
                    LanguageChooser(
                        currentLanguageTag = currentLanguageTag,
                        onChangeLanguage = onChangeLanguage,
                    )
                }

                ProfileSection(stringResource(R.string.feature_settings_impl_section_reader)) {
                    ReaderModeChooser(
                        readerMode = settings.readerMode,
                        onChangeReaderMode = onChangeReaderMode,
                    )
                    ArabicFontScaleChooser(
                        scale = settings.arabicFontScale,
                        onChangeArabicFontScale = onChangeArabicFontScale,
                    )
                    ReaderDisplayChooser(
                        settings = settings,
                        onChangeShowTransliteration = onChangeShowTransliteration,
                        onChangeShowTranslation = onChangeShowTranslation,
                        onChangeArabicLineSpacing = onChangeArabicLineSpacing,
                        onChangeTranslationScale = onChangeTranslationScale,
                        onChangeKeepScreenOn = onChangeKeepScreenOn,
                    )
                }

                if (uiState.recitations.isNotEmpty() || uiState.translationSources.isNotEmpty()) {
                    ProfileSection(stringResource(R.string.feature_settings_impl_section_recitation)) {
                        if (uiState.recitations.isNotEmpty()) {
                            RecitationSelect(
                                recitations = uiState.recitations,
                                selectedId = settings.recitationId,
                                onChangeRecitation = onChangeRecitation,
                            )
                        }
                        if (uiState.translationSources.isNotEmpty()) {
                            if (uiState.recitations.isNotEmpty()) Spacer(Modifier.size(16.dp))
                            TranslationSourceSelect(
                                sources = uiState.translationSources,
                                selectedId = settings.translationSourceId,
                                onChangeTranslationSource = onChangeTranslationSource,
                            )
                        }
                    }
                }

                ProfileSection(stringResource(R.string.feature_settings_impl_section_offline)) {
                    QuranDownloadSection(
                        state = uiState.quranDownloadState,
                        onStartDownload = onStartQuranDownload,
                        onCancelDownload = onCancelQuranDownload,
                    )
                }

                SectionTitle(stringResource(R.string.feature_settings_impl_section_about))
                Text(
                    text = stringResource(R.string.profile_version, appVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SurauTheme.colors.muted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Spacer(Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun AccountHeaderCard(name: String, subtitle: String) {
    SurauSurface(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(SurauTheme.colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = SurauIcons.Person,
                    contentDescription = null,
                    tint = SurauTheme.colors.accent,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SurauTheme.colors.muted,
                )
            }
        }
    }
}

@Composable
private fun AccountActionsCard(
    onManageAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    SurauListGroup(modifier = Modifier.fillMaxWidth()) {
        SurauCell(
            title = stringResource(R.string.feature_settings_impl_manage_account),
            leading = {
                Icon(
                    imageVector = SurauIcons.Person,
                    contentDescription = null,
                    tint = SurauTheme.colors.muted,
                )
            },
            onClick = onManageAccount,
            modifier = Modifier.testTag("profile:manageAccount"),
        )
        HorizontalDivider(color = SurauTheme.colors.separator)
        SurauCell(
            title = stringResource(R.string.feature_settings_impl_logout),
            leading = {
                Icon(
                    imageVector = SurauIcons.Logout,
                    contentDescription = null,
                    tint = SurauTheme.colors.muted,
                )
            },
            onClick = { showLogoutConfirm = true },
            showChevron = false,
            modifier = Modifier.testTag("profile:logout"),
        )
    }

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
                    modifier = Modifier.testTag("profile:logoutConfirm"),
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

/** A titled section: a [SectionTitle] above a rounded card holding its settings rows. */
@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionTitle(title)
    SurauSurface(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
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
    val palette: ThemePalette,
    val name: String,
    /** The color shown on the swatch chip. */
    val swatch: Color,
)

// Design-token palettes. Display names are token proper-nouns, identical across locales.
private val themePresets = listOf(
    ThemePreset(ThemePalette.SURAU_BASE, "Surau Base", Color(0xFF4F772D)),
    ThemePreset(ThemePalette.DEFAULT, "Default", Color(0xFF0485F7)),
    ThemePreset(ThemePalette.MOUVE, "Mouve", Color(0xFF8451C9)),
    ThemePreset(ThemePalette.SKY, "Sky", Color(0xFF7DD3FC)),
    ThemePreset(ThemePalette.MINT, "Mint", Color(0xFF86EFAC)),
    ThemePreset(ThemePalette.DISCORD, "Discord", Color(0xFF5865F2)),
    ThemePreset(ThemePalette.UBER, "Uber", Color(0xFF000000)),
    ThemePreset(ThemePalette.AIRBNB, "Airbnb", Color(0xFFFF385C)),
)

@Composable
private fun ThemeColorChooser(
    settings: UserEditableSettings,
    onChangeThemeSeed: (Long) -> Unit,
    onChangeThemeStyle: (ThemeStyle) -> Unit,
    onChangeThemeContrast: (ThemeContrast) -> Unit,
    onChangeThemePalette: (ThemePalette) -> Unit,
    onChangeMeshGradient: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_theme_color),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp),
    )

    // The preview uses the currently-applied theme, which re-skins instantly when a preset is tapped.
    ThemePreviewCard()

    // Choosing a named palette is mutually exclusive with custom seed and wallpaper dynamic color.
    val namedPaletteActive = !settings.useDynamicColor && settings.seedColorArgb == 0L
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
                selected = namedPaletteActive && preset.palette == settings.themePalette,
                onClick = { onChangeThemePalette(preset.palette) },
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
            onClick = { onChangeThemePalette(ThemePalette.SURAU_BASE) },
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
        variant = SurauSurfaceVariant.Secondary,
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
private fun RecitationSelect(
    recitations: List<Recitation>,
    selectedId: String?,
    onChangeRecitation: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_qari),
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.size(8.dp))
    val effectiveId = selectedId ?: recitations.firstOrNull { it.isDefault }?.id
    val selected = recitations.firstOrNull { it.id == effectiveId }
    SurauSelect(
        value = selected,
        onValueChange = { onChangeRecitation(it.id) },
        options = recitations,
        label = { it.displayName },
        modifier = Modifier.testTag("profile:reciterSelect"),
    )
}

@Composable
private fun TranslationSourceSelect(
    sources: List<TranslationSource>,
    selectedId: String?,
    onChangeTranslationSource: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.feature_settings_impl_translation_source),
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.size(8.dp))
    val effectiveId = selectedId ?: sources.firstOrNull { it.isDefault }?.id
    val selected = sources.firstOrNull { it.id == effectiveId }
    SurauSelect(
        value = selected,
        onValueChange = { onChangeTranslationSource(it.id) },
        options = sources,
        label = { it.name },
        modifier = Modifier.testTag("profile:translationSelect"),
    )
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
