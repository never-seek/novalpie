# Reader Feedback Four Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 娣囶喖顦查梼鍛邦嚢閸ｃ劍娓堕弬鏉垮冀妫ｅ牅鑵戦惃鍕磽娑擃亪妫舵０姗堢窗娑撳顣介悩鑸碘偓浣圭埉鐠虹喖娈㈤梼鍛邦嚢娑撳顣介妴渚€銆婇柈銊︽▔缁€杞板姛閸氬秲鈧胶鐐曟い鍨佸蹇撳絿濞戝牊鏆ｆい闈涚€惄瀛樼泊閸斻劌濮╅悽璇茶嫙閺€顖涘瘮闂婃娊鍣洪柨顔剧倳妞ょ偣鈧胶娲拌ぐ鏇熷ⅵ瀵偓閺冭泛鐣炬担宥呯秼閸撳秶鐝烽懞鍌樷偓?
**Architecture:** 娣囨繃瀵旈悳鐗堟箒 Compose reader 閸?ViewModel 鐠侯垳鏁辩紒鎾寸€敍灞肩瑝瀵洖鍙?WebView 閹存牗鏌婇惃鍕鐠囪娅掔€圭懓娅掗妴淇乪aderState 閹镐焦婀侀崣顖滅彌閸楄櫕妯夌粈铏规畱娑旓箑鎮曢敍娑氭窗瑜版洑濞囬悽銊у缁?LazyListState 閸滃瞼鍑界槐銏犵穿鐟欙絾鐎介敍娑氱倳妞ら潧褰ч幎?LazyColumn 閻ㄥ嫪缍呯粔缁樻暭娑撹櫣鐝涢崡?scrollBy閿涘奔绻氶悾娆忓嚒閺堝浜ら柌蹇氼潒鐟欏鏅ラ弸婊愮幢MainActivity 闁俺绻冮惌顓犳晸閸涜棄鎳嗛張鐔锋礀鐠嬪啯濡搁悧鈺冩倞闂婃娊鍣洪柨顔挎祮缂佹瑥缍嬮崜?reader閿涘矂娼紙濠氥€夊Ο鈥崇础鐎瑰苯鍙忔禍銈呮礀缁崵绮洪妴鍌滃Ц閹焦鐖懗灞炬珯閻?reader route 閻?palette 閸氬本顒為崚?Scaffold/window閿涘苯娴橀弽鍥ㄦ閺嗘鏁辩痪顖氬毐閺佹澘鍠呯€规哎鈧?
**Tech Stack:** Kotlin, Jetpack Compose Material3, Android Activity dispatchKeyEvent, JUnit 4, Gradle Android unit tests, MuMu ADB QA.

---

### Task 1: Establish the regression contract

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`
- Create: `app/src/test/java/com/novalpie/nativeapp/ReaderVolumeKeyTest.kt`

- [x] **Step 1: Record the observed roots and baseline failure**

The existing implementation repeats `readerChapterProgressLabel` in `ReaderTopBar`, calls `LazyListState.animateScrollBy`, creates the catalog `LazyColumn` without a state, and has no volume-key dispatch path. The first baseline command was blocked only because the isolated worktree lacked `local.properties`; the project SDK path is now configured.

- [x] **Step 2: Write failing tests for the four contracts**

Add focused tests that assert:

```kotlin
assertEquals("缁€杞扮伐娑旓箑鎮?, readerTopBarTitle("缁€杞扮伐娑旓箑鎮?))
assertEquals("闂冨懓顕?, readerTopBarTitle(null))
assertEquals(2, readerCatalogCurrentChapterIndex(chapters, currentChapterId = 30L))
assertEquals(ReaderPageScrollMotion.Immediate, readerPageScrollMotion())
assertEquals(ReaderVolumeKeyAction.PreviousPage, readerVolumeKeyAction(...VOLUME_UP...))
assertEquals(ReaderVolumeKeyAction.NextPage, readerVolumeKeyAction(...VOLUME_DOWN...))
assertEquals(ReaderVolumeKeyAction.Ignore, readerVolumeKeyAction(..., pageTurnEnabled = false))
assertTrue(readerSystemBarUsesDarkIcons(Color.White))
assertFalse(readerSystemBarUsesDarkIcons(Color.Black))
```

Use real `Chapter` values and Android `KeyEvent` constants. Keep each test one behavior.

- [x] **Step 3: Run the focused tests and verify RED**

Run:

```
.\gradlew.bat :app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.ReaderPresentationTest --tests com.novalpie.nativeapp.ReaderVolumeKeyTest --offline --no-daemon --max-workers=1 --console=plain
```

Expected result: compilation/test failure because the new production contracts do not exist yet. Do not change production code until this RED result is recorded.

### Task 2: Add reader presentation contracts and state

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`

- [x] **Step 1: Add pure helpers**

Add `readerTopBarTitle(bookTitle: String?)`, `readerCatalogCurrentChapterIndex`, `ReaderPageScrollMotion.Immediate` plus `readerPageScrollMotion()`, `ReaderVolumeKeyAction` plus `readerVolumeKeyAction`, and `readerSystemBarUsesDarkIcons`. Blank titles fall back to the existing localized `闂冨懓顕癭; catalog indices return null when absent; volume repeats are consumed without generating another page turn.

- [x] **Step 2: Add `bookTitle` to `ReaderState` and seed it**

Add nullable `bookTitle`. When `loadReader` creates state, retain a matching existing title or resolve one from loaded Book Detail/favorites/history/search results. If it remains unknown, asynchronously request `api.bookDetail(bookId)` under the existing reader request freshness guard and update only the title.

- [x] **Step 3: Run focused tests**

Run the same focused command. Expected: all new helper/state tests pass while existing reader tests remain green.

### Task 3: Fix reader UI behavior

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [x] **Step 1: Use the book title in the top rail**

Change `ReaderTopBar` to call `readerTopBarTitle(state.bookTitle)`; keep chapter progress in the bottom status rail only.

- [x] **Step 2: Make catalog opening locate the current chapter**

Remember a `LazyListState` in `ReaderCatalogPanel`; when the unfiltered catalog is available, resolve the current chapter index and `scrollToItem` it. A nonblank search query must never be overwritten by this effect.

- [x] **Step 3: Remove vertical scroll animation from page turns**

Replace `listState.animateScrollBy(distance, tween(duration))` with immediate `listState.scrollBy(distance)`. Keep the existing optional alpha/offset visual treatment and boundary transition behavior.

- [x] **Step 4: Run focused tests and compile**

Run focused reader tests and `.\gradlew.bat :app:compileDebugKotlin --offline --no-daemon --max-workers=1 --console=plain`.

### Task 4: Add physical volume-key page turns

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/MainActivity.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`

- [x] **Step 1: Register a reader-only callback**

MainActivity dispatches volume key down/up events through `readerVolumeKeyAction`, invokes the registered direction callback only once per physical press, consumes repeats/up events while page mode is active, and returns to normal Android volume behavior outside page mode.

- [x] **Step 2: Bind/unbind from ReaderScreen**

Register the callback only while `pageTurnEnabled` is true and clear it on reader disposal/route change. Map volume up to previous page and volume down to next page.

- [x] **Step 3: Run volume unit tests and compile**

Run `ReaderVolumeKeyTest`, `ReaderPresentationTest`, and `compileDebugKotlin`.

### Task 5: Synchronize the reader palette with system bars

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieTheme.kt` only if the route-level synchronization requires a shared reset

- [x] **Step 1: Paint the route background behind safe insets**

Set the Scaffold/container surface to the current reader palette background while the reader route is active, so the transparent status-bar region cannot expose the app's gray background.

- [x] **Step 2: Update system-bar colors and icon contrast**

On every reader palette change, set status/navigation bar colors and icon appearance from the active palette; restore the normal app theme configuration when leaving the reader. Use the pure luminance helper for custom, sepia, dark, and high-contrast themes.

- [x] **Step 3: Run all unit tests and static checks**

Run:

```
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain
git diff --check
```

### Task 6: MuMu runtime regression and handoff

**Files:**
- Create: `docs/superpowers/plans/2026-08-22-reader-feedback-four-fixes.md` (this plan)
- Update: `D:\NovalPie\agent-bridge\bridge-state.md`
- Update: `D:\NovalPie\agent-bridge\progress-log.md`
- Create/update: `D:\NovalPie\agent-bridge\screenshots\20260822-reader-feedback-*.png`
- Create/update: `D:\NovalPie\agent-bridge\artifacts\20260822-reader-feedback-*.txt`

- [x] **Step 1: Install the verified debug APK without clearing MuMu data**

Use the existing serial `127.0.0.1:16384`, preserve login/cookies/progress, and reapply `adb reverse tcp:7890 tcp:7890` if needed.

- [x] **Step 2: Exercise each reported flow**

Verify a light/custom reader theme paints the status bar, top rail shows the book title, page mode turns immediately with tap and volume up/down, previous/next chapter boundaries work, and opening an unfiltered catalog scrolls to the active chapter while a filtered catalog preserves the query position.

- [x] **Step 3: Capture evidence and inspect logcat**

Save screenshots and UI/logcat artifacts under the bridge directory. Confirm no app FATAL, ANR, or OOM; distinguish any MuMu `uiautomator` helper crash from app process errors.

- [x] **Step 4: Update bridge state**

Record changed files, fresh test/build results, APK hash, screenshots, known limitations (landscape/double-page intentionally excluded), and keep owner as Codex. Do not change GitHub Release in this turn.
