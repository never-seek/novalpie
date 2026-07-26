package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
