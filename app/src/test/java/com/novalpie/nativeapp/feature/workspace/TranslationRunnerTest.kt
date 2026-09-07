package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28])
class TranslationRunnerTest {
    @get:Rule val temp = TemporaryFolder()
    private val config = WorkspaceLocalApiConfig(1, "synthetic", "fixture", "https://fixture.test", "test-only", 3)
    private open class Source : TranslationSource {
        val submissions = mutableListOf<String>()
        override suspend fun chapters(bookId: Long) = listOf(TranslationChapter(10, "标题", 1, "pending"))
        override suspend fun prepare(bookId: Long, chapterId: Long) = TranslationPreparation(bookId, chapterId, "标题", listOf(TranslationChunk(0, "一", emptyMap()), TranslationChunk(1, "二", emptyMap())))
        override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long): Boolean { submissions += content; return true }
    }
    @Test fun outOfOrderChunkResponsesStillSubmitExactlyOneCompleteChapter() = runBlocking {
        val source = Source(); val store = TranslationTaskStore(temp.newFolder())
        val runner = TranslationRunner(source, object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>): TranslationResult {
                if (source == "一") delay(20)
                return TranslationResult("译$source")
            }
        }, store)
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试书", configId = 1)
        val result = runner.run(task, config, {}, {})
        assertEquals(TranslationPhase.Completed, result.phase)
        assertEquals(listOf("译一\n译二"), source.submissions)
        assertEquals(setOf(10L), result.completed)
    }
    @Test fun incompleteChunkNeverSubmitsAndCompletedChunksAreReusedOnExplicitRetry() = runBlocking {
        val source = Source(); val store = TranslationTaskStore(temp.newFolder())
        var failing = true; var firstCalls = 0
        val runner = TranslationRunner(source, object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>): TranslationResult {
                if (source == "一") firstCalls++
                if (source == "二" && failing) { delay(30); error("synthetic failure") }
                return TranslationResult("译$source")
            }
        }, store)
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        assertTrue(runCatching { runner.run(task, config, {}, {}) }.isFailure)
        assertTrue(source.submissions.isEmpty())
        failing = false; runner.run(task, config, {}, {})
        assertEquals(1, firstCalls)
        assertEquals(1, source.submissions.size)
    }
    @Test fun uncertainSubmissionDoesNotAutomaticallyReplayEvenWhenUserRetries() = runBlocking {
        val source = Source(); val store = TranslationTaskStore(temp.newFolder())
        val runner = TranslationRunner(source, object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>) = error("must not translate")
        }, store)
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1, phase = TranslationPhase.SubmissionUncertain, currentChapterId = 10)
        val result = runner.run(task, config, {}, {})
        assertEquals(TranslationPhase.SubmissionUncertain, result.phase)
        assertTrue(source.submissions.isEmpty())
    }
    @Test fun missingUntranslatedCandidateIsNotEvidenceThatAnUncertainSubmissionSucceeded() = runBlocking {
        val source = object : Source() {
            override suspend fun chapters(bookId: Long) = emptyList<TranslationChapter>()
        }
        val store = TranslationTaskStore(temp.newFolder())
        val runner = TranslationRunner(source, object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>) = error("must not translate")
        }, store)
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1,
            phase = TranslationPhase.SubmissionUncertain, currentChapterId = 10)
        val result = runner.run(task, config, {}, {})
        assertEquals(TranslationPhase.SubmissionUncertain, result.phase)
        assertTrue(result.completed.isEmpty())
        assertTrue(source.submissions.isEmpty())
    }
    @Test fun storeRecoveryNeverTurnsAnInterruptedSubmissionIntoAReadyTask() {
        val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1, phase = TranslationPhase.Submitting, currentChapterId = 10)
        store.save(task)
        assertEquals(TranslationPhase.SubmissionUncertain, store.load(1).single().phase)
        assertTrue(store.load(2).isEmpty())
    }
}
