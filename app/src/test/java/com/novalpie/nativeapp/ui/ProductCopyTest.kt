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
    }

    // Two tests were removed here, and it is worth recording why rather than leaving a gap.
    //
    // They asserted properties of forumFeedItems() -- that it held exactly 6 entries, that some
    // were pinned, that some were featured, that titles stayed under 18 characters. That fixture
    // was six hardcoded forum threads with invented authors, reply counts and view counts, and
    // ForumScreen substituted it for real content whenever the feed was idle, loading, failed or
    // empty. The tests were therefore pinning fabricated content in place: they would have failed
    // if anyone removed it.
    //
    // The fixture is gone and ForumScreen now renders explicit loading, error and empty states, so
    // there is nothing left for these tests to assert. The 37 strings they covered are recorded in
    // the commit that removed them.

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
