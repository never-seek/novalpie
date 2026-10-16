package com.novalpie.nativeapp.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.feature.library.*
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.NovalPieTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*

/** Actual Compose taps, with a controlled repository: no changes to the user's online shelf. */
class FavoriteDestinationDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun plusCreatesFolderDirectlyWithoutOpeningManagement() {
        var state by mutableStateOf(HomeState(groups = LoadResult.Success(listOf(FavoriteGroup(17,"原有收藏夹")))))
        val names = mutableListOf<String>()
        compose.setContent {
            NovalPieTheme {
                FavoritesControlPanel(state, "", {}, {}, {}, {}, {}, {}, { _, _ -> }, {},
                    onCreateGroup = { name -> names += name; state = state.copy(actionMessage = "已创建分组",
                        groups = LoadResult.Success(listOf(FavoriteGroup(17,"原有收藏夹"),FavoriteGroup(23,name)))) },
                    onRenameGroup = { _, _ -> }, onDeleteGroup = {}, onMoveSelectedFavorites = {},
                    onRemoveSelectedFavorites = {}, onDeleteSelectedHistory = {}, onClearAllHistory = {},
                    onCycleCacheMode = {}, onClearFavoritesCache = {}, onClearImageCache = {}, onRetry = {})
            }
        }
        compose.onNodeWithContentDescription("新建收藏夹").performClick()
        compose.onNodeWithText("分组与显示管理").assertDoesNotExist()
        compose.onNodeWithText("收藏夹名称").performTextInput("QA 快速创建")
        compose.onNodeWithText("创建", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { names.size == 1 }
        compose.onNodeWithText("QA 快速创建").assertExists()
        assertEquals(listOf("QA 快速创建"), names)
    }

    @Test fun destinationTapUsesCustomFolderAndCreateThenAddUsesTheNewId() {
        val requests = mutableListOf<Pair<Long,Long>>()
        val groups = mutableListOf(FavoriteGroup(17,"自选收藏夹"))
        val repository = object : FavoriteDestinationRepository {
            override suspend fun groups() = groups.toList()
            override suspend fun create(name: String) = FavoriteGroup(23,name).also(groups::add)
            override suspend fun add(bookId: Long, groupId: Long) = FavoriteStatus(true, groupId).also { requests += bookId to groupId }
        }
        lateinit var vm: FavoriteDestinationViewModel
        compose.setContent {
            val scope = rememberCoroutineScope()
            vm = remember { FavoriteDestinationViewModel(repository, scope, { _, _ -> }) }
            NovalPieTheme {
                FavoriteDestinationDialog(vm.state, vm::dismiss, vm::choose, vm::refresh, vm::startCreate,
                    vm::cancelCreate, vm::updateName, vm::createAndChoose)
            }
        }
        compose.runOnIdle { vm.open(42) }
        compose.waitUntil(5_000) { vm.state.groups is LoadResult.Success }
        compose.onNodeWithText("自选收藏夹").performClick()
        compose.waitUntil(5_000) { !vm.state.visible }
        compose.runOnIdle { vm.open(43) }
        compose.waitUntil(5_000) { vm.state.groups is LoadResult.Success }
        compose.onNodeWithText("新建收藏夹").performClick()
        compose.onNodeWithText("收藏夹名称").performTextInput("QA 第二夹")
        compose.onNodeWithText("创建并收藏").performClick()
        compose.waitUntil(5_000) { !vm.state.visible }
        assertEquals(listOf(42L to 17L,43L to 23L), requests)
    }
}
