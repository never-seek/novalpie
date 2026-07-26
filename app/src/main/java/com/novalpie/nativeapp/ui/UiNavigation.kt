package com.novalpie.nativeapp.ui

internal fun bottomTabDisplayLabel(tab: BottomTab): String = when (tab) {
    BottomTab.Collection -> "收藏"
    BottomTab.Discover -> "搜索"
    BottomTab.Tools -> "工具"
    BottomTab.Forum -> "论坛"
    BottomTab.Profile -> "我的"
}

internal fun bottomTabShortLabel(tab: BottomTab): String = when (tab) {
    BottomTab.Collection -> "收"
    BottomTab.Discover -> "搜"
    BottomTab.Tools -> "工"
    BottomTab.Forum -> "论"
    BottomTab.Profile -> "我"
}

internal fun routeContextLabel(route: AppRoute, fallbackTab: BottomTab): String = when (route) {
    AppRoute.MessageCenter -> "消息中心"
    is AppRoute.MessageDetail -> "消息详情"
    is AppRoute.MessageConversation -> "私信"
    AppRoute.MessageSettings -> "消息设置"
    AppRoute.Workspace -> "工作区"
    AppRoute.UploadBook -> "上传书籍"
    AppRoute.UploadEditor -> "EPUB 编辑器"
    AppRoute.ForumCreate -> "发布帖子"
    is AppRoute.ForumPostDetail -> "帖子详情"
    is AppRoute.BookDetail -> "书籍详情"
    is AppRoute.BookEditInfo -> "编辑书籍信息"
    is AppRoute.BookChapters -> "章节管理"
    is AppRoute.BookAppend -> "追加章节"
    is AppRoute.Reader -> "阅读"
    AppRoute.Settings -> "应用设置"
    is AppRoute.UserProfileDetail -> "用户主页"
    is AppRoute.Admin -> "管理后台"
    else -> bottomTabDisplayLabel(fallbackTab)
}
