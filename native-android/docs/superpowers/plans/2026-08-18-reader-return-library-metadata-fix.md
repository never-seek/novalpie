# Reader Return and Library Metadata Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve the expected previous-chapter page position and make Collection resume/card metadata immediately readable and aligned.

**Architecture:** Reader boundary navigation carries an explicit one-shot entry position through `AppRoute.Reader` and `ReaderState`, leaving ordinary catalog and deep-link opens at the top. The local reading-progress store keeps a resolved book title, while Collection can still resolve a live title from the loaded shelf for legacy entries. Compact shelf cards use fixed text slots rather than content-dependent vertical positions.

**Tech Stack:** Kotlin, Jetpack Compose, ViewModel state, SharedPreferences, JUnit/Robolectric.

---

### Task 1: Return from chapter B to the end of chapter A

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderAdjacentChapterTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [x] Add a failing pure-state test showing that a previous chapter boundary resolves to `End`, whereas normal/next opens resolve to `Start`.
- [x] Run `./gradlew.bat :app:testReleaseUnitTest --tests com.novalpie.nativeapp.ui.ReaderAdjacentChapterTest --offline --no-daemon --max-workers=1 --console=plain` and confirm the new contract is absent.
- [x] Add `ReaderChapterEntryPosition`, propagate it only for page-boundary navigation, and consume it after the new body has composed by scrolling to the final list item.
- [x] Re-run the focused reader test and confirm it passes.

### Task 2: Give continue-reading a book title

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/data/ReaderProgressStoreTest.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/LibraryPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/model/Models.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/ReaderProgressStore.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/LibraryPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/CollectionPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [x] Add failing tests for persisted book titles and the two-line resume identity (`book title`, then `chapter title`).
- [x] Run the focused tests and confirm the missing title contract fails.
- [x] Persist a sanitized title without breaking legacy progress records; resolve it from the current Book Detail/shelf when a new progress record is saved.
- [x] Make the Collection resume row show book identity first and chapter identity second, with a non-empty fallback for old records.
- [x] Re-run the focused tests and confirm they pass.

### Task 3: Align compact Collection card metadata

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/LibraryPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/CompactLibraryPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [x] Add a failing layout-contract test for fixed two-line title, one-line author, and reserved progress footer slots.
- [x] Run the focused test and confirm the layout contract does not exist.
- [x] Use the shared slot contract in `CompactLibraryBookCardItem`; the progress row remains allocated for Collection only, so upload-management cards remain compact.
- [x] Re-run the focused test and confirm it passes.

### Task 4: Verify and capture runtime evidence

**Files:**
- Modify: `docs/APP2_COMPLETION_AUDIT.md`
- Modify: `D:/NovalPie/agent-bridge/bridge-state.md`
- Modify: `D:/NovalPie/agent-bridge/progress-log.md`

- [x] Run the complete release unit test, lint, and debug APK assembly gate.
- [x] Install only with `adb install -r`, preserving the user session and app data.
- [x] In MuMu, verify B -> back-to-end-of-A, the Collection resume row title, and mixed-length card row alignment; save screenshots under `D:/NovalPie/agent-bridge/screenshots`.
