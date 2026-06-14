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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.surau.app.feature.quran.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauTextButton
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.ui.TrackScreenViewEvent

@Composable
fun QuranBookmarksScreen(
    onBackClick: () -> Unit,
    onOpenInReader: (surahId: Int, ayahNumber: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuranBookmarksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    QuranBookmarksScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onOpenInReader = onOpenInReader,
        onSelectTag = viewModel::setTagFilter,
        onSaveBookmark = viewModel::updateBookmark,
        onDeleteBookmark = viewModel::removeBookmark,
        modifier = modifier,
    )
}

@Composable
internal fun QuranBookmarksScreen(
    uiState: BookmarksUiState,
    onBackClick: () -> Unit,
    onOpenInReader: (surahId: Int, ayahNumber: Int) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSaveBookmark: (AyahKey, String?, List<String>) -> Unit,
    onDeleteBookmark: (AyahKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreenViewEvent(screenName = "QuranBookmarks")

    var editing by remember { mutableStateOf<BookmarkListItem?>(null) }
    var deleting by remember { mutableStateOf<BookmarkListItem?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.testTag("bookmarks:back")) {
                Icon(
                    imageVector = SurauIcons.ArrowBack,
                    contentDescription = stringResource(R.string.feature_quran_impl_back),
                )
            }
            Text(
                text = stringResource(R.string.feature_quran_impl_bookmarks_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        when (uiState) {
            BookmarksUiState.Loading -> CenteredBox {
                SurauLoadingWheel(contentDesc = stringResource(R.string.feature_quran_impl_loading))
            }

            BookmarksUiState.Empty -> BookmarksEmpty()

            is BookmarksUiState.Success -> BookmarksList(
                uiState = uiState,
                onOpenInReader = onOpenInReader,
                onSelectTag = onSelectTag,
                onEdit = { editing = it },
                onDelete = { deleting = it },
            )
        }
    }

    editing?.let { item ->
        BookmarkEditorSheet(
            item = item,
            onDismiss = { editing = null },
            onSave = { note, tags ->
                onSaveBookmark(item.ayahKey, note, tags)
                editing = null
            },
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = {
                Text(stringResource(R.string.feature_quran_impl_bookmarks_delete_confirm_title))
            },
            text = {
                Text(stringResource(R.string.feature_quran_impl_bookmarks_delete_confirm_body))
            },
            confirmButton = {
                SurauTextButton(
                    onClick = {
                        onDeleteBookmark(item.ayahKey)
                        deleting = null
                    },
                    modifier = Modifier.testTag("bookmarks:deleteConfirm"),
                ) {
                    Text(stringResource(R.string.feature_quran_impl_bookmarks_delete))
                }
            },
            dismissButton = {
                SurauTextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.feature_quran_impl_bookmarks_cancel))
                }
            },
        )
    }
}

@Composable
private fun BookmarksList(
    uiState: BookmarksUiState.Success,
    onOpenInReader: (Int, Int) -> Unit,
    onSelectTag: (String?) -> Unit,
    onEdit: (BookmarkListItem) -> Unit,
    onDelete: (BookmarkListItem) -> Unit,
) {
    if (uiState.allTags.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.activeTag == null,
                onClick = { onSelectTag(null) },
                label = { Text(stringResource(R.string.feature_quran_impl_bookmarks_filter_all)) },
            )
            uiState.allTags.forEach { tag ->
                FilterChip(
                    selected = uiState.activeTag == tag,
                    onClick = { onSelectTag(if (uiState.activeTag == tag) null else tag) },
                    label = { Text(tag) },
                )
            }
        }
    }

    LazyColumn(modifier = Modifier.testTag("bookmarks:list")) {
        uiState.sections.forEach { section ->
            item(key = "header-${section.surahId}") {
                Text(
                    text = section.surahName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(section.items, key = { it.ayahKey.value }) { item ->
                BookmarkRow(
                    item = item,
                    onClick = { onOpenInReader(item.surahId, item.ayahNumber) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                )
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    item: BookmarkListItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(stringResource(R.string.feature_quran_impl_ayah_number, item.ayahNumber))
        },
        supportingContent = {
            val note = item.note?.takeIf { it.isNotBlank() }
            val tagLine = item.tags.takeIf { it.isNotEmpty() }?.joinToString("  ") { "#$it" }
            val supporting = listOfNotNull(note, tagLine).joinToString("\n")
            if (supporting.isNotEmpty()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.testTag("bookmarks:edit:${item.ayahKey.value}")) {
                    Icon(
                        imageVector = SurauIcons.ShortText,
                        contentDescription = stringResource(R.string.feature_quran_impl_bookmarks_edit),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("bookmarks:delete:${item.ayahKey.value}")) {
                    Icon(
                        imageVector = SurauIcons.Delete,
                        contentDescription = stringResource(R.string.feature_quran_impl_bookmarks_delete),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("bookmarks:item:${item.ayahKey.value}"),
    )
}

@Composable
private fun BookmarkEditorSheet(
    item: BookmarkListItem,
    onDismiss: () -> Unit,
    onSave: (note: String?, tags: List<String>) -> Unit,
) {
    var note by rememberSaveable(item.ayahKey.value) { mutableStateOf(item.note.orEmpty()) }
    val tags = remember(item.ayahKey.value) { item.tags.toMutableStateList() }
    var tagInput by rememberSaveable(item.ayahKey.value) { mutableStateOf("") }

    fun addTag() {
        val candidate = tagInput.trim()
        if (candidate.isNotEmpty() && candidate !in tags) tags.add(candidate)
        tagInput = ""
    }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("bookmarks:editor")) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                text = "${item.surahName} : ${item.ayahNumber}",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.size(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.feature_quran_impl_bookmarks_note_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bookmarks:editor:note"),
            )

            Spacer(modifier = Modifier.size(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text(stringResource(R.string.feature_quran_impl_bookmarks_tag_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bookmarks:editor:tagInput"),
                )
                SurauTextButton(onClick = { addTag() }) {
                    Text(stringResource(R.string.feature_quran_impl_bookmarks_add_tag))
                }
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.toList().forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { tags.remove(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = SurauIcons.Close,
                                    contentDescription = stringResource(
                                        R.string.feature_quran_impl_bookmarks_remove_tag,
                                    ),
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
            SurauButton(
                onClick = { onSave(note.ifBlank { null }, tags.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bookmarks:editor:save"),
            ) {
                Text(stringResource(R.string.feature_quran_impl_bookmarks_save))
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BookmarksEmpty() {
    CenteredBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = SurauIcons.BookmarksBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.feature_quran_impl_bookmarks_empty),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.feature_quran_impl_bookmarks_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
