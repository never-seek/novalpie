package com.novalpie.nativeapp.feature.reader.progress

import com.novalpie.nativeapp.model.ForumActionResult
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingProgressSynchronizerTest {
    @Test fun aPendingNewPositionWaitsForTheOldWriteAndCoalescesByBook() = runTest {
        val release = CompletableDeferred<Unit>()
        val writes = mutableListOf<Pair<Long, Long>>()
        val sync = ReadingProgressSynchronizer(backgroundScope, { 1L }) { book, chapter ->
            writes += book to chapter
            if (chapter == 10L) release.await()
            ForumActionResult(true)
        }
        sync.request(1, 10); runCurrent()
        sync.request(1, 11); sync.request(1, 12); sync.request(2, 20); runCurrent()
        assertEquals(listOf(1L to 10L), writes)
        release.complete(Unit); runCurrent()
        assertEquals(listOf(1L to 10L, 1L to 12L, 2L to 20L), writes)
        sync.request(1, 12); runCurrent()
        assertEquals(3, writes.size)
    }

    @Test fun failedProgressIsNeverAutomaticallyReplayedByEveryUtterance() = runTest {
        var calls = 0
        val sync = ReadingProgressSynchronizer(backgroundScope, { 1L }) { _, _ -> calls++; ForumActionResult(false, "合成拒绝") }
        sync.request(1, 10); runCurrent()
        repeat(20) { sync.request(1, 10) }; runCurrent()
        assertEquals(1, calls)
        assertEquals(10L, sync.failure.value?.chapterId)
    }

    @Test fun staleAccountRequestAndFailureCannotAffectTheNewAccount() = runTest {
        var account = 1L
        val old = CompletableDeferred<Unit>()
        val calls = mutableListOf<Long>()
        val sync = ReadingProgressSynchronizer(backgroundScope, { account }) { _, chapter ->
            calls += chapter
            if (chapter == 10L) { old.await(); throw CancellationException("old account") }
            ForumActionResult(true)
        }
        sync.request(1, 10); runCurrent(); sync.request(1, 11)
        account = 2; sync.request(2, 20); old.complete(Unit); runCurrent()
        assertEquals(listOf(10L, 20L), calls)
        assertNull(sync.failure.value)
    }
}
