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
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.surau.app.core.data.test.QuranTestData
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.domain.ReaderContent
import org.surau.app.core.model.data.quran.ReaderMode
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class SurahReaderInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun perAyahBookmarkToggle_click_emitsAyahNumber() {
        var toggled: Int? = null
        composeTestRule.setContent {
            SurauTheme { Reader(onToggleBookmark = { toggled = it }) }
        }

        composeTestRule.onNodeWithTag("reader:menu:1").performClick()
        composeTestRule.onNodeWithTag("reader:menu:save:1").performClick()

        assertEquals(1, toggled)
    }

    @Test
    fun ayahActions_noteAndCollection_savesPresetTag() {
        var saved: Triple<Int, String?, List<String>>? = null
        composeTestRule.setContent {
            SurauTheme {
                Reader(onSaveBookmark = { ayah, note, tags -> saved = Triple(ayah, note, tags) })
            }
        }

        // Open the ayah kebab menu -> "Catatan & koleksi" -> inline editor.
        composeTestRule.onNodeWithTag("reader:menu:1").performClick()
        composeTestRule.onNodeWithTag("reader:menu:note:1").performClick()

        // Pick a preset collection (resolved from resources so it works in any locale), then save.
        val preset = composeTestRule.activity.resources
            .getStringArray(R.array.feature_quran_impl_bookmark_collections)
            .first()
        composeTestRule.onNodeWithText(preset).performClick()
        composeTestRule.onNodeWithTag("bookmarks:editor:save").performScrollTo().performClick()

        assertEquals(1, saved?.first)
        assertEquals(null, saved?.second)
        assertEquals(listOf(preset), saved?.third)
    }

    @Composable
    private fun Reader(
        onToggleBookmark: (Int) -> Unit = {},
        onSaveBookmark: (Int, String?, List<String>) -> Unit = { _, _, _ -> },
    ) {
        SurahReaderScreen(
            uiState = ReaderUiState.Success(
                content = ReaderContent(
                    surah = QuranTestData.surahs.first(),
                    ayahs = QuranTestData.ayahsBySurah.getValue(1),
                    readerMode = ReaderMode.ARABIC_TRANSLATION,
                    arabicFontScale = 1f,
                    translationSourceId = QuranTestData.TEST_TRANSLATION_SOURCE_ID,
                ),
                initialAyahNumber = null,
            ),
            onBackClick = {},
            onAyahVisible = {},
            onReaderModeChange = {},
            onFontScaleChange = {},
            onTranslationSourceChange = {},
            translationSources = QuranTestData.translationSources,
            bookmarksByAyah = emptyMap(),
            onToggleBookmark = onToggleBookmark,
            onSaveBookmark = onSaveBookmark,
        )
    }
}
