# 02 — ViewModel State & Action Inventory (`ui/NovalPieViewModel.kt`, 4063 lines)

**Target of inventory:** `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
(read in full, line by line, in 9 chunks). Supporting files read in full:
`ui/RequestFreshness.kt`, `ui/ApiMessages.kt`, `ui/ErrorRecovery.kt`, `ui/VisibleUiLabels.kt`,
`ui/RouteStackPolicy.kt`, `ui/UploadPresentation.kt`, `ui/EditorPresentation.kt`,
`ui/ForumCreatePresentation.kt`, `ui/BookEditPresentation.kt`, `ui/BookChapterPresentation.kt`,
`ui/WorkspacePresentation.kt`, `ui/MessagePresentation.kt`, `model/Models.kt`, and all 8 `data/*Store.kt`.

**Headline counts**

| Thing | Count |
|---|---|
| `mutableStateOf` properties (public, `private set`) | 40 |
| `mutableStateListOf` properties (private) | 1 (`routes`) |
| Derived public read-only state | 1 (`currentRoute`) |
| Data classes declared in this file | 26 (25 public + 1 private `EditorLoadResult`) |
| Enums declared in this file | 3 (`BottomTab`, `UserProfileTab`, `AdminSection`) |
| Sealed route hierarchy | `AppRoute` + 24 route objects/data classes |
| Public functions | **227** |
| Private functions / extensions inside the class | 23 |
| Top-level functions at file end | 3 (`resolveUserLoadResult`, 2 private mapping extensions) |
| `*RequestSerial` fields | **16** |
| Store instances held | 8 |
| Distinct `NovalPieApi` methods called | 104 |
| `isFreshRequestSerial(...)` guard sites | 34 |
| Route-equality guard sites (`currentRoute != AppRoute…`) | 15 |
| Feature domains identified | 17 |

---

## 0. How to read this document

* Every claim carries `file:line`. Unqualified line numbers refer to `ui/NovalPieViewModel.kt`.
* Chinese strings are quoted **verbatim and decoded**. The source is inconsistent: some literals are
  written as real UTF-8 CJK, others as `\uXXXX` escapes (e.g. line 1266 `"\u6d88\u606f\u7edf\u8ba1"` =
  `消息统计`). Both forms are shown decoded. §11 is the complete decoded string table.
* Two string literals in this file are **mojibake** (byte-level corruption baked into source) and are
  currently shown to users as garbage — see §12.1. They must be reproduced *as intentional fixes*, not
  copied blindly.
* §13 is the only section that proposes structure.

---

## 1. File anatomy (line map)

| Lines | Content |
|---|---|
| 1–94 | imports (44 model imports, 14 data imports, coroutines, `java.io`, `java.util.Calendar`) |
| 96–102 | `enum class BottomTab(val title: String)` |
| 104–129 | `sealed class AppRoute` + 24 routes |
| 131–415 | 25 public state/support data classes + `UserProfileTab` + `AdminSection` enums |
| 417–426 | class header + 8 Store instantiations |
| 427–436 | proxy/auth state holders (must precede `api` — `api` closes over them) |
| 438–448 | `private val api: NovalPieApi` with cookie / token / proxy providers |
| 450–467 | `routes` list, 16 request serials, `selectedFavoriteGroupId` |
| 469–548 | 36 remaining `mutableStateOf` state holders |
| 550 | `val currentRoute: AppRoute get() = routes.lastOrNull() ?: AppRoute.Home` |
| 552–556 | `init { configureNovalPieImageLoader(...); loadForum(); loadHome() }` |
| 558–734 | shell: bookshelf query, favorites group, search options, reader font/theme, proxy, auth, tabs |
| 735–999 | Profile (own) + UserProfileDetail |
| 1001–1251 | Admin (sections, queries, mutations, `runAdminMutation`) |
| 1253–1270 | Tools hub |
| 1272–1660 | Message center / detail / conversation / settings + 2 private action runners |
| 1662–1663 | `currentUserProfile()` |
| 1665–1834 | Upload book (EPUB pick, submit, append) |
| 1836–2265 | EPUB editor (31 functions) |
| 2267–2400 | Political exam |
| 2402–2477 | upload/editor IO private helpers + `EditorLoadResult` |
| 2479–2690 | Workspace |
| 2692–2947 | Forum feed / post detail / create / reactions |
| 2949–3150 | Book & chapter comment drafts + reactions (3 near-identical blocks) |
| 3152–3350 | Book management (edit info, access policy, transfer, cover) |
| 3352–3657 | Chapter management (order, editor, illustrations, translate) |
| 3659–3748 | Book/reader navigation, deep link, `goBack` |
| 3750–3916 | Home/favorites + Search |
| 3918–3997 | Book detail + Reader loads + progress save |
| 3999–4030 | private utilities + `companion object { PAGE_SIZE = 20; TOOLS_MESSAGE_PREVIEW_LIMIT = 6 }` |
| 4032–4063 | top-level `resolveUserLoadResult` + `SearchOptions`↔`PersistedSearchSettings` mappers |

---

## 2. Enums and route model (context needed by everything else)

### 2.1 `BottomTab` (96–102) — 5 tabs, titles are user-visible

| Constant | `title` (verbatim) |
|---|---|
| `Collection` | `收藏` |
| `Discover` | `搜索` |
| `Tools` | `工具` |
| `Forum` | `论坛` |
| `Profile` | `我的` |

Note: `BottomTab.title` is **not** what the bottom bar renders — `ui/UiNavigation.kt:3
bottomTabDisplayLabel()` supplies the displayed label. `title` is effectively dead but public.

### 2.2 `AppRoute` (104–129) — 24 routes

Parameterless objects: `Forum`, `Home`, `Search`, `Tools`, `Profile`, `Settings`, `MessageCenter`,
`MessageSettings`, `Workspace`, `UploadBook`, `UploadEditor`, `PoliticalExam`, `ForumCreate`.
Parameterised: `MessageDetail(messageId: Long)`, `MessageConversation(targetUserId: Long, targetName: String?)`,
`ForumPostDetail(postId: Long)`, `BookDetail(bookId: Long)`, `BookEditInfo(bookId: Long)`,
`BookChapters(bookId: Long)`, `BookAppend(bookId: Long)`, `Reader(bookId: Long, chapterId: Long)`,
`UserProfileDetail(userId: Long)`, `Admin(section: AdminSection)`, `WebFallback(url: String)`.

Because these are `data class`es, route **equality** is structural — this is load-bearing: 15 call sites
guard staleness with `if (currentRoute != AppRoute.X(id)) return@launch` (see §8.3).

### 2.3 `UserProfileTab` (160–164)
`Checkin`, `Activities`, `Books`. Default in state: `Activities`.

### 2.4 `AdminSection(val websitePath: String)` (177–184)

| Constant | `websitePath` |
|---|---|
| `Overview` | `/admin` |
| `Review` | `/admin/review` |
| `Keys` | `/admin/key-management` |
| `OperationLogs` | `/admin/operation-logs` |
| `Scraper` | `/admin/scraper-management` |
| `Shop` | `/admin/shop` |

`websitePath` is used by `ToolsScreen` route dispatch (`ui/NovalPieApp.kt:279`) to map a tapped tool entry
back to a section.

---

## 3. State data classes — every field, type, default

### 3.1 Declared inside `NovalPieViewModel.kt`

**`HomeState` (131–139)** — domain: Library/Bookshelf
| Field | Type | Default |
|---|---|---|
| `user` | `LoadResult<UserProfile>` | `LoadResult.Idle` |
| `groups` | `LoadResult<List<FavoriteGroup>>` | `Idle` |
| `favorites` | `LoadResult<List<NovelCard>>` | `Idle` |
| `favoritesPage` | `Int` | `1` |
| `favoritesCanLoadMore` | `Boolean` | `false` |
| `favoritesLoadingMore` | `Boolean` | `false` |
| `selectedFavoriteGroupId` | `Long?` | `null` |

**`ForumState` (141–143)** — `posts: LoadResult<List<ForumPost>> = Idle` (single field).

**`ProfileState` (145–158)** — domain: own profile
| Field | Type | Default |
|---|---|---|
| `profile` | `LoadResult<UserProfile>` | `Idle` |
| `checkinStats` | `LoadResult<UserCheckinStats>` | `Idle` |
| `nameDraft` | `String` | `""` |
| `bioDraft` | `String` | `""` |
| `showCheckin` | `Boolean` | `true` |
| `autoCheckin` | `Boolean` | `false` |
| `adultBirthYearDraft` | `String` | `""` |
| `saving` | `Boolean` | `false` |
| `checkingIn` | `Boolean` | `false` |
| `verifyingAdult` | `Boolean` | `false` |
| `uploadingAvatar` | `Boolean` | `false` |
| `actionMessage` | `String?` | `null` |

**`UserProfileDetailState` (166–175)** — domain: other-user profile
`userId: Long = 0`, `profile: LoadResult<UserProfile> = Idle`,
`activities: LoadResult<List<UserActivity>> = Idle`, `books: LoadResult<List<NovelCard>> = Idle`,
`checkinStats: LoadResult<UserCheckinStats> = Idle`,
`checkinRecords: LoadResult<List<UserCheckinRecord>> = Idle`,
`checkinSettings: LoadResult<UserCheckinSettings> = Idle`,
`selectedTab: UserProfileTab = UserProfileTab.Activities`.

**`AdminState` (186–201)**
`section: AdminSection = Overview`, `reviewQuery: AdminReviewQuery = AdminReviewQuery()`,
`operationLogQuery: AdminOperationLogQuery = AdminOperationLogQuery()`,
`overview: LoadResult<AdminOverviewStats> = Idle`, `reviewSettings: LoadResult<AdminReviewSettings> = Idle`,
`reviewRequests: LoadResult<List<AdminReviewRequest>> = Idle`, `keys: LoadResult<List<AdminKeyItem>> = Idle`,
`operationLogs: LoadResult<AdminOperationLogPage> = Idle`,
`cookieConfigs: LoadResult<List<AdminCookieConfig>> = Idle`,
`baseUrlRules: LoadResult<List<AdminBaseUrlRule>> = Idle`,
`schedulerLogs: LoadResult<AdminSchedulerLogs> = Idle`, `shopItems: LoadResult<List<AdminShopItem>> = Idle`,
`actionLoading: Boolean = false`, `actionMessage: String? = null`.

**`AdminReviewQuery` (203–207)** — `type: String = ""`, `status: String = ""`, `keyword: String = ""`.

**`AdminOperationLogQuery` (209–218)** — `page: Int = 1`, `action = ""`, `status = ""`, `userId = ""`,
`novelId = ""`, `keyword = ""`, `startDate = ""`, `endDate = ""` (all `String` except `page`).

**`ToolsState` (220–223)** — `stats: LoadResult<MessageStats> = Idle`,
`messages: LoadResult<List<SiteMessage>> = Idle`.

**`MessageCenterState` (225–234)**
`query: MessageQuery = MessageQuery()`, `messages: LoadResult<List<SiteMessage>> = Idle`,
`pagination: MessagePagination = MessagePagination()`, `stats: LoadResult<MessageStats> = Idle`,
`selectedIds: Set<Long> = emptySet()`, `loadingMore = false`, `actionLoading = false`,
`actionMessage: String? = null`.

**`MessageDetailState` (236–241)** — `messageId: Long = 0`, `detail: LoadResult<SiteMessage> = Idle`,
`actionLoading = false`, `actionMessage: String? = null`.

**`MessageConversationState` (243–250)** — `targetUserId: Long = 0`, `targetName: String? = null`,
`messages: LoadResult<List<DirectMessage>> = Idle`, `draft: String = ""`, `sending = false`,
`actionMessage: String? = null`.

**`MessageSettingsState` (252–257)** — `settings: LoadResult<MessageSettings> = Idle`,
`draft: MessageSettings = MessageSettings()`, `saving = false`, `actionMessage: String? = null`.

**`WorkspaceState` (259–269)**
`selectedTab: WorkspaceTab = WorkspaceTab.Overview`, `apiConfigs: LoadResult<List<WorkspaceApiConfig>> = Idle`,
`cookieStatus: LoadResult<WorkspaceCookieStatus> = Idle`,
`cookieConfigs: LoadResult<WorkspaceCookieConfigs> = Idle`, `health: LoadResult<WorkspaceHealth> = Idle`,
`localApis: List<WorkspaceLocalApiConfig> = emptyList()`,
`jobs: List<WorkspaceTranslationJob> = emptyList()`, `actionLoading = false`, `actionMessage: String? = null`.

**`UploadDocument` (271–276)** — `uri: String`, `displayName: String`, `sizeBytes: Long`,
`mimeType: String? = null` (no defaults on first three).

**`UploadBookState` (278–288)**
`existingNovelId: Long? = null` (non-null ⇒ "append chapters to existing book" mode),
`draft: UploadBookDraft = UploadBookDraft()`, `selectedFile: UploadDocument? = null`,
`chapters: LoadResult<List<UploadChapter>> = Idle`, `serverFilePath: String? = null`,
`processing = false`, `progressLabel: String? = null`,
`submitResult: LoadResult<UploadActionResult> = Idle`, `actionMessage: String? = null`.

**`UploadEditorState` (290–313)** — 21 fields, the widest state class
| Field | Type | Default |
|---|---|---|
| `selectedTab` | `EditorTab` | `EditorTab.Text` |
| `text` | `String` | `""` |
| `fileName` | `String?` | `null` |
| `encoding` | `String` | `"UTF-8"` |
| `metadata` | `EditorBookMetadata` | `EditorBookMetadata()` |
| `chapters` | `List<UploadChapter>` | `emptyList()` |
| `splitMode` | `EditorSplitMode` | `EditorSplitMode.Regex` |
| `splitPattern` | `String` | `DEFAULT_EDITOR_CHAPTER_REGEX` = `^第[\d零一二三四五六七八九十百千万]+章.*$` |
| `splitTarget` | `String` | `"3000"` |
| `customScript` | `String` | `DEFAULT_EDITOR_CUSTOM_SCRIPT` (`function processText(text, options) { return text; }`) |
| `scriptChunked` | `Boolean` | `false` |
| `scriptChunkSize` | `String` | `"200000"` |
| `scriptRunId` | `Long` | `0` |
| `aiConfigs` | `List<WorkspaceLocalApiConfig>` | `emptyList()` |
| `selectedAiConfigId` | `Long?` | `null` |
| `findText` | `String` | `""` |
| `replaceText` | `String` | `""` |
| `findUsesRegex` | `Boolean` | `false` |
| `archiveName` | `String` | `""` |
| `archives` | `List<EditorArchive>` | `emptyList()` |
| `busy` | `Boolean` | `false` |
| `actionMessage` | `String?` | `null` |

**`PoliticalExamState` (315–324)** — `phase: PoliticalExamPhase = Landing`,
`session: LoadResult<PoliticalExamSession> = Idle`, `answers: PoliticalExamAnswers = PoliticalExamAnswers()`,
`remainingTimeSeconds: Int = 1800`, `deadlineEpochMillis: Long? = null`,
`result: LoadResult<PoliticalExamResult> = Idle`, `submitting = false`, `actionMessage: String? = null`.

**`ForumPostDetailState` (326–335)** — `postId: Long = 0`, `detail: LoadResult<ForumPostDetail> = Idle`,
`comments: LoadResult<List<ForumComment>> = Idle`, `commentDraft: String = ""`,
`replyingToCommentId: Long? = null`, `replyingToName: String? = null`, `actionMessage: String? = null`,
`actionLoading = false`.

**`ForumCreateState` (337–343)** — `draft: ForumCreateDraft = ForumCreateDraft()`, `isAdmin = false`,
`accessMessage: String? = null` (non-null blocks submission), `submitting = false`,
`actionMessage: String? = null`.

**`BookDetailState` (345–357)** — `bookId: Long = 0`, `book: LoadResult<NovelCard> = Idle`,
`chapters: LoadResult<List<Chapter>> = Idle`, `comments: LoadResult<List<ChapterComment>> = Idle`,
`favoriteStatus: LoadResult<FavoriteStatus> = Idle`, `readerProgress: ReaderProgress? = null`,
`commentDraft: String = ""`, `replyingToCommentId: Long? = null`, `replyingToName: String? = null`,
`actionMessage: String? = null`, `actionLoading = false`.

**`BookEditState` (359–371)** — `bookId: Long = 0`, `info: LoadResult<BookEditInfo> = Idle`,
`permissions: LoadResult<BookEditPermissions> = Idle`, `draft: BookEditDraft = BookEditDraft()`,
`accessPolicyDraft: BookAccessPolicyDraft = BookAccessPolicyDraft()`, `transferIdentifier: String = ""`,
`saving = false`, `uploadingCover = false`, `savingAccessPolicy = false`, `transferringBook = false`,
`actionMessage: String? = null`.

**`BookChapterManagerState` (373–387)** — `bookId: Long = 0`,
`chapters: LoadResult<List<Chapter>> = Idle`, `selectedIds: Set<Long> = emptySet()`,
`orderDirty = false`, `editor: ManagedChapterDraft? = null`, `editorLoading = false`,
`actionLoading = false`, `translationMode: String = "shared"`, `illustrationChapter: Chapter? = null`,
`illustrations: LoadResult<ChapterIllustrationPage> = Idle`, `uploadingIllustrations = false`,
`deletingIllustrationId: Long? = null`, `actionMessage: String? = null`.

**`ReaderState` (389–400)** — `bookId: Long = 0`, `chapterId: Long = 0`,
`content: LoadResult<ReaderContent> = Idle`, `chapters: LoadResult<List<Chapter>> = Idle`,
`comments: LoadResult<List<ChapterComment>> = Idle`, `commentDraft: String = ""`,
`replyingToCommentId: Long? = null`, `replyingToName: String? = null`, `actionMessage: String? = null`,
`actionLoading = false`.

**`SearchOptions` (402–410)** — `sortBy = "relevance"`, `sortOrder = "desc"`, `scope = "all"`,
`matchType = "ai"`, `adultFilter = "all"`, `source = ""`, `wordCountRange = ""` (all `String`).
Round-trips to `PersistedSearchSettings` (4043–4063) 1:1 — identical field names & defaults.

**`ReaderUiOptions` (412–415)** — `fontSizeSp: Int = 18`, `theme: String = "system"`.

**`EditorLoadResult` (private, 2472–2477)** — `document: UploadDocument`, `text: String`,
`metadata: EditorBookMetadata`, `chapters: List<UploadChapter>` (no defaults). Internal transport only.

### 3.2 Draft/support classes that live in state but are declared elsewhere

These are *part of the ViewModel state contract* even though they sit in presentation files.

| Class | File:line | Fields (defaults) |
|---|---|---|
| `UploadBookDraft` | `ui/UploadPresentation.kt:8` | `title=""`, `titleTranslation=""`, `author=""`, `description=""`, `language="ja"`, `spans="balanced"`, `isAdult=false`, `source=""`, `sourceUrl=""`, `tagsText=""`, `submitType="chinese"`, `coverUrl=""`, `chapterCount=0` |
| `UploadParseMode` | `ui/UploadPresentation.kt:6` | enum `LOCAL`, `SERVER_CHUNKED`; threshold `WEBSITE_SERVER_EPUB_THRESHOLD_BYTES = 50 MiB`, chunk `WEBSITE_UPLOAD_CHUNK_BYTES = 5 MiB` |
| `EditorTab` | `ui/EditorPresentation.kt:3` | `Text("文本")`, `Split("分章")`, `Chapters("目录")`, `Metadata("书籍")`, `Archives("存档")` |
| `EditorSplitMode` | `ui/EditorPresentation.kt:11` | `Regex("正则表达式")`, `MarkdownH1("Markdown 一级标题")`, `MarkdownH2("Markdown 二级标题")`, `KeywordNumber("关键词 + 数字")`, `CharacterCount("按字数")`, `ParagraphCount("按段落数")`, `CustomScript("自定义脚本")` |
| `ForumCreateDraft` | `ui/ForumCreatePresentation.kt:9` | `type=""`, `title=""`, `content=""`, `tags=emptyList()`, `tagDraft=""`, `pollEnabled=false`, `pollQuestion=""`, `pollOptions=listOf("","")`, `pollAllowMultiple=false`, `pollMaxChoices=2`, `pollEndsAt=""` |
| `BookEditDraft` | `ui/BookEditPresentation.kt:5` | `title=""`, `titleTranslation=""`, `authorName=""`, `description=""`, `source=""`, `sourceUrl=""`, `language="zh"`, `status="连载中"`, `isAdult=false`, `photoUrl=""`, `tags=emptyList()`, `tagDraft=""` |
| `BookAccessPolicyDraft` | `ui/BookChapterPresentation.kt:19` | `allowDownload=true`, `downloadThresholdType="none"`, `downloadThresholdValue="0"`, `readThresholdType="none"`, `readThresholdValue="0"` |
| `ManagedChapterDraft` | `ui/BookChapterPresentation.kt:5` | `chapterId: Long?=null`, `insertAt=1`, `title=""`, `content=""` |
| `WorkspaceTab` | `ui/WorkspacePresentation.kt:5` | `Overview("概览")`, `Apis("API 管理")`, `Cookies("Cookie 管理")`, `Queue("任务队列")` |
| `WorkspaceApiDraft` | `ui/WorkspacePresentation.kt:12` | `id=null`, `serverId=null`, `name=""`, `model="deepseek-chat"`, `endpoint="https://api.deepseek.com"`, `apiKey=""`, `concurrency="10"`, `shareToServer=false` |
| `WorkspaceCookieDraft` | `ui/WorkspacePresentation.kt:23` | `id=null`, `configKey=""`, `description=""`, `cookieRaw=""`, `proxyIp=""`, `isActive=true` |
| `PoliticalExamPhase` | `ui/PoliticalExamPresentation.kt:6` | `Landing`, `Active`, `Result` |
| `SearchRequestSnapshot` | `ui/RequestFreshness.kt:28` | `serial: Long`, `keyword: String`, `options: SearchOptions`, `page: Int` |

Model defaults referenced by state defaults (from `model/Models.kt`):
`MessageQuery()` = `keyword=""`, `messageType=null`, `isRead=null`, `priority=null` (325);
`MessagePagination()` = `page=1`, `pageSize=20`, `total=0`, `totalPages=1` (332);
`MessageSettings()` = `enableNotifications=true`, `enableEmail=false`, `enableBrowserPush=true`,
`notificationTypes=null`, `quietHoursStart=null`, `quietHoursEnd=null`, `autoReadAfterDays=null` (344);
`EditorBookMetadata()` = `title/author/description=""`, `language="zh"`, `tags=""`, `isAdult=false`,
`source/sourceUrl=""` (498); `PoliticalExamAnswers()` = 4 empty lists (541);
`LoadResult` = `Idle | Loading | Success<T>(value) | Error(message: String)` (650–655).

---

## 4. State holder properties (all 41)

All `mutableStateOf` properties are `var … private set` (public read, VM-only write) unless noted.
"Persisted" = value is read from and/or written to a Store.

| # | Line | Property | Type | Initial value | Persisted? | Domain |
|---|---|---|---|---|---|---|
| 1 | 427 | `proxySettings` | `ProxySettings` | `networkConfigStore.loadProxySettings()` | **read+write** `NetworkConfigStore` | Settings/Network |
| 2 | 429 | `proxyEnabled` | `Boolean` | `proxySettings.enabled` | via `proxySettings` | Settings/Network |
| 3 | 431 | `proxyHost` | `String` | `proxySettings.host` | via `proxySettings` | Settings/Network |
| 4 | 433 | `proxyPortText` | `String` | `proxySettings.port.toString()` | via `proxySettings` | Settings/Network |
| 5 | 435 | `authToken` | `String?` | `authSessionStore.loadToken()` | **read+write** `AuthSessionStore` | Session/Auth |
| 6 | 450 | `routes` (**private**, `mutableStateListOf`) | `MutableList<AppRoute>` | `[AppRoute.Home]` | no | Navigation |
| 7 | 469 | `currentTab` | `BottomTab` | `BottomTab.Collection` | no | Navigation |
| 8 | 471 | `forumState` | `ForumState` | `ForumState()` | no | Forum |
| 9 | 473 | `forumPostDetailState` | `ForumPostDetailState` | default | no | Forum |
| 10 | 475 | `forumCreateState` | `ForumCreateState` | default | no | Forum |
| 11 | 477 | `homeState` | `HomeState` | default | no (user comes from JWT/API) | Library |
| 12 | 479 | `profileState` | `ProfileState` | default | no | Profile |
| 13 | 481 | `userProfileDetailState` | `UserProfileDetailState` | default | no | UserProfile |
| 14 | 483 | `adminState` | `AdminState` | default | no | Admin |
| 15 | 485 | `toolsState` | `ToolsState` | default | no | Tools |
| 16 | 487 | `messageCenterState` | `MessageCenterState` | default | no | Messages |
| 17 | 489 | `messageDetailState` | `MessageDetailState` | default | no | Messages |
| 18 | 491 | `messageConversationState` | `MessageConversationState` | default | no | Messages |
| 19 | 493 | `messageSettingsState` | `MessageSettingsState` | default | no | Messages |
| 20 | 495–501 | `workspaceState` | `WorkspaceState` | `WorkspaceState(localApis = workspaceLocalStore.loadApis(), jobs = workspaceLocalStore.loadJobs())` | **read+write** `WorkspaceLocalStore` | Workspace |
| 21 | 502 | `uploadBookState` | `UploadBookState` | default | no | Upload |
| 22 | 504 | `uploadEditorState` | `UploadEditorState` | `UploadEditorState(archives = editorArchiveStore.list())` | **read+write** `EditorArchiveStore`; `aiConfigs` read from `WorkspaceLocalStore` | Editor |
| 23 | 506 | `politicalExamState` | `PoliticalExamState` | default | writes `AuthSessionStore` on pass (2384) | Exam |
| 24 | 508 | `bookshelfQuery` | `String` | `""` | no | Library |
| 25 | 510 | `searchKeyword` | `String` | `searchHistoryStore.loadLastKeyword()` | **read** `SearchHistoryStore` | Search |
| 26 | 512 | `searchHistory` | `List<String>` | `searchHistoryStore.load()` | **read+write** `SearchHistoryStore` | Search |
| 27 | 514 | `searchOptions` | `SearchOptions` | `searchSettingsStore.load().toSearchOptions()` | **read+write** `SearchSettingsStore` | Search |
| 28 | 516 | `searchResults` | `LoadResult<List<NovelCard>>` | `Idle` | no | Search |
| 29 | 518 | `searchTags` | `LoadResult<List<NovelTag>>` | `Idle` | no | Search |
| 30 | 520 | `searchPage` | `Int` | `1` | no | Search (**never read by UI** — VM-internal paging cursor) |
| 31 | 522 | `searchCanLoadMore` | `Boolean` | `false` | no | Search |
| 32 | 524 | `searchLoadingMore` | `Boolean` | `false` | no | Search |
| 33 | 526 | `bookCatalogQuery` | `String` | `""` | no | BookDetail (catalog filter) |
| 34 | 528 | `bookDetailState` | `BookDetailState` | default | reads `ReaderProgressStore` | BookDetail |
| 35 | 530 | `bookEditState` | `BookEditState` | default | no | BookManage |
| 36 | 532 | `bookChapterManagerState` | `BookChapterManagerState` | default | no | ChapterManage |
| 37 | 534 | `readerCatalogQuery` | `String` | `""` | no | Reader |
| 38 | 536 | `readerState` | `ReaderState` | default | no | Reader |
| 39 | 538–544 | `readerUiOptions` | `ReaderUiOptions` | `ReaderUiOptions(fontSizeSp = readerSettingsStore.loadFontSizeSp(), theme = readerSettingsStore.loadTheme())` | **read+write** `ReaderSettingsStore` | Reader |
| 40 | 545 | `readerProgress` | `ReaderProgress?` | `readerProgressStore.load()` | **read+write** `ReaderProgressStore` | Reader/Library |
| 41 | 547 | `recentReaderProgresses` | `List<ReaderProgress>` | `readerProgressStore.loadRecent()` (limit 5) | **read** `ReaderProgressStore` | Library |

Derived: `currentRoute` (550) = `routes.lastOrNull() ?: AppRoute.Home`.

### 4.1 Non-observable VM fields

| Line | Field | Purpose |
|---|---|---|
| 418–425 | `networkConfigStore`, `authSessionStore`, `readerProgressStore`, `readerSettingsStore`, `searchHistoryStore`, `searchSettingsStore`, `workspaceLocalStore`, `editorArchiveStore` | all 8 stores, constructed from `Application` |
| 438–448 | `api: NovalPieApi` | `cookieProvider` = WebView `CookieManager.getInstance().getCookie("https://novalpie.cc")` wrapped in `runCatching`; `authTokenProvider = { authToken }`; `proxySelectorProvider = { proxySettings.toProxySelector(preferEmulatorProxy = shouldPreferEmulatorProxy()) }` |
| 451–466 | 16 `*RequestSerial: Long = 0L` | see §8 |
| 467 | `selectedFavoriteGroupId: Long?` | mirrors `homeState.selectedFavoriteGroupId`; survives `HomeState` re-creation |

### 4.2 `init` (552–556)

```
configureNovalPieImageLoader(application, proxySettings)   // Coil loader with proxy
loadForum()
loadHome()
```
Both Forum **and** Home load at construction even though the start tab is `Collection`/`Home` —
2 concurrent load fan-outs (5 API calls) on cold start.

---

## 5. Public functions by domain

Legend: **M** = state mutated, **A** = API calls, **S** = store writes, **N** = navigation effect,
**G** = staleness guard used.

### 5.1 App shell / session / navigation (12 functions)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 658 | `updateProxyEnabled(value: Boolean)` | M `proxyEnabled` |
| 662 | `updateProxyHost(value: String)` | M `proxyHost` |
| 666 | `updateProxyPort(value: String)` | M `proxyPortText` (digits only, `take(5)`) |
| 670 | `saveProxySettings()` | M `proxySettings`, `proxyEnabled`, `proxyHost`, `proxyPortText`; S `NetworkConfigStore.saveProxySettings`; side effects `configureNovalPieImageLoader(getApplication(), next)` + `loadHome()`. Normalises: blank host → `ProxySettings.DEFAULT_PROXY_HOST` (`10.0.2.2`), port coerced `1..65535` else `7890` |
| 685 | `saveCapturedAuthToken(token: String)` | no-op if blank or unchanged; S `AuthSessionStore.saveToken`; M `authToken`; then `loadHome()`. Called from `WebFallbackScreen` (`ui/NovalPieApp.kt:576`) |
| 693 | `clearAuthToken()` | S `AuthSessionStore.clearToken`; M `authToken=null`, `profileRequestSerial++`, `profileState = ProfileState()`; then `loadHome()` |
| 701 | `openTab(tab: BottomTab)` | maps tab→route (`Collection→Home`, `Discover→Search`, `Tools→Tools`, `Forum→Forum`, `Profile→Profile`). If already on that tab+route: re-runs the tab's loader and returns (pull-to-refresh via tab tap). Otherwise runs the loader **then** `routes.clear(); routes.add(target); currentTab = tab` |
| 731 | `openSettings()` | N push `AppRoute.Settings` via `pushDistinctRoute` |
| 3705 | `openWebFallback(url: String)` | N push `AppRoute.WebFallback(url)` |
| 3709 | `openLoginFallback()` | N `openWebFallback("https://novalpie.cc/login")` |
| 3713 | `openDeepLink(rawUri: String)` | accepts `novalpie://app/...` or `http(s)://novalpie.cc/...`; `/user/{id}` → `openUserProfile`; `/book/{id}[/{chapterId}]` → resets stack to `[Home, BookDetail]` (+`Reader`) and loads both; anything else ignored. M `currentTab = Collection`, `routes` |
| 3744 | `goBack(): Boolean` | pops `routes` if size > 1; returns whether it popped (drives `BackHandler`) |

### 5.2 Library / Home (6)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 558 | `updateBookshelfQuery(value)` | M `bookshelfQuery` (client-side filter, `ui/BookFilter.kt`) |
| 562 | `selectFavoriteGroup(groupId: Long?)` | no-op if unchanged; M `selectedFavoriteGroupId`; then `loadHome()` |
| 3750 | `loadHome()` | M `homeState` = fresh `HomeState(user = JWT profile or Loading, groups/favorites = Loading, favoritesPage=1, selectedFavoriteGroupId)`; A `currentUser()`, `favoriteGroups()`, `favorites(page=1, limit=20, groupId)` (3 parallel `async`); G `homeRequestSerial`; `favoritesCanLoadMore = size == PAGE_SIZE`; `user` resolved via `resolveUserLoadResult` (JWT fallback) |
| 3778 | `loadMoreFavorites()` | requires `favorites is Success` and `favoritesCanLoadMore`; M `favoritesLoadingMore`, `favorites` (merged by `mergeBooksById` → `distinctBy { it.id }`), `favoritesPage`; A `favorites(page=n+1, …)`; G reads `homeRequestSerial` **without incrementing** |
| 3689 | `continueReading(progress: ReaderProgress)` | M `currentTab = Collection`, `routes = [Home, BookDetail(bookId), Reader(bookId, chapterId)]`; triggers `loadBookDetail` + `loadReader` |
| 3699 | `clearReaderProgress()` | S `ReaderProgressStore.clear()` (clears whole prefs file); M `readerProgress = null`, `recentReaderProgresses = emptyList()` |

### 5.3 Search / Discover (14)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 568 | `updateSearchKeyword(value)` | if changed → `invalidateSearchRequests()`; M `searchKeyword` |
| 573 | `useSearchHistory(keyword)` | → `performSearch(keyword)` |
| 577 | `useSearchTag(tagName)` | → `performSearch(tagName)` |
| 581 | `clearSearchHistory()` | S `SearchHistoryStore.clear()`; M `searchHistory = emptyList()` |
| 586/592/598/604/610/616/622 | `updateSearchSortBy` / `SortOrder` / `Scope` / `MatchType` / `AdultFilter` / `Source` / `WordCountRange` | each: if changed → `invalidateSearchRequests()`; M `searchOptions.copy(...)`; S `saveSearchOptions()` → `SearchSettingsStore.save` |
| 3809 | `performSearch(submittedKeyword: String? = null)` | keyword = `searchKeywordForSubmission(searchKeyword, submitted)` (trim); M `searchKeyword`, `searchResults=Loading`, `searchPage=1`, `searchCanLoadMore=false`, `searchLoadingMore=false`; S `SearchHistoryStore.saveKeyword` + M `searchHistory`; A `search(keyword, page=1, limit=20, sortBy, sortOrder, scope, matchType, adultFilter, source, minWordCount, maxWordCount)` where min/max come from `searchMinWordCount`/`searchMaxWordCount` (`ui/ProductCopy.kt:99/102`, splitting on `..`); G `SearchRequestSnapshot` + `isFreshSearchResult` |
| 3856 | `loadMoreSearch()` | requires `searchResults is Success` && `searchCanLoadMore`; A same `search(page = searchPage+1)`; M merges via `mergeBooksById`, `searchPage`, `searchCanLoadMore`, `searchLoadingMore`; G snapshot with **current** serial (no increment) |
| 3909 | `loadSearchTags()` | guard `if (searchTags is LoadResult.Loading) return`; A `tags(sort="count", limit=24)`; M `searchTags`; **no serial** |

### 5.4 Book detail + book comments (11)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 3670 | `openBook(bookId)` | ignores `<=0`; N `pushDistinctRoute(BookDetail)`; returns early if the route is already on top; then `loadBookDetail` |
| 3918 | `loadBookDetail(bookId)` | M `bookDetailState` (fresh, all `Loading`, `readerProgress = readerProgressStore.load(bookId)`); A 5 parallel: `bookDetail`, `bookCoverPhoto`, `chapters`, `bookComments(page=1,limit=20)`, `favoriteStatus`; **two-phase commit**: phase 1 sets `book` + `chapters`, then overrides `book.fullCoverUrl` from `bookCoverPhoto` if non-blank, phase 2 sets `comments` + `favoriteStatus` + re-reads progress; G `isFreshBookDetailResult(currentRoute, state, bookId)` twice (accepts `BookDetail` **or** `Reader` route for the same book) |
| 628 | `updateBookCatalogQuery(value)` | M `bookCatalogQuery` (client filter, `ui/ChapterFilter.kt`) |
| 2949 | `updateBookCommentDraft(value)` | M `bookDetailState.commentDraft` |
| 2953 | `replyToBookComment(comment)` | M `replyingToCommentId/Name`; pre-fills draft with `"@{authorName} "` if draft blank |
| 2963 | `cancelBookCommentReply()` | M clears reply target |
| 2967 | `submitBookComment()` | requires bookId>0, non-blank draft, not `actionLoading`; A `createCommentReply(commentId, content, replyToName)` if replying else `createBookComment(bookId, content)`; M `actionLoading`, clears draft/reply, `actionMessage` = server msg ?: `评论已提交`, failure `apiFailureMessage("评论提交", …)`; then **unconditional** `loadBookDetail(bookId)` |
| 3003 | `likeBookComment(comment)` | A `toggleCommentLike(comment.id)`; label `评论点赞` |
| 3007 | `dislikeBookComment(comment)` | A `reactToCommentOrReply(comment, "down")`; label `评论点踩` |
| 3011 | `emojiBookComment(comment)` | A `reactToCommentOrReply(comment, "emoji:heart")`; label `评论表情` |
| 3015 | `awardBookComment(comment)` | A `reactToCommentOrReply(comment, "award", awardPoints = 10)`; label `评论打赏` |

All four reaction functions route through private `reactOnBookComment` (3019) → sets `actionLoading`,
`actionMessage` = server msg ?: `"$label 已同步"`, then `loadBookDetail(bookId)`.
`reactToCommentOrReply` (3139) picks `reactToCommentReply(parentCommentId, replyId, …)` when
`comment.parentCommentId > 0`, else `reactToComment(commentId, …)`.

### 5.5 Reader (14)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 3679 | `openReader(bookId, chapterId)` | N `replaceTopReaderRoute` — if the top route is already a `Reader`, it is **replaced** (no stack growth while chapter-hopping); then `loadReader` |
| 3958 | `loadReader(bookId, chapterId)` | M `readerState` fresh (all `Loading`); A 3 parallel: `chapterContent(chapterId)`, `chapters(bookId)`, `chapterComments(bookId, chapterId, page=1, limit=20)`; on content success → `saveReaderProgress(bookId, chapterId, chapterTitle-from-catalog)`; G `isFreshReaderResult` |
| 632 | `updateReaderCatalogQuery(value)` | M `readerCatalogQuery` |
| 636 | `increaseReaderFont()` | `+1` coerced to `ReaderSettingsStore.MAX_FONT_SIZE_SP` (28); S `saveFontSizeSp`; M `readerUiOptions` |
| 642 | `decreaseReaderFont()` | `-1` coerced to `MIN_FONT_SIZE_SP` (14); S+M as above |
| 648 | `cycleReaderTheme()` | cycle `system → sepia → dark → system`; S `saveTheme`; M `readerUiOptions` |
| 3043 | `updateReaderCommentDraft(value)` | M `readerState.commentDraft` |
| 3047 | `replyToReaderComment(comment)` | M reply target + `"@name "` prefill |
| 3057 | `cancelReaderCommentReply()` | M clears reply target |
| 3061 | `submitReaderComment()` | A `createCommentReply` or `createChapterComment(bookId, chapterId, content)`; msg ?: `评论已提交`; failure label `章节评论提交`; then `loadReader(bookId, chapterId)` |
| 3098/3102/3106/3110 | `likeReaderComment` / `dislikeReaderComment` / `emojiReaderComment` / `awardReaderComment` | labels `章节评论点赞` / `章节评论点踩` / `章节评论表情` / `章节评论打赏`; via private `reactOnReaderComment` (3114) → reload `loadReader` |

### 5.6 Forum (18)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 2692 | `loadForum()` | M `forumState = ForumState(Loading)`; A `forumPosts(page=1, limit=20)`; G `forumRequestSerial`; **label is mojibake** (see §12.1) |
| 2702 | `openForumPost(postId)` | N `pushDistinctRoute(ForumPostDetail)`; early-return when already top; then `loadForumPostDetail` |
| 2796 | `loadForumPostDetail(postId)` | M `forumPostDetailState` fresh but **preserves** `commentDraft`/`replyingToCommentId`/`replyingToName` when `postId` matches the previous state; A `forumPostDetail(postId)` + `forumPostComments(postId, page=1, limit=20)`; G route equality `currentRoute != AppRoute.ForumPostDetail(postId)`; labels `VisibleUiLabels.ForumPostDetail` (`帖子详情`), `VisibleUiLabels.Comments` (`评论`) |
| 2711 | `openForumCreate()` | if no `authToken` → `openLoginFallback()` and return; M `forumCreateState = ForumCreateState(isAdmin = isAdminProfile(profile), accessMessage = "游客账号不能发帖，请先升级账号" when role == "guest")`; N push `ForumCreate` |
| 2728 | `updateForumCreateDraft(draft)` | M `forumCreateState.draft`, clears `actionMessage` |
| 2732 | `submitForumPost()` | blocked when `submitting` or `accessMessage != null`; validates via `validateForumCreateDraft` (title ≤100, content ≤10000, ≤5 tags, tag ≤20 chars, no dup, poll 2..10 options, `announcement` admin-only); A `createForumPost(ForumCreateRequest(type,title,content,tags,poll?))`; on success `loadForum()`, then replaces `ForumCreate` on the stack with `ForumPostDetail(postId)` + `loadForumPostDetail`, or just pops; M resets `forumCreateState = ForumCreateState(isAdmin)`; G route equality `currentRoute != AppRoute.ForumCreate` |
| 2816 | `updateForumCommentDraft(value)` | M draft |
| 2820 | `replyToForumComment(comment)` | M reply target + `"@name "` prefill |
| 2830 | `cancelForumReply()` | M clears reply target |
| 2834 | `submitForumComment()` | A `createForumComment(postId, content, parentCommentId, replyToName)`; msg ?: `评论已提交`; failure label `VisibleUiLabels.CommentSubmit` (`评论提交`); then `loadForumPostDetail(postId)` |
| 2869/2873/2877/2881 | `likeForumPost` / `dislikeForumPost` / `emojiForumPost` / `awardForumPost` | A `toggleForumPostLike(postId)` / `reactToForumPost(postId,"down")` / `…"emoji:heart"` / `…"award", awardPoints=10`; labels from `forumPostActionLabel`: `点赞`/`点踩`/`表情`/`打赏`; via `reactOnForumPost` (2901) → reload detail |
| 2885/2889/2893/2897 | `likeForumComment(commentId)` / `dislike…` / `emoji…` / `award…` | A `toggleForumCommentLike` / `reactToForumComment(commentId, …)`; labels `评论点赞`/`评论点踩`/`评论表情`/`评论打赏` (`forumCommentActionLabel`); via `reactOnForumComment` (2925) → reload detail |

### 5.7 Own profile (10)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 735 | `loadProfile()` | M `profileState` (`profile` = JWT profile if token decodes else `Loading`, `checkinStats = Loading`); A `currentUser()` + `currentUserCheckinStats()` parallel; on completion builds fresh `ProfileState` with `nameDraft`/`bioDraft`/`showCheckin`(default true)/`autoCheckin`(default false) seeded from the profile; **also writes `homeState.user`**; G `profileRequestSerial` |
| 762 | `updateProfileName(value)` | M `nameDraft`, clears `actionMessage` |
| 766 | `updateProfileBio(value)` | M `bioDraft` |
| 770 | `updateProfileShowCheckin(value)` | M `showCheckin` |
| 774 | `updateProfileAutoCheckin(value)` | M `autoCheckin` |
| 778 | `updateProfileAdultBirthYear(value)` | M `adultBirthYearDraft` = digits only, `take(4)` |
| 785 | `saveProfile()` | blocked when `saving`/`checkingIn`; needs profile (state or `currentUserProfile()`) else `请先登录后再编辑资料`; blank name → `用户名不能为空`; A `updateCurrentUser(updated)` where `updated = profile.copy(name, bio, showCheckin, autoCheckin)`; M `saving`, `profile`, `actionMessage` `正在保存资料…` → `资料已保存` / `apiFailureMessage("保存资料")`; **also writes `homeState.user`**; G `profileRequestSerial++` |
| 827 | `checkinCurrentUser()` | blocked when `checkingIn`/`saving`; no token → `请先登录后再签到`; A `checkinCurrentUser()` then re-fetch `currentUser()` + `currentUserCheckinStats()`; M `profile`, `checkinStats`, `checkingIn`, `actionMessage` (`正在签到…` → server msg ?: `签到成功`/`签到未完成`); writes `homeState.user`; G serial |
| 865 | `verifyCurrentUserAdult()` | blocked when `verifyingAdult`/`saving`/`checkingIn`; no token → `请先登录后再进行成年验证`; birth-year must parse and be in `1900..currentYear` else `请输入有效的出生年份`; A `verifyCurrentUserAdult(birthYear)`; M `profile.copy(isAdult = action.success)`, `verifyingAdult`, msg `正在提交成年验证…` → `成年验证已完成`/`成年验证未通过`; writes `homeState.user`; G serial |
| 901 | `uploadProfileAvatar(rawUri)` | blocked when `uploadingAvatar` or blank uri; reads document via `readUploadDocument`, requires `sizeBytes > 0` (`头像文件为空`) and `mimeType startsWith "image/"` (`请选择图片文件`); A `uploadCurrentUserAvatar(source)` then `currentUser()`; M `profile`, `uploadingAvatar`, msg `正在上传头像…` → `头像已更新`; writes `homeState.user`; G serial |

### 5.8 Other-user profile (4)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 930 | `openUserProfile(userId)` | ignores `<=0`; **if it is the current user** (`currentUserProfile()?.id == userId`) → `currentTab = Profile`, `routes = [Profile]`, `loadProfile()`; else push `UserProfileDetail(userId)` + `loadUserProfile` |
| 944 | `loadUserProfile(userId)` | M `userProfileDetailState` fresh (6 `Loading` slots, keeps `selectedTab`); A 6 parallel: `userProfile`, `userActivities(userId)`, `userNovels(userId)`, `userCheckinStats(userId)`, `userCheckinRecords(userId, "{year}-01-01", "{year}-12-31")`, `userCheckinSettings(userId)`; labels `用户资料`/`用户动态`/`用户作品`/`签到统计`/`签到记录`/`签到设置`; G `userProfileRequestSerial` |
| 988 | `selectUserProfileTab(tab)` | M `selectedTab` |
| 992 | `openUserActivity(activity)` | dispatch: `postId != null` → `openForumPost`; `bookId && chapterId` → `openReader`; `bookId` → `openBook`; else nothing |

### 5.9 Admin (21) — all gated by `isAdminProfile(currentUserProfile())` (`role == "admin"`)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 1001 | `openAdminSection(section)` | silent return if not admin; N push `Admin(section)`; `loadAdminSection(section)` |
| 1007 | `loadAdminSection(section = adminState.section)` | delegates to private `loadAdminSectionInternal(section, null)` |
| 1011 | `updateAdminReviewQuery(query)` | M `reviewQuery` |
| 1015 | `applyAdminReviewQuery()` | `loadAdminSection(Review)` |
| 1019 | `resetAdminReviewQuery()` | M `reviewQuery = AdminReviewQuery()`; reload Review |
| 1024 | `updateAdminOperationLogQuery(query)` | M `operationLogQuery` with `page = page.coerceAtLeast(1)` |
| 1028 | `applyAdminOperationLogQuery()` | reload `OperationLogs` |
| 1032 | `resetAdminOperationLogQuery()` | M reset query; reload |
| 1153 | `toggleAdminReviewSetting(kind: String)` | requires `reviewSettings is Success`; flips `autoApproveUpload` when `kind=="upload"`, `autoApproveDelete` when `"delete"`; A `adminUpdateReviewSettings(upload, delete)`; labels `更新审核设置` / `审核设置已更新` |
| 1162 | `adminReviewAction(requestId, action)` | A `adminReviewAction`; success msg `审核已通过` when `action=="approve"` else `审核已拒绝`; label `处理审核请求` |
| 1168 | `updateAdminKeyStatus(keyId, status)` | A `adminUpdateKeyStatus`; `更新 Key 状态` / `Key 状态已更新` |
| 1174 | `deleteAdminKey(keyId)` | A `adminDeleteKey`; `删除 Key` / `Key 已删除` |
| 1178 | `toggleAdminCookieConfig(config)` | A `adminSaveCookieConfig(config.copy(isActive = !isActive), cookieRaw = null)`; `更新 Cookie 配置` / `Cookie 配置状态已更新` |
| 1184 | `saveAdminCookieConfig(config, cookieRaw)` | A `adminSaveCookieConfig`; `保存 Cookie 配置` / `Cookie 配置已保存` |
| 1190 | `deleteAdminCookieConfig(configId)` | A `adminDeleteCookieConfig`; `删除 Cookie 配置` / `Cookie 配置已删除` |
| 1196 | `setAdminBaseUrlRuleAction(rule, action)` | A `adminSaveBaseUrlRule(rule.copy(action = action))`; `更新 BaseURL 规则` / `BaseURL 规则已更新` |
| 1202 | `saveAdminBaseUrlRule(rule)` | A `adminSaveBaseUrlRule`; `保存 BaseURL 规则` / `BaseURL 规则已保存` |
| 1208 | `deleteAdminBaseUrlRule(ruleId)` | A `adminDeleteBaseUrlRule`; `删除 BaseURL 规则` / `BaseURL 规则已删除` |
| 1214 | `toggleAdminShopItem(item)` | A `adminSaveShopItem(item.copy(isActive = !isActive))`; `更新商品状态` / `商品状态已更新` |
| 1220 | `saveAdminShopItem(item)` | A `adminSaveShopItem`; `保存商品` / `商品已保存` |
| 1224 | `deleteAdminShopItem(itemId)` | A `adminDeleteShopItem`; `删除商品` / `商品已保存`→`商品已删除` |

`loadAdminSectionInternal(section, message)` (1037–1151): sets only the `LoadResult` slots that the
section needs to `Loading`, sets `actionLoading = false`, `actionMessage = message`, then per-section
fetch:
* `Overview` → `adminOverview()` (label `管理总览`)
* `Review` → `adminReviewSettings()` + `adminReviewRequests(type, status, keyword)` (trimmed) (`审核设置`, `审核请求`)
* `Keys` → `adminKeys()` (`Key 管理`)
* `OperationLogs` → `adminOperationLogs(page, action, status, userId, novelId, keyword, startDate, endDate)` all trimmed (`操作日志`)
* `Scraper` → `adminCookieConfigs()` + `adminBaseUrlRules()` + `adminSchedulerLogs()` (`Cookie 配置`, `BaseURL 规则`, `调度日志`)
* `Shop` → `adminShopItems()` (`商店商品`)

`runAdminMutation(label, successMessage, block)` (1228–1251): admin+`!actionLoading` gate → `actionLoading = true`,
`actionMessage = "$label…"` → on success `loadAdminSectionInternal(section, action.message ?: successMessage)`;
on failure `actionMessage = apiFailureMessage(label, failure)`. All admin mutations return
`UserCheckinAction` (reused DTO).

### 5.10 Tools hub (1)

| Line | Function | M / A / G |
|---|---|---|
| 1253 | `loadTools()` | M `toolsState = ToolsState(Loading, Loading)`; A `messageStats()` + `messages(page=1, pageSize = TOOLS_MESSAGE_PREVIEW_LIMIT = 6)`; labels `消息统计` / `消息列表`; G `toolsRequestSerial` |

Tool entry list itself is static presentation (`ui/ToolsPresentation.kt:10 toolsEntries(isAdmin)`), routed
through `openWorkspace` / `openUploadBook` / `openUploadEditor` / `openPoliticalExam` / `openAdminSection`
in `ui/NovalPieApp.kt:272-286`.

### 5.11 Messages (30)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 1272 | `openMessageCenter()` | N push `MessageCenter`; `loadMessageCenter()` |
| 1277 | `loadMessageCenter()` | M `messages=Loading`, `stats=Loading`, `pagination = MessagePagination()`, `selectedIds = emptySet()`, `loadingMore=false`, `actionMessage=null`; A `messagePage(query, page=1, pageSize=20)` + `messageStats()`; label `消息列表` / `消息统计`; G `messageCenterRequestSerial` |
| 1305 | `loadMoreMessages()` | requires `messages is Success` and `pagination.page < pagination.totalPages`; A `messagePage(query, page+1, pagination.pageSize)`; M merge via `mergeMessagePages` (dedupe by id), `pagination`, `loadingMore`; failure label `加载更多消息`; G serial (**incremented**) |
| 1338 | `updateMessageKeyword(value)` | M `query.keyword` |
| 1344 | `applyMessageSearch()` | `= loadMessageCenter()` |
| 1346 | `selectMessageType(type: Int?)` | no-op if unchanged; M `query.messageType`; reload |
| 1352 | `selectMessageReadFilter(isRead: Boolean?)` | no-op if unchanged; M `query.isRead`; reload |
| 1358 | `selectMessagePriority(priority: Int?)` | no-op if unchanged; M `query.priority`; reload |
| 1364 | `toggleMessageSelected(messageId)` | M `selectedIds` via `toggleMessageSelection` |
| 1370 | `selectAllVisibleMessages(select)` | M `selectedIds` = all visible ids or empty |
| 1375 | `markSelectedMessagesRead()` | returns if empty; A `markMessagesRead(ids)`; label `批量已读` |
| 1381 | `deleteSelectedMessages()` | returns if empty; A `deleteMessages(ids)`; label `批量删除` |
| 1387 | `markAllMessagesRead()` | A `markAllMessagesRead()`; label `全部已读` |
| 1391 | `toggleMessageStar(message)` | A `starMessage(id, !isStarred)`; label `取消星标` / `添加星标` |
| 1397 | `openMessage(message)` | if `type == 8` (direct message) and a peer id is resolvable via `directMessageTargetUserId(message, currentUserId)`: fire-and-forget `markMessageRead(id)` when unread, then `openMessageConversation(targetUserId, message.username)`; else `openMessageDetail(id)` |
| 1412 | `openMessageDetail(messageId)` | ignores `<=0`; N push `MessageDetail`; `loadMessageDetail` |
| 1418 | `loadMessageDetail(messageId)` | M `messageDetailState = MessageDetailState(messageId, Loading)`; A `messageDetail(messageId)`; label `消息详情`; G `messageDetailRequestSerial` |
| 1430 | `markCurrentMessageRead()` | requires `detail is Success`, `!isRead`, `!actionLoading`; A `markMessageRead`; success msg `已标记为已读` |
| 1436 | `toggleCurrentMessageStar()` | A `starMessage(id, !isStarred)`; msg `已取消星标` / `已添加星标` |
| 1444 | `deleteCurrentMessage()` | M `actionLoading`; A `deleteMessage(id)`; on success `goBack()` + `loadMessageCenter()`; failure label `删除消息`; **no guard** |
| 1462 | `openCurrentMessageConversation()` | resolves peer from detail; `openMessageConversation` |
| 1469 | `openMessageAction(actionUrl)` | absolutises relative URLs against `https://novalpie.cc/`; path dispatch: `forum`/`posts` + numeric → `openForumPost`; `book/{id}[/{chapterId}]` → `openReader`/`openBook`; otherwise `openWebFallback(absolute)` |
| 1493 | `openMessageConversation(targetUserId, targetName)` | ignores `<=0`; N push `MessageConversation`; `loadMessageConversation` |
| 1500 | `loadMessageConversation(targetUserId, targetName)` | M fresh `MessageConversationState(Loading)`; A `messageConversation(targetUserId)`; label `私信对话`; G `messageConversationRequestSerial` |
| 1516 | `updateMessageDraft(value)` | M `draft` |
| 1520 | `sendMessageDraft()` | requires `currentUserProfile()` with non-null `id`, non-blank trimmed draft, `targetUserId > 0`, `!sending`; A `sendDirectMessage(currentUserId, targetUserId, currentUserName, content)`; M `draft=""`, `sending`, msg server ?: `私信已发送` / `apiFailureMessage("发送私信")`; on success reloads conversation |
| 1555 | `openMessageSettings()` | N push `MessageSettings`; `loadMessageSettings()` |
| 1560 | `loadMessageSettings()` | M `MessageSettingsState(Loading)`; A `messageSettings()`; on success `settings` + `draft` both set; on failure **both** `settings = Error(...)` and `actionMessage` set; label `消息设置`; G `messageSettingsRequestSerial` |
| 1580 | `updateMessageSettingsDraft(transform: (MessageSettings) -> MessageSettings)` | M `draft = transform(draft)` (function-shaped setter — unusual, UI supplies lambdas) |
| 1587 | `saveMessageSettings()` | blocked when `saving`; validated by `validateMessageSettings` (`HH:MM` regex for quiet hours, `autoReadAfterDays >= 0`); A `updateMessageSettings(draft)`; M `settings = Success(draft)`, `saving`, msg server ?: `消息设置已保存` / `apiFailureMessage("保存消息设置")` |

Private runners: `runMessageCenterAction(label, action)` (1614) → sets `actionLoading`, msg
`"{label}已同步"` on success, clears `selectedIds`, then `loadMessageCenter()`;
`runMessageDetailAction(successMessage, action)` (1641) → same shape, then `loadMessageDetail(messageId)`.
Neither uses a serial.

### 5.12 Upload book (7)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 1665 | `openUploadBook()` | if `existingNovelId != null` resets to a clean `UploadBookState()`; N push `UploadBook` |
| 1670 | `updateUploadBookDraft(draft)` | M `draft` with `chapterCount` forced to the parsed chapter count; clears `actionMessage`, resets `submitResult = Idle` |
| 1678 | `selectUploadEpub(rawUri)` | blocked when `processing`; M `processing`, `progressLabel = 正在读取 EPUB 文件…`, `chapters=Loading`, `serverFilePath=null`; validates `.epub` suffix (`仅支持 EPUB 格式文件`) and non-zero size (`EPUB 文件为空`); **branch on `uploadParseMode(size)`**: `SERVER_CHUNKED` (>50 MiB) → label `文件超过 50 MiB，正在按 5 MiB 流式分片上传…`, A `uploadFileInChunks(source)` then `parseUploadedEpub(path)` (keeps `epubFilePath`); `LOCAL` → label `正在本机解析 EPUB 目录与章节…`, `EpubParser.parse(source)` on `Dispatchers.IO`; on success fills draft `title`/`author`/`description` only when blank, always takes `language` when parsed non-blank, `chapters`, `serverFilePath`, msg `EPUB 解析完成，共 N 章`; G `uploadRequestSerial` (checked twice: 1696, 1713) |
| 1744 | `submitUploadBook()` | blocked when `processing`; **append mode** (`existingNovelId != null`) validates only chapters non-empty (`请先选择并解析 EPUB 文件`) + `submitType ∈ {chinese, personal, shared}` (`提交方式无效`); **new-book mode** uses `validateUploadBookDraft` (title/author/chapterCount/submitType); requires `selectedFile` (`请先选择 EPUB 文件`); M `processing`, `progressLabel = 正在安全上传书籍与 N 章内容…`, `submitResult=Loading`; A `appendManagedChapters(bookId, submitType, chapters, epubFilePath, epubFile?)` or `uploadBook(request, epubFile?)` where the raw file is sent only when `serverFilePath == null`; treats `!success` as failure with server message or `上传失败`; msg `章节追加成功` / `上传成功` / `apiFailureMessage("上传书籍")`; **no serial guard** |
| 1827 | `clearUploadBook()` | `uploadRequestSerial++`; M reset preserving `existingNovelId` from `currentRoute as? BookAppend` |
| 1832 | `openUploadedBook(novelId)` | `openBook(novelId)` when > 0 |
| 3659 | `openBookAppend(bookId)` | ignores `<=0`; requires token else `openLoginFallback()`; `uploadRequestSerial++`; M `uploadBookState = UploadBookState(existingNovelId = bookId)`; N push `BookAppend(bookId)` |

`UploadBookRequest` assembled at 1773–1788 maps draft → DTO, with `tags = normalizeUploadTags(tagsText)`
(splits on `,`/`，`/newline, trims, distinct) and `coverUrl = draft.coverUrl.takeIf { isNotBlank() }`.

### 5.13 EPUB editor (31)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 1836 | `openUploadEditor()` | reloads `archives` from `EditorArchiveStore.list()`; `aiConfigs` = `workspaceLocalStore.loadApis()` filtered to those with non-blank `endpoint`+`model`+`apiKey`; keeps `selectedAiConfigId` if still valid else first; N push `UploadEditor` |
| 1851 | `selectEditorTab(tab)` | M `selectedTab` |
| 1855 | `updateEditorText(value)` | M `text` |
| 1859 | `updateEditorEncoding(value)` | M `encoding` |
| 1863 | `updateEditorMetadata(value: EditorBookMetadata)` | M `metadata` |
| 1867 | `updateEditorSplitMode(value)` | M `splitMode` |
| 1871 | `updateEditorSplitPattern(value)` | M `splitPattern` |
| 1875 | `updateEditorSplitTarget(value)` | M `splitTarget` (digits only) |
| 1879 | `updateEditorCustomScript(value)` | M `customScript` |
| 1883 | `updateEditorScriptChunked(value)` | M `scriptChunked` |
| 1887 | `updateEditorScriptChunkSize(value)` | M `scriptChunkSize` (digits only) |
| 1891 | `selectEditorAiConfig(id)` | no-op if id not in `aiConfigs`; M `selectedAiConfigId` |
| 1896 | `generateEditorRegexWithAi()` | blocked when `busy`; needs ≥2 chapters (`请先生成至少两个章节标题`) and a selected config (`请先在工作区保存可用的本地 API 配置`); A `generateEditorRegex(endpoint, apiKey, model, chapterTitles = first 20 titles)` — **direct third-party LLM call**; on success M `splitMode=Regex`, `splitPattern=regex`, `selectedTab=Split`, msg `AI 已生成正则，请检查后再执行分章`; failure label `AI 生成正则` |
| 1938 | `updateEditorFind(value)` | M `findText` |
| 1942 | `updateEditorReplace(value)` | M `replaceText` |
| 1946 | `updateEditorFindUsesRegex(value)` | M `findUsesRegex` |
| 1950 | `updateEditorArchiveName(value)` | M `archiveName` |
| 1954 | `selectEditorDocument(rawUri)` | blocked when `busy`; msg `正在打开文件…`; `.epub` → `EpubParser.parse` + `EditorProcessor.toWebsiteIdentifiers(chapters)` as text + metadata from EPUB; otherwise `readEditorText(document, encoding)`; M `text`, `fileName`, `metadata`, `chapters`, `selectedTab` (`Chapters` if chapters else `Text`), msg `EPUB 已加载，共 N 章` / `文件已加载，请配置分章规则`; failure label `打开编辑文件`; G `editorRequestSerial` |
| 2003 | `processEditorSplit()` | blank text → `请先加载或输入文本`; validated by `editorSplitTargetError`; **`CustomScript` mode does not compute here** — it only sets `busy = true`, `scriptRunId++`, msg `正在本地沙箱执行脚本…` and returns, handing execution to `EditorScriptEngine` in `ui/UploadEditorScreens.kt:118`; other modes call `EditorProcessor.splitByRegex` (multi-line patterns split per line) / `splitByMarkdown(1|2)` / `splitByKeywordNumber` / `splitByCharacterCount(target)` / `splitByParagraphCount(target)`; empty result → `没有匹配到章节标题，请调整规则`; success msg `已生成 N 章`; exception → `分章失败：{message ?: 规则无效}` |
| 2054 | `completeEditorCustomScript(runId, processedText, error)` | ignored unless `scriptRunId == runId && busy`; error/null → `脚本执行失败：{error ?: 未返回文本}`; else `EditorProcessor.parseWebsiteIdentifiers(processedText)`; M `text`, `chapters` (kept if none parsed), `selectedTab`, msg `脚本处理完成，已生成 N 章` or `脚本处理完成；未发现网站章节标识，已保留处理后的文本` |
| 2078 | `replaceEditorText()` | empty find → `请输入查找内容`; regex or literal replace; msg `替换完成` / `未找到匹配项` / `替换失败：{message ?: 正则无效}` |
| 2097 | `updateEditorChapter(index, title, content)` | bounds-checked; blank title → `第 {index+1} 章`; msg `章节已更新` |
| 2104 | `addEditorChapter()` | appends `UploadChapter("第 {n+1} 章", "", n+1)`; msg `已添加章节` |
| 2112 | `deleteEditorChapter(index)` | removes and renumbers `chapterNumber`; msg `章节已删除并重新编号` |
| 2119 | `saveEditorArchive()` | needs text or chapters (`没有可保存的编辑内容`); blocked when `busy`; builds `EditorArchive(id = "archive_{ts}_{random 0..9999}", name = archiveName ?: metadata.title ?: "存档 {ts}", timestamp, textContent, metadata, fileName, chapterCount, totalWords = sum of chapter lengths or text length)`; S `EditorArchiveStore.save` on `Dispatchers.IO`; M `archiveName=""`, `archives` reloaded, msg `存档已保存`; failure label `保存存档` |
| 2156 | `loadEditorArchive(id)` | S read `EditorArchiveStore.load(id)`; missing → `存档不存在`; M `text`, `metadata`, `fileName`, `chapters = emptyList()`, `selectedTab = Text`, msg `存档已加载，请重新生成章节目录` |
| 2171 | `deleteEditorArchive(id)` | S `EditorArchiveStore.delete`; M `archives`, msg `存档已删除` / label `删除存档` |
| 2177 | `clearEditorArchives()` | S `EditorArchiveStore.clear`; M `archives = emptyList()`, msg `所有存档已清空` / label `清空存档` |
| 2183 | `exportEditorEpub(rawUri)` | `validateEditorOutput` (title/author/chapters); writes via `contentResolver.openOutputStream(uri, "w")` + `EpubWriter.write(output, metadata, chapters)` on IO; `无法写入目标文件` if stream null; msg `正在生成 EPUB…` → `EPUB 已生成`; failure label `生成 EPUB` |
| 2206 | `sendEditorToUpload()` | `validateEditorOutput`; writes a temp EPUB into `cacheDir` named `{sanitised title, ≤48 chars, non-blank else "novalpie"}_{ts}.epub`; **cross-domain write**: replaces `uploadBookState` with a fully seeded `UploadBookState` (draft from `metadata`, `selectedFile` pointing at the cache file with `mimeType = application/epub+zip`, `chapters = Success(...)`, msg `编辑器内容已准备好，请核对后确认上传`); target route = topmost `BookAppend` on the stack if any, else `UploadBook`; pops `UploadEditor` before pushing; M `uploadEditorState.actionMessage = 已发送到上传页`; failure label `生成上传文件` |
| 2258 | `clearUploadEditor()` | `editorRequestSerial++`; M fresh `UploadEditorState(archives = store.list(), aiConfigs, selectedAiConfigId)` |

### 5.14 Political exam (9)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 2267 | `openPoliticalExam()` | `refreshPoliticalExamTimer()`; N push `PoliticalExam` |
| 2272 | `startPoliticalExam()` | blocked when `session is Loading` or `submitting`; no token → `请先登录后再开始考试`; M `PoliticalExamState(phase=Landing, session=Loading, actionMessage=正在创建考试会话…)`; A `startPoliticalExam()`; on success builds answer arrays sized to the paper (`singleChoice` nulls, `multipleChoice` empty lists, `trueFalse` nulls, `fillBlank` empty strings), `phase=Active`, `remainingTimeSeconds`, `deadlineEpochMillis = now + remaining*1000`; failure label `开始考试`; **no serial** |
| 2312 | `selectPoliticalExamSingle(index, option)` | bounds-checked; M `answers.singleChoice[index]` |
| 2322 | `togglePoliticalExamMultiple(index, option)` | toggles into a sorted list; M `answers.multipleChoice[index]` |
| 2334 | `selectPoliticalExamTrueFalse(index, answer)` | M `answers.trueFalse[index]` |
| 2344 | `updatePoliticalExamBlank(index, answer)` | M `answers.fillBlank[index]` |
| 2354 | `tickPoliticalExamTimer()` | only when `phase == Active && !submitting`; recomputes remaining from `deadlineEpochMillis`; auto-submits at `<= 0` |
| 2368 | `submitPoliticalExam()` | requires `Active`, `!submitting`, `session is Success`; A `submitPoliticalExam(sessionId, answers)`; M `phase=Result`, `result`, `deadlineEpochMillis=null`, msg `考试通过`/`考试未通过`; **if the result carries a replacement `token`** → S `AuthSessionStore.saveToken`, M `authToken`, then `loadHome()`; failure label `提交考试` |
| 2398 | `resetPoliticalExam()` | M fresh `PoliticalExamState()` |

Timer math lives in private `refreshPoliticalExamTimer` (2360): `ceil((deadline - now)/1000)` coerced `>= 0`.

### 5.15 Workspace (11)

| Line | Function | M / A / S / N / G |
|---|---|---|
| 2479 | `openWorkspace()` | N push `Workspace`; `loadWorkspace()` |
| 2484 | `selectWorkspaceTab(tab)` | M `selectedTab` |
| 2488 | `loadWorkspace()` | M 4 slots `Loading` + reloads `localApis`/`jobs` from `WorkspaceLocalStore`; A `workspaceApiConfigs()`, `workspaceCookieStatus()`, `workspaceCookieConfigs()`, `workspaceHealth()` in parallel; labels `工作区 API 配置` / `Cookie 状态` / `Cookie 配置` / `工作区健康状态`; G `workspaceRequestSerial` |
| 2518 | `saveWorkspaceApi(draft: WorkspaceApiDraft)` | validated by `validateWorkspaceApiDraft` (name/model non-blank, http(s) endpoint, apiKey non-blank, concurrency 1..100); blocked when `actionLoading`; A branch: `shareToServer && serverId != null` → `updateWorkspaceApi`; `shareToServer` → `createWorkspaceApi`; `!shareToServer && serverId != null` → `deleteWorkspaceApi` (un-share); else local-only no-op result; throws on `!success`; S `WorkspaceLocalStore.upsertApi(WorkspaceLocalApiConfig(id = draft.id ?: currentTimeMillis, …, serverId = if shared serverResult.id ?: draft.serverId else null))`; M `localApis`, msg server ?: `API 配置已保存` / `apiFailureMessage("保存 API 配置")`; then `loadWorkspace()` |
| 2581 | `deleteWorkspaceLocalApi(config)` | A `deleteWorkspaceApi(serverId)` when present; S `WorkspaceLocalStore.deleteApi(id)`; M `localApis`, msg `API 配置已删除` / label `删除 API 配置`; then `loadWorkspace()` |
| 2608 | `deleteWorkspaceServerApi(config)` | A `deleteWorkspaceApi(config.id)`; msg `API 配置已删除` |
| 2612 | `saveWorkspaceCookie(draft)` | validated by `validateWorkspaceCookieDraft` (new needs `configKey` + `cookieRaw`; proxy must be `IP:PORT` or `http(s)://…`); A `createWorkspaceCookie(configKey, description, cookieRaw, proxyIp, isActive)` or `updateWorkspaceCookie(id, description, cookieRaw-if-non-blank, proxyIp, isActive)`; msg `Cookie 配置已保存` |
| 2638 | `toggleWorkspaceCookie(config)` | A `setWorkspaceCookieActive(id, !isActive)`; msg `Cookie 状态已更新` |
| 2644 | `deleteWorkspaceCookie(config)` | A `deleteWorkspaceCookie(id)`; msg `Cookie 配置已删除` |
| 2650 | `updateWorkspaceJobStatus(job, status)` | **local only**: S `WorkspaceLocalStore.upsertJob(job.copy(status, updatedAt = currentTimeMillis.toString()))`; M `jobs`, msg `任务状态已更新` |
| 2658 | `deleteWorkspaceJob(job)` | S `WorkspaceLocalStore.deleteJob(id)`; M `jobs`, msg `任务已删除` |

`runWorkspaceAction(successMessage, action)` (2666): `actionLoading` gate → msg server ?: `successMessage`
(the success message doubles as the failure label) → `loadWorkspace()` on success.

### 5.16 Book management (9)

| Line | Function | M / A / N / G |
|---|---|---|
| 3152 | `openBookEditInfo(bookId)` | ignores `<=0`; requires token else `openLoginFallback()`; N push `BookEditInfo(bookId)`; `loadBookEditInfo` |
| 3162 | `loadBookEditInfo(bookId)` | M fresh `BookEditState(bookId, info=Loading, permissions=Loading)`; A `managedBookInfo(bookId)` + `managedBookPermissions(bookId)`; on success `draft = bookEditDraft(info)`; labels `加载书籍信息` / `加载编辑权限`; G `bookEditRequestSerial` **and** route equality |
| 3186 | `updateBookEditDraft(draft)` | M `draft` |
| 3190 | `updateBookAccessPolicyDraft(draft)` | M `accessPolicyDraft` |
| 3194 | `updateBookTransferIdentifier(identifier)` | M `transferIdentifier` |
| 3198 | `saveManagedBookAccessPolicy()` | blocked when any of `saving`/`uploadingCover`/`savingAccessPolicy`/`transferringBook`; validated by `validateBookAccessPolicyDraft` (types `none`/`points_min`/`points_pay`; value >0; max 50 for `points_pay`, 100 for `points_min`); A `updateManagedBookAccessPolicy(bookId, bookAccessPolicyFromDraft(draft))`; msg `正在保存读写门槛…` → server ?: `读写门槛已保存`; failure label `保存读写门槛`; G route equality |
| 3228 | `transferManagedBook()` | same mutual-exclusion gate; blank identifier → `请输入接收方 UID 或用户名`; A `transferManagedBook(bookId, identifier)`; on success clears `transferIdentifier`, msg server ?: `已提交转让给 {targetUsername ?: "UID {id}" ?: identifier}`; failure label `转让书籍` |
| 3261 | `saveManagedBook()` | gate as above; `validateBookEditDraft` (title/author non-blank, no blank/duplicate tags); A `updateManagedBook(bookId, BookEditRequest(...11 fields...))`; message logic: `!success` → server msg ?: joined `errors` ?: `保存失败`; `failedFields` non-empty → `部分信息保存失败：{fields joined ", "}`; else server ?: `书籍信息保存成功`; **on success also calls `loadBookDetail(bookId)`** (cross-domain); failure label `保存书籍信息` |
| 3311 | `uploadManagedBookCover(rawUri)` | requires `permissions.photoUrl == true` plus the mutual-exclusion gate; document must be non-empty (`封面文件为空`) and an image (`请选择图片文件`); A `uploadManagedBookCover(bookId, source)`; M `draft.photoUrl = url`, msg `正在上传原始封面…` → `封面已上传，保存信息后生效`; failure label `上传封面`; G **captures `bookEditRequestSerial` without incrementing** (3323) + route equality |

### 5.17 Chapter management (19)

| Line | Function | M / A / N / G |
|---|---|---|
| 3352 | `openBookChapters(bookId)` | ignores `<=0`; token required else `openLoginFallback()`; N push `BookChapters(bookId)`; `loadManagedChapters` |
| 3362 | `loadManagedChapters(bookId)` | M fresh `BookChapterManagerState(bookId, chapters=Loading)`; A `chapters(bookId)`; label `加载章节管理列表`; G `bookChapterRequestSerial` + route equality |
| 3376 | `toggleManagedChapterSelection(chapterId)` | M `selectedIds` |
| 3382 | `selectAllManagedChapters()` | select-all / clear-all toggle based on size equality |
| 3390 | `moveManagedChapter(chapterId, delta)` | local reorder with `number` renumbering; M `chapters`, `orderDirty = true`, msg `章节顺序尚未保存` |
| 3404 | `saveManagedChapterOrder()` | requires `orderDirty`, `!actionLoading`, non-empty; A `reorderManagedChapters(bookId, ids)`; M `orderDirty=false`, msg `正在保存章节顺序…` → server ?: `章节顺序已更新`; failure label `保存章节顺序`; G route equality |
| 3419 | `openManagedChapterEditor(chapter: Chapter? = null)` | refuses while `orderDirty` (`请先保存章节顺序`); `null` → new-chapter draft with `insertAt = count + 1`; else A `chapterContent(chapter.id)` → `editor = ManagedChapterDraft(chapterId, insertAt = chapter.number ?: 1, title = content.title ?: chapter.title, content)`; msg `正在加载章节正文…`; failure label `加载章节正文`; M `editorLoading` |
| 3455 | `updateManagedChapterDraft(draft)` | M `editor` |
| 3459 | `dismissManagedChapterEditor()` | only when `!actionLoading`; M `editor=null`, `editorLoading=false` |
| 3465 | `openManagedChapterIllustrations(chapter)` | refuses while `orderDirty`; M `illustrationChapter`, `illustrations=Loading`; A `managedChapterIllustrations(chapter.id)`; label `加载章节插图`; G bumps **`bookChapterRequestSerial`** + route equality |
| 3487 | `dismissManagedChapterIllustrations()` | only when not uploading/deleting; M `illustrationChapter=null`, `illustrations=Idle` |
| 3497 | `uploadManagedChapterIllustrations(rawUris: List<String>)` | requires an open `illustrationChapter`, non-empty uris, no in-flight upload/delete; each file must be `1..NovalPieApi.WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES` (`单张插图必须在 20 MiB 以内`) and an image (`请选择图片文件`); A `uploadManagedChapterIllustrations(chapterId, sources with fallback contentType image/jpeg)`; msg `正在上传原始章节插图…` → server ?: `章节插图已上传`, then re-opens the illustration panel (refresh); failure label `上传章节插图` |
| 3535 | `deleteManagedChapterIllustration(imageId)` | M `deletingIllustrationId`; A `deleteManagedChapterIllustration(chapterId, imageId)`; msg `正在删除章节插图…` → server ?: `章节插图已删除` + refresh; failure label `删除章节插图` |
| 3561 | `insertChapterIllustrationPlaceholder(index)` | requires the open editor to be the same chapter as the illustration panel, else `请先打开同一章节的正文编辑器，再插入图片占位符`; appends `chapterIllustrationPlaceholder(index)` = `[[img:N]]` with newline separation; msg `已插入 [[img:N]]` |
| 3577 | `saveManagedChapterDraft()` | `validateManagedChapterDraft` (insertAt ≥1 for new, non-blank title/content); A `insertManagedChapter(bookId, insertAt, title, content)` or `updateManagedChapter(chapterId, title, content)`; msg `正在保存章节…` → server ?: `章节已保存`; then `loadManagedChapters`; failure label `保存章节`; G route equality |
| 3605 | `deleteManagedChapter(chapterId)` | refuses while `orderDirty`/`actionLoading`; A `deleteManagedChapter`; label `删除章节` |
| 3611 | `batchDeleteManagedChapters()` | needs selection; A `batchDeleteManagedChapters(bookId, selectedIds)`; label `批量删除章节` |
| 3619 | `updateManagedTranslationMode(mode)` | accepts only `personal`/`shared`; M `translationMode` |
| 3625 | `translateSelectedManagedChapters()` | needs selection; A `requestManagedChapterTranslation(bookId, selectedIds, translationMode)`; label `提交章节翻译`; `refresh = false` so the selection is **kept** and no reload happens |

`runManagedChapterMutation(state, label, refresh = true, action)` (3633): msg `"{label}…"` → server ?:
`"{label} 已完成"`, clears `selectedIds` when `refresh`, reloads chapters when `refresh`; guard is route
equality only.

### 5.18 Private members (23)

| Line | Member | Role |
|---|---|---|
| 1037 | `loadAdminSectionInternal(section, message)` | admin fetch fan-out (§5.9) |
| 1228 | `runAdminMutation(label, successMessage, block)` | admin mutation wrapper |
| 1614 | `runMessageCenterAction(label, action)` | message-center mutation wrapper |
| 1641 | `runMessageDetailAction(successMessage, action)` | message-detail mutation wrapper |
| 1662 | `currentUserProfile(): UserProfile?` | `homeState.user` Success value **else** `decodeAuthTokenProfile(authToken)` — the single source of identity/role for admin gating, forum-create gating, DM peer resolution, self-profile shortcut |
| 2360 | `refreshPoliticalExamTimer()` | recompute remaining seconds |
| 2402 | `validateEditorOutput(state)` | `请填写书名` / `请填写作者` / `请先生成章节目录` |
| 2409 | `readEditorText(document, encoding)` | charset-aware streamed read, 16 KiB buffer, hard cap 50 000 000 chars (`文本超过 5000 万字符，请先分割文件`), unsupported charset → `不支持的编码：{encoding}` |
| 2424 | `readUploadDocument(rawUri)` | takes persistable URI permission, resolves `DISPLAY_NAME`/`SIZE` via `ContentResolver.query`, falls back to `openAssetFileDescriptor().length`, default name `book.epub` |
| 2451 | `uploadSource(document, fallbackContentType = "application/epub+zip")` | builds `UploadFileSource`; `file://` URIs go through `FileInputStream`, others via `contentResolver.openInputStream` (`本地文件路径无效` / `无法读取所选文件`) |
| 2666 | `runWorkspaceAction(successMessage, action)` | workspace mutation wrapper |
| 2901 | `reactOnForumPost(label, action)` | forum post reaction wrapper |
| 2925 | `reactOnForumComment(commentId, label, action)` | forum comment reaction wrapper |
| 3019 | `reactOnBookComment(comment, label, action)` | book comment reaction wrapper |
| 3114 | `reactOnReaderComment(comment, label, action)` | chapter comment reaction wrapper |
| 3139 | `reactToCommentOrReply(comment, reactionType, awardPoints?)` | reply-vs-top-level API selection |
| 3633 | `runManagedChapterMutation(state, label, refresh, action)` | chapter mutation wrapper |
| 3990 | `saveReaderProgress(bookId, chapterId, chapterTitle)` | S `ReaderProgressStore.save`; M `readerProgress`, `recentReaderProgresses`, and `bookDetailState.readerProgress` when the book matches |
| 3999 | `saveSearchOptions()` | S `SearchSettingsStore.save(searchOptions.toPersistedSearchSettings())` |
| 4003 | `invalidateSearchRequests()` | `searchRequestSerial += 1`, `searchCanLoadMore = false`, `searchLoadingMore = false` |
| 4009 | `mergeBooksById(current, next)` | `(current + next).distinctBy { it.id }` |
| 4014 | `MutableList<AppRoute>.replaceWith(next)` | no-op when equal, else `clear()` + `addAll()` |
| 4020 | `Result<T>.toLoadResult(label)` | `Success` / `Error(apiFailureMessage(label, throwable))` |

Top-level: `resolveUserLoadResult(remote, tokenProfile)` (4032) — remote success wins; on failure falls
back to the JWT profile, else `Error(apiFailureMessage("登录状态", failure))`.
`PersistedSearchSettings.toSearchOptions()` (4043) / `SearchOptions.toPersistedSearchSettings()` (4054).

---

## 6. The request-serial / staleness mechanism

### 6.1 Primitives (`ui/RequestFreshness.kt`, 51 lines)

```kotlin
isFreshRequestSerial(requestSerial: Long, activeSerial: Long): Boolean = requestSerial == activeSerial   // :35
isFreshBookDetailResult(route, state: BookDetailState, requestedBookId): Boolean                          // :3
isFreshReaderResult(route, state: ReaderState, requestedBookId, requestedChapterId): Boolean              // :16
data class SearchRequestSnapshot(serial, keyword, options, page)                                          // :28
isFreshSearchResult(request, activeSerial, currentKeyword, currentOptions, expectedPage): Boolean          // :38
searchKeywordForSubmission(currentKeyword, submittedKeyword): String = (submitted ?: current).trim()       // :50
```

* `isFreshBookDetailResult` — `state.bookId == requestedBookId` **and** the current route is
  `BookDetail(id)` **or** `Reader(bookId = id, …)`. The `Reader` allowance is deliberate: opening a
  chapter from the detail page keeps the detail load valid.
* `isFreshReaderResult` — requires both `bookId` and `chapterId` to match state *and* route.
* `isFreshSearchResult` — serial equal **and** keyword equal **and** the whole `SearchOptions` value
  equal **and** page equal. Strongest guard in the codebase.

### 6.2 The three coexisting staleness strategies

| Strategy | How | Where |
|---|---|---|
| **A. Monotonic serial** | `val requestSerial = ++xRequestSerial` before dispatch; after `await`, `if (!isFreshRequestSerial(requestSerial, xRequestSerial)) return@launch` | 14 of 16 serials |
| **B. Route identity** | `if (currentRoute != AppRoute.X(id)) return@launch` (works because routes are data classes) | 15 call sites, mostly book/chapter management + forum detail |
| **C. State+route identity** | `isFreshBookDetailResult` / `isFreshReaderResult` / `isFreshSearchResult` | book detail (×2 phases), reader, search (×2) |

A single flow may use two of them (e.g. `loadBookEditInfo` uses A **and** B; `loadManagedChapters` uses
A **and** B).

### 6.3 The 16 serials — who increments, who guards, who is unguarded

| Serial | Declared | Incremented by | Guarded reads | Loads/mutations in the same domain that do **NOT** guard |
|---|---|---|---|---|
| `forumRequestSerial` | 451 | `loadForum` 2693 | 2697 | `loadForumPostDetail` (uses B), `submitForumPost` (uses B), all forum reactions (none) |
| `bookEditRequestSerial` | 452 | `loadBookEditInfo` 3164 | 3175, and `uploadManagedBookCover` 3332 (**captures without incrementing**, 3323) | `saveManagedBook`, `saveManagedBookAccessPolicy`, `transferManagedBook` (B only) |
| `bookChapterRequestSerial` | 453 | `loadManagedChapters` 3364, `openManagedChapterIllustrations` 3471 | 3368, 3479 | all chapter mutations (B only) |
| `homeRequestSerial` | 454 | `loadHome` 3751 | 3766, and `loadMoreFavorites` 3788 (**captures without incrementing**, 3783 — correct for append) | — |
| `profileRequestSerial` | 455 | `loadProfile` 736, `saveProfile` 804, `checkinCurrentUser` 833, `verifyCurrentUserAdult` 876, `uploadProfileAvatar` 903, `clearAuthToken` 696 (bump only, to kill in-flight loads) | 748, 809/818, 842/856, 881/892, 913 | — (best-covered domain) |
| `userProfileRequestSerial` | 456 | `loadUserProfile` 946 | 975 | — |
| `adminRequestSerial` | 457 | `loadAdminSectionInternal` 1039, `runAdminMutation` 1235 | 1087, 1103, 1111, 1127, 1137, 1146, 1240, 1244 | — (note double-increment: mutation bumps, then its success path calls `loadAdminSectionInternal`, which bumps again) |
| `toolsRequestSerial` | 458 | `loadTools` 1254 | 1264 | — |
| `messageCenterRequestSerial` | 459 | `loadMessageCenter` 1278, `loadMoreMessages` 1309 | 1293, 1319 | `runMessageCenterAction` (nothing) |
| `messageDetailRequestSerial` | 460 | `loadMessageDetail` 1419 | 1423 | `runMessageDetailAction`, `deleteCurrentMessage` (nothing) |
| `messageConversationRequestSerial` | 461 | `loadMessageConversation` 1501 | 1509 | `sendMessageDraft` (nothing) |
| `messageSettingsRequestSerial` | 462 | `loadMessageSettings` 1561 | 1565 | `saveMessageSettings` (nothing) |
| `workspaceRequestSerial` | 463 | `loadWorkspace` 2489 | 2508 | every workspace mutation (nothing) |
| `uploadRequestSerial` | 464 | `selectUploadEpub` 1680, `clearUploadBook` 1828 (bump only), `openBookAppend` 3665 (bump only) | 1696, 1713 | `submitUploadBook` (nothing) |
| `editorRequestSerial` | 465 | `selectEditorDocument` 1956, `clearUploadEditor` 2259 (bump only) | 1983 | `generateEditorRegexWithAi`, `saveEditorArchive`, `exportEditorEpub`, `sendEditorToUpload` (nothing; `completeEditorCustomScript` uses `scriptRunId` instead) |
| `searchRequestSerial` | 466 | `performSearch` 3812, `invalidateSearchRequests` 4004 (bump on keyword/option change) | 3843 (`isFreshSearchResult`), and `loadMoreSearch` 3864 (captures without incrementing) | `loadSearchTags` (guards on `searchTags is Loading` instead) |

### 6.4 Flows with **no** staleness protection at all

`loadSearchTags` (3909, uses a Loading-flag mutex), `startPoliticalExam`, `submitPoliticalExam`,
`deleteCurrentMessage`, `sendMessageDraft`, `saveMessageSettings`, all `runMessageCenterAction` /
`runMessageDetailAction` / `runWorkspaceAction` mutations, `submitUploadBook`, all forum reaction
wrappers (`reactOnForumPost`, `reactOnForumComment`), `reactOnBookComment`, `reactOnReaderComment`,
`submitBookComment`, `submitReaderComment`, `submitForumComment` (each simply reloads its surface
afterwards, unconditionally).

### 6.5 Secondary "run id" mechanism

`UploadEditorState.scriptRunId: Long` (303) is a *different* freshness token: `processEditorSplit`
increments it (2023) and `ui/UploadEditorScreens.kt:118` runs the JS sandbox in a
`LaunchedEffect(state.scriptRunId)`; `completeEditorCustomScript(runId, …)` (2054) drops results whose
`runId` no longer matches. This is the only bidirectional VM↔UI handshake in the app.

### 6.6 Test coverage of the mechanism

`app/src/test/java/com/novalpie/nativeapp/ui/RequestFreshnessTest.kt` exercises the helpers directly, so
the helper signatures are a de-facto public contract for the refactor.

---

## 7. Persistence layer — every `data/*Store.kt`

| Store | File | Backing | Keys / format |
|---|---|---|---|
| `NetworkConfigStore` | `data/NetworkConfigStore.kt:85` | SharedPreferences **`novalpie_native_network`** | `proxy_enabled: Boolean`, `proxy_host: String` (default `10.0.2.2`), `proxy_port: Int` (default `7890`, coerced `1..65535` on save), `proxy_user_configured: Boolean`. **Migration rule (88–109):** if `!proxy_user_configured` and the saved values are exactly the enabled-default triple, load returns `enabled = false` (rescues users from a legacy default-on proxy). `saveProxySettings` always sets `proxy_user_configured = true`. |
| `AuthSessionStore` | `data/AuthSessionStore.kt:43` | SharedPreferences **`novalpie_native_auth`** | `auth_token: String` (trimmed, blank rejected). `loadToken` returns null for blank. Same file also hosts `decodeAuthTokenProfile(token, now)` (:8) — base64-decodes JWT segment 1, honours `exp`, reads `sub`/`data.user_id`/`data.id`, `data.username`/`name`, `role` (default `"user"`), name fallback `"Logged user"`. |
| `ReaderProgressStore` | `data/ReaderProgressStore.kt:6` | SharedPreferences **`novalpie_native_reader_progress`** | Global "last read": `book_id: Long`, `chapter_id: Long`, `chapter_title: String?`, `updated_at: Long`. Per-book: `book_{bookId}_chapter_id`, `book_{bookId}_chapter_title`, `book_{bookId}_updated_at`. Recents: `recent_book_ids` = comma-joined ids, most-recent-first, capped at `MAX_RECENT_BOOKS = 20`; `loadRecent(limit = 5)`. `loadLegacyProgress` (:56) still reads the pre-per-book globals. `clear()` wipes the entire prefs file. |
| `ReaderSettingsStore` | `data/ReaderSettingsStore.kt:5` | SharedPreferences **`novalpie_native_reader_settings`** | `font_size_sp: Int` (`MIN 14`, `MAX 28`, default `18`, coerced on both read and write), `theme: String` ∈ `{system, sepia, dark}`, default `system`, invalid values snap back to default. |
| `SearchHistoryStore` | `data/SearchHistoryStore.kt:5` | SharedPreferences **`novalpie_native_search_history`** | `keywords: String` = newline-joined list, trimmed, blank-filtered, `distinct()`, capped at `MAX_HISTORY = 10`, most recent first. `loadLastKeyword()` = first entry or `""`. `clear()` wipes the file. |
| `SearchSettingsStore` | `data/SearchSettingsStore.kt:15` | SharedPreferences **`novalpie_native_search_settings`** | `sort_by`, `sort_order`, `scope`, `match_type`, `adult_filter` (blank falls back to defaults `relevance`/`desc`/`all`/`ai`/`all`), `source`, `word_count_range` (blank allowed). Data holder `PersistedSearchSettings` (:5). |
| `WorkspaceLocalStore` | `data/WorkspaceLocalStore.kt:9` | SharedPreferences **`novalpie_native_workspace`** | `api_configs: String` = JSON array of `{id, name, model, endpoint, api_key, concurrency (default 10), shared_to_server, server_id?}`; `translation_jobs: String` = JSON array of `{id, book_id, book_title, translator_name, translator_id?, chapter_count, completed_chapters, status (default "pending"), created_at?, updated_at?}`. Upserts are keyed by `id` into a `LinkedHashMap` (insertion order preserved). Rows without `id` (or jobs without `book_id`) are silently dropped on read. `clearAll()` removes both keys. **API keys are stored in plaintext prefs.** |
| `EditorArchiveStore` | `data/EditorArchiveStore.kt:10` | **Files**, not prefs: `context.filesDir/epub-editor-archives/` | Per archive: `{safeId}.json` (metadata) + `{safeId}.txt` (full text, UTF-8). `safeId` = id with `[^A-Za-z0-9._-]` → `_`. Writes are atomic-ish: write `.tmp`, delete target, rename (`无法替换现有存档信息` / `无法替换现有存档正文` / `保存存档失败` on failure). `list()` reads only `.json` (so `textContent` is `""` in list results) and sorts by `timestamp` descending. `load(id)` merges the `.txt`. `clear()` deletes `.json`/`.txt`/`.tmp` in the directory. JSON metadata shape: `{id, name, timestamp, fileName, chapterCount, totalWords, metadata:{title, author, description, language, tags, isAdult, source, sourceUrl}}`; `name` default on read `存档`, `language` default `zh`. |
| *(implicit)* WebView cookie jar | `NovalPieViewModel.kt:440` | Android `CookieManager` | `getCookie("https://novalpie.cc")` is read on every API call as the cookie header source. Not a Store class, but it *is* persistent session state that survives process death and is populated by the `WebFallback` WebView. |

No Room/DataStore/Proto anywhere. Every store is synchronous and called on the **main thread** from the
VM (only `editorArchiveStore.save` is wrapped in `withContext(Dispatchers.IO)` at 2140).

---

## 8. Error-handling pattern

### 8.1 The pipeline

```
suspend api.foo()                      // throws IOException("NovalPie API 404 …") etc.
  └─ runCatching { … }                 // Result<T>
       ├─ .toLoadResult(label)         // VM:4020  → LoadResult.Success | LoadResult.Error(message)
       ├─ .fold(onSuccess, onFailure)  // for mutations: writes actionMessage
       └─ resolveUserLoadResult(...)   // VM:4032  → JWT-profile fallback for the user slot
```

`LoadResult` (`model/Models.kt:650`) has exactly four states: `Idle`, `Loading`, `Success<T>(value)`,
`Error(message: String)`. **The error payload is a pre-rendered user-facing string** — no error codes,
no throwable retained. This is the single most important constraint for the refactor: any new layering
must still produce the same strings at the same moments.

### 8.2 `apiFailureMessage` (`ui/ApiMessages.kt`, 14 lines)

```kotlin
fun apiFailureMessage(label: String, throwable: Throwable): String {
    val detail = throwable.message?.takeIf { it.isNotBlank() } ?: throwable.javaClass.simpleName
    return "${visibleFailureLabel(label)}请求失败: ${visibleFailureDetail(detail)}"
}
```
* `visibleFailureLabel` — `label.trim().removeSuffix("/API")`, blank → `请求`.
* `visibleFailureDetail` — if the detail matches `NovalPie API (\d+)`, it is replaced with
  `服务返回错误 {code}`; otherwise the raw exception message (or class simple name) is shown.
* Output shape: `"{label}请求失败: {detail}"` — e.g. `书架请求失败: 服务返回错误 502`.
* Covered by `app/src/test/java/com/novalpie/nativeapp/ui/ApiFailureMessageTest.kt`.

### 8.3 `ErrorRecovery` / retry affordance (`ui/ErrorRecovery.kt`, 6 lines)

`retryActionLabel(surface)` → `"重试{surface}"`, or plain `重试` when blank. Used at 9 UI sites with
surfaces `书架` (NovalPieApp:1285), `搜索` (:1429), `书籍详情` (:1759), `章节目录` (:1778, :1968),
`正文` (:1864), `章节评论` (:2099), `评论区` (:3258), and the bare fallback in `ErrorBlock` (:3590).
Every `ErrorBlock` pairs `LoadResult.Error.message` with an `onRetry` that calls the domain's `load*`.

### 8.4 `VisibleUiLabels` (`ui/VisibleUiLabels.kt`, 30 lines)

Nine shared labels: `ForumPostDetail = 帖子详情`, `Comments = 评论`, `CommentSubmit = 评论提交`,
`FavoriteGroups = 收藏分组`, `Bookshelf = 书架`, `Search = 搜索`, `BookDetail = 书籍详情`,
`ChapterCatalog = 章节目录`, `ChapterComments = 章节评论`.
Plus `forumPostActionLabel(action)` → `点赞`/`点踩`/`表情`/`打赏`,
`forumCommentActionLabel(action)` = `"评论" + …`, and `enum ForumPostAction { Like, Dislike, Emoji, Award }`.
Covered by `VisibleUiLabelsTest.kt`.

**Inconsistency to preserve or deliberately normalise:** only 9 of ~120 failure labels come from
`VisibleUiLabels`; the rest are inline literals (some UTF-8, some `\uXXXX`). E.g. the book-detail comment
load uses the inline `"评论区"` (3951) while the reader uses `VisibleUiLabels.ChapterComments` (3985).

### 8.5 Transient feedback (`actionMessage`)

15 state classes carry a nullable `actionMessage`. Conventions observed:
* Optimistic in-flight text ends with the ellipsis character `…`: `正在保存资料…`, `正在签到…`, `正在保存章节…`.
* Success prefers the **server** message: `it.message ?: "本地兜底文案"`. This means the exact success
  string is often server-controlled — the local fallback is the only thing the refactor can guarantee.
* Failure always goes through `apiFailureMessage(label, throwable)`.
* Several wrappers reuse the *success message* as the failure label (e.g. `runWorkspaceAction`,
  `runMessageDetailAction`), producing strings like `Cookie 配置已保存请求失败: …`. This is existing
  behaviour, not a typo in this document.
* Most `update*` setters clear `actionMessage` as a side effect (`actionMessage = null`), so typing in a
  field dismisses the last toast.

### 8.6 Auth-degradation pattern

`resolveUserLoadResult` (4032) plus `currentUserProfile()` (1662) implement offline/failed-auth
degradation: if `currentUser()` fails but a JWT is present and unexpired, the app shows the JWT-derived
`UserProfile(id, name, role)` as `Success` instead of an error. Consequences: admin gating
(`isAdminProfile`) and forum-create gating can succeed purely from a locally-decoded token.

---

## 9. Verbatim Chinese UI strings owned by the ViewModel

258 CJK string literals live in this file (extracted and `\uXXXX`-decoded). Complete list, grouped by
purpose. Format: `line — string`.

### 9.1 Tab titles (BottomTab)
`97 — 收藏` · `98 — 搜索` · `99 — 工具` · `100 — 论坛` · `101 — 我的`

### 9.2 `LoadResult` failure labels (feed into `"{label}请求失败: …"`)
`747/847/981 — 签到统计` · `978 — 用户资料` · `979 — 用户动态` · `980 — 用户作品` · `982 — 签到记录` ·
`983 — 签到设置` · `1088 — 管理总览` · `1105 — 审核设置` · `1106 — 审核请求` · `1112 — Key 管理` ·
`1128 — 操作日志` · `1139 — Cookie 配置` · `1140 — BaseURL 规则` · `1141 — 调度日志` · `1147 — 商店商品` ·
`1266 — 消息统计` · `1267/1297 — 消息列表` · `1300 — 消息统计` · `1331 — 加载更多消息` ·
`1425 — 消息详情` · `1511 — 私信对话` · `1572/1573 — 消息设置` · `2510 — 工作区 API 配置` ·
`2511 — Cookie 状态` · `2512 — Cookie 配置` · `2513 — 工作区健康状态` · `2698 — 璁哄潧` **(mojibake, should be 论坛)** ·
`3179 — 加载书籍信息` · `3180 — 加载编辑权限` · `3371 — 加载章节管理列表` · `3450 — 加载章节正文` ·
`3482 — 加载章节插图` · `3914 — 鏍囩` **(mojibake, should be 标签)** · `3951 — 评论区` · `3952 — 收藏状态` ·
`3983 — 阅读器正文` · `3984 — 阅读器目录` · `4039 — 登录状态`

### 9.3 Validation / precondition messages
`790 — 请先登录后再编辑资料` · `795 — 用户名不能为空` · `830 — 请先登录后再签到` ·
`868 — 请先登录后再进行成年验证` · `873 — 请输入有效的出生年份` · `908 — 头像文件为空` ·
`909/3329/3509 — 请选择图片文件` · `1693 — 仅支持 EPUB 格式文件` · `1695 — EPUB 文件为空` ·
`1751 — 请先选择并解析 EPUB 文件` · `1752 — 提交方式无效` · `1763 — 请先选择 EPUB 文件` ·
`1900 — 请先生成至少两个章节标题` · `1905 — 请先在工作区保存可用的本地 API 配置` ·
`2006 — 请先加载或输入文本` · `2047 — 没有匹配到章节标题，请调整规则` · `2081 — 请输入查找内容` ·
`2122 — 没有可保存的编辑内容` · `2158 — 存档不存在` · `2275 — 请先登录后再开始考试` ·
`2403 — 请填写书名` · `2404 — 请填写作者` · `2405 — 请先生成章节目录` · `2410 — 不支持的编码：$encoding` ·
`2418 — 文本超过 5000 万字符，请先分割文件` · `2463 — 本地文件路径无效` · `2466 — 无法读取所选文件` ·
`2196 — 无法写入目标文件` · `2720 — 游客账号不能发帖，请先升级账号` · `3233 — 请输入接收方 UID 或用户名` ·
`3328 — 封面文件为空` · `3400 — 章节顺序尚未保存` · `3422/3468 — 请先保存章节顺序` ·
`3507 — 单张插图必须在 20 MiB 以内` · `3566 — 请先打开同一章节的正文编辑器，再插入图片占位符`

### 9.4 In-flight progress messages
`805 — 正在保存资料…` · `834 — 正在签到…` · `877 — 正在提交成年验证…` · `904 — 正在上传头像…` ·
`1683 — 正在读取 EPUB 文件…` · `1700 — 文件超过 50 MiB，正在按 5 MiB 流式分片上传…` ·
`1702 — 正在本机解析 EPUB 目录与章节…` · `1768 — 正在安全上传书籍与 ${chapters.size} 章内容…` ·
`1908 — 正在生成章节正则…` · `1957 — 正在打开文件…` · `2024 — 正在本地沙箱执行脚本…` ·
`2126 — 正在保存存档…` · `2189 — 正在生成 EPUB…` · `2212 — 正在生成上传文件…` ·
`2281 — 正在创建考试会话…` · `2372 — 正在提交考试…` · `3205 — 正在保存读写门槛…` ·
`3236 — 正在提交书籍转让…` · `3270 — 正在保存书籍信息…` · `3324 — 正在上传原始封面…` ·
`3408 — 正在保存章节顺序…` · `3433 — 正在加载章节正文…` · `3501 — 正在上传原始章节插图…` ·
`3539 — 正在删除章节插图…` · `3585 — 正在保存章节…`

### 9.5 Success / outcome messages (local fallbacks; server `message` wins when present)
`813 — 资料已保存` · `849 — 签到成功` / `签到未完成` · `887 — 成年验证已完成` / `成年验证未通过` ·
`918 — 头像已更新` · `1157 — 审核设置已更新` · `1163 — 审核已通过` / `审核已拒绝` ·
`1169 — Key 状态已更新` · `1175 — Key 已删除` · `1179 — Cookie 配置状态已更新` ·
`1185 — Cookie 配置已保存` · `1191 — Cookie 配置已删除` · `1197 — BaseURL 规则已更新` ·
`1203 — BaseURL 规则已保存` · `1209 — BaseURL 规则已删除` · `1215 — 商品状态已更新` ·
`1221 — 商品已保存` · `1225 — 商品已删除` · `1433 — 已标记为已读` · `1439 — 已取消星标` / `已添加星标` ·
`1541 — 私信已发送` · `1601 — 消息设置已保存` · `1626 — "$label已同步"` (message-center batch actions) ·
`1729 — EPUB 解析完成，共 ${parsed.chapters.size} 章` · `1812 — 章节追加成功` / `上传成功` ·
`1925 — AI 已生成正则，请检查后再执行分章` · `1993 — 文件已加载，请配置分章规则` /
`EPUB 已加载，共 ${loaded.chapters.size} 章` · `2048 — 已生成 ${chapters.size} 章` ·
`2071 — 脚本处理完成；未发现网站章节标识，已保留处理后的文本` ·
`2073 — 脚本处理完成，已生成 ${chapters.size} 章` · `2091 — 替换完成` / `未找到匹配项` ·
`2101 — 章节已更新` · `2108 — 已添加章节` · `2116 — 章节已删除并重新编号` · `2148 — 存档已保存` ·
`2167 — 存档已加载，请重新生成章节目录` · `2173 — 存档已删除` · `2179 — 所有存档已清空` ·
`2200 — EPUB 已生成` · `2244 — 编辑器内容已准备好，请核对后确认上传` · `2246 — 已发送到上传页` ·
`2381 — 考试通过` / `考试未通过` · `2567 — API 配置已保存` · `2594/2609 — API 配置已删除` ·
`2617 — Cookie 配置已保存` · `2639 — Cookie 状态已更新` · `2645 — Cookie 配置已删除` ·
`2654 — 任务状态已更新` · `2662 — 任务已删除` · `2855/2989/3084 — 评论已提交` ·
`2911/2935/3029/3125 — "$label 已同步"` · `3215 — 读写门槛已保存` · `3248 — 已提交转让给 $target` ·
`3295 — 部分信息保存失败：${saved.failedFields.joinToString(", ")}` · `3296 — 书籍信息保存成功` ·
`3339 — 封面已上传，保存信息后生效` · `3413 — 章节顺序已更新` · `3521 — 章节插图已上传` ·
`3547 — 章节插图已删除` · `3573 — 已插入 $placeholder` · `3597 — 章节已保存` · `3648 — "$label 已完成"`

### 9.6 Failure labels for mutations (become `"{label}请求失败: …"`)
`821 — 保存资料` · `859 — 签到` · `895 — 成年验证` · `924 — 上传头像` · `1157 — 更新审核设置` ·
`1163 — 处理审核请求` · `1169 — 更新 Key 状态` · `1175 — 删除 Key` · `1179 — 更新 Cookie 配置` ·
`1185 — 保存 Cookie 配置` · `1191 — 删除 Cookie 配置` · `1197 — 更新 BaseURL 规则` ·
`1203 — 保存 BaseURL 规则` · `1209 — 删除 BaseURL 规则` · `1215 — 更新商品状态` · `1221 — 保存商品` ·
`1225 — 删除商品` · `1378 — 批量已读` · `1384 — 批量删除` · `1388 — 全部已读` ·
`1392 — 取消星标` / `添加星标` · `1456 — 删除消息` · `1547 — 发送私信` · `1607 — 保存消息设置` ·
`1734/1737 — 解析 EPUB` · `1819/1820 — 上传书籍` · `1931 — AI 生成正则` · `1997 — 打开编辑文件` ·
`2151 — 保存存档` · `2174 — 删除存档` · `2180 — 清空存档` · `2201 — 生成 EPUB` · `2253 — 生成上传文件` ·
`2304/2305 — 开始考试` · `2391/2392 — 提交考试` · `2573 — 保存 API 配置` · `2600 — 删除 API 配置` ·
`2789 — 发布帖子` · `2995 — 评论提交` · `3004 — 评论点赞` · `3008 — 评论点踩` · `3012 — 评论表情` ·
`3016 — 评论打赏` · `3090 — 章节评论提交` · `3099 — 章节评论点赞` · `3103 — 章节评论点踩` ·
`3107 — 章节评论表情` · `3111 — 章节评论打赏` · `3221 — 保存读写门槛` · `3254 — 转让书籍` ·
`3304 — 保存书籍信息` · `3345 — 上传封面` · `3414 — 保存章节顺序` · `3528 — 上传章节插图` ·
`3554 — 删除章节插图` · `3600 — 保存章节` · `3608 — 删除章节` · `3614 — 批量删除章节` ·
`3628 — 提交章节翻译`

### 9.7 Interpolated / composed strings (must keep the interpolation, not just the text)
`1729`, `1768`, `1993`, `2048`, `2073`, `2100 — 第 ${index + 1} 章`, `2107 — 第 ${nextIndex + 1} 章`,
`2132 — 存档 $timestamp`, `2050 — 分章失败：${failure.message ?: "规则无效"}`,
`2060 — 脚本执行失败：${error ?: "未返回文本"}`, `2093 — 替换失败：${failure.message ?: "正则无效"}`,
`2410`, `3248`, `3295`, `3573`, `1626`, `2911`, `3648`, and the server-message fallbacks
(`1804 — 上传失败`, `2768 — 发布失败`, `3294 — 保存失败`).

---

## 10. Known defects and traps that the refactor must handle deliberately

1. **Two mojibake literals (user-visible garbage).**
   * `2698` — `forumState = ForumState(posts = result.toLoadResult("璁哄潧"))`. Code points
     `U+7481 U+54C4 U+6F67`; this is `论坛` encoded as UTF-8 and re-decoded as GBK. Forum load failures
     currently read `璁哄潧请求失败: …`. Correct value: `论坛` (or `VisibleUiLabels`-style constant).
   * `3914` — `searchTags = result.toLoadResult("鏍囩")`. Code points `U+93CD U+56E9 U+E137`; the last
     one is a Private-Use-Area character. Should be `标签`. (Verified byte-level; these are the only two
     corrupted literals in the whole `main` source tree.)
2. **`profileState.profile` and `homeState.user` are two copies of the same entity.** Five profile
   functions write both (`loadProfile` 758, `saveProfile` 815, `checkinCurrentUser` 852,
   `verifyCurrentUserAdult` 889, `uploadProfileAvatar` 920) but `loadHome` only writes `homeState.user`.
   Any split that separates Profile from Library must keep this fan-out or introduce a shared session
   entity.
3. **`currentUserProfile()` reads `homeState`** — so Admin gating, Forum-create gating and DM peer
   resolution silently depend on the Library domain having loaded (or on a decodable JWT).
4. **Admin double-increment of `adminRequestSerial`**: `runAdminMutation` bumps it (1235) and then its
   success path calls `loadAdminSectionInternal`, which bumps again (1039). Harmless today, but any
   refactor that guards on the captured value across that boundary will break.
5. **`bookChapterRequestSerial` is shared** between the chapter list and the illustration panel
   (3364, 3471): opening illustrations invalidates an in-flight chapter-list load.
6. **`uploadManagedBookCover` intentionally does not increment** `bookEditRequestSerial` (3323) — it
   piggybacks on the current load generation. Same pattern in `loadMoreFavorites` (3783) and
   `loadMoreSearch` (3864).
7. **Cross-domain writes**: `sendEditorToUpload` (2206) overwrites `uploadBookState` wholesale and
   rewrites the route stack; `saveManagedBook` (3299) calls `loadBookDetail`; `submitPoliticalExam`
   (2383) rewrites `authToken` and calls `loadHome`; `saveProxySettings` (682) and
   `saveCapturedAuthToken`/`clearAuthToken` also call `loadHome`.
8. **`openTab` runs the target loader before mutating the stack** (720–728), and re-taps of the active
   tab reload (711–717). A naive extraction of navigation will change refresh semantics.
9. **`ReaderProgressStore.clear()` and `SearchHistoryStore.clear()` / `SearchSettingsStore.clear()` wipe
   the whole prefs file**, not individual keys.
10. **All store reads/writes happen on the main thread** (except `editorArchiveStore.save`).
11. **`BottomTab.title` is dead** (display labels come from `bottomTabDisplayLabel`); `searchPage` is
    public but never read outside the VM; `openMessageDetail` is only reachable through `openMessage`.
12. **Every `submit*` / reaction handler ends with an unconditional reload** of its surface, even on
    failure (e.g. `submitBookComment` 2999, `reactOnForumPost` 2921). That reload is the only mechanism
    that reconciles server-side counters — dropping it would lose visible behaviour.
13. **Workspace API keys and site cookies are persisted in plaintext SharedPreferences**
    (`novalpie_native_workspace`, `novalpie_native_auth`).
14. **`readEditorText` caps at 50 000 000 characters** and `EpubParser.parse` runs fully in memory on
    `Dispatchers.IO`; the 50 MiB server-chunked branch exists only for the *upload* path, not the editor.

---

## 11. Proposed domain decomposition

This is the only prescriptive section. Seventeen feature domains fall out of the inventory cleanly; the
hard part is the four shared concerns that every domain touches. Suggested shape: **1 shared core + 17
feature state holders**, with the current `NovalPieViewModel` reduced to a thin facade (or replaced by
per-screen state holders wired in `NovalPieApp`).

### 11.1 Shared core (must be extracted first — everything depends on it)

| Component | Owns | Current locations |
|---|---|---|
| **`NavigationController`** | `routes` (`mutableStateListOf`), `currentTab`, `currentRoute`, `pushDistinctRoute`/`replaceTopReaderRoute`/`replaceWith`, `goBack`, `openTab`, `openSettings`, `openWebFallback`, `openLoginFallback`, `openDeepLink`, `openMessageAction` URL dispatch | 450, 469, 550, 701–733, 3705–3748, 1469, 4014, `ui/RouteStackPolicy.kt` |
| **`SessionController`** | `authToken`, `AuthSessionStore`, JWT decode, `currentUserProfile()`, `isAdminProfile` gating, `saveCapturedAuthToken`, `clearAuthToken`, the "token replaced by exam" hook | 435, 685–699, 1662, 2383, `data/AuthSessionStore.kt` |
| **`NetworkSettingsController`** | `proxySettings`/`proxyEnabled`/`proxyHost`/`proxyPortText`, `NetworkConfigStore`, image-loader reconfiguration, the `NovalPieApi` instance and its three providers | 418, 427–448, 658–683 |
| **`RequestGeneration` utility** | one reusable serial-token abstraction replacing the 16 ad-hoc `Long`s, plus the three existing freshness predicates (already isolated and unit-tested in `ui/RequestFreshness.kt`) | 451–466, `ui/RequestFreshness.kt` |
| **`UiFeedback` / error mapping** | `LoadResult`, `Result.toLoadResult(label)`, `apiFailureMessage`, `retryActionLabel`, `VisibleUiLabels`, and a **single registry of failure labels** (fixes the two mojibake strings and the inline/`\uXXXX` split) | 4020, `ui/ApiMessages.kt`, `ui/ErrorRecovery.kt`, `ui/VisibleUiLabels.kt` |

Two shared *data* seams also need an owner, because they are read by more than one domain:
* **`UserSessionState`** — the current `UserProfile` (today duplicated in `homeState.user` and
  `profileState.profile`). One writer, many readers.
* **`ReaderProgressRepository`** — `ReaderProgressStore` + `readerProgress` + `recentReaderProgresses`,
  read by Library (recents), BookDetail (per-book marker), written by Reader.

### 11.2 The 17 feature domains

| # | Domain | State it owns | Public functions (count) | Stores | Cross-domain edges to preserve |
|---|---|---|---|---|---|
| 1 | **Library / Bookshelf** | `homeState`, `bookshelfQuery`, `selectedFavoriteGroupId`, (reads `recentReaderProgresses`, `readerProgress`) | `updateBookshelfQuery`, `selectFavoriteGroup`, `loadHome`, `loadMoreFavorites`, `continueReading`, `clearReaderProgress` (6) | ReaderProgress | writes `homeState.user` (session); `loadHome` is called by Settings, Session, Exam, `openTab` |
| 2 | **Search / Discover** | `searchKeyword`, `searchHistory`, `searchOptions`, `searchResults`, `searchTags`, `searchPage`, `searchCanLoadMore`, `searchLoadingMore` | 14 (7 option setters + history/tag/clear + `performSearch`, `loadMoreSearch`, `loadSearchTags`) | SearchHistory, SearchSettings | → `openBook` |
| 3 | **Book detail** | `bookDetailState`, `bookCatalogQuery` | `openBook`, `loadBookDetail`, `updateBookCatalogQuery`, + 8 comment functions (11) | ReaderProgress (read) | reloaded by `saveManagedBook`; shares comment-reaction plumbing with Forum/Reader |
| 4 | **Reader** | `readerState`, `readerCatalogQuery`, `readerUiOptions` | `openReader`, `loadReader`, 3 font/theme, + 8 comment functions (14) | ReaderSettings, ReaderProgress (write) | writes `bookDetailState.readerProgress` |
| 5 | **Forum** | `forumState`, `forumPostDetailState`, `forumCreateState` | 18 | — | gating via session role; `openUserProfile` |
| 6 | **Own profile / check-in** | `profileState` | 10 | — | writes `homeState.user` (5 sites) |
| 7 | **User profile (others)** | `userProfileDetailState` | 4 | — | `openUserProfile` self-redirect needs session id + navigation |
| 8 | **Admin** | `adminState` (+ 2 query classes) | 21 | — | gated on session role via `homeState` |
| 9 | **Tools hub** | `toolsState` | 1 (`loadTools`) | — | pure launcher into Workspace/Upload/Editor/Exam/Admin |
| 10 | **Message center** | `messageCenterState` | 14 | — | `openMessage` branches into Conversation |
| 11 | **Message detail** | `messageDetailState` | 7 (+`openMessageAction` URL dispatch) | — | `deleteCurrentMessage` calls `goBack` + reloads center |
| 12 | **Message conversation (DM)** | `messageConversationState` | 4 | — | needs session id for `sendDirectMessage` |
| 13 | **Message settings** | `messageSettingsState` | 4 | — | — |
| 14 | **Upload book** | `uploadBookState` | 7 | — | overwritten by Editor's `sendEditorToUpload`; `existingNovelId` derived from the route |
| 15 | **EPUB editor** | `uploadEditorState` | 31 | EditorArchive (files), WorkspaceLocal (read, for `aiConfigs`) | writes `uploadBookState`; `scriptRunId` handshake with `EditorScriptEngine` |
| 16 | **Political exam** | `politicalExamState` | 9 | AuthSession (write on pass) | rewrites `authToken`, then `loadHome` |
| 17 | **Workspace** | `workspaceState` | 11 | WorkspaceLocal | `localApis` is the source of the Editor's AI configs |
| 18 | **Book management** | `bookEditState` | 9 | — | `saveManagedBook` → `loadBookDetail` |
| 19 | **Chapter management** | `bookChapterManagerState` | 19 | — | shares one serial between list + illustrations; `openBookAppend` hands off to Upload |

(Rows 1–17 are the 17 domains; rows 18–19 are the two book-*management* domains — count them as 19
modules if you keep Book management and Chapter management separate, which the state boundaries support:
they share only `bookId`.)

### 11.3 Reusable behaviour worth factoring out of the domains

Four near-identical clusters were found; extracting them removes roughly 500 duplicated lines:

1. **Comment surface** — `{Book, Reader, Forum}` each have `updateXCommentDraft` / `replyToX` /
   `cancelXReply` / `submitXComment` / `like|dislike|emoji|awardXComment` plus a private `reactOnX`
   wrapper. State shape is identical (`commentDraft`, `replyingToCommentId`, `replyingToName`,
   `actionLoading`, `actionMessage`); only the API pair and the label prefix differ
   (`""` / `章节` / forum-specific), plus the reload target.
2. **Mutation wrapper** — `runAdminMutation` (1228), `runMessageCenterAction` (1614),
   `runMessageDetailAction` (1641), `runWorkspaceAction` (2666), `runManagedChapterMutation` (3633) are
   the same algorithm: busy-flag gate → in-flight message → server-message-or-fallback → reload. One
   generic `runAction(label, successMessage, reload, guard)` covers all five (careful: they differ in
   whether the success message doubles as the failure label, and whether they reload).
3. **Fan-out load** — `loadHome`/`loadProfile`/`loadUserProfile`/`loadWorkspace`/`loadTools`/
   `loadMessageCenter`/`loadBookDetail`/`loadReader`/`loadForumPostDetail`/`loadBookEditInfo` all follow
   "set N slots to `Loading` → N parallel `async { runCatching { … } }` → guard → commit". One
   `loadInto` helper plus the shared generation token collapses this.
4. **Load-more paging** — `loadMoreFavorites` / `loadMoreSearch` / `loadMoreMessages` share
   capture-current-generation + merge-dedupe + `loadingMore` flag; the merge functions already exist
   (`mergeBooksById`, `mergeMessagePages`).

### 11.4 Suggested extraction order (lowest risk first)

1. `RequestGeneration` + the freshness predicates (already unit-tested, no state moved).
2. `UiFeedback`/label registry — and fix the two mojibake strings in the same commit so the change is
   reviewable.
3. `NavigationController` (mechanical; 15 route-equality guards must keep reading the *same* live route).
4. `SessionController` + `UserSessionState` (unifies `homeState.user` / `profileState.profile`).
5. Leaf domains with no inbound edges: Exam, Workspace, Message settings, Message conversation, Tools,
   User profile.
6. Comment-surface extraction, then Forum / BookDetail / Reader.
7. Upload + Editor together (they are bidirectionally coupled through `sendEditorToUpload`).
8. Book management + Chapter management (route-guard heavy; needs `NavigationController` first).
9. Library + Search last — `loadHome` is the most-called cross-domain effect in the file.
