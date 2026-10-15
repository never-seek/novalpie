package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ReaderCatalogDeadlineTest {
    @Test fun oneDeadlineIncludesSafeReadAuthenticationFallback()=runBlocking {
        MockWebServer().use{s->s.start()
            s.enqueue(MockResponse().setResponseCode(401).setHeadersDelay(700,TimeUnit.MILLISECONDS).setBody("{}"))
            s.enqueue(MockResponse().setHeadersDelay(700,TimeUnit.MILLISECONDS).setBody("""{"chapters":[]}"""))
            val api=NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'),authTokenProvider={"synthetic"},cookieProvider={"fixture=synthetic"})
            val result=runCatching{api.chapters(2,deadlineSeconds=1)}
            assertTrue("Two auth attempts must not each receive a fresh deadline",result.isFailure)
        }
    }
    @Test fun deadlineReachesTheBlockingHttpCallInsteadOfOnlyTheCoroutine()=runBlocking {
        MockWebServer().use{s->s.start()
            s.enqueue(MockResponse().setHeadersDelay(3,TimeUnit.SECONDS).setBody("""{"chapters":[]}"""))
            val api=NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))
            val start=System.nanoTime();val result=runCatching{api.chapters(2,deadlineSeconds=1)}
            val elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)
            assertTrue(result.isFailure);assertTrue("HTTP must time out before the delayed headers ($elapsed ms)",elapsed<2500)
        }
    }
}
