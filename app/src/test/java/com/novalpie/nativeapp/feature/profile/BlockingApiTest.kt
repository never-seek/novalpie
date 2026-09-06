package com.novalpie.nativeapp.feature.profile

import com.novalpie.nativeapp.data.NovalPieApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlockingApiTest {
    @Test fun blockedUsersUseTheVerifiedV2RouteAndPreservePagination()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"blocked_users":[{"id":42,"username":"测试用户","avatar":"/avatar.png"}],"pagination":{"page":2,"limit":20,"total":21,"pages":2}}"""))
            val api=NovalPieApi(baseUrl=server.url("/").toString().trimEnd('/'))
            val page=api.blockedUsers(2)
            assertEquals(42L,page.users.single().id)
            assertEquals(2,page.page)
            assertEquals(21,page.total)
            val request=server.takeRequest()
            assertEquals("/api/v2/users/me/blocks",request.requestUrl?.encodedPath)
            assertEquals("2",request.requestUrl?.queryParameter("page"))
            server.enqueue(MockResponse().setBody("""{"success":true}"""))
            api.setUserBlocked(42,false)
            assertEquals("DELETE",server.takeRequest().method)
        } finally {server.shutdown()}
    }
}
