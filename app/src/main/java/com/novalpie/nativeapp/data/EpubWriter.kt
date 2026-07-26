package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.EditorBookMetadata
import com.novalpie.nativeapp.model.UploadChapter
import java.io.OutputStream
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object EpubWriter {
    private const val MIMETYPE = "application/epub+zip"

    fun write(output: OutputStream, metadata: EditorBookMetadata, chapters: List<UploadChapter>) {
        require(metadata.title.isNotBlank()) { "书名不能为空" }
        require(metadata.author.isNotBlank()) { "作者不能为空" }
        require(chapters.isNotEmpty()) { "至少需要一个章节" }
        ZipOutputStream(output.buffered()).use { zip ->
            writeStoredMimetype(zip)
            writeText(zip, "META-INF/container.xml", containerXml())
            writeText(zip, "OEBPS/content.opf", packageXml(metadata, chapters))
            writeText(zip, "OEBPS/nav.xhtml", navigationXhtml(metadata, chapters))
            chapters.forEachIndexed { index, chapter ->
                writeText(zip, "OEBPS/chapter-${index + 1}.xhtml", chapterXhtml(metadata, chapter))
            }
        }
    }

    private fun writeStoredMimetype(zip: ZipOutputStream) {
        val bytes = MIMETYPE.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(bytes) }
        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            this.crc = crc.value
        }
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeText(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun containerXml(): String = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>"""

    private fun packageXml(metadata: EditorBookMetadata, chapters: List<UploadChapter>): String {
        val manifest = chapters.indices.joinToString("\n") { index ->
            "    <item id=\"chapter-${index + 1}\" href=\"chapter-${index + 1}.xhtml\" media-type=\"application/xhtml+xml\"/>"
        }
        val spine = chapters.indices.joinToString("\n") { index -> "    <itemref idref=\"chapter-${index + 1}\"/>" }
        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="book-id">urn:uuid:${UUID.randomUUID()}</dc:identifier>
    <dc:title>${xml(metadata.title)}</dc:title>
    <dc:creator>${xml(metadata.author)}</dc:creator>
    <dc:language>${xml(metadata.language.ifBlank { "zh" })}</dc:language>
    <dc:description>${xml(metadata.description)}</dc:description>
    <meta property="dcterms:modified">2026-07-10T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
$manifest
  </manifest>
  <spine>
$spine
  </spine>
</package>"""
    }

    private fun navigationXhtml(metadata: EditorBookMetadata, chapters: List<UploadChapter>): String {
        val links = chapters.mapIndexed { index, chapter ->
            "<li><a href=\"chapter-${index + 1}.xhtml\">${xml(chapter.title)}</a></li>"
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml"><head><title>${xml(metadata.title)}</title></head>
<body><nav epub:type="toc" xmlns:epub="http://www.idpf.org/2007/ops"><h1>目录</h1><ol>$links</ol></nav></body></html>"""
    }

    private fun chapterXhtml(metadata: EditorBookMetadata, chapter: UploadChapter): String {
        val paragraphs = chapter.content
            .replace("\r", "")
            .split(Regex("\\n\\s*\\n"))
            .joinToString("\n") { "<p>${xml(it).replace("\n", "<br/>")}</p>" }
        return """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml"><head><title>${xml(chapter.title)}</title></head>
<body><h1>${xml(chapter.title)}</h1>$paragraphs</body></html>"""
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
