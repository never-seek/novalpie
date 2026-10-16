package com.novalpie.nativeapp.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.feature.library.*
import com.novalpie.nativeapp.model.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Actual History UI + real progress preferences, simulated remote reads, no source writes. */
class HistorySyncAndUpdateDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun coldHistorySyncAndRemoteProgressKeepUpdateCountsConsistentAcrossRefreshes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val prefs = app.getSharedPreferences("novalpie_native_reader_progress",Context.MODE_PRIVATE)
        val original = prefs.all.mapValues { (_,v) -> if (v is Set<*>) v.toSet() else v }
        val options = FavoritesSettingsStore(app).load()
        val session = ReaderSessionStore(app).load()
        val store = ReaderProgressStore(app)
        val vm = ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
        val feature = vm.javaClass.getDeclaredField("libraryFeature").apply { isAccessible=true }.get(vm) as LibraryContentViewModel
        val field = feature.javaClass.getDeclaredField("repository").apply { isAccessible=true }
        val previous = field.get(feature)
        var read = 141; var total = 147; var secondRead = 89
        val repo = object : LibraryRepository {
            override suspend fun page(query: LibraryQuery) = FavoritePage(listOf(
                FavoriteEntry(book=NovelCard(980631,"QA 跨设备进度",author="测试"),lastChapter=read,
                    lastChapterId=980631000L+read,chapterCount=total),
                FavoriteEntry(book=NovelCard(980632,"QA 单章更新",author="测试"),lastChapter=secondRead,
                    lastChapterId=980632000L+secondRead,chapterCount=90),
            ),query.page,20,53,3)
            override suspend fun groups() = emptyList<FavoriteGroup>()
            override suspend fun user() = UserProfile(1,"QA")
        }
        fun screenshot(name:String) {
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                File(app.getExternalFilesDir(null),name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
            }
        }
        fun waitFor(number:Int) = compose.waitUntil(15_000) {
            (vm.homeState.history as? LoadResult.Success)?.value?.firstOrNull()?.lastChapter == number &&
                (vm.homeState.history as? LoadResult.Success)?.value?.firstOrNull()?.chapterCount == total
        }
        try {
            instrumentation.runOnMainSync {
                store.save(980631,980631104,"旧目录已读完",chapterNumber=104,chapterCountAtLastRead=104)
                store.save(980632,980632089,"旧目录已读完",chapterNumber=89,chapterCountAtLastRead=89)
                field.set(feature,repo); vm.openTab(BottomTab.Collection)
                vm.selectFavoritesContentTab(FavoritesContentTab.History); vm.loadHome()
            }
            waitFor(141)
            compose.onNodeWithText("已同步").assertExists()
            compose.onNodeWithText("待同步").assertDoesNotExist()
            compose.onNodeWithText("记录 53").assertExists()
            compose.onNodeWithText("总页数 3").assertExists()
            compose.onNodeWithText("收藏 —").assertDoesNotExist()
            compose.onNodeWithText("141/147").assertExists()
            compose.onNodeWithText("更新 6 章").assertExists()
            compose.onNodeWithText("更新 43 章").assertDoesNotExist()
            compose.onNodeWithText("更新 1 章").assertExists()
            screenshot("history-update-six-verified.png")

            instrumentation.runOnMainSync { read=145; secondRead=90; vm.loadHome() }
            waitFor(145)
            compose.onNodeWithText("145/147").assertExists()
            compose.onNodeWithText("更新 2 章").assertExists()
            compose.onNodeWithText("90/90").assertExists()
            compose.onNodeWithText("更新 1 章").assertDoesNotExist()
            screenshot("history-update-two-verified.png")

            instrumentation.runOnMainSync { read=147; vm.loadHome() }
            waitFor(147)
            compose.onNodeWithText("147/147").assertExists()
            compose.onNodeWithText("更新 2 章").assertDoesNotExist()
            instrumentation.runOnMainSync { total=148; vm.loadHome() }
            waitFor(147)
            compose.onNodeWithText("147/148").assertExists()
            compose.onNodeWithText("更新 1 章").assertExists()
        } finally {
            instrumentation.runOnMainSync {
                val edit=prefs.edit().clear()
                original.forEach { (k,v) -> when(v) {
                    is String -> edit.putString(k,v); is Int -> edit.putInt(k,v); is Long -> edit.putLong(k,v)
                    is Boolean -> edit.putBoolean(k,v); is Float -> edit.putFloat(k,v)
                    is Set<*> -> edit.putStringSet(k,v.filterIsInstance<String>().toSet())
                    else -> Unit
                } }
                edit.commit(); assertEquals(original,prefs.all)
                field.set(feature,previous)
                FavoritesSettingsStore(app).save(options)
                if (session!=null) ReaderSessionStore(app).save(session.bookId,session.chapterId)
                vm.openTab(BottomTab.Collection)
                vm.selectFavoritesContentTab(if(options.tab=="history")FavoritesContentTab.History else FavoritesContentTab.Favorites)
                vm.loadHome()
            }
        }
        File(app.getExternalFilesDir(null),"history-status-updates-verified.json").writeText(JSONObject()
            .put("loadedHistorySynced",true).put("updateSequence","141/147:6 -> 145/147:2 -> 147/147:0 -> 147/148:1")
            .put("preferencesRestored",true).put("sourceWrites",0).toString(2))
    }
}
