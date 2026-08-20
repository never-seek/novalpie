# NovalPie 2.0 — Android client

A native Android reader for [novalpie.cc](https://novalpie.cc): browsing and searching the catalogue,
reading chapters, the forum, private messages, uploading and editing EPUBs, and the site's
administration tools. Kotlin and Jetpack Compose throughout; the WebView is a fallback, not the app.

The interface is Simplified Chinese only, deliberately — see [Localisation](#localisation).

---

## Getting started

You need JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).

```bash
cd D:\NovalPie\native-android
```

```bash
GRADLE_USER_HOME=D:\NovalPie\.gradle-sandbox ./gradlew :app:assembleDebug
```

`GRADLE_USER_HOME` points at a sandbox cache used by this project. Without it Gradle uses the
default `~/.gradle` and re-downloads everything, which works but is slow.

Common tasks:

```bash
GRADLE_USER_HOME=D:\NovalPie\.gradle-sandbox ./gradlew :app:testDebugUnitTest
```

```bash
GRADLE_USER_HOME=D:\NovalPie\.gradle-sandbox ./gradlew :app:assembleRelease
```

A cold build takes 5–10 minutes on a typical machine. Release builds run R8, which takes longer
still.

### Release signing

`assembleRelease` produces an **unsigned** APK unless you create `signing.properties` in the project
root. That file is gitignored and never committed:

```properties
storeFile=../keystore/novalpie.jks
storePassword=...
keyAlias=novalpie
keyPassword=...
```

---

## Architecture

```
app/src/main/java/com/novalpie/nativeapp/
├── MainActivity.kt          entry point: splash, edge-to-edge, deep links
├── model/Models.kt          data classes for every API response
├── data/                    network, persistence, file formats
│   ├── NovalPieApi.kt       ~130 endpoints and their response normalisers
│   ├── NovalPieApiException.kt
│   ├── NetworkConfigStore.kt    proxy settings and route selection
│   ├── AuthSessionStore.kt      the site JWT
│   ├── *Store.kt                reader progress, settings, search history, drafts
│   ├── Epub{Parser,Writer}.kt   EPUB import and export
│   └── EditorProcessor.kt       chapter splitting and text transforms
└── ui/
    ├── NovalPieApp.kt       route dispatch and the core screens
    ├── NovalPieViewModel.kt all application state and actions
    ├── NovalPieTheme.kt     assembles the design system
    ├── design/              the design system (see below)
    ├── *Screens.kt          admin, messages, workspace, upload, exam, book editing
    └── *Presentation.kt     pure functions turning models into display text
```

### How a screen gets its data

There is a single `NovalPieViewModel`. It owns one state object per feature area (`HomeState`,
`ForumState`, `ReaderState`, …), calls `NovalPieApi` directly, and persists through the `*Store`
classes. `NovalPieApp.kt` reads that state and passes callbacks down.

**This is not the intended end state.** `NovalPieViewModel.kt` is ~4000 lines and `NovalPieApi.kt`
~3400, and splitting them is planned work — see
[the refactor plan](docs/REFACTOR_PLAN_2026-07-26.md). Until then, expect to work in large files.

### Navigation

Routes are an `AppRoute` sealed class held in a `mutableStateListOf` stack inside the ViewModel, not
Navigation-Compose. `goBack()` pops; `openTab()` resets. There is one shared stack rather than one
per tab, so switching tabs discards the previous tab's history.

Deep links use the `novalpie://app` scheme and currently handle `/book/{id}`,
`/book/{id}/{chapterId}` and `/user/{id}`.

### The design system

`ui/design/` is where visual decisions live. Screens consume it and should not invent their own
colours, sizes or type.

| File | Contents |
|---|---|
| `NovalPieColors.kt` | All 36 Material 3 colour roles, light and dark. `NovalPieColorTokens` has no default values, so the compiler rejects a scheme that forgets a role. |
| `NovalPieType.kt` | The type scale, with line heights tuned for Chinese text (~1.6 for body, against Material's Latin-oriented 1.43). |
| `NovalPieTokens.kt` | Spacing, radius, elevation, size and motion scales. |
| `NpChip.kt` | Metadata chips. Colour is semantic via `NpChipTone`; `NpChipRow` wraps rather than scrolling. |
| `NpStates.kt` | `NpErrorState`, `NpEmptyState`, `NpSkeleton`. |
| `NpComponents.kt` | `NpCard`, `NpSectionHeader`, `NpSearchField`. |

Two rules carry real weight:

- **Never hardcode a colour or dimension.** Use a role and a token. `ColorContrastTest` computes
  WCAG contrast ratios and fails the build if a pair drops below AA, which is what caught the
  previous palette's 3.53:1 body text.
- **Colour must mean something.** `NpChipTone` exists because chips used to be coloured
  per-call-site, so a search result showed `上传` in grey beside `已完结` and `奇幻` in another
  colour, signifying nothing.

### The reader protocol

Chapter content uses a signed session the site enforces, reverse-engineered from its JavaScript:

1. `GET /api/reader/session-key` with `X-Client-Signature`, `X-Client-Timestamp`, `X-Client-Nonce`
2. `GET /api/chapters/{id}/content?session=…`
3. Decrypt with `AES/GCM/NoPadding`, where `aesKey = SHA-256(base64Decode(session_key))`

It uses a **custom base64 alphabet** and a rotate-left step over the timestamp. All of it lives in
`NovalPieApi.kt` (`readerSignatureHeaders`, `decryptReaderContent`). Changing any detail breaks
chapter reading outright, so treat this code as fixed unless the site changes.

### Response normalisation

The server returns inconsistent shapes for the same data — an array might arrive as `results`,
`novels`, `list` or `records`; a title as `title`, `true_name` or `original_title`. The ~90
`normalize*()` functions in `NovalPieApi.kt` accept every observed alias.

**Those alias lists are a feature, not redundancy.** Trimming one to tidy the code will silently
blank a field for some responses.

---

## Networking

By default the app talks to the site directly. A proxy can be configured in Settings, and on an
**emulator** two development proxies (`127.0.0.1:7890`, then `10.0.2.2:7890`) are tried before the
direct route. `isEmulatorRuntime()` gates that on `Build` markers.

On a real device there is no proxy fallback. This matters: the fallbacks were previously applied
everywhere, so every request on a real phone stalled on an unreachable emulator address for the full
12-second connect timeout before falling through.

For emulator QA, forward the port first:

```bash
adb reverse tcp:7890 tcp:7890
```

---

## Testing

```bash
GRADLE_USER_HOME=D:\NovalPie\.gradle-sandbox ./gradlew :app:testDebugUnitTest
```

274 tests across 53 suites. Three kinds:

- **MockWebServer contract tests** cover 103 of 106 API functions, asserting request shape and
  response normalisation. These are the safety net for changing the data layer.
- **Presentation tests** pin the exact Chinese text pure helper functions produce.
- **Robolectric tests** cover anything touching Android classes — including `org.json`, which ships
  in the platform and is stubbed to throw under a plain JVM runner.

There are **no UI tests yet**; no Composable is currently exercised by a test.

### The string golden master

```bash
py tools/golden_strings.py
```

This extracts every user-visible string in the app and fails if one disappeared. It exists because
the ongoing refactor moves a lot of code, and a dropped label is easy to miss and hard to notice.

Adding strings is fine. Removing one fails, and if the removal is intentional it must be justified
in [`tools/golden/REMOVALS.md`](tools/golden/REMOVALS.md) — never silently rebaselined.

---

## Localisation

The app is Simplified Chinese only, on purpose. Strings live inline in Kotlin rather than in
`res/values/strings.xml`, and the `MissingTranslation` lint check is disabled.

This is a deliberate trade, not an oversight: about 40 presentation helpers are pure functions
returning Chinese text, pinned by fast unit tests. Externalising would force an Android `Context`
into all of them and convert those tests to Robolectric, in exchange for a multi-language capability
nobody wants. If the app ever needs a second language, that calculus changes.

Strings were previously written as `\uXXXX` escapes, which is what made the files unreadable. They
are now plain characters. If escapes reappear:

```bash
py tools/decode_unicode_escapes.py --check
```

One trap that tool handles: Kotlin identifiers may contain CJK letters, so `"$label已同步"` parses as
a variable named `label已同步`. Templates followed by a CJK letter need braces — `"${label}已同步"`.

---

## Documentation

| Document | What it is for |
|---|---|
| [docs/REFACTOR_PLAN_2026-07-26.md](docs/REFACTOR_PLAN_2026-07-26.md) | The current refactor: diagnosis, phases, decisions, progress |
| [docs/inventory/](docs/inventory/README.md) | ~13,000 lines cataloguing every route, endpoint, screen element and string, plus ranked correctness and design findings. The parity contract for the refactor. |
| [docs/inventory/07-bugs.md](docs/inventory/07-bugs.md) | 33 ranked correctness findings with reproduction detail |
| [docs/LIVE_SITE_ROUTE_API_MATRIX.md](docs/LIVE_SITE_ROUTE_API_MATRIX.md) | Website routes and API shapes observed from the live site |
| [docs/history/](docs/history/) | The former README: a turn-by-turn build log from June–July 2026 |

The build log is kept for provenance — APK hashes, runtime evidence — but it is **not** a reliable
description of the app. It records builds passing at commits where the tree did not compile.

---

## Known gaps

Honest list, so nobody rediscovers these the hard way:

- `NovalPieViewModel.kt` and `NovalPieApi.kt` are far too large.
- No UI tests.
- The forum's `全部 / 书评 / 章节 / 动态` tabs are `onClick = {}` although `GET /api/posts` accepts a
  matching `type` parameter.
- `/login`, `/register` and `/reset-password` have no native screen; sign-in goes through the
  WebView fallback, which is also the only way an auth token is captured.
- Deep links ignore `/forum/{id}`, `/messages` and the admin routes.
- The auth JWT is stored in plain `SharedPreferences` (excluded from backup, but not encrypted).
- Layouts assume a phone: grid columns are hardcoded to 2, with no `WindowSizeClass` handling.

More detail, with file and line references, in
[docs/inventory/07-bugs.md](docs/inventory/07-bugs.md) and
[docs/inventory/08-design-and-toolchain-audit.md](docs/inventory/08-design-and-toolchain-audit.md).
