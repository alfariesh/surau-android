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

package org.surau.app.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Upcoming

/**
 * App icon set. Almost everything comes from the custom [SurauHugeIcons] set (duotone default,
 * outline for unselected, solid for active states). A handful of generic glyphs with no custom
 * artwork yet still fall back to Material Icons — see the bottom block.
 */
object SurauIcons {
    val Activity = SurauHugeIcons.CalendarAnalysis
    val ArrowBack = SurauHugeIcons.ArrowLeft
    val AutoStories = SurauHugeIcons.Hadith
    val AutoStoriesBorder = SurauHugeIcons.HadithOutline
    val Bedtime = SurauHugeIcons.Moon
    val Book = SurauHugeIcons.Book
    val BookBorder = SurauHugeIcons.BookOutline
    val BookOpen = SurauHugeIcons.BookOpen

    // Bookmark: outline/duotone = not saved, solid (with check) = saved.
    val Bookmark = SurauHugeIcons.BookmarkSaved
    val BookmarkBorder = SurauHugeIcons.Bookmark
    val Bookmarks = SurauHugeIcons.Bookmark
    val BookmarksBorder = SurauHugeIcons.Bookmark
    val CalendarAnalysis = SurauHugeIcons.CalendarAnalysis
    val Check = SurauHugeIcons.Tick
    val CheckCircle = SurauHugeIcons.CheckCircle
    val CloudOff = SurauHugeIcons.CloudOff
    val Colors = SurauHugeIcons.Colors
    val Dashboard = SurauHugeIcons.Dashboard
    val Delete = SurauHugeIcons.Delete
    val Download = SurauHugeIcons.Download
    val DownloadDone = SurauHugeIcons.CheckCircle
    val Flame = SurauHugeIcons.Flame
    val Flow = SurauHugeIcons.AudioWave
    val Headphones = SurauHugeIcons.Headphones
    val Home = SurauHugeIcons.Mosque
    val HomeBorder = SurauHugeIcons.MosqueOutline
    val Language = SurauHugeIcons.Language
    val LibraryBooks = SurauHugeIcons.Book
    val LibraryBooksBorder = SurauHugeIcons.BookOutline
    val Lock = SurauHugeIcons.Lock
    val Login = SurauHugeIcons.Login
    val Logout = SurauHugeIcons.Logout
    val Mail = SurauHugeIcons.Mail
    val MenuBook = SurauHugeIcons.Quran
    val MenuBookBorder = SurauHugeIcons.QuranOutline
    val Mic = SurauHugeIcons.Mic
    val Moon = SurauHugeIcons.Moon
    val Mosque = SurauHugeIcons.Mosque
    val Note = SurauHugeIcons.Note
    val Palette = SurauHugeIcons.Palette
    val Pause = SurauHugeIcons.Pause
    val Person = SurauHugeIcons.Muslim
    val PersonBorder = SurauHugeIcons.MuslimOutline
    val PlayArrow = SurauHugeIcons.Play
    val ScreenCast = SurauHugeIcons.ScreenCast
    val Search = SurauHugeIcons.Search
    val Settings = SurauHugeIcons.Settings
    val Share = SurauHugeIcons.Marketing
    val ShortText = SurauHugeIcons.TextFont
    val SkipNext = SurauHugeIcons.Next
    val SkipPrevious = SurauHugeIcons.Previous
    val SmartPhone = SurauHugeIcons.SmartPhone
    val Streak = SurauHugeIcons.Flame
    val View = SurauHugeIcons.View
    val ViewOff = SurauHugeIcons.ViewOff

    // --- Fallbacks: no custom artwork yet (still Material Icons) ---
    val Add = Icons.Rounded.Add
    val ArrowForward = Icons.AutoMirrored.Rounded.ArrowForward
    val ChevronDown = Icons.Rounded.KeyboardArrowDown
    val ChevronRight = Icons.AutoMirrored.Rounded.KeyboardArrowRight
    val Close = Icons.Rounded.Close
    val ContentCopy = Icons.Rounded.ContentCopy
    val Grid3x3 = Icons.Rounded.Grid3x3
    val MoreVert = Icons.Default.MoreVert
    val Repeat = Icons.Rounded.Repeat
    val RepeatOne = Icons.Rounded.RepeatOne
    val Upcoming = Icons.Rounded.Upcoming
    val UpcomingBorder = Icons.Outlined.Upcoming
}
