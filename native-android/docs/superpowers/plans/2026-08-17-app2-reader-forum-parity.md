# App 2.0 Reader and Forum Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the native Android client match the live NovalPie mobile information hierarchy while preserving every observed API-backed feature, starting with phone-first forum reviews and the source-style reader.

**Architecture:** Keep Kotlin + Jetpack Compose and the existing signed reader/session API unchanged. Put responsive decisions and action vocabulary in pure presentation helpers, keep Composables responsible for layout only, and verify each behavior with unit tests before emulator interaction.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Coil, existing NovalPieApi/ViewModel, JUnit, Playwright source captures, MuMu ADB QA.

---

### Task 1: Record live source contracts

**Files:**
- Read: `D:\NovalPie\native-android\docs\READER_REFERENCE_GUIDE.md`
- Read: `D:\NovalPie\native-android\docs\LIVE_SITE_ROUTE_API_MATRIX.md`
- Evidence: `D:\NovalPie\native-android\output\playwright\source-reader-mobile-turn172.png`

- [x] Confirm the 412dp source reader uses a narrow article rail, right-side action rail, chapter drawer, chapter comments, original-resolution illustrations, and source labels `帮助 / 目录 / 设置 / 主题 / 上章 / 下章 / 滑动 / 听书 / 全屏 / 导航`.
- [x] Confirm the 412dp source book-review feed is one column; reserve two columns only for tablet width.

### Task 2: Make the forum review grid responsive

**Files:**
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\ProductCopy.kt`
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt`
- Test: `D:\NovalPie\native-android\app\src\test\java\com\novalpie\nativeapp\ui\ForumPresentationTest.kt`

- [x] Prove the old unconditional two-column behavior fails for the phone contract.
- [x] Implement `forumGridColumnCount(type, availableWidthDp)` with one review column below `720dp` and two at or above it; pass `LocalConfiguration.current.screenWidthDp` at the route boundary.
- [x] Verify the focused test passes without changing forum request parameters or card data.

### Task 3: Stabilize Reader action vocabulary and responsive chrome

**Files:**
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\ReaderPresentation.kt`
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt`
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\ReaderSettingsPresentation.kt`
- Test: `D:\NovalPie\native-android\app\src\test\java\com\novalpie\nativeapp\ui\ReaderPresentationTest.kt`

- [ ] Add a pure `ReaderRailActionSpec` list with stable action IDs, source order, accessibility labels, and enabled-state rules; render icons and labels from that single list so icon/content mismatches cannot recur.
- [ ] Keep the source rail at `64dp` on phones and cap the panel at `440dp`; use a scrollable rail when the safe height cannot fit all actions instead of shrinking hit targets with unconstrained weights.
- [ ] Preserve the center-tap/drag distinction, long-press-only illustration preview, infinite-scroll sentinel, comments, TTS, favorite, fullscreen, and Back behavior.
- [ ] Add tests for action order, disabled first/last chapter controls, phone/tablet rail geometry, and drag not toggling chrome.

### Task 4: Verify source parity on the four high-traffic surfaces

**Files:**
- Read/compare: `D:\NovalPie\native-android\output\playwright\source-search-mobile-turn172.png`
- Read/compare: `D:\NovalPie\native-android\output\playwright\source-forum-review-mobile-turn172.png`
- Read/compare: `D:\NovalPie\native-android\output\playwright\source-book-detail-mobile-turn172.png`
- Read/compare: `D:\NovalPie\native-android\output\playwright\source-reader-mobile-turn172.png`
- Evidence: `D:\NovalPie\agent-bridge\screenshots\`

- [ ] Install the debug APK with `adb install -r` only after a MuMu serial reports `device`.
- [ ] Drive Search -> book detail -> Reader -> Back -> Forum review using UI-tree-derived coordinates.
- [ ] Capture portrait screenshots and logcat; verify no stale detail route, blank review list, duplicate toolbar action, or orientation flip.

### Task 5: Full regression gate and audit update

- [ ] Run `.\gradlew.bat :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain` from `D:\NovalPie\native-android`.
- [ ] Run `git diff --check` from the same repository.
- [ ] Append source evidence, changed files, test counts, APK hash, and any emulator blocker to `D:\NovalPie\native-android\docs\APP2_COMPLETION_AUDIT.md` and `D:\NovalPie\agent-bridge\progress-log.md`.

## Self-review

- Scope is split into independently testable forum, reader, runtime, and audit gates.
- The signed reader session protocol, JSON normalizers, admin authorization, and download behavior are explicitly preserved.
- No task removes source data, adult-content filters, comments, reactions, rewards, cover metadata, or Web fallback routes.
