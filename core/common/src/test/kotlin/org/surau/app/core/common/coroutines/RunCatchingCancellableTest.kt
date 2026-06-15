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

import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RunCatchingCancellableTest {

    @Test
    fun success_returnsValue() {
        assertEquals(42, runCatchingExceptCancellation { 42 }.getOrNull())
    }

    @Test
    fun ordinaryThrowable_isCaughtAsFailure() {
        val result = runCatchingExceptCancellation { error("boom") }
        assertTrue(result.isFailure)
    }

    @Test
    fun cancellation_isRethrown_notSwallowed() {
        assertFailsWith<CancellationException> {
            runCatchingExceptCancellation { throw CancellationException("cancelled") }
        }
    }
}
