package com.novalpie.nativeapp.feature.library

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryContentViewModelTest {
    private fun page(number:Int,id:Long)=FavoritePage(listOf(FavoriteEntry(book=NovelCard(id,"书$id"))),number,20,41,3)
    private class Repository(val result:suspend(LibraryQuery)->FavoritePage):LibraryRepository {
        val queries=mutableListOf<LibraryQuery>()
        override suspend fun page(query:LibraryQuery):FavoritePage{queries+=query;return result(query)}
        override suspend fun groups()=emptyList<FavoriteGroup>()
        override suspend fun user()=UserProfile(id=1,name="测试")
    }
    @Test fun aColdLoadAlwaysStartsFromPageOneAndRetainsTheSourceTotal()=runTest {
        val repository=Repository{page(it.page,10)}
        val model=LibraryContentViewModel(repository,backgroundScope)
        model.load(LibraryQuery(page=8));runCurrent()
        assertEquals(1,repository.queries.single().page)
        assertEquals(41,model.state.favoriteTotal)
        assertEquals(1,model.state.favoritesPage)
    }
    @Test fun switchingGroupsInvalidatesLateResponsesEvenIfNetworkCancellationIsIgnored()=runTest {
        val first=CompletableDeferred<FavoritePage>()
        val repository=Repository{if(it.groupId==10L)withContext(NonCancellable){first.await()}else page(1,20)}
        val model=LibraryContentViewModel(repository,backgroundScope)
        model.load(LibraryQuery(groupId=10));runCurrent()
        model.load(LibraryQuery(groupId=20));runCurrent()
        first.complete(page(1,10));runCurrent()
        assertEquals(20L,(model.state.favoriteEntries as LoadResult.Success).value.single().book.id)
    }
    @Test fun additionalPageFailurePreservesBooksAndRetriesTheSamePage()=runTest {
        var fail=true
        val repository=Repository{if(it.page==2&&fail)error("offline")else page(it.page,it.page.toLong())}
        val model=LibraryContentViewModel(repository,backgroundScope)
        model.load(LibraryQuery());runCurrent();model.loadMore();runCurrent()
        assertEquals(listOf(1L),(model.state.favoriteEntries as LoadResult.Success).value.map{it.book.id})
        assertNotNull(model.state.favoritesLoadMoreError)
        fail=false;model.loadMore();runCurrent()
        assertEquals(listOf(1,2,2),repository.queries.map{it.page})
        assertEquals(listOf(1L,2L),(model.state.favoriteEntries as LoadResult.Success).value.map{it.book.id})
    }
    @Test fun presentationChangesNeverCancelAnInFlightQueryOrLoseItsResponse()=runTest {
        val pending=CompletableDeferred<FavoritePage>()
        val model=LibraryContentViewModel(Repository{pending.await()},backgroundScope)
        model.load(LibraryQuery());runCurrent()
        model.present{it.copy(options=it.options.copy(gridColumns=4))}
        pending.complete(page(1,10));runCurrent()
        assertEquals(4,model.state.options.gridColumns)
        assertTrue(model.state.favoriteEntries is LoadResult.Success)
    }

    @Test fun refreshingAfterReadingKeepsAllPreviouslyLoadedPagesNotJustTheLastPage()=runTest {
        val repository=Repository{page(it.page,it.page.toLong())}
        val model=LibraryContentViewModel(repository,backgroundScope)
        model.load(LibraryQuery());runCurrent();model.loadMore();runCurrent()
        model.load(LibraryQuery(page=2),retain=true);runCurrent()
        assertEquals(listOf(1L,2L),(model.state.favoriteEntries as LoadResult.Success).value.map{it.book.id})
        assertEquals(2,model.state.favoritesPage)
    }
}
