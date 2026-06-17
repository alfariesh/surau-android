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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.test.repository.FakeActivityRepository
import org.surau.app.core.data.test.repository.FakeAuthRepository
import org.surau.app.core.data.test.repository.FakeKhatamRepository
import org.surau.app.core.data.test.repository.FakeQuranRepository
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
    private val quranRepository = FakeQuranRepository()
    private val authRepository = FakeAuthRepository()

    private fun viewModel() = ActivityViewModel(
        khatamRepository = khatamRepository,
        activityRepository = activityRepository,
        quranRepository = quranRepository,
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
    fun unmarkJuz_rollsBackOnError_restoresOnlyThatJuz() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1, 2, 3)))
        khatamRepository.failNextMutation = true
        val vm = viewModel()

        vm.errors.test {
            vm.unmarkJuz(2)
            assertEquals(R.string.feature_activity_impl_error_generic, awaitItem())
        }

        // The optimistic unmark is reverted by re-adding only juz 2; the others are untouched.
        val state = success(vm)
        assertEquals(setOf(1, 2, 3), state.khatam?.completedJuz)
        assertTrue(state.juzInFlight.isEmpty())
    }

    @Test
    fun concurrentJuzToggles_failingOneRevertsOnlyItself_notTheOther() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1)))
        val gate = CompletableDeferred<Unit>()
        khatamRepository.mutationGate = gate
        khatamRepository.failJuz.add(2) // juz 2's server call fails AFTER the gate
        val vm = viewModel()

        vm.markJuz(2) // optimistic {1,2}, server in flight
        vm.markJuz(3) // optimistic {1,2,3}, server in flight

        // Both optimistic flips are visible while their server calls are gated.
        assertEquals(setOf(1, 2, 3), success(vm).khatam?.completedJuz)
        assertEquals(setOf(2, 3), success(vm).juzInFlight)

        gate.complete(Unit)
        advanceUntilIdle()

        // juz 2 reverts ONLY itself; juz 3 stays. A whole-cycle snapshot rollback would have
        // clobbered juz 3 back to {1}.
        val state = success(vm)
        assertEquals(setOf(1, 3), state.khatam?.completedJuz)
        assertTrue(state.juzInFlight.isEmpty())
    }

    @Test
    fun markJuz_showsOptimisticGuessThenConfirmedServerResult() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1)))
        val gate = CompletableDeferred<Unit>()
        khatamRepository.mutationGate = gate
        // The server returns an authoritative set the optimistic guess ({1,2}) would never produce.
        khatamRepository.nextMutationResult = activeCycle(setOf(1, 2, 5))
        val vm = viewModel()

        vm.markJuz(2)

        // Optimistic phase: just the local delta, juz 2 in flight.
        assertEquals(setOf(1, 2), success(vm).khatam?.completedJuz)
        assertTrue(2 in success(vm).juzInFlight)

        gate.complete(Unit)
        advanceUntilIdle()

        // Confirmed phase: the server's set replaces the optimistic guess; nothing left in flight.
        val state = success(vm)
        assertEquals(setOf(1, 2, 5), state.khatam?.completedJuz)
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

    @Test
    fun authenticated_loadFailure_showsError() = runTest {
        authRepository.login("a@b.com", "password")
        activityRepository.failLoads = true

        assertEquals(ActivityUiState.Error, viewModel().uiState.value)
    }

    @Test
    fun logout_duringLoad_doesNotClobberLoginRequired() = runTest {
        val gate = CompletableDeferred<Unit>()
        activityRepository.loadGate = gate
        authRepository.login("a@b.com", "password")

        val vm = viewModel() // load launches and suspends on the gate
        assertEquals(ActivityUiState.Loading, vm.uiState.value)

        authRepository.logout() // -> LoginRequired, cancelling the in-flight load
        assertEquals(ActivityUiState.LoginRequired, vm.uiState.value)

        gate.complete(Unit) // the cancelled load must not resurrect Success
        assertEquals(ActivityUiState.LoginRequired, vm.uiState.value)
    }

    @Test
    fun historyFailure_stillShowsSuccessWithEmptyHistory() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle(setOf(1, 2)))
        khatamRepository.failHistory = true

        val state = success(viewModel())
        assertEquals(setOf(1, 2), state.khatam?.completedJuz)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun completeKhatam_error_resyncsAndKeepsActiveCycle() = runTest {
        authRepository.login("a@b.com", "password")
        khatamRepository.setActiveCycle(activeCycle((1..30).toSet()))
        khatamRepository.failNextMutation = true
        val vm = viewModel()

        vm.errors.test {
            vm.completeKhatam()
            assertEquals(R.string.feature_activity_impl_error_generic, awaitItem())
        }

        // complete() failed before clearing → the cycle is resynced and stays active.
        val state = success(vm)
        assertEquals(30, state.khatam?.completedJuz?.size)
        assertFalse(state.completing)
    }
}
