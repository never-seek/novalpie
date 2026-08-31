package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.NovalPieApiException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiFailureMessageTest {
    @Test
    fun includesApiAreaLabelAndThrowableMessage() {
        val message = apiFailureMessage("搜索", IllegalStateException("timeout"))

        assertEquals("搜索请求失败: timeout", message)
    }

    @Test
    fun fallsBackToThrowableClassNameWhenMessageIsBlank() {
        val message = apiFailureMessage("书籍详情", RuntimeException(""))

        assertEquals("书籍详情请求失败: RuntimeException", message)
    }

    @Test
    fun hidesTechnicalApiSuffixAndEndpointPathsFromVisibleMessage() {
        val message = apiFailureMessage(
            "阅读器正文/API",
            IllegalStateException("NovalPie API 400: /api/chapters/8001/content")
        )

        assertEquals("阅读器正文请求失败: 服务返回错误 400", message)
    }

    /**
     * The point of the change: the server's own explanation reaches the user. Previously the error
     * body was read and discarded, so this rendered as `保存章节请求失败: 服务返回错误 422`.
     */
    @Test
    fun surfacesTheServerExplanationInsteadOfOnlyTheStatusCode() {
        val message = apiFailureMessage(
            "保存章节",
            NovalPieApiException(
                statusCode = 422,
                path = "/api/novels/1/chapters",
                serverMessage = "章节标题重复，请修改后重试",
            )
        )

        assertEquals("保存章节请求失败: 章节标题重复，请修改后重试", message)
    }

    @Test
    fun translatesAnUnavailableNewChapterEndpointIntoAReaderFacingAction() {
        assertEquals(
            "获取新章请求失败: 源站暂未开放该功能，请使用网页详情重试",
            apiFailureMessage(
                "获取新章",
                NovalPieApiException(
                    statusCode = 501,
                    path = "/api/novels/354491/chapters/request",
                    serverMessage = "API endpoint not implemented in Laravel yet.",
                ),
            ),
        )
        assertEquals(
            "获取新章请求失败: 源站暂未开放该功能，请使用网页详情重试",
            apiFailureMessage(
                "获取新章",
                NovalPieApiException(
                    statusCode = 405,
                    path = "/api/novels/354491/chapters/request",
                    serverMessage = "The POST method is not supported for route api/novels/354491/chapters/request.",
                ),
            ),
        )
    }

    @Test
    fun explainsStatusCodesInPlainLanguageWhenServerSaysNothing() {
        fun detailFor(code: Int): String =
            apiFailureMessage("书架", NovalPieApiException(code, "/api/favorites", null))
                .removePrefix("书架请求失败: ")

        assertEquals("登录已失效，请重新登录", detailFor(401))
        assertEquals("没有访问权限", detailFor(403))
        assertEquals("内容不存在或已被删除", detailFor(404))
        assertEquals("操作过于频繁，请稍后再试", detailFor(429))
        assertEquals("服务暂时不可用，请稍后重试（500）", detailFor(500))
        // Anything unmapped still reports its code rather than inventing an explanation.
        assertEquals("服务返回错误 418", detailFor(418))
    }

    /**
     * Network failures used to be printed verbatim, e.g.
     * `书架请求失败: failed to connect to /10.0.2.2 (port 7890) from /192.168.1.5 ... after 12000ms`.
     */
    @Test
    fun replacesTechnicalNetworkTextWithReadableCauses() {
        assertEquals(
            "书架请求失败: 网络连接超时，请稍后重试",
            apiFailureMessage("书架", SocketTimeoutException("timeout")),
        )
        assertEquals(
            "书架请求失败: 无法解析服务器地址，请检查网络连接",
            apiFailureMessage("书架", UnknownHostException("novalpie.cc")),
        )
        assertEquals(
            "书架请求失败: 无法连接服务器，请检查网络连接",
            apiFailureMessage(
                "书架",
                ConnectException("failed to connect to /10.0.2.2 (port 7890) after 12000ms"),
            ),
        )
    }

    /** OkHttp wraps the real reason, so the cause chain has to be inspected. */
    @Test
    fun findsTheReadableCauseThroughAWrappingException() {
        val wrapped = IOException("unexpected end of stream", SocketTimeoutException("read timed out"))

        assertEquals("阅读器正文请求失败: 网络连接超时，请稍后重试", apiFailureMessage("阅读器正文", wrapped))
    }
}
