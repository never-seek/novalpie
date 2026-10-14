package com.novalpie.nativeapp.feature.reader.tts

import androidx.compose.ui.text.AnnotatedString
import com.novalpie.nativeapp.feature.reader.pagination.*
import com.novalpie.nativeapp.feature.reader.text.readerAnnotatedTextWithWordSpacing
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ReaderSpeechFocusTest {
    private val content=ReaderContent("Title","<p>Repeated sentence.</p><img src='https://fixture.invalid/p.png'><p>Middle.</p><p>Repeated sentence.</p>","fixture")
    @Test fun repeatedParagraphFollowsItsStableBlockThroughGlobalOffsets() {
        val options=ReaderUiOptions(showImages=true,showComments=true)
        val layout=readerBodyLayoutForContents(listOf(ReaderChapterContent(9,"Before",ReaderContent("Before","before","fixture")),
            ReaderChapterContent(10,"Title",content)),options).withPreviousChapterControl()
        val speech=buildSpeechChapter(1,10,"QA",content,null,2,2,0)
        val focus=SpeechPlaybackState(SpeechStatus.Speaking,speech,2).readerSpeechFocus()!!
        assertEquals("10:p:3",focus.position!!.blockId)
        val expected=layout.textLocations.single {it.blockId=="10:p:3"}.itemIndex
        assertEquals(expected,readerBodyItemIndexForSpeechFocus(layout,focus))
    }
    @Test fun pagedFollowChoosesSecondIdenticalParagraph() {
        val doc=chapterDocumentFromContent(1,10,content,content,"qa")
        val chapter=buildSpeechChapter(1,10,"QA",content,null,1,1,0)
        val focus=SpeechPlaybackState(SpeechStatus.Speaking,chapter,2).readerSpeechFocus()!!
        assertEquals("10:p:3",readerAnchorForSpeech(doc,focus)!!.blockId)
        assertNull(readerSpeechDisplayRange("Repeated sentence.",focus,10,"10:p:0"))
        assertNotNull(readerSpeechDisplayRange("Repeated sentence.",focus,10,"10:p:3"))
    }
    @Test fun identicalUtterancesWithinSameBlockRespectOffsets() {
        val text="Repeat. Repeat."
        val focus=ReaderSpeechFocus(10,"Repeat.",SpeechTextPosition("10:p:0",1,8,text))
        assertEquals(8..14,readerSpeechDisplayRange(text,focus,10,"10:p:0"))
    }
    @Test fun spacingProjectionDoesNotRewindToFirstOccurrence() {
        val source="a b. a b."
        val display=readerAnnotatedTextWithWordSpacing(AnnotatedString(source),4f).text
        val focus=ReaderSpeechFocus(10,"a b.",SpeechTextPosition("10:p:0",1,5,source))
        val range=readerSpeechDisplayRange(display,focus,10,"10:p:0",wordSpacing=4f)!!
        assertTrue(range.first>display.length/2-1)
        assertEquals("ab.",display.substring(range).filterNot {it.isWhitespace()||Character.isSpaceChar(it)})
    }
    @Test fun conversionAndStaleDocumentAreChecked() {
        val source="读书。读书。"
        val display=convertChineseVariantText(source,ChineseVariant.Traditional)
        val focus=ReaderSpeechFocus(10,"读书。",SpeechTextPosition("10:p:0",1,3,source))
        assertEquals(3..5,readerSpeechDisplayRange(display,focus,10,"10:p:0",ChineseVariant.Traditional))
        assertNull(readerSpeechDisplayRange("Changed by a new rule",focus,10,"10:p:0"))
        assertNull(readerSpeechDisplayRange(display,focus,20,"10:p:0",ChineseVariant.Traditional))
    }
    @Test fun legacyTextOnlyFocusDoesNotGuessBetweenTwoOccurrences() {
        val doc=chapterDocumentFromContent(1,10,content,content,"qa")
        assertNull(readerAnchorForSpeech(doc,ReaderSpeechFocus(10,"Repeated sentence.",null)))
        assertNull(readerSpeechDisplayRange("Repeat. Repeat.",ReaderSpeechFocus(10,"Repeat.",null),10,"10:p:0"))
    }
}
