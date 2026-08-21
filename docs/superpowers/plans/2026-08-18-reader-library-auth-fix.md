# Reader, Library, Auth, Detail, and Forum Fix Implementation Plan

> **For agentic workers:** Execute one verified behavior at a time. Every production behavior begins with a failing unit test, then receives focused emulator verification when the MuMu device is online.

**Goal:** Make in-app login verification work on MuMu, reduce collection/profile book-card noise, preserve chapter comments in continuous reading, advance page-turn reading at a chapter boundary, make detail tabs exclusive, and distribute all forum categories evenly.

**Architecture:** Keep network routing in `WebFallbackScreen.kt`; keep card and tab decisions in presentation helpers where they can be unit-tested; make reader comment state explicitly keyed by chapter ID so a continuous reading window owns each chapter's discussion independently of the current route.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, ViewModel/coroutines, Robolectric/JUnit, Android WebView, Android TextToSpeech, adb/MuMu.

---

### Task 1: CAPTCHA proxy route

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/WebFallbackScreen.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/WebFallbackPolicyTest.kt`

- [ ] Add a failing test proving a VirtualBox/MuMu WebView chooses `127.0.0.1:7890`, while an AVD still uses its documented proxy fallback.
- [ ] Change `webViewProxyUrl` to receive the selected emulator host rather than hard-coding `10.0.2.2`.
- [ ] Run `:app:testReleaseUnitTest --tests '*WebFallbackPolicyTest*'` and verify it passes.

### Task 2: Compact library cards and source progress marker

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/LibraryPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ProfileScreens.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/LibraryPresentationTest.kt`

- [ ] Add failing tests for a `read/total` marker and a completed-book update marker.
- [ ] Render collection cards as cover, title, author, and optional progress/update text only; retain long-press selection and 2/3/4-column alignment.
- [ ] Render uploaded-book cards as cover, title, and author only; retain their independent 2/3/4-column selector and disabled cover preview.

### Task 3: Reader per-chapter comments and page boundary advance

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`

- [ ] Add failing tests for per-chapter comment lookup/order and page-turn boundary behavior.
- [ ] Preserve `LoadResult` comments by chapter and append each chapter's section immediately after that chapter's body.
- [ ] Fetch appended chapter comments independently so a slow comment request cannot block next-body loading.
- [ ] When a page turn has reached the final viewport, open the adjacent chapter instead of leaving a dead end.

### Task 4: Exclusive detail tabs and equal forum categories

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/BookDetailPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ForumPresentation.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/BookDetailPresentationTest.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ForumPresentationTest.kt`

- [ ] Add failing tests for one visible detail section and a five-way category allocation.
- [ ] Replace detail scroll anchors with an exclusive `Introduction`, `Catalog`, or `Comments` content branch.
- [ ] Replace the forum's content-width `LazyRow` category layout with a five-equal-width row.

### Task 5: Verification and evidence

**Files:**
- Modify: `docs/APP2_COMPLETION_AUDIT.md`
- Modify: `D:\NovalPie\agent-bridge\bridge-state.md`
- Modify: `D:\NovalPie\agent-bridge\progress-log.md`

- [ ] Run focused tests after every change and then `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain`.
- [ ] Install with `adb install -r`, set only `adb reverse tcp:7890 tcp:7890`, and retain app data/login state.
- [ ] Capture current screenshots and logcat evidence in `D:\NovalPie\agent-bridge\screenshots`.
