package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import java.text.NumberFormat
import java.util.Locale

internal fun bookDetailFacts(book: NovelCard): List<String> = buildList {
    book.status?.trim()?.takeIf { it.isNotBlank() }?.let { add("状态: $it") }
    book.author?.trim()?.takeIf { it.isNotBlank() }?.let { add("作者: $it") }
    novelPlatformLabel(book.platform)?.let { add("来源: $it") }
    book.wordCount?.let { add("字数: ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.favoriteCount?.let { add("收藏: ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.siteReadCount?.let { add("本站阅读: ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.sourceReadCount?.let { add("源阅读: ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.sourceFavoriteCount?.let { add("源收藏: ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.updatedAt?.trim()?.takeIf { it.isNotBlank() }?.let { add("更新: $it") }
}
