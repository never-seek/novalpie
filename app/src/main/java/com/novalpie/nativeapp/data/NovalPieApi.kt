package com.novalpie.nativeapp.data

import android.util.Base64
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.ChapterIllustrationMutationResult
import com.novalpie.nativeapp.model.ChapterIllustrationPage
import com.novalpie.nativeapp.model.BookEditInfo
import com.novalpie.nativeapp.model.BookEditPermissions
import com.novalpie.nativeapp.model.BookEditRequest
import com.novalpie.nativeapp.model.BookEditResult
import com.novalpie.nativeapp.model.DirectMessage
import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.FavoriteGroup
import com.novalpie.nativeapp.model.FavoritePage
import com.novalpie.nativeapp.model.FavoriteStatus
import com.novalpie.nativeapp.model.ForumActionResult
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumCreateRequest
import com.novalpie.nativeapp.model.ForumCreateResult
import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.ForumPostPage
import com.novalpie.nativeapp.model.ForumPostDetail
import com.novalpie.nativeapp.model.MessageStats
import com.novalpie.nativeapp.model.MessageActionResult
import com.novalpie.nativeapp.model.MessagePage
import com.novalpie.nativeapp.model.MessagePagination
import com.novalpie.nativeapp.model.MessageQuery
import com.novalpie.nativeapp.model.MessageSettings
import com.novalpie.nativeapp.model.ManagedBookAccessPolicy
import com.novalpie.nativeapp.model.ManagedBookTransferResult
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.SearchPage
import com.novalpie.nativeapp.model.ShopItem
import com.novalpie.nativeapp.model.ShopPurchaseResult
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.SiteMessage
import com.novalpie.nativeapp.model.ParsedEpub
import com.novalpie.nativeapp.model.TerminologyEntry
import com.novalpie.nativeapp.model.TerminologyPage
import com.novalpie.nativeapp.model.PoliticalExamAnswers
import com.novalpie.nativeapp.model.PoliticalExamDetail
import com.novalpie.nativeapp.model.PoliticalExamPaper
import com.novalpie.nativeapp.model.PoliticalExamQuestion
import com.novalpie.nativeapp.model.PoliticalExamResult
import com.novalpie.nativeapp.model.PoliticalExamSession
import com.novalpie.nativeapp.model.UploadActionResult
import com.novalpie.nativeapp.model.UploadBookRequest
import com.novalpie.nativeapp.model.UploadChapter
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserBadge
import com.novalpie.nativeapp.model.UserCheckinAction
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserContentActivityFeed
import com.novalpie.nativeapp.model.UserCheckinRecord
import com.novalpie.nativeapp.model.UserCheckinSettings
import com.novalpie.nativeapp.model.UserInventory
import com.novalpie.nativeapp.model.UserInventoryItem
import com.novalpie.nativeapp.model.UserQuizRewardStatus
import com.novalpie.nativeapp.model.WorkspaceActionResult
import com.novalpie.nativeapp.model.WorkspaceApiConfig
import com.novalpie.nativeapp.model.WorkspaceApiStatus
import com.novalpie.nativeapp.model.WorkspaceCookieConfig
import com.novalpie.nativeapp.model.WorkspaceCookieConfigs
import com.novalpie.nativeapp.model.WorkspaceCookieStatus
import com.novalpie.nativeapp.model.WorkspaceHealth
import com.novalpie.nativeapp.model.WorkspaceTranslatorHealth
import com.novalpie.nativeapp.model.AdminBaseUrlRule
import com.novalpie.nativeapp.model.AdminCookieConfig
import com.novalpie.nativeapp.model.AdminDailyCount
import com.novalpie.nativeapp.model.AdminKeyItem
import com.novalpie.nativeapp.model.AdminOperationLog
import com.novalpie.nativeapp.model.AdminOperationLogPage
import com.novalpie.nativeapp.model.AdminOverviewStats
import com.novalpie.nativeapp.model.AdminReviewRequest
import com.novalpie.nativeapp.model.AdminReviewSettings
import com.novalpie.nativeapp.model.AdminSchedulerLogs
import com.novalpie.nativeapp.model.AdminShopItem
import com.novalpie.nativeapp.model.AuthActionResult
import com.novalpie.nativeapp.model.AuthSession
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EpubDownloadTicket(
    val fileName: String,
    val userPointsAfter: Long? = null,
    val hasDownloadPurchase: Boolean = false,
)

/**
 * The source normally applies `platform` server-side, but older responses and proxy caches have
 * occasionally returned mixed cards. Keep the native source selector strict at the response
 * boundary without changing the source's pagination envelope.
 */
internal fun filterSearchPageByPlatform(
    page: SearchPage,
    source: String?,
    platform: String?,
): SearchPage {
    val requested = canonicalSearchPlatform(platform ?: source) ?: return page
    return page.copy(items = page.items.filter { canonicalSearchPlatform(it.platform) == requested })
}

private fun canonicalSearchPlatform(value: String?): String? {
    val normalized = value
        ?.trim()
        ?.lowercase()
        ?.filter(Char::isLetterOrDigit)
        ?.takeIf(String::isNotBlank)
        ?: return null
    return when (normalized) {
        "all", "any" -> null
        "novelpia" -> "novelpia"
        "upload", "uploaded", "userupload", "useruploaded" -> "upload"
        else -> normalized
    }
}

class NovalPieApi(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://novalpie.cc",
    private val cookieProvider: () -> String? = { null },
    private val authTokenProvider: () -> String? = { null },
    private val proxyProvider: () -> Proxy? = { null },
    private val proxySelectorProvider: () -> ProxySelector? = { null }
) {
    @Volatile
    private var cachedReaderSession: ReaderSessionKey? = null
    private val readerSessionLock = Any()

    /** Current source contract: POST /api/sessions with a provider-agnostic CAPTCHA token. */
    suspend fun loginPassword(
        username: String,
        password: String,
        captchaToken: String
    ): AuthSession = withContext(Dispatchers.IO) {
        normalizeAuthSession(
            postUnauthenticated(
                "/api/sessions",
                JSONObject()
                    .put("username", username.trim())
                    .put("password", password)
                    .put("turnstile_token", captchaToken.trim())
            )
        )
    }

    suspend fun sendLoginVerificationCode(email: String, captchaToken: String): AuthActionResult =
        withContext(Dispatchers.IO) {
            normalizeAuthAction(
                postUnauthenticated(
                    "/api/verification-codes/login",
                    JSONObject()
                        .put("email", email.trim())
                        .put("turnstile_token", captchaToken.trim())
                )
            )
        }

    suspend fun loginWithVerificationCode(
        email: String,
        code: String,
        captchaToken: String
    ): AuthSession = withContext(Dispatchers.IO) {
        normalizeAuthSession(
            postUnauthenticated(
                "/api/verification-codes/login/verify",
                JSONObject()
                    .put("email", email.trim())
                    .put("code", code.trim())
                    .put("turnstile_token", captchaToken.trim())
            )
        )
    }

    suspend fun sendRegistrationVerificationCode(email: String, captchaToken: String): AuthActionResult =
        withContext(Dispatchers.IO) {
            normalizeAuthAction(
                postUnauthenticated(
                    "/api/verification-codes/email",
                    JSONObject()
                        .put("email", email.trim())
                        .put("turnstile_token", captchaToken.trim())
                )
            )
        }

    suspend fun verifyRegistrationEmail(email: String, code: String): AuthActionResult = withContext(Dispatchers.IO) {
        normalizeAuthAction(
            postUnauthenticated(
                "/api/verification-codes/email/verify",
                JSONObject().put("email", email.trim()).put("code", code.trim())
            )
        )
    }

    suspend fun registerAccount(username: String, email: String, password: String): AuthSession =
        withContext(Dispatchers.IO) {
            normalizeAuthSession(
                postUnauthenticated(
                    "/api/users",
                    JSONObject()
                        .put("username", username.trim())
                        .put("email", email.trim())
                        .put("password", password)
                )
            )
        }

    suspend fun requestPasswordReset(email: String): AuthActionResult = withContext(Dispatchers.IO) {
        normalizeAuthAction(postUnauthenticated("/api/password-resets", JSONObject().put("email", email.trim())))
    }

    suspend fun resetPassword(token: String, password: String): AuthActionResult = withContext(Dispatchers.IO) {
        normalizeAuthAction(
            putUnauthenticated(
                "/api/password-resets",
                JSONObject().put("token", token.trim()).put("password", password)
            )
        )
    }

    suspend fun search(
        keyword: String,
        page: Int = 1,
        limit: Int = 20,
        sortBy: String = "relevance",
        sortOrder: String = "desc",
        scope: String = "all",
        matchType: String = "fuzzy_strict",
        adultFilter: String = "unrestricted",
        source: String = "",
        minWordCount: Long? = null,
        maxWordCount: Long? = null,
        requiredTags: List<String> = emptyList(),
        blockedTags: List<String> = emptyList(),
        tagsAny: List<String> = emptyList(),
        tagsExpression: String? = null,
        blockedTerms: List<String> = emptyList(),
        platform: String? = null,
        novelType: String? = null,
        status: String? = null
    ): List<NovelCard> = searchPage(
        keyword = keyword,
        page = page,
        limit = limit,
        sortBy = sortBy,
        sortOrder = sortOrder,
        scope = scope,
        matchType = matchType,
        adultFilter = adultFilter,
        source = source,
        minWordCount = minWordCount,
        maxWordCount = maxWordCount,
        requiredTags = requiredTags,
        blockedTags = blockedTags,
        tagsAny = tagsAny,
        tagsExpression = tagsExpression,
        blockedTerms = blockedTerms,
        platform = platform,
        novelType = novelType,
        status = status
    ).items

    /**
     * Preserves the live `/api/search` pagination envelope instead of deriving it from a short
     * page. The public `search` method remains as a list-only compatibility wrapper.
     */
    suspend fun searchPage(
        keyword: String,
        page: Int = 1,
        limit: Int = 20,
        sortBy: String = "relevance",
        sortOrder: String = "desc",
        scope: String = "all",
        matchType: String = "fuzzy_strict",
        adultFilter: String = "unrestricted",
        source: String = "",
        minWordCount: Long? = null,
        maxWordCount: Long? = null,
        requiredTags: List<String> = emptyList(),
        blockedTags: List<String> = emptyList(),
        tagsAny: List<String> = emptyList(),
        tagsExpression: String? = null,
        blockedTerms: List<String> = emptyList(),
        platform: String? = null,
        novelType: String? = null,
        status: String? = null
    ): SearchPage =
        withContext(Dispatchers.IO) {
            val params = mutableMapOf(
                "page" to page.toString(),
                "limit" to limit.toString(),
                "sort_by" to sortBy,
                "sort_order" to sortOrder,
                "scope" to scope,
                "match_type" to matchType,
                "adult_filter" to adultFilter
            )
            keyword.trim().takeIf { it.isNotEmpty() }?.let { params["q"] = it }
            // The search UI calls this control "source", but the live website sends its
            // NovelPia/upload value as `platform`; `source` is ignored by `/api/search`.
            // Advanced syntax wins when it explicitly supplied a platform, including `all` which
            // intentionally clears the basic source selector. Older persisted settings used the
            // same `all` sentinel, and the website interprets it as a nonexistent platform.
            val sourcePlatform = if (platform != null) {
                platform.trim().takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
            } else {
                source.trim().takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
            }
            sourcePlatform?.let { params["platform"] = it }
            minWordCount?.takeIf { it > 0 }?.let { params["min_word_count"] = it.toString() }
            maxWordCount?.takeIf { it > 0 }?.let { params["max_word_count"] = it.toString() }
            requiredTags.toSearchTagParam()?.let { params["tags"] = it }
            blockedTags.toSearchTagParam()?.let { params["blocked_tags"] = it }
            tagsAny.toSearchTagParam()?.let { params["tags_any"] = it }
            tagsExpression?.trim()?.takeIf { it.isNotEmpty() }?.let { params["tags_expr"] = it }
            blockedTerms.toSearchTermParam()?.let { params["blocked_terms"] = it }
            novelType?.trim()?.takeIf { it.isNotEmpty() }?.let { params["type"] = it }
            status?.trim()?.takeIf { it.isNotEmpty() }?.let { params["status"] = it }
            val raw = get(
                "/api/search",
                params
            )
            filterSearchPageByPlatform(
                page = normalizeSearchPage(raw, requestedPage = page, requestedLimit = limit),
                source = source,
                platform = platform,
            )
        }

    private fun List<String>.toSearchTagParam(): String? =
        asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .joinToString(",")
            .takeIf(String::isNotBlank)

    private fun List<String>.toSearchTermParam(): String? =
        asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .joinToString(",")
            .takeIf(String::isNotBlank)

    /**
     * Requests the source site's normal EPUB authorization. The server performs the points and
     * permission checks; the native client only consumes the returned TXT ticket afterwards.
     */
    suspend fun requestEpubDownload(bookId: Long): EpubDownloadTicket =
        requestDownloadAuthorization(bookId, downloadType = "epub")

    /** Requests the source site's TXT authorization; its returned file is written natively. */
    suspend fun requestTxtDownload(bookId: Long): EpubDownloadTicket =
        requestDownloadAuthorization(bookId, downloadType = "txt")

    private suspend fun requestDownloadAuthorization(
        bookId: Long,
        downloadType: String,
    ): EpubDownloadTicket = withContext(Dispatchers.IO) {
        require(bookId > 0) { "书籍 ID 无效" }
        normalizeEpubDownloadTicket(
            post(
                "/api/downloads",
                JSONObject()
                    .put("novel_id", bookId)
                    .put("download_type", downloadType)
            )
        )
    }

    /** Streams the authorized source TXT without materializing the complete book as a String. */
    suspend fun streamDownloadFile(
        fileName: String,
        consumer: suspend (InputStream) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val safeName = fileName.trim()
        require(safeName.isNotEmpty()) { "下载文件名为空" }
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("api")
            .addPathSegment("downloads")
            .addPathSegment(safeName)
            .build()
        streamResponse(
            label = "/downloads/$safeName",
            requestBuilder = baseRequestBuilder(url.toString()).header("accept", "text/plain, */*"),
            consumer = { input, _ -> consumer(input) },
            callTimeoutSeconds = EPUB_DOWNLOAD_CALL_TIMEOUT_SECONDS,
            readTimeoutSeconds = EPUB_DOWNLOAD_READ_TIMEOUT_SECONDS,
        )
    }

    /** Streams an original illustration and exposes its content type for lossless EPUB output. */
    suspend fun streamAsset(
        url: String,
        consumer: suspend (InputStream, String?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val resolved = resolveAssetUrl(url)
            ?: throw IOException("插图链接无效")
        streamResponse(
            label = resolved.toString(),
            requestBuilder = baseRequestBuilder(resolved.toString()).header("accept", "*/*"),
            consumer = consumer,
            callTimeoutSeconds = ASSET_CALL_TIMEOUT_SECONDS,
            readTimeoutSeconds = ASSET_READ_TIMEOUT_SECONDS,
        )
    }

    suspend fun currentUser(): UserProfile = withContext(Dispatchers.IO) {
        normalizeUser(get("/api/users/me"))
    }

    /**
     * Source account cosmetics are exposed separately from the profile payload.  This stays
     * read-only: equipping an item is a distinct, user-confirmed write action.
     */
    suspend fun currentUserInventory(): UserInventory = withContext(Dispatchers.IO) {
        normalizeUserInventory(get("/api/users/me/inventory"))
    }

    /** The ordinary storefront; do not substitute the administrator-only shop route here. */
    suspend fun shopItems(): List<ShopItem> = withContext(Dispatchers.IO) {
        normalizeShopItems(get("/api/shop/items"))
    }

    /**
     * Mirrors the website's cosmetic action contract. Callers keep the UI disabled until the
     * following inventory refresh completes, so the server remains the source of truth.
     */
    suspend fun setCurrentUserEquipment(itemId: Long, action: String): UserCheckinAction =
        withContext(Dispatchers.IO) {
            require(itemId > 0) { "shop item id must be positive" }
            require(action in setOf("equip", "unequip")) { "unsupported equipment action" }
            normalizeAdminAction(
                post(
                    "/api/users/me/equipment",
                    JSONObject()
                        .put("item_id", itemId)
                        .put("action", action)
                )
            )
        }

    suspend fun purchaseShopItem(itemId: Long): ShopPurchaseResult = withContext(Dispatchers.IO) {
        require(itemId > 0) { "shop item id must be positive" }
        normalizeShopPurchase(post("/api/shop/purchases", JSONObject().put("item_id", itemId)))
    }

    /** Read-only state for the source site's account quiz reward. */
    suspend fun currentUserQuizRewardStatus(): UserQuizRewardStatus = withContext(Dispatchers.IO) {
        normalizeUserQuizRewardStatus(get("/api/users/me/quiz-reward"))
    }

    /** The source's owner profile loads its uploaded books from the authenticated books endpoint. */
    suspend fun currentUserUploadedBooks(): List<NovelCard> = withContext(Dispatchers.IO) {
        normalizeNovelList(
            get("/api/users/me/books"),
            "uploads",
            "items",
            "novels",
            "books",
            "data",
            "result"
        )
    }

    suspend fun userProfile(userId: Long): UserProfile = withContext(Dispatchers.IO) {
        require(userId > 0) { "userId must be positive" }
        normalizeUser(get("/api/users/$userId"))
    }

    suspend fun userActivities(
        userId: Long? = null,
        type: String = "",
        page: Int = 1,
        limit: Int = 100
    ): List<UserActivity> = withContext(Dispatchers.IO) {
        val path = userId?.takeIf { it > 0 }?.let { "/api/users/$it/activities" }
            ?: "/api/users/me/activities"
        normalizeUserActivities(
            get(
                path,
                mapOf(
                    "type" to type,
                    "page" to page.coerceAtLeast(1).toString(),
                    // The source ActivityTab requests its first window with limit=200.
                    // Keep the public helper's 100 default, but do not silently clamp an
                    // explicit source-sized request back to 100.
                    "limit" to limit.coerceIn(1, 200).toString()
                )
            )
        )
    }

    /**
     * The legacy `/users/{id}/activities` route is still advertised by the web bundle, but the
     * live Laravel service returns an empty or unimplemented response for it.  The website's
     * visible user content is instead backed by these three user-filtered feeds.  Keep all of
     * them here so a temporary failure in one feed cannot turn the whole profile timeline blank.
     */
    suspend fun userContentActivities(
        userId: Long,
        page: Int = 1,
        limit: Int = 100
    ): List<UserActivity> = userContentActivityFeed(userId, page, limit).activities

    /**
     * Preserves source pagination totals for profile counters while still isolating a transient
     * failure in any single feed. The visible timeline remains sorted across all feeds.
     */
    suspend fun userContentActivityFeed(
        userId: Long,
        page: Int = 1,
        limit: Int = 100
    ): UserContentActivityFeed = withContext(Dispatchers.IO) {
        require(userId > 0) { "userId must be positive" }
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceIn(1, 200)
        val userFeedParams = linkedMapOf(
            "page" to safePage.toString(),
            "limit" to safeLimit.toString(),
            "user_id" to userId.toString()
        )

        coroutineScope {
            // This is the canonical source ActivityTab contract. Some current Laravel
            // deployments still answer 404, so it is intentionally merged with the older
            // user-filtered feeds below instead of being treated as the only source.
            val canonical = async {
                captureUserActivityFeed {
                    normalizeCanonicalUserActivityFeed(
                        get(
                            "/api/users/$userId/activities",
                            mapOf(
                                "page" to safePage.toString(),
                                "limit" to safeLimit.toString(),
                            )
                        )
                    )
                }
            }
            val posts = async {
                captureUserActivityFeed {
                    normalizeUserPostActivityFeed(get("/api/posts", userFeedParams))
                }
            }
            val comments = async {
                captureUserActivityFeed {
                    normalizeUserPostCommentActivityFeed(get("/api/posts/comments", userFeedParams))
                }
            }
            val reviews = async {
                captureUserActivityFeed {
                    normalizeUserBookReviewActivityFeed(
                        get(
                            "/api/comments/book-reviews",
                            userFeedParams + ("hide_spoilers" to "1")
                        )
                    )
                }
            }

            val canonicalResult = canonical.await()
            val postResult = posts.await()
            val commentResult = comments.await()
            val reviewResult = reviews.await()
            val results = listOf(canonicalResult, postResult, commentResult, reviewResult)
            val availableActivities = results.flatMap { it.getOrNull()?.activities.orEmpty() }
            if (availableActivities.isEmpty()) {
                results.mapNotNull(Result<UserContentActivityFeed>::exceptionOrNull).firstOrNull()?.let { throw it }
            }
            UserContentActivityFeed(
                activities = mergeUserContentActivities(availableActivities, safeLimit),
                postCount = canonicalResult.getOrNull()?.postCount
                    ?: postResult.getOrNull()?.postCount,
                forumCommentCount = canonicalResult.getOrNull()?.forumCommentCount
                    ?: commentResult.getOrNull()?.forumCommentCount,
                bookReviewCount = canonicalResult.getOrNull()?.bookReviewCount
                    ?: reviewResult.getOrNull()?.bookReviewCount
            )
        }
    }

    suspend fun userNovels(userId: Long? = null): List<NovelCard> = withContext(Dispatchers.IO) {
        val path = userId?.takeIf { it > 0 }?.let { "/api/users/$it/novels" }
            ?: "/api/users/me/novels"
        normalizeNovelList(get(path))
    }

    suspend fun managedBookInfo(bookId: Long): BookEditInfo = withContext(Dispatchers.IO) {
        require(bookId > 0) { "bookId must be positive" }
        val source = unwrapObject(get("/api/novels/$bookId/detail"), "data", "novel", "result")
        val spans = source.firstStringOrNull("spans", "status").orEmpty()
        BookEditInfo(
            id = source.longOrNull("id") ?: source.longOrNull("novel_id") ?: bookId,
            title = source.firstStringOrNull("title", "name").orEmpty(),
            titleTranslation = source.firstStringOrNull("true_name", "trueName", "title_translation").orEmpty(),
            authorName = source.firstStringOrNull("author_name", "authorName", "author").orEmpty(),
            description = source.firstStringOrNull("description", "summary").orEmpty(),
            source = source.firstStringOrNull("source").orEmpty(),
            sourceUrl = source.firstStringOrNull("source_url", "sourceUrl").orEmpty(),
            language = source.firstStringOrNull("language").orEmpty().ifBlank { "zh" },
            status = if (spans.contains("完结")) "已完结" else "连载中",
            isAdult = source.firstBooleanOrNull("is_adult", "isAdult") ?: spans.contains("19"),
            photoUrl = normalizeAssetUrl(source.firstStringOrNull("photo_url", "photoUrl", "cover_url", "coverUrl")).orEmpty(),
            tags = normalizeEditableBookTags(source)
        )
    }

    suspend fun managedBookPermissions(bookId: Long): BookEditPermissions = withContext(Dispatchers.IO) {
        require(bookId > 0) { "bookId must be positive" }
        val source = unwrapObject(
            get("/api/users/me/novels/$bookId/permissions/check"),
            "permissions",
            "data",
            "result"
        )
        BookEditPermissions(
            title = source.firstBooleanOrNull("title") ?: false,
            titleTranslation = source.firstBooleanOrNull("true_name", "title_translation") ?: false,
            authorName = source.firstBooleanOrNull("author_name") ?: false,
            description = source.firstBooleanOrNull("description") ?: false,
            source = source.firstBooleanOrNull("source") ?: false,
            sourceUrl = source.firstBooleanOrNull("source_url") ?: false,
            language = source.firstBooleanOrNull("language") ?: false,
            isAdult = source.firstBooleanOrNull("is_adult") ?: false,
            photoUrl = source.firstBooleanOrNull("photo_url") ?: false,
            spans = source.firstBooleanOrNull("spans") ?: false,
            tags = source.firstBooleanOrNull("tags") ?: false
        )
    }

    suspend fun updateManagedBook(bookId: Long, request: BookEditRequest): BookEditResult = withContext(Dispatchers.IO) {
        require(bookId > 0) { "bookId must be positive" }
        require(request.title.trim().isNotBlank()) { "book title is required" }
        require(request.authorName.trim().isNotBlank()) { "book author is required" }
        var spans = request.status.trim().ifBlank { "连载中" }
        if (request.isAdult && !spans.contains("19")) spans = "19 $spans"
        val body = JSONObject()
            .put("title", request.title.trim())
            .put("title_translation", request.titleTranslation.trim().takeIf(String::isNotBlank) ?: JSONObject.NULL)
            .put("author_name", request.authorName.trim())
            .put("description", request.description.trim().takeIf(String::isNotBlank) ?: JSONObject.NULL)
            .put("source", request.source.trim().takeIf(String::isNotBlank) ?: JSONObject.NULL)
            .put("source_url", request.sourceUrl.trim().takeIf(String::isNotBlank) ?: JSONObject.NULL)
            .put("language", request.language.trim().ifBlank { "zh" })
            .put("spans", spans)
            .put("is_adult", if (request.isAdult) 1 else 0)
            .put("photo_url", request.photoUrl.trim().takeIf(String::isNotBlank) ?: JSONObject.NULL)
            .put("tags", JSONArray(request.tags.map(String::trim).filter(String::isNotBlank).distinct()))
        val source = unwrapObject(patch("/api/users/me/novels/$bookId", body), "data", "result")
        BookEditResult(
            success = source.firstBooleanOrNull("success", "ok") ?: true,
            message = source.firstStringOrNull("message", "msg", "detail"),
            failedFields = (source.opt("failed_fields") as? JSONArray)?.toList()?.mapNotNull { it?.toString() }.orEmpty(),
            errors = (source.opt("errors") as? JSONArray)?.toList()?.mapNotNull { it?.toString() }.orEmpty()
        )
    }

    suspend fun transferManagedBook(bookId: Long, identifier: String): ManagedBookTransferResult = withContext(Dispatchers.IO) {
        require(bookId > 0) { "bookId must be positive" }
        val target = identifier.trim()
        require(target.isNotBlank()) { "transfer target is required" }
        normalizeManagedBookTransfer(
            post(
                "/api/users/me/novels/$bookId/transfers",
                JSONObject().put("identifier", target)
            )
        )
    }

    suspend fun updateManagedBookAccessPolicy(
        bookId: Long,
        policy: ManagedBookAccessPolicy
    ): ForumActionResult = withContext(Dispatchers.IO) {
        require(bookId > 0) { "bookId must be positive" }
        val download = if (policy.allowDownload) {
            normalizedThreshold(policy.downloadThresholdType, policy.downloadThresholdValue, "download")
        } else {
            "none" to 0
        }
        val read = normalizedThreshold(policy.readThresholdType, policy.readThresholdValue, "read")
        normalizeForumActionResult(
            patch(
                "/api/users/me/novels/$bookId/permissions",
                JSONObject()
                    .put("allow_download", if (policy.allowDownload) 1 else 0)
                    .put("download_threshold_type", download.first)
                    .put("download_threshold_value", download.second)
                    .put("read_threshold_type", read.first)
                    .put("read_threshold_value", read.second)
            )
        )
    }

    suspend fun uploadManagedBookCover(bookId: Long, file: UploadFileSource): String = withContext(Dispatchers.IO) {
        require(bookId > 0) { "bookId must be positive" }
        require(file.sizeBytes > 0L) { "cover file is empty" }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "cover",
                file.fileName,
                UploadRangeRequestBody(file, offset = 0L, length = file.sizeBytes)
            )
            .build()
        val source = unwrapObject(
            requestBody("/api/novels/$bookId/photo", "PUT", multipart),
            "data",
            "result"
        )
        normalizeAssetUrl(source.firstStringOrNull("photo_url", "photoUrl", "url"))
            ?: throw IOException("cover upload did not return photo_url")
    }

    suspend fun userCheckinRecords(
        userId: Long? = null,
        startDate: String,
        endDate: String
    ): List<UserCheckinRecord> = withContext(Dispatchers.IO) {
        val path = userId?.takeIf { it > 0 }?.let { "/api/users/$it/checkins" }
            ?: "/api/users/me/checkins"
        normalizeUserCheckinRecords(
            get(path, mapOf("start_date" to startDate, "end_date" to endDate))
        )
    }

    suspend fun userCheckinSettings(userId: Long? = null): UserCheckinSettings =
        withContext(Dispatchers.IO) {
            val publicUserId = userId?.takeIf { it > 0 }
            val path = publicUserId?.let { "/api/users/$it/checkins/settings" }
                ?: "/api/users/me/checkins/settings"
            val params = publicUserId?.let { mapOf("user_id" to it.toString()) }.orEmpty()
            val source = unwrapObject(get(path, params), "checkin_settings", "settings", "data", "result")
            UserCheckinSettings(
                showCheckin = source.firstBooleanOrNull("show_checkin", "showCheckin") ?: true,
                autoCheckin = source.firstBooleanOrNull("auto_checkin", "autoCheckin") ?: false
            )
        }

    suspend fun verifyCurrentUserAdult(birthYear: Int): UserCheckinAction = withContext(Dispatchers.IO) {
        require(birthYear in 1900..2100) { "birthYear is out of range" }
        val source = unwrapObject(
            post("/api/users/me/verifies/adult", JSONObject().put("birth_year", birthYear)),
            "data",
            "result"
        )
        UserCheckinAction(
            success = source.firstBooleanOrNull("success", "ok") ?: true,
            message = source.firstStringOrNull("message", "msg", "detail")
        )
    }

    suspend fun uploadCurrentUserAvatar(file: UploadFileSource): UserCheckinAction = withContext(Dispatchers.IO) {
        require(file.sizeBytes > 0L) { "avatar file is empty" }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "avatar",
                file.fileName,
                UploadRangeRequestBody(file, offset = 0L, length = file.sizeBytes)
            )
            .build()
        val source = unwrapObject(
            requestBody("/api/users/me/avatar", "POST", multipart),
            "data",
            "result"
        )
        UserCheckinAction(
            success = source.firstBooleanOrNull("success", "ok") ?: true,
            message = source.firstStringOrNull("message", "msg", "detail")
        )
    }

    suspend fun adminOverview(days: Int = 5): AdminOverviewStats = withContext(Dispatchers.IO) {
        val source = unwrapObject(
            get("/api/admin/overview", mapOf("days" to days.coerceIn(1, 90).toString())),
            "stats",
            "data",
            "result"
        )
        val daily = (source.opt("recent_user_daily") as? JSONArray)?.toList().orEmpty().mapNotNull { value ->
            val item = value as? JSONObject ?: return@mapNotNull null
            AdminDailyCount(
                date = item.firstStringOrNull("date", "day") ?: return@mapNotNull null,
                count = item.intOrNull("count") ?: 0
            )
        }
        AdminOverviewStats(
            pendingReviewTotal = source.intOrNull("pending_review_total") ?: 0,
            pendingReviewUpload = source.intOrNull("pending_review_upload") ?: 0,
            pendingReviewDelete = source.intOrNull("pending_review_delete") ?: 0,
            pendingKeys = source.intOrNull("pending_keys") ?: 0,
            approvedKeys = source.intOrNull("approved_keys") ?: 0,
            activeTranslators = source.intOrNull("active_translators") ?: 0,
            todayUsers = source.intOrNull("today_users") ?: 0,
            activeNovelTotal = source.intOrNull("novel_active_total") ?: 0,
            registeredUserTotal = source.intOrNull("user_registered_total") ?: 0,
            recentUserDaily = daily
        )
    }

    suspend fun adminReviewSettings(): AdminReviewSettings = withContext(Dispatchers.IO) {
        val source = unwrapObject(get("/api/admin/review-settings"), "settings", "data", "result")
        AdminReviewSettings(
            autoApproveUpload = source.firstBooleanOrNull("auto_approve_upload", "autoApproveUpload") ?: false,
            autoApproveDelete = source.firstBooleanOrNull("auto_approve_delete", "autoApproveDelete") ?: false
        )
    }

    suspend fun adminReviewRequests(
        type: String = "",
        status: String = "",
        keyword: String = ""
    ): List<AdminReviewRequest> = withContext(Dispatchers.IO) {
        extractArray(
            get(
                "/api/admin/review-requests",
                mapOf(
                    "page" to "1",
                    "page_size" to "100",
                    "type" to type,
                    "status" to status,
                    "q" to keyword
                )
            ),
            "list",
            "items",
            "data",
            "requests"
        ).mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            val user = source.opt("user") as? JSONObject
            val novel = source.opt("novel") as? JSONObject
            AdminReviewRequest(
                id = source.longOrNull("id") ?: return@mapNotNull null,
                type = source.firstStringOrNull("type", "request_type") ?: "unknown",
                status = source.firstStringOrNull("status", "review_status") ?: "pending",
                username = source.firstStringOrNull("username", "user_name", "userName")
                    ?: user?.firstStringOrNull("username", "name"),
                userId = source.longOrNull("user_id")
                    ?: source.longOrNull("userId")
                    ?: user?.longOrNull("id"),
                novelId = source.longOrNull("novel_id")
                    ?: source.longOrNull("novelId")
                    ?: novel?.longOrNull("id"),
                title = source.firstStringOrNull("title", "novel_title", "name")
                    ?: novel?.firstStringOrNull("title", "name"),
                reason = source.firstStringOrNull("reason", "description", "message"),
                createdAt = source.firstStringOrNull("created_at", "createdAt")
            )
        }
    }

    suspend fun adminKeys(): List<AdminKeyItem> = withContext(Dispatchers.IO) {
        extractArray(get("/api/admin/key-management"), "data", "items", "keys", "list").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            AdminKeyItem(
                id = source.longOrNull("id") ?: return@mapNotNull null,
                name = source.firstStringOrNull("name", "model", "provider_name") ?: "Key",
                model = source.stringOrNull("model"),
                providerName = source.firstStringOrNull("provider_name", "providerName", "provider"),
                approvalStatus = source.firstStringOrNull("approval_status", "approvalStatus", "status") ?: "pending",
                baseUrl = source.firstStringOrNull("base_url", "baseUrl"),
                createdAt = source.firstStringOrNull("created_at", "createdAt")
            )
        }
    }

    suspend fun adminOperationLogs(
        page: Int = 1,
        action: String = "",
        status: String = "",
        userId: String = "",
        novelId: String = "",
        keyword: String = "",
        startDate: String = "",
        endDate: String = ""
    ): AdminOperationLogPage = withContext(Dispatchers.IO) {
        val raw = get(
            "/api/admin/operation-logs",
            mapOf(
                "page" to page.coerceAtLeast(1).toString(),
                "page_size" to "20",
                "action" to action,
                "status" to status,
                "user_id" to userId,
                "novel_id" to novelId,
                "keyword" to keyword,
                "start_date" to startDate,
                "end_date" to endDate
            )
        )
        val source = unwrapObject(raw, "data", "result")
        val items = extractArray(raw, "logs", "items", "data", "list").mapNotNull { value ->
            val item = value as? JSONObject ?: return@mapNotNull null
            AdminOperationLog(
                id = item.longOrNull("id") ?: return@mapNotNull null,
                action = item.firstStringOrNull("action", "operation") ?: "unknown",
                status = item.firstStringOrNull("status", "state") ?: "unknown",
                userId = item.longOrNull("user_id") ?: item.longOrNull("userId"),
                username = item.firstStringOrNull("username", "user_name", "userName"),
                email = item.firstStringOrNull("email", "user_email", "userEmail"),
                novelId = item.longOrNull("novel_id") ?: item.longOrNull("novelId"),
                novelTitle = item.firstStringOrNull("novel_title", "novelTitle"),
                chapterId = item.longOrNull("chapter_id") ?: item.longOrNull("chapterId"),
                ipAddress = item.firstStringOrNull("ip_address", "ipAddress", "ip"),
                message = item.firstStringOrNull("message", "detail", "error_message"),
                content = item.firstStringOrNull("content", "request_content", "payload"),
                result = item.firstStringOrNull("result", "response", "operation_result"),
                userAgent = item.firstStringOrNull("user_agent", "userAgent"),
                createdAt = item.firstStringOrNull("created_at", "createdAt"),
                updatedAt = item.firstStringOrNull("updated_at", "updatedAt")
            )
        }
        AdminOperationLogPage(
            items = items,
            total = source.intOrNull("total") ?: items.size,
            totalPages = source.intOrNull("total_pages") ?: source.intOrNull("totalPages") ?: 1,
            actionTypes = (source.opt("action_types") as? JSONArray)?.toList().orEmpty()
                .mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
        )
    }

    suspend fun adminCookieConfigs(): List<AdminCookieConfig> = withContext(Dispatchers.IO) {
        extractArray(get("/api/admin/cookie-config"), "configs", "items", "data", "list").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            AdminCookieConfig(
                id = source.longOrNull("id") ?: return@mapNotNull null,
                configKey = source.firstStringOrNull("config_key", "configKey", "key") ?: "config",
                description = source.stringOrNull("description"),
                proxyIp = source.firstStringOrNull("proxy_ip", "proxyIp"),
                isActive = source.firstBooleanOrNull("is_active", "isActive", "active") ?: false,
                isHealthy = source.firstBooleanOrNull("is_healthy", "isHealthy"),
                lastError = source.firstStringOrNull("last_error", "lastError"),
                updatedAt = source.firstStringOrNull("updated_at", "updatedAt"),
                updatedByUsername = source.firstStringOrNull("updated_by_username", "updatedByUsername"),
                successCount = source.intOrNull("success_count") ?: 0,
                failCount = source.intOrNull("fail_count") ?: 0
            )
        }
    }

    suspend fun adminBaseUrlRules(): List<AdminBaseUrlRule> = withContext(Dispatchers.IO) {
        extractArray(get("/api/admin/baseurl-rules"), "data", "rules", "items", "list").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            AdminBaseUrlRule(
                id = source.longOrNull("id") ?: return@mapNotNull null,
                pattern = source.firstStringOrNull("pattern", "base_url", "baseUrl") ?: return@mapNotNull null,
                action = source.firstStringOrNull("action", "policy") ?: "manual",
                description = source.stringOrNull("description"),
                createdAt = source.firstStringOrNull("created_at", "createdAt")
            )
        }
    }

    suspend fun adminSchedulerLogs(lines: Int = 100): AdminSchedulerLogs = withContext(Dispatchers.IO) {
        val source = unwrapObject(
            get("/api/admin/scheduler-logs", mapOf("lines" to lines.coerceIn(10, 1000).toString())),
            "data",
            "result"
        )
        AdminSchedulerLogs(
            logs = (source.opt("logs") as? JSONArray)?.toList().orEmpty().mapNotNull { it?.toString() },
            totalLines = source.intOrNull("total_lines") ?: 0,
            fileSizeMb = source.opt("file_size_mb")?.let { value ->
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull()
                    else -> null
                }
            },
            lastModified = source.firstStringOrNull("last_modified", "lastModified")
        )
    }

    suspend fun adminShopItems(
        type: String = "",
        active: Boolean? = null,
        keyword: String = ""
    ): List<AdminShopItem> = withContext(Dispatchers.IO) {
        extractArray(
            get(
                "/api/admin/shop/items",
                mapOf(
                    "type" to type,
                    "is_active" to when (active) {
                        true -> "1"
                        false -> "0"
                        null -> ""
                    },
                    "keyword" to keyword,
                    "page" to "1",
                    "page_size" to "100"
                )
            ),
            "items",
            "data",
            "list"
        ).mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            AdminShopItem(
                id = source.longOrNull("id") ?: return@mapNotNull null,
                name = source.firstStringOrNull("name", "title") ?: "Item",
                description = source.stringOrNull("description"),
                price = source.longOrNull("price") ?: 0L,
                type = source.firstStringOrNull("type", "item_type") ?: "frame",
                imageUrl = normalizeAssetUrl(source.firstStringOrNull("image_url", "imageUrl")),
                badgeHtml = source.firstStringOrNull("badge_html", "badgeHtml"),
                badgeCss = source.firstStringOrNull("badge_css", "badgeCss"),
                isActive = source.firstBooleanOrNull("is_active", "isActive", "active") ?: false
            )
        }
    }

    suspend fun adminUpdateReviewSettings(
        autoApproveUpload: Boolean,
        autoApproveDelete: Boolean
    ): UserCheckinAction = withContext(Dispatchers.IO) {
        normalizeAdminAction(
            post(
                "/api/admin/review-settings",
                JSONObject()
                    .put("auto_approve_upload", autoApproveUpload)
                    .put("auto_approve_delete", autoApproveDelete)
            )
        )
    }

    suspend fun adminReviewAction(id: Long, action: String): UserCheckinAction = withContext(Dispatchers.IO) {
        require(id > 0) { "review id must be positive" }
        require(action in setOf("approve", "reject")) { "unsupported review action" }
        normalizeAdminAction(
            post(
                "/api/admin/review-requests",
                JSONObject().put("id", id).put("action", action)
            )
        )
    }

    suspend fun adminApproveAllReviews(
        type: String = "",
        status: String = "",
        keyword: String = ""
    ): UserCheckinAction = withContext(Dispatchers.IO) {
        val body = JSONObject().put("action", "approve_all")
        type.takeIf(String::isNotBlank)?.let { body.put("type", it) }
        status.takeIf(String::isNotBlank)?.let { body.put("status", it) }
        keyword.takeIf(String::isNotBlank)?.let { body.put("q", it) }
        normalizeAdminAction(post("/api/admin/review-requests", body))
    }

    suspend fun adminUpdateKeyStatus(id: Long, approvalStatus: String): UserCheckinAction =
        withContext(Dispatchers.IO) {
            require(id > 0) { "key id must be positive" }
            require(approvalStatus in setOf("pending", "approved", "rejected")) { "unsupported key status" }
            normalizeAdminAction(
                put(
                    "/api/admin/key-management",
                    JSONObject().put("id", id).put("approval_status", approvalStatus)
                )
            )
        }

    suspend fun adminDeleteKey(id: Long): UserCheckinAction = withContext(Dispatchers.IO) {
        require(id > 0) { "key id must be positive" }
        normalizeAdminAction(delete("/api/admin/key-management?id=$id"))
    }

    suspend fun adminSaveCookieConfig(
        config: AdminCookieConfig,
        cookieRaw: String?
    ): UserCheckinAction = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("description", config.description)
            .put("is_active", config.isActive)
        cookieRaw?.trim()?.takeIf(String::isNotBlank)?.let { body.put("cookie_raw", it) }
        config.proxyIp?.trim()?.takeIf(String::isNotBlank)?.let { body.put("proxy_ip", it) }
        val raw = if (config.id > 0) {
            body.put("id", config.id)
            put("/api/admin/cookie-config", body)
        } else {
            body.put("config_key", config.configKey)
            post("/api/admin/cookie-config", body)
        }
        normalizeAdminAction(raw)
    }

    suspend fun adminDeleteCookieConfig(id: Long): UserCheckinAction = withContext(Dispatchers.IO) {
        require(id > 0) { "cookie config id must be positive" }
        normalizeAdminAction(delete("/api/admin/cookie-config", JSONObject().put("id", id)))
    }

    suspend fun adminSaveBaseUrlRule(rule: AdminBaseUrlRule): UserCheckinAction = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("action", rule.action)
            .put("description", rule.description)
        val raw = if (rule.id > 0) {
            body.put("id", rule.id)
            put("/api/admin/baseurl-rules", body)
        } else {
            body.put("pattern", rule.pattern)
            post("/api/admin/baseurl-rules", body)
        }
        normalizeAdminAction(raw)
    }

    suspend fun adminDeleteBaseUrlRule(id: Long): UserCheckinAction = withContext(Dispatchers.IO) {
        require(id > 0) { "rule id must be positive" }
        normalizeAdminAction(delete("/api/admin/baseurl-rules?id=$id"))
    }

    suspend fun adminSaveShopItem(item: AdminShopItem): UserCheckinAction = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("name", item.name)
            .put("description", item.description)
            .put("price", item.price)
            .put("type", item.type)
            .put("is_active", if (item.isActive) 1 else 0)
        if (item.type == "frame") {
            body.put("image_url", item.imageUrl)
        } else {
            body.put("image_url", "")
            body.put("badge_html", item.badgeHtml)
            body.put("badge_css", item.badgeCss)
        }
        val raw = if (item.id > 0) {
            body.put("id", item.id)
            put("/api/admin/shop/items", body)
        } else {
            post("/api/admin/shop/items", body)
        }
        normalizeAdminAction(raw)
    }

    suspend fun adminDeleteShopItem(id: Long): UserCheckinAction = withContext(Dispatchers.IO) {
        require(id > 0) { "shop item id must be positive" }
        normalizeAdminAction(delete("/api/admin/shop/items?id=$id"))
    }

    suspend fun updateCurrentUser(profile: UserProfile): UserProfile = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("username", profile.name)
            .put("bio", profile.bio.orEmpty())
        profile.showCheckin?.let { body.put("show_checkin", it) }
        profile.autoCheckin?.let { body.put("auto_checkin", it) }
        patch("/api/users/me", body)
        profile
    }

    suspend fun updateCurrentUserCheckinSettings(
        showCheckin: Boolean,
        autoCheckin: Boolean
    ): UserCheckinAction = withContext(Dispatchers.IO) {
        val source = unwrapObject(
            patch(
            "/api/users/me/checkins/settings",
            JSONObject()
                .put("show_checkin", showCheckin)
                .put("auto_checkin", autoCheckin)
            ),
            "data",
            "result"
        )
        UserCheckinAction(
            success = source.firstBooleanOrNull("success", "ok") ?: true,
            message = source.firstStringOrNull("message", "msg", "detail")
        )
    }

    suspend fun currentUserCheckinStats(): UserCheckinStats = withContext(Dispatchers.IO) {
        userCheckinStats()
    }

    suspend fun userCheckinStats(userId: Long? = null): UserCheckinStats = withContext(Dispatchers.IO) {
        val publicUserId = userId?.takeIf { it > 0 }
        val path = publicUserId?.let { "/api/users/$it/checkins/stats" }
            ?: "/api/users/me/checkins/stats"
        val params = publicUserId?.let { mapOf("user_id" to it.toString()) }.orEmpty()
        val source = unwrapObject(get(path, params), "stats", "data", "result")
        UserCheckinStats(
            totalDays = source.intOrNull("total_days")
                ?: source.intOrNull("totalDays")
                ?: source.intOrNull("days")
                ?: source.intOrNull("total")
                ?: source.intOrNull("checkin_days")
                ?: 0,
            totalPoints = source.longOrNull("total_points")
                ?: source.longOrNull("totalPoints")
                ?: source.longOrNull("points")
                ?: source.longOrNull("point")
                ?: 0L,
            maxStreak = source.intOrNull("max_streak")
                ?: source.intOrNull("maxStreak")
                ?: source.intOrNull("longest_streak")
                ?: source.intOrNull("longestStreak")
                ?: 0,
            currentStreak = source.intOrNull("current_streak")
                ?: source.intOrNull("currentStreak")
                ?: source.intOrNull("streak")
                ?: 0
        )
    }

    suspend fun checkinCurrentUser(): UserCheckinAction = withContext(Dispatchers.IO) {
        val source = unwrapObject(post("/api/users/me/checkins", JSONObject()), "data", "result")
        UserCheckinAction(
            success = source.firstBooleanOrNull("success", "ok") ?: true,
            message = source.firstStringOrNull("message", "msg", "detail"),
            points = source.longOrNull("points") ?: source.longOrNull("point")
        )
    }

    suspend fun favoriteGroups(previewLimit: Int = 6): List<FavoriteGroup> = withContext(Dispatchers.IO) {
        normalizeFavoriteGroups(
            get(
                "/api/favorites/groups",
                mapOf(
                    "preview_limit" to previewLimit.coerceIn(1, 10).toString(),
                    "with_preview" to "true"
                )
            )
        )
    }

    suspend fun tags(sort: String = "count", limit: Int = 24): List<NovelTag> = withContext(Dispatchers.IO) {
        normalizeNovelTags(
            get(
                "/api/tags",
                mapOf(
                    "sort" to sort,
                    "limit" to limit.toString()
                )
            )
        )
    }

    suspend fun messages(page: Int = 1, pageSize: Int = 20): List<SiteMessage> =
        messagePage(MessageQuery(), page, pageSize).items

    suspend fun messagePage(
        query: MessageQuery,
        page: Int = 1,
        pageSize: Int = 20
    ): MessagePage = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "page" to page.coerceAtLeast(1).toString(),
            "page_size" to pageSize.coerceAtLeast(1).toString()
        )
        query.messageType?.let { params["message_type"] = it.toString() }
        query.isRead?.let { params["is_read"] = it.toString() }
        query.priority?.let { params["priority"] = it.toString() }
        query.keyword.trim().takeIf { it.isNotEmpty() }?.let { params["keyword"] = it }
        normalizeMessagePage(
            raw = get("/api/messages", params),
            requestedPage = page,
            requestedPageSize = pageSize
        )
    }

    suspend fun messageDetail(messageId: Long): SiteMessage = withContext(Dispatchers.IO) {
        val source = unwrapObject(get("/api/messages/$messageId"), "message", "data", "result")
        normalizeMessage(source) ?: throw IOException("NovalPie message detail is missing: $messageId")
    }

    suspend fun messageStats(): MessageStats = withContext(Dispatchers.IO) {
        normalizeMessageStats(get("/api/messages/stats"))
    }

    suspend fun workspaceApiConfigs(): List<WorkspaceApiConfig> = withContext(Dispatchers.IO) {
        normalizeWorkspaceApiConfigs(get("/workspace/apis"))
    }

    suspend fun workspaceCookieStatus(): WorkspaceCookieStatus = withContext(Dispatchers.IO) {
        val source = unwrapObject(get("/workspace/cookie-status"), "data", "result")
        WorkspaceCookieStatus(
            hasCookie = source.firstBooleanOrNull("hasCookie", "has_cookie") ?: false
        )
    }

    suspend fun workspaceCookieConfigs(): WorkspaceCookieConfigs = withContext(Dispatchers.IO) {
        normalizeWorkspaceCookieConfigs(get("/workspace/cookie-config"))
    }

    suspend fun workspaceHealth(): WorkspaceHealth = withContext(Dispatchers.IO) {
        val stats = normalizeWorkspaceApiStatus(get("/workspace/stats"))
        val translators = normalizeWorkspaceTranslators(get("/workspace/translator-health"))
        WorkspaceHealth(apiStatus = stats, translators = translators)
    }

    suspend fun createWorkspaceApi(
        name: String,
        model: String,
        endpoint: String,
        apiKey: String,
        concurrency: Int
    ): WorkspaceActionResult = withContext(Dispatchers.IO) {
        normalizeWorkspaceActionResult(
            post(
                "/workspace/apis",
                JSONObject()
                    .put("name", name.trim())
                    .put("model", model.trim())
                    .put("endpoint", endpoint.trim())
                    .put("key", apiKey.trim())
                    .put("concurrency", concurrency.coerceAtLeast(1))
            )
        )
    }

    suspend fun updateWorkspaceApi(
        id: Long,
        name: String,
        model: String,
        endpoint: String,
        apiKey: String,
        concurrency: Int
    ): WorkspaceActionResult = withContext(Dispatchers.IO) {
        normalizeWorkspaceActionResult(
            put(
                "/workspace/apis/$id",
                JSONObject()
                    .put("name", name.trim())
                    .put("model", model.trim())
                    .put("endpoint", endpoint.trim())
                    .put("key", apiKey.trim())
                    .put("concurrency", concurrency.coerceAtLeast(1))
            )
        )
    }

    suspend fun deleteWorkspaceApi(id: Long): WorkspaceActionResult = withContext(Dispatchers.IO) {
        normalizeWorkspaceActionResult(delete("/workspace/apis/$id"))
    }

    /** Matches the current workspace's server-side active/inactive toggle control. */
    suspend fun toggleWorkspaceApi(id: Long): WorkspaceActionResult = withContext(Dispatchers.IO) {
        require(id > 0) { "workspace api id must be positive" }
        normalizeWorkspaceActionResult(post("/workspace/apis/$id/toggle"))
    }

    suspend fun createWorkspaceCookie(
        configKey: String,
        description: String?,
        cookieRaw: String,
        proxyIp: String?,
        isActive: Boolean
    ): WorkspaceActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("config_key", configKey.trim())
            .put("cookie_raw", cookieRaw.trim())
            .put("is_active", isActive)
        description?.trim()?.takeIf { it.isNotEmpty() }?.let { body.put("description", it) }
        proxyIp?.trim()?.takeIf { it.isNotEmpty() }?.let { body.put("proxy_ip", it) }
        normalizeWorkspaceActionResult(post("/workspace/cookie-config", body))
    }

    suspend fun updateWorkspaceCookie(
        id: Long,
        description: String?,
        cookieRaw: String?,
        proxyIp: String?,
        isActive: Boolean
    ): WorkspaceActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("id", id)
            .put("is_active", isActive)
        description?.let { body.put("description", it.trim()) }
        cookieRaw?.let { body.put("cookie_raw", it.trim()) }
        proxyIp?.let { body.put("proxy_ip", it.trim()) }
        normalizeWorkspaceActionResult(put("/workspace/cookie-config", body))
    }

    suspend fun setWorkspaceCookieActive(id: Long, isActive: Boolean): WorkspaceActionResult =
        withContext(Dispatchers.IO) {
            normalizeWorkspaceActionResult(
                put(
                    "/workspace/cookie-config",
                    JSONObject().put("id", id).put("is_active", isActive)
                )
            )
        }

    suspend fun deleteWorkspaceCookie(id: Long): WorkspaceActionResult = withContext(Dispatchers.IO) {
        normalizeWorkspaceActionResult(
            delete("/workspace/cookie-config", JSONObject().put("id", id))
        )
    }

    suspend fun markMessageRead(messageId: Long): MessageActionResult = withContext(Dispatchers.IO) {
        normalizeMessageActionResult(
            post("/api/messages/$messageId/read", JSONObject().put("id", messageId))
        )
    }

    suspend fun markMessagesRead(messageIds: List<Long>): MessageActionResult = withContext(Dispatchers.IO) {
        normalizeMessageActionResult(
            post("/api/messages/read", JSONObject().put("ids", JSONArray(messageIds)))
        )
    }

    suspend fun markAllMessagesRead(): MessageActionResult = withContext(Dispatchers.IO) {
        normalizeMessageActionResult(
            post("/api/messages/read", JSONObject().put("all", true))
        )
    }

    suspend fun starMessage(messageId: Long, starred: Boolean): MessageActionResult = withContext(Dispatchers.IO) {
        normalizeMessageActionResult(
            post("/api/messages/$messageId/star", JSONObject().put("starred", if (starred) 1 else 0))
        )
    }

    suspend fun deleteMessage(
        messageId: Long,
        permanent: Boolean = false
    ): MessageActionResult = withContext(Dispatchers.IO) {
        normalizeMessageActionResult(
            delete(
                "/api/messages/$messageId",
                JSONObject()
                    .put("id", messageId)
                    .put("permanent", permanent)
            )
        )
    }

    suspend fun deleteMessages(messageIds: List<Long>): MessageActionResult = withContext(Dispatchers.IO) {
        normalizeMessageActionResult(
            delete("/api/messages", JSONObject().put("ids", JSONArray(messageIds)))
        )
    }

    suspend fun messageSettings(): MessageSettings = withContext(Dispatchers.IO) {
        normalizeMessageSettings(get("/api/messages/settings"))
    }

    suspend fun updateMessageSettings(settings: MessageSettings): MessageActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("enable_notifications", settings.enableNotifications)
            .put("enable_email", settings.enableEmail)
            .put("enable_browser_push", settings.enableBrowserPush)
        settings.notificationTypes?.let { body.put("notification_types", JSONArray(it.toList())) }
        settings.quietHoursStart?.let { body.put("quiet_hours_start", it) }
        settings.quietHoursEnd?.let { body.put("quiet_hours_end", it) }
        settings.autoReadAfterDays?.let { body.put("auto_read_after_days", it) }
        normalizeMessageActionResult(put("/api/messages/settings", body))
    }

    suspend fun messageConversation(
        targetUserId: Long,
        page: Int = 1,
        pageSize: Int = 100
    ): List<DirectMessage> = withContext(Dispatchers.IO) {
        normalizeDirectMessages(
            get(
                "/api/messages/conversations",
                mapOf(
                    "target_user_id" to targetUserId.toString(),
                    "page" to page.coerceAtLeast(1).toString(),
                    "page_size" to pageSize.coerceAtLeast(1).toString()
                )
            )
        )
    }

    suspend fun sendDirectMessage(
        currentUserId: Long,
        targetUserId: Long,
        currentUserName: String,
        content: String
    ): MessageActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("user_id", targetUserId)
            .put("execute_user_id", currentUserId)
            .put("message_type", 8)
            .put("message_title", "来自 ${currentUserName.trim()} 的私信")
            .put("message_content", content.trim())
        normalizeMessageActionResult(post("/api/messages", body))
    }

    suspend fun favorites(page: Int = 1, limit: Int = 20, groupId: Long? = null): List<NovelCard> = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "page" to page.toString(),
            "limit" to limit.toString(),
            "sort_field" to "updated_at",
            "sort_order" to "desc",
            "type" to "novel"
        )
        groupId?.let { params["group_id"] = it.toString() }
        normalizeNovelList(
            get(
                "/api/favorites",
                params
            )
        )
    }

    /**
     * Source-compatible favourite query. The website uses a dedicated group-items endpoint rather
     * than a `group_id` parameter when a named group is open, and each result retains the favourite
     * record metadata needed by management actions.
     */
    suspend fun favoritePage(
        page: Int = 1,
        limit: Int = 20,
        groupId: Long? = null,
        search: String = "",
        sortField: String = "created_at",
        sortOrder: String = "desc",
        excludeAdult: Boolean = false
    ): FavoritePage = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "type" to "novel",
            "page" to page.coerceAtLeast(1).toString(),
            "limit" to limit.coerceIn(1, 100).toString(),
            "sort_field" to sortField.trim().ifBlank { "created_at" },
            "sort_order" to (sortOrder.trim().lowercase().takeIf { it in setOf("asc", "desc") } ?: "desc")
        )
        search.trim().takeIf(String::isNotBlank)?.let { params["search"] = it }
        if (excludeAdult) params["exclude_adult"] = "1"
        val raw = if (groupId == null) {
            get("/api/favorites", params)
        } else {
            get("/api/favorites/groups/$groupId/items", params)
        }
        normalizeFavoritePage(raw, requestedPage = page, requestedPageSize = limit)
    }

    suspend fun readingHistoryPage(
        page: Int = 1,
        limit: Int = 20,
        type: String = "novel"
    ): FavoritePage = withContext(Dispatchers.IO) {
        normalizeFavoritePage(
            get(
                "/api/favorites/history",
                mapOf(
                    "type" to type.trim().ifBlank { "novel" },
                    "page" to page.coerceAtLeast(1).toString(),
                    "limit" to limit.coerceIn(1, 100).toString()
                )
            ),
            requestedPage = page,
            requestedPageSize = limit
        )
    }

    suspend fun createFavoriteGroup(name: String): FavoriteGroup = withContext(Dispatchers.IO) {
        val normalized = name.trim()
        require(normalized.isNotBlank()) { "group name is required" }
        normalizeFavoriteGroup(post("/api/favorites/groups", JSONObject().put("name", normalized)))
    }

    suspend fun renameFavoriteGroup(groupId: Long, name: String): FavoriteGroup = withContext(Dispatchers.IO) {
        val normalized = name.trim()
        require(groupId > 0) { "group id is required" }
        require(normalized.isNotBlank()) { "group name is required" }
        normalizeFavoriteGroup(put("/api/favorites/groups/$groupId", JSONObject().put("name", normalized)))
    }

    suspend fun deleteFavoriteGroup(groupId: Long) = withContext(Dispatchers.IO) {
        require(groupId > 0) { "group id is required" }
        delete("/api/favorites/groups/$groupId")
    }

    suspend fun moveFavoriteToGroup(favoriteId: Long, groupId: Long?) = withContext(Dispatchers.IO) {
        require(favoriteId > 0) { "favorite id is required" }
        post(
            "/api/favorites/management",
            JSONObject()
                .put("action", "move_group")
                .put("favorite_id", favoriteId)
                .put("group_id", groupId ?: JSONObject.NULL)
        )
    }

    suspend fun removeFavorite(favoriteId: Long) = withContext(Dispatchers.IO) {
        require(favoriteId > 0) { "favorite id is required" }
        post(
            "/api/favorites/management",
            JSONObject().put("action", "remove").put("favorite_id", favoriteId)
        )
    }

    suspend fun setFavoritePinned(favoriteId: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        require(favoriteId > 0) { "favorite id is required" }
        post(
            "/api/favorites/management",
            JSONObject()
                .put("action", "set_pin")
                .put("favorite_id", favoriteId)
                .put("is_pinned", isPinned)
        )
    }

    suspend fun deleteReadingHistory(favoriteIds: List<Long> = emptyList(), clearAll: Boolean = false) =
        withContext(Dispatchers.IO) {
            require(clearAll || favoriteIds.isNotEmpty()) { "history selection is required" }
            val body = JSONObject()
            if (clearAll) {
                body.put("clear_all", true)
            } else {
                body.put("novel_ids", JSONArray(favoriteIds.filter { it > 0 }.distinct()))
            }
            delete("/api/favorites/history", body)
        }

    suspend fun forumPosts(
        page: Int = 1,
        limit: Int = 20,
        type: String = "all",
        search: String = "",
        hideSpoilers: Boolean = true
    ): List<ForumPost> = forumPostsPage(
        page = page,
        limit = limit,
        type = type,
        search = search,
        hideSpoilers = hideSpoilers
    ).posts

    /**
     * Mirrors the mobile forum's review request. When spoilers are visible the source omits
     * `hide_spoilers` entirely instead of sending a false-like value.
     */
    suspend fun forumPostsPage(
        page: Int = 1,
        limit: Int = 20,
        type: String = "all",
        search: String = "",
        hideSpoilers: Boolean = true
    ): ForumPostPage = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        if (search.isNotBlank()) params["search"] = search.trim()
        if (type.trim().equals("review", ignoreCase = true)) {
            // The forum's 书评 rail is a book-comment feed, not a /api/posts type.
            // The latter returns an empty list on the live source for type=review.
            if (hideSpoilers) params["hide_spoilers"] = "1"
            normalizeForumPostsPage(
                get("/api/comments/book-reviews", params),
                forceBookReview = true,
                requestedPage = page,
                requestedLimit = limit
            )
        } else {
            if (type.isNotBlank() && type != "all") params["type"] = type
            normalizeForumPostsPage(
                get("/api/posts", params),
                requestedPage = page,
                requestedLimit = limit
            )
        }
    }

    suspend fun forumPostDetail(postId: Long): ForumPostDetail = withContext(Dispatchers.IO) {
        normalizeForumPostDetail(get("/api/posts/$postId"))
    }

    suspend fun createForumPost(request: ForumCreateRequest): ForumCreateResult = withContext(Dispatchers.IO) {
        val type = request.type.trim()
        val title = request.title.trim()
        val content = request.content.trim()
        val tags = request.tags.map(String::trim).filter(String::isNotBlank).distinct()
        require(type in setOf("recommend", "discussion", "feedback", "announcement")) { "unsupported forum type" }
        require(title.isNotBlank() && title.length <= 100) { "forum title must contain 1 to 100 characters" }
        require(content.isNotBlank() && content.length <= 10_000) { "forum content must contain 1 to 10000 characters" }
        require(tags.size <= 5 && tags.all { it.length <= 20 }) { "forum tags exceed website limits" }

        val body = JSONObject()
            .put("type", type)
            .put("title", title)
            .put("content", content)
            .put("tags", JSONArray(tags))
        request.poll?.let { poll ->
            val options = poll.options.map(String::trim).filter(String::isNotBlank)
            require(options.size in 2..10 && options.distinct().size == options.size) {
                "forum poll must contain 2 to 10 unique options"
            }
            val maxChoices = if (poll.allowMultiple) {
                poll.maxChoices.coerceIn(2, options.size)
            } else {
                1
            }
            val pollBody = JSONObject()
                .put("options", JSONArray(options))
                .put("allowMultiple", poll.allowMultiple)
                .put("maxChoices", maxChoices)
                .put("endsAt", poll.endsAt?.trim()?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
            poll.question?.trim()?.takeIf(String::isNotBlank)?.let { pollBody.put("question", it) }
            body.put("poll", pollBody)
        }

        normalizeForumCreateResult(post("/api/posts", body))
    }

    /**
     * The mobile forum detail loads its complete first reply window with limit=100.
     * Keep that source contract here instead of inheriting a generic list-page size.
     */
    suspend fun forumPostComments(postId: Long, page: Int = 1, limit: Int = 100): List<ForumComment> = withContext(Dispatchers.IO) {
        normalizeForumComments(
            get(
                "/api/posts/$postId/comments",
                mapOf(
                    "page" to page.toString(),
                    "limit" to limit.toString()
                )
            )
        )
    }

    suspend fun createForumComment(
        postId: Long,
        content: String,
        parentCommentId: Long? = null,
        replyToName: String? = null
    ): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("content", content)
        parentCommentId?.let { body.put("comment_id", it) }
        replyToName?.takeIf { it.isNotBlank() }?.let { body.put("reply_to_name", it) }
        normalizeForumActionResult(post("/api/posts/$postId/comments", body))
    }

    suspend fun toggleForumPostLike(postId: Long): ForumActionResult = withContext(Dispatchers.IO) {
        normalizeForumActionResult(post("/api/posts/$postId/likes", JSONObject()))
    }

    suspend fun reactToForumPost(postId: Long, reactionType: String, awardPoints: Int? = null): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("reaction_type", reactionType)
        awardPoints?.let { body.put("award_points", it) }
        normalizeForumActionResult(post("/api/posts/$postId/reactions", body))
    }

    suspend fun toggleForumCommentLike(commentId: Long): ForumActionResult = withContext(Dispatchers.IO) {
        normalizeForumActionResult(post("/api/comments/$commentId/likes", JSONObject()))
    }

    suspend fun reactToForumComment(commentId: Long, reactionType: String, awardPoints: Int? = null): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("reaction_type", reactionType)
        awardPoints?.let { body.put("award_points", it) }
        normalizeForumActionResult(post("/api/comments/$commentId/reactions", body))
    }

    suspend fun createBookComment(bookId: Long, content: String): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("type", "book")
            .put("book_id", bookId)
            .put("content", content)
        normalizeForumActionResult(post("/api/comments", body))
    }

    suspend fun createChapterComment(bookId: Long, chapterId: Long, content: String): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("type", "chapter")
            .put("book_id", bookId)
            .put("chapter_id", chapterId)
            .put("content", content)
        normalizeForumActionResult(post("/api/comments", body))
    }

    suspend fun createCommentReply(commentId: Long, content: String, replyToName: String? = null): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("content", content)
        replyToName?.takeIf { it.isNotBlank() }?.let { body.put("reply_to_name", it) }
        normalizeForumActionResult(post("/api/comments/$commentId/replies", body))
    }

    suspend fun toggleCommentLike(commentId: Long): ForumActionResult = withContext(Dispatchers.IO) {
        normalizeForumActionResult(post("/api/comments/$commentId/likes", JSONObject()))
    }

    suspend fun reactToComment(commentId: Long, reactionType: String, awardPoints: Int? = null): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("reaction_type", reactionType)
        awardPoints?.let { body.put("award_points", it) }
        normalizeForumActionResult(post("/api/comments/$commentId/reactions", body))
    }

    suspend fun reactToCommentReply(parentCommentId: Long, replyId: Long, reactionType: String, awardPoints: Int? = null): ForumActionResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("reaction_type", reactionType)
        awardPoints?.let { body.put("award_points", it) }
        normalizeForumActionResult(post("/api/comments/$parentCommentId/replies/$replyId/reactions", body))
    }

    suspend fun favoriteStatus(bookId: Long): FavoriteStatus = withContext(Dispatchers.IO) {
        normalizeFavoriteStatus(
            get(
                "/api/favorites/status",
                mapOf(
                    "object_id" to bookId.toString(),
                    "type" to "novel"
                )
            )
        )
    }

    /** Mirrors the website reader's favourite action and returns the server-confirmed state. */
    suspend fun toggleFavorite(
        bookId: Long,
        isFavorited: Boolean,
        groupId: Long = 0L,
    ): FavoriteStatus = withContext(Dispatchers.IO) {
        require(bookId > 0) { "book id is required" }
        post(
            "/api/favorites",
            JSONObject()
                .put("object_id", bookId)
                .put("favorite_type", "novel")
                .put("action", if (isFavorited) "remove" else "add")
                .put("group_id", groupId.coerceAtLeast(0L))
        )
        // The action response has changed shape between source deployments. A follow-up status
        // read keeps the native button honest instead of guessing from a success message.
        favoriteStatus(bookId)
    }

    /**
     * Reads the source terminology table without guessing at a client-side maximum. The website
     * uses zero-based pages here, unlike its favourites and search routes.
     */
    suspend fun terminologyPage(
        novelId: Long,
        keyword: String = "",
        page: Int = 0,
    ): TerminologyPage = withContext(Dispatchers.IO) {
        require(novelId > 0L) { "novel id is required" }
        val requestedPage = page.coerceAtLeast(0)
        normalizeTerminologyPage(
            raw = get(
                "/api/terminologies",
                mapOf(
                    "novel_id" to novelId.toString(),
                    "keyword" to keyword.trim(),
                    "page" to requestedPage.toString(),
                ),
            ),
            requestedNovelId = novelId,
            requestedPage = requestedPage,
        )
    }

    suspend fun bookDetail(bookId: Long): NovelCard = withContext(Dispatchers.IO) {
        normalizeBook(get("/api/novels/$bookId/detail"))
    }

    suspend fun bookCoverPhoto(bookId: Long, favoriteType: String = "novel"): String? =
        bookCoverPhotoInfo(bookId, favoriteType).originalUrl

    /**
     * The source deliberately publishes two cover URLs for some layered/animated images:
     * [BookCoverPhoto.previewUrl] is the lightweight card image and [BookCoverPhoto.originalUrl]
     * is the image used by its full-screen viewer. Keep both values so native cards never need to
     * guess the inner image from a filename.
     */
    internal suspend fun bookCoverPhotoInfo(bookId: Long, favoriteType: String = "novel"): BookCoverPhoto =
        withContext(Dispatchers.IO) {
            val raw = get(
                "/api/novels/$bookId/photo",
                mapOf("favorite_type" to favoriteType)
            )
            val source = unwrapObject(raw, "photo", "novel", "data", "result")
            val previewUrl = normalizeAssetUrl(
                source.firstStringOrNull(
                    "photo_url",
                    "photoUrl",
                    "cover_url",
                    "coverUrl"
                )
            )
            val originalUrl = normalizeAssetUrl(
                source.firstStringOrNull(
                    "photo_true_url",
                    "photoTrueUrl",
                    "full_cover_url",
                    "fullCoverUrl",
                    "original_cover_url",
                    "originalCoverUrl"
                )
            ) ?: previewUrl
            BookCoverPhoto(previewUrl = previewUrl, originalUrl = originalUrl)
        }

    suspend fun generateEditorRegex(
        endpoint: String,
        apiKey: String,
        model: String,
        chapterTitles: List<String>
    ): String = withContext(Dispatchers.IO) {
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) { "API endpoint must use HTTP or HTTPS" }
        require(apiKey.isNotBlank()) { "API key is required" }
        require(model.isNotBlank()) { "Model is required" }
        val titles = chapterTitles.filter(String::isNotBlank).take(20)
        require(titles.size >= 2) { "At least two chapter titles are required" }
        val systemPrompt = """
            You are a regular-expression expert. Generate JavaScript-compatible regex patterns that match all supplied chapter titles.
            Return one JSON object with a regex string field. Multiple patterns may be separated by newlines.
        """.trimIndent()
        val userPrompt = buildString {
            appendLine("Generate regex patterns for these chapter titles:")
            titles.forEachIndexed { index, title -> appendLine("${index + 1}. $title") }
        }.trimEnd()
        val payload = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userPrompt))
            )
            .put("temperature", 0.3)
            .put("response_format", JSONObject().put("type", "json_object"))
        val url = endpoint.trimEnd('/') + "/v1/chat/completions"
        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = baseRequestBuilder(url)
            .header("content-type", "application/json")
            .header("authorization", "Bearer $apiKey")
            .post(requestBody)
        val raw = executeExternal("editor AI regex", requestBuilder) as? JSONObject
            ?: throw IOException("AI returned an invalid response")
        val content = raw.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.stringOrNull("content")
            ?: throw IOException("AI response did not contain message content")
        val parsed = runCatching { JSONObject(content) }.getOrNull()
        val regex = parsed?.firstStringOrNull("regex", "pattern", "expression")
            ?: Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
                .find(content)?.groupValues?.getOrNull(1)
                ?.let { runCatching { JSONObject(it.trim()) }.getOrNull() }
                ?.firstStringOrNull("regex", "pattern", "expression")
            ?: content.trim()
        regex.takeIf(String::isNotBlank) ?: throw IOException("AI did not return a regex pattern")
    }

    /**
     * Implements the source upload editor's local processor contract:
     * POST JSON {"text":"..."} and accept either {"text"}, {"data"}, or a JSON string.
     * The request deliberately omits NovalPie session headers so a user-selected processor never
     * receives the website authentication token.
     */
    suspend fun processEditorTextWithApi(
        endpoint: String,
        text: String,
        timeoutSeconds: Int = 30
    ): String = withContext(Dispatchers.IO) {
        require(text.isNotBlank()) { "Text is required" }
        require(timeoutSeconds in 1..120) { "Timeout must be between 1 and 120 seconds" }
        val url = normalizeEditorProcessorUrl(endpoint)
        val requestBody = JSONObject()
            .put("text", text)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val result = executeExternal(
            label = "editor processor",
            requestBuilder = baseRequestBuilder(url)
                .header("content-type", "application/json")
                .post(requestBody),
            callTimeoutSeconds = timeoutSeconds.toLong()
        )
        when (result) {
            is JSONObject -> result.firstStringOrNull("text", "data")
                ?: throw IOException("Processor response must contain text or data")
            is String -> decodeEditorProcessorString(result)
            else -> throw IOException("Processor returned an invalid response")
        }
    }

    suspend fun startPoliticalExam(): PoliticalExamSession = withContext(Dispatchers.IO) {
        normalizePoliticalExamSession(post("/api/political-exams/sessions", JSONObject()))
    }

    suspend fun submitPoliticalExam(
        sessionId: String,
        answers: PoliticalExamAnswers
    ): PoliticalExamResult = withContext(Dispatchers.IO) {
        val answerBody = JSONObject()
            .put("single_choice", JSONArray().apply {
                answers.singleChoice.forEach { value -> put(value ?: JSONObject.NULL) }
            })
            .put("multiple_choice", JSONArray().apply {
                answers.multipleChoice.forEach { values -> put(JSONArray(values)) }
            })
            .put("true_false", JSONArray().apply {
                answers.trueFalse.forEach { value -> put(value ?: JSONObject.NULL) }
            })
            .put("fill_blank", JSONArray(answers.fillBlank))
        normalizePoliticalExamResult(
            post(
                "/api/political-exams/sessions/submit",
                JSONObject()
                    .put("session_id", sessionId)
                    .put("answers", answerBody)
            )
        )
    }

    suspend fun chapters(bookId: Long): List<Chapter> = withContext(Dispatchers.IO) {
        // Current source detail pages request the v2 directory endpoint.  It includes per-chapter
        // image_count and is the contract used by the web catalogue.
        normalizeChapters(get("/api/v2/novels/$bookId/chapters"))
    }

    suspend fun chapterContent(
        chapterId: Long,
        replaceMode: String = "india",
        showImages: Boolean = true,
    ): ReaderContent = withContext(Dispatchers.IO) {
        var latestFailure: Throwable? = null
        for (attempt in 0 until READER_CONTENT_MAX_ATTEMPTS) {
            try {
                // The website keeps this short-lived reader session around for adjacent chapters.
                // Reusing it removes one proxy round trip from every infinite-scroll append.
                val session = readerSessionKey(forceRefresh = attempt > 0)
                return@withContext normalizeReaderContent(
                    raw = get(
                        "/api/chapters/$chapterId/content",
                        mapOf(
                            "session" to session.sessionId,
                            "replace_mode" to replaceMode,
                            "show_images" to if (showImages) "1" else "0"
                        ),
                        callTimeoutSeconds = READER_CONTENT_CALL_TIMEOUT_SECONDS,
                        readTimeoutSeconds = READER_CONTENT_READ_TIMEOUT_SECONDS,
                    ),
                    readerSessionKey = session.sessionKey
                )
            } catch (failure: Throwable) {
                if (failure is CancellationException) throw failure
                latestFailure = failure
                if (attempt == READER_CONTENT_MAX_ATTEMPTS - 1 || !isRetryableReaderContentFailure(failure)) {
                    throw failure
                }
                // A timeout can leave either the proxy tunnel or the source reader session stale.
                // A single fresh-session retry recovers transient failures without repeating writes.
                invalidateReaderSession()
                delay(READER_CONTENT_RETRY_DELAY_MS)
            }
        }
        throw latestFailure ?: IOException("Chapter content request did not complete.")
    }

    suspend fun managedChapterIllustrations(chapterId: Long): ChapterIllustrationPage = withContext(Dispatchers.IO) {
        require(chapterId > 0) { "chapter id is required" }
        normalizeChapterIllustrations(get("/api/users/me/chapters/$chapterId/illustrations"))
    }

    suspend fun uploadManagedChapterIllustrations(
        chapterId: Long,
        files: List<UploadFileSource>
    ): ChapterIllustrationMutationResult = withContext(Dispatchers.IO) {
        require(chapterId > 0) { "chapter id is required" }
        require(files.isNotEmpty()) { "illustration files are required" }
        files.forEach { file ->
            require(file.sizeBytes in 1..WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES) {
                "illustration file must be between 1 byte and 20 MiB"
            }
            require(file.contentType?.startsWith("image/") == true) { "illustration file must be an image" }
        }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chapter_id", chapterId.toString())
        files.forEach { file ->
            multipart.addFormDataPart(
                "illustrations[]",
                file.fileName,
                UploadStreamRequestBody(file)
            )
        }
        normalizeChapterIllustrationMutation(
            requestBody("/api/users/me/chapters/$chapterId/illustrations", "POST", multipart.build())
        )
    }

    suspend fun deleteManagedChapterIllustration(
        chapterId: Long,
        imageId: Long
    ): ChapterIllustrationMutationResult = withContext(Dispatchers.IO) {
        require(chapterId > 0 && imageId > 0) { "chapter and image ids are required" }
        normalizeChapterIllustrationMutation(
            delete("/api/users/me/chapters/$chapterId/illustrations/$imageId")
        )
    }

    suspend fun reorderManagedChapters(bookId: Long, orderedChapterIds: List<Long>): ForumActionResult = withContext(Dispatchers.IO) {
        require(bookId > 0 && orderedChapterIds.isNotEmpty()) { "book and chapter ids are required" }
        normalizeForumActionResult(
            post(
                "/api/users/me/chapters/reorder",
                JSONObject()
                    .put("novel_id", bookId)
                    .put("ordered_chapter_ids", JSONArray(orderedChapterIds))
            )
        )
    }

    suspend fun insertManagedChapter(bookId: Long, insertAt: Int, title: String, content: String): ForumActionResult = withContext(Dispatchers.IO) {
        require(bookId > 0 && insertAt >= 1) { "book id and insertion position are required" }
        require(title.trim().isNotBlank() && content.trim().isNotBlank()) { "chapter title and content are required" }
        normalizeForumActionResult(
            post(
                "/api/users/me/chapters/insert",
                JSONObject()
                    .put("novel_id", bookId)
                    .put("insert_at", insertAt)
                    .put("title", title.trim())
                    .put("content", content.trim())
            )
        )
    }

    suspend fun updateManagedChapter(chapterId: Long, title: String, content: String): ForumActionResult = withContext(Dispatchers.IO) {
        require(chapterId > 0) { "chapter id is required" }
        require(title.trim().isNotBlank() && content.trim().isNotBlank()) { "chapter title and content are required" }
        normalizeForumActionResult(
            patch(
                "/api/users/me/chapters/$chapterId",
                JSONObject().put("title", title.trim()).put("content", content.trim())
            )
        )
    }

    suspend fun deleteManagedChapter(chapterId: Long): ForumActionResult = withContext(Dispatchers.IO) {
        require(chapterId > 0) { "chapter id is required" }
        normalizeForumActionResult(delete("/api/users/me/chapters/$chapterId"))
    }

    suspend fun batchDeleteManagedChapters(bookId: Long, chapterIds: List<Long>): ForumActionResult = withContext(Dispatchers.IO) {
        require(bookId > 0 && chapterIds.isNotEmpty()) { "book and chapter ids are required" }
        normalizeForumActionResult(
            post(
                "/api/users/me/chapters/batch-delete",
                JSONObject().put("novel_id", bookId).put("chapter_ids", JSONArray(chapterIds))
            )
        )
    }

    suspend fun requestManagedChapterTranslation(
        bookId: Long,
        chapterIds: List<Long>,
        mode: String
    ): ForumActionResult = withContext(Dispatchers.IO) {
        require(bookId > 0 && chapterIds.isNotEmpty()) { "book and chapter ids are required" }
        require(mode in setOf("personal", "shared")) { "translation mode is invalid" }
        normalizeForumActionResult(
            post(
                "/api/users/me/novels/$bookId/translation-requests",
                JSONObject().put("chapter_ids", JSONArray(chapterIds)).put("mode", mode)
            )
        )
    }

    suspend fun appendManagedChapters(
        bookId: Long,
        submitType: String,
        chapters: List<UploadChapter>,
        epubFilePath: String? = null,
        epubFile: UploadFileSource? = null
    ): UploadActionResult = withContext(Dispatchers.IO) {
        require(bookId > 0 && chapters.isNotEmpty()) { "book and chapters are required" }
        require(submitType in setOf("chinese", "personal", "shared")) { "submit type is invalid" }
        val shouldChunk = chapters.size > 50 || chapters.sumOf { it.title.length.toLong() + it.content.length.toLong() } > 2_500_000L
        val chunks = if (shouldChunk) chapters.chunked(50) else listOf(chapters)
        var lastResult = UploadActionResult(success = true, novelId = bookId)
        chunks.forEachIndexed { index, chunk ->
            val chaptersJson = uploadChaptersJson(chunk).toString()
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("existing_novel_id", bookId.toString())
                .addFormDataPart("submit_type", submitType)
                .addFormDataPart("chapters", chaptersJson)
                .addFormDataPart("chapters_md5", md5Hex(chaptersJson))
            epubFilePath?.takeIf(String::isNotBlank)?.let { multipart.addFormDataPart("epub_file_path", it) }
            epubFile?.let { multipart.addFormDataPart("epub_file", it.fileName, UploadStreamRequestBody(it)) }
            if (chunks.size > 1) {
                multipart
                    .addFormDataPart("chunk_index", index.toString())
                    .addFormDataPart("total_chunks", chunks.size.toString())
                    .addFormDataPart("is_chunked", "1")
            }
            val result = normalizeUploadResult(
                requestBody("/api/users/me/chapters/append", "POST", multipart.build())
            )
            if (!result.success) throw IOException(result.message ?: "append chapters failed")
            lastResult = result.copy(novelId = result.novelId ?: bookId)
        }
        lastResult
    }

    suspend fun bookComments(bookId: Long, page: Int = 1, limit: Int = 30): List<ChapterComment> = withContext(Dispatchers.IO) {
        normalizeChapterComments(
            get(
                "/api/comments",
                mapOf(
                    "type" to "book",
                    "book_id" to bookId.toString(),
                    "page" to page.toString(),
                    "limit" to limit.toString()
                )
            )
        )
    }

    suspend fun chapterComments(bookId: Long? = null, chapterId: Long, page: Int = 1, limit: Int = 20): List<ChapterComment> = withContext(Dispatchers.IO) {
        val query = mutableMapOf(
            "type" to "chapter",
            "chapter_id" to chapterId.toString(),
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        bookId?.takeIf { it > 0 }?.let { query["book_id"] = it.toString() }
        normalizeChapterComments(
            get(
                "/api/comments",
                query
            )
        )
    }

    suspend fun uploadBook(
        upload: UploadBookRequest,
        epubFile: UploadFileSource? = null,
        coverFile: UploadFileSource? = null
    ): UploadActionResult = withContext(Dispatchers.IO) {
        val chaptersJson = uploadChaptersJson(upload.chapters).toString()
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", upload.title.trim())
            .addFormDataPart("title_translation", upload.titleTranslation.trim())
            .addFormDataPart("author_name", upload.authorName.trim())
            .addFormDataPart("description", upload.description.trim())
            .addFormDataPart("language", upload.language.trim())
            .addFormDataPart("spans", upload.spans.trim())
            .addFormDataPart("is_adult", if (upload.isAdult) "1" else "0")
            .addFormDataPart("source", upload.source.trim())
            .addFormDataPart("source_url", upload.sourceUrl.trim())
            .addFormDataPart("tags", upload.tags.joinToString(","))
            .addFormDataPart("submit_type", upload.submitType)
            .addFormDataPart("chapters", chaptersJson)
            .addFormDataPart("chapters_md5", md5Hex(chaptersJson))
        upload.epubFilePath?.takeIf { it.isNotBlank() }?.let { multipart.addFormDataPart("epub_file_path", it) }
        upload.coverUrl?.takeIf { it.isNotBlank() }?.let { multipart.addFormDataPart("cover_url", it) }
        epubFile?.let { multipart.addFormDataPart("epub_file", it.fileName, UploadStreamRequestBody(it)) }
        coverFile?.let { multipart.addFormDataPart("cover", it.fileName, UploadStreamRequestBody(it)) }
        normalizeUploadResult(requestBody("/api/uploads/books", "POST", multipart.build()))
    }

    suspend fun uploadFileInChunks(
        file: UploadFileSource,
        fileId: String = UUID.randomUUID().toString(),
        chunkSizeBytes: Int = WEBSITE_UPLOAD_CHUNK_BYTES
    ): String = withContext(Dispatchers.IO) {
        require(file.sizeBytes > 0L) { "文件为空" }
        require(chunkSizeBytes > 0) { "分片大小必须大于 0" }
        val totalChunks = ((file.sizeBytes + chunkSizeBytes - 1L) / chunkSizeBytes).toInt()
        repeat(totalChunks) { index ->
            val offset = index.toLong() * chunkSizeBytes
            val length = min(chunkSizeBytes.toLong(), file.sizeBytes - offset)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.fileName, UploadRangeRequestBody(file, offset, length))
                .addFormDataPart("file_id", fileId)
                .addFormDataPart("chunk_index", index.toString())
                .addFormDataPart("total_chunks", totalChunks.toString())
                .addFormDataPart("file_name", file.fileName)
                .addFormDataPart("file_size", file.sizeBytes.toString())
                .build()
            val response = unwrapObject(requestBody("/api/uploads/chunks", "POST", multipart), "data", "result")
            if (response.firstBooleanOrNull("success") == false) {
                throw IOException(response.firstStringOrNull("message") ?: "上传分片失败")
            }
        }
        val merge = unwrapObject(
            post(
                "/api/uploads/chunks",
                JSONObject()
                    .put("action", "merge")
                    .put("file_id", fileId)
                    .put("file_name", file.fileName)
                    .put("total_chunks", totalChunks)
            ),
            "data",
            "result"
        )
        if (merge.firstBooleanOrNull("success") == false) {
            throw IOException(merge.firstStringOrNull("message") ?: "合并文件失败")
        }
        merge.firstStringOrNull("file_path", "filePath") ?: throw IOException("服务器未返回文件路径")
    }

    suspend fun parseUploadedEpub(filePath: String): ParsedEpub = withContext(Dispatchers.IO) {
        normalizeParsedEpub(
            post(
                "/api/uploads/epubs",
                JSONObject().put("file_path", filePath).put("parse_only", true)
            )
        )
    }

    private fun get(
        path: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        callTimeoutSeconds: Long? = null,
        readTimeoutSeconds: Long? = null,
    ): Any {
        return request(
            path = path,
            params = params,
            method = "GET",
            headers = headers,
            callTimeoutSeconds = callTimeoutSeconds,
            readTimeoutSeconds = readTimeoutSeconds,
        )
    }

    private fun post(path: String): Any {
        return requestBody(path, "POST", ByteArray(0).toRequestBody())
    }

    private fun post(path: String, body: JSONObject): Any {
        return request(path = path, method = "POST", body = body)
    }

    private fun postUnauthenticated(path: String, body: JSONObject): Any {
        return request(path = path, method = "POST", body = body, includeSession = false)
    }

    private fun put(path: String, body: JSONObject): Any {
        return request(path = path, method = "PUT", body = body)
    }

    private fun putUnauthenticated(path: String, body: JSONObject): Any {
        return request(path = path, method = "PUT", body = body, includeSession = false)
    }

    private fun patch(path: String, body: JSONObject): Any {
        return request(path = path, method = "PATCH", body = body)
    }

    private fun delete(path: String, body: JSONObject? = null): Any {
        return request(path = path, method = "DELETE", body = body)
    }

    private fun requestBody(path: String, method: String, body: RequestBody): Any {
        val requestBuilder = baseRequestBuilder(path)
        when (method) {
            "PUT" -> requestBuilder.put(body)
            else -> requestBuilder.post(body)
        }
        return execute(path, requestBuilder)
    }

    private fun request(
        path: String,
        params: Map<String, String> = emptyMap(),
        method: String,
        body: JSONObject? = null,
        headers: Map<String, String> = emptyMap(),
        includeSession: Boolean = true,
        callTimeoutSeconds: Long? = null,
        readTimeoutSeconds: Long? = null,
    ): Any {
        val builder = (baseUrl.trimEnd('/') + path).toHttpUrl().newBuilder()
        params.forEach { (key, value) ->
            if (value.isNotBlank()) builder.addQueryParameter(key, value)
        }

        val requestBuilder = baseRequestBuilder(builder.build().toString())

        headers.forEach { (key, value) ->
            if (value.isNotBlank()) requestBuilder.header(key, value)
        }

        when (method) {
            "POST", "PUT", "PATCH" -> {
                val payload = (body ?: JSONObject()).toString()
                val requestBody = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
                requestBuilder.header("content-type", "application/json")
                when (method) {
                    "POST" -> requestBuilder.post(requestBody)
                    "PATCH" -> requestBuilder.patch(requestBody)
                    else -> requestBuilder.put(requestBody)
                }
            }
            "DELETE" -> {
                if (body == null) {
                    requestBuilder.delete()
                } else {
                    val payload = body.toString()
                    requestBuilder
                        .header("content-type", "application/json")
                        .delete(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                }
            }
            else -> requestBuilder.get()
        }

        return execute(
            path = path,
            requestBuilder = requestBuilder,
            includeSession = includeSession,
            callTimeoutSeconds = callTimeoutSeconds,
            readTimeoutSeconds = readTimeoutSeconds,
        )
    }

    private fun baseRequestBuilder(pathOrUrl: String): Request.Builder {
        val url = if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            pathOrUrl
        } else {
            baseUrl.trimEnd('/') + pathOrUrl
        }
        return Request.Builder()
            .url(url)
            .header("accept", "application/json")
            .header("user-agent", USER_AGENT)
    }

    private fun execute(
        path: String,
        requestBuilder: Request.Builder,
        includeSession: Boolean = true,
        callTimeoutSeconds: Long? = null,
        readTimeoutSeconds: Long? = null,
    ): Any {
        if (includeSession) {
            cookieProvider()?.takeIf { it.isNotBlank() }?.let { cookie ->
                requestBuilder.header("cookie", cookie)
            }
            authTokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                requestBuilder.header("authorization", "Bearer $token")
            }
        }

        val request = requestBuilder.build()

        val explicitProxy = proxyProvider()
        val proxySelector = if (explicitProxy == null) proxySelectorProvider() else null
        val callClient = if (callTimeoutSeconds != null || readTimeoutSeconds != null ||
            explicitProxy != null || proxySelector != null
        ) {
            client.newBuilder().apply {
                when {
                    explicitProxy != null -> proxy(explicitProxy)
                    proxySelector != null -> this.proxySelector(proxySelector)
                }
                callTimeoutSeconds?.let { callTimeout(it, TimeUnit.SECONDS) }
                readTimeoutSeconds?.let { readTimeout(it, TimeUnit.SECONDS) }
            }.build()
        } else {
            client
        }

        callClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // The body was already read; carry the server's own explanation instead of
                // discarding it and leaving the user with only a status code.
                throw NovalPieApiException(
                    statusCode = response.code,
                    path = path,
                    serverMessage = NovalPieApiException.extractServerMessage(responseBody),
                )
            }
            return parseJsonOrString(responseBody)
        }
    }

    private fun executeExternal(
        label: String,
        requestBuilder: Request.Builder,
        callTimeoutSeconds: Long? = null
    ): Any {
        val request = requestBuilder.build()
        val explicitProxy = proxyProvider()
        val proxySelector = if (explicitProxy == null) proxySelectorProvider() else null
        val callBuilder = client.newBuilder().apply {
            when {
                explicitProxy != null -> proxy(explicitProxy)
                proxySelector != null -> proxySelector(proxySelector)
            }
            callTimeoutSeconds?.let { callTimeout(it, TimeUnit.SECONDS) }
        }
        val callClient = callBuilder.build()
        callClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // Same reasoning as execute(): the AI-regex helper used to discard the reason the
                // external service rejected the call.
                throw NovalPieApiException(
                    statusCode = response.code,
                    path = label,
                    serverMessage = NovalPieApiException.extractServerMessage(responseBody),
                )
            }
            return parseJsonOrString(responseBody)
        }
    }

    private suspend fun streamResponse(
        label: String,
        requestBuilder: Request.Builder,
        consumer: suspend (InputStream, String?) -> Unit,
        callTimeoutSeconds: Long,
        readTimeoutSeconds: Long,
    ) {
        cookieProvider()?.takeIf { it.isNotBlank() }?.let { cookie ->
            requestBuilder.header("cookie", cookie)
        }
        authTokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
            requestBuilder.header("authorization", "Bearer $token")
        }

        val explicitProxy = proxyProvider()
        val proxySelector = if (explicitProxy == null) proxySelectorProvider() else null
        val callClient = client.newBuilder().apply {
            when {
                explicitProxy != null -> proxy(explicitProxy)
                proxySelector != null -> this.proxySelector(proxySelector)
            }
            callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
            readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        }.build()

        val response = callClient.newCall(requestBuilder.build()).execute()
        try {
            if (!response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                throw NovalPieApiException(
                    statusCode = response.code,
                    path = label,
                    serverMessage = NovalPieApiException.extractServerMessage(responseBody),
                )
            }
            val body = response.body ?: throw IOException("响应没有内容")
            body.byteStream().use { input ->
                consumer(input, response.header("content-type"))
            }
        } finally {
            response.close()
        }
    }

    private fun uploadChaptersJson(chapters: List<UploadChapter>): JSONArray = JSONArray().apply {
        chapters.forEach { chapter ->
            put(
                JSONObject()
                    .put("title", chapter.title)
                    .put("content", chapter.content)
                    .put("chapter_number", chapter.chapterNumber)
                    .apply {
                        chapter.rawPath?.let { put("raw_path", it) }
                        chapter.spineIndex?.let { put("spine_index", it) }
                    }
            )
        }
    }

    private fun normalizeUploadResult(raw: Any): UploadActionResult {
        val source = unwrapObject(raw, "data", "result")
        return UploadActionResult(
            success = source.firstBooleanOrNull("success") ?: true,
            message = source.firstStringOrNull("message", "msg"),
            novelId = source.longOrNull("novel_id") ?: source.longOrNull("novelId") ?: source.longOrNull("id")
        )
    }

    private fun normalizeParsedEpub(raw: Any): ParsedEpub {
        val source = unwrapObject(raw, "data", "result")
        if (source.firstBooleanOrNull("success") == false) {
            throw IOException(source.firstStringOrNull("message") ?: "解析 EPUB 失败")
        }
        val metadata = source.optJSONObject("metadata") ?: JSONObject()
        val chapters = source.optJSONArray("chapters") ?: JSONArray()
        return ParsedEpub(
            title = metadata.firstStringOrNull("title").orEmpty(),
            author = metadata.firstStringOrNull("author", "creator").orEmpty(),
            description = metadata.firstStringOrNull("description").orEmpty(),
            language = metadata.firstStringOrNull("language").orEmpty().ifBlank { "zh" },
            epubFilePath = source.firstStringOrNull("epub_file_path", "file_path", "filePath"),
            chapters = (0 until chapters.length()).mapNotNull { index ->
                val chapter = chapters.optJSONObject(index) ?: return@mapNotNull null
                val sectionPath = chapter.optJSONArray("section_path")?.let { array ->
                    (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
                }.orEmpty()
                UploadChapter(
                    title = chapter.firstStringOrNull("title").orEmpty().ifBlank { "第 ${index + 1} 章" },
                    content = chapter.firstStringOrNull("content").orEmpty(),
                    chapterNumber = chapter.intOrNull("chapter_number") ?: index + 1,
                    hierarchyLevel = chapter.intOrNull("hierarchy_level") ?: 0,
                    sectionPath = sectionPath,
                    rawPath = chapter.firstStringOrNull("raw_path", "rawPath"),
                    spineIndex = chapter.intOrNull("spine_index") ?: chapter.intOrNull("spineIndex")
                )
            }
        )
    }

    private fun parseJsonOrString(body: String): Any {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return JSONObject()
        return when {
            trimmed.startsWith("{") -> JSONObject(trimmed)
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> trimmed
        }
    }

    private fun normalizeEpubDownloadTicket(raw: Any): EpubDownloadTicket {
        val root = raw as? JSONObject ?: throw IOException("下载授权响应格式无效")
        val source = unwrapObject(raw, "data", "result")
        val success = source.firstBooleanOrNull("success", "ok")
            ?: root.firstBooleanOrNull("success", "ok")
        if (success == false) {
            throw IOException(
                source.firstStringOrNull("message", "error", "detail")
                    ?: root.firstStringOrNull("message", "error", "detail")
                    ?: "下载授权失败"
            )
        }
        val fileName = source.firstStringOrNull("file_name", "fileName", "filename", "path")
            ?: root.firstStringOrNull("file_name", "fileName", "filename", "path")
            ?: throw IOException("下载授权未返回文件名")
        return EpubDownloadTicket(
            fileName = fileName,
            userPointsAfter = source.firstLongOrNull("user_points_after", "userPointsAfter", "points_after")
                ?: root.firstLongOrNull("user_points_after", "userPointsAfter", "points_after"),
            hasDownloadPurchase = source.firstBooleanOrNull(
                "has_download_purchase",
                "hasDownloadPurchase",
                "download_purchased",
            ) ?: root.firstBooleanOrNull(
                "has_download_purchase",
                "hasDownloadPurchase",
                "download_purchased",
            ) ?: false,
        )
    }

    private fun resolveAssetUrl(raw: String): okhttp3.HttpUrl? {
        val value = raw.trim()
        if (value.isBlank()) return null
        return value.toHttpUrlOrNull() ?: baseUrl.toHttpUrl().resolve(value)
    }

    private fun normalizeEditorProcessorUrl(endpoint: String): String {
        val candidate = endpoint.trim().let { value ->
            if (value.startsWith("http://") || value.startsWith("https://")) value else "http://$value"
        }
        val parsed = candidate.toHttpUrlOrNull() ?: throw IllegalArgumentException("Processor endpoint is invalid")
        require(parsed.scheme == "http" || parsed.scheme == "https") { "Processor endpoint must use HTTP or HTTPS" }
        return parsed.toString()
    }

    private fun decodeEditorProcessorString(raw: String): String {
        val decoded = runCatching { JSONTokener(raw).nextValue() }.getOrNull()
        return decoded as? String ?: raw
    }

    private fun normalizePoliticalExamSession(raw: Any): PoliticalExamSession {
        val source = unwrapObject(raw, "session", "data", "result")
        val exam = source.optJSONObject("exam")
            ?: throw IOException("Exam response did not contain exam questions")
        var singleChoice = normalizePoliticalExamQuestions(exam.optJSONArray("single_choice"))
        var multipleChoice = normalizePoliticalExamQuestions(exam.optJSONArray("multiple_choice"))
        if (singleChoice.isEmpty() && multipleChoice.size == 50) {
            singleChoice = multipleChoice.take(40)
            multipleChoice = multipleChoice.drop(40)
        }
        val paper = PoliticalExamPaper(
            singleChoice = singleChoice,
            multipleChoice = multipleChoice,
            trueFalse = normalizePoliticalExamQuestions(exam.optJSONArray("true_false")),
            fillBlank = normalizePoliticalExamQuestions(exam.optJSONArray("fill_blank"))
        )
        if (paper.totalQuestions == 0) throw IOException("Exam response contained no questions")
        return PoliticalExamSession(
            sessionId = source.firstStringOrNull("session_id", "sessionId", "id")
                ?: throw IOException("Exam response did not contain a session id"),
            remainingTimeSeconds = (source.intOrNull("remaining_time")
                ?: source.intOrNull("remainingTime")
                ?: 1800).coerceAtLeast(0),
            paper = paper
        )
    }

    private fun normalizePoliticalExamQuestions(raw: JSONArray?): List<PoliticalExamQuestion> {
        if (raw == null) return emptyList()
        return (0 until raw.length()).mapNotNull { index ->
            val source = raw.optJSONObject(index) ?: return@mapNotNull null
            val question = source.firstStringOrNull("question", "title", "text") ?: return@mapNotNull null
            val options = source.optJSONArray("options")?.toList()
                ?.mapNotNull { value -> value?.takeUnless { it == JSONObject.NULL }?.toString() }
                .orEmpty()
            PoliticalExamQuestion(question = question, options = options)
        }
    }

    private fun normalizePoliticalExamResult(raw: Any): PoliticalExamResult {
        val source = unwrapObject(raw, "data", "result")
        val score = source.intOrNull("score") ?: throw IOException("Exam result did not contain a score")
        val total = source.intOrNull("total") ?: throw IOException("Exam result did not contain a total")
        val passed = source.booleanOrNull("passed") ?: throw IOException("Exam result did not contain pass status")
        val details = linkedMapOf<String, List<PoliticalExamDetail>>()
        source.optJSONObject("details")?.let { detailObject ->
            val keys = detailObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val values = detailObject.optJSONArray(key) ?: continue
                details[key] = (0 until values.length()).mapNotNull { index ->
                    val detail = values.optJSONObject(index) ?: return@mapNotNull null
                    PoliticalExamDetail(
                        correct = detail.booleanOrNull("correct") ?: false,
                        question = detail.firstStringOrNull("question", "title"),
                        userAnswer = detail.valueAsDisplayText("user_answer", "userAnswer", "answer"),
                        correctAnswer = detail.valueAsDisplayText("correct_answer", "correctAnswer"),
                        explanation = detail.firstStringOrNull("explanation", "reason")
                    )
                }
            }
        }
        return PoliticalExamResult(
            score = score,
            total = total,
            passed = passed,
            details = details,
            token = source.firstStringOrNull("token", "access_token", "accessToken")
        )
    }

    private fun JSONObject.valueAsDisplayText(vararg keys: String): String? {
        for (key in keys) {
            if (!has(key) || isNull(key)) continue
            return when (val value = opt(key)) {
                is JSONArray -> value.toList().joinToString(", ") { it?.toString().orEmpty() }
                else -> value?.toString()
            }?.takeIf(String::isNotBlank)
        }
        return null
    }

    private fun normalizeNovelList(raw: Any, vararg preferredKeys: String): List<NovelCard> {
        val values = if (preferredKeys.isEmpty()) {
            extractArray(raw)
        } else {
            extractArray(raw, *preferredKeys)
                .ifEmpty { extractArray(unwrapObject(raw, *preferredKeys), *preferredKeys) }
        }
        return values.mapNotNull { value ->
            (value as? JSONObject)?.let(::normalizeBook)
        }
    }

    private fun normalizeSearchPage(
        raw: Any,
        requestedPage: Int,
        requestedLimit: Int
    ): SearchPage {
        val root = raw as? JSONObject
        val nestedData = root?.optJSONObject("data")
        val metadata = when {
            root == null -> JSONObject()
            root.has("total") || root.has("total_pages") || root.has("totalPages") || root.has("page") -> root
            nestedData != null -> nestedData
            else -> root
        }
        val pagination = metadata.optJSONObject("pagination")
            ?: root?.optJSONObject("pagination")
            ?: nestedData?.optJSONObject("pagination")
        fun metadataInt(vararg names: String): Int? {
            fun firstFrom(source: JSONObject?): Int? {
                if (source == null) return null
                for (name in names) {
                    source.intOrNull(name)?.let { return it }
                }
                return null
            }
            return firstFrom(pagination) ?: firstFrom(metadata) ?: firstFrom(root)
        }

        val page = (metadataInt("page", "current_page", "currentPage") ?: requestedPage).coerceAtLeast(1)
        val pageSize = (metadataInt("limit", "page_size", "pageSize", "per_page", "perPage")
            ?: requestedLimit).coerceAtLeast(1)
        val total = metadataInt("total", "total_count", "totalCount")?.takeIf { it >= 0 }
        val totalPages = metadataInt("total_pages", "totalPages", "pages")
            ?.takeIf { it > 0 }
            ?: total?.let { count -> ((count.toLong() + pageSize - 1L) / pageSize).toInt().coerceAtLeast(1) }

        return SearchPage(
            items = normalizeNovelList(raw, "results", "items", "novels", "books", "data"),
            page = page,
            pageSize = pageSize,
            total = total,
            totalPages = totalPages
        )
    }

    private fun normalizeBook(raw: Any): NovelCard {
        val source = unwrapObject(raw, "novel", "item", "data")
        // The detail endpoint exposes both a native and an original-platform read count.  Its
        // `novelRead` field is ambiguous in search responses, but is the original-platform count
        // when an explicit `siteReadCount` is present, as on the current source detail route.
        val explicitSiteReadCount = source.longOrNull("site_read_count")
            ?: source.longOrNull("siteReadCount")
        val ambiguousNovelReadCount = source.longOrNull("novel_read")
            ?: source.longOrNull("novelRead")
        val explicitSourceReadCount = source.longOrNull("source_read_count")
            ?: source.longOrNull("sourceReadCount")
            ?: source.longOrNull("original_read_count")
            ?: source.longOrNull("originalReadCount")
        val favoriteObjectId = source.longOrNull("object_id")
            ?: source.longOrNull("objectId")
        val favoriteType = source.firstStringOrNull("favorite_type", "favoriteType")
        val id = if (
            favoriteObjectId != null &&
            (
                favoriteType?.contains("novel", ignoreCase = true) == true ||
                    !source.firstStringOrNull("novel_title", "novelTitle", "object_name", "objectName").isNullOrBlank()
                )
        ) {
            favoriteObjectId
        } else {
            null
        } ?: source.longOrNull("id")
            ?: source.longOrNull("novel_id")
            ?: source.longOrNull("novelId")
            ?: 0L
        return NovelCard(
            id = id,
            title = source.stringOrNull("title")
                ?: source.stringOrNull("name")
                ?: source.stringOrNull("novel_title")
                ?: source.stringOrNull("object_name")
                ?: "Untitled",
            originalTitle = source.firstStringOrNull(
                "true_name",
                "trueName",
                "original_title",
                "originalTitle",
                "title_original",
                "titleOriginal",
                "raw_title",
                "rawTitle"
            ),
            author = source.objectStringOrNull("author", "name", "username", "display_name")
                ?: source.stringOrNull("author")
                ?: source.firstStringOrNull("author_name", "authorName", "writer_name", "writerName"),
            platform = source.firstStringOrNull(
                "platform",
                "favorite_type",
                "favoriteType",
                "source",
                "source_platform",
                "sourcePlatform"
            ),
            status = normalizeBookStatus(source),
            coverUrl = normalizeAssetUrl(
                source.firstStringOrNull(
                    "cover_url",
                    "coverUrl",
                    "photo_url",
                    "photoUrl",
                    "cover_image_url",
                    "coverImageUrl",
                    "cover_image",
                    "coverImage",
                    "image_url",
                    "imageUrl",
                    "thumbnail_url",
                    "thumbnail",
                    "photo",
                    "photo_path",
                    "photoPath",
                    "cover_path",
                    "coverPath",
                    "cover"
                )
            ),
            fullCoverUrl = normalizeAssetUrl(
                source.firstStringOrNull(
                    "photo_true_url",
                    "photoTrueUrl",
                    "photo_original_url",
                    "photoOriginalUrl",
                    "photo_ori_url",
                    "photoOriUrl",
                    "full_cover_url",
                    "fullCoverUrl",
                    "cover_full_url",
                    "coverFullUrl",
                    "original_cover_url",
                    "originalCoverUrl",
                    "cover_original_url",
                    "coverOriginalUrl",
                    "origin_cover_url",
                    "originCoverUrl",
                    "photo_true_path",
                    "photoTruePath",
                    "photo_original_path",
                    "photoOriginalPath",
                    "photo_ori_path",
                    "photoOriPath",
                    "original_photo_url",
                    "originalPhotoUrl",
                    "origin_photo_url",
                    "originPhotoUrl",
                    "photo_origin_url",
                    "photoOriginUrl",
                    "cover_original_path",
                    "coverOriginalPath",
                    "original_cover_path",
                    "originalCoverPath"
                )
            ),
            description = source.stringOrNull("description")
                ?: source.stringOrNull("summary")
                ?: source.stringOrNull("synopsis"),
            wordCount = source.longOrNull("word_count")
                ?: source.longOrNull("wordCount")
                ?: source.longOrNull("words")
                ?: source.longOrNull("font_number")
                ?: source.longOrNull("fontNumber"),
            favoriteCount = source.longOrNull("favorite_count")
                ?: source.longOrNull("favoriteCount")
                ?: source.longOrNull("favorites")
                ?: source.longOrNull("novel_like")
                ?: source.longOrNull("novelLike")
                ?: source.longOrNull("bookmark_count")
                ?: source.longOrNull("bookmarkCount")
                ?: source.longOrNull("collect_count")
                ?: source.longOrNull("collectCount"),
            siteReadCount = explicitSiteReadCount
                ?: ambiguousNovelReadCount
                ?: source.longOrNull("read_count")
                ?: source.longOrNull("readCount")
                ?: source.longOrNull("view_count")
                ?: source.longOrNull("viewCount")
                ?: source.longOrNull("views"),
            recommendCount = source.longOrNull("recommend")
                ?: source.longOrNull("recommend_count")
                ?: source.longOrNull("recommendCount")
                ?: source.longOrNull("site_recommend_count")
                ?: source.longOrNull("siteRecommendCount"),
            sourceReadCount = explicitSourceReadCount
                ?: ambiguousNovelReadCount?.takeIf { explicitSiteReadCount != null },
            sourceFavoriteCount = source.longOrNull("source_favorite_count")
                ?: source.longOrNull("sourceFavoriteCount")
                ?: source.longOrNull("original_favorite_count")
                ?: source.longOrNull("originalFavoriteCount"),
            updatedAt = source.stringOrNull("updated_at")
                ?: source.stringOrNull("updateTime")
                ?: source.stringOrNull("created_at"),
            tags = normalizeBookTags(source),
            createdAt = source.firstStringOrNull("created_at", "createdAt", "published_at", "publishedAt"),
            chapterCount = source.firstIntOrNull("chapter_num", "chapterNum", "chapter_count", "chapterCount"),
            maxChapterNumber = source.firstIntOrNull(
                "max_chapter_number",
                "maxChapterNumber",
                "max_chapter_num",
                "maxChapterNum",
            ),
            guarantorId = source.firstLongOrNull("guarantor_id", "guarantorId")
                ?: source.optJSONObject("guarantorInfo")?.firstLongOrNull("userId", "user_id")
                ?: source.optJSONObject("guarantor_info")?.firstLongOrNull("userId", "user_id"),
            guarantorName = source.firstStringOrNull("guarantor_name", "guarantorName")
                ?: source.objectStringOrNull("guarantorInfo", "username", "name", "display_name")
                ?: source.objectStringOrNull("guarantor_info", "username", "name", "display_name"),
            guaranteedAt = source.firstStringOrNull("guaranteed_at", "guaranteedAt")
                ?: source.optJSONObject("guarantorInfo")?.firstStringOrNull("guaranteedAt", "guaranteed_at")
                ?: source.optJSONObject("guarantor_info")?.firstStringOrNull("guaranteedAt", "guaranteed_at"),
            uploaderName = source.firstStringOrNull("uploader_name", "uploaderName", "uploaded_by", "uploadedBy")
                ?: source.objectStringOrNull("uploader", "username", "name", "display_name")
                ?: source.objectStringOrNull("uploaderInfo", "username", "name", "display_name")
                ?: source.objectStringOrNull("uploadUser", "username", "name", "display_name"),
            isAdult = source.firstBooleanOrNull("is_adult", "isAdult", "adult", "adultOnly"),
            allowDownload = source.firstBooleanOrNull("allowDownload", "allow_download"),
        )
    }

    private fun normalizeChapters(raw: Any): List<Chapter> {
        return extractArray(raw, "chapters", "items").mapIndexedNotNull { index, value ->
            val source = value as? JSONObject ?: return@mapIndexedNotNull null
            val id = source.longOrNull("id")
                ?: source.longOrNull("chapter_id")
                ?: source.longOrNull("chapterId")
                ?: return@mapIndexedNotNull null
            val number = source.intOrNull("chapter_number")
                    ?: source.intOrNull("chapterNumber")
                    ?: source.intOrNull("number")
                    ?: source.intOrNull("display_order")
                    ?: source.intOrNull("order")
            IndexedChapter(
                index = index,
                chapter = Chapter(
                    id = id,
                    title = source.stringOrNull("title")
                        ?: source.stringOrNull("name")
                        ?: source.stringOrNull("chapter_title")
                        ?: source.stringOrNull("chapter_name")
                        ?: "Chapter $id",
                    number = number,
                    wordCount = source.longOrNull("word_count")
                    ?: source.longOrNull("wordCount")
                    ?: source.longOrNull("words"),
                    imageCount = source.intOrNull("image_count")
                        ?: source.intOrNull("imageCount")
                        ?: source.intOrNull("illustration_count")
                        ?: source.intOrNull("illustrationCount"),
                    updatedAt = source.stringOrNull("updated_at")
                    ?: source.stringOrNull("updateTime")
                    ?: source.stringOrNull("created_at")
                )
            )
        }
            .sortedWith(compareBy<IndexedChapter> { it.chapter.number ?: Int.MAX_VALUE }.thenBy { it.index })
            .map { it.chapter }
    }

    private data class IndexedChapter(val index: Int, val chapter: Chapter)

    private data class ReaderSessionKey(
        val sessionId: String,
        val sessionKey: String,
        val cacheUntilMillis: Long,
    ) {
        fun isUsableAt(nowMillis: Long): Boolean = nowMillis < cacheUntilMillis
    }

    private fun readerSessionKey(forceRefresh: Boolean = false): ReaderSessionKey {
        val now = System.currentTimeMillis()
        cachedReaderSession?.takeIf { !forceRefresh && it.isUsableAt(now) }?.let { return it }

        return synchronized(readerSessionLock) {
            val synchronizedNow = System.currentTimeMillis()
            cachedReaderSession
                ?.takeIf { !forceRefresh && it.isUsableAt(synchronizedNow) }
                ?.let { return@synchronized it }

            val raw = get("/api/reader/session-key", headers = readerSignatureHeaders())
            val source = unwrapObject(raw, "data", "result", "session")
            val sessionId = source.firstStringOrNull("session_id", "sessionId", "id")
                ?: throw IOException("Reader session id is empty.")
            val sessionKey = source.firstStringOrNull("session_key", "sessionKey", "key")
                ?: throw IOException("Reader session key is empty.")
            ReaderSessionKey(
                sessionId = sessionId,
                sessionKey = sessionKey,
                cacheUntilMillis = readerSessionCacheUntilMillis(source, synchronizedNow),
            ).also { cachedReaderSession = it }
        }
    }

    private fun invalidateReaderSession() {
        cachedReaderSession = null
    }

    private fun readerSessionCacheUntilMillis(source: JSONObject, nowMillis: Long): Long {
        val rawExpiry = source.firstLongOrNull(
            "expires",
            "expires_at",
            "expiresAt",
            "expiry",
            "expiry_at",
            "expiryAt",
        )
        val expiryMillis = rawExpiry?.let { value ->
            if (value in 1L..9_999_999_999L) value * 1_000L else value
        }
        val serverDeadline = expiryMillis?.minus(READER_SESSION_EXPIRY_SKEW_MILLIS)
        return serverDeadline
            ?.takeIf { it > nowMillis + READER_SESSION_MIN_REUSE_MILLIS }
            ?.coerceAtMost(nowMillis + READER_SESSION_MAX_CACHE_MILLIS)
            ?: (nowMillis + READER_SESSION_FALLBACK_CACHE_MILLIS)
    }

    /** Only retry safe, idempotent reader reads when transport or a short-lived reader key is unstable. */
    private fun isRetryableReaderContentFailure(failure: Throwable): Boolean {
        var current: Throwable? = failure
        repeat(5) {
            when (current) {
                is CancellationException -> return false
                is NovalPieApiException -> {
                    // Content reads are GETs protected by a short-lived reader session. A stale
                    // key or transient proxy response should refresh the key once, not strand the
                    // continuous reader at a chapter boundary.
                    return current.statusCode in READER_CONTENT_RETRYABLE_STATUS_CODES
                }
                is ReaderContentUnavailableException -> return true
                is SocketTimeoutException,
                is ConnectException,
                is UnknownHostException -> return true
                is IOException -> {
                    val detail = current?.message.orEmpty().lowercase()
                    if (
                        detail.contains("unexpected end") ||
                        detail.contains("connection reset") ||
                        detail.contains("connection aborted") ||
                        detail.contains("broken pipe") ||
                        detail.contains("stream was reset")
                    ) {
                        return true
                    }
                }
            }
            current = current?.cause
            if (current == null) return false
        }
        return false
    }

    private fun normalizeReaderContent(raw: Any, readerSessionKey: String? = null): ReaderContent {
        if (raw is String && raw.isNotBlank()) {
            return ReaderContent(title = null, content = raw, source = "plain")
        }

        val topLevel = raw as? JSONObject
        val source = unwrapObject(raw, "data", "result", "chapter")
        val success = source.firstBooleanOrNull("success", "ok")
            ?: topLevel?.firstBooleanOrNull("success", "ok")
        if (success == false) {
            throw IOException(
                source.firstStringOrNull("message", "error", "detail")
                    ?: topLevel?.firstStringOrNull("message", "error", "detail")
                    ?: "章节内容加载失败"
            )
        }
        val content = if (source.optBoolean("encrypted", false)) {
            decryptReaderContent(
                encryptedContent = source.stringOrNull("content")
                    ?: throw IOException("Encrypted chapter content is empty."),
                iv = source.stringOrNull("iv")
                    ?: throw IOException("Encrypted chapter iv is empty."),
                tag = source.stringOrNull("tag")
                    ?: throw IOException("Encrypted chapter tag is empty."),
                sessionKey = readerSessionKey
                    ?: throw IOException("Encrypted chapter session key is empty.")
            )
        } else {
            source.stringOrNull("content")
                ?: source.stringOrNull("html")
                ?: source.stringOrNull("body_html")
                ?: source.stringOrNull("bodyHtml")
                ?: source.stringOrNull("text")
                ?: source.stringOrNull("body")
                // A proxy tunnel that is dropped after headers can surface as a nominally
                // successful response with an empty body. Mark it separately so the safe
                // chapter-read retry can recover instead of leaving infinite scroll stranded.
                ?: throw ReaderContentUnavailableException("Chapter content is empty.")
        }
        return ReaderContent(
            title = source.stringOrNull("title")
                ?: source.stringOrNull("chapter_title")
                ?: source.stringOrNull("chapter_name"),
            content = content,
            source = "api",
            illustrations = normalizeReaderContentIllustrations(raw, source)
        )
    }

    private fun normalizeReaderContentIllustrations(raw: Any, source: JSONObject): List<ChapterIllustration> {
        val direct = normalizeChapterIllustrationItems(
            extractArray(
                source,
                "illustrations",
                "illustration_list",
                "illustrationList",
                "images",
                "chapter_images",
                "chapterImages",
                "chapter_illustrations",
                "chapterIllustrations",
                "image_list",
                "imageList",
                "photo_list",
                "photoList",
                "photos",
                "own_photos",
                "ownPhotos"
            )
        )
        if (direct.isNotEmpty()) return direct

        val nestedChapter = source.optJSONObject("chapter")
        if (nestedChapter != null) {
            val nested = normalizeChapterIllustrationItems(
                extractArray(
                    nestedChapter,
                    "illustrations",
                    "illustration_list",
                    "illustrationList",
                    "images",
                    "chapter_images",
                    "chapterImages",
                    "chapter_illustrations",
                    "chapterIllustrations",
                    "image_list",
                    "imageList",
                    "photo_list",
                    "photoList",
                    "photos",
                    "own_photos",
                    "ownPhotos"
                )
            )
            if (nested.isNotEmpty()) return nested
        }

        return normalizeChapterIllustrationItems(
            extractArray(
                raw,
                "illustrations",
                "illustration_list",
                "illustrationList",
                "images",
                "chapter_images",
                "chapterImages",
                "chapter_illustrations",
                "chapterIllustrations",
                "image_list",
                "imageList",
                "photo_list",
                "photoList",
                "photos",
                "own_photos",
                "ownPhotos"
            )
        )
    }

    private fun normalizeChapterIllustrationItems(values: List<Any?>): List<ChapterIllustration> =
        values.mapIndexedNotNull { fallbackIndex, value ->
            val item = value as? JSONObject ?: return@mapIndexedNotNull null
            normalizeChapterIllustrationItem(item, fallbackIndex)
        }

    private fun readerSignatureHeaders(): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000L).toString()
        val nonce = randomNonce()
        val rotatedTimestamp = rotateLeft3Hex(timestamp)
        val digest = md5Hex("$USER_AGENT$timestamp$nonce")
        val payload = sha256Hex("$digest$READER_SIGNATURE_SECRET$rotatedTimestamp")
        val hmacKey = md5Hex(READER_SIGNATURE_SECRET)
        val signature = customBase64(hmacSha1(hmacKey, payload))
        return mapOf(
            "X-Client-Signature" to signature,
            "X-Client-Timestamp" to timestamp,
            "X-Client-Nonce" to nonce
        )
    }

    private fun decryptReaderContent(
        encryptedContent: String,
        iv: String,
        tag: String,
        sessionKey: String
    ): String {
        val decodedSessionKey = decodeBase64(sessionKey)
        val aesKey = MessageDigest.getInstance("SHA-256").digest(decodedSessionKey)
        val ciphertext = decodeBase64(encryptedContent)
        val authTag = decodeBase64(tag)
        val cipherInput = ciphertext + authTag
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(aesKey, "AES"),
            GCMParameterSpec(128, decodeBase64(iv))
        )
        return cipher.doFinal(cipherInput).toString(Charsets.UTF_8)
    }

    private fun randomNonce(): String =
        (1..8)
            .map { READER_NONCE_ALPHABET[secureRandom.nextInt(READER_NONCE_ALPHABET.length)] }
            .joinToString("")

    private fun rotateLeft3Hex(timestamp: String): String {
        val rotated = Integer.rotateLeft(timestamp.toLong().toInt(), 3)
        return (rotated.toLong() and 0xffffffffL).toString(16)
    }

    private fun md5Hex(value: String): String = digestHex("MD5", value)

    private fun sha256Hex(value: String): String = digestHex("SHA-256", value)

    private fun digestHex(algorithm: String, value: String): String =
        MessageDigest.getInstance(algorithm)
            .digest(value.toByteArray(Charsets.UTF_8))
            .toHexString()

    private fun hmacSha1(key: String, message: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8))
    }

    private fun customBase64(value: ByteArray): String =
        Base64.encodeToString(value, Base64.NO_WRAP)
            .map { char ->
                val index = STANDARD_BASE64_ALPHABET.indexOf(char)
                if (index >= 0) READER_BASE64_ALPHABET[index] else char
            }
            .joinToString("")

    private fun decodeBase64(value: String): ByteArray = Base64.decode(value, Base64.DEFAULT)

    private fun ByteArray.toHexString(): String = joinToString("") { byte -> "%02x".format(byte) }

    private fun normalizeFavoriteGroups(raw: Any): List<FavoriteGroup> {
        return extractArray(raw, "groups", "favorite_groups", "favoriteGroups", "items").mapNotNull { value ->
            (value as? JSONObject)?.let(::normalizeFavoriteGroup)
        }
    }

    private fun normalizeFavoriteGroup(raw: Any): FavoriteGroup =
        normalizeFavoriteGroup(
            unwrapObject(raw, "group", "favorite_group", "favoriteGroup", "data", "result")
        )

    private fun normalizeFavoriteGroup(source: JSONObject): FavoriteGroup {
        val previewValues = sequenceOf("preview", "previews", "preview_items", "previewItems", "items")
            .mapNotNull { key -> (source.opt(key) as? JSONArray)?.toList() }
            .firstOrNull()
            .orEmpty()
        return FavoriteGroup(
            id = source.longOrNull("id") ?: source.longOrNull("group_id"),
            name = source.stringOrNull("name")
                ?: source.stringOrNull("title")
                ?: source.stringOrNull("group_name")
                ?: source.stringOrNull("groupName")
                ?: "Group",
            count = source.intOrNull("count")
                ?: source.intOrNull("novel_count")
                ?: source.intOrNull("novelCount")
                ?: source.intOrNull("book_count")
                ?: source.intOrNull("bookCount")
                ?: source.intOrNull("books_count")
                ?: source.intOrNull("booksCount"),
            previews = previewValues.mapNotNull { value ->
                (value as? JSONObject)?.let(::normalizeFavoriteEntry)
            }
        )
    }

    private fun normalizeFavoritePage(
        raw: Any,
        requestedPage: Int,
        requestedPageSize: Int
    ): FavoritePage {
        val root = raw as? JSONObject
        val pagination = root?.optJSONObject("pagination")
            ?: root?.optJSONObject("data")?.optJSONObject("pagination")
        val entries = extractArray(raw, "favorites", "items", "history", "records", "list", "data")
            .mapNotNull { value -> (value as? JSONObject)?.let(::normalizeFavoriteEntry) }
        return FavoritePage(
            items = entries,
            page = pagination?.intOrNull("page") ?: requestedPage.coerceAtLeast(1),
            pageSize = pagination?.intOrNull("page_size")
                ?: pagination?.intOrNull("pageSize")
                ?: pagination?.intOrNull("limit")
                ?: requestedPageSize.coerceAtLeast(1),
            total = pagination?.intOrNull("total")
                ?: root?.optJSONObject("data")?.intOrNull("total")
                ?: root?.intOrNull("total"),
            totalPages = pagination?.intOrNull("total_pages")
                ?: pagination?.intOrNull("totalPages")
                ?: pagination?.intOrNull("pages")
                ?: root?.optJSONObject("data")?.intOrNull("pages")
                ?: root?.intOrNull("pages")
        )
    }

    private fun normalizeFavoriteEntry(source: JSONObject): FavoriteEntry? {
        val book = normalizeBook(source)
        if (book.id <= 0L) return null
        val objectId = source.longOrNull("object_id") ?: source.longOrNull("objectId")
        val rawId = source.longOrNull("id")
        val favoriteId = source.longOrNull("favorite_id")
            ?: source.longOrNull("favoriteId")
            ?: rawId?.takeIf { objectId == null || it != objectId }
        return FavoriteEntry(
            favoriteId = favoriteId,
            book = book,
            groupId = source.longOrNull("group_id") ?: source.longOrNull("groupId"),
            groupName = source.firstStringOrNull("group_name", "groupName"),
            isPinned = source.firstBooleanOrNull("is_pinned", "isPinned", "pinned") ?: false,
            createdAt = source.firstStringOrNull("created_at", "createdAt", "favorited_at", "favoritedAt"),
            lastReadAt = source.firstStringOrNull("last_read_time", "lastReadTime", "read_at", "readAt"),
            lastChapterId = source.longOrNull("last_chapter_id")
                ?: source.longOrNull("lastChapterId")
                ?: source.longOrNull("chapter_id")
                ?: source.longOrNull("chapterId"),
            lastChapter = source.intOrNull("last_chapter")
                ?: source.intOrNull("lastChapter"),
            chapterCount = source.intOrNull("chapter_count")
                ?: source.intOrNull("chapterCount")
        )
    }

    /** Normalizes the current Spring-style `content` envelope used by `/api/terminologies`. */
    private fun normalizeTerminologyPage(
        raw: Any,
        requestedNovelId: Long,
        requestedPage: Int,
    ): TerminologyPage {
        val root = raw as? JSONObject
        val data = root?.optJSONObject("data") ?: root ?: JSONObject()
        val metadata = when {
            data.has("page") || data.has("size") || data.has("total") || data.has("totalPages") -> data
            root?.has("page") == true || root?.has("size") == true || root?.has("total") == true -> root
            else -> data
        }
        fun metadataInt(vararg names: String): Int? {
            fun firstFrom(source: JSONObject?): Int? {
                if (source == null) return null
                for (name in names) {
                    source.intOrNull(name)?.let { return it }
                }
                return null
            }
            return firstFrom(metadata) ?: firstFrom(data) ?: firstFrom(root)
        }

        val entries = extractArray(data, "content", "items", "records", "list")
            .ifEmpty { extractArray(raw, "content", "items", "records", "list") }
            .mapNotNull { value ->
                (value as? JSONObject)?.let { source ->
                    normalizeTerminologyEntry(source, requestedNovelId)
                }
            }

        val resolvedPage = (metadataInt("page", "current_page", "currentPage") ?: requestedPage)
            .coerceAtLeast(0)
        val pageSize = (metadataInt("size", "page_size", "pageSize", "limit", "per_page") ?: 20)
            .coerceAtLeast(1)
        val total = metadataInt("total", "total_count", "totalCount")?.takeIf { it >= 0 }
        val totalPages = metadataInt("totalPages", "total_pages", "pages")
            ?.takeIf { it > 0 }
            ?: total?.let { count -> ((count.toLong() + pageSize - 1L) / pageSize).toInt().coerceAtLeast(1) }

        return TerminologyPage(
            items = entries,
            page = resolvedPage,
            pageSize = pageSize,
            total = total,
            totalPages = totalPages,
        )
    }

    private fun normalizeTerminologyEntry(
        source: JSONObject,
        fallbackNovelId: Long,
    ): TerminologyEntry? {
        val id = source.longOrNull("id") ?: return null
        if (id <= 0L) return null
        val info = source.optJSONObject("info")
        val sourceName = source.firstStringOrNull(
            "sourceName",
            "source_name",
            "source",
            "original",
            "original_name",
        ).orEmpty()
        val targetName = source.firstStringOrNull(
            "targetName",
            "target_name",
            "target",
            "translation",
            "translated_name",
        ).orEmpty()
        return TerminologyEntry(
            id = id,
            novelId = source.longOrNull("novelId")
                ?: source.longOrNull("novel_id")
                ?: fallbackNovelId,
            sourceName = sourceName,
            targetName = targetName,
            description = info?.firstStringOrNull("description", "remark", "note")
                ?: source.firstStringOrNull("description", "remark", "note"),
            lockStatus = source.firstStringOrNull("lockStatus", "lock_status", "locked"),
            isActive = source.firstBooleanOrNull("isActive", "is_active", "active", "enabled"),
            createdAt = source.firstStringOrNull("createdAt", "created_at"),
            updatedAt = source.firstStringOrNull("updatedAt", "updated_at"),
        )
    }

    private fun normalizeNovelTags(raw: Any): List<NovelTag> {
        return extractArray(raw, "tags", "data", "items", "records", "list").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            val name = source.firstStringOrNull("name", "tag_name", "tagName", "label", "title")
                ?: return@mapNotNull null
            NovelTag(
                id = source.longOrNull("id") ?: source.longOrNull("tag_id") ?: source.longOrNull("tagId"),
                name = name,
                count = source.intOrNull("count")
                    ?: source.intOrNull("book_count")
                    ?: source.intOrNull("bookCount")
                    ?: source.intOrNull("novel_count")
                    ?: source.intOrNull("novelCount")
            )
        }
    }

    private fun normalizeMessagePage(
        raw: Any,
        requestedPage: Int,
        requestedPageSize: Int
    ): MessagePage {
        val root = raw as? JSONObject
        val pagination = root?.optJSONObject("pagination")
            ?: root?.optJSONObject("data")?.optJSONObject("pagination")
        return MessagePage(
            items = normalizeMessages(raw),
            pagination = MessagePagination(
                page = pagination?.intOrNull("page") ?: requestedPage.coerceAtLeast(1),
                pageSize = pagination?.intOrNull("page_size")
                    ?: pagination?.intOrNull("pageSize")
                    ?: requestedPageSize.coerceAtLeast(1),
                total = pagination?.intOrNull("total")
                    ?: pagination?.intOrNull("total_count")
                    ?: pagination?.intOrNull("totalCount")
                    ?: 0,
                totalPages = pagination?.intOrNull("total_pages")
                    ?: pagination?.intOrNull("totalPages")
                    ?: 1
            )
        )
    }

    private fun normalizeMessages(raw: Any): List<SiteMessage> =
        extractArray(raw, "list", "messages", "items", "records", "data")
            .mapNotNull { value -> (value as? JSONObject)?.let(::normalizeMessage) }

    private fun normalizeMessage(source: JSONObject): SiteMessage? {
        val id = source.longOrNull("id")
            ?: source.longOrNull("message_id")
            ?: source.longOrNull("messageId")
            ?: return null
        val extra = source.optJSONObject("extra_data")
            ?: source.optJSONObject("extraData")
        return SiteMessage(
            id = id,
            type = source.intOrNull("message_type")
                ?: source.intOrNull("messageType")
                ?: 0,
            title = source.firstStringOrNull("message_title", "messageTitle", "title")
                ?: "Message",
            content = source.firstStringOrNull("message_content", "messageContent", "content", "body"),
            username = source.firstStringOrNull("username", "user_name", "userName", "sender_name", "senderName"),
            createdAt = source.firstStringOrNull("created_at", "createdAt", "sent_at", "sentAt"),
            isRead = source.firstBooleanOrNull("is_read", "isRead", "read") ?: false,
            isStarred = source.firstBooleanOrNull("is_starred", "isStarred", "starred") ?: false,
            priority = source.intOrNull("priority") ?: 0,
            actionUrl = source.firstStringOrNull("action_url", "actionUrl"),
            actionText = source.firstStringOrNull("action_text", "actionText"),
            readAt = source.firstStringOrNull("read_at", "readAt"),
            userId = source.longOrNull("user_id") ?: source.longOrNull("userId"),
            executeUserId = source.longOrNull("execute_user_id") ?: source.longOrNull("executeUserId"),
            avatarUrl = source.firstStringOrNull("avatar", "avatar_url", "avatarUrl"),
            avatarFrameUrl = source.firstStringOrNull("avatar_frame", "avatarFrame", "avatar_frame_url", "avatarFrameUrl"),
            extraData = extra?.toStringMap().orEmpty()
        )
    }

    private fun normalizeMessageStats(raw: Any): MessageStats {
        val source = unwrapObject(raw, "stats", "data", "result")
        val unreadByType = mutableMapOf<Int, Int>()
        val unreadObject = source.optJSONObject("unread_by_type")
            ?: source.optJSONObject("unreadByType")
        unreadObject?.keys()?.forEach { key ->
            key.toIntOrNull()?.let { type -> unreadByType[type] = unreadObject.optInt(key, 0) }
        }
        return MessageStats(
            totalCount = source.intOrNull("total_count") ?: source.intOrNull("totalCount") ?: 0,
            unreadCount = source.intOrNull("unread_count") ?: source.intOrNull("unreadCount") ?: 0,
            readCount = source.intOrNull("read_count") ?: source.intOrNull("readCount") ?: 0,
            starredCount = source.intOrNull("starred_count") ?: source.intOrNull("starredCount") ?: 0,
            importantCount = source.intOrNull("important_count") ?: source.intOrNull("importantCount") ?: 0,
            recentSevenDaysCount = source.intOrNull("recent_7days_count")
                ?: source.intOrNull("recentSevenDaysCount")
                ?: 0,
            unreadByType = unreadByType
        )
    }

    private fun normalizeWorkspaceApiConfigs(raw: Any): List<WorkspaceApiConfig> =
        extractArray(raw, "data", "apis", "items", "list", "records").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            val id = source.longOrNull("id") ?: return@mapNotNull null
            val activationStatus = source.firstStringOrNull(
                "status",
                "activation_status",
                "activationStatus"
            )
            WorkspaceApiConfig(
                id = id,
                name = source.firstStringOrNull("name", "api_name", "apiName") ?: "API #$id",
                model = source.firstStringOrNull("model", "model_name", "modelName").orEmpty(),
                endpoint = source.firstStringOrNull("endpoint", "base_url", "baseUrl").orEmpty(),
                apiKey = source.firstStringOrNull("key", "api_key", "apiKey"),
                concurrency = source.intOrNull("concurrency") ?: 10,
                isActive = source.firstBooleanOrNull("is_active", "isActive", "active")
                    ?: workspaceApiStatusIsActive(activationStatus)
                    ?: true,
                isHealthy = source.firstBooleanOrNull("is_healthy", "isHealthy", "healthy"),
                approvalStatus = source.firstStringOrNull("approval_status", "approvalStatus"),
                totalRequests = source.longOrNull("totalRequests")
                    ?: source.longOrNull("total_requests")
                    ?: source.longOrNull("callCount")
                    ?: source.longOrNull("call_count")
                    ?: 0,
                activationStatus = activationStatus,
                actualStatus = source.firstStringOrNull("actual_status", "actualStatus")
            )
        }

    private fun workspaceApiStatusIsActive(status: String?): Boolean? = when (status?.trim()?.lowercase()) {
        "active", "enabled", "on" -> true
        "inactive", "disabled", "off", "suspended" -> false
        else -> null
    }

    private fun normalizeWorkspaceCookieConfigs(raw: Any): WorkspaceCookieConfigs {
        val source = unwrapObject(raw, "data", "result")
        fun normalizeList(vararg keys: String): List<WorkspaceCookieConfig> {
            val values = keys.firstNotNullOfOrNull { key -> source.optJSONArray(key)?.toList() }.orEmpty()
            return values.mapNotNull { value ->
                val item = value as? JSONObject ?: return@mapNotNull null
                val id = item.longOrNull("id") ?: return@mapNotNull null
                WorkspaceCookieConfig(
                    id = id,
                    configKey = item.firstStringOrNull("config_key", "configKey") ?: "cookie-$id",
                    description = item.firstStringOrNull("description", "desc"),
                    cookieRaw = item.firstStringOrNull("cookie_raw", "cookieRaw"),
                    proxyIp = item.firstStringOrNull("proxy_ip", "proxyIp"),
                    isActive = item.firstBooleanOrNull("is_active", "isActive", "active") ?: true,
                    isHealthy = item.firstBooleanOrNull("is_healthy", "isHealthy", "healthy"),
                    lastCheckAt = item.firstStringOrNull("last_check_at", "lastCheckAt"),
                    updatedByUsername = item.firstStringOrNull("updated_by_username", "updatedByUsername", "provider_username")
                )
            }
        }
        return WorkspaceCookieConfigs(
            myConfigs = normalizeList("myConfigs", "my_configs"),
            sharedConfigs = normalizeList("otherConfigs", "other_configs", "sharedConfigs", "shared_configs")
        )
    }

    private fun normalizeWorkspaceApiStatus(raw: Any): WorkspaceApiStatus {
        val source = unwrapObject(raw, "apiStatus", "api_status", "data", "result")
        return WorkspaceApiStatus(
            total = source.intOrNull("total") ?: source.intOrNull("total_count") ?: 0,
            active = source.intOrNull("active") ?: source.intOrNull("active_count") ?: 0,
            healthy = source.intOrNull("healthy") ?: source.intOrNull("healthy_count") ?: 0,
            totalRequests = source.longOrNull("total_requests")
                ?: source.longOrNull("totalRequests")
                ?: 0
        )
    }

    private fun normalizeWorkspaceTranslators(raw: Any): List<WorkspaceTranslatorHealth> {
        val source = unwrapObject(raw, "data", "result")
        val values = source.optJSONArray("translators")?.toList()
            ?: extractArray(raw, "translators", "items", "list")
        return values.mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            val id = source.longOrNull("id") ?: return@mapNotNull null
            WorkspaceTranslatorHealth(
                id = id,
                name = source.firstStringOrNull("name", "translatorName", "translator_name") ?: "Translator #$id",
                model = source.firstStringOrNull("model", "modelName", "model_name"),
                endpoint = source.firstStringOrNull("endpoint", "baseUrl", "base_url"),
                isHealthy = source.firstBooleanOrNull("isHealthy", "is_healthy", "healthy") ?: false,
                isActive = source.firstBooleanOrNull("isActive", "is_active", "active") ?: false,
                approvalStatus = source.firstStringOrNull("approval_status", "approvalStatus"),
                responseTimeMs = source.longOrNull("responseTime")
                    ?: source.longOrNull("response_time")
                    ?: source.longOrNull("responseTimeMs")
                    ?: 0,
                successRate = source.doubleOrNull("successRate")
                    ?: source.doubleOrNull("success_rate")
                    ?: source.doubleOrNull("uptime")
                    ?: 0.0,
                lastHealthError = source.firstStringOrNull("lastHealthError", "last_health_error", "lastError", "last_error")
            )
        }
    }

    private fun normalizeWorkspaceActionResult(raw: Any): WorkspaceActionResult {
        val source = unwrapObject(raw, "data", "result")
        return WorkspaceActionResult(
            success = booleanFromAny(raw)
                ?: source.firstBooleanOrNull("success", "ok", "status")
                ?: true,
            message = source.firstStringOrNull("message", "msg", "detail"),
            id = source.longOrNull("id")
        )
    }

    private fun normalizeMessageActionResult(raw: Any): MessageActionResult {
        val source = unwrapObject(raw, "data", "result")
        return MessageActionResult(
            success = booleanFromAny(raw)
                ?: source.firstBooleanOrNull("success", "ok", "status")
                ?: true,
            message = source.firstStringOrNull("message", "msg", "detail")
        )
    }

    private fun normalizeMessageSettings(raw: Any): MessageSettings {
        val source = unwrapObject(raw, "settings", "data", "result")
        val notificationTypes = source.optJSONArray("notification_types")
            ?: source.optJSONArray("notificationTypes")
        return MessageSettings(
            enableNotifications = source.firstBooleanOrNull("enable_notifications", "enableNotifications") ?: true,
            enableEmail = source.firstBooleanOrNull("enable_email", "enableEmail") ?: false,
            enableBrowserPush = source.firstBooleanOrNull("enable_browser_push", "enableBrowserPush") ?: true,
            notificationTypes = notificationTypes?.let { array ->
                (0 until array.length()).mapNotNull { index -> array.opt(index)?.toString()?.toIntOrNull() }.toSet()
            },
            quietHoursStart = source.firstStringOrNull("quiet_hours_start", "quietHoursStart"),
            quietHoursEnd = source.firstStringOrNull("quiet_hours_end", "quietHoursEnd"),
            autoReadAfterDays = source.intOrNull("auto_read_after_days")
                ?: source.intOrNull("autoReadAfterDays")
        )
    }

    private fun normalizeDirectMessages(raw: Any): List<DirectMessage> =
        extractArray(raw, "list", "messages", "items", "records", "data").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            val id = source.longOrNull("id")
                ?: source.longOrNull("message_id")
                ?: source.longOrNull("messageId")
                ?: return@mapNotNull null
            val content = source.firstStringOrNull("message_content", "messageContent", "content", "body")
                ?: return@mapNotNull null
            DirectMessage(
                id = id,
                content = content,
                createdAt = source.firstStringOrNull("created_at", "createdAt", "sent_at", "sentAt"),
                userId = source.longOrNull("user_id") ?: source.longOrNull("userId"),
                executeUserId = source.longOrNull("execute_user_id") ?: source.longOrNull("executeUserId")
            )
        }

    private fun normalizeFavoriteStatus(raw: Any): FavoriteStatus {
        val source = unwrapObject(raw, "data", "result", "favorite", "item")
        val rawState = source.firstStringOrNull(
            "state",
            "status",
            "favorite_status",
            "favoriteStatus",
            "status_text",
            "statusText",
            "message"
        )
        val isFavorited = booleanFromAny(raw)
            ?: source.firstBooleanOrNull(
                "is_favorited",
                "isFavorited",
                "is_favorite",
                "isFavorite",
                "favorited",
                "favorite",
                "exists",
                "status",
                "collected",
                "in_favorites",
                "inFavorites"
            )
            ?: rawState?.let(::statusTextToBoolean)
            ?: false

        return FavoriteStatus(
            isFavorited = isFavorited,
            groupId = source.longOrNull("group_id")
                ?: source.longOrNull("groupId")
                ?: source.longOrNull("favorite_group_id")
                ?: source.longOrNull("favoriteGroupId")
                ?: (source.opt("group") as? JSONObject)?.longOrNull("id")
                ?: (source.opt("favorite_group") as? JSONObject)?.longOrNull("id")
                ?: (source.opt("favoriteGroup") as? JSONObject)?.longOrNull("id"),
            rawState = rawState
        )
    }

    private fun normalizeForumPosts(raw: Any, forceBookReview: Boolean = false): List<ForumPost> {
        return extractArray(raw, "posts", "items", "records", "list", "data").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            normalizeForumPost(source, forceBookReview = forceBookReview)
        }
    }

    private fun normalizeForumPostsPage(
        raw: Any,
        forceBookReview: Boolean = false,
        requestedPage: Int = 1,
        requestedLimit: Int = 20
    ): ForumPostPage {
        val root = raw as? JSONObject
        val data = root?.optJSONObject("data")
        val result = root?.optJSONObject("result")
        val pagination = root?.optJSONObject("pagination")
            ?: data?.optJSONObject("pagination")
            ?: result?.optJSONObject("pagination")
        val metadataSources = listOfNotNull(pagination, data, result, root)

        fun metadataInt(vararg keys: String): Int? {
            for (source in metadataSources) {
                for (key in keys) {
                    source.intOrNull(key)?.let { return it }
                }
            }
            return null
        }

        val page = metadataInt("page", "current_page", "currentPage")?.coerceAtLeast(1)
            ?: requestedPage.coerceAtLeast(1)
        val pageSize = metadataInt("limit", "page_size", "pageSize", "per_page", "perPage")
            ?.coerceAtLeast(1)
            ?: requestedLimit.coerceAtLeast(1)
        val total = metadataInt("total", "count", "total_count", "totalCount")
        val totalPages = metadataInt("pages", "total_pages", "totalPages", "page_count", "pageCount")
            ?.coerceAtLeast(1)
            ?: total?.let { count ->
                ((count.toLong() + pageSize - 1L) / pageSize).toInt().coerceAtLeast(1)
            }

        return ForumPostPage(
            posts = normalizeForumPosts(raw, forceBookReview = forceBookReview),
            total = total,
            page = page,
            totalPages = totalPages
        )
    }

    private fun normalizeForumPost(raw: JSONObject, forceBookReview: Boolean = false): ForumPost {
        val source = unwrapObject(raw, "post", "item", "data")
        val id = source.longOrNull("id")
            ?: source.longOrNull("post_id")
            ?: source.longOrNull("postId")
            ?: source.longOrNull("topic_id")
            ?: source.longOrNull("topicId")
            ?: 0L
        val rawCategory = source.firstStringOrNull(
            "type",
            "category",
            "category_name",
            "categoryName",
            "section",
            "forum_type"
        )
        val linkedBook = source.optJSONObject("book") ?: source.optJSONObject("novel")
        val title = source.firstStringOrNull("title", "subject", "name")
            ?: plainSnippet(source.firstStringOrNull("content", "body", "text", "summary", "excerpt"))?.take(32)
            ?: "站内讨论"
        val tags = linkedSetOf<String>()
        val tagAliases = arrayOf("tags", "tag", "tag_names", "tagNames", "labels", "keywords")
        for (key in tagAliases) {
            when (val tagValue = source.opt(key)) {
                is JSONArray -> tags.addAll(normalizeTags(tagValue.toList()))
                is String -> tags.addAll(splitTagString(tagValue))
            }
        }

        return ForumPost(
            id = id,
            category = normalizeForumCategory(rawCategory),
            title = title,
            authorName = source.objectStringOrNull("author", "name", "username", "display_name", "nickname")
                ?: source.objectStringOrNull("user", "name", "username", "display_name", "nickname")
                ?: source.firstStringOrNull("author_name", "authorName", "username", "nickname"),
            authorAvatarUrl = normalizeAssetUrl(
                source.firstStringOrNull("author_avatar", "authorAvatar", "avatar", "avatar_url", "avatarUrl")
                    ?: source.objectStringOrNull("author", "avatar", "avatar_url", "avatarUrl")
                    ?: source.objectStringOrNull("user", "avatar", "avatar_url", "avatarUrl")
            ),
            authorAvatarFrameUrl = normalizeAssetUrl(
                source.firstStringOrNull(
                    "author_avatar_frame",
                    "authorAvatarFrame",
                    "avatar_frame",
                    "avatarFrame",
                    "avatar_frame_url",
                    "avatarFrameUrl"
                )
                    ?: source.objectStringOrNull("author", "avatar_frame", "avatarFrame", "avatar_frame_url", "avatarFrameUrl")
                    ?: source.objectStringOrNull("user", "avatar_frame", "avatarFrame", "avatar_frame_url", "avatarFrameUrl")
            ),
            authorBadges = normalizeForumAuthorBadges(source),
            authorBadgeVisuals = normalizeForumAuthorBadgeVisuals(source),
            authorId = source.optJSONObject("author")?.longOrNull("id")
                ?: source.optJSONObject("user")?.longOrNull("id")
                ?: source.longOrNull("user_id")
                ?: source.longOrNull("userId")
                ?: source.longOrNull("author_id")
                ?: source.longOrNull("authorId"),
            bookId = source.longOrNull("book_id")
                ?: source.longOrNull("bookId")
                ?: source.longOrNull("novel_id")
                ?: source.longOrNull("novelId")
                ?: linkedBook?.longOrNull("id")
                ?: linkedBook?.longOrNull("book_id")
                ?: linkedBook?.longOrNull("bookId"),
            bookTitle = linkedBook?.firstStringOrNull("title", "name", "novel_title", "novelTitle", "book_title", "bookTitle")
                ?: source.firstStringOrNull("novel_title", "novelTitle", "book_title", "bookTitle"),
            bookCoverUrl = normalizeAssetUrl(
                source.firstStringOrNull(
                    "book_cover",
                    "bookCover",
                    "book_cover_url",
                    "bookCoverUrl",
                    "novel_cover",
                    "novelCover"
                )
                    ?: linkedBook?.firstStringOrNull(
                        "cover",
                        "cover_url",
                        "coverUrl",
                        "photo_url",
                        "photoUrl",
                        "book_cover",
                        "bookCover"
                    )
            ),
            isBookReview = forceBookReview || rawCategory?.trim()?.lowercase() in setOf(
                "review",
                "reviews",
                "book_review",
                "book-reviews",
                "book_review_comment"
            ),
            replyCount = source.intOrNull("reply_count")
                ?: source.intOrNull("replyCount")
                ?: source.intOrNull("comments_count")
                ?: source.intOrNull("comment_count")
                ?: source.intOrNull("commentCount")
                ?: source.intOrNull("replies"),
            likeCount = source.intOrNull("like_count")
                ?: source.intOrNull("likeCount")
                ?: source.intOrNull("likes"),
            helpfulCount = source.intOrNull("helpful_count")
                ?: source.intOrNull("helpfulCount"),
            notHelpfulCount = source.intOrNull("not_helpful_count")
                ?: source.intOrNull("notHelpfulCount"),
            funnyCount = source.intOrNull("funny_count")
                ?: source.intOrNull("funnyCount"),
            reactionCount = source.intOrNull("reaction_count")
                ?: source.intOrNull("reactionCount")
                ?: source.intOrNull("reactions")
                ?: source.intOrNull("funny_count"),
            awardPoints = source.intOrNull("award_points")
                ?: source.intOrNull("awardPoints")
                ?: source.intOrNull("reward_points")
                ?: source.intOrNull("rewardPoints")
                ?: source.intOrNull("award_count")
                ?: source.intOrNull("awardCount")
                ?: source.intOrNull("awards"),
            viewCount = source.intOrNull("view_count")
                ?: source.intOrNull("viewCount")
                ?: source.intOrNull("views")
                ?: source.intOrNull("read_count")
                ?: source.intOrNull("readCount"),
            createdAt = source.firstStringOrNull("created_at", "createdAt"),
            lastActiveLabel = source.firstStringOrNull(
                "last_active_at",
                "lastActiveAt",
                "updated_at",
                "updatedAt",
                "created_at",
                "createdAt"
            ),
            excerpt = plainSnippet(
                source.firstStringOrNull(
                    "excerpt",
                    "summary",
                    "content",
                    "full_content",
                    "fullContent",
                    "body",
                    "text"
                )
            ),
            tags = tags.toList().take(5),
            pinned = source.firstBooleanOrNull("pinned", "is_pinned", "isPinned", "top", "is_top", "isTop") ?: false,
            featured = source.firstBooleanOrNull(
                "featured",
                "is_featured",
                "isFeatured",
                "essence",
                "is_essence",
                "isEssence",
                "starred",
                "is_starred",
                "isStarred"
            ) ?: false
        )
    }

    private fun normalizeForumAuthorBadges(source: JSONObject): List<String> {
        val badgeOwners = listOfNotNull(source, source.optJSONObject("author"), source.optJSONObject("user"))
        return buildList {
            for (owner in badgeOwners) {
                for (key in arrayOf("authorBadges", "author_badges", "badges")) {
                    when (val value = owner.opt(key)) {
                        is JSONArray -> addAll(normalizeTags(value.toList()))
                        is JSONObject -> addAll(normalizeTags(listOf(value)))
                        is String -> addAll(splitTagString(value))
                    }
                }
            }
        }.map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(4)
    }

    /**
     * Forum/review APIs return badge records with optional HTML/CSS presentation metadata. Keep
     * those records alongside the legacy text labels so Compose can render them safely without
     * ever evaluating server-provided markup or stylesheet code.
     */
    private fun normalizeForumAuthorBadgeVisuals(source: JSONObject): List<UserBadge> {
        val badgeOwners = listOfNotNull(source, source.optJSONObject("author"), source.optJSONObject("user"))
        return buildList {
            for (owner in badgeOwners) {
                for (key in arrayOf("authorBadges", "author_badges", "badges")) {
                    addAll(normalizeUserBadges(owner.opt(key)))
                }
            }
        }
            .filter { badge -> badge.name.isNotBlank() }
            .distinctBy { badge -> badge.id?.let { "id:$it" } ?: "name:${badge.name.trim().lowercase()}" }
            .take(4)
    }

    private fun normalizeForumPostDetail(raw: Any): ForumPostDetail {
        val source = unwrapObject(raw, "post", "item", "data")
        return ForumPostDetail(
            post = normalizeForumPost(source),
            content = source.firstStringOrNull(
                "content_html",
                "contentHtml",
                "body_html",
                "bodyHtml",
                "content",
                "body",
                "text"
            ),
            likeCount = source.intOrNull("like_count")
                ?: source.intOrNull("likeCount")
                ?: source.intOrNull("likes"),
            dislikeCount = source.intOrNull("dislike_count")
                ?: source.intOrNull("dislikeCount")
                ?: source.intOrNull("down_count")
                ?: source.intOrNull("downCount")
                ?: source.intOrNull("dislikes"),
            reactionCount = source.intOrNull("reaction_count")
                ?: source.intOrNull("reactionCount")
                ?: source.intOrNull("reactions"),
            awardPoints = source.intOrNull("award_points")
                ?: source.intOrNull("awardPoints")
                ?: source.intOrNull("reward_points")
                ?: source.intOrNull("rewardPoints")
        )
    }

    private fun normalizeForumComments(raw: Any): List<ForumComment> {
        val comments = mutableListOf<ForumComment>()
        extractArray(raw, "comments", "items", "records", "list", "data").forEach { value ->
            val source = value as? JSONObject ?: return@forEach
            appendForumCommentWithReplies(comments, source)
        }
        // The website occasionally repeats a child comment in both a top-level page and its
        // parent's `replies` array. Keep its first position so the visual thread remains stable.
        return comments.distinctBy(ForumComment::id)
    }

    private fun appendForumCommentWithReplies(
        output: MutableList<ForumComment>,
        source: JSONObject,
        fallbackPostId: Long? = null,
        fallbackParentCommentId: Long? = null,
        fallbackReplyToName: String? = null
    ) {
        val comment = normalizeForumComment(
            source = source,
            fallbackPostId = fallbackPostId,
            fallbackParentCommentId = fallbackParentCommentId,
            fallbackReplyToName = fallbackReplyToName
        ) ?: return
        output += comment
        extractArray(source, "replies", "children", "reply_list", "replyList").forEach { value ->
            val reply = value as? JSONObject ?: return@forEach
            appendForumCommentWithReplies(
                output = output,
                source = reply,
                fallbackPostId = comment.postId,
                fallbackParentCommentId = comment.id,
                fallbackReplyToName = comment.authorName
            )
        }
    }

    private fun normalizeForumComment(
        source: JSONObject,
        fallbackPostId: Long? = null,
        fallbackParentCommentId: Long? = null,
        fallbackReplyToName: String? = null
    ): ForumComment? {
        val id = source.longOrNull("id")
            ?: source.longOrNull("comment_id")
            ?: source.longOrNull("commentId")
            ?: return null
        val replies = extractArray(source, "replies", "children", "reply_list", "replyList")
        return ForumComment(
            id = id,
            postId = source.longOrNull("post_id")
                ?: source.longOrNull("postId")
                ?: fallbackPostId,
            parentCommentId = source.longOrNull("parent_comment_id")
                ?: source.longOrNull("parentCommentId")
                ?: source.longOrNull("comment_id_parent")
                ?: source.longOrNull("parent_id")
                ?: source.longOrNull("parentId")
                ?: fallbackParentCommentId,
            authorName = source.objectStringOrNull("author", "name", "username", "display_name", "nickname")
                ?: source.objectStringOrNull("user", "name", "username", "display_name", "nickname")
                ?: source.firstStringOrNull("author_name", "authorName", "username", "nickname"),
            authorAvatarUrl = normalizeAssetUrl(
                source.firstStringOrNull("author_avatar", "authorAvatar", "avatar", "avatar_url", "avatarUrl")
                    ?: source.objectStringOrNull("author", "avatar", "avatar_url", "avatarUrl")
                    ?: source.objectStringOrNull("user", "avatar", "avatar_url", "avatarUrl")
            ),
            authorAvatarFrameUrl = normalizeAssetUrl(
                source.firstStringOrNull(
                    "author_avatar_frame",
                    "authorAvatarFrame",
                    "avatar_frame",
                    "avatarFrame",
                    "avatar_frame_url",
                    "avatarFrameUrl"
                )
                    ?: source.objectStringOrNull("author", "avatar_frame", "avatarFrame", "avatar_frame_url", "avatarFrameUrl")
                    ?: source.objectStringOrNull("user", "avatar_frame", "avatarFrame", "avatar_frame_url", "avatarFrameUrl")
            ),
            authorBadges = normalizeForumAuthorBadges(source),
            authorBadgeVisuals = normalizeForumAuthorBadgeVisuals(source),
            authorId = source.optJSONObject("author")?.longOrNull("id")
                ?: source.optJSONObject("user")?.longOrNull("id")
                ?: source.longOrNull("user_id")
                ?: source.longOrNull("userId")
                ?: source.longOrNull("author_id")
                ?: source.longOrNull("authorId"),
            replyToName = source.firstStringOrNull("reply_to_name", "replyToName") ?: fallbackReplyToName,
            content = source.firstStringOrNull(
                "content_html",
                "contentHtml",
                "body_html",
                "bodyHtml",
                "content",
                "body",
                "text"
            ) ?: "",
            likeCount = source.intOrNull("like_count")
                ?: source.intOrNull("likeCount")
                ?: source.intOrNull("helpful_count")
                ?: source.intOrNull("helpfulCount")
                ?: source.intOrNull("likes"),
            dislikeCount = source.intOrNull("dislike_count")
                ?: source.intOrNull("dislikeCount")
                ?: source.intOrNull("not_helpful_count")
                ?: source.intOrNull("notHelpfulCount")
                ?: source.intOrNull("down_count")
                ?: source.intOrNull("downCount")
                ?: source.intOrNull("dislikes"),
            reactionCount = source.intOrNull("reaction_count")
                ?: source.intOrNull("reactionCount")
                ?: source.intOrNull("funny_count")
                ?: source.intOrNull("funnyCount")
                ?: source.intOrNull("reactions"),
            awardPoints = source.intOrNull("award_points")
                ?: source.intOrNull("awardPoints")
                ?: source.intOrNull("award_count")
                ?: source.intOrNull("awardCount")
                ?: source.intOrNull("reward_points")
                ?: source.intOrNull("rewardPoints"),
            replyCount = source.intOrNull("reply_count")
                ?: source.intOrNull("replyCount")
                ?: source.intOrNull("replies_count")
                ?: source.intOrNull("repliesCount")
                ?: replies.size.takeIf { it > 0 },
            createdAt = source.firstStringOrNull("created_at", "createdAt", "updated_at", "updatedAt")
        )
    }

    private fun normalizeChapterComments(raw: Any): List<ChapterComment> {
        val comments = mutableListOf<ChapterComment>()
        extractArray(raw, "comments", "items", "records", "list", "data").forEach { value ->
            val source = value as? JSONObject ?: return@forEach
            appendChapterCommentWithReplies(comments, source)
        }
        // The source may return a reply both inside its parent and in the top-level page. Keep the
        // first occurrence so native thread reconstruction does not render it twice.
        return comments.distinctBy(ChapterComment::id)
    }

    private fun appendChapterCommentWithReplies(
        output: MutableList<ChapterComment>,
        source: JSONObject,
        fallbackBookId: Long? = null,
        fallbackChapterId: Long? = null,
        fallbackParentCommentId: Long? = null,
        fallbackReplyToName: String? = null
    ) {
        val comment = normalizeChapterComment(
            source = source,
            fallbackBookId = fallbackBookId,
            fallbackChapterId = fallbackChapterId,
            fallbackParentCommentId = fallbackParentCommentId,
            fallbackReplyToName = fallbackReplyToName
        ) ?: return
        output += comment
        extractArray(source, "replies", "children", "reply_list", "replyList").forEach { value ->
            val reply = value as? JSONObject ?: return@forEach
            appendChapterCommentWithReplies(
                output = output,
                source = reply,
                fallbackBookId = comment.bookId,
                fallbackChapterId = comment.chapterId,
                fallbackParentCommentId = comment.id,
                fallbackReplyToName = comment.authorName
            )
        }
    }

    private fun normalizeChapterComment(
        source: JSONObject,
        fallbackBookId: Long? = null,
        fallbackChapterId: Long? = null,
        fallbackParentCommentId: Long? = null,
        fallbackReplyToName: String? = null
    ): ChapterComment? {
        val id = source.longOrNull("id")
            ?: source.longOrNull("comment_id")
            ?: source.longOrNull("commentId")
            ?: return null
        val replies = extractArray(source, "replies", "children", "reply_list", "replyList")
        return ChapterComment(
                id = id,
                bookId = source.longOrNull("book_id") ?: source.longOrNull("bookId") ?: fallbackBookId,
                chapterId = source.longOrNull("chapter_id") ?: source.longOrNull("chapterId") ?: fallbackChapterId,
                parentCommentId = source.longOrNull("parent_comment_id")
                    ?: source.longOrNull("parentCommentId")
                    ?: source.longOrNull("comment_id_parent")
                    ?: source.longOrNull("parent_id")
                    ?: source.longOrNull("parentId")
                    ?: fallbackParentCommentId,
                authorName = source.objectStringOrNull("author", "name", "username", "display_name", "nickname")
                    ?: source.objectStringOrNull("user", "name", "username", "display_name", "nickname")
                    ?: source.firstStringOrNull("author_name", "authorName", "username", "nickname"),
                authorAvatarUrl = normalizeAssetUrl(
                    source.firstStringOrNull("author_avatar", "authorAvatar", "avatar", "avatar_url", "avatarUrl")
                        ?: source.objectStringOrNull("author", "avatar", "avatar_url", "avatarUrl")
                        ?: source.objectStringOrNull("user", "avatar", "avatar_url", "avatarUrl")
                ),
                authorAvatarFrameUrl = normalizeAssetUrl(
                    source.firstStringOrNull(
                        "author_avatar_frame",
                        "authorAvatarFrame",
                        "avatar_frame",
                        "avatarFrame",
                        "avatar_frame_url",
                        "avatarFrameUrl"
                    )
                        ?: source.objectStringOrNull("author", "avatar_frame", "avatarFrame", "avatar_frame_url", "avatarFrameUrl")
                        ?: source.objectStringOrNull("user", "avatar_frame", "avatarFrame", "avatar_frame_url", "avatarFrameUrl")
                ),
                authorBadges = normalizeForumAuthorBadges(source),
                authorBadgeVisuals = normalizeForumAuthorBadgeVisuals(source),
                authorId = source.optJSONObject("author")?.longOrNull("id")
                    ?: source.optJSONObject("user")?.longOrNull("id")
                    ?: source.longOrNull("user_id")
                    ?: source.longOrNull("userId")
                    ?: source.longOrNull("author_id")
                    ?: source.longOrNull("authorId"),
                replyToName = source.firstStringOrNull("reply_to_name", "replyToName") ?: fallbackReplyToName,
                content = source.firstStringOrNull(
                    "content_html",
                    "contentHtml",
                    "body_html",
                    "bodyHtml",
                    "content",
                    "body",
                    "text"
                ) ?: "",
                likeCount = source.intOrNull("like_count")
                    ?: source.intOrNull("likeCount")
                    ?: source.intOrNull("helpful_count")
                    ?: source.intOrNull("helpfulCount")
                    ?: source.intOrNull("likes"),
                dislikeCount = source.intOrNull("dislike_count")
                    ?: source.intOrNull("dislikeCount")
                    ?: source.intOrNull("not_helpful_count")
                    ?: source.intOrNull("notHelpfulCount")
                    ?: source.intOrNull("down_count")
                    ?: source.intOrNull("downCount")
                    ?: source.intOrNull("dislikes"),
                reactionCount = source.intOrNull("reaction_count")
                    ?: source.intOrNull("reactionCount")
                    ?: source.intOrNull("funny_count")
                    ?: source.intOrNull("funnyCount")
                    ?: source.intOrNull("reactions"),
                awardPoints = source.intOrNull("award_points")
                    ?: source.intOrNull("awardPoints")
                    ?: source.intOrNull("award_count")
                    ?: source.intOrNull("awardCount")
                    ?: source.intOrNull("reward_points")
                    ?: source.intOrNull("rewardPoints"),
                replyCount = source.intOrNull("reply_count")
                    ?: source.intOrNull("replyCount")
                    ?: source.intOrNull("replies_count")
                    ?: source.intOrNull("repliesCount")
                    ?: replies.size.takeIf { it > 0 },
                createdAt = source.firstStringOrNull("created_at", "createdAt", "updated_at", "updatedAt")
            )
    }

    private fun normalizeAdminAction(raw: Any): UserCheckinAction {
        val source = unwrapObject(raw, "data", "result")
        return UserCheckinAction(
            success = source.firstBooleanOrNull("success", "ok") ?: true,
            message = source.firstStringOrNull("message", "msg", "detail")
        )
    }

    private fun normalizeForumActionResult(raw: Any): ForumActionResult {
        val source = unwrapObject(raw, "data", "result")
        val success = booleanFromAny(raw)
            ?: source.firstBooleanOrNull("success", "ok", "status")
            ?: true
        return ForumActionResult(
            success = success,
            message = source.firstStringOrNull("message", "msg", "detail")
        )
    }

    private fun normalizeChapterIllustrations(raw: Any): ChapterIllustrationPage {
        val source = unwrapObject(raw, "data", "result")
        val images = normalizeChapterIllustrationItems(extractArray(raw, "images", "items", "data", "list"))
        return ChapterIllustrationPage(
            images = images,
            total = source.intOrNull("total")
                ?: source.intOrNull("image_count")
                ?: source.intOrNull("imageCount")
                ?: images.size
        )
    }

    private fun normalizeChapterIllustrationItem(item: JSONObject, fallbackIndex: Int): ChapterIllustration? {
        val src = normalizeAssetUrl(
            item.firstStringOrNull(
                "src",
                "url",
                "image_url",
                "imageUrl",
                "photo_url",
                "photoUrl",
                "file_url",
                "fileUrl",
                "path",
                "photo"
            )
        )
            ?: return null
        val originalSrc = normalizeAssetUrl(
            item.firstStringOrNull(
                "photo_true_url",
                "photoTrueUrl",
                "original_url",
                "originalUrl",
                "full_url",
                "fullUrl",
                "source_url",
                "sourceUrl"
            )
        )
        return ChapterIllustration(
            id = item.longOrNull("id") ?: item.longOrNull("image_id") ?: item.longOrNull("imageId") ?: (fallbackIndex + 1L),
            index = item.intOrNull("index")
                ?: item.intOrNull("order")
                ?: item.intOrNull("position")
                ?: item.intOrNull("display_order")
                ?: item.intOrNull("displayOrder")
                ?: item.intOrNull("sort_order")
                ?: item.intOrNull("sortOrder")
                ?: fallbackIndex + 1,
            src = src,
            originalSrc = originalSrc,
        )
    }

    private fun normalizeChapterIllustrationMutation(raw: Any): ChapterIllustrationMutationResult {
        val source = unwrapObject(raw, "data", "result")
        return ChapterIllustrationMutationResult(
            success = booleanFromAny(raw)
                ?: source.firstBooleanOrNull("success", "ok", "status")
                ?: true,
            imageCount = source.intOrNull("image_count") ?: source.intOrNull("imageCount"),
            message = source.firstStringOrNull("message", "msg", "detail"),
            errors = (source.opt("errors") as? JSONArray)?.toList()?.mapNotNull { it?.toString() }.orEmpty()
        )
    }

    private fun normalizeManagedBookTransfer(raw: Any): ManagedBookTransferResult {
        val source = unwrapObject(raw, "data", "result")
        val target = source.optJSONObject("target") ?: (raw as? JSONObject)?.optJSONObject("target")
        return ManagedBookTransferResult(
            success = booleanFromAny(raw)
                ?: source.firstBooleanOrNull("success", "ok", "status")
                ?: true,
            message = source.firstStringOrNull("message", "msg", "detail"),
            targetUsername = source.firstStringOrNull("target_username", "targetUsername", "username")
                ?: target?.firstStringOrNull("username", "name", "target_username", "targetUsername"),
            targetUserId = source.longOrNull("target_user_id")
                ?: source.longOrNull("targetUserId")
                ?: target?.longOrNull("id")
                ?: target?.longOrNull("user_id")
                ?: target?.longOrNull("userId")
        )
    }

    private fun normalizedThreshold(type: String, value: Int, label: String): Pair<String, Int> {
        val normalizedType = type.trim().ifBlank { "none" }
        require(normalizedType in setOf("none", "points_min", "points_pay")) { "$label threshold type is invalid" }
        if (normalizedType == "none") return "none" to 0
        require(value > 0) { "$label threshold value must be positive" }
        val maxValue = if (normalizedType == "points_pay") 50 else 100
        require(value <= maxValue) { "$label threshold value exceeds website limit" }
        return normalizedType to value
    }

    private fun normalizeForumCreateResult(raw: Any): ForumCreateResult {
        val source = unwrapObject(raw, "data", "result")
        val post = source.optJSONObject("post")
            ?: (raw as? JSONObject)?.optJSONObject("post")
        return ForumCreateResult(
            success = booleanFromAny(raw)
                ?: source.firstBooleanOrNull("success", "ok", "status")
                ?: true,
            message = source.firstStringOrNull("message", "msg", "detail"),
            postId = post?.longOrNull("id")
                ?: post?.longOrNull("post_id")
                ?: source.longOrNull("post_id")
                ?: source.longOrNull("postId")
        )
    }

    private fun normalizeForumCategory(value: String?): String {
        return when (value?.trim()?.lowercase()) {
            "announcement", "notice", "news", "activity" -> "公告"
            "recommend", "recommendation", "recommendations" -> "推书"
            "discussion", "post", "posts", "topic", "topics", "forum" -> "交流"
            "review", "reviews", "book_review", "book-reviews", "book_review_comment" -> "书评"
            "chapter", "chapters", "chapter_comment", "chapter-comments" -> "章节"
            "feedback", "suggestion", "bug", "bugs" -> "反馈"
            null, "" -> "交流"
            else -> value.trim()
        }
    }

    private fun plainSnippet(value: String?): String? {
        return value
            ?.replace(Regex("<[^>]+>"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun normalizeUser(raw: Any): UserProfile {
        val source = unwrapObject(raw, "user", "data", "profile")
        return UserProfile(
            id = source.longOrNull("id")
                ?: source.longOrNull("user_id")
                ?: source.longOrNull("userId")
                ?: source.longOrNull("uid"),
            name = source.stringOrNull("username")
                ?: source.stringOrNull("name")
                ?: source.stringOrNull("display_name")
                ?: source.stringOrNull("displayName")
                ?: source.stringOrNull("nickname")
                ?: source.stringOrNull("nick_name")
                ?: "Logged user",
            role = source.stringOrNull("role")
                ?: source.stringOrNull("user_role")
                ?: source.stringOrNull("userRole"),
            points = source.longOrNull("point") ?: source.longOrNull("points"),
            createdAt = source.firstStringOrNull("created_at", "createdAt"),
            avatarUrl = normalizeAssetUrl(source.firstStringOrNull("avatar", "avatar_url", "avatarUrl")),
            avatarFrameUrl = normalizeAssetUrl(
                source.firstStringOrNull("avatar_frame", "avatar_frame_url", "avatarFrameUrl")
            ),
            bio = source.firstStringOrNull("bio", "description"),
            email = source.stringOrNull("email"),
            isBanned = source.firstBooleanOrNull("is_banned", "isBanned"),
            banReason = source.firstStringOrNull("ban_reason", "banReason"),
            banExpiresAt = source.firstStringOrNull("ban_expires_at", "banExpiresAt"),
            isAdult = source.firstBooleanOrNull("is_adult", "isAdult"),
            deleted = source.firstBooleanOrNull("deleted", "is_deleted", "isDeleted"),
            badges = normalizeUserBadges(source.opt("badges")),
            stats = normalizeUserStats(source.opt("stats")),
            showCheckin = source.firstBooleanOrNull("show_checkin", "showCheckin"),
            autoCheckin = source.firstBooleanOrNull("auto_checkin", "autoCheckin")
        )
    }

    private fun normalizeUserActivities(raw: Any): List<UserActivity> {
        val values = extractArray(
            raw,
            "activities",
            "items",
            "data",
            "results",
            "posts",
            "comments",
            "reviews",
            "records",
            "list",
        ).ifEmpty {
            (raw as? JSONObject)
                ?.optJSONObject("data")
                ?.let {
                    nested -> extractArray(
                        nested,
                        "activities",
                        "items",
                        "results",
                        "data",
                        "posts",
                        "comments",
                        "reviews",
                        "records",
                        "list",
                    )
                }
                .orEmpty()
        }
        return values.mapNotNull { item ->
            val source = item as? JSONObject ?: return@mapNotNull null
            val comment = source.optJSONObject("comment")
            val post = source.optJSONObject("post")
            val book = source.optJSONObject("book")
            val chapter = source.optJSONObject("chapter")
            val chapterBook = chapter?.optJSONObject("book")
            val rawType = source.firstStringOrNull("type", "activity_type", "activityType") ?: "post"
            val type = when (rawType.trim().lowercase()) {
                "post_comment", "comment", "forum_comment" -> "post_comment"
                "novel_comment", "book_review", "book-review", "review" -> "novel_comment"
                "chapter_comment", "chapter-review", "chapter_review" -> "chapter_comment"
                "post", "announcement", "discussion", "topic" -> "post"
                else -> rawType.ifBlank { "post" }
            }
            val title = when (type) {
                "novel_comment" -> book?.firstStringOrNull("title", "name")
                "chapter_comment" -> chapterBook?.firstStringOrNull("title", "name")
                "post_comment", "post" -> post?.firstStringOrNull("title", "subject", "name")
                else -> source.firstStringOrNull("title", "name")
            } ?: source.firstStringOrNull("title", "subject", "name") ?: "动态"
            val content = when (type) {
                "novel_comment", "chapter_comment", "post_comment" ->
                    comment?.firstStringOrNull("content", "body", "text")
                "post" -> post?.firstStringOrNull("content", "body", "text", "excerpt")
                else -> source.firstStringOrNull("content", "body", "text", "excerpt")
            } ?: source.firstStringOrNull("content", "body", "text", "excerpt")
            UserActivity(
                id = source.longOrNull("id") ?: comment?.longOrNull("id") ?: post?.longOrNull("id") ?: 0L,
                type = type,
                title = title,
                content = plainSnippet(content),
                createdAt = source.firstStringOrNull("created_at", "createdAt", "updated_at", "updatedAt"),
                postId = post?.longOrNull("id")
                    ?: source.firstLongOrNull("post_id", "postId")
                    ?: source.longOrNull("id")?.takeIf { type == "post" },
                bookId = book?.longOrNull("id")
                    ?: chapterBook?.longOrNull("id")
                    ?: source.firstLongOrNull("book_id", "bookId")
                    ?: source.firstLongOrNull("novel_id", "novelId"),
                chapterId = chapter?.longOrNull("id")
                    ?: source.firstLongOrNull("chapter_id", "chapterId"),
                commentId = comment?.longOrNull("id")
                    ?: source.firstLongOrNull("comment_id", "commentId")
                    ?: source.longOrNull("id")?.takeIf { type != "post" },
                coverUrl = normalizeAssetUrl(
                    (book ?: chapterBook)?.firstStringOrNull("cover", "cover_url", "photo_url", "photo")
                        ?: source.firstStringOrNull(
                            "book_cover",
                            "bookCover",
                            "cover",
                            "cover_url",
                            "coverUrl",
                        )
                )
            )
        }
    }

    /** Normalizes the source ActivityTab envelope while preserving optional aggregate counters. */
    private fun normalizeCanonicalUserActivityFeed(raw: Any): UserContentActivityFeed {
        val source = raw as? JSONObject
        val nested = source?.optJSONObject("data")
        val counts = source?.optJSONObject("counts") ?: nested?.optJSONObject("counts")
        fun firstCount(vararg keys: String): Long? = keys.asSequence()
            .mapNotNull { key ->
                source?.longOrNull(key)
                    ?: nested?.longOrNull(key)
                    ?: counts?.longOrNull(key)
            }
            .firstOrNull()

        return UserContentActivityFeed(
            activities = normalizeUserActivities(raw),
            postCount = firstCount("post_count", "posts_count", "postCount", "posts"),
            forumCommentCount = firstCount(
                "forum_comment_count",
                "forumCommentCount",
                "post_comment_count",
                "postCommentCount",
            ),
            bookReviewCount = firstCount(
                "book_review_count",
                "bookReviewCount",
                "novel_comment_count",
                "novelCommentCount",
            ),
        )
    }

    private inline fun captureUserActivityFeed(
        block: () -> UserContentActivityFeed
    ): Result<UserContentActivityFeed> = try {
        Result.success(block())
    } catch (failure: Throwable) {
        // Cancellation must retain normal coroutine semantics; network/endpoint failures are
        // intentionally isolated so the other source feeds can still populate the timeline.
        if (failure is CancellationException) throw failure
        Result.failure(failure)
    }

    private fun normalizeUserPostActivityFeed(raw: Any): UserContentActivityFeed =
        UserContentActivityFeed(
            activities = extractArray(raw, "posts", "items", "records", "list", "data")
            .mapNotNull { value ->
                val post = value as? JSONObject ?: return@mapNotNull null
                val normalized = normalizeForumPost(post)
                normalized.id.takeIf { it > 0 }?.let { id ->
                    UserActivity(
                        id = id,
                        type = "post",
                        title = normalized.title,
                        content = normalized.excerpt,
                        createdAt = normalized.createdAt ?: normalized.lastActiveLabel,
                        postId = id
                    )
                }
            },
            postCount = userFeedTotal(raw)
        )

    private fun normalizeUserPostCommentActivityFeed(raw: Any): UserContentActivityFeed =
        UserContentActivityFeed(
            activities = extractArray(raw, "comments", "items", "records", "list", "data")
            .flatMap { value ->
                val comment = value as? JSONObject ?: return@flatMap emptyList()
                flattenUserPostCommentActivities(comment)
            },
            forumCommentCount = userFeedTotal(raw)
        )

    private fun flattenUserPostCommentActivities(
        source: JSONObject,
        fallbackPostId: Long? = null,
        fallbackTitle: String? = null
    ): List<UserActivity> {
        val post = source.optJSONObject("post") ?: source.optJSONObject("topic")
        val postId = source.longOrNull("post_id")
            ?: source.longOrNull("postId")
            ?: post?.longOrNull("id")
            ?: fallbackPostId
        val postTitle = post?.firstStringOrNull("title", "subject", "name")
            ?: source.firstStringOrNull("post_title", "postTitle", "topic_title", "topicTitle")
            ?: fallbackTitle
            ?: "论坛回复"
        val commentId = source.longOrNull("id")
            ?: source.longOrNull("comment_id")
            ?: source.longOrNull("commentId")
        val current = commentId?.let { id ->
            UserActivity(
                id = id,
                type = "post_comment",
                title = postTitle,
                content = plainSnippet(
                    source.firstStringOrNull(
                        "content_html",
                        "contentHtml",
                        "body_html",
                        "bodyHtml",
                        "content",
                        "body",
                        "text"
                    )
                ),
                createdAt = source.firstStringOrNull("created_at", "createdAt", "updated_at", "updatedAt"),
                postId = postId,
                commentId = id
            )
        }
        val replies = extractArray(source, "replies", "children", "reply_list", "replyList")
            .flatMap { child ->
                (child as? JSONObject)?.let {
                    flattenUserPostCommentActivities(it, postId, postTitle)
                }.orEmpty()
            }
        return listOfNotNull(current) + replies
    }

    private fun normalizeUserBookReviewActivityFeed(raw: Any): UserContentActivityFeed =
        UserContentActivityFeed(
            activities = extractArray(raw, "posts", "reviews", "comments", "items", "records", "list", "data")
            .mapNotNull { value ->
                val review = value as? JSONObject ?: return@mapNotNull null
                val normalized = normalizeForumPost(review, forceBookReview = true)
                val id = normalized.id.takeIf { it > 0 } ?: return@mapNotNull null
                UserActivity(
                    id = id,
                    type = "novel_comment",
                    title = normalized.bookTitle ?: normalized.title,
                    content = normalized.excerpt,
                    createdAt = normalized.createdAt ?: normalized.lastActiveLabel,
                    bookId = normalized.bookId,
                    commentId = id,
                    coverUrl = normalized.bookCoverUrl
                )
            },
            bookReviewCount = userFeedTotal(raw)
        )

    private fun userFeedTotal(raw: Any): Long? {
        val source = raw as? JSONObject ?: return null
        val nestedData = source.optJSONObject("data")
        val pagination = source.optJSONObject("pagination")
            ?: source.optJSONObject("page")
            ?: source.optJSONObject("meta")
            ?: nestedData?.optJSONObject("pagination")
        return pagination?.longOrNull("total")
            ?: pagination?.longOrNull("count")
            ?: source.longOrNull("total")
            ?: nestedData?.longOrNull("total")
    }

    private fun mergeUserContentActivities(
        activities: List<UserActivity>,
        limit: Int
    ): List<UserActivity> = activities
        .filter { activity -> activity.id > 0 }
        .distinctBy { activity -> "${activity.type}:${activity.id}" }
        .sortedWith(
            compareByDescending<UserActivity> { activityTimestampSortKey(it.createdAt) }
                .thenByDescending { it.id }
        )
        .take(limit)

    /** ISO and source SQL timestamps preserve newest-first lexical order after this normalization. */
    private fun activityTimestampSortKey(value: String?): String =
        value.orEmpty()
            .trim()
            .replace('T', ' ')
            .removeSuffix("Z")
            .take(19)
            .padEnd(19, '0')

    private fun normalizeUserCheckinRecords(raw: Any): List<UserCheckinRecord> {
        val source = unwrapObject(raw, "records", "checkins", "data", "result")
        val result = mutableListOf<UserCheckinRecord>()
        val keys = source.keys()
        while (keys.hasNext()) {
            val date = keys.next()
            val value = source.opt(date)
            val points = when (value) {
                is JSONObject -> value.longOrNull("points") ?: value.longOrNull("point") ?: 0L
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
            if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                result += UserCheckinRecord(date = date, points = points)
            }
        }
        return result.sortedBy { it.date }
    }

    private fun normalizeUserInventory(raw: Any): UserInventory {
        val source = unwrapObject(raw, "inventory", "data", "result")
        val equippedIds = linkedSetOf<Long>()
        inventoryItemIds(source.opt("equipped_items")).forEach(equippedIds::add)
        inventoryItemIds(source.opt("equippedItems")).forEach(equippedIds::add)
        inventoryItemIds(source.opt("equipment")).forEach(equippedIds::add)

        val itemValues = extractArray(source, "items", "inventory", "records", "data", "result")
            .ifEmpty { extractArray(raw, "items", "inventory", "records", "data", "result") }
        val items = itemValues
            .mapNotNull { value ->
                val item = value as? JSONObject ?: return@mapNotNull null
                val sourceItem = item.optJSONObject("item")
                    ?: item.optJSONObject("shop_item")
                    ?: item.optJSONObject("shopItem")
                    ?: item.optJSONObject("asset")
                    ?: item.optJSONObject("cosmetic")
                fun firstString(vararg keys: String): String? =
                    item.firstStringOrNull(*keys) ?: sourceItem?.firstStringOrNull(*keys)
                val inventoryId = item.longOrNull("inventory_id")
                    ?: item.longOrNull("inventoryId")
                    ?: item.longOrNull("id")
                val itemId = item.longOrNull("item_id")
                    ?: item.longOrNull("itemId")
                    ?: sourceItem?.longOrNull("id")
                    ?: sourceItem?.longOrNull("item_id")
                    ?: sourceItem?.longOrNull("itemId")
                val id = itemId
                    ?: inventoryId
                    ?: return@mapNotNull null
                val name = firstString("name", "item_name", "itemName", "title", "label")
                    ?: return@mapNotNull null
                UserInventoryItem(
                    id = id,
                    name = name,
                    inventoryId = inventoryId ?: id,
                    itemId = itemId ?: id,
                    type = firstString("type", "item_type", "itemType", "category"),
                    description = firstString("description", "desc", "content"),
                    quantity = (item.intOrNull("quantity")
                        ?: item.intOrNull("count")
                        ?: item.intOrNull("amount")
                        ?: 1).coerceAtLeast(0),
                    imageUrl = normalizeAssetUrl(
                        firstString(
                            "image_url",
                            "imageUrl",
                            "icon_url",
                            "iconUrl",
                            "photo_url",
                            "photoUrl",
                            "cover_url",
                            "coverUrl"
                        )
                    ),
                    badgeHtml = firstString("badge_html", "badgeHtml"),
                    badgeCss = firstString("badge_css", "badgeCss"),
                    slot = firstString("slot", "equipment_slot", "equipmentSlot", "position"),
                    equipped = item.firstBooleanOrNull("equipped", "is_equipped", "isEquipped") == true ||
                        sourceItem?.firstBooleanOrNull("equipped", "is_equipped", "isEquipped") == true ||
                        id in equippedIds ||
                        (inventoryId != null && inventoryId in equippedIds),
                    expiresAt = firstString("expires_at", "expiresAt", "expired_at", "expiredAt")
                )
            }
            .distinctBy(UserInventoryItem::inventoryId)

        return UserInventory(
            items = items,
            equippedItemIds = equippedIds + items.filter(UserInventoryItem::equipped).flatMap { item ->
                listOf(item.inventoryId, item.itemId, item.id)
            }
        )
    }

    private fun normalizeShopItems(raw: Any): List<ShopItem> =
        extractArray(raw, "items", "data", "list", "results")
            .mapNotNull { value ->
                val item = value as? JSONObject ?: return@mapNotNull null
                val source = item.optJSONObject("item") ?: item
                val id = source.longOrNull("id")
                    ?: source.longOrNull("item_id")
                    ?: item.longOrNull("item_id")
                    ?: return@mapNotNull null
                val name = source.firstStringOrNull("name", "item_name", "itemName", "title")
                    ?: item.firstStringOrNull("name", "item_name", "itemName", "title")
                    ?: return@mapNotNull null
                ShopItem(
                    id = id,
                    name = name,
                    description = source.firstStringOrNull("description", "desc", "content")
                        ?: item.firstStringOrNull("description", "desc", "content"),
                    price = source.longOrNull("price")
                        ?: source.longOrNull("points")
                        ?: item.longOrNull("price")
                        ?: item.longOrNull("points")
                        ?: 0L,
                    type = source.firstStringOrNull("type", "item_type", "itemType", "category")
                        ?: item.firstStringOrNull("type", "item_type", "itemType", "category")
                        ?: "frame",
                    imageUrl = normalizeAssetUrl(
                        source.firstStringOrNull("image_url", "imageUrl", "icon_url", "iconUrl")
                            ?: item.firstStringOrNull("image_url", "imageUrl", "icon_url", "iconUrl")
                    ),
                    badgeHtml = source.firstStringOrNull("badge_html", "badgeHtml")
                        ?: item.firstStringOrNull("badge_html", "badgeHtml"),
                    badgeCss = source.firstStringOrNull("badge_css", "badgeCss")
                        ?: item.firstStringOrNull("badge_css", "badgeCss")
                )
            }
            .distinctBy(ShopItem::id)

    private fun normalizeShopPurchase(raw: Any): ShopPurchaseResult {
        val source = unwrapObject(raw, "data", "result")
        return ShopPurchaseResult(
            success = booleanFromAny(raw)
                ?: source.firstBooleanOrNull("success", "ok", "status")
                ?: true,
            message = source.firstStringOrNull("message", "msg", "detail")
        )
    }

    private fun inventoryItemIds(value: Any?): Set<Long> = when (value) {
        is JSONArray -> value.toList().flatMapTo(linkedSetOf()) { inventoryItemIds(it) }
        is JSONObject -> buildSet {
            value.longOrNull("id")?.let(::add)
            value.longOrNull("item_id")?.let(::add)
            value.longOrNull("itemId")?.let(::add)
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key !in setOf("id", "item_id", "itemId")) inventoryItemIds(value.opt(key)).forEach(::add)
            }
        }
        is Number -> setOf(value.toLong())
        is String -> value.toLongOrNull()?.let(::setOf).orEmpty()
        else -> emptySet()
    }

    private fun normalizeUserQuizRewardStatus(raw: Any): UserQuizRewardStatus {
        val source = unwrapObject(raw, "quiz_reward", "quizReward", "status", "data", "result")
        return UserQuizRewardStatus(
            claimed = source.firstBooleanOrNull("claimed", "is_claimed", "isClaimed", "completed", "is_completed"),
            eligible = source.firstBooleanOrNull("eligible", "is_eligible", "isEligible", "available", "can_claim"),
            rewardName = source.firstStringOrNull("reward_name", "rewardName", "name", "title"),
            message = source.firstStringOrNull("message", "msg", "detail", "description"),
            questionCount = source.intOrNull("question_count")
                ?: source.intOrNull("questionCount")
                ?: source.intOrNull("questions_count")
        )
    }

    private fun normalizeUserBadges(raw: Any?): List<UserBadge> {
        val values = when (raw) {
            is JSONArray -> raw.toList()
            is Collection<*> -> raw.toList()
            is JSONObject -> extractArray(raw, "badges", "items", "data").ifEmpty { listOf(raw) }
            else -> emptyList()
        }
        return values.mapNotNull { value ->
            when (value) {
                is JSONObject -> {
                    val source = value.optJSONObject("badge") ?: value
                    val name = source.firstStringOrNull("name", "title", "label", "code")
                        ?: value.firstStringOrNull("name", "title", "label", "code")
                        ?: return@mapNotNull null
                    UserBadge(
                        id = source.longOrNull("id")
                            ?: source.longOrNull("badge_id")
                            ?: value.longOrNull("badge_id"),
                        name = name,
                        description = source.firstStringOrNull("description", "desc", "content"),
                        imageUrl = normalizeAssetUrl(
                            source.firstStringOrNull("image_url", "imageUrl", "icon_url", "iconUrl")
                        ),
                        badgeHtml = source.firstStringOrNull("badge_html", "badgeHtml"),
                        badgeCss = source.firstStringOrNull("badge_css", "badgeCss"),
                    )
                }
                null, JSONObject.NULL -> null
                else -> value.toString().trim()
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?.let { name -> UserBadge(name = name) }
            }
        }.distinctBy { badge -> badge.id?.let { "id:$it" } ?: "name:${badge.name}" }
    }

    private fun normalizeUserStats(raw: Any?): Map<String, Long> {
        val source = raw as? JSONObject ?: return emptyMap()
        val result = linkedMapOf<String, Long>()
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            source.longOrNull(key)?.let { result[key] = it }
        }
        return result
    }

    private fun extractArray(raw: Any, vararg preferredKeys: String): List<Any?> {
        if (raw is JSONArray) return raw.toList()
        val source = raw as? JSONObject ?: return emptyList()
        val keysToCheck = if (preferredKeys.isEmpty()) {
            arrayOf("items", "data", "results", "novels", "favorites", "books", "list", "records")
        } else {
            preferredKeys
        }

        for (key in keysToCheck) {
            val direct = source.opt(key)
            if (direct is JSONArray) return direct.toList()
            if (direct is JSONObject) {
                val nested = extractArray(direct, "items", "chapters", "groups", "favorite_groups", "favoriteGroups", "results", "novels", "favorites", "books", "posts", "reviews", "comments", "list", "records", "data")
                if (nested.isNotEmpty()) return nested
            }
        }

        val data = source.opt("data")
        if (data is JSONArray) return data.toList()
        if (data is JSONObject) {
            val nested = extractArray(data, "items", "chapters", "groups", "favorite_groups", "favoriteGroups", "results", "novels", "favorites", "books", "posts", "reviews", "comments", "list", "records", "data")
            if (nested.isNotEmpty()) return nested
        }

        val numericValues = mutableListOf<Any?>()
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.toLongOrNull() != null) numericValues.add(source.opt(key))
        }
        return numericValues
    }

    private fun unwrapObject(raw: Any, vararg keys: String): JSONObject {
        if (raw is JSONObject) {
            for (key in keys) {
                if (key == "data" || key == "result") continue
                val value = raw.opt(key)
                if (value is JSONObject) return value
            }
            for (envelope in arrayOf("data", "result")) {
                val nested = raw.opt(envelope)
                if (nested is JSONObject && nested !== raw) {
                    val unwrapped = unwrapObject(nested, *keys)
                    if (unwrapped.length() > 0) return unwrapped
                }
            }
            for (key in keys) {
                val value = raw.opt(key)
                if (value is JSONObject) return value
            }
            return raw
        }
        return JSONObject()
    }

    private fun JSONArray.toList(): List<Any?> = (0 until length()).map { opt(it) }

    private fun JSONObject.toStringMap(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val iterator = keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            opt(key)?.takeUnless { it == JSONObject.NULL }?.let { value ->
                result[key] = value.toString()
            }
        }
        return result
    }

    private fun JSONObject.arrayOrNull(key: String): List<Any?>? {
        val array = optJSONArray(key) ?: return null
        return array.toList()
    }

    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return opt(key)?.toString()?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun JSONObject.objectStringOrNull(key: String, vararg nestedKeys: String): String? {
        val nested = opt(key) as? JSONObject ?: return null
        return nested.firstStringOrNull(*nestedKeys)
    }

    private fun JSONObject.firstStringOrNull(vararg keys: String): String? {
        for (key in keys) {
            val value = stringOrNull(key)
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun normalizeBookTags(source: JSONObject): List<String> {
        val ordered = linkedSetOf<String>()
        source.firstStringOrNull(
            "novel_type",
            "novelType",
            "category",
            "category_name",
            "categoryName",
            "type",
            "type_name",
            "typeName"
        )?.let { ordered.add(it) }
        source.firstStringOrNull(
            "genre",
            "genre_name",
            "genreName"
        )?.let { ordered.add(it) }
        source.firstStringOrNull("spans", "span", "badges", "badge")?.let {
            ordered.addAll(splitTagString(it, splitWhitespace = true))
        }

        val tagValue = source.opt("tags")
        when (tagValue) {
            is JSONArray -> ordered.addAll(normalizeTags(tagValue.toList()))
            is String -> ordered.addAll(splitTagString(tagValue))
        }

        val tagAliases = arrayOf(
            "tag",
            "tag_names",
            "tagNames",
            "keywords",
            "labels",
            "categories",
            "category_list",
            "categoryList",
            "genres",
            "genre_list",
            "genreList",
            "book_tags",
            "bookTags",
            "novel_tags",
            "novelTags",
            "tag_list",
            "tagList",
            "tag_relations",
            "tagRelations",
            "novel_tag_relations",
            "novelTagRelations",
            "taggings"
        )
        for (key in tagAliases) {
            when (val value = source.opt(key)) {
                is JSONArray -> ordered.addAll(normalizeTags(value.toList()))
                is String -> ordered.addAll(splitTagString(value))
            }
        }

        return ordered.toList()
    }

    private fun normalizeEditableBookTags(source: JSONObject): List<String> {
        val ordered = linkedSetOf<String>()
        val tagAliases = arrayOf(
            "tags",
            "tag",
            "tag_names",
            "tagNames",
            "keywords",
            "labels",
            "categories",
            "category_list",
            "categoryList",
            "genres",
            "genre_list",
            "genreList",
            "book_tags",
            "bookTags",
            "novel_tags",
            "novelTags",
            "tag_list",
            "tagList",
            "tag_relations",
            "tagRelations",
            "novel_tag_relations",
            "novelTagRelations",
            "taggings"
        )
        for (key in tagAliases) {
            when (val value = source.opt(key)) {
                is JSONArray -> ordered.addAll(normalizeTags(value.toList()))
                is String -> ordered.addAll(splitTagString(value))
            }
        }
        return ordered.toList()
    }

    private fun normalizeBookStatus(source: JSONObject): String? {
        source.firstStringOrNull(
            "status",
            "state",
            "book_status",
            "bookStatus",
            "novel_status",
            "novelStatus",
            "completion_status",
            "completionStatus",
            "serial_status",
            "serialStatus"
        )?.let { return it }

        val completed = source.firstBooleanOrNull(
            "is_completed",
            "isCompleted",
            "completed",
            "is_finished",
            "isFinished",
            "finished"
        ) ?: return null

        return if (completed) "已完结" else "连载中"
    }

    private fun normalizeAuthSession(raw: Any): AuthSession {
        val source = unwrapObject(raw, "data", "result", "session")
        val topLevel = raw as? JSONObject
        val success = source.firstBooleanOrNull("success", "ok")
            ?: topLevel?.firstBooleanOrNull("success", "ok")
        if (success == false) {
            throw IOException(
                source.firstStringOrNull("message", "error", "detail")
                    ?: topLevel?.firstStringOrNull("message", "error", "detail")
                    ?: "认证失败"
            )
        }
        val token = source.firstStringOrNull("token", "auth_token", "authToken", "access_token", "accessToken")
            ?: topLevel?.firstStringOrNull("token", "auth_token", "authToken", "access_token", "accessToken")
            ?: throw IOException("认证响应未返回会话令牌")
        val user = source.optJSONObject("user")
            ?: source.optJSONObject("profile")
            ?: topLevel?.optJSONObject("user")
            ?: topLevel?.optJSONObject("profile")
        return AuthSession(
            token = token,
            user = user?.let(::normalizeUser),
            message = source.firstStringOrNull("message", "detail")
                ?: topLevel?.firstStringOrNull("message", "detail")
        )
    }

    private fun normalizeAuthAction(raw: Any): AuthActionResult {
        val source = unwrapObject(raw, "data", "result")
        val topLevel = raw as? JSONObject
        val success = source.firstBooleanOrNull("success", "ok")
            ?: topLevel?.firstBooleanOrNull("success", "ok")
            ?: true
        return AuthActionResult(
            success = success,
            message = source.firstStringOrNull("message", "detail", "error")
                ?: topLevel?.firstStringOrNull("message", "detail", "error")
        )
    }

    private fun normalizeTags(values: List<Any?>): List<String> =
        values.mapNotNull { value ->
            when (value) {
                is JSONObject -> value.firstStringOrNull("name", "title", "label", "tag_name", "tagName", "value", "text")
                    ?: value.optJSONObject("tag")?.firstStringOrNull("name", "title", "label", "tag_name", "tagName", "value", "text")
                    ?: value.optJSONObject("novel_tag")?.firstStringOrNull("name", "title", "label", "tag_name", "tagName", "value", "text")
                    ?: value.optJSONObject("novelTag")?.firstStringOrNull("name", "title", "label", "tag_name", "tagName", "value", "text")
                null -> null
                else -> value.toString().takeIf { it.isNotBlank() && it != "null" }
            }
        }

    private fun splitTagString(value: String, splitWhitespace: Boolean = false): List<String> {
        val separators = if (splitWhitespace) {
            charArrayOf(',', ';', '/', '|', '，', '、', ' ', '\t', '\n', '\r')
        } else {
            charArrayOf(',', ';', '/', '|', '，', '、')
        }
        return value.split(*separators)
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "null" }
    }

    private fun JSONObject.firstBooleanOrNull(vararg keys: String): Boolean? {
        for (key in keys) {
            val value = booleanOrNull(key)
            if (value != null) return value
        }
        return null
    }

    private fun booleanFromAny(value: Any?): Boolean? {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> statusTextToBoolean(value)
            is JSONObject -> value.firstBooleanOrNull(
                "data",
                "result",
                "is_favorited",
                "isFavorited",
                "is_favorite",
                "isFavorite",
                "favorited",
                "favorite",
                "exists",
                "status"
            )
            else -> null
        }
    }

    private fun JSONObject.booleanOrNull(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> statusTextToBoolean(value)
            is JSONObject -> booleanFromAny(value)
            else -> null
        }
    }

    private fun JSONObject.doubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return opt(key)?.toString()?.toDoubleOrNull()
    }

    private fun statusTextToBoolean(value: String): Boolean? {
        return when (value.trim().lowercase()) {
            "true", "1", "yes", "y", "favorited", "favorite", "exists", "collected", "added" -> true
            "false", "0", "no", "n", "none", "not_favorited", "not-favorited", "missing", "removed" -> false
            else -> null
        }
    }

    private fun normalizeAssetUrl(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalized = when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("//") -> "https:$value"
            value.startsWith("/") -> baseUrl.trimEnd('/') + value
            else -> baseUrl.trimEnd('/') + "/" + value
        }
        return normalized.takeUnless(::isBareImageHost)
    }

    private fun isBareImageHost(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        return url.host.equals("images.novelpia.com", ignoreCase = true) &&
            url.encodedPath.trim('/').isBlank()
    }

    private fun JSONObject.longOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun JSONObject.firstLongOrNull(vararg keys: String): Long? =
        keys.firstNotNullOfOrNull { key -> longOrNull(key) }

    private fun JSONObject.firstIntOrNull(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key -> intOrNull(key) }

    private class ReaderContentUnavailableException(message: String) : IOException(message)

    private fun JSONObject.intOrNull(key: String): Int? = longOrNull(key)?.toInt()

    companion object {
        const val WEBSITE_UPLOAD_CHUNK_BYTES = 5 * 1024 * 1024
        const val WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES = 20 * 1024 * 1024
        private const val READER_CONTENT_MAX_ATTEMPTS = 2
        private const val READER_CONTENT_RETRY_DELAY_MS = 500L
        private const val READER_CONTENT_CALL_TIMEOUT_SECONDS = 45L
        private const val READER_CONTENT_READ_TIMEOUT_SECONDS = 45L
        private const val EPUB_DOWNLOAD_CALL_TIMEOUT_SECONDS = 15 * 60L
        private const val EPUB_DOWNLOAD_READ_TIMEOUT_SECONDS = 2 * 60L
        private const val ASSET_CALL_TIMEOUT_SECONDS = 90L
        private const val ASSET_READ_TIMEOUT_SECONDS = 90L
        private const val READER_SESSION_FALLBACK_CACHE_MILLIS = 60_000L
        private const val READER_SESSION_MAX_CACHE_MILLIS = 5 * 60_000L
        private const val READER_SESSION_EXPIRY_SKEW_MILLIS = 5_000L
        private const val READER_SESSION_MIN_REUSE_MILLIS = 5_000L
        private val READER_CONTENT_RETRYABLE_STATUS_CODES =
            setOf(401, 403, 408, 425, 429, 500, 502, 503, 504)
        private const val USER_AGENT = "NovalPieNative/2.0 Android"
        private const val READER_SIGNATURE_SECRET =
            "X9f2m8Q5zL1p4R7t0Y3u6W2s5V8x1B4n7M0k3J6h9G2d5F8c1A4b7E0r3T6y9U2i"
        private const val STANDARD_BASE64_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        private const val READER_BASE64_ALPHABET =
            "M9N8B7V6C5X4Z3L2K1J0HGFDSAPOIUYTREWQmnbvcxzlkjhgfdsaqwertyuiop+/"
        private const val READER_NONCE_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
        private val secureRandom = SecureRandom()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

private class UploadStreamRequestBody(
    private val source: UploadFileSource
) : RequestBody() {
    override fun contentType() = (source.contentType ?: "application/octet-stream").toMediaType()
    override fun contentLength(): Long = source.sizeBytes

    override fun writeTo(sink: BufferedSink) {
        source.openStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
            }
        }
    }
}

private class UploadRangeRequestBody(
    private val source: UploadFileSource,
    private val offset: Long,
    private val length: Long
) : RequestBody() {
    override fun contentType() = (source.contentType ?: "application/octet-stream").toMediaType()
    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink) {
        source.openStream().use { input ->
            var skipped = 0L
            while (skipped < offset) {
                val count = input.skip(offset - skipped)
                if (count > 0) {
                    skipped += count
                } else if (input.read() >= 0) {
                    skipped++
                } else {
                    throw IOException("文件长度小于分片偏移")
                }
            }
            var remaining = length
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (remaining > 0) {
                val read = input.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                if (read < 0) throw IOException("文件在分片读取时提前结束")
                sink.write(buffer, 0, read)
                remaining -= read
            }
        }
    }
}
