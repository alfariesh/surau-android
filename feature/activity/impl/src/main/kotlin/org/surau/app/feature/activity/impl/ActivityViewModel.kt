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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.surau.app.core.data.repository.ActivityRepository
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.KhatamRepository
import org.surau.app.core.model.data.activity.ReadingActivity
import org.surau.app.core.model.data.activity.ReadingStreak
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.KhatamCycle
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val khatamRepository: KhatamRepository,
    private val activityRepository: ActivityRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityUiState>(ActivityUiState.Loading)
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    /** One-shot error messages (string resource ids) for a transient snackbar. */
    private val _errors = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    private var loadedForSession = false
    private var loadJob: Job? = null
    private var startInFlight = false

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> if (!loadedForSession) load()
                    AuthState.Guest -> {
                        loadedForSession = false
                        // Cancel any in-flight load so a late result can't overwrite LoginRequired.
                        loadJob?.cancel()
                        _uiState.value = ActivityUiState.LoginRequired
                    }
                    AuthState.Unknown -> Unit // keep Loading until the session resolves
                }
            }
        }
    }

    fun refresh() = load()

    private fun load() {
        loadedForSession = true
        loadJob?.cancel()
        _uiState.value = ActivityUiState.Loading
        loadJob = viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val from = today.minus(FETCH_DAYS, DateTimeUnit.DAY)
            val result = try {
                coroutineScope {
                    val streak = async { activityRepository.getStreak(today) }
                    val activity = async { activityRepository.getActivity(from, today) }
                    val khatam = async { khatamRepository.getActiveCycle() }
                    // History is secondary — its failure must not take down the whole screen.
                    val history = async {
                        runCatching { khatamRepository.history() }.getOrDefault(emptyList())
                    }
                    ActivityUiState.Success(
                        today = today,
                        streak = streak.await(),
                        activity = activity.await(),
                        khatam = khatam.await(),
                        history = history.await(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ActivityUiState.Error
            }
            // Bail if we were cancelled (e.g. logout) before publishing the result.
            ensureActive()
            _uiState.value = result
        }
    }

    fun markJuz(juz: Int) = toggleJuz(juz, mark = true)

    fun unmarkJuz(juz: Int) = toggleJuz(juz, mark = false)

    /** Optimistically flips a juz, then reconciles to the server result (or rolls back on error). */
    private fun toggleJuz(juz: Int, mark: Boolean) {
        val before = (_uiState.value as? ActivityUiState.Success)?.khatam ?: return
        val optimistic = if (mark) before.completedJuz + juz else before.completedJuz - juz
        updateSuccess {
            it.copy(
                khatam = before.copy(
                    completedJuz = optimistic,
                    juzCount = optimistic.size,
                    percent = optimistic.size * 100.0 / KhatamCycle.TOTAL_JUZ,
                ),
                juzInFlight = it.juzInFlight + juz,
            )
        }
        viewModelScope.launch {
            try {
                val updated = if (mark) khatamRepository.markJuz(juz) else khatamRepository.unmarkJuz(juz)
                updateSuccess { it.copy(khatam = updated, juzInFlight = it.juzInFlight - juz) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateSuccess { it.copy(khatam = before, juzInFlight = it.juzInFlight - juz) }
                _errors.tryEmit(R.string.feature_activity_impl_error_generic)
            }
        }
    }

    fun startKhatam(notes: String?) {
        if (startInFlight) return
        startInFlight = true
        viewModelScope.launch {
            try {
                val cycle = khatamRepository.startCycle(notes)
                updateSuccess { it.copy(khatam = cycle) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errors.tryEmit(R.string.feature_activity_impl_error_generic)
            } finally {
                startInFlight = false
            }
        }
    }

    fun completeKhatam() {
        val state = _uiState.value as? ActivityUiState.Success ?: return
        val khatam = state.khatam ?: return
        if (!khatam.isCompletable || state.completing) return
        updateSuccess { it.copy(completing = true) }
        viewModelScope.launch {
            try {
                val completed = khatamRepository.complete()
                updateSuccess {
                    it.copy(khatam = null, history = listOf(completed) + it.history, completing = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errors.tryEmit(R.string.feature_activity_impl_error_generic)
                // Resync to the server's truth so a stale 30/30 doesn't loop on a 409.
                val fresh = runCatching { khatamRepository.getActiveCycle() }.getOrNull()
                updateSuccess { it.copy(khatam = fresh ?: it.khatam, completing = false) }
            }
        }
    }

    private inline fun updateSuccess(transform: (ActivityUiState.Success) -> ActivityUiState.Success) {
        _uiState.update { state -> if (state is ActivityUiState.Success) transform(state) else state }
    }

    private companion object {
        // Fetch six weeks of activity; the heatmap displays the most recent five.
        const val FETCH_DAYS = 42
    }
}

sealed interface ActivityUiState {
    data object Loading : ActivityUiState

    /** Guests can't read personal activity — the screen shows a login CTA. */
    data object LoginRequired : ActivityUiState

    data object Error : ActivityUiState

    data class Success(
        val today: LocalDate,
        val streak: ReadingStreak,
        val activity: ReadingActivity,
        val khatam: KhatamCycle?,
        val history: List<KhatamCycle>,
        val juzInFlight: Set<Int> = emptySet(),
        val completing: Boolean = false,
    ) : ActivityUiState
}
