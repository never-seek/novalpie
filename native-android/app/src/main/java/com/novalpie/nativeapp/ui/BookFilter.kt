package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import java.text.NumberFormat
import java.util.Locale

internal fun bookMatchesQuery(book: NovelCard, query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true

    return book.title.contains(normalized, ignoreCase = true) ||
        book.author?.contains(normalized, ignoreCase = true) == true ||
        book.status?.contains(normalized, ignoreCase = true) == true ||
        book.wordCount?.let { wordCount ->
            wordCount.toString().contains(normalized) ||
                NumberFormat.getIntegerInstance(Locale.US).format(wordCount).contains(normalized)
        } == true ||
        book.updatedAt?.contains(normalized, ignoreCase = true) == true ||
        book.tags.any { it.contains(normalized, ignoreCase = true) } ||
        book.id.toString().contains(normalized)
}
