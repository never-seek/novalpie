package com.novalpie.nativeapp.feature.reader.tts

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.ReaderProgressStore
import com.novalpie.nativeapp.feature.reader.pagination.ReaderAnchorStore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechProgressLatencyTest {
    @Test fun slowWebsiteProgressMustNotBlockLaterLocalParagraphAnchors() = runTest {
        val requested = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requested.countDown()
                release.await(8, TimeUnit.SECONDS)
                return MockResponse().setBody("""{"success":true}""")
            }
        }
        server.start()
        try {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val recorder = SpeechProgressRecorder(app, NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')), scope) { 1L }
            val chapter = SpeechChapter(99801, 99802, "测试", "测试章", listOf("首段", "后段"), positions = listOf(
                SpeechTextPosition("99802:p:0", 1, 0), SpeechTextPosition("99802:p:0", 1, 20)))
            try {
                recorder.record(chapter, 0)
                assertTrue(requested.await(4, TimeUnit.SECONDS))
                recorder.record(chapter, 1)
                withContext(Dispatchers.Default) {
                    withTimeout(1200) { while (ReaderAnchorStore(app).load(99801, 99802)?.textOffset != 20) delay(10) }
                }
                assertEquals(99802L, ReaderProgressStore(app).load(99801)?.chapterId)
                assertEquals(1, server.requestCount)
            } finally { release.countDown(); scope.cancel() }
        } finally { release.countDown(); server.shutdown() }
    }
}
