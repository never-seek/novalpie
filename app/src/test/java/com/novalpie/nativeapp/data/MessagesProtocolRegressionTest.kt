package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.MessageQuery
import com.novalpie.nativeapp.feature.messages.messageWriteFailure
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
class MessagesProtocolRegressionTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi
    @Before fun setUp() {
        server = MockWebServer(); server.start()
        api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
    }
    @After fun tearDown() { server.shutdown() }
    private fun response(body: String) { server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody(body)) }

    @Test fun explicitOuterRejectionCannotBeHiddenByAnInnerDataObject() = runBlocking {
        response("""{"success":false,"message":"合成发送拒绝","data":{}}""")
        val result = api.sendDirectMessage(1, 2, "synthetic", "synthetic")
        assertFalse(result.success)
        assertEquals("合成发送拒绝", result.message)
        assertEquals(1, server.requestCount)
    }

    @Test fun rejectedReadsCannotMasqueradeAsEmptyMessagesOrDefaultSettings() = runBlocking {
        val reads: List<suspend () -> Any> = listOf(
            { api.messagePage(MessageQuery()) }, { api.messageStats() },
            { api.messageConversation(2) }, { api.messageSettings() },
        )
        for (read in reads) {
            response("""{"success":false,"message":"合成权限不足","data":{}}""")
            val failure = runCatching { read() }.exceptionOrNull()
            assertNotNull("明确拒绝不能归一化成空结果", failure)
            assertTrue(failure?.message.orEmpty().contains("合成权限不足"))
        }
    }

    @Test fun detailIdentityMustMatchTheRequestedMessage() = runBlocking {
        response("""{"success":true,"message":{"id":22,"message_type":1,"message_title":"合成不同消息"}}""")
        assertTrue(runCatching { api.messageDetail(11) }.isFailure)
    }

    @Test fun knownHttpRejectionRemainsAnActionableErrorNotAnUnknownDelivery() {
        val text = messageWriteFailure("发送私信", NovalPieApiException(403, "/api/messages", "没有发送权限"))
        assertTrue(text.contains("没有发送权限"))
        assertFalse(text.contains("结果未确认"))
    }
}
