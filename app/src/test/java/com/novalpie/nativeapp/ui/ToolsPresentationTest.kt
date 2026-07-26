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
        assertEquals("\u79c1\u4fe1", messageTypeLabel(8))
        assertEquals("\u7cfb\u7edf\u516c\u544a", messageTypeLabel(9))
        assertEquals("\u4e3e\u62a5\u901a\u77e5", messageTypeLabel(10))
        assertEquals("\u672a\u77e5\u7c7b\u578b", messageTypeLabel(99))
    }
}
