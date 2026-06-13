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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * The backend's error envelope: `{error, code, message, details?, retry_after?, request_id?}`.
 */
@Serializable
data class ApiErrorBodyDto(
    @SerialName("error") val error: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("retry_after") val retryAfterSeconds: Long? = null,
    @SerialName("request_id") val requestId: String? = null,
)

/**
 * A typed API failure with the backend's error code and optional rate-limit retry delay.
 */
class SurauApiException(
    val httpStatus: Int,
    val code: String?,
    override val message: String,
    val retryAfterSeconds: Long? = null,
    val requestId: String? = null,
) : Exception(message) {

    val isEmailNotVerified: Boolean get() = code == CODE_EMAIL_NOT_VERIFIED
    val isInvalidCredentials: Boolean get() = code == CODE_INVALID_CREDENTIALS
    val isRateLimited: Boolean get() = httpStatus == 429 || code == CODE_RATE_LIMITED

    companion object {
        const val CODE_EMAIL_NOT_VERIFIED = "AUTH_EMAIL_NOT_VERIFIED"
        const val CODE_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS"
        const val CODE_RATE_LIMITED = "AUTH_RATE_LIMITED"
        const val CODE_TOKEN_INVALID = "AUTH_TOKEN_INVALID"
    }
}

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs [block] and maps Retrofit [HttpException]s to [SurauApiException] with the parsed backend
 * error envelope. [IOException]s (connectivity) pass through unchanged.
 */
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (exception: HttpException) {
    throw exception.toSurauApiException()
}

fun HttpException.toSurauApiException(): SurauApiException {
    val body = try {
        response()?.errorBody()?.string()?.let { raw ->
            if (raw.isNotBlank()) errorJson.decodeFromString<ApiErrorBodyDto>(raw) else null
        }
    } catch (_: Exception) {
        null
    }
    return SurauApiException(
        httpStatus = code(),
        code = body?.code,
        message = body?.message ?: body?.error ?: message(),
        retryAfterSeconds = body?.retryAfterSeconds,
        requestId = body?.requestId,
    )
}
