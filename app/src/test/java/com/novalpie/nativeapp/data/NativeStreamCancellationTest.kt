package com.novalpie.nativeapp.data

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class NativeStreamCancellationTest {
    @Test fun externalOriginalImagesNeverReceiveSiteSessionHeaders()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("image fixture"))
            val api=NovalPieApi(baseUrl="https://novalpie.cc",authTokenProvider={"site-token-fixture"},cookieProvider={"site-cookie=fixture"})
            api.streamAsset(server.url("/image.webp").toString()){input,_->input.readBytes()}
            val request=server.takeRequest()
            assertNull(request.getHeader("authorization"))
            assertNull(request.getHeader("cookie"))
        }finally{server.shutdown()}
    }
    @Test fun cancellingAStalledStreamClosesTheSocketBeforeTheReadTimeout()=runBlocking {
        val server=MockWebServer();server.start()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))
        val job=launch(Dispatchers.IO){api.streamDownloadFile("test-cancel.txt"){input->input.read()}}
        try {
            assertNotNull(server.takeRequest(5,TimeUnit.SECONDS))
            job.cancel()
            assertTrue("取消仍等socket长超时",withTimeoutOrNull(1500){job.join();true}==true)
        } finally {server.shutdown();job.cancelAndJoin()}
    }
}
