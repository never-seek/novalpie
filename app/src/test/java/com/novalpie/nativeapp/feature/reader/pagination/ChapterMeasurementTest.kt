package com.novalpie.nativeapp.feature.reader.pagination

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChapterMeasurementTest {
    @get:Rule val compose = createComposeRule()

    @Test fun imageVisibilityAndDuplicateParagraphPreferencesRemainIndependentOfPagination() {
        val raw=com.novalpie.nativeapp.model.ReaderContent("标题","<p>段落</p><p>段落</p><img src='https://image.test/a.png'><p>末尾</p>","test")
        val complete=chapterDocumentFromContent(1,2,raw,raw,"a")
        val hidden=chapterDocumentFromContent(1,2,raw,raw,"b",showImages=false,removeDuplicateLines=true)
        assertEquals(1,complete.blocks.filterIsInstance<ChapterDocumentBlock.Image>().size)
        assertEquals(0,hidden.blocks.filterIsInstance<ChapterDocumentBlock.Image>().size)
        assertEquals(1,hidden.blocks.filterIsInstance<ChapterDocumentBlock.Paragraph>().count {it.text.text=="段落"})
        assertEquals(raw.content,hidden.originalContent)
    }

    @Test fun actualComposeLineMeasurementCoversCjkEmojiAndStyledLongParagraphExactlyOnce() {
        lateinit var measurer: TextMeasurer
        compose.setContent { val actual = rememberTextMeasurer(); SideEffect { measurer = actual } }
        val source = "这是带有加粗文字与 emoji 的长段落🙂。".repeat(35)
        val text = AnnotatedString(source, listOf(AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 2, 12)))
        compose.runOnIdle {
            val document = ChapterDocument(10, 20, "原始文本未改", "revision-1", listOf(ChapterDocumentBlock.Paragraph("p", text)))
            val key = PageLayoutKey(10,20,"revision-1",180,120,"20sp","none")
            val measured = measureChapterDocument(document, key, measurer, TextStyle(fontSize = 20.sp,lineHeight = 28.sp))
            val slices = measured.plan.pages.flatMap { it.fragments }.filterIsInstance<PageFragment.Text>()
            assertTrue(slices.size > 3)
            assertEquals(0, slices.first().startOffset)
            assertEquals(source.length, slices.last().endOffset)
            slices.zipWithNext().forEach { (a,b) -> assertEquals(a.endOffset,b.startOffset) }
            slices.forEach { slice ->
                assertTrue(slice.heightPx <= key.heightPx)
                val actual = measured.textLayouts.getValue(slice.blockId)
                assertEquals(actual.getLineTop(slice.firstLine),slice.sourceTopPx,0.001f)
                assertEquals(actual.getLineEnd(slice.endLineExclusive-1),slice.endOffset)
            }
            assertEquals(source,slices.joinToString(""){source.substring(it.startOffset,it.endOffset)})
            assertEquals(text.spanStyles,measured.textLayouts.getValue("p").layoutInput.text.spanStyles)
        }
    }

    @Test fun lateImageDimensionsRelayoutAroundTheTextAnchorWithoutMutatingOriginalDocument() {
        lateinit var measurer: TextMeasurer
        compose.setContent { val actual = rememberTextMeasurer(); SideEffect { measurer = actual } }
        compose.runOnIdle {
            val document=ChapterDocument(10,20,"untouched","1",listOf(
                ChapterDocumentBlock.Paragraph("a",AnnotatedString("前文。".repeat(15))),
                ChapterDocumentBlock.Image("i","https://image.test/picture.webp"),
                ChapterDocumentBlock.Paragraph("b",AnnotatedString("后文。".repeat(80))),
            ))
            val key=PageLayoutKey(10,20,"1",180,180,"20sp","unknown")
            val style=TextStyle(fontSize=20.sp,lineHeight=28.sp)
            val initial=measureChapterDocument(document,key,measurer,style)
            val anchor=ReaderAnchor(10,20,"b",70)
            val late=measureChapterDocument(document,key.copy(imageRevision="loaded"),measurer,style,
                imageDimensions=mapOf("i" to ImageDimensions(1200,300)))
            val page=late.plan.pages[late.plan.pageForAnchor(anchor)]
            assertTrue(page.fragments.any { it.blockId=="b" && it is PageFragment.Text && anchor.textOffset in it.startOffset until it.endOffset })
            assertEquals("untouched",document.originalContent)
            assertEquals(initial.textLayouts.getValue("b").layoutInput.text,late.textLayouts.getValue("b").layoutInput.text)
        }
    }
}
