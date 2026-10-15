package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import com.novalpie.nativeapp.feature.books.*
import com.novalpie.nativeapp.model.Chapter
import java.util.concurrent.atomic.AtomicLong
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChapterRetranslationProtocolTest {
    @Test fun usesTheCurrentWebsiteV2RouteAndOrdinalArray()=runBlocking {
        MockWebServer().use {server->server.start();server.enqueue(MockResponse().setBody("""{"success":true,"message":"queued"}"""))
            val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))
            assertTrue(api.retranslateNovelChapters(360661,listOf(50,49,50)).success)
            val request=server.takeRequest();assertEquals("POST",request.method)
            assertEquals("/api/v2/novels/360661/chapters/retranslate",request.path)
            assertEquals("[49,50]",JSONObject(request.body.readUtf8()).getJSONArray("chapter_range").toString())
        }
    }
    @Test fun absentAcknowledgementIsNotReportedAsSuccess()=runBlocking {
        MockWebServer().use {server->server.start();server.enqueue(MockResponse().setBody("{}"))
            val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))
            assertTrue(runCatching{api.retranslateNovelChapters(1,listOf(1))}.isFailure)
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun explicitServerRejectionIsPreserved()=runBlocking {
        MockWebServer().use {server->server.start();server.enqueue(MockResponse().setBody("""{"success":false,"message":"no model"}"""))
            val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))
            val response=api.retranslateNovelChapters(1,listOf(1));assertFalse(response.success);assertEquals("no model",response.message)
        }
    }
    @Test fun productionApiTransportCancellationAfterPostMustRetainUncertainGuard()=runBlocking {
        val revision=AtomicLong()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
        MockWebServer().use {server->server.start()
            server.dispatcher=object:Dispatcher() {
                override fun dispatch(request:RecordedRequest)=when(request.requestUrl!!.encodedPath) {
                    "/api/workspace/apis"->MockResponse().setBody("""{"data":[{"id":1,"is_active":1,"healthy":1,"approval_status":"approved"}]}""")
                    "/api/v2/novels/1/chapters/retranslate"->{
                        // The POST reached the server. A proxy change invalidates its response.
                        revision.incrementAndGet();MockResponse().setBody("""{"success":true}""")
                    }
                    else->MockResponse().setResponseCode(404)
                }
            }
            val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'),requestRevisionProvider=revision::get)
            val controller=ChapterRetranslationController(WebsiteChapterRetranslationRepository(api),scope,revision::get)
            try {
                controller.open(1,"QA",listOf(Chapter(901,"EP1",number=1)));controller.submit()
                withTimeout(10_000) {flow {while(true){emit(controller.state.submitting);delay(10)}}.first{!it}}
                assertEquals("The write reached the actual API route",1,server.requestCount)
                assertTrue("Cancelled stale response does not prove the POST failed",controller.state.uncertain)
                controller.transportChanged();controller.open(1,"QA",listOf(Chapter(901,"EP1",number=1)));controller.submit()
                assertTrue(controller.state.uncertain);assertEquals(1,server.requestCount)
            }finally {scope.cancel()}
        }
    }
}
