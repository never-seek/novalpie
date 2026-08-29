package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.FavoriteGroup
import com.novalpie.nativeapp.model.ReaderProgress

internal data class LibraryOverview(
    val title: String,
    val subtitle: String,
    val syncLabel: String,
    val stats: List<String>
)

internal fun libraryOverview(
    hasAuthToken: Boolean,
    favoriteCount: Int,
    groupCount: Int,
    recentCount: Int,
    pageCount: Int = 0
): LibraryOverview = LibraryOverview(
    title = "我的收藏",
    subtitle = "继续阅读、收藏分组和最近进度",
    syncLabel = if (hasAuthToken) "已同步" else "未同步",
    stats = listOf(
        "收藏 $favoriteCount",
        "分组 $groupCount",
        "最近 $recentCount",
        "总页数 ${pageCount.coerceAtLeast(0)}",
    )
)

/** A paged shelf must report the source total, never merely the currently rendered page size. */
internal fun collectionFavoriteCount(sourceTotal: Int?, loadedCount: Int): Int =
    sourceTotal?.coerceAtLeast(0) ?: loadedCount.coerceAtLeast(0)

internal fun libraryContinueTitle(hasProgress: Boolean): String =
    if (hasProgress) "继续阅读" else "阅读记录"

internal fun libraryContinueActions(): List<String> = listOf("继续阅读", "清除")

internal data class CompactFavoritesResumePresentation(
    val bookTitle: String,
    val chapterTitle: String,
)

/** The shelf title is freshest; persisted data covers a legacy or not-yet-loaded shelf. */
internal fun compactFavoritesResumePresentation(
    progress: ReaderProgress,
    libraryBookTitle: String?,
): CompactFavoritesResumePresentation = CompactFavoritesResumePresentation(
    bookTitle = libraryBookTitle?.trim()?.takeIf { it.isNotBlank() }
        ?: progress.bookTitle?.trim()?.takeIf { it.isNotBlank() }
        ?: "作品 #${progress.bookId}",
    chapterTitle = progress.chapterTitle?.trim()?.takeIf { it.isNotBlank() }
        ?: "章节 ${progress.chapterId}",
)

/** Legacy local progress had no book title; only request a detail after every local source fails. */
internal fun readerProgressNeedsBookTitleLookup(
    progress: ReaderProgress?,
    libraryBookTitle: String?,
): Boolean = progress != null &&
    progress.bookId > 0L &&
    progress.bookTitle.isNullOrBlank() &&
    libraryBookTitle.isNullOrBlank()

internal fun libraryFavoritesTitle(tab: FavoritesContentTab = FavoritesContentTab.Favorites): String =
    when (tab) {
        FavoritesContentTab.Favorites -> "收藏书籍"
        FavoritesContentTab.History -> "阅读历史"
    }

/**
 * The server only has one general favourites collection endpoint. Default and unclassified views
 * are therefore derived from its returned envelopes, while an explicitly opened group is already
 * scoped by its dedicated endpoint and must not be filtered back out locally.
 */
internal fun visibleFavoriteEntries(
    entries: List<FavoriteEntry>,
    query: String,
    displayMode: FavoritesDisplayMode,
    selectedGroupId: Long?
): List<FavoriteEntry> {
    val normalizedQuery = query.trim()
    return entries
        .asSequence()
        .filter { entry ->
            normalizedQuery.isBlank() ||
                bookMatchesQuery(entry.book, normalizedQuery) ||
                entry.groupName?.contains(normalizedQuery, ignoreCase = true) == true
        }
        .filter { entry ->
            when {
                selectedGroupId != null -> true
                displayMode == FavoritesDisplayMode.All -> true
                else -> entry.groupId == null || entry.groupId == 0L
            }
        }
        .toList()
}

internal fun shouldShowFavoriteGroupFolders(
    options: FavoritesUiOptions,
    selectedGroupId: Long?
): Boolean =
    options.tab == FavoritesContentTab.Favorites &&
        options.displayMode == FavoritesDisplayMode.Default &&
        selectedGroupId == null

/**
 * Empty folders remain available from the compact group filter and manager, but do not consume a
 * full shelf row. A missing count is kept visible because older source responses omit it.
 */
internal fun visibleFavoriteGroupFolders(groups: List<FavoriteGroup>): List<FavoriteGroup> =
    groups.filter { group ->
        group.count == null || group.count > 0 || group.previews.isNotEmpty()
    }

/**
 * The default source view can legitimately contain folder cards without any unclassified books.
 * Do not show a misleading "no match" state underneath those folders; only show it when a query
 * was entered or when there are no folder rows to explain the empty book area.
 */
internal fun shouldShowFavoriteNoMatchState(
    visibleEntries: List<FavoriteEntry>,
    visibleGroupFolders: List<FavoriteGroup>,
    query: String,
): Boolean = visibleEntries.isEmpty() &&
    (query.isNotBlank() || visibleGroupFolders.isEmpty())

/**
 * A stationary long press is the quick entry to the source-style bulk management controls.
 * The first long press starts selection and keeps the pressed book selected; later long presses
 * behave like a normal selection toggle.  This stays separate from the cover-preview gesture,
 * which is intentionally disabled on collection cards.
 */
internal data class FavoriteLongPressSelection(
    val selectionMode: Boolean,
    val selectedBookIds: Set<Long>,
)

internal fun favoriteLongPressSelection(
    selectionMode: Boolean,
    selectedBookIds: Set<Long>,
    bookId: Long,
): FavoriteLongPressSelection {
    if (bookId <= 0L) return FavoriteLongPressSelection(selectionMode, selectedBookIds)
    if (!selectionMode) return FavoriteLongPressSelection(true, setOf(bookId))

    val nextIds = selectedBookIds.toMutableSet()
    if (!nextIds.add(bookId)) nextIds.remove(bookId)
    return FavoriteLongPressSelection(true, nextIds)
}

/** A missing source count must not be presented as a false zero. */
internal fun favoriteGroupFolderSubtitle(group: FavoriteGroup): String =
    group.previews.firstOrNull()?.book?.title
        ?: group.count?.let { "$it 本收藏" }
        ?: "收藏分组"
