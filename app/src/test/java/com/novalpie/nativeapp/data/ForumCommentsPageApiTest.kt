package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForumCommentsPageApiTest {
    @Test fun nestedRepliesDoNotCountAsAnotherRootPageAndDifferentPageIsRejected() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            server.enqueue(MockResponse().setBody("""{"comments":[{"id":10,"content":"父","replies":[{"id":20,"content":"子"}]}],"pagination":{"page":1,"pages":2,"total":101}}"""))
            val result = api.forumPostCommentPage(1, 1)
            assertTrue(result.hasMore); assertEquals(2, result.items.size)
            assertEquals("100", server.takeRequest().requestUrl!!.queryParameter("limit"))
            server.enqueue(MockResponse().setBody("""{"comments":[],"pagination":{"page":1,"pages":2,"total":101}}"""))
            assertTrue(runCatching { api.forumPostCommentPage(1, 2) }.isFailure)
        } finally { server.shutdown() }
    }
}
