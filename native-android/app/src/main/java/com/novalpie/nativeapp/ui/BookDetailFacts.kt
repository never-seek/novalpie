package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import java.text.NumberFormat
import java.util.Locale

internal data class BookDetailStatistic(
    val label: String,
    val value: String,
)

/**
 * Metadata belongs beside the source description. Counters are presented separately in the
 * source-style statistics rail so the same values are not repeated as a wall of chips.
 */
internal fun bookDetailFacts(book: NovelCard): List<String> = buildList {
    book.status?.trim()?.takeIf { it.isNotBlank() }?.let { add("状态: $it") }
    book.author?.trim()?.takeIf { it.isNotBlank() }?.let { add("作者: $it") }
    novelPlatformLabel(book.platform)?.let { add("来源: $it") }
    book.wordCount?.let { add("字数: ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.updatedAt?.trim()?.takeIf { it.isNotBlank() }?.let { add("更新: $it") }
    book.createdAt?.trim()?.takeIf { it.isNotBlank() }?.let { add("上架: $it") }
    book.guarantorName?.trim()?.takeIf { it.isNotBlank() }?.let { guarantor ->
        val guaranteedAt = book.guaranteedAt?.trim()?.takeIf { it.isNotBlank() }
        add(if (guaranteedAt == null) "担保人: $guarantor" else "担保人: $guarantor ($guaranteedAt)")
    }
    book.uploaderName?.trim()?.takeIf { it.isNotBlank() }?.let { add("上传者: $it") }
    if (book.isAdult == true) add("成人内容")
    when (book.allowDownload) {
        true -> add("允许下载")
        false -> add("禁止下载")
        null -> Unit
    }
}

/**
 * Counters are glance-only metadata, not primary detail content. Keep the local and upstream
 * values in two terse rows so they do not displace the synopsis, catalogue, or reviews.
 */
internal fun bookDetailStatistics(book: NovelCard): List<BookDetailStatistic> = buildList {
    buildStatisticSummary(
        "本站",
        listOf(
            "推荐" to book.recommendCount,
            "阅读" to book.siteReadCount,
            "收藏" to book.favoriteCount,
        ),
    )?.let(::add)
    buildStatisticSummary(
        "源站",
        listOf(
            "阅读" to book.sourceReadCount,
            "收藏" to book.sourceFavoriteCount,
        ),
    )?.let(::add)
}

private fun buildStatisticSummary(
    label: String,
    values: List<Pair<String, Long?>>,
): BookDetailStatistic? = values
    .mapNotNull { (name, value) -> value?.let { "$name ${formatBookDetailStatCount(it)}" } }
    .takeIf(List<String>::isNotEmpty)
    ?.joinToString(" · ")
    ?.let { BookDetailStatistic(label, it) }

private fun formatBookDetailStatCount(value: Long): String =
    formatNovelCardCompactCount(value.coerceAtLeast(0L))
