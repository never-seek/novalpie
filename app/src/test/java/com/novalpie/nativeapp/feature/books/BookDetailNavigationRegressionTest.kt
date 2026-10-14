package com.novalpie.nativeapp.feature.books
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailNavigationRegressionTest {
 private open class Repo:BookDetailRepository {
  override suspend fun book(id:Long)=NovelCard(id,"Book$id")
  override suspend fun cover(id:Long):String?=null
  override suspend fun chapters(id:Long)=listOf(Chapter(id*10,"One",1))
  override suspend fun comments(id:Long)=emptyList<ChapterComment>()
  override suspend fun favorite(id:Long)=FavoriteStatus(false)
  override suspend fun permissions(id:Long)=BookEditPermissions()
 }
 @Test fun returningToAnEarlierBookRetainsItsOwnDraftAndReply()=runTest {
  val vm=BookDetailViewModel(Repo(),backgroundScope)
  vm.load(1,true);runCurrent();vm.present{it.copy(commentDraft="A draft",replyingToCommentId=12,replyingToName="A user")}
  vm.load(2,true);runCurrent();vm.present{it.copy(commentDraft="B draft",replyingToCommentId=22,replyingToName="B user")}
  vm.load(1,true,retainComposer=true);runCurrent()
  assertEquals(1L,vm.state.bookId);assertEquals("A draft",vm.state.commentDraft);assertEquals(12L,vm.state.replyingToCommentId)
  vm.load(2,true,retainComposer=true);runCurrent();assertEquals("B draft",vm.state.commentDraft)
 }
 @Test fun explicitRefreshDoesNotSilentlyDiscardAnUnsentDraft()=runTest {
  val vm=BookDetailViewModel(Repo(),backgroundScope);vm.load(1,true);runCurrent();vm.present{it.copy(commentDraft="Unsent")}
  vm.load(1,true);runCurrent();assertEquals("Unsent",vm.state.commentDraft)
 }
 @Test fun switchingAccountClearsArchivedDraftsAndPrivilegedData()=runTest {
  val vm=BookDetailViewModel(Repo(),backgroundScope);vm.load(1,true);runCurrent();vm.present{it.copy(commentDraft="private A")}
  vm.load(2,true);runCurrent();vm.environmentChanged();vm.load(1,false,retainComposer=true);runCurrent()
  assertEquals("",vm.state.commentDraft);assertFalse((vm.state.managementPermissions as LoadResult.Success).value.title)
 }
 @Test fun olderSameBookReplyCannotReplaceReenteredBookData()=runTest {
  var calls=0;val old=CompletableDeferred<NovelCard>()
  val repo=object:Repo(){override suspend fun book(id:Long):NovelCard {
   if(id==1L&&++calls==1)return withContext(NonCancellable){old.await()};return NovelCard(id,"Fresh$id")
  }}
  val vm=BookDetailViewModel(repo,backgroundScope);vm.load(1,true);runCurrent();vm.present{it.copy(commentDraft="A")}
  vm.load(2,true);runCurrent();vm.load(1,true,retainComposer=true);runCurrent();old.complete(NovelCard(1,"Stale"));runCurrent()
  assertEquals("Fresh1",(vm.state.book as LoadResult.Success).value.title);assertEquals("A",vm.state.commentDraft)
 }

 @Test fun boundedHeavySnapshotsStillRetainAnEvictedUnsentComposer()=runTest {
  val vm=BookDetailViewModel(Repo(),backgroundScope);vm.load(1,true);runCurrent();vm.present{it.copy(commentDraft="Old draft",catalogQuery="Old query")}
  for(id in 2L..26L){vm.load(id,true);runCurrent()}
  val cache=vm.javaClass.getDeclaredField("retained").apply{isAccessible=true}.get(vm) as Map<*,*>
  assertTrue(cache.size<=12);vm.load(1,true);runCurrent();assertEquals("Old draft",vm.state.commentDraft);assertEquals("Old query",vm.state.catalogQuery)
 }
 @Test fun wrongServerBookIdentityIsNotRenderedAsTheRequestedBook()=runTest {
  val vm=BookDetailViewModel(object:Repo(){override suspend fun book(id:Long)=NovelCard(999,"Wrong")},backgroundScope)
  vm.load(1,true);runCurrent();assertTrue(vm.state.book is LoadResult.Error)
 }
 @Test fun archivedMutationCompletesOnlyItsBookAndCannotRepeat()=runTest {
  val vm=BookDetailViewModel(Repo(),backgroundScope);vm.load(1,true);runCurrent();vm.present{it.copy(commentDraft="A")}
  val token=vm.beginAction(BookActionLane.Comments)!!;assertNull(vm.beginAction(BookActionLane.Comments))
  vm.load(2,true);runCurrent();vm.present{it.copy(commentDraft="B")}
  assertTrue(vm.finishAction(token){it.copy(commentDraft="",actionMessage="A sent")});assertFalse(vm.finishAction(token){error("duplicate completion")})
  assertEquals(2L,vm.state.bookId);assertEquals("B",vm.state.commentDraft)
  vm.restore(1,true);runCurrent();assertFalse(vm.state.actionLoading);assertEquals("",vm.state.commentDraft)
 }
 @Test fun accountChangeInvalidatesPendingWritesAndRetainedPrivateComposers()=runTest {
  val vm=BookDetailViewModel(Repo(),backgroundScope);vm.load(1,true);runCurrent();vm.present{it.copy(commentDraft="A private")}
  val token=vm.beginAction(BookActionLane.Comments)!!;vm.environmentChanged();vm.load(1,true);runCurrent()
  assertFalse(vm.finishAction(token){error("old account")});assertEquals("",vm.state.commentDraft);assertFalse(vm.state.actionLoading)
 }
 @Test fun lateReadCannotUndoAFavoriteWriteConfirmedDuringRefresh()=runTest {
  var defer=false;val response=CompletableDeferred<FavoriteStatus>()
  val vm=BookDetailViewModel(object:Repo(){override suspend fun favorite(id:Long)=if(defer)response.await()else FavoriteStatus(false)},backgroundScope)
  vm.load(1,true);runCurrent();val token=vm.beginAction(BookActionLane.Favorite)!!;defer=true;vm.load(1,true);runCurrent()
  vm.finishAction(token){it.copy(favoriteStatus=LoadResult.Success(FavoriteStatus(true)))}
  response.complete(FavoriteStatus(false));runCurrent();assertTrue((vm.state.favoriteStatus as LoadResult.Success).value.isFavorited)
 }
}
