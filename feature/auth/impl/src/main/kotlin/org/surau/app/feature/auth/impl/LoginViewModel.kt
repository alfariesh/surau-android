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

package org.surau.app.feature.auth.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.domain.SyncUserDataAfterLoginUseCase
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncUserDataAfterLogin: SyncUserDataAfterLoginUseCase,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    private val _submitState = MutableStateFlow<AuthSubmitState>(AuthSubmitState.Idle)
    val submitState: StateFlow<AuthSubmitState> = _submitState

    fun login(email: String, password: String) {
        if (_submitState.value is AuthSubmitState.Submitting) return

        viewModelScope.launch {
            _submitState.value = AuthSubmitState.Submitting
            try {
                authRepository.login(email.trim(), password)
                // First sign-in implies the welcome decision has been made.
                userDataRepository.setWelcomeShown(true)
                syncUserDataAfterLogin()
                _submitState.value = AuthSubmitState.Success
            } catch (exception: Exception) {
                val state = exception.toAuthSubmitState(email = email.trim())
                _submitState.value = state
                if (state is AuthSubmitState.RateLimited) startRateLimitCountdown(state.secondsLeft)
            }
        }
    }

    fun consumeNavigation() {
        _submitState.value = AuthSubmitState.Idle
    }

    private fun startRateLimitCountdown(seconds: Long) {
        viewModelScope.launch {
            var left = seconds
            while (left > 0 && _submitState.value is AuthSubmitState.RateLimited) {
                delay(1_000)
                left -= 1
                if (_submitState.value is AuthSubmitState.RateLimited) {
                    _submitState.value =
                        if (left > 0) AuthSubmitState.RateLimited(left) else AuthSubmitState.Idle
                }
            }
        }
    }
}
