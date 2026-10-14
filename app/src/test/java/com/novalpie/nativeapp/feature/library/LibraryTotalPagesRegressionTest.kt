package com.novalpie.nativeapp.feature.library
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryTotalPagesRegressionTest {
 private open class Repo:LibraryRepository {
  override suspend fun page(query:LibraryQuery)=FavoritePage(listOf(FavoriteEntry(book=NovelCard(query.page.toLong(),"QA"))),query.page,20,86,5)
  override suspend fun groups()=emptyList<FavoriteGroup>();override suspend fun user()=UserProfile(1,"QA")
 }
 @Test fun realShelfOverviewUsesTotalPagesWhileTheCurrentPageKeepsItsOwnIdentity()=runTest {
  val vm=LibraryContentViewModel(Repo(),backgroundScope);vm.load(LibraryQuery());runCurrent()
  assertEquals(1,vm.state.favoritesPage);assertEquals(5,vm.state.favoriteTotalPages)
  assertTrue(libraryOverviewForState(true,vm.state).stats.contains("总页数 5"));vm.loadMore();runCurrent()
  assertEquals(2,vm.state.favoritesPage);assertTrue(libraryOverviewForState(true,vm.state).stats.contains("总页数 5"))
 }
 @Test fun totalCanDeriveCountButUnknownTotalsNeverPretendLoadedPageIsTheTotal()=runTest {
  var total:Int?=86
  val vm=LibraryContentViewModel(object:Repo(){override suspend fun page(query:LibraryQuery)=FavoritePage(emptyList(),query.page,20,total,null)},backgroundScope)
  vm.load(LibraryQuery());runCurrent();assertEquals(5,vm.state.favoriteTotalPages)
  total=null;vm.load(LibraryQuery(groupId=99));runCurrent();assertNull(vm.state.favoriteTotalPages)
  assertTrue(libraryOverviewForState(true,vm.state).stats.contains("总页数 —"))
 }
 @Test fun historyPaginationCannotOverwriteFavoriteTotalPages()=runTest {
  val vm=LibraryContentViewModel(object:Repo(){override suspend fun page(query:LibraryQuery)=if(query.tab==FavoritesContentTab.History)FavoritePage(emptyList(),1,20,2,1)else super.page(query)},backgroundScope)
  vm.load(LibraryQuery());runCurrent();vm.load(LibraryQuery(tab=FavoritesContentTab.History));runCurrent()
  assertEquals(5,vm.state.favoriteTotalPages);assertEquals(1,vm.state.historyTotalPages);assertEquals(1,vm.state.favoritesPage)
 }
 @Test fun changingAFilterDoesNotCarryTheOldQuerysPageCount()=runTest {
  val gate=CompletableDeferred<FavoritePage>()
  val vm=LibraryContentViewModel(object:Repo(){override suspend fun page(query:LibraryQuery)=if(query.groupId==3L)gate.await()else super.page(query)},backgroundScope)
  vm.load(LibraryQuery());runCurrent();vm.load(LibraryQuery(groupId=3));runCurrent();assertNull(vm.state.favoriteTotalPages)
  gate.complete(FavoritePage(emptyList(),1,20,0,0));runCurrent();assertEquals(0,vm.state.favoriteTotalPages)
 }
}
