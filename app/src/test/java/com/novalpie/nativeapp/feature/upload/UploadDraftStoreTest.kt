package com.novalpie.nativeapp.feature.upload

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UploadDraftStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private fun state(book: Long? = 2) = UploadBookState(book, UploadBookDraft(title = "全部信息", titleTranslation = "Original", author = "作者", description = "简介",
        language = "ko", spans = "已完结", isAdult = true, source = "upload", sourceUrl = "https://fixture.invalid/source", tagsText = "甲,乙", coverUrl = "https://fixture.invalid/cover.file", chapterCount = 1),
        selectedFile = UploadDocument("content://fixture/book.epub", "书.epub", 80), serverFilePath = "owned.epub",
        chapters = LoadResult.Success(listOf(UploadChapter("章", "段落\n\n😀&特殊文本", 1, 2, listOf("卷", "部"), "OPS/a.xhtml", 0))))
    @Test fun completeDraftAndChaptersAreIsolatedByAccountAndBookAndPreserveEveryField() {
        val root = temp.newFolder(); val store = UploadDraftStore(root); val original = state()
        store.save(1, original)
        val restored = UploadDraftStore(root).load(1, 2)!!
        assertEquals(original.draft, restored.draft); assertEquals(original.selectedFile, restored.selectedFile)
        assertEquals(original.chapters, restored.chapters); assertEquals("owned.epub", restored.serverFilePath)
        assertNull(store.load(2, 2)); assertNull(store.load(1, 3)); assertNull(store.load(1, null))
    }
    @Test fun writingRecordRestoresAsUncertainButNeverBusyOrSuccessful() {
        val store = UploadDraftStore(temp.newFolder())
        store.save(1, state().copy(processing = true, submitResult = LoadResult.Loading))
        val restored = store.load(1, 2)!!
        assertFalse(restored.processing); assertTrue(restored.submissionUncertain)
        assertTrue(restored.submitResult is LoadResult.Error)
    }
    @Test fun metadataEditsReuseImmutableChaptersAndCorruptBodyDoesNotBecomeEmptyDraft() {
        val root = temp.newFolder(); val store = UploadDraftStore(root); val first = state()
        store.save(1, first)
        repeat(4) { store.save(1, first.copy(draft = first.draft.copy(title = "改名$it"))) }
        val dir = File(root, "1/2"); val bodies = dir.listFiles()!!.filter { it.extension == "bin" }
        assertEquals(1, bodies.size)
        bodies.single().writeText("坏内容")
        assertTrue(runCatching { store.load(1, 2) }.isFailure)
        assertTrue(File(dir, "draft.json").exists())
        assertTrue(bodies.single().exists())
    }
    @Test fun preSubmissionCheckpointMustVerifyEvenASameLengthPreviouslyCachedBody() {
        val root = temp.newFolder(); val store = UploadDraftStore(root); val first = state()
        store.save(1, first)
        val body = File(root, "1/2").listFiles()!!.single { it.extension == "bin" }
        val bytes = body.readBytes(); bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); body.writeBytes(bytes)
        assertTrue("正式提交检查点必须核验章节哈希，不能只比较长度", runCatching { store.save(1, first.copy(submitResult = LoadResult.Loading, processing = true)) }.isFailure)
    }
    @Test fun metadataBackupRecoversAndDiscardOnlyRemovesTheNamedAccountAndTarget() {
        val root = temp.newFolder(); val store = UploadDraftStore(root)
        store.save(1, state()); store.save(1, state(3)); store.save(2, state())
        val file = File(root, "1/2/draft.json"); assertTrue(file.renameTo(File(file.path + ".bak")))
        assertEquals("全部信息", store.load(1, 2)?.draft?.title)
        store.discard(1, 2)
        assertNull(store.load(1, 2)); assertNotNull(store.load(1, 3)); assertNotNull(store.load(2, 2))
    }
    @Test fun delayedAutoSaveCannotOverwriteANewerConfirmedCheckpoint() = runBlocking {
        val store = UploadDraftStore(temp.newFolder())
        val scheduler = TestCoroutineScheduler()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scheduler))
        val persistence = StoredUploadDrafts(store, scope)
        try {
            persistence.schedule(1, state().copy(draft = state().draft.copy(title = "旧输入"))) { error("unexpected error") }
            persistence.checkpoint(1, state().copy(draft = state().draft.copy(title = "提交时的最新输入"), submitResult = LoadResult.Loading))
            scheduler.advanceUntilIdle()
            val restored = persistence.load(1, 2)!!
            assertEquals("提交时的最新输入", restored.draft.title)
            assertTrue(restored.submissionUncertain)
        } finally { scope.cancel() }
    }
}
