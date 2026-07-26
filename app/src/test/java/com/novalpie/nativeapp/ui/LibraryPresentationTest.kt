package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryPresentationTest {
    @Test
    fun libraryOverviewReadsLikeAReaderLibraryClient() {
        assertEquals(
            LibraryOverview(
                title = "书架",
                subtitle = "继续阅读、收藏分组和最近进度",
                syncLabel = "已同步",
                stats = listOf("收藏 12", "分组 3", "最近 2")
            ),
            libraryOverview(
                hasAuthToken = true,
                favoriteCount = 12,
                groupCount = 3,
                recentCount = 2
            )
        )
    }

    @Test
    fun libraryOverviewShowsUnsignedStateWithoutDebugLanguage() {
        val overview = libraryOverview(
            hasAuthToken = false,
            favoriteCount = 0,
            groupCount = 0,
            recentCount = 0
        )

        assertEquals("未同步", overview.syncLabel)
        listOf(overview.title, overview.subtitle, overview.syncLabel).plus(overview.stats).forEach { value ->
            assertFalse(value.contains("API", ignoreCase = true))
            assertFalse(value.contains("fallback", ignoreCase = true))
        }
    }

    @Test
    fun libraryShelfSectionTitlesStayCompact() {
        assertEquals("继续阅读", libraryContinueTitle(hasProgress = true))
        assertEquals("阅读记录", libraryContinueTitle(hasProgress = false))
        assertEquals(listOf("继续阅读", "清除"), libraryContinueActions())
        assertEquals("收藏书籍", libraryFavoritesTitle())
    }
}
