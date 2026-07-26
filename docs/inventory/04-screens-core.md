# UI Content Inventory — Part 1 of 3: Core Reading Surfaces

Scope: Forum feed, Forum post detail, Home/Collection (bookshelf), Search/Discover, Book detail, Reader.

Source of truth read for this document (all paths absolute):
- `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt` (3654 lines)
- `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieViewModel.kt` (4063 lines — state classes + handlers)
- `...\ui\ForumPresentation.kt`, `...\ui\LibraryPresentation.kt`, `...\ui\DiscoverPresentation.kt`,
  `...\ui\BookDetailPresentation.kt`, `...\ui\BookDetailFacts.kt`, `...\ui\BookDetailProgressMarker.kt`,
  `...\ui\NovelCardFacts.kt`, `...\ui\ReaderPresentation.kt`, `...\ui\ReaderText.kt`,
  `...\ui\ReaderProgressLabel.kt`, `...\ui\ReaderAdjacentChapter.kt`, `...\ui\CatalogSummary.kt`,
  `...\ui\BookFilter.kt`, `...\ui\ChapterFilter.kt`, `...\ui\ImagePreviewDialog.kt`, `...\ui\ProductCopy.kt`,
  `...\ui\VisibleUiLabels.kt`, `...\ui\UiNavigation.kt`, `...\ui\ErrorRecovery.kt`, `...\ui\ApiMessages.kt`,
  `...\ui\RouteStackPolicy.kt`, `...\ui\RequestFreshness.kt`, `...\ui\NovalPieTheme.kt`
- `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\model\Models.kt`
- `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\data\ReaderSettingsStore.kt`,
  `...\data\ReaderProgressStore.kt`, `...\data\SearchHistoryStore.kt`

Note on encoding: nearly all Chinese literals in these files are stored as **literal UTF-8 CJK**, not
`\uXXXX` escapes. The exceptions that ARE `\uXXXX`-escaped are in `UiNavigation.kt` and in a handful of
`NovalPieViewModel.kt` action messages; every one of those is decoded inline below and marked
`(decoded from \uXXXX)`.

---

## 0. Shared chrome that wraps every screen in this part

### 0.1 `NovalPieApp` — the single Scaffold

`NovalPieApp.kt:114-581`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovalPieApp(startUri: String? = null, viewModel: NovalPieViewModel = viewModel())
```

- `NovalPieApp.kt:119-121` — `LaunchedEffect(startUri)`: if `startUri` non-blank → `viewModel.openDeepLink(startUri)`.
- `NovalPieApp.kt:123-125` — `BackHandler` enabled for every route EXCEPT `Forum`, `Home`, `Search`,
  `Tools`, `Profile` (the five tab roots). Calls `viewModel.goBack()`.

**Top bar** (`NovalPieApp.kt:128-150`) — `CenterAlignedTopAppBar`, shown only when
`globalProductTopBarVisible(route)` is true. That helper (`ReaderPresentation.kt:30-31`) returns
`route !is AppRoute.Reader` — **so the Reader is the only screen with no global top bar**.
- Title block, 2 stacked centered lines:
  - Line 1: `"NovalPie"` (Bold) — `NovalPieApp.kt:133`
  - Line 2: `routeContextLabel(route, viewModel.currentTab)` in `labelSmall` — `NovalPieApp.kt:134`
- Navigation icon: `Icons.Filled.ArrowBack`, contentDescription `"返回"`, only on non-tab-root routes
  (`NovalPieApp.kt:137-143`). Click → `viewModel.goBack()`.
- Container color = `colorScheme.surface` for both normal and scrolled states.

`routeContextLabel` (`UiNavigation.kt:19-38`) — **all values are `\uXXXX`-escaped in source, decoded here**:

| route | label (decoded) |
|---|---|
| `MessageCenter` | 消息中心 |
| `MessageDetail` | 消息详情 |
| `MessageConversation` | 私信 |
| `MessageSettings` | 消息设置 |
| `Workspace` | 工作区 |
| `UploadBook` | 上传书籍 |
| `UploadEditor` | `EPUB 编辑器` |
| `ForumCreate` | 发布帖子 |
| `ForumPostDetail` | 帖子详情 |
| `BookDetail` | 书籍详情 |
| `BookEditInfo` | 编辑书籍信息 |
| `BookChapters` | 章节管理 |
| `BookAppend` | 追加章节 |
| `Reader` | 阅读 |
| `Settings` | 应用设置 |
| `UserProfileDetail` | 用户主页 |
| `Admin` | 管理后台 |
| else | `bottomTabDisplayLabel(fallbackTab)` |

**Bottom bar** (`NovalPieApp.kt:151-174`) — `NavigationBar`, shown only on the five tab roots
(`Forum`, `Home`, `Search`, `Tools`, `Profile`). Five `NavigationBarItem`s from `BottomTab.values()`.
- Icons (`NovalPieApp.kt:583-589`): Collection→`Icons.Filled.Favorite`, Discover→`Icons.Filled.Search`,
  Tools→`Icons.Filled.GridView`, Forum→`Icons.Filled.Forum`, Profile→`Icons.Filled.Person`.
- Labels via `bottomTabDisplayLabel` (`UiNavigation.kt:3-9`, `\uXXXX`-escaped, decoded):
  Collection = **收藏**, Discover = **搜索**, Tools = **工具**, Forum = **论坛**, Profile = **我的**.
  Same string is used as the icon `contentDescription` (`NovalPieApp.kt:161`).
- `maxLines = 1`, `overflow = Ellipsis`. Colors: selected icon/text = `primary`, unselected = `secondary`,
  indicator = `primaryContainer`.
- Click → `viewModel.openTab(tab)`.
- `BottomTab` enum also carries a `title` property with the same values
  (`NovalPieViewModel.kt:96-102`: `Collection("收藏")`, `Discover("搜索")`, `Tools("工具")`,
  `Forum("论坛")`, `Profile("我的")`). The `title` property is **not** what the bar renders —
  `bottomTabDisplayLabel` is. Both must be preserved (they are duplicated content).
- `bottomTabShortLabel` (`UiNavigation.kt:11-17`, decoded): 收 / 搜 / 工 / 论 / 我 — **currently unused
  by any composable** but part of the label contract.

Body: `Surface(color = colorScheme.background)` + `when (route)` dispatch table
(`NovalPieApp.kt:182-578`). The six screens in scope are dispatched at:
Forum `183-192`, ForumPostDetail `201-218`, Home `220-236`, Search `238-262`,
BookDetail `482-501`, Reader `550-570`.

### 0.2 Shared atoms used by all six screens

| Composable | File:line | Structure & strings |
|---|---|---|
| `LoadingBlock(message: String)` | `NovalPieApp.kt:3568-3574` | `Column(spacedBy 8dp)` → full-width `LinearProgressIndicator` + `StatusText(message)` |
| `ErrorBlock(message: String, retryLabel: String? = null, onRetry: (() -> Unit)? = null)` | `NovalPieApp.kt:3576-3595` | `ElevatedCard(containerColor = surfaceVariant)`, padding 16dp → `Text(message, bodyMedium)`; if `onRetry != null` → `OutlinedButton { Text(retryLabel ?: retryActionLabel("")) }` |
| `StatusText(message: String)` | `NovalPieApp.kt:3597-3602` | `Box(fillMaxWidth, vertical padding 8dp)` → `Text(message, bodyMedium)` |
| `LibraryStatPill(label: String)` | `NovalPieApp.kt:3604-3620` | `Surface(RoundedCornerShape(4dp), primaryContainer)` → `Text` labelMedium, SemiBold, `onPrimaryContainer`, padding 8/4, maxLines 1 |
| `NovelTagPill(label)` | `NovalPieApp.kt:3015-3030` | `Surface(4dp, primaryContainer)` → labelSmall `onPrimaryContainer`, padding 6/2, maxLines 1 |
| `NovelSourcePill(label)` | `NovalPieApp.kt:3032-3047` | `Surface(4dp, secondaryContainer)` → labelSmall `onSecondaryContainer`, padding 6/2, maxLines 1 |
| `BookDetailFactLabel(label)` | `NovalPieApp.kt:3168-3178` | `Surface(4dp, secondaryContainer)` → labelSmall `onSecondaryContainer`, padding 8/4 |
| `CompactForumBadge(label)` | `NovalPieApp.kt:799-815` | `Surface(16dp, primaryContainer)` → labelSmall SemiBold `onPrimaryContainer`, padding 8/4, maxLines 1 |
| `LoadMoreRow(canLoadMore, loading, onLoadMore, idleText, loadText)` | `NovalPieApp.kt:2935-2957` | if `loading` → `LinearProgressIndicator` + `Text("正在加载更多", bodySmall)`; else if `canLoadMore` → `OutlinedButton { Text(loadText) }`; else → `Text(idleText, bodySmall)` |
| `CatalogSummaryText(value)` | `NovalPieApp.kt:3435-3438` | `Text(value, labelMedium, onSurfaceVariant)` |
| `CatalogFilterField(value, onValueChange)` | `NovalPieApp.kt:3440-3449` | full-width single-line `OutlinedTextField`, label `"筛选目录"` |
| `ChapterRow(chapter, selected, onClick)` | `NovalPieApp.kt:3416-3433` | `ElevatedCard`, container `surfaceVariant` if selected else `surface`; Row padding 14dp: `Text(chapter.number?.let { "#$it" } ?: "CH", labelLarge Bold)`, `Spacer(12dp)`, Column: `Text(chapter.title)` Bold-if-selected, and if `wordCount != null` → `Text("$wordCount 字", labelSmall)` |
| `ForumActionIcon(icon, label, onClick, modifier)` | `NovalPieApp.kt:974-1001` | Row clip 16dp, clickable, padding 8/6: 20×20 `Icon` (contentDescription = label, tint onSurfaceVariant) + `Text(label, labelMedium, onSurfaceVariant)` |
| `ForumLinkPreviewRows(links)` | `NovalPieApp.kt:1021-1036` | Returns early if `links.isEmpty()`. Else `Text("链接预览", titleSmall Bold)` then **first 4** links each in `Surface(6dp, surfaceVariant)` → `Text(link, bodySmall, maxLines 2, Ellipsis)` |
| `InlineCommentComposer(...)` | `NovalPieApp.kt:1076-1110` | see §2.4 |
| `ForumCommentComposer(...)` | `NovalPieApp.kt:1038-1074` | same content as InlineCommentComposer but wrapped in `ElevatedCard(RoundedCornerShape(16dp), surface)` padding 12dp; used only by Book detail (`NovalPieApp.kt:3246`) |
| `ChapterCommentActionRow(comment, ...)` | `NovalPieApp.kt:2163-2179` | `LazyRow(spacedBy 8dp)` of 5 `ForumActionIcon`: ThumbUp `"赞 N"`, ThumbDown `"踩 N"`, EmojiEmotions `"表情 N"`, CardGiftcard `"打赏 N"`, Reply `"回复"` (N defaults to 0 via `?: 0`) |
| `BookCover(title, coverUrl, width, height, modifier, previewUrl = coverUrl)` | `NovalPieApp.kt:3349-3397` | see §5.3 |
| `ImagePreviewDialog(imageUrl, title, onDismiss)` | `ImagePreviewDialog.kt:61-164` | see §6.9 |

`retryActionLabel(surface: String)` (`ErrorRecovery.kt:3-6`): blank → `"重试"`, else `"重试" + surface`.
Concrete values seen in these screens: `重试书架`, `重试搜索`, `重试书籍详情`, `重试章节目录`,
`重试评论区`, `重试正文`, `重试章节评论`, plus literals `重新同步`, `重试帖子`, `重试评论`, `重试标签`.

`apiFailureMessage(label, throwable)` (`ApiMessages.kt:3-14`) — every `LoadResult.Error` message body:
`"${label 去掉后缀 /API，空则 请求}请求失败: ${detail}"` where detail is the exception message, or if it
matches `NovalPie API (\d+)` it is replaced by `"服务返回错误 $status"`. So error card text looks like
`书架请求失败: 服务返回错误 500`.

`VisibleUiLabels` (`VisibleUiLabels.kt:3-13`) — labels fed into `apiFailureMessage`:
`ForumPostDetail = "帖子详情"`, `Comments = "评论"`, `CommentSubmit = "评论提交"`,
`FavoriteGroups = "收藏分组"`, `Bookshelf = "书架"`, `Search = "搜索"`, `BookDetail = "书籍详情"`,
`ChapterCatalog = "章节目录"`, `ChapterComments = "章节评论"`.

`LoadResult<T>` (`Models.kt:650-655`): `Idle | Loading | Success(value) | Error(message)`. Every screen
below branches on all four.

---

## 1. Forum feed — `ForumScreen`

### 1.1 Signature

`NovalPieApp.kt:591-602`

```kotlin
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ForumScreen(
    posts: LoadResult<List<ForumPost>>,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenPost: (Long) -> Unit,
    onCreatePost: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenWeb: () -> Unit
)
```

Wiring (`NovalPieApp.kt:183-192`): `posts = viewModel.forumState.posts`;
`hasAuthToken = !viewModel.authToken.isNullOrBlank()`; `onRefresh = viewModel::loadForum`;
`onOpenPost = viewModel::openForumPost`; `onCreatePost = viewModel::openForumCreate`;
`onOpenUser = viewModel::openUserProfile`; `onOpenLogin = viewModel::openLoginFallback`;
`onOpenWeb = { openWebFallback("https://novalpie.cc") }`.

### 1.2 CRITICAL: demo-data fallback

`NovalPieApp.kt:605-608`

```kotlin
val feedItems = when (posts) {
    is LoadResult.Success -> posts.value.map(::forumPostFeedItem).ifEmpty { forumFeedItems() }
    else -> forumFeedItems()
}
```

So during Idle/Loading/Error **and** on an empty successful response, the feed renders 6 hard-coded
demo rows from `forumCardCopies`-adjacent `forumFeedItems()` (`ProductCopy.kt:139-220`). Because these
mock items have `id = 0`, `ForumFeedRow`'s `clickable(enabled = item.id > 0)` disables navigation
(`NovalPieApp.kt:724`). This is user-visible content and must be preserved verbatim:

| # | category | title | bookTitle | authorName | reply | like | reaction | award | view | lastActive | tags | pinned | featured |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 书评 | 角色弧光讨论 | 热门作品 | 北港读者 | 42 | 81 | 12 | 7 | 7305 | 刚刚 | 热议, 长评 | ✔ | ✔ |
| 2 | 章节 | 最新章节伏笔整理 | 连载专区 | 栗子校对 | 28 | 34 | 6 | 2 | 1904 | 8分钟前 | 剧情, 伏笔 | | |
| 3 | 动态 | 作者更新说明 | 站内公告 | 运营记录 | 17 | 22 | 4 | 1 | 860 | 23分钟前 | 公告 | | |
| 4 | 书评 | 结局走向猜测 | 长篇讨论 | 雾灯 | 64 | 73 | 9 | 5 | 4201 | 1小时前 | 推理, 讨论 | | |
| 5 | 章节 | 翻译名词校对 | 协作校对 | 灰页 | 11 | 19 | 3 | 0 | 640 | 2小时前 | 校对, 术语 | | |
| 6 | 动态 | 收藏榜单变化 | 作品榜 | 榜单观察 | 9 | 15 | 2 | 0 | 512 | 今天 | 榜单 | | |

`forumPostFeedItem(post)` (`ForumPresentation.kt:11-28`) maps `ForumPost` → `ForumFeedItem`
(`ProductCopy.kt:20-36`) with these fallbacks:
- `bookTitle = post.bookTitle ?: "站内讨论"`
- `authorName = post.authorName ?: "匿名用户"`
- `replyCount/likeCount/reactionCount/awardPoints/viewCount = ?: 0`
- `lastActiveLabel = post.lastActiveLabel ?: "刚刚"`
- `tags = post.tags.ifEmpty { listOf(post.category) }`
- `pinned`, `featured`, `authorId`, `id`, `category`, `title` pass through.

### 1.3 Visual structure, top to bottom

Root: `Box(fillMaxSize)` (`NovalPieApp.kt:609`) containing a `LazyColumn` + a FAB overlay.

`LazyColumn` (`NovalPieApp.kt:610-616`): `semantics { contentDescription = "NOVALPIE_NATIVE_COMPOSE_HOME" }`
(instrumentation marker — must be kept), contentPadding start/top/end 12dp, **bottom 80dp**,
vertical spacing 8dp.

1. **Header row** (`617-638`) — `Row(SpaceBetween, CenterVertically)`:
   - Left `Column(spacedBy 2dp)`: `Text(forumHeader().title, titleLarge Bold)` = **论坛**;
     `Text(forumHeader().subtitle, bodySmall, onSurfaceVariant)` = **小说讨论、书评和站内动态**
     (`ProductCopy.kt:112-113`).
   - Right: `Text(if (hasAuthToken) "已同步" else "未同步", labelMedium, primary, SemiBold)` (`631-636`).
2. **Action rail** (`639-645`) — `LazyRow(spacedBy 8dp)`, labels from
   `forumPrimaryActions(hasAuthToken)` (`ProductCopy.kt:115-116`) = `["同步", if(hasAuthToken) "已登录" else "登录", "网页论坛"]`:
   - `Button { Text(actions[0]) }` = **同步** → `onRefresh`
   - `OutlinedButton { Text(actions[1]) }` = **已登录** / **登录** → `onOpenLogin` (always navigates to
     the web login fallback, even when already logged in)
   - `OutlinedButton { Text(actions[2]) }` = **网页论坛** → `onOpenWeb`
3. **Stats strip** (`646-648`, impl `690-713`) — `ForumStatsStrip(feedItems)`:
   `ElevatedCard(containerColor = surface)`, Row padding 12/10, `SpaceBetween`, three `ForumStat` cells
   (`Column`, centered, value `titleMedium Bold` above label `labelSmall onSurfaceVariant`):
   - **主题** = `feedItems.size`
   - **回复** = `feedItems.sumOf { it.replyCount }`
   - **分区** = `forumFeedTabs().size` (constant 4)
4. **Load state slot** (`649-662`) — inserted only for these states:
   - `Loading` → `LoadingBlock("正在同步论坛")`
   - `Error` → `ErrorBlock(message = posts.message, retryLabel = "重新同步", onRetry = onRefresh)`
   - `Success` and empty → `StatusText("论坛暂时没有可显示的讨论")`
   - `Idle` → nothing
5. **Category tab chips** (`663-673`) — `LazyRow(spacedBy 8dp)` over `forumFeedTabs()`
   (`ProductCopy.kt:136-137`) = **全部 / 书评 / 章节 / 动态**. Each is a `FilterChip` with
   `selected = tab == forumFeedTabs().first()` (i.e. 全部 always selected) and **`onClick = {}` — a
   no-op**. Non-functional decoration today; the four labels are still content.
6. **Feed rows** (`674-676`) — one `ForumFeedRow` per `feedItems` entry.

**FAB** (`679-686`, inside the outer `Box`, `align(BottomEnd).padding(16dp)`):
`FloatingActionButton(containerColor = primary, contentColor = onPrimary)` with
`Icon(Icons.Filled.Reply, contentDescription = "发布帖子")` → `onCreatePost`.

### 1.4 `ForumFeedRow`

`NovalPieApp.kt:715-797`

```kotlin
private fun ForumFeedRow(item: ForumFeedItem, onOpenPost: (Long) -> Unit, onOpenUser: (Long) -> Unit)
```

- `ElevatedCard`, `RoundedCornerShape(12dp)`, `clickable(enabled = item.id > 0) { onOpenPost(item.id) }`.
- Pinned styling: container `secondaryContainer` (else `surface`), elevation 4dp (else 1dp) (`726-729`).
- Row padding 10/9, spacing 10dp:
  1. **Avatar box** (`736-755`) — 42×42 circle, background `primary` if pinned else `surfaceVariant`;
     centered `Text(item.category.take(1))` Bold, color `onPrimary` if pinned else `onSurfaceVariant`.
  2. **Middle column** (`756-790`, weight 1, spacing 5dp):
     - `LazyRow(spacedBy 5dp)` of `CompactForumBadge` for each entry of `forumFeedBadges(item)`
       (`ForumPresentation.kt:30-37`): `"置顶"` if pinned, `"精华"` if featured, then `item.category`,
       then up to the first 2 `item.tags` that are non-blank and differ from the category.
     - `Text(item.title, titleMedium Bold, onSurface, maxLines 1, Ellipsis)`
     - `Text(forumFeedMetaLine(item), bodySmall, onSurfaceVariant, maxLines 1, Ellipsis)` —
       `clickable(enabled = item.authorId != null)` → `onOpenUser(authorId)`.
       `forumFeedMetaLine` (`ForumPresentation.kt:39-43`) = `authorName · bookTitle · lastActiveLabel`
       joined with `" · "` after trimming and dropping blanks.
     - `LazyRow(spacedBy 8dp)` of `forumFeedMetricLabels(item)` (`ForumPresentation.kt:45-52`), each
       `labelSmall onSurfaceVariant maxLines 1`. **Exact formats**:
       `"${replyCount} 条回复"`, `"赞 ${likeCount}"`, `"表情 ${reactionCount}"`,
       `"打赏 ${awardPoints}"`, `"${viewCount} 次浏览"`.
  3. **Trailing column** (`791-794`, End-aligned, spacing 2dp):
     `Text(item.replyCount.toString(), titleMedium Bold)` over `Text("回复", labelSmall, onSurfaceVariant)`.

### 1.5 Backing data & loader

`loadForum()` (`NovalPieViewModel.kt:2692-2700`): sets `forumState = ForumState(posts = Loading)`,
calls `api.forumPosts(page = 1, limit = PAGE_SIZE /* 20 */)`, then
`result.toLoadResult("璁哄潧")`.

> **BUG / content defect to preserve-or-fix consciously:** `NovalPieViewModel.kt:2698` contains the
> mojibake literal `"璁哄潧"` (bytes `e7 92 81 e5 93 84 e6 bd a7`) — this is double-encoded `论坛`.
> A forum load failure therefore renders `璁哄潧请求失败: …` in the `ErrorBlock`. The intended text is
> `论坛请求失败: …`. (The second occurrence of this class of bug is `"鏍囩"` for `标签` at
> `NovalPieViewModel.kt:3914`, see §4.7.)

`ForumState` (`NovalPieViewModel.kt:141-143`): `posts: LoadResult<List<ForumPost>> = Idle`.

`ForumPost` (`Models.kt:564-581`): `id, category, title, authorName?, bookTitle?, replyCount?,
likeCount?, reactionCount?, awardPoints?, viewCount?, lastActiveLabel?, excerpt?, tags, pinned,
featured, authorId?`. **`excerpt` is never rendered on the feed.**

Dead-but-present forum copy that no composable uses: `forumCardCopies()` (`ProductCopy.kt:118-134`) —
three `ForumCardCopy(title, subtitle, meta)` triples:
1. `最新讨论` / `追踪书评、章节讨论和作品动态` / `站内动态`
2. `热门书评` / `查看近期被回复、收藏和引用的评论` / `书友反馈`
3. `关联书籍` / `讨论行展示作品、标签、回复数和最后活跃时间` / `作品索引`

---

## 2. Forum post detail — `ForumPostDetailScreen`

### 2.1 Signature

`NovalPieApp.kt:817-835`

```kotlin
@Composable
private fun ForumPostDetailScreen(
    state: ForumPostDetailState,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyComment: (ForumComment) -> Unit,
    onCancelReply: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onCommentLike: (Long) -> Unit,
    onCommentDislike: (Long) -> Unit,
    onCommentEmoji: (Long) -> Unit,
    onCommentAward: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenWeb: () -> Unit
)
```

Wiring (`NovalPieApp.kt:201-218`): `onRetry = { loadForumPostDetail(route.postId) }`,
`onOpenWeb = { openWebFallback("https://novalpie.cc/posts/${route.postId}") }`, the rest are direct
`viewModel::` refs (`updateForumCommentDraft`, `submitForumComment`, `replyToForumComment`,
`cancelForumReply`, `likeForumPost`, `dislikeForumPost`, `emojiForumPost`, `awardForumPost`,
`likeForumComment`, `dislikeForumComment`, `emojiForumComment`, `awardForumComment`,
`openUserProfile`).

`ForumPostDetailState` (`NovalPieViewModel.kt:326-335`): `postId`, `detail: LoadResult<ForumPostDetail>`,
`comments: LoadResult<List<ForumComment>>`, `commentDraft`, `replyingToCommentId?`,
`replyingToName?`, `actionMessage?`, `actionLoading`.

### 2.2 Visual structure, top to bottom

`LazyColumn(fillMaxSize, contentPadding 12dp, spacedBy 10dp)` (`NovalPieApp.kt:836-840`).

1. **Post block** — `when (state.detail)` (`841-862`):
   - `Idle` or `Loading` → `LoadingBlock("正在打开帖子")`
   - `Error` → `ErrorBlock(detail.message, retryLabel = "重试帖子", onRetry)`
   - `Success` → `ForumPostHeader(...)` then `ForumPostBody(detail.value.content.orEmpty())`
2. **Composer** (`864-874`) — `InlineCommentComposer` fed from `state.commentDraft`,
   `state.replyingToName`, `state.actionLoading`, `state.actionMessage`.
3. **Section title** (`876-878`) — `Text("评论", titleMedium Bold)`.
4. **Comments block** — `when (state.comments)` (`879-907`):
   - `Idle`/`Loading` → `LoadingBlock("正在同步评论")`
   - `Error` → `ErrorBlock(comments.message, retryLabel = "重试评论", onRetry)`
   - `Success` empty → `StatusText("还没有评论")`
   - `Success` non-empty → build `forumCommentThreads(comments.value)`, render
     `Text(forumCommentThreadSummary(threads), bodySmall, onSurfaceVariant)` then one
     `ForumCommentThreadBlock` per thread.

`forumCommentThreadSummary` (`ForumPresentation.kt:86-89`) = `"${threads.size} 条评论 · $replyCount 条回复"`
where `replyCount = threads.sumOf { it.replies.size }`.

`forumCommentThreads` (`ForumPresentation.kt:67-84`): groups flat comments into roots + replies by
walking `parentCommentId` up to the root (`forumCommentRoot`, `91-102`, with a visited-set cycle guard).
Roots keep first-seen order; a comment whose parent id is missing from the list becomes its own root.

### 2.3 `ForumPostHeader`

`NovalPieApp.kt:911-948`. `ElevatedCard`, `Column(padding 14dp, spacedBy 10dp)`:
1. `LazyRow(spacedBy 6dp)` of `CompactForumBadge` over `forumFeedBadges(forumPostFeedItem(detail.post))`
   — same 置顶 / 精华 / category / up-to-2-tags rule as the feed.
2. `Text(detail.post.title, headlineSmall Bold)`.
3. `Text(forumFeedMetaLine(forumPostFeedItem(detail.post)), bodySmall, onSurfaceVariant)`,
   `clickable(enabled = detail.post.authorId != null)` → `onOpenUser`.
4. `ForumActionBar(likeCount = detail.likeCount ?: 0, dislikeCount = detail.dislikeCount ?: 0,
   reactionCount = detail.reactionCount ?: 0, awardPoints = detail.awardPoints ?: 0, …, onOpenWeb)`.

`ForumActionBar` (`NovalPieApp.kt:950-972`) — `Row(fillMaxWidth, SpaceBetween)` of `ForumActionIcon`s,
labels come from `forumActionBarLabels()` (`ForumPresentation.kt:54-55`) = `["赞","踩","表情","打赏","网页"]`:
- `Icons.Filled.ThumbUp` → `"赞 $likeCount"` → `onLike`
- `Icons.Filled.ThumbDown` → `"踩 $dislikeCount"` → `onDislike`
- `Icons.Filled.EmojiEmotions` → `"表情 $reactionCount"` → `onEmoji`
- `Icons.Filled.CardGiftcard` → `"打赏 $awardPoints"` → `onAward`
- `Icons.Filled.OpenInBrowser` → `"网页"` (no count) → `onOpenWeb`, **only rendered when
  `onOpenWeb != null`** (`968-970`).

### 2.4 `ForumPostBody` and the composer

`ForumPostBody(content)` (`NovalPieApp.kt:1003-1019`):
- `paragraphs = readerParagraphsFromContent(content)` (HTML → plain paragraphs, see §6.6)
- `links = forumContentLinks(paragraphs)`
- `ElevatedCard`, `Column(padding 14dp, spacedBy 10dp)`:
  - if `paragraphs.isEmpty()` → `Text("正文暂时为空", color = onSurfaceVariant)`
  - else each paragraph as `Text(bodyLarge)`
  - then `ForumLinkPreviewRows(links)`

`forumContentLinks` (`ForumPresentation.kt:57-62`): finds `https?://[^\s<>"']+` in each paragraph,
trims trailing `. , ; : ，。、；：！？) ）`, dedupes.

`InlineCommentComposer` (`NovalPieApp.kt:1076-1110`) — `Column(spacedBy 8dp)`:
1. If `replyingToName != null` → `Row(spacedBy 8dp)`: `Text("回复 $replyingToName", labelLarge, primary)`
   + `TextButton { Text("取消") }` → `onCancelReply`.
2. Full-width `OutlinedTextField(minLines = 2, shape = RoundedCornerShape(12dp), label = { Text("写评论") })`
   bound to `draft`/`onDraftChange`.
3. `Row(spacedBy 8dp)`: `Button(enabled = !loading && draft.isNotBlank()) { Text(if (loading) "发送中" else "发送") }`
   → `onSubmit`; then, if `message != null`, `Text(message, bodySmall, onSurfaceVariant)`.

`ForumCommentComposer` (`NovalPieApp.kt:1038-1074`) is the card-wrapped twin used by Book detail —
identical strings 回复 / 取消 / 写评论 / 发送中 / 发送.

### 2.5 `ForumCommentThreadBlock` and `ForumCommentRow`

`ForumCommentThreadBlock` (`NovalPieApp.kt:1112-1157`): `Column(spacedBy 6dp)` → root
`ForumCommentRow`; then each reply in a `Row(fillMaxWidth, padding start 14dp, spacedBy 8dp)` with a
2dp-wide × **86dp-tall** `Box(background = outlineVariant)` thread rail, then the reply's
`ForumCommentRow` in a weight-1 `Box`.

`ForumCommentRow` (`NovalPieApp.kt:1159-1205`):
- `paragraphs = readerParagraphsFromContent(comment.content).ifEmpty { listOf(comment.content) }`
- `links = forumCommentLinkPreviews(comment)` (`ForumPresentation.kt:64-65` — strips HTML then extracts URLs)
- `Surface(fillMaxWidth, RoundedCornerShape(12dp), color = surfaceVariant.copy(alpha = 0.5f))`,
  `Column(padding 12dp, spacedBy 8dp)`:
  1. `Row(SpaceBetween)`: `Text(comment.authorName ?: "匿名用户", SemiBold)` with
     `clickable(enabled = comment.authorId != null)` → `onOpenUser`; and if `createdAt != null`
     `Text(createdAt, labelSmall, onSurfaceVariant)`.
  2. If `comment.replyToName != null` → `Text("回复 $replyToName", labelSmall, primary)`.
  3. Each paragraph as `Text(bodyMedium)`.
  4. `ForumLinkPreviewRows(links)`.
  5. `Row(fillMaxWidth, SpaceBetween)` of 5 `ForumActionIcon`:
     `"赞 ${likeCount ?: 0}"`, `"踩 ${dislikeCount ?: 0}"`, `"表情 ${reactionCount ?: 0}"`,
     `"打赏 ${awardPoints ?: 0}"`, `"回复"` (Reply icon → `onReply`).

`ForumComment` (`Models.kt:592-605`): `id, postId?, parentCommentId?, authorName?, replyToName?, content,
likeCount?, dislikeCount?, reactionCount?, awardPoints?, createdAt?, authorId?`.
`ForumPostDetail` (`Models.kt:583-590`): `post, content?, likeCount?, dislikeCount?, reactionCount?,
awardPoints?`.

### 2.6 Interaction semantics (from the ViewModel)

- `loadForumPostDetail(postId)` (`NovalPieViewModel.kt:2796-2814`): resets state to Loading/Loading but
  **preserves `commentDraft`, `replyingToCommentId`, `replyingToName` if `postId` is unchanged**; fires
  `api.forumPostDetail(postId)` and `api.forumPostComments(postId, page 1, limit 20)` in parallel;
  bails if the route changed; error labels `帖子详情` / `评论`.
- `replyToForumComment(comment)` (`2820-2828`): sets reply target and, **if the draft is blank,
  pre-fills it with `"@${comment.authorName} "`**.
- `cancelForumReply()` (`2830-2832`): clears reply target only (draft is kept).
- `submitForumComment()` (`2834-2867`): no-ops when `postId <= 0`, draft blank after trim, or
  `actionLoading`. On success: clears draft + reply target, `actionMessage = it.message ?: "评论已提交"`
  (source is `\u8bc4\u8bba\u5df2\u63d0\u4ea4`, **decoded = 评论已提交**). On failure:
  `apiFailureMessage("评论提交", e)`. Always reloads the post detail afterwards.
- Post reactions (`2869-2923`): `likeForumPost` → `api.toggleForumPostLike`; `dislikeForumPost` →
  `api.reactToForumPost(id, "down")`; `emojiForumPost` → `"emoji:heart"`; `awardForumPost` →
  `"award", awardPoints = 10`. Shared `reactOnForumPost(label)` sets
  `actionMessage = it.message ?: "$label 已同步"` (source `\u5df2\u540c\u6b65` = **已同步**) or
  `apiFailureMessage(label, e)`, then reloads. Labels from `forumPostActionLabel`
  (`VisibleUiLabels.kt:15-20`): **点赞 / 点踩 / 表情 / 打赏**.
- Comment reactions (`2885-2899`) use `forumCommentActionLabel` (`VisibleUiLabels.kt:22-23`) =
  `"评论" + 点赞|点踩|表情|打赏` → **评论点赞 / 评论点踩 / 评论表情 / 评论打赏**; same
  `award = 10` and `emoji:heart` constants.
- All `actionMessage` values surface in the composer's trailing `Text` (`NovalPieApp.kt:1105-1107`).

---

## 3. Home / Collection (bookshelf) — `HomeScreen`

### 3.1 Signature

`NovalPieApp.kt:1207-1224`

```kotlin
@Composable
private fun HomeScreen(
    state: HomeState,
    hasAuthToken: Boolean,
    readerProgress: ReaderProgress?,
    recentReaderProgresses: List<ReaderProgress>,
    bookshelfQuery: String,
    onRefresh: () -> Unit,
    onBookshelfQueryChange: (String) -> Unit,
    onFavoriteGroupSelected: (Long?) -> Unit,
    onOpenLogin: () -> Unit,
    onContinueReading: (ReaderProgress) -> Unit,
    onClearReaderProgress: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onLoadMoreFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenWeb: () -> Unit
)
```

Wiring (`NovalPieApp.kt:220-236`): `onOpenSearch = { viewModel.openTab(BottomTab.Discover) }`,
`onOpenWeb = { openWebFallback("https://novalpie.cc/favorites") }`, rest are `viewModel::` refs
(`loadHome`, `updateBookshelfQuery`, `selectFavoriteGroup`, `openLoginFallback`, `continueReading`,
`clearReaderProgress`, `openBook`, `loadMoreFavorites`).

`HomeState` (`NovalPieViewModel.kt:131-139`): `user: LoadResult<UserProfile>`,
`groups: LoadResult<List<FavoriteGroup>>`, `favorites: LoadResult<List<NovelCard>>`, `favoritesPage`,
`favoritesCanLoadMore`, `favoritesLoadingMore`, `selectedFavoriteGroupId?`. **`state.user` is not
rendered anywhere on this screen.**

### 3.2 Derived overview

`NovalPieApp.kt:1225-1238` computes `favoriteCount` (size of the Success list else 0),
`groupCount` (same), then `libraryOverview(hasAuthToken, favoriteCount, groupCount,
recentCount = recentReaderProgresses.size)`.

`libraryOverview` (`LibraryPresentation.kt:10-20`) returns `LibraryOverview`:
- `title = "书架"`
- `subtitle = "继续阅读、收藏分组和最近进度"`
- `syncLabel = if (hasAuthToken) "已同步" else "未同步"`
- `stats = listOf("收藏 $favoriteCount", "分组 $groupCount", "最近 $recentCount")`

(There is a *second*, unused header for this surface: `productHeader(ProductSurface.Library)` =
`ProductHeader("书架", "收藏、分组和阅读进度")` at `ProductCopy.kt:39`. Also unused:
`libraryPrimaryActions()` = `["同步书架","登录同步","网页收藏"]` at `ProductCopy.kt:47-48`.)

### 3.3 Visual structure, top to bottom

`LazyColumn(fillMaxSize, contentPadding 12dp, spacedBy 10dp)` (`NovalPieApp.kt:1240-1244`).

1. **`LibraryOverviewBlock`** (`1245-1253`, impl `2637-2721`) — `Column(spacedBy 10dp)`:
   1. `Row(SpaceBetween, CenterVertically)`:
      - Left `Column(spacedBy 2dp)`: `Text("书架", titleLarge Bold)`;
        `Text("继续阅读、收藏分组和最近进度", bodySmall, onSurfaceVariant)`.
      - Right `Row`: `Surface(RoundedCornerShape(4dp), secondaryContainer)` →
        `Text(syncLabel, labelSmall, onSecondaryContainer, padding 8/4)` = **已同步** / **未同步**;
        `IconButton { Icon(Icons.Filled.Refresh, contentDescription = "同步书架", tint = primary) }` → `onRefresh`;
        `IconButton { Icon(Icons.Filled.OpenInBrowser, contentDescription = "打开网页收藏") }` → `onOpenWeb`.
   2. **Search entry bar** (`2676-2695`) — `Surface(fillMaxWidth, 8dp, surface)` `clickable(onOpenSearch)`,
      Row padding 14/13, spacing 10dp: leading `Icons.Filled.Search` (contentDescription `null`,
      tint `onSurfaceVariant`); `Text("搜索小说、作者或标签", bodyMedium, onSurfaceVariant, weight 1)`;
      trailing `Icons.Filled.Search` with contentDescription **"进入搜索"**, tint `primary`.
   3. **Conditional login CTA** (`2696-2700`) — rendered only when `overview.syncLabel == "未同步"`:
      `TextButton(align Start) { Text("登录后同步收藏") }` → `onOpenLogin`.
   4. **Metric strip** (`2701-2719`) — `Surface(fillMaxWidth, 8dp, secondaryContainer)`, Row padding
      12/10, over `overview.stats.take(3)`, each a weight-1 `LibraryMetricCell`
      (`2723-2734`: `Text(label, labelSmall, onSurfaceVariant)` above
      `Text(value.ifBlank { "0" }, titleLarge SemiBold, onSurface)`). Labels are re-derived by index:
      0 → **收藏**, 1 → **分组**, else → **最近**; the value is `stat.filter(Char::isDigit)`, i.e. the
      digits scraped out of `"收藏 12"`.
2. **`ContinueReadingCard`** (`1254-1262`, impl `2832-2860`) — rendered **only when
   `readerProgress != null`**. `Surface(fillMaxWidth, 8dp, primaryContainer)`, Row padding 12dp,
   spacing 10dp:
   - 24×24 `Icon(Icons.Filled.MenuBook, contentDescription = null, tint = primary)`
   - weight-1 `Column(spacedBy 3dp)`:
     - `Text(libraryContinueTitle(hasProgress = true), titleMedium Bold, onPrimaryContainer)` = **继续阅读**
       (`LibraryPresentation.kt:22-23`)
     - `Text("章节 ${progress.chapterId}", bodySmall, onPrimaryContainer α .8)`
     - if `progress.chapterTitle != null` → `Text(chapterTitle, maxLines 1, Ellipsis, onPrimaryContainer)`
   - `Button { Text(actions[0]) }` = **继续阅读** → `onContinue`
   - `TextButton { Text(actions[1]) }` = **清除** → `onClear`
     (`libraryContinueActions()` = `["继续阅读","清除"]`, `LibraryPresentation.kt:25`)
3. **`RecentReadingSection`** (`1263-1271`, impl `2862-2889`) — rendered only when
   `recentReaderProgresses.filterNot { it == readerProgress }` is non-empty. `Column(spacedBy 8dp)`:
   - `Text(libraryContinueTitle(hasProgress = false), titleSmall Bold)` = **阅读记录**
   - `LazyRow(spacedBy 8dp)` over the first **8** entries; each is a 196dp-wide `ElevatedCard`,
     `clickable { onContinueReading(progress) }`, `Column(padding 12dp, spacedBy 5dp)`:
     `Text("章节 ${progress.chapterId}", labelMedium SemiBold)` and
     `Text(progress.chapterTitle ?: "继续上次阅读", maxLines 2, Ellipsis, bodySmall)`.
4. **`LibraryShelfControls`** (`1272-1280`, impl `2736-2788`) — `Surface(fillMaxWidth, 8dp, surface)`,
   `Column(padding 12dp, spacedBy 10dp)`:
   - Header `Row(spacedBy 6dp)`: `Icon(Icons.Filled.Tune, contentDescription = null, tint = primary)` +
     `Text("分组与筛选", titleSmall Bold)`
   - `when (groups)`:
     - `Idle` → `StatusText("等待加载分组")`
     - `Loading` → `LoadingBlock("正在加载收藏分组")`
     - `Error` → `Text(groups.message, bodySmall)` (**no retry button here**)
     - `Success` → `LazyRow(spacedBy 8dp)`: a leading `FilterChip(selected = selectedGroupId == null,
       label = "全部")` → `onGroupSelected(null)`, then up to the first **8** groups as
       `FilterChip(selected = group.id != null && selectedGroupId == group.id,
       enabled = group.id != null, label = "${group.name}${group.count?.let { " $it" } ?: ""}")`
       → `onGroupSelected(group.id)`. Note: **no empty-state text** in this variant when the list is empty.
   - `OutlinedTextField(fillMaxWidth, singleLine, label = { Text("筛选书架") })` bound to
     `bookshelfQuery` / `onBookshelfQueryChange`.
5. **Favorites title** (`1281`) — `Text(libraryFavoritesTitle(), titleMedium Bold)` = **收藏书籍**
   (`LibraryPresentation.kt:27`).
6. **Favorites body** — `when (state.favorites)` (`1282-1324`):
   - `Idle` → `StatusText("等待加载书架")`
   - `Loading` → `LoadingBlock("正在加载收藏书籍")`
   - `Error` → `ErrorBlock(favorites.message, retryLabel = retryActionLabel("书架") /* 重试书架 */, onRetry = onRefresh)`
   - `Success`:
     - `visibleBooks = filterBooks(favorites.value, bookshelfQuery)`
     - `favorites.value.isEmpty()` → `EmptyCollectionState(onOpenLogin, onOpenWeb)`
     - `visibleBooks.isEmpty()` (but the raw list wasn't) → `StatusText("没有匹配的收藏")`
     - else → a manual 2-column grid: `visibleBooks.chunked(novelGridColumnCount() /* 2 */)`,
       each chunk a `Row(spacedBy 10dp, fillMaxWidth)` of weight-1 `NovelCardItem`s, padded with
       weight-1 `Spacer`s to fill the row. The `LazyColumn` item key is
       `it.joinToString { b -> b.id.toString() }`.
     - Additionally, when `favorites.value.isNotEmpty()`, a trailing `LoadMoreRow`
       (`1312-1322`) with `canLoadMore = state.favoritesCanLoadMore`,
       `loading = state.favoritesLoadingMore`, `idleText = "已显示 ${visibleBooks.size} 本"`,
       `loadText = "加载更多收藏"`.

`EmptyCollectionState` (`NovalPieApp.kt:3622-3641`) — `ElevatedCard`, `Column(padding 16dp, spacedBy 10dp)`:
- `Text("暂无收藏", titleMedium Bold)`
- `Text("登录后同步网页收藏，或先打开网页确认账号状态。", bodyMedium, onSurfaceVariant)`
- `LazyRow(spacedBy 8dp)`: `Button { Text("网页登录") }` → `onOpenLogin`;
  `OutlinedButton { Text("打开网页") }` → `onOpenWeb`

`filterBooks` (`NovalPieApp.kt:3643-3647`) delegates to `bookMatchesQuery` (`BookFilter.kt:7-21`):
blank query matches everything; otherwise case-insensitive `contains` against `title`, `author`,
`status`, `wordCount` (raw digits **and** `NumberFormat.getIntegerInstance(Locale.US)` grouped form),
`updatedAt`, any `tags` entry, or `id.toString()`.

### 3.4 `NovelCardItem` (the bookshelf/search grid tile)

`NovalPieApp.kt:2959-3013` — `internal` (the only non-private composable in this group).

```kotlin
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun NovelCardItem(book: NovelCard, onClick: () -> Unit)
```

`Column(fillMaxWidth, clickable(onClick), spacedBy 6dp)`:
1. `Box(fillMaxWidth, aspectRatio(bookCoverAspectRatio() /* 2f/3f */))` → `BookCover(book.title,
   displayCoverUrl, previewUrl = displayCoverUrl)`.
   `novelDisplayCoverUrl` (`NovelCardFacts.kt:45-47`) prefers `fullCoverUrl` then `coverUrl`, blank-safe.
2. `Text(book.title, titleSmall Bold, maxLines 2, Ellipsis)`
3. `Text(preview.authorLabel, labelMedium, onSurfaceVariant, maxLines 1, Ellipsis)` —
   `authorLabel = book.author?.trim()?.ifNotBlank ?: "作者未知"` (`NovelCardFacts.kt:51`)
4. `preview.originalTitleLabel?.let { Text(it, labelSmall, onSurfaceVariant, maxLines 1, Ellipsis) }` —
   `novelOriginalTitleLabel` (`NovelCardFacts.kt:31-34`) returns `originalTitle` only when non-blank
   **and different from `title.trim()`**.
5. If `platformLabel != null || tags.isNotEmpty()` → `FlowRow(spacedBy 4dp/4dp)`:
   one `NovelSourcePill(platformLabel)` first, then a `NovelTagPill` per tag.
   `novelPlatformLabel` (`NovelCardFacts.kt:36-43`): `"novelPia"` (ignore case) → **NovelPia**;
   `"upload"` → **上传**; any other non-blank value passes through verbatim; blank/null → null.
   `novelCardTags` (`NovelCardFacts.kt:25-29`): trim, drop blanks, distinct — **no cap**.
6. If `facts.isNotEmpty()` → a single `Text(facts.joinToString(" · "), labelSmall, onSurfaceVariant,
   maxLines 3, Ellipsis)`.

**Fact chips for the card** — `novelCardFacts(book)` (`NovelCardFacts.kt:15-23`), in this exact order,
each omitted when its field is null/blank. Numbers are formatted with
`NumberFormat.getIntegerInstance(Locale.US)` (i.e. `1,234,567`):
1. `"状态 $status"` (trimmed, non-blank)
2. `"字数 <grouped wordCount>"`
3. `"收藏 <grouped favoriteCount>"`
4. `"本站阅读 <grouped siteReadCount>"`
5. `"源阅读 <grouped sourceReadCount>"`
6. `"源收藏 <grouped sourceFavoriteCount>"`
7. `"更新 <updatedAt.take(10)>"` ← truncated to 10 chars (date part)

Note the **space-separated** form here vs. the **colon** form used on Book detail (§5.2).

`NovelCard` (`Models.kt:3-20`): `id, title, originalTitle?, author?, platform?, status?, coverUrl?,
description?, wordCount?, favoriteCount?, siteReadCount?, sourceReadCount?, sourceFavoriteCount?,
updatedAt?, tags, fullCoverUrl?`. **`description` is not rendered on the card** (only on Book detail).

### 3.5 Loaders / interactions

- `loadHome()` (`NovalPieViewModel.kt:3750-3776`): optimistically seeds `user` from the decoded auth
  token; runs `api.currentUser()`, `api.favoriteGroups()`, `api.favorites(page 1, limit 20, groupId)`
  concurrently. Error labels: groups → `收藏分组`, favorites → `书架`; user falls back to the
  token-decoded profile, else `apiFailureMessage("登录状态", …)` (source `\u767b\u5f55\u72b6\u6001`,
  decoded = **登录状态**, `NovalPieViewModel.kt:4039`). `favoritesCanLoadMore = size == 20`.
- `loadMoreFavorites()` (`3778-3807`): guards on `favoritesLoadingMore` / `!favoritesCanLoadMore`;
  merges with `mergeBooksById` (`distinctBy { it.id }`); on failure replaces `favorites` with
  `Error(apiFailureMessage("书架", …))` — **existing rows are dropped on a load-more failure**.
- `selectFavoriteGroup(groupId)` (`562-566`): no-op if unchanged, else stores and calls `loadHome()`.
- `updateBookshelfQuery` (`558-560`): pure local filter, no network.
- `continueReading(progress)` (`3689-3697`): switches `currentTab = Collection`, clears the route stack
  and rebuilds it as `Home → BookDetail(bookId) → Reader(bookId, chapterId)`, loading both.
- `clearReaderProgress()` (`3699-3703`): clears the store and both `readerProgress` and
  `recentReaderProgresses` (so the whole 继续阅读 + 阅读记录 area disappears).
- `ReaderProgress` (`Models.kt:43-48`): `bookId, chapterId, chapterTitle?, updatedAtMillis`.
  `ReaderProgressStore` keeps `MAX_RECENT_BOOKS = 20` and `loadRecent` defaults to
  `DEFAULT_RECENT_LIMIT = 5` (`ReaderProgressStore.kt:84-85`) — so 阅读记录 shows at most 5 even
  though the rail would take 8.
- `FavoriteGroup` (`Models.kt:50-54`): `id: Long?`, `name`, `count: Int?`.

---

## 4. Search / Discover — `SearchScreen`

### 4.1 Signature

`NovalPieApp.kt:1328-1353`

```kotlin
@Composable
private fun SearchScreen(
    keyword: String,
    searchHistory: List<String>,
    options: SearchOptions,
    results: LoadResult<List<NovelCard>>,
    tags: LoadResult<List<NovelTag>>,
    onKeywordChange: (String) -> Unit,
    onUseSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onUseTag: (String) -> Unit,
    onRefreshTags: () -> Unit,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit,
    onSearch: (String?) -> Unit,
    searchCanLoadMore: Boolean,
    searchLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onOpenWeb: () -> Unit
)
```

Wiring: `NovalPieApp.kt:238-262`; `onOpenWeb = { openWebFallback("https://novalpie.cc/search?sort_by=relevance") }`.

`SearchOptions` (`NovalPieViewModel.kt:402-410`) with defaults:
`sortBy = "relevance"`, `sortOrder = "desc"`, `scope = "all"`, `matchType = "ai"`,
`adultFilter = "all"`, `source = ""`, `wordCountRange = ""`.

### 4.2 Dynamic section ordering

`NovalPieApp.kt:1354-1361` → `discoverSectionOrder(results, searchHistory.isNotEmpty())`
(`DiscoverPresentation.kt:126-133`) builds the vertical order:

1. `SearchPanel` — always
2. `Results` — only when `results != LoadResult.Idle`
3. `History` — only when `searchHistory.isNotEmpty()`
4. `Tags` — always
5. `Filters` — always
6. `IdlePrompts` — only when `results == LoadResult.Idle`

So after the first search the idle recommendation rail disappears and the results block appears above
history/tags/filters. Container: `LazyColumn(fillMaxSize, contentPadding 12dp, spacedBy 10dp)`.

### 4.3 `DiscoverSearchPanel`

`NovalPieApp.kt:1549-1615`

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverSearchPanel(
    overview: DiscoverOverview,
    keyword: String,
    options: SearchOptions,
    onKeywordChange: (String) -> Unit,
    onSearch: (String?) -> Unit,
    onOpenWeb: () -> Unit
)
```

`submitSearch` = `focusManager.clearFocus(); onSearch(keyword)` (`1560-1563`).

`Column(spacedBy 8dp)`:
1. `Row(SpaceBetween, CenterVertically)`:
   - Left `Column(spacedBy 2dp)`: `Text(overview.title, titleLarge Bold)` = **发现**;
     `Text(overview.subtitle, bodySmall, onSurfaceVariant)` = **搜索作品、作者和标签**
     (from `productHeader(ProductSurface.Discover)`, `ProductCopy.kt:40`, via
     `discoverOverview`, `DiscoverPresentation.kt:33-41`).
   - Right `Row`: `Surface(RoundedCornerShape(4dp), secondaryContainer)` →
     `Text(overview.statusLabel, labelSmall SemiBold, padding 8/4)`;
     `IconButton { Icon(Icons.Filled.OpenInBrowser, contentDescription = "打开网页搜索") }` → `onOpenWeb`.
   - `discoverStatusLabel(results)` (`DiscoverPresentation.kt:43-51`): `Idle` → **就绪**,
     `Loading` → **加载中**, `Error` → **错误**, `Success` → `"${count} 个结果"` where count is the
     collection size (0 if not a collection).
2. `OutlinedTextField(fillMaxWidth, singleLine)` bound to `keyword`/`onKeywordChange`:
   - `label = { Text(overview.hint) }` = **输入关键词、作品名或作者** (`DiscoverPresentation.kt:39`)
   - `trailingIcon = IconButton { Icon(Icons.Filled.Search, contentDescription = "搜索", tint = primary) }`
     → `submitSearch`
   - `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)`,
     `keyboardActions = KeyboardActions(onSearch = { submitSearch() })`
3. `FlowRow(spacedBy 6dp/6dp)` of `LibraryStatPill` over `discoverSelectedFilterSummaries(options)` —
   the live filter summary chips (§4.4).

### 4.4 Selected-filter summary pills

`discoverSelectedFilterSummaries(options)` (`ProductCopy.kt:56-88`) returns exactly 7 strings, in order,
each `"<组名>: <所选项标签>"` (falling back to the raw value if unmapped):

`排序: …`, `顺序: …`, `范围: …`, `内容: …`, `字数: …`, `来源: …`, `模式: …`

(There is a parallel unused list of just the group names: `discoverFilterLabels()` =
`["排序","顺序","范围","内容","字数","来源","模式"]`, `ProductCopy.kt:53-54`.)

### 4.5 `SearchOptionSection` + `FilterChoiceRail` — the complete filter contract

`SearchOptionSection` (`NovalPieApp.kt:1648-1687`) — `Surface(fillMaxWidth, 8dp, surface)`,
`Column(padding 10dp, spacedBy 8dp)`:
- Header `Row(spacedBy 6dp)`: `Icon(Icons.Filled.Tune, contentDescription = null, tint = primary)` +
  `Text("筛选", titleSmall Bold)`
- `Column(spacedBy 12dp)` over `discoverFilterGroups(options)`, each rendered as a `FilterChoiceRail`.
- The callback is chosen by matching the **Chinese group label** (`1673-1681`):
  `"排序"`→`onSortByChange`, `"顺序"`→`onSortOrderChange`, `"范围"`→`onScopeChange`,
  `"内容"`→`onAdultFilterChange`, `"字数"`→`onWordCountRangeChange`, `"来源"`→`onSourceChange`,
  **else**→`onMatchTypeChange`. (Renaming any label silently rewires the screen — a refactor hazard.)

`FilterChoiceRail(group, onSelected)` (`NovalPieApp.kt:1689-1707`): `Column(spacedBy 6dp)` →
`Text(group.label, titleSmall Bold)` + `LazyRow(spacedBy 8dp, contentPadding horizontal 2dp)` of
`FilterChip(selected = choice.selected, onClick = { onSelected(choice.value) }, label = choice.label)`.

`discoverFilterGroups(options)` (`DiscoverPresentation.kt:53-113`) — **every group, every option value
and its label, in source order**:

**1. 排序 (`sortBy`)** — 9 options
| value | label |
|---|---|
| `relevance` | 相关度 |
| `updated_at` | 更新时间 |
| `created_at` | 上架时间 |
| `favorite_count` | 收藏数 |
| `site_read_count` | 本站阅读 |
| `recommend` | 推荐 |
| `source_read_count` | 源阅读 |
| `word_count` | 字数 |
| `source_favorite_count` | 源收藏 |

**2. 顺序 (`sortOrder`)** — 2 options
| value | label |
|---|---|
| `desc` | 降序 |
| `asc` | 升序 |

**3. 范围 (`scope`)** — 4 options
| value | label |
|---|---|
| `all` | 全部内容 |
| `title` | 仅标题 |
| `author` | 仅作者 |
| `tags` | 仅标签 |

**4. 内容 (`adultFilter`)** — 3 options
| value | label |
|---|---|
| `all` | 所有 |
| `adult_only` | 仅成人 |
| `unrestricted` | 全年龄 |

**5. 字数 (`wordCountRange`)** — 5 options, from `searchWordCountRangeChoices()` (`ProductCopy.kt:90-97`)
| value | label |
|---|---|
| `""` | 不限 |
| `..100000` | 10万以下 |
| `100000..500000` | 10-50万 |
| `500000..1000000` | 50-100万 |
| `1000000..` | 100万以上 |

Parsed into API params by `searchMinWordCount` / `searchMaxWordCount` (`ProductCopy.kt:99-103`):
min = text before `".."`, max = text after `".."`, each `toLongOrNull()` and null when blank.

**6. 来源 (`source`)** — 3 options
| value | label |
|---|---|
| `""` | 全部 |
| `novelPia` | NovelPia |
| `upload` | 上传 |

**7. 模式 (`matchType`)** — 4 options
| value | label |
|---|---|
| `ai` | AI搜索 |
| `fuzzy_strict` | 模糊-严格 |
| `fuzzy_loose` | 模糊-宽松 |
| `exact` | 精确匹配 |

`DiscoverFilterChoice` (`DiscoverPresentation.kt:13-17`) = `value, label, selected`;
selection computed by `discoverChoices` (`135-141`) as `selected == value`.

There is also an **unused** generic chip row `ChoiceChips(label, selected, choices, onSelected)`
(`NovalPieApp.kt:1709-1724`) — `Text(label, labelLarge)` + `LazyRow` of `FilterChip`s keyed by
`choice.first`. Not referenced anywhere.

### 4.6 Results block — `searchResultItems`

`NovalPieApp.kt:1418-1459` (a `LazyListScope` extension, not a composable):

```kotlin
private fun LazyListScope.searchResultItems(
    results: LoadResult<List<NovelCard>>,
    searchCanLoadMore: Boolean,
    searchLoadingMore: Boolean,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenBook: (Long) -> Unit
)
```

- `Idle` → nothing (and the section isn't even in the order — see §4.2)
- `Loading` → `LoadingBlock("正在请求 NovalPie 搜索")`
- `Error` → `ErrorBlock(results.message, retryLabel = retryActionLabel("搜索") /* 重试搜索 */,
  onRetry = onSearch)` where `onSearch` here is `{ onSearch(null) }` (`1377`) → re-runs with the
  current keyword.
- `Success` empty → `StatusText("没有找到搜索结果")`
- `Success` non-empty → same manual 2-column grid as the bookshelf (`chunked(2)`, weight-1
  `NovelCardItem`, `Spacer` padding, key = joined ids), followed by `LoadMoreRow` with
  `idleText = "已显示 ${results.value.size} 个结果"` and `loadText = "加载更多结果"`.

### 4.7 `SearchTagSection` (hot tags)

`NovalPieApp.kt:1461-1508`. `Surface(fillMaxWidth, RoundedCornerShape(8dp), surface)`,
`Column(padding 12dp, spacedBy 8dp)`:
1. `Row(SpaceBetween, CenterVertically)`: `Text("热门标签", titleSmall Bold)`;
   `TextButton { Icon(Icons.Filled.Refresh, contentDescription = null); Spacer(4dp); Text("刷新") }`
   → `onRefresh`.
2. `when (tags)`:
   - `Idle` → `StatusText("打开发现页后同步网站标签")`
   - `Loading` → full-width `LinearProgressIndicator`
   - `Error` → `ErrorBlock(tags.message, retryLabel = "重试标签", onRetry = onRefresh)`
   - `Success` empty → `StatusText("暂无可显示标签")`
   - `Success` non-empty → `LazyRow(spacedBy 8dp)` of
     `FilterChip(selected = false, onClick = { onUseTag(tag.name) },
     label = discoverTagLabels(listOf(tag)).single())`.
     `discoverTagLabels` (`DiscoverPresentation.kt:121-124`): `"${tag.name} ${tag.count}"` when
     `count != null`, else just `tag.name`.

`NovelTag` (`Models.kt:22-26`): `id?, name, count?`.
`loadSearchTags()` (`NovalPieViewModel.kt:3909-3916`): skips if already Loading; calls
`api.tags(sort = "count", limit = 24)`; error label is the mojibake `"鏍囩"`.

> **BUG:** `NovalPieViewModel.kt:3914` holds `"鏍囩"` — double-encoded `标签`. A tag load failure
> renders `鏍囩请求失败: …` instead of `标签请求失败: …`.

### 4.8 `SearchHistorySection`

`NovalPieApp.kt:1617-1646`. `Surface(fillMaxWidth, 8dp, surface)`, `Column(padding 12dp, spacedBy 8dp)`:
1. `Row(SpaceBetween, CenterVertically)`: `Text("搜索历史", titleMedium Bold)`;
   `TextButton { Text("清空") }` → `onClear`.
2. `LazyRow(spacedBy 8dp)` over `history` (key = the keyword string): each an
   `OutlinedButton { Text(keyword, maxLines 1, Ellipsis) }` → `onUseKeyword(keyword)`.

Backed by `SearchHistoryStore` with `MAX_HISTORY = 10` (`SearchHistoryStore.kt:32`); newest first,
de-duplicated (`SearchHistoryStore.kt:22`). `useSearchHistory(keyword)` simply calls
`performSearch(keyword)` (`NovalPieViewModel.kt:573-575`); `clearSearchHistory()` wipes the store and
the in-memory list (`581-584`) — which makes the whole History section disappear.

### 4.9 `DiscoverIdlePanel` (idle recommendation rail)

`NovalPieApp.kt:1510-1533`. `ElevatedCard`, `Column(padding 14dp, spacedBy 10dp)`:
1. `Text(discoverIdleMessage(), bodyMedium)` = **输入关键词后搜索，也可以先看推荐方向。**
   (`DiscoverPresentation.kt:118-119`)
2. `LazyRow(spacedBy 8dp)` over `discoverQuickPrompts()` (`DiscoverPresentation.kt:115-116`) =
   **最近更新 / 热门书评 / 长篇连载 / 完结作品**. Each is a `FilterChip(selected = false)` whose
   click runs both `onUsePrompt(prompt)` (→ `updateSearchKeyword`) and `onSearch(prompt)`
   (→ `performSearch(prompt)`).

Also present but **unused**: `DiscoverEmptyResultPanel()` (`NovalPieApp.kt:1535-1547`) —
`ElevatedCard` with `Text("没有匹配结果", titleSmall Bold)` and
`Text("可以换一个关键词，或调整范围、匹配方式和内容筛选。", bodySmall, onSurfaceVariant)`. And
`SearchResultHeader(results)` (`2924-2933`) — `Text("搜索状态: <就绪|加载中|错误|N 个结果>", labelLarge)`.
And `discoverPrimaryActions()` = `["搜索","网页发现"]` (`ProductCopy.kt:50-51`).

### 4.10 Search interaction semantics

- Every `updateSearch*` setter (`NovalPieViewModel.kt:586-626`) calls `invalidateSearchRequests()`
  when the value actually changes (bumping the request serial, clearing `searchCanLoadMore` /
  `searchLoadingMore`) and persists via `saveSearchOptions()` → `searchSettingsStore`.
  `updateSearchKeyword` (`568-571`) invalidates but does **not** persist.
- `performSearch(submittedKeyword)` (`3809-3852`): keyword = `searchKeywordForSubmission(current,
  submitted)` = `(submitted ?: current).trim()` (`RequestFreshness.kt:50-51`). **A blank keyword is
  still submitted** — no guard. Resets results to `Loading`, page to 1, saves the keyword to history
  (so history refreshes immediately), calls `api.search(keyword, page 1, limit 20, sortBy, sortOrder,
  scope, matchType, adultFilter, source, minWordCount, maxWordCount)`, discards stale responses via
  `isFreshSearchResult`, sets `searchResults = result.toLoadResult(VisibleUiLabels.Search /* 搜索 */)`
  and `searchCanLoadMore = size == 20`.
- `loadMoreSearch()` (`3854-3907`): guards on loading/can-load-more, requests the next page with the
  identical option set, merges via `mergeBooksById`.
- `useSearchTag(tagName)` → `performSearch(tagName)` (`577-579`) — note it does not first push the
  tag into the text field, so the field is updated only because `performSearch` writes
  `searchKeyword = keyword`.

---

## 5. Book detail — `BookDetailScreen`

### 5.1 Signature

`NovalPieApp.kt:1726-1746`

```kotlin
@Composable
private fun BookDetailScreen(
    state: BookDetailState,
    readerProgress: ReaderProgress?,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyComment: (ChapterComment) -> Unit,
    onCancelCommentReply: () -> Unit,
    onCommentLike: (ChapterComment) -> Unit,
    onCommentDislike: (ChapterComment) -> Unit,
    onCommentEmoji: (ChapterComment) -> Unit,
    onCommentAward: (ChapterComment) -> Unit,
    onOpenWeb: () -> Unit
)
```

Wiring (`NovalPieApp.kt:482-501`): `readerProgress = viewModel.bookDetailState.readerProgress` (the
per-book progress, not the global one); `onRetry = { loadBookDetail(route.bookId) }`;
`onEditInfo = { openBookEditInfo(route.bookId) }`; `onManageChapters = { openBookChapters(route.bookId) }`;
`onAppendChapters = { openBookAppend(route.bookId) }`;
`onOpenWeb = { openWebFallback("https://novalpie.cc/book/${route.bookId}") }`.

`BookDetailState` (`NovalPieViewModel.kt:345-357`): `bookId`, `book: LoadResult<NovelCard>`,
`chapters: LoadResult<List<Chapter>>`, `comments: LoadResult<List<ChapterComment>>`,
`favoriteStatus: LoadResult<FavoriteStatus>`, `readerProgress?`, `commentDraft`,
`replyingToCommentId?`, `replyingToName?`, `actionMessage?`, `actionLoading`.

Local derivations (`NovalPieApp.kt:1747-1749`):
- `sectionTitles = bookDetailSectionTitles()` = `["作品","阅读","章节目录","评论区"]`
  (`ProductCopy.kt:105-106`). **Only indices 2 and 3 are rendered**; `作品` and `阅读` are unused.
- `firstChapter = (state.chapters as? Success)?.value?.firstOrNull()`
- `progressForBook = readerProgress?.takeIf { it.bookId == state.bookId }`

### 5.2 Visual structure, top to bottom

`LazyColumn(fillMaxSize, contentPadding 12dp, spacedBy 10dp)` (`1750-1754`).

1. **Hero slot** — `when (state.book)` (`1755-1772`):
   - `Idle` → `StatusText("等待加载书籍详情")`
   - `Loading` → `LoadingBlock("正在加载书籍详情")`
   - `Error` → `ErrorBlock(book.message, retryLabel = retryActionLabel("书籍详情") /* 重试书籍详情 */, onRetry)`
   - `Success` → `BookDetailHero(...)`
2. **Catalog heading** (`1773`) — `Text(sectionTitles[2], titleMedium Bold)` = **章节目录**
3. **Catalog filter** (`1774`) — `CatalogFilterField(catalogQuery, onCatalogQueryChange)`,
   label **筛选目录**
4. **Catalog body** — `when (state.chapters)` (`1775-1802`):
   - `Idle` → `StatusText("等待加载章节")`
   - `Loading` → `LoadingBlock("正在加载章节目录")`
   - `Error` → `ErrorBlock(chapters.message, retryLabel = 重试章节目录, onRetry)`
   - `Success`:
     - `visible = filterChapters(chapters.value, catalogQuery)`
     - `CatalogSummaryText(catalogSummaryLabel(all, visible,
       currentChapterId = readerProgress?.takeIf { it.bookId == state.bookId }?.chapterId))`
     - `chapters.value.isEmpty()` → `StatusText("章节目录为空，可打开网页详情。")`
     - `visible.isEmpty()` → `StatusText("没有匹配的章节")`
     - else → one `ChapterRow` per visible chapter (key = `it.id`), `selected =
       isBookDetailProgressChapter(state.bookId, chapter.id, readerProgress)`, click →
       `onOpenReader(state.bookId, chapter.id)`
5. **Comments section** (`1803-1819`) — `BookCommentsSection(title = sectionTitles[3] /* 评论区 */, …)`

`catalogSummaryLabel(allChapters, visibleChapters, currentChapterId)` (`CatalogSummary.kt:5-23`):
- empty catalog → **目录未加载**
- otherwise parts joined with `" · "`: always `"共 ${all.size} 章"`; plus
  `"已筛选 ${visible.size} 章"` when the counts differ; plus `"当前第 ${index+1} 章"` when
  `currentChapterId` is found in the list.

`isBookDetailProgressChapter` (`BookDetailProgressMarker.kt:5-10`): true iff
`progress?.bookId == bookId && progress.chapterId == chapterId`.

`filterChapters` (`NovalPieApp.kt:3649-3653`) → `chapterMatchesQuery` (`ChapterFilter.kt:7-19`):
blank matches all; else case-insensitive `contains` on `title`, `number.toString()`, `wordCount`
(raw and `Locale.US`-grouped), `updatedAt`, `id.toString()`.

`Chapter` (`Models.kt:28-34`): `id, title, number?, wordCount?, updatedAt?`.

### 5.3 `BookDetailHero`

`NovalPieApp.kt:3068-3148`

```kotlin
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BookDetailHero(
    book: NovelCard,
    favoriteStatus: LoadResult<FavoriteStatus>,
    progress: ReaderProgress?,
    firstChapter: Chapter?,
    onOpenReader: (Long, Long) -> Unit,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
    onOpenWeb: () -> Unit
)
```

`Surface(fillMaxWidth, RoundedCornerShape(8dp), surface)`, `Column(padding 12dp, spacedBy 12dp)`:
1. `Row(spacedBy 12dp)`:
   - `BookCover(book.title, displayCoverUrl, width = 100.dp, height = 150.dp, previewUrl = displayCoverUrl)`
   - weight-1 `Column(spacedBy 7dp)`:
     - `Text(book.title, titleLarge Bold, maxLines 3, Ellipsis)`
     - `novelOriginalTitleLabel(book)?.let { Text(it, bodySmall, onSurfaceVariant) }`
     - `BookDetailFavoriteChip(favoriteStatus)`
     - `progress?.chapterTitle?.takeIf { it.isNotBlank() }?.let { Text("上次读到: $it", bodySmall,
       onSurfaceVariant) }`
2. **Facts chips** — if `bookDetailFacts(book).isNotEmpty()` → `FlowRow(spacedBy 6dp/6dp)` of
   `BookDetailFactLabel(fact)`.
3. **Tag chips** — if `book.tags.isNotEmpty()` → `FlowRow(spacedBy 6dp/6dp)` of `NovelTagPill(tag)`
   (**all** tags, no cap, no de-dupe here).
4. **Description** — `book.description?.takeIf { it.isNotBlank() }?.let { Text(it, bodyMedium,
   maxLines 6, Ellipsis) }`.
5. `BookDetailActionRow(...)`.

**`bookDetailFacts(book)`** (`BookDetailFacts.kt:7-17`) — colon-separated, order-fixed, each entry
skipped when the field is null/blank. Counts use `NumberFormat.getIntegerInstance(Locale.US)`:
1. `"状态: $status"`
2. `"作者: $author"`
3. `"来源: ${novelPlatformLabel(platform)}"` (NovelPia / 上传 / raw)
4. `"字数: <grouped>"`
5. `"收藏: <grouped>"`
6. `"本站阅读: <grouped>"`
7. `"源阅读: <grouped>"`
8. `"源收藏: <grouped>"`
9. `"更新: $updatedAt"` ← **full string, not truncated** (unlike the card's `take(10)`)

**`BookDetailFavoriteChip`** (`NovalPieApp.kt:3150-3166`) — `Surface(4dp, primaryContainer)` →
`Text(label, labelSmall SemiBold, onPrimaryContainer, padding 8/4)`. Label by state
(`BookDetailPresentation.kt:12-17`):
- `Idle` / `Loading` → `bookDetailFavoriteLoadingLabel()` = **收藏同步中**
- `Error` → `bookDetailFavoriteUnavailableLabel()` = **收藏状态不可用**
- `Success` → `bookDetailFavoriteLabel(isFavorited)` = **已收藏** / **未收藏**

`FavoriteStatus` (`Models.kt:56-60`): `isFavorited, groupId?, rawState?`. On this screen only
`isFavorited` is used; `groupId`/`rawState` are rendered only by the **unused** `FavoriteStatusCard`
(§5.6).

**`BookDetailActionRow`** (`NovalPieApp.kt:3180-3214`) — `FlowRow(spacedBy 8dp/8dp)`.
Labels come from `bookDetailPrimaryActions(hasProgress)` (`BookDetailPresentation.kt:5-10`):
- with progress: `["继续阅读", "开始阅读", "网页详情"]`
- without: `["开始阅读", "网页详情"]`

Resolved as `continueLabel = actions.first()`, `startLabel = if (progress != null)
actions.getOrElse(1) { "开始阅读" } else actions.first()`, `webLabel = actions.last()`. Buttons in order:
1. **继续阅读** `Button` — rendered **only when `progress != null`** → `onOpenReader(progress.bookId,
   progress.chapterId)`
2. **开始阅读** `Button(enabled = firstChapter != null)` → `onOpenReader(bookId, firstChapter.id)`
3. **网页详情** `OutlinedButton` → `onOpenWeb`
4. **编辑信息** `OutlinedButton` → `onEditInfo` (hard-coded literal, `3210`)
5. **章节管理** `OutlinedButton` → `onManageChapters` (`3211`)
6. **追加章节** `OutlinedButton` → `onAppendChapters` (`3212`)

Buttons 4–6 are **always visible regardless of login or ownership**; the gate lives in the ViewModel —
`openBookEditInfo` / `openBookChapters` / `openBookAppend` all redirect to `openLoginFallback()` when
`authToken` is null/blank (`NovalPieViewModel.kt:3152-3160`, `3352-3360`, `3659-3668`).

**`BookCover`** (`NovalPieApp.kt:3349-3397`)

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCover(
    title: String,
    coverUrl: String?,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    modifier: Modifier = Modifier,
    previewUrl: String? = coverUrl
)
```
- shape `RoundedCornerShape(6dp)`, background `surfaceVariant`; sizing: explicit width/height when
  given, `fillMaxSize()` when neither is given.
- When `previewUrl` is non-blank the box gets `combinedClickable(onClick, onLongClick)` — **both**
  open the preview dialog.
- `coverUrl` blank → `BookCoverFallbackText(bookCoverFallbackText(title))`; the fallback text is
  `title.trim().firstOrNull()?.toString() ?: "N"` (`3409-3410`), rendered `titleLarge Bold
  onSurfaceVariant` (`3399-3407`).
- Otherwise Coil `SubcomposeAsyncImage`, request `.size(1024, 1536).precision(EXACT).crossfade(true)`,
  `contentDescription = title`, `ContentScale.Crop`; both `loading` and `error` slots show the same
  fallback letter.
- On preview: `ImagePreviewDialog(imageUrl = previewUrl, title = "$title · 封面", onDismiss = …)`
  (`3395`).

### 5.4 `BookCommentsSection` + `BookCommentRow`

`BookCommentsSection` (`NovalPieApp.kt:3216-3278`) — `ElevatedCard`, `Column(padding 16dp, spacedBy 10dp)`:
1. `Row(SpaceBetween, CenterVertically)`:
   - `Text(title.ifBlank { bookCommentsSectionTitle() /* 评论区 */ }, titleMedium Bold)`
   - `OutlinedButton { Icon(Icons.Filled.OpenInBrowser, contentDescription = bookCommentsFallbackLabel(),
     18×18); Spacer(4dp); Text(bookCommentsFallbackLabel(), maxLines 1, Ellipsis) }` → `onOpenWeb`;
     `bookCommentsFallbackLabel()` = **打开网页评论** (`BookDetailPresentation.kt:24`)
2. `ForumCommentComposer(draft = state.commentDraft, replyingToName = state.replyingToName,
   loading = state.actionLoading, message = state.actionMessage, …)` — the card-wrapped composer
   (strings: 回复 X / 取消 / 写评论 / 发送中 / 发送)
3. `when (comments)`:
   - `Idle` → `StatusText("等待加载评论")`
   - `Loading` → `LoadingBlock("正在同步评论")`
   - `Error` → `ErrorBlock(comments.message, retryLabel = retryActionLabel("评论区") /* 重试评论区 */, onRetry)`
   - `Success` empty → `StatusText("还没有评论")`
   - `Success` non-empty → **only the first 6 comments** (`comments.value.take(6)`, `3263`) rendered
     as `BookCommentRow`. There is **no load-more and no "see all" affordance** — comments 7+ are
     only reachable via 打开网页评论.

`BookCommentRow` (`NovalPieApp.kt:3280-3320`) — `Surface(fillMaxWidth, 8dp, surfaceVariant)`,
`Column(padding 12dp, spacedBy 8dp)`:
1. `Row(SpaceBetween)`: `Text(comment.authorName ?: "匿名用户", SemiBold)` (**not clickable here**,
   unlike the forum row) and `createdAt?.let { Text(it, labelSmall, onSurfaceVariant) }`
2. `comment.replyToName?.let { Text("回复 $it", labelSmall, primary) }`
3. Paragraphs from `readerParagraphsFromContent(comment.content).ifEmpty { listOf(comment.content) }`
   as `Text(bodyMedium)`
4. `ForumLinkPreviewRows(forumContentLinks(paragraphs))`
5. `ChapterCommentActionRow(comment, …)` → 赞 N / 踩 N / 表情 N / 打赏 N / 回复

`ChapterComment` (`Models.kt:607-621`): `id, bookId?, chapterId?, parentCommentId?, authorName?,
replyToName?, content, likeCount?, dislikeCount?, reactionCount?, awardPoints?, createdAt?, authorId?`.
`authorId` is carried but **never used** on this screen.

Unused helper: `bookCommentMetricLabels(comment)` (`BookDetailPresentation.kt:19-20`) delegates to
`chapterCommentMetricLabels` (`ReaderPresentation.kt:33-40`) = `["赞 N","踩 N","表情 N","打赏 N","回复"]`
— the same strings the action row builds inline.

### 5.5 Loaders / interactions

- `loadBookDetail(bookId)` (`NovalPieViewModel.kt:3918-3956`): resets everything to Loading and seeds
  `readerProgress = readerProgressStore.load(bookId)`. Fires 5 parallel calls: `api.bookDetail`,
  `api.bookCoverPhoto` (used to overwrite `fullCoverUrl` with the original-resolution cover, `3942-3948`),
  `api.chapters`, `api.bookComments(page 1, limit 20)`, `api.favoriteStatus`. Two freshness gates
  (`isFreshBookDetailResult`). Error labels: book → `书籍详情`, chapters → `章节目录`,
  comments → `评论区`, favoriteStatus → `收藏状态` (source `\u6536\u85cf\u72b6\u6001`, decoded).
- Comment interactions mirror the forum ones (`2949-3041`):
  - `replyToBookComment` pre-fills `"@name "` into a blank draft
  - `submitBookComment` posts `api.createCommentReply(...)` when replying, else
    `api.createBookComment(bookId, content)`; success message `it.message ?: "评论已提交"`;
    failure `apiFailureMessage("评论提交", …)`; then reloads
  - reactions: `likeBookComment` → `api.toggleCommentLike`, others via
    `reactToCommentOrReply(comment, "down" | "emoji:heart" | "award" + awardPoints = 10)` which routes
    to `api.reactToCommentReply` when `parentCommentId != null` else `api.reactToComment`
    (`3139-3151`). Labels: **评论点赞 / 评论点踩 / 评论表情 / 评论打赏**; success
    `"$label 已同步"`.
- `updateBookCatalogQuery` (`628-630`) is pure local state.

### 5.6 Dead composables in the Book-detail neighbourhood (present in source, never called)

- `FavoriteStatusCard(status)` (`NovalPieApp.kt:3049-3066`) — `ElevatedCard`, `Text("收藏状态",
  titleMedium Bold)`; `Idle` → `StatusText("等待读取收藏状态")`; `Loading` →
  `LoadingBlock("正在检查 /api/favorites/status")`; `Error` → `Text(status.message, bodySmall)`;
  `Success` → `Text(if (isFavorited) "当前账号已收藏" else "当前账号未收藏", Bold)`,
  `groupId?.let { Text("分组 id: $it", bodySmall) }`, `rawState?.let { Text("原始状态: $it", bodySmall) }`.
- `BookSummary(book)` (`3322-3347`) — `ElevatedCard` + `BookCover(104dp × 148dp)` +
  `Text(title, headlineSmall Bold)` + `LazyRow` of `AssistChip`s over `bookDetailFacts(book)` +
  description (maxLines 5) + `LazyRow` of `AssistChip`s over `book.tags.take(8)`.
- `GroupSection(groups, selectedGroupId, onGroupSelected)` (`2790-2830`) — `ElevatedCard`,
  `Text("收藏分组", titleMedium Bold)`, `Idle`→`等待加载分组`, `Loading`→`正在加载收藏分组`,
  `Error`→raw message, `Success` empty→`StatusText("暂无分组")`, else the same 全部 + first-8 chip rail
  as `LibraryShelfControls`. **This is the only variant with the `暂无分组` empty state.**
- `HeroCard(title, subtitle, semanticsMarker)` (`2903-2922`) and
  `ProductHeaderBlock(header)` (`2891-2901`).

---

## 6. Reader — `ReaderScreen`

### 6.1 Signature

`NovalPieApp.kt:1823-1844`

```kotlin
@Composable
private fun ReaderScreen(
    state: ReaderState,
    options: ReaderUiOptions,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onCycleTheme: () -> Unit,
    onRetry: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onBack: () -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyComment: (ChapterComment) -> Unit,
    onCancelCommentReply: () -> Unit,
    onCommentLike: (ChapterComment) -> Unit,
    onCommentDislike: (ChapterComment) -> Unit,
    onCommentEmoji: (ChapterComment) -> Unit,
    onCommentAward: (ChapterComment) -> Unit,
    onOpenWeb: () -> Unit
)
```

Wiring (`NovalPieApp.kt:550-570`): `onRetry = { loadReader(route.bookId, route.chapterId) }`;
`onBack = viewModel::goBack`;
`onOpenWeb = { openWebFallback("https://novalpie.cc/book/${route.bookId}/${route.chapterId}") }`.

`ReaderState` (`NovalPieViewModel.kt:389-400`): `bookId`, `chapterId`,
`content: LoadResult<ReaderContent>`, `chapters: LoadResult<List<Chapter>>`,
`comments: LoadResult<List<ChapterComment>>`, `commentDraft`, `replyingToCommentId?`,
`replyingToName?`, `actionMessage?`, `actionLoading`.
`ReaderUiOptions` (`412-415`): `fontSizeSp: Int = 18`, `theme: String = "system"`.
`ReaderContent` (`Models.kt:36-41`): `title?`, `content`, `source`, `illustrations: List<ChapterIllustration>`.
`ChapterIllustration` (`Models.kt:112-116`): `id`, `index`, `src`.

### 6.2 Layout, chrome toggling, and the immersive shell

`NovalPieApp.kt:1845-1934`. Local state:
- `chapters = (state.chapters as? Success)?.value.orEmpty()` (`1845`)
- `catalogVisible = remember { mutableStateOf(false) }` (`1846`)
- `toolbarsVisible = remember { mutableStateOf(false) }` (`1847`) — **starts hidden**
- `listState = rememberLazyListState()` (`1848`, created but never read)

Root `Box(fillMaxSize)`:
1. **Content `LazyColumn`** (`1851-1883`) with `state = listState`, an indication-less
   `clickable` that toggles `toolbarsVisible` on any tap (`1853-1856`),
   `contentPadding(start 16, top 24, end 16, bottom 48)`, `spacedBy 12dp`. Two items:
   - `when (state.content)` (`1861-1866`):
     - `Idle` → `StatusText("等待加载正文")`
     - `Loading` → `LoadingBlock("正在加载正文")`
     - `Error` → `ErrorBlock(content.message, retryLabel = retryActionLabel("正文") /* 重试正文 */, onRetry)`
     - `Success` → `ReaderBody(content.value, options)`
   - `ReaderChapterCommentsSection(...)` (`1868-1882`)
2. **Top bar** (`1885-1897`) — `AnimatedVisibility(visible = toolbarsVisible.value,
   enter = slideInVertically(-it), exit = slideOutVertically(-it))`, aligned `TopCenter` →
   `ReaderTopBar`.
3. **Catalog panel** (`1899-1914`) — plain `if (catalogVisible.value)`, aligned `BottomCenter` →
   `ReaderCatalogPanel`. Opening a chapter from it sets `catalogVisible = false` **and**
   `toolbarsVisible = false` before `onOpenReader(state.bookId, chapterId)` (`1906-1910`).
4. **Bottom toolbar** (`1916-1933`) — `AnimatedVisibility(visible = toolbarsVisible.value &&
   !catalogVisible.value, slide from/to bottom)`, aligned `BottomCenter` → `ReaderToolbar`, with
   `onOpenCatalog = { catalogVisible.value = true }`.

Because `globalProductTopBarVisible` excludes `AppRoute.Reader` (`ReaderPresentation.kt:30-31`) and
the bottom `NavigationBar` only appears on tab roots, the Reader is fully immersive; the only way
back is the in-bar 返回 or the system back handler.

### 6.3 `ReaderTopBar`

`NovalPieApp.kt:2000-2037`. `Surface(fillMaxWidth, color = surfaceVariant,
contentColor = onSurface, tonalElevation = 6dp)`, Row padding 12/10, `SpaceBetween`:
- `TextButton { Text(labels.back) }` = **返回** → `onBack`
- weight-1 centered `Column(spacedBy 2dp)`:
  - `Text(labels.title, titleMedium Bold)` = **阅读**
  - `Text(readerChapterProgressLabel(state.chapterId, chapters), labelMedium, onSurfaceVariant,
    maxLines 1, Ellipsis)`
- `TextButton { Text(labels.web) }` = **网页** → `onOpenWeb`

`readerTopBarLabels()` (`ReaderPresentation.kt:27-28`) = `ReaderTopBarLabels(back = "返回",
title = "阅读", web = "网页")`.

**Progress summary** — `readerChapterProgressLabel(currentChapterId, chapters)`
(`ReaderProgressLabel.kt:5-13`), three cases:
1. `chapters.isEmpty()` → `"当前章节 $currentChapterId · 目录未加载"`
2. chapter id not found → `"当前章节 $currentChapterId · 目录共 ${chapters.size} 章"`
3. found at `index` → `"第 ${index + 1} / ${chapters.size} 章 · ${chapter.title}"`

### 6.4 `ReaderToolbar` — font, theme, prev/next gating

`NovalPieApp.kt:2181-2220`

```kotlin
private fun ReaderToolbar(
    state: ReaderState,
    chapters: List<Chapter>,
    options: ReaderUiOptions,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onOpenWeb: () -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier
)
```

`adjacent = adjacentReaderChapters(state.chapterId, chapters)` (`ReaderAdjacentChapter.kt:10-21`):
finds the current index; if not found returns `(null, null)`; else `previous = chapters[i-1]`,
`next = chapters[i+1]` (both null-safe via `getOrNull`).

`labels = readerToolbarLabels()` (`ReaderPresentation.kt:11-12`) =
`["上一章", "目录", "下一章", "A-", "A+", "主题", "网页"]`. **Index 5 (`主题`) is never used** — the
theme button shows the current theme name instead.

`Surface(fillMaxWidth, surfaceVariant, tonalElevation 6dp)` → `LazyRow(padding 12/10, spacedBy 8dp,
CenterVertically)` with items in this exact order (`2208-2218`):
1. `TextButton(enabled = previous != null) { Text("上一章") }` → `onOpenReader(bookId, previous.id)`
2. `TextButton { Text("目录") }` → `onOpenCatalog`
3. `TextButton(enabled = next != null) { Text("下一章") }` → `onOpenReader(bookId, next.id)`
4. `Spacer(8dp)`
5. `TextButton { Text("A-") }` → `onDecreaseFont`
6. `Text("${options.fontSizeSp}sp", labelLarge)` — e.g. `18sp`
7. `TextButton { Text("A+") }` → `onIncreaseFont`
8. `Spacer(8dp)`
9. `TextButton { Text(options.theme.themeLabel()) }` → `onCycleTheme`
10. `TextButton { Text("网页") }` → `onOpenWeb`

**Font size:** `increaseReaderFont` / `decreaseReaderFont` (`NovalPieViewModel.kt:636-646`) step by ±1
and clamp to `ReaderSettingsStore.MIN_FONT_SIZE_SP = 14` … `MAX_FONT_SIZE_SP = 28`
(`ReaderSettingsStore.kt:26-28`, default 18); each change is persisted immediately. Buttons are never
disabled at the bounds — they just stop changing the value.

**Theme cycling:** `cycleReaderTheme` (`NovalPieViewModel.kt:648-656`) —
`"system" → "sepia" → "dark" → "system"` (the `else` branch catches `dark` and anything unknown),
persisted via `readerSettingsStore.saveTheme`.
Button label via `String.themeLabel()` (`NovalPieApp.kt:3562-3566`):
`"sepia"` → **护眼**, `"dark"` → **深色**, else → **系统**.
Palette via `readerPalette(theme)` (`3555-3560`), a `ReaderPalette(background, text, meta)`:
- `"sepia"` → bg `0xFFF4ECD8`, text `0xFF30271B`, meta `0xFF76634B`
- `"dark"` → bg `0xFF111111`, text `0xFFECECEC`, meta `0xFFAAAAAA`
- else → `colorScheme.surface` / `onSurface` / `onSurfaceVariant`

### 6.5 `ReaderCatalogPanel`

`NovalPieApp.kt:1937-1998`

```kotlin
private fun ReaderCatalogPanel(
    state: ReaderState,
    chapters: LoadResult<List<Chapter>>,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenReader: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)
```

`ElevatedCard(fillMaxWidth, padding start 8 / end 8 / **bottom 86dp**, RoundedCornerShape(16dp),
containerColor = surface)`, `Column(padding 12dp, spacedBy 10dp)`:
1. `Row(SpaceBetween, CenterVertically)`:
   - `Text(readerCatalogPanelTitle(), titleMedium Bold)` = **章节目录** (`ReaderPresentation.kt:14`)
   - `TextButton { Text(readerCloseCatalogLabel()) }` = **回到正文** (`ReaderPresentation.kt:16`) → `onClose`
2. `CatalogFilterField(catalogQuery, onCatalogQueryChange)` — label **筛选目录**
3. `when (chapters)`:
   - `Idle` → `StatusText("等待加载目录")`
   - `Loading` → `LoadingBlock("正在加载目录")`
   - `Error` → `ErrorBlock(chapters.message, retryLabel = retryActionLabel("章节目录"), onRetry)`
   - `Success` → `CatalogSummaryText(catalogSummaryLabel(all, visible, currentChapterId = state.chapterId))`
     then a **fixed 300dp-tall inner `LazyColumn(spacedBy 8dp)`**: `visible.isEmpty()` →
     `StatusText("没有匹配的章节")`, else `ChapterRow(chapter, selected = chapter.id == state.chapterId,
     onClick = { onOpenReader(chapter.id) })` per chapter (key = `it.id`).

Note `readerCatalogTitle()` = **章节** (`ProductCopy.kt:110`) and `readerSurfaceSections()` =
`["正文","目录","设置"]` (`ReaderPresentation.kt:18-19`) are defined but unused.

### 6.6 `ReaderBody` — text, `[[img:N]]` resolution, inline illustrations

`NovalPieApp.kt:3451-3494`

```kotlin
private fun ReaderBody(content: ReaderContent, options: ReaderUiOptions)
```

- `palette = readerPalette(options.theme)`
- `imagePlaceholders = remember(content.illustrations) {
     readerImagePlaceholdersFromIllustrations(content.illustrations) }`
- `blocks = remember(content.content, imagePlaceholders) {
     readerBlocksFromContent(content.content, imagePlaceholders = imagePlaceholders) }`
- `ElevatedCard(fillMaxWidth, containerColor = palette.background)`,
  `Column(padding 16dp, spacedBy 12dp)`:
  - `content.title?.let { SelectionContainer { Text(it, titleLarge Bold, color = palette.text) } }`
  - `var imageOrdinal = 0`; for each block:
    - `Text` block → `SelectionContainer { Text(value, bodyLarge, fontSize = options.fontSizeSp.sp,
      lineHeight = (options.fontSizeSp + 8).sp, color = palette.text) }`
    - `Image` block → `ReaderIllustration(image = block, ordinal = ++imageOrdinal, palette = palette)`
  - `readerSourceDebugLine(content.source)?.let { Text(it, labelSmall, color = palette.meta) }` —
    `readerSourceDebugLine` **always returns `null`** (`ReaderPresentation.kt:5-6`), so this line never
    renders. Same for `readerDebugIdentityLine` (`8-9`).

Body text is selectable (`SelectionContainer`); line height is always font size + 8sp.

**`readerImagePlaceholdersFromIllustrations`** (`ReaderText.kt:11-21`): keeps illustrations with
`index > 0 && src.isNotBlank()`, mapping `index → ReaderContentBlock.Image(url = src,
alt = "正文插图 ${index}")`.

**`readerBlocksFromContent(raw, baseUrl = "https://novalpie.cc", imagePlaceholders)`**
(`ReaderText.kt:23-71`) splits the raw chapter HTML into an ordered `List<ReaderContentBlock>`
(`ReaderText.kt:6-9`: `Text(value)` | `Image(url, alt?)`). Recognised image tokens
(`readerImagePattern`, `ReaderText.kt:98-104`, case-insensitive + DOTALL):
1. HTML `<img …>`
2. Markdown `![alt](url "title")`
3. Placeholder `[[img:N]]` — regex `\[\[\s*img\s*:\s*(\d+)\s*]]`, case-insensitive, tolerates
   internal whitespace

Resolution rules:
- `[[img:N]]` → look up `imagePlaceholders[N]`; normalise its URL; if that yields null (missing index
  or unusable URL) **the literal token text is emitted as a text block instead** (`ReaderText.kt:51`),
  so an unresolved `[[img:3]]` shows up verbatim in the prose.
- HTML `<img>` → URL from `data-src`, then `data-original`, then `src` (`57-59`); alt from `alt`.
- Markdown → URL from group 2, alt from group 1.
- `normalizeReaderImageUrl` (`ReaderText.kt:132-141`): rejects `javascript:` URLs; passes through
  `http://`, `https://`, `data:`; `//x` → `https://x`; `/x` → `baseUrl + /x`; anything else →
  `baseUrl + "/" + value`.
- alt values are HTML-decoded (`decodeHtmlValue`, `143-145`) and blank-stripped.

**`readerParagraphsFromContent(raw)`** (`ReaderText.kt:73-91`) — also used by every comment/forum body:
1. `<br>`/`<br/>` → private-use marker `\uE000`; closing `</p|div|section|article|li|h1-6>` → `\uE001`
2. `Html.fromHtml(..., FROM_HTML_MODE_LEGACY)` to decode entities
3. `\u00A0` → space; markers back to `\n` and `\n\n`; CRLF/CR → `\n`
4. split on `\n{2,}`; per paragraph, trim each line, collapse runs of spaces/tabs, drop blank lines,
   rejoin with `\n`; drop empty paragraphs

### 6.7 `ReaderIllustration`

`NovalPieApp.kt:3496-3539`

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderIllustration(image: ReaderContentBlock.Image, ordinal: Int, palette: ReaderPalette)
```

- `label = readerIllustrationLabel(image.alt, ordinal)` (`3541-3542`) = trimmed non-blank `alt`,
  else `"正文插图 ${ordinal.coerceAtLeast(1)}"`
- `ElevatedCard(fillMaxWidth, RoundedCornerShape(12dp), containerColor = palette.background)` with
  `combinedClickable(onClick = { previewVisible = true }, onLongClick = { previewVisible = true })` —
  tap **and** long-press both open the preview
- `Column`:
  - `SubcomposeAsyncImage`, request `.size(2048, 2048).precision(EXACT).crossfade(true)`,
    `contentDescription = readerIllustrationContentDescription(label)` =
    **`"$label，点击或长按查看大图"`** (`3544-3545`),
    `Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 720.dp)`, `ContentScale.Fit`;
    `loading = LoadingBlock(readerIllustrationLoadingLabel())` = **正在加载插图** (`3549`);
    `error = ErrorBlock(readerIllustrationErrorLabel())` = **插图加载失败** (`3551`, no retry button)
  - `Row(fillMaxWidth, padding 12/8, SpaceBetween, CenterVertically)`:
    `Text(label, weight 1, labelMedium, palette.meta, maxLines 1, Ellipsis)` and
    `Text(readerIllustrationPreviewHint(), labelSmall, palette.meta)` = **点击 / 长按看大图** (`3547`)
- On preview → `ImagePreviewDialog(imageUrl = image.url, title = label, onDismiss = …)` (`3537`)

### 6.8 `ReaderChapterCommentsSection` + `ReaderChapterCommentRow`

`ReaderChapterCommentsSection` (`NovalPieApp.kt:2054-2119`) — `Surface(fillMaxWidth,
RoundedCornerShape(8dp), surface)`, `Column(padding 14dp, spacedBy 10dp)`:
1. `Row(SpaceBetween, CenterVertically)`:
   - `Text(chapterCommentsSectionTitle(), titleMedium Bold)` = **章节评论** (`ReaderPresentation.kt:42`)
   - `OutlinedButton { Icon(Icons.Filled.OpenInBrowser, contentDescription =
     chapterCommentsFallbackLabel(), 18×18); Spacer(4dp); Text(chapterCommentsFallbackLabel(),
     maxLines 1, Ellipsis) }` → `onOpenWeb`; label = **打开网页评论** (`ReaderPresentation.kt:44`)
2. `InlineCommentComposer(draft = state.commentDraft, replyingToName = state.replyingToName,
   loading = state.actionLoading, message = state.actionMessage, …)` — the un-carded composer
3. `when (comments)`:
   - `Idle` → `StatusText("等待加载章节评论")`
   - `Loading` → `LoadingBlock("正在同步章节评论")`
   - `Error` → `ErrorBlock(comments.message, retryLabel = retryActionLabel("章节评论") /* 重试章节评论 */, onRetry)`
   - `Success` empty → `StatusText("还没有章节评论")`
   - `Success` non-empty → **all** comments (no `take`), each a `ReaderChapterCommentRow`

`ReaderChapterCommentRow` (`NovalPieApp.kt:2121-2161`) — identical structure/strings to
`BookCommentRow` (§5.4) except the container is `Surface(8dp, surfaceVariant)` at full opacity:
author-or-**匿名用户** + `createdAt`, optional `"回复 $replyToName"`, paragraphs, link previews,
`ChapterCommentActionRow`.

Unused: `ReaderHeader(state, chapters)` (`NovalPieApp.kt:2039-2052`) — `Text(readerScreenTitle()
/* 阅读 */, titleMedium Bold)`, the always-null debug identity line, and the progress label in
`bodyMedium SemiBold`.

### 6.9 `ImagePreviewDialog` (shared by covers and illustrations)

`ImagePreviewDialog.kt:61-164`

```kotlin
@Composable
internal fun ImagePreviewDialog(imageUrl: String, title: String, onDismiss: () -> Unit)
```

- Local state, all re-keyed on `imageUrl`: `scale = 1f`, `offset = Offset.Zero`, `viewport = IntSize.Zero`
- `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true,
  **dismissOnClickOutside = false**))` (`86`)
- Backdrop: `Box(fillMaxSize, background = Color(0xF20B0D12))`, `onSizeChanged { viewport = it }`
- **Image** (`95-127`): `SubcomposeAsyncImage`, request `.size(3072, 3072).precision(EXACT)
  .crossfade(true)`, `contentDescription = "$title 大图"`, `fillMaxSize().padding(top 72dp,
  bottom 72dp)`, `graphicsLayer` applying `scale`/`offset`, `ContentScale.Fit`.
  - `loading` → centered white `CircularProgressIndicator`
  - `error` → centered `Text("大图加载失败", color = White)`
  - Gestures: `detectTapGestures(onDoubleTap = { if (scale > 1.05f) reset() else setScale(2.5f) })`
    (`112-118`) plus `transformable(transformState)` for pinch-zoom + pan (`70-74`).
  - `clampImagePreviewScale(value)` (`52`) = `coerceIn(1f, 6f)`.
    `clampImagePreviewOffset(offset, scale, viewport)` (`54-59`) returns `Offset.Zero` when
    `scale <= 1f` or the viewport is empty; otherwise clamps each axis to
    `±viewport.dimension * (scale - 1) / 2`.
- **Top overlay** (`129-144`): `Surface(align TopCenter, fillMaxWidth, Black α 0.72)`,
  Row padding 12/10:
  - weight-1 `Column`: `Text(title, White, Bold, maxLines 1, Ellipsis)` and
    `Text("双击复原/放大 · 双指缩放 · 放大后拖动", White α 0.72, labelSmall)`
  - `Text("${(scale * 100).toInt()}%", White, labelMedium)` — e.g. `100%`, `250%`
  - `IconButton { Icon(Icons.Filled.Close, "关闭大图", tint = White) }` → `onDismiss`
- **Bottom overlay** (`146-161`): `Surface(align BottomCenter, padding bottom 14dp, Black α 0.72,
  RoundedCornerShape(28dp))`, Row padding 8/4, spacing 2dp, four `IconButton`s:
  1. `Icons.Filled.ZoomOut`, contentDescription **缩小** → `setScale(scale - 0.5f)`
  2. `Icons.Filled.FitScreen`, contentDescription **适应屏幕** → `reset()`
  3. `Icons.Filled.Refresh`, contentDescription **还原** → `reset()`
  4. `Icons.Filled.ZoomIn`, contentDescription **放大** → `setScale(scale + 0.5f)`

  (2 and 3 are functionally identical — both call `reset()`.)

### 6.10 Reader loading & progress persistence

`loadReader(bookId, chapterId)` (`NovalPieViewModel.kt:3958-3988`):
- Resets `readerState` to all-Loading for the requested ids.
- Parallel: `api.chapterContent(chapterId)`, `api.chapters(bookId)`,
  `api.chapterComments(bookId, chapterId, page 1, limit 20)`.
- `chapterTitle` is resolved from the fetched catalog by matching `it.id == chapterId`.
- Freshness gate `isFreshReaderResult(currentRoute, readerState, bookId, chapterId)`.
- **Only if the content call succeeded** → `saveReaderProgress(bookId, chapterId, chapterTitle)`
  (`3977-3979`), which writes the store, refreshes `readerProgress` + `recentReaderProgresses`, and
  syncs `bookDetailState.readerProgress` when the book matches (`3990-3997`).
- Error labels (all `\uXXXX`-escaped in source, decoded): content → **阅读器正文**
  (`\u9605\u8bfb\u5668\u6b63\u6587`), chapters → **阅读器目录**
  (`\u9605\u8bfb\u5668\u76ee\u5f55`), comments → `VisibleUiLabels.ChapterComments` = **章节评论**.
  So a failed body shows `阅读器正文请求失败: …` while the retry button says `重试正文` — the two
  strings intentionally differ.

Reader comment handlers (`NovalPieViewModel.kt:3043-3137`): same shape as the book ones —
`replyToReaderComment` pre-fills `"@name "`; `submitReaderComment` uses `api.createCommentReply` when
replying else `api.createChapterComment(bookId, chapterId, content)`, success
`it.message ?: "评论已提交"`, failure `apiFailureMessage("章节评论提交", …)`; reactions labelled
**章节评论点赞 / 章节评论点踩 / 章节评论表情 / 章节评论打赏**, success `"$label 已同步"`,
`emoji:heart` and `awardPoints = 10` constants, then `loadReader` re-runs.

Route behaviour: `openReader` (`3679-3687`) uses `replaceTopReaderRoute`
(`RouteStackPolicy.kt:8-15`) — if the top of the stack is already a `Reader`, it is **replaced** rather
than pushed, so chapter-to-chapter navigation does not grow the back stack; back from any chapter
returns to whatever was under the first Reader entry.

---

## 7. Cross-screen string index (verbatim content contract)

Grouped by kind; `file:line` is where the literal lives.

**Titles / headers**
`NovalPie` (App.kt:133) · `论坛` + `小说讨论、书评和站内动态` (ProductCopy.kt:113) ·
`书架` + `继续阅读、收藏分组和最近进度` (LibraryPresentation.kt:16-17) ·
`书架` + `收藏、分组和阅读进度` (ProductCopy.kt:39, unused) ·
`发现` + `搜索作品、作者和标签` (ProductCopy.kt:40) · `我的` + `账号、阅读偏好和连接设置` (ProductCopy.kt:41) ·
`作品`/`阅读`/`章节目录`/`评论区` (ProductCopy.kt:106) · `阅读` (ProductCopy.kt:108) ·
`章节` (ProductCopy.kt:110, unused) · `章节目录` (ReaderPresentation.kt:14) ·
`章节评论` (ReaderPresentation.kt:42) · `评论区` (BookDetailPresentation.kt:22) ·
`评论` (App.kt:877) · `热门标签` (App.kt:1479) · `搜索历史` (App.kt:1634) · `筛选` (App.kt:1667) ·
`分组与筛选` (App.kt:2753) · `收藏书籍` (LibraryPresentation.kt:27) · `收藏分组` (App.kt:2799, unused) ·
`收藏状态` (App.kt:3053, unused) · `链接预览` (App.kt:1024) · `没有匹配结果` (App.kt:1539, unused) ·
`暂无收藏` (App.kt:3629)

**Buttons / actions**
`同步`, `登录`/`已登录`, `网页论坛` (ProductCopy.kt:116) · `发布帖子` (App.kt:685) ·
`同步书架`/`登录同步`/`网页收藏` (ProductCopy.kt:48, unused) · `搜索`/`网页发现` (ProductCopy.kt:51, unused) ·
`继续阅读`, `清除` (LibraryPresentation.kt:25) · `登录后同步收藏` (App.kt:2698) ·
`网页登录`, `打开网页` (App.kt:3636-3637) · `加载更多收藏` (App.kt:1319) · `加载更多结果` (App.kt:1453) ·
`刷新` (App.kt:1483) · `清空` (App.kt:1635) · `继续阅读`/`开始阅读`/`网页详情`
(BookDetailPresentation.kt:5-10) · `编辑信息`/`章节管理`/`追加章节` (App.kt:3210-3212) ·
`打开网页评论` (BookDetailPresentation.kt:24, ReaderPresentation.kt:44) ·
`上一章`/`目录`/`下一章`/`A-`/`A+`/`主题`/`网页` (ReaderPresentation.kt:12) ·
`返回`/`阅读`/`网页` (ReaderPresentation.kt:28) · `返回` (App.kt:140) · `回到正文` (ReaderPresentation.kt:16) ·
`系统`/`护眼`/`深色` (App.kt:3563-3565) · `写评论`/`发送`/`发送中`/`取消`/`回复 X`
(App.kt:1056-1069, 1089-1103) · `赞`/`踩`/`表情`/`打赏`/`网页` (ForumPresentation.kt:55) ·
`回复` (App.kt:1201, 2177)

**Loading**
`正在同步论坛` (App.kt:650) · `正在打开帖子` (842) · `正在同步评论` (880, 3257) · `正在加载收藏书籍` (1284) ·
`正在加载收藏分组` (2757, 2802) · `正在请求 NovalPie 搜索` (1428) · `正在加载书籍详情` (1758) ·
`正在加载章节目录` (1777) · `正在加载正文` (1863) · `正在加载目录` (1967) · `正在同步章节评论` (2098) ·
`正在检查 /api/favorites/status` (3056, unused) · `正在加载更多` (2950) · `正在加载插图` (3549)

**Idle / waiting**
`等待加载书架` (1283) · `等待加载分组` (2756, 2801) · `等待加载书籍详情` (1757) · `等待加载章节` (1776) ·
`等待加载正文` (1862) · `等待加载目录` (1966) · `等待加载评论` (3256) · `等待加载章节评论` (2097) ·
`等待读取收藏状态` (3055, unused) · `打开发现页后同步网站标签` (1487) · `就绪`/`加载中`/`错误`
(DiscoverPresentation.kt:44-46; also App.kt:2927-2929 unused)

**Empty**
`论坛暂时没有可显示的讨论` (659) · `还没有评论` (884, 3261) · `还没有章节评论` (2102) ·
`没有匹配的收藏` (1295) · `没有找到搜索结果` (1432) · `暂无可显示标签` (1492) ·
`章节目录为空，可打开网页详情。` (1791) · `没有匹配的章节` (1792, 1983) · `正文暂时为空` (1010) ·
`目录未加载` (CatalogSummary.kt:10) · `暂无分组` (2806, unused) ·
`输入关键词后搜索，也可以先看推荐方向。` (DiscoverPresentation.kt:119) ·
`登录后同步网页收藏，或先打开网页确认账号状态。` (3631) ·
`可以换一个关键词，或调整范围、匹配方式和内容筛选。` (1541, unused)

**Error / retry**
`重新同步` (654) · `重试帖子` (844) · `重试评论` (881) · `重试标签` (1489) · `插图加载失败` (3551) ·
`大图加载失败` (ImagePreviewDialog.kt:124) · `重试` + surface via `retryActionLabel`
(ErrorRecovery.kt:5) · `…请求失败: ` / `服务返回错误 N` (ApiMessages.kt:5,13)

**Fields / hints**
`输入关键词、作品名或作者` (DiscoverPresentation.kt:39) · `搜索小说、作者或标签` (App.kt:2688) ·
`筛选书架` (2784) · `筛选目录` (3447) · `写评论` (1066, 1099)

**Counters / formatted**
`N 条回复` / `赞 N` / `表情 N` / `打赏 N` / `N 次浏览` (ForumPresentation.kt:47-51) ·
`赞 N`/`踩 N`/`表情 N`/`打赏 N`/`回复` (ReaderPresentation.kt:34-39) ·
`N 条评论 · M 条回复` (ForumPresentation.kt:88) · `主题`/`回复`/`分区` (App.kt:700-702) ·
`收藏 N`/`分组 N`/`最近 N` (LibraryPresentation.kt:19) + re-labelled cells `收藏`/`分组`/`最近`
(App.kt:2711-2713) · `已显示 N 本` (1318) · `已显示 N 个结果` (1452) · `N 个结果`
(DiscoverPresentation.kt:49) · `章节 N` (2853, 2877) · `继续上次阅读` (2879) · `上次读到: X` (3103) ·
`共 N 章` / `已筛选 N 章` / `当前第 N 章` (CatalogSummary.kt:12-19) ·
`第 i / N 章 · title` / `当前章节 N · 目录未加载` / `当前章节 N · 目录共 N 章` (ReaderProgressLabel.kt) ·
`Nsp` (App.kt:2213) · `N 字` (3429) · `#N` / `CH` (3425) · `N%` (ImagePreviewDialog.kt:141) ·
`正文插图 N` (ReaderText.kt:19, App.kt:3542)

**contentDescription-only strings** (accessibility content, easy to lose in a refactor)
`返回` (140) · `发布帖子` (685) · `同步书架` (2669) · `打开网页收藏` (2672) · `进入搜索` (2693) ·
`打开网页搜索` (1588) · `搜索` (1600) · `打开网页评论` (2082, 3241) ·
`$label，点击或长按查看大图` (3545) · `$title 大图` (ImagePreviewDialog.kt:102) · `关闭大图` (142) ·
`缩小` / `适应屏幕` / `还原` / `放大` (156-159) · `点击 / 长按看大图` (3547) ·
`NOVALPIE_NATIVE_COMPOSE_HOME` semantics marker on the Forum feed (613)

---

## 8. Refactor hazards found while inventorying

1. **`NovalPieViewModel.kt:2698` — mojibake `"璁哄潧"`** (double-encoded `论坛`). Renders in the
   Forum feed's `ErrorBlock` as `璁哄潧请求失败: …`.
2. **`NovalPieViewModel.kt:3914` — mojibake `"鏍囩"`** (double-encoded `标签`). Renders in the hot-tags
   `ErrorBlock` as `鏍囩请求失败: …`.
3. **Forum demo data leaks into production UI.** `NovalPieApp.kt:605-608` substitutes the 6 hard-coded
   `forumFeedItems()` rows on Idle/Loading/Error and on an empty success. Preserving "every content
   element" means preserving these rows (or making a deliberate decision to drop them).
4. **Forum category chips are inert** (`onClick = {}`, `NovalPieApp.kt:668`) and always show 全部 as
   selected.
5. **Filter dispatch keys on Chinese labels** (`NovalPieApp.kt:1673-1681`). Any rename of 排序 / 顺序 /
   范围 / 内容 / 字数 / 来源 silently routes that group to `onMatchTypeChange`.
6. **Two different fact-chip formats for the same data**: `novelCardFacts` uses spaces and
   `updatedAt.take(10)`; `bookDetailFacts` uses colons, adds `作者:` and `来源:`, and keeps the full
   `updatedAt`.
7. **Book detail comments are capped at 6** with no pagination (`NovalPieApp.kt:3263`); Reader
   comments are uncapped.
8. **`loadMoreFavorites` failure destroys the loaded list** (replaces `favorites` with `Error`,
   `NovalPieViewModel.kt:3800-3803`).
9. **`performSearch` accepts a blank keyword** and still writes it to history
   (`NovalPieViewModel.kt:3809-3826`, `searchKeywordForSubmission` only trims).
10. **Recent-reading rail asks for 8 but the store returns 5** (`NovalPieApp.kt:2870` vs
    `ReaderProgressStore.kt:84`).
11. **`ReaderScreen`'s `listState` is created and never used** (`NovalPieApp.kt:1848`) — no scroll
    restoration across chapters.
12. **Reader error label vs retry label mismatch**: `阅读器正文请求失败` in the message but
    `重试正文` on the button; likewise `阅读器目录` vs `重试章节目录`.
13. **Book-detail management buttons are always shown**; the login gate is only in the ViewModel, so
    an anonymous user is bounced to the web login page after tapping.
14. **Dead composables that still carry unique strings** — do not delete without a decision:
    `DiscoverEmptyResultPanel` (1535), `SearchResultHeader` (2924), `ReaderHeader` (2039),
    `GroupSection` (2790, sole owner of `暂无分组`), `FavoriteStatusCard` (3049, sole owner of
    `当前账号已收藏`/`当前账号未收藏`/`分组 id:`/`原始状态:`/`正在检查 /api/favorites/status`),
    `BookSummary` (3322), `ChoiceChips` (1709), `HeroCard` (2903), `ProductHeaderBlock` (2891).
15. **Dead copy helpers**: `forumCardCopies()` (ProductCopy.kt:118), `libraryPrimaryActions()` (47),
    `discoverPrimaryActions()` (50), `discoverFilterLabels()` (53), `readerCatalogTitle()` (110),
    `bookDetailSectionTitles()[0..1]`, `productHeader(ProductSurface.Library)` (39),
    `readerSurfaceSections()` (ReaderPresentation.kt:18), `bookCommentMetricLabels()`
    (BookDetailPresentation.kt:19), `bottomTabShortLabel()` (UiNavigation.kt:11),
    `readerSourceDebugLine`/`readerDebugIdentityLine` (always null), `readerToolbarLabels()[5]` (`主题`).
16. **The `NOVALPIE_NATIVE_COMPOSE_HOME` semantics marker sits on the Forum feed**, not Home
    (`NovalPieApp.kt:613`). Instrumentation may depend on it.
17. **Both `BottomTab.title` and `bottomTabDisplayLabel` define the tab names** — duplicated content
    that can drift.
