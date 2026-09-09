package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.feature.upload.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Recreates the feature/persistence against real Android files, using isolated synthetic drafts. */
class UploadDraftDeviceTest {
    @Test fun recreatedFeatureRestoresDataAndNeverReplaysAnInterruptedUpload() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "beta7-upload-draft-${UUID.randomUUID()}").canonicalFile
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val persistence = StoredUploadDrafts(UploadDraftStore(root), scope)
        var writes = 0
        val repository = object : UploadRepository {
            override suspend fun document(uri: String) = error("unused")
            override suspend fun parse(document: UploadDocument) = error("unused")
            override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes++; return UploadActionResult(true, novelId = 900012) }
        }
        var model: UploadBookViewModel? = null
        try {
            val ready = UploadBookState(existingNovelId = 900012, draft = UploadBookDraft(title = "受控可恢复上传", author = "合成作者", spans = "已完结", chapterCount = 1),
                selectedFile = UploadDocument("content://synthetic/book.epub", "book.epub", 80), serverFilePath = "synthetic/owned.epub",
                chapters = LoadResult.Success(listOf(UploadChapter("首章", "恢复段落😀", 1, 1, listOf("卷一"), "OPS/chapter.xhtml", 0))))
            persistence.checkpoint(900011, ready.copy(processing = true, submitResult = LoadResult.Loading))
            val recreated = StoredUploadDrafts(UploadDraftStore(root), scope)
            withContext(Dispatchers.Main) {
                model = UploadBookViewModel(repository, scope, recreated, { 900011 })
                model!!.enter(900012)
            }
            withTimeout(5000) { while (model!!.state.restoringDraft) delay(30) }
            withContext(Dispatchers.Main) {
                assertEquals(ready.draft, model!!.state.draft); assertEquals(ready.chapters, model!!.state.chapters)
                assertEquals(ready.selectedFile, model!!.state.selectedFile)
                assertTrue(model!!.state.submissionUncertain); assertFalse(model!!.state.processing)
                model!!.submit()
            }
            assertEquals(0, writes)
            assertNull(recreated.load(900013, 900012)); assertNull(recreated.load(900011, 900014))
            val folder = File(context.cacheDir, "beta7-upload-draft-report").apply { mkdirs() }
            File(folder, "restore.json").writeText(JSONObject().put("passed", true).put("accountAndBookIsolated", true)
                .put("allDraftAndChapterFieldsPreserved", true).put("uncertainRestoredWithoutReplay", true).put("remoteWrites", writes).toString(2))
        } finally {
            withContext(Dispatchers.Main) { model?.close() }
            scope.coroutineContext[Job]?.cancelAndJoin()
            check(root.parentFile == context.cacheDir.canonicalFile && root.name.startsWith("beta7-upload-draft-"))
            root.deleteRecursively()
        }
    }
}
