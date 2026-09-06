package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderDerivedStructureTest {
    private fun derive(raw: String, source: String, target: String): List<ReaderContentBlock> {
        val rule = ReaderReplacementRule("fixture", 1, source, target)
        val content = ReaderContent(null, raw, "test")
        val chapter = effectiveReaderChapterContent(ReaderChapterContent(2, null, content), 1, ReaderReplacementState(novelId = 1, personalRules = listOf(rule)))
        return readerBlocksForContent(chapter.content)
    }

    @Test fun markdownImageSyntaxIntroducedByReplacementStaysVisibleText() {
        val blocks = derive("Alice", "Alice", "![不是插图](https://host.test/new.png)")
        assertEquals(0, blocks.filterIsInstance<ReaderContentBlock.Image>().size)
        assertEquals("![不是插图](https://host.test/new.png)", blocks.filterIsInstance<ReaderContentBlock.Text>().single().value)
    }

    @Test fun replacementAsterisksAreNotInterpretedAsNewFormatting() {
        val text = derive("Alice", "Alice", "**保留星号**").filterIsInstance<ReaderContentBlock.Text>().single()
        assertEquals("**保留星号**", text.value)
        assertTrue(text.formatted!!.spanStyles.isEmpty())
    }

    @Test fun existingMarkupAndImageOccurrencesAreRetainedAndSpanOffsetsStayCorrect() {
        val blocks = derive("<p>前面 <b>Alice</b> 后面</p><img src='https://host.test/Alice.png'><p>Alice</p>", "Alice", "更长的译名")
        assertEquals("https://host.test/Alice.png", blocks.filterIsInstance<ReaderContentBlock.Image>().single().url)
        val first = blocks.filterIsInstance<ReaderContentBlock.Text>().first()
        assertEquals("前面 更长的译名 后面", first.value)
        val span = first.formatted!!.spanStyles.single()
        assertEquals("更长的译名", first.value.substring(span.start, span.end))
    }

    @Test fun literalComparisonCharactersAndNestedEntitiesDoNotDisappearOrDecodeTwice() {
        val blocks = derive("<p>Alice &amp;lt;保留&amp;gt;</p>", "Alice", "<不是标签>")
        assertEquals("<不是标签> &lt;保留&gt;", blocks.filterIsInstance<ReaderContentBlock.Text>().single().value)
    }
}
