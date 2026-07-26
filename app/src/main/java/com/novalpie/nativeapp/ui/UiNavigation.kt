package com.novalpie.nativeapp.ui

internal fun bottomTabDisplayLabel(tab: BottomTab): String = when (tab) {
    BottomTab.Collection -> "\u6536\u85cf"
    BottomTab.Discover -> "\u641c\u7d22"
    BottomTab.Tools -> "\u5de5\u5177"
    BottomTab.Forum -> "\u8bba\u575b"
    BottomTab.Profile -> "\u6211\u7684"
}

internal fun bottomTabShortLabel(tab: BottomTab): String = when (tab) {
    BottomTab.Collection -> "\u6536"
    BottomTab.Discover -> "\u641c"
    BottomTab.Tools -> "\u5de5"
    BottomTab.Forum -> "\u8bba"
    BottomTab.Profile -> "\u6211"
}

internal fun routeContextLabel(route: AppRoute, fallbackTab: BottomTab): String = when (route) {
    AppRoute.MessageCenter -> "\u6d88\u606f\u4e2d\u5fc3"
    is AppRoute.MessageDetail -> "\u6d88\u606f\u8be6\u60c5"
    is AppRoute.MessageConversation -> "\u79c1\u4fe1"
    AppRoute.MessageSettings -> "\u6d88\u606f\u8bbe\u7f6e"
    AppRoute.Workspace -> "\u5de5\u4f5c\u533a"
    AppRoute.UploadBook -> "\u4e0a\u4f20\u4e66\u7c4d"
    AppRoute.UploadEditor -> "EPUB \u7f16\u8f91\u5668"
    AppRoute.ForumCreate -> "\u53d1\u5e03\u5e16\u5b50"
    is AppRoute.ForumPostDetail -> "\u5e16\u5b50\u8be6\u60c5"
    is AppRoute.BookDetail -> "\u4e66\u7c4d\u8be6\u60c5"
    is AppRoute.BookEditInfo -> "\u7f16\u8f91\u4e66\u7c4d\u4fe1\u606f"
    is AppRoute.BookChapters -> "\u7ae0\u8282\u7ba1\u7406"
    is AppRoute.BookAppend -> "\u8ffd\u52a0\u7ae0\u8282"
    is AppRoute.Reader -> "\u9605\u8bfb"
    AppRoute.Settings -> "\u5e94\u7528\u8bbe\u7f6e"
    is AppRoute.UserProfileDetail -> "\u7528\u6237\u4e3b\u9875"
    is AppRoute.Admin -> "\u7ba1\u7406\u540e\u53f0"
    else -> bottomTabDisplayLabel(fallbackTab)
}
