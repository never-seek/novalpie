package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.BookEditPermissions
import com.novalpie.nativeapp.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailPresentationTest {
    @Test
    fun bookManagementActionsFollowWebsiteFieldPermissions() {
        assertFalse(bookManagementActionsVisible(null))
        assertFalse(bookManagementActionsVisible(BookEditPermissions()))
        assertTrue(bookManagementActionsVisible(BookEditPermissions(description = true)))
        assertTrue(bookManagementActionsVisible(BookEditPermissions(tags = true)))
    }

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
    fun chapterRowsUseTheLiveSourceEpisodeDateWordAndIllustrationFields() {
        val presentation = chapterListPresentation(
            Chapter(
                id = 1419162,
                title = "序章",
                number = 1,
                wordCount = 1110,
                imageCount = 1,
                updatedAt = "2025-12-29 02:29:33",
            ),
        )

        assertEquals("EP.1", presentation.numberLabel)
        assertEquals("2025年12月29日 02:29", presentation.updatedLabel)
        assertEquals(listOf("1.1K字", "1图"), presentation.metrics)
        assertEquals(
            ChapterListPresentation(
                numberLabel = "第1章",
                updatedLabel = null,
                metrics = listOf("1.1K", "1图"),
            ),
            chapterListPresentation(
                Chapter(
                    id = 1419162,
                    title = "序章",
                    number = 1,
                    wordCount = 1110,
                    imageCount = 1,
                    updatedAt = "2025-12-29 02:29:33",
                ),
                context = ChapterListContext.ReaderCatalog,
            ),
        )
        assertEquals("3K", formatSourceChapterCount(3000))
        assertEquals("1.3M", formatSourceChapterCount(1_289_814))
    }

    @Test
    fun bookDetailCatalogSupportsSourceOrderSwitchWithoutMutatingTheLoadedList() {
        val chapters = listOf(
            Chapter(id = 10, title = "第一章", number = 1),
            Chapter(id = 20, title = "第二章", number = 2),
            Chapter(id = 30, title = "第三章", number = 3),
        )

        assertEquals(
            listOf(10L, 20L, 30L),
            sortBookDetailChapters(chapters, BookDetailCatalogOrder.Ascending).map(Chapter::id),
        )
        assertEquals(
            listOf(30L, 20L, 10L),
            sortBookDetailChapters(chapters, BookDetailCatalogOrder.Descending).map(Chapter::id),
        )
        assertEquals(listOf(10L, 20L, 30L), chapters.map(Chapter::id))
        assertEquals("正文卷 · 共 378 章", bookDetailCatalogHeading(378))
        assertEquals("正文卷 · 共 0 章", bookDetailCatalogHeading(-1))
    }

    @Test
    fun bookDetailKeepsAdvertisedChapterCountAndDownloadPolicyWhenCatalogIsUnavailable() {
        val loaded = listOf(Chapter(id = 1, title = "第一章"), Chapter(id = 2, title = "第二章"))

        assertEquals(2, bookDetailDisplayedChapterCount(loaded, sourceChapterCount = 378))
        assertEquals(378, bookDetailDisplayedChapterCount(emptyList(), sourceChapterCount = 378))
        assertEquals(0, bookDetailDisplayedChapterCount(emptyList(), sourceChapterCount = null))
        assertEquals(0, bookDetailDisplayedChapterCount(emptyList(), sourceChapterCount = -1))
        assertTrue(bookDetailAllowsNativeEpubDownload(hasAuthToken = true, allowDownload = true))
        assertTrue(bookDetailAllowsNativeEpubDownload(hasAuthToken = true, allowDownload = null))
        assertFalse(bookDetailAllowsNativeEpubDownload(hasAuthToken = true, allowDownload = false))
        assertFalse(bookDetailAllowsNativeEpubDownload(hasAuthToken = false, allowDownload = true))
        assertEquals("原生下载 EPUB（保存到下载）", nativeEpubDownloadMenuLabel(isDownloading = false))
        assertEquals("正在原生下载 EPUB…", nativeEpubDownloadMenuLabel(isDownloading = true))
        assertEquals("原生下载 TXT（保存到下载）", nativeTxtDownloadMenuLabel(isDownloading = false))
        assertEquals("正在原生下载 TXT…", nativeTxtDownloadMenuLabel(isDownloading = true))
    }

    @Test
    fun bookDetailTabsExposeOnlyTheSelectedContentPane() {
        assertEquals("简介", bookDetailTabLabel(BookDetailContentTabLabel.Introduction, 378))
        assertEquals("目录 378", bookDetailTabLabel(BookDetailContentTabLabel.Catalog, 378))
        assertEquals("评论", bookDetailTabLabel(BookDetailContentTabLabel.Comments, 378))
        assertEquals(
            listOf(BookDetailContentTabLabel.Catalog),
            bookDetailVisibleContentSections(BookDetailContentTabLabel.Catalog),
        )
        assertEquals(
            listOf(BookDetailContentTabLabel.Comments),
            bookDetailVisibleContentSections(BookDetailContentTabLabel.Comments),
        )
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
        assertEquals("书评", bookCommentsSectionTitle())
        assertEquals("打开网页书评", bookCommentsFallbackLabel())
    }

    @Test
    fun bookCommentThreadsKeepNestedRepliesUnderTheirSourceRoot() {
        val root = ChapterComment(
            id = 4925,
            bookId = 772,
            authorName = "Root User",
            content = "root",
            replyCount = 2,
        )
        val reply = ChapterComment(
            id = 829,
            bookId = 772,
            parentCommentId = 4925,
            authorName = "Reply User",
            replyToName = "Root User",
            content = "reply",
        )
        val nestedReply = ChapterComment(
            id = 1214,
            bookId = 772,
            parentCommentId = 829,
            authorName = "Nested User",
            replyToName = "Reply User",
            content = "nested reply",
        )
        val independent = ChapterComment(
            id = 57600,
            bookId = 772,
            authorName = "Independent User",
            content = "independent",
        )

        val threads = chapterCommentThreads(listOf(root, reply, nestedReply, independent))

        assertEquals(2, threads.size)
        assertEquals(4925L, threads[0].comment.id)
        assertEquals(listOf(829L, 1214L), threads[0].replies.map(ChapterComment::id))
        assertEquals(57600L, threads[1].comment.id)
        assertEquals("2 条书评 · 2 条回复", chapterCommentThreadSummary(threads, rootLabel = "书评"))
    }
}
