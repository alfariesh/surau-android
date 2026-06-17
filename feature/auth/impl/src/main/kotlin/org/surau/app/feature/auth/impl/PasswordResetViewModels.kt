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
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : AuthSubmitViewModel() {

    fun sendResetEmail(email: String) {
        if (isSubmitting) return

        viewModelScope.launch {
            submitStateFlow.value = AuthSubmitState.Submitting
            try {
                authRepository.forgotPassword(email.trim())
                submitStateFlow.value = AuthSubmitState.Success
            } catch (exception: Exception) {
                handleFailure(exception)
            }
        }
    }
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : AuthSubmitViewModel() {

    fun resetPassword(token: String, newPassword: String) {
        if (isSubmitting) return

        viewModelScope.launch {
            submitStateFlow.value = AuthSubmitState.Submitting
            try {
                authRepository.resetPassword(token.trim(), newPassword)
                submitStateFlow.value = AuthSubmitState.Success
            } catch (exception: Exception) {
                handleFailure(exception)
            }
        }
    }
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val userDataRepository: org.surau.app.core.data.repository.UserDataRepository,
) : ViewModel() {

    fun markWelcomeShown() {
        viewModelScope.launch { userDataRepository.setWelcomeShown(true) }
    }
}
