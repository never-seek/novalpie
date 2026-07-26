# Refactor inventory — the parity contract

These files were produced by reading the pre-refactor source tree (commit `fc1d555`) in full.
They exist for one reason: the refactor described in
[../REFACTOR_PLAN_2026-07-26.md](../REFACTOR_PLAN_2026-07-26.md) restructures three god-files,
relocates ~1236 user-visible strings, and rebuilds every screen. Anything not written down
here can be lost silently.

**Treat these as the contract.** When a Phase 6 slice rebuilds a screen, the corresponding
inventory section is the checklist: every control, label, conditional and rendered field listed
there has to exist in the rebuilt screen. Paired with `tools/golden_strings.py`, which fails on
any removed string, this is what makes "retain every feature and content element" verifiable.

| File | Contents |
|---|---|
| [01-routes-navigation.md](01-routes-navigation.md) | Every `AppRoute`, bottom tab, the full route→screen dispatch with per-screen callback counts, back-stack semantics, deep links, top/bottom-bar visibility rules, WebView fallback URLs |
| [02-viewmodel-state-actions.md](02-viewmodel-state-actions.md) | Every state holder and state class field, every public action grouped by domain, the request-serial staleness mechanism, the persistence stores, and a proposed domain decomposition |
| [03-api-and-models.md](03-api-and-models.md) | All ~130 endpoints with methods/paths/params, the request plumbing, the reader signed-session + AES-GCM protocol, chunked upload, every `normalize*()` with its full JSON field-alias lists, every model class |
| [04-screens-core.md](04-screens-core.md) | Forum feed & post detail, Collection/bookshelf, Discover/Search, Book detail, Reader — structure, every visible string, every control and conditional |
| [05-screens-account.md](05-screens-account.md) | Tools centre, Profile, Settings, user profile detail, the four Message screens, WebView fallback |
| [06-screens-authoring.md](06-screens-authoring.md) | Workspace, Upload, EPUB editor, Political exam, Admin (6 sections), Book edit info, Chapter manager, Forum create, plus the EPUB/editor engines behind them |
| [07-bugs.md](07-bugs.md) | 33 ranked correctness findings with file:line, failure scenario and severity — the Phase 7 work list |
| [08-design-and-toolchain-audit.md](08-design-and-toolchain-audit.md) | Design-token inventory, the unbranded-colour-role leak, accessibility and layout robustness, dependency currency, production-readiness gaps |
| [09-tests-and-build.md](09-tests-and-build.md) | Per-file test coverage, what is and is not covered, the MockWebServer endpoint contracts, the offline build requirement, and what `tools/verify-native-project.ps1` asserts |
| [10-gap-analysis.md](10-gap-analysis.md) | Cross-check for uncovered files, contradictions between sections, and features at risk of being missed |

## Caveats

- Line numbers refer to the tree at `fc1d555` and drift as the refactor proceeds.
- Section 08 cites `qa-screenshots/turn36`–`turn39`. Those PNGs predate the current blue-grey
  palette: the large pink surfaces they show came from an older rose palette and the composables
  in question now use `primaryContainer`. The *mechanism* they illustrate is still live, just on
  fewer surfaces — see the note in §1.3 and item 9 of that file.
- `docs/LIVE_SITE_ROUTE_API_MATRIX.md` is stale where it lists workspace, upload, political exam
  and admin as awaiting native migration; all are native today. Phase 9 corrects it.
