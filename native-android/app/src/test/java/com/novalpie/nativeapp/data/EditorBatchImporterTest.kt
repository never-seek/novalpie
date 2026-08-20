package com.novalpie.nativeapp.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorBatchImporterTest {
    @Test
    fun archiveImportsOnlySupportedTextEntriesInArchiveOrder() {
        val archive = zipOf(
            "chapter-01.txt" to "first",
            "notes.bin" to "ignored",
            "nested/chapter-02.md" to "# second"
        )

        val entries = EditorBatchImporter.readArchive(
            ByteArrayInputStream(archive),
            StandardCharsets.UTF_8
        )

        assertEquals(listOf("chapter-01.txt", "chapter-02.md"), entries.map { it.displayName })
        assertEquals(listOf("first", "# second"), entries.map { it.text })
    }

    @Test
    fun archiveRejectsSingleEntryThatWouldExceedEditorMemoryBudget() {
        val oversized = "x".repeat(EditorBatchImporter.MAX_ENTRY_BYTES + 1)
        val archive = zipOf("too-large.txt" to oversized)

        val failure = runCatching {
            EditorBatchImporter.readArchive(ByteArrayInputStream(archive), StandardCharsets.UTF_8)
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(failure?.message.orEmpty().contains("过大"))
    }

    @Test
    fun fileTypeDetectionOnlyAcceptsSourceAdvertisedTextAndZipFiles() {
        assertTrue(EditorBatchImporter.isTextFile("story.TXT"))
        assertTrue(EditorBatchImporter.isTextFile("chapter.markdown"))
        assertTrue(EditorBatchImporter.isBatchFile("book.zip"))
        assertFalse(EditorBatchImporter.isBatchFile("cover.jpg"))
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
