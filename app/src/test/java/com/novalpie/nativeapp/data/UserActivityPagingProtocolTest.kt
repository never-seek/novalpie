package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserActivityPagingProtocolTest {
    @Test fun explicitDifferentAuthorsCannotLeakThroughPostOrBookReviewFeeds() = runBlocking {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.requestUrl!!.encodedPath) {
                    "/api/posts" -> """{"posts":[{"id":1,"user_id":1,"title":"本人"},{"id":2,"user_id":9,"title":"其他人"}]}"""
                    "/api/comments/book-reviews" -> """{"posts":[{"id":3,"author_id":1,"bookId":10,"content":"本人书评"},{"id":4,"author_id":9,"bookId":10,"content":"其他人书评"}]}"""
                    else -> """{"activities":[],"comments":[]}"""
                }
                return MockResponse().setHeader("content-type", "application/json").setBody(body)
            }
        }
        server.start()
        try {
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            val result = api.userContentActivityFeed(1, limit = 20)
            assertEquals(setOf(1L, 3L), result.activities.map { it.id }.toSet())
        } finally { server.shutdown() }
    }
    @Test fun mergingOnePageOfEachSourceCannotSilentlyDiscardTheRestOfThatPage() = runBlocking {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath
                val body = when (path) {
                    "/api/posts" -> JSONObject().put("posts", JSONArray().apply { for (id in 1..2) put(JSONObject().put("id", id).put("title", "帖子$id")) }).put("pagination", JSONObject().put("total", 6))
                    "/api/posts/comments" -> JSONObject().put("comments", JSONArray().apply { for (id in 3..4) put(JSONObject().put("id", id).put("post_id", 1).put("content", "评论$id").put("author_id", 1)) }).put("pagination", JSONObject().put("total", 6))
                    "/api/comments/book-reviews" -> JSONObject().put("posts", JSONArray().apply { for (id in 5..6) put(JSONObject().put("id", id).put("bookId", 1).put("bookTitle", "作品").put("content", "书评$id")) }).put("pagination", JSONObject().put("total", 6))
                    else -> JSONObject().put("activities", JSONArray())
                }
                return MockResponse().setHeader("content-type", "application/json").setBody(body.toString())
            }
        }
        server.start()
        try {
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            val result = api.userContentActivityFeed(1, page = 1, limit = 2)
            assertEquals(6, result.activities.size)
            assertTrue(result.hasMore)
            val requests = (1..4).map { server.takeRequest() }
            assertTrue(requests.all { it.requestUrl?.queryParameter("page") == "1" })
        } finally { server.shutdown() }
    }
}
