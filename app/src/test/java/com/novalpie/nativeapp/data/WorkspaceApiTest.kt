package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun workspaceDashboardNormalizesCurrentWebsiteResponses() = runBlocking {
        server.enqueue(jsonResponse("""
            {"success":true,"data":[{
              "id":9,"name":"Shared DeepSeek","model":"deepseek-chat",
              "endpoint":"https://api.deepseek.com","key":"sk-secret-value",
              "concurrency":10,"is_active":1,"is_healthy":1,
              "approval_status":"approved","totalRequests":42,
              "status":"active","actualStatus":"enabled"
            },{
              "id":10,"name":"Disabled provider","model":"gpt-4o-mini",
              "endpoint":"https://api.example.com","status":"inactive",
              "actual_status":"offline","callCount":3
            }]}
        """))
        server.enqueue(jsonResponse("""{"success":true,"data":{"hasCookie":true}}"""))
        server.enqueue(jsonResponse("""
            {"success":true,"data":{
              "myConfigs":[{
                "id":7,"config_key":"primary","description":"Main cookie",
                "proxy_ip":"10.0.2.2:7890","is_active":1,"is_healthy":1,
                "last_check_at":"2026-07-10T04:00:00Z","updated_by_username":"Admin"
              }],
              "otherConfigs":[{
                "id":8,"config_key":"shared","is_active":1,"is_healthy":0,
                "updated_by_username":"Other"
              }]
            }}
        """))
        server.enqueue(jsonResponse("""
            {"success":true,"data":{
              "apiStatus":{"total":3,"active":2,"healthy":1,"total_requests":99}
            }}
        """))
        server.enqueue(jsonResponse("""
            {"success":true,"data":{
              "translators":[{
                "id":9,"name":"Shared DeepSeek","model":"deepseek-chat",
                "endpoint":"https://api.deepseek.com","isHealthy":true,
                "isActive":true,"approval_status":"approved","responseTime":321,
                "successRate":98.5
              }]
            }}
        """))

        val configs = api.workspaceApiConfigs()
        val cookieStatus = api.workspaceCookieStatus()
        val cookies = api.workspaceCookieConfigs()
        val health = api.workspaceHealth()

        assertEquals(2, configs.size)
        val activeConfig = configs.first()
        assertEquals(9L, activeConfig.id)
        assertEquals("Shared DeepSeek", activeConfig.name)
        assertEquals("deepseek-chat", activeConfig.model)
        assertEquals("sk-secret-value", activeConfig.apiKey)
        assertEquals(10, activeConfig.concurrency)
        assertTrue(activeConfig.isActive)
        assertTrue(activeConfig.isHealthy == true)
        assertEquals("approved", activeConfig.approvalStatus)
        assertEquals(42L, activeConfig.totalRequests)
        assertEquals("active", activeConfig.activationStatus)
        assertEquals("enabled", activeConfig.actualStatus)

        val inactiveConfig = configs.last()
        assertFalse(inactiveConfig.isActive)
        assertEquals("inactive", inactiveConfig.activationStatus)
        assertEquals("offline", inactiveConfig.actualStatus)
        assertEquals(3L, inactiveConfig.totalRequests)

        assertTrue(cookieStatus.hasCookie)
        assertEquals(1, cookies.myConfigs.size)
        assertEquals("primary", cookies.myConfigs.single().configKey)
        assertEquals("10.0.2.2:7890", cookies.myConfigs.single().proxyIp)
        assertTrue(cookies.myConfigs.single().isHealthy == true)
        assertEquals(1, cookies.sharedConfigs.size)
        assertFalse(cookies.sharedConfigs.single().isHealthy == true)

        assertEquals(3, health.apiStatus.total)
        assertEquals(2, health.apiStatus.active)
        assertEquals(1, health.apiStatus.healthy)
        assertEquals(99L, health.apiStatus.totalRequests)
        assertEquals(1, health.translators.size)
        assertEquals(321L, health.translators.single().responseTimeMs)
        assertEquals(98.5, health.translators.single().successRate, 0.001)

        assertEquals("/workspace/apis", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/workspace/cookie-status", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/workspace/cookie-config", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/workspace/stats", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/workspace/translator-health", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun workspaceApiMutationsUseCurrentWebsiteMethodsAndBodies() = runBlocking {
        repeat(4) { server.enqueue(jsonResponse("""{"success":true,"message":"ok","data":{"id":9}}""")) }

        assertTrue(api.createWorkspaceApi("DeepSeek", "deepseek-chat", "https://api.deepseek.com", "sk-test", 8).success)
        assertTrue(api.updateWorkspaceApi(9, "DeepSeek V2", "deepseek-chat", "https://api.deepseek.com/v1", "sk-next", 12).success)
        assertTrue(api.deleteWorkspaceApi(9).success)
        assertTrue(api.toggleWorkspaceApi(9).success)

        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/workspace/apis", create.requestUrl?.encodedPath)
        JSONObject(create.body.readUtf8()).also { body ->
            assertEquals("DeepSeek", body.getString("name"))
            assertEquals("deepseek-chat", body.getString("model"))
            assertEquals("https://api.deepseek.com", body.getString("endpoint"))
            assertEquals("sk-test", body.getString("key"))
            assertEquals(8, body.getInt("concurrency"))
        }

        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/workspace/apis/9", update.requestUrl?.encodedPath)
        assertEquals("DeepSeek V2", JSONObject(update.body.readUtf8()).getString("name"))

        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/workspace/apis/9", delete.requestUrl?.encodedPath)
        assertEquals(0L, delete.bodySize)

        val toggle = server.takeRequest()
        assertEquals("POST", toggle.method)
        assertEquals("/workspace/apis/9/toggle", toggle.requestUrl?.encodedPath)
        assertEquals(0L, toggle.bodySize)
        Unit
    }

    @Test
    fun workspaceCookieMutationsUseCurrentWebsiteMethodsAndBodies() = runBlocking {
        repeat(4) { server.enqueue(jsonResponse("""{"success":true,"message":"ok"}""")) }

        assertTrue(api.createWorkspaceCookie("primary", "Main", "cookie=value", "10.0.2.2:7890", true).success)
        assertTrue(api.updateWorkspaceCookie(7, description = "Updated", cookieRaw = null, proxyIp = "", isActive = false).success)
        assertTrue(api.setWorkspaceCookieActive(7, true).success)
        assertTrue(api.deleteWorkspaceCookie(7).success)

        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/workspace/cookie-config", create.requestUrl?.encodedPath)
        JSONObject(create.body.readUtf8()).also { body ->
            assertEquals("primary", body.getString("config_key"))
            assertEquals("cookie=value", body.getString("cookie_raw"))
            assertEquals("10.0.2.2:7890", body.getString("proxy_ip"))
            assertTrue(body.getBoolean("is_active"))
        }

        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        JSONObject(update.body.readUtf8()).also { body ->
            assertEquals(7L, body.getLong("id"))
            assertEquals("Updated", body.getString("description"))
            assertEquals("", body.getString("proxy_ip"))
            assertFalse(body.has("cookie_raw"))
            assertFalse(body.getBoolean("is_active"))
        }

        val toggle = server.takeRequest()
        assertEquals("PUT", toggle.method)
        JSONObject(toggle.body.readUtf8()).also { body ->
            assertEquals(7L, body.getLong("id"))
            assertTrue(body.getBoolean("is_active"))
        }

        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals(7L, JSONObject(delete.body.readUtf8()).getLong("id"))
        Unit
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(body.trimIndent())
}
