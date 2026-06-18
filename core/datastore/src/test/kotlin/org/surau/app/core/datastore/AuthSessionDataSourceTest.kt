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
import org.surau.app.core.datastore.crypto.AuthCrypto
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.UserSession
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun tokensAreEncryptedAtRest_andDecryptedOnRead() = testScope.runTest {
        val store = InMemoryDataStore(AuthSession.getDefaultInstance())
        val encrypted = AuthSessionDataSource(store, ReversibleCrypto())

        encrypted.setSession(
            session = session,
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSeconds = 1_000,
        )

        // Stored bytes carry the cipher prefix and are not the plaintext…
        val raw = store.data.first()
        assertTrue(raw.accessToken.startsWith(AuthCrypto.CIPHER_PREFIX))
        assertTrue(raw.refreshToken.startsWith(AuthCrypto.CIPHER_PREFIX))
        // …while reads transparently decrypt, and identity stays usable.
        assertEquals("access-1", encrypted.currentAccessToken())
        assertEquals("refresh-1", encrypted.currentRefreshToken())
        assertEquals(AuthState.Authenticated(session), encrypted.authState.first())
    }

    @Test
    fun legacyPlaintextTokens_readBackUnchanged() = testScope.runTest {
        // Simulate an install written before encryption: plaintext tokens, no cipher prefix.
        val store = InMemoryDataStore(
            AuthSession.getDefaultInstance().toBuilder()
                .setAccessToken("legacy-access")
                .setRefreshToken("legacy-refresh")
                .setUserId(session.userId)
                .setEmail(session.email)
                .build(),
        )
        val encrypted = AuthSessionDataSource(store, ReversibleCrypto())

        assertEquals("legacy-access", encrypted.currentAccessToken())
        assertEquals("legacy-refresh", encrypted.currentRefreshToken())

        // The next rotation rewrites them encrypted (lazy migration, no forced logout).
        encrypted.updateTokens("access-2", "refresh-2", expiresAtEpochSeconds = 2_000)
        assertTrue(store.data.first().accessToken.startsWith(AuthCrypto.CIPHER_PREFIX))
        assertEquals("access-2", encrypted.currentAccessToken())
    }

    @Test
    fun undecryptableToken_selfHealsToGuest() = testScope.runTest {
        // Models an invalidated Keystore keyset: a non-empty stored token that can't be decrypted.
        val store = InMemoryDataStore(AuthSession.getDefaultInstance())
        val dataSource = AuthSessionDataSource(store, FailingCrypto())
        dataSource.setSession(
            session = session,
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSeconds = 1_000,
        )
        // The stored ciphertext is non-empty, so the raw record still looks authenticated…
        assertTrue(store.data.first().accessToken.isNotEmpty())

        // …but a failed decrypt self-heals to a clean Guest state instead of a "zombie" session
        // (authenticated UI while every request silently sends no token and 401s).
        assertNull(dataSource.currentAccessToken())
        assertEquals(AuthState.Guest, dataSource.authState.first())
        assertNull(dataSource.currentRefreshToken())
        assertTrue(store.data.first().accessToken.isEmpty())
    }
}

/** Encrypts to a non-empty blob but never decrypts — models an invalidated Keystore keyset. */
private class FailingCrypto : AuthCrypto {
    override fun encrypt(plaintext: String): String = AuthCrypto.CIPHER_PREFIX + plaintext
    override fun decrypt(stored: String): String? = null
}

/** Reversible stand-in for the Keystore AEAD: prefix + reversed string, no Android dependency. */
private class ReversibleCrypto : AuthCrypto {
    override fun encrypt(plaintext: String): String =
        AuthCrypto.CIPHER_PREFIX + plaintext.reversed()

    override fun decrypt(stored: String): String? =
        if (stored.startsWith(AuthCrypto.CIPHER_PREFIX)) {
            stored.removePrefix(AuthCrypto.CIPHER_PREFIX).reversed()
        } else {
            stored
        }
}
