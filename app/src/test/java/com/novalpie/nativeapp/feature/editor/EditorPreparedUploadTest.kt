package com.novalpie.nativeapp.feature.editor

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.UploadFileSource
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.UploadBookState
import com.novalpie.nativeapp.ui.UploadDocument
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
class EditorPreparedUploadTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun cancellationAtIoReturnDeletesOnlyTheUnreceivedEpub() {
        val directory = temporary.newFolder("cache")
        val repository = repository(directory)
        val ui = EditorUiQueue()
        val scope = CoroutineScope(SupervisorJob() + ui)
        var received: UploadBookState? = null
        val adopted = runBlocking { repository.prepareUpload(41, metadata, chapters) }
        val adoptedFile = File(java.net.URI(adopted.selectedFile!!.uri))
        val job = scope.launch { received = repository.prepareUpload(42, metadata, chapters) }
        try {
            ui.runNext()
            val returnFromIo = ui.next() // IO has completed; caller has not resumed or adopted its file.
            val files = File(directory, "editor-upload-files").listFiles()!!.toList()
            assertEquals(2, files.size)
            files.forEach { ZipFile(it).use { zip -> assertNotNull(zip.getEntry("mimetype")) } }
            job.cancel()
            returnFromIo.run()
            ui.drain()
            assertNull(received)
            assertTrue(job.isCompleted)
            assertEquals("Unreceived EPUB leaked across dispatcher return", listOf(adoptedFile.name),
                File(directory, "editor-upload-files").listFiles()!!.map { it.name })
            assertTrue(adoptedFile.isFile)
        } finally { scope.cancel(); ui.drain() }
    }

    @Test fun successfulPreparationRetainsOriginalTargetUntilExplicitDiscard() = runBlocking {
        val directory = temporary.newFolder("cache")
        val repository = repository(directory)
        val prepared = repository.prepareUpload(73, metadata, chapters)
        assertEquals(73L, prepared.existingNovelId)
        val file = File(java.net.URI(prepared.selectedFile!!.uri))
        assertTrue(file.isFile)
        ZipFile(file).use { assertNotNull(it.getEntry("mimetype")) }
        repository.discardPrepared(prepared)
        assertFalse(file.exists())
    }

    @Test fun writerFailureDeletesItsPartiallyCreatedFile() = runBlocking {
        val directory = temporary.newFolder("cache")
        val repository = repository(directory)
        try {
            repository.prepareUpload(null, metadata.copy(title = ""), chapters)
            fail("Invalid metadata must fail")
        } catch (_: IllegalArgumentException) { }
        assertTrue(File(directory, "editor-upload-files").listFiles()!!.isEmpty())
    }

    @Test fun epubLoadSerializesParsedChaptersBeforeReturningToUi() {
        val directory = temporary.newFolder("cache")
        val prose = "文".repeat(2_000_000)
        val prepared = runBlocking { repository(directory).prepareUpload(null, metadata, listOf(UploadChapter("第一章", prose, 1))) }
        val repository = repository(directory, prepared.selectedFile)
        val ui = EditorUiQueue()
        val scope = CoroutineScope(SupervisorJob() + ui)
        var loaded: EditorLoadedDocument? = null
        try {
            scope.launch { loaded = repository.loadDocument(prepared.selectedFile!!.uri, "UTF-8", metadata) }
            val allocated = uiAllocations { while (loaded == null) ui.runNext() }
            assertTrue("EPUB load allocated $allocated bytes on UI", allocated < prose.length)
            assertTrue(loaded!!.text.contains(prose))
            assertEquals("测试书名", loaded!!.metadata.title)
        } finally { scope.cancel(); ui.drain() }
    }

    private fun repository(directory: File, document: UploadDocument? = null): AndroidEditorRepository {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getCacheDir(): File = directory
        }
        return AndroidEditorRepository(context, NovalPieApi(baseUrl = "https://fixture.invalid"),
            readDocument = { requireNotNull(document) }, source = { input ->
                val file = File(java.net.URI(input.uri))
                UploadFileSource(input.displayName, file.length(), input.mimeType) { file.inputStream() }
            })
    }
    private val metadata = EditorBookMetadata(title = "测试书名", author = "测试作者")
    private val chapters = listOf(UploadChapter("第一章", "必须保留的正文", 1))
}
