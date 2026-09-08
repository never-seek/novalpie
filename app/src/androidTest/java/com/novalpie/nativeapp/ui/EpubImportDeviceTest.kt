package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.EpubParser
import com.novalpie.nativeapp.data.EpubWriter
import com.novalpie.nativeapp.data.UploadFileSource
import com.novalpie.nativeapp.model.EditorBookMetadata
import com.novalpie.nativeapp.model.UploadChapter
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Real Android XML parsing and EPUB roundtrip, entirely synthetic and never uploaded. */
class EpubImportDeviceTest {
    @Test fun androidXmlParserPreservesExportedProseAndSplitsSharedFileNavigation() {
        val writerOutput = ByteArrayOutputStream()
        val authored = listOf(UploadChapter("唯一章名甲", "甲 &lt; 😀 '原文'\n\n末尾正文", 1), UploadChapter("唯一章名乙", "第二章末尾", 2))
        EpubWriter.write(writerOutput, EditorBookMetadata(title = "受控往返书", author = "合成作者"), authored)
        fun source(bytes: ByteArray) = UploadFileSource("synthetic.epub", bytes.size.toLong(), "application/epub+zip") { ByteArrayInputStream(bytes) }
        val parsed = EpubParser.parse(source(writerOutput.toByteArray()))
        assertEquals(authored.map { it.title }, parsed.chapters.map { it.title })
        authored.zip(parsed.chapters).forEach { (old, new) ->
            assertTrue(new.content.endsWith(old.content))
            assertEquals("head标题不能再进入正文", 1, Regex(Regex.escape(old.title)).findAll(new.content).count())
        }
        val output = ByteArrayOutputStream()
        val entries = mapOf(
            "META-INF/container.xml" to """<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OPS/content.opf"/></rootfiles></container>""",
            "OPS/content.opf" to """<package xmlns="http://www.idpf.org/2007/opf"><metadata/><manifest><item id="text" href="text.xhtml" media-type="application/xhtml+xml"/><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/></manifest><spine><itemref idref="text"/></spine></package>""",
            "OPS/nav.xhtml" to """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol><li><a href="text.xhtml#one">甲章</a></li><li><a href="text.xhtml#two">乙章</a></li></ol></nav></body></html>""",
            "OPS/text.xhtml" to """<html><head><title>隐藏元信息</title></head><body><p id="one">甲完整正文&#x1F600;</p><p id="two">乙完整正文</p></body></html>""",
        )
        ZipOutputStream(output).use { zip -> entries.forEach { (name, value) -> zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray()); zip.closeEntry() } }
        val divided = EpubParser.parse(source(output.toByteArray()))
        assertEquals(listOf("甲完整正文😀", "乙完整正文"), divided.chapters.map { it.content })
        assertEquals(listOf("OPS/text.xhtml", "OPS/text.xhtml"), divided.chapters.map { it.rawPath })
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "beta7-epub-import").apply { mkdirs() }
        File(folder, "import.json").writeText(JSONObject().put("passed", true).put("roundtripChapters", parsed.chapters.size)
            .put("fragmentChapters", divided.chapters.size).put("headExcluded", true).put("textPreserved", true)
            .put("remoteWrites", 0).toString(2))
    }
}
