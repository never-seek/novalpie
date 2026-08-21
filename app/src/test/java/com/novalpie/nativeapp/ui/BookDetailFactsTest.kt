package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BookDetailFactsTest {
    @Test
    fun bookDetailFactsKeepSourceMetadataSeparateFromCounterRailsInCleanChinese() {
        val book = NovelCard(
            id = 354491,
            title = "Native Book",
            author = "Author Name",
            platform = "upload",
            status = "连载中",
            wordCount = 1234567,
            favoriteCount = 2345,
            siteReadCount = 120000,
            recommendCount = 19,
            sourceReadCount = 980000,
            sourceFavoriteCount = 45000,
            updatedAt = "2026-07-02T08:30:00Z",
            createdAt = "2026-06-30T08:30:00Z",
            guarantorName = "Guarantor",
            guaranteedAt = "2026-07-01T08:30:00Z",
            uploaderName = "Uploader",
            isAdult = true,
            allowDownload = true,
        )
        val facts = bookDetailFacts(book)

        assertEquals(
            listOf(
                "状态: 连载中",
                "作者: Author Name",
                "来源: 上传",
                "字数: 1,234,567",
                "更新: 2026-07-02T08:30:00Z",
                "上架: 2026-06-30T08:30:00Z",
                "担保人: Guarantor (2026-07-01T08:30:00Z)",
                "上传者: Uploader",
                "成人内容",
                "允许下载",
            ),
            facts
        )
        facts.forEach(::assertNoMojibake)
        assertEquals(
            listOf(
                BookDetailStatistic("本站", "推荐 19 · 阅读 12w · 收藏 2.3k"),
                BookDetailStatistic("源站", "阅读 98w · 收藏 4.5w"),
            ),
            bookDetailStatistics(book),
        )
    }

    @Test
    fun bookDetailFactsSkipBlankOrMissingValues() {
        val facts = bookDetailFacts(
            NovelCard(
                id = 354491,
                title = "Native Book",
                author = " ",
                wordCount = null,
                updatedAt = null
            )
        )

        assertEquals(emptyList<String>(), facts)
    }

    private fun assertNoMojibake(value: String) {
        listOf("鐘", "浣", "瀛", "鏇", "€").forEach { fragment ->
            assertFalse("Book detail visible fact contains mojibake: $value", value.contains(fragment))
        }
    }
}
