package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
// Android 15 AtomicFile's native replace-rename cannot overwrite on Windows JVMs. API 28's
// backup-based atomic algorithm works here; Android 15 is separately verified on MuMu.
@org.robolectric.annotation.Config(sdk = [28])
class TranslationCoordinatorTest {
    @Test fun recoveredQueuedTaskIsPausedUntilTheUserExplicitlyContinues() {
        val store = TranslationTaskStore(temp.newFolder())
        store.save(TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1))
        assertEquals(TranslationPhase.Paused, store.load(1).single().phase)
    }
    @get:Rule val temp = TemporaryFolder()
    private val config = WorkspaceLocalApiConfig(1, "synthetic", "fixture", "https://fixture.test", "test-only", 1)
    private val unusedSource = object : TranslationSource {
        override suspend fun chapters(bookId: Long) = error("must not request")
        override suspend fun prepare(bookId: Long, chapterId: Long) = error("must not prepare")
        override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long) = error("must not submit")
    }
    private val unusedModel = object : TranslationModel {
        override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>) = error("must not translate")
    }
    @Test fun stoppingAnAlreadyUncertainTaskDoesNotHideTheUnconfirmedWrite() = runBlocking {
        val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1,
            phase = TranslationPhase.SubmissionUncertain, currentChapterId = 10)
        store.save(task)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val queue = TranslationCoordinator(scope, store, TranslationRunner(unusedSource, unusedModel, store), { 1L }, { config })
            queue.restore(); queue.cancel(task.id)
            delay(100)
            assertEquals(TranslationPhase.SubmissionUncertain, queue.state.value.tasks.single().phase)
            assertEquals("disk=" + java.io.File(store.directory(task.id), "task.json").readText(), TranslationPhase.SubmissionUncertain, store.load(1).single().phase)
        } finally { scope.cancel() }
    }
    @Test fun configRemovedAfterConfirmationProducesAFailedRecordInsteadOfThrowingFromService() = runBlocking {
        val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val queue = TranslationCoordinator(scope, store, TranslationRunner(unusedSource, unusedModel, store), { 1L }, { null })
            queue.restore()
            assertTrue(runCatching { queue.enqueue(task) }.isSuccess)
            withTimeout(3000) { queue.state.first { it.tasks.any { item -> item.phase == TranslationPhase.Failed } } }
            assertEquals(TranslationPhase.Failed, store.load(1).single().phase)
        } finally { scope.cancel() }
    }
    @Test fun repeatedResumeCannotDowngradeAnUncertainSubmissionIntoARetryableFailure() = runBlocking {
        val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1, phase = TranslationPhase.SubmissionUncertain, currentChapterId = 10)
        store.save(task)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val source = object : TranslationSource {
                override suspend fun chapters(bookId: Long) = error("must not automatically retry")
                override suspend fun prepare(bookId: Long, chapterId: Long) = error("unused")
                override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long) = error("must not submit")
            }
            val model = object : TranslationModel { override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>) = error("unused") }
            val queue = TranslationCoordinator(scope, store, TranslationRunner(source, model, store), { 1L }, { config })
            queue.restore(); queue.resume(task.id); queue.resume(task.id)
            assertEquals(TranslationPhase.SubmissionUncertain, queue.state.value.tasks.single().phase)
            assertNull(queue.state.value.activeId)
        } finally { scope.cancel() }
    }
    @Test fun accountChangeDuringSubmitKeepsOldCheckpointUncertainAndNeverLeaksIntoNewQueue() = runBlocking {
        val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        var accountId = 1L
        val submitting = CompletableDeferred<Unit>()
        val source = object : TranslationSource {
            override suspend fun chapters(bookId: Long) = listOf(TranslationChapter(10, "标题", 1, "pending"))
            override suspend fun prepare(bookId: Long, chapterId: Long) = TranslationPreparation(bookId, chapterId, "标题", listOf(TranslationChunk(0, "原文", emptyMap())))
            override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long): Boolean {
                submitting.complete(Unit); awaitCancellation()
            }
        }
        val model = object : TranslationModel { override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>) = TranslationResult("译$source") }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val queue = TranslationCoordinator(scope, store, TranslationRunner(source, model, store), { accountId }, { config })
            queue.enqueue(task).join()
            withTimeout(4000) { submitting.await() }
            accountId = 3
            queue.environmentChanged().join(); queue.restore()
            delay(100)
            assertTrue(queue.state.value.tasks.isEmpty())
            assertNull(queue.state.value.activeId)
            assertEquals("queue=" + queue.state.value + "; disk=" + java.io.File(store.directory(task.id), "task.json").readText(), TranslationPhase.SubmissionUncertain, store.load(1).single().phase)
            assertTrue(store.load(3).isEmpty())
        } finally { scope.cancel() }
    }
    @Test fun pauseFinishesInFlightChunkButDoesNotDispatchNextOrSubmitUntilResumed() = runBlocking {
        val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val calls = java.util.concurrent.CopyOnWriteArrayList<String>()
        var submitted = 0
        val source = object : TranslationSource {
            override suspend fun chapters(bookId: Long) = listOf(TranslationChapter(10, "标题", 1, "pending"))
            override suspend fun prepare(bookId: Long, chapterId: Long) = TranslationPreparation(bookId, chapterId, "标题", listOf(TranslationChunk(0, "一", emptyMap()), TranslationChunk(1, "二", emptyMap())))
            override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long): Boolean { submitted++; return true }
        }
        val model = object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>): TranslationResult {
                calls += source
                if (source == "一") { entered.complete(Unit); release.await() }
                return TranslationResult("译$source")
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val queue = TranslationCoordinator(scope, store, TranslationRunner(source, model, store), { 1L }, { config })
            queue.enqueue(task).join(); withTimeout(4000) { entered.await() }
            queue.pause(task.id).join(); release.complete(Unit)
            withTimeout(4000) { queue.state.first { it.tasks.single().finishedChunks == 1 } }
            delay(80)
            assertEquals(listOf("一"), calls)
            assertEquals(0, submitted)
            assertNotNull(store.checkpoint(task, 10, "chunk-0"))
            queue.resume(task.id).join()
            withTimeout(4000) { queue.state.first { it.tasks.single().phase == TranslationPhase.Completed } }
            assertEquals(1, submitted)
            assertEquals("queue=" + queue.state.value + "; disk=" + java.io.File(store.directory(task.id), "task.json").readText(), TranslationPhase.Completed, store.load(1).single().phase)
        } finally { scope.cancel() }
    }
    @Test fun multipleEnqueuesAndRepeatedResumeNeverRunOneBookTwice() = runBlocking {
        val store = TranslationTaskStore(temp.newFolder())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val submitted = java.util.concurrent.CopyOnWriteArrayList<Long>()
        val source = object : TranslationSource {
            override suspend fun chapters(bookId: Long): List<TranslationChapter> {
                if (bookId == 2L) { entered.complete(Unit); release.await() }
                return listOf(TranslationChapter(10, "标题", 1, "pending"))
            }
            override suspend fun prepare(bookId: Long, chapterId: Long) = TranslationPreparation(bookId, chapterId, "标题", listOf(TranslationChunk(0, "原文", emptyMap())))
            override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long): Boolean { submitted += bookId; return true }
        }
        val model = object : TranslationModel { override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>) = TranslationResult("译$source") }
        try {
            val queue = TranslationCoordinator(scope, store, TranslationRunner(source, model, store), { 1L }, { config })
            val first = TranslationTask(accountId = 1, bookId = 2, title = "一", configId = 1)
            queue.enqueue(first).join(); withTimeout(4000) { entered.await() }
            queue.enqueue(first.copy(id = "duplicate")).join()
            queue.enqueue(first.copy(id = "second", bookId = 3)).join()
            queue.resume(first.id).join(); release.complete(Unit)
            withTimeout(4000) { queue.state.first { it.tasks.size == 2 && it.tasks.all { task -> task.phase == TranslationPhase.Completed } } }
            assertEquals(listOf(2L, 3L), submitted)
            assertEquals(2, store.load(1).size)
        } finally { scope.cancel() }
    }
}
