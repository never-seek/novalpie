package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdminApiTest {
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
    fun overviewAndReviewEndpointsMatchWebsiteContracts() = runBlocking {
        server.enqueue(json("""{"success":true,"stats":{"pending_review_total":5,"pending_review_upload":3,"pending_review_delete":2,"pending_keys":7,"approved_keys":12,"active_translators":4,"today_users":15,"novel_active_total":400,"user_registered_total":1200,"recent_user_daily":[{"date":"2026-07-10","count":9}]}}"""))
        server.enqueue(json("""{"success":true,"settings":{"auto_approve_upload":true,"auto_approve_delete":false}}"""))
        server.enqueue(json("""{"success":true,"list":[{"id":17,"type":"upload","status":"pending","user":{"id":9,"username":"Alice"},"novel":{"id":354491,"title":"New book"},"created_at":"2026-07-10T00:00:00Z"}]}"""))

        val overview = api.adminOverview(days = 7)
        val settings = api.adminReviewSettings()
        val requests = api.adminReviewRequests(type = "upload", status = "pending", keyword = "book")

        assertEquals(5, overview.pendingReviewTotal)
        assertEquals(7, overview.pendingKeys)
        assertEquals(12, overview.approvedKeys)
        assertEquals(4, overview.activeTranslators)
        assertEquals(15, overview.todayUsers)
        assertEquals(9, overview.recentUserDaily.single().count)
        assertTrue(settings.autoApproveUpload)
        assertFalse(settings.autoApproveDelete)
        assertEquals(17L, requests.single().id)
        assertEquals("Alice", requests.single().username)
        assertEquals("/api/admin/overview?days=7", server.takeRequest().path)
        assertEquals("/api/admin/review-settings", server.takeRequest().path)
        assertEquals(
            "/api/admin/review-requests?page=1&page_size=100&type=upload&status=pending&q=book",
            server.takeRequest().path
        )
    }

    @Test
    fun keyAndOperationLogEndpointsNormalizeWebsiteData() = runBlocking {
        server.enqueue(json("""{"data":[{"id":4,"name":"GPT","model":"gpt-test","provider_name":"Provider","approval_status":"pending","base_url":"https://api.example.test"}]}"""))
        server.enqueue(json("""{"success":true,"logs":[{"id":8,"action":"upload_novel","status":"success","user_id":42,"username":"Alice","email":"alice@example.test","novel_id":354491,"novel_title":"New book","chapter_id":3,"ip_address":"127.0.0.1","message":"done","content":"payload","result":"ok","user_agent":"agent","created_at":"2026-07-10T01:00:00Z","updated_at":"2026-07-10T01:01:00Z"}],"total":1,"total_pages":1,"action_types":["upload_novel"]}"""))

        val keys = api.adminKeys()
        val logs = api.adminOperationLogs(page = 1, keyword = "done")

        assertEquals("pending", keys.single().approvalStatus)
        assertEquals("Provider", keys.single().providerName)
        assertEquals("upload_novel", logs.items.single().action)
        assertEquals("Alice", logs.items.single().username)
        assertEquals("New book", logs.items.single().novelTitle)
        assertEquals(3L, logs.items.single().chapterId)
        assertEquals("payload", logs.items.single().content)
        assertEquals(1, logs.totalPages)
        assertEquals("/api/admin/key-management", server.takeRequest().path)
        assertEquals("/api/admin/operation-logs?page=1&page_size=20&keyword=done", server.takeRequest().path)
    }

    @Test
    fun operationLogEndpointSendsAllWebsiteFilterParameters() = runBlocking {
        server.enqueue(json("""{"success":true,"logs":[],"total":0,"total_pages":1,"action_types":["upload_novel"]}"""))

        api.adminOperationLogs(
            page = 3,
            action = "upload_novel",
            status = "success",
            userId = "42",
            novelId = "354491",
            keyword = "done",
            startDate = "2026-07-01",
            endDate = "2026-07-11"
        )

        assertEquals(
            "/api/admin/operation-logs?page=3&page_size=20&action=upload_novel&status=success&user_id=42&novel_id=354491&keyword=done&start_date=2026-07-01&end_date=2026-07-11",
            server.takeRequest().path
        )
    }

    @Test
    fun scraperManagementEndpointsMatchWebsiteContracts() = runBlocking {
        server.enqueue(json("""{"success":true,"configs":[{"id":2,"config_key":"source-a","description":"Main","proxy_ip":"10.0.2.2:7890","is_active":true,"is_healthy":true,"updated_at":"2026-07-10","updated_by_username":"Admin","success_count":4,"fail_count":1}]}"""))
        server.enqueue(json("""{"data":[{"id":3,"pattern":"*","action":"manual","description":"Default"},{"id":4,"pattern":"https://api.example.test","action":"allow","description":"Allowed","created_at":"2026-07-10"}]}"""))
        server.enqueue(json("""{"success":true,"logs":["INFO ready","ERROR sample"],"total_lines":2,"file_size_mb":0.1,"last_modified":"2026-07-10"}"""))

        val configs = api.adminCookieConfigs()
        val rules = api.adminBaseUrlRules()
        val logs = api.adminSchedulerLogs(lines = 100)

        assertEquals("source-a", configs.single().configKey)
        assertTrue(configs.single().isHealthy == true)
        assertEquals(4, configs.single().successCount)
        assertEquals("manual", rules.first().action)
        assertEquals("2026-07-10", rules.last().createdAt)
        assertEquals(2, logs.logs.size)
        assertEquals("/api/admin/cookie-config", server.takeRequest().path)
        assertEquals("/api/admin/baseurl-rules", server.takeRequest().path)
        assertEquals("/api/admin/scheduler-logs?lines=100", server.takeRequest().path)
    }

    @Test
    fun shopEndpointNormalizesFrameAndBadgeItems() = runBlocking {
        server.enqueue(json("""{"success":true,"items":[{"id":6,"name":"Blue frame","description":"Frame","price":90,"type":"frame","image_url":"/shop/frame.png","is_active":1},{"id":7,"name":"Founder","description":"Badge","price":120,"type":"badge","badge_html":"<span>F</span>","badge_css":"color:red","is_active":0}]}"""))

        val items = api.adminShopItems(type = "frame", active = true, keyword = "Blue")

        assertEquals(2, items.size)
        assertTrue(items.first().isActive)
        assertTrue(items.first().imageUrl.orEmpty().endsWith("/shop/frame.png"))
        assertFalse(items.last().isActive)
        assertEquals(
            "/api/admin/shop/items?type=frame&is_active=1&keyword=Blue&page=1&page_size=100",
            server.takeRequest().path
        )
    }

    @Test
    fun reviewAndKeyMutationsUseWebsiteMethodsAndBodies() = runBlocking {
        repeat(4) { server.enqueue(json("{\"success\":true}")) }

        api.adminUpdateReviewSettings(autoApproveUpload = true, autoApproveDelete = false)
        api.adminReviewAction(id = 17, action = "approve")
        api.adminUpdateKeyStatus(id = 4, approvalStatus = "approved")
        api.adminDeleteKey(id = 4)

        val settings = server.takeRequest()
        assertEquals("POST", settings.method)
        assertEquals("/api/admin/review-settings", settings.path)
        assertTrue(settings.body.readUtf8().contains("\"auto_approve_upload\":true"))
        val review = server.takeRequest()
        assertEquals("POST", review.method)
        assertEquals("/api/admin/review-requests", review.path)
        assertTrue(review.body.readUtf8().contains("\"action\":\"approve\""))
        val key = server.takeRequest()
        assertEquals("PUT", key.method)
        assertEquals("/api/admin/key-management", key.path)
        assertTrue(key.body.readUtf8().contains("\"approval_status\":\"approved\""))
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/admin/key-management?id=4", delete.path)
    }

    @Test
    fun batchReviewApprovalUsesTheWebsiteScopeParameters() = runBlocking {
        server.enqueue(json("""{"success":true,"approved_count":3,"rejected_missing_count":1}"""))

        api.adminApproveAllReviews(type = "upload", status = "pending", keyword = "book")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/admin/review-requests", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"action\":\"approve_all\""))
        assertTrue(body.contains("\"type\":\"upload\""))
        assertTrue(body.contains("\"status\":\"pending\""))
        assertTrue(body.contains("\"q\":\"book\""))
    }

    @Test
    fun scraperAndShopMutationsUseWebsiteContracts() = runBlocking {
        repeat(6) { server.enqueue(json("{\"success\":true}")) }
        val cookie = com.novalpie.nativeapp.model.AdminCookieConfig(
            id = 2,
            configKey = "source-a",
            description = "Main",
            proxyIp = "10.0.2.2:7890",
            isActive = false
        )
        val rule = com.novalpie.nativeapp.model.AdminBaseUrlRule(4, "https://api.example.test", "allow", "Allowed")
        val item = com.novalpie.nativeapp.model.AdminShopItem(6, "Blue frame", "Frame", 90, "frame", "/shop/frame.png", isActive = true)

        api.adminSaveCookieConfig(cookie, cookieRaw = null)
        api.adminDeleteCookieConfig(2)
        api.adminSaveBaseUrlRule(rule)
        api.adminDeleteBaseUrlRule(4)
        api.adminSaveShopItem(item)
        api.adminDeleteShopItem(6)

        assertEquals("PUT", server.takeRequest().method)
        val cookieDelete = server.takeRequest()
        assertEquals("DELETE", cookieDelete.method)
        assertTrue(cookieDelete.body.readUtf8().contains("\"id\":2"))
        assertEquals("PUT", server.takeRequest().method)
        assertEquals("/api/admin/baseurl-rules?id=4", server.takeRequest().path)
        assertEquals("PUT", server.takeRequest().method)
        assertEquals("/api/admin/shop/items?id=6", server.takeRequest().path)
    }

    private fun json(body: String): MockResponse = MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(body)
}
