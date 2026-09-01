# Reader Replacement And TTS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a safe, user-controlled full-text replacement system to the native reader and make Android system TTS play continuously and correctly across reader state changes.

**Architecture:** Keep fetched chapter text immutable. Build a deterministic effective-text pipeline that merges shared and personal replacement rules, applies scoped rules to title/content, and supplies the result to reader rendering, search/highlighting and TTS. Refactor TTS around a session generation and rolling utterance queue so stale callbacks cannot update UI, sentence boundaries are packed into natural utterances, and cross-chapter playback has a real next-text handoff.

**Tech Stack:** Kotlin, Jetpack Compose, Android `TextToSpeech`, `UtteranceProgressListener`, OkHttp, SharedPreferences, JUnit/Robolectric.

---

## Product Decisions From Community Feedback

- Personal rules are the default effective source. Shared rules are visible in an "all rules" view but each must be explicitly enabled or hidden per book.
- A shared rule can be copied into a personal rule. A personal rule with the same source takes precedence.
- Rules support literal text and safe regex (`$1` replacement groups), enable state, stable order, title/content targets, and whole-book/current-chapter/chapter-range scopes.
- Replace results are derived display data. Source downloads and the original chapter cache remain unchanged; exported/copy text can opt into the effective rules later without mutating source data.
- Replacements apply before text conversion, whitespace formatting and TTS segmentation.
- TTS queues sentence groups without splitting at commas; it uses generation IDs to invalidate stale callbacks and never speaks a chapter-switch placeholder.

## File Structure

- Create: `app/src/main/java/com/novalpie/nativeapp/model/ReaderReplacementRule.kt`
  - Reader rule, scope, target, source type, shared rule and effective-rule models.
- Create: `app/src/main/java/com/novalpie/nativeapp/data/ReaderReplacementRulesStore.kt`
  - Local rule persistence, per-book shared-rule visibility and local rule revision.
- Create: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderReplacementPresentation.kt`
  - Deterministic merge, parsing, validation and effective text transformation.
- Create: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderTtsPlaybackPlan.kt`
  - Pure rolling-queue/session reducer used by the Android TTS controller.
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt`
  - Personal glossary CRUD and shared glossary retrieval.
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderTtsController.kt`
  - Sentence packing, `QUEUE_ADD` rolling queue, error cleanup and voice fallback.
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderSettingsPresentation.kt`
  - Replacement summary/settings entry.
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderAdvancedControls.kt`
  - Rule source picker, rule editor/list and TTS queue-aware labels.
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
  - Rule loading, selected-text prefill, effective chapter text, TTS integration and cache invalidation.
- Test: `app/src/test/java/com/novalpie/nativeapp/data/ReaderReplacementRulesStoreTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderReplacementPresentationTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderTtsPlaybackPlanTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderTextTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt`

## Task 1: Add Replacement Rule Types And Safe Transformation Tests

**Files:**
- Create: `app/src/main/java/com/novalpie/nativeapp/model/ReaderReplacementRule.kt`
- Create: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderReplacementPresentation.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderReplacementPresentationTest.kt`

- [ ] **Step 1: Write failing tests for precedence, scope, literal replacement and regex groups.**

```kotlin
@Test fun personalRuleOverridesSharedRuleWithSameSource() {
    val effective = effectiveReplacementRules(
        sharedRules = listOf(sharedRule("Alice", "Shared")),
        personalRules = listOf(personalRule("Alice", "Mine")),
        hiddenSharedRuleIds = emptySet(),
        chapterIndex = 3,
        target = ReaderReplacementTarget.Content,
    )
    assertEquals("Mine", applyReaderReplacementRules("Alice", effective))
}

@Test fun regexRuleUsesCaptureGroupsOnlyWhenEnabled() {
    val rule = personalRule("(A)(B)", "$2-$1", isRegex = true)
    assertEquals("B-A", applyReaderReplacementRules("AB", listOf(rule)))
}

@Test fun hiddenSharedRuleDoesNotApply() {
    val shared = sharedRule("广告", "")
    val effective = effectiveReplacementRules(listOf(shared), emptyList(), setOf(shared.id), 1, ReaderReplacementTarget.Content)
    assertEquals("广告", applyReaderReplacementRules("广告", effective))
}
@Test fun chapterScopedRuleDoesNotLeakOutsideItsRange() {
    val rule = personalRule("由男", "由乃", scope = ReaderReplacementScope.ChapterRange(2, 4))
    assertEquals("由乃", applyReaderReplacementRules("由男", listOf(rule), chapterIndex = 3))
    assertEquals("由男", applyReaderReplacementRules("由男", listOf(rule), chapterIndex = 5))
}
@Test fun literalReplacementTreatsDollarAsLiteralText() {
    assertEquals("$1", applyReaderReplacementRules("src", listOf(personalRule("src", "$1"))))
}
```

- [ ] **Step 2: Run the focused test class and verify it fails because the rule model/pipeline does not exist.**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.novalpie.nativeapp.ui.ReaderReplacementPresentationTest"
```

Expected: compilation failure for `ReaderReplacementRule` / `effectiveReplacementRules`.

- [ ] **Step 3: Implement immutable rule models and the transformation pipeline.**

```kotlin
enum class ReaderReplacementSource { Personal, Shared }
enum class ReaderReplacementTarget { Content, Title, Both }
sealed interface ReaderReplacementScope {
    data object WholeBook : ReaderReplacementScope
    data object CurrentChapter : ReaderReplacementScope
    data class ChapterRange(val start: Int, val endInclusive: Int) : ReaderReplacementScope
}

data class ReaderReplacementRule(
    val id: String,
    val source: String,
    val replacement: String,
    val isRegex: Boolean = false,
    val isEnabled: Boolean = true,
    val order: Int = 0,
    val target: ReaderReplacementTarget = ReaderReplacementTarget.Content,
    val scope: ReaderReplacementScope = ReaderReplacementScope.WholeBook,
    val owner: ReaderReplacementSource = ReaderReplacementSource.Personal,
    val sharedRuleId: String? = null,
)
```

Implement rules in this exact order: enabled shared rules not hidden, then enabled personal rules; within each group sort by `order`, then `id`. Remove shared rules whose `source` is overridden by a personal source before applying. Use `Regex` only after a validation function accepts the pattern and reject unsupported/unsafe patterns with a surfaced rule error rather than applying a partial transformation.

- [ ] **Step 4: Run focused tests and verify all pass.**

Run the Task 1 command again. Expected: PASS.

## Task 2: Persist Local Controls And Integrate Website Glossary Endpoints

**Files:**
- Create: `app/src/main/java/com/novalpie/nativeapp/data/ReaderReplacementRulesStore.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/data/ReaderReplacementRulesStoreTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt`

- [ ] **Step 1: Write failing persistence and API contract tests.**

```kotlin
@Test fun hiddenSharedRulesArePersistedPerBook() {
    store.saveHiddenSharedRuleIds(bookId = 12, ids = setOf("shared-a", "shared-b"))
    assertEquals(setOf("shared-a", "shared-b"), store.loadHiddenSharedRuleIds(12))
}
@Test fun personalRulesPreserveOrderAndScope() {
    val rules = listOf(personalRule("a", "b", order = 2), personalRule("c", "d", scope = ReaderReplacementScope.CurrentChapter, order = 1))
    store.savePersonalRules(bookId = 12, rules = rules)
    assertEquals(rules, store.loadPersonalRules(12))
}
@Test fun glossaryApiUsesPersonalCrudEndpoints() {
    api.createPersonalGlossary(12, "Alice", "艾莉丝")
    assertEquals("/api/users/me/glossaries", server.takeRequest().path)
}
@Test fun sharedGlossaryUsesReadOnlyNovelEndpoint() {
    api.sharedGlossaries(12)
    assertEquals("/api/users/me/novels/12/glossary", server.takeRequest().path)
}
```

- [ ] **Step 2: Run tests and verify red.**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.novalpie.nativeapp.data.ReaderReplacementRulesStoreTest" --tests "com.novalpie.nativeapp.data.NovalPieApiTest"
```

- [ ] **Step 3: Implement storage and API calls.**

Persist local rules and shared-rule hides using a book-specific preference key. Add these API methods:

```kotlin
suspend fun personalGlossaries(novelId: Long): List<ReaderReplacementRule>
suspend fun createPersonalGlossary(novelId: Long, source: String, replacement: String): ReaderReplacementRule
suspend fun updatePersonalGlossary(ruleId: Long, replacement: String): ReaderReplacementRule
suspend fun deletePersonalGlossary(ruleId: Long)
suspend fun sharedGlossaries(novelId: Long): List<ReaderReplacementRule>
```

Use the source website contracts exactly: `GET/POST /api/users/me/glossaries`, `PUT/DELETE /api/users/me/glossaries/{id}`, and `GET /api/users/me/novels/{novelId}/glossary`. Do not add a shared-rule write endpoint.

- [ ] **Step 4: Run focused tests and verify green.**

Run the Task 2 command again. Expected: PASS.

## Task 3: Build The Reader Rule Management Surface

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderSettingsPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderAdvancedControls.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`

- [ ] **Step 1: Add failing presentation tests for source selection and effective rule state.**

```kotlin
@Test fun defaultRuleSourceIsPersonal() { assertEquals(Personal, defaultReaderRuleSource()) }
@Test fun cloningSharedRuleCreatesEditablePersonalOverride() {
    val cloned = cloneSharedRule(sharedRule("Alice", "艾莉丝", id = "shared-1"))
    assertEquals(ReaderReplacementSource.Personal, cloned.owner)
    assertEquals("shared-1", cloned.sharedRuleId)
    assertNotEquals("shared-1", cloned.id)
}
@Test fun selectedTextPrefillTrimsButDoesNotTransformSource() {
    assertEquals("Alice $1", selectedTextReplacementSource("  Alice $1  "))
}
```

- [ ] **Step 2: Run test and verify red.**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.novalpie.nativeapp.ui.ReaderPresentationTest"
```

- [ ] **Step 3: Implement the Compose surface.**

Add a `文本替换` section matching the supplied reference but designed for mobile scanning:

- Source segmented control: `我的规则` / `全部规则`.
- Personal list: add, edit, enable, delete, reorder, scope and title/content target.
- Shared list: per-rule visibility toggle and `复制到我的规则` action; no shared edit/delete.
- Current chapter preview line: matched rule count plus a text-only before/after sample. Selecting reader text opens the editor with source prefilled.
- Any rules mutation increments a replacement revision, recalculates the effective current chapter text, refreshes reader search/highlight and invokes TTS stop before a new session can start.

- [ ] **Step 4: Run focused presentation tests.**

Expected: PASS.

## Task 4: Send Effective Text Through Rendering And TTS

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderText.kt` if current chapter helpers live there
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderTextTest.kt`

- [ ] **Step 1: Write failing tests proving one pipeline feeds reading and TTS.**

```kotlin
@Test fun readerAndTtsUseTheSameEffectiveChapterText() {
    val rules = listOf(personalRule("广告", ""))
    val text = effectiveReaderText("广告正文", rules, chapterIndex = 1)
    assertEquals("正文", text.displayText)
    assertEquals(text.displayText, text.ttsText)
}
```

- [ ] **Step 2: Run focused test and verify red.**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.novalpie.nativeapp.ui.ReaderTextTest"
```

- [ ] **Step 3: Implement immutable effective chapter text.**

Retain fetched `ChapterContent` as source data. Create `EffectiveReaderChapterContent` with transformed title/content and a revision. Feed the same transformed paragraphs into reader body construction, text selection, search/highlight resolution and `toggleTts()`. Do not overwrite the API response or download cache.

- [ ] **Step 4: Run focused test and verify green.**

## Task 5: Make TTS Sentence Groups Continuous And Lifecycle Safe

**Files:**
- Create: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderTtsPlaybackPlan.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderTtsController.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderTtsPlaybackPlanTest.kt`
- Test: `app/src/test/java/com/novalpie/nativeapp/ui/ReaderTextTest.kt`

- [ ] **Step 1: Write failing tests for sentence packing and queue transitions.**

```kotlin
@Test fun packsSentencesUntilNaturalUtteranceLimit() {
    val chunks = readerTtsSegments(listOf("第一句。第二句。第三句。"), maxLength = 8)
    assertEquals(listOf("第一句。第二句。", "第三句。"), chunks)
}

@Test fun staleGenerationCompletionCannotAdvanceQueue() {
    val state = ReaderTtsPlaybackPlan.start(listOf("甲。", "乙。"), generation = 5)
    assertEquals(state, state.onUtteranceDone(generation = 4, utteranceIndex = 0))
}
@Test fun errorClearsQueuedPlaybackAndReturnsTerminalState() {
    assertTrue(ReaderTtsPlaybackPlan.start(listOf("甲。"), 1).onError(1).isTerminal)
}
@Test fun nextChapterStartsWithItsFirstActualUtterance() {
    assertEquals("下一章首句。", ReaderTtsPlaybackPlan.nextChapter(listOf("下一章首句。"), 2).currentText)
}
```

- [ ] **Step 2: Run TTS tests and verify red.**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.novalpie.nativeapp.ui.ReaderTtsPlaybackPlanTest" --tests "com.novalpie.nativeapp.ui.ReaderTextTest"
```

- [ ] **Step 3: Implement rolling utterance queue.**

- Pack sentences up to `DEFAULT_TTS_SEGMENT_LENGTH`; commas are never boundaries.
- Give each queued chunk a generation-scoped utterance id.
- Start with `QUEUE_FLUSH`, then maintain a bounded `QUEUE_ADD` look-ahead window of two chunks.
- Drive state from `UtteranceProgressListener`; all callbacks check generation before mutating Compose state.
- On error, call `engine.stop()`, clear queued chunks/callbacks, set terminal error state and make the next play action safe.
- When a persisted voice is missing, clear it in memory and use the selected language default with a visible nonfatal message.

- [ ] **Step 4: Run focused TTS tests and verify green.**

## Task 6: Reader Integration And Device QA

**Files:**
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- Modify: `app/src/main/java/com/novalpie/nativeapp/ui/ReaderAdvancedControls.kt`
- Test: `ReaderPresentationTest.kt`, `ReaderTextTest.kt`, `ReaderTtsPlaybackPlanTest.kt`, and the configured Android emulator QA flow

- [ ] **Step 1: Add reader-level regression tests.**

```kotlin
@Test fun ruleRevisionStopsActiveTtsBeforeNewEffectiveTextIsSpoken() {
    assertEquals(ReaderTtsState.Stopped, ttsStateAfterReplacementRevision(ReaderTtsState.Speaking))
}
@Test fun autoNextChapterUsesFirstEffectiveParagraph() {
    assertEquals("更名后的首句。", firstEffectiveTtsParagraph("原名首句。", listOf(personalRule("原名", "更名后"))))
}
```

- [ ] **Step 2: Run tests to verify red, then implement minimum integration.**

Update the reader callback flow so rule edits invalidate effective-text state and safely stop active TTS; update auto-next to load the adjacent chapter content before queueing its first transformed utterance.

- [ ] **Step 3: Run complete unit suite and build debug APK.**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Expected: both commands exit `0`.

- [ ] **Step 4: Run device QA.**

Verify on the configured Android target:

1. Add literal and regex personal rules; confirm reader body and TTS both change.
2. Hide a shared rule, clone it personal, then confirm personal override wins.
3. Apply a chapter range rule and confirm adjacent chapters are untouched.
4. Play a long chapter, pause/stop/restart, switch chapter, and confirm no sentence loss or stale audio.
5. Change voice/rate while playing and confirm old queued utterances never play.
