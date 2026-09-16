package com.novalpie.nativeapp.feature.editor

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class EditorViewModelTest {
    @Test fun proxyChangePreservesTheDraftButAccountChangeClearsIt() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = EditorViewModel(Repository(), scope)
        try {
            model.updateEditorText("当前账号草稿")
            model.environmentChanged(accountChanged = false)
            assertEquals("当前账号草稿", model.uploadEditorState.text)
            model.environmentChanged(accountChanged = true)
            assertEquals("", model.uploadEditorState.text)
            assertFalse(model.uploadEditorState.canUndo)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun staleScriptCallbackCannotMatchANewRunAfterClear() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = EditorViewModel(Repository(), scope)
        try {
            model.updateEditorText("旧脚本原文"); model.updateEditorSplitMode(EditorSplitMode.CustomScript); model.processEditorSplit()
            val oldRun = model.uploadEditorState.scriptRunId
            model.clearUploadEditor()
            model.updateEditorText("新脚本原文"); model.updateEditorSplitMode(EditorSplitMode.CustomScript); model.processEditorSplit()
            assertNotEquals(oldRun, model.uploadEditorState.scriptRunId)
            model.completeEditorCustomScript(oldRun, "过期结果", null)
            assertEquals("新脚本原文", model.uploadEditorState.text)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun aiResponseDoesNotOverwriteAManuallyEditedRegex() = runBlocking {
        val response = CompletableDeferred<String>()
        val repository = object : Repository() { override suspend fun generateRegex(config: WorkspaceLocalApiConfig, chapterTitles: List<String>) = response.await() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = EditorViewModel(repository, scope)
        try {
            model.openUploadEditor(); model.addEditorChapter(); model.addEditorChapter()
            awaitEditorIdle(model)
            model.generateEditorRegexWithAi()
            model.updateEditorSplitPattern("手动规则"); response.complete("旧AI规则")
            assertEquals("手动规则", model.uploadEditorState.splitPattern)
            assertFalse(model.uploadEditorState.busy)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun loadedFileCannotOverwriteTextTypedWhileLoading() = runBlocking {
        val response = CompletableDeferred<EditorLoadedDocument>()
        val repository = object : Repository() { override suspend fun loadDocument(uri: String, encoding: String, metadata: EditorBookMetadata) = response.await() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = EditorViewModel(repository, scope)
        try {
            model.selectEditorDocument("fixture"); model.updateEditorText("后输入的新稿")
            response.complete(EditorLoadedDocument(UploadDocument("fixture", "old.txt", 3), "旧文件", EditorBookMetadata(), emptyList()))
            assertEquals("后输入的新稿", model.uploadEditorState.text)
            assertFalse(model.uploadEditorState.busy)
        } finally { model.close(); scope.cancel() }
    }
    private open class Repository : EditorRepository {
        val saved = mutableListOf<EditorArchive>()
        override fun configs() = listOf(WorkspaceLocalApiConfig(1, "合成配置", "fixture", "https://fixture.invalid", "test-only-not-a-real-key"))
        override fun archives() = saved.toList()
        override suspend fun document(uri: String) = error("unused")
        override suspend fun loadDocument(uri: String, encoding: String, metadata: EditorBookMetadata): EditorLoadedDocument = error("unused")
        override suspend fun importDocuments(documents: List<UploadDocument>, encoding: String) = emptyList<UploadChapter>()
        override suspend fun generateRegex(config: WorkspaceLocalApiConfig, chapterTitles: List<String>) = "^第.+章"
        override suspend fun processText(endpoint: String, text: String, timeoutSeconds: Int) = text
        override fun saveArchive(archive: EditorArchive) { saved += archive }
        override fun loadArchive(id: String) = saved.find { it.id == id }
        override fun deleteArchive(id: String) { saved.removeAll { it.id == id } }
        override fun clearArchives() { saved.clear() }
        override suspend fun export(uri: String, metadata: EditorBookMetadata, chapters: List<UploadChapter>) = Unit
        override suspend fun prepareUpload(bookId: Long?, metadata: EditorBookMetadata, chapters: List<UploadChapter>) = error("unused")
        override fun discardPrepared(prepared: UploadBookState) = Unit
    }
    @Test fun aiRegexResponseIsAppliedAndAlwaysClearsTheBusyIndicator() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = EditorViewModel(Repository(), scope)
        try {
            model.openUploadEditor(); model.addEditorChapter(); model.addEditorChapter()
            awaitEditorIdle(model)
            model.generateEditorRegexWithAi()
            assertFalse("AI完成后不能永远处理中", model.uploadEditorState.busy)
            assertEquals("^第.+章", model.uploadEditorState.splitPattern)
            assertEquals(EditorTab.Split, model.uploadEditorState.selectedTab)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun localSplitIsScheduledInsteadOfBlockingTheCallingUiThread() {
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        val scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.test.StandardTestDispatcher(scheduler))
        val model = EditorViewModel(Repository(), scope)
        try {
            model.updateEditorText("第一段\n\n第二段"); model.updateEditorSplitMode(EditorSplitMode.ParagraphCount); model.updateEditorSplitTarget("1")
            model.processEditorSplit()
            assertTrue("大文本分章不能在按钮调用栈上同步完成", model.uploadEditorState.busy)
            assertTrue(model.uploadEditorState.chapters.isEmpty())
        } finally { model.close(); scope.cancel() }
    }
    @Test fun clearingEditorWhileProcessorIsRunningMustNotRestoreOldText() = runBlocking {
        val response = CompletableDeferred<String>()
        val repository = object : Repository() { override suspend fun processText(endpoint: String, text: String, timeoutSeconds: Int) = withContext(NonCancellable) { response.await() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = EditorViewModel(repository, scope)
        try {
            model.updateEditorText("旧草稿"); model.updateEditorSplitMode(EditorSplitMode.ApiProcess); model.processEditorSplit()
            model.clearUploadEditor(); model.updateEditorText("用户新草稿")
            response.complete("迟到的旧处理结果"); yield()
            assertEquals("用户新草稿", model.uploadEditorState.text)
            assertFalse(model.uploadEditorState.busy)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun editedChapterContentSurvivesSavingAnArchive() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repository = Repository(); val model = EditorViewModel(repository, scope)
        try {
            model.addEditorChapter(); model.updateEditorChapter(0, "自己的标题", "修改后的正文")
            model.saveEditorArchive()
            withTimeout(5000) { while (model.uploadEditorState.busy) delay(10) }
            assertTrue("存档不能只保存过期的总文本", repository.saved.single().textContent.contains("修改后的正文"))
        } finally { model.close(); scope.cancel() }
    }
    private suspend fun awaitEditorIdle(model: EditorViewModel) {
        withTimeout(5000) { while (model.uploadEditorState.busy) delay(10) }
    }
}
