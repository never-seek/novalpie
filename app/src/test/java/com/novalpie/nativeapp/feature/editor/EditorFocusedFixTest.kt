package com.novalpie.nativeapp.feature.editor

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class EditorFocusedFixTest {
    @Test fun addingChapterPreservesUnsplitProseAndUndoRedoAndArchive() = withEditor { model, repository, ui ->
        val prose = "  手稿第一行\n\n最后一句，不能丢。\n"
        model.updateEditorDocument(prose, 4)
        model.addEditorChapter()
        finish(model, ui)
        val created = model.uploadEditorState
        assertEquals(listOf(prose, ""), created.chapters.map { it.content })
        assertTrue(created.text.contains(prose))
        model.undoEditorDocument()
        assertEquals(prose, model.uploadEditorState.text)
        assertEquals(4, model.uploadEditorState.cursorPosition)
        assertTrue(model.uploadEditorState.chapters.isEmpty())
        model.redoEditorDocument()
        assertEquals(created.text, model.uploadEditorState.text)
        model.saveEditorArchive()
        finish(model, ui)
        assertTrue(repository.saved.single().textContent.contains(prose))
        assertEquals(2, repository.saved.single().chapterCount)
    }

    @Test fun changedSplitTargetRejectsOldBackgroundResult() = staleSplit(EditorSplitMode.ParagraphCount) {
        it.updateEditorSplitTarget("2")
    }
    @Test fun changedBatchModeRejectsOldBackgroundResult() = staleSplit(EditorSplitMode.BatchGenerate) {
        it.updateEditorBatchMode(EditorBatchMode.Characters)
    }
    @Test fun changedBatchTargetRejectsOldBackgroundResult() = staleSplit(EditorSplitMode.BatchGenerate) {
        it.updateEditorBatchTarget("2")
    }
    private fun staleSplit(mode: EditorSplitMode, change: (EditorViewModel) -> Unit) = withEditor { model, _, ui ->
        model.updateEditorText("第一段\n\n第二段\n\n第三段")
        model.updateEditorSplitMode(mode)
        model.updateEditorSplitTarget("1")
        model.updateEditorBatchMode(EditorBatchMode.Paragraphs)
        model.updateEditorBatchTarget("1")
        val before = model.uploadEditorState.text
        model.processEditorSplit()
        ui.runNext() // Real Default dispatcher computes while its return is held.
        val completedWorker = ui.next()
        change(model)
        model.updateEditorMetadata(EditorBookMetadata(title = "后来输入的书名"))
        completedWorker.run()
        assertEquals(before, model.uploadEditorState.text)
        assertTrue(model.uploadEditorState.chapters.isEmpty())
        assertEquals("后来输入的书名", model.uploadEditorState.metadata.title)
        assertFalse(model.uploadEditorState.busy)
    }

    @Test fun splitReturnDoesNotSerializeManuscriptOnUiThread() = withEditor { model, _, ui ->
        // A full serialization allocates several copies. Check allocations, not wall-clock timing.
        val prose = "文".repeat(2_000_000)
        model.updateEditorText(prose)
        model.updateEditorSplitMode(EditorSplitMode.ParagraphCount)
        model.updateEditorSplitTarget("1")
        model.processEditorSplit()
        ui.runNext()
        val completedWorker = ui.next()
        val allocated = uiAllocations { completedWorker.run() }
        assertTrue("UI return allocated $allocated bytes for a ${prose.length}-character draft", allocated < prose.length)
        assertEquals(prose, model.uploadEditorState.chapters.single().content)
        assertTrue(model.uploadEditorState.text.contains(prose))
    }

    @Test fun chapterEditsSerializeOffUiAndKeepEveryRapidEdit() = withEditor { model, repository, ui ->
        val prose = "文".repeat(2_000_000)
        repository.loadedChapters = listOf(UploadChapter("原章", prose, 1))
        model.selectEditorDocument("fixture")
        finish(model, ui)
        val allocated = uiAllocations {
            model.updateEditorChapter(0, "修改标题", prose)
            model.addEditorChapter()
            model.updateEditorChapter(1, "新增标题", "新章正文")
            model.addEditorChapter()
            model.deleteEditorChapter(2)
            finish(model, ui)
        }
        assertTrue("Chapter callbacks allocated $allocated bytes on UI", allocated < prose.length)
        finish(model, ui)
        assertEquals(listOf("修改标题", "新增标题"), model.uploadEditorState.chapters.map { it.title })
        assertEquals(listOf(prose, "新章正文"), model.uploadEditorState.chapters.map { it.content })
        assertTrue(model.uploadEditorState.text.contains("新章正文"))
        assertFalse(model.uploadEditorState.text.contains("##__T[00003]__##"))
        model.saveEditorArchive()
        finish(model, ui)
        assertEquals(model.uploadEditorState.text, repository.saved.single().textContent)
    }

    @Test fun batchImportSerializesManuscriptOffUiThread() = withEditor { model, repository, ui ->
        val prose = "文".repeat(2_000_000)
        repository.loadedChapters = listOf(UploadChapter("导入章节", prose, 1))
        model.queueEditorDocuments(listOf("fixture"))
        finish(model, ui)
        model.importQueuedEditorDocuments()
        val allocated = uiAllocations { finish(model, ui) }
        assertTrue("Import allocated $allocated bytes on UI", allocated < prose.length)
        assertTrue(model.uploadEditorState.text.endsWith(prose))
    }

    @Test fun lateChapterSerializationCannotOverwriteNewTextOrMetadata() = withEditor { model, repository, ui ->
        repository.loadedChapters = listOf(UploadChapter("原章", "正文", 1))
        model.selectEditorDocument("fixture")
        finish(model, ui)
        model.updateEditorChapter(0, "旧操作", "旧正文")
        model.updateEditorText("新手稿")
        model.updateEditorMetadata(EditorBookMetadata(title = "新书名"))
        finish(model, ui)
        assertEquals("新手稿", model.uploadEditorState.text)
        assertTrue(model.uploadEditorState.chapters.isEmpty())
        assertEquals("新书名", model.uploadEditorState.metadata.title)
    }

    @Test fun chapterSerializationRejectsAnEditAndUndoAbaAtTheWorkerReturn() = withEditor { model, repository, ui ->
        repository.loadedChapters = listOf(UploadChapter("原章", "正文", 1))
        model.selectEditorDocument("fixture")
        finish(model, ui)
        val original = model.uploadEditorState.text
        model.updateEditorChapter(0, "不应应用的标题", "旧操作正文")
        assertTrue("Chapter serialization must be asynchronous", model.uploadEditorState.busy)
        ui.runNext()
        val completedWorker = ui.next()
        model.updateEditorText("中间的新稿")
        model.updateEditorText(original)
        completedWorker.run()
        finish(model, ui)
        assertEquals(original, model.uploadEditorState.text)
        assertTrue(model.uploadEditorState.chapters.isEmpty())
    }

    @Test fun saveImmediatelyAfterChapterEditsWaitsForCoherentText() = withEditor { model, repository, ui ->
        model.addEditorChapter()
        model.updateEditorChapter(0, "保存标题", "立即保存的正文")
        model.saveEditorArchive()
        // Drain both chapter work and the archive action requested before it finished.
        while (repository.saved.isEmpty() || model.uploadEditorState.busy) ui.runNext()
        assertEquals("##__T[00001]__##\n保存标题\n##__C[00001]__##\n立即保存的正文", repository.saved.single().textContent)
    }

    @Test fun eachQueuedChapterEditHasItsOwnCoherentUndoSnapshot() = withEditor { model, _, ui ->
        model.addEditorChapter()
        model.updateEditorChapter(0, "一", "第一版")
        model.addEditorChapter()
        model.updateEditorChapter(1, "二", "第二章正文")
        finish(model, ui)
        model.undoEditorDocument()
        assertEquals(listOf("第一版", ""), model.uploadEditorState.chapters.map { it.content })
        assertFalse(model.uploadEditorState.text.contains("第二章正文"))
        model.undoEditorDocument()
        assertEquals(listOf("第一版"), model.uploadEditorState.chapters.map { it.content })
        model.redoEditorDocument()
        model.redoEditorDocument()
        assertTrue(model.uploadEditorState.text.endsWith("第二章正文"))
    }

    @Test fun clearingDuringQueuedChapterWorkCannotRestoreItsText() = withEditor { model, _, ui ->
        model.addEditorChapter()
        model.addEditorChapter()
        model.clearUploadEditor()
        model.updateEditorText("清空后的新稿")
        ui.drain()
        assertEquals("清空后的新稿", model.uploadEditorState.text)
        assertTrue(model.uploadEditorState.chapters.isEmpty())
        assertFalse(model.uploadEditorState.busy)
    }

    @Test fun queuedDeleteDoesNotRetargetAnEditFromTheStillVisibleChapterList() = withEditor { model, repository, ui ->
        repository.loadedChapters = listOf(UploadChapter("一", "甲", 1), UploadChapter("二", "乙", 2), UploadChapter("三", "丙", 3))
        model.selectEditorDocument("fixture")
        finish(model, ui)
        model.deleteEditorChapter(0)
        assertTrue("Delete must serialize asynchronously", model.uploadEditorState.busy)
        model.updateEditorChapter(1, "仍在界面上的第二章", "保留编辑")
        finish(model, ui)
        assertEquals(listOf("保留编辑", "丙"), model.uploadEditorState.chapters.map { it.content })
        assertEquals(listOf(1, 2), model.uploadEditorState.chapters.map { it.chapterNumber })
    }

    @Test fun aiQueuedBehindChapterEditsCannotOverwriteALaterManualRegex() = withEditor { model, _, ui ->
        model.openUploadEditor()
        model.addEditorChapter()
        model.addEditorChapter()
        model.generateEditorRegexWithAi()
        model.updateEditorSplitPattern("新手动规则")
        finish(model, ui)
        assertEquals("新手动规则", model.uploadEditorState.splitPattern)
        assertFalse(model.uploadEditorState.busy)
    }

    private fun finish(model: EditorViewModel, ui: EditorUiQueue) {
        while (model.uploadEditorState.busy) ui.runNext()
        ui.drain()
    }
    private fun withEditor(block: (EditorViewModel, EditorMemoryRepository, EditorUiQueue) -> Unit) {
        val ui = EditorUiQueue()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val repository = EditorMemoryRepository()
        val model = EditorViewModel(repository, scope)
        try { block(model, repository, ui) }
        finally { model.close(); scope.cancel(); ui.drain() }
    }
}
