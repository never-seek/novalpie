package com.novalpie.nativeapp.ui
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.feature.books.*
import com.novalpie.nativeapp.model.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Production root navigation/UI with synthetic book data only; no comment, point or asset writes. */
class BookNavigationIsolationDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private class Repo:BookDetailRepository {
  override suspend fun book(id:Long)=NovelCard(id,if(id==980001L)"QA-A 返回身份"else "QA-B 第二本",author="合成作者",platform="upload")
  override suspend fun cover(id:Long):String?=null
  override suspend fun chapters(id:Long)=listOf(Chapter(id*10,"第一章",1))
  override suspend fun comments(id:Long)=listOf(ChapterComment(id*100,bookId=id,authorName="QA评论者",content="合成书评，不会发送到网站",likeCount=16,dislikeCount=1,reactionCount=7,helpfulCount=13,notHelpfulCount=0,funnyCount=2))
  override suspend fun favorite(id:Long)=FavoriteStatus(false)
  override suspend fun permissions(id:Long)=BookEditPermissions()
 }
 @Test fun actualRootRestoresBookIdentityDraftCommentTabAndCatalogQueryOnBackAndRecreation(){
  val model=ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
  val feature=model.javaClass.getDeclaredField("bookFeature").apply{isAccessible=true}.get(model) as BookDetailViewModel
  val field=feature.javaClass.getDeclaredField("repository").apply{isAccessible=true};val original=field.get(feature)
  assertFalse("需要保留的已有登录态",model.authToken.isNullOrBlank())
  try {
   compose.runOnIdle {field.set(feature,Repo());model.openBook(980001)}
   compose.waitUntil(10000){model.bookDetailState.book is LoadResult.Success&&model.bookDetailState.comments is LoadResult.Success}
   compose.onNodeWithText("评论").performScrollTo().performClick()
   compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("写书评"))
   compose.onNodeWithText("写书评").performClick()
   compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("写评论"))
   compose.onNodeWithText("写评论").performTextInput("QA-A 未发送草稿")
   compose.runOnIdle {model.updateBookCatalogQuery("A-only-filter",980001);model.openBook(980002)}
   compose.waitUntil(10000){model.bookDetailState.bookId==980002L&&model.bookDetailState.book is LoadResult.Success}
   assertEquals("",model.bookDetailState.commentDraft);assertEquals("",model.bookCatalogQuery)
   compose.runOnIdle {model.goBack()}
   compose.waitUntil(10000){model.bookDetailState.bookId==980001L&&model.bookDetailState.comments is LoadResult.Success}
   compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("写评论"))
   compose.onNodeWithText("写评论").assertExists()
   compose.onNodeWithText("QA-A 未发送草稿").assertExists()
   assertEquals("A-only-filter",model.bookCatalogQuery)
   compose.onNodeWithText("赞 13").assertExists();compose.onNodeWithText("踩 0").assertExists();compose.onNodeWithText("表情 2").assertExists()
   compose.onNodeWithText("赞 16").assertDoesNotExist()
   capture("back-to-A")
   compose.activityRule.scenario.recreate()
   compose.waitForIdle()
   compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("写评论"))
   compose.onNodeWithText("写评论").assertExists()
   compose.onNodeWithText("QA-A 未发送草稿").assertExists()
   assertEquals(AppRoute.BookDetail(980001),model.currentRoute)
   capture("recreated-A")
  } finally {
   compose.runOnIdle {feature.environmentChanged();field.set(feature,original);model.openTab(BottomTab.Collection)}
  }
 }
 private fun capture(name:String){
  val instrument=InstrumentationRegistry.getInstrumentation()
  val dir=File(instrument.targetContext.cacheDir,"beta7-book-navigation-qa").apply{mkdirs()}
  instrument.uiAutomation.takeScreenshot().let {bitmap->File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 }
}
