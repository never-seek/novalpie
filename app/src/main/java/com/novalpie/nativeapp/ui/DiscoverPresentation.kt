package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelTag

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

internal enum class DiscoverSection {
    SearchPanel,
    Results,
    History,
    Tags,
    Filters,
    IdlePrompts
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

internal fun discoverFilterGroups(options: SearchOptions): List<DiscoverFilterGroup> = listOf(
    DiscoverFilterGroup(
        label = "排序",
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
        label = "顺序",
        choices = discoverChoices(
            selected = options.sortOrder,
            values = listOf("desc" to "降序", "asc" to "升序")
        )
    ),
    DiscoverFilterGroup(
        label = "范围",
        choices = discoverChoices(
            selected = options.scope,
            values = listOf("all" to "全部内容", "title" to "仅标题", "author" to "仅作者", "tags" to "仅标签")
        )
    ),
    DiscoverFilterGroup(
        label = "内容",
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
        label = "模式",
        choices = discoverChoices(
            selected = options.matchType,
            values = listOf("ai" to "AI搜索", "fuzzy_strict" to "模糊-严格", "fuzzy_loose" to "模糊-宽松", "exact" to "精确匹配")
        )
    )
)

internal fun discoverQuickPrompts(): List<String> =
    listOf("最近更新", "热门书评", "长篇连载", "完结作品")

internal fun discoverIdleMessage(): String =
    "输入关键词后搜索，也可以先看推荐方向。"

internal fun discoverTagLabels(tags: List<NovelTag>): List<String> =
    tags.map { tag ->
        tag.count?.let { "${tag.name} $it" } ?: tag.name
    }

internal fun discoverSectionOrder(results: LoadResult<*>, hasHistory: Boolean): List<DiscoverSection> = buildList {
    add(DiscoverSection.SearchPanel)
    if (results != LoadResult.Idle) add(DiscoverSection.Results)
    if (hasHistory) add(DiscoverSection.History)
    add(DiscoverSection.Tags)
    add(DiscoverSection.Filters)
    if (results == LoadResult.Idle) add(DiscoverSection.IdlePrompts)
}

private fun discoverChoices(
    selected: String,
    values: List<Pair<String, String>>
): List<DiscoverFilterChoice> =
    values.map { (value, label) ->
        DiscoverFilterChoice(value = value, label = label, selected = selected == value)
    }
