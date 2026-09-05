package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.ReaderProgress

/** The compact shelf deliberately omits tags, platform and counters to keep cover grids readable. */
internal data class CompactLibraryBookCardPresentation(
    val title: String,
    val author: String,
    val progressLabel: String? = null,
    val updateLabel: String? = null,
)

/** Fixed text slots keep every card's author and progress baseline aligned within a shelf row. */
internal data class CompactLibraryBookCardTextSlots(
    val titleLines: Int = 2,
    val authorLines: Int = 1,
    val progressMinHeightDp: Int = 16,
)

internal fun compactLibraryBookCardTextSlots(): CompactLibraryBookCardTextSlots =
    CompactLibraryBookCardTextSlots()

/** Collection rows need aligned unread progress space; upload management deliberately does not. */
internal fun compactLibraryCardReservesProgressSlot(
    presentation: CompactLibraryBookCardPresentation,
    collectionCard: Boolean,
): Boolean = collectionCard || presentation.progressLabel != null

/** A reader exit needs one source refresh only when newer local progress exists. */
internal fun collectionRefreshRequired(
    readerProgressRevision: Long,
    syncedProgressRevision: Long,
): Boolean = readerProgressRevision > syncedProgressRevision

/** Local reader state is newer than a returned shelf page, but never rolls server progress back. */
internal fun favoriteEntryWithLocalReaderProgress(
    entry: FavoriteEntry,
    localProgress: ReaderProgress?,
): FavoriteEntry {
    if (localProgress?.bookId != entry.book.id) return entry
    val localChapterNumber = localProgress.chapterNumber?.takeIf { it > 0 } ?: return entry
    val sourceChapterNumber = entry.lastChapter?.coerceAtLeast(0) ?: 0
    return entry.copy(lastChapter = maxOf(sourceChapterNumber, localChapterNumber))
}

/** Applies persisted reader progress without mutating the source-derived favourite records. */
internal fun favoriteEntriesWithLocalReaderProgress(
    entries: List<FavoriteEntry>,
    localProgresses: List<ReaderProgress>,
): List<FavoriteEntry> {
    if (entries.isEmpty() || localProgresses.isEmpty()) return entries
    val localProgressByBookId = localProgresses.associateBy(ReaderProgress::bookId)
    return entries.map { entry ->
        favoriteEntryWithLocalReaderProgress(entry, localProgressByBookId[entry.book.id])
    }
}

/**
 * The website persists only a chapter, while the native reader also owns a paragraph anchor.
 * Adopt source progress only when it is strictly farther ahead; equal or older source data must
 * never erase a more precise native resume point.
 */
internal fun readerProgressAfterRemoteFavoriteProgress(
    localProgress: ReaderProgress?,
    entry: FavoriteEntry,
): ReaderProgress? {
    if (localProgress != null && localProgress.bookId != entry.book.id) return localProgress
    val remoteChapterId = entry.lastChapterId?.takeIf { it > 0L } ?: return localProgress
    val remoteChapterNumber = entry.lastChapter?.takeIf { it > 0 } ?: return localProgress
    val localChapterNumber = localProgress?.chapterNumber?.takeIf { it > 0 }
    if (localChapterNumber != null && remoteChapterNumber <= localChapterNumber) return localProgress

    val sourceChapterCount = (entry.chapterCount ?: entry.book.chapterCount)?.takeIf { it > 0 }
    val remoteCompletedCurrentCatalogue = sourceChapterCount != null && remoteChapterNumber >= sourceChapterCount
    val sameChapter = localProgress?.chapterId == remoteChapterId
    return ReaderProgress(
        bookId = entry.book.id,
        chapterId = remoteChapterId,
        chapterTitle = localProgress?.chapterTitle?.takeIf { sameChapter },
        updatedAtMillis = localProgress?.updatedAtMillis ?: 0L,
        bookTitle = entry.book.title.trim().takeIf { it.isNotBlank() } ?: localProgress?.bookTitle,
        chapterNumber = remoteChapterNumber,
        chapterCountAtLastRead = if (remoteCompletedCurrentCatalogue) {
            sourceChapterCount
        } else {
            localProgress?.chapterCountAtLastRead
        },
        viewportItemIndex = localProgress?.viewportItemIndex?.takeIf { sameChapter },
        viewportItemScrollOffsetPx = localProgress?.viewportItemScrollOffsetPx?.takeIf { sameChapter },
    )
}

internal fun compactFavoriteBookCardPresentation(
    entry: FavoriteEntry,
    localProgress: ReaderProgress? = null,
): CompactLibraryBookCardPresentation {
    val effectiveProgress = readerProgressAfterRemoteFavoriteProgress(localProgress, entry) ?: localProgress
    val effectiveEntry = favoriteEntryWithLocalReaderProgress(entry, effectiveProgress)
    val total = (effectiveEntry.chapterCount ?: effectiveEntry.book.chapterCount)?.coerceAtLeast(0)
    val read = effectiveEntry.lastChapter?.coerceAtLeast(0)
    val visibleRead = total?.let { (read ?: 0).coerceAtMost(it) }
    return CompactLibraryBookCardPresentation(
        title = effectiveEntry.book.title,
        author = effectiveEntry.book.author?.trim().takeUnless { it.isNullOrBlank() } ?: "未知作者",
        progressLabel = total?.let { "$visibleRead/$it" },
        updateLabel = favoriteBookUpdateLabel(entry, effectiveProgress),
    )
}

/**
 * A `read == total - 1` shelf response is ambiguous: it can be an unfinished book or a newly
 * added chapter. Only a locally persisted completed catalogue gives the app enough evidence to
 * distinguish those two cases without inventing a notification.
 */
internal fun favoriteBookUpdateLabel(
    entry: FavoriteEntry,
    localProgress: ReaderProgress?,
): String? {
    if (localProgress?.bookId != entry.book.id) return null
    val currentTotal = (entry.chapterCount ?: entry.book.chapterCount)?.takeIf { it > 0 } ?: return null
    val completedCatalogueSize = localProgress.chapterCountAtLastRead?.takeIf { it > 0 } ?: return null
    val localChapterNumber = localProgress.chapterNumber?.takeIf { it > 0 } ?: return null
    if (localChapterNumber < completedCatalogueSize || currentTotal <= completedCatalogueSize) return null
    return "更新 ${currentTotal - completedCatalogueSize} 章"
}

internal fun compactUploadedBookCardPresentation(
    book: NovelCard,
): CompactLibraryBookCardPresentation = CompactLibraryBookCardPresentation(
    title = book.title,
    author = book.author?.trim().takeUnless { it.isNullOrBlank() } ?: "未知作者",
)
