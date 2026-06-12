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

package org.surau.app.core.data.di

import org.surau.app.core.data.auth.SessionTokenProvider
import org.surau.app.core.data.repository.OfflineFirstUserDataRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.data.util.ConnectivityManagerNetworkMonitor
import org.surau.app.core.data.util.NetworkMonitor
import org.surau.app.core.data.util.TimeZoneBroadcastMonitor
import org.surau.app.core.data.util.TimeZoneMonitor
import org.surau.app.core.network.auth.AuthTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsUserDataRepository(
        userDataRepository: OfflineFirstUserDataRepository,
    ): UserDataRepository

    @Binds
    internal abstract fun bindsAuthTokenProvider(
        sessionTokenProvider: SessionTokenProvider,
    ): AuthTokenProvider

    @Binds
    internal abstract fun bindsNetworkMonitor(
        networkMonitor: ConnectivityManagerNetworkMonitor,
    ): NetworkMonitor

    @Binds
    internal abstract fun binds(impl: TimeZoneBroadcastMonitor): TimeZoneMonitor
}
