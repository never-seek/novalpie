package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.util.Base64
import java.util.UUID
import java.util.zip.ZipInputStream

/** Actual Android file staging + ZIP construction; no remote image requests or point usage. */
class DownloadImageTypeDeviceTest {
    @Test fun opaqueFileUrlUsesActualPngBytesAndHtmlErrorsAreNotPublished() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(app.cacheDir, "beta7-image-types-${UUID.randomUUID()}").canonicalFile.apply { mkdirs() }
        val png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WlqVwAAAABJRU5ErkJggg==")
        try {
            val output = ByteArrayOutputStream()
            NativeEpubArchiveWriter.write(output, NativeEpubMetadata("合成格式测试", "测试"), StringReader("第1章 测试\n前文\n[图片: https://fixture.invalid/cover.file]\n后文"),
                openAsset = { NativeEpubAsset("image/webp", png.inputStream()) }, stagingDirectory = root)
            val entries = linkedMapOf<String, ByteArray>()
            ZipInputStream(output.toByteArray().inputStream()).use { zip -> while (true) {
                val next = zip.nextEntry ?: break
                entries[next.name] = zip.readBytes()
            } }
            assertArrayEquals(png, entries.getValue("OEBPS/images/image-1.png"))
            assertTrue(entries.getValue("OEBPS/content.opf").toString(Charsets.UTF_8).contains("media-type=\"image/png\""))
            assertFalse(entries.keys.any { it.endsWith(".webp") || it.endsWith(".file") })
            val failed = File(root, "never-publish.asset")
            assertTrue(runCatching { stageNativeEpubFile("<html>Temporary error</html>".byteInputStream(), "image/png", failed) }.isFailure)
            assertFalse(failed.exists())
            val proof = File(app.cacheDir, "beta7-image-type-report").apply { mkdirs() }
            File(proof, "types.json").writeText(JSONObject().put("passed", true).put("originalBytesPreserved", true)
                .put("wrongHeaderOverridden", true).put("opaqueFileUrlSupported", true).put("htmlRejectedBeforePublication", true)
                .put("networkRequests", 0).toString(2))
        } finally {
            check(root.parentFile == app.cacheDir.canonicalFile && root.name.startsWith("beta7-image-types-"))
            root.deleteRecursively()
        }
        Unit
    }
}
