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

package org.surau.app.core.data.test

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.surau.app.core.data.di.DataModule
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.QuranAudioRepository
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.data.repository.UserRepository
import org.surau.app.core.data.test.repository.FakeAuthRepository
import org.surau.app.core.data.test.repository.FakeQuranAudioRepository
import org.surau.app.core.data.test.repository.FakeQuranProgressRepository
import org.surau.app.core.data.test.repository.FakeQuranRepository
import org.surau.app.core.data.test.repository.FakeUserDataRepository
import org.surau.app.core.data.test.repository.FakeUserRepository
import org.surau.app.core.data.util.NetworkMonitor
import org.surau.app.core.data.util.TimeZoneMonitor
import org.surau.app.core.network.auth.AuthTokenProvider

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class],
)
internal interface TestDataModule {
    @Binds
    fun bindsUserDataRepository(
        userDataRepository: FakeUserDataRepository,
    ): UserDataRepository

    @Binds
    fun bindsAuthRepository(
        authRepository: FakeAuthRepository,
    ): AuthRepository

    @Binds
    fun bindsUserRepository(
        userRepository: FakeUserRepository,
    ): UserRepository

    @Binds
    fun bindsQuranRepository(
        quranRepository: FakeQuranRepository,
    ): QuranRepository

    @Binds
    fun bindsQuranAudioRepository(
        quranAudioRepository: FakeQuranAudioRepository,
    ): QuranAudioRepository

    @Binds
    fun bindsQuranProgressRepository(
        quranProgressRepository: FakeQuranProgressRepository,
    ): QuranProgressRepository

    @Binds
    fun bindsAuthTokenProvider(
        authTokenProvider: FakeAuthTokenProvider,
    ): AuthTokenProvider

    @Binds
    fun bindsNetworkMonitor(
        networkMonitor: AlwaysOnlineNetworkMonitor,
    ): NetworkMonitor

    @Binds
    fun binds(impl: DefaultZoneIdTimeZoneMonitor): TimeZoneMonitor
}
