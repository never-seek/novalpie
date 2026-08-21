# NovalPie Live Site Route/API Matrix

Last verified: 2026-08-17

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

### Native authentication route/API parity (Turn 46)

Current public source routes and request contracts:

- `/login`: password login and email-code login;
- `/register`: email -> six-digit verification code -> username/password;
- `/reset-password?token=...`: reset request and token-based password change;
- `POST /api/sessions` with `username`, `password`, and `turnstile_token`;
- `POST /api/verification-codes/login` and `/verify`;
- `POST /api/verification-codes/email` and `/verify`;
- `POST /api/users`, `POST /api/password-resets`, and `PUT /api/password-resets`.

Native coverage:

- `/login`, `/register`, and `/reset-password` map to native `AppRoute.Auth`.
- The native form validates the live source rules before a request: email shape,
  six-digit code, username length 3-50, and a password with 6+ characters plus
  upper-case, lower-case, and numeric characters.
- CAPTCHA remains source-owned in the small `AuthCaptchaScreen` WebView because
  the provider validates the `novalpie.cc` origin. Credentials and CAPTCHA
  tokens are never persisted by the screen.
- The CAPTCHA container removes only the source WebView's injected
  `localStorage.auth_token` once if an existing web session redirects `/login`;
  the Android auth token and cookies are not read, copied, or cleared. If the
  source still redirects, the user receives a visible fallback message instead
  of a misleading blank CAPTCHA page.
- System Back and the top navigation affordance cancel a pending CAPTCHA action
  rather than leaving it queued for a later tap.

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

## Android coverage, revised through Turn 95

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

Later migration turns replaced the historical fallback-only pages with native implementations:

- workspace internals;
- upload and EPUB-editor flows;
- political exam;
- administrator overview, review, key management, operation logs, scraper management, and shop;
- profile/account-management routes and uploaded-book search.

Intentional WebView use is now limited to source-owned CAPTCHA validation and explicit fallback
for an unknown external website link. It is not used for any path in the current source route map.

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

## Turn 50 administrator source parity

The administrator source bundle and a read-only MuMu runtime audit establish the
current native contract below. Administrator routes remain unreachable unless the
current profile has the exact `role == "admin"` value.

- `GET /api/admin/overview?days=<5|15|30>` supplies review totals, pending/
  approved Key totals, active translators, registered/today users, active works,
  and `recent_user_daily`. The native dashboard now exposes all of these fields,
  source-style day ranges, a compact growth chart, and direct shortcuts to every
  source administrator route.
- `/admin/review` uses `GET /api/admin/review-settings` and
  `GET /api/admin/review-requests?page=1&page_size=100&type=&status=&q=`.
  The source's `POST /api/admin/review-requests` `approve_all` action is now
  available behind an explicit confirmation, alongside the existing per-request
  approve/reject confirmation. No review mutation was sent during QA.
- `/admin/key-management` is a two-pane source page: Key review plus BaseURL
  rules. Native now loads `GET /api/admin/key-management` and
  `GET /api/admin/baseurl-rules` together, shows the source default `*` policy
  separately from specific allow/block/manual rules, and retains all existing
  confirmation-gated edits and deletes.
- `/admin/operation-logs` accepts `page`, `action`, `status`, `user_id`,
  `novel_id`, `keyword`, `start_date`, and `end_date`. Native now keeps the
  source user/novel/chapter/IP/content/result/agent/update metadata, uses source
  labels for known operations, offers an explicit detail expander, and restores
  source-style previous/next pagination.
- `/admin/scraper-management` is split into Cookie configuration and scheduler
  logs. Cookie cards retain active/health/count/update metadata but deliberately
  never render server cookie contents. The live scheduler endpoint returned an
  empty log stream during this audit; native renders that as an ordinary empty
  state rather than a failure.
- `/admin/shop` uses `GET /api/admin/shop/items?type=&is_active=<1|0>&keyword=&page=1&page_size=100`.
  Native now uses the source numeric active filter, supports type/status/keyword
  queries plus grid/list display, and renders original frame asset URLs with Coil.

Verification:

- Debug and Release unit tests: 55 suites / 299 tests / 0 failures / 0 errors /
  0 skipped each.
- Debug and unsigned Release packages built successfully.
- Debug SHA256: `F4FC7D85F56CE8C4063C12C2D7EEFD653C87D51B88E33AB6196C934F24E9E8C4`.
- Release unsigned SHA256: `BE2942C92741D35501597F80A9D50260AB455CBB5CC1FCD8A9A04176D7FDEFA4`.
- MuMu `127.0.0.1:16384` used `adb reverse tcp:7890 tcp:7890` and a data-preserving
  `adb install -r`. Dashboard, review, Key/rules, logs, Cookie/log split, and
  filtered shop-frame grid rendered without an app crash. No live administrator
  mutation, Cookie readout, review, delete, or item action was executed.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn50-admin-audit\admin-dashboard-redesign-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn50-admin-audit\admin-key-rules-redesign-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn50-admin-audit\admin-shop-frames-redesign-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn50-admin-audit\admin-logs-localized-final.png`

## Turn 49 account/profile source parity

Read-only source-account audit and MuMu QA established the following current contracts:

```text
GET /api/users/me
GET /api/users/me/checkins?start_date={YYYY-01-01}&end_date={YYYY-12-31}
GET /api/users/me/checkins/stats
GET /api/users/me/inventory
GET /api/users/me/quiz-reward
GET /api/users/me/uploads?user_id={currentUserId}
```

The current web bundle still advertises `GET /api/users/{id}/activities`, but
the live server returns `API endpoint not implemented in Laravel yet` for both
the owner and public-id forms. Native treats that as a recoverable display
state; it does not surface the server implementation string to users.

Native coverage added in this slice:

- Account profile now has a compact source-style hero with avatar, badges,
  UID, role, points, activity counters, refresh, and avatar update affordance.
- Root navigation no longer consumes a second generic product top bar on
  `/user`; the native account hero owns that space.
- The owner profile has `账号 / 签到 / 动态 / 书籍 / 背包` tabs.
- Check-in reads source records and aliases such as `days`, `points`,
  `maxStreak`, and `streak`; valid records safely fill a zeroed/unavailable
  summary without changing server state.
- Own uploaded books use the live `uploads` contract with `user_id`, preserving
  original covers, status/source badges, tags, and native detail navigation.
- Inventory normalization retains item type, quantity, image, slot, expiry, and
  server-equipped state. Quiz reward state is read-only.
- Avatar update, profile save, adult verification, and check-in remain explicit
  user-confirmed actions. No live mutation was performed for this audit.

Verification:

- Debug and Release unit tests: 55 suites / 297 tests / 0 failures / 0 errors /
  0 skipped for each variant.
- `:app:assembleDebug` and `:app:assembleRelease` passed offline.
- Debug SHA256:
  `225908286C22794710FDB8708142E6306A685BBD0F8DB1BE3F5C26A1942E8155`.
- Release unsigned SHA256:
  `17E13C94A2C5F19CA1BC615E78CBBB3001C9DD17D8BFA7927181BF847F503275`.
- MuMu Debug install used `adb install -r` with existing application data kept
  intact and `adb reverse tcp:7890 tcp:7890` active.
- Runtime evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn49-profile-account`.

## Turn 48 forum detail and comment-thread parity

The detail-page audit uses the live mobile route `https://novalpie.cc/forum/535`
and its read-only JSON dependencies:

- `GET /api/posts/{postId}` returns the author, title, source tags, pinned/featured
  status, formatted body, activity totals, creation time, and update time.
- `GET /api/posts/{postId}/comments?page={page}&limit={limit}` returns root comments with nested
  `replies`. The current mobile detail first window uses `page=1&limit=100`, rather than the
  generic list-page size. Each node can carry `authorAvatar`, `authorAvatarFrame`,
  `authorBadges`, `helpful_count`, `not_helpful_count`, `funny_count`,
  `award_count`, `reply_count`, and `created_at`.

Native implementation:

- `ForumComment` now retains source avatars, avatar frames, named badges, and reply
  counts. `NovalPieApi` recursively flattens nested `replies` while preserving the
  parent/reply-to relationship, so no source comment data is silently dropped.
- The native forum detail calls that endpoint with the source's 100-comment first window.
  Its API regression test asserts both `page=1` and `limit=100`, so a generic paging constant
  cannot silently truncate a busy thread again.
- Current review/feed responses also carry structured `authorBadges` records with `id`, `name`,
  `badge_html`, and `badge_css`. Native stores that visual metadata on forum posts, forum comments,
  book comments, and chapter comments, then maps it to the existing safe Compose badge renderer.
  It does not execute server HTML or CSS; plain-text labels remain a fallback for older responses.
- Forum HTML/Markdown is rendered through a small native rich-text presentation
  layer: HTML `<strong>`/`<b>`, Markdown `**bold**`, HTML/Markdown links, and bare
  `http(s)` URLs are preserved. Internal relative links normalize to
  `https://novalpie.cc/...`; links are actionable rather than a static preview list.
- The native detail route hides the generic product top bar and uses a compact
  `返回论坛` affordance. Author avatar/badges/date, source tags, pinned/featured
  status, body, and interaction row now live in one continuous source-style card.
- Unauthenticated readers see the source-equivalent login prompt. Comment threads
  show only roots initially and provide `展开 N 条回复` / `收起 N 条回复`; reply rows
  retain avatars, frames, badges, reply target, rich content, and all source actions.

Verification:

- Focused API/presentation/reader tests: 82 tests, 0 failures.
- Full release report: 55 suites / 291 tests / 0 failures / 0 errors / 0 skips.
- `:app:assembleDebug` and `:app:assembleRelease` completed successfully.
- Debug APK SHA256:
  `7CD3D2385A15832ED74C2EB15FDEAD7F2A4F0CA01E33F37CD3496DEDB501B4D0`.
- Unsigned Release APK SHA256:
  `F146C74BCD860E5A0CD3A76858D7EB473716D18D6B01DF816D1D0AA3D1C944B2`.
- MuMu `127.0.0.1:16384` retained its existing data. With
  `adb reverse tcp:7890 tcp:7890`, live forum root/detail/comments, reply expansion,
  Android Back, original book cover clarity, and complete book tags all rendered
  successfully. No post, comment, reaction, award, upload, administrator, or payment
  mutation was performed.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn48-forum-detail-parity\forum-root-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn48-forum-detail-parity\forum-detail-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn48-forum-detail-parity\forum-comments-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn48-forum-detail-parity\forum-replies-expanded-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn48-forum-detail-parity\forum-back-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn48-forum-detail-parity\book-detail-live.png`

## Turn 47 live mobile forum feed audit and native parity

Read-only mobile browser audit of `https://novalpie.cc/forum` established the
current source behavior:

- The root page begins with `搜索帖子...`, followed by the five tabs `公告`,
  `推书`, `交流`, `书评`, and `反馈`; source URL state uses `tab` and `search`,
  for example `/forum?tab=review&search=dsv4f`.
- The feed endpoint is `GET /api/posts?page=1&limit=20`, with `type` set to the
  selected source category and `search` set only when a query is submitted.
- Live post rows include `avatar`/`authorAvatar`, `authorBadges`, `content_preview`,
  `created_at`, `helpful_count`, `not_helpful_count`, `funny_count`, `award_count`,
  `comment_count`, `view_count`, `is_pinned`, `is_featured`, and raw `tags`.
- `type` values map to the mobile labels as follows:

  - `announcement` -> `公告`
  - `recommend` -> `推书`
  - `discussion` -> `交流`
  - `review` -> `书评`
  - `feedback` -> `反馈`

Native implementation:

- `ForumPost` and `ForumFeedItem` now keep source avatar, author badge, preview,
  create time, raw topic tags, and separate reaction counters instead of flattening
  them into a generic forum card.
- Relative source avatar URLs run through `normalizeAssetUrl`, matching the
  existing cover-resolution behavior.
- `ForumState` carries selected source type, submitted search query, page, and
  non-destructive load-more state. A failed additional page leaves prior posts in
  place.
- The Compose root uses source-style search/tabs/cards, avoids the old fabricated
  stats strip, hides global root chrome on the forum route, and retains a compact
  logged-in create button for the already-native `/forum/create` flow.

Verification:

- Focused API/presentation tests passed before full validation.
- Full release test report: 55 suites / 288 tests / 0 failures / 0 errors / 0 skips.
- `:app:assembleDebug` and `:app:assembleRelease` completed successfully.
- Final Debug APK SHA256:
  `007542F5269E7DB825E245097CD56CE392427E3B8629CA91C4AE0D90DFF6E740`.
- Final unsigned Release APK SHA256:
  `CB32FC60013C69733F00BBA6570DE0A15B032B45A6C16C41343D98E274E0B4A6`.
- MuMu `127.0.0.1:16384`, using `adb reverse tcp:7890 tcp:7890`, showed live
  discussion/announcement cards, real avatars and badges, raw tags, positive
  `R18` search, detail opening, and filtered-state restoration on Android Back.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn47-forum-source-parity\forum-final-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn47-forum-source-parity\forum-announcement-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn47-forum-source-parity\forum-search-r18-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn47-forum-source-parity\forum-detail-live.png`

## Turn 46 native authentication and live rendering QA

This slice finished the native authentication surface that was already wired
into the API and ViewModel layers, then re-ran the live image/tag regression on
MuMu.

Native changes:

- Added `AuthScreen` for source-equivalent password login, verification-code
  login, three-step registration, and password reset forms.
- Added a source-origin CAPTCHA bridge that forwards only a short-lived
  Turnstile/reCAPTCHA/hCaptcha response to the pending native request.
- Connected `AppRoute.Auth` and `AppRoute.AuthCaptcha` to the Compose route
  host, including safe Back handling for the pending action.
- Reworded a successful empty chapter response as "source currently provides no
  readable chapters". Read-only source evidence for book `360209` was a
  successful empty chapter array, not a network failure.

Verification:

- Full Release unit-test XML report: 55 suites, 283 tests, 0 failures, 0
  errors.
- `:app:assembleDebug` and `:app:assembleRelease` both completed successfully.
- Final Debug APK SHA256:
  `0F5A6F7D4401FF7EB4522984242BD72FA2FC64652688FD06074F46CCDE7A1706`.
- Final Release unsigned APK SHA256:
  `790BD41D01AD68341BE2FC7F7D00BD40B0490172CB7A0AC77D826135D3558B4D`.
- MuMu `127.0.0.1:16384` was kept on the existing data set and used
  `adb reverse tcp:7890 tcp:7890` for the host Clash listener.
- `novalpie://app/search?q=%E5%A5%87%E5%B9%BB` returned 20 live results with
  original clear covers, source/status/tag chips, and site statistics.
- A live 365-chapter source book opened its native detail page and reader; the
  reader toolbar appeared on tap and system Back returned to the same detail
  page with its recorded reading position.
- The native `/login` deep link rendered the expected native form. No real
  login, registration, reset, upload, administrator, payment, or forum action
  was submitted during QA.

Runtime evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn46-auth-network\final-search-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn46-auth-network\book-with-chapters-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn46-auth-network\reader-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn46-auth-network\reader-toolbar-live.png`
- `D:\NovalPie\native-android\qa-screenshots\turn46-auth-network\native-login-live.png`

## Turn 45 source-route and political-exam native parity

Current source inspection used a headed Playwright browser against
`https://novalpie.cc/political-exam`. The landing page presents the following
server-owned contract before a session is created:

- 40 single-choice questions worth 1 point each;
- 10 multiple-choice questions worth 2 points each;
- 25 true/false and 25 fill-blank questions worth 1 point each;
- 100 points total, 30 minute time limit, and an 80-point pass threshold;
- randomized question/option order;
- all-or-nothing scoring for multiple-choice questions;
- navigation/reload may lose progress; maximum three attempts per day.

Source visual evidence:

- `D:\NovalPie\native-android\output\playwright\source-political-exam-20260809.png`.

Native changes:

- `PoliticalExamPresentation` now mirrors all seven current website rules,
  source title/subtitle, and the `100 points / 30 minutes / 80 pass / 3 daily`
  fact strip.
- The native landing view uses a website-style pale-blue bordered rules callout
  instead of a generic Material card. It keeps native login state, confirmation,
  timer, answer, submit, and result handling intact.
- `nativeWebsiteRoute(path, isAdmin)` is a pure, unit-tested mapping layer for
  `/`, `/favorites`, `/search`, `/forum`, `/forum/:id`, `/forum/create`,
  `/user`, `/user/:id`, `/messages`, `/workspace`, `/upload`,
  `/upload-editor`, `/political-exam`, `/book-detail/:id`, `/book/:bookId`,
  `/book/:bookId/:chapterId`, and all three `/book-edit/*/:id` routes.
- The mapper also accepts legacy `/posts/:id` links. Unknown normal website
  pages deliberately use the authenticated WebView fallback; unknown and all
  non-admin `/admin*` paths never expose an administrator route.
- The Android manifest now registers `https://novalpie.cc` as a BROWSABLE VIEW
  handler in addition to `novalpie://app`, so the system can offer native
  handling for source website links.
- A managed-book deep link verifies the source per-book permission endpoint
  before it places an edit, chapter-management, or append route on the stack.

Verification:

- Unit XML report: 54 suites, 279 tests, 0 failures, 0 errors.
- `:app:assembleDebug`: passed.
- `:app:assembleRelease`: passed, including Release lint-vital.
- Debug APK SHA256:
  `C1EC45BFA7AE2AEBD8E3D5097A12FC3ED29037C3F2459777DF6BC7C6D4E6F34E`.
- Release unsigned APK SHA256:
  `2764E4EAFDBA5057B9C96F774BFF90C650CB64CCF5B369DD5FC7953DC7512CC6`.
- MuMu `127.0.0.1:16384` opened
  `novalpie://app/political-exam` into the native exam landing page and
  `novalpie://app/search?q=%E5%A5%87%E5%B9%BB` into 20 live search results.
- Installed-package inspection confirmed the HTTPS intent filter for
  `novalpie.cc`.

Runtime screenshots:

- `D:\NovalPie\native-android\qa-screenshots\turn45-political-exam\native-deeplink-political.png`.
- `D:\NovalPie\native-android\qa-screenshots\turn45-political-exam\custom-search.png`.

No live exam session, answer submission, administrator mutation, upload, or
payment action was performed in this turn.

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

## Turn 51 - Source upload-editor advanced workflow parity

Source route and native route:

- Website editor: `/upload-editor`.
- Native route: `AppRoute.UploadEditor`.

Implemented source-compatible editor behavior:

- Added the processor contract used by the source editor: `POST` JSON
  `{"text":"..."}` to the user-selected processor endpoint. The response accepts
  `{"text":"..."}`, `{"data":"..."}`, or a JSON string. The request deliberately
  omits NovalPie session headers and tokens.
- Added the three source batch split modes: paragraph count, character count,
  and an even split that preserves whole paragraphs.
- Added cursor-driven manual tools: insert a complete chapter pair, delete the
  chapter at the cursor, renumber markers, validate markers, and clear markers.
- Source marker output is retained exactly as
  `##__T[00001]__##` and `##__C[00001]__##`; generated chapter output is written
  back into that form before EPUB/export workflows consume it.

Verification on 2026-08-09:

- Debug and Release unit reports: 55 suites / 301 tests / 0 failures / 0 errors
  / 0 skipped for each variant.
- `testDebugUnitTest testReleaseUnitTest assembleDebug assembleRelease` completed
  successfully with the isolated offline Gradle cache.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA256
  `AA22794E8A8216DD1A179C81C9C5293C75E0F8304DFF5E511C191BC9EC409F7E`.
- Unsigned Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`,
  SHA256 `32D7ECD2976E89EAFD353A4A6DF0EB9580730C8CDBBF11F754A7811FD61B99EC`.
- MuMu `127.0.0.1:16384` verified text editing, processor UI, split-tool entry,
  manual-tool entry, and a blade insertion producing a valid source marker pair.
  The temporary verification text was cleared afterwards; the final editor state
  is 0 chapters and 0 characters.
- Runtime evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn51-upload-editor`.

No live upload, administrator mutation, payment, account, or external processor
request was submitted during this verification.

## Turn 52 - Upload-editor history and network/image/tag runtime regression

Source-editor parity completed in this slice:

- The native editor now keeps bounded undo/redo history for text input, batch
  splitting, processor/script output, marker cleanup/renumbering, find/replace,
  chapter edits, and restored archives. The history is limited to 64 entries and
  one million total characters so a large EPUB does not retain unbounded string
  copies in memory.
- The source-style toolbar now exposes `切`, `T`, `C`, clear identifiers,
  renumber, undo, and redo. Single `T`/`C` insertion advances the cursor so a
  consecutive pair uses the same chapter id.
- The source editor was inspected read-only at `/upload-editor`; its separate
  information, tools, and file panels plus its `.txt`, `.md`, and `.zip` file
  affordances remain the reference for the next file-browser/batch-import slice.

Regression verification on 2026-08-09:

- `EditorProcessorTest` and `EditorDocumentHistoryTest` passed before the full
  suite. Final Debug and Release reports each contain 56 suites / 304 tests /
  0 failures / 0 errors / 0 skipped.
- `:app:assembleDebug` and `:app:assembleRelease` completed successfully.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA256
  `E655804A37604A5D01C34B778023ADEEDE9832A067A6A5CC44DAD300CB9B93C5`.
- Unsigned Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`,
  SHA256 `869D252BB02CBACBE2CF43C4D75FBD4C627A0367B96D3A59C7CA1464998E4ACF`.
- MuMu `127.0.0.1:16384` retained its application data during `adb install -r`.
  With `adb reverse tcp:7890 tcp:7890` active, live Discover loaded hot tags and
  a 20-result `奇幻` query with sharp source covers, source/status/tag pills, and
  favourite/read/word metrics. The authenticated Collection loaded 20 saved
  books with the same original-cover and metadata chain.
- Runtime evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn52-editor-history` and
  `D:\NovalPie\native-android\qa-screenshots\turn52-final-verification`.

No live upload, editor processing request, account mutation, administrator
mutation, payment, or external processor request was submitted.

## Turn 53 - Upload-editor file browser and ZIP batch import

Read-only mobile-source audit at `/upload-editor` established the current file
panel contract:

- The page exposes the `信息 / 工具 / 文件` panels beside the chapter directory
  and source-marker toolbar.
- `文件` contains `文件浏览器`, `上传文件`, `刷新`, a file-search field, and a
  drop/select target that explicitly advertises `.txt`, `.md`, and `.zip`.
- The original mobile layout is desktop-like and narrow, so native uses the
  platform document picker rather than attempting to reproduce browser drag and
  drop on a touch screen. Source visual evidence:
  `D:\NovalPie\native-android\output\playwright\source-upload-editor-file-panel-turn53.png`.

Native implementation:

- Added the `文件` editor tab with a searchable local file queue, multi-document
  system picker, individual `打开`, remove, and explicit `批量导入` actions.
- Batch imports accept `.txt`, `.md`, `.markdown`, `.epub`, and `.zip`. Text
  files and supported ZIP entries become ordered native chapters, then emit the
  source `##__T[...]__##` / `##__C[...]__##` document form.
- ZIP content is streamed locally only. It limits the archive to 500 entries,
  10 MiB per extracted text entry, 50 MiB total extracted bytes, and 50 million
  total characters; no file or editor content is sent to NovalPie.
- Existing single-document behavior is retained for normal text and EPUB opens.

Verification on 2026-08-09:

- New `EditorBatchImporterTest` covers supported-entry ordering, unsupported
  entry filtering, oversized-entry rejection, and file-type detection.
- Full Debug and Release reports each passed 57 suites / 307 tests / 0 failures
  / 0 errors / 0 skipped. Debug and Release packages both built successfully.
- Debug APK SHA256:
  `6F7A055A0D974E81BB9F1B20A1A709FA593FDE0CEED7D78BB103FD9229FCFA80`.
- Unsigned Release APK SHA256:
  `EA7A0297FBDEBCBBB39C554F747A975F87F9C98F475C1A1F682C776915C563DE`.
- MuMu `127.0.0.1:16384` selected a local two-text-entry ZIP through Android's
  document picker, queued it in the native file panel, imported it as two
  chapters, and showed both source marker pairs in the text editor. The QA
  document was cleared afterwards and the temporary emulator ZIP was removed.
- Runtime evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn53-editor-file-browser-empty.png`,
  `turn53-editor-file-picker-selected.png`, `turn53-editor-batch-imported.png`,
  `turn53-editor-batch-markers.png`, and `turn53-editor-batch-cleared.png` in
  the same directory.

No live upload, account mutation, administrator mutation, payment, or external
processor request was submitted.

## Turn 54 - Search-grid performance and card accessibility

MuMu read-only runtime QA on a `900x1600` Android 15 instance focused on the
previously reported image-heavy search scroll:

- Live `奇幻` search loaded 20 source results with original covers, tags, and
  compact facts. A first-load plus scrolling sample rendered 1,133 frames with
  3.35% jank (p95 24 ms, p99 117 ms) and zero slow bitmap uploads.
- After the image cache was warm, 20 alternating full-card scrolls rendered 807
  frames with 3.10% jank (p95 20 ms, p99 81 ms), three missed vsyncs, and zero
  slow bitmap uploads. The flow did not reproduce a frozen list or a blank card.
- Android's accessibility hierarchy showed that each card's outer click target
  was focusable but unlabeled. `NovelCardItem` now supplies a card-level spoken
  summary with open action, title, author, source, content tags, and the same
  compact metrics visible on screen. The cover preview keeps its separate child
  action, so this does not change tap or long-press behavior.
- Reinstalled Debug and verified the real accessibility XML contains labels such
  as `打开 成为暗黑奇幻里的猎人，作者 …，来源 上传，标签 奇幻，本站收藏 …`.

Verification on 2026-08-09:

- Full Debug and Release reports each passed 57 suites / 308 tests / 0 failures
  / 0 errors / 0 skipped. Debug and Release packages built successfully.
- Debug APK SHA256:
  `E36F87869C8354E75E0BEF3B9A4B6912E89E4872D6378E464C98A51AC734FE38`.
- Unsigned Release APK SHA256:
  `F591055811E121D9D97B40670D3B16EE8F0A604EE1E512BA14E780A913358050`.
- Evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn54-search-perf-start.png`,
  `turn54-search-perf-grid.png`, `turn54-search-perf-scrolled.png`,
  `turn54-search-accessibility.xml`, and `turn54-search-accessibility-after.xml`
  in the same directory.

No live account, administrator, upload, payment, or other source mutation was
submitted.

## Turn 55 - Administrator shop asset preview parity

Read-only inspection of the live `/admin/shop` data and MuMu UI established the
following asset contract:

- `GET /api/admin/shop/items` returns `image_url` for `frame` items and
  `badge_html` / `badge_css` for `badge` items. A live `透明龙PROMAX` badge, for
  example, contains cyan-to-purple CSS color stops but no bitmap asset.
- Native shop cards now render frame URLs as original images and render badges
  as safe Compose gradients. The preview reads only non-neutral `rgb` / `rgba`
  color tokens and plain text from server fields; it never executes server CSS
  or HTML.
- The shop editor now has an Android `OpenDocument` image picker for a local,
  ephemeral frame draft preview. `content:`, `file:`, `android.resource:`, and
  `data:` values are rejected from the persisted image URL. A local-only draft
  cannot be saved until a remote image URL is supplied.
- Saving Cookie, BaseURL-rule, and shop edits now requires a second explicit
  confirmation. The shop confirmation states that a local draft preview is not
  uploaded and only the remote URL or badge fields would be sent.

Verification on 2026-08-09:

- `AdminPresentationTest` covers safe badge text/color extraction and verifies
  that local Android URI schemes never enter a shop save payload.
- Full Debug and Release reports each passed 57 suites / 310 tests / 0 failures
  / 0 errors / 0 skipped. Debug and unsigned Release packages built successfully.
- Debug APK SHA256:
  `E89B70A36238E7A3DA9792BAC07B5B60F82D32BB23B887C81F7B8D0ABF5A3ED5`.
- Unsigned Release APK SHA256:
  `85B41D4693806BC7861FB506844E195653E5F8AAD02BC6FD4A29A1E940BD03C1`.
- MuMu Android 15 verified the native badge gradient cards, original frame
  images, the folder-based document picker, selected local-only frame preview,
  and the final save confirmation. The confirmation was cancelled; no live
  administrator request was submitted. Temporary device test images were removed.
- Evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn55-admin-shop-badge-preview.png`,
  `turn55-admin-shop-frame-grid.png`, `turn55-admin-shop-document-picker.png`,
  `turn55-admin-shop-local-preview.png`, and
  `turn55-admin-shop-save-confirmation.png`.

## Turn 56 - Workspace API activation parity

Current live-source audit used the public route map in `CxFG0gqQ.js` and the
current workspace page chunk `dSlFh-Ca.js`. The source contract has an API
activation action that the previous native screen omitted:

- `GET /workspace/apis` returns the existing API fields plus source status
  fields such as `status`, `actualStatus`, and `callCount`.
- `POST /workspace/apis/{id}/toggle` changes a shared API between active and
  inactive. The current source sends a zero-byte POST body.
- Native now normalizes the activation/actual status separately from health,
  presents readable state pills and a `启用` / `停用` action, and requires a
  confirmation before the server action. A failed action result now becomes a
  visible workspace error instead of a false success notice.

Verification on 2026-08-09:

- `WorkspaceApiTest` verifies source-shaped active and inactive API payloads,
  including `status`-only inactive fallback. It also proves the action is a
  zero-byte `POST /workspace/apis/9/toggle` request.
- `WorkspacePresentationTest` covers source status labels and the corresponding
  Chinese enable/disable action labels.
- MuMu Android 15 loaded the authenticated native workspace overview and API
  tab without crash or stale navigation. The current account returned zero local
  and zero shared API configurations, so no live toggle target existed and no
  mutation was attempted.
- Full Debug and Release reports each passed 57 suites / 311 tests / 0 failures
  / 0 errors / 0 skipped. Debug and unsigned Release packages built successfully.
- Debug APK SHA256:
  `334B2D4BE62A4CA5129CA8C6379C514021DD59C1CCCB926DB89F0621D98A9CE3`.
- Unsigned Release APK SHA256:
  `6E407EF0D600A8EEC73D92E4986174A309059D69098CE78F03E4B71D7000BF84`.
- Evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn56-workspace-final-overview.png`
  and `turn56-workspace-api-empty.png`.

No live workspace, administrator, upload, account, or payment mutation was
submitted.

## Turn 57 - Reader typography and illustration-preview parity

Current source-reader inspection of the `/book/:id/:chapterId` route chunk
`8L_S-edK.js` established the defaults used by the website reader:

- `fontSize: 16`
- `lineHeight: 1.6`
- `textIndent: true`
- `emptyLine: true`
- an 800px desktop content-width cap, with the mobile view flowing at the
  available reading width.

Native reader implementation now follows that presentation without overriding
reader preferences that users already saved:

- `ReaderSettingsStore` uses 16sp only when `font_size_sp` is absent. Existing
  saved sizes, including an explicit 18sp setting, continue to load unchanged.
- `ReaderTextLayout` derives source-style 1.6 line-height, 2em first-line
  indent, and 8dp paragraph rhythm from the selected font size.
- The text canvas and illustration surfaces no longer use elevated rounded
  cards. The top and bottom overlay toolbars use the same opaque reader paper
  with zero tonal elevation, so they mask content cleanly without looking like
  a second product chrome.

Runtime verification on 2026-08-09:

- MuMu Android 15 (`127.0.0.1:16384`) installed the Debug APK via `adb install
  -r`, retaining existing application data and proxy reverse mapping.
- A live 143-chapter text reader verified the hidden/visible toolbar toggle and
  Android Back returned from the reader to its own book detail route.
- Public image collection `352787`, non-restricted chapter `5899549`, returned
  `imageCount: 2`. Both inline originals loaded sharply; tapping an image opened
  the full-screen zoom/pan preview, and Android Back closed only the preview and
  returned to the same inline image.
- A fresh live `奇幻` search loaded all 20 results with original-resolution
  covers, source/status pills, full content tags, and favourite/read/word facts.
- `ReaderSettingsStoreTest`, `ReaderPresentationTest`, and `ReaderTextTest`
  cover source defaults, saved-size preservation, typography bounds, and image
  parsing. Full Debug and Release reports each passed 58 suites / 315 tests /
  0 failures / 0 errors / 0 skipped.
- Debug APK SHA256:
  `34BD5276D9443DD3E2F32A3454961AB31C8F4AE10766A83B8CA83C4C210D277A`.
- Unsigned Release APK SHA256:
  `30E413621DF662939C0918A6B4A8D015E55B1EC4823E505A4508A7938FE97B6D`.
- Evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn57-reader-layout-source-aligned.png`,
  `turn57-reader-toolbar-source-aligned.png`, `turn57-reader-back-detail.png`,
  `turn57-reader-illustration-inline.png`, `turn57-reader-image-preview.png`,
  `turn57-reader-preview-back.png`, and `turn57-search-network-cover-tags.png`.

No live upload, account, administrator, workspace, payment, or content mutation
was submitted.

## Turn 58 mobile search tag-filter parity

Read-only Playwright audit of the current mobile `/search` route confirmed the
following source contract:

- Default mobile search uses `GET /api/search` with `scope=all`,
  `match_type=fuzzy_strict`, `sort_by=relevance`, `sort_order=desc`, and
  `adult_filter=unrestricted`.
- `GET /api/tags?sort=count&limit=1000&offset=<offset>` is the source tag
  catalogue endpoint. The website eagerly pages all tags, but the native client
  deliberately shows an initial popular subset and accepts arbitrary manual tag
  input so it does not block a phone on an 18k-tag download.
- Source advanced query `tag:奇幻 NOT tag:后宫` becomes
  `GET /api/search?tags=奇幻&blocked_tags=后宫...`; its response echoes
  `search_params.tags` and `search_params.blocked_tags`. The native UI does not
  advertise the rest of the source advanced grammar until it has a complete
  parser; the verified tag semantics are implemented directly.

Native implementation:

- `SearchOptions` and `PersistedSearchSettings` now retain mutually exclusive
  `requiredTags` and `blockedTags`. New defaults match the live source's
  `fuzzy_strict` / `unrestricted` behavior without overwriting an existing
  explicit user preference.
- `NovalPieApi.search()` serializes those selections as source-compatible
  `tags` and `blocked_tags` parameters, omitting blank `q` and `source`
  parameters instead of sending fake empty values.
- Discover now presents `规则 / 标签 / 字数` before results. The tag panel has
  `包含` / `屏蔽`, removable active chips, comma-separated manual entry, 24
  initial hot tags, and an explicit `显示更多热门标签` action. A tag change
  immediately triggers the real search request.

Verification:

- Focused API/store/presentation/tag-filter tests passed.
- Full Debug and Release validation completed with 59 suites / 319 tests / 0
  failures / 0 errors / 0 skipped per variant using:

  ```powershell
  .\gradlew.bat --offline --no-daemon --max-workers=1 --no-watch-fs `
    :app:testDebugUnitTest :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease `
    --console=plain
  ```

- MuMu (`127.0.0.1:16384`) retained its app data through `adb install -r` and
  used the existing `adb reverse tcp:7890 tcp:7890` route. Live QA selected
  `奇幻`, switched to `屏蔽`, selected `后宫`, restarted the app to confirm
  persistence, then reran the live query. It returned 20 sharp-cover results
  with source/status/tag/fact cards.
- Debug APK SHA256:
  `A595518AD71505E09A108D4091B897DCEB4E62BE45D7FFE418663107006ED04A`.
- Unsigned Release APK SHA256:
  `2EB16F3462107F6F9D30F9EB2FE836B5463185E2431A28AEAF79E22D013ADB6E`.
- Evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn58-search-tag-filter-panel.png`,
  `turn58-search-required-blocked-results.png`,
  `turn58-search-persisted-tag-filters.png`,
  `turn58-search-tag-results-grid.png`, and
  `turn58-search-tag-results-grid-facts.png`.

No live account, upload, administrator, workspace, payment, or content mutation
was submitted. The temporary `奇幻` / `后宫` test tags were cleared through the
native UI after evidence capture; login, bookshelf, and reading data remained
untouched.

## Turn 59 mobile advanced-search syntax parity

The mobile source search was re-audited with a read-only browser session. The
following inputs were observed to become the listed `GET /api/search` fields:

| Source input | API fields |
| --- | --- |
| `tag:奇幻 NOT tag:后宫` | `tags=奇幻`, `blocked_tags=后宫` |
| `@title:魔法 学院 NOT 续作` | `q=魔法 学院`, `scope=title`, `blocked_terms=续作` |
| `in:author 叶轻灵 NOT tag:虐心` | `q=叶轻灵`, `scope=author`, `blocked_tags=虐心` |
| `in:tags 猎人 NOT 续作` | `q=猎人`, `scope=tags`, `blocked_terms=续作` |
| `word:10w..50w` | `min_word_count=100000`, `max_word_count=500000` |
| `platform:novelPia type:玄幻 status:连载 match:loose` | `platform=novelPia`, `type=玄幻`, `status=连载`, `match_type=fuzzy_loose` |

Native implementation now provides an opt-in, persisted `高级语法` mode:

- `AdvancedSearchSyntax.kt` parses the observed directives and reports malformed
  syntax locally before any request. It supports title/author/tag scope, required
  and blocked tags, excluded terms, word ranges, platform/type/status, match
  mode, `#标签`, and source-compatible tag alternatives.
- `NovalPieApi.search()` now serializes `blocked_terms`, `platform`, `type`,
  `status`, `tags_any`, and `tags_expr` in addition to the previously verified
  basic tag parameters.
- When advanced mode is enabled, regular `规则 / 标签 / 字数` controls are
  hidden so no conflicting panel takes over the screen. The native guide is
  compact by default, supplies two tap-to-fill source examples, and expands for
  the full reference only when requested.
- Editing a search field now invalidates any prior result grid immediately;
  newly opened search screens start with an empty field and leave historical
  keywords as explicit choices instead of silently restoring the last query.
- `AdvancedSearchSyntaxTest`, API parameter assertions, store persistence, and
  Discover section-order coverage protect parser, transport, and presentation
  behavior. `adult:only` is recognized from the source help syntax; anonymous
  source traffic did not expose adult-only results, so this claim was not made
  from anonymous runtime data.

Runtime verification on 2026-08-09:

- MuMu Android 15 at `127.0.0.1:16384` retained app data through
  `adb install -r`; host proxy port `7890` and `adb reverse tcp:7890 tcp:7890`
  were live.
- A clean app restart retained the advanced-mode toggle. The live source query
  `tag:奇幻 NOT tag:后宫` returned 20 results. Result cards showed original
  sharp covers, source/status pills, full tags, favourite count, site reads,
  and word count.
- Debug and Release reports each passed 61 suites / 327 tests / 0 failures /
  0 errors / 0 skipped. Both APK variants were rebuilt.
- Debug APK SHA256:
  `7228916BA62A604AB25A5505429E9DF3B088864AA68C05C29FEE373725C0E7C6`.
- Unsigned Release APK SHA256:
  `E4C29CC1E63378AF94CCB4780A514037AFC3CD89A889F202C5EABC459A2B6A25`.
- Evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn59-final-advanced.png`,
  `turn59-final-advanced-results-grid.png`, and
  `turn59-final-results-facts.png`, plus the final clean-state capture
  `turn59-final-clean-search.png`.

No live account, upload, administrator, workspace, payment, or content mutation
was submitted.

## Turn 82 runtime review, cover priority, and Collection folder hierarchy

MuMu Android 15 was available at `127.0.0.1:16384` with
`adb reverse tcp:7890 tcp:7890`. Debug installs used `adb install -r`; no app
data, authenticated app state, or browser state was cleared.

Native runtime findings and changes:

- Book `354491` now uses a compact book-context header on the Reviews tab. Its
  two live source reviews, reaction counters, award and reply affordances are
  visible in the first viewport rather than below a second full detail hero.
- Cold-cache search instrumentation showed the search response and card metadata
  before 700 ms, while cover work was still pending. Search no longer starts the
  four-card speculative preload before a user has actually scrolled past the
  search header, so visible covers keep the proxy/CDN connection priority.
  On the fresh post-change cold-cache capture, the two visible original covers
  were complete at 2.0 s and remained sharp; no lower-resolution fallback was
  introduced.
- The default Collection folder was previously a short tile in the same grid row
  as tall 2:3 covers. Folder cards now span their own compact horizontal row
  with a folder icon and an explicit open action. A missing source `count` is
  rendered as `收藏分组`, never as a false zero; the open action was exercised
  against the live default group.

Verification:

- `SearchCoverPrefetchTest`: 3 passing tests.
- `LibraryPresentationTest`: passed, including empty/unknown folder presentation
  and source-count subtitle coverage.
- Debug package: `BUILD SUCCESSFUL`; final Debug SHA256:
  `A0F302D310575CF3907AD5092C1B9D420753DFA308CBEF41E4A3C08E5D4B5007`.
- Runtime evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn82-book-reviews.png`,
  `turn82-search-q-no-initial-prefetch-700ms.png`,
  `turn82-search-q-no-initial-prefetch-2s.png`, and
  `turn82-collection-folder-subtitle-final.png`.

No live account, upload, administrator, workspace, payment, or content mutation
was submitted.

## Turn 80-81 search performance and compact mobile parity

The source search control contract captured from the mobile page keeps all content filters visible:
`scope`, `adult_filter`, `source`, `match_type`, `sort_by`, `sort_order`, tag filters and word-count
filters. A later live mobile audit on 2026-08-17 supersedes the earlier default-value observation:
the source initial request uses `adult_filter=unrestricted`.

Native changes:

- Fresh and cleared native settings default to `unrestricted`; any previously stored `all`,
  `adult_only`, or `unrestricted` selection remains the user's choice.
- The basic filter page is now three compact two-column rows: range/content, source/match mode,
  and sort/direction. Tag and word-count pages remain separate source-style tabs, so no transport
  option was dropped.
- Search pagination retains the source five-page window plus previous/next navigation. It now uses
  a single 32dp pager rail; tapping `共 N 页` opens validated direct page jump instead of reserving
  a second full-height input row above the first books.
- Search cover warmup now has exactly one UI-owned, cancelable batch. It waits for visible covers,
  fetches at most four look-ahead thumbnails, and disposes unfinished requests when the scroll
  target changes. The former ViewModel warmup was removed because it duplicated UI requests and
  could make visible covers contend with up to sixteen speculative image fetches.

Verification on 2026-08-12:

- `SearchSettingsStoreTest`, `DiscoverPresentationTest`, and `SearchCoverPrefetchTest` passed.
- `:app:compileDebugKotlin` passed as part of each test run; no new errors were reported.
- Debug package prior to the final compact-pager source change passed at
  `build-agent-logs/turn81-search-ui-package-20260812-014307.out.log`; the current source passed
  the follow-up compile/test log at
  `build-agent-logs/turn81-compact-pager-20260812-014717.out.log`.
- MuMu's `127.0.0.1:16384` endpoint refused the ADB connection during this pass. No emulator data,
  browser session, login state, or live source data was changed; fresh runtime screenshots remain
  queued for the next emulator-online pass.

## Turn 68 search grid/list presentation and emulator runtime QA

The mobile source search view stores its presentation preference locally and switches
between a cover grid and a metadata-rich list without submitting another search request.

Native implementation:

- `PersistedSearchSettings.viewMode` now saves only `grid` or `list`; invalid stored values
  safely fall back to `grid`.
- `SearchViewMode` and `toggleSearchViewMode()` preserve the existing result session while
  updating the local preference.
- The list presentation keeps the same original cover action, cover badges, source pill,
  untruncated content tag rail, and favorite/read/word metrics as the grid.
- Regression coverage now persists `list` through the store and a recreated ViewModel.
- The debug-only launcher label is `NovalPie 2.0 Debug`, making the current test package
  distinguishable from an older installed release package.

Runtime verification on 2026-08-10:

- MuMu Android 15 was started, exposed at `127.0.0.1:16384`, and received
  `adb reverse tcp:7890 tcp:7890`; the host proxy was listening on port 7890.
- The current Debug APK was installed with `adb install -r`, preserving application data.
  A live search returned 60 results and 19,457 total works. Original-resolution covers,
  source/status badges, tags, and compact metrics rendered in both grid and list modes.
- A multi-tag result visibly rendered every returned tag across multiple rows in list mode.
  After force-stop and relaunch, the UI automation tree reported `switch to grid view`, proving
  that the selected list presentation persisted.
- Evidence: `D:\NovalPie\native-android\qa-screenshots\turn68-home-network-result.png`,
  `D:\NovalPie\native-android\qa-screenshots\turn68-search-grid-cards.png`,
  `D:\NovalPie\native-android\qa-screenshots\turn68-search-list-cards.png`, and
  `D:\NovalPie\native-android\qa-screenshots\turn68-search-list-persist-results.xml`.
- Debug and Release reports each passed 66 suites / 351 tests / 0 failures / 0 errors / 0 skipped.
  Debug APK SHA256: `8AE62E7071A0C71C54EE8838A91AF858265A27613357CC7D7E8BDBFE6A5D5497`.
  Unsigned Release APK SHA256: `C85146D86A7A7F6458625027AA3C5EF0D1B043628725ADD6137884B26A3F96F6`.

No live account, upload, administrator, workspace, payment, or content mutation was submitted.

## Favourites cache-policy parity (Turn 65, 2026-08-10)

Read-only mobile-source bundle audit found that `favorites_cache_mode` is unrelated to image
caching and has no API request. Its three local states are:

| Source value | Source behavior | Native behavior |
| --- | --- | --- |
| `none` | Do not restore favourites UI state. | Clears every cached Collection presentation value, while retaining the policy itself. |
| `no-search` | Restore view, group, tab, page, and sort; leave the search field blank. | Persists tab/layout/display group/current page/sort, never persists the Collection query. |
| `all` | Restore all supported favourites UI state. | Persists the complete native equivalent, including query text. |

The source clear-cache action resets query, grid/list mode, display selection, active tab, page,
and `created_at desc` defaults without changing `favorites_cache_mode`. Native now has the same
separation: Collection's policy-cycle action, clear-Collection-cache action, and actual Coil
memory/disk image-cache clear action are three distinct local controls.

Native data/rendering safeguards retained during this slice:

- `NovalPieApi.normalizeBook()` keeps complete source tag aliases and prefers source full-cover
  aliases where present.
- `NovelCardItem` renders all normalized content tags rather than a fixed subset.
- `BookCover` prefers `fullCoverUrl` and requests `1024x1536` with `Precision.EXACT`; it does not
  intentionally compress the source cover for storage.
- API and Coil image traffic both use the source proxy policy, with emulator fallbacks ordered as
  `127.0.0.1:7890`, `10.0.2.2:7890`, then direct.

Verification on 2026-08-10:

- `FavoritesSettingsStoreTest` covers all/no-search/no-cache persistence and clear behavior.
- Debug and Release unit-test reports each contain 347 tests with 0 failures, 0 errors, and 0
  skipped.
- Debug APK SHA256:
  `BE0BD1DB49CAF07C9EA225D03579D876D12671CDB53533B8DC237CBD96AD1664`.
- Unsigned Release APK SHA256:
  `F81B7CAD1493EFEFD0CCC9BD27CCD001906A3C8766BCF6FB891A6A4DF944AAFF`.
- MuMu had no exposed Android ADB endpoint during this verification. The host proxy listener was
  present, but no current-device screenshot or install claim is made. Existing live visual evidence
  for sharp original covers and full tag rows is
  `D:\NovalPie\native-android\qa-screenshots\turn59-final-results-facts.png`.

## Turn 62 native favourites and reading-history presentation completion

The previously implemented favourites API envelope was connected to the native Collection screen
instead of being reduced to a list of books. The screen now preserves the source record metadata
needed for group, pin, history, and batch-action flows.

Native changes:

- Collection can switch between the source-compatible favourites and reading-history tabs, adaptive
  cover grid and full-width list rows, persisted across app restarts.
- Default favourites layout shows group folder cards plus unclassified works; All expands the current
  page; Unclassified filters locally; entering a group uses the source group-items request.
- Book cards retain original covers, source/status/tag pills, compact favourite/read/word facts, and
  an explicit selection marker. Selection mode turns card taps into selection instead of navigation.
- Group selection, sorting, search forwarding, load-more, move/remove/history controls, and pin
  actions are wired to the existing view-model and source API contracts. Destructive history and
  removal actions remain confirmation-gated in the UI.
- The Collection cache action now clears the actual global Coil memory and disk image caches rather
  than claiming a nonexistent server cache. Refresh reloads local Collection data through the normal
  source request path.
- Added persisted-settings and collection-presentation regression coverage, including the default,
  all, unclassified, and opened-group filtering rules.

Verification on 2026-08-10:

- Debug and Release unit-test reports each passed 334 tests with 0 failures, 0 errors, and 0 skipped.
- Debug and Release APK packaging completed. Debug SHA256:
  `AA8A83900BEEBEA2FD154799F7104114CFD77A1F7A49F67885F0C53FBADA7650`.
- Unsigned Release SHA256:
  `4A742528721D14CF2331B769982A55E008700FF89E797ADA85A1CC8450C8C34D`.
- The APK archives contain the expected Android metadata, DEX, baseline profile, and native ABI
  entries. MuMu Android endpoints were unavailable at package time, so no new runtime screenshot is
  claimed for this turn.

No live account, upload, administrator, workspace, payment, favourite, history, or content mutation
was submitted during verification.

## Turn 63 Chinese variant / OpenCC display parity

Read-only mobile source inspection on 2026-08-10 established that the tools drawer's `原文模式`
control is a global display conversion feature:

- The source persists `chinese-variant` as `original`, `traditional`, or `simplified`, alongside
  `opencc-enabled` and a default `s2t.json` OpenCC config.
- Clicking the drawer control moved the isolated browser from original source text to Traditional
  Chinese across collection labels, placeholders, cards, navigation, and the drawer itself. It did
  not issue a server mutation.
- The current source boot chunk applies OpenCC to ordinary text nodes plus title/placeholder/alt/
  aria-label attributes, observes later DOM additions, and skips `SCRIPT`, `STYLE`, `NOSCRIPT`,
  `CODE`, and `PRE` nodes.

Native changes:

- Added local `ChineseVariantSettingsStore` with the exact source state names and default original
  mode; the setting never leaves the device.
- Added Tools quick-cycle and Settings explicit mode controls for original, Traditional, and
  Simplified. The quick label mirrors the source's current-state label.
- Added a global Compose text presentation layer. Ordinary app UI and source strings transform via
  Android ICU's Simplified-Traditional / Traditional-Simplified transforms, while styled annotated
  text intentionally stays intact to preserve spans and clickable source links.
- Android 6 fallback is conservative and local-only; Android 7+ uses ICU. The API payload and
  stored model values remain raw source values, so changing a display mode cannot mutate remote
  content.

Verification:

- `ChineseVariantPresentationTest` verified the source three-state cycle, original no-op, and both
  directions of `阅读小说 <-> 閱讀小說` conversion under Android API 35 Robolectric.
- `ChineseVariantSettingsStoreTest` verified default and persisted mode behavior.
- Full Debug and Release reports each passed 340 tests with 0 failures, 0 errors, and 0 skipped.
- Fresh Debug APK SHA256:
  `0033B379211D66D4A675BCDA6C4EBDC582D9E12E2670C5BBBFEF56151ED3D61B`.
- Fresh unsigned Release APK SHA256:
  `9AA8A3BCEFD135591BB60B00CFA4C5AB8CF8F33DBA8B1358DB914DDB97101F8F`.
- Evidence:
  `D:\NovalPie\native-android\output\playwright\source-tools-traditional-variant-turn63.png`.

No live account, upload, administrator, workspace, payment, favourite, history, or content mutation
was submitted.

## Turn 64 source dark/light appearance parity

Read-only mobile source inspection on 2026-08-10 confirmed the remaining companion control in the
Tools drawer:

- Initial light appearance exposes the source action `深色`.
- Clicking it writes local `theme=dark`, switches the visible page and drawer to dark surfaces, and
  changes the action label to `浅色`.
- This is browser-local display preference behavior and did not issue an API request or mutate the
  source account. Source evidence:
  `D:\NovalPie\native-android\output\playwright\source-tools-dark-traditional-turn64.png`.

Native changes:

- Added local `AppThemeSettingsStore` with System, Light, and Dark preferences. System remains the
  fresh-install default; the Tools control maps the source quick toggle to explicit Light/Dark.
- Moved `NovalPieTheme` ownership inside `NovalPieApp`, after the ViewModel preference is available.
  This makes a user-selected theme update all Material surfaces, controls, status-bar icon tint, and
  navigation-bar icon tint in one recomposition.
- Added source-style quick appearance control in Tools and explicit three-option appearance control
  in Settings. Reader-local paper/sepia/dark preferences remain independent reading preferences.

Verification:

- `AppThemeSettingsStoreTest` covers default, Light, and Dark persistence; `AppThemePresentationTest`
  covers system resolution and the source opposite-action label.
- Full Debug and Release reports each passed 344 tests with 0 failures, 0 errors, and 0 skipped.
- Fresh Debug APK SHA256:
  `A9E2559A93A434035647A443050FA9BFB46DDC3F3050273315A460CBDFE5FAB6`.
- Fresh unsigned Release APK SHA256:
  `B0283B2CA1AE811045E8C2DB3335DDBB36974FC933F196E7FC38CE892FA246D0`.

MuMu's ADB endpoint was unavailable at package time, so no runtime install or screenshot is claimed.
No live account, upload, administrator, workspace, payment, favourite, history, or content mutation
was submitted.

## Turn 61 MuMu proxy detection and cover/tag recovery

The reported blank network state, fallback covers, and missing card tags were
traced to one shared prerequisite rather than three separate presentation
defects. A host-side read-only check showed that direct
`GET /api/search?limit=1` received HTTP 403, while the same request through the
local `127.0.0.1:7890` proxy received HTTP 200 with the source `tags` array and
the original `photo_url` asset.

Native changes:

- `isEmulatorRuntime()` now checks `ro.kernel.qemu`, `ro.boot.qemu`, the AVD
  property, and the qemud service in addition to the ordinary Build markers.
- The current MuMu image can spoof an OPPO profile. It is detected narrowly
  when that profile is paired with an x86/x86_64 ABI, so real ARM OPPO phones do
  not receive emulator-only proxy routing.
- Both API calls and Coil cover requests already use this one runtime decision,
  so source data, original cover URLs, and complete tags recover together once
  the automatic host-proxy route is selected.

Verification on 2026-08-10:

- `NetworkConfigStoreTest` (7), `NovalPieImageLoadingTest` (6),
  `NovalPieApiTest` (62), and `NovelCardFactsTest` (10) passed with zero
  failures or errors.
- Debug packaging passed. Debug APK SHA256:
  `26F0CCC094AD0A79E5BBD4BD856F5C9C4EF5C0EE06AC1584311A066CBE2A3926`.
- MuMu's former ADB endpoints `127.0.0.1:16384` and `127.0.0.1:5555` were not
  listening at package time, so this turn does not claim a fresh emulator
  screenshot or install result. No app data was cleared.

No live account, upload, administrator, workspace, payment, or content mutation
was submitted.

## Turn 66-67 mobile Collection hierarchy and search pagination parity

Read-only mobile-source audit on 2026-08-10 established two source contracts that guide the
native presentation:

- `/` Collection starts with its compact search field, six lightweight toolbar actions, and the
  favourites/history tabs. It does not use a product masthead or a large "continue reading" card.
- `/search` requests `GET /api/search?page=<n>&limit=60` and returns `results`, `total`, `page`,
  `limit`, and `total_pages`. The mobile page renders `全部小说 (共 N 部作品)`, a five-page window,
  previous/next actions, a page-number jump, and the same pager above and below the result cards.
  On the live default query the observed envelope was `total=47331`, `page=1`, `limit=60`, and
  `total_pages=789`; page 5 displayed the centred `3,4,5,6,7` window.

Native implementation:

- Collection was adjusted to the audited mobile hierarchy while retaining group, selection,
  history, UI-cache policy, and Coil-image-cache actions behind compact controls.
- `SearchPage` preserves the source response envelope. `NovalPieApi.search()` remains a
  list-only compatibility wrapper, while `searchPage()` normalizes source pagination metadata.
- Discover now requests the source page size of 60 and replaces the current grid when a source
  page is selected. It renders source-style total, top/bottom five-page controls, and validated
  page jump input. Request serial freshness checks continue to reject stale responses.
- Regression coverage includes live-envelope normalization plus first/middle page windows and
  jump bounds.

Verification on 2026-08-10:

- Read-only source screenshot: `D:\NovalPie\native-android\output\playwright\source-search-pagination-turn67.png`.
- Debug and Release unit tests each passed 66 suites / 350 tests / 0 failures / 0 errors / 0 skipped.
- Debug APK SHA256: `FFCD4366917DD19340421E1E11086B449B1683D0D6A3992706A04BA55EABC174`.
- Unsigned Release APK SHA256: `E8FCB63BA9645F63F4FD94938BC96C247D04A5BCA5D18E0CED8AFB3A16A2C950`.
- MuMu did not expose an ADB device during this turn. Host proxy port 7890 was listening; no
  install, app-data change, login-state change, or live source mutation was performed.

## Turn 60 advanced-search logical-expression parity

Read-only mobile-source Playwright verification on 2026-08-10 established the
operator behavior that is not obvious from the web help alone:

| Source input | Observed `GET /api/search` fields |
| --- | --- |
| `魔法 AND 学院 NOT 续作` | `q=魔法 学院`, `blocked_terms=续作` |
| `魔法 OR 学院` | `q=魔法 学院` |
| `tag:恋爱 AND tag:校园 OR tag:轻小说` | `tags=恋爱,校园,轻小说` |
| `tag:(恋爱 AND 校园) OR 轻小说 word:10w..50w` | `tags_expr=( 恋爱 AND 校园 ) OR 轻小说`, `min_word_count=100000`, `max_word_count=500000` |

Native changes:

- `AND` and `OR` outside a parenthesized tag expression are now recognized as
  source operators and never leak into the normal keyword `q` parameter.
- Parenthesized tag logic remains a dedicated `tags_expr` request, preserving
  source spacing and operator order rather than flattening it into ordinary
  tags.
- Advanced mode now uses the source syntax placeholder. Its long parentheses
  example is a full-width, two-line-capable fill button instead of an unbounded
  chip, which prevents clipping on narrow Android screens.
- Parser coverage now includes source operator stripping and the exact
  parenthesized expression request shape.

Verification:

- Debug and Release reports each passed 61 suites / 328 tests / 0 failures /
  0 errors / 0 skipped.
- Debug APK SHA256:
  `2ECB16188970B667E1EF01A516ECD94F6C87716EBA527E7E197DA796F4D3E735`.
- Unsigned Release APK SHA256:
  `B35CB267B0C291A6B005E8A16DF1EECC4B16CEE002E0B4E0215BE22F9B89A957`.
- At final package time, MuMu's expected `127.0.0.1:16384` endpoint was not
  listening, so this slice has source-browser and automated verification only.
  It remains queued for the next emulator-online runtime pass.

No live account, upload, administrator, workspace, payment, or content mutation
was submitted.

## Turn 83 Collection request priority and tab-state regression

Runtime analysis on MuMu Android 15 found that the visible authenticated shelf
was competing with work that the user had not requested:

- App startup fetched the Forum feed even though Collection is the initial tab.
- `loadHome()` started the profile and group requests before its visible
  `/api/favorites` page request.
- Tapping an already selected bottom tab cleared its loaded state and requested
  it again. A Home deep link during launch could also start a duplicate shelf
  request after `init` had already started one.

Native changes:

- Forum is now demand-loaded from its tab/deep-link entry, rather than during
  Collection startup.
- The visible favorites/history page starts first; profile and group chrome
  resolve afterward without blocking returned book cards.
- Repeated tab taps preserve the current screen. Collection and Forum only load
  on first entry or after an error; explicit refresh actions remain available.
- A startup Home deep link joins an in-flight initial Collection load rather
  than spawning a competing request.

Verification:

- With the same force-stop-and-launch MuMu procedure, full live Collection
  cards were present at the 6 s capture after request prioritization; the
  preceding on-demand-Forum version reached the same complete state at 8 s.
- Live Forum still loads on first tab entry. Re-tapping Forum preserves its
  loaded posts; returning to Collection and tapping it again preserves sharp
  covers and cards without a loading reset.
- `SearchCoverPrefetchTest`, `LibraryPresentationTest`, and `UiNavigationTest`
  passed with zero failures.
- Final Debug APK SHA256:
  `9B88784190879E60D9D0750ED2A1995A2AC19602F920723DF7DFE56B176A956E`.
- Runtime evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn83-collection-priority-4s.png`,
  `turn83-collection-priority-6s.png`, `turn83-forum-entry-final.png`,
  `turn83-forum-repeat-no-reload.png`, and
  `turn83-collection-return-preserved.png`, and `turn83-home-deeplink-6s.png`.

No live account, upload, administrator, workspace, payment, or content mutation
was submitted.

## Turn 85 search-cover warmup and missing-cover contract

Read-only live checks on 2026-08-12 established the following cover behavior:

- `GET /api/search` can return a normal `photo_url`, an empty value, or the bare
  `https://images.novelpia.com` host. The bare host is not an image asset and must not remain in
  an indefinite loading state; it is presented as the native title fallback.
- A representative valid original cover from `images.novelpia.com` was 341,830 bytes with
  `Cache-Control: max-age=2592000`. Through the configured host proxy it completed in about
  0.40 seconds, so delayed request scheduling rather than source image resolution was the
  actionable mobile bottleneck.

Native search now warms the first six distinct thumbnail URLs as soon as its source response
arrives. The preload request shares `BookCover`'s 512x768 request shape and cache key, so cards
reuse the same original-quality bitmap rather than downloading a smaller substitute. Scroll
look-ahead remains bounded to four URLs, but its debounce is 160 ms rather than 650 ms.

Verification:

- `SearchCoverPrefetchTest` and `NovalPieImageLoadingTest` were force-rerun with zero failures.
- The Debug APK was installed to MuMu with `adb install -r`; authenticated app data was kept and
  `adb reverse tcp:7890 tcp:7890` remained active.
- A live 20-result `q=a` search showed source-backed covers on the first result surface and after
  a rapid scroll. The remaining title-initial tiles corresponded to bare-host/empty source
  `photo_url` values, not stalled Coil requests.
- Evidence: `D:\NovalPie\native-android\qa-screenshots\turn85-search-cover-prewarm-6\a-native-response-2.1s.png`
  and `D:\NovalPie\native-android\qa-screenshots\turn85-search-cover-prewarm-6\a-native-results-0.8s-after-scroll.png`.

No live account, upload, administrator, workspace, payment, favourite, history, or content
mutation was submitted.

## Turn 86 reader, reviews, and profile-upload runtime regression

MuMu runtime verification on 2026-08-12 covered the user-visible routes most affected by earlier
navigation and rendering reports:

- Book `354491` opened with its original cover, source facts, complete tags, management controls,
  catalog, and Reviews tab. The compact review header exposed two live review cards plus the
  source actions for useful/not useful, emoji, award, and reply.
- From the Reader, a single catalog tap moved from chapter 1 to chapter 2. The Reader Back action
  returned to the same book detail page, whose progress marker correctly reported chapter 2.
- A rendered chapter illustration opened the full zoom/pan image preview. Android Back closed only
  that preview and restored the same chapter and scroll position.
- Text-area taps showed and hid the reader controls. After hiding them, a three-second idle capture
  kept the reader immersive; the historical toolbar auto-reopen issue did not reproduce.
- Profile -> Books rendered live uploaded-cover cards and the local "title, author, tag" search
  field. A `zzzz` query produced the explicit empty state and clearing it restored the cards.

Regression verification:

- `ReaderAdjacentChapterTest`, `ReaderPresentationTest`, `BookDetailProgressMarkerTest`, and
  `ProfilePresentationTest` were force-rerun successfully.
- This was a runtime-only audit; no source mutation, upload, administrator action, reaction, or
  comment submission was made. The existing Debug APK SHA256 remained
  `7C1025913A7D6E6548146418ADBBE6C2ABE77B7CE6DE30E358AC9C6E3CCB44C2`.
- Evidence: `D:\NovalPie\native-android\qa-screenshots\turn86-reader-regression\reader-catalog-open.png`,
  `reader-system-back-book-detail.png`, `reader-toolbar-text-toggle-hidden-stable.png`,
  `book-354491-reviews.png`, and `profile-books-search-empty.png`.

## Turn 87 forum navigation, administrator visibility, and IME environment audit

MuMu runtime verification on 2026-08-12 covered forum route freshness and current administrator
visibility:

- The live Forum feed loaded its five source categories and populated cards. Opening post A,
  returning, then opening post B produced B's distinct content rather than retaining post A.
  The original feed remained populated after the first Back action.
- A six-swipe fast scroll through A's 20-comment thread still rendered real comment cards near the
  bottom; the earlier disappearing-comments report did not reproduce.
- Tools showed the administrator-only management entry for the authenticated administrator. Admin
  Overview loaded live dashboard counts and trend data. No review, Key, Cookie, log, shop, or
  other administrative action was submitted.
- The MuMu configuration has hardware-keyboard mode enabled. Text fields accepted focus but did
  not summon the soft keyboard, so the historical soft-keyboard-close blank-space defect remains
  unverified in this environment rather than being marked passed.

Regression verification:

- `ForumPresentationTest`, `RouteStackPolicyTest`, `AdminPresentationTest`, and
  `WebsiteDeepLinkRouteTest` were force-rerun successfully.
- This was a read-only audit with no APK source change. Evidence:
  `D:\NovalPie\native-android\qa-screenshots\turn87-forum-navigation\forum-feed-after-a-back.png`,
  `forum-post-b.png`, `forum-post-a-fast-bottom.png`, and `admin-overview.png`.

## Turn 89 mobile-search source presentation, cover preview, and startup timing

The retained audited mobile-source artifact
`D:\NovalPie\native-android\output\playwright\source-search-grid-turn68.png` exposes
the live page's rules/tag/word-count controls as the default search surface. The native screen had
the same controls but hid them behind an app-only collapsed accordion on first entry.

Native changes:

- `SOURCE_SEARCH_FILTERS_START_EXPANDED` is now the explicit source-parity default. Rules, Tags,
  and Word Count are visible immediately on the first native search surface; the user can still
  collapse the compact panel after choosing filters.
- The collapse affordance now uses an up arrow while expanded and a down arrow while collapsed,
  eliminating the prior visually contradictory state.
- No search request, tag parameter, pagination, image size, proxy, authenticated session, or
  local search-history data contract changed.

MuMu runtime verification on `127.0.0.1:16384`:

- Debug was installed with `adb install -r` and `adb reverse tcp:7890 tcp:7890`; app data,
  authentication, reader progress, and the user-owned Edge session were left intact.
- The default Search tab showed the source-style Rules/Tags/Word Count rail and all six live rule
  selectors without an extra tap. A live `a` search returned the source pagination envelope and
  sharp valid covers within the two-second capture; the card surface showed complete platform,
  status/content tags, favorite/read/word facts after one normal scroll.
- One `a` result intentionally displayed its title-initial cover fallback at both 2 s and 4 s.
  This is the documented source empty/bare-host `photo_url` case, not a pending image request.
- Long-pressing a real Collection cover opened the original-resolution full-screen zoom/pan
  preview. This covers the source-style cover-preview requirement independently from the Reader
  illustration preview regression in Turn 86.
- A startup tab experiment initially looked like an ignored Search tap. The 250 ms evidence showed
  the app was still on the splash surface when the tap was sent; after the first interactive frame,
  the tab changes with one tap. This did not reproduce the historical double-tap/navigation bug.

Verification:

- `DiscoverPresentationTest`: 10 tests, 0 failures/errors.
- Full `:app:testReleaseUnitTest`: 67 suites / 365 tests / 0 failures / 0 errors / 0 skipped.
- `:app:assembleDebug` completed successfully. Debug APK SHA256:
  `4053870543302166EB99790D91BF71BE4AA433AB78B7C1744C7D86703CBD3F5A`.
- Evidence directory:
  `D:\NovalPie\native-android\qa-screenshots\turn89-source-parity-audit\`;
  key captures are `search-source-default.png`, `search-result-2s.png`,
  `search-cards-tags.png`, `cover-long-press-preview.png`, and
  `startup-tab-test\after-tap-4500ms.png`.

The existing user-owned Edge profile was preserved. It did not expose a direct automation attach
endpoint in this turn, so no browser restart, new profile, fresh login, cookie/token read, export,
or persistence was attempted.

## Turn 90 administrator native child-route runtime audit

Read-only MuMu verification used the existing administrator-authenticated native session and
native-scheme deep links, without opening a WebView fallback or invoking a server mutation:

- `novalpie://app/admin/shop` opened the native shop module with real item cards, type/status
  filters, grid/list controls, search, and its explicit create affordance. No create/edit/delete
  control was activated.
- `novalpie://app/admin/key-management` opened the native Key review and BaseURL policy screen.
  The visible review rows exposed only item names, owner/model/status, and confirmation-gated
  actions; no API Key secret value was rendered or accessed.
- `novalpie://app/admin/operation-logs` opened the native source-style operation filter rail and
  returned the live `382274`-record pagination plus real log rows after one normal scroll.
- Android Back from the Shop child route returned to the native Tools root rather than reopening a
  stale detail or the web site.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn90-admin-audit\admin-shop.png`
- `D:\NovalPie\native-android\qa-screenshots\turn90-admin-audit\admin-shop-back.png`
- `D:\NovalPie\native-android\qa-screenshots\turn90-admin-audit\admin-keys.png`
- `D:\NovalPie\native-android\qa-screenshots\turn90-admin-audit\admin-operation-logs-results.png`

No review, Key, BaseURL, scraper, shop, cookie, payment, or account operation was submitted.

## Turn 91 root IME inset contract and remaining Tools-route audit

The historical keyboard-close blank region could not be mechanically reproduced because the current
MuMu image exposes a hardware keyboard and does not show a software IME for normal text fields.
The root inset path nevertheless had a concrete Android 15 edge-to-edge gap: the activity used
`adjustResize`, but the root Material `Scaffold` did not include `WindowInsets.ime` in its content
budget or consume the supplied insets before delegating to route screens.

Native compatibility change:

- Root `Scaffold` now uses `WindowInsets.safeDrawing.union(WindowInsets.ime)` and the content
  `Surface` consumes the Scaffold padding after applying it. Status, navigation, gesture, and IME
  resizing now share one contract, so route content remeasures to the true post-keyboard height
  rather than retaining a stale inset budget.
- The change is inert while no IME is present. It does not alter authentication, navigation data,
  proxy selection, Coil requests, reader progress, or any source mutation contract.

Runtime verification:

- After `adb install -r` and `adb reverse tcp:7890 tcp:7890`, native Search rendered its complete
  source-style filter rail with no new empty band, clipping, or bottom-navigation shift.
- Native Reader for `354491/6992449` rendered a full text surface without top/bottom clipping after
  the root inset change.
- `novalpie://app/upload`, `novalpie://app/upload-editor`, and
  `novalpie://app/political-exam` each opened their native page directly. Upload/Editor file
  selection and Exam start/submit controls were deliberately not activated.
- Full `:app:testReleaseUnitTest` passed: 67 suites / 365 tests / 0 failures / 0 errors / 0 skipped.
  Debug APK SHA256: `34B262CD76FB53268C2E2E574C88A4335BC16470B22091CCBA71933C239B9B79`.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn91-tools-route-audit\insets-search-ready.png`
- `D:\NovalPie\native-android\qa-screenshots\turn91-tools-route-audit\insets-reader.png`
- `D:\NovalPie\native-android\qa-screenshots\turn91-tools-route-audit\upload-book.png`
- `D:\NovalPie\native-android\qa-screenshots\turn91-tools-route-audit\upload-editor.png`
- `D:\NovalPie\native-android\qa-screenshots\turn91-tools-route-audit\political-exam.png`

The software-IME close transition remains an explicit physical-runtime regression item; no emulator
or host keyboard setting was modified solely to manufacture that scenario.

## 2026-08-12 - Search cover bandwidth priority and source-missing assets

Read-only source inspection of the live `q=a` response returned 60 records. Twenty-five records
had either an empty `photo_url` or only the bare `https://images.novelpia.com` host, which is not
an image asset. Native normalisation continues to render those records as immediate title-initial
fallbacks; they are not reported as delayed or failed download work.

The valid source covers are original image files, not alternate thumbnail endpoints. A
representative cover returned `Content-Length: 463898` and completed through the configured local
proxy in about 0.62 seconds. To improve the user-visible first row without reducing cover clarity:

- `BookCover` still requests the shared 512x768 inexact Coil size and uses the same memory/disk
  cache key for display and warm-up.
- Search now starts only the first two cover warm-ups when a result page arrives.
- Off-screen warm-ups run through a shared two-fetch dispatcher. The existing four-cover
  post-scroll look-ahead remains in place after the visible row settles.

Runtime verification on MuMu `127.0.0.1:16384` used `adb install -r` and preserved application
data, authenticated app state, reader progress, and `tcp:7890` reverse proxy. A fresh `h` query
showed both valid first-row images sharp at two seconds; a quick scroll showed the next image row
complete at about one second. Full Release tests passed: 67 suites / 366 tests / 0 failures /
0 errors / 0 skipped. Debug SHA256:
`43B4D2E82792BE73C7413A00F14AC54D49CC67773BFE1B0BC67D1ED660AD63A1`.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn93-search-cover-performance\search-b-2s.png`
- `D:\NovalPie\native-android\qa-screenshots\turn93-search-cover-performance\search-b-fast-scroll-0.2s.png`
- `D:\NovalPie\native-android\qa-screenshots\turn93-search-cover-performance\search-b-fast-scroll-1.1s.png`

## 2026-08-12 - Mobile search layout parity and review/profile regression proof

The retained mobile source screenshot (`output/playwright/source-search-turn69.png`) uses a
compact search card followed by a thin Rules / Tags / Word Count rail. Its rule controls are
short, left-aligned label/select rows rather than a desktop-sized configuration form.

Native now follows that structure while preserving the complete live search contract:

- Rules exposes search scope, content filter, source, match mode, sort field, and sort direction.
- Tags still supports include/block modes, typed tags, selected tags, and live popular tags.
- Word Count remains its own source-style tab. Help, settings-cache policy, and clear-cache
  actions remain available in the compact utility row.
- Search history changed to compact filter chips; no history or quick-prompt behavior was removed.

MuMu runtime verification covered the compact Rules screen, open scope dropdown, Tags tab,
Word Count tab, and a live `h` result page with original-quality covers, full cards, and
pagination. My Profile -> Books displayed the title/author/tag upload search. Cover tap opened
the existing full zoom/pan preview. `novalpie://app/book/354491` loaded its live Reviews page with
two review cards and useful/not-useful, emoji, award, and reply controls.

Targeted Discover/BookDetail/Profile tests and full `:app:testReleaseUnitTest` passed:
67 suites / 366 tests / 0 failures / 0 errors / 0 skipped. Debug SHA256:
`E3C54C70A8A60D288D6556DF1C77BC9FD995CF44E0554F3AF707464AED951F19`.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn94-search-layout\search-compact-idle-after-tab.png`
- `D:\NovalPie\native-android\qa-screenshots\turn94-search-layout\search-rule-menu.png`
- `D:\NovalPie\native-android\qa-screenshots\turn94-search-layout\search-results-compact.png`
- `D:\NovalPie\native-android\qa-screenshots\turn94-search-layout\profile-books-search.png`
- `D:\NovalPie\native-android\qa-screenshots\turn94-search-layout\book-354491-reviews.png`

## 2026-08-12 - Live `/reader` redirect parity and foreground deep-link compatibility

Current Nuxt source was re-read from `/_nuxt/CxFG0gqQ.js`. Its complete 25-path route table matches
the documented public and administrator pages. The dedicated `/_nuxt/bNkDIOaS.js` component proves
that `/reader` is not a separate content page: it redirects
`/reader?novel=<id>&chapter=<id>` to `/book/<id>/<chapter>` and redirects a bare `/reader` to `/`.

Native now mirrors that behavior without a WebView:

- `readerLandingRoute` converts valid `novel` + `chapter` query ids into `AppRoute.Reader`.
- Missing, zero, or malformed ids resolve to the native Collection root, as the source does.
- `MainActivity` uses `launchMode="singleTop"`, so a new foreground browser/notification deep
  link is delivered to the existing Compose activity through `onNewIntent` rather than creating a
  stale duplicate route surface.

MuMu verification used explicit native URIs while the application was already foregrounded:

- `novalpie://app/reader?novel=354491&chapter=6992449` opened the native chapter reader.
- `novalpie://app/reader` returned to the loaded native Collection root.

`WebsiteDeepLinkRouteTest` covers the redirect query semantics. Full Release tests passed:
67 suites / 367 tests / 0 failures / 0 errors / 0 skipped. Debug SHA256:
`D67B7BFD890847B43E59E7AD1042FF14BE83CBDAA2220FCD66922DDEA74CE9B1`.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn95-reader-route\reader-query-manifest-retry.png`
- `D:\NovalPie\native-android\qa-screenshots\turn95-reader-route\reader-root-home-loaded.png`

## 2026-08-12 - Native detail Back-stack and grid-position restoration

Native `BookDetail` navigation was regression-tested with real MuMu source cards rather than only
route-stack unit tests. Search performed `A -> Back -> B -> Back` without resurfacing A: B rendered
its own title and the final Back returned to Search. The historical Favorites case also passed:
favorite A opened, Back returned to its row, favorite B then loaded B instead of stale A, and Back
returned to that same shelf row.

Search and Collection now retain their `LazyVerticalGrid` viewport when a detail route temporarily
leaves composition. A reader returns to the exact result/shelf row they opened instead of silently
resetting to the top. New searches, page changes, favorite query/filter changes, sorting, and
layout changes deliberately reset their respective grid to the top so a changed dataset never
restores an invalid old index.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn98-scroll-restoration\search-after-book-b-back.png`
- `D:\NovalPie\native-android\qa-screenshots\turn98-scroll-restoration\favorites-after-book-back.png`
- `D:\NovalPie\native-android\qa-screenshots\turn98-scroll-restoration\favorites-after-a-b-back.png`

## 2026-08-12 - Reader process-reclaim restoration and Back-chain regression

Android may reclaim the process while the reader is in the background. The native reader now
persists a minimal active-session marker containing only the book and chapter ids; reading
position remains owned by the existing progress store. On a cold process restart, the route stack
is rebuilt as `Home -> BookDetail -> Reader`, so System Back still has a meaningful destination.
The marker is cleared for normal reader exit and other deliberate navigation, preventing an old
reader from reopening after the user has left it.

MuMu verification used `novalpie://app/book/354491/6992449`, sent the app to Home, executed
`am kill com.novalpie.app.debug`, and cold-started the activity. The restored surface was the
same first chapter. System Back then opened the correct native book detail for book `354491`.
After that normal exit, another Home + process-kill + cold-start opened Collection rather than
the reader; the ordinary Continue Reading progress card remained available. Debug installation
used `adb install -r`, preserving existing app data, authentication, proxy setup, and progress.

`:app:assembleDebug` and full `:app:testReleaseUnitTest` both passed. Release tests: 69 suites /
375 tests / 0 failures / 0 errors / 0 skipped. Debug SHA256:
`F3627AC6C84CDDB8E78E0964E70DA63DAD922EDA1BD00040FAA741CBACC579B8`.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn99-reader-lifecycle\reader-before-kill.png`
- `D:\NovalPie\native-android\qa-screenshots\turn99-reader-lifecycle\reader-after-kill.png`
- `D:\NovalPie\native-android\qa-screenshots\turn99-reader-lifecycle\book-detail-after-reader-back.png`
- `D:\NovalPie\native-android\qa-screenshots\turn99-reader-lifecycle\home-after-normal-exit-cold-start.png`

## 2026-08-12 - Forum gesture and feed-position restoration

The forum feed uses normal Compose click cancellation during drag gestures. A MuMu fast-scroll
regression performed five short flings across live cards and remained on the feed; it did not
open a post accidentally. A subsequent deliberate tap still opened the selected live post and
System Back returned to the feed.

The return flow previously recreated `ForumScreen` with a fresh `LazyColumn`, losing the reader's
place. Forum now stores a route-owned viewport snapshot on disposal and restores it when a post
detail route is popped. Category changes, explicit forum searches, and refreshes still reset to
the top because they replace the dataset. The implementation shares the already-tested viewport
coordinate model used by Collection and Search without altering post requests or content state.

MuMu verified a scroll to the live `求真操逆转文` card, opened it, and returned with the same
surrounding card group still visible. Debug was installed with `adb install -r`, preserving
application data, authentication, reader progress, and proxy routing. Full Release tests passed:
69 suites / 375 tests / 0 failures / 0 errors / 0 skipped. Debug SHA256:
`E1342E20A90875AC2C66DBE26F225E8ABA008B089C2C5D319AF02AE0E0C701A3`.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn100-gesture-regression\forum-feed-after-fast-scroll.png`
- `D:\NovalPie\native-android\qa-screenshots\turn100-gesture-regression\forum-detail-from-scrolled-feed.png`
- `D:\NovalPie\native-android\qa-screenshots\turn100-gesture-regression\forum-scroll-before-open-restoration.png`
- `D:\NovalPie\native-android\qa-screenshots\turn100-gesture-regression\forum-scroll-after-back-restored.png`

## 2026-08-12 - Search-card gesture routing and cover-preview regression

Live `q=h` search returned the source's 285-result pagination with two-column cards, original
covers, full platform/status/tag/fact payloads, and explicit title-initial fallbacks for records
whose source `photo_url` is absent. The fallback is immediate rather than an indefinite loading
placeholder.

Card gesture routing was rechecked end-to-end on MuMu:

- A normal cover tap opens the full-resolution zoom/pan preview only; it does not navigate to
  the book detail. Android Back closes the preview and returns to the same card viewport.
- A deliberate title/body tap opens the selected native book detail. Android Back restores the
  same search-card group.
- Five rapid vertical swipes through populated cards remained in Search, proving that drag
  cancellation does not turn a fling into a detail navigation.

This confirms both source-visible cover-preview behavior and the earlier accidental-navigation
regression for book cards. The native Debug install and proxy route were left intact; the prior
full Release verification remains 69 suites / 375 tests / 0 failures / 0 errors / 0 skipped.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn101-book-card-gestures\search-cover-tap-preview.png`
- `D:\NovalPie\native-android\qa-screenshots\turn101-book-card-gestures\search-after-cover-preview-back.png`
- `D:\NovalPie\native-android\qa-screenshots\turn101-book-card-gestures\book-card-body-opened-detail.png`
- `D:\NovalPie\native-android\qa-screenshots\turn101-book-card-gestures\search-after-book-detail-back.png`
- `D:\NovalPie\native-android\qa-screenshots\turn101-book-card-gestures\search-fast-scroll-no-navigation.png`

## 2026-08-12 - Current live route-table drift check

A read-only fetch of the current public Nuxt bootstrap through the existing proxy confirmed build
`cbb20abd-9b37-4381-993e-3008189d738b`, theme metadata `#4F46E5`, and the same two router chunks
(`CgAuL9Tc.js`, `CxFG0gqQ.js`). The live router exposes 29 path patterns: the documented public,
book/editor, and six administrator routes. No new route or source-route removal was detected
against the native route matrix.

`nativeWebsiteRoute` and `WebsiteDeepLinkRouteTest` cover every live route shape, including the
bare `/reader` redirect, both book detail forms, chapter reader, managed-book subroutes, and
strict pre-navigation administrator gating. The check used no browser automation and did not
attach to, read, export, clear, or modify the user-owned Edge profile or its authentication state.

Artifacts:

- `D:\NovalPie\native-android\qa-artifacts\turn102-live-source-audit\home.html`
- `D:\NovalPie\native-android\qa-artifacts\turn102-live-source-audit\router.js`
- `D:\NovalPie\native-android\qa-artifacts\turn102-live-source-audit\entry.js`

## 2026-08-12 - Bottom-navigation double-tap regression

The historical report was that a quick second tap on Tools could eventually expose Workspace.
MuMu started from a populated Search result page and sent two Tools-tab taps 130 ms apart. The
result remained on the native Tools root (`功能中心`), with its message preview, appearance,
Chinese-variant, and website capability surfaces loaded; Workspace was not opened.

`openTab` intentionally treats a repeated tap on the already-selected root route as a no-op. This
preserves the current surface and prevents a delayed duplicate tap from creating an extra route or
restarting an unrelated tool flow. The regression was read-only and did not invoke a workspace,
message, upload, administrator, or account mutation.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn103-bottom-nav-double-tap\tools-double-tap.png`

## 2026-08-12 - Own-upload search regression

The owner profile's Books tab loaded real uploaded-book cards with original covers, source/status
badges, and the dedicated local search field labelled `Search uploaded books (title, author, tags)`.
Entering a deliberate no-match query rendered an explicit empty state; clearing the field restored
the original card grid without a new route, stale state, or blank card.

`filterBooks` is covered by `ProfilePresentationTest` for independent title, author, and tag
matches. The runtime input/clear check made no server mutation and did not alter any account data.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn104-profile-upload-search\profile-books-search-initial.png`
- `D:\NovalPie\native-android\qa-screenshots\turn104-profile-upload-search\profile-books-search-empty.png`
- `D:\NovalPie\native-android\qa-screenshots\turn104-profile-upload-search\profile-books-search-cleared.png`

## 2026-08-12 - Managed book-info save contract audit

The native managed-book editor for `book/354491` loaded the live cover, title, original title,
author, description, field permissions, reading/download policy, and transfer controls. Its primary
Save Basic Information action is enabled only after local validation and now opens an explicit
confirmation dialog before the mutation path; cancelling leaves both the source and local draft
unchanged.

The contract test `managedBookInfoPermissionsAndSaveUseWebsiteContracts` proves the request is
`PATCH /api/users/me/novels/{id}`, with source field names and partial-failure reporting. No save,
cover upload, transfer, policy change, or other remote mutation was confirmed during this audit.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn105-book-edit-save-contract\book-edit-info-loaded.png`
- `D:\NovalPie\native-android\qa-screenshots\turn105-book-edit-save-contract\book-edit-save-visible.png`
- `D:\NovalPie\native-android\qa-screenshots\turn105-book-edit-save-contract\book-edit-save-confirmation.png`

## 2026-08-12 - Book-review visibility regression

Current native `book/354491` opened its Reviews tab with two live review cards. The compact
source-style review header showed the count, write-review affordance, and website fallback link.
Each visible card retained author, timestamp, body, and useful/not-useful, emoji, award, and reply
actions. This rechecks the original report that book reviews rendered as an empty/blank area.

The audit was read-only: no review, reaction, award, reply, favourite, or account mutation was
submitted.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn106-book-reviews\book-detail-before-reviews.png`
- `D:\NovalPie\native-android\qa-screenshots\turn106-book-reviews\book-reviews-loaded.png`

## 2026-08-12 - Search-cover scroll and Android compatibility verification

The native card path continues to request source cover assets at the shared 512x768 inexact Coil
size; it does not substitute a lower-quality image. The search grid now derives its viewport data
before calculating bounded preload URLs, preventing every scroll measurement from recomposing the
surrounding search screen.

After an in-place Debug update on MuMu, a live `q=h` query showed the expected native skeleton at
0.35 seconds and a valid sharp source cover at 1.50 seconds. A source record with no valid image
URL displayed the intentional `暂无封面` fallback. A subsequent fast vertical scroll retained
source tags, platform/status pills, favourites, reads, and word-count facts without blank cards,
route changes, or an app crash.

The same build adds API 23 HTML parsing, API 28 Chinese-variant fallback, API 23 collection
compatibility, and API 27-qualified navigation-bar theme resources. Full Release tests passed
(69 suites / 377 tests / 0 failures / 0 errors / 0 skipped), and `lintDebug --no-daemon` reported
`No issues found.` Browser verification remains reuse-only: the existing user-owned Edge profile
was not attached to or modified, and no cookie, token, password, tab, or storage value was read or
persisted.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn108-compat-search-scroll\search-h-0.35s.png`
- `D:\NovalPie\native-android\qa-screenshots\turn108-compat-search-scroll\search-h-1.50s.png`
- `D:\NovalPie\native-android\qa-screenshots\turn108-compat-search-scroll\search-h-scroll-1.20s.png`

## 2026-08-12 - Search-cover first-scroll warm-cache verification

The live search contract continues to provide original `images.novelpia.com` cover files rather
than a separate mobile thumbnail API. A representative source image returned 341,830 bytes with
`Cache-Control: max-age=2592000`; native therefore retains the 512 x 768 decode target instead of
reducing source cover quality.

The native warm-up now queues the first two two-card rows (at most four valid URLs) when a search
page arrives. It shares the exact same Coil cache key and visible-work dispatcher as the grid;
scroll look-ahead remains a separately capped four-URL/two-request speculative batch. This avoids
the earlier visual gap when a reader immediately flings past the filter panel.

MuMu (`127.0.0.1:16384`) received the Debug APK via `adb install -r`, with the existing app
session and `tcp:7890` reverse route preserved. A fresh live `b` query showed the request state at
about 0.35 s, sharp first-row covers at about 1.50 s, and complete next-row covers/tags/facts
after a fast scroll. No source mutation occurred. The compact search field also now reflects real
focus visually.

Verification: targeted image/search/reader tests passed; full Release unit tests passed 69 suites
/ 378 tests / 0 failures / 0 errors / 0 skipped; `lintDebug --no-daemon` reported no errors or
warnings. Debug SHA-256:
`100715728BAFFF6120CE145EC1F56AA8F90D5FA66347E11C232F5B896110D6E3`.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn110-search-reader-regression\search-b-correct-0.35s.png`
- `D:\NovalPie\native-android\qa-screenshots\turn110-search-reader-regression\search-b-correct-1.50s.png`
- `D:\NovalPie\native-android\qa-screenshots\turn110-search-reader-regression\search-b-correct-scroll-0.25s.png`

## 2026-08-12 - Account hub and uploaded-book search regression

The native account route retains the source-backed profile and upload contracts documented above:
`GET /api/users/me`, account check-in/inventory/reward reads, and
`GET /api/users/me/uploads?user_id={currentUserId}`. No source route or request contract was
changed in this pass.

The default Account surface now prioritises native navigation to Upload Books, Check-in Centre,
and App Settings. Edit Profile and Adult Verification preserve their existing inputs, save and
confirmation flows but are collapsed until explicitly opened. The Upload Books shortcut enters
the existing local search surface, whose query still checks source card title, author, and tags.
The shortcut does not display a work-count derived from the raw uploads list, so a paged or broad
server response cannot be misrepresented as an author-work total.

On MuMu, the account hub rendered the retained profile identity/status and its Upload Books
shortcut. The Books tab displayed original-cover cards and the source/status badges; a deliberate
`zzzzzzzz` query displayed `没有匹配的上传书籍`, and clear restored the same cards without a route
change. The run used `adb install -r` and kept the native session/proxy route intact. It made no
account, book, review, or other source mutation.

Verification: `:app:testReleaseUnitTest --no-daemon` passed 69 suites / 379 tests / 0 failures /
0 errors / 0 skipped; `:app:assembleDebug --no-daemon` passed. The generated lint report contains
`No issues found.`

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn112-profile-hub-final.png`
- `D:\NovalPie\native-android\qa-screenshots\turn112-profile-upload-search-final.png`
- `D:\NovalPie\native-android\qa-screenshots\turn112-profile-upload-search-empty.png`
- `D:\NovalPie\native-android\qa-screenshots\turn112-profile-upload-search-cleared.png`

## 2026-08-12 - Search content-rating (adult) filter restoration

The native `/api/search` contract has always carried the live source parameter
`adult_filter` with its three source values: `all`, `adult_only`, and `unrestricted`.
The compact mobile redesign incorrectly placed that selector only inside the search-settings
dialog, which made the normal Rules rail omit a source search control.

The Rules rail now exposes **内容筛选** directly after **范围**, matching the source control
order: scope, content rating, source, search mode, sort field, and sort direction. The selector
shows **所有**, **仅成人**, and **全年龄**; changing it updates the persisted search option and
the next search sends the matching `adult_filter` value to `/api/search`.

Verification: targeted Debug tests and the full Release unit suite passed (69 suites / 381 tests /
0 failures / 0 errors / 0 skipped). MuMu received the Debug APK through `adb install -r`, with
existing application data and native authentication preserved. Its Rules UI hierarchy confirms the
visible `内容筛选: 所有` row. The MuMu ADB listener became unavailable before the dropdown-menu
capture, so choice-menu runtime evidence remains queued for the existing instance's next ADB
availability; no emulator reset, app-data clear, or source mutation was performed.

Evidence:

- `D:\NovalPie\native-android\qa-screenshots\turn114-search-adult-filter\search-rules-adult-filter.png`
- `D:\NovalPie\native-android\qa-artifacts\turn114-search-adult-filter.xml`

## 2026-08-16 - Collection/profile grid controls and long-press preview policy

The live mobile source was rechecked read-only with Playwright. The unauthenticated Collection
route exposes the six compact actions (layout, groups, selection, cache, clear cache, sort), the
favourites/history tabs, four overview metrics, and the login/search empty state. The source
Search route continues to expose the complete tag rail and card facts.

Native changes in this pass:

- Collection now persists an explicit `gridColumns` choice limited to 2, 3, or 4 and exposes the
  choice beside the source-compatible toolbar. The selected count drives `LazyVerticalGrid` and
  row tag-height calculation instead of being inferred only from screen width.
- Owner uploaded-book management now has the same 2/3/4 control with an independent local store.
  Each row computes the maximum tag footprint, and every card keeps its title and author slots.
- Collection, uploaded-book, upload-editor, public-profile, and forum thumbnail covers no longer
  open previews; their parent navigation targets remain unchanged.
- Search cards, book-detail covers, and reader illustrations use a stationary long-press-only
  preview. A normal tap remains navigation, and movement past touch slop cancels the preview.
  The long-press handler consumes only the pointer-up event after the threshold so an ancestor
  card cannot also navigate.

Verification:

- Final `:app:testReleaseUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon
  --max-workers=1` verification passed.
- Release unit tests: 72 suites / 463 tests / 0 failures / 0 errors / 0 skipped.
- `lintDebug`: no issues found.
- Debug APK SHA-256: `4C64D84FAE0E5CEE5A14DF33E77E0F0E3DC247A5272E6EB0DDF6AE6E3746272D`.
- Current APK: `D:\NovalPie\native-android\app\build\outputs\apk\debug\app-debug.apk`.
- MuMu local ADB was unavailable during this pass: `adb devices` returned no device and
  `127.0.0.1:16384` refused the connection. No app data or login state was cleared.

Source evidence:

- Playwright snapshot: `D:\NovalPie\.playwright-cli\page-2026-08-16T11-51-07-268Z.yml`.

## 2026-08-16 - Mobile search-rule and book-review source parity

Read-only Playwright captures at a `412 x 915` viewport reconfirmed the current mobile source
layout. `/search?sort_by=relevance` renders the Rules tab as compact **single-column** rows on a
phone: a separate label and a fixed-width bordered selector, rather than two compressed controls
inside one wide card. The source returns 47,398 works and retains all scope, source, match mode,
sort, cache, tab, pagination, card-tag, cover-status, favourite, read-count, and word-count
surfaces. `/forum?tab=review` currently reports 22,793 book reviews and renders a direct
book-detail card with reviewer identity, badges, linked cover, preview, reply count, interaction
count, and relative time.

Native changes:

- Search rules now stack on content widths below `520dp`, preserving readable labels and the
  source's compact 32dp selector density on portrait phones.
- At `520dp` and above, paired rules return only when both label and source-width selector fit
  without truncating the content-rating label. The selector border now surrounds the value only,
  matching the source hierarchy rather than turning every rule row into a large card.
- The legacy filter callback was updated to the current `内容筛选` label so the adult-content
  selector cannot silently fall through if that presentation path is used.

Verification:

- `DiscoverPresentationTest`: 14 tests / 0 failures / 0 errors / 0 skipped.
- `:app:assembleDebug` passed and `:app:lintDebug` reported `No issues found`.
- Debug APK SHA-256: `C601200FED9A3D8550B1A66F235B685D343913497A77541354A9A0C2ECF4154A`.
- MuMu remains unavailable (`adb devices` empty), so this change has no fabricated device
  screenshot or installation claim.

Evidence:

- `D:\NovalPie\.playwright-cli\page-2026-08-16T15-28-16-825Z.yml`
- `D:\NovalPie\.playwright-cli\page-2026-08-16T15-28-52-955Z.png`
- `D:\NovalPie\.playwright-cli\page-2026-08-16T15-32-06-715Z.yml`
- `D:\NovalPie\.playwright-cli\page-2026-08-16T15-33-01-959Z.png`

## 2026-08-17 - Search vocabulary source parity

The same current `412 x 915` mobile source capture names its visible Rules controls **搜索范围**,
**内容筛选**, **来源**, **搜索模式**, **排序方式**, and **排序方向**. Native previously shortened four
of those labels, making the compact UI less self-explanatory and visually inconsistent with the
site even though the request parameters were correct.

Native now uses the exact source vocabulary in filter groups, selected-filter summaries, help
copy, and the legacy fallback dispatcher. The compact view keeps the source's full-width Chinese
labels with a display-only full-width colon while preserving the internal value mapping for all
search API parameters.

Verification:

- `DiscoverPresentationTest`: 14 tests / 0 failures / 0 errors / 0 skipped.
- `ProductCopyTest`: 11 tests / 0 failures / 0 errors / 0 skipped.
- Full Release unit suite: 72 suites / 464 tests / 0 failures / 0 errors / 0 skipped.
- `:app:assembleDebug` passed; `:app:lintDebug` reported `No issues found`.
- Debug APK SHA-256: `6512E5D8ADC4B4D12BF3AF4094C713C762C444EDF229825BE2D2256098CB1850`.
- MuMu's current exposed port is `127.0.0.1:49792`, but its ADB transport is still `offline`;
  no installation, state reset, rotation change, or proxy mutation was attempted.

## 2026-08-17 - Live book detail and v2 chapter directory parity

Read-only Playwright verification on public `/book/95654` confirmed the current production page
uses these data contracts:

- `GET /api/novels/95654/detail` with `fontNumber`, `siteReadCount`, `novelRead`, `recommend`,
  and `sourceFavoriteCount` for the five visible numerical detail statistics;
- `GET /api/v2/novels/95654/chapters` with `chapter_number`, `word_count`, `image_count`, and
  `updated_at` for each directory row.

The source page renders each row as title, `EP.<number>`, a localized timestamp, compact word
count and an image marker when `image_count > 0`. Its visible `无缓存` marker is not in the public
directory response, so Native does not pretend it knows a cache result.

Native changes:

- `NovalPieApi.chapters()` now requests the v2 directory path and normalizes all documented
  chapter metadata, including common illustration-count aliases;
- `NovelCard` maps the current detail stats without confusing the explicit native `siteReadCount`
  and original-platform `novelRead`; `fontNumber` backs numeric word count and `recommend` backs
  the visible native recommendation fact;
- Native Book Detail and the reader catalog now render `EP.`, source-style dates, `K/M` word
  counts, and image count markers.

Verification:

- focused `NovalPieApiTest`, `BookDetailFactsTest`, and `BookDetailPresentationTest` passed;
- full Release suite: 72 suites / 466 tests / 0 failures / 0 errors / 0 skipped;
- `:app:lintDebug` reported `No issues found`; `:app:assembleDebug` passed;
- Debug APK SHA-256: `C74D80DDB399D84D2452F12E1C22A467DCE091D4B9ECB6686A3FC996A5C294B8`.

Evidence:

- `D:\NovalPie\native-android\.playwright-cli\page-2026-08-16T16-33-26-607Z.yml`;
- `D:\NovalPie\native-android\.playwright-cli\page-2026-08-16T16-43-53-073Z.png`.

## 2026-08-17 - Collection/upload grid and preview-policy revalidation

The preserved `412 x 915` mobile-source audit for `/favorites` exposes six compact Collection
actions (layout, groups, selection, cache mode, clear cache, sort), Favorites/History tabs, and
the authenticated library workflow. Native keeps the associated source contracts: group folders
and management, display modes, sort, pin/unpin, local search, paged loading, batch move/remove,
selected or full reading-history deletion, settings-cache policy, and explicit Coil image-cache
clearing.

The Collection grid persists a user-selected 2, 3, or 4 columns. It uses a keyed
`LazyVerticalGrid` and computes the maximum rendered tag-line footprint for each row; every card
therefore preserves its two-line title and one-line author slots while tag-rich neighbours do not
leave misaligned card bottoms. Owner Upload Books has a separate persisted 2/3/4-column choice
and the same tag-row alignment rule.

Cover preview is intentionally disabled in Collection and Upload Books so long press is reserved
for selection/management and cannot hijack navigation. Search cards, Book Detail, and reader
illustrations use a stationary-long-press-only image viewer. The detector observes rather than
consumes ordinary gestures, cancels once travel exceeds touch slop, and consumes only the terminal
pointer-up after a valid long press.

Verification:

- `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1`
  passed;
- 73 suites / 476 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`;
- Debug APK SHA-256:
  `1F2930C06CCD94832051CD1EDAA3238D185F406F92F2697EE7FF8E3E5F09C0B9`.

The exposed MuMu device `127.0.0.1:49792` was offline during this validation, so no installation
or runtime screenshot is claimed and no emulator, proxy, application data, login, or browser
session state was altered.

## 2026-08-17 - Book Detail actions and terminology contract

Public mobile source inspection of `/book/95654` confirms the visible Book Detail actions include
continue/read, favourite, terminology, and `更多 -> 分享`. The terminology overlay requests:

```text
GET /api/terminologies?novel_id=95654&keyword=&page=0
```

The response is zero-based and carries `content[].id`, `content[].novelId`,
`content[].sourceName`, `content[].targetName`, `content[].info.description`,
`content[].lockStatus`, `content[].isActive`, `content[].createdAt`, `content[].updatedAt`, plus
`page`, `size`, `total`, and `totalPages`. The inspected book reports 16,507 entries, so the
native route intentionally uses server paging and a keyed `LazyColumn`; it does not load all
terminology into a normal `Column`.

Native implementation now includes:

- `NovalPieApi.terminologyPage()` with the observed `novel_id`, `keyword`, and zero-based `page`
  contract;
- a native read-only terminology route with keyword search, entry status/description display,
  load-more pagination, and a clear webpage-management fallback for un-audited write operations;
- source-backed Book Detail favourite add/remove, a native Android share sheet under `更多`, and an
  authenticated `网页下载 EPUB` entry that reuses `WebFallbackScreen` plus `WebDownloadBridge` rather
  than generating a second EPUB or duplicating illustration bytes;
- independent Book Detail and terminology request serials so rapid A -> B -> A navigation cannot
  paint a stale response over the active book.

Regression proof:

- API test covers endpoint, query values, `content` fields, zero-based page metadata, and nested
  `info.description`;
- presentation tests cover terminology status labels and pagination rules;
- request-freshness and navigation tests cover the terminology child route.

Full `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon
--max-workers=1` verification passed with 74 suites / 482 tests / 0 failures / 0 errors / 0
skipped. `lintDebug` reports `No issues found`; Debug APK SHA-256:
`6583605CBE2B82963ABF8C77980272DB89BE4651AD3767C49FAF63D6C1165A17`. MuMu remains
`127.0.0.1:49792 offline`; no installation, app-data, proxy, or authenticated browser/session
state was altered.

## 2026-08-17 - Book Detail mobile presentation contract

Read-only mobile source evidence for `/book/95654` establishes the route's primary presentation
order: compact book identity and data, then `简介`, `目录 <chapter count>`, and `评论`; the directory
shows `正文卷 · 共 <count> 章` with `正序` and `倒序`; the fixed bottom controls are favourite, menu,
and immediate/continue reading.

The native route keeps the existing data/API contracts and now maps that presentation directly:

- `简介` holds full description, source facts, tags, and granted management controls;
- `目录 <count>` derives its count from `/api/v2/novels/{id}/chapters`, filters locally, and only
  reverses the rendered copy for descending order;
- `评论` retains the source-confirmed `/api/comments?type=book&book_id={id}&page=1&limit=30`
  request and all existing reply/reaction actions;
- the fixed `收藏 / 菜单 / 立即阅读` bar preserves terminology, Android sharing, webpage detail,
  authenticated webpage EPUB download, and administrator/owner routes.

Presentation-only verification is covered by `BookDetailPresentationTest`, including tab labels,
heading count, and ascending/descending rendering. Runtime capture remains pending the existing
MuMu ADB transport; no authenticated browser state is used or stored by this audit.

## 2026-08-17 - Collection mobile controls and Book Detail detail-field parity

Fresh read-only public mobile capture at `412 x 915` reconfirmed `/favorites` exposes:

- a local `搜索收藏（书名、作者、类型）` field;
- six toolbar actions: view, group/display management, selection, cache policy, clear cache, and
  sort;
- `我的收藏` / `阅读历史` tabs;
- group-display choices: `默认布局`, `全部小说`, and `未分类`; and
- login/search empty-state actions when no source session is available.

Evidence is stored in:

- `output/playwright/source-favorites-mobile-412x915-turn170.png`;
- `output/playwright/source-favorites-group-manager-mobile-turn170.png`.

Native Collection preserves the entire authenticated contract already recorded in this matrix
(groups, pinning, pagination, image-cache clearing, batch movement/removal, and history deletion)
and now renders books as full-span logical rows. Each row is measured to its tallest card, then
every sibling fills that height; variable tag counts therefore cannot leave ragged card frames.
The persisted Collection and owner Upload Books `2 / 3 / 4` column selectors remain independent.
Collection/Upload covers retain disabled preview for safe management; Search, Book Detail, and
reader illustrations use stationary-long-press-only preview with touch-slop cancellation.

The detail normalizer additionally maps the live detail aliases for `chapter_num`,
`max_chapter_number`, `guarantorInfo`, uploader objects, `is_adult`, and `allowDownload`. Native
Book Detail displays source/local statistic rails separately from description metadata, falls back
to the advertised chapter count only while the chapter list has no entries, and exposes webpage
EPUB download only when authenticated and `allowDownload` is not explicitly false.

Verification: full `:app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline
--no-daemon --max-workers=1` passed with 74 suites / 485 tests / 0 failures / 0 errors / 0
skipped; lint reports `No issues found`. The produced debug APK SHA-256 is
`94E08934D9357C8F76736283ECDC019749CDDBB6623BDD1C94903E05BB7C7A87`. The exposed MuMu transport
remains `127.0.0.1:49792 offline`, so no native runtime screenshot is claimed for this turn.

## 2026-08-17 - Final implementation verification refresh

The source-backed Collection, Search, and owner Upload Books contract above was rebuilt after the final
intrinsic-row safety correction. Local verification passed with 74 Release test suites / 485 tests / 0
failures / 0 errors, `lintDebug` reporting `No issues found`, and a successful Debug assembly. The current
Debug APK SHA-256 is `D1DB0EAC6B44C34A55C9DB30D4B382D5159829042EEBCDD78A7A31D5435743E5`.

MuMu remains exposed as `127.0.0.1:49792 offline`; a scoped reconnect of this same endpoint returned to
`offline`. Runtime visual confirmation is deferred until the existing transport is online, without resetting
the emulator or touching its proxy, app data, login, cookies, or browser state.

## 2026-08-17 - Turn 172 live mobile source evidence

At `412 x 915`, the public source currently confirms:

- Search defaults to a populated two-column feed. Its first request is
  `/api/search?page=1&limit=60&scope=all&match_type=fuzzy_strict&sort_by=relevance&sort_order=desc&adult_filter=unrestricted&max_word_count=10000000`; the response currently reports 47,398 works. Rules, tags, word-count, cache/help/settings, pagination, and grid/list controls are visible in the initial mobile surface.
- Review feed requests `/api/comments/book-reviews?page=1&limit=20&hide_spoilers=1`, currently
  reports 22,832 items, and returns `bookId`, `bookCover`, avatar/frame URLs, `authorBadges`, and
  content containing `||...||` spoiler delimiters. Native now maps that delimiter to a masked
  `Spoiler` segment in both feed and detail renderers.
- Book Detail `/book/95654` and Reader `/book/95654/1419162` retain the source hierarchy and
  reader measurements documented above. Fresh visual captures are stored under
  `output/playwright/*-mobile-turn172.png`.

The current native verification for this source refresh is 74 Release test suites / 486 tests with
0 failures, lint `No issues found`, and Debug APK SHA-256
`027321D582D12FE95E93302E0353DB6284595560DC8F2DE39C10F400B3D3EFAD`. Authenticated/admin route
visual verification remains explicitly pending the user-owned Edge session becoming attachable and
MuMu's existing ADB transport becoming online.

## 2026-08-17 - Turn 173 Collection/Upload grid verification

Native Collection and owner Upload Books now have independently persisted `2 / 3 / 4` column
selectors. Their logical rows occupy the full grid span and use the maximum tag footprint in that
row; each sibling fills the resulting height, while title, author, and all source tags remain
visible. Collection and list-mode covers use `CoverPreviewPolicy.Disabled`, and Collection long
press is reserved for batch selection. Search, Book Detail, and reader illustrations retain the
stationary-long-press-only preview contract with touch-slop cancellation.

The source Collection action set remains mapped: layout, groups/display, selection, cache policy,
clear cache, sort, Favorites/History tabs, group management, pinning, local search, paging, batch
move/remove, and selected/full history deletion.

Post-portrait-policy verification passed: 75 Release test suites / 488 tests / 0 failures / 0 errors /
0 skipped; `lintDebug` has zero severity Error/Warning issues; `git diff --check` passed. Debug APK
SHA-256: `22EC26FB5479302B92C3207FB9D276E678335B185EA37EFA2D6F09D03D0582F8`.

MuMu `127.0.0.1:49792` is still offline after a scoped reconnect. No runtime install or screenshot
is claimed, and no app data, proxy, account, cookie, token, or browser state was changed.

## Turn 174 - Collection controls and custom reader theme parity

The mobile source Collection toolbar and group manager remain the reference for the native shelf.
Native keeps the source actions (layout, groups/display, selection, cache, clear, sort,
Favorites/History, pinning, search, paging, and batch mutations) and adds a local presentation
selector for `2 / 3 / 4` books per row. Collection, Search, and owner Upload Books render a full-span
logical row and fill siblings to the row's tallest card, preserving source tags plus title/author
slots. Management covers use no image-preview gesture. Search, Book Detail, and reader images use
only a stationary long press, and touch-slop movement cancels it.

The source reader theme surface also exposes custom-theme editing in addition to presets. Native
now persists up to twelve `ReaderCustomTheme` records with page/sidebar colors, accent color, name,
and an optional local background image. The palette resolver validates `#RRGGBB` values and applies
the selected record to page text, reader chrome, side panels, and the background image layer;
missing or deleted custom records safely fall back to `system`.

Local verification after this pass:

- 76 Release test suites / 494 tests / 0 failures / 0 errors / 0 skipped;
- `lintDebug`: `No issues found`;
- Debug APK SHA-256:
  `72B0B60AEFC8D5F6EFAE13BC85AAC8555235D66ADF2265B6699EA6D9D1E32A42`.

MuMu `127.0.0.1:49792` remains offline, so this turn claims no new native runtime screenshot.

## Turn 175 - Collection toolbar and group-preview source audit

Read-only mobile source review at `412 x 915` confirms the Collection surface keeps its compact
toolbar in this order: layout, group/display management, selection, cache policy, clear cache, and
sort. The group sheet exposes `默认布局`, `全部小说`, and `未分类`; the sort sheet exposes
`收藏时间`, `阅读时间`, and `更新顺序`, each with ascending/descending order. Captures:

- `output/playwright/source-favorites-group-manager-mobile-turn175.png`;
- `output/playwright/source-favorites-sort-mobile-turn175.png`.

Native already sends `with_preview=true` and `preview_limit=6` to
`GET /api/favorites/groups`. The response's `previews` are now rendered in the default-layout
folder card as a static stack of up to three deduplicated source covers. This restores the data
that had been normalized but visually dropped, while preserving the user-requested management
gesture policy: folder-stack covers do not expose image zoom, Collection book-card long press enters
batch selection, Upload-management covers have no preview, and Search/Book Detail/reader images
require a stationary long press with touch-slop cancellation.

The cache action's visual selected state now maps to the persisted local policy instead of being
permanently selected. It remains a client-presentation setting and never sends a mutating favourite
request.

The current public `GET /api/users/100000/activities` response remains HTTP 404 with
`API endpoint not implemented in Laravel yet.` Native therefore continues to present this as a
recoverable unavailable state rather than fabricating an empty activity feed.

Full local verification passed: 76 Release test suites / 496 tests / 0 failures / 0 errors / 0
skipped, `lintDebug` reports `No issues found`, `git diff --check` passed, and Debug APK SHA-256 is
`33D32011688EC813C201F00DCEE0B31804EFEBE5C5EFF901485C4C364811E636`. `adb devices -l` now shows
no attached device, so no install, app-data, proxy, token, cookie, account, or browser-state change
was made in this turn.

## Turn 176 - Reader mobile computed-style verification

Public read-only capture of `/book/95654/1419162` at `412 x 915` establishes the current mobile
reader's actual computed values:

| Element | Source measurement | Native mapping |
| --- | --- | --- |
| Article body | `16px`, `25.6px` line height, `20px 16px 30px` padding | Existing 16sp, 1.6 line height, 16dp horizontal gutter retained |
| Article rail | `max-width: 800px` | Existing 800dp wide-screen cap retained |
| Chapter separator | 2px top rule, 16px outer margin, 16/8px top/bottom inner rhythm | Native 2dp top rule and matching first-separator space |
| Chapter heading | 20px/28px, bold muted gray | Native 20sp/28sp-equivalent, bold `palette.meta` heading |

The native reader previously rendered a 24sp body-colored heading and placed its divider below the
heading. It now follows the source sequence: top rule, 16dp breathing room, compact gray heading,
then source-derived lower space. This is presentation-only and leaves the reader's existing
continuous chapter window, stationary illustration long-press zoom, comments, TTS, settings,
offline cache, and navigation semantics unchanged.

Evidence: `output/playwright/source-reader-computed-style-mobile-turn176.png`.

Verification: 76 Release test suites / 496 tests / 0 failures / 0 errors / 0 skipped;
`ReaderPresentationTest` 31/31; `lintDebug` `No issues found`; `assembleDebug` passed; Debug APK
SHA-256 `6BB3DF86045091D4E67B459036ED4CADBF2FF78082ADE0C268B509BF8116E37E`; `git diff --check`
passed. `adb devices -l` reports no attached MuMu device, so no runtime install or state mutation
was performed.

## Turn 177 - Current mobile search-default verification

The current read-only mobile source session at `/search?sort_by=relevance` rendered the populated
two-column result feed and recorded this first API request:

```text
GET /api/search?page=1&limit=60&scope=all&match_type=fuzzy_strict&sort_by=relevance&sort_order=desc&adult_filter=unrestricted&max_word_count=10000000
```

The source's current visible Rules rail does not render a content-rating selector in that session,
but the API continues to accept `all`, `adult_only`, and `unrestricted`. Native therefore retains
the restored content-rating control instead of removing the user's adult-only option. Its fresh
and cleared state now correctly defaults to `unrestricted`; a persisted selection is always
preserved unchanged.

Regression proof covers the default in `SearchSettingsStore`, the serialized default request in
`NovalPieApiTest`, and the selected Rules-rail value in `DiscoverPresentationTest`. Full local
verification passed with 76 Release suites / 497 tests / 0 failures / 0 errors / 0 skipped,
`lintDebug` `No issues found`, and Debug APK SHA-256
`3A4FEF3969A56D3995DCFA7F49C4FF81BAEC1C1E574717E3AB99674762F67053`. The known MuMu endpoint
`127.0.0.1:49792` is currently `offline`, so no APK installation or runtime screenshot is claimed.
No browser cookies, token values, account state, emulator state, or source content were copied or
modified.

## Turn 178 - Live review-grid and badge-asset parity

Fresh mobile source evidence for `/forum?tab=review` confirms the review rail is a distinct
comment feed rather than `/api/posts?type=review`:

```text
GET /api/comments/book-reviews?page=1&limit=20&hide_spoilers=1
```

The current response reports `total=22852`, `pages=1143`, and review records with `bookId`,
`bookCover`, `authorAvatarFrame`, structured `authorBadges`, `badge_html`, and `badge_css`.
At the captured mobile width the source lays these cards out two-up. The reader control capture
also records the source's right-side action rail: close, help, catalog, settings, theme, chapter
navigation, scroll/page mode, TTS, fullscreen, and navigation.

Native now uses a restored grid container for the Forum route: `review` is two columns and all
other categories are one column. Header/filter/error/pagination content spans the grid, while
review cards use compact source-style spacing. Static CSS geometry is safely bounded and applied
to custom badge rendering, so fixed artwork sizes are not collapsed into a universal pill; WebP
frame URLs remain ordinary remote image assets.

Regression coverage includes the exact structured review badge/WebP response shape, source review
pagination, and the one-vs-two-column presentation contract. Full verification passed with 76
Release suites / 501 tests / 0 failures / 0 errors / 0 skipped, `lintDebug` reporting `No issues
found`, and Debug APK SHA-256
`60CF05AB8FB826E37C1DB240E6B9EAF2CB52075AC31DFD8CD6370184DAF26324`.

The known MuMu endpoint `127.0.0.1:49792` is still `offline`, so native runtime installation and
visual screenshots remain unverified for this turn.

## Turn 184 - Profile activity-category parity

The source profile ActivityTab exposes five local display categories: `全部`, `帖子`, `评论`,
`书评`, and `章评`. Native now keeps the merged activity feed in memory and applies the same
category mapping without issuing a second request when a chip is selected:

| Source category | Native activity type |
| --- | --- |
| 全部 | all normalized activity records |
| 帖子 | `post` |
| 评论 | `post_comment` |
| 书评 | `novel_comment` |
| 章评 | `chapter_comment` |

The rail is present on both the owner profile and public user profile. A non-empty feed with no
records in the selected category reports `该分类暂无动态`, while a genuinely empty feed retains
`暂无公开动态`; this prevents a populated account from looking blank after switching filters.
The existing canonical `/api/users/{id}/activities` attempt and the three live fallback feeds
remain unchanged, including the `limit=200` first-window contract and aggregate counter merge.

Verification for this turn: 76 Release test suites / 506 tests / 0 failures / 0 errors / 0 skipped;
`lintDebug` reports no issues; `git diff --check` passes. Debug APK SHA-256:
`33FE15FDE80A292C066DD5F5E226D03936256AF70E25D742E033A9A9F90382A2`.

MuMu `127.0.0.1:49792` remains offline, so no native runtime screenshot or APK installation is
claimed. No account, cookie, token, proxy, app-data, or browser-session state was changed.

## 2026-08-17 - Turn 183 - Collection history-action parity

The Collection and owner Upload Books contracts remain source-aligned with the local presentation
enhancements already documented above: persisted `2 / 3 / 4` columns, tallest-sibling row alignment,
complete title/author/tag rendering, disabled management-cover preview, Collection long-press batch
selection, and stationary-long-press-only preview for Search, Book Detail, and reader images.

The final action boundary is now explicit: pin/unpin is rendered only for an unselected Favorites
entry with a valid favourite-record id. History rows no longer expose the Favorites-only pin
mutation. The Grid and List paths share the same predicate and `LibraryPresentationTest` covers the
four relevant states.

Verification passed with 76 Release test suites / 504 tests / 0 failures / 0 errors / 0 skipped,
`lintDebug` reporting `No issues found`, and `git diff --check` passing. Debug APK SHA-256:
`52269A030806A7BED5FFDE6696F2A50A3CDA578A7CA9B412A7A273246F967E90`.

## 2026-08-17 - Turn 185 - live mobile source and independent reader/detail loading

Fresh public read-only Playwright inspection used a separate session with the viewport configured
to `412 x 915`; no user browser profile, cookies, credentials, or storage were loaded. The current
source contracts observed were:

- Search `/search?sort_by=relevance` opens with populated results and the compact Rules rail;
- Forum Reviews `/forum?tab=review` requests
  `GET /api/comments/book-reviews?page=1&limit=20&hide_spoilers=1` and renders structured
  reviewer badges, book covers, counts, and pagination;
- Book Detail `/book/76570` requests
  `GET /api/novels/76570/detail`, `GET /api/v2/novels/76570/chapters`, and
  `GET /api/comments?type=book&book_id=76570&page=1&limit=30`;
- Reader `/book/76570/5988757` requests the short-lived reader session endpoint and then chapter
  content with `replace_mode=india` and `show_images=1`, while adjacent chapters and chapter
  comment metadata load independently.

Evidence screenshots are retained at:

- `output/playwright/source-search-mobile-turn184.png`;
- `output/playwright/source-forum-review-mobile-turn185.png`;
- `output/playwright/source-book-detail-mobile-turn185.png`;
- `output/playwright/source-reader-mobile-turn185.png`.

Native loading was hardened around the source's independent request model. Reader正文/本地缓存
now publishes before the directory, chapter comments, or favourite status finishes; those panels
update independently with request-serial and route freshness guards. Book Detail likewise publishes
the core book immediately and updates chapters, book reviews, favourite state, management
permissions, and the full-resolution cover independently. A slow or failed auxiliary endpoint no
longer leaves the main reading surface or review section behind a global spinner.

Verification: 76 Release test suites / 507 tests / 0 failures / 0 errors / 0 skipped;
`lintDebug` reports `No issues found`; `git diff --check` passes. Debug APK SHA-256:
`0F513ECB504990D6FC80633C6431F0CF84A001661A3D322E30E9F3674832331B`.

MuMu `127.0.0.1:49792` remains `offline`; a scoped ADB reconnect did not restore transport, so no
APK installation or native runtime screenshot is claimed.
