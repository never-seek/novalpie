package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.SearchPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverPresentationTest {
    @Test
    fun discoverOverviewUsesContentClientSearchLanguage() {
        assertEquals(
            DiscoverOverview(
                title = "发现",
                subtitle = "搜索作品、作者和标签",
                hint = "输入关键词、作品名或作者",
                statusLabel = "就绪"
            ),
            discoverOverview(LoadResult.Idle)
        )
    }

    @Test
    fun discoverStatusLabelsAreUserFacing() {
        assertEquals("加载中", discoverOverview(LoadResult.Loading).statusLabel)
        assertEquals("错误", discoverOverview(LoadResult.Error("boom")).statusLabel)
        assertEquals("3 个结果", discoverOverview(LoadResult.Success(listOf(1, 2, 3))).statusLabel)
    }

    @Test
    fun discoverFilterGroupsMatchWebsiteSearchControls() {
        val groups = discoverFilterGroups(
            SearchOptions(
                sortBy = "favorite_count",
                sortOrder = "desc",
                scope = "tags",
                matchType = "ai",
                adultFilter = "adult_only",
                source = "novelPia",
                wordCountRange = "100000..500000"
            )
        )

        assertEquals(listOf("排序方式", "排序方向", "搜索范围", "内容筛选", "字数", "来源", "搜索模式"), groups.map { it.label })
        assertEquals(
            listOf("相关度", "更新时间", "上架时间", "收藏数", "本站阅读", "推荐", "源阅读", "字数", "源收藏"),
            groups[0].choices.map { it.label }
        )
        assertEquals(listOf("降序", "升序"), groups[1].choices.map { it.label })
        assertEquals(listOf("全部内容", "仅标题", "仅作者", "仅标签"), groups[2].choices.map { it.label })
        assertEquals(listOf("所有", "仅成人", "全年龄"), groups[3].choices.map { it.label })
        assertEquals(listOf("不限", "10万以下", "10-50万", "50-100万", "100万以上"), groups[4].choices.map { it.label })
        assertEquals(listOf("全部", "NovelPia", "上传"), groups[5].choices.map { it.label })
        assertEquals(listOf("AI搜索", "模糊-严格", "模糊-宽松", "精确匹配"), groups[6].choices.map { it.label })
        assertEquals("100000..500000", groups[4].choices.single { it.selected }.value)
        assertEquals("novelPia", groups[5].choices.single { it.selected }.value)
        assertEquals("ai", groups[6].choices.single { it.selected }.value)
        groups.flatMap { group -> listOf(group.label).plus(group.choices.map { it.label }) }
            .forEach(::assertNoMojibake)
    }

    @Test
    fun sourceSearchFiltersStartExpandedSoWebsiteControlsAreImmediatelyAvailable() {
        assertTrue(SOURCE_SEARCH_FILTERS_START_EXPANDED)
    }

    @Test
    fun sourceSearchRuleControlsKeepTheMobileWebsiteDensity() {
        assertEquals(32, SOURCE_SEARCH_RULE_ROW_HEIGHT_DP)
        assertEquals(68, SOURCE_SEARCH_RULE_LABEL_WIDTH_DP)
        assertEquals(132, SOURCE_SEARCH_RULE_CONTROL_WIDTH_DP)
        assertEquals(520, SOURCE_SEARCH_RULES_TWO_COLUMN_MIN_WIDTH_DP)
        assertEquals(72, SOURCE_SEARCH_RULE_PAIR_LABEL_WIDTH_DP)
    }

    @Test
    fun sourceSearchRulesStackOnPhoneWidthsAndPairOnlyOnWideWindows() {
        assertFalse(sourceSearchRulesUseTwoColumns(412))
        assertFalse(sourceSearchRulesUseTwoColumns(519))
        assertTrue(sourceSearchRulesUseTwoColumns(520))
        assertTrue(sourceSearchRulesUseTwoColumns(900))
    }

    @Test
    fun sourceSearchGridUsesMoreColumnsOnRealWideAndroidWindows() {
        assertEquals(2, sourceSearchGridColumnCount(412))
        assertEquals(2, sourceSearchGridColumnCount(600))
        assertEquals(4, sourceSearchGridColumnCount(720))
        assertEquals(5, sourceSearchGridColumnCount(900))
        assertEquals(6, sourceSearchGridColumnCount(1_066))
    }

    @Test
    fun sourcePrimaryRulesExposeTheWebsiteContentRatingFilter() {
        val options = SearchOptions(adultFilter = "adult_only")

        val primaryRules = sourcePrimarySearchFilterGroups(options)

        assertEquals(
            listOf("搜索范围", "内容筛选", "来源", "搜索模式", "排序方式", "排序方向"),
            primaryRules.map(DiscoverFilterGroup::label),
        )
        assertEquals(
            "adult_only",
            primaryRules.single { group ->
                group.choices.any { choice -> choice.value == "adult_only" }
            }.choices.single { choice -> choice.selected }.value,
        )
    }

    @Test
    fun sourceDefaultSearchUsesTheWebsiteSafeContentRating() {
        val contentRating = sourcePrimarySearchFilterGroups(SearchOptions())
            .single { group -> group.choices.any { choice -> choice.value == "adult_only" } }

        assertEquals(
            "unrestricted",
            contentRating.choices.single { choice -> choice.selected }.value,
        )
    }

    @Test
    fun discoverUnsupportedReaderToolingDoesNotAppear() {
        val forbidden = listOf("书源", "爬取", "净化", "编辑源", "API", "fallback")
        val copy = listOf(discoverOverview(LoadResult.Idle).title, discoverOverview(LoadResult.Idle).subtitle)
            .plus(discoverPrimaryActions())
            .plus(discoverFilterGroups(SearchOptions()).flatMap { group ->
                listOf(group.label).plus(group.choices.map { it.label })
            })

        copy.forEach { value ->
            forbidden.forEach { word -> assertFalse(value.contains(word, ignoreCase = true)) }
        }
    }

    @Test
    fun discoverEmptyStateOffersSearchPromptsInsteadOfBlankSpace() {
        val prompts = discoverQuickPrompts()

        assertEquals(listOf("最近更新", "热门书评", "长篇连载", "完结作品"), prompts)
        prompts.forEach(::assertNoMojibake)
        assertEquals("输入关键词后搜索，也可以先看推荐方向。", discoverIdleMessage())
    }

    @Test
    fun discoverTagLabelsShowWebsiteTagNameAndCount() {
        val labels = discoverTagLabels(
            listOf(
                NovelTag(id = 1, name = "异世界", count = 88),
                NovelTag(id = 2, name = "完结", count = null)
            )
        )

        assertEquals(listOf("异世界 88", "完结"), labels)
    }

    @Test
    fun discoverSectionOrderPromotesResultsAfterSearchStarts() {
        assertEquals(
            listOf(DiscoverSection.SearchPanel, DiscoverSection.Filters, DiscoverSection.History, DiscoverSection.IdlePrompts),
            discoverSectionOrder(LoadResult.Idle, hasHistory = true)
        )
        assertEquals(
            listOf(DiscoverSection.SearchPanel, DiscoverSection.Filters, DiscoverSection.Results, DiscoverSection.History),
            discoverSectionOrder(LoadResult.Success(listOf("book")), hasHistory = true)
        )
        assertEquals(
            listOf(DiscoverSection.SearchPanel, DiscoverSection.Filters, DiscoverSection.Results),
            discoverSectionOrder(LoadResult.Loading, hasHistory = false)
        )
    }

    @Test
    fun advancedSyntaxHidesTheBasicFilterPagesButKeepsSearchAndResults() {
        assertEquals(
            listOf(DiscoverSection.SearchPanel, DiscoverSection.History, DiscoverSection.IdlePrompts),
            discoverSectionOrder(
                results = LoadResult.Idle,
                hasHistory = true,
                advancedSyntaxEnabled = true
            )
        )
        assertEquals(
            listOf(DiscoverSection.SearchPanel, DiscoverSection.Results),
            discoverSectionOrder(
                results = LoadResult.Success(listOf("book")),
                hasHistory = false,
                advancedSyntaxEnabled = true
            )
        )
    }

    @Test
    fun searchPaginationMatchesTheLiveFivePageWindowAndJumpBounds() {
        val firstPage = SearchPage(
            items = emptyList(),
            page = 1,
            pageSize = 60,
            total = 47331,
            totalPages = 789
        )
        val middlePage = firstPage.copy(page = 5)

        assertEquals(listOf(1, 2, 3, 4, 5), searchPaginationWindow(firstPage).pages)
        assertEquals(null, searchPaginationWindow(firstPage).previousPage)
        assertEquals(2, searchPaginationWindow(firstPage).nextPage)
        assertEquals(listOf(3, 4, 5, 6, 7), searchPaginationWindow(middlePage).pages)
        assertEquals(4, searchPaginationWindow(middlePage).previousPage)
        assertEquals(6, searchPaginationWindow(middlePage).nextPage)
        assertEquals("全部小说", searchResultsHeading("", firstPage))
        assertEquals("共 47331 部作品", searchResultsCountLabel(firstPage))
        assertEquals(789, searchPageJumpTarget("789", 789))
        assertEquals(null, searchPageJumpTarget("790", 789))
        assertEquals(null, searchPageJumpTarget("0", 789))
    }

    private fun assertNoMojibake(value: String) {
        val fragments = listOf("闁", "婵", "闂", "缂", "濞", "閺", "閳", "娑", "閹", "閻", "缁", "鐠", "娴")
        fragments.forEach { fragment ->
            assertFalse("Visible copy contains mojibake '$fragment': $value", value.contains(fragment))
        }
    }
}
