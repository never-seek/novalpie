package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import java.text.NumberFormat
import java.util.Locale

/** Grid cards must wrap source metadata instead of hiding it behind an ellipsis. */
internal const val NOVEL_CARD_METADATA_MAX_LINES: Int = Int.MAX_VALUE

internal data class NovelSearchPreview(
    val authorLabel: String,
    val originalTitleLabel: String?,
    val platformLabel: String?,
    val facts: List<String>,
    val tags: List<String>
)

/** Source-search card overlay labels: a primary category on the cover and a short status badge. */
internal data class NovelCardCoverBadges(
    val category: String?,
    val status: String?
)

internal enum class NovelCardMetricKind {
    Favorite,
    Read,
    WordCount
}

/** A compact metric keeps the source card's density without dropping accessible meaning. */
internal data class NovelCardCompactMetric(
    val kind: NovelCardMetricKind,
    val contentDescription: String,
    val value: String
)

internal fun novelCardFacts(book: NovelCard): List<String> = buildList {
    book.status?.trim()?.takeIf { it.isNotBlank() }?.let { add("状态 $it") }
    book.wordCount?.let { add("字数 ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.favoriteCount?.let { add("收藏 ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.siteReadCount?.let { add("本站阅读 ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.sourceReadCount?.let { add("源阅读 ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.sourceFavoriteCount?.let { add("源收藏 ${NumberFormat.getIntegerInstance(Locale.US).format(it)}") }
    book.updatedAt?.trim()?.takeIf { it.isNotBlank() }?.let { add("更新 ${it.take(10)}") }
}

internal fun novelCardTags(book: NovelCard): List<String> =
    book.tags
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

internal fun novelOriginalTitleLabel(book: NovelCard): String? {
    val original = book.originalTitle?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return original.takeUnless { it == book.title.trim() }
}

internal fun novelPlatformLabel(platform: String?): String? {
    val normalized = platform?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return when {
        normalized.equals("novelPia", ignoreCase = true) -> "NovelPia"
        normalized.equals("upload", ignoreCase = true) -> "上传"
        else -> normalized
    }
}

internal fun novelDisplayCoverUrl(book: NovelCard): String? =
    book.fullCoverUrl?.trim()?.takeIf { it.isNotBlank() }
        ?: book.coverUrl?.trim()?.takeIf { it.isNotBlank() }

/**
 * Grid and list cells should reuse the source thumbnail when it is available. The original URL
 * remains reserved for the tap-to-preview flow, where the extra bytes are worthwhile.
 */
internal fun novelThumbnailCoverUrl(book: NovelCard): String? =
    book.coverUrl?.trim()?.takeIf { it.isNotBlank() }
        ?: book.fullCoverUrl?.trim()?.takeIf { it.isNotBlank() }

internal fun novelCardCoverBadges(book: NovelCard): NovelCardCoverBadges {
    val tags = novelCardTags(book)
    val category = tags.firstOrNull { sourceStatusLabel(it) == null } ?: novelPlatformLabel(book.platform)
    return NovelCardCoverBadges(
        category = category,
        status = novelStatusBadge(book.status) ?: tags.firstNotNullOfOrNull(::sourceStatusLabel)
    )
}

/** Status remains visible in the cover badge, so the content tag rail only carries actual topics. */
internal fun novelCardContentTags(book: NovelCard): List<String> =
    novelCardTags(book).filter { sourceStatusLabel(it) == null }

internal fun novelCardCompactMetrics(book: NovelCard): List<NovelCardCompactMetric> = buildList {
    val favorite = book.favoriteCount ?: book.sourceFavoriteCount
    val favoriteLabel = if (book.favoriteCount != null) "本站收藏" else "源收藏"
    favorite?.let {
        val value = formatNovelCardCompactCount(it)
        add(NovelCardCompactMetric(NovelCardMetricKind.Favorite, "$favoriteLabel $value", value))
    }

    val reads = book.siteReadCount ?: book.sourceReadCount
    val readsLabel = if (book.siteReadCount != null) "本站阅读" else "源阅读"
    reads?.let {
        val value = formatNovelCardCompactCount(it)
        add(NovelCardCompactMetric(NovelCardMetricKind.Read, "$readsLabel $value", value))
    }

    book.wordCount?.let {
        val value = formatNovelCardCompactCount(it)
        add(NovelCardCompactMetric(NovelCardMetricKind.WordCount, "字数 $value", value))
    }
}

/** Spoken summary for the card-level click target; cover preview remains a separate child action. */
internal fun novelCardAccessibilityLabel(book: NovelCard): String {
    val preview = novelSearchPreview(book)
    return buildList {
        add("打开 ${book.title.trim().ifBlank { "未命名作品" }}")
        add("作者 ${preview.authorLabel}")
        preview.platformLabel?.let { add("来源 $it") }
        novelStatusBadge(book.status)?.let { add("状态 $it") }
        novelCardContentTags(book).take(4).takeIf { it.isNotEmpty() }?.let { tags ->
            add("标签 ${tags.joinToString("、")}")
        }
        novelCardCompactMetrics(book).forEach { metric -> add(metric.contentDescription) }
    }.joinToString("，")
}

internal fun novelStatusBadge(status: String?): String? {
    val normalized = status?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return sourceStatusLabel(normalized) ?: normalized.take(12)
}

private fun sourceStatusLabel(value: String): String? =
    listOf("已完结", "完结", "连载中断", "连载中", "连载")
        .firstOrNull(value::contains)

/** The mobile source uses `w` for ten-thousands and `k` for thousands. */
internal fun formatNovelCardCompactCount(value: Long): String {
    val normalized = value.coerceAtLeast(0L)
    return when {
        normalized >= 10_000L -> "${formatCompactDecimal(normalized / 10_000.0)}w"
        normalized >= 1_000L -> "${formatCompactDecimal(normalized / 1_000.0)}k"
        else -> normalized.toString()
    }
}

private fun formatCompactDecimal(value: Double): String {
    val rounded = "%.1f".format(Locale.US, value)
    return rounded.removeSuffix(".0")
}

internal fun novelSearchPreview(book: NovelCard): NovelSearchPreview =
    NovelSearchPreview(
        authorLabel = book.author?.trim()?.takeIf { it.isNotBlank() } ?: "作者未知",
        originalTitleLabel = novelOriginalTitleLabel(book),
        platformLabel = novelPlatformLabel(book.platform),
        facts = novelCardFacts(book),
        tags = novelCardTags(book)
    )
