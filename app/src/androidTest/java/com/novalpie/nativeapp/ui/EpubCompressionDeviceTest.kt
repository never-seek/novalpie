package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.*
import java.io.File
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Android's real Deflater/file staging, with no online authorization or real novel content. */
class EpubCompressionDeviceTest {
    @Test fun levelsZeroAndNineDifferWhileExtractedImagesAndChaptersStayIdentical() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DownloadSettingsStore(app)
        val original = store.load()
        val image = "GIF89a".toByteArray() + ByteArray(256*1024) { (it % 8).toByte() }
        val source = "第1章 无损测试\n" + "原图原文不能因为ZIP压缩改变。".repeat(2000) + "\n[图片: https://fixture.invalid/original.gif]"
        val files = listOf(0,9).map { File.createTempFile("qa-zip-$it-", ".epub", app.cacheDir) }
        try {
            for ((i, level) in listOf(0,9).withIndex()) {
                store.save(original.copy(zipCompressionLevel = level, compressImages = false))
                files[i].outputStream().use { out ->
                    NativeEpubArchiveWriter.write(out, NativeEpubMetadata("压缩对比", "测试"), StringReader(source),
                        openAsset = { NativeEpubAsset("image/gif",image.inputStream()) },
                        zipCompressionLevel = store.load().zipCompressionLevel)
                }
            }
            ZipFile(files[0]).use { zero -> ZipFile(files[1]).use { nine ->
                for (entry in zero.entries().asSequence()) {
                    assertEquals(ZipEntry.STORED, entry.method)
                    val compressed = nine.getEntry(entry.name)
                    assertEquals(if (entry.name == "mimetype") ZipEntry.STORED else ZipEntry.DEFLATED, compressed.method)
                    assertArrayEquals(zero.getInputStream(entry).use { it.readBytes() }, nine.getInputStream(compressed).use { it.readBytes() })
                }
                assertArrayEquals(image, nine.getInputStream(nine.getEntry("OEBPS/images/image-1.gif")).use { it.readBytes() })
            } }
            assertTrue(files[1].length() < files[0].length()/3)
            File(app.getExternalFilesDir(null), "epub-compression-verified.json").writeText(JSONObject()
                .put("level0Bytes",files[0].length()).put("level9Bytes",files[1].length())
                .put("allExtractedBytesIdentical",true).put("networkRequests",0).toString(2))
        } finally { store.save(original); files.forEach(File::delete) }
    }
}
