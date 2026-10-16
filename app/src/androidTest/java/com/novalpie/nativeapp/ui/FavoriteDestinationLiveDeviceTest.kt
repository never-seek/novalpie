package com.novalpie.nativeapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.FavoritesSettingsStore
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Opt-in: only a new QA folder and a previously unfavorited book in this account are changed. */
class FavoriteDestinationLiveDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun realPlusAndDetailFavoriteSaveToTheChosenFolderAndCleanup() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveFavorites") == "1")
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer.from(app)
        container.refreshEnvironmentFromStores()
        val api = container.api
        val owner = api.currentUser().id ?: error("需要用户已登录的MuMu")
        val options = FavoritesSettingsStore(app).load()
        val vm = ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
        val candidate = api.search("", limit = 20).firstOrNull { !api.favoriteStatus(it.id).isFavorited }
            ?: error("本页没有未收藏测试书，不会修改已有收藏")
        val name = "QA收藏夹${System.currentTimeMillis()}"
        var groupId: Long? = null
        var added = false
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { vm.openTab(BottomTab.Collection); vm.selectFavoritesContentTab(FavoritesContentTab.Favorites); vm.loadHome() }
            compose.waitUntil(30_000) { vm.homeState.groups is LoadResult.Success }
            compose.onNodeWithContentDescription("新建收藏夹").performClick()
            compose.onNodeWithText("收藏夹名称").performTextInput(name)
            compose.onNodeWithText("创建", useUnmergedTree = true).performClick()
            compose.waitUntil(30_000) { (vm.homeState.groups as? LoadResult.Success)?.value?.any { it.name == name } == true }
            groupId = (vm.homeState.groups as LoadResult.Success).value.single { it.name == name }.id!!
            InstrumentationRegistry.getInstrumentation().runOnMainSync { vm.openBook(candidate.id) }
            compose.waitUntil(30_000) { vm.bookDetailState.book is LoadResult.Success && vm.bookDetailState.favoriteStatus is LoadResult.Success }
            compose.onNodeWithText("收藏", useUnmergedTree = true).performClick()
            compose.waitUntil(30_000) { vm.favoriteDestinationState.groups is LoadResult.Success }
            compose.onNodeWithText(name).performClick()
            compose.waitUntil(30_000) { (vm.bookDetailState.favoriteStatus as? LoadResult.Success)?.value?.isFavorited == true }
            added = true
            val status = api.favoriteStatus(candidate.id)
            assertTrue(status.isFavorited); assertEquals(groupId,status.groupId)
            assertTrue(api.favoritePage(groupId = groupId, limit = 100).items.any { it.book.id == candidate.id })
            File(app.getExternalFilesDir(null),"favorite-live-verified.json").writeText(JSONObject()
                .put("plusCreateVerified",true).put("detailCustomFolderVerified",true)
                .put("bookId",candidate.id).put("groupId",groupId).put("cleanupPending",true).toString(2))
        } finally {
            check(api.currentUser().id == owner) { "账号改变，不会对另一账号执行清理" }
            // If a UI assertion timed out after an accepted write, query before cleanup.
            if (groupId == null) groupId = api.favoriteGroups().singleOrNull { it.name == name }?.id
            if (groupId != null) {
                val current = api.favoriteStatus(candidate.id)
                if (current.isFavorited && current.groupId == groupId) api.toggleFavorite(candidate.id,true,groupId!!)
                val remaining = api.favoritePage(groupId = groupId, limit = 100)
                check(remaining.items.isEmpty()) { "测试收藏夹中出现其他条目，不删除" }
                api.deleteFavoriteGroup(groupId!!)
                assertFalse(api.favoriteGroups().any { it.id == groupId })
                assertFalse(api.favoriteStatus(candidate.id).isFavorited)
            }
            File(app.getExternalFilesDir(null),"favorite-live-cleanup.json").writeText(JSONObject()
                .put("testFolderRemoved",groupId != null).put("testFavoriteRemoved",added).put("originalFavoritesUntouched",true).toString(2))
            FavoritesSettingsStore(app).save(options)
            InstrumentationRegistry.getInstrumentation().runOnMainSync { vm.openTab(BottomTab.Collection); vm.selectFavoritesContentTab(if(options.tab=="history")FavoritesContentTab.History else FavoritesContentTab.Favorites); vm.loadHome() }
        }
    }
}
