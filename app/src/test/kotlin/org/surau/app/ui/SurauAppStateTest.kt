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

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import org.surau.app.core.navigation.NavigationState
import org.surau.app.core.testing.util.TestNetworkMonitor
import org.surau.app.core.testing.util.TestTimeZoneMonitor
import org.surau.app.navigation.PlaceholderHomeNavKey
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Tests [SurauAppState].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class SurauAppStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Create the test dependencies.
    private val networkMonitor = TestNetworkMonitor()

    private val timeZoneMonitor = TestTimeZoneMonitor()

    // Subject under test.
    private lateinit var state: SurauAppState

    private fun testNavigationState() = NavigationState(
        startKey = PlaceholderHomeNavKey,
        topLevelStack = NavBackStack(PlaceholderHomeNavKey),
        subStacks = mapOf(
            PlaceholderHomeNavKey to NavBackStack(PlaceholderHomeNavKey),
        ),
    )

    @Test
    fun niaAppState_currentDestination() = runTest {
        val navigationState = testNavigationState()

        composeTestRule.setContent {
            state = remember(navigationState) {
                SurauAppState(
                    coroutineScope = backgroundScope,
                    networkMonitor = networkMonitor,
                    timeZoneMonitor = timeZoneMonitor,
                    navigationState = navigationState,
                )
            }
        }

        assertEquals(PlaceholderHomeNavKey, state.navigationState.currentTopLevelKey)
        assertEquals(PlaceholderHomeNavKey, state.navigationState.currentKey)
    }

    @Test
    fun niaAppState_whenNetworkMonitorIsOffline_StateIsOffline() = runTest(UnconfinedTestDispatcher()) {
        composeTestRule.setContent {
            state = SurauAppState(
                coroutineScope = backgroundScope,
                networkMonitor = networkMonitor,
                timeZoneMonitor = timeZoneMonitor,
                navigationState = testNavigationState(),
            )
        }

        backgroundScope.launch { state.isOffline.collect() }
        networkMonitor.setConnected(false)
        assertEquals(
            true,
            state.isOffline.value,
        )
    }

    @Test
    fun niaAppState_differentTZ_withTimeZoneMonitorChange() = runTest(UnconfinedTestDispatcher()) {
        composeTestRule.setContent {
            state = SurauAppState(
                coroutineScope = backgroundScope,
                networkMonitor = networkMonitor,
                timeZoneMonitor = timeZoneMonitor,
                navigationState = testNavigationState(),
            )
        }
        val changedTz = TimeZone.of("Europe/Prague")
        backgroundScope.launch { state.currentTimeZone.collect() }
        timeZoneMonitor.setTimeZone(changedTz)
        assertEquals(
            changedTz,
            state.currentTimeZone.value,
        )
    }
}
