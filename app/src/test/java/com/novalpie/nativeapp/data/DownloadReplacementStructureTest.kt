package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.ui.ReaderReplacementState
import com.novalpie.nativeapp.ui.readerDownloadReplacementSnapshot
import com.novalpie.nativeapp.ui.toNativeDownloadText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

class DownloadReplacementStructureTest {
    @Test fun txtKeepsVisibleReplacementCharactersWithoutHtmlEscapes() = runBlocking {
        val snapshot = readerDownloadReplacementSnapshot(true, ReaderReplacementState(novelId = 1,
            personalRules = listOf(ReaderReplacementRule("r", 1, "Alice", "<em>不是标签</em> **保留星号**"))))
        val output = java.io.StringWriter()
        NativeEpubArchiveWriter.writeTransformedTxt(output, StringReader("第1章\nAlice\n[图片: https://host.test/Alice.png]"),
            transformChapter = { order, title, body -> snapshot.transform(order, title, body).toNativeDownloadText() })
        assertTrue(output.toString().contains("<em>不是标签</em> **保留星号**"))
        assertFalse(output.toString().contains("&lt;"))
        assertTrue(output.toString().contains("[图片: https://host.test/Alice.png]"))
    }
    @Test fun replacementCannotCreateAnAdditionalDownloadIllustration() = runBlocking {
        val snapshot = readerDownloadReplacementSnapshot(true, ReaderReplacementState(novelId = 1,
            personalRules = listOf(ReaderReplacementRule("r", 1, "Alice", "[图片: https://host.test/not-an-image.png]"))))
        val output = ByteArrayOutputStream()
        var requested = 0
        NativeEpubArchiveWriter.write(output, NativeEpubMetadata("测试", "作者"), StringReader("第1章\nAlice"),
            openAsset = { requested++; error("No authored illustration") },
            transformChapter = { order, title, body -> snapshot.transform(order, title, body).toNativeDownloadText() },
        )
        assertEquals(0, requested)
        val entries = mutableMapOf<String, String>()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; if (entry.name.endsWith("xhtml")) entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8); zip.closeEntry() }
        }
        assertTrue(entries.getValue("OEBPS/chapter-1.xhtml").contains("[图片: https://host.test/not-an-image.png]"))
    }
}
