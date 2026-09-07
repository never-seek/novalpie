package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.*
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class TranslationModelHttpTest {
    private fun config(server: MockWebServer) = WorkspaceLocalApiConfig(1, "synthetic", "fixture-model", server.url("/v1").toString(), "test-only-model-key", 1)
    private fun response(finish: String = "stop") = MockResponse().setBody(JSONObject()
        .put("choices", org.json.JSONArray().put(JSONObject().put("finish_reason", finish).put("message", JSONObject().put("content", """{"lines":{"0":"译文"},"table_add":[],"table_remove":[]}"""))))
        .put("usage", JSONObject().put("total_tokens", 12)).toString())

    @Test fun modelUsesOnlyItsOwnKeyAndResolvesTheCurrentProxyForEveryRequest() = runBlocking {
        val server = MockWebServer(); server.start()
        var selected = 0
        val selector = object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> { selected++; return listOf(Proxy.NO_PROXY) }
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) = Unit
        }
        var providers = 0
        try {
            server.enqueue(response()); server.enqueue(response())
            val model = CompatibleTranslationModel(proxySelectorProvider = { providers++; selector })
            repeat(2) {
                val result = model.translate(config(server), "原文", mapOf("词" to "Term"))
                assertEquals("译文", result.text); assertEquals(12L, result.tokens)
                val request = server.takeRequest()
                assertEquals("/v1/chat/completions", request.path)
                assertEquals("Bearer test-only-model-key", request.getHeader("Authorization"))
                assertNull(request.getHeader("Cookie"))
                assertNull(request.getHeader("X-Auth-Token"))
                val body = JSONObject(request.body.readUtf8())
                assertEquals("fixture-model", body.getString("model"))
                assertEquals("json_object", body.getJSONObject("response_format").getString("type"))
            }
            assertEquals(2, providers)
            assertTrue(selected > 0)
        } finally { server.shutdown() }
    }
    @Test fun redirectTruncationAndHttpFailureNeverCauseAutomaticBillableReplay() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/other")))
            server.enqueue(response("length"))
            server.enqueue(MockResponse().setResponseCode(429).setBody("private failure details"))
            val model = CompatibleTranslationModel()
            repeat(3) { assertTrue(runCatching { model.translate(config(server), "原文", emptyMap()) }.isFailure) }
            assertEquals(3, server.requestCount)
        } finally { server.shutdown() }
    }
    @Test fun cancellingAnInFlightRequestClosesItWithoutWaitingForReadTimeout() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val model = CompatibleTranslationModel(OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build())
            val job = launch(Dispatchers.Default) { model.translate(config(server), "原文", emptyMap()) }
            assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(3, TimeUnit.SECONDS) })
            withTimeout(1000) { job.cancelAndJoin() }
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }
}
