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

package org.surau.app.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.UserSession
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthSessionDataSourceTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var subject: AuthSessionDataSource

    private val session = UserSession(
        userId = "user-1",
        email = "user@surau.org",
        displayName = "Hamba Allah",
        sessionId = "session-1",
        emailVerified = true,
    )

    @Before
    fun setup() {
        subject = AuthSessionDataSource(InMemoryDataStore(AuthSession.getDefaultInstance()))
    }

    @Test
    fun defaultStateIsGuest() = testScope.runTest {
        assertEquals(AuthState.Guest, subject.authState.first())
        assertNull(subject.currentAccessToken())
        assertNull(subject.currentRefreshToken())
    }

    @Test
    fun setSessionAuthenticates() = testScope.runTest {
        subject.setSession(
            session = session,
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSeconds = 1_000,
        )

        assertEquals(AuthState.Authenticated(session), subject.authState.first())
        assertEquals("access-1", subject.currentAccessToken())
        assertEquals("refresh-1", subject.currentRefreshToken())
    }

    @Test
    fun updateTokensKeepsIdentity() = testScope.runTest {
        subject.setSession(
            session = session,
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSeconds = 1_000,
        )

        subject.updateTokens(
            accessToken = "access-2",
            refreshToken = "refresh-2",
            expiresAtEpochSeconds = 2_000,
        )

        assertEquals(AuthState.Authenticated(session), subject.authState.first())
        assertEquals("access-2", subject.currentAccessToken())
        assertEquals("refresh-2", subject.currentRefreshToken())
    }

    @Test
    fun clearReturnsToGuest() = testScope.runTest {
        subject.setSession(
            session = session,
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSeconds = 1_000,
        )

        subject.clear()

        assertEquals(AuthState.Guest, subject.authState.first())
        assertNull(subject.currentAccessToken())
    }
}
