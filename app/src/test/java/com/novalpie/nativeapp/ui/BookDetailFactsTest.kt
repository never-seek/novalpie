package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BookDetailFactsTest {
    @Test
    fun bookDetailFactsIncludeStatusAuthorWordCountAndUpdatedAtInCleanChinese() {
        val facts = bookDetailFacts(
            NovelCard(
                id = 354491,
                title = "Native Book",
                author = "Author Name",
                platform = "upload",
                status = "连载中",
                wordCount = 1234567,
                favoriteCount = 2345,
                siteReadCount = 120000,
                sourceReadCount = 980000,
                sourceFavoriteCount = 45000,
                updatedAt = "2026-07-02T08:30:00Z"
            )
        )

        assertEquals(
            listOf(
                "状态: 连载中",
                "作者: Author Name",
                "来源: 上传",
                "字数: 1,234,567",
                "收藏: 2,345",
                "本站阅读: 120,000",
                "源阅读: 980,000",
                "源收藏: 45,000",
                "更新: 2026-07-02T08:30:00Z"
            ),
            facts
        )
        facts.forEach(::assertNoMojibake)
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
