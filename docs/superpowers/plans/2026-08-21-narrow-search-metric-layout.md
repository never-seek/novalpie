# Narrow Search Metric Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep every search-card metric visible on narrow Android phones while preserving equal card bottoms within a grid row.

**Architecture:** Search cards already calculate a shared tag height per row before Compose measures the cells. Add the same pure presentation calculation for compact metrics, then render metrics with `FlowRow` instead of a width-constrained `Row`. The grid passes each row's maximum metric-line count to its cards, which reserve the same minimum metric area without clipping an individual card.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, Gradle Android unit tests.

---

### Task 1: Reproduce the 360dp regression

**Files:**
- Modify: `app/src/test/java/com/novalpie/nativeapp/ui/SearchGridPresentationTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
@Test
fun narrowTwoColumnPhoneWrapsAllThreeMetricsInsteadOfClippingWordCount() {
    val book = NovelCard(
        id = 4,
        title = "Narrow metrics",
        favoriteCount = 2_400,
        siteReadCount = 296_000,
        wordCount = 1_221_000,
    )
    val metricWidth = searchGridTagContentWidthDp(availableWidthDp = 360, columnCount = 2)

    assertEquals(142, metricWidth)
    assertEquals(2, searchGridMetricLineCount(book, availableMetricWidthDp = metricWidth))
    assertEquals(40, searchGridMetricAreaMinHeightDp(lineCount = 2))
}
```

- [x] **Step 2: Run the focused test to verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.novalpie.nativeapp.ui.SearchGridPresentationTest" --offline --no-daemon --max-workers=1 --console=plain`

Expected: compilation fails because the metric-line calculation is absent.

### Task 2: Model metric wrapping before composition

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/SearchGridPresentation.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/SearchGridPresentationTest.kt`

- [x] **Step 1: Add metric constants and pure row calculations**

```kotlin
internal fun searchGridMetricLineCount(
    book: NovelCard,
    availableMetricWidthDp: Int,
    fontScale: Float = 1f,
): Int

internal fun searchGridRowMetricLineCount(
    books: List<NovelCard>,
    availableMetricWidthDp: Int,
    fontScale: Float = 1f,
): Int

internal fun searchGridMetricAreaMinHeightDp(lineCount: Int): Int
```

The width estimate must include the 13dp icon, icon/text gap, item gap, rendered compact value, and a small rounding allowance. It must use the same compact values produced by `novelCardCompactMetrics`.

- [x] **Step 2: Run the focused test to verify it passes**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.novalpie.nativeapp.ui.SearchGridPresentationTest" --offline --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL` and the narrow-phone test passes.

### Task 3: Render and reserve wrapped metric rows

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/SearchGridPresentationTest.kt`

- [x] **Step 1: Pass the shared metric-line count from each search row**

```kotlin
val rowMetricLineCount = searchGridRowMetricLineCount(
    books = rowBooks,
    availableMetricWidthDp = gridMetricContentWidthDp,
    fontScale = gridFontScale,
)
```

- [x] **Step 2: Replace the fixed 20dp single-row metric container**

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = metricAreaMinHeight),
) {
    NovelCompactMetricRow(compactMetrics)
}
```

`NovelCompactMetricRow` must use `FlowRow` so an item moves intact to the next line instead of compressing its value.

- [x] **Step 3: Verify on an emulated 360dp viewport**

Run: install the debug APK, temporarily override the `900x1600` MuMu device to `400dpi` (an effective 360dp width), open Search, and capture the result. Restore its original `240dpi` configuration afterwards.

### Task 4: Run the complete project gate

**Files:**
- Verify: affected source, unit tests, lint, APK

- [x] **Step 1: Execute full verification**

Run: `./gradlew.bat :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain`

Expected: all unit tests, lint, and debug assembly succeed.
