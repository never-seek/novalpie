package com.novalpie.nativeapp.feature.reader.tts

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.ReaderProgressStore
import com.novalpie.nativeapp.feature.reader.pagination.ReaderAnchorStore
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechProgressRecorderTest {
    @Test fun actualSpeechSavesBookChapterParagraphAndOnlySynchronizesOncePerChapter()=runTest {
        val server=MockWebServer();server.start()
        server.enqueue(MockResponse().setBody("""{"success":true}"""))
        try {
            val app=ApplicationProvider.getApplicationContext<Application>()
            val recorder=SpeechProgressRecorder(app,NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/')),this){1L}
            val chapter=SpeechChapter(991,992,"测试听书","第二章",listOf("第一句","第二句"),positions=listOf(
                SpeechTextPosition("992:p:0",1,0),SpeechTextPosition("992:p:0",1,3)),chapterNumber=2,chapterCount=5)
            recorder.record(chapter,0);recorder.record(chapter,1)
            runCurrent()
            coroutineContext[Job]!!.children.toList().joinAll()
            val progress=ReaderProgressStore(app).load(991)!!
            assertEquals(992L,progress.chapterId)
            assertEquals("测试听书",progress.bookTitle)
            assertEquals(2,progress.chapterNumber)
            assertEquals(3,ReaderAnchorStore(app).load(991,992)?.textOffset)
            assertEquals(1,server.requestCount)
        }finally{server.shutdown()}
    }
}
