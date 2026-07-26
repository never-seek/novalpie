package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter

internal data class ReaderAdjacentChapters(
    val previous: Chapter?,
    val next: Chapter?
)

internal fun adjacentReaderChapters(
    currentChapterId: Long,
    chapters: List<Chapter>
): ReaderAdjacentChapters {
    val selectedIndex = chapters.indexOfFirst { it.id == currentChapterId }
    if (selectedIndex < 0) return ReaderAdjacentChapters(previous = null, next = null)

    return ReaderAdjacentChapters(
        previous = chapters.getOrNull(selectedIndex - 1),
        next = chapters.getOrNull(selectedIndex + 1)
    )
}
