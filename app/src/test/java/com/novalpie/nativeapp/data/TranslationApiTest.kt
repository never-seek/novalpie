package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.feature.workspace.TranslationResult
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TranslationApiTest {
    @Test fun sourcePreparationAndSubmitPreserveWebsiteIdentitiesAndDoNotReplay() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            server.enqueue(MockResponse().setBody("""{"success":true,"data":[{"id":10,"title":"标题","chapter_number":1,"translate_status":"pending"}]}"""))
            server.enqueue(MockResponse().setBody("""{"success":true,"novel_id":2,"chapter_id":10,"chapter_title":"标题","total_chunks":1,"chunks":[{"index":0,"content":"原文","glossary":{"A":{"target":"甲"}}}]}"""))
            server.enqueue(MockResponse().setBody("""{"success":false,"message":"fixture rejection"}"""))
            assertEquals(10L, api.translationChapterCandidates(2).single().id)
            assertEquals("甲", api.prepareWorkspaceTranslation(2, 10).chunks.single().glossary["A"])
            assertFalse(api.submitWorkspaceTranslation(2, 10, "译文", "标题", TranslationResult("译文"), "fixture", 50))
            assertEquals("/api/translations/chapters/raw", server.takeRequest().requestUrl!!.encodedPath)
            assertEquals("/api/translations/prepare", server.takeRequest().requestUrl!!.encodedPath)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/translations", request.requestUrl!!.encodedPath)
            val body = JSONObject(request.body.readUtf8())
            assertEquals(2L, body.getLong("novel_id")); assertEquals(10L, body.getLong("chapter_id"))
            assertEquals("译文", body.getString("translated_content"))
            assertEquals(3, server.requestCount)
        } finally { server.shutdown() }
    }
}
