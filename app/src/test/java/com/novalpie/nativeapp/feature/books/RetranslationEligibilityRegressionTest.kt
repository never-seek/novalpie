package com.novalpie.nativeapp.feature.books

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.Chapter
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RetranslationEligibilityRegressionTest {
    @Test fun missingApprovalMetadataCannotOverrideServerAcceptedRetranslation()=runBlocking {
        MockWebServer().use{s->s.start();s.dispatcher=object:Dispatcher(){override fun dispatch(r:RecordedRequest)=when(r.requestUrl!!.encodedPath){
            "/api/workspace/apis"->MockResponse().setBody("""{"data":[{"id":1,"is_active":1,"healthy":1}]}""")
            "/api/v2/novels/2/chapters/retranslate"->MockResponse().setBody("""{"success":true,"message":"重新翻译请求已提交"}""")
            else->MockResponse().setResponseCode(404)
        }}
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
        val controller=ChapterRetranslationController(WebsiteChapterRetranslationRepository(NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))),scope){0}
        try {
            controller.open(2,"QA",listOf(Chapter(901,"EP1",1)));controller.submit()
            withTimeout(8000){while(controller.state.submitting)delay(10)}
            assertTrue("The authoritative endpoint accepted this account; absent approval metadata is not rejection",controller.state.confirmed)
        }finally{scope.cancel()}
        }
    }
}
