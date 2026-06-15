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

package org.surau.app.core.common.coroutines

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but never swallows coroutine cancellation: a [CancellationException] is
 * rethrown so the surrounding coroutine can still be cancelled cooperatively. Bare
 * `runCatching { ... }` around a suspend call is a structured-concurrency foot-gun — it turns a
 * cancellation into a "failure", which breaks the cancellation chain. Prefer this helper whenever
 * the block suspends.
 */
inline fun <R> runCatchingExceptCancellation(block: () -> R): Result<R> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
