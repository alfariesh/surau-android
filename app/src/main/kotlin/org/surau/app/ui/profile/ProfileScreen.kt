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

package org.surau.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.R
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauSurface
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.model.data.auth.AuthState

@Composable
fun ProfileScreen(
    appVersionName: String,
    onManageAccount: () -> Unit,
    onSettings: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val signedIn = authState is AuthState.Authenticated
    val session = (authState as? AuthState.Authenticated)?.session

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Account header
        SurauSurface(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(SurauTheme.colors.accentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SurauIcons.Person,
                        contentDescription = null,
                        tint = SurauTheme.colors.accent,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session?.displayName?.takeIf { it.isNotBlank() }
                            ?: session?.email
                            ?: stringResource(R.string.profile_guest),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (signedIn) session?.email.orEmpty() else stringResource(R.string.profile_guest_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SurauTheme.colors.muted,
                    )
                }
            }
        }

        if (!signedIn) {
            SurauButton(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                text = { Text(stringResource(R.string.profile_sign_in)) },
            )
        }

        // Links
        SurauSurface(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
            Column {
                if (signedIn) {
                    ProfileRow(SurauIcons.Person, stringResource(R.string.profile_manage_account), onManageAccount)
                    HorizontalDivider(color = SurauTheme.colors.separator)
                }
                ProfileRow(SurauIcons.Settings, stringResource(R.string.profile_settings), onSettings)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_version, appVersionName),
            style = MaterialTheme.typography.bodySmall,
            color = SurauTheme.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = SurauTheme.colors.muted)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(SurauIcons.ChevronRight, contentDescription = null, tint = SurauTheme.colors.muted)
    }
}
