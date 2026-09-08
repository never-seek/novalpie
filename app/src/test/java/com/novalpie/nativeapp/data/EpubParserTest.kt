package com.novalpie.nativeapp.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubParserTest {
    private fun minimal(body: String, secondBody: String? = null, secondSpine: Boolean = false, navigation: String? = null, ncx: Boolean = false): UploadFileSource {
        val entries = mutableListOf(
            "META-INF/container.xml" to """<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OPS/content.opf"/></rootfiles></container>""",
            "OPS/content.opf" to """<package xmlns="http://www.idpf.org/2007/opf"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Test</dc:title><dc:creator>Author</dc:creator></metadata><manifest><item id="one" href="one.xhtml" media-type="application/xhtml+xml"/><item id="two" href="two.xhtml" media-type="application/xhtml+xml"/>${if (navigation != null) """<item id="nav" href="nav.xhtml" media-type="${if (ncx) "application/x-dtbncx+xml" else "application/xhtml+xml"}" properties="${if (ncx) "" else "nav"}"/>""" else ""}</manifest><spine><itemref idref="one"/>${if (secondSpine) """<itemref idref="two"/>""" else ""}</spine></package>""",
            "OPS/one.xhtml" to body,
        )
        if (secondBody != null) entries += "OPS/two.xhtml" to secondBody
        if (navigation != null) entries += "OPS/nav.xhtml" to navigation
        val bytes = epubBytes(*entries.toTypedArray())
        return UploadFileSource("test.epub", bytes.size.toLong(), "application/epub+zip") { ByteArrayInputStream(bytes) }
    }

    @Test fun missingSpineChapterIsAnErrorNotASuccessfulPartialBook() {
        val source = minimal("<html><body><p>First</p></body></html>", secondSpine = true)
        assertTrue("不能静默跳过目录中缺失的章", runCatching { EpubParser.parse(source) }.isFailure)
    }

    @Test fun headMetadataIsExcludedAndEntitiesAreDecodedExactlyOnce() {
        val source = minimal("<html><head><title>Header must not enter prose</title><style>p{color:red}</style></head><body><p>&amp;lt; &#x1F600; &#20013; &apos;quoted&apos;</p></body></html>")
        assertEquals("&lt; 😀 中 'quoted'", EpubParser.parse(source).chapters.single().content)
    }

    @Test fun navigationFragmentsSplitOneSpineFileIntoSeparateChapters() {
        val source = minimal("<html><body><h2 id='a'>第一章</h2><p>甲正文</p><h2 id='b'>第二章</h2><p>乙正文</p></body></html>",
            navigation = """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol><li><a href="one.xhtml#a">第一章</a></li><li><a href="one.xhtml#b">第二章</a></li></ol></nav></body></html>""")
        val chapters = EpubParser.parse(source).chapters
        assertEquals(listOf("第一章", "第二章"), chapters.map { it.title })
        assertTrue(chapters[0].content.contains("甲正文")); assertTrue(!chapters[0].content.contains("乙正文"))
        assertTrue(chapters[1].content.contains("乙正文")); assertTrue(!chapters[1].content.contains("甲正文"))
        assertEquals(listOf(1, 2), chapters.map { it.chapterNumber })
        assertEquals(listOf("OPS/one.xhtml", "OPS/one.xhtml"), chapters.map { it.rawPath })
    }

    @Test fun brokenNavigationAnchorMustNotDuplicateTheEntireChapter() {
        val source = minimal("<html><body><p>只有一个正文</p></body></html>", navigation = """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol><li><a href="one.xhtml#missing">不存在的锚点</a></li></ol></nav></body></html>""")
        assertTrue("目录锚点不符时不能提交错误拆章", runCatching { EpubParser.parse(source) }.isFailure)
    }

    @Test fun legacyNcxKeepsItsTitlesAndSharedFileFragmentOrder() {
        val source = minimal("<html><body><p id='a'>甲</p><p id='b'>乙</p></body></html>", ncx = true,
            navigation = """<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/"><navMap><navPoint><navLabel><text>甲章</text></navLabel><content src="one.xhtml#a"/></navPoint><navPoint><navLabel><text>乙章</text></navLabel><content src="one.xhtml#b"/></navPoint></navMap></ncx>""")
        val chapters = EpubParser.parse(source).chapters
        assertEquals(listOf("甲章", "乙章"), chapters.map { it.title })
        assertEquals(listOf("甲", "乙"), chapters.map { it.content })
    }

    @Test fun volumeNavigationAndUnlistedContinuationFileKeepProseAndHierarchy() {
        val source = minimal("<html><body><p>甲文件正文</p></body></html>", "<html><body><p>续页正文</p></body></html>", true,
            navigation = """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol><li><span>第一卷</span><ol><li><a href="one.xhtml">第一章</a></li></ol></li></ol></nav></body></html>""")
        val chapter = EpubParser.parse(source).chapters.single()
        assertEquals("第一卷 / 第一章", chapter.title)
        assertEquals(listOf("第一卷"), chapter.sectionPath)
        assertEquals(1, chapter.hierarchyLevel)
        assertEquals("甲文件正文\n\n续页正文", chapter.content)
        assertEquals(0, chapter.spineIndex)
    }

    @Test fun unknownPrefaceIsPreservedBeforeTheFirstNavigationAnchor() {
        val source = minimal("<html><body><p>未列出的序言</p><p id='start'>真正第一章</p></body></html>",
            navigation = """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol><li><a href="one.xhtml#start">第一章</a></li></ol></nav></body></html>""")
        assertEquals(listOf("未列出的序言", "真正第一章"), EpubParser.parse(source).chapters.map { it.content })
    }

    @Test fun quotedAngleBracketAttributesAndCommentsDoNotBecomeProse() {
        val source = minimal("""<html><body><!-- <h1>不应出现</h1> --><p title="x > y">甲 &amp;#20013;</p></body></html>""")
        assertEquals("甲 &#20013;", EpubParser.parse(source).chapters.single().content)
    }
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
