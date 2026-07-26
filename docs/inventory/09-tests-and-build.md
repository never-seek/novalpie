# 09 — Test suite and build inventory (the refactor safety net)

Project: `D:/NovalPie/native-android` — Kotlin + Jetpack Compose native client for `novalpie.cc`.
Inventory produced by reading actual source, build scripts, the offline Gradle cache directory
listing, and the last green test-results XML. Docs were used only for cross-checking.

---

## 0. Baseline pinned for this inventory + IN-FLIGHT CONCURRENT CHANGE (read first)

**Baseline commit:** `f2cc124` — *"Phase 0: add string golden master and refactor plan"*
(parent `fc1d555` *"Baseline: NovalPie 2.0 native app before refactor"*).
All test-source and pre-migration build-config claims below are against that commit.

**WARNING — the working tree changed while this inventory was being produced.**
A parallel agent is executing "Phase 1: Toolchain and build hygiene". At the time of writing,
`git status --short` reports:

```
 D app/build.gradle          ?? app/build.gradle.kts
 D build.gradle              ?? build.gradle.kts
 D settings.gradle           ?? settings.gradle.kts
 M gradle.properties         ?? gradle/libs.versions.toml
 M gradle/wrapper/gradle-wrapper.properties
 M app/proguard-rules.pro
 M app/src/main/AndroidManifest.xml
 M app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt
 ?? app/src/main/res/xml/
```

Sections 5 therefore documents **two** toolchains: the baseline (§5.1) and the in-flight
migration target (§5.6). No test source file was touched by that migration — the
51 test files and 252 test methods inventoried here are the same in both states.

**Nothing in this document was reverted or modified.** This is a read-only inventory.

---

## 1. Test suite at a glance

| Metric | Value | Evidence |
|---|---|---|
| Test source files | **51** (16 in `data/`, 35 in `ui/`) | `app/src/test/java/com/novalpie/nativeapp/{data,ui}/*.kt` |
| Test source lines | **6010** | `wc -l` over all 51 files |
| `@Test` methods | **252** | `grep -c '@Test'` per file, summed |
| Last green run | **51 suites / 252 tests / 0 failures / 0 errors / 0 skipped** | `app/build/test-results/testDebugUnitTest/*.xml`, newest write `2026-07-26 14:36:12` |
| Robolectric suites | **14** | files containing `@RunWith(RobolectricTestRunner::class)` |
| MockWebServer suites | **5** | `NovalPieApiTest`, `AdminApiTest`, `BookManagementApiTest`, `UploadApiTest`, `WorkspaceApiTest` |
| Pure-JVM suites | **37** | no `@RunWith`, no MockWebServer |
| Instrumentation / Compose UI tests | **0** | `app/src/androidTest` **does not exist** |
| `robolectric.properties` | **absent** | no file anywhere in the repo |
| Test resources (`src/test/resources`) | **absent** | `app/src/test/` contains only `java/` |
| Coroutine test infra (`runTest`, `TestDispatcher`) | **absent** — suspend tests use `runBlocking` (78 call sites) | `grep runTest` → none |

Three kinds of test exist, and only three:

1. **Pure function / pure data** (37 suites) — plain JUnit4, no Android, no network. These
   assert on presentation helpers, filters, validators, route policies, palettes, and label sets.
2. **Robolectric** (14 suites) — needed for `SharedPreferences`, `Context`, `android.text.Html`,
   `org.json` (Android's JSON impl, not a JVM library), and `android.util.Base64`.
3. **MockWebServer request/response contract** (5 suites, all also Robolectric) — start a local
   `MockWebServer`, construct `NovalPieApi(baseUrl = server.url("/"))`, enqueue a canned website
   response, call the API, then assert both **the parsed model** and **the outgoing request shape**
   (method, encoded path, query parameters, headers, JSON/multipart body).

---

## 2. Every test file — coverage, method count, kind

### 2.1 `data/` (16 files, 104 test methods)

| File | Tests | Kind | Production unit under test | What it pins |
|---|---|---|---|---|
| `data/NovalPieApiTest.kt` (2240 L) | **55** | MockWebServer + Robolectric | `data/NovalPieApi.kt` | The main website contract suite. Full breakdown in §3. |
| `data/BookManagementApiTest.kt` (224 L) | 5 | MockWebServer + Robolectric | `NovalPieApi` chapter-management + illustration + transfer/threshold endpoints | §3.3 |
| `data/WorkspaceApiTest.kt` (188 L) | 3 | MockWebServer + Robolectric | `NovalPieApi` `/workspace/*` endpoints | §3.4 |
| `data/AdminApiTest.kt` (186 L) | 7 | MockWebServer + Robolectric | `NovalPieApi` `/api/admin/*` endpoints | §3.2 |
| `data/UploadApiTest.kt` (158 L) | 3 | MockWebServer + Robolectric | `NovalPieApi` upload/chunk/EPUB-parse endpoints | §3.5 |
| `data/NovalPieImageLoadingTest.kt` (105 L) | 5 | Pure JVM (OkHttp objects only) | `data/NovalPieImageLoading.kt` | `novalPieImageOkHttpClient` proxy-selector order, `readTimeoutMillis >= 30_000`, `callTimeoutMillis >= 60_000`, and that the single interceptor sets `referer: https://novalpie.cc/` and a `user-agent` containing `NovalPieNative` (`:22,:36,:45,:59,:69`). Uses a hand-written `RecordingChain : Interceptor.Chain` (`:80–104`). |
| `data/NetworkConfigStoreTest.kt` (87 L) | 5 | Robolectric (`ApplicationProvider`) | `data/NetworkConfigStore.kt` | Default proxy is disabled and `toJavaProxy()` is null (`:30`); `toProxyRoutes()` order is `10.0.2.2:7890` → `127.0.0.1:7890` → `Proxy.NO_PROXY` (`:38`); with `preferEmulatorProxy = true` the first two swap (`:54`); an explicit proxy is prepended but the automatic fallbacks are kept (`:70`); `shouldPreferEmulatorProxy(arrayOf("x86_64", …))` is true and arm-only is false (`:83`). Clears prefs file `novalpie_native_network` in `setUp` (`:23`). |
| `data/EditorProcessorTest.kt` (80 L) | 4 | Pure JVM | `data/EditorProcessor.kt` | `splitByRegex` with `^第[\d零一二三四五六七八九十百千万]+章.*$` yields titles `第1章 开端`, `第2章 继续` and keeps the pre-heading `preface` in chapter 1 (`:9`); `splitByMarkdown(level = 1)`; `splitByCharacterCount(targetCharacters = 55)`; `splitByParagraphCount(targetParagraphs = 3)` → exactly 4 chunks, no text dropped (`:33`); website chapter identifiers `##__T[00001]__##` / `##__C[00002]__##` round-trip through `toWebsiteIdentifiers` / `validateWebsiteIdentifiers` / `parseWebsiteIdentifiers` (`:46,:61`). |
| `data/WorkspaceLocalStoreTest.kt` (78 L) | 2 | Robolectric | `data/WorkspaceLocalStore.kt` | `upsertApi` is keyed by id (second upsert replaces, does not append), `apiKey`/`serverId` persist, `deleteApi` empties (`:23`); `upsertJob`/`loadJobs`/`deleteJob` round-trip with `completedChapters` + `status` updates (`:57`). |
| `data/EpubParserTest.kt` (72 L) | 1 | Pure JVM (builds a zip in memory) | `data/EpubParser.kt` | Parses `META-INF/container.xml` → `OEBPS/content.opf`; extracts `dc:title/creator/language/description`; orders chapters by **spine**, not manifest (manifest lists chapter-two first, spine lists chapter-one first → result is `["First","Second"]`); `<br/>` becomes text; `&amp;` decoded; `rawPath = "OEBPS/text/one.xhtml"`, `spineIndex = 0`; the `image/jpeg` manifest item is **not** decoded (`:13`). |
| `data/ReaderProgressStoreTest.kt` (66 L) | 4 | Robolectric | `data/ReaderProgressStore.kt` | Per-book isolation (`:22`), null for unknown book (`:36`), `loadRecent(limit)` in most-recent-first order (`:43`), re-saving an existing book moves it to the front and updates chapter/title (`:55`). |
| `data/SearchHistoryStoreTest.kt` (55 L) | 4 | Robolectric | `data/SearchHistoryStore.kt` | Most-recent-first (`:21`), duplicate promotion (`:30`), blank keyword ignored + history capped at **10** entries (`:39` asserts `(12 downTo 3).map { "k$it" }`), `loadLastKeyword()` returns `""` when empty (`:47`). |
| `data/SearchSettingsStoreTest.kt` (41 L) | 2 | Robolectric | `data/SearchSettingsStore.kt` | Full `PersistedSearchSettings` round-trip incl. `wordCountRange = "100000..500000"` (`:21`); defaults when nothing saved (`:38`). |
| `data/AuthTokenProfileTest.kt` (41 L) | 2 | Robolectric (needs `android.util.Base64`/`org.json`) | `decodeAuthTokenProfile` in `data/AuthSessionStore.kt` | Decodes the **nested** website JWT shape `{"sub":…,"exp":…,"data":{"username":…,"role":…}}` (`:13`); returns null for expired `exp` and for `"not-a-jwt"` (`:26`). |
| `data/EpubWriterTest.kt` (40 L) | 1 | Pure JVM | `data/EpubWriter.kt` | `mimetype` is the **first** zip entry with content `application/epub+zip`; `META-INF/container.xml` present; `OEBPS/content.opf` references `chapter-2.xhtml`; `OEBPS/nav.xhtml` contains `Two`; `OEBPS/chapter-1.xhtml` escapes `&` as `&amp;` (`:14`). |
| `data/EditorArchiveStoreTest.kt` (37 L) | 1 | Robolectric | `data/EditorArchiveStore.kt` | File-backed archive save → load (`textContent` survives) → list → delete → null (`:16`). Uses a unique dir name `editor-test-${System.nanoTime()}`. |

### 2.2 `ui/` (35 files, 148 test methods)

| File | Tests | Kind | Production unit | Notes |
|---|---|---|---|---|
| `ui/ForumPresentationTest.kt` (239 L) | **11** | Pure | `ui/ForumPresentation.kt` (+ `ProductCopy.kt` for `ForumFeedItem`) | §4.1 |
| `ui/RequestFreshnessTest.kt` (163 L) | 6 | Pure | `ui/RequestFreshness.kt` + state classes in `NovalPieViewModel.kt` | §2.3 |
| `ui/ProfilePresentationTest.kt` (163 L) | 9 | Pure | `ui/ProfilePresentation.kt`, `ui/NovalPieApp.kt` (`ProfileOverview`), `ui/NovalPieViewModel.kt` (`resolveUserLoadResult`) | §4.2 |
| `ui/ProductCopyTest.kt` (133 L) | 12 | Pure | `ui/ProductCopy.kt` | §4.3 |
| `ui/NovelCardFactsTest.kt` (131 L) | 6 | Pure | `ui/NovelCardFacts.kt` | §4.4 |
| `ui/DiscoverPresentationTest.kt` (119 L) | 7 | Pure | `ui/DiscoverPresentation.kt` | §4.5 |
| `ui/ReaderPresentationTest.kt` (92 L) | 9 | Pure | `ui/ReaderPresentation.kt` + `ui/NovalPieApp.kt` (illustration labels) + `ui/BookDetailPresentation.kt` | §4.6 |
| `ui/MessagePresentationTest.kt` (91 L) | 7 | Pure | `ui/MessagePresentation.kt`, `ui/ToolsPresentation.kt` (`messageTypeLabel`) | §4.7 |
| `ui/ReaderTextTest.kt` (74 L) | 4 | **Robolectric** (needs `android.text.Html`) | `ui/ReaderText.kt` | §2.4 |
| `ui/RouteStackPolicyTest.kt` (70 L) | 7 | Pure | `ui/RouteStackPolicy.kt` + `AppRoute` (in `NovalPieViewModel.kt`) | §2.3 |
| `ui/PoliticalExamPresentationTest.kt` (64 L) | 3 | Pure | `ui/PoliticalExamPresentation.kt` | §4.8 |
| `ui/ForumCreatePresentationTest.kt` (64 L) | 3 | Pure | `ui/ForumCreatePresentation.kt` | §4.9 |
| `ui/BookDetailFactsTest.kt` (64 L) | 2 | Pure | `ui/BookDetailFacts.kt` | §4.10 |
| `ui/EditorScriptContractTest.kt` (61 L) | 3 | **Robolectric** | `ui/EditorScriptEngine.kt` | §2.4 |
| `ui/BookManagementPresentationTest.kt` (60 L) | 3 | Pure | `ui/BookChapterPresentation.kt` | §4.11 |
| `ui/BookDetailPresentationTest.kt` (58 L) | 4 | Pure | `ui/BookDetailPresentation.kt` | §4.12 |
| `ui/UiNavigationTest.kt` (52 L) | 5 | Pure | `ui/UiNavigation.kt` + `BottomTab`/`AppRoute` (in `NovalPieViewModel.kt`) | §4.13 |
| `ui/LibraryPresentationTest.kt` (49 L) | 3 | Pure | `ui/LibraryPresentation.kt` | §4.14 |
| `ui/VisibleUiLabelsTest.kt` (48 L) | 2 | Pure | `ui/VisibleUiLabels.kt` | §4.15 |
| `ui/BookFilterTest.kt` (46 L) | 3 | Pure | `ui/BookFilter.kt` | `bookMatchesQuery` matches status (`完结`/`已完结`, not `连载`), title/author/tag/id, word count with and without thousands separators (`1234567` and `1,234,567`), and `updatedAt` prefix `2026-07-02` (`:10,:19,:34`). |
| `ui/UploadPresentationTest.kt` (44 L) | 3 | Pure | `ui/UploadPresentation.kt` | §4.16 |
| `ui/CatalogSummaryTest.kt` (44 L) | 3 | Pure | `ui/CatalogSummary.kt` | §4.17 |
| `ui/ToolsPresentationTest.kt` (39 L) | 3 | Pure | `ui/ToolsPresentation.kt` | §4.18 |
| `ui/ReaderAdjacentChapterTest.kt` (38 L) | 3 | Pure | `ui/ReaderAdjacentChapter.kt` | prev/next resolution; first chapter has no previous; an **unmatched** chapter id yields `null`/`null` and must *not* fall back to catalog edges (`:16,:24,:32`). |
| `ui/WebFallbackPolicyTest.kt` (35 L) | 3 | Pure | `ui/WebFallbackScreen.kt` (`webViewProxyUrl`) | Explicit proxy wins → `http://proxy.example:8080`; emulator fallback → `http://10.0.2.2:7890`; real device → `null` (`:10,:20,:30`). |
| `ui/ReaderProgressLabelTest.kt` (33 L) | 3 | Pure | `ui/ReaderProgressLabel.kt` | §4.19 |
| `ui/ThemePaletteTest.kt` (31 L) | 2 | Pure | `ui/NovalPieTheme.kt` | §5.5 / §4.20 — pins exact ARGB tokens. |
| `ui/WorkspacePresentationTest.kt` (30 L) | 3 | Pure | `ui/WorkspacePresentation.kt` | §4.21 |
| `ui/ChapterFilterTest.kt` (30 L) | 2 | Pure | `ui/ChapterFilter.kt` | title / number / id matching; word count with and without separators; `updatedAt` prefix (`:9,:18`). |
| `ui/ApiFailureMessageTest.kt` (30 L) | 3 | Pure | `ui/ApiMessages.kt` | §4.22 |
| `ui/AdminPresentationTest.kt` (30 L) | 2 | Pure | `ui/AdminScreens.kt` (option builders only) | §4.23 |
| `ui/BookCoverFallbackTest.kt` (28 L) | 4 | Pure | **`ui/NovalPieApp.kt`** (`bookCoverAspectRatio`, `novelGridColumnCount`, `bookCoverFallbackText`) | §4.24 |
| `ui/BookDetailProgressMarkerTest.kt` (22 L) | 2 | Pure | `ui/BookDetailProgressMarker.kt` | Marks a chapter only when *both* bookId and chapterId match, and never when progress is null (`:10,:19`). |
| `ui/ImagePreviewTransformTest.kt` (19 L) | 1 | Pure (imports `androidx.compose.ui.geometry.Offset`, `unit.IntSize` — data classes only, no Compose runtime) | `ui/ImagePreviewDialog.kt` | `clampImagePreviewScale`: min 1f, max 6f; `clampImagePreviewOffset(Offset(50,50), 1f, IntSize(900,1600)) == Offset.Zero`; at 2f the clamp is `Offset(450f, -800f)` (`:10`). |
| `ui/ErrorRecoveryTest.kt` (18 L) | 2 | Pure | `ui/ErrorRecovery.kt` | §4.25 |

### 2.3 Navigation / request-lifecycle tests (the only tests that touch god-file types)

`ui/RequestFreshnessTest.kt` and `ui/RouteStackPolicyTest.kt` are the two suites that
exercise types **declared inside `NovalPieViewModel.kt`** (`AppRoute`, `BottomTab`,
`BookDetailState`, `ReaderState`, `SearchOptions`, `ReaderUiOptions`). They do **not**
instantiate the ViewModel. What they pin:

- `isFreshBookDetailResult` is true for `AppRoute.BookDetail(200)` **and** for
  `AppRoute.Reader(bookId = 200, …)`, false for a different bookId and false for `AppRoute.Home`
  (`RequestFreshnessTest.kt:11`).
- `isFreshReaderResult` requires route == Reader **and** matching chapterId
  (`RequestFreshnessTest.kt:45`); `ReaderState` carries a separate `comments: LoadResult`
  (`:75`).
- `isFreshSearchResult(request, activeSerial, currentKeyword, currentOptions, expectedPage)` is
  false if any of serial / keyword / options / page differ (`:95`).
- `searchKeywordForSubmission` prefers a trimmed explicit submitted value over the current field
  (`" 354491 "` → `"354491"`), else falls back to the current keyword (`:152`).
- `isFreshRequestSerial(7, 7)` true / `(7, 8)` false (`:159`).
- `pushDistinctRoute` returns the **same instance** (`assertSame`) when the top route already
  equals the pushed route — verified for `BookDetail`, `WebFallback("https://novalpie.cc/favorites")`
  and `ForumPostDetail(91)` (`RouteStackPolicyTest.kt:9,:45,:54`); appends for a different id
  (`:18,:63`).
- `replaceTopReaderRoute` returns the same instance for the same reader chapter (`:27`) and
  replaces the top entry (does **not** grow the stack) for a different chapter (`:36`).

### 2.4 The two Robolectric-only `ui/` suites

- `ui/ReaderTextTest.kt` — Robolectric because `ReaderText.kt` calls `Html.fromHtml`.
  Pins: `&nbsp;` → space, `&amp;` → `&`, `<br>` → `\n` inside a paragraph, `<div>` treated as a
  paragraph, `<p>&nbsp;</p><br>` → empty list, bare text `" Plain text line "` kept as fallback
  (`:12,:32`). `readerBlocksFromContent` preserves text/illustration order and resolves relative
  image srcs against `https://novalpie.cc` — asserted literally as
  `"https://novalpie.cc/uploads/chapters/one.webp"` (`:38`) — plus markdown `![alt](url)` images
  and website `[[img:N]]` placeholders resolved from an `imagePlaceholders` map (`:57`).
- `ui/EditorScriptContractTest.kt` — Robolectric. Pins that `chunkEditorScriptText` is lossless
  (`chunks.joinToString("") == text`) and that every non-final chunk ends with `\n` (`:13`); that
  the generated JS program exposes helpers `processText`, `insertMarker`, `findMatches`,
  `splitByParagraphs`, `getWordCount` and JSON-quotes user text (`quote: \" and newline\nnext`)
  (`:29`); and that `parseEditorScriptCallback` returns `result` on `ok:true` and throws with
  `message == "boom"` on `ok:false` (`:53`).

---

## 3. MockWebServer contract tests — the highest-value tests for the refactor

All five suites share the same fixture shape:

```kotlin
@RunWith(RobolectricTestRunner::class)
class …Test {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi
    @Before fun setUp() { server = MockWebServer(); server.start()
        api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')) }
    @After fun tearDown() { server.shutdown() }
}
```
(`NovalPieApiTest.kt:28–43`, `AdminApiTest.kt:15–30`, `BookManagementApiTest.kt:18–31`,
`UploadApiTest.kt:18–33`, `WorkspaceApiTest.kt:16–31`.)

Because the base URL is injected via the constructor, **`NovalPieApi`'s single-arg
`baseUrl` constructor plus optional `proxySelectorProvider` is itself a hard contract** —
`NovalPieApiTest.kt:934` constructs `NovalPieApi(baseUrl = …, proxySelectorProvider = { FixedProxySelector(…) })`.
Any refactor that changes that constructor signature breaks all 73 contract tests at once.

### 3.0 Coverage of the API surface

`data/NovalPieApi.kt` declares **106** non-private API functions (plus one helper `normalizeList`).
Cross-referencing every `api.<fn>(` call site in the test tree:

**Only 3 declared functions are never called by any test:**

| Untested function | Line in `NovalPieApi.kt` |
|---|---|
| `adminApproveAllReviews` | `:592` |
| `userCheckinStats` (public-user variant; the `me` variant *is* tested) | `:726` |
| `normalizeList` (helper) | `:2318` |

**103/106 API functions are invoked at least once.** Note the distinction: *invoked* is not the
same as *request-shape asserted*. The endpoints with explicit path/method/body assertions are
enumerated below.

### 3.1 `NovalPieApiTest.kt` — 55 tests

Request-shape assertions, grouped by endpoint. `P` = path/encodedPath asserted,
`M` = HTTP method asserted, `Q` = query parameters asserted, `B` = request body asserted,
`H` = request header asserted.

| Endpoint | Method | Asserted | Test (`NovalPieApiTest.kt:`) |
|---|---|---|---|
| `/api/novels/{id}/chapters` | GET | P | `chaptersNormalizeWebsiteFieldAliases` `:46`; sorting `:81` |
| `/api/novels/{id}/detail` | GET | P | `bookDetailUnwraps…` `:107`; `managedBookInfo…` `:555` |
| `/api/novels/{id}/photo` | GET | P, Q(`favorite_type=novel`) | `bookCoverPhotoUsesWebsiteOriginalPhotoEndpoint` `:165` |
| `/api/novels/{id}/photo` | **PUT** | P, M, H(`content-type` starts `multipart/form-data`), B(`name="cover"`, `filename="cover.png"`, raw bytes present) | `managedBookCoverUploadKeepsOriginalFileAndUsesPut` `:629` |
| `/v1/chat/completions` (editor AI, external endpoint) | POST | P, H(`authorization: Bearer test-editor-key`), B(`model`, `temperature == 0.3`, `response_format.type == "json_object"`, `messages[1].content` contains the chapter titles) | `editorAiRegexUsesWebsiteOpenAiCompatibleContract` `:193` |
| `/api/political-exams/sessions` | POST | P, M | `politicalExamSessionNormalizesAllWebsiteQuestionTypes` `:231` |
| `/api/political-exams/sessions/submit` | POST | P, B(`session_id`; `answers.single_choice[1]` is JSON **null**; `answers.multiple_choice[0][1]`; `answers.true_false[1] == false`; `answers.fill_blank[0]`) | `politicalExamSubmitUsesWebsiteAnswersShapeAndNormalizesResult` `:274` |
| `/api/users/me` | GET | P | `currentUserNormalizesWebsiteProfileFields` `:330`; aliases `:2147` |
| `/api/users/me` | **PATCH** | P, M, B(`username`, `bio`, `show_checkin`, `auto_checkin`) | `updateCurrentUserAndCheckinSettingsUseWebsitePatchBodies` `:372` |
| `/api/users/me/checkins/settings` | **PATCH** | P, M | same test `:396` |
| `/api/users/me/checkins/stats` | GET | P | `currentUserCheckinStatsAndCheckinUseWebsiteEndpoints` `:402` |
| `/api/users/me/checkins` | POST | P, M | same test `:428` |
| `/api/users/{id}` | GET | P | `publicUserProfileAndActivity…` `:434` |
| `/api/users/{id}/activities` | GET | full path+query string `?type=post_comment&page=2&limit=30` **in that exact order** | same test `:462` |
| `/api/users/{id}/novels` | GET | P | `publicUserNovelsAndCheckinDataUseWebsiteEndpoints` `:469` |
| `/api/users/{id}/checkins` | GET | `?start_date=2026-01-01&end_date=2026-12-31` exact order | same test `:496` |
| `/api/users/{id}/checkins/settings` | GET | `?user_id=42` | same test `:500` |
| `/api/users/me/verifies/adult` | POST | P, M, B(`birth_year == 1995`) | `adultVerificationUsesWebsiteBirthYearBody` `:504` |
| `/api/users/me/avatar` | POST | P, M, H(multipart), B(`name="avatar"`, `filename="avatar.png"`) | `avatarUploadUsesWebsiteMultipartFieldAndEndpoint` `:531` |
| `/api/users/me/novels/{id}/permissions/check` | GET | P | `managedBookInfoPermissionsAndSaveUseWebsiteContracts` `:611` |
| `/api/users/me/novels/{id}` | **PATCH** | P, M, B(`title`, `title_translation`, `author_name`, `description`, `spans == "19 已完结"`, `is_adult == 1` **as int**, `photo_url`, `tags` array) | same test `:612–625` |
| `/api/search` | GET | P, Q(`q` **not** `keyword` — `assertEquals(null, …queryParameter("keyword"))`, `page`, `limit`, `sort_by`, `sort_order`, `scope`, `match_type`, `adult_filter`, `source`, `min_word_count`, `max_word_count`), H(`user-agent` contains `NovalPieNative`) | `searchNormalizesResultArrayAliasesAndSendsQueryParameters` `:710` |
| `/api/search` via HTTP proxy | GET | proxy `requestLine` contains the absolute URL `http://127.0.0.1:{closedPort}/api/search` | `searchFallsBackToProxySelectorWhenDirectConnectionFails` `:907` |
| `/api/tags` | GET | P, Q(`sort`, `limit`) | `tagsNormalizeWebsiteTagAliasesAndSendSortLimitParameters` `:951` |
| `/api/messages` | GET | P, Q(`page`, `page_size`) | `messagesNormalizeCurrentWebsiteFieldsAndSendPagination` `:985` |
| `/api/messages` (filtered) | GET | P, M, Q(`page`, `page_size`, `message_type`, `is_read=false`, `priority`, `keyword=更新`) | `messagePageSendsWebsiteFiltersAndNormalizesPaginationAndMetadata` `:1037` |
| `/api/messages/{id}` | GET | P, M | `messageDetailNormalizesCurrentWebsiteResponse` `:1113` |
| `/api/messages/stats` | GET | P | `messageStatsNormalizeCurrentWebsiteCounters` `:1154` |
| `/api/messages/{id}/read` | POST | P, M, B(`id`) | `messageMutationsUseWebsiteMethodsAndPayloads` `:1204` |
| `/api/messages/read` (selected) | POST | P, M, B(`ids` array) | same `:1209` |
| `/api/messages/read` (all) | POST | P, M, B(`all == true`) | same `:1216` |
| `/api/messages/{id}/star` | POST | P, M, B(`starred == 1` **as int**) | same `:1221` |
| `/api/messages/{id}` | **DELETE** | P, M, B(`id`, `permanent == false`) | same `:1226` |
| `/api/messages` (bulk) | **DELETE** | P, M, B(`ids` array) | same `:1234` |
| `/api/messages/settings` | GET | P, M | `messageSettingsUseWebsiteFieldsAndPutPayload` `:1287` |
| `/api/messages/settings` | **PUT** | P, M, B(`enable_notifications`, `enable_email`, `enable_browser_push`, `notification_types` set, `quiet_hours_start`, `quiet_hours_end`, and **`auto_read_after_days` is OMITTED when null** — `assertFalse(body.has(…))`) | same `:1291–1304` |
| `/api/messages/conversations` | GET | P, M, Q(`target_user_id`, `page`, `page_size`) | `messageConversationUsesWebsiteQueryAndNormalizesMessages` `:1309` |
| `/api/messages` (send DM) | POST | P, M, B(`user_id`, `execute_user_id`, `message_type == 8`, `message_title == "来自 Alice 的私信"`, `message_content`) | `sendDirectMessageUsesCurrentWebsitePayload` `:1347` |
| `/api/reader/session-key` | GET | P, M, H(`X-Client-Signature`, `X-Client-Timestamp`, `X-Client-Nonce` all non-blank) | `chapterContentRequestsSignedReaderSessionAndSendsSessionParameter` `:1472` |
| `/api/chapters/{id}/content` | GET | P, Q(`session` == the id returned by the session-key call, `replace_mode=india`, `show_images=1`) | `:1376`, `:1472` |
| `/api/chapters/{id}/content` (AES-GCM) | GET | decrypts `content`+`iv`+`tag` with the base64 `session_key` → `"Decrypted reader body"`; `source == "api"` | `chapterContentDecryptsWebsiteEncryptedPayloadWithReaderSessionKey` `:1520` |
| `/api/favorites` | GET | P, Q(`page`, `limit`, `sort_field=updated_at`, `sort_order=desc`, `type=novel`) | `favoritesNormalizesFavoriteArrayAliasesAndSendsBookshelfParameters` `:1560` |
| `/api/favorites` (group) | GET | P, Q(`page`, `limit`, `group_id`, `type=novel`) | `favoritesCanRequestSpecificFavoriteGroup` `:1646` |
| `/api/favorites/status` | GET | P, Q(`object_id`, `type=novel`) | `favoriteStatusNormalizesWebsiteStatusAliasesAndSendsParameters` `:2175` |
| `/api/favorites/groups` | GET | P, Q(`preview_limit=6`, `with_preview=true`) | `favoriteGroupsNormalizesWebsiteGroupAliasesAndSendsPreviewParameters` `:2207` |
| `/api/posts` | GET | P, Q(`page`, `limit`) | `forumPostsNormalizeWebsiteAliasesAndSendReadonlyParameters` `:1664` |
| `/api/posts` | POST | P, M, B(`type`, `title`, `content`, `tags` array, `poll.question`, `poll.options`, `poll.allowMultiple`, `poll.maxChoices`, `poll.endsAt` — **camelCase inside `poll`**) | `createForumPostUsesWebsitePayloadAndReturnsCreatedPostId` `:1724` |
| `/api/posts/{id}` | GET | P | `forumPostDetailNormalizesWebsiteAliases` `:1772` |
| `/api/posts/{id}/comments` | GET | P, Q(`page`, `limit`) | `forumPostCommentsNormalize…` `:1823` |
| `/api/posts/{id}/comments` | POST | P, M, B(`content`, `comment_id` = parent, `reply_to_name`) | `createForumCommentPostsContentAndReplyMetadata` `:2081` |
| `/api/posts/{id}/likes` | POST | P, M | `forumPostLikeAndReactionUseWebsiteMutationEndpoints` `:2107` |
| `/api/posts/{id}/reactions` | POST | P, M, B(`reaction_type`, `award_points`) | same `:2118` |
| `/api/comments` (chapter list) | GET | P, Q(`type=chapter`, `book_id`, `chapter_id`, `page`, `limit`) | `chapterCommentsNormalize…` `:1875` |
| `/api/comments` (book list) | GET | P, Q(`type=book`, `book_id`, `page`, `limit`) | `bookCommentsNormalize…` `:1932` |
| `/api/comments` (create) | POST | P, M, raw-body substrings `"type":"book"` / `"type":"chapter"`, `"book_id":354491`, `"chapter_id":9901`, `"content":"书籍评论"` / `"content":"章节评论"` | `createBookAndChapterCommentsUseWebsiteMutationBodies` `:2005` |
| `/api/comments/{id}/replies` | POST | P, M, B(`"content":"回复内容"`, `"reply_to_name":"beruuz"`) | `commentReplyLikeAndReactionUseWebsiteMutationEndpoints` `:2055` |
| `/api/comments/{id}/likes` | POST | P, M | same `:2062`; forum comment variant `:2134` |
| `/api/comments/{id}/reactions` | POST | P, M, B(`"reaction_type":"award"`, `"award_points":10`) | same `:2066`; forum comment variant `"down"`/`5` `:2138` |
| `/api/comments/{parent}/replies/{reply}/reactions` | POST | P, M, B(`"reaction_type":"emoji:heart"`) | same `:2073` |

Response-normalisation behaviour also pinned by this suite (no request assertion, but a hard
model contract):

- Chapter field aliases `chapter_id`/`chapter_name`/`display_order`/`words`/`created_at` → `Chapter(id,title,number,wordCount,updatedAt)` (`:46`); chapters sorted by `display_order` with numbers renumbered `1,2,3` (`:81`).
- Book detail unwraps `data.novel`; aliases `novel_id`, `novel_title`, `true_name`→`originalTitle`, `author.name`, `cover_path`→absolute `coverUrl`, `photo_true_url`→`fullCoverUrl`, `synopsis`→`description`, `words`, `favorite_count`, `site_read_count`, `source_read_count`, `source_favorite_count`, `status`, `created_at`; `tags` accepts `{name}`, `{title}` and bare strings (`:107`).
- `is_completed: true` → `status == "已完结"` (`:658`).
- `category` + `genre` + comma-string `tags` merge into `["Fantasy","Adventure","Native","Commercial"]`, de-duplicated (`:683`).
- Search array aliases `results`/`novels`/`list`; `author.display_name`, `author_name`; `favoriteCount`/`siteReadCount`/`sourceReadCount`/`sourceFavoriteCount` camelCase accepted; `tags:[{label}]` (`:710`).
- Relative cover paths **without** a leading slash resolve against the base URL (`imagebox/cover/relative-book.jpg`) (`:784`).
- Absolute `photo_url` on `images.novelpia.com` is kept verbatim (`:814`).
- Expanded tag aliases: `category`, `spans` (space-split), `categories[{tag_name}|{value}|{tag:{name}}]`, `novel_tags` (comma string), `tag_list[{text}]`, `tag_relations[{tag:{name}}]` → exact ordered list `["规则","R19","完结","日常","现代","治愈","电视剧","言情","心理","冒险"]` (`:843`).
- A **bare image host** `https://images.novelpia.com` (no path) → `coverUrl == null`, but `spans` tags are still parsed (`:879`).
- Favorites prefer `object_id` over `id` (`1673` not `382566`), read `novel_read`/`novel_like` as live counters, and split `spans` `"15 PLUS 独家 连载中"` into tags with `novel_type` first (`:1606`).
- Forum post `type: "book_review"` → `category == "书评"`, and `"书评"` is prepended to the tag list (`:1664`). `type: "discussion"` → `"讨论"` (`:1772`).
- Forum post `content` HTML is stripped for `excerpt` (`<p>Readable excerpt</p>` → `Readable excerpt`) but kept as raw HTML in `detail.content` (`:1664` vs `:1772`).
- `forumPosts` normalises `author.id` → `post.authorId` for native profile navigation (`:517`).
- `bookComments` **flattens replies into the same list** — one comment with one reply returns `size == 2`, with the reply carrying `parentCommentId` and `replyToName` (`:1932`). `helpfulCount`/`notHelpfulCount`/`funnyCount`/`awardCount` map to `dislikeCount`/`reactionCount`/`awardPoints`.
- Chapter content aliases: empty `body_html` falls through to `bodyHtml` (`:1376`); `illustrations[{id,index,src}]` with relative `src` resolved absolute, and the `[[img:2]]` placeholder is **left in the content string** (`:1426`).
- Message metadata: `extra_data.book_id` numeric `354491` → the **string** `"354491"` (`:1098`); `read_at`, `user_id`, `execute_user_id`, `avatar`, `avatar_frame` (`:1037`).
- `messageStats.unreadByType` keys parse from JSON string keys `"8"`/`"9"` to Int (`:1154`).
- `currentUser` nested alias path `data.profile.{uid,nickname,user_role}` (`:2147`).
- `favoriteStatus` aliases `isFavorite`, `status_text`→`rawState`, `favorite_group.id`→`groupId` (`:2175`).
- Managed-book permissions map `title_translation`→`titleTranslation` etc. (`:604`); `updateManagedBook` result carries `failed_fields` (`:608`).

### 3.2 `AdminApiTest.kt` — 7 tests

Uses full-path assertions (`request.path`, including query string) — these are the most
brittle-but-precise assertions in the suite because they pin **parameter order**:

| Endpoint | Asserted request | Test (`AdminApiTest.kt:`) |
|---|---|---|
| `/api/admin/overview?days=7` | exact path | `:48` |
| `/api/admin/review-settings` | exact path (GET) | `:49` |
| `/api/admin/review-requests?page=1&page_size=100&type=upload&status=pending&q=book` | exact path — note `q`, not `keyword` | `:50` |
| `/api/admin/key-management` | exact path (GET) | `:68` |
| `/api/admin/operation-logs?page=1&page_size=20&keyword=done` | exact path | `:69` |
| `/api/admin/operation-logs?page=3&page_size=20&action=…&status=…&user_id=…&novel_id=…&keyword=…&start_date=…&end_date=…` | exact path, all 9 params in this order | `:87` |
| `/api/admin/cookie-config` | exact path (GET) | `:106` |
| `/api/admin/baseurl-rules` | exact path (GET) | `:107` |
| `/api/admin/scheduler-logs?lines=100` | exact path | `:108` |
| `/api/admin/shop/items?type=frame&is_active=true&keyword=Blue&page=1&page_size=100` | exact path | `:121` |
| `/api/admin/review-settings` | POST + body contains `"auto_approve_upload":true` | `:136` |
| `/api/admin/review-requests` | POST + body contains `"action":"approve"` | `:140` |
| `/api/admin/key-management` | **PUT** + body contains `"approval_status":"approved"` | `:144` |
| `/api/admin/key-management?id=4` | **DELETE** | `:148` |
| cookie config save | **PUT** | `:173` |
| cookie config delete | **DELETE** + body contains `"id":2` | `:174` |
| baseurl rule save | **PUT** | `:177` |
| `/api/admin/baseurl-rules?id=4` | **DELETE** | `:178` |
| shop item save | **PUT** | `:179` |
| `/api/admin/shop/items?id=6` | **DELETE** | `:180` |

Response shapes pinned: `stats.{pending_review_total,pending_review_upload,pending_review_delete,novel_active_total,user_registered_total,recent_user_daily[{date,count}]}`;
`settings.{auto_approve_upload,auto_approve_delete}`; review request `list[{id,type,status,username,novel_id,title,created_at}]`;
keys `data[{id,name,model,provider_name,approval_status,base_url}]`; logs `{logs[…],total,total_pages,action_types[…]}`;
cookie configs `configs[{id,config_key,description,proxy_ip,is_active,updated_at}]`;
baseurl rules `data[{id,pattern,action,description}]`; scheduler logs `{logs[String],total_lines,file_size_mb,last_modified}`;
shop items `items[{id,name,description,price,type,image_url,is_active}]` where `is_active` is
**`1`/`0` as int** and `image_url` is resolved absolute (`:113–124`).

### 3.3 `BookManagementApiTest.kt` — 5 tests

| Endpoint | Method | Asserted | Test line |
|---|---|---|---|
| `/api/users/me/chapters/reorder` | POST | P, B(`ordered_chapter_ids == [3,1,2]`, order preserved) | `:46` |
| `/api/users/me/chapters/insert` | POST | P, B(`insert_at == 2`) | `:52` |
| `/api/users/me/chapters/{id}` | **PATCH** | P, M | `:56` |
| `/api/users/me/chapters/{id}` | **DELETE** | P, M | `:60` |
| `/api/users/me/chapters/batch-delete` | POST | P, B(`novel_id`) | `:64` |
| `/api/users/me/novels/{id}/translation-requests` | POST | P, B(`mode == "shared"`) | `:68` |
| `/api/users/me/chapters/append` | POST | P, M, H(multipart), B(`existing_novel_id`, `submit_type`, `chapters`, **`chapters_md5`**) | `:74` |
| `/api/users/me/chapters/{id}/illustrations` | GET | P, M | `:134` |
| `/api/users/me/chapters/{id}/illustrations/{imageId}` | **DELETE** | P, M | `:138` |
| `/api/users/me/chapters/{id}/illustrations` | POST | P, M, H(multipart), B(`name="chapter_id"`, **`name="illustrations[]"; filename="a.png"`** with per-part `Content-Type: image/png`, raw bytes) | `:144` |
| `/api/users/me/novels/{id}/transfers` | POST | P, M, B(`identifier` **trimmed** — `" uid:100002 "` → `"uid:100002"`) | `:196` |
| `/api/users/me/novels/{id}/permissions` | **PATCH** | P, M, B(`allow_download == 1` int, `download_threshold_type`, `download_threshold_value`, `read_threshold_type`, `read_threshold_value`) | `:201` |

Illustration list responses normalise both `imagebox/chapter/a.png` (no leading slash) and
`/imagebox/chapter/b.png` to absolute URLs (`:130`).

### 3.4 `WorkspaceApiTest.kt` — 3 tests

Note: the workspace endpoints are **not** under `/api/`.

| Endpoint | Method | Asserted | Test line |
|---|---|---|---|
| `/workspace/apis` | GET | P | `:105` |
| `/workspace/cookie-status` | GET | P | `:106` |
| `/workspace/cookie-config` | GET | P | `:107` |
| `/workspace/stats` | GET | P | `:108` |
| `/workspace/translator-health` | GET | P | `:109` |
| `/workspace/apis` | POST | P, M, B(`name`, `model`, `endpoint`, **`key`** not `api_key`, `concurrency`) | `:120` |
| `/workspace/apis/{id}` | **PUT** | P, M, B(`name`) | `:131` |
| `/workspace/apis/{id}` | **DELETE** | P, M, **`bodySize == 0`** (no body) | `:136` |
| `/workspace/cookie-config` | POST | P, M, B(`config_key`, `cookie_raw`, `proxy_ip`, `is_active`) | `:152` |
| `/workspace/cookie-config` | **PUT** | M, B(`id`, `description`, `proxy_ip == ""`, **`cookie_raw` OMITTED when null**, `is_active == false`) | `:162` |
| `/workspace/cookie-config` (toggle) | **PUT** | M, B(`id`, `is_active`) | `:172` |
| `/workspace/cookie-config` | **DELETE** | M, B(`id`) — DELETE **with** a body here, unlike `/workspace/apis/{id}` | `:179` |

Response shape pinned: api config aliases `key`→`apiKey`, `is_active`/`is_healthy` as ints,
`totalRequests`; cookie config `myConfigs`→`myConfigs` and **`otherConfigs`→`sharedConfigs`**;
health `apiStatus.{total,active,healthy,total_requests}` and
`translators[{isHealthy,isActive,responseTime→responseTimeMs,successRate}]` (`:78–103`).

### 3.5 `UploadApiTest.kt` — 3 tests

| Endpoint | Method | Asserted | Test line |
|---|---|---|---|
| `/api/uploads/books` | POST | P, M, H(`content-type` starts `multipart/form-data; boundary=`), B(`title`, `title_translation`, `author_name`, `language`, `is_adult == "1"`, `tags` as **comma-joined string** `fantasy,romance`, `submit_type`, `chapters`, **`chapters_md5`**, `epub_file` with `filename="sample.epub"` and raw bytes) | `:36` |
| `/api/uploads/chunks` | POST ×3 | P, M, B per chunk(`file_id`, `chunk_index`, `total_chunks`) | `:80` |
| `/api/uploads/chunks` (merge) | POST | H(`content-type` = `application/json`), B(`action == "merge"`, `file_id`, `file_name`, `total_chunks == 3`) | `:104` |
| `/api/uploads/epubs` | POST | P, M, B(`file_path`, `parse_only == true`) | `:138` |

`largeEpubChunkUploadMatchesWebsiteFiveMiBProtocolWithoutWholeFileBuffer` (`:80`) is
significant: it pins that chunking is **streaming** (the test drives it with `chunkSizeBytes = 4`
over a 10-byte source) rather than buffering the whole file.
Server-side EPUB parse normalises `hierarchy_level`, `section_path`, `raw_path`, `spine_index`
and renumbers `chapterNumber` from the array index (`:131–137`).

---

## 4. Presentation-helper tests — the pinned user-visible strings

These are the tests that make a UI redesign safe: they pin exact Chinese copy. Every string
below is asserted with `assertEquals` (or forbidden with `assertFalse`) and is reproduced
verbatim (`\uXXXX` escapes in source decoded).

### 4.1 `ui/ForumPresentationTest.kt`
- Fallback copy: `bookTitle` → **`站内讨论`**, `authorName` → **`匿名用户`**, `lastActiveLabel` → **`刚刚`** (`:44`, re-asserted `:200`).
- `forumFeedBadges` order: pinned badge **`置顶`** first, then featured **`精华`**, then category, then up to **2** tags — `["置顶","精华","书评","热议","长评"]` from a 3-tag item (`:66`); non-pinned → `["章节","剧情","伏笔"]` (`:83`); `["动态"]` alone (`:62`).
- `forumFeedMetaLine` = **`运营记录 · 站内公告 · 23分钟前`** — a single line joined with ` · `, and asserted to contain neither `API` nor `fallback` (`:99`).
- `forumFeedMetricLabels` = **`["80 条回复", "赞 81", "表情 12", "打赏 7", "7305 次浏览"]`** (`:118`, re-asserted `:200`).
- `forumActionBarLabels()` = **`["赞", "踩", "表情", "打赏", "网页"]`** (`:140`, `:227`).
- `forumCommentThreadSummary` = **`3 条评论 · 2 条回复`** (`:196`, `:229`).
- `forumContentLinks` extracts bare URLs from paragraphs, stripping a trailing Chinese full stop `。` (`:145`); `forumCommentLinkPreviews` does the same from `<p>`-parsed comment HTML (`:164`).
- `forumCommentThreads` groups replies under parents and keeps **orphans** (parent not on this page) as top-level threads — `[1,3,4]` with replies `[2,5]` under thread 1 (`:182`).

### 4.2 `ui/ProfilePresentationTest.kt`
- `profileWebsiteFacts` = **`["积分 3210", "作品 4", "评论 29", "连续签到 3 天"]`** (`:15`).
- `isAdminProfile` requires the exact literal `"admin"` — `"ADMIN"` and `"administrator"` are **not** admin, and `null` is not admin (`:31`).
- `profileAccountStatusLabels(active)` = **`["账号正常", "成年已验证", "邮箱已绑定", "注册 2026-01-02", "签到公开", "自动签到未开"]`** (`:39`).
- `profileAccountStatusLabels(banned)` = **`["账号封禁至 2026-08-09", "封禁原因 spam", "成年未验证"]`** (`:64`).
- `profileAccountStatusLabels(deleted)` = **`["账号已删除"]`** (`:68`).
- `profileOverview` signed-in: title **`我的`**, subtitle **`账号、阅读偏好和连接设置`**, syncLabel **`已同步`**, roleLabel **`管理员`**, stats **`["阅读 章节 8001", "字号 19sp", "主题 护眼", "连接 已启用"]`** (`:72`).
- `profileOverview` guest: accountName **`未登录`**, syncLabel **`未同步`**, roleLabel **`普通用户`**, stats **`["阅读 无进度", "字号 18sp", "主题 系统", "连接 未启用"]`** (`:90`).
- `profileOverview` token-present-but-refresh-failed: accountName **`账号已同步`**, syncLabel **`已同步`**, roleLabel **`身份待同步`** — i.e. a network failure must **not** render as logged out (`:106`).
- `resolveUserLoadResult(remote = Result.failure(IOException), tokenProfile = cached)` returns `LoadResult.Success(cached)` (`:121`). **This is the only test that exercises a function declared in `NovalPieViewModel.kt`.**
- `profileSectionTitles()` = **`["账号", "阅读偏好", "连接设置", "网页入口"]`**; `profileAccountActions(true)` = **`["同步账号", "网页登录", "退出同步"]`**; `profileAccountActions(false)` = **`["同步账号", "网页登录"]`**; `profileWebActions()` = **`["打开网站", "网页搜索"]`** (`:133`).
- **Forbidden-word guard** (`:141`): none of the above copy may contain `Package`, `role:`, `API`, `诊断`, `书源`, `规则`, `爬取`, `下载`, `净化`, `编辑源` (case-insensitive).

### 4.3 `ui/ProductCopyTest.kt`
- `productHeader(Library)` = **`ProductHeader("书架", "收藏、分组和阅读进度")`**; `productHeader(Discover)` = **`("发现", "搜索作品、作者和标签")`**; `productHeader(Profile)` = **`("我的", "账号、阅读偏好和连接设置")`** (`:9`).
- Every `ProductSurface` header must be mojibake-free and contain neither `API` nor `诊断` (`:16`).
- `accountSyncSummary(true)` = **`登录同步: 已连接`**; `(false)` = **`登录同步: 未同步`** (`:29`).
- `libraryPrimaryActions()` = **`["同步书架", "登录同步", "网页收藏"]`** and **exactly 3** items (`:35`).
- `discoverPrimaryActions()` = **`["搜索", "网页发现"]`** and **exactly 2** items (`:41`).
- `discoverFilterLabels()` = **`["排序", "顺序", "范围", "内容", "字数", "来源", "模式"]`** (`:47`).
- `discoverSelectedFilterSummaries` = **`["排序: 收藏数", "顺序: 降序", "范围: 仅标签", "内容: 仅成人", "字数: 10-50万", "来源: NovelPia", "模式: AI搜索"]`** (`:52`).
- `bookDetailSectionTitles()` = **`["作品", "阅读", "章节目录", "评论区"]`** (`:70`).
- `readerScreenTitle()` = **`阅读`**; `readerCatalogTitle()` = **`章节`** (`:78`).
- `forumFeedTabs()` = **`["全部", "书评", "章节", "动态"]`**; `forumFeedItems()` has **exactly 6** items, at least one `pinned`, at least one `featured`, every title ≤ **18** characters, every item has 1–3 non-empty tags, and `bookTitle`/`authorName`/`lastActiveLabel` are all non-blank (`:95`).
- **Mojibake guard** `assertCleanVisibleCopy` (`:124`) rejects any of these 20 GBK-mojibake fragments in visible copy: `涔 鏀 銆 闃 鍙 鎼 浣 璐 鐧 缃 鎺 椤 鑼 鍐 瀛 妯 绔 璇 鍔 婧`.
- Forum card copy must not contain `参考`, `建议中`, `fallback`, `API` (`:84`); forum feed copy must not contain `书源`, `规则编辑`, `爬取`, `下载源`, `净化`, `fallback`, `API` (`:113`).

### 4.4 `ui/NovelCardFactsTest.kt`
- `novelCardFacts` = **`["状态 连载中", "字数 1,234,567", "收藏 2,345", "本站阅读 120,000", "源阅读 980,000", "源收藏 45,000", "更新 2026-07-02"]`** — space separator, thousands separators, and the update date **truncated to `yyyy-MM-dd`** (`:9`).
- Blank/`null` values are skipped entirely → `emptyList()` (`:39`).
- `novelCardTags` trims, drops empties, de-duplicates, and keeps the **complete** set (5 tags, not capped) (`:54`).
- `novelDisplayCoverUrl` prefers a trimmed `fullCoverUrl`, else trimmed `coverUrl` (`:67`).
- `novelSearchPreview`: `platformLabel` maps `"upload"` → **`上传`** and `"novelPia"` → **`NovelPia`**; `originalTitleLabel` is `null` when the original title equals the title after trim; facts subset = **`["状态 连载中", "字数 1,234,567", "收藏 2,345", "本站阅读 120,000", "更新 2026-07-02"]`** (note: **no** source counters in the compact card) (`:92,:118`).

### 4.5 `ui/DiscoverPresentationTest.kt`
- `discoverOverview(Idle)` = **`DiscoverOverview(title = "发现", subtitle = "搜索作品、作者和标签", hint = "输入关键词、作品名或作者", statusLabel = "就绪")`** (`:11`).
- Status labels: Loading → **`加载中`**, Error → **`错误`**, Success(3) → **`3 个结果`** (`:24`).
- `discoverFilterGroups` labels = `["排序","顺序","范围","内容","字数","来源","模式"]`, and each group's choice labels in exact order (`:31`):
  - 排序: **`["相关度", "更新时间", "上架时间", "收藏数", "本站阅读", "推荐", "源阅读", "字数", "源收藏"]`**
  - 顺序: **`["降序", "升序"]`**
  - 范围: **`["全部内容", "仅标题", "仅作者", "仅标签"]`**
  - 内容: **`["所有", "仅成人", "全年龄"]`**
  - 字数: **`["不限", "10万以下", "10-50万", "50-100万", "100万以上"]`**
  - 来源: **`["全部", "NovelPia", "上传"]`**
  - 模式: **`["AI搜索", "模糊-严格", "模糊-宽松", "精确匹配"]`**
  - Selected values: `字数` → `"100000..500000"`, `来源` → `"novelPia"`, `模式` → `"ai"`.
- `discoverQuickPrompts()` = **`["最近更新", "热门书评", "长篇连载", "完结作品"]`**; `discoverIdleMessage()` = **`输入关键词后搜索，也可以先看推荐方向。`** (`:77`).
- `discoverTagLabels` = **`["异世界 88", "完结"]`** — count appended only when non-null (`:86`).
- `discoverSectionOrder`: Idle+history → `[SearchPanel, History, Tags, Filters, IdlePrompts]`; Success+history → `[SearchPanel, Results, History, Tags, Filters]`; Loading+no-history → `[SearchPanel, Results, Tags, Filters]` (`:98`).
- Forbidden: `书源`, `爬取`, `净化`, `编辑源`, `API`, `fallback` (`:63`). Mojibake guard rejects `闁 婵 闂 缂 濞 閺 閳 娑 閹 閻 缁 鐠 娴` (`:113`).

### 4.6 `ui/ReaderPresentationTest.kt`
- `readerSourceDebugLine("api"|"fallback"|"")` must be **`null`** — no debug source line in the reader (`:11`).
- `readerDebugIdentityLine(bookId, chapterId)` must be **`null`** (`:18`).
- `readerToolbarLabels()` = **`["上一章", "目录", "下一章", "A-", "A+", "主题", "网页"]`** (`:23`).
- `readerCatalogPanelTitle()` = **`章节目录`**; `readerCloseCatalogLabel()` = **`回到正文`**; `readerSurfaceSections()` = **`["正文", "目录", "设置"]`** (`:31`).
- `globalProductTopBarVisible(AppRoute.Reader(…))` is **false** — the reader owns its own chrome (`:52`).
- `readerTopBarLabels()` = **`ReaderTopBarLabels(back = "返回", title = "阅读", web = "网页")`** (`:57`).
- Illustration copy (functions live in `NovalPieApp.kt`): `readerIllustrationLabel(null, 1)` = **`正文插图 1`**; blank alt → **`正文插图 1`**; `" 章节插画 "` → **`章节插画`**; `readerIllustrationContentDescription("正文插图 1")` = **`正文插图 1，点击或长按查看大图`**; `readerIllustrationPreviewHint()` = **`点击 / 长按看大图`**; `readerIllustrationLoadingLabel()` = **`正在加载插图`**; `readerIllustrationErrorLabel()` = **`插图加载失败`** (`:62`).
- `chapterCommentMetricLabels` = **`["赞 8", "踩 1", "表情 3", "打赏 20", "回复"]`**; `chapterCommentsSectionTitle()` = **`章节评论`**; `chapterCommentsFallbackLabel()` = **`打开网页评论`** (`:73`).
- Forbidden in all reader labels: `书源`, `规则`, `编辑`, `爬取`, `下载`, `净化` (`:38`).

### 4.7 `ui/MessagePresentationTest.kt`
- `directMessageTargetUserId` returns the *other* participant in both orientations (`:11,:24`).
- `validateMessageSettings`: valid defaults → `null`; `quietHoursStart = "25:99"` → **`免打扰开始时间格式无效`**; `autoReadAfterDays = -1` → **`自动已读天数不能小于 0`** (`:37`); `quietHoursEnd = "7pm"` → **`免打扰结束时间格式无效`** (`:50`).
- `mergeMessagePages` de-duplicates by id, keeps **the newer version's** content (`"b2"` wins over `"b"`), and preserves order `[1,2,3]` (`:58`).
- `messageTypeLabel(null)` = **`全部类型`**; `(1)` = **`用户互动`**; `(8)` = **`私信`**; `(10)` = **`举报通知`**; `(99)` = **`未知类型`**; `messageTypeOptions()` values = `1..10` (`:75`).
- `toggleMessageSelection` / `selectVisibleMessages` are deterministic set operations (`:85`).

### 4.8 `ui/PoliticalExamPresentationTest.kt`
- Signed-in: title **`政治考试`**, statusLabel **`已登录`**, primaryAction **`开始考试`**. Signed-out: **`需要登录`** / **`登录后参加考试`** (`:11`).
- `stats` = **`["100 题", "30 分钟", "80 分通过", "每日次数受限"]`**.
- `rules` = **`["40 道单选题，每题 1 分", "10 道多选题，每题 2 分，必须全部选对", "25 道判断题，每题 1 分", "25 道填空题，每题 1 分", "开始与提交都会同步源站账号状态"]`**.
- `politicalExamAnsweredCount` counts only completed answers across all four types (nulls, empty lists and whitespace-only strings do not count) → `4`; `formatPoliticalExamTime(125)` = **`02:05`** (`:34`).
- `politicalExamCorrectSummary(result, "single_choice")` = **`2 / 3`**; missing key → **`0 / 0`** (`:47`).

### 4.9 `ui/ForumCreatePresentationTest.kt`
- `forumCategoryOptions(isAdmin = false).map { it.id }` = `["recommend","discussion","feedback"]`; `isAdmin = true` prepends `"announcement"` (`:11`).
- Duplicate poll options → **`投票选项不能重复`** (`:43`).
- Non-admin posting `type = "announcement"` → **`只有管理员可以发布公告`** (`:43`).
- A valid draft returns `canSubmit == true, message == null` (`:23`).

### 4.10 `ui/BookDetailFactsTest.kt`
- `bookDetailFacts` = **`["状态: 连载中", "作者: Author Name", "来源: 上传", "字数: 1,234,567", "收藏: 2,345", "本站阅读: 120,000", "源阅读: 980,000", "源收藏: 45,000", "更新: 2026-07-02T08:30:00Z"]`** — note the **colon-space** separator (unlike `novelCardFacts`, which uses a plain space) and the **full ISO timestamp** (unlike `novelCardFacts`, which truncates) (`:10`).
- Blank/null → `emptyList()` (`:45`). Mojibake guard rejects `鐘 浣 瀛 鏇 €`.

### 4.11 `ui/BookManagementPresentationTest.kt`
- `validateBookAccessPolicyDraft`: read threshold `points_pay` max **50** → **`阅读门槛 不能超过 50`**; download threshold `points_min` max **100** → **`下载门槛 不能超过 100`**; at the limits → `null` (`:9`).
- `bookAccessPolicyFromDraft` with `allowDownload = false` forces `downloadThresholdType = "none"` and `downloadThresholdValue = 0`, leaving the read threshold intact (`:37`).
- `chapterIllustrationPlaceholder(0)` = **`[[img:1]]`**; `(3)` = **`[[img:3]]`** — i.e. index 0 maps to 1, but non-zero passes through unchanged (`:56`).

### 4.12 `ui/BookDetailPresentationTest.kt`
- `bookDetailPrimaryActions(hasProgress = true)` = **`["继续阅读", "开始阅读", "网页详情"]`**; `false` = **`["开始阅读", "网页详情"]`** (`:10`).
- `bookDetailFavoriteLabel(true)` = **`已收藏`**; `(false)` = **`未收藏`**; `bookDetailFavoriteLoadingLabel()` = **`收藏同步中`**; `bookDetailFavoriteUnavailableLabel()` = **`收藏状态不可用`** (`:31`).
- `bookCommentMetricLabels` = **`["赞 4", "踩 1", "表情 3", "打赏 5", "回复"]`**; `bookCommentsSectionTitle()` = **`评论区`**; `bookCommentsFallbackLabel()` = **`打开网页评论`** (`:39`).
- Forbidden in primary actions: `书源`, `规则`, `爬取`, `下载`, `净化`, `编辑源` (`:22`).

### 4.13 `ui/UiNavigationTest.kt`
- `BottomTab.values()` order = **`[Collection, Discover, Tools, Forum, Profile]`** (`:8`).
- `BottomTab.title` = **`["收藏", "搜索", "工具", "论坛", "我的"]`** (`:16`); `bottomTabDisplayLabel` same values (`:21`); `bottomTabShortLabel` = **`["收", "搜", "工", "论", "我"]`** (`:30`).
- `routeContextLabel` (`:39`): `MessageCenter` → **`消息中心`**, `MessageDetail` → **`消息详情`**, `MessageConversation` → **`私信`**, `MessageSettings` → **`消息设置`**, `Workspace` → **`工作区`**, `UploadBook` → **`上传书籍`**, `UploadEditor` → **`EPUB 编辑器`**, `Home`+Collection → **`收藏`**, `ForumPostDetail` → **`帖子详情`**, `BookDetail` → **`书籍详情`**, `Reader` → **`阅读`**.

### 4.14 `ui/LibraryPresentationTest.kt`
- `libraryOverview(hasAuthToken = true, 12, 3, 2)` = **`LibraryOverview(title = "书架", subtitle = "继续阅读、收藏分组和最近进度", syncLabel = "已同步", stats = ["收藏 12", "分组 3", "最近 2"])`** (`:9`).
- `hasAuthToken = false` → syncLabel **`未同步`**; no copy may contain `API` or `fallback` (`:27`).
- `libraryContinueTitle(hasProgress = true)` = **`继续阅读`**; `false` = **`阅读记录`**; `libraryContinueActions()` = **`["继续阅读", "清除"]`**; `libraryFavoritesTitle()` = **`收藏书籍`** (`:43`).

### 4.15 `ui/VisibleUiLabelsTest.kt`
- `VisibleUiLabels.{ForumPostDetail, Comments, CommentSubmit, FavoriteGroups, Bookshelf, Search, BookDetail, ChapterCatalog, ChapterComments}` = **`["帖子详情", "评论", "评论提交", "收藏分组", "书架", "搜索", "书籍详情", "章节目录", "章节评论"]`** (`:9`). These are the labels fed to `apiFailureMessage`.
- `forumPostActionLabel(Like|Dislike|Emoji|Award)` = **`点赞` / `点踩` / `表情` / `打赏`**; `forumCommentActionLabel(...)` = **`评论点赞` / `评论点踩` / `评论表情` / `评论打赏`** (`:30`).
- Mojibake guard rejects `甯 璇 鐐 鎼 涔 鏀 琛 鎵 绔`.

### 4.16 `ui/UploadPresentationTest.kt`
- `validateUploadBookDraft`: empty → **`请输入书名`**; title only → **`请输入作者`**; title+author → **`请先选择并解析 EPUB 文件`**; with chapters and `submitType = "shared"` → `null`; `submitType = "invalid"` → **`提交方式无效`** (`:9`).
- `normalizeUploadTags(" 奇幻,恋爱，奇幻\n冒险 ")` = **`["奇幻", "恋爱", "冒险"]`** — splits on ASCII comma, **fullwidth comma `，`** and newline; trims; de-duplicates (`:30`).
- `WEBSITE_UPLOAD_CHUNK_BYTES == 5 MiB`; `WEBSITE_SERVER_EPUB_THRESHOLD_BYTES == 50 MiB`; `uploadParseMode(50 MiB) == LOCAL`; `uploadParseMode(50 MiB + 1) == SERVER_CHUNKED` (boundary is inclusive-local) (`:38`).

### 4.17 `ui/CatalogSummaryTest.kt`
- Unfiltered = **`共 3 章 · 当前第 2 章`** (`:9`).
- Filtered = **`共 3 章 · 已筛选 2 章 · 当前第 3 章`** — the current position is computed against the **full** catalog, not the filtered view (`:23`).
- Empty = **`目录未加载`** (`:38`).

### 4.18 `ui/ToolsPresentationTest.kt`
- `toolsEntries(isAdmin = false).map { it.path }` = **`["/messages", "/workspace", "/upload", "/upload-editor", "/political-exam"]`**, none `adminOnly` (`:10`).
- `isAdmin = true` additionally contains `adminOnly` entries for `/admin`, `/admin/review`, `/admin/key-management`, `/admin/operation-logs`, `/admin/scraper-management`, `/admin/shop` (`:21`).
- `messageTypeLabel(8|9|10|99)` = **`私信` / `系统公告` / `举报通知` / `未知类型`** (`:33`).

### 4.19 `ui/ReaderProgressLabelTest.kt`
- Matched chapter = **`第 2 / 3 章 · 第二章`** (`:9`).
- Unmatched chapter, non-empty catalog = **`当前章节 99 · 目录共 2 章`** (`:20`).
- Empty catalog = **`当前章节 99 · 目录未加载`** (`:30`).

### 4.20 `ui/ThemePaletteTest.kt` — exact colour tokens

| Token | Light (`:9`) | Dark (`:22`) |
|---|---|---|
| `background` | `0xFFF2F2F2` | `0xFF191C1F` |
| `surface` | `0xFFFFFFFF` | `0xFF23262A` |
| `primary` | `0xFF3182ED` | `0xFF4D9DFF` |
| `secondaryContainer` | `0xFFEDF0F2` | `0xFF2A2F34` |
| `onSurface` | `0xFF45525E` | (not asserted) |
| `outline` | `0xFFCED4DA` | (not asserted) |

Both palettes additionally assert `primary != secondary`.

### 4.21 `ui/WorkspacePresentationTest.kt`
- `maskWorkspaceApiKey("sk-secret-value")` = **`sk-s******alue`** (first 4 + 6 stars + last 4); short key → **`********`**; `null` → **`未配置`** (`:9`).
- `validateWorkspaceApiDraft`: empty → **`API 名称不能为空`**; `endpoint = "file://x"` → **`API 端点必须是 http(s) URL`**; `concurrency = "0"` → **`并发数必须介于 1 到 100`**; valid → `null` (`:16`).
- `validateWorkspaceCookieDraft`: no key → **`配置键名不能为空`**; no cookie → **`Cookie 内容不能为空`**; `proxyIp = "bad"` → **`代理格式应为 IP:PORT 或 http(s)://...`**; valid → `null` (`:24`).

### 4.22 `ui/ApiFailureMessageTest.kt`
- `apiFailureMessage("搜索", IllegalStateException("timeout"))` = **`搜索请求失败: timeout`** (`:8`).
- Blank throwable message → falls back to the simple class name: **`书籍详情请求失败: RuntimeException`** (`:15`).
- The `/API` label suffix is stripped and an endpoint path is replaced by a status summary:
  `apiFailureMessage("阅读器正文/API", IllegalStateException("NovalPie API 400: /api/chapters/8001/content"))` = **`阅读器正文请求失败: 服务返回错误 400`** (`:22`).
- Implementation: `ui/ApiMessages.kt:5` (`"${visibleFailureLabel(label)}请求失败: ${visibleFailureDetail(detail)}"`), `:9` blank label → **`请求`**, `:12` regex `NovalPie API (\d+)`.

### 4.23 `ui/AdminPresentationTest.kt`
- `adminReviewTypeOptions()` = **`["" to "全部", "upload" to "上传", "delete" to "删除"]`** (`:8`).
- `adminReviewStatusOptions()` = **`["" to "全部", "pending" to "待审核", "approved" to "已通过", "rejected" to "已拒绝"]`** (`:8`).
- `adminOperationStatusOptions()` = **`["" to "全部", "success" to "成功", "failed" to "失败", "pending" to "处理中"]`** (`:20`).
- `adminOperationActionOptions(list)` prepends `"" to "全部"`, de-duplicates, drops blanks, and uses the raw action id as its own label (`:20`).

### 4.24 `ui/BookCoverFallbackTest.kt` — pins geometry inside `NovalPieApp.kt`
- `bookCoverAspectRatio() == 2f/3f` (`:8`); `novelGridColumnCount() == 2` (`:13`).
- `bookCoverFallbackText(" NovalPie")` = `"N"`; `("  书名")` = **`书`**; blank/empty → `"N"` (`:18,:24`).

### 4.25 `ui/ErrorRecoveryTest.kt`
- `retryActionLabel("搜索")` = **`重试搜索`**; `(" 书籍详情 ")` = **`重试书籍详情`** (trimmed); blank → **`重试`** (`:8,:14`).

---

## 5. Coverage map — what IS and IS NOT covered

### 5.1 Answers to the three specific questions

| Question | Answer |
|---|---|
| **Is `NovalPieViewModel.kt` (4063 L) covered?** | **Essentially no.** Zero tests instantiate it (`grep NovalPieViewModel` in the test tree → no hits). Exactly **one** function declared in it is tested: `resolveUserLoadResult` (`ProfilePresentationTest.kt:121`). Five *types* declared in it are used as test fixtures — `AppRoute`, `BottomTab`, `BookDetailState`, `ReaderState`, `SearchOptions`, `ReaderUiOptions`, `PersistedSearchSettings` — via `RequestFreshnessTest`, `RouteStackPolicyTest`, `UiNavigationTest`, `ReaderPresentationTest`, `ProfilePresentationTest`. **No state transition, no coroutine, no store interaction, no error path, no pagination, no deep link, no cookie/proxy wiring in the ViewModel is tested at all.** |
| **Is `NovalPieApp.kt` (3654 L, Compose UI) covered?** | **No composable is covered.** Four non-composable helpers that happen to live in that file are tested: `bookCoverAspectRatio`, `novelGridColumnCount`, `bookCoverFallbackText` (`BookCoverFallbackTest`), and `readerIllustrationLabel` / `readerIllustrationContentDescription` / `readerIllustrationPreviewHint` (`ReaderPresentationTest.kt:62`). Also `filterBooks`, `LibraryOverview`, `ProfileOverview`, `ForumCommentThread` are declared or re-declared in this file. **No `@Composable` is ever invoked by a test.** |
| **Any instrumentation / Compose UI tests?** | **None.** `app/src/androidTest` does not exist. No `createComposeRule`, `ComposeTestRule`, `onNodeWith*`, or `setContent` anywhere in `app/src/test`. The baseline `app/build.gradle` has no `androidTestImplementation` line and no `testInstrumentationRunner`. The only runtime UI verification is the external ADB script `tools/verify-mumu-compose-launch.ps1`, which asserts the single string `NOVALPIE_NATIVE_COMPOSE_HOME` appears in a `uiautomator dump` — i.e. **one smoke assertion for the entire UI**. |

### 5.2 Production files WITH test coverage

| Production file | Lines | Covering test(s) |
|---|---|---|
| `data/NovalPieApi.kt` | 3404 | `NovalPieApiTest`, `AdminApiTest`, `BookManagementApiTest`, `UploadApiTest`, `WorkspaceApiTest` (103/106 fns invoked) |
| `data/AuthSessionStore.kt` | 67 | `AuthTokenProfileTest` (only `decodeAuthTokenProfile`; the SharedPreferences read/write path is untested) |
| `data/EditorArchiveStore.kt` | 110 | `EditorArchiveStoreTest` |
| `data/EditorProcessor.kt` | 120 | `EditorProcessorTest` |
| `data/EpubParser.kt` | 176 | `EpubParserTest` |
| `data/EpubWriter.kt` | 104 | `EpubWriterTest` |
| `data/NetworkConfigStore.kt` | 126 | `NetworkConfigStoreTest` |
| `data/NovalPieImageLoading.kt` | 42 | `NovalPieImageLoadingTest` |
| `data/ReaderProgressStore.kt` | 87 | `ReaderProgressStoreTest` |
| `data/SearchHistoryStore.kt` | 34 | `SearchHistoryStoreTest` |
| `data/SearchSettingsStore.kt` | 54 | `SearchSettingsStoreTest` |
| `data/WorkspaceLocalStore.kt` | 126 | `WorkspaceLocalStoreTest` |
| `data/UploadFileSource.kt` | 10 | indirectly (fixture in 4 suites) |
| `ui/ApiMessages.kt` | 14 | `ApiFailureMessageTest` |
| `ui/BookChapterPresentation.kt` | 61 | `BookManagementPresentationTest` |
| `ui/BookDetailFacts.kt` | 17 | `BookDetailFactsTest` |
| `ui/BookDetailPresentation.kt` | 24 | `BookDetailPresentationTest` |
| `ui/BookDetailProgressMarker.kt` | 10 | `BookDetailProgressMarkerTest` |
| `ui/BookFilter.kt` | 21 | `BookFilterTest` |
| `ui/CatalogSummary.kt` | 23 | `CatalogSummaryTest` |
| `ui/ChapterFilter.kt` | 19 | `ChapterFilterTest` |
| `ui/DiscoverPresentation.kt` | 141 | `DiscoverPresentationTest` |
| `ui/EditorScriptEngine.kt` | 207 | `EditorScriptContractTest` |
| `ui/ErrorRecovery.kt` | 6 | `ErrorRecoveryTest` |
| `ui/ForumCreatePresentation.kt` | 66 | `ForumCreatePresentationTest` |
| `ui/ForumPresentation.kt` | 123 | `ForumPresentationTest` |
| `ui/LibraryPresentation.kt` | 27 | `LibraryPresentationTest` |
| `ui/MessagePresentation.kt` | 53 | `MessagePresentationTest` |
| `ui/NovalPieTheme.kt` | 118 | `ThemePaletteTest` (tokens only; no `MaterialTheme` wiring test) |
| `ui/NovelCardFacts.kt` | 56 | `NovelCardFactsTest` |
| `ui/PoliticalExamPresentation.kt` | 50 | `PoliticalExamPresentationTest` |
| `ui/ProductCopy.kt` | 220 | `ProductCopyTest`, `ForumPresentationTest` |
| `ui/ProfilePresentation.kt` | 95 | `ProfilePresentationTest` |
| `ui/ReaderAdjacentChapter.kt` | 21 | `ReaderAdjacentChapterTest` |
| `ui/ReaderPresentation.kt` | 44 | `ReaderPresentationTest` |
| `ui/ReaderProgressLabel.kt` | 13 | `ReaderProgressLabelTest` |
| `ui/ReaderText.kt` | 145 | `ReaderTextTest` |
| `ui/RequestFreshness.kt` | 51 | `RequestFreshnessTest` |
| `ui/RouteStackPolicy.kt` | 15 | `RouteStackPolicyTest` |
| `ui/ToolsPresentation.kt` | 63 | `ToolsPresentationTest`, `MessagePresentationTest` |
| `ui/UiNavigation.kt` | 38 | `UiNavigationTest` |
| `ui/UploadPresentation.kt` | 39 | `UploadPresentationTest` |
| `ui/VisibleUiLabels.kt` | 30 | `VisibleUiLabelsTest` |
| `ui/WorkspacePresentation.kt` | 72 | `WorkspacePresentationTest` |
| `ui/AdminScreens.kt` | 849 | **partial** — only `adminReview*Options` / `adminOperation*Options` (~20 of 849 lines) |
| `ui/ImagePreviewDialog.kt` | 164 | **partial** — only `clampImagePreviewScale`/`clampImagePreviewOffset` |
| `ui/WebFallbackScreen.kt` | 176 | **partial** — only `webViewProxyUrl` |
| `ui/NovalPieApp.kt` | 3654 | **partial** — 6 pure helpers only, 0 composables |
| `ui/NovalPieViewModel.kt` | 4063 | **partial** — 1 function (`resolveUserLoadResult`) + 6 types as fixtures |

### 5.3 Production files with ZERO test coverage — the refactor blind spots

| Production file | Lines | Risk note |
|---|---|---|
| `ui/MessageScreens.kt` | **681** | Entire message-centre UI: list, detail, conversation, settings screens. |
| `ui/UploadEditorScreens.kt` | **600** | EPUB editor UI (the JS engine's *contract* is tested, the screen is not). |
| `ui/WorkspaceScreens.kt` | **500** | Workspace dashboard/api/cookie UI. |
| `ui/ProfileScreens.kt` | **465** | Profile screen composables. |
| `ui/BookEditScreens.kt` | **465** | Book-edit UI. |
| `ui/PoliticalExamScreens.kt` | **418** | Exam-taking UI incl. the timer. |
| `ui/UploadScreens.kt` | **409** | Upload wizard UI. |
| `ui/BookChapterScreens.kt` | **377** | Chapter-management UI. |
| `ui/ForumCreateScreens.kt` | **375** | Post-composer UI. |
| `ui/UserProfileScreens.kt` | **250** | Public user profile UI. |
| `ui/EditorPresentation.kt` | **48** | **No test file at all** — the only `*Presentation.kt` with no matching `*PresentationTest.kt`. |
| `ui/BookEditPresentation.kt` | **40** | **No test file** (`BookManagementPresentationTest` covers `BookChapterPresentation.kt`, not this). |
| `data/ReaderSettingsStore.kt` | **34** | **No test file** — the only store with no test. Font size / theme persistence is unverified, yet `ProfilePresentationTest` pins the labels it feeds (`字号 18sp`, `主题 系统`). |
| `model/Models.kt` | **655** | No dedicated test; exercised only as fixtures. Default values (e.g. `ReaderUiOptions()` → 18sp/`"system"`) are pinned indirectly by `ProfilePresentationTest.kt:90`. |
| `MainActivity.kt` | 22 | Deep-link `startUri` plumbing verified only by the `verify-native-project.ps1` grep. |

### 5.4 Behaviours not covered anywhere

- **Every ViewModel state transition**: loading→success→error, retry, pagination
  (`loadMoreFavorites`, `loadMoreSearch`), deep-link routing (`openDeepLink`), cookie
  extraction from `CookieManager`, proxy save/load, auth token clear, reader progress save
  on chapter change, search-history persistence on submit. All of these are asserted only
  *structurally* by grep in `tools/verify-native-project.ps1` (§6), never behaviourally.
- **Every Compose recomposition, layout, and interaction.**
- **`ReaderSettingsStore` round-trip.**
- `adminApproveAllReviews`, public-user `userCheckinStats` (§3.0).
- **Auth token storage** (`AuthSessionStore` save/load/clear) — only JWT decoding is tested.
- **Coroutine cancellation / concurrency** — `runBlocking` only, no `TestDispatcher`, so nothing
  about scope cancellation or race behaviour is pinned.
- **Deep-link URI parsing** (`novalpie://` scheme).
- **R8 / minification** behaviour (baseline had `minifyEnabled false`).
- **Any `strings.xml` content** — baseline `app/src/main/res/values/strings.xml` contains
  exactly one entry, `app_name = "NovalPie 2.0"`. All ~1236 user-visible strings live in Kotlin.

### 5.5 The string golden master (added in the baseline commit — already-existing scaffolding)

`tools/golden_strings.py` (285 L) + `tools/golden/user-visible-strings.txt` (1240 lines,
**1236 unique strings**). This is the second half of the safety net and is *not* a Gradle test.

- Extracts every Kotlin string literal containing CJK from `app/src/main/java/**.kt`, plus every
  `<string>`/`<plurals>`/`<string-array>` value from `app/src/main/res/values*/strings.xml`
  (`golden_strings.py:167`, `:189`).
- Decodes `\uXXXX` escapes (~5000 in this codebase) so a Kotlin literal and the string resource
  it becomes in Phase 3 compare equal (`:58`).
- Collapses Kotlin templates (`$x`, `${x.y}`) and resource format specifiers (`%s`, `%1$d`, `%%`)
  to the single token `<>` (`:53–55`), NFKC-normalises, collapses whitespace (`:90`).
- Strips comments and char literals with a hand-written state machine so `//` inside a string
  literal is not mistaken for a comment (`:104`).
- **Removals fail (exit 1); additions are reported and allowed** (`:264–281`).
- Usage: `py tools/golden_strings.py --write` / `py tools/golden_strings.py` / `--list [--show-origins]`.

---

## 6. Build and tooling

### 6.1 Baseline toolchain (commit `f2cc124`)

| Component | Version | Source |
|---|---|---|
| Gradle wrapper | **8.0.2** (`-all` distribution) | `gradle/wrapper/gradle-wrapper.properties:3` |
| Wrapper `distributionUrl` | `https://mirrors.cloud.tencent.com/gradle/gradle-8.0.2-all.zip` — **Tencent mirror, fails TLS handshake from this machine** | same line |
| `networkTimeout` | `60000` | `gradle-wrapper.properties:4` |
| Android Gradle Plugin | **8.0.0** | `build.gradle:2` |
| Kotlin Android plugin | **1.8.10** | `build.gradle:3` |
| Compose compiler extension | **1.4.3** | `app/build.gradle` `composeOptions.kotlinCompilerExtensionVersion` |
| `compileSdk` / `targetSdk` / `minSdk` | **34 / 34 / 23** | `app/build.gradle` |
| Java source/target + `jvmTarget` | **17** | `app/build.gradle` `compileOptions` + `kotlinOptions` |
| JDK used by the release script | `C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot` | `tools/build-release.ps1:54` |
| Also installed on the machine | `jdk-21.0.10`, `jdk1.8.0_333` | `C:\Program Files\Microsoft\`, `C:\Program Files\Java\` |
| Android SDK | `C:\Users\86188\AppData\Local\Android\Sdk` | `local.properties:1` |
| Build tools (signing) | `34.0.0` | `tools/build-release.ps1:5` |
| `applicationId` | `com.novalpie.app` (`.debug` suffix on debug) | `app/build.gradle` |
| `versionCode` / `versionName` | `2026070601` / `2.0.0-native-alpha1` | `app/build.gradle` |
| `namespace` | `com.novalpie.nativeapp` | `app/build.gradle` |
| Release minification | **disabled** (`minifyEnabled false`) | `app/build.gradle`; `app/proguard-rules.pro` contained a single comment line |

Baseline `gradle.properties`:
```
org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
kotlin.compiler.execution.strategy=in-process
```

Baseline production dependencies (all pinned literals, no version catalog):
`androidx.activity:activity-compose:1.7.0`, `androidx.compose.ui:ui:1.4.3`,
`ui-tooling-preview:1.4.3`, `androidx.compose.foundation:foundation:1.4.3`,
`androidx.compose.material3:material3:1.1.0`,
`androidx.compose.material:material-icons-extended:1.4.3`,
`androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1`, `lifecycle-runtime-ktx:2.6.1`,
`androidx.webkit:webkit:1.8.0`, `io.coil-kt:coil-compose:2.4.0`,
`org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1`,
`com.squareup.okhttp3:okhttp:4.11.0`.
Test dependencies: `junit:junit:4.13.2`, `androidx.test:core:1.5.0`,
`org.robolectric:robolectric:4.11.1`, `com.squareup.okhttp3:mockwebserver:4.11.0`.
Debug: `androidx.compose.ui:ui-tooling:1.4.3`.
`testOptions.unitTests.includeAndroidResources = true` — required by Robolectric.

`settings.gradle`: `pluginManagement` repos `google()`, `mavenCentral()`, `gradlePluginPortal()`;
`dependencyResolutionManagement` with `RepositoriesMode.FAIL_ON_PROJECT_REPOS` and
`google()`, `mavenCentral()`; `rootProject.name = 'NovalPieNative'`; `include ':app'`.

### 6.2 The offline-build requirement — and an important correction

**The documented problem** (`docs/REFACTOR_PLAN_2026-07-26.md:19`, finding B3): the wrapper's
`distributionUrl` points at `mirrors.cloud.tencent.com`, which fails the TLS handshake from this
machine, so the wrapper cannot bootstrap on a clean `GRADLE_USER_HOME`. Builds worked only
because `D:\NovalPie\.gradle-sandbox\wrapper\dists\gradle-8.0.2-all\5owlw31sddu3apboq9xcnpod7\`
already contained an unpacked `gradle-8.0.2` distribution plus a
`gradle-8.0.2-all.zip.ok` marker (created `2026-06-11 16:08`).

**Working baseline build/test command:**

```powershell
$env:GRADLE_USER_HOME = 'D:\NovalPie\.gradle-sandbox'
$env:JAVA_HOME        = 'C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot'
$env:Path             = "$env:JAVA_HOME\bin;$env:Path"
cd D:\NovalPie\native-android
.\gradlew.bat --offline --no-daemon --console=plain :app:testDebugUnitTest
```

Variants used historically in `README.md`:
- `:app:testReleaseUnitTest` (README:265, :1021 — the canonical full-suite task)
- `:app:assembleDebug` / `:app:assembleRelease` (README:267, :269)
- focused: `--tests 'com.novalpie.nativeapp.data.NovalPieApiTest'` (README:1020)
- memory-constrained: `--max-workers=1 --no-watch-fs` (README:1131)
- `GRADLE_USER_HOME=D:\NovalPie\.gradle-sandbox ./gradlew --console=plain :app:testDebugUnitTest`
  (REFACTOR_PLAN:251)

**Correction to the "offline is mandatory" premise.** Observed during this inventory: at
`2026-07-26 14:48` a new distribution `D:\NovalPie\.gradle-sandbox\wrapper\dists\gradle-8.9-bin`
appeared, and `com.android.tools.build:gradle:8.7.3`,
`org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21`,
`org.jetbrains.kotlin.plugin.compose:…:2.0.21`, `androidx.core:core-ktx:1.15.0`,
`androidx.activity:activity-compose:1.9.3`, `androidx.lifecycle:*:2.8.7` and
`androidx.core:core-splashscreen:1.0.1` were freshly downloaded into
`.gradle-sandbox\caches\modules-2`. **`services.gradle.org`, Google Maven and Maven Central are
reachable from this machine right now.** The offline constraint is a property of the *Tencent
mirror URL*, not of the network. `--offline` is still the correct flag for a **reproducible,
fast, no-surprise** build against the cache, and it is required if you keep the Tencent URL.

**Verified failure mode of `--offline` after the toolchain change:** running the baseline command
against the migrated build scripts fails with
`Plugin [id: 'com.android.application', version: '8.7.3'] was not found` — because only the
`.pom` and `.module` metadata for AGP 8.7.3 are cached, not the jars. **An `--offline` build is
therefore currently broken and will stay broken until a networked build has fully populated the
cache.**

### 6.3 Offline cache inventory — `D:\NovalPie\.gradle-sandbox\caches\modules-2\files-2.1`

The cache holds **116 top-level groups**. Versions relevant to a dependency upgrade:

| Artifact | Versions in cache | Upgrade headroom |
|---|---|---|
| **AGP** `com.android.tools.build:gradle` | **8.0.0**, **8.7.3** (metadata only for 8.7.3) | 8.0.0 usable offline; 8.7.3 needs network |
| `com.android.application.gradle.plugin` | 8.0.0 | 8.7.3 marker also present (see §6.6) |
| `com.android.tools.build:aapt2` | `8.0.0-9289358` | pinned to AGP 8.0.0 |
| `com.android.tools.build:bundletool` | 1.13.2 | — |
| **Kotlin** `kotlin-gradle-plugin` | **1.8.10**, **2.0.21** | 2.0.21 was fetched today; 1.8.10 is the only fully-cached one |
| `kotlin-stdlib` | 1.7.10, **1.8.10**, 1.8.21 | 1.8.21 available (bumping stdlib alone is possible offline) |
| `kotlin-compiler-embeddable` | 1.8.10 | Kotlin compile offline is locked to 1.8.10 |
| `kotlin-reflect` | 1.6.10, 1.7.10 | — |
| **Compose compiler** `androidx.compose.compiler:compiler` | **1.4.3** only | **no offline path to any other Compose compiler version** |
| **Compose UI** `androidx.compose.ui:*` | **1.4.3** only (ui, ui-geometry, ui-graphics, ui-text, ui-tooling, ui-tooling-data, ui-tooling-preview, ui-unit, ui-util) | **no upgrade possible offline** |
| `androidx.compose.foundation:foundation`, `foundation-layout` | **1.4.3** only | **no upgrade offline** |
| `androidx.compose.runtime:runtime` | 1.0.1, 1.2.1, **1.4.3** | only downgrades available |
| `androidx.compose.material:material` | 1.0.0, 1.4.1, **1.4.3** | — |
| `androidx.compose.material:material-icons-extended` | **1.4.3** only | — |
| `androidx.compose.animation:animation` | 1.2.1, 1.4.1, **1.4.3** | — |
| **Material3** `androidx.compose.material3:material3` | **1.1.0** only | **no offline path to 1.2/1.3.** `PullToRefreshBox` and other 1.3 APIs are unreachable offline. |
| `androidx.compose:compose-bom` | **ABSENT** | a BOM-based build cannot resolve offline |
| **coil** `io.coil-kt:coil`, `coil-base`, `coil-compose`, `coil-compose-base` | **2.4.0** only | **no upgrade offline** |
| **okhttp** `com.squareup.okhttp3:okhttp` | **4.11.0** only | **no upgrade offline** |
| `com.squareup.okhttp3:mockwebserver` | **4.11.0** only | must stay lock-step with okhttp |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.7.1 | — |
| `kotlinx-coroutines-core` / `-core-jvm` | 1.7.1 / 1.5.0, 1.6.4, 1.7.1 | — |
| **`kotlinx-coroutines-test`** | **ABSENT** | cannot add `runTest` offline |
| `androidx.activity:activity-compose` | **1.7.0**, 1.9.3 | 1.9.3 fetched today |
| `androidx.lifecycle:*` | 2.3.1/**2.6.1**, plus 2.8.7 for `lifecycle-runtime-compose` and `lifecycle-viewmodel-compose` | 2.8.7 partial |
| `androidx.webkit:webkit` | **1.8.0** only | — |
| `androidx.core:core-ktx` | 1.1.0, 1.9.0, **1.15.0** | 1.15.0 fetched today |
| `androidx.core:core-splashscreen` | 1.0.1 | fetched today |
| **Robolectric** `org.robolectric:*` | **4.11.1** (all 15 modules) + `nativeruntime-dist-compat:1.0.2` | **no upgrade offline** (4.14.1 not cached) |
| `junit:junit` | **4.13.2** | — |
| `androidx.test:core` | **1.5.0** only | 1.6.1 not cached |
| `androidx.test:monitor` | 1.6.0, 1.6.1 | — |
| `androidx.test:annotation` | 1.0.1 | — |
| `androidx.test.espresso:espresso-idling-resource` | 3.5.1 | only the idling-resource module |
| **`androidx.compose.ui:ui-test-junit4`** | **ABSENT** | **Compose UI tests cannot be added offline** |
| **`androidx.compose.ui:ui-test-manifest`** | **ABSENT** | same |
| **`androidx.test.ext:junit`** / `androidx.test:runner` / `androidx.test:rules` | **ABSENT** | **no instrumentation tests offline** |
| **`io.mockk`, `org.mockito`, `app.cash.turbine`, `com.google.truth`** | **ABSENT** | no mocking or Flow-testing library offline |
| **`androidx.navigation`, `androidx.datastore`, `androidx.room`, `androidx.paging`** | **ABSENT** | none of these can be introduced offline |
| Gradle distributions | `gradle-8.0.2-all` (unpacked, `.ok`), `gradle-8.9-bin` (added today) | — |
| `.gradle-sandbox/jdks` | **empty** — no toolchain JDK provisioning; `JAVA_HOME` must be set externally | — |

**Bottom line for offline dependency upgrades:** none of the six libraries named in the brief
can be upgraded offline. AGP is pinned to **8.0.0**, Kotlin to **1.8.10**, Compose to **1.4.3**,
Material3 to **1.1.0**, coil to **2.4.0**, okhttp to **4.11.0**. Any upgrade — including
*adding a single test dependency* — requires network. That is the decisive constraint.

### 6.4 `tools/build-release.ps1` (117 L)

Signs and verifies the release APK. Parameters: `ProjectDir` (default
`D:\NovalPie\native-android`), `OutputApk` (`D:\NovalPie\NovalPie-native-2.0-release.apk`),
`SigningDir` (`D:\NovalPie\commercial-app\signing`), `BuildTools`
(`…\Android\Sdk\build-tools\34.0.0`). Flow:
1. Sets `JAVA_HOME` to `jdk-17.0.18.8-hotspot` and prepends its `bin` to `PATH` (`:54`).
2. Asserts `keystore` (`novalpie-release.jks`), `keystore-credentials.txt`, `zipalign.exe`,
   `apksigner.bat` all exist (`:65`).
3. Runs `.\gradlew.bat :app:assembleRelease --console=plain --no-daemon` — **note: no
   `--offline`, and it does not set `GRADLE_USER_HOME`**, so this script uses the *default*
   `~\.gradle` and will attempt the Tencent wrapper download (`:73`).
4. `zipalign -p -f 4` → `apksigner -JXmx128M sign` (alias defaults to `novalpie`) →
   `apksigner verify --verbose` (`:98–108`).
5. Copies to `OutputApk`, prints `{SignedApk, OutputApk, Sha256}` as JSON.

### 6.5 `tools/verify-mumu-compose-launch.ps1` (173 L)

The only runtime UI check. Parameters include `AdbPath`, `MuMuManagerPath`
(`D:\Program Files\MuMu\emulator\MuMuPlayer-12.0\nx_main\MuMuManager.exe`), `VmIndex 0`,
`ApkPath`, `PackageName com.novalpie.app`, `ActivityName com.novalpie.nativeapp.MainActivity`,
`Marker NOVALPIE_NATIVE_COMPOSE_HOME`. Flow: MuMu `info` + `adb connect` → `adb devices -l` →
fallback `adb connect 127.0.0.1:5555` → `adb reverse tcp:7890 tcp:7890` → `adb install -r` →
`cmd package resolve-activity` → `am start -n` → `Start-Sleep 6` →
`exec-out uiautomator dump /dev/tty` → `exec-out screencap -p` → `logcat -b crash -d`.
Exit codes: **0** marker found, **1** launched but marker missing, **2** blocked (no adb device).
Writes a `summary.json` + evidence dir under `D:\NovalPie\smoke-results\`.

### 6.6 The in-flight Phase-1 migration target (`?? gradle/libs.versions.toml` etc.)

Documented here because it changes every version in §6.1 and §6.3. **Not part of the baseline.**

`gradle/libs.versions.toml`: `agp = 8.7.3`, `kotlin = 2.0.21`,
`composeBom = 2024.12.01` (→ compose-ui/foundation 1.7.6, material3 1.3.1),
`activityCompose = 1.9.3`, `lifecycle = 2.8.7`, `coreKtx = 1.15.0`, `splashscreen = 1.0.1`,
`webkit = 1.12.1`, `coil = 2.7.0`, `okhttp = 4.12.0`, `coroutines = 1.9.0`,
`junit = 4.13.2`, `androidxTestCore = 1.6.1`, `robolectric = 4.14.1`.
Plugins add `org.jetbrains.kotlin.plugin.compose` (Kotlin 2.0 moved the Compose compiler there,
replacing `composeOptions.kotlinCompilerExtensionVersion`).
Wrapper `distributionUrl` → `https://services.gradle.org/distributions/gradle-8.9-bin.zip`
plus `validateDistributionUrl=true`.
`app/build.gradle.kts`: `compileSdk`/`targetSdk` **35**, `versionCode 2026072601`,
`versionName 2.0.0-native-beta1`, `testInstrumentationRunner AndroidJUnitRunner`,
`isMinifyEnabled = true` + `isShrinkResources = true`, out-of-band signing via a gitignored
`signing.properties`, `lint { abortOnError = true; baseline = lint-baseline.xml; disable
MissingTranslation/ExtraTranslation }`, `buildConfig = true`.
**New test dependencies it introduces:** `kotlinx-coroutines-test`,
`androidx.compose.ui:ui-test-junit4` (both `testImplementation` and `androidTestImplementation`),
`ui-test-manifest` (`debugImplementation`).
`gradle.properties` now documents the native-memory OOM: `hs_err_pid137816.log` at the project
root records *"Native memory allocation (malloc) failed to allocate 912976 bytes.
Error detail: Chunk::new"* — caused by `kotlin.compiler.execution.strategy=in-process` inside a
small Gradle daemon on a 16 GB machine. The migration drops that setting and adds
`kotlin.daemon.jvmargs=-Xmx2048m`, `org.gradle.jvmargs=-Xmx1536m`, `org.gradle.parallel=false`,
`org.gradle.caching=true`. **This is a real, reproducible build hazard on this machine.**

---

## 7. `tools/verify-native-project.ps1` — the invariants the refactor will break

303 lines. **This is a grep-based structural verifier, not a test.** It reads 39 specific files
with `Read-Required` (throws if any is missing) and then asserts substring/regex presence.
Every file path and every grep string below becomes a hard constraint on the refactor.

Invoke: `powershell -ExecutionPolicy Bypass -File …\tools\verify-native-project.ps1 [-RequireApk]`.
`-RequireApk` additionally requires `app\build\outputs\apk\release\app-release.apk` (`:298`).

### 7.1 Files it requires to exist (renaming or deleting ANY of these fails the script)

Build/manifest: `settings.gradle`, `build.gradle`, `app\build.gradle`,
`app\src\main\AndroidManifest.xml`.

Production Kotlin (`app\src\main\java\com\novalpie\nativeapp\`):
`MainActivity.kt`, `ui\NovalPieApp.kt`, `ui\ProductCopy.kt`, `ui\LibraryPresentation.kt`,
`ui\DiscoverPresentation.kt`, `ui\ProfilePresentation.kt`, `ui\NovalPieViewModel.kt`,
`ui\ApiMessages.kt`, `ui\RequestFreshness.kt`, `ui\RouteStackPolicy.kt`,
`ui\ReaderPresentation.kt`, `ui\ReaderText.kt`, `data\NovalPieApi.kt`,
`data\NetworkConfigStore.kt`, `model\Models.kt`, `data\AuthSessionStore.kt`,
`data\ReaderProgressStore.kt`, `data\ReaderSettingsStore.kt`, `data\SearchHistoryStore.kt`,
`data\SearchSettingsStore.kt`.

**Test Kotlin — the script also pins test file names and test method names:**
`data\ReaderProgressStoreTest.kt`, `data\SearchHistoryStoreTest.kt`,
`data\SearchSettingsStoreTest.kt`, `data\NovalPieApiTest.kt`, `ui\ApiFailureMessageTest.kt`,
`ui\ProductCopyTest.kt`, `ui\LibraryPresentationTest.kt`, `ui\DiscoverPresentationTest.kt`,
`ui\ProfilePresentationTest.kt`, `ui\BookCoverFallbackTest.kt`, `ui\RequestFreshnessTest.kt`,
`ui\RouteStackPolicyTest.kt`, `ui\ReaderPresentationTest.kt`, `ui\ReaderTextTest.kt`.
Plus `tools\verify-mumu-compose-launch.ps1`.

**Note: `settings.gradle`, `build.gradle` and `app\build.gradle` have already been deleted by the
in-flight migration, so this script currently throws on line 24.**

### 7.2 Every grep string it asserts

Build/manifest (`:64–80`): `include ':app'`; root build must **not** match
`capacitor|getcapacitor|cordova`; `com.android.application`; `kotlin-android`; `compose true`;
`applicationId 'com.novalpie.app'`; app build must not match capacitor/cordova; app build must
contain `junit:junit` and `robolectric`; manifest must contain
`com.novalpie.nativeapp.MainActivity`, must not contain `BridgeActivity|Capacitor`, must contain
`android:scheme="novalpie"`; `MainActivity.kt` must contain `ComponentActivity`, `setContent`,
`startUri`, and must **not** contain `WebView|loadUrl|file:///android_asset|appassets.androidplatform.net`.

Compose app markers (`:82–96`): `LaunchedEffect`; **`NOVALPIE_NATIVE_COMPOSE_HOME`** (in both
`NovalPieApp.kt` and `verify-mumu-compose-launch.ps1`); `uiautomator dump`; `screencap`;
**ordering check** — `IndexOf("ReaderBody(content.value, options)")` must appear *before*
`IndexOf("CatalogFilterField(catalogQuery, onCatalogQueryChange)")` in `NovalPieApp.kt`
(`:87–89`); `private fun HomeScreen`, `private fun SearchScreen`, `private fun BookDetailScreen`,
`private fun ReaderScreen`, `private fun SettingsScreen`; `AppRoute.Home`, `AppRoute.Search`,
`AppRoute.Settings`, `AppRoute.BookDetail`, `AppRoute.Reader`, `AppRoute.WebFallback`.

Settings/proxy (`:98`): `proxyEnabled`, `proxyHost`, `proxyPort`, `onSaveProxy`.

Home/bookshelf (`:102`): `ContinueReadingCard`, `RecentReadingSection`, `recentReaderProgresses`,
`GroupSection`, `UserSection`, `filterBooks`, `onBookshelfQueryChange`.

Library (`:105`, in `NovalPieApp.kt` **or** `LibraryPresentation.kt`): `LibraryOverview`,
`libraryOverview`, `LibraryOverviewBlock`, `LibraryShelfControls`, `libraryFavoritesTitle`.
Test-name assertions (`:108`): `libraryOverviewReadsLikeAReaderLibraryClient`,
`libraryOverviewShowsUnsignedStateWithoutDebugLanguage`, `libraryShelfSectionTitlesStayCompact`.

Forum (`:112`, `NovalPieApp.kt` **or** `ProductCopy.kt`): `ForumFeedItem`, `authorName`, `tags`,
`pinned`, `replyCount`, `CompactForumBadge`, `forumFeedBadges`, `forumFeedMetaLine`.
Test names (`:115`): `forumHomeUsesForumClientFeedStructure`,
`forumFeedCopyAvoidsUnsupportedReaderTooling`.

Home pagination (`:119`, app **or** ViewModel): `loadMoreFavorites`, `favoritesCanLoadMore`,
`favoritesLoadingMore`, `onLoadMoreFavorites`.

Search (`:123`): `SearchHistorySection`, `searchHistory`, `onUseSearchHistory`,
`SearchOptionSection`, `ChoiceChips`, `sortBy`, `sortOrder`, `scope`, `matchType`, `adultFilter`.

Discover (`:126`, app **or** `DiscoverPresentation.kt`): `DiscoverOverview`, `discoverOverview`,
`DiscoverSearchPanel`, `discoverFilterGroups`, `DiscoverFilterGroup`, `discoverQuickPrompts`,
`DiscoverIdlePanel`. Test names (`:129`): `discoverOverviewUsesContentClientSearchLanguage`,
`discoverFilterGroupsMatchWebsiteSearchControls`,
`discoverUnsupportedReaderToolingDoesNotAppear`,
`discoverEmptyStateOffersSearchPromptsInsteadOfBlankSpace`.

Search pagination (`:133`): `loadMoreSearch`, `searchCanLoadMore`, `searchLoadingMore`,
`onLoadMore`, `LoadMoreRow`.

Book detail (`:137`, `NovalPieApp.kt` only): `BookDetailHero`, `BookDetailActionRow`,
`bookDetailPrimaryActions`, `bookDetailFavoriteLabel`, `readerProgress`, `CatalogFilterField`,
`filterChapters`, `BookCover`.

Reader (`:141`, app **or** ViewModel **or** `ReaderPresentation.kt`): `ReaderToolbar`,
`ReaderBody`, `ReaderCatalogPanel`, `readerCatalogPanelTitle`, `readerCloseCatalogLabel`,
`increaseReaderFont`, `decreaseReaderFont`, `cycleReaderTheme`, `ReaderProgressStore`,
`ReaderSettingsStore`. Plus the exact call expression
**`readerParagraphsFromContent(content.content)`** in `NovalPieApp.kt` (`:144`), and the test
name `readerCatalogPanelUsesReaderAppLanguage` (`:145`).

Profile (`:150`, app **or** `ProfilePresentation.kt`): `ProfileOverview`, `profileOverview`,
`ProfileOverviewBlock`, `ProfileAccountCard`, `ProfileReaderCard`, `ProfileConnectionCard`,
`profileWebActions`. Test names (`:153`): `profileOverviewUsesUserCenterLanguage`,
`profileOverviewShowsGuestStateCleanly`, `profileSectionsStayProductFacing`,
`profileCopyDoesNotExposeDebugOrUnsupportedReaderTooling`.
Action signals (`:156`, `NovalPieApp.kt` only): `onSaveProxy`, `onClearToken`, `onOpenLogin`,
`onProxyEnabledChange`, `onOpenHomeFallback`, `onOpenSearchFallback`.
**Negative** assertions (`:159`) — `NovalPieApp.kt` must **not** contain: `RuntimeModeCard`,
`SettingsAccountCard`, `SettingsReaderCard`, `Package: com.novalpie.app`.

Covers (`:163`): `SubcomposeAsyncImage`; `bookCoverFallbackText`, `BookCoverFallbackText`,
and the exact fragments **`loading = { BookCoverFallbackText`** and
**`error = { BookCoverFallbackText`**; test names `coverFallbackTextUsesFirstNonBlankTitleCharacter`,
`coverFallbackTextUsesDefaultForBlankTitle`; `WebFallbackScreen` (`:170`).

ViewModel (`:172–195`): `openDeepLink`, `CookieManager`, `NetworkConfigStore`,
`saveProxySettings`, `AuthSessionStore`, `ReaderProgressStore`, `ReaderSettingsStore`,
`SearchSettingsStore`, `SearchHistoryStore`, and these exact expressions:
**`searchSettingsStore.load().toSearchOptions()`**, **`searchHistoryStore.loadLastKeyword()`**,
**`saveSearchOptions()`**, **`searchHistoryStore.saveKeyword`**,
**`routes.replaceWith(nextStack)`**, **`apiFailureMessage(label`**.
`pushDistinctRoute` and `replaceTopReaderRoute` must appear in `RouteStackPolicy.kt` **and**
`NovalPieViewModel.kt` **and** `RouteStackPolicyTest.kt` (`:185`), plus test names
`pushDistinctRouteDoesNotDuplicateCurrentTopRoute`,
`replaceTopReaderRouteSkipsReloadForSameReaderChapter`.
`ApiMessages.kt` must contain `fun apiFailureMessage` and **`visibleFailureLabel(label)`**
(`:193`); test name `includesApiAreaLabelAndThrowableMessage` (`:196`).
`isFreshBookDetailResult` / `isFreshReaderResult` must appear in `RequestFreshness.kt` **and**
`NovalPieViewModel.kt` **and** `RequestFreshnessTest.kt` (`:197`), plus test names
`bookDetailResultIsFreshOnlyForCurrentBookDetailOrReaderRoute`,
`readerResultIsFreshOnlyForCurrentReaderRouteAndChapter`.
`ReaderText.kt` must contain `readerParagraphsFromContent`, `Html.fromHtml`,
`LINE_BREAK_MARKER`, `PARAGRAPH_BREAK_MARKER` (`:204`); `ReaderTextTest.kt` must contain
`readerParagraphsDecodeHtmlEntitiesAndPreserveParagraphBreaks`,
`readerParagraphsIgnoreBlankMarkupButKeepPlainTextFallback`, `&nbsp;`, `&amp;` (`:207`).

API endpoints and aliases (`:211–263`) — required in `NovalPieApi.kt`, and most also required in
`NovalPieApiTest.kt`:
endpoints `/api/search`, `/api/users/me`, `/api/favorites`, `/api/novels/`, `/api/chapters/`,
`/api/favorites/groups`, `/api/favorites/status`;
models in `Models.kt` as `data class` or `sealed interface`: `NovelCard`, `Chapter`,
`ReaderContent`, `FavoriteGroup`, `FavoriteStatus`, `UserProfile`, `ReaderProgress`;
`cookieProvider`, `proxyProvider`;
chapter aliases `chapter_name`, `display_order`, `words`, `created_at`;
`IndexedChapter` and the exact fragment **`.sortedWith(compareBy<IndexedChapter>`**;
book aliases/helpers `cover_path`, `synopsis`, `normalizeTags`, `objectStringOrNull`;
search/list array aliases `results`, `novels`, `list`, `records`;
favorites aliases `favorites`, `books`;
favorite-group aliases `favorite_groups`, `group_name`, `book_count`;
favorite-status aliases `isFavorite`, `status_text`, `favorite_group`;
current-user aliases `uid`, `nickname`, `user_role`;
reader-content aliases `body_html`, `bodyHtml`, `chapter_name`.
Required **test method names** in `NovalPieApiTest.kt`: `chaptersNormalizeWebsiteFieldAliases`,
`chaptersAreSortedByWebsiteDisplayOrder`,
`bookDetailUnwrapsNestedNovelAndNormalizesWebsiteFieldAliases`,
`searchNormalizesResultArrayAliasesAndSendsQueryParameters`,
`favoritesNormalizesFavoriteArrayAliasesAndSendsBookshelfParameters`,
`favoriteGroupsNormalizesWebsiteGroupAliasesAndSendsPreviewParameters`,
`favoriteStatusNormalizesWebsiteStatusAliasesAndSendsParameters`,
`currentUserNormalizesWebsiteProfileAliases`,
`chapterContentNormalizesWebsiteBodyAliasesAndSendsReaderParameters`.

Stores (`:264–287`): `NetworkConfigStore.kt` must contain `Proxy.Type.HTTP`, `127.0.0.1`, `7890`;
`AuthSessionStore.kt`, `ReaderProgressStore.kt`, `ReaderSettingsStore.kt`,
`SearchHistoryStore.kt`, `SearchSettingsStore.kt` must each contain `SharedPreferences`;
`ReaderProgressStore.kt` must match `fun\s+load\(bookId:\s*Long\)` and
`fun\s+loadRecent\(limit:\s*Int` and contain `bookKey(bookId` and `recent_book_ids`;
`SearchHistoryStore.kt` must contain `MAX_HISTORY`;
`SearchSettingsStore.kt` must contain `sort_by`, `sort_order`, `scope`, `match_type`,
`adult_filter`; plus test names `loadsProgressForRequestedBookWithoutLosingOtherBooks`,
`loadsRecentProgressesInMostRecentOrderWithLimit`, `savesRecentKeywordsMostRecentFirst`,
`savingExistingKeywordMovesItToFront`, `ignoresBlankKeywordsAndLimitsHistorySize`,
`loadsLastKeywordFromMostRecentHistoryEntry`, `savesAndLoadsSearchSettings`,
`returnsDefaultsWhenNothingWasSaved`.

Whole-tree scans (`:289–296`): no file under `app\src\main` (`*.kt,*.java,*.xml,*.gradle`) may
contain `capacitor|getcapacitor|cordova`; any file matching
`android.webkit.WebView|WebView\(` must be `WebFallbackScreen.kt`.

### 7.3 Two latent encoding bugs in the verifier (important — they make some checks meaningless)

The `.ps1` is **UTF-8 with BOM** (`ef bb bf`), so PowerShell 5.1 reads *its own* literals
correctly as UTF-8. Every Kotlin source file is **UTF-8 without BOM**, so
`Get-Content -Raw` decodes it as the system ANSI code page (CP936/GBK on this Chinese-locale
Windows). Consequence: **all Chinese in the Kotlin sources arrives as mojibake in the script's
variables.**

1. **`:194` only works by accident.** `ApiMessages.kt:5` contains `请求失败`
   (UTF-8 `e8afb7 e6b182 e5a4b1 e8b4a5`). Read as GBK that becomes `璇锋眰澶辫触`, and line 194
   literally asserts `$apiMessagesSource.Contains("璇锋眰澶辫触")`. The check passes **only** on a
   CP936 Windows host with PowerShell 5.1. It will start failing the moment the file gains a BOM,
   the host locale changes, or the script is run under PowerShell 7 (which defaults to UTF-8).
2. **`:146–147` are vacuous.** The loop asserts
   `-not $readerPresentationSource.Contains("书源")` for `书源`, `规则`, `爬取`, `下载`, `净化`,
   `编辑源`. Because `$readerPresentationSource` is GBK-mojibake, it can never contain
   correctly-decoded Chinese, so these six negative assertions **always pass** — even if the
   forbidden words were present. The real enforcement of that rule lives in
   `ReaderPresentationTest.kt:38`, which is a genuine test.

---

## 8. Recommendation — test scaffolding to add BEFORE refactoring

Ordered by leverage. Only this section proposes solutions.

**Gate 0 — do this first: unblock dependency changes.**
The offline cache cannot supply `kotlinx-coroutines-test`, `androidx.compose.ui:ui-test-junit4`,
`ui-test-manifest`, `androidx.test.ext:junit`, `androidx.test:runner`, or any mocking library
(§6.3). **No new test scaffolding of any kind can be added while `--offline` is mandatory.**
Do one networked `:app:testDebugUnitTest` + `:app:assembleDebug` after the toolchain change to
fully populate `D:\NovalPie\.gradle-sandbox`, then re-pin `--offline` for the rest of the
refactor. Verify the cache contains the *jars*, not just `.pom`/`.module` metadata — that is
exactly what the observed `AGP 8.7.3 was not found` failure was.

1. **Characterisation tests for `NovalPieViewModel` — the single biggest gap.**
   4063 lines, one tested function. Before splitting it, add `kotlinx-coroutines-test` and write
   a `NovalPieViewModelTest` driving the ViewModel against a `MockWebServer`-backed
   `NovalPieApi` and Robolectric-backed stores, asserting the observable state sequence
   (`LoadResult.Idle → Loading → Success/Error`) for at least: home/favorites load + `loadMore`,
   search submit + `loadMore` + option change, book detail load, reader chapter load + progress
   save, auth token clear, proxy save, `openDeepLink`. Use `StandardTestDispatcher` +
   `advanceUntilIdle` so the concurrency guards (`isFresh*`, request serials) are actually
   exercised rather than merely unit-tested in isolation.

2. **A `ReaderSettingsStore` round-trip test.** The only untested store, and
   `ProfilePresentationTest` already pins the labels it feeds (`字号 18sp`, `主题 系统`).
   Trivial to write, closes a real hole.

3. **Compose UI smoke tests, one per top-level screen** (`ui-test-junit4` runs on Robolectric
   with AGP's `unitTests.includeAndroidResources = true`, so no emulator is needed). For each of
   `HomeScreen`, `SearchScreen`, `BookDetailScreen`, `ReaderScreen`, `SettingsScreen`, the five
   `BottomTab` destinations, and the ten currently-uncovered `*Screens.kt` files (§5.3): render
   the composable with a fixed state and `onNodeWithText(...).assertIsDisplayed()` for the
   strings already pinned in §4. This converts §4's helper-level string pins into *screen-level*
   pins and is the only thing that can catch "the label still exists in Kotlin but no screen
   renders it any more" — the exact failure mode a full UI rebuild produces.

4. **Wire `tools/golden_strings.py` into the build gate.** It exists (§5.5) but nothing runs it.
   Add it to a `verify` entry point next to `verify-native-project.ps1` so a lost string fails
   the build rather than being noticed later.

5. **Rewrite `tools/verify-native-project.ps1` as the refactor proceeds — do not try to keep it
   passing.** It hard-codes 39 file paths, ~180 identifier/expression greps, five
   `private fun <Screen>` names, and 30 test *method* names. A god-file split invalidates
   essentially all of it. Concretely: (a) fix the encoding bugs in §7.3 by reading Kotlin with
   `Get-Content -Raw -Encoding UTF8`; (b) delete every assertion that greps for a *test method
   name* — a test suite that runs green is stronger evidence than a grep for its method name;
   (c) keep and strengthen the genuinely valuable checks that do not depend on file layout: no
   Capacitor/Cordova anywhere in `app/src/main`; `WebView` only in the web-fallback file;
   `applicationId 'com.novalpie.app'`; the `novalpie` deep-link scheme; `MainActivity` is not a
   WebView launcher; the `NOVALPIE_NATIVE_COMPOSE_HOME` marker; and no
   `RuntimeModeCard`/`SettingsAccountCard`/`SettingsReaderCard`/`Package: com.novalpie.app`.

6. **Freeze the API-layer contract before splitting `NovalPieApi.kt`.** The 73 MockWebServer
   tests are the strongest asset in the repo, but they all construct
   `NovalPieApi(baseUrl = …[, proxySelectorProvider = …])`. Keep that constructor (or provide a
   test factory with the same shape) so all five suites keep compiling through the split. Then
   add the three missing coverage points: `adminApproveAllReviews`, public-user
   `userCheckinStats`, and request-shape assertions for `/api/users/me/activities` and
   `/api/users/me/novels`, which are called in production but never asserted.

7. **Add negative/error-path coverage at the API boundary.** Every one of the 73 contract tests
   enqueues a 200 with a well-formed body. Nothing tests a 401, a 403, a 500, an HTML error page
   served with `content-type: text/html`, an empty body, or malformed JSON — yet
   `apiFailureMessage` has a dedicated branch for `NovalPie API <status>` strings
   (`ApiMessages.kt:12`) that only `ApiFailureMessageTest` reaches synthetically. Add one
   `MockResponse().setResponseCode(...)` test per response family.

8. **Add a `robolectric.properties`.** There is none. Robolectric currently infers the SDK from
   `targetSdk` (34 baseline → 35 after migration). Pinning `sdk=34` (or an explicit supported
   value) makes the test run independent of `targetSdk` changes, which the migration is already
   making.

9. **Record the green baseline as an artifact before touching production code.** The last known
   green run is `app/build/test-results/testDebugUnitTest/*.xml` at `2026-07-26 14:36:12`
   (51 suites / 252 tests / 0 failures). Copy that XML set out of `app/build/` (which is
   gitignored and will be wiped) so the refactor can diff test *counts* per suite, not just
   pass/fail — a silently deleted test method is otherwise invisible.
