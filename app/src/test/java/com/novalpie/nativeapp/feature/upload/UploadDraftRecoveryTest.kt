package com.novalpie.nativeapp.feature.upload

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class UploadDraftRecoveryTest {
    private class Repository : UploadRepository {
        var writes = 0
        override suspend fun document(uri: String) = error("unused")
        override suspend fun parse(document: UploadDocument) = error("unused")
        override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes++; return UploadActionResult(true, novelId = 10) }
    }
    private open class Drafts(private val scope: CoroutineScope) : UploadDraftPersistence {
        val stored = mutableMapOf<Pair<Long,Long?>, UploadBookState>()
        var fail = false
        override suspend fun load(accountId: Long, bookId: Long?) = stored[accountId to bookId]
        override suspend fun checkpoint(accountId: Long, state: UploadBookState) {
            if (fail) throw java.io.IOException("synthetic checkpoint failure")
            stored[accountId to state.existingNovelId] = state.copy(processing = false,
                submissionUncertain = state.submissionUncertain || state.submitResult == LoadResult.Loading)
        }
        override fun schedule(accountId: Long, state: UploadBookState, onFailure: () -> Unit) = scope.launch {
            try { checkpoint(accountId, state) } catch (_: Exception) { onFailure() }
        }
    }
    private fun prepared() = UploadBookState(existingNovelId = 10, draft = UploadBookDraft(title = "需要保留的书名", author = "作者", chapterCount = 1),
        selectedFile = UploadDocument("content://synthetic/book.epub", "book.epub", 100), serverFilePath = "owned/book.epub",
        chapters = LoadResult.Success(listOf(UploadChapter("第一章", "正文", 1))))

    @Test fun recreationRestoresTheCorrectAccountsDraftWithoutAnyAutomaticUpload() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val drafts = Drafts(scope); val repository = Repository()
        val first = UploadBookViewModel(repository, scope, drafts, { 1 })
        val second = UploadBookViewModel(repository, scope, drafts, { 1 })
        val other = UploadBookViewModel(repository, scope, drafts, { 2 })
        try {
            first.adopt(prepared()); first.close()
            second.enter(10); other.enter(10)
            assertEquals("需要保留的书名", second.state.draft.title)
            assertEquals("owned/book.epub", second.state.serverFilePath)
            assertEquals("正文", (second.state.chapters as LoadResult.Success).value.single().content)
            assertEquals("", other.state.draft.title)
            assertEquals(0, repository.writes)
        } finally { first.close(); second.close(); other.close(); scope.cancel() }
    }
    @Test fun failedPreSubmissionCheckpointMustPreventAllNetworkWrites() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val drafts = Drafts(scope); val repository = Repository()
        val model = UploadBookViewModel(repository, scope, drafts, { 1 })
        try {
            model.adopt(prepared()); drafts.fail = true; model.submit()
            assertEquals("未成功保存提交身份前不能上传", 0, repository.writes)
            assertEquals("需要保留的书名", model.state.draft.title)
            assertFalse(model.state.processing)
        } finally { model.close(); scope.cancel() }
    }

    @Test fun restoredUncertainRequestCannotBeSentByOrdinarySubmit() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val drafts = Drafts(scope); val repository = Repository()
        drafts.checkpoint(1, prepared().copy(processing = true, submitResult = LoadResult.Loading))
        val model = UploadBookViewModel(repository, scope, drafts, { 1 })
        try {
            model.enter(10); model.submit()
            assertTrue(model.state.submissionUncertain); assertEquals(0, repository.writes)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun unreadableSavedDraftIsNotOverwrittenByAnEmptyDefault() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val drafts = object : Drafts(scope) { override suspend fun load(accountId: Long, bookId: Long?): UploadBookState? = error("synthetic corrupt metadata") }
        val repository = Repository(); val model = UploadBookViewModel(repository, scope, drafts, { 1 })
        try {
            model.enter(10); model.draft(prepared().draft); model.submit()
            assertNotNull(model.state.draftStorageError)
            assertFalse(model.adopt(prepared()))
            assertTrue(drafts.stored.isEmpty()); assertEquals(0, repository.writes)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun editorTransferWaitsForInitialDiskRecoveryWithoutDroppingItsPreparedBook() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<UploadBookState?>()
        val drafts = object : Drafts(scope) { override suspend fun load(accountId: Long, bookId: Long?) = gate.await() }
        val model = UploadBookViewModel(Repository(), scope, drafts, { 1 })
        try {
            val transfer = async { model.adoptFromEditor(prepared()) }
            yield(); assertFalse(transfer.isCompleted)
            gate.complete(null)
            assertTrue(transfer.await())
            assertEquals("需要保留的书名", model.state.draft.title)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun cancelledEditorTransferMustNotOverwriteARecoveredDraftAfterTheDiskReadFinishes() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<UploadBookState?>()
        val drafts = object : Drafts(scope) { override suspend fun load(accountId: Long, bookId: Long?) = gate.await() }
        val model = UploadBookViewModel(Repository(), scope, drafts, { 1 })
        var requested = true
        try {
            val transfer = async { model.adoptFromEditor(prepared()) { requested } }
            yield(); requested = false
            gate.complete(prepared().copy(draft = prepared().draft.copy(title = "原来保存的内容")))
            assertFalse(transfer.await()); assertEquals("原来保存的内容", model.state.draft.title)
        } finally { model.close(); scope.cancel() }
    }
}
