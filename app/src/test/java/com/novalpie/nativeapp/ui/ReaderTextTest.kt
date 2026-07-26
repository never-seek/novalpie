package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
}
