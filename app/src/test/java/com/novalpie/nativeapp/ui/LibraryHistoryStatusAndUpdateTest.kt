package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.*
import org.junit.Assert.*
import org.junit.Test

class LibraryHistoryStatusAndUpdateTest {
    private fun history(result: LoadResult<List<FavoriteEntry>>) = HomeState(
        options = FavoritesUiOptions(tab = FavoritesContentTab.History),
        history = result, historyTotal = 53, historyTotalPages = 3,
    )
    @Test fun aLoadedHistoryIsSyncedWithoutHavingToLoadFavoritesFirst() {
        val overview = libraryOverviewForState(true, history(LoadResult.Success(emptyList())))
        assertEquals("已同步",overview.syncLabel)
        assertEquals("阅读历史",overview.title)
        assertTrue(overview.stats.contains("总页数 3"))
        assertTrue(overview.stats.contains("记录 53"))
        assertFalse(overview.stats.contains("收藏 —"))
    }
    @Test fun historyFailureCannotBorrowTheInactiveFavoritesSuccess() {
        val state = history(LoadResult.Error("断网")).copy(favoriteTotal = 67, favoriteTotalPages = 4,
            favoriteEntries = LoadResult.Success(emptyList()))
        val overview = libraryOverviewForState(true,state)
        assertEquals("同步失败",overview.syncLabel)
        assertTrue(overview.stats.contains("总页数 3"))
    }
    @Test fun historyLoadingAndLoggedOutAreNotPresentedAsSynced() {
        assertEquals("同步中",libraryOverviewForState(true,history(LoadResult.Loading)).syncLabel)
        assertEquals("未同步",libraryOverviewForState(false,history(LoadResult.Success(emptyList()))).syncLabel)
    }
    private fun entry(read: Int, chapterId: Long? = null) = FavoriteEntry(
        book = NovelCard(9,"截图复现"), lastChapter = read, lastChapterId = chapterId, chapterCount = 147,
    )
    private fun completed() = ReaderProgress(9,9104,chapterNumber = 104,chapterCountAtLastRead = 104)
    @Test fun remotePartialReadCountsOnlyTheRemainingUpdatesNotFortyThree() {
        val card = compactFavoriteBookCardPresentation(entry(141,9141),completed())
        assertEquals("141/147",card.progressLabel)
        assertEquals("更新 6 章",card.updateLabel)
    }
    @Test fun aRemoteReadWithoutAChapterIdStillUsesTheSameVisibleProgressForTheBadge() {
        val card = compactFavoriteBookCardPresentation(entry(141),completed())
        assertEquals("141/147",card.progressLabel)
        assertEquals("更新 6 章",card.updateLabel)
    }
    @Test fun updateBadgeShrinksAndDisappearsAsOtherDevicesReadTheNewChapters() {
        val local = ReaderProgress(9,9141,chapterNumber = 141,chapterCountAtLastRead = 104)
        assertEquals("更新 2 章",compactFavoriteBookCardPresentation(entry(145,9145),local).updateLabel)
        assertNull(compactFavoriteBookCardPresentation(entry(147),local).updateLabel)
    }
}
