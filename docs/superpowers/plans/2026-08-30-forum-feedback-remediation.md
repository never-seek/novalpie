# Forum Feedback Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the reproducible defects reported in forum post 1871 while preserving the native reader, forum, and download flows already verified on the installed APK.

**Architecture:** Keep presentation policy in the existing small UI helpers so unit tests can exercise reader layout, formatted text, Chinese conversion, poll mapping, and request-state behavior without a device. Compose remains responsible only for rendering the resulting state; `NovalPieApi` owns source API normalization and `NovalPieViewModel` owns asynchronous request serial/state protection.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Robolectric/JUnit, OkHttp/JSON source client, Android Debug build, MuMu via ADB.

---

## Evidence and root cause baseline

- Forum post `1871` currently has three votes, all for “还不错，有点小毛病”, three top-level comments, and three nested replies.
- The currently installed native forum detail does not render the post poll because `ForumPostDetail` has no source poll model and `normalizeForumPostDetail()` drops it.
- `turnReaderPage()` applies a `0.86 * viewport` pixel scroll. It therefore lands inside arbitrary text/image blocks; the volume keys invoke the same path.
- Fullscreen reader removes the root Scaffold inset budget while transient system bars can still be visible, so the article can be painted beneath the status bar.
- `Html.fromHtml(...).toString()` removes bold spans. Per-paragraph `SelectionContainer`s prevent selection across headings and paragraphs. `Text(AnnotatedString)` skips traditional/simplified conversion.
- The source exposes `POST /api/novels/{novelId}/chapters/request` for the authenticated, non-upload book-detail “获取新章” action. It is not equivalent to a local directory refresh.

### Task 1: Lock reader viewport geometry and page-turn behavior

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [ ] **Step 1: Write failing presentation tests for protected system bars and anchored page movement.**

```kotlin
@Test
fun fullscreenReaderKeepsTransientSystemBarsOutOfTheArticleViewport() {
    assertEquals(true, readerContentUsesSystemBarPadding(isFullscreen = true, systemBarsVisible = true))
}

@Test
fun pageTurnMovesBetweenReaderItemsInsteadOfAnArbitraryViewportFraction() {
    assertEquals(ReaderPageMove.NextItem, readerPageMoveFor(direction = 1, currentItemIndex = 3, itemCount = 8))
    assertEquals(ReaderPageMove.PreviousItem, readerPageMoveFor(direction = -1, currentItemIndex = 3, itemCount = 8))
}
```

- [ ] **Step 2: Run the focused test class and confirm RED.**

Run:
`./gradlew :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.ReaderPresentationTest --rerun-tasks --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false --console=plain`

Expected: compilation/test failure for the missing page/system-bar policy, not an unrelated Gradle error.

- [ ] **Step 3: Implement the minimum policy and consume it in the reader.**

```kotlin
internal fun readerContentUsesSystemBarPadding(isFullscreen: Boolean, systemBarsVisible: Boolean): Boolean =
    !isFullscreen || systemBarsVisible

internal enum class ReaderPageMove { PreviousItem, NextItem, AtStart, AtEnd }
```

Replace fractional `scrollBy(viewport * 0.86f)` page movement with `LazyListState.animateScrollToItem()`/`scrollToItem()` at a stable content-item boundary, retaining explicit adjacent-chapter behavior at the first/last item. Apply status-bar padding while bars are visible and retain immersive layout only after the bars are hidden.

- [ ] **Step 4: Re-run the focused test and verify GREEN.**

Run the same command from Step 2. Expected: `BUILD SUCCESSFUL` with zero failures.

### Task 2: Preserve reader rich text, selection, and Chinese variants

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderTextTest.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ChineseVariantPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderText.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ChineseVariantPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [ ] **Step 1: Add failing format and conversion tests.**

```kotlin
@Test
fun readerParagraphsKeepStrongAndBoldRanges() {
    val paragraph = readerFormattedParagraphsFromContent("<p>A <strong>bold</strong> <b>word</b></p>").single()
    assertEquals("A bold word", paragraph.text)
    assertEquals(2, paragraph.boldRanges.size)
}

@Test
fun traditionalConversionRetainsAnnotatedRanges() {
    val value = annotatedReaderText("阅读 小说", bold = 0..1)
    val converted = convertChineseVariantAnnotatedText(value, ChineseVariant.Traditional)
    assertEquals("閱讀 小說", converted.text)
    assertEquals(value.spanStyles, converted.spanStyles)
}
```

- [ ] **Step 2: Run both focused classes and confirm RED.**

Run:
`./gradlew :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.ReaderTextTest --tests com.novalpie.nativeapp.ui.ChineseVariantPresentationTest --rerun-tasks --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false --console=plain`

Expected: formatter/converter symbols or assertions fail because spans are currently flattened/skipped.

- [ ] **Step 3: Implement rich text and a single selection scope.**

Use `Html.fromHtml()` spans to build an `AnnotatedString` with `SpanStyle(fontWeight = FontWeight.Bold)` for `StyleSpan.BOLD`/`Typeface.BOLD`. Convert `AnnotatedString.text` through the existing Chinese mapper while copying its annotations/spans. Wrap the entire reader article item stream in one `SelectionContainer` and remove nested per-heading/per-paragraph containers.

- [ ] **Step 4: Re-run focused tests and verify GREEN.**

Run the Step 2 command. Expected: zero failures.

### Task 3: Render source forum polls and native “获取新章” requests

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ForumPresentationTest.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/BookDetailPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/model/Models.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [ ] **Step 1: Write failing source-normalization and visibility tests.**

```kotlin
@Test
fun forumDetailKeepsPollQuestionOptionsAndCurrentVoteTotals() { /* fixture: API post 1871 */ }

@Test
fun nativeRequestNewChapterIsOnlyVisibleForAuthenticatedNonUploadBooks() {
    assertTrue(bookDetailShowsRequestNewChapter(hasAuthToken = true, source = "novelPia"))
    assertFalse(bookDetailShowsRequestNewChapter(hasAuthToken = false, source = "novelPia"))
    assertFalse(bookDetailShowsRequestNewChapter(hasAuthToken = true, source = "upload"))
}
```

- [ ] **Step 2: Run focused tests and confirm RED.**

Run:
`./gradlew :app:testDebugUnitTest --tests com.novalpie.nativeapp.data.NovalPieApiTest --tests com.novalpie.nativeapp.ui.ForumPresentationTest --tests com.novalpie.nativeapp.ui.BookDetailPresentationTest --rerun-tasks --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false --console=plain`

Expected: current model/API has no poll/request-new-chapter behavior.

- [ ] **Step 3: Implement source-faithful models, API mapping, and stateful UI.**

Add a read model for poll question/options/counts/user vote/end state to `ForumPostDetail`, normalize it from `/api/posts/{id}`, and render it before forum comments. Add `submitForumPoll(postId, optionIds)` with the source payload `{"option_ids":[...]}` but do not vote during automated QA. Add `requestNovelChapters(novelId)` with `POST /api/novels/{novelId}/chapters/request`, a ViewModel loading/result state, and a book-detail action shown only for a signed-in non-upload source.

- [ ] **Step 4: Re-run focused tests and verify GREEN.**

Run the Step 2 command. Expected: zero failures and API request payload assertions pass.

### Task 4: Improve reader setting language without changing stored behavior

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [ ] **Step 1: Add the failing summary-label assertion.**

```kotlin
@Test
fun readerLayoutSummaryUsesHumanReadingWidthRatherThanRawDp() {
    assertEquals("舒适宽度 · 翻页模式", readerSettingsOverviewSummary(ReaderSettingsCategory.Layout, options))
}
```

- [ ] **Step 2: Run the targeted test and confirm RED.**

Run the focused `ReaderPresentationTest` command from Task 1. Expected: actual summary includes raw `dp`.

- [ ] **Step 3: Map saved width values to source-style labels.**

Keep `contentWidthDp` unchanged in storage. Convert only the visible summary/tag to labels such as `窄`, `舒适宽度`, `宽`, and `全宽`.

- [ ] **Step 4: Re-run the focused test and verify GREEN.**

Expected: zero failures.

### Task 5: Build, preserve-state install, and runtime proof

**Files:**
- Modify: `docs/APP2_REGRESSION_AUDIT_LEDGER.md`
- Modify: `D:\NovalPie\agent-bridge\bridge-state.md`
- Create: `D:\NovalPie\agent-bridge\screenshots\20260830-feedback-*.png`
- Create: `D:\NovalPie\agent-bridge\artifacts\20260830-feedback-*.xml`

- [ ] **Step 1: Run complete automated gate.**

Run:
`./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false --console=plain`

Expected: all tests pass, lint says `No issues found`, and debug APK assembles.

- [ ] **Step 2: Install without clearing user data and restore proxy reverse.**

Run:
`adb -s 127.0.0.1:16384 install -r app/build/outputs/apk/debug/app-debug.apk`

Then:
`adb -s 127.0.0.1:16384 reverse tcp:7890 tcp:7890`

Expected: package replaces in place; login/session, favorites, reader progress, downloads, and settings are retained.

- [ ] **Step 3: Reproduce user paths with screenshots.**

Verify post 1871 displays poll data without submitting a vote; verify the book detail action is visible but do not trigger a point/credit consuming request; verify pagination and volume keys stop at paragraph/item boundaries, system bars do not cover text, bold source text renders bold, traditional mode changes reader text/chrome, and selection can span consecutive paragraphs.

- [ ] **Step 4: Update the audit ledger with only observed evidence.**

Record test command/result, device APK hash, screenshots/UI XML, and any external action deliberately not performed. Mark an item verified only after the matching current APK behavior has been observed.
