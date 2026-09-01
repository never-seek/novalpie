package com.novalpie.nativeapp.model

data class NovelCard(
    val id: Long,
    val title: String,
    val originalTitle: String? = null,
    val author: String? = null,
    val platform: String? = null,
    val status: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val wordCount: Long? = null,
    val favoriteCount: Long? = null,
    val siteReadCount: Long? = null,
    val recommendCount: Long? = null,
    val sourceReadCount: Long? = null,
    val sourceFavoriteCount: Long? = null,
    val updatedAt: String? = null,
    val tags: List<String> = emptyList(),
    val fullCoverUrl: String? = null,
    /** Detail-only source metadata; search/favourite payloads may omit these fields. */
    val createdAt: String? = null,
    val chapterCount: Int? = null,
    val maxChapterNumber: Int? = null,
    val guarantorId: Long? = null,
    val guarantorName: String? = null,
    val guaranteedAt: String? = null,
    val uploaderName: String? = null,
    val isAdult: Boolean? = null,
    val allowDownload: Boolean? = null,
)

/**
 * The live search route returns pagination alongside its cards.  Keeping the envelope prevents
 * the native UI from guessing whether a short result list means "last page" or a partial reply.
 */
data class SearchPage(
    val items: List<NovelCard>,
    val page: Int = 1,
    val pageSize: Int = 60,
    val total: Int? = null,
    val totalPages: Int? = null
)

data class NovelTag(
    val id: Long? = null,
    val name: String,
    val count: Int? = null
)

data class Chapter(
    val id: Long,
    val title: String,
    val number: Int? = null,
    val wordCount: Long? = null,
    val imageCount: Int? = null,
    val updatedAt: String? = null
)

data class ReaderContent(
    val title: String?,
    val content: String,
    val source: String,
    val illustrations: List<ChapterIllustration> = emptyList()
)

/** A chapter payload kept in the reader's continuous-scroll window. */
data class ReaderChapterContent(
    val chapterId: Long,
    val title: String?,
    val content: ReaderContent,
)

/** A source-compatible reader tap zone. Width accepts values such as `30%` or `200px`. */
data class ReaderTapArea(
    val position: String,
    val width: String,
    val action: String,
)

data class ReaderProgress(
    val bookId: Long,
    val chapterId: Long,
    val chapterTitle: String? = null,
    val updatedAtMillis: Long = 0L,
    /** Local identity retained with the chapter title so Collection never loses reading context. */
    val bookTitle: String? = null,
    /** Source directory position retained so Collection can render a real N/total progress. */
    val chapterNumber: Int? = null,
    /**
     * Full source catalogue size observed when this reading position was saved.
     *
     * It is deliberately separate from [chapterNumber]: a shelf can only call a newer chapter an
     * update when the app actually observed that the reader had completed the previous catalogue.
     * Old records legitimately omit this value and then show progress without guessing an update.
     */
    val chapterCountAtLastRead: Int? = null,
    /** Same-chapter native viewport anchor; website sync intentionally remains chapter-level. */
    val viewportItemIndex: Int? = null,
    val viewportItemScrollOffsetPx: Int? = null,
)

/** A stable reader body item plus an intra-item offset, persisted only on this device. */
data class ReaderViewportAnchor(
    val chapterId: Long,
    val itemIndexWithinChapter: Int,
    val itemScrollOffsetPx: Int,
)

/** The last reader route, persisted only to recover from Android reclaiming the process. */
data class ReaderSession(
    val bookId: Long,
    val chapterId: Long,
)

/**
 * A local reader cache is only marked current when its source chapter revision still matches the
 * directory. This keeps the native drawer honest instead of painting every downloaded body as
 * the website's “缓存最新”.
 */
enum class ReaderChapterCacheState {
    Missing,
    Current,
    Stale,
}

data class FavoriteGroup(
    val id: Long?,
    val name: String,
    val count: Int? = null,
    val previews: List<FavoriteEntry> = emptyList()
)

/**
 * Website favourites are not merely novels: the favourite record has its own id, group, pin and
 * reading metadata. Keeping that envelope is required for source-compatible group management,
 * bulk actions and history rendering.
 */
data class FavoriteEntry(
    val favoriteId: Long? = null,
    val book: NovelCard,
    val groupId: Long? = null,
    val groupName: String? = null,
    val isPinned: Boolean = false,
    val createdAt: String? = null,
    val lastReadAt: String? = null,
    val lastChapterId: Long? = null,
    val lastChapter: Int? = null,
    val chapterCount: Int? = null
)

data class FavoritePage(
    val items: List<FavoriteEntry>,
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Int? = null,
    val totalPages: Int? = null
)

data class FavoriteStatus(
    val isFavorited: Boolean,
    val groupId: Long? = null,
    val rawState: String? = null
)

/**
 * A source glossary entry belongs to one novel and is intentionally kept separate from reader
 * replacement settings. The website may expose thousands of entries for a single title, so the
 * native screen always consumes this through [TerminologyPage] rather than loading a giant list.
 */
data class TerminologyEntry(
    val id: Long,
    val novelId: Long,
    val sourceName: String,
    val targetName: String,
    val description: String? = null,
    val lockStatus: String? = null,
    val isActive: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** The live `/api/terminologies` response is zero-based and returns its own page metadata. */
data class TerminologyPage(
    val items: List<TerminologyEntry>,
    val page: Int = 0,
    val pageSize: Int = 20,
    val total: Int? = null,
    val totalPages: Int? = null,
)

data class BookEditInfo(
    val id: Long,
    val title: String,
    val titleTranslation: String = "",
    val authorName: String,
    val description: String = "",
    val source: String = "",
    val sourceUrl: String = "",
    val language: String = "zh",
    val status: String = "连载中",
    val isAdult: Boolean = false,
    val photoUrl: String = "",
    val tags: List<String> = emptyList()
)

data class BookEditPermissions(
    val title: Boolean = false,
    val titleTranslation: Boolean = false,
    val authorName: Boolean = false,
    val description: Boolean = false,
    val source: Boolean = false,
    val sourceUrl: Boolean = false,
    val language: Boolean = false,
    val isAdult: Boolean = false,
    val photoUrl: Boolean = false,
    val spans: Boolean = false,
    val tags: Boolean = false
)

data class BookEditRequest(
    val title: String,
    val titleTranslation: String = "",
    val authorName: String,
    val description: String = "",
    val source: String = "",
    val sourceUrl: String = "",
    val language: String = "zh",
    val status: String = "连载中",
    val isAdult: Boolean = false,
    val photoUrl: String = "",
    val tags: List<String> = emptyList()
)

data class BookEditResult(
    val success: Boolean,
    val message: String? = null,
    val failedFields: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

data class ChapterIllustration(
    val id: Long,
    val index: Int,
    val src: String,
    /** The source viewer's original image when a list payload explicitly provides one. */
    val originalSrc: String? = null,
)

data class ChapterIllustrationPage(
    val images: List<ChapterIllustration> = emptyList(),
    val total: Int = images.size
)

data class ChapterIllustrationMutationResult(
    val success: Boolean,
    val imageCount: Int? = null,
    val message: String? = null,
    val errors: List<String> = emptyList()
)

data class ManagedBookAccessPolicy(
    val allowDownload: Boolean = true,
    val downloadThresholdType: String = "none",
    val downloadThresholdValue: Int = 0,
    val readThresholdType: String = "none",
    val readThresholdValue: Int = 0
)

data class ManagedBookTransferResult(
    val success: Boolean,
    val message: String? = null,
    val targetUsername: String? = null,
    val targetUserId: Long? = null
)

data class UserProfile(
    val id: Long?,
    val name: String,
    val role: String? = null,
    val points: Long? = null,
    val createdAt: String? = null,
    val avatarUrl: String? = null,
    val avatarFrameUrl: String? = null,
    val bio: String? = null,
    val email: String? = null,
    val isBanned: Boolean? = null,
    val banReason: String? = null,
    val banExpiresAt: String? = null,
    val isAdult: Boolean? = null,
    val deleted: Boolean? = null,
    /**
     * Source badges are styled records, not just labels. Keeping their safe presentation metadata
     * lets the native profile and backpack render the same individual cosmetic instead of a
     * generic Material chip.
     */
    val badges: List<UserBadge> = emptyList(),
    val stats: Map<String, Long> = emptyMap(),
    val showCheckin: Boolean? = null,
    val autoCheckin: Boolean? = null
)

/**
 * A user-facing Badge record returned by the profile or equipped-inventory APIs. The app only
 * renders presentation metadata; it never executes the source HTML or CSS.
 */
data class UserBadge(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val badgeHtml: String? = null,
    val badgeCss: String? = null,
)

/** A successful source authentication response. Credentials and CAPTCHA tokens are never stored. */
data class AuthSession(
    val token: String,
    val user: UserProfile? = null,
    val message: String? = null
)

/** A source acknowledgement without a replacement session, such as a verification email. */
data class AuthActionResult(
    val success: Boolean,
    val message: String? = null
)

data class UserCheckinStats(
    val totalDays: Int = 0,
    val totalPoints: Long = 0,
    val maxStreak: Int = 0,
    val currentStreak: Int = 0
)

data class UserCheckinAction(
    val success: Boolean,
    val message: String? = null,
    val points: Long? = null
)

data class UserActivity(
    val id: Long,
    val type: String,
    val title: String,
    val content: String? = null,
    val createdAt: String? = null,
    val postId: Long? = null,
    val bookId: Long? = null,
    val chapterId: Long? = null,
    val commentId: Long? = null,
    val coverUrl: String? = null
)

/**
 * The profile endpoint does not always include counters. The source's user-filtered content feeds
 * do expose pagination totals, so retain them separately from the currently visible timeline.
 */
data class UserContentActivityFeed(
    val activities: List<UserActivity> = emptyList(),
    val postCount: Long? = null,
    val forumCommentCount: Long? = null,
    val bookReviewCount: Long? = null
) {
    val commentCount: Long?
        get() = when {
            forumCommentCount == null && bookReviewCount == null -> null
            else -> (forumCommentCount ?: 0L) + (bookReviewCount ?: 0L)
        }
}

data class UserCheckinRecord(
    val date: String,
    val points: Long = 0
)

data class UserCheckinSettings(
    val showCheckin: Boolean = true,
    val autoCheckin: Boolean = false
)

/**
 * A cosmetic or account item returned by the source inventory endpoint.  The source has evolved
 * between an `items` envelope and a flat inventory list, so the native model intentionally keeps
 * only the presentation-safe fields shared by both shapes.
 */
data class UserInventoryItem(
    val id: Long,
    val name: String,
    /**
     * The inventory record and the shop item are different source identities.  A user can own
     * multiple records for one shop item, so presentation keys must use [inventoryId].
     */
    val inventoryId: Long = id,
    val itemId: Long = id,
    val type: String? = null,
    val description: String? = null,
    val quantity: Int = 1,
    val imageUrl: String? = null,
    /** Read-only source decoration metadata used by the native badge preview. */
    val badgeHtml: String? = null,
    val badgeCss: String? = null,
    val slot: String? = null,
    val equipped: Boolean = false,
    val expiresAt: String? = null
)

/** The user's inventory plus the server-confirmed currently equipped item ids. */
data class UserInventory(
    val items: List<UserInventoryItem> = emptyList(),
    val equippedItemIds: Set<Long> = emptySet()
)

/** A public cosmetic item from the normal user-facing shop endpoint. */
data class ShopItem(
    val id: Long,
    val name: String,
    val description: String? = null,
    val price: Long = 0L,
    val type: String = "frame",
    val imageUrl: String? = null,
    val badgeHtml: String? = null,
    val badgeCss: String? = null
)

/** Result returned after buying a normal shop cosmetic. */
data class ShopPurchaseResult(
    val success: Boolean = true,
    val message: String? = null
)

/** Read-only status for the source site's account/quiz reward. */
data class UserQuizRewardStatus(
    val claimed: Boolean? = null,
    val eligible: Boolean? = null,
    val rewardName: String? = null,
    val message: String? = null,
    val questionCount: Int? = null
)

data class AdminDailyCount(
    val date: String,
    val count: Int
)

data class AdminOverviewStats(
    val pendingReviewTotal: Int = 0,
    val pendingReviewUpload: Int = 0,
    val pendingReviewDelete: Int = 0,
    val pendingKeys: Int = 0,
    val approvedKeys: Int = 0,
    val activeTranslators: Int = 0,
    val todayUsers: Int = 0,
    val activeNovelTotal: Int = 0,
    val registeredUserTotal: Int = 0,
    val recentUserDaily: List<AdminDailyCount> = emptyList()
)

data class AdminReviewSettings(
    val autoApproveUpload: Boolean = false,
    val autoApproveDelete: Boolean = false
)

data class AdminReviewRequest(
    val id: Long,
    val type: String,
    val status: String,
    val username: String? = null,
    val userId: Long? = null,
    val novelId: Long? = null,
    val title: String? = null,
    val reason: String? = null,
    val createdAt: String? = null
)

data class AdminKeyItem(
    val id: Long,
    val name: String,
    val model: String? = null,
    val providerName: String? = null,
    val approvalStatus: String,
    val baseUrl: String? = null,
    val createdAt: String? = null
)

data class AdminOperationLog(
    val id: Long,
    val action: String,
    val status: String,
    val userId: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val novelId: Long? = null,
    val novelTitle: String? = null,
    val chapterId: Long? = null,
    val ipAddress: String? = null,
    val message: String? = null,
    val content: String? = null,
    val result: String? = null,
    val userAgent: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class AdminOperationLogPage(
    val items: List<AdminOperationLog> = emptyList(),
    val total: Int = 0,
    val totalPages: Int = 0,
    val actionTypes: List<String> = emptyList()
)

data class AdminCookieConfig(
    val id: Long,
    val configKey: String,
    val description: String? = null,
    val proxyIp: String? = null,
    val isActive: Boolean = false,
    val isHealthy: Boolean? = null,
    val lastError: String? = null,
    val updatedAt: String? = null,
    val updatedByUsername: String? = null,
    val successCount: Int = 0,
    val failCount: Int = 0
)

data class AdminBaseUrlRule(
    val id: Long,
    val pattern: String,
    val action: String,
    val description: String? = null,
    val createdAt: String? = null
)

data class AdminSchedulerLogs(
    val logs: List<String> = emptyList(),
    val totalLines: Int = 0,
    val fileSizeMb: Double? = null,
    val lastModified: String? = null
)

data class AdminShopItem(
    val id: Long,
    val name: String,
    val description: String? = null,
    val price: Long = 0,
    val type: String,
    val imageUrl: String? = null,
    val badgeHtml: String? = null,
    val badgeCss: String? = null,
    val isActive: Boolean = false
)

data class MessageStats(
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val readCount: Int = 0,
    val starredCount: Int = 0,
    val importantCount: Int = 0,
    val recentSevenDaysCount: Int = 0,
    val unreadByType: Map<Int, Int> = emptyMap()
)

data class SiteMessage(
    val id: Long,
    val type: Int,
    val title: String,
    val content: String? = null,
    val username: String? = null,
    val createdAt: String? = null,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val priority: Int = 0,
    val actionUrl: String? = null,
    val actionText: String? = null,
    val readAt: String? = null,
    val userId: Long? = null,
    val executeUserId: Long? = null,
    val avatarUrl: String? = null,
    val avatarFrameUrl: String? = null,
    val extraData: Map<String, String> = emptyMap()
)

data class MessageQuery(
    val keyword: String = "",
    val messageType: Int? = null,
    val isRead: Boolean? = null,
    val priority: Int? = null
)

data class MessagePagination(
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 1
)

data class MessagePage(
    val items: List<SiteMessage>,
    val pagination: MessagePagination
)

data class MessageSettings(
    val enableNotifications: Boolean = true,
    val enableEmail: Boolean = false,
    val enableBrowserPush: Boolean = true,
    val notificationTypes: Set<Int>? = null,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val autoReadAfterDays: Int? = null
)

data class DirectMessage(
    val id: Long,
    val content: String,
    val createdAt: String? = null,
    val userId: Long? = null,
    val executeUserId: Long? = null
)

data class MessageActionResult(
    val success: Boolean,
    val message: String? = null
)

data class WorkspaceApiConfig(
    val id: Long,
    val name: String,
    val model: String,
    val endpoint: String,
    val apiKey: String? = null,
    val concurrency: Int = 10,
    val isActive: Boolean = true,
    val isHealthy: Boolean? = null,
    val approvalStatus: String? = null,
    val totalRequests: Long = 0,
    /** Source `/workspace/apis` activation fields are separate from health. */
    val activationStatus: String? = null,
    val actualStatus: String? = null
)

data class WorkspaceCookieStatus(
    val hasCookie: Boolean = false
)

data class WorkspaceCookieConfig(
    val id: Long,
    val configKey: String,
    val description: String? = null,
    val cookieRaw: String? = null,
    val proxyIp: String? = null,
    val isActive: Boolean = true,
    val isHealthy: Boolean? = null,
    val lastCheckAt: String? = null,
    val updatedByUsername: String? = null
)

data class WorkspaceCookieConfigs(
    val myConfigs: List<WorkspaceCookieConfig> = emptyList(),
    val sharedConfigs: List<WorkspaceCookieConfig> = emptyList()
)

data class WorkspaceApiStatus(
    val total: Int = 0,
    val active: Int = 0,
    val healthy: Int = 0,
    val totalRequests: Long = 0
)

data class WorkspaceTranslatorHealth(
    val id: Long,
    val name: String,
    val model: String? = null,
    val endpoint: String? = null,
    val isHealthy: Boolean = false,
    val isActive: Boolean = false,
    val approvalStatus: String? = null,
    val responseTimeMs: Long = 0,
    val successRate: Double = 0.0,
    val lastHealthError: String? = null
)

data class WorkspaceHealth(
    val apiStatus: WorkspaceApiStatus = WorkspaceApiStatus(),
    val translators: List<WorkspaceTranslatorHealth> = emptyList()
)

data class WorkspaceActionResult(
    val success: Boolean,
    val message: String? = null,
    val id: Long? = null
)

data class WorkspaceLocalApiConfig(
    val id: Long,
    val name: String,
    val model: String,
    val endpoint: String,
    val apiKey: String,
    val concurrency: Int = 10,
    val sharedToServer: Boolean = false,
    val serverId: Long? = null
)

data class WorkspaceTranslationJob(
    val id: Long,
    val bookId: Long,
    val bookTitle: String,
    val translatorId: Long? = null,
    val translatorName: String,
    val chapterCount: Int = 0,
    val completedChapters: Int = 0,
    val status: String = "pending",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class UploadChapter(
    val title: String,
    val content: String,
    val chapterNumber: Int,
    val hierarchyLevel: Int = 0,
    val sectionPath: List<String> = emptyList(),
    val rawPath: String? = null,
    val spineIndex: Int? = null
)

data class ParsedEpub(
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val language: String = "zh",
    val chapters: List<UploadChapter> = emptyList(),
    val epubFilePath: String? = null
)

data class UploadBookRequest(
    val title: String,
    val titleTranslation: String = "",
    val authorName: String,
    val description: String = "",
    val language: String = "zh",
    val spans: String = "balanced",
    val isAdult: Boolean = false,
    val source: String = "",
    val sourceUrl: String = "",
    val tags: List<String> = emptyList(),
    val submitType: String = "chinese",
    val chapters: List<UploadChapter>,
    val epubFilePath: String? = null,
    val coverUrl: String? = null
)

data class UploadActionResult(
    val success: Boolean,
    val message: String? = null,
    val novelId: Long? = null
)

data class EditorBookMetadata(
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val language: String = "zh",
    val tags: String = "",
    val isAdult: Boolean = false,
    val source: String = "",
    val sourceUrl: String = ""
)

data class EditorArchive(
    val id: String,
    val name: String,
    val timestamp: Long,
    val textContent: String,
    val metadata: EditorBookMetadata = EditorBookMetadata(),
    val fileName: String? = null,
    val chapterCount: Int = 0,
    val totalWords: Int = 0
)

data class PoliticalExamQuestion(
    val question: String,
    val options: List<String> = emptyList()
)

data class PoliticalExamPaper(
    val singleChoice: List<PoliticalExamQuestion> = emptyList(),
    val multipleChoice: List<PoliticalExamQuestion> = emptyList(),
    val trueFalse: List<PoliticalExamQuestion> = emptyList(),
    val fillBlank: List<PoliticalExamQuestion> = emptyList()
) {
    val totalQuestions: Int
        get() = singleChoice.size + multipleChoice.size + trueFalse.size + fillBlank.size
}

data class PoliticalExamSession(
    val sessionId: String,
    val remainingTimeSeconds: Int = 1800,
    val paper: PoliticalExamPaper = PoliticalExamPaper()
)

data class PoliticalExamAnswers(
    val singleChoice: List<Int?> = emptyList(),
    val multipleChoice: List<List<Int>> = emptyList(),
    val trueFalse: List<Boolean?> = emptyList(),
    val fillBlank: List<String> = emptyList()
)

data class PoliticalExamDetail(
    val correct: Boolean,
    val question: String? = null,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val explanation: String? = null
)

data class PoliticalExamResult(
    val score: Int,
    val total: Int,
    val passed: Boolean,
    val details: Map<String, List<PoliticalExamDetail>> = emptyMap(),
    val token: String? = null
)

data class ForumPost(
    val id: Long,
    val category: String,
    val title: String,
    val authorName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorAvatarFrameUrl: String? = null,
    val authorBadges: List<String> = emptyList(),
    val authorBadgeVisuals: List<UserBadge> = emptyList(),
    val bookId: Long? = null,
    val bookTitle: String? = null,
    val bookCoverUrl: String? = null,
    val isBookReview: Boolean = false,
    val replyCount: Int? = null,
    val likeCount: Int? = null,
    val helpfulCount: Int? = null,
    val notHelpfulCount: Int? = null,
    val funnyCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null,
    val viewCount: Int? = null,
    val createdAt: String? = null,
    val lastActiveLabel: String? = null,
    val excerpt: String? = null,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val featured: Boolean = false,
    val authorId: Long? = null
)

/**
 * The book-review feed publishes its live total alongside the current page. Keeping that envelope
 * lets the forum rail show the same review count as the source without an extra network request.
 */
data class ForumPostPage(
    val posts: List<ForumPost>,
    val total: Int? = null,
    /** Source pagination is visible in the mobile forum footer, so retain it instead of inferring. */
    val page: Int = 1,
    val totalPages: Int? = null
)

/** Source poll option retained verbatim enough for native result rendering and a vote submission. */
data class ForumPollOption(
    val id: Long,
    val text: String,
    val voteCount: Int = 0,
    val sortOrder: Int? = null,
)

/** A forum-post poll is part of the post detail payload, not a separate WebView-only feature. */
data class ForumPoll(
    val id: Long = 0L,
    val question: String? = null,
    val allowMultiple: Boolean = false,
    val maxChoices: Int = 1,
    val endsAt: String? = null,
    val isClosed: Boolean = false,
    val totalVotes: Int = 0,
    val options: List<ForumPollOption> = emptyList(),
    val userVoteOptionIds: Set<Long> = emptySet(),
)

data class ForumPostDetail(
    val post: ForumPost,
    val content: String? = null,
    val likeCount: Int? = null,
    val dislikeCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null,
    val poll: ForumPoll? = null,
)

data class ForumComment(
    val id: Long,
    val postId: Long? = null,
    val parentCommentId: Long? = null,
    val authorName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorAvatarFrameUrl: String? = null,
    val authorBadges: List<String> = emptyList(),
    val authorBadgeVisuals: List<UserBadge> = emptyList(),
    val replyToName: String? = null,
    val content: String,
    val likeCount: Int? = null,
    val dislikeCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null,
    val replyCount: Int? = null,
    val createdAt: String? = null,
    val authorId: Long? = null
)

data class ChapterComment(
    val id: Long,
    val bookId: Long? = null,
    val chapterId: Long? = null,
    val parentCommentId: Long? = null,
    val authorName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorAvatarFrameUrl: String? = null,
    val authorBadges: List<String> = emptyList(),
    val authorBadgeVisuals: List<UserBadge> = emptyList(),
    val replyToName: String? = null,
    val content: String,
    val likeCount: Int? = null,
    val dislikeCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null,
    val replyCount: Int? = null,
    val createdAt: String? = null,
    val authorId: Long? = null
)

data class ForumActionResult(
    val success: Boolean,
    val message: String? = null,
    /** The source echoes a newly created forum reply so clients can render it immediately. */
    val reply: ForumComment? = null,
)

data class ForumPollDraft(
    val question: String? = null,
    val options: List<String>,
    val allowMultiple: Boolean = false,
    val maxChoices: Int = 1,
    val endsAt: String? = null
)

data class ForumCreateRequest(
    val type: String,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val poll: ForumPollDraft? = null
)

data class ForumCreateResult(
    val success: Boolean,
    val message: String? = null,
    val postId: Long? = null
)

sealed interface LoadResult<out T> {
    object Idle : LoadResult<Nothing>
    object Loading : LoadResult<Nothing>
    data class Success<T>(val value: T) : LoadResult<T>
    data class Error(val message: String) : LoadResult<Nothing>
}
