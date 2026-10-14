package com.novalpie.nativeapp.feature.reader.tts

import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SpeechIdentityRegressionTest {
    @Test fun ambiguousLegacyTextCannotSelectTheFirstOfRepeatedParagraphs() {
        val layout=readerBodyLayoutForContents(listOf(ReaderChapterContent(10,"Title",
            ReaderContent("Title","<p>Repeated sentence.</p><p>Middle.</p><p>Repeated sentence.</p>","fixture"))),ReaderUiOptions())
        assertNull("A text-only callback cannot identify which repeated paragraph is active",
            readerBodyItemIndexForText(layout,"Repeated sentence.",10))
    }
    @Test fun positionsChangedUnderEqualTextMustUpdatePlaybackDocument() = runTest {
        val engine=Engine()
        val c=TtsPlaybackCoordinator(engine,SpeechChapterSource {_,_->error("no next")},backgroundScope)
        val initial=SpeechChapter(1,10,"QA","Title",listOf("Repeated."),positions=listOf(SpeechTextPosition("10:p:0",1,0)))
        c.start(initial,ReaderTtsSettings());engine.segment(0)
        val generation=c.state.value.generation
        val updated=initial.copy(positions=listOf(SpeechTextPosition("10:p:2",3,0)))
        c.replaceChapter(updated)
        assertEquals(updated.positions,c.state.value.chapter!!.positions)
        assertTrue(c.state.value.generation>generation)
        c.close()
    }
    @Test fun equalTextCatalogUpdateMustUseTheActualNextChapter() = runTest {
        val engine=Engine();val calls=mutableListOf<Long>()
        val c=TtsPlaybackCoordinator(engine,SpeechChapterSource {book,id->calls+=id;SpeechChapter(book,id,"QA","Next",listOf("Actual first sentence."))},backgroundScope)
        val old=SpeechChapter(1,10,"QA","Title",listOf("Body."),nextChapterId=20)
        c.start(old,ReaderTtsSettings(enableAutoNextChapter=true));engine.segment(0)
        c.replaceChapter(old.copy(nextChapterId=30))
        engine.finished();runCurrent()
        assertEquals(listOf(30L),calls)
        assertEquals(30L,c.state.value.chapter!!.chapterId)
        c.close()
    }
    private class Engine:SpeechEngine {
        lateinit var segment:(Int)->Unit;lateinit var finished:()->Unit
        override fun start(segments:List<String>,startIndex:Int,settings:ReaderTtsSettings,onSegment:(Int)->Unit,onFinished:()->Unit,onError:(String)->Unit) {
            segment=onSegment;finished=onFinished
        }
        override fun stop()=Unit
        override fun close()=Unit
    }
}
