package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ReaderTextTest {
    @Test
    fun readerParagraphsDecodeHtmlEntitiesAndPreserveParagraphBreaks() {
        val paragraphs = readerParagraphsFromContent(
            """
            <p>First&nbsp;line &amp; title</p>
            <p>Second<br>line</p>
            <div>Third&nbsp;&nbsp;line</div>
            """.trimIndent()
        )

        assertEquals(
            listOf(
                "First line & title",
                "Second\nline",
                "Third line"
            ),
            paragraphs
        )
    }

    @Test
    fun readerParagraphsIgnoreBlankMarkupButKeepPlainTextFallback() {
        assertEquals(emptyList<String>(), readerParagraphsFromContent("<p>&nbsp;</p><br>"))
        assertEquals(listOf("Plain text line"), readerParagraphsFromContent(" Plain text line "))
    }

    @Test
    fun plainTextLineBreaksBecomeReaderParagraphsButHtmlBreaksStayInsideOneParagraph() {
        assertEquals(
            listOf("First paragraph", "Second paragraph", "Third paragraph"),
            readerParagraphsFromContent("First paragraph\nSecond paragraph\r\nThird paragraph")
        )
        assertEquals(
            listOf("First line\nSecond line"),
            readerParagraphsFromContent("<p>First line<br>Second line</p>")
        )
    }

    @Test
    @Config(sdk = [23])
    fun readerParagraphsUseTheAndroid6HtmlCompatibilityPath() {
        assertEquals(
            listOf("First line & title"),
            readerParagraphsFromContent("<p>First&nbsp;line &amp; title</p>")
        )
    }

    @Test
    fun readerBlocksPreserveTextAndIllustrationsInOrder() {
        val blocks = readerBlocksFromContent(
            """
            <p>Before image</p>
            <div class="image-wrapper"><img data-src="/uploads/chapters/one.webp" alt="Illustration one"></div>
            <p>After image</p>
            ![Second](https://cdn.example.test/two.png)
            """.trimIndent()
        )

        assertEquals(4, blocks.size)
        assertEquals("Before image", (blocks[0] as ReaderContentBlock.Text).value)
        assertEquals("https://novalpie.cc/uploads/chapters/one.webp", (blocks[1] as ReaderContentBlock.Image).url)
        assertEquals("Illustration one", (blocks[1] as ReaderContentBlock.Image).alt)
        assertTrue((blocks[2] as ReaderContentBlock.Text).value.contains("After image"))
        assertEquals("https://cdn.example.test/two.png", (blocks[3] as ReaderContentBlock.Image).url)
    }

    @Test
    fun readerBlocksKeepTheInlineOuterImageAndItsOriginalPreviewImageSeparate() {
        val blocks = readerBlocksFromContent(
            "<img src=\"/covers/outer.file\" data-original=\"/covers/inner.file\" alt=\"Layered image\">"
        )

        val image = blocks.single() as ReaderContentBlock.Image
        assertEquals("https://novalpie.cc/covers/outer.file", image.url)
        assertEquals("https://novalpie.cc/covers/inner.file", image.originalUrl)
        assertEquals("Layered image", image.alt)
    }

    @Test
    fun readerBlocksResolveWebsiteImagePlaceholdersFromIllustrationList() {
        val blocks = readerBlocksFromContent(
            raw = "<p>Before</p>[[img:2]]<p>After</p>",
            imagePlaceholders = mapOf(
                2 to ReaderContentBlock.Image(
                    url = "/uploads/chapters/two.webp",
                    alt = "Chapter image 2"
                )
            )
        )

        assertEquals(3, blocks.size)
        assertEquals("Before", (blocks[0] as ReaderContentBlock.Text).value)
        assertEquals("https://novalpie.cc/uploads/chapters/two.webp", (blocks[1] as ReaderContentBlock.Image).url)
        assertEquals("Chapter image 2", (blocks[1] as ReaderContentBlock.Image).alt)
        assertEquals("After", (blocks[2] as ReaderContentBlock.Text).value)
    }

    @Test
    fun readerBlocksKeepIllustrationsWhenSourceContentOmitsPlaceholders() {
        val blocks = readerBlocksFromContent(
            raw = "<p>Translated text without image tokens</p>",
            imagePlaceholders = mapOf(
                2 to ReaderContentBlock.Image("/uploads/chapters/two.webp", "Second"),
                1 to ReaderContentBlock.Image("/uploads/chapters/one.webp", "First")
            )
        )

        assertEquals(3, blocks.size)
        assertEquals("Translated text without image tokens", (blocks[0] as ReaderContentBlock.Text).value)
        assertEquals("https://novalpie.cc/uploads/chapters/one.webp", (blocks[1] as ReaderContentBlock.Image).url)
        assertEquals("https://novalpie.cc/uploads/chapters/two.webp", (blocks[2] as ReaderContentBlock.Image).url)
    }

    @Test
    fun readerBlocksPreserveRepeatedAuthoredImagesButDoNotAppendPlacedPlaceholderAgain() {
        val blocks = readerBlocksFromContent(
            raw = """
                <p>Before</p>
                <img src="/uploads/chapters/one.webp">
                ![same image](https://novalpie.cc/uploads/chapters/one.webp)
                [[img:1]]
                <p>After</p>
            """.trimIndent(),
            imagePlaceholders = mapOf(
                1 to ReaderContentBlock.Image("/uploads/chapters/one.webp", "One")
            )
        )

        assertEquals(5, blocks.size)
        assertEquals("Before", (blocks[0] as ReaderContentBlock.Text).value)
        assertEquals("https://novalpie.cc/uploads/chapters/one.webp", (blocks[1] as ReaderContentBlock.Image).url)
        assertEquals("https://novalpie.cc/uploads/chapters/one.webp", (blocks[2] as ReaderContentBlock.Image).url)
        assertEquals("https://novalpie.cc/uploads/chapters/one.webp", (blocks[3] as ReaderContentBlock.Image).url)
        assertEquals("After", (blocks[4] as ReaderContentBlock.Text).value)
    }

    @Test
    fun readerBlocksAppendOnlyUnplacedServerIllustrations() {
        val blocks = readerBlocksFromContent(
            raw = "<p>Before</p>[[img:1]]<p>After</p>",
            imagePlaceholders = mapOf(
                1 to ReaderContentBlock.Image("/uploads/chapters/one.webp", "One"),
                2 to ReaderContentBlock.Image("/uploads/chapters/two.webp", "Two"),
            ),
        )

        assertEquals(4, blocks.size)
        assertEquals("https://novalpie.cc/uploads/chapters/one.webp", (blocks[1] as ReaderContentBlock.Image).url)
        assertEquals("After", (blocks[2] as ReaderContentBlock.Text).value)
        assertEquals("https://novalpie.cc/uploads/chapters/two.webp", (blocks[3] as ReaderContentBlock.Image).url)
    }

    @Test
    fun duplicateLineCleanupOnlyRemovesAdjacentRepeatedTextBlocks() {
        val blocks = listOf(
            ReaderContentBlock.Text("same"),
            ReaderContentBlock.Text("same"),
            ReaderContentBlock.Image("https://example.test/a.webp"),
            ReaderContentBlock.Text("same"),
        )

        assertEquals(4, readerBlocksForDisplay(blocks, removeDuplicateLines = false).size)
        assertEquals(
            listOf(
                ReaderContentBlock.Text("same"),
                ReaderContentBlock.Image("https://example.test/a.webp"),
                ReaderContentBlock.Text("same"),
            ),
            readerBlocksForDisplay(blocks, removeDuplicateLines = true),
        )
    }

    @Test
    fun ttsSegmentsStayBoundedAndPreferSentenceBoundaries() {
        val segments = readerTtsSegments(
            listOf("第一句。 第二句！ 第三句？"),
            maxLength = 8,
        )

        assertEquals(listOf("第一句。", "第二句！", "第三句？"), segments)
        assertTrue(segments.all { it.length <= 8 })
    }

    @Test
    fun ttsSegmentsHardSplitAnUnbrokenLongParagraph() {
        val segments = readerTtsSegments(listOf("a".repeat(201)), maxLength = 80)

        assertEquals(3, segments.size)
        assertEquals(80, segments[0].length)
        assertEquals(41, segments.last().length)
    }
}
