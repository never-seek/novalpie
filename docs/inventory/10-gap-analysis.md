# 10 — Gap Analysis / Completeness Critique of the Refactor Inventory

Critic pass over sections 01–09. Method: mechanical coverage diffing (file-name, symbol-name and
user-visible-string set arithmetic between the nine markdown files and the actual source tree),
then targeted source verification of every claim that looked internally inconsistent.

Verified against `D:/NovalPie/native-android` working tree at 2026-07-26, HEAD = `fc1d555`
("Baseline: NovalPie 2.0 native app before refactor").

**Verdict up front:** the inventory is unusually complete at the level it was asked to cover — file
coverage is 65/65, and **1,111 of 1,111 distinct user-visible CJK string literals in the whole main
source tree are documented somewhere (zero undocumented)**. The gaps that matter are not missing
files; they are (a) a handful of load-bearing *non-string* behaviours nobody wrote down, (b) six
factual contradictions, two of which are wrong in a way that will cause a wrong refactor decision,
and (c) the fact that the working tree has silently moved out from under sections 01–08.

---

## 1. File coverage: no uncovered files

**Correction to the brief.** The brief says "115 `.kt` files, 65 in main". At the moment the nine
sections were written the tree held **115 `.kt` files = 64 main + 51 test** — section 09's "51 test
files" is exactly right, and the main count was 64, not 65. During this analysis the concurrent
agent added two more files (§5), so the tree is now **117 = 65 main + 52 test**. The two newcomers
are Phase-2 output, not baseline code:

```
app/src/main/java/com/novalpie/nativeapp/ui/design/NovalPieColors.kt   (199 L, created 15:34, untracked)
app/src/test/java/com/novalpie/nativeapp/ui/design/ColorContrastTest.kt (created 15:34, untracked)
```

Everything below therefore assesses the **64 baseline main files**, which is the correct denominator
for judging the inventory.

Set-differencing every baseline `main` basename against the union of all nine sections:

```
main files never mentioned in any section : 0
main files never mentioned in a BEHAVIOURAL section (01–07) : 0
```

Every one of the 64 baseline main files is named by at least one behavioural section, not merely by
the design (08) or test (09) audits. There is no orphaned file. I specifically checked the small helpers
that are easiest to lose, and each has its declared symbols documented by name:

`ErrorRecovery.kt` (`retryActionLabel`), `BookDetailProgressMarker.kt`
(`isBookDetailProgressChapter`), `CatalogSummary.kt` (`catalogSummaryLabel`), `BookFilter.kt`
(`bookMatchesQuery`), `ChapterFilter.kt` (`chapterMatchesQuery`), `ReaderProgressLabel.kt`
(`readerChapterProgressLabel`), `VisibleUiLabels.kt`, `NovelCardFacts.kt`, `BookDetailFacts.kt`,
`ReaderAdjacentChapter.kt`, `RouteStackPolicy.kt`, `RequestFreshness.kt`, `UploadFileSource.kt`,
`EpubWriter.kt`, `EditorScriptEngine.kt`, `BookChapterPresentation.kt`
(`bookAccessPolicyFromDraft`, `chapterIllustrationPlaceholder`), `WorkspacePresentation.kt`
(`maskWorkspaceApiKey`), `PoliticalExamPresentation.kt` (`formatPoliticalExamTime`,
`politicalExamCorrectSummary`), `ForumPresentation.kt` (all 11 symbols incl. the private
`forumCommentRoot` / `forumPlainParagraphs`).

### String-level coverage is effectively total

This is the strongest available evidence of rebuild-sufficiency for UI copy, so I measured it
exactly rather than sampling:

| population | distinct strings | undocumented |
|---|---|---|
| literal-CJK `"…"` in all baseline main files | 877 | **0** |
| `\uXXXX`-escaped CJK `"…"` in all baseline main files | 234 | **0** |
| **total** | **1,111** | **0** |

(The single apparent miss, `鏍囩`, is the known mojibake literal and *is* documented in 02/04/07 —
it only failed byte-exact matching because those sections quote it alongside its decoded form.)

Spot-check on the file with the *lowest* citation density — `AdminScreens.kt`, 849 source lines but
only 25 `file:line` citations across the inventory — found **0 of its 91 visible strings missing**.
So low citation density in section 06 reflects a narrative (rather than tabular) documentation
style, not thin coverage. Do not treat citation count as a coverage proxy for that section.

---

## 2. Real content gaps — behaviours no section documents

These are the actual losses. All are non-string behaviours, which is precisely the class the
string-level completeness above cannot protect.

### G1 (HIGH) — `data/NovalPieImageLoading.kt` is documented only as "the thing that gets reconfigured"

Every mention of this file across all nine sections concerns *when* `Coil.setImageLoader` is called
(cold start, `VM:553`, `saveProxySettings`) or *that* it takes `ProxySettings`. **Nothing documents
what the loader actually does.** Verified contents:

```kotlin
private const val IMAGE_USER_AGENT = "NovalPieNative/2.0 Android"   // :9
private const val IMAGE_REFERER    = "https://novalpie.cc/"          // :10
// :27-33 request interceptor sets BOTH headers on every image request
// :24-26 connect 15s / read 45s / call 60s   (note: different from the API client's 12/20/30)
// :19    .crossfade(true)
// NO diskCache(), NO memoryCache(), NO respectCacheHeaders() — all Coil defaults
```

The `referer: https://novalpie.cc/` header is almost certainly hotlink-protection bypass for the
cover CDN. A rebuilt image layer that omits it, or that reuses the API OkHttp client's timeouts,
will render broken or slow covers app-wide with no compile error and no test failure
(`NovalPieImageLoadingTest.kt` exists but section 09 does not record what it asserts). Grep
confirms: `referer` appears once in the entire inventory (section 09, incidentally);
`IMAGE_REFERER`, `IMAGE_USER_AGENT`, `diskCache` appear zero times.

### G2 (HIGH) — the cover decode target `.size(1024, 1536)` + `Precision.EXACT`

`NovalPieApp.kt:3380-3385` builds every cover `ImageRequest` with `.size(1024, 1536)` and
`.precision(Precision.EXACT)`. `README.md:176-178` explicitly records this as a deliberate fix
("Coil requests a 1024x1536 decode target to avoid visibly blurry card covers"). **No inventory
section mentions `1024`, `1536`, `decodeTarget`, or `Precision`.** Section 08 discusses cover
*sizes* (100×150, 104×148, 72×96) and the 2:3 ratio drift but not the decode target. Section 04
documents the `SubcomposeAsyncImage` loading/error fallback but not the request builder. A rebuild
that drops this regresses a specifically-fixed visual defect.

### G3 (HIGH) — the automatic proxy route order **flips on x86**, and no section says so

`NetworkConfigStore.kt:59-60`:

```kotlin
private fun defaultProxyHosts(preferEmulatorProxy: Boolean): List<String> =
    if (preferEmulatorProxy) DEFAULT_EMULATOR_PROXY_HOSTS.asReversed()   // ["127.0.0.1","10.0.2.2"]
    else DEFAULT_EMULATOR_PROXY_HOSTS                                    // ["10.0.2.2","127.0.0.1"]
```

`README.md:200-203` and `docs/LIVE_SITE_ROUTE_API_MATRIX.md:154-168` both document the two-way
behaviour explicitly, and the matrix records *why* ("Turn 36 superseded the preferred order because
the current MuMu QA path uses adb reverse reliably through `127.0.0.1:7890`"). The inventory
documents only the ARM branch — and section 08 applies the ARM order to the x86 case, i.e. exactly
backwards. `defaultProxyHosts` and `asReversed` (in this context) appear zero times in the
inventory. See contradiction C1 below; this is both a gap and an error.

Corollary also undocumented: `ProxySettings.summary()` hardcodes
`"auto: 127.0.0.1/10.0.2.2:7890 + direct"` (`NetworkConfigStore.kt:27`), which is the *emulator*
order. On a real ARM device the Settings screen therefore displays a route order that is the reverse
of what the app actually attempts. Section 05 quotes the summary string verbatim but does not flag
that it is wrong on the majority of hardware.

### G4 (MEDIUM) — `/register` and `/reset-password` have no representation anywhere

`docs/LIVE_SITE_ROUTE_API_MATRIX.md:19-20` lists both as live site routes. The app has no native
route, and — verified against section 01's complete 9-URL `openWebFallback` list — **no web-fallback
entry point either**. `grep -i 'reset-password'` returns zero hits in both the source tree and the
whole inventory. Consequence: a user who is not already logged in on the WebView cannot create an
account or recover a password from inside the app at all; the only auth affordance is
`openLoginFallback` → `https://novalpie.cc/login`. This is a pre-existing product hole rather than a
refactor risk, but it belongs on the record because the inventory currently reads as if `/login` is
the complete auth story. `/reader` (standalone) and `/user` (own-profile, id-less) are likewise
site routes with no app representation, though those are adequately covered by native equivalents.

### G5 (LOW) — `MainActivity.kt` cold-start ordering

Sections 07 and 08 both cite `MainActivity.kt:14` for the Coil configuration and `:15-19` for the
`startUri` rotation bug, but no section states the full 4-statement contract, which is load-bearing
for the "Coil cache discarded twice per cold start" finding to be actionable:

```kotlin
configureNovalPieImageLoader(this, NetworkConfigStore(this).loadProxySettings())  // :14 — a SECOND
                                                                                  // NetworkConfigStore
                                                                                  // instance, before the VM exists
val startUri = intent?.data?.toString()                                           // :15
setContent { NovalPieTheme { NovalPieApp(startUri = startUri) } }                 // :16-20
```

That `NetworkConfigStore(this)` is a distinct instance from the ViewModel's, doing a main-thread
prefs read before `onCreate` returns — relevant to whoever fixes the triple `setImageLoader`.

### G6 (LOW) — manifest surface

Only section 08 mentions the manifest at all (2 hits), and only for `allowBackup` / missing
`android:icon` / `screenOrientation`. Not documented anywhere: the app declares exactly **one**
permission (`android.permission.INTERNET`), there is no `usesCleartextTraffic`, no
`networkSecurityConfig`, and no `android:name` Application subclass. Small, but a rebuild that
adds a `network_security_config.xml` or an `Application` class is changing an undocumented baseline.

---

## 3. Contradictions between sections

### C1 (MATERIAL — will cause a wrong decision) — proxy route order on x86

- **07** (finding 1, and again at 07:89): "route selection returns `[10.0.2.2:7890,
  127.0.0.1:7890, DIRECT]`. OkHttp tries `10.0.2.2:7890`" — stated unconditionally.
- **08** (P3): "on Chromebooks/WSA/**x86** tablets a release build routes every OkHttp request
  through `Proxy(HTTP, 10.0.2.2:7890)` then `127.0.0.1:7890`".
- **Source** (`NetworkConfigStore.kt:59-60`, and `shouldPreferEmulatorProxy()` at `:71-75` which is
  true for x86/x86_64): on x86 the order is **reversed** — `127.0.0.1` first.
- **Docs** (`README.md:200-203`, `LIVE_SITE_ROUTE_API_MATRIX.md:156-158`) state both branches
  correctly.

Section 08's sharpest network claim is therefore precisely inverted for the exact hardware class it
names. The underlying defect (no `equals` on `FixedProxySelector` → zero connection reuse; dead
proxy tried before DIRECT) is real and correctly diagnosed in 07 — only the ordering is wrong. Fix
the wording before anyone uses it to reason about which host to strip.

### C2 (MATERIAL — will cause a wrong decision) — the backup-rules finding is stale and now false

**08**'s P3 headline: "**The auth-JWT backup mitigations are dead code.** `AndroidManifest.xml:6`
sets `allowBackup="true"` with **no** `android:fullBackupContent` and **no**
`android:dataExtractionRules`, while `res/xml/backup_rules.xml` and
`res/xml/data_extraction_rules.xml` exist and are … never referenced from the manifest and therefore
do nothing."

Verified working tree:

```
app/src/main/AndroidManifest.xml  (modified 14:48:23, not committed)
+        android:fullBackupContent="@xml/backup_rules"
+        android:dataExtractionRules="@xml/data_extraction_rules"
app/src/main/res/xml/             (UNTRACKED — both files created 14:48)
```

`git show HEAD:…/AndroidManifest.xml` has `allowBackup="true"` and neither attribute, and
`res/xml/` does not exist at HEAD at all. So: the two XML files and the manifest wiring were
created **together**, by the concurrent migration agent, at 14:48 — 24 minutes *before* section 08
was written (15:12). Section 08 read the new `res/xml/` files but the old manifest, and inferred a
dangling-mitigation defect that has never existed in either state. `backup_rules.xml` even contains
the migration agent's own commentary ("`allowBackup` was true with no exclusions, so `adb backup`
could lift a logged-in session off the device"), which section 08 quoted as if it were pre-existing
baseline code.

The residual true findings from that item survive: the JWT *is* plaintext in SharedPreferences
(`AuthSessionStore.kt:43-65`, no `EncryptedSharedPreferences`) and *is* injected into WebView
`localStorage` (`WebFallbackScreen.kt:138-140`). Only the "dead code" framing is wrong.

### C3 — dead-composable count: 9 vs 5 vs +1, all three low

Reference-counted every candidate across `app/src` (a count of 1 = the declaration only ⇒ dead):

| composable | `NovalPieApp.kt` line | refs | 04 | 05 | 08 |
|---|---|---|---|---|---|
| `GroupSection` | 2792 | 1 | ✔ | | ✔ |
| `FavoriteStatusCard` | 3050 | 1 | ✔ | | ✔ |
| `SearchResultHeader` | 2925 | 1 | ✔ | | ✔ |
| `ReaderHeader` | 2040 | 1 | ✔ | | ✔ |
| `ProductHeaderBlock` | 2892 | 1 | ✔ | | ✔ |
| `DiscoverEmptyResultPanel` | 1536 | 1 | ✔ | | — |
| `BookSummary` | 3323 | 1 | ✔ | | — |
| `ChoiceChips` | 1711 | 1 | ✔ | | — |
| `HeroCard` | 2904 | 1 | ✔ | | — |
| `UserSection` | 2620 | 1 | — | ✔ | — |

**Truth: 10 dead composables, all in `NovalPieApp.kt`.** Section 04 has 9, section 05 independently
has the 10th, section 08 claims "5 dead composables (90 unreachable lines, **verified by
full-project reference count**)" — the parenthetical is not supportable; it undercounts by half.
Nobody has the union. Since 04 correctly warns that several of these are the *sole owners* of
unique strings (`暂无分组`, `当前账号已收藏`, `没有匹配结果`, and `UserSection`'s debug copy which
`ProfilePresentationTest.kt:141-162` actively forbids), the union list above is the one to work
from — and note that `08`'s "90 unreachable lines" is therefore also low.

### C4 — `AppRoute` object/data-class split: section 01 contradicts itself

- 01 `counts:` line — "24 AppRoute entries (**12 object, 12 data class**)".
- 01 finding 1 — "Correction to the count: **13 objects + 11 data classes**."
- **Source** (`NovalPieViewModel.kt:104-129`) — 13 objects (Forum, Home, Search, Tools, Profile,
  Settings, MessageCenter, MessageSettings, Workspace, UploadBook, UploadEditor, PoliticalExam,
  **ForumCreate**) + 11 data classes = 24. **The finding is right; the headline count is wrong.**

`ForumCreate` being an `object` declared at `:120` in the middle of the data-class block is the
trap. Section 02 says only "24-member", so it is not wrong, merely silent.

### C5 — `routeContextLabel` omissions: 06 is incomplete

- 01: "`AppRoute.PoliticalExam` **and** `AppRoute.WebFallback` have NO `routeContextLabel` entry …
  17 mappings + 1 else-fallback."
- 05: agrees `WebFallback` has no entry.
- 06: "`AppRoute.PoliticalExam` has NO case … a live bug" — does not mention `WebFallback`.
- **Source** (`UiNavigation.kt:19-38`) — exactly 17 `->` mappings + `else`. Uncovered routes:
  `PoliticalExam`, `WebFallback`, plus the 5 tab roots (Forum/Home/Search/Tools/Profile, whose
  fall-through to `bottomTabDisplayLabel` is intentional).

01 is correct and complete. Anyone reading only 06 will fix one of the two bugs.

### C6 — endpoint counting basis differs (not an error, but reconcile it)

- 03: "**74** distinct endpoint path templates (68 `/api/*`, 6 `/workspace/*`)".
- 09: "**79** distinct endpoints with request assertions".

You cannot assert 79 endpoints if 74 exist. Verified: 77 raw quoted path strings in
`NovalPieApi.kt`, collapsing to ~74 real templates once prefix fragments are merged
(`/api/messages/` + `/api/messages/{}`, `/api/posts/`, `/api/users/`,
`/api/users/me/chapters/`, `/api/users/me/novels/`, `/workspace/apis/`, and the three
`?id=`-in-path admin deletes). Section 03's 74 is right. Section 09's 79 counts **method × path**
pairs (`GET` vs `DELETE /api/messages` etc.). Harmless, but label the bases or a reviewer will
hunt five phantom endpoints. I re-verified that section 03's per-function table does cover the
endpoints I initially thought were missing — `/api/novels/{bookId}/detail`, `/chapters`, `/photo`
and `/api/tags` are all present; my first grep was malformed.

### C7 — minor line-number drift (no action needed, listed for completeness)

`openUserProfile`-self: 01 says `VM:935-936`, 07 says `VM:933-938`. `enableOnBackInvokedCallback`:
01 says `AndroidManifest.xml:8`, actual is `:9` in the working tree (`:7` at HEAD, before the two
backup attributes were inserted) — itself a symptom of C2. Section 08's "1685 inline CJK literals
(1253 distinct)" vs my 1,111 distinct is a filter difference (08 counts interpolated and
1-character literals; I excluded `$`-interpolated and length<2), not a conflict.

---

## 4. Features in README / route matrix that no section documents

I walked `README.md:27-228` ("Implemented first-stage native surfaces", the canonical feature
list), `README.md:957-992` (proxy notes + known alpha limits) and the whole route matrix against
the inventory. Almost everything is covered — the reader crypto flow, `replace_mode=india&
show_images=1`, `min_word_count`/`max_word_count`, `group_id` favorite chips, catalog sorting,
`NOVALPIE_NATIVE_COMPOSE_HOME`, search-history bounds, JWT admin gating, all 12 message types, all
6 admin routes, chapter illustrations/transfer/access-policy, the `adb reverse` QA contract. The
at-risk items are:

1. **Coil `1024x1536` decode target** — README:176-178 documents it as a deliberate anti-blur fix;
   inventory silent. (= G2)
2. **Two-branch automatic proxy order** — README:200-203 + matrix:154-168 document it; inventory
   documents one branch and gets the other backwards. (= G3/C1)
3. **`/register`, `/reset-password`** — matrix:19-20; zero app and zero inventory presence. (= G4)
4. **Image-loader `referer`/UA headers** — not in README either, but it is the mechanism behind
   README's cover-rendering claims. (= G1)
5. **README:35-36 understates the demo-post fallback**: "falls back to local seed rows only when
   live data is not available." Sections 04 and 07 correctly establish that the 6 fake posts also
   render on a *successful but empty* feed, stacked under the error card. Here the inventory is
   **better than the doc** — flagging it so the README is corrected rather than trusted.
6. **Matrix:141-147 "Still requiring native migration"** (workspace internals, upload/editor,
   political exam, all admin internals) is stale — section 06 documents all of these as fully
   native. The matrix's "Safe embedded fallback now: workspace / upload/editor / political exam /
   all six administrator pages" (matrix:134-139) directly contradicts section 01's finding that
   those 14 routes have **no** web escape hatch. Section 01 matches the source. Do not let the
   matrix's stale claim justify deleting native admin/workspace code.

---

## 5. THE dominant risk: the working tree is moving under sections 01–08 *right now*

Section 09 flagged a concurrent migration; it understated the blast radius, because it checked only
build files and test files. The refactor is **not** paused waiting on this inventory — it is running
concurrently, and it advanced twice during this gap analysis:

| time | event |
|---|---|
| 14:36:12 | section 09's "last verified green baseline" (252 tests, 0 failures) |
| 14:48:23 | Phase 1: manifest + `res/xml/` backup rules created (→ invalidates 08's P3, see C2) |
| 15:04–15:05 | Phase 1: **8 main UI files rewritten** (AutoMirrored icons + FlowRow API) |
| 15:06 / 15:12 | sections 09 / 08 written — i.e. **after** the 15:04 UI edits |
| **15:34:43** | **Phase 2 begins:** `ui/design/NovalPieColors.kt` (199 L) + `ColorContrastTest.kt` created |

`NovalPieColors.kt` already defines `NovalPieColorTokens`, `NovalPieLightColorTokens`,
`NovalPieDarkColorTokens` and `toColorScheme()` — i.e. the colour half of the design system section
08 recommended is being written while section 08's own contrast numbers (`onSurfaceVariant` 3.53:1,
dark `onPrimary` 2.77:1, `surfaceVariant`/`background` 1.04:1) are the input to it. Anyone re-reading
section 08 as a to-do list will duplicate work already done. Note also that `ThemePaletteTest.kt`
asserts the *old* token values, so 08's warning that "fixes require test updates" is now live.

Full working-tree divergence from `HEAD`/`fc1d555`:

```
 D app/build.gradle          D build.gradle          D settings.gradle
?? app/build.gradle.kts     ?? build.gradle.kts     ?? settings.gradle.kts
?? gradle/libs.versions.toml
?? app/src/main/res/xml/          <-- NEW (backup_rules.xml, data_extraction_rules.xml)
?? app/src/main/java/com/novalpie/nativeapp/ui/design/   <-- NEW, Phase 2, 15:34
?? app/src/test/java/com/novalpie/nativeapp/ui/design/   <-- NEW, Phase 2, 15:34
?? docs/inventory/                <-- byte-identical copy of sections 01-09 (verified via cmp)
 M gradle.properties  M gradle/wrapper/gradle-wrapper.properties  M app/proguard-rules.pro (+86)
 M app/src/main/AndroidManifest.xml                               (+2)
 M .../ui/NovalPieApp.kt            (28 changed)
 M .../ui/MessageScreens.kt (4)   M .../ui/UploadEditorScreens.kt (8)
 M .../ui/UploadScreens.kt (4)    M .../ui/WorkspaceScreens.kt (2)
 M .../ui/PoliticalExamScreens.kt (2)  M .../ui/WebFallbackScreen.kt (5)
 M .../ui/EditorScriptEngine.kt (4)
```

**Nine main UI source files are already modified.** Two classes of change, and the second is not
cosmetic:

- `Icons.Filled.{ArrowBack,MenuBook,Reply}` → `Icons.AutoMirrored.Filled.*` (mechanical Material3
  1.3 deprecation fix).
- **`verticalAlignment = Alignment.CenterVertically` → `verticalArrangement = Arrangement.spacedBy(4|6|8.dp)`**
  at 5+ `FlowRow` sites in `NovalPieApp.kt`. This is the Compose 1.7 `FlowRow` API change section 08
  predicted, but the substitution is **not** semantics-preserving: it replaces cross-axis *alignment*
  with cross-axis *spacing*, so multi-line flow rows now have injected gaps. There are 6 `FlowRow`
  sites in `NovalPieApp.kt` today.

Consequences the parent agent must act on:

1. `app/proguard-rules.pro` grew from 1 comment line to ~87 lines, and section 08's
   `minifyEnabled false` / "one-comment-line ProGuard file" finding is against the deleted
   `app/build.gradle`. Re-verify R8 state from `app/build.gradle.kts` before acting on that item.
2. Every `file:line` citation into those nine UI files carries up to ~28 lines of drift.
   `NovalPieApp.kt` line numbers are the most-cited artefact in the entire inventory (400 citations)
   and are now the least trustworthy. Sections 04/05/06/08 should be re-anchored, or all consumers
   told to resolve citations against `git show fc1d555:<path>` rather than the working tree.
3. Section 09's "last verified green baseline" (252 tests, `2026-07-26 14:36:12`) predates the
   FlowRow layout edits *and* the new `ColorContrastTest.kt`. It is a valid *count* baseline but no
   longer a behavioural one, and the count itself will shift as Phase 2 adds tests.
4. `docs/inventory/` duplicates all nine sections byte-for-byte. Declare one canonical location
   before either copy is edited, or the next agent will patch the wrong one.
5. **Sections 08 and 09 are partly reports on work already completed, not on the baseline.** Both
   were written after the 15:04 UI rewrite. Every toolchain recommendation in 08's P3 block (AGP,
   Gradle, Kotlin, Compose BOM, Material3, coil, okhttp, version catalog, R8) is already implemented
   in the untracked `.kts` + `libs.versions.toml` files. Treat 08's P3 section as *verification
   criteria*, not as a work plan.

---

## 6. Sufficiency assessment: can the app be rebuilt from the inventory alone?

**For user-visible copy and API contracts: yes.** 1,111/1,111 strings and 74/74 endpoint templates
are documented, with the byte-level detail that matters (`tags` as comma-string vs JSON array,
camelCase poll keys, omitted-when-null fields, the blank-query-param drop at
`NovalPieApi.kt:1595`, the reader crypto chain constant-by-constant). Section 03 is the strongest
document in the set; the reader HMAC/AES section alone is reproducible from the prose.

**For behaviour: yes with three named exceptions** — G1 (image loader headers/timeouts), G2 (cover
decode target), G3 (x86 proxy order). Fix those three and the behavioural contract closes.

**Where it is genuinely thin:**

- **Screen *layout* below the section level.** Every section documents section order, controls and
  strings, but no section captures the composable *tree* — nesting, weights, `Arrangement` values,
  which `Text` sits inside which `Row`. Section 08 catalogues the primitives (747 `.dp`, 97
  `RoundedCornerShape`, 63 `SpaceBetween` rows) as *audit findings to eliminate*, not as a spec to
  reproduce. That is defensible given Phase 6 is "rebuild all screens on the design system" — but
  it means the inventory cannot answer "did I lose a row?" during that rebuild. Given the FlowRow
  edits above already changed spacing silently, this is the thinnest load-bearing area. Mitigation
  is cheap and should happen before Phase 6 starts: `tools/golden_strings.py` (section 09) already
  gives a 1,236-string golden master and nothing runs it — wire it into the verify gate now.
- **`ui/AdminScreens.kt` (849 lines) and the four `*Screens.kt` files at ~0.03–0.05 citations per
  source line.** Strings are complete (verified 91/91 for Admin), but table column order, dialog
  field order and filter-rail composition are prose-only. Enough to rebuild a *working* admin
  screen, not enough to rebuild the *same* one.
- **Negative paths.** Section 09 establishes that all 73 API contract tests enqueue HTTP 200 with
  well-formed JSON — zero 401/403/500/HTML/malformed coverage. The inventory inherits that blind
  spot: it documents what the ~50 `normalize*` helpers accept, but not what they do with garbage.
  Combined with 07's finding that error bodies are read and discarded (`Api:1661-1667`), the entire
  failure surface is specified only by the pre-rendered Chinese message strings.
- **`app/src/test` internals.** Section 09 lists suites, counts and the highest-value pinned
  strings, but the 50 test files are not documented assertion-by-assertion. Since section 09 also
  establishes that `verify-native-project.ps1` hard-codes 39 file paths, ~180 greps and 30 test
  method names — and that two of its assertions are *vacuous* because of the CP936/GBK decoding bug
  — the verification layer is the least-specified part of the system and the one most likely to
  block the refactor mechanically.
- **Process-death / restore.** Section 01 correctly records that there is no `SavedStateHandle` and
  nothing survives process death. Nobody documents what *should* survive, so Phase 7 has no target.

**Priority order for closing gaps before the refactor proceeds:**

1. Correct C1 and C2 in place (both are wrong in a direction that invites a harmful change).
2. Write down G1 + G2 (10 lines of prose; irreversible visual regressions otherwise).
3. Publish the union 10-item dead-composable list (C3) with its sole-owner strings.
4. Re-anchor citations for the nine modified UI files, or pin all consumers to `fc1d555`.
5. Wire `tools/golden_strings.py` into the verify gate before Phase 6 touches any screen.
6. Fix 01's headline `AppRoute` count (C4) and add `WebFallback` to 06's label-omission note (C5).
