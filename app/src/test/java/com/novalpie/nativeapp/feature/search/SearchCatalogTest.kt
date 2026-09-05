package com.novalpie.nativeapp.feature.search

import com.novalpie.nativeapp.data.NovalPieApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchCatalogTest {
    @Test fun unnamedTagsStillCountTowardTheSourceOffset()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"success":true,"data":[{"id":1,"name":"奇幻"},{"id":2,"name":""}],"total":3,"hasMore":true}"""))
            server.enqueue(MockResponse().setBody("""{"success":true,"data":[{"id":3,"name":"轻小说"}],"total":3,"hasMore":false}"""))
            val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))
            assertEquals(2,api.allTags(pageSize=2).size)
            server.takeRequest()
            assertEquals("2",server.takeRequest().requestUrl?.queryParameter("offset"))
        } finally {server.shutdown()}
    }
    @Test fun tagsFollowOffsetPaginationUntilTheSourceReportsCompletion()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"success":true,"data":[{"id":1,"name":"奇幻"},{"id":2,"name":"学院"}],"total":3,"hasMore":true}"""))
            server.enqueue(MockResponse().setBody("""{"success":true,"data":[{"id":3,"name":"轻小说"}],"total":3,"hasMore":false}"""))
            val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))
            val tags=api.allTags(pageSize=2)
            assertEquals(listOf("奇幻","学院","轻小说"),tags.map{it.name})
            assertEquals("0",server.takeRequest().requestUrl?.queryParameter("offset"))
            assertEquals("2",server.takeRequest().requestUrl?.queryParameter("offset"))
        } finally {server.shutdown()}
    }

    @Test fun wordRangeAcceptsUnboundedEndsAndRejectsReversedOrOverflowingInput() {
        assertEquals("300000..2000000",searchWordRangeInput("300000","2000000").value)
        assertEquals("50000..",searchWordRangeInput("50000","").value)
        assertEquals("",searchWordRangeInput("","").value)
        assertNotNull(searchWordRangeInput("100","99").error)
        assertNotNull(searchWordRangeInput("-1","100").error)
        assertNotNull(searchWordRangeInput("999999999999999999999999","").error)
        assertTrue(searchTagQueryMatches("轻小说","輕小說"))
    }
}
