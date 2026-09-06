package com.novalpie.nativeapp.feature.reader.tts

import com.novalpie.nativeapp.data.ReaderTtsSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPlaybackCoordinatorTest {
    private fun chapter(id: Long,next:Long?=null)=SpeechChapter(1,id,"测试书","第$id 章",listOf("首句$id","次句$id"),next)

    @Test fun isolatedPreviewPlaybackDoesNotWriteAReadersRealProgress()=runTest {
        var writes=0
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope,onPosition={_,_->writes++})
        coordinator.start(chapter(10).copy(recordProgress=false),ReaderTtsSettings())
        engine.segment(0)
        assertEquals(SpeechStatus.Speaking,coordinator.state.value.status)
        assertEquals(0,writes)
    }

    @Test fun progressOnlyTracksAnActuallyStartedUtteranceAndNeverAPreloadOrLateCallback()=runTest {
        val seen=mutableListOf<Pair<Long,Int>>()
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope,onPosition={value,index->seen+=value.chapterId to index})
        coordinator.start(chapter(10,11),ReaderTtsSettings(enableAutoNextChapter=true))
        assertTrue(seen.isEmpty())
        engine.segment(0)
        engine.finish();runCurrent()
        assertEquals(listOf(10L to 0),seen)
        engine.segment(0)
        val old=engine.calls.last()
        coordinator.stop();old.onSegment(1)
        assertEquals(listOf(10L to 0,11L to 0),seen)
    }

    @Test fun togglingVisualFollowPreferencesDoesNotPauseOrFlushAnActiveVoice()=runTest {
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope)
        val settings=ReaderTtsSettings(enableHighlight=true,enableAutoScroll=true)
        coordinator.start(chapter(10,11),settings)
        engine.segment(0)
        coordinator.updateSettings(settings.copy(enableHighlight=false,enableAutoScroll=false,enableAutoNextChapter=false))
        assertEquals(SpeechStatus.Speaking,coordinator.state.value.status)
        assertEquals(1,engine.calls.size)
        engine.finish();runCurrent()
        assertEquals(SpeechStatus.Stopped,coordinator.state.value.status)
        assertEquals(1,engine.calls.size)
    }

    @Test fun deniedAudioFocusNeverStartsTheEngineButKeepsAnExplicitResumePoint()=runTest {
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope)
        coordinator.preparePaused(chapter(10),ReaderTtsSettings(),1,"等待音频焦点")
        assertEquals(SpeechStatus.Paused,coordinator.state.value.status)
        assertTrue(engine.calls.isEmpty())
        coordinator.resume()
        assertEquals(1,engine.calls.single().startIndex)
    }

    @Test fun completionLoadsTheRealNextChapterAndStartsAtItsFirstSentence()=runTest {
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope)
        coordinator.start(chapter(10,11),ReaderTtsSettings(enableAutoNextChapter=true))
        engine.segment(0)
        assertEquals(0,coordinator.state.value.segmentIndex)
        engine.finish()
        runCurrent()
        assertEquals(11L,coordinator.state.value.chapter?.chapterId)
        assertEquals("首句11",engine.calls.last().segments.first())
        assertEquals(0,engine.calls.last().startIndex)
    }

    @Test fun stopInvalidatesLateEngineAndChapterFetchCallbacks()=runTest {
        val pending=CompletableDeferred<SpeechChapter>()
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,_->withContext(NonCancellable){pending.await()}},backgroundScope)
        coordinator.start(chapter(10,11),ReaderTtsSettings(enableAutoNextChapter=true))
        val old=engine.calls.last()
        engine.finish()
        runCurrent()
        coordinator.stop()
        old.onSegment(1)
        old.onFinished()
        pending.complete(chapter(11))
        runCurrent()
        assertEquals(SpeechStatus.Stopped,coordinator.state.value.status)
        assertEquals(1,engine.calls.size)
    }

    @Test fun pauseResumeUsesTheCurrentSentenceAndANewCallbackGeneration()=runTest {
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope)
        coordinator.start(chapter(10),ReaderTtsSettings())
        engine.segment(1)
        val previous=engine.calls.last()
        coordinator.pause()
        assertEquals(SpeechStatus.Paused,coordinator.state.value.status)
        previous.onFinished()
        coordinator.resume()
        assertEquals(1,engine.calls.last().startIndex)
        previous.onError("late")
        assertNotEquals(SpeechStatus.Error,coordinator.state.value.status)
    }

    @Test fun audioInterruptionPausesAndDoesNotAutomaticallyRestartWithoutUserIntent()=runTest {
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope)
        coordinator.start(chapter(10),ReaderTtsSettings())
        engine.segment(0)
        coordinator.interrupt("耳机已断开")
        assertEquals(SpeechStatus.Paused,coordinator.state.value.status)
        assertEquals("耳机已断开",coordinator.state.value.message)
        assertEquals(1,engine.calls.size)
    }

    @Test fun replacementRevisionRequeuesDerivedTextAndDropsTheOldSentence()=runTest {
        val engine=FakeEngine()
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->chapter(id)},backgroundScope)
        val source=chapter(10)
        coordinator.start(source,ReaderTtsSettings())
        val old=engine.calls.last()
        coordinator.replaceChapter(source.copy(segments=listOf("替换后的首句","次句"),textRevision=2))
        old.onSegment(1)
        assertEquals("替换后的首句",engine.calls.last().segments.first())
        assertEquals(0,coordinator.state.value.segmentIndex)
    }

    @Test fun sourceFailureIsRetryableWithoutSpeakingPlaceholderText()=runTest {
        val engine=FakeEngine()
        var fail=true
        val coordinator=TtsPlaybackCoordinator(engine,SpeechChapterSource{_,id->if(fail)error("网络断开")else chapter(id)},backgroundScope)
        coordinator.start(chapter(10,11),ReaderTtsSettings(enableAutoNextChapter=true))
        engine.finish()
        runCurrent()
        assertEquals(SpeechStatus.Error,coordinator.state.value.status)
        assertEquals(1,engine.calls.size)
        fail=false
        coordinator.retry()
        runCurrent()
        assertEquals("首句11",engine.calls.last().segments.first())
    }

    private class FakeEngine:SpeechEngine {
        data class Call(val segments:List<String>,val startIndex:Int,val onSegment:(Int)->Unit,val onFinished:()->Unit,val onError:(String)->Unit)
        val calls=mutableListOf<Call>()
        override fun start(segments:List<String>,startIndex:Int,settings:ReaderTtsSettings,onSegment:(Int)->Unit,onFinished:()->Unit,onError:(String)->Unit) {calls+=Call(segments,startIndex,onSegment,onFinished,onError)}
        override fun stop()=Unit
        override fun close()=Unit
        fun segment(index:Int)=calls.last().onSegment(index)
        fun finish()=calls.last().onFinished()
    }
}
