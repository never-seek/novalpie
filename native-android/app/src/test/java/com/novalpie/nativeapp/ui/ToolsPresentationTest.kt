package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsPresentationTest {
    @Test
    fun regularUsersSeeCurrentWebsiteUtilityRoutesWithoutAdminRoutes() {
        val entries = toolsEntries(isAdmin = false)

        assertEquals(
            listOf("/messages", "/workspace", "/upload", "/upload-editor", "/political-exam"),
            entries.map { it.path }
        )
        assertFalse(entries.any { it.adminOnly })
    }

    @Test
    fun adminsSeeCurrentWebsiteAdminRoutes() {
        val entries = toolsEntries(isAdmin = true)

        assertTrue(entries.any { it.path == "/admin" && it.adminOnly })
        assertTrue(entries.any { it.path == "/admin/review" && it.adminOnly })
        assertTrue(entries.any { it.path == "/admin/key-management" && it.adminOnly })
        assertTrue(entries.any { it.path == "/admin/operation-logs" && it.adminOnly })
        assertTrue(entries.any { it.path == "/admin/scraper-management" && it.adminOnly })
        assertTrue(entries.any { it.path == "/admin/shop" && it.adminOnly })
    }

    @Test
    fun messageTypeLabelsMatchCurrentWebsiteTypes() {
        assertEquals("私信", messageTypeLabel(8))
        assertEquals("系统公告", messageTypeLabel(9))
        assertEquals("举报通知", messageTypeLabel(10))
        assertEquals("未知类型", messageTypeLabel(99))
    }
}
