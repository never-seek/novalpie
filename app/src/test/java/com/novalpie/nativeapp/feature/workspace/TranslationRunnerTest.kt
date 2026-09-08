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

    @Test fun retryKeepsTheOriginalChapterSetInsteadOfTranslatingNewArrivals() = runBlocking {
        var retry = false
        val preparedIds = mutableListOf<Long>()
        val source = object : Source() {
            override suspend fun chapters(bookId: Long) = (if (retry) listOf(11L, 12L) else listOf(10L, 11L))
                .map { TranslationChapter(it, "章节$it", it.toInt(), "pending") }
            override suspend fun prepare(bookId: Long, chapterId: Long): TranslationPreparation {
                preparedIds += chapterId
                if (!retry && chapterId == 11L) error("准备结果暂时不可用")
                return super.prepare(bookId, chapterId)
            }
        }
        val store = TranslationTaskStore(temp.newFolder())
        val runner = TranslationRunner(source, identityModel(), store)
        var latest = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        assertTrue(runCatching { runner.run(latest, config, {}) { latest = it; store.save(it) } }.isFailure)
        retry = true
        val restored = store.load(1).single().copy(phase = TranslationPhase.Queued)
        val result = runner.run(restored, config, {}) { latest = it; store.save(it) }
        assertEquals(TranslationPhase.Completed, result.phase)
        assertEquals(setOf(10L, 11L), result.completed)
        assertEquals(2, result.total)
        assertFalse("后来新增章节未经本任务确认", 12L in preparedIds)
    }

    @Test fun disappearedPendingChapterMustNotBecomeAFalseCompletedTask() = runBlocking {
        var retry = false
        val source = object : Source() {
            override suspend fun chapters(bookId: Long) = if (retry) emptyList() else listOf(10L, 11L)
                .map { TranslationChapter(it, "章节$it", it.toInt(), "pending") }
            override suspend fun prepare(bookId: Long, chapterId: Long): TranslationPreparation {
                if (chapterId == 11L) error("准备结果暂时不可用")
                return super.prepare(bookId, chapterId)
            }
        }
        val store = TranslationTaskStore(temp.newFolder())
        val runner = TranslationRunner(source, identityModel(), store)
        var latest = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        assertTrue(runCatching { runner.run(latest, config, {}) { latest = it; store.save(it) } }.isFailure)
        retry = true
        val result = runner.run(store.load(1).single().copy(phase = TranslationPhase.Queued), config, {}) {}
        assertEquals(TranslationPhase.Failed, result.phase)
        assertEquals(setOf(10L), result.completed)
        assertEquals(2, result.total)
        assertTrue(result.message.orEmpty().contains("未确认"))
        assertEquals(1, source.submissions.size)
    }

    @Test fun corruptedCompletedChunkIsNeverSubmittedOrSilentlyBilledAgain() = runBlocking {
        val source = Source(); val store = TranslationTaskStore(temp.newFolder())
        var failing = true; var firstCalls = 0
        val model = object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>): TranslationResult {
                if (source == "一") firstCalls++
                if (source == "二" && failing) { delay(100); error("翻译服务中断") }
                return TranslationResult("译$source")
            }
        }
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        val runner = TranslationRunner(source, model, store)
        assertTrue(runCatching { runner.run(task, config, {}, {}) }.isFailure)
        val saved = requireNotNull(store.checkpoint(task, 10, "chunk-0"))
        store.checkpoint(task, 10, "chunk-0", saved.put("text", "被损坏但JSON仍可读取的文字"))
        failing = false
        val failure = runCatching { runner.run(task, config, {}, {}) }.exceptionOrNull()
        assertNotNull("不能将损坏的检查点提交", failure)
        assertTrue(failure?.message.orEmpty().contains("检查点"))
        assertTrue(source.submissions.isEmpty())
        assertEquals(1, firstCalls)
    }

    private fun identityModel() = object : TranslationModel {
        override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>) = TranslationResult(source)
    }

    @Test fun legacyPartiallyCompletedTaskCannotGuessItsMissingOriginalScope() = runBlocking {
        val source = Source(); val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "旧任务", configId = 1,
            phase = TranslationPhase.Paused, completed = setOf(9), total = 4, currentChapterId = 10)
        store.save(task)
        val restored = store.load(1).single()
        val result = TranslationRunner(source, identityModel(), store).run(restored, config, {}, {})
        assertEquals(TranslationPhase.Failed, result.phase)
        assertTrue(result.message.orEmpty().contains("范围"))
        assertEquals(setOf(9L), result.completed)
        assertEquals(4, result.total)
        assertTrue(source.submissions.isEmpty())
    }

    @Test fun titleCheckpointCorruptionCannotChangeAReplayedSubmission() = runBlocking {
        val source = object : Source() {
            var reject = true
            override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long): Boolean {
                if (reject) return false
                return super.submit(bookId, chapterId, content, title, result, model, elapsedMs)
            }
        }
        val store = TranslationTaskStore(temp.newFolder())
        var calls = 0
        val model = object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>): TranslationResult { calls++; return TranslationResult(source) }
        }
        val runner = TranslationRunner(source, model, store)
        val task = TranslationTask(accountId = 1, bookId = 2, title = "测试", configId = 1)
        val first = runner.run(task, config, {}, {})
        assertEquals(TranslationPhase.Failed, first.phase)
        val saved = requireNotNull(store.checkpoint(task, 10, "title"))
        store.checkpoint(task, 10, "title", saved.put("text", "错误标题"))
        source.reject = false
        val failure = runCatching { runner.run(first.copy(phase = TranslationPhase.Queued), config, {}, {}) }.exceptionOrNull()
        assertTrue(failure?.message.orEmpty().contains("检查点"))
        assertEquals(3, calls)
        assertTrue(source.submissions.isEmpty())
    }

    @Test fun olderTaskRecordWithoutTargetsLoadsWithoutLosingItsExistingProgress() {
        val store = TranslationTaskStore(temp.newFolder())
        val task = TranslationTask(accountId = 1, bookId = 2, title = "旧版", configId = 1, phase = TranslationPhase.Failed, completed = setOf(10), total = 2)
        store.save(task)
        val file = java.io.File(store.directory(task.id), "task.json")
        val json = org.json.JSONObject(file.readText()).put("schema", 1).apply { remove("targets") }
        file.writeText(json.toString())
        val restored = store.load(1).single()
        assertEquals(task.completed, restored.completed)
        assertEquals(2, restored.total)
        assertNull(restored.targets)
        assertEquals(TranslationPhase.Failed, restored.phase)
    }
}
