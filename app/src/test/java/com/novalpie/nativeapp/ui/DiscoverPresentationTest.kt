package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        assertEquals(listOf("排序", "顺序", "范围", "内容", "字数", "来源", "模式"), groups.map { it.label })
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
            listOf(DiscoverSection.SearchPanel, DiscoverSection.History, DiscoverSection.Tags, DiscoverSection.Filters, DiscoverSection.IdlePrompts),
            discoverSectionOrder(LoadResult.Idle, hasHistory = true)
        )
        assertEquals(
            listOf(DiscoverSection.SearchPanel, DiscoverSection.Results, DiscoverSection.History, DiscoverSection.Tags, DiscoverSection.Filters),
            discoverSectionOrder(LoadResult.Success(listOf("book")), hasHistory = true)
        )
        assertEquals(
            listOf(DiscoverSection.SearchPanel, DiscoverSection.Results, DiscoverSection.Tags, DiscoverSection.Filters),
            discoverSectionOrder(LoadResult.Loading, hasHistory = false)
        )
    }

    private fun assertNoMojibake(value: String) {
        val fragments = listOf("闁", "婵", "闂", "缂", "濞", "閺", "閳", "娑", "閹", "閻", "缁", "鐠", "娴")
        fragments.forEach { fragment ->
            assertFalse("Visible copy contains mojibake '$fragment': $value", value.contains(fragment))
        }
    }
}
