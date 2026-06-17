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

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : AuthSubmitViewModel() {

    fun register(email: String, password: String, displayName: String) {
        if (isSubmitting) return

        viewModelScope.launch {
            submitStateFlow.value = AuthSubmitState.Submitting
            try {
                authRepository.register(email.trim(), password, displayName.trim())
                // Registration always requires email verification before login.
                submitStateFlow.value = AuthSubmitState.RequiresVerification(email.trim())
            } catch (exception: Exception) {
                handleFailure(exception)
            }
        }
    }
}
