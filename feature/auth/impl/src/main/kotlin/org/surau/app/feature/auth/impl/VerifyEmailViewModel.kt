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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.AuthRepository

@HiltViewModel(assistedFactory = VerifyEmailViewModel.Factory::class)
class VerifyEmailViewModel @AssistedInject constructor(
    private val authRepository: AuthRepository,
    @Assisted val email: String,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(email: String): VerifyEmailViewModel
    }

    private val _submitState = MutableStateFlow<AuthSubmitState>(AuthSubmitState.Idle)
    val submitState: StateFlow<AuthSubmitState> = _submitState

    private val _resendCooldownSeconds = MutableStateFlow(0L)
    val resendCooldownSeconds: StateFlow<Long> = _resendCooldownSeconds

    fun verify(otp: String) {
        if (_submitState.value is AuthSubmitState.Submitting) return

        viewModelScope.launch {
            _submitState.value = AuthSubmitState.Submitting
            try {
                authRepository.verifyEmail(email, otp.trim())
                _submitState.value = AuthSubmitState.Success
            } catch (exception: Exception) {
                _submitState.value = exception.toAuthSubmitState()
            }
        }
    }

    fun resend() {
        if (_resendCooldownSeconds.value > 0) return

        viewModelScope.launch {
            try {
                authRepository.resendVerification(email)
            } catch (_: Exception) {
                // Resend is best effort; the cooldown still applies to avoid hammering.
            }
            _resendCooldownSeconds.value = RESEND_COOLDOWN_SECONDS
            while (_resendCooldownSeconds.value > 0) {
                delay(1_000)
                _resendCooldownSeconds.value -= 1
            }
        }
    }

    companion object {
        private const val RESEND_COOLDOWN_SECONDS = 60L
    }
}
