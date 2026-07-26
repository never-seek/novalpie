# NovalPie Native — API Surface & Data Model Inventory

Scope of this document:

- `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt` (3404 lines, read in full)
- `D:/NovalPie/native-android/app/src/main/java/com/novalpie/nativeapp/model/Models.kt` (655 lines, read in full)
- Supporting reads for plumbing/wiring: `data/UploadFileSource.kt`, `data/NetworkConfigStore.kt`,
  `ui/ApiMessages.kt`, `ui/NovalPieViewModel.kt:438-448`
- Cross-check for dead code: every `api.*(` call site in `app/src/main`, plus `app/src/test`

Every claim below is anchored to `file:line`. Chinese literals are quoted verbatim (source uses a mix of
literal CJK and `\uXXXX` escapes; escapes are decoded and both forms noted).

---

## 0. Class shape and construction

`NovalPieApi.kt:95-102`:

```kotlin
class NovalPieApi(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://novalpie.cc",
    private val cookieProvider: () -> String? = { null },
    private val authTokenProvider: () -> String? = { null },
    private val proxyProvider: () -> Proxy? = { null },
    private val proxySelectorProvider: () -> ProxySelector? = { null }
)
```

- Single class, 106 public `suspend` functions, all `= withContext(Dispatchers.IO) { ... }` except
  `messages()` (`NovalPieApi.kt:765`) which is a pure delegating wrapper with no `withContext`.
- Closing brace `NovalPieApi.kt:3353`. Two file-private `RequestBody` subclasses follow:
  `UploadStreamRequestBody` (`3355-3371`) and `UploadRangeRequestBody` (`3373-3404`).
- Production wiring (`ui/NovalPieViewModel.kt:438-448`):
  - `cookieProvider` = `CookieManager.getInstance().getCookie("https://novalpie.cc")` wrapped in `runCatching`
    (shares the WebView cookie jar with the native client — this is how login persists).
  - `authTokenProvider` = `{ authToken }` (ViewModel state, loaded from `AuthSessionStore`, `NovalPieViewModel.kt:435`).
  - `proxySelectorProvider` = `proxySettings.toProxySelector(preferEmulatorProxy = shouldPreferEmulatorProxy())`.
  - `baseUrl` and `proxyProvider` are left at defaults in production; tests override `baseUrl` with a MockWebServer URL.

### Companion constants (`NovalPieApi.kt:3334-3351`)

| Name | Value | Line |
|---|---|---|
| `WEBSITE_UPLOAD_CHUNK_BYTES` | `5 * 1024 * 1024` (public const) | 3335 |
| `WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES` | `20 * 1024 * 1024` (public const) | 3336 |
| `USER_AGENT` | `"NovalPieNative/2.0 Android"` | 3337 |
| `READER_SIGNATURE_SECRET` | `"X9f2m8Q5zL1p4R7t0Y3u6W2s5V8x1B4n7M0k3J6h9G2d5F8c1A4b7E0r3T6y9U2i"` | 3338-3339 |
| `STANDARD_BASE64_ALPHABET` | `"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"` | 3340-3341 |
| `READER_BASE64_ALPHABET` | `"M9N8B7V6C5X4Z3L2K1J0HGFDSAPOIUYTREWQmnbvcxzlkjhgfdsaqwertyuiop+/"` | 3342-3343 |
| `READER_NONCE_ALPHABET` | `"abcdefghijklmnopqrstuvwxyz0123456789"` | 3344 |
| `secureRandom` | `SecureRandom()` (companion-level singleton) | 3345 |

Note: `WEBSITE_UPLOAD_CHUNK_BYTES` is duplicated as a top-level `Long` in `ui/UploadPresentation.kt:3`
(`5L * 1024L * 1024L`). Two independent definitions of the same constant — both must stay in sync.

---

## 1. Request plumbing

### 1.1 `defaultClient()` — `NovalPieApi.kt:3347-3351`

```kotlin
OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS)
    .build()
```

No write timeout is configured (OkHttp default 10s applies) — relevant for large chunked uploads.
No interceptors, no cookie jar, no retry configuration, no logging.

### 1.2 Verb helpers

| Helper | Line | Behaviour |
|---|---|---|
| `get(path, params = emptyMap(), headers = emptyMap()): Any` | 1553-1559 | delegates to `request(method = "GET")` |
| `post(path, body: JSONObject): Any` | 1561-1563 | `request(method = "POST")` |
| `put(path, body: JSONObject): Any` | 1565-1567 | `request(method = "PUT")` |
| `patch(path, body: JSONObject): Any` | 1569-1571 | `request(method = "PATCH")` |
| `delete(path, body: JSONObject? = null): Any` | 1573-1575 | `request(method = "DELETE")` |
| `requestBody(path, method, body: RequestBody): Any` | 1577-1584 | multipart/streaming path. `"PUT"` → `.put(body)`, **anything else** → `.post(body)` |
| `request(path, params, method, body, headers): Any` | 1586-1629 | URL + verb assembly, see below |
| `baseRequestBuilder(pathOrUrl): Request.Builder` | 1631-1641 | URL + static headers |
| `execute(path, requestBuilder): Any` | 1643-1668 | auth headers, proxy selection, status check, body parse |
| `executeExternal(label, requestBuilder): Any` | 1670-1684 | same but **no cookie/auth headers**, different error label |

### 1.3 `request()` details — `NovalPieApi.kt:1586-1629`

1. URL: `(baseUrl.trimEnd('/') + path).toHttpUrl().newBuilder()` — note `toHttpUrl()` (throwing variant).
   Because the whole string is parsed, a `path` that already contains a query string works
   (e.g. `delete("/api/admin/key-management?id=$id")`, line 618).
2. Query params: `params.forEach { if (value.isNotBlank()) builder.addQueryParameter(key, value) }` (1594-1596).
   **Blank values are silently dropped** — this is why call sites can pass `"type" to ""` unconditionally.
3. Extra headers: applied via `requestBuilder.header(k, v)`, blank values skipped (1600-1602).
4. Body:
   - `POST`/`PUT`/`PATCH` (1605-1614): payload = `(body ?: JSONObject()).toString()`;
     media type `"application/json; charset=utf-8"`; sets header `content-type: application/json`.
     A null body becomes `{}` — never an empty body.
   - `DELETE` (1615-1624): if `body == null` → bodyless `.delete()`; else `content-type: application/json`
     plus a JSON DELETE body.
   - default (1625) → `.get()`.

### 1.4 `baseRequestBuilder()` — `NovalPieApi.kt:1631-1641`

- If `pathOrUrl` starts with `http://` or `https://` it is used verbatim; otherwise
  `baseUrl.trimEnd('/') + pathOrUrl`. (This is what lets `generateEditorRegex` reuse it for a third-party host.)
- Always sets: `accept: application/json`, `user-agent: NovalPieNative/2.0 Android`.

### 1.5 `execute()` — `NovalPieApi.kt:1643-1668`

Order of operations:

1. `cookieProvider()?.takeIf { it.isNotBlank() }` → header `cookie: <value>` (1644-1646).
2. `authTokenProvider()?.takeIf { it.isNotBlank() }` → header `authorization: Bearer <token>` (1647-1649).
3. Proxy selection (1653-1659):
   - `explicitProxy = proxyProvider()`; if non-null → `client.newBuilder().proxy(explicitProxy).build()`.
   - else `proxySelector = proxySelectorProvider()`; if non-null → `client.newBuilder().proxySelector(...).build()`.
   - else the shared `client`.
   - **A new `OkHttpClient` is built per call whenever a proxy or selector is present** (connection-pool
     churn; the pool is inherited via `newBuilder()` so it is not catastrophic, but it is per-request work).
4. `callClient.newCall(request).execute().use { ... }`:
   - `responseBody = response.body?.string().orEmpty()` is read **before** the success check (1662).
   - `if (!response.isSuccessful) throw IOException("NovalPie API ${response.code}: $path")` (1663-1665).
     The response body of an error is read and discarded — server error messages are lost here.
   - success → `parseJsonOrString(responseBody)`.

### 1.6 `executeExternal()` — `NovalPieApi.kt:1670-1684`

Identical proxy logic, but: no `cookie`, no `authorization` header injected, and the error is
`IOException("External API ${response.code}: $label")`. Only caller: `generateEditorRegex` (1231).

### 1.7 `parseJsonOrString()` — `NovalPieApi.kt:1741-1749`

- empty/blank body → `JSONObject()` (empty object, **not** an error)
- starts with `{` → `JSONObject(trimmed)`
- starts with `[` → `JSONArray(trimmed)`
- otherwise → the trimmed `String`

Every normalizer therefore takes `raw: Any` and must tolerate `JSONObject | JSONArray | String`.
`normalizeReaderContent` (2033-2036) explicitly uses the `String` case (`source = "plain"`).

### 1.8 Error mapping (status code → exception → user message)

| Layer | Location | Behaviour |
|---|---|---|
| HTTP non-2xx (site) | `NovalPieApi.kt:1664` | `IOException("NovalPie API <code>: <path>")` |
| HTTP non-2xx (external AI) | `NovalPieApi.kt:1681` | `IOException("External API <code>: <label>")` |
| Argument validation | `require(...)` throughout | `IllegalArgumentException` with the messages listed in §3.9 |
| Semantic failures | e.g. 1437, 1523, 1539, 1541, 1713, 1754, 1767, 1770, 1792-1794, 2027-2029, 2042-2048, 2057, 1232, 1237, 1245, 296, 790 | `IOException` with a specific message |
| UI mapping | `ui/ApiMessages.kt:3-14` | `apiFailureMessage(label, throwable)` → `"${label}请求失败: ${detail}"`; a detail matching `Regex("""NovalPie API (\d+)""")` is rewritten to `"服务返回错误 <code>"` (i.e. the raw path is hidden from users) |

There is **no** per-status-code branching (no 401→re-login, no 429→backoff, no 5xx→retry) anywhere in the
API layer. Retry/refresh policy lives in the ViewModel/`ui/ErrorRecovery.kt`, not here.

---

## 2. Complete public suspend function table (106 functions)

Conventions used in the table:
- **Path** is the literal passed to the verb helper; `{x}` marks Kotlin string interpolation.
- **Query** lists `params` map entries (blank values are dropped by `request()`, §1.3).
- **Body** lists JSON keys, or multipart part names for `requestBody()` calls.
- `→` names the normalizer (see §5) or "inline" when the model is built at the call site.

### 2.1 Search / discovery / tags

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 1 | `search(keyword, page=1, limit=20, sortBy="relevance", sortOrder="desc", scope="all", matchType="ai", adultFilter="all", source="", minWordCount=null, maxWordCount=null)` (103) | GET | `/api/search` | `q`=keyword.trim(), `page`, `limit`, `sort_by`, `sort_order`, `scope`, `match_type`, `adult_filter`, `source`; `min_word_count` / `max_word_count` added only when `> 0` (128-129) | `List<NovelCard>` → `normalizeNovelList` |
| 2 | `tags(sort="count", limit=24)` (753) | GET | `/api/tags` | `sort`, `limit` | `List<NovelTag>` → `normalizeNovelTags` |

### 2.2 Users / profile / check-in

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 3 | `currentUser()` (137) | GET | `/api/users/me` | — | `UserProfile` → `normalizeUser` |
| 4 | `userProfile(userId)` (141) | GET | `/api/users/{userId}` | `require(userId > 0) { "userId must be positive" }` | `UserProfile` → `normalizeUser` |
| 5 | `userActivities(userId=null, type="", page=1, limit=100)` (146) | GET | `/api/users/{id}/activities` if `userId > 0` else `/api/users/me/activities` (152-153) | `type`, `page`=coerceAtLeast(1), `limit`=coerceIn(1,100) | `List<UserActivity>` → `normalizeUserActivities` |
| 6 | `userNovels(userId=null)` (166) | GET | `/api/users/{id}/novels` else `/api/users/me/novels` | — | `List<NovelCard>` → `normalizeNovelList` |
| 7 | `userCheckinRecords(userId=null, startDate, endDate)` (298) | GET | `/api/users/{id}/checkins` else `/api/users/me/checkins` | `start_date`, `end_date` | `List<UserCheckinRecord>` → `normalizeUserCheckinRecords` |
| 8 | `userCheckinSettings(userId=null)` (310) | GET | `/api/users/{id}/checkins/settings` else `/api/users/me/checkins/settings` | `user_id` only for the public form (315) | `UserCheckinSettings` inline (unwrap keys `checkin_settings`,`settings`,`data`,`result`; `show_checkin`/`showCheckin` default `true`, `auto_checkin`/`autoCheckin` default `false`) |
| 9 | `userCheckinStats(userId=null)` (726) | GET | `/api/users/{id}/checkins/stats` else `/api/users/me/checkins/stats` | `user_id` only for the public form | `UserCheckinStats` inline (`total_days`, `total_points`, `max_streak`, `current_streak`, all default 0) |
| 10 | `currentUserCheckinStats()` (722) | — | delegates to `userCheckinStats()` | — | `UserCheckinStats` |
| 11 | `checkinCurrentUser()` (740) | POST | `/api/users/me/checkins` | body `{}` | `UserCheckinAction` inline (`success` from `success`/`ok` default `true`; `message` from `message`/`msg`/`detail`; `points` from `points`/`point`) |
| 12 | `updateCurrentUser(profile)` (692) | PATCH | `/api/users/me` | `username`=profile.name, `bio`=profile.bio.orEmpty(), `show_checkin` and `auto_checkin` only when non-null (696-697) | `UserProfile` — **returns the input `profile` unchanged; the HTTP response is discarded** (698-699) |
| 13 | `updateCurrentUserCheckinSettings(showCheckin, autoCheckin)` (702) | PATCH | `/api/users/me/checkins/settings` | `show_checkin`, `auto_checkin` | `UserCheckinAction` inline — **DEAD, see §7** |
| 14 | `verifyCurrentUserAdult(birthYear)` (323) | POST | `/api/users/me/verifies/adult` | `birth_year`; `require(birthYear in 1900..2100) { "birthYear is out of range" }` | `UserCheckinAction` inline |
| 15 | `uploadCurrentUserAvatar(file: UploadFileSource)` (336) | POST (multipart) | `/api/users/me/avatar` | part `avatar` = `UploadRangeRequestBody(file, offset=0, length=sizeBytes)`; `require(file.sizeBytes > 0L) { "avatar file is empty" }` | `UserCheckinAction` inline |

### 2.3 Books / chapters (read side)

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 16 | `bookDetail(bookId)` (1171) | GET | `/api/novels/{bookId}/detail` | — | `NovelCard` → `normalizeBook` |
| 17 | `bookCoverPhoto(bookId, favoriteType="novel")` (1175) | GET | `/api/novels/{bookId}/photo` | `favorite_type` | `String?` — unwrap `photo`,`novel`,`data`,`result`; then `normalizeAssetUrl(firstStringOrNull("photo_true_url","photoTrueUrl","full_cover_url","fullCoverUrl","original_cover_url","originalCoverUrl","photo_url","photoUrl"))` (1181-1193) |
| 18 | `chapters(bookId)` (1277) | GET | `/api/novels/{bookId}/chapters` | — | `List<Chapter>` → `normalizeChapters` |
| 19 | `chapterContent(chapterId)` (1281) | GET ×2 | `readerSessionKey()` → `GET /api/reader/session-key`, then `GET /api/chapters/{chapterId}/content` | `session`=sessionId, `replace_mode`=`"india"`, `show_images`=`"1"` | `ReaderContent` → `normalizeReaderContent(raw, readerSessionKey = session.sessionKey)` |

### 2.4 Favorites

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 20 | `favorites(page=1, limit=20, groupId=null)` (1003) | GET | `/api/favorites` | `page`, `limit`, `sort_field`=`"updated_at"`, `sort_order`=`"desc"`, `type`=`"novel"`, `group_id` when non-null | `List<NovelCard>` → `normalizeNovelList` |
| 21 | `favoriteGroups()` (749) | GET | `/api/favorites/groups` | `preview_limit`=`"6"`, `with_preview`=`"true"` | `List<FavoriteGroup>` → `normalizeFavoriteGroups` |
| 22 | `favoriteStatus(bookId)` (1159) | GET | `/api/favorites/status` | `object_id`=bookId, `type`=`"novel"` | `FavoriteStatus` → `normalizeFavoriteStatus` |

Note: there is **no** add/remove-favorite endpoint in the API layer. Favoriting is read-only here
(the ViewModel routes mutation through the WebView fallback).

### 2.5 Managed books (author/owner editing)

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 23 | `managedBookInfo(bookId)` (172) | GET | `/api/novels/{bookId}/detail` | `require(bookId > 0)` | `BookEditInfo` inline — see §5.24 |
| 24 | `managedBookPermissions(bookId)` (192) | GET | `/api/users/me/novels/{bookId}/permissions/check` | `require(bookId > 0)` | `BookEditPermissions` inline — unwrap `permissions`,`data`,`result`; booleans from `title`, (`true_name`\|`title_translation`), `author_name`, `description`, `source`, `source_url`, `language`, `is_adult`, `photo_url`, `spans`, `tags`, each default `false` (200-212) |
| 25 | `updateManagedBook(bookId, request: BookEditRequest)` (215) | PATCH | `/api/users/me/novels/{bookId}` | see §2.5.1 | `BookEditResult` inline |
| 26 | `transferManagedBook(bookId, identifier)` (242) | POST | `/api/users/me/novels/{bookId}/transfers` | `identifier`=identifier.trim(); requires bookId>0 and non-blank target | `ManagedBookTransferResult` → `normalizeManagedBookTransfer` |
| 27 | `updateManagedBookAccessPolicy(bookId, policy: ManagedBookAccessPolicy)` (254) | PATCH | `/api/users/me/novels/{bookId}/permissions` | `allow_download` (1/0), `download_threshold_type`, `download_threshold_value`, `read_threshold_type`, `read_threshold_value`; when `!allowDownload` the download pair is forced to `"none" to 0` (259-263) | `ForumActionResult` → `normalizeForumActionResult` |
| 28 | `uploadManagedBookCover(bookId, file)` (278) | PUT (multipart) | `/api/novels/{bookId}/photo` | part `cover` = `UploadRangeRequestBody(file, 0, sizeBytes)`; `require(bookId > 0)`, `require(file.sizeBytes > 0L) { "cover file is empty" }` | `String` — `normalizeAssetUrl(firstStringOrNull("photo_url","photoUrl","url"))`, else `IOException("cover upload did not return photo_url")` (294-296) |

#### 2.5.1 `updateManagedBook` body (`NovalPieApi.kt:216-233`)

Preconditions: `require(bookId > 0)`, `require(request.title.trim().isNotBlank()) { "book title is required" }`,
`require(request.authorName.trim().isNotBlank()) { "book author is required" }`.

`spans` derivation (219-220):
```kotlin
var spans = request.status.trim().ifBlank { "连载中" }
if (request.isAdult && !spans.contains("19")) spans = "19 $spans"
```

Body keys: `title`, `title_translation` (trimmed, `JSONObject.NULL` if blank), `author_name`,
`description` (nullable), `source` (nullable), `source_url` (nullable), `language`
(`ifBlank { "zh" }`), `spans`, `is_adult` (`1`/`0` int), `photo_url` (nullable),
`tags` (JSONArray of trimmed, non-blank, `distinct()`).

Response → `BookEditResult` (unwrap `data`,`result`): `success` from `success`/`ok` default `true`;
`message` from `message`/`msg`/`detail`; `failed_fields` array → `failedFields`; `errors` array → `errors`
(234-239).

### 2.6 Managed chapters

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 29 | `managedChapterIllustrations(chapterId)` (1296) | GET | `/api/users/me/chapters/{chapterId}/illustrations` | `require(chapterId > 0) { "chapter id is required" }` | `ChapterIllustrationPage` → `normalizeChapterIllustrations` |
| 30 | `uploadManagedChapterIllustrations(chapterId, files: List<UploadFileSource>)` (1301) | POST (multipart) | `/api/users/me/chapters/{chapterId}/illustrations` | part `chapter_id`; then one `illustrations[]` part per file using `UploadStreamRequestBody`. Validation per file: `sizeBytes in 1..WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES` → `"illustration file must be between 1 byte and 20 MiB"`; `contentType?.startsWith("image/") == true` → `"illustration file must be an image"` (1307-1312) | `ChapterIllustrationMutationResult` → `normalizeChapterIllustrationMutation` |
| 31 | `deleteManagedChapterIllustration(chapterId, imageId)` (1328) | DELETE (no body) | `/api/users/me/chapters/{chapterId}/illustrations/{imageId}` | `require(chapterId > 0 && imageId > 0)` | `ChapterIllustrationMutationResult` |
| 32 | `reorderManagedChapters(bookId, orderedChapterIds)` (1338) | POST | `/api/users/me/chapters/reorder` | `novel_id`, `ordered_chapter_ids` (JSONArray) | `ForumActionResult` |
| 33 | `insertManagedChapter(bookId, insertAt, title, content)` (1350) | POST | `/api/users/me/chapters/insert` | `novel_id`, `insert_at`, `title` (trim), `content` (trim); `require(bookId > 0 && insertAt >= 1)` | `ForumActionResult` |
| 34 | `updateManagedChapter(chapterId, title, content)` (1365) | PATCH | `/api/users/me/chapters/{chapterId}` | `title` (trim), `content` (trim) | `ForumActionResult` |
| 35 | `deleteManagedChapter(chapterId)` (1376) | DELETE (no body) | `/api/users/me/chapters/{chapterId}` | `require(chapterId > 0)` | `ForumActionResult` |
| 36 | `batchDeleteManagedChapters(bookId, chapterIds)` (1381) | POST | `/api/users/me/chapters/batch-delete` | `novel_id`, `chapter_ids` (JSONArray) | `ForumActionResult` |
| 37 | `requestManagedChapterTranslation(bookId, chapterIds, mode)` (1391) | POST | `/api/users/me/novels/{bookId}/translation-requests` | `chapter_ids` (JSONArray), `mode`; `require(mode in setOf("personal","shared")) { "translation mode is invalid" }` | `ForumActionResult` |
| 38 | `appendManagedChapters(bookId, submitType, chapters, epubFilePath=null, epubFile=null)` (1406) | POST (multipart, possibly N times) | `/api/users/me/chapters/append` | see §4.2 | `UploadActionResult` |

### 2.7 Comments (book / chapter / forum)

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 39 | `bookComments(bookId, page=1, limit=20)` (1443) | GET | `/api/comments` | `type`=`"book"`, `book_id`, `page`, `limit` | `List<ChapterComment>` → `normalizeChapterComments` |
| 40 | `chapterComments(bookId=null, chapterId, page=1, limit=20)` (1457) | GET | `/api/comments` | `type`=`"chapter"`, `chapter_id`, `page`, `limit`, plus `book_id` when `bookId > 0` (1464) | `List<ChapterComment>` → `normalizeChapterComments` |
| 41 | `createBookComment(bookId, content)` (1117) | POST | `/api/comments` | `type`=`"book"`, `book_id`, `content` | `ForumActionResult` |
| 42 | `createChapterComment(bookId, chapterId, content)` (1125) | POST | `/api/comments` | `type`=`"chapter"`, `book_id`, `chapter_id`, `content` | `ForumActionResult` |
| 43 | `createCommentReply(commentId, content, replyToName=null)` (1134) | POST | `/api/comments/{commentId}/replies` | `content`, `reply_to_name` when non-blank | `ForumActionResult` |
| 44 | `toggleCommentLike(commentId)` (1141) | POST | `/api/comments/{commentId}/likes` | body `{}` | `ForumActionResult` |
| 45 | `reactToComment(commentId, reactionType, awardPoints=null)` (1145) | POST | `/api/comments/{commentId}/reactions` | `reaction_type`, `award_points` when non-null | `ForumActionResult` |
| 46 | `reactToCommentReply(parentCommentId, replyId, reactionType, awardPoints=null)` (1152) | POST | `/api/comments/{parentCommentId}/replies/{replyId}/reactions` | `reaction_type`, `award_points` when non-null | `ForumActionResult` |
| 47 | `toggleForumCommentLike(commentId)` (1106) | POST | `/api/comments/{commentId}/likes` | body `{}` | `ForumActionResult` — **byte-identical to #44** |
| 48 | `reactToForumComment(commentId, reactionType, awardPoints=null)` (1110) | POST | `/api/comments/{commentId}/reactions` | `reaction_type`, `award_points` | `ForumActionResult` — **byte-identical to #45** |

### 2.8 Forum posts

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 49 | `forumPosts(page=1, limit=20, type="all")` (1020) | GET | `/api/posts` | `page`, `limit`; `type` added only when non-blank **and** `!= "all"` (1025) | `List<ForumPost>` → `normalizeForumPosts` |
| 50 | `forumPostDetail(postId)` (1029) | GET | `/api/posts/{postId}` | — | `ForumPostDetail` → `normalizeForumPostDetail` |
| 51 | `createForumPost(request: ForumCreateRequest)` (1033) | POST | `/api/posts` | see §2.8.1 | `ForumCreateResult` → `normalizeForumCreateResult` |
| 52 | `forumPostComments(postId, page=1, limit=20)` (1070) | GET | `/api/posts/{postId}/comments` | `page`, `limit` | `List<ForumComment>` → `normalizeForumComments` |
| 53 | `createForumComment(postId, content, parentCommentId=null, replyToName=null)` (1082) | POST | `/api/posts/{postId}/comments` | `content`; `comment_id` when parentCommentId non-null; `reply_to_name` when non-blank | `ForumActionResult` |
| 54 | `toggleForumPostLike(postId)` (1095) | POST | `/api/posts/{postId}/likes` | body `{}` | `ForumActionResult` |
| 55 | `reactToForumPost(postId, reactionType, awardPoints=null)` (1099) | POST | `/api/posts/{postId}/reactions` | `reaction_type`, `award_points` when non-null | `ForumActionResult` |

#### 2.8.1 `createForumPost` validation and body (`NovalPieApi.kt:1034-1067`)

Normalization first: `type`/`title`/`content` trimmed; `tags` = trimmed, non-blank, `distinct()`.

Validations (all `IllegalArgumentException` on failure):
- `type in setOf("recommend", "discussion", "feedback", "announcement")` → `"unsupported forum type"`
- `title.isNotBlank() && title.length <= 100` → `"forum title must contain 1 to 100 characters"`
- `content.isNotBlank() && content.length <= 10_000` → `"forum content must contain 1 to 10000 characters"`
- `tags.size <= 5 && tags.all { it.length <= 20 }` → `"forum tags exceed website limits"`
- if a poll is present: `options.size in 2..10 && options.distinct().size == options.size`
  → `"forum poll must contain 2 to 10 unique options"`

Body: `type`, `title`, `content`, `tags` (JSONArray). Optional `poll` object with camelCase keys
(**note the inconsistency: post keys are snake_case, poll keys are camelCase**):
`options` (JSONArray), `allowMultiple`, `maxChoices` (= `poll.maxChoices.coerceIn(2, options.size)`
when `allowMultiple`, else `1`), `endsAt` (trimmed or `JSONObject.NULL`), and `question` only when non-blank.

### 2.9 Messages / notifications / DMs

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 56 | `messages(page=1, pageSize=20)` (765) | — | wrapper → `messagePage(MessageQuery(), page, pageSize).items` | — | `List<SiteMessage>` |
| 57 | `messagePage(query: MessageQuery, page=1, pageSize=20)` (768) | GET | `/api/messages` | `page`=coerceAtLeast(1), `page_size`=coerceAtLeast(1); `message_type`, `is_read`, `priority` added only when non-null; `keyword` only when trimmed non-empty (777-780) | `MessagePage` → `normalizeMessagePage(raw, requestedPage, requestedPageSize)` |
| 58 | `messageDetail(messageId)` (788) | GET | `/api/messages/{messageId}` | unwrap `message`,`data`,`result` | `SiteMessage` → `normalizeMessage`, else `IOException("NovalPie message detail is missing: $messageId")` (790) |
| 59 | `messageStats()` (793) | GET | `/api/messages/stats` | — | `MessageStats` → `normalizeMessageStats` |
| 60 | `markMessageRead(messageId)` (911) | POST | `/api/messages/{messageId}/read` | `id`=messageId | `MessageActionResult` |
| 61 | `markMessagesRead(messageIds)` (917) | POST | `/api/messages/read` | `ids` (JSONArray) | `MessageActionResult` |
| 62 | `markAllMessagesRead()` (923) | POST | `/api/messages/read` | `all`=`true` | `MessageActionResult` |
| 63 | `starMessage(messageId, starred)` (929) | POST | `/api/messages/{messageId}/star` | `starred` = `1`/`0` int | `MessageActionResult` |
| 64 | `deleteMessage(messageId, permanent=false)` (935) | DELETE (with body) | `/api/messages/{messageId}` | `id`, `permanent` (boolean) | `MessageActionResult` |
| 65 | `deleteMessages(messageIds)` (949) | DELETE (with body) | `/api/messages` | `ids` (JSONArray) | `MessageActionResult` |
| 66 | `messageSettings()` (955) | GET | `/api/messages/settings` | — | `MessageSettings` → `normalizeMessageSettings` |
| 67 | `updateMessageSettings(settings)` (959) | PUT | `/api/messages/settings` | `enable_notifications`, `enable_email`, `enable_browser_push`; optional `notification_types` (JSONArray of Int), `quiet_hours_start`, `quiet_hours_end`, `auto_read_after_days` (964-967) | `MessageActionResult` |
| 68 | `messageConversation(targetUserId, page=1, pageSize=100)` (971) | GET | `/api/messages/conversations` | `target_user_id`, `page`, `page_size` | `List<DirectMessage>` → `normalizeDirectMessages` |
| 69 | `sendDirectMessage(currentUserId, targetUserId, currentUserName, content)` (988) | POST | `/api/messages` | `user_id`=targetUserId, `execute_user_id`=currentUserId, `message_type`=`8`, `message_title`=`"来自 {name} 的私信"` (source: `"\u6765\u81ea ${currentUserName.trim()} \u7684\u79c1\u4fe1"`, line 998), `message_content`=content.trim() | `MessageActionResult` |

### 2.10 Workspace (translation infrastructure)

Note: workspace endpoints are **not** under `/api` — they are `/workspace/...`.

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 70 | `workspaceApiConfigs()` (797) | GET | `/workspace/apis` | — | `List<WorkspaceApiConfig>` → `normalizeWorkspaceApiConfigs` |
| 71 | `workspaceCookieStatus()` (801) | GET | `/workspace/cookie-status` | — | `WorkspaceCookieStatus` inline (`hasCookie` from `hasCookie`/`has_cookie`, default `false`) |
| 72 | `workspaceCookieConfigs()` (808) | GET | `/workspace/cookie-config` | — | `WorkspaceCookieConfigs` → `normalizeWorkspaceCookieConfigs` |
| 73 | `workspaceHealth()` (812) | GET ×2 | `/workspace/stats` then `/workspace/translator-health` (sequential, not parallel) | — | `WorkspaceHealth(apiStatus, translators)` → `normalizeWorkspaceApiStatus` + `normalizeWorkspaceTranslators` |
| 74 | `createWorkspaceApi(name, model, endpoint, apiKey, concurrency)` (818) | POST | `/workspace/apis` | `name`, `model`, `endpoint` (all trimmed), `key`=apiKey.trim() (**not** `api_key`), `concurrency`=coerceAtLeast(1) | `WorkspaceActionResult` |
| 75 | `updateWorkspaceApi(id, name, model, endpoint, apiKey, concurrency)` (838) | PUT | `/workspace/apis/{id}` | same as #74 | `WorkspaceActionResult` |
| 76 | `deleteWorkspaceApi(id)` (859) | DELETE (no body) | `/workspace/apis/{id}` | — | `WorkspaceActionResult` |
| 77 | `createWorkspaceCookie(configKey, description, cookieRaw, proxyIp, isActive)` (863) | POST | `/workspace/cookie-config` | `config_key`, `cookie_raw`, `is_active`; optional `description`, `proxy_ip` (only when trimmed non-empty) | `WorkspaceActionResult` |
| 78 | `updateWorkspaceCookie(id, description, cookieRaw, proxyIp, isActive)` (879) | PUT | `/workspace/cookie-config` | `id`, `is_active`; `description` / `cookie_raw` / `proxy_ip` added when the parameter is non-null (trimmed — blank IS sent, unlike create) | `WorkspaceActionResult` |
| 79 | `setWorkspaceCookieActive(id, isActive)` (895) | PUT | `/workspace/cookie-config` | `id`, `is_active` | `WorkspaceActionResult` |
| 80 | `deleteWorkspaceCookie(id)` (905) | DELETE (with body) | `/workspace/cookie-config` | `id` | `WorkspaceActionResult` |

### 2.11 Admin

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 81 | `adminOverview(days=5)` (357) | GET | `/api/admin/overview` | `days`=coerceIn(1,90) | `AdminOverviewStats` inline (unwrap `stats`,`data`,`result`; keys `pending_review_total`, `pending_review_upload`, `pending_review_delete`, `novel_active_total`→`activeNovelTotal`, `user_registered_total`→`registeredUserTotal`, `recent_user_daily[]` of `{date|day, count}`) |
| 82 | `adminReviewSettings()` (381) | GET | `/api/admin/review-settings` | — | `AdminReviewSettings` inline (`auto_approve_upload`/`autoApproveUpload`, `auto_approve_delete`/`autoApproveDelete`, both default `false`) |
| 83 | `adminReviewRequests(type="", status="", keyword="")` (389) | GET | `/api/admin/review-requests` | `page`=`"1"`, `page_size`=`"100"`, `type`, `status`, `q`=keyword | `List<AdminReviewRequest>` inline (array keys `list`,`items`,`data`,`requests`) — aliases in §5.30 |
| 84 | `adminKeys()` (425) | GET | `/api/admin/key-management` | — | `List<AdminKeyItem>` inline (array keys `data`,`items`,`keys`,`list`) |
| 85 | `adminOperationLogs(page=1, action="", status="", userId="", novelId="", keyword="", startDate="", endDate="")` (440) | GET | `/api/admin/operation-logs` | `page`=coerceAtLeast(1), `page_size`=`"20"`, `action`, `status`, `user_id`, `novel_id`, `keyword`, `start_date`, `end_date` | `AdminOperationLogPage` inline (array keys `logs`,`items`,`data`,`list`; page meta from `total`, `total_pages`/`totalPages` default 1, `action_types[]`) |
| 86 | `adminCookieConfigs()` (486) | GET | `/api/admin/cookie-config` | — | `List<AdminCookieConfig>` inline (array keys `configs`,`items`,`data`,`list`) |
| 87 | `adminBaseUrlRules()` (500) | GET | `/api/admin/baseurl-rules` | — | `List<AdminBaseUrlRule>` inline (array keys `data`,`rules`,`items`,`list`) |
| 88 | `adminSchedulerLogs(lines=100)` (512) | GET | `/api/admin/scheduler-logs` | `lines`=coerceIn(10,1000) | `AdminSchedulerLogs` inline (`logs[]`, `total_lines`, `file_size_mb` parsed from Number **or** String, `last_modified`/`lastModified`) |
| 89 | `adminShopItems(type="", active=null, keyword="")` (532) | GET | `/api/admin/shop/items` | `type`, `is_active`=`active?.toString() ?: ""`, `keyword`, `page`=`"1"`, `page_size`=`"100"` | `List<AdminShopItem>` inline (array keys `items`,`data`,`list`) |
| 90 | `adminUpdateReviewSettings(autoApproveUpload, autoApproveDelete)` (567) | POST | `/api/admin/review-settings` | `auto_approve_upload`, `auto_approve_delete` (booleans) | `UserCheckinAction` → `normalizeAdminAction` |
| 91 | `adminReviewAction(id, action)` (581) | POST | `/api/admin/review-requests` | `id`, `action`; `require(id > 0) { "review id must be positive" }`, `require(action in setOf("approve","reject")) { "unsupported review action" }` | `UserCheckinAction` → `normalizeAdminAction` |
| 92 | `adminApproveAllReviews(type="", status="", keyword="")` (592) | POST | `/api/admin/review-requests` | `action`=`"approve_all"`; `type`, `status`, `q` only when non-blank | `UserCheckinAction` — **DEAD, see §7** |
| 93 | `adminUpdateKeyStatus(id, approvalStatus)` (604) | PUT | `/api/admin/key-management` | `id`, `approval_status`; `require(id > 0) { "key id must be positive" }`, `require(approvalStatus in setOf("pending","approved","rejected")) { "unsupported key status" }` | `UserCheckinAction` |
| 94 | `adminDeleteKey(id)` (616) | DELETE (no body) | `/api/admin/key-management?id={id}` — query embedded in the path string | `require(id > 0)` | `UserCheckinAction` |
| 95 | `adminSaveCookieConfig(config: AdminCookieConfig, cookieRaw: String?)` (621) | PUT if `config.id > 0` else POST | `/api/admin/cookie-config` | `description`, `is_active`; `cookie_raw` when non-blank; `proxy_ip` when non-blank; plus `id` (update) or `config_key` (create) (630-636) | `UserCheckinAction` |
| 96 | `adminDeleteCookieConfig(id)` (640) | DELETE (with body) | `/api/admin/cookie-config` | `id`; `require(id > 0) { "cookie config id must be positive" }` | `UserCheckinAction` |
| 97 | `adminSaveBaseUrlRule(rule: AdminBaseUrlRule)` (645) | PUT if `rule.id > 0` else POST | `/api/admin/baseurl-rules` | `action`, `description`; plus `id` (update) or `pattern` (create) | `UserCheckinAction` |
| 98 | `adminDeleteBaseUrlRule(id)` (659) | DELETE (no body) | `/api/admin/baseurl-rules?id={id}` | `require(id > 0) { "rule id must be positive" }` | `UserCheckinAction` |
| 99 | `adminSaveShopItem(item: AdminShopItem)` (664) | PUT if `item.id > 0` else POST | `/api/admin/shop/items` | `name`, `description`, `price`, `type`, `is_active` (1/0); if `type == "frame"` → `image_url`; else `image_url=""` + `badge_html` + `badge_css` (671-677); plus `id` on update | `UserCheckinAction` |
| 100 | `adminDeleteShopItem(id)` (687) | DELETE (no body) | `/api/admin/shop/items?id={id}` | `require(id > 0) { "shop item id must be positive" }` | `UserCheckinAction` |

### 2.12 Political exam ("政治考试")

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 101 | `startPoliticalExam()` (1248) | POST | `/api/political-exams/sessions` | body `{}` | `PoliticalExamSession` → `normalizePoliticalExamSession` |
| 102 | `submitPoliticalExam(sessionId, answers: PoliticalExamAnswers)` (1252) | POST | `/api/political-exams/sessions/submit` | `session_id`; `answers` = `{single_choice:[Int|null], multiple_choice:[[Int]], true_false:[Boolean|null], fill_blank:[String]}` — nulls encoded as `JSONObject.NULL` (1256-1266) | `PoliticalExamResult` → `normalizePoliticalExamResult` |

### 2.13 Uploads

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 103 | `uploadBook(upload: UploadBookRequest, epubFile=null, coverFile=null)` (1473) | POST (multipart) | `/api/uploads/books` | see §4.1 | `UploadActionResult` → `normalizeUploadResult` |
| 104 | `uploadFileInChunks(file, fileId=UUID.randomUUID().toString(), chunkSizeBytes=WEBSITE_UPLOAD_CHUNK_BYTES)` (1501) | POST ×(N+1) | `/api/uploads/chunks` | see §4.3 | `String` (server file path) |
| 105 | `parseUploadedEpub(filePath)` (1544) | POST | `/api/uploads/epubs` | `file_path`, `parse_only`=`true` | `ParsedEpub` → `normalizeParsedEpub` |

### 2.14 External (non-NovalPie) endpoint

| # | Function (line) | Method | Path | Query / Body | Returns |
|---|---|---|---|---|---|
| 106 | `generateEditorRegex(endpoint, apiKey, model, chapterTitles)` (1196) | POST | `{endpoint.trimEnd('/')}/v1/chat/completions` (OpenAI-compatible, arbitrary host) | see §2.14.1 | `String` (a regex) |

#### 2.14.1 `generateEditorRegex` detail (`NovalPieApi.kt:1196-1246`)

Validations: `endpoint` must start with `http://` or `https://` → `"API endpoint must use HTTP or HTTPS"`;
`apiKey.isNotBlank()` → `"API key is required"`; `model.isNotBlank()` → `"Model is required"`;
`titles = chapterTitles.filter(String::isNotBlank).take(20)` and `titles.size >= 2`
→ `"At least two chapter titles are required"`.

System prompt (verbatim, `1207-1210`):
```
You are a regular-expression expert. Generate JavaScript-compatible regex patterns that match all supplied chapter titles.
Return one JSON object with a regex string field. Multiple patterns may be separated by newlines.
```
User prompt (`1211-1214`): `"Generate regex patterns for these chapter titles:"` followed by
`"${index + 1}. $title"` lines.

Payload: `model`, `messages` (system + user), `temperature` = `0.3`,
`response_format` = `{"type":"json_object"}`.

Headers set directly on `baseRequestBuilder(url)`: `content-type: application/json`,
`authorization: Bearer $apiKey`. Executed via `executeExternal("editor AI regex", ...)`, so no site
cookie / site auth token leaks to the third-party host.

Response extraction chain (1233-1245):
1. `choices[0].message.content` must exist, else `IOException("AI response did not contain message content")`.
2. Try `JSONObject(content)` → `firstStringOrNull("regex","pattern","expression")`.
3. Else strip a fenced block with `Regex("```(?:json)?\\s*([\\s\\S]*?)```", IGNORE_CASE)`, parse group 1
   as JSON, take the same three keys.
4. Else use `content.trim()` verbatim.
5. Blank result → `IOException("AI did not return a regex pattern")`.

Also `IOException("AI returned an invalid response")` when the response is not a `JSONObject` (1231-1232).

---

## 3. Reader signed-session protocol (fragile crypto — must survive byte-identical)

Four cooperating pieces: `readerSessionKey()`, `readerSignatureHeaders()`, `decryptReaderContent()`,
and the helpers `randomNonce()` / `rotateLeft3Hex()` / `md5Hex()` / `sha256Hex()` / `hmacSha1()` /
`customBase64()` / `decodeBase64()` / `toHexString()`.

### 3.1 `readerSessionKey()` — `NovalPieApi.kt:2023-2031`

```kotlin
private data class ReaderSessionKey(val sessionId: String, val sessionKey: String)  // line 2021

private fun readerSessionKey(): ReaderSessionKey {
    val raw = get("/api/reader/session-key", headers = readerSignatureHeaders())
    val source = unwrapObject(raw, "data", "result", "session")
    val sessionId = source.firstStringOrNull("session_id", "sessionId", "id")
        ?: throw IOException("Reader session id is empty.")
    val sessionKey = source.firstStringOrNull("session_key", "sessionKey", "key")
        ?: throw IOException("Reader session key is empty.")
    return ReaderSessionKey(sessionId = sessionId, sessionKey = sessionKey)
}
```

- `GET /api/reader/session-key` with **no query parameters** and the three signature headers.
- Note it is a plain `get()`, so cookie + bearer auth headers are still applied by `execute()`.
- Field aliases: id from `session_id` | `sessionId` | `id`; key from `session_key` | `sessionKey` | `key`.
- Called once per `chapterContent()` invocation (no caching — one session fetch per chapter read,
  `NovalPieApi.kt:1282`).

### 3.2 `readerSignatureHeaders()` — `NovalPieApi.kt:2102-2115`

Exact algorithm (order matters, all string concatenation, no separators):

```kotlin
val timestamp        = (System.currentTimeMillis() / 1000L).toString()   // Unix seconds, decimal
val nonce            = randomNonce()                                     // 8 chars from [a-z0-9]
val rotatedTimestamp = rotateLeft3Hex(timestamp)
val digest           = md5Hex("$USER_AGENT$timestamp$nonce")              // lowercase hex, 32 chars
val payload          = sha256Hex("$digest$READER_SIGNATURE_SECRET$rotatedTimestamp")  // lowercase hex, 64 chars
val hmacKey          = md5Hex(READER_SIGNATURE_SECRET)                     // lowercase hex, 32 chars
val signature        = customBase64(hmacSha1(hmacKey, payload))
```

Headers returned (exact casing):

| Header | Value |
|---|---|
| `X-Client-Signature` | `signature` |
| `X-Client-Timestamp` | `timestamp` |
| `X-Client-Nonce` | `nonce` |

Notes that must be preserved:
- `USER_AGENT` participates in the MD5 digest **and** is sent as the `user-agent` header
  (`baseRequestBuilder`, line 1640). Changing the UA string breaks reader auth.
- `READER_SIGNATURE_SECRET` is used twice with different roles: as literal text inside the SHA-256
  input, and as the **plaintext input to MD5** whose lowercase hex output becomes the HMAC-SHA1 key.
- The HMAC key is the 32-char lowercase hex **string** UTF-8 bytes, not the raw 16 MD5 bytes
  (`hmacSha1` does `key.toByteArray(Charsets.UTF_8)`, line 2158).

### 3.3 `randomNonce()` — `NovalPieApi.kt:2137-2140`

```kotlin
(1..8).map { READER_NONCE_ALPHABET[secureRandom.nextInt(READER_NONCE_ALPHABET.length)] }.joinToString("")
```
8 characters drawn from `"abcdefghijklmnopqrstuvwxyz0123456789"` (36 symbols) using the companion-level
`SecureRandom`.

### 3.4 `rotateLeft3Hex()` — `NovalPieApi.kt:2142-2145`

```kotlin
val rotated = Integer.rotateLeft(timestamp.toLong().toInt(), 3)
return (rotated.toLong() and 0xffffffffL).toString(16)
```
- Parses the decimal timestamp as `Long`, **narrows to 32-bit `Int`** (`toInt()` truncation is intentional),
  rotates left by 3 bits (32-bit rotate, not shift), then masks back to unsigned 32-bit and formats as
  lowercase hex **without zero padding** (`Long.toString(16)`).
- Throws `NumberFormatException` if the string is not a valid Long — not defensively handled.

### 3.5 Digest / MAC helpers — `NovalPieApi.kt:2147-2172`

```kotlin
md5Hex(v)    = digestHex("MD5", v)
sha256Hex(v) = digestHex("SHA-256", v)
digestHex(alg, v) = MessageDigest.getInstance(alg).digest(v.toByteArray(UTF_8)).toHexString()
hmacSha1(key, msg) = Mac.getInstance("HmacSHA1")
    .apply { init(SecretKeySpec(key.toByteArray(UTF_8), "HmacSHA1")) }
    .doFinal(msg.toByteArray(UTF_8))            // returns raw ByteArray (20 bytes)
ByteArray.toHexString() = joinToString("") { "%02x".format(it) }   // lowercase, zero-padded
```

### 3.6 `customBase64()` — the substituted alphabet — `NovalPieApi.kt:2162-2168`

```kotlin
Base64.encodeToString(value, Base64.NO_WRAP)     // android.util.Base64, standard alphabet, WITH padding '='
    .map { char ->
        val index = STANDARD_BASE64_ALPHABET.indexOf(char)
        if (index >= 0) READER_BASE64_ALPHABET[index] else char
    }
    .joinToString("")
```

- Standard base64 (no line wrapping) is produced first, then each character is **position-substituted**
  through the two alphabets.
- `STANDARD_BASE64_ALPHABET` = `ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/`
- `READER_BASE64_ALPHABET`   = `M9N8B7V6C5X4Z3L2K1J0HGFDSAPOIUYTREWQmnbvcxzlkjhgfdsaqwertyuiop+/`
- Index-wise mapping (must be preserved exactly): `A→M, B→9, C→N, D→8, E→B, F→7, G→V, H→6, I→C, J→5,
  K→X, L→4, M→Z, N→3, O→L, P→2, Q→K, R→1, S→J, T→0, U→H, V→G, W→F, X→D, Y→S, Z→A,
  a→P, b→O, c→I, d→U, e→Y, f→T, g→R, h→E, i→W, j→Q, k→m, l→n, m→b, n→v, o→c, p→x, q→z, r→l, s→k,
  t→j, u→h, v→g, w→f, x→d, y→s, z→a, 0→q, 1→w, 2→e, 3→r, 4→t, 5→y, 6→u, 7→i, 8→o, 9→p, +→+, /→/`.
- `=` padding characters are **not** in `STANDARD_BASE64_ALPHABET`, so `indexOf` returns `-1` and they are
  passed through unchanged. HMAC-SHA1 is 20 bytes → base64 length 28 with exactly one trailing `=`.
- `+` and `/` map to themselves — the substitution is not a permutation of the full 64-symbol set in a
  way that changes those two.

### 3.7 `decryptReaderContent()` — `NovalPieApi.kt:2117-2135`

```kotlin
val decodedSessionKey = decodeBase64(sessionKey)                     // Base64.DEFAULT
val aesKey    = MessageDigest.getInstance("SHA-256").digest(decodedSessionKey)   // 32 bytes
val ciphertext = decodeBase64(encryptedContent)
val authTag    = decodeBase64(tag)
val cipherInput = ciphertext + authTag                               // tag APPENDED to ciphertext
val cipher = Cipher.getInstance("AES/GCM/NoPadding")
cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, decodeBase64(iv)))
return cipher.doFinal(cipherInput).toString(Charsets.UTF_8)
```

Invariants:
- AES key = **SHA-256 of the base64-decoded session key bytes** (not of the base64 string).
- GCM tag length = **128 bits**; the tag arrives as a separate base64 field and must be concatenated
  after the ciphertext because JCE expects tag-appended input.
- IV is base64-decoded as-is (length not validated; server sends 12 bytes in practice).
- Output is decoded as UTF-8.
- `decodeBase64(value) = Base64.decode(value, Base64.DEFAULT)` (line 2170) — the **standard** alphabet.
  The custom alphabet is used only for outgoing signatures, never for decryption inputs.

### 3.8 Where decryption is triggered — `normalizeReaderContent` (`NovalPieApi.kt:2033-2067`)

- If `raw` is a non-blank `String` → `ReaderContent(title = null, content = raw, source = "plain")` (2034-2036).
- Else `source = unwrapObject(raw, "data", "result", "chapter")`.
- If `source.optBoolean("encrypted", false)` is true → `decryptReaderContent(...)` with:
  - `content` (required, else `IOException("Encrypted chapter content is empty.")`)
  - `iv` (required, else `IOException("Encrypted chapter iv is empty.")`)
  - `tag` (required, else `IOException("Encrypted chapter tag is empty.")`)
  - `readerSessionKey` (required, else `IOException("Encrypted chapter session key is empty.")`)
- Else plaintext fallback chain: `content` → `html` → `body_html` → `bodyHtml` → `text` → `body`,
  else `IOException("Chapter content is empty.")` (2051-2057).
- `title` aliases: `title` → `chapter_title` → `chapter_name` (2060-2062).
- `source` field is `"api"` for the JSON path, `"plain"` for the raw-string path.
- Illustrations: `normalizeReaderContentIllustrations(raw, source)` (see §5.15).

### 3.9 Test-observable contract

`app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt:1416, 1506-1510` asserts:
`GET /api/reader/session-key` is issued first with method `GET` and non-blank
`X-Client-Signature` / `X-Client-Timestamp` / `X-Client-Nonce`, then
`/api/chapters/{id}/content` with `session`, `replace_mode=india`, `show_images=1`.
Tests use `session_key = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="` (base64 of the 32-char ASCII
string `0123456789abcdef0123456789abcdef`).

---

## 4. Multipart and chunked upload protocols

Two `RequestBody` implementations back all uploads:

- **`UploadStreamRequestBody`** (`NovalPieApi.kt:3355-3371`): whole-file streaming.
  `contentType()` = `source.contentType ?: "application/octet-stream"`; `contentLength()` = `source.sizeBytes`;
  `writeTo` copies `openStream()` with a `DEFAULT_BUFFER_SIZE` (8 KiB) buffer.
- **`UploadRangeRequestBody`** (`NovalPieApi.kt:3373-3404`): byte-range slice.
  `contentLength()` = `length`. `writeTo` first skips `offset` bytes using `input.skip()`, falling back to
  single-byte `read()` when `skip` returns `<= 0`; if the stream ends early it throws
  `IOException("文件长度小于分片偏移")` (line 3391). Then it copies exactly `length` bytes, throwing
  `IOException("文件在分片读取时提前结束")` (line 3398) on premature EOF.

`UploadFileSource` (`data/UploadFileSource.kt:5-10`): `fileName: String`, `sizeBytes: Long`,
`contentType: String? = null`, `openStream: () -> InputStream`. Note `openStream` is re-invoked for every
chunk, so the source must be re-openable (content URIs are).

### 4.1 `uploadBook` — `POST /api/uploads/books` (`NovalPieApi.kt:1473-1499`)

Single multipart request, `MultipartBody.FORM`. Parts, in exact order:

| Part | Value |
|---|---|
| `title` | `upload.title.trim()` |
| `title_translation` | `upload.titleTranslation.trim()` |
| `author_name` | `upload.authorName.trim()` |
| `description` | `upload.description.trim()` |
| `language` | `upload.language.trim()` |
| `spans` | `upload.spans.trim()` |
| `is_adult` | `"1"` / `"0"` |
| `source` | `upload.source.trim()` |
| `source_url` | `upload.sourceUrl.trim()` |
| `tags` | `upload.tags.joinToString(",")` — comma-joined string, **not** JSON |
| `submit_type` | `upload.submitType` |
| `chapters` | `uploadChaptersJson(upload.chapters).toString()` |
| `chapters_md5` | `md5Hex(chaptersJson)` — lowercase hex MD5 of the exact JSON string sent |
| `epub_file_path` | only when `upload.epubFilePath` is non-blank |
| `cover_url` | only when `upload.coverUrl` is non-blank |
| `epub_file` | file part, `UploadStreamRequestBody`, only when `epubFile != null` |
| `cover` | file part, `UploadStreamRequestBody`, only when `coverFile != null` |

`uploadChaptersJson` (`1686-1699`) emits a JSON array of objects with keys `title`, `content`,
`chapter_number`, plus `raw_path` and `spine_index` **only when non-null**.
`hierarchyLevel` and `sectionPath` from `UploadChapter` are **not transmitted** — they exist only for
local presentation.

Sent via `requestBody(path, "POST", multipart)`.

### 4.2 `appendManagedChapters` — `POST /api/users/me/chapters/append` (`NovalPieApi.kt:1406-1441`)

Preconditions: `require(bookId > 0 && chapters.isNotEmpty()) { "book and chapters are required" }`,
`require(submitType in setOf("chinese", "personal", "shared")) { "submit type is invalid" }`.

Chunking decision (1415-1416):
```kotlin
val shouldChunk = chapters.size > 50 ||
    chapters.sumOf { it.title.length.toLong() + it.content.length.toLong() } > 2_500_000L
val chunks = if (shouldChunk) chapters.chunked(50) else listOf(chapters)
```
So: chunk when > 50 chapters **or** > 2,500,000 total title+content characters; chunk size is 50 chapters.

Per chunk, one multipart POST with parts:

| Part | Value |
|---|---|
| `existing_novel_id` | `bookId` |
| `submit_type` | `submitType` |
| `chapters` | `uploadChaptersJson(chunk).toString()` |
| `chapters_md5` | `md5Hex(chaptersJson)` (per-chunk, not whole-payload) |
| `epub_file_path` | when non-blank |
| `epub_file` | `UploadStreamRequestBody(epubFile)` when non-null — **re-sent with every chunk** |
| `chunk_index` | `index.toString()` — only when `chunks.size > 1` |
| `total_chunks` | `chunks.size.toString()` — only when `chunks.size > 1` |
| `is_chunked` | `"1"` — only when `chunks.size > 1` |

Failure handling (1437): the first chunk whose `result.success == false` throws
`IOException(result.message ?: "append chapters failed")` — **no rollback of already-sent chunks**.
Return value is the last chunk's result with `novelId = result.novelId ?: bookId` (1438).

### 4.3 `uploadFileInChunks` — `POST /api/uploads/chunks` (`NovalPieApi.kt:1501-1542`)

Preconditions: `require(file.sizeBytes > 0L) { "文件为空" }`,
`require(chunkSizeBytes > 0) { "分片大小必须大于 0" }`.

```kotlin
val totalChunks = ((file.sizeBytes + chunkSizeBytes - 1L) / chunkSizeBytes).toInt()   // ceil division
```

**Phase 1 — N sequential multipart POSTs** (`1509-1525`). For chunk `index` in `0 until totalChunks`:
`offset = index * chunkSizeBytes`, `length = min(chunkSizeBytes, sizeBytes - offset)`.

| Part | Value |
|---|---|
| `file` | file part, `UploadRangeRequestBody(file, offset, length)` |
| `file_id` | `fileId` (defaults to a fresh `UUID.randomUUID().toString()`) |
| `chunk_index` | `index.toString()` (0-based) |
| `total_chunks` | `totalChunks.toString()` |
| `file_name` | `file.fileName` |
| `file_size` | `file.sizeBytes.toString()` |

Each response is `unwrapObject(..., "data", "result")`; if `success` is explicitly `false`
→ `IOException(message ?: "上传分片失败")` (1522-1524). Note: a *missing* `success` key is treated as OK.

**Phase 2 — merge, a JSON POST to the same path** (`1526-1541`):
```json
{"action": "merge", "file_id": "...", "file_name": "...", "total_chunks": N}
```
If `success` is explicitly `false` → `IOException(message ?: "合并文件失败")`.
Returns `firstStringOrNull("file_path", "filePath")`, else `IOException("服务器未返回文件路径")`.

**Chinese strings in this path (verbatim):** `"文件为空"`, `"分片大小必须大于 0"`, `"上传分片失败"`,
`"合并文件失败"`, `"服务器未返回文件路径"`, `"文件长度小于分片偏移"`, `"文件在分片读取时提前结束"`,
and in `normalizeParsedEpub`: `"解析 EPUB 失败"`, `"第 {n} 章"`.

### 4.4 Other multipart endpoints (summary)

| Function | Path | Method | Parts | Body impl |
|---|---|---|---|---|
| `uploadCurrentUserAvatar` (336) | `/api/users/me/avatar` | POST | `avatar` (filename = `file.fileName`) | `UploadRangeRequestBody(file, 0, sizeBytes)` |
| `uploadManagedBookCover` (278) | `/api/novels/{bookId}/photo` | **PUT** | `cover` | `UploadRangeRequestBody(file, 0, sizeBytes)` |
| `uploadManagedChapterIllustrations` (1301) | `/api/users/me/chapters/{chapterId}/illustrations` | POST | `chapter_id` + N × `illustrations[]` | `UploadStreamRequestBody` |

`requestBody()` only distinguishes `"PUT"`; every other method string becomes POST (`1579-1582`).

---

## 5. Normalizers — model produced and every accepted field alias

Two envelope helpers underpin everything; understanding them is a prerequisite.

### 5.0.1 `unwrapObject(raw, vararg keys)` — `NovalPieApi.kt:3047-3068`

1. If `raw` is not a `JSONObject` → return an **empty** `JSONObject()`.
2. First pass: for each `key` **skipping `"data"` and `"result"`**, if `raw.opt(key)` is a `JSONObject`, return it.
3. Second pass: for `"data"` then `"result"`, if the value is a distinct `JSONObject`, recurse
   `unwrapObject(nested, *keys)`; return it if the result has `length() > 0`.
4. Third pass: for each `key` (now including `data`/`result`), return the first `JSONObject` value.
5. Fallback: return `raw` itself.

Consequence: named keys always win over generic envelopes, and nested `data.<key>` shapes are handled.

### 5.0.2 `extractArray(raw, vararg preferredKeys)` — `NovalPieApi.kt:3013-3045`

1. `raw is JSONArray` → its items.
2. `raw` not a `JSONObject` → empty list.
3. Keys to check = `preferredKeys`, or when empty the default list
   `["items", "data", "results", "novels", "favorites", "books", "list", "records"]`.
4. For each key: a `JSONArray` value is returned directly; a `JSONObject` value is recursed with the
   fixed nested key list
   `["items","chapters","groups","favorite_groups","favoriteGroups","results","novels","favorites","books","posts","reviews","comments","list","records","data"]`.
5. Then an unconditional retry on the `"data"` key with the same nested list.
6. Last resort: collect every value whose **key is numeric** (`key.toLongOrNull() != null`) — this handles
   the server's occasional `{"0": {...}, "1": {...}}` map-as-array shape.

### 5.0.3 Scalar accessors (alias tolerance primitives)

| Helper | Line | Semantics |
|---|---|---|
| `JSONObject.stringOrNull(key)` | 3089-3092 | null when absent/JSON-null; `opt(key).toString()` filtered by `isNotBlank() && != "null"` — so the literal string `"null"` is rejected |
| `JSONObject.firstStringOrNull(vararg keys)` | 3099-3105 | first non-blank `stringOrNull` |
| `JSONObject.objectStringOrNull(key, vararg nestedKeys)` | 3094-3097 | descend into a nested object then `firstStringOrNull` |
| `JSONObject.longOrNull(key)` | 3323-3330 | `Number.toLong()`, `String.toLongOrNull()`, else null |
| `JSONObject.intOrNull(key)` | 3332 | `longOrNull(key)?.toInt()` |
| `JSONObject.doubleOrNull(key)` | 3293-3296 | `opt(key).toString().toDoubleOrNull()` |
| `JSONObject.booleanOrNull(key)` | 3282-3291 | `Boolean` as-is; `Number` → `toInt() != 0`; `String` → `statusTextToBoolean`; `JSONObject` → `booleanFromAny` |
| `JSONObject.firstBooleanOrNull(vararg keys)` | 3253-3259 | first non-null `booleanOrNull` |
| `booleanFromAny(value)` | 3261-3280 | `Boolean`/`Number`/`String` as above; `JSONObject` → `firstBooleanOrNull("data","result","is_favorited","isFavorited","is_favorite","isFavorite","favorited","favorite","exists","status")` |
| `statusTextToBoolean(value)` | 3298-3304 | true: `"true","1","yes","y","favorited","favorite","exists","collected","added"`; false: `"false","0","no","n","none","not_favorited","not-favorited","missing","removed"`; else null (case-insensitive, trimmed) |
| `JSONObject.toStringMap()` | 3072-3082 | all non-JSON-null values `toString()`ed into a `LinkedHashMap` |
| `JSONArray.toList()` | 3070 | `(0 until length()).map { opt(it) }` |
| `JSONObject.valueAsDisplayText(vararg keys)` | 1822-1831 | first present non-null key; `JSONArray` → `", "`-joined; else `toString()`; filtered non-blank |
| `JSONObject.arrayOrNull(key)` | 3084-3087 | **never called — dead helper** |

### 5.0.4 `normalizeAssetUrl(raw)` — `NovalPieApi.kt:3306-3315`

- Trim; blank → null.
- Already `http://`/`https://` → unchanged.
- Starts with `//` → prefixed `https:`.
- Starts with `/` → `baseUrl.trimEnd('/') + value`.
- Otherwise → `baseUrl.trimEnd('/') + "/" + value`.
- Finally `takeUnless(::isBareImageHost)`: `isBareImageHost` (3317-3321) returns true when the host
  equals `images.novelpia.com` (case-insensitive) **and** the path trimmed of `/` is blank — i.e. a
  useless bare-host URL is converted to `null`.

---

### 5.1 `normalizeNovelList(raw)` → `List<NovelCard>` — line 1833

`extractArray(raw)` with the **default** key list, mapping each `JSONObject` through `normalizeBook`.

### 5.2 `normalizeBook(raw)` → `NovelCard` — lines 1839-1982

Envelope: `unwrapObject(raw, "novel", "item", "data")`.

**`id` resolution (1841-1857)** — deliberately convoluted to handle favorite rows:
`favoriteObjectId` = `object_id` | `objectId`. It is used as the id only when non-null **and**
(`favorite_type`/`favoriteType` contains `"novel"` case-insensitively **or** any of
`novel_title` | `novelTitle` | `object_name` | `objectName` is non-blank). Otherwise fall back to
`id` → `novel_id` → `novelId` → `0L`.

| Field | Aliases (in order) |
|---|---|
| `title` | `title`, `name`, `novel_title`, `object_name`, else `"Untitled"` |
| `originalTitle` | `true_name`, `trueName`, `original_title`, `originalTitle`, `title_original`, `titleOriginal`, `raw_title`, `rawTitle` |
| `author` | nested `author.{name,username,display_name}`, then plain `author` string, then `author_name`, `authorName`, `writer_name`, `writerName` |
| `platform` | `platform`, `favorite_type`, `favoriteType`, `source`, `source_platform`, `sourcePlatform` |
| `status` | `normalizeBookStatus(source)` (§5.36) |
| `coverUrl` | `normalizeAssetUrl` of: `cover_url`, `coverUrl`, `photo_url`, `photoUrl`, `cover_image_url`, `coverImageUrl`, `cover_image`, `coverImage`, `image_url`, `imageUrl`, `thumbnail_url`, `thumbnail`, `photo`, `photo_path`, `photoPath`, `cover_path`, `coverPath`, `cover` |
| `fullCoverUrl` | `normalizeAssetUrl` of (28 aliases): `photo_true_url`, `photoTrueUrl`, `photo_original_url`, `photoOriginalUrl`, `photo_ori_url`, `photoOriUrl`, `full_cover_url`, `fullCoverUrl`, `cover_full_url`, `coverFullUrl`, `original_cover_url`, `originalCoverUrl`, `cover_original_url`, `coverOriginalUrl`, `origin_cover_url`, `originCoverUrl`, `photo_true_path`, `photoTruePath`, `photo_original_path`, `photoOriginalPath`, `photo_ori_path`, `photoOriPath`, `original_photo_url`, `originalPhotoUrl`, `origin_photo_url`, `originPhotoUrl`, `photo_origin_url`, `photoOriginUrl`, `cover_original_path`, `coverOriginalPath`, `original_cover_path`, `originalCoverPath` |
| `description` | `description`, `summary`, `synopsis` |
| `wordCount` | `word_count`, `wordCount`, `words` |
| `favoriteCount` | `favorite_count`, `favoriteCount`, `favorites`, `novel_like`, `novelLike`, `bookmark_count`, `bookmarkCount`, `collect_count`, `collectCount` |
| `siteReadCount` | `site_read_count`, `siteReadCount`, `novel_read`, `novelRead`, `read_count`, `readCount`, `view_count`, `viewCount`, `views` |
| `sourceReadCount` | `source_read_count`, `sourceReadCount`, `original_read_count`, `originalReadCount` |
| `sourceFavoriteCount` | `source_favorite_count`, `sourceFavoriteCount`, `original_favorite_count`, `originalFavoriteCount` |
| `updatedAt` | `updated_at`, `updateTime`, `created_at` |
| `tags` | `normalizeBookTags(source)` (§5.34) |

### 5.3 `normalizeChapters(raw)` → `List<Chapter>` — lines 1984-2019

Array keys: `chapters`, `items` (then `extractArray` fallbacks).

| Field | Aliases |
|---|---|
| `id` | `id`, `chapter_id`, `chapterId` — **row dropped if none** |
| `title` | `title`, `name`, `chapter_title`, `chapter_name`, else `"Chapter $id"` |
| `number` | `chapter_number`, `chapterNumber`, `number`, `display_order`, `order` |
| `wordCount` | `word_count`, `wordCount`, `words` |
| `updatedAt` | `updated_at`, `updateTime`, `created_at` |

Sorting (2015): `compareBy<IndexedChapter> { it.chapter.number ?: Int.MAX_VALUE }.thenBy { it.index }`
— chapters without a number sink to the end preserving server order; a private `IndexedChapter`
holder (2019) carries the original index.

### 5.4 `normalizeReaderContent` → `ReaderContent` — see §3.8

### 5.5 `normalizeReaderContentIllustrations(raw, source)` → `List<ChapterIllustration>` — 2069-2094

Tries `source` first, then the outer `raw`, in both cases with array keys
`illustrations`, `images`, `chapter_images`, `chapterImages`, `image_list`, `imageList`.

### 5.6 `normalizeChapterIllustrationItems(values)` — 2096-2100 / `normalizeChapterIllustrationItem(item, fallbackIndex)` — 2804-2812

| Field | Aliases / fallback |
|---|---|
| `src` | `normalizeAssetUrl` of `src`, `url`, `photo_url`, `photoUrl` — **item dropped if null** |
| `id` | `id`, `image_id`, `imageId`, else `fallbackIndex + 1L` |
| `index` | `index`, `order`, else `fallbackIndex + 1` |

### 5.7 `normalizeChapterIllustrations(raw)` → `ChapterIllustrationPage` — 2792-2802

Items via `extractArray(raw, "images", "items", "data", "list")`;
`total` from `total`, `image_count`, `imageCount`, else `images.size`.
Envelope for `total` is `unwrapObject(raw, "data", "result")`.

### 5.8 `normalizeChapterIllustrationMutation(raw)` → `ChapterIllustrationMutationResult` — 2814-2824

`success` = `booleanFromAny(raw)` → `firstBooleanOrNull("success","ok","status")` → `true`;
`imageCount` from `image_count` | `imageCount`; `message` from `message`|`msg`|`detail`;
`errors` from the `errors` array (`toString()` of each element).

### 5.9 `normalizeFavoriteGroups(raw)` → `List<FavoriteGroup>` — 2174-2193

Array keys: `groups`, `favorite_groups`, `favoriteGroups`, `items`.

| Field | Aliases |
|---|---|
| `id` | `id`, `group_id` (nullable — no row drop) |
| `name` | `name`, `title`, `group_name`, `groupName`, else `"Group"` |
| `count` | `count`, `novel_count`, `novelCount`, `book_count`, `bookCount`, `books_count`, `booksCount` |

### 5.10 `normalizeFavoriteStatus(raw)` → `FavoriteStatus` — 2439-2478

Envelope: `unwrapObject(raw, "data", "result", "favorite", "item")`.

- `rawState` aliases: `state`, `status`, `favorite_status`, `favoriteStatus`, `status_text`, `statusText`, `message`.
- `isFavorited` = `booleanFromAny(raw)` → `firstBooleanOrNull("is_favorited","isFavorited","is_favorite","isFavorite","favorited","favorite","exists","status","collected","in_favorites","inFavorites")` → `statusTextToBoolean(rawState)` → `false`.
- `groupId` = `group_id`, `groupId`, `favorite_group_id`, `favoriteGroupId`, then nested
  `group.id`, `favorite_group.id`, `favoriteGroup.id`.

### 5.11 `normalizeNovelTags(raw)` → `List<NovelTag>` — 2195-2210

Array keys: `tags`, `data`, `items`, `records`, `list`.

| Field | Aliases |
|---|---|
| `name` | `name`, `tag_name`, `tagName`, `label`, `title` — **row dropped if none** |
| `id` | `id`, `tag_id`, `tagId` |
| `count` | `count`, `book_count`, `bookCount`, `novel_count`, `novelCount` |

### 5.12 `normalizeMessagePage(raw, requestedPage, requestedPageSize)` → `MessagePage` — 2212-2236

Pagination object located at `raw.pagination` **or** `raw.data.pagination` (2218-2219).

| Field | Aliases / fallback |
|---|---|
| `page` | `page`, else `requestedPage.coerceAtLeast(1)` |
| `pageSize` | `page_size`, `pageSize`, else `requestedPageSize.coerceAtLeast(1)` |
| `total` | `total`, `total_count`, `totalCount`, else `0` |
| `totalPages` | `total_pages`, `totalPages`, else `1` |

Items via `normalizeMessages(raw)`.

### 5.13 `normalizeMessages(raw)` → `List<SiteMessage>` — 2238-2240

Array keys: `list`, `messages`, `items`, `records`, `data`.

### 5.14 `normalizeMessage(source)` → `SiteMessage?` — 2242-2271

| Field | Aliases |
|---|---|
| `id` | `id`, `message_id`, `messageId` — **null return if none** |
| `type` | `message_type`, `messageType`, else `0` |
| `title` | `message_title`, `messageTitle`, `title`, else `"Message"` |
| `content` | `message_content`, `messageContent`, `content`, `body` |
| `username` | `username`, `user_name`, `userName`, `sender_name`, `senderName` |
| `createdAt` | `created_at`, `createdAt`, `sent_at`, `sentAt` |
| `isRead` | `is_read`, `isRead`, `read`, else `false` |
| `isStarred` | `is_starred`, `isStarred`, `starred`, else `false` |
| `priority` | `priority`, else `0` |
| `actionUrl` | `action_url`, `actionUrl` |
| `actionText` | `action_text`, `actionText` |
| `readAt` | `read_at`, `readAt` |
| `userId` | `user_id`, `userId` |
| `executeUserId` | `execute_user_id`, `executeUserId` |
| `avatarUrl` | `avatar`, `avatar_url`, `avatarUrl` (**not** passed through `normalizeAssetUrl`) |
| `avatarFrameUrl` | `avatar_frame`, `avatarFrame`, `avatar_frame_url`, `avatarFrameUrl` |
| `extraData` | `extra_data` | `extraData` object → `toStringMap()` |

### 5.15 `normalizeMessageStats(raw)` → `MessageStats` — 2273-2292

Envelope: `unwrapObject(raw, "stats", "data", "result")`.
Fields: `total_count`|`totalCount`, `unread_count`|`unreadCount`, `read_count`|`readCount`,
`starred_count`|`starredCount`, `important_count`|`importantCount`,
`recent_7days_count`|`recentSevenDaysCount` (all default 0).
`unreadByType` built from the `unread_by_type` | `unreadByType` object, keeping only keys that parse as Int.

### 5.16 `normalizeMessageSettings(raw)` → `MessageSettings` — 2403-2419

Envelope: `unwrapObject(raw, "settings", "data", "result")`.

| Field | Aliases / default |
|---|---|
| `enableNotifications` | `enable_notifications`, `enableNotifications`, default `true` |
| `enableEmail` | `enable_email`, `enableEmail`, default `false` |
| `enableBrowserPush` | `enable_browser_push`, `enableBrowserPush`, default `true` |
| `notificationTypes` | `notification_types` | `notificationTypes` array → `Set<Int>` (elements parsed via `toString().toIntOrNull()`); **null when the key is absent** (distinct from empty set) |
| `quietHoursStart` | `quiet_hours_start`, `quietHoursStart` |
| `quietHoursEnd` | `quiet_hours_end`, `quietHoursEnd` |
| `autoReadAfterDays` | `auto_read_after_days`, `autoReadAfterDays` |

### 5.17 `normalizeMessageActionResult(raw)` → `MessageActionResult` — 2393-2401

`success` = `booleanFromAny(raw)` → `firstBooleanOrNull("success","ok","status")` → `true`;
`message` from `message`|`msg`|`detail`. Envelope `unwrapObject(raw, "data", "result")`.

### 5.18 `normalizeDirectMessages(raw)` → `List<DirectMessage>` — 2421-2437

Array keys: `list`, `messages`, `items`, `records`, `data`.

| Field | Aliases |
|---|---|
| `id` | `id`, `message_id`, `messageId` — **row dropped if none** |
| `content` | `message_content`, `messageContent`, `content`, `body` — **row dropped if none** |
| `createdAt` | `created_at`, `createdAt`, `sent_at`, `sentAt` |
| `userId` | `user_id`, `userId` |
| `executeUserId` | `execute_user_id`, `executeUserId` |

### 5.19 `normalizeWorkspaceApiConfigs(raw)` → `List<WorkspaceApiConfig>` — 2294-2314

Array keys: `data`, `apis`, `items`, `list`, `records`.

| Field | Aliases / default |
|---|---|
| `id` | `id` — **row dropped if none** |
| `name` | `name`, `api_name`, `apiName`, else `"API #$id"` |
| `model` | `model`, `model_name`, `modelName`, else `""` |
| `endpoint` | `endpoint`, `base_url`, `baseUrl`, else `""` |
| `apiKey` | `key`, `api_key`, `apiKey` |
| `concurrency` | `concurrency`, else `10` |
| `isActive` | `is_active`, `isActive`, `active`, else `true` |
| `isHealthy` | `is_healthy`, `isHealthy`, `healthy` (nullable) |
| `approvalStatus` | `approval_status`, `approvalStatus` |
| `totalRequests` | `totalRequests`, `total_requests`, `callCount`, `call_count`, else `0` |

### 5.20 `normalizeWorkspaceCookieConfigs(raw)` → `WorkspaceCookieConfigs` — 2316-2340

Envelope `unwrapObject(raw, "data", "result")`. Two lists located by key group:
- `myConfigs` ← first present of `myConfigs`, `my_configs`
- `sharedConfigs` ← first present of `otherConfigs`, `other_configs`, `sharedConfigs`, `shared_configs`

Per item:

| Field | Aliases / default |
|---|---|
| `id` | `id` — **row dropped if none** |
| `configKey` | `config_key`, `configKey`, else `"cookie-$id"` |
| `description` | `description`, `desc` |
| `cookieRaw` | `cookie_raw`, `cookieRaw` |
| `proxyIp` | `proxy_ip`, `proxyIp` |
| `isActive` | `is_active`, `isActive`, `active`, else `true` |
| `isHealthy` | `is_healthy`, `isHealthy`, `healthy` |
| `lastCheckAt` | `last_check_at`, `lastCheckAt` |
| `updatedByUsername` | `updated_by_username`, `updatedByUsername`, `provider_username` |

### 5.21 `normalizeWorkspaceApiStatus(raw)` → `WorkspaceApiStatus` — 2342-2352

Envelope: `unwrapObject(raw, "apiStatus", "api_status", "data", "result")`.
`total` ← `total`|`total_count`; `active` ← `active`|`active_count`; `healthy` ← `healthy`|`healthy_count`;
`totalRequests` ← `total_requests`|`totalRequests`. All default 0.

### 5.22 `normalizeWorkspaceTranslators(raw)` → `List<WorkspaceTranslatorHealth>` — 2354-2380

Items: `unwrapObject(raw,"data","result").optJSONArray("translators")` first, else
`extractArray(raw, "translators", "items", "list")`.

| Field | Aliases / default |
|---|---|
| `id` | `id` — **row dropped if none** |
| `name` | `name`, `translatorName`, `translator_name`, else `"Translator #$id"` |
| `model` | `model`, `modelName`, `model_name` |
| `endpoint` | `endpoint`, `baseUrl`, `base_url` |
| `isHealthy` | `isHealthy`, `is_healthy`, `healthy`, else `false` |
| `isActive` | `isActive`, `is_active`, `active`, else `false` |
| `approvalStatus` | `approval_status`, `approvalStatus` |
| `responseTimeMs` | `responseTime`, `response_time`, `responseTimeMs`, else `0` |
| `successRate` | `successRate`, `success_rate`, `uptime`, else `0.0` |
| `lastHealthError` | `lastHealthError`, `last_health_error`, `lastError`, `last_error` |

### 5.23 `normalizeWorkspaceActionResult(raw)` → `WorkspaceActionResult` — 2382-2391

`success` = `booleanFromAny(raw)` → `firstBooleanOrNull("success","ok","status")` → `true`;
`message` ← `message`|`msg`|`detail`; `id` ← `id`.

### 5.24 `managedBookInfo` inline normalizer → `BookEditInfo` — 172-190

Envelope: `unwrapObject(get(...), "data", "novel", "result")`.
`spans` = `firstStringOrNull("spans", "status").orEmpty()`.

| Field | Aliases / derivation |
|---|---|
| `id` | `id`, `novel_id`, else the requested `bookId` |
| `title` | `title`, `name`, else `""` |
| `titleTranslation` | `true_name`, `trueName`, `title_translation`, else `""` |
| `authorName` | `author_name`, `authorName`, `author`, else `""` |
| `description` | `description`, `summary`, else `""` |
| `source` | `source`, else `""` |
| `sourceUrl` | `source_url`, `sourceUrl`, else `""` |
| `language` | `language`, `ifBlank { "zh" }` |
| `status` | `if (spans.contains("完结")) "已完结" else "连载中"` |
| `isAdult` | `is_adult` \| `isAdult`, else `spans.contains("19")` |
| `photoUrl` | `normalizeAssetUrl(photo_url \| photoUrl \| cover_url \| coverUrl)`, else `""` |
| `tags` | `normalizeEditableBookTags(source)` (§5.35) |

### 5.25 `normalizeManagedBookTransfer(raw)` → `ManagedBookTransferResult` — 2826-2842

Envelope `unwrapObject(raw,"data","result")`; `target` = `source.target` or `raw.target`.
`success` = `booleanFromAny(raw)` → `success`|`ok`|`status` → `true`;
`message` ← `message`|`msg`|`detail`;
`targetUsername` ← `target_username`|`targetUsername`|`username`, then `target.{username,name,target_username,targetUsername}`;
`targetUserId` ← `target_user_id`|`targetUserId`, then `target.{id,user_id,userId}`.

### 5.26 `normalizeForumPosts(raw)` → `List<ForumPost>` — 2480-2485

Array keys: `posts`, `items`, `records`, `list`, `data`.

### 5.27 `normalizeForumPost(raw)` → `ForumPost` — 2487-2577

Envelope: `unwrapObject(raw, "post", "item", "data")`.

| Field | Aliases / derivation |
|---|---|
| `id` | `id`, `post_id`, `postId`, `topic_id`, `topicId`, else `0L` |
| `category` | `normalizeForumCategory(rawCategory)` where `rawCategory` ← `type`, `category`, `category_name`, `categoryName`, `section`, `forum_type` |
| `title` | `title`, `subject`, `name`; else `plainSnippet(content \| body \| text \| summary \| excerpt)?.take(32)`; else `"站内讨论"` |
| `authorName` | nested `author.{name,username,display_name,nickname}`, nested `user.{...}`, then `author_name`, `authorName`, `username`, `nickname` |
| `authorId` | `author.id`, `user.id`, `user_id`, `userId`, `author_id`, `authorId` |
| `bookTitle` | nested `novel.{title,name,novel_title}`, nested `book.{title,name,book_title}`, then `novel_title`, `novelTitle`, `book_title`, `bookTitle` |
| `replyCount` | `reply_count`, `replyCount`, `comments_count`, `comment_count`, `commentCount`, `replies` |
| `likeCount` | `like_count`, `likeCount`, `likes` |
| `reactionCount` | `reaction_count`, `reactionCount`, `reactions` |
| `awardPoints` | `award_points`, `awardPoints`, `reward_points`, `rewardPoints`, `awards` |
| `viewCount` | `view_count`, `viewCount`, `views`, `read_count`, `readCount` |
| `lastActiveLabel` | `last_active_at`, `lastActiveAt`, `updated_at`, `updatedAt`, `created_at`, `createdAt` |
| `excerpt` | `plainSnippet` of `excerpt`, `summary`, `content`, `body`, `text` |
| `tags` | `linkedSetOf`: normalized category first, then for each of `tags`, `tag`, `tag_names`, `tagNames`, `labels`, `keywords` — arrays via `normalizeTags`, strings via `splitTagString`; then `.take(3)` |
| `pinned` | `pinned`, `is_pinned`, `isPinned`, `top`, `is_top`, `isTop`, else `false` |
| `featured` | `featured`, `is_featured`, `isFeatured`, `essence`, `is_essence`, `isEssence`, `starred`, `is_starred`, `isStarred`, else `false` |

### 5.28 `normalizeForumPostDetail(raw)` → `ForumPostDetail` — 2579-2608

Envelope: `unwrapObject(raw, "post", "item", "data")`; `post` = `normalizeForumPost(source)`.
`content` ← `content_html`, `contentHtml`, `body_html`, `bodyHtml`, `content`, `body`, `text`.
`likeCount` ← `like_count`|`likeCount`|`likes`.
`dislikeCount` ← `dislike_count`|`dislikeCount`|`down_count`|`downCount`|`dislikes`.
`reactionCount` ← `reaction_count`|`reactionCount`|`reactions`.
`awardPoints` ← `award_points`|`awardPoints`|`reward_points`|`rewardPoints`.

### 5.29 `normalizeForumComments(raw)` → `List<ForumComment>` — 2610-2668

Array keys: `comments`, `items`, `records`, `list`, `data`. **No reply flattening** (unlike chapter comments).

| Field | Aliases |
|---|---|
| `id` | `id`, `comment_id`, `commentId` — **row dropped if none** |
| `postId` | `post_id`, `postId` |
| `parentCommentId` | `parent_comment_id`, `parentCommentId`, `comment_id_parent` |
| `authorName` | nested `author.{name,username,display_name,nickname}`, nested `user.{...}`, then `author_name`, `authorName`, `username`, `nickname` |
| `authorId` | `author.id`, `user.id`, `user_id`, `userId`, `author_id`, `authorId` |
| `replyToName` | `reply_to_name`, `replyToName` |
| `content` | `content_html`, `contentHtml`, `body_html`, `bodyHtml`, `content`, `body`, `text`, else `""` |
| `likeCount` | `like_count`, `likeCount`, `helpful_count`, `helpfulCount`, `likes` |
| `dislikeCount` | `dislike_count`, `dislikeCount`, `not_helpful_count`, `notHelpfulCount`, `down_count`, `downCount`, `dislikes` |
| `reactionCount` | `reaction_count`, `reactionCount`, `funny_count`, `funnyCount`, `reactions` |
| `awardPoints` | `award_points`, `awardPoints`, `award_count`, `awardCount`, `reward_points`, `rewardPoints` |
| `createdAt` | `created_at`, `createdAt`, `updated_at`, `updatedAt` |

### 5.30 `normalizeChapterComments(raw)` → `List<ChapterComment>` (flattened tree) — 2670-2771

- Top level uses array keys `comments`, `items`, `records`, `list`, `data`.
- `appendChapterCommentWithReplies` (2679-2706) appends the parent, then **recursively** walks
  `extractArray(source, "replies", "children", "reply_list", "replyList")`, producing a **flat** list in
  depth-first order. Each child inherits fallbacks from its parent: `bookId`, `chapterId`,
  `parentCommentId = parent.id`, `replyToName = parent.authorName`.
- `normalizeChapterComment` (2708-2771) aliases are identical to `ForumComment` (§5.29) plus:
  `bookId` ← `book_id`|`bookId`|fallback; `chapterId` ← `chapter_id`|`chapterId`|fallback;
  `replyToName` ← `reply_to_name`|`replyToName`|fallback.

### 5.31 `normalizeForumActionResult(raw)` → `ForumActionResult` — 2781-2790

`success` = `booleanFromAny(raw)` → `success`|`ok`|`status` → `true`; `message` ← `message`|`msg`|`detail`.

### 5.32 `normalizeForumCreateResult(raw)` → `ForumCreateResult` — 2854-2868

Envelope `unwrapObject(raw,"data","result")`; `post` = `source.post` or `raw.post`.
`success` as above; `message` ← `message`|`msg`|`detail`;
`postId` ← `post.id`, `post.post_id`, `source.post_id`, `source.postId`.

### 5.33 `normalizeForumCategory(value)` → Chinese label — 2870-2879

Lowercased, trimmed input:

| Input tokens | Output (verbatim Chinese) |
|---|---|
| `review`, `reviews`, `book_review`, `book-reviews`, `book_review_comment` | `书评` |
| `chapter`, `chapters`, `chapter_comment`, `chapter-comments` | `章节` |
| `post`, `posts`, `topic`, `topics`, `discussion`, `forum` | `讨论` |
| `notice`, `announcement`, `news`, `activity` | `动态` |
| `null`, `""` | `动态` |
| anything else | `value.trim()` (passed through) |

### 5.34 `normalizeBookTags(source)` → ordered unique tag list — 3107-3166

Insertion order into a `linkedSetOf<String>()`:
1. first non-blank of `novel_type`, `novelType`, `category`, `category_name`, `categoryName`, `type`, `type_name`, `typeName`
2. first non-blank of `genre`, `genre_name`, `genreName`
3. first non-blank of `spans`, `span`, `badges`, `badge` — split with `splitTagString(splitWhitespace = true)`
4. `tags`: `JSONArray` → `normalizeTags`; `String` → `splitTagString`
5. then, in order, each of the 21 aliases: `tag`, `tag_names`, `tagNames`, `keywords`, `labels`,
   `categories`, `category_list`, `categoryList`, `genres`, `genre_list`, `genreList`, `book_tags`,
   `bookTags`, `novel_tags`, `novelTags`, `tag_list`, `tagList`, `tag_relations`, `tagRelations`,
   `novel_tag_relations`, `novelTagRelations`, `taggings` — arrays via `normalizeTags`, strings via `splitTagString`

### 5.35 `normalizeEditableBookTags(source)` — 3168-3202

Same alias list as §5.34 step 5 but **starting with `tags`** and **omitting** category/genre/spans
derivation — i.e. only true tags, no derived pseudo-tags. Aliases: `tags`, `tag`, `tag_names`, `tagNames`,
`keywords`, `labels`, `categories`, `category_list`, `categoryList`, `genres`, `genre_list`, `genreList`,
`book_tags`, `bookTags`, `novel_tags`, `novelTags`, `tag_list`, `tagList`, `tag_relations`, `tagRelations`,
`novel_tag_relations`, `novelTagRelations`, `taggings`.

### 5.36 `normalizeBookStatus(source)` → `String?` — 3204-3228

First non-blank of `status`, `state`, `book_status`, `bookStatus`, `novel_status`, `novelStatus`,
`completion_status`, `completionStatus`, `serial_status`, `serialStatus`.
Otherwise boolean from `is_completed`, `isCompleted`, `completed`, `is_finished`, `isFinished`, `finished`
→ `"已完结"` / `"连载中"`. Null when neither is present.

### 5.37 `normalizeTags(values)` — 3230-3240

For each element: `JSONObject` → `firstStringOrNull("name","title","label","tag_name","tagName","value","text")`,
else nested `tag.{...}`, `novel_tag.{...}`, `novelTag.{...}` with the same 7 keys.
`null` → dropped. Anything else → `toString()` filtered `isNotBlank() && != "null"`.

### 5.38 `splitTagString(value, splitWhitespace = false)` — 3242-3251

Separators: `,` `;` `/` `|` `，` `、`; plus, when `splitWhitespace = true`, space, tab, `\n`, `\r`.
Each piece trimmed; blanks and the literal `"null"` dropped.

### 5.39 `plainSnippet(value)` — 2881-2887

`replace(Regex("<[^>]+>"), " ")` → `replace(Regex("\\s+"), " ")` → `trim()` →
`takeIf { isNotBlank() && it != "null" }`. Strips HTML tags for excerpt/preview use.

### 5.40 `normalizeUser(raw)` → `UserProfile` — 2889-2924

Envelope: `unwrapObject(raw, "user", "data", "profile")`.

| Field | Aliases / default |
|---|---|
| `id` | `id`, `user_id`, `userId`, `uid` |
| `name` | `username`, `name`, `display_name`, `displayName`, `nickname`, `nick_name`, else `"Logged user"` |
| `role` | `role`, `user_role`, `userRole` |
| `points` | `point`, `points` (note `point` singular first) |
| `createdAt` | `created_at`, `createdAt` |
| `avatarUrl` | `normalizeAssetUrl` of `avatar`, `avatar_url`, `avatarUrl` |
| `avatarFrameUrl` | `normalizeAssetUrl` of `avatar_frame`, `avatar_frame_url`, `avatarFrameUrl` |
| `bio` | `bio`, `description` |
| `email` | `email` |
| `isBanned` | `is_banned`, `isBanned` |
| `banReason` | `ban_reason`, `banReason` |
| `banExpiresAt` | `ban_expires_at`, `banExpiresAt` |
| `isAdult` | `is_adult`, `isAdult` |
| `deleted` | `deleted`, `is_deleted`, `isDeleted` |
| `badges` | `normalizeUserBadges(source.opt("badges"))` |
| `stats` | `normalizeUserStats(source.opt("stats"))` |
| `showCheckin` | `show_checkin`, `showCheckin` |
| `autoCheckin` | `auto_checkin`, `autoCheckin` |

### 5.41 `normalizeUserBadges(raw)` — 2987-3000

Accepts `JSONArray` or any Kotlin `Collection`. `JSONObject` elements → `firstStringOrNull("name","title","label","code")`;
`null`/`JSONObject.NULL` → dropped; anything else → `toString().trim()` filtered non-blank and `!= "null"`.
Result `.distinct()`.

### 5.42 `normalizeUserStats(raw)` — 3002-3011

`raw as? JSONObject` else empty map. Iterates all keys, keeping only those whose value parses via
`longOrNull` — into a `LinkedHashMap<String, Long>` (server key names preserved as-is).

### 5.43 `normalizeUserActivities(raw)` → `List<UserActivity>` — 2926-2965

Array keys: `activities`, `items`, `data`, `results`.
Sub-objects read: `comment`, `post`, `book`, `chapter`, and `chapter.book` (as `chapterBook`).

`type` ← `type`, `activity_type`, `activityType`, else `"post"`.

`title` is **type-dependent** (2935-2940):
- `"novel_comment"` → `book.{title,name}`
- `"chapter_comment"` → `chapter.book.{title,name}`
- `"post_comment"` or `"post"` → `post.{title,subject,name}`
- else → `source.{title,name}`
- fallback `"动态"`

`content` is **type-dependent** (2941-2946), then `plainSnippet`:
- `"novel_comment"`, `"chapter_comment"`, `"post_comment"` → `comment.{content,body,text}`
- `"post"` → `post.{content,body,text,excerpt}`
- else → `source.{content,body,text,excerpt}`

Other fields: `id` ← `source.id` | `comment.id` | `post.id` | `0L`;
`createdAt` ← `created_at`, `createdAt`, `updated_at`, `updatedAt`;
`postId` ← `post.id` | `post_id`; `bookId` ← `book.id` | `chapterBook.id` | `book_id` | `novel_id`;
`chapterId` ← `chapter.id` | `chapter_id`; `commentId` ← `comment.id` | `comment_id`;
`coverUrl` ← `normalizeAssetUrl((book ?: chapterBook).{cover, cover_url, photo_url, photo})`.

### 5.44 `normalizeUserCheckinRecords(raw)` → `List<UserCheckinRecord>` — 2967-2985

Envelope: `unwrapObject(raw, "records", "checkins", "data", "result")`.
The object is a **date-keyed map**, not an array. Each key matching `Regex("\\d{4}-\\d{2}-\\d{2}")`
produces a record; the value may be a `JSONObject` (`points` | `point`), a `Number`, or a numeric `String`
(default 0). Result sorted by date string.

### 5.45 `normalizePoliticalExamSession(raw)` → `PoliticalExamSession` — 1751-1776

Envelope: `unwrapObject(raw, "session", "data", "result")`; requires `source.exam` object else
`IOException("Exam response did not contain exam questions")`.

Question arrays: `single_choice`, `multiple_choice`, `true_false`, `fill_blank`.

**Compatibility hack (1757-1760)**: if `singleChoice` is empty **and** `multipleChoice.size == 50`,
the server has collapsed both sections — split as `singleChoice = multipleChoice.take(40)` and
`multipleChoice = multipleChoice.drop(40)`.

`paper.totalQuestions == 0` → `IOException("Exam response contained no questions")`.
`sessionId` ← `session_id`, `sessionId`, `id`, else `IOException("Exam response did not contain a session id")`.
`remainingTimeSeconds` ← `remaining_time` | `remainingTime` | `1800`, `.coerceAtLeast(0)`.

### 5.46 `normalizePoliticalExamQuestions(raw)` — 1778-1788

`question` ← `question`, `title`, `text` — **item dropped if none**.
`options` from the `options` array; each element `toString()` with `JSONObject.NULL` filtered out.

### 5.47 `normalizePoliticalExamResult(raw)` → `PoliticalExamResult` — 1790-1820

Envelope `unwrapObject(raw,"data","result")`. Hard requirements:
`score` (`IOException("Exam result did not contain a score")`),
`total` (`"Exam result did not contain a total"`),
`passed` (`"Exam result did not contain pass status"`).
`details` = the `details` object's keys → arrays of `PoliticalExamDetail`, preserved in a `LinkedHashMap`:
- `correct` ← `correct`, default `false`
- `question` ← `question`, `title`
- `userAnswer` ← `valueAsDisplayText("user_answer","userAnswer","answer")`
- `correctAnswer` ← `valueAsDisplayText("correct_answer","correctAnswer")`
- `explanation` ← `explanation`, `reason`

`token` ← `token`, `access_token`, `accessToken`.

### 5.48 `normalizeUploadResult(raw)` → `UploadActionResult` — 1701-1708

Envelope `unwrapObject(raw,"data","result")`.
`success` ← `success`, default `true`; `message` ← `message`|`msg`;
`novelId` ← `novel_id`, `novelId`, `id`.

### 5.49 `normalizeParsedEpub(raw)` → `ParsedEpub` — 1710-1739

Envelope `unwrapObject(raw,"data","result")`.
`success == false` → `IOException(message ?: "解析 EPUB 失败")`.
Metadata read from `source.metadata`: `title`; `author` ← `author`|`creator`; `description`;
`language` (`ifBlank { "zh" }`).
`epubFilePath` ← `epub_file_path`, `file_path`, `filePath`.
Chapters from `source.chapters` array; per chapter:
- `title` ← `title`, `ifBlank { "第 ${index + 1} 章" }`
- `content` ← `content`, else `""`
- `chapterNumber` ← `chapter_number`, else `index + 1`
- `hierarchyLevel` ← `hierarchy_level`, else `0`
- `sectionPath` ← `section_path` array, non-blank strings only
- `rawPath` ← `raw_path`, `rawPath`
- `spineIndex` ← `spine_index`, `spineIndex`

### 5.50 `normalizeAdminAction(raw)` → `UserCheckinAction` — 2773-2779

Envelope `unwrapObject(raw,"data","result")`; `success` ← `success`|`ok`, default `true`;
`message` ← `message`|`msg`|`detail`. `points` is never populated by this normalizer.

### 5.51 Admin inline normalizers (aliases)

- **`AdminReviewRequest`** (409-422): `id` (required), `type` ← `type`|`request_type` else `"unknown"`,
  `status` ← `status`|`review_status` else `"pending"`, `username` ← `username`|`user_name`|`userName`,
  `userId` ← `user_id`|`userId`, `novelId` ← `novel_id`|`novelId`,
  `title` ← `title`|`novel_title`|`name`, `reason` ← `reason`|`description`|`message`,
  `createdAt` ← `created_at`|`createdAt`.
- **`AdminKeyItem`** (426-437): `id` (required), `name` ← `name`|`model`|`provider_name` else `"Key"`,
  `model` ← `model`, `providerName` ← `provider_name`|`providerName`|`provider`,
  `approvalStatus` ← `approval_status`|`approvalStatus`|`status` else `"pending"`,
  `baseUrl` ← `base_url`|`baseUrl`, `createdAt` ← `created_at`|`createdAt`.
- **`AdminOperationLog`** (466-475): `id` (required), `action` ← `action`|`operation` else `"unknown"`,
  `status` ← `status`|`state` else `"unknown"`, `userId` ← `user_id`|`userId`,
  `novelId` ← `novel_id`|`novelId`, `message` ← `message`|`detail`|`error_message`,
  `createdAt` ← `created_at`|`createdAt`.
- **`AdminCookieConfig`** (488-496): `id` (required), `configKey` ← `config_key`|`configKey`|`key`
  else `"config"`, `description`, `proxyIp` ← `proxy_ip`|`proxyIp`,
  `isActive` ← `is_active`|`isActive`|`active` else `false`, `updatedAt` ← `updated_at`|`updatedAt`.
- **`AdminBaseUrlRule`** (502-508): `id` (required), `pattern` ← `pattern`|`base_url`|`baseUrl`
  (**row dropped if none**), `action` ← `action`|`policy` else `"manual"`, `description`.
- **`AdminShopItem`** (552-563): `id` (required), `name` ← `name`|`title` else `"Item"`, `description`,
  `price` ← `price` else `0L`, `type` ← `type`|`item_type` else `"frame"`,
  `imageUrl` ← `normalizeAssetUrl(image_url|imageUrl)`, `badgeHtml` ← `badge_html`|`badgeHtml`,
  `badgeCss` ← `badge_css`|`badgeCss`, `isActive` ← `is_active`|`isActive`|`active` else `false`.
- **`AdminDailyCount`** (365-369): `date` ← `date`|`day` (**row dropped if none**), `count` else `0`.

### 5.52 `normalizedThreshold(type, value, label)` — 2844-2852 (validator, not a normalizer)

`normalizedType = type.trim().ifBlank { "none" }`;
`require(normalizedType in setOf("none","points_min","points_pay")) { "$label threshold type is invalid" }`;
`"none"` → `("none", 0)`; otherwise `require(value > 0) { "$label threshold value must be positive" }`
and `require(value <= maxValue) { "$label threshold value exceeds website limit" }` where
`maxValue = 50` for `points_pay`, `100` otherwise. `label` is `"download"` or `"read"` (260, 264).

---

## 6. `model/Models.kt` — every class, field, type, default

All are `data class` unless noted. File is pure data — no methods except one computed property.

| # | Class (line) | Fields (type = default) |
|---|---|---|
| 1 | `NovelCard` (3-20) | `id: Long`; `title: String`; `originalTitle: String? = null`; `author: String? = null`; `platform: String? = null`; `status: String? = null`; `coverUrl: String? = null`; `description: String? = null`; `wordCount: Long? = null`; `favoriteCount: Long? = null`; `siteReadCount: Long? = null`; `sourceReadCount: Long? = null`; `sourceFavoriteCount: Long? = null`; `updatedAt: String? = null`; `tags: List<String> = emptyList()`; `fullCoverUrl: String? = null` |
| 2 | `NovelTag` (22-26) | `id: Long? = null`; `name: String`; `count: Int? = null` |
| 3 | `Chapter` (28-34) | `id: Long`; `title: String`; `number: Int? = null`; `wordCount: Long? = null`; `updatedAt: String? = null` |
| 4 | `ReaderContent` (36-41) | `title: String?`; `content: String`; `source: String`; `illustrations: List<ChapterIllustration> = emptyList()` |
| 5 | `ReaderProgress` (43-48) | `bookId: Long`; `chapterId: Long`; `chapterTitle: String? = null`; `updatedAtMillis: Long = 0L` |
| 6 | `FavoriteGroup` (50-54) | `id: Long?`; `name: String`; `count: Int? = null` |
| 7 | `FavoriteStatus` (56-60) | `isFavorited: Boolean`; `groupId: Long? = null`; `rawState: String? = null` |
| 8 | `BookEditInfo` (62-75) | `id: Long`; `title: String`; `titleTranslation: String = ""`; `authorName: String`; `description: String = ""`; `source: String = ""`; `sourceUrl: String = ""`; `language: String = "zh"`; `status: String = "连载中"`; `isAdult: Boolean = false`; `photoUrl: String = ""`; `tags: List<String> = emptyList()` |
| 9 | `BookEditPermissions` (77-89) | `title: Boolean = false`; `titleTranslation = false`; `authorName = false`; `description = false`; `source = false`; `sourceUrl = false`; `language = false`; `isAdult = false`; `photoUrl = false`; `spans = false`; `tags = false` (all `Boolean`) |
| 10 | `BookEditRequest` (91-103) | `title: String`; `titleTranslation: String = ""`; `authorName: String`; `description: String = ""`; `source: String = ""`; `sourceUrl: String = ""`; `language: String = "zh"`; `status: String = "连载中"`; `isAdult: Boolean = false`; `photoUrl: String = ""`; `tags: List<String> = emptyList()` |
| 11 | `BookEditResult` (105-110) | `success: Boolean`; `message: String? = null`; `failedFields: List<String> = emptyList()`; `errors: List<String> = emptyList()` |
| 12 | `ChapterIllustration` (112-116) | `id: Long`; `index: Int`; `src: String` |
| 13 | `ChapterIllustrationPage` (118-121) | `images: List<ChapterIllustration> = emptyList()`; `total: Int = images.size` (default references the previous parameter) |
| 14 | `ChapterIllustrationMutationResult` (123-128) | `success: Boolean`; `imageCount: Int? = null`; `message: String? = null`; `errors: List<String> = emptyList()` |
| 15 | `ManagedBookAccessPolicy` (130-136) | `allowDownload: Boolean = true`; `downloadThresholdType: String = "none"`; `downloadThresholdValue: Int = 0`; `readThresholdType: String = "none"`; `readThresholdValue: Int = 0` |
| 16 | `ManagedBookTransferResult` (138-143) | `success: Boolean`; `message: String? = null`; `targetUsername: String? = null`; `targetUserId: Long? = null` |
| 17 | `UserProfile` (145-164) | `id: Long?`; `name: String`; `role: String? = null`; `points: Long? = null`; `createdAt: String? = null`; `avatarUrl: String? = null`; `avatarFrameUrl: String? = null`; `bio: String? = null`; `email: String? = null`; `isBanned: Boolean? = null`; `banReason: String? = null`; `banExpiresAt: String? = null`; `isAdult: Boolean? = null`; `deleted: Boolean? = null`; `badges: List<String> = emptyList()`; `stats: Map<String, Long> = emptyMap()`; `showCheckin: Boolean? = null`; `autoCheckin: Boolean? = null` |
| 18 | `UserCheckinStats` (166-171) | `totalDays: Int = 0`; `totalPoints: Long = 0`; `maxStreak: Int = 0`; `currentStreak: Int = 0` |
| 19 | `UserCheckinAction` (173-177) | `success: Boolean`; `message: String? = null`; `points: Long? = null` |
| 20 | `UserActivity` (179-190) | `id: Long`; `type: String`; `title: String`; `content: String? = null`; `createdAt: String? = null`; `postId: Long? = null`; `bookId: Long? = null`; `chapterId: Long? = null`; `commentId: Long? = null`; `coverUrl: String? = null` |
| 21 | `UserCheckinRecord` (192-195) | `date: String`; `points: Long = 0` |
| 22 | `UserCheckinSettings` (197-200) | `showCheckin: Boolean = true`; `autoCheckin: Boolean = false` |
| 23 | `AdminDailyCount` (202-205) | `date: String`; `count: Int` |
| 24 | `AdminOverviewStats` (207-214) | `pendingReviewTotal: Int = 0`; `pendingReviewUpload: Int = 0`; `pendingReviewDelete: Int = 0`; `activeNovelTotal: Int = 0`; `registeredUserTotal: Int = 0`; `recentUserDaily: List<AdminDailyCount> = emptyList()` |
| 25 | `AdminReviewSettings` (216-219) | `autoApproveUpload: Boolean = false`; `autoApproveDelete: Boolean = false` |
| 26 | `AdminReviewRequest` (221-231) | `id: Long`; `type: String`; `status: String`; `username: String? = null`; `userId: Long? = null`; `novelId: Long? = null`; `title: String? = null`; `reason: String? = null`; `createdAt: String? = null` |
| 27 | `AdminKeyItem` (233-241) | `id: Long`; `name: String`; `model: String? = null`; `providerName: String? = null`; `approvalStatus: String`; `baseUrl: String? = null`; `createdAt: String? = null` |
| 28 | `AdminOperationLog` (243-251) | `id: Long`; `action: String`; `status: String`; `userId: Long? = null`; `novelId: Long? = null`; `message: String? = null`; `createdAt: String? = null` |
| 29 | `AdminOperationLogPage` (253-258) | `items: List<AdminOperationLog> = emptyList()`; `total: Int = 0`; `totalPages: Int = 0`; `actionTypes: List<String> = emptyList()` |
| 30 | `AdminCookieConfig` (260-267) | `id: Long`; `configKey: String`; `description: String? = null`; `proxyIp: String? = null`; `isActive: Boolean = false`; `updatedAt: String? = null` |
| 31 | `AdminBaseUrlRule` (269-274) | `id: Long`; `pattern: String`; `action: String`; `description: String? = null` |
| 32 | `AdminSchedulerLogs` (276-281) | `logs: List<String> = emptyList()`; `totalLines: Int = 0`; `fileSizeMb: Double? = null`; `lastModified: String? = null` |
| 33 | `AdminShopItem` (283-293) | `id: Long`; `name: String`; `description: String? = null`; `price: Long = 0`; `type: String`; `imageUrl: String? = null`; `badgeHtml: String? = null`; `badgeCss: String? = null`; `isActive: Boolean = false` |
| 34 | `MessageStats` (295-303) | `totalCount: Int = 0`; `unreadCount: Int = 0`; `readCount: Int = 0`; `starredCount: Int = 0`; `importantCount: Int = 0`; `recentSevenDaysCount: Int = 0`; `unreadByType: Map<Int, Int> = emptyMap()` |
| 35 | `SiteMessage` (305-323) | `id: Long`; `type: Int`; `title: String`; `content: String? = null`; `username: String? = null`; `createdAt: String? = null`; `isRead: Boolean = false`; `isStarred: Boolean = false`; `priority: Int = 0`; `actionUrl: String? = null`; `actionText: String? = null`; `readAt: String? = null`; `userId: Long? = null`; `executeUserId: Long? = null`; `avatarUrl: String? = null`; `avatarFrameUrl: String? = null`; `extraData: Map<String, String> = emptyMap()` |
| 36 | `MessageQuery` (325-330) | `keyword: String = ""`; `messageType: Int? = null`; `isRead: Boolean? = null`; `priority: Int? = null` |
| 37 | `MessagePagination` (332-337) | `page: Int = 1`; `pageSize: Int = 20`; `total: Int = 0`; `totalPages: Int = 1` |
| 38 | `MessagePage` (339-342) | `items: List<SiteMessage>`; `pagination: MessagePagination` |
| 39 | `MessageSettings` (344-352) | `enableNotifications: Boolean = true`; `enableEmail: Boolean = false`; `enableBrowserPush: Boolean = true`; `notificationTypes: Set<Int>? = null`; `quietHoursStart: String? = null`; `quietHoursEnd: String? = null`; `autoReadAfterDays: Int? = null` |
| 40 | `DirectMessage` (354-360) | `id: Long`; `content: String`; `createdAt: String? = null`; `userId: Long? = null`; `executeUserId: Long? = null` |
| 41 | `MessageActionResult` (362-365) | `success: Boolean`; `message: String? = null` |
| 42 | `WorkspaceApiConfig` (367-378) | `id: Long`; `name: String`; `model: String`; `endpoint: String`; `apiKey: String? = null`; `concurrency: Int = 10`; `isActive: Boolean = true`; `isHealthy: Boolean? = null`; `approvalStatus: String? = null`; `totalRequests: Long = 0` |
| 43 | `WorkspaceCookieStatus` (380-382) | `hasCookie: Boolean = false` |
| 44 | `WorkspaceCookieConfig` (384-394) | `id: Long`; `configKey: String`; `description: String? = null`; `cookieRaw: String? = null`; `proxyIp: String? = null`; `isActive: Boolean = true`; `isHealthy: Boolean? = null`; `lastCheckAt: String? = null`; `updatedByUsername: String? = null` |
| 45 | `WorkspaceCookieConfigs` (396-399) | `myConfigs: List<WorkspaceCookieConfig> = emptyList()`; `sharedConfigs: List<WorkspaceCookieConfig> = emptyList()` |
| 46 | `WorkspaceApiStatus` (401-406) | `total: Int = 0`; `active: Int = 0`; `healthy: Int = 0`; `totalRequests: Long = 0` |
| 47 | `WorkspaceTranslatorHealth` (408-419) | `id: Long`; `name: String`; `model: String? = null`; `endpoint: String? = null`; `isHealthy: Boolean = false`; `isActive: Boolean = false`; `approvalStatus: String? = null`; `responseTimeMs: Long = 0`; `successRate: Double = 0.0`; `lastHealthError: String? = null` |
| 48 | `WorkspaceHealth` (421-424) | `apiStatus: WorkspaceApiStatus = WorkspaceApiStatus()`; `translators: List<WorkspaceTranslatorHealth> = emptyList()` |
| 49 | `WorkspaceActionResult` (426-430) | `success: Boolean`; `message: String? = null`; `id: Long? = null` |
| 50 | `WorkspaceLocalApiConfig` (432-441) | `id: Long`; `name: String`; `model: String`; `endpoint: String`; `apiKey: String`; `concurrency: Int = 10`; `sharedToServer: Boolean = false`; `serverId: Long? = null` — **local-only model, never produced by the API** (used by `data/WorkspaceLocalStore.kt`) |
| 51 | `WorkspaceTranslationJob` (443-454) | `id: Long`; `bookId: Long`; `bookTitle: String`; `translatorId: Long? = null`; `translatorName: String`; `chapterCount: Int = 0`; `completedChapters: Int = 0`; `status: String = "pending"`; `createdAt: String? = null`; `updatedAt: String? = null` — **no normalizer produces this**; see §7 |
| 52 | `UploadChapter` (456-464) | `title: String`; `content: String`; `chapterNumber: Int`; `hierarchyLevel: Int = 0`; `sectionPath: List<String> = emptyList()`; `rawPath: String? = null`; `spineIndex: Int? = null` |
| 53 | `ParsedEpub` (466-473) | `title: String = ""`; `author: String = ""`; `description: String = ""`; `language: String = "zh"`; `chapters: List<UploadChapter> = emptyList()`; `epubFilePath: String? = null` |
| 54 | `UploadBookRequest` (475-490) | `title: String`; `titleTranslation: String = ""`; `authorName: String`; `description: String = ""`; `language: String = "zh"`; `spans: String = "balanced"`; `isAdult: Boolean = false`; `source: String = ""`; `sourceUrl: String = ""`; `tags: List<String> = emptyList()`; `submitType: String = "chinese"`; `chapters: List<UploadChapter>`; `epubFilePath: String? = null`; `coverUrl: String? = null` |
| 55 | `UploadActionResult` (492-496) | `success: Boolean`; `message: String? = null`; `novelId: Long? = null` |
| 56 | `EditorBookMetadata` (498-507) | `title: String = ""`; `author: String = ""`; `description: String = ""`; `language: String = "zh"`; `tags: String = ""` (comma string, not a list); `isAdult: Boolean = false`; `source: String = ""`; `sourceUrl: String = ""` |
| 57 | `EditorArchive` (509-518) | `id: String`; `name: String`; `timestamp: Long`; `textContent: String`; `metadata: EditorBookMetadata = EditorBookMetadata()`; `fileName: String? = null`; `chapterCount: Int = 0`; `totalWords: Int = 0` |
| 58 | `PoliticalExamQuestion` (520-523) | `question: String`; `options: List<String> = emptyList()` |
| 59 | `PoliticalExamPaper` (525-533) | `singleChoice: List<PoliticalExamQuestion> = emptyList()`; `multipleChoice = emptyList()`; `trueFalse = emptyList()`; `fillBlank = emptyList()`; **computed property** `val totalQuestions: Int get() = singleChoice.size + multipleChoice.size + trueFalse.size + fillBlank.size` (531-532) |
| 60 | `PoliticalExamSession` (535-539) | `sessionId: String`; `remainingTimeSeconds: Int = 1800`; `paper: PoliticalExamPaper = PoliticalExamPaper()` |
| 61 | `PoliticalExamAnswers` (541-546) | `singleChoice: List<Int?> = emptyList()`; `multipleChoice: List<List<Int>> = emptyList()`; `trueFalse: List<Boolean?> = emptyList()`; `fillBlank: List<String> = emptyList()` |
| 62 | `PoliticalExamDetail` (548-554) | `correct: Boolean`; `question: String? = null`; `userAnswer: String? = null`; `correctAnswer: String? = null`; `explanation: String? = null` |
| 63 | `PoliticalExamResult` (556-562) | `score: Int`; `total: Int`; `passed: Boolean`; `details: Map<String, List<PoliticalExamDetail>> = emptyMap()`; `token: String? = null` |
| 64 | `ForumPost` (564-581) | `id: Long`; `category: String`; `title: String`; `authorName: String? = null`; `bookTitle: String? = null`; `replyCount: Int? = null`; `likeCount: Int? = null`; `reactionCount: Int? = null`; `awardPoints: Int? = null`; `viewCount: Int? = null`; `lastActiveLabel: String? = null`; `excerpt: String? = null`; `tags: List<String> = emptyList()`; `pinned: Boolean = false`; `featured: Boolean = false`; `authorId: Long? = null` |
| 65 | `ForumPostDetail` (583-590) | `post: ForumPost`; `content: String? = null`; `likeCount: Int? = null`; `dislikeCount: Int? = null`; `reactionCount: Int? = null`; `awardPoints: Int? = null` |
| 66 | `ForumComment` (592-605) | `id: Long`; `postId: Long? = null`; `parentCommentId: Long? = null`; `authorName: String? = null`; `replyToName: String? = null`; `content: String`; `likeCount: Int? = null`; `dislikeCount: Int? = null`; `reactionCount: Int? = null`; `awardPoints: Int? = null`; `createdAt: String? = null`; `authorId: Long? = null` |
| 67 | `ChapterComment` (607-621) | `id: Long`; `bookId: Long? = null`; `chapterId: Long? = null`; `parentCommentId: Long? = null`; `authorName: String? = null`; `replyToName: String? = null`; `content: String`; `likeCount: Int? = null`; `dislikeCount: Int? = null`; `reactionCount: Int? = null`; `awardPoints: Int? = null`; `createdAt: String? = null`; `authorId: Long? = null` |
| 68 | `ForumActionResult` (623-626) | `success: Boolean`; `message: String? = null` |
| 69 | `ForumPollDraft` (628-634) | `question: String? = null`; `options: List<String>`; `allowMultiple: Boolean = false`; `maxChoices: Int = 1`; `endsAt: String? = null` |
| 70 | `ForumCreateRequest` (636-642) | `type: String`; `title: String`; `content: String`; `tags: List<String> = emptyList()`; `poll: ForumPollDraft? = null` |
| 71 | `ForumCreateResult` (644-648) | `success: Boolean`; `message: String? = null`; `postId: Long? = null` |

### 6.1 The only sealed hierarchy — `LoadResult<out T>` (`Models.kt:650-655`)

```kotlin
sealed interface LoadResult<out T> {
    object Idle : LoadResult<Nothing>
    object Loading : LoadResult<Nothing>
    data class Success<T>(val value: T) : LoadResult<T>
    data class Error(val message: String) : LoadResult<Nothing>
}
```

Note the variance quirk: the interface is declared `out T` but `Success<T>` is **invariant** (`data class Success<T>`,
not `Success<out T>`). This is the single state wrapper used by every ViewModel field
(e.g. `NovalPieViewModel.kt:196`).

### 6.2 Models defined nowhere in `Models.kt` but part of the API contract

- `UploadFileSource` — `data/UploadFileSource.kt:5-10` (class, not data class): `fileName: String`,
  `sizeBytes: Long`, `contentType: String? = null`, `openStream: () -> InputStream`.
- `NovalPieApi.ReaderSessionKey` — private data class, `NovalPieApi.kt:2021`: `sessionId`, `sessionKey`.
- `NovalPieApi.IndexedChapter` — private data class, `NovalPieApi.kt:2019`: `index: Int`, `chapter: Chapter`.

---

## 7. Dead code and doc/implementation gaps

### 7.1 Endpoints defined in the API but never called from production code

Verified by diffing every `suspend fun` name against every `api.*(` call site under `app/src/main`.

| Function | Line | Status |
|---|---|---|
| `adminApproveAllReviews(type, status, keyword)` | 592-602 | **Fully dead.** Zero call sites in `app/src/main` **and** zero in `app/src/test`. Posts `{"action":"approve_all", ...}` to `/api/admin/review-requests`. |
| `updateCurrentUserCheckinSettings(showCheckin, autoCheckin)` | 702-720 | **Dead in production.** Only caller is `app/src/test/java/com/novalpie/nativeapp/data/NovalPieApiTest.kt:385`. The ViewModel instead sends `show_checkin`/`auto_checkin` inside `updateCurrentUser()` (`PATCH /api/users/me`, lines 692-700). Two divergent write paths exist for the same two settings — the `/api/users/me/checkins/settings` path is unused. |

### 7.2 Redundant duplicate endpoints (not dead, but exact aliases)

| Pair | Lines | Note |
|---|---|---|
| `toggleForumCommentLike(commentId)` / `toggleCommentLike(commentId)` | 1106-1108 / 1141-1143 | Identical bodies: `POST /api/comments/{id}/likes` with `{}`. Both are called (2 and 2 sites in the ViewModel). |
| `reactToForumComment(...)` / `reactToComment(...)` | 1110-1115 / 1145-1150 | Identical bodies: `POST /api/comments/{id}/reactions` with `reaction_type` + optional `award_points`. Both called. |

### 7.3 Dead private helpers / duplicated constants

| Item | Line | Note |
|---|---|---|
| `JSONObject.arrayOrNull(key)` | 3084-3087 | Defined, never called anywhere. |
| `WEBSITE_UPLOAD_CHUNK_BYTES` | `NovalPieApi.kt:3335` and `ui/UploadPresentation.kt:3` | Two independent definitions (`Int` vs `Long`) of the same 5 MiB value. `UploadPresentationTest.kt:39` asserts the UI copy. |
| `model.WorkspaceTranslationJob` | `Models.kt:443-454` | **Not dead — local-only.** No normalizer produces it and no `/workspace/jobs` endpoint exists; it is persisted/loaded by `data/WorkspaceLocalStore.kt:36-56, 89` and surfaced through `WorkspaceState.jobs` (`NovalPieViewModel.kt:266`, mutated at 2650/2658). Same pattern as `WorkspaceLocalApiConfig` (`Models.kt:432-441`). Flagged only so a refactor does not mistake it for an orphan API model. |
| `proxyProvider` constructor parameter | `NovalPieApi.kt:100` | Never supplied in production (`NovalPieViewModel.kt:438-448` passes only `proxySelectorProvider`); the `explicitProxy` branch in `execute()`/`executeExternal()` is reachable only from tests. |

### 7.4 Endpoints the docs mention that are NOT implemented

Extracted every `METHOD /path` pair from `README.md` and `docs/*.md` and diffed against the implemented
path set. Only two doc-mentioned endpoints are absent from the API, and both are documented as
**deliberately** avoided:

| Doc-mentioned endpoint | Doc location | Reason it is absent |
|---|---|---|
| `GET /api/comments/book-reviews?page=...&book_id=...` | `docs/LIVE_SITE_ROUTE_API_MATRIX.md:919-921` | Documented negative evidence: it "returns the global forum review feed rather than a reliable per-book comment list, so the native detail page uses `type=book`". Intentional — `bookComments()` uses `/api/comments?type=book`. |
| `GET /comments?...` | `docs/LIVE_SITE_ROUTE_API_MATRIX.md` (same negative-evidence block) | A Nuxt **page** route, not JSON. Correctly not treated as an API. |

Every other endpoint named in the docs is implemented. The implemented set is **74 distinct literal path
templates** (68 under `/api/*`, 6 under `/workspace/*`), abbreviated below:

```
/api/admin/baseurl-rules            /api/messages/{id}/read              /api/users/me
/api/admin/cookie-config            /api/messages/{id}/star              /api/users/me/activities
/api/admin/key-management           /api/messages/conversations          /api/users/me/avatar
/api/admin/operation-logs           /api/messages/read                   /api/users/me/chapters/{id}
/api/admin/overview                 /api/messages/settings               /api/users/me/chapters/{id}/illustrations
/api/admin/review-requests          /api/messages/stats                  /api/users/me/chapters/{id}/illustrations/{imageId}
/api/admin/review-settings          /api/novels/{id}/chapters            /api/users/me/chapters/append
/api/admin/scheduler-logs           /api/novels/{id}/detail              /api/users/me/chapters/batch-delete
/api/admin/shop/items               /api/novels/{id}/photo               /api/users/me/chapters/insert
/api/chapters/{id}/content          /api/political-exams/sessions        /api/users/me/chapters/reorder
/api/comments                       /api/political-exams/sessions/submit /api/users/me/checkins
/api/comments/{id}/likes            /api/posts                          /api/users/me/checkins/settings
/api/comments/{id}/reactions        /api/posts/{id}                     /api/users/me/checkins/stats
/api/comments/{id}/replies          /api/posts/{id}/comments            /api/users/me/novels
/api/comments/{pid}/replies/{rid}/reactions  /api/posts/{id}/likes      /api/users/me/novels/{id}
/api/favorites                      /api/posts/{id}/reactions           /api/users/me/novels/{id}/permissions
/api/favorites/groups               /api/reader/session-key              /api/users/me/novels/{id}/permissions/check
/api/favorites/status               /api/search                          /api/users/me/novels/{id}/transfers
/api/messages                       /api/tags                            /api/users/me/novels/{id}/translation-requests
/api/messages/{id}                  /api/uploads/books                   /api/users/me/verifies/adult
                                    /api/uploads/chunks                  /api/users/{id}  (+ /activities /checkins /checkins/settings /checkins/stats /novels)
                                    /api/uploads/epubs
/workspace/apis  /workspace/apis/{id}  /workspace/cookie-config  /workspace/cookie-status  /workspace/stats  /workspace/translator-health
```

Note there is **no** login/logout/register/session endpoint in the API layer at all — authentication is
entirely delegated to the WebView (cookie sharing via `CookieManager`) plus a bearer token from
`AuthSessionStore`. Any refactor must preserve that: the native client never authenticates directly.

### 7.5 Behaviours that are easy to lose in a refactor (checklist)

1. `request()` **drops blank query params** (1595). Many call sites rely on this; if you switch to a
   typed query builder, blank strings must still be omitted, or `/api/admin/*` filters will change semantics.
2. `POST`/`PUT`/`PATCH` with a null body still send `{}` (1606).
3. `DELETE` sends a JSON body for 5 endpoints (`/api/messages/{id}`, `/api/messages`,
   `/api/admin/cookie-config`, `/workspace/cookie-config`) but is bodyless for the other 6.
4. `requestBody()` treats every non-`"PUT"` method as POST (1579-1582).
5. `execute()` reads the error body then discards it (1662-1665) — server error text never reaches the UI.
6. `updateCurrentUser()` returns its input argument, not the parsed response (699).
7. `unwrapObject` deliberately defers `data`/`result` to a second pass (3050-3053).
8. `extractArray`'s numeric-key fallback (3038-3044) handles `{"0":…,"1":…}` responses.
9. `stringOrNull` rejects the literal text `"null"` (3091).
10. `normalizeAssetUrl` nulls out bare `images.novelpia.com` URLs (3314-3321).
11. `normalizePoliticalExamSession`'s 50→40/10 split hack (1757-1760).
12. `normalizeChapters` stable sort with `Int.MAX_VALUE` for missing numbers (2015).
13. `normalizeChapterComments` flattens the reply tree depth-first with parent fallbacks (2679-2706).
14. `appendManagedChapters` chunk thresholds (50 chapters / 2,500,000 chars) and per-chunk `chapters_md5`.
15. `uploadBook` sends `tags` as a **comma-joined string**, while `updateManagedBook` sends a **JSON array**.
16. `createForumPost` mixes snake_case (post) and camelCase (poll) body keys.
17. `createWorkspaceApi`/`updateWorkspaceApi` send the API key under the key `key`, not `api_key`.
18. Reader crypto: see §3 — every constant, concatenation order, hex casing, and the tag-append
    requirement is load-bearing.
