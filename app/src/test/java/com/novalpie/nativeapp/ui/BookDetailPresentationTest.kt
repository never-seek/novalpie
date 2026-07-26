package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChapterComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BookDetailPresentationTest {
    @Test
    fun bookDetailPrimaryActionsPrioritizeReadingWithoutAddingUnsupportedFeatures() {
        assertEquals(
            listOf("继续阅读", "开始阅读", "网页详情"),
            bookDetailPrimaryActions(hasProgress = true)
        )
        assertEquals(
            listOf("开始阅读", "网页详情"),
            bookDetailPrimaryActions(hasProgress = false)
        )
    }

    @Test
    fun bookDetailPrimaryActionsDoNotExposeUnsupportedCrawlerOrDownloadTools() {
        val forbidden = listOf("书源", "规则", "爬取", "下载", "净化", "编辑源")

        bookDetailPrimaryActions(hasProgress = true).forEach { action ->
            forbidden.forEach { word -> assertFalse(action.contains(word, ignoreCase = true)) }
        }
    }

    @Test
    fun bookDetailStatusLabelsAreCompactProductCopy() {
        assertEquals("已收藏", bookDetailFavoriteLabel(isFavorited = true))
        assertEquals("未收藏", bookDetailFavoriteLabel(isFavorited = false))
        assertEquals("收藏同步中", bookDetailFavoriteLoadingLabel())
        assertEquals("收藏状态不可用", bookDetailFavoriteUnavailableLabel())
    }

    @Test
    fun bookDetailCommentMetricsMirrorWebsiteActions() {
        val comment = ChapterComment(
            id = 45570,
            bookId = 354491,
            authorName = "Book Reader",
            content = "book comment body",
            likeCount = 4,
            dislikeCount = 1,
            reactionCount = 3,
            awardPoints = 5
        )

        assertEquals(
            listOf("赞 4", "踩 1", "表情 3", "打赏 5", "回复"),
            bookCommentMetricLabels(comment)
        )
        assertEquals("评论区", bookCommentsSectionTitle())
        assertEquals("打开网页评论", bookCommentsFallbackLabel())
    }
}
