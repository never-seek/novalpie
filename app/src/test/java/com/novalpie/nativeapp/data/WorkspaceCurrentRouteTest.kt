package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Captured Nuxt runtime apiBase=https://novalpie.cc/api + dSlFh-Ca workspace paths. */
@RunWith(RobolectricTestRunner::class)
class WorkspaceCurrentRouteTest {
    @Test fun liveStatusArrayMustNotBecomeZeroApis() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"success":true,"data":{"apiStatus":[{"status":"translate_pending","label":"待翻译","count":82}]}}"""))
            server.enqueue(MockResponse().setBody("""{"success":true,"data":{"translators":[{"id":1,"name":"合成","isActive":true,"isHealthy":true}]}}"""))
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            val health = api.workspaceHealth()
            assertEquals(1, health.apiStatus.total)
            assertEquals(1, health.apiStatus.healthy)
            assertEquals("translate_pending", health.translationCounts.single().status)
            assertEquals(82L, health.translationCounts.single().count)
        } finally { server.shutdown() }
    }
    @Test fun workspaceReadsAndWritesUseTheApiPrefixNotTheHtmlPageNamespace() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            repeat(4) { server.enqueue(MockResponse().setBody("""{"success":true,"data":{"id":9}}""")) }
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            api.workspaceApiConfigs(); api.createWorkspaceApi("synthetic", "model", "https://fixture.test", "test-only", 2)
            api.workspaceCookieConfigs(); api.setWorkspaceCookieActive(9, true)
            val paths = (1..4).map { server.takeRequest().requestUrl!!.encodedPath }
            assertEquals(listOf("/api/workspace/apis", "/api/workspace/apis", "/api/workspace/cookie-config", "/api/workspace/cookie-config"), paths)
        } finally { server.shutdown() }
    }
}
