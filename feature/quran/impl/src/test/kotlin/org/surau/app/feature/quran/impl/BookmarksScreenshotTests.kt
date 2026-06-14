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

package org.surau.app.feature.quran.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.testing.util.captureMultiTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class BookmarksScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bookmarksList_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "QuranBookmarksList",
            shouldCompareDynamicColor = false,
        ) {
            QuranBookmarksScreen(
                uiState = BookmarksUiState.Success(
                    sections = listOf(
                        BookmarkSurahSection(
                            surahId = 1,
                            surahName = "Al-Fatihah",
                            items = listOf(
                                BookmarkListItem(
                                    ayahKey = AyahKey("1:1"),
                                    surahId = 1,
                                    ayahNumber = 1,
                                    surahName = "Al-Fatihah",
                                    note = "Pembuka setiap surah",
                                    tags = listOf("tafsir", "favorit"),
                                ),
                            ),
                        ),
                        BookmarkSurahSection(
                            surahId = 73,
                            surahName = "Al-Muzzammil",
                            items = listOf(
                                BookmarkListItem(
                                    ayahKey = AyahKey("73:4"),
                                    surahId = 73,
                                    ayahNumber = 4,
                                    surahName = "Al-Muzzammil",
                                    note = null,
                                    tags = listOf("tartil"),
                                ),
                            ),
                        ),
                    ),
                    allTags = listOf("favorit", "tafsir", "tartil"),
                    activeTag = null,
                ),
                onBackClick = {},
                onOpenInReader = { _, _ -> },
                onSelectTag = {},
                onSaveBookmark = { _, _, _ -> },
                onDeleteBookmark = {},
            )
        }
    }

    @Test
    fun bookmarksEmpty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "QuranBookmarksEmpty",
            shouldCompareDynamicColor = false,
        ) {
            QuranBookmarksScreen(
                uiState = BookmarksUiState.Empty,
                onBackClick = {},
                onOpenInReader = { _, _ -> },
                onSelectTag = {},
                onSaveBookmark = { _, _, _ -> },
                onDeleteBookmark = {},
            )
        }
    }
}
