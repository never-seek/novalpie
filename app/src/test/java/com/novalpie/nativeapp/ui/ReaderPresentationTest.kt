package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChapterComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderPresentationTest {
    @Test
    fun readerDoesNotExposeSourceDebugLine() {
        assertNull(readerSourceDebugLine("api"))
        assertNull(readerSourceDebugLine("fallback"))
        assertNull(readerSourceDebugLine(""))
    }

    @Test
    fun readerDoesNotExposeBookAndChapterDebugIdentityLine() {
        assertNull(readerDebugIdentityLine(bookId = 354491, chapterId = 8001))
    }

    @Test
    fun readerToolbarUsesNativeReaderActionsOnly() {
        assertEquals(
            listOf("上一章", "目录", "下一章", "A-", "A+", "主题", "网页"),
            readerToolbarLabels()
        )
    }

    @Test
    fun readerCatalogPanelUsesReaderAppLanguage() {
        assertEquals("章节目录", readerCatalogPanelTitle())
        assertEquals("回到正文", readerCloseCatalogLabel())
        assertEquals(listOf("正文", "目录", "设置"), readerSurfaceSections())
    }

    @Test
    fun readerToolbarDoesNotExposeCrawlerOrEditorActions() {
        val forbidden = listOf("书源", "规则", "编辑", "爬取", "下载", "净化")
        readerToolbarLabels()
            .plus(readerCatalogPanelTitle())
            .plus(readerCloseCatalogLabel())
            .plus(readerSurfaceSections())
            .forEach { label ->
                forbidden.forEach { word ->
                    assertFalse(label.contains(word, ignoreCase = true))
                }
            }
    }

    @Test
    fun readerRouteOwnsReaderChromeInsteadOfGlobalProductChrome() {
        assertFalse(globalProductTopBarVisible(AppRoute.Reader(bookId = 354491, chapterId = 8001)))
    }

    @Test
    fun readerTopBarUsesReaderSpecificLabels() {
        assertEquals(ReaderTopBarLabels(back = "返回", title = "阅读", web = "网页"), readerTopBarLabels())
    }

    @Test
    fun readerIllustrationPreviewCopySupportsTapAndLongPressLargeImage() {
        assertEquals("正文插图 1", readerIllustrationLabel(null, 1))
        assertEquals("正文插图 1", readerIllustrationLabel("   ", 0))
        assertEquals("章节插画", readerIllustrationLabel(" 章节插画 ", 2))
        assertEquals("正文插图 1，点击或长按查看大图", readerIllustrationContentDescription("正文插图 1"))
        assertEquals("点击 / 长按看大图", readerIllustrationPreviewHint())
        assertEquals("正在加载插图", readerIllustrationLoadingLabel())
        assertEquals("插图加载失败", readerIllustrationErrorLabel())
    }

    @Test
    fun chapterCommentMetricsMirrorWebsiteActions() {
        val comment = ChapterComment(
            id = 701,
            chapterId = 9901,
            authorName = "章节读者",
            content = "章节评论正文",
            likeCount = 8,
            dislikeCount = 1,
            reactionCount = 3,
            awardPoints = 20
        )

        assertEquals(
            listOf("赞 8", "踩 1", "表情 3", "打赏 20", "回复"),
            chapterCommentMetricLabels(comment)
        )
        assertEquals("章节评论", chapterCommentsSectionTitle())
        assertEquals("打开网页评论", chapterCommentsFallbackLabel())
    }
}
