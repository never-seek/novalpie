package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.FavoritesCacheMode
import com.novalpie.nativeapp.model.FavoriteGroup
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.ReaderProgress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryPresentationTest {
    @Test
    fun libraryOverviewReadsLikeAReaderLibraryClient() {
        assertEquals(
            LibraryOverview(
                title = "我的收藏",
                subtitle = "继续阅读、收藏分组和最近进度",
                syncLabel = "已同步",
                stats = listOf("收藏 12", "分组 3", "最近 2", "总页数 0")
            ),
            libraryOverview(
                hasAuthToken = true,
                favoriteCount = 12,
                groupCount = 3,
                recentCount = 2
            )
        )
    }

    @Test
    fun libraryOverviewShowsUnsignedStateWithoutDebugLanguage() {
        val overview = libraryOverview(
            hasAuthToken = false,
            favoriteCount = 0,
            groupCount = 0,
            recentCount = 0
        )

        assertEquals("未同步", overview.syncLabel)
        listOf(overview.title, overview.subtitle, overview.syncLabel).plus(overview.stats).forEach { value ->
            assertFalse(value.contains("API", ignoreCase = true))
            assertFalse(value.contains("fallback", ignoreCase = true))
        }
    }

    @Test
    fun libraryShelfSectionTitlesStayCompact() {
        assertEquals("继续阅读", libraryContinueTitle(hasProgress = true))
        assertEquals("阅读记录", libraryContinueTitle(hasProgress = false))
        assertEquals(listOf("继续阅读", "清除"), libraryContinueActions())
        assertEquals("收藏书籍", libraryFavoritesTitle())
        assertEquals("阅读历史", libraryFavoritesTitle(FavoritesContentTab.History))
    }

    @Test
    fun collectionToolbarKeepsTheSourceMobileActionOrder() {
        assertEquals(
            listOf("列表", "分组", "选择", "缓存", "清除", "排序"),
            favoritesToolbarActionLabels(FavoritesLayout.Grid, selectionMode = false)
        )
        assertEquals(
            listOf("网格", "分组", "完成", "缓存", "清除", "排序"),
            favoritesToolbarActionLabels(FavoritesLayout.List, selectionMode = true)
        )
    }

    @Test
    fun defaultCollectionShowsOnlyUnclassifiedEntriesUntilAGroupIsOpened() {
        val unclassified = FavoriteEntry(book = NovelCard(id = 1, title = "Unclassified"))
        val grouped = FavoriteEntry(
            favoriteId = 2,
            book = NovelCard(id = 2, title = "Grouped"),
            groupId = 8,
            groupName = "Reading list"
        )

        assertEquals(
            listOf(unclassified),
            visibleFavoriteEntries(
                entries = listOf(unclassified, grouped),
                query = "",
                displayMode = FavoritesDisplayMode.Default,
                selectedGroupId = null
            )
        )
        assertEquals(
            listOf(unclassified, grouped),
            visibleFavoriteEntries(
                entries = listOf(unclassified, grouped),
                query = "",
                displayMode = FavoritesDisplayMode.All,
                selectedGroupId = null
            )
        )
        assertEquals(
            listOf(grouped),
            visibleFavoriteEntries(
                entries = listOf(grouped),
                query = "",
                displayMode = FavoritesDisplayMode.Default,
                selectedGroupId = 8
            )
        )
    }

    @Test
    fun groupFoldersAppearOnlyForTheDefaultUnscopedFavoritesView() {
        val options = FavoritesUiOptions()

        assertTrue(shouldShowFavoriteGroupFolders(options, selectedGroupId = null))
        assertFalse(shouldShowFavoriteGroupFolders(options, selectedGroupId = 1))
        assertFalse(
            shouldShowFavoriteGroupFolders(
                options.copy(displayMode = FavoritesDisplayMode.All),
                selectedGroupId = null
            )
        )
        assertFalse(
            shouldShowFavoriteGroupFolders(
                options.copy(tab = FavoritesContentTab.History),
                selectedGroupId = null
            )
        )
    }

    @Test
    fun emptyFolderCardsDoNotPushRealBooksOffTheShelf() {
        val populated = FavoriteGroup(id = 1, name = "Populated", count = 2)
        val empty = FavoriteGroup(id = 2, name = "Empty", count = 0)
        val unknownCount = FavoriteGroup(id = 3, name = "Unknown")

        assertEquals(
            listOf(populated, unknownCount),
            visibleFavoriteGroupFolders(listOf(populated, empty, unknownCount))
        )
    }

    @Test
    fun longPressStartsBulkSelectionWithoutUsingTheCollectionCoverPreview() {
        assertEquals(
            FavoriteLongPressSelection(selectionMode = true, selectedBookIds = setOf(8L)),
            favoriteLongPressSelection(
                selectionMode = false,
                selectedBookIds = emptySet(),
                bookId = 8L,
            ),
        )
        assertEquals(
            FavoriteLongPressSelection(selectionMode = true, selectedBookIds = setOf(2L, 8L)),
            favoriteLongPressSelection(
                selectionMode = true,
                selectedBookIds = setOf(2L),
                bookId = 8L,
            ),
        )
        assertEquals(
            FavoriteLongPressSelection(selectionMode = true, selectedBookIds = setOf(2L)),
            favoriteLongPressSelection(
                selectionMode = true,
                selectedBookIds = setOf(2L, 8L),
                bookId = 8L,
            ),
        )
    }

    @Test
    fun folderSubtitleNeverTurnsAnUnknownSourceCountIntoZero() {
        val preview = FavoriteEntry(book = NovelCard(id = 9, title = "Preview"))

        assertEquals("4 本收藏", favoriteGroupFolderSubtitle(FavoriteGroup(id = 1, name = "Count", count = 4)))
        assertEquals("Preview", favoriteGroupFolderSubtitle(FavoriteGroup(id = 2, name = "Preview", previews = listOf(preview))))
        assertEquals("收藏分组", favoriteGroupFolderSubtitle(FavoriteGroup(id = 3, name = "Unknown")))
    }

    @Test
    fun groupFolderUsesTheSourcePreviewBooksWithoutEnablingCoverPreview() {
        val first = FavoriteEntry(book = NovelCard(id = 1, title = "First"))
        val duplicate = FavoriteEntry(book = NovelCard(id = 1, title = "Duplicate"))
        val second = FavoriteEntry(book = NovelCard(id = 2, title = "Second"))
        val third = FavoriteEntry(book = NovelCard(id = 3, title = "Third"))
        val fourth = FavoriteEntry(book = NovelCard(id = 4, title = "Fourth"))

        assertEquals(
            listOf(first, second, third),
            favoriteGroupPreviewEntries(
                FavoriteGroup(
                    id = 1,
                    name = "Preview",
                    previews = listOf(first, duplicate, second, third, fourth),
                )
            ),
        )
    }

    @Test
    fun cacheToolbarSelectionTracksTheActualPersistedPolicy() {
        assertFalse(favoritesCacheActionIsSelected(FavoritesCacheMode.None))
        assertTrue(favoritesCacheActionIsSelected(FavoritesCacheMode.NoSearch))
        assertTrue(favoritesCacheActionIsSelected(FavoritesCacheMode.All))
    }

    @Test
    fun gridColumnPickerDoesNotPretendToChangeAListLayout() {
        assertTrue(
            favoritesGridColumnsPickerEnabled(
                layout = FavoritesLayout.Grid,
                actionLoading = false,
            ),
        )
        assertFalse(
            favoritesGridColumnsPickerEnabled(
                layout = FavoritesLayout.List,
                actionLoading = false,
            ),
        )
        assertFalse(
            favoritesGridColumnsPickerEnabled(
                layout = FavoritesLayout.Grid,
                actionLoading = true,
            ),
        )
    }

    @Test
    fun pinActionIsLimitedToUnselectedFavouriteRecords() {
        assertTrue(
            favoritePinActionEnabled(
                tab = FavoritesContentTab.Favorites,
                selecting = false,
                favoriteId = 12L,
            )
        )
        assertFalse(
            favoritePinActionEnabled(
                tab = FavoritesContentTab.History,
                selecting = false,
                favoriteId = 12L,
            )
        )
        assertFalse(
            favoritePinActionEnabled(
                tab = FavoritesContentTab.Favorites,
                selecting = true,
                favoriteId = 12L,
            )
        )
        assertFalse(
            favoritePinActionEnabled(
                tab = FavoritesContentTab.Favorites,
                selecting = false,
                favoriteId = null,
            )
        )
    }

    @Test
    fun defaultCollectionDoesNotShowNoMatchBelowNonEmptyGroupFolders() {
        val group = FavoriteGroup(id = 9, name = "待读", count = 3)

        assertFalse(
            shouldShowFavoriteNoMatchState(
                visibleEntries = emptyList(),
                visibleGroupFolders = listOf(group),
                query = "",
            )
        )
        assertTrue(
            shouldShowFavoriteNoMatchState(
                visibleEntries = emptyList(),
                visibleGroupFolders = listOf(group),
                query = "不存在",
            )
        )
        assertTrue(
            shouldShowFavoriteNoMatchState(
                visibleEntries = emptyList(),
                visibleGroupFolders = emptyList(),
                query = "",
            )
        )
    }

    @Test
    fun compactLibraryCardsKeepOnlyIdentityAndReadingProgress() {
        val unread = compactFavoriteBookCardPresentation(
            FavoriteEntry(
                book = NovelCard(id = 9, title = "Unread", author = "Author", tags = listOf("tag")),
                lastChapter = null,
                chapterCount = 130,
            ),
        )
        val updated = compactFavoriteBookCardPresentation(
            FavoriteEntry(
                book = NovelCard(id = 10, title = "Updated", author = "Author", tags = listOf("tag")),
                lastChapter = 130,
                chapterCount = 131,
            ),
        )
        val uploaded = compactUploadedBookCardPresentation(
            NovelCard(id = 11, title = "Uploaded", author = "Uploader", tags = listOf("ignored")),
        )

        assertEquals("Unread", unread.title)
        assertEquals("Author", unread.author)
        assertEquals("0/130", unread.progressLabel)
        assertEquals(null, unread.updateLabel)
        assertEquals("130/131", updated.progressLabel)
        assertEquals("更新 1 章", updated.updateLabel)
        assertEquals("Uploaded", uploaded.title)
        assertEquals("Uploader", uploaded.author)
        assertEquals(null, uploaded.progressLabel)
        assertEquals(null, uploaded.updateLabel)
    }

    @Test
    fun compactLibraryCardUsesNewerLocalChapterNumberForItsProgress() {
        val card = compactFavoriteBookCardPresentation(
            entry = FavoriteEntry(
                book = NovelCard(id = 9, title = "Reading", author = "Author"),
                lastChapter = null,
                chapterCount = 100,
            ),
            localProgress = ReaderProgress(
                bookId = 9,
                chapterId = 9005,
                chapterNumber = 5,
            ),
        )

        assertEquals("5/100", card.progressLabel)
        assertEquals(null, card.updateLabel)
    }

    @Test
    fun compactLibraryCardShowsAnUpdateAfterACompletedLocalBookGetsOneNewChapter() {
        val card = compactFavoriteBookCardPresentation(
            entry = FavoriteEntry(
                book = NovelCard(id = 10, title = "Updated", author = "Author"),
                lastChapter = null,
                chapterCount = 131,
            ),
            localProgress = ReaderProgress(
                bookId = 10,
                chapterId = 9130,
                chapterNumber = 130,
            ),
        )

        assertEquals("130/131", card.progressLabel)
        assertEquals("更新 1 章", card.updateLabel)
    }

    @Test
    fun localReaderProgressOnlyUpdatesTheMatchingFavoriteEntry() {
        val entries = listOf(
            FavoriteEntry(book = NovelCard(id = 9, title = "Reading"), chapterCount = 100),
            FavoriteEntry(book = NovelCard(id = 10, title = "Unread"), chapterCount = 200),
        )

        val updated = favoriteEntriesWithLocalReaderProgress(
            entries = entries,
            localProgresses = listOf(
                ReaderProgress(bookId = 9, chapterId = 9005, chapterNumber = 5),
            ),
        )

        assertEquals(5, updated[0].lastChapter)
        assertEquals(null, updated[1].lastChapter)
    }

    @Test
    fun collectionRefreshRequirementTracksOnlyUnsyncedReaderProgress() {
        assertFalse(collectionRefreshRequired(readerProgressRevision = 0, syncedProgressRevision = 0))
        assertTrue(collectionRefreshRequired(readerProgressRevision = 1, syncedProgressRevision = 0))
        assertFalse(collectionRefreshRequired(readerProgressRevision = 1, syncedProgressRevision = 1))
        assertTrue(collectionRefreshRequired(readerProgressRevision = 2, syncedProgressRevision = 1))
    }

    @Test
    fun compactResumeUsesBookIdentityBeforeChapterIdentityAndKeepsLegacyRecordsReadable() {
        val resolved = compactFavoritesResumePresentation(
            progress = ReaderProgress(
                bookId = 9,
                chapterId = 90,
                chapterTitle = "第九十章",
                bookTitle = "已保存书名",
            ),
            libraryBookTitle = "书架当前书名",
        )
        val legacy = compactFavoritesResumePresentation(
            progress = ReaderProgress(bookId = 10, chapterId = 100),
            libraryBookTitle = null,
        )

        assertEquals("书架当前书名", resolved.bookTitle)
        assertEquals("第九十章", resolved.chapterTitle)
        assertEquals("作品 #10", legacy.bookTitle)
        assertEquals("章节 100", legacy.chapterTitle)
    }

    @Test
    fun legacyResumeProgressRequestsOneTitleLookupOnlyWhenNeitherLocalSourceCanNameTheBook() {
        val missing = ReaderProgress(bookId = 10, chapterId = 100)
        val persisted = missing.copy(bookTitle = "已保存书名")

        assertTrue(readerProgressNeedsBookTitleLookup(missing, libraryBookTitle = null))
        assertFalse(readerProgressNeedsBookTitleLookup(missing, libraryBookTitle = "书架书名"))
        assertFalse(readerProgressNeedsBookTitleLookup(persisted, libraryBookTitle = null))
        assertFalse(readerProgressNeedsBookTitleLookup(null, libraryBookTitle = null))
    }

    @Test
    fun compactLibraryCardsReserveTheSameTitleAuthorAndProgressSlotsInEveryGridCell() {
        assertEquals(
            CompactLibraryBookCardTextSlots(
                titleLines = 2,
                authorLines = 1,
                progressMinHeightDp = 16,
            ),
            compactLibraryBookCardTextSlots(),
        )
    }

    @Test
    fun progressSlotIsReservedForCollectionCardsWithoutAddingBlankSpaceToUploadedBooks() {
        val withoutProgress = CompactLibraryBookCardPresentation(title = "Title", author = "Author")

        assertTrue(compactLibraryCardReservesProgressSlot(withoutProgress, collectionCard = true))
        assertFalse(compactLibraryCardReservesProgressSlot(withoutProgress, collectionCard = false))
        assertTrue(
            compactLibraryCardReservesProgressSlot(
                withoutProgress.copy(progressLabel = "1/10"),
                collectionCard = false,
            )
        )
    }
}
