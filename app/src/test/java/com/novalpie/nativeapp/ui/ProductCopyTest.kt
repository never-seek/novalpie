package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductCopyTest {
    @Test
    fun primaryProductHeadersUseCleanNovelForumLanguage() {
        assertEquals(ProductHeader("书架", "收藏、分组和阅读进度"), productHeader(ProductSurface.Library))
        assertEquals(ProductHeader("发现", "搜索作品、作者和标签"), productHeader(ProductSurface.Discover))
        assertEquals(ProductHeader("我的", "账号、阅读偏好和连接设置"), productHeader(ProductSurface.Profile))
    }

    @Test
    fun primaryProductHeadersDoNotExposeApiDebugLanguageOrMojibake() {
        ProductSurface.values().forEach { surface ->
            val header = productHeader(surface)
            assertCleanVisibleCopy(header.title)
            assertCleanVisibleCopy(header.subtitle)
            assertFalse(header.title.contains("API", ignoreCase = true))
            assertFalse(header.subtitle.contains("API", ignoreCase = true))
            assertFalse(header.title.contains("诊断"))
            assertFalse(header.subtitle.contains("诊断"))
        }
    }

    @Test
    fun accountSyncSummaryUsesUserLanguage() {
        assertEquals("登录同步: 已连接", accountSyncSummary(true))
        assertEquals("登录同步: 未同步", accountSyncSummary(false))
    }

    @Test
    fun libraryPrimaryActionsStayCompactForPhoneWidth() {
        assertEquals(listOf("同步书架", "登录同步", "网页收藏"), libraryPrimaryActions())
        assertEquals(3, libraryPrimaryActions().size)
    }

    @Test
    fun discoverPrimaryActionsAppearBeforeAdvancedFilters() {
        assertEquals(listOf("搜索", "网页发现"), discoverPrimaryActions())
        assertEquals(2, discoverPrimaryActions().size)
    }

    @Test
    fun discoverFilterLabelsFollowWebsiteGroups() {
        assertEquals(listOf("排序", "顺序", "范围", "内容", "字数", "来源", "模式"), discoverFilterLabels())
    }

    @Test
    fun discoverSelectedFilterSummariesFitSingleHorizontalRail() {
        val options = SearchOptions(
            sortBy = "favorite_count",
            sortOrder = "desc",
            scope = "tags",
            matchType = "ai",
            adultFilter = "adult_only",
            source = "novelPia",
            wordCountRange = "100000..500000"
        )

        assertEquals(
            listOf("排序: 收藏数", "顺序: 降序", "范围: 仅标签", "内容: 仅成人", "字数: 10-50万", "来源: NovelPia", "模式: AI搜索"),
            discoverSelectedFilterSummaries(options)
        )
    }

    @Test
    fun bookDetailSectionsReadLikeNovelProductPage() {
        assertEquals(
            listOf("作品", "阅读", "章节目录", "评论区"),
            bookDetailSectionTitles()
        )
    }

    @Test
    fun readerChromeUsesImmersiveReaderLanguage() {
        assertEquals("阅读", readerScreenTitle())
        assertEquals("章节", readerCatalogTitle())
    }

    @Test
    fun forumCardsDoNotExposeImplementationNotes() {
        val forbidden = listOf("参考", "建议中", "fallback", "API")
        forumCardCopies().forEach { copy ->
            listOf(copy.title, copy.subtitle, copy.meta).forEach { value ->
                assertCleanVisibleCopy(value)
                forbidden.forEach { word -> assertFalse(value.contains(word, ignoreCase = true)) }
            }
        }
    }

    @Test
    fun forumHomeUsesForumClientFeedStructure() {
        assertEquals(listOf("全部", "书评", "章节", "动态"), forumFeedTabs())
        assertEquals(6, forumFeedItems().size)
        assert(forumFeedItems().any { it.pinned }) { "Forum feed should include pinned or highlighted topics." }
        assert(forumFeedItems().any { it.featured }) { "Forum feed should include featured topics from the website model." }
        forumFeedItems().forEach { item ->
            listOf(item.category, item.title, item.bookTitle, item.authorName, item.lastActiveLabel).forEach(::assertCleanVisibleCopy)
            assert(item.title.length <= 18) { "Forum feed title is too long: ${item.title}" }
            assert(item.replyCount >= 0)
            assertFalse(item.bookTitle.isBlank())
            assertFalse(item.authorName.isBlank())
            assertFalse(item.lastActiveLabel.isBlank())
            assert(item.tags.isNotEmpty()) { "Forum feed item should carry scan-friendly tags." }
            assert(item.tags.size <= 3) { "Forum feed tags should stay compact." }
        }
    }

    @Test
    fun forumFeedCopyAvoidsUnsupportedReaderTooling() {
        val forbidden = listOf("书源", "规则编辑", "爬取", "下载源", "净化", "fallback", "API")
        forumFeedItems().forEach { item ->
            listOf(item.category, item.title, item.bookTitle, item.authorName, item.lastActiveLabel)
                .plus(item.tags)
                .forEach { value ->
                    forbidden.forEach { word -> assertFalse(value.contains(word, ignoreCase = true)) }
                }
        }
    }

    private fun assertCleanVisibleCopy(value: String) {
        val mojibakeFragments = listOf(
            "涔", "鏀", "銆", "闃", "鍙", "鎼", "浣", "璐", "鐧", "缃",
            "鎺", "椤", "鑼", "鍐", "瀛", "妯", "绔", "璇", "鍔", "婧"
        )
        mojibakeFragments.forEach { fragment ->
            assertFalse("Visible copy contains mojibake '$fragment': $value", value.contains(fragment))
        }
    }
}
