package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorRecoveryTest {
    @Test
    fun retryActionLabelNamesTheFailedSurface() {
        assertEquals("重试搜索", retryActionLabel("搜索"))
        assertEquals("重试书籍详情", retryActionLabel(" 书籍详情 "))
    }

    @Test
    fun retryActionLabelFallsBackWhenSurfaceIsBlank() {
        assertEquals("重试", retryActionLabel(""))
        assertEquals("重试", retryActionLabel("   "))
    }
}
