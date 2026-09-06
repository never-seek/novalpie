package com.novalpie.nativeapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReplacementProtocolRegressionTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi
    @Before fun setUp() { server = MockWebServer(); server.start(); api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')) }
    @After fun tearDown() { server.shutdown() }
    private fun rejected() { server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":false,"message":"合成规则拒绝","data":{}}""")) }

    @Test fun refusedDeleteAndReadsMustNotReturnSuccessOrEmptyRules() = runBlocking {
        val operations: List<suspend () -> Any> = listOf({ api.deletePersonalGlossary(1) }, { api.personalGlossaries(1) }, { api.sharedGlossaries(1) })
        for (operation in operations) { rejected(); assertTrue(runCatching { operation() }.isFailure) }
    }

    @Test fun outerForumFailureCannotAcknowledgeACommentOrReaderProgress() = runBlocking {
        rejected()
        assertFalse(api.createForumComment(1, "synthetic").success)
        rejected()
        assertFalse(api.saveReadingProgress(1, 2).success)
    }
}
