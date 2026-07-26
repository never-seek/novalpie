package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiNavigationTest {
    @Test
    fun bottomTabsMatchForumReaderProductStructure() {
        assertEquals(
            listOf(BottomTab.Collection, BottomTab.Discover, BottomTab.Tools, BottomTab.Forum, BottomTab.Profile),
            BottomTab.values().toList()
        )
    }

    @Test
    fun bottomTabEnumTitlesAreCleanProductLabels() {
        assertEquals(listOf("收藏", "搜索", "工具", "论坛", "我的"), BottomTab.values().map { it.title })
    }

    @Test
    fun bottomTabLabelsAreCleanChineseProductLabels() {
        assertEquals("收藏", bottomTabDisplayLabel(BottomTab.Collection))
        assertEquals("搜索", bottomTabDisplayLabel(BottomTab.Discover))
        assertEquals("工具", bottomTabDisplayLabel(BottomTab.Tools))
        assertEquals("论坛", bottomTabDisplayLabel(BottomTab.Forum))
        assertEquals("我的", bottomTabDisplayLabel(BottomTab.Profile))
    }

    @Test
    fun bottomTabShortLabelsAreSingleCleanChineseCharacters() {
        assertEquals("收", bottomTabShortLabel(BottomTab.Collection))
        assertEquals("搜", bottomTabShortLabel(BottomTab.Discover))
        assertEquals("工", bottomTabShortLabel(BottomTab.Tools))
        assertEquals("论", bottomTabShortLabel(BottomTab.Forum))
        assertEquals("我", bottomTabShortLabel(BottomTab.Profile))
    }

    @Test
    fun messageRoutesUseSpecificProductContextLabels() {
        assertEquals("消息中心", routeContextLabel(AppRoute.MessageCenter, BottomTab.Tools))
        assertEquals("消息详情", routeContextLabel(AppRoute.MessageDetail(7), BottomTab.Tools))
        assertEquals("私信", routeContextLabel(AppRoute.MessageConversation(20, "Alice"), BottomTab.Tools))
        assertEquals("消息设置", routeContextLabel(AppRoute.MessageSettings, BottomTab.Tools))
        assertEquals("工作区", routeContextLabel(AppRoute.Workspace, BottomTab.Tools))
        assertEquals("上传书籍", routeContextLabel(AppRoute.UploadBook, BottomTab.Tools))
        assertEquals("EPUB 编辑器", routeContextLabel(AppRoute.UploadEditor, BottomTab.Tools))
        assertEquals("收藏", routeContextLabel(AppRoute.Home, BottomTab.Collection))
        assertEquals("帖子详情", routeContextLabel(AppRoute.ForumPostDetail(7), BottomTab.Forum))
        assertEquals("书籍详情", routeContextLabel(AppRoute.BookDetail(354491), BottomTab.Discover))
        assertEquals("阅读", routeContextLabel(AppRoute.Reader(354491, 8001), BottomTab.Collection))
    }
}
