package com.novalpie.nativeapp.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NativeEpubCompressionLevelTest {
    @Test fun levelZeroAndNineApplyToImagesAndTextWithoutChangingTheirBytes() = runBlocking {
        // A deterministic compressible binary image fixture detects STORED-only image regressions.
        val image = "GIF89a".toByteArray() + ByteArray(256 * 1024) { (it % 16).toByte() }
        val source = "第1章 测试\n" + "无损文字压缩测试。".repeat(2000) + "\n[图片: https://example.test/animation.gif]"
        suspend fun archive(level: Int): ByteArray {
            val out = ByteArrayOutputStream()
            NativeEpubArchiveWriter.write(out, NativeEpubMetadata("压缩测试", "测试作者"), StringReader(source),
                openAsset = { NativeEpubAsset("image/gif", image.inputStream()) }, zipCompressionLevel = level)
            return out.toByteArray()
        }
        val plain = archive(0)
        val compressed = archive(9)
        val files = listOf(File.createTempFile("epub-level0-", ".epub"), File.createTempFile("epub-level9-", ".epub"))
        try {
            files[0].writeBytes(plain); files[1].writeBytes(compressed)
            ZipFile(files[0]).use { zero -> ZipFile(files[1]).use { nine ->
                val entries = zero.entries().asSequence().toList()
                assertTrue("Level 0 must genuinely store every entry", entries.all { it.method == ZipEntry.STORED })
                assertEquals(ZipEntry.STORED, nine.getEntry("mimetype").method)
                for (entry in entries) {
                    val other = nine.getEntry(entry.name)
                    if (entry.name != "mimetype") assertEquals("Level 9 must also compress original image entries", ZipEntry.DEFLATED, other.method)
                    assertArrayEquals("Compression must never change extracted resource bytes: ${entry.name}",
                        zero.getInputStream(entry).use { it.readBytes() }, nine.getInputStream(other).use { it.readBytes() })
                }
                assertArrayEquals(image, nine.getInputStream(nine.getEntry("OEBPS/images/image-1.gif")).use { it.readBytes() })
            } }
            assertTrue("Compressible image-heavy EPUB must shrink measurably", compressed.size < plain.size / 3)
            println("EPUB_COMPRESSION level0=${plain.size} level9=${compressed.size}")
        } finally { files.forEach(File::delete) }
    }
}
