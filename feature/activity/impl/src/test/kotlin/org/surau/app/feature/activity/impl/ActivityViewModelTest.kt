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

package org.surau.app.feature.activity.impl

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.test.repository.FakeActivityRepository
import org.surau.app.core.data.test.repository.FakeAuthRepository
import org.surau.app.core.data.test.repository.FakeKhatamRepository
import org.surau.app.core.model.data.quran.KhatamCycle
import org.surau.app.core.testing.util.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val khatamRepository = FakeKhatamRepository()
    private val activityRepository = FakeActivityRepository()
    private val authRepository = FakeAuthRepository()

    private fun viewModel() = ActivityViewModel(
        khatamRepository = khatamRepository,
        activityRepository = activityRepository,
        authRepository = authRepository,
    )

    private fun activeCycle(juz: Set<Int>) = KhatamCycle(
        id = "c1",
        startedAt = null,
        completedAt = null,
        notes = null,
        completedJuz = juz,
        juzCount = juz.size,
        percent = juz.size * 100.0 / KhatamCycle.TOTAL_JUZ,
    )

    private fun success(vm: ActivityViewModel) =
        assertIs<ActivityUiState.Success>(vm.uiState.value)

    @Test
    fun guest_showsLoginRequired() = runTest {
        // FakeAuthRepository starts as a guest.
        assertEquals(ActivityUiState.LoginRequired, viewModel().uiState.value)
    }

    @Test
    fun authenticated_loadsActivityAndKhatam() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1, 2, 3)))

        val state = success(viewModel())
        assertEquals(setOf(1, 2, 3), state.khatam?.completedJuz)
    }

    @Test
    fun authenticated_noActiveCycle_isNull() = runTest {
        authRepository.login("a@b.com", "password")
        // getActiveCycle returns null (the 404 "no active cycle" case).
        assertNull(success(viewModel()).khatam)
    }

    @Test
    fun markJuz_updatesCycle() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1)))
        val vm = viewModel()

        vm.markJuz(2)

        assertEquals(setOf(1, 2), success(vm).khatam?.completedJuz)
    }

    @Test
    fun markJuz_rollsBackOnError_andEmitsError() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1)))
        khatamRepository.failNextMutation = true
        val vm = viewModel()

        vm.errors.test {
            vm.markJuz(5)
            assertEquals(R.string.feature_activity_impl_error_generic, awaitItem())
        }

        // The optimistic mark was reverted; juz 5 is not present and nothing is in flight.
        val state = success(vm)
        assertFalse(5 in state.khatam!!.completedJuz)
        assertTrue(state.juzInFlight.isEmpty())
    }

    @Test
    fun completeKhatam_movesToHistory_whenAllJuzMarked() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle((1..30).toSet()))
        val vm = viewModel()

        vm.completeKhatam()

        val state = success(vm)
        assertNull(state.khatam)
        assertEquals(1, state.history.size)
    }

    @Test
    fun completeKhatam_noOp_whenIncomplete() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1, 2, 3)))
        val vm = viewModel()

        vm.completeKhatam()

        // Not completable (< 30 juz) → the active cycle is untouched.
        assertEquals(setOf(1, 2, 3), success(vm).khatam?.completedJuz)
    }

    @Test
    fun startKhatam_createsActiveCycle() = runTest {
        authRepository.login("a@b.com", "password")
        val vm = viewModel()
        assertNull(success(vm).khatam)

        vm.startKhatam("Khatam Ramadhan")

        assertEquals("Khatam Ramadhan", success(vm).khatam?.notes)
    }
}
