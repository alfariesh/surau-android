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

package org.surau.app.core.network.model

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SurauApiErrorTest {

    private fun httpException(code: Int, body: String): HttpException =
        HttpException(
            Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())),
        )

    @Test
    fun parsesBackendErrorEnvelope() = runTest {
        val exception = assertFailsWith<SurauApiException> {
            apiCall {
                throw httpException(
                    403,
                    """{"error":"email not verified","code":"AUTH_EMAIL_NOT_VERIFIED","message":"email not verified"}""",
                )
            }
        }

        assertEquals(403, exception.httpStatus)
        assertEquals("AUTH_EMAIL_NOT_VERIFIED", exception.code)
        assertTrue(exception.isEmailNotVerified)
        assertEquals("email not verified", exception.message)
    }

    @Test
    fun parsesRetryAfterOnRateLimit() = runTest {
        val exception = assertFailsWith<SurauApiException> {
            apiCall {
                throw httpException(
                    429,
                    """{"error":"rate limited","code":"AUTH_RATE_LIMITED","message":"rate limited","retry_after":60}""",
                )
            }
        }

        assertTrue(exception.isRateLimited)
        assertEquals(60, exception.retryAfterSeconds)
    }

    @Test
    fun survivesNonJsonErrorBody() = runTest {
        val exception = assertFailsWith<SurauApiException> {
            apiCall { throw httpException(500, "<html>Bad Gateway</html>") }
        }

        assertEquals(500, exception.httpStatus)
        assertEquals(null, exception.code)
    }
}
