# NovalPie 2.0 Native — Refactor Execution Plan

Date: 2026-07-26
Target: `D:\NovalPie\native-android` (Kotlin + Jetpack Compose Android client for novalpie.cc)
Baseline snapshot: `D:\NovalPie\native-android-baseline-snapshot-20260726`

---

## 1. Diagnosis

Measured, not inferred. Every claim below is verified against the current tree.

### 1.1 Blocking

| # | Finding | Evidence |
|---|---|---|
| B1 | **The app does not compile.** 5 errors, all `FlowRow(verticalArrangement=)`. That parameter does not exist in Compose Foundation 1.4.3 — `javap` on the cached `foundation-layout-1.4.3.aar` confirms the signature is `FlowRow(Modifier, Arrangement.Horizontal, Alignment.Vertical, Int, content)`. `verticalArrangement` arrived in Compose 1.6. | `ui/NovalPieApp.kt:1608,2997,3111,3121,3198` |
| B2 | **No version control.** `D:\NovalPie\.git` is an empty directory; `git status` reports "not a git repository". A 20k-LOC refactor had no rollback path. | `ls -la /d/NovalPie/.git` → empty |
| B3 | **Gradle wrapper is unusable on a clean machine.** `distributionUrl` points at `mirrors.cloud.tencent.com`, which fails TLS handshake. Builds only work with `GRADLE_USER_HOME=D:\NovalPie\.gradle-sandbox` where an 8.0.2 dist was already unpacked. | `gradle/wrapper/gradle-wrapper.properties:3` |

### 1.2 Architecture

| # | Finding | Evidence |
|---|---|---|
| A1 | **Three god files hold 55% of the code.** | `NovalPieViewModel.kt` 4063, `NovalPieApp.kt` 3654, `NovalPieApi.kt` 3404 of 20 587 total main LOC |
| A2 | **One ViewModel owns the entire app.** ~40 `mutableStateOf` properties, ~30 state data classes, and every user action across 25 routes — search, reader, forum, messages, upload, EPUB editing, admin, exams. | `ui/NovalPieViewModel.kt:417-4031` |
| A3 | **Callback threading instead of state.** The route dispatch passes individual lambdas per screen — `BookChapterManagerScreen` takes 19, `UploadEditorScreen` takes 27. Adding one control means touching three files. | `ui/NovalPieApp.kt:182-578` |
| A4 | **Staleness handled by 16 hand-rolled counters.** `forumRequestSerial`, `homeRequestSerial`, … each load re-implements the same guard; some loads have no guard at all. | `ui/NovalPieViewModel.kt:451-466` |
| A5 | **No repository layer, no DI.** The ViewModel constructs `NovalPieApi` and 8 `*Store` classes directly, so nothing above the network layer is unit-testable. | `ui/NovalPieViewModel.kt:418-448` |
| A6 | **3.4k lines of hand-written `org.json` normalization.** ~90 `normalize*()` functions with wide field-alias tolerance. The tolerance is load-bearing (the server returns inconsistent shapes) but it is untyped and duplicated. | `data/NovalPieApi.kt:1701-3320` |
| A7 | **Hand-rolled navigation.** `mutableStateListOf<AppRoute>` back-stack; top bar, bottom bar and `BackHandler` each independently re-test "is this a root route" with three separate copies of the same 5-way condition. | `ui/NovalPieApp.kt:123,138,152` |

### 1.3 Design — the "lacks good UI design" complaint, diagnosed

The dominant defect is a **half-applied colour scheme**.
`lightColorScheme()`/`darkColorScheme()` are called with only 14 of Material3's ~30 roles.
Every unspecified role falls back to Material3's *baseline purple/pink*. Result, visible in
every QA screenshot: pink chips, a pink "继续阅读" card and a pink navigation indicator sitting
next to the declared blue `#3182ED` primary.

| # | Finding | Evidence |
|---|---|---|
| D1 | **Palette clash.** Unspecified `tertiary*`, `error*`, `surfaceTint`, `inverse*`, `scrim`, `outlineVariant`, `surfaceContainer*` leak baseline pink. | `ui/NovalPieTheme.kt:69-101`; `qa-screenshots/turn38/search_results_postfix.png`, `turn39/home_after_fix.png` |
| D2 | **System bars don't match the app.** `styles.xml` hardcodes `#FFF8F4` status/navigation bar against a `#F2F2F2` light background and `#191C1F` dark background. No `values-night/`. No edge-to-edge, no `WindowCompat`. | `res/values/styles.xml:4-6` vs `ui/NovalPieTheme.kt:41,60` |
| D3 | **No app identity.** `res/` contains only `values/`. Zero launcher icons — no `mipmap-*`, no adaptive icon, no themed/monochrome icon, no splash screen. The app ships with the default Android icon. | `find app/src/main/res -type d` → `values` only |
| D4 | **Content buried under chrome.** On Home and Search the first piece of real content sits below the fold, under a two-line centre title, a heading that repeats the tab name, a search field, a filter chip row and two button rows. | `turn39/home_after_fix.png` (book grid starts at ~85% height); `turn38/search_results_postfix.png` |
| D5 | **Chip rows clip mid-glyph.** The Discover filter row runs off the right edge with no scroll affordance — "字…" is cut through the character. | `turn38/search_results_postfix.png` |
| D6 | **Three names for one screen.** Top bar "搜索", page heading "发现", tab label "搜索". | `ui/UiNavigation.kt` + `SearchScreen` |
| D7 | **Two different search inputs.** Home uses a pill Surface + separate 搜索 button; Discover uses an `OutlinedTextField` with a floating label. | `HomeScreen` vs `DiscoverSearchPanel` |
| D8 | **No design tokens beyond colour and shape.** No typography scale (Material3 defaults, untuned for CJK), no spacing scale, no elevation or motion tokens. `.dp`/`.sp` literals are scattered across all UI files. | `ui/NovalPieTheme.kt` defines only `ColorScheme` + `Shapes` |
| D9 | **No shared component layer.** Chips/pills are reimplemented per screen (`NovelTagPill`, `NovelSourcePill`, `LibraryStatPill`, `BookDetailFactLabel`, `CompactForumBadge`), as are loading, error, empty and section-header patterns. | `ui/NovalPieApp.kt:800,3016,3033,3169,3605` |
| D10 | **No feedback affordances.** No `SnackbarHost` anywhere; no pull-to-refresh (manual 刷新 buttons instead); errors render as inline body text; no skeletons; no route transitions. | whole-tree grep |
| D11 | **Error states lose navigation.** When chapter content fails the reader renders no top bar — only an error card and a comment composer, with no visible way back. | `turn39/reader.png` |
| D12 | **Arbitrary colour semantics.** On a result card `上传` is grey while `已完结` and `奇幻` are pink; the colours carry no meaning. | `turn38/search_results_postfix.png` |
| D13 | **No string resources.** `strings.xml` holds exactly one entry (`app_name`). ~1331 inline CJK literals plus thousands of `\uXXXX`-escaped literals across 50 UI files (`NovalPieApp.kt` alone: 1155 escapes; `NovalPieViewModel.kt`: 1977). Source is near-unreadable and cannot be localised. | `grep -c '\\u'` per file |

### 1.4 Production readiness

| # | Finding | Evidence |
|---|---|---|
| P1 | `minifyEnabled false` in **release**. No R8, no shrinking, no obfuscation. | `app/build.gradle:24` |
| P2 | No signing config in Gradle — releases are built unsigned and signed out-of-band. | `app/build.gradle` |
| P3 | `allowBackup="true"` while the auth JWT lives in SharedPreferences → token exfiltration via `adb backup`. | `AndroidManifest.xml:7`, `data/AuthSessionStore.kt` |
| P4 | Stale toolchain; AGP 8.0.0 is only tested to compileSdk 33 but the project sets 34, producing a build warning on every run. | `app/build.gradle:10`, build output |
| P5 | No version catalog, no Kotlin DSL, no lint/detekt/ktlint config, no CI. | repo root |
| P6 | **Zero UI tests and zero ViewModel tests.** 252 tests across 50 files are pure-function or MockWebServer contract tests. Nothing covers `NovalPieViewModel` (4063 lines) or any Composable. | `app/src/test/…`, no `androidTest/` |

---

## 2. Constraints

Non-negotiable properties the refactor must preserve.

1. **Every feature, route, control, label and rendered data point survives.** The inventory
   files under `docs/inventory/` are the written contract; Phase 6 ticks them off screen by screen.
2. **The reader's signed-session protocol moves verbatim.** `readerSessionKey()`,
   `readerSignatureHeaders()`, the custom base64 alphabet, `rotateLeft3Hex`, and
   `AES/GCM/NoPadding` with `aesKey = SHA-256(base64Decode(session_key))` are reverse-engineered
   from the live site. Any behavioural change breaks chapter reading outright.
3. **JSON field-alias tolerance is a feature, not redundancy.** The server returns
   `results`/`novels`/`list`/`records` for the same array and a dozen aliases per scalar. Every
   alias list carries forward.
4. **Chinese UI copy is the product.** Strings move to resources unchanged, verbatim.
5. **Builds must stay reproducible on this machine**, including the offline path.

---

## 3. Decisions

| Decision | Choice |
|---|---|
| Toolchain | **Modern stable** — Gradle 8.9+, AGP 8.7.x, Kotlin 2.0.x + Compose compiler plugin, Compose 1.7.x / Material3 1.3.x, compileSdk 35, version catalog |
| Version control | **`git init` scoped to `native-android`**, green-baseline commit, then one commit per phase |
| Visual scope | **Redesign layout and visuals freely**; every feature, control, label and data point retained |
| Parity target | **Refactor the existing surface + add native `/login`, `/register`, `/reset-password`**; WebView demoted to a true fallback |
| Navigation | **Keep the `AppRoute` sealed hierarchy, extract a `Navigator`.** Not Navigation-Compose: the sealed routes are already type-safe, the existing back semantics are pinned by tests, and `AnimatedContent` gives transitions without a migration. |
| Serialization | **Keep `org.json` normalizers, restructured.** Not kotlinx.serialization: the alias tolerance is the whole point and a strict-schema rewrite would be a behavioural regression risk across ~90 normalizers for no user-visible gain. |
| DI | **Hand-rolled `AppContainer`.** Not Hilt: avoids adding kapt/KSP to the build for a single-module app. |

---

## 4. Phases

Every phase ends green: unit tests pass, `assembleDebug` and `assembleRelease` succeed, and a
commit is made. Phases are ordered so the safety net exists before anything risky moves.

### Phase 0 — Safety net and green baseline
- [x] Filesystem snapshot → `native-android-baseline-snapshot-20260726`
- [x] Fix B1 minimally (`verticalArrangement` → `verticalAlignment` at the 5 sites) to get a
      trustworthy baseline on the *unchanged* dependency set. Reverted in Phase 1, where the
      upgrade makes the original intent (`Arrangement.spacedBy`) legal.
- [ ] Full unit test suite → record the pass count as the regression floor
- [ ] `git init`, `.gitignore`, commit the green baseline
- [ ] **String golden master**: extract every user-visible literal into a fixture plus a test
      asserting the set is unchanged. This is the mechanism that makes "retain every content
      element" verifiable rather than aspirational, and it must exist before Phase 3 moves
      strings into resources.

**Exit:** compiles, tests green, baseline committed, golden master in place.

### Phase 1 — Toolchain and build hygiene
- Wrapper → official `services.gradle.org` distribution, Gradle 8.9+ (fixes B3)
- AGP 8.7.x, Kotlin 2.0.x + `org.jetbrains.kotlin.plugin.compose`, compileSdk 35
- Compose BOM 1.7.x / Material3 1.3.x; revert the Phase 0 workaround to the intended
  `verticalArrangement = Arrangement.spacedBy(...)`
- `gradle/libs.versions.toml` version catalog; migrate `build.gradle` → `.kts`
- R8 on for release with rules verified against the reflection surface (`org.json`, OkHttp,
  Coil, WebView JS bridge); signing config wired to a properties file
- `allowBackup` addressed: exclude the auth prefs from backup (P3)
- Android Lint baseline + `ktlint`
- Verify: full test suite, `assembleDebug`, `assembleRelease`, APK installs

**Exit:** modern toolchain, release build minified and signed, tests green, warnings resolved.

### Phase 2 — Design system foundation
The highest user-visible return in the plan; fixes D1, D2, D3, D8, D9, D10.
- **Complete colour system**: all Material3 roles specified for light *and* dark, derived from
  the site's indigo `#4F46E5` / blue `#3182ED`. No role left to baseline → no pink leak.
  A test asserts every role is explicitly set.
- **Typography scale**: all 15 M3 roles, line-height and letter-spacing tuned for CJK.
- **Token sets**: spacing, radius, elevation, motion duration/easing, icon sizing.
- `values-night/`, edge-to-edge via `enableEdgeToEdge()`, system bars driven by the Compose
  theme, IME/cutout inset handling.
- **App identity**: adaptive launcher icon (background/foreground/monochrome) + splash via
  `androidx.core.splashscreen`.
- **Component library** (`ui/design/`): `NpScaffold`, `NpCard`, `NpChip` with *semantic*
  variants (fixes D12), `NpButton` hierarchy, `NpSectionHeader`, `NpEmptyState`, `NpErrorState`
  (always retains navigation — fixes D11), `NpSkeleton`, `NpSearchField` (one implementation —
  fixes D7), `NpFactsRow` (wrapping, fixes D5), `NpCoverImage`, `NpSnackbarHost`,
  `NpPullToRefresh`.
- Route transitions via `AnimatedContent`.

**Exit:** tokens + components exist with previews and tests; theme test proves no unspecified
colour role; icon renders on the launcher; screens still work (unmigrated, but now on a sane theme).

### Phase 3 — String externalisation
- Every user-visible literal → `res/values/strings.xml`, verbatim, `\uXXXX` decoded to readable
  Chinese. Plural/format cases use proper resource syntax.
- Presentation-helper tests keep asserting resolved strings, so the golden master stays live.
- Fixes D13 and makes the 4063-line ViewModel and 3654-line screen file readable for Phases 4–6.

**Exit:** no user-visible literal left in Kotlin; golden master unchanged; tests green.

### Phase 4 — Data layer
- Split `NovalPieApi.kt` (3404) into:
  - `data/net/NovalPieHttp.kt` — request plumbing, base URL, headers, proxy selector, timeouts,
    status→error mapping
  - `data/net/ReaderSessionCrypto.kt` — the signed-session + AES-GCM protocol, **moved verbatim**
  - `data/api/*.kt` — one source per domain (search, book, reader, forum, comment, message, user,
    admin, workspace, upload, exam, auth)
  - `data/normalize/*.kt` — the ~90 normalizers, grouped by domain, alias lists intact
- Split `model/Models.kt` (655) per domain.
- Repository interfaces per domain over api + stores; `AppContainer` for construction (fixes A5).
- Replace the 16 request-serial counters with one `latestOnly`/`flatMapLatest` primitive (fixes A4).
- Every existing MockWebServer contract test must pass **unmodified** — that is the proof the
  wire behaviour did not change.

**Exit:** no file over ~400 lines in `data/`; all API contract tests green unmodified.

### Phase 5 — Presentation layer
- Split `NovalPieViewModel.kt` (4063) into per-domain ViewModels over `AppContainer`, plus a
  `SessionViewModel` for auth/proxy/identity (fixes A1, A2).
- Extract `Navigator` owning the `AppRoute` stack, with **one** definition of "root route" that
  the top bar, bottom bar and `BackHandler` all consume (fixes A7).
- Replace per-screen lambda threading with per-screen `UiState` + a single `onEvent(Event)` sink
  (fixes A3).
- Add the ViewModel test coverage that does not exist today (P6), against fake repositories.

**Exit:** no ViewModel over ~400 lines; navigation conditions defined once; ViewModel tests exist.

### Phase 6 — Screen rebuild on the design system
One slice at a time, each ending green and committed, each ticked off against the inventory
contract. Order puts the highest-traffic reading surfaces first.

| Slice | Screens |
|---|---|
| 6.1 | Collection / bookshelf |
| 6.2 | Discover / search (fixes D4, D5, D6, D7) |
| 6.3 | Book detail |
| 6.4 | Reader (+ catalog panel, illustrations, comments; fixes D11) |
| 6.5 | Forum feed, post detail, create |
| 6.6 | Message centre, detail, conversation, settings |
| 6.7 | Profile, settings, user profile detail |
| 6.8 | Tools / function centre |
| 6.9 | Upload + EPUB editor |
| 6.10 | Book edit info + chapter manager |
| 6.11 | Admin (6 sections) |
| 6.12 | Political exam |
| 6.13 | WebView fallback (demoted to true fallback) |
| 6.14 | **New**: native login / register / reset-password |

Each slice: rebuild layout on tokens and shared components, surface content above the fold, add
pull-to-refresh + skeletons + snackbar errors, and keep every string, control and data point.

**Exit:** every inventory line item ticked; all screens on the design system.

### Phase 7 — Bug fixes
Work the ranked findings from the correctness audit (`docs/inventory/07-bugs.md`): stale-response
races, `state.copy()` lost updates, swallowed errors, unchecked JSON casts, pagination desync,
back-stack defects, unvalidated numeric input. Each fix lands with a regression test.

**Exit:** every high and medium finding fixed or explicitly deferred with a reason.

### Phase 8 — Verification hardening
- Compose UI tests per screen asserting the critical content contract
- Screenshot tests for the design system in light and dark
- Rewrite `tools/verify-native-project.ps1` — it currently greps source for string literals that
  this refactor deliberately relocates, so it will report false failures until updated
- Full parity audit: inventory contract vs shipped app

**Exit:** UI tests cover every route; verify script green; parity audit signed off.

### Phase 9 — Release and documentation
- R8-minified signed release APK, installed and smoke-tested
- Rewrite `README.md` (currently a 1361-line turn-by-turn changelog) as an architecture document
- Refresh `docs/LIVE_SITE_ROUTE_API_MATRIX.md`, which is stale: it lists workspace, upload,
  political exam and admin as "still requiring native migration" although all five are native today

**Exit:** signed APK, accurate docs.

---

## 5. Verification

Per phase: unit tests green · `assembleDebug` + `assembleRelease` · string golden master
unchanged · inventory checklist advanced · commit.

Build command on this machine:

```bash
GRADLE_USER_HOME=D:\NovalPie\.gradle-sandbox ./gradlew --console=plain :app:testDebugUnitTest
```

Note: `--offline` stops working from Phase 1, since the upgraded dependencies are not in the
sandbox cache and must download. Network reachability to `dl.google.com`,
`repo.maven.apache.org` and `services.gradle.org` is confirmed.

## 6. Risks

| Risk | Mitigation |
|---|---|
| Reader crypto breaks → chapters unreadable | Move verbatim, never retype; existing tests pin it; treat as a copy operation |
| Feature silently lost in a 20k-LOC restructure | Inventory contract + string golden master + per-slice tick-off |
| Toolchain upgrade cascades into unrelated failures | Phase 1 is isolated and committed separately from any refactor |
| Emulator unavailable for QA — MuMu failed to boot in 4 of the last 6 recorded turns | Rely on unit, Robolectric, Compose and screenshot tests; treat emulator runs as a bonus, never the gate |
| Alias tolerance regressed by "tidying" normalizers | Alias lists are copied, not rewritten; MockWebServer tests must pass unmodified |
| No offline fallback after Phase 1 | Snapshot before the upgrade; sandbox cache left untouched for rollback |
