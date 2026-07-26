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
import com.novalpie.nativeapp.model.FavoriteGroup
import com.novalpie.nativeapp.model.FavoriteStatus
import com.novalpie.nativeapp.model.ForumActionResult
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumCreateRequest
import com.novalpie.nativeapp.model.ForumCreateResult
import com.novalpie.nativeapp.model.ForumPost
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
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.SiteMessage
import com.novalpie.nativeapp.model.ParsedEpub
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
import com.novalpie.nativeapp.model.UserCheckinAction
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserCheckinRecord
import com.novalpie.nativeapp.model.UserCheckinSettings
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
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class NovalPieApi(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://novalpie.cc",
    private val cookieProvider: () -> String? = { null },
    private val authTokenProvider: () -> String? = { null },
    private val proxyProvider: () -> Proxy? = { null },
    private val proxySelectorProvider: () -> ProxySelector? = { null }
) {
    suspend fun search(
        keyword: String,
        page: Int = 1,
        limit: Int = 20,
        sortBy: String = "relevance",
        sortOrder: String = "desc",
        scope: String = "all",
        matchType: String = "ai",
        adultFilter: String = "all",
        source: String = "",
        minWordCount: Long? = null,
        maxWordCount: Long? = null
    ): List<NovelCard> =
        withContext(Dispatchers.IO) {
            val params = mutableMapOf(
                "q" to keyword.trim(),
                "page" to page.toString(),
                "limit" to limit.toString(),
                "sort_by" to sortBy,
                "sort_order" to sortOrder,
                "scope" to scope,
                "match_type" to matchType,
                "adult_filter" to adultFilter,
                "source" to source
            )
            minWordCount?.takeIf { it > 0 }?.let { params["min_word_count"] = it.toString() }
            maxWordCount?.takeIf { it > 0 }?.let { params["max_word_count"] = it.toString() }
            val raw = get(
                "/api/search",
                params
            )
            normalizeNovelList(raw)
        }

    suspend fun currentUser(): UserProfile = withContext(Dispatchers.IO) {
        normalizeUser(get("/api/users/me"))
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
                    "limit" to limit.coerceIn(1, 100).toString()
                )
            )
        )
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
            AdminReviewRequest(
                id = source.longOrNull("id") ?: return@mapNotNull null,
                type = source.firstStringOrNull("type", "request_type") ?: "unknown",
                status = source.firstStringOrNull("status", "review_status") ?: "pending",
                username = source.firstStringOrNull("username", "user_name", "userName"),
                userId = source.longOrNull("user_id") ?: source.longOrNull("userId"),
                novelId = source.longOrNull("novel_id") ?: source.longOrNull("novelId"),
                title = source.firstStringOrNull("title", "novel_title", "name"),
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
                novelId = item.longOrNull("novel_id") ?: item.longOrNull("novelId"),
                message = item.firstStringOrNull("message", "detail", "error_message"),
                createdAt = item.firstStringOrNull("created_at", "createdAt")
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
                updatedAt = source.firstStringOrNull("updated_at", "updatedAt")
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
                description = source.stringOrNull("description")
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
                    "is_active" to (active?.toString() ?: ""),
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
            totalDays = source.intOrNull("total_days") ?: 0,
            totalPoints = source.longOrNull("total_points") ?: 0L,
            maxStreak = source.intOrNull("max_streak") ?: 0,
            currentStreak = source.intOrNull("current_streak") ?: 0
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

    suspend fun favoriteGroups(): List<FavoriteGroup> = withContext(Dispatchers.IO) {
        normalizeFavoriteGroups(get("/api/favorites/groups", mapOf("preview_limit" to "6", "with_preview" to "true")))
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

    suspend fun forumPosts(page: Int = 1, limit: Int = 20, type: String = "all"): List<ForumPost> = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        if (type.isNotBlank() && type != "all") params["type"] = type
        normalizeForumPosts(get("/api/posts", params))
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

    suspend fun forumPostComments(postId: Long, page: Int = 1, limit: Int = 20): List<ForumComment> = withContext(Dispatchers.IO) {
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

    suspend fun bookDetail(bookId: Long): NovelCard = withContext(Dispatchers.IO) {
        normalizeBook(get("/api/novels/$bookId/detail"))
    }

    suspend fun bookCoverPhoto(bookId: Long, favoriteType: String = "novel"): String? =
        withContext(Dispatchers.IO) {
            val raw = get(
                "/api/novels/$bookId/photo",
                mapOf("favorite_type" to favoriteType)
            )
            val source = unwrapObject(raw, "photo", "novel", "data", "result")
            normalizeAssetUrl(
                source.firstStringOrNull(
                    "photo_true_url",
                    "photoTrueUrl",
                    "full_cover_url",
                    "fullCoverUrl",
                    "original_cover_url",
                    "originalCoverUrl",
                    "photo_url",
                    "photoUrl"
                )
            )
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
        normalizeChapters(get("/api/novels/$bookId/chapters"))
    }

    suspend fun chapterContent(chapterId: Long): ReaderContent = withContext(Dispatchers.IO) {
        val session = readerSessionKey()
        normalizeReaderContent(
            raw = get(
                "/api/chapters/$chapterId/content",
                mapOf(
                    "session" to session.sessionId,
                    "replace_mode" to "india",
                    "show_images" to "1"
                )
            ),
            readerSessionKey = session.sessionKey
        )
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

    suspend fun bookComments(bookId: Long, page: Int = 1, limit: Int = 20): List<ChapterComment> = withContext(Dispatchers.IO) {
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
        headers: Map<String, String> = emptyMap()
    ): Any {
        return request(path = path, params = params, method = "GET", headers = headers)
    }

    private fun post(path: String, body: JSONObject): Any {
        return request(path = path, method = "POST", body = body)
    }

    private fun put(path: String, body: JSONObject): Any {
        return request(path = path, method = "PUT", body = body)
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
        headers: Map<String, String> = emptyMap()
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

        return execute(path, requestBuilder)
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

    private fun execute(path: String, requestBuilder: Request.Builder): Any {
        cookieProvider()?.takeIf { it.isNotBlank() }?.let { cookie ->
            requestBuilder.header("cookie", cookie)
        }
        authTokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
            requestBuilder.header("authorization", "Bearer $token")
        }

        val request = requestBuilder.build()

        val explicitProxy = proxyProvider()
        val proxySelector = if (explicitProxy == null) proxySelectorProvider() else null
        val callClient = when {
            explicitProxy != null -> client.newBuilder().proxy(explicitProxy).build()
            proxySelector != null -> client.newBuilder().proxySelector(proxySelector).build()
            else -> client
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

    private fun executeExternal(label: String, requestBuilder: Request.Builder): Any {
        val request = requestBuilder.build()
        val explicitProxy = proxyProvider()
        val proxySelector = if (explicitProxy == null) proxySelectorProvider() else null
        val callClient = when {
            explicitProxy != null -> client.newBuilder().proxy(explicitProxy).build()
            proxySelector != null -> client.newBuilder().proxySelector(proxySelector).build()
            else -> client
        }
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

    private fun normalizeNovelList(raw: Any): List<NovelCard> {
        return extractArray(raw).mapNotNull { value ->
            (value as? JSONObject)?.let(::normalizeBook)
        }
    }

    private fun normalizeBook(raw: Any): NovelCard {
        val source = unwrapObject(raw, "novel", "item", "data")
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
                ?: source.longOrNull("words"),
            favoriteCount = source.longOrNull("favorite_count")
                ?: source.longOrNull("favoriteCount")
                ?: source.longOrNull("favorites")
                ?: source.longOrNull("novel_like")
                ?: source.longOrNull("novelLike")
                ?: source.longOrNull("bookmark_count")
                ?: source.longOrNull("bookmarkCount")
                ?: source.longOrNull("collect_count")
                ?: source.longOrNull("collectCount"),
            siteReadCount = source.longOrNull("site_read_count")
                ?: source.longOrNull("siteReadCount")
                ?: source.longOrNull("novel_read")
                ?: source.longOrNull("novelRead")
                ?: source.longOrNull("read_count")
                ?: source.longOrNull("readCount")
                ?: source.longOrNull("view_count")
                ?: source.longOrNull("viewCount")
                ?: source.longOrNull("views"),
            sourceReadCount = source.longOrNull("source_read_count")
                ?: source.longOrNull("sourceReadCount")
                ?: source.longOrNull("original_read_count")
                ?: source.longOrNull("originalReadCount"),
            sourceFavoriteCount = source.longOrNull("source_favorite_count")
                ?: source.longOrNull("sourceFavoriteCount")
                ?: source.longOrNull("original_favorite_count")
                ?: source.longOrNull("originalFavoriteCount"),
            updatedAt = source.stringOrNull("updated_at")
                ?: source.stringOrNull("updateTime")
                ?: source.stringOrNull("created_at"),
            tags = normalizeBookTags(source)
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

    private data class ReaderSessionKey(val sessionId: String, val sessionKey: String)

    private fun readerSessionKey(): ReaderSessionKey {
        val raw = get("/api/reader/session-key", headers = readerSignatureHeaders())
        val source = unwrapObject(raw, "data", "result", "session")
        val sessionId = source.firstStringOrNull("session_id", "sessionId", "id")
            ?: throw IOException("Reader session id is empty.")
        val sessionKey = source.firstStringOrNull("session_key", "sessionKey", "key")
            ?: throw IOException("Reader session key is empty.")
        return ReaderSessionKey(sessionId = sessionId, sessionKey = sessionKey)
    }

    private fun normalizeReaderContent(raw: Any, readerSessionKey: String? = null): ReaderContent {
        if (raw is String && raw.isNotBlank()) {
            return ReaderContent(title = null, content = raw, source = "plain")
        }

        val source = unwrapObject(raw, "data", "result", "chapter")
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
                ?: throw IOException("Chapter content is empty.")
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
                "images",
                "chapter_images",
                "chapterImages",
                "image_list",
                "imageList"
            )
        )
        if (direct.isNotEmpty()) return direct

        return normalizeChapterIllustrationItems(
            extractArray(
                raw,
                "illustrations",
                "images",
                "chapter_images",
                "chapterImages",
                "image_list",
                "imageList"
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
            val source = value as? JSONObject ?: return@mapNotNull null
            FavoriteGroup(
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
                    ?: source.intOrNull("booksCount")
            )
        }
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
            WorkspaceApiConfig(
                id = id,
                name = source.firstStringOrNull("name", "api_name", "apiName") ?: "API #$id",
                model = source.firstStringOrNull("model", "model_name", "modelName").orEmpty(),
                endpoint = source.firstStringOrNull("endpoint", "base_url", "baseUrl").orEmpty(),
                apiKey = source.firstStringOrNull("key", "api_key", "apiKey"),
                concurrency = source.intOrNull("concurrency") ?: 10,
                isActive = source.firstBooleanOrNull("is_active", "isActive", "active") ?: true,
                isHealthy = source.firstBooleanOrNull("is_healthy", "isHealthy", "healthy"),
                approvalStatus = source.firstStringOrNull("approval_status", "approvalStatus"),
                totalRequests = source.longOrNull("totalRequests")
                    ?: source.longOrNull("total_requests")
                    ?: source.longOrNull("callCount")
                    ?: source.longOrNull("call_count")
                    ?: 0
            )
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

    private fun normalizeForumPosts(raw: Any): List<ForumPost> {
        return extractArray(raw, "posts", "items", "records", "list", "data").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            normalizeForumPost(source)
        }
    }

    private fun normalizeForumPost(raw: JSONObject): ForumPost {
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
        val title = source.firstStringOrNull("title", "subject", "name")
            ?: plainSnippet(source.firstStringOrNull("content", "body", "text", "summary", "excerpt"))?.take(32)
            ?: "站内讨论"
        val tags = linkedSetOf<String>()
        rawCategory?.takeIf { it.isNotBlank() }?.let { tags.add(normalizeForumCategory(it)) }
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
            authorId = source.optJSONObject("author")?.longOrNull("id")
                ?: source.optJSONObject("user")?.longOrNull("id")
                ?: source.longOrNull("user_id")
                ?: source.longOrNull("userId")
                ?: source.longOrNull("author_id")
                ?: source.longOrNull("authorId"),
            bookTitle = source.objectStringOrNull("novel", "title", "name", "novel_title")
                ?: source.objectStringOrNull("book", "title", "name", "book_title")
                ?: source.firstStringOrNull("novel_title", "novelTitle", "book_title", "bookTitle"),
            replyCount = source.intOrNull("reply_count")
                ?: source.intOrNull("replyCount")
                ?: source.intOrNull("comments_count")
                ?: source.intOrNull("comment_count")
                ?: source.intOrNull("commentCount")
                ?: source.intOrNull("replies"),
            likeCount = source.intOrNull("like_count")
                ?: source.intOrNull("likeCount")
                ?: source.intOrNull("likes"),
            reactionCount = source.intOrNull("reaction_count")
                ?: source.intOrNull("reactionCount")
                ?: source.intOrNull("reactions"),
            awardPoints = source.intOrNull("award_points")
                ?: source.intOrNull("awardPoints")
                ?: source.intOrNull("reward_points")
                ?: source.intOrNull("rewardPoints")
                ?: source.intOrNull("awards"),
            viewCount = source.intOrNull("view_count")
                ?: source.intOrNull("viewCount")
                ?: source.intOrNull("views")
                ?: source.intOrNull("read_count")
                ?: source.intOrNull("readCount"),
            lastActiveLabel = source.firstStringOrNull(
                "last_active_at",
                "lastActiveAt",
                "updated_at",
                "updatedAt",
                "created_at",
                "createdAt"
            ),
            excerpt = plainSnippet(source.firstStringOrNull("excerpt", "summary", "content", "body", "text")),
            tags = tags.toList().take(3),
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
        return extractArray(raw, "comments", "items", "records", "list", "data").mapNotNull { value ->
            val source = value as? JSONObject ?: return@mapNotNull null
            val id = source.longOrNull("id")
                ?: source.longOrNull("comment_id")
                ?: source.longOrNull("commentId")
                ?: return@mapNotNull null
            ForumComment(
                id = id,
                postId = source.longOrNull("post_id") ?: source.longOrNull("postId"),
                parentCommentId = source.longOrNull("parent_comment_id")
                    ?: source.longOrNull("parentCommentId")
                    ?: source.longOrNull("comment_id_parent"),
                authorName = source.objectStringOrNull("author", "name", "username", "display_name", "nickname")
                    ?: source.objectStringOrNull("user", "name", "username", "display_name", "nickname")
                    ?: source.firstStringOrNull("author_name", "authorName", "username", "nickname"),
                authorId = source.optJSONObject("author")?.longOrNull("id")
                    ?: source.optJSONObject("user")?.longOrNull("id")
                    ?: source.longOrNull("user_id")
                    ?: source.longOrNull("userId")
                    ?: source.longOrNull("author_id")
                    ?: source.longOrNull("authorId"),
                replyToName = source.firstStringOrNull("reply_to_name", "replyToName"),
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
                createdAt = source.firstStringOrNull("created_at", "createdAt", "updated_at", "updatedAt")
            )
        }
    }

    private fun normalizeChapterComments(raw: Any): List<ChapterComment> {
        val comments = mutableListOf<ChapterComment>()
        extractArray(raw, "comments", "items", "records", "list", "data").forEach { value ->
            val source = value as? JSONObject ?: return@forEach
            appendChapterCommentWithReplies(comments, source)
        }
        return comments
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
        return ChapterComment(
                id = id,
                bookId = source.longOrNull("book_id") ?: source.longOrNull("bookId") ?: fallbackBookId,
                chapterId = source.longOrNull("chapter_id") ?: source.longOrNull("chapterId") ?: fallbackChapterId,
                parentCommentId = source.longOrNull("parent_comment_id")
                    ?: source.longOrNull("parentCommentId")
                    ?: source.longOrNull("comment_id_parent")
                    ?: fallbackParentCommentId,
                authorName = source.objectStringOrNull("author", "name", "username", "display_name", "nickname")
                    ?: source.objectStringOrNull("user", "name", "username", "display_name", "nickname")
                    ?: source.firstStringOrNull("author_name", "authorName", "username", "nickname"),
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
        val src = normalizeAssetUrl(item.firstStringOrNull("src", "url", "photo_url", "photoUrl"))
            ?: return null
        return ChapterIllustration(
            id = item.longOrNull("id") ?: item.longOrNull("image_id") ?: item.longOrNull("imageId") ?: (fallbackIndex + 1L),
            index = item.intOrNull("index") ?: item.intOrNull("order") ?: fallbackIndex + 1,
            src = src
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
            "review", "reviews", "book_review", "book-reviews", "book_review_comment" -> "书评"
            "chapter", "chapters", "chapter_comment", "chapter-comments" -> "章节"
            "post", "posts", "topic", "topics", "discussion", "forum" -> "讨论"
            "notice", "announcement", "news", "activity" -> "动态"
            null, "" -> "动态"
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
        return extractArray(raw, "activities", "items", "data", "results").mapNotNull { item ->
            val source = item as? JSONObject ?: return@mapNotNull null
            val comment = source.optJSONObject("comment")
            val post = source.optJSONObject("post")
            val book = source.optJSONObject("book")
            val chapter = source.optJSONObject("chapter")
            val chapterBook = chapter?.optJSONObject("book")
            val type = source.firstStringOrNull("type", "activity_type", "activityType") ?: "post"
            val title = when (type) {
                "novel_comment" -> book?.firstStringOrNull("title", "name")
                "chapter_comment" -> chapterBook?.firstStringOrNull("title", "name")
                "post_comment", "post" -> post?.firstStringOrNull("title", "subject", "name")
                else -> source.firstStringOrNull("title", "name")
            } ?: "动态"
            val content = when (type) {
                "novel_comment", "chapter_comment", "post_comment" ->
                    comment?.firstStringOrNull("content", "body", "text")
                "post" -> post?.firstStringOrNull("content", "body", "text", "excerpt")
                else -> source.firstStringOrNull("content", "body", "text", "excerpt")
            }
            UserActivity(
                id = source.longOrNull("id") ?: comment?.longOrNull("id") ?: post?.longOrNull("id") ?: 0L,
                type = type,
                title = title,
                content = plainSnippet(content),
                createdAt = source.firstStringOrNull("created_at", "createdAt", "updated_at", "updatedAt"),
                postId = post?.longOrNull("id") ?: source.longOrNull("post_id"),
                bookId = book?.longOrNull("id")
                    ?: chapterBook?.longOrNull("id")
                    ?: source.longOrNull("book_id")
                    ?: source.longOrNull("novel_id"),
                chapterId = chapter?.longOrNull("id") ?: source.longOrNull("chapter_id"),
                commentId = comment?.longOrNull("id") ?: source.longOrNull("comment_id"),
                coverUrl = normalizeAssetUrl(
                    (book ?: chapterBook)?.firstStringOrNull("cover", "cover_url", "photo_url", "photo")
                )
            )
        }
    }

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

    private fun normalizeUserBadges(raw: Any?): List<String> {
        val values = when (raw) {
            is JSONArray -> raw.toList()
            is Collection<*> -> raw.toList()
            else -> emptyList()
        }
        return values.mapNotNull { value ->
            when (value) {
                is JSONObject -> value.firstStringOrNull("name", "title", "label", "code")
                null, JSONObject.NULL -> null
                else -> value.toString().trim().takeIf { it.isNotBlank() && it != "null" }
            }
        }.distinct()
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

    private fun JSONObject.intOrNull(key: String): Int? = longOrNull(key)?.toInt()

    companion object {
        const val WEBSITE_UPLOAD_CHUNK_BYTES = 5 * 1024 * 1024
        const val WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES = 20 * 1024 * 1024
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
