package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.ManagedBookAccessPolicy
import com.novalpie.nativeapp.model.UploadChapter
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookManagementApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun chapterManagementMutationsUseCurrentWebsiteContracts() = runBlocking {
        repeat(6) {
            server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true}"""))
        }

        assertTrue(api.reorderManagedChapters(354491, listOf(3L, 1L, 2L)).success)
        assertTrue(api.insertManagedChapter(354491, 2, "Inserted", "Body").success)
        assertTrue(api.updateManagedChapter(9001, "Updated", "Updated body").success)
        assertTrue(api.deleteManagedChapter(9002).success)
        assertTrue(api.batchDeleteManagedChapters(354491, listOf(9003, 9004)).success)
        assertTrue(api.requestManagedChapterTranslation(354491, listOf(9001, 9002), "shared").success)

        val reorder = server.takeRequest()
        assertEquals("/api/users/me/chapters/reorder", reorder.requestUrl?.encodedPath)
        assertEquals(listOf(3L, 1L, 2L), JSONObject(reorder.body.readUtf8()).getJSONArray("ordered_chapter_ids").let { a ->
            (0 until a.length()).map(a::getLong)
        })

        val insert = server.takeRequest()
        assertEquals("/api/users/me/chapters/insert", insert.requestUrl?.encodedPath)
        assertEquals(2, JSONObject(insert.body.readUtf8()).getInt("insert_at"))

        val update = server.takeRequest()
        assertEquals("PATCH", update.method)
        assertEquals("/api/users/me/chapters/9001", update.requestUrl?.encodedPath)

        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/users/me/chapters/9002", delete.requestUrl?.encodedPath)

        val batch = server.takeRequest()
        assertEquals("/api/users/me/chapters/batch-delete", batch.requestUrl?.encodedPath)
        assertEquals(354491L, JSONObject(batch.body.readUtf8()).getLong("novel_id"))

        val translation = server.takeRequest()
        assertEquals("/api/users/me/novels/354491/translation-requests", translation.requestUrl?.encodedPath)
        assertEquals("shared", JSONObject(translation.body.readUtf8()).getString("mode"))
    }

    @Test
    fun appendManagedChaptersUsesWebsiteMultipartEndpoint() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json")
                .setBody("""{"success":true,"novel_id":354491,"message":"appended"}""")
        )

        val result = api.appendManagedChapters(
            bookId = 354491,
            submitType = "chinese",
            chapters = listOf(
                UploadChapter(title = "Chapter 11", content = "Text", chapterNumber = 11),
                UploadChapter(title = "Chapter 12", content = "More", chapterNumber = 12)
            )
        )

        assertTrue(result.success)
        assertEquals(354491L, result.novelId)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/users/me/chapters/append", request.requestUrl?.encodedPath)
        assertTrue(request.getHeader("content-type").orEmpty().startsWith("multipart/form-data"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("name=\"existing_novel_id\""))
        assertTrue(body.contains("354491"))
        assertTrue(body.contains("name=\"submit_type\""))
        assertTrue(body.contains("chinese"))
        assertTrue(body.contains("name=\"chapters\""))
        assertTrue(body.contains("Chapter 11"))
        assertTrue(body.contains("name=\"chapters_md5\""))
    }

    @Test
    fun chapterIllustrationsUseWebsiteContracts() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "success": true,
                  "images": [
                    {"id": 11, "index": 1, "src": "imagebox/chapter/a.png"},
                    {"id": 12, "index": 2, "src": "/imagebox/chapter/b.png"}
                  ],
                  "total": 2
                }
                """
            )
        )
        server.enqueue(jsonResponse("""{"success":true,"image_count":1}"""))

        val page = api.managedChapterIllustrations(9001)
        val deleted = api.deleteManagedChapterIllustration(9001, 11)

        assertEquals(2, page.total)
        assertEquals(2, page.images.size)
        assertEquals(11L, page.images.first().id)
        assertEquals(1, page.images.first().index)
        assertEquals("${server.url("/").toString().trimEnd('/')}/imagebox/chapter/a.png", page.images.first().src)
        assertTrue(deleted.success)
        assertEquals(1, deleted.imageCount)

        val list = server.takeRequest()
        assertEquals("GET", list.method)
        assertEquals("/api/users/me/chapters/9001/illustrations", list.requestUrl?.encodedPath)

        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/users/me/chapters/9001/illustrations/11", delete.requestUrl?.encodedPath)
    }

    @Test
    fun uploadManagedChapterIllustrationsUsesWebsiteMultipartContract() = runBlocking {
        server.enqueue(jsonResponse("""{"success":true,"image_count":2}"""))

        val result = api.uploadManagedChapterIllustrations(
            chapterId = 9001,
            files = listOf(
                uploadSource("a.png", "image/png", "png-bytes".toByteArray()),
                uploadSource("b.webp", "image/webp", "webp-bytes".toByteArray())
            )
        )

        assertTrue(result.success)
        assertEquals(2, result.imageCount)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/users/me/chapters/9001/illustrations", request.requestUrl?.encodedPath)
        assertTrue(request.getHeader("content-type").orEmpty().startsWith("multipart/form-data"))
        val body = request.body.readUtf8()
        listOf(
            "name=\"chapter_id\"",
            "9001",
            "name=\"illustrations[]\"; filename=\"a.png\"",
            "Content-Type: image/png",
            "png-bytes",
            "name=\"illustrations[]\"; filename=\"b.webp\"",
            "Content-Type: image/webp",
            "webp-bytes"
        ).forEach { expected -> assertTrue("Missing multipart value: $expected", body.contains(expected)) }
    }

    @Test
    fun managedBookTransferAndThresholdUseWebsiteContracts() = runBlocking {
        server.enqueue(jsonResponse("""{"success":true,"target_username":"new-owner","target_user_id":100002}"""))
        server.enqueue(jsonResponse("""{"success":true,"message":"saved"}"""))

        val transfer = api.transferManagedBook(354491, " uid:100002 ")
        val threshold = api.updateManagedBookAccessPolicy(
            354491,
            ManagedBookAccessPolicy(
                allowDownload = true,
                downloadThresholdType = "points_min",
                downloadThresholdValue = 20,
                readThresholdType = "points_pay",
                readThresholdValue = 5
            )
        )

        assertTrue(transfer.success)
        assertEquals("new-owner", transfer.targetUsername)
        assertEquals(100002L, transfer.targetUserId)
        assertTrue(threshold.success)

        val transferRequest = server.takeRequest()
        assertEquals("POST", transferRequest.method)
        assertEquals("/api/users/me/novels/354491/transfers", transferRequest.requestUrl?.encodedPath)
        assertEquals("uid:100002", JSONObject(transferRequest.body.readUtf8()).getString("identifier"))

        val thresholdRequest = server.takeRequest()
        assertEquals("PATCH", thresholdRequest.method)
        assertEquals("/api/users/me/novels/354491/permissions", thresholdRequest.requestUrl?.encodedPath)
        JSONObject(thresholdRequest.body.readUtf8()).also { body ->
            assertEquals(1, body.getInt("allow_download"))
            assertEquals("points_min", body.getString("download_threshold_type"))
            assertEquals(20, body.getInt("download_threshold_value"))
            assertEquals("points_pay", body.getString("read_threshold_type"))
            assertEquals(5, body.getInt("read_threshold_value"))
        }
        Unit
    }

    private fun uploadSource(name: String, contentType: String, bytes: ByteArray) = UploadFileSource(
        fileName = name,
        sizeBytes = bytes.size.toLong(),
        contentType = contentType,
        openStream = { ByteArrayInputStream(bytes) }
    )

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(body.trimIndent())
}
