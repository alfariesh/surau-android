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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
     * [AuthSubmitState.Success]); [passwordOnly] tunes the 401 message for re-auth forms and
     * [otpForm] tunes the 400 message for code-entry steps.
     */
    protected fun submit(
        passwordOnly: Boolean = false,
        otpForm: Boolean = false,
        onSuccess: () -> Unit = { submitStateFlow.value = AuthSubmitState.Success },
        action: suspend () -> Unit,
    ) {
        if (submitStateFlow.value is AuthSubmitState.Submitting) return

        viewModelScope.launch {
            submitStateFlow.value = AuthSubmitState.Submitting
            try {
                action()
                onSuccess()
            } catch (cancellation: CancellationException) {
                // Navigating away / clearing the VM mid-submit is not an error — let it propagate
                // instead of mapping it to a generic failure state.
                throw cancellation
            } catch (exception: Exception) {
                val state = exception.toAuthSubmitState(passwordOnly = passwordOnly, otpForm = otpForm)
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

    /** One-shot error messages (string resource ids) for a transient snackbar. */
    private val _errors = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    fun logoutAllDevices() {
        viewModelScope.launch {
            try {
                authRepository.logoutAllDevices()
                _signedOut.value = true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Stay on the hub, but tell the user it didn't work so they can retry.
                _errors.tryEmit(R.string.feature_auth_impl_account_logout_all_error)
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
        submit(otpForm = true) { authRepository.verifyEmailChange(_newEmail.value, otp.trim()) }

    /** Returns from the OTP step to the request form (there is no resend — re-enter to restart). */
    fun restartEmailChange() {
        _awaitingOtp.value = false
        submitStateFlow.value = AuthSubmitState.Idle
    }
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

    /** One-shot error messages (string resource ids) for a transient snackbar. */
    private val _errors = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = SessionsUiState.Loading
            _uiState.value = try {
                SessionsUiState.Success(authRepository.listSessions())
            } catch (cancellation: CancellationException) {
                throw cancellation
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
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Keep the current list, but surface the failure so the user can retry.
                _errors.tryEmit(R.string.feature_auth_impl_account_session_revoke_error)
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

    /** The last server-confirmed value — the rollback target, so racing toggles never revert wrong. */
    private var confirmedOptIn: Boolean? = null
    private var updateJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = EmailPrefsUiState.Loading
            _uiState.value = try {
                val optIn = authRepository.emailPreferences().marketingOptIn
                confirmedOptIn = optIn
                EmailPrefsUiState.Success(optIn)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                EmailPrefsUiState.Error
            }
        }
    }

    fun setMarketingOptIn(optIn: Boolean) {
        // Roll back to the server-confirmed value, not the (possibly optimistic) current UI value.
        val baseline = confirmedOptIn ?: return
        _uiState.value = EmailPrefsUiState.Success(optIn) // optimistic
        // Supersede any in-flight toggle so the latest tap wins.
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            try {
                val result = authRepository.updateEmailPreferences(optIn).marketingOptIn
                confirmedOptIn = result
                _uiState.value = EmailPrefsUiState.Success(result)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                confirmedOptIn = baseline
                _uiState.value = EmailPrefsUiState.Success(baseline)
            }
        }
    }
}

sealed interface EmailPrefsUiState {
    data object Loading : EmailPrefsUiState
    data object Error : EmailPrefsUiState
    data class Success(val marketingOptIn: Boolean) : EmailPrefsUiState
}
