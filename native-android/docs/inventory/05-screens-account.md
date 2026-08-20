# UI Content Inventory — Part 2/3: ACCOUNT, MESSAGING, TOOLS

Repo: `D:/NovalPie/native-android`
Source root: `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp`
Scope of this document: Tools / function center, Profile (个人中心 / 我的), Settings (应用设置),
User profile detail (`/user/:id`), Message center, Message detail, Message conversation (DM),
Message settings, WebFallbackScreen.

All strings below are **verbatim**, decoded from the `\uXXXX` escapes present in source.
Every claim carries a `file:line` citation. Line numbers are from the current working tree
(commit-clean state as of inventory).

---

## 0. Global chrome that wraps every screen in this document

### 0.1 Scaffold / top bar

`D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt:114-181`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovalPieApp(startUri: String? = null, viewModel: NovalPieViewModel = viewModel())
```

* Top bar is a `CenterAlignedTopAppBar`, shown when `globalProductTopBarVisible(route)` is true
  (`NovalPieApp.kt:129`). That helper is `route !is AppRoute.Reader`
  (`ui/ReaderPresentation.kt:30-31`) — so **every** screen in this document has the global top bar.
* Title block is two stacked centered lines (`NovalPieApp.kt:131-136`):
  * line 1: **`NovalPie`** (bold)
  * line 2: `routeContextLabel(route, viewModel.currentTab)`, `labelSmall`
* Back arrow (`Icons.Filled.ArrowBack`, contentDescription **`返回`**) is rendered only when the
  route is not one of Forum / Home / Search / Tools / Profile (`NovalPieApp.kt:137-143`).
  So: Settings, MessageCenter, MessageDetail, MessageConversation, MessageSettings,
  UserProfileDetail and WebFallback all get a back arrow; Tools and Profile do not.
* Container color: `MaterialTheme.colorScheme.surface` for both normal and scrolled
  (`NovalPieApp.kt:144-147`).

`routeContextLabel` — `ui/UiNavigation.kt:19-38`, verbatim decoded:

| Route | Top-bar subtitle |
| --- | --- |
| `AppRoute.MessageCenter` | `消息中心` |
| `AppRoute.MessageDetail` | `消息详情` |
| `AppRoute.MessageConversation` | `私信` |
| `AppRoute.MessageSettings` | `消息设置` |
| `AppRoute.Workspace` | `工作区` |
| `AppRoute.UploadBook` | `上传书籍` |
| `AppRoute.UploadEditor` | `EPUB 编辑器` |
| `AppRoute.ForumCreate` | `发布帖子` |
| `AppRoute.ForumPostDetail` | `帖子详情` |
| `AppRoute.BookDetail` | `书籍详情` |
| `AppRoute.BookEditInfo` | `编辑书籍信息` |
| `AppRoute.BookChapters` | `章节管理` |
| `AppRoute.BookAppend` | `追加章节` |
| `AppRoute.Reader` | `阅读` (never shown — top bar hidden) |
| `AppRoute.Settings` | `应用设置` |
| `AppRoute.UserProfileDetail` | `用户主页` |
| `AppRoute.Admin` | `管理后台` |
| **everything else** (incl. `Tools`, `Profile`, `PoliticalExam`, `WebFallback`) | `bottomTabDisplayLabel(currentTab)` |

> **Gap to preserve/note:** `AppRoute.WebFallback` and `AppRoute.PoliticalExam` have **no** entry in
> `routeContextLabel`, so the web fallback screen's top bar subtitle shows whatever bottom tab was
> last active (e.g. `我的`, `工具`). This is current observable behavior.

### 0.2 Bottom navigation bar

`NovalPieApp.kt:151-174`. Rendered only for `AppRoute.Forum | Home | Search | Tools | Profile`.
`NavigationBar` with `containerColor = surface`, `tonalElevation = 0.dp`.

Tabs come from `BottomTab.values()` (`ui/NovalPieViewModel.kt:96-102`) — declaration order is
Collection, Discover, Tools, Forum, Profile.

| BottomTab | Label (`bottomTabDisplayLabel`, `UiNavigation.kt:3-9`) | Short (`bottomTabShortLabel`, `UiNavigation.kt:11-17`) | Icon (`NovalPieApp.kt:583-589`) | Target route (`NovalPieViewModel.kt:701-708`) |
| --- | --- | --- | --- | --- |
| Collection | `收藏` | `收` | `Icons.Filled.Favorite` | `AppRoute.Home` |
| Discover | `搜索` | `搜` | `Icons.Filled.Search` | `AppRoute.Search` |
| Tools | `工具` | `工` | `Icons.Filled.GridView` | `AppRoute.Tools` |
| Forum | `论坛` | `论` | `Icons.Filled.Forum` | `AppRoute.Forum` |
| Profile | `我的` | `我` | `Icons.Filled.Person` | `AppRoute.Profile` |

`BottomTab` enum also carries its own constructor label (`NovalPieViewModel.kt:96-102`):
`Collection("收藏")`, `Discover("搜索")`, `Tools("工具")`, `Forum("论坛")`, `Profile("我的")` —
these are duplicated by `bottomTabDisplayLabel`; the composable uses the function, not the enum field.

Item colors (`NovalPieApp.kt:163-169`): selected icon/text = `primary`, unselected = `secondary`,
indicator = `primaryContainer`.

Tab tap behavior (`NovalPieViewModel.kt:701-729`):
* If already on that tab **and** already on the tab's root route → re-load only:
  Collection→`loadHome()`, Discover→`loadSearchTags()`, Tools→`loadTools()`,
  Forum→`loadForum()`, Profile→`loadProfile()`.
* Otherwise: fire the same loader, then `routes.clear()` + push the target route, set `currentTab`.
  **Switching tabs wipes the whole back stack.**

### 0.3 Back handling

`NovalPieApp.kt:123-125`: `BackHandler(enabled = route !is Forum && !is Home && !is Search && !is Tools && !is Profile) { viewModel.goBack() }`.
`goBack()` pops the last route if `routes.size > 1` (`NovalPieViewModel.kt:3744-3748`).

### 0.4 Shared status/error blocks used by these screens

* `LoadingBlock(message)` — `LinearProgressIndicator` full width + `StatusText(message)`
  (`NovalPieApp.kt:3568-3574`).
* `StatusText(message)` — `bodyMedium` text in a full-width `Box` with 8dp vertical padding
  (`NovalPieApp.kt:3597-3602`).
* `ErrorBlock(message, retryLabel?, onRetry?)` — `ElevatedCard` with `surfaceVariant` container,
  message text, and an `OutlinedButton` whose label is `retryLabel ?: retryActionLabel("")`
  (`NovalPieApp.kt:3576-3595`).
* `LibraryStatPill(label)` — 4dp rounded `Surface` in `primaryContainer`, `labelMedium`, SemiBold,
  single line, ellipsized (`NovalPieApp.kt:3604-3620`).
* API failure text format — `apiFailureMessage(label, throwable)` in `ui/ApiMessages.kt:3-14`:
  `"${label}请求失败: ${detail}"`, where a detail matching `NovalPie API (\d+)` is rewritten to
  `服务返回错误 {status}`. Example rendered string: `消息列表请求失败: 服务返回错误 401`.

---

## 1. Tools screen — 功能中心

### 1.1 Signature & wiring

`ui/NovalPieApp.kt:2222-2311`

```kotlin
@Composable
private fun ToolsScreen(
    state: ToolsState,
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenMessage: (SiteMessage) -> Unit,
    onOpenRoute: (String) -> Unit
)
```

Call site: `NovalPieApp.kt:264-285`.

| Param | Bound to |
| --- | --- |
| `state` | `viewModel.toolsState` |
| `user` | `viewModel.homeState.user` (**not** `profileState`) |
| `hasAuthToken` | `!viewModel.authToken.isNullOrBlank()` |
| `onRefresh` | `viewModel::loadTools` |
| `onOpenLogin` | `viewModel::openLoginFallback` → WebFallback `https://novalpie.cc/login` (`NovalPieViewModel.kt:3709-3711`) |
| `onOpenMessages` | `viewModel::openMessageCenter` |
| `onOpenMessage` | `viewModel::openMessage` |
| `onOpenRoute` | inline lambda, see §1.5 |

State model — `ui/NovalPieViewModel.kt:220-223`:

```kotlin
data class ToolsState(
    val stats: LoadResult<MessageStats> = LoadResult.Idle,
    val messages: LoadResult<List<SiteMessage>> = LoadResult.Idle
)
```

### 1.2 Layout skeleton

`LazyColumn`, `contentPadding = start 16, top 16, end 16, bottom 96 dp`, item spacing 12dp
(`NovalPieApp.kt:2236-2240`).

Item order, top to bottom:

1. **Header row** (`NovalPieApp.kt:2241-2261`) — `SpaceBetween`:
   * Column: title `功能中心` (`headlineSmall`, Bold) + subtitle
     `消息、工作区与网站管理入口` (`bodyMedium`, `onSurfaceVariant`).
   * `OutlinedButton` with `Icons.Filled.Refresh` + 6dp spacer + text **`刷新`** → `onRefresh`.
2. **Login prompt** — only when `!hasAuthToken` (`NovalPieApp.kt:2263-2265`), see §1.3.
3. **Message stats strip** (`ToolsMessageStats`) — §1.4.
4. Section title `最近消息` (`titleMedium`, Bold) (`NovalPieApp.kt:2268-2270`).
5. Recent-message list / status, driven by `state.messages` (`NovalPieApp.kt:2271-2286`):
   * `Idle` → `StatusText("打开功能中心后同步消息")`
   * `Loading` → `LoadingBlock("正在同步消息")`
   * `Error` → `ErrorBlock(message, retryLabel = "重试消息", onRetry = onRefresh)`
   * `Success` + empty → `StatusText("暂无消息")`
   * `Success` + non-empty → `messages.value.take(6)` rendered as `ToolsMessageRow`, keyed by `it.id`
6. **Full-inbox button** (`NovalPieApp.kt:2287-2293`): full-width `OutlinedButton`,
   `Icons.Filled.Forum` + 8dp spacer + **`打开完整消息中心`** → `onOpenMessages`.
7. Section title `网站功能` (`titleMedium`, Bold) (`NovalPieApp.kt:2295-2297`).
8. **Function-center cards**, `entries.chunked(2)`, two per `Row` with 10dp gap; odd trailing row
   gets a `Spacer(Modifier.weight(1f))`. Row key = `row.joinToString("|") { it.path }`
   (`NovalPieApp.kt:2298-2309`).

### 1.3 `ToolsLoginPrompt`

`ui/NovalPieApp.kt:2313-2333`

```kotlin
@Composable
private fun ToolsLoginPrompt(onOpenLogin: () -> Unit)
```

`ElevatedCard`, 16dp padding, `SpaceBetween` row:
* Column (weight 1): **`登录后同步`** (Bold) and
  `消息、工作区和管理功能需要网站账号` (`bodySmall`, `onSurfaceVariant`).
* 12dp spacer, then `Button` **`登录`** → `onOpenLogin`.

### 1.4 `ToolsMessageStats`

`ui/NovalPieApp.kt:2335-2349`

```kotlin
@Composable
private fun ToolsMessageStats(stats: LoadResult<MessageStats>)
```

* `Idle` → `StatusText("等待同步消息统计")`
* `Loading` → `LoadingBlock("正在同步消息统计")`
* `Error` → `ErrorBlock(stats.message)` (no retry button — `onRetry` omitted)
* `Success` → horizontal `LazyRow` (8dp gap) of 5 `LibraryStatPill`s, in this exact order:
  1. `未读 {unreadCount}`
  2. `全部 {totalCount}`
  3. `重要 {importantCount}`
  4. `7日 {recentSevenDaysCount}`
  5. `星标 {starredCount}`

`MessageStats` shape — `model/Models.kt:295-303`: `totalCount, unreadCount, readCount,
starredCount, importantCount, recentSevenDaysCount, unreadByType: Map<Int,Int>`.
`readCount` and `unreadByType` are parsed but **never rendered anywhere** in the app.

### 1.5 `ToolsMessageRow`

`ui/NovalPieApp.kt:2351-2399`

```kotlin
@Composable
private fun ToolsMessageRow(message: SiteMessage, onOpenMessage: (SiteMessage) -> Unit)
```

* `ElevatedCard`, whole card clickable → `onOpenMessage(message)`.
* Container color: read → `surface`; unread → `primaryContainer` (full alpha).
* 14dp padding, 6dp vertical spacing:
  * Row 1 `SpaceBetween`: title (`titleSmall`, weight 1, `Medium` if read else `Bold`, 1 line,
    ellipsis) — 8dp spacer — `messageTypeLabel(message.type)` (`labelSmall`, `primary`).
  * Content preview (only when `content` non-blank): HTML tags stripped via
    `content.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()`,
    `bodySmall`, `onSurfaceVariant`, max 2 lines, ellipsis.
  * Row 3 `SpaceBetween`: `message.username.orEmpty()` (`labelSmall`) and
    `message.createdAt?.replace('T',' ')?.take(16).orEmpty()` (`labelSmall`, `onSurfaceVariant`).

### 1.6 Function-center entries — complete list

Data source: `ui/ToolsPresentation.kt:3-48`

```kotlin
internal data class ToolEntry(
    val title: String,
    val subtitle: String,
    val path: String,
    val adminOnly: Boolean = false
)

internal fun toolsEntries(isAdmin: Boolean): List<ToolEntry>
```

Card renderer: `ToolRouteCard` — `ui/NovalPieApp.kt:2401-2417`

```kotlin
@Composable
private fun ToolRouteCard(entry: ToolEntry, onClick: () -> Unit, modifier: Modifier = Modifier)
```
`ElevatedCard` clickable; 14dp padding, 8dp spacing; icon (`toolEntryIcon(entry.path)`, tinted
`primary`, no contentDescription) → title (`titleSmall`, Bold) → subtitle (`bodySmall`,
`onSurfaceVariant`, `minLines = 2`, `maxLines = 2`, ellipsis).

Icon mapping — `ui/NovalPieApp.kt:2419-2426`.

**Core entries (always shown), in order** (`ToolsPresentation.kt:11-37`):

| # | Title | Subtitle | `path` | Icon | Routing on tap |
| --- | --- | --- | --- | --- | --- |
| 1 | `消息中心` | `通知、私信与用户互动` | `/messages` | `Icons.Filled.Forum` | intercepted at card level → `onOpenMessages()` → native `AppRoute.MessageCenter` (`NovalPieApp.kt:2303`) |
| 2 | `工作区` | `翻译接口、Cookie 与服务状态` | `/workspace` | `Icons.Filled.GridView` | native `viewModel.openWorkspace()` (`NovalPieApp.kt:274`) |
| 3 | `上传书籍` | `导入 EPUB 并提交到网站` | `/upload` | `Icons.Filled.OpenInBrowser` | native `viewModel.openUploadBook()` (`NovalPieApp.kt:275`) |
| 4 | `上传编辑器` | `分章、替换、AI 正则与草稿` | `/upload-editor` | `Icons.Filled.MenuBook` | native `viewModel.openUploadEditor()` (`NovalPieApp.kt:276`) |
| 5 | `政治考试` | `网站积分奖励入口` | `/political-exam` | `Icons.Filled.CardGiftcard` | native `viewModel.openPoliticalExam()` (`NovalPieApp.kt:277`) |

**Admin-only entries, appended when `isAdmin == true`** (`ToolsPresentation.kt:38-47`).
All six use the `else` icon branch → `Icons.Filled.Tune`.

| # | Title | Subtitle | `path` | `adminOnly` | Routing on tap |
| --- | --- | --- | --- | --- | --- |
| 6 | `管理后台` | `管理员功能总览` | `/admin` | true | native `openAdminSection(AdminSection.Overview)` |
| 7 | `内容审核` | `审核与内容处理` | `/admin/review` | true | native `openAdminSection(AdminSection.Review)` |
| 8 | `密钥管理` | `API 密钥与使用状态` | `/admin/key-management` | true | native `openAdminSection(AdminSection.Keys)` |
| 9 | `操作日志` | `管理操作记录` | `/admin/operation-logs` | true | native `openAdminSection(AdminSection.OperationLogs)` |
| 10 | `抓取管理` | `抓取器与任务状态` | `/admin/scraper-management` | true | native `openAdminSection(AdminSection.Scraper)` |
| 11 | `商店管理` | `站内商店配置` | `/admin/shop` | true | native `openAdminSection(AdminSection.Shop)` |

Admin path→section mapping is by string equality on `AdminSection.websitePath`
(`NovalPieViewModel.kt:177-184`):
`Overview("/admin")`, `Review("/admin/review")`, `Keys("/admin/key-management")`,
`OperationLogs("/admin/operation-logs")`, `Scraper("/admin/scraper-management")`,
`Shop("/admin/shop")`.

**Native-vs-WebView routing rule** (`NovalPieApp.kt:272-284`):

```kotlin
onOpenRoute = { path ->
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
}
```

> **Important for the refactor:** with the current 11 entries **no** tool entry ever reaches the
> WebView fallback — the `openWebFallback("https://novalpie.cc$path")` branch is a safety net for
> unknown paths only. Any new entry whose path is not in the `when` and not an `AdminSection`
> path will silently open the WebView.

### 1.7 Admin gating (two layers + JWT role)

1. **Visibility layer** — `ToolsScreen` computes
   `val isAdmin = isAdminProfile((user as? LoadResult.Success)?.value)` (`NovalPieApp.kt:2233-2235`)
   and passes it to `toolsEntries(isAdmin)`.
   `isAdminProfile` requires **exact** `role == "admin"` (`ui/ProfilePresentation.kt:8`) —
   `"ADMIN"` and `"administrator"` are rejected (test: `ProfilePresentationTest.kt:31-36`).
2. **Action layer** — `openAdminSection` and `loadAdminSectionInternal` both re-check
   `if (!isAdminProfile(currentUserProfile())) return`
   (`NovalPieViewModel.kt:1002`, `NovalPieViewModel.kt:1038`), silently no-op for non-admins.
   Admin write actions also re-check (`NovalPieViewModel.kt:1233`).
3. **JWT role source** — `currentUserProfile()` is
   `(homeState.user as? Success)?.value ?: authToken?.let(::decodeAuthTokenProfile)`
   (`NovalPieViewModel.kt:1662-1663`).
   `decodeAuthTokenProfile` (`data/AuthSessionStore.kt:8-35`):
   * splits the token on `.`, takes segment `[1]`, base64-decodes, parses JSON;
   * rejects the token when `exp` is present and `exp <= now` (returns null);
   * `data = payload.optJSONObject("data") ?: payload`;
   * `id = payload["sub"] ?: data["user_id"] ?: data["id"]`;
   * `name = data["username"] ?: data["name"] ?: payload["username"]`;
   * `role = data["role"] ?: payload["role"] ?: "user"`;
   * returns null if both `id` and `name` are missing; fallback display name is `Logged user`.
   `loadHome()` seeds `homeState.user` from the decoded token before the network call
   (`NovalPieViewModel.kt:3752-3754`) and keeps it on network failure via `resolveUserLoadResult`
   (`NovalPieViewModel.kt:4032-4041`). **Net effect: the JWT `role` claim alone is enough to
   reveal and enter the admin entries.**

### 1.8 `loadTools()`

`NovalPieViewModel.kt:1253-1270`. Sets both `stats` and `messages` to `Loading`, then in parallel
`api.messageStats()` and `api.messages(page = 1, pageSize = TOOLS_MESSAGE_PREVIEW_LIMIT)`.
`TOOLS_MESSAGE_PREVIEW_LIMIT = 6` (`NovalPieViewModel.kt:4028`); `PAGE_SIZE = 20`
(`NovalPieViewModel.kt:4027`).
Error labels: `消息统计` and `消息列表` (`NovalPieViewModel.kt:1266-1267`), fed through
`apiFailureMessage`. Stale-response guard: `isFreshRequestSerial(requestSerial, toolsRequestSerial)`.

API: `GET /api/messages/stats` (`data/NovalPieApi.kt:793-795`) and
`GET /api/messages?page&page_size` (`data/NovalPieApi.kt:765-786`).

---

## 2. Profile screen — 个人中心 (bottom tab `我的`)

### 2.1 Signature & wiring

`ui/ProfileScreens.kt:54-217`

```kotlin
@Composable
internal fun ProfileScreen(
    state: ProfileState,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenLogin: () -> Unit,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onShowCheckinChange: (Boolean) -> Unit,
    onAutoCheckinChange: (Boolean) -> Unit,
    onAdultBirthYearChange: (String) -> Unit,
    onSave: () -> Unit,
    onCheckin: () -> Unit,
    onVerifyAdult: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onOpenSettings: () -> Unit
)
```

Call site: `NovalPieApp.kt:434-449` → `viewModel.profileState`, `viewModel::loadProfile`,
`viewModel::openLoginFallback`, `updateProfileName`, `updateProfileBio`,
`updateProfileShowCheckin`, `updateProfileAutoCheckin`, `updateProfileAdultBirthYear`,
`saveProfile`, `checkinCurrentUser`, `verifyCurrentUserAdult`, `uploadProfileAvatar`,
`openSettings`.

State model — `NovalPieViewModel.kt:145-158`:

```kotlin
data class ProfileState(
    val profile: LoadResult<UserProfile> = LoadResult.Idle,
    val checkinStats: LoadResult<UserCheckinStats> = LoadResult.Idle,
    val nameDraft: String = "",
    val bioDraft: String = "",
    val showCheckin: Boolean = true,
    val autoCheckin: Boolean = false,
    val adultBirthYearDraft: String = "",
    val saving: Boolean = false,
    val checkingIn: Boolean = false,
    val verifyingAdult: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val actionMessage: String? = null
)
```

### 2.2 Confirmation dialogs (local `remember` state)

`ProfileScreens.kt:72-114`

**Checkin confirm** (`confirmCheckin`, `ProfileScreens.kt:78-95`) — `AlertDialog`:
* title `确认签到`
* body `签到会立即提交到 NovalPie 账号。`
* confirm `Button` **`确认签到`** → dismiss + `onCheckin()`
* dismiss `TextButton` **`取消`**

**Adult verification confirm** (`confirmAdult`, `ProfileScreens.kt:97-114`) — `AlertDialog`:
* title `确认成年验证`
* body `出生年份将提交到 NovalPie 完成年验证。提交前请确认填写正确。`
* confirm `Button` **`确认提交`** → dismiss + `onVerifyAdult()`
* dismiss `TextButton` **`取消`**

### 2.3 Avatar picker

`ProfileScreens.kt:74-76`:
`rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())`, launched with
`arrayOf("image/*")` (`ProfileScreens.kt:145`); result URI stringified into `onAvatarSelected`.

### 2.4 Layout skeleton

`LazyColumn`, padding `start 16 / top 16 / end 16 / bottom 96`, 12dp spacing
(`ProfileScreens.kt:116-120`).

1. **Header item** (`ProfileScreens.kt:121-128`): `个人中心` (`headlineSmall`, Bold) and
   `资料、积分、签到与账号设置` (`bodyMedium`, `onSurfaceVariant`).
2. **`when (state.profile)`** (`ProfileScreens.kt:130-184`):
   * `Idle` / `Loading` → `ProfileLoadingCard()`
   * `Error` → `ProfileErrorCard(message, hasAuthToken, onRefresh, onOpenLogin)`
   * `Success` →
     a. Column: `ProfileHeroCard(profile, checkinStats)` + 8dp gap + full-width `OutlinedButton`
        labeled `头像上传中…` when `state.uploadingAvatar` else `更换头像`, disabled while uploading,
        launching the avatar picker (`ProfileScreens.kt:141-150`).
     b. `ProfileCheckinCard(...)` (`ProfileScreens.kt:151-158`)
     c. `ProfileEditCard(...)` (`ProfileScreens.kt:159-172`)
     d. `ProfileAdultVerificationCard(...)` — **only when `state.profile.value.isAdult != true`**
        (i.e. `false` or `null`) (`ProfileScreens.kt:173-182`)
3. **Action message banner** — `state.actionMessage?.let { … }` (`ProfileScreens.kt:186-196`):
   full-width `Surface`, `RoundedCornerShape(12.dp)`, `secondaryContainer`, message in `bodyMedium`
   with 12dp padding. **Rendered regardless of load state** (also visible on error/loading).
4. **App-settings card** (`ProfileScreens.kt:198-215`) — always present:
   * `应用设置` (`titleMedium`, Bold)
   * `阅读偏好、网络连接与网页登录入口` (`bodySmall`, `onSurfaceVariant`)
   * full-width `OutlinedButton` **`进入应用设置`** → `onOpenSettings` (pushes `AppRoute.Settings`)
   * when `!hasAuthToken`: additional full-width `Button` **`登录 NovalPie`** → `onOpenLogin`

`checkinStats` local = `(state.checkinStats as? LoadResult.Success)?.value` (`ProfileScreens.kt:71`).

### 2.5 `ProfileHeroCard` (shared with user-profile detail)

`ui/ProfileScreens.kt:219-271`

```kotlin
@Composable
internal fun ProfileHeroCard(profile: UserProfile, checkinStats: UserCheckinStats?)
```

`ElevatedCard`, container `surface`, 16dp padding, 14dp vertical spacing.

1. Row (`CenterVertically`): `ProfileAvatar(profile)` — 14dp spacer — Column(weight 1, 3dp spacing):
   * `profile.name` (`titleLarge`, Bold, 1 line, ellipsis)
   * role line: `管理员` if `isAdminProfile(profile)` else `普通用户` (`labelLarge`, `primary`)
   * `profile.id?.let { Text("用户 ID $it") }` (`bodySmall`, `onSurfaceVariant`)
2. `profile.bio` if non-blank → `bodyMedium` (`ProfileScreens.kt:248-250`)
3. `LazyRow` of `profileWebsiteFacts(profile, checkinStats)` as `ProfileFactPill`s
4. `LazyRow` of `profileAccountStatusLabels(profile)` as `ProfileFactPill`s — only if non-empty
5. Badges block — only if `profile.badges.isNotEmpty()`: label `徽章` (`labelLarge`, Bold) then a
   `LazyRow` of `ProfileFactPill(badge)` (`ProfileScreens.kt:263-268`)

`ProfileAvatar` (`ProfileScreens.kt:273-307`): 76dp `CircleShape` `Surface` in `primaryContainer`.
* Fallback content: first char of `profile.name` uppercased, or literal `"N"` when the name is
  empty (`headlineMedium`, Bold).
* If `avatarUrl` blank → fallback; else `SubcomposeAsyncImage` with
  `contentDescription = "${profile.name}的头像"`, `ContentScale.Crop`, clipped to circle;
  loading state = 24dp `CircularProgressIndicator` (stroke 2dp); error state = fallback.
* **`UserProfile.avatarFrameUrl` (`model/Models.kt:152`) is parsed but never rendered.**

`ProfileFactPill` (`ProfileScreens.kt:309-319`): 999dp-rounded `Surface`, `primaryContainer`,
horizontal 12dp / vertical 7dp padding, `labelMedium`, `onPrimaryContainer`.

### 2.6 Account status chips — exact strings & order

`ui/ProfilePresentation.kt:20-41`

```kotlin
internal fun profileAccountStatusLabels(profile: UserProfile): List<String>
```

Emission order (each conditional):

1. Mutually exclusive account state:
   * `profile.deleted == true` → `账号已删除` — and **nothing else in this branch**
   * else if `profile.isBanned == true` →
     * `账号封禁至 {yyyy-MM-dd}` when `banExpiresAt` non-blank, else `账号封禁`
     * plus `封禁原因 {banReason}` when `banReason` non-blank
   * else if `profile.isBanned == false` → `账号正常`
   * (`isBanned == null` and not deleted → no status chip)
2. Adult verification (`profile.isAdult`): `true` → `成年已验证`; `false` → `成年未验证`;
   `null` → omitted
3. `email` non-blank → `邮箱已绑定` (the address itself is never shown)
4. `createdAt` non-blank → `注册 {yyyy-MM-dd}`
5. `showCheckin` non-null → `签到公开` / `签到隐藏`
6. `autoCheckin` non-null → `自动签到已开` / `自动签到未开`

Date shortening (`ProfilePresentation.kt:40-41`): `profileShortDate` takes the first 10 chars only
when `value.length >= 10 && value[4] == '-' && value[7] == '-'`, otherwise the raw value.

Frozen by test `ProfilePresentationTest.kt:38-69`:
* active user → `["账号正常", "成年已验证", "邮箱已绑定", "注册 2026-01-02", "签到公开", "自动签到未开"]`
* banned user → `["账号封禁至 2026-08-09", "封禁原因 spam", "成年未验证"]`
* deleted user → `["账号已删除"]`

### 2.7 Hero fact pills

`ui/ProfilePresentation.kt:10-18`

```kotlin
internal fun profileWebsiteFacts(profile: UserProfile, checkinStats: UserCheckinStats?): List<String>
```

Fixed 4 entries in order:
1. `积分 {profile.points ?: 0}`
2. `作品 {profile.stats["novels"] ?: 0}`
3. `评论 {profile.stats["comments"] ?: 0}`
4. `连续签到 {checkinStats?.currentStreak ?: 0} 天`

Frozen by `ProfilePresentationTest.kt:15-28` (e.g. `积分 3210`, `作品 4`, `评论 29`, `连续签到 3 天`).
> `profile.stats` may carry other keys (e.g. `followers`) that are **never displayed**.

### 2.8 `ProfileCheckinCard` — 每日签到

`ui/ProfileScreens.kt:321-350`

```kotlin
@Composable
private fun ProfileCheckinCard(
    stats: UserCheckinStats?,
    statsLoading: Boolean,
    checkingIn: Boolean,
    onCheckin: () -> Unit
)
```

`ElevatedCard`, 16dp padding, 10dp spacing:
* Title `每日签到` (`titleMedium`, Bold)
* If `statsLoading` (i.e. `state.checkinStats is LoadResult.Loading`): Row with 20dp
  `CircularProgressIndicator` (stroke 2dp), 8dp spacer, text `正在同步签到记录`
* Else two lines:
  * `累计 {totalDays ?: 0} 天 · 签到积分 {totalPoints ?: 0}`
  * `当前连续 {currentStreak ?: 0} 天 · 最长连续 {maxStreak ?: 0} 天` (`bodySmall`, `onSurfaceVariant`)
* Full-width `Button`, disabled while `checkingIn`, label `签到中…` when checking in else
  **`立即签到`** → opens the confirm dialog (`ProfileScreens.kt:156`)

`UserCheckinStats` — `model/Models.kt:166-171`: `totalDays: Int`, `totalPoints: Long`,
`maxStreak: Int`, `currentStreak: Int`.

### 2.9 `ProfileEditCard` — 编辑资料

`ui/ProfileScreens.kt:352-390`

```kotlin
@Composable
private fun ProfileEditCard(
    name: String, bio: String, showCheckin: Boolean, autoCheckin: Boolean, saving: Boolean,
    onNameChange: (String) -> Unit, onBioChange: (String) -> Unit,
    onShowCheckinChange: (Boolean) -> Unit, onAutoCheckinChange: (Boolean) -> Unit,
    onSave: () -> Unit
)
```

`ElevatedCard`, 16dp padding, 12dp spacing:
* Title `编辑资料` (`titleMedium`, Bold)
* `OutlinedTextField` label **`用户名`**, `singleLine = true`, full width
* `OutlinedTextField` label **`个人简介`**, `minLines = 3`, `maxLines = 6`, full width
* `ProfileToggleRow("公开签到记录", showCheckin, onShowCheckinChange)`
* `ProfileToggleRow("自动签到", autoCheckin, onAutoCheckinChange)`
* Full-width `Button`, disabled while `saving`, label `保存中…` when saving else **`保存资料`**

`ProfileToggleRow(label, checked, onCheckedChange)` — `ProfileScreens.kt:425-435`: full-width Row,
`SpaceBetween`, `CenterVertically`, plain `Text(label)` + `Switch`.

### 2.10 `ProfileAdultVerificationCard` — 成年验证

`ui/ProfileScreens.kt:392-423`

```kotlin
@Composable
private fun ProfileAdultVerificationCard(
    birthYear: String, verifying: Boolean,
    onBirthYearChange: (String) -> Unit, onVerify: () -> Unit
)
```

`ElevatedCard`, 16dp padding, 10dp spacing:
* Title `成年验证` (`titleMedium`, Bold)
* Explainer `网站可能要求账号注册满 30 天后才能验证，最终资格由服务器判断。`
  (`bodySmall`, `onSurfaceVariant`)
* `OutlinedTextField`, label **`出生年份`**, placeholder **`例如 1995`**, `singleLine = true`,
  `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)`
* Full-width `Button`, `enabled = !verifying && birthYear.length == 4`, label `验证中…` when
  verifying else **`提交成年验证`** → opens confirm dialog

### 2.11 `ProfileLoadingCard` / `ProfileErrorCard`

`ProfileLoadingCard` — `ProfileScreens.kt:437-446`: `ElevatedCard`, 20dp padding Row with 24dp
`CircularProgressIndicator` (stroke 2dp), 12dp spacer, text `正在同步个人资料`.

`ProfileErrorCard(message, hasAuthToken, onRefresh, onOpenLogin)` — `ProfileScreens.kt:448-465`:
* Title: `个人资料暂时无法加载` when `hasAuthToken`, else `登录后查看个人资料` (Bold)
* Detail `message` (`bodySmall`) — **only when `hasAuthToken`** (error text hidden from guests)
* Row (8dp gap):
  * when `hasAuthToken`: `OutlinedButton` **`重试`** → `onRefresh`
  * always: `Button` labeled `重新登录` when `hasAuthToken` else `去登录` → `onOpenLogin`

### 2.12 Profile ViewModel actions

`loadProfile()` — `NovalPieViewModel.kt:735-760`
* Bumps `profileRequestSerial`. Optimistically shows JWT-decoded profile as `Success` if available,
  else `Loading`; `checkinStats = Loading`; clears `actionMessage`.
* Parallel `api.currentUser()` (`GET /api/users/me`, `NovalPieApi.kt:137-139`) and
  `api.currentUserCheckinStats()` (`GET /api/users/me/checkins/stats`, `NovalPieApi.kt:722-738`).
* `resolveUserLoadResult` keeps the token profile on failure; otherwise
  `LoadResult.Error(apiFailureMessage("登录状态", failure))` (`NovalPieViewModel.kt:4032-4041`).
* Stats failure label: `签到统计`.
* Rebuilds `ProfileState` wholesale and seeds drafts:
  `nameDraft = profile?.name`, `bioDraft = profile?.bio`, `showCheckin = profile?.showCheckin ?: true`,
  `autoCheckin = profile?.autoCheckin ?: false`.
* On success also mirrors the profile into `homeState.user`.

Draft updaters (each also clears `actionMessage`): `updateProfileName` (:762),
`updateProfileBio` (:766), `updateProfileShowCheckin` (:770), `updateProfileAutoCheckin` (:774),
`updateProfileAdultBirthYear` (:778) — the last filters to digits and truncates to 4 chars.

`saveProfile()` — `NovalPieViewModel.kt:785-825`
* No-op when `saving || checkingIn`.
* Profile source: `profileState.profile` success value, else `currentUserProfile()`; if neither →
  `actionMessage = "请先登录后再编辑资料"`.
* Blank trimmed name → `actionMessage = "用户名不能为空"`.
* Sets `actionMessage = "正在保存资料…"`, `saving = true`.
* `api.updateCurrentUser(updated)` → `PATCH /api/users/me` with body
  `{username, bio, show_checkin?, auto_checkin?}` (`NovalPieApi.kt:692-700`).
* Success → `actionMessage = "资料已保存"`, profile replaced locally, `homeState.user` updated.
* Failure → `apiFailureMessage("保存资料", failure)`.

`checkinCurrentUser()` — `NovalPieViewModel.kt:827-863`
* No-op when `checkingIn || saving`. Missing token → `actionMessage = "请先登录后再签到"`.
* `actionMessage = "正在签到…"`; `POST /api/users/me/checkins` (`NovalPieApi.kt:740-747`).
* Success: re-fetches `currentUser()` + `currentUserCheckinStats()`;
  `actionMessage = action.message ?: (if success "签到成功" else "签到未完成")`.
* Failure → `apiFailureMessage("签到", failure)`.

`verifyCurrentUserAdult()` — `NovalPieViewModel.kt:865-899`
* No-op when `verifyingAdult || saving || checkingIn`. Missing token →
  `actionMessage = "请先登录后再进行成年验证"`.
* Year must parse and be in `1900..Calendar.getInstance().get(Calendar.YEAR)`, else
  `actionMessage = "请输入有效的出生年份"`.
* `actionMessage = "正在提交成年验证…"`;
  `POST /api/users/me/verifies/adult` body `{birth_year}` (`NovalPieApi.kt:323-334`;
  API-level `require(birthYear in 1900..2100)`).
* Success → local `isAdult = action.success`;
  `actionMessage = action.message ?: (if success "成年验证已完成" else "成年验证未通过")`.
* Failure → `apiFailureMessage("成年验证", failure)`.

`uploadProfileAvatar(rawUri)` — `NovalPieViewModel.kt:901-928`
* No-op when already uploading or URI blank. `actionMessage = "正在上传头像…"`.
* Local validation via `require`: `头像文件为空` (size 0) and `请选择图片文件`
  (mime not starting with `image/`) — these `require` messages surface through
  `apiFailureMessage("上传头像", …)` as `上传头像请求失败: 头像文件为空` etc.
* `POST /api/users/me/avatar` multipart, part name `avatar` (`NovalPieApi.kt:336-348`),
  then re-fetch `currentUser()`.
* Success → `actionMessage = "头像已更新"`, profile + `homeState.user` refreshed.
* Failure → `apiFailureMessage("上传头像", failure)`.

`openSettings()` — `NovalPieViewModel.kt:731-733`: `pushDistinctRoute(routes, AppRoute.Settings)`.
**Does not trigger any load.**

---

## 3. Settings screen — 应用设置

### 3.1 Signature & wiring

`ui/NovalPieApp.kt:2428-2474`

```kotlin
@Composable
private fun SettingsScreen(
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    readerProgress: ReaderProgress?,
    readerOptions: ReaderUiOptions,
    proxyEnabled: Boolean,
    proxyHost: String,
    proxyPort: String,
    proxySummary: String,
    onRefreshAccount: () -> Unit,
    onOpenLogin: () -> Unit,
    onClearToken: () -> Unit,
    onProxyEnabledChange: (Boolean) -> Unit,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (String) -> Unit,
    onSaveProxy: () -> Unit,
    onOpenHomeFallback: () -> Unit,
    onOpenSearchFallback: () -> Unit
)
```

Call site: `NovalPieApp.kt:462-480`:
* `user = viewModel.profileState.profile` (**profileState**, not homeState)
* `readerOptions = viewModel.readerUiOptions` (`ReaderUiOptions(fontSizeSp = 18, theme = "system")`
  defaults — `NovalPieViewModel.kt:412-415`)
* `proxySummary = viewModel.proxySettings.summary()`
* `onRefreshAccount = viewModel::loadHome`
* `onClearToken = viewModel::clearAuthToken`
* `onOpenHomeFallback = { viewModel.openWebFallback("https://novalpie.cc") }`
* `onOpenSearchFallback = { viewModel.openWebFallback("https://novalpie.cc/search?sort_by=relevance") }`

> **Two behavior quirks to preserve or consciously fix in the refactor:**
> 1. `openSettings()` never calls `loadProfile()`, and the account card reads `profileState.profile`.
>    Entering Settings without having visited the 我的 tab shows `等待同步账号` (Idle branch).
> 2. `onRefreshAccount` is bound to `loadHome()`, which updates `homeState.user` only — pressing
>    **`同步账号`** in Settings therefore does **not** refresh what this card displays.

### 3.2 Layout

`LazyColumn`, `contentPadding = 16.dp` all round, 12dp spacing (`NovalPieApp.kt:2455-2459`):
1. `ProfileOverviewBlock(overview)` where `overview = profileOverview(user, hasAuthToken,
   readerProgress, readerOptions, proxyEnabled)` (`NovalPieApp.kt:2448-2461`)
2. `ProfileAccountCard(user, hasAuthToken, onRefreshAccount, onOpenLogin, onClearToken)`
3. `ProfileReaderCard(readerProgress, readerOptions)`
4. `ProfileConnectionCard(proxyEnabled, proxyHost, proxyPort, proxySummary, …)`
5. Web-entry row (`NovalPieApp.kt:2466-2472`): `LazyRow` 8dp gap, using
   `profileWebActions()` = `["打开网站", "网页搜索"]` (`ProfilePresentation.kt:86-87`):
   * `Button` label `actions[0]` = **`打开网站`** → `onOpenHomeFallback`
   * `OutlinedButton` label `actions[1]` = **`网页搜索`** → `onOpenSearchFallback`

### 3.3 `profileOverview` / `ProfileOverviewBlock`

`ui/ProfilePresentation.kt:43-95`

```kotlin
internal data class ProfileOverview(
    val title: String, val subtitle: String, val accountName: String,
    val syncLabel: String, val roleLabel: String, val stats: List<String>
)

internal fun profileOverview(
    user: LoadResult<UserProfile>, hasAuthToken: Boolean, readerProgress: ReaderProgress?,
    readerOptions: ReaderUiOptions, proxyEnabled: Boolean
): ProfileOverview
```

* `title` / `subtitle` from `productHeader(ProductSurface.Profile)` = **`我的`** /
  **`账号、阅读偏好和连接设置`** (`ui/ProductCopy.kt:38-42`)
* `accountName` = `profile?.name` ?: (`账号已同步` if `hasAuthToken` else `未登录`)
* `syncLabel` = `已同步` / `未同步`
* `roleLabel` = `profileRoleLabel(role, hasAuthToken)` (`ProfilePresentation.kt:89-95`):
  * `role == "admin"` → `管理员`
  * role non-blank (any other) → `普通用户`
  * role null/blank but `hasAuthToken` → `身份待同步`
  * else → `普通用户`
* `stats` — exactly 4 pills, in order:
  1. `阅读 章节 {chapterId}` when progress exists, else `阅读 无进度`
  2. `字号 {fontSizeSp}sp`
  3. `主题 {theme.themeLabel()}` — `themeLabel` (`NovalPieApp.kt:3562-3566`):
     `"sepia"` → `护眼`, `"dark"` → `深色`, anything else → `系统`
  4. `连接 已启用` / `连接 未启用`

Frozen by `ProfilePresentationTest.kt:71-118`.

`ProfileOverviewBlock` — `ui/NovalPieApp.kt:2476-2517`: `ElevatedCard` (container `surface`),
14dp padding, 10dp spacing:
* Row `SpaceBetween`: Column(`overview.title` `titleLarge` Bold; `overview.subtitle` `bodySmall`
  `onSurfaceVariant`) and a `Surface(RoundedCornerShape(6.dp), surfaceVariant)` badge containing
  `overview.syncLabel` (`labelSmall`, SemiBold, 8/4dp padding)
* `overview.accountName` (`headlineSmall`, Bold)
* `overview.roleLabel` (`bodySmall`, `onSurfaceVariant`)
* `LazyRow` of `LibraryStatPill(stat)` for the 4 stats

### 3.4 `ProfileAccountCard` — 账号

`ui/NovalPieApp.kt:2519-2554`

```kotlin
@Composable
private fun ProfileAccountCard(
    user: LoadResult<UserProfile>, hasAuthToken: Boolean,
    onRefreshAccount: () -> Unit, onOpenLogin: () -> Unit, onClearToken: () -> Unit
)
```

`ElevatedCard`, 16dp padding, 10dp spacing:
* Section title `profileSectionTitles()[0]` = **`账号`** (`titleMedium`, Bold).
  `profileSectionTitles()` = `["账号", "阅读偏好", "连接设置", "网页入口"]`
  (`ProfilePresentation.kt:76-77`). **`网页入口` (index 3) is defined but never rendered as a
  heading** — the web row (§3.2 item 5) has no title.
* `accountSyncSummary(hasAuthToken)` (`ProductCopy.kt:44-45`):
  `登录同步: 已连接` / `登录同步: 未同步`
* `when (user)`:
  * `Idle` → `Text("等待同步账号")`
  * `Loading` → `LoadingBlock("正在同步账号")`
  * `Error` → `Text(user.message, bodySmall)`
  * `Success` → `Text(name, Bold)`; `Text(管理员 | 普通用户)`; then, if non-empty, a `LazyRow` of
    `LibraryStatPill(status)` for `profileAccountStatusLabels(user.value)` (same chip set as §2.6)
* Action row — `LazyRow` 8dp gap, labels from
  `profileAccountActions(hasAuthToken)` (`ProfilePresentation.kt:79-84`):
  * `hasAuthToken == true` → `["同步账号", "网页登录", "退出同步"]`
  * `hasAuthToken == false` → `["同步账号", "网页登录"]`
  * `Button` `actions[0]` **`同步账号`** → `onRefreshAccount`
  * `OutlinedButton` `actions[1]` **`网页登录`** → `onOpenLogin`
  * when `hasAuthToken`: `OutlinedButton` `actions[2]` **`退出同步`** → `onClearToken`

`clearAuthToken()` — `NovalPieViewModel.kt:693-699`: clears the token from
`AuthSessionStore` (SharedPreferences file `novalpie_native_auth`, key `auth_token` —
`data/AuthSessionStore.kt:43-67`), nulls `authToken`, bumps `profileRequestSerial`,
**resets `profileState` to a fresh `ProfileState()`**, then `loadHome()`.
It does **not** clear WebView cookies or WebView `localStorage`.

### 3.5 `ProfileReaderCard` — 阅读偏好

`ui/NovalPieApp.kt:2556-2571`

```kotlin
@Composable
private fun ProfileReaderCard(readerProgress: ReaderProgress?, readerOptions: ReaderUiOptions)
```

`ElevatedCard`, 16dp padding, 8dp spacing:
* Title `profileSectionTitles()[1]` = **`阅读偏好`** (`titleMedium`, Bold)
* `字号: {fontSizeSp}sp`
* `主题: {theme.themeLabel()}`
* If `readerProgress == null` → `进度: 无`
* Else → `进度: 章节 {chapterId}` and, when present, `chapterTitle` (1 line, ellipsis)

Read-only card — no controls. Font size/theme are changed only from the Reader screen
(`increaseReaderFont`, `decreaseReaderFont`, `cycleReaderTheme` —
`NovalPieViewModel.kt:636-656`; theme cycle order `system → sepia → dark → system`).

### 3.6 `ProfileConnectionCard` — 连接设置 (proxy)

`ui/NovalPieApp.kt:2573-2606`

```kotlin
@Composable
private fun ProfileConnectionCard(
    proxyEnabled: Boolean, proxyHost: String, proxyPort: String, proxySummary: String,
    onProxyEnabledChange: (Boolean) -> Unit, onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (String) -> Unit, onSaveProxy: () -> Unit
)
```

`ElevatedCard`, 16dp padding, 10dp spacing:
* Row `SpaceBetween`: Column(title `profileSectionTitles()[2]` = **`连接设置`** `titleMedium` Bold;
  `当前: {proxySummary}` `bodySmall`) and a `Switch(checked = proxyEnabled)`
* `OutlinedTextField` full width, `singleLine`, label **`连接主机`** → `onProxyHostChange`
* `OutlinedTextField` full width, `singleLine`, label **`连接端口`** → `onProxyPortChange`
* Explainer `模拟器访问受限时可使用本机连接设置，保存后重新同步页面。` (`bodySmall`)
* `Button` **`保存连接`** → `onSaveProxy`

Summary text — `ProxySettings.summary()` (`data/NetworkConfigStore.kt`):
* enabled → `"{host}:{port} + fallback"`
* disabled → `"auto: 127.0.0.1/10.0.2.2:7890 + direct"`
  (built from `DEFAULT_PROXY_PORT = 7890`)

Proxy state & persistence:
* VM state: `proxySettings` (loaded from store), `proxyEnabled`, `proxyHost`,
  `proxyPortText` (`NovalPieViewModel.kt:427-434`)
* `updateProxyEnabled` (:658), `updateProxyHost` (:662),
  `updateProxyPort` (:666) — **digits only, max 5 chars**
* `saveProxySettings()` (:670-683): builds
  `ProxySettings(enabled, host.trim().ifBlank { "10.0.2.2" }, port.toIntOrNull()?.coerceIn(1,65535) ?: 7890)`,
  writes to `NetworkConfigStore` (prefs file `novalpie_native_network`, keys `proxy_enabled`,
  `proxy_host`, `proxy_port`, `proxy_user_configured`), reconfigures the Coil image loader, then
  `loadHome()`.
* Defaults: `DEFAULT_PROXY_ENABLED = false`, `DEFAULT_PROXY_HOST = "10.0.2.2"`,
  `DEFAULT_PROXY_PORT = 7890`, `DEFAULT_EMULATOR_PROXY_HOSTS = ["10.0.2.2", "127.0.0.1"]`.
* `NetworkConfigStore.loadProxySettings()` contains a migration: if the user never explicitly saved
  (`proxy_user_configured == false`) and the stored value is enabled-with-defaults, it is forced
  back to `enabled = false`.
* `shouldPreferEmulatorProxy()` = true when `Build.SUPPORTED_ABIS` contains `x86` or `x86_64`.

### 3.7 Dead/unused settings-adjacent composables

* `UserSection(user)` — `ui/NovalPieApp.kt:2619-2635`. Title `账号状态`; branches
  `等待检查登录状态` / `LoadingBlock("正在检查 /api/users/me")` /
  `"${user.message}\n未拿到网站 Cookie 或 auth token 时这里会失败。"` / name + `role: {role ?: "unknown"}`.
  **No call sites remain** — it is a leftover debug card. `ProfilePresentationTest.kt:141-162`
  explicitly forbids `role:` / `API` copy in the shipped profile strings, so this card is
  deliberately unreachable. Do not resurrect it.
* `ReaderProgressHint(progress)` — `ui/NovalPieApp.kt:2608-2617`, title `本书阅读进度`,
  `章节 {chapterId}` + optional title. Used by book-detail flows (part 1/3 territory), not Settings.

---

## 4. User profile detail — `/user/:id`

### 4.1 Signature & wiring

`ui/UserProfileScreens.kt:33-158`

```kotlin
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UserProfileDetailScreen(
    state: UserProfileDetailState,
    hasAuthToken: Boolean,
    onRetry: () -> Unit,
    onTabSelected: (UserProfileTab) -> Unit,
    onOpenActivity: (UserActivity) -> Unit,
    onOpenBook: (Long) -> Unit,
    onMessageUser: (Long, String?) -> Unit,
    onOpenLogin: () -> Unit
)
```

Call site `NovalPieApp.kt:451-460`: `state = viewModel.userProfileDetailState`,
`onRetry = { viewModel.loadUserProfile(route.userId) }`, `onTabSelected = selectUserProfileTab`,
`onOpenActivity = openUserActivity`, `onOpenBook = openBook`,
`onMessageUser = openMessageConversation`, `onOpenLogin = openLoginFallback`.

State — `NovalPieViewModel.kt:160-175`:

```kotlin
enum class UserProfileTab { Checkin, Activities, Books }

data class UserProfileDetailState(
    val userId: Long = 0,
    val profile: LoadResult<UserProfile> = LoadResult.Idle,
    val activities: LoadResult<List<UserActivity>> = LoadResult.Idle,
    val books: LoadResult<List<NovelCard>> = LoadResult.Idle,
    val checkinStats: LoadResult<UserCheckinStats> = LoadResult.Idle,
    val checkinRecords: LoadResult<List<UserCheckinRecord>> = LoadResult.Idle,
    val checkinSettings: LoadResult<UserCheckinSettings> = LoadResult.Idle,
    val selectedTab: UserProfileTab = UserProfileTab.Activities   // default tab = 帖子与评论
)
```

### 4.2 Layout

`LazyColumn`, `contentPadding = 16.dp`, 12dp spacing (`UserProfileScreens.kt:47-51`).

1. `when (state.profile)` (`UserProfileScreens.kt:52-81`):
   * `Idle`/`Loading` → `PublicProfileStatusCard("正在加载用户资料")`
   * `Error` → `ElevatedCard` with title `用户资料加载失败` (Bold), `state.profile.message`
     (`bodySmall`), `OutlinedButton` **`重试`** → `onRetry`
   * `Success` →
     * `ProfileHeroCard(state.profile.value, stats)` — identical hero card as §2.5 (shared)
     * DM action item:
       * `hasAuthToken` → full-width `Button` **`发送私信`**, `enabled = id != null`,
         click → `onMessageUser(id, name)`
       * else → full-width `OutlinedButton` **`登录后发送私信`** → `onOpenLogin`
2. **Tab chips** (only when profile loaded, `UserProfileScreens.kt:83-94`) —
   `LazyRow` of `FilterChip` over `UserProfileTab.values()` in declaration order.
   Labels (`userProfileTabLabel`, `UserProfileScreens.kt:238-242`):
   * `Checkin` → **`签到`**
   * `Activities` → **`帖子与评论`**
   * `Books` → **`书籍`**
3. Tab content (`UserProfileScreens.kt:96-155`).

### 4.3 签到 tab

`UserProfileScreens.kt:97-119`

* If `checkinSettings` resolved to `Success` **and** `showCheckin == false` →
  `PublicProfileStatusCard("该用户未公开签到记录")` and nothing else.
  (Loading/error/`showCheckin == true` all fall through to the normal content.)
* Otherwise:
  * `ElevatedCard` (16dp padding, 6dp spacing): title `签到` (`titleMedium`, Bold), then by
    `state.checkinStats`:
    * `Idle`/`Loading` → `Text("正在加载签到统计")`
    * `Error` → `Text(message, bodySmall)`
    * `Success` → two lines:
      * `累计 {totalDays} 天 · {totalPoints} 积分`
      * `当前连续 {currentStreak} 天 · 最长 {maxStreak} 天`
  * `PublicCheckinRecords(state.checkinRecords)`

`PublicCheckinRecords` — `UserProfileScreens.kt:209-229`: `ElevatedCard`, 16dp padding, 8dp spacing:
* Title **`本年签到记录`** (`titleMedium`, Bold)
* `Idle`/`Loading` → `Text("正在加载签到记录")`
* `Error` → `Text(message, bodySmall)`
* `Success`:
  * empty → `Text("暂无签到记录")`
  * rows: `records.value.takeLast(30).reversed()` — each a full-width `SpaceBetween` Row with
    `record.date` on the left and `+{points} 积分` (colored `primary`) on the right

`UserCheckinRecord` = `{date: String, points: Long = 0}` (`model/Models.kt:192-195`).
`UserCheckinSettings` = `{showCheckin: Boolean = true, autoCheckin: Boolean = false}`
(`model/Models.kt:197-200`) — `autoCheckin` is fetched but never displayed on this screen.

### 4.4 帖子与评论 tab

`UserProfileScreens.kt:121-130`
* `Idle`/`Loading` → `PublicProfileStatusCard("正在加载用户动态")`
* `Error` → `PublicProfileStatusCard(value.message)`
* `Success` empty → `PublicProfileStatusCard("暂无公开动态")`
* `Success` → `items(value.value, key = { "${it.type}-${it.id}" }) { UserActivityCard(it, onOpenActivity) }`

`UserActivityCard` — `UserProfileScreens.kt:160-207`: `ElevatedCard`, whole card
`clickable(enabled = activity.postId != null || activity.bookId != null)`; 14dp padding, 6dp spacing:
* Row `SpaceBetween`: a 999dp-rounded `primaryContainer` `Surface` holding
  `userActivityTypeLabel(activity.type)` (`labelSmall`, 9/4dp padding), and `activity.createdAt`
  when non-null (`labelSmall`, `onSurfaceVariant`)
* `activity.title` (`titleMedium`, Bold, max 2 lines, ellipsis)
* `activity.content` when non-null (`bodyMedium`, `onSurfaceVariant`, max 4 lines, ellipsis)

`userActivityTypeLabel` — `UserProfileScreens.kt:244-250`:

| `type` | Label |
| --- | --- |
| `novel_comment` | `书评` |
| `chapter_comment` | `章评` |
| `post_comment` | `评论` |
| `post` | `帖子` |
| anything else | `动态` |

Activity tap routing — `openUserActivity` (`NovalPieViewModel.kt:992-999`):
`postId != null` → `openForumPost(postId)`; else `bookId != null && chapterId != null` →
`openReader(bookId, chapterId)`; else `bookId != null` → `openBook(bookId)`.
`UserActivity.coverUrl` and `commentId` (`model/Models.kt:179-190`) are parsed but never used here.

### 4.5 书籍 tab

`UserProfileScreens.kt:132-154`
* `Idle`/`Loading` → `PublicProfileStatusCard("正在加载用户作品")`
* `Error` → `PublicProfileStatusCard(value.message)`
* `Success` empty → `PublicProfileStatusCard("暂无上传作品")`
* `Success` → manual 2-column grid: `items(value.value.chunked(2))` with a `Row`
  (12dp gap, `Alignment.Top`); each cell is a `Box(Modifier.weight(1f))` containing
  `NovelCardItem(book) { onOpenBook(book.id) }`; the trailing partial row is padded with
  `Spacer(Modifier.weight(1f))`.

`NovelCardItem` — `ui/NovalPieApp.kt:2959-3013` (shared with browse surfaces): cover
(aspect ratio `2f/3f`, `bookCoverAspectRatio()` at `NovalPieApp.kt:3412`), title (`titleSmall`,
Bold, 2 lines), author label, optional original-title label, `FlowRow` of source pill + tag pills,
and a `·`-joined facts line.

### 4.6 `PublicProfileStatusCard`

`UserProfileScreens.kt:231-236`: `ElevatedCard` containing a single `Text(message)` with 18dp
padding, colored `onSurfaceVariant`.

### 4.7 Data loading

`openUserProfile(userId)` — `NovalPieViewModel.kt:930-942`
* Ignores `userId <= 0`.
* **Self-redirect:** if `currentUserProfile()?.id == userId`, it switches to
  `BottomTab.Profile`, clears the stack, pushes `AppRoute.Profile` and calls `loadProfile()` —
  i.e. tapping your own name lands on 个人中心, not on this screen.
* Otherwise pushes `AppRoute.UserProfileDetail(userId)` and calls `loadUserProfile(userId)`.

`loadUserProfile(userId)` — `NovalPieViewModel.kt:944-986`
* Resets state to all-`Loading` while **preserving the previously selected tab**.
* Six parallel calls:
  | Call | Endpoint | Error label |
  | --- | --- | --- |
  | `api.userProfile(userId)` | `GET /api/users/{id}` (`NovalPieApi.kt:141-144`) | `用户资料` |
  | `api.userActivities(userId)` | `GET /api/users/{id}/activities` (`NovalPieApi.kt:146-164`, default `page=1, limit=100`) | `用户动态` |
  | `api.userNovels(userId)` | `GET /api/users/{id}/novels` (`NovalPieApi.kt:166-170`) | `用户作品` |
  | `api.userCheckinStats(userId)` | `GET /api/users/{id}/checkins/stats?user_id=` (`NovalPieApi.kt:726-738`) | `签到统计` |
  | `api.userCheckinRecords(userId, "{year}-01-01", "{year}-12-31")` | `GET /api/users/{id}/checkins?start_date&end_date` (`NovalPieApi.kt:298-308`) | `签到记录` |
  | `api.userCheckinSettings(userId)` | `GET /api/users/{id}/checkins/settings?user_id=` (`NovalPieApi.kt:310-321`) | `签到设置` |
* Year comes from `Calendar.getInstance().get(Calendar.YEAR)` — hence the card title `本年签到记录`.

`selectUserProfileTab(tab)` — `NovalPieViewModel.kt:988-990`: pure state copy, no reload.

Deep link: `openDeepLink` handles `novalpie://app/user/{id}` and
`https://novalpie.cc/user/{id}` → `openUserProfile(id)` (`NovalPieViewModel.kt:3713-3724`).

---

## 5. Message center — 我的消息

### 5.1 Signature & wiring

`ui/MessageScreens.kt:75-233` (file-level `@OptIn(ExperimentalMaterial3Api::class)` at
`MessageScreens.kt:1`)

```kotlin
@Composable
internal fun MessageCenterScreen(
    state: MessageCenterState,
    hasAuthToken: Boolean,
    onOpenLogin: () -> Unit,
    onRefresh: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onTypeSelected: (Int?) -> Unit,
    onReadSelected: (Boolean?) -> Unit,
    onPrioritySelected: (Int?) -> Unit,
    onToggleSelected: (Long) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onMarkSelectedRead: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMarkAllRead: () -> Unit,
    onToggleStar: (SiteMessage) -> Unit,
    onOpenMessage: (SiteMessage) -> Unit,
    onLoadMore: () -> Unit,
    onOpenSettings: () -> Unit
)
```

Call site: `NovalPieApp.kt:312-331` (all handlers are direct VM method references).

State — `NovalPieViewModel.kt:225-234`:

```kotlin
data class MessageCenterState(
    val query: MessageQuery = MessageQuery(),
    val messages: LoadResult<List<SiteMessage>> = LoadResult.Idle,
    val pagination: MessagePagination = MessagePagination(),
    val stats: LoadResult<MessageStats> = LoadResult.Idle,
    val selectedIds: Set<Long> = emptySet(),
    val loadingMore: Boolean = false,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null
)
```

`MessageQuery` = `{keyword: String = "", messageType: Int? = null, isRead: Boolean? = null,
priority: Int? = null}` (`model/Models.kt:325-330`).
`MessagePagination` = `{page = 1, pageSize = 20, total = 0, totalPages = 1}`
(`model/Models.kt:332-337`).

### 5.2 Batch-delete confirm dialog

`MessageScreens.kt:96-110`, local `confirmBatchDelete` state:
* title **`批量删除`**
* body **`确定删除已选中的 {state.selectedIds.size} 条消息吗？`** (full-width Chinese question mark)
* confirm `TextButton` **`删除`** → dismiss + `onDeleteSelected()`
* dismiss `TextButton` **`取消`**

### 5.3 Layout skeleton

`LazyColumn`, padding `start 16 / top 16 / end 16 / bottom 40`, 12dp spacing
(`MessageScreens.kt:112-116`).

1. **Hero** `MessageCenterHero(state.stats, onRefresh, onOpenSettings)` — §5.4
2. **Login-required card** — only when `!hasAuthToken` (`MessageScreens.kt:119-136`):
   `ElevatedCard` with `errorContainer` container; 16dp padding Row `SpaceBetween`:
   Column(weight 1) `需要登录` (Bold) + `登录网站账号后才能同步通知与私信` (`bodySmall`);
   12dp spacer; `Button` **`登录`** → `onOpenLogin`
3. **Keyword field** (`MessageScreens.kt:138-150`): full-width `OutlinedTextField`, `singleLine`,
   label **`搜索标题或内容`**, leading `Icons.Filled.Search` (no contentDescription), trailing
   `IconButton` with `Icons.Filled.Search` and contentDescription **`搜索`** → `onSearch`,
   `imeAction = Search`, `keyboardActions.onSearch = onSearch`
4. **Three filter rails** (`MessageScreens.kt:152-175`) — §5.5
5. **Action message notice** — `state.actionMessage?.let { MessageNotice(it) } }`
6. **Selection bar / inbox header** (`MessageScreens.kt:179-202`) — §5.6
7. **Message list** (`MessageScreens.kt:204-223`) — §5.7
8. **Load-more button** — only when `state.pagination.page < state.pagination.totalPages`
   (`MessageScreens.kt:225-231`): full-width `OutlinedButton`, disabled while `state.loadingMore`,
   label `加载中...` (ASCII dots) when loading else **`加载更多`** → `onLoadMore`

### 5.4 `MessageCenterHero`

`ui/MessageScreens.kt:235-285`

```kotlin
@Composable
private fun MessageCenterHero(
    stats: LoadResult<MessageStats>, onRefresh: () -> Unit, onOpenSettings: () -> Unit
)
```

Full-width `Box`, `clip(RoundedCornerShape(24.dp))`, background
`Brush.linearGradient(listOf(colorScheme.primary, colorScheme.tertiary))`, 20dp padding.
Column with 14dp spacing:
* Row `SpaceBetween`:
  * Column: `我的消息` (`headlineSmall`, Bold, `Color.White`) and
    `通知、回复与私信` (`Color.White.copy(alpha = 0.82f)`)
  * Row of two white-tinted `IconButton`s:
    * `Icons.Filled.Refresh`, contentDescription **`刷新`** → `onRefresh`
    * `Icons.Filled.Settings`, contentDescription **`消息设置`** → `onOpenSettings`
* Stats area:
  * `Success` → `LazyRow` (8dp gap) of 4 `HeroStat`s in order:
    `未读`/`unreadCount`, `全部`/`totalCount`, `重要`/`importantCount`, `星标`/`starredCount`
    (note: **no `7日` pill here**, unlike the Tools strip which has 5)
  * `Loading` → full-width `LinearProgressIndicator`
  * `Idle` or `Error` → `Text("暂未取得消息统计", Color.White.copy(alpha = 0.82f))`
    (the error message itself is **not** shown)

`HeroStat(label, value)` — `MessageScreens.kt:277-285`: `Surface(Color.White.copy(alpha=0.16f),
RoundedCornerShape(14.dp))`, 14/10dp padding, value (white, Bold) above label
(white 0.8 alpha, `labelSmall`).

### 5.5 Filters — complete inventory

Generic rail: `MessageFilterRail` — `ui/MessageScreens.kt:287-306`

```kotlin
@Composable
private fun <T> MessageFilterRail(
    label: String, options: List<Pair<T, String>>, selected: T, onSelected: (T) -> Unit
)
```
Column 6dp spacing: rail label (`labelMedium`, `onSurfaceVariant`) then a `LazyRow` (8dp gap) of
`FilterChip(selected = selected == option.first, onClick = { onSelected(option.first) },
label = { Text(option.second) })`.

**(a) Keyword** — free text in the `OutlinedTextField` (§5.3 item 3). `onKeywordChange` only
edits `query.keyword`; the search is issued on `onSearch` (`applyMessageSearch()` →
`loadMessageCenter()`, `NovalPieViewModel.kt:1338-1344`).

**(b) 消息类型** (`MessageScreens.kt:152-159`) — rail label **`消息类型`**.
Options = `listOf(null to messageTypeLabel(null)) + messageTypeOptions().map { it.value to it.label }`,
i.e. 11 chips. `messageTypeOptions()` = `(1..10).map { MessageTypeOption(it, messageTypeLabel(it)) }`
(`ui/MessagePresentation.kt:6-12`).

Complete label table — `ui/ToolsPresentation.kt:50-63` (all 10 types + null + unknown):

| `type` | Label |
| --- | --- |
| `null` | `全部类型` |
| `1` | `用户互动` |
| `2` | `帖子回复` |
| `3` | `系统通知` |
| `4` | `小说更新` |
| `5` | `评论回复` |
| `6` | `点赞通知` |
| `7` | `关注通知` |
| `8` | `私信` |
| `9` | `系统公告` |
| `10` | `举报通知` |
| any other | `未知类型` |

Frozen by `ToolsPresentationTest.kt:32-38` and `MessagePresentationTest.kt:74-82`
(including `messageTypeOptions().map { it.value } == (1..10).toList()`).

**(c) 已读状态** (`MessageScreens.kt:160-167`) — rail label **`已读状态`**; options
`null to "全部"`, `false to "未读"`, `true to "已读"` → `onReadSelected(Boolean?)`.

**(d) 优先级** (`MessageScreens.kt:168-175`) — rail label **`优先级`**; options
`null to "全部"`, `0 to "普通"`, `1 to "重要"`, `2 to "紧急"` → `onPrioritySelected(Int?)`.

Selecting any of (b)(c)(d) is a no-op if the value is unchanged, otherwise it updates the query and
immediately reloads page 1 (`NovalPieViewModel.kt:1346-1362`).

### 5.6 Selection mode & batch actions

`MessageScreens.kt:179-202`

* When `state.selectedIds.isNotEmpty()`:
  `ElevatedCard` with `secondaryContainer` container, 14dp padding, 10dp spacing:
  * `已选 {selectedIds.size} 条消息` (Bold)
  * `LazyRow` (8dp gap) with three controls, each disabled while `state.actionLoading` (except the
    last):
    * `Button` **`标记已读`** → `onMarkSelectedRead` (`enabled = !actionLoading`)
    * `OutlinedButton` **`删除`** → opens the batch-delete dialog (`enabled = !actionLoading`)
    * `TextButton` **`取消选择`** → `onSelectAll(false)` (never disabled)
* Otherwise (nothing selected):
  full-width Row `SpaceBetween`, `CenterVertically`:
  * `收件箱` (`titleMedium`, Bold)
  * Row of two `TextButton`s: **`全选`** → `onSelectAll(true)`;
    **`全部已读`** → `onMarkAllRead` (`enabled = !actionLoading`)

Selection helpers (`ui/MessagePresentation.kt:14-18`):
* `toggleMessageSelection(selected, messageId)` — set XOR of a single id
* `selectVisibleMessages(messageIds, select)` — `messageIds.toSet()` or `emptySet()`
  (`selectAllVisibleMessages` only takes ids from the **currently loaded** page(s),
  `NovalPieViewModel.kt:1370-1373`)

Batch action VM entry points (`NovalPieViewModel.kt:1375-1395`):

| UI control | VM method | Label used in messages | API |
| --- | --- | --- | --- |
| 标记已读 (selection) | `markSelectedMessagesRead()` | `批量已读` | `POST /api/messages/read` body `{ids:[…]}` (`NovalPieApi.kt:917-921`) |
| 删除 (selection) | `deleteSelectedMessages()` | `批量删除` | `DELETE /api/messages` body `{ids:[…]}` (`NovalPieApi.kt:949-953`) |
| 全部已读 | `markAllMessagesRead()` | `全部已读` | `POST /api/messages/read` body `{all:true}` (`NovalPieApi.kt:923-927`) |
| star toggle | `toggleMessageStar(message)` | `取消星标` / `添加星标` | `POST /api/messages/{id}/star` body `{starred: 1|0}` (`NovalPieApi.kt:929-933`) |

All four funnel through `runMessageCenterAction(label, action)` — `NovalPieViewModel.kt:1614-1639`:
no-op while `actionLoading`; sets `actionLoading = true`; on success
`actionMessage = it.message ?: "{label}已同步"` and `selectedIds = emptySet()`; on failure
`actionMessage = apiFailureMessage(label, failure)`; on success also calls `loadMessageCenter()`
(so page 1 is re-fetched and any loaded extra pages are dropped).

### 5.7 `MessageInboxCard`

`ui/MessageScreens.kt:308-371`

```kotlin
@Composable
private fun MessageInboxCard(
    message: SiteMessage, selected: Boolean,
    onToggleSelected: () -> Unit, onToggleStar: () -> Unit, onOpen: () -> Unit
)
```

`ElevatedCard`, `shape = RoundedCornerShape(18.dp)`, full-width, whole card
`clickable(onClick = onOpen)`. Container color precedence (`MessageScreens.kt:318-324`):
1. `selected` → `secondaryContainer`
2. `!message.isRead` → `primaryContainer.copy(alpha = 0.58f)`
3. else → `surface`

Body: Row (12dp padding, `Alignment.Top`):
* `Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })`
* Column(weight 1, top padding 4dp, 7dp spacing):
  * Row `CenterVertically`:
    * when unread: an 8dp `Box` clipped to `RoundedCornerShape(4.dp)`, background `primary`
      (unread dot), then a 7dp spacer
    * title (weight 1, `SemiBold` if read else `Bold`, 1 line, ellipsis)
    * 36dp `IconButton` → `onToggleStar`, icon `Icons.Filled.Star` when starred else
      `Icons.Filled.StarBorder`, contentDescription **`星标`**, tint
      `Color(0xFFF59E0B)` when starred else `onSurfaceVariant`
  * content preview (when `content != null`): `plainMessageText(content)`, `bodySmall`,
    `onSurfaceVariant`, max 2 lines, ellipsis
  * Row `SpaceBetween`:
    * Row (6dp gap) of `AssistChip`s (all with `onClick = onOpen`):
      * `messageTypeLabel(message.type)`
      * if `message.priority > 0`: a second chip labeled `紧急` when `priority == 2` else `重要`
    * `messageDateLabel(message.createdAt)` (`labelSmall`, `onSurfaceVariant`)

Text helpers (private, `ui/MessageScreens.kt:673-681`):
```kotlin
private fun plainMessageText(value: String): String = value
    .replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

private fun messageDateLabel(value: String?): String = value
    ?.replace('T', ' ')?.take(16).orEmpty()
```

`SiteMessage` — `model/Models.kt:305-323`: `id, type, title, content?, username?, createdAt?,
isRead, isStarred, priority, actionUrl?, actionText?, readAt?, userId?, executeUserId?,
avatarUrl?, avatarFrameUrl?, extraData: Map<String,String>`.
> **Never rendered in the inbox:** `avatarUrl`, `avatarFrameUrl`, `readAt`, `username`
> (username shows in the Tools preview row and in the detail screen, but not in the inbox card).

### 5.8 Empty / loading / error / pagination

List branch (`MessageScreens.kt:204-223`):
* `Idle` → `MessageEmpty("等待同步消息")`
* `Loading` → `MessageLoading("正在同步收件箱")`
* `Error` → `MessageError(messages.message, onRefresh)`
* `Success` empty → `MessageEmpty("没有匹配的消息")`
* `Success` non-empty → `items(messages.value, key = { it.id })`

Shared blocks (all private in `MessageScreens.kt`):
* `MessageLoading(label)` (:639-645) — full-width `LinearProgressIndicator` + label
  (`onSurfaceVariant`), 20dp vertical padding, 8dp spacing
* `MessageError(message, onRetry)` (:647-655) — `ElevatedCard` with `errorContainer`,
  message in `onErrorContainer`, `OutlinedButton` **`重试`**
* `MessageEmpty(label)` (:657-664) — centered column, 28dp vertical padding, `Icons.Filled.Mail`
  32dp tinted `onSurfaceVariant`, 8dp spacer, label in `onSurfaceVariant`
* `MessageNotice(message)` (:666-671) — `Surface(secondaryContainer, RoundedCornerShape(14.dp))`,
  full-width text with 12dp padding, `onSecondaryContainer`

Pagination:
* `loadMessageCenter()` (`NovalPieViewModel.kt:1277-1303`) — resets `messages`/`stats` to Loading,
  `pagination = MessagePagination()`, `selectedIds = emptySet()`, `loadingMore = false`,
  `actionMessage = null`; then parallel `api.messagePage(query, page = 1, pageSize = PAGE_SIZE /*20*/)`
  and `api.messageStats()`. Error labels: `消息列表`, `消息统计`.
* `loadMoreMessages()` (`NovalPieViewModel.kt:1305-1336`) — requires an existing `Success` list,
  no-op when `loadingMore` or `pagination.page >= pagination.totalPages`; fetches
  `page = pagination.page + 1`, `pageSize = pagination.pageSize`; merges via `mergeMessagePages`
  (`ui/MessagePresentation.kt:39-45` — `(current + next).associateBy { it.id }.values.toList()`,
  i.e. **the newer copy of a duplicate id wins**, order preserved; frozen by
  `MessagePresentationTest.kt:57-72`). Failure → `actionMessage = apiFailureMessage("加载更多消息", …)`.

API: `GET /api/messages` with params `page`, `page_size`, plus optional `message_type`,
`is_read`, `priority`, `keyword` (only when the trimmed keyword is non-empty)
(`data/NovalPieApi.kt:768-786`).

### 5.9 Opening a message (type-8 special case)

`openMessage(message)` — `NovalPieViewModel.kt:1397-1410`
* If `message.type == 8` (私信) and a counterpart user id can be resolved:
  * fire-and-forget `api.markMessageRead(message.id)` when the message is unread
  * `openMessageConversation(targetUserId, message.username)` — jumps straight to the DM thread
* Otherwise `openMessageDetail(message.id)`.

`directMessageTargetUserId(message, currentUserId)` — `ui/MessagePresentation.kt:20-24`:
first of `[message.executeUserId, message.userId]` that is non-null and `!= currentUserId`
(tests: `MessagePresentationTest.kt:10-34`).

---

## 6. Message detail

### 6.1 Signature & wiring

`ui/MessageScreens.kt:373-443`

```kotlin
@Composable
internal fun MessageDetailScreen(
    state: MessageDetailState,
    onRetry: () -> Unit,
    onMarkRead: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    onOpenAction: (String) -> Unit,
    onOpenConversation: () -> Unit
)
```

Call site `NovalPieApp.kt:333-341`: `onRetry = { viewModel.loadMessageDetail(route.messageId) }`,
`onMarkRead = markCurrentMessageRead`, `onToggleStar = toggleCurrentMessageStar`,
`onDelete = deleteCurrentMessage`, `onOpenAction = openMessageAction`,
`onOpenConversation = openCurrentMessageConversation`.

State — `NovalPieViewModel.kt:236-241`:
`MessageDetailState(messageId = 0, detail = Idle, actionLoading = false, actionMessage = null)`.

### 6.2 Delete confirm dialog

`MessageScreens.kt:383-392`, local `confirmDelete`:
* title **`删除消息`**
* body **`删除后将从收件箱移除，确定继续吗？`**
* confirm `TextButton` **`删除`** → dismiss + `onDelete()`
* dismiss `TextButton` **`取消`**

### 6.3 Layout

`LazyColumn`, `contentPadding = 16.dp`, 14dp spacing (`MessageScreens.kt:393-397`).

`when (state.detail)` (`MessageScreens.kt:398-441`):
* `Idle` → `MessageEmpty("等待加载消息")`
* `Loading` → `MessageLoading("正在加载消息详情")`
* `Error` → `MessageError(detail.message, onRetry)`
* `Success` →
  1. **Detail card** — `ElevatedCard(shape = RoundedCornerShape(22.dp))`, 20dp padding,
     14dp spacing:
     * `AssistChip(onClick = {})` labeled `messageTypeLabel(message.type)` (inert chip)
     * `message.title` (`headlineSmall`, Bold)
     * Row (10dp gap): `message.username` when non-null (`SemiBold`), then
       `messageDateLabel(message.createdAt)` (`onSurfaceVariant`)
     * `SelectionContainer { Text(plainMessageText(message.content.orEmpty()), bodyLarge) }`
       — HTML-stripped body, user-selectable
     * **Extra data block** when `message.extraData.isNotEmpty()`:
       `Surface(surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(14.dp))`, 14dp padding,
       5dp spacing; heading **`附加信息`** (Bold), then one `bodySmall` line per entry rendered as
       `"$key: $value"` (raw keys, untranslated)
  2. **Action row** — `LazyRow`, 8dp gap (`MessageScreens.kt:430-438`), items in this exact order:
     * only when `!message.isRead`: `Button` `Icons.Filled.Check` + 6dp spacer + **`标记已读`**,
       `enabled = !state.actionLoading` → `onMarkRead`
     * `OutlinedButton` with `Icons.Filled.Star`/`StarBorder` + 6dp spacer + label
       `取消星标` when starred else `添加星标`, `enabled = !state.actionLoading` → `onToggleStar`
     * only when `message.type == 8`: `OutlinedButton` `Icons.Filled.Mail` + 6dp spacer +
       **`打开私信`** → `onOpenConversation`
     * only when `message.actionUrl != null`: `Button` labeled
       `message.actionText ?: "打开相关内容"` → `onOpenAction(url)`
     * `OutlinedButton` `Icons.Filled.Delete` + 6dp spacer + **`删除`**,
       `enabled = !state.actionLoading` → opens the delete dialog
  3. `state.actionMessage?.let { MessageNotice(it) }`

### 6.4 Detail VM actions

`loadMessageDetail(messageId)` — `NovalPieViewModel.kt:1418-1428`:
fresh `MessageDetailState(messageId, detail = Loading)`, then
`api.messageDetail(id)` = `GET /api/messages/{id}` (`NovalPieApi.kt:788-791`, unwraps
`message`/`data`/`result`, throws `IOException("NovalPie message detail is missing: {id}")`
when normalization fails). Error label `消息详情`.

`markCurrentMessageRead()` (:1430-1434) — skips when already read or `actionLoading`;
`runMessageDetailAction("已标记为已读") { api.markMessageRead(id) }`
(`POST /api/messages/{id}/read` body `{id}` — `NovalPieApi.kt:911-915`).

`toggleCurrentMessageStar()` (:1436-1442) — success message
`已取消星标` when currently starred else `已添加星标`; same star endpoint as §5.6.

`deleteCurrentMessage()` (:1444-1460) — sets `actionLoading = true`;
`api.deleteMessage(id)` = `DELETE /api/messages/{id}` body `{id, permanent:false}`
(`NovalPieApi.kt:935-947`; `permanent` defaults to `false` and is **never** set to true from UI).
On success: `goBack()` **and** `loadMessageCenter()`. On failure:
`actionMessage = apiFailureMessage("删除消息", failure)`.

`openCurrentMessageConversation()` (:1462-1467) — resolves the counterpart via
`directMessageTargetUserId(message, currentUserProfile()?.id)`; silently no-op if unresolvable.

`openMessageAction(actionUrl)` (:1469-1491) — deep-link resolution:
* Relative URLs become `https://novalpie.cc/{path without leading '/'}`.
* First path segment routing:
  * `forum` or `posts` → `openForumPost(segment[1].toLong())`, falling back to
    `openWebFallback(absolute)` when the id doesn't parse
  * `book` → no id → web fallback; id + chapter id → `openReader(bookId, chapterId)`;
    id only → `openBook(bookId)`
  * anything else → `openWebFallback(absolute)`

`runMessageDetailAction(successMessage, action)` — `NovalPieViewModel.kt:1641-1659`:
sets `actionLoading = true`, `actionMessage = null`; success →
`actionMessage = it.message ?: successMessage`; failure →
`apiFailureMessage(successMessage, failure)` (note the *success* string doubles as the failure
label, producing e.g. `已标记为已读请求失败: …`); on success re-runs `loadMessageDetail(messageId)`.

---

## 7. Message conversation (DM thread)

### 7.1 Signature & wiring

`ui/MessageScreens.kt:445-497`

```kotlin
@Composable
internal fun MessageConversationScreen(
    state: MessageConversationState,
    currentUserId: Long?,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
)
```

Call site `NovalPieApp.kt:343-349`:
`currentUserId = (viewModel.homeState.user as? LoadResult.Success)?.value?.id`,
`onRetry = { viewModel.loadMessageConversation(route.targetUserId, route.targetName) }`,
`onDraftChange = updateMessageDraft`, `onSend = sendMessageDraft`.

State — `NovalPieViewModel.kt:243-250`:
`MessageConversationState(targetUserId = 0, targetName = null, messages = Idle, draft = "",
sending = false, actionMessage = null)`.

### 7.2 Layout

Root `Column(Modifier.fillMaxSize().padding(horizontal = 14.dp))` (`MessageScreens.kt:453`).

1. **Local header row** (`MessageScreens.kt:454-464`) — 12dp vertical padding, `SpaceBetween`,
   `CenterVertically`:
   * Column:
     * `state.targetName ?: "私信对话"` (`titleLarge`, Bold)
     * `与用户 #{state.targetUserId} 的对话` (`bodySmall`, `onSurfaceVariant`)
   * `IconButton` with `Icons.Filled.Refresh`, contentDescription **`刷新`** → `onRetry`
   (This is *in addition to* the global top bar, whose subtitle reads `私信`.)
2. **History area** (`MessageScreens.kt:465-479`):
   * `Idle` → `MessageEmpty("等待加载私信")`
   * `Loading` → `MessageLoading("正在同步私信")`
   * `Error` → `MessageError(messages.message, onRetry)`
   * `Success` → `LazyColumn(Modifier.weight(1f).fillMaxWidth())`, `contentPadding` vertical 8dp,
     10dp spacing:
     * if empty: `MessageEmpty("还没有私信，发送第一条消息吧")` as the first item
     * `items(messages.value, key = { it.id }) { DirectMessageBubble(it, currentUserId) }`
   > Note: only the `Success` branch is wrapped in `weight(1f)`; the Idle/Loading/Error branches are
   > not weighted, so the composer sits directly under them.
   > Ordering is exactly the API order — no client-side sort, no date separators, no auto-scroll.
3. `state.actionMessage?.let { MessageNotice(it) }` (`MessageScreens.kt:480`)
4. **Composer** (`MessageScreens.kt:481-495`) — full-width Row, 10dp vertical padding,
   `CenterVertically`:
   * `OutlinedTextField(weight 1)`, placeholder **`输入私信内容`**, `maxLines = 4`,
     `imeAction = Send`, `keyboardActions.onSend = onSend`
   * 8dp spacer
   * `IconButton` `Icons.Filled.Send`, contentDescription **`发送`**,
     `enabled = state.draft.isNotBlank() && !state.sending` → `onSend`

### 7.3 `DirectMessageBubble` — own vs other styling

`ui/MessageScreens.kt:499-519`

```kotlin
@Composable
private fun DirectMessageBubble(message: DirectMessage, currentUserId: Long?)
```

* `val outgoing = currentUserId != null && message.executeUserId == currentUserId`
  (`MessageScreens.kt:501`) — **outgoing is determined by `executeUserId` only**; when
  `currentUserId` is null every bubble renders as incoming.
* Row alignment: `Arrangement.End` when outgoing, else `Arrangement.Start`.
* Bubble `Surface`:
  * color: outgoing → `primaryContainer`; incoming → `surfaceVariant`
  * shape: `topStart = 18, topEnd = 18`, `bottomStart = 18 if outgoing else 4`,
    `bottomEnd = 4 if outgoing else 18` (tail on the sender's side)
  * width: `Modifier.fillMaxWidth(0.82f)`
* Content: Column 13dp padding, 5dp spacing: `message.content` (default body style) then
  `messageDateLabel(message.createdAt)` (`labelSmall`, `onSurfaceVariant`).
* No avatars, no sender names, no read receipts, no failed-send retry affordance.

`DirectMessage` — `model/Models.kt:354-360`: `{id, content, createdAt?, userId?, executeUserId?}`.

### 7.4 Conversation VM actions

`openMessageConversation(targetUserId, targetName)` — `NovalPieViewModel.kt:1493-1498`:
ignores `targetUserId <= 0`; pushes `AppRoute.MessageConversation(targetUserId, targetName)`
(a data class, so the same pair de-duplicates via `pushDistinctRoute`) then loads.

`loadMessageConversation(targetUserId, targetName)` (:1500-1514): fresh state with
`messages = Loading`; `api.messageConversation(targetUserId)` =
`GET /api/messages/conversations?target_user_id&page=1&page_size=100`
(`NovalPieApi.kt:971-986`). Error label `私信对话`. **No pagination UI — the first 100 messages only.**

`updateMessageDraft(value)` (:1516-1518): sets draft, clears `actionMessage`.

`sendMessageDraft()` (:1520-1553):
* Requires `currentUserProfile()` **and** a non-null `profile.id`, non-blank trimmed content,
  `targetUserId > 0`, and `!sending` — otherwise silent no-op (no user-visible error).
* `api.sendDirectMessage(currentUserId, targetUserId, currentUserName, content)` →
  `POST /api/messages` (`NovalPieApi.kt:988-1000`) with body:
  `{user_id: targetUserId, execute_user_id: currentUserId, message_type: 8,
  message_title: "来自 {currentUserName.trim()} 的私信", message_content: content.trim()}`
* Success → clears the draft, `actionMessage = it.message ?: "私信已发送"`, then reloads the thread.
* Failure → `actionMessage = apiFailureMessage("发送私信", failure)`.

---

## 8. Message settings

### 8.1 Signature & wiring

`ui/MessageScreens.kt:521-625`

```kotlin
@Composable
internal fun MessageSettingsScreen(
    state: MessageSettingsState,
    onRetry: () -> Unit,
    onDraftChange: ((MessageSettings) -> MessageSettings) -> Unit,
    onSave: () -> Unit
)
```

Call site `NovalPieApp.kt:351-356`: `onRetry = loadMessageSettings`,
`onDraftChange = updateMessageSettingsDraft`, `onSave = saveMessageSettings`.

State — `NovalPieViewModel.kt:252-257`:
`MessageSettingsState(settings = Idle, draft = MessageSettings(), saving = false, actionMessage = null)`.

`MessageSettings` — `model/Models.kt:344-352`:
```kotlin
data class MessageSettings(
    val enableNotifications: Boolean = true,
    val enableEmail: Boolean = false,
    val enableBrowserPush: Boolean = true,
    val notificationTypes: Set<Int>? = null,   // null == all types
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val autoReadAfterDays: Int? = null
)
```

### 8.2 Layout

`LazyColumn`, `contentPadding = 16.dp`, 14dp spacing (`MessageScreens.kt:528-532`).

1. **Header** (`MessageScreens.kt:533-538`): `消息设置` (`headlineSmall`, Bold) and
   `管理通知方式、免打扰时间和自动已读` (`onSurfaceVariant`)
2. `when (state.settings)` (`MessageScreens.kt:539-623`):
   * `Idle` → `MessageEmpty("等待加载设置")`
   * `Loading` → `MessageLoading("正在同步消息设置")`
   * `Error` → `MessageError(settings.message, onRetry)`
   * `Success` → the four cards below + save button

**Card A — notification switches** (`MessageScreens.kt:544-558`),
`ElevatedCard(RoundedCornerShape(20.dp))`, 16dp padding, 14dp spacing, three
`MessageSettingSwitch(title, subtitle, checked, onToggle)`:

| Title | Subtitle | Field |
| --- | --- | --- |
| `启用通知` | `接收站内系统消息` | `enableNotifications` |
| `邮件通知` | `通过邮件接收重要通知` | `enableEmail` |
| `浏览器推送` | `允许设备推送消息` | `enableBrowserPush` |

Each toggle does `onDraftChange { it.copy(field = !it.field) }` — note the switch flips the draft
value rather than using the `Switch` callback's boolean.

`MessageSettingSwitch` — `MessageScreens.kt:627-637`: full-width Row `SpaceBetween`
`CenterVertically`; Column(weight 1) title (`SemiBold`) + subtitle (`bodySmall`,
`onSurfaceVariant`); 12dp spacer; `Switch(checked, onCheckedChange = { onToggle() })`.

**Card B — 通知类型** (`MessageScreens.kt:559-583`), `ElevatedCard(RoundedCornerShape(20.dp))`,
16dp padding, 10dp spacing:
* Title **`通知类型`** (`titleMedium`, Bold)
* Hint **`未筛选时接收全部类型`** (`bodySmall`, `onSurfaceVariant`)
* `LazyRow` (8dp gap) of `FilterChip` over `messageTypeOptions()` — 10 chips with the labels from
  §5.5(b) for types 1..10 (no `全部类型` chip here).
  * `selected = state.draft.notificationTypes?.contains(option.value) ?: true`
    (null set ⇒ **all chips appear selected**)
  * Toggle logic (`MessageScreens.kt:569-576`):
    ```kotlin
    onDraftChange { current ->
        val all = (1..10).toSet()
        val selected = current.notificationTypes ?: all
        val next = if (option.value in selected) selected - option.value else selected + option.value
        current.copy(notificationTypes = if (next == all) null else next)
    }
    ```
    i.e. deselecting from "all" materializes the set; reaching the full set again collapses back
    to `null`.

**Card C — 免打扰与自动处理** (`MessageScreens.kt:584-615`),
`ElevatedCard(RoundedCornerShape(20.dp))`, 16dp padding, 12dp spacing:
* Title **`免打扰与自动处理`** (`titleMedium`, Bold)
* Row (10dp gap) of two equal-weight `OutlinedTextField`s, both `singleLine`:
  * label **`开始 HH:mm`** ↔ `quietHoursStart` (blank input stored as `null`)
  * label **`结束 HH:mm`** ↔ `quietHoursEnd` (blank input stored as `null`)
* Full-width `OutlinedTextField`, `singleLine`, `keyboardType = Number`,
  label **`多少天后自动已读`**, supportingText **`0 表示不自动处理`**,
  value `autoReadAfterDays?.toString().orEmpty()`, `onValueChange` = `value.toIntOrNull()`
  (non-numeric input silently becomes `null`)

**Notice + Save** (`MessageScreens.kt:616-621`):
* `state.actionMessage?.let { MessageNotice(it) }`
* Full-width `Button`, disabled while `state.saving`, label `保存中...` when saving else
  **`保存消息设置`** → `onSave`

### 8.3 Validation & persistence

`validateMessageSettings(settings)` — `ui/MessagePresentation.kt:26-53`, returns the first error
or null:
* invalid `quietHoursStart` → **`免打扰开始时间格式无效`**
* invalid `quietHoursEnd` → **`免打扰结束时间格式无效`**
* `(autoReadAfterDays ?: 0) < 0` → **`自动已读天数不能小于 0`**

Clock validation: blank/null is allowed; otherwise must match
`Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")` (`MessagePresentation.kt:47-53`).
Tests: `MessagePresentationTest.kt:36-55`.

`loadMessageSettings()` — `NovalPieViewModel.kt:1560-1578`: fresh state with `settings = Loading`;
`api.messageSettings()` = `GET /api/messages/settings` (`NovalPieApi.kt:955-957`).
On success `settings = Success(s)` **and** `draft = s`. On failure both `settings` and
`actionMessage` are set to `apiFailureMessage("消息设置", failure)` — the error text is therefore
shown twice (in the `MessageError` card and, when reachable, as a notice).

`updateMessageSettingsDraft(transform)` (:1580-1585): applies the transform, clears `actionMessage`.

`saveMessageSettings()` (:1587-1612): no-op while saving; runs `validateMessageSettings` first and
surfaces its string as `actionMessage` without any network call; else `saving = true` and
`api.updateMessageSettings(draft)` = `PUT /api/messages/settings` (`NovalPieApi.kt:959-969`) with
body `{enable_notifications, enable_email, enable_browser_push}` plus optional
`notification_types` (JSON array), `quiet_hours_start`, `quiet_hours_end`, `auto_read_after_days`
(each omitted when null). Success → `settings = Success(draft)`,
`actionMessage = it.message ?: "消息设置已保存"`. Failure →
`apiFailureMessage("保存消息设置", failure)`.

Server normalization defaults (`NovalPieApi.kt:2403-2419`): `enable_notifications`/
`enableNotifications` default true, `enable_email` false, `enable_browser_push` true,
`notification_types`/`notificationTypes` parsed into a `Set<Int>` (absent ⇒ null),
`quiet_hours_start`/`quiet_hours_end` strings, `auto_read_after_days` int.

---

## 9. WebFallbackScreen

### 9.1 Signature & wiring

`ui/WebFallbackScreen.kt:23-59`

```kotlin
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebFallbackScreen(
    url: String,
    proxySettings: ProxySettings,
    authToken: String?,
    onAuthTokenCaptured: (String) -> Unit
)
```

Call site `NovalPieApp.kt:572-577`: `url = route.url`, `proxySettings = viewModel.proxySettings`,
`authToken = viewModel.authToken`, `onAuthTokenCaptured = viewModel::saveCapturedAuthToken`.

### 9.2 What it renders (user-visible chrome)

**Nothing of its own.** The composable body is a single
`AndroidView(modifier = Modifier.fillMaxSize()) { WebView(context) }`
(`WebFallbackScreen.kt:36-58`). There is:
* no title bar of its own, no URL bar, no reload button, no in-page back/forward controls,
* no loading indicator, no error page, no "open in external browser" affordance.

The only chrome is the app's global one (§0): the `CenterAlignedTopAppBar` with `NovalPie` plus the
`routeContextLabel` fallback (last bottom-tab label, since `WebFallback` has no entry), and the
`返回` back arrow. Bottom nav bar is hidden for this route.

WebView configuration (`WebFallbackScreen.kt:39-47`):
* `tag = webStateKey` where `webStateKey = "${proxySettings.summary()}:${authToken.orEmpty()}"`
  (`WebFallbackScreen.kt:31-32`) — used as the identity for "must reload" decisions
* `setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())` — theme-matched background,
  reapplied on every `update`
* `webViewClient = authSyncingWebViewClient(authToken, onAuthTokenCaptured)`
* `settings.javaScriptEnabled = true`
* `settings.domStorageEnabled = true`
* `settings.databaseEnabled = true`

`update` block (`WebFallbackScreen.kt:49-57`): recolors, re-installs the client, computes
`proxyChanged = webView.tag != webStateKey`, updates the tag, and reloads when
`proxyChanged || webView.url != url`.
> Consequence to preserve: because the state key includes the auth token, capturing a token
> triggers a fresh `loadUrl` of the same page.

### 9.3 Auth-token capture (localStorage + cookie)

`authSyncingWebViewClient` — `WebFallbackScreen.kt:115-125`: a `WebViewClient` whose
`onPageFinished` calls `syncAuthToken(view, authToken, onAuthTokenCaptured)`.

`syncAuthToken` — `WebFallbackScreen.kt:127-170`, two JS evaluations per page load:

**(1) Push the native token into the page** — only when `authToken` is non-blank
(`WebFallbackScreen.kt:132-148`). Token is quoted with `JSONObject.quote`:

```javascript
(function(){
  try {
    if (localStorage.getItem('auth_token') !== <quoted token>) {
      localStorage.setItem('auth_token', <quoted token>);
      if (location.pathname !== '/login') location.reload();
    }
  } catch (e) {}
  return true;
})()
```
So the app writes its saved token into `localStorage['auth_token']` and force-reloads the page —
except on `/login`, which is left alone so the user can actually log in.

**(2) Pull a token out of the page** — always (`WebFallbackScreen.kt:150-169`):

```javascript
(function(){
  try {
    var token = localStorage.getItem('auth_token') || '';
    if (!token) {
      var match = document.cookie.match(/(?:^|;\s*)auth_token=([^;]+)/);
      if (match) token = decodeURIComponent(match[1]);
    }
    return token || '';
  } catch (e) {
    return '';
  }
})()
```
Precedence: `localStorage['auth_token']` first, then the `auth_token` **cookie**
(URL-decoded). The JS string result is unwrapped by
`decodeJavascriptString(raw)` (`WebFallbackScreen.kt:172-176`) which trims, rejects `""`/`"null"`,
and parses via `JSONArray("[$value]").optString(0)`. A non-blank result is passed to
`onAuthTokenCaptured`.

`saveCapturedAuthToken(token)` — `NovalPieViewModel.kt:685-691`: trims; ignores blank or unchanged
tokens; otherwise persists to `AuthSessionStore` (prefs `novalpie_native_auth`, key `auth_token`),
sets `authToken`, and calls `loadHome()` (which re-derives `homeState.user`, including the admin
role, from the JWT immediately).

Note the app *also* feeds the WebView cookie jar into the native API client:
`cookieProvider = { CookieManager.getInstance().getCookie("https://novalpie.cc") }`
(`NovalPieViewModel.kt:438-441`) — so a WebView login also authenticates native API calls even
before a token is captured.

### 9.4 Proxy override behavior

`loadUrlAfterProxyReady(webView, url, settings, webStateKey, useEmulatorFallback)` —
`WebFallbackScreen.kt:61-101`:

1. If `WebViewFeature.PROXY_OVERRIDE` is unsupported → plain `webView.loadUrl(url)` and return.
2. Otherwise, inside `runCatching`, get `ProxyController.getInstance()` and prepare a
   `loadWhenReady` runnable that posts back to the WebView and loads the URL **only if
   `webView.tag == webStateKey`** (guards against a stale reload after settings changed).
3. `proxyUrl = webViewProxyUrl(settings, useEmulatorFallback)`:
   * non-null → build `ProxyConfig` with `addProxyRule(proxyUrl)`,
     `addBypassRule("127.0.0.1")`, `addBypassRule("localhost")`, `bypassSimpleHostnames()`,
     then `controller.setProxyOverride(config, executor, loadWhenReady)`
   * null → `controller.clearProxyOverride(executor, loadWhenReady)`
4. Any thrown exception → fall back to a direct `webView.loadUrl(url)`.

Executor is a `Handler(Looper.getMainLooper())` post (`WebFallbackScreen.kt:73-75`).

`webViewProxyUrl(settings, useEmulatorFallback)` — `WebFallbackScreen.kt:103-113` (internal,
directly unit-tested):
* explicit proxy wins: `settings.enabled && host non-blank && port in 1..65535` →
  `"http://{host}:{port}"`
* else if `!useEmulatorFallback` → `null` (real devices follow the system network)
* else → `"http://{DEFAULT_EMULATOR_PROXY_HOSTS.first()}:{DEFAULT_PROXY_PORT}"` = `http://10.0.2.2:7890`

`useEmulatorFallback = shouldPreferEmulatorProxy()` (`WebFallbackScreen.kt:34`), true on x86/x86_64
ABIs (`data/NetworkConfigStore.kt`).

Tests: `app/src/test/java/com/novalpie/nativeapp/ui/WebFallbackPolicyTest.kt:9-34`
(explicit proxy wins; emulator falls back to `http://10.0.2.2:7890`; real device → null).

### 9.5 Every URL that reaches this screen

`openWebFallback(url)` pushes `AppRoute.WebFallback(url)` (`NovalPieViewModel.kt:3705-3707`).
Complete set of call sites:

| Origin | URL |
| --- | --- |
| `openLoginFallback()` (`NovalPieViewModel.kt:3709-3711`) — used by Tools, Profile, Settings, Message center, Forum, Home, Search, Upload, PoliticalExam, ForumCreate, book-management gates | `https://novalpie.cc/login` |
| Settings `打开网站` (`NovalPieApp.kt:478`) / Forum `网页论坛` (`NovalPieApp.kt:191`) | `https://novalpie.cc` |
| Settings `网页搜索` (`NovalPieApp.kt:479`) / Search web action (`NovalPieApp.kt:261`) | `https://novalpie.cc/search?sort_by=relevance` |
| Home web action (`NovalPieApp.kt:235`) | `https://novalpie.cc/favorites` |
| Forum post detail web action (`NovalPieApp.kt:217`) | `https://novalpie.cc/posts/{postId}` |
| Book detail web action (`NovalPieApp.kt:500`) | `https://novalpie.cc/book/{bookId}` |
| Reader web action (`NovalPieApp.kt:569`) | `https://novalpie.cc/book/{bookId}/{chapterId}` |
| Tools unknown path (`NovalPieApp.kt:281`) | `https://novalpie.cc{path}` — unreachable with the current 11 entries |
| `openMessageAction` fallback (`NovalPieViewModel.kt:1469-1491`) | the message's `actionUrl`, absolutized against `https://novalpie.cc/` |

---

## 10. Cross-screen notes, quirks and gaps to carry through the refactor

1. **Two different "profile" surfaces with different titles.** The bottom-tab screen is
   `个人中心` (hardcoded in `ProfileScreens.kt:122`); the pushed Settings screen's overview card
   uses `productHeader(ProductSurface.Profile)` = `我的` (`ProductCopy.kt:41`). Both must survive.
2. **Settings reads `profileState`, refreshes `homeState`.** `SettingsScreen(user = viewModel.profileState.profile)`
   (`NovalPieApp.kt:463`) vs `onRefreshAccount = viewModel::loadHome` (`NovalPieApp.kt:471`), and
   `openSettings()` triggers no load (`NovalPieViewModel.kt:731-733`). Entering Settings cold shows
   `等待同步账号`, and `同步账号` cannot fix it.
3. **`profileSectionTitles()[3]` = `网页入口` is never rendered** — the Settings web-action row has
   no heading (`NovalPieApp.kt:2466-2472`).
4. **`UserSection` is dead code** (`NovalPieApp.kt:2619-2635`), retained only as history; it
   contains debug copy (`role:`, `/api/users/me`) that `ProfilePresentationTest.kt:141-162`
   explicitly forbids in shipped strings.
5. **Stats fields parsed but never displayed:** `MessageStats.readCount`, `MessageStats.unreadByType`
   (`model/Models.kt:295-303`); `UserProfile.avatarFrameUrl` (`Models.kt:152`);
   `SiteMessage.avatarUrl`/`avatarFrameUrl`/`readAt` (`Models.kt:317-321`);
   `UserActivity.coverUrl`/`commentId` (`Models.kt:188-189`);
   `UserCheckinSettings.autoCheckin` on the public profile screen (`Models.kt:199`).
6. **Tools stats strip has 5 pills including `7日`; the message-center hero has 4 without it.**
   (`NovalPieApp.kt:2341-2347` vs `MessageScreens.kt:264-269`.)
7. **`MessageCenterScreen` shows the hero, search box and all filters even when logged out** —
   only an extra `需要登录` card is added (`MessageScreens.kt:117-136`).
8. **`sendMessageDraft` fails silently** when the profile or its id is missing
   (`NovalPieViewModel.kt:1521-1522`) — no `actionMessage`.
9. **Selection is page-scoped.** `全选` only selects the ids currently in
   `messageCenterState.messages` (`NovalPieViewModel.kt:1370-1373`), including pages appended by
   `加载更多`; any successful batch action re-runs `loadMessageCenter()` and drops those extra pages.
10. **`runMessageDetailAction` reuses the success string as the failure label**
    (`NovalPieViewModel.kt:1641-1656`), producing strings like `已标记为已读请求失败: …`.
11. **DM ownership relies on `executeUserId` and `homeState.user.id`.** If `homeState.user` has not
    resolved (or has no `id`), every bubble is styled as incoming
    (`NovalPieApp.kt:345`, `MessageScreens.kt:501`).
12. **DM threads are capped at 100 messages** with no pagination UI (`NovalPieApi.kt:971-986`).
13. **`deleteMessage(permanent)` exists in the API but the UI always sends `permanent = false`**
    (`NovalPieApi.kt:935-947`; `NovalPieViewModel.kt:1449`).
14. **`clearAuthToken` does not clear WebView cookies/localStorage** — the native API's
    `cookieProvider` can still authenticate requests after `退出同步`
    (`NovalPieViewModel.kt:693-699` vs `NovalPieViewModel.kt:438-441`).
15. **Admin visibility is JWT-driven.** A token whose `role` claim is `admin` is sufficient to
    render entries 6-11 and to enter the native admin screens, even if `/api/users/me` fails
    (`AuthSessionStore.kt:25-27`; `NovalPieViewModel.kt:3752-3768`, `:1662-1663`).
16. **Mixed ellipsis characters** in loading labels: the Profile screen uses the U+2026 form
    (`头像上传中…`, `签到中…`, `保存中…`, `验证中…`) while the messaging screens use three ASCII dots
    (`加载中...`, `保存中...`). Preserve verbatim.
17. **`AppRoute.WebFallback` and `AppRoute.PoliticalExam` have no `routeContextLabel` entry**
    (`UiNavigation.kt:19-38`), so their top-bar subtitle leaks the last bottom-tab label.
18. **Switching bottom tabs clears the entire back stack** (`NovalPieViewModel.kt:726-728`), so an
    in-progress DM draft / message-settings draft is lost.
