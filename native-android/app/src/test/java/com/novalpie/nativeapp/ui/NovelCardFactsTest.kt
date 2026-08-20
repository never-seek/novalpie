package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertEquals
import org.junit.Test

class NovelCardFactsTest {
    @Test
    fun cardMetadataUsesWrappingInsteadOfTruncation() {
        assertEquals(Int.MAX_VALUE, NOVEL_CARD_METADATA_MAX_LINES)
    }

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
    fun novelThumbnailCoverUrlPrefersGridCoverAndFallsBackToOriginal() {
        assertEquals(
            "https://images.novelpia.com/thumb.file",
            novelThumbnailCoverUrl(
                NovelCard(
                    id = 354491,
                    title = "Native Book",
                    coverUrl = " https://images.novelpia.com/thumb.file ",
                    fullCoverUrl = "https://images.novelpia.com/original.file"
                )
            )
        )
        assertEquals(
            "https://images.novelpia.com/original.file",
            novelThumbnailCoverUrl(
                NovelCard(
                    id = 354491,
                    title = "Native Book",
                    fullCoverUrl = " https://images.novelpia.com/original.file "
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

    @Test
    fun sourceStyleCardPresentationKeepsCoverBadgesAndCompactMetrics() {
        val book = NovelCard(
            id = 759,
            title = "Source Book",
            platform = "novelPia",
            status = "15 PLUS 独家 完结",
            favoriteCount = 143,
            siteReadCount = 16_100,
            wordCount = 161_000,
            tags = listOf("奇幻", "轻小说", "后悔")
        )

        assertEquals(
            NovelCardCoverBadges(category = "奇幻", status = "完结"),
            novelCardCoverBadges(book)
        )
        assertEquals(
            listOf(
                NovelCardCompactMetric(NovelCardMetricKind.Favorite, "本站收藏 143", "143"),
                NovelCardCompactMetric(NovelCardMetricKind.Read, "本站阅读 1.6w", "1.6w"),
                NovelCardCompactMetric(NovelCardMetricKind.WordCount, "字数 16.1w", "16.1w")
            ),
            novelCardCompactMetrics(book)
        )
    }

    @Test
    fun compactMetricFormattingMatchesSourceThousandAndTenThousandNotation() {
        assertEquals("999", formatNovelCardCompactCount(999))
        assertEquals("1k", formatNovelCardCompactCount(1_000))
        assertEquals("9.9k", formatNovelCardCompactCount(9_900))
        assertEquals("1w", formatNovelCardCompactCount(10_000))
        assertEquals("16.1w", formatNovelCardCompactCount(161_000))
    }

    @Test
    fun cardAccessibilityLabelKeepsTheCardActionAndVisibleSourceFacts() {
        val label = novelCardAccessibilityLabel(
            NovelCard(
                id = 759,
                title = "Source Book",
                author = "Source Author",
                platform = "upload",
                status = "连载中",
                favoriteCount = 143,
                siteReadCount = 16_100,
                wordCount = 161_000,
                tags = listOf("奇幻", "轻小说")
            )
        )

        assertEquals(
            "打开 Source Book，作者 Source Author，来源 上传，状态 连载中，标签 奇幻、轻小说，本站收藏 143，本站阅读 1.6w，字数 16.1w",
            label
        )
    }

    @Test
    fun sourceStatusTagBecomesCoverBadgeInsteadOfReplacingTheGenre() {
        val book = NovelCard(
            id = 360209,
            title = "Upload Book",
            status = null,
            tags = listOf("已完结", "奇幻", "冒险")
        )

        assertEquals(
            NovelCardCoverBadges(category = "奇幻", status = "已完结"),
            novelCardCoverBadges(book)
        )
        assertEquals(listOf("奇幻", "冒险"), novelCardContentTags(book))
    }
}
