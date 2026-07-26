# NovalPie native-android — Routes & Navigation Inventory

Scope: everything needed to rebuild the app's routing/navigation layer byte-for-behaviour without
reading the god-files. All line numbers refer to the repo state at inventory time.

Primary files:

| File | Lines | Role |
|---|---|---|
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt` | 4063 | `BottomTab`, `AppRoute`, `routes` back-stack, all `open*` navigators, `goBack`, `openDeepLink` |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt` | 3654 | `NovalPieApp` Scaffold, `BackHandler`, topBar/bottomBar, the 24-branch route dispatch `when` (182-578), and 6 inline screen composables |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/RouteStackPolicy.kt` | 15 | `pushDistinctRoute`, `replaceTopReaderRoute` |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/UiNavigation.kt` | 38 | `bottomTabDisplayLabel`, `bottomTabShortLabel`, `routeContextLabel` |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt` | — | `globalProductTopBarVisible` (line 30-31) |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/RequestFreshness.kt` | 49 | route-aware staleness guards `isFreshBookDetailResult`, `isFreshReaderResult` |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/ToolsPresentation.kt` | — | `ToolEntry` / `toolsEntries(isAdmin)` — the website-path list the Tools tab dispatches on |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/WebFallbackScreen.kt` | 172 | the WebView host for `AppRoute.WebFallback` |
| `D:/NovalPie/native-android/app/src/main/AndroidManifest.xml` | 30 | `novalpie://app` deep-link intent filter |
| `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/MainActivity.kt` | 26 | reads `intent.data`, passes as `startUri` |

There is **no Jetpack Navigation / NavHost**. Navigation is a hand-rolled
`mutableStateListOf<AppRoute>` stack inside the ViewModel plus one exhaustive `when` in the
composable. `AppRoute` has **24 entries**; the dispatch `when` has **24 branches** — it is
exhaustive with no `else`, so adding a route is a compile error until it is dispatched.

---

## 1. `AppRoute` sealed class

`NovalPieViewModel.kt:104-129`

```kotlin
sealed class AppRoute {
    object Forum : AppRoute()                                                       // :105
    object Home : AppRoute()                                                        // :106
    object Search : AppRoute()                                                      // :107
    object Tools : AppRoute()                                                       // :108
    object Profile : AppRoute()                                                     // :109
    object Settings : AppRoute()                                                    // :110
    object MessageCenter : AppRoute()                                               // :111
    object MessageSettings : AppRoute()                                             // :112
    object Workspace : AppRoute()                                                   // :113
    object UploadBook : AppRoute()                                                  // :114
    object UploadEditor : AppRoute()                                                // :115
    object PoliticalExam : AppRoute()                                               // :116
    data class MessageDetail(val messageId: Long) : AppRoute()                      // :117
    data class MessageConversation(val targetUserId: Long, val targetName: String?) // :118
    data class ForumPostDetail(val postId: Long) : AppRoute()                       // :119
    object ForumCreate : AppRoute()                                                 // :120
    data class BookDetail(val bookId: Long) : AppRoute()                            // :121
    data class BookEditInfo(val bookId: Long) : AppRoute()                          // :122
    data class BookChapters(val bookId: Long) : AppRoute()                          // :123
    data class BookAppend(val bookId: Long) : AppRoute()                            // :124
    data class Reader(val bookId: Long, val chapterId: Long) : AppRoute()           // :125
    data class UserProfileDetail(val userId: Long) : AppRoute()                     // :126
    data class Admin(val section: AdminSection) : AppRoute()                        // :127
    data class WebFallback(val url: String) : AppRoute()                            // :128
}
```

Full table (12 `object` singletons, 12 `data class` parameterised routes):

| # | Route | Kind | Parameters (name: type) | Pushed by (ViewModel fn : line) |
|---|---|---|---|---|
| 1 | `Forum` | object | — | `openTab(BottomTab.Forum)` :701-729 (stack reset) |
| 2 | `Home` | object | — | initial value :450; `openTab(Collection)`; `continueReading` :3691-3692; `openDeepLink` :3731-3732 |
| 3 | `Search` | object | — | `openTab(BottomTab.Discover)` |
| 4 | `Tools` | object | — | `openTab(BottomTab.Tools)` |
| 5 | `Profile` | object | — | `openTab(BottomTab.Profile)`; `openUserProfile(self)` :934-936 |
| 6 | `Settings` | object | — | `openSettings()` :731-733 |
| 7 | `MessageCenter` | object | — | `openMessageCenter()` :1272-1275 |
| 8 | `MessageSettings` | object | — | `openMessageSettings()` :1555-1558 |
| 9 | `Workspace` | object | — | `openWorkspace()` :2479-2482 |
| 10 | `UploadBook` | object | — | `openUploadBook()` :1665-1668; `sendEditorToUpload` :2247-2251 (when no append id) |
| 11 | `UploadEditor` | object | — | `openUploadEditor()` :1836-1849 |
| 12 | `PoliticalExam` | object | — | `openPoliticalExam()` :2267-2270 |
| 13 | `MessageDetail` | data class | `messageId: Long` | `openMessageDetail(messageId)` :1412-1416 (via `openMessage` :1397-1410) |
| 14 | `MessageConversation` | data class | `targetUserId: Long`, `targetName: String?` | `openMessageConversation(...)` :1493-1498 |
| 15 | `ForumPostDetail` | data class | `postId: Long` | `openForumPost(postId)` :2702-2709; `submitForumPost` success :2775-2780 |
| 16 | `ForumCreate` | object | — | `openForumCreate()` :2711-2726 |
| 17 | `BookDetail` | data class | `bookId: Long` | `openBook(bookId)` :3670-3677; `continueReading` :3693; `openDeepLink` :3735 |
| 18 | `BookEditInfo` | data class | `bookId: Long` | `openBookEditInfo(bookId)` :3152-3160 |
| 19 | `BookChapters` | data class | `bookId: Long` | `openBookChapters(bookId)` :3352-3360 |
| 20 | `BookAppend` | data class | `bookId: Long` | `openBookAppend(bookId)` :3659-3668; `sendEditorToUpload` :2247-2251 (when append id found) |
| 21 | `Reader` | data class | `bookId: Long`, `chapterId: Long` | `openReader(bookId, chapterId)` :3679-3687; `continueReading` :3695; `openDeepLink` :3737 |
| 22 | `UserProfileDetail` | data class | `userId: Long` | `openUserProfile(userId)` :930-942 (non-self branch) |
| 23 | `Admin` | data class | `section: AdminSection` | `openAdminSection(section)` :1001-1005 (gated by `isAdminProfile`) |
| 24 | `WebFallback` | data class | `url: String` | `openWebFallback(url)` :3705-3707; `openLoginFallback()` :3709-3711 |

### `AdminSection` (the only enum embedded in a route)

`NovalPieViewModel.kt:177-184`

```kotlin
enum class AdminSection(val websitePath: String) {
    Overview("/admin"),
    Review("/admin/review"),
    Keys("/admin/key-management"),
    OperationLogs("/admin/operation-logs"),
    Scraper("/admin/scraper-management"),
    Shop("/admin/shop")
}
```

`websitePath` is load-bearing: `ToolsScreen.onOpenRoute` reverse-looks-up a tool card's path
against `AdminSection.values()` to decide native-admin vs. web fallback (`NovalPieApp.kt:279-282`).

### Route params that are NOT in the route

Screens read most of their data from ViewModel state objects, not route params. The route param is
only used for (a) identity/`onRetry` re-fetch closures, (b) building the `onOpenWeb` URL, and
(c) freshness checks. Notable: `MessageConversation.targetName` is the *only* non-id, human-readable
payload carried in a route.

---

## 2. `BottomTab`

`NovalPieViewModel.kt:96-102`

```kotlin
enum class BottomTab(val title: String) {
    Collection("收藏"),
    Discover("搜索"),
    Tools("工具"),
    Forum("论坛"),
    Profile("我的")
}
```

Declaration order **is** display order (`BottomTab.values().forEach` at `NovalPieApp.kt:157`).

| Order | Enum | `title` (enum ctor) | `bottomTabDisplayLabel` | `bottomTabShortLabel` | Icon (`bottomTabIcon`, NovalPieApp.kt:583-589) | Route (`openTab`, :702-708) |
|---|---|---|---|---|---|---|
| 1 | `Collection` | 收藏 | 收藏 | 收 | `Icons.Filled.Favorite` | `AppRoute.Home` |
| 2 | `Discover` | 搜索 | 搜索 | 搜 | `Icons.Filled.Search` | `AppRoute.Search` |
| 3 | `Tools` | 工具 | 工具 | 工 | `Icons.Filled.GridView` | `AppRoute.Tools` |
| 4 | `Forum` | 论坛 | 论坛 | 论 | `Icons.Filled.Forum` | `AppRoute.Forum` |
| 5 | `Profile` | 我的 | 我的 | 我 | `Icons.Filled.Person` | `AppRoute.Profile` |

Note the naming skew that must be preserved: `Collection` → 收藏 → renders `AppRoute.Home`;
`Discover` → 搜索 → renders `AppRoute.Search`. The enum name, the Chinese label, and the route name
disagree three ways. `BottomTab.title` is **duplicated** by `bottomTabDisplayLabel` — both exist and
both are asserted in tests (`app/src/test/java/com/novalpie/nativeapp/ui/UiNavigationTest.kt`
`bottomTabEnumTitlesAreCleanProductLabels` and `bottomTabLabelsAreCleanChineseProductLabels`).
`bottomTabShortLabel` is defined and unit-tested but has **no production call site** — dead but
contract-tested.

`NavigationBarItem` wiring (`NovalPieApp.kt:158-170`):
- `selected = viewModel.currentTab == tab`
- `onClick = { viewModel.openTab(tab) }`
- `icon = Icon(bottomTabIcon(tab), contentDescription = bottomTabDisplayLabel(tab))`
- `label = Text(bottomTabDisplayLabel(tab), maxLines = 1, overflow = TextOverflow.Ellipsis)`
- colors: selectedIcon/selectedText = `colorScheme.primary`; unselectedIcon/unselectedText =
  `colorScheme.secondary`; indicator = `colorScheme.primaryContainer`
- `NavigationBar(containerColor = colorScheme.surface, tonalElevation = 0.dp)`

---

## 3. Route dispatch `when` — `NovalPieApp.kt:182-578`

Container: `Scaffold { padding -> Surface(Modifier.fillMaxSize().padding(padding), color = colorScheme.background) { when (route) { ... } } }`
(`NovalPieApp.kt:175-181`).

`route = viewModel.currentRoute` captured once at `NovalPieApp.kt:117`.

Screen composable locations:

| Screen | Defined in |
|---|---|
| `ForumScreen` | `NovalPieApp.kt:593` (private) |
| `ForumPostDetailScreen` | `NovalPieApp.kt:818` (private) |
| `HomeScreen` | `NovalPieApp.kt:1208` (private) |
| `SearchScreen` | `NovalPieApp.kt:1329` (private) |
| `BookDetailScreen` | `NovalPieApp.kt:1727` (private) |
| `ReaderScreen` | `NovalPieApp.kt:1824` (private) |
| `ToolsScreen` | `NovalPieApp.kt:2223` (private) |
| `SettingsScreen` | `NovalPieApp.kt:2429` (private) |
| `ForumCreateScreen` | `ForumCreateScreens.kt:43` (internal) |
| `MessageCenterScreen` | `MessageScreens.kt:76` |
| `MessageDetailScreen` | `MessageScreens.kt:374` |
| `MessageConversationScreen` | `MessageScreens.kt:446` |
| `MessageSettingsScreen` | `MessageScreens.kt:522` |
| `AdminScreen` | `AdminScreens.kt:55` |
| `WorkspaceScreen` | `WorkspaceScreens.kt:76` |
| `UploadBookScreen` | `UploadScreens.kt:56` (public) |
| `UploadEditorScreen` | `UploadEditorScreens.kt:73` (public) |
| `PoliticalExamScreen` | `PoliticalExamScreens.kt:43` (public) |
| `ProfileScreen` | `ProfileScreens.kt:55` |
| `UserProfileDetailScreen` | `UserProfileScreens.kt:35` |
| `BookEditInfoScreen` | `BookEditScreens.kt:50` |
| `BookChapterManagerScreen` | `BookChapterScreens.kt:62` |
| `WebFallbackScreen` | `WebFallbackScreen.kt:25` (public) |

Every screen is called with **all** of its declared parameters — no parameter has a default value
that the dispatch relies on. So the call-site list below == the composable signature.

### 3.1 `AppRoute.Forum` → `ForumScreen` — :183-192 — 8 params / **6 callbacks**

| Param | Value passed |
|---|---|
| `posts: LoadResult<List<ForumPost>>` | `viewModel.forumState.posts` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `onRefresh: () -> Unit` | `viewModel::loadForum` |
| `onOpenPost: (Long) -> Unit` | `viewModel::openForumPost` |
| `onCreatePost: () -> Unit` | `viewModel::openForumCreate` |
| `onOpenUser: (Long) -> Unit` | `viewModel::openUserProfile` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onOpenWeb: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc") }` |

### 3.2 `AppRoute.ForumCreate` → `ForumCreateScreen` — :194-199 — 4 params / **3 callbacks**

| Param | Value |
|---|---|
| `state: ForumCreateState` | `viewModel.forumCreateState` |
| `onDraftChange: (ForumCreateDraft) -> Unit` | `viewModel::updateForumCreateDraft` |
| `onSubmit: () -> Unit` | `viewModel::submitForumPost` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |

### 3.3 `is AppRoute.ForumPostDetail` → `ForumPostDetailScreen` — :201-218 — 16 params / **15 callbacks**

| Param | Value |
|---|---|
| `state: ForumPostDetailState` | `viewModel.forumPostDetailState` |
| `onRetry: () -> Unit` | `{ viewModel.loadForumPostDetail(route.postId) }` |
| `onDraftChange: (String) -> Unit` | `viewModel::updateForumCommentDraft` |
| `onSubmitComment: () -> Unit` | `viewModel::submitForumComment` |
| `onReplyComment: (ForumComment) -> Unit` | `viewModel::replyToForumComment` |
| `onCancelReply: () -> Unit` | `viewModel::cancelForumReply` |
| `onLike: () -> Unit` | `viewModel::likeForumPost` |
| `onDislike: () -> Unit` | `viewModel::dislikeForumPost` |
| `onEmoji: () -> Unit` | `viewModel::emojiForumPost` |
| `onAward: () -> Unit` | `viewModel::awardForumPost` |
| `onCommentLike: (Long) -> Unit` | `viewModel::likeForumComment` |
| `onCommentDislike: (Long) -> Unit` | `viewModel::dislikeForumComment` |
| `onCommentEmoji: (Long) -> Unit` | `viewModel::emojiForumComment` |
| `onCommentAward: (Long) -> Unit` | `viewModel::awardForumComment` |
| `onOpenUser: (Long) -> Unit` | `viewModel::openUserProfile` |
| `onOpenWeb: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc/posts/${route.postId}") }` |

### 3.4 `AppRoute.Home` → `HomeScreen` — :220-236 — 15 params / **10 callbacks**

| Param | Value |
|---|---|
| `state: HomeState` | `viewModel.homeState` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `readerProgress: ReaderProgress?` | `viewModel.readerProgress` |
| `recentReaderProgresses: List<ReaderProgress>` | `viewModel.recentReaderProgresses` |
| `bookshelfQuery: String` | `viewModel.bookshelfQuery` |
| `onRefresh: () -> Unit` | `viewModel::loadHome` |
| `onBookshelfQueryChange: (String) -> Unit` | `viewModel::updateBookshelfQuery` |
| `onFavoriteGroupSelected: (Long?) -> Unit` | `viewModel::selectFavoriteGroup` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onContinueReading: (ReaderProgress) -> Unit` | `viewModel::continueReading` |
| `onClearReaderProgress: () -> Unit` | `viewModel::clearReaderProgress` |
| `onOpenBook: (Long) -> Unit` | `viewModel::openBook` |
| `onLoadMoreFavorites: () -> Unit` | `viewModel::loadMoreFavorites` |
| `onOpenSearch: () -> Unit` | `{ viewModel.openTab(BottomTab.Discover) }` — **the only in-content tab switch** |
| `onOpenWeb: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc/favorites") }` |

### 3.5 `AppRoute.Search` → `SearchScreen` — :238-262 — 23 params / **16 callbacks**

| Param | Value |
|---|---|
| `keyword: String` | `viewModel.searchKeyword` |
| `searchHistory: List<String>` | `viewModel.searchHistory` |
| `options: SearchOptions` | `viewModel.searchOptions` |
| `results: LoadResult<List<NovelCard>>` | `viewModel.searchResults` |
| `tags: LoadResult<List<NovelTag>>` | `viewModel.searchTags` |
| `onKeywordChange: (String) -> Unit` | `viewModel::updateSearchKeyword` |
| `onUseSearchHistory: (String) -> Unit` | `viewModel::useSearchHistory` |
| `onClearSearchHistory: () -> Unit` | `viewModel::clearSearchHistory` |
| `onUseTag: (String) -> Unit` | `viewModel::useSearchTag` |
| `onRefreshTags: () -> Unit` | `viewModel::loadSearchTags` |
| `onSortByChange: (String) -> Unit` | `viewModel::updateSearchSortBy` |
| `onSortOrderChange: (String) -> Unit` | `viewModel::updateSearchSortOrder` |
| `onScopeChange: (String) -> Unit` | `viewModel::updateSearchScope` |
| `onMatchTypeChange: (String) -> Unit` | `viewModel::updateSearchMatchType` |
| `onAdultFilterChange: (String) -> Unit` | `viewModel::updateSearchAdultFilter` |
| `onSourceChange: (String) -> Unit` | `viewModel::updateSearchSource` |
| `onWordCountRangeChange: (String) -> Unit` | `viewModel::updateSearchWordCountRange` |
| `onSearch: (String?) -> Unit` | `{ submittedKeyword -> viewModel.performSearch(submittedKeyword) }` |
| `searchCanLoadMore: Boolean` | `viewModel.searchCanLoadMore` |
| `searchLoadingMore: Boolean` | `viewModel.searchLoadingMore` |
| `onLoadMore: () -> Unit` | `viewModel::loadMoreSearch` |
| `onOpenBook: (Long) -> Unit` | `viewModel::openBook` |
| `onOpenWeb: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc/search?sort_by=relevance") }` |

Ordering quirk to preserve: `searchCanLoadMore` / `searchLoadingMore` are **data** params sitting in
the middle of the callback block (positions 20-21), between `onSearch` and `onLoadMore`.

### 3.6 `AppRoute.Tools` → `ToolsScreen` — :264-285 — 8 params / **5 callbacks**

| Param | Value |
|---|---|
| `state: ToolsState` | `viewModel.toolsState` |
| `user: LoadResult<UserProfile>` | `viewModel.homeState.user` — note: Tools reads the **Home** user, not `profileState` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `onRefresh: () -> Unit` | `viewModel::loadTools` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onOpenMessages: () -> Unit` | `viewModel::openMessageCenter` |
| `onOpenMessage: (SiteMessage) -> Unit` | `viewModel::openMessage` |
| `onOpenRoute: (String) -> Unit` | inline `when(path)` dispatcher, :272-284 |

`onOpenRoute` body (`NovalPieApp.kt:272-284`) — the app's second, *string-path* router:

```kotlin
when (path) {
    "/workspace"      -> viewModel.openWorkspace()
    "/upload"         -> viewModel.openUploadBook()
    "/upload-editor"  -> viewModel.openUploadEditor()
    "/political-exam" -> viewModel.openPoliticalExam()
    else -> {
        val adminSection = AdminSection.values().firstOrNull { it.websitePath == path }
        if (adminSection != null) viewModel.openAdminSection(adminSection)
        else viewModel.openWebFallback("https://novalpie.cc$path")
    }
}
```

`"/messages"` never reaches `onOpenRoute`: `ToolRouteCard`'s onClick short-circuits it
(`NovalPieApp.kt:2303`): `onClick = { if (entry.path == "/messages") onOpenMessages() else onOpenRoute(entry.path) }`.

Tool cards driving it (`ToolsPresentation.kt:9-49`, decoded):

| path | title | subtitle | adminOnly | Icon (`toolEntryIcon`, NovalPieApp.kt:2419-2426) |
|---|---|---|---|---|
| `/messages` | 消息中心 | 通知、私信与用户互动 | false | `Icons.Filled.Forum` |
| `/workspace` | 工作区 | 翻译接口、Cookie 与服务状态 | false | `Icons.Filled.GridView` |
| `/upload` | 上传书籍 | 导入 EPUB 并提交到网站 | false | `Icons.Filled.OpenInBrowser` |
| `/upload-editor` | 上传编辑器 | 分章、替换、AI 正则与草稿 | false | `Icons.Filled.MenuBook` |
| `/political-exam` | 政治考试 | 网站积分奖励入口 | false | `Icons.Filled.CardGiftcard` |
| `/admin` | 管理后台 | 管理员功能总览 | true | `Icons.Filled.Tune` (else branch) |
| `/admin/review` | 内容审核 | 审核与内容处理 | true | `Icons.Filled.Tune` |
| `/admin/key-management` | 密钥管理 | API 密钥与使用状态 | true | `Icons.Filled.Tune` |
| `/admin/operation-logs` | 操作日志 | 管理操作记录 | true | `Icons.Filled.Tune` |
| `/admin/scraper-management` | 抓取管理 | 抓取器与任务状态 | true | `Icons.Filled.Tune` |
| `/admin/shop` | 商店管理 | 站内商店配置 | true | `Icons.Filled.Tune` |

Non-admin users get only the first 5 (`toolsEntries` returns `core` early, `ToolsPresentation.kt:44`).
Note `/upload-editor`'s card title is 上传编辑器 while `routeContextLabel` calls the same route
`EPUB 编辑器` — two different names for one screen; both must survive the refactor.

### 3.7 `is AppRoute.Admin` → `AdminScreen` — :287-310 — 22 params / **21 callbacks**

| Param | Value |
|---|---|
| `state: AdminState` | `viewModel.adminState` |
| `onRefresh: () -> Unit` | `{ viewModel.loadAdminSection(route.section) }` |
| `onSectionSelected: (AdminSection) -> Unit` | `viewModel::openAdminSection` |
| `onReviewQueryChange: (AdminReviewQuery) -> Unit` | `viewModel::updateAdminReviewQuery` |
| `onApplyReviewQuery: () -> Unit` | `viewModel::applyAdminReviewQuery` |
| `onResetReviewQuery: () -> Unit` | `viewModel::resetAdminReviewQuery` |
| `onOperationLogQueryChange: (AdminOperationLogQuery) -> Unit` | `viewModel::updateAdminOperationLogQuery` |
| `onApplyOperationLogQuery: () -> Unit` | `viewModel::applyAdminOperationLogQuery` |
| `onResetOperationLogQuery: () -> Unit` | `viewModel::resetAdminOperationLogQuery` |
| `onToggleReviewSetting: (String) -> Unit` | `viewModel::toggleAdminReviewSetting` |
| `onReviewAction: (Long, String) -> Unit` | `viewModel::adminReviewAction` |
| `onUpdateKeyStatus: (Long, String) -> Unit` | `viewModel::updateAdminKeyStatus` |
| `onDeleteKey: (Long) -> Unit` | `viewModel::deleteAdminKey` |
| `onSaveCookie: (AdminCookieConfig, String?) -> Unit` | `viewModel::saveAdminCookieConfig` |
| `onToggleCookie: (AdminCookieConfig) -> Unit` | `viewModel::toggleAdminCookieConfig` |
| `onDeleteCookie: (Long) -> Unit` | `viewModel::deleteAdminCookieConfig` |
| `onSaveRule: (AdminBaseUrlRule) -> Unit` | `viewModel::saveAdminBaseUrlRule` |
| `onSetRuleAction: (AdminBaseUrlRule, String) -> Unit` | `viewModel::setAdminBaseUrlRuleAction` |
| `onDeleteRule: (Long) -> Unit` | `viewModel::deleteAdminBaseUrlRule` |
| `onSaveShopItem: (AdminShopItem) -> Unit` | `viewModel::saveAdminShopItem` |
| `onToggleShopItem: (AdminShopItem) -> Unit` | `viewModel::toggleAdminShopItem` |
| `onDeleteShopItem: (Long) -> Unit` | `viewModel::deleteAdminShopItem` |

`AdminScreen` is a single screen with an in-screen section switcher; `onSectionSelected` pushes a
**new** `AppRoute.Admin(section)` (via `openAdminSection`), so switching admin sections grows the
back-stack one entry per switch (each `Admin(sectionX)` differs from the top, so
`pushDistinctRoute` appends). Back-navigating through admin walks the section history.

### 3.8 `AppRoute.MessageCenter` → `MessageCenterScreen` — :312-331 — 18 params / **16 callbacks**

| Param | Value |
|---|---|
| `state: MessageCenterState` | `viewModel.messageCenterState` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onRefresh: () -> Unit` | `viewModel::loadMessageCenter` |
| `onKeywordChange: (String) -> Unit` | `viewModel::updateMessageKeyword` |
| `onSearch: () -> Unit` | `viewModel::applyMessageSearch` |
| `onTypeSelected: (Int?) -> Unit` | `viewModel::selectMessageType` |
| `onReadSelected: (Boolean?) -> Unit` | `viewModel::selectMessageReadFilter` |
| `onPrioritySelected: (Int?) -> Unit` | `viewModel::selectMessagePriority` |
| `onToggleSelected: (Long) -> Unit` | `viewModel::toggleMessageSelected` |
| `onSelectAll: (Boolean) -> Unit` | `viewModel::selectAllVisibleMessages` |
| `onMarkSelectedRead: () -> Unit` | `viewModel::markSelectedMessagesRead` |
| `onDeleteSelected: () -> Unit` | `viewModel::deleteSelectedMessages` |
| `onMarkAllRead: () -> Unit` | `viewModel::markAllMessagesRead` |
| `onToggleStar: (SiteMessage) -> Unit` | `viewModel::toggleMessageStar` |
| `onOpenMessage: (SiteMessage) -> Unit` | `viewModel::openMessage` |
| `onLoadMore: () -> Unit` | `viewModel::loadMoreMessages` |
| `onOpenSettings: () -> Unit` | `viewModel::openMessageSettings` |

### 3.9 `is AppRoute.MessageDetail` → `MessageDetailScreen` — :333-341 — 7 params / **6 callbacks**

| Param | Value |
|---|---|
| `state: MessageDetailState` | `viewModel.messageDetailState` |
| `onRetry: () -> Unit` | `{ viewModel.loadMessageDetail(route.messageId) }` |
| `onMarkRead: () -> Unit` | `viewModel::markCurrentMessageRead` |
| `onToggleStar: () -> Unit` | `viewModel::toggleCurrentMessageStar` |
| `onDelete: () -> Unit` | `viewModel::deleteCurrentMessage` |
| `onOpenAction: (String) -> Unit` | `viewModel::openMessageAction` — **third router** (URL→route, see §5.3) |
| `onOpenConversation: () -> Unit` | `viewModel::openCurrentMessageConversation` |

`deleteCurrentMessage` calls `goBack()` on success (`NovalPieViewModel.kt:1451`) then
`loadMessageCenter()` — the only place a screen action itself pops the stack.

### 3.10 `is AppRoute.MessageConversation` → `MessageConversationScreen` — :343-349 — 5 params / **3 callbacks**

| Param | Value |
|---|---|
| `state: MessageConversationState` | `viewModel.messageConversationState` |
| `currentUserId: Long?` | `(viewModel.homeState.user as? LoadResult.Success)?.value?.id` |
| `onRetry: () -> Unit` | `{ viewModel.loadMessageConversation(route.targetUserId, route.targetName) }` — the only retry that uses **two** route params |
| `onDraftChange: (String) -> Unit` | `viewModel::updateMessageDraft` |
| `onSend: () -> Unit` | `viewModel::sendMessageDraft` |

### 3.11 `AppRoute.MessageSettings` → `MessageSettingsScreen` — :351-356 — 4 params / **3 callbacks**

| Param | Value |
|---|---|
| `state: MessageSettingsState` | `viewModel.messageSettingsState` |
| `onRetry: () -> Unit` | `viewModel::loadMessageSettings` |
| `onDraftChange: ((MessageSettings) -> MessageSettings) -> Unit` | `viewModel::updateMessageSettingsDraft` — higher-order (reducer) callback, unique in the app |
| `onSave: () -> Unit` | `viewModel::saveMessageSettings` |

### 3.12 `AppRoute.Workspace` → `WorkspaceScreen` — :358-371 — 12 params / **11 callbacks**

| Param | Value |
|---|---|
| `state: WorkspaceState` | `viewModel.workspaceState` |
| `onRefresh: () -> Unit` | `viewModel::loadWorkspace` |
| `onTabSelected: (WorkspaceTab) -> Unit` | `viewModel::selectWorkspaceTab` |
| `onSaveApi: (WorkspaceApiDraft) -> Unit` | `viewModel::saveWorkspaceApi` |
| `onDeleteLocalApi: (WorkspaceLocalApiConfig) -> Unit` | `viewModel::deleteWorkspaceLocalApi` |
| `onDeleteServerApi: (WorkspaceApiConfig) -> Unit` | `viewModel::deleteWorkspaceServerApi` |
| `onSaveCookie: (WorkspaceCookieDraft) -> Unit` | `viewModel::saveWorkspaceCookie` |
| `onToggleCookie: (WorkspaceCookieConfig) -> Unit` | `viewModel::toggleWorkspaceCookie` |
| `onDeleteCookie: (WorkspaceCookieConfig) -> Unit` | `viewModel::deleteWorkspaceCookie` |
| `onUpdateJobStatus: (WorkspaceTranslationJob, String) -> Unit` | `viewModel::updateWorkspaceJobStatus` |
| `onDeleteJob: (WorkspaceTranslationJob) -> Unit` | `viewModel::deleteWorkspaceJob` |
| `onOpenUpload: () -> Unit` | `viewModel::openUploadBook` |

`WorkspaceTab` is an in-screen tab (state only), not a route.

### 3.13 `AppRoute.UploadBook` → `UploadBookScreen` — :373-383 — 9 params / **7 callbacks**

| Param | Value |
|---|---|
| `state: UploadBookState` | `viewModel.uploadBookState` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onPickEpub: (String) -> Unit` | `viewModel::selectUploadEpub` |
| `onDraftChange: (UploadBookDraft) -> Unit` | `viewModel::updateUploadBookDraft` |
| `onSubmit: () -> Unit` | `viewModel::submitUploadBook` |
| `onClear: () -> Unit` | `viewModel::clearUploadBook` |
| `onOpenEditor: () -> Unit` | `viewModel::openUploadEditor` |
| `onOpenBook: (Long) -> Unit` | `viewModel::openUploadedBook` |

### 3.14 `AppRoute.UploadEditor` → `UploadEditorScreen` — :385-417 — 31 params / **30 callbacks** (largest screen)

| Param | Value |
|---|---|
| `state: UploadEditorState` | `viewModel.uploadEditorState` |
| `onTabSelected: (EditorTab) -> Unit` | `viewModel::selectEditorTab` |
| `onOpenDocument: (String) -> Unit` | `viewModel::selectEditorDocument` |
| `onEncodingChange: (String) -> Unit` | `viewModel::updateEditorEncoding` |
| `onTextChange: (String) -> Unit` | `viewModel::updateEditorText` |
| `onMetadataChange: (EditorBookMetadata) -> Unit` | `viewModel::updateEditorMetadata` |
| `onSplitModeChange: (EditorSplitMode) -> Unit` | `viewModel::updateEditorSplitMode` |
| `onSplitPatternChange: (String) -> Unit` | `viewModel::updateEditorSplitPattern` |
| `onSplitTargetChange: (String) -> Unit` | `viewModel::updateEditorSplitTarget` |
| `onCustomScriptChange: (String) -> Unit` | `viewModel::updateEditorCustomScript` |
| `onScriptChunkedChange: (Boolean) -> Unit` | `viewModel::updateEditorScriptChunked` |
| `onScriptChunkSizeChange: (String) -> Unit` | `viewModel::updateEditorScriptChunkSize` |
| `onCustomScriptResult: (Long, String?, String?) -> Unit` | `viewModel::completeEditorCustomScript` |
| `onAiConfigSelected: (Long) -> Unit` | `viewModel::selectEditorAiConfig` |
| `onGenerateAiRegex: () -> Unit` | `viewModel::generateEditorRegexWithAi` |
| `onProcessSplit: () -> Unit` | `viewModel::processEditorSplit` |
| `onFindChange: (String) -> Unit` | `viewModel::updateEditorFind` |
| `onReplaceChange: (String) -> Unit` | `viewModel::updateEditorReplace` |
| `onFindRegexChange: (Boolean) -> Unit` | `viewModel::updateEditorFindUsesRegex` |
| `onReplaceAll: () -> Unit` | `viewModel::replaceEditorText` |
| `onUpdateChapter: (Int, String, String) -> Unit` | `viewModel::updateEditorChapter` |
| `onAddChapter: () -> Unit` | `viewModel::addEditorChapter` |
| `onDeleteChapter: (Int) -> Unit` | `viewModel::deleteEditorChapter` |
| `onArchiveNameChange: (String) -> Unit` | `viewModel::updateEditorArchiveName` |
| `onSaveArchive: () -> Unit` | `viewModel::saveEditorArchive` |
| `onLoadArchive: (String) -> Unit` | `viewModel::loadEditorArchive` |
| `onDeleteArchive: (String) -> Unit` | `viewModel::deleteEditorArchive` |
| `onClearArchives: () -> Unit` | `viewModel::clearEditorArchives` |
| `onExportEpub: (String) -> Unit` | `viewModel::exportEditorEpub` |
| `onSendToUpload: () -> Unit` | `viewModel::sendEditorToUpload` — **stack-rewriting** (see §4.6) |
| `onClear: () -> Unit` | `viewModel::clearUploadEditor` |

### 3.15 `AppRoute.PoliticalExam` → `PoliticalExamScreen` — :419-432 — 12 params / **10 callbacks**

| Param | Value |
|---|---|
| `state: PoliticalExamState` | `viewModel.politicalExamState` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `onStart: () -> Unit` | `viewModel::startPoliticalExam` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onSelectSingle: (Int, Int) -> Unit` | `viewModel::selectPoliticalExamSingle` |
| `onToggleMultiple: (Int, Int) -> Unit` | `viewModel::togglePoliticalExamMultiple` |
| `onSelectTrueFalse: (Int, Boolean) -> Unit` | `viewModel::selectPoliticalExamTrueFalse` |
| `onUpdateBlank: (Int, String) -> Unit` | `viewModel::updatePoliticalExamBlank` |
| `onTick: () -> Unit` | `viewModel::tickPoliticalExamTimer` |
| `onSubmit: () -> Unit` | `viewModel::submitPoliticalExam` |
| `onReset: () -> Unit` | `viewModel::resetPoliticalExam` |
| `onBack: () -> Unit` | `viewModel::goBack` |

**Nested BackHandler**: `PoliticalExamScreens.kt:61`
`BackHandler(enabled = state.phase == PoliticalExamPhase.Active) { confirmExit = true }`.
This is nested *inside* the app-level BackHandler, so while an exam is active, system-back opens
the exit-confirmation dialog instead of popping the route. `onBack = viewModel::goBack` is the
screen's own explicit exit path.

### 3.16 `AppRoute.Profile` → `ProfileScreen` — :434-449 — 14 params / **12 callbacks**

| Param | Value |
|---|---|
| `state: ProfileState` | `viewModel.profileState` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `onRefresh: () -> Unit` | `viewModel::loadProfile` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onNameChange: (String) -> Unit` | `viewModel::updateProfileName` |
| `onBioChange: (String) -> Unit` | `viewModel::updateProfileBio` |
| `onShowCheckinChange: (Boolean) -> Unit` | `viewModel::updateProfileShowCheckin` |
| `onAutoCheckinChange: (Boolean) -> Unit` | `viewModel::updateProfileAutoCheckin` |
| `onAdultBirthYearChange: (String) -> Unit` | `viewModel::updateProfileAdultBirthYear` |
| `onSave: () -> Unit` | `viewModel::saveProfile` |
| `onCheckin: () -> Unit` | `viewModel::checkinCurrentUser` |
| `onVerifyAdult: () -> Unit` | `viewModel::verifyCurrentUserAdult` |
| `onAvatarSelected: (String) -> Unit` | `viewModel::uploadProfileAvatar` |
| `onOpenSettings: () -> Unit` | `viewModel::openSettings` — the only entry point to `AppRoute.Settings` |

### 3.17 `is AppRoute.UserProfileDetail` → `UserProfileDetailScreen` — :451-460 — 8 params / **6 callbacks**

| Param | Value |
|---|---|
| `state: UserProfileDetailState` | `viewModel.userProfileDetailState` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `onRetry: () -> Unit` | `{ viewModel.loadUserProfile(route.userId) }` |
| `onTabSelected: (UserProfileTab) -> Unit` | `viewModel::selectUserProfileTab` |
| `onOpenActivity: (UserActivity) -> Unit` | `viewModel::openUserActivity` — **fourth router** (activity→route, see §5.4) |
| `onOpenBook: (Long) -> Unit` | `viewModel::openBook` |
| `onMessageUser: (Long, String?) -> Unit` | `viewModel::openMessageConversation` |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |

`UserProfileTab` = `Checkin`, `Activities`, `Books` (`NovalPieViewModel.kt:160-164`); default
`Activities` (:174). In-screen state, not a route; **and it survives reloads** — `loadUserProfile`
preserves `selectedTab = userProfileDetailState.selectedTab` (:955).

### 3.18 `AppRoute.Settings` → `SettingsScreen` — :462-480 — 17 params / **9 callbacks**

| Param | Value |
|---|---|
| `user: LoadResult<UserProfile>` | `viewModel.profileState.profile` |
| `hasAuthToken: Boolean` | `!viewModel.authToken.isNullOrBlank()` |
| `readerProgress: ReaderProgress?` | `viewModel.readerProgress` |
| `readerOptions: ReaderUiOptions` | `viewModel.readerUiOptions` |
| `proxyEnabled: Boolean` | `viewModel.proxyEnabled` |
| `proxyHost: String` | `viewModel.proxyHost` |
| `proxyPort: String` | `viewModel.proxyPortText` |
| `proxySummary: String` | `viewModel.proxySettings.summary()` |
| `onRefreshAccount: () -> Unit` | `viewModel::loadHome` (not `loadProfile`) |
| `onOpenLogin: () -> Unit` | `viewModel::openLoginFallback` |
| `onClearToken: () -> Unit` | `viewModel::clearAuthToken` |
| `onProxyEnabledChange: (Boolean) -> Unit` | `viewModel::updateProxyEnabled` |
| `onProxyHostChange: (String) -> Unit` | `viewModel::updateProxyHost` |
| `onProxyPortChange: (String) -> Unit` | `viewModel::updateProxyPort` |
| `onSaveProxy: () -> Unit` | `viewModel::saveProxySettings` |
| `onOpenHomeFallback: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc") }` |
| `onOpenSearchFallback: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc/search?sort_by=relevance") }` |

### 3.19 `is AppRoute.BookDetail` → `BookDetailScreen` — :482-501 — 18 params / **15 callbacks**

| Param | Value |
|---|---|
| `state: BookDetailState` | `viewModel.bookDetailState` |
| `readerProgress: ReaderProgress?` | `viewModel.bookDetailState.readerProgress` (nested, not the top-level `viewModel.readerProgress`) |
| `catalogQuery: String` | `viewModel.bookCatalogQuery` |
| `onCatalogQueryChange: (String) -> Unit` | `viewModel::updateBookCatalogQuery` |
| `onRetry: () -> Unit` | `{ viewModel.loadBookDetail(route.bookId) }` |
| `onOpenReader: (Long, Long) -> Unit` | `viewModel::openReader` |
| `onEditInfo: () -> Unit` | `{ viewModel.openBookEditInfo(route.bookId) }` |
| `onManageChapters: () -> Unit` | `{ viewModel.openBookChapters(route.bookId) }` |
| `onAppendChapters: () -> Unit` | `{ viewModel.openBookAppend(route.bookId) }` |
| `onCommentDraftChange: (String) -> Unit` | `viewModel::updateBookCommentDraft` |
| `onSubmitComment: () -> Unit` | `viewModel::submitBookComment` |
| `onReplyComment: (ChapterComment) -> Unit` | `viewModel::replyToBookComment` |
| `onCancelCommentReply: () -> Unit` | `viewModel::cancelBookCommentReply` |
| `onCommentLike: (ChapterComment) -> Unit` | `viewModel::likeBookComment` |
| `onCommentDislike: (ChapterComment) -> Unit` | `viewModel::dislikeBookComment` |
| `onCommentEmoji: (ChapterComment) -> Unit` | `viewModel::emojiBookComment` |
| `onCommentAward: (ChapterComment) -> Unit` | `viewModel::awardBookComment` |
| `onOpenWeb: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc/book/${route.bookId}") }` |

### 3.20 `is AppRoute.BookEditInfo` → `BookEditInfoScreen` — :503-513 — 9 params / **8 callbacks**

| Param | Value |
|---|---|
| `state: BookEditState` | `viewModel.bookEditState` |
| `onRetry: () -> Unit` | `{ viewModel.loadBookEditInfo(route.bookId) }` |
| `onDraftChange: (BookEditDraft) -> Unit` | `viewModel::updateBookEditDraft` |
| `onCoverSelected: (String) -> Unit` | `viewModel::uploadManagedBookCover` |
| `onAccessPolicyDraftChange: (BookAccessPolicyDraft) -> Unit` | `viewModel::updateBookAccessPolicyDraft` |
| `onSaveAccessPolicy: () -> Unit` | `viewModel::saveManagedBookAccessPolicy` |
| `onTransferIdentifierChange: (String) -> Unit` | `viewModel::updateBookTransferIdentifier` |
| `onTransfer: () -> Unit` | `viewModel::transferManagedBook` |
| `onSave: () -> Unit` | `viewModel::saveManagedBook` |

### 3.21 `is AppRoute.BookChapters` → `BookChapterManagerScreen` — :515-536 — 20 params / **19 callbacks**

| Param | Value |
|---|---|
| `state: BookChapterManagerState` | `viewModel.bookChapterManagerState` |
| `onRetry: () -> Unit` | `{ viewModel.loadManagedChapters(route.bookId) }` |
| `onToggleSelection: (Long) -> Unit` | `viewModel::toggleManagedChapterSelection` |
| `onSelectAll: () -> Unit` | `viewModel::selectAllManagedChapters` |
| `onMove: (Long, Int) -> Unit` | `viewModel::moveManagedChapter` |
| `onSaveOrder: () -> Unit` | `viewModel::saveManagedChapterOrder` |
| `onOpenEditor: (Chapter?) -> Unit` | `viewModel::openManagedChapterEditor` (dialog, not a route) |
| `onUpdateEditor: (ManagedChapterDraft) -> Unit` | `viewModel::updateManagedChapterDraft` |
| `onDismissEditor: () -> Unit` | `viewModel::dismissManagedChapterEditor` |
| `onSaveEditor: () -> Unit` | `viewModel::saveManagedChapterDraft` |
| `onDelete: (Long) -> Unit` | `viewModel::deleteManagedChapter` |
| `onBatchDelete: () -> Unit` | `viewModel::batchDeleteManagedChapters` |
| `onTranslationMode: (String) -> Unit` | `viewModel::updateManagedTranslationMode` |
| `onTranslate: () -> Unit` | `viewModel::translateSelectedManagedChapters` |
| `onOpenIllustrations: (Chapter) -> Unit` | `viewModel::openManagedChapterIllustrations` (dialog) |
| `onDismissIllustrations: () -> Unit` | `viewModel::dismissManagedChapterIllustrations` |
| `onUploadIllustrations: (List<String>) -> Unit` | `viewModel::uploadManagedChapterIllustrations` |
| `onDeleteIllustration: (Long) -> Unit` | `viewModel::deleteManagedChapterIllustration` |
| `onInsertIllustrationPlaceholder: (Int) -> Unit` | `viewModel::insertChapterIllustrationPlaceholder` |
| `onAppend: () -> Unit` | `{ viewModel.openBookAppend(route.bookId) }` |

### 3.22 `is AppRoute.BookAppend` → `UploadBookScreen` — :538-548 — 9 params / **7 callbacks**

**Byte-identical argument list to §3.13 `AppRoute.UploadBook`.** Two route branches, one screen,
one duplicated call site. The append-vs-create distinction lives entirely in
`uploadBookState.existingNovelId`, which `openBookAppend` seeds (`NovalPieViewModel.kt:3666`) and
`clearUploadBook` re-reads from the route (`:1829` —
`UploadBookState(existingNovelId = (currentRoute as? AppRoute.BookAppend)?.bookId)`).
`UploadScreens.kt:177` uses that to swap the warning copy: append →
`追加会写入现有书籍。提交前请确认章节顺序与翻译类型。`, create →
`上传会写入 novalpie.cc。提交前请确认书名、作者、标签、成人内容标记与翻译类型。`

### 3.23 `is AppRoute.Reader` → `ReaderScreen` — :550-570 — 19 params / **16 callbacks**

| Param | Value |
|---|---|
| `state: ReaderState` | `viewModel.readerState` |
| `options: ReaderUiOptions` | `viewModel.readerUiOptions` |
| `catalogQuery: String` | `viewModel.readerCatalogQuery` |
| `onCatalogQueryChange: (String) -> Unit` | `viewModel::updateReaderCatalogQuery` |
| `onDecreaseFont: () -> Unit` | `viewModel::decreaseReaderFont` |
| `onIncreaseFont: () -> Unit` | `viewModel::increaseReaderFont` |
| `onCycleTheme: () -> Unit` | `viewModel::cycleReaderTheme` |
| `onRetry: () -> Unit` | `{ viewModel.loadReader(route.bookId, route.chapterId) }` |
| `onOpenReader: (Long, Long) -> Unit` | `viewModel::openReader` (chapter prev/next — replaces top, see §4.4) |
| `onBack: () -> Unit` | `viewModel::goBack` |
| `onCommentDraftChange: (String) -> Unit` | `viewModel::updateReaderCommentDraft` |
| `onSubmitComment: () -> Unit` | `viewModel::submitReaderComment` |
| `onReplyComment: (ChapterComment) -> Unit` | `viewModel::replyToReaderComment` |
| `onCancelCommentReply: () -> Unit` | `viewModel::cancelReaderCommentReply` |
| `onCommentLike: (ChapterComment) -> Unit` | `viewModel::likeReaderComment` |
| `onCommentDislike: (ChapterComment) -> Unit` | `viewModel::dislikeReaderComment` |
| `onCommentEmoji: (ChapterComment) -> Unit` | `viewModel::emojiReaderComment` |
| `onCommentAward: (ChapterComment) -> Unit` | `viewModel::awardReaderComment` |
| `onOpenWeb: () -> Unit` | `{ viewModel.openWebFallback("https://novalpie.cc/book/${route.bookId}/${route.chapterId}") }` |

Reader owns its own chrome: tapping the body toggles `toolbarsVisible`
(`NovalPieApp.kt:1852-1856`), which slide-animates an internal `ReaderTopBar(state, chapters,
onBack, onOpenWeb)` (`:1885-1893`). Its labels come from `readerTopBarLabels()`
(`ReaderPresentation.kt:27-28`): `back = "返回"`, `title = "阅读"`, `web = "网页"`.

### 3.24 `is AppRoute.WebFallback` → `WebFallbackScreen` — :572-577 — 4 params / **1 callback**

| Param | Value |
|---|---|
| `url: String` | `route.url` |
| `proxySettings: ProxySettings` | `viewModel.proxySettings` |
| `authToken: String?` | `viewModel.authToken` |
| `onAuthTokenCaptured: (String) -> Unit` | `viewModel::saveCapturedAuthToken` |

### Callback-count summary (24 branches)

| Route | Screen | Params | Callbacks |
|---|---|---:|---:|
| `UploadEditor` | `UploadEditorScreen` | 31 | **30** |
| `Admin` | `AdminScreen` | 22 | 21 |
| `BookChapters` | `BookChapterManagerScreen` | 20 | 19 |
| `MessageCenter` | `MessageCenterScreen` | 18 | 16 |
| `Search` | `SearchScreen` | 23 | 16 |
| `Reader` | `ReaderScreen` | 19 | 16 |
| `ForumPostDetail` | `ForumPostDetailScreen` | 16 | 15 |
| `BookDetail` | `BookDetailScreen` | 18 | 15 |
| `Profile` | `ProfileScreen` | 14 | 12 |
| `Workspace` | `WorkspaceScreen` | 12 | 11 |
| `Home` | `HomeScreen` | 15 | 10 |
| `PoliticalExam` | `PoliticalExamScreen` | 12 | 10 |
| `Settings` | `SettingsScreen` | 17 | 9 |
| `BookEditInfo` | `BookEditInfoScreen` | 9 | 8 |
| `UploadBook` | `UploadBookScreen` | 9 | 7 |
| `BookAppend` | `UploadBookScreen` | 9 | 7 |
| `Forum` | `ForumScreen` | 8 | 6 |
| `MessageDetail` | `MessageDetailScreen` | 7 | 6 |
| `UserProfileDetail` | `UserProfileDetailScreen` | 8 | 6 |
| `Tools` | `ToolsScreen` | 8 | 5 |
| `ForumCreate` | `ForumCreateScreen` | 4 | 3 |
| `MessageConversation` | `MessageConversationScreen` | 5 | 3 |
| `MessageSettings` | `MessageSettingsScreen` | 4 | 3 |
| `WebFallback` | `WebFallbackScreen` | 4 | 1 |
| **Totals** | | **312** | **255** |

(Counts are per call site; `UploadBookScreen`'s 9/7 is listed twice because the dispatch passes it
twice.) Distinct screen composables: **23**.

---

## 4. Navigation mechanism

### 4.1 The stack

`NovalPieViewModel.kt:450`

```kotlin
private val routes = mutableStateListOf<AppRoute>(AppRoute.Home)
```

- `private`, so no one outside the ViewModel can read the stack. The only exposure is
  `NovalPieViewModel.kt:550`: `val currentRoute: AppRoute get() = routes.lastOrNull() ?: AppRoute.Home`
- Initial stack = `[Home]`. Initial `currentTab = BottomTab.Collection` (`:469`).
- `init { configureNovalPieImageLoader(...); loadForum(); loadHome() }` (`:552-556`) — Forum data is
  fetched at startup even though the initial route is Home.
- It is a Compose snapshot list, so mutating it recomposes `NovalPieApp`.
- **No `SavedStateHandle`, no process-death restore.** The stack is lost on process death; the app
  restarts at `[Home]`/`Collection`.

### 4.2 `replaceWith` — the write primitive

`NovalPieViewModel.kt:4014-4018` (private extension on `MutableList<AppRoute>`)

```kotlin
private fun MutableList<AppRoute>.replaceWith(next: List<AppRoute>) {
    if (this == next) return
    clear()
    addAll(next)
}
```

Semantics: structural-equality no-op guard, otherwise full clear+refill (so Compose sees one
whole-list change, not incremental diffs). Every push goes through this except three sites that
mutate `routes` directly: `openTab` (`:726-727`), `openUserProfile`-self (`:935-936`),
`continueReading` (`:3691-3695`), `openDeepLink` (`:3731-3737`), `submitForumPost`'s no-postId
branch (`:3782` → `routes.removeAt`), and `goBack` (`:3746`).

### 4.3 `RouteStackPolicy.kt` (whole file, 15 lines)

```kotlin
internal fun pushDistinctRoute(stack: List<AppRoute>, route: AppRoute): List<AppRoute> {
    if (stack.lastOrNull() == route) return stack        // identity-returns the SAME list
    return stack + route
}

internal fun replaceTopReaderRoute(stack: List<AppRoute>, route: AppRoute.Reader): List<AppRoute> {
    if (stack.lastOrNull() == route) return stack
    return if (stack.lastOrNull() is AppRoute.Reader) {
        stack.dropLast(1) + route                       // swap chapter in place
    } else {
        stack + route
    }
}
```

**Duplicate-push semantics**: only the *immediate top* is compared, by `data class` equality. So:
- Tapping the same book twice in a row → no second `BookDetail(354491)` entry (test
  `pushDistinctRouteDoesNotDuplicateCurrentTopRoute`).
- Tapping the same `onOpenWeb` twice → no duplicate `WebFallback(url)` (test
  `webFallbackRouteIsNotDuplicatedOnDoubleTap`).
- `Home → BookDetail(100) → BookDetail(200)` **does** stack two book details (test
  `pushDistinctRouteAddsDifferentDetailRoute`), and `Forum → Post(91) → Post(92)` likewise (test
  `forumPostDetailRouteCanMoveBetweenDifferentPosts`). Deeper duplicates are allowed:
  `A → B → A` produces `[A, B, A]`.
- Because `pushDistinctRoute` returns the **same instance** on a no-op, callers use `===` to detect
  "nothing happened" and skip the network reload: `openBook` (`:3674`), `openReader` (`:3684`),
  `openForumPost` (`:2706`). Other pushers (`openSettings`, `openMessageCenter`,
  `openBookEditInfo`, `openBookChapters`, `openWorkspace`, `openUploadBook`, `openUploadEditor`,
  `openAdminSection`, `openMessageDetail`, `openMessageConversation`, `openMessageSettings`,
  `openUserProfile`, `openPoliticalExam`, `openBookAppend`, `openWebFallback`) do **not** check
  `===` and re-fetch unconditionally.

Reference tests: `app/src/test/java/com/novalpie/nativeapp/ui/RouteStackPolicyTest.kt` (7 tests).

### 4.4 `openReader` — the reader-replacement rule

`NovalPieViewModel.kt:3679-3687`

```kotlin
fun openReader(bookId: Long, chapterId: Long) {
    if (bookId <= 0 || chapterId <= 0) return
    val next = AppRoute.Reader(bookId, chapterId)
    val currentStack = routes.toList()
    val nextStack = replaceTopReaderRoute(currentStack, next)
    if (nextStack === currentStack) return
    routes.replaceWith(nextStack)
    loadReader(bookId, chapterId)
}
```

Effect: chapter-to-chapter reading never grows the stack. From `[Home, BookDetail(b), Reader(b,c1)]`
going to `c2` yields `[Home, BookDetail(b), Reader(b,c2)]` — one back-press lands on the book
detail, not on chapter 1. Note it replaces on **any** `Reader` top, including a different `bookId`
(so an in-reader jump to another book's chapter would also replace, not push).

### 4.5 `openTab` — stack reset + refetch

`NovalPieViewModel.kt:701-729`

```kotlin
fun openTab(tab: BottomTab) {
    val targetRoute = when (tab) { Collection->Home; Discover->Search; Tools->Tools; Forum->Forum; Profile->Profile }
    if (currentTab == tab && currentRoute == targetRoute) {          // re-tap on the tab root
        when (tab) { Collection->loadHome(); Discover->loadSearchTags(); Tools->loadTools(); Forum->loadForum(); Profile->loadProfile() }
        return                                                       // refresh only, stack untouched
    }
    if (tab == BottomTab.Collection) loadHome()
    if (tab == BottomTab.Forum) loadForum()
    if (tab == BottomTab.Discover) loadSearchTags()
    if (tab == BottomTab.Tools) loadTools()
    if (tab == BottomTab.Profile) loadProfile()
    routes.clear()
    routes.add(targetRoute)                                          // FULL RESET to 1 entry
    currentTab = tab
}
```

Exact semantics to preserve:
1. **Re-tap on tab root = pull-to-refresh**, stack untouched, `return` early.
2. **Tapping the tab you are already on but deep in its stack** (e.g. `currentTab == Tools` and
   `currentRoute == Workspace`) does **not** hit the early return — it falls through and
   **destroys the whole back-stack**, replacing it with `[Tools]`. That is the tab's
   "pop-to-root", and it also refetches.
3. **Any tab switch destroys the other tab's stack.** There are no per-tab stacks. Coming back to a
   tab always lands on its root, freshly reloaded.
4. Data loading happens *before* the stack mutation.
5. The five `if` statements (`:720-724`) are not `else if` — exactly one can match, but the shape
   is a series of independent ifs, and they duplicate the `when` in the early-return branch.

### 4.6 Stack rewrites that are not plain push/pop

| Site | File:line | Behaviour |
|---|---|---|
| `openTab` | VM:726-727 | `clear()` + single route (see §4.5) |
| `openUserProfile(self)` | VM:930-942 | If `userId == currentUserProfile()?.id`: `currentTab = Profile`, `routes.clear()`, `routes.add(AppRoute.Profile)`, `loadProfile()`, return. Tapping your own avatar anywhere **wipes the back-stack** and hard-switches tabs. Otherwise push `UserProfileDetail(userId)`. |
| `continueReading(progress)` | VM:3689-3697 | `currentTab = Collection`; `clear()`; add `Home`; add `BookDetail(bookId)`; `loadBookDetail`; add `Reader(bookId, chapterId)`; `loadReader`. Synthesises a 3-deep stack so back works naturally. |
| `openDeepLink` (book) | VM:3730-3741 | Same synthesis; see §5. |
| `submitForumPost` success with postId | VM:2774-2780 | Pops the `ForumCreate` top if present, pushes `ForumPostDetail(postId)` — replace-not-push so back from the new post returns to the forum list, not the composer. |
| `submitForumPost` success without postId | VM:2781-2783 | `if (routes.lastOrNull() is AppRoute.ForumCreate) routes.removeAt(routes.lastIndex)` — bare pop. |
| `sendEditorToUpload` | VM:2222-2251 | Scans `routes.asReversed().filterIsInstance<AppRoute.BookAppend>().firstOrNull()?.bookId` — a **stack search**, the only read of non-top entries. Target = `BookAppend(id)` if found else `UploadBook`. Then drops a top `UploadEditor` and `pushDistinctRoute`s the target. So "send to upload" returns you to the append screen you came from, if any. |
| `deleteCurrentMessage` | VM:1444-1460 | On success `goBack()` then `loadMessageCenter()`. |

### 4.7 `goBack`

`NovalPieViewModel.kt:3744-3748`

```kotlin
fun goBack(): Boolean {
    if (routes.size <= 1) return false
    routes.removeAt(routes.lastIndex)
    return true
}
```

- Pure pop of one entry. **`currentTab` is never touched by `goBack`.**
- Returns `false` at the root; the return value is **ignored at all three call sites**
  (`NovalPieApp.kt:124`, `:139`, and `viewModel::goBack` passed as `onBack` to
  `ReaderScreen`/`PoliticalExamScreen`).
- No re-fetch on pop: popping back to `BookDetail` shows whatever `bookDetailState` currently holds.
  Combined with §4.4 that's fine for reader→detail, but popping from e.g. `BookDetail(200)` to
  `BookDetail(100)` leaves `bookDetailState` describing book 200 — a known consequence of one
  shared per-screen state object (mitigated only by `isFreshBookDetailResult`, which suppresses
  *late* responses, not stale display).

### 4.8 `BackHandler` wiring

`NovalPieApp.kt:123-125`

```kotlin
BackHandler(enabled = route !is AppRoute.Forum && route !is AppRoute.Home &&
                      route !is AppRoute.Search && route !is AppRoute.Tools &&
                      route !is AppRoute.Profile) {
    viewModel.goBack()
}
```

- Disabled on the five tab-root routes, so system-back at a tab root falls through to the Activity
  and exits the app. There is **no** "back returns to the Collection tab" behaviour and no
  double-back-to-exit.
- `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml:8` — predictive back is opted
  in application-wide.
- Nested override: `PoliticalExamScreens.kt:61` (§3.15).
- `MainActivity` (`MainActivity.kt:1-26`) is the single Activity; `screenOrientation="unspecified"`.

### 4.9 Route-aware freshness guards (`RequestFreshness.kt`)

These make the route part of the async-response contract; a refactor that changes route identity
breaks them silently.

```kotlin
internal fun isFreshBookDetailResult(route, state, requestedBookId): Boolean {
    if (state.bookId != requestedBookId) return false
    return when (route) {
        is AppRoute.BookDetail -> route.bookId == requestedBookId
        is AppRoute.Reader     -> route.bookId == requestedBookId   // reader also accepts detail data
        else -> false
    }
}
internal fun isFreshReaderResult(route, state, requestedBookId, requestedChapterId): Boolean =
    state.bookId == requestedBookId && state.chapterId == requestedChapterId &&
    route is AppRoute.Reader && route.bookId == requestedBookId && route.chapterId == requestedChapterId
```

Used at `NovalPieViewModel.kt:3933`, `:3949`, `:3976`.

Additionally, **12 raw `currentRoute != AppRoute.X(...)` equality checks** guard async completions:
`:2762` (`ForumCreate`), `:2808` (`ForumPostDetail(postId)`), `:3176`, `:3210`, `:3239`, `:3290`,
`:3333` (`BookEditInfo(bookId)`), `:3369`, `:3411`, `:3436`, `:3480`, `:3516`, `:3542`, `:3594`,
`:3642` (`BookChapters(bookId)`). Every one relies on `AppRoute` remaining a `data class` with
value equality.

`clearUploadBook` (`:1829`) and `sendEditorToUpload` (`:2223`) **read route data as state** —
`currentRoute as? AppRoute.BookAppend` and a reverse stack scan respectively.

---

## 5. Deep links

### 5.1 Manifest

`AndroidManifest.xml:15-24`

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="novalpie" android:host="app" />
</intent-filter>
```

Only `novalpie://app/...` is registered. `https://novalpie.cc/...` is **not** an app link in the
manifest (no `autoVerify`, no https `<data>`), even though `openDeepLink` accepts it — so https
URIs can only arrive via an explicit in-process call, never from the system.

### 5.2 Entry path

`MainActivity.kt:15` `val startUri = intent?.data?.toString()` → `NovalPieApp(startUri = startUri)`
→ `NovalPieApp.kt:119-121`:

```kotlin
LaunchedEffect(startUri) { if (!startUri.isNullOrBlank()) viewModel.openDeepLink(startUri) }
```

Keyed on `startUri`, so it runs once per value. **`onNewIntent` is not overridden** — a second deep
link into an already-running task does not re-navigate.

### 5.3 `openDeepLink`

`NovalPieViewModel.kt:3713-3742`

```kotlin
fun openDeepLink(rawUri: String) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return
    val isNativeScheme  = uri.scheme == "novalpie" && uri.host == "app"
    val isWebsiteRoute  = uri.scheme in setOf("https", "http") && uri.host == "novalpie.cc"
    if (!isNativeScheme && !isWebsiteRoute) return

    val segments = uri.pathSegments
    if (segments.firstOrNull() == "user") {
        val userId = segments.getOrNull(1)?.toLongOrNull() ?: return
        openUserProfile(userId); return
    }
    if (segments.firstOrNull() != "book") return

    val bookId = segments.getOrNull(1)?.toLongOrNull() ?: return
    val chapterId = segments.getOrNull(2)?.toLongOrNull()

    currentTab = BottomTab.Collection
    routes.clear(); routes.add(AppRoute.Home)
    if (chapterId != null && chapterId > 0) {
        routes.add(AppRoute.BookDetail(bookId)); loadBookDetail(bookId)
        routes.add(AppRoute.Reader(bookId, chapterId)); loadReader(bookId, chapterId)
    } else {
        openBook(bookId)
    }
}
```

URI → route table (only two path families are handled):

| URI | Resulting stack / action |
|---|---|
| `novalpie://app/book/<id>` | `currentTab=Collection`; stack `[Home]` then `openBook(id)` → `[Home, BookDetail(id)]` + `loadBookDetail` |
| `novalpie://app/book/<id>/<chapterId>` (chapterId > 0) | `currentTab=Collection`; `[Home, BookDetail(id), Reader(id, chapterId)]` + `loadBookDetail` + `loadReader` |
| `novalpie://app/book/<id>/0` or non-numeric 3rd segment | chapterId is null/≤0 → falls to `openBook(id)` → `[Home, BookDetail(id)]` |
| `novalpie://app/user/<id>` | `openUserProfile(id)`: if it's your own id → `currentTab=Profile`, stack `[Profile]`; else **push** `UserProfileDetail(id)` onto the *existing* stack (no reset, tab unchanged) |
| `https://novalpie.cc/book/...`, `http://novalpie.cc/book/...`, `.../user/<id>` | Same as above (accepted by `isWebsiteRoute`) but unreachable from the OS given the manifest |
| `novalpie://app/` , `novalpie://app/forum/12`, `novalpie://app/messages`, `/workspace`, `/political-exam`, `/admin*`, `/search`, `/favorites`, `/login`, anything else | **silently `return`** — no navigation, no error, no web fallback |
| any other scheme/host (e.g. `https://example.com/book/1`) | `return` |
| malformed URI (`Uri.parse` throws) | `return` |

Notable gaps to preserve or consciously change: the website has `/forum/:id`, `/messages`,
`/workspace`, `/upload`, `/upload-editor`, `/political-exam`, `/book-detail/:id`,
`/book-edit/{info,append,chapters}/:id`, and 6 `/admin*` routes
(`docs/LIVE_SITE_ROUTE_API_MATRIX.md`) — **none** are deep-linkable. Note also that the site's
canonical post path used for the web fallback is `/posts/<id>` (`NovalPieApp.kt:217`) while
`openMessageAction` accepts **both** `forum` and `posts` as the first segment (`VM:1478`).

### 5.4 The other three routers (non-URI dispatch)

**`openMessageAction(actionUrl: String)`** — `NovalPieViewModel.kt:1469-1491`. Normalises to
absolute (`"https://novalpie.cc/" + actionUrl.trimStart('/')` when not already http/https), then:

| First path segment | Action |
|---|---|
| `forum` or `posts` | `segments[1].toLongOrNull()?.let(::openForumPost)` — **`?:` falls through to `openWebFallback(absolute)` when the id is missing/non-numeric**. (Note: because `openForumPost` returns `Unit`, `let` returns `Unit`, so the elvis only fires when segment[1] is absent/non-numeric — as intended.) |
| `book` with numeric id + numeric chapter | `openReader(bookId, chapterId)` |
| `book` with numeric id only | `openBook(bookId)` |
| `book` with non-numeric id | `openWebFallback(absolute)` |
| anything else | `openWebFallback(absolute)` |

**`openMessage(message: SiteMessage)`** — `NovalPieViewModel.kt:1397-1410`. If `message.type == 8`
(私信, per `messageTypeLabel`) and `directMessageTargetUserId(message, currentUserProfile()?.id)`
resolves, it silently marks the message read (`api.markMessageRead`) and goes to
`MessageConversation(targetUserId, message.username)`; otherwise `openMessageDetail(message.id)`.
`directMessageTargetUserId` (`MessagePresentation.kt:20-24`) = first of
`[message.executeUserId, message.userId]` that is non-null and != currentUserId.

**`openUserActivity(activity: UserActivity)`** — `NovalPieViewModel.kt:992-999`. Priority order:
`postId != null` → `openForumPost`; else `bookId != null && chapterId != null` → `openReader`;
else `bookId != null` → `openBook`; else nothing.

### 5.5 Auth gates that redirect instead of navigating

Several `open*` functions bail to the login WebView instead of pushing their route:

| Function | File:line | Gate |
|---|---|---|
| `openForumCreate` | VM:2711-2715 | `authToken.isNullOrBlank()` → `openLoginFallback()`. Also sets `accessMessage = "游客账号不能发帖，请先升级账号"` when `profile?.role == "guest"` (route still pushed; the screen blocks submit). |
| `openBookEditInfo` | VM:3152-3157 | `authToken.isNullOrBlank()` → `openLoginFallback()` |
| `openBookChapters` | VM:3352-3357 | same |
| `openBookAppend` | VM:3659-3664 | same (then `uploadRequestSerial++` and `uploadBookState = UploadBookState(existingNovelId = bookId)` before the push, VM:3665-3667) |
| `openAdminSection` | VM:1001-1002 | `if (!isAdminProfile(currentUserProfile())) return` — **silent no-op**, not a login redirect |
| `openBook` / `openReader` / `openForumPost` / `openMessageDetail` / `openMessageConversation` / `openUserProfile` / `openUploadedBook` | — | reject id `<= 0` with a silent `return` |

---

## 6. Top bar / bottom bar / BackHandler visibility

### 6.1 The three predicates

| Element | File:line | Condition |
|---|---|---|
| `topBar` (whole `CenterAlignedTopAppBar`) | `NovalPieApp.kt:129` | `globalProductTopBarVisible(route)` = `route !is AppRoute.Reader` (`ReaderPresentation.kt:30-31`) |
| topBar `navigationIcon` (back arrow) | `NovalPieApp.kt:138` | `route !is Forum && !is Home && !is Search && !is Tools && !is Profile` (inline, 5-term) |
| `bottomBar` (`NavigationBar`) | `NovalPieApp.kt:152` | `route is Forum \|\| is Home \|\| is Search \|\| is Tools \|\| is Profile` (inline, 5-term) |
| `BackHandler(enabled=)` | `NovalPieApp.kt:123` | `route !is Forum && !is Home && !is Search && !is Tools && !is Profile` (inline, 5-term — a verbatim copy of the navigationIcon condition) |

Top bar content when visible (`:130-148`): `CenterAlignedTopAppBar` whose title is a centered
`Column` of two `Text`s — `"NovalPie"` (bold) over `routeContextLabel(route, viewModel.currentTab)`
in `typography.labelSmall`. Back icon `Icons.Filled.ArrowBack`, `contentDescription = "返回"`,
`onClick = { viewModel.goBack() }`. Colors: `containerColor` and `scrolledContainerColor` both
`colorScheme.surface`.

### 6.2 Per-route matrix

| Route | Top bar | Back arrow in top bar | Bottom bar | BackHandler enabled |
|---|:--:|:--:|:--:|:--:|
| `Forum` | yes | no | **yes** | no |
| `Home` | yes | no | **yes** | no |
| `Search` | yes | no | **yes** | no |
| `Tools` | yes | no | **yes** | no |
| `Profile` | yes | no | **yes** | no |
| `Settings` | yes | yes | no | yes |
| `MessageCenter` | yes | yes | no | yes |
| `MessageDetail` | yes | yes | no | yes |
| `MessageConversation` | yes | yes | no | yes |
| `MessageSettings` | yes | yes | no | yes |
| `Workspace` | yes | yes | no | yes |
| `UploadBook` | yes | yes | no | yes |
| `UploadEditor` | yes | yes | no | yes |
| `PoliticalExam` | yes | yes | no | yes* |
| `ForumCreate` | yes | yes | no | yes |
| `ForumPostDetail` | yes | yes | no | yes |
| `BookDetail` | yes | yes | no | yes |
| `BookEditInfo` | yes | yes | no | yes |
| `BookChapters` | yes | yes | no | yes |
| `BookAppend` | yes | yes | no | yes |
| `UserProfileDetail` | yes | yes | no | yes |
| `Admin` | yes | yes | no | yes |
| `WebFallback` | yes | yes | no | yes |
| `Reader` | **no** | n/a (bar hidden) | no | yes |

\* PoliticalExam: the app-level handler is enabled, but the screen-level
`BackHandler(enabled = phase == Active)` wins while an exam is running and shows a confirm dialog.

### 6.3 Inconsistencies / smells to carry forward deliberately

1. **The same 5-route predicate is written three times** (`:123` BackHandler, `:138` navigationIcon,
   `:152` bottomBar as the positive form). There is no `isTabRootRoute(route)` helper even though
   `globalProductTopBarVisible` exists for the Reader case. Three copies to keep in sync.
2. **`globalProductTopBarVisible` is asymmetric with the others**: the Reader special case lives in
   a named function in `ReaderPresentation.kt`, the tab-root cases live inline in `NovalPieApp.kt`.
3. **Reader loses the top bar but keeps nothing to replace `routeContextLabel`** — its own
   `ReaderTopBar` is toggled by tapping the body and is hidden by default, so on entering Reader
   there is momentarily no visible back affordance at all except system back.
4. **`WebFallback` gets the native NovalPie top bar over a full web page**, giving two nested
   chromes and two back semantics (app back pops the route; the WebView's own history is
   unreachable — `WebFallbackScreen` never calls `webView.goBack()`, so in-page navigation is a
   one-way trip and app-back exits the whole web session).
5. **`Admin` shows the back arrow but its in-screen `onSectionSelected` pushes routes**, so back
   inside admin retraces section-switch history rather than leaving admin.
6. **`bottomBar` is driven by the *route*, `selected` is driven by `currentTab`.** These can
   disagree: `openUserProfile(self)` sets `currentTab = Profile` and stack `[Profile]` (consistent),
   but `goBack()` never restores `currentTab`, and `continueReading` / `openDeepLink` force
   `currentTab = Collection` while building a `[Home, ...]` stack (consistent). No path currently
   produces a visible mismatch, but nothing enforces it — the invariant "the bottom of the stack is
   `currentTab`'s root route" is maintained only by convention across 5 separate mutation sites.
7. **No route shows both bars**; no route shows the bottom bar without the top bar.
8. **`Search`, `Tools`, `Home`, `Forum`, `Profile` show a top bar whose subtitle is just the tab
   name** — duplicating the selected bottom-nav label on screen.
9. **`AppRoute.PoliticalExam` and `AppRoute.WebFallback` have no `routeContextLabel` entry** — they
   fall to `else -> bottomTabDisplayLabel(fallbackTab)`, so their top bar subtitle shows the
   *current tab's* name (usually 工具) rather than the screen's name. Both are reachable from Tools,
   so users see "工具" while on the exam or a web page. `AppRoute.ForumCreate` **does** have a label
   (发布帖子) — the omission is inconsistent, not systematic.

---

## 7. `UiNavigation.kt` — label helpers (whole file, 38 lines)

All strings are `\uXXXX`-escaped in source; decoded below.

### `bottomTabDisplayLabel(tab: BottomTab): String` — :3-9

| Tab | Escape in source | Decoded |
|---|---|---|
| `Collection` | `\u6536\u85cf` | 收藏 |
| `Discover` | `\u641c\u7d22` | 搜索 |
| `Tools` | `\u5de5\u5177` | 工具 |
| `Forum` | `\u8bba\u575b` | 论坛 |
| `Profile` | `\u6211\u7684` | 我的 |

### `bottomTabShortLabel(tab: BottomTab): String` — :11-17 (no production call site)

| Tab | Escape | Decoded |
|---|---|---|
| `Collection` | `\u6536` | 收 |
| `Discover` | `\u641c` | 搜 |
| `Tools` | `\u5de5` | 工 |
| `Forum` | `\u8bba` | 论 |
| `Profile` | `\u6211` | 我 |

### `routeContextLabel(route: AppRoute, fallbackTab: BottomTab): String` — :19-38

Complete route → label mapping, in source order:

| # | Route pattern | Escape in source | Decoded label |
|---|---|---|---|
| 1 | `AppRoute.MessageCenter` | `\u6d88\u606f\u4e2d\u5fc3` | 消息中心 |
| 2 | `is AppRoute.MessageDetail` | `\u6d88\u606f\u8be6\u60c5` | 消息详情 |
| 3 | `is AppRoute.MessageConversation` | `\u79c1\u4fe1` | 私信 |
| 4 | `AppRoute.MessageSettings` | `\u6d88\u606f\u8bbe\u7f6e` | 消息设置 |
| 5 | `AppRoute.Workspace` | `\u5de5\u4f5c\u533a` | 工作区 |
| 6 | `AppRoute.UploadBook` | `\u4e0a\u4f20\u4e66\u7c4d` | 上传书籍 |
| 7 | `AppRoute.UploadEditor` | `EPUB \u7f16\u8f91\u5668` | `EPUB 编辑器` (ASCII "EPUB" + space) |
| 8 | `AppRoute.ForumCreate` | `\u53d1\u5e03\u5e16\u5b50` | 发布帖子 |
| 9 | `is AppRoute.ForumPostDetail` | `\u5e16\u5b50\u8be6\u60c5` | 帖子详情 |
| 10 | `is AppRoute.BookDetail` | `\u4e66\u7c4d\u8be6\u60c5` | 书籍详情 |
| 11 | `is AppRoute.BookEditInfo` | `\u7f16\u8f91\u4e66\u7c4d\u4fe1\u606f` | 编辑书籍信息 |
| 12 | `is AppRoute.BookChapters` | `\u7ae0\u8282\u7ba1\u7406` | 章节管理 |
| 13 | `is AppRoute.BookAppend` | `\u8ffd\u52a0\u7ae0\u8282` | 追加章节 |
| 14 | `is AppRoute.Reader` | `\u9605\u8bfb` | 阅读 (never displayed — Reader hides the global top bar) |
| 15 | `AppRoute.Settings` | `\u5e94\u7528\u8bbe\u7f6e` | 应用设置 |
| 16 | `is AppRoute.UserProfileDetail` | `\u7528\u6237\u4e3b\u9875` | 用户主页 |
| 17 | `is AppRoute.Admin` | `\u7ba1\u7406\u540e\u53f0` | 管理后台 (all 6 sections share one label) |
| 18 | `else` | — | `bottomTabDisplayLabel(fallbackTab)` |

Routes hitting the `else` branch (7 of 24): `Home`, `Search`, `Tools`, `Forum`, `Profile`
(intentional — they show the tab name), plus **`PoliticalExam`** and **`WebFallback`**
(unintentional, see §6.3 item 9).

Contract tests: `app/src/test/java/com/novalpie/nativeapp/ui/UiNavigationTest.kt`
`messageRoutesUseSpecificProductContextLabels` asserts 11 of these (消息中心, 消息详情, 私信,
消息设置, 工作区, 上传书籍, `EPUB 编辑器`, 收藏 for Home/Collection, 帖子详情, 书籍详情, 阅读).

Other Chinese navigation strings outside `UiNavigation.kt`:
- `NovalPieApp.kt:133` `"NovalPie"` — top bar brand line (ASCII).
- `NovalPieApp.kt:140` `"返回"` — back-arrow `contentDescription`.
- `ReaderPresentation.kt:28` `readerTopBarLabels()` → back `返回`, title `阅读`, web `网页`.
- `NovalPieApp.kt:2249-2251` Tools header `功能中心` / `消息、工作区与网站管理入口`.
- `NovalPieApp.kt:2288` `打开完整消息中心` (button under the Tools message preview).
- `NovalPieApp.kt:2296` `网站功能` (Tools section header).

---

## 8. WebView fallback routes

Single native surface: `AppRoute.WebFallback(url)` → `WebFallbackScreen`
(`ui/WebFallbackScreen.kt:25-56`). Pushed only via
`openWebFallback(url)` (`NovalPieViewModel.kt:3705-3707`, `pushDistinctRoute` so double-taps don't
stack) and `openLoginFallback()` (`:3709-3711`).

### 8.1 Every `openWebFallback` call site and its exact URL

| # | Call site | Trigger | Exact URL |
|---|---|---|---|
| 1 | `NovalPieViewModel.kt:3710` (`openLoginFallback`) | any login gate: `ForumScreen.onOpenLogin`, `HomeScreen.onOpenLogin`, `ToolsScreen.onOpenLogin`, `MessageCenterScreen.onOpenLogin`, `UploadBookScreen.onOpenLogin` (×2 routes), `PoliticalExamScreen.onOpenLogin`, `ProfileScreen.onOpenLogin`, `UserProfileDetailScreen.onOpenLogin`, `SettingsScreen.onOpenLogin`, `ForumCreateScreen.onOpenLogin`, plus VM auth gates `openForumCreate`/`openBookEditInfo`/`openBookChapters`/`openBookAppend` | `https://novalpie.cc/login` |
| 2 | `NovalPieApp.kt:191` | `ForumScreen.onOpenWeb` | `https://novalpie.cc` |
| 3 | `NovalPieApp.kt:217` | `ForumPostDetailScreen.onOpenWeb` | `https://novalpie.cc/posts/${route.postId}` |
| 4 | `NovalPieApp.kt:235` | `HomeScreen.onOpenWeb` | `https://novalpie.cc/favorites` |
| 5 | `NovalPieApp.kt:261` | `SearchScreen.onOpenWeb` | `https://novalpie.cc/search?sort_by=relevance` |
| 6 | `NovalPieApp.kt:281` | `ToolsScreen.onOpenRoute` else-branch (tool path is neither a native path nor an `AdminSection.websitePath`) | `https://novalpie.cc$path` — with the current `toolsEntries` list this branch is **unreachable**: all 11 paths resolve natively. It is the safety net for future/unknown tool paths. |
| 7 | `NovalPieApp.kt:478` | `SettingsScreen.onOpenHomeFallback` | `https://novalpie.cc` |
| 8 | `NovalPieApp.kt:479` | `SettingsScreen.onOpenSearchFallback` | `https://novalpie.cc/search?sort_by=relevance` |
| 9 | `NovalPieApp.kt:500` | `BookDetailScreen.onOpenWeb` | `https://novalpie.cc/book/${route.bookId}` |
| 10 | `NovalPieApp.kt:569` | `ReaderScreen.onOpenWeb` (also the chapter-comments `打开网页评论` action, label at `ReaderPresentation.kt:44`) | `https://novalpie.cc/book/${route.bookId}/${route.chapterId}` |
| 11 | `NovalPieViewModel.kt:1479` | `openMessageAction`, forum/posts segment with a missing/non-numeric id | the normalised `absolute` URL |
| 12 | `NovalPieViewModel.kt:1484` | `openMessageAction`, `book` segment with non-numeric id | the normalised `absolute` URL |
| 13 | `NovalPieViewModel.kt:1489` | `openMessageAction`, any unrecognised first segment | the normalised `absolute` URL: `actionUrl` if already `http(s)://`, else `"https://novalpie.cc/" + actionUrl.trimStart('/')` |

Distinct literal URLs: `https://novalpie.cc/login`, `https://novalpie.cc` (×2 sites),
`https://novalpie.cc/favorites`, `https://novalpie.cc/search?sort_by=relevance` (×2 sites),
`https://novalpie.cc/posts/{postId}`, `https://novalpie.cc/book/{bookId}`,
`https://novalpie.cc/book/{bookId}/{chapterId}`, `https://novalpie.cc{toolPath}`, plus
message-action pass-through.

Screens with **no** web-fallback escape hatch: `ForumCreate`, `Tools` (as a screen),
`MessageCenter`/`MessageDetail`/`MessageConversation`/`MessageSettings`, `Workspace`,
`UploadBook`/`BookAppend`, `UploadEditor`, `PoliticalExam`, `Profile`, `UserProfileDetail`,
`BookEditInfo`, `BookChapters`, `Admin`. Those features are native-only; if the native path fails
the user has no web route to the same content.

### 8.2 `WebFallbackScreen` behaviour worth preserving

`ui/WebFallbackScreen.kt`

- `AndroidView` hosting a raw `WebView` with `javaScriptEnabled`, `domStorageEnabled`,
  `databaseEnabled` all true (`:41-45`).
- `webView.tag` is used as an identity/state key: `webStateKey = "${proxySettings.summary()}:${authToken.orEmpty()}"` (`:32`).
  On `update`, reload happens if the key changed **or** `webView.url != url` (`:49-54`).
- Proxy: if `WebViewFeature.PROXY_OVERRIDE` is supported, `ProxyController.setProxyOverride` with
  `webViewProxyUrl(settings, useEmulatorFallback)` and bypass rules `127.0.0.1`, `localhost`,
  `bypassSimpleHostnames()`; otherwise plain `loadUrl` (`:59-96`). `webViewProxyUrl` (`:98-108`):
  explicit enabled proxy wins → `http://host:port`; else if emulator → `http://10.0.2.2:7890`
  (`ProxySettings.DEFAULT_EMULATOR_PROXY_HOSTS.first()` + `DEFAULT_PROXY_PORT`); else `null`
  (system network). Tested in `app/src/test/java/com/novalpie/nativeapp/ui/WebFallbackPolicyTest.kt`.
- Auth bridge (`:110-158`): on `onPageFinished`, injects `localStorage.setItem('auth_token', …)` if
  the native token differs (and `location.reload()` unless on `/login`), then reads back
  `localStorage.auth_token` or the `auth_token` cookie and reports it via `onAuthTokenCaptured`
  → `viewModel::saveCapturedAuthToken`. This is how the login fallback returns a session to the
  native app.
- The native side also reads WebView cookies: `NovalPieViewModel.kt:440`
  `CookieManager.getInstance().getCookie("https://novalpie.cc")`.
- **No `WebViewClient.shouldOverrideUrlLoading`** — in-page links stay in the WebView and never
  hand back to native routing. **No `webView.goBack()`** wired to app back.

---

## 9. Refactor-contract checklist (facts a refactor must not break)

1. 24 `AppRoute` entries, 24 dispatch branches, exhaustive `when` with no `else`.
2. `AppRoute` members must stay value-equal (`object` / `data class`) — 15+ `currentRoute != AppRoute.X(...)` async guards and both `RouteStackPolicy` functions depend on it.
3. `pushDistinctRoute` must return the **identical list instance** on a no-op; `openBook`, `openReader`, `openForumPost` use `===` to skip reloads.
4. Reader chapter navigation replaces, never pushes.
5. `openTab` on a non-root route of the current tab destroys the stack (pop-to-root + refetch); on the root it only refetches.
6. Tab switch destroys the previous tab's stack — no per-tab back-stacks.
7. `goBack` never touches `currentTab` and never refetches.
8. BackHandler disabled on exactly the 5 tab-root routes (app exits); PoliticalExam-Active overrides with a confirm dialog.
9. Reader is the only route without the global top bar; the 5 tab roots are the only routes with the bottom bar.
10. Deep links cover only `/book/<id>[/<chapterId>]` and `/user/<id>`; everything else silently no-ops. `novalpie://app` is the only registered scheme; no `onNewIntent`.
11. `sendEditorToUpload` searches the stack in reverse for `BookAppend` — the only non-top stack read.
12. `clearUploadBook` reads `currentRoute as? AppRoute.BookAppend` — route-as-state.
13. All 13 web-fallback URLs (§8.1) must remain reachable from the same UI affordances.
14. Every Chinese label in §7 must survive verbatim, including the `EPUB 编辑器` / 上传编辑器 double-naming and the `PoliticalExam` / `WebFallback` label gap (or be fixed deliberately, not silently).
