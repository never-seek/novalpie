package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter
import java.text.NumberFormat
import java.util.Locale

internal fun chapterMatchesQuery(chapter: Chapter, query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true

    return chapter.title.contains(normalized, ignoreCase = true) ||
        chapter.number?.toString()?.contains(normalized) == true ||
        chapter.wordCount?.let { wordCount ->
            wordCount.toString().contains(normalized) ||
                NumberFormat.getIntegerInstance(Locale.US).format(wordCount).contains(normalized)
        } == true ||
        chapter.updatedAt?.contains(normalized, ignoreCase = true) == true ||
        chapter.id.toString().contains(normalized)
}
