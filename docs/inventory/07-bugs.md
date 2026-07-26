# 07 — Correctness and Bug Hunt (NovalPie native Android)

Scope: `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp` (all 66 Kotlin files read
or grepped; the three god-files read in full). Every finding below is derived from source, not docs.
Line numbers are from the working tree at the time of writing.

Paths are abbreviated as:

| Short | Full path |
|---|---|
| `VM` | `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt` |
| `App` | `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt` |
| `Api` | `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt` |

Everything else is cited with its full path.

---

## Summary table (ranked)

| # | Severity | Area | One-line |
|---|---|---|---|
| 1 | high | network | Every API request builds a fresh `ProxySelector`, defeating OkHttp connection pooling, and every request probes two emulator proxies before going direct |
| 2 | high | state | Every mutation's success/error message (and the user's typed draft) is destroyed by the reload fired immediately after it |
| 3 | high | forum | Six hardcoded fake forum threads are rendered as real content whenever the forum load is idle/loading/failing or empty |
| 4 | high | navigation | `saveManagedBook` leaves `bookDetailState` permanently stuck in `Loading`; nothing re-loads a screen on back-navigation |
| 5 | high | exam | Unthrottled infinite submit loop when the exam clock hits 0 and submission fails |
| 6 | high | editor | Whole novel text bound to a single `OutlinedTextField` (up to 50 000 000 chars allowed) |
| 7 | high | pagination | Load-more failure replaces the entire accumulated list with an error, discarding all previously loaded pages |
| 8 | high | i18n | Two user-visible labels are GBK/UTF-8 mojibake (`璁哄潧`, `鏍囩` + a private-use tofu char) |
| 9 | high | lifecycle | Rotation re-runs the deep link and throws the user back to the deep-linked book/chapter |
| 10 | medium-high | mutations | ~25 mutating API calls never check `success`; `saveManagedChapterOrder` clears `orderDirty` on a rejected save |
| 11 | medium-high | editor | Custom-script run lives in a composition-scoped `LaunchedEffect`; leaving the screen wedges `busy = true` forever |
| 12 | medium-high | errors | HTTP error response bodies are read and thrown away; the user only ever sees `服务返回错误 <code>` |
| 13 | medium | messaging | `sendMessageDraft` has no request guard and writes into whatever conversation is current when it returns |
| 14 | medium | pagination | `loadMoreMessages` re-reads the (possibly edited) query, and dies permanently if the server omits `total_pages` |
| 15 | medium | auth | An expired/revoked token is never cleared; UI keeps claiming `已登录` while every call 401s |
| 16 | medium | navigation | One shared route stack: switching bottom tabs (and tapping your own name) destroys the stack |
| 17 | medium | upload | `readUploadDocument` can return `sizeBytes = -1`; avatar/illustration uploads then reject a perfectly good file |
| 18 | medium | parsing | `EpubParser` hard-codes UTF-8 for chapter XHTML → garbled text for GB2312/UTF-16 EPUBs |
| 19 | medium | reader | Scroll position is retained across chapter changes; you land mid-chapter after "next chapter" |
| 20 | medium | reader | Prev/next silently disabled whenever the current chapter is not in the catalog list |
| 21 | medium | lifecycle | `WebFallbackScreen` never `destroy()`s its WebView; `Coil.setImageLoader` is swapped globally at least twice per launch |
| 22 | medium | editor | `EditorProcessor.chaptersFromMatches` throws a raw `StringIndexOutOfBoundsException` on overlapping multi-pattern matches |
| 23 | medium | editor | `EditorScriptEngine` does its heavy string work on `Dispatchers.Main.immediate` |
| 24 | medium | parsing | `NovelCard.id` silently falls back to `0L`; those cards are un-tappable and can crash the `LazyColumn` key contract |
| 25 | medium | data | `intOrNull` = `longOrNull()?.toInt()` — silent 32-bit truncation of server counters |
| 26 | medium | reader | `clearReaderProgress` wipes **all** books' progress, not the current one |
| 27 | medium | admin | Admin entry points and mutations `return` silently when the role check fails — dead buttons |
| 28 | medium | ui | "收藏 N"/"主题 N" report the loaded-page count as if it were the collection total |
| 29 | low-medium | data | Illustration ids fall back to `index + 1` and can collide with real ids on delete |
| 30 | low-medium | store | `EditorArchiveStore.save` deletes the target before renaming the temp file — a failed rename loses both |
| 31 | low-medium | state | `startPoliticalExam` / `loadSearchTags` have no staleness guard at all |
| 32 | low | data | `unwrapObject` returns an empty `JSONObject` for array/string payloads, silently blanking every field |
| 33 | low | ui | Numeric fields (`插入位置`, `自动已读天数`) silently coerce unparseable text to `0`/`null` |

---

## 1. Every request rebuilds the proxy selector → no connection reuse, plus a mandatory emulator-proxy probe — **high**

**Where**
- `VM:443-447` — the selector provider allocates a new selector per call:
  ```kotlin
  proxySelectorProvider = {
      proxySettings.toProxySelector(preferEmulatorProxy = shouldPreferEmulatorProxy())
  }
  ```
- `Api:1653-1659` — `execute()` invokes the provider on **every** request and rebuilds the client:
  ```kotlin
  val proxySelector = if (explicitProxy == null) proxySelectorProvider() else null
  val callClient = when { ... proxySelector != null -> client.newBuilder().proxySelector(proxySelector).build() ... }
  ```
- `data/NetworkConfigStore.kt:31-47` — `toProxyRoutes()` appends `10.0.2.2:7890` and `127.0.0.1:7890`
  **unconditionally**, even when the proxy is disabled, with `Proxy.NO_PROXY` only at the end.
- `data/NetworkConfigStore.kt:49-51,77-83` — `FixedProxySelector` is a plain class with no
  `equals`/`hashCode` override.
- `data/NetworkConfigStore.kt:71-75` — `shouldPreferEmulatorProxy()` returns true for **any** x86/x86_64
  ABI, which includes x86_64 tablets/Chromebooks, not just emulators.
- OkHttp is 4.12.0 (`gradle/libs.versions.toml:12`). `okhttp3.Address.equalsNonHost` includes
  `proxySelector` in its comparison, and `RealConnectionPool` only reuses a connection when the
  addresses match.

**Failure scenario**
Real phone, proxy toggle off, user opens the bookshelf. `loadHome` (`VM:3760-3764`) fires three
parallel requests. For each one:
1. A brand-new `FixedProxySelector` instance is created, so the resulting `Address` never equals the
   `Address` of any pooled connection → the pooled TLS connection to `novalpie.cc` is never reused.
   Every single API call pays a fresh TCP + TLS handshake.
2. Route selection returns `[10.0.2.2:7890, 127.0.0.1:7890, DIRECT]`. OkHttp tries `10.0.2.2:7890`
   first. On a phone whose LAN is not `10.0.2.x` the SYN is dropped, so the request stalls for the
   full `connectTimeout(12s)` (`Api:3347-3351`), then `127.0.0.1:7890` fails instantly
   (`ECONNREFUSED`), then DIRECT finally works. `callTimeout(30s)` means two such hops per request
   can abort the whole call.

Observable behaviour: the app takes 12–24 s to show the bookshelf, or shows
`书架请求失败: ...` timeouts, on every cold screen. This single defect plausibly accounts for most of
the "app is broken/slow" complaint.

---

## 2. Post-mutation reload destroys the mutation's own feedback message and the user's draft — **high**

**Where** — the pattern is `state = ...copy(actionMessage = X)` immediately followed by a `loadX()`
that constructs a *fresh* state object (or explicitly sets `actionMessage = null`):

| Mutation | Sets message at | Reload that wipes it | Reload resets |
|---|---|---|---|
| `submitBookComment` | `VM:2982-2998` | `VM:2999` → `loadBookDetail` | `VM:3919-3926` builds a new `BookDetailState` (drops `actionMessage` **and** `commentDraft`) |
| `reactOnBookComment` | `VM:3025-3038` | `VM:3039` | same |
| `submitReaderComment` | `VM:3077-3093` | `VM:3094` → `loadReader` | `VM:3959-3965` builds a new `ReaderState` (drops `actionMessage` **and** `commentDraft`) |
| `reactOnReaderComment` | `VM:3121-3134` | `VM:3135` | same |
| `submitForumComment` | `VM:2848-2864` | `VM:2865` → `loadForumPostDetail` | `VM:2797-2804` builds a new `ForumPostDetailState` (`actionMessage` dropped) |
| `reactOnForumPost` | `VM:2907-2920` | `VM:2921` | same |
| `reactOnForumComment` | `VM:2931-2944` | `VM:2945` | same |
| `runMessageCenterAction` | `VM:1622-1636` | `VM:1637` → `loadMessageCenter` | `VM:1280-1287` sets `actionMessage = null` |
| `runMessageDetailAction` | `VM:1649-1657` | `VM:1658` → `loadMessageDetail` | `VM:1420` builds a new `MessageDetailState` |
| `runWorkspaceAction`, `saveWorkspaceApi`, `deleteWorkspaceLocalApi` | `VM:2674-2687`, `2562-2576`, `2589-2603` | `VM:2688`, `2577`, `2604` → `loadWorkspace` | `VM:2490-2498` sets `actionMessage = null` |

**Failure scenario (worst case, comment on a book)**
1. User types a 200-character review into the book-detail composer.
2. Taps 发送. `submitBookComment` (`VM:2967`) suspends.
3. The POST fails (rate limit, 403, network). `onFailure` sets
   `actionMessage = "评论提交请求失败: …"` and keeps `commentDraft` intact (`VM:2992-2997`).
4. Line `VM:2999` then unconditionally calls `loadBookDetail(bookId)`, whose first statement
   (`VM:3919`) replaces the whole state with `BookDetailState(bookId = …, book = Loading, …)`.

Observable behaviour: the error message flashes for one frame at most, the composer is emptied, and
the user's 200 characters are gone with no explanation. The same applies to every like/dislike/emoji/
award tap (they appear to do nothing), to every message-centre batch action, and to the entire
workspace screen, where **no** success or failure message can ever be seen.

---

## 3. Fabricated forum threads presented as real content — **high**

**Where**
- `App:605-608`:
  ```kotlin
  val feedItems = when (posts) {
      is LoadResult.Success -> posts.value.map(::forumPostFeedItem).ifEmpty { forumFeedItems() }
      else -> forumFeedItems()
  }
  ```
- `ui/ProductCopy.kt:139-220` — `forumFeedItems()` is six hardcoded `ForumFeedItem`s with invented
  titles, authors and metrics: `"角色弧光讨论"` / author `"北港读者"` / 42 replies / 7305 views /
  pinned+featured, `"最新章节伏笔整理"` / `"栗子校对"` / 28 replies, `"作者更新说明"` / `"运营记录"`,
  `"结局走向猜测"` / `"雾灯"` / 64 replies, `"翻译名词校对"` / `"灰页"`, `"收藏榜单变化"` / `"榜单观察"`.
- `App:647` — `ForumStatsStrip(feedItems)` computes 主题/回复 counts from the same fake list
  (`App:700-702`).
- `App:674-676` — the fake rows are rendered *in addition to* the error block emitted at
  `App:651-657`.

**Failure scenario**
Airplane mode, open the 论坛 tab. `loadForum` (`VM:2692-2700`) fails. The screen shows an error card
**and underneath it** six plausible-looking Chinese discussion threads with reply counts and view
counts, plus a stats strip reading "主题 6 · 回复 171 · 分区 4". Tapping any of them does nothing,
because `ForumFeedItem.id` defaults to `0` and the row is gated on `item.id > 0` (`App:724`). The same
fake feed also appears during the normal loading spinner and when the forum genuinely has zero posts.

This is fabricated content shown as real site data — the single most damaging "crude" symptom.

---

## 4. `saveManagedBook` wedges the book-detail screen in `Loading` forever — **high**

**Where**
- `VM:3299` — inside `saveManagedBook`, while the current route is `BookEditInfo`:
  ```kotlin
  if (saved.success) loadBookDetail(state.bookId)
  ```
- `VM:3919-3926` — `loadBookDetail` *synchronously* sets
  `bookDetailState = BookDetailState(book = Loading, chapters = Loading, comments = Loading, favoriteStatus = Loading, …)`.
- `VM:3933` — the coroutine then bails out:
  ```kotlin
  if (!isFreshBookDetailResult(currentRoute, bookDetailState, bookId)) return@launch
  ```
  `ui/RequestFreshness.kt:9-13` only accepts `AppRoute.BookDetail` or `AppRoute.Reader`; the current
  route is `AppRoute.BookEditInfo`, so it returns `false`.
- `App:119-121` is the **only** `LaunchedEffect` in the app shell; there is no per-route effect that
  reloads a screen when it becomes visible, and `goBack()` (`VM:3744-3748`) only pops the stack.

**Failure scenario**
Book detail → 编辑信息 → change the title → 保存 (server returns success) → press back. The book
detail screen now renders `LoadingBlock("正在加载书籍详情")` (`App:1758`) plus
`LoadingBlock("正在加载章节目录")` (`App:1777`) indefinitely. Because the state is `Loading` and not
`Error`, no retry button is drawn (`App:1756-1771`); the only escape is switching tabs (which
destroys the stack) or re-entering the book from a list.

The same hazard exists for `saveManagedChapterDraft`/`runManagedChapterMutation` → `loadManagedChapters`
(`VM:3598`, `VM:3650`) if the route changes between the mutation and the reload.

---

## 5. Political-exam auto-submit becomes an unthrottled retry loop — **high**

**Where**
- `ui/PoliticalExamScreens.kt:63-68`:
  ```kotlin
  LaunchedEffect(state.phase, state.remainingTimeSeconds, state.submitting) {
      if (state.phase == PoliticalExamPhase.Active && !state.submitting) {
          if (state.remainingTimeSeconds > 0) delay(1_000)
          onTick()
      }
  }
  ```
- `VM:2354-2358` — `tickPoliticalExamTimer` calls `submitPoliticalExam()` when
  `remainingTimeSeconds <= 0`.
- `VM:2388-2394` — on failure `submitPoliticalExam` sets `submitting = false` and **leaves
  `phase = Active`**.

**Failure scenario**
Exam clock reaches `00:00` while the network is flaky.
1. `remainingTimeSeconds == 0`, `submitting == false` → the effect body runs with **no `delay`** and
   calls `onTick()`.
2. `tickPoliticalExamTimer` → `submitPoliticalExam()` → `submitting = true` (key change, effect
   restarts, guard blocks it).
3. The POST fails → `submitting = false`, `phase` still `Active`, `remainingTimeSeconds` still `0`.
4. The `submitting` key flips `true → false` → the effect restarts → step 1 again, immediately.

Observable behaviour: a tight loop of `/api/political-exams/sessions/submit` POSTs with zero backoff,
continuous recomposition, and the phone heating up until the user leaves the screen. The same effect
also freezes the countdown whenever the exam screen is not the visible route, so backgrounding the
exam means it never auto-submits and the server-side session simply expires.

---

## 6. The whole novel is bound to one `OutlinedTextField` — **high**

**Where**
- `ui/UploadEditorScreens.kt:292-300`:
  ```kotlin
  OutlinedTextField(
      value = state.text,
      onValueChange = onTextChange,
      minLines = 18, maxLines = 32, …
  )
  ```
- `VM:2409-2422` — `readEditorText` accepts up to **50 000 000 characters** before it throws
  (`"文本超过 5000 万字符，请先分割文件"`).
- `VM:1962-1965` / `data/EditorProcessor.kt:70-73` — for an EPUB the editor text is
  `EditorProcessor.toWebsiteIdentifiers(parsed.chapters)`, i.e. the entire book concatenated into one
  `String`.
- `data/EpubParser.kt:52` — all spine entries are decompressed into memory at once
  (`MAX_CHAPTER_BYTES = 24 MiB` per entry, no total cap).

**Failure scenario**
Open a normal 1.5 M-character Chinese web novel (`.txt`, ~3 MB) in the EPUB 编辑器. `state.text`
becomes a 1.5 M-char `String` inside a Compose `MutableState`, handed to a single `BasicTextField`.
Compose must lay out the entire string (`maxLines` clips rendering but not measurement/`TextLayout`),
so the UI thread blocks for tens of seconds → ANR; on a 2 GB device it OOMs first, because the text
also exists as the raw file bytes, the parsed chapter list, and the archive copy. Every keystroke
re-runs the layout.

---

## 7. A failed "load more" destroys everything already loaded — **high**

**Where**
- `VM:3799-3804` (favorites):
  ```kotlin
  onFailure = {
      homeState.copy(
          favorites = LoadResult.Error(apiFailureMessage(VisibleUiLabels.Bookshelf, it)),
          favoritesLoadingMore = false
      )
  }
  ```
- `VM:3901-3903` (search):
  ```kotlin
  onFailure = { searchResults = LoadResult.Error(apiFailureMessage(VisibleUiLabels.Search, it)) }
  ```

**Failure scenario**
User searches, taps 加载更多结果 four times (80 results on screen), then the fifth page times out
(very likely given finding #1). `searchResults` is replaced by `LoadResult.Error`, so
`searchResultItems` (`App:1429`) renders a bare error card and all 80 results vanish. The keyword is
still in the box but the accumulated pages are unrecoverable without a full re-search from page 1.
Same for the bookshelf.

Contrast with `loadMoreMessages` (`VM:1328-1333`), which correctly keeps `messages` untouched on
failure — so the correct pattern is already present in the file, just not applied here.

---

## 8. Mojibake in user-visible error labels — **high** (trivial to fix, embarrassing to ship)

**Where**
- `VM:2698`:
  ```kotlin
  forumState = ForumState(posts = result.toLoadResult("璁哄潧"))
  ```
  Code points `U+7481 U+54C4 U+6F67`. Re-encoding as GBK yields the bytes
  `E8 AE BA E5 9D 9B`, which is UTF-8 for **`论坛`**.
- `VM:3914`:
  ```kotlin
  searchTags = result.toLoadResult("鏍囩")
  ```
  Code points `U+93CD U+56E9 U+E137`. Bytes `E6 A0 87 E7 AD BE` = UTF-8 for **`标签`**.
  `U+E137` is a Unicode Private Use Area code point and renders as a tofu box.

**Failure scenario**
Forum load fails → the error card (`App:651-657`) reads
`璁哄潧请求失败: 服务返回错误 500` instead of `论坛请求失败: …`.
Tag load fails → `鏍囩<tofu>请求失败: …` instead of `标签请求失败: …`.
`apiFailureMessage` (`ui/ApiMessages.kt:3-6`) passes the label straight through, so there is no
sanitisation anywhere. These are the only two occurrences in the source tree (verified by scanning
every `.kt` file for GBK-mojibake code-point ranges).

---

## 9. Rotating the device replays the deep link — **high**

**Where**
- `MainActivity.kt:15-19` — `startUri = intent?.data?.toString()` is read in `onCreate` and passed as a
  parameter on every Activity creation.
- `App:119-121`:
  ```kotlin
  LaunchedEffect(startUri) { if (!startUri.isNullOrBlank()) viewModel.openDeepLink(startUri) }
  ```
- `VM:3730-3741` — `openDeepLink` does `currentTab = Collection; routes.clear(); routes.add(Home)`
  and then pushes `BookDetail` + `Reader`.
- `AndroidManifest.xml` declares no `android:configChanges`, so rotation destroys and recreates
  `MainActivity` while the `ViewModel` survives.

**Failure scenario**
Launch the app from `novalpie://app/book/123/456`. Read chapter 456, go back, browse to the 论坛 tab,
open a thread, rotate the phone. The recreated Activity passes the same `startUri`, the new
composition runs the `LaunchedEffect`, and the user is teleported back into the reader for
`123/456` with the forum stack wiped. Repeats on every rotation for the whole process lifetime.

Secondary: there is no `https`/`novalpie.cc` intent filter in the manifest, so the entire
`isWebsiteRoute` branch of `openDeepLink` (`VM:3716`) is unreachable in practice.

---

## 10. Mutating API results assumed successful — **medium-high**

`ForumActionResult`/`MessageActionResult`/`UserCheckinAction`/`WorkspaceActionResult` all default
`success = true` when the field is absent (`Api:1704`, `Api:2776`, `Api:2783-2785`, `Api:2396-2398`,
`Api:2384-2387`), and almost every caller only reads `.message`, never `.success`.

Non-checking call sites:

| Caller | Line | What it claims on `success = false` |
|---|---|---|
| `saveManagedChapterOrder` | `VM:3412-3413` | sets `orderDirty = false` and says `章节顺序已更新` |
| `runManagedChapterMutation` | `VM:3644-3650` | `$label 已完成` + refreshes |
| `saveManagedChapterDraft` | `VM:3596-3598` | `章节已保存`, closes the editor, drops the text |
| `uploadManagedChapterIllustrations` | `VM:3518-3523` | `章节插图已上传` |
| `deleteManagedChapterIllustration` | `VM:3544-3548` | `章节插图已删除` |
| `saveManagedBookAccessPolicy` | `VM:3212-3216` | `读写门槛已保存` |
| `transferManagedBook` | `VM:3241-3249` | `已提交转让给 …` |
| `runAdminMutation` | `VM:1239-1242` | passes `action.message ?: successMessage`, e.g. `Key 已删除` |
| `runMessageCenterAction` | `VM:1622-1628` | `$label已同步` |
| `runMessageDetailAction` | `VM:1649-1650` | e.g. `已标记为已读` |
| `runWorkspaceAction` | `VM:2674-2679` | e.g. `Cookie 配置已保存` |
| `sendMessageDraft` | `VM:1536-1542` | `私信已发送` |
| `saveMessageSettings` | `VM:1596-1602` | writes `settings = Success(draft)` optimistically |
| `uploadProfileAvatar` | `VM:906-920` | ignores the returned `UserCheckinAction` entirely, says `头像已更新` |
| `submitForumComment` / all 8 react* helpers | `VM:2848`, `2907`, `2931`, `3025`, `3121` | `评论已提交` / `$label 已同步` |

Plus the API layer itself: `Api:692-700` `updateCurrentUser` fires the PATCH and **discards the
response**, returning the caller's own `UserProfile`:
```kotlin
patch("/api/users/me", body)
profile
```
so `saveProfile` (`VM:807-816`) writes the optimistic profile into both `profileState` and
`homeState` and reports `资料已保存`.

**Worst failure scenario (`saveManagedChapterOrder`)**
Reorder 12 chapters, tap 保存章节顺序. Server replies `200 {"success": false, "message": null}` (e.g.
the book is under review). `orderDirty` is cleared, the toast says `章节顺序已更新`, the list keeps
the *new* local numbering from `moveManagedChapter` (`VM:3397-3401`). The user leaves. Next visit the
old order is back and the reordering work is silently lost.

Only three sites do this correctly: `submitUploadBook` (`VM:1804`), `saveWorkspaceApi`'s server hop
(`VM:2546`), and `appendManagedChapters` (`Api:1437`).

---

## 11. The custom-script run is owned by the composition, so leaving the screen wedges the editor — **medium-high**

**Where**
- `VM:2020-2027` — `processEditorSplit` for `CustomScript` sets `busy = true`, bumps `scriptRunId`,
  and **returns**; it never runs anything itself.
- `ui/UploadEditorScreens.kt:118-133` — the actual execution lives in
  `LaunchedEffect(state.scriptRunId)` inside `UploadEditorScreen`.
- `VM:2054-2056` — `completeEditorCustomScript` is the only thing that clears `busy`, and it is only
  called from that effect.

**Failure scenario**
Start a custom-script run on a large text, then press back (or tap a bottom tab) while it runs.
`UploadEditorScreen` leaves the composition → the `LaunchedEffect` coroutine is cancelled →
`onCustomScriptResult` is never invoked → `uploadEditorState.busy` stays `true`. Re-open the editor:
every button is disabled (`state.busy` gates 打开/生成 EPUB/发送到上传 at
`ui/UploadEditorScreens.kt:239-241` and 生成章节目录 at `:426`) and the banner still says
`正在本地沙箱执行脚本…`. The only recovery is the 清空编辑器 trash icon
(`ui/UploadEditorScreens.kt:236` → `VM:2258-2265`), which throws away all the user's work.

---

## 12. HTTP error bodies are read and discarded — **medium-high**

**Where** `Api:1661-1667`:
```kotlin
callClient.newCall(request).execute().use { response ->
    val responseBody = response.body?.string().orEmpty()
    if (!response.isSuccessful) {
        throw IOException("NovalPie API ${response.code}: $path")
    }
    return parseJsonOrString(responseBody)
}
```
`responseBody` is fully read and then thrown away on failure. `ui/ApiMessages.kt:11-14` then reduces
even the status code to a generic string:
```kotlin
val status = Regex("""NovalPie API (\d+)""").find(detail)?.groupValues?.getOrNull(1)
return if (status == null) detail else "服务返回错误 $status"
```

**Failure scenario**
The site rejects a chapter insert with `422 {"message":"章节标题重复，请修改后重试"}`. The user sees
`保存章节请求失败: 服务返回错误 422` and has no idea what to change. Same for every 400/403/409/429
across the app. `executeExternal` (`Api:1679-1683`) has the identical problem for the AI-regex call.

Conversely, when the exception is *not* an HTTP status the raw technical text is shown verbatim
(`ui/ApiMessages.kt:4`), producing Chinese-plus-English hybrids such as
`书架请求失败: failed to connect to /10.0.2.2 (port 7890) from /192.168.1.5 … after 12000ms`.

---

## 13. `sendMessageDraft` writes into the wrong conversation — **medium**

**Where** `VM:1520-1553`. No request serial; `messageConversationRequestSerial` exists (`VM:461`) but
is only used by `loadMessageConversation`. After the suspend point:
```kotlin
messageConversationState = result.fold(
    onSuccess = { messageConversationState.copy(draft = "", sending = false, actionMessage = …) }, …
)
if (result.isSuccess) loadMessageConversation(targetUserId, messageConversationState.targetName)
```

**Failure scenario**
User is chatting with Alice (`targetUserId = 11`). Taps 发送, and while the POST is in flight taps
back and opens the conversation with Bob (`targetUserId = 22`) from the message list. The POST
returns:
1. `messageConversationState.copy(...)` is applied to **Bob's** state → Bob's screen shows
   `私信已发送` and Bob's draft is cleared.
2. `loadMessageConversation(11, "Bob")` is then called with Alice's id and **Bob's** name
   (`VM:1551`), so the state becomes Alice's message list rendered under Bob's title, while the route
   is still `MessageConversation(22, "Bob")`.

Observable behaviour: the private-message screen shows another person's conversation history under
the wrong name. Because `loadMessageConversation` builds a fresh state (`VM:1502-1506`), the route
parameters and the state disagree until the user backs out.

---

## 14. Message pagination desync — **medium**

**Where**
- `VM:1312-1317` — the load-more request re-reads live state inside the coroutine:
  ```kotlin
  api.messagePage(query = messageCenterState.query, page = pagination.page + 1, pageSize = pagination.pageSize)
  ```
  while `pagination` was captured at `VM:1307` before launching.
- `VM:1338-1342` — `updateMessageKeyword` mutates `query` without triggering a reload.
- `Api:2231-2233` — when the server response has no `pagination` object,
  `totalPages` defaults to `1`.
- `VM:1308` — the guard is `if (… pagination.page >= pagination.totalPages) return`.

**Failure scenarios**
1. User loads page 1 (unfiltered, 20 messages), types `退款` in the keyword box but does not press
   search, then scrolls and taps 加载更多消息. Page 2 is fetched **with `keyword=退款`** and merged
   into the unfiltered page 1 (`VM:1323` → `ui/MessagePresentation.kt:39-45`), producing a list that
   matches no single filter, with `pagination` now describing the filtered result set.
2. If `/api/messages` omits `pagination`, `totalPages == 1` and `page == 1`, so 加载更多消息 is
   permanently a no-op even when the mailbox has 500 messages. `loadMessageCenter` also swallows the
   error with `pageResult.getOrNull()?.pagination ?: MessagePagination()` (`VM:1299`).

---

## 15. Expired tokens are never cleared; the UI keeps claiming you are logged in — **medium**

**Where**
- `data/AuthSessionStore.kt:15-16` — `decodeAuthTokenProfile` returns `null` for an expired `exp`,
  but nothing clears the stored token.
- `VM:435` — `authToken` is loaded from prefs at construction; the only clear path is the manual
  `clearAuthToken()` (`VM:694-699`) behind a settings button.
- `App:185, 222, 267, 314, …` — every screen's `hasAuthToken = !viewModel.authToken.isNullOrBlank()`,
  so an expired token still renders `已同步` / `已登录` (`App:632`,
  `ui/ProductCopy.kt:44-45,115-116`).
- `Api:1663-1665` — a `401` becomes a generic `IOException`; no caller inspects the status.

**Failure scenario**
A 7-day token expires. The user opens the app: the bookshelf, tools, profile and message centre all
show `已同步` plus error cards reading `服务返回错误 401`. There is no "session expired, please log
in again" path and no automatic redirect to `openLoginFallback()`. The user has to guess that
设置 → 清除令牌 is the fix.

Related: `resolveUserLoadResult` (`VM:4032-4041`) falls back to the JWT-derived profile whose `role`
defaults to `"user"` (`data/AuthSessionStore.kt:25-27`), which then silently disables admin features
(see finding #27).

---

## 16. One shared route stack — tab switches and self-profile taps destroy it — **medium**

**Where**
- `VM:450` — a single `routes` list backs all five tabs.
- `VM:726-728` — `openTab` does `routes.clear(); routes.add(targetRoute)`.
- `VM:933-938` — `openUserProfile`, when the target is you, does
  `currentTab = Profile; routes.clear(); routes.add(Profile)`.
- `VM:3691-3696` / `VM:3730-3737` — `continueReading` and `openDeepLink` also `routes.clear()`.

**Failure scenarios**
1. 论坛 → open a thread → switch to 收藏 to check something → switch back to 论坛: you are at the
   forum root, the thread is gone. Every tab shares one stack, so there is no per-tab back stack.
2. Reading a thread, you tap your own name in a comment (`App:1180-1182` →
   `viewModel::openUserProfile`). Because `ownId == userId`, the whole stack is cleared and you are
   dropped on the 我的 tab. The thread you were reading is unrecoverable via back.

The `BackHandler` condition (`App:123`) and the nav-icon condition (`App:138`) do correctly match the
five root routes, so system back exits only at a root — that part is consistent.

---

## 17. `sizeBytes = -1` rejects valid files — **medium**

**Where**
- `VM:2424-2449` — `readUploadDocument` starts with `var size = -1L`, only overwrites it if the
  provider returns `OpenableColumns.SIZE`, then falls back to `openAssetFileDescriptor(...)?.length`,
  and can still end at `-1L` (`VM:2440-2442`).
- `VM:908` — `require(document.sizeBytes > 0L) { "头像文件为空" }`
- `VM:3328` — `require(document.sizeBytes > 0L) { "封面文件为空" }`
- `VM:3506-3508` — `require(document.sizeBytes in 1..WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES)`
- `Api:280`, `Api:337`, `Api:1308` — the same requirement repeated in the API layer.
- `Api:3359`, `Api:3379` — `contentLength()` returns the raw `sizeBytes`, i.e. `-1`.

**Failure scenario**
Pick an avatar from a `DocumentsProvider` that does not publish `SIZE` (several cloud/photo pickers,
and any `content://` provider that returns `null` for the column). `readUploadDocument` yields
`sizeBytes = -1`. The `require` fires and the user is told `上传头像请求失败: 头像文件为空` for a
perfectly readable 400 KB JPEG. If a `-1` ever slips through to `UploadStreamRequestBody`
(`selectUploadEpub` only rejects `== 0L`, `VM:1695`), OkHttp treats `-1` as unknown length and
switches to chunked transfer encoding, which the PHP upload endpoint is unlikely to accept.

---

## 18. EPUB chapters are decoded as UTF-8 unconditionally — **medium**

**Where** `data/EpubParser.kt:55`:
```kotlin
val html = bytes.toString(Charsets.UTF_8)
```
No inspection of the XML declaration, `<meta charset>`, or a BOM. Everything downstream
(`extractHtmlTitle`, `htmlToPlainText`) operates on the mis-decoded string.

**Failure scenario**
An older EPUB whose XHTML files are `encoding="gb2312"` (common for Chinese scanlations) or UTF-16.
Every chapter title and body becomes `` replacement characters. The upload proceeds happily and
publishes the garbage to the site, because `htmlToPlainText` returns non-blank text so the
`if (content.isBlank())` skip (`data/EpubParser.kt:57`) never triggers.

Secondary (perf): `readEntries` (`data/EpubParser.kt:77-98`) is called three times
(`:19`, `:27`, `:52`), each time re-opening the stream and re-inflating the zip from the start — three
full passes over a file that may be just under the 50 MiB local-parse threshold
(`ui/UploadPresentation.kt:4,38-39`).

---

## 19. Reader scroll position leaks across chapters — **medium**

**Where** `App:1848`:
```kotlin
val listState = rememberLazyListState()
```
`ReaderScreen` is selected by `is AppRoute.Reader ->` in a single `when` (`App:550`), so navigating
from `Reader(b, 5)` to `Reader(b, 6)` reuses the same composable slot and the same
`LazyListState`. The chapter body is rendered as **one** lazy item (`App:1860-1867`), so
`firstVisibleItemScrollOffset` is a pixel offset inside the chapter.

**Failure scenario**
Read to the middle of a 6 000-word chapter (offset ≈ 4 000 px), tap 下一章. The new chapter loads and
the list stays at 4 000 px, so the reader opens roughly halfway through the new chapter. Also,
`toolbarsVisible`/`catalogVisible` (`App:1846-1847`) persist, and there is no scroll-offset
persistence at all — `ReaderProgressStore` only records `chapterId`
(`data/ReaderProgressStore.kt:34-50`), so "continue reading" always restarts at the top of the
chapter.

---

## 20. Prev/next silently dead when the catalog is incomplete — **medium**

**Where**
- `ui/ReaderAdjacentChapter.kt:14-15`:
  ```kotlin
  val selectedIndex = chapters.indexOfFirst { it.id == currentChapterId }
  if (selectedIndex < 0) return ReaderAdjacentChapters(previous = null, next = null)
  ```
- `App:1845` — `chapters = (state.chapters as? LoadResult.Success)?.value.orEmpty()`, so a failed
  catalog load yields an empty list.
- `App:2208-2210` — `TextButton(enabled = previous != null …)` / `enabled = next != null`.

**Failure scenario**
Open a chapter from a deep link or from 继续阅读 while `/api/novels/{id}/chapters` fails (very likely
given finding #1). The chapter text loads fine, but 上一章 and 下一章 are both greyed out with no
explanation, and the 目录 panel shows an error. Same effect when the returned catalog is paginated or
filtered server-side and does not contain the chapter the user is on. `Api:2015` additionally sorts
number-less chapters to the end (`it.chapter.number ?: Int.MAX_VALUE`), so a partially numbered
catalog produces wrong prev/next neighbours.

---

## 21. WebView and ImageLoader lifecycle — **medium**

**WebView leak** — `ui/WebFallbackScreen.kt:36-58`: the `AndroidView` has no `onRelease`/
`DisposableEffect`, so the `WebView` is never `destroy()`ed when the `WebFallback` route is popped.
Its renderer process, `WebViewClient`, and JS timers stay alive. `ui/EditorScriptEngine.kt:167-198`
does it correctly (`webView.destroy()` on both completion and cancellation), so the pattern is known
in this codebase.

Also `ui/WebFallbackScreen.kt:51` allocates a **new** `WebViewClient` on every recomposition, and
`:54` reloads whenever `webView.url != url` — after a server-side redirect (`/login` → `/`) that
condition is permanently true, so any recomposition of the shell re-navigates the WebView back to
`/login`. There is no in-WebView back handling either: the shell's `BackHandler` (`App:123`) pops the
route, so pressing back mid-login leaves the flow entirely.

**ImageLoader churn** — `configureNovalPieImageLoader` calls the global
`Coil.setImageLoader(...)` (`data/NovalPieImageLoading.kt:12-14`) from three places:
`MainActivity.onCreate` (`MainActivity.kt:14`), the ViewModel `init` (`VM:553`), and
`saveProxySettings` (`VM:681`). Each call builds a new `OkHttpClient` + `ImageLoader` **on the main
thread** and discards the previous memory cache. Consequence: two rebuilds on every cold start, one
more on every rotation (Activity recreated), and a full cover re-download storm each time.

**CookieManager** — `VM:439-441`:
```kotlin
cookieProvider = { runCatching { CookieManager.getInstance().getCookie("https://novalpie.cc") }.getOrNull() }
```
This lambda is invoked from `Api:1644`, which runs on `Dispatchers.IO`. If WebView provider
initialisation fails or throws on that thread, `getOrNull()` swallows it and the request silently goes
out **without cookies** — indistinguishable from "not logged in". No log, no message.

---

## 22. `EditorProcessor.chaptersFromMatches` throws on overlapping matches — **medium**

**Where** `data/EditorProcessor.kt:101-119`, reached from `splitByRegex` (`:9-16`), which merges
matches from *multiple* user patterns:
```kotlin
val matches = patterns.filter(String::isNotBlank)
    .flatMap { pattern -> Regex(pattern, MULTILINE).findAll(text).toList() }
    .distinctBy { it.range.first }
    .sortedBy { it.range.first }
…
val bodyStart = match.range.last + 1
val bodyEnd = matches.getOrNull(index + 1)?.range?.first ?: text.length
val body = text.substring(bodyStart, bodyEnd).trim()
```
`distinctBy { it.range.first }` only removes matches with the *same* start; it does not remove
matches nested inside another match.

**Failure scenario**
The 分章 panel accepts one regex per line (`VM:2030-2033`). User enters:
```
^第[0-9]+章.*$
^.*序章.*$
```
against text containing `第12章 序章回顾`. Pattern 1 matches `[100, 110]`, pattern 2 matches the same
line but the sort keeps both if their starts differ (e.g. a second pattern matching a substring at
`[104, 108]`). For the first match `bodyStart = 111` and `bodyEnd = 104`, so
`text.substring(111, 104)` throws `StringIndexOutOfBoundsException`. `processEditorSplit` wraps it in
`runCatching` (`VM:2028-2051`), so the user gets
`分章失败：begin 111, end 104, length 250000` — a raw Java message with no actionable meaning.

---

## 23. The script sandbox does its heavy work on the main thread — **medium**

**Where** `ui/EditorScriptEngine.kt:149-161`:
```kotlin
suspend fun process(...) = withContext(Dispatchers.Main.immediate) {
    val chunks = if (chunked) chunkEditorScriptText(text, targetChunkSize) else listOf(text)
    …
    buildString(text.length) { chunks.forEachIndexed { … append(evaluate(buildEditorScriptProgram(script, chunk, options[index]))) } }
}
```
`chunkEditorScriptText` (string splitting over the whole novel), `buildEditorScriptProgram`
(`JSONObject.quote` of each chunk, `:75-76`) and the final `buildString` concatenation all run on the
UI thread. Only `evaluateJavascript` itself suspends.

**Failure scenario**
Non-chunked run on a 2 M-char text: `JSONObject.quote(text)` on the main thread allocates a ~4 MB
escaped string synchronously (visible jank / possible ANR), and the resulting `evaluateJavascript`
program string exceeds what the Chromium IPC bridge will accept, so the callback returns `null` and
`parseEditorScriptCallback` (`:135-146`) throws `JSONTokener` noise instead of a useful message.
`withTimeout(15_000)` is *per chunk* (`:163`), so a 20-chunk run can legitimately occupy the UI for
five minutes.

---

## 24. `NovelCard.id` falls back to `0L` — **medium**

**Where** `Api:1844-1857`:
```kotlin
val id = if (favoriteObjectId != null && (…)) favoriteObjectId else null
    ?: source.longOrNull("id")
    ?: source.longOrNull("novel_id")
    ?: source.longOrNull("novelId")
    ?: 0L
```

**Failure scenarios**
1. `App:1302` / `App:1439` — the card's `onClick` calls `onOpenBook(book.id)` → `openBook(0)` →
   `if (bookId <= 0) return` (`VM:3671`). The card looks normal and simply does nothing when tapped.
2. `App:1298` and `App:1435` build LazyColumn keys from the row's ids:
   ```kotlin
   items(visibleBooks.chunked(columns), key = { it.joinToString { b -> b.id.toString() } })
   ```
   Three or more id-less books produce two rows whose key is both `"0, 0"`, which violates the
   uniqueness contract and throws `IllegalArgumentException: Key "0, 0" was already used`, crashing
   the bookshelf/search list. Page 1 is not de-duplicated (`mergeBooksById` at `VM:4009-4012` only
   runs on load-more), so nothing prevents it.

---

## 25. `intOrNull` silently truncates to 32 bits — **medium**

**Where** `Api:3332`:
```kotlin
private fun JSONObject.intOrNull(key: String): Int? = longOrNull(key)?.toInt()
```
Used for ~60 fields, including `total`, `total_pages`, `total_count`, `word_count`-adjacent counters,
`view_count`, `points`-adjacent values, and `remaining_time`.

**Failure scenario**
`/api/messages` returns `"total": 5000000000`. `toInt()` wraps to `705032704`. Any UI derived from it
(`ToolsMessageStats` at `App:2341-2347`, `AdminOperationLogPage.total` at `Api:479`) shows a
nonsensical number. More dangerous: a wrapped `total_pages` can go negative, which turns
`pagination.page >= pagination.totalPages` (`VM:1308`) into an immediate `return` — load-more dies.
`Long`s stay `Long` elsewhere (`Api:3323-3330`), so the truncation is gratuitous.

---

## 26. `clearReaderProgress` erases every book's progress — **medium**

**Where**
- `App:1259` — the 清除 button on the "continue reading" card maps to `onClearReaderProgress`.
- `VM:3699-3703` → `readerProgressStore.clear()`.
- `data/ReaderProgressStore.kt:52-54` — `prefs.edit().clear().apply()` wipes the whole
  `novalpie_native_reader_progress` file, including every `book_<id>_chapter_id` entry and the
  `recent_book_ids` list.

**Failure scenario**
User has progress in 15 books. The 继续阅读 card shows the most recent one; they tap 清除 expecting to
dismiss that one card. All 15 books' positions and the entire 最近在读 section
(`App:1263-1271`) disappear permanently. `bookDetailState.readerProgress` is also not reset
(`VM:3699-3703` does not touch it), so an already-open book detail keeps showing the stale
"you were here" marker (`App:1786`, `App:1796`).

---

## 27. Admin entry points fail silently — **medium**

**Where**
- `VM:1002` — `openAdminSection`: `if (!isAdminProfile(currentUserProfile())) return`
- `VM:1038` — `loadAdminSectionInternal`: same guard
- `VM:1233` — `runAdminMutation`: `if (!isAdminProfile(...) || adminState.actionLoading) return`
- `ui/ProfilePresentation.kt:8` — `isAdminProfile(profile) = profile?.role == "admin"` (exact match)
- `VM:1662-1663` — `currentUserProfile()` prefers `homeState.user`, else the JWT profile
- `data/AuthSessionStore.kt:25-27` — the JWT profile's `role` defaults to `"user"`

**Failure scenario**
An admin is inside 管理后台. A background `loadHome()` runs (e.g. after `saveProxySettings`,
`VM:682`) and `api.currentUser()` fails. `resolveUserLoadResult` (`VM:4032-4041`) substitutes the
JWT profile; if that token's payload has no `role` claim it becomes `"user"`. From then on the 刷新
button (`App:289`), the section tabs, and every 审核/删除 action are **completely inert** — no error,
no toast, nothing. The role string is also compared exactly, so any server-side variant
(`"administrator"`, `"superadmin"`, `"admin,editor"`) permanently locks the admin out.

---

## 28. Counters report the loaded page size as the total — **medium**

**Where**
- `App:1225-1238` → `ui/LibraryPresentation.kt:15-20`:
  `favoriteCount` = `favorites.value.size`, rendered as `收藏 N`.
- `App:1318` — `idleText = "已显示 ${visibleBooks.size} 本"` uses the *filtered* count while
  `favoritesCanLoadMore` (`VM:3772`) refers to unfiltered pages.
- `App:700-702` — `ForumStat("主题", items.size)` / `sumOf { it.replyCount }` over the (possibly fake)
  page-1 list.
- `App:1452` — `"已显示 ${results.value.size} 个结果"`.

**Failure scenario**
A user with 500 favourites opens 书架. `PAGE_SIZE = 20` (`VM:4027`), so the overview pill reads
`收藏 20`. After typing a filter that matches 3 books, the footer says `已显示 3 本` while the
加载更多收藏 button is still enabled — the two numbers describe different sets.

---

## 29. Synthetic illustration ids can collide — **low-medium**

**Where** `Api:2804-2812`:
```kotlin
id = item.longOrNull("id") ?: item.longOrNull("image_id") ?: item.longOrNull("imageId") ?: (fallbackIndex + 1L)
```
**Failure scenario** A response mixes items with and without ids: `[{id: 3, …}, {src: …}, {src: …}]`
→ ids `3, 2, 3`. `deleteManagedChapterIllustration(3)` (`VM:3535`) then sends whichever `imageId` the
tapped row carried, and the UI can delete the wrong row (or the server deletes an image the user did
not select). `readerImagePlaceholdersFromIllustrations` (`ui/ReaderText.kt:11-21`) also keys the
placeholder map on `index`, which has the same synthetic fallback (`Api:2809`), so duplicate indices
silently overwrite each other in the `associate` call.

---

## 30. `EditorArchiveStore.save` is destructively non-atomic — **low-medium**

**Where** `data/EditorArchiveStore.kt:16-29`:
```kotlin
metadataTemporary.writeText(…); textTemporary.writeText(…)
if (metadataTarget.exists() && !metadataTarget.delete()) throw IOException("无法替换现有存档信息")
if (textTarget.exists() && !textTarget.delete()) throw IOException("无法替换现有存档正文")
if (!metadataTemporary.renameTo(metadataTarget) || !textTemporary.renameTo(textTarget)) throw IOException("保存存档失败")
```
**Failure scenario** Overwriting an existing archive when the device is out of storage: both
`.tmp` writes may partially succeed, the targets are deleted, and the rename fails →
the old archive and the new one are both gone. If only the *second* rename fails, the metadata points
at a `.txt` that no longer exists, and `load` (`:42`) returns an archive with empty `textContent`
without any warning.

---

## 31. Loads with no staleness guard at all — **low-medium**

The codebase defines 16 `*RequestSerial` counters (`VM:451-466`) plus the
`ui/RequestFreshness.kt` helpers, but several loads use neither:

| Load | Line | Guard |
|---|---|---|
| `loadSearchTags` | `VM:3909-3916` | only `if (searchTags is LoadResult.Loading) return`; no serial |
| `startPoliticalExam` | `VM:2283-2309` | none — overwrites `politicalExamState` wholesale |
| `submitPoliticalExam` | `VM:2373-2395` | only the `submitting` flag |
| `loadForumPostDetail` | `VM:2805-2813` | route equality only, no serial |
| `loadBookDetail` | `VM:3927-3955` | route + `state.bookId`, no serial |
| `loadReader` | `VM:3966-3987` | route + `state.bookId/chapterId`, no serial |
| `sendMessageDraft` | `VM:1527-1552` | none (see #13) |
| `runWorkspaceAction` / `saveWorkspaceApi` / `deleteWorkspaceLocalApi` | `VM:2672`, `2525`, `2584` | `actionLoading` only |

`loadSearchTags`'s `Loading` short-circuit also means the 刷新 button on 热门标签 (`App:1480`) does
nothing while a previous request is in flight, and because `searchTags` is never reset to `Idle`, a
tag request that is dropped by a proxy stall leaves the section spinning until it times out.

`startPoliticalExam`: tap 开始考试, tap 返回 → the exam response still lands and sets
`phase = Active` on a screen the user has left; re-entering shows a live exam the user never started
(and `resetPoliticalExam`, `VM:2398-2400`, cannot prevent it).

`loadForumPostDetail` for the *same* postId: `reactOnForumPost` fires it after every like
(`VM:2921`). Double-tapping 赞 quickly queues two loads that both pass the route check; the slower one
wins and can show older like counts than the faster one.

---

## 32. `unwrapObject` blanks out non-object payloads — **low**

**Where** `Api:3047-3068`:
```kotlin
private fun unwrapObject(raw: Any, vararg keys: String): JSONObject {
    if (raw is JSONObject) { … return raw }
    return JSONObject()          // <-- arrays and strings become {}
}
```
**Failure scenario** `/api/users/me/checkins/stats` returns a bare array or a plain-text body (a
proxy interstitial, an HTML error page — `parseJsonOrString` at `Api:1741-1749` returns the raw
`String` for anything not starting with `{`/`[`). `userCheckinStats` (`Api:731-737`) then reads every
field from an empty object and reports `totalDays = 0, totalPoints = 0, maxStreak = 0,
currentStreak = 0` as a **successful** result. The user sees a fully populated 签到统计 card full of
zeros with no error. The same silent-zeros path exists for `messageStats`, `adminOverview`,
`workspaceApiStatus`, `messageSettings`, and `managedBookPermissions` (which then reports every
permission as `false`, disabling the whole edit form).

Also related: `extractArray`'s last-resort branch (`Api:3038-3044`) returns the values of any
numeric-keyed properties, so an object like `{"1": {...}, "code": 500}` is interpreted as a list of
domain objects.

---

## 33. Numeric text fields coerce unparseable input — **low**

- `ui/BookChapterScreens.kt:361-367` — 插入位置:
  `onValueChange = { onChange(draft.copy(insertAt = it.toIntOrNull() ?: 0)) }` with
  `value = draft.insertAt.toString()`. No `KeyboardOptions(keyboardType = Number)`, so an alphabetic
  keyboard appears; typing any non-digit or clearing the field snaps the displayed value to `0` and
  the validation error `插入位置必须大于 0` appears (`ui/BookChapterPresentation.kt:13`).
- `ui/MessageScreens.kt:605-611` — 多少天后自动已读: `value.toIntOrNull()` maps unparseable text to
  `null`, which `validateMessageSettings` (`ui/MessagePresentation.kt:33`) treats as valid `0`, i.e.
  the setting is silently cleared instead of rejected.
- `ui/AdminScreens.kt:761,770` — 价格: `price.toLongOrNull() ?: 0` on submit, guarded by
  `(price.toLongOrNull() ?: -1) >= 0` on the button, so a 20-digit price disables the button with no
  explanation.

Correctly handled, for the record: proxy port (`VM:666-668` digit filter + `VM:674`
`coerceIn(1, 65535)`), birth year (`VM:780` 4-digit filter + `VM:871-874` `in 1900..currentYear`),
workspace concurrency (`ui/WorkspacePresentation.kt:44-45` `in 1..100`, validated before
`toInt()` at `VM:2534`), split target and script chunk size
(`ui/EditorPresentation.kt:39-45`, checked before the `toInt()` calls at `VM:2040-2041`), and access
thresholds (`ui/BookChapterPresentation.kt:53-61` + `Api:2844-2852`).

---

## Additional smaller observations (not ranked above)

- `VM:1444-1459` `deleteCurrentMessage` calls `goBack()` unconditionally on success. If the user has
  already navigated elsewhere while the DELETE was in flight, this pops whatever route is now on top.
  It also never resets `actionLoading` on the success path.
- `VM:883-889` `verifyCurrentUserAdult` sets `isAdult = action.success`, so a *failed* verification
  overwrites an existing `isAdult = true` with `false`. If `profileState.profile` is not yet
  `Success`, `verified` is `null` and the whole result is discarded except for the message.
- `VM:1666-1668` `openUploadBook` only resets the state when `existingNovelId != null`, so
  re-entering 上传书籍 after a successful upload still shows the previous file, the previous draft, and
  the stale `上传成功` banner.
- `VM:2216-2251` `sendEditorToUpload` writes an EPUB into `cacheDir` and never deletes it; repeated
  use accumulates full-book copies in the cache.
- `ui/UiNavigation.kt:19-38` `routeContextLabel` has no branch for `AppRoute.PoliticalExam` or
  `AppRoute.WebFallback`, so the top-bar subtitle falls back to the bottom-tab name on those screens.
- `App:3470-3487` `ReaderBody` mutates `var imageOrdinal` during composition and passes `++imageOrdinal`
  into a child — a Compose anti-pattern that can produce stale illustration numbers under partial
  recomposition.
- `App:1860-1867` renders the entire chapter as a **single** lazy item, so a long chapter creates
  hundreds of `Text` composables eagerly; the `LazyColumn` provides no virtualisation for the body.
- `Api:2142-2145` `rotateLeft3Hex` does `timestamp.toLong().toInt()`, i.e. it deliberately truncates
  epoch seconds to 32 bits for the reader signature. Fine until 2038, but worth a comment.
- `App:663-673` renders the 全部/书评/章节/动态 filter chips with `onClick = {}` — decorative controls
  that do nothing.
- `Api:1757-1760` `normalizePoliticalExamSession` contains a magic reshaping rule
  (`if (singleChoice.isEmpty() && multipleChoice.size == 50) { take(40); drop(40) }`) that will
  silently mis-slice the paper if the server ever returns 50 genuine multiple-choice questions.
