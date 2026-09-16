package com.novalpie.nativeapp.feature.reader.preferences

import com.novalpie.nativeapp.data.NovalPieApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderPreferenceRepositoryTest {
    @Test fun matchesCurrentWebsiteListLoadCreateUpdateDeleteProtocol() = runBlocking {
        val server = MockWebServer(); server.start()
        val repository = WebsiteReaderPreferenceRepository(NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')))
        fun respond(body: String) { server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body)) }
        try {
            respond("""{"data":[{"id":7,"config_name":"测试","is_default":1}]}""")
            assertEquals(ReaderPreferenceProfile(7,"测试",true), repository.list().single()); assertEquals("/api/reader/settings", server.takeRequest().path)
            respond("""{"preferences":{"fontSize":22}}""")
            assertEquals(22, JSONObject(repository.load("测试")).getInt("fontSize")); assertEquals("测试", server.takeRequest().requestUrl!!.queryParameter("config_name"))
            respond("""{"success":true,"id":8}"""); repository.create("新配置", """{"fontSize":20}""", false)
            val created = server.takeRequest(); assertEquals("POST", created.method)
            val payload = JSONObject(created.body.readUtf8()); assertEquals("新配置", payload.getString("config_name")); assertEquals(20,payload.getInt("fontSize")); assertFalse(payload.has("preferences"))
            respond("""{"success":true}"""); repository.update(8,"""{"is_default":true}""")
            val updated = server.takeRequest(); assertEquals("PUT",updated.method); assertEquals(8,JSONObject(updated.body.readUtf8()).getInt("id"))
            respond("""{"success":true}"""); repository.delete(8)
            val deleted = server.takeRequest(); assertEquals("DELETE",deleted.method); assertEquals("8",deleted.requestUrl!!.queryParameter("id"))
        } finally { server.shutdown() }
    }
    @Test fun outerRejectionCannotBeHiddenAndNoWriteIsAutomaticallyRepeated() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type","application/json").setBody("""{"success":false,"message":"拒绝","data":{"success":true,"id":8}}"""))
            val repository = WebsiteReaderPreferenceRepository(NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')))
            assertTrue(runCatching { repository.create("测试","{}",false) }.isFailure)
            assertEquals(1,server.requestCount)
        } finally { server.shutdown() }
    }
}
