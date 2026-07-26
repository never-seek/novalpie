package com.novalpie.nativeapp.ui

internal enum class ProductSurface {
    Library,
    Discover,
    Profile
}

internal data class ProductHeader(
    val title: String,
    val subtitle: String
)

internal data class ForumCardCopy(
    val title: String,
    val subtitle: String,
    val meta: String
)

internal data class ForumFeedItem(
    val id: Long = 0,
    val category: String,
    val title: String,
    val bookTitle: String,
    val authorName: String,
    val replyCount: Int,
    val likeCount: Int = 0,
    val reactionCount: Int = 0,
    val awardPoints: Int = 0,
    val viewCount: Int = 0,
    val lastActiveLabel: String,
    val tags: List<String>,
    val pinned: Boolean = false,
    val featured: Boolean = false,
    val authorId: Long? = null
)

internal fun productHeader(surface: ProductSurface): ProductHeader = when (surface) {
    ProductSurface.Library -> ProductHeader("书架", "收藏、分组和阅读进度")
    ProductSurface.Discover -> ProductHeader("发现", "搜索作品、作者和标签")
    ProductSurface.Profile -> ProductHeader("我的", "账号、阅读偏好和连接设置")
}

internal fun accountSyncSummary(hasAuthToken: Boolean): String =
    if (hasAuthToken) "登录同步: 已连接" else "登录同步: 未同步"

internal fun libraryPrimaryActions(): List<String> =
    listOf("同步书架", "登录同步", "网页收藏")

internal fun discoverPrimaryActions(): List<String> =
    listOf("搜索", "网页发现")

internal fun discoverFilterLabels(): List<String> =
    listOf("排序", "顺序", "范围", "内容", "字数", "来源", "模式")

internal fun discoverSelectedFilterSummaries(options: SearchOptions): List<String> {
    val sortBy = mapOf(
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
    val sortOrder = mapOf("desc" to "降序", "asc" to "升序")
    val scope = mapOf("all" to "全部内容", "title" to "仅标题", "author" to "仅作者", "tags" to "仅标签")
    val matchType = mapOf(
        "ai" to "AI搜索",
        "fuzzy_strict" to "模糊-严格",
        "fuzzy_loose" to "模糊-宽松",
        "exact" to "精确匹配"
    )
    val adultFilter = mapOf("all" to "所有", "adult_only" to "仅成人", "unrestricted" to "全年龄")
    val wordCount = searchWordCountRangeChoices().toMap()
    val source = mapOf("" to "全部", "novelPia" to "NovelPia", "upload" to "上传")
    return listOf(
        "排序: ${sortBy[options.sortBy] ?: options.sortBy}",
        "顺序: ${sortOrder[options.sortOrder] ?: options.sortOrder}",
        "范围: ${scope[options.scope] ?: options.scope}",
        "内容: ${adultFilter[options.adultFilter] ?: options.adultFilter}",
        "字数: ${wordCount[options.wordCountRange] ?: options.wordCountRange}",
        "来源: ${source[options.source] ?: options.source}",
        "模式: ${matchType[options.matchType] ?: options.matchType}"
    )
}

internal fun searchWordCountRangeChoices(): List<Pair<String, String>> =
    listOf(
        "" to "不限",
        "..100000" to "10万以下",
        "100000..500000" to "10-50万",
        "500000..1000000" to "50-100万",
        "1000000.." to "100万以上"
    )

internal fun searchMinWordCount(range: String): Long? =
    range.substringBefore("..").takeIf { it.isNotBlank() }?.toLongOrNull()

internal fun searchMaxWordCount(range: String): Long? =
    range.substringAfter("..", "").takeIf { it.isNotBlank() }?.toLongOrNull()

internal fun bookDetailSectionTitles(): List<String> =
    listOf("作品", "阅读", "章节目录", "评论区")

internal fun readerScreenTitle(): String = "阅读"

internal fun readerCatalogTitle(): String = "章节"

internal fun forumHeader(): ProductHeader =
    ProductHeader("论坛", "小说讨论、书评和站内动态")

internal fun forumPrimaryActions(hasAuthToken: Boolean): List<String> =
    listOf("同步", if (hasAuthToken) "已登录" else "登录", "网页论坛")

internal fun forumCardCopies(): List<ForumCardCopy> = listOf(
    ForumCardCopy(
        title = "最新讨论",
        subtitle = "追踪书评、章节讨论和作品动态",
        meta = "站内动态"
    ),
    ForumCardCopy(
        title = "热门书评",
        subtitle = "查看近期被回复、收藏和引用的评论",
        meta = "书友反馈"
    ),
    ForumCardCopy(
        title = "关联书籍",
        subtitle = "讨论行展示作品、标签、回复数和最后活跃时间",
        meta = "作品索引"
    )
)

internal fun forumFeedTabs(): List<String> =
    listOf("全部", "书评", "章节", "动态")

internal fun forumFeedItems(): List<ForumFeedItem> = listOf(
    ForumFeedItem(
        category = "书评",
        title = "角色弧光讨论",
        bookTitle = "热门作品",
        authorName = "北港读者",
        replyCount = 42,
        likeCount = 81,
        reactionCount = 12,
        awardPoints = 7,
        viewCount = 7305,
        lastActiveLabel = "刚刚",
        tags = listOf("热议", "长评"),
        pinned = true,
        featured = true
    ),
    ForumFeedItem(
        category = "章节",
        title = "最新章节伏笔整理",
        bookTitle = "连载专区",
        authorName = "栗子校对",
        replyCount = 28,
        likeCount = 34,
        reactionCount = 6,
        awardPoints = 2,
        viewCount = 1904,
        lastActiveLabel = "8分钟前",
        tags = listOf("剧情", "伏笔")
    ),
    ForumFeedItem(
        category = "动态",
        title = "作者更新说明",
        bookTitle = "站内公告",
        authorName = "运营记录",
        replyCount = 17,
        likeCount = 22,
        reactionCount = 4,
        awardPoints = 1,
        viewCount = 860,
        lastActiveLabel = "23分钟前",
        tags = listOf("公告")
    ),
    ForumFeedItem(
        category = "书评",
        title = "结局走向猜测",
        bookTitle = "长篇讨论",
        authorName = "雾灯",
        replyCount = 64,
        likeCount = 73,
        reactionCount = 9,
        awardPoints = 5,
        viewCount = 4201,
        lastActiveLabel = "1小时前",
        tags = listOf("推理", "讨论")
    ),
    ForumFeedItem(
        category = "章节",
        title = "翻译名词校对",
        bookTitle = "协作校对",
        authorName = "灰页",
        replyCount = 11,
        likeCount = 19,
        reactionCount = 3,
        awardPoints = 0,
        viewCount = 640,
        lastActiveLabel = "2小时前",
        tags = listOf("校对", "术语")
    ),
    ForumFeedItem(
        category = "动态",
        title = "收藏榜单变化",
        bookTitle = "作品榜",
        authorName = "榜单观察",
        replyCount = 9,
        likeCount = 15,
        reactionCount = 2,
        awardPoints = 0,
        viewCount = 512,
        lastActiveLabel = "今天",
        tags = listOf("榜单")
    )
)
