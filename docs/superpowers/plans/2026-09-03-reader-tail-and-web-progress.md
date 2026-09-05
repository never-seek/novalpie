# Reader Tail Pagination and Web Progress Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove duplicate short terminal pages in the native paged reader and reconcile strictly newer website chapter progress into App resume state.

**Architecture:** Page mode gains a terminal LazyColumn spacer that creates physical room for the final semantic reader item; the page planner turns an attempted spacer navigation into adjacent-chapter navigation. Favorites/history loading applies a pure, strictly-forward merge from source `FavoriteEntry` chapter metadata to local `ReaderProgress`, preserving same-chapter viewport anchors and clearing completion notices only after remote completion.

**Tech Stack:** Kotlin, Jetpack Compose LazyColumn, Android SharedPreferences reader store, JUnit 4, Gradle Android plugin, ADB/MuMu.

---

### Task 1: Lock terminal-spacer page planning with a red test

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun pageTurnTreatsTheTerminalSpacerAsTheNextChapterInsteadOfABlankPage() {
    assertEquals(
        ReaderPageTurnPlan.OpenAdjacentChapter(ReaderPageBoundaryTarget.NextChapter),
        readerPageTurnPlan(
            direction = 1,
            pageTargetIndex = 18,
            reachedBoundaryBeforeTurn = false,
            hasPrevious = true,
            hasNext = true,
            terminalSpacerIndex = 18,
        ),
    )
}
```

- [ ] **Step 2: Run it to verify RED**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\NovalPie\native-android\.gradle-user'
.\gradlew.bat :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.ReaderPresentationTest.pageTurnTreatsTheTerminalSpacerAsTheNextChapterInsteadOfABlankPage --offline --no-daemon --max-workers=1 '-Pkotlin.incremental=false' --console=plain
```

Expected: compile/test failure because `terminalSpacerIndex` is not yet a supported planner input.

- [ ] **Step 3: Implement the minimal planner behavior**

Extend `readerPageTurnPlan` with an optional `terminalSpacerIndex: Int? = null`; before returning `ScrollWithinChapter`, return `OpenAdjacentChapter(NextChapter)` only when direction is forward, the target index equals the terminal spacer and a next chapter exists.

- [ ] **Step 4: Run it to verify GREEN**

Run the command from Step 2. Expected: `BUILD SUCCESSFUL`.

### Task 2: Add the page-mode terminal spacer to ReaderScreen

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun pageModeAddsOneTerminalSpacerAfterReaderBodyButContinuousModeDoesNot() {
    assertTrue(readerPageTerminalSpacerRequired(pageTurnEnabled = true, hasReadableBody = true))
    assertFalse(readerPageTerminalSpacerRequired(pageTurnEnabled = false, hasReadableBody = true))
    assertFalse(readerPageTerminalSpacerRequired(pageTurnEnabled = true, hasReadableBody = false))
}
```

- [ ] **Step 2: Run it to verify RED**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\NovalPie\native-android\.gradle-user'
.\gradlew.bat :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.ReaderPresentationTest.pageModeAddsOneTerminalSpacerAfterReaderBodyButContinuousModeDoesNot --offline --no-daemon --max-workers=1 '-Pkotlin.incremental=false' --console=plain
```

Expected: compile/test failure because the terminal spacer requirement helper does not yet exist.

- [ ] **Step 3: Implement the minimal reader layout change**

Add `readerPageTerminalSpacerRequired` in `ReaderPresentation.kt`. In the `LazyColumn` content branch for successful reader content, append a keyed spacer only when the helper returns true. Its height equals the live `viewportEndOffset - viewportStartOffset`, converted through `LocalDensity`, with a safe positive fallback. Pass the last item index as `terminalSpacerIndex` to `readerPageTurnPlan` only while that spacer exists.

- [ ] **Step 4: Run focused reader tests**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\NovalPie\native-android\.gradle-user'
.\gradlew.bat :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.ReaderPresentationTest --offline --no-daemon --max-workers=1 '-Pkotlin.incremental=false' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

### Task 3: Lock website-ahead ReaderProgress merge semantics with red tests

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/CompactLibraryPresentationTest.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/CompactLibraryPresentation.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun remoteFavoriteProgressAheadOfLocalBecomesTheResumeChapterAndDropsViewportAnchor() {
    val merged = readerProgressAfterRemoteFavoriteProgress(
        localProgress = ReaderProgress(7, 70, chapterNumber = 7, chapterCountAtLastRead = 7, viewportItemIndex = 3, viewportItemScrollOffsetPx = 48),
        entry = FavoriteEntry(book = NovelCard(id = 7, title = "Book"), lastChapterId = 100, lastChapter = 10, chapterCount = 10),
    )
    assertEquals(100L, merged?.chapterId)
    assertEquals(10, merged?.chapterNumber)
    assertEquals(10, merged?.chapterCountAtLastRead)
    assertNull(merged?.viewportItemIndex)
}

@Test
fun equalOrOlderRemoteFavoriteProgressCannotOverwriteLocalAnchor() {
    val local = ReaderProgress(7, 100, chapterNumber = 10, viewportItemIndex = 3, viewportItemScrollOffsetPx = 48)
    assertEquals(local, readerProgressAfterRemoteFavoriteProgress(local, FavoriteEntry(book = NovelCard(id = 7, title = "Book"), lastChapterId = 99, lastChapter = 10, chapterCount = 10)))
}
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\NovalPie\native-android\.gradle-user'
.\gradlew.bat :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.CompactLibraryPresentationTest.remoteFavoriteProgressAheadOfLocalBecomesTheResumeChapterAndDropsViewportAnchor --tests com.novalpie.nativeapp.ui.CompactLibraryPresentationTest.equalOrOlderRemoteFavoriteProgressCannotOverwriteLocalAnchor --offline --no-daemon --max-workers=1 '-Pkotlin.incremental=false' --console=plain
```

Expected: compile/test failure because the merge helper does not yet exist.

- [ ] **Step 3: Implement the pure merge helper**

Add `readerProgressAfterRemoteFavoriteProgress(localProgress, entry)` to `CompactLibraryPresentation.kt`. Require positive remote ID/number, adopt only when local is absent or remote number is greater, preserve the previous catalogue baseline for partial remote reads, set the baseline to the current total only when remote reaches it, and clear local viewport fields on any adopted remote chapter.

- [ ] **Step 4: Run focused progress tests**

Run the command from Step 2. Expected: `BUILD SUCCESSFUL`.

### Task 4: Persist remote-ahead favorite/history progress after accepted home responses

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/CompactLibraryPresentationTest.kt`

- [ ] **Step 1: Add a failing merge integration assertion**

Add a test for a partial remote chapter: remote chapter 8 of current total 10 adopts chapter 8 but retains a prior completed-catalogue baseline of 7. This proves that the update marker is only cleared when remote reaches current total.

- [ ] **Step 2: Run it to verify RED**

Run the focused progress test command from Task 3. Expected: assertion failure until the helper preserves the baseline correctly.

- [ ] **Step 3: Implement persistence in ViewModel**

Create a private `reconcileRemoteReaderProgress(entries)` that runs only after a fresh page result is accepted. For each entry, calculate the pure merge; when it differs, save it through `ReaderProgressStore.save`, refresh `readerProgress` and `recentReaderProgresses`, then call `updateLoadedCollectionProgress`. Invoke it for favorites and history result pages before publishing the entries.

- [ ] **Step 4: Run relevant tests**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\NovalPie\native-android\.gradle-user'
.\gradlew.bat :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.CompactLibraryPresentationTest --tests com.novalpie.nativeapp.ui.ReaderPresentationTest --offline --no-daemon --max-workers=1 '-Pkotlin.incremental=false' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

### Task 5: Build and device verification

**Files:**
- Evidence: `D:\NovalPie\agent-bridge\screenshots\`
- Evidence: `D:\NovalPie\agent-bridge\artifacts\`

- [ ] **Step 1: Run the full Debug gate**

```powershell
$env:GRADLE_USER_HOME='D:\NovalPie\native-android\.gradle-user'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon --max-workers=1 '-Pkotlin.incremental=false' --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install and verify hash on MuMu**

```powershell
$adb='C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$apk='D:\NovalPie\native-android\app\build\outputs\apk\debug\app-debug.apk'
& $adb -s 127.0.0.1:16384 install -r $apk
& $adb -s 127.0.0.1:16384 shell sha256sum ((& $adb -s 127.0.0.1:16384 shell pm path com.novalpie.app.debug) -replace '^package:')
```

Expected: install success and device SHA-256 equals local SHA-256.

- [ ] **Step 3: Exercise the reader regression**

Open `novalpie://app/book/360661/11395509`, enable page mode, then turn through chapter 49. Verify: final content page does not repeat into the comments page; next turn opens chapter 50; Back returns to the comments page without truncation or action rail.

- [ ] **Step 4: Exercise progress merge safely**

Refresh the current collection/history and validate current source `lastChapter` against resulting card label and resume target without posting web progress. Capture UI tree/screenshots and record any source limitations.
