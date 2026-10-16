package com.novalpie.nativeapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppNavigator
import com.novalpie.nativeapp.data.FavoritesSettingsStore
import com.novalpie.nativeapp.data.ReaderSessionStore
import com.novalpie.nativeapp.feature.library.*
import com.novalpie.nativeapp.model.*
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.atomic.AtomicInteger
import android.graphics.Bitmap
import java.io.File

/** Exercise actual Collection UI and root return/entry hooks, without writing source progress. */
class HistoryAutoSyncDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun historyAutomaticallySynchronizesAfterReadingAndTabReentryWithoutRefreshClick() {
        val app=InstrumentationRegistry.getInstrumentation().targetContext
        val saved=FavoritesSettingsStore(app).load()
        val session=ReaderSessionStore(app).load()
        val vm=ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
        val feature=vm.javaClass.getDeclaredField("libraryFeature").apply{isAccessible=true}.get(vm) as LibraryContentViewModel
        val repoField=feature.javaClass.getDeclaredField("repository").apply{isAccessible=true}
        val old=repoField.get(feature)
        val revisions=vm.javaClass.getDeclaredField("readerProgressRevision").apply{isAccessible=true}
        val navigator=vm.javaClass.getDeclaredField("navigator").apply{isAccessible=true}.get(vm) as AppNavigator
        val calls=AtomicInteger();var chapter=1
        val repository=object:LibraryRepository {
            override suspend fun page(query:LibraryQuery):FavoritePage {
                if(query.tab==FavoritesContentTab.History)calls.incrementAndGet()
                return FavoritePage(listOf(FavoriteEntry(book=NovelCard(980501,"QA 历史自动同步",author="测试作者"),
                    lastChapter=chapter,chapterCount=20)),query.page,20,1,1)
            }
            override suspend fun groups()=emptyList<FavoriteGroup>()
            override suspend fun user()=UserProfile(1,"QA")
        }
        try {
            // Install the controlled fixture immediately, not after waiting for the user's
            // previously restored network/GIF screen to become Compose-idle.
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                repoField.set(feature,repository);vm.openTab(BottomTab.Collection)
                vm.selectFavoritesContentTab(FavoritesContentTab.History);vm.loadHome()
            }
            compose.waitUntil(10_000){(vm.homeState.history as? LoadResult.Success)?.value?.singleOrNull()?.book?.id==980501L}
            compose.onNodeWithText("1/20").assertExists()
            val initial=calls.get()
            compose.runOnIdle {
                chapter=4;revisions.setLong(vm,revisions.getLong(vm)+1)
                navigator.replaceAll(listOf(AppRoute.Home,AppRoute.Reader(980501,98050104)))
                vm.goBack()
            }
            compose.waitUntil(10_000){calls.get()>initial&&(vm.homeState.history as? LoadResult.Success)?.value?.singleOrNull()?.lastChapter==4}
            compose.onNodeWithText("4/20").assertExists()
            assertEquals(FavoritesContentTab.History,vm.homeState.options.tab)
            val afterReader=calls.get()
            compose.runOnIdle {vm.openTab(BottomTab.Tools);chapter=5;revisions.setLong(vm,revisions.getLong(vm)+1);vm.openTab(BottomTab.Collection)}
            compose.waitUntil(10_000){calls.get()>afterReader&&(vm.homeState.history as? LoadResult.Success)?.value?.singleOrNull()?.lastChapter==5}
            compose.onNodeWithText("5/20").assertExists()
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let {bitmap->
                File(app.getExternalFilesDir(null),"history-auto-sync-verified.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
        }finally {
            compose.runOnIdle {repoField.set(feature,old);vm.selectFavoritesContentTab(if(saved.tab=="history")FavoritesContentTab.History else FavoritesContentTab.Favorites);vm.openTab(BottomTab.Collection);vm.loadHome()}
            FavoritesSettingsStore(app).save(saved)
            if(session!=null)ReaderSessionStore(app).save(session.bookId,session.chapterId)
        }
    }
}
