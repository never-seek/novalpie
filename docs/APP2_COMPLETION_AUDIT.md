# NovalPie App 2.0 Completion Audit

Last updated: 2026-08-17 (Turn 179 Collection picker interaction verification)

This is a requirement-to-evidence index for the native App 2.0 rebuild. It is deliberately not a
release sign-off: visual acceptance and the remaining physical-device cases still need evidence.

## Turn 168 - Book Detail actions, freshness, and scalable terminology

- The preserved public mobile source contract exposes a book-level terminology action. Its first
  request is `GET /api/terminologies?novel_id=<id>&keyword=&page=0`; the zero-based response
  returns `content`, `page`, `size`, `total`, and `totalPages`. A sample public book has 16,507
  entries, so Native now uses a keyed `LazyColumn`, explicit keyword submission, server-page load
  more, and total-aware progress instead of attempting to render the whole glossary in one
  composition. Entry cards preserve source name, target name, description, lock state, enabled
  state, and update time. Editing/import/export remains an explicit webpage-management action
  until those write contracts receive the same source audit.
- Book Detail now has a real source-backed add/remove favourite action with an isolated loading
  state, native terminology entry point, and source-style `更多 -> 分享` Android share sheet.
  Authenticated users also receive `更多 -> 网页下载 EPUB`, which deliberately opens the first-party
  page inside the existing blob-download bridge rather than reconstructing EPUB content or
  duplicating embedded illustrations. The ordinary `网页详情` action remains separate.
- Detail requests now carry an independent request serial. It prevents an older A -> B -> A detail
  response from overwriting the newest A state after rapid navigation. The glossary has its own
  serial and accepts results only while its matching route/book remains current.
- The user-requested Collection and Upload Books work remains intact: persisted 2/3/4-column
  grids, row-normalised tag space, reserved title/author slots, long-press selection, and disabled
  management-cover preview. Search, Book Detail, and reader illustrations still require a
  stationary long press for enlargement.
- Verification: `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed: 74 suites / 482 tests / 0 failures / 0 errors / 0 skipped. Lint reports
  `No issues found`; `git diff --check` passed. Debug APK SHA-256:
  `6583605CBE2B82963ABF8C77980272DB89BE4651AD3767C49FAF63D6C1165A17`.
- MuMu is visible as `127.0.0.1:49792 offline`. No reconnect, installation, app-data reset,
  proxy, login, cookie, or browser-session mutation was attempted. Runtime evidence pending the
  existing transport becoming online: Book Detail favourite/terms/more menu, glossary search and
  paging, Collection 2/3/4 columns, and Upload Books 2/3/4 columns.

## Turn 166 - forum, review, and comment badge visual parity

- The captured current review feed includes structured `authorBadges` data, not only text labels:
  each record can carry `id`, `name`, `badge_html`, and `badge_css`. Native had discarded the
  visual fields and rendered every forum badge as the same plain chip. Forum posts, Forum Detail
  comments, Book Detail reviews, and reader chapter comments now retain the records in
  `authorBadgeVisuals` and render them through the existing safe native badge mapper. It derives
  colors, gradients, border, dot, and supported artwork properties without executing source
  HTML/CSS; labels remain the fallback for old API shapes.
- API regression tests cover visual-metadata preservation for forum posts/comments and book
  comments. Presentation coverage verifies that source artwork wins over duplicate text labels
  and that an unmatched label stays visible as a fallback.
- Full `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed: 73 suites / 476 tests / 0 failures / 0 errors / 0 skipped. Lint
  reports `No issues found`; `git diff --check` passed.
- Source evidence: `D:\NovalPie\native-android\qa-artifacts\turn115-forum-review-source\book-reviews-0.json`.
  MuMu remains `127.0.0.1:49792 offline`; no reconnect, install, proxy, data, login, or browser
  session mutation was attempted. Live capture remains pending the existing transport recovering.

Latest Debug APK SHA-256: `1F2930C06CCD94832051CD1EDAA3238D185F406F92F2697EE7FF8E3E5F09C0B9`.

## Turn 165 - forum detail first-window comment parity

- The live mobile forum detail first request is `GET /api/posts/{postId}/comments?page=1&limit=100`.
  Native had inherited the generic 20-row list size, which could make a busy forum topic look as
  if later replies had vanished. The API default and `loadForumPostDetail` now use the source's
  100-comment first window; recursive reply normalization remains unchanged.
- The API contract test covers the default request path plus its `page=1` and `limit=100` query
  values. The complete Release verification passed: 73 suites / 473 tests / 0 failures / 0
  errors / 0 skipped; `lintDebug` reported `No issues found`; Debug APK assembled; and
  `git diff --check` passed.
- MuMu remains visible as `127.0.0.1:49792 offline`. No reconnect, install, proxy, data, login,
  browser-session, or orientation mutation was attempted. Runtime forum-comment recapture is
  pending the existing device transport becoming online.

Latest Debug APK SHA-256: `D292938D2ADEEFF17B99FA00C0CAC049261C08BE87CCD57CD1DE54E7E5FC81A2`.

## Turn 164 - collection parity and stationary-long-press cover policy

- A fresh public mobile `412 x 915` audit of `/favorites` confirmed the source-visible Collection
  hierarchy: search, six compact actions (layout, groups, selection, cache, clear cache, sort),
  My Favorites / Reading History tabs, and the login-to-search empty state. Existing native
  Collection already covers those actions, plus the requested persisted 2/3/4-column choice,
  group display modes, pagination, pinning, and batch move/remove/history actions.
- Collection and the owner Upload Books shelf keep their existing row-normalised tag area and
  fixed title/author slots at every 2/3/4-column density. Long-pressing a Collection card enters
  batch management; its cover remains non-previewable so it cannot compete with selection or
  navigation. Upload-management covers are also non-previewable.
- User gesture policy now wins over the source's direct-cover viewer: Book Detail, Search, and
  reader illustrations accept only a stationary long press for zoom. A normal tap never opens an
  image dialog, and movement beyond touch slop cancels the pending preview. The obsolete
  tap-preview enum branch was removed and the policy has a focused regression assertion.
- Verification: focused Collection/Profile/Cover settings tests passed. Full
  `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed with 73 suites / 472 tests / 0 failures / 0 errors / 0 skipped. Lint
  reports `No issues found`; `git diff --check` passed.
- MuMu still reports `127.0.0.1:49792 offline`; no reconnect, install, proxy, rotation, app-data,
  or account/browser-session change was attempted. Runtime gesture screenshots remain the next
  non-mutating check when the existing transport becomes usable.

Source evidence: `D:\NovalPie\.playwright-cli\page-2026-08-16T19-25-45-623Z.png` and
`D:\NovalPie\.playwright-cli\page-2026-08-16T19-24-55-291Z.yml`.

Latest Debug APK SHA-256: `925B259EFE1C3E1F665347311334D4E9E4BC7A03B21CCBE3219F7724FBFF6465`.

## Turn 163 - mobile-source cover preview, review, and offline-reader closure

- Fresh public mobile captures at `412 x 915` verified that `/search?sort_by=relevance` opens with
  a compact Rules card, 2-column cover feed, real default results, and source pagination. Native
  already loads the same unfiltered feed when Discover opens and preserves the compact phone rule
  layout rather than treating an empty query as an empty screen.
- A public mobile audit of `/book/95654` verified `GET /api/novels/95654/detail`,
  `GET /api/v2/novels/95654/chapters`, and
  `GET /api/comments?type=book&book_id=95654&page=1&limit=30`. The source book-detail cover opens
  a full-image viewer with fit, zoom, reset, and close controls. Native Book Detail uses the
  user-requested stationary-long-press-only gesture; management shelves use the explicit `Disabled`
  preview policy so selection/navigation stays reliable.
- Book Detail now requests the source's 30-review first page rather than the generic 20-row page.
  Nested replies continue through the existing comment normalizer and thread renderer.
- Reader chapter bodies retain their bounded, variant-aware local cache and now expose a real
  `设置 -> 其他 -> 清除本书离线缓存` action. It clears only this book's disk copies, preserves the
  visible in-memory chapter, and resets directory cache indicators without touching downloads,
  source data, account state, favourites, or reading progress.
- A fresh public reader capture of `/book/95654/1419162` confirmed the current source's centered
  title/status rails, generous paragraph rhythm, continuous chapter window, and white right-side
  vertical control rail. Native already uses that structure. The source image tap opens its rail;
  Native keeps stationary-long-press full-image preview as the requested mobile convenience
  without turning an ordinary reader-body tap into accidental zoom.
- The root Compose route dispatcher exceeded the JVM 64 KB generated-method limit after this
  feature set grew. The Reader route callback surface was isolated into `ReaderRoute`, restoring
  both Debug and Release compilation without changing navigation behavior.
- Verification: targeted Debug API/cover/cache/reader tests passed; full
  `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed with 73 suites / 472 tests / 0 failures / 0 errors / 0 skipped. Lint
  reports `No issues found`; `git diff --check` passed.
- `adb` was not on the shell PATH; the discovered SDK executable reports MuMu
  `127.0.0.1:49792 offline`. No installation, reconnect, rotation, proxy, app-data, or login-state
  mutation was attempted. A temporary browser login inspection reached a Cloudflare challenge, but
  the local OpenBrowser helper refused its connection; no authenticated session or browser storage
  was persisted by this turn.

Source evidence: `D:\NovalPie\.playwright-cli\page-2026-08-16T18-45-57-543Z.png`,
`D:\NovalPie\.playwright-cli\page-2026-08-16T18-58-28-989Z.png`, and
`D:\NovalPie\.playwright-cli\page-2026-08-16T18-57-54-131Z.png`; reader references:
`D:\NovalPie\.playwright-cli\page-2026-08-16T19-14-47-449Z.png` and
`D:\NovalPie\.playwright-cli\page-2026-08-16T19-15-46-912Z.png`.

Latest Debug APK SHA-256: `53C714175C8825A5FF555625C19E031538EC0C65FBEC3E76429AB842CFCAEF73`.

| Requirement | Current state | Strongest evidence |
| --- | --- | --- |
| Native, mobile-first product UI rather than a website wrapper | Implemented and repeatedly run on MuMu | Forum, Collection, Search, Profile, Tools, Detail and Reader screenshots in `qa-screenshots/turn44-*` through `turn106-*` |
| Current source route coverage | Implemented; no current route drift | Live build/router audit in `qa-artifacts/turn102-live-source-audit`; `WebsiteDeepLinkRouteTest` |
| Administrator pages restricted to administrators | Implemented and runtime audited | Exact `role == admin` route guard, Turn 90 admin route evidence |
| Full book cards: original cover, author, source, status, tags, favourites, reads, word count | Implemented and live verified | Search cards in `turn101-book-card-gestures`; card model/presentation tests |
| Stationary long-press full-image preview | Implemented and regression-covered | `BookCoverFallbackTest` and Turn 164 source audit |
| Inline reader illustration full-image preview | Implemented and live verified | `turn86-reader-regression/reader-illustration-preview.png` and Turn 57 evidence |
| Reader typography, controls, catalog, Back behavior | Implemented; newest source-style rail awaits MuMu visual recapture | Turn 57, Turn 86, Turn 99 screenshots, Turn 162 source capture, and reader tests |
| Book reviews show and expose source actions | Implemented and live verified | `turn106-book-reviews/book-reviews-loaded.png` |
| Own-upload book search by title, author, tag | Implemented and live verified | Turn 104 screenshots; `ProfilePresentationTest` |
| Account first visit prioritises actions over a large edit form | Implemented and live verified | Turn 112 MuMu profile hub, collapsed editor and verification evidence |
| Search rules/tags/word-count and compact mobile layout | Implemented and live verified | Turn 94 and Turn 101 evidence; `DiscoverPresentationTest` |
| Network/proxy, sharp cover delivery, full tag data | Implemented and live verified | Turn 93, Turn 101, and Turn 108 live result screenshots |
| Back-stack freshness and scroll preservation | Implemented and live verified | Turn 98 Collection/Search, Turn 100 Forum, Turn 101 Search |
| Background process reclaim from Reader | Implemented and live verified | `turn99-reader-lifecycle` |
| Rapid gesture / accidental-navigation regressions | Implemented and live verified | Turn 100 Forum, Turn 101 search cards, Turn 103 bottom navigation |
| Managed-book edit/save flow | Implemented; contract and confirmation verified | Turn 105 screenshot plus `NovalPieApiTest.managedBookInfoPermissionsAndSaveUseWebsiteContracts` |

## Turn 157 - mobile search-rule source parity

- A fresh public `412 x 915` capture of `/search?sort_by=relevance` showed the mobile source
  keeps rules vertically stacked: labels are outside the bordered selector and each selector is
  compact. The old native paired form compressed the labels on portrait phones, especially
  `内容筛选`.
- Native now stacks rules below a 520dp content width, uses the source-sized 32dp selector row,
  and restores paired rows only on a wide enough window. The value border no longer spans the
  entire rule row.
- A live `/forum?tab=review` source audit showed 22,793 review entries and direct book-detail
  links with reviewer, badge, cover, reply, interaction, and time content. Existing native
  review-feed mapping remains aligned with this current shape.
- `DiscoverPresentationTest` passed 14/14; `assembleDebug` and `lintDebug` passed with no lint
  issues. MuMu ADB was offline, so no device screenshot is claimed.

Evidence: `D:\NovalPie\.playwright-cli\page-2026-08-16T15-28-52-955Z.png` and
`D:\NovalPie\.playwright-cli\page-2026-08-16T15-32-06-715Z.yml`.

## Turn 158 - mobile search vocabulary parity

- Source-visible Rules labels were aligned exactly: `搜索范围`, `内容筛选`, `来源`, `搜索模式`,
  `排序方式`, and `排序方向`. The display adds the source-style full-width colon while keeping the
  existing request-value mappings stable.
- The old fallback filter dispatcher now uses the same vocabulary, so an alternate presentation
  route cannot misroute the adult-content, sort, scope, or match-mode changes after a label update.
- `DiscoverPresentationTest` passed 14/14; `ProductCopyTest` passed 11/11; the full Release suite
  passed 72 suites / 464 tests / 0 failures / 0 errors / 0 skipped; `assembleDebug` and `lintDebug`
  passed with no lint issues. MuMu exposes an offline ADB transport at
  `127.0.0.1:49792`, so no device screenshot is claimed.

Latest Debug APK SHA-256: `6512E5D8ADC4B4D12BF3AF4094C713C762C444EDF229825BE2D2256098CB1850`.

## Turn 160 - collection long-press management and grid consistency

- Collection keeps its source-backed tabs, groups, display modes, sort, history, paging, pin and
  batch management flows. A stationary long press on either a grid card or list row now enters
  batch-selection immediately and selects that book; later long presses toggle selection. The
  existing action row then exposes move/remove (or history deletion) without forcing navigation
  to the detail page.
- The collection's 2/3/4-column picker remains persisted through `FavoritesSettingsStore`; the
  owner Upload Books shelf has its independent 2/3/4-column setting through
  `ProfileBooksSettingsStore`. Both reuse the same fixed title, author, metrics and row-maximum
  tag footprint, so a tag-rich card cannot make its neighbour lose its title or author line.
- Collection and Upload Books explicitly use `CoverPreviewPolicy.Disabled`. Enlarged cover preview
  remains limited to a stationary long press in Search, Book Detail, and reader illustrations;
  scroll movement cancels the preview gesture.
- Verification: focused `LibraryPresentationTest` passed, then `:app:testReleaseUnitTest`,
  `:app:lintDebug`, and `:app:assembleDebug` all passed. Release result: 72 suites / 465 tests /
  0 failures / 0 errors / 0 skipped. Lint reports `No issues found.`
- MuMu's only detected transport was `127.0.0.1:49792 offline`; no reconnect, app-data clear,
  installation, proxy change, or browser-session mutation was attempted while it is unavailable.

Latest Debug APK SHA-256: `3596EA7361DFBE5DA1104FCB2AF645616747CEBA5160A044987E68B093374404`.

## Turn 162 - source-style reader side drawer and action rail

- Re-read the existing public `412 x 915` mobile reader capture. The source uses a compact
  title/status rail, a left full-height content drawer, and a single right full-height action rail:
  `关闭`、`帮助`、`目录`、`设置`、`主题`、`上章`、`下章`、`滑动`、`听书`、`全屏`、`导航`.
- Native no longer renders the old horizontal bottom reader toolbar. A stationary centre tap now
  reveals the right rail; the same-target double-tap guard still keeps it visible, a different
  stationary body tap dismisses it, and a drag remains a scroll. The rail opens full-height
  left-side Directory, Help, Settings/Theme, and Navigation panels without letting a panel-close
  tap fall through to the article.
- The directory now uses the native reader format (`第N章`, compact `K`, image count) inside the
  source-style drawer. Navigation keeps the two non-duplicated escape routes distinct: `书本页`
  returns to native detail and `网页正文` opens the existing web fallback. Favourite remains in
  Navigation, while the gear retains all eight categorised local reader settings. The fullscreen
  action now controls Android system bars and restores them when the reader leaves.
- The requested Collection and Upload Books work remains intact: independent persisted 2/3/4
  columns, row-normalised tag/card height, title and author in every density, batch selection by
  stationary long press, and no cover-preview gesture on those management shelves. Search,
  detail, and reader illustrations remain stationary-long-press-only preview targets.
- Verification: `ReaderPresentationTest` passed after the control migration. Full
  `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed: 72 suites / 466 tests / 0 failures / 0 errors / 0 skipped; lint says
  `No issues found`; `git diff --check` passed.
- MuMu currently exposes only `127.0.0.1:49792 offline`. No reconnect, installation, proxy
  mutation, app-data change, browser-session access, or screenshot claim was made. The new reader
  layout should be visually recaptured as soon as that existing ADB transport becomes usable.

Source evidence: `D:\NovalPie\native-android\.playwright-cli\page-2026-08-16T17-00-41-453Z.png` and
`D:\NovalPie\native-android\.playwright-cli\page-2026-08-16T17-02-11-303Z.png`.

Latest Debug APK SHA-256: `BA75B91930F33CC6868282FDBB5BEEF37BE3A61D537C23A3D6AD1379C435E146`.

## Turn 161 - live book-detail and chapter-directory parity

- Read-only source verification on public book `95654` confirmed the current web page calls
  `GET /api/novels/95654/detail` and `GET /api/v2/novels/95654/chapters`. The detail payload
  carries `fontNumber`, native `siteReadCount`, original-platform `novelRead`, `recommend`, and
  `sourceFavoriteCount`; the directory carries `chapter_number`, `word_count`, `image_count`, and
  `updated_at` for every row.
- Native now maps those missing detail fields. Book Detail presents the source order for native
  favourite count, native reads, native recommendations, original-platform reads, and
  original-platform favourites. `fontNumber` is a numeric word-count fallback, so it no longer
  disappears when the source's display-only `wordCount` is a string such as `129万字`.
- The reader and Book Detail now use the source's v2 chapter route. Directory rows show title,
  `EP.<number>`, source-style date, compact word count, and image count (`1图`). The source does
  not return a per-user cache state on this public endpoint, so Native deliberately does not fake
  the web page's `无缓存` marker.
- Verification: focused API/detail tests passed, then `:app:testReleaseUnitTest`, `:app:lintDebug`,
  and `:app:assembleDebug` passed. Release result: 72 suites / 466 tests / 0 failures / 0 errors /
  0 skipped. Lint reports `No issues found.`
- MuMu remains `127.0.0.1:49792 offline`; no install, reconnect, proxy change, app-data clear, or
  browser-session modification was performed.

Source evidence: `D:\NovalPie\native-android\.playwright-cli\page-2026-08-16T16-33-26-607Z.yml` and
`D:\NovalPie\native-android\.playwright-cli\page-2026-08-16T16-43-53-073Z.png`.

Latest Debug APK SHA-256: `C74D80DDB399D84D2452F12E1C22A467DCE091D4B9ECB6686A3FC996A5C294B8`.

## Turn 149 - current live source and image-preview revalidation

- A cookie-free live fetch on 2026-08-14 extracted the current public Nuxt router. Its 29 unique
  path patterns continue to map to native routes, including all book/editor pages and the six
  administrator-only pages. The current entry artifacts are in
  `qa-artifacts/turn149-live-source-route-audit/`.
- A 412 x 915 public source `/search` capture confirmed the compact source search card,
  Rules/Tags/Word Count rail, pagination, and two-column card grid. The native portrait comparison
  preserves those structures; its visible content-rating row is intentional for the authenticated
  account and is backed by the source `adult_filter` contract.
- Latest-MuMu runtime revalidated source-image behavior. Live chapter `352787/5899549` rendered
  its inline illustration, and both tap and long press opened the full zoom/pan/reset preview.
  Book `354491` cover long press opened the same preview. Evidence:
  `qa-artifacts/turn149-cover-inline-preview-regression/` and
  `D:\NovalPie\output\playwright\turn149-source-mobile-search.png`.

## Turn 117 - Forum book-review runtime closure and review-cover preview

- The reported blank surface was verified as the **Forum bottom-tab -> 书评** category, not the
  book-detail review tab. The native category now uses the source-specific
  `/api/comments/book-reviews` feed instead of `/api/posts?type=review`.
- MuMu runtime proof after an in-place `adb install -r`: the 书评 rail loaded live cards with
  reviewer identity, avatar/badges, linked cover, review preview, and source metrics. Selecting a
  card opened its linked native Book Detail; system Back returned to the same book-review list.
- The compact linked-book cover on a review card now explicitly preserves the source cover-preview
  gesture. Tap/long-press opens the native full-image zoom/pan dialog, rather than only enabling
  preview on search, shelf, and detail covers.
- The reader's first chapter for the selected live work contained no inline illustration, so this
  turn does not claim a new inline-image runtime capture. Existing direct reader illustration
  runtime proof remains Turn 86; parser/presentation coverage continues to exercise HTML,
  Markdown, and source illustration-list image blocks.

Evidence: `qa-screenshots/turn116-forum-reviews.png`,
`turn116-review-book-detail.png`, `turn116-review-back.png`, and
`turn117-review-card-cover-preview.png`; corresponding UI hierarchies are in `qa-artifacts/`.

Verification: focused `ForumPresentationTest` and `ImagePreviewTransformTest` passed, followed by
`:app:assembleDebug`. The in-place installed Debug APK before the explicit preview-parameter
clarification has SHA-256 `0906E56235E46A548B9C43E2C079FD6987340C41868FAD7A54BE71EBA494CF25`.

## Turn 108 - Android compatibility and search-cover scroll verification

- Android 6 now uses the compatible one-argument `Html.fromHtml` path; Android 9 and below use
  the existing local Chinese-variant map instead of calling unavailable ICU APIs. API 23 and 28
  Robolectric regressions cover both paths.
- The forum reply tree and inventory JSON parser no longer rely on API 24 collection methods.
  The launch theme keeps `windowLightNavigationBar` in API 27-qualified resources, so Android 6
  no longer receives a later framework attribute.
- Search derives the changing grid viewport state before calculating cover look-ahead. This avoids
  recomposing the whole screen on every layout measurement while retaining the sharp 512x768 Coil
  request and bounded preload policy.
- MuMu ran a live `h` search after an `adb install -r` update. At 0.35 s the native skeleton was
  visible; at 1.50 s valid first-row covers loaded, source-missing covers rendered the explicit
  fallback, and a fast scroll retained full cards, tags, facts, and no accidental navigation.
- The user-owned logged-in Edge session was not attached to, inspected, exported, cleared, or
  replaced. `BROWSER_SESSION_POLICY.md` remains the authoritative reuse-only rule for any later
  browser review.

Evidence: `qa-screenshots/turn108-compat-search-scroll/home-after-upgrade.png`,
`search-h-0.35s.png`, `search-h-1.50s.png`, and `search-h-scroll-1.20s.png`.

## Turn 110 - search-cover first-scroll and reader-settings cleanup

- The compact source-style search field now binds `onFocusChanged`, making its border accurately
  reflect real text focus without changing IME submission or query behavior.
- Search warm-up now covers the first two two-card rows, at most four valid original-cover URLs.
  It retains the shared 512 x 768 Coil request/cache key, and its off-screen look-ahead remains
  bounded to four URLs on two background fetches. This improves the first fast scroll without
  lowering source cover quality.
- MuMu evidence for a fresh live `b` query shows request skeleton at approximately 0.35 seconds,
  sharp initial covers at approximately 1.50 seconds, and populated next rows immediately after
  a fast scroll. Missing source photo URLs remain explicit fallbacks, not stalled loads.
- QA temporarily changed reader settings while checking the new toolbar. They were restored through
  the native settings panel to `16sp + system`; no app data, authentication state, browser state,
  or reading progress was cleared.
- Targeted image/search/reader tests passed (23 tests); full Release tests passed 69 suites / 378
  tests / 0 failures / 0 errors / 0 skipped. `lintDebug --no-daemon` completed with no errors or
  warnings. The installed Debug APK SHA-256 is
  `100715728BAFFF6120CE145EC1F56AA8F90D5FA66347E11C232F5B896110D6E3`.

Evidence: `qa-screenshots/turn110-search-reader-regression/search-b-correct-0.35s.png`,
`search-b-correct-1.50s.png`, `search-b-correct-scroll-0.25s.png`, and
`reader-preferences-restored-system16.png`.

## Turn 112 - profile account hub and uploaded-book discovery

- The default Account tab is now an information centre: identity/status remain visible, while
  Upload Books, Check-in Centre, and App Settings are reachable before any editing form.
- Upload Books is a direct shortcut to the existing Books tab. Its local search still matches
  title, author, and tags, shows original source covers and source/status badges, renders a clear
  no-match state, and restores the same grid after clear. The shortcut deliberately does not
  infer an author-work count from the uploads response length.
- Edit Profile and Adult Verification retain all prior input, save, confirmation, and server
  callbacks, but start collapsed. This prevents the default profile visit from being dominated by
  editable fields while keeping every source-supported setting available on demand.
- MuMu used `adb install -r`; existing app data, authenticated native session, reader progress,
  and the `tcp:7890` reverse route were retained. No source mutation was submitted.
- `:app:testReleaseUnitTest --no-daemon` passed: 69 suites / 379 tests / 0 failures / 0 errors /
  0 skipped. The generated Debug lint report says `No issues found.` The Gradle wrapper process
  exceeded the tool wait window after report generation, so this is recorded as report evidence
  rather than claiming a clean command exit. `:app:assembleDebug --no-daemon` passed.
- Debug APK SHA-256:
  `E885A0104D6FFFAE0CC5B33752D3F9AC59030CDA9932352347A3925511847F1C`.

Evidence: `qa-screenshots/turn112-profile-hub-final.png`,
`turn112-profile-upload-search-final.png`, `turn112-profile-upload-search-empty.png`, and
`turn112-profile-upload-search-cleared.png`.

## Turn 114 - source adult-content search filter visibility

- The Rules rail now visibly includes **内容筛选** between scope and source instead of hiding the
  source's adult-content filter in a settings dialog. It offers `所有` (`all`), `仅成人`
  (`adult_only`), and `全年龄` (`unrestricted`).
- `NovalPieApi.searchPage` continues to send the selected value as `adult_filter`; the API
  contract test covers `adult_only`, while presentation tests cover the selected value and the
  direct visible rule group.
- The Debug APK was installed in place on MuMu; app data, native auth, reader state, and proxy
  configuration were retained. The live hierarchy shows `内容筛选: 所有` on the Rule page.
- Targeted Debug checks and full Release tests passed: 69 suites / 381 tests / 0 failures / 0
  errors / 0 skipped. The installed Debug APK SHA-256 is
  `2D5DB5BDCBEEDB11ACAC5D5D6C5CDE5ECE76E2D7BCACA819AC71B6FF9F0177BC`.
- The MuMu ADB listener became unavailable after installation, before dropdown-choice capture.
  This is recorded as incomplete runtime evidence rather than treated as a completed menu test;
  no reset, data clear, or source mutation was used to work around it.

Evidence: `qa-screenshots/turn114-search-adult-filter/search-rules-adult-filter.png` and
`qa-artifacts/turn114-search-adult-filter.xml`.

## Remaining evidence or product work

1. Software-IME close transition: the root IME inset contract is implemented, but the MuMu image
   has hardware-keyboard mode and cannot demonstrate a real software keyboard close. Do not alter
   emulator keyboard settings without user direction.
2. Physical-device validation: performance, accessibility, network fallback, rotation, and font
   scaling still need at least one real Android device in addition to MuMu.
3. User acceptance visual pass: source-parity screens are native and data-complete, but product
   visual quality is ultimately a user-facing acceptance decision rather than a unit-test result.
4. Live mutations: native review/reply, book save, upload, transfer, administration, and payment
   paths are implemented with confirmation and API tests. They are intentionally not submitted
   during QA without a scoped user request because each changes real source data.
5. Adult-filter dropdown evidence: the selected value and request contract are covered by tests,
   and the visible Rules row is captured; the live dropdown options should be recaptured once the
   existing MuMu ADB listener is available again.

## 2026-08-14 - Reader continuous-scroll and recovery regression

- Continuous scroll now starts the next safe GET before the reader reaches the literal end marker,
  while retaining a per-window sentinel fallback. The trigger uses `LazyColumn`'s live item count,
  not a repeated parse of every previously appended chapter, so the trigger itself does not grow
  more expensive across a long reading window.
- A loading/error placeholder cannot masquerade as an article end: the next-chapter trigger is
  gated until current chapter content is actually readable.
- A static sentinel can remain visible across an append and suppress the next visibility event.
  The sentinel key now contains the continuous window's final chapter id and size, so every
  successful append receives a fresh observer boundary.
- If the catalog appears to end, the reader refreshes it once before exposing the final-chapter
  state. This prevents a partial source catalog from turning into a false "已读完" stop.
- Reader content retries once with a fresh short-lived reader session on transient transport
  failures and session-related `401/403/429/5xx` responses. The retry only covers the idempotent
  chapter GET path.
- Continuous scroll keeps the native route anchored to its original entry chapter, but saves
  resume progress only after a newly appended chapter's title/text becomes visible. This prevents
  prefetch from moving the resume point ahead of what was actually read.

Runtime verification on MuMu `127.0.0.1:16384`:

- Installed Debug APK in place using `adb install -r`; existing authentication, source proxy route,
  and app data remained intact.
- Started `novalpie://app/book/354491/6992449`, scrolled through adjacent live chapters, then
  force-stopped the process without clearing data.
- The persisted session and progress both held chapter `6992459` (`第3话-女神大人，我会好好享用的♡(3)`), and a normal
  launcher restart reopened that same chapter. The evidence does not depend on a manually supplied
  deep link after the force-stop.
- A previous 100-swipe chain on the same live work progressed through multiple appended chapters
  without a `重试下一章`, `章节目录暂时不完整`, or false `已读完` surface.

Evidence:

- `D:\NovalPie\qa-artifacts\reader-infinite-scroll-fix-20260814\`
- `D:\NovalPie\qa-artifacts\reader-resume-20260814\progress-before-stop.xml`
- `D:\NovalPie\qa-artifacts\reader-resume-20260814\session-before-stop.xml`
- `D:\NovalPie\qa-artifacts\reader-resume-20260814\after-restart.png`
- `D:\NovalPie\qa-artifacts\reader-resume-20260814\after-restart.xml`

Verification: targeted `ReaderPresentationTest`, `ReaderAdjacentChapterTest`, `ReaderSessionStoreTest`,
and `NovalPieApiTest` passed with `--offline --no-daemon --max-workers=1`. Debug APK assembly and
in-place MuMu installation passed before the runtime recovery check.

## 2026-08-14 - Reader default gesture migration

- The native reader previously shipped an enabled double-tap radial tool panel as its factory
  preference. This made a normal reader action look unresponsive and could be confused with a
  scroll-ending gesture. The default now follows the source reader's direct interaction model:
  one centre tap reveals the immersive top and bottom toolbars.
- The optional radial menu is still available in Reader Settings. New installs start with it off.
  An upgrade migrates only the exact old default (`enabled + doubleTap`) to the normal toolbar;
  an explicit long-press radial choice is preserved.
- MuMu runtime installed the Debug APK with `adb install -r` and retained existing app data. The
  persisted old preference was migrated to `show_radial_menu=false` with a one-time version marker.
  A single centre tap then showed Back, chapter progress, Catalog, next chapter, size, theme,
  favorite, TTS, and website actions in the native reader chrome.

Evidence: `qa-screenshots/turn139-reader-single-tap-toolbar.png` and
`qa-artifacts/turn139-reader-layout/single-tap-toolbar.xml`.

Verification: focused `ReaderPresentationTest` and `ReaderSettingsStoreTest`, followed by
`:app:assembleDebug --offline --no-daemon --max-workers=1`, passed. Installed Debug APK SHA-256:
`C937937118E165CE82E3D534456C83A4C4663199680B509E87DB94851EB64506`.

## 2026-08-14 - Continuous-scroll boundary delivery hardening

- A live chapter-end could previously be observed while the chapter directory or the previous
  append was changing state. The `LazyColumn` visibility stream is edge-triggered, so that one
  event could be discarded and leave a correctly-enabled infinite reader with no later append
  request.
- The reader now records an reached article boundary separately from the request state. Once the
  directory becomes ready, it consumes that boundary and starts exactly one next-chapter GET.
  Successful append windows get a new sentinel and must be reached again; error and terminal
  states retain their explicit visible actions and never enter an automatic retry loop.
- MuMu runtime used the live 145-chapter book `354491` and started at chapter 144. Scrolling past
  the real boundary appended chapter 145 (`新作宣传！！`) without a manual action. Scrolling to the
  final boundary then showed the genuine terminal state `已读完全部章节`, rather than a blank or a
  stuck scroll surface.
- The Debug APK was installed with `adb install -r`; no app data, authenticated session, proxy
  route, browser state, or reading settings were cleared. `adb reverse tcp:7890 tcp:7890` remains
  active.

Evidence: `qa-artifacts/turn141-reader-infinite-fix/chapter144-after-boundary.png`,
`chapter145-loaded.png`, `chapter145-terminal.png`, and their matching UI hierarchies/logs.

Verification: focused `ReaderPresentationTest` (25) and `ReaderAdjacentChapterTest` (7) passed,
followed by `:app:assembleDebug --offline --no-daemon --max-workers=1` and `git diff --check`.
Installed Debug APK SHA-256:
`725B3129C192DDACFEFC0A31F293F011C532853424EF47A46A82D13B1AF813A4`.

## Reproducible baseline

- Workspace: `D:\NovalPie\native-android`
- MuMu serial: `127.0.0.1:16384`
- Install mode: `adb install -r` (do not clear app data)
- Proxy bridge: `adb reverse tcp:7890 tcp:7890`
- Latest full regression after source changes: `:app:testReleaseUnitTest`, 69 suites / 381 tests /
  0 failures / 0 errors / 0 skipped.
- Latest static analysis report: `app/build/reports/lint-results-debug.txt`, `No issues found.`
- Latest Debug APK SHA256:
  `725B3129C192DDACFEFC0A31F293F011C532853424EF47A46A82D13B1AF813A4`.
- Browser policy: `BROWSER_SESSION_POLICY.md`; reuse only the user-owned, already logged-in Edge
  profile when it is directly attachable. Never export, persist, clear, or recreate its cookies or
  tokens.

## 2026-08-17 - Collection/upload grid and cover-preview revalidation

- Collection retains the live-source library surfaces: the six compact toolbar actions,
  Favorites/History tabs, groups, display mode, sort, pinning, local query, load-more, batch
  move/remove, selected/full history deletion, presentation-cache policy, and actual image-cache
  clearing.
- The Collection 2/3/4-column choice persists independently and drives the real `LazyVerticalGrid`.
  Row tag footprints are aligned to the widest tag row, while every card reserves two title lines
  and one author line. Upload Books uses a separate 2/3/4-column preference with identical
  title/author and tag-row guarantees.
- Collection and Upload Books covers never open the image viewer. Search, Book Detail, and reader
  images open it only after a stationary long press; normal taps preserve their existing behavior
  and movement beyond touch slop cancels the press.
- Revalidated with `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline
  --no-daemon --max-workers=1`: 73 suites / 476 tests / 0 failures / 0 errors / 0 skipped;
  `lintDebug` reported `No issues found`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256
`1F2930C06CCD94832051CD1EDAA3238D185F406F92F2697EE7FF8E3E5F09C0B9`.
- Runtime capture remains pending because MuMu `127.0.0.1:49792` is offline. No reconnect,
installation, app-data reset, proxy, login, or browser-state mutation was attempted.

## 2026-08-17 - Mobile Book Detail structure and collection label repair

The current public mobile Book Detail layout was used as the structural reference: a compact
identity hero, `简介 / 目录 N / 评论` tabs, an explicit `正文卷 · 共 N 章` directory header with
`正序 / 倒序`, and a persistent action bar for favourite, menu, and reading.

Native now keeps the book hero focused on identity, source tags, favourite state, and reader
progress. The full description, source facts, tags, and owner/admin controls live under `简介`.
The bottom action bar retains all previous verified actions: favourite add/remove, terminology,
share, first-party WebView EPUB download, webpage detail, and permitted edit/chapter/append
management. The loaded chapter list is never mutated when the order switch is used.

Collection and owner Upload Books retain their persisted 2/3/4-column controls, fixed two-line
title and one-line author slots, row-aligned tag footprints, long-press selection, and disabled
cover preview. The visible column-picker and shelf statistic labels were repaired from mojibake to
normal Chinese (`每行`, `2/3/4 列`, `收藏`, `分组`, `最近`, `总页数`).

Verification:

- focused Book Detail, catalog, collection, grid, and card tests passed;
- full `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed: 74 suites / 484 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`; `git diff --check` passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256
  `20B6AB725B572755A2CA3068A1B5EDD6EF2514D020E25F84EC9CE86BA37B741E`.

No fresh MuMu screenshot is claimed: the known Android SDK platform-tools `adb` confirms
`127.0.0.1:49792 offline`. No install, reset, proxy, login, cookie, or browser session action was
performed. When the existing transport recovers, install only with `adb install -r`, then capture
Introduction, Catalog order, Comment, and bottom-action-bar states alongside the existing
Collection/Upload 2/3/4-column and long-press-selection evidence.

## 2026-08-17 - Collection row alignment and detail metadata completion

- Read-only `412 x 915` source captures reconfirmed the mobile Collection contract: local query,
  view switch, groups/display manager, selection, cache, clear-cache, sort, Favorites/History
  tabs, and the Default/All/Unclassified group-display choices. Evidence:
  `output/playwright/source-favorites-mobile-412x915-turn170.png` and
  `output/playwright/source-favorites-group-manager-mobile-turn170.png`.
- Collection, Search, and owner Upload Books now compose each logical book row as a full-span
  intrinsic-height Row. Cards in the same row fill the tallest sibling, so variable source tag
  counts no longer leave uneven card frames. Tags remain rendered rather than being clipped.
- Collection and Upload Books expose persistent `每行 2 列 / 3 列 / 4 列` choices. Their controls
  remain reachable on narrow phones, and every card reserves the title plus up to two author
  lines; management covers still never open the image viewer. A stationary long press on a
  Collection card remains reserved for bulk selection; Search, Book Detail, and reader
  illustrations retain stationary-long-press-only preview with drag cancellation.
- Detail normalization now preserves source chapter counts, guarantor, uploader, adult status,
  and `allowDownload`. Book Detail separates local/source statistic rails from metadata, keeps a
  non-zero advertised chapter count while the chapter endpoint is temporarily empty, and hides
  webpage EPUB download when the source explicitly disallows it.
- Verification: `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed. Release unit tests: 74 suites / 485 tests / 0 failures / 0 errors / 0
  skipped. Lint: `No issues found`. Debug APK SHA-256:
  `94E08934D9357C8F76736283ECDC019749CDDBB6623BDD1C94903E05BB7C7A87`.
- Runtime revalidation is pending only because `adb devices -l` still reports
  `127.0.0.1:49792 offline`; no reconnect, install, proxy, app-data, account, cookie, or browser
  session mutation was attempted.

## 2026-08-17 - Final Collection / Upload Books verification refresh

The final build was re-run after the safe intrinsic-measurement correction used by Collection, Search,
and owner Upload Books. Each logical book row is full span; all cards in that row fill the tallest sibling,
which keeps tag-rich and tag-light cards aligned without hiding titles, authors, or tags. Collection and
Upload Books retain separate persistent `2 / 3 / 4` column controls.

The gesture contract is explicit and regression-covered: Collection long press enters batch management and
does not open an image viewer; Upload Books does not preview covers; Search, Book Detail, and reader
illustrations preview only after a stationary long press, while movement beyond touch slop cancels it.

Verification completed with:

- `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1`;
- 74 suites / 485 tests / 0 failures / 0 errors / 0 skipped;
- lint result: `No issues found`;
- `git -C native-android diff --check` passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `D1DB0EAC6B44C34A55C9DB30D4B382D5159829042EEBCDD78A7A31D5435743E5`.

No native runtime screenshot is claimed in this refresh: the existing MuMu ADB transport remains
`127.0.0.1:49792 offline`. A scoped reconnect of this same endpoint was attempted and returned to `offline`;
no emulator reset, installation, proxy, login, cookie, browser-state, or app-data change was made. The only
remaining validation is an in-place install and gesture/layout capture after that existing transport becomes
online.

## 2026-08-17 - Turn 172 - Live mobile source audit and spoiler parity

Read-only public source review was repeated at a fixed `412 x 915` viewport. The current mobile
contracts and visual evidence are:

- `/search?sort_by=relevance` opens with real results rather than an empty query state. The first
  request was `GET /api/search?page=1&limit=60&scope=all&match_type=fuzzy_strict&sort_by=relevance&sort_order=desc&adult_filter=unrestricted&max_word_count=10000000` and returned the source's 47,398-work result rail. Rules, Tags, Word Count, help/settings/cache controls, pagination, and the grid/list switch were visible above the two-column cards.
- `/forum?tab=review` uses `GET /api/comments/book-reviews?page=1&limit=20&hide_spoilers=1` and
  reported 22,832 reviews. Cards carry a direct book route, cover, avatar/avatar-frame URLs,
  structured badge records, review text, and footer metrics. Hidden content is encoded by the
  source as `||spoiler||`, which the website paints as a black mask.
- `/book/95654` exposes the compact source hero, local/source statistics, `简介 / 目录 378 /
  评论`, guarantor/uploader metadata, a continuous catalog, nested reviews, and the fixed
  `收藏 / 菜单 / 立即阅读 (378章)` action bar. `/book/95654/1419162` uses a 16px body, 25.6px
  line height, 16px article gutters, first-line full-width spacing, chapter separators, and
  comments after each chapter in the continuous reader.

The native forum renderer now preserves this spoiler contract without executing server HTML/CSS:
`ForumTextSegment.Spoiler` is parsed from `||...||` and rendered with a black foreground/background
in both review feed excerpts and forum/detail rich content. Turning off the source hide-spoilers
switch continues to request ordinary text, so no local state is needed to reveal masked content.

Evidence screenshots:

- `output/playwright/source-search-mobile-turn172.png`
- `output/playwright/source-forum-review-mobile-turn172.png`
- `output/playwright/source-book-detail-mobile-turn172.png`
- `output/playwright/source-reader-mobile-turn172.png`

Verification after the renderer change: `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug
--offline --no-daemon --max-workers=1`; 74 suites / 486 tests / 0 failures / 0 errors / 0 skipped;
lint `No issues found`; `git diff --check` passed. Debug APK SHA-256:
`027321D582D12FE95E93302E0353DB6284595560DC8F2DE39C10F400B3D3EFAD`.

MuMu runtime verification remains open: the known ADB endpoint is still `127.0.0.1:49792 offline`.
The existing Edge process does not expose a direct Playwright debugging port, so the authenticated
admin page was not replaced with a fresh login or a copied browser profile. No cookie, token, account,
proxy, or app-data state was changed. Device screenshots remain the next runtime gate.

## 2026-08-17 - Turn 173 - Collection/Upload grid completion verification

The requested Collection and owner Upload Books behavior is present in the native build:

- Collection and Upload Books each persist an independent `2 / 3 / 4` columns choice;
- each logical row is full span and fills every sibling card to the tallest measured card, so tag-rich
  and tag-light books keep aligned frames;
- title and author fields remain rendered at every density, with tags retained rather than truncated;
- Collection long press enters batch selection and does not open a cover viewer;
- Upload Books cover preview is disabled; Search, Book Detail, and reader illustrations use only a
  stationary long press, with movement beyond touch slop cancelling the preview.

The source Collection actions remain available: layout, groups/display, selection, cache policy,
clear cache, sort, Favorites/History tabs, group management, pinning, local search, paging, batch
move/remove, and selected/full history deletion.

After the portrait policy change, the full verification command passed:
`:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1`;
75 suites / 488 tests / 0 failures / 0 errors / 0 skipped; lint has zero Error/Warning issues;
`git -C native-android diff --check` passed. The Debug APK is
`app/build/outputs/apk/debug/app-debug.apk` with SHA-256
`22EC26FB5479302B92C3207FB9D276E678335B185EA37EFA2D6F09D03D0582F8`.

MuMu's known transport `127.0.0.1:49792` remains `offline` after a scoped reconnect attempt. No
install, emulator reset, proxy change, app-data change, login, cookie, token, or browser-session
mutation was made; native screenshots remain the only outstanding runtime gate.

## 2026-08-17 - Turn 174 - Collection controls and source-complete reader themes

The Collection and owner Upload Books grid contract remains source-aligned: each page exposes an
independent persisted `每行 2 列 / 3 列 / 4 列` selector, logical rows fill the tallest sibling,
and title, author, and source tag fields remain rendered at every density. Collection long press
enters batch management instead of opening a cover viewer; Upload Books management also disables
cover preview. Search, Book Detail, and reader illustrations still require a stationary long press,
with movement beyond touch slop cancelling the preview.

The live reader theme audit showed one remaining native gap. Native now supports up to twelve local
custom themes with the source fields: theme name, page background, page text, sidebar background,
sidebar text, accent color, and an optional persisted local background-image URI. Themes are saved
in the existing reader preferences, invalid colors fall back to safe `#RRGGBB` defaults, deleting
the selected theme returns to `系统`, and the selected custom palette applies to the article,
status/header rails, action rail, settings/catalog panels, and optional low-opacity background image.

Verification:

- `:app:testReleaseUnitTest --offline --no-daemon --max-workers=1`: 76 suites / 494 tests /
  0 failures / 0 errors / 0 skipped;
- `:app:lintDebug --offline --no-daemon --max-workers=1`: `No issues found`;
- `:app:assembleDebug --offline --no-daemon --max-workers=1`: `BUILD SUCCESSFUL`;
- `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `72B0B60AEFC8D5F6EFAE13BC85AAC8555235D66ADF2265B6699EA6D9D1E32A42`.

MuMu remains `127.0.0.1:49792 offline`; no installation, reset, proxy, account, login, cookie,
token, or browser-state mutation was performed. Runtime screenshots for the Collection/Upload
2/3/4-column states, stationary preview gesture, and custom theme remain the next online gate.

## 2026-08-17 - Turn 175 - Collection source-preview and cache-state closure

Fresh public mobile source review reconfirmed the Collection toolbar order and its group/display
and sort sheets. Evidence is retained at:

- `output/playwright/source-favorites-group-manager-mobile-turn175.png`;
- `output/playwright/source-favorites-sort-mobile-turn175.png`.

Native already requested `/api/favorites/groups?with_preview=true`, and normalized the returned
`previews`, but the Collection folder card had discarded those covers and always rendered a generic
folder glyph. Folder cards now display a static, deduplicated stack of up to three source preview
covers. Those covers explicitly use `CoverPreviewPolicy.Disabled`: they cannot open an image
viewer and do not take over the Collection book-card long press, which remains batch selection.
When a group has no preview items, the original folder glyph remains the fallback.

The native cache toolbar no longer stays highlighted when its persisted policy is `None`; its
selected state now reflects the same local cache choice that the source control exposes. The
existing modes and clear-cache action still affect local presentation data only, never source
favourites, history, or groups.

The user-activity route was separately rechecked with a public read-only request:
`/api/users/100000/activities` still returns HTTP 404 with `API endpoint not implemented in Laravel
yet.` This remains a recoverable source limitation rather than a native blank-state regression.

Verification passed:

- `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1`;
- 76 suites / 496 tests / 0 failures / 0 errors / 0 skipped;
- lint: `No issues found`;
- `git diff --check`: passed;
- Debug APK SHA-256: `33D32011688EC813C201F00DCEE0B31804EFEBE5C5EFF901485C4C364811E636`.

`adb devices -l` currently reports no attached MuMu device. No APK install, reset, proxy change,
app-data change, browser-session access, or account mutation was attempted. The next runtime gate
is a data-preserving install and visual check when the existing device transport is available.

## 2026-08-17 - Turn 176 - Reader chapter-separator source-style typography

Fresh read-only mobile source inspection of `/book/95654/1419162` at `412 x 915` measured the
actual reader surface rather than relying on prior CSS assumptions:

- article body: `16px` type, `25.6px` line height, `20px 16px 30px` article padding, and an
  `800px` wide-screen cap;
- chapter title: `20px / 28px`, bold, muted gray (`#6B7280` family);
- chapter separator: a `2px rgba(0,0,0,0.1)` top rule with `16px 0` outer margin and
  `16px 0 8px` inner padding.

The default native body was already aligned at 16sp / 1.6 line-height and retained all user
typography controls. The remaining mismatch was visual: its chapter title was 24sp, body-colored,
and had a heavy divider below it. Native now uses the source's restrained 20sp meta-colored title,
places the 2dp divider above it, and reserves the source-derived first-separator breathing room.
This changes only chapter presentation; reading settings, continuous-scroll loading, TTS,
illustration long-press zoom, downloads, and chapter comments remain untouched.

Source screenshot: `output/playwright/source-reader-computed-style-mobile-turn176.png`.

Verification passed:

- `:app:testReleaseUnitTest :app:lintDebug --offline --no-daemon --max-workers=1`;
- 76 suites / 496 tests / 0 failures / 0 errors / 0 skipped;
- focused `ReaderPresentationTest`: 31/31 passed;
- lint: `No issues found`;
- `:app:assembleDebug` passed; Debug APK SHA-256:
  `6BB3DF86045091D4E67B459036ED4CADBF2FF78082ADE0C268B509BF8116E37E`;
- `git diff --check`: passed.

`adb devices -l` still has no attached device, so a post-install visual capture of the adjusted
separator remains deferred without changing emulator, account, proxy, app data, or browser state.

## 2026-08-17 - Turn 177 - Live search-default correction and package refresh

Read-only inspection of the already-open mobile source search session confirmed the initial data
request is:

```text
GET /api/search?page=1&limit=60&scope=all&match_type=fuzzy_strict&sort_by=relevance&sort_order=desc&adult_filter=unrestricted&max_word_count=10000000
```

The app still exposes all three source-compatible content-rating selections (`all`, `adult_only`,
and `unrestricted`) in its Rules rail. The correction changes only the fresh/cleared-settings
default to `unrestricted`, matching the live request. A previously stored choice is not migrated
or overwritten, so users who selected `all`, `adult_only`, or `unrestricted` retain that exact
selection. The selected-filter summary also treats `unrestricted` as the default state.

Verification passed:

- focused forced Release regression: `SearchSettingsStoreTest`, `NovalPieApiTest`, and
  `DiscoverPresentationTest`;
- full `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1` passed;
- 76 suites / 497 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`; `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `3A4FEF3969A56D3995DCFA7F49C4FF81BAEC1C1E574717E3AB99674762F67053`.

The known MuMu endpoint `127.0.0.1:49792` currently reports `offline`, so no install, app-data
mutation, proxy change, account action, or runtime screenshot is claimed for this package refresh.

## 2026-08-17 - Turn 178 - Review grid and source badge geometry

The live mobile source was rechecked in the read-only browser session:

- `/forum?tab=review` requests `GET /api/comments/book-reviews?page=1&limit=20&hide_spoilers=1`;
- the current response reports 22,852 reviews and `pages=1143`;
- the rendered review feed uses two compact cards per mobile row, while the ordinary forum feed
  remains one card per row;
- review records include structured `authorBadges` (`badge_html` / `badge_css`) and WebP avatar
  frame URLs.

Evidence:

- `output/playwright/source-forum-review-mobile-turn178.png`;
- `output/playwright/source-reader-controls-mobile-turn178.png`.

Native changes:

- Forum now uses one restored `LazyVerticalGrid`: normal categories keep one column, and the
  source `review` category uses two columns. Search, category, spoiler, empty/error, pagination,
  and load-more controls span the full grid width; the existing route scroll-position restore and
  book/user destinations remain intact.
- Review cards use compact spacing and cover/avatar sizing appropriate to the source's two-column
  mobile card, without enabling accidental cover preview.
- Source badge CSS dimensions (`width`, `height`, `padding-left/right`, and `font-size`) are read
  as bounded passive values. This preserves artwork badges such as `125px x 34px` instead of
  forcing every badge into the same 18/22dp pill. WebP frame URLs remain normal Coil assets.
- API regression now covers the current structured badge and WebP frame response shape, including
  review totals and pagination.

Verification passed:

- `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --offline
  --no-daemon --max-workers=1`;
- 76 suites / 501 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`; `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `60CF05AB8FB826E37C1DB240E6B9EAF2CB52075AC31DFD8CD6370184DAF26324`.

MuMu's `127.0.0.1:49792` transport remains `offline`; no install, reset, proxy change, account
action, cookie/token access, or runtime APK screenshot was performed.

## 2026-08-17 - Turn 179 - Collection picker interaction verification

The requested Collection and owner Upload Books contract remains present: independent persisted
`2 / 3 / 4` column choices, full-span rows aligned to the tallest sibling, visible title/author/tag
content, Collection long-press batch selection, and disabled management-cover preview. Search,
Book Detail, and reader illustrations remain stationary-long-press-only preview targets, with drag
movement cancelling the pending preview.

One interaction edge was corrected: the Collection column picker is now disabled while the user is
in List layout, because changing a column count there has no immediate visual effect. The saved
choice is retained and becomes active when Grid layout is selected again; the control explains this
state instead of accepting a no-op-looking tap. Upload Books remains a grid-only surface, so its
`2 / 3 / 4` picker is always actionable.

Verification:

- focused `LibraryPresentationTest`: passed;
- full `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1`: `76` suites / `502` tests / `0` failures / `0` errors / `0` skipped;
- lint: `No issues found`;
- `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `9234A11703544FACC4706356414A0A0E473F26F4B859FAB3050E0E9A07C8F485`.

MuMu's known endpoint `127.0.0.1:49792` still reports `offline` after a scoped ADB server restart
and reconnect. No emulator reset, installation, app-data change, proxy change, login/cookie access,
or browser-session mutation was made; native runtime screenshots and touch-gesture evidence remain
pending transport recovery.

## 2026-08-17 - Turn 180 - Collection empty-state and stationary preview boundary

A fresh read-only source check at `412 x 915` reconfirmed the mobile Collection toolbar, the
Favorites/History tabs, and the Group Display Manager. The source capture is
`native-android/.playwright-cli/page-2026-08-17T06-03-41-438Z.png`; it was collected in a new public
session and did not read or alter any login state.

Native corrections:

- The Collection header now follows the source mobile title (`我的收藏`).
- Default group mode no longer shows a misleading `没有匹配的收藏` message when the API returns
  only populated group folders and no unclassified books. A real query still reports no matches.
- Cover preview gesture handling now uses Compose's platform long-press detector: it triggers at the
  long-press threshold even when the finger is still, cancels on movement/multi-touch, and consumes
  the eventual up event so the parent card cannot also navigate. Collection and Upload Books remain
  `CoverPreviewPolicy.Disabled`; Search, Book Detail, and reader illustrations remain stationary
  long-press-only targets.

Changed files for this turn:

- `app/src/main/java/com/novalpie/nativeapp/ui/LibraryPresentation.kt`
- `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
- `app/src/test/java/com/novalpie/nativeapp/ui/LibraryPresentationTest.kt`

Verification passed:

- `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1 --console=plain`;
- 503 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`;
- `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `F511A3D80ABA82D7111E7B18EBC87E918249DC7EA5E37CA36377FBFB077623EB`.

MuMu's known endpoint `127.0.0.1:49792` remains `offline` after a scoped disconnect/connect attempt.
No install, reset, proxy change, app-data mutation, account action, cookie/token access, or browser
session mutation was made; native runtime screenshots and actual touch traces remain pending ADB
transport recovery.

## 2026-08-17 - Turn 181 - Collection list metadata parity

The Collection list renderer was still limiting tags to the first four items, unlike the grid and
the source card contract. It now renders the complete normalized tag set and reserves up to two
author lines, so switching between Grid and List does not silently drop book metadata. The existing
2 / 3 / 4 grid selector, tallest-sibling row alignment, disabled management-cover preview, and
stationary-long-press preview policy are unchanged.

Changed file:

- `app/src/main/java/com/novalpie/nativeapp/ui/CollectionPresentation.kt`

Verification passed:

- `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
  --max-workers=1 --console=plain`;
- 503 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`;
- `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `349CB5CFE9DD48EE88DB7591449595869EB00EE3E820827918520280B326AF12`.

MuMu `127.0.0.1:49792` remains offline; no installation or runtime screenshot is claimed.

## 2026-08-17 - Turn 182 - Final gesture implementation verification

The stationary preview watcher now uses the Compose `AwaitPointerEventScope` timeout primitive in
the `Initial` event pass. This preserves the parent card's normal tap while guaranteeing that a
completely still press reaches the threshold, and explicitly cancels on movement or a second
pointer. The final up event is consumed only after a preview has actually triggered.

Final verification after this implementation change:

- `:app:testReleaseUnitTest --offline --no-daemon --max-workers=1 --console=plain`: 503 tests / 0
  failures / 0 errors / 0 skipped;
- preceding `:app:lintDebug :app:assembleDebug` passed; lint `No issues found`;
- `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `11F6F1D2AD65A17BDB664D930CBEE90E209B88F084C4BC6C1F3EB1A092B52F22`.

MuMu `127.0.0.1:49792` is still `offline`; no runtime install or screenshot is claimed.

## 2026-08-17 - Turn 183 - Collection history-action boundary

The requested Collection/Upload Books presentation remains complete: Collection and owner Upload
Books keep independent persisted `2 / 3 / 4` column selectors, full-span logical rows aligned to
the tallest sibling, complete title/author/tag fields, and disabled management-cover previews.
Collection long press remains the batch-selection entry point; Search, Book Detail, and reader
illustrations remain stationary-long-press-only preview targets with movement cancellation.

One source-parity edge was corrected in this turn. The `置顶 / 取消置顶` action is now exposed only
for an unselected Favorites record with a valid favourite-record id. Reading History rows no longer
show a remote favourite mutation that cannot apply to history entries. Grid and List renderers use
the same predicate, with a regression test covering history, selection mode, and missing ids.

Changed files:

- `app/src/main/java/com/novalpie/nativeapp/ui/CollectionPresentation.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`;
- `app/src/test/java/com/novalpie/nativeapp/ui/LibraryPresentationTest.kt`.

Verification:

- `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1`;
- 76 suites / 504 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`;
- `git diff --check`: passed;
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- SHA-256: `52269A030806A7BED5FFDE6696F2A50A3CDA578A7CA9B412A7A273246F967E90`.

Read-only source evidence at `412 x 915` is stored at
`output/playwright/.playwright-cli/page-2026-08-17T07-58-12-243Z.png`. The public session was
unauthenticated and did not read or alter any browser login state. MuMu
`127.0.0.1:49792` remains `offline`; no installation, reset, proxy, app-data, account, cookie,
token, or browser-state mutation was performed, so native runtime screenshots remain pending ADB
transport recovery.

## Turn 184 - Profile activity filters

The owner and public profile activity surfaces now expose the same five source categories:
`全部`, `帖子`, `评论`, `书评`, and `章评`. Filtering is performed against the already merged
activity window (`post`, `post_comment`, `novel_comment`, and `chapter_comment`) so selecting a
category is immediate and does not duplicate network requests. The empty-state copy distinguishes
an actually empty feed from a category with no matching records.

Changed files in this slice:

- `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/ProfilePresentation.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/ProfileScreens.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/UserProfileScreens.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`;
- `app/src/test/java/com/novalpie/nativeapp/ui/ProfilePresentationTest.kt`.

Verification: `:app:testReleaseUnitTest`, `:app:lintDebug`, and `:app:assembleDebug` passed
offline with 76 suites / 506 tests / 0 failures / 0 errors / 0 skipped. Lint reports no issues,
and `git diff --check` passes. Debug APK SHA-256 is
`33FE15FDE80A292C066DD5F5E226D03936256AF70E25D742E033A9A9F90382A2`.

MuMu remains `127.0.0.1:49792 offline`; runtime install and screenshots are still pending
transport recovery. No login, cookie, token, proxy, or app-data state was changed.

## Turn 185 - Independent reader and Book Detail loading

The public mobile source was rechecked in a fresh read-only Playwright session. Search opens with
real default results; the forum review feed uses `/api/comments/book-reviews` with spoiler hiding;
Book Detail uses `/api/comments?type=book&book_id=<id>&page=1&limit=30`; and Reader obtains a
short-lived session key before requesting chapter content and adjacent chapters. Screenshots are
stored in `output/playwright/source-*-mobile-turn185.png`.

The reader and Book Detail ViewModel paths no longer wait for all auxiliary requests before
publishing their core content.正文/缓存、书籍详情、目录、书评、收藏状态、权限和高清封面 now
update independently, with route/request-serial guards preventing stale A -> B responses from
painting over the active route. This directly covers the prior symptom where a slow comment or
favourite request made the reader appear frozen or made book reviews look absent.

Changed files:

- `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/ReaderPresentation.kt`;
- `app/src/test/java/com/novalpie/nativeapp/ui/ReaderPresentationTest.kt`.

Verification passed: `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline
--no-daemon --max-workers=1`; 76 suites / 507 tests / 0 failures / 0 errors / 0 skipped;
lint reports no issues; `git diff --check` passes. Debug APK SHA-256:
`0F513ECB504990D6FC80633C6431F0CF84A001661A3D322E30E9F3674832331B`.

MuMu remains `127.0.0.1:49792 offline`; no APK installation or native runtime screenshot was
possible in this turn.

## Turn 186 - Runtime collection/upload grid verification

The configured MuMu OPPO profile reports `sw600dp` while being used as a handset. The portrait
policy now includes the 600dp boundary, and runtime launched at `900x1600`, `ROTATION_0`.

Collection and owner Upload Books were exercised on the live app with independent 2/3/4-column
selectors. Full-span logical rows still align cards to the tallest sibling. Card titles and authors
now wrap to the complete source text rather than showing an ellipsis; tags remain complete. Collection
cover preview is disabled and a stationary long press enters bulk selection. Upload management cover
preview remains disabled.

Changed files in this slice:

- `app/src/main/java/com/novalpie/nativeapp/OrientationPolicy.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/NovelCardFacts.kt`;
- `app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`;
- `app/src/test/java/com/novalpie/nativeapp/OrientationPolicyTest.kt`;
- `app/src/test/java/com/novalpie/nativeapp/ui/NovelCardFactsTest.kt`.

Runtime screenshots are stored under `D:\NovalPie\agent-bridge\screenshots\turn186-*`.

Verification passed:

- `:app:testReleaseUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon --max-workers=1`;
- 76 suites / 508 tests / 0 failures / 0 errors / 0 skipped;
- lint task succeeded with no Error/Warning issues;
- `git diff --check` passed;
- Debug APK SHA-256: `FB07930A976E56A728E1FFF23B271C17E43DD27F1C248EEB10ED86617A4A10CE`.

## Turn 192 - CAPTCHA route-marker repair and reader regression pass

The native CAPTCHA screen used a bare `String` as its WebView tag, while the shared asynchronous
proxy loader only resumes a route whose tag is a `WebViewStateMarker`. On MuMu this caused the
proxy callback to silently reject `/login`, so the WebView never called `loadUrl` and rendered a
blank area. `AuthCaptchaScreen` now creates the same route marker as `WebFallbackScreen`, keyed to
`https://novalpie.cc/login`; its recomposition guard uses the shared marker predicate as well.

Changed files:

- `app/src/main/java/com/novalpie/nativeapp/ui/AuthCaptchaScreen.kt`;
- `app/src/test/java/com/novalpie/nativeapp/ui/AuthPresentationTest.kt`.

The focused regression was first observed failing because the CAPTCHA marker factory was absent,
then passed after the marker wiring. Runtime on MuMu (`127.0.0.1:16384`) was installed in place
with `adb install -r`, preserving app data and the existing login state. The pre-fix blank screen
is recorded at `D:\NovalPie\agent-bridge\screenshots\turn192-auth-before-diagnostics.png`; the
fixed route loads source content at
`D:\NovalPie\agent-bridge\screenshots\turn192-auth-runtime-fixed.png`. Chromium emitted a source
console event after the fixed navigation. Because the existing source WebView session is already
authenticated, it renders the authenticated landing page instead of an anonymous challenge; no
cookie, token, browser session, proxy setting, or app data was cleared to force that condition.

Reader and presentation runtime evidence:

- `turn192-reader-toolbar-bodytap.png` shows a short article-body tap opening the right action rail.
- `turn192-reader-after-scroll-7.png` shows chapter body followed by its preserved chapter-comment
  section; `turn192-reader-comments-nextchapter.png` is the continued next-body window.
- `turn192-reader-page-turn-progress.png` shows page-turn reading advancing from chapter 3 into
  chapter 4 at the boundary rather than stopping.
- `turn192-reader-tts-timeout.png` and `turn192-reader-tts-system-settings.png` show a bounded
  no-engine state and its working system Text-to-Speech settings action. This MuMu image has no
  selected or installed TTS engine, so audio cannot start until an Android TTS engine is enabled.
- `turn192-book-detail-catalog-tab.png` and `turn192-book-detail-comments-tab.png` verify the
  exclusive Introduction/Catalog/Comments surface. `turn192-forum-five-categories.png` verifies
  the five equal-width forum categories.

After the page-turn boundary test, QA returned through the normal directory to chapter 3 and then
to Collection. The temporary chapter-comments and reader-mode settings were restored to their
pre-test values.

Full verification passed: `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline
--no-daemon --max-workers=1 --console=plain`; 77 suites / 520 tests / 0 failures / 0 errors / 0
skipped. Lint reports `No issues found`, and `git diff --check` passed. Debug APK SHA-256:
`E92D0C30FD7A8D5B81C08BF4B2D6492315437379A0DDD55419FC08F29BAFDB39`.

## Turn 193 - Previous-page reader return and Collection identity alignment

Page-turn boundaries now carry an explicit `ReaderChapterEntryPosition`. A left-side page turn at
the beginning of chapter B opens chapter A with a one-shot `End` target; normal catalog, deep-link,
and next-chapter opens retain `Start`. The target is applied only after the new chapter body has
composed, so the loading placeholder cannot consume the final-item scroll.

Collection resume progress now persists `bookTitle` beside `chapterTitle`. The shelf renders book
identity first, then chapter identity. Older local records are repaired by one guarded
`bookDetail(bookId)` lookup only when the current Book Detail and loaded shelf cannot name the
book. Compact Collection cards reserve two title lines, one author line, and a progress footer;
the empty footer is limited to Collection so Upload Books remains compact.

MuMu runtime evidence at `127.0.0.1:16384`:

- `D:\NovalPie\agent-bridge\screenshots\turn193-resume-title-after-lookup.png` shows the old
  record resolved to its real book title above the saved chapter title.
- `D:\NovalPie\agent-bridge\screenshots\turn193-reader-page-previous-end.png` shows the chapter
  header changed from 3 to 2 while the visible body begins at the final illustration and tail
  paragraphs, proving the previous chapter opened at its end rather than its heading.
- `D:\NovalPie\agent-bridge\screenshots\turn193-final-home.png` shows Collection restored with
  the real resume title and aligned author/progress rows. The temporary page-turn setting was
  restored to sliding mode and the reader returned to chapter 3 before normal Back navigation.

Regression coverage was added to `ReaderAdjacentChapterTest`, `ReaderProgressStoreTest`, and
`LibraryPresentationTest`. Full verification passed: `:app:testReleaseUnitTest :app:lintDebug
:app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain`; 77 suites / 526 tests
/ 0 failures / 0 errors / 0 skipped; lint reports `No issues found`; `git diff --check` passed.
Debug APK SHA-256: `F70D5E451FF5198748216D5C97BAFB069F23450678083221D12907494E1D186C`.

## Turn 194 - image-grid jank profiling and bounded decode/layout work

The MuMu portrait debug build was profiled with a focused flow: open Search, switch to an
uncached result page, then perform seven fast vertical swipes. The baseline showed 19/93 janky
frames (20.43%), P95 150 ms, 17 slow UI-thread frames, GPU P99 1 ms, and zero slow bitmap-upload
frames. Logcat recorded 24 400x600 bitmap allocations and a 76.522 ms concurrent compact GC. The
Simpleperf call graph also identified per-row `BoxWithConstraints`/`IntrinsicSize.Max`, LazyGrid
prefetch measurement, and Coil BitmapFactory decoding as the combined burst source.

Implemented:

- `NovalPieImageLoading.kt` limits Coil BitmapFactory decoding to two concurrent decodes. Cover
  request dimensions remain 512x768; no image-quality reduction or cache clearing was used.
- Search grid tag width is calculated once from the viewport. Search result rows no longer perform
  per-row `BoxWithConstraints` or intrinsic-height measurement.
- Collection and Upload Books rows use their existing fixed title/author/progress slots directly,
  removing redundant intrinsic-height measurement while retaining aligned columns.

Runtime evidence on MuMu `127.0.0.1:16384` with `adb install -r` and the existing reverse proxy:

- Search uncached page 6: 15/157 janky frames (9.55%), P95 109 ms.
- Search warm-grid comparison: 10/75 janky frames (13.33%), P95 85 ms; the pre-change warm run
  was 22/38 (57.89%), P95 550 ms.
- Collection fast scroll: 13/129 (10.08%), P95 85 ms.
- Upload Books fast scroll: 12/212 (5.66%), P95 34 ms.

Evidence is stored in `D:\NovalPie\agent-bridge\screenshots\turn194-*` and the complete raw
captures are in `D:\NovalPie\native-android\qa-artifacts\turn194-image-jank\`.

Verification: `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
--max-workers=1 --console=plain`; 77 suites / 527 tests / 0 failures / 0 errors / 0 skipped;
lint reports no errors or warnings; `git diff --check` is clean. Final debug APK SHA-256:
`FD4F8B927AFCCF05943E69C5A0B80D53B99AAED3397BF49DDC44E2BE843955DC`.

## Turn 195 - animated original covers and safe image-preview chrome

Two live source-cover samples were retained as binary QA fixtures: book `349955` (400x600,
26-frame GIF at 100 ms) and book `356821` (400x600, 7-frame GIF at 500 ms). Both were served to
the production preview composable. Before the decoder change they remained on their first frame;
after it, frame-pair sampling changed 81,338 / 85,204 pixels for `349955` and 79,049 / 85,204
pixels for `356821`. The source file extension is deliberately not used as the decoder contract:
API 28+ uses Coil's `ImageDecoderDecoder`, while older devices use Coil's `GifDecoder`, covering
GIF and Animated WebP assets delivered by the source.

The old preview also requested an exact 3072px bitmap and let its floating bottom control bar
cover or clip the image. The native preview now keeps a stable request at most 1440x2160 with
`Precision.INEXACT`, keeps the image inside a dedicated viewport between fixed top and bottom
chrome, and reserves both `navigationBarsPadding()` and a 16dp minimum bottom safe area. The
minimum protects MuMu/gesture configurations that report a zero navigation-bar inset while still
clipping the final display pixels. The targeted regression is
`ImagePreviewTransformTest.previewBottomControlsReserveSpaceAboveTheScreenEdge`.

Runtime GIF evidence is under `D:\NovalPie\agent-bridge\screenshots\turn195-*`; in particular
`turn195-debug-preview-policy-direct-frame-a.png` and `-b.png` show distinct animated frames.
The 10-second preview profile recorded 299 frames, 0.33% jank, P99 10 ms, and zero slow bitmap
uploads in `qa-artifacts\turn195-cover-preview\debug-preview-policy-gfxinfo.txt`.

The debug-only `AnimatedImagePreviewDebugActivity`, its debug manifest, the local `tcp:7878`
reverse mapping, and its served QA route were removed from the source/ADB configuration; the
existing debug application was restored to `MainActivity` without clearing its data.

Verification after the final layout change used a project-local copy of the existing offline Gradle
cache and Android platform/build-tools, so the sandbox never needed to write the host cache. The
targeted `ImagePreviewTransformTest` passed; `lintDebug` reports `No issues found`; and
`assembleDebug` passed. Debug APK SHA-256 is
`ED0399FCABDBF450B708D9A54BEE17A7AE2658ADB2D818CAF3714671265BCB0B`.

The full Release unit suite executed 530 tests; 202 failed before application test setup because
Robolectric attempted to download its absent dynamic Android runtime
`org.robolectric:android-all-instrumented:15-robolectric-12650502-i7`, while sandbox networking
is disabled. The targeted preview test is independent of that runtime and passed. The full suite
should be rerun when that standard Robolectric artifact is available offline or host networking is
enabled.

`adb install -r` correctly refused to overwrite the retained MuMu application: its installed
debug certificate SHA-256 is `cc7d6fe6844d4a4ee510d65b185ac5e32a642faa9bfb819df75d51be4c607047`,
while this sandbox build uses
`aa0fb9d1b22401905ede43ab3403fa1c9397a41edce00759c5aeef4e116482ba`. No uninstall, data clear,
login change, or cookie change was performed. A final live long-press screenshot requires a build
signed by the existing application certificate or an explicitly approved clean reinstall.
