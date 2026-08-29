package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.LoadResult

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

/** Root routes own their tab selection; child pages preserve it until Back reaches the root. */
internal fun rootRouteTab(route: AppRoute): BottomTab? = when (route) {
    AppRoute.Home -> BottomTab.Collection
    AppRoute.Search -> BottomTab.Discover
    AppRoute.Tools -> BottomTab.Tools
    AppRoute.Forum -> BottomTab.Forum
    AppRoute.Profile -> BottomTab.Profile
    else -> null
}

/**
 * An admin route is only valid while the current account still has the exact website admin role.
 * If that access disappears, keep the user inside the native tools surface but drop every stale
 * admin child route so previously loaded management data cannot remain visible after logout or a
 * role refresh.
 */
internal fun sanitizeAdminRouteStack(
    routes: List<AppRoute>,
    isAdmin: Boolean,
): List<AppRoute> = if (
    isAdmin || routes.none { it is AppRoute.Admin }
) {
    routes
} else {
    listOf(AppRoute.Tools)
}

/**
 * A completed `/users/me` response is authoritative for the active account's role.  Route
 * cleanup must therefore follow that response rather than a possibly stale JWT payload or an
 * earlier profile snapshot.
 */
internal fun sanitizeAdminRouteStackForAuthoritativeProfile(
    routes: List<AppRoute>,
    profile: UserProfile,
): List<AppRoute> = sanitizeAdminRouteStack(
    routes = routes,
    isAdmin = isAdminProfile(profile),
)

/**
 * Select the account identity used to gate the tools surface.
 *
 * The profile tab and the collection shelf load `/users/me` independently. During a fast tab
 * switch one result can be newer than the other, so the profile-tab result wins when it belongs to
 * the current token identity. A stale profile from a previous account is ignored; the shelf result
 * or the token identity is then the safe fallback.
 */
internal fun effectiveToolsUserProfile(
    profile: LoadResult<UserProfile>,
    shelfUser: LoadResult<UserProfile>,
    tokenProfile: UserProfile?,
): UserProfile? {
    fun belongsToCurrentAccount(candidate: UserProfile): Boolean {
        val tokenId = tokenProfile?.id ?: return true
        val candidateId = candidate.id ?: return false
        return candidateId == tokenId
    }

    return listOf(profile, shelfUser)
        .asSequence()
        .mapNotNull { (it as? LoadResult.Success)?.value }
        .firstOrNull(::belongsToCurrentAccount)
        ?: tokenProfile
}

internal fun routeContextLabel(route: AppRoute, fallbackTab: BottomTab): String = when (route) {
    AppRoute.PoliticalExam -> "考试"
    is AppRoute.Auth -> route.page.title
    AppRoute.AuthCaptcha -> "安全验证"
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
    is AppRoute.Terminology -> "术语表"
    is AppRoute.BookEditInfo -> "编辑书籍信息"
    is AppRoute.BookChapters -> "章节管理"
    is AppRoute.BookAppend -> "追加章节"
    is AppRoute.Reader -> "阅读"
    AppRoute.Settings -> "应用设置"
    is AppRoute.UserProfileDetail -> "用户主页"
    is AppRoute.Admin -> "管理后台"
    else -> bottomTabDisplayLabel(fallbackTab)
}

/**
 * Maps a current NovalPie website path to the native destination which owns the same page.
 *
 * The parser deliberately receives only a path, not an Android [android.net.Uri], so the route
 * contract can be covered by ordinary JVM tests. Authentication and any network loading happen
 * in [NovalPieViewModel] after this pure mapping step. Unknown paths stay null so callers can use
 * the explicit WebView fallback rather than silently inventing a native destination.
 */
internal fun nativeWebsiteRoute(path: String, isAdmin: Boolean): AppRoute? {
    val segments = path
        .substringBefore('?')
        .trim()
        .trim('/')
        .split('/')
        .filter(String::isNotBlank)
        .map(String::lowercase)

    fun positiveId(index: Int): Long? = segments.getOrNull(index)?.toLongOrNull()?.takeIf { it > 0 }

    if (segments.isEmpty()) return AppRoute.Home

    return when (segments.first()) {
        "home", "favorites" -> if (segments.size == 1) AppRoute.Home else null
        "search" -> if (segments.size == 1) AppRoute.Search else null
        "tools" -> if (segments.size == 1) AppRoute.Tools else null
        "settings" -> if (segments.size == 1) AppRoute.Settings else null
        "login" -> if (segments.size == 1) AppRoute.Auth(AuthPage.Login) else null
        "register" -> if (segments.size == 1) AppRoute.Auth(AuthPage.Register) else null
        "reset-password" -> if (segments.size == 1) AppRoute.Auth(AuthPage.ResetPassword) else null
        "forum" -> when {
            segments.size == 1 -> AppRoute.Forum
            segments.size == 2 && segments[1] == "create" -> AppRoute.ForumCreate
            segments.size == 2 -> positiveId(1)?.let(AppRoute::ForumPostDetail)
            else -> null
        }
        // Older notifications still point to /posts/{id}; the current website uses /forum/{id}.
        "posts" -> positiveId(1)?.takeIf { segments.size == 2 }?.let(AppRoute::ForumPostDetail)
        "user" -> when {
            segments.size == 1 -> AppRoute.Profile
            segments.size == 2 -> positiveId(1)?.let(AppRoute::UserProfileDetail)
            else -> null
        }
        "messages" -> when {
            segments.size == 1 -> AppRoute.MessageCenter
            segments.size == 2 -> positiveId(1)?.let(AppRoute::MessageDetail)
            else -> null
        }
        "workspace" -> if (segments.size == 1) AppRoute.Workspace else null
        "upload" -> if (segments.size == 1) AppRoute.UploadBook else null
        "upload-editor" -> if (segments.size == 1) AppRoute.UploadEditor else null
        "political-exam" -> if (segments.size == 1) AppRoute.PoliticalExam else null
        // The live `/reader` route is only a redirect shell. Query parameters are resolved by
        // [readerLandingRoute] before this path mapper runs; a bare route redirects home.
        "reader" -> if (segments.size == 1) AppRoute.Home else null
        "book-detail" -> positiveId(1)?.takeIf { segments.size == 2 }?.let(AppRoute::BookDetail)
        "book" -> when (segments.size) {
            2 -> positiveId(1)?.let(AppRoute::BookDetail)
            3 -> {
                val bookId = positiveId(1)
                val chapterId = positiveId(2)
                if (bookId != null && chapterId != null) AppRoute.Reader(bookId, chapterId) else null
            }
            else -> null
        }
        "book-edit" -> when (segments.getOrNull(1)) {
            "info" -> positiveId(2)?.takeIf { segments.size == 3 }?.let(AppRoute::BookEditInfo)
            "chapters" -> positiveId(2)?.takeIf { segments.size == 3 }?.let(AppRoute::BookChapters)
            "append" -> positiveId(2)?.takeIf { segments.size == 3 }?.let(AppRoute::BookAppend)
            else -> null
        }
        "admin" -> if (!isAdmin) {
            null
        } else {
            when (segments.drop(1)) {
                emptyList<String>() -> AppRoute.Admin(AdminSection.Overview)
                listOf("review") -> AppRoute.Admin(AdminSection.Review)
                listOf("key-management") -> AppRoute.Admin(AdminSection.Keys)
                listOf("operation-logs") -> AppRoute.Admin(AdminSection.OperationLogs)
                listOf("scraper-management") -> AppRoute.Admin(AdminSection.Scraper)
                listOf("shop") -> AppRoute.Admin(AdminSection.Shop)
                else -> null
            }
        }
        else -> null
    }
}

/** Mirrors the live `/reader?novel=<id>&chapter=<id>` redirect without opening a WebView. */
internal fun readerLandingRoute(novelId: String?, chapterId: String?): AppRoute {
    val novel = novelId?.trim()?.toLongOrNull()?.takeIf { it > 0 }
    val chapter = chapterId?.trim()?.toLongOrNull()?.takeIf { it > 0 }
    return if (novel != null && chapter != null) AppRoute.Reader(novel, chapter) else AppRoute.Home
}
