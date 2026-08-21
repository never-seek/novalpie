package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.SearchPage

internal data class DiscoverOverview(
    val title: String,
    val subtitle: String,
    val hint: String,
    val statusLabel: String
)

internal data class DiscoverFilterChoice(
    val value: String,
    val label: String,
    val selected: Boolean
)

internal data class DiscoverFilterGroup(
    val label: String,
    val choices: List<DiscoverFilterChoice>
)

/** Compact five-page window used by the mobile source at both ends of a result page. */
internal data class SearchPaginationWindow(
    val currentPage: Int,
    val totalPages: Int,
    val pages: List<Int>,
    val previousPage: Int?,
    val nextPage: Int?
)

internal enum class DiscoverSection {
    SearchPanel,
    Filters,
    Results,
    History,
    IdlePrompts
}

/** The live mobile search starts with its rules/tag/word-count rail visible. */
internal const val SOURCE_SEARCH_FILTERS_START_EXPANDED = true

/** The mobile source renders its selector rows at 32px; keep native controls equally dense. */
internal const val SOURCE_SEARCH_RULE_ROW_HEIGHT_DP = 32
// "内容筛选：" is the widest source rule label. Keep it whole on a 360dp phone rather
// than trading readability for a few unused pixels at the end of each compact row.
internal const val SOURCE_SEARCH_RULE_LABEL_WIDTH_DP = 68
internal const val SOURCE_SEARCH_RULE_CONTROL_WIDTH_DP = 132
/** At this width two compact label/select pairs stay readable side by side on a large phone. */
internal const val SOURCE_SEARCH_RULES_TWO_COLUMN_MIN_WIDTH_DP = 520
/** Wide enough for the source's longest rule label, "内容筛选：", in a two-column row. */
internal const val SOURCE_SEARCH_RULE_PAIR_LABEL_WIDTH_DP = 72

/** Phone-width source rules stack vertically; only a genuinely wide window keeps paired rows. */
internal fun sourceSearchRulesUseTwoColumns(availableWidthDp: Int): Boolean =
    availableWidthDp >= SOURCE_SEARCH_RULES_TWO_COLUMN_MIN_WIDTH_DP

// The source uses a 2 / 4 / 5 / 6 column Tailwind grid. Native uses dp breakpoints so a
// phone stays comfortably two-up in portrait while MuMu/tablet landscape no longer wastes most
// of the screen on two oversized covers.
internal const val SOURCE_SEARCH_GRID_MEDIUM_MIN_WIDTH_DP = 720
internal const val SOURCE_SEARCH_GRID_LARGE_MIN_WIDTH_DP = 900
internal const val SOURCE_SEARCH_GRID_EXTRA_LARGE_MIN_WIDTH_DP = 1_000

internal fun sourceSearchGridColumnCount(availableWidthDp: Int): Int = when {
    availableWidthDp >= SOURCE_SEARCH_GRID_EXTRA_LARGE_MIN_WIDTH_DP -> 6
    availableWidthDp >= SOURCE_SEARCH_GRID_LARGE_MIN_WIDTH_DP -> 5
    availableWidthDp >= SOURCE_SEARCH_GRID_MEDIUM_MIN_WIDTH_DP -> 4
    else -> 2
}

internal fun discoverOverview(results: LoadResult<*>): DiscoverOverview {
    val header = productHeader(ProductSurface.Discover)
    return DiscoverOverview(
        title = header.title,
        subtitle = header.subtitle,
        hint = "输入关键词、作品名或作者",
        statusLabel = discoverStatusLabel(results)
    )
}

internal fun discoverStatusLabel(results: LoadResult<*>): String = when (results) {
    LoadResult.Idle -> "就绪"
    LoadResult.Loading -> "加载中"
    is LoadResult.Error -> "错误"
    is LoadResult.Success<*> -> {
        val count = (results.value as? Collection<*>)?.size ?: 0
        "$count 个结果"
    }
}

internal fun searchResultsHeading(keyword: String, page: SearchPage): String =
    if (keyword.isBlank()) "全部小说" else "搜索结果"

internal fun searchResultsCountLabel(page: SearchPage): String = page.total
    ?.let { "共 $it 部作品" }
    ?: "本页 ${page.items.size} 部作品"

internal fun searchPaginationWindow(page: SearchPage, maxVisiblePages: Int = 5): SearchPaginationWindow {
    require(maxVisiblePages > 0) { "maxVisiblePages must be positive" }
    val calculatedPages = page.total?.let { count ->
        ((count.toLong() + page.pageSize.coerceAtLeast(1) - 1L) / page.pageSize.coerceAtLeast(1)).toInt()
    }
    val totalPages = (page.totalPages ?: calculatedPages ?: page.page).coerceAtLeast(1)
    val currentPage = page.page.coerceIn(1, totalPages)
    val visibleCount = minOf(maxVisiblePages, totalPages)
    val maxStart = (totalPages - visibleCount + 1).coerceAtLeast(1)
    val start = (currentPage - visibleCount / 2).coerceIn(1, maxStart)
    return SearchPaginationWindow(
        currentPage = currentPage,
        totalPages = totalPages,
        pages = (start until start + visibleCount).toList(),
        previousPage = (currentPage - 1).takeIf { it >= 1 },
        nextPage = (currentPage + 1).takeIf { it <= totalPages }
    )
}

internal fun searchPageJumpTarget(value: String, totalPages: Int): Int? =
    value.trim().toIntOrNull()?.takeIf { it in 1..totalPages.coerceAtLeast(1) }

internal fun discoverFilterGroups(options: SearchOptions): List<DiscoverFilterGroup> = listOf(
    DiscoverFilterGroup(
        label = "排序方式",
        choices = discoverChoices(
            selected = options.sortBy,
            values = listOf(
                "relevance" to "相关度",
                "updated_at" to "更新时间",
                "created_at" to "上架时间",
                "favorite_count" to "收藏数",
                "site_read_count" to "本站阅读",
                "recommend" to "推荐",
                "source_read_count" to "源阅读",
                "word_count" to "字数",
                "source_favorite_count" to "源收藏"
            )
        )
    ),
    DiscoverFilterGroup(
        label = "排序方向",
        choices = discoverChoices(
            selected = options.sortOrder,
            values = listOf("desc" to "降序", "asc" to "升序")
        )
    ),
    DiscoverFilterGroup(
        label = "搜索范围",
        choices = discoverChoices(
            selected = options.scope,
            values = listOf("all" to "全部内容", "title" to "仅标题", "author" to "仅作者", "tags" to "仅标签")
        )
    ),
    DiscoverFilterGroup(
        label = "内容筛选",
        choices = discoverChoices(
            selected = options.adultFilter,
            values = listOf("all" to "所有", "adult_only" to "仅成人", "unrestricted" to "全年龄")
        )
    ),
    DiscoverFilterGroup(
        label = "字数",
        choices = discoverChoices(
            selected = options.wordCountRange,
            values = searchWordCountRangeChoices()
        )
    ),
    DiscoverFilterGroup(
        label = "来源",
        choices = discoverChoices(
            selected = options.source,
            values = listOf("" to "全部", "novelPia" to "NovelPia", "upload" to "上传")
        )
    ),
    DiscoverFilterGroup(
        label = "搜索模式",
        choices = discoverChoices(
            selected = options.matchType,
            values = listOf("ai" to "AI搜索", "fuzzy_strict" to "模糊-严格", "fuzzy_loose" to "模糊-宽松", "exact" to "精确匹配")
        )
    )
)

/** The source search keeps the content-rating selector in the Rules rail. */
internal fun sourcePrimarySearchFilterGroups(options: SearchOptions): List<DiscoverFilterGroup> {
    val groups = discoverFilterGroups(options)
    return listOf(
        groups[2], // Scope
        groups[3], // Content rating
        groups[5], // Source
        groups[6], // Match mode
        groups[0], // Sort field
        groups[1], // Sort direction
    )
}

internal fun discoverQuickPrompts(): List<String> =
    listOf("最近更新", "热门书评", "长篇连载", "完结作品")

internal fun discoverIdleMessage(): String =
    "输入关键词后搜索，也可以先看推荐方向。"

internal fun discoverTagLabels(tags: List<NovelTag>): List<String> =
    tags.map { tag ->
        tag.count?.let { "${tag.name} $it" } ?: tag.name
    }

internal fun discoverSectionOrder(
    results: LoadResult<*>,
    hasHistory: Boolean,
    advancedSyntaxEnabled: Boolean = false
): List<DiscoverSection> = buildList {
    add(DiscoverSection.SearchPanel)
    if (!advancedSyntaxEnabled) add(DiscoverSection.Filters)
    if (results != LoadResult.Idle) add(DiscoverSection.Results)
    if (hasHistory) add(DiscoverSection.History)
    if (results == LoadResult.Idle) add(DiscoverSection.IdlePrompts)
}

private fun discoverChoices(
    selected: String,
    values: List<Pair<String, String>>
): List<DiscoverFilterChoice> =
    values.map { (value, label) ->
        DiscoverFilterChoice(value = value, label = label, selected = selected == value)
    }
