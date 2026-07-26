# Native Forum Reader UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the current native Compose UI from a test-panel style shell into a novel-forum client with Forum, Library, Discover, Profile, book detail, and reader presentation aligned to the design spec.

**Architecture:** Keep the existing Kotlin + Jetpack Compose project and API/ViewModel foundation. Introduce small pure UI contract helpers for testable labels and presentation decisions, then update `NovalPieApp.kt` in focused passes so navigation, default screen, library, discover, profile, book detail, and reader remain verifiable.

**Tech Stack:** Kotlin, Jetpack Compose Material3, JUnit/Robolectric, existing OkHttp API client, existing MuMu verifier.

---

### Task 1: Navigation Contract and Default Forum Entry

**Files:**
- Create: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\UiNavigation.kt`
- Create: `D:\NovalPie\native-android\app\src\test\java\com\novalpie\nativeapp\ui\UiNavigationTest.kt`
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieViewModel.kt`
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt`

- [ ] Write failing tests for four bottom tabs: Forum, Library, Discover, Profile.
- [ ] Run `.\gradlew.bat :app:testReleaseUnitTest --tests com.novalpie.nativeapp.ui.UiNavigationTest --console=plain` and confirm failure because helpers/tabs are missing.
- [ ] Add `UiNavigation.kt` helper functions and update `BottomTab`.
- [ ] Add `AppRoute.Forum`, make it the default route, and route the Forum tab to it.
- [ ] Add a native `ForumScreen` carrying `NOVALPIE_NATIVE_COMPOSE_HOME`.
- [ ] Rerun the focused test and full unit suite.

### Task 2: Clean Presentation Text and Reader Source Removal

**Files:**
- Create: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\ReaderPresentation.kt`
- Create: `D:\NovalPie\native-android\app\src\test\java\com\novalpie\nativeapp\ui\ReaderPresentationTest.kt`
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt`

- [ ] Write failing test proving reader metadata should not render a visible `source:` line.
- [ ] Run focused test and confirm failure.
- [ ] Add reader presentation helper and update `ReaderBody` to hide debug source text.
- [ ] Replace visible mojibake labels touched by navigation, forum, library, discover, profile, and reader chrome.
- [ ] Rerun focused and full unit tests.

### Task 3: Library and Discover Visual Restructure

**Files:**
- Modify: `D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt`
- Modify: `D:\NovalPie\native-android\tools\verify-native-project.ps1`

- [ ] Keep existing bookshelf/search API behavior.
- [ ] Rename user-facing Home copy to Library copy and remove explanatory hero wording.
- [ ] Present search as Discover with history/filter/result sections.
- [ ] Update structural verifier from old Bookshelf/Search/Settings-only copy to Forum/Library/Discover/Profile contract.
- [ ] Rerun structural verifier.

### Task 4: Build, Runtime Proof, and Documentation

**Files:**
- Modify: `D:\NovalPie\native-android\README.md`
- Modify: `D:\NovalPie\NOVALPIE_WORKSPACE_MASTER_README.md`

- [ ] Run `.\gradlew.bat :app:testReleaseUnitTest --console=plain`.
- [ ] Run `powershell -ExecutionPolicy Bypass -File D:\NovalPie\native-android\tools\verify-native-project.ps1 -RequireApk`.
- [ ] Run `powershell -ExecutionPolicy Bypass -File D:\NovalPie\native-android\tools\build-release.ps1`.
- [ ] Run `powershell -ExecutionPolicy Bypass -File D:\NovalPie\native-android\tools\verify-mumu-compose-launch.ps1`.
- [ ] Record APK hash and evidence directory in both READMEs.

## Self-review

- Spec coverage: navigation, forum default, library, discover, book detail, reader, non-goals, and verification gates are covered.
- Placeholder scan: no TBD/TODO placeholders remain.
- Scope: first implementation pass focuses on native UI shell and reader presentation, preserving API behavior and Web fallback.
