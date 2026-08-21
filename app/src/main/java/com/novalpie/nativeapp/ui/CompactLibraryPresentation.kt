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

internal fun compactFavoriteBookCardPresentation(
    entry: FavoriteEntry,
    localProgress: ReaderProgress? = null,
): CompactLibraryBookCardPresentation {
    val effectiveEntry = favoriteEntryWithLocalReaderProgress(entry, localProgress)
    val total = (effectiveEntry.chapterCount ?: effectiveEntry.book.chapterCount)?.coerceAtLeast(0)
    val read = effectiveEntry.lastChapter?.coerceAtLeast(0)
    val visibleRead = total?.let { (read ?: 0).coerceAtMost(it) }
    return CompactLibraryBookCardPresentation(
        title = effectiveEntry.book.title,
        author = effectiveEntry.book.author?.trim().takeUnless { it.isNullOrBlank() } ?: "未知作者",
        progressLabel = total?.let { "$visibleRead/$it" },
        // Without a source-side historical total, only the exact completed-then-one-new-chapter
        // shape can be stated as an update rather than incorrectly flagging every paused book.
        updateLabel = total
            ?.takeIf { visibleRead != null && visibleRead > 0 && visibleRead == it - 1 }
            ?.let { "更新 1 章" },
    )
}

internal fun compactUploadedBookCardPresentation(
    book: NovelCard,
): CompactLibraryBookCardPresentation = CompactLibraryBookCardPresentation(
    title = book.title,
    author = book.author?.trim().takeUnless { it.isNullOrBlank() } ?: "未知作者",
)
