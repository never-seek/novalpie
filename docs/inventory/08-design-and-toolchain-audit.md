# 08 — UI/UX, Design-System and Toolchain Audit

**Target:** `D:/NovalPie/native-android` (Kotlin + Jetpack Compose, package `com.novalpie.nativeapp`)
**Audit date:** 2026-07-26, against `git HEAD` = `f2cc124` ("Phase 0: add string golden master and refactor plan"), parent `fc1d555` ("Baseline: NovalPie 2.0 native app before refactor").
**Method:** read every file in `app/src/main/res/`, `AndroidManifest.xml`, `MainActivity.kt`, `ui/NovalPieTheme.kt`, all 20 Compose screen files, both Gradle scripts, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `tools/build-release.ps1`; plus mechanical censuses of `.dp` / `.sp` / colour / chip / a11y / inset usage across all 63 `.kt` files. Rendered-result judgement uses `qa-screenshots/turn34..turn40` (see §11 for staleness caveats).

**One-line diagnosis:** the app has *colour tokens and shape tokens only*. There is no typography scale, no spacing scale, no elevation scale, no motion scale, no icon-size scale, no component library, no string resources, no launcher icon, no splash, no dark-mode system bars, no edge-to-edge, no snackbar, no pull-to-refresh, no skeletons, no route transitions, and no CI/lint. Every screen re-derives its own visual language from ~747 hardcoded `.dp` literals and ~1685 inline Chinese string literals, and only 14 of Material 3's ~30 colour roles are branded — the rest still render Material's baseline purple/pink inside a blue-branded app.

---

## Table of contents

1. [Design tokens: what exists, what is missing](#1-design-tokens)
2. [Theme correctness: system bars, dark mode, edge-to-edge](#2-theme-correctness)
3. [App identity: launcher icon, splash, cold start](#3-app-identity)
4. [Localization: the scale of the hardcoded-string problem](#4-localization)
5. [Component consistency: the duplication inventory](#5-component-consistency)
6. [Interaction and feedback gaps](#6-interaction-and-feedback-gaps)
7. [Accessibility](#7-accessibility)
8. [Layout robustness](#8-layout-robustness)
9. [Dependency and toolchain currency](#9-dependency-and-toolchain-currency)
10. [Missing production concerns](#10-missing-production-concerns)
11. [Rendered-result evidence: the qa-screenshots corpus](#11-rendered-result-evidence)
12. [Findings ranked by user-visible impact](#12-findings-ranked-by-user-visible-impact)
13. [Appendix A — full `.dp` value census](#appendix-a--full-dp-value-census)
14. [Appendix B — contrast-ratio computations](#appendix-b--contrast-ratio-computations)
15. [Appendix C — file inventory with sizes](#appendix-c--file-inventory)

---

## 1. Design tokens

### 1.1 The entire design system is one 118-line file

`ui/NovalPieTheme.kt` (118 lines) is the whole design system. It contains:

| Line range | Content |
| --- | --- |
| `NovalPieTheme.kt:14-29` | `internal data class ThemeTokens` — 14 `Long` colour fields |
| `NovalPieTheme.kt:31-47` | `lightThemeTokens()` — 14 light colour values |
| `NovalPieTheme.kt:49-64` | `darkThemeTokens()` — 14 dark colour values |
| `NovalPieTheme.kt:69-101` | `LightColors` / `DarkColors` = `lightColorScheme(...)` / `darkColorScheme(...)` with those 14 roles |
| `NovalPieTheme.kt:103-109` | `NovalPieShapes` = `Shapes(4/6/8/12/16.dp)` |
| `NovalPieTheme.kt:111-118` | `NovalPieTheme { }` = `MaterialTheme(colorScheme = …, shapes = …, content)` |

`MaterialTheme(...)` at `NovalPieTheme.kt:113-117` passes **only** `colorScheme` and `shapes`. **`typography` is not passed**, so the app runs on stock Material 3 baseline typography (Roboto, `displayLarge` 57sp … `labelSmall` 11sp) with no CJK-specific line-height, no weight ramp, and no font family.

### 1.2 Colour tokens — what exists

`lightThemeTokens()` (`NovalPieTheme.kt:31-47`), comment on line 32: *"Mirrors the source site's current blue-gray mobile palette."*

| Role | Light (`NovalPieTheme.kt`) | Dark (`NovalPieTheme.kt`) |
| --- | --- | --- |
| `primary` | `0xFF3182ED` :33 | `0xFF4D9DFF` :50 |
| `onPrimary` | `0xFFFFFFFF` :34 | `0xFFFFFFFF` :51 |
| `primaryContainer` | `0xFFE7F1FF` :35 | `0xFF001D3D` :52 |
| `onPrimaryContainer` | `0xFF146DE1` :36 | `0xFFB8D6FF` :53 |
| `secondary` | `0xFF7D8A97` :37 | `0xFF8E99A4` :54 |
| `secondaryContainer` | `0xFFEDF0F2` :38 | `0xFF2A2F34` :55 |
| `onSecondaryContainer` | `0xFF45525E` :39 | `0xFFDCE0E5` :56 |
| `background` | `0xFFF2F2F2` :40 | `0xFF191C1F` :57 |
| `surface` | `0xFFFFFFFF` :41 | `0xFF23262A` :58 |
| `surfaceVariant` | `0xFFF5F7FA` :42 | `0xFF2A2F34` :59 |
| `onBackground` | `0xFF1F2933` :43 | `0xFFF0F2F5` :60 |
| `onSurface` | `0xFF45525E` :44 | `0xFFF0F2F5` :61 |
| `onSurfaceVariant` | `0xFF7D8A97` :45 | `0xFFB8C1CA` :62 |
| `outline` | `0xFFCED4DA` :46 | `0xFF4A545E` :63 |

That is **14 roles**. Material 3's `ColorScheme` has ~30. Everything else keeps the Material baseline (purple/pink) value.

### 1.3 Colour tokens — the 16 unbranded roles, and the 3 that are actually rendered

Roles never assigned in `lightColorScheme(...)` / `darkColorScheme(...)` (`NovalPieTheme.kt:69-101`), so they resolve to Material 3 baseline:

`onSecondary`, `tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer`, `error`, `onError`, `errorContainer`, `onErrorContainer`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, `surfaceTint`, `outlineVariant`, `scrim`, `onBackground`-adjacent extras.

Three of these are **read by live code**, so Material's purple/pink palette is on screen today:

| Unbranded role | Baseline light value | Baseline dark value | Live call site(s) |
| --- | --- | --- | --- |
| `tertiary` | `#7D5260` (dusty maroon) | `#EFB8C8` (pink) | `MessageScreens.kt:247` — the Message-centre hero gradient is `linearGradient(primary → tertiary)`, i.e. site-blue `#3182ED` → Material maroon `#7D5260`. Nothing in `ThemeTokens` defines this colour. |
| `errorContainer` / `onErrorContainer` | `#F9DEDC` / `#410E0B` | `#8C1D18` / `#F9DEDC` | `ForumCreateScreens.kt:88`, `MessageScreens.kt:121`, `MessageScreens.kt:649`, `UploadScreens.kt:101`, `UploadScreens.kt:392`, `WorkspaceScreens.kt:494` (6 × `errorContainer`, 3 × `onErrorContainer`) |
| `outlineVariant` | `#CAC4D0` (purple-grey) | `#49454F` | 1 use |

Colour-role usage census across all `.kt` (occurrences of `colorScheme.X`):

```
103  onSurfaceVariant     6  onSurface            2  secondary
 43  primary              6  onSecondaryContainer 2  onPrimary
 21  surfaceVariant       6  errorContainer       2  background
 21  surface              3  onErrorContainer     1  tertiary
 19  primaryContainer                             1  outlineVariant
 16  secondaryContainer
 14  error
  9  onPrimaryContainer
```

`onSurfaceVariant` is the most-used colour in the app (103 sites) and it fails WCAG AA at every background it is drawn on — see §7.3 and Appendix B.

### 1.4 Hardcoded colours outside the theme

13 sites bypass the theme entirely with literal ARGB, plus 36 `Color.White`/`Color.Black`:

| File:line | Literal | Purpose |
| --- | --- | --- |
| `ImagePreviewDialog.kt:91` | `Color(0xF20B0D12)` | full-screen scrim (should be `colorScheme.scrim`) |
| `MessageScreens.kt:346` | `Color(0xFFF59E0B)` | starred-message amber |
| `NovalPieApp.kt:3557` | `Color(0xFFF4ECD8)`, `Color(0xFF30271B)`, `Color(0xFF76634B)` | reader "sepia" palette |
| `NovalPieApp.kt:3558` | `Color(0xFF111111)`, `Color(0xFFECECEC)`, `Color(0xFFAAAAAA)` | reader "dark" palette |
| `UploadEditorScreens.kt:221` | `Color(0xFF111827)`, `Color(0xFF3730A3)`, `Color(0xFF7C3AED)` | editor hero gradient (Tailwind slate-900/indigo-800/violet-600) |
| `UploadScreens.kt:193` | `Color(0xFF7C3AED)`, `Color(0xFFDB2777)` | upload hero gradient (violet-600/pink-600) |
| `UploadScreens.kt:376,378,380,381` | `Color(0xFFDCFCE7)`, `Color(0xFF15803D)`, `Color(0xFF166534)` | "upload success" green (Tailwind green-100/700/800) |
| `WorkspaceScreens.kt:125` | `Color(0xFF0F172A)` | workspace hero gradient |
| `WorkspaceScreens.kt:485-486` | `Color(0xFFDCFCE7)`, `Color(0xFF166534)` | duplicate of the same "good" green |

`Color.White` × 34, `Color.Black` × 2. There is no semantic `success` / `warning` / `info` token, so "success green" is re-typed as raw Tailwind hex in two files (`UploadScreens.kt:376`, `WorkspaceScreens.kt:485`) and the four gradient heroes each invent their own palette.

### 1.5 Shape tokens exist but are never used

`NovalPieShapes` is defined at `NovalPieTheme.kt:103-109`:
```
extraSmall = 4.dp, small = 6.dp, medium = 8.dp, large = 12.dp, extraLarge = 16.dp
```
**`MaterialTheme.shapes` is referenced 0 times in the entire codebase.** Instead there are **97 inline `RoundedCornerShape(...)` calls** across 15 distinct radii:

```
17 × RoundedCornerShape(16.dp)      6 × 14.dp       1 × 30.dp
14 × RoundedCornerShape(18.dp)      4 × 6.dp        1 × 28.dp
13 × RoundedCornerShape(8.dp)       3 × 999.dp      1 × 15.dp
12 × RoundedCornerShape(12.dp)      3 × 24.dp       1 × 10.dp
 9 × RoundedCornerShape(4.dp)       2 × 22.dp
 9 × RoundedCornerShape(20.dp)
```
plus 4 × `CircleShape`. Note `RoundedCornerShape(999.dp)` × 3 (e.g. `ProfileScreens.kt:310`) is the "make it a pill" hack instead of `CircleShape`. Radii 4, 6, 8, 10, 12, 14, 15, 16, 18, 20, 22, 24, 28, 30 all coexist; there is no rule about which container gets which.

### 1.6 MISSING tokens — the complete list

| Token family | Status | Evidence |
| --- | --- | --- |
| **Typography scale** | **absent.** `MaterialTheme(...)` never receives `typography` (`NovalPieTheme.kt:113-117`). No `Typography(...)` object anywhere (0 hits for `Typography(` construction). No `FontFamily` (only `FontFamily.Monospace` at `AdminScreens.kt:591`). No CJK line-height tuning. | Consequence: 11 of the 15 baseline M3 styles are used, ad hoc: `bodySmall` 100×, `titleMedium` 47×, `labelSmall` 41×, `titleLarge` 26×, `bodyMedium` 22×, `labelMedium` 21×, `headlineSmall` 15×, `titleSmall` 10×, `labelLarge` 10×, `headlineMedium` 5×, `bodyLarge` 3×, `displaySmall` 1× |
| **Weight scale** | **absent.** `fontWeight` is applied inline 195 times: `Bold` × 157, `SemiBold` × 31, `ExtraBold` × 4, `Medium` × 2, `Normal` × 1. `FontWeight.Bold` on top of `titleMedium` (which is already `Medium` 500) appears 45 times, on `titleLarge` 21×, on `headlineSmall` 15×, on `titleSmall` 8×. | The type ramp is effectively "everything is bold". |
| **Spacing scale** | **absent.** 747 `.dp` literals (see §1.7). `Arrangement.spacedBy` uses **11 distinct values**: 8(×122), 10(×61), 6(×35), 12(×28), 4(×15), 14(×15), 2(×8), 3(×7), 7(×5), 5(×5), 16(×2). `.padding(n.dp)` uses **8 distinct values**: 14(×46), 16(×41), 12(×24), 18(×9), 10(×5), 20(×4), 22(×2), 13(×1). |
| **Elevation scale** | **absent.** 1 explicit elevation in the whole app (`NovalPieApp.kt:729`, `elevatedCardElevation(defaultElevation = if (item.pinned) 4.dp else 1.dp)`). 103 `ElevatedCard(` and 56 `Surface(` calls all take default elevation. `tonalElevation` appears 3× and **two of them are dead** — see §2.6. |
| **Motion / duration / easing** | **absent.** 0 hits for `tween`, `spring(`, `snap`, `keyframes`, `AnimationSpec`, `Crossfade`, `updateTransition`, `animate*AsState`. The only animation in the app is 2 × `AnimatedVisibility` with default-spec `slideInVertically`/`slideOutVertically` for the reader bars (`NovalPieApp.kt:1885-1897`, `NovalPieApp.kt:1916-1933`). |
| **Icon-size scale** | **absent.** Icon sizes are inline and inconsistent: `20.dp` (`NovalPieApp.kt:992`), `18.dp` (`NovalPieApp.kt:2082`, `NovalPieApp.kt:3241`, `UploadEditorScreens.kt:250`), `24.dp` (`NovalPieApp.kt:2849`), `26.dp` (`UploadScreens.kt:236`), `32.dp` (`MessageScreens.kt:660`), `36.dp` (`MessageScreens.kt:342`), `42.dp` (`NovalPieApp.kt:738-739`), `76.dp` (`ProfileScreens.kt:276`). Two of them use `.width(18.dp).height(18.dp)` instead of `.size(18.dp)`. |
| **Semantic colours** (`success`/`warning`/`info`) | **absent.** Hand-rolled twice with the same hex — `UploadScreens.kt:376` and `WorkspaceScreens.kt:485`. |
| **Opacity/alpha scale** | **absent.** `.copy(alpha = …)` uses 0.14, 0.16, 0.17, 0.38, 0.45, 0.72, 0.78, 0.8, 0.82, 0.9 — 10 distinct values, several within 0.02 of each other (`WorkspaceScreens.kt:149` 0.14 vs `MessageScreens.kt:279` 0.16; `WorkspaceScreens.kt:152` 0.78 vs `MessageScreens.kt:282` 0.8). |
| **Breakpoints / window size class** | **absent.** 0 hits for `WindowSizeClass`, `BoxWithConstraints`, `LocalConfiguration`, `calculateWindowSizeClass`. Grid column count is a hardcoded constant: `internal fun novelGridColumnCount(): Int = 2` (`NovalPieApp.kt:3414`). |
| **`values-night/` resources** | **absent.** `app/src/main/res/` contains only `values/strings.xml`, `values/styles.xml`, `xml/backup_rules.xml`, `xml/data_extraction_rules.xml`. |
| **`dimens.xml` / `colors.xml` / `themes.xml`** | **absent.** |

### 1.7 Quantification: hardcoded `.dp` and `.sp` literals

**747 `.dp` literals** across 14 files. **0 `.sp` literals** used as design values (the only `.sp` conversions are the user-controlled reader font size, `NovalPieApp.kt:3477-3478`).

| Count | File |
| --- | --- |
| **271** | `ui/NovalPieApp.kt` |
| 80 | `ui/MessageScreens.kt` |
| 64 | `ui/UploadEditorScreens.kt` |
| 53 | `ui/UploadScreens.kt` |
| 50 | `ui/WorkspaceScreens.kt` |
| 48 | `ui/PoliticalExamScreens.kt` |
| 44 | `ui/AdminScreens.kt` |
| 39 | `ui/ProfileScreens.kt` |
| 25 | `ui/ForumCreateScreens.kt` |
| 23 | `ui/BookEditScreens.kt` |
| 20 | `ui/BookChapterScreens.kt` |
| 16 | `ui/UserProfileScreens.kt` |
| 9 | `ui/ImagePreviewDialog.kt` |
| 5 | `ui/NovalPieTheme.kt` (the `Shapes` definition — the only legitimate ones) |
| **747** | **total** |

**46 distinct `.dp` values** are in use. The top 8 (8, 16, 12, 10, 14, 6, 4, 18) account for 598 of 747 (80%) — but they include both 4-multiples and non-4-multiples (`6`, `10`, `14`, `18`, `22`, `26`, `42`, `54`, `86`), so there is no consistent base unit. Full census in [Appendix A](#appendix-a--full-dp-value-census).

### 1.8 Typography: text scaling is accidentally OK, everything else is not

Because no `fontSize` is ever hardcoded and the app uses `MaterialTheme.typography.*` (which is `sp`-based), **system font-scale accessibility works by accident**. There is no `TextUnit`/`dp`-based text sizing anywhere, so no clipping-on-large-font risk from fixed text sizes. The risks come from *container* sizes instead — see §8.

Two typography defects that *are* live:
- `NovalPieApp.kt:2213`: `Text("${options.fontSizeSp}sp", …)` renders literally `18sp` in the reader toolbar.
- `NovalPieApp.kt:2561`: `Text("字号: ${readerOptions.fontSizeSp}sp")` renders `字号: 18sp` in the profile reader card. `ProfilePresentation.kt:69` produces the same string.
  Both leak the developer unit `sp` into Chinese product copy.

---

## 2. Theme correctness

### 2.1 `res/values/styles.xml` in full (9 lines, the entire XML theme)

```xml
<resources>
    <style name="Theme.NovalPie" parent="@android:style/Theme.Material.NoActionBar">
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
        <item name="android:statusBarColor">#FFF8F4</item>
        <item name="android:navigationBarColor">#FFF8F4</item>
        <item name="android:fontFamily">sans</item>
    </style>
</resources>
```

### 2.2 Defect: three different near-whites stack at the top of every screen

| Layer | Colour | Source |
| --- | --- | --- |
| System status bar | `#FFF8F4` — **warm cream**, hue ≈ 24° | `styles.xml:5` |
| `CenterAlignedTopAppBar` container | `#FFFFFF` — pure white | `NovalPieApp.kt:145` `containerColor = MaterialTheme.colorScheme.surface`, and `surface = 0xFFFFFFFF` (`NovalPieTheme.kt:41`) |
| Screen content | `#F2F2F2` — **neutral grey** | `NovalPieApp.kt:180` `color = MaterialTheme.colorScheme.background`, and `background = 0xFFF2F2F2` (`NovalPieTheme.kt:40`) |

So the top of the window is a cream band → white band → grey band. `#FFF8F4` is warm (R>G>B) while `#F2F2F2` is perfectly neutral; the seam is visible as a colour-temperature shift, not just a brightness step. This is visible in **every** screenshot in `qa-screenshots/` (e.g. `turn39/home_after_fix.png`, `turn38/book_detail_wrapped.png`, `turn36/search_initial_after_proxy_fix.png`, `turn39/reader.png`). Same at the bottom: the system navigation bar is `#FFF8F4` cream while the Compose `NavigationBar` above it is `#FFFFFF` (`NovalPieApp.kt:154`).

`#FFF8F4` appears **nowhere** in `ThemeTokens` — it is an orphan colour with no owner.

### 2.3 Defect: the XML parent theme is the **dark** platform theme → dark cold-start flash

`styles.xml:2` inherits `@android:style/Theme.Material.NoActionBar`. `android:Theme.Material` is the **dark** platform Material theme (`android:Theme.Material.Light` is the light one). Consequences:

- `android:windowBackground` resolves to the dark-material window background (≈ `#303030`), so the **first frame of every cold start is a dark-grey rectangle** before Compose composes and paints `#F2F2F2`. Combined with §3 (no splash screen), the launch experience is: default Android robot icon → dark grey flash → grey app.
- `android:colorBackground`, `android:textColorPrimary` etc. are all dark-theme values. These leak into any platform-drawn surface — notably the `WebView` in `WebFallbackScreen.kt` before `setBackgroundColor(...)` runs (`WebFallbackScreen.kt:41`), and any platform dialog/`Toast`/autofill UI.
- `android:windowLightStatusBar=true` (`styles.xml:3`) — dark icons — is set unconditionally against a dark parent theme, which is internally contradictory config.

### 2.4 Defect: no dark-mode system-bar handling at all

`NovalPieTheme.kt:114` switches the Compose `colorScheme` on `isSystemInDarkTheme()`. Nothing switches the system bars. There is **no `values-night/styles.xml`**, and **no runtime system-bar control** — the following are all absent from the codebase (0 hits):

`WindowCompat`, `WindowInsetsControllerCompat`, `enableEdgeToEdge`, `SideEffect { window.statusBarColor = … }`, `rememberSystemUiController`, `LocalView`, `isAppearanceLightStatusBars`.

Therefore in **dark mode**:
- Status bar stays `#FFF8F4` cream with `windowLightStatusBar=true` (dark icons) above a `#191C1F` near-black app background (`NovalPieTheme.kt:57`). Contrast between the two bands is ~**14:1** — a bright cream strip across the top of a dark app.
- Navigation bar stays `#FFF8F4` cream with `windowLightNavigationBar=true` below a `#23262A` `NavigationBar` (`NovalPieApp.kt:154` → `surface` = `0xFF23262A`, `NovalPieTheme.kt:58`).

This is the single most obvious "the app looks broken" defect in dark mode.

### 2.5 Defect: no edge-to-edge, and `targetSdk 35` will force it

- `MainActivity.kt:11-21` is 11 lines: `super.onCreate` → `configureNovalPieImageLoader` → `setContent { NovalPieTheme { NovalPieApp(startUri) } }`. No `WindowCompat.setDecorFitsSystemWindows(window, false)`, no `enableEdgeToEdge()`, no `installSplashScreen()`.
- 0 hits across all `.kt` for `WindowInsets`, `imePadding`, `navigationBarsPadding`, `statusBarsPadding`, `systemBarsPadding`, `safeDrawing`, `displayCutout`, `consumeWindowInsets`.
- The manifest has no `android:windowSoftInputMode`, and `styles.xml` has no `android:windowSoftInputMode` — so the default `adjustResize`-ish behaviour applies, but **nothing in Compose reacts to the IME**. Every text field in the app (`OutlinedTextField` × dozens, incl. the comment composers at `NovalPieApp.kt:1039-1076` / `NovalPieApp.kt:1077-1112`, the EPUB editor body at `UploadEditorScreens.kt`, the reply box at `MessageScreens.kt:492`) can be covered by the soft keyboard with no way to scroll it into view.
- No `displayCutout` handling: on notched/punch-hole devices the top app bar draws under the cutout region only insofar as the platform inset applies; because the theme opts *in* to decor-fitting there is no cutout bleed today, but there is also no `LayoutInDisplayCutoutMode` policy.

**Upgrade blast radius:** `targetSdk 35` (Android 15) makes edge-to-edge mandatory — `Window.setDecorFitsSystemWindows` is deprecated and the opt-out (`android:windowOptOutEdgeToEdgeEnforcement`) is a temporary escape hatch only. Because the app has *zero* inset handling, bumping `targetSdk` from 34 → 35 will immediately put content under the status bar and behind the navigation bar. See §9.

### 2.6 Defect: two dead `tonalElevation` values on the reader bars

`NovalPieApp.kt:2008-2012` (`ReaderTopBar`) and `NovalPieApp.kt:2199-2201` (`ReaderToolbar`) both do:
```kotlin
Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 6.dp
)
```
Material 3's `Surface` only applies `surfaceColorAtElevation(...)` when `color == colorScheme.surface`. With `color = surfaceVariant` the `tonalElevation = 6.dp` has **no visual effect**, so the reader bars have no tonal separation from the reading area. In light mode `surfaceVariant` is `#F5F7FA` — see §2.7, the bars are ~1.04:1 against the page.

### 2.7 Defect: cards are invisible against the page in **both** themes

| Pair | Light | Dark |
| --- | --- | --- |
| `surfaceVariant` on `background` | `#F5F7FA` on `#F2F2F2` → **1.04 : 1** | `#2A2F34` on `#191C1F` → **1.27 : 1** |
| `surface` on `background` | `#FFFFFF` on `#F2F2F2` → **1.06 : 1** | `#23262A` on `#191C1F` → **1.13 : 1** |
| `outline` on `surface` (border visibility) | `#CED4DA` on `#FFFFFF` → **1.49 : 1** | `#4A545E` on `#23262A` → **1.97 : 1** |

WCAG 2.1 SC 1.4.11 (non-text contrast) requires **3:1** for boundaries of UI components that convey meaning. Every card boundary, every `OutlinedTextField` border, every `OutlinedButton` border in this app is below that in both themes. Because there is also no elevation scale (§1.6), cards have neither a colour boundary nor a shadow boundary. This is why the screenshots read as "flat undifferentiated blocks of grey". Full arithmetic in [Appendix B](#appendix-b--contrast-ratio-computations).

### 2.8 Defect: `android:fontFamily="sans"` is dead config

`styles.xml:7` sets `android:fontFamily` on the platform theme. Compose does **not** read platform theme attributes for typography; `MaterialTheme.typography` (baseline, `FontFamily.Default`) wins for 100% of the UI. The only consumer would be the `WebView` (`WebFallbackScreen.kt:39`) chrome, and `"sans"` is not a valid named family alias for `android:fontFamily` (the valid ones are `sans-serif`, `serif`, `monospace`, …), so it is likely ignored there too.

### 2.9 `values-night/`: absent

Confirmed by full `res/` walk:
```
app/src/main/res/values/strings.xml
app/src/main/res/values/styles.xml
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
```
No `values-night/`, no `values-v27/`, no `values-sw600dp/`, no `values-zh/`, no `values-en/`.

### 2.10 Defect: the reader is a *card*, not a reading surface — dark/sepia themes are framed in grey

`NovalPieApp.kt:3460-3463`: `ReaderBody` wraps the chapter text in an `ElevatedCard(containerColor = palette.background)` where `palette` comes from `readerPalette(options.theme)` (`NovalPieApp.kt:3555-3560`):
- `"sepia"` → `#F4ECD8` background
- `"dark"` → `#111111` background
- else → `colorScheme.surface`

The card sits inside `ReaderScreen`'s `LazyColumn` with `contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 48.dp)` (`NovalPieApp.kt:1857`), inside the global `Surface(color = colorScheme.background)` (`NovalPieApp.kt:176-181`). So selecting the reader's **dark** theme in **light** system mode gives a `#111111` reading card floating on a `#F2F2F2` grey page with a 16 dp grey gutter and rounded corners — the opposite of an immersive reader. The reader palette and the app theme are two independent, unreconciled colour systems.

---

## 3. App identity

### 3.1 There is NO launcher icon. None.

`AndroidManifest.xml` in full (29 lines):
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:allowBackup="true"
        android:enableOnBackInvokedCallback="true"
        android:label="@string/app_name"
        android:theme="@style/Theme.NovalPie">
        <activity
            android:name="com.novalpie.nativeapp.MainActivity"
            android:exported="true"
            android:screenOrientation="unspecified">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="novalpie" android:host="app" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- **No `android:icon`.** No `android:roundIcon`. No `android:banner`.
- **No `mipmap-*` directory, no `drawable*` directory, no `ic_launcher*` file anywhere in the project** (verified: `find app -iname "*mipmap*" -o -iname "*drawable*" -o -iname "ic_launcher*"` excluding `build/` returns nothing).
- No adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml` with `<adaptive-icon>`), no monochrome layer for Android 13 themed icons.

**What the app looks like on the launcher today:** the platform falls back to the default system application icon (`android.R.drawable.sym_def_app_icon` — the generic grey/green Android robot placeholder), labelled `NovalPie 2.0` (`strings.xml:2`). There is no brand mark on the home screen, in the app switcher, in Settings → Apps, in share sheets, or in notification small-icons. On Android 13+ themed-icon launchers it cannot participate in theming at all.

This is the single highest-visibility identity defect: the user's first and most frequent contact with the app has no design.

### 3.2 There is NO splash screen

- `androidx.core:core-splashscreen` is **not** a dependency (`app/build.gradle:56-73` — full dep list in §9.2). 0 hits for `splashscreen`, `SplashScreen`, `installSplashScreen`.
- `styles.xml` defines no `Theme.NovalPie.Splash`, no `windowSplashScreenBackground`, no `windowSplashScreenAnimatedIcon`, no `postSplashScreenTheme`.
- `MainActivity.onCreate` (`MainActivity.kt:12-21`) does not call `installSplashScreen()`.

On Android 12+ the platform *always* shows a splash. With no icon and no `windowSplashScreenBackground`, it uses the default app icon (the robot placeholder, §3.1) on `android:windowBackground` — which resolves to the **dark** Material window background (§2.3). So the launch sequence is: robot icon on dark grey → dark grey → grey app. On Android ≤11 there is no splash at all, just the dark-grey `windowBackground` flash.

### 3.3 `versionName` / branding strings

`app/build.gradle:16-21`: `applicationId 'com.novalpie.app'`, `versionCode 2026070601`, `versionName '2.0.0-native-alpha1'`.
`strings.xml:2`: `<string name="app_name">NovalPie 2.0</string>` — the launcher label carries a version number, which is unusual for a shipping app.
`NovalPieApp.kt:133`: the top app bar hardcodes `Text("NovalPie", …)` rather than reading `app_name`, so the label and the in-app wordmark can drift.
`UploadScreens.kt:201`: a badge reads `NOVALPIE STUDIO` — a second, undocumented sub-brand appearing on one screen only.

---

## 4. Localization

### 4.1 There is exactly ONE string resource

`res/values/strings.xml` in full:
```xml
<resources>
    <string name="app_name">NovalPie 2.0</string>
</resources>
```

### 4.2 Scale of the problem: 1685 inline CJK string literals, 1253 distinct

Measured by tokenising every Kotlin string literal in `app/src/main/java/com/novalpie/nativeapp/**/*.kt`, decoding `\uXXXX` escapes, and testing for CJK/CJK-punctuation code points:

- **1685** string literals contain Chinese text.
- **1253** distinct decoded values (so ~432 are duplicated across files — the same label re-typed).
- **316** of the 1685 are written in `\uXXXX`-escaped form rather than as readable UTF-8.
- **1437** individual `\uXXXX` escape sequences total.

Per-file breakdown:

| CJK literals | of which `\uXXXX`-escaped | File |
| --- | --- | --- |
| 258 | 44 | `ui/NovalPieViewModel.kt` |
| 192 | 20 | `ui/NovalPieApp.kt` |
| 133 | 0 | `ui/AdminScreens.kt` |
| 121 | 0 | `ui/ProductCopy.kt` |
| 90 | **90 (100%)** | `ui/WorkspaceScreens.kt` |
| 84 | **84 (100%)** | `ui/MessageScreens.kt` |
| 77 | 0 | `ui/UploadEditorScreens.kt` |
| 65 | 0 | `ui/BookChapterScreens.kt` |
| 65 | 0 | `ui/BookEditScreens.kt` |
| 56 | 0 | `ui/UploadScreens.kt` |
| 46 | 0 | `ui/ProfileScreens.kt` |
| 42 | 0 | `ui/ProfilePresentation.kt` |
| 41 | 0 | `ui/DiscoverPresentation.kt` |
| 38 | 0 | `ui/PoliticalExamScreens.kt` |
| 35 | 0 | `ui/ForumCreateScreens.kt` |
| 34 | **34 (100%)** | `ui/ToolsPresentation.kt` |
| 27 | **27 (100%)** | `ui/UiNavigation.kt` |
| 26 | 0 | `ui/UserProfileScreens.kt` |
| 23 | 1 | `data/NovalPieApi.kt` |
| 21 | 0 | `ui/ForumCreatePresentation.kt` |
| 20 | 0 | `ui/ReaderPresentation.kt` |
| 19 | 0 | `ui/EditorPresentation.kt` |
| 16 | 0 | `ui/ForumPresentation.kt` |
| 15 | 0 | `ui/PoliticalExamPresentation.kt` |
| 14 | 0 | `ui/VisibleUiLabels.kt` |
| 13 | **13 (100%)** | `ui/WorkspacePresentation.kt` |
| 12 | 0 | `ui/LibraryPresentation.kt` |
| 11 | 0 | `ui/BookDetailPresentation.kt` |
| 11 | 0 | `data/EditorProcessor.kt` |
| 9 | 0 | `ui/NovelCardFacts.kt` |
| 9 | 0 | `ui/BookChapterPresentation.kt` |
| 9 | 0 | `ui/BookDetailFacts.kt` |
| 8 | 0 | `ui/ImagePreviewDialog.kt` |
| 8 | 0 | `data/EditorArchiveStore.kt` |
| 7 | 0 | `data/EpubParser.kt` |
| 5 | 0 | `ui/BookEditPresentation.kt` |
| 4 | 0 | `ui/CatalogSummary.kt` |
| 4 | 0 | `ui/UploadPresentation.kt` |
| 3 | 0 | `ui/ReaderProgressLabel.kt` |
| 3 | **3 (100%)** | `ui/MessagePresentation.kt` |
| 3 | 0 | `ui/ApiMessages.kt` |
| 3 | 0 | `data/EpubWriter.kt` |
| 2 | 0 | `ui/ErrorRecovery.kt` |
| 2 | 0 | `model/Models.kt` |
| 1 | 0 | `ui/ReaderText.kt` |
| **1685** | **316** | **45 files** |

### 4.3 The `\uXXXX` files are unreadable and unreviewable

Eight files store their entire Chinese UI copy as escapes. `ui/WorkspaceScreens.kt` and `ui/MessageScreens.kt` are 100% escaped. Examples decoded verbatim:

| Source | Escaped as written | Decoded |
| --- | --- | --- |
| `UiNavigation.kt:4` | `"\u6536\u85cf"` | `收藏` |
| `UiNavigation.kt:5` | `"\u641c\u7d22"` | `搜索` |
| `UiNavigation.kt:6` | `"\u5de5\u5177"` | `工具` |
| `UiNavigation.kt:7` | `"\u8bba\u575b"` | `论坛` |
| `UiNavigation.kt:8` | `"\u6211\u7684"` | `我的` |
| `UiNavigation.kt:12-16` | `"\u6536"`,`"\u641c"`,`"\u5de5"`,`"\u8bba"`,`"\u6211"` | `收`,`搜`,`工`,`论`,`我` |
| `UiNavigation.kt:20` | `"\u6d88\u606f\u4e2d\u5fc3"` | `消息中心` |
| `UiNavigation.kt:21` | `"\u6d88\u606f\u8be6\u60c5"` | `消息详情` |
| `UiNavigation.kt:22` | `"\u79c1\u4fe1"` | `私信` |
| `UiNavigation.kt:23` | `"\u6d88\u606f\u8bbe\u7f6e"` | `消息设置` |
| `UiNavigation.kt:24` | `"\u5de5\u4f5c\u533a"` | `工作区` |
| `UiNavigation.kt:25` | `"\u4e0a\u4f20\u4e66\u7c4d"` | `上传书籍` |
| `UiNavigation.kt:26` | `"EPUB \u7f16\u8f91\u5668"` | `EPUB 编辑器` |
| `UiNavigation.kt:27` | `"\u53d1\u5e03\u5e16\u5b50"` | `发布帖子` |
| `UiNavigation.kt:28` | `"\u5e16\u5b50\u8be6\u60c5"` | `帖子详情` |
| `UiNavigation.kt:29` | `"\u4e66\u7c4d\u8be6\u60c5"` | `书籍详情` |
| `UiNavigation.kt:30` | `"\u7f16\u8f91\u4e66\u7c4d\u4fe1\u606f"` | `编辑书籍信息` |
| `UiNavigation.kt:31` | `"\u7ae0\u8282\u7ba1\u7406"` | `章节管理` |
| `UiNavigation.kt:32` | `"\u8ffd\u52a0\u7ae0\u8282"` | `追加章节` |
| `UiNavigation.kt:33` | `"\u9605\u8bfb"` | `阅读` |
| `UiNavigation.kt:34` | `"\u5e94\u7528\u8bbe\u7f6e"` | `应用设置` |
| `UiNavigation.kt:35` | `"\u7528\u6237\u4e3b\u9875"` | `用户主页` |
| `UiNavigation.kt:36` | `"\u7ba1\u7406\u540e\u53f0"` | `管理后台` |
| `MessageScreens.kt:255` | `"\u6211\u7684\u6d88\u606f"` | `我的消息` |
| `MessageScreens.kt:256` | `"\u901a\u77e5\u3001\u56de\u590d\u4e0e\u79c1\u4fe1"` | `通知、回复与私信` |
| `MessageScreens.kt:259` | `"\u5237\u65b0"` | `刷新` |
| `MessageScreens.kt:260` | `"\u6d88\u606f\u8bbe\u7f6e"` | `消息设置` |
| `MessageScreens.kt:281-284` | `"\u672a\u8bfb"`,`"\u5168\u90e8"`,`"\u91cd\u8981"`,`"\u661f\u6807"` | `未读`,`全部`,`重要`,`星标` |
| `MessageScreens.kt:432` | `"\u6807\u8bb0\u5df2\u8bfb"` | `标记已读` |
| `MessageScreens.kt:433` | `"\u53d6\u6d88\u661f\u6807"` / `"\u6dfb\u52a0\u661f\u6807"` | `取消星标` / `添加星标` |
| `MessageScreens.kt:434` | `"\u6253\u5f00\u79c1\u4fe1"` | `打开私信` |
| `MessageScreens.kt:436` | `"\u5220\u9664"` | `删除` |
| `MessageScreens.kt:535` | `"\u6d88\u606f\u8bbe\u7f6e"` | `消息设置` |
| `WorkspaceScreens.kt:131` | `"\u5de5\u4f5c\u533a"` | `工作区` |
| `WorkspaceScreens.kt:132` | `"\u7ba1\u7406 API\u3001Cookie \u4e0e\u7ffb\u8bd1\u4efb\u52a1"` | `管理 API、Cookie 与翻译任务` |
| `WorkspaceScreens.kt:150-153` | `"\u5065\u5eb7"`, `"\u4efb\u52a1"` | `健康`, `任务` |
| `WorkspaceScreens.kt:208` | `"\u6253\u5f00"` | `打开` |
| `WorkspaceScreens.kt:257`,`367` | `"\u6dfb\u52a0"` | `添加` |
| `WorkspaceScreens.kt:324` | `"\u7f16\u8f91"` / `"\u5220\u9664"` | `编辑` / `删除` |
| `WorkspaceScreens.kt:424` | `"\u7ee7\u7eed"` | `继续` |
| `WorkspaceScreens.kt:425` | `"\u6682\u505c"` | `暂停` |
| `WorkspaceScreens.kt:447` | `"\u5171\u4eab\u5230\u670d\u52a1\u5668"` / `"\u5173\u95ed\u65f6\u53ea\u4fdd\u5b58\u5728\u672c\u673a"` | `共享到服务器` / `关闭时只保存在本机` |
| `WorkspaceScreens.kt:469` | `"\u542f\u7528\u6b64\u914d\u7f6e"` | `启用此配置` |
| `WorkspaceScreens.kt:480` | `"\u5220\u9664"` / `"\u53d6\u6d88"` | `删除` / `取消` |
| `WorkspaceScreens.kt:494` | `"\u91cd\u8bd5"` | `重试` |
| `NovalPieApp.kt:2248` | `"\u529f\u80fd\u4e2d\u5fc3"` | `功能中心` |
| `NovalPieApp.kt:2342-2346` | `"\u672a\u8bfb …"`, `"\u5168\u90e8 …"`, `"\u91cd\u8981 …"`, `"7\u65e5 …"`, `"\u661f\u6807 …"` | `未读 N`, `全部 N`, `重要 N`, `7日 N`, `星标 N` |
| `MessageScreens.kt:660` context | `"\u8fd8\u6ca1\u6709\u7ae0\u8282\u8bc4\u8bba"` (in `NovalPieApp`) | `还没有章节评论` |

`README.md:86` records that *"Visible Chinese product copy has been restored from UTF-8 mojibake"* — the escapes are the scar tissue from an earlier encoding accident. They are still there, they defeat `grep`-by-Chinese-text, and they make copy review impossible without a decoder.

### 4.4 The same label is retyped many times

Because there is no string catalogue, identical labels are independently defined:

| Label | Sites |
| --- | --- |
| `重试` | `WorkspaceScreens.kt:494`, `MessageScreens.kt:655`, plus `ErrorRecovery.kt:3` `retryActionLabel(surface)` used by 19 `ErrorBlock(` call sites |
| `刷新` | `NovalPieApp.kt:2669`, `MessageScreens.kt:259`, `MessageScreens.kt:463`, `WorkspaceScreens.kt:134`, `NovalPieApp.kt:1481`(cd=null), `NovalPieApp.kt:2256`, `NovalPieApp.kt:1483` |
| `删除` | `BookChapterScreens.kt:225`, `MessageScreens.kt:436`, `UploadEditorScreens.kt:540`, `UploadScreens.kt:261`, `WorkspaceScreens.kt:324`, `WorkspaceScreens.kt:480` |
| `消息设置` | `UiNavigation.kt:23`, `MessageScreens.kt:260`, `MessageScreens.kt:535` |
| `工作区` | `UiNavigation.kt:24`, `WorkspaceScreens.kt:131` |
| `章节管理` | `UiNavigation.kt:31`, `BookChapterScreens.kt:144`, `NovalPieApp.kt:3211` |
| `编辑书籍信息` | `UiNavigation.kt:30`, `BookEditScreens.kt:117` |
| `发布帖子` | `UiNavigation.kt:27`, `ForumCreateScreens.kt:77` |
| `管理后台` | `UiNavigation.kt:36`, `AdminScreens.kt:136` |
| `继续阅读` | `BookDetailPresentation.kt:7`, `LibraryPresentation.kt:23`, `LibraryPresentation.kt:25`, `NovalPieApp.kt:3202` |
| `打开网页评论` | `BookDetailPresentation.kt:24`, `ReaderPresentation.kt:44` |

### 4.5 Partial centralisation exists but is incomplete and inconsistently named

There *is* a copy layer — `ui/ProductCopy.kt` (220 lines, 121 CJK literals), `ui/VisibleUiLabels.kt` (30 lines), `ui/UiNavigation.kt` (38 lines), plus 20 `*Presentation.kt` files that return label strings and are unit-tested (`app/src/test/.../ProductCopyTest.kt`, `VisibleUiLabelsTest.kt`, `UiNavigationTest.kt`, …). But:

- It covers a minority of copy: 121 + 14 + 27 = 162 of 1685 literals (≈10%).
- The screen files still inline the rest (`AdminScreens.kt` 133, `UploadEditorScreens.kt` 77, `BookChapterScreens.kt` 65, `BookEditScreens.kt` 65, `UploadScreens.kt` 56, …).
- Even the centralised layer duplicates itself: `bookCommentsFallbackLabel()` (`BookDetailPresentation.kt:24`) and `chapterCommentsFallbackLabel()` (`ReaderPresentation.kt:44`) both return `"打开网页评论"`.
- The `ViewModel` holds 258 CJK literals (`ui/NovalPieViewModel.kt`) — user-facing error and status text is generated in the state layer, so it cannot be resource-ified without touching the ViewModel.
- `data/NovalPieApi.kt` holds 23 CJK literals — user-visible network error copy is produced in the data layer.

### 4.6 Localization landmine: a Chinese UI string is used as a control-flow key

`NovalPieApp.kt:1670-1682` dispatches search filter callbacks by matching the group's **display label**:
```kotlin
discoverFilterGroups(options).forEach { group ->
    FilterChoiceRail(
        group = group,
        onSelected = when (group.label) {
            "排序" -> onSortByChange
            "顺序" -> onSortOrderChange
            "范围" -> onScopeChange
            "内容" -> onAdultFilterChange
            "字数" -> onWordCountRangeChange
            "来源" -> onSourceChange
            else  -> onMatchTypeChange
        }
    )
}
```
Moving `"排序"` etc. into `strings.xml` (or translating them) silently routes **every** filter to `onMatchTypeChange` via the `else` branch. Same class of coupling: `AdminScreens.kt:842` `adminSectionLabel(section)`, `UserProfileScreens.kt:238/244` label maps.

### 4.7 Developer language leaking into product copy

| File:line | Rendered text |
| --- | --- |
| `NovalPieApp.kt:2626` | `正在检查 /api/users/me` |
| `NovalPieApp.kt:3056` | `正在检查 /api/favorites/status` |
| `NovalPieApp.kt:3060` | `分组 id: {n}` |
| `NovalPieApp.kt:3061` | `原始状态: {raw}` |
| `NovalPieApp.kt:2213` | `18sp` |
| `NovalPieApp.kt:2561` | `字号: 18sp` |
| `ProfilePresentation.kt:69` | `字号 18sp` |
| `DiscoverPresentation.kt:43-46` | badge showing `就绪` / `加载中` / `错误` — the raw `LoadResult` state name is a user-facing chip (`NovalPieApp.kt:1579-1586`) |
| `NovalPieApp.kt:2925-2933` | `搜索状态: 就绪` (dead code, see §5.8) |
| `AdminScreens.kt:560`, `WorkspaceScreens.kt:132` | `BaseURL 规则`, `管理 API、Cookie 与翻译任务` — untranslated technical nouns in user UI |
| `UploadScreens.kt:201` | `NOVALPIE STUDIO` |

`docs/APP2_NATIVE_DESIGN_REFERENCES.md:38` explicitly forbids this: *"Do not add: Debug/API labels in visible product UI."* Four sites violate it.

### 4.8 12 user-visible "idle" strings

The `LoadResult.Idle` branch is rendered as plain text in 28 `StatusText(` call sites. Distinct copy:

`等待 EPUB`, `等待加载书架`, `等待加载书籍详情`, `等待加载分组`, `等待加载正文`, `等待加载目录`, `等待加载章节`, `等待加载章节评论`, `等待加载评论`, `等待同步账号`, `等待检查登录状态`, `等待读取收藏状态`

Plus 40+ distinct `正在…` loading strings (`正在加载 BaseURL 规则`, `正在加载 Cookie 配置`, `正在加载 Key`, `正在加载书籍信息`, `正在加载书籍详情`, `正在加载商品`, `正在加载审核设置`, `正在加载审核请求`, `正在加载插图`, `正在加载操作日志`, `正在加载收藏书籍`, `正在加载收藏分组`, `正在加载更多`, `正在加载正文`, `正在加载用户作品`, `正在加载用户动态`, `正在加载用户资料`, `正在加载目录`, `正在加载章节`, `正在加载章节插图`, `正在加载章节正文`, `正在加载章节正文…`, `正在加载章节目录`, `正在加载签到统计`, `正在加载签到记录`, `正在加载管理总览`, `正在加载调度日志`, `正在同步个人资料`, `正在同步章节评论`, `正在上传原始封面…`, `正在上传原始章节插图…`, `正在上传头像…`, `正在保存书籍信息…`, `正在保存存档…`, `正在保存章节…`, `正在保存章节顺序…`, `正在保存读写门槛…`, `正在保存资料…`, `正在创建考试会话…`, `正在删除章节插图…`). Note the inconsistent ellipsis: some end `…`, most do not.

---

## 5. Component consistency

**Verdict: everything is ad hoc.** There is no `ui/components/` package. The only two shared composables in the whole app are `LoadingBlock` and `ErrorBlock` (both `internal`, declared inside the 3654-line `NovalPieApp.kt`), and both are shadowed by per-screen re-implementations.

Container census across all `.kt`: `ElevatedCard(` × **103**, plain Material 3 `Card(` × **0**, `OutlinedCard(` × **0**, `Surface(` × **56**, `Box(` × 21. So the app uses exactly one of Material 3's three card variants and hand-rolls the rest out of `Surface`. Chip census: `FilterChip(` × 26, `AssistChip(` × 11 (7 of them no-op, §7.5), `SuggestionChip(` × 0, `InputChip(` × 0, `ElevatedFilterChip(` × 0.

### 5.1 Pill / chip / badge: **9 hand-rolled implementations**

All nine are `Surface { Text(padding) }`; none share code; all use different radii, padding, colours, typography.

| # | Composable | File:line | Shape | Colour | Padding | Typography |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `NovelTagPill` | `NovalPieApp.kt:3016-3031` | `RoundedCornerShape(4.dp)` | `primaryContainer`/`onPrimaryContainer` | `h6/v2` | `labelSmall` |
| 2 | `NovelSourcePill` | `NovalPieApp.kt:3033-3048` | `RoundedCornerShape(4.dp)` | `secondaryContainer`/`onSecondaryContainer` | `h6/v2` | `labelSmall` |
| 3 | `BookDetailFavoriteChip` | `NovalPieApp.kt:3151-3166` | `RoundedCornerShape(4.dp)` | `primaryContainer`/`onPrimaryContainer` | `h8/v4` | `labelSmall` + `SemiBold` |
| 4 | `BookDetailFactLabel` | `NovalPieApp.kt:3169-3178` | `RoundedCornerShape(4.dp)` | `secondaryContainer`/`onSecondaryContainer` | `h8/v4` | `labelSmall` |
| 5 | `CompactForumBadge` | `NovalPieApp.kt:800-816` | `RoundedCornerShape(16.dp)` | `primaryContainer`/`onPrimaryContainer` | `h8/v4` | `labelSmall` + `SemiBold` |
| 6 | `LibraryStatPill` | `NovalPieApp.kt:3605-3620` | `RoundedCornerShape(4.dp)` | `primaryContainer`/`onPrimaryContainer` | `h8/v4` | `labelMedium` + `SemiBold` |
| 7 | `ProfileFactPill` | `ProfileScreens.kt:310-320` | `RoundedCornerShape(999.dp)` | `primaryContainer`/`onPrimaryContainer` | `h12/v7` | `labelMedium` |
| 8 | `WorkspaceStatusChip` | `WorkspaceScreens.kt:484-488` | `RoundedCornerShape(20.dp)` | `Color(0xFFDCFCE7)`/`Color(0xFF166534)` **hardcoded** or `surfaceVariant` | `h10/v5` | `labelMedium` |
| 9 | *inline status badge* | `NovalPieApp.kt:1579-1586` | `RoundedCornerShape(4.dp)` | `secondaryContainer` | `h8/v4` | `labelSmall` + `SemiBold` |

Pairs 1↔3↔6, and 2↔4↔9, are semantically the same component with three different paddings/type styles. `LibraryStatPill` (#6) is named for the library screen but is called from Tools message stats (`NovalPieApp.kt:2342-2346`), Profile (`NovalPieApp.kt:2512`, `:2542`) and Discover (`NovalPieApp.kt:1611`) — 8 call sites in 4 unrelated features.

Meanwhile `AssistChip(onClick = {})` is used **7 times** as a decorative non-interactive badge — a 10th chip idiom, and an a11y bug (see §7.5): `MessageScreens.kt:407`, `NovalPieApp.kt:3333`, `NovalPieApp.kt:3341`, `UploadScreens.kt:210`, `UploadScreens.kt:211`, `UploadScreens.kt:344`, `WorkspaceScreens.kt:328`.

So **10 distinct ways to draw a small labelled pill**, and on the book-detail screen three of them are visible simultaneously (`BookDetailFavoriteChip` + `BookDetailFactLabel` + `AssistChip`) — confirmed in `qa-screenshots/turn38/book_detail_wrapped.png`, where 未收藏 / 作者 / 来源 / 收藏 / 本站阅读 / 源收藏 / 已完结 / 奇幻 all render as visually identical outlined pills with no hierarchy between "favorite state", "metric" and "genre tag".

### 5.2 Loading block: **7 implementations**

| # | Composable / site | File:line | Form |
| --- | --- | --- | --- |
| 1 | `LoadingBlock(message)` — the "shared" one | `NovalPieApp.kt:3568-3574` | `Column { LinearProgressIndicator(fillMaxWidth); StatusText(msg) }`, 27 call sites |
| 2 | `MessageLoading(label)` | `MessageScreens.kt:640-645` | `Column(padding v20) { LinearProgressIndicator; Text(onSurfaceVariant) }` — same thing, different padding + colour |
| 3 | `WorkspaceLoading(label)` | `WorkspaceScreens.kt:491` | `Column(padding v18) { LinearProgressIndicator; Text(onSurfaceVariant) }` — same thing, `18.dp` instead of `20.dp` |
| 4 | `ProfileLoadingCard()` | `ProfileScreens.kt:438-446` | `CircularProgressIndicator(size 24, stroke 2) + Text` |
| 5 | bare `LinearProgressIndicator` | `MessageScreens.kt:270`, `NovalPieApp.kt:1488`, `NovalPieApp.kt:2949`, `PoliticalExamScreens.kt:142`, `PoliticalExamScreens.kt:240`, `UploadScreens.kt:137`, `UploadScreens.kt:348`, `UploadScreens.kt:374`, `UploadEditorScreens.kt:159` | no label at all (9 sites) |
| 6 | inline `CircularProgressIndicator` | `ProfileScreens.kt:299` (size 24, stroke 2), `ProfileScreens.kt:333` (size 20, stroke 2), `ImagePreviewDialog.kt:121` (white) | 3 sites, 3 sizes |
| 7 | `LinearProgressIndicator(progress = …)` determinate | `WorkspaceScreens.kt:422`, `PoliticalExamScreens.kt:232` | the only determinate progress in the app |

**No skeleton loaders anywhere.** The loading state is always a 4 dp indeterminate bar plus a sentence of Chinese text. `qa-screenshots/turn39/launch.png` shows the result: the home screen on cold start reads `正在加载收藏分组` and `正在加载收藏书籍` as bare left-aligned body text on a grey page, with no layout reservation, so content jumps when data arrives.

The only layout-reserving load in the app is the book cover: `BookCover` (`NovalPieApp.kt:3351-3398`) uses `SubcomposeAsyncImage` with `loading = { BookCoverFallbackText(fallbackText) }` and `error = { … }`, showing the title's first character in the cover slot (`NovalPieApp.kt:3409-3410`).

### 5.3 Error block: **5 implementations**

| # | Composable | File:line | Container | Body |
| --- | --- | --- | --- | --- |
| 1 | `ErrorBlock(message, retryLabel, onRetry)` | `NovalPieApp.kt:3576-3595` | `ElevatedCard(containerColor = surfaceVariant)` — **not** `errorContainer` | `bodyMedium` + `OutlinedButton` |
| 2 | `MessageError(message, onRetry)` | `MessageScreens.kt:648-655` | `ElevatedCard(containerColor = errorContainer)` | `onErrorContainer` + `OutlinedButton("重试")` |
| 3 | `WorkspaceError(message, onRetry?)` | `WorkspaceScreens.kt:494` | `ElevatedCard(containerColor = errorContainer)` | default colour + `OutlinedButton("重试")` |
| 4 | `ProfileErrorCard(...)` | `ProfileScreens.kt:449` | — | — |
| 5 | `UploadNotice(message, isError)` | `UploadScreens.kt:390-400` | `Surface(errorContainer or secondaryContainer, RoundedCornerShape(16.dp))` | no retry |

`ErrorBlock` (the shared one, 19 call sites) is the **only** one that does *not* use the error colour role — errors rendered through it look identical to a neutral card. The other four do use `errorContainer`, which is unbranded Material pink `#F9DEDC` (§1.3). So the app has two mutually inconsistent error looks, one of which is off-brand.

Plus 3 more notice/banner variants: `MessageNotice` (`MessageScreens.kt:668`), `WorkspaceNotice` (`WorkspaceScreens.kt:500`), `AdminStatusCard` (`AdminScreens.kt:796`).

### 5.4 Empty state: **5 implementations, zero illustrations**

| Composable | File:line | Content |
| --- | --- | --- |
| `EmptyCollectionState` | `NovalPieApp.kt:3622-3641` | `ElevatedCard` + `暂无收藏` title + `登录后同步网页收藏，或先打开网页确认账号状态。` + 2 buttons |
| `DiscoverEmptyResultPanel` | `NovalPieApp.kt:1535-1548` | `ElevatedCard` + `没有匹配结果` + `可以换一个关键词，或调整范围、匹配方式和内容筛选。` |
| `MessageEmpty(label)` | `MessageScreens.kt:657-663` | `Column(v28)` + 32 dp `Icons.Filled.Mail` (`contentDescription = null`) + label |
| `WorkspaceEmpty(label)` | `WorkspaceScreens.kt:497` | `Surface(surfaceVariant α0.45, radius 16)` + text only |
| bare `StatusText` | `NovalPieApp.kt:1295` `没有匹配的收藏`, `:1983` `没有匹配的章节`, `:2806` `暂无分组`, `:1500` `暂无可显示标签` | plain left-aligned text |

`MessageEmpty` is the *only* empty state with a graphic, and it is a 32 dp system icon. **There are no illustrations, no vector assets, no `drawable/` directory** (§3.1) — so an illustrated empty state is not even possible today.

### 5.5 Filter rail (label + horizontal chip list): **4 implementations**

| Composable | File:line | Label typography |
| --- | --- | --- |
| `FilterChoiceRail(group, onSelected)` | `NovalPieApp.kt:1690-1706` | `titleSmall` + `Bold` |
| `ChoiceChips(label, selected, choices, onSelected)` | `NovalPieApp.kt:1710-1725` | `labelLarge` |
| `MessageFilterRail<T>(label, options, selected, onSelected)` | `MessageScreens.kt:288-306` | `labelMedium` + `onSurfaceVariant` |
| `AdminStringFilterRail(label, options, selected, onSelected)` | `AdminScreens.kt:803-822` | `labelMedium` + `onSurfaceVariant` |

All four are structurally `Column(spacedBy(6.dp)) { Text(label); LazyRow(spacedBy(8.dp)) { items { FilterChip(...) } } }`. `MessageFilterRail` and `AdminStringFilterRail` are byte-for-byte equivalent except for the generic type parameter. `ChoiceChips` and `FilterChoiceRail` sit in the **same file, 4 lines apart**, and differ only in label style and input shape.

### 5.6 Hero header: **4 gradient implementations, 4 different palettes**

| Composable | File:line | Gradient | Radius | Padding |
| --- | --- | --- | --- | --- |
| `MessageHero` | `MessageScreens.kt:242-252` | `linearGradient(primary, tertiary)` — `#3182ED` → **baseline Material `#7D5260`** | `clip(24.dp)` | `20.dp` |
| `UploadHero` | `UploadScreens.kt:187-198` | `linearGradient(primary, Color(0xFF7C3AED), Color(0xFFDB2777))` | `shape = 24.dp` on `background` | `22.dp` |
| `WorkspaceHero` | `WorkspaceScreens.kt:120-127` | `linearGradient(Color(0xFF0F172A), primary)` | `clip(24.dp)` | `20.dp` |
| *editor hero* | `UploadEditorScreens.kt:218-223` | `horizontalGradient(Color(0xFF111827), Color(0xFF3730A3), Color(0xFF7C3AED))` | **no radius at all** | `h18/v16` |

All four write white text over an unvalidated gradient (`Color.White` × 34, mostly here). None of the four gradients is defined in `NovalPieTheme.kt`. `UploadScreens.kt:212-216` compounds it: an `OutlinedButton` on the gradient with `Icon(tint = default = primary blue)` next to `Text(color = Color.White)` — icon and label are different colours in the same button, and the button's outline uses `outline` `#CED4DA` against a violet gradient.

Meanwhile the *non*-gradient hero pattern also exists twice:
- `HeroCard(title, subtitle, semanticsMarker)` — `NovalPieApp.kt:2903-2921`, `ElevatedCard(surface)` + `headlineSmall Bold` + `bodyMedium`
- `ProductHeaderBlock(header)` — `NovalPieApp.kt:2891-2901`, bare `Column` + `headlineMedium Bold` + `bodyMedium onSurfaceVariant` — **dead code** (§5.8)

### 5.7 Stat cell: **4 implementations, two of them near-identical**

| Composable | File:line | Body |
| --- | --- | --- |
| `HeroStat(label, value)` | `MessageScreens.kt:278-286` | `Surface(White α0.16, radius 14) { Column(h14/v10) { Text(value, White, Bold); Text(label, White α0.8, labelSmall) } }` |
| `WorkspaceHeroStat(label, value)` | `WorkspaceScreens.kt:147-154` | `Surface(White α0.14, radius 14) { Column(h14/v10) { Text(value, White, Bold); Text(label, White α0.78, labelSmall) } }` |
| `ForumStat(label, value)` | `NovalPieApp.kt:707-713` | `Column(centered, spacedBy 2) { Text(value, titleMedium Bold); Text(label, labelSmall onSurfaceVariant) }` |
| `LibraryMetricCell(modifier, label, value)` | `NovalPieApp.kt:2723-2734` | `Column(spacedBy 2) { Text(label, labelSmall onSurfaceVariant); Text(value, titleLarge SemiBold onSurface) } }` — **label above value**, inverting the other three |
| `AdminMetricCard(label, value, modifier)` | `AdminScreens.kt:780-788` | `ElevatedCard { Column(12, centered) { Text(value, titleLarge Bold); Text(label, labelSmall) } }` |

`HeroStat` vs `WorkspaceHeroStat` differ **only** in `α 0.16 vs 0.14` and `α 0.8 vs 0.78` — a copy-paste with drifted alpha. `LibraryMetricCell` puts the label first, so the same "metric" concept reads top-down in three screens and bottom-up in one.

### 5.8 Section header: 5 typography patterns, no component

There is no `SectionHeader` composable. Headers are inline `Text(..., style, fontWeight)`:

| Pattern | Occurrences |
| --- | --- |
| `typography.titleMedium` + `FontWeight.Bold` | **45** |
| `typography.titleLarge` + `FontWeight.Bold` | **21** |
| `typography.headlineSmall` + `FontWeight.Bold` | **15** |
| `typography.titleSmall` + `FontWeight.Bold` | **8** |
| `typography.labelLarge` / `labelMedium` bare | 10 / 21 |

And the header **row** pattern (title left, action right) is re-typed inline: `Arrangement.SpaceBetween` appears **63 times** across 11 files — `NovalPieApp.kt` ×22, `WorkspaceScreens.kt` ×12, `AdminScreens.kt` ×7, `MessageScreens.kt` ×6, `ForumCreateScreens.kt` ×3, `PoliticalExamScreens.kt` ×3, `UploadEditorScreens.kt` ×3, `BookEditScreens.kt` ×2, `UploadScreens.kt` ×2, `UserProfileScreens.kt` ×2, `ProfileScreens.kt` ×1. There is no `SectionHeaderRow` composable.

Also: **8 screens print their own screen title even though the global `CenterAlignedTopAppBar` already shows the same route label** (`NovalPieApp.kt:130-148` renders `NovalPie` + `routeContextLabel(route, tab)`):

| Screen title (in body) | File:line | Duplicate of `routeContextLabel` value |
| --- | --- | --- |
| `管理后台` | `AdminScreens.kt:136` | `UiNavigation.kt:36` |
| `章节管理` | `BookChapterScreens.kt:144` | `UiNavigation.kt:31` |
| `编辑书籍信息` | `BookEditScreens.kt:117` | `UiNavigation.kt:30` |
| `发布帖子` | `ForumCreateScreens.kt:77` | `UiNavigation.kt:27` |
| `我的消息` | `MessageScreens.kt:255` | `消息中心`, `UiNavigation.kt:20` |
| `消息设置` | `MessageScreens.kt:535` | `UiNavigation.kt:23` |
| `工作区` | `WorkspaceScreens.kt:131` | `UiNavigation.kt:24` |
| `个人中心` | `ProfileScreens.kt:122` | `我的`, `UiNavigation.kt:8` |

So most secondary screens show the title twice, ~24 dp apart, in two different type styles. Visible in `qa-screenshots/turn36/search_initial_after_proxy_fix.png`: top bar reads `NovalPie` / `搜索` and 100 px below it the body reads `发现` / `搜索作品、作者和标签`.

### 5.9 Section container: two competing idioms in the same screen

Within the Discover/Search screen alone:
- `Surface(fillMaxWidth, shape = RoundedCornerShape(8.dp), color = surface)` — `SearchHistorySection` `NovalPieApp.kt:1623-1627`, `SearchOptionSection` `NovalPieApp.kt:1659-1663`, `SearchTagSection` `NovalPieApp.kt:1468-1472`, `LibraryShelfControls` `NovalPieApp.kt:2745-2749`, `LibraryOverviewBlock`'s inner strip `NovalPieApp.kt:2701-2705`
- `ElevatedCard(fillMaxWidth)` — `DiscoverIdlePanel` `NovalPieApp.kt:1516`, `DiscoverEmptyResultPanel` `NovalPieApp.kt:1537`, `GroupSection` `NovalPieApp.kt:2797`, `BookCommentsSection` `NovalPieApp.kt:3232`

Global: 103 `ElevatedCard(` vs 56 `Surface(`. Two container systems, chosen per-composable with no rule.

### 5.10 Dead composables (declared, never called)

Confirmed by full-project reference count (declaration is the only hit):

| Composable | File:line | Lines |
| --- | --- | --- |
| `ReaderHeader(state, chapters)` | `NovalPieApp.kt:2040-2052` | 13 |
| `GroupSection(groups, selectedGroupId, onGroupSelected)` | `NovalPieApp.kt:2792-2830` | 39 |
| `FavoriteStatusCard(status)` | `NovalPieApp.kt:3050-3067` | 18 |
| `SearchResultHeader(results)` | `NovalPieApp.kt:2925-2933` | 9 |
| `ProductHeaderBlock(header)` | `NovalPieApp.kt:2891-2901` | 11 |

90 lines of unreachable UI, including the only two composables that would emit `搜索状态: 就绪` and `分组 id:` debug copy. `GroupSection` is a near-duplicate of the live `LibraryShelfControls` group chips (`NovalPieApp.kt:2760-2777`).

---

## 6. Interaction and feedback gaps

| Capability | Status | Evidence |
| --- | --- | --- |
| **`SnackbarHost` / `Snackbar`** | **absent — 0 hits** for `SnackbarHost`, `SnackbarHostState`, `Snackbar`. The `Scaffold` at `NovalPieApp.kt:127-175` passes only `topBar` and `bottomBar`. | Every transient result is instead an inline `Text` or a card that pushes layout. E.g. `ForumCommentComposer` renders `message` inline (`NovalPieApp.kt:1039-1076`), `UploadNotice` (`UploadScreens.kt:390`), `MessageNotice` (`MessageScreens.kt:668`), `WorkspaceNotice` (`WorkspaceScreens.kt:500`), `AdminStatusCard` (`AdminScreens.kt:796`). |
| **`Toast`** | absent — 0 hits. | — |
| **Pull-to-refresh** | **absent — 0 hits** for `pullRefresh`, `PullRefresh`, `PullToRefreshBox`, `SwipeRefresh`, `rememberPullToRefreshState`. Also *unavailable*: `material3 1.1.0` has no `PullToRefreshBox` (added 1.3.0) and `androidx.compose.material:material` (which has `Modifier.pullRefresh` at 1.4) is not a dependency — only `material-icons-extended` is. | Refresh is a manual `IconButton(Icons.Filled.Refresh)`: `NovalPieApp.kt:2668-2670` (library), `NovalPieApp.kt:1480-1483` (tags), `NovalPieApp.kt:2255-2258` (tools), `MessageScreens.kt:259`, `MessageScreens.kt:463`, `WorkspaceScreens.kt:134`. On the Forum, Book Detail, Reader and Search screens there is **no refresh affordance at all**. |
| **Skeleton loaders** | **absent.** See §5.2 — the load state is a `LinearProgressIndicator` + a Chinese sentence, 27 + 9 sites. | `qa-screenshots/turn39/launch.png` shows bare text `正在加载收藏分组` / `正在加载收藏书籍`. |
| **Route transition animations** | **absent.** Navigation is a `when (route)` block over a sealed `AppRoute` (`NovalPieApp.kt:182-580`) with **no `AnimatedContent`, no `Crossfade`, no `NavHost`**. `androidx.navigation:navigation-compose` is not a dependency. | Route changes are instant hard cuts, including the tab bar (`viewModel.openTab(tab)`, `NovalPieApp.kt:160`). |
| **Any animation** | 2 instances, both in the reader. `AnimatedVisibility(slideInVertically/slideOutVertically)` at `NovalPieApp.kt:1885-1897` (top bar) and `NovalPieApp.kt:1916-1933` (toolbar). Both use the default spec — no duration/easing token. | The reader **catalog panel** at `NovalPieApp.kt:1899-1914` is a bare `if (catalogVisible.value) { … }` — it pops in with no animation, unlike the two bars it sits between. Inconsistent within one screen. |
| **Ripple / press states** | Mostly default. **1 site deliberately removes it**: `NovalPieApp.kt:1853-1856`, the reader's full-screen tap-to-toggle-chrome uses `clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)` — correct there. | **Unclipped ripples:** `NovelCardItem` (`NovalPieApp.kt:2963-2966`) is a bare `Column(fillMaxWidth().clickable(onClick))` with **no `Card`/`Surface`/`clip`** — the ripple paints as a hard rectangle across the whole grid cell including the rounded cover. Same at `NovalPieApp.kt:2354` (`Modifier.fillMaxWidth().clickable`), `NovalPieApp.kt:2677`, `NovalPieApp.kt:3419` (`ChapterRow`), `MessageScreens.kt:317`, `PoliticalExamScreens.kt:310`, `UserProfileScreens.kt:165`, `ForumCreateScreens.kt:104`. Only `ForumActionIcon` (`NovalPieApp.kt:981-983`) correctly does `.clip(RoundedCornerShape(16.dp)).clickable(...)`. |
| **Haptics** | **absent — 0 hits** for `HapticFeedback`, `LocalHapticFeedback`, `performHapticFeedback`. | No haptic on long-press despite 3 `combinedClickable(onLongClick = …)` sites (`NovalPieApp.kt:3367-3370` cover, `NovalPieApp.kt:3508` illustration, `ImagePreviewDialog.kt`) — long-press fires with no confirmation. |
| **Empty-state illustrations** | **absent.** No `drawable/`, no vector assets (§3.1). | §5.4. |
| **Determinate progress** | 2 sites only: `WorkspaceScreens.kt:422` (translation job), `PoliticalExamScreens.kt:232` (exam). Uploads use indeterminate bars (`UploadScreens.kt:137`, `UploadEditorScreens.kt:159`) despite `UploadFileSource`/streaming existing. |
| **Confirmation dialogs** | inconsistent. `AlertDialog` is used at `WorkspaceScreens.kt:480` and `MessageScreens.kt` (`confirmDelete`), `BookEditScreens.kt:461` shows a warning as inline red text instead. |
| **Focus / keyboard** | partial. `LocalFocusManager` + `clearFocus()` + `KeyboardActions(onSearch)` only in `DiscoverSearchPanel` (`NovalPieApp.kt:1559-1563`, `:1603-1604`). No other text field sets `imeAction`, `keyboardActions`, or dismisses the keyboard. No `imePadding()` anywhere (§2.5). |
| **Scroll position preservation** | `rememberLazyListState()` in the reader (`NovalPieApp.kt:1848`) only; every other `LazyColumn`/`LazyRow` (59 `LazyRow` + ~20 `LazyColumn`) uses the implicit default state, which is destroyed on route change because there is no `NavHost` back-stack. Returning from a book detail to a scrolled search result list resets to the top. |
| **WebView UX** | bare. `WebFallbackScreen.kt:36-58` is an `AndroidView { WebView }` with **no progress bar, no error page, no in-WebView back handling, no title, no reload, no insets**. On a slow load the user sees a blank rectangle. |

---

## 7. Accessibility

### 7.1 `contentDescription` coverage

Census across all `.kt`:
- `Icon(` call sites: **86**
- `contentDescription` occurrences: **40**
- `contentDescription = null`: **13**
- positional `Icon(Icons.X.Y, null, …)`: **19**
- `IconButton(` call sites: **25**

So **32 of 86 icons (37%) are explicitly unlabelled** and, subtracting the 40 labelled ones, ~14 more `Icon(` calls appear inside `Button`/`OutlinedButton` where the adjacent `Text` provides the label (legitimate). Net: **~32 unlabelled icons**, of which the following are **not** decorative — they carry meaning that is now invisible to TalkBack:

| File:line | Icon | Why it matters |
| --- | --- | --- |
| `NovalPieApp.kt:1481` | `Icons.Filled.Refresh`, `cd = null` | inside a `TextButton` whose label is `刷新` — acceptable |
| `NovalPieApp.kt:1666` | `Icons.Filled.Tune`, `cd = null` | section icon, label `筛选` follows — acceptable |
| `NovalPieApp.kt:2256` | `Icons.Filled.Refresh`, `cd = null` | inside labelled button — acceptable |
| `NovalPieApp.kt:2289` | `Icons.Filled.Forum`, `cd = null` | decorative |
| **`NovalPieApp.kt:2405`** | **`toolEntryIcon(entry.path)`, `cd = null`** | **the only visual differentiator between the ~12 Tools route cards; TalkBack announces only the text** |
| `NovalPieApp.kt:2686` | `Icons.Filled.Search`, `cd = null` | inside the tappable "search" row (`:2677 clickable`) — the row has no label either, so the whole affordance is announced by its child text only |
| `NovalPieApp.kt:2752` | `Icons.Filled.Tune`, `cd = null` | decorative |
| **`NovalPieApp.kt:2847`** | **`Icons.Filled.MenuBook`, `cd = null`** | continue-reading card icon |
| `MessageScreens.kt:145` | `Icons.Filled.Search`, `cd = null` | `leadingIcon` of a text field — acceptable |
| **`MessageScreens.kt:660`** | **`Icons.Filled.Mail`, `cd = null`** | the empty-state graphic |
| `UploadScreens.kt:172`, `:214`, `:236` | `UploadFile` / `Edit` / `AutoStories`, `cd = null` | 236 is a standalone 26 dp decorative icon |
| **`UploadScreens.kt:378`** | **`Icons.Filled.CheckCircle`, positional `null`** | **the "upload succeeded" success indicator** |
| **`UploadEditorScreens.kt:279`, `:323`, `:519`** | `FindReplace` / `AutoFixHigh` / `Archive`, positional `null` | section-identifying icons |
| **`WorkspaceScreens.kt:323`, `:389`** | `Key` / `Security`, positional `null` | distinguish "API key" rows from "security config" rows |
| **`WorkspaceScreens.kt:424`, `:425`** | `PlayArrow` / `Pause`, positional `null` | inside buttons labelled `继续` / `暂停` — acceptable |
| `MessageScreens.kt:432`, `:433`, `:434`, `:436` | `Check`/`Star`/`Mail`/`Delete`, positional `null` | inside labelled buttons — acceptable |
| `BookChapterScreens.kt:150`, `UploadEditorScreens.kt:453`, `:540`, `WorkspaceScreens.kt:208`, `:257`, `:367`, `UploadScreens.kt:255`, `:261` | `Add`/`Delete`/`Upload`, positional `null` | inside labelled buttons — acceptable |

**Genuinely missing, meaning-bearing labels: 9** (`NovalPieApp.kt:2405`, `:2847`, `:2686`+`:2677`, `MessageScreens.kt:660`, `UploadScreens.kt:378`, `UploadEditorScreens.kt:279`/`:323`/`:519`, `WorkspaceScreens.kt:323`/`:389`).

`IconButton` labelling is good: 24 of 25 have a `contentDescription`. The one gap is that several are `\uXXXX`-escaped so they cannot be reviewed as copy (`MessageScreens.kt:259` `刷新`, `:260` `消息设置`, `:463` `刷新`, `WorkspaceScreens.kt:134` `刷新`, `:324` `编辑`/`删除`).

### 7.2 Touch targets below 48 dp

| File:line | Component | Computed size | Notes |
| --- | --- | --- | --- |
| **`MessageScreens.kt:342`** | `IconButton(modifier = Modifier.size(36.dp))` | **36 × 36 dp** | explicit override below the 48 dp `IconButton` default; this is the star/unstar toggle on every inbox row |
| **`NovalPieApp.kt:975-1000`** | `ForumActionIcon` × 5 per post | 20 dp icon + `vertical = 6.dp` × 2 = **32 dp tall** | `Row(.clip(16.dp).clickable().padding(h8/v6))`, no `minimumInteractiveComponentSize`. Five of them in a `SpaceBetween` row (`NovalPieApp.kt:963-972`) — 赞/踩/表情/打赏/网页 |
| **`NovalPieApp.kt:770-774`** | `Text(forumFeedMetaLine(item), modifier = Modifier.clickable(...))` | `bodySmall` line box ≈ **16–20 dp tall** | opens the author profile; nested inside the card's own `clickable` (`NovalPieApp.kt:724`) |
| **`NovalPieApp.kt:927-932`** | `Text(..., Modifier.clickable(enabled = authorId != null))` | ≈ 20 dp | post-detail author link |
| **`NovalPieApp.kt:1178-1182`** | `Text(..., Modifier.clickable(...))` | ≈ 16 dp (`labelSmall`/`bodySmall`) | comment author link |
| `UserProfileScreens.kt:165` | `.clickable` on an activity row | row height depends on content | may be < 48 dp for single-line entries |
| `NovalPieApp.kt:2164-2180` | `ChapterCommentActionRow` | same 32 dp pattern as `ForumActionIcon` | |

0 hits for `minimumInteractiveComponentSize`, `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`, or `LocalMinimumInteractiveComponentEnforcement`.

Note: `FilterChip` / `AssistChip` / `Button` / `TextButton` all satisfy 48 dp via Material defaults, so the chip rails are fine.

### 7.3 Contrast failures (WCAG 2.1 AA)

Computed from the actual token values — full arithmetic in [Appendix B](#appendix-b--contrast-ratio-computations).

| Pair | Ratio | AA text (4.5:1) | AA large (3:1) | Where |
| --- | --- | --- | --- | --- |
| **light `onSurfaceVariant` `#7D8A97` on `surface` `#FFFFFF`** | **3.53 : 1** | ❌ | ✅ | **103 call sites** — nearly every caption, metric, subtitle, timestamp, `bodySmall`, `labelSmall` in the app |
| **light `onSurfaceVariant` on `background` `#F2F2F2`** | **3.15 : 1** | ❌ | ✅ | same role, on the page background |
| **light `onSurfaceVariant` on `surfaceVariant` `#F5F7FA`** | **3.29 : 1** | ❌ | ✅ | same role, inside `ErrorBlock` (`NovalPieApp.kt:3583`) and the reader bars |
| **light `onPrimary` `#FFFFFF` on `primary` `#3182ED`** | **3.79 : 1** | ❌ | ✅ | **every filled `Button` label in the app** (e.g. `搜索`, `继续阅读`, `发送`) |
| **light `onPrimaryContainer` `#146DE1` on `primaryContainer` `#E7F1FF`** | **4.32 : 1** | ❌ (marginal) | ✅ | 6 of the 9 pill implementations (§5.1), 9 call sites |
| **light `primary` `#3182ED` as text on `surface` `#FFFFFF`** | **3.79 : 1** | ❌ | ✅ | every `TextButton` / `OutlinedButton` label (`清空`, `返回`, `网页详情`, `编辑信息`, …) |
| **dark `onPrimary` `#FFFFFF` on `primary` `#4D9DFF`** | **2.77 : 1** | ❌ | ❌ | **fails even the 3:1 large-text/UI floor.** `darkThemeTokens().onPrimary = 0xFFFFFFFF` (`NovalPieTheme.kt:51`) is wrong: a light-blue dark-mode primary needs *dark* on-colour. All filled buttons are unreadable in dark mode. |
| **light `surfaceVariant` boundary vs `background`** | **1.04 : 1** | — | ❌ SC 1.4.11 | every card edge |
| **dark `surfaceVariant` vs `background`** | **1.27 : 1** | — | ❌ SC 1.4.11 | every card edge |
| **light `outline` `#CED4DA` on `surface`** | **1.49 : 1** | — | ❌ SC 1.4.11 | every `OutlinedTextField` / `OutlinedButton` border |
| **dark `outline` `#4A545E` on `surface`** | **1.97 : 1** | — | ❌ SC 1.4.11 | ditto |
| light `onSurface` `#45525E` on `surface` | 8.00 : 1 | ✅ | ✅ | body text — the one thing that passes comfortably |
| light `onSecondaryContainer` `#45525E` on `secondaryContainer` `#EDF0F2` | 6.99 : 1 | ✅ | ✅ | ok |
| dark `onSurface` `#F0F2F5` on `surface` `#23262A` | 13.5 : 1 | ✅ | ✅ | ok (arguably harsh) |
| dark `onPrimaryContainer` `#B8D6FF` on `primaryContainer` `#001D3D` | 11.3 : 1 | ✅ | ✅ | ok |

`ThemePaletteTest.kt:9-30` locks these exact values (`assertEquals(0xFF3182ED, palette.primary)` etc.) — the failing contrast pairs are **asserted** by the test suite, so fixing them requires updating the test.

### 7.4 `semantics` usage: 3 sites, all test hooks

- `NovalPieApp.kt:613`: `.semantics { contentDescription = "NOVALPIE_NATIVE_COMPOSE_HOME" }` — a QA marker string exposed as a `contentDescription`, i.e. TalkBack will read `NOVALPIE_NATIVE_COMPOSE_HOME` aloud on the forum screen.
- `NovalPieApp.kt:2904-2916`: `HeroCard(semanticsMarker)` — same pattern, optional.
- No `Modifier.semantics { heading() }` anywhere → screen readers cannot navigate by heading despite 89 bold section headers (§5.8).
- No `role = Role.Button` on the 26 custom `.clickable` sites → they announce as generic clickable, not as buttons.
- No `stateDescription` on the 7 no-op `AssistChip`s or the toggle rows.
- No `testTag` anywhere → the (absent) UI tests would have nothing to anchor on.

### 7.5 Seven fake buttons

`AssistChip(onClick = {})` × 7 (`MessageScreens.kt:407`, `NovalPieApp.kt:3333`, `NovalPieApp.kt:3341`, `UploadScreens.kt:210`, `:211`, `:344`, `WorkspaceScreens.kt:328`). Each is announced to TalkBack as an actionable button, shows a ripple on press, and does nothing. On the book-detail screen (`NovalPieApp.kt:3333`, `:3341`) that is up to 8 fake buttons for tags plus N for facts.

### 7.6 Text scaling

Accidentally fine (§1.8): no hardcoded `fontSize`, everything routes through `sp`-based `MaterialTheme.typography`. The risk is container clipping at large font scales — see §8.

---

## 8. Layout robustness

### 8.1 Fixed heights that will clip at large font scales or long content

| File:line | Constraint | Risk |
| --- | --- | --- |
| `NovalPieApp.kt:1979` | `LazyColumn(modifier = Modifier.height(300.dp))` — the reader catalog list | fixed 300 dp regardless of screen height; on a small screen the panel plus its `86.dp` bottom padding (`:1951`) can exceed the viewport |
| `UploadScreens.kt:170` | `Button(modifier = Modifier.fillMaxWidth().height(54.dp))` | a `Button` with a fixed height clips its label at font scale ≥ 1.3 |
| `UploadEditorScreens.kt:426` | `Button(modifier = Modifier.fillMaxWidth().height(50.dp))` | same |
| `BookChapterScreens.kt:268` | `Modifier.heightIn(max = 560.dp).verticalScroll(...)` | nested vertical scroll inside a `LazyColumn` item; on a tall tablet it wastes space, on a short phone it exceeds the viewport |
| `BookChapterScreens.kt:359` | `Modifier.heightIn(max = 520.dp).verticalScroll(...)` | same |
| `NovalPieApp.kt:3521` | `heightIn(min = 120.dp, max = 720.dp)` on a reader illustration | 720 dp exceeds most phone viewport heights, so tall images fill more than a screen |
| `NovalPieApp.kt:1140` | `.width(2.dp).height(86.dp)` — comment thread indent rail | fixed 86 dp regardless of the comment's actual height, so the rail under/over-shoots |
| `NovalPieApp.kt:2873` | `ElevatedCard(Modifier.width(196.dp))` recent-reading card | fixed width; the card contains `maxLines = 2` `bodySmall` text, so it clips rather than growing |
| `NovalPieApp.kt:3089` | `BookCover(title, url, 100.dp, 150.dp, …)` | fixed 100×150 cover in the detail hero |
| `NovalPieApp.kt:3327` | `BookCover(title, url, 104.dp, 148.dp, …)` | **different** fixed size for the same visual role, 238 lines away |
| `BookEditScreens.kt:238` | `Modifier.width(100.dp).height(150.dp)` | a **third** cover size |
| `BookChapterScreens.kt:328` | `Modifier.width(72.dp).height(96.dp)` | a **fourth** cover size |
| `ProfileScreens.kt:276` | `Modifier.size(76.dp)` avatar | fixed |
| `NovalPieApp.kt:738-739` | `.width(42.dp).height(42.dp)` forum avatar | fixed, and 42 dp is not on any grid |

Four different cover aspect-boxes (`100×150` = 0.667, `104×148` = 0.703, `72×96` = 0.75) versus the declared `bookCoverAspectRatio() = 2f/3f = 0.667` (`NovalPieApp.kt:3412`) — three of the four are off-ratio and will letterbox or crop.

### 8.2 Hardcoded bottom padding double-counts the Scaffold inset

`NovalPieApp.kt:175-181` already applies the `Scaffold`'s `padding` (which includes the `NavigationBar` height) to the content `Surface`. Yet four screens add their own bottom compensation on top:

| File:line | `contentPadding` bottom |
| --- | --- |
| `NovalPieApp.kt:614` | `bottom = 80.dp` (Forum) |
| `NovalPieApp.kt:2238` | `bottom = 96.dp` (Tools) |
| `PoliticalExamScreens.kt:222` | `bottom = 96.dp` |
| `ProfileScreens.kt:118` | `bottom = 96.dp` |
| `NovalPieApp.kt:1951` | `bottom = 86.dp` (reader catalog panel) |

So those screens have ~160–176 dp of dead space at the bottom of their scroll, while `HomeScreen` (`NovalPieApp.kt:1242`, `PaddingValues(12.dp)`) and `SearchScreen` do not. Inconsistent, and the magic numbers (80/86/96) drift from the real `NavigationBar` height (80 dp) once insets change.

### 8.3 Chip rails: 59 `LazyRow`s that clip instead of wrapping

`FlowRow` is used **5 times**, all in `NovalPieApp.kt`: `:1606` (search filter summary), `:2995` (novel card tags), `:3109` + `:3119` (book detail facts / tags), `:3196` (book detail action row). `FlowColumn`: 0.

`LazyRow` is used **59 times** (`NovalPieApp.kt` 24, `MessageScreens.kt` 6, `UploadEditorScreens.kt` 6, `AdminScreens.kt` 3, `BookChapterScreens.kt` 4, `BookEditScreens.kt` 4, `ProfileScreens.kt` 4, `WorkspaceScreens.kt` 4, `ForumCreateScreens.kt` 2, `UserProfileScreens.kt` 2). Many hold chip sets that should wrap:

| File:line | Content | Symptom |
| --- | --- | --- |
| `NovalPieApp.kt:1637-1643` | search history as `OutlinedButton`s | clips at the right edge with no scroll hint |
| `NovalPieApp.kt:1494-1503` | hot tags as `FilterChip`s | **confirmed clipped** in `qa-screenshots/turn36/search_initial_after_proxy_fix.png` — `奇幻 26779 / 同人 12805 / 现代 12255 / 后宫 10…` cut mid-glyph |
| `NovalPieApp.kt:1693-1705` | `FilterChoiceRail` × 6 groups (排序/顺序/范围/内容/字数/来源/匹配) | **confirmed clipped** in the same screenshot |
| `NovalPieApp.kt:2760-2777`, `:2808-2824` | favourite-group filter chips, `.take(8)` | clipped |
| `NovalPieApp.kt:761-765` | forum badges (`置顶`, `精华`, category) | clipped |
| `NovalPieApp.kt:779-788` | forum metrics | clipped |
| `NovalPieApp.kt:2341-2347` | message stat pills | clipped |
| `NovalPieApp.kt:2542` | profile account-status pills | clipped |
| `NovalPieApp.kt:3635-3638` | the two buttons of `EmptyCollectionState` (`网页登录`, `打开网页`) — a `LazyRow` for **two** items | pointless virtualisation, and clips on narrow screens |
| `NovalPieApp.kt:2210-2219` | `ReaderToolbar` — 7 `TextButton`s + 2 spacers + a text label | clips; the reader's primary controls can scroll off-screen with no indication |
| `MessageScreens.kt:430-437` | message actions (`标记已读`, `添加星标`, `打开私信`, `删除`) | clipped |
| `UploadEditorScreens.kt:238-241` | `打开` / `生成 EPUB` / `发送到上传` | clipped |

None of the 59 uses `contentPadding` fade, an edge gradient, or an overflow indicator (only `NovalPieApp.kt:1696` sets `contentPadding = PaddingValues(horizontal = 2.dp)`). The user cannot tell that content continues.

Also: **nested `LazyRow` inside a `LazyColumn` item inside a `clickable` card** (`NovalPieApp.kt:724` card `clickable` → `:761` `LazyRow` → `:779` `LazyRow`). Horizontal drags inside the rails compete with the card's click and with the outer vertical scroll.

### 8.4 `Row` children without `weight` that can overflow

`Arrangement.SpaceBetween` on a `fillMaxWidth()` `Row` appears **60 times**. The following put an unbounded `Text` next to a fixed-width action with **no `weight(1f)` and no `maxLines`** — a long value pushes the action off-screen:

| File:line | Row contents | Overflow risk |
| --- | --- | --- |
| `NovalPieApp.kt:3234-3245` | `Text(title, titleMedium Bold)` **no weight, no maxLines** + `OutlinedButton(打开网页评论)` | a long comments-section title clips the button |
| `NovalPieApp.kt:2077-2085` | same pattern in `ReaderChapterCommentsSection` | ditto |
| `NovalPieApp.kt:1629-1636` | `Text(搜索历史)` + `TextButton(清空)` | short strings today, breaks on translation |
| `NovalPieApp.kt:1474-1484` | `Text(热门标签, titleSmall Bold)` + `TextButton(Icon+刷新)` | ditto |
| `MessageScreens.kt:253-262` | `Column { Text(我的消息); Text(通知、回复与私信) }` **no weight** + `Row { 2 IconButtons }` | a longer subtitle overlaps the icons |
| `WorkspaceScreens.kt:129-135` | `Column { Text(工作区); Text(管理 API、Cookie 与翻译任务) }` **no weight** + `IconButton` | the subtitle is already 15 chars; at font scale 1.5 it collides |
| `NovalPieApp.kt:1565-1591` | `Column { Text(overview.title, titleLarge Bold); Text(subtitle) }` **no weight** + `Row { Surface(status pill); IconButton }` | ditto |
| `NovalPieApp.kt:2840-2858` | `Icon` + `Column(weight 1f)` + `Button(继续阅读)` + `TextButton(清除)` | `Column` has weight, but the two buttons plus icon plus 10 dp gaps need ≈ 230 dp; below ~320 dp width the title column collapses to zero |
| `AdminScreens.kt:536`, `:546`, `:559`, `:789` | `Text` + control, no weight | ditto |
| `BookEditScreens.kt:294`, `:364` | `Text` + `Switch`, no weight | ditto |
| `ForumCreateScreens.kt:296`, `:340` | `Text` + control | ditto |
| `WorkspaceScreens.kt:469` | `Text(启用此配置)` + `Switch`, no weight | ditto |
| `PoliticalExamScreens.kt:388` | `Row(SpaceBetween)` | ditto |

Good counter-examples where `weight(1f)` **is** used: `NovalPieApp.kt:751` (forum feed title column), `NovalPieApp.kt:2020` (reader top bar centre), `WorkspaceScreens.kt:420`, `WorkspaceScreens.kt:447`, `UploadEditorScreens.kt:225`.

### 8.5 `maxLines` / `overflow` coverage

- `Text(` call sites: **907**
- `maxLines` occurrences: **51**
- `TextOverflow` occurrences: **53**

So **~94% of `Text` calls have no `maxLines`**. Most are legitimately multi-line body copy, but the following one-line-by-intent texts have neither `maxLines` nor `overflow` and will wrap unpredictably or overflow their fixed container:

`NovalPieApp.kt:3239` (comments title), `NovalPieApp.kt:1571` (`overview.title`), `NovalPieApp.kt:1573` (`overview.subtitle`), `NovalPieApp.kt:2508` (`overview.accountName` — user-controlled!), `NovalPieApp.kt:2853` (`章节 {id}`), `NovalPieApp.kt:2877` (`章节 {id}` in a fixed 196 dp card), `NovalPieApp.kt:3060-3061` (`分组 id`, `原始状态`), `MessageScreens.kt:256`, `MessageScreens.kt:281-284` (`HeroStat` value+label), `WorkspaceScreens.kt:132`, `WorkspaceScreens.kt:150-153`, `WorkspaceScreens.kt:323` (`name` — user-controlled), `WorkspaceScreens.kt:389` (`config.configKey`), `AdminScreens.kt:781-782`, `AdminScreens.kt:811`, `UploadScreens.kt:380-381`.

Server-controlled strings that reach an unbounded `Text`: `overview.accountName` (`NovalPieApp.kt:2508`), `config.configKey` (`WorkspaceScreens.kt:389`), key `name` (`WorkspaceScreens.kt:323`), `book.title` in the hero (`NovalPieApp.kt:3329`, has `headlineSmall` but no `maxLines`) — visible in `qa-screenshots/turn38/book_detail_wrapped.png` where `成为暗黑奇幻里的猎人` wraps to two lines and pushes the metadata down.

### 8.6 Manual grids instead of `LazyVerticalGrid`

Both grids are hand-chunked (`NovalPieApp.kt:1297-1309` for favourites, `NovalPieApp.kt:1434-1447` for search results):
```kotlin
val columns = novelGridColumnCount()          // == 2, hardcoded at NovalPieApp.kt:3414
items(visibleBooks.chunked(columns), key = { it.joinToString { b -> b.id.toString() } }) { rowBooks ->
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        for (book in rowBooks) { Box(Modifier.weight(1f)) { NovelCardItem(...) } }
        for (i in 0 until (columns - rowBooks.size)) { Spacer(Modifier.weight(1f)) }
    }
}
```
Consequences: 0 hits for `LazyVerticalGrid`; the `key` is a joined string of row ids so adding one item re-keys every subsequent row and defeats item reuse; the column count cannot adapt (no `BoxWithConstraints`/`WindowSizeClass`, §1.6), so a 10-inch tablet or a foldable in unfolded state shows two enormous cards per row. `android:screenOrientation="unspecified"` (`AndroidManifest.xml:13`) means landscape is reachable and gets the same 2 columns.

### 8.7 IME / cutout: nothing

Restating §2.5 for completeness: 0 hits for `imePadding`, `WindowInsets`, `navigationBarsPadding`, `statusBarsPadding`, `systemBarsPadding`, `safeDrawing`, `displayCutout`. No `android:windowSoftInputMode`. Every composer — `ForumCommentComposer` (`NovalPieApp.kt:1039`), `InlineCommentComposer` (`NovalPieApp.kt:1077`), the message reply box (`MessageScreens.kt:480-495`), the EPUB editor text area (`UploadEditorScreens.kt`), the book-edit form (`BookEditScreens.kt`) — can be fully covered by the keyboard.

---

## 9. Dependency and toolchain currency

### 9.1 Build files, verbatim inventory

`build.gradle` (root, 4 lines):
```groovy
plugins {
    id 'com.android.application' version '8.0.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.8.10' apply false
}
```

`gradle/wrapper/gradle-wrapper.properties`:
```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.0.2-all.zip
networkTimeout=60000
```
→ Gradle **8.0.2**, fetched from a **third-party Tencent Cloud mirror**, with **no `distributionSha256Sum`**.

`gradle.properties` (6 lines):
```
org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
kotlin.compiler.execution.strategy=in-process
```
Missing: `org.gradle.parallel`, `org.gradle.caching`, `org.gradle.configuration-cache`, `android.nonFinalResIds`, `android.suppressUnsupportedCompileSdk`, `kotlin.incremental`. `-Xmx1024m` is very low (README:232-238 explains it is a deliberate cap for this machine's JVM native-memory failures — see the `hs_err_pid137816.log` and `replay_pid137816.log` in the repo root).

`app/build.gradle:6-54` — `namespace 'com.novalpie.nativeapp'`, `compileSdk 34`, `applicationId 'com.novalpie.app'`, `minSdk 23`, `targetSdk 34`, `versionCode 2026070601`, `versionName '2.0.0-native-alpha1'`, `kotlinCompilerExtensionVersion '1.4.3'`, Java/Kotlin target 17.

### 9.2 Dependency currency table

`app/build.gradle:56-73`, evaluated as of 2026-07. (Recommended versions are the current stable line as of the audit; verify exact patch numbers before pinning.)

| Dependency | Current | Released | Current in 2026? | Recommended | What breaks on upgrade |
| --- | --- | --- | --- | --- | --- |
| `com.android.application` (AGP) | **8.0.0** | Apr 2023 | **No — ~3 years stale, 13+ minor releases behind** | AGP **8.13.x** (latest 8-series stable) as the low-risk target; AGP 9.x only after the Kotlin 2 migration | AGP ≥ 8.2 needs Gradle ≥ 8.2; ≥ 8.13 needs Gradle ≥ 8.13 and JDK 17 (already satisfied). `namespace` already migrated. `buildFeatures.compose true` still valid. **AGP 8.1.1+ removes the compileSdk-34 warning** (see §9.3). Groovy `build.gradle` still works. |
| Gradle wrapper | **8.0.2** | Mar 2023 | **No** | **8.13+** (or 9.x with AGP 9) | Configuration cache and isolated projects become available; `org.gradle.workers.max=2` still honoured. **Also: replace the Tencent mirror URL with `services.gradle.org` and add `distributionSha256Sum`.** |
| `org.jetbrains.kotlin.android` | **1.8.10** | Feb 2023 | **No — pre-2.0, ~3 years stale** | Kotlin **2.1.x / 2.2.x** | **Largest break.** (a) `composeOptions.kotlinCompilerExtensionVersion` is obsolete — must add the `org.jetbrains.kotlin.plugin.compose` Gradle plugin and delete the `composeOptions {}` block (`app/build.gradle:35-37`). (b) K2 front-end is stricter about platform-type nullability and unresolved references — expect new errors in `NovalPieApi.kt` (`org.json` interop, 3404 lines) and `Models.kt`. (c) `kotlinx-coroutines` must be ≥ 1.8. (d) `kotlin.compiler.execution.strategy=in-process` still valid but slower with K2. |
| `androidx.compose.ui:ui` / `ui-tooling-preview` / `ui-tooling` | **1.4.3** | Apr 2023 | **No** | switch to `androidx.compose:compose-bom` (2025/2026 release) → UI ~1.8/1.9 | `FlowRow`/`FlowColumn` graduate out of `ExperimentalLayoutApi` (5 `@OptIn` sites become warnings: `NovalPieApp.kt:1549`, `:2960`, `:3069`, `:3180`, plus the imports at `:11-12`). Ripple API moves from `LocalRippleTheme` to `LocalRippleConfiguration` (not used here → no break). `SubcomposeAsyncImage` unaffected. |
| `androidx.compose.foundation:foundation` | **1.4.3** | Apr 2023 | **No** | via BOM | `combinedClickable` stable; `ExperimentalFoundationApi` opt-in at `NovalPieApp.kt:3496` becomes unnecessary. |
| `androidx.compose.material3:material3` | **1.1.0** | Apr 2023 | **No — 1.1.0 is the *first* stable M3 release** | **1.3.x / 1.4.x** | **Unlocks what this audit says is missing:** `PullToRefreshBox` (1.3.0), stable `SearchBar`, `Carousel`, `LoadingIndicator`, expressive motion tokens, `HorizontalFloatingToolbar`. Breaks: `Divider` → `HorizontalDivider`/`VerticalDivider`; several `*ChipColors`/`*ButtonColors` factory signatures gain parameters; `ExperimentalMaterial3Api` set shifts (the app has 8 `@OptIn(ExperimentalMaterial3Api::class)` sites — `NovalPieApp.kt:1688`, `:1709`, `:1791`, `:2737`, `:2791`, `AdminScreens.kt:802`, plus others — some become redundant, some move). `CenterAlignedTopAppBar` gains `scrollBehavior` requirements for the new scroll-tint behaviour. |
| `androidx.compose.material:material-icons-extended` | **1.4.3** | Apr 2023 | No | via BOM; **but consider dropping it** | This artifact is ~5 MB of vector code and is discouraged for shipping apps (it is deprecated in newer Compose lines in favour of per-icon assets). With `minifyEnabled false` (§10.1) **none of it is stripped** — every unused icon ships. |
| `androidx.activity:activity-compose` | **1.7.0** | Mar 2023 | No | **1.9.x / 1.10.x** | `enableEdgeToEdge()` lives in `androidx.activity` **1.8.0+** — required for §2.5. Predictive-back animation support needs 1.8+. `AndroidManifest.xml:7` already sets `enableOnBackInvokedCallback="true"`, which with 1.7.0 routes through `OnBackPressedDispatcher` but gets no predictive-back animation. The `BackHandler` at `NovalPieApp.kt:123-125` keeps working. |
| `androidx.lifecycle:lifecycle-viewmodel-compose` / `-runtime-ktx` | **2.6.1** | Mar 2023 | No | **2.8.x** (2.9+ requires Kotlin 2) | `viewModel()` signature stable. 2.8 makes `collectAsStateWithLifecycle` available from `-runtime-compose` (currently unused — the ViewModel exposes plain `mutableStateOf` properties). |
| `androidx.webkit:webkit` | **1.8.0** | Jun 2023 | No | **1.12+ / 1.14** | `ProxyController` / `ProxyConfig` / `WebViewFeature.PROXY_OVERRIDE` API used at `WebFallbackScreen.kt:68-97` is stable. Newer versions add `WebViewCompat` safe-browsing and `setProfile` APIs. |
| `io.coil-kt:coil-compose` | **2.4.0** | May 2023 | No | **2.7.0** (drop-in) or **3.x** (rename) | 2.7.0 is a drop-in patch bump. Coil **3.x** renames the package to `coil3` and requires Kotlin 2 + okio 3; `AsyncImage`/`SubcomposeAsyncImage`/`ImageRequest.Builder` are near-identical but `ImageLoader.Builder` and the OkHttp integration move to `coil-network-okhttp`. Affects `data/NovalPieImageLoading.kt` (42 lines) and `SubcomposeAsyncImage` at `NovalPieApp.kt:3513`, `:3373`, `ImagePreviewDialog.kt:121`. |
| `com.squareup.okhttp3:okhttp` | **4.11.0** | Jun 2023 | No | **4.12.0** (drop-in, security fixes) or **5.x** | 4.12.0 is a safe patch. OkHttp **5.x** is Kotlin-first: `MockWebServer` moves to `mockwebserver3` (breaks `app/src/test/.../NovalPieApiTest.kt`, `AdminApiTest.kt`, `UploadApiTest.kt`, `WorkspaceApiTest.kt`, `BookManagementApiTest.kt` — 5 test files use `com.squareup.okhttp3:mockwebserver`), and several `Call`/`EventListener` signatures change. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | **1.7.1** | Jun 2023 | No | **1.9.x / 1.10.x** | Required ≥ 1.8 by Kotlin 2. `Dispatchers.IO` limited-parallelism API stable. |
| `junit:junit` | 4.13.2 | 2021 | still supported | 4.13.2 (or JUnit 5) | — |
| `androidx.test:core` | 1.5.0 | 2022 | No | 1.6.x | needed for Robolectric ≥ 4.13 |
| `org.robolectric:robolectric` | **4.11.1** | Nov 2023 | No | **4.14+ / 4.15** | Required for SDK 35/36 shadows. All 51 unit tests run through it (`testOptions.unitTests.includeAndroidResources = true`, `app/build.gradle:48-52`). |
| `com.squareup.okhttp3:mockwebserver` | 4.11.0 | Jun 2023 | No | match okhttp | see okhttp row |

### 9.3 `compileSdk` / `targetSdk` / `minSdk`

| Setting | Current | Status | Recommended | Break on upgrade |
| --- | --- | --- | --- | --- |
| `compileSdk` | **34** (`app/build.gradle:12`) | **Mismatched with AGP 8.0.0.** AGP 8.0.0 was tested only up to `compileSdk 33`, so every build emits the warning the README calls the *"AGP/compileSdk pairing"* warning (README.md:1211, :1247, :1286, :1319, :725). It is a warning, not an error, and can be silenced with `android.suppressUnsupportedCompileSdk=34`, but the correct fix is AGP ≥ 8.1.1. | **36** (Android 16) | New lint checks; deprecated-API warnings. Requires AGP ≥ 8.9-ish for SDK 36 support. |
| `targetSdk` | **34** (`app/build.gradle:19`) | **Below the Google Play submission floor.** Play has required `targetSdk 35` for new apps and updates since 2025-08-31, and raises the bar annually (36 expected for 2026). The app as-configured cannot be published. | **35 minimum, 36 preferred** | **This is the change that forces the §2.5 work.** `targetSdk 35` makes **edge-to-edge mandatory**: `Window.setDecorFitsSystemWindows` is deprecated, `android:statusBarColor` / `android:navigationBarColor` (i.e. `styles.xml:5-6`) are **ignored**, and the only opt-out (`android:windowOptOutEdgeToEdgeEnforcement`) is temporary and removed at 36. With zero inset handling, content will draw under the status bar and behind the navigation bar on day one. Also: 16 KB page-size alignment for native libs (no native libs here → no impact), stricter foreground-service and `BroadcastReceiver` rules (not used), predictive-back becomes default-on. |
| `minSdk` | **23** (Android 6.0, `app/build.gradle:18`) | Very low but harmless. Compose supports 21+. Android 6.0 has ~0% share in 2026. | **24 or 26** | Nothing in this codebase depends on API 23. Raising to 26 removes multidex concerns and unlocks adaptive-icon-only shipping (relevant to §3.1). |
| `compileOptions` / `kotlinOptions` | Java 17 / `jvmTarget 17` (`app/build.gradle:39-46`) | Current and correct | — | — |

### 9.4 Version catalog / Kotlin DSL / lint / static analysis / CI

| Item | Status | Evidence |
| --- | --- | --- |
| **`gradle/libs.versions.toml`** (version catalog) | **absent.** `gradle/` contains only `wrapper/gradle-wrapper.jar` and `wrapper/gradle-wrapper.properties`. All 18 dependency coordinates are inline strings with 12 distinct hardcoded version literals, `1.4.3` repeated 5 times. | `ls gradle/` |
| **Kotlin DSL** | **absent.** `build.gradle`, `app/build.gradle`, `settings.gradle` are all Groovy. No `.kts` file in the project. | |
| **Android lint config** | **absent.** No `lint {}` block in `app/build.gradle`, no `lint.xml`, no `lint-baseline.xml`, no `lintOptions`. Lint runs only with defaults and nothing gates on it. | |
| **detekt** | absent | `find . -iname "*detekt*"` → nothing |
| **ktlint / spotless** | absent | `find . -iname "*ktlint*"` → nothing |
| **`.editorconfig`** | absent | |
| **CI** | **absent.** No `.github/`, no `.gitlab-ci.yml`, no `Jenkinsfile`, no `azure-pipelines.yml`, no `bitrise.yml`. | |
| **Instrumentation / UI tests** | **absent.** No `app/src/androidTest/` source set. No `androidTestImplementation` dependency. No `androidx.compose.ui:ui-test-junit4`, no `ui-test-manifest`. | `ls app/src` → `main`, `test` only |
| **Screenshot / snapshot tests** | absent. No Paparazzi, no Roborazzi, no Shot. | |
| Unit tests | **51 files** (16 in `data/`, 35 in `ui/`), all pure-JVM + Robolectric. They cover presentation *strings* and API parsing well (`ProductCopyTest`, `VisibleUiLabelsTest`, `UiNavigationTest`, `ThemePaletteTest`, `DiscoverPresentationTest`, …) but **cannot see any of the layout, contrast, inset, touch-target or component-duplication defects in this report.** | `app/src/test/java/com/novalpie/nativeapp/{data,ui}` |
| Verification scripts (the de-facto CI) | `tools/verify-native-project.ps1`, `tools/verify-mumu-compose-launch.ps1`, `tools/build-release.ps1` — Windows-only, machine-path-hardcoded PowerShell, run manually. | README.md:245-251 |

---

## 10. Missing production concerns

### 10.1 R8 / ProGuard is disabled in release

`app/build.gradle:22-32`:
```groovy
buildTypes {
    debug { applicationIdSuffix '.debug'; versionNameSuffix '-debug' }
    release {
        minifyEnabled false
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```
- `minifyEnabled false` → **no code shrinking, no obfuscation, no optimisation in release.**
- `shrinkResources` is not set (and would be a no-op without minify) → no resource shrinking.
- `app/proguard-rules.pro` is **one comment line**: `# Native alpha keeps minification disabled. Rules file is present for release parity.`
- Combined with `material-icons-extended` (§9.2) the release APK ships the entire extended icon set plus all dead code, including the 5 dead composables (§5.10).
- Turning `minifyEnabled true` on later will need rules for `org.json` reflection paths in `NovalPieApi.kt` (3404 lines of manual `JSONObject` parsing) and for the Robolectric-tested model classes.

### 10.2 No Gradle signing config; release signing is an out-of-band PowerShell script

- `app/build.gradle` has **no `signingConfigs` block** and the `release` build type has no `signingConfig`. `./gradlew :app:assembleRelease` produces `app-release-unsigned.apk`.
- Signing happens in `tools/build-release.ps1`, which:
  - hardcodes `$SigningDir = "D:\NovalPie\commercial-app\signing"`, `$BuildTools = "C:\Users\86188\AppData\Local\Android\Sdk\build-tools\34.0.0"`, `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"`
  - reads the keystore password from a **plaintext file** `keystore-credentials.txt` (`Read-SigningCredentials`, lines 11-28) and passes it on the `apksigner` command line as `--ks-pass "pass:$storePass"` (visible in the process table)
  - defaults the key alias to the literal `"novalpie"` if absent
  - runs `zipalign` then `apksigner sign` then `apksigner verify` then copies to `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Not reproducible on any other machine, not runnable in CI, no v3/v4 signing scheme control, no App Bundle (`bundleRelease`) path at all — so Play's required AAB format is not produced.
- `.gitignore` correctly excludes `signing.properties`, `*.jks`, `*.keystore`.

### 10.3 Zero crash reporting and zero logging

- **0 hits** across all 63 `.kt` files for `Log.`, `println(`, `System.out`, `printStackTrace`, `android.util.Log`, `Timber`, `Crashlytics`, `FirebaseCrashlytics`, `Sentry`, `ACRA`, `bugsnag`.
- There is no logging *strategy* because there is no logging at all. Runtime diagnosis relies entirely on `adb logcat` catching framework-level exceptions — which is exactly what the README turn logs show (`qa-screenshots/turn38/post_fix_runtime_logcat.txt` 400 KB, `turn36/after_search_logcat.txt` 187 KB, etc.: raw full-device logcat dumps, grepped by hand).
- Failure paths swallow silently: `runCatching { … }.getOrNull()` appears throughout `AuthSessionStore.kt:15,17`, `WebFallbackScreen.kt:77-100`, `NetworkConfigStore.kt:72`, `EpubParser.kt:118-120` — errors vanish with no breadcrumb.
- No `StrictMode` in debug, no `LeakCanary`.

### 10.4 `allowBackup="true"` with the auth JWT in plaintext SharedPreferences — and the mitigations are DEAD CODE

This is the most serious security finding.

- `AuthSessionStore.kt:43`: `context.getSharedPreferences("novalpie_native_auth", Context.MODE_PRIVATE)`, key `"auth_token"` (`:65`). The value is the site JWT, stored **in plaintext XML** at `/data/data/com.novalpie.app/shared_prefs/novalpie_native_auth.xml`. No `EncryptedSharedPreferences`, no Keystore, no `androidx.security:security-crypto` dependency.
- `AndroidManifest.xml:6`: `android:allowBackup="true"`.
- **`AndroidManifest.xml` has no `android:fullBackupContent` and no `android:dataExtractionRules` attribute.**
- Yet the repo contains two files written specifically to fix this, with comments that describe the exact threat:

`app/src/main/res/xml/backup_rules.xml`:
```xml
<!--
  Android 11 and below (android:fullBackupContent).
  novalpie_native_auth holds the site JWT. allowBackup was true with no exclusions,
  so `adb backup` could lift a logged-in session off the device. ...
-->
<full-backup-content>
    <exclude domain="sharedpref" path="novalpie_native_auth.xml" />
</full-backup-content>
```

`app/src/main/res/xml/data_extraction_rules.xml`:
```xml
<!--
  Android 12+ (android:dataExtractionRules).
  The auth JWT is excluded from both cloud backup and device-to-device transfer.
  A transferred token would be a live session on another device.
-->
<data-extraction-rules>
    <cloud-backup><exclude domain="sharedpref" path="novalpie_native_auth.xml" /></cloud-backup>
    <device-transfer><exclude domain="sharedpref" path="novalpie_native_auth.xml" /></device-transfer>
</data-extraction-rules>
```

**Neither file is referenced from the manifest, so neither has any effect.** The vulnerability the comments describe is fully live: on API ≤ 30 `adb backup` extracts the JWT; on API 31+ cloud backup and device-to-device transfer copy it. The `WebFallbackScreen` also *writes* the token into the WebView's `localStorage` (`WebFallbackScreen.kt:138-140`), and WebView local storage is inside the backed-up data dir too.

Additionally: `WebFallbackScreen.kt:43-45` enables `javaScriptEnabled`, `domStorageEnabled`, `databaseEnabled` on a WebView that receives the JWT via `evaluateJavascript` — with `@SuppressLint("SetJavaScriptEnabled")` at `:23`. There is no `setAllowFileAccess(false)`, no `setAllowContentAccess(false)`, no `setMixedContentMode(MIXED_CONTENT_NEVER_ALLOW)`, and no `shouldOverrideUrlLoading` allow-list, so any navigation the site performs stays inside a WebView holding a live session token.

### 10.5 No network security config; a debug proxy ships to release and activates on real x86 hardware

- No `android:networkSecurityConfig` in the manifest, no `res/xml/network_security_config.xml`, no `android:usesCleartextTraffic`. With `targetSdk 34` the platform default blocks cleartext, which is the right default — the API base URL is `https://novalpie.cc` (`NovalPieApi.kt:97`).
- However `NovalPieApi.kt:1202` explicitly permits plaintext endpoints: `require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) { "API endpoint must use HTTP or HTTPS" }`, and `NovalPieApi.kt:1632` / `:3309` / `ReaderText.kt:136` / `NovalPieViewModel.kt:1470` all accept `http://` URLs for images and links.
- **The emulator proxy is compiled into release and activates on real devices.** `NetworkConfigStore.kt:71-75`:
  ```kotlin
  internal fun shouldPreferEmulatorProxy(
      supportedAbis: Array<String>? = runCatching { Build.SUPPORTED_ABIS }.getOrNull()
  ): Boolean = supportedAbis.orEmpty().any { abi ->
      abi.equals("x86", ignoreCase = true) || abi.equals("x86_64", ignoreCase = true)
  }
  ```
  On **any x86/x86_64 Android device** — Chromebooks running ARC++, Windows Subsystem for Android, x86 tablets — a release build routes every OkHttp request through `Proxy(HTTP, 10.0.2.2:7890)` then `127.0.0.1:7890` before falling back to `Proxy.NO_PROXY` (`NetworkConfigStore.kt:32-47`), and sets a WebView proxy override to `http://10.0.2.2:7890` (`WebFallbackScreen.kt:103-113`). Result on such hardware: two failed TCP connects per request (added latency) and, if anything *is* listening on `127.0.0.1:7890`, all traffic including the JWT-bearing WebView goes through it.
- `ProxySettings.DEFAULT_PROXY_HOST = "10.0.2.2"`, `DEFAULT_PROXY_PORT = 7890` (`NetworkConfigStore.kt:52-54`) are release constants.

### 10.6 No baseline profile

- No `androidx.baselineprofile` Gradle plugin, no `androidx.profileinstaller:profileinstaller` dependency, no `app/src/main/baseline-prof.txt`, no `baselineProfile {}` block, no `benchmark` module.
- Combined with `minifyEnabled false` (no R8 optimisation) and the manual-chunked grids with unstable keys (§8.6), first-run cold start and first-scroll jank are un-mitigated. There is no macrobenchmark to measure it either.

### 10.7 Other production gaps

| Gap | Evidence |
| --- | --- |
| No App Bundle output | `tools/build-release.ps1` builds `assembleRelease` (APK) only; no `bundleRelease`, no `bundle {}` config, no density/language splits |
| No `resConfigs` | ships all Android library locales even though the app is Chinese-only |
| No `ndk { abiFilters }` / no native code | fine |
| Deep link is unverified and un-namespaced | `AndroidManifest.xml:18-25` registers `novalpie://app` with `BROWSABLE` — any app can launch it; no `android:autoVerify`, no App Links (`https://novalpie.cc/...`), no signature check on the incoming `startUri` which flows straight into `viewModel.openDeepLink(startUri)` (`MainActivity.kt:15`, `NovalPieApp.kt:119-121`) |
| No `android:exported` audit needed beyond the single activity | only one component |
| `android:screenOrientation="unspecified"` with no landscape/tablet layouts | `AndroidManifest.xml:13` + §8.6 |
| Repo hygiene | `hs_err_pid137816.log` (79 KB) and `replay_pid137816.log` (309 KB) are **committed at the repo root** despite `.gitignore` listing `hs_err_pid*.log` / `replay_pid*.log`; `app/build/` and `build/` directories exist on disk |
| `qa-screenshots/` is gitignored | `.gitignore:24` — the only rendered-result evidence is untracked and will be lost |

---

## 11. Rendered-result evidence

### 11.1 Inventory of `qa-screenshots/`

7 turn directories, 45 files. **Note: `qa-screenshots/` is listed in `.gitignore:24`, so these are untracked local artefacts from 2026-07-12; `README.md` documents turns up to 42, so the newest screenshots are ~2-3 turns behind `HEAD`.** Structural claims in this report are all taken from source; the screenshots are used only for colour/hierarchy/clipping judgement.

| Dir | Date | Files |
| --- | --- | --- |
| `turn34` | 07-12 11:36 | `home_book_cards_tags.png` (89 KB), `home_collection_network_loaded.png` (217 KB) |
| `turn35` | 07-12 12:16 | `after_install_current_retry.png` (**0 bytes**), `collection_book_cards_after_scroll.png` (638 KB), `current_start.png` (217 KB), `search_initial.png` (119 KB), `search_results_after_query.png` (1.16 MB) |
| `turn36` | 07-12 13:16 | `collection_cards_after_proxy_fix.png` (643 KB), `launch_collection.png` (414 KB), `search_cover_preview_after_proxy_fix.png` (2.23 MB), `search_initial_after_proxy_fix.png` (120 KB), `search_results_after_proxy_fix.png` (1.16 MB) + 4 `*_ui.xml` dumps + 3 `*_logcat.txt` (187 KB / 96 KB / 38 KB) |
| `turn37` | 07-12 13:48 | `launch_after_visible_labels.png` (123 KB), `app_pid_logcat.txt` (24 KB) |
| `turn38` | 07-12 14:32 | `after_search_tab.png`, `book_detail.png` (371 KB), `book_detail_wrapped.png` (375 KB), `cover_preview.png` (2.23 MB), `launch_collection.png` (217 KB), `search_initial.png`, `search_results.png` (1.16 MB), `search_results_postfix.png`, `search_results_scrolled.png` (1.35 MB) + 8 `*_ui.xml` + 6 `*_logcat.txt` (up to 503 KB) |
| `turn39` | 07-12 15:31 | `home_after_fix.png` (137 KB), `launch.png` (122 KB), `reader.png` (63 KB) + 3 `*_ui.xml` + 3 `*_logcat.txt` |
| `turn40` | 07-12 16:07 | `tools_upload_editor_entry_logcat.txt` (**0 bytes**), `tools_upload_editor_entry_ui.xml` (**0 bytes**) — the turn-40 capture failed |

Device: MuMu emulator, 900 × 1600 px (per image dimensions), Android status bar showing wifi/cellular/battery.

### 11.2 What the screenshots actually show

**Read: `turn39/home_after_fix.png`, `turn39/launch.png`, `turn39/reader.png`, `turn38/book_detail_wrapped.png`, `turn36/search_initial_after_proxy_fix.png`.**

Confirmed against `HEAD` source:

1. **The cream/white/grey three-band seam at the top is present in all five.** Status bar cream `#FFF8F4`, then a white top bar, then a grey page. Also at the bottom: cream system nav bar under a white `NavigationBar`. → §2.2, still live (`styles.xml:5-6` vs `NovalPieTheme.kt:40-41`).
2. **Cards are invisible.** In `turn38/book_detail_wrapped.png` the entire book-detail card (`surfaceVariant`-ish on `background`) reads as a slightly lighter rectangle with no edge; in `turn36/search_initial_after_proxy_fix.png` the three stacked section cards (搜索历史 / 热门标签 / 筛选) have no discernible boundary from each other or the page. → §2.7.
3. **Chip rails clip mid-glyph with no affordance.** `turn36`: the filter-summary rail shows `排序: 相关度 | 顺序: 降序 | 范围: 全部内容 | 内容: 所有 | 字…` cut at the right edge; the 热门标签 rail shows `奇幻 26779 | 同人 12805 | 现代 12255 | 后宫 10…` cut. → §8.3.
4. **The book-detail action row clips.** `turn38/book_detail_wrapped.png` (a screenshot explicitly named for a wrap fix) still shows `开始阅读 | 网页详情 | 编辑信息 | 章节…` with the fourth action cut off. → §8.3/§8.4.
5. **No hierarchy between chip types.** `turn38`: 未收藏 / 作者: 무사칼리 / 来源: 上传 / 收藏: 6 / 本站阅读: 0 / 源收藏: 0 / 已完结 / 奇幻 all render as identical outlined pills across three rows. → §5.1.
6. **Loading is bare text.** `turn39/launch.png` shows `正在加载收藏分组` and `正在加载收藏书籍` as plain left-aligned sentences with no layout reservation. → §5.2, §6.
7. **Errors are un-styled neutral cards.** `turn39/reader.png` shows `阅读器正文请求失败: 服务返回错误 500` in a neutral grey card with a `重试正文` outlined button — no error colour, no icon. That is `ErrorBlock` (`NovalPieApp.kt:3582-3583`, `containerColor = surfaceVariant`). → §5.3.
8. **The reader is a card on grey.** `turn39/reader.png` shows the failed reader as two floating cards on the grey page with 16 dp gutters — not an immersive reading surface. → §2.10.
9. **Unbranded pink surfaces appear in a blue-branded app.** In `turn36/search_initial_after_proxy_fix.png` the filter-summary pills render **pink with magenta text**, and the bottom-nav selected indicator is a **pink pill**; in `turn39/launch.png` and `turn39/home_after_fix.png` the 继续阅读 card is a **large pink block with magenta text** and the selected-tab indicator is pink. None of these colours exist in `ThemeTokens` — they are Material 3 baseline `tertiaryContainer` `#FFD8E4` / `onTertiaryContainer` family values. The specific `HEAD` composables for those elements now use `primaryContainer` (`NovalPieApp.kt:2838`, `:168`, `:3608`), so those exact pills are presumably fixed post-turn-40; **but the same leak class is still live** at `MessageScreens.kt:247` (`colorScheme.tertiary` = baseline `#7D5260` in the message hero gradient) and at the 6 `errorContainer` sites. → §1.3. The screenshots are direct evidence that leaving 16 colour roles unbranded has already put off-brand pink on screen.
10. **The turn-38 build used a text `返回` link instead of the arrow back button.** `HEAD` uses `IconButton(Icon(Icons.Filled.ArrowBack, cd="返回"))` (`NovalPieApp.kt:139-141`) — evidence the screenshots predate `HEAD`.
11. **Duplicate titles.** `turn36`: top bar `NovalPie` / `搜索`, and ~100 px below, body header `发现` / `搜索作品、作者和标签`. → §5.8.
12. **A raw `LoadResult` state is a user-facing badge.** `turn36` top-right shows a `就绪` pill. → §4.7 (`DiscoverPresentation.kt:43-46`, rendered at `NovalPieApp.kt:1579-1586`).

---

## 12. Findings ranked by user-visible impact

Ranked by how immediately and how often a real user notices. Each entry is a defect, not a proposal.

### P0 — noticed within the first 3 seconds, every launch

| # | Finding | Evidence | Blast radius |
| --- | --- | --- | --- |
| **1** | **No launcher icon at all.** No `android:icon`, no `mipmap-*`, no adaptive icon, no monochrome layer. The app appears on the home screen as the generic Android robot placeholder. | `AndroidManifest.xml:5-9`; `find app -iname "*mipmap*" -o -iname "ic_launcher*"` → empty; `res/` contains only `values/` + `xml/` | 100% of users, 100% of sessions, before the app even opens |
| **2** | **Dark-grey cold-start flash + no splash.** `styles.xml:2` inherits the **dark** platform theme `@android:style/Theme.Material.NoActionBar`, so `android:windowBackground` is dark grey while the app is light. No `core-splashscreen`, no `windowSplashScreenBackground`. | `styles.xml:2`; `MainActivity.kt:12-21` (no `installSplashScreen`); `app/build.gradle:56-73` (no splashscreen dep) | every cold start |
| **3** | **Cream/white/grey three-band seam at the top and bottom of every screen.** `statusBarColor`/`navigationBarColor` `#FFF8F4` (warm) vs top bar `#FFFFFF` vs page `#F2F2F2` (neutral). `#FFF8F4` exists nowhere in the theme. | `styles.xml:5-6` vs `NovalPieTheme.kt:40-41` vs `NovalPieApp.kt:145`, `:154`; visible in all 5 screenshots read | every screen |
| **4** | **Dark mode is visibly broken.** Cream `#FFF8F4` system bars with dark icons above/below a `#191C1F` app; no `values-night/`, no `WindowCompat`, no `enableEdgeToEdge`, nothing switches the bars. | `styles.xml:3-6`; `NovalPieTheme.kt:57`; 0 hits for `WindowCompat`/`enableEdgeToEdge`/`values-night` | every dark-mode user, every screen |
| **5** | **Dark-mode filled buttons are unreadable: white on `#4D9DFF` = 2.77:1**, below even the 3:1 non-text floor. | `NovalPieTheme.kt:50-51`; Appendix B | every primary CTA in dark mode |

### P1 — noticed within the first minute of use

| # | Finding | Evidence |
| --- | --- | --- |
| **6** | **Cards have no visible edge in either theme** (1.04:1 light, 1.27:1 dark) and no elevation scale to compensate, so every screen reads as undifferentiated flat blocks. Outlined-control borders are 1.49:1 / 1.97:1. | `NovalPieTheme.kt:40-42`, `:57-59`, `:46`, `:63`; only 1 explicit elevation in the app (`NovalPieApp.kt:729`); Appendix B |
| **7** | **The most-used text colour fails WCAG AA.** `onSurfaceVariant #7D8A97` at **103 call sites** measures 3.53:1 on white, 3.15:1 on the page background — and it is applied mostly to `bodySmall`/`labelSmall`. | `NovalPieTheme.kt:45`; 103 `colorScheme.onSurfaceVariant` occurrences; Appendix B |
| **8** | **Filled-button and text-button labels fail AA** (3.79:1 both). | `NovalPieTheme.kt:33-34` |
| **9** | **59 `LazyRow` chip rails clip content mid-glyph with no scroll affordance**, including the reader's own toolbar (`上一章 目录 下一章 A- 18sp A+ 主题 网页`) and all 6 search filter groups. `FlowRow` is used only 5 times. | `NovalPieApp.kt:2210-2219`, `:1494-1503`, `:1693-1705`, `:1637-1643`; confirmed in `turn36/search_initial_after_proxy_fix.png` |
| **10** | **No pull-to-refresh anywhere**, and it is not even installable with the current dep set (material3 1.1.0 predates `PullToRefreshBox`; the M2 `material` artifact is absent). Forum / Book Detail / Reader / Search have **no** refresh affordance at all. | 0 hits for `pullRefresh`; `app/build.gradle:60-62` |
| **11** | **No `SnackbarHost`.** Every action result is an inline card or text that shifts layout. 5 different notice/banner idioms exist instead. | `NovalPieApp.kt:127-175` (Scaffold has no `snackbarHost`); 0 hits for `Snackbar` |
| **12** | **Loading is a progress bar plus a Chinese sentence, 36 times, with no skeletons and no layout reservation** → content jump on every load. 40+ distinct `正在…` strings, 12 user-visible `等待…` idle strings. | §5.2, §4.8; `turn39/launch.png` |
| **13** | **No route transitions at all.** Navigation is a bare `when (route)`; no `AnimatedContent`/`Crossfade`/`NavHost`. Scroll position is lost on every back-navigation because there is no back-stack state. | `NovalPieApp.kt:182-580`; `rememberLazyListState` used once (`:1848`) |
| **14** | **The reader renders as a card on the grey app background.** Choosing the reader's dark or sepia palette leaves a 16 dp light-grey gutter and rounded corners around the text; the reader bars' `tonalElevation = 6.dp` is dead code so they have no separation either. | `NovalPieApp.kt:3460-3463`, `:1857`, `:2012`, `:2201`, `:3555-3560`; `turn39/reader.png` |
| **15** | **The soft keyboard covers text fields.** Zero inset handling anywhere; every comment composer, reply box and form field is affected. | 0 hits for `imePadding`/`WindowInsets`; `MainActivity.kt`; no `windowSoftInputMode` |
| **16** | **Most secondary screens show their title twice** in two different type styles, ~24 dp apart. | 8 sites, §5.8 |
| **17** | **Off-brand Material pink/maroon on screen.** Only 14 of ~30 colour roles are branded; `tertiary` (baseline `#7D5260`) is used in the message hero gradient and `errorContainer` (baseline `#F9DEDC`) in 6 error cards. Historical screenshots show large pink surfaces from the same mechanism. | `NovalPieTheme.kt:69-101`; `MessageScreens.kt:247`; 6 + 3 `errorContainer` sites; `turn36`/`turn39` screenshots |
| **18** | **Errors have two mutually inconsistent looks**, and the shared one (19 call sites) is the only one that does *not* use the error colour — so most errors look like ordinary neutral cards. | `NovalPieApp.kt:3582-3583` vs `MessageScreens.kt:649`, `WorkspaceScreens.kt:494`, `UploadScreens.kt:392` |

### P2 — noticed on inspection, on other devices, or by assistive tech

| # | Finding | Evidence |
| --- | --- | --- |
| **19** | **7 fake buttons** (`AssistChip(onClick = {})`) show ripples, announce as buttons, and do nothing — up to 8+ of them at once on the book detail screen. | `MessageScreens.kt:407`, `NovalPieApp.kt:3333`, `:3341`, `UploadScreens.kt:210`,`:211`,`:344`, `WorkspaceScreens.kt:328` |
| **20** | **Touch targets below 48 dp**: 36 dp star toggle on every inbox row; 32 dp-tall forum/comment action icons (5 per post × every post); ~16-20 dp clickable author-name texts nested inside clickable cards. 0 hits for `minimumInteractiveComponentSize`. | `MessageScreens.kt:342`; `NovalPieApp.kt:975-1000`, `:2164-2180`, `:770-774`, `:927-932`, `:1178-1182` |
| **21** | **~9 meaning-bearing icons have `contentDescription = null`**, including the only visual differentiator between the ~12 Tools route cards and the "upload succeeded" checkmark. 32 of 86 `Icon(` calls are unlabelled. | `NovalPieApp.kt:2405`, `:2847`, `:2686`, `MessageScreens.kt:660`, `UploadScreens.kt:378`, `UploadEditorScreens.kt:279`/`:323`/`:519`, `WorkspaceScreens.kt:323`/`:389` |
| **22** | **A QA marker string is exposed as a `contentDescription`** — TalkBack reads `NOVALPIE_NATIVE_COMPOSE_HOME` aloud. No `heading()` semantics on 89 section headers; no `Role.Button` on 26 custom clickables; no `testTag` anywhere. | `NovalPieApp.kt:613`, `:2904-2916` |
| **23** | **Unclipped ripples**: 8 custom `.clickable` sites have no `clip()`, so the press highlight paints as a hard rectangle across rounded content (notably every book grid card). | `NovalPieApp.kt:2963-2966`, `:2354`, `:2677`, `:3419`, `MessageScreens.kt:317`, `PoliticalExamScreens.kt:310`, `UserProfileScreens.kt:165`, `ForumCreateScreens.kt:104` |
| **24** | **No haptics** despite 3 long-press interactions. | 0 hits for `HapticFeedback` |
| **25** | **Not responsive.** Grid columns hardcoded to 2, hand-chunked rows with unstable keys, 0 hits for `WindowSizeClass`/`BoxWithConstraints`. Landscape and tablets get two enormous cards per row. | `NovalPieApp.kt:3414`, `:1297-1309`, `:1434-1447`; `AndroidManifest.xml:13` |
| **26** | **`Row(SpaceBetween)` × 60 with un-weighted `Text` next to fixed actions** → overflow on narrow screens and at large font scales. ~94% of `Text` calls have no `maxLines`, including server-controlled strings (`accountName`, `configKey`, book titles). | §8.4, §8.5 |
| **27** | **Fixed heights on buttons and panels** (`54.dp`, `50.dp`, `300.dp`, `560.dp`, `520.dp`, `720.dp`) clip at large font scales or on short screens. Four different book-cover sizes, three of them off the declared 2:3 ratio. | §8.1 |
| **28** | **Hardcoded bottom padding double-counts the `Scaffold` inset** on 4 screens (80/86/96 dp on top of the Scaffold's own), and 2 screens don't → ~160 dp of dead space, inconsistently. | `NovalPieApp.kt:614`, `:2238`, `:1951`, `PoliticalExamScreens.kt:222`, `ProfileScreens.kt:118` vs `NovalPieApp.kt:1242` |
| **29** | **Developer language in product copy**: `正在检查 /api/users/me`, `正在检查 /api/favorites/status`, `分组 id:`, `原始状态:`, `18sp`, `字号: 18sp`, `就绪`/`加载中`/`错误`, `NOVALPIE STUDIO`. Explicitly forbidden by `docs/APP2_NATIVE_DESIGN_REFERENCES.md:38`. | §4.7 |
| **30** | **Bare `WebView` fallback**: no progress bar, no error page, no title, no reload, no insets — a blank rectangle on slow loads. | `WebFallbackScreen.kt:36-58` |

### P3 — invisible to users, fatal to the refactor

| # | Finding | Evidence |
| --- | --- | --- |
| **31** | **The auth-JWT backup mitigations are dead code.** `allowBackup="true"` with the JWT in plaintext SharedPreferences and **no `android:fullBackupContent` / `android:dataExtractionRules` attribute**, even though `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` exist specifically to fix it. `adb backup` / cloud backup / device-transfer can lift a live session. | `AndroidManifest.xml:5-9`; `AuthSessionStore.kt:43-65`; `res/xml/backup_rules.xml`; `res/xml/data_extraction_rules.xml` |
| **32** | **`targetSdk 34` is below the Google Play submission floor** (35 since 2025-08-31). The app cannot be published as configured. Raising it forces mandatory edge-to-edge, which the app has zero handling for and which makes `styles.xml:5-6` inert. | `app/build.gradle:19`; §2.5, §9.3 |
| **33** | **`minifyEnabled false` in release.** No R8, no shrinking, no obfuscation; `proguard-rules.pro` is a single comment line; `material-icons-extended` ships whole. | `app/build.gradle:29`; `app/proguard-rules.pro:1` |
| **34** | **No Gradle signing config.** Release signing is a machine-specific PowerShell script reading a plaintext password file and passing it on the command line; no AAB output. | `app/build.gradle:22-32`; `tools/build-release.ps1` |
| **35** | **Zero logging, zero crash reporting.** 0 hits for `Log.`/`Timber`/`Crashlytics`/`Sentry`/`ACRA`. Failures are swallowed by `runCatching { }.getOrNull()`. Diagnosis is 500 KB hand-grepped logcat dumps. | §10.3 |
| **36** | **The debug emulator proxy ships to release and activates on any x86/x86_64 device.** | `NetworkConfigStore.kt:71-75`, `:32-47`; `WebFallbackScreen.kt:103-113` |
| **37** | **Toolchain is ~3 years stale end-to-end.** AGP 8.0.0 / Gradle 8.0.2 / Kotlin 1.8.10 / Compose 1.4.3 / Material3 1.1.0 / compileSdk 34 / coil 2.4.0 / okhttp 4.11.0. AGP 8.0.0 + compileSdk 34 emits the pairing warning on every build (README.md:1211, :1247, :1286, :1319). Material3 1.1.0 is the reason pull-to-refresh and modern components are unavailable. | §9.2, §9.3 |
| **38** | **No version catalog, no Kotlin DSL, no lint config, no detekt/ktlint, no `.editorconfig`, no CI.** 18 inline dependency coordinates, `1.4.3` repeated 5×. | §9.4 |
| **39** | **No `androidTest` source set → zero UI tests, zero screenshot tests.** All 51 unit tests are string/parsing tests that cannot observe any defect in this report. | `ls app/src` → `main`, `test` |
| **40** | **No baseline profile, no profileinstaller, no macrobenchmark.** | §10.6 |
| **41** | **10 duplicate pill implementations, 7 loading, 5 error, 5 empty-state, 4 filter-rail, 4 hero, 5 stat-cell, 5 section-header patterns; 0 shared component package; 90 lines of dead composables; `MaterialTheme.shapes` referenced 0 times against 97 inline `RoundedCornerShape`s.** Every one of these must be re-derived by hand during the refactor or a variant will be silently lost. | §5 |
| **42** | **1685 inline CJK string literals (1253 distinct, 316 `\uXXXX`-escaped) against exactly 1 string resource**, with a Chinese UI string used as a `when` control-flow key. | §4 |
| **43** | **747 hardcoded `.dp` literals across 46 distinct values, no spacing/typography/elevation/motion/icon-size scale, `typography` never passed to `MaterialTheme`.** | §1 |
| **44** | Committed JVM crash logs (`hs_err_pid137816.log`, `replay_pid137816.log`, 389 KB) despite matching `.gitignore` entries; `qa-screenshots/` — the only rendered-result evidence — is gitignored and will be lost. | repo root; `.gitignore:24` |

---

## Appendix A — full `.dp` value census

747 literals, 46 distinct values, across 14 files.

| Value | Count | | Value | Count | | Value | Count |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `8.dp` | 168 | | `24.dp` | 8 | | `40.dp` | 2 |
| `16.dp` | 90 | | `7.dp` | 7 | | `28.dp` | 2 |
| `12.dp` | 85 | | `3.dp` | 7 | | `150.dp` | 2 |
| `10.dp` | 81 | | `96.dp` | 4 | | `13.dp` | 2 |
| `14.dp` | 80 | | `22.dp` | 4 | | `100.dp` | 2 |
| `6.dp` | 54 | | `999.dp` | 3 | | `80.dp` | 1 |
| `4.dp` | 40 | | `72.dp` | 3 | | `76.dp` | 1 |
| `18.dp` | 34 | | `9.dp` | 2 | | `720.dp` | 1 |
| `20.dp` | 18 | | `86.dp` | 2 | | `560.dp` | 1 |
| `2.dp` | 16 | | `42.dp` | 2 | | `54.dp` | 1 |
| `5.dp` | 8 | | | | | `520.dp` | 1 |

remaining singletons: `50.dp`, `48.dp`, `36.dp`, `32.dp`, `300.dp`, `30.dp`, `26.dp`, `196.dp`, `15.dp`, `148.dp`, `120.dp`, `11.dp`, `104.dp`, `1.dp`, `0.dp`.

Observations:
- Both 4-multiples (4, 8, 12, 16, 20, 24) **and** non-4-multiples (6, 10, 14, 18, 22, 26, 42, 54, 86) are heavily used — 10 appears 81 times, 14 appears 80 times, 18 appears 34 times. There is no base unit.
- `999.dp` × 3 is the pill hack (`ProfileScreens.kt:310` and 2 others).
- `0.dp` × 1 is `tonalElevation = 0.dp` (`NovalPieApp.kt:155`).
- 5 of the 747 are in `NovalPieTheme.kt` (the `Shapes` definition) — the only ones that belong in a token file.
- `.sp` literals used as design values: **0**.

---

## Appendix B — contrast-ratio computations

Method: WCAG 2.1 relative luminance `L = 0.2126·R + 0.7152·G + 0.0722·B` with the sRGB linearisation `c ≤ 0.03928 ? c/12.92 : ((c+0.055)/1.055)^2.4`; contrast `= (L_light + 0.05) / (L_dark + 0.05)`.

Computed relative luminances:

| Colour | Role(s) | L |
| --- | --- | --- |
| `#FFFFFF` | light `surface`, `onPrimary`, `Color.White` | 1.0000 |
| `#FFF8F4` | `android:statusBarColor` / `android:navigationBarColor` (`styles.xml:5-6`) — **not in `ThemeTokens`** | 0.9493 |
| `#F5F7FA` | light `surfaceVariant` | 0.9285 |
| `#F2F2F2` | light `background` | 0.8877 |
| `#F0F2F5` | dark `onSurface`, `onBackground` | 0.8861 |
| `#E7F1FF` | light `primaryContainer` | 0.8808 |
| `#EDF0F2` | light `secondaryContainer` | 0.8673 |
| `#CED4DA` | light `outline` | 0.6526 |
| `#B8D6FF` | dark `onPrimaryContainer` | 0.6551 |
| `#B8C1CA` | dark `onSurfaceVariant` | 0.5262 |
| `#4D9DFF` | dark `primary` | 0.3291 |
| `#7D8A97` | light `onSurfaceVariant`, light `secondary` | 0.2478 |
| `#3182ED` | light `primary` | 0.2273 |
| `#146DE1` | light `onPrimaryContainer` | 0.1654 |
| `#45525E` | light `onSurface`, `onSecondaryContainer` | 0.0812 |
| `#4A545E` | dark `outline` | 0.0862 |
| `#2A2F34` | dark `surfaceVariant`, `secondaryContainer` | 0.0277 |
| `#23262A` | dark `surface` | 0.0191 |
| `#191C1F` | dark `background` | 0.0114 |
| `#001D3D` | dark `primaryContainer` | 0.0122 |

Resulting ratios:

| Pair | Computation | Ratio | Verdict |
| --- | --- | --- | --- |
| `onSurfaceVariant` on `surface` (light) | (1.0500)/(0.2978) | **3.53** | ❌ AA text; ✅ AA large / non-text |
| `onSurfaceVariant` on `surfaceVariant` (light) | (0.9785)/(0.2978) | **3.29** | ❌ AA text |
| `onSurfaceVariant` on `background` (light) | (0.9377)/(0.2978) | **3.15** | ❌ AA text |
| `onPrimary` on `primary` (light) | (1.0500)/(0.2773) | **3.79** | ❌ AA text; ✅ AA large |
| `primary` as text on `surface` (light) | (1.0500)/(0.2773) | **3.79** | ❌ AA text |
| `onPrimaryContainer` on `primaryContainer` (light) | (0.9308)/(0.2154) | **4.32** | ❌ AA text (marginal) |
| `onSurface` on `surface` (light) | (1.0500)/(0.1312) | **8.00** | ✅ |
| `onSecondaryContainer` on `secondaryContainer` (light) | (0.9173)/(0.1312) | **6.99** | ✅ |
| `surfaceVariant` vs `background` (light, boundary) | (0.9785)/(0.9377) | **1.04** | ❌ SC 1.4.11 (needs 3.0) |
| `surface` vs `background` (light, boundary) | (1.0500)/(0.9377) | **1.12** | ❌ SC 1.4.11 |
| `outline` on `surface` (light, border) | (1.0500)/(0.7026) | **1.49** | ❌ SC 1.4.11 |
| `onPrimary` on `primary` (**dark**) | (1.0500)/(0.3791) | **2.77** | ❌ **fails even 3.0 non-text** |
| `onSurface` on `surface` (dark) | (0.9361)/(0.0691) | **13.5** | ✅ |
| `onSurfaceVariant` on `surface` (dark) | (0.5762)/(0.0691) | **8.34** | ✅ |
| `onPrimaryContainer` on `primaryContainer` (dark) | (0.7051)/(0.0622) | **11.3** | ✅ |
| `surfaceVariant` vs `background` (dark, boundary) | (0.0777)/(0.0614) | **1.27** | ❌ SC 1.4.11 |
| `surface` vs `background` (dark, boundary) | (0.0691)/(0.0614) | **1.13** | ❌ SC 1.4.11 |
| `outline` on `surface` (dark, border) | (0.1362)/(0.0691) | **1.97** | ❌ SC 1.4.11 |
| `#FFF8F4` status bar vs `#F2F2F2` background (light) | L(`#FFF8F4`) = 0.9493; (0.9993)/(0.9377) | **1.07** | brightness step is small; the visible defect is the **hue** shift — `#FFF8F4` is warm (R 255 > G 248 > B 244, hue ≈ 24°) while `#F2F2F2` is perfectly neutral |
| `#FFF8F4` status bar vs `#191C1F` background (**dark**) | (0.9993)/(0.0614) | **16.3** | a maximally bright cream strip across the top of a near-black app |

All light/dark token values are asserted by `app/src/test/java/com/novalpie/nativeapp/ui/ThemePaletteTest.kt:12-29`, so the failing pairs are locked by the test suite.

---

## Appendix C — file inventory

### `app/src/main/res/` (4 files, 2 of them unreferenced)

| File | Lines | Referenced from manifest? |
| --- | --- | --- |
| `values/strings.xml` | 3 | yes (`android:label="@string/app_name"`) |
| `values/styles.xml` | 9 | yes (`android:theme="@style/Theme.NovalPie"`) |
| `xml/backup_rules.xml` | 11 | **NO** — dead |
| `xml/data_extraction_rules.xml` | 15 | **NO** — dead |

No `mipmap-*`, no `drawable*`, no `values-night/`, no `font/`, no `raw/`, no `anim/`, no `menu/`, no `dimens.xml`, no `colors.xml`, no `themes.xml`, no `network_security_config.xml`.

### UI source files, ranked by size (design-relevant subset)

| Lines | File | `.dp` | CJK literals |
| --- | --- | --- | --- |
| 4063 | `ui/NovalPieViewModel.kt` | 0 | 258 |
| 3654 | `ui/NovalPieApp.kt` | 271 | 192 |
| 3404 | `data/NovalPieApi.kt` | 0 | 23 |
| 849 | `ui/AdminScreens.kt` | 44 | 133 |
| 681 | `ui/MessageScreens.kt` | 80 | 84 (all escaped) |
| 655 | `model/Models.kt` | 0 | 2 |
| 600 | `ui/UploadEditorScreens.kt` | 64 | 77 |
| 500 | `ui/WorkspaceScreens.kt` | 50 | 90 (all escaped) |
| 465 | `ui/ProfileScreens.kt` | 39 | 46 |
| 465 | `ui/BookEditScreens.kt` | 23 | 65 |
| 418 | `ui/PoliticalExamScreens.kt` | 48 | 38 |
| 409 | `ui/UploadScreens.kt` | 53 | 56 |
| 377 | `ui/BookChapterScreens.kt` | 20 | 65 |
| 375 | `ui/ForumCreateScreens.kt` | 25 | 35 |
| 250 | `ui/UserProfileScreens.kt` | 16 | 26 |
| 220 | `ui/ProductCopy.kt` | 0 | 121 |
| 176 | `ui/WebFallbackScreen.kt` | 0 | 0 |
| 164 | `ui/ImagePreviewDialog.kt` | 9 | 8 |
| **118** | **`ui/NovalPieTheme.kt`** | **5** | **0** |
| 38 | `ui/UiNavigation.kt` | 0 | 27 (all escaped) |
| 30 | `ui/VisibleUiLabels.kt` | 0 | 14 |
| 22 | `MainActivity.kt` | 0 | 0 |

The design system (118 lines) is 1.6% the size of the largest screen file.

### Build / tooling files

| File | Lines / size | Notes |
| --- | --- | --- |
| `build.gradle` (root) | 4 | AGP 8.0.0, Kotlin 1.8.10 |
| `app/build.gradle` | 74 | full inventory in §9.1-9.2 |
| `settings.gradle` | 15 | `FAIL_ON_PROJECT_REPOS`, google + mavenCentral |
| `gradle.properties` | 6 | §9.1 |
| `gradle/wrapper/gradle-wrapper.properties` | 6 | Gradle 8.0.2 from a Tencent mirror, no checksum |
| `app/proguard-rules.pro` | 1 (comment only) | §10.1 |
| `tools/build-release.ps1` | 113 | §10.2 |
| `tools/verify-native-project.ps1` | — | manual structural check |
| `tools/verify-mumu-compose-launch.ps1` | — | manual emulator smoke test |
| `README.md` | 1361 lines / 79 KB | turn-by-turn changelog to Turn 42; AGP/compileSdk warning noted at :725, :1211, :1247, :1286, :1319 |
| `docs/APP2_NATIVE_DESIGN_REFERENCES.md` | 87 | the only design-intent doc; :38 forbids debug labels in UI (violated 4×); :57 requires layout reservation during load (not implemented, §5.2); :46 requires taps not to misfire while scrolling (nested clickable/LazyRow conflicts, §8.3) |
| `docs/LIVE_SITE_ROUTE_API_MATRIX.md` | 1022 | route/API parity matrix |
| `docs/REFACTOR_PLAN_2026-07-26.md` | 267 | added by `HEAD` |
| `hs_err_pid137816.log` | 79 KB | **committed** despite `.gitignore:14` |
| `replay_pid137816.log` | 309 KB | **committed** despite `.gitignore:15` |
