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

package org.surau.app.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Custom icon set ported from HugeIcons (24×24, `currentColor`). Built as [ImageVector]s so they
 * drop into `Icon(imageVector = …)` everywhere and tint via `LocalContentColor`. Paths are drawn in
 * opaque black; the [Icon] tint recolours them while preserving the duotone 40% fill alpha.
 *
 * Variants: **duotone** (40% fill + 1.5dp outline) is the default look, **outline** (stroke only) is
 * the unselected/inactive state, and **solid** marks an active state (e.g. a saved bookmark).
 */
internal object SurauHugeIcons {

    // Player
    val Play = icon("Play") {
        fillSoft(PLAY)
        line(PLAY)
    }
    val Pause = icon("Pause") {
        fillSoft(PAUSE_L)
        fillSoft(PAUSE_R)
        line(PAUSE_L)
        line(PAUSE_R)
    }
    val Next = icon("Next") {
        fillSoft(NEXT_TRI)
        line(NEXT_TRI)
        line("M20 4.5L20 19.5")
    }
    val Previous = icon("Previous") {
        fillSoft(PREV_TRI)
        line(PREV_TRI)
        line("M4 19.5L4 4.5")
    }
    val AudioWave = icon("AudioWave") {
        line(
            "M3 14V9.5C3 8.67157 3.67157 8 4.5 8C5.32843 8 6 8.67157 6 9.5V16.5C6 17.3284 6.67157 " +
                "18 7.5 18C8.32843 18 9 17.3284 9 16.5V4.5C9 3.67157 9.67157 3 10.5 3C11.3284 3 12 " +
                "3.67157 12 4.5V19.5C12 20.3284 12.6716 21 13.5 21C14.3284 21 15 20.3284 15 19.5V8.5C15 " +
                "7.67157 15.6716 7 16.5 7C17.3284 7 18 7.67157 18 8.5V15.5C18 16.3284 18.6716 17 19.5 " +
                "17C20.3284 17 21 16.3284 21 15.5V12",
        )
    }
    val Mic = icon("Mic") {
        fillSoft(MIC_HEAD)
        line(MIC_HEAD)
        line("M17 7H14M17 11H14")
        line("M20 11C20 15.4183 16.4183 19 12 19M12 19C7.58172 19 4 15.4183 4 11M12 19V22M12 22H14.5M12 22H9.5")
    }

    // Bookmark — duotone = default, solid (with check) = saved
    val Bookmark = icon("Bookmark") {
        fillSoft(BOOKMARK)
        line(BOOKMARK)
    }
    val BookmarkSaved = icon("BookmarkSaved") {
        fillSolid(
            "M6.75 2C5.23122 2 4 3.23122 4 4.75V21.75C4 22.0134 4.13822 22.2576 4.36413 22.3931C4.59003 " +
                "22.5287 4.87049 22.5357 5.10294 22.4118L12.25 18.6L19.3971 22.4118C19.6295 22.5357 19.91 " +
                "22.5287 20.1359 22.3931C20.3618 22.2576 20.5 22.0135 20.5 21.75V4.75C20.5 3.23122 19.2688 " +
                "2 17.75 2H6.75ZM15.9571 9.45711C16.3476 9.06658 16.3476 8.43342 15.9571 8.04289C15.5666 " +
                "7.65237 14.9334 7.65237 14.5429 8.04289L11.25 11.3358L9.95711 10.0429C9.56658 9.65237 " +
                "8.93342 9.65237 8.54289 10.0429C8.15237 10.4334 8.15237 11.0666 8.54289 11.4571L10.5429 " +
                "13.4571C10.9334 13.8476 11.5666 13.8476 11.9571 13.4571L15.9571 9.45711Z",
        )
    }

    // Content / navigation
    val Quran = icon("Quran") {
        fillSoft(QURAN_FILL, evenOdd = true)
        line(QURAN_STAR)
        line(BOOK_COVER)
    }
    val QuranOutline = icon("QuranOutline") {
        line(QURAN_STAR)
        line(BOOK_COVER)
    }
    val Hadith = icon("Hadith") {
        fillSoft(BOOK_PAGE)
        line(HADITH_FACE)
        line(HADITH_EYE)
        line(BOOK_COVER_SPINE)
    }
    val HadithOutline = icon("HadithOutline") {
        line(HADITH_FACE)
        line(HADITH_EYE)
        line(BOOK_COVER_SPINE)
    }
    val Book = icon("Book") {
        fillSoft(BOOK_PAGE)
        line("M15.501 7H8.50098M12.5 11H8.5")
        line(BOOK_COVER)
        line(BOOK_CLASP)
    }
    val BookOutline = icon("BookOutline") {
        line("M15.5 7H8.5M12.499 11H8.49902")
        line(BOOK_COVER)
        line(BOOK_CLASP)
    }
    val BookOpen = icon("BookOpen") {
        fillSoft("M22 5.55847C20.9578 5.19634 20.1689 5 19 5V16.5C13 16.5 12 22 12 22C13.8315 20.3871 16.2062 19.4966 18.6667 19.5C19.8356 19.5 20.9578 19.6963 22 20.0585V5.55847Z")
        fillSoft(BOOK_OPEN_LEFT)
        line("M12 7.5V22C13.8315 20.3871 16.2062 19.4966 18.6667 19.5C19.8356 19.5 20.9578 19.6963 22 20.0585V5.55847C20.9578 5.19634 20.1689 5 19 5")
        line("M19 2C13 2 12 7.5 12 7.5V22C12 22 13 16.5 19 16.5V2Z")
        line(BOOK_OPEN_LEFT)
    }
    val Note = icon("Note") {
        fillSoft(NOTE_BODY)
        line("M16.5 2V5M7.5 2V5M12 2V5")
        line(NOTE_BODY)
        line("M8 15H12M8 11H16")
    }
    val Muslim = icon("Muslim") {
        fillSoft(MUSLIM_ROBE)
        fillSoft(MUSLIM_HOOD_TOP)
        muslimStrokes()
    }
    val MuslimOutline = icon("MuslimOutline") {
        muslimStrokes()
    }
    val Mosque = icon("Mosque") {
        fillSoft(MOSQUE_DOME)
        fillSoft(MOSQUE_MINARET)
        mosqueStrokes()
    }
    val MosqueOutline = icon("MosqueOutline") {
        mosqueStrokes()
    }

    // Standalone
    val Flame = icon("Flame") {
        fillSoft(FLAME)
        line(FLAME)
    }
    val Moon = icon("Moon") {
        fillSoft(MOON)
        line(MOON)
    }
    val Settings = icon("Settings") {
        fillSoft(SETTINGS_FILL, evenOdd = true)
        line("M19.5 2.5C20.6046 2.5 21.5 3.39543 21.5 4.5V19.5C21.5 20.6046 20.6046 21.5 19.5 21.5H4.5C3.39543 21.5 2.5 20.6046 2.5 19.5L2.5 4.5C2.5 3.39543 3.39543 2.5 4.5 2.5L19.5 2.5Z")
        line("M16 14C16.5523 14 17 14.4477 17 15L17 16C17 16.5523 16.5523 17 16 17L15 17C14.4477 17 14 16.5523 14 16L14 15C14 14.4477 14.4477 14 15 14L16 14Z")
        line("M9 7C9.55228 7 10 7.44772 10 8L10 9C10 9.55228 9.55228 10 9 10L8 10C7.44772 10 7 9.55228 7 9L7 8C7 7.44772 7.44772 7 8 7L9 7Z")
        line("M7.5 15.5H14")
        line("M10.5 8.5H17")
    }
    val Dashboard = icon("Dashboard") {
        fillSoft(DASHBOARD_DOT)
        line(DASHBOARD_DOT)
        line("M12 15V10")
        line("M22 13C22 7.47715 17.5228 3 12 3C6.47715 3 2 7.47715 2 13")
    }
    val CalendarAnalysis = icon("CalendarAnalysis") {
        fillSoft("M5 4H19C20.1046 4 21 4.89543 21 6V10H3V6C3 4.89543 3.89543 4 5 4Z")
        line("M16.5 2V6M7.5 2V6")
        line("M5 4H19C20.1046 4 21 4.89543 21 6V20C21 21.1046 20.1046 22 19 22H5C3.89543 22 3 21.1046 3 20V6C3 4.89543 3.89543 4 5 4Z")
        line("M8 18L11 15L13 17L16 14")
        line("M3 10H21")
    }
    val ArrowLeft = icon("ArrowLeft") {
        line("M15 6L9 12.0001L15 18")
    }

    // Actions / status
    val Search = icon("Search") {
        fillSoft(SEARCH_LENS)
        line("M17 17L21 21")
        line(SEARCH_LENS)
    }
    val Tick = icon("Tick") {
        line("M5 14C5 14 7 14.5 8.5 17.5C8.5 17.5 14.0588 8.33333 19 6.5")
    }
    val CheckCircle = icon("CheckCircle") {
        fillSoft(CIRCLE)
        line(CIRCLE)
        line("M8 13.0714C8 13.0714 9.5 13.7143 10.4 15C10.4 15 12.5 10.7143 16 9")
    }
    val Delete = icon("Delete") {
        fillSoft("M19.5 5.5L18.6139 20.121C18.5499 21.1766 17.6751 22 16.6175 22H7.38246C6.32488 22 5.4501 21.1766 5.38612 20.121L4.5 5.5H19.5Z")
        line("M19.5 5.5L18.6139 20.121C18.5499 21.1766 17.6751 22 16.6175 22H7.38246C6.32488 22 5.4501 21.1766 5.38612 20.121L4.5 5.5")
        line("M16 5.5H21M16 5.5L14.7597 2.60608C14.6022 2.2384 14.2406 2 13.8406 2H10.1594C9.75937 2 9.39783 2.2384 9.24025 2.60608L8 5.5M16 5.5H8M3 5.5H8")
        line("M9.5 16.5L9.5 10.5")
        line("M14.5 16.5L14.5 10.5")
    }
    val Download = icon("Download") {
        fillSoft(CIRCLE)
        line(CIRCLE)
        line("M16 12.5L12 16.5L8 12.5M12 15.5V7.5")
    }
    val Language = icon("Language") {
        fillSoft(CIRCLE_R10)
        line(CIRCLE_R10)
        line("M7 8.5H11.5M11.5 8.5H17M11.5 8.5V7M8.5 17C11 15 14 11 14.5 8.5M10 11C10.5 12.5 12.5 15 13.5 15.5")
    }
    val CloudOff = icon("CloudOff") {
        fillSoft("M14 9C14 10.1046 13.1046 11 12 11C10.8954 11 10 10.1046 10 9C10 7.89543 10.8954 7 12 7C13.1046 7 14 7.89543 14 9Z")
        line("M12 12.5L12 20")
        line("M11.5 7.06301C11.6598 7.02188 11.8274 7 12 7C13.1046 7 14 7.89543 14 9C14 9.17265 13.9781 9.34019 13.937 9.5")
        line("M2 2L22 22")
        line("M16.9588 6C17.6186 6.86961 18 7.89801 18 9C18 10.102 17.6186 11.1304 16.9588 12M7.04117 12C6.38143 11.1304 6 10.102 6 9C6 8.29588 6.15572 7.62181 6.44027 7")
        line("M20.3159 4C21.3796 5.43008 22 7.14984 22 9C22 10.8502 21.3796 12.5699 20.3159 14M3.68409 4C2.62036 5.43008 2 7.14984 2 9C2 10.8502 2.62036 12.5699 3.68409 14")
    }
    val TextFont = icon("TextFont") {
        line("M14 19L9 5H7L2 19M4 14H12")
        line("M16.5 11.5L16.6298 11.3053C17.1735 10.4898 18.0887 10 19.0688 10C20.6876 10 22 11.3124 22 12.9312V18.5M22 14H18.561C17.1466 14 16 15.1466 16 16.561C16 17.908 17.092 19 18.439 19H18.7408C19.2376 19 19.725 18.865 20.151 18.6094L20.3033 18.518C21.3559 17.8864 22 16.7489 22 15.5213V14Z")
    }

    // Auth / account
    val Login = icon("Login") {
        fillSoft(LOGIN_DOOR)
        line(LOGIN_DOOR)
        line("M14 3.99986H9C8.44772 3.99986 8 4.44757 8 4.99986V5.99986M14 19.9999H9C8.44772 19.9999 8 19.5521 8 18.9999V17.9999")
        line("M8.5 14.4999L11 11.9999L8.5 9.49986M10 11.9999H3")
    }
    val Logout = icon("Logout") {
        fillSoft(LOGOUT_DOOR)
        line(LOGOUT_DOOR)
        line("M10 4H15C15.5523 4 16 4.44772 16 5V7M10 20H15C15.5523 20 16 19.5523 16 19V17")
        line("M18.5 14.5L21 12L18.5 9.5M20 12H13")
    }
    val Mail = icon("Mail") {
        fillSoft("M4 20H20C21.1046 20 22 19.1046 22 18V7L12.8944 11.5528C12.3314 11.8343 11.6686 11.8343 11.1056 11.5528L2 7V18C2 19.1046 2.89543 20 4 20Z")
        line("M4 4H20C21.1046 4 22 4.89543 22 6V18C22 19.1046 21.1046 20 20 20H4C2.89543 20 2 19.1046 2 18V6C2 4.89543 2.89543 4 4 4Z")
        line("M22 7L12.8944 11.5528C12.3314 11.8343 11.6686 11.8343 11.1056 11.5528L2 7")
    }
    val Lock = icon("Lock") {
        fillSoft(LOCK_BODY)
        line(LOCK_BODY)
        line("M16.5001 8.99902V6.49902C16.5001 4.01374 14.4854 1.99902 12.0001 1.99902C9.51484 1.99902 7.50012 4.01374 7.50012 6.49902V8.99902")
        line("M12.1251 15.499H12.0001M12.2501 15.499C12.2501 15.6371 12.1382 15.749 12.0001 15.749C11.8621 15.749 11.7501 15.6371 11.7501 15.499C11.7501 15.361 11.8621 15.249 12.0001 15.249C12.1382 15.249 12.2501 15.361 12.2501 15.499Z")
        line("M8.12512 15.499H8.00012M8.25012 15.499C8.25012 15.6371 8.13819 15.749 8.00012 15.749C7.86205 15.749 7.75012 15.6371 7.75012 15.499C7.75012 15.361 7.86205 15.249 8.00012 15.249C8.13819 15.249 8.25012 15.361 8.25012 15.499Z")
        line("M16.1251 15.499H16.0001M16.2501 15.499C16.2501 15.6371 16.1382 15.749 16.0001 15.749C15.8621 15.749 15.7501 15.6371 15.7501 15.499C15.7501 15.361 15.8621 15.249 16.0001 15.249C16.1382 15.249 16.2501 15.361 16.2501 15.499Z")
    }
    val View = icon("View") {
        fillSoft(
            "M11.9997 5C16.1815 5 19.7644 9.01291 21.2576 10.9619C21.7318 11.581 21.7318 12.419 21.2576 " +
                "13.0381C19.7644 14.9871 16.1815 19 11.9997 19C7.81825 18.9999 4.23613 14.9871 2.74291 " +
                "13.0381C2.26864 12.419 2.26864 11.581 2.74291 10.9619C4.23613 9.01286 7.81825 5.00015 " +
                "11.9997 5ZM11.9997 9C10.343 9.00013 8.99975 10.3432 8.99975 12C8.99975 13.6568 10.343 " +
                "14.9999 11.9997 15C13.6566 15 14.9997 13.6569 14.9997 12C14.9997 10.3431 13.6566 9 11.9997 9Z",
            evenOdd = true,
        )
        line("M15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15C13.6569 15 15 13.6569 15 12Z")
        line("M12 5C16.1818 5 19.764 9.01321 21.2572 10.9622C21.7314 11.5813 21.7314 12.4187 21.2572 13.0378C19.764 14.9868 16.1818 19 12 19C7.81823 19 4.23598 14.9868 2.74283 13.0378C2.26856 12.4187 2.26857 11.5813 2.74283 10.9622C4.23598 9.01321 7.81823 5 12 5Z")
    }
    val ViewOff = icon("ViewOff") {
        fillSoft("M2 12C3.5 15 7.31078 19 12 19C13.8793 19 15.5657 18.3368 17 17.4186L14 14.1421C13.47 14.6722 12.7377 15 11.9289 15C10.3113 15 9 13.6887 9 12.0711C9 11.2623 9.32783 10.53 9.85786 9.99999L6.5 6.91846C3.5 8.99999 2 12 2 12Z")
        line("M19.439 15.439C21 14 22 12 22 12C20.5 9 16.6892 5 12 5C11.0922 5 10.2294 5.15476 9.41827 5.41827M17 17.4186C15.5657 18.3368 13.8793 19 12 19C7.31078 19 3.5 15 2 12C2 12 3.5 9 6.5 6.91847")
        line("M9.85786 10C9.32783 10.53 9 11.2623 9 12.0711C9 13.6887 10.3113 15 11.9289 15C12.7377 15 13.47 14.6722 14 14.1421")
        line("M3 3L21 21")
    }

    // Settings / device / misc
    val Headphones = icon("Headphones") {
        fillSoft(HEADPHONE_L)
        fillSoft(HEADPHONE_R)
        line("M20.0849 17C20.5849 15.5 21 13.4368 21 12C21 7.02944 16.9706 3 12 3C7.02944 3 3 7.02944 3 12C3 13.4368 3.41512 15.5 3.91512 17")
        line(HEADPHONE_L)
        line(HEADPHONE_R)
    }
    val Palette = icon("Palette") {
        fillSoft(
            "M12 2C17.5228 2 22 6.47715 22 12C22 13.9084 21.601 15.1999 19.5 15.5C18.3334 15.6667 17.3333 " +
                "15.667 16.4814 15.667C14.7778 15.667 13.6667 15.6667 13 17C12.4524 18.0951 12.9079 18.7713 " +
                "13.3682 19.4541C13.6828 19.9209 14 20.391 14 21C14 22.1163 12.8417 22 12 22C6.47715 22 2 " +
                "17.5228 2 12C2 6.47715 6.47715 2 12 2ZM16.5 8C15.6716 8 15 8.67157 15 9.5C15 10.3284 15.6716 " +
                "11 16.5 11C17.3284 11 18 10.3284 18 9.5C18 8.67157 17.3284 8 16.5 8ZM9.5 7C8.67157 7 8 " +
                "7.67157 8 8.5C8 9.32843 8.67157 10 9.5 10C10.3284 10 11 9.32843 11 8.5C11 7.67157 10.3284 7 9.5 7Z",
            evenOdd = true,
        )
        line("M22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C12.8417 22 14 22.1163 14 21C14 20.391 13.6832 19.9212 13.3686 19.4544C12.9082 18.7715 12.4523 18.0953 13 17C13.6667 15.6667 14.7778 15.6667 16.4815 15.6667C17.3334 15.6667 18.3334 15.6667 19.5 15.5C21.601 15.1999 22 13.9084 22 12Z")
        line("M9.5 10C10.3284 10 11 9.32843 11 8.5C11 7.67157 10.3284 7 9.5 7C8.67157 7 8 7.67157 8 8.5C8 9.32843 8.67157 10 9.5 10Z")
        line("M16.5 11C17.3284 11 18 10.3284 18 9.5C18 8.67157 17.3284 8 16.5 8C15.6716 8 15 8.67157 15 9.5C15 10.3284 15.6716 11 16.5 11Z")
        line("M7.125 15H7M7.25 15C7.25 15.1381 7.13807 15.25 7 15.25C6.86193 15.25 6.75 15.1381 6.75 15C6.75 14.8619 6.86193 14.75 7 14.75C7.13807 14.75 7.25 14.8619 7.25 15Z")
    }
    val Colors = icon("Colors") {
        fillSoft("M12 2C8.68629 2 6 4.68629 6 8C6 8.7805 6.14903 9.52618 6.42018 10.2102C3.87294 10.9036 2 13.2331 2 16C2 19.3137 4.68629 22 8 22C9.53671 22 10.9385 21.4223 12 20.4722C10.7725 19.3736 10 17.777 10 16C10 15.2195 10.149 14.4738 10.4202 13.7898C11.3002 11.5699 13.4668 10 16 10C16.5468 10 17.0765 10.0731 17.5798 10.2102C17.851 9.52618 18 8.7805 18 8C18 4.68629 15.3137 2 12 2Z")
        line("M17.5798 10.2102C17.0765 10.0731 16.5468 10 16 10C13.4668 10 11.3002 11.5699 10.4202 13.7898M17.5798 10.2102C20.1271 10.9036 22 13.2331 22 16C22 19.3137 19.3137 22 16 22C14.4633 22 13.0615 21.4223 12 20.4722M17.5798 10.2102C17.851 9.52618 18 8.7805 18 8C18 4.68629 15.3137 2 12 2C8.68629 2 6 4.68629 6 8C6 8.7805 6.14903 9.52618 6.42018 10.2102M10.4202 13.7898C10.149 14.4738 10 15.2195 10 16C10 17.777 10.7725 19.3736 12 20.4722M10.4202 13.7898C8.59146 13.292 7.11029 11.951 6.42018 10.2102M6.42018 10.2102C3.87294 10.9036 2 13.2331 2 16C2 19.3137 4.68629 22 8 22C9.53671 22 10.9385 21.4223 12 20.4722")
    }
    val ScreenCast = icon("ScreenCast") {
        fillSoft("M3 8C9.62742 8 15 13.3726 15 20H19C20.6569 20 22 18.6569 22 17V6C22 4.34315 20.6569 3 19 3H6C4.34315 3 3 4.34315 3 6V8Z")
        line("M4 21C4 19.8954 3.10457 19 2 19M8 21C8 17.6863 5.31371 15 2 15M12 21C12 15.4772 7.52285 11 2 11")
        line("M15.2941 20H19C20.6569 20 22 18.6569 22 17V6C22 4.34315 20.6569 3 19 3H6C4.34315 3 3 4.34315 3 6V8")
    }
    val SmartPhone = icon("SmartPhone") {
        fillSoft(PHONE_BODY)
        line(PHONE_BODY)
        line("M12.125 19H12M12.25 19C12.25 19.1381 12.1381 19.25 12 19.25C11.8619 19.25 11.75 19.1381 11.75 19C11.75 18.8619 11.8619 18.75 12 18.75C12.1381 18.75 12.25 18.8619 12.25 19Z")
    }
    val Marketing = icon("Marketing") {
        fillSoft("M14 10a4 8 0 1 0 8 0a4 8 0 1 0 -8 0Z")
        line("M14 10a4 8 0 1 0 8 0a4 8 0 1 0 -8 0Z")
        line("M18 2C14.8969 2 8.46512 4.37761 4.77105 5.85372C3.07942 6.52968 2 8.17832 2 10C2 11.8217 3.07942 13.4703 4.77105 14.1463C8.46512 15.6224 14.8969 18 18 18")
        line("M11 22L9.05674 20.9303C6.94097 19.7657 5.74654 17.4134 6.04547 15")
    }

    // --- shared sub-paths ---
    private const val CIRCLE =
        "M22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C17.5228 22 22 17.5228 22 12Z"
    private const val CIRCLE_R10 = "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z"
    private const val SEARCH_LENS =
        "M19 11C19 6.58172 15.4183 3 11 3C6.58172 3 3 6.58172 3 11C3 15.4183 6.58172 19 11 19C15.4183 19 19 15.4183 19 11Z"
    private const val LOGIN_DOOR =
        "M14.8604 1.99986C14.3852 1.99986 14 2.38506 14 2.86024V21.1395C14 21.6147 14.3852 21.9999 14.8604 " +
            "21.9999C14.9529 21.9999 15.0447 21.985 15.1325 21.9557L20.3162 20.2278C20.7246 20.0917 21 19.7095 " +
            "21 19.2791V4.72062C21 4.29019 20.7246 3.90805 20.3162 3.77193L15.1325 2.04401C15.0447 2.01477 14.9529 1.99986 14.8604 1.99986Z"
    private const val LOGOUT_DOOR =
        "M10 2.86038V21.1396C10 21.6148 9.61479 22 9.13962 22C9.04714 22 8.95527 21.9851 8.86754 21.9558L3.68377 " +
            "20.2279C3.27543 20.0918 3 19.7097 3 19.2792V4.72076C3 4.29033 3.27543 3.90819 3.68377 3.77208L8.86754 " +
            "2.04415C8.95527 2.01491 9.04714 2 9.13962 2C9.61479 2 10 2.38521 10 2.86038Z"
    private const val LOCK_BODY =
        "M17.9999 8.99902H6.00012C4.89548 8.99902 4.00002 9.89457 4.00012 10.9992L4.00095 19.9992C4.00106 " +
            "21.1037 4.89646 21.999 6.00095 21.999H17.9999C19.1045 21.999 19.9999 21.1036 19.9999 19.999V10.999C19.9999 9.89445 19.1045 8.99902 17.9999 8.99902Z"
    private const val HEADPHONE_L =
        "M8.97651 19.6043L7.23857 14.6127C7.05341 14.1466 6.4617 13.9131 5.97493 14.0297C4.46441 14.5333 3.6462 " +
            "16.1718 4.14742 17.6895L4.58543 19.0158C5.08664 20.5334 6.71747 21.3555 8.22799 20.8519C8.68896 20.6556 9.10449 20.0897 8.97651 19.6043Z"
    private const val HEADPHONE_R =
        "M15.0235 19.6043L16.7614 14.6127C16.9466 14.1466 17.5383 13.9131 18.0251 14.0297C19.5356 14.5333 20.3538 " +
            "16.1718 19.8526 17.6895L19.4146 19.0158C18.9134 20.5334 17.2825 21.3555 15.772 20.8519C15.311 20.6556 14.8955 20.0897 15.0235 19.6043Z"
    private const val PHONE_BODY =
        "M16.5 2H7.5C6.39543 2 5.5 2.89543 5.5 4V20C5.5 21.1046 6.39543 22 7.5 22H16.5C17.6046 22 18.5 21.1046 18.5 20V4C18.5 2.89543 17.6046 2 16.5 2Z"
    private const val BOOK_PAGE =
        "M20 18H6C4.89543 18 4 18.8954 4 20V4C4 2.89543 4.89543 2 6 2H20V18Z"
    private const val BOOK_COVER =
        "M20 22H6C4.89543 22 4 21.1046 4 20M4 20C4 18.8954 4.89543 18 6 18H20V2H6C4.89543 2 4 2.89543 4 4V20Z"
    private const val BOOK_COVER_SPINE =
        "M20 22H6C4.89543 22 4 21.1046 4 20M4 20C4 18.8954 4.89543 18 6 18H20V2H6C4.89543 2 4 2.89543 4 " +
            "4V20ZM19.5 18C19.5 18 18.5 18.7628 18.5 20C18.5 21.2372 19.5 22 19.5 22"
    private const val BOOK_CLASP =
        "M19.5 18C19.5 18 18.5 18.7628 18.5 20C18.5 21.2372 19.5 22 19.5 22"
    private const val BOOK_OPEN_LEFT =
        "M5.33333 5.00001C7.79379 4.99657 10.1685 5.88709 12 7.5V22C10.1685 20.3871 7.79379 19.4966 " +
            "5.33333 19.5C4.16444 19.5 3.04222 19.6963 2 20.0585V5.55847C3.04222 5.19634 4.16444 " +
            "5.00001 5.33333 5.00001Z"
    private const val QURAN_STAR =
        "M12 6L10.8 7L9 7L9 8.8L8 10L9 11.2L9 13L10.8 13L12 14L13.2 13H15L15 11.2L16 10L15 8.8L15 7L13.2 7L12 6Z"
    private const val QURAN_FILL =
        "M20 18H6C4.89543 18 4 18.8954 4 20V4C4 2.89543 4.89543 2 6 2H20V18ZM10.7998 7H9V8.7998L8 10L9 " +
            "11.2002V13H10.7998L12 14L13.2002 13H15V11.2002L16 10L15 8.7998V7H13.2002L12 6L10.7998 7Z"
    private const val HADITH_FACE =
        "M15 11.4343C14.4347 12.3725 13.406 13 12.2308 13C10.4465 13 9 11.5535 9 9.76923C9 8.594 9.6275 7.56534 10.5657 7"
    private const val HADITH_EYE =
        "M14.125 8H14M14.25 8C14.25 8.13807 14.1381 8.25 14 8.25C13.8619 8.25 13.75 8.13807 13.75 8C13.75 " +
            "7.86193 13.8619 7.75 14 7.75C14.1381 7.75 14.25 7.86193 14.25 8Z"
    private const val NOTE_BODY =
        "M18 3.5H6C4.89543 3.5 4 4.39543 4 5.5V20C4 21.1046 4.89543 22 6 22H18C19.1046 22 20 21.1046 20 " +
            "20V5.5C20 4.39543 19.1046 3.5 18 3.5Z"
    private const val PLAY =
        "M7.5241 19.0621C6.85783 19.4721 6 18.9928 6 18.2104V5.78956C6 5.00724 6.85783 4.52789 7.5241 " +
            "4.93791L17.6161 11.1483C18.2506 11.5388 18.2506 12.4612 17.6161 12.8517L7.5241 19.0621Z"
    private const val PAUSE_L =
        "M5 20H9C9.55228 20 10 19.5523 10 19V5C10 4.44772 9.55228 4 9 4H5C4.44772 4 4 4.44772 4 5V19C4 19.5523 4.44772 20 5 20Z"
    private const val PAUSE_R =
        "M15 20H19C19.5523 20 20 19.5523 20 19V5C20 4.44772 19.5523 4 19 4H15C14.4477 4 14 4.44772 14 5V19C14 19.5523 14.4477 20 15 20Z"
    private const val NEXT_TRI =
        "M5.5241 19.0621C4.85783 19.4721 4 18.9928 4 18.2104V5.78956C4 5.00724 4.85783 4.52789 5.5241 " +
            "4.93791L15.6161 11.1483C16.2506 11.5388 16.2506 12.4612 15.6161 12.8517L5.5241 19.0621Z"
    private const val PREV_TRI =
        "M18.4759 19.0621C19.1422 19.4721 20 18.9928 20 18.2104V5.78956C20 5.00724 19.1422 4.52789 " +
            "18.4759 4.93791L8.38394 11.1483C7.74941 11.5388 7.74941 12.4612 8.38394 12.8517L18.4759 19.0621Z"
    private const val MIC_HEAD =
        "M17 7V11C17 13.7614 14.7614 16 12 16C9.23858 16 7 13.7614 7 11V7C7 4.23858 9.23858 2 12 2C14.7614 2 17 4.23858 17 7Z"
    private const val BOOKMARK =
        "M12 17.5L19.5 21.5V4.5C19.5 3.39543 18.6046 2.5 17.5 2.5H6.5C5.39543 2.5 4.5 3.39543 4.5 4.5V21.5L12 17.5Z"
    private const val FLAME =
        "M12 21.5C16.1421 21.5 19.5 18.1421 19.5 14C19.5 12.5 19.5 11 18 8C17.6667 8.83333 16.6 11 15 " +
            "11C15 5 12.3333 3.16667 11 2.5C10 7 4.5 8 4.5 14C4.5 18.1421 7.85786 21.5 12 21.5Z"
    private const val MOON =
        "M21.5 14.0784C20.3003 14.7189 18.9301 15.0821 17.4751 15.0821C12.7491 15.0821 8.91792 11.2509 " +
            "8.91792 6.52485C8.91792 5.06986 9.28105 3.69968 9.92163 2.5C5.66765 3.49698 2.5 7.31513 2.5 " +
            "11.8731C2.5 17.1899 6.8101 21.5 12.1269 21.5C16.6849 21.5 20.503 18.3324 21.5 14.0784Z"
    private const val SETTINGS_FILL =
        "M19.5 2.5C20.6046 2.5 21.5 3.39543 21.5 4.5V19.5C21.5 20.6046 20.6046 21.5 19.5 21.5H4.5C3.39543 " +
            "21.5 2.5 20.6046 2.5 19.5V4.5C2.5 3.39543 3.39543 2.5 4.5 2.5H19.5ZM15 14C14.4477 14 14 " +
            "14.4477 14 15V16C14 16.5523 14.4477 17 15 17H16C16.5523 17 17 16.5523 17 16V15C17 14.4477 " +
            "16.5523 14 16 14H15ZM8 7C7.44772 7 7 7.44772 7 8V9C7 9.55228 7.44772 10 8 10H9C9.55228 10 10 " +
            "9.55228 10 9V8C10 7.44772 9.55228 7 9 7H8Z"
    private const val DASHBOARD_DOT = "M9 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z"
    private const val MUSLIM_ROBE =
        "M4.212 16.4804L9.99826 14L11.9996 16L14 14L19.7863 16.4804C20.5215 16.7956 20.9983 17.5187 " +
            "20.9983 18.3187V22H3V18.3187C3 17.5187 3.47672 16.7956 4.212 16.4804Z"
    private const val MUSLIM_HOOD_TOP =
        "M8.71674 3.52486C9.42263 2.44833 10.7123 2 11.9996 2C13.2869 2 14.577 2.44833 15.2829 " +
            "3.52486C15.7829 4.2874 15.9993 5.09433 15.9996 6.01197H8C8.00036 5.09433 8.21673 4.2874 8.71674 3.52486Z"
    private const val MUSLIM_HOOD =
        "M8.71674 3.52486C9.42263 2.44833 10.7123 2 11.9996 2C13.2869 2 14.577 2.44833 15.2829 " +
            "3.52486C15.7829 4.2874 15.9993 5.09433 15.9996 6.01197C15.9999 6.66714 15.9044 7.31882 " +
            "15.7161 7.94636C15.1367 9.87779 14.4561 12 11.9996 12C9.54314 12 8.86295 9.87779 8.28352 " +
            "7.94636C8.09526 7.31882 7.99975 6.66714 8 6.01197C8.00036 5.09433 8.21673 4.2874 8.71674 3.52486Z"
    private const val MOSQUE_DOME =
        "M11.0006 8C12.984 10.25 16.9992 11 16.9992 17H5C5 11 9.01516 10.25 10.9986 8"
    private const val MOSQUE_MINARET =
        "M17 17L17.5 6H20.5L21 22H19V19.5C19 17.4317 18.6547 17 17 17Z"

    private fun ImageVector.Builder.muslimStrokes() {
        line("M10 11.5V14L12 16L14 14V11.5")
        line("M12 16V20")
        line("M9.99826 14L4.212 16.4804C3.47672 16.7956 3 17.5187 3 18.3187V22M14 14L19.7863 16.4804C20.5215 16.7956 20.9983 17.5187 20.9983 18.3187V22")
        line(MUSLIM_HOOD)
        line("M8 6.00003L15.9986 6")
    }

    private fun ImageVector.Builder.mosqueStrokes() {
        line("M2 22H22")
        line("M17.5125 6C15.9698 4 18.3389 3 19 2C19.6611 3 22.0302 4 20.4875 6H17.5125Z")
        line("M17.5 6L17 17M17 22H21L20.5 6")
        line(MOSQUE_DOME)
        line("M3 22V19C3 17.8954 3.89543 17 5 17H17C18.1046 17 19 17.8954 19 19V22")
    }
}

private val Ink = SolidColor(Color.Black)

private inline fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

/** Duotone background fill at 40% alpha. */
private fun ImageVector.Builder.fillSoft(pathData: String, evenOdd: Boolean = false) {
    addPath(
        pathData = addPathNodes(pathData),
        pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
        fill = Ink,
        fillAlpha = 0.4f,
    )
}

/** Fully filled (solid) path. */
private fun ImageVector.Builder.fillSolid(pathData: String) {
    addPath(
        pathData = addPathNodes(pathData),
        pathFillType = PathFillType.EvenOdd,
        fill = Ink,
    )
}

/** 1.5dp round stroke outline. */
private fun ImageVector.Builder.line(pathData: String) {
    addPath(
        pathData = addPathNodes(pathData),
        stroke = Ink,
        strokeLineWidth = 1.5f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}
