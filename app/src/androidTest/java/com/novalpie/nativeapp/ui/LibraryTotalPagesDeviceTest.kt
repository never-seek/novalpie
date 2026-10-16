package com.novalpie.nativeapp.ui
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.FavoritesSettingsStore
import com.novalpie.nativeapp.data.ReaderSessionStore
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.feature.library.*
import com.novalpie.nativeapp.model.*
import org.junit.*
import org.junit.Assert.*
class LibraryTotalPagesDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun actualHomeStatsShowsFiveTotalPagesNotTheFirstLoadedPage() {
  val app=InstrumentationRegistry.getInstrumentation().targetContext
  val options=FavoritesSettingsStore(app).load();val session=ReaderSessionStore(app).load()
  val model=ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
  val feature=model.javaClass.getDeclaredField("libraryFeature").apply{isAccessible=true}.get(model) as LibraryContentViewModel
  val field=feature.javaClass.getDeclaredField("repository").apply{isAccessible=true};val before=field.get(feature)
  val fake=object:LibraryRepository {
   override suspend fun page(query:LibraryQuery)=FavoritePage(listOf(FavoriteEntry(book=NovelCard(999991,"Synthetic shelf page"))),query.page,20,86,5)
   override suspend fun groups()=emptyList<FavoriteGroup>();override suspend fun user()=UserProfile(999999990,"Synthetic user")
  }
  try {
   // This is a Favorites-page assertion; do not inherit the user's History tab preference.
   InstrumentationRegistry.getInstrumentation().runOnMainSync {
    field.set(feature,fake);model.openTab(BottomTab.Collection)
    model.selectFavoritesContentTab(FavoritesContentTab.Favorites);model.loadHome()
   }
   compose.waitUntil(10000){feature.state.favoriteEntries is LoadResult.Success}
   assertEquals(1,feature.state.favoritesPage);assertEquals(5,feature.state.favoriteTotalPages)
   compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("总页数 5"))
   compose.onNodeWithText("总页数 5").assertIsDisplayed()
   compose.onNodeWithText("总页数 1").assertDoesNotExist()
  }finally{InstrumentationRegistry.getInstrumentation().runOnMainSync {
   feature.environmentChanged();field.set(feature,before)
   model.selectFavoritesContentTab(if(options.tab=="history")FavoritesContentTab.History else FavoritesContentTab.Favorites)
   model.loadHome();FavoritesSettingsStore(app).save(options)
   if(session!=null)ReaderSessionStore(app).save(session.bookId,session.chapterId)
  }}
 }
}
