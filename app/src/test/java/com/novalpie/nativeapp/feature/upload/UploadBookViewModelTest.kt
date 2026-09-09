package com.novalpie.nativeapp.feature.upload

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class UploadBookViewModelTest {
    @Test fun repeatedClickAfterAConfirmedCompletionCannotAppendTheSameChaptersTwice() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repository = Repository(); val model = UploadBookViewModel(repository, scope)
        try {
            model.enter(10); model.select("first"); model.submit(); model.submit()
            assertTrue(model.state.submitResult is LoadResult.Success)
            assertEquals(1, repository.writes.size)
        } finally { model.close(); scope.cancel() }
    }
    private open class Repository : UploadRepository {
        val writes = mutableListOf<UploadSubmission>()
        override suspend fun document(uri: String) = UploadDocument(uri, "合成.epub", 100)
        override suspend fun parse(document: UploadDocument) = ParsedEpub("合成书", "作者", chapters = listOf(UploadChapter("一", "正文", 1)), epubFilePath = "owned/${document.uri}")
        override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes += submission; return UploadActionResult(true, novelId = submission.existingBookId ?: 99) }
    }
    @Test fun lateAppendWriteNeverOverwritesTheNextBookAndPayloadIsFrozen() = runBlocking {
        val response = CompletableDeferred<UploadActionResult>()
        val repository = object : Repository() {
            override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes += submission; return response.await() }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = UploadBookViewModel(repository, scope)
        try {
            model.enter(10); model.select("first"); model.submit()
            model.enter(20); model.select("second"); model.draft(model.state.draft.copy(title = "第二本草稿"))
            response.complete(UploadActionResult(true, novelId = 10))
            assertEquals(20L, model.state.existingNovelId)
            assertEquals("第二本草稿", model.state.draft.title)
            assertEquals(LoadResult.Idle, model.state.submitResult)
            assertEquals(10L, repository.writes.single().existingBookId)
            assertEquals("owned/first", repository.writes.single().request.epubFilePath)
            model.enter(10)
            assertTrue(model.state.submitResult is LoadResult.Success)
            assertFalse(model.state.processing)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun lateParseBelongsToOriginalBookAndKeepsMetadataEdits() = runBlocking {
        val parsed = CompletableDeferred<ParsedEpub>()
        val repository = object : Repository() { override suspend fun parse(document: UploadDocument) = parsed.await() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = UploadBookViewModel(repository, scope)
        try {
            model.enter(10); model.select("first"); model.draft(model.state.draft.copy(title = "自己的书名"))
            model.enter(20)
            parsed.complete(ParsedEpub("导入名", "作者", chapters = listOf(UploadChapter("一", "正文", 1))))
            assertEquals(20L, model.state.existingNovelId); assertEquals(LoadResult.Idle, model.state.chapters)
            model.enter(10); assertEquals("自己的书名", model.state.draft.title)
            assertTrue(model.state.chapters is LoadResult.Success)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun interruptedSubmitRequiresExplicitRetryAndPreservesTheDraft() = runBlocking {
        val repository = object : Repository() {
            override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes += submission; throw java.io.IOException("synthetic interrupted write") }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = UploadBookViewModel(repository, scope)
        try {
            model.select("first"); model.submit(); model.submit()
            assertTrue(model.state.submissionUncertain); assertEquals(1, repository.writes.size)
            assertEquals("合成书", model.state.draft.title); assertEquals("first", model.state.selectedFile?.uri)
            model.submit(confirmUncertainRetry = true); assertEquals(2, repository.writes.size)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun accountChangeDiscardsLateWorkWithoutLeakingDraftsOrAutoSubmitting() = runBlocking {
        val parsed = CompletableDeferred<ParsedEpub>()
        val repository = object : Repository() { override suspend fun parse(document: UploadDocument) = withContext(NonCancellable) { parsed.await() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = UploadBookViewModel(repository, scope)
        try {
            model.enter(10); model.select("first"); model.environmentChanged()
            parsed.complete(ParsedEpub("旧账号", "旧作者", chapters = listOf(UploadChapter("一", "正文", 1))))
            assertNull(model.state.existingNovelId); assertEquals("", model.state.draft.title)
            assertEquals(LoadResult.Idle, model.state.chapters); assertTrue(repository.writes.isEmpty())
        } finally { model.close(); scope.cancel() }
    }
    @Test fun explicitRejectionAllowsRetryButEmptyParsedDocumentCannotBeSubmitted() = runBlocking {
        val repository = object : Repository() {
            var empty = true
            override suspend fun parse(document: UploadDocument) = if (empty) ParsedEpub() else super.parse(document)
            override suspend fun submit(submission: UploadSubmission): UploadActionResult { writes += submission; return UploadActionResult(false, "明确拒绝") }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = UploadBookViewModel(repository, scope)
        try {
            model.select("first"); model.submit(); assertTrue(repository.writes.isEmpty())
            repository.empty = false; model.select("second"); model.submit(); model.submit()
            assertEquals(2, repository.writes.size); assertFalse(model.state.submissionUncertain)
            assertEquals("合成书", model.state.draft.title)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun sourceStatusEncodingKeepsAdultAndCompletionIndependent() {
        assertEquals("连载中", websiteUploadSpans(UploadBookDraft(spans = "balanced")))
        assertEquals("已完结", websiteUploadSpans(UploadBookDraft(spans = "已完结")))
        assertEquals("19", websiteUploadSpans(UploadBookDraft(isAdult = true)))
        assertEquals("19 完结", websiteUploadSpans(UploadBookDraft(spans = "已完结", isAdult = true)))
    }
}
