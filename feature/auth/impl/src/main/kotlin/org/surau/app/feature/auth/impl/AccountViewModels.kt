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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import javax.inject.Inject

private const val SUBSCRIBE_TIMEOUT_MS = 5_000L

/**
 * Shared base for account forms: a single-flight [submitState] with error mapping and a ticking
 * rate-limit countdown, mirroring [LoginViewModel].
 */
abstract class SubmittingViewModel : ViewModel() {

    protected val submitStateFlow = MutableStateFlow<AuthSubmitState>(AuthSubmitState.Idle)
    val submitState: StateFlow<AuthSubmitState> = submitStateFlow

    /**
     * Runs [action] as a single submit. [onSuccess] decides the terminal state (default
     * [AuthSubmitState.Success]); [passwordOnly] tunes the 401 message for re-auth forms.
     */
    protected fun submit(
        passwordOnly: Boolean = false,
        onSuccess: () -> Unit = { submitStateFlow.value = AuthSubmitState.Success },
        action: suspend () -> Unit,
    ) {
        if (submitStateFlow.value is AuthSubmitState.Submitting) return

        viewModelScope.launch {
            submitStateFlow.value = AuthSubmitState.Submitting
            try {
                action()
                onSuccess()
            } catch (exception: Exception) {
                val state = exception.toAuthSubmitState(passwordOnly = passwordOnly)
                submitStateFlow.value = state
                if (state is AuthSubmitState.RateLimited) startRateLimitCountdown(state.secondsLeft)
            }
        }
    }

    private fun startRateLimitCountdown(seconds: Long) {
        viewModelScope.launch {
            var left = seconds
            while (left > 0 && submitStateFlow.value is AuthSubmitState.RateLimited) {
                delay(1_000)
                left -= 1
                if (submitStateFlow.value is AuthSubmitState.RateLimited) {
                    submitStateFlow.value =
                        if (left > 0) AuthSubmitState.RateLimited(left) else AuthSubmitState.Idle
                }
            }
        }
    }
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), AuthState.Unknown)

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut

    fun logoutAllDevices() {
        viewModelScope.launch {
            try {
                authRepository.logoutAllDevices()
                _signedOut.value = true
            } catch (_: Exception) {
                // Best effort — staying on the hub is acceptable if the call fails.
            }
        }
    }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : SubmittingViewModel() {

    val currentDisplayName: StateFlow<String?> = authRepository.authState
        .map { (it as? AuthState.Authenticated)?.session?.displayName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), null)

    fun save(displayName: String, countryCode: String) =
        submit { authRepository.updateProfile(displayName.trim(), countryCode.trim()) }
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : SubmittingViewModel() {

    fun changePassword(currentPassword: String, newPassword: String) =
        submit(passwordOnly = true) {
            authRepository.changePassword(currentPassword, newPassword)
        }
}

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : SubmittingViewModel() {

    private val _newEmail = MutableStateFlow("")
    val newEmail: StateFlow<String> = _newEmail

    private val _awaitingOtp = MutableStateFlow(false)
    val awaitingOtp: StateFlow<Boolean> = _awaitingOtp

    fun requestChange(currentPassword: String, email: String) =
        submit(
            passwordOnly = true,
            onSuccess = {
                _newEmail.value = email.trim()
                _awaitingOtp.value = true
                submitStateFlow.value = AuthSubmitState.Idle
            },
        ) {
            authRepository.requestEmailChange(currentPassword, email.trim())
        }

    fun verify(otp: String) =
        submit { authRepository.verifyEmailChange(_newEmail.value, otp.trim()) }
}

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : SubmittingViewModel() {

    fun deleteAccount(currentPassword: String) =
        submit(passwordOnly = true) { authRepository.deleteAccount(currentPassword) }
}

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = SessionsUiState.Loading
            _uiState.value = try {
                SessionsUiState.Success(authRepository.listSessions())
            } catch (_: Exception) {
                SessionsUiState.Error
            }
        }
    }

    fun revoke(sessionId: String) {
        viewModelScope.launch {
            try {
                authRepository.revokeSession(sessionId)
                load()
            } catch (_: Exception) {
                // Keep the current list; the row stays until a successful refresh.
            }
        }
    }
}

sealed interface SessionsUiState {
    data object Loading : SessionsUiState
    data object Error : SessionsUiState
    data class Success(val sessions: List<AccountSession>) : SessionsUiState
}

@HiltViewModel
class EmailPreferencesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmailPrefsUiState>(EmailPrefsUiState.Loading)
    val uiState: StateFlow<EmailPrefsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = EmailPrefsUiState.Loading
            _uiState.value = try {
                EmailPrefsUiState.Success(authRepository.emailPreferences().marketingOptIn)
            } catch (_: Exception) {
                EmailPrefsUiState.Error
            }
        }
    }

    fun setMarketingOptIn(optIn: Boolean) {
        val previous = (_uiState.value as? EmailPrefsUiState.Success)?.marketingOptIn
        _uiState.value = EmailPrefsUiState.Success(optIn) // optimistic
        viewModelScope.launch {
            try {
                authRepository.updateEmailPreferences(optIn)
            } catch (_: Exception) {
                if (previous != null) _uiState.value = EmailPrefsUiState.Success(previous)
            }
        }
    }
}

sealed interface EmailPrefsUiState {
    data object Loading : EmailPrefsUiState
    data object Error : EmailPrefsUiState
    data class Success(val marketingOptIn: Boolean) : EmailPrefsUiState
}
