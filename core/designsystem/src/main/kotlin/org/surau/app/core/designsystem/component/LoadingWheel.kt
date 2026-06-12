/*
 * Copyright 2022 The Android Open Source Project
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

package org.surau.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.SurauTheme

/**
 * Surau loading indicator, backed by the Material 3 Expressive [LoadingIndicator].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SurauLoadingWheel(
    contentDesc: String,
    modifier: Modifier = Modifier,
) {
    LoadingIndicator(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = contentDesc }
            .testTag("loadingWheel"),
    )
}

/**
 * Surau loading indicator on a contained surface, for use over content.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SurauOverlayLoadingWheel(
    contentDesc: String,
    modifier: Modifier = Modifier,
) {
    ContainedLoadingIndicator(
        modifier = modifier
            .size(60.dp)
            .semantics { contentDescription = contentDesc }
            .testTag("loadingWheel"),
    )
}

@Preview
@Composable
private fun SurauLoadingWheelPreview() {
    SurauTheme {
        SurauLoadingWheel(contentDesc = "Loading")
    }
}

@Preview
@Composable
private fun SurauOverlayLoadingWheelPreview() {
    SurauTheme {
        SurauOverlayLoadingWheel(contentDesc = "Loading")
    }
}
