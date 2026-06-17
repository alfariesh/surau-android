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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.Clock
import org.surau.app.core.data.repository.KhatamRepository
import org.surau.app.core.model.data.quran.KhatamCycle
import java.io.IOException
import javax.inject.Inject

/**
 * Fake [KhatamRepository] holding one active cycle + history in memory.
 *
 * Set [failNextMutation] to make the next mark/unmark/start/complete throw once — exercising the
 * ViewModel's optimistic-update rollback path.
 */
class FakeKhatamRepository @Inject constructor() : KhatamRepository {

    private var activeCycle: KhatamCycle? = null
    private val historyCycles = mutableListOf<KhatamCycle>()

    /** When true, the next mutating call throws (then resets) so tests can assert rollback. */
    var failNextMutation: Boolean = false

    /** When true, [history] throws — to verify the screen tolerates a failed history load. */
    var failHistory: Boolean = false

    /** Juz whose mark/unmark fails AFTER the gate — lets a test fail one of two concurrent toggles. */
    val failJuz: MutableSet<Int> = mutableSetOf()

    /** When set, mark/unmark suspends here before returning, so a test can observe the optimistic state. */
    var mutationGate: CompletableDeferred<Unit>? = null

    /** When non-null, the next mark/unmark returns this exact cycle (one-shot) instead of the computed
     * one — lets a test prove the confirmed server result wins over the optimistic guess. */
    var nextMutationResult: KhatamCycle? = null

    fun setActiveCycle(cycle: KhatamCycle?) {
        activeCycle = cycle
    }

    fun setHistory(cycles: List<KhatamCycle>) {
        historyCycles.clear()
        historyCycles.addAll(cycles)
    }

    override suspend fun getActiveCycle(): KhatamCycle? = activeCycle

    override suspend fun startCycle(notes: String?): KhatamCycle {
        maybeFail()
        return KhatamCycle(
            id = "fake-cycle",
            startedAt = Clock.System.now(),
            completedAt = null,
            notes = notes,
            completedJuz = emptySet(),
            juzCount = 0,
            percent = 0.0,
        ).also { activeCycle = it }
    }

    override suspend fun markJuz(juz: Int): KhatamCycle = mutateJuz(juz) { it + juz }

    override suspend fun unmarkJuz(juz: Int): KhatamCycle = mutateJuz(juz) { it - juz }

    override suspend fun complete(): KhatamCycle {
        maybeFail()
        val current = activeCycle ?: error("no active cycle")
        val completed = current.copy(completedAt = Clock.System.now())
        historyCycles.add(0, completed)
        activeCycle = null
        return completed
    }

    override suspend fun history(limit: Int, offset: Int): List<KhatamCycle> {
        if (failHistory) throw IOException("fake history failure")
        return historyCycles.toList()
    }

    private suspend fun mutateJuz(juz: Int, transform: (Set<Int>) -> Set<Int>): KhatamCycle {
        maybeFail()
        mutationGate?.await()
        if (failJuz.remove(juz)) throw IOException("fake juz failure")
        nextMutationResult?.let { override ->
            nextMutationResult = null
            activeCycle = override
            return override
        }
        val current = activeCycle ?: error("no active cycle")
        val updated = transform(current.completedJuz)
        return current.copy(
            completedJuz = updated,
            juzCount = updated.size,
            percent = updated.size * 100.0 / KhatamCycle.TOTAL_JUZ,
        ).also { activeCycle = it }
    }

    private fun maybeFail() {
        if (failNextMutation) {
            failNextMutation = false
            throw IOException("fake khatam failure")
        }
    }
}
