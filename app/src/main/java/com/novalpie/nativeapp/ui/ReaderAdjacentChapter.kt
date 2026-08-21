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

/**
 * Finds the first chapter after the contiguous portion already present in the scroll window.
 * Keeping this pure makes the infinite-scroll trigger deterministic and prevents repeated loads
 * of the same chapter when the list is remeasured near its end.
 */
internal fun nextReaderChapterForInfiniteScroll(
    currentChapterId: Long,
    chapters: List<Chapter>,
    loadedChapterIds: Set<Long>,
): Chapter? {
    val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }
    if (currentIndex < 0) return null
    var lastLoadedIndex = currentIndex
    while (lastLoadedIndex + 1 < chapters.size && chapters[lastLoadedIndex + 1].id in loadedChapterIds) {
        lastLoadedIndex += 1
    }
    return chapters.getOrNull(lastLoadedIndex + 1)?.takeUnless { it.id in loadedChapterIds }
}

/**
 * A successful but empty/partial catalog cannot prove that the current chapter is the final one.
 * Keeping this check separate prevents the reader from converting a transient catalog response
 * into a permanent end-of-book state.
 */
internal fun readerCatalogIsIncomplete(
    currentChapterId: Long,
    chapters: List<Chapter>,
): Boolean = chapters.isEmpty() || chapters.none { it.id == currentChapterId }
