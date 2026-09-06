package com.novalpie.nativeapp.feature.reader.pagination

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.novalpie.nativeapp.feature.reader.text.readerAnnotatedTextWithWordSpacing
import com.novalpie.nativeapp.feature.reader.text.readerSpokenTextRange
import org.junit.Assert.*
import org.junit.Test

class ReaderDisplayTextTest {
    @Test fun wordSpacingRemapsBoldRangesAndDoesNotDestroyTheUnmodifiedSource() {
        val input=AnnotatedString("one two three",listOf(AnnotatedString.Range(SpanStyle(fontWeight=FontWeight.Bold),4,7)))
        val transformed=readerAnnotatedTextWithWordSpacing(input,6f)
        assertEquals("one\u200A\u200A\u200Atwo\u200A\u200A\u200Athree",transformed.text)
        assertEquals(6,transformed.spanStyles.single().start)
        assertEquals(9,transformed.spanStyles.single().end)
        assertEquals("one two three",input.text)
        val tight=readerAnnotatedTextWithWordSpacing(input,-2f)
        assertEquals("onetwothree",tight.text)
        assertEquals(3,tight.spanStyles.single().start)
        assertEquals(6,tight.spanStyles.single().end)
    }

    @Test fun spokenWhitespaceNormalizationStillLocatesTheExactVisibleCharacters() {
        val text="前文。one\u200A\u200A\u200Atwo\n\n接着继续。后文"
        val range=readerSpokenTextRange(text,"one two 接着继续。")!!
        assertEquals("one\u200A\u200A\u200Atwo\n\n接着继续。",text.substring(range))
        assertNull(readerSpokenTextRange(text,"不存在的句子"))
        assertNull(readerSpokenTextRange(text,"   "))
    }

    @Test fun insertedParagraphBeforeAnUnchangedTargetDoesNotMoveTheAnchorToAnotherParagraph() {
        fun paragraph(id:String,text:String)=ChapterDocumentBlock.Paragraph(id,AnnotatedString(text))
        val before=ChapterDocument(1,2,"source","old",listOf(paragraph("2:p:0","上文"),paragraph("2:p:1","保持这里的阅读位置")))
        val after=before.copy(revision="new",blocks=listOf(paragraph("2:p:0","增加"),paragraph("2:p:1","上文"),paragraph("2:p:2","保持这里的阅读位置")))
        val remapped=remapReaderAnchor(ReaderAnchor(1,2,"2:p:1",4),before,after)
        assertEquals(ReaderAnchor(1,2,"2:p:2",4),remapped)
    }

    @Test fun splittingAParagraphByReplacementPreservesItsTextOffsetInTheNewParagraph() {
        val before=ChapterDocument(1,2,"source","old",listOf(ChapterDocumentBlock.Paragraph("2:p:0",AnnotatedString("之前。这里是正在阅读的句子。"))))
        val after=before.copy(revision="new",blocks=listOf(
            ChapterDocumentBlock.Paragraph("2:p:0",AnnotatedString("之前。")),
            ChapterDocumentBlock.Paragraph("2:p:1",AnnotatedString("这里是正在阅读的句子。"))))
        assertEquals(ReaderAnchor(1,2,"2:p:1",3),remapReaderAnchor(ReaderAnchor(1,2,"2:p:0",6),before,after))
    }
}
