# UI Content Inventory — Part 3 of 3: Authoring, Admin, Workspace

Repo: `D:/NovalPie/native-android`
Package root: `app/src/main/java/com/novalpie/nativeapp`
Generated from actual source reads (not docs). All Chinese strings below are **decoded** verbatim from the source; where the source stores them as `\uXXXX` escapes the decoded form is given and the escaped origin is noted.

Screens covered:

| # | Screen | Composable | File |
|---|--------|-----------|------|
| 1 | Workspace (工作区) | `WorkspaceScreen` | `ui/WorkspaceScreens.kt:76` |
| 2 | Upload book / append chapters | `UploadBookScreen` | `ui/UploadScreens.kt:56` |
| 3 | Upload / EPUB editor | `UploadEditorScreen` | `ui/UploadEditorScreens.kt:73` |
| 4 | Political exam (政治考试) | `PoliticalExamScreen` | `ui/PoliticalExamScreens.kt:43` |
| 5 | Admin (管理后台, 6 sections) | `AdminScreen` | `ui/AdminScreens.kt:55` |
| 6 | Book edit info | `BookEditInfoScreen` | `ui/BookEditScreens.kt:50` |
| 7 | Book chapter manager | `BookChapterManagerScreen` | `ui/BookChapterScreens.kt:62` |
| 8 | Forum create | `ForumCreateScreen` | `ui/ForumCreateScreens.kt:43` |

Backing data/engine layers documented in §9: `data/EpubParser.kt`, `data/EpubWriter.kt`, `data/EditorProcessor.kt`, `data/EditorArchiveStore.kt`, `data/UploadFileSource.kt`, `ui/EditorScriptEngine.kt`.

---

## 0. Shared navigation chrome that wraps every screen here

`ui/NovalPieViewModel.kt:104-129` — `sealed class AppRoute`. Routes relevant to this part:

```
object Workspace                       (AppRoute.kt:113)
object UploadBook                      (:114)
object UploadEditor                    (:115)
object PoliticalExam                   (:116)
object ForumCreate                     (:120)
data class BookEditInfo(bookId: Long)  (:122)
data class BookChapters(bookId: Long)  (:123)
data class BookAppend(bookId: Long)    (:124)
data class Admin(section: AdminSection)(:127)
```

`ui/NovalPieApp.kt:127-175` — global `Scaffold`:
- Top bar (`CenterAlignedTopAppBar`) is shown when `globalProductTopBarVisible(route)` (`ui/ReaderPresentation.kt:30-31` → true for everything except `AppRoute.Reader`). So **all 8 screens here get the global top bar.**
- Top bar title is two stacked lines: `"NovalPie"` (bold) then `routeContextLabel(route, currentTab)` in `labelSmall`.
- Navigation icon: back arrow `Icons.Filled.ArrowBack`, `contentDescription = "返回"`, calls `viewModel.goBack()`. Shown for every route that is not one of the 5 bottom-tab roots — i.e. shown on all 8 screens here.
- `BackHandler` at `ui/NovalPieApp.kt:123-125` maps hardware back to `goBack()` for all non-root routes.
- Bottom `NavigationBar` is **hidden** on all 8 screens (only shown for Forum/Home/Search/Tools/Profile).

`ui/UiNavigation.kt:19-38` — `routeContextLabel` (source uses `\uXXXX`; decoded):

| Route | Sub-title label |
|---|---|
| `Workspace` | 工作区 |
| `UploadBook` | 上传书籍 |
| `UploadEditor` | EPUB 编辑器 |
| `ForumCreate` | 发布帖子 |
| `BookEditInfo` | 编辑书籍信息 |
| `BookChapters` | 章节管理 |
| `BookAppend` | 追加章节 |
| `Admin` | 管理后台 |
| `PoliticalExam` | **MISSING** → falls through to `else -> bottomTabDisplayLabel(fallbackTab)`, so the exam screen shows the current bottom-tab name (usually 工具) instead of 政治考试. Preserve-or-fix decision needed in refactor. |

### Entry points (Tools tab)
`ui/ToolsPresentation.kt:10-50` — `toolsEntries(isAdmin)` produces the tiles that open these screens. Decoded:

| title | subtitle | path | adminOnly |
|---|---|---|---|
| 消息中心 | 通知、私信与用户互动 | `/messages` | no |
| 工作区 | 翻译接口、Cookie 与服务状态 | `/workspace` | no |
| 上传书籍 | 导入 EPUB 并提交到网站 | `/upload` | no |
| 上传编辑器 | 分章、替换、AI 正则与草稿 | `/upload-editor` | no |
| 政治考试 | 网站积分奖励入口 | `/political-exam` | no |
| 管理后台 | 管理员功能总览 | `/admin` | yes |
| 内容审核 | 审核与内容处理 | `/admin/review` | yes |
| 密钥管理 | API 密钥与使用状态 | `/admin/key-management` | yes |
| 操作日志 | 管理操作记录 | `/admin/operation-logs` | yes |
| 抓取管理 | 抓取器与任务状态 | `/admin/scraper-management` | yes |
| 商店管理 | 站内商店配置 | `/admin/shop` | yes |

Path→action dispatch: `ui/NovalPieApp.kt:272-284`. `/workspace`→`openWorkspace()`, `/upload`→`openUploadBook()`, `/upload-editor`→`openUploadEditor()`, `/political-exam`→`openPoliticalExam()`, anything matching an `AdminSection.websitePath`→`openAdminSection(section)`, else `openWebFallback("https://novalpie.cc$path")`.

`isAdminProfile(profile) = profile?.role == "admin"` (`ui/ProfilePresentation.kt:8`).

### Shared status widgets used by these screens
- `LoadingBlock(message: String)` — `ui/NovalPieApp.kt:3568-3574`: `LinearProgressIndicator(fillMaxWidth)` + message text.
- `ErrorBlock(message, retryLabel: String? = null, onRetry: (() -> Unit)? = null)` — `ui/NovalPieApp.kt:3576-3595`: `ElevatedCard` on `surfaceVariant`, message + optional `OutlinedButton` with `retryLabel`.
- `ImagePreviewDialog(imageUrl, title, onDismiss)` — `ui/ImagePreviewDialog.kt:62`: full-screen `Dialog` (`usePlatformDefaultWidth = false`, `dismissOnClickOutside = false`), background `Color(0xF20B0D12)`, pinch/pan `rememberTransformableState`, double-tap zoom, Coil `size(3072,3072)`, `contentDescription = "$title 大图"`.
- `apiFailureMessage(label, throwable)` — `ui/ApiMessages.kt:3-6`: renders `"${label}请求失败: ${detail}"`; when the detail matches `NovalPie API (\d+)` it becomes `"服务返回错误 <status>"`. Every failure toast/notice string in this part is built through this helper, so the refactor must keep the label strings intact (they are listed per screen below).

---

## 1. Workspace screen (工作区)

### 1.1 Signature
`ui/WorkspaceScreens.kt:75-89`
```kotlin
@Composable
internal fun WorkspaceScreen(
    state: WorkspaceState,
    onRefresh: () -> Unit,
    onTabSelected: (WorkspaceTab) -> Unit,
    onSaveApi: (WorkspaceApiDraft) -> Unit,
    onDeleteLocalApi: (WorkspaceLocalApiConfig) -> Unit,
    onDeleteServerApi: (WorkspaceApiConfig) -> Unit,
    onSaveCookie: (WorkspaceCookieDraft) -> Unit,
    onToggleCookie: (WorkspaceCookieConfig) -> Unit,
    onDeleteCookie: (WorkspaceCookieConfig) -> Unit,
    onUpdateJobStatus: (WorkspaceTranslationJob, String) -> Unit,
    onDeleteJob: (WorkspaceTranslationJob) -> Unit,
    onOpenUpload: () -> Unit
)
```
Wiring: `ui/NovalPieApp.kt:358-371`.

### 1.2 State
`ui/NovalPieViewModel.kt:259-269`
```kotlin
data class WorkspaceState(
    selectedTab: WorkspaceTab = WorkspaceTab.Overview,
    apiConfigs: LoadResult<List<WorkspaceApiConfig>> = Idle,
    cookieStatus: LoadResult<WorkspaceCookieStatus> = Idle,
    cookieConfigs: LoadResult<WorkspaceCookieConfigs> = Idle,
    health: LoadResult<WorkspaceHealth> = Idle,
    localApis: List<WorkspaceLocalApiConfig> = emptyList(),
    jobs: List<WorkspaceTranslationJob> = emptyList(),
    actionLoading: Boolean = false,
    actionMessage: String? = null
)
```

### 1.3 Layout
Root: `LazyColumn(fillMaxSize, contentPadding = start/top/end 16.dp, bottom 40.dp, spacedBy 14.dp)` (`:90-94`).

Item order:
1. `WorkspaceHero(state, onRefresh)` (`:95`, defined `:118-144`)
2. Tab rail — `LazyRow(spacedBy 8.dp)` of `FilterChip` for every `WorkspaceTab.values()`, label `tab.label` (`:96-106`)
3. `state.actionMessage?.let { WorkspaceNotice(it) }` (`:107`)
4. Tab body: `Overview | Apis | Cookies | Queue` (`:109-114`)

### 1.4 Tabs
`ui/WorkspacePresentation.kt:5-10` — `enum class WorkspaceTab(val label: String)`:
| enum | label (decoded) | source escape |
|---|---|---|
| `Overview` | 概览 | `\u6982\u89c8` |
| `Apis` | API 管理 | `API \u7ba1\u7406` |
| `Cookies` | Cookie 管理 | `Cookie \u7ba1\u7406` |
| `Queue` | 任务队列 | `\u4efb\u52a1\u961f\u5217` |

### 1.5 Hero (`WorkspaceHero`, `:118-144`)
- `Box(fillMaxWidth, clip RoundedCornerShape(24.dp), background Brush.linearGradient(listOf(Color(0xFF0F172A), colorScheme.primary)), padding 20.dp)`.
- Title `工作区` (`headlineSmall`, Bold, white); subtitle `管理 API、Cookie 与翻译任务` (white α 0.78).
- `IconButton(onRefresh)` with `Icons.Filled.Refresh`, `contentDescription = "刷新"`, tint white.
- Stat rail `LazyRow(spacedBy 8.dp)` of `WorkspaceHeroStat(label, value)` (`:146-154`, `Surface` white α 0.14, radius 14.dp):
  | label | value expression |
  |---|---|
  | `API` | `health?.apiStatus?.total ?: 0` |
  | `健康` | `health?.apiStatus?.healthy ?: 0` |
  | `Cookie` | `cookieConfigs.success?.myConfigs?.size ?: 0` |
  | `任务` | `state.jobs.size` |

### 1.6 Overview tab (`workspaceOverviewItems`, `:156-212`)
- Header row: title `健康状态与统计` (`titleLarge` Bold) + subtitle `监控可用大模型与 Cookie 状态`; right `TextButton` `刷新` → `onRefresh`.
- `state.health` states:
  - `Idle`/`Loading` → `WorkspaceLoading("正在检查服务状态")`
  - `Error` → `WorkspaceError(message, onRefresh)` (retry button `重试`)
  - `Success`:
    - 3 metric cards in a `Row(spacedBy 10.dp)`, each `weight(1f)`, via `WorkspaceMetricCard(label, value)` (`:214-222`, `ElevatedCard` radius 16.dp, big value in `primary`):
      `配置` = `apiStatus.total`, `激活` = `apiStatus.active`, `健康` = `apiStatus.healthy`
    - if `translators.isEmpty()` → `WorkspaceEmpty("暂无翻译器健康数据")`
    - else one `ElevatedCard(radius 18.dp)` per translator, keyed by `it.id`:
      - Row: `translator.name` (Bold) + `WorkspaceStatusChip(if (isHealthy && isActive) "健康" else "异常", good)`
      - `listOfNotNull(model, endpoint).joinToString(" · ")` in `bodySmall`/`onSurfaceVariant`
      - `"${responseTimeMs} ms · ${successRate}%"` in `labelMedium`
      - `lastHealthError?.let { Text(it, color = error, bodySmall) }`
- Trailing CTA card: `ElevatedCard(containerColor = secondaryContainer)` with title `上传新书` (Bold), subtitle `使用专业编辑器处理文本与 EPUB`, and `Button(onOpenUpload)` containing `Icons.Filled.Upload` + `打开`.

### 1.7 API tab (`workspaceApiItems`, `:224-247`)
- `WorkspaceApiHeader(onSaveApi)` (`:249-260`): title `API 管理` (`titleLarge` Bold), subtitle `本地配置与服务器共享分开管理`, `Button` with `Icons.Filled.Add` + `添加` → opens `WorkspaceApiDialog(WorkspaceApiDraft())`.
- Section label `本地 API` (`titleMedium` Bold).
  - Empty: `WorkspaceEmpty("暂无本地 API，可以添加第一个 API")`.
  - Items keyed `"local-${it.id}"` → `WorkspaceLocalApiCard` (`:262-284`).
- Section label `服务器共享 API`.
  - `state.apiConfigs`: `Idle`/`Loading` → `WorkspaceLoading("正在同步 API 配置")`; `Error` → `WorkspaceError(message, null)` (no retry button); `Success` empty → `WorkspaceEmpty("暂无服务器共享 API")`; items keyed `"server-${it.id}"` → `WorkspaceServerApiCard` (`:286-308`).

**Card body** `WorkspaceApiCardBody(name, model, endpoint, apiKey, badges, onEdit, onDelete)` (`:310-331`):
- `ElevatedCard(radius 18.dp)`, `Icons.Filled.Key` tinted `primary` + `name` Bold; right: `IconButton(Edit, contentDescription "编辑")`, `IconButton(Delete, contentDescription "删除")`.
- Line: `"$model · ${maskWorkspaceApiKey(apiKey)}"`.
- Line: `endpoint`, `bodySmall`, `onSurfaceVariant`, `maxLines = 1`, ellipsis.
- Badge rail: `LazyRow(spacedBy 6.dp)` of `AssistChip(onClick = {})` — non-interactive chips.

Badges per card type:
- Local (`:280`): `if (sharedToServer) "已共享" else "仅本机"`, and `"并发 ${config.concurrency}"`.
- Server (`:304`): `listOfNotNull(approvalStatus, if (isHealthy == true) "健康" else "未检测", "${totalRequests} 次")`.

`maskWorkspaceApiKey` (`ui/WorkspacePresentation.kt:32-37`): blank → `未配置`; length ≤ 8 → `********`; else `first4 + "******" + last4`.

**Edit/delete dialogs**
- Local edit seeds `WorkspaceApiDraft(config.id, config.serverId, name, model, endpoint, apiKey, concurrency.toString(), sharedToServer)` (`:271`).
- Local delete: `WorkspaceDeleteDialog("删除 API", "确定删除 ${config.name} 吗？")` (`:274`).
- Server edit seeds `WorkspaceApiDraft(serverId = config.id, …, apiKey = config.apiKey.orEmpty(), shareToServer = true)` (`:295`).
- Server delete: `WorkspaceDeleteDialog("删除共享 API", "该配置将从服务器删除。")` (`:298`).
- `WorkspaceDeleteDialog(title, message, onDismiss, onConfirm)` (`:478-481`): confirm `删除`, dismiss `取消`.

**`WorkspaceApiDialog`** (`:433-454`)
- Title: `添加 API` when `initial.id == null && initial.serverId == null`, else `编辑 API`.
- Fields (all `OutlinedTextField`, `Column(spacedBy 8.dp)`):
  | label | binding | notes |
  |---|---|---|
  | `API 名称` | `draft.name` | singleLine |
  | `模型` | `draft.model` | singleLine |
  | `API 端点` | `draft.endpoint` | singleLine |
  | `API Key` | `draft.apiKey` | singleLine, `PasswordVisualTransformation()` |
  | `并发数` | `draft.concurrency` | singleLine, `KeyboardType.Number` |
- Switch row: title `共享到服务器` (SemiBold) + helper `关闭时只保存在本机`, bound to `draft.shareToServer`.
- Inline `error` text in `colorScheme.error`/`bodySmall`.
- Confirm `保存` runs `validateWorkspaceApiDraft(draft)`; on `null` calls `onSave`, else sets `error`. Dismiss `取消`.

**`WorkspaceApiDraft`** (`ui/WorkspacePresentation.kt:12-21`) — defaults are load-bearing:
```kotlin
id = null, serverId = null, name = "",
model = "deepseek-chat",
endpoint = "https://api.deepseek.com",
apiKey = "", concurrency = "10", shareToServer = false
```

**`validateWorkspaceApiDraft`** (`ui/WorkspacePresentation.kt:39-47`) — returns first failing message:
1. blank name → `API 名称不能为空`
2. blank model → `模型不能为空`
3. `!isHttpUrl(endpoint)` → `API 端点必须是 http(s) URL`
4. blank apiKey → `API Key 不能为空`
5. `concurrency` not an Int in `1..100` → `并发数必须介于 1 到 100`

`isHttpUrl` (`:58-61`): `URI(value.trim())` must parse, `scheme in {http, https}`, non-blank host.

### 1.8 Cookie tab (`workspaceCookieItems`, `:333-356`)
- `WorkspaceCookieHeader(state.cookieStatus, onSaveCookie)` (`:358-369`): title `Cookie 管理` (`titleLarge` Bold); subtitle is `服务器已有可用 Cookie` when `cookieStatus.success.hasCookie == true`, else `尚未确认可用 Cookie`; `Button` `Icons.Filled.Add` + `添加` opens `WorkspaceCookieDialog(WorkspaceCookieDraft())`.
- `state.cookieConfigs`: `Idle`/`Loading` → `WorkspaceLoading("正在同步 Cookie 配置")`; `Error` → `WorkspaceError(message, null)`; `Success`:
  - label `我的配置`; empty → `WorkspaceEmpty("暂无 Cookie 配置")`; items keyed `"mine-${id}"`, `editable = true`
  - label `其他共享配置`; empty → `WorkspaceEmpty("暂无其他共享 Cookie")`; items keyed `"shared-${id}"`, `editable = false`

**`WorkspaceCookieCard(config, editable, onSave, onToggle, onDelete)`** (`:371-402`)
- `ElevatedCard(radius 18.dp)`; header `Icons.Filled.Security` tinted `primary` + `config.configKey` Bold; right `WorkspaceStatusChip`:
  - `isHealthy == true` → `健康` (good)
  - `isHealthy == false` → `异常`
  - `null` → `未检测`
- `config.description?.let { Text(it, bodySmall) }`
- `config.proxyIp ?: "无代理"` (`bodySmall`, `onSurfaceVariant`)
- `"提供人: ${config.updatedByUsername ?: "我"} · ${config.lastCheckAt ?: "未检测"}"` (`labelSmall`)
- Only when `editable`: `OutlinedButton 编辑`, `OutlinedButton` labelled `禁用` when `isActive` else `启用` (→ `onToggle`), `TextButton 删除` (→ delete dialog `删除 Cookie` / `此操作不可撤销。`).
- Edit dialog seeds `WorkspaceCookieDraft(config.id, config.configKey, config.description.orEmpty(), "", config.proxyIp.orEmpty(), config.isActive)` — note `cookieRaw` intentionally seeded empty.

**`WorkspaceCookieDialog`** (`:456-476`)
- Title `添加 Cookie` when `initial.id == null` else `编辑 Cookie`.
- Fields:
  | label | binding | notes |
  |---|---|---|
  | `配置键名` | `configKey` | singleLine; `enabled = initial.id == null` (immutable on edit) |
  | `配置说明` | `description` | multi-line default |
  | `Cookie 内容` (new) / `Cookie 内容（留空表示不修改）` (edit) | `cookieRaw` | `minLines = 3` |
  | `代理配置` | `proxyIp` | `supportingText = "IP:PORT 或 http(s)://user:pass@host:port"` |
- Switch row `启用此配置` bound to `isActive`.
- Confirm `保存` runs `validateWorkspaceCookieDraft`; dismiss `取消`.

**`WorkspaceCookieDraft`** (`ui/WorkspacePresentation.kt:23-30`): `id=null, configKey="", description="", cookieRaw="", proxyIp="", isActive=true`.

**`validateWorkspaceCookieDraft`** (`:49-56`):
1. new (`id == null`) and blank `configKey` → `配置键名不能为空`
2. new and blank `cookieRaw` → `Cookie 内容不能为空`
3. `proxyIp` non-blank and `!isProxyValue(proxyIp)` → `代理格式应为 IP:PORT 或 http(s)://...`

`isProxyValue` (`:63-70`): if it starts with `http://`/`https://`, must parse as URI with non-blank host and `port in 1..65535`; otherwise must match `HOST_PORT_REGEX = ^[^:\s]+:\d{1,5}(?::[^:\s]+:[^:\s]+)?$` (i.e. `host:port` optionally followed by `:user:pass`) and the port must be in `1..65535`.

### 1.9 Queue tab (`workspaceQueueItems`, `:404-431`)
- Header: title `任务队列` (`titleLarge` Bold), subtitle `本机翻译任务与进度`; right `OutlinedButton 上传新书` → `onOpenUpload`.
- Empty → `WorkspaceEmpty("任务队列为空")`.
- Per job (`ElevatedCard(radius 18.dp)`, keyed `it.id`):
  - Row: `job.bookTitle` (Bold, `weight(1f)`, `maxLines = 1`, ellipsis) + `WorkspaceStatusChip(job.status, job.status == "completed")` — the chip shows the raw status string (`pending`/`paused`/`completed`/…), untranslated.
  - `"${job.translatorName} · ${job.completedChapters}/${job.chapterCount} 章"` (`bodySmall`).
  - When `chapterCount > 0`: `LinearProgressIndicator(progress = (completed / count).coerceIn(0f, 1f))`.
  - Actions row: if `status == "paused"` → `Button` (`Icons.Filled.PlayArrow` + `继续`) calling `onUpdateStatus(job, "pending")`; else if `status != "completed"` → `OutlinedButton` (`Icons.Filled.Pause` + `暂停`) calling `onUpdateStatus(job, "paused")`; always `TextButton 删除` → `onDeleteJob`.

### 1.10 Shared workspace atoms
- `WorkspaceStatusChip(label, good)` (`:483-488`): `Surface` `Color(0xFFDCFCE7)` when good else `surfaceVariant`; text color `Color(0xFF166534)` when good else `onSurfaceVariant`; radius 20.dp, padding h10/v5, `labelMedium`.
- `WorkspaceLoading(label)` (`:490-491`): `LinearProgressIndicator` + label in `onSurfaceVariant`, vertical padding 18.dp.
- `WorkspaceError(message, onRetry?)` (`:493-494`): `ElevatedCard(containerColor = errorContainer)`, message + optional `OutlinedButton 重试`.
- `WorkspaceEmpty(label)` (`:496-497`): `Surface(surfaceVariant α 0.45, radius 16.dp)`, padding 18.dp.
- `WorkspaceNotice(message)` (`:499-500`): `Surface(secondaryContainer, radius 14.dp)`, padding 12.dp.

### 1.11 Behaviour (ViewModel)
`ui/NovalPieViewModel.kt`
- `openWorkspace()` (`:2479-2482`) pushes `AppRoute.Workspace` via `pushDistinctRoute` then `loadWorkspace()`. **No login gate.**
- `selectWorkspaceTab(tab)` (`:2484-2486`) sets tab, clears `actionMessage`.
- `loadWorkspace()` (`:2488-2516`): bumps `workspaceRequestSerial`; sets 4 `LoadResult.Loading`; refreshes `localApis`/`jobs` **synchronously from `WorkspaceLocalStore`**; then fires 4 parallel `async` calls — `api.workspaceApiConfigs()`, `api.workspaceCookieStatus()`, `api.workspaceCookieConfigs()`, `api.workspaceHealth()` — and applies with staleness guard `isFreshRequestSerial`. Error labels (decoded): `工作区 API 配置`, `Cookie 状态`, `Cookie 配置`, `工作区健康状态`.
- `saveWorkspaceApi(draft)` (`:2518-2579`): validates first (message goes to `actionMessage`); short-circuits if `actionLoading`. Server behaviour matrix:
  | condition | server call |
  |---|---|
  | `shareToServer && serverId != null` | `api.updateWorkspaceApi(id, name, model, endpoint, apiKey, concurrency)` |
  | `shareToServer && serverId == null` | `api.createWorkspaceApi(...)` |
  | `!shareToServer && serverId != null` | `api.deleteWorkspaceApi(serverId)` (un-share) |
  | `!shareToServer && serverId == null` | no call — synthetic `WorkspaceActionResult(success = true)` |
  Then always upserts a local record via `workspaceLocalStore.upsertApi(...)` with `id = draft.id ?: System.currentTimeMillis()` and `serverId = if (shareToServer) serverResult.id ?: draft.serverId else null`. Success message `it.message ?: "API 配置已保存"`; failure label `保存 API 配置`. Reloads workspace on success.
- `deleteWorkspaceLocalApi(config)` (`:2581-2606`): deletes the server copy first if `config.serverId != null`, then local. Success `API 配置已删除`; failure label `删除 API 配置`.
- `deleteWorkspaceServerApi(config)` (`:2608-2610`) → `runWorkspaceAction("API 配置已删除") { api.deleteWorkspaceApi(config.id) }`.
- `saveWorkspaceCookie(draft)` (`:2612-2636`): validates; new → `api.createWorkspaceCookie(configKey, description, cookieRaw, proxyIp, isActive)`; existing → `api.updateWorkspaceCookie(id, description, cookieRaw.takeIf { it.isNotBlank() }, proxyIp, isActive)` (blank raw = keep existing). Success `Cookie 配置已保存`.
- `toggleWorkspaceCookie` (`:2638-2642`) → `api.setWorkspaceCookieActive(id, !isActive)`, success `Cookie 状态已更新`.
- `deleteWorkspaceCookie` (`:2644-2648`) → `api.deleteWorkspaceCookie(id)`, success `Cookie 配置已删除`.
- `updateWorkspaceJobStatus(job, status)` (`:2650-2656`): **purely local** — `workspaceLocalStore.upsertJob(job.copy(status = status, updatedAt = now))`, message `任务状态已更新`. No server call.
- `deleteWorkspaceJob(job)` (`:2658-2664`): local delete, message `任务已删除`.
- `runWorkspaceAction(successMessage, action)` (`:2666-2690`): guards `actionLoading`, sets loading, uses `it.message ?: successMessage`, failure via `apiFailureMessage(successMessage, failure)` (note: the *success* message doubles as the failure label), reloads workspace on success.

### 1.12 Local persistence for Workspace
`data/WorkspaceLocalStore.kt` — `SharedPreferences` name `novalpie_native_workspace`, keys `api_configs`, `translation_jobs`, JSON arrays.
- `loadApis/upsertApi/deleteApi` — API JSON fields: `id, name, model, endpoint, api_key, concurrency, shared_to_server, server_id`. `concurrency` defaults 10.
- `loadJobs/upsertJob/deleteJob` — job JSON fields: `id, book_id, book_title, translator_id, translator_name, chapter_count, completed_chapters, status, created_at, updated_at`. `status` blank → `pending`. Rows missing `id` or `book_id` are dropped.
- `clearAll()` removes both keys.
- **The API key is stored in plain text in SharedPreferences.** This same store feeds the editor's AI-regex config list.

### 1.13 Workspace API surface (`data/NovalPieApi.kt`)
`workspaceApiConfigs()` :797, `workspaceCookieStatus()` :801, `workspaceCookieConfigs()` :808, `workspaceHealth()` :812, `createWorkspaceApi(...)` :818, `updateWorkspaceApi(...)` :838, `deleteWorkspaceApi(id)` :859, `createWorkspaceCookie(...)` :863, `updateWorkspaceCookie(...)` :879, `setWorkspaceCookieActive(id, isActive)` :895, `deleteWorkspaceCookie(id)` :905.

Models: `WorkspaceApiConfig` (`model/Models.kt:367`), `WorkspaceCookieStatus` (:380), `WorkspaceCookieConfig` (:384), `WorkspaceCookieConfigs` (:396 — `myConfigs`/`sharedConfigs`), `WorkspaceApiStatus` (:401 — `total/active/healthy/totalRequests`), `WorkspaceTranslatorHealth` (:408), `WorkspaceHealth` (:421), `WorkspaceActionResult` (:426), `WorkspaceLocalApiConfig` (:432), `WorkspaceTranslationJob` (:443).

---

## 2. Upload book screen / Append chapters (上传书籍 / 追加章节)

One composable serves **two routes**: `AppRoute.UploadBook` (`ui/NovalPieApp.kt:373-383`) and `AppRoute.BookAppend(bookId)` (`:538-548`), with identical callback wiring. The append variant is distinguished purely by `state.existingNovelId != null`.

### 2.1 Signature
`ui/UploadScreens.kt:55-66`
```kotlin
@Composable
fun UploadBookScreen(
    state: UploadBookState,
    hasAuthToken: Boolean,
    onOpenLogin: () -> Unit,
    onPickEpub: (String) -> Unit,
    onDraftChange: (UploadBookDraft) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenBook: (Long) -> Unit
)
```

### 2.2 State + draft
`ui/NovalPieViewModel.kt:271-288`
```kotlin
data class UploadDocument(uri: String, displayName: String, sizeBytes: Long, mimeType: String? = null)

data class UploadBookState(
    existingNovelId: Long? = null,
    draft: UploadBookDraft = UploadBookDraft(),
    selectedFile: UploadDocument? = null,
    chapters: LoadResult<List<UploadChapter>> = Idle,
    serverFilePath: String? = null,
    processing: Boolean = false,
    progressLabel: String? = null,
    submitResult: LoadResult<UploadActionResult> = Idle,
    actionMessage: String? = null
)
```
`ui/UploadPresentation.kt:8-22`
```kotlin
data class UploadBookDraft(
    title = "", titleTranslation = "", author = "", description = "",
    language = "ja",            // NOTE: default is Japanese
    spans = "balanced",         // never exposed in UI, sent to server
    isAdult = false, source = "", sourceUrl = "", tagsText = "",
    submitType = "chinese", coverUrl = "", chapterCount = 0
)
```

Constants (`ui/UploadPresentation.kt:3-6`):
```kotlin
const val WEBSITE_UPLOAD_CHUNK_BYTES = 5L * 1024L * 1024L            // 5 MiB
const val WEBSITE_SERVER_EPUB_THRESHOLD_BYTES = 50L * 1024L * 1024L  // 50 MiB
enum class UploadParseMode { LOCAL, SERVER_CHUNKED }
```
`uploadParseMode(sizeBytes)` (`:38-39`): `> 50 MiB` → `SERVER_CHUNKED`, else `LOCAL`.

### 2.3 Layout
Root `LazyColumn(fillMaxSize, contentPadding = 20.dp, spacedBy 16.dp)` (`:73-77`). Locals: `picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` → `onPickEpub(uri.toString())`; `chapters = state.chapters.success.orEmpty()`; `appendMode = state.existingNovelId != null`.

Item order:
1. **Hero** `UploadHero(chapterCount, processing, appendMode, onOpenEditor)` (`:78-85`, defined `:186-220`)
2. **Append banner** — only when `appendMode` (`:87-96`)
3. **Login banner** — only when `!hasAuthToken` (`:98-117`)
4. **File card** `UploadFileCard` (`:119-126`, defined `:222-268`)
5. **Progress card** — only when `state.progressLabel != null` (`:128-143`)
6. **Notice** — only when `state.actionMessage != null` (`:145-147`)
7. **Metadata card** — only when `!appendMode` (`:149-151`, defined `:270-306`)
8. **Submission-type card** `UploadSubmissionCard` (`:153-155`, defined `:308-335`)
9. **Chapter preview** `UploadChapterSection` (`:157-159`, defined `:337-368`)
10. **Submit result** `UploadSubmitResult` (`:161-163`, defined `:370-387`)
11. **Submit button + disclaimer** (`:165-182`)

### 2.4 Hero (`:186-220`)
- `Box(fillMaxWidth, Brush.linearGradient(listOf(primary, Color(0xFF7C3AED), Color(0xFFDB2777)), shape = RoundedCornerShape(24.dp)), padding 22.dp)`.
- Pill `Surface(white α 0.17, radius 30.dp)` with `labelMedium` text **`NOVALPIE STUDIO`**.
- Headline (`headlineMedium`, ExtraBold, white): `追加章节` in append mode, otherwise `上传书籍`.
- Body (white α 0.9): `沿用源站 EPUB、章节与翻译提交协议，并针对 Android 大文件做流式处理。`
- Chip rail: `AssistChip` label `"$chapterCount 章已就绪"` when `chapterCount > 0`, else `等待 EPUB`; plus a second `AssistChip 处理中` when `processing`.
- `OutlinedButton(onOpenEditor)` with `Icons.Filled.Edit` + white text `打开文本 / EPUB 编辑器`.

### 2.5 Append banner (`:87-96`)
`Surface(primaryContainer, radius 18.dp)`, padding 16.dp:
- `正在追加到书籍 #${state.existingNovelId}` (Bold)
- `只新增解析后的章节，不覆盖书名、作者、封面和已有章节。` (`bodySmall`)

### 2.6 Login banner (`:98-117`)
`Surface(errorContainer, radius 18.dp)`:
- `需要登录` (Bold) + `源站上传接口要求有效账号会话。` (`bodySmall`)
- `Button(onOpenLogin)` labelled `去登录`.
Note the screen itself is still fully usable/parsable without a token; only the submit button is disabled.

### 2.7 File card (`UploadFileCard`, `:222-268`)
- `ElevatedCard(radius 20.dp)`, padding 18.dp, `spacedBy 14.dp`.
- Header: `Surface(primaryContainer, radius 14.dp)` holding `Icons.Filled.AutoStories` (padding 12.dp, size 26.dp, tint primary); title `EPUB 文件` (`titleMedium` Bold); subtitle `源站阈值：50 MiB；超出后自动 5 MiB 分片`.
- If no document: `Surface(surfaceVariant α 0.55, radius 16.dp)` with `选择 EPUB 后将自动读取元数据、目录和正文。图片不会被整体解码到内存。`
- If document: `document.displayName` (SemiBold, `maxLines = 2`, ellipsis) and `formatUploadBytes(document.sizeBytes)`.
- Buttons row: `Button(onPick, enabled = !processing, weight 1f)` with `Icons.Filled.UploadFile` + (`选择 EPUB` when none else `更换文件`); and, when a document exists, `OutlinedButton(onClear, enabled = !processing)` with `Icons.Filled.Delete` + `清空`.
- Picker MIME filter: `arrayOf("application/epub+zip", "application/octet-stream", "*/*")` (`:123`).
- `formatUploadBytes(value)` (`:403-409`): `< 0` → `大小未知`; ≥ 1 GiB → `"%.2f GiB"`; ≥ 1 MiB → `"%.2f MiB"`; ≥ 1 KiB → `"%.1f KiB"`; else `"$value B"`.

### 2.8 Progress card (`:128-143`)
`ElevatedCard(containerColor = primaryContainer)`: indeterminate `LinearProgressIndicator`, `state.progressLabel` (SemiBold), then fixed copy `文件内容采用流式读取，不会把大型 EPUB 和图片一次性分配到内存。`

### 2.9 Notice (`UploadNotice`, `:389-401`)
`Surface(errorContainer if isError else secondaryContainer, radius 16.dp)`, padding 14.dp. `isError` is computed at the call site as `state.submitResult is LoadResult.Error || state.chapters is LoadResult.Error` (`:146`).

### 2.10 Metadata card (`UploadMetadataCard`, `:270-306`) — hidden in append mode
`ElevatedCard(radius 20.dp)`, padding 18.dp, `spacedBy 12.dp`.
- Title `书籍信息` (`titleLarge` Bold); subtitle `字段与源站上传页保持一致`.
- Fields (all `OutlinedTextField(fillMaxWidth, enabled = !processing)`):
  | label | binding | shape |
  |---|---|---|
  | `中文书名 *` | `title` | singleLine |
  | `原文书名（可选）` | `titleTranslation` | singleLine |
  | `作者 *` | `author` | singleLine |
  | `简介` | `description` | `minLines = 4` |
  | `来源` | `source` | singleLine |
  | `来源链接` | `sourceUrl` | singleLine |
  | `封面图片链接` | `coverUrl` | singleLine |
  | `标签（逗号或换行分隔）` | `tagsText` | `minLines = 2` |
- Language selector: label `语言` (SemiBold) then `Row(spacedBy 8.dp)` of `FilterChip`s over `listOf("zh" to "中文", "ja" to "日本語", "other" to "其他")`, `enabled = !processing` (`:287-292`).
- Adult switch row: `19禁内容` (SemiBold) + helper `书籍包含成人内容时必须开启`, bound to `draft.isAdult`.

### 2.11 Submission-type card (`UploadSubmissionCard`, `:308-335`)
Title `提交方式` (`titleLarge` Bold). Three selectable `Surface`s (clickable, radius 15.dp, `primaryContainer` when selected else `surfaceVariant α 0.4`), each containing a `FilterChip` (title) + subtitle text:
| value | chip title | subtitle |
|---|---|---|
| `chinese` | `中文书籍` | `直接提交中文正文` |
| `personal` | `个人翻译` | `仅进入个人翻译工作流` |
| `shared` | `共享翻译` | `进入共享翻译工作流` |
Both the `Surface.onClick` and the chip `onClick` set `submitType`; the surface click is gated on `!processing`, the chip on `enabled = !processing`. This card is shown in **both** normal and append mode.

### 2.12 Chapter preview (`UploadChapterSection`, `:337-368`)
- Header row: `章节预览` (`titleLarge` Bold) + `AssistChip("$count 章")` when count > 0.
- `Idle` → `选择 EPUB 后在此核对章节顺序。` (`onSurfaceVariant`)
- `Loading` → `LinearProgressIndicator`
- `Error` → `chaptersState.message` in `colorScheme.error`
- `Success` → first **16** chapters only, `Divider()` between rows; each row: `chapter.chapterNumber` in `primary` Bold, then `chapter.title` (`maxLines = 2`, ellipsis, Medium) and `"${chapter.content.length} 字符"`. If `size > 16` a trailing note: `仅预览前 16 章；提交时会上传全部 ${size} 章。`

### 2.13 Submit result (`UploadSubmitResult`, `:370-387`)
- `Idle` → nothing; `Loading` → `LinearProgressIndicator`; `Error` → `UploadNotice(message, true)`.
- `Success` → `Surface(Color(0xFFDCFCE7), radius 18.dp)` with `Icons.Filled.CheckCircle` tint `Color(0xFF15803D)`, text `上传成功` in `Color(0xFF166534)` Bold, `书籍 ID：$novelId` when present, and a `TextButton 查看` → `onOpenBook(novelId)`.
  Note: the success banner reads `上传成功` even in append mode.

### 2.14 Submit button + disclaimer (`:165-182`)
- `Button(onSubmit, enabled = !state.processing && hasAuthToken && chapters.isNotEmpty(), fillMaxWidth, height 54.dp)` with `Icons.Filled.UploadFile` and label:
  - `处理中…` when `state.processing`
  - `确认追加 ${chapters.size} 章` in append mode
  - `确认上传 ${chapters.size} 章` otherwise
- Disclaimer (`bodySmall`, `onSurfaceVariant`):
  - append: `追加会写入现有书籍。提交前请确认章节顺序与翻译类型。`
  - normal: `上传会写入 novalpie.cc。提交前请确认书名、作者、标签、成人内容标记与翻译类型。`

### 2.15 Validation
`validateUploadBookDraft(draft)` (`ui/UploadPresentation.kt:24-30`) — order matters:
1. blank `title` → `请输入书名`
2. blank `author` → `请输入作者`
3. `chapterCount <= 0` → `请先选择并解析 EPUB 文件`
4. `submitType !in {chinese, personal, shared}` → `提交方式无效`

`normalizeUploadTags(raw)` (`:32-36`): split on `,`, `，`, `\n`; trim; drop empties; `distinct()`.

**Append-mode validation is different** (`ui/NovalPieViewModel.kt:1749-1757`): only `chapters.isEmpty() → 请先选择并解析 EPUB 文件` and `submitType` check; title/author are not validated because they are not editable.

### 2.16 Behaviour (ViewModel)
- `openUploadBook()` (`:1665-1668`): if the current upload state is an append state, resets to a fresh `UploadBookState()`, then pushes `AppRoute.UploadBook`. No login gate.
- `openBookAppend(bookId)` (`:3659-3668`): requires `bookId > 0`; **login-gated** — `authToken.isNullOrBlank()` → `openLoginFallback()`; bumps `uploadRequestSerial`; sets `UploadBookState(existingNovelId = bookId)`; pushes `AppRoute.BookAppend(bookId)`.
- `updateUploadBookDraft(draft)` (`:1670-1676`): copies draft but force-overwrites `chapterCount` from the parsed chapter list, and clears `actionMessage` + `submitResult`.
- `selectUploadEpub(rawUri)` (`:1678-1742`):
  1. no-op if `processing`; bumps `uploadRequestSerial`; sets `processing = true`, `progressLabel = "正在读取 EPUB 文件…"`, `chapters = Loading`, clears `serverFilePath`.
  2. `readUploadDocument(rawUri)` (`:2424-2449`): takes a persistable read URI permission (best-effort), queries `OpenableColumns.DISPLAY_NAME`/`SIZE`, falls back to `openAssetFileDescriptor(...).length`, records `resolver.getType(uri)`.
  3. Rejects non-`.epub` names → `IOException("仅支持 EPUB 格式文件")`; rejects `sizeBytes == 0L` → `IOException("EPUB 文件为空")`.
  4. Sets `progressLabel` to `文件超过 50 MiB，正在按 5 MiB 流式分片上传…` (SERVER_CHUNKED) or `正在本机解析 EPUB 目录与章节…` (LOCAL).
  5. SERVER_CHUNKED: `api.uploadFileInChunks(source)` → server path, then `api.parseUploadedEpub(path).copy(epubFilePath = path)`. LOCAL: `EpubParser.parse(source)` on `Dispatchers.IO`.
  6. On success, **only blank draft fields are filled** from the parsed EPUB (`title`, `author`, `description` use `ifBlank`); `language` is overwritten from the EPUB when non-blank; `chapterCount` set; `chapters = Success`; `serverFilePath = parsed.epubFilePath`; message `EPUB 解析完成，共 ${n} 章`.
  7. On failure: `chapters = Error(...)` and `actionMessage` both via `apiFailureMessage("解析 EPUB", failure)`.
- `submitUploadBook()` (`:1744-1825`): validates per mode; requires `selectedFile` else `请先选择 EPUB 文件`; sets `progressLabel = "正在安全上传书籍与 ${n} 章内容…"`, `submitResult = Loading`.
  - Append: `api.appendManagedChapters(bookId, submitType, chapters, epubFilePath = serverFilePath, epubFile = if (serverFilePath == null) uploadSource(document) else null)`.
  - New: `api.uploadBook(upload = UploadBookRequest(...), epubFile = if (epubFilePath == null) uploadSource(document) else null)`.
  - `UploadBookRequest` carries `spans = draft.spans` (always `"balanced"`), `tags = normalizeUploadTags(tagsText)`, `coverUrl = draft.coverUrl.takeIf { it.isNotBlank() }`.
  - `if (!uploaded.success) error(uploaded.message ?: "上传失败")`.
  - Success message `uploaded.message ?: (append ? "章节追加成功" : "上传成功")`. Failure label `上传书籍`.
- `clearUploadBook()` (`:1827-1830`): bumps serial and resets to `UploadBookState(existingNovelId = (currentRoute as? AppRoute.BookAppend)?.bookId)` — i.e. staying in append mode when on the append route.
- `openUploadedBook(novelId)` (`:1832-1834`): `openBook(novelId)` when > 0.
- `uploadSource(document, fallbackContentType = "application/epub+zip")` (`:2451-2470`): builds `UploadFileSource`; for `file://` URIs uses `FileInputStream`, otherwise `contentResolver.openInputStream` (error `无法读取所选文件`, or `本地文件路径无效`).

### 2.17 Upload API surface
- `api.uploadFileInChunks(file, fileId = UUID.randomUUID(), chunkSizeBytes = WEBSITE_UPLOAD_CHUNK_BYTES)` (`data/NovalPieApi.kt:1501-1542`): `POST /api/uploads/chunks` multipart per chunk with parts `file`, `file_id`, `chunk_index`, `total_chunks`, `file_name`, `file_size`; then a JSON `POST /api/uploads/chunks` with `action = "merge"` returning `file_path`. Errors: `文件为空`, `分片大小必须大于 0`, `上传分片失败`, `合并文件失败`, `服务器未返回文件路径`.
- `api.parseUploadedEpub(filePath)` (`:1544-1551`): `POST /api/uploads/epubs` with `{file_path, parse_only: true}`.
- `api.uploadBook(upload, epubFile, coverFile)` (`:1473-1499`): multipart `POST /api/uploads/books` with parts `title, title_translation, author_name, description, language, spans, is_adult (1/0), source, source_url, tags (comma-joined), submit_type, chapters (JSON), chapters_md5`, optional `epub_file_path`, `cover_url`, `epub_file`, `cover`.
- `api.appendManagedChapters(bookId, submitType, chapters, epubFilePath, epubFile)` (`:1406-1441`): `POST /api/users/me/chapters/append`. **Auto-chunks** when `chapters.size > 50` OR total title+content length `> 2_500_000`, in batches of 50, adding `chunk_index`, `total_chunks`, `is_chunked = "1"`. Parts: `existing_novel_id`, `submit_type`, `chapters`, `chapters_md5`, optional `epub_file_path`/`epub_file`. Throws on the first `!result.success`.
- Constants `data/NovalPieApi.kt:3335-3336`: `WEBSITE_UPLOAD_CHUNK_BYTES = 5 * 1024 * 1024`, `WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES = 20 * 1024 * 1024`.

---

## 3. Upload / EPUB editor (EPUB 编辑器)

The largest authoring surface. 5 tabs, 7 chapter-split modes, find/replace with regex, AI regex generation, a sandboxed JavaScript scripting engine with optional chunking, a chapter CRUD dialog, a local archive store, EPUB export, and a hand-off into the upload screen.

### 3.1 Signature
`ui/UploadEditorScreens.kt:72-105`
```kotlin
@Composable
fun UploadEditorScreen(
    state: UploadEditorState,
    onTabSelected: (EditorTab) -> Unit,
    onOpenDocument: (String) -> Unit,
    onEncodingChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onMetadataChange: (EditorBookMetadata) -> Unit,
    onSplitModeChange: (EditorSplitMode) -> Unit,
    onSplitPatternChange: (String) -> Unit,
    onSplitTargetChange: (String) -> Unit,
    onCustomScriptChange: (String) -> Unit,
    onScriptChunkedChange: (Boolean) -> Unit,
    onScriptChunkSizeChange: (String) -> Unit,
    onCustomScriptResult: (Long, String?, String?) -> Unit,
    onAiConfigSelected: (Long) -> Unit,
    onGenerateAiRegex: () -> Unit,
    onProcessSplit: () -> Unit,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onFindRegexChange: (Boolean) -> Unit,
    onReplaceAll: () -> Unit,
    onUpdateChapter: (Int, String, String) -> Unit,
    onAddChapter: () -> Unit,
    onDeleteChapter: (Int) -> Unit,
    onArchiveNameChange: (String) -> Unit,
    onSaveArchive: () -> Unit,
    onLoadArchive: (String) -> Unit,
    onDeleteArchive: (String) -> Unit,
    onClearArchives: () -> Unit,
    onExportEpub: (String) -> Unit,
    onSendToUpload: () -> Unit,
    onClear: () -> Unit
)
```
Wiring: `ui/NovalPieApp.kt:385-417`.

### 3.2 State
`ui/NovalPieViewModel.kt:290-313`
```kotlin
data class UploadEditorState(
    selectedTab: EditorTab = EditorTab.Text,
    text: String = "",
    fileName: String? = null,
    encoding: String = "UTF-8",
    metadata: EditorBookMetadata = EditorBookMetadata(),
    chapters: List<UploadChapter> = emptyList(),
    splitMode: EditorSplitMode = EditorSplitMode.Regex,
    splitPattern: String = DEFAULT_EDITOR_CHAPTER_REGEX,
    splitTarget: String = "3000",
    customScript: String = DEFAULT_EDITOR_CUSTOM_SCRIPT,
    scriptChunked: Boolean = false,
    scriptChunkSize: String = "200000",
    scriptRunId: Long = 0,
    aiConfigs: List<WorkspaceLocalApiConfig> = emptyList(),
    selectedAiConfigId: Long? = null,
    findText: String = "",
    replaceText: String = "",
    findUsesRegex: Boolean = false,
    archiveName: String = "",
    archives: List<EditorArchive> = emptyList(),
    busy: Boolean = false,
    actionMessage: String? = null
)
```
`ui/EditorPresentation.kt:21-27`
```kotlin
val DEFAULT_EDITOR_CHAPTER_REGEX = "^第[\\d零一二三四五六七八九十百千万]+章.*$"
val DEFAULT_EDITOR_CUSTOM_SCRIPT = """
    function processText(text, options) {
      return text;
    }
""".trimIndent()
```
`EditorBookMetadata` (`model/Models.kt:498-507`): `title, author, description, language = "zh", tags (comma-separated String), isAdult, source, sourceUrl`.

### 3.3 Tabs
`ui/EditorPresentation.kt:3-9` — `enum class EditorTab(val label: String)`:
| enum | label |
|---|---|
| `Text` | 文本 |
| `Split` | 分章 |
| `Chapters` | 目录 |
| `Metadata` | 书籍 |
| `Archives` | 存档 |

### 3.4 Screen shell (`:135-208`)
- Root `Column(fillMaxSize)`.
- `EditorHero(...)` at top (`:136-145`).
- Tab rail `LazyRow(contentPadding h16/v10, spacedBy 8.dp)` of `FilterChip`s over `EditorTab.values()`.
- `state.actionMessage?.let { Surface(secondaryContainer, radius 12.dp, padding h16/v2) { Text(..., bodySmall, padding 10.dp) } }`.
- `if (state.busy) LinearProgressIndicator(fillMaxWidth, padding h16/v8)`.
- `when (state.selectedTab)` renders the tab body.
- Three dialogs hoisted at the bottom of the composable:
  - `editingChapter: Int?` → `EditorChapterDialog(index, chapter, onDismiss, onSave, onDelete)`.
  - `deletingArchive: EditorArchive?` → `EditorConfirmDialog(title = "删除存档", message = "确定删除“${archive.name}”吗？此操作不可撤销。")` (note the full-width CJK quotes `“”`).
  - `confirmClearArchives: Boolean` → `EditorConfirmDialog(title = "清空所有存档", message = "这会删除 App 私有目录中的全部编辑器存档。")`.
- `EditorConfirmDialog` (`:578-587`): confirm `确认`, dismiss `取消`.

**Document pickers** (`:106-111`):
- Open: `ActivityResultContracts.OpenDocument()`, launched with `arrayOf("text/*", "application/epub+zip", "application/octet-stream", "*/*")` (`:138`).
- Export: `ActivityResultContracts.CreateDocument("application/epub+zip")`, launched with a suggested filename computed as
  `state.metadata.title.trim().ifBlank { "novalpie-book" }.replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".epub"` (`:140-141`).

### 3.5 Custom-script execution bridge (`:112-133`)
```kotlin
val scriptEngine = remember(context) { EditorScriptEngine(context) }
LaunchedEffect(state.scriptRunId) {
    if (state.scriptRunId <= 0L || !state.busy || state.splitMode != EditorSplitMode.CustomScript) return@LaunchedEffect
    val runId = state.scriptRunId
    val result = runCatching {
        scriptEngine.process(
            script = state.customScript,
            text = state.text,
            chunked = state.scriptChunked,
            targetChunkSize = state.scriptChunkSize.toIntOrNull()?.coerceIn(1_024, 1_000_000) ?: 200_000
        )
    }
    result.fold(
        onSuccess = { processed -> onCustomScriptResult(runId, processed, null) },
        onFailure = { failure -> onCustomScriptResult(runId, null, failure.message ?: "脚本执行失败") }
    )
}
```
The ViewModel raises `scriptRunId` to trigger this effect; the composable owns the WebView-based engine. Chunk size is clamped to `1024..1_000_000` with fallback `200_000`. Failure fallback string: `脚本执行失败`.

### 3.6 Hero (`EditorHero`, `:210-254`)
- `Box(fillMaxWidth, Brush.horizontalGradient(listOf(Color(0xFF111827), Color(0xFF3730A3), Color(0xFF7C3AED))), padding h18/v16)`.
- Title `EPUB 编辑器` (`titleLarge`, ExtraBold, white).
- Subtitle (white α 0.78, `bodySmall`, `maxLines = 1`, ellipsis):
  `"${state.fileName ?: "未打开文件"} · ${state.chapters.size} 章 · ${formatEditorCount(state.text.length)} 字符"`
- `IconButton(onClear, enabled = !state.busy)` with `Icons.Filled.Delete`, `contentDescription = "清空编辑器"`, tint white.
- Action rail `LazyRow(spacedBy 8.dp)` of `EditorHeroButton(label, icon, disabled, onClick)` (`:247-254`, `OutlinedButton` with white content, greyed to `onSurface α 0.38` when disabled):
  | label | icon | disabled when |
  |---|---|---|
  | `打开` | `Icons.Filled.FolderOpen` | `state.busy` |
  | `生成 EPUB` | `Icons.Filled.Download` | `state.busy || state.chapters.isEmpty()` |
  | `发送到上传` | `Icons.Filled.Send` | `state.busy || state.chapters.isEmpty()` |
- `formatEditorCount(value)` (`:596-600`): `>= 10_000` → `"%.1f万"`; `>= 1_000` → `"%.1fk"`; else raw.

### 3.7 Tab: 文本 (`EditorTextTab`, `:256-303`)
`LazyColumn(fillMaxSize, contentPadding 16.dp, spacedBy 14.dp)`.

**Encoding selection** (`:267-274`)
- Section title `打开编码` (`titleMedium` Bold).
- `LazyRow(spacedBy 7.dp, contentPadding v8)` of `FilterChip`s, `selected = state.encoding.equals(encoding, true)`. Exact option list and order:
  `UTF-8`, `UTF-16LE`, `UTF-16BE`, `GB18030`, `GBK`, `Big5`, `Shift_JIS`, `EUC-JP`, `EUC-KR`, `windows-1252`.
- **Semantics:** the encoding is only consumed when a *non-EPUB* file is opened (`readEditorText(document, uploadEditorState.encoding)`, `ui/NovalPieViewModel.kt:1977`). Changing the chip after loading does **not** re-decode the already-loaded text — the user must reopen the file. `updateEditorEncoding` only stores the value (`:1859-1861`).

**Find / replace card** (`:275-290`)
- `ElevatedCard(radius 18.dp)`; header `Icons.Filled.FindReplace` tinted `primary` + `查找 / 替换` (Bold).
- `OutlinedTextField(state.findText, label "查找", singleLine, fillMaxWidth)`.
- `OutlinedTextField(state.replaceText, label "替换为", singleLine, fillMaxWidth)`.
- Row: label `正则表达式` + `Switch(state.findUsesRegex)` on the left, `Button` `全部替换` on the right.

**Body editor** (`:291-301`)
- `OutlinedTextField(state.text, label "正文文本", textStyle = bodyMedium + FontFamily.Monospace, minLines = 18, maxLines = 32, fillMaxWidth)`.

**Replace behaviour** — `replaceEditorText()` (`ui/NovalPieViewModel.kt:2078-2095`):
- Empty `findText` → message `请输入查找内容` (no-op).
- `findUsesRegex` → `text.replace(Regex(findText), replaceText)`; else literal `text.replace(findText, replaceText)`.
- Result message: `替换完成` when the text changed, `未找到匹配项` when unchanged.
- Invalid regex → `替换失败：${failure.message ?: "正则无效"}`.
- Note: `replaceText` is used as a **regex replacement template** in regex mode, so `$1` back-references work (and stray `$` will throw).

### 3.8 Tab: 分章 (`EditorSplitTab`, `:305-442`)
`LazyColumn(fillMaxSize, contentPadding 16.dp, spacedBy 14.dp)`, two cards.

**Card 1 — 分章工具** (`:319-431`)
- Header: `Icons.Filled.AutoFixHigh` tinted `primary` + `分章工具` (`titleLarge` Bold).
- Blurb: `与源站编辑器一致，规则只在本机处理文本。` (`bodySmall`, `onSurfaceVariant`).
- Mode rail: `LazyRow(spacedBy 8.dp)` of `FilterChip`s over `EditorSplitMode.values()`.
- Mode-specific input (`when (state.splitMode)`):
  | mode | control | label |
  |---|---|---|
  | `Regex` | `OutlinedTextField(minLines = 5, Monospace)` bound to `splitPattern` | `每行一个正则` |
  | `KeywordNumber` | same field | `每行一个关键词` |
  | `CustomScript` | script sub-panel, see below | — |
  | `CharacterCount` | `OutlinedTextField(singleLine)` bound to `splitTarget` | `每章目标字符数` |
  | `ParagraphCount` | same field | `每章目标段落数` |
  | `MarkdownH1`/`MarkdownH2` (`else` branch) | static text | `将识别对应 Markdown 标题并按出现顺序生成目录。` |
- **AI regex sub-panel** — only rendered when `splitMode == Regex` (`:393-425`):
  - `Divider()`, then `AI 生成正则` (SemiBold).
  - If `state.aiConfigs.isEmpty()`: `工作区没有保存可用的本地 API 配置。API key 不会在这里显示。` (`bodySmall`, `onSurfaceVariant`).
  - Else: `LazyRow(spacedBy 8.dp)` of `FilterChip`s keyed `it.id`, label `"${config.name.ifBlank { "API" }} · ${config.model}"`, selection bound to `state.selectedAiConfigId`.
  - `OutlinedButton(fillMaxWidth)` labelled `根据前 20 个章节标题生成正则`, `enabled = !state.busy && state.chapters.size >= 2 && state.selectedAiConfigId != null`.
  - Footnote (`labelSmall`): `请求沿用源站 OpenAI-compatible /v1/chat/completions 协议；生成后仅填入正则框，不会自动执行分章。`
- Primary action: `Button(onProcess, enabled = state.text.isNotBlank() && !state.busy, fillMaxWidth, height 50.dp)` labelled `生成章节目录`.

**Card 2 — 源站章节标识符** (`:432-440`)
- `源站章节标识符` (Bold)
- `支持 `##__T[00001]__##` 标题标识与 `##__C[00001]__##` 内容标识；生成 EPUB 前目录会使用连续编号。` (backticks are literal characters in the Kotlin string, `bodySmall`)
- `当前文本 ${formatEditorCount(state.text.length)} 字符，已生成 ${state.chapters.size} 章。` in `primary`.

**Custom-script sub-panel** (`:342-382`)
- Blurb: `脚本必须定义 processText(text, options)，仅在本地无网络/文件权限沙箱中运行。`
- `OutlinedTextField(state.customScript, label "JavaScript", minLines = 12, maxLines = 24, Monospace, fillMaxWidth)`.
- Switch row: `分块处理` (SemiBold) + helper `大文本按换行拆分并依次执行`, bound to `state.scriptChunked`.
- When chunked: `OutlinedTextField(state.scriptChunkSize, label "每块目标字符数（1024 - 1000000）", singleLine, fillMaxWidth)`.
- Helper-function list (`labelSmall`, `onSurfaceVariant`):
  `辅助函数：insertMarker、findMatches、splitByParagraphs、splitByWords、getParagraphs、getWordCount、getLineCount。`

### 3.9 Split modes
`ui/EditorPresentation.kt:11-19` — `enum class EditorSplitMode(val label: String)`; chip order equals declaration order:
| # | enum | label | algorithm (see §9.3) |
|---|---|---|---|
| 1 | `Regex` | 正则表达式 | `EditorProcessor.splitByRegex(text, patterns)` — one pattern per line |
| 2 | `MarkdownH1` | Markdown 一级标题 | `splitByMarkdown(text, 1)` |
| 3 | `MarkdownH2` | Markdown 二级标题 | `splitByMarkdown(text, 2)` |
| 4 | `KeywordNumber` | 关键词 + 数字 | `splitByKeywordNumber(text, keywords)` — one keyword per line |
| 5 | `CharacterCount` | 按字数 | `splitByCharacterCount(text, splitTarget.toInt())` |
| 6 | `ParagraphCount` | 按段落数 | `splitByParagraphCount(text, splitTarget.toInt())` |
| 7 | `CustomScript` | 自定义脚本 | WebView JS sandbox + `parseWebsiteIdentifiers` |

**Pre-flight validation** — `editorSplitTargetError(mode, pattern, target, customScript, scriptChunked, scriptChunkSize)` (`ui/EditorPresentation.kt:29-48`):
| mode | rule → message |
|---|---|
| `Regex` | no non-blank line in `pattern` → `请输入至少一个正则表达式` |
| `KeywordNumber` | no non-blank line → `请输入至少一个关键词` |
| `CharacterCount`, `ParagraphCount` | `(target.toIntOrNull() ?: 0) <= 0` → `分块目标必须大于 0` |
| `CustomScript` | blank script → `请输入 processText JavaScript 脚本`; script not containing the substring `processText` → `脚本必须定义 processText(text, options)`; chunked and `scriptChunkSize` not in `1024..1_000_000` → `分块大小必须介于 1024 到 1000000 字符` |
| others | `null` |

**`processEditorSplit()`** (`ui/NovalPieViewModel.kt:2003-2052`):
1. blank text → `请先加载或输入文本`.
2. run `editorSplitTargetError`; on failure set the message and return.
3. If `CustomScript`: set `busy = true`, `scriptRunId += 1`, message `正在本地沙箱执行脚本…` and return (execution continues in the composable's `LaunchedEffect`).
4. Otherwise run the matching `EditorProcessor` function synchronously on the main thread.
5. Empty result → `没有匹配到章节标题，请调整规则`. Non-empty → `chapters = result`, `selectedTab = EditorTab.Chapters`, message `已生成 ${n} 章`.
6. Thrown exception → `分章失败：${failure.message ?: "规则无效"}`.

**`completeEditorCustomScript(runId, processedText, error)`** (`:2054-2076`):
- Ignores stale runs (`scriptRunId != runId`) or non-busy state.
- Error or `null` text → `busy = false`, message `脚本执行失败：${error ?: "未返回文本"}`.
- Else: `chapters = EditorProcessor.parseWebsiteIdentifiers(processedText)`; if that yields nothing the previous chapter list is preserved and the tab stays on `Text` with message `脚本处理完成；未发现网站章节标识，已保留处理后的文本`; if chapters were found the tab switches to `Chapters` with `脚本处理完成，已生成 ${n} 章`. **The processed text always replaces `state.text`.**

**AI regex generation** — `generateEditorRegexWithAi()` (`:1896-1936`):
- No-op when `busy`.
- `chapters.size < 2` → `请先生成至少两个章节标题`.
- No matching config in `state.aiConfigs` → `请先在工作区保存可用的本地 API 配置`.
- Sets `busy = true`, message `正在生成章节正则…`.
- Calls `api.generateEditorRegex(endpoint, apiKey, model, chapterTitles = state.chapters.take(20).map { it.title })`.
- Success: forces `splitMode = Regex`, sets `splitPattern = regex`, jumps to `selectedTab = EditorTab.Split`, message `AI 已生成正则，请检查后再执行分章`. **It never auto-runs the split.**
- Failure label `AI 生成正则`.
- Config list source: `openUploadEditor()` (`:1836-1849`) loads `workspaceLocalStore.loadApis()` filtered to `endpoint`, `model`, `apiKey` all non-blank; keeps the previous selection if still present, else selects the first.

`api.generateEditorRegex` (`data/NovalPieApi.kt:1196-1246`):
- Requires `http(s)` endpoint (`API endpoint must use HTTP or HTTPS`), non-blank key (`API key is required`), non-blank model (`Model is required`), ≥ 2 titles (`At least two chapter titles are required`); titles are filtered non-blank and capped at 20.
- System prompt (verbatim):
  `You are a regular-expression expert. Generate JavaScript-compatible regex patterns that match all supplied chapter titles.` / `Return one JSON object with a regex string field. Multiple patterns may be separated by newlines.`
- User prompt: `Generate regex patterns for these chapter titles:` then `1. <title>` … numbered lines.
- Payload: `{model, messages:[system,user], temperature: 0.3, response_format: {type: "json_object"}}`; `POST ${endpoint.trimEnd('/')}/v1/chat/completions` with `authorization: Bearer <key>`, `content-type: application/json`.
- Response extraction chain: `choices[0].message.content` → parse as JSON and read first of `regex`/`pattern`/`expression` → else strip a ```` ```json ```` fence, parse, read the same keys → else the raw trimmed content. Errors: `AI returned an invalid response`, `AI response did not contain message content`, `AI did not return a regex pattern`.
- Uses `executeExternal("editor AI regex", …)` — i.e. a **direct outbound call to the user's own LLM endpoint**, not through novalpie.cc.

### 3.10 Tab: 目录 (`EditorChaptersTab`, `:444-475`)
- Header row: `章节目录` (`titleLarge` Bold) + subtitle `共 ${chapters.size} 章，点击卡片编辑标题与正文`; right `Button(onAdd)` with `Icons.Filled.Add` + `新增`.
- Empty → `EditorEmpty("还没有章节。先在“分章”页生成目录，或手动新增章节。")` (`EditorEmpty` at `:589-594`: `Surface(surfaceVariant α 0.5, radius 16.dp, padding 18.dp)`).
- Rows via `itemsIndexed(chapters, key = { index, chapter -> "${chapter.chapterNumber}-$index-${chapter.title}" })`:
  - `ElevatedCard(onClick = { onEdit(index) }, radius 16.dp)`.
  - Number badge `Surface(primaryContainer, radius 12.dp)` with `chapter.chapterNumber` in `primary` Bold.
  - `chapter.title` (SemiBold, `maxLines = 2`, ellipsis) and `"${formatEditorCount(chapter.content.length)} 字符"`.
  - Trailing `Icons.Filled.Edit` with `contentDescription = "编辑章节"`, tint `primary`.

**Chapter dialog** (`EditorChapterDialog`, `:548-576`)
- Title `编辑第 ${index + 1} 章`.
- Body is a `LazyColumn(spacedBy 10.dp)` with:
  - `OutlinedTextField(title, label "章节标题", fillMaxWidth)`
  - `OutlinedTextField(content, label "章节正文", minLines = 12, maxLines = 22, Monospace, fillMaxWidth)`
  - `"${formatEditorCount(content.length)} 字符"` (`bodySmall`, `onSurfaceVariant`)
- Confirm `保存` → `onSave(title, content)`.
- Dismiss slot holds **two** buttons in a `Row`: `TextButton 删除` (text colored `colorScheme.error`) → `onDelete`, and `TextButton 取消` → dismiss. Deleting from here does not ask for extra confirmation.
- Local state keyed `remember(index, chapter.title)` / `remember(index, chapter.content)`.

**Chapter CRUD behaviour** (`ui/NovalPieViewModel.kt`)
- `updateEditorChapter(index, title, content)` (`:2097-2102`): bounds-checked; blank title falls back to `第 ${index + 1} 章`; content stored verbatim; message `章节已更新`. **Does not renumber.**
- `addEditorChapter()` (`:2104-2110`): appends `UploadChapter("第 ${n+1} 章", "", n+1)`; message `已添加章节`.
- `deleteEditorChapter(index)` (`:2112-2117`): removes and **renumbers all chapters** (`chapterNumber = i + 1`); message `章节已删除并重新编号`.

### 3.11 Tab: 书籍 (`EditorMetadataTab`, `:477-504`)
`LazyColumn(fillMaxSize, contentPadding 16.dp, spacedBy 12.dp)`:
- Title `书籍信息` (`titleLarge` Bold).
- `OutlinedTextField` items (all `fillMaxWidth`):
  | label | binding | shape |
  |---|---|---|
  | `书名 *` | `metadata.title` | singleLine |
  | `作者 *` | `metadata.author` | singleLine |
  | `简介` | `metadata.description` | `minLines = 4` |
  | `标签（逗号分隔）` | `metadata.tags` | `minLines = 2` |
  | `来源` | `metadata.source` | singleLine |
  | `来源链接` | `metadata.sourceUrl` | singleLine |
- Language block: `语言` (SemiBold) then `Row(spacedBy 8.dp, top padding 6.dp)` of `FilterChip`s over `listOf("zh" to "中文", "ja" to "日本語", "other" to "其他")`.
- Adult card: `ElevatedCard(radius 16.dp)` with `19禁内容` (Bold) + `生成与上传时保留成人标记` (`bodySmall`) and a `Switch` bound to `metadata.isAdult`.
- No inline validation here; validation happens on export / send (see §3.13).

### 3.12 Tab: 存档 (`EditorArchivesTab`, `:506-546`)
- Save card `ElevatedCard(radius 18.dp)`:
  - Header `Icons.Filled.Archive` tinted `primary` + `保存当前存档` (`titleMedium` Bold).
  - `OutlinedTextField(state.archiveName, label "存档名称（可选）", singleLine, fillMaxWidth)`.
  - `Button(onSave, enabled = !state.busy && (state.text.isNotBlank() || state.chapters.isNotEmpty()), fillMaxWidth)` labelled `保存存档`.
  - Footnote: `正文与索引分文件保存；列表不会把全部长文本重新载入内存。`
- List header row: `已保存的存档（${state.archives.size}）` (`titleMedium` Bold, `weight(1f)`) + `TextButton 清空` (only when the list is non-empty) → sets `confirmClearArchives = true`.
- Empty → `EditorEmpty("暂无存档")`.
- Per archive (`ElevatedCard(radius 16.dp)`, keyed `EditorArchive::id`):
  - `archive.name` (Bold, `maxLines = 2`, ellipsis).
  - `"${archive.chapterCount} 章 · ${formatEditorCount(archive.totalWords)} 字符 · ${archive.fileName ?: "本地编辑"}"` (`bodySmall`, `onSurfaceVariant`).
  - `Button(weight 1f)` labelled `加载` → `onLoad(archive.id)`; `OutlinedButton` with `Icons.Filled.Delete` + `删除` → opens the delete confirm dialog.

**Archive behaviour** (`ui/NovalPieViewModel.kt`)
- `updateEditorArchiveName(value)` (`:1950-1952`).
- `saveEditorArchive()` (`:2119-2154`):
  - Nothing to save (`text.isBlank() && chapters.isEmpty()`) → `没有可保存的编辑内容`. No-op when `busy`.
  - Sets `busy`, message `正在保存存档…`.
  - Builds `EditorArchive(id = "archive_${timestamp}_${(0..9999).random()}", name = archiveName.trim().ifBlank { metadata.title.ifBlank { "存档 $timestamp" } }, timestamp, textContent = state.text, metadata, fileName = state.fileName, chapterCount = state.chapters.size, totalWords = state.chapters.sumOf { it.content.length }.takeIf { it > 0 } ?: state.text.length)`.
  - Writes on `Dispatchers.IO`, then clears `archiveName`, refreshes `archives`, message `存档已保存`. Failure label `保存存档`.
- `loadEditorArchive(id)` (`:2156-2169`): missing → `存档不存在`. On success restores `text`, `metadata`, `fileName`, **clears `chapters` to `emptyList()`**, switches to `EditorTab.Text`, message `存档已加载，请重新生成章节目录`.
- `deleteEditorArchive(id)` (`:2171-2175`): message `存档已删除`; failure label `删除存档`.
- `clearEditorArchives()` (`:2177-2181`): message `所有存档已清空`; failure label `清空存档`.
- `clearUploadEditor()` (`:2258-2265`): bumps `editorRequestSerial`, resets to a fresh `UploadEditorState` but **preserves** `archives` (re-listed), `aiConfigs`, and `selectedAiConfigId`.

### 3.13 EPUB export and send-to-upload
Shared pre-validation `validateEditorOutput(state)` (`ui/NovalPieViewModel.kt:2402-2407`):
1. blank `metadata.title` → `请填写书名`
2. blank `metadata.author` → `请填写作者`
3. `chapters.isEmpty()` → `请先生成章节目录`

**`exportEditorEpub(rawUri)`** (`:2183-2204`): sets `busy`, message `正在生成 EPUB…`; opens the SAF output stream with mode `"w"` (missing stream → `IOException("无法写入目标文件")`); `EpubWriter.write(output, metadata, chapters)`; success message `EPUB 已生成`; failure label `生成 EPUB`.

**`sendEditorToUpload()`** (`:2206-2256`):
1. Same validation; message `正在生成上传文件…`.
2. Builds a cache file: `safeTitle = metadata.title.replace(Regex("[^A-Za-z0-9\\p{L}\\p{N}._-]"), "_").take(48).ifBlank { "novalpie" }`; file name `"${safeTitle}_${System.currentTimeMillis()}.epub"` inside `cacheDir`; written with `EpubWriter`.
3. Detects an in-stack append target: `routes.asReversed().filterIsInstance<AppRoute.BookAppend>().firstOrNull()?.bookId`.
4. Replaces `uploadBookState` with a new `UploadBookState(existingNovelId = appendBookId, draft = UploadBookDraft(title, author, description, language, isAdult, source, sourceUrl, tagsText = metadata.tags, chapterCount = chapters.size), selectedFile = UploadDocument(uri = file.toURI().toString(), displayName = file.name, sizeBytes = file.length(), mimeType = "application/epub+zip"), chapters = LoadResult.Success(state.chapters), actionMessage = "编辑器内容已准备好，请核对后确认上传")`.
   Note `submitType` resets to the default `"chinese"`, `coverUrl` is not carried over, and `language` is copied straight from the editor metadata (default `"zh"`).
5. Editor message becomes `已发送到上传页`.
6. Pops the editor route off the stack and pushes `AppRoute.BookAppend(id)` when appending, else `AppRoute.UploadBook`.
7. Failure label `生成上传文件`.

### 3.14 Opening documents
`selectEditorDocument(rawUri)` (`ui/NovalPieViewModel.kt:1954-2001`):
- No-op when `busy`; bumps `editorRequestSerial`; message `正在打开文件…`.
- `.epub` (case-insensitive): `EpubParser.parse(uploadSource(document))` on IO, then
  - `text = EditorProcessor.toWebsiteIdentifiers(parsed.chapters)` (i.e. the whole book is flattened into the `##__T[…]__##` / `##__C[…]__##` marker format),
  - `metadata = EditorBookMetadata(title, author, description, language)` from the EPUB,
  - `chapters = parsed.chapters`.
- Any other file: `text = readEditorText(document, state.encoding)`, metadata unchanged, `chapters = emptyList()`.
- Post-load: `selectedTab = if (chapters.isEmpty()) Text else Chapters`; message `文件已加载，请配置分章规则` or `EPUB 已加载，共 ${n} 章`. Failure label `打开编辑文件`.
- `readEditorText` (`:2409-2422`): `Charset.forName(encoding)`, error `不支持的编码：$encoding`; streams in 16 KiB char buffers and throws `文本超过 5000 万字符，请先分割文件` above 50,000,000 characters.

---

## 4. Political exam (政治考试)

### 4.1 Signature
`ui/PoliticalExamScreens.kt:42-56`
```kotlin
@Composable
fun PoliticalExamScreen(
    state: PoliticalExamState,
    hasAuthToken: Boolean,
    onStart: () -> Unit,
    onOpenLogin: () -> Unit,
    onSelectSingle: (Int, Int) -> Unit,
    onToggleMultiple: (Int, Int) -> Unit,
    onSelectTrueFalse: (Int, Boolean) -> Unit,
    onUpdateBlank: (Int, String) -> Unit,
    onTick: () -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
)
```
Wiring: `ui/NovalPieApp.kt:419-432` (`onBack = viewModel::goBack`).

### 4.2 State + phases
`ui/NovalPieViewModel.kt:315-324`
```kotlin
data class PoliticalExamState(
    phase: PoliticalExamPhase = Landing,
    session: LoadResult<PoliticalExamSession> = Idle,
    answers: PoliticalExamAnswers = PoliticalExamAnswers(),
    remainingTimeSeconds: Int = 1800,
    deadlineEpochMillis: Long? = null,
    result: LoadResult<PoliticalExamResult> = Idle,
    submitting: Boolean = false,
    actionMessage: String? = null
)
```
`ui/PoliticalExamPresentation.kt:6-10` — `enum class PoliticalExamPhase { Landing, Active, Result }`.

Models (`model/Models.kt:520-562`):
```kotlin
PoliticalExamQuestion(question: String, options: List<String> = emptyList())
PoliticalExamPaper(singleChoice, multipleChoice, trueFalse, fillBlank: List<PoliticalExamQuestion>) {
    val totalQuestions get() = sum of the four sizes
}
PoliticalExamSession(sessionId: String, remainingTimeSeconds: Int = 1800, paper: PoliticalExamPaper)
PoliticalExamAnswers(singleChoice: List<Int?>, multipleChoice: List<List<Int>>, trueFalse: List<Boolean?>, fillBlank: List<String>)
PoliticalExamDetail(correct: Boolean, question: String?, userAnswer: String?, correctAnswer: String?, explanation: String?)
PoliticalExamResult(score: Int, total: Int, passed: Boolean, details: Map<String, List<PoliticalExamDetail>>, token: String?)
```

### 4.3 Screen shell (`:57-114`)
Three local confirm flags: `confirmStart`, `confirmSubmit`, `confirmExit`.

- `BackHandler(enabled = state.phase == PoliticalExamPhase.Active) { confirmExit = true }` (`:61`) — an in-progress exam intercepts back and asks first. Otherwise the global back handler applies.
- Timer driver (`:63-68`):
  ```kotlin
  LaunchedEffect(state.phase, state.remainingTimeSeconds, state.submitting) {
      if (state.phase == Active && !state.submitting) {
          if (state.remainingTimeSeconds > 0) delay(1_000)
          onTick()
      }
  }
  ```
  Self-retriggering because `remainingTimeSeconds` is a key. At 0 there is no delay so `onTick()` fires immediately, which triggers auto-submit.
- Phase dispatch: `Landing → PoliticalExamLanding`, `Active → PoliticalExamActive`, `Result → PoliticalExamResultView`.

**Confirm dialogs** — all via `PoliticalExamConfirmDialog(title, message, onDismiss, onConfirm)` (`:409-418`), confirm `确认` / dismiss `取消`:
| flag | title | message |
|---|---|---|
| `confirmStart` | `开始考试` | `开始会创建源站考试会话并计入每日次数。考试限时 30 分钟，确定继续吗？` |
| `confirmSubmit` | `提交考试` | `已作答 $answered / ${session?.paper?.totalQuestions ?: 0} 题。提交后不能修改，确定提交吗？` |
| `confirmExit` | `离开考试` | `考试计时仍以服务器会话截止时间为准。离开后可以从工具页重新进入当前本地会话。` |

### 4.4 Landing phase (`PoliticalExamLanding`, `:116-156`)
`LazyColumn(fillMaxSize, contentPadding 16.dp, spacedBy 14.dp)`:
1. `PoliticalExamOverviewCard(overview)` (`:158-202`).
2. Rules card `ElevatedCard(radius 18.dp, padding 18.dp)`: title `考试规则` (`titleLarge` Bold) then `overview.rules.forEach { Text("• $rule") }`.
3. `state.actionMessage?.let { ExamMessage(it) }`.
4. `state.session` → `Loading`: `LinearProgressIndicator`; `Error`: `ExamMessage(message)`; else nothing.
5. Primary button `fillMaxWidth`:
   - logged in → `Button(onStart, enabled = state.session != LoadResult.Loading)`
   - not logged in → `Button(onOpenLogin)`
   - label is always `overview.primaryAction`.

**Overview copy** — `politicalExamOverview(hasAuthToken)` (`ui/PoliticalExamPresentation.kt:21-34`):
```
title        = "政治考试"
subtitle     = "通过后按源站规则解锁阅读权限，题目和顺序由服务器实时生成。"
statusLabel  = hasAuthToken ? "已登录" : "需要登录"
stats        = ["100 题", "30 分钟", "80 分通过", "每日次数受限"]
rules        = [
  "40 道单选题，每题 1 分",
  "10 道多选题，每题 2 分，必须全部选对",
  "25 道判断题，每题 1 分",
  "25 道填空题，每题 1 分",
  "开始与提交都会同步源站账号状态"
]
primaryAction = hasAuthToken ? "开始考试" : "登录后参加考试"
```

**Overview card** (`:158-202`): `ElevatedCard(radius 22.dp, padding 18.dp)`; Row with `title` (`headlineMedium` ExtraBold) + `subtitle` (`onSurfaceVariant`) on the left and a pill `Surface(RoundedCornerShape(999.dp), primaryContainer)` with `statusLabel` (`labelMedium` SemiBold) on the right; then a Row of 4 equal-weight `Surface(radius 12.dp, surfaceVariant α 0.7)` stat tiles rendering the raw strings.

### 4.5 Active phase (`PoliticalExamActive`, `:204-295`)
- If `state.session` is not `Success` → `ExamMessage("考试会话不可用")` and return.
- `answered = politicalExamAnsweredCount(state.answers)`; `total = session.paper.totalQuestions`.
- `LazyColumn(fillMaxSize, contentPadding start/top/end 14.dp, bottom 96.dp, spacedBy 12.dp)`.
1. Status card `ElevatedCard(radius 18.dp, padding 16.dp)`:
   - Row: `考试进行中` (`titleLarge` Bold) and the clock `formatPoliticalExamTime(remainingTimeSeconds)`, colored `colorScheme.error` when `remainingTimeSeconds <= 300` else `primary`, Bold.
   - `LinearProgressIndicator(progress = if (total == 0) 0f else answered / total)`.
   - `已作答 $answered / $total 题` (`bodySmall`).
2. `if (state.submitting) LinearProgressIndicator`.
3. `state.actionMessage?.let { ExamMessage(it) }`.
4. Four question sections, each rendered only when its list is non-empty, via `ExamSectionTitle(title, subtitle)` (`:394-400`, `titleLarge` Bold + `bodySmall`/`onSurfaceVariant`):
   | section | title | subtitle |
   |---|---|---|
   | single | `一、单选题` | `${size} 题 · 每题 1 分` |
   | multiple | `二、多选题` | `${size} 题 · 每题 2 分` |
   | true/false | `三、判断题` | `${size} 题 · 每题 1 分` |
   | fill blank | `四、填空题` | `${size} 题 · 每题 1 分` |
   Item keys: `"single-$index"`, `"multiple-$index"`, `"boolean-$index"`, `"blank-$index"`.
   **Numbering restarts at 1 in every section** (`number = index + 1`).
5. Footer `Button(onSubmit, enabled = !state.submitting, fillMaxWidth)` labelled `正在提交…` when submitting else `提交考试`.

**Question renderers**
- `ExamChoiceQuestion(number, question, selected: Set<Int>, multiple, onSelect)` (`:297-323`): `ElevatedCard(radius 16.dp, padding 14.dp)`; prompt `"$number. ${question.question}"` (SemiBold); each option a clickable `Surface(radius 12.dp, primaryContainer when selected else surfaceVariant α 0.5)` containing `Checkbox` (multiple) or `RadioButton` (single) plus label `"${('A'.code + index).toChar()}. $option"` — so options are lettered A, B, C, … Both the surface click and the control's callback invoke `onSelect(index)`.
  - Single choice passes `selected = state.answers.singleChoice.getOrNull(index)?.let(::setOf).orEmpty()`.
  - Multiple choice passes `state.answers.multipleChoice.getOrNull(index).orEmpty().toSet()`.
- `ExamTrueFalseQuestion(number, question, answer: Boolean?, onSelect)` (`:325-338`): prompt line, then a Row of two equal-weight buttons — `正确` and `错误`; the chosen one renders as a filled `Button`, the other as `OutlinedButton`. `null` = neither filled.
- `ExamFillBlankQuestion(number, question, answer, onChange)` (`:340-348`): prompt line + `OutlinedTextField(answer, label "答案", fillMaxWidth)`.

**Helpers** (`ui/PoliticalExamPresentation.kt`)
- `politicalExamAnsweredCount(answers)` (`:36-40`): single non-null + multiple non-empty + trueFalse non-null + fillBlank non-blank.
- `formatPoliticalExamTime(seconds)` (`:42-45`): clamps to ≥ 0 then `"%02d:%02d".format(s / 60, s % 60)` — **mm:ss with no hour field, so 30:00 down to 00:00.**
- `politicalExamCorrectSummary(result, key)` (`:47-50`): `"${details[key].count { it.correct }} / ${details[key].size}"`.

### 4.6 Result phase (`PoliticalExamResultView`, `:350-384`)
- `result == null` → `ExamMessage((state.result as? LoadResult.Error)?.message ?: "成绩不可用")`.
- Score card `ElevatedCard(radius 20.dp, padding 22.dp, centered)`:
  - Verdict `考试通过` / `考试未通过` (`headlineMedium` ExtraBold, `primary` / `error`).
  - `"${result.score} / ${result.total}"` (`displaySmall` Bold).
  - Sub-line `新的阅读权限已按源站结果同步` when passed, else `可按源站限制重新参加考试`.
- Breakdown card `ElevatedCard(radius 16.dp, padding 16.dp)`: `分题型结果` (`titleMedium` Bold) then four `ExamResultLine(label, result, key)` rows (`:386-392`, `SpaceBetween`, right value SemiBold):
  | label | details key |
  |---|---|
  | `单选题` | `single_choice` |
  | `多选题` | `multiple_choice` |
  | `判断题` | `true_false` |
  | `填空题` | `fill_blank` |
- Footer `Button(onReset, fillMaxWidth)` labelled `返回考试说明`.
- **Gap to preserve knowingly:** `PoliticalExamDetail` carries `question`, `userAnswer`, `correctAnswer`, `explanation`, but the UI only renders correct-count ratios. Per-question review is parsed but never displayed.

- `ExamMessage(message)` (`:402-407`): `Surface(secondaryContainer, radius 12.dp, padding 12.dp)`.

### 4.7 Behaviour (ViewModel)
- `openPoliticalExam()` (`:2267-2270`): `refreshPoliticalExamTimer()` then push `AppRoute.PoliticalExam`. **No login gate on navigation** — the landing screen handles it.
- `startPoliticalExam()` (`:2272-2310`):
  - No-op when session is `Loading` or `submitting`.
  - `authToken.isNullOrBlank()` → message `请先登录后再开始考试`.
  - Sets `PoliticalExamState(phase = Landing, session = Loading, actionMessage = "正在创建考试会话…")`.
  - `api.startPoliticalExam()` → on success builds a fresh state with `phase = Active`, answer arrays sized to the paper (`null` / `emptyList()` / `null` / `""`), `remainingTimeSeconds = session.remainingTimeSeconds`, `deadlineEpochMillis = now + remaining * 1000`.
  - On failure stays on `Landing` with `session = Error(...)` and the same message; failure label `开始考试`.
- Answer mutators (all bounds-checked, all clear `actionMessage`):
  - `selectPoliticalExamSingle(index, option)` (`:2312-2320`) — overwrite.
  - `togglePoliticalExamMultiple(index, option)` (`:2322-2332`) — set-toggle then `.sorted()`.
  - `selectPoliticalExamTrueFalse(index, answer)` (`:2334-2342`).
  - `updatePoliticalExamBlank(index, answer)` (`:2344-2352`) — raw string, no trimming.
- `tickPoliticalExamTimer()` (`:2354-2358`): only in `Active` and not submitting; recomputes remaining and auto-calls `submitPoliticalExam()` at `<= 0`.
- `refreshPoliticalExamTimer()` (`:2360-2366`): `remaining = ((deadline - now + 999) / 1000).coerceAtLeast(0)` — i.e. **ceiling** seconds; only writes state when the value changed. Returns immediately if `deadlineEpochMillis == null`.
- `submitPoliticalExam()` (`:2368-2396`): requires `Active`, not already submitting, and a `Success` session. Sets `submitting = true`, message `正在提交考试…`. Calls `api.submitPoliticalExam(session.sessionId, state.answers)`.
  - Success: `phase = Result`, `result = Success`, `deadlineEpochMillis = null`, message `考试通过` / `考试未通过`. If `examResult.token` is non-blank it is persisted via `authSessionStore.saveToken(token)`, assigned to `authToken`, and `loadHome()` is called — **a passed exam rotates the session token.**
  - Failure: `submitting = false`, `result = Error(...)`, message; failure label `提交考试`.
- `resetPoliticalExam()` (`:2398-2400`): full reset to `PoliticalExamState()` — back to `Landing`, timer back to 1800.

API: `api.startPoliticalExam()` → `POST /api/political-exams/sessions` with `{}` (`data/NovalPieApi.kt:1248-1250`).
`api.submitPoliticalExam(sessionId, answers)` → `POST /api/political-exams/sessions/submit` with `{session_id, answers: {single_choice: [Int|null], multiple_choice: [[Int]], true_false: [Bool|null], fill_blank: [String]}}` (`:1252-1275`).

---

## 5. Admin (管理后台) — all 6 sections

### 5.1 Signature
`ui/AdminScreens.kt:53-78`
```kotlin
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AdminScreen(
    state: AdminState,
    onRefresh: () -> Unit,
    onSectionSelected: (AdminSection) -> Unit,
    onReviewQueryChange: (AdminReviewQuery) -> Unit,
    onApplyReviewQuery: () -> Unit,
    onResetReviewQuery: () -> Unit,
    onOperationLogQueryChange: (AdminOperationLogQuery) -> Unit,
    onApplyOperationLogQuery: () -> Unit,
    onResetOperationLogQuery: () -> Unit,
    onToggleReviewSetting: (String) -> Unit,
    onReviewAction: (Long, String) -> Unit,
    onUpdateKeyStatus: (Long, String) -> Unit,
    onDeleteKey: (Long) -> Unit,
    onSaveCookie: (AdminCookieConfig, String?) -> Unit,
    onToggleCookie: (AdminCookieConfig) -> Unit,
    onDeleteCookie: (Long) -> Unit,
    onSaveRule: (AdminBaseUrlRule) -> Unit,
    onSetRuleAction: (AdminBaseUrlRule, String) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onSaveShopItem: (AdminShopItem) -> Unit,
    onToggleShopItem: (AdminShopItem) -> Unit,
    onDeleteShopItem: (Long) -> Unit
)
```
Wiring: `ui/NovalPieApp.kt:287-310`.

### 5.2 Sections + state
`ui/NovalPieViewModel.kt:177-218`
```kotlin
enum class AdminSection(val websitePath: String) {
    Overview("/admin"), Review("/admin/review"), Keys("/admin/key-management"),
    OperationLogs("/admin/operation-logs"), Scraper("/admin/scraper-management"), Shop("/admin/shop")
}
data class AdminState(section, reviewQuery, operationLogQuery,
    overview: LoadResult<AdminOverviewStats>, reviewSettings: LoadResult<AdminReviewSettings>,
    reviewRequests: LoadResult<List<AdminReviewRequest>>, keys: LoadResult<List<AdminKeyItem>>,
    operationLogs: LoadResult<AdminOperationLogPage>, cookieConfigs: LoadResult<List<AdminCookieConfig>>,
    baseUrlRules: LoadResult<List<AdminBaseUrlRule>>, schedulerLogs: LoadResult<AdminSchedulerLogs>,
    shopItems: LoadResult<List<AdminShopItem>>, actionLoading: Boolean, actionMessage: String?)
data class AdminReviewQuery(type = "", status = "", keyword = "")
data class AdminOperationLogQuery(page = 1, action = "", status = "", userId = "", novelId = "",
    keyword = "", startDate = "", endDate = "")
```
`adminSectionLabel(section)` (`ui/AdminScreens.kt:842-849`):
| enum | chip / heading label |
|---|---|
| `Overview` | 总览 |
| `Review` | 审核 |
| `Keys` | Key |
| `OperationLogs` | 操作日志 |
| `Scraper` | 抓取管理 |
| `Shop` | 商店 |

### 5.3 Shell (`:79-259`)
Hoisted dialog state: `confirmation: AdminConfirmation?`, `cookieEditor: AdminCookieConfig?`, `ruleEditor: AdminBaseUrlRule?`, `shopEditor: AdminShopItem?`.

`private data class AdminConfirmation(title: String, message: String, destructive: Boolean = false, action: () -> Unit)` (`:46-51`). Its `AlertDialog` (`:83-98`): confirm button is a filled `Button` labelled `确认删除` when `destructive` else `确认`; dismiss `TextButton 取消`.

Root `LazyColumn(fillMaxSize, contentPadding 16.dp, spacedBy 12.dp)`:
1. Header (`:135-142`): `管理后台` (`headlineSmall` Bold) + `仅管理员可访问 · 数据与操作来自实时网站接口` (`bodySmall`, `onSurfaceVariant`).
2. Section chip rail: `LazyRow(spacedBy 8.dp)` of `FilterChip`s over `AdminSection.values()`, label `adminSectionLabel(section)`, `onClick = onSectionSelected` (which pushes a new route).
3. Sub-header row: `adminSectionLabel(state.section)` (`titleLarge` Bold) + `OutlinedButton(onRefresh, enabled = !state.actionLoading)` labelled `刷新`.
4. `state.actionMessage?.let { Surface(secondaryContainer, radius 10.dp, padding 12.dp) { Text(message) } }`.
5. Section body.

`AdminStatusCard(message)` (`:795-800`): `ElevatedCard(fillMaxWidth)` with the message in `onSurfaceVariant`, padding 16.dp. Used for loading, error, and empty states alike.
`AdminMetricCard(label, value, modifier)` (`:777-785`): `ElevatedCard`, centered, value in `titleLarge` Bold, label in `labelSmall`.
`AdminSwitchRow(label, checked, onToggle)` (`:787-793`): `SpaceBetween` row with a `Switch`.
`AdminStringFilterRail(label, options: List<Pair<String,String>>, selected, onSelected)` (`:802-822`): small label in `labelMedium`/`onSurfaceVariant` above a `LazyRow(spacedBy 8.dp)` of `FilterChip`s keyed `option.first.ifBlank { "__all" }`, rendering `option.second`.

### 5.4 Section 1 — 总览 (`adminOverviewItems`, `:261-289`)
- `Idle`/`Loading` → `AdminStatusCard("正在加载管理总览")`; `Error` → `AdminStatusCard(message)`.
- `Success`:
  - Row of 3 `AdminMetricCard`s, each `weight(1f)`:
    | label | value |
    |---|---|
    | `待审核` | `stats.pendingReviewTotal` |
    | `作品` | `stats.activeNovelTotal` |
    | `用户` | `stats.registeredUserTotal` |
  - `ElevatedCard`: title `近期注册` (Bold) then one `SpaceBetween` row per `stats.recentUserDaily` entry — `day.date` on the left, `day.count` on the right in `primary`.
- Unused-but-parsed fields: `pendingReviewUpload`, `pendingReviewDelete` (`model/Models.kt:209-210`) are never rendered.
- API: `api.adminOverview(days = 5)` → `GET /api/admin/overview?days=5` (clamped `1..90`), fields `pending_review_total`, `pending_review_upload`, `pending_review_delete`, `novel_active_total`, `user_registered_total`, `recent_user_daily[].date|day` + `.count` (`data/NovalPieApi.kt:357-379`).

### 5.5 Section 2 — 审核 (`adminReviewItems`, `:291-343`)
**Auto-review settings card** (`:299-311`)
- `Idle`/`Loading` → `AdminStatusCard("正在加载审核设置")`; `Error` → `AdminStatusCard(message)`.
- `Success` → `ElevatedCard`: title `自动审核` (Bold) then two `AdminSwitchRow`s:
  | label | bound to | toggles kind |
  |---|---|---|
  | `自动通过上传` | `autoApproveUpload` | `"upload"` |
  | `自动通过删除` | `autoApproveDelete` | `"delete"` |
- Toggling raises a confirmation: title `修改审核设置`, message `确认切换${if (kind == "upload") "上传" else "删除"}请求的自动审核设置？` (`:181-186`).

**Filter card** (`AdminReviewFilterCard`, `:345-381`)
- Title `筛选审核请求` (Bold).
- `AdminStringFilterRail(label = "类型", options = adminReviewTypeOptions(), selected = query.type)`
  `adminReviewTypeOptions()` (`:824-825`): `"" to 全部`, `upload to 上传`, `delete to 删除`.
- `AdminStringFilterRail(label = "状态", options = adminReviewStatusOptions(), selected = query.status)`
  `adminReviewStatusOptions()` (`:827-828`): `"" to 全部`, `pending to 待审核`, `approved to 已通过`, `rejected to 已拒绝`.
- `OutlinedTextField(query.keyword, label "关键词", singleLine, fillMaxWidth)`.
- Row: `Button(onApply, enabled = !actionLoading)` labelled `应用筛选`; `TextButton(onReset, enabled = enabled && query != AdminReviewQuery())` labelled `清空`.

**Request list** (`:321-342`)
- `Idle`/`Loading` → `AdminStatusCard("正在加载审核请求")`; `Error` → `AdminStatusCard(message)`; `Success` empty → `AdminStatusCard("暂无审核请求")`.
- Each request `ElevatedCard(fillMaxWidth, padding 14.dp, spacedBy 6.dp)`, keyed `it.id`:
  - `request.title ?: "请求 #${request.id}"` (Bold)
  - `"${request.type} · ${request.status} · ${request.username ?: "未知用户"}"` — type/status shown as raw server strings
  - `request.reason?.let { Text(it, bodySmall) }`
  - Actions **only when `request.status == "pending"`**: `Button 通过` and `OutlinedButton 拒绝`, both `enabled = !state.actionLoading`.
- Confirmation on action (`:187-192`): title `通过审核` / `拒绝审核`; message `确认${通过|拒绝}请求 #${request.id}？`.
- Parsed but never rendered: `userId`, `novelId`, `createdAt` (`model/Models.kt:226-230`).
- API: `api.adminReviewSettings()` → `GET /api/admin/review-settings`; `api.adminUpdateReviewSettings(upload, delete)` → `POST /api/admin/review-settings` `{auto_approve_upload, auto_approve_delete}`; `api.adminReviewRequests(type, status, keyword)` → `GET /api/admin/review-requests?page=1&page_size=100&type=&status=&q=`; `api.adminReviewAction(id, action)` → `POST /api/admin/review-requests` `{id, action}` with `action in {approve, reject}` (`data/NovalPieApi.kt:381-590`).
- **Gap:** `api.adminApproveAllReviews(type, status, keyword)` (`data/NovalPieApi.kt:592-602`, `POST /api/admin/review-requests` with `{action: "approve_all", …}`) exists but has **no UI entry point** anywhere in the app.
- **Gap:** the request list is fetched with a hard `page_size = 100` and no pagination control.

### 5.6 Section 3 — Key (`adminKeyItems`, `:383-412`)
- `Idle`/`Loading` → `AdminStatusCard("正在加载 Key")`; `Error` → `AdminStatusCard(message)`; empty → `AdminStatusCard("暂无 Key")`.
- Each key `ElevatedCard(fillMaxWidth, padding 14.dp, spacedBy 6.dp)`, keyed `it.id`:
  - `key.name` (Bold)
  - `listOfNotNull(key.providerName, key.model, key.approvalStatus).joinToString(" · ")`
  - `key.baseUrl?.let { Text(it, maxLines = 1, ellipsis, bodySmall) }`
  - Horizontally scrollable action row (`Modifier.horizontalScroll(rememberScrollState())`, `spacedBy 6.dp`): three `OutlinedButton`s labelled with the **raw English status strings** `pending`, `approved`, `rejected`, each `enabled = !actionLoading && key.approvalStatus != status`; then `TextButton 删除`, `enabled = !actionLoading`.
- Confirmations: status → title `修改 Key 状态`, message `确认将 ${key.name} 设为 $status？` (`:196-199`); delete → title `删除 Key`, message `确认永久删除 ${key.name}？此操作不可撤销。`, `destructive = true` (`:201-207`).
- Parsed but never rendered: `createdAt`.
- API: `api.adminKeys()` → `GET /api/admin/key-management`; `api.adminUpdateKeyStatus(id, approvalStatus)` → `PUT /api/admin/key-management` `{id, approval_status}` (validated against `{pending, approved, rejected}`); `api.adminDeleteKey(id)` → `DELETE /api/admin/key-management?id=$id` (`data/NovalPieApi.kt:425-619`).

### 5.7 Section 4 — 操作日志 (`adminOperationLogItems`, `:414-450`)
**Filter card** (`AdminOperationLogFilterCard`, `:452-522`) — every change also resets `page = 1`:
- Title `筛选操作日志` (Bold).
- `AdminStringFilterRail(label = "操作", options = adminOperationActionOptions(actionTypes), selected = query.action)`.
  `adminOperationActionOptions(actionTypes)` (`:833-840`): `listOf("" to 全部)` plus each distinct non-blank server-supplied action type mapped to itself (raw string as both value and label). `actionTypes` comes from the currently loaded page (`state.operationLogs.success.actionTypes`), so the rail is empty-but-`全部` until the first load succeeds.
- `AdminStringFilterRail(label = "状态", options = adminOperationStatusOptions(), selected = query.status)`.
  `adminOperationStatusOptions()` (`:830-831`): `"" to 全部`, `success to 成功`, `failed to 失败`, `pending to 处理中`.
- Row of two fields: `用户 ID` (bound to `query.userId`, input filtered with `it.filter(Char::isDigit)`) and `作品 ID` (bound to `query.novelId`, same digit filter), each `weight(1f)`, singleLine.
- `OutlinedTextField(query.keyword, label "关键词", singleLine, fillMaxWidth)`.
- Row of two fields: `开始日期` and `结束日期` (free-text, no picker, no format validation), each `weight(1f)`, singleLine.
- Row: `Button 应用筛选` (`enabled = !actionLoading`) and `TextButton 清空` (`enabled = enabled && query != AdminOperationLogQuery()`).

**Log list** (`:430-449`)
- `Idle`/`Loading` → `AdminStatusCard("正在加载操作日志")`; `Error` → `AdminStatusCard(message)`.
- `Success`: a summary line `共 ${total} 条 · ${totalPages} 页` (`bodySmall`), then per-log `ElevatedCard(fillMaxWidth, padding 12.dp, spacedBy 4.dp)` keyed `it.id`:
  - Row `SpaceBetween`: `log.action` (Bold) and `log.status` in `primary` — both raw server strings.
  - `"用户 ${log.userId ?: "-"} · 作品 ${log.novelId ?: "-"}"` (`bodySmall`).
  - `log.message?.let { Text(it, bodySmall) }`.
  - `log.createdAt?.let { Text(it, labelSmall) }`.
- **Gap:** `totalPages` is displayed and `AdminOperationLogQuery.page` exists (and `updateAdminOperationLogQuery` clamps it to ≥ 1, `ui/NovalPieViewModel.kt:1024-1026`), but **there is no next/prev page control**; the page is always 1 in practice.
- API: `api.adminOperationLogs(page, action, status, userId, novelId, keyword, startDate, endDate)` → `GET /api/admin/operation-logs?page=&page_size=20&action=&status=&user_id=&novel_id=&keyword=&start_date=&end_date=` (`data/NovalPieApi.kt:440-484`).

### 5.8 Section 5 — 抓取管理 (`adminScraperItems`, `:524-597`)
Three stacked blocks.

**5.8a Cookie 配置**
- Header row: `Cookie 配置` (`titleMedium` Bold) + `OutlinedButton 新增` → opens the editor seeded with `AdminCookieConfig(0, "", isActive = true)` (`:217`).
- `Idle`/`Loading` → `AdminStatusCard("正在加载 Cookie 配置")`; `Error` → `AdminStatusCard(message)`.
- `Success` → one `ElevatedCard` per config, keyed `"cookie-${it.id}"`, a single `SpaceBetween` Row (padding 12.dp):
  - Left column (`weight 1f`): `config.configKey` (Bold), `config.proxyIp ?: "未配置代理"` (`bodySmall`).
  - `TextButton 编辑` → editor dialog.
  - `Switch(checked = config.isActive, onCheckedChange = { onToggleCookie(config) })`.
  - `TextButton 删除`.
- Confirmations: toggle → title `修改 Cookie 状态`, message `确认${if (isActive) "停用" else "启用"} ${configKey}？` (`:219-223`); delete → title `删除 Cookie 配置`, message `确认删除 ${configKey}？`, destructive (`:224-228`).
- **No empty-state card** for cookies (unlike keys/shop/review).

**`AdminCookieEditorDialog`** (`:634-682`)
- Title `编辑 Cookie 配置` when `initial.id > 0`, else `新增 Cookie 配置`.
- Fields (`Column(spacedBy 8.dp)`, all `fillMaxWidth`):
  | label | binding | notes |
  |---|---|---|
  | `配置键名` | `key` | singleLine; `enabled = initial.id <= 0` |
  | `说明` | `description` | singleLine |
  | `新 Cookie（留空不修改）` (edit) / `Cookie` (new) | `cookieRaw` | `minLines = 2, maxLines = 4`; starts empty even when editing |
  | `代理 IP/URL` | `proxy` | singleLine |
- `AdminSwitchRow("启用", active)`.
- Confirm `保存` `enabled = key.isNotBlank() && (initial.id > 0 || cookieRaw.contains("="))` — i.e. a **new** cookie must contain a `=` character. Dismiss `取消`.
- On save it emits `initial.copy(configKey = key.trim(), description = description.trim().ifBlank { null }, proxyIp = proxy.trim().ifBlank { null }, isActive = active)` plus `cookieRaw.trim().ifBlank { null }`.

**5.8b BaseURL 规则**
- Header row: `BaseURL 规则` (`titleMedium` Bold) + `OutlinedButton 新增` → editor seeded with `AdminBaseUrlRule(0, "", "manual")` (`:229`).
- `Idle`/`Loading` → `AdminStatusCard("正在加载 BaseURL 规则")`; `Error` → `AdminStatusCard(message)`.
- `Success` → one `ElevatedCard` per rule, keyed `"rule-${it.id}"`:
  - `rule.pattern` (Bold, `maxLines = 2`, ellipsis).
  - Horizontally scrollable action row: `TextButton 编辑`; three `OutlinedButton`s with the **raw** labels `allow`, `block`, `manual`, each `enabled = rule.action != action`; `TextButton 删除`.
  - `rule.description` is **parsed but never rendered in the list** (only editable in the dialog).
- Confirmations: action → title `修改 BaseURL 规则`, message `确认将 ${rule.pattern} 设为 $action？` (`:231-235`); delete → title `删除 BaseURL 规则`, message `确认删除 ${rule.pattern}？`, destructive (`:236-240`).

**`AdminRuleEditorDialog`** (`:684-717`)
- Title `编辑 BaseURL 规则` / `新增 BaseURL 规则`.
- `OutlinedTextField(pattern, label "匹配规则", singleLine, enabled = initial.id <= 0)` — pattern immutable once created.
- Horizontally scrollable Row of three `OutlinedButton`s `allow` / `block` / `manual` setting the local `action`, each `enabled = action != value`.
- `OutlinedTextField(description, label "说明", minLines = 2)`.
- Confirm `保存` `enabled = pattern.isNotBlank()`; emits `initial.copy(pattern = pattern.trim(), action = action, description = description.trim().ifBlank { null })`. Dismiss `取消`.

**5.8c 调度日志** (`:582-596`)
- Section title `调度日志` (`titleMedium` Bold).
- `Idle`/`Loading` → `AdminStatusCard("正在加载调度日志")`; `Error` → `AdminStatusCard(message)`.
- `Success` → `ElevatedCard(fillMaxWidth, padding 12.dp, spacedBy 3.dp)`:
  - `"${totalLines} 行 · ${fileSizeMb ?: 0.0} MB"` (`labelSmall`).
  - `value.logs.takeLast(100).forEach { Text(line, labelSmall, FontFamily.Monospace) }` — last 100 lines only, monospace, no horizontal scroll (lines wrap).
  - `AdminSchedulerLogs.lastModified` is parsed but never rendered.
- API: `api.adminSchedulerLogs(lines = 100)` → `GET /api/admin/scheduler-logs?lines=100` (clamped `10..1000`) (`data/NovalPieApi.kt:512-530`).

Other APIs: `api.adminCookieConfigs()` → `GET /api/admin/cookie-config`; `api.adminSaveCookieConfig(config, cookieRaw)` → `PUT` when `id > 0` (body `{description, is_active, id}` + optional `cookie_raw`, `proxy_ip`) else `POST` (adds `config_key`), both `/api/admin/cookie-config`; `api.adminDeleteCookieConfig(id)` → `DELETE /api/admin/cookie-config` with body `{id}`; `api.adminBaseUrlRules()` → `GET /api/admin/baseurl-rules`; `api.adminSaveBaseUrlRule(rule)` → `PUT` (`{action, description, id}`) / `POST` (`{action, description, pattern}`) on `/api/admin/baseurl-rules`; `api.adminDeleteBaseUrlRule(id)` → `DELETE /api/admin/baseurl-rules?id=$id` (`data/NovalPieApi.kt:486-662`).
Note: `adminSaveBaseUrlRule` does **not** send `pattern` on update — consistent with the dialog disabling that field.

### 5.9 Section 6 — 商店 (`adminShopItems`, `:599-632`)
- Top row `Arrangement.End`: `OutlinedButton 新增商品` → editor seeded with `AdminShopItem(0, "", price = 0, type = "frame", isActive = true)` (`:244`).
- `Idle`/`Loading` → `AdminStatusCard("正在加载商品")`; `Error` → `AdminStatusCard(message)`; empty → `AdminStatusCard("暂无商品")`.
- Each item `ElevatedCard(fillMaxWidth)`, keyed `it.id`, one Row (padding 14.dp, `spacedBy 10.dp`):
  - Left column (`weight 1f`, `spacedBy 3.dp`): `item.name` (Bold); `"${item.type} · ${item.price} 积分"` (raw type string `frame`/`badge`); `item.description?.let { Text(it, bodySmall, maxLines = 2) }`.
  - `TextButton 编辑`; `Switch(checked = item.isActive, onCheckedChange = { onToggle(item) })`; `TextButton 删除`.
  - `imageUrl` / `badgeHtml` / `badgeCss` are **not previewed** in the list.
- Confirmations: toggle → title `修改商品状态`, message `确认${if (isActive) "下架" else "上架"} ${item.name}？` (`:246-250`); delete → title `删除商品`, message `确认删除 ${item.name}？`, destructive (`:251-255`).

**`AdminShopEditorDialog`** (`:719-775`)
- Title `编辑商品` / `新增商品`.
- Fields (`Column(spacedBy 7.dp)`, all `fillMaxWidth`):
  | label | binding | notes |
  |---|---|---|
  | `名称` | `name` | singleLine |
  | `说明` | `description` | `minLines = 2` |
  | `积分价格` | `price` | singleLine; input filtered `it.filter(Char::isDigit)` |
- Type selector: Row of two `OutlinedButton`s — `头像框` (sets `type = "frame"`, `enabled = type != "frame"`) and `徽章` (sets `type = "badge"`).
- Conditional fields:
  - `type == "frame"` → `OutlinedTextField(imageUrl, label "图片 URL", singleLine)`
  - else → `OutlinedTextField(badgeHtml, label "徽章 HTML", minLines = 2)` and `OutlinedTextField(badgeCss, label "徽章 CSS", minLines = 2)`
- `AdminSwitchRow("上架", active)`.
- Confirm `保存` `enabled = name.isNotBlank() && (price.toLongOrNull() ?: -1) >= 0`; emits `initial.copy(name.trim(), description.trim().ifBlank{null}, price = price.toLongOrNull() ?: 0, type, imageUrl.trim().ifBlank{null}, badgeHtml.trim().ifBlank{null}, badgeCss.trim().ifBlank{null}, isActive)`. Dismiss `取消`.
- API: `api.adminShopItems(type = "", active = null, keyword = "")` → `GET /api/admin/shop/items?type=&is_active=&keyword=&page=1&page_size=100`. **The API supports filters but the UI never passes any.** `api.adminSaveShopItem(item)` → `PUT`/`POST /api/admin/shop/items` with `{name, description, price, type, is_active (1/0)}` plus `image_url` for frames or `image_url: "", badge_html, badge_css` for badges. `api.adminDeleteShopItem(id)` → `DELETE /api/admin/shop/items?id=$id` (`data/NovalPieApi.kt:532-690`).

### 5.10 Admin behaviour (ViewModel)
- `openAdminSection(section)` (`:1001-1005`): **hard gate** `if (!isAdminProfile(currentUserProfile())) return` — silently does nothing for non-admins. Pushes `AppRoute.Admin(section)` then loads.
- `loadAdminSection(section = adminState.section)` (`:1007-1009`) → `loadAdminSectionInternal(section, null)`.
- `loadAdminSectionInternal(section, message)` (`:1037-1151`): re-checks admin; bumps `adminRequestSerial`; sets only that section's `LoadResult`s to `Loading` and `actionLoading = false`; then per section:
  | section | calls |
  |---|---|
  | Overview | `api.adminOverview()` → label `管理总览` |
  | Review | parallel `api.adminReviewSettings()` (label `审核设置`) + `api.adminReviewRequests(type, status, keyword)` (label `审核请求`) |
  | Keys | `api.adminKeys()` → label `Key 管理` |
  | OperationLogs | `api.adminOperationLogs(page, action, status, userId, novelId, keyword, startDate, endDate)` → label `操作日志` (all query fields `.trim()`ed) |
  | Scraper | parallel `api.adminCookieConfigs()` (`Cookie 配置`) + `api.adminBaseUrlRules()` (`BaseURL 规则`) + `api.adminSchedulerLogs()` (`调度日志`) |
  | Shop | `api.adminShopItems()` → label `商店商品` |
  All results are gated by `isFreshRequestSerial`.
- Query plumbing: `updateAdminReviewQuery` (`:1011`), `applyAdminReviewQuery` → reload Review (`:1015`), `resetAdminReviewQuery` → reset to defaults + reload (`:1019`); `updateAdminOperationLogQuery` (clamps `page ≥ 1`), `applyAdminOperationLogQuery`, `resetAdminOperationLogQuery` (`:1024-1035`).
- `runAdminMutation(label, successMessage, block)` (`:1228-1251`): re-checks admin, guards `actionLoading`; sets `actionMessage = "$label…"`; on success reloads the current section passing `action.message ?: successMessage` as the banner; on failure sets `apiFailureMessage(label, failure)`. All mutations return `UserCheckinAction`.
  | action | label | success message |
  |---|---|---|
  | `toggleAdminReviewSetting(kind)` (`:1153`) | `更新审核设置` | `审核设置已更新` |
  | `adminReviewAction(id, action)` (`:1162`) | `处理审核请求` | `审核已通过` / `审核已拒绝` |
  | `updateAdminKeyStatus(id, status)` (`:1168`) | `更新 Key 状态` | `Key 状态已更新` |
  | `deleteAdminKey(id)` (`:1174`) | `删除 Key` | `Key 已删除` |
  | `toggleAdminCookieConfig(config)` (`:1178`) | `更新 Cookie 配置` | `Cookie 配置状态已更新` (calls save with `isActive` flipped and `cookieRaw = null`) |
  | `saveAdminCookieConfig(config, raw)` (`:1184`) | `保存 Cookie 配置` | `Cookie 配置已保存` |
  | `deleteAdminCookieConfig(id)` (`:1190`) | `删除 Cookie 配置` | `Cookie 配置已删除` |
  | `setAdminBaseUrlRuleAction(rule, action)` (`:1196`) | `更新 BaseURL 规则` | `BaseURL 规则已更新` |
  | `saveAdminBaseUrlRule(rule)` (`:1202`) | `保存 BaseURL 规则` | `BaseURL 规则已保存` |
  | `deleteAdminBaseUrlRule(id)` (`:1208`) | `删除 BaseURL 规则` | `BaseURL 规则已删除` |
  | `toggleAdminShopItem(item)` (`:1214`) | `更新商品状态` | `商品状态已更新` (save with `isActive` flipped) |
  | `saveAdminShopItem(item)` (`:1220`) | `保存商品` | `商品已保存` |
  | `deleteAdminShopItem(id)` (`:1224`) | `删除商品` | `商品已删除` |
- `toggleAdminReviewSetting(kind)` reads the current `reviewSettings` (returns early if not `Success`) and posts **both** flags, flipping only the requested one.

---

## 6. Book edit info (编辑书籍信息)

### 6.1 Signature
`ui/BookEditScreens.kt:49-60`
```kotlin
@Composable
internal fun BookEditInfoScreen(
    state: BookEditState,
    onRetry: () -> Unit,
    onDraftChange: (BookEditDraft) -> Unit,
    onCoverSelected: (String) -> Unit,
    onAccessPolicyDraftChange: (BookAccessPolicyDraft) -> Unit,
    onSaveAccessPolicy: () -> Unit,
    onTransferIdentifierChange: (String) -> Unit,
    onTransfer: () -> Unit,
    onSave: () -> Unit
)
```
Wiring: `ui/NovalPieApp.kt:503-513`. Reached from Book detail via `onEditInfo` (`ui/NovalPieApp.kt:489`).

### 6.2 State
`ui/NovalPieViewModel.kt:359-371`
```kotlin
data class BookEditState(
    bookId: Long = 0,
    info: LoadResult<BookEditInfo> = Idle,
    permissions: LoadResult<BookEditPermissions> = Idle,
    draft: BookEditDraft = BookEditDraft(),
    accessPolicyDraft: BookAccessPolicyDraft = BookAccessPolicyDraft(),
    transferIdentifier: String = "",
    saving: Boolean = false, uploadingCover: Boolean = false,
    savingAccessPolicy: Boolean = false, transferringBook: Boolean = false,
    actionMessage: String? = null
)
```
`ui/BookEditPresentation.kt:5-18`
```kotlin
data class BookEditDraft(
    title = "", titleTranslation = "", authorName = "", description = "",
    source = "", sourceUrl = "", language = "zh", status = "连载中",
    isAdult = false, photoUrl = "", tags: List<String> = emptyList(), tagDraft = ""
)
```
`bookEditDraft(info)` (`:20-32`) copies every `BookEditInfo` field except `tagDraft`.

`BookEditPermissions` (`model/Models.kt:77-89`) — 11 booleans, all default `false`: `title, titleTranslation, authorName, description, source, sourceUrl, language, isAdult, photoUrl, spans, tags`. Note `spans` gates the serialization-status chips and there is **no permission flag for `status` itself**.

`BookAccessPolicyDraft` (`ui/BookChapterPresentation.kt:19-25`): `allowDownload = true, downloadThresholdType = "none", downloadThresholdValue = "0", readThresholdType = "none", readThresholdValue = "0"`.

### 6.3 Layout (`:110-211`)
`busy = state.saving || state.uploadingCover || state.savingAccessPolicy || state.transferringBook` (`:65`).
`validation = validateBookEditDraft(state.draft)` computed on every recomposition (`:61`).

Three confirm dialogs, hoisted before the list:
| flag | title | text | confirm | dismiss |
|---|---|---|---|---|
| `confirmSave` (`:67-80`) | `保存书籍信息` | `将把当前可编辑字段同步到网站。服务器仍会逐字段校验权限。` | `保存` | `取消` |
| `confirmPolicy` (`:81-94`) | `保存读写门槛` | `将同步阅读门槛、下载门槛和下载开关到网站。` | `保存` | `取消` |
| `confirmTransfer` (`:95-108`) | `转让书籍` | `转让会把此书的管理权交给接收方。请确认接收方标识无误：${state.transferIdentifier.trim()}` | `确认转让` | `取消` |

Root `LazyColumn(fillMaxSize, contentPadding 14.dp, spacedBy 12.dp)`:
1. Header: `编辑书籍信息` (`headlineSmall` Bold) + `书籍 #${state.bookId} · 字段权限、封面上传、门槛和转让均按网站接口执行。` (`onSurfaceVariant`).
2. `state.info` gate (`:121-125`): `Idle`/`Loading` → `LoadingBlock("正在加载书籍信息")`; `Error` → `ErrorBlock(message, "重新加载", onRetry)`; `Success` → nothing extra.
3. `state.permissions` gate (`:126-209`) — **the entire form only renders on `permissions is Success`**: `Idle`/`Loading` → `LoadingBlock("正在检查编辑权限")`; `Error` → `ErrorBlock(message, "重试权限", onRetry)`.
4. Inside `Success`, in order: cover section, 7 text fields, status/rating section, tag section, access-policy section, transfer section, and the message + save block.

### 6.4 Cover section (`BookEditCoverSection`, `:213-252`)
- Picker `ActivityResultContracts.OpenDocument()` launched with `arrayOf("image/*")`.
- Local `preview` flag keyed on `url`; when true and `url` non-blank shows `ImagePreviewDialog(url, "$title · 封面", onDismiss)`.
- `ElevatedCard(fillMaxWidth)`, Row padding 14.dp, `spacedBy 14.dp`:
  - `SubcomposeAsyncImage` (Coil), `model = ImageRequest.Builder(context).data(url).crossfade(true).precision(Precision.EXACT).build()`, `contentDescription = "书籍封面"`, size `100 × 150 dp`, `ContentScale.Crop`, clickable only when `url.isNotBlank()`; `loading = { LoadingBlock("加载封面") }`, `error = { Text("暂无封面") }`.
  - Right column: `封面` (Bold); `上传原始图片，不在 App 内压缩。点击封面可查看大图。` (`bodySmall`); `OutlinedButton(onClick = picker, enabled = enabled)` where `enabled = permissions.photoUrl && !busy`, label:
    - `上传中...` when `state.uploadingCover`
    - `选择图片` when enabled
    - `无编辑权限` otherwise

### 6.5 Text fields (`BookEditTextField`, `:254-272`)
`OutlinedTextField(fillMaxWidth, label, supportingText = { if (!permitted) Text("当前账号无此字段编辑权限") }, minLines, enabled = permitted && !busy)`.

| # | label | draft field | permission flag | minLines |
|---|---|---|---|---|
| 1 | `中文书名 *` | `title` | `title` | 1 |
| 2 | `原文书名` | `titleTranslation` | `titleTranslation` | 1 |
| 3 | `作者 *` | `authorName` | `authorName` | 1 |
| 4 | `简介` | `description` | `description` | 5 |
| 5 | `来源` | `source` | `source` | 1 |
| 6 | `来源链接` | `sourceUrl` | `sourceUrl` | 1 |
| 7 | `语言` | `language` | `language` | 1 |

Note field 7 is a **free-text** language field here (unlike the chip pickers in upload/editor).

### 6.6 Status / rating section (`BookEditStatusSection`, `:274-307`)
`ElevatedCard`, padding 14.dp, `spacedBy 10.dp`:
- Title `连载与分级` (Bold).
- `LazyRow(spacedBy 8.dp)` of `FilterChip`s over `listOf("连载中", "已完结")` — the chip label **is** the value written into `draft.status`. `enabled = permissions.spans && !busy`.
- Adult row `SpaceBetween`: label `成人内容`; when `!permissions.isAdult` an extra line `当前账号无此字段编辑权限` (`bodySmall`); `Switch(checked = draft.isAdult, enabled = permissions.isAdult && !busy)`.

### 6.7 Tag section (`BookEditTagSection`, `:309-350`)
`enabled = permissions.tags && !busy`.
- Title `标签` (Bold).
- When `draft.tags.isNotEmpty()`: `LazyRow(spacedBy 8.dp)` of `AssistChip(label = "$tag  ×", enabled = enabled)` whose click **removes** the tag (`tags - tag`). Note the label uses two spaces before `×`.
- Input row: `OutlinedTextField(draft.tagDraft, label "新增标签", singleLine, weight 1f, enabled)` + `OutlinedButton 添加`.
- `addTag()` (`:317-321`): trims; **silently ignores** blank or duplicate tags; on success appends and clears `tagDraft`. No length limit, no count limit (contrast with the forum tag editor).
- When `!permissions.tags`: trailing note `当前账号无标签编辑权限` (`bodySmall`).

### 6.8 Access-policy section (`BookAccessPolicySection`, `:352-399`)
`ElevatedCard`, padding 14.dp, `spacedBy 12.dp`:
- Title `阅读与下载门槛` (Bold).
- Allow-download row `SpaceBetween`: label `允许下载` + helper `关闭后下载门槛会按网页逻辑强制为无。`; `Switch(draft.allowDownload, enabled = !busy)`.
- `ThresholdEditor(title = "下载门槛", type = if (draft.allowDownload) draft.downloadThresholdType else "none", value = draft.downloadThresholdValue, enabled = draft.allowDownload && !busy, …)` — when downloads are off the editor is forced to display `none` and is disabled, but the underlying draft value is retained.
- `ThresholdEditor(title = "阅读门槛", type = draft.readThresholdType, value = draft.readThresholdValue, enabled = !busy, …)`.
- `validation?.let { Text(it, color = error) }` where `validation = validateBookAccessPolicyDraft(draft)`.
- `Button(onSave, enabled = validation == null && !busy, fillMaxWidth)` labelled `保存中...` when `state.savingAccessPolicy` else `保存读写门槛`. Click opens the `confirmPolicy` dialog.

**`ThresholdEditor(title, type, value, enabled, onTypeChange, onValueChange)`** (`:401-434`)
- `Text(title, labelLarge)`.
- `LazyRow(spacedBy 8.dp)` of `FilterChip`s over `listOf("none" to "不限", "points_min" to "最低积分", "points_pay" to "付费积分")`.
- When `type != "none"`: `OutlinedTextField(value, label "积分值", singleLine, fillMaxWidth, enabled)` — plain text field, **no digit filter**.

**Validation** — `validateBookAccessPolicyDraft` (`ui/BookChapterPresentation.kt:27-35`) checks download first (forcing `none`/`"0"` when downloads are disabled) then read, via `validateThresholdDraft(type, value, label)` (`:53-61`):
1. type not in `{none, points_min, points_pay}` → `$label 类型无效`
2. `none` → OK
3. value not an Int → `$label 必须是数字`
4. value `<= 0` → `$label 必须大于 0`
5. value above max → `$label 不能超过 $max`, where **max = 50 for `points_pay`, 100 otherwise**

Labels used in messages are the literals `下载门槛` and `阅读门槛`.

**`bookAccessPolicyFromDraft(draft)`** (`ui/BookChapterPresentation.kt:37-49`): forces `downloadType = "none"` and `downloadValue = 0` when `!allowDownload`; blank types normalize to `"none"`; values parse with `toIntOrNull() ?: 0` and are 0 whenever the type is `none`. Produces `ManagedBookAccessPolicy(allowDownload, downloadThresholdType, downloadThresholdValue, readThresholdType, readThresholdValue)` (`model/Models.kt:130-136`).

**Known gap:** nothing in `loadBookEditInfo` populates `accessPolicyDraft` from the server — it always starts at the defaults (`allowDownload = true`, both thresholds `none`/`0`). The section is write-only.

### 6.9 Transfer section (`BookTransferSection`, `:436-465`)
`ElevatedCard`, padding 14.dp, `spacedBy 10.dp`:
- Title `书籍转让` (Bold).
- Helper `支持填写 uid:数字 或用户名。服务器会再次校验接收方。` (`bodySmall`).
- `OutlinedTextField(state.transferIdentifier, label "接收方 UID 或用户名", singleLine, fillMaxWidth, enabled = !busy)`.
- Row: `Button(onTransfer, enabled = state.transferIdentifier.trim().isNotEmpty() && !busy)` labelled `提交中...` when `state.transferringBook` else `转让书籍`; then a spacer and the warning `这是管理权变更操作。` in `bodySmall`/`colorScheme.error`.

### 6.10 Message + save block (`:192-207`)
- `state.actionMessage?.let { Text(it, color = if (it.contains("失败") || it.contains("错误")) colorScheme.error else colorScheme.primary) }` — error coloring is decided by **substring matching on the Chinese words 失败 / 错误**.
- `validation?.let { Text(it, color = error) }`.
- `Button(onClick = { confirmSave = true }, fillMaxWidth, enabled = validation == null && !busy)` labelled `保存中...` when `state.saving` else `保存基本信息`.

**`validateBookEditDraft`** (`ui/BookEditPresentation.kt:34-40`):
1. blank `title` → `请填写中文书名`
2. blank `authorName` → `请填写作者`
3. any blank tag → `标签不能为空`
4. duplicate tags → `标签不能重复`

### 6.11 Behaviour (ViewModel)
- `openBookEditInfo(bookId)` (`:3152-3160`): requires `bookId > 0`; **login-gated** (`openLoginFallback()` when no token); pushes `AppRoute.BookEditInfo(bookId)` then loads.
- `loadBookEditInfo(bookId)` (`:3162-3184`): bumps `bookEditRequestSerial`; resets state to `Loading`/`Loading`; runs `api.managedBookInfo(bookId)` and `api.managedBookPermissions(bookId)` in parallel; guards on serial **and** on `currentRoute == AppRoute.BookEditInfo(bookId)`; error labels `加载书籍信息` and `加载编辑权限`; seeds `draft` from `bookEditDraft(info)` or `BookEditDraft()`.
- `updateBookEditDraft`, `updateBookAccessPolicyDraft`, `updateBookTransferIdentifier` (`:3186-3196`) all clear `actionMessage`.
- `saveManagedBook()` (`:3261-3309`): guarded by `bookId > 0` and none of the 4 busy flags; runs `validateBookEditDraft` (message into `actionMessage`); sets `saving = true`, message `正在保存书籍信息…`; calls `api.updateManagedBook(bookId, BookEditRequest(title, titleTranslation, authorName, description, source, sourceUrl, language, status, isAdult, photoUrl, tags))`. Route-guarded. Result message resolution:
  - `!saved.success` → `saved.message ?: saved.errors.joinToString("\n").ifBlank { "保存失败" }`
  - `saved.failedFields.isNotEmpty()` → `部分信息保存失败：${failedFields.joinToString(", ")}`
  - else → `saved.message ?: "书籍信息保存成功"`
  - on success also calls `loadBookDetail(bookId)`. Failure label `保存书籍信息`.
- `uploadManagedBookCover(rawUri)` (`:3311-3350`): guarded by `bookId > 0`, non-blank uri, no busy flag, and `permissions.photoUrl == true`. Sets `uploadingCover = true`, message `正在上传原始封面…`. Requires `document.sizeBytes > 0` (`封面文件为空`) and `mimeType?.startsWith("image/") == true` (`请选择图片文件`). Calls `api.uploadManagedBookCover(bookId, uploadSource(document))` which returns a URL; writes it into `draft.photoUrl` with message `封面已上传，保存信息后生效` — **the cover is not persisted until 保存基本信息 is pressed.** Failure label `上传封面`.
- `saveManagedBookAccessPolicy()` (`:3198-3226`): same busy guards; validates; message `正在保存读写门槛…`; `api.updateManagedBookAccessPolicy(bookId, bookAccessPolicyFromDraft(draft))`; success `it.message ?: "读写门槛已保存"`; failure label `保存读写门槛`.
- `transferManagedBook()` (`:3228-3259`): same busy guards; blank identifier → `请输入接收方 UID 或用户名`; message `正在提交书籍转让…`; `api.transferManagedBook(bookId, identifier)`; on success clears `transferIdentifier` and shows `transferred.message ?: "已提交转让给 $target"` where `target = transferred.targetUsername ?: transferred.targetUserId?.let { "UID $it" } ?: identifier`; failure label `转让书籍`.

APIs: `managedBookInfo(bookId)` `data/NovalPieApi.kt:172`, `managedBookPermissions(bookId)` :192, `updateManagedBook(bookId, request)` :215, `transferManagedBook(bookId, identifier)` :242, `updateManagedBookAccessPolicy(...)` :254, `uploadManagedBookCover(bookId, file)` :278.
Models: `BookEditInfo` :62, `BookEditPermissions` :77, `BookEditRequest` :91, `BookEditResult` :105 (`success, message, failedFields, errors`), `ManagedBookAccessPolicy` :130, `ManagedBookTransferResult` :138 (`success, message, targetUsername, targetUserId`).

---

## 7. Book chapter manager (章节管理)

### 7.1 Signature
`ui/BookChapterScreens.kt:61-83`
```kotlin
@Composable
internal fun BookChapterManagerScreen(
    state: BookChapterManagerState,
    onRetry: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onMove: (Long, Int) -> Unit,
    onSaveOrder: () -> Unit,
    onOpenEditor: (Chapter?) -> Unit,
    onUpdateEditor: (ManagedChapterDraft) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: () -> Unit,
    onDelete: (Long) -> Unit,
    onBatchDelete: () -> Unit,
    onTranslationMode: (String) -> Unit,
    onTranslate: () -> Unit,
    onOpenIllustrations: (Chapter) -> Unit,
    onDismissIllustrations: () -> Unit,
    onUploadIllustrations: (List<String>) -> Unit,
    onDeleteIllustration: (Long) -> Unit,
    onInsertIllustrationPlaceholder: (Int) -> Unit,
    onAppend: () -> Unit
)
```
Wiring: `ui/NovalPieApp.kt:515-536`.

### 7.2 State
`ui/NovalPieViewModel.kt:373-387`
```kotlin
data class BookChapterManagerState(
    bookId: Long = 0,
    chapters: LoadResult<List<Chapter>> = Idle,
    selectedIds: Set<Long> = emptySet(),
    orderDirty: Boolean = false,
    editor: ManagedChapterDraft? = null,
    editorLoading: Boolean = false,
    actionLoading: Boolean = false,
    translationMode: String = "shared",
    illustrationChapter: Chapter? = null,
    illustrations: LoadResult<ChapterIllustrationPage> = Idle,
    uploadingIllustrations: Boolean = false,
    deletingIllustrationId: Long? = null,
    actionMessage: String? = null
)
```
`ManagedChapterDraft` (`ui/BookChapterPresentation.kt:5-10`): `chapterId: Long? = null, insertAt: Int = 1, title = "", content = ""`.

### 7.3 Layout (`:137-196`)
Three local confirm flags: `deleteTarget: Chapter?`, `confirmBatchDelete`, `confirmTranslation`.

Dialogs hoisted first:
| trigger | title | text | confirm / dismiss |
|---|---|---|---|
| `state.editor != null` | see §7.5 | — | — |
| `state.illustrationChapter != null` | see §7.6 | — | — |
| `deleteTarget` (`:109-117`) | `删除章节` | `确定删除第 ${chapter.number ?: "?"} 章《${chapter.title}》吗？删除后无法恢复。` | `删除` / `取消` |
| `confirmBatchDelete` (`:118-126`) | `批量删除章节` | `确定删除选中的 ${state.selectedIds.size} 个章节吗？此操作不可恢复。` | `删除` / `取消` |
| `confirmTranslation` (`:127-135`) | `提交翻译` | `将选中的 ${state.selectedIds.size} 章提交到${if (state.translationMode == "shared") "共享" else "个人"}翻译任务。` | `提交` / `取消` |

Root `LazyColumn(fillMaxSize, contentPadding 14.dp, spacedBy 10.dp)`:
1. Header: `章节管理` (`headlineSmall` Bold) + `书籍 #${state.bookId} · 调整顺序后请先保存，再执行编辑、删除、插图或翻译。` (`onSurfaceVariant`).
2. Toolbar `LazyRow(spacedBy 8.dp)` (`:148-155`):
   | control | label | enabled |
   |---|---|---|
   | `Button` + `Icons.Filled.Add` | `插入章节` | `!actionLoading && !orderDirty` → `onOpenEditor(null)` |
   | `OutlinedButton` | `批量追加 EPUB` | `!actionLoading && !orderDirty` → `onAppend` (opens `AppRoute.BookAppend`) |
   | `OutlinedButton` | `取消全选` when `selectedIds.size == chapters.size` else `全选` | `chapters.isNotEmpty()` → `onSelectAll` |
   | `Button` (only when `orderDirty`) | `保存顺序` | `!actionLoading` → `onSaveOrder` |
3. `state.actionMessage?.let { Text(it, color = primary) }` — always primary-colored here, even for failures.
4. `if (state.editorLoading) LoadingBlock("正在加载章节正文")`.
5. Selection panel — only when `selectedIds.isNotEmpty()` (`:158-172`): `ElevatedCard` with `已选择 ${n} 章` (Bold) then a `LazyRow(spacedBy 8.dp)`:
   - `FilterChip(selected = translationMode == "personal")` label `个人翻译`
   - `FilterChip(selected = translationMode == "shared")` label `共享翻译`
   - `OutlinedButton 提交翻译`, `enabled = !actionLoading && !orderDirty`
   - `OutlinedButton 批量删除`, `enabled = !actionLoading && !orderDirty`
6. Chapter list (`:173-195`): `Idle`/`Loading` → `LoadingBlock("正在加载章节")`; `Error` → `ErrorBlock(message, "重新加载", onRetry)`; `Success` empty → `Text("暂无章节，可插入第一章或从 EPUB 批量追加。")`; else `items(result.value, key = { it.id })` → `ManagedChapterRow`.

### 7.4 Chapter row (`ManagedChapterRow`, `:199-228`)
`ElevatedCard(fillMaxWidth)`, single Row (padding 10.dp, `spacedBy 6.dp`, centered):
- `Checkbox(checked = selected, enabled = !busy)` → `onToggle`.
- Column (`weight 1f`, `spacedBy 3.dp`): `第 ${chapter.number ?: "?"} 章` (`labelMedium`, `primary`); `chapter.title` (SemiBold, `maxLines = 2`, ellipsis); `chapter.wordCount?.let { Text("$it 字", bodySmall) }`.
- Five `IconButton`s with content descriptions:
  | icon | contentDescription | enabled |
  |---|---|---|
  | `ArrowUpward` | `上移` | `!first && !busy` |
  | `ArrowDownward` | `下移` | `!last && !busy` |
  | `Edit` | `编辑` | `!busy` |
  | `Image` | `插图` | `!busy` |
  | `Delete` | `删除` | `!busy` |
  (`busy = state.actionLoading`; `first`/`last` computed by comparing ids to `result.value.first().id` / `.last().id`.)

### 7.5 Inline chapter editor (`ManagedChapterEditorDialog`, `:345-377`)
- Title `插入章节` when `draft.chapterId == null`, else `编辑章节`.
- Body: `Column(heightIn(max = 520.dp).verticalScroll(rememberScrollState()), spacedBy 10.dp)`:
  - Only for inserts: `OutlinedTextField(draft.insertAt.toString(), label "插入位置", singleLine, enabled = !busy)`; parsed with `it.toIntOrNull() ?: 0`.
  - `OutlinedTextField(draft.title, label "章节标题", fillMaxWidth, enabled = !busy)`.
  - `OutlinedTextField(draft.content, label "章节正文", minLines = 12, fillMaxWidth, enabled = !busy)`.
  - `(validation ?: message)?.let { Text(it, color = error) }` — validation takes priority over `state.actionMessage`, and both render in error color inside the dialog.
- Confirm `TextButton`, `enabled = validation == null && !busy`, label `保存中...` when `busy` else `保存`.
- Dismiss `TextButton 取消`, `enabled = !busy`.

**`validateManagedChapterDraft`** (`ui/BookChapterPresentation.kt:12-17`):
1. insert with `insertAt < 1` → `插入位置必须大于 0`
2. blank title → `章节标题不能为空`
3. blank content → `章节内容不能为空`

### 7.6 Illustration manager (`ManagedChapterIllustrationDialog`, `:230-303`)
- Picker `ActivityResultContracts.OpenMultipleDocuments()` launched with `arrayOf("image/*")`; passes all selected URIs as strings when non-empty.
- `busy = state.uploadingIllustrations || state.deletingIllustrationId != null`.
- Local `preview: ChapterIllustration?` → `ImagePreviewDialog(image.src, "插图 ${chapterIllustrationPlaceholder(image.index)}", …)`.
- Local `deleteTarget: ChapterIllustration?` → `AlertDialog(title = "删除章节插图", text = "确定删除 ${chapterIllustrationPlaceholder(image.index)} 吗？正文里的占位符不会自动移除。")`, confirm `删除` / dismiss `取消`.
- Outer `AlertDialog`, `onDismissRequest` gated on `!busy`, single confirm button `关闭` (`enabled = !busy`), title `章节插图`:
  - `chapter.title` (Bold).
  - Copy: `网页占位符格式：[[img:N]]。打开同一章节正文编辑器后，可把占位符直接插入草稿。` (`bodySmall`).
  - `OutlinedButton(picker, enabled = !busy)` labelled `上传中...` when uploading else `上传插图`.
  - `state.actionMessage?.let { Text(it, color = primary) }`.
  - `state.illustrations`: `Idle`/`Loading` → `LoadingBlock("正在加载章节插图")`; `Error` → `Text(message, color = error)`; `Success` → `共 ${total} 张` (`labelMedium`), then `暂无插图。` if the list is empty, else one `ManagedChapterIllustrationRow` per image.
  - Container: `Column(heightIn(max = 560.dp).verticalScroll(rememberScrollState()), spacedBy 10.dp)`.

**`ManagedChapterIllustrationRow`** (`:305-343`)
- `ElevatedCard(fillMaxWidth)`, Row padding 10.dp, `spacedBy 10.dp`:
  - Coil `SubcomposeAsyncImage(data = image.src, crossfade, Precision.EXACT)`, `contentDescription = chapterIllustrationPlaceholder(image.index)`, `72 × 96 dp`, `ContentScale.Crop`, clickable → preview; `loading = { LoadingBlock("图") }`, `error = { Text("图") }`.
  - Column (`weight 1f`): `chapterIllustrationPlaceholder(image.index)` (Bold) then a `LazyRow(spacedBy 8.dp)`:
    | button | label | enabled |
    |---|---|---|
    | `OutlinedButton` | `预览` | always |
    | `OutlinedButton` | `插入` | `canInsert` = `state.editor?.chapterId == chapter.id` |
    | `OutlinedButton` | `删除中...` when deleting else `删除` | `!deleting` |

`chapterIllustrationPlaceholder(index)` (`ui/BookChapterPresentation.kt:51`): `"[[img:${index.coerceAtLeast(1)}]]"`.
`ChapterIllustration` (`model/Models.kt:112-116`): `id, index, src`. `ChapterIllustrationPage` (:118-121): `images`, `total` (default `images.size`).

### 7.7 Behaviour (ViewModel)
- `openBookChapters(bookId)` (`:3352-3360`): requires `bookId > 0`; **login-gated**; pushes `AppRoute.BookChapters(bookId)` then loads.
- `loadManagedChapters(bookId)` (`:3362-3374`): bumps `bookChapterRequestSerial`; **resets the whole state** to `BookChapterManagerState(bookId, chapters = Loading)` (so selection, order-dirty, editor, illustrations are all cleared on every reload); `api.chapters(bookId)`; guarded by serial + route; error label `加载章节管理列表`.
- `toggleManagedChapterSelection(chapterId)` (`:3376-3380`): set-toggle.
- `selectAllManagedChapters()` (`:3382-3388`): if `selectedIds.size == allIds.size` → clear, else select all. (Note: compares sizes, not contents.)
- `moveManagedChapter(chapterId, delta)` (`:3390-3402`): local reorder only; `to = (from + delta).coerceIn(0, lastIndex)`; on success **renumbers every chapter's `number` to its 1-based index**, sets `orderDirty = true`, message `章节顺序尚未保存`.
- `saveManagedChapterOrder()` (`:3404-3417`): requires `orderDirty`, not `actionLoading`, non-empty list; message `正在保存章节顺序…`; `api.reorderManagedChapters(bookId, chapters.map { it.id })`; success clears `orderDirty` with `it.message ?: "章节顺序已更新"`; failure label `保存章节顺序`.
- `openManagedChapterEditor(chapter: Chapter? = null)` (`:3419-3453`):
  - If `orderDirty` → message `请先保存章节顺序` and abort.
  - `chapter == null` (insert): `editor = ManagedChapterDraft(insertAt = chapterCount + 1)` — i.e. defaults to appending at the end.
  - Otherwise sets `editorLoading = true`, message `正在加载章节正文…`, calls `api.chapterContent(chapter.id)` and seeds `ManagedChapterDraft(chapterId = chapter.id, insertAt = chapter.number ?: 1, title = content.title ?: chapter.title, content = content.content)`. Failure label `加载章节正文`.
- `updateManagedChapterDraft(draft)` (`:3455-3457`), `dismissManagedChapterEditor()` (`:3459-3463`, ignored while `actionLoading`).
- `saveManagedChapterDraft()` (`:3577-3603`): validates; guarded on `actionLoading`; message `正在保存章节…`; insert → `api.insertManagedChapter(bookId, insertAt, title, content)`, update → `api.updateManagedChapter(chapterId, title, content)`; on success closes the dialog with `it.message ?: "章节已保存"` and reloads the list; failure label `保存章节`.
- `deleteManagedChapter(chapterId)` (`:3605-3609`): blocked when `orderDirty`, `actionLoading`, or `chapterId <= 0`; `runManagedChapterMutation(state, "删除章节") { api.deleteManagedChapter(chapterId) }`.
- `batchDeleteManagedChapters()` (`:3611-3617`): blocked when `orderDirty`/`actionLoading`/empty selection; label `批量删除章节`; `api.batchDeleteManagedChapters(bookId, selectedIds.toList())`.
- `updateManagedTranslationMode(mode)` (`:3619-3623`): accepts only `personal` / `shared`.
- `translateSelectedManagedChapters()` (`:3625-3631`): blocked when `orderDirty`/`actionLoading`/empty selection; label `提交章节翻译`; `api.requestManagedChapterTranslation(bookId, selectedIds.toList(), translationMode)` with `refresh = false` — **selection is kept and the list is not reloaded** after a translation request.
- `runManagedChapterMutation(state, label, refresh = true, action)` (`:3633-3657`): sets `actionLoading`, message `"$label…"`; on success message `it.message ?: "$label 已完成"`, clears `selectedIds` only when `refresh`, and reloads the list when `refresh`; on failure `apiFailureMessage(label, it)`. Route-guarded on `AppRoute.BookChapters(bookId)`.
- `openManagedChapterIllustrations(chapter)` (`:3465-3485`): blocked when `orderDirty` (message `请先保存章节顺序`); bumps the shared `bookChapterRequestSerial`; sets `illustrationChapter` and `illustrations = Loading`; `api.managedChapterIllustrations(chapter.id)`; error label `加载章节插图`.
- `dismissManagedChapterIllustrations()` (`:3487-3495`): only when not uploading/deleting; clears `illustrationChapter` and resets `illustrations` to `Idle`.
- `uploadManagedChapterIllustrations(rawUris)` (`:3497-3533`): requires an open `illustrationChapter`, non-empty uris, and no in-flight upload/delete; message `正在上传原始章节插图…`; per document requires `sizeBytes in 1..WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES` (`单张插图必须在 20 MiB 以内`) and `mimeType == null || startsWith("image/")` (`请选择图片文件`); calls `api.uploadManagedChapterIllustrations(chapter.id, documents.map { uploadSource(it, fallbackContentType = "image/jpeg") })`; on success message `it.message ?: "章节插图已上传"` and **re-opens** the illustration list to refresh; failure label `上传章节插图`.
- `deleteManagedChapterIllustration(imageId)` (`:3535-3559`): requires an open chapter, `imageId > 0`, no in-flight op; sets `deletingIllustrationId`, message `正在删除章节插图…`; `api.deleteManagedChapterIllustration(chapter.id, imageId)`; success `it.message ?: "章节插图已删除"` then refresh; failure label `删除章节插图`.
- `insertChapterIllustrationPlaceholder(index)` (`:3561-3575`): if there is no open illustration chapter, or the open editor's `chapterId` differs, sets message `请先打开同一章节的正文编辑器，再插入图片占位符` and aborts. Otherwise appends `chapterIllustrationPlaceholder(index)` to `editor.content`, inserting a leading `\n` unless the content already ends with a newline or is blank; message `已插入 ${placeholder}`.

APIs: `api.chapters(bookId)` → `GET /api/novels/$bookId/chapters` (`data/NovalPieApi.kt:1277`); `api.chapterContent(chapterId)` → `GET /api/chapters/$chapterId/content?session=…&replace_mode=india&show_images=1` (:1281); `api.managedChapterIllustrations(chapterId)` → `GET /api/users/me/chapters/$chapterId/illustrations` (:1296); `api.uploadManagedChapterIllustrations(chapterId, files)` → multipart with `chapter_id` part, per-file size ≤ 20 MiB and `image/*` content type enforced again server-side of the client (:1301); `api.deleteManagedChapterIllustration(chapterId, imageId)` (:1328); `api.reorderManagedChapters(bookId, orderedChapterIds)` (:1338); `api.insertManagedChapter(bookId, insertAt, title, content)` (:1350); `api.updateManagedChapter(chapterId, title, content)` (:1365); `api.deleteManagedChapter(chapterId)` (:1376); `api.batchDeleteManagedChapters(bookId, chapterIds)` (:1381); `api.requestManagedChapterTranslation(bookId, chapterIds, mode)` → `POST /api/users/me/novels/$bookId/translation-requests` `{chapter_ids, mode}` with `mode in {personal, shared}` (:1391).

---

## 8. Forum create (发布帖子)

### 8.1 Signature
`ui/ForumCreateScreens.kt:42-48`
```kotlin
@Composable
internal fun ForumCreateScreen(
    state: ForumCreateState,
    onDraftChange: (ForumCreateDraft) -> Unit,
    onSubmit: () -> Unit,
    onOpenLogin: () -> Unit
)
```
Wiring: `ui/NovalPieApp.kt:194-199`.

### 8.2 State + draft
`ui/NovalPieViewModel.kt:337-343`
```kotlin
data class ForumCreateState(
    draft: ForumCreateDraft = ForumCreateDraft(),
    isAdmin: Boolean = false,
    accessMessage: String? = null,
    submitting: Boolean = false,
    actionMessage: String? = null
)
```
`ui/ForumCreatePresentation.kt:9-21`
```kotlin
data class ForumCreateDraft(
    type = "", title = "", content = "",
    tags: List<String> = emptyList(), tagDraft = "",
    pollEnabled = false, pollQuestion = "",
    pollOptions: List<String> = listOf("", ""),
    pollAllowMultiple = false, pollMaxChoices = 2, pollEndsAt = ""
)
```

### 8.3 Categories
`forumCategoryOptions(isAdmin)` (`ui/ForumCreatePresentation.kt:28-35`) — order matters; the announcement entry only exists for admins:
| id | title | description |
|---|---|---|
| `announcement` (admin only) | 公告 | 站务公告，仅管理员可以发布 |
| `recommend` | 推书 | 分享喜欢的作品，详细介绍会更有帮助 |
| `discussion` | 交流 | 聊剧情、设定、吐槽或求书 |
| `feedback` | 反馈 | 反馈问题或建议，并附复现步骤、错误信息和链接 |

### 8.4 Layout (`:49-195`)
`validation = validateForumCreateDraft(draft, state.isAdmin)`; local `showSubmitConfirmation`.

Confirm dialog (`:53-68`): title `确认发布`; text `帖子将立即发布到“${forumCategoryOptions(isAdmin).firstOrNull { it.id == draft.type }?.title.orEmpty()}”分区。` (full-width CJK quotes); confirm `发布`, dismiss `继续编辑`.

Root `LazyColumn(fillMaxSize, contentPadding 14.dp, spacedBy 14.dp)`:
1. Header: `发布帖子` (`headlineSmall` Bold) + `分享想法、推书或反馈，正文支持 Markdown。` (`onSurfaceVariant`).
2. Access banner — only when `state.accessMessage != null` (`:85-96`): `ElevatedCard(containerColor = errorContainer)` with the message (Bold, `onErrorContainer`) and `Button(onOpenLogin)` labelled `升级或切换账号`.
3. **Two-stage form.** When `draft.type.isBlank()` a category picker is shown; once a type is set the full form appears.

**Stage A — category picker** (`:98-113`)
- Section title `选择发布分区` (`titleMedium` Bold).
- One `ElevatedCard(fillMaxWidth, clickable(enabled = state.accessMessage == null))` per category, keyed `it.id`, containing `category.title` (`titleMedium` Bold) and `category.description` (`onSurfaceVariant`). Clicking sets `draft.type`.

**Stage B — full form** (`:114-194`), in order:
1. `ForumSelectedCategory` (`:198-218`): `ElevatedCard` Row — left column shows `category?.title ?: draft.type` (Bold) and the description; right `TextButton 更换` which clears `draft.type` back to `""` (returning to stage A, keeping title/content/tags/poll).
2. Title field (`:122-132`): `OutlinedTextField(draft.title, label "标题 *", supportingText = "${draft.title.length}/100", singleLine, enabled = !submitting && accessMessage == null)`; input hard-truncated with `it.take(100)`.
3. Content field (`:133-144`): `OutlinedTextField(draft.content, label "正文 *", placeholder "支持标题、粗体、列表、引用、代码和链接等 Markdown 语法", supportingText = "${draft.content.length}/10000", minLines = 10, enabled = …)`; input truncated with `it.take(10_000)`.
4. `ForumMarkdownPreview(draft.content)` (`:220-243`) — **returns early and renders nothing when content is blank**. Otherwise `ElevatedCard`:
   - `正文预览` (Bold).
   - `SelectionContainer` wrapping a `Column(spacedBy 6.dp)` of the **first 12** paragraphs from `readerParagraphsFromContent(content)` (`ui/ReaderText.kt:73-91`: converts `<br>`/block-end tags to markers, decodes with `Html.fromHtml(FROM_HTML_MODE_LEGACY)`, normalizes NBSP and CRLF, splits on blank lines). So the "Markdown preview" is really an HTML-decoded plain-paragraph preview — Markdown syntax is not rendered.
   - When `forumContentLinks(paragraphs)` (`ui/ForumPresentation.kt:57-62`, URL regex with trailing-punctuation trimming, `distinct()`) is non-empty: label `链接` (`labelLarge`) then the **first 4** links, each in a `Surface(radius 8.dp, surfaceVariant, padding 10.dp, bodySmall)`.
5. `ForumTagEditor` (`:245-286`).
6. `ForumPollEditor` (`:288-375`).
7. Guidance card (`:162-171`): `ElevatedCard` with `发布须知` (Bold) and three bullet lines, verbatim:
   - `• 内容应真实准确并遵守社区规范`
   - `• 标题应简明，标签应与主题相关`
   - `• 重要链接和复现步骤请直接写入正文`
8. Footer (`:172-193`):
   - `message = state.actionMessage ?: validation.message`; when non-blank it renders in `onSurfaceVariant` only if `validation.canSubmit && state.actionMessage == null`, otherwise in `colorScheme.error`.
   - `Button(onClick = { showSubmitConfirmation = true }, fillMaxWidth, enabled = validation.canSubmit && !state.submitting && state.accessMessage == null)` labelled `发布中…` when submitting else `发布帖子`.

### 8.5 Tag editor (`ForumTagEditor`, `:245-286`)
- Title `标签（选填）` (Bold).
- Existing tags: `LazyRow(spacedBy 8.dp)` of `AssistChip(label = "#$tag  ×", enabled)`; click removes.
- Input row: `OutlinedTextField(draft.tagDraft, label "输入标签", singleLine, weight 1f, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { addTag() }), enabled)`; input truncated with `it.take(20)`.
- `OutlinedButton 添加`, `enabled = enabled && draft.tags.size < 5`.
- Footnote: `最多 5 个，每个不超过 20 个字符` (`bodySmall`).
- `addTag()` (`:251-255`): silently ignores blank, duplicate, `tags.size >= 5`, or `tag.length > 20`.

### 8.6 Poll editor (`ForumPollEditor`, `:288-375`)
- Header row `SpaceBetween`: left column `投票（可选）` (Bold) + `2–10 个选项` (`bodySmall`, note the en-dash `–`); right `Switch(draft.pollEnabled, enabled)`.
- All the following render only when `pollEnabled`:
  - `OutlinedTextField(draft.pollQuestion, label "投票问题", placeholder "留空时使用帖子标题", enabled)`; truncated `it.take(200)`.
  - One row per option: `OutlinedTextField(option, label "选项 ${index + 1}", weight 1f, enabled)` truncated `it.take(200)`, plus `TextButton 删除` (`enabled = enabled && draft.pollOptions.size > 2`) which removes the option and clamps `pollMaxChoices` to the new size.
  - `OutlinedButton 添加选项`, `enabled = enabled && draft.pollOptions.size < 10`, appends an empty string.
  - Row `SpaceBetween`: label `允许多选` + `Switch(draft.pollAllowMultiple, enabled)`.
  - Only when `pollAllowMultiple`: a stepper row — label `最多选择`, `OutlinedButton "−"` (U+2212 minus, `enabled = pollMaxChoices > 2`, decrements with `coerceAtLeast(2)`), the current value in Bold, `OutlinedButton "+"` (`enabled = pollMaxChoices < pollOptions.size`, increments with `coerceAtMost(pollOptions.size)`).
  - `OutlinedTextField(draft.pollEndsAt, label "截止时间（选填）", placeholder "例如 2026-07-20T08:00:00.000Z", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), singleLine, enabled)` — free text, no format validation client-side.

### 8.7 Validation
`validateForumCreateDraft(draft, isAdmin)` (`ui/ForumCreatePresentation.kt:37-66`) returns `ForumCreateValidation(canSubmit, message)`; first failure wins:
1. `type == "announcement" && !isAdmin` → `只有管理员可以发布公告`
2. `type` not in the allowed set → `请选择发布分区`
3. blank trimmed title → `请输入帖子标题`
4. title length > 100 → `标题不能超过 100 个字符`
5. blank trimmed content → `请输入帖子内容`
6. content length > 10 000 → `内容不能超过 10000 个字符`
7. > 5 non-blank tags → `最多添加 5 个标签`
8. any tag longer than 20 → `单个标签不能超过 20 个字符`
9. duplicate tags → `标签不能重复`
10. if `!pollEnabled` → `ForumCreateValidation(true)` (no message)
11. < 2 non-blank options → `至少填写 2 个投票选项`
12. > 10 options → `投票选项不能超过 10 个`
13. duplicate options → `投票选项不能重复`
14. `pollAllowMultiple && pollMaxChoices !in 2..options.size` → `多选上限应为 2 到 ${options.size}`
15. else `ForumCreateValidation(true)`

### 8.8 Behaviour (ViewModel)
- `openForumCreate()` (`:2711-2726`): **login-gated** — no token → `openLoginFallback()` and abort. Otherwise resets `ForumCreateState(isAdmin = isAdminProfile(profile), accessMessage = if (profile?.role == "guest") "游客账号不能发帖，请先升级账号" else null)` and pushes `AppRoute.ForumCreate`.
- `updateForumCreateDraft(draft)` (`:2728-2730`): clears `actionMessage`.
- `submitForumPost()` (`:2732-2794`):
  - No-op when already `submitting` or `accessMessage != null`.
  - Re-validates; failure writes `validation.message` to `actionMessage`.
  - `api.createForumPost(ForumCreateRequest(type, title, content, tags, poll = draft.takeIf { it.pollEnabled }?.let { ForumPollDraft(question = pollQuestion, options = pollOptions, allowMultiple = pollAllowMultiple, maxChoices = pollMaxChoices, endsAt = pollEndsAt.takeIf(String::isNotBlank)) }))`.
    Note the **raw, untrimmed** `pollOptions` list (including empty strings) is sent.
  - Bails out silently if the user navigated away (`currentRoute != AppRoute.ForumCreate`).
  - `!created.success` → `actionMessage = created.message ?: "发布失败"`.
  - Success: `loadForum()`, then if `created.postId > 0` pop the create route and push `AppRoute.ForumPostDetail(postId)` + `loadForumPostDetail(postId)`; otherwise just pop the create route. State resets to `ForumCreateState(isAdmin = state.isAdmin)`.
  - Failure label `发布帖子`.
- API: `api.createForumPost(request)` → `data/NovalPieApi.kt:1033`. Models `ForumPollDraft` (`model/Models.kt:628`), `ForumCreateRequest` (:636), `ForumCreateResult` (:644).

---

## 9. Backing engines and data layers

### 9.1 `data/UploadFileSource.kt` (whole file)
```kotlin
class UploadFileSource(
    val fileName: String,
    val sizeBytes: Long,
    val contentType: String? = null,
    val openStream: () -> InputStream
)
```
A lazily-reopenable stream handle. `openStream` may be invoked more than once (chunked uploads reopen per range), which is why the ViewModel builds it from a `ContentResolver`/`File` rather than holding an open stream.

### 9.2 `data/EpubParser.kt` — reading EPUBs
`object EpubParser`, `fun parse(source: UploadFileSource): ParsedEpub`.

Limits (`:14-16`): `CONTAINER_PATH = "META-INF/container.xml"`, `MAX_XML_BYTES = 4 MiB`, `MAX_CHAPTER_BYTES = 24 MiB`.

Algorithm (`:18-75`):
1. Read `META-INF/container.xml`; missing → `IOException("EPUB 缺少 META-INF/container.xml")`.
2. Take the first `rootfile/@full-path`; missing/blank → `IOException("EPUB 未声明 OPF 路径")`.
3. Read the OPF; missing → `IOException("EPUB 缺少 OPF 文件")`.
4. Build a `manifest: LinkedHashMap<id, resolvedPath>` from all `item` elements (namespace-agnostic `getElementsByTagNameNS("*", …)`), stripping `#fragment` and resolving against the OPF directory.
5. Build `spinePaths` from `itemref/@idref` in document order; empty → `IOException("EPUB 目录为空")`.
6. Read every spine entry (single ZIP pass), convert each to plain text; skip entries that are missing or whose text is blank.
7. Chapter title = `extractHtmlTitle(html)`, falling back to `"第 ${index + 1} 章"`; `chapterNumber = index + 1`; `rawPath = path`; `spineIndex = index`.
8. Zero surviving chapters → `IOException("EPUB 未解析到有效章节")`.
9. Metadata from the OPF: `title`, `creator` → author, `description`, `language` (blank → `"zh"`).

Details:
- `readEntries` (`:77-98`): one `ZipInputStream` pass, only requested normalized paths, early-exits once all are found. Per-entry cap enforced by `readCurrentEntry` → `IOException("EPUB 条目过大：$name")`.
- Chapter bytes are decoded as **UTF-8 unconditionally** (`bytes.toString(Charsets.UTF_8)`, `:56`).
- `parseXml` (`:114-123`): namespace-aware, `isExpandEntityReferences = false`, and best-effort XXE hardening — `disallow-doctype-decl`, `external-general-entities`, `external-parameter-entities` all disabled inside `runCatching`.
- `extractHtmlTitle` (`:131-137`): first `<h1>`–`<h3>` inner text, else `<title>`, then tag-stripped, entity-decoded, trimmed.
- `htmlToPlainText` (`:139-150`): drops `<script>`/`<style>` blocks; converts `</?(p|div|section|article|blockquote|li|h1-6)…>` and `<br>` to `\n`; strips remaining tags; decodes entities; removes `\r`; collapses `[ \t]+\n` → `\n` and 3+ newlines → `\n\n`; trims.
- `decodeHtmlEntities` (`:154-160`): only `&nbsp; &amp; &lt; &gt; &quot; &#39;` (case-insensitive). Numeric/other named entities are **not** decoded.
- `normalizeZipPath` (`:165-175`): backslash→slash, drops empty and `.` segments, resolves `..` by popping — i.e. zip-slip safe.

`ParsedEpub` (`model/Models.kt:466-473`): `title, author, description, language = "zh", chapters, epubFilePath`.
`UploadChapter` (`model/Models.kt:456-464`): `title, content, chapterNumber, hierarchyLevel = 0, sectionPath = emptyList(), rawPath, spineIndex`. **`hierarchyLevel` and `sectionPath` are never populated or read by any of the screens in this part.**

Tests: `app/src/test/java/com/novalpie/nativeapp/data/EpubParserTest.kt`.

### 9.3 `data/EditorProcessor.kt` — chapter splitting + website markers
`object EditorProcessor`. Marker regexes (`:6-7`):
```kotlin
titleIdentifier   = Regex("^##__T\\[(\\d{5})]__##$", MULTILINE)
contentIdentifier = Regex("^##__C\\[(\\d{5})]__##$", MULTILINE)
```

| function | signature | behaviour |
|---|---|---|
| `splitByRegex` | `(text, patterns: List<String>): List<UploadChapter>` (`:9-16`) | Runs every non-blank pattern with `RegexOption.MULTILINE`, unions the matches, `distinctBy { it.range.first }`, sorts by position. Title = the whole matched line, trimmed. |
| `splitByMarkdown` | `(text, level: Int)` (`:18-24`) | `require(level in 1..6)` else `Markdown 标题层级必须介于 1 到 6`. Pattern `^#{level}[ \t]+(.+)$` MULTILINE. Title = capture group 1, trimmed. The `[ \t]+` immediately after the hashes makes the level **exact** — H1 mode does not match `## …` and H2 mode does not match `### …`. Only levels 1 and 2 are reachable from the UI. |
| `splitByKeywordNumber` | `(text, keywords: List<String>)` (`:26-33`) | Escapes and de-duplicates keywords, joins with `\|`, matches `^.*(?:kw1\|kw2).*\d+.*$` with MULTILINE + IGNORE_CASE. Returns `emptyList()` when all keywords are blank. Title = the whole line, trimmed. |
| `splitByCharacterCount` | `(text, targetCharacters: Int)` (`:35-56`) | `require(> 0)` else `目标字数必须大于 0`. Splits on blank lines (`\n\s*\n`), trims, drops empties, then greedily groups paragraphs, starting a new group when adding the next one (plus 2 separator chars) would exceed the target. Titles are auto-generated `第 N 章`; content joined with `\n\n`. |
| `splitByParagraphCount` | `(text, targetParagraphs: Int)` (`:58-68`) | `require(> 0)` else `目标段落数必须大于 0`. Same paragraph split then `chunked(n)`; titles `第 N 章`. |
| `toWebsiteIdentifiers` | `(chapters): String` (`:70-73`) | Emits, per chapter, `##__T[00001]__##\n<title>\n##__C[00001]__##\n<content>` joined with `\n`. Number is `chapterNumber.coerceAtLeast(1)` zero-padded to 5. |
| `validateWebsiteIdentifiers` | `(text): List<String>` (`:75-85`) | Cross-checks title vs content markers and returns human-readable errors: `章节 $id 缺少内容标识符`, `章节 $id 缺少标题标识符`, `存在重复的标题标识符`, `存在重复的内容标识符`, `标题与内容标识符顺序不一致`. **Never called from any screen in this part — dead but preserved capability.** |
| `parseWebsiteIdentifiers` | `(text): List<UploadChapter>` (`:87-99`) | Regex `^##__T\[(\d{5})]__##[ \t]*\r?\n([^\r\n]+)[ \t]*\r?\n##__C\[\1]__##[ \t]*\r?\n(.*?)(?=^##__T\[\d{5}]__##\|\z)` with MULTILINE + DOT_MATCHES_ALL (note the `\1` back-reference: title and content ids must match). Title falls back to `Chapter ${index + 1}` (English), content trimmed, `chapterNumber = index + 1`. |
| `chaptersFromMatches` | private `(text, matches, title)` (`:101-119`) | Any text before the first match becomes a **preface** that is prepended to chapter 1's body (joined with `\n\n`). Each body spans from just after the match to the start of the next match (or EOF), trimmed. Blank titles fall back to `第 ${index + 1} 章`. |

Tests: `app/src/test/java/com/novalpie/nativeapp/data/EditorProcessorTest.kt`.

### 9.4 `ui/EditorScriptEngine.kt` — the JS sandbox
**Data types**
```kotlin
internal data class EditorScriptOptions(
    mode: String, chunkSize: Int, chunkIndex: Int, totalChunks: Int,
    textLength: Int, isFirstChunk: Boolean, isLastChunk: Boolean
)
```

**`chunkEditorScriptText(text, targetSize): List<String>`** (`:25-41`)
- `require(targetSize > 0)` else `Chunk size must be positive`.
- Empty text → `listOf("")`.
- Walks forward `targetSize` characters, then extends to the **next `\n` inclusive** (so chunks always break on line boundaries) or to EOF. Concatenating the chunks reproduces the input exactly (asserted in `EditorScriptContractTest`).

**`editorScriptOptions(chunks, textLength): List<EditorScriptOptions>`** (`:43-56`)
- `mode = if (chunks.size > 1) "chunked" else "full"`, per-chunk `chunkSize = chunk.length`, `chunkIndex`, `totalChunks`, `textLength`, `isFirstChunk`, `isLastChunk`.
- Note `EditorScriptEngine.process` then **overrides** `mode`/`chunkSize` (`:152-155`): chunked runs report `mode = "chunked"` and `chunkSize = targetChunkSize`; non-chunked runs report `mode = "full"` and `chunkSize = text.length`.

**`buildEditorScriptProgram(script, text, options): String`** (`:58-133`)
Generates a self-contained IIFE with `"use strict"`. Injected values are JSON-quoted (`JSONObject.quote`) so arbitrary user text/script is safe to embed. `options` is a JSON object with keys `mode, chunkSize, chunkIndex, totalChunks, textLength, isFirstChunk, isLastChunk`.

Exposed sandbox API (the exact set advertised in the UI):
| helper | behaviour |
|---|---|
| `insertMarker(index, type)` | Pushes `{index: max(0, floor(index)), type: type === "content" ? "content" : "title"}`. Non-finite index → `Error("insertMarker index must be numeric")`. |
| `findMatches(pattern, flags)` | Accepts a `RegExp` or a string (default flags `"gm"`). Iterates over the **original chunk text** returning `{index, value, groups: [...]}`; guards zero-length matches by bumping `lastIndex`. |
| `splitByParagraphs(value)` | `String(value).split(/\n\s*\n/)` |
| `splitByWords(value)` | `String(value).trim().split(/\s+/).filter(Boolean)` |
| `getParagraphs()` | Paragraphs of the chunk text with blank ones removed |
| `getWordCount()` | `splitByWords(text).length` |
| `getLineCount()` | `text.split("\n").length` |
| `console` | A local shim whose `log/info/warn/error` push stringified values into an internal `__logs` array (returned in the payload but **currently discarded** by `parseEditorScriptCallback`) |

Execution contract:
- The user script is compiled via `new Function(<helpers…>, "console", source + "\nreturn typeof processText === 'function' ? processText : null;")`, so the script body may declare anything; only `processText` is exported.
- Missing/non-function `processText` → `Error("Script must define processText(text, options)")`.
- Invocation is arity-aware: `processText.length >= 2 ? processText(text, options) : processText(text)`.
- `undefined`/`null` return falls back to the original chunk text; everything else is `String(...)`-coerced.
- Markers are then applied to the **output** string: sorted descending by index and spliced in as `##__T[NNNNN]__##\n` or `##__C[NNNNN]__##\n`, where `NNNNN` is the marker's *index* zero-padded to 5 and the insertion position is `min(index, output.length)`. (Note: the padded number is the character offset, not a chapter number — so `parseWebsiteIdentifiers` will only match when a title marker and a content marker were inserted at the *same* index value.)
- Return payload: `JSON.stringify({ok: true, result, logs})` or `{ok: false, error, logs}`.

**`parseEditorScriptCallback(raw): String`** (`:135-146`)
`evaluateJavascript` hands back a JSON-encoded value, so the parser accepts either a JSON string (double-encoded) or a JSON object; anything else → `IllegalArgumentException("Script returned an invalid response")`. `ok != true` → `IllegalArgumentException(payload.error ?: "Script execution failed")`. Otherwise returns `payload.result`.

**`internal class EditorScriptEngine(context: Context)`** (`:148-207`)
```kotlin
suspend fun process(script: String, text: String, chunked: Boolean, targetChunkSize: Int): String
```
- Runs on `Dispatchers.Main.immediate` (WebView requirement).
- Chunks when requested, builds per-chunk options, evaluates each chunk **sequentially**, and concatenates the results with `buildString(text.length)`.
- `evaluate(program)` (`:163-202`): `withTimeout(SCRIPT_TIMEOUT_MS = 15_000L)` around a fresh `WebView` per chunk. Settings hardening: `javaScriptEnabled = true`, `javaScriptCanOpenWindowsAutomatically = false`, `domStorageEnabled = false`, `databaseEnabled = false`, `blockNetworkLoads = true`, `allowFileAccess = false`, `allowContentAccess = false`, `loadsImagesAutomatically = false`. Loads `<html><body></body></html>` with `loadDataWithBaseURL(null, …)` and runs the program in `onPageFinished`. `stopLoading()` + `destroy()` on completion or cancellation; guarded by a `completed` flag so it resumes exactly once.
- Timeout semantics: **15 s per chunk**, not per whole run.

Tests: `app/src/test/java/com/novalpie/nativeapp/ui/EditorScriptContractTest.kt` (Robolectric) asserts chunk reassembly, option ordering, helper presence in the generated program, JSON-quoting of user input, and callback success/error parsing.

### 9.5 `data/EpubWriter.kt` — writing EPUBs
`object EpubWriter`, `fun write(output: OutputStream, metadata: EditorBookMetadata, chapters: List<UploadChapter>)`.

Preconditions (`:15-17`): `书名不能为空`, `作者不能为空`, `至少需要一个章节`.

Emitted ZIP layout (`:18-26`), in order:
1. `mimetype` — written **STORED (uncompressed)** with an explicit CRC32, value `application/epub+zip` (`writeStoredMimetype`, `:29-41`).
2. `META-INF/container.xml` → single `rootfile full-path="OEBPS/content.opf"`.
3. `OEBPS/content.opf` — EPUB 3.0 package, `unique-identifier="book-id"`, `dc:identifier = urn:uuid:${UUID.randomUUID()}`, `dc:title`, `dc:creator`, `dc:language` (blank → `zh`), `dc:description`, and a **hard-coded** `<meta property="dcterms:modified">2026-07-10T00:00:00Z</meta>` (`:67`). Manifest lists `nav` plus `chapter-N` items; spine lists `chapter-N` itemrefs in order.
4. `OEBPS/nav.xhtml` — `<nav epub:type="toc">` with `<h1>目录</h1>` and one `<li><a href="chapter-N.xhtml">title</a></li>` per chapter.
5. `OEBPS/chapter-N.xhtml` (1-based) — `<h1>title</h1>` followed by paragraphs: content has `\r` removed, is split on `\n\s*\n`, each block wrapped in `<p>…</p>` with remaining single `\n` converted to `<br/>`.

`xml(value)` escapes `& < > " '` (in that order) to `&amp; &lt; &gt; &quot; &apos;` (`:98-104`).
Note: chapter file names/order come from the **list index**, not `UploadChapter.chapterNumber`, so the exported book is always contiguously numbered. `metadata.tags`, `metadata.isAdult`, `metadata.source`, `metadata.sourceUrl` are **not written into the EPUB** (they only travel through the archive store and the upload hand-off).

Tests: `app/src/test/java/com/novalpie/nativeapp/data/EpubWriterTest.kt`.

### 9.6 `data/EditorArchiveStore.kt` — the draft archive
```kotlin
class EditorArchiveStore(context: Context, directoryName: String = "epub-editor-archives")
```
Directory: `File(context.filesDir, "epub-editor-archives")` — app-private internal storage (matching the UI copy `这会删除 App 私有目录中的全部编辑器存档。`).

**Two files per archive**, which is what the UI blurb `正文与索引分文件保存；列表不会把全部长文本重新载入内存。` refers to:
- `<safeId>.json` — metadata index
- `<safeId>.txt` — the raw body text

`archiveFile(id)` (`:63-67`) sanitizes with `id.replace(Regex("[^A-Za-z0-9._-]"), "_")` and `require(safeId.isNotBlank())` else `存档 ID 不能为空`.

| method | behaviour |
|---|---|
| `save(archive)` (`:16-29`) | Creates the directory (`无法创建存档目录`), writes both files to `.tmp` siblings, deletes existing targets (`无法替换现有存档信息`, `无法替换现有存档正文`), then renames both (`保存存档失败`). Atomic-ish two-phase write. |
| `list()` (`:31-37`) | Missing directory → `emptyList()`. Reads only `*.json`, deserializes with `textContent = ""` (so **the list never loads bodies**), silently drops unparseable files, sorted by `timestamp` descending. |
| `load(id)` (`:39-44`) | Missing json → `null`. Reads the `.txt` if present (else `""`) and returns the fully hydrated archive; parse failures return `null`. |
| `delete(id)` (`:46-51`) | Deletes json (`删除存档失败`) then txt (`删除存档正文失败`). |
| `clear()` (`:53-57`) | Deletes every file in the directory whose extension is `json`, `txt`, or `tmp` (ignoring failures). |

JSON schema (`toJson`/`fromJson`, `:71-109`): top level `id, name, timestamp, fileName, chapterCount, totalWords`, nested `metadata { title, author, description, language, tags, isAdult, source, sourceUrl }`. Defaults on read: `name → "存档"`, `language → "zh"`, others empty/0/false. `fileName` blank → `null`. Missing `id` throws (`getString`) and is swallowed by the caller's `runCatching`.

Tests: `app/src/test/java/com/novalpie/nativeapp/data/EditorArchiveStoreTest.kt`.

---

## 10. Cross-screen notes, gaps, and refactor traps

1. **Login gating is inconsistent and per-screen.** `openBookEditInfo`, `openBookChapters`, `openBookAppend`, `openForumCreate` all redirect to `openLoginFallback()`. `openWorkspace`, `openUploadBook`, `openUploadEditor`, `openPoliticalExam` do not gate at all (Upload and Political Exam render their own in-screen login banners; Workspace and the Editor render nothing). `openAdminSection` silently `return`s for non-admins with no user feedback.
2. **Admin is gated three times**: `openAdminSection`, `loadAdminSectionInternal`, and `runAdminMutation` each re-check `isAdminProfile(currentUserProfile())`. `currentUserProfile()` (`ui/NovalPieViewModel.kt:1662-1663`) falls back to `decodeAuthTokenProfile(authToken)` when `homeState.user` is not loaded.
3. **`AppRoute.PoliticalExam` has no `routeContextLabel` case** — the top bar shows the bottom-tab name instead of 政治考试 (`ui/UiNavigation.kt:19-38`).
4. **Raw English/server strings surface in the UI**: admin key statuses (`pending`/`approved`/`rejected`), BaseURL rule actions (`allow`/`block`/`manual`), shop item `type` (`frame`/`badge`), review request `type`/`status`, operation-log `action`/`status` and the action filter chips, and workspace translation-job `status` chips (`pending`/`paused`/`completed`). These are load-bearing display strings today.
5. **Never-rendered parsed fields** (safe to keep parsing, but do not "clean up" the models without a decision): `AdminOverviewStats.pendingReviewUpload/pendingReviewDelete`; `AdminReviewRequest.userId/novelId/createdAt`; `AdminKeyItem.createdAt`; `AdminBaseUrlRule.description` (list only); `AdminCookieConfig.updatedAt`; `AdminSchedulerLogs.lastModified`; `AdminShopItem.imageUrl/badgeHtml/badgeCss` (list only); `PoliticalExamDetail.question/userAnswer/correctAnswer/explanation`; `UploadChapter.hierarchyLevel/sectionPath`; `EditorProcessor.validateWebsiteIdentifiers`; `NovalPieApi.adminApproveAllReviews`.
6. **No pagination controls** exist for admin review requests (fixed `page_size=100`) or operation logs (`totalPages` displayed, `page` always 1).
7. **`BookEditState.accessPolicyDraft` is never seeded from the server** — the threshold section always opens at defaults and is effectively write-only.
8. **Cover upload is two-phase**: `uploadManagedBookCover` only writes the returned URL into the draft; the user must then press `保存基本信息`. The copy `封面已上传，保存信息后生效` encodes this.
9. **Order-dirty is a hard interlock** in the chapter manager: 插入章节, 批量追加 EPUB, 提交翻译, 批量删除, single delete, opening the editor, and opening illustrations are all blocked while `orderDirty`, with the message `请先保存章节顺序`.
10. **`loadManagedChapters` resets the entire manager state**, so any successful mutation with `refresh = true` also clears selection, the editor, and the illustration dialog. Translation submission deliberately uses `refresh = false` to keep the selection.
11. **Editor encoding is apply-on-open only** — changing the chip after the file is loaded has no effect until the file is reopened.
12. **`loadEditorArchive` deliberately drops the chapter list** and forces the user back to the 文本 tab with `存档已加载，请重新生成章节目录`.
13. **The editor's `scriptRunId` counter is the only trigger** for JS execution, and the WebView engine lives in the composable, not the ViewModel. Any refactor that moves script execution must preserve: the `busy` + `scriptRunId` + `splitMode == CustomScript` triple-guard in the `LaunchedEffect`, the stale-run check in `completeEditorCustomScript`, the `1024..1_000_000` clamp with `200_000` fallback, and the 15 s per-chunk timeout.
14. **API keys are stored in plaintext** in `SharedPreferences` (`novalpie_native_workspace`) by `WorkspaceLocalStore` and are read back by the editor's AI-regex feature. The editor UI deliberately advertises `API key 不会在这里显示。`
15. **`generateEditorRegex` is the only outbound call that does not go to novalpie.cc** — it posts directly to the user's configured OpenAI-compatible endpoint via `executeExternal("editor AI regex", …)`.
16. **`apiFailureMessage` labels are user-visible.** Full list used by this part: 解析 EPUB, 上传书籍, 打开编辑文件, AI 生成正则, 保存存档, 删除存档, 清空存档, 生成 EPUB, 生成上传文件, 开始考试, 提交考试, 工作区 API 配置, Cookie 状态, Cookie 配置, 工作区健康状态, 保存 API 配置, 删除 API 配置, API 配置已删除, Cookie 配置已保存, Cookie 状态已更新, Cookie 配置已删除, 管理总览, 审核设置, 审核请求, Key 管理, 操作日志, BaseURL 规则, 调度日志, 商店商品, 更新审核设置, 处理审核请求, 更新 Key 状态, 删除 Key, 更新 Cookie 配置, 保存 Cookie 配置, 删除 Cookie 配置, 更新 BaseURL 规则, 保存 BaseURL 规则, 删除 BaseURL 规则, 更新商品状态, 保存商品, 删除商品, 加载书籍信息, 加载编辑权限, 保存书籍信息, 上传封面, 保存读写门槛, 转让书籍, 加载章节管理列表, 保存章节顺序, 加载章节正文, 保存章节, 删除章节, 批量删除章节, 提交章节翻译, 加载章节插图, 上传章节插图, 删除章节插图, 发布帖子.
17. **Request-staleness serials** used by these screens: `adminRequestSerial`, `uploadRequestSerial`, `editorRequestSerial`, `workspaceRequestSerial`, `bookEditRequestSerial`, `bookChapterRequestSerial`. Several handlers *also* guard on `currentRoute == <expected route>`; `uploadManagedBookCover` notably reads (not increments) `bookEditRequestSerial`. Both guard styles must survive the refactor or in-flight results will land in the wrong state.
18. **Two composables serve the append route** and share the same `UploadBookState`; `existingNovelId` is the only discriminator, and `clearUploadBook()` re-derives it from `currentRoute`.
19. **Relevant unit tests to keep green**: `ui/WorkspacePresentationTest.kt`, `ui/UploadPresentationTest.kt`, `ui/EditorScriptContractTest.kt`, `ui/AdminPresentationTest.kt`, `ui/PoliticalExamPresentationTest.kt`, `ui/BookManagementPresentationTest.kt`, `ui/ForumCreatePresentationTest.kt`, `ui/ToolsPresentationTest.kt`, `ui/UiNavigationTest.kt`, `data/EpubParserTest.kt`, `data/EpubWriterTest.kt`, `data/EditorProcessorTest.kt`, `data/EditorArchiveStoreTest.kt`, `data/WorkspaceLocalStoreTest.kt`, `data/AdminApiTest.kt`, `data/UploadApiTest.kt`, `data/WorkspaceApiTest.kt`, `data/BookManagementApiTest.kt`.
