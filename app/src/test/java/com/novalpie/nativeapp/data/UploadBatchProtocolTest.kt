package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.UploadBookRequest
import com.novalpie.nativeapp.model.UploadChapter
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Current website uses create-first, then append to that returned id for large chapter JSON. */
@RunWith(RobolectricTestRunner::class)
class UploadBatchProtocolTest {
    private fun book() = UploadBookRequest("受控大书", authorName = "合成作者", epubFilePath = "owned/synthetic.epub",
        chapters = (1..101).map { UploadChapter("Chapter $it", "text ".repeat(11000), it) })
    @Test fun largeBookCreatesOneBookThenAppendsRemainingBatchesToItsReturnedIdentity() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            repeat(3) { server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"success":true,"novel_id":4001}""")) }
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            assertEquals(4001L, api.uploadBook(book()).novelId)
            assertEquals("大正文必须按源协议分批", 3, server.requestCount)
            repeat(3) { index ->
                val request = server.takeRequest(); val body = request.body.readUtf8()
                assertEquals(if (index == 0) "/api/uploads/books" else "/api/users/me/chapters/append", request.requestUrl!!.encodedPath)
                fun field(name: String) = Regex("name=\"$name\"[^\\r\\n]*\\r\\n(?:[^\\r\\n]+\\r\\n)*\\r\\n([^\\r\\n]*)").find(body)?.groupValues?.get(1)
                assertEquals(index.toString(), field("chunk_index"))
                assertEquals("3", field("total_chunks"))
                if (index > 0) assertEquals("4001", field("existing_novel_id"))
                assertTrue(body.contains("name=\"chapters_md5\""))
                assertTrue(body.contains("owned/synthetic.epub"))
            }
        } finally { server.shutdown() }
    }
    @Test fun failedLaterBatchStopsAndReportsTheAlreadyCreatedBookWithoutCreatingItAgain() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"success":true,"novel_id":4001}"""))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"success":false,"message":"受控拒绝追加"}"""))
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            val failed = runCatching { api.uploadBook(book()) }.exceptionOrNull()
            assertNotNull("部分完成不能显示整书成功", failed)
            assertEquals(2, server.requestCount)
            assertTrue("保留已创建书ID供恢复", failed!!.message.orEmpty().contains("4001"))
        } finally { server.shutdown() }
    }
}
