package com.novalpie.nativeapp.ui

import android.app.NotificationManager
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.audit.TranslationTestService
import com.novalpie.nativeapp.feature.workspace.*
import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/** Synthetic source/model only; actual Android 15 FGS, notification and durable file IO. */
class TranslationQueueDeviceTest {
    @Test fun backgroundPauseResumeAndInterruptedSubmitKeepRealCheckpoints() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val root = File(context.cacheDir, "beta7-translation-device-${UUID.randomUUID()}").canonicalFile
        check(root.parentFile == context.cacheDir.canonicalFile)
        val store = TranslationTaskStore(root)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val task = TranslationTask(accountId = 900011, bookId = 900012, title = "Beta7受控翻译测试", configId = 900013)
        val config = WorkspaceLocalApiConfig(900013, "受控模型", "synthetic", "https://unused.invalid", "synthetic-only", 1)
        val firstStarted = CompletableDeferred<Unit>(); val firstRelease = CompletableDeferred<Unit>()
        val submitStarted = CompletableDeferred<Unit>()
        val calls = CopyOnWriteArrayList<String>()
        var submits = 0
        var interruptSubmit = false
        val source = object : TranslationSource {
            override suspend fun chapters(bookId: Long) = listOf(TranslationChapter(900014, "合成标题", 1, "pending"))
            override suspend fun prepare(bookId: Long, chapterId: Long) = TranslationPreparation(bookId, chapterId, "合成标题",
                listOf(TranslationChunk(0, "第一块\n\n[[img:1]]", emptyMap()), TranslationChunk(1, "第二块", emptyMap())))
            override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long): Boolean {
                submits++
                assertTrue(content.contains("\n\n[[img:1]]"))
                if (interruptSubmit) { submitStarted.complete(Unit); awaitCancellation() }
                return true
            }
        }
        val model = object : TranslationModel {
            override suspend fun translate(config: WorkspaceLocalApiConfig, source: String, glossary: Map<String, String>): TranslationResult {
                calls += source
                if (source.startsWith("第一块")) { firstStarted.complete(Unit); firstRelease.await() }
                return TranslationResult(source.replace("块", "块译文"))
            }
        }
        val queue = TranslationCoordinator(scope, store, TranslationRunner(source, model, store), { task.accountId }, { config })
        val notifications = context.getSystemService(NotificationManager::class.java)
        var scenario = ActivityScenario.launch(ReaderTestActivity::class.java)
        fun start(value: TranslationTask) {
            TranslationTestService.pending = value
            scenario.onActivity { ContextCompat.startForegroundService(it, Intent(it, TranslationTestService::class.java).setAction("novalpie.translation.START")) }
        }
        try {
            TranslationTestService.queue = queue
            start(task)
            withTimeout(15000) { firstStarted.await() }
            withTimeout(5000) { while (notifications.activeNotifications.none { it.id == 7103 && it.notification.actions?.isNotEmpty() == true }) delay(50) }
            instrumentation.uiAutomation.executeShellCommand("input keyevent 3").close()
            notifications.activeNotifications.first { it.id == 7103 }.notification.actions[0].actionIntent.send()
            withTimeout(5000) { queue.state.first { it.paused } }
            firstRelease.complete(Unit)
            withTimeout(5000) { queue.state.first { it.tasks.single().finishedChunks == 1 } }
            delay(400)
            assertEquals(1, calls.size); assertEquals(0, submits)
            assertNotNull(store.checkpoint(task, 900014, "chunk-0"))
            withTimeout(5000) { while (notifications.activeNotifications.first { it.id == 7103 }.notification.actions[0].title.toString() != "继续") delay(50) }
            notifications.activeNotifications.first { it.id == 7103 }.notification.actions[0].actionIntent.send()
            withTimeout(15000) { queue.state.first { it.tasks.single().phase == TranslationPhase.Completed && it.activeId == null } }
            assertEquals(1, submits)
            assertEquals(TranslationPhase.Completed, store.load(task.accountId).single().phase)
            withTimeout(5000) { while (TranslationTestService.alive) delay(50) }

            // A second controlled task tests service destruction exactly while a server write is in flight.
            interruptSubmit = true
            val second = task.copy(id = UUID.randomUUID().toString(), bookId = 900015)
            // moveToState(RESUMED) cannot bring a task back above the Android launcher after HOME.
            scenario.close()
            scenario = ActivityScenario.launch(ReaderTestActivity::class.java)
            start(second)
            withTimeout(15000) { submitStarted.await() }
            context.stopService(Intent(context, TranslationTestService::class.java))
            withTimeout(5000) { queue.state.first { it.tasks.any { item -> item.id == second.id && item.phase == TranslationPhase.SubmissionUncertain } && it.activeId == null } }
            assertEquals(TranslationPhase.SubmissionUncertain, store.load(task.accountId).first { it.id == second.id }.phase)
            queue.resume(second.id).join()
            assertEquals(2, submits)
            assertEquals(TranslationPhase.SubmissionUncertain, queue.state.value.tasks.first { it.id == second.id }.phase)
        } finally {
            context.stopService(Intent(context, TranslationTestService::class.java))
            queue.environmentChanged().join()
            withTimeout(5000) { while (TranslationTestService.alive) delay(50) }
            scope.cancel()
            scenario.close()
            TranslationTestService.pending = null; TranslationTestService.queue = null
            notifications.cancel(7103)
            check(root.parentFile == context.cacheDir.canonicalFile && root.name.startsWith("beta7-translation-device-"))
            root.deleteRecursively() // This test's synthetic files only, never the user's queue.
        }
    }
}
