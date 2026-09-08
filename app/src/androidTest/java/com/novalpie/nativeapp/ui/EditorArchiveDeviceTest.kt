package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.EditorArchiveStore
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID

/** Real Android AtomicFile/rename/fsync, synthetic drafts in an isolated test-owned directory. */
class EditorArchiveDeviceTest {
    @Test fun interruptedSaveRecoversCompletePriorGenerationAndDetectsBodyCorruption() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folderName = "beta7-editor-store-${UUID.randomUUID()}"
        val root = File(context.filesDir, folderName).canonicalFile
        var reject = false
        val store = EditorArchiveStore(context, folderName) { if (reject) throw IOException("synthetic full disk") }
        val original = EditorArchive("sample", "受控原稿", 1, "仅供验收的原稿\n第二行", EditorBookMetadata(title = "原书名"))
        try {
            store.save(original)
            reject = true
            assertTrue(runCatching { store.save(original.copy(textContent = "新版", metadata = EditorBookMetadata(title = "新书名"))) }.isFailure)
            assertEquals(original.textContent, store.load("sample")?.textContent)
            assertEquals("原书名", store.load("sample")?.metadata?.title)
            reject = false
            val next = original.copy(textContent = "已提交的新正文\n第三行", metadata = EditorBookMetadata(title = "新书名"))
            store.save(next)
            assertEquals(next.textContent, store.load("sample")?.textContent)
            val metadata = File(root, "sample.json")
            assertTrue(metadata.renameTo(File(metadata.path + ".bak")))
            assertEquals(listOf("sample"), store.list().map { it.id })
            assertEquals(next.textContent, store.load("sample")?.textContent)
            val source = JSONObject(metadata.readText())
            val body = File(root, source.getString("textFile"))
            body.writeText("损坏")
            assertTrue(runCatching { store.load("sample") }.isFailure)
            assertTrue(metadata.exists()); assertTrue(body.exists())
            val proof = File(context.cacheDir, "beta7-editor-archive").apply { mkdirs() }
            File(proof, "store.json").writeText(JSONObject().put("passed", true).put("failurePreservedPriorGeneration", true)
                .put("replacementCommitted", true).put("backupRecovered", true).put("corruptionRejected", true)
                .put("productionArchivesTouched", false).toString(2))
        } finally {
            check(root.parentFile == context.filesDir.canonicalFile && root.name.startsWith("beta7-editor-store-"))
            root.deleteRecursively() // Only this synthetic test directory; never production archives.
        }
    }
}
