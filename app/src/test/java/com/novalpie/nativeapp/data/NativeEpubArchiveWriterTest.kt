package com.novalpie.nativeapp.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import okio.ByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeEpubArchiveWriterTest {
    @Test
    fun streamsChaptersKeepsOriginalImagesAndDeduplicatesRepeatedSourceUrls() = runBlocking {
        val imageBytes = byteArrayOf(0, 1, 2, 127, -1, 42)
        val openedUrls = mutableListOf<String>()
        val progress = mutableListOf<NativeEpubExportProgress>()
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(
                title = "Native book",
                author = "Writer",
                description = "Description",
            ),
            source = StringReader(
                """
                第1章 开始
                第一段 [图片: https://images.example.test/shared.webp]
                第2章 继续
                第二段 [图片：https://images.example.test/shared.webp]
                """.trimIndent()
            ),
            openAsset = { url ->
                openedUrls += url
                NativeEpubAsset(
                    mediaType = "image/webp",
                    input = ByteArrayInputStream(imageBytes),
                )
            },
            onProgress = progress::add,
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        assertEquals("mimetype", entries.keys.first())
        assertEquals("application/epub+zip", entries.getValue("mimetype").toString(Charsets.US_ASCII))
        assertEquals(listOf("https://images.example.test/shared.webp"), openedUrls)
        assertArrayEquals(imageBytes, entries.getValue("OEBPS/images/image-1.webp"))
        assertTrue(entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8).contains("images/image-1.webp"))
        assertTrue(entries.getValue("OEBPS/chapter-2.xhtml").toString(Charsets.UTF_8).contains("images/image-1.webp"))
        val opf = entries.getValue("OEBPS/content.opf").toString(Charsets.UTF_8)
        assertTrue(opf.contains("image-1.webp"))
        assertTrue(opf.contains("chapter-2.xhtml"))
        assertEquals(2, progress.last().completedChapters)
        assertEquals(1, progress.last().completedImages)
    }

    @Test
    fun missingImageDoesNotDiscardCompletedChapterText() = runBlocking {
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Book", author = "Writer"),
            source = StringReader("第1章\n正文 [图片: https://images.example.test/missing.jpg]"),
            openAsset = { throw IllegalStateException("image unavailable") },
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        val chapter = entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
        assertTrue(chapter.contains("正文"))
        assertTrue(chapter.contains("image-missing"))
    }
}
