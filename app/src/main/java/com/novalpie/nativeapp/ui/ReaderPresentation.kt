package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChapterComment

@Suppress("UNUSED_PARAMETER")
internal fun readerSourceDebugLine(source: String): String? = null

@Suppress("UNUSED_PARAMETER")
internal fun readerDebugIdentityLine(bookId: Long, chapterId: Long): String? = null

internal fun readerToolbarLabels(): List<String> =
    listOf("上一章", "目录", "下一章", "A-", "A+", "主题", "网页")

internal fun readerCatalogPanelTitle(): String = "章节目录"

internal fun readerCloseCatalogLabel(): String = "回到正文"

internal fun readerSurfaceSections(): List<String> =
    listOf("正文", "目录", "设置")

internal data class ReaderTopBarLabels(
    val back: String,
    val title: String,
    val web: String
)

internal fun readerTopBarLabels(): ReaderTopBarLabels =
    ReaderTopBarLabels(back = "返回", title = "阅读", web = "网页")

internal fun globalProductTopBarVisible(route: AppRoute): Boolean =
    route !is AppRoute.Reader

internal fun chapterCommentMetricLabels(comment: ChapterComment): List<String> =
    listOf(
        "赞 ${comment.likeCount ?: 0}",
        "踩 ${comment.dislikeCount ?: 0}",
        "表情 ${comment.reactionCount ?: 0}",
        "打赏 ${comment.awardPoints ?: 0}",
        "回复"
    )

internal fun chapterCommentsSectionTitle(): String = "章节评论"

internal fun chapterCommentsFallbackLabel(): String = "打开网页评论"
