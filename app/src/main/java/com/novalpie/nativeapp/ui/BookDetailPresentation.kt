package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ChapterComment

internal fun bookDetailPrimaryActions(hasProgress: Boolean): List<String> =
    if (hasProgress) {
        listOf("继续阅读", "开始阅读", "网页详情")
    } else {
        listOf("开始阅读", "网页详情")
    }

internal fun bookDetailFavoriteLabel(isFavorited: Boolean): String =
    if (isFavorited) "已收藏" else "未收藏"

internal fun bookDetailFavoriteLoadingLabel(): String = "收藏同步中"

internal fun bookDetailFavoriteUnavailableLabel(): String = "收藏状态不可用"

internal fun bookCommentMetricLabels(comment: ChapterComment): List<String> =
    chapterCommentMetricLabels(comment)

internal fun bookCommentsSectionTitle(): String = "评论区"

internal fun bookCommentsFallbackLabel(): String = "打开网页评论"
