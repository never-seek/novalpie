package com.novalpie.nativeapp.data

import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeDownloadTransferTest {
    @Test
    fun pausedDownloadWaitsUntilTheControlIsResumed() = runBlocking {
        val control = NativeDownloadControl()
        control.pause()
        var reachedCheckpoint = false
        val worker: Job = launch {
            control.awaitIfPaused()
            reachedCheckpoint = true
        }

        delay(50)
        assertFalse(reachedCheckpoint)
        control.resume()
        worker.join()
        assertTrue(reachedCheckpoint)
    }

    @Test
    fun streamCopyRespectsPauseWithoutDroppingBytes() = runBlocking {
        val bytes = ByteArray(128 * 1024) { index -> (index * 7).toByte() }
        val control = NativeDownloadControl()
        control.pause()
        val output = ByteArrayOutputStream()
        val worker = launch {
            copyNativeDownloadStream(
                input = bytes.inputStream(),
                output = output,
                awaitIfPaused = control::awaitIfPaused,
            )
        }

        delay(50)
        assertEquals(0, output.size())
        control.resume()
        worker.join()
        assertArrayEquals(bytes, output.toByteArray())
    }

    @Test
    fun nativeEpubGenerationUsesAPrivatePartFileBeforePublication() {
        val workDirectory = File("/private/novalpie-epub-work")

        val target = nativeEpubGenerationFile(workDirectory, bookId = 356636L)

        assertEquals(workDirectory, target.parentFile)
        assertTrue(target.name.endsWith(".epub.part"))
        assertTrue(target.name.contains("356636"))
    }

    @Test
    fun copiesTheCompleteGeneratedFileAndReportsTheExactByteCount() {
        val source = File.createTempFile("novalpie-generated-", ".epub").apply {
            writeBytes(ByteArray(2 * 1024 * 1024 + 17) { index -> (index * 13).toByte() })
        }
        val output = ByteArrayOutputStream()

        try {
            val copied = copyNativeDownloadFile(source, output)

            assertEquals(source.length(), copied)
            assertArrayEquals(source.readBytes(), output.toByteArray())
        } finally {
            source.delete()
        }
    }

    @Test
    fun privateFilePublicationRespectsPauseWithoutDroppingBytes() = runBlocking {
        val source = File.createTempFile("novalpie-generated-", ".epub").apply {
            writeBytes(ByteArray(128 * 1024) { index -> (index * 19).toByte() })
        }
        val control = NativeDownloadControl()
        control.pause()
        val output = ByteArrayOutputStream()
        val worker = launch {
            copyNativeDownloadFilePausable(
                source = source,
                output = output,
                awaitIfPaused = control::awaitIfPaused,
            )
        }

        try {
            delay(50)
            assertEquals(0, output.size())
            control.resume()
            worker.join()
            assertArrayEquals(source.readBytes(), output.toByteArray())
        } finally {
            source.delete()
        }
    }

    @Test
    fun nativeDownloadNameMatcherRejectsUnrelatedOrIncompleteNames() {
        assertTrue(isNativeDownloadDisplayName("书名_356636_1787421625776.epub"))
        assertTrue(isNativeDownloadDisplayName("title_7_1787421625776.TXT"))
        assertFalse(isNativeDownloadDisplayName("book.epub"))
        assertFalse(isNativeDownloadDisplayName("book_356636.epub"))
        assertFalse(isNativeDownloadDisplayName("book_356636_123.epub"))
        assertFalse(isNativeDownloadDisplayName("book_356636_1787421625776.pdf"))
    }
}
