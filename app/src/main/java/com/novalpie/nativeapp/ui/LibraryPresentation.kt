package com.novalpie.nativeapp.ui

internal data class LibraryOverview(
    val title: String,
    val subtitle: String,
    val syncLabel: String,
    val stats: List<String>
)

internal fun libraryOverview(
    hasAuthToken: Boolean,
    favoriteCount: Int,
    groupCount: Int,
    recentCount: Int
): LibraryOverview = LibraryOverview(
    title = "书架",
    subtitle = "继续阅读、收藏分组和最近进度",
    syncLabel = if (hasAuthToken) "已同步" else "未同步",
    stats = listOf("收藏 $favoriteCount", "分组 $groupCount", "最近 $recentCount")
)

internal fun libraryContinueTitle(hasProgress: Boolean): String =
    if (hasProgress) "继续阅读" else "阅读记录"

internal fun libraryContinueActions(): List<String> = listOf("继续阅读", "清除")

internal fun libraryFavoritesTitle(): String = "收藏书籍"
