package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import java.text.NumberFormat
import java.util.Locale

internal data class NovelSearchPreview(
    val authorLabel: String,
    val originalTitleLabel: String?,
    val platformLabel: String?,
    val facts: List<String>,
    val tags: List<String>
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

internal fun novelSearchPreview(book: NovelCard): NovelSearchPreview =
    NovelSearchPreview(
        authorLabel = book.author?.trim()?.takeIf { it.isNotBlank() } ?: "作者未知",
        originalTitleLabel = novelOriginalTitleLabel(book),
        platformLabel = novelPlatformLabel(book.platform),
        facts = novelCardFacts(book),
        tags = novelCardTags(book)
    )
