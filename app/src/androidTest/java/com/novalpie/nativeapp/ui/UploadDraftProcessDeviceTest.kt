package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.feature.upload.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

private object UploadProcessFixture {
    fun caseId(): String = InstrumentationRegistry.getArguments().getString("processCaseId").orEmpty().also {
        require(it.matches(Regex("[a-f0-9]{32}"))) { "Explicit process case ID required" }
    }
    fun root() = File(InstrumentationRegistry.getInstrumentation().targetContext.noBackupFilesDir, "beta7-upload-process-${caseId()}").canonicalFile
    fun ready() = UploadBookState(existingNovelId = 900012,
        draft = UploadBookDraft(title = "进程恢复受控草稿", author = "合成作者", spans = "已完结", chapterCount = 1),
        selectedFile = UploadDocument("content://unused/synthetic.epub", "synthetic.epub", 100), serverFilePath = "synthetic/owned.epub",
        chapters = LoadResult.Success(listOf(UploadChapter("唯一章", "仅测试持久恢复😀", 1, 1, listOf("卷一"), "OPS/a.xhtml", 0))))
    open class Repository : UploadRepository {
        var writes = 0
        override suspend fun document(uri: String) = error("No real files are accessed")
        override suspend fun parse(document: UploadDocument) = error("No real files are parsed")
        override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes++; return UploadActionResult(true, novelId = 900012) }
    }
}

/** Phase 1, followed by an explicit host am force-stop before phase 2. No real upload is made. */
class UploadDraftProcessPrepareTest {
    @Test fun persistWhileTheSyntheticServerReplyIsStillPending() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = UploadProcessFixture.root()
        check(!root.exists()) { "Do not overwrite an earlier process fixture" }
        val container = AppContainer.from(context)
        container.refreshEnvironmentFromStores()
        check(!container.downloads.state.value.busy) { "A real download is active" }
        val account = requireNotNull(container.api.currentUser().id)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val drafts = StoredUploadDrafts(UploadDraftStore(root), scope)
        val entered = CompletableDeferred<Unit>()
        val repository = object : UploadProcessFixture.Repository() {
            override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes++; entered.complete(Unit); awaitCancellation() }
        }
        val model = UploadBookViewModel(repository, scope, drafts, { 900011 })
        try {
            withContext(Dispatchers.Main) { assertTrue(model.adoptFromEditor(UploadProcessFixture.ready())); model.submit() }
            withTimeout(5000) { entered.await() }
            assertTrue(drafts.load(900011, 900012)!!.submissionUncertain)
            File(root, "fixture.json").writeText(JSONObject().put("case", UploadProcessFixture.caseId()).put("pid", android.os.Process.myPid())
                .put("authenticatedAccount", account).put("syntheticSubmits", repository.writes).toString())
        } finally {
            withContext(Dispatchers.Main) { model.close() }
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }
}

/** Phase 2 must be in another actual Android process, not just another ViewModel instance. */
class UploadDraftProcessVerifyTest {
    @Test fun newProcessRestoresTheDraftWithoutReplayingAndKeepsTheLogin() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = UploadProcessFixture.root()
        val before = JSONObject(File(root, "fixture.json").readText())
        assertEquals(UploadProcessFixture.caseId(), before.getString("case"))
        assertNotEquals("Must be a different Android process", before.getInt("pid"), android.os.Process.myPid())
        val container = AppContainer.from(context); container.refreshEnvironmentFromStores()
        val account = requireNotNull(container.api.currentUser().id)
        assertEquals("Existing website login must survive", before.getLong("authenticatedAccount"), account)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val drafts = StoredUploadDrafts(UploadDraftStore(root), scope)
        val repository = UploadProcessFixture.Repository()
        val model = UploadBookViewModel(repository, scope, drafts, { 900011 })
        var passed = false
        try {
            withContext(Dispatchers.Main) { model.enter(900012) }
            withTimeout(5000) { while (model.state.restoringDraft) delay(20) }
            val expected = UploadProcessFixture.ready()
            withContext(Dispatchers.Main) {
                assertEquals(expected.draft, model.state.draft); assertEquals(expected.selectedFile, model.state.selectedFile)
                assertEquals(expected.chapters, model.state.chapters); assertEquals(expected.serverFilePath, model.state.serverFilePath)
                assertTrue(model.state.submissionUncertain); assertFalse(model.state.processing)
                model.submit()
            }
            assertEquals(0, repository.writes)
            assertNull(drafts.load(900014, 900012))
            val evidence = File(context.cacheDir, "beta7-upload-process-report").apply { mkdirs() }
            File(evidence, "process.json").writeText(JSONObject().put("passed", true).put("case", UploadProcessFixture.caseId())
                .put("previousPid", before.getInt("pid")).put("currentPid", android.os.Process.myPid())
                .put("loginReadVerifiedUnchanged", true).put("draftAndChaptersPreserved", true).put("remoteUploadWrites", 0)
                .put("ordinarySubmitDidNotReplay", true).toString(2))
            passed = true
        } finally {
            withContext(Dispatchers.Main) { model.close() }
            scope.coroutineContext[Job]?.cancelAndJoin()
            if (passed) {
                check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name == "beta7-upload-process-${UploadProcessFixture.caseId()}")
                root.deleteRecursively()
            }
        }
    }
}
