package com.novalpie.nativeapp.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.Text
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.feature.search.*
import com.novalpie.nativeapp.feature.books.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SearchRetranslationDeviceTest {
    @get:Rule val compose=createComposeRule()
    private class TestContext(base:Context):ContextWrapper(base) {
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences =
            baseContext.getSharedPreferences("qa_search_20261008_$name",mode)
    }
    @Test fun nativeSaveAndClearButtonsPreserveActiveTagsAndRestoreFullSnapshot() {
        val context=TestContext(InstrumentationRegistry.getInstrumentation().targetContext)
        val store=SearchSettingsStore(context).also{it.clear()}
        val chosen=PersistedSearchSettings(sortBy="updated_at",source="upload",adultFilter="adult_only",
            requiredTags=listOf("奇幻"),blockedTags=listOf("后宫"),wordCountRange="100000..500000")
        store.save(chosen)
        val prefs=StoredSearchPreferences(store,SearchHistoryStore(context))
        lateinit var model:SearchViewModel
        compose.runOnIdle {
            model=SearchViewModel(object:SearchRepository {
                override suspend fun page(request:ResolvedSearchRequest,page:Int)=SearchPage(emptyList(),page=page)
                override suspend fun tags()=emptyList<NovelTag>()
            },prefs)
        }
        compose.setContent {
            NovalPieTheme {
                SourceSearchFilterPanel(options=model.state.options,tags=LoadResult.Success(emptyList()),
                    onApplyRequiredTag={model.tag(it,SearchTagFilterMode.Required)},
                    onApplyBlockedTag={model.tag(it,SearchTagFilterMode.Blocked)},onRemoveTag=model::removeTag,
                    onClearTags=model::clearTags,onRefreshTags={},onSortByChange={},onSortOrderChange={},
                    onScopeChange={},onMatchTypeChange={},onAdultFilterChange={},onSourceChange={},
                    onWordCountRangeChange={},onSaveConditions=model::saveConditions,
                    onToggleSettingsCache=model::toggleCache,onClearSettingsCache=model::clearOptions)
            }
        }
        try {
            compose.onNodeWithContentDescription("不缓存搜索设置").performClick()
            compose.onNodeWithContentDescription("保存当前搜索条件").performClick()
            compose.runOnIdle { assertEquals(chosen,store.load()) }
            compose.onNodeWithContentDescription("清除搜索设置缓存").performClick()
            compose.onNodeWithText("清除",useUnmergedTree=true).performClick()
            compose.runOnIdle {
                assertEquals(chosen.toSearchOptions(),model.state.options)
                assertEquals(PersistedSearchSettings(),store.load())
            }
            compose.onNodeWithContentDescription("保存当前搜索条件").performClick()
            compose.runOnIdle { assertEquals(chosen,SearchSettingsStore(context).load()) }
        } finally { compose.runOnIdle {model.close()};store.clear() }
    }
    @Test fun nativeRetranslationDialogPrefillsOrdinalAndSubmitsOnlyOnce() {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        var posts=0;var submitted=emptyList<Int>()
        lateinit var controller:ChapterRetranslationController
        compose.runOnIdle {
            controller=ChapterRetranslationController(object:ChapterRetranslationRepository {
                override suspend fun chapters(bookId:Long)=emptyList<Chapter>()
                override suspend fun hasApprovedPersonalModel()=true
                override suspend fun submit(bookId:Long,numbers:List<Int>):ForumActionResult {
                    posts++;submitted=numbers;return ForumActionResult(true)
                }
            },scope){0}
            controller.open(360661,"受控 QA 书籍",listOf(Chapter(901,"EP49",number=49),Chapter(902,"EP50",number=50)),49)
        }
        compose.setContent { NovalPieTheme {
            ChapterRetranslationDialog(controller.state,controller::updateRange,controller::submit,controller::dismiss,
                onWorkspace={},onRefresh={},onCheckedForRetry=controller::confirmCheckedForRetry)
        } }
        try {
            compose.onNodeWithText("49").assertExists()
            compose.onNodeWithText("提交重翻任务").performClick()
            compose.waitForIdle()
            compose.runOnIdle {assertEquals(1,posts);assertEquals(listOf(49),submitted);assertTrue(controller.state.confirmed)}
            compose.onNodeWithText("提交重翻任务").assertIsNotEnabled()
        } finally {scope.cancel()}
    }
    @Test fun nativeReaderBrightnessRestoresTheWindowWhenReaderCloses() {
        val active=mutableStateOf(true)
        var window:android.view.Window?=null;var previous=-1f
        compose.setContent {
            val view=LocalView.current
            val activity=view.context as android.app.Activity
            if(window==null){window=activity.window;previous=activity.window.attributes.screenBrightness}
            ReaderWindowBrightness(activity.window,active.value,.3f)
            Text("Brightness QA")
        }
        compose.runOnIdle {assertEquals(.3f,window!!.attributes.screenBrightness,.0001f);active.value=false}
        compose.waitForIdle()
        compose.runOnIdle {assertEquals(previous,window!!.attributes.screenBrightness,.0001f)}
    }
}
