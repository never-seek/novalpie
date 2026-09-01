# Reader Rule Download And Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make public replacement behavior controllable per device and per book, optionally apply the effective reader rules to native EPUB/TXT exports, and close the actionable defects reported in the latest App 2.0 feedback poll replies.

**Architecture:** Keep source chapter data immutable. Resolve an effective replacement snapshot from the book’s personal/public mode and per-rule visibility, then pass that snapshot into only the export transform path when the user chooses it. Persist a stable reader viewport anchor separately from chapter progress, and extend the existing rich-text converter without changing source content.

**Tech Stack:** Kotlin, Jetpack Compose, SharedPreferences, NativeEpubArchiveWriter, Android MediaStore, JUnit/Robolectric, ADB/MuMu QA.

---

### Task 1: Public Rule Policy

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/ReaderReplacementRulesStore.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderReplacementPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderReplacementControls.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderReplacementPresentationTest.kt`

- [x] Add failing tests for a device default and a per-book public-rule switch, proving that an off switch removes every shared rule while retaining personal rules.
- [x] Persist the device default and a per-book override without conflating “not configured” with “explicitly off”.
- [x] Add a compact `本书应用公共规则` switch and keep individual shared-rule hides active beneath it.
- [x] Re-run the focused tests until green.

### Task 2: Export Replacement Choice

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/NativeEpubArchiveWriter.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/BookDetailPresentation.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/data/NativeEpubArchiveWriterTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderReplacementPresentationTest.kt`

- [x] Write failing EPUB/TXT transformation tests showing that selected effective rules change titles and body text while the default export remains source text.
- [x] Add an explicit download-sheet choice: `原文下载` or `应用当前替换规则`.
- [x] Capture the effective rule snapshot at download start; never let a later rule edit mutate an in-flight export.
- [x] Stream transformed TXT in bounded memory and apply the same transformation before EPUB chapters are rendered; leave image URLs, byte payloads, and original cache untouched.
- [x] Re-run focused tests until green.

### Task 3: Poll Feedback Defects

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/model/Models.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/ReaderProgressStore.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderText.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/data/ReaderProgressStoreTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderTextTest.kt`

- [x] Write failing tests for a persisted viewport anchor, reader reopen restoration, HTML/Markdown italic, underline, and strikethrough.
- [x] Persist a chapter-relative reader anchor and restore it only after body composition; preserve existing chapter-level website progress synchronization.
- [x] Map HTML `em/i/u/s/strike/del` and Markdown emphasis/strikethrough to overlapping Compose styles without losing bold spans.
- [x] Align content-width control categorization with its settings overview, and move low-frequency book-management actions behind the existing detail menu after proving the current accidental-tap path.
- [x] Re-run focused tests until green.

### Task 4: Verification

**Files:**
- Modify: `docs/APP2_REGRESSION_AUDIT_LEDGER.md`
- Modify: `D:/NovalPie/agent-bridge/bridge-state.md`
- Modify: `D:/NovalPie/agent-bridge/progress-log.md`

- [x] Run `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- [x] Install the resulting Debug APK with `adb install -r`, preserve user data, and confirm the device APK hash matches local.
- [x] On MuMu, verify public-rule off/on plus per-rule hide; generate one temporary EPUB and one temporary TXT with replacement enabled, inspect content, then delete only those two precisely named QA files.
- [x] Verify reader position restore, italic/underline/strike rendering, and no App FATAL/ANR/OOM.
