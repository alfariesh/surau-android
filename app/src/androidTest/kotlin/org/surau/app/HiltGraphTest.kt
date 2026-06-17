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

package org.surau.app

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.media.SurauPlayerController
import org.surau.app.core.network.retrofit.SurauAuthApi
import org.surau.app.core.network.retrofit.SurauQuranApi
import org.surau.app.core.network.retrofit.SurauUserApi
import javax.inject.Inject

/**
 * Smoke test that the M1/M2 Hilt graph actually assembles at runtime. Resolving these singletons
 * forces Hilt to build the OkHttp clients (including the @AuthClient/@PublicClient split that the
 * Retrofit APIs depend on), the media controller, the session store and the user-data repository —
 * catching scope/qualifier/provider regressions the compile-time check can't.
 */
@HiltAndroidTest
class HiltGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var userDataRepository: UserDataRepository

    @Inject
    lateinit var playerController: SurauPlayerController

    @Inject
    lateinit var authSessionDataSource: AuthSessionDataSource

    @Inject
    lateinit var quranApi: SurauQuranApi

    @Inject
    lateinit var authApi: SurauAuthApi

    @Inject
    lateinit var userApi: SurauUserApi

    @Before
    fun setup() = hiltRule.inject()

    @Test
    fun m1m2Graph_assembles() {
        assertNotNull(userDataRepository)
        assertNotNull(playerController)
        assertNotNull(authSessionDataSource)
        assertNotNull(quranApi)
        assertNotNull(authApi)
        assertNotNull(userApi)
    }
}
