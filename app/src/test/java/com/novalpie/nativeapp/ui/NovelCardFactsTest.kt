package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertEquals
import org.junit.Test

class NovelCardFactsTest {
    @Test
    fun novelCardFactsIncludeStatusWordCountAndShortUpdateDate() {
        val facts = novelCardFacts(
            NovelCard(
                id = 354491,
                title = "Native Book",
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
                "状态 连载中",
                "字数 1,234,567",
                "收藏 2,345",
                "本站阅读 120,000",
                "源阅读 980,000",
                "源收藏 45,000",
                "更新 2026-07-02"
            ),
            facts
        )
    }

    @Test
    fun novelCardFactsSkipBlankOrMissingValues() {
        val facts = novelCardFacts(
            NovelCard(
                id = 354491,
                title = "Native Book",
                status = " ",
                wordCount = null,
                updatedAt = null
            )
        )

        assertEquals(emptyList<String>(), facts)
    }

    @Test
    fun novelCardTagsTrimDeduplicateAndKeepCompleteWebsiteTagSet() {
        val tags = novelCardTags(
            NovelCard(
                id = 354491,
                title = "Native Book",
                tags = listOf(" Fantasy ", "", "Drama", "Fantasy", "Adventure", "Longform", "Extra")
            )
        )

        assertEquals(listOf("Fantasy", "Drama", "Adventure", "Longform", "Extra"), tags)
    }

    @Test
    fun novelDisplayCoverUrlPrefersFullCoverAndFallsBackToGridCover() {
        assertEquals(
            "https://images.novelpia.com/original.file",
            novelDisplayCoverUrl(
                NovelCard(
                    id = 354491,
                    title = "Native Book",
                    coverUrl = "https://images.novelpia.com/thumb.file",
                    fullCoverUrl = " https://images.novelpia.com/original.file "
                )
            )
        )
        assertEquals(
            "https://images.novelpia.com/thumb.file",
            novelDisplayCoverUrl(
                NovelCard(
                    id = 354491,
                    title = "Native Book",
                    coverUrl = " https://images.novelpia.com/thumb.file "
                )
            )
        )
    }

    @Test
    fun searchResultPreviewKeepsMobileCardCompact() {
        val preview = novelSearchPreview(
            NovelCard(
                id = 354491,
                title = "Native Book",
                originalTitle = "네이티브 북",
                author = "Author",
                platform = "upload",
                status = "连载中",
                description = "A long story that should be clipped in the visual card.",
                wordCount = 1234567,
                favoriteCount = 2345,
                siteReadCount = 120000,
                updatedAt = "2026-07-02T08:30:00Z",
                tags = listOf("Fantasy", "Drama", "Adventure", "Longform", "Extra")
            )
        )

        assertEquals("Author", preview.authorLabel)
        assertEquals("네이티브 북", preview.originalTitleLabel)
        assertEquals("上传", preview.platformLabel)
        assertEquals(listOf("状态 连载中", "字数 1,234,567", "收藏 2,345", "本站阅读 120,000", "更新 2026-07-02"), preview.facts)
        assertEquals(listOf("Fantasy", "Drama", "Adventure", "Longform", "Extra"), preview.tags)
    }

    @Test
    fun originalTitleAndPlatformLabelsSkipRedundantValues() {
        val preview = novelSearchPreview(
            NovelCard(
                id = 354491,
                title = "Native Book",
                originalTitle = " Native Book ",
                platform = "novelPia"
            )
        )

        assertEquals(null, preview.originalTitleLabel)
        assertEquals("NovelPia", preview.platformLabel)
    }
}
