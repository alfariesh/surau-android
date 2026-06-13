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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.icon.SurauIcons

/**
 * Shared scaffold for auth forms: back button, headline, subtitle, content.
 */
@Composable
internal fun AuthScreenScaffold(
    title: String,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        if (onBackClick != null) {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = SurauIcons.ArrowBack,
                        contentDescription = stringResource(R.string.feature_auth_impl_back),
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            content()
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.feature_auth_impl_email)) },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.feature_auth_impl_email_invalid)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.feature_auth_impl_password),
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.feature_auth_impl_password_too_short)) }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) SurauIcons.Check else SurauIcons.MoreVert,
                    contentDescription = stringResource(
                        if (visible) {
                            R.string.feature_auth_impl_hide_password
                        } else {
                            R.string.feature_auth_impl_show_password
                        },
                    ),
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Inline error/rate-limit message under a form.
 */
@Composable
internal fun AuthSubmitError(state: AuthSubmitState, modifier: Modifier = Modifier) {
    val message = when (state) {
        is AuthSubmitState.Error -> when (state.kind) {
            AuthErrorKind.INVALID_CREDENTIALS ->
                stringResource(R.string.feature_auth_impl_invalid_credentials)
            AuthErrorKind.EMAIL_EXISTS ->
                stringResource(R.string.feature_auth_impl_email_exists)
            AuthErrorKind.OFFLINE -> stringResource(R.string.feature_auth_impl_offline_error)
            AuthErrorKind.GENERIC -> stringResource(R.string.feature_auth_impl_generic_error)
        }

        is AuthSubmitState.RateLimited ->
            stringResource(R.string.feature_auth_impl_rate_limited, state.secondsLeft)

        else -> null
    }
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(top = 8.dp),
        )
    }
}

internal object AuthValidators {
    private val EMAIL_REGEX = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= 8

    fun isValidOtp(otp: String): Boolean = otp.trim().length == 6 && otp.trim().all(Char::isDigit)
}
