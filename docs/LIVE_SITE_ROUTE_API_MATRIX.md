# NovalPie Live Site Route/API Matrix

Last verified: 2026-07-11

This file is the durable source-of-truth for the current native Android migration. It is based on the actively served `novalpie.cc` Nuxt assets and MuMu runtime behavior, not on old screenshots or dead source.

## Current site build

- Nuxt build id observed: `cbb20abd-9b37-4381-993e-3008189d738b`
- Primary runtime chunks inspected: `/_nuxt/CgAuL9Tc.js`, `/_nuxt/CxFG0gqQ.js`
- Website theme metadata: `#4F46E5`

## Current website routes

Public/authenticated routes observed in the live route map:

- `/`
- `/login`
- `/register`
- `/reset-password`
- `/search`
- `/favorites`
- `/forum`
- `/forum/:id`
- `/forum/create`
- `/user`
- `/user/:id`
- `/messages`
- `/workspace`
- `/upload`
- `/upload-editor`
- `/reader`
- `/book-detail/:id`
- `/book/:bookId`
- `/book/:bookId/:chapterId`
- `/book-edit/info/:id`
- `/book-edit/append/:id`
- `/book-edit/chapters/:id`
- `/political-exam`

Administrator-only routes observed in the live route map:

- `/admin`
- `/admin/review`
- `/admin/key-management`
- `/admin/operation-logs`
- `/admin/scraper-management`
- `/admin/shop`

The website has no `/tools` route. The Android Tools tab is an app-owned native function center that aggregates current website capabilities.

## Authentication and admin gating

The live website restores identity from the JWT payload before its profile refresh finishes. Relevant fields are:

- payload `sub`
- payload `exp`
- payload `data.username`
- payload `data.role`

The website considers the user an administrator only when `role === "admin"`.

The Android app now follows the same rule:

- decode the saved JWT for immediate identity restoration;
- reject expired/malformed JWTs;
- use `/api/users/me` as a later profile refresh;
- preserve the valid JWT identity when the profile refresh times out;
- append administrator cards only for `role == "admin"`;
- never show `/admin*` cards to ordinary users;
- keep server-side authorization as the final access control.

## Message API snapshot

Observed current endpoints:

- `GET /api/messages?page=&page_size=&message_type=&is_read=&priority=&keyword=`
- `GET /api/messages/{id}`
- `POST /api/messages/{id}/read`
- `POST /api/messages/read`
- `DELETE /api/messages/{id}`
- `DELETE /api/messages`
- `POST /api/messages/{id}/star`
- `GET /api/messages/stats`
- `GET /api/messages/settings`
- `PUT /api/messages/settings`
- `GET /api/messages/conversations`
- `POST /api/messages` (direct message)

Current message fields:

- `id`
- `message_type`
- `message_title`
- `message_content`
- `username`
- `created_at`
- `is_read`
- `is_starred`
- `priority`
- `action_url`
- `action_text`
- `extra_data`

Message type labels:

1. User interaction
2. Forum reply
3. System notification
4. Novel update
5. Comment reply
6. Like notification
7. Follow notification
8. Direct message
9. System announcement
10. Report notification

## Android coverage after Turn 21

Native now:

- Tools/function center shell
- message statistics and latest six-message preview
- full message inbox with keyword/type/read/priority filters and pagination
- message selection, batch read, batch delete, all-read, star, and single-delete operations
- ordinary message detail with action routing and extra metadata
- native direct-message conversation history and send composer
- native message notification settings
- route-specific message-center/detail/conversation/settings chrome
- website capability cards
- JWT-derived account/admin identity
- administrator-only route visibility

Safe embedded fallback now:

- workspace
- upload/editor
- political exam
- all six administrator pages

Still requiring native migration for full App 2.0 completion:

- workspace internals
- upload/editor flows
- political exam flow
- administrator dashboard/review/key/log/scraper/shop internals
- remaining profile and account-management routes

## Network policy

For the native app runtime, explicit user proxy settings are tried first, but
automatic fallback routes are kept for API and Coil image loading.

Automatic mode:

- ARM/normal devices: `10.0.2.2:7890`, then `127.0.0.1:7890`, then direct.
- x86/x86_64 MuMu/emulator runtime: `127.0.0.1:7890`, then
  `10.0.2.2:7890`, then direct.
- Native API and Coil image loading use a multi-route selector containing
  both host-proxy routes plus direct fallback.
- In MuMu QA, keep `adb reverse tcp:7890 tcp:7890` active so the preferred
  `127.0.0.1:7890` route reaches the Windows host proxy.
- WebView proxy override cannot express multiple routes, so the automatic
  WebView emulator fallback uses `10.0.2.2:7890`.

Turn 29 confirmed `10.0.2.2:7890` can reach the Windows host proxy from inside
MuMu. Turn 36 superseded the preferred order because the current MuMu QA path
uses adb reverse reliably through `127.0.0.1:7890`.

## Turn 26 network, cover, and tag verification

Build commands:

```powershell
.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain
```

Results:

- release unit tests passed: 233 tests, 0 failures;
- Debug and Release packaging passed;
- debug APK SHA256:
  `B8A89F51F91D092C655019E0AB8B26848716E32522DD5FFAF12EA6F66E1C4881`;
- release unsigned APK SHA256:
  `84ECEAC7FF1C52F8A31DD0C896E9D7A2EBF5C7ABB984B918E337BC06B86199E3`.

Runtime evidence:

- MuMu instance 0 was launched with `MuMuManager.exe api -v 0 launch_player`.
- ADB connected at `127.0.0.1:16384`.
- Debug package `com.novalpie.app.debug` was installed with `adb install -r`.
- `adb reverse tcp:7890 tcp:7890` was active for the host proxy route.
- Home/favorites loaded real account data, clear covers, and tag chips:
  `D:\NovalPie\agent-bridge\screenshots\codex-runtime-card-tags-20260711-0322.png`.
- Search loaded live `/api/search` results with clear covers and visible tag
  chips:
  `D:\NovalPie\agent-bridge\screenshots\codex-runtime-search-results-tags-visible-20260711-0324.png`.

Live read-only API confirmation:

- `GET /api/search?q=恋爱&page=1&limit=2...` returned `photo_url`, `spans`,
  `tags`, `word_count`, `favorite_count`, `site_read_count`,
  `source_read_count`, and `source_favorite_count`.
- No live mutating/admin/payment/upload endpoint was exercised in this turn.

## Turn 27 source/original-title runtime verification

Live read-only `/api/search` confirmation for query `奇幻` showed the current
search payload includes:

- localized display title: `title`;
- original/source title: `true_name`;
- author name: `author_name`;
- source platform: `platform`, with observed values including `upload` and
  `novelPia`;
- cover URL: `photo_url`;
- status/badge fields: `spans`;
- website tags: `tags`;
- counts: `word_count`, `favorite_count`, `site_read_count`,
  `source_read_count`, and `source_favorite_count`.

Native normalization now maps those fields to the card/detail model:

- `true_name`, `trueName`, `original_title`, `originalTitle`,
  `title_original`, `titleOriginal`, `raw_title`, and `rawTitle` map to
  `NovelCard.originalTitle`;
- `platform`, `source_platform`, `sourcePlatform`, `novel_type`, and
  `novelType` map to `NovelCard.platform`;
- `NovelCard.fullCoverUrl` remains preferred over the thumbnail/grid URL for
  card and detail rendering.

Runtime evidence:

- MuMu ADB serial: `127.0.0.1:16384`;
- install mode: `adb install -r`, preserving data/login state;
- proxy bridge: `adb reverse tcp:7890 tcp:7890`;
- collection page loaded real account counts and cover grid:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn27-collection-loaded-20260711-0407.png`;
- search result page loaded 20 live results for `奇幻` and visibly showed
  full cover artwork, Chinese display title, Korean original title, author,
  source pill (`上传`), status/tag chips (`已完结`, `奇幻`), and read/favorite
  facts:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn27-search-tags-visible-20260711-0407.png`.

Build and package verification:

```powershell
.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain
```

Results:

- release unit tests passed: 234 tests in 49 suites, 0 failures/errors/skips;
- Debug and Release packaging passed;
- copied debug APK:
  `D:\NovalPie\NovalPie-native-2.0-debug-turn27-20260711.apk`;
- copied debug APK SHA256:
  `9121EB7D2C892249ACCB5F05F6CB1CD2A94101F788D9F6E76A3539FA91042AB8`;
- release unsigned APK SHA256:
  `CABBF669887E7A28A0424E850FA0DD7FE3B679AFAA601C34598711F7266AE370`.

No live mutating/admin/payment/upload endpoint was exercised in this turn.

## Turn 42 profile account-status parity

Native profile coverage now includes a shared account-status presentation layer
for `/user` and the app Settings account card. It renders server-provided
`UserProfile` state without requiring a mutating request:

- account state: normal, banned until date, ban reason, or deleted;
- adult verification state;
- email binding presence;
- registration date from website timestamps;
- public check-in visibility;
- auto-check-in state.

The same helper is used by the `我的` profile hero and Settings account card,
so those two account surfaces stay aligned.

Verification:

- focused profile presentation test passed;
- full release unit test XML report: 51 suites, 252 tests, 0 failures/errors;
- debug APK packaging passed;
- APK:
  `D:\NovalPie\NovalPie-native-2.0-debug-turn42-profile-account-status-20260712.apk`;
- SHA256:
  `656031BAE126414800843841B53ED7424EB2429AAC73D340022475F51979CDAF`.

Runtime note: MuMu instance 0 could not reach Android startup in this slice.
Both `mumu-cli control --vmindex 0 --version 15 launch` and
`MuMuManager.exe api -v 0 launch_player` briefly started the player, but
`mumu-cli info --vmindex 0` returned to `is_process_started=false` and
`is_android_started=false`; `adb devices` stayed empty. No runtime screenshot is
claimed, and no live mutating/admin/payment/upload endpoint was exercised.

## Turn 36 MuMu proxy fallback, cover, and tag verification

Runtime evidence on `2026-07-12 13:15`:

- MuMu Android 15 recovered and ADB connected at `127.0.0.1:16384`.
- Windows proxy port `7890` was listening, and
  `adb reverse tcp:7890 tcp:7890` returned:
  `host-13 tcp:7890 tcp:7890`.
- The Turn 36 debug APK was installed with `adb install -r`, preserving app
  data and the authenticated session.

Root cause fixed:

- The native network layer exposed `preferEmulatorProxy` but did not actually
  reorder routes for MuMu/adb-reverse testing.
- API requests and Coil image requests could use a single explicit proxy route,
  so a stale saved `10.0.2.2:7890` setting could block real data, covers, and
  tags even while `127.0.0.1:7890` was reachable through adb reverse.

Current route policy:

- On x86/MuMu-style runtime, automatic API and image networking now tries:
  `127.0.0.1:7890` -> `10.0.2.2:7890` -> direct.
- Explicit proxy settings are still honored first, but automatic fallback routes
  are kept after the explicit route.
- `NovalPieViewModel` and `NovalPieImageLoading` both use the same
  `ProxySelector` route list, so JSON API calls and cover images fail over the
  same way.

Runtime user-facing confirmation:

- Collection loaded `我的收藏 20`, favorite groups, clear cover images, and
  visible chips such as `NovelPia`, `PLUS`, `独家`, and `连载中`.
- Search loaded hot tags such as `奇幻 26779`, `同人 12805`, and `现代 12255`.
- Searching `奇幻` loaded `20 个结果`, clear covers, and visible result chips
  including `上传`, `已完结`, and `奇幻`.
- Tapping a search-result cover opened the native full-screen image preview.
- App PID logcat had no NovalPie API, Coil image, socket, DNS, TLS, or app
  crash signatures. `uiautomator dump` SIGSEGV entries are MuMu tooling process
  failures after XML dumps, not crashes in `com.novalpie.app.debug`.

Verification:

```powershell
.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.data.NetworkConfigStoreTest' --tests 'com.novalpie.nativeapp.data.NovalPieImageLoadingTest' --console=plain
.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --console=plain
.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:assembleDebug --console=plain
```

Results:

- focused proxy/image tests passed;
- full release unit tests passed;
- debug build passed;
- APK:
  `D:\NovalPie\NovalPie-native-2.0-debug-turn36-mumu-proxy-cover-tags-20260712.apk`;
- SHA256:
  `9CB02395C3485329A85B78883EF32E72FF1363DC7E597122366EEF9BDAB889AC`.

Runtime screenshots:

- `D:\NovalPie\native-android\qa-screenshots\turn36\collection_cards_after_proxy_fix.png`
- `D:\NovalPie\native-android\qa-screenshots\turn36\search_initial_after_proxy_fix.png`
- `D:\NovalPie\native-android\qa-screenshots\turn36\search_results_after_proxy_fix.png`
- `D:\NovalPie\native-android\qa-screenshots\turn36\search_cover_preview_after_proxy_fix.png`

No live mutating/admin/payment/upload endpoint was exercised in this turn.

## Turn 28 reader image-placeholder and preview verification

Current website/source evidence:

- live book pages still load the current Nuxt client from `/_nuxt/CgAuL9Tc.js`
  and `/_nuxt/CxFG0gQ.js`/`CxFG0gqQ.js`-style chunks;
- live read-only `/api/search` remains reachable and returns current cover
  URLs for runtime cover-preview QA;
- previously captured current chapter-management source confirms body image
  placeholders use `[[img:N]]`, and chapter illustration management returns
  image fields including `id`, `index`, and `src`.

Native reader changes:

- `ReaderContent` now carries `illustrations: List<ChapterIllustration>`.
- `normalizeReaderContent()` reads image arrays from `illustrations`,
  `images`, `chapter_images`, `chapterImages`, `image_list`, and `imageList`
  when they are returned with `/api/chapters/{chapterId}/content`.
- `readerBlocksFromContent()` now accepts an `imagePlaceholders` map and
  resolves `[[img:N]]` into `ReaderContentBlock.Image` in the correct text
  order.
- Unknown/missing placeholders are preserved as text instead of being silently
  dropped.
- Resolved placeholder images use the same native `ReaderIllustration` full
  image preview path as ordinary HTML/Markdown `<img>` reader images.

Build and package verification:

```powershell
.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain
```

Results:

- release unit tests passed: 236 tests in 49 suites, 0 failures/errors/skips;
- Debug and Release packaging passed;
- copied debug APK:
  `D:\NovalPie\NovalPie-native-2.0-debug-turn28-20260711.apk`;
- copied debug APK SHA256:
  `D7AD1B059D45A1AA628694C26ED2CE713BDA09B325E71F6623D502308D61A60A`;
- release unsigned APK SHA256:
  `2EA6EEAF6886F85784715BDB219E490A98C85C61B8B3935D1CA223EC3BB8A72B`.

Runtime evidence:

- MuMu ADB serial: `127.0.0.1:16384`;
- install mode: `adb install -r`, preserving data/login state;
- proxy bridge: `adb reverse tcp:7890 tcp:7890`;
- search results loaded live cover images;
- tapping a book cover opened the full-screen native image viewer with title,
  close, zoom, reset, and fit controls:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn28-cover-preview-20260711-0438.png`;
- crash buffer entries observed during this QA were for MuMu `uiautomator`
  shutdown only; `com.novalpie.app.debug/com.novalpie.nativeapp.MainActivity`
  remained focused with a live pid after the screenshot.

No live mutating/admin/payment/upload endpoint was exercised in this turn.

## Turn 29 MuMu network, cover, and tag verification

Live network evidence:

- MuMu serial: `127.0.0.1:16384`;
- runtime architecture: x86/x86_64 emulator;
- MuMu route subnet: `10.0.2.0/24`;
- `10.0.2.2:7890` from inside MuMu reached the Windows host proxy and returned
  HTTP 200;
- `adb reverse tcp:7890 tcp:7890` was also active so `127.0.0.1:7890` remains a
  secondary route;
- live read-only `/api/search` returned tags, spans, counts, and cover URLs.

Native fixes:

- `ProxySettings.DEFAULT_PROXY_HOST` now defaults to `10.0.2.2`.
- Historical Turn 29 automatic proxy fallback order was `10.0.2.2:7890`, then
  `127.0.0.1:7890`; Turn 36 supersedes the MuMu/x86 order to
  `127.0.0.1:7890`, then `10.0.2.2:7890`, then direct.
- WebView fallback's automatic proxy route uses `10.0.2.2:7890`.
- Coil image HTTP timeouts were raised for slow remote cover hosts.
- `normalizeAssetUrl()` drops bare `https://images.novelpia.com` values because
  that host-only URL can return a tiny HTML response, not a usable cover.
- Search/list cover image taps no longer intercept card navigation; card and
  cover taps now open book detail, while detail covers still preview
  full-screen.

Build commands:

```powershell
.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain
```

Results:

- release unit tests passed: 238 tests, 0 failures;
- Debug and Release packaging passed;
- copied debug APK:
  `D:\NovalPie\NovalPie-native-2.0-debug-turn29-20260711.apk`;
- copied debug APK SHA256:
  `0A9584184466B42A90CA251C3A1E7B4C9DCE0864D85E659138695F0C31BC96B1`;
- copied release unsigned APK:
  `D:\NovalPie\NovalPie-native-2.0-release-unsigned-turn29-20260711.apk`;
- release unsigned APK SHA256:
  `D31CFAB54C5692B77994D33CF96EDF859065F32D87FBC424A9EE02154E4BB3B6`.

Runtime evidence:

- install mode: `adb install -r`, preserving app data, WebView storage, and
  login state;
- launch/bookshelf loaded covers:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn29-launch-20260711-0522.png`;
- search idle state loaded hot tags, including `奇幻 26777`:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn29-search-idle-20260711-0526.png`;
- search results loaded clear covers:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn29-search-results-20260711-0528.png`;
- search result cards showed title, original title, author, source/status/tag
  chips, word count, favorite count, site reads, and source reads:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn29-search-result-tags-20260711-0530.png`;
- final post-install search proof:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn29-final-search-20260711-0536.png`;
- tapping the card/cover opened native book detail:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn29-final-book-detail-20260711-0538.png`.

No app network/image exceptions were found in the filtered logcat during this
QA. `uiautomator dump` sometimes ended with a MuMu `Segmentation fault`; that
was a tooling crash, not an app crash.

No live mutating/admin/payment/upload endpoint was exercised in this turn.

## Turn 20 verification evidence

Build command:

```powershell
.\gradlew.bat --no-daemon :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease --console=plain
```

Result: `BUILD SUCCESSFUL`; 155 release unit tests passed.

APK hashes:

- debug: `3AB82EB00B6C8C876A20DD6A7E0A50BD6D3207117B8C41EA1C969F4BF0E3EFAE`
- release unsigned: `D02555AE3B068B675B00C572391101E063C6CCBF213862BA18187CD8533ECF67`

MuMu evidence:

- `turn20-final-tools-network-admin-20260710.png`
- `turn20-final-admin-gated-20260710.png`
- `turn20-final-admin-review-loaded-20260710.png`
- `turn20-final-admin-review-back-tools-20260710.png`

Observed runtime results:

- native message stats returned 275 total messages in about 10 seconds;
- latest message cards rendered;
- six administrator cards rendered for the administrator JWT;
- `/admin/review` rendered live review controls and rows;
- Android system Back returned to the native Tools screen;
- no app-process crash, TLS, DNS, socket timeout, or OOM signature was found after the final flow.

## Turn 21 native message-center verification evidence

Build command:

```powershell
.\gradlew.bat --no-daemon :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease --console=plain
```

Result: `BUILD SUCCESSFUL`; 169 release unit tests passed with zero failures, errors, or skips.

APK hashes:

- debug: `8DCFA7587CEF9598E584B5C6EBE8DC8B42A874C7CDF724D14EC8B49CEDDCDFA9`
- release unsigned: `948F895ABED152F55E117F13E7C5451EF63EC98314DCC260025723EAF748CDFD`

MuMu evidence:

- `turn21-native-message-center-correct-20260710.png`
- `turn21-native-message-conversation-20260710.png`
- `turn21-native-message-settings-20260710.png`
- `turn21-native-message-center-final-20260710.png` (ordinary message detail)

Observed runtime results:

- authenticated account loaded 275 real messages from the live API;
- inbox filters, pagination shell, detail, direct-message history, and settings rendered natively;
- no delete, star, send, batch, all-read, or settings-save mutation was executed against the live account;
- every mutation payload is covered by MockWebServer tests;
- no app-process crash or ANR was observed in the final read-only MuMu flow.

## Turn 22 native workspace and upload baseline

The live `/workspace` route is now represented by a native Compose product surface instead of an embedded page.

Observed workspace APIs from the actively served Nuxt chunk `commercial-app/research/nuxt-current/DJlDEw_L.js`:

- `GET /workspace/apis`
- `POST /workspace/apis`
- `PUT /workspace/apis/{id}`
- `DELETE /workspace/apis/{id}`
- `GET /workspace/cookie-status`
- `GET /workspace/cookie-config`
- `POST /workspace/cookie-config`
- `PUT /workspace/cookie-config`
- `DELETE /workspace/cookie-config`
- `GET /workspace/stats`
- `GET /workspace/translator-health`

Native workspace coverage:

- overview statistics and translator-health cards;
- server and local-only API configurations with masked credentials;
- create/edit/delete and explicit local-versus-shared ownership;
- Cookie health/configuration management without rendering saved Cookie content;
- local translation queue status, pause/resume, and delete;
- native route from Tools and a native handoff to Upload.

The live `/upload` route is sourced from the current Nuxt chunks `CzlM00MT.js` and `DlU_hlIc.js`. Current website thresholds and protocols are preserved:

- EPUB server threshold: `50 * 1024 * 1024` bytes;
- file chunk size: `5 * 1024 * 1024` bytes;
- `POST /api/uploads/chunks` for each multipart file chunk;
- `POST /api/uploads/chunks` with JSON `{action:"merge", file_id, file_name, total_chunks}`;
- `POST /api/uploads/epubs` with `{file_path, parse_only:true}` for server parsing;
- `POST /api/uploads/books` multipart submission;
- website-compatible fields including `title`, `title_translation`, `author_name`, `description`, `language`, `spans`, `is_adult`, `source`, `source_url`, `tags`, `submit_type`, `chapters`, `chapters_md5`, `epub_file_path`, `epub_file`, and `cover_url`.

Native upload coverage currently includes:

- Android document picker and persistable read permission;
- local EPUB metadata/manifest/spine parsing in reading order;
- image entries skipped during parsing instead of being decoded into memory;
- stream-backed multipart request bodies;
- range-backed 5 MiB chunk requests that never allocate the complete file;
- server parser normalization for metadata, hierarchy, section paths, raw paths, and spine indices;
- source-aligned metadata, language, adult flag, source URL, cover URL, tags, submit type, and chapter-preview UI;
- explicit confirmation before any live upload mutation.

Verification completed in this slice:

- workspace API/local-store/presentation tests pass;
- upload API, chunk protocol, EPUB parser, presentation, navigation, and message DELETE regression tests pass;
- Release and Debug Kotlin compilation passes;
- Debug APK packaging passes;
- live workspace/upload read-only runtime QA is pending because MuMu instance 0 currently starts its desktop processes but does not bring up Android or listen on ADB port `127.0.0.1:16384`.

No workspace or upload mutation was executed against the live account.

## Turn 23 native upload editor and media parity baseline

The native `/upload-editor` route now covers the website's core local editing
workflow without embedding the website:

- text and EPUB import with selectable text encodings;
- regex/plain find and replace;
- regex, Markdown heading, keyword-number, character-count, and paragraph-count
  chapter splitting;
- website-compatible `##__T[00001]__##` / `##__C[00001]__##` identifiers and
  validation;
- chapter add/edit/delete and metadata editing;
- file-backed local archives whose list operation does not load every body;
- standards-based EPUB export and direct transfer into the native upload form.

Current live book-detail source confirms that full cover preview uses:

```text
GET /api/novels/{id}/photo?favorite_type=novel
```

The preferred response field is `photo_true_url`, with `photo_url` retained as
a fallback. Native book-detail loading now requests this endpoint in parallel;
failure is non-fatal and falls back to the cover already present in detail data.

Native image parity now includes:

- website-style 2:3 cover layout and exact Coil precision;
- tap or long-press on a cover to open a full-screen preview;
- 1x-6x pinch/button zoom, pan, double-tap zoom/reset, fit/reset, and Back/close;
- reader HTML/Markdown image extraction in source order, including `data-src`,
  `data-original`, `src`, relative URLs, protocol-relative URLs, and alt text;
- tap or long-press on reader illustrations to use the same full-screen viewer;
- bounded 2048-pixel inline and 3072-pixel preview decode requests to avoid the
  previous large-bitmap allocation failure mode while retaining useful detail.

Verification command:

```powershell
.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease --console=plain
```

Result: `BUILD SUCCESSFUL`; 192 tests in 43 suites passed with zero failures,
errors, or skips.

APK hashes:

- debug: `207B994C2B9165BBC714EA47122394F55A62C42D1E376C6630F3D87E99556C35`
- release unsigned: `8E37CE1AB4FCC8E987CC433F301AD03E77DB1BD4D855437A13359228401562AF`

MuMu runtime media QA remains pending. Instance 0 accepted a normal launch but
its Android 15 engine exited before ADB became available; final state returned
to `is_process_started=false` and `is_android_started=false`. No instance data,
login state, or installed package was cleared.

## Turn 24 native forum authoring and managed-book baseline

The current `/forum/create` Nuxt chunk `BPzy_gx3.js` confirms the website
authoring contract:

- `POST /api/posts` with `type`, `title`, `content`, and `tags`;
- optional `poll` with `question`, 2-10 unique `options`, `allowMultiple`,
  `maxChoices`, and ISO `endsAt`;
- ordinary accounts can publish `recommend`, `discussion`, and `feedback`;
- `announcement` is available only when the exact role is `admin`;
- guest accounts cannot publish.

Native forum coverage now includes category selection, Markdown body and link
preview, website length/tag validation, complete poll editing, explicit publish
confirmation, stale-navigation protection, and success routing to the newly
created post. The forum FAB is no longer a stub.

The current book-information page chunk `Cx2z-4LE.js` confirms:

- `GET /api/novels/{id}/detail`;
- `GET /api/users/me/novels/{id}/permissions/check`;
- `PUT /api/novels/{id}/photo` multipart field `cover`;
- `PATCH /api/users/me/novels/{id}` with `title`, `title_translation`,
  `author_name`, `description`, `source`, `source_url`, `language`, `spans`,
  `is_adult`, `photo_url`, and `tags`.

The native editor renders every server permission as an enabled/disabled field,
uploads the original cover bytes without App-side compression, supports the
shared full-screen image viewer, requires save confirmation, and renders
`failed_fields` instead of silently treating a partial save as success.

Current chapter-management source confirms:

- `POST /api/users/me/chapters/append` multipart append;
- `POST /api/users/me/chapters/reorder`;
- `POST /api/users/me/chapters/insert`;
- `PATCH /api/users/me/chapters/{id}`;
- `DELETE /api/users/me/chapters/{id}`;
- `POST /api/users/me/chapters/batch-delete`;
- `POST /api/users/me/novels/{id}/translation-requests`.

Native chapter management now supports selection, move up/down, explicit order
save, insert, encrypted-content load then edit, single/batch delete, personal or
shared translation requests, and EPUB append. Append requests use the website's
50-chapter batching form for large payloads. Order changes must be saved before
edit/delete/translation, and every destructive or externally mutating action has
an explicit confirmation.

Verification command:

```powershell
.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease --console=plain
```

Result: `BUILD SUCCESSFUL` in 10m 58s; 225 tests in 48 suites passed with zero
failures, errors, or skips. Release lint-vital and both APK packaging tasks
passed.

Current APKs:

- debug: `D:\NovalPie\native-android\app\build\outputs\apk\debug\app-debug.apk`
  - SHA256 `2AACDF4F798D12F913BFEE622590C65C95EF3F01C98DACD937BFD7CDB998A86F`
- release unsigned:
  `D:\NovalPie\native-android\app\build\outputs\apk\release\app-release-unsigned.apk`
  - SHA256 `9BE054979742DD8EF6F3510EB097E3D62157B9B4F8D63088FD307406EE540254`

Live-site browser verification reached the current book page but the selected
book was correctly blocked behind login/adult verification. No age verification
or form submission was attempted. MuMu instance 0 remained fully stopped after
a normal hidden CLI launch request (`is_process_started=false`,
`is_android_started=false`), so no new runtime screenshot is claimed for this
slice. No live post, book save, cover upload, chapter mutation, or translation
request was executed.

## Turn 25 native chapter illustrations, transfer, and access policy

Current Nuxt source confirms the chapter-illustration management contract:

- `GET /api/users/me/chapters/{chapterId}/illustrations`;
- `POST /api/users/me/chapters/{chapterId}/illustrations`;
- `DELETE /api/users/me/chapters/{chapterId}/illustrations/{imageId}`;
- multipart upload includes `chapter_id` and repeated `illustrations[]` file
  fields;
- the website accepts `image/*` and enforces a 20 MiB per-image limit;
- returned image fields include `id`, `index`, and `src`;
- chapter body placeholders use `[[img:N]]`.

Native chapter management now includes an image-management dialog per chapter:

- list existing illustrations with normalized relative/absolute URLs;
- preview illustrations through the shared full-screen image viewer;
- upload multiple original image files without App-side compression;
- delete a selected illustration with confirmation;
- insert `[[img:N]]` into the open matching chapter editor draft;
- block illustration actions while unsaved chapter ordering is pending.

Current managed-book source confirms:

- transfer submit: `POST /api/users/me/novels/{bookId}/transfers` with JSON
  `{identifier}`;
- access policy update:
  `PATCH /api/users/me/novels/{bookId}/permissions` with
  `allow_download`, `download_threshold_type`, `download_threshold_value`,
  `read_threshold_type`, and `read_threshold_value`;
- threshold types are `none`, `points_min`, and `points_pay`;
- `points_pay` is capped at 50 and `points_min` is capped at 100;
- when downloads are disabled, the download threshold is forced to `none`/`0`.

Native book editing now has dedicated cards for:

- basic field editing and original cover upload;
- read/download access policy editing;
- explicit managed-book transfer by UID or username.

Every live mutating operation remains behind an explicit confirmation dialog.
No live transfer, permission update, illustration upload, illustration delete,
book save, or chapter mutation was executed in this slice.

Verification command:

```powershell
.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease --console=plain
```

Result: `BUILD SUCCESSFUL` in 8m 56s; 231 tests in 49 suites passed with zero
failures, errors, or skips. Release lint-vital and both APK packaging tasks
passed.

Current APKs:

- debug: `D:\NovalPie\native-android\app\build\outputs\apk\debug\app-debug.apk`
  - SHA256 `8DDDE2314551A4D9979F38934CFC7D74A94F5F949A74AEA7BB5357F8AECD0881`
- copied debug test APK:
  `D:\NovalPie\NovalPie-native-2.0-debug-turn25-20260711.apk`
  - SHA256 `8DDDE2314551A4D9979F38934CFC7D74A94F5F949A74AEA7BB5357F8AECD0881`
- release unsigned:
  `D:\NovalPie\native-android\app\build\outputs\apk\release\app-release-unsigned.apk`
  - SHA256 `DE9229AEBF7A5D64EDDFDC3EFE86D54A2D546704E10059ADF62ABA2EF6028EA1`

MuMu runtime QA remains pending for this slice. ADB started successfully but
reported no attached devices, and `127.0.0.1:16384` was not listening. No app
data, login state, or installed package was cleared.

## Turn 31 native bookshelf/search card data parity

User-reported runtime concerns for this slice:

- app access appeared broken;
- covers appeared poor or incomplete;
- list/search cards appeared to have no tags.

Runtime investigation on MuMu (`127.0.0.1:16384`) showed native API access was
working for the debug package: Home loaded the authenticated bookshelf count,
favorite groups, covers, and favorite novels; Search loaded hot tags and query
results. The remaining verified defect was data/layout parity: the first mobile
viewport of grid cards prioritized cover/original title over tags and counters,
and `/api/favorites` live fields were not fully normalized.

Live read-only evidence captured through the host proxy:

- `D:\NovalPie\site-research\live-favorites-auth-20260711.json`
- `D:\NovalPie\site-research\live-search-qihuang-auth-20260711.json`
- `D:\NovalPie\site-research\live-tags-auth-20260711.json`

Confirmed `/api/favorites` live item fields:

- `id`: favorite-record id, not the novel id;
- `object_id`: actual novel id;
- `object_name` / `novel_title`: display title;
- `favorite_type`: source/platform such as `novelPia`;
- `novel_type`: genre/category such as `武侠`;
- `spans`: website chips such as `15 PLUS 独家 连载中`;
- `novel_read`: website read count;
- `novel_like`: like/favorite-style count used as the native compact count.

Native changes:

- `NovalPieApi.normalizeBook()` now prefers `object_id` for novel favorites so
  tapping bookshelf covers opens the real novel detail instead of the favorite
  row id.
- The same normalizer now reads `favorite_type`, `object_name`, `novel_type`,
  `novel_read`, and `novel_like`.
- `NovelCardItem` now renders source/tag chips and compact facts before original
  title and author, making tags visible in the first mobile viewport of Search
  and Bookshelf grid cards.

Verification:

- Focused tests passed:
  `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.data.NovalPieApiTest' --tests 'com.novalpie.nativeapp.ui.NovelCardFactsTest' --console=plain`
- Full release unit tests passed:
  `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
- Debug build passed:
  `.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain`
- Release unsigned build passed:
  `.\gradlew.bat --offline --no-daemon :app:assembleRelease --console=plain`
- Installed with `adb install -r`; app data and login state were preserved.

APK:

- `D:\NovalPie\NovalPie-native-2.0-debug-turn31-bookshelf-search-cards-20260711.apk`
- SHA256 `FA104417BC99372875E6CD25FC470506CBA23458D39B80D8BBA05A6EEAEF25B6`
- `D:\NovalPie\NovalPie-native-2.0-release-unsigned-turn31-bookshelf-search-cards-20260711.apk`
- SHA256 `921BB58C8C6BECE56A0F25401D0CE6DD98D8B6D6178DC6E0DA224EBA631C299B`

Runtime screenshots:

- before fix, Home showed loaded data and clear covers but less visible card
  metadata:
  `D:\NovalPie\agent-bridge\screenshots\codex-current-home-cards-20260711.png`
- after fix, Bookshelf cards show source, tags, collection-like count, and read
  count:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn31-home-cards-20260711.png`
- after fix, Search result cards show source and tag chips in the first viewport:
  `D:\NovalPie\agent-bridge\screenshots\codex-turn31-search-results-20260711.png`

Filtered logcat during runtime QA showed no NovalPie API, Coil image, or app
crash signatures. The only observed `UnknownHostException` belonged to MuMu's
own app store process (`store-api.mumu.163.com`), not NovalPie.

## Turn 32 native book comments and chapter-comment API correction

User-facing gap addressed in this slice: book detail still had a fallback-only
comment block, and reader chapter comments used the page route `/comments`
instead of the JSON API. Live source and runtime GET probes confirmed the
current website contract:

```text
GET /api/comments?type=book&book_id={bookId}&page={page}&limit={limit}
GET /api/comments?type=chapter&book_id={bookId}&chapter_id={chapterId}&page={page}&limit={limit}
```

Read-only live evidence:

- `D:\NovalPie\site-research\live-comments-book-20260711.json`
- `D:\NovalPie\site-research\live-comments-chapter-with-book-20260711.json`

Important negative evidence:

- `GET /comments?...` is a Nuxt page route, not JSON.
- `GET /api/comments?type=chapter&chapter_id=...` without `book_id` returns a
  JSON error for missing book id.
- `GET /api/comments?type=novel&book_id=...` returns a JSON error for invalid
  comment type.
- `GET /api/comments/book-reviews?page=...&book_id=...` currently returns the
  global forum review feed rather than a reliable per-book comment list, so the
  native detail page uses `type=book` for the book-specific section.

Native changes:

- `NovalPieApi.bookComments(bookId, page, limit)` reads the book-specific
  comment stream.
- `NovalPieApi.chapterComments(bookId, chapterId, page, limit)` now sends
  `/api/comments` with both ids.
- Comment normalization now accepts live website aliases:
  `authorName`, `authorId`, `helpfulCount`, `notHelpfulCount`, `funnyCount`,
  and `awardCount`.
- `BookDetailState` carries comment load state, and `loadBookDetail()` fetches
  comments in parallel with detail/catalog/favorite calls.
- `BookDetailScreen` now renders native book comments with loading, retry,
  empty, comment body, link preview, and website-style metric chips.
- `loadReader()` now passes `book_id` when syncing chapter comments.

Verification:

- TDD red test first failed for missing `bookId` in `chapterComments()` and
  missing `bookComments()`.
- Focused tests passed after implementation:
  `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --tests 'com.novalpie.nativeapp.data.NovalPieApiTest.chapterCommentsNormalizeWebsiteAliasesAndSendReadonlyParameters' --tests 'com.novalpie.nativeapp.data.NovalPieApiTest.bookCommentsNormalizeWebsiteAliasesAndSendReadonlyParameters' --tests 'com.novalpie.nativeapp.ui.BookDetailPresentationTest.bookDetailCommentMetricsMirrorWebsiteActions' --console=plain`
- Full release unit tests passed:
  `.\gradlew.bat --offline --no-daemon :app:testReleaseUnitTest --console=plain`
- Debug build passed:
  `.\gradlew.bat --offline --no-daemon :app:assembleDebug --console=plain`

APK:

- `D:\NovalPie\NovalPie-native-2.0-debug-turn32-book-comments-20260711.apk`
- SHA256 `D2FFC82C7CF18EF3AE18F8BE0D735D624B0680E88302A33FADE9009ACBE853BF`

Runtime note: MuMu/ADB was unavailable in this slice. `adb devices` returned no
connected devices and `adb connect 127.0.0.1:16384` returned connection refused,
so no emulator screenshot is claimed. No live mutating/admin/payment/upload
endpoint was exercised.

## Turn 35 network/tag runtime check and cover-preview restoration

Runtime read-only evidence on MuMu before the Turn 35 reinstall:

- Serial: `127.0.0.1:16384`.
- Package: `com.novalpie.app.debug`.
- Proxy bridge: `adb reverse tcp:7890 tcp:7890` succeeded; automatic app
  networking keeps both host-proxy routes. Turn 36 supersedes the MuMu/x86
  order to prefer `127.0.0.1:7890`, then `10.0.2.2:7890`, then direct.
- Collection loaded authenticated live data:
  - `我的收藏 20`;
  - favorite groups;
  - clear cover images;
  - visible website-style chips such as `NovelPia`, `PLUS`, `独家`,
    `连载中`.
- Search loaded live data:
  - hot tags including `奇幻 26779`, `同人 12805`, `现代 12255`;
  - `/api/search` returned 20 results for `奇幻`;
  - result cards showed clear covers and chips such as `上传`, `已完结`,
    `奇幻`.
- Filtered logcat showed no NovalPie API, Coil image, socket, DNS, TLS, or app
  crash signatures during the collection/search flow.

Native change:

- `NovelCardItem` now passes the preferred display cover URL into
  `BookCover(... previewUrl = displayCoverUrl)`. This restores tap/long-press
  full-screen preview specifically on the cover area while preserving detail
  navigation for the rest of the card.

Build/package evidence:

```powershell
.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:testReleaseUnitTest --console=plain
.\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs :app:assembleDebug --console=plain
```

Results:

- test report: 246 release unit tests, 0 failures;
- debug APK:
  `D:\NovalPie\NovalPie-native-2.0-debug-turn35-cover-preview-20260712.apk`;
- debug APK SHA256:
  `3DFA5E3CC602FC4D60001A8E6D161366219DEF0EF0D5105BC34C7B23A762CFEA`;
- installed with `adb install -r`, preserving app data/login state.

Screenshots:

- `D:\NovalPie\native-android\qa-screenshots\turn35\current_start.png`
- `D:\NovalPie\native-android\qa-screenshots\turn35\collection_book_cards_after_scroll.png`
- `D:\NovalPie\native-android\qa-screenshots\turn35\search_initial.png`
- `D:\NovalPie\native-android\qa-screenshots\turn35\search_results_after_query.png`

Runtime limitation:

- After the Turn 35 install, MuMu dropped from online to `device offline`.
- `mumu-cli info --vmindex 0` repeatedly returned
  `is_android_started=false`, and `adb connect 127.0.0.1:16384` failed or left
  the device offline.
- Because of that emulator state, the post-install cover-preview tap screenshot
  remains pending. Do not claim it as runtime-verified until MuMu/ADB is online
  again.

No live mutating/admin/payment/upload endpoint was exercised in this turn.
