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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import org.surau.app.core.data.util.NetworkMonitor
import org.surau.app.core.data.util.TimeZoneMonitor
import org.surau.app.core.navigation.NavigationState
import org.surau.app.core.navigation.rememberNavigationState
import org.surau.app.core.ui.TrackDisposableJank
import org.surau.app.navigation.HomeNavKey
import org.surau.app.navigation.TOP_LEVEL_NAV_ITEMS

@Composable
fun rememberSurauAppState(
    networkMonitor: NetworkMonitor,
    timeZoneMonitor: TimeZoneMonitor,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): SurauAppState {
    val navigationState = rememberNavigationState(HomeNavKey, TOP_LEVEL_NAV_ITEMS.keys)

    NavigationTrackingSideEffect(navigationState)

    return remember(
        navigationState,
        coroutineScope,
        networkMonitor,
        timeZoneMonitor,
    ) {
        SurauAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope,
            networkMonitor = networkMonitor,
            timeZoneMonitor = timeZoneMonitor,
        )
    }
}

@Stable
class SurauAppState(
    val navigationState: NavigationState,
    coroutineScope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    timeZoneMonitor: TimeZoneMonitor,
) {
    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val currentTimeZone = timeZoneMonitor.currentTimeZone
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5_000),
            TimeZone.currentSystemDefault(),
        )
}

/**
 * Stores information about navigation events to be used with JankStats
 */
@Composable
private fun NavigationTrackingSideEffect(navigationState: NavigationState) {
    TrackDisposableJank(navigationState.currentKey) { metricsHolder ->
        metricsHolder.state?.putState("Navigation", navigationState.currentKey.toString())
        onDispose {}
    }
}
