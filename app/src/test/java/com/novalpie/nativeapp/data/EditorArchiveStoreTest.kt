package com.novalpie.nativeapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
// Android15 replacement-rename is separately exercised on-device; API28 backup IO works on Windows.
@org.robolectric.annotation.Config(sdk = [28])
class EditorArchiveStoreTest {
    private fun sample(id: String = "archive-1") = EditorArchive(id, "Draft", 123L, "原始草稿正文", EditorBookMetadata(title = "原书"), "book.txt", 1, 6)

    @Test fun missingBodyMustNotLoadAsAnEmptyDocumentAndOverwriteTheEditor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = "editor-missing-${System.nanoTime()}"
        val store = EditorArchiveStore(context, folder)
        store.save(sample())
        val root = java.io.File(context.filesDir, folder)
        val metadata = org.json.JSONObject(java.io.File(root, "archive-1.json").readText())
        val text = java.io.File(root, metadata.optString("textFile").ifBlank { "archive-1.txt" })
        assertTrue(text.delete()) // Simulate loss only inside this test's owned temporary directory.
        assertTrue("缺失正文应报告损坏，不得返回空白存档", runCatching { store.load("archive-1") }.isFailure)
        assertTrue(java.io.File(root, "archive-1.json").exists())
    }

    @Test fun readableButCorruptedBodyCannotBeReturnedUnderValidMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = "editor-corrupt-${System.nanoTime()}"
        val store = EditorArchiveStore(context, folder)
        store.save(sample())
        val root = java.io.File(context.filesDir, folder)
        val metadata = org.json.JSONObject(java.io.File(root, "archive-1.json").readText())
        java.io.File(root, metadata.optString("textFile").ifBlank { "archive-1.txt" }).writeText("被截断的草稿")
        assertTrue("可读JSON不能掩盖正文损坏", runCatching { store.load("archive-1") }.isFailure)
        assertTrue(java.io.File(root, "archive-1.json").exists())
    }

    @Test fun atomicMetadataBackupCanBeDiscoveredAndRecoveredAfterInterruptedSave() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = "editor-backup-${System.nanoTime()}"
        val store = EditorArchiveStore(context, folder)
        store.save(sample())
        val original = java.io.File(context.filesDir, "$folder/archive-1.json")
        assertTrue(original.renameTo(java.io.File(original.path + ".bak")))
        assertEquals(listOf("archive-1"), store.list().map { it.id })
        assertEquals("原始草稿正文", store.load("archive-1")?.textContent)
    }

    @Test fun failedSaveBeforeMetadataSwitchPreservesTheLastGoodGeneration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = "editor-atomic-${System.nanoTime()}"
        var interrupt = false
        val store = EditorArchiveStore(context, folder) { if (interrupt) throw java.io.IOException("synthetic full disk") }
        store.save(sample())
        interrupt = true
        assertTrue(runCatching { store.save(sample().copy(textContent = "新版正文", metadata = EditorBookMetadata(title = "新书"))) }.isFailure)
        val old = store.load("archive-1")!!
        assertEquals("原始草稿正文", old.textContent); assertEquals("原书", old.metadata.title)
        interrupt = false
        store.save(sample().copy(textContent = "新版正文", metadata = EditorBookMetadata(title = "新书")))
        assertEquals("新版正文", store.load("archive-1")?.textContent)
        assertEquals("新书", store.load("archive-1")?.metadata?.title)
        assertEquals(1, java.io.File(context.filesDir, folder).listFiles()!!.count { it.name.contains(".body-") })
    }

    @Test fun unreadableCommitStatusRetainsBothBodiesInsteadOfGuessingWhichOneToDelete() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = "editor-unknown-${System.nanoTime()}"
        val root = java.io.File(context.filesDir, folder)
        var breakMetadata = false
        val store = EditorArchiveStore(context, folder) {
            if (breakMetadata) {
                java.io.File(root, "archive-1.json").writeText("{damaged")
                throw java.io.IOException("synthetic metadata read failure")
            }
        }
        store.save(sample())
        breakMetadata = true
        assertTrue(runCatching { store.save(sample().copy(textContent = "需要保留的另一份正文")) }.isFailure)
        assertEquals("不知道指针是否提交时不应删除任何一代正文", 2, root.listFiles()!!.count { it.name.contains(".body-") })
    }

    @Test fun deletingOneArchiveCannotDeleteTheBodyOfAnotherPrefixedArchiveId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = EditorArchiveStore(context, "editor-prefix-${System.nanoTime()}")
        store.save(sample())
        store.save(sample("archive-1.body-another").copy(textContent = "另一份独立存档"))
        store.delete("archive-1")
        assertEquals("另一份独立存档", store.load("archive-1.body-another")?.textContent)
    }

    @Test fun legacyPairLoadsAndRemainsRecoverableWhenUpgraded() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = "editor-legacy-${System.nanoTime()}"
        val root = java.io.File(context.filesDir, folder).apply { mkdirs() }
        val legacy = org.json.JSONObject().put("id", "archive-1").put("name", "旧存档").put("timestamp", 1)
            .put("metadata", org.json.JSONObject().put("title", "旧标题"))
        java.io.File(root, "archive-1.json").writeText(legacy.toString())
        java.io.File(root, "archive-1.txt").writeText("旧版正文")
        val store = EditorArchiveStore(context, folder)
        assertEquals("旧版正文", store.load("archive-1")?.textContent)
        store.save(sample())
        assertEquals("原始草稿正文", store.load("archive-1")?.textContent)
        assertEquals("旧版正文", java.io.File(root, "archive-1.txt").readText())
        java.io.File(root, "user-note.txt").writeText("不归存档所有")
        store.clear()
        assertTrue(store.list().isEmpty()); assertEquals("不归存档所有", java.io.File(root, "user-note.txt").readText())
    }
    @Test
    fun savesLoadsListsAndDeletesFileBackedArchives() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = EditorArchiveStore(context, directoryName = "editor-test-${System.nanoTime()}")
        val archive = EditorArchive(
            id = "archive-1",
            name = "Draft",
            timestamp = 123L,
            textContent = "very long source text",
            metadata = EditorBookMetadata(title = "Book", author = "Writer"),
            fileName = "book.txt",
            chapterCount = 2,
            totalWords = 21
        )

        store.save(archive)

        assertEquals("very long source text", store.load("archive-1")?.textContent)
        assertEquals(listOf("archive-1"), store.list().map { it.id })
        store.delete("archive-1")
        assertNull(store.load("archive-1"))
    }
}
