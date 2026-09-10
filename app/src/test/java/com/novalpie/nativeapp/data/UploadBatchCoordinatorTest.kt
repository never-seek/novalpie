package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class UploadBatchCoordinatorTest {
    @Test fun checkpointsBracketEachWriteAndBindLaterBatchesToTheCreatedBook() = runBlocking {
        val events = mutableListOf<String>()
        val result = runUploadBatches("frozen", 3, null, null, { events += "${it.nextBatch}:${it.inFlight}:${it.novelId}" }) { index, id ->
            assertEquals(if (index == 0) null else 42L, id); events += "POST$index"; UploadActionResult(true, novelId = 42)
        }
        assertEquals(42L, result.novelId)
        assertEquals(listOf("0:true:null", "POST0", "1:false:42", "1:true:42", "POST1", "2:false:42", "2:true:42", "POST2", "3:false:42"), events)
    }
    @Test fun rejectedBatchCanResumeWithoutRecreatingOrReappendingConfirmedChapters() = runBlocking {
        val failure = runCatching { runUploadBatches("frozen", 3, null, null, {}) { index, _ ->
            UploadActionResult(index == 0, "rejected", 42)
        } }.exceptionOrNull() as UploadBatchFailure
        assertFalse(failure.checkpoint.inFlight); assertEquals(1, failure.checkpoint.nextBatch)
        val sent = mutableListOf<Int>()
        runUploadBatches("frozen", 3, null, failure.checkpoint, {}) { index, id -> sent += index; assertEquals(42L, id); UploadActionResult(true) }
        assertEquals(listOf(1, 2), sent)
    }
    @Test fun unknownResultBlocksReplayEvenIfUserClicksRetry() = runBlocking {
        val failure = runCatching { runUploadBatches("frozen", 2, null, null, {}) { _, _ -> throw IOException("timeout") } }.exceptionOrNull() as UploadBatchFailure
        assertTrue(failure.checkpoint.inFlight)
        var writes = 0
        assertTrue(runCatching { runUploadBatches("frozen", 2, null, failure.checkpoint, {}) { _, _ -> writes++; UploadActionResult(true, novelId = 42) } }.isFailure)
        assertEquals(0, writes)
    }
    @Test fun storageFailureBeforeRequestCannotWriteAndFailureAfterAckCannotSkipUnstoredBoundary() = runBlocking {
        var writes = 0
        val before = runCatching { runUploadBatches("frozen", 2, null, null, { throw IOException("full") }) { _, _ -> writes++; UploadActionResult(true, novelId = 42) } }.exceptionOrNull() as UploadBatchFailure
        assertEquals(0, writes); assertFalse(before.checkpoint.inFlight)
        val after = runCatching { runUploadBatches("frozen", 2, null, null, { if (!it.inFlight) throw IOException("full") }) { _, _ -> writes++; UploadActionResult(true, novelId = 42) } }.exceptionOrNull() as UploadBatchFailure
        assertEquals(1, writes); assertTrue(after.checkpoint.inFlight); assertEquals(0, after.checkpoint.nextBatch)
    }
    @Test fun changedPayloadOrWrongBookAcknowledgementNeverAdvancesAnotherBatch() = runBlocking {
        var writes = 0
        assertTrue(runCatching { runUploadBatches("new", 2, 42, UploadBatchCheckpoint("old",42,1,2,false), {}) { _, _ -> writes++; UploadActionResult(true,novelId=42) } }.isFailure)
        assertEquals(0, writes)
        val failed = runCatching { runUploadBatches("new", 2, 42, null, {}) { _, _ -> writes++; UploadActionResult(true,novelId=99) } }.exceptionOrNull() as UploadBatchFailure
        assertEquals(1, writes); assertTrue(failed.checkpoint.inFlight); assertEquals(42L, failed.checkpoint.novelId)
    }
}
