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

package org.surau.app.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.ThemePalette
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurauPreferencesDataSourceTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var subject: SurauPreferencesDataSource

    @Before
    fun setup() {
        subject = SurauPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance()))
    }

    @Test
    fun shouldUseDynamicColorFalseByDefault() = testScope.runTest {
        assertFalse(subject.userData.first().useDynamicColor)
    }

    @Test
    fun userShouldUseDynamicColorIsTrueWhenSet() = testScope.runTest {
        subject.setDynamicColorPreference(true)
        assertTrue(subject.userData.first().useDynamicColor)
    }

    @Test
    fun darkThemeConfigFollowsSystemByDefault() = testScope.runTest {
        assertEquals(DarkThemeConfig.FOLLOW_SYSTEM, subject.userData.first().darkThemeConfig)
    }

    @Test
    fun darkThemeConfigIsDarkWhenSet() = testScope.runTest {
        subject.setDarkThemeConfig(DarkThemeConfig.DARK)
        assertEquals(DarkThemeConfig.DARK, subject.userData.first().darkThemeConfig)
    }

    @Test
    fun themePaletteDefaultsToSurauBase() = testScope.runTest {
        assertEquals(ThemePalette.SURAU_BASE, subject.userData.first().themePalette)
    }

    @Test
    fun themePalette_mapsExistingAndNewProtoValues() = testScope.runTest {
        val cases = listOf(
            ThemePaletteProto.THEME_PALETTE_DEFAULT to ThemePalette.DEFAULT,
            ThemePaletteProto.THEME_PALETTE_MOUVE to ThemePalette.MOUVE,
            ThemePaletteProto.THEME_PALETTE_SKY to ThemePalette.SKY,
            ThemePaletteProto.THEME_PALETTE_SURAU_BASE to ThemePalette.SURAU_BASE,
            ThemePaletteProto.THEME_PALETTE_MINT to ThemePalette.MINT,
            ThemePaletteProto.THEME_PALETTE_DISCORD to ThemePalette.DISCORD,
            ThemePaletteProto.THEME_PALETTE_UBER to ThemePalette.UBER,
            ThemePaletteProto.THEME_PALETTE_AIRBNB to ThemePalette.AIRBNB,
        )

        cases.forEach { (proto, palette) ->
            val seededSubject = SurauPreferencesDataSource(
                InMemoryDataStore(
                    UserPreferences.getDefaultInstance().copy {
                        themePalette = proto
                    },
                ),
            )
            assertEquals(palette, seededSubject.userData.first().themePalette)
        }
    }

    @Test
    fun themePalette_roundTripsThroughSetter() = testScope.runTest {
        val cases = listOf(
            ThemePalette.SURAU_BASE to ThemePaletteProto.THEME_PALETTE_SURAU_BASE,
            ThemePalette.DEFAULT to ThemePaletteProto.THEME_PALETTE_DEFAULT,
            ThemePalette.MOUVE to ThemePaletteProto.THEME_PALETTE_MOUVE,
            ThemePalette.SKY to ThemePaletteProto.THEME_PALETTE_SKY,
            ThemePalette.MINT to ThemePaletteProto.THEME_PALETTE_MINT,
            ThemePalette.DISCORD to ThemePaletteProto.THEME_PALETTE_DISCORD,
            ThemePalette.UBER to ThemePaletteProto.THEME_PALETTE_UBER,
            ThemePalette.AIRBNB to ThemePaletteProto.THEME_PALETTE_AIRBNB,
        )

        cases.forEach { (palette, proto) ->
            val store = InMemoryDataStore(UserPreferences.getDefaultInstance())
            val seededSubject = SurauPreferencesDataSource(store)

            seededSubject.setThemePalette(palette)

            assertEquals(palette, seededSubject.userData.first().themePalette)
            assertEquals(proto, store.data.first().themePalette)
        }
    }

    @Test
    fun advancedReaderPrefs_haveCorrectDefaults() = testScope.runTest {
        val data = subject.userData.first()
        // Inverse-bool defaults: transliteration off, translation + keep-screen-on on.
        assertFalse(data.readerShowTransliteration)
        assertTrue(data.readerShowTranslation)
        assertTrue(data.readerKeepScreenOn)
        assertEquals(1f, data.readerArabicLineSpacing)
        assertEquals(1f, data.readerTranslationScale)
    }

    @Test
    fun advancedReaderPrefs_roundTripThroughSetters() = testScope.runTest {
        subject.setReaderShowTransliteration(true)
        subject.setReaderShowTranslation(false)
        subject.setReaderKeepScreenOn(false)
        subject.setReaderArabicLineSpacing(1.5f)
        subject.setReaderTranslationScale(1.2f)

        val data = subject.userData.first()
        assertTrue(data.readerShowTransliteration)
        assertFalse(data.readerShowTranslation)
        assertFalse(data.readerKeepScreenOn)
        assertEquals(1.5f, data.readerArabicLineSpacing)
        assertEquals(1.2f, data.readerTranslationScale)
    }
}
