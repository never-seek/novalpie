# Native Message Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete app-owned native message center matching all currently observed `novalpie.cc/messages` data and actions.

**Architecture:** Add typed message models and API operations, independent route/state holders with request-serial protection, and a focused Compose screen file for inbox/detail/conversation/settings. Tools becomes the native entry point; server auth remains authoritative and administrator gating is untouched.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android ViewModel, coroutines, OkHttp/MockWebServer, `org.json`, JUnit/Robolectric, adb/MuMu.

---

## File structure

- Modify `app/src/main/java/com/novalpie/nativeapp/model/Models.kt`: typed message/query/page/settings/conversation models.
- Modify `app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt`: exact website message endpoints and normalization.
- Create `app/src/main/java/com/novalpie/nativeapp/ui/MessagePresentation.kt`: pure labels, validation, participant and state reducers.
- Create `app/src/main/java/com/novalpie/nativeapp/ui/MessageCenterScreen.kt`: inbox, detail, conversation, settings UI.
- Modify `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`: routes and request/action state.
- Modify `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`: route wiring and Tools callbacks only.
- Modify `app/src/main/java/com/novalpie/nativeapp/ui/UiNavigation.kt`: route labels where required.
- Extend `app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt`.
- Create `app/src/test/java/com/novalpie/nativeapp/ui/MessagePresentationTest.kt`.
- Extend `app/src/test/java/com/novalpie/nativeapp/ui/RouteStackPolicyTest.kt` and `RequestFreshnessTest.kt`.

### Task 1: Typed models and pure message behavior

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/model/Models.kt`
- Create: `app/src/main/java/com/novalpie/nativeapp/ui/MessagePresentation.kt`
- Create: `app/src/test/java/com/novalpie/nativeapp/ui/MessagePresentationTest.kt`

- [ ] **Step 1: Write failing model/presentation tests**

Add tests that require the intended public behavior:

```kotlin
@Test fun resolvesOtherDirectMessageParticipant() {
    val message = SiteMessage(
        id = 1,
        type = 8,
        title = "dm",
        userId = 20,
        executeUserId = 10
    )
    assertEquals(20L, directMessageTargetUserId(message, currentUserId = 10))
}

@Test fun validatesQuietHoursAndAutoReadDays() {
    assertNull(validateMessageSettings(MessageSettings()))
    assertEquals("免打扰开始时间格式无效", validateMessageSettings(MessageSettings(quietHoursStart = "25:99")))
    assertEquals("自动已读天数不能小于 0", validateMessageSettings(MessageSettings(autoReadAfterDays = -1)))
}

@Test fun mergesPagesWithoutDuplicateMessages() {
    val merged = mergeMessagePages(
        listOf(SiteMessage(1, 1, "a"), SiteMessage(2, 1, "b")),
        listOf(SiteMessage(2, 1, "b2"), SiteMessage(3, 1, "c"))
    )
    assertEquals(listOf(1L, 2L, 3L), merged.map { it.id })
    assertEquals("b2", merged[1].title)
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.ui.MessagePresentationTest' --console=plain
```

Expected: Kotlin compilation fails because the new models/helpers do not exist.

- [ ] **Step 3: Add minimal models and helpers**

Add models with these signatures:

```kotlin
data class MessageQuery(
    val keyword: String = "",
    val messageType: Int? = null,
    val isRead: Boolean? = null,
    val priority: Int? = null
)

data class MessagePagination(
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 1
)

data class MessagePage(
    val items: List<SiteMessage>,
    val pagination: MessagePagination
)

data class MessageSettings(
    val enableNotifications: Boolean = true,
    val enableEmail: Boolean = false,
    val enableBrowserPush: Boolean = true,
    val notificationTypes: Set<Int>? = null,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val autoReadAfterDays: Int? = null
)

data class DirectMessage(
    val id: Long,
    val content: String,
    val createdAt: String?,
    val userId: Long?,
    val executeUserId: Long?
)

data class MessageActionResult(val success: Boolean, val message: String? = null)
```

Expand `SiteMessage` with nullable `readAt`, `userId`, `executeUserId`, `avatarUrl`, `avatarFrameUrl`, and `extraData`.

Implement pure helpers:

```kotlin
internal fun directMessageTargetUserId(message: SiteMessage, currentUserId: Long?): Long? =
    listOf(message.executeUserId, message.userId).firstOrNull { it != null && it != currentUserId }

internal fun mergeMessagePages(current: List<SiteMessage>, next: List<SiteMessage>): List<SiteMessage> =
    (current + next).associateBy { it.id }.values.toList()
```

Use a strict `HH:mm` regex plus numeric range checks in `validateMessageSettings`.

- [ ] **Step 4: Run tests and verify GREEN**

Expected: `MessagePresentationTest` passes.

### Task 2: List, detail, and stats API parity

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt`

- [ ] **Step 1: Write failing request/normalization tests**

Add MockWebServer tests that call:

```kotlin
api.messagePage(
    query = MessageQuery(keyword = "更新", messageType = 4, isRead = false, priority = 2),
    page = 2,
    pageSize = 20
)
```

Assert the recorded path contains:

```text
/api/messages?page=2&page_size=20&message_type=4&is_read=false&priority=2&keyword=%E6%9B%B4%E6%96%B0
```

Return a fixture containing `list` and `pagination`; assert message metadata and pagination fields. Add `messageDetail(77)` and assert `GET /api/messages/77` plus detail normalization.

- [ ] **Step 2: Run the two tests and verify RED**

Expected: unresolved `messagePage` and `messageDetail`.

- [ ] **Step 3: Implement minimal read APIs**

Add:

```kotlin
suspend fun messagePage(query: MessageQuery, page: Int = 1, pageSize: Int = 20): MessagePage
suspend fun messageDetail(messageId: Long): SiteMessage
```

Only include optional query keys when non-null/non-blank. Extract `list` and `pagination`, supporting snake_case and camelCase. Reuse one `normalizeMessage(JSONObject)` function from both list and detail.

- [ ] **Step 4: Run targeted API tests and verify GREEN**

Expected: exact methods/paths and normalized values pass.

### Task 3: Mutation, settings, and conversation APIs

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt`

- [ ] **Step 1: Write failing mutation tests**

Cover each exact request:

```kotlin
api.markMessageRead(9)                 // POST /api/messages/9/read {"id":9}
api.markMessagesRead(listOf(9, 10))    // POST /api/messages/read {"ids":[9,10]}
api.markAllMessagesRead()              // POST /api/messages/read {"all":true}
api.starMessage(9, starred = false)    // POST /api/messages/9/star {"starred":0}
api.deleteMessage(9)                   // DELETE /api/messages/9 {"id":9,"permanent":false}
api.deleteMessages(listOf(9, 10))      // DELETE /api/messages {"ids":[9,10]}
```

Also test settings GET/PUT and conversation/send:

```kotlin
api.messageSettings()
api.updateMessageSettings(MessageSettings(enableEmail = true, quietHoursStart = "23:00"))
api.messageConversation(targetUserId = 20, page = 1, pageSize = 100)
api.sendDirectMessage(currentUserId = 10, targetUserId = 20, currentUserName = "seeking", content = "hello")
```

- [ ] **Step 2: Run tests and verify RED**

Expected: missing methods.

- [ ] **Step 3: Implement exact APIs and typed normalizers**

Use existing `post`, `put`, and `delete` helpers. If `delete` currently lacks a body, extend the internal request helper with an optional JSON body and cover it with the tests above. Normalize all action responses into `MessageActionResult`.

- [ ] **Step 4: Run targeted API tests and verify GREEN**

Expected: all request methods, paths, JSON bodies, settings, and conversation fixtures pass.

### Task 4: Routes and independent state machines

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/UiNavigation.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/RouteStackPolicyTest.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/RequestFreshnessTest.kt`

- [ ] **Step 1: Write failing route and reducer tests**

Require routes:

```kotlin
AppRoute.Messages
AppRoute.MessageDetail(77)
AppRoute.MessageConversation(targetUserId = 20, targetName = "UIvou")
```

Test Tools -> Messages -> Detail -> Back, then opening another ID produces only the new detail route. Add stale-page tests where serial `1` cannot replace serial `2`.

- [ ] **Step 2: Run tests and verify RED**

Expected: routes/state helpers unresolved.

- [ ] **Step 3: Add states and ViewModel operations**

Add:

```kotlin
data class MessageCenterState(
    val stats: LoadResult<MessageStats> = LoadResult.Idle,
    val page: LoadResult<MessagePage> = LoadResult.Idle,
    val query: MessageQuery = MessageQuery(),
    val selectedIds: Set<Long> = emptySet(),
    val loadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null,
    val settings: LoadResult<MessageSettings> = LoadResult.Idle,
    val settingsVisible: Boolean = false
)
```

Add matching detail and conversation states. Implement open/load/filter/page/action methods with separate serials. Successful actions update list/detail/stats from returned server state or a deterministic reducer; failures preserve current content and expose retry text.

- [ ] **Step 4: Run route/freshness tests and verify GREEN**

Expected: all route-stack and stale-request assertions pass.

### Task 5: Premium native inbox UI

**Files:**
- Create: `app/src/main/java/com/novalpie/nativeapp/ui/MessageCenterScreen.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/MessagePresentationTest.kt`

- [ ] **Step 1: Add failing copy/layout contract tests**

Assert inbox sections contain:

```kotlin
assertEquals(listOf("消息中心", "搜索消息", "类型", "状态", "优先级"), messageInboxCoreLabels())
assertEquals(listOf("全部已读", "设置"), messageInboxTopActions())
assertFalse(messageInboxCoreLabels().any { it.contains("WebView", ignoreCase = true) })
```

- [ ] **Step 2: Verify RED**

Expected: helper functions unresolved.

- [ ] **Step 3: Implement inbox Compose components**

Create `MessageCenterScreen` using:

- `LazyColumn` with bottom navigation-safe padding;
- stat `LazyRow`;
- Material 3 search field;
- horizontal filter rails;
- `ElevatedCard` rows with unread accent and star action;
- selection toolbar;
- load-more/retry row;
- confirmation dialogs for all-read and delete.

Wire `AppRoute.Messages` in `NovalPieApp.kt`. Keep `NovalPieApp.kt` limited to callbacks and route selection.

- [ ] **Step 4: Run tests and compile release Kotlin**

Run:

```powershell
.\gradlew.bat --no-daemon :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.ui.MessagePresentationTest' :app:compileReleaseKotlin --console=plain
```

Expected: tests pass and Compose compiles.

### Task 6: Detail, conversation, and settings UI

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/MessageCenterScreen.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/MessagePresentationTest.kt`

- [ ] **Step 1: Add failing behavior tests**

Cover direct-message target resolution, send payload copy, settings validation, destructive confirmation labels, relative conversation time labels, and action-link routing restricted to `novalpie.cc` or existing supported deep links.

- [ ] **Step 2: Verify RED**

Expected: missing detail/conversation/settings helpers.

- [ ] **Step 3: Implement the three native surfaces**

- `MessageDetailScreen`: metadata, body, mark-read, star, action, delete dialog.
- `MessageConversationScreen`: bubble list, refresh, composer, sending/failed states, retry.
- `MessageSettingsSheet`: switches, type chips, time/day fields, validation, save progress.

Use full-screen routes for detail/conversation and a modal bottom sheet for settings. Do not expose administrator controls.

- [ ] **Step 4: Run tests and compile**

Expected: helper tests and Compose compilation pass.

### Task 7: Replace Tools WebView message entry

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ToolsPresentation.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ToolsPresentationTest.kt`

- [ ] **Step 1: Write failing integration tests**

Require the regular message card to be native-owned and all recent message rows to expose a message ID callback. Preserve the existing regular/admin route lists for every non-message card.

- [ ] **Step 2: Verify RED**

Expected: current Tools entry still only contains `/messages` WebView routing.

- [ ] **Step 3: Wire native callbacks**

Add `onOpenMessages` and `onOpenMessage(id)` to `ToolsScreen`. The message-center card and full-message button open `AppRoute.Messages`; recent non-DM messages open detail; type-8 messages resolve/open conversation when IDs permit, otherwise open detail. Other cards retain their current safe fallback routes and administrator gating.

- [ ] **Step 4: Run Tools/navigation tests and verify GREEN**

Expected: message routes are native; admin visibility tests remain unchanged and pass.

### Task 8: Full verification, read-only MuMu QA, and docs

**Files:**
- Modify: `native-android/docs/LIVE_SITE_ROUTE_API_MATRIX.md`
- Modify: `NOVALPIE_WORKSPACE_MASTER_README.md`
- Add evidence under `agent-bridge/screenshots` and `agent-bridge/artifacts` (historical bridge directory only; no Gemini workflow).

- [ ] **Step 1: Run full build verification**

```powershell
.\gradlew.bat --no-daemon :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease --console=plain
```

Expected: `BUILD SUCCESSFUL` with zero failing tests.

- [ ] **Step 2: Install without clearing app data**

```powershell
adb -s 127.0.0.1:16384 reverse --remove-all
adb -s 127.0.0.1:16384 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s 127.0.0.1:16384 shell am force-stop com.novalpie.app.debug
adb -s 127.0.0.1:16384 shell am start -n com.novalpie.app.debug/com.novalpie.nativeapp.MainActivity
```

- [ ] **Step 3: Perform read-only UI-tree-driven QA**

Verify:

- Tools -> native Messages;
- stats/list/filter/pagination rendering;
- non-destructive detail rendering;
- direct-message conversation load without sending;
- settings load without saving;
- system Back at each depth;
- ordinary/admin route visibility remains role-correct.

Do not tap mark-read, star, send, delete, bulk delete, all-read, or settings save on the real account.

- [ ] **Step 4: Capture evidence and inspect app-process logcat**

Capture inbox, filtered inbox, detail, conversation, and settings screenshots plus UI XML. Search logcat for `FATAL EXCEPTION`, `AndroidRuntime`, `UnknownHost`, `SSLHandshake`, `ConnectException`, `SocketTimeoutException`, and `OutOfMemoryError`.

- [ ] **Step 5: Recompute APK hashes and update durable docs**

Record exact hashes, test count, screenshots, routes, endpoints, and remaining native migration gaps. Do not mark the overall App 2.0 goal complete.

## Execution note

This workspace is not assumed to be a clean Git repository, and the user did not request commits. Execute inline in the current workspace, preserve unrelated changes, and do not create commits or branches.
