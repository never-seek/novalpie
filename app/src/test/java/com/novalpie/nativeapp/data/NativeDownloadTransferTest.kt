package com.novalpie.nativeapp.data

import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeDownloadTransferTest {
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
}
