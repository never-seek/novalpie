package com.novalpie.nativeapp.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.feature.search.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Live account-owned read requests, isolated local settings: never mutates the user's filters. */
class SearchLiveControlsDeviceTest {
    @get:Rule val compose=createComposeRule()
    private class Sandbox(base:Context):ContextWrapper(base) {
        override fun getSharedPreferences(name:String,mode:Int)=baseContext.getSharedPreferences("qa_feedback_controls_$name",mode)
    }
    @Test fun allNativeSmallSearchControlsMapToRequestsAndRealResponses() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveNovalPie")=="1")
        val app=InstrumentationRegistry.getInstrumentation().targetContext
        val store=SearchSettingsStore(Sandbox(app)).also{it.clear()}
        val preferences=StoredSearchPreferences(store,SearchHistoryStore(Sandbox(app)))
        val api=AppContainer.from(app).also{it.refreshEnvironmentFromStores()}.api
        lateinit var model:SearchViewModel
        val results=JSONArray()
        compose.runOnIdle{model=SearchViewModel(WebsiteSearchRepository(api),preferences)}
        fun options(transform:(SearchOptions)->SearchOptions){model.updateOptions(transform(model.state.options),refresh=false)}
        compose.setContent {NovalPieTheme {
            SourceSearchFilterPanel(model.state.options,model.state.tags,
                onApplyRequiredTag={model.tag(it,SearchTagFilterMode.Required)},onApplyBlockedTag={model.tag(it,SearchTagFilterMode.Blocked)},
                onRemoveTag=model::removeTag,onClearTags=model::clearTags,onRefreshTags=model::loadTags,
                onSortByChange={options{v->v.copy(sortBy=it)}},onSortOrderChange={options{v->v.copy(sortOrder=it)}},
                onScopeChange={options{v->v.copy(scope=it)}},onMatchTypeChange={options{v->searchOptionsAfterMatchTypeChange(v,it)}},
                onAdultFilterChange={options{v->v.copy(adultFilter=it)}},onSourceChange={options{v->v.copy(source=it)}},
                onWordCountRangeChange={options{v->v.copy(wordCountRange=it)}},onSaveConditions=model::saveConditions,
                onToggleSettingsCache=model::cycleCacheMode,onClearSettingsCache=model::clearOptions,
                onSelectCacheMode=model::setCacheMode)
        }}
        fun requestEvidence(label:String,assertCards:(List<NovelCard>)->Unit={}) {
            compose.runOnIdle{model.submit()}
            compose.waitUntil(35_000){!model.state.loadingPage}
            val state=model.state
            val record=JSONObject().put("label",label).put("options",state.options.toString())
            val books=(state.results as? LoadResult.Success)?.value
            record.put("success",books!=null).put("count",books?.size ?: 0)
            if(books==null)record.put("error",(state.results as? LoadResult.Error)?.message)
            else assertCards(books)
            results.put(record)
        }
        try {
            compose.onNodeWithContentDescription("搜索使用说明").performClick();compose.onNodeWithText("知道了").performClick()
            compose.onNodeWithContentDescription("搜索设置").performClick()
            compose.onNodeWithText("不缓存搜索框信息").performClick();compose.onNodeWithText("完成").performClick()
            compose.runOnIdle{assertEquals("no-query",model.state.options.cacheMode)}
            compose.onNodeWithContentDescription(searchCachePolicyActionLabel("no-query")).performClick()
            compose.runOnIdle{assertEquals("all",model.state.options.cacheMode)}
            val groups=sourcePrimarySearchFilterGroups(model.state.options)
            for(group in groups) {
                for(choice in group.choices) {
                    val label=group.label
                    val current=sourcePrimarySearchFilterGroups(model.state.options).single{it.label==label}.choices.first{it.selected}
                    compose.onNodeWithContentDescription("$label: ${current.label}").performClick()
                    compose.onAllNodesWithText(choice.label,useUnmergedTree=true).onLast().performClick()
                    compose.runOnIdle {
                        val selected=sourcePrimarySearchFilterGroups(model.state.options).single{it.label==label}.choices.first{it.selected}
                        assertEquals(choice.value,selected.value)
                    }
                    // Every visible selector is exercised; network evidence uses representative
                    // boundaries per group to avoid dozens of billable AI searches.
                    if(choice.value in setOf("upload","novelPia","adult_only","unrestricted","exact","fuzzy_loose","author","tags","asc","word_count")) {
                        requestEvidence("$label=${choice.value}"){books->
                            if(label=="来源")assertTrue(books.all{it.platform.equals(choice.value,true)})
                        }
                    }
                }
            }
            compose.onNodeWithText("标签").performClick()
            compose.onNodeWithText("刷新",substring=false).performClick()
            compose.waitUntil(35_000){model.state.tags !is LoadResult.Loading}
            compose.runOnIdle{assertTrue("The native refresh action must load real website tags",model.state.tags is LoadResult.Success)}
            compose.onNodeWithText("输入标签").performTextInput("奇幻");compose.onNodeWithText("添加").performClick()
            compose.onNodeWithText("屏蔽",substring=false).performClick()
            compose.onNodeWithText("输入标签").performTextInput("后宫");compose.onNodeWithText("添加").performClick()
            compose.onNodeWithContentDescription("保存当前搜索条件").performClick()
            compose.onNodeWithContentDescription("清除搜索设置缓存").performClick();compose.onNodeWithText("清除",useUnmergedTree=true).performClick()
            compose.onNodeWithText("包含 奇幻 ×").assertExists();compose.onNodeWithText("屏蔽 后宫 ×").assertExists()
            compose.onNodeWithText("清除标签").performClick()
            compose.onNodeWithText("字数",substring=false).performClick()
            compose.runOnIdle{options{it.copy(source="",adultFilter="all",scope="all",matchType="fuzzy_strict",sortBy="word_count",sortOrder="asc")}}
            compose.onNodeWithText("最少字数").performTextInput("300000")
            compose.onNodeWithText("最多字数").performTextInput("500000")
            compose.onNodeWithText("确定").performClick()
            compose.runOnIdle{assertEquals("300000..500000",model.state.options.wordCountRange)}
            requestEvidence("word-count-confirm-300000-500000"){books->assertTrue(books.all{it.wordCount?.let{words->words in 300000L..500000L}==true})}
            compose.onNodeWithText("重置",substring=false).performClick()
            compose.runOnIdle{assertEquals("",model.state.options.wordCountRange)}
        } finally {
            File(app.getExternalFilesDir(null),"feedback-search-controls.json").writeText(results.toString(2))
            compose.runOnIdle{model.close()};store.clear()
        }
    }
}
