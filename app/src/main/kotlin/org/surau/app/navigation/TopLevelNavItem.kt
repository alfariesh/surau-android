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

package org.surau.app.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import org.surau.app.R
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.feature.quran.api.navigation.QuranHomeNavKey

/**
 * Type for the top level navigation items in the application. Contains UI information about the
 * current route that is used in the top app bar and common navigation UI.
 *
 * @param selectedIcon The icon to be displayed in the navigation UI when this destination is
 * selected.
 * @param unselectedIcon The icon to be displayed in the navigation UI when this destination is
 * not selected.
 * @param iconTextId Text that to be displayed in the navigation UI.
 * @param titleTextId Text that is displayed on the top app bar.
 */
data class TopLevelNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
)

val HOME = TopLevelNavItem(
    selectedIcon = SurauIcons.Home,
    unselectedIcon = SurauIcons.HomeBorder,
    iconTextId = R.string.tab_home,
    titleTextId = R.string.tab_home,
)

val QURAN = TopLevelNavItem(
    selectedIcon = SurauIcons.MenuBook,
    unselectedIcon = SurauIcons.MenuBookBorder,
    iconTextId = R.string.tab_quran,
    titleTextId = R.string.tab_quran,
)

val HADITH = TopLevelNavItem(
    selectedIcon = SurauIcons.AutoStories,
    unselectedIcon = SurauIcons.AutoStoriesBorder,
    iconTextId = R.string.tab_hadith,
    titleTextId = R.string.tab_hadith,
)

val KITABS = TopLevelNavItem(
    selectedIcon = SurauIcons.LibraryBooks,
    unselectedIcon = SurauIcons.LibraryBooksBorder,
    iconTextId = R.string.tab_kitabs,
    titleTextId = R.string.tab_kitabs,
)

val PROFILE = TopLevelNavItem(
    selectedIcon = SurauIcons.Person,
    unselectedIcon = SurauIcons.PersonBorder,
    iconTextId = R.string.tab_profile,
    titleTextId = R.string.tab_profile,
)

/**
 * The bottom navigation tabs, in display order. Insertion order is preserved by [LinkedHashMap], so
 * iterating this map renders the tabs left-to-right as declared here.
 */
val TOP_LEVEL_NAV_ITEMS: Map<NavKey, TopLevelNavItem> = linkedMapOf(
    HomeNavKey to HOME,
    QuranHomeNavKey to QURAN,
    HadithNavKey to HADITH,
    KitabsNavKey to KITABS,
    ProfileNavKey to PROFILE,
)
