# Forum Markdown and EPUB Throughput Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the source website's visible Markdown/comment behavior in native forum, book-review, and chapter-comment surfaces, and make native EPUB exports download source illustrations concurrently without duplicating temporary-file copies.

**Architecture:** Keep server content as raw Markdown/HTML and parse it into a safe native presentation model rather than using a WebView or evaluating source HTML. The forum renderer will preserve source Markdown blocks and custom `||spoiler||` / `[fold:title]...[/fold]` syntax, while the comment composer will insert the exact syntax offered by the website's `MarkdownEditor`. EPUB export will spool text chapters to temporary files, discover unique images, fetch them through a bounded worker pool directly to one temporary file per image, then write the ZIP sequentially.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Kotlin coroutines, OkHttp streaming, `ZipOutputStream`, JUnit/Robolectric.

---

### Task 1: Capture the source-format contract in failing presentation tests

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ForumPresentationTest.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/data/NativeEpubArchiveWriterTest.kt`

- [ ] **Step 1: Add a source Markdown document regression**

Add a test that passes one document containing a heading, italic/bold/underline/strike/inline-code text, an internal Markdown link, a quote, unordered and ordered list items, a fenced code block, a table, `||spoiler||`, and `[fold:标题]内容[/fold]` into `forumMarkdownDocument`. Assert that no textual payload is discarded and that the returned blocks preserve heading, link, spoiler, fold, code, list, and table identities.

- [ ] **Step 2: Add a source editor-toolbar insertion regression**

Add a table-driven test for `forumMarkdownToolbarInsertion` proving the native actions emit the exact website syntax: `**文字**`, `*文字*`, `## 标题`, `> 引用文字`, `` `代码` ``, `[链接文字](url)`, `- 列表项`, `<u>下划线文字</u>`, `~~删除线文字~~`, `||黑幕文字||`, and `[fold:点击展开]\n这里是可以折叠的内容...\n[/fold]`.

- [ ] **Step 3: Add a bounded EPUB image-concurrency regression**

Add a writer test with at least three distinct image URLs and an `openAsset` test double that increments an in-flight counter, delays briefly, then returns bytes. Assert that at least two fetches overlap and that the observed maximum never exceeds `NATIVE_EPUB_IMAGE_DOWNLOAD_PARALLELISM`.

- [ ] **Step 4: Run the focused tests and confirm red**

Run:

```powershell
.\gradlew.bat :app:testReleaseUnitTest --tests com.novalpie.nativeapp.ui.ForumPresentationTest --tests com.novalpie.nativeapp.data.NativeEpubArchiveWriterTest --offline --no-daemon --max-workers=1 --console=plain
```

Expected: the Markdown/document and toolbar symbols are absent or the EPUB test reports a serial maximum of `1`; no production implementation is added before this result is observed.

### Task 2: Implement a safe source-compatible Markdown model and parser

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ForumPresentation.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ForumPresentationTest.kt`

- [ ] **Step 1: Add native block and inline presentation types**

Add `ForumMarkdownBlock` types for paragraph, heading, quote, list, code block, horizontal rule, table, fold, and image. Extend `ForumTextSegment` with italic, underline, strikethrough, inline code, image reference, and source book reference while retaining existing plain, bold, link, and spoiler behavior.

- [ ] **Step 2: Parse source blocks without dropping raw text**

Implement `forumMarkdownDocument(raw)` with a block parser for GFM headings/lists/quotes/code fences/tables/rules and the source custom fold marker. Convert source HTML aliases (`p`, `br`, `a`, `strong`, `em`, `u`, `s`, `code`, `pre`, `blockquote`, list tags, `details`, and `.spoiler`) into the same safe model. Unsupported markup must remain visible as text after stripping only the tag delimiters.

- [ ] **Step 3: Parse source inline forms and links**

Recognize Markdown links with absolute and `/forum/...` or `/book/...` relative URLs, raw URLs, HTML anchors, markdown images, HTML images, source `bookid` references, native inline code, and the source format buttons' bold/italic/underline/strike/spoiler forms. Keep code spans/fences opaque so `||` and `[fold:]` inside code do not become interactive syntax.

- [ ] **Step 4: Implement `forumMarkdownToolbarInsertion`**

Return a replacement string plus selection range for every source toolbar action. Use the exact strings from `site-chunks/CNfb3ub5.js` and keep an existing selection inside the matching delimiter pair.

- [ ] **Step 5: Run the focused forum test class and confirm green**

Run the Task 1 test command. Expected: all `ForumPresentationTest` cases pass, including legacy spoiler/link tests and the new source-format cases.

### Task 3: Render the document and expose the source editor controls in Compose

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ForumPresentationTest.kt`

- [ ] **Step 1: Replace paragraph-only content rendering with native block rendering**

Make `ForumRichContent` consume `forumMarkdownDocument`. Render headings, quotes, lists, code, tables, folds, images, links, and spoilers with Compose. A fold starts collapsed and toggles only its own body. A spoiler remains masked by default and reveals only its own segment. Existing explicit link handling remains the route owner; neither links nor spoilers may fall through to card navigation.

- [ ] **Step 2: Preserve compact feed behavior**

Keep `ForumFeedExcerpt` bounded and fast, but parse its text through the same inline rules so relative links and spoiler boundaries are not lost. Full images, tables, and fold bodies remain detail-only to avoid expensive feed composition.

- [ ] **Step 3: Replace both comment text fields with `ForumMarkdownComposer`**

Replace `ForumCommentComposer` and `InlineCommentComposer` internals with one reusable editor that contains the source toolbar actions, selection-aware insertion, a character counter, a Markdown preview switch, and the source spoiler/fold hint. Keep the existing draft callbacks and submit/reply behavior unchanged.

- [ ] **Step 4: Run source and UI-focused tests**

Run:

```powershell
.\gradlew.bat :app:testReleaseUnitTest --tests com.novalpie.nativeapp.ui.ForumPresentationTest --tests com.novalpie.nativeapp.ui.BookDetailPresentationTest --tests com.novalpie.nativeapp.ui.ReaderPresentationTest --offline --no-daemon --max-workers=1 --console=plain
```

Expected: source-format, forum, book-review, and chapter-comment presentation tests pass.

### Task 4: Rework EPUB image staging and bounded concurrency

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/NativeEpubArchiveWriter.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/data/NativeEpubArchiveWriterTest.kt`

- [ ] **Step 1: Preserve streaming text while discovering image work**

Spool each parsed source chapter into a temporary text file, retaining only its title and path in memory. Scan those chapter files for unique image URLs in source order; do not materialize the complete book in a `String`.

- [ ] **Step 2: Fetch unique assets through a bounded worker pool**

Add `NATIVE_EPUB_IMAGE_DOWNLOAD_PARALLELISM = 4`. Download each unique source asset directly into exactly one writer-owned temporary file with four coroutine workers at most. Keep original bytes and media type; cancellation must stop workers and delete all staged files.

- [ ] **Step 3: Write the archive sequentially from staged files**

After the worker pool completes, stream each successful image file once into the ZIP and render chapter XHTML in original chapter/image order. Continue rendering text when an image fails and report failed image count in the existing progress object.

- [ ] **Step 4: Remove the ViewModel's duplicate image temporary files**

Replace the `NativeEpubAsset(input = temporary.inputStream())` bridge with a callback that writes each OkHttp stream straight into the writer-owned destination. Preserve MediaStore pending/commit/cleanup semantics and never open a WebView for EPUB download.

- [ ] **Step 5: Run focused writer/API tests and confirm green**

Run:

```powershell
.\gradlew.bat :app:testReleaseUnitTest --tests com.novalpie.nativeapp.data.NativeEpubArchiveWriterTest --tests com.novalpie.nativeapp.data.NovalPieApiTest --offline --no-daemon --max-workers=1 --console=plain
```

Expected: original bytes, deduplication, failed-image text retention, and bounded concurrent loading all pass.

### Task 5: Full verification and MuMu handoff build

**Files:**
- Modify: `docs/EPUB_DOWNLOAD_AUDIT.md`
- Modify: `agent-bridge/bridge-state.md`
- Modify: `agent-bridge/progress-log.md`
- Artifact: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Document the observed root causes and new constraints**

Record the source Markdown editor/renderer contract, the former limited native parser, the former serial double-staging EPUB path, the new four-worker bound, and the no-recompression/no-WebView constraints.

- [ ] **Step 2: Run the complete Android gate**

Run:

```powershell
.\gradlew.bat :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --offline --no-daemon --max-workers=1 --console=plain
```

Expected: zero test failures/errors/skips, `No issues found`, and a debug APK.

- [ ] **Step 3: Install without clearing MuMu state when it reconnects**

Use the existing MuMu serial and `adb install -r`; do not clear app data, login, proxy, cookies, or downloads. Capture: a forum comment editor with all toolbar actions, a live folded/hidden/link-rich post, a book-review comment, and EPUB progress showing multiple image downloads.

- [ ] **Step 4: Hand the APK to the user for acceptance**

Report the APK path, SHA-256, tests, lint, screenshots, and the known behavior. Do not alter the existing GitHub Release until the user explicitly confirms the build works.
