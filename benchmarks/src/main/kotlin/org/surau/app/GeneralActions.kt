/*
 * Copyright 2023 The Android Open Source Project
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

package org.surau.app

import android.Manifest.permission
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Because the app under test is different from the one running the instrumentation test,
 * the permission has to be granted manually by either:
 *
 * - tapping the Allow button
 *    ```kotlin
 *    val obj = By.text("Allow")
 *    val dialog = device.wait(Until.findObject(obj), TIMEOUT)
 *    dialog?.let {
 *        it.click()
 *        device.wait(Until.gone(obj), 5_000)
 *    }
 *    ```
 * - or (preferred) executing the grant command on the target package.
 */
fun MacrobenchmarkScope.allowNotifications() {
    if (SDK_INT >= TIRAMISU) {
        val command = "pm grant $packageName ${permission.POST_NOTIFICATIONS}"
        device.executeShellCommand(command)
    }
}

/**
 * Wraps starting the default activity, waiting for it to start and then allowing notifications in
 * one convenient call.
 */
fun MacrobenchmarkScope.startActivityAndAllowNotifications() {
    startActivityAndWait()
    allowNotifications()
}

/**
 * The core reading journey used for baseline-profile generation and jank measurement:
 * scroll the surah list on the home screen, open a surah, scroll the reader, then go back.
 * Mirrors the most common real path so the captured profile precompiles it.
 */
fun MacrobenchmarkScope.quranReaderJourney() {
    // Home: the surah list (data loads from cache/network on first run).
    val surahList = device.waitAndFindObject(By.res("quranHome:surahList"), 10_000)
    device.flingElementDownUp(surahList)

    // Open Al-Fatihah and scroll the reader.
    device.waitAndFindObject(By.res("quranHome:surah:1"), 10_000).click()
    val ayahList = device.waitAndFindObject(By.res("reader:ayahList"), 10_000)
    device.flingElementDownUp(ayahList)

    // Back to the home list.
    device.pressBack()
    device.waitAndFindObject(By.res("quranHome:surahList"), 10_000)
}

/**
 * The search journey: open search from the home app bar, type a query, wait for results, scroll
 * them, then return home. Precompiles the offline-FTS / search path (a headline M6 feature) that
 * [quranReaderJourney] does not exercise, so the captured profile covers it too.
 */
fun MacrobenchmarkScope.quranSearchJourney() {
    // Open search from the home app bar.
    device.waitAndFindObject(By.res("quranHome:search"), 10_000).click()

    // Type a query and wait for the results list to populate.
    device.waitAndFindObject(By.res("quranSearch:input"), 10_000).text = "Allah"
    val results = device.waitAndFindObject(By.res("quranSearch:results"), 10_000)
    device.flingElementDownUp(results)

    // Dismiss the IME (first back) and return to the home list (second back).
    device.pressBack()
    device.pressBack()
    device.waitAndFindObject(By.res("quranHome:surahList"), 10_000)
}

/**
 * Waits for and returns the `niaTopAppBar`
 */
fun MacrobenchmarkScope.getTopAppBar(): UiObject2 {
    device.wait(Until.hasObject(By.res("niaTopAppBar")), 2_000)
    return device.findObject(By.res("niaTopAppBar"))
}

/**
 * Waits for an object on the top app bar, passed in as [selector].
 */
fun MacrobenchmarkScope.waitForObjectOnTopAppBar(selector: BySelector, timeout: Long = 2_000) {
    getTopAppBar().wait(Until.hasObject(selector), timeout)
}
