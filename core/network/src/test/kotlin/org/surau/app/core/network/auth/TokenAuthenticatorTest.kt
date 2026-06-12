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

package org.surau.app.core.network.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

/**
 * A fake provider mimicking [SessionTokenProvider] semantics: single source of truth for the
 * current token; refresh swaps it.
 */
private class FakeTokenProvider(
    @Volatile var currentToken: String? = "stale-token",
    private val refreshResult: (failed: String?) -> String?,
) : AuthTokenProvider {
    val refreshCalls = AtomicInteger(0)

    private val lock = Any()

    override suspend fun accessToken(): String? = currentToken

    override suspend fun refreshTokens(failedAccessToken: String?): String? =
        synchronized(lock) {
            // Mirror the real single-flight semantics (SessionTokenProvider's Mutex): only one
            // caller refreshes; the rest observe the already-swapped token.
            val current = currentToken
            if (current != null && current != failedAccessToken) return current
            refreshCalls.incrementAndGet()
            val refreshed = refreshResult(failedAccessToken)
            currentToken = refreshed
            refreshed
        }
}

class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun client(provider: AuthTokenProvider): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(provider))
            .authenticator(TokenAuthenticator(provider))
            .build()

    @Test
    fun on401_refreshesAndRetriesWithNewToken() {
        val provider = FakeTokenProvider { "fresh-token" }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.getHeader("Authorization") == "Bearer fresh-token") {
                    MockResponse().setResponseCode(200).setBody("ok")
                } else {
                    MockResponse().setResponseCode(401)
                }
        }

        val response = client(provider)
            .newCall(Request.Builder().url(server.url("/v1/me/quran/progress")).build())
            .execute()

        assertEquals(200, response.code)
        assertEquals(1, provider.refreshCalls.get())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun onRefreshFailure_givesUpWithoutRetrying() {
        val provider = FakeTokenProvider { null }
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client(provider)
            .newCall(Request.Builder().url(server.url("/v1/me/quran/progress")).build())
            .execute()

        assertEquals(401, response.code)
        assertEquals(1, provider.refreshCalls.get())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun whenServerKeepsRejecting_doesNotLoopForever() {
        // Refresh "succeeds" each time with a new token, but the server always 401s.
        val counter = AtomicInteger(0)
        val provider = FakeTokenProvider { "token-${counter.incrementAndGet()}" }
        repeat(5) { server.enqueue(MockResponse().setResponseCode(401)) }

        val response = client(provider)
            .newCall(Request.Builder().url(server.url("/v1/me/quran/progress")).build())
            .execute()

        assertEquals(401, response.code)
        // initial request + at most MAX_RETRIES(2) authenticated retries
        assertEquals(3, server.requestCount)
    }

    @Test
    fun concurrentRequests_singleRefreshSharedByAll() {
        val provider = FakeTokenProvider { "fresh-token" }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.getHeader("Authorization") == "Bearer fresh-token") {
                    MockResponse().setResponseCode(200).setBody("ok")
                } else {
                    MockResponse().setResponseCode(401)
                }
        }

        val httpClient = client(provider)
        val pool = Executors.newFixedThreadPool(4)
        val done = CountDownLatch(4)
        val codes = mutableListOf<Int>()
        repeat(4) {
            pool.execute {
                val code = httpClient
                    .newCall(Request.Builder().url(server.url("/v1/me/test")).build())
                    .execute()
                    .code
                synchronized(codes) { codes += code }
                done.countDown()
            }
        }
        done.await()
        pool.shutdown()

        assertEquals(listOf(200, 200, 200, 200), codes.sorted())
        // The fake provider's fast path means only the first 401 triggers an actual refresh.
        assertEquals(1, provider.refreshCalls.get())
    }
}
