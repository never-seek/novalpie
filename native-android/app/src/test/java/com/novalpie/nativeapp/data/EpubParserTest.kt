package com.novalpie.nativeapp.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubParserTest {
    @Test
    fun parsesMetadataAndSpineInReadingOrderWithoutLoadingImages() {
        val bytes = epubBytes(
            "META-INF/container.xml" to """
                <?xml version="1.0"?>
                <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
                </container>
            """,
            "OEBPS/content.opf" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="id">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Native Novel</dc:title><dc:creator>Writer</dc:creator>
                    <dc:language>ja</dc:language><dc:description>Short intro</dc:description>
                  </metadata>
                  <manifest>
                    <item id="chapter-two" href="text/two.xhtml" media-type="application/xhtml+xml"/>
                    <item id="chapter-one" href="text/one.xhtml" media-type="application/xhtml+xml"/>
                    <item id="cover" href="images/cover.jpg" media-type="image/jpeg"/>
                  </manifest>
                  <spine><itemref idref="chapter-one"/><itemref idref="chapter-two"/></spine>
                </package>
            """,
            "OEBPS/text/one.xhtml" to "<html><head><title>First</title></head><body><h1>First</h1><p>Hello<br/>world.</p></body></html>",
            "OEBPS/text/two.xhtml" to "<html><head><title>Second</title></head><body><p>Next &amp; final.</p></body></html>",
            "OEBPS/images/cover.jpg" to "not-decoded-image-data"
        )
        val source = UploadFileSource(
            fileName = "book.epub",
            sizeBytes = bytes.size.toLong(),
            contentType = "application/epub+zip",
            openStream = { ByteArrayInputStream(bytes) }
        )

        val parsed = EpubParser.parse(source)

        assertEquals("Native Novel", parsed.title)
        assertEquals("Writer", parsed.author)
        assertEquals("ja", parsed.language)
        assertEquals("Short intro", parsed.description)
        assertEquals(listOf("First", "Second"), parsed.chapters.map { it.title })
        assertEquals(listOf(1, 2), parsed.chapters.map { it.chapterNumber })
        assertTrue(parsed.chapters.first().content.contains("Hello"))
        assertTrue(parsed.chapters.first().content.contains("world."))
        assertEquals("OEBPS/text/one.xhtml", parsed.chapters.first().rawPath)
        assertEquals(0, parsed.chapters.first().spineIndex)
    }

    private fun epubBytes(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.trimIndent().trim().toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
