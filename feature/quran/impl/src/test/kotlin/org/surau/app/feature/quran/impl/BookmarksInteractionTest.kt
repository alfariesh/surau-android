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
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.model.data.quran.AyahKey
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class BookmarksInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun editor_typeNote_addTag_save_emitsValues() {
        var saved: Pair<String?, List<String>>? = null
        composeTestRule.setContent {
            SurauTheme {
                BookmarkEditorContent(
                    item = listItem(note = null, tags = emptyList()),
                    onSave = { note, tags -> saved = note to tags },
                )
            }
        }

        composeTestRule.onNodeWithTag("bookmarks:editor:note").performTextInput("renungan")
        composeTestRule.onNodeWithTag("bookmarks:editor:tagInput").performTextInput("tafsir")
        composeTestRule.onNodeWithText("Tambahkan").performClick()
        composeTestRule.onNodeWithTag("bookmarks:editor:save").performScrollTo().performClick()

        assertEquals("renungan", saved?.first)
        assertEquals(listOf("tafsir"), saved?.second)
    }

    @Test
    fun editor_clearedNote_savesNull() {
        var saved: Pair<String?, List<String>>? = null
        composeTestRule.setContent {
            SurauTheme {
                BookmarkEditorContent(
                    item = listItem(note = null, tags = listOf("tafsir")),
                    onSave = { note, tags -> saved = note to tags },
                )
            }
        }

        // Note left blank -> repository receives null (which clears it via PATCH).
        composeTestRule.onNodeWithTag("bookmarks:editor:save").performScrollTo().performClick()

        assertEquals(null, saved?.first)
        assertEquals(listOf("tafsir"), saved?.second)
    }

    @Test
    fun bookmarkRow_click_opensReader() {
        var opened: Pair<Int, Int>? = null
        composeTestRule.setContent { SurauTheme { ScreenWith(onOpenInReader = { s, a -> opened = s to a }) } }

        composeTestRule.onNodeWithTag("bookmarks:item:1:1").performClick()

        assertEquals(1 to 1, opened)
    }

    @Test
    fun tagChip_click_selectsTag() {
        var selected: String? = null
        composeTestRule.setContent { SurauTheme { ScreenWith(onSelectTag = { selected = it }) } }

        composeTestRule.onNodeWithTag("bookmarks:filter:favorit").performClick()

        assertEquals("favorit", selected)
    }

    @Test
    fun editor_pickCollection_save_emitsPresetTag() {
        var saved: Pair<String?, List<String>>? = null
        composeTestRule.setContent {
            SurauTheme {
                BookmarkEditorContent(
                    item = listItem(note = null, tags = emptyList()),
                    onSave = { note, tags -> saved = note to tags },
                )
            }
        }

        // Tapping a preset collection chip adds it as a tag without typing.
        composeTestRule.onNodeWithText("Hafalan").performClick()
        composeTestRule.onNodeWithTag("bookmarks:editor:save").performScrollTo().performClick()

        assertEquals(listOf("Hafalan"), saved?.second)
    }

    @Test
    fun delete_confirm_emitsDelete() {
        var deleted: AyahKey? = null
        composeTestRule.setContent { SurauTheme { ScreenWith(onDeleteBookmark = { deleted = it }) } }

        composeTestRule.onNodeWithTag("bookmarks:delete:1:1").performClick()
        composeTestRule.onNodeWithTag("bookmarks:deleteConfirm").performClick()

        assertEquals(AyahKey("1:1"), deleted)
    }

    @Composable
    private fun ScreenWith(
        onOpenInReader: (Int, Int) -> Unit = { _, _ -> },
        onSelectTag: (String?) -> Unit = {},
        onDeleteBookmark: (AyahKey) -> Unit = {},
    ) {
        QuranBookmarksScreen(
            uiState = BookmarksUiState.Success(
                sections = listOf(
                    BookmarkSurahSection(
                        surahId = 1,
                        surahName = "Al-Fatihah",
                        items = listOf(listItem(note = "catatan", tags = listOf("favorit"))),
                    ),
                ),
                allTags = listOf("favorit"),
                activeTag = null,
            ),
            onBackClick = {},
            onOpenInReader = onOpenInReader,
            onSelectTag = onSelectTag,
            onSaveBookmark = { _, _, _ -> },
            onDeleteBookmark = onDeleteBookmark,
        )
    }

    private fun listItem(note: String?, tags: List<String>) = BookmarkListItem(
        ayahKey = AyahKey("1:1"),
        surahId = 1,
        ayahNumber = 1,
        surahName = "Al-Fatihah",
        arabicText = null,
        note = note,
        tags = tags,
    )
}
