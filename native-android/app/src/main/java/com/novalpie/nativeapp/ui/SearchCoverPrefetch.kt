package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard

/** Search starts with two responsive grid rows warm, bounded to avoid a full-page bandwidth burst. */
internal const val SEARCH_INITIAL_COVER_PRELOAD_ROWS = 2

/** One bounded responsive look-ahead batch protects covers after the user starts scrolling. */
internal const val SEARCH_SCROLL_COVER_PRELOAD_ROWS = 2

/** Six-column landscape must warm its whole first row without allowing unbounded fetch bursts. */
internal const val SEARCH_COVER_PRELOAD_MAX_COUNT = 8

internal fun searchCoverPreloadCount(
    columnCount: Int,
    rowCount: Int,
): Int = (columnCount.coerceAtLeast(1) * rowCount.coerceAtLeast(1))
    .coerceAtMost(SEARCH_COVER_PRELOAD_MAX_COUNT)

/**
 * Starts the visible search-result covers as soon as the API response arrives. Search controls
 * occupy the first part of the screen, so waiting until cards compose makes every cold search
 * look slower than the source website even when the CDN itself is fast. This uses the thumbnail
 * URL that [BookCover] uses, keeping the Coil memory-cache key shared with the actual card.
 */
internal fun searchInitialCoverPreloadUrls(
    books: List<NovelCard>,
    columnCount: Int = 2,
    preloadCount: Int = searchCoverPreloadCount(
        columnCount = columnCount,
        rowCount = SEARCH_INITIAL_COVER_PRELOAD_ROWS,
    ),
): List<String> {
    if (preloadCount <= 0) return emptyList()

    return books.asSequence()
        .mapNotNull(::novelThumbnailCoverUrl)
        .distinct()
        .take(preloadCount)
        .toList()
}

/**
 * Continues warming the next screenful after a user scrolls past the initial preload window.
 * The grid supplies only actual book ids, so header, filter and pagination cells never affect
 * where the look-ahead window begins.
 */
internal fun searchCoverPreloadUrlsAfterVisible(
    books: List<NovelCard>,
    visibleBookIds: Collection<Long>,
    columnCount: Int = 2,
    preloadCount: Int = searchCoverPreloadCount(
        columnCount = columnCount,
        rowCount = SEARCH_SCROLL_COVER_PRELOAD_ROWS,
    ),
    allowSpeculativePreload: Boolean = true
): List<String> {
    if (!allowSpeculativePreload || preloadCount <= 0 || visibleBookIds.isEmpty()) return emptyList()
    val visibleIds = visibleBookIds.toHashSet()
    val lastVisibleIndex = books.indexOfLast { book -> book.id in visibleIds }
    if (lastVisibleIndex < 0) return emptyList()

    return books.asSequence()
        .drop(lastVisibleIndex + 1)
        .mapNotNull(::novelThumbnailCoverUrl)
        .distinct()
        .take(preloadCount)
        .toList()
}
