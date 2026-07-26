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
    val sourceReadCount: Long? = null,
    val sourceFavoriteCount: Long? = null,
    val updatedAt: String? = null,
    val tags: List<String> = emptyList(),
    val fullCoverUrl: String? = null
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
    val updatedAt: String? = null
)

data class ReaderContent(
    val title: String?,
    val content: String,
    val source: String,
    val illustrations: List<ChapterIllustration> = emptyList()
)

data class ReaderProgress(
    val bookId: Long,
    val chapterId: Long,
    val chapterTitle: String? = null,
    val updatedAtMillis: Long = 0L
)

data class FavoriteGroup(
    val id: Long?,
    val name: String,
    val count: Int? = null
)

data class FavoriteStatus(
    val isFavorited: Boolean,
    val groupId: Long? = null,
    val rawState: String? = null
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
    val src: String
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
    val badges: List<String> = emptyList(),
    val stats: Map<String, Long> = emptyMap(),
    val showCheckin: Boolean? = null,
    val autoCheckin: Boolean? = null
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

data class UserCheckinRecord(
    val date: String,
    val points: Long = 0
)

data class UserCheckinSettings(
    val showCheckin: Boolean = true,
    val autoCheckin: Boolean = false
)

data class AdminDailyCount(
    val date: String,
    val count: Int
)

data class AdminOverviewStats(
    val pendingReviewTotal: Int = 0,
    val pendingReviewUpload: Int = 0,
    val pendingReviewDelete: Int = 0,
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
    val novelId: Long? = null,
    val message: String? = null,
    val createdAt: String? = null
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
    val updatedAt: String? = null
)

data class AdminBaseUrlRule(
    val id: Long,
    val pattern: String,
    val action: String,
    val description: String? = null
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
    val totalRequests: Long = 0
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
    val bookTitle: String? = null,
    val replyCount: Int? = null,
    val likeCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null,
    val viewCount: Int? = null,
    val lastActiveLabel: String? = null,
    val excerpt: String? = null,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val featured: Boolean = false,
    val authorId: Long? = null
)

data class ForumPostDetail(
    val post: ForumPost,
    val content: String? = null,
    val likeCount: Int? = null,
    val dislikeCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null
)

data class ForumComment(
    val id: Long,
    val postId: Long? = null,
    val parentCommentId: Long? = null,
    val authorName: String? = null,
    val replyToName: String? = null,
    val content: String,
    val likeCount: Int? = null,
    val dislikeCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null,
    val createdAt: String? = null,
    val authorId: Long? = null
)

data class ChapterComment(
    val id: Long,
    val bookId: Long? = null,
    val chapterId: Long? = null,
    val parentCommentId: Long? = null,
    val authorName: String? = null,
    val replyToName: String? = null,
    val content: String,
    val likeCount: Int? = null,
    val dislikeCount: Int? = null,
    val reactionCount: Int? = null,
    val awardPoints: Int? = null,
    val createdAt: String? = null,
    val authorId: Long? = null
)

data class ForumActionResult(
    val success: Boolean,
    val message: String? = null
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
