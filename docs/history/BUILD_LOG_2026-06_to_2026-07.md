# NovalPie 2.0 Native Android

This is the real Android-native NovalPie 2.0 alpha project.

It is separate from the old Capacitor/WebView shell in
`D:\NovalPie\minimal-commercial`.

## Current baseline

- Project: `D:\NovalPie\native-android`
- Latest built APK: `D:\NovalPie\NovalPie-native-2.0-debug-turn42-profile-account-status-20260712.apk`
- Latest built APK SHA256:
  `656031BAE126414800843841B53ED7424EB2429AAC73D340022475F51979CDAF`
- Latest local build verification: `2026-07-12 17:16`, full release unit test
  report stayed green and debug packaging succeeded for Turn 42.
- Last verified live runtime remains the previous stable MuMu evidence from
  Turn 39/Turn 38. Turn 42 could not capture a runtime screenshot because MuMu
  instance 0 launched briefly, then returned `is_process_started=false` and
  `is_android_started=false`; ADB had no connected devices.
- Tested package id: `com.novalpie.app.debug`
- Release package id: `com.novalpie.app`
- Launch activity: `com.novalpie.nativeapp.MainActivity`
- UI stack: Kotlin + Jetpack Compose + Material3
- API client: OkHttp read-only NovalPie API calls
- WebView: only `ui/WebFallbackScreen.kt`, not the main UI

## Implemented first-stage native surfaces

- Native startup/default Forum page
  - default route is a native forum-style Compose screen, not the old
    bookshelf/API test panel
  - bottom navigation is now `收藏 / 搜索 / 工具 / 论坛 / 我的`
  - native launch marker `NOVALPIE_NATIVE_COMPOSE_HOME` is carried by the
    Forum screen for MuMu/UIAutomator proof
  - native forum feed now reads `/api/posts` through `ForumPost` and falls
    back to local seed rows only when live data is not available
  - native forum detail comments now render as threaded comment blocks:
    top-level comments stay in the main flow, replies are grouped under their
    parent, and orphan replies remain visible instead of being dropped
  - bottom navigation uses Material icons plus labels instead of text-only
    placeholder glyphs
- Native Library/Bookshelf page
  - page header now uses a reader-client overview: `书架 / 继续阅读、收藏分组和最近进度`
  - overview shows sync state plus `收藏 / 分组 / 最近` stats before the list
  - primary phone-width actions are capped to `同步书架 / 登录同步 / 网页收藏`
    to avoid clipped toolbar buttons
  - local continue-reading row backed by native SharedPreferences
  - native recent-reading rail backed by per-book local progress
  - favorite groups and local text filtering are combined into a single
    `分组与筛选` control surface
  - native bookshelf filter for favorites by title, author, tag, or book id
  - native bookshelf filter also matches book status such as `连载` or `完结`
  - native bookshelf filter also matches word count and update timestamp shown
    in card facts
  - native favorite-group filter chips, including an `全部` option, backed by
    `/api/favorites?group_id=...`
  - native load-more pagination for favorites
  - stale home/favorites pagination API results are dropped when a newer home
    refresh has started
  - API bookshelf/favorites normalization accepts `favorites` and `books`
    array aliases
  - API favorite-group normalization accepts `favorite_groups`,
    `group_name`, and `book_count` aliases
- Bottom navigation
- Bookshelf/Home
- Search
  - Discover page now uses a content-client search panel with status, keyword
    input, selected-filter chips, and primary search actions in one surface
  - Discover filters are generated from `DiscoverPresentation.kt` and use
    clean compact Chinese labels instead of hardcoded parameter/debug copy
  - Discover idle state now shows recommendation prompts (`最近更新 / 热门书评 /
    长篇连载 / 完结作品`) instead of leaving the first screen mostly blank
  - native keyword, sort, order, scope, source, word-count range, match-type,
    and adult-filter controls
  - persisted native search settings for sort, order, scope, source,
    word-count range, match type, and adult filter
  - native `/api/search` sends `source`, `min_word_count`, and
    `max_word_count` when the corresponding website-parity filters are selected
  - persisted native search history, most-recent-first, duplicate promotion,
    bounded to 10 keywords, with clear and reuse actions
  - last nonblank search keyword restores into the native search box after
    restart
- API result normalization accepts `results`, `novels`, `list`, and
  `records` array aliases
  - API book normalization accepts expanded original-cover aliases such as
    `photo_original_url`, `cover_original_url`, and `origin_cover_url`
  - API book tag normalization accepts website fields such as `spans`,
    `badges`, `categories`, `genres`, `book_tags`, `novel_tags`, and
    `tag_list`; editor screens intentionally exclude `spans` from editable
    tag fields because adult/completion state is modeled separately
  - native load-more pagination for search results
  - stale search and search-pagination API results are dropped when the
    keyword, filters, page, or active request serial no longer match
- Book detail
  - native read-only favorite status from `/api/favorites/status`
  - API favorite-status normalization accepts website-style status/group
    aliases such as `isFavorite`, `status_text`, and `favorite_group`
  - stale book-detail API results are dropped when the current route/state no
    longer matches the requested book
  - duplicate taps on the currently opened book detail do not push duplicate
    routes
  - native same-book Continue Reading hint/action when local reader progress
    belongs to the opened book
  - chapter catalog rows mark the saved same-book reading progress chapter
  - native detail facts show status, author, word count, update time, favorite
    count, site/source read counts, and source favorite count as Compose chips
    when the API provides them
  - API book normalization covers nested `data.novel` payloads, object-style
    authors/tags, `cover_path`, `synopsis`, `words`, and `created_at`
  - API book status normalization accepts text aliases such as `status`,
    `state`, and `completion_status`, plus boolean completion aliases such as
    `is_completed`
  - API book tag normalization also combines website-style `category`, `genre`,
    string `tags`, and tag alias fields into stable de-duplicated detail chips
- Chapter catalog
  - native catalog filter on book detail and reader catalog
  - native catalog filter matches title, number, id, word count, and update
    timestamp
  - native catalog summary on book detail and reader catalog shows total
    chapter count, filtered count, and the current saved/reading chapter
    position when available
  - API chapter normalization covers website-style aliases such as
    `chapter_name`, `display_order`, `words`, and `created_at`
  - normalized chapters are sorted by website order fields before Compose
    renders the catalog and before reader previous/next controls calculate
    adjacent chapters
- Reader foundation
  - native current-chapter progress summary, such as `第 N / M 章 · 标题`,
    shown at the top of the reader when the catalog is loaded
  - native previous/next chapter controls
    - previous/next controls are disabled when the current chapter is not
      present in the loaded catalog, avoiding accidental jumps to catalog edges
  - native font-size controls
  - native reader theme switch: system, sepia, dark
  - persisted reader font/theme settings
  - API reader content normalization covers `body_html`, `bodyHtml`, and
    `chapter_name`
  - Reader content now mirrors the website session flow: signed
    `GET /api/reader/session-key` with `X-Client-Signature`,
    `X-Client-Timestamp`, and `X-Client-Nonce`, followed by
    `GET /api/chapters/{chapterId}/content?session=...&replace_mode=india&show_images=1`
  - encrypted reader payloads are decrypted in native code with
    `AES/GCM/NoPadding`; the AES key is `SHA-256(base64Decode(session_key))`,
    and the response `content` ciphertext is finalized with the response `tag`
  - native reader text normalization decodes HTML entities, preserves paragraph
    and line breaks, and removes blank markup before Compose renders text
  - native chapter catalog now opens as an in-reader panel from the bottom
    `目录` action instead of being appended below the body scroll
  - stale reader API results are dropped before replacing content or saving
    progress when the current route/state no longer matches the requested
    chapter
  - same-chapter reader taps do not reload or duplicate the reader route, while
    switching chapters replaces the top reader route
  - reader-internal chapter switching replaces the current reader route instead
    of stacking duplicate reader pages
- Profile / My
  - native user center for account, reading preferences, connection settings,
    and website entry points
  - API current-user normalization accepts website-style profile aliases such
    as `uid`, `nickname`, and `user_role`
  - account sync, web login, token clear, reader preference, reader progress,
    and proxy-backed connection controls remain available without exposing
    package/API/debug shell copy
- Native API failure messages include the affected area, such as search,
  bookshelf, book detail, chapter catalog, or reader content.
- Native API failure messages hide implementation-only `/API` suffixes and
  endpoint paths from visible UI; HTTP errors are summarized as user-readable
  service status messages.
- Native error states now expose explicit retry actions for bookshelf,
  search, book detail, chapter catalog, reader content, and reader catalog
  instead of leaving the user on a passive error card.
- Native cover rendering for bookshelf/search cards and book detail, using
  fixed-size Compose image slots to avoid list layout shifts
  - cover loading and error states render a native title-initial fallback
    instead of a blank cover box
  - book cards/details prefer `fullCoverUrl` over the grid `coverUrl` when the
    API exposes an original/full cover, and Coil requests a 1024x1536 decode
    target to avoid visibly blurry card covers
- Native bookshelf/search cards show normalized facts chips for status, word
  count, update date, favorite count, site/source read counts, and source
  favorite count through `NovelCardFacts.kt`
- Native bookshelf/search cards show website-style tag chips through
  `NovelCardFacts.kt`, trimming blanks and de-duplicating without helper-level
  truncation.
- Cover URL normalization for common website API field names and relative asset
  paths
- Explicit WebView fallback for unported routes
- WebView fallback uses the same proxy settings through AndroidX WebKit
  `ProxyController` when the installed WebView supports proxy override
- Preserved WebView cookie reuse for API requests when CookieManager has a
  usable `novalpie.cc` session
- Native auth token bridge:
  - WebView fallback captures `auth_token` from `localStorage` or cookies
  - token is stored in native SharedPreferences through `AuthSessionStore`
  - native API requests send `Authorization: Bearer <token>`
  - Home exposes "web login/sync" and token clear controls
- Native connection settings in Profile:
  - default mode is automatic, not an always-on explicit proxy
  - native API and Coil image requests use multi-route proxy fallbacks
  - on x86/x86_64 MuMu QA, keep `adb reverse tcp:7890 tcp:7890` active; the
    native route order is `127.0.0.1:7890`, then `10.0.2.2:7890`, then direct
  - WebView fallback has only one proxy override slot, so its automatic
    emulator fallback uses `10.0.2.2:7890`
  - persisted in Android SharedPreferences
- Native deep links:
  - `novalpie://app/book/{bookId}`
  - `novalpie://app/book/{bookId}/{chapterId}`
- Compose first-stage UI copy was rewritten as UTF-8 Chinese text. The main UI
  source now avoids the previous mojibake strings while keeping WebView as an
  explicit fallback only.
- Native project verification now checks explicit first-stage Compose screen
  functions, route coverage, and proxy-control signals instead of relying on
  stale UI text literals.
- Native project verification also checks the first-stage product contract:
  home/bookshelf signals, search controls, book detail/catalog/favorite status,
  cover rendering, reader toolbar/body/progress/settings, Profile account and
  connection controls, API endpoint coverage, and SharedPreferences-backed local
  state stores.
- The default native home screen exposes a UIAutomator-visible semantics marker:
  `NOVALPIE_NATIVE_COMPOSE_HOME`. After MuMu/ADB recovers, use
  `adb shell uiautomator dump /dev/tty` and search for that marker to prove
  default launch is Compose-native.
- One-command MuMu runtime verifier:
  `powershell -ExecutionPolicy Bypass -File D:\NovalPie\native-android\tools\verify-mumu-compose-launch.ps1`
  It performs `adb install -r`, starts
  `com.novalpie.app/com.novalpie.nativeapp.MainActivity`, captures a screenshot,
  dumps the UI tree, and checks `NOVALPIE_NATIVE_COMPOSE_HOME`.

## Build

Use JDK 17.

Gradle is intentionally capped to reduce JVM native-memory failures on this
machine:

```properties
org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
```

```powershell
Set-Location D:\NovalPie\native-android
powershell -ExecutionPolicy Bypass -File D:\NovalPie\native-android\tools\build-release.ps1
```

Structural verification:

```powershell
powershell -ExecutionPolicy Bypass -File D:\NovalPie\native-android\tools\verify-native-project.ps1 -RequireApk
```

## Latest 2026-07-11 Turn 26 network/cover/tag verification

- Product fixes:
  - automatic proxy fallback now covers direct, `127.0.0.1:7890`, and
    `10.0.2.2:7890` for native API and Coil image loading;
  - WebView fallback keeps `10.0.2.2:7890` as the single emulator proxy because
    WebView cannot try multiple proxy routes;
  - search/bookshelf/detail cards render `fullCoverUrl` when available and
    decode covers at 1024x1536;
  - cards render website-style tag chips instead of hiding tags in a cramped
    text line;
  - managed book edit tags no longer absorb `spans` status badges.
- Verification:
  - `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
    passed: 233 tests, 0 failures.
  - `.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain`
    passed.
  - `.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain`
    passed.
  - MuMu instance 0 was started with `MuMuManager.exe api -v 0 launch_player`,
    connected through `127.0.0.1:16384`, installed with `adb install -r`, and
    verified with `adb reverse tcp:7890 tcp:7890`.
- Runtime screenshots:
  - `D:\NovalPie\agent-bridge\screenshots\codex-runtime-home-search-tags-20260711-0318.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-runtime-card-tags-20260711-0322.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-runtime-search-results-tags-20260711-0324.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-runtime-search-results-tags-visible-20260711-0324.png`
- APK:
  - debug: `D:\NovalPie\NovalPie-native-2.0-debug-turn26-20260711.apk`
  - debug SHA256:
    `B8A89F51F91D092C655019E0AB8B26848716E32522DD5FFAF12EA6F66E1C4881`
  - release unsigned SHA256:
    `84ECEAC7FF1C52F8A31DD0C896E9D7A2EBF5C7ABB984B918E337BC06B86199E3`

## Latest 2026-07-11 Turn 27 source/original-title runtime verification

- Product fixes:
  - native novel normalization now maps website/API original-title fields such
    as `true_name`, `original_title`, and `raw_title`;
  - native novel normalization now maps source/platform fields such as
    `platform`, `source_platform`, and `novel_type`;
  - search cards render original title, author, source pill, status/tag chips,
    and card facts together with the preferred full/original cover URL;
  - book detail hero also shows the original title and source fact.
- Verification:
  - `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
    passed: 234 tests in 49 suites, 0 failures/errors/skips.
  - `.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain`
    passed.
  - `.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain`
    passed.
  - MuMu `127.0.0.1:16384` was used with `adb install -r` and
    `adb reverse tcp:7890 tcp:7890`; no app data, WebView storage, or login
    state was cleared.
- Runtime screenshots:
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn27-collection-loaded-20260711-0407.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn27-search-tags-visible-20260711-0407.png`
- APK:
  - debug: `D:\NovalPie\NovalPie-native-2.0-debug-turn27-20260711.apk`
  - debug SHA256:
    `9121EB7D2C892249ACCB5F05F6CB1CD2A94101F788D9F6E76A3539FA91042AB8`
  - release unsigned SHA256:
    `CABBF669887E7A28A0424E850FA0DD7FE3B679AFAA601C34598711F7266AE370`

## Latest 2026-07-11 Turn 28 reader image-preview parity

- Product fixes:
  - reader content now carries website chapter illustrations returned beside
    chapter text;
  - `/api/chapters/{id}/content?show_images=1` normalization accepts
    `illustrations`, `images`, `chapter_images`, `chapterImages`,
    `image_list`, and `imageList`;
  - reader text parsing now resolves website placeholders such as `[[img:2]]`
    into native illustration blocks instead of leaving them as plain text;
  - placeholder illustrations reuse the same tap/long-press full-screen image
    preview as HTML/Markdown reader images and book covers.
- Verification:
  - TDD red test first failed for missing `imagePlaceholders` and
    `ReaderContent.illustrations`;
  - focused Reader/API tests passed after implementation;
  - `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
    passed: 236 tests in 49 suites, 0 failures/errors/skips.
  - `.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain`
    passed.
  - `.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain`
    passed.
  - MuMu `127.0.0.1:16384` was used with `adb install -r` and
    `adb reverse tcp:7890 tcp:7890`; no app data, WebView storage, or login
    state was cleared.
- Runtime screenshots:
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn28-cover-preview-20260711-0438.png`
- APK:
  - debug: `D:\NovalPie\NovalPie-native-2.0-debug-turn28-20260711.apk`
  - debug SHA256:
    `D7AD1B059D45A1AA628694C26ED2CE713BDA09B325E71F6623D502308D61A60A`
  - release unsigned SHA256:
    `2EA6EEAF6886F85784715BDB219E490A98C85C61B8B3935D1CA223EC3BB8A72B`

## Latest 2026-07-11 Turn 29 MuMu network, cover, and tag verification

- Product fixes:
  - automatic emulator proxy fallback now tries `10.0.2.2:7890` before
    `127.0.0.1:7890`, matching MuMu's working route to the Windows host proxy;
  - WebView fallback also uses `10.0.2.2:7890` for its single automatic proxy
    override slot;
  - Coil image loading allows slower remote cover hosts with longer connect,
    read, and call timeouts;
  - bare `https://images.novelpia.com` cover URLs are dropped during API
    normalization because the host can return a tiny HTML response instead of
    an image;
  - search/list cover taps no longer open the image preview before the card can
    navigate; card and cover taps open book detail, while detail covers still
    support full-screen preview.
- Verification:
  - `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
    passed: 238 tests.
  - `.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain`
    passed.
  - `.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain`
    passed.
  - MuMu `127.0.0.1:16384` was used with `adb install -r`, preserving app data,
    WebView storage, and login state.
  - Live runtime confirmed that `10.0.2.2:7890` reaches the Windows proxy from
    MuMu. Turn 36 supersedes the current MuMu QA order to prefer the
    adb-reverse `127.0.0.1:7890` route first.
- Runtime screenshots:
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn29-launch-20260711-0522.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn29-search-idle-20260711-0526.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn29-search-results-20260711-0528.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn29-search-result-tags-20260711-0530.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn29-final-search-20260711-0536.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn29-final-book-detail-20260711-0538.png`
- Runtime result:
  - Discover/Search shows hot tags such as `奇幻 26777`.
  - Search results show clear covers, display title, original title, author,
    source/status/tag chips, and facts for word count, favorite count, site
    reads, and source reads.
  - Book detail opens from card/cover taps and shows cover, display/original
    title, favorite state, author/source/facts, tags, description, actions, and
    chapter catalog area.
  - Filtered logcat showed no app network/image exceptions during QA; observed
    `uiautomator` segmentation faults were MuMu tooling crashes, not app
    process crashes.
- APK:
  - debug: `D:\NovalPie\NovalPie-native-2.0-debug-turn29-20260711.apk`
  - debug SHA256:
    `0A9584184466B42A90CA251C3A1E7B4C9DCE0864D85E659138695F0C31BC96B1`
  - release unsigned: `D:\NovalPie\NovalPie-native-2.0-release-unsigned-turn29-20260711.apk`
  - release unsigned SHA256:
    `D31CFAB54C5692B77994D33CF96EDF859065F32D87FBC424A9EE02154E4BB3B6`

Install without clearing app data:

```powershell
adb install -r D:\NovalPie\NovalPie-native-2.0-release.apk
```

## MuMu evidence from 2026-07-06

- Default launch screenshot:
  `D:\NovalPie\smoke-results\native-android-compose-cookie-default.png`
- Default launch focus evidence:
  `D:\NovalPie\smoke-results\native-android-compose-cookie-default-display-summary.txt`
- Search page screenshot:
  `D:\NovalPie\smoke-results\native-android-compose-search.png`
- Settings page screenshot:
  `D:\NovalPie\smoke-results\native-android-compose-settings.png`
- Book deep link screenshot:
  `D:\NovalPie\smoke-results\native-android-compose-book-back-display3.png`
- Reader deep link screenshot:
  `D:\NovalPie\smoke-results\native-android-compose-reader-fixed-display4.png`
- Crash buffer:
  `D:\NovalPie\smoke-results\native-android-compose-default-crash-buffer.txt`
- Proxy settings screenshot:
  `D:\NovalPie\smoke-results\native-android-proxy-settings.png`
- Proxy-backed search screenshot:
  `D:\NovalPie\smoke-results\native-android-proxy-search.png`

## 2026-07-07 build and blocker note

- Latest 2026-07-08 21:40 native reader session/content parity pass:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `AF5BE1737541106BFE930C6A6E9E8E890030CCF5D229F6D24319488F9E3DCC59`
  - Size: `10342599`
  - Live mobile source snapshot:
    `D:\NovalPie\site-research\live-20260708-mobile`
  - Product change:
    Native reader content requests now reproduce the website's signed reader
    session handshake before chapter body loading. The content request sends
    the returned `session_id`, and encrypted website payloads are decrypted
    through `session_key` + `AES/GCM/NoPadding` instead of surfacing the old
    "encrypted/session dependent" failure.
  - Interface notes:
    Signature behavior lives in `NovalPieApi.readerSignatureHeaders()`.
    Website evidence was recovered from `CxFG0gqQ.js` and `B0ZzNCml.js`.
    Constants are kept in `NovalPieApi` as `USER_AGENT`,
    `READER_SIGNATURE_SECRET`, and `READER_BASE64_ALPHABET`.
  - Verification:
    TDD red tests added `chapterContentRequestsSignedReaderSessionAndSendsSessionParameter`
    and `chapterContentDecryptsWebsiteEncryptedPayloadWithReaderSessionKey`.
    Focused `NovalPieApiTest` report: 22 tests, 0 failures, 0 errors.
    Full `.\gradlew.bat --no-daemon :app:testReleaseUnitTest --console=plain`
    passed. `tools\build-release.ps1` passed, zipaligned, signed, verified,
    and copied the APK. MuMu runtime smoke was not executed because ADB lists
    no devices; attempted `127.0.0.1:16384`, `16416`, `16385`, `7555`,
    `54098`, and `15721`, all refused connection.

- Latest 2026-07-08 02:44 native forum comment-thread pass:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `18EDA614031D7CB89D48DA96EA156D753D3C4C79BA66CF67A6613DE2BCD20CD5`
  - Size: `10330311`
  - MuMu launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260708-024414`
  - Product change:
    Forum detail comments now group replies under parent comments in native Compose, show a `X 条评论 · Y 条回复` summary, and keep orphan replies visible as standalone comments. This moves forum discussion closer to the website's reply flow without adding unsafe new endpoint behavior.
  - Verification:
    TDD red/green added `ForumPresentationTest.forumCommentThreadsGroupRepliesWithoutDroppingOrphans`; focused `ForumPresentationTest` passed after implementation. Full `.\gradlew.bat --no-daemon :app:testReleaseUnitTest --console=plain` passed. `tools\verify-native-project.ps1 -RequireApk` passed. `tools\build-release.ps1` passed and signed the APK. MuMu runtime verification passed on serial `127.0.0.1:16384` with `NOVALPIE_NATIVE_COMPOSE_HOME`.

- Latest 2026-07-08 02:30 native website-parity pass:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `13A607FD53C4323B82F9E2603C7E26A3A86CDECDCE7C06780CBB6A081A20DB3C`
  - Size: `10322119`
  - MuMu launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260708-023024`
  - Live mobile source snapshot:
    `D:\NovalPie\site-research\live-20260708-mobile`
  - Product changes:
    Native book cards/details now preserve website-style favorite/read/source metrics and full de-duplicated tags. Forum feed/detail data now carries `置顶` and `精华` badges. Discover/Search now includes the website `字数` range filter, persists it, and sends `min_word_count` / `max_word_count` to `/api/search`.
  - Verification:
    Focused release unit tests covered `NovelCardFactsTest`, `BookDetailFactsTest`, `ForumPresentationTest`, `ProductCopyTest`, `DiscoverPresentationTest`, `SearchSettingsStoreTest`, and `NovalPieApiTest`. Full `.\gradlew.bat --no-daemon :app:testReleaseUnitTest --console=plain` passed. `tools\verify-native-project.ps1 -RequireApk` passed. `tools\build-release.ps1` passed and signed the APK. MuMu runtime verification passed on serial `127.0.0.1:16384`; the UI tree includes the native Compose marker and the Discover evidence includes `字数: 不限`. Documentation closeout at `2026-07-08 02:36` re-ran the structural verifier and full release unit test after the README updates.

- Latest 2026-07-08 00:43 native forum feed metrics pass:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `01FBEFA993B3A6D54DE4E37522B75913DF64C030D01F8DB650DA23C15382285E`
  - MuMu launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260708-004356`
  - Product change:
    Forum feed rows now carry website-style footer metrics in native data and UI: replies, likes, reactions, rewards, and views. `/api/posts` normalization accepts common website aliases such as `like_count`, `reaction_count`, `award_points`, and `view_count`. `ForumPresentation.kt`, `ForumPresentationTest`, and `APP2_NATIVE_DESIGN_REFERENCES.md` were rebuilt with clean UTF-8 Chinese copy.
  - Verification:
    `ForumPresentationTest`, `ProductCopyTest`, and `NovalPieApiTest` passed through release unit-test XML with 0 failures. Full `.\gradlew.bat --no-daemon :app:testReleaseUnitTest --console=plain` passed. `tools\verify-native-project.ps1 -RequireApk` passed. `tools\build-release.ps1` passed and signed the APK. MuMu runtime verification is still blocked because both the Android SDK adb and MuMu adb list no devices.

- Latest 2026-07-07 21:42 native forum/search parity pass:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `93D093CC77DE11B87E0ACF4F0473801DFCBD7DC0B83BB47380C913AEACDB155D`
  - MuMu launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-214227`
  - Product changes:
    The native 2.0 design reference now treats NovalPie as a novel forum/community app, with FluxDO-style forum structure, PicaComic-style browse/detail/reader structure, and Legado only as reader-structure guidance. Forum comments now have link-preview extraction coverage. Discover/Search now exposes website-parity controls for sort field, sort direction, scope including `tags`, content filter, source (`全部 / NovelPia / 上传`), and search mode (`AI搜索 / 模糊-严格 / 模糊-宽松 / 精确匹配`). Native `/api/search` requests now send the `source` parameter, and search settings persist it locally. Product copy and tests were restored to clean UTF-8 Chinese.
  - Verification:
    Focused TDD red/green covered Discover/ProductCopy/SearchSettingsStore/NovalPieApi. Full test XML summary shows 27 files / 121 tests / 0 failures / 0 errors. `tools\verify-native-project.ps1 -RequireApk` passed. `tools\build-release.ps1` passed and signed the APK. MuMu install/launch verification is currently blocked because ADB reports no device and `127.0.0.1:5555` refuses connection; do not treat this as an app runtime failure until MuMu Android/ADB is healthy.

- Latest 2026-07-07 16:32 native Discover search-result priority:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `71C0F09FAD0B7DEF44B995D1F051E06C70E156E102DE9D6F4083D1C9B39A59C5`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-163139`
  - Discover visual/UIAutomator evidence:
    `D:\NovalPie\smoke-results\native-android-discover-native-ui-20260707-1632`
  - Product change:
    Discover/Search now exposes website hot tags natively, keeps visible copy
    in clean UTF-8 Chinese, and promotes live search results directly below the
    search surface after a query starts. Result rows use a lighter mobile book
    list layout with cover slot, author, two-line description, compact `字数 /
    更新` facts, and up to three tags. History, hot tags, and advanced filters
    remain available but no longer push results below the first screen.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, `tools\verify-mumu-compose-launch.ps1`, and
    UIAutomator/screenshot inspection of idle Discover plus live `奇幻` results.

- Latest 2026-07-07 15:41 native forum detail action bar:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `ACA08E6DB613BC388432DA3C20CA828F18284A08BD551A2EDC0340686E0E440C`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-154102`
  - Forum detail visual evidence:
    `D:\NovalPie\smoke-results\native-android-forum-detail-actionbar-20260707-1541`
  - Product change:
    Forum/Product presentation copy was restored to clean Chinese in
    `ProductCopy.kt` and `ForumPresentation.kt`. Native post detail now uses a
    compact equal-width forum action bar for `赞 / 踩 / 表情 / 打赏 / 网页`
    instead of oversized chip rows, and the 900px MuMu screenshot confirms the
    action labels fit without clipping.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, `tools\verify-mumu-compose-launch.ps1`, plus
    UIAutomator/screenshot inspection of a live native forum detail page.

- Latest 2026-07-07 14:29 native Reader catalog panel:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `5EF51F176F51147500C66161AE1FF7B9E369BD2D4D4E8F50D34BC5A55B323E1C`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-142820`
  - Reader visual evidence:
    `D:\NovalPie\smoke-results\native-android-reader-catalog-panel-20260707-1429`
  - Product change:
    Reader now keeps the body surface separate from the chapter catalog. The
    bottom `目录` action opens a native chapter panel with search, chapter
    summary, current-chapter marking, and `回到正文`, closer to a dedicated
    reader app while preserving only website-native reading/catalog behavior.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and `tools\verify-mumu-compose-launch.ps1`.

- Latest 2026-07-07 14:16 native Profile redesign:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `68E48DD3B91B514A1D5221A69D44B29808D05538DFA458E00B763744E7212C9E`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-141548`
  - Profile visual evidence:
    `D:\NovalPie\smoke-results\native-android-profile-redesign-20260707-1416`
  - Product change:
    The `我的` tab is now a native user center, not a Settings/debug panel. It
    shows account name/role/sync state, reading progress/preferences,
    connection state, account actions, and website entry actions while hiding
    package/API diagnostics and unsupported reader tooling.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, `tools\verify-mumu-compose-launch.ps1`, and a
    main-source mojibake scan plus updated presentation tests.

- Latest 2026-07-07 14:00 native Discover redesign:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `09BF61E1D465630E1233EFAD260BAD329184ADDE089C67522361A4298DFEA8ED`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-140001`
  - Discover visual evidence:
    `D:\NovalPie\smoke-results\native-android-discover-redesign-20260707-1400`
  - Product change:
    Discover/Search now opens as a native content-client search surface rather
    than a stacked parameter panel. The screen has clean UTF-8 labels, selected
    filter chips, compact filter groups, and an idle recommendation prompt rail
    so the empty state no longer reads as unfinished blank space.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, `tools\verify-mumu-compose-launch.ps1`, and a
    main UI mojibake scan over `app\src\main\java\com\novalpie\nativeapp\ui`.

- Latest 2026-07-07 13:45 native library redesign:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `66617A1CA8446A2512BBB446C40992308E5F142F7D9D2D8B28646546B6A184A1`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-134251`
  - Library visual evidence:
    `D:\NovalPie\smoke-results\native-android-library-redesign-20260707-1345`
  - Product change:
    Library/Bookshelf now opens as a reading-client surface with a compact
    overview, sync badge, stats, continue-reading row, combined group/filter
    controls, and cleaned UTF-8 copy. `LibraryPresentation.kt` and
    `LibraryPresentationTest` lock the visible structure, while
    `verify-native-project.ps1` checks the native library signals.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and
    `tools\verify-mumu-compose-launch.ps1`.

- Latest 2026-07-07 13:30 native forum compact badges:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `C39D7A264F5F5E5CDFEFE8D2D579BD2571C0C1FF14FD65255F940C25FF51B5ED`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-132932`
  - Forum visual evidence:
    `D:\NovalPie\smoke-results\native-android-forum-compact-badges-20260707-1330`
  - Product change:
    Forum feed rows now use compact text badges instead of large Material
    assist chips for pinned/category/tag labels. This keeps more topics visible
    in the first viewport and better matches a dense forum client.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and
    `tools\verify-mumu-compose-launch.ps1`.

- Latest 2026-07-07 13:21 native forum client feed:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `2FDE696FB9C336FCB7E21136999D8A4C3F7D5A99A00228B2E62501941BBDEE47`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-131953`
  - Forum visual evidence:
    `D:\NovalPie\smoke-results\native-android-forum-client-feed-20260707-1321`
  - Product change:
    Default Forum now uses clean UTF-8 copy and a forum-client feed model with
    author, related book, last-active time, compact tags, pinned topics, and
    reply counts. `verify-native-project.ps1` now checks those forum-client
    signals so the default entry cannot silently regress to a plain card list.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and
    `tools\verify-mumu-compose-launch.ps1`.

- Latest 2026-07-07 13:10 native book detail hero:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `D1B7FDE429258AEC66E4F53B41E01EF47EC048F1803BC020CF7B198243F4A286`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-130853`
  - Book detail visual evidence:
    `D:\NovalPie\smoke-results\native-android-book-detail-hero-20260707-1310`
  - Product change:
    Book detail now uses a content-client hero with cover, title, compact
    favorite state, facts/tags, description, and primary reading actions in one
    surface. The old separate favorite/progress/action card stack is no longer
    the first-screen structure.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and
    `tools\verify-mumu-compose-launch.ps1`.

- Latest 2026-07-07 13:00 native reader owned chrome:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `BD0E6E30DAC41F55E7942B22FF101EAFF2B3CA40AED38DF1FEC6D84AA3B279E7`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-125905`
  - Reader deep-link visual evidence:
    `D:\NovalPie\smoke-results\native-android-reader-owned-chrome-20260707-1300`
  - Reader product change:
    Reader routes no longer render the global `NovalPie / 书架` top app bar.
    The reader now owns a lightweight `返回 / 阅读 / 网页` chrome with current
    chapter progress, moving the page closer to a real reading client.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and
    `tools\verify-mumu-compose-launch.ps1`.

- Latest 2026-07-07 12:50 native reader copy cleanup:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `888099FC8EEAF8CE6C8D7C252EFEDC8291182AB22CB487FBDC1F4850F07761E0`
  - MuMu default launch evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-124930`
  - Reader deep-link visual evidence:
    `D:\NovalPie\smoke-results\native-android-reader-copy-fix-20260707-1250`
  - Fixes:
    top reader navigation now shows `返回` instead of mojibake, search/history
    and settings hardcoded labels no longer contain visible mojibake, reader
    theme labels are `系统 / 护眼 / 深色`, and reader content failures no
    longer expose `/api/...` paths in the visible error card.
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and
    `tools\verify-mumu-compose-launch.ps1`.

- Latest 2026-07-07 12:24 native forum-reader UI rebuild:
  - APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
  - SHA256: `613BF8ECA830B97339CF4CC32BEDB9573238B2019006B1902AEEAF0F961F503E`
  - MuMu evidence:
    `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-122410`
  - Verification passed:
    `.\gradlew.bat :app:testReleaseUnitTest --console=plain`,
    `tools\verify-native-project.ps1 -RequireApk`,
    `tools\build-release.ps1`, and
    `tools\verify-mumu-compose-launch.ps1`.
  - UI copy and visual cleanup:
    `ProductCopy.kt` now owns forum/card/detail/reader labels,
    `ProductCopyTest` blocks development/reference wording in forum cards,
    `ForumScreen` renders a denser forum feed with tabs, reply counts, and
    book/forum metadata, `ThemePaletteTest` locks a neutral green reading
    palette, reader debug identity/source lines remain hidden, and a common
    mojibake scan over `app\src\main\java\com\novalpie\nativeapp\ui\*.kt`
    produced no hits.

- Release APK rebuilt and verified:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Current SHA256:
  `71C0F09FAD0B7DEF44B995D1F051E06C70E156E102DE9D6F4083D1C9B39A59C5`
- `:app:compileReleaseKotlin`, release APK signing verification, and
  `tools\verify-native-project.ps1 -RequireApk` passed for this APK.
- Native cover rendering was added to the Compose bookshelf/search cards and
  book detail surface, with cover field and URL normalization in the API layer.
- Native cover rendering now uses `SubcomposeAsyncImage` with loading/error
  fallbacks derived from the book title, so search/bookshelf cards do not show
  visually empty cover slots while remote images are still loading or failed.
- Native search controls now expose key `/api/search` parameters directly in
  Compose instead of requiring the website fallback for basic sort/filter use:
  `sort_by`, `sort_order`, `scope`, `match_type`, and `adult_filter`.
- Native search API normalization now accepts `results`, `novels`, `list`, and
  `records` array aliases, with request-level `NovalPieApiTest` coverage for
  query parameters and result-card normalization.
- Native reader controls now expose previous/next chapter navigation, font-size
  controls, and system/sepia/dark reading themes directly in Compose.
- Native reader content API normalization now accepts `body_html`, `bodyHtml`,
  and `chapter_name`, with request-level `NovalPieApiTest` coverage for
  `/api/chapters/{chapterId}/content` query parameters.
- Native reader text rendering now uses `ReaderText.kt` to decode HTML entities
  such as `&nbsp;` and `&amp;`, preserve `<p>` and `<br>` boundaries, collapse
  layout-only whitespace, and drop blank markup before Compose renders
  paragraphs. This is covered by `ReaderTextTest`.
- Native reading progress now persists the last opened book/chapter locally and
  exposes a Continue Reading card on the Compose home screen.
- Native reading progress is also stored per book, so opening/reading book B no
  longer overwrites the detail-page progress hint for book A.
- Native reader font/theme settings are persisted locally, and reader-internal
  chapter switching now replaces the active reader route to keep Back behavior
  predictable.
- Native chapter catalogs now include a local Catalog filter on both book detail
  and reader screens.
- Native chapter API normalization now accepts additional website-style chapter
  aliases: `chapter_name`, `display_order`, `words`, and `created_at`. This is
  covered by request-level `NovalPieApiTest` using MockWebServer.
- Native chapter catalogs now sort normalized chapters by website order fields
  such as `display_order`/`chapter_number`, with stable fallback for chapters
  missing an order. This keeps book-detail catalogs and reader previous/next
  controls aligned to the website sequence.
- Native Profile now presents account, reading preference, connection settings,
  and website entry actions as a user center instead of a diagnostics screen.
- Native Bookshelf now includes a local Bookshelf filter for favorites by title,
  author, tag, or book id.
- Native Bookshelf and Search now support load-more pagination with page state,
  duplicate-id merging, and per-list loading guards.
- Native bookshelf/favorites API normalization now accepts `favorites` and
  `books` array aliases, with request-level `NovalPieApiTest` coverage for
  `/api/favorites` pagination/sort/type parameters.
- Native favorite-group API normalization now accepts `favorite_groups`,
  `group_name`, and `book_count`, with request-level `NovalPieApiTest`
  coverage for `/api/favorites/groups` preview parameters.
- Native Book detail now displays current-account favorite status using the
  observed read-only `/api/favorites/status?object_id={bookId}&type=novel`
  endpoint. Favorite management remains website fallback for now.
- Native book-detail and reader loads now use `RequestFreshness.kt` guards so
  late API responses from an older book/chapter cannot overwrite the currently
  visible book, reader content, or reader progress. This targets the previous
  Book A -> Back -> Book B stale-page behavior.
- Native Home, favorites pagination, Search, and search pagination now use
  request serials and search snapshots from `RequestFreshness.kt`, so late API
  responses from older refreshes, keywords, filters, or pages cannot overwrite
  the current Compose list state.
- Native error recovery now uses `ErrorRecovery.kt` and `ErrorRecoveryTest`:
  API error cards can show a surface-specific retry button wired to the owning
  native request, including bookshelf refresh, search, book detail/catalog, and
  reader content/catalog.
- Native reader progress labels now use `ReaderProgressLabel.kt` and
  `ReaderProgressLabelTest`; the reader header shows the current chapter
  position against the loaded catalog and falls back cleanly when the catalog is
  empty or does not contain the current chapter.
- Native reader adjacent-chapter controls now use `ReaderAdjacentChapter.kt`
  and `ReaderAdjacentChapterTest`; previous/next are only enabled when the
  current chapter exists in the loaded catalog.
- Native favorite groups are now actionable in Compose: `GroupSection` uses
  selectable chips, `NovalPieViewModel.selectFavoriteGroup` reloads the
  bookshelf, and `NovalPieApi.favorites` sends `group_id` with unit coverage.
- Native Book detail chapter catalogs now mark the same-book saved progress
  chapter through `BookDetailProgressMarker.kt` and
  `BookDetailProgressMarkerTest`.
- Native route navigation now uses `RouteStackPolicy.kt`: repeated taps on the
  same book detail, same reader chapter, or same Web fallback no longer stack
  duplicate pages, and reader chapter switches replace the current reader route.
- Native current-user and favorite-status API normalization now accepts
  additional website-style aliases: `uid`, `nickname`, `user_role`,
  `isFavorite`, `status_text`, and `favorite_group`. These are covered by
  request-level `NovalPieApiTest` cases and enforced by
  `tools\verify-native-project.ps1`.
- Native Book detail now reuses local reader progress: when the opened book
  matches the saved progress, the detail page shows a progress hint and a
  native `继续阅读` action that opens the saved chapter.
- Native book API normalization now accepts nested `data.novel` payloads,
  object-style author/tag values, and additional website-style aliases:
  `cover_path`, `synopsis`, `words`, and `created_at`. This is covered by
  request-level `NovalPieApiTest` using MockWebServer.
- Native ReaderProgressStore now keeps per-book progress keys and has
  Robolectric unit coverage for reading progress by requested book.
- Native Home now shows a recent-reading list from per-book progress, so more
  than one book can be resumed from the Compose home surface.
- ReaderProgressStore unit coverage now verifies recent-reading order and
  limit behavior.
- Native API failure formatting now uses `apiFailureMessage(label, throwable)`
  so visible errors keep context such as `搜索/API 请求失败`.
- Native search settings are now stored in `SearchSettingsStore`, so sort,
  order, scope, match type, and adult filter survive app restarts.
- Native search history is now stored in `SearchHistoryStore`, so nonblank
  search keywords survive app restarts, are shown most-recent-first, duplicate
  searches are promoted to the front, and the history is bounded to 10 entries.
- Search history has native UI on the Compose Search surface: clicking a
  history item re-runs the search, and the clear action removes local history.
- Native Search initializes its keyword box from the latest persisted search
  history entry, so restarting the app keeps the last nonblank search keyword
  visible.
- Native Compose UI copy was cleaned up in `NovalPieApp.kt`; UTF-8 source audit
  found no obvious mojibake remnants in the main UI file.
- Runtime install/screenshot verification is blocked because MuMu is not
  currently exposing Android or ADB:
  - `adb devices -l` returns no devices
  - `MuMuManager info --vmindex 0` reports `is_android_started=false` and
    `is_process_started=true`, with `launch_err_msg=VERR_ROM_START_TIMEOUT`
  - `MuMuManager adb --vmindex 0 --cmd connect` reports `errcode=-202`,
    `vm not ready, can not connect adb !!`
- Evidence directory:
  `D:\NovalPie\smoke-results\native-android-auth-sync-20260707-001`
- Latest cover-card build evidence:
  `D:\NovalPie\smoke-results\native-android-cover-cards-20260707-001`
- Latest native-search-controls build evidence:
  `D:\NovalPie\smoke-results\native-android-search-controls-20260707-001`
- Latest native-search-api-array-aliases build evidence:
  `D:\NovalPie\smoke-results\native-android-search-api-array-aliases-20260707-001`
- Latest native-search-history build evidence:
  `D:\NovalPie\smoke-results\native-android-search-history-20260707-001`
- Latest native-last-search-keyword build evidence:
  `D:\NovalPie\smoke-results\native-android-last-search-keyword-20260707-001`
- Latest native-reader-controls build evidence:
  `D:\NovalPie\smoke-results\native-android-reader-controls-20260707-001`
- Latest native-reader-content-api-aliases build evidence:
  `D:\NovalPie\smoke-results\native-android-reader-content-api-aliases-20260707-001`
- Latest native-reader-progress build evidence:
  `D:\NovalPie\smoke-results\native-android-reader-progress-20260707-001`
- Latest native-reader-settings build evidence:
  `D:\NovalPie\smoke-results\native-android-reader-settings-20260707-001`
- Latest native-catalog-filter build evidence:
  `D:\NovalPie\smoke-results\native-android-catalog-filter-20260707-001`
- Latest native-chapter-api-aliases build evidence:
  `D:\NovalPie\smoke-results\native-android-chapter-api-aliases-20260707-001`
- Latest native-book-detail-progress build evidence:
  `D:\NovalPie\smoke-results\native-android-book-detail-progress-20260707-001`
- Latest native-book-api-aliases build evidence:
  `D:\NovalPie\smoke-results\native-android-book-api-aliases-20260707-001`
- Latest native-per-book-progress build evidence:
  `D:\NovalPie\smoke-results\native-android-per-book-progress-20260707-001`
- Latest native-settings-diagnostics build evidence:
  `D:\NovalPie\smoke-results\native-android-settings-diagnostics-20260707-001`
- Latest native-bookshelf-filter build evidence:
  `D:\NovalPie\smoke-results\native-android-bookshelf-filter-20260707-001`
- Latest native-bookshelf-api-array-aliases build evidence:
  `D:\NovalPie\smoke-results\native-android-bookshelf-api-array-aliases-20260707-001`
- Latest native-favorite-status build evidence:
  `D:\NovalPie\smoke-results\native-android-favorite-status-20260707-001`
- Latest native-clean-compose-copy build evidence:
  `D:\NovalPie\smoke-results\native-android-clean-compose-copy-20260707-001`
- Latest native-verifier-tightened evidence:
  `D:\NovalPie\smoke-results\native-android-verifier-tightened-20260707-001`
- Latest native-compose-marker evidence:
  `D:\NovalPie\smoke-results\native-android-compose-marker-20260707-001`
- Latest one-command MuMu Compose launch verifier evidence:
  `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-043752`
- Latest first-stage contract verification evidence:
  `D:\NovalPie\smoke-results\native-android-first-stage-contract-20260707-001`
- Latest MuMu Compose launch verifier retry:
  `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-095621`
- Latest successful MuMu Compose launch verifier:
  `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-114316`
  - APK SHA256:
    `AD6C21C68262CBADEA4ADB72187BECC44660D0DE9A530BB76BCC5F6F70A33EA0`
  - UIAutomator marker found:
    `NOVALPIE_NATIVE_COMPOSE_HOME`
  - Serial:
    `127.0.0.1:5555`
- Latest forum-reader UI tab screenshots:
  `D:\NovalPie\smoke-results\native-android-forum-reader-ui-tabs-20260707-1131`
- Latest compact Discover screenshot:
  `D:\NovalPie\smoke-results\native-android-discover-compact-20260707-1143`
- Latest native forum-reader UI design spec:
  `D:\NovalPie\native-android\docs\superpowers\specs\2026-07-07-native-forum-reader-ui-design.md`
- Latest native forum-reader UI implementation plan:
  `D:\NovalPie\native-android\docs\superpowers\plans\2026-07-07-native-forum-reader-ui.md`
- Latest native book-detail facts/tag normalization build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native catalog summary build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native book-status normalization build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native card-facts build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native card-tags build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native bookshelf status-filter build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native bookshelf fact-filter build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native chapter fact-filter build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native profile/favorite-status API alias evidence:
  `D:\NovalPie\smoke-results\native-android-profile-favorite-status-api-aliases-20260707-001`
- Latest native reader text normalization evidence:
  `D:\NovalPie\smoke-results\native-android-reader-text-normalizer-20260707-001`
- Latest native request freshness evidence:
  `D:\NovalPie\smoke-results\native-android-request-freshness-20260707-001`
- Latest native search/home request freshness build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native error-recovery build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native reader progress-label build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native reader adjacent-chapter build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native favorite-group filter build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native book-detail progress-marker build:
  `D:\NovalPie\NovalPie-native-2.0-release.apk`
- Latest native route stack policy evidence:
  `D:\NovalPie\smoke-results\native-android-route-stack-policy-20260707-001`
- Latest native chapter display-order evidence:
  `D:\NovalPie\smoke-results\native-android-chapter-display-order-20260707-001`
- Latest native cover fallback evidence:
  `D:\NovalPie\smoke-results\native-android-cover-fallback-20260707-001`
- Latest native-pagination evidence:
  `D:\NovalPie\smoke-results\native-android-pagination-20260707-001`
- Do not uninstall, clear app data, or wipe WebView storage during MuMu QA.
  Use overwrite installs with `adb install -r` and keep `adb reverse
  tcp:7890 tcp:7890` before live API checks.

## Proxy notes

Direct access to `novalpie.cc` is not assumed to work in the current Android
test environment. Treat the proxy path as the default live-API path for MuMu QA.
Native API requests support an explicit HTTP proxy.

MuMu with a proxy running on the Windows host:

```powershell
adb reverse tcp:7890 tcp:7890
```

Current MuMu QA should keep `adb reverse tcp:7890 tcp:7890` active. The native
automatic route now tries `127.0.0.1:7890` first on MuMu/x86, then
`10.0.2.2:7890`, then direct. If App Profile uses an explicit proxy, that route
is tried first but the automatic fallbacks are still retained for API and image
loading.

Current MuMu proxy confirmation evidence:
`D:\NovalPie\smoke-results\native-android-proxy-confirm-20260706-001\proxy-confirm.json`.

If using a LAN-accessible proxy on the Windows host, set the host field to the
PC LAN IP and keep the proxy software's "allow LAN" option enabled.

On a real Android device, use either a device-local proxy app that listens on
`127.0.0.1:7890`, or configure the host/port to a reachable LAN proxy.

## Known alpha limits

- Website cookies are passed from `CookieManager` when available. Native auth
  token sync has been added; default Compose launch is verified in MuMu, while
  live account/API checks still require a valid website session and proxy.
- Reader content API can return 400/session-dependent responses for some
  chapters; the native reader keeps website reader fallback visible.
- Admin/payment/upload/workspace flows remain fallback-only.
- UI is a native foundation, not final commercial polish.

# Native 2.0 UI Direction

See `docs/APP2_NATIVE_DESIGN_REFERENCES.md` for the current native UI reference boundary:
FluxDO-style forum client structure, PicaComic-style browse/detail/reader structure, and Legado only as a reader-structure reference without source/rule/crawler/editor features.

## Latest 2026-07-07 15:41 native forum detail action-bar pass

- APK: `D:\NovalPie\NovalPie-native-2.0-release.apk`
- SHA256: `ACA08E6DB613BC388432DA3C20CA828F18284A08BD551A2EDC0340686E0E440C`
- MuMu evidence: `D:\NovalPie\smoke-results\native-android-mumu-compose-launch-20260707-154102`
- Forum detail evidence: `D:\NovalPie\smoke-results\native-android-forum-detail-actionbar-20260707-1541`
- Changes:
  - restored clean visible Chinese copy in forum/product presentation helpers
  - replaced native forum detail AssistChip action rows with compact icon action buttons
  - verified `赞 / 踩 / 表情 / 打赏 / 网页` fit in a live MuMu detail screenshot
  - preserved the native forum feed, website fallback, and Material bottom navigation icons

## Latest 2026-07-11 06:34 native bookshelf/search card data pass

- APK: `D:\NovalPie\NovalPie-native-2.0-debug-turn31-bookshelf-search-cards-20260711.apk`
- SHA256: `FA104417BC99372875E6CD25FC470506CBA23458D39B80D8BBA05A6EEAEF25B6`
- Release unsigned APK:
  `D:\NovalPie\NovalPie-native-2.0-release-unsigned-turn31-bookshelf-search-cards-20260711.apk`
- Release unsigned SHA256:
  `921BB58C8C6BECE56A0F25401D0CE6DD98D8B6D6178DC6E0DA224EBA631C299B`
- Tests:
  - `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.data.NovalPieApiTest' --tests 'com.novalpie.nativeapp.ui.NovelCardFactsTest' --console=plain`
  - `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
  - `.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain`
- Runtime evidence:
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn31-home-cards-20260711.png`
  - `D:\NovalPie\agent-bridge\screenshots\codex-turn31-search-results-20260711.png`
- Live read-only evidence:
  - `D:\NovalPie\site-research\live-favorites-auth-20260711.json`
  - `D:\NovalPie\site-research\live-search-qihuang-auth-20260711.json`
  - `D:\NovalPie\site-research\live-tags-auth-20260711.json`
- Changes:
  - `/api/favorites` now treats `object_id` as the real novel id when the
    favorite item represents a novel, avoiding navigation to favorite-record ids.
  - Bookshelf cards now parse live `favorite_type`, `novel_type`, `spans`,
    `novel_read`, and `novel_like` fields.
  - Search/list cards now show source and tag chips immediately after the title,
    before original title/author, so mobile first viewport no longer looks like
    cover-only results.
  - MuMu runtime confirmed loaded bookshelf/search data and clear cover images;
    filtered logcat showed no NovalPie API, Coil, or app crash signatures.

## Latest 2026-07-11 07:30 native book comments pass

- APK: `D:\NovalPie\NovalPie-native-2.0-debug-turn32-book-comments-20260711.apk`
- SHA256: `D2FFC82C7CF18EF3AE18F8BE0D735D624B0680E88302A33FADE9009ACBE853BF`
- Live read-only evidence:
  - `D:\NovalPie\site-research\live-comments-book-20260711.json`
  - `D:\NovalPie\site-research\live-comments-chapter-with-book-20260711.json`
- Confirmed website API behavior:
  - book detail comments use `GET /api/comments?type=book&book_id={bookId}&page=...&limit=...`;
  - chapter comments require both `book_id` and `chapter_id`:
    `GET /api/comments?type=chapter&book_id={bookId}&chapter_id={chapterId}&page=...&limit=...`;
  - `/comments?...` returns the Nuxt page route, not the JSON API;
  - `type=novel` is rejected by the live API.
- Changes:
  - `NovalPieApi.chapterComments()` now calls `/api/comments` and accepts the
    parent `bookId`.
  - `NovalPieApi.bookComments()` now loads the current book's comment stream.
  - Book comments normalize website fields including `authorName`, `authorId`,
    `helpfulCount`, `notHelpfulCount`, `funnyCount`, and `awardCount`.
  - Book detail now renders a native comment section with loading, retry, empty,
    and comment-card states instead of only telling the user to open the website.
  - Reader chapter comments now pass `book_id` to match the live site contract.
- Verification:
  - focused API/UI tests passed:
    `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.data.NovalPieApiTest.chapterCommentsNormalizeWebsiteAliasesAndSendReadonlyParameters' --tests 'com.novalpie.nativeapp.data.NovalPieApiTest.bookCommentsNormalizeWebsiteAliasesAndSendReadonlyParameters' --tests 'com.novalpie.nativeapp.ui.BookDetailPresentationTest.bookDetailCommentMetricsMirrorWebsiteActions' --console=plain`
  - full release unit tests passed:
    `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
  - debug build passed:
    `.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain`
- Runtime note:
  - MuMu/ADB was not available for this slice. `adb connect 127.0.0.1:16384`
    returned connection refused and `adb devices` was empty, so no new emulator
    screenshot is claimed.

## Latest 2026-07-12 Turn 35 network/tag runtime and cover-preview build

- User-facing checks:
  - Collection loaded authenticated live data through the app proxy path:
    `我的收藏 20`, favorite groups, clear covers, and card tags such as
    `NovelPia`, `PLUS`, `独家`, and `连载中`.
  - Search loaded hot tags and live `/api/search` results for `奇幻`, showing
    clear covers and visible `上传 / 已完结 / 奇幻` chips.
  - Filtered logcat during the live flow showed no NovalPie API, Coil image,
    socket, DNS, TLS, or app-crash signatures.
- Product fix:
  - Search/Bookshelf grid covers now use the shared full-screen
    `ImagePreviewDialog` on cover tap or long-press again, while the rest of
    the card still navigates to book detail.
- Verification:
  - Release unit test XML/HTML report shows 246 tests, 0 failures:
    data package 103 tests and ui package 143 tests, both 100%.
  - `:app:assembleDebug` produced a new debug APK. Gradle processes had to be
    cleaned manually because this Windows/MuMu session left Java workers alive
    after command timeouts, but the APK timestamp and hash were regenerated.
  - Installed with `adb install -r`, preserving app data and login state.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn35-cover-preview-20260712.apk`
  - SHA256:
    `3DFA5E3CC602FC4D60001A8E6D161366219DEF0EF0D5105BC34C7B23A762CFEA`
- Runtime screenshots:
  - `D:\NovalPie\native-android\qa-screenshots\turn35\current_start.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn35\collection_book_cards_after_scroll.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn35\search_initial.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn35\search_results_after_query.png`
- Runtime limitation:
  - After installing the Turn 35 APK, MuMu dropped to `device offline`; repeated
    CLI launches left `is_android_started=false` and adb port
    `127.0.0.1:16384` unavailable/offline. Therefore the post-install
    cover-preview tap screenshot is still pending. No app data was cleared.

## Latest 2026-07-12 Turn 36 MuMu proxy fallback and cover/tag verification

- Root cause fixed:
  - `ProxySettings.toProxyRoutes(preferEmulatorProxy = true)` had a
    `preferEmulatorProxy` parameter but still tried `10.0.2.2:7890` before
    `127.0.0.1:7890`.
  - API calls and Coil image loading could lock onto one explicit proxy route,
    so an old saved `10.0.2.2:7890` setting could make MuMu look offline even
    when `adb reverse tcp:7890 tcp:7890` was active.
- Product fix:
  - MuMu/x86 fallback now prefers the adb-reverse loopback route
    `127.0.0.1:7890`, then `10.0.2.2:7890`, then direct.
  - Explicit proxy settings now keep automatic fallback routes instead of
    becoming a single point of failure.
  - `NovalPieViewModel` and `NovalPieImageLoading` now use the same
    `ProxySelector` route list for API JSON and cover images.
- Verification:
  - focused proxy/image tests passed:
    `:app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.data.NetworkConfigStoreTest' --tests 'com.novalpie.nativeapp.data.NovalPieImageLoadingTest'`
  - full release unit tests passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --console=plain`
  - debug build passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:assembleDebug --console=plain`
- MuMu runtime:
  - `mumu-cli control --vmindex 0 --version 15 launch` started Android 15.
  - ADB connected at `127.0.0.1:16384`.
  - `adb reverse tcp:7890 tcp:7890` succeeded and Windows had a live proxy
    listener on port `7890`.
  - Installed with `adb install -r`, preserving app data/login state.
  - Collection showed authenticated `我的收藏 20`, clear covers, and visible
    `NovelPia / PLUS / 独家 / 连载中` chips.
  - Search showed live hot tags including `奇幻 26779`, `同人 12805`, and
    `现代 12255`.
  - Search for `奇幻` returned `20 个结果` with clear covers and visible
    `上传 / 已完结 / 奇幻` chips.
  - Tapping a search-result cover opened the full-screen native image preview.
  - App PID logcat showed no NovalPie API, Coil image, socket, DNS, TLS, or app
    crash signatures. The observed `uiautomator dump` SIGSEGV is a MuMu tooling
    crash after dumping UI XML, not an app process crash.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn36-mumu-proxy-cover-tags-20260712.apk`
  - SHA256:
    `9CB02395C3485329A85B78883EF32E72FF1363DC7E597122366EEF9BDAB889AC`
- Runtime screenshots:
  - `D:\NovalPie\native-android\qa-screenshots\turn36\collection_cards_after_proxy_fix.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn36\search_initial_after_proxy_fix.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn36\search_results_after_proxy_fix.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn36\search_cover_preview_after_proxy_fix.png`

## Latest 2026-07-12 Turn 37 visible-label cleanup

- Product fix:
  - Removed remaining mojibake from ViewModel-driven visible feedback/loading
    labels for forum detail, forum comments, comment submit failures,
    forum post/comment actions, bookshelf/favorite groups, search, book detail,
    chapter catalog, and chapter comments.
  - Added `VisibleUiLabels.kt` so future ViewModel state messages use shared
    clean Chinese labels instead of inline fragile strings.
- Verification:
  - focused release unit tests passed:
    `:app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.ui.VisibleUiLabelsTest' --tests 'com.novalpie.nativeapp.ui.ApiFailureMessageTest'`
  - UI source scan for the known mojibake fragments returned no matches.
  - debug build passed with explicit success on the second up-to-date run:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:assembleDebug --console=plain`
  - installed with `adb install -r`, preserving app data/login state.
  - MuMu launch smoke passed; App PID logcat contained no crash, API, socket,
    DNS, TLS, or Coil exception signatures.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn37-visible-labels-20260712.apk`
  - SHA256:
    `F225465728F4D334215AD8B4E470A63A1A0ADC2EEF52043F3813195B1097B283`
- Runtime screenshot:
  - `D:\NovalPie\native-android\qa-screenshots\turn37\launch_after_visible_labels.png`

## Latest 2026-07-12 Turn 38 network/tag/detail-wrap verification

- Product fix:
  - `BookDetailHero` now uses wrapping `FlowRow` chips for book facts and tags.
    This prevents detail metadata such as author, source, favorite count,
    site/source read counts, status, and tags from being clipped off-screen on
    narrow Android widths.
- MuMu runtime:
  - MuMu Android 15 was relaunched through `mumu-cli control --vmindex 0 launch`.
  - ADB connected at `127.0.0.1:16384`.
  - `adb reverse tcp:7890 tcp:7890` succeeded before installation.
  - Installed with `adb install -r`, preserving app data/login state.
  - Collection loaded authenticated live data (`我的收藏 20`, `阅读历史 1`) and
    clear cover images.
  - Search for `奇幻` returned `20 个结果` with clear covers and visible
    `上传 / 已完结 / 奇幻` chips.
  - Scrolled search cards showed full metadata lines including word count,
    favorites, site reads, source reads, original title, and author.
  - Long-pressing a search cover opened the full-screen native image preview at
    100%.
  - Book detail loaded the same book with wrapped metadata chips:
    author, source, favorites, site reads, source favorites, status, and tag.
- Verification:
  - full release unit tests and debug build passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest :app:assembleDebug --console=plain`
  - Gradle warnings were unchanged pre-existing warnings for unsupported
    compileSdk/AGP pairing and unused/shadowed parameters.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn38-network-tags-detail-wrap-20260712.apk`
  - SHA256:
    `76CEF0306D254739E19E13C552DB89C150E4EB467C1E9E842F4CB1CCBC7AC7E7`
- Runtime screenshots:
  - `D:\NovalPie\native-android\qa-screenshots\turn38\launch_collection.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn38\search_results_postfix.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn38\search_results_scrolled.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn38\cover_preview.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn38\book_detail_wrapped.png`

## Latest 2026-07-12 Turn 39 reader image preview and progress persistence

- Product fixes:
  - Reader inline illustrations use the same native full-screen
    `ImagePreviewDialog` interaction as book covers: tap or long-press opens
    the large image, with double-tap zoom, pinch zoom, and drag while zoomed.
  - Reader illustration fallback numbering now counts images only, so the first
    image is labeled `正文插图 1` even when text blocks appear before it.
  - Reader illustration copy is centralized in test-covered presentation
    helpers: content description, tap/long-press hint, loading text, and error
    text.
  - Reader progress is no longer persisted before chapter content succeeds.
    `openReader()` now routes and loads first, while `loadReader()` saves the
    progress only when the正文 request succeeds. This prevents invalid/stale
    chapters from becoming permanent `继续阅读` entries after a 500/failed
    chapter request.
  - The home `继续阅读` card now exposes the existing clear-progress action as
    a visible `清除` button, so stale local progress can be removed without
    clearing app data.
- Verification:
  - focused reader/library tests passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.ui.ReaderPresentationTest' --tests 'com.novalpie.nativeapp.ui.ReaderTextTest' --tests 'com.novalpie.nativeapp.ui.LibraryPresentationTest' --console=plain`
  - full release unit tests and debug build passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest :app:assembleDebug --console=plain`
  - unchanged Gradle warnings remain for the AGP/compileSdk pairing and two
    pre-existing Kotlin warnings in data classes/API code.
- MuMu runtime:
  - Debug APK installed with `adb install -r`, preserving app data.
  - `adb reverse tcp:7890 tcp:7890` was applied before launch.
  - Home loaded live account/library data and showed the updated `继续阅读`
    card with both `继续阅读` and `清除` actions.
  - Attempting the previously saved `章节 1` progress before this fix returned
    `阅读器正文请求失败: 服务返回错误 500`, confirming the stale progress bug
    this turn prevents from being re-persisted.
  - App log scan showed no NovalPie process crash. Observed exceptions were
    MuMu/system screenshot or store-resolution noise, not app failures. ADB
    later went `device offline` after evidence was collected.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn39-reader-image-preview-progress-20260712.apk`
  - SHA256:
    `9D1B4A5805F09D59282DBDDF7A40A7FF63705B159D66896DE8291F0C06B376F2`
- Runtime screenshots:
  - `D:\NovalPie\native-android\qa-screenshots\turn39\reader.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn39\home_after_fix.png`

## Latest 2026-07-12 Turn 40 upload-editor route parity in Tools

- Product fix:
  - Tools/function center now exposes the website `/upload-editor` route as its
    own native card: `上传编辑器`.
  - `/upload` and `/upload-editor` are now separated in the app like the live
    website route map:
    - `/upload`: import EPUB and submit a book.
    - `/upload-editor`: chapter splitting, replace, AI regex, and draft editor.
  - The new card routes through the existing native `AppRoute.UploadEditor`
    path; it is not a WebView fallback.
  - The card icon mapping now treats `/upload-editor` as an editor/reading
    surface instead of falling through to the generic settings icon.
- Verification:
  - focused tools presentation test passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.ui.ToolsPresentationTest' --console=plain`
  - full release unit tests and debug build passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest :app:assembleDebug --console=plain`
  - unchanged Gradle warnings remain for the AGP/compileSdk pairing and two
    pre-existing Kotlin warnings in data/API code.
- MuMu runtime:
  - MuMu Android 15 initially relaunched, but repeatedly stopped during the
    install/screenshot flow and ADB returned no devices afterward.
  - No valid Turn 40 runtime screenshot is claimed. The failed attempt left
    zero-byte placeholder files in `qa-screenshots\turn40`; they are not
    evidence.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn40-upload-editor-tool-entry-20260712.apk`
  - SHA256:
    `05C49EF5D3C035748DDFD1DE73303C96A43175F7FFC4ECEC407D153104F4ADC5`

## Latest 2026-07-12 Turn 41 political-exam native overview

- Product fix:
  - Political exam landing now uses a dedicated native presentation model
    instead of scattered Compose literals.
  - The first screen now presents the source-site exam contract as a compact
    product overview:
    - login state: `已登录` / `需要登录`;
    - key stats: `100 题`, `30 分钟`, `80 分通过`, `每日次数受限`;
    - rules for single choice, multiple choice, true/false, fill blank, and
      source-account state synchronization.
  - The primary CTA now follows account state: `开始考试` for signed-in users
    and `登录后参加考试` for signed-out users.
  - This keeps `/political-exam` on the native route and makes the route easier
    to maintain against future website rule changes.
- Verification:
  - focused political exam presentation test passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.ui.PoliticalExamPresentationTest' --console=plain`
  - full release unit tests and debug build passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest :app:assembleDebug --console=plain`
  - unchanged Gradle warnings remain for the AGP/compileSdk pairing and two
    pre-existing Kotlin warnings in data/API code.
- MuMu runtime:
  - MuMu Android 15 failed to start this turn. `mumu-cli info --vmindex 0`
    reported `is_process_started=false` and `is_android_started=false`; `adb
    devices` was empty. No Turn 41 runtime screenshot is claimed.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn41-political-exam-overview-20260712.apk`
  - SHA256:
    `8FC58A2C08D0DA700183DD4087A355E854296E7AFAD5104A067A2E3EE0B2299F`

## Latest 2026-07-12 Turn 42 profile account-status parity

- Product fix:
  - The native Profile hero now surfaces website-style account state chips
    derived from `UserProfile`: account normal/banned/deleted, adult
    verification state, email binding, registration date, public check-in
    visibility, and auto-check-in state.
  - The Settings account card uses the same presentation helper, so account
    status remains consistent between the main `我的` page and app settings.
  - Date-like website timestamps are shortened to the visible `YYYY-MM-DD`
    portion for compact mobile chips.
- Verification:
  - focused profile presentation test passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.ui.ProfilePresentationTest' --console=plain`
  - full release unit test XML report shows 51 suites, 252 tests, 0 failures,
    0 errors, 0 skipped.
  - debug build passed:
    `.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:assembleDebug --console=plain`
  - The combined full test/build command timed out after the test report was
    written because Gradle worker cleanup hung on this Windows session; the
    XML report was independently summed before `assembleDebug` was rerun
    successfully.
- MuMu runtime:
  - `mumu-cli control --vmindex 0 --version 15 launch` and
    `MuMuManager.exe api -v 0 launch_player` both started the player briefly,
    but Android never reached `is_android_started=true`.
  - The process returned to `is_process_started=false`, and `adb devices`
    remained empty, so no Turn 42 emulator screenshot is claimed.
- APK:
  - `D:\NovalPie\NovalPie-native-2.0-debug-turn42-profile-account-status-20260712.apk`
  - SHA256:
    `656031BAE126414800843841B53ED7424EB2429AAC73D340022475F51979CDAF`
