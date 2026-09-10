package com.novalpie.nativeapp.ui

import android.app.Application
import android.content.ContentValues
import android.content.ContentUris
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.webkit.CookieManager
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.core.AppNavigator
import com.novalpie.nativeapp.feature.download.DownloadTask
import com.novalpie.nativeapp.feature.download.DownloadFormat
import com.novalpie.nativeapp.feature.download.DownloadPhase
import com.novalpie.nativeapp.feature.download.NativeDownloadService
import com.novalpie.nativeapp.feature.download.downloadStatusText
import com.novalpie.nativeapp.feature.search.SearchViewModel
import com.novalpie.nativeapp.feature.search.WebsiteSearchRepository
import com.novalpie.nativeapp.feature.search.StoredSearchPreferences
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.data.AppThemeSettingsStore
import com.novalpie.nativeapp.data.AuthSessionStore
import com.novalpie.nativeapp.data.ChineseVariantSettingsStore
import com.novalpie.nativeapp.data.DownloadSettingsStore
import com.novalpie.nativeapp.data.EpubDownloadTicket
import com.novalpie.nativeapp.data.EpubParser
import com.novalpie.nativeapp.feature.admin.AdminCommand
import com.novalpie.nativeapp.data.EpubWriter
import com.novalpie.nativeapp.data.EditorArchiveStore
import com.novalpie.nativeapp.data.EditorBatchImporter
import com.novalpie.nativeapp.data.EditorProcessor
import com.novalpie.nativeapp.data.FavoritesSettingsStore
import com.novalpie.nativeapp.data.ProfileBooksSettingsStore
import com.novalpie.nativeapp.data.NetworkConfigStore
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.NativeEpubArchiveWriter
import com.novalpie.nativeapp.data.NativeEpubAsset
import com.novalpie.nativeapp.data.NativeDownloadChapterText
import com.novalpie.nativeapp.data.NativeEpubExportProgress
import com.novalpie.nativeapp.data.NativeEpubMetadata
import com.novalpie.nativeapp.data.NativeDownloadControl
import com.novalpie.nativeapp.data.copyNativeDownloadFilePausable
import com.novalpie.nativeapp.data.copyNativeDownloadStream
import com.novalpie.nativeapp.data.cleanupNativeEpubTempFiles
import com.novalpie.nativeapp.data.nativeEpubGenerationFile
import com.novalpie.nativeapp.data.isNativeDownloadDisplayName
import com.novalpie.nativeapp.data.PersistedFavoritesSettings
import com.novalpie.nativeapp.data.PersistedSearchSettings
import com.novalpie.nativeapp.data.ProxySettings
import com.novalpie.nativeapp.data.ReaderProgressStore
import com.novalpie.nativeapp.data.ReaderChapterCacheStore
import com.novalpie.nativeapp.data.ReaderSessionStore
import com.novalpie.nativeapp.data.ReaderSettingsStore
import com.novalpie.nativeapp.data.ReaderFontStore
import com.novalpie.nativeapp.data.ReaderSettingsValues
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.data.ReaderTtsSettingsStore
import com.novalpie.nativeapp.data.ReaderReplacementRulesStore
import com.novalpie.nativeapp.data.SearchHistoryStore
import com.novalpie.nativeapp.data.SearchSettingsStore
import com.novalpie.nativeapp.data.WorkspaceLocalStore
import com.novalpie.nativeapp.data.UploadFileSource
import com.novalpie.nativeapp.data.clearNovalPieImageCaches
import com.novalpie.nativeapp.data.configureNovalPieImageLoader
import com.novalpie.nativeapp.data.decodeAuthTokenProfile
import com.novalpie.nativeapp.data.isEmulatorRuntime
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.AppThemeMode
import com.novalpie.nativeapp.model.ChineseVariant
import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.ChapterIllustrationPage
import com.novalpie.nativeapp.model.BookEditInfo
import com.novalpie.nativeapp.model.BookEditPermissions
import com.novalpie.nativeapp.model.BookEditRequest
import com.novalpie.nativeapp.model.DirectMessage
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.FavoritesCacheMode
import com.novalpie.nativeapp.model.FavoriteGroup
import com.novalpie.nativeapp.model.FavoritePage
import com.novalpie.nativeapp.model.FavoriteStatus
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumActionResult
import com.novalpie.nativeapp.model.ForumCreateRequest
import com.novalpie.nativeapp.model.ForumPollDraft
import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.ForumPostDetail
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.MessageStats
import com.novalpie.nativeapp.model.MessagePagination
import com.novalpie.nativeapp.model.MessageQuery
import com.novalpie.nativeapp.model.MessageSettings
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.SearchPage
import com.novalpie.nativeapp.model.ShopItem
import com.novalpie.nativeapp.model.ShopPurchaseResult
import com.novalpie.nativeapp.model.PoliticalExamAnswers
import com.novalpie.nativeapp.model.PoliticalExamResult
import com.novalpie.nativeapp.model.PoliticalExamSession
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.ReaderChapterCacheState
import com.novalpie.nativeapp.model.ReaderCustomTheme
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.ReaderSession
import com.novalpie.nativeapp.model.ReaderTapArea
import com.novalpie.nativeapp.model.ReaderViewportAnchor
import com.novalpie.nativeapp.model.ReaderReplacementOwner
import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.SiteMessage
import com.novalpie.nativeapp.model.TerminologyEntry
import com.novalpie.nativeapp.model.TerminologyPage
import com.novalpie.nativeapp.model.UploadActionResult
import com.novalpie.nativeapp.model.UploadBookRequest
import com.novalpie.nativeapp.model.UploadChapter
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserContentActivityFeed
import com.novalpie.nativeapp.model.UserCheckinSettings
import com.novalpie.nativeapp.model.UserCheckinRecord
import com.novalpie.nativeapp.model.UserInventory
import com.novalpie.nativeapp.model.UserInventoryItem
import com.novalpie.nativeapp.model.UserQuizRewardStatus
import com.novalpie.nativeapp.model.WorkspaceApiConfig
import com.novalpie.nativeapp.model.WorkspaceCookieConfigs
import com.novalpie.nativeapp.model.WorkspaceCookieStatus
import com.novalpie.nativeapp.model.WorkspaceHealth
import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import com.novalpie.nativeapp.model.WorkspaceTranslationJob
import com.novalpie.nativeapp.model.next
import com.novalpie.nativeapp.model.AdminBaseUrlRule
import com.novalpie.nativeapp.model.AdminCookieConfig
import com.novalpie.nativeapp.model.AdminKeyItem
import com.novalpie.nativeapp.model.AdminOperationLogPage
import com.novalpie.nativeapp.model.AdminOverviewStats
import com.novalpie.nativeapp.model.AdminReviewRequest
import com.novalpie.nativeapp.model.AdminReviewSettings
import com.novalpie.nativeapp.model.AdminSchedulerLogs
import com.novalpie.nativeapp.model.AdminShopItem
import com.novalpie.nativeapp.model.AuthActionResult
import com.novalpie.nativeapp.model.AuthSession
import com.novalpie.nativeapp.model.UserCheckinAction
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

enum class BottomTab(val title: String) {
    Collection("收藏"),
    Discover("搜索"),
    Tools("工具"),
    Forum("论坛"),
    Profile("我的")
}

sealed class AppRoute {
    object Forum : AppRoute()
    object Home : AppRoute()
    object Search : AppRoute()
    object Tools : AppRoute()
    object Profile : AppRoute()
    object Settings : AppRoute()
    object MessageCenter : AppRoute()
    object MessageSettings : AppRoute()
    object Workspace : AppRoute()
    object UploadBook : AppRoute()
    object UploadEditor : AppRoute()
    object PoliticalExam : AppRoute()
    data class Auth(val page: AuthPage) : AppRoute()
    object AuthCaptcha : AppRoute()
    data class MessageDetail(val messageId: Long) : AppRoute()
    data class MessageConversation(val targetUserId: Long, val targetName: String?) : AppRoute()
    data class ForumPostDetail(val postId: Long) : AppRoute()
    object ForumCreate : AppRoute()
    data class BookDetail(val bookId: Long) : AppRoute()
    data class Terminology(val bookId: Long) : AppRoute()
    data class BookEditInfo(val bookId: Long) : AppRoute()
    data class BookChapters(val bookId: Long) : AppRoute()
    data class BookAppend(val bookId: Long) : AppRoute()
    data class Reader(
        val bookId: Long,
        val chapterId: Long,
        val entryPosition: ReaderChapterEntryPosition = ReaderChapterEntryPosition.Start,
    ) : AppRoute()
    data class UserProfileDetail(val userId: Long) : AppRoute()
    data class Admin(val section: AdminSection) : AppRoute()
    data class WebFallback(val url: String) : AppRoute()
}

data class HomeState(
    val user: LoadResult<UserProfile> = LoadResult.Idle,
    val groups: LoadResult<List<FavoriteGroup>> = LoadResult.Idle,
    val favorites: LoadResult<List<NovelCard>> = LoadResult.Idle,
    val favoriteEntries: LoadResult<List<FavoriteEntry>> = LoadResult.Idle,
    /** Source total remains accurate even when only the first/visible page has loaded. */
    val favoriteTotal: Int? = null,
    val history: LoadResult<List<FavoriteEntry>> = LoadResult.Idle,
    val historyTotal:Int?=null,
    val favoritesPage: Int = 1,
    val favoritesCanLoadMore: Boolean = false,
    val favoritesLoadingMore: Boolean = false,
    /**
     * A failed *additional* page, kept separate from [favorites].
     *
     * Load-more failures used to overwrite `favorites` with `LoadResult.Error`, so a user who had
     * paged through 80 books and then hit one timeout lost all 80 and had to start from page 1.
     * The already-loaded pages are still perfectly good data; only the new page failed.
     */
    val favoritesLoadMoreError: String? = null,
    val selectedFavoriteGroupId: Long? = null,
    val options: FavoritesUiOptions = FavoritesUiOptions(),
    val selectionMode: Boolean = false,
    val selectedBookIds: Set<Long> = emptySet(),
    val actionLoading: Boolean = false,
    val actionMessage: String? = null
)

enum class FavoritesContentTab { Favorites, History }

enum class FavoritesLayout { Grid, List }

enum class FavoritesDisplayMode { Default, All, Unclassified }

data class FavoritesUiOptions(
    val cacheMode: FavoritesCacheMode = FavoritesCacheMode.All,
    val tab: FavoritesContentTab = FavoritesContentTab.Favorites,
    val layout: FavoritesLayout = FavoritesLayout.Grid,
    val gridColumns: Int = 2,
    val displayMode: FavoritesDisplayMode = FavoritesDisplayMode.Default,
    val currentPage: Int = 1,
    val sortField: String = "created_at",
    val sortOrder: String = "desc"
)

data class ForumState(
    val posts: LoadResult<List<ForumPost>> = LoadResult.Idle,
    val selectedType: String = "discussion",
    val searchQuery: String = "",
    val reviewTotal: Int? = null,
    val hideSpoilers: Boolean = true,
    val page: Int = 1,
    val totalPages: Int? = null,
    val canLoadMore: Boolean = false,
    val loadingMore: Boolean = false,
    val loadMoreError: String? = null
)

data class ProfileState(
    val profile: LoadResult<UserProfile> = LoadResult.Idle,
    val checkinStats: LoadResult<UserCheckinStats> = LoadResult.Idle,
    val checkinRecords: LoadResult<List<UserCheckinRecord>> = LoadResult.Idle,
    val activities: LoadResult<List<UserActivity>> = LoadResult.Idle,
    val books: LoadResult<List<NovelCard>> = LoadResult.Idle,
    /** Raw section payloads let late arrivals enrich the hero without resetting other panels. */
    val activityFeed: UserContentActivityFeed? = null,
    val activityPage: Int = 0,
    val loadingMoreActivities: Boolean = false,
    val activityPageMessage: String? = null,
    val uploadedBooks: List<NovelCard>? = null,
    val bookQuery: String = "",
    val booksGridColumns: Int = 2,
    val downloadImageConcurrency: Int = com.novalpie.nativeapp.data.DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY,
    val inventory: LoadResult<UserInventory> = LoadResult.Idle,
    val shopItems: LoadResult<List<ShopItem>> = LoadResult.Idle,
    val quizReward: LoadResult<UserQuizRewardStatus> = LoadResult.Idle,
    val selectedTab: ProfileTab = ProfileTab.Account,
    val activityFilter: ProfileActivityFilter = ProfileActivityFilter.All,
    val personalizationTab: PersonalizationTab = PersonalizationTab.Shop,
    val nameDraft: String = "",
    val bioDraft: String = "",
    val showCheckin: Boolean = true,
    val autoCheckin: Boolean = false,
    val adultBirthYearDraft: String = "",
    val saving: Boolean = false,
    val checkingIn: Boolean = false,
    val verifyingAdult: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val inventoryActionInventoryId: Long? = null,
    val shopPurchaseItemId: Long? = null,
    val actionMessage: String? = null
)

/** Mirrors the website's profile tabs while retaining the account editor only the owner can see. */
enum class ProfileTab {
    Account,
    Checkin,
    Activities,
    Books,
    Inventory,
    BlockedUsers,
    Downloads,
}

/** The same activity categories exposed by the website profile ActivityTab. */
enum class ProfileActivityFilter {
    All,
    Posts,
    Comments,
    BookReviews,
    ChapterReviews,
}

/** Mirrors the two inner tabs of the website's owner-only PersonalizationTab. */
enum class PersonalizationTab {
    Shop,
    Inventory
}

enum class UserProfileTab {
    Checkin,
    Activities,
    Books
}

data class UserProfileDetailState(
    val userId: Long = 0,
    val profile: LoadResult<UserProfile> = LoadResult.Idle,
    val activities: LoadResult<List<UserActivity>> = LoadResult.Idle,
    val books: LoadResult<List<NovelCard>> = LoadResult.Idle,
    /** Raw secondary payloads are retained so out-of-order profile requests can be merged. */
    val activityFeed: UserContentActivityFeed? = null,
    val activityPage: Int = 0,
    val loadingMoreActivities: Boolean = false,
    val activityPageMessage: String? = null,
    val publicBooks: List<NovelCard>? = null,
    val checkinStats: LoadResult<UserCheckinStats> = LoadResult.Idle,
    val checkinRecords: LoadResult<List<UserCheckinRecord>> = LoadResult.Idle,
    val checkinSettings: LoadResult<UserCheckinSettings> = LoadResult.Idle,
    val selectedTab: UserProfileTab = UserProfileTab.Activities,
    val activityFilter: ProfileActivityFilter = ProfileActivityFilter.All,
)

enum class AdminSection(val websitePath: String) {
    Overview("/admin"),
    Review("/admin/review"),
    Keys("/admin/key-management"),
    OperationLogs("/admin/operation-logs"),
    Scraper("/admin/scraper-management"),
    Shop("/admin/shop")
}

data class AdminState(
    val section: AdminSection = AdminSection.Overview,
    val overviewDays: Int = 5,
    val reviewQuery: AdminReviewQuery = AdminReviewQuery(),
    val operationLogQuery: AdminOperationLogQuery = AdminOperationLogQuery(),
    val shopQuery: AdminShopQuery = AdminShopQuery(),
    val overview: LoadResult<AdminOverviewStats> = LoadResult.Idle,
    val reviewSettings: LoadResult<AdminReviewSettings> = LoadResult.Idle,
    val reviewRequests: LoadResult<List<AdminReviewRequest>> = LoadResult.Idle,
    val keys: LoadResult<List<AdminKeyItem>> = LoadResult.Idle,
    val operationLogs: LoadResult<AdminOperationLogPage> = LoadResult.Idle,
    val cookieConfigs: LoadResult<List<AdminCookieConfig>> = LoadResult.Idle,
    val baseUrlRules: LoadResult<List<AdminBaseUrlRule>> = LoadResult.Idle,
    val schedulerLogs: LoadResult<AdminSchedulerLogs> = LoadResult.Idle,
    val shopItems: LoadResult<List<AdminShopItem>> = LoadResult.Idle,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null,
    val actionError: Boolean = false,
    val actionUncertain: Boolean = false,
    val failedEditDraft: com.novalpie.nativeapp.feature.admin.AdminEditDraft? = null,
    val schedulerLines: Int = 100,
    val accessRevision: Long = 0,
)

data class AdminReviewQuery(
    val type: String = "",
    val status: String = "",
    val keyword: String = ""
)

data class AdminOperationLogQuery(
    val page: Int = 1,
    val action: String = "",
    val status: String = "",
    val userId: String = "",
    val novelId: String = "",
    val keyword: String = "",
    val startDate: String = "",
    val endDate: String = ""
)

data class AdminShopQuery(
    val type: String = "",
    val isActive: Boolean? = null,
    val keyword: String = ""
)

data class ToolsState(
    val stats: LoadResult<MessageStats> = LoadResult.Idle,
    val messages: LoadResult<List<SiteMessage>> = LoadResult.Idle
)


data class WorkspaceState(
    val selectedTab: WorkspaceTab = WorkspaceTab.Overview,
    val apiConfigs: LoadResult<List<WorkspaceApiConfig>> = LoadResult.Idle,
    val cookieStatus: LoadResult<WorkspaceCookieStatus> = LoadResult.Idle,
    val cookieConfigs: LoadResult<WorkspaceCookieConfigs> = LoadResult.Idle,
    val health: LoadResult<WorkspaceHealth> = LoadResult.Idle,
    val localApis: List<WorkspaceLocalApiConfig> = emptyList(),
    val jobs: List<WorkspaceTranslationJob> = emptyList(),
    val actionLoading: Boolean = false,
    val actionMessage: String? = null,
    val failedApiDraft: WorkspaceApiDraft? = null,
    val failedCookieDraft: WorkspaceCookieDraft? = null,
    val hasUnassignedLegacyData: Boolean = false,
    val translationBookId: Long? = null,
)

data class UploadDocument(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String? = null
)

data class UploadBookState(
    val existingNovelId: Long? = null,
    val draft: UploadBookDraft = UploadBookDraft(),
    val selectedFile: UploadDocument? = null,
    val chapters: LoadResult<List<UploadChapter>> = LoadResult.Idle,
    val serverFilePath: String? = null,
    val processing: Boolean = false,
    val progressLabel: String? = null,
    val submitResult: LoadResult<UploadActionResult> = LoadResult.Idle,
    val actionMessage: String? = null,
    val submissionUncertain: Boolean = false,
    val restoringDraft: Boolean = false,
    val draftStorageError: String? = null,
    val batchCheckpoint: com.novalpie.nativeapp.model.UploadBatchCheckpoint? = null,
)

data class UploadEditorState(
    val selectedTab: EditorTab = EditorTab.Text,
    val text: String = "",
    val cursorPosition: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val fileName: String? = null,
    val files: List<UploadDocument> = emptyList(),
    val encoding: String = "UTF-8",
    val metadata: EditorBookMetadata = EditorBookMetadata(),
    val chapters: List<UploadChapter> = emptyList(),
    val splitMode: EditorSplitMode = EditorSplitMode.Regex,
    val splitPattern: String = DEFAULT_EDITOR_CHAPTER_REGEX,
    val splitTarget: String = "3000",
    val customScript: String = DEFAULT_EDITOR_CUSTOM_SCRIPT,
    val scriptChunked: Boolean = false,
    val scriptChunkSize: String = "200000",
    val scriptRunId: Long = 0,
    val apiEndpoint: String = "http://localhost:8000",
    val apiTimeoutSeconds: String = "30",
    val apiMarkerMode: EditorMarkerMode = EditorMarkerMode.Incremental,
    val batchMode: EditorBatchMode = EditorBatchMode.Chapters,
    val batchTarget: String = EditorBatchMode.Chapters.defaultTarget,
    val markerValidationErrors: List<String> = emptyList(),
    val aiConfigs: List<WorkspaceLocalApiConfig> = emptyList(),
    val selectedAiConfigId: Long? = null,
    val findText: String = "",
    val replaceText: String = "",
    val findUsesRegex: Boolean = false,
    val archiveName: String = "",
    val archives: List<EditorArchive> = emptyList(),
    val busy: Boolean = false,
    val actionMessage: String? = null
)

data class PoliticalExamState(
    val phase: PoliticalExamPhase = PoliticalExamPhase.Landing,
    val session: LoadResult<PoliticalExamSession> = LoadResult.Idle,
    val answers: PoliticalExamAnswers = PoliticalExamAnswers(),
    val remainingTimeSeconds: Int = 1800,
    val deadlineEpochMillis: Long? = null,
    val result: LoadResult<PoliticalExamResult> = LoadResult.Idle,
    val submitting: Boolean = false,
    /**
     * Set once the expiry auto-submit has been attempted, so it is never retried automatically.
     *
     * Without this the screen hammered the server. The timer effect is keyed on `submitting`, and
     * a failed submit set `submitting = false` while leaving `phase = Active` and
     * `remainingTimeSeconds = 0`. Because the effect skips its 1s delay once the clock reaches
     * zero, the false->true->false transition re-fired it immediately, producing an unthrottled
     * loop of POSTs to /api/political-exams/sessions/submit for as long as the user stayed on the
     * screen. Retrying is now the user's explicit action.
     */
    val autoSubmitAttempted: Boolean = false,
    val actionMessage: String? = null
)

data class AuthState(
    val loginMethod: AuthLoginMethod = AuthLoginMethod.Password,
    val loginUsername: String = "",
    val loginPassword: String = "",
    val loginEmail: String = "",
    val loginCode: String = "",
    val registerStep: AuthRegisterStep = AuthRegisterStep.Email,
    val registerEmail: String = "",
    val registerCode: String = "",
    val registerUsername: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    val resetEmail: String = "",
    val resetToken: String = "",
    val resetPassword: String = "",
    val resetConfirmPassword: String = "",
    val captchaToken: String? = null,
    val pendingCaptchaAction: AuthCaptchaAction? = null,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null
)

data class ForumPostDetailState(
    val postId: Long = 0,
    val detail: LoadResult<ForumPostDetail> = LoadResult.Idle,
    val comments: LoadResult<List<ForumComment>> = LoadResult.Idle,
    /** Source [bookid:...] markers resolved for the currently open post and its discussion tree. */
    val bookReferences: Map<Long, LoadResult<NovelCard>> = emptyMap(),
    val commentDraft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
    /** Reused after a transport failure so a retry cannot create a duplicate reply. */
    val commentClientRequestId: String? = null,
    val expandedCommentIds: Set<Long> = emptySet(),
    val actionMessage: String? = null,
    val actionLoading: Boolean = false,
    /** Locally staged option ids are kept separate from the source's authoritative user votes. */
    val selectedPollOptionIds: Set<Long> = emptySet(),
    val commentsPage: Int = 1,
    val commentsHasMore: Boolean = false,
    val commentsLoadingMore: Boolean = false,
)

/**
 * Keep the post body and its discussion tree independently observable. The source returns them
 * from separate endpoints, so waiting for comments must never turn a readable post into a blank
 * loading screen.
 */
internal fun forumPostDetailWithLoadedDetail(
    state: ForumPostDetailState,
    result: Result<ForumPostDetail>,
): ForumPostDetailState = state.copy(
    detail = result.fold(
        onSuccess = { LoadResult.Success(it) },
        onFailure = { LoadResult.Error(apiFailureMessage(VisibleUiLabels.ForumPostDetail, it)) },
    ),
)

/** Apply a comments result without replacing a successfully rendered post body. */
internal fun forumPostDetailWithLoadedComments(
    state: ForumPostDetailState,
    result: Result<List<ForumComment>>,
    retainVisibleComments: Boolean,
): ForumPostDetailState {
    val visibleComments = (state.comments as? LoadResult.Success<List<ForumComment>>)?.value.orEmpty()
    val comments = result.fold(
        onSuccess = { sourceComments ->
            LoadResult.Success(
                (sourceComments + visibleComments.filterNot { current ->
                    sourceComments.any { it.id == current.id && it.parentCommentId == current.parentCommentId }
                }).distinctBy { it.parentCommentId to it.id },
            )
        },
        onFailure = { failure ->
            if (retainVisibleComments && state.comments is LoadResult.Success) {
                state.comments
            } else {
                LoadResult.Error(apiFailureMessage(VisibleUiLabels.Comments, failure))
            }
        },
    )
    return state.copy(comments = comments)
}

/** Retry one post section without blanking the other independently loaded section. */
internal fun forumPostDetailForPostRetry(state: ForumPostDetailState): ForumPostDetailState =
    state.copy(detail = LoadResult.Loading)

/** Retry comments without hiding a post body that is already readable. */
internal fun forumPostDetailForCommentsRetry(state: ForumPostDetailState): ForumPostDetailState =
    state.copy(comments = LoadResult.Loading)

/** A late partial reference lookup must never replace a newer complete post/comment lookup. */
internal fun forumPostDetailWithResolvedBookReferences(
    state: ForumPostDetailState,
    resolutionSerial: Long,
    activeResolutionSerial: Long,
    resolved: Map<Long, LoadResult<NovelCard>>,
): ForumPostDetailState = if (resolutionSerial == activeResolutionSerial) {
    state.copy(bookReferences = resolved)
} else {
    state
}

private fun forumPostDetailWithImmediateReply(
    state: ForumPostDetailState,
    reply: ForumComment?,
): ForumPostDetailState {
    val echoedReply = reply ?: return state
    val visible = (state.comments as? LoadResult.Success)?.value ?: return state
    val merged = (visible.filterNot { it.id == echoedReply.id && it.parentCommentId == echoedReply.parentCommentId } + echoedReply)
    val rootId = forumCommentThreads(merged)
        .firstOrNull { thread ->
            thread.comment.id == echoedReply.id || thread.replies.any { it.id == echoedReply.id }
        }
        ?.comment
        ?.id
    return state.copy(
        comments = LoadResult.Success(merged),
        expandedCommentIds = rootId?.let { state.expandedCommentIds + it } ?: state.expandedCommentIds,
    )
}

/** Applies a comment result without throwing away a nested-reply target before refresh. */
internal fun forumPostDetailAfterCommentSubmission(
    state: ForumPostDetailState,
    result: Result<ForumActionResult>,
): ForumPostDetailState = result.fold(
    onSuccess = { action ->
        // An echoed reply is authoritative about its source post.  A late completion from a
        // prior detail route must not erase the current route's draft/reply retry state.
        if (action.success && action.reply?.postId != null && action.reply.postId != state.postId) {
            state
        } else
        if (!action.success) {
            state.copy(
                actionLoading = false,
                actionMessage = action.message ?: "评论提交失败",
            )
        } else {
            forumPostDetailWithImmediateReply(state, action.reply).copy(
                commentDraft = "",
                replyingToCommentId = null,
                replyingToName = null,
                commentClientRequestId = null,
                actionLoading = false,
                actionMessage = action.message ?: "评论已提交",
            )
        }
    },
    onFailure = {
        state.copy(
            actionLoading = false,
            actionMessage = "评论提交失败：${it.message ?: "未知错误"}",
        )
    },
)

data class ForumCreateState(
    val draft: ForumCreateDraft = ForumCreateDraft(),
    val isAdmin: Boolean = false,
    val accessMessage: String? = null,
    val submitting: Boolean = false,
    val actionMessage: String? = null
)

data class BookDetailState(
    val bookId: Long = 0,
    val book: LoadResult<NovelCard> = LoadResult.Idle,
    val chapters: LoadResult<List<Chapter>> = LoadResult.Idle,
    val comments: LoadResult<List<ChapterComment>> = LoadResult.Idle,
    /** Source [bookid:...] markers resolved for the current book-review tree. */
    val bookReferences: Map<Long, LoadResult<NovelCard>> = emptyMap(),
    val favoriteStatus: LoadResult<FavoriteStatus> = LoadResult.Idle,
    val managementPermissions: LoadResult<BookEditPermissions> = LoadResult.Idle,
    val readerProgress: ReaderProgress? = null,
    val commentDraft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
    val favoriteLoading: Boolean = false,
    val actionMessage: String? = null,
    val actionLoading: Boolean = false,
    val requestNewChapterLoading: Boolean = false,
)

enum class NativeBookDownloadFormat {
    Epub,
    Txt,
}

enum class NativeDownloadReplacementMode {
    Source,
    EffectiveReaderRules,
}

data class NativeEpubDownloadState(
    val bookId: Long = 0,
    val format: NativeBookDownloadFormat? = null,
    val replacementMode: NativeDownloadReplacementMode = NativeDownloadReplacementMode.Source,
    val busy: Boolean = false,
    val paused: Boolean = false,
    val progress: NativeEpubExportProgress? = null,
    val message: String? = null,
    /** A failed/cancelled job can be retried from the same native detail page. */
    val canRetry: Boolean = false,
    val completedUri: String? = null,
)

/** Global because a card can request its original before a detail route has been opened. */
data class ImagePreviewState(
    val title: String = "",
    val displayUrl: String? = null,
    val originalUrl: String? = null,
    val loading: Boolean = false,
)

/** A paged source glossary; never inflate all terminology entries into a regular Column. */
data class TerminologyState(
    val bookId: Long = 0,
    val keyword: String = "",
    val entries: LoadResult<List<TerminologyEntry>> = LoadResult.Idle,
    val page: TerminologyPage? = null,
    val loadingMore: Boolean = false,
    val loadMoreError: String? = null,
)

data class BookEditState(
    val bookId: Long = 0,
    val info: LoadResult<BookEditInfo> = LoadResult.Idle,
    val permissions: LoadResult<BookEditPermissions> = LoadResult.Idle,
    val draft: BookEditDraft = BookEditDraft(),
    val accessPolicyDraft: BookAccessPolicyDraft = BookAccessPolicyDraft(),
    val transferIdentifier: String = "",
    val saving: Boolean = false,
    val uploadingCover: Boolean = false,
    val savingAccessPolicy: Boolean = false,
    val transferringBook: Boolean = false,
    val actionMessage: String? = null
)

data class BookChapterManagerState(
    val bookId: Long = 0,
    val chapters: LoadResult<List<Chapter>> = LoadResult.Idle,
    val selectedIds: Set<Long> = emptySet(),
    val orderDirty: Boolean = false,
    val editor: ManagedChapterDraft? = null,
    val editorLoading: Boolean = false,
    val actionLoading: Boolean = false,
    val translationMode: String = "shared",
    val illustrationChapter: Chapter? = null,
    val illustrations: LoadResult<ChapterIllustrationPage> = LoadResult.Idle,
    val uploadingIllustrations: Boolean = false,
    val deletingIllustrationId: Long? = null,
    val actionMessage: String? = null
)

data class ReaderChapterCommentState(
    val comments: LoadResult<List<ChapterComment>> = LoadResult.Idle,
    /** Source [bookid:...] markers resolved for this chapter's inline comment tree. */
    val bookReferences: Map<Long, LoadResult<NovelCard>> = emptyMap(),
    val draft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
    val actionMessage: String? = null,
    val actionLoading: Boolean = false,
)

data class ReaderState(
    val bookId: Long = 0,
    /** The work title shown in the reader header; chapter progress is reserved for the footer. */
    val bookTitle: String? = null,
    val chapterId: Long = 0,
    /** One-shot viewport request consumed by ReaderScreen after this chapter body composes. */
    val entryPosition: ReaderChapterEntryPosition = ReaderChapterEntryPosition.Start,
    /** One-shot local same-chapter anchor consumed after the actual body has composed. */
    val restoreViewportAnchor: ReaderViewportAnchor? = null,
    val content: LoadResult<ReaderContent> = LoadResult.Idle,
    val chapterContents: List<ReaderChapterContent> = emptyList(),
    val chapters: LoadResult<List<Chapter>> = LoadResult.Idle,
    val comments: LoadResult<List<ChapterComment>> = LoadResult.Idle,
    /** Continuous reading keeps one independent comment panel state per rendered chapter. */
    val chapterCommentStates: Map<Long, ReaderChapterCommentState> = emptyMap(),
    val favoriteStatus: LoadResult<FavoriteStatus> = LoadResult.Idle,
    val favoriteLoading: Boolean = false,
    val loadingNextChapter: Boolean = false,
    val loadingPreviousChapter: Boolean = false,
    val previousChapterError: String? = null,
    val visibleChapterId: Long? = null,
    val nextChapterError: String? = null,
    /** True when a successful catalog did not contain enough data to identify the next chapter. */
    val nextChapterWaitingForCatalog: Boolean = false,
    /**
     * A short catalog response cannot prove that the last visible entry is the final chapter.
     * This flag forces one fresh catalog read before exposing the terminal reader state.
     */
    val nextChapterEndConfirmationRequested: Boolean = false,
    /** Set after the catalog confirms that the continuous window has reached its final chapter. */
    val nextChapterExhausted: Boolean = false,
    val commentDraft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
    val actionMessage: String? = null,
    val actionLoading: Boolean = false,
    /** Real local chapter-cache state for the reader directory; never inferred from a remote list. */
    val chapterCacheStates: Map<Long, ReaderChapterCacheState> = emptyMap(),
    /** True only while the current body is being served from disk after a transport failure. */
    val contentFromCache: Boolean = false,
    /** Local-only action state for the reader settings' targeted cache cleanup. */
    val clearingChapterCache: Boolean = false,
    val chapterCacheActionMessage: String? = null,
)

enum class SearchViewMode {
    Grid,
    List
}

data class SearchOptions(
    val sortBy: String = "favorite_count",
    val sortOrder: String = "desc",
    val scope: String = "all",
    val matchType: String = "fuzzy_strict",
    /** Matches the live source's initial all-content search request. */
    val adultFilter: String = "all",
    val source: String = "",
    val wordCountRange: String = "",
    val requiredTags: List<String> = emptyList(),
    val blockedTags: List<String> = emptyList(),
    val advancedSyntaxEnabled: Boolean = false,
    val viewMode: SearchViewMode = SearchViewMode.Grid,
    /** Local source-compatible policy: retain search preferences across launches or not. */
    val cacheEnabled: Boolean = true
)

data class ReaderUiOptions(
    val fontSizeSp: Int = ReaderSettingsStore.DEFAULT_FONT_SIZE_SP,
    val lineHeight: Float = ReaderSettingsStore.DEFAULT_LINE_HEIGHT,
    val fontFamily: String = ReaderSettingsStore.DEFAULT_FONT_FAMILY,
    val fontWeight: Int = ReaderSettingsStore.DEFAULT_FONT_WEIGHT,
    val letterSpacing: Float = ReaderSettingsStore.DEFAULT_LETTER_SPACING,
    val wordSpacing: Float = ReaderSettingsStore.DEFAULT_WORD_SPACING,
    val theme: String = ReaderSettingsStore.DEFAULT_THEME,
    val customThemes: List<ReaderCustomTheme> = emptyList(),
    val emptyLine: Boolean = true,
    val textIndent: Boolean = true,
    val removeDuplicateLines: Boolean = false,
    val showComments: Boolean = true,
    val showImages: Boolean = true,
    val showTts: Boolean = true,
    val showRadialMenu: Boolean = false,
    val radialMenuOpenMode: String = "doubleTap",
    val showHeader: Boolean = true,
    val showFooter: Boolean = true,
    val showFavoriteButton: Boolean = true,
    val screenPaddingTopDp: Int = 0,
    val screenPaddingBottomDp: Int = 0,
    val contentWidthDp: Int = ReaderSettingsStore.DEFAULT_CONTENT_WIDTH_DP,
    val replaceMode: String = ReaderSettingsStore.DEFAULT_REPLACE_MODE,
    val useInfiniteScroll: Boolean = true,
    val pageTurnMode: Boolean = false,
    val pageTurnEffect: String = "fade",
    val volumeKeyPageTurn: Boolean = true,
    val tapAreas: List<ReaderTapArea> = defaultReaderTapAreas(),
)

internal fun defaultReaderTapAreas(): List<ReaderTapArea> = listOf(
    ReaderTapArea("left", "30%", "pagePrev"),
    ReaderTapArea("center", "40%", "sidebar"),
    ReaderTapArea("right", "30%", "pageNext"),
)

internal fun ReaderSettingsValues.toReaderUiOptions(): ReaderUiOptions = ReaderUiOptions(
    fontSizeSp = fontSizeSp,
    lineHeight = lineHeight,
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing,
    wordSpacing = wordSpacing,
    theme = theme,
    customThemes = customThemes,
    emptyLine = emptyLine,
    textIndent = textIndent,
    removeDuplicateLines = removeDuplicateLines,
    showComments = showComments,
    showImages = showImages,
    showTts = showTts,
    showRadialMenu = showRadialMenu,
    radialMenuOpenMode = radialMenuOpenMode,
    showHeader = showHeader,
    showFooter = showFooter,
    showFavoriteButton = showFavoriteButton,
    screenPaddingTopDp = screenPaddingTopDp,
    screenPaddingBottomDp = screenPaddingBottomDp,
    contentWidthDp = contentWidthDp,
    replaceMode = replaceMode,
    useInfiniteScroll = useInfiniteScroll,
    pageTurnMode = pageTurnMode,
    pageTurnEffect = pageTurnEffect,
    volumeKeyPageTurn = volumeKeyPageTurn,
    tapAreas = tapAreas.ifEmpty { defaultReaderTapAreas() },
)

internal fun ReaderUiOptions.toReaderSettingsValues(): ReaderSettingsValues = ReaderSettingsValues(
    fontSizeSp = fontSizeSp,
    lineHeight = lineHeight,
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing,
    wordSpacing = wordSpacing,
    theme = theme,
    customThemes = customThemes,
    emptyLine = emptyLine,
    textIndent = textIndent,
    removeDuplicateLines = removeDuplicateLines,
    showComments = showComments,
    showImages = showImages,
    showTts = showTts,
    showRadialMenu = showRadialMenu,
    radialMenuOpenMode = radialMenuOpenMode,
    showHeader = showHeader,
    showFooter = showFooter,
    showFavoriteButton = showFavoriteButton,
    screenPaddingTopDp = screenPaddingTopDp,
    screenPaddingBottomDp = screenPaddingBottomDp,
    contentWidthDp = contentWidthDp,
    replaceMode = replaceMode,
    useInfiniteScroll = useInfiniteScroll,
    pageTurnMode = pageTurnMode,
    pageTurnEffect = pageTurnEffect,
    volumeKeyPageTurn = volumeKeyPageTurn,
    tapAreas = tapAreas,
)

internal fun ReaderUiOptions.normalizedReaderOptions(): ReaderUiOptions {
    val normalizedCustomThemes = com.novalpie.nativeapp.model.normalizeReaderCustomThemes(customThemes)
    val normalizedTheme = theme.takeIf {
        it in setOf("system", "light", "sepia", "green", "gray", "dark", "high_contrast") ||
            com.novalpie.nativeapp.model.readerCustomThemeIdFromKey(it)
                ?.let { id -> normalizedCustomThemes.any { customTheme -> customTheme.id == id } } == true
    } ?: ReaderSettingsStore.DEFAULT_THEME
    return copy(
        fontSizeSp = fontSizeSp.coerceIn(ReaderSettingsStore.MIN_FONT_SIZE_SP, ReaderSettingsStore.MAX_FONT_SIZE_SP),
        lineHeight = lineHeight.coerceIn(ReaderSettingsStore.MIN_LINE_HEIGHT, ReaderSettingsStore.MAX_LINE_HEIGHT),
        fontFamily = fontFamily.takeIf(ReaderFontStore::isSupportedFamily)
            ?: ReaderSettingsStore.DEFAULT_FONT_FAMILY,
        fontWeight = fontWeight.coerceIn(ReaderSettingsStore.MIN_FONT_WEIGHT, ReaderSettingsStore.MAX_FONT_WEIGHT),
        letterSpacing = letterSpacing.coerceIn(ReaderSettingsStore.MIN_LETTER_SPACING, ReaderSettingsStore.MAX_LETTER_SPACING),
        wordSpacing = wordSpacing.coerceIn(ReaderSettingsStore.MIN_WORD_SPACING, ReaderSettingsStore.MAX_WORD_SPACING),
        customThemes = normalizedCustomThemes,
        theme = normalizedTheme,
        radialMenuOpenMode = radialMenuOpenMode.takeIf { it == "doubleTap" || it == "longPress" } ?: "doubleTap",
        screenPaddingTopDp = screenPaddingTopDp.coerceIn(0, ReaderSettingsStore.MAX_SCREEN_PADDING_DP),
        screenPaddingBottomDp = screenPaddingBottomDp.coerceIn(0, ReaderSettingsStore.MAX_SCREEN_PADDING_DP),
        contentWidthDp = contentWidthDp.coerceIn(ReaderSettingsStore.MIN_CONTENT_WIDTH_DP, ReaderSettingsStore.MAX_CONTENT_WIDTH_DP),
        replaceMode = replaceMode.takeIf { it in READER_REPLACE_MODES } ?: ReaderSettingsStore.DEFAULT_REPLACE_MODE,
        // Prefer the user's explicit continuous-scroll switch when an old saved preference contains
        // both modes. The settings UI also writes them as mutually exclusive values.
        pageTurnMode = pageTurnMode && !useInfiniteScroll,
        pageTurnEffect = pageTurnEffect.takeIf { it in setOf("none", "fade", "cover", "slide", "simulated") } ?: "fade",
        tapAreas = tapAreas.takeIf { it.size == 3 } ?: defaultReaderTapAreas(),
    )
}

private val READER_REPLACE_MODES = setOf(
    "", "korea", "india", "europe", "usa", "hyrule", "azeroth", "tamriel",
    "middle_earth", "terra", "genshin"
)

class NovalPieViewModel(application: Application) : AndroidViewModel(application) {
    private val networkConfigStore = NetworkConfigStore(application)
    private val authSessionStore = AuthSessionStore(application)
    private val readerProgressStore = ReaderProgressStore(application)
    private val readerChapterCacheStore = ReaderChapterCacheStore(application)
    private val readerSessionStore = ReaderSessionStore(application)
    private val readerSettingsStore = ReaderSettingsStore(application)
    private val readerTtsSettingsStore = ReaderTtsSettingsStore(application)
    private val readerReplacementRulesStore = ReaderReplacementRulesStore(application)
    private val appThemeSettingsStore = AppThemeSettingsStore(application)
    private val chineseVariantSettingsStore = ChineseVariantSettingsStore(application)
    private val searchHistoryStore = SearchHistoryStore(application)
    private val searchSettingsStore = SearchSettingsStore(application)
    private val favoritesSettingsStore = FavoritesSettingsStore(application)
    private val profileBooksSettingsStore = ProfileBooksSettingsStore(application)
    private val downloadSettingsStore = DownloadSettingsStore(application)
    private val workspaceLocalStore = WorkspaceLocalStore(application)
    private val editorArchiveStore = EditorArchiveStore(application)
    private val editorDocumentHistory = EditorDocumentHistory()

    var proxySettings by mutableStateOf(networkConfigStore.loadProxySettings())
        private set
    var proxyEnabled by mutableStateOf(proxySettings.enabled)
        private set
    var proxyHost by mutableStateOf(proxySettings.host)
        private set
    var proxyPortText by mutableStateOf(proxySettings.port.toString())
        private set
    var authToken by mutableStateOf(authSessionStore.loadToken())
        private set

    private val dependencies = AppContainer.from(application).also { it.refreshEnvironmentFromStores() }
    private val api: NovalPieApi = dependencies.api

    private val startupReaderSession: ReaderSession? = readerSessionStore.load()
    private val searchFeature = SearchViewModel(
        WebsiteSearchRepository(api),StoredSearchPreferences(searchSettingsStore,searchHistoryStore),
    )
    private val navigator = AppNavigator(readerSessionRouteStack(startupReaderSession))
    private val routes:List<AppRoute> get() = navigator.entries
    private val forumFeature=com.novalpie.nativeapp.feature.forum.ForumFeedViewModel(dependencies.forumFeedRepository)
    private val forumPostFeature = com.novalpie.nativeapp.feature.forum.ForumPostViewModel(dependencies.forumPostRepository)
    private val bookFeature=com.novalpie.nativeapp.feature.books.BookDetailViewModel(
        dependencies.bookRepository,progress=readerProgressStore::load,
        onComments={bookId,comments->loadBookCommentBookReferences(bookId,comments,bookDetailRequestSerial)},
    )
    private val bookDetailRequestSerial:Long get()=bookFeature.revision
    private var terminologyRequestSerial = 0L
    private var bookEditRequestSerial = 0L
    private var bookChapterRequestSerial = 0L
    private val publicProfileFeature = com.novalpie.nativeapp.feature.profile.PublicProfileViewModel(dependencies.publicProfileRepository)
    private val adminFeature = com.novalpie.nativeapp.feature.admin.AdminViewModel(dependencies.adminRepository, { isAdminProfile(currentUserProfile()) })
    private var toolsRequestSerial = 0L
    private val messageInboxFeature = com.novalpie.nativeapp.feature.messages.MessageInboxViewModel(dependencies.messagesRepository)
    private val conversationFeature = com.novalpie.nativeapp.feature.messages.ConversationViewModel(dependencies.messagesRepository)
    private val messageSettingsFeature = com.novalpie.nativeapp.feature.messages.MessageSettingsViewModel(dependencies.messagesRepository)
    private val messageDetailFeature = com.novalpie.nativeapp.feature.messages.MessageDetailViewModel(
        dependencies.messagesRepository,
        onDeleted = { id -> if ((currentRoute as? AppRoute.MessageDetail)?.messageId == id) goBack() },
        onChanged = { if (messageCenterState.messages !is LoadResult.Idle) messageInboxFeature.refresh() },
    )
    private val uploadFeature = com.novalpie.nativeapp.feature.upload.UploadBookViewModel(
        com.novalpie.nativeapp.feature.upload.WebsiteUploadRepository(api, ::readUploadDocument, { uploadSource(it) }),
        drafts = dependencies.uploadDrafts,
        accountId = { authToken?.let { decodeAuthTokenProfile(it, nowEpochSeconds = 0)?.id } },
    )
    private var editorRequestSerial = 0L
    private var editorProcessorRequestSerial = 0L
    private var authRequestSerial = 0L
    private var readerRequestSerial = 0L
    private var readerCatalogRequestSerial = 0L
    private var imagePreviewRequestSerial = 0L
    /** One in-flight source detail lookup prevents home refreshes from duplicating legacy repair. */
    private var readerProgressTitleLookupBookId: Long? = null
    private val initialFavoritesSettings = favoritesSettingsStore.load()
    private val initialProfileBooksSettings = profileBooksSettingsStore.load()
    private val initialDownloadSettings = downloadSettingsStore.load()
    private var selectedFavoriteGroupId: Long? = initialFavoritesSettings.selectedDisplayGroupId
    // Pagination belongs to the active shelf session, never to a cold launch.  Keep this explicit
    // here as a second guard for installations that still hold an old persisted page preference.
    private var favoritesUiOptions = initialFavoritesSettings.toFavoritesUiOptions().copy(currentPage = 1)
    private val libraryFeature=com.novalpie.nativeapp.feature.library.LibraryContentViewModel(
        repository=dependencies.libraryRepository,
        reconcile={entries->
            reconcileRemoteReaderProgress(entries)
            if(readerProgressStore.backfillCompletedFavoriteCatalogues(entries)>0) {
                readerProgress=readerProgressStore.load()
                recentReaderProgresses=readerProgressStore.loadRecent(READER_PROGRESS_HISTORY_LIMIT)
            }
            favoriteEntriesWithLocalReaderProgress(entries,recentReaderProgresses)
        },
        onUser=::publishHomeUserProfile,
        onLoaded=::onLibraryPageLoaded,
    )

    var currentTab by mutableStateOf(BottomTab.Collection)
        private set
    val forumState:ForumState get()=forumFeature.state.feed
    internal val forumScrollPosition:GridScrollPosition get()=forumFeature.state.scroll
    val forumPostDetailState: ForumPostDetailState get() = forumPostFeature.state
    var forumCreateState by mutableStateOf(ForumCreateState())
        private set
    var homeState:HomeState
        get()=libraryFeature.state
        private set(value){libraryFeature.present{value}}
    private val profileFeature = com.novalpie.nativeapp.feature.profile.ProfileViewModel(
        dependencies.profileRepository,
        ProfileState(
            booksGridColumns = initialProfileBooksSettings.gridColumns,
            downloadImageConcurrency = initialDownloadSettings.imageConcurrency,
        ), onProfile = ::publishHomeUserProfile,
    )
    var profileState: ProfileState
        get() = profileFeature.state
        private set(value) { profileFeature.present { value } }
    var userProfileDetailState: UserProfileDetailState
        get() = publicProfileFeature.state
        private set(value) { publicProfileFeature.present { value } }
    var adminState: AdminState
        get() = adminFeature.state
        private set(value) { adminFeature.present { value } }
    var toolsState by mutableStateOf(ToolsState())
        private set
    val messageCenterState: MessageCenterState get() = messageInboxFeature.state
    val messageDetailState: MessageDetailState get() = messageDetailFeature.state
    val messageConversationState: MessageConversationState get() = conversationFeature.state
    val messageSettingsState: MessageSettingsState get() = messageSettingsFeature.state
    private val workspaceFeature = com.novalpie.nativeapp.feature.workspace.WorkspaceViewModel(
        dependencies.workspaceRepository, com.novalpie.nativeapp.feature.workspace.StoredWorkspaceLocalRepository(workspaceLocalStore),
    )
    var workspaceState: WorkspaceState
        get() = workspaceFeature.state
        private set(value) { workspaceFeature.present { value } }
    val uploadBookState: UploadBookState get() = uploadFeature.state
    var uploadEditorState by mutableStateOf(UploadEditorState(archives = editorArchiveStore.list()))
        private set
    var politicalExamState by mutableStateOf(PoliticalExamState())
        private set
    var authState by mutableStateOf(AuthState())
        private set
    var bookshelfQuery by mutableStateOf(initialFavoritesSettings.searchQuery)
        private set
    internal var homeGridScrollPosition by mutableStateOf(GridScrollPosition())
        private set
    // History is an explicit choice below the field. Pre-filling the last value made a new
    // search session look active even though no request had run for that text.
    val searchKeyword get() = searchFeature.state.keyword
    val searchHistory get() = searchFeature.state.history
    val searchOptions get() = searchFeature.state.options
    val searchResults get() = searchFeature.state.results
    val searchTags get() = searchFeature.state.tags
    val searchPage get() = searchFeature.state.page
    /** Last successful source search envelope, used for source-style direct page navigation. */
    val searchResultPage get() = searchFeature.state.envelope
    val searchCanLoadMore get() = searchFeature.state.canLoadMore
    val searchLoadingMore get() = searchFeature.state.loadingPage

    /** See [HomeState.favoritesLoadMoreError]; search had the identical defect. */
    val searchLoadMoreError get() = searchFeature.state.pageError
    internal val searchGridScrollPosition get() = searchFeature.state.scroll
    var bookCatalogQuery by mutableStateOf("")
        private set
    var bookDetailState:BookDetailState
        get()=bookFeature.state
        private set(value){bookFeature.present{value}}
    var nativeEpubDownloadState by mutableStateOf(NativeEpubDownloadState())
        private set
    var imagePreviewState by mutableStateOf(ImagePreviewState())
        private set
    var terminologyState by mutableStateOf(TerminologyState())
        private set
    var bookEditState by mutableStateOf(BookEditState())
        private set
    var bookChapterManagerState by mutableStateOf(BookChapterManagerState())
        private set
    var readerCatalogQuery by mutableStateOf("")
        private set
    var readerState by mutableStateOf(ReaderState())
        private set
    var readerUiOptions by mutableStateOf(
        readerSettingsStore.load().toReaderUiOptions()
    )
        private set
    /** Transient window state shared with the root Scaffold so fullscreen removes stale insets. */
    var readerFullscreen by mutableStateOf(false)
        private set
    var readerTtsSettings by mutableStateOf(readerTtsSettingsStore.load())
        private set
    var readerReplacementState by mutableStateOf(ReaderReplacementState())
        private set
    /** Tracks a local rule while its first remote glossary row is being created. */
    private val pendingReaderReplacementCreates = mutableSetOf<String>()
    private val deletedPendingReaderReplacementCreates = mutableSetOf<String>()
    private val pendingReaderReplacementDeletes = mutableSetOf<Pair<Long, String>>()
    private val replacementWriter = com.novalpie.nativeapp.feature.reader.replacement.ReplacementRemoteWriter(dependencies.replacementRemoteRepository)
    private val replacementWriteLocks = mutableMapOf<Pair<Long, String>, Mutex>()
    private var replacementLoadRevision = 0L
    var appThemeMode by mutableStateOf(appThemeSettingsStore.loadMode())
        private set
    var chineseVariant by mutableStateOf(chineseVariantSettingsStore.loadVariant())
        private set
    var readerProgress by mutableStateOf(readerProgressStore.load())
        private set
    var recentReaderProgresses by mutableStateOf(
        readerProgressStore.loadRecent(limit = READER_PROGRESS_HISTORY_LIMIT)
    )
        private set
    private var readerProgressRevision = 0L
    private var syncedCollectionProgressRevision = 0L

    val currentRoute: AppRoute get() = routes.lastOrNull() ?: AppRoute.Home
    /** A search opened from a detail page is a child route, so Android Back must return there. */
    val canNavigateBack: Boolean get() = routes.size > 1

    init {
        configureNovalPieImageLoader(application, proxySettings)
        viewModelScope.launch {
            readerProgressStore.changes().collect {
                val latest=withContext(Dispatchers.IO){readerProgressStore.load() to readerProgressStore.loadRecent(READER_PROGRESS_HISTORY_LIMIT)}
                if(latest.first!=readerProgress||latest.second!=recentReaderProgresses) {
                    readerProgress=latest.first;recentReaderProgresses=latest.second
                    readerProgressRevision++
                    updateLoadedCollectionProgress()
                    if(bookDetailState.bookId>0)bookDetailState=bookDetailState.copy(readerProgress=readerProgressStore.load(bookDetailState.bookId))
                }
            }
        }
        viewModelScope.launch {
            snapshotFlow {currentRoute==AppRoute.Search}.collect {visible->
                if(visible)searchFeature.enter() else searchFeature.leave()
            }
        }
        viewModelScope.launch {
            snapshotFlow {currentRoute==AppRoute.Forum}.collect {visible->
                if(visible)forumFeature.enter()else forumFeature.leave()
            }
        }
        viewModelScope.launch {
            var previous=authToken to proxySettings
            snapshotFlow {authToken to proxySettings}.collect {next->
                if(next!=previous){previous=next;searchFeature.environmentChanged();forumFeature.environmentChanged();libraryFeature.environmentChanged();bookFeature.environmentChanged()
                    messageInboxFeature.environmentChanged();messageDetailFeature.environmentChanged();conversationFeature.environmentChanged();messageSettingsFeature.environmentChanged()
                    profileFeature.environmentChanged()
                    publicProfileFeature.environmentChanged()
                    workspaceFeature.environmentChanged()
                    forumPostFeature.environmentChanged()
                    uploadFeature.environmentChanged()
                    adminFeature.environmentChanged()
                    nativeEpubDownloadState = NativeEpubDownloadState()
                    replacementLoadRevision++;pendingReaderReplacementCreates.clear();deletedPendingReaderReplacementCreates.clear()
                    readerReplacementState = ReaderReplacementState()
                    when(val route=currentRoute){
                        AppRoute.Home->loadHome()
                        is AppRoute.BookDetail->loadBookDetail(route.bookId)
                        is AppRoute.ForumPostDetail->loadForumPostDetail(route.postId)
                        AppRoute.MessageCenter->loadMessageCenter()
                        is AppRoute.MessageDetail->loadMessageDetail(route.messageId)
                        is AppRoute.MessageConversation->loadMessageConversation(route.targetUserId,route.targetName)
                        AppRoute.MessageSettings->loadMessageSettings()
                        AppRoute.Profile->loadProfile()
                        AppRoute.Workspace->loadWorkspace()
                        is AppRoute.UserProfileDetail->loadUserProfile(route.userId)
                        is AppRoute.Reader->loadReaderReplacementRules(route.bookId)
                        else->Unit
                    }}
            }
        }
        viewModelScope.launch {
            AppContainer.from(application).downloads.state.collect { state ->
                val task=state.task ?: return@collect
                val currentAccount = authToken?.let(::decodeAuthTokenProfile)?.id
                if (currentAccount == null || task.accountId != currentAccount) {
                    nativeEpubDownloadState = NativeEpubDownloadState()
                    return@collect
                }
                nativeEpubDownloadState = com.novalpie.nativeapp.feature.download.nativeDownloadStateFromTask(task, state)
            }
        }
        viewModelScope.launch {
            val account=authToken?.let(::decodeAuthTokenProfile)?.id
            if(account!=null) {
                val container=AppContainer.from(application)
                val recovered=withContext(Dispatchers.IO){container.downloadStore.recover(account)}
                recovered.tasks.filter {it.phase!=DownloadPhase.Completed}.maxByOrNull {it.updatedAt}?.let(container.downloads::restore)
            }
        }
        // The app opens on Collection. Loading an unseen forum feed here competes with the
        // authenticated shelf requests through the same proxy/CDN route; Forum loads on tab or
        // deep-link entry instead.
        startupReaderSession?.let { session ->
            loadReader(session.bookId, session.chapterId, restoreViewport = true)
        } ?: loadHome()
    }

    fun updateBookshelfQuery(value: String) {
        if (bookshelfQuery == value) return
        bookshelfQuery = value
        resetFavoritesPage()
        saveFavoritesOptions()
    }

    fun selectFavoriteGroup(groupId: Long?) {
        if (selectedFavoriteGroupId == groupId) return
        selectedFavoriteGroupId = groupId
        resetFavoritesPage()
        saveFavoritesOptions()
        loadHome()
    }

    fun selectFavoritesContentTab(tab: FavoritesContentTab) {
        if (favoritesUiOptions.tab == tab) return
        favoritesUiOptions = favoritesUiOptions.copy(tab = tab)
        if (tab == FavoritesContentTab.History) selectedFavoriteGroupId = null
        resetFavoritesPage()
        saveFavoritesOptions()
        loadHome()
    }

    fun toggleFavoritesLayout() {
        resetHomeGridScrollPosition()
        favoritesUiOptions = favoritesUiOptions.copy(
            layout = if (favoritesUiOptions.layout == FavoritesLayout.Grid) FavoritesLayout.List else FavoritesLayout.Grid
        )
        saveFavoritesOptions()
        homeState = homeState.copy(options = favoritesUiOptions)
    }

    fun selectFavoritesGridColumns(columns: Int) {
        val normalized = com.novalpie.nativeapp.data.normalizeGridColumns(columns)
        if (favoritesUiOptions.gridColumns == normalized) return
        favoritesUiOptions = favoritesUiOptions.copy(gridColumns = normalized)
        resetHomeGridScrollPosition()
        saveFavoritesOptions()
        homeState = homeState.copy(options = favoritesUiOptions)
    }

    fun selectFavoritesDisplayMode(mode: FavoritesDisplayMode) {
        if (favoritesUiOptions.displayMode == mode) return
        favoritesUiOptions = favoritesUiOptions.copy(displayMode = mode)
        if (mode != FavoritesDisplayMode.Default) selectedFavoriteGroupId = null
        resetFavoritesPage()
        saveFavoritesOptions()
        loadHome()
    }

    /** Cycles the same local-only cache policy as the source favourites toolbar. */
    fun cycleFavoritesCacheMode() {
        favoritesUiOptions = favoritesUiOptions.copy(cacheMode = favoritesUiOptions.cacheMode.next())
        saveFavoritesOptions()
        homeState = homeState.copy(
            options = favoritesUiOptions,
            actionMessage = "收藏缓存：${favoritesCacheModeLabel(favoritesUiOptions.cacheMode)}"
        )
    }

    /** Clears cached UI state only; remote favourites and the cache policy remain intact. */
    fun clearFavoritesCache() {
        if (homeState.actionLoading) return
        favoritesSettingsStore.clearCachedPresentationValues()
        favoritesUiOptions = FavoritesUiOptions(cacheMode = favoritesUiOptions.cacheMode)
        selectedFavoriteGroupId = null
        bookshelfQuery = ""
        loadHome("已清除收藏缓存")
    }

    fun clearFavoriteImageCache() {
        if (homeState.actionLoading) return
        homeState = homeState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    clearNovalPieImageCaches(getApplication<Application>())
                }
            }
            homeState = result.fold(
                onSuccess = {
                    homeState.copy(
                        actionLoading = false,
                        actionMessage = "已清除本地图片缓存"
                    )
                },
                onFailure = { failure ->
                    homeState.copy(
                        actionLoading = false,
                        actionMessage = "清除本地图片缓存失败：${failure.message ?: "未知错误"}"
                    )
                }
            )
        }
    }

    fun updateFavoritesSort(field: String? = null, order: String? = null) {
        val nextField = field?.takeIf { it in FAVORITES_SORT_FIELDS } ?: favoritesUiOptions.sortField
        val nextOrder = order?.lowercase()?.takeIf { it in FAVORITES_SORT_ORDERS } ?: favoritesUiOptions.sortOrder
        if (favoritesUiOptions.sortField == nextField && favoritesUiOptions.sortOrder == nextOrder) return
        favoritesUiOptions = favoritesUiOptions.copy(sortField = nextField, sortOrder = nextOrder, currentPage = 1)
        resetHomeGridScrollPosition()
        saveFavoritesOptions()
        loadHome()
    }

    fun toggleFavoritesSelectionMode() {
        homeState = homeState.copy(
            selectionMode = !homeState.selectionMode,
            selectedBookIds = emptySet(),
            actionMessage = null
        )
    }

    fun toggleFavoritesBookSelection(bookId: Long) {
        if (!homeState.selectionMode) return
        val next = homeState.selectedBookIds.toMutableSet()
        if (!next.add(bookId)) next.remove(bookId)
        homeState = homeState.copy(selectedBookIds = next)
    }

    /** Starts or extends collection bulk management from a stationary card long press. */
    fun longPressFavoritesBook(bookId: Long) {
        if (homeState.actionLoading) return
        val selection = favoriteLongPressSelection(
            selectionMode = homeState.selectionMode,
            selectedBookIds = homeState.selectedBookIds,
            bookId = bookId,
        )
        homeState = homeState.copy(
            selectionMode = selection.selectionMode,
            selectedBookIds = selection.selectedBookIds,
            actionMessage = null,
        )
    }

    fun createFavoriteGroup(name: String) {
        runFavoritesMutation("已创建分组") { api.createFavoriteGroup(name) }
    }

    fun renameFavoriteGroup(groupId: Long, name: String) {
        runFavoritesMutation("已更新分组") { api.renameFavoriteGroup(groupId, name) }
    }

    fun deleteFavoriteGroup(groupId: Long) {
        runFavoritesMutation("已删除分组") {
            api.deleteFavoriteGroup(groupId)
            if (selectedFavoriteGroupId == groupId) selectedFavoriteGroupId = null
        }
    }

    fun moveSelectedFavoritesToGroup(groupId: Long?) {
        val entries = selectedFavoriteEntries()
        if (entries.isEmpty()) {
            homeState = homeState.copy(actionMessage = "请先选择收藏")
            return
        }
        runFavoritesMutation("已移动 ${entries.size} 本收藏") {
            entries.forEach { entry ->
                entry.favoriteId?.let { api.moveFavoriteToGroup(it, groupId) }
            }
        }
    }

    fun removeSelectedFavorites() {
        val entries = selectedFavoriteEntries()
        if (entries.isEmpty()) {
            homeState = homeState.copy(actionMessage = "请先选择收藏")
            return
        }
        runFavoritesMutation("已移除 ${entries.size} 本收藏") {
            entries.forEach { entry ->
                entry.favoriteId?.let { favoriteId -> api.removeFavorite(favoriteId) }
            }
        }
    }

    fun setFavoritePinned(entry: FavoriteEntry, isPinned: Boolean) {
        val favoriteId = entry.favoriteId ?: run {
            homeState = homeState.copy(actionMessage = "该收藏缺少可管理记录 ID")
            return
        }
        runFavoritesMutation(if (isPinned) "已置顶收藏" else "已取消置顶") {
            api.setFavoritePinned(favoriteId, isPinned)
        }
    }

    fun deleteSelectedReadingHistory() {
        val bookIds = homeState.selectedBookIds.toList()
        if (bookIds.isEmpty()) {
            homeState = homeState.copy(actionMessage = "请先选择阅读记录")
            return
        }
        runFavoritesMutation("已删除 ${bookIds.size} 条阅读记录") {
            api.deleteReadingHistory(bookIds)
        }
    }

    fun clearAllReadingHistory() {
        runFavoritesMutation("已清空阅读历史") { api.deleteReadingHistory(clearAll = true) }
    }

    fun updateSearchKeyword(value: String) {
        searchFeature.setVisible(currentRoute==AppRoute.Search)
        searchFeature.updateKeyword(value)
    }

    fun useSearchHistory(keyword: String) {
        performSearch(keyword)
    }

    fun useSearchTag(tagName: String) {
        applySearchTag(tagName, SearchTagFilterMode.Required)
    }

    /** Opens the source-equivalent author result route without discarding the current book detail. */
    fun openBookDetailAuthorSearch(author: String) {
        bookDetailAuthorSearchTarget(author)?.let(::openBookDetailSearch)
    }

    /** Opens the source-equivalent tag result route without discarding the current book detail. */
    fun openBookDetailTagSearch(tag: String) {
        bookDetailTagSearchTarget(tag)?.let(::openBookDetailSearch)
    }

    private fun openBookDetailSearch(target: BookDetailSearchTarget) {
        val stack = routes.toList()
        navigator.replaceAll(pushDistinctRoute(stack, AppRoute.Search))
        currentTab = BottomTab.Discover
        searchFeature.openTarget(target)
    }

    fun clearSearchHistory() {
        searchFeature.clearHistory()
    }

    /** Mirrors the source toolbar: keep the active filters, but opt in/out of retaining them locally. */
    fun toggleSearchSettingsCache() {
        searchFeature.toggleCache()
    }

    /** Clears local search-option state only; no source API request or search history is changed. */
    fun clearSearchSettingsCache() {
        searchFeature.setVisible(currentRoute==AppRoute.Search)
        searchFeature.clearOptions()
    }

    fun updateSearchSortBy(value: String) {
        updateSearchOptions(searchOptions.copy(sortBy=value))
    }

    fun updateSearchSortOrder(value: String) {
        updateSearchOptions(searchOptions.copy(sortOrder=value))
    }

    fun updateSearchScope(value: String) {
        updateSearchOptions(searchOptions.copy(scope=value))
    }

    fun updateSearchMatchType(value: String) {
        updateSearchOptions(searchOptionsAfterMatchTypeChange(searchOptions,value))
    }

    fun updateSearchAdultFilter(value: String) {
        updateSearchOptions(searchOptions.copy(adultFilter=value))
    }

    fun updateSearchSource(value: String) {
        updateSearchOptions(searchOptions.copy(source=value))
    }

    fun updateSearchWordCountRange(value: String) {
        updateSearchOptions(searchOptions.copy(wordCountRange=value))
    }

    fun updateSearchAdvancedSyntaxEnabled(value: Boolean) {
        updateSearchOptions(searchOptions.copy(advancedSyntaxEnabled=value))
    }

    /** Source search toggles its local view mode without rerunning or invalidating the query. */
    fun toggleSearchViewMode() {
        searchFeature.toggleViewMode()
    }

    fun applyRequiredSearchTag(tagName: String) {
        applySearchTag(tagName, SearchTagFilterMode.Required)
    }

    fun applyBlockedSearchTag(tagName: String) {
        applySearchTag(tagName, SearchTagFilterMode.Blocked)
    }

    fun removeSearchTag(tagName: String) {
        searchFeature.removeTag(tagName)
    }

    fun clearSearchTags() {
        searchFeature.clearTags()
    }

    private fun applySearchTag(tagName: String, mode: SearchTagFilterMode) {
        searchFeature.tag(tagName,mode)
    }

    private fun updateSearchOptions(options:SearchOptions) {
        searchFeature.updateOptions(options,refresh=currentRoute==AppRoute.Search)
    }

    fun updateBookCatalogQuery(value: String) {
        bookCatalogQuery = value
    }

    fun updateReaderCatalogQuery(value: String) {
        readerCatalogQuery = value
    }

    fun increaseReaderFont() {
        val next = (readerUiOptions.fontSizeSp + 1).coerceAtMost(ReaderSettingsStore.MAX_FONT_SIZE_SP)
        updateReaderOptions { it.copy(fontSizeSp = next) }
    }

    fun decreaseReaderFont() {
        val next = (readerUiOptions.fontSizeSp - 1).coerceAtLeast(ReaderSettingsStore.MIN_FONT_SIZE_SP)
        updateReaderOptions { it.copy(fontSizeSp = next) }
    }

    fun cycleReaderTheme() {
        val next = when (readerUiOptions.theme) {
            "system" -> "light"
            "light" -> "sepia"
            "sepia" -> "green"
            "green" -> "gray"
            "gray" -> "dark"
            "dark" -> "high_contrast"
            else -> "system"
        }
        updateReaderOptions { it.copy(theme = next) }
    }

    fun updateReaderOptions(transform: (ReaderUiOptions) -> ReaderUiOptions) {
        val previous = readerUiOptions
        val next = transform(readerUiOptions).normalizedReaderOptions()
        readerSettingsStore.save(next.toReaderSettingsValues())
        readerUiOptions = next
        val route = currentRoute as? AppRoute.Reader
        if (route != null && (previous.replaceMode != next.replaceMode || previous.showImages != next.showImages)) {
            loadReader(route.bookId, route.chapterId, preserveContinuousWindow = false)
        }
    }

    fun updateReaderFullscreen(value: Boolean) {
        readerFullscreen = value
    }

    fun setReaderOptions(value: ReaderUiOptions) {
        val previous = readerUiOptions
        val next = value.normalizedReaderOptions()
        readerSettingsStore.save(next.toReaderSettingsValues())
        readerUiOptions = next
        val route = currentRoute as? AppRoute.Reader
        if (route != null && (previous.replaceMode != next.replaceMode || previous.showImages != next.showImages)) {
            loadReader(route.bookId, route.chapterId, preserveContinuousWindow = false)
        }
    }

    fun resetReaderOptions() {
        readerSettingsStore.reset()
        readerUiOptions = readerSettingsStore.load().toReaderUiOptions()
    }

    /** Removes every on-disk chapter variant for the currently open book, never source data. */
    fun clearReaderChapterCache() {
        val route = currentRoute as? AppRoute.Reader ?: return
        val bookId = route.bookId
        val state = readerState
        if (bookId <= 0L || state.bookId != bookId || state.clearingChapterCache) return

        val requestSerial = readerRequestSerial
        readerState = state.copy(clearingChapterCache = true, chapterCacheActionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    readerChapterCacheStore.clearBook(bookId)
                }
            }
            if (
                requestSerial != readerRequestSerial ||
                currentRoute != route ||
                readerState.bookId != bookId
            ) return@launch

            val current = readerState
            readerState = result.fold(
                onSuccess = {
                    val knownChapterIds = (
                        ((current.chapters as? LoadResult.Success)?.value.orEmpty().map(Chapter::id)) +
                            current.chapterCacheStates.keys
                        ).toSet()
                    current.copy(
                        clearingChapterCache = false,
                        chapterCacheStates = knownChapterIds.associateWith { ReaderChapterCacheState.Missing },
                        contentFromCache = false,
                        chapterCacheActionMessage = "已清除本书离线缓存",
                    )
                },
                onFailure = { failure ->
                    current.copy(
                        clearingChapterCache = false,
                        chapterCacheActionMessage = "清除本书离线缓存失败：${failure.message ?: "未知错误"}",
                    )
                },
            )
        }
    }

    fun updateReaderTtsSettings(transform: (ReaderTtsSettings) -> ReaderTtsSettings) {
        val next = transform(readerTtsSettings)
        readerTtsSettingsStore.save(next)
        readerTtsSettings = readerTtsSettingsStore.load()
    }

    fun selectReaderReplacementSource(source: ReaderReplacementRuleSource) {
        val current = readerReplacementState
        if (current.source == source) return
        readerReplacementState = current.copy(
            source = source,
            actionMessage = if (source == ReaderReplacementRuleSource.All) {
                if (current.sharedRulesEnabled) "正在管理全部规则" else "公共规则当前未应用；可在本书开关中启用"
            } else "正在管理我的规则",
        )
    }

    fun restoreUnassignedReaderReplacementRules() {
        val bookId = readerReplacementState.novelId.takeIf { it > 0 } ?: return
        val count = readerReplacementRulesStore.importUnassignedLegacyRules(bookId)
        loadReaderReplacementRules(bookId)
        readerReplacementState = readerReplacementState.copy(actionMessage = "已恢复 $count 条旧规则为停用的本机规则；确认后可逐条启用")
    }

    /** Explicit per-book control: it governs reader, TTS, and captured download rule snapshots. */
    fun setReaderSharedRulesEnabled(enabled: Boolean) {
        val current = readerReplacementState
        if (current.novelId <= 0L || current.sharedRulesEnabled == enabled) return
        readerReplacementRulesStore.saveSharedRulesEnabledOverride(current.novelId, enabled)
        readerReplacementState = current.copy(
            source = if (enabled) ReaderReplacementRuleSource.All else ReaderReplacementRuleSource.Personal,
            sharedRulesEnabledOverride = enabled,
            revision = readerReplacementNextLocalRevision(
                currentRevision = current.revision,
                persistedRevision = readerReplacementRulesStore.revision(current.novelId),
            ),
            ttsRevision = current.ttsRevision + 1L,
            actionMessage = if (enabled) "本书已应用公共规则；可逐条关闭不需要的规则" else "本书现仅应用我的规则",
        )
    }

    /** Controls the default used only by books without their own explicit public-rule choice. */
    fun setDefaultReaderSharedRulesEnabled(enabled: Boolean) {
        val current = readerReplacementState
        if (current.defaultSharedRulesEnabled == enabled) return
        readerReplacementRulesStore.saveDefaultSharedRulesEnabled(enabled)
        val effectiveChanged = current.sharedRulesEnabledOverride == null && current.sharedRulesEnabled != enabled
        readerReplacementState = current.copy(
            source = if (effectiveChanged && enabled) ReaderReplacementRuleSource.All else current.source,
            defaultSharedRulesEnabled = enabled,
            revision = if (effectiveChanged) current.revision + 1L else current.revision,
            ttsRevision = if (effectiveChanged) current.ttsRevision + 1L else current.ttsRevision,
            actionMessage = if (enabled) "未单独设置的书籍将默认应用公共规则" else "未单独设置的书籍将默认只应用我的规则",
        )
    }

    /** Removes only this book's override so it follows the device-wide default again. */
    fun resetReaderSharedRulesOverride() {
        val current = readerReplacementState
        if (current.novelId <= 0L || current.sharedRulesEnabledOverride == null) return
        readerReplacementRulesStore.saveSharedRulesEnabledOverride(current.novelId, null)
        val effectiveEnabled = readerSharedRulesEnabled(
            defaultEnabled = current.defaultSharedRulesEnabled,
            bookOverride = null,
        )
        readerReplacementState = current.copy(
            source = if (effectiveEnabled) ReaderReplacementRuleSource.All else ReaderReplacementRuleSource.Personal,
            sharedRulesEnabledOverride = null,
            revision = current.revision + 1L,
            ttsRevision = current.ttsRevision + 1L,
            actionMessage = "本书已恢复跟随默认设置",
        )
    }

    fun saveReaderReplacementRule(rule: ReaderReplacementRule) {
        val state = readerReplacementState
        if (state.novelId <= 0L) return
        val normalized = rule.copy(
            novelId = state.novelId,
            owner = ReaderReplacementOwner.Personal,
        )
        val validation = validateReaderReplacementRule(normalized)
        if (!validation.isValid) {
            readerReplacementState = state.copy(actionMessage = validation.message ?: "替换规则无效")
            return
        }
        val previous = state.personalRules.firstOrNull { it.id == normalized.id }
        val syncAction = readerReplacementSaveSyncAction(previous = previous, saved = normalized)
        val nextRules = state.personalRules
            .filterNot { it.id == normalized.id }
            .plus(normalized)
            .sortedWith(compareBy<ReaderReplacementRule> { it.order }.thenBy { it.id })
        readerReplacementRulesStore.savePersonalRules(state.novelId, nextRules)
        readerReplacementState = state.copy(
            personalRules = nextRules,
            revision = readerReplacementNextLocalRevision(
                currentRevision = state.revision,
                persistedRevision = readerReplacementRulesStore.revision(state.novelId),
            ),
            ttsRevision = state.ttsRevision + 1L,
            actionMessage = when (syncAction) {
                ReaderReplacementRemoteSyncAction.None -> "规则已保存在当前设备（章节范围、标题规则和停用规则不会改动网页）"
                else -> "我的替换规则已保存，正在同步网页…"
            },
        )
        if (
            syncAction is ReaderReplacementRemoteSyncAction.Create &&
            normalized.id in pendingReaderReplacementCreates
        ) {
            // A rapid second save must update the local draft but never create duplicate website
            // glossary rows. The first create callback reconciles and then syncs this newest form.
            return
        }
        syncReaderReplacementSave(
            bookId = state.novelId,
            previous = previous,
            saved = normalized,
            action = syncAction,
        )
    }

    fun deleteReaderReplacementRule(ruleId: String) {
        val state = readerReplacementState
        if (state.novelId <= 0 || ruleId.isBlank()) return
        val rule = state.personalRules.firstOrNull { it.id == ruleId } ?: return
        if (ruleId in pendingReaderReplacementCreates) {
            deletedPendingReaderReplacementCreates += ruleId
            readerReplacementState = state.copy(actionMessage = "正在等待规则保存结果，之后删除本条贡献")
            return
        }
        val remote = readerReplacementDeleteSyncAction(rule)
        if (remote is ReaderReplacementRemoteSyncAction.Delete) {
            syncReaderReplacementDelete(state.novelId, ruleId, remote)
        } else {
            val remaining = state.personalRules.filterNot { it.id == ruleId }
            readerReplacementRulesStore.savePersonalRules(state.novelId, remaining)
            readerReplacementState = state.copy(personalRules = remaining, revision = state.revision + 1,
                ttsRevision = state.ttsRevision + 1, actionMessage = "本机规则已删除")
        }
    }

    private fun persistReaderReplacementAcknowledgement(bookId: Long, saved: ReaderReplacementRule, remote: ReaderReplacementRule) {
        val current = readerReplacementRulesStore.loadPersonalRules(bookId)
        val merged = com.novalpie.nativeapp.feature.reader.replacement.bindReaderReplacementAcknowledgement(current, saved, remote)
        if (merged != current) readerReplacementRulesStore.savePersonalRules(bookId, merged)
        if (readerReplacementState.novelId == bookId) {
            readerReplacementState = readerReplacementState.copy(personalRules = merged)
        }
    }

    private fun syncReaderReplacementSave(
        bookId: Long,
        previous: ReaderReplacementRule?,
        saved: ReaderReplacementRule,
        action: ReaderReplacementRemoteSyncAction,
    ) {
        if (action == ReaderReplacementRemoteSyncAction.None && saved.websiteCleanupRuleId == null) return
        if (action is ReaderReplacementRemoteSyncAction.Create) pendingReaderReplacementCreates += saved.id
        val environment = dependencies.environment.revision
        val lock = replacementWriteLocks.getOrPut(bookId to saved.id) { Mutex() }
        viewModelScope.launch {
            lock.withLock {
                if (environment != dependencies.environment.revision) return@withLock
                var executedAction = action
                val result = runCatching {
                    var latest = readerReplacementRulesStore.loadPersonalRules(bookId).firstOrNull { it.id == saved.id }
                    if (latest?.websiteCleanupRuleId != null) {
                        latest = replacementWriter.cleanup(latest) { remote ->
                            check(environment == dependencies.environment.revision)
                            persistReaderReplacementAcknowledgement(bookId, saved, remote)
                        }
                    }
                    // The stored acknowledged pair, not a later UI edit, is the next write's baseline.
                    val outgoing = saved.copy(
                        websiteRuleId = latest?.websiteRuleId ?: saved.websiteRuleId,
                        websiteSource = latest?.websiteSource ?: saved.websiteSource,
                        websiteReplacement = latest?.websiteReplacement ?: saved.websiteReplacement,
                        websiteCleanupRuleId = latest?.websiteCleanupRuleId,
                    )
                    executedAction = readerReplacementSaveSyncAction(
                        com.novalpie.nativeapp.feature.reader.replacement.readerReplacementSyncBaseline(previous, latest), outgoing,
                    )
                    replacementWriter.execute(executedAction) { remote ->
                        check(environment == dependencies.environment.revision)
                        persistReaderReplacementAcknowledgement(bookId, saved, remote)
                    }
                }
                if (action is ReaderReplacementRemoteSyncAction.Create) pendingReaderReplacementCreates -= saved.id
                if (environment != dependencies.environment.revision) return@withLock
                result.onFailure { failure ->
                    if (readerReplacementState.novelId == bookId) {
                        readerReplacementState = readerReplacementState.copy(
                            actionMessage = "规则已保存在本机，网页同步未完成：${apiFailureMessage("替换规则", failure)}",
                        )
                    }
                }.onSuccess { remoteRule ->
                    val currentRule = readerReplacementRulesStore.loadPersonalRules(bookId).firstOrNull { it.id == saved.id }
                    if (saved.id in deletedPendingReaderReplacementCreates) {
                        deletedPendingReaderReplacementCreates -= saved.id
                        remoteRule?.websiteRuleId?.let { id ->
                            // This is the user's already requested deletion, not cancellation rollback.
                            syncReaderReplacementDelete(bookId, saved.id, ReaderReplacementRemoteSyncAction.Delete(id))
                        }
                        return@onSuccess
                    }
                    if (readerReplacementState.novelId == bookId) {
                        readerReplacementState = readerReplacementState.copy(actionMessage =
                            if (remoteRule != null) "替换规则已同步到网页" else "规则已保存在本机")
                    }
                    if (executedAction is ReaderReplacementRemoteSyncAction.Create && currentRule != null && remoteRule != null &&
                        (currentRule.source != saved.source || currentRule.replacement != saved.replacement ||
                            currentRule.isRegex != saved.isRegex || currentRule.regexFlags != saved.regexFlags)) {
                        val followUp = readerReplacementSaveSyncAction(currentRule, currentRule)
                        syncReaderReplacementSave(bookId, currentRule, currentRule, followUp)
                    }
                }
            }
        }
    }

    private fun syncReaderReplacementDelete(
        bookId: Long,
        removedRuleId: String,
        action: ReaderReplacementRemoteSyncAction,
    ) {
        val delete = action as? ReaderReplacementRemoteSyncAction.Delete ?: return
        val environment = dependencies.environment.revision
        val identity = bookId to removedRuleId
        if (!pendingReaderReplacementDeletes.add(identity)) return
        val lock = replacementWriteLocks.getOrPut(identity) { Mutex() }
        if (readerReplacementState.novelId == bookId) readerReplacementState =
            readerReplacementState.copy(actionMessage = "正在删除本条规则；确认成功前保留本机内容")
        viewModelScope.launch {
            try {
                lock.withLock {
                    if (environment != dependencies.environment.revision) return@withLock
                    val current = readerReplacementRulesStore.loadPersonalRules(bookId).firstOrNull { it.id == removedRuleId }
                    val ids = listOfNotNull(current?.websiteCleanupRuleId, current?.websiteRuleId ?: delete.serverRuleId).distinct()
                    val result = runCatching {
                        for (id in ids) {
                            try { api.deletePersonalGlossary(id) }
                            catch (failure: com.novalpie.nativeapp.data.NovalPieApiException) {
                                if (failure.statusCode != 404) throw failure
                            }
                        }
                    }
                    if (environment != dependencies.environment.revision) return@withLock
                    if (result.isSuccess) {
                        val remaining = readerReplacementRulesStore.loadPersonalRules(bookId).filterNot { it.id == removedRuleId }
                        readerReplacementRulesStore.savePersonalRules(bookId, remaining)
                        if (readerReplacementState.novelId == bookId) readerReplacementState = readerReplacementState.copy(
                            personalRules = remaining, revision = readerReplacementState.revision + 1,
                            ttsRevision = readerReplacementState.ttsRevision + 1, actionMessage = "替换规则已从网站及本机删除",
                        )
                    } else if (readerReplacementState.novelId == bookId) readerReplacementState = readerReplacementState.copy(
                        actionMessage = "删除未完成，原规则已保留，可重试：${apiFailureMessage("删除规则", result.exceptionOrNull()!!)}",
                    )
                }
            } finally { pendingReaderReplacementDeletes.remove(identity) }
        }
    }

    fun setSharedReaderReplacementRuleVisible(ruleId: String, visible: Boolean) {
        val state = readerReplacementState
        if (state.novelId <= 0L || ruleId.isBlank()) return
        val nextHidden = state.hiddenSharedRuleIds.toMutableSet().apply {
            if (visible) remove(ruleId) else add(ruleId)
        }
        readerReplacementRulesStore.saveHiddenSharedRuleIds(state.novelId, nextHidden)
        readerReplacementState = state.copy(
            hiddenSharedRuleIds = nextHidden,
            revision = readerReplacementNextLocalRevision(
                currentRevision = state.revision,
                persistedRevision = readerReplacementRulesStore.revision(state.novelId),
            ),
            ttsRevision = state.ttsRevision + 1L,
            actionMessage = if (visible) "公共规则已启用" else "公共规则已隐藏",
        )
    }

    fun cloneSharedReaderReplacementRule(sharedRule: ReaderReplacementRule) {
        val state = readerReplacementState
        if (state.novelId <= 0L) return
        val clone = cloneSharedReaderReplacementRule(
            sharedRule = sharedRule,
            newId = "local-${java.util.UUID.randomUUID()}",
        ).copy(novelId = state.novelId)
        saveReaderReplacementRule(clone)
        readerReplacementState = readerReplacementState.copy(actionMessage = "已复制到我的规则，可继续编辑")
    }

    private fun loadReaderReplacementRules(bookId: Long) {
        if (bookId <= 0L) return
        val serial = ++replacementLoadRevision
        val environment = dependencies.environment.revision
        // The server glossary contract only represents a simple source/target pair. Import it as
        // a useful baseline, but preserve edits locally so scoped and regex rules cannot be
        // silently flattened or race a delayed remote create/delete response.
        val localRules = readerReplacementRulesStore.loadPersonalRules(bookId)
        val hiddenSharedRuleIds = readerReplacementRulesStore.loadHiddenSharedRuleIds(bookId)
        val defaultSharedRulesEnabled = readerReplacementRulesStore.loadDefaultSharedRulesEnabled()
        val sharedRulesEnabledOverride = readerReplacementRulesStore.loadSharedRulesEnabledOverride(bookId)
        val sharedRulesEnabled = readerSharedRulesEnabled(defaultSharedRulesEnabled, sharedRulesEnabledOverride)
        readerReplacementState = ReaderReplacementState(
            novelId = bookId,
            source = if (sharedRulesEnabled) ReaderReplacementRuleSource.All else ReaderReplacementRuleSource.Personal,
            personalRules = localRules,
            sharedRules = LoadResult.Loading,
            hiddenSharedRuleIds = hiddenSharedRuleIds,
            defaultSharedRulesEnabled = defaultSharedRulesEnabled,
            sharedRulesEnabledOverride = sharedRulesEnabledOverride,
            revision = readerReplacementRulesStore.revision(bookId),
            hasUnassignedLegacyRules = readerReplacementRulesStore.hasUnassignedLegacyRules(bookId),
        )
        viewModelScope.launch {
            val personalRequest = async { runCatching { api.personalGlossaries(bookId) } }
            val sharedRequest = async { runCatching { api.sharedGlossaries(bookId) } }
            val personal = personalRequest.await()
            val shared = sharedRequest.await()
            if (readerReplacementState.novelId != bookId || serial != replacementLoadRevision || environment != dependencies.environment.revision) return@launch
            val current = readerReplacementState
            val mergedPersonalRules = personal.getOrNull()?.let { remoteRules ->
                mergeReaderReplacementPersonalRules(
                    localRules = current.personalRules,
                    remoteRules = remoteRules,
                )
            } ?: current.personalRules
            if (mergedPersonalRules != current.personalRules) {
                readerReplacementRulesStore.savePersonalRules(bookId, mergedPersonalRules)
            }
            val loadedSharedRules = shared.toLoadResult("加载公共替换规则")
            val actionMessage = when {
                shared.isFailure -> apiFailureMessage("加载公共替换规则", shared.exceptionOrNull()!!)
                else -> null
            }
            readerReplacementState = current.copy(
                personalRules = mergedPersonalRules,
                sharedRules = loadedSharedRules,
                revision = readerReplacementRevisionAfterRulesLoad(
                    current = current,
                    personalRules = mergedPersonalRules,
                    sharedRules = loadedSharedRules,
                ),
                actionMessage = actionMessage,
            )
        }
    }

    /** Appends the next source chapter to the current reader window for website-style scrolling. */
    fun loadNextReaderChapter() = loadContinuousReaderChapter(1)
    fun loadPreviousReaderChapter() = loadContinuousReaderChapter(-1)

    private fun loadContinuousReaderChapter(direction: Int) {
        val route = currentRoute as? AppRoute.Reader ?: return
        val state = readerState
        if (
            state.loadingNextChapter || state.loadingPreviousChapter ||
            (direction > 0 && (state.nextChapterExhausted || state.nextChapterWaitingForCatalog))
        ) return
        val chapters = (state.chapters as? LoadResult.Success)?.value
        // The initial catalog request owns the Loading state. A failed/idle catalog can be retried
        // explicitly, while a successful partial catalog is refreshed once by the branch below.
        if (chapters == null) {
            if (state.chapters is LoadResult.Idle) refreshReaderCatalog()
            return
        }
        val loadedIds = state.chapterContents.map { it.chapterId }.toSet().ifEmpty { setOf(state.chapterId) }
        val edgeId = (if (direction < 0) state.chapterContents.firstOrNull() else state.chapterContents.lastOrNull())?.chapterId ?: state.chapterId
        if (direction > 0 && state.chapterContents.size >= com.novalpie.nativeapp.feature.reader.text.READER_CONTINUOUS_WINDOW_SIZE &&
            state.chapterContents.firstOrNull()?.chapterId == (state.visibleChapterId ?: state.chapterId)) return
        // An empty/incomplete catalog is not evidence that this is the last chapter. Refresh only
        // once for this sentinel state; the refreshed result decides whether to retry or to show a
        // visible manual retry affordance.
        if (readerCatalogIsIncomplete(edgeId, chapters)) {
            readerState = state.copy(nextChapterWaitingForCatalog = true)
            refreshReaderCatalog()
            return
        }
        val next = if (direction < 0) adjacentReaderChapters(edgeId, chapters).previous
            else nextReaderChapterForInfiniteScroll(edgeId, chapters, loadedIds)
        if (next == null) {
            if (direction < 0) { readerState = state.copy(previousChapterError = "目录中没有更早的章节"); return }
            // A source deployment can temporarily return a truncated directory. Do not convert
            // that into an unrecoverable "已读完" state until one fresh read confirms it.
            if (!state.nextChapterEndConfirmationRequested) {
                readerState = state.copy(
                    nextChapterWaitingForCatalog = true,
                    nextChapterEndConfirmationRequested = true,
                    nextChapterError = null,
                )
                refreshReaderCatalog()
                return
            }
            readerState = state.copy(
                nextChapterExhausted = true,
                nextChapterError = null,
                nextChapterWaitingForCatalog = false,
            )
            return
        }
        readerState = state.copy(
            loadingNextChapter = direction > 0,
            loadingPreviousChapter = direction < 0,
            previousChapterError = null,
            nextChapterError = null,
            nextChapterWaitingForCatalog = false,
            nextChapterEndConfirmationRequested = false,
            nextChapterExhausted = false,
        )
        val requestSerial = readerRequestSerial
        val replaceMode = readerUiOptions.replaceMode
        val showImages = readerUiOptions.showImages
        viewModelScope.launch {
            val cachedContent = async(Dispatchers.IO) {
                readerChapterCacheStore.load(
                    bookId = route.bookId,
                    chapterId = next.id,
                    replaceMode = replaceMode,
                    showImages = showImages,
                )
            }
            val chapterComments = async {
                runCatching {
                    api.chapterComments(
                        bookId = route.bookId,
                        chapterId = next.id,
                        page = 1,
                        limit = PAGE_SIZE,
                    )
                }
            }
            val result = runCatching {
                api.chapterContent(
                    chapterId = next.id,
                    replaceMode = replaceMode,
                    showImages = showImages,
                )
            }
            val cached = cachedContent.await()
            val servedFromCache = result.isFailure && cached != null
            val content = result.getOrNull() ?: cached?.content
            if (
                requestSerial != readerRequestSerial ||
                currentRoute != route ||
                readerState.bookId != route.bookId ||
                readerState.chapterId != route.chapterId
            ) return@launch
            if (content != null) {
                if (!servedFromCache) {
                    withContext(Dispatchers.IO) {
                        readerChapterCacheStore.save(
                            bookId = route.bookId,
                            chapterId = next.id,
                            replaceMode = replaceMode,
                            showImages = showImages,
                            sourceUpdatedAt = next.updatedAt,
                            content = content,
                        )
                    }
                }
                val cacheStates = withContext(Dispatchers.IO) {
                    readerChapterCacheStore.cacheStates(
                        bookId = route.bookId,
                        replaceMode = replaceMode,
                        showImages = showImages,
                        chapters = chapters,
                    )
                }
                if (requestSerial != readerRequestSerial || currentRoute != route || readerState.bookId != route.bookId) return@launch
                val existing = readerState.chapterContents
                val addition = ReaderChapterContent(next.id, next.title, content)
                val window = com.novalpie.nativeapp.feature.reader.text.boundedReaderChapterWindow(
                    if (direction < 0) listOf(addition) + existing else existing + addition,
                    readerState.visibleChapterId ?: state.chapterId, direction)
                val retainedIds = window.map { it.chapterId }.toSet()
                val commentStates = com.novalpie.nativeapp.feature.reader.text.retainedReaderCommentWindow(readerState.chapterCommentStates, retainedIds)
                val targetComment = commentStates[next.id] ?: ReaderChapterCommentState()
                readerState = readerState.copy(
                    chapterContents = window,
                    loadingNextChapter = false,
                    loadingPreviousChapter = false,
                    nextChapterError = null,
                    nextChapterWaitingForCatalog = false,
                    nextChapterEndConfirmationRequested = false,
                    nextChapterExhausted = false,
                    chapterCacheStates = cacheStates,
                    contentFromCache = readerState.contentFromCache || servedFromCache,
                    chapterCommentStates = if (next.id in retainedIds) commentStates +
                        (next.id to targetComment.copy(comments = LoadResult.Loading)) else commentStates,
                )
                launch {
                    val commentsResult = chapterComments.await()
                    if (
                        requestSerial != readerRequestSerial ||
                        currentRoute != route ||
                        readerState.bookId != route.bookId ||
                        readerState.chapterId != route.chapterId ||
                        readerState.chapterContents.none { it.chapterId == next.id }
                    ) return@launch
                    val existingCommentState = readerChapterCommentState(readerState, next.id)
                    readerState = readerState.copy(
                        chapterCommentStates = readerState.chapterCommentStates + (
                            next.id to existingCommentState.copy(
                                comments = commentsResult.toLoadResult(VisibleUiLabels.ChapterComments),
                                bookReferences = if (commentsResult.isFailure) {
                                    existingCommentState.bookReferences
                                } else {
                                    emptyMap()
                                },
                            )
                        ),
                    )
                    commentsResult.getOrNull()?.let { commentList ->
                        loadReaderCommentBookReferences(
                            bookId = route.bookId,
                            chapterId = next.id,
                            comments = commentList,
                            requestSerial = requestSerial,
                        )
                    }
                }
            } else {
                readerState = readerState.copy(
                    loadingNextChapter = false,
                    loadingPreviousChapter = false,
                    previousChapterError = if (direction < 0) apiFailureMessage("上一章", result.exceptionOrNull() ?: IllegalStateException("缓存不可用")) else readerState.previousChapterError,
                    nextChapterError = if (direction > 0) apiFailureMessage("下一章", result.exceptionOrNull() ?: IllegalStateException("缓存不可用")) else readerState.nextChapterError,
                    nextChapterWaitingForCatalog = false,
                    nextChapterEndConfirmationRequested = false,
                    nextChapterExhausted = false,
                )
            }
        }
    }

    /** Refreshes only the chapter catalog, preserving the current body and LazyColumn position. */
    fun refreshReaderCatalog() {
        val route = currentRoute as? AppRoute.Reader ?: return
        val state = readerState
        if (state.bookId != route.bookId || state.chapterId != route.chapterId) return
        if (state.chapters is LoadResult.Loading) return

        val requestSerial = ++readerCatalogRequestSerial
        readerState = state.copy(
            chapters = LoadResult.Loading,
            nextChapterError = null,
            nextChapterWaitingForCatalog = false,
            nextChapterExhausted = false,
        )
        val replaceMode = readerUiOptions.replaceMode
        val showImages = readerUiOptions.showImages
        viewModelScope.launch {
            val result = runCatching { api.chapters(route.bookId) }
            if (
                requestSerial != readerCatalogRequestSerial ||
                currentRoute != route ||
                readerState.bookId != route.bookId ||
                readerState.chapterId != route.chapterId
            ) return@launch

            val refreshed = result.getOrNull()
            val hasCurrentChapter = refreshed != null && !readerCatalogIsIncomplete(
                currentChapterId = route.chapterId,
                chapters = refreshed,
            )
            val loadedIds = readerState.chapterContents.map { it.chapterId }.toSet()
                .ifEmpty { setOf(route.chapterId) }
            val next = refreshed?.let {
                nextReaderChapterForInfiniteScroll(route.chapterId, it, loadedIds)
            }
            val cacheStates = refreshed?.let { chapters ->
                withContext(Dispatchers.IO) {
                    readerChapterCacheStore.cacheStates(
                        bookId = route.bookId,
                        replaceMode = replaceMode,
                        showImages = showImages,
                        chapters = chapters,
                    )
                }
            }.orEmpty()
            readerState = readerState.copy(
                chapters = result.toLoadResult("阅读器目录"),
                nextChapterWaitingForCatalog = result.isSuccess && !hasCurrentChapter,
                // A refresh that was explicitly requested to confirm an apparent end may now
                // close the book; ordinary initial catalog loads still leave the confirmation to
                // loadNextReaderChapter so the user never gets a false terminal state.
                nextChapterEndConfirmationRequested = false,
                nextChapterExhausted = result.isSuccess &&
                    hasCurrentChapter &&
                    next == null &&
                    state.nextChapterEndConfirmationRequested,
                nextChapterError = null,
                chapterCacheStates = cacheStates,
            )
        }
    }

    fun cycleChineseVariant() {
        updateChineseVariant(chineseVariant.next())
    }

    fun toggleAppTheme(isCurrentlyDark: Boolean) {
        updateAppThemeMode(if (isCurrentlyDark) AppThemeMode.Light else AppThemeMode.Dark)
    }

    fun updateAppThemeMode(mode: AppThemeMode) {
        if (appThemeMode == mode) return
        appThemeSettingsStore.saveMode(mode)
        appThemeMode = mode
    }

    fun updateChineseVariant(variant: ChineseVariant) {
        if (chineseVariant == variant) return
        chineseVariantSettingsStore.saveVariant(variant)
        chineseVariant = variant
    }

    fun updateProxyEnabled(value: Boolean) {
        proxyEnabled = value
    }

    fun updateProxyHost(value: String) {
        proxyHost = value
    }

    fun updateProxyPort(value: String) {
        proxyPortText = value.filter { it.isDigit() }.take(5)
    }

    fun saveProxySettings() {
        val next = ProxySettings(
            enabled = proxyEnabled,
            host = proxyHost.trim().ifBlank { ProxySettings.DEFAULT_PROXY_HOST },
            port = proxyPortText.toIntOrNull()?.coerceIn(1, 65535) ?: ProxySettings.DEFAULT_PROXY_PORT
        )
        proxySettings = next
        proxyEnabled = next.enabled
        proxyHost = next.host
        proxyPortText = next.port.toString()
        networkConfigStore.saveProxySettings(next)
        AppContainer.from(getApplication()).refreshEnvironmentFromStores()
        configureNovalPieImageLoader(getApplication(), next)
        loadHome()
    }

    fun saveCapturedAuthToken(token: String) {
        val normalized = token.trim()
        if (normalized.isBlank() || normalized == authToken) return
        // A token change may switch away from an administrator account. Close any already-open
        // management surface before the replacement account has been resolved.
        sanitizeAdminSurfaceIfNeeded(isAdmin = false)
        authSessionStore.saveToken(normalized)
        authToken = normalized
        AppContainer.from(getApplication()).refreshEnvironmentFromStores()
        loadHome()
    }

    fun clearAuthToken() {
        authSessionStore.clearToken()
        authToken = null
        AppContainer.from(getApplication()).refreshEnvironmentFromStores()
        profileFeature.environmentChanged()
        sanitizeAdminSurfaceIfNeeded(isAdmin = false)
        loadHome()
    }

    fun openTab(tab: BottomTab) {
        val targetRoute = when (tab) {
            BottomTab.Collection -> AppRoute.Home
            BottomTab.Discover -> AppRoute.Search
            BottomTab.Tools -> AppRoute.Tools
            BottomTab.Forum -> AppRoute.Forum
            BottomTab.Profile -> AppRoute.Profile
        }
        if (currentTab == tab && currentRoute == targetRoute) {
            // Repeated bottom-navigation taps should preserve the live screen instead of clearing
            // it and issuing another request. Explicit refresh controls remain responsible for a
            // deliberate reload.
            return
        }

        clearReaderSessionWhenLeaving()

        if (tab == BottomTab.Collection && shouldLoadHomeOnTabEntry()) loadHome()
        if (tab == BottomTab.Forum && shouldLoadForumOnTabEntry()) loadForum()
        if (tab == BottomTab.Discover) {
            loadSearchTags()
            loadDefaultSearchResultsIfNeeded()
        }
        if (tab == BottomTab.Tools) loadTools()
        if (tab == BottomTab.Profile) loadProfile()

        navigator.reset(targetRoute)
        currentTab = tab
    }

    private fun shouldLoadHomeOnTabEntry(): Boolean = when (favoritesUiOptions.tab) {
        FavoritesContentTab.Favorites -> collectionRefreshRequired(
            readerProgressRevision = readerProgressRevision,
            syncedProgressRevision = syncedCollectionProgressRevision,
        ) || homeState.favoriteEntries is LoadResult.Idle || homeState.favoriteEntries is LoadResult.Error
        FavoritesContentTab.History -> homeState.history is LoadResult.Idle || homeState.history is LoadResult.Error
    }

    private fun shouldLoadForumOnTabEntry(): Boolean =
        forumState.posts is LoadResult.Idle || forumState.posts is LoadResult.Error

    fun openSettings() {
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.Settings))
    }

    fun loadProfile() = profileFeature.load(authToken?.let(::decodeAuthTokenProfile), forumState.hideSpoilers)

    fun selectProfileTab(tab: ProfileTab) {
        if (profileState.selectedTab != tab) {
            profileState = profileState.copy(selectedTab = tab, actionMessage = null)
        }
    }

    fun selectProfileActivityFilter(filter: ProfileActivityFilter) {
        if (profileState.activityFilter != filter) {
            profileState = profileState.copy(activityFilter = filter, actionMessage = null)
        }
    }
    fun loadMoreProfileActivities() = profileFeature.loadMoreActivities()
    fun loadMorePublicProfileActivities() = publicProfileFeature.loadMoreActivities()
    internal fun publicProfileScrollPosition() = publicProfileFeature.scrollPosition()
    fun savePublicProfileScroll(userId: Long, tab: UserProfileTab, filter: ProfileActivityFilter, index: Int, offset: Int) =
        publicProfileFeature.saveScroll(userId, tab, filter, index, offset)

    fun selectPersonalizationTab(tab: PersonalizationTab) {
        if (profileState.personalizationTab != tab) {
            profileState = profileState.copy(personalizationTab = tab, actionMessage = null)
        }
    }

    fun toggleCurrentUserEquipment(item: UserInventoryItem) {
        if (authToken.isNullOrBlank()) { profileState = profileState.copy(actionMessage = "请先登录"); return }
        profileFeature.equip(item)
    }

    fun purchaseCurrentUserShopItem(item: ShopItem) {
        if (authToken.isNullOrBlank()) { profileState = profileState.copy(actionMessage = "请先登录"); return }
        profileFeature.purchase(item)
    }

    /** Local filtering only; uploaded books have already been fetched for the current profile. */
    fun updateProfileBookQuery(value: String) {
        if (profileState.bookQuery != value) {
            profileState = profileState.copy(bookQuery = value, actionMessage = null)
        }
    }

    fun selectProfileBooksGridColumns(columns: Int) {
        val normalized = com.novalpie.nativeapp.data.normalizeGridColumns(columns)
        if (profileState.booksGridColumns == normalized) return
        profileBooksSettingsStore.save(
            com.novalpie.nativeapp.data.PersistedProfileBooksSettings(gridColumns = normalized)
        )
        profileState = profileState.copy(booksGridColumns = normalized, actionMessage = null)
    }

    fun updateDownloadImageConcurrency(value: Int) {
        val normalized = com.novalpie.nativeapp.data.normalizeDownloadImageConcurrency(value)
        if (profileState.downloadImageConcurrency == normalized) return
        downloadSettingsStore.save(
            com.novalpie.nativeapp.data.DownloadSettings(imageConcurrency = normalized),
        )
        profileState = profileState.copy(
            downloadImageConcurrency = normalized,
            actionMessage = "下载并发已设为 ${normalized} 路",
        )
    }

    fun updateProfileName(value: String) {
        profileFeature.edit { it.copy(nameDraft = value, actionMessage = null) }
    }

    fun updateProfileBio(value: String) {
        profileFeature.edit { it.copy(bioDraft = value, actionMessage = null) }
    }

    fun updateProfileShowCheckin(value: Boolean) {
        profileFeature.edit { it.copy(showCheckin = value, actionMessage = null) }
    }

    fun updateProfileAutoCheckin(value: Boolean) {
        profileFeature.edit { it.copy(autoCheckin = value, actionMessage = null) }
    }

    fun updateProfileAdultBirthYear(value: String) {
        profileState = profileState.copy(
            adultBirthYearDraft = value.filter(Char::isDigit).take(4),
            actionMessage = null
        )
    }

    fun saveProfile() = profileFeature.save()

    fun checkinCurrentUser() {
        if (authToken.isNullOrBlank()) { profileState = profileState.copy(actionMessage = "请先登录后再签到"); return }
        profileFeature.checkin()
    }

    fun verifyCurrentUserAdult() {
        if (authToken.isNullOrBlank()) { profileState = profileState.copy(actionMessage = "请先登录"); return }
        profileFeature.verifyAdult()
    }

    fun uploadProfileAvatar(rawUri: String) {
        if (rawUri.isBlank()) return
        profileFeature.uploadAvatar {
            val document = readUploadDocument(rawUri)
            require(document.sizeBytes > 0) { "头像文件为空" }
            require(document.mimeType?.startsWith("image/") == true) { "请选择图片文件" }
            api.uploadCurrentUserAvatar(uploadSource(document))
        }
    }

    fun openUserProfile(userId: Long) {
        if (userId <= 0) return
        val ownId = currentUserProfile()?.id
        if (ownId != null && ownId == userId) {
            currentTab = BottomTab.Profile
            navigator.reset(AppRoute.Profile)
            loadProfile()
            return
        }
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.UserProfileDetail(userId)))
        loadUserProfile(userId)
    }

    fun loadUserProfile(userId: Long) = publicProfileFeature.enter(userId, forumState.hideSpoilers, refresh = true)
    fun retryUserProfileActivities() = publicProfileFeature.retry(PublicUserProfilePanel.Activities)
    fun retryUserProfileBooks() = publicProfileFeature.retry(PublicUserProfilePanel.Books)
    fun retryUserProfileCheckinStats() = publicProfileFeature.retry(PublicUserProfilePanel.CheckinStats)
    fun retryUserProfileCheckinRecords() = publicProfileFeature.retry(PublicUserProfilePanel.CheckinRecords)
    fun retryUserProfileCheckinSettings() = publicProfileFeature.retry(PublicUserProfilePanel.CheckinSettings)

    fun selectUserProfileTab(tab: UserProfileTab) {
        userProfileDetailState = userProfileDetailState.copy(selectedTab = tab)
    }

    fun selectUserProfileActivityFilter(filter: ProfileActivityFilter) {
        if (userProfileDetailState.activityFilter != filter) {
            userProfileDetailState = userProfileDetailState.copy(activityFilter = filter)
        }
    }

    fun openUserActivity(activity: UserActivity) {
        when {
            activity.postId != null -> openForumPost(activity.postId)
            activity.bookId != null && activity.chapterId != null ->
                openReader(activity.bookId, activity.chapterId)
            activity.bookId != null -> openBook(activity.bookId)
        }
    }

    fun openAdminSection(section: AdminSection) {
        if (!isAdminProfile(currentUserProfile())) return
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.Admin(section)))
        loadAdminSection(section)
    }

    fun loadAdminSection(section: AdminSection = adminState.section) {
        adminFeature.load(section)
    }

    fun updateAdminOverviewDays(days: Int) {
        val normalizedDays = days.coerceIn(1, 90)
        if (adminState.overviewDays == normalizedDays) return
        adminState = adminState.copy(overviewDays = normalizedDays)
        loadAdminSection(AdminSection.Overview)
    }

    fun updateAdminReviewQuery(query: AdminReviewQuery) {
        adminState = adminState.copy(reviewQuery = query)
    }

    fun applyAdminReviewQuery() {
        loadAdminSection(AdminSection.Review)
    }

    fun resetAdminReviewQuery() {
        adminState = adminState.copy(reviewQuery = AdminReviewQuery())
        loadAdminSection(AdminSection.Review)
    }

    fun updateAdminOperationLogQuery(query: AdminOperationLogQuery) {
        adminState = adminState.copy(operationLogQuery = query.copy(page = query.page.coerceAtLeast(1)))
    }

    fun applyAdminOperationLogQuery() {
        loadAdminSection(AdminSection.OperationLogs)
    }

    fun resetAdminOperationLogQuery() {
        adminState = adminState.copy(operationLogQuery = AdminOperationLogQuery())
        loadAdminSection(AdminSection.OperationLogs)
    }

    fun selectAdminOperationLogPage(page: Int) {
        val normalizedPage = page.coerceAtLeast(1)
        if (adminState.operationLogQuery.page == normalizedPage) return
        adminState = adminState.copy(
            operationLogQuery = adminState.operationLogQuery.copy(page = normalizedPage)
        )
        loadAdminSection(AdminSection.OperationLogs)
    }

    fun updateAdminShopQuery(query: AdminShopQuery) {
        adminState = adminState.copy(shopQuery = query)
    }

    fun applyAdminShopQuery() {
        loadAdminSection(AdminSection.Shop)
    }

    fun resetAdminShopQuery() {
        adminState = adminState.copy(shopQuery = AdminShopQuery())
        loadAdminSection(AdminSection.Shop)
    }

    fun toggleAdminReviewSetting(kind: String) {
        if (kind !in setOf("upload", "delete")) return
        val settings = (adminState.reviewSettings as? LoadResult.Success)?.value ?: return
        adminFeature.mutate(AdminCommand.ReviewSettings(
            if (kind == "upload") !settings.autoApproveUpload else settings.autoApproveUpload,
            if (kind == "delete") !settings.autoApproveDelete else settings.autoApproveDelete))
    }

    fun approveAllAdminReviews() {
        val query = adminState.reviewQuery
        if (query.status.isNotBlank() && query.status != "pending") return
        adminFeature.mutate(AdminCommand.ReviewAll(query))
    }

    fun adminReviewAction(requestId: Long, action: String) = adminFeature.mutate(AdminCommand.Review(requestId, action))
    fun updateAdminKeyStatus(keyId: Long, status: String) = adminFeature.mutate(AdminCommand.KeyStatus(keyId, status))
    fun deleteAdminKey(keyId: Long) = adminFeature.mutate(AdminCommand.DeleteKey(keyId))
    fun toggleAdminCookieConfig(config: AdminCookieConfig) = adminFeature.mutate(AdminCommand.SaveCookie(config.copy(isActive = !config.isActive), null, false))
    fun saveAdminCookieConfig(config: AdminCookieConfig, cookieRaw: String?) = adminFeature.mutate(AdminCommand.SaveCookie(config, cookieRaw))
    fun deleteAdminCookieConfig(configId: Long) = adminFeature.mutate(AdminCommand.DeleteCookie(configId))
    fun setAdminBaseUrlRuleAction(rule: AdminBaseUrlRule, action: String) = adminFeature.mutate(AdminCommand.SaveRule(rule.copy(action = action), false))
    fun saveAdminBaseUrlRule(rule: AdminBaseUrlRule) = adminFeature.mutate(AdminCommand.SaveRule(rule))
    fun deleteAdminBaseUrlRule(ruleId: Long) = adminFeature.mutate(AdminCommand.DeleteRule(ruleId))
    fun toggleAdminShopItem(item: AdminShopItem) = adminFeature.mutate(AdminCommand.SaveShop(item.copy(isActive = !item.isActive), false))
    fun saveAdminShopItem(item: AdminShopItem) = adminFeature.mutate(AdminCommand.SaveShop(item))
    fun deleteAdminShopItem(itemId: Long) = adminFeature.mutate(AdminCommand.DeleteShop(itemId))

    fun updateAdminSchedulerLines(lines: Int) {
        if (lines !in setOf(50, 100, 200, 500, 1000)) return
        adminState = adminState.copy(schedulerLines = lines)
        adminFeature.load(AdminSection.Scraper)
    }

    fun loadTools() {
        val requestSerial = ++toolsRequestSerial
        toolsState = ToolsState(
            stats = LoadResult.Loading,
            messages = LoadResult.Loading
        )
        viewModelScope.launch {
            val stats = async { runCatching { api.messageStats() } }
            val messages = async { runCatching { api.messages(page = 1, pageSize = TOOLS_MESSAGE_PREVIEW_LIMIT) } }
            val statsResult = stats.await()
            val messagesResult = messages.await()
            if (!isFreshRequestSerial(requestSerial, toolsRequestSerial)) return@launch
            toolsState = ToolsState(
                stats = statsResult.toLoadResult("消息统计"),
                messages = messagesResult.toLoadResult("消息列表")
            )
        }
    }

    fun openMessageCenter() {
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.MessageCenter))
        loadMessageCenter()
    }

    fun loadMessageCenter() = messageInboxFeature.refresh()
    fun loadMoreMessages() = messageInboxFeature.loadMore()
    fun updateMessageKeyword(value: String) = messageInboxFeature.editKeyword(value)
    fun applyMessageSearch() = messageInboxFeature.submit()
    fun selectMessageType(type: Int?) = messageInboxFeature.filter { it.copy(messageType = type) }
    fun selectMessageReadFilter(isRead: Boolean?) = messageInboxFeature.filter { it.copy(isRead = isRead) }
    fun selectMessagePriority(priority: Int?) = messageInboxFeature.filter { it.copy(priority = priority) }
    fun toggleMessageSelected(messageId: Long) = messageInboxFeature.toggleSelected(messageId)
    fun selectAllVisibleMessages(select: Boolean) = messageInboxFeature.selectAll(select)
    fun markSelectedMessagesRead() = messageInboxFeature.markSelectedRead()
    fun deleteSelectedMessages() = messageInboxFeature.deleteSelected()
    fun markAllMessagesRead() = messageInboxFeature.markAllRead()
    fun toggleMessageStar(message: SiteMessage) = messageInboxFeature.star(message)

    fun openMessage(message: SiteMessage) {
        if (message.type == 8) {
            val targetUserId = directMessageTargetUserId(message, currentUserProfile()?.id)
            if (targetUserId != null) {
                if (!message.isRead) messageInboxFeature.markRead(message.id)
                openMessageConversation(targetUserId, message.username)
                return
            }
        }
        openMessageDetail(message.id)
    }

    fun openMessageDetail(messageId: Long) {
        if (messageId <= 0) return
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.MessageDetail(messageId)))
        loadMessageDetail(messageId)
    }

    fun loadMessageDetail(messageId: Long) = messageDetailFeature.load(messageId)
    fun markCurrentMessageRead() = messageDetailFeature.markRead()
    fun toggleCurrentMessageStar() = messageDetailFeature.toggleStar()
    fun deleteCurrentMessage() = messageDetailFeature.delete()

    fun openCurrentMessageConversation() {
        val message = (messageDetailState.detail as? LoadResult.Success)?.value ?: return
        directMessageTargetUserId(message, currentUserProfile()?.id)?.let { targetUserId ->
            openMessageConversation(targetUserId, message.username)
        }
    }

    fun openMessageAction(actionUrl: String) {
        val absolute = if (actionUrl.startsWith("http://") || actionUrl.startsWith("https://")) {
            actionUrl
        } else {
            "https://novalpie.cc/${actionUrl.trimStart('/')}"
        }
        val uri = runCatching { Uri.parse(absolute) }.getOrNull()
        if (uri?.host?.equals("novalpie.cc", ignoreCase = true) != true) {
            openWebFallback(absolute)
            return
        }
        val segments = uri.pathSegments.orEmpty()
        when (segments.firstOrNull()) {
            "forum", "posts" -> segments.getOrNull(1)?.toLongOrNull()?.let(::openForumPost)
                ?: openWebFallback(absolute)
            "book" -> {
                val bookId = segments.getOrNull(1)?.toLongOrNull()
                val chapterId = segments.getOrNull(2)?.toLongOrNull()
                when {
                    bookId == null -> openWebFallback(absolute)
                    chapterId != null -> openReader(bookId, chapterId)
                    else -> openBook(bookId)
                }
            }
            else -> openWebFallback(absolute)
        }
    }

    fun openMessageConversation(targetUserId: Long, targetName: String?) {
        if (targetUserId <= 0) return
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.MessageConversation(targetUserId, targetName)))
        loadMessageConversation(targetUserId, targetName)
    }

    fun loadMessageConversation(targetUserId: Long, targetName: String?) = conversationFeature.load(targetUserId, targetName)
    fun loadMoreDirectMessages() = conversationFeature.loadMore()
    fun updateMessageDraft(value: String) = conversationFeature.edit(value)

    fun sendMessageDraft() {
        val profile = currentUserProfile() ?: return
        val id = profile.id ?: return
        conversationFeature.send(id, profile.name)
    }

    fun openMessageSettings() {
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.MessageSettings))
        loadMessageSettings()
    }

    fun loadMessageSettings() = messageSettingsFeature.load()
    fun updateMessageSettingsDraft(transform: (MessageSettings) -> MessageSettings) = messageSettingsFeature.edit(transform)
    fun saveMessageSettings() = messageSettingsFeature.save()

    private fun currentUserProfile(): UserProfile? = effectiveToolsUserProfile(
        profile = profileState.profile,
        shelfUser = homeState.user,
        tokenProfile = authToken?.let(::decodeAuthTokenProfile),
    )

    /**
     * The tools surface can be opened while the collection and profile requests are at different
     * points in flight. Use the freshest same-account profile for admin visibility, while keeping
     * the collection/token identity as a safe fallback during a cold launch.
     */
    fun toolsUserProfile(): UserProfile? = effectiveToolsUserProfile(
        profile = profileState.profile,
        shelfUser = homeState.user,
        tokenProfile = authToken?.let(::decodeAuthTokenProfile),
    )

    /** Publish an authenticated profile and immediately revoke any stale administrator surface. */
    private fun publishHomeUserProfile(profile: UserProfile) {
        homeState = homeState.copy(user = LoadResult.Success(profile))
        val currentStack = routes.toList()
        applySanitizedAdminRouteStack(
            currentStack = currentStack,
            sanitizedStack = sanitizeAdminRouteStackForAuthoritativeProfile(
                routes = currentStack,
                profile = profile,
            ),
        )
    }

    /**
     * Remove stale admin routes as soon as the authoritative role is no longer administrator.
     * Invalidating the request serial is important: a slow admin response must not repopulate the
     * cleared state after logout or role revocation.
     */
    private fun sanitizeAdminSurfaceIfNeeded(isAdmin: Boolean) {
        if (!isAdmin) adminFeature.environmentChanged()
        val currentStack = routes.toList()
        val sanitizedStack = sanitizeAdminRouteStack(currentStack, isAdmin)
        applySanitizedAdminRouteStack(currentStack, sanitizedStack)
    }

    private fun applySanitizedAdminRouteStack(
        currentStack: List<AppRoute>,
        sanitizedStack: List<AppRoute>,
    ) {
        if (sanitizedStack == currentStack) return

        adminFeature.environmentChanged()
        navigator.replaceAll(sanitizedStack)
        currentTab = BottomTab.Tools
        adminState = AdminState()
    }

    fun openUploadBook() {
        uploadFeature.enter(null)
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.UploadBook))
    }

    fun updateUploadBookDraft(draft: UploadBookDraft) = uploadFeature.draft(draft)

    fun selectUploadEpub(rawUri: String) = uploadFeature.select(rawUri)

    fun submitUploadBook() = uploadFeature.submit()

    fun confirmRetryUploadBook() = uploadFeature.submit(confirmUncertainRetry = true)

    fun clearUploadBook() = uploadFeature.clear()

    fun openUploadedBook(novelId: Long) {
        if (novelId > 0L) openBook(novelId)
    }

    fun openUploadEditor() {
        val aiConfigs = workspaceLocalStore.loadApis()
            .filter { it.endpoint.isNotBlank() && it.model.isNotBlank() && it.apiKey.isNotBlank() }
        val selectedAiConfigId = uploadEditorState.selectedAiConfigId
            ?.takeIf { selected -> aiConfigs.any { it.id == selected } }
            ?: aiConfigs.firstOrNull()?.id
        uploadEditorState = uploadEditorState.copy(
            archives = editorArchiveStore.list(),
            aiConfigs = aiConfigs,
            selectedAiConfigId = selectedAiConfigId,
            actionMessage = null
        )
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.UploadEditor))
    }

    fun selectEditorTab(tab: EditorTab) {
        uploadEditorState = uploadEditorState.copy(selectedTab = tab, actionMessage = null)
    }

    private fun editorSnapshot(state: UploadEditorState) = EditorDocumentSnapshot(
        text = state.text,
        cursorPosition = state.cursorPosition,
        chapters = state.chapters.toList(),
        markerValidationErrors = state.markerValidationErrors.toList()
    )

    private fun withEditorHistoryFlags(state: UploadEditorState) = state.copy(
        canUndo = editorDocumentHistory.canUndo,
        canRedo = editorDocumentHistory.canRedo
    )

    private fun commitEditorDocumentChange(
        previous: UploadEditorState,
        updated: UploadEditorState
    ): UploadEditorState {
        editorDocumentHistory.record(editorSnapshot(previous), editorSnapshot(updated))
        return withEditorHistoryFlags(updated)
    }

    private fun replaceEditorDocument(updated: UploadEditorState): UploadEditorState {
        editorDocumentHistory.clear()
        return withEditorHistoryFlags(updated)
    }

    private fun restoreEditorDocument(
        state: UploadEditorState,
        snapshot: EditorDocumentSnapshot,
        actionMessage: String
    ): UploadEditorState = withEditorHistoryFlags(
        state.copy(
            text = snapshot.text,
            cursorPosition = snapshot.cursorPosition.coerceIn(0, snapshot.text.length),
            chapters = snapshot.chapters,
            markerValidationErrors = snapshot.markerValidationErrors,
            selectedTab = if (snapshot.chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
            actionMessage = actionMessage
        )
    )

    /** Atomic text/selection updates keep a history snapshot at the exact caret position. */
    fun updateEditorDocument(value: String, cursorPosition: Int) {
        val state = uploadEditorState
        val textChanged = value != state.text
        val updated = state.copy(
            text = value,
            cursorPosition = cursorPosition.coerceIn(0, value.length),
            chapters = if (textChanged) emptyList() else state.chapters,
            markerValidationErrors = if (textChanged) emptyList() else state.markerValidationErrors,
            selectedTab = if (textChanged) EditorTab.Text else state.selectedTab,
            actionMessage = null
        )
        uploadEditorState = if (textChanged) commitEditorDocumentChange(state, updated) else withEditorHistoryFlags(updated)
    }

    fun updateEditorText(value: String) {
        updateEditorDocument(value, uploadEditorState.cursorPosition)
    }

    fun updateEditorCursor(position: Int) {
        uploadEditorState = withEditorHistoryFlags(uploadEditorState.copy(
            cursorPosition = position.coerceIn(0, uploadEditorState.text.length)
        ))
    }

    fun undoEditorDocument() {
        val state = uploadEditorState
        if (state.busy) return
        val previous = editorDocumentHistory.undo(editorSnapshot(state)) ?: return
        uploadEditorState = restoreEditorDocument(state, previous, "撤销上一步编辑")
    }

    fun redoEditorDocument() {
        val state = uploadEditorState
        if (state.busy) return
        val next = editorDocumentHistory.redo(editorSnapshot(state)) ?: return
        uploadEditorState = restoreEditorDocument(state, next, "重做上一步编辑")
    }

    fun updateEditorEncoding(value: String) {
        uploadEditorState = uploadEditorState.copy(encoding = value, actionMessage = null)
    }

    fun updateEditorMetadata(value: EditorBookMetadata) {
        uploadEditorState = uploadEditorState.copy(metadata = value, actionMessage = null)
    }

    fun updateEditorSplitMode(value: EditorSplitMode) {
        uploadEditorState = uploadEditorState.copy(splitMode = value, actionMessage = null)
    }

    fun updateEditorSplitPattern(value: String) {
        uploadEditorState = uploadEditorState.copy(splitPattern = value, actionMessage = null)
    }

    fun updateEditorSplitTarget(value: String) {
        uploadEditorState = uploadEditorState.copy(splitTarget = value.filter(Char::isDigit), actionMessage = null)
    }

    fun updateEditorCustomScript(value: String) {
        uploadEditorState = uploadEditorState.copy(customScript = value, actionMessage = null)
    }

    fun updateEditorScriptChunked(value: Boolean) {
        uploadEditorState = uploadEditorState.copy(scriptChunked = value, actionMessage = null)
    }

    fun updateEditorScriptChunkSize(value: String) {
        uploadEditorState = uploadEditorState.copy(scriptChunkSize = value.filter(Char::isDigit), actionMessage = null)
    }

    fun updateEditorApiEndpoint(value: String) {
        uploadEditorState = uploadEditorState.copy(apiEndpoint = value, actionMessage = null)
    }

    fun updateEditorApiTimeout(value: String) {
        uploadEditorState = uploadEditorState.copy(apiTimeoutSeconds = value.filter(Char::isDigit), actionMessage = null)
    }

    fun updateEditorApiMarkerMode(value: EditorMarkerMode) {
        uploadEditorState = uploadEditorState.copy(apiMarkerMode = value, actionMessage = null)
    }

    fun updateEditorBatchMode(value: EditorBatchMode) {
        val state = uploadEditorState
        val target = if (state.batchTarget == state.batchMode.defaultTarget) value.defaultTarget else state.batchTarget
        uploadEditorState = state.copy(batchMode = value, batchTarget = target, actionMessage = null)
    }

    fun updateEditorBatchTarget(value: String) {
        uploadEditorState = uploadEditorState.copy(batchTarget = value.filter(Char::isDigit), actionMessage = null)
    }

    fun selectEditorAiConfig(id: Long) {
        if (uploadEditorState.aiConfigs.none { it.id == id }) return
        uploadEditorState = uploadEditorState.copy(selectedAiConfigId = id, actionMessage = null)
    }

    fun generateEditorRegexWithAi() {
        val state = uploadEditorState
        if (state.busy) return
        if (state.chapters.size < 2) {
            uploadEditorState = state.copy(actionMessage = "请先生成至少两个章节标题")
            return
        }
        val config = state.aiConfigs.firstOrNull { it.id == state.selectedAiConfigId }
        if (config == null) {
            uploadEditorState = state.copy(actionMessage = "请先在工作区保存可用的本地 API 配置")
            return
        }
        uploadEditorState = state.copy(busy = true, actionMessage = "正在生成章节正则…")
        viewModelScope.launch {
            val result = runCatching {
                api.generateEditorRegex(
                    endpoint = config.endpoint,
                    apiKey = config.apiKey,
                    model = config.model,
                    chapterTitles = state.chapters.take(20).map { it.title }
                )
            }
            val updated = result.fold(
                onSuccess = { regex ->
                    uploadEditorState.copy(
                        splitMode = EditorSplitMode.Regex,
                        splitPattern = regex,
                        selectedTab = EditorTab.Split,
                        busy = false,
                        actionMessage = "AI 已生成正则，请检查后再执行分章"
                    )
                },
                onFailure = { failure ->
                    uploadEditorState.copy(
                        busy = false,
                        actionMessage = apiFailureMessage("AI 生成正则", failure)
                    )
                }
            )
        }
    }

    fun updateEditorFind(value: String) {
        uploadEditorState = uploadEditorState.copy(findText = value, actionMessage = null)
    }

    fun updateEditorReplace(value: String) {
        uploadEditorState = uploadEditorState.copy(replaceText = value, actionMessage = null)
    }

    fun updateEditorFindUsesRegex(value: Boolean) {
        uploadEditorState = uploadEditorState.copy(findUsesRegex = value, actionMessage = null)
    }

    fun updateEditorArchiveName(value: String) {
        uploadEditorState = uploadEditorState.copy(archiveName = value, actionMessage = null)
    }

    fun queueEditorDocuments(rawUris: List<String>) {
        if (uploadEditorState.busy || rawUris.isEmpty()) return
        val serial = ++editorRequestSerial
        uploadEditorState = uploadEditorState.copy(busy = true, actionMessage = "正在加入文件…")
        viewModelScope.launch {
            val result = runCatching {
                val documents = mutableListOf<UploadDocument>()
                for (rawUri in rawUris.distinct()) {
                    documents += readUploadDocument(rawUri)
                }
                documents
            }
            if (!isFreshRequestSerial(serial, editorRequestSerial)) return@launch
            uploadEditorState = result.fold(
                onSuccess = { documents ->
                    val previous = uploadEditorState.files.associateBy(UploadDocument::uri)
                    val merged = (previous.values + documents).distinctBy(UploadDocument::uri)
                    uploadEditorState.copy(
                        files = merged,
                        selectedTab = EditorTab.Files,
                        busy = false,
                        actionMessage = "已加入 ${documents.size} 个文件"
                    )
                },
                onFailure = { failure ->
                    uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("加入编辑文件", failure))
                }
            )
        }
    }

    fun removeQueuedEditorDocument(rawUri: String) {
        if (uploadEditorState.busy) return
        val updated = uploadEditorState.files.filterNot { it.uri == rawUri }
        if (updated.size == uploadEditorState.files.size) return
        uploadEditorState = uploadEditorState.copy(files = updated, actionMessage = "文件已移除")
    }

    fun selectEditorDocument(rawUri: String) {
        if (uploadEditorState.busy) return
        val serial = ++editorRequestSerial
        uploadEditorState = uploadEditorState.copy(busy = true, actionMessage = "正在打开文件…")
        viewModelScope.launch {
            val result = runCatching {
                val document = readUploadDocument(rawUri)
                if (document.displayName.endsWith(".epub", ignoreCase = true)) {
                    val parsed = withContext(Dispatchers.IO) { EpubParser.parse(uploadSource(document)) }
                    EditorLoadResult(
                        document = document,
                        text = EditorProcessor.toWebsiteIdentifiers(parsed.chapters),
                        metadata = EditorBookMetadata(
                            title = parsed.title,
                            author = parsed.author,
                            description = parsed.description,
                            language = parsed.language
                        ),
                        chapters = parsed.chapters
                    )
                } else {
                    EditorLoadResult(
                        document = document,
                        text = readEditorText(document, uploadEditorState.encoding),
                        metadata = uploadEditorState.metadata,
                        chapters = emptyList()
                    )
                }
            }
            if (!isFreshRequestSerial(serial, editorRequestSerial)) return@launch
            uploadEditorState = result.fold(
                onSuccess = { loaded ->
                    replaceEditorDocument(uploadEditorState.copy(
                        text = loaded.text,
                        cursorPosition = 0,
                        fileName = loaded.document.displayName,
                        files = (uploadEditorState.files + loaded.document).distinctBy(UploadDocument::uri),
                        metadata = loaded.metadata,
                        chapters = loaded.chapters,
                        markerValidationErrors = emptyList(),
                        selectedTab = if (loaded.chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
                        busy = false,
                        actionMessage = if (loaded.chapters.isEmpty()) "文件已加载，请配置分章规则" else "EPUB 已加载，共 ${loaded.chapters.size} 章"
                    ))
                },
                onFailure = { failure ->
                    uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("打开编辑文件", failure))
                }
            )
        }
    }

    fun importQueuedEditorDocuments() {
        val state = uploadEditorState
        if (state.busy) return
        if (state.files.isEmpty()) {
            uploadEditorState = state.copy(actionMessage = "请先在文件面板添加 .txt、.md、.epub 或 .zip 文件")
            return
        }
        val serial = ++editorRequestSerial
        uploadEditorState = state.copy(busy = true, actionMessage = "正在批量导入 ${state.files.size} 个文件…")
        viewModelScope.launch {
            val result = runCatching { importEditorDocuments(state.files, state.encoding) }
            if (!isFreshRequestSerial(serial, editorRequestSerial)) return@launch
            uploadEditorState = result.fold(
                onSuccess = { chapters ->
                    val fileName = if (state.files.size == 1) state.files.single().displayName
                    else "批量导入（${state.files.size} 个文件）"
                    replaceEditorDocument(state.copy(
                        text = EditorProcessor.toWebsiteIdentifiers(chapters),
                        cursorPosition = 0,
                        fileName = fileName,
                        chapters = chapters,
                        markerValidationErrors = emptyList(),
                        selectedTab = EditorTab.Chapters,
                        busy = false,
                        actionMessage = "已从 ${state.files.size} 个文件导入 ${chapters.size} 章"
                    ))
                },
                onFailure = { failure ->
                    uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("批量导入编辑文件", failure))
                }
            )
        }
    }

    fun processEditorSplit() {
        val state = uploadEditorState
        if (state.text.isBlank()) {
            uploadEditorState = state.copy(actionMessage = "请先加载或输入文本")
            return
        }
        editorSplitTargetError(
            state.splitMode,
            editorSplitPattern(state),
            editorSplitTarget(state),
            state.customScript,
            state.scriptChunked,
            state.scriptChunkSize
        )?.let { error ->
            uploadEditorState = state.copy(actionMessage = error)
            return
        }
        if (state.splitMode == EditorSplitMode.CustomScript) {
            uploadEditorState = state.copy(
                busy = true,
                scriptRunId = state.scriptRunId + 1,
                actionMessage = "正在本地沙箱执行脚本…"
            )
            return
        }
        if (state.splitMode == EditorSplitMode.ApiProcess) {
            processEditorThroughApi(state)
            return
        }
        if (state.splitMode == EditorSplitMode.Manual) {
            uploadEditorState = state.copy(actionMessage = "请使用下方的标识符手动工具")
            return
        }
        val result = runCatching {
            when (state.splitMode) {
                EditorSplitMode.Regex -> EditorProcessor.splitByRegex(
                    state.text,
                    state.splitPattern.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                )
                EditorSplitMode.MarkdownH1 -> EditorProcessor.splitByMarkdown(state.text, 1)
                EditorSplitMode.MarkdownH2 -> EditorProcessor.splitByMarkdown(state.text, 2)
                EditorSplitMode.KeywordNumber -> EditorProcessor.splitByKeywordNumber(
                    state.text,
                    state.splitPattern.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                )
                EditorSplitMode.CharacterCount -> EditorProcessor.splitByCharacterCount(state.text, state.splitTarget.toInt())
                EditorSplitMode.ParagraphCount -> EditorProcessor.splitByParagraphCount(state.text, state.splitTarget.toInt())
                EditorSplitMode.BatchGenerate -> when (state.batchMode) {
                    EditorBatchMode.Paragraphs -> EditorProcessor.splitByParagraphCount(state.text, state.batchTarget.toInt())
                    EditorBatchMode.Characters -> EditorProcessor.splitByCharacterInterval(state.text, state.batchTarget.toInt())
                    EditorBatchMode.Chapters -> EditorProcessor.splitEvenlyByChapterCount(state.text, state.batchTarget.toInt())
                }
                EditorSplitMode.ApiProcess,
                EditorSplitMode.CustomScript,
                EditorSplitMode.Manual -> emptyList()
            }
        }
        val updated = result.fold(
            onSuccess = { chapters ->
                if (chapters.isEmpty()) state.copy(actionMessage = "没有匹配到章节标题，请调整规则")
                else state.copy(
                    text = EditorProcessor.toWebsiteIdentifiers(chapters),
                    cursorPosition = 0,
                    chapters = chapters,
                    markerValidationErrors = emptyList(),
                    selectedTab = EditorTab.Chapters,
                    actionMessage = "已生成 ${chapters.size} 章"
                )
            },
            onFailure = { failure -> state.copy(actionMessage = "分章失败：${failure.message ?: "规则无效"}") }
        )
        uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
            commitEditorDocumentChange(state, updated)
        } else {
            withEditorHistoryFlags(updated)
        }
    }

    private fun editorSplitPattern(state: UploadEditorState): String = when (state.splitMode) {
        EditorSplitMode.ApiProcess -> state.apiEndpoint
        else -> state.splitPattern
    }

    private fun editorSplitTarget(state: UploadEditorState): String = when (state.splitMode) {
        EditorSplitMode.ApiProcess -> state.apiTimeoutSeconds
        EditorSplitMode.BatchGenerate -> state.batchTarget
        else -> state.splitTarget
    }

    private fun processEditorThroughApi(state: UploadEditorState) {
        val serial = ++editorProcessorRequestSerial
        val processorInput = when (state.apiMarkerMode) {
            EditorMarkerMode.Incremental -> state.text
            EditorMarkerMode.Full -> EditorProcessor.clearWebsiteIdentifiers(state.text)
        }
        uploadEditorState = state.copy(busy = true, actionMessage = "正在发送文本到接口处理…")
        viewModelScope.launch {
            val result = runCatching {
                api.processEditorTextWithApi(
                    endpoint = state.apiEndpoint,
                    text = processorInput,
                    timeoutSeconds = state.apiTimeoutSeconds.toInt()
                )
            }
            if (!isFreshRequestSerial(serial, editorProcessorRequestSerial)) return@launch
            val updated = result.fold(
                onSuccess = { processedText ->
                    val markerErrors = EditorProcessor.validateWebsiteIdentifiers(processedText)
                    val chapters = if (markerErrors.isEmpty()) {
                        EditorProcessor.parseWebsiteIdentifiers(processedText)
                    } else {
                        emptyList()
                    }
                    uploadEditorState.copy(
                        text = processedText,
                        cursorPosition = 0,
                        chapters = chapters,
                        markerValidationErrors = markerErrors,
                        selectedTab = if (chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
                        busy = false,
                        actionMessage = when {
                            markerErrors.isNotEmpty() -> "接口处理已返回文本，但章节标识符需要修复"
                            chapters.isEmpty() -> "接口处理完成，返回文本未包含章节标识符"
                            else -> "接口处理完成，已识别 ${chapters.size} 章"
                        }
                    )
                },
                onFailure = { failure ->
                    uploadEditorState.copy(
                        busy = false,
                        actionMessage = apiFailureMessage("接口处理", failure)
                    )
                }
            )
            uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
                commitEditorDocumentChange(state, updated)
            } else {
                withEditorHistoryFlags(updated)
            }
        }
    }

    fun insertEditorTitleMarkerAtCursor() {
        applyEditorMarkerOperation(
            successMessage = "已插入标题标识符",
            cursorPosition = ::standaloneEditorMarkerCursor
        ) { state ->
            EditorProcessor.insertWebsiteTitleMarkerAtCursor(state.text, state.cursorPosition)
        }
    }

    fun insertEditorContentMarkerAtCursor() {
        applyEditorMarkerOperation(
            successMessage = "已插入内容标识符",
            cursorPosition = ::standaloneEditorMarkerCursor
        ) { state ->
            EditorProcessor.insertWebsiteContentMarkerAtCursor(state.text, state.cursorPosition)
        }
    }

    fun insertEditorChapterAtCursor() {
        applyEditorMarkerOperation("已在光标位置插入新章节") { state ->
            EditorProcessor.insertWebsiteChapterAtCursor(state.text, state.cursorPosition)
        }
    }

    fun deleteEditorChapterAtCursor() {
        applyEditorMarkerOperation("已删除光标所在章节并重新编号") { state ->
            EditorProcessor.deleteWebsiteChapterAtCursor(state.text, state.cursorPosition)
        }
    }

    fun renumberEditorMarkers() {
        applyEditorMarkerOperation("已重新编号所有章节标识符") { state ->
            EditorProcessor.renumberWebsiteIdentifiers(state.text)
        }
    }

    fun clearEditorMarkers() {
        val state = uploadEditorState
        val cleared = EditorProcessor.clearWebsiteIdentifiers(state.text)
        val updated = state.copy(
            text = cleared,
            cursorPosition = state.cursorPosition.coerceIn(0, cleared.length),
            chapters = emptyList(),
            markerValidationErrors = emptyList(),
            selectedTab = EditorTab.Text,
            actionMessage = "已清除所有章节标识符"
        )
        uploadEditorState = if (cleared == state.text) {
            withEditorHistoryFlags(updated)
        } else {
            commitEditorDocumentChange(state, updated)
        }
    }

    fun validateEditorMarkers() {
        val state = uploadEditorState
        val errors = EditorProcessor.validateWebsiteIdentifiers(state.text)
        uploadEditorState = state.copy(
            markerValidationErrors = errors,
            actionMessage = if (errors.isEmpty()) "所有章节标识符验证通过" else "发现 ${errors.size} 个章节标识符问题"
        )
    }

    private fun standaloneEditorMarkerCursor(state: UploadEditorState, updatedText: String): Int {
        val cursor = state.cursorPosition.coerceIn(0, state.text.length)
        val prefixLength = if (cursor > 0 && !state.text.substring(0, cursor).endsWith("\n")) 1 else 0
        return (cursor + prefixLength + "##__T[00001]__##".length).coerceIn(0, updatedText.length)
    }

    private fun applyEditorMarkerOperation(
        successMessage: String,
        cursorPosition: (UploadEditorState, String) -> Int = { state, updatedText ->
            state.cursorPosition.coerceIn(0, updatedText.length)
        },
        operation: (UploadEditorState) -> String
    ) {
        val state = uploadEditorState
        val result = runCatching { operation(state) }
        val updated = result.fold(
            onSuccess = { updatedText ->
                val errors = EditorProcessor.validateWebsiteIdentifiers(updatedText)
                val chapters = if (errors.isEmpty()) EditorProcessor.parseWebsiteIdentifiers(updatedText) else emptyList()
                state.copy(
                    text = updatedText,
                    cursorPosition = cursorPosition(state, updatedText).coerceIn(0, updatedText.length),
                    chapters = chapters,
                    markerValidationErrors = errors,
                    selectedTab = if (chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
                    actionMessage = if (errors.isEmpty()) successMessage else "$successMessage，但仍有标识符问题"
                )
            },
            onFailure = { failure ->
                state.copy(actionMessage = "标识符处理失败：${failure.message ?: "规则无效"}")
            }
        )
        uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
            commitEditorDocumentChange(state, updated)
        } else {
            withEditorHistoryFlags(updated)
        }
    }

    fun completeEditorCustomScript(runId: Long, processedText: String?, error: String?) {
        val state = uploadEditorState
        if (state.scriptRunId != runId || !state.busy) return
        if (!error.isNullOrBlank() || processedText == null) {
            uploadEditorState = state.copy(
                busy = false,
                actionMessage = "脚本执行失败：${error ?: "未返回文本"}"
            )
            return
        }
        val markerErrors = EditorProcessor.validateWebsiteIdentifiers(processedText)
        val chapters = if (markerErrors.isEmpty()) EditorProcessor.parseWebsiteIdentifiers(processedText) else emptyList()
        uploadEditorState = commitEditorDocumentChange(state, state.copy(
            text = processedText,
            cursorPosition = 0,
            chapters = chapters,
            markerValidationErrors = markerErrors,
            selectedTab = if (chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
            busy = false,
            actionMessage = if (markerErrors.isNotEmpty()) {
                "脚本处理完成，但章节标识符需要修复"
            } else if (chapters.isEmpty()) {
                "脚本处理完成；未发现网站章节标识，已保留处理后的文本"
            } else {
                "脚本处理完成，已生成 ${chapters.size} 章"
            }
        ))
    }

    fun replaceEditorText() {
        val state = uploadEditorState
        if (state.findText.isEmpty()) {
            uploadEditorState = state.copy(actionMessage = "请输入查找内容")
            return
        }
        val result = runCatching {
            if (state.findUsesRegex) state.text.replace(Regex(state.findText), state.replaceText)
            else state.text.replace(state.findText, state.replaceText)
        }
        val updated = result.fold(
            onSuccess = { replaced ->
                val changed = replaced != state.text
                state.copy(
                    text = replaced,
                    cursorPosition = state.cursorPosition.coerceIn(0, replaced.length),
                    chapters = if (changed) emptyList() else state.chapters,
                    markerValidationErrors = if (changed) emptyList() else state.markerValidationErrors,
                    selectedTab = if (changed) EditorTab.Text else state.selectedTab,
                    actionMessage = if (changed) "替换完成" else "未找到匹配项"
                )
            },
            onFailure = { failure -> state.copy(actionMessage = "替换失败：${failure.message ?: "正则无效"}") }
        )
        uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
            commitEditorDocumentChange(state, updated)
        } else {
            withEditorHistoryFlags(updated)
        }
    }

    fun updateEditorChapter(index: Int, title: String, content: String) {
        val before = uploadEditorState
        if (index !in uploadEditorState.chapters.indices) return
        val next = uploadEditorState.chapters.toMutableList()
        next[index] = next[index].copy(title = title.trim().ifBlank { "第 ${index + 1} 章" }, content = content)
        uploadEditorState = uploadEditorState.copy(chapters = next, actionMessage = "章节已更新")
        uploadEditorState = commitEditorDocumentChange(before, uploadEditorState)
    }

    fun addEditorChapter() {
        val before = uploadEditorState
        val nextIndex = uploadEditorState.chapters.size
        uploadEditorState = uploadEditorState.copy(
            chapters = uploadEditorState.chapters + UploadChapter("第 ${nextIndex + 1} 章", "", nextIndex + 1),
            actionMessage = "已添加章节"
        )
        uploadEditorState = commitEditorDocumentChange(before, uploadEditorState)
    }

    fun deleteEditorChapter(index: Int) {
        val before = uploadEditorState
        if (index !in uploadEditorState.chapters.indices) return
        val next = uploadEditorState.chapters.filterIndexed { chapterIndex, _ -> chapterIndex != index }
            .mapIndexed { chapterIndex, chapter -> chapter.copy(chapterNumber = chapterIndex + 1) }
        uploadEditorState = uploadEditorState.copy(chapters = next, actionMessage = "章节已删除并重新编号")
        uploadEditorState = commitEditorDocumentChange(before, uploadEditorState)
    }

    fun saveEditorArchive() {
        val state = uploadEditorState
        if (state.text.isBlank() && state.chapters.isEmpty()) {
            uploadEditorState = state.copy(actionMessage = "没有可保存的编辑内容")
            return
        }
        if (state.busy) return
        uploadEditorState = state.copy(busy = true, actionMessage = "正在保存存档…")
        viewModelScope.launch {
            val result = runCatching {
                val timestamp = System.currentTimeMillis()
                val archive = EditorArchive(
                    id = "archive_${timestamp}_${(0..9999).random()}",
                    name = state.archiveName.trim().ifBlank { state.metadata.title.ifBlank { "存档 $timestamp" } },
                    timestamp = timestamp,
                    textContent = state.text,
                    metadata = state.metadata,
                    fileName = state.fileName,
                    chapterCount = state.chapters.size,
                    totalWords = state.chapters.sumOf { it.content.length }.takeIf { it > 0 } ?: state.text.length
                )
                withContext(Dispatchers.IO) { editorArchiveStore.save(archive) }
            }
            uploadEditorState = result.fold(
                onSuccess = {
                    uploadEditorState.copy(
                        archiveName = "",
                        archives = editorArchiveStore.list(),
                        busy = false,
                        actionMessage = "存档已保存"
                    )
                },
                onFailure = { failure -> uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("保存存档", failure)) }
            )
        }
    }

    fun loadEditorArchive(id: String) {
        if (uploadEditorState.busy) return
        val request = ++editorRequestSerial
        val before = uploadEditorState
        uploadEditorState = before.copy(busy = true, actionMessage = "正在校验存档…")
        viewModelScope.launch {
            val result = try { Result.success(withContext(Dispatchers.IO) { editorArchiveStore.load(id) }) }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (failure: Exception) { Result.failure(failure) }
            if (request != editorRequestSerial) return@launch
            val current = uploadEditorState.copy(busy = false)
            if (current.text != before.text || current.metadata != before.metadata) {
                uploadEditorState = current.copy(actionMessage = "编辑内容已变化，未用存档覆盖新草稿")
                return@launch
            }
            uploadEditorState = result.fold(onSuccess = { archive ->
                if (archive == null) current.copy(actionMessage = "存档不存在，当前编辑内容保留")
                else replaceEditorDocument(current.copy(text = archive.textContent, metadata = archive.metadata,
                    fileName = archive.fileName, chapters = emptyList(), selectedTab = EditorTab.Text,
                    actionMessage = "存档已加载，请重新生成章节目录"))
            }, onFailure = { current.copy(actionMessage = apiFailureMessage("读取存档", it)) })
        }
    }

    fun deleteEditorArchive(id: String) {
        runCatching { editorArchiveStore.delete(id) }
            .onSuccess { uploadEditorState = uploadEditorState.copy(archives = editorArchiveStore.list(), actionMessage = "存档已删除") }
            .onFailure { uploadEditorState = uploadEditorState.copy(actionMessage = apiFailureMessage("删除存档", it)) }
    }

    fun clearEditorArchives() {
        runCatching { editorArchiveStore.clear() }
            .onSuccess { uploadEditorState = uploadEditorState.copy(archives = emptyList(), actionMessage = "所有存档已清空") }
            .onFailure { uploadEditorState = uploadEditorState.copy(actionMessage = apiFailureMessage("清空存档", it)) }
    }

    fun exportEditorEpub(rawUri: String) {
        val state = uploadEditorState
        validateEditorOutput(state)?.let { error ->
            uploadEditorState = state.copy(actionMessage = error)
            return
        }
        uploadEditorState = state.copy(busy = true, actionMessage = "正在生成 EPUB…")
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val uri = Uri.parse(rawUri)
                    getApplication<Application>().contentResolver.openOutputStream(uri, "w")?.use { output ->
                        EpubWriter.write(output, state.metadata, state.chapters)
                    } ?: throw IOException("无法写入目标文件")
                }
            }
            uploadEditorState = result.fold(
                onSuccess = { uploadEditorState.copy(busy = false, actionMessage = "EPUB 已生成") },
                onFailure = { failure -> uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("生成 EPUB", failure)) }
            )
        }
    }

    fun sendEditorToUpload() {
        val state = uploadEditorState
        validateEditorOutput(state)?.let { error ->
            uploadEditorState = state.copy(actionMessage = error)
            return
        }
        uploadEditorState = state.copy(busy = true, actionMessage = "正在生成上传文件…")
        val appendBookId = routes.asReversed().filterIsInstance<AppRoute.BookAppend>().firstOrNull()?.bookId
        val requestSerial = ++editorRequestSerial
        val environmentRevision = dependencies.environment.revision
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val safeTitle = state.metadata.title.replace(Regex("[^A-Za-z0-9\\p{L}\\p{N}._-]"), "_").take(48).ifBlank { "novalpie" }
                    val file = File(getApplication<Application>().cacheDir, "${safeTitle}_${System.currentTimeMillis()}.epub")
                    file.outputStream().use { EpubWriter.write(it, state.metadata, state.chapters) }
                    file
                }
            }
            result.onSuccess { file ->
                if (requestSerial != editorRequestSerial || environmentRevision != dependencies.environment.revision || currentRoute != AppRoute.UploadEditor) {
                    uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "页面已变化，未发送到其他书籍；编辑草稿保留")
                    return@onSuccess
                }
                val adopted = uploadFeature.adoptFromEditor(UploadBookState(
                    existingNovelId = appendBookId,
                    draft = UploadBookDraft(
                        title = state.metadata.title,
                        author = state.metadata.author,
                        description = state.metadata.description,
                        language = state.metadata.language,
                        isAdult = state.metadata.isAdult,
                        source = state.metadata.source,
                        sourceUrl = state.metadata.sourceUrl,
                        tagsText = state.metadata.tags,
                        chapterCount = state.chapters.size
                    ),
                    selectedFile = UploadDocument(
                        uri = file.toURI().toString(),
                        displayName = file.name,
                        sizeBytes = file.length(),
                        mimeType = "application/epub+zip"
                    ),
                    chapters = LoadResult.Success(state.chapters),
                    actionMessage = "编辑器内容已准备好，请核对后确认上传"
                )) { requestSerial == editorRequestSerial && environmentRevision == dependencies.environment.revision && currentRoute == AppRoute.UploadEditor }
                if (!adopted) {
                    uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "目标书籍仍有上传任务，请完成后重试；编辑草稿保留")
                    return@onSuccess
                }
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "已发送到上传页")
                val target = appendBookId?.let { AppRoute.BookAppend(it) } ?: AppRoute.UploadBook
                val withoutEditor = routes.toMutableList().apply {
                    if (lastOrNull() is AppRoute.UploadEditor) removeAt(lastIndex)
                }
                navigator.replaceAll(pushDistinctRoute(withoutEditor, target))
            }.onFailure { failure ->
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("生成上传文件", failure))
            }
        }
    }

    fun clearUploadEditor() {
        editorRequestSerial++
        editorDocumentHistory.clear()
        uploadEditorState = UploadEditorState(
            archives = editorArchiveStore.list(),
            aiConfigs = uploadEditorState.aiConfigs,
            selectedAiConfigId = uploadEditorState.selectedAiConfigId
        )
    }

    fun openPoliticalExam() {
        refreshPoliticalExamTimer()
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.PoliticalExam))
    }

    fun startPoliticalExam() {
        if (politicalExamState.session == LoadResult.Loading || politicalExamState.submitting) return
        if (authToken.isNullOrBlank()) {
            politicalExamState = politicalExamState.copy(actionMessage = "请先登录后再开始考试")
            return
        }
        politicalExamState = PoliticalExamState(
            phase = PoliticalExamPhase.Landing,
            session = LoadResult.Loading,
            actionMessage = "正在创建考试会话…"
        )
        viewModelScope.launch {
            val result = runCatching { api.startPoliticalExam() }
            politicalExamState = result.fold(
                onSuccess = { session ->
                    PoliticalExamState(
                        phase = PoliticalExamPhase.Active,
                        session = LoadResult.Success(session),
                        answers = PoliticalExamAnswers(
                            singleChoice = List(session.paper.singleChoice.size) { null },
                            multipleChoice = List(session.paper.multipleChoice.size) { emptyList() },
                            trueFalse = List(session.paper.trueFalse.size) { null },
                            fillBlank = List(session.paper.fillBlank.size) { "" }
                        ),
                        remainingTimeSeconds = session.remainingTimeSeconds,
                        deadlineEpochMillis = System.currentTimeMillis() + session.remainingTimeSeconds * 1000L,
                        actionMessage = null
                    )
                },
                onFailure = { failure ->
                    PoliticalExamState(
                        phase = PoliticalExamPhase.Landing,
                        session = LoadResult.Error(apiFailureMessage("开始考试", failure)),
                        actionMessage = apiFailureMessage("开始考试", failure)
                    )
                }
            )
        }
    }

    fun selectPoliticalExamSingle(index: Int, option: Int) {
        val next = politicalExamState.answers.singleChoice.toMutableList()
        if (index !in next.indices) return
        next[index] = option
        politicalExamState = politicalExamState.copy(
            answers = politicalExamState.answers.copy(singleChoice = next),
            actionMessage = null
        )
    }

    fun togglePoliticalExamMultiple(index: Int, option: Int) {
        val next = politicalExamState.answers.multipleChoice.toMutableList()
        if (index !in next.indices) return
        val selected = next[index].toMutableSet()
        if (!selected.add(option)) selected.remove(option)
        next[index] = selected.sorted()
        politicalExamState = politicalExamState.copy(
            answers = politicalExamState.answers.copy(multipleChoice = next),
            actionMessage = null
        )
    }

    fun selectPoliticalExamTrueFalse(index: Int, answer: Boolean) {
        val next = politicalExamState.answers.trueFalse.toMutableList()
        if (index !in next.indices) return
        next[index] = answer
        politicalExamState = politicalExamState.copy(
            answers = politicalExamState.answers.copy(trueFalse = next),
            actionMessage = null
        )
    }

    fun updatePoliticalExamBlank(index: Int, answer: String) {
        val next = politicalExamState.answers.fillBlank.toMutableList()
        if (index !in next.indices) return
        next[index] = answer
        politicalExamState = politicalExamState.copy(
            answers = politicalExamState.answers.copy(fillBlank = next),
            actionMessage = null
        )
    }

    fun tickPoliticalExamTimer() {
        if (politicalExamState.phase != PoliticalExamPhase.Active || politicalExamState.submitting) return
        refreshPoliticalExamTimer()
        if (politicalExamState.remainingTimeSeconds > 0) return
        // Expiry auto-submit fires at most once. See PoliticalExamState.autoSubmitAttempted: a
        // failed submit used to re-enter this path immediately and loop against the server.
        if (politicalExamState.autoSubmitAttempted) return
        politicalExamState = politicalExamState.copy(autoSubmitAttempted = true)
        submitPoliticalExam()
    }

    private fun refreshPoliticalExamTimer() {
        val deadline = politicalExamState.deadlineEpochMillis ?: return
        val remaining = ((deadline - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L).toInt()
        if (remaining != politicalExamState.remainingTimeSeconds) {
            politicalExamState = politicalExamState.copy(remainingTimeSeconds = remaining)
        }
    }

    fun submitPoliticalExam() {
        val state = politicalExamState
        if (state.phase != PoliticalExamPhase.Active || state.submitting) return
        val session = (state.session as? LoadResult.Success)?.value ?: return
        politicalExamState = state.copy(submitting = true, actionMessage = "正在提交考试…")
        viewModelScope.launch {
            val result = runCatching { api.submitPoliticalExam(session.sessionId, state.answers) }
            result.onSuccess { examResult ->
                politicalExamState = politicalExamState.copy(
                    phase = PoliticalExamPhase.Result,
                    result = LoadResult.Success(examResult),
                    submitting = false,
                    deadlineEpochMillis = null,
                    actionMessage = if (examResult.passed) "考试通过" else "考试未通过"
                )
                examResult.token?.takeIf(String::isNotBlank)?.let { replacementToken ->
                    authSessionStore.saveToken(replacementToken)
                    authToken = replacementToken
                    AppContainer.from(getApplication()).refreshEnvironmentFromStores()
                    loadHome()
                }
            }.onFailure { failure ->
                politicalExamState = politicalExamState.copy(
                    submitting = false,
                    result = LoadResult.Error(apiFailureMessage("提交考试", failure)),
                    actionMessage = apiFailureMessage("提交考试", failure)
                )
            }
        }
    }

    fun resetPoliticalExam() {
        politicalExamState = PoliticalExamState()
    }

    private fun validateEditorOutput(state: UploadEditorState): String? = when {
        state.metadata.title.isBlank() -> "请填写书名"
        state.metadata.author.isBlank() -> "请填写作者"
        state.chapters.isEmpty() -> "请先生成章节目录"
        else -> null
    }

    private suspend fun readEditorText(document: UploadDocument, encoding: String): String = withContext(Dispatchers.IO) {
        val charset = runCatching { Charset.forName(encoding) }.getOrElse { throw IOException("不支持的编码：$encoding") }
        InputStreamReader(uploadSource(document).openStream(), charset).use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(16 * 1024)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                result.append(buffer, 0, read)
                if (result.length > 50_000_000) throw IOException("文本超过 5000 万字符，请先分割文件")
            }
            result.toString()
        }
    }

    private suspend fun importEditorDocuments(
        documents: List<UploadDocument>,
        encoding: String
    ): List<UploadChapter> = withContext(Dispatchers.IO) {
        val charset = runCatching { Charset.forName(encoding) }
            .getOrElse { throw IOException("不支持的编码：$encoding") }
        val imported = mutableListOf<UploadChapter>()

        documents.forEach { document ->
            when {
                document.displayName.endsWith(".epub", ignoreCase = true) -> {
                    imported += EpubParser.parse(uploadSource(document)).chapters
                }
                document.displayName.endsWith(".zip", ignoreCase = true) -> {
                    val entries = EditorBatchImporter.readArchive(uploadSource(document).openStream(), charset)
                    entries.forEach { entry ->
                        imported += UploadChapter(
                            title = editorBatchChapterTitle(entry.displayName, imported.size + 1),
                            content = entry.text,
                            chapterNumber = imported.size + 1
                        )
                    }
                }
                EditorBatchImporter.isTextFile(document.displayName) -> {
                    imported += UploadChapter(
                        title = editorBatchChapterTitle(document.displayName, imported.size + 1),
                        content = readEditorText(document, encoding),
                        chapterNumber = imported.size + 1
                    )
                }
                else -> throw IOException("不支持的批量文件：${document.displayName}")
            }
        }

        if (imported.isEmpty()) throw IOException("没有可导入的章节内容")
        val totalCharacters = imported.sumOf { it.title.length + it.content.length }
        if (totalCharacters > EditorBatchImporter.MAX_TOTAL_CHARACTERS) {
            throw IOException("批量文本超过 5000 万字符，请减少文件后重试")
        }
        imported.mapIndexed { index, chapter ->
            chapter.copy(
                title = chapter.title.trim().ifBlank { "第 ${index + 1} 章" },
                chapterNumber = index + 1
            )
        }
    }

    private fun editorBatchChapterTitle(displayName: String, index: Int): String = displayName
        .substringBeforeLast('.', displayName)
        .replace(Regex("[\\r\\n\\t]"), " ")
        .trim()
        .ifBlank { "第 $index 章" }

    private suspend fun readUploadDocument(rawUri: String): UploadDocument = withContext(Dispatchers.IO) {
        val uri = Uri.parse(rawUri)
        val resolver = getApplication<Application>().contentResolver
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "book.epub"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        if (size < 0L) {
            size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
        }
        UploadDocument(
            uri = uri.toString(),
            displayName = displayName,
            sizeBytes = size,
            mimeType = resolver.getType(uri)
        )
    }

    private fun uploadSource(
        document: UploadDocument,
        fallbackContentType: String = "application/epub+zip"
    ): UploadFileSource {
        val resolver = getApplication<Application>().contentResolver
        val uri = Uri.parse(document.uri)
        return UploadFileSource(
            fileName = document.displayName,
            sizeBytes = document.sizeBytes,
            contentType = document.mimeType ?: fallbackContentType,
            openStream = {
                if (uri.scheme == "file") {
                    val path = uri.path ?: throw IOException("本地文件路径无效")
                    FileInputStream(File(path))
                } else {
                    resolver.openInputStream(uri) ?: throw IOException("无法读取所选文件")
                }
            }
        )
    }

    private data class EditorLoadResult(
        val document: UploadDocument,
        val text: String,
        val metadata: EditorBookMetadata,
        val chapters: List<UploadChapter>
    )

    fun openWorkspace() {
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.Workspace))
        loadWorkspace()
    }

    fun openWorkspaceTranslation(bookId: Long) {
        workspaceState = workspaceState.copy(selectedTab = WorkspaceTab.Queue, translationBookId = bookId)
        openWorkspace()
    }
    fun consumeWorkspaceTranslationBook() { workspaceState = workspaceState.copy(translationBookId = null) }

    fun selectWorkspaceTab(tab: WorkspaceTab) {
        workspaceState = workspaceState.copy(selectedTab = tab, actionMessage = null)
    }

    fun loadWorkspace() = workspaceFeature.load()
    fun saveWorkspaceApi(draft: WorkspaceApiDraft) = workspaceFeature.saveApi(draft)
    fun deleteWorkspaceLocalApi(config: WorkspaceLocalApiConfig) = workspaceFeature.deleteLocal(config)
    fun deleteWorkspaceServerApi(config: WorkspaceApiConfig) = workspaceFeature.deleteServer(config.id)
    fun toggleWorkspaceApi(config: WorkspaceApiConfig) = workspaceFeature.toggleApi(config.id)
    fun saveWorkspaceCookie(draft: WorkspaceCookieDraft) = workspaceFeature.saveCookie(draft)
    fun toggleWorkspaceCookie(config: com.novalpie.nativeapp.model.WorkspaceCookieConfig) = workspaceFeature.toggleCookie(config.id, !config.isActive)
    fun deleteWorkspaceCookie(config: com.novalpie.nativeapp.model.WorkspaceCookieConfig) = workspaceFeature.deleteCookie(config.id)
    fun dismissWorkspaceFailedDrafts() = workspaceFeature.dismissFailedDrafts()
    fun restoreWorkspaceLegacy() = workspaceFeature.restoreLegacy()

    fun updateWorkspaceJobStatus(job: WorkspaceTranslationJob, status: String) {
        workspaceLocalStore.upsertJob(job.copy(status = status, updatedAt = System.currentTimeMillis().toString()))
        workspaceState = workspaceState.copy(
            jobs = workspaceLocalStore.loadJobs(),
            actionMessage = "任务状态已更新"
        )
    }

    fun deleteWorkspaceJob(job: WorkspaceTranslationJob) {
        workspaceLocalStore.deleteJob(job.id)
        workspaceState = workspaceState.copy(
            jobs = workspaceLocalStore.loadJobs(),
            actionMessage = "任务已删除"
        )
    }



    fun updateForumSearchQuery(value: String) = forumFeature.updateQuery(value)

    fun selectForumCategory(type: String) = forumFeature.selectCategory(type)

    fun updateForumHideSpoilers(hideSpoilers: Boolean) {
        if(forumState.hideSpoilers==hideSpoilers)return
        forumFeature.setHideSpoilers(hideSpoilers)
        when(val route=currentRoute) {
            AppRoute.Profile -> loadProfile()
            is AppRoute.UserProfileDetail -> loadUserProfile(route.userId)
            else -> Unit
        }
    }

    fun loadForum() = forumFeature.refresh()
    fun goToForumPage(page: Int) = forumFeature.goToPage(page)
    fun loadMoreForum() = forumFeature.loadMore()

    fun openForumPost(postId: Long) {
        if (postId <= 0) return
        val currentStack = routes.toList()
        val nextStack = pushDistinctRoute(currentStack, AppRoute.ForumPostDetail(postId))
        if (nextStack === currentStack) return
        navigator.replaceAll(nextStack)
        loadForumPostDetail(postId)
    }

    fun openForumCreate() {
        if (authToken.isNullOrBlank()) {
            openLoginFallback()
            return
        }
        val profile = currentUserProfile()
        forumCreateState = ForumCreateState(
            isAdmin = isAdminProfile(profile),
            accessMessage = if (profile?.role == "guest") {
                "游客账号不能发帖，请先升级账号"
            } else {
                null
            }
        )
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.ForumCreate))
    }

    fun updateForumCreateDraft(draft: ForumCreateDraft) {
        forumCreateState = forumCreateState.copy(draft = draft, actionMessage = null)
    }

    fun submitForumPost() {
        val state = forumCreateState
        if (state.submitting || state.accessMessage != null) return
        val validation = validateForumCreateDraft(state.draft, state.isAdmin)
        if (!validation.canSubmit) {
            forumCreateState = state.copy(actionMessage = validation.message)
            return
        }
        val draft = state.draft
        forumCreateState = state.copy(submitting = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                api.createForumPost(
                    ForumCreateRequest(
                        type = draft.type,
                        title = draft.title,
                        content = draft.content,
                        tags = draft.tags,
                        poll = draft.takeIf { it.pollEnabled }?.let {
                            ForumPollDraft(
                                question = it.pollQuestion,
                                options = it.pollOptions,
                                allowMultiple = it.pollAllowMultiple,
                                maxChoices = it.pollMaxChoices,
                                endsAt = it.pollEndsAt.takeIf(String::isNotBlank)
                            )
                        }
                    )
                )
            }
            if (currentRoute != AppRoute.ForumCreate) return@launch
            result.fold(
                onSuccess = { created ->
                    if (!created.success) {
                        forumCreateState = forumCreateState.copy(
                            submitting = false,
                            actionMessage = created.message ?: "发布失败"
                        )
                        return@fold
                    }
                    loadForum()
                    val postId = created.postId
                    if (postId != null && postId > 0) {
                        val nextStack = routes.toMutableList().apply {
                            if (lastOrNull() is AppRoute.ForumCreate) removeAt(lastIndex)
                            add(AppRoute.ForumPostDetail(postId))
                        }
                        navigator.replaceAll(nextStack)
                        loadForumPostDetail(postId)
                    } else if (routes.lastOrNull() is AppRoute.ForumCreate) {
                        navigator.pop()
                    }
                    forumCreateState = ForumCreateState(isAdmin = state.isAdmin)
                },
                onFailure = { failure ->
                    forumCreateState = forumCreateState.copy(
                        submitting = false,
                        actionMessage = apiFailureMessage("发布帖子", failure)
                    )
                }
            )
        }
    }

    fun loadForumPostDetail(postId: Long, preservedActionMessage: String? = null, retainVisibleComments: Boolean = false) =
        forumPostFeature.load(postId, preservedActionMessage, retainVisibleComments)
    fun retryForumPostBody() = forumPostFeature.retryBody()
    fun retryForumPostComments() = forumPostFeature.retryComments()
    fun loadMoreForumPostComments() = forumPostFeature.loadMore()
    internal fun forumPostScroll(postId: Long) = forumPostFeature.scroll(postId)
    fun saveForumPostScroll(postId: Long, index: Int, offset: Int) = forumPostFeature.saveScroll(postId, index, offset)

    /**
     * Book reviews use the same source sharing marker as forum comments, but they live under a
     * different detail route. Resolve their references in the ViewModel so a recomposed review
     * card never starts its own request and every nested reply sees the same resolved card.
     */
    private fun loadBookCommentBookReferences(
        bookId: Long,
        comments: List<ChapterComment>,
        requestSerial: Long,
    ) {
        val referenceIds = chapterCommentBookReferenceIds(comments)
        if (
            !isFreshRequestSerial(requestSerial, bookDetailRequestSerial) ||
            !isFreshBookDetailResult(currentRoute, bookDetailState, bookId)
        ) return
        if (referenceIds.isEmpty()) {
            bookDetailState = bookDetailState.copy(bookReferences = emptyMap())
            return
        }

        val previous = bookDetailState.bookReferences
        bookDetailState = bookDetailState.copy(
            bookReferences = referenceIds.associateWith { previous[it] ?: LoadResult.Loading },
        )
        viewModelScope.launch {
            val requests = referenceIds.associateWith { referenceId ->
                async { runCatching { api.bookDetail(referenceId) } }
            }
            val resolved = requests.mapValues { (_, request) ->
                request.await().toLoadResult("关联书籍")
            }
            if (
                !isFreshRequestSerial(requestSerial, bookDetailRequestSerial) ||
                !isFreshBookDetailResult(currentRoute, bookDetailState, bookId)
            ) return@launch
            bookDetailState = bookDetailState.copy(bookReferences = resolved)
        }
    }

    /** Resolve `[bookid:...]` markers embedded in one chapter's inline comment tree. */
    private fun loadReaderCommentBookReferences(
        bookId: Long,
        chapterId: Long,
        comments: List<ChapterComment>,
        requestSerial: Long,
    ) {
        val referenceIds = chapterCommentBookReferenceIds(comments)
        if (
            requestSerial != readerRequestSerial ||
            !isFreshReaderCommentRequest(requestSerial, bookId)
        ) return
        if (referenceIds.isEmpty()) {
            updateReaderChapterCommentState(chapterId) { it.copy(bookReferences = emptyMap()) }
            return
        }

        val previous = readerChapterCommentState(readerState, chapterId).bookReferences
        updateReaderChapterCommentState(chapterId) {
            it.copy(bookReferences = referenceIds.associateWith { previous[it] ?: LoadResult.Loading })
        }
        viewModelScope.launch {
            val requests = referenceIds.associateWith { referenceId ->
                async { runCatching { api.bookDetail(referenceId) } }
            }
            val resolved = requests.mapValues { (_, request) ->
                request.await().toLoadResult("关联书籍")
            }
            if (
                requestSerial != readerRequestSerial ||
                !isFreshReaderCommentRequest(requestSerial, bookId)
            ) return@launch
            updateReaderChapterCommentState(chapterId) { it.copy(bookReferences = resolved) }
        }
    }

    fun updateForumCommentDraft(value: String) = forumPostFeature.draft(value)
    fun replyToForumComment(comment: ForumComment) = forumPostFeature.reply(comment)
    fun cancelForumReply() = forumPostFeature.cancelReply()
    fun toggleForumCommentReplies(commentId: Long) = forumPostFeature.toggleReplies(commentId)
    fun toggleForumPollOption(optionId: Long) = forumPostFeature.toggleVote(optionId)
    fun submitForumPoll() = forumPostFeature.vote(!authToken.isNullOrBlank())
    fun submitForumComment() = forumPostFeature.send()
    fun likeForumPost() = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Like)
    fun dislikeForumPost() = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Dislike)
    fun emojiForumPost() = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Joy)
    fun awardForumPost() = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Award)
    fun likeForumComment(comment: ForumComment) = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Like, comment)
    fun dislikeForumComment(comment: ForumComment) = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Dislike, comment)
    fun emojiForumComment(comment: ForumComment) = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Joy, comment)
    fun awardForumComment(comment: ForumComment) = forumPostFeature.react(com.novalpie.nativeapp.feature.forum.ForumReaction.Award, comment)

    fun updateBookCommentDraft(value: String) {
        bookDetailState = bookDetailState.copy(commentDraft = value)
    }

    fun replyToBookComment(comment: ChapterComment) {
        bookDetailState = bookDetailState.copy(
            replyingToCommentId = chapterCommentReplySubmissionCommentId(comment, currentBookComments()),
            replyingToName = comment.authorName,
            commentDraft = replyComposerDraftForTarget(
                currentDraft = bookDetailState.commentDraft,
                previousTargetName = bookDetailState.replyingToName,
                nextTargetName = comment.authorName,
            ),
        )
    }

    fun cancelBookCommentReply() {
        bookDetailState = bookDetailState.copy(replyingToCommentId = null, replyingToName = null)
    }

    fun submitBookComment() {
        val bookId = bookDetailState.bookId
        val content = bookDetailState.commentDraft.trim()
        if (bookId <= 0 || content.isBlank() || bookDetailState.actionLoading) return
        val replyId = bookDetailState.replyingToCommentId
        val replyToName = bookDetailState.replyingToName
        val identity=ContentMutationIdentity(AppRoute.BookDetail(bookId),bookDetailRequestSerial,dependencies.environment.revision)
        bookDetailState = bookDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            if(!isCurrentContentMutation(identity,currentRoute,bookDetailRequestSerial,dependencies.environment.revision))return@launch
            val result = runCatching {
                if (replyId != null) {
                    api.createCommentReply(commentId = replyId, content = content, replyToName = replyToName)
                } else {
                    api.createBookComment(bookId = bookId, content = content)
                }
            }
            if(!isCurrentContentMutation(identity,currentRoute,bookDetailRequestSerial,dependencies.environment.revision))return@launch
            bookDetailState = bookCommentAfterSubmission(
                bookDetailState,
                result,
            )
            if (result.getOrNull()?.success == true) {
                loadBookDetail(
                    bookId = bookId,
                    preservedActionMessage = bookDetailState.actionMessage,
                    retainCommentComposer = true,
                )
            }
        }
    }

    fun likeBookComment(comment: ChapterComment) {
        val target = chapterCommentActionTarget(comment, currentBookComments())
        reactOnBookComment(comment, "评论点赞") {
            api.toggleCommentLike(target.parentCommentId, target.replyId)
        }
    }

    fun dislikeBookComment(comment: ChapterComment) {
        reactOnBookComment(comment, "评论点踩") { reactToCommentOrReply(comment, currentBookComments(), "down") }
    }

    fun emojiBookComment(comment: ChapterComment) {
        reactOnBookComment(comment, "评论表情") { reactToCommentOrReply(comment, currentBookComments(), "emoji:heart") }
    }

    fun awardBookComment(comment: ChapterComment) {
        reactOnBookComment(comment, "评论打赏") {
            reactToCommentOrReply(comment, currentBookComments(), "award", awardPoints = 10)
        }
    }

    private fun currentBookComments(): List<ChapterComment> =
        (bookDetailState.comments as? LoadResult.Success<List<ChapterComment>>)?.value.orEmpty()

    private fun reactOnBookComment(comment: ChapterComment, label: String, action: suspend () -> com.novalpie.nativeapp.model.ForumActionResult) {
        if (comment.id <= 0 || bookDetailState.bookId <= 0 || bookDetailState.actionLoading) return
        val bookId = bookDetailState.bookId
        val identity=ContentMutationIdentity(AppRoute.BookDetail(bookId),bookDetailRequestSerial,dependencies.environment.revision)
        bookDetailState = bookDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            if(!isCurrentContentMutation(identity,currentRoute,bookDetailRequestSerial,dependencies.environment.revision))return@launch
            val result = runCatching { action() }
            if(!isCurrentContentMutation(identity,currentRoute,bookDetailRequestSerial,dependencies.environment.revision))return@launch
            bookDetailState = result.fold(
                onSuccess = {
                    bookDetailState.copy(
                        actionLoading = false,
                        actionMessage = it.message ?: "$label 已同步"
                    )
                },
                onFailure = {
                    bookDetailState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, it)
                    )
                }
            )
            if (result.getOrNull()?.success == true) {
                loadBookDetail(
                    bookId = bookId,
                    preservedActionMessage = bookDetailState.actionMessage,
                    retainCommentComposer = true,
                )
            }
        }
    }

    fun updateReaderCommentDraft(chapterId: Long, value: String) {
        updateReaderChapterCommentState(chapterId) { it.copy(draft = value) }
    }

    fun replyToReaderComment(chapterId: Long, comment: ChapterComment) {
        updateReaderChapterCommentState(chapterId) { current ->
            current.copy(
                replyingToCommentId = chapterCommentReplySubmissionCommentId(
                    comment,
                    readerChapterComments(chapterId),
                ),
                replyingToName = comment.authorName,
                draft = replyComposerDraftForTarget(
                    currentDraft = current.draft,
                    previousTargetName = current.replyingToName,
                    nextTargetName = comment.authorName,
                ),
            )
        }
    }

    fun cancelReaderCommentReply(chapterId: Long) {
        updateReaderChapterCommentState(chapterId) {
            it.copy(replyingToCommentId = null, replyingToName = null)
        }
    }

    fun submitReaderComment(chapterId: Long) {
        val state = readerState
        val bookId = state.bookId
        val chapterState = readerChapterCommentState(state, chapterId)
        val content = chapterState.draft.trim()
        if (bookId <= 0 || chapterId <= 0 || content.isBlank() || chapterState.actionLoading) return
        val replyId = chapterState.replyingToCommentId
        val replyToName = chapterState.replyingToName
        updateReaderChapterCommentState(chapterId) { it.copy(actionLoading = true, actionMessage = null) }
        val requestSerial = readerRequestSerial
        viewModelScope.launch {
            val result = runCatching {
                if (replyId != null) {
                    api.createCommentReply(commentId = replyId, content = content, replyToName = replyToName)
                } else {
                    api.createChapterComment(bookId = bookId, chapterId = chapterId, content = content)
                }
            }
            if (!isFreshReaderCommentRequest(requestSerial, bookId)) return@launch
            updateReaderChapterCommentState(chapterId) { current ->
                readerChapterCommentAfterSubmission(current, result)
            }
            if (result.getOrNull()?.success == true) refreshReaderChapterComments(chapterId)
        }
    }

    fun likeReaderComment(chapterId: Long, comment: ChapterComment) {
        val availableComments = readerChapterComments(chapterId)
        val target = chapterCommentActionTarget(comment, availableComments)
        reactOnReaderComment(chapterId, comment, "章节评论点赞") {
            api.toggleCommentLike(target.parentCommentId, target.replyId)
        }
    }

    fun dislikeReaderComment(chapterId: Long, comment: ChapterComment) {
        reactOnReaderComment(chapterId, comment, "章节评论点踩") {
            reactToCommentOrReply(comment, readerChapterComments(chapterId), "down")
        }
    }

    fun emojiReaderComment(chapterId: Long, comment: ChapterComment) {
        reactOnReaderComment(chapterId, comment, "章节评论表情") {
            reactToCommentOrReply(comment, readerChapterComments(chapterId), "emoji:heart")
        }
    }

    fun awardReaderComment(chapterId: Long, comment: ChapterComment) {
        reactOnReaderComment(chapterId, comment, "章节评论打赏") {
            reactToCommentOrReply(comment, readerChapterComments(chapterId), "award", awardPoints = 10)
        }
    }

    private fun readerChapterComments(chapterId: Long): List<ChapterComment> =
        readerChapterCommentState(readerState, chapterId).comments
            .let { it as? LoadResult.Success<List<ChapterComment>> }
            ?.value
            .orEmpty()

    fun refreshReaderChapterComments(chapterId: Long) {
        val state = readerState
        val route = currentRoute as? AppRoute.Reader ?: return
        val bookId = state.bookId
        if (chapterId <= 0 || bookId <= 0 || route.bookId != bookId) return
        val requestSerial = readerRequestSerial
        val previousComments = readerChapterCommentState(readerState, chapterId).comments
        val retainedComments = previousComments.takeIf { it is LoadResult.Success } ?: LoadResult.Loading
        updateReaderChapterCommentState(chapterId) { it.copy(comments = retainedComments) }
        viewModelScope.launch {
            val result = runCatching {
                api.chapterComments(bookId = bookId, chapterId = chapterId, page = 1, limit = PAGE_SIZE)
            }
            if (!isFreshReaderCommentRequest(requestSerial, bookId)) return@launch
            val loadResult = result.fold(
                onSuccess = { LoadResult.Success(it) },
                onFailure = {
                    if (retainedComments is LoadResult.Success) {
                        retainedComments
                    } else {
                        LoadResult.Error(apiFailureMessage(VisibleUiLabels.ChapterComments, it))
                    }
                },
            )
            updateReaderChapterCommentState(chapterId) { it.copy(comments = loadResult) }
            if (readerState.chapterId == chapterId) {
                readerState = readerState.copy(comments = loadResult)
            }
            result.getOrNull()?.let { commentList ->
                loadReaderCommentBookReferences(
                    bookId = bookId,
                    chapterId = chapterId,
                    comments = commentList,
                    requestSerial = requestSerial,
                )
            }
        }
    }

    private fun reactOnReaderComment(
        chapterId: Long,
        comment: ChapterComment,
        label: String,
        action: suspend () -> com.novalpie.nativeapp.model.ForumActionResult,
    ) {
        val state = readerState
        val bookId = state.bookId
        val chapterState = readerChapterCommentState(state, chapterId)
        if (comment.id <= 0 || bookId <= 0 || chapterId <= 0 || chapterState.actionLoading) return
        updateReaderChapterCommentState(chapterId) { it.copy(actionLoading = true, actionMessage = null) }
        val requestSerial = readerRequestSerial
        viewModelScope.launch {
            val result = runCatching { action() }
            if (!isFreshReaderCommentRequest(requestSerial, bookId)) return@launch
            updateReaderChapterCommentState(chapterId) { current ->
                result.fold(
                    onSuccess = {
                        current.copy(actionLoading = false, actionMessage = it.message ?: "$label 已同步")
                    },
                    onFailure = {
                        current.copy(actionLoading = false, actionMessage = apiFailureMessage(label, it))
                    },
                )
            }
            if (result.getOrNull()?.success == true) refreshReaderChapterComments(chapterId)
        }
    }

    private fun updateReaderChapterCommentState(
        chapterId: Long,
        transform: (ReaderChapterCommentState) -> ReaderChapterCommentState,
    ) {
        if (chapterId <= 0) return
        readerState = readerStateWithChapterCommentState(
            state = readerState,
            chapterId = chapterId,
            transform = transform,
        )
    }

    private fun isFreshReaderCommentRequest(requestSerial: Long, bookId: Long): Boolean {
        val route = currentRoute as? AppRoute.Reader ?: return false
        return requestSerial == readerRequestSerial && route.bookId == bookId && readerState.bookId == bookId
    }

    private suspend fun reactToCommentOrReply(
        comment: ChapterComment,
        availableComments: List<ChapterComment>,
        reactionType: String,
        awardPoints: Int? = null
    ): com.novalpie.nativeapp.model.ForumActionResult {
        val target = chapterCommentActionTarget(comment, availableComments)
        return api.reactToComment(
            commentId = target.parentCommentId,
            replyId = target.replyId,
            reactionType = reactionType,
            awardPoints = awardPoints,
        )
    }

    fun openBookEditInfo(bookId: Long) {
        if (bookId <= 0) return
        if (authToken.isNullOrBlank()) {
            openLoginFallback()
            return
        }
        if (!hasBookManagementAccess(bookId)) return
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.BookEditInfo(bookId)))
        loadBookEditInfo(bookId)
    }

    fun loadBookEditInfo(bookId: Long) {
        if (bookId <= 0) return
        val requestSerial = ++bookEditRequestSerial
        bookEditState = BookEditState(
            bookId = bookId,
            info = LoadResult.Loading,
            permissions = LoadResult.Loading
        )
        viewModelScope.launch {
            val infoResult = async { runCatching { api.managedBookInfo(bookId) } }
            val permissionResult = async { runCatching { api.managedBookPermissions(bookId) } }
            val resolvedInfo = infoResult.await()
            val resolvedPermissions = permissionResult.await()
            if (!isFreshRequestSerial(requestSerial, bookEditRequestSerial)) return@launch
            if (currentRoute != AppRoute.BookEditInfo(bookId)) return@launch
            bookEditState = BookEditState(
                bookId = bookId,
                info = resolvedInfo.toLoadResult("加载书籍信息"),
                permissions = resolvedPermissions.toLoadResult("加载编辑权限"),
                draft = resolvedInfo.getOrNull()?.let(::bookEditDraft) ?: BookEditDraft()
            )
        }
    }

    fun updateBookEditDraft(draft: BookEditDraft) {
        bookEditState = bookEditState.copy(draft = draft, actionMessage = null)
    }

    fun updateBookAccessPolicyDraft(draft: BookAccessPolicyDraft) {
        bookEditState = bookEditState.copy(accessPolicyDraft = draft, actionMessage = null)
    }

    fun updateBookTransferIdentifier(identifier: String) {
        bookEditState = bookEditState.copy(transferIdentifier = identifier, actionMessage = null)
    }

    fun saveManagedBookAccessPolicy() {
        val state = bookEditState
        if (state.bookId <= 0 || state.saving || state.uploadingCover || state.savingAccessPolicy || state.transferringBook) return
        validateBookAccessPolicyDraft(state.accessPolicyDraft)?.let { error ->
            bookEditState = state.copy(actionMessage = error)
            return
        }
        bookEditState = state.copy(savingAccessPolicy = true, actionMessage = "正在保存读写门槛…")
        viewModelScope.launch {
            val result = runCatching {
                api.updateManagedBookAccessPolicy(state.bookId, bookAccessPolicyFromDraft(state.accessPolicyDraft))
            }
            if (currentRoute != AppRoute.BookEditInfo(state.bookId)) return@launch
            bookEditState = result.fold(
                onSuccess = {
                    bookEditState.copy(
                        savingAccessPolicy = false,
                        actionMessage = it.message ?: "读写门槛已保存"
                    )
                },
                onFailure = {
                    bookEditState.copy(
                        savingAccessPolicy = false,
                        actionMessage = apiFailureMessage("保存读写门槛", it)
                    )
                }
            )
        }
    }

    fun transferManagedBook() {
        val state = bookEditState
        val identifier = state.transferIdentifier.trim()
        if (state.bookId <= 0 || state.saving || state.uploadingCover || state.savingAccessPolicy || state.transferringBook) return
        if (identifier.isBlank()) {
            bookEditState = state.copy(actionMessage = "请输入接收方 UID 或用户名")
            return
        }
        bookEditState = state.copy(transferringBook = true, actionMessage = "正在提交书籍转让…")
        viewModelScope.launch {
            val result = runCatching { api.transferManagedBook(state.bookId, identifier) }
            if (currentRoute != AppRoute.BookEditInfo(state.bookId)) return@launch
            bookEditState = result.fold(
                onSuccess = { transferred ->
                    val target = transferred.targetUsername
                        ?: transferred.targetUserId?.let { "UID $it" }
                        ?: identifier
                    bookEditState.copy(
                        transferringBook = false,
                        transferIdentifier = "",
                        actionMessage = transferred.message ?: "已提交转让给 $target"
                    )
                },
                onFailure = {
                    bookEditState.copy(
                        transferringBook = false,
                        actionMessage = apiFailureMessage("转让书籍", it)
                    )
                }
            )
        }
    }

    fun saveManagedBook() {
        val state = bookEditState
        if (state.bookId <= 0 || state.saving || state.uploadingCover || state.savingAccessPolicy || state.transferringBook) return
        val validation = validateBookEditDraft(state.draft)
        if (validation != null) {
            bookEditState = state.copy(actionMessage = validation)
            return
        }
        val draft = state.draft
        bookEditState = state.copy(saving = true, actionMessage = "正在保存书籍信息…")
        viewModelScope.launch {
            val result = runCatching {
                api.updateManagedBook(
                    state.bookId,
                    BookEditRequest(
                        title = draft.title,
                        titleTranslation = draft.titleTranslation,
                        authorName = draft.authorName,
                        description = draft.description,
                        source = draft.source,
                        sourceUrl = draft.sourceUrl,
                        language = draft.language,
                        status = draft.status,
                        isAdult = draft.isAdult,
                        photoUrl = draft.photoUrl,
                        tags = draft.tags
                    )
                )
            }
            if (currentRoute != AppRoute.BookEditInfo(state.bookId)) return@launch
            result.fold(
                onSuccess = { saved ->
                    val message = when {
                        !saved.success -> saved.message ?: saved.errors.joinToString("\n").ifBlank { "保存失败" }
                        saved.failedFields.isNotEmpty() -> "部分信息保存失败：${saved.failedFields.joinToString(", ")}"
                        else -> saved.message ?: "书籍信息保存成功"
                    }
                    bookEditState = bookEditState.copy(saving = false, actionMessage = message)
                    if (saved.success) loadBookDetail(state.bookId)
                },
                onFailure = { failure ->
                    bookEditState = bookEditState.copy(
                        saving = false,
                        actionMessage = apiFailureMessage("保存书籍信息", failure)
                    )
                }
            )
        }
    }

    fun uploadManagedBookCover(rawUri: String) {
        val state = bookEditState
        val permissions = (state.permissions as? LoadResult.Success)?.value
        if (
            state.bookId <= 0 ||
            rawUri.isBlank() ||
            state.uploadingCover ||
            state.saving ||
            state.savingAccessPolicy ||
            state.transferringBook ||
            permissions?.photoUrl != true
        ) return
        val requestSerial = bookEditRequestSerial
        bookEditState = state.copy(uploadingCover = true, actionMessage = "正在上传原始封面…")
        viewModelScope.launch {
            val result = runCatching {
                val document = readUploadDocument(rawUri)
                require(document.sizeBytes > 0L) { "封面文件为空" }
                require(document.mimeType?.startsWith("image/") == true) { "请选择图片文件" }
                api.uploadManagedBookCover(state.bookId, uploadSource(document))
            }
            if (!isFreshRequestSerial(requestSerial, bookEditRequestSerial)) return@launch
            if (currentRoute != AppRoute.BookEditInfo(state.bookId)) return@launch
            result.fold(
                onSuccess = { url ->
                    bookEditState = bookEditState.copy(
                        draft = bookEditState.draft.copy(photoUrl = url),
                        uploadingCover = false,
                        actionMessage = "封面已上传，保存信息后生效"
                    )
                },
                onFailure = { failure ->
                    bookEditState = bookEditState.copy(
                        uploadingCover = false,
                        actionMessage = apiFailureMessage("上传封面", failure)
                    )
                }
            )
        }
    }

    fun openBookChapters(bookId: Long) {
        if (bookId <= 0) return
        if (authToken.isNullOrBlank()) {
            openLoginFallback()
            return
        }
        if (!hasBookManagementAccess(bookId)) return
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.BookChapters(bookId)))
        loadManagedChapters(bookId)
    }

    fun loadManagedChapters(bookId: Long) {
        if (bookId <= 0) return
        val requestSerial = ++bookChapterRequestSerial
        bookChapterManagerState = BookChapterManagerState(bookId = bookId, chapters = LoadResult.Loading)
        viewModelScope.launch {
            val result = runCatching { api.chapters(bookId) }
            if (!isFreshRequestSerial(requestSerial, bookChapterRequestSerial)) return@launch
            if (currentRoute != AppRoute.BookChapters(bookId)) return@launch
            bookChapterManagerState = bookChapterManagerState.copy(
                chapters = result.toLoadResult("加载章节管理列表")
            )
        }
    }

    fun toggleManagedChapterSelection(chapterId: Long) {
        val selected = bookChapterManagerState.selectedIds.toMutableSet()
        if (!selected.add(chapterId)) selected.remove(chapterId)
        bookChapterManagerState = bookChapterManagerState.copy(selectedIds = selected)
    }

    fun selectAllManagedChapters() {
        val chapters = (bookChapterManagerState.chapters as? LoadResult.Success)?.value.orEmpty()
        val allIds = chapters.mapTo(mutableSetOf()) { it.id }
        bookChapterManagerState = bookChapterManagerState.copy(
            selectedIds = if (bookChapterManagerState.selectedIds.size == allIds.size) emptySet() else allIds
        )
    }

    fun moveManagedChapter(chapterId: Long, delta: Int) {
        val chapters = (bookChapterManagerState.chapters as? LoadResult.Success)?.value?.toMutableList() ?: return
        val from = chapters.indexOfFirst { it.id == chapterId }
        val to = (from + delta).coerceIn(0, chapters.lastIndex)
        if (from < 0 || from == to) return
        val item = chapters.removeAt(from)
        chapters.add(to, item)
        bookChapterManagerState = bookChapterManagerState.copy(
            chapters = LoadResult.Success(chapters.mapIndexed { index, chapter -> chapter.copy(number = index + 1) }),
            orderDirty = true,
            actionMessage = "章节顺序尚未保存"
        )
    }

    fun saveManagedChapterOrder() {
        val state = bookChapterManagerState
        val chapters = (state.chapters as? LoadResult.Success)?.value.orEmpty()
        if (!state.orderDirty || state.actionLoading || chapters.isEmpty()) return
        bookChapterManagerState = state.copy(actionLoading = true, actionMessage = "正在保存章节顺序…")
        viewModelScope.launch {
            val result = runCatching { api.reorderManagedChapters(state.bookId, chapters.map { it.id }) }
            if (currentRoute != AppRoute.BookChapters(state.bookId)) return@launch
            bookChapterManagerState = result.fold(
                onSuccess = { bookChapterManagerState.copy(actionLoading = false, orderDirty = false, actionMessage = it.message ?: "章节顺序已更新") },
                onFailure = { bookChapterManagerState.copy(actionLoading = false, actionMessage = apiFailureMessage("保存章节顺序", it)) }
            )
        }
    }

    fun openManagedChapterEditor(chapter: Chapter? = null) {
        val state = bookChapterManagerState
        if (state.orderDirty) {
            bookChapterManagerState = state.copy(actionMessage = "请先保存章节顺序")
            return
        }
        if (chapter == null) {
            val count = (state.chapters as? LoadResult.Success)?.value?.size ?: 0
            bookChapterManagerState = state.copy(
                editor = ManagedChapterDraft(insertAt = count + 1),
                actionMessage = null
            )
            return
        }
        bookChapterManagerState = state.copy(editorLoading = true, actionMessage = "正在加载章节正文…")
        viewModelScope.launch {
            val result = runCatching { api.chapterContent(chapter.id) }
            if (currentRoute != AppRoute.BookChapters(state.bookId)) return@launch
            bookChapterManagerState = result.fold(
                onSuccess = { content ->
                    bookChapterManagerState.copy(
                        editorLoading = false,
                        editor = ManagedChapterDraft(
                            chapterId = chapter.id,
                            insertAt = chapter.number ?: 1,
                            title = content.title ?: chapter.title,
                            content = content.content
                        ),
                        actionMessage = null
                    )
                },
                onFailure = { bookChapterManagerState.copy(editorLoading = false, actionMessage = apiFailureMessage("加载章节正文", it)) }
            )
        }
    }

    fun updateManagedChapterDraft(draft: ManagedChapterDraft) {
        bookChapterManagerState = bookChapterManagerState.copy(editor = draft, actionMessage = null)
    }

    fun dismissManagedChapterEditor() {
        if (!bookChapterManagerState.actionLoading) {
            bookChapterManagerState = bookChapterManagerState.copy(editor = null, editorLoading = false)
        }
    }

    fun openManagedChapterIllustrations(chapter: Chapter) {
        val state = bookChapterManagerState
        if (state.orderDirty) {
            bookChapterManagerState = state.copy(actionMessage = "请先保存章节顺序")
            return
        }
        val requestSerial = ++bookChapterRequestSerial
        bookChapterManagerState = state.copy(
            illustrationChapter = chapter,
            illustrations = LoadResult.Loading,
            actionMessage = null
        )
        viewModelScope.launch {
            val result = runCatching { api.managedChapterIllustrations(chapter.id) }
            if (!isFreshRequestSerial(requestSerial, bookChapterRequestSerial)) return@launch
            if (currentRoute != AppRoute.BookChapters(state.bookId)) return@launch
            bookChapterManagerState = bookChapterManagerState.copy(
                illustrations = result.toLoadResult("加载章节插图")
            )
        }
    }

    fun dismissManagedChapterIllustrations() {
        val state = bookChapterManagerState
        if (!state.uploadingIllustrations && state.deletingIllustrationId == null) {
            bookChapterManagerState = state.copy(
                illustrationChapter = null,
                illustrations = LoadResult.Idle
            )
        }
    }

    fun uploadManagedChapterIllustrations(rawUris: List<String>) {
        val state = bookChapterManagerState
        val chapter = state.illustrationChapter ?: return
        if (rawUris.isEmpty() || state.uploadingIllustrations || state.deletingIllustrationId != null) return
        bookChapterManagerState = state.copy(uploadingIllustrations = true, actionMessage = "正在上传原始章节插图…")
        viewModelScope.launch {
            val result = runCatching {
                val documents = rawUris.map { readUploadDocument(it) }
                documents.forEach { document ->
                    require(document.sizeBytes in 1..NovalPieApi.WEBSITE_CHAPTER_ILLUSTRATION_MAX_BYTES) {
                        "单张插图必须在 20 MiB 以内"
                    }
                    require(document.mimeType == null || document.mimeType.startsWith("image/")) { "请选择图片文件" }
                }
                api.uploadManagedChapterIllustrations(
                    chapter.id,
                    documents.map { uploadSource(it, fallbackContentType = "image/jpeg") }
                )
            }
            if (currentRoute != AppRoute.BookChapters(state.bookId)) return@launch
            result.fold(
                onSuccess = {
                    bookChapterManagerState = bookChapterManagerState.copy(
                        uploadingIllustrations = false,
                        actionMessage = it.message ?: "章节插图已上传"
                    )
                    openManagedChapterIllustrations(chapter)
                },
                onFailure = {
                    bookChapterManagerState = bookChapterManagerState.copy(
                        uploadingIllustrations = false,
                        actionMessage = apiFailureMessage("上传章节插图", it)
                    )
                }
            )
        }
    }

    fun deleteManagedChapterIllustration(imageId: Long) {
        val state = bookChapterManagerState
        val chapter = state.illustrationChapter ?: return
        if (imageId <= 0 || state.uploadingIllustrations || state.deletingIllustrationId != null) return
        bookChapterManagerState = state.copy(deletingIllustrationId = imageId, actionMessage = "正在删除章节插图…")
        viewModelScope.launch {
            val result = runCatching { api.deleteManagedChapterIllustration(chapter.id, imageId) }
            if (currentRoute != AppRoute.BookChapters(state.bookId)) return@launch
            result.fold(
                onSuccess = {
                    bookChapterManagerState = bookChapterManagerState.copy(
                        deletingIllustrationId = null,
                        actionMessage = it.message ?: "章节插图已删除"
                    )
                    openManagedChapterIllustrations(chapter)
                },
                onFailure = {
                    bookChapterManagerState = bookChapterManagerState.copy(
                        deletingIllustrationId = null,
                        actionMessage = apiFailureMessage("删除章节插图", it)
                    )
                }
            )
        }
    }

    fun insertChapterIllustrationPlaceholder(index: Int) {
        val state = bookChapterManagerState
        val chapter = state.illustrationChapter
        val editor = state.editor
        if (chapter == null || editor?.chapterId != chapter.id) {
            bookChapterManagerState = state.copy(actionMessage = "请先打开同一章节的正文编辑器，再插入图片占位符")
            return
        }
        val placeholder = chapterIllustrationPlaceholder(index)
        val separator = if (editor.content.endsWith("\n") || editor.content.isBlank()) "" else "\n"
        bookChapterManagerState = state.copy(
            editor = editor.copy(content = editor.content + separator + placeholder),
            actionMessage = "已插入 $placeholder"
        )
    }

    fun saveManagedChapterDraft() {
        val state = bookChapterManagerState
        val draft = state.editor ?: return
        validateManagedChapterDraft(draft)?.let {
            bookChapterManagerState = state.copy(actionMessage = it)
            return
        }
        if (state.actionLoading) return
        bookChapterManagerState = state.copy(actionLoading = true, actionMessage = "正在保存章节…")
        viewModelScope.launch {
            val result = runCatching {
                if (draft.chapterId == null) {
                    api.insertManagedChapter(state.bookId, draft.insertAt, draft.title, draft.content)
                } else {
                    api.updateManagedChapter(draft.chapterId, draft.title, draft.content)
                }
            }
            if (currentRoute != AppRoute.BookChapters(state.bookId)) return@launch
            result.fold(
                onSuccess = {
                    bookChapterManagerState = bookChapterManagerState.copy(editor = null, actionLoading = false, actionMessage = it.message ?: "章节已保存")
                    loadManagedChapters(state.bookId)
                },
                onFailure = { bookChapterManagerState = bookChapterManagerState.copy(actionLoading = false, actionMessage = apiFailureMessage("保存章节", it)) }
            )
        }
    }

    fun deleteManagedChapter(chapterId: Long) {
        val state = bookChapterManagerState
        if (state.orderDirty || state.actionLoading || chapterId <= 0) return
        runManagedChapterMutation(state, "删除章节") { api.deleteManagedChapter(chapterId) }
    }

    fun batchDeleteManagedChapters() {
        val state = bookChapterManagerState
        if (state.orderDirty || state.actionLoading || state.selectedIds.isEmpty()) return
        runManagedChapterMutation(state, "批量删除章节") {
            api.batchDeleteManagedChapters(state.bookId, state.selectedIds.toList())
        }
    }

    fun updateManagedTranslationMode(mode: String) {
        if (mode in setOf("personal", "shared")) {
            bookChapterManagerState = bookChapterManagerState.copy(translationMode = mode)
        }
    }

    fun translateSelectedManagedChapters() {
        val state = bookChapterManagerState
        if (state.orderDirty || state.actionLoading || state.selectedIds.isEmpty()) return
        runManagedChapterMutation(state, "提交章节翻译", refresh = false) {
            api.requestManagedChapterTranslation(state.bookId, state.selectedIds.toList(), state.translationMode)
        }
    }

    private fun runManagedChapterMutation(
        state: BookChapterManagerState,
        label: String,
        refresh: Boolean = true,
        action: suspend () -> com.novalpie.nativeapp.model.ForumActionResult
    ) {
        bookChapterManagerState = state.copy(actionLoading = true, actionMessage = "$label…")
        viewModelScope.launch {
            val result = runCatching { action() }
            if (currentRoute != AppRoute.BookChapters(state.bookId)) return@launch
            result.fold(
                onSuccess = {
                    bookChapterManagerState = bookChapterManagerState.copy(
                        actionLoading = false,
                        selectedIds = if (refresh) emptySet() else bookChapterManagerState.selectedIds,
                        actionMessage = it.message ?: "$label 已完成"
                    )
                    if (refresh) loadManagedChapters(state.bookId)
                },
                onFailure = {
                    bookChapterManagerState = bookChapterManagerState.copy(actionLoading = false, actionMessage = apiFailureMessage(label, it))
                }
            )
        }
    }

    fun openBookAppend(bookId: Long) {
        if (bookId <= 0) return
        if (authToken.isNullOrBlank()) {
            openLoginFallback()
            return
        }
        if (!hasBookManagementAccess(bookId)) return
        uploadFeature.enter(bookId)
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.BookAppend(bookId)))
    }

    fun openBook(bookId: Long) {
        if (bookId <= 0) return
        clearReaderSessionWhenLeaving()
        val currentStack = routes.toList()
        val nextStack = pushDistinctRoute(currentStack, AppRoute.BookDetail(bookId))
        if (nextStack === currentStack) return
        navigator.replaceAll(nextStack)
        loadBookDetail(bookId)
    }

    /** Opens the source-compatible read-only glossary from a book detail route. */
    fun openTerminology(bookId: Long) {
        if (bookId <= 0) return
        val currentStack = routes.toList()
        val nextStack = pushDistinctRoute(currentStack, AppRoute.Terminology(bookId))
        if (nextStack === currentStack) return
        navigator.replaceAll(nextStack)
        // A terminology query is meaningful only within its book. Do not carry a previous
        // title's source/translation term into a newly opened glossary.
        val query = terminologyKeywordForBook(terminologyState, bookId)
        loadTerminologies(bookId, query)
    }

    fun updateTerminologyKeyword(value: String) {
        if (terminologyState.keyword == value) return
        // An old page must never replace the list after the query has changed, even before the
        // person presses the IME search action.
        terminologyRequestSerial++
        terminologyState = terminologyState.copy(
            keyword = value,
            entries = LoadResult.Idle,
            page = null,
            loadingMore = false,
            loadMoreError = null,
        )
    }

    fun searchTerminologies() {
        val state = terminologyState
        if (state.bookId <= 0 || currentRoute !is AppRoute.Terminology) return
        loadTerminologies(state.bookId, state.keyword)
    }

    fun loadTerminologies(bookId: Long, keyword: String = terminologyState.keyword) {
        if (bookId <= 0) return
        loadTerminologyPage(
            bookId = bookId,
            keyword = keyword,
            requestedPage = 0,
            append = false,
        )
    }

    fun loadMoreTerminologies() {
        val state = terminologyState
        val page = state.page ?: return
        val entries = (state.entries as? LoadResult.Success)?.value ?: return
        if (state.loadingMore || !canLoadMoreTerminologyEntries(page, entries.size)) return
        loadTerminologyPage(
            bookId = state.bookId,
            keyword = state.keyword,
            requestedPage = page.page + 1,
            append = true,
        )
    }

    fun openReader(bookId: Long, chapterId: Long) {
        openReader(bookId, chapterId, ReaderChapterEntryPosition.Start)
    }

    fun openReader(
        bookId: Long,
        chapterId: Long,
        entryPosition: ReaderChapterEntryPosition,
    ) {
        if (bookId <= 0 || chapterId <= 0) return
        val next = AppRoute.Reader(bookId, chapterId, entryPosition)
        val currentStack = routes.toList()
        val nextStack = replaceTopReaderRoute(currentStack, next)
        if (nextStack === currentStack) return
        navigator.replaceAll(nextStack)
        loadReader(
            bookId,
            chapterId,
            entryPosition = entryPosition,
            restoreViewport = entryPosition == ReaderChapterEntryPosition.Start,
        )
    }

    fun continueReading(progress: ReaderProgress) {
        currentTab = BottomTab.Collection
        navigator.replaceAll(listOf(AppRoute.Home,AppRoute.BookDetail(progress.bookId),AppRoute.Reader(progress.bookId,progress.chapterId)))
        loadBookDetail(progress.bookId)
        loadReader(progress.bookId, progress.chapterId, restoreViewport = true)
    }

    fun clearReaderProgress() {
        val targetBookId = readerProgress?.bookId ?: return
        readerProgressStore.clear(targetBookId)
        readerSessionStore.clear()
        readerProgress = readerProgressStore.load()
        recentReaderProgresses = readerProgressStore.loadRecent(limit = READER_PROGRESS_HISTORY_LIMIT)
        updateLoadedCollectionProgress()
        readerProgressRevision += 1
    }

    fun openWebFallback(url: String) {
        clearReaderSessionWhenLeaving()
        navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.WebFallback(url)))
    }

    fun openLoginFallback() {
        openAuth(AuthPage.Login)
    }

    fun openWebLoginFallback() {
        openWebFallback("https://novalpie.cc/login")
    }

    fun openAuth(page: AuthPage, resetToken: String? = null) {
        val route = AppRoute.Auth(page)
        val stack = routes.toList()
        val next = if (stack.lastOrNull() is AppRoute.Auth) {
            stack.dropLast(1) + route
        } else {
            pushDistinctRoute(stack, route)
        }
        navigator.replaceAll(next)
        authRequestSerial++
        authState = AuthState(resetToken = resetToken.orEmpty())
    }

    fun switchAuthPage(page: AuthPage) {
        openAuth(page)
    }

    fun selectAuthLoginMethod(method: AuthLoginMethod) {
        authState = authState.copy(
            loginMethod = method,
            captchaToken = null,
            pendingCaptchaAction = null,
            actionMessage = null
        )
    }

    fun updateAuthLoginUsername(value: String) {
        authState = authState.copy(loginUsername = value, actionMessage = null)
    }

    fun updateAuthLoginPassword(value: String) {
        authState = authState.copy(loginPassword = value, actionMessage = null)
    }

    fun updateAuthLoginEmail(value: String) {
        authState = authState.copy(loginEmail = value, actionMessage = null)
    }

    fun updateAuthLoginCode(value: String) {
        authState = authState.copy(loginCode = value, actionMessage = null)
    }

    fun updateAuthRegisterEmail(value: String) {
        authState = authState.copy(registerEmail = value, actionMessage = null)
    }

    fun updateAuthRegisterCode(value: String) {
        authState = authState.copy(registerCode = value, actionMessage = null)
    }

    fun updateAuthRegisterUsername(value: String) {
        authState = authState.copy(registerUsername = value, actionMessage = null)
    }

    fun updateAuthRegisterPassword(value: String) {
        authState = authState.copy(registerPassword = value, actionMessage = null)
    }

    fun updateAuthRegisterConfirmPassword(value: String) {
        authState = authState.copy(registerConfirmPassword = value, actionMessage = null)
    }

    fun updateAuthResetEmail(value: String) {
        authState = authState.copy(resetEmail = value, actionMessage = null)
    }

    fun updateAuthResetPassword(value: String) {
        authState = authState.copy(resetPassword = value, actionMessage = null)
    }

    fun updateAuthResetConfirmPassword(value: String) {
        authState = authState.copy(resetConfirmPassword = value, actionMessage = null)
    }

    fun submitAuthLogin() {
        val action = if (authState.loginMethod == AuthLoginMethod.Password) {
            AuthCaptchaAction.PasswordLogin
        } else {
            AuthCaptchaAction.LoginWithCode
        }
        beginAuthCaptchaAction(action)
    }

    fun sendAuthLoginCode() {
        beginAuthCaptchaAction(AuthCaptchaAction.SendLoginCode)
    }

    fun sendAuthRegistrationCode() {
        beginAuthCaptchaAction(AuthCaptchaAction.SendRegistrationCode)
    }

    fun verifyAuthRegistrationCode() {
        val state = authState
        val validation = validateAuthEmail(state.registerEmail) ?: validateAuthCode(state.registerCode)
        if (validation != null) {
            authState = state.copy(actionMessage = validation)
            return
        }
        runAuthAction("验证邮箱", { api.verifyRegistrationEmail(state.registerEmail, state.registerCode) }) { result ->
            authState = authState.copy(
                registerStep = AuthRegisterStep.Account,
                actionMessage = result.message ?: "邮箱验证成功"
            )
        }
    }

    fun submitAuthRegistration() {
        val state = authState
        val validation = validateAuthUsername(state.registerUsername)
            ?: validateAuthPassword(state.registerPassword)
            ?: if (state.registerPassword != state.registerConfirmPassword) "两次输入的密码不一致" else null
        if (validation != null) {
            authState = state.copy(actionMessage = validation)
            return
        }
        runAuthSession("注册账号") {
            api.registerAccount(state.registerUsername, state.registerEmail, state.registerPassword)
        }
    }

    fun requestAuthPasswordReset() {
        val state = authState
        val validation = validateAuthEmail(state.resetEmail)
        if (validation != null) {
            authState = state.copy(actionMessage = validation)
            return
        }
        runAuthAction("发送重置邮件", { api.requestPasswordReset(state.resetEmail) }) { result ->
            authState = authState.copy(actionMessage = result.message ?: "重置邮件已发送，请在邮件中打开链接")
        }
    }

    fun submitAuthPasswordReset() {
        val state = authState
        val validation = when {
            state.resetToken.isBlank() -> "重置链接无效，请重新申请"
            else -> validateAuthPassword(state.resetPassword)
                ?: if (state.resetPassword != state.resetConfirmPassword) "两次输入的密码不一致" else null
        }
        if (validation != null) {
            authState = state.copy(actionMessage = validation)
            return
        }
        runAuthAction("重置密码", { api.resetPassword(state.resetToken, state.resetPassword) }) { result ->
            openAuth(AuthPage.Login)
            authState = authState.copy(actionMessage = result.message ?: "密码已重置，请登录")
        }
    }

    fun cancelAuthCaptcha() {
        authState = authState.copy(pendingCaptchaAction = null, actionMessage = null)
        if (currentRoute == AppRoute.AuthCaptcha) goBack()
    }

    fun completeAuthCaptcha(token: String) {
        val normalized = token.trim().takeIf { it.length >= 20 } ?: run {
            authState = authState.copy(actionMessage = "安全验证未返回有效令牌，请重试")
            return
        }
        val action = authState.pendingCaptchaAction ?: return
        authState = authState.copy(captchaToken = normalized, pendingCaptchaAction = null, actionMessage = null)
        if (currentRoute == AppRoute.AuthCaptcha) goBack()
        executeAuthCaptchaAction(action)
    }

    private fun beginAuthCaptchaAction(action: AuthCaptchaAction) {
        val state = authState
        val validation = when (action) {
            AuthCaptchaAction.PasswordLogin -> when {
                state.loginUsername.trim().isBlank() -> "请输入用户名或邮箱"
                state.loginPassword.isBlank() -> "请输入密码"
                else -> null
            }
            AuthCaptchaAction.SendLoginCode -> validateAuthEmail(state.loginEmail)
            AuthCaptchaAction.LoginWithCode -> validateAuthEmail(state.loginEmail) ?: validateAuthCode(state.loginCode)
            AuthCaptchaAction.SendRegistrationCode -> validateAuthEmail(state.registerEmail)
        }
        if (validation != null) {
            authState = state.copy(actionMessage = validation)
            return
        }
        if (state.captchaToken.isNullOrBlank()) {
            authState = state.copy(pendingCaptchaAction = action, actionMessage = "请先完成源站安全验证")
            navigator.replaceAll(pushDistinctRoute(routes.toList(), AppRoute.AuthCaptcha))
            return
        }
        executeAuthCaptchaAction(action)
    }

    private fun executeAuthCaptchaAction(action: AuthCaptchaAction) {
        val state = authState
        val captchaToken = state.captchaToken.orEmpty()
        if (captchaToken.isBlank()) {
            authState = state.copy(actionMessage = "请先完成源站安全验证")
            return
        }
        when (action) {
            AuthCaptchaAction.PasswordLogin -> runAuthSession("登录") {
                api.loginPassword(state.loginUsername, state.loginPassword, captchaToken)
            }
            AuthCaptchaAction.SendLoginCode -> runAuthAction("发送登录验证码", {
                api.sendLoginVerificationCode(state.loginEmail, captchaToken)
            }) { result ->
                authState = authState.copy(actionMessage = result.message ?: "验证码已发送，请查收邮箱")
            }
            AuthCaptchaAction.LoginWithCode -> runAuthSession("验证码登录") {
                api.loginWithVerificationCode(state.loginEmail, state.loginCode, captchaToken)
            }
            AuthCaptchaAction.SendRegistrationCode -> runAuthAction("发送注册验证码", {
                api.sendRegistrationVerificationCode(state.registerEmail, captchaToken)
            }) { result ->
                authState = authState.copy(
                    registerStep = AuthRegisterStep.Verify,
                    actionMessage = result.message ?: "验证码已发送，请查收邮箱"
                )
            }
        }
    }

    private fun runAuthSession(label: String, request: suspend () -> AuthSession) {
        val requestSerial = ++authRequestSerial
        authState = authState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { request() }
            if (!isFreshRequestSerial(requestSerial, authRequestSerial)) return@launch
            result.fold(
                onSuccess = { session ->
                    authSessionStore.saveToken(session.token)
                    authToken = session.token
                    AppContainer.from(getApplication()).refreshEnvironmentFromStores()
                    authState = AuthState()
                    currentTab = BottomTab.Collection
                    navigator.reset(AppRoute.Home)
                    loadHome()
                },
                onFailure = { failure ->
                    authState = authState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, failure)
                    )
                }
            )
        }
    }

    private fun runAuthAction(
        label: String,
        request: suspend () -> AuthActionResult,
        onSuccess: (AuthActionResult) -> Unit
    ) {
        val requestSerial = ++authRequestSerial
        authState = authState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { request() }
            if (!isFreshRequestSerial(requestSerial, authRequestSerial)) return@launch
            result.fold(
                onSuccess = { action ->
                    if (!action.success) {
                        authState = authState.copy(
                            actionLoading = false,
                            actionMessage = action.message ?: "$label 失败"
                        )
                    } else {
                        authState = authState.copy(actionLoading = false)
                        onSuccess(action)
                    }
                },
                onFailure = { failure ->
                    authState = authState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, failure)
                    )
                }
            )
        }
    }

    fun openDeepLink(rawUri: String) {
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return
        val isNativeScheme = uri.scheme == "novalpie" && uri.host == "app"
        val isWebsiteRoute = uri.scheme in setOf("https", "http") && uri.host.equals("novalpie.cc", ignoreCase = true)
        if (!isNativeScheme && !isWebsiteRoute) return

        clearReaderSessionWhenLeaving()

        val path = uri.path.orEmpty()
        val destination = if (path.trimEnd('/').equals("/reader", ignoreCase = true)) {
            readerLandingRoute(
                novelId = uri.getQueryParameter("novel"),
                chapterId = uri.getQueryParameter("chapter"),
            )
        } else {
            nativeWebsiteRoute(
                path = path,
                isAdmin = isAdminProfile(currentUserProfile())
            )
        }
        if (destination != null) {
            openNativeWebsiteRoute(
                route = destination,
                searchQuery = uri.getQueryParameter("q") ?: uri.getQueryParameter("keyword"),
                resetToken = uri.getQueryParameter("token")
            )
            return
        }

        // Unknown website pages remain available through the authenticated fallback. Administrator
        // paths are intentionally excluded: an ordinary account must not receive an admin surface
        // simply because it was handed an external URL.
        val isAdminPath = path.trim('/').substringBefore('/').equals("admin", ignoreCase = true)
        if (isWebsiteRoute && !isAdminPath) openWebFallback(uri.toString())
    }

    private fun resetToTabRoot(tab: BottomTab) {
        currentTab = tab
        navigator.reset(
            when (tab) {
                BottomTab.Collection -> AppRoute.Home
                BottomTab.Discover -> AppRoute.Search
                BottomTab.Tools -> AppRoute.Tools
                BottomTab.Forum -> AppRoute.Forum
                BottomTab.Profile -> AppRoute.Profile
            }
        )
    }

    private fun openNativeWebsiteRoute(route: AppRoute, searchQuery: String?, resetToken: String? = null) {
        when (route) {
            AppRoute.Home -> {
                resetToTabRoot(BottomTab.Collection)
                // `init` already starts the first shelf request. A Home deep link received during
                // launch must join that request instead of starting a competing duplicate.
                if (shouldLoadHomeOnTabEntry()) loadHome()
            }
            AppRoute.Search -> {
                resetToTabRoot(BottomTab.Discover)
                val keyword = searchQuery?.trim()
                if (keyword.isNullOrBlank()) {
                    loadSearchTags()
                    loadDefaultSearchResultsIfNeeded()
                } else {
                    performSearch(keyword)
                }
            }
            AppRoute.Tools -> {
                resetToTabRoot(BottomTab.Tools)
                loadTools()
            }
            AppRoute.Forum -> {
                resetToTabRoot(BottomTab.Forum)
                loadForum()
            }
            AppRoute.ForumCreate -> {
                resetToTabRoot(BottomTab.Forum)
                loadForum()
                openForumCreate()
            }
            is AppRoute.ForumPostDetail -> {
                resetToTabRoot(BottomTab.Forum)
                loadForum()
                openForumPost(route.postId)
            }
            AppRoute.Profile -> {
                resetToTabRoot(BottomTab.Profile)
                loadProfile()
            }
            is AppRoute.UserProfileDetail -> {
                resetToTabRoot(BottomTab.Profile)
                // The public profile is pushed on top of Profile. Hydrate that root now so a
                // system-back from this deep link cannot reveal an Idle "我的" screen forever.
                loadProfile()
                openUserProfile(route.userId)
            }
            AppRoute.Settings -> {
                resetToTabRoot(BottomTab.Profile)
                loadProfile()
                openSettings()
            }
            AppRoute.MessageCenter -> {
                resetToTabRoot(BottomTab.Tools)
                loadTools()
                openMessageCenter()
            }
            is AppRoute.MessageDetail -> {
                resetToTabRoot(BottomTab.Tools)
                openMessageCenter()
                openMessageDetail(route.messageId)
            }
            is AppRoute.MessageConversation -> {
                resetToTabRoot(BottomTab.Tools)
                openMessageConversation(route.targetUserId, route.targetName)
            }
            AppRoute.MessageSettings -> {
                resetToTabRoot(BottomTab.Tools)
                openMessageSettings()
            }
            AppRoute.Workspace -> {
                resetToTabRoot(BottomTab.Tools)
                openWorkspace()
            }
            AppRoute.UploadBook -> {
                resetToTabRoot(BottomTab.Tools)
                openUploadBook()
            }
            AppRoute.UploadEditor -> {
                resetToTabRoot(BottomTab.Tools)
                openUploadEditor()
            }
            AppRoute.PoliticalExam -> {
                resetToTabRoot(BottomTab.Tools)
                openPoliticalExam()
            }
            is AppRoute.Auth -> {
                resetToTabRoot(BottomTab.Profile)
                // Auth and captcha routes return to the Profile root.  Start that root's request
                // before pushing the form so Cancel/Back cannot reveal an Idle state that the UI
                // correctly has no account data to render yet.
                loadProfile()
                openAuth(route.page, resetToken)
            }
            AppRoute.AuthCaptcha -> Unit
            is AppRoute.BookDetail -> {
                resetToTabRoot(BottomTab.Collection)
                openBook(route.bookId)
            }
            is AppRoute.Terminology -> {
                resetToTabRoot(BottomTab.Collection)
                openBook(route.bookId)
                openTerminology(route.bookId)
            }
            is AppRoute.Reader -> {
                resetToTabRoot(BottomTab.Collection)
                openBook(route.bookId)
                openReader(route.bookId, route.chapterId)
            }
            is AppRoute.BookEditInfo,
            is AppRoute.BookChapters,
            is AppRoute.BookAppend -> openDeepLinkedManagedBook(route)
            is AppRoute.Admin -> {
                if (!isAdminProfile(currentUserProfile())) return
                resetToTabRoot(BottomTab.Tools)
                openAdminSection(route.section)
            }
            is AppRoute.WebFallback -> {
                resetToTabRoot(BottomTab.Collection)
                openWebFallback(route.url)
            }
        }
    }

    /**
     * Management routes require an asynchronous per-book permission check. The public detail
     * route stays underneath the destination so Back has a meaningful native destination, while
     * no editor route is ever placed on the stack until the source permission API permits it.
     */
    private fun openDeepLinkedManagedBook(route: AppRoute) {
        val bookId = when (route) {
            is AppRoute.BookEditInfo -> route.bookId
            is AppRoute.BookChapters -> route.bookId
            is AppRoute.BookAppend -> route.bookId
            else -> return
        }
        if (authToken.isNullOrBlank()) {
            resetToTabRoot(BottomTab.Collection)
            openLoginFallback()
            return
        }

        resetToTabRoot(BottomTab.Collection)
        navigator.push(AppRoute.BookDetail(bookId))
        loadBookDetail(bookId)
        viewModelScope.launch {
            val permissions = runCatching { api.managedBookPermissions(bookId) }.getOrNull()
            if (!bookManagementActionsVisible(permissions)) return@launch
            if (currentRoute != AppRoute.BookDetail(bookId)) return@launch

            navigator.push(route)
            when (route) {
                is AppRoute.BookEditInfo -> loadBookEditInfo(bookId)
                is AppRoute.BookChapters -> loadManagedChapters(bookId)
                is AppRoute.BookAppend -> {
                    uploadFeature.enter(bookId)
                }
                else -> Unit
            }
        }
    }

    fun goBack(): Boolean {
        if (routes.size <= 1) return false
        val leavingReader = currentRoute as? AppRoute.Reader
        navigator.pop()
        rootRouteTab(currentRoute)?.let { currentTab = it }
        when (val restored = currentRoute) {
            is AppRoute.Admin -> adminFeature.enter(restored.section)
            AppRoute.UploadBook -> uploadFeature.enter(null)
            is AppRoute.BookAppend -> uploadFeature.enter(restored.bookId)
            is AppRoute.MessageDetail -> if (messageDetailState.messageId != restored.messageId) loadMessageDetail(restored.messageId)
            is AppRoute.MessageConversation -> if (messageConversationState.targetUserId != restored.targetUserId) loadMessageConversation(restored.targetUserId, restored.targetName)
            is AppRoute.UserProfileDetail -> publicProfileFeature.enter(restored.userId, forumState.hideSpoilers)
            is AppRoute.ForumPostDetail -> forumPostFeature.enter(restored.postId)
            else -> Unit
        }
        if (leavingReader != null) {
            readerSessionStore.clear()
            val detail = currentRoute as? AppRoute.BookDetail
            if (detail != null && bookDetailState.bookId != detail.bookId) {
                loadBookDetail(detail.bookId)
            }
            if (
                favoritesUiOptions.tab == FavoritesContentTab.Favorites &&
                collectionRefreshRequired(
                    readerProgressRevision = readerProgressRevision,
                    syncedProgressRevision = syncedCollectionProgressRevision,
                )
            ) {
                loadHome()
            }
        }
        return true
    }

    fun saveHomeGridScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        val next = GridScrollPosition.from(firstVisibleItemIndex, firstVisibleItemScrollOffset)
        if (homeGridScrollPosition != next) homeGridScrollPosition = next
    }

    fun saveForumScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        forumFeature.saveScroll(firstVisibleItemIndex,firstVisibleItemScrollOffset)
    }

    fun saveSearchGridScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        searchFeature.saveScroll(firstVisibleItemIndex,firstVisibleItemScrollOffset)
    }

    fun loadHome(actionMessage: String? = null) {
        val options=favoritesUiOptions
        libraryFeature.load(
            requested=com.novalpie.nativeapp.feature.library.LibraryQuery(options.tab,options.currentPage,selectedFavoriteGroupId,bookshelfQuery,options.sortField,options.sortOrder),
            options=options,
            tokenProfile=authToken?.let(::decodeAuthTokenProfile),
            message=actionMessage,
            retain=options.tab==FavoritesContentTab.Favorites&&collectionRefreshRequired(readerProgressRevision,syncedCollectionProgressRevision),
        )
        resolveMissingReaderProgressBookTitle()
    }

    fun loadMoreFavorites() = libraryFeature.loadMore()

    fun performSearch(submittedKeyword: String? = null) {
        searchFeature.submit(submittedKeyword)
    }

    fun loadMoreSearch() {
        goToSearchPage(searchPage + 1)
    }

    /** Source parity: result pages replace the current grid instead of appending an endless list. */
    fun goToSearchPage(requestedPage: Int) {
        searchFeature.goToPage(requestedPage)
    }

    fun loadSearchTags() {
        searchFeature.loadTags()
    }

    /** Populate Discover with the source's unfiltered work feed before the first typed search. */
    private fun loadDefaultSearchResultsIfNeeded() {
        if (searchKeyword.isNotBlank() || searchResults !is LoadResult.Idle) return
        performSearch("")
    }

    private fun loadTerminologyPage(
        bookId: Long,
        keyword: String,
        requestedPage: Int,
        append: Boolean,
    ) {
        val requestSerial = ++terminologyRequestSerial
        val normalizedKeyword = keyword.trim()
        val existingEntries = (terminologyState.entries as? LoadResult.Success)?.value.orEmpty()
        terminologyState = if (append) {
            terminologyState.copy(loadingMore = true, loadMoreError = null)
        } else {
            TerminologyState(
                bookId = bookId,
                keyword = keyword,
                entries = LoadResult.Loading,
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                api.terminologyPage(
                    novelId = bookId,
                    keyword = normalizedKeyword,
                    page = requestedPage,
                )
            }
            if (
                !isFreshRequestSerial(requestSerial, terminologyRequestSerial) ||
                !isFreshTerminologyResult(currentRoute, terminologyState, bookId)
            ) return@launch

            terminologyState = result.fold(
                onSuccess = { page ->
                    val merged = if (append) {
                        (existingEntries + page.items).distinctBy { entry -> entry.id }
                    } else {
                        page.items
                    }
                    TerminologyState(
                        bookId = bookId,
                        keyword = keyword,
                        entries = LoadResult.Success(merged),
                        page = page,
                    )
                },
                onFailure = { failure ->
                    if (append) {
                        terminologyState.copy(
                            loadingMore = false,
                            loadMoreError = apiFailureMessage("术语表", failure),
                        )
                    } else {
                        TerminologyState(
                            bookId = bookId,
                            keyword = keyword,
                            entries = LoadResult.Error(apiFailureMessage("术语表", failure)),
                        )
                    }
                },
            )
        }
    }

    fun loadBookDetail(
        bookId: Long,
        preservedActionMessage: String? = null,
        retainCommentComposer: Boolean = false,
    ) {
        if(bookId<=0)return
        if (!nativeEpubDownloadState.busy) nativeEpubDownloadState = NativeEpubDownloadState(bookId = bookId)
        bookFeature.load(bookId,!authToken.isNullOrBlank(),retainCommentComposer,preservedActionMessage)
        loadLatestCompletedNativeDownload(bookId)
    }

    private fun loadLatestCompletedNativeDownload(bookId: Long) {
        val account = authToken?.let(::decodeAuthTokenProfile)?.id ?: return
        val container = AppContainer.from(getApplication())
        val expectedState = nativeEpubDownloadState
        viewModelScope.launch(Dispatchers.IO) {
            val completed = runCatching {
                container.downloadStore.recover(account).tasks
                    .filter { it.bookId == bookId && it.phase == DownloadPhase.Completed && !it.destinationUri.isNullOrBlank() }
                    .maxByOrNull { it.updatedAt }
            }.getOrNull() ?: return@launch
            withContext(Dispatchers.Main.immediate) {
                if (currentRoute == AppRoute.BookDetail(bookId) && bookDetailState.bookId == bookId &&
                    authToken?.let(::decodeAuthTokenProfile)?.id == account &&
                    !container.downloads.state.value.busy && nativeEpubDownloadState.bookId == bookId && nativeEpubDownloadState === expectedState) {
                    nativeEpubDownloadState = com.novalpie.nativeapp.feature.download.nativeDownloadCompletedState(completed)
                }
            }
        }
    }

    /** Mirrors the reader's favourite action while keeping the detail action independently busy. */
    fun toggleBookDetailFavorite() {
        val state = bookDetailState
        val bookId = state.bookId
        val current = (state.favoriteStatus as? LoadResult.Success)?.value ?: return
        if (bookId <= 0 || state.favoriteLoading) return
        val requestSerial = bookDetailRequestSerial
        bookDetailState = state.copy(favoriteLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                api.toggleFavorite(bookId, current.isFavorited, current.groupId ?: 0L)
            }
            if (
                !isFreshRequestSerial(requestSerial, bookDetailRequestSerial) ||
                !isFreshBookDetailResult(currentRoute, bookDetailState, bookId)
            ) return@launch
            bookDetailState = result.fold(
                onSuccess = { status ->
                    bookDetailState.copy(
                        favoriteStatus = LoadResult.Success(status),
                        favoriteLoading = false,
                        actionMessage = if (status.isFavorited) "已加入收藏" else "已取消收藏",
                    )
                },
                onFailure = { failure ->
                    bookDetailState.copy(
                        favoriteLoading = false,
                        actionMessage = apiFailureMessage("收藏", failure),
                    )
                },
            )
        }
    }

    /** Ask the source to fetch new chapters; this is intentionally not a local catalogue refresh. */
    fun requestBookDetailNewChapters() {
        val state = bookDetailState
        val book = (state.book as? LoadResult.Success)?.value ?: return
        if (
            state.bookId <= 0 || state.requestNewChapterLoading ||
            !bookDetailShowsRequestNewChapter(!authToken.isNullOrBlank(), book.platform)
        ) return
        val requestSerial = bookDetailRequestSerial
        bookDetailState = state.copy(requestNewChapterLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { api.requestNovelChapters(book.id) }
            if (
                !isFreshRequestSerial(requestSerial, bookDetailRequestSerial) ||
                !isFreshBookDetailResult(currentRoute, bookDetailState, book.id)
            ) return@launch
            bookDetailState = result.fold(
                onSuccess = { action ->
                    bookDetailState.copy(
                        requestNewChapterLoading = false,
                        actionMessage = action.message ?: if (action.success) "已提交获取新章请求" else "获取新章失败",
                    )
                },
                onFailure = { failure ->
                    bookDetailState.copy(
                        requestNewChapterLoading = false,
                        actionMessage = apiFailureMessage("获取新章", failure),
                    )
                },
            )
        }
    }


    fun downloadBookEpub(bookId:Long,replacementMode:NativeDownloadReplacementMode=NativeDownloadReplacementMode.Source) =
        startNativeDownloadTask(bookId,DownloadFormat.Epub,replacementMode)

    fun downloadBookTxt(bookId:Long,replacementMode:NativeDownloadReplacementMode=NativeDownloadReplacementMode.Source) =
        startNativeDownloadTask(bookId,DownloadFormat.Txt,replacementMode)

    private fun startNativeDownloadTask(bookId:Long,format:DownloadFormat,mode:NativeDownloadReplacementMode) {
        val account=currentUserProfile()?.id
        val container=AppContainer.from(getApplication())
        if(bookId<=0||container.downloads.state.value.busy)return
        if(account==null||authToken.isNullOrBlank()) {
            nativeEpubDownloadState=NativeEpubDownloadState(bookId=bookId,message="请先登录后下载")
            return
        }
        val book=(bookDetailState.book as? LoadResult.Success)?.value?.takeIf {it.id==bookId}
        val task=DownloadTask(java.util.UUID.randomUUID().toString(),account,bookId,book?.title ?: "NovalPie-$bookId",format,
            applyReplacement=mode==NativeDownloadReplacementMode.EffectiveReaderRules,requestedConcurrency=profileState.downloadImageConcurrency)
        runCatching {NativeDownloadService.start(getApplication(),task)}.onFailure {
            nativeEpubDownloadState=NativeEpubDownloadState(bookId=bookId,message="无法启动原生下载：${it.message}")
        }
    }
    fun cancelNativeBookDownload(bookId:Long) {
        val coordinator=AppContainer.from(getApplication()).downloads
        if(coordinator.state.value.task?.bookId==bookId)coordinator.cancel()
    }
    fun pauseNativeBookDownload(bookId:Long) {
        val coordinator=AppContainer.from(getApplication()).downloads
        if(coordinator.state.value.task?.bookId==bookId)coordinator.pause()
    }
    fun resumeNativeBookDownload(bookId:Long) {
        val coordinator=AppContainer.from(getApplication()).downloads
        if(coordinator.state.value.task?.bookId==bookId) {
            if(coordinator.state.value.busy)coordinator.resume()else retryNativeBookDownload(bookId)
        }
    }
    fun toggleNativeBookDownloadPause(bookId:Long) {
        if(nativeEpubDownloadState.paused)resumeNativeBookDownload(bookId)else pauseNativeBookDownload(bookId)
    }
    fun retryNativeBookDownload(bookId:Long) {
        val coordinator=AppContainer.from(getApplication()).downloads
        val task=coordinator.state.value.task?.takeIf{it.bookId==bookId} ?: return
        if(coordinator.state.value.busy)return
        if(task.phase==DownloadPhase.AuthorizationUncertain) {
            nativeEpubDownloadState=nativeEpubDownloadState.copy(message="上次授权结果未确认，不会自动再次扣分")
            return
        }
        NativeDownloadService.start(getApplication(),task.copy(phase=DownloadPhase.Queued,failure=null))
    }

    /**
     * Opens only a server-confirmed original in the native preview. A long press never navigates
     * to the book route and never upgrades every search thumbnail in the background.
     */
    fun previewBookCover(book: NovelCard) {
        if (book.id <= 0) return
        val knownOriginal = originalBookCoverPreviewUrl(book, photo = null)
        if (knownOriginal != null) {
            imagePreviewState = ImagePreviewState(
                title = "${book.title} · 封面",
                displayUrl = knownOriginal,
                originalUrl = knownOriginal,
            )
            return
        }
        val requestSerial = ++imagePreviewRequestSerial
        imagePreviewState = ImagePreviewState(
            title = "${book.title} · 封面",
            displayUrl = null,
            loading = true,
        )
        viewModelScope.launch {
            val photo = runCatching { api.bookCoverPhotoInfo(book.id) }.getOrNull()
            if (requestSerial != imagePreviewRequestSerial) return@launch
            val originalUrl = originalBookCoverPreviewUrl(book, photo)
            imagePreviewState = ImagePreviewState(
                title = "${book.title} · 封面",
                displayUrl = originalUrl,
                originalUrl = originalUrl,
            )
        }
    }

    /** Reader images can use a payload original immediately and only query cover metadata as a fallback. */
    internal fun previewReaderImage(bookId: Long, image: ReaderContentBlock.Image, title: String) {
        if (image.url.isBlank()) return
        val directOriginal = image.originalUrl?.trim()?.takeIf(String::isNotBlank)
        if (directOriginal != null) {
            imagePreviewState = ImagePreviewState(
                title = title,
                displayUrl = directOriginal,
                originalUrl = directOriginal,
            )
            return
        }
        val requestSerial = ++imagePreviewRequestSerial
        imagePreviewState = ImagePreviewState(title = title, loading = true)
        viewModelScope.launch {
            val photo = if (bookId > 0) runCatching { api.bookCoverPhotoInfo(bookId) }.getOrNull() else null
            if (requestSerial != imagePreviewRequestSerial) return@launch
            val originalUrl = originalReaderImagePreviewUrl(image, photo)
            imagePreviewState = ImagePreviewState(
                title = title,
                displayUrl = originalUrl,
                originalUrl = originalUrl,
            )
        }
    }

    fun dismissImagePreview() {
        imagePreviewRequestSerial++
        imagePreviewState = ImagePreviewState()
    }


    private fun hasBookManagementAccess(bookId: Long): Boolean =
        bookDetailState.bookId == bookId &&
            bookManagementActionsVisible(
                (bookDetailState.managementPermissions as? LoadResult.Success)?.value
            )

    fun loadReader(
        bookId: Long,
        chapterId: Long,
        preserveContinuousWindow: Boolean = false,
        entryPosition: ReaderChapterEntryPosition = ReaderChapterEntryPosition.Start,
        restoreViewport: Boolean = entryPosition == ReaderChapterEntryPosition.Start,
    ) {
        val requestSerial = ++readerRequestSerial
        val catalogRequestSerial = ++readerCatalogRequestSerial
        loadReaderReplacementRules(bookId)
        val requestedReplaceMode = readerUiOptions.replaceMode
        val requestedShowImages = readerUiOptions.showImages
        readerSessionStore.save(bookId, chapterId)
        val previousState = readerState
        val keepWindow = preserveContinuousWindow &&
            previousState.bookId == bookId &&
            previousState.chapterId == chapterId
        fun chapterWindow(content: ReaderContent, title: String?): List<ReaderChapterContent> {
            val loaded = ReaderChapterContent(chapterId, title ?: content.title, content)
            return if (keepWindow) {
                previousState.chapterContents.map { if (it.chapterId == chapterId) loaded else it }.ifEmpty { listOf(loaded) }
            } else {
                listOf(loaded)
            }
        }
        val retainedBookTitle = previousState.bookTitle
            ?.trim()
            ?.takeIf { keepWindow && previousState.bookId == bookId }
        val initialBookTitle = readerBookTitle(bookId) ?: retainedBookTitle
        val savedViewportAnchor = readerProgressStore.load(bookId)
            ?.takeIf { restoreViewport && it.chapterId == chapterId }
            ?.viewportItemIndex
            ?.let { itemIndex ->
                readerProgressStore.load(bookId)?.viewportItemScrollOffsetPx?.let { offset ->
                    ReaderViewportAnchor(
                        chapterId = chapterId,
                        itemIndexWithinChapter = itemIndex,
                        itemScrollOffsetPx = offset,
                    )
                }
            }
        readerState = ReaderState(
            bookId = bookId,
            bookTitle = initialBookTitle,
            chapterId = chapterId,
            visibleChapterId = if (keepWindow) previousState.visibleChapterId else chapterId,
            entryPosition = entryPosition,
            restoreViewportAnchor = savedViewportAnchor,
            content = LoadResult.Loading,
            chapterContents = if (keepWindow) previousState.chapterContents else emptyList(),
            chapters = LoadResult.Loading,
            comments = LoadResult.Loading,
            chapterCommentStates = (if (keepWindow) previousState.chapterCommentStates else emptyMap()) +
                (chapterId to ReaderChapterCommentState(comments = LoadResult.Loading)),
            favoriteStatus = LoadResult.Loading,
        )
        if (initialBookTitle == null) {
            viewModelScope.launch {
                val resolvedTitle = runCatching { api.bookDetail(bookId).title }
                    .getOrNull()
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                if (
                    resolvedTitle != null &&
                    requestSerial == readerRequestSerial &&
                    isFreshReaderResult(currentRoute, readerState, bookId, chapterId)
                ) {
                    readerState = readerState.copy(bookTitle = resolvedTitle)
                }
            }
        }
        viewModelScope.launch {
            val content = async {
                runCatching {
                    api.chapterContent(
                        chapterId = chapterId,
                        replaceMode = requestedReplaceMode,
                        showImages = requestedShowImages,
                    )
                }
            }
            val cachedContent = async(Dispatchers.IO) {
                readerChapterCacheStore.load(
                    bookId = bookId,
                    chapterId = chapterId,
                    replaceMode = requestedReplaceMode,
                    showImages = requestedShowImages,
                )
            }
            val chapters = async { runCatching { api.chapters(bookId) } }
            val comments = async { runCatching { api.chapterComments(bookId = bookId, chapterId = chapterId, page = 1, limit = PAGE_SIZE) } }
            val favorite = async { runCatching { api.favoriteStatus(bookId) } }
            val cached = cachedContent.await()
            // Start showing an already-downloaded body immediately. The network request remains in
            // flight and replaces it when successful, so this is a real offline fallback rather
            // than a stale-data preference that prevents source refreshes.
            if (
                cached != null &&
                requestSerial == readerRequestSerial &&
                isFreshReaderResult(currentRoute, readerState, bookId, chapterId)
            ) {
                readerState = readerState.copy(
                    content = LoadResult.Success(cached.content),
                    chapterContents = chapterWindow(cached.content, cached.content.title),
                    contentFromCache = true,
                )
            }
            var contentResult = content.await()
            var effectiveReplaceMode = requestedReplaceMode
            // The website retries a politically restricted "no replacement" request using its
            // default India mode. Keep the same recovery path so a selectable mode cannot strand
            // the native reader on an empty error state.
            if (contentResult.isFailure && requestedReplaceMode.isBlank() &&
                isReplacementRestriction(contentResult.exceptionOrNull())) {
                contentResult = runCatching {
                    api.chapterContent(
                        chapterId = chapterId,
                        replaceMode = ReaderSettingsStore.DEFAULT_REPLACE_MODE,
                        showImages = requestedShowImages,
                    )
                }
                effectiveReplaceMode = ReaderSettingsStore.DEFAULT_REPLACE_MODE
                if (contentResult.isSuccess && readerUiOptions.replaceMode.isBlank()) {
                    readerUiOptions = readerUiOptions.copy(replaceMode = ReaderSettingsStore.DEFAULT_REPLACE_MODE)
                    readerSettingsStore.save(readerUiOptions.toReaderSettingsValues())
                }
            }
            val contentFromCache = contentResult.isFailure && cached != null
            if (contentFromCache) contentResult = Result.success(cached!!.content)
            val contentValue = contentResult.getOrNull()
            if (
                requestSerial == readerRequestSerial &&
                isFreshReaderResult(currentRoute, readerState, bookId, chapterId)
            ) {
                // Publish the core body first. The directory, chapter comments, and favourite
                // status are useful secondary panels and must never make a readable chapter look
                // stuck on a spinner when one auxiliary endpoint is slow or unavailable.
                readerState = readerStateAfterCoreContent(
                    current = readerState,
                    content = contentResult.toLoadResult("阅读器正文"),
                    chapterContents = contentValue?.let { content -> chapterWindow(content, content.title) }.orEmpty(),
                    contentFromCache = contentFromCache,
                )
                launch {
                    val commentsResult = comments.await()
                    if (
                        requestSerial == readerRequestSerial &&
                        isFreshReaderResult(currentRoute, readerState, bookId, chapterId)
                    ) {
                        readerState = readerState.copy(
                            comments = commentsResult.toLoadResult(VisibleUiLabels.ChapterComments),
                            chapterCommentStates = readerState.chapterCommentStates + (
                                chapterId to ReaderChapterCommentState(
                                    comments = commentsResult.toLoadResult(VisibleUiLabels.ChapterComments),
                                    bookReferences = emptyMap(),
                                )
                            ),
                        )
                        commentsResult.getOrNull()?.let { commentList ->
                            loadReaderCommentBookReferences(
                                bookId = bookId,
                                chapterId = chapterId,
                                comments = commentList,
                                requestSerial = requestSerial,
                            )
                        }
                    }
                }
                launch {
                    val favoriteResult = favorite.await()
                    if (
                        requestSerial == readerRequestSerial &&
                        isFreshReaderResult(currentRoute, readerState, bookId, chapterId)
                    ) {
                        readerState = readerState.copy(
                            favoriteStatus = favoriteResult.toLoadResult("收藏状态"),
                        )
                    }
                }
            }
            val chaptersResult = chapters.await()
            val chapterTitle = chaptersResult.getOrNull()
                ?.firstOrNull { it.id == chapterId }
                ?.title
            if (
                requestSerial != readerRequestSerial ||
                !isFreshReaderResult(currentRoute, readerState, bookId, chapterId)
            ) return@launch

            // A restored session can outlive a source-side chapter deletion or an old deep link can
            // carry a chapter from another revision of the work.  Do not keep presenting that body
            // as if it belonged to this book: its chapter comments and adjacent navigation would
            // necessarily target the wrong route.  Empty catalogues remain recoverable/incomplete;
            // only a non-empty successful catalogue is strong evidence of stale membership.
            val catalogValue = chaptersResult.getOrNull()
            if (catalogValue != null && readerCatalogConfirmsStaleChapter(chapterId, catalogValue)) {
                // Secondary comment/favourite coroutines were launched above. Invalidate them
                // before replacing the body so a late response cannot repopulate the stale route.
                readerRequestSerial++
                readerCatalogRequestSerial++
                readerSessionStore.clear()
                readerState = readerState.copy(
                    content = LoadResult.Error(READER_STALE_CHAPTER_MESSAGE),
                    chapterContents = emptyList(),
                    chapters = LoadResult.Success(catalogValue),
                    comments = LoadResult.Error(READER_STALE_CHAPTER_MESSAGE),
                    chapterCommentStates = emptyMap(),
                    loadingNextChapter = false,
                    nextChapterError = null,
                    nextChapterWaitingForCatalog = false,
                    nextChapterEndConfirmationRequested = false,
                    nextChapterExhausted = false,
                    contentFromCache = false,
                    actionLoading = false,
                    actionMessage = READER_STALE_CHAPTER_MESSAGE,
                )
                return@launch
            }
            // A catalog-only retry may have started while the body request was in flight. Keep its
            // newer state (including Error/Loading) instead of overwriting it with this older call.
            val catalogWasSuperseded = catalogRequestSerial != readerCatalogRequestSerial
            val effectiveChapters = if (!catalogWasSuperseded) {
                chaptersResult.toLoadResult("阅读器目录")
            } else {
                readerState.chapters
            }
            val effectiveCatalogWaiting = if (catalogWasSuperseded) {
                readerState.nextChapterWaitingForCatalog
            } else {
                false
            }
            val effectiveCatalogExhausted = if (catalogWasSuperseded) {
                readerState.nextChapterExhausted
            } else {
                false
            }
            val cacheChapters = (effectiveChapters as? LoadResult.Success)?.value.orEmpty()
            if (contentValue != null && !contentFromCache) {
                val sourceUpdatedAt = cacheChapters.firstOrNull { it.id == chapterId }?.updatedAt
                withContext(Dispatchers.IO) {
                    readerChapterCacheStore.save(
                        bookId = bookId,
                        chapterId = chapterId,
                        replaceMode = effectiveReplaceMode,
                        showImages = requestedShowImages,
                        sourceUpdatedAt = sourceUpdatedAt,
                        content = contentValue,
                    )
                }
            }
            val cacheStates = if (cacheChapters.isEmpty()) {
                emptyMap()
            } else {
                withContext(Dispatchers.IO) {
                    readerChapterCacheStore.cacheStates(
                        bookId = bookId,
                        replaceMode = effectiveReplaceMode,
                        showImages = requestedShowImages,
                        chapters = cacheChapters,
                    )
                }
            }
            if (contentResult.isSuccess) {
                val chapterNumber = cacheChapters.takeIf { it.isNotEmpty() }?.let { chapters ->
                    val chapterIndex = chapters.indexOfFirst { it.id == chapterId }
                    if (chapterIndex < 0) {
                        null
                    } else {
                        chapters[chapterIndex].number?.takeIf { it > 0 } ?: (chapterIndex + 1)
                    }
                }
                val chapterCountAtLastRead = cacheChapters.size.takeIf { it > 0 }
                saveReaderProgress(
                    bookId = bookId,
                    chapterId = chapterId,
                    chapterTitle = chapterTitle ?: contentValue?.title,
                    chapterNumber = chapterNumber,
                    chapterCountAtLastRead = chapterCountAtLastRead,
                )
            }
            readerState = readerState.copy(
                content = contentResult.toLoadResult("阅读器正文"),
                chapterContents = contentValue?.let { content -> chapterWindow(content, chapterTitle ?: content.title) }.orEmpty(),
                chapters = effectiveChapters,
                nextChapterWaitingForCatalog = effectiveCatalogWaiting,
                nextChapterExhausted = effectiveCatalogExhausted,
                chapterCacheStates = cacheStates,
                contentFromCache = contentFromCache,
            )
        }
    }

    private fun isReplacementRestriction(error: Throwable?): Boolean {
        val message = error?.message.orEmpty()
        return message.contains("无替换") ||
            message.contains("政治考试") ||
            message.contains("political", ignoreCase = true) ||
            message.contains("whitelist", ignoreCase = true)
    }

    fun toggleReaderFavorite() {
        val bookId = readerState.bookId
        val current = (readerState.favoriteStatus as? LoadResult.Success)?.value ?: return
        if (bookId <= 0 || readerState.favoriteLoading) return
        readerState = readerState.copy(favoriteLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { api.toggleFavorite(bookId, current.isFavorited, current.groupId ?: 0L) }
            if (!isFreshReaderResult(currentRoute, readerState, bookId, readerState.chapterId)) return@launch
            readerState = result.fold(
                onSuccess = {
                    readerState.copy(
                        favoriteStatus = LoadResult.Success(it),
                        favoriteLoading = false,
                        actionMessage = if (it.isFavorited) "已加入收藏" else "已取消收藏",
                    )
                },
                onFailure = {
                    readerState.copy(
                        favoriteLoading = false,
                        actionMessage = apiFailureMessage("收藏", it),
                    )
                },
            )
        }
    }

    private fun saveReaderProgress(
        bookId: Long,
        chapterId: Long,
        chapterTitle: String?,
        chapterNumber: Int? = null,
        chapterCountAtLastRead: Int? = null,
    ) {
        val existing = readerProgressStore.load(bookId)
        readerProgressStore.save(
            bookId = bookId,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            bookTitle = readerProgressBookTitle(bookId) ?: existing?.bookTitle,
            chapterNumber = chapterNumber,
            chapterCountAtLastRead = chapterCountAtLastRead,
        )
        readerProgress = readerProgressStore.load()
        recentReaderProgresses = readerProgressStore.loadRecent(limit = READER_PROGRESS_HISTORY_LIMIT)
        updateLoadedCollectionProgress()
        readerProgressRevision += 1
        if (bookDetailState.bookId == bookId) {
            bookDetailState = bookDetailState.copy(readerProgress = readerProgressStore.load(bookId))
        }
        if (!authToken.isNullOrBlank()) {
            dependencies.readingProgressSync.request(bookId, chapterId)
        }
    }

    /** Prefer the currently loaded source detail; the shelf remains a useful native fallback. */
    private fun readerBookTitle(bookId: Long): String? {
        readerProgressBookTitle(bookId)?.let { return it }
        return (searchResults as? LoadResult.Success<List<NovelCard>>)
            ?.value
            ?.firstOrNull { it.id == bookId }
            ?.title
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }

    /** Prefer the currently loaded source detail; the shelf remains a useful native fallback. */
    private fun readerProgressBookTitle(bookId: Long): String? {
        val detailTitle = if (bookDetailState.bookId == bookId) {
            ((bookDetailState.book as? LoadResult.Success)?.value?.title)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        if (detailTitle != null) return detailTitle

        return buildList {
            addAll((homeState.favoriteEntries as? LoadResult.Success)?.value.orEmpty().map(FavoriteEntry::book))
            addAll((homeState.history as? LoadResult.Success)?.value.orEmpty().map(FavoriteEntry::book))
            addAll((homeState.favorites as? LoadResult.Success)?.value.orEmpty())
        }
            .firstOrNull { it.id == bookId }
            ?.title
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    /** Keep a loaded shelf visually current before its one post-reader source refresh returns. */
    private fun updateLoadedCollectionProgress() {
        val entries = (homeState.favoriteEntries as? LoadResult.Success)?.value ?: return
        val updatedEntries = favoriteEntriesWithLocalReaderProgress(entries, recentReaderProgresses)
        if (updatedEntries == entries) return
        homeState = homeState.copy(
            favorites = LoadResult.Success(updatedEntries.map(FavoriteEntry::book)),
            favoriteEntries = LoadResult.Success(updatedEntries),
        )
    }

    /**
     * Shelf/history responses contain the chapter saved by the website. Apply only a strictly
     * newer source chapter to its per-book native record; this never writes a website progress
     * request and never promotes a remote book over existing native recent-reading entries.
     */
    private fun reconcileRemoteReaderProgress(entries: List<FavoriteEntry>) {
        var changed = false
        entries.forEach { entry ->
            val existing = readerProgressStore.load(entry.book.id)
            val merged = readerProgressAfterRemoteFavoriteProgress(existing, entry)
            if (merged != null && merged != existing) {
                changed = readerProgressStore.saveRemoteProgress(merged) || changed
            }
        }
        if (!changed) return
        readerProgress = readerProgressStore.load()
        recentReaderProgresses = readerProgressStore.loadRecent(limit = READER_PROGRESS_HISTORY_LIMIT)
        updateLoadedCollectionProgress()
        if (bookDetailState.bookId > 0L) {
            bookDetailState = bookDetailState.copy(
                readerProgress = readerProgressStore.load(bookDetailState.bookId),
            )
        }
    }

    /** Repairs older local progress records that predate persisted book titles. */
    private fun resolveMissingReaderProgressBookTitle() {
        val progress = readerProgress ?: return
        val localBookTitle = readerProgressBookTitle(progress.bookId)
        if (!readerProgressNeedsBookTitleLookup(progress, localBookTitle)) return
        if (readerProgressTitleLookupBookId == progress.bookId) return

        val bookId = progress.bookId
        readerProgressTitleLookupBookId = bookId
        viewModelScope.launch {
            val resolvedTitle = runCatching { api.bookDetail(bookId).title }
                .getOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            if (readerProgressTitleLookupBookId != bookId) return@launch
            readerProgressTitleLookupBookId = null

            val current = readerProgress
            if (
                resolvedTitle == null ||
                current?.bookId != bookId ||
                !current.bookTitle.isNullOrBlank()
            ) {
                return@launch
            }
            readerProgressStore.save(
                bookId = current.bookId,
                chapterId = current.chapterId,
                chapterTitle = current.chapterTitle,
                bookTitle = resolvedTitle,
                chapterNumber = current.chapterNumber,
                chapterCountAtLastRead = current.chapterCountAtLastRead,
            )
            readerProgress = readerProgressStore.load()
            recentReaderProgresses = readerProgressStore.loadRecent(limit = READER_PROGRESS_HISTORY_LIMIT)
            if (bookDetailState.bookId == bookId) {
                bookDetailState = bookDetailState.copy(readerProgress = readerProgressStore.load(bookId))
            }
        }
    }

    /**
     * The reader's route stays anchored to the originally opened chapter during infinite scroll.
     * This separate visibility callback makes background recovery and Collection's continue card
     * resume at the body the person actually reached, rather than at the first window entry.
     */
    fun recordVisibleReaderChapter(chapterId: Long, chapterTitle: String?) {
        val route = currentRoute as? AppRoute.Reader ?: return
        val visible = readerState.chapterContents.firstOrNull { it.chapterId == chapterId } ?: return
        if (route.bookId != readerState.bookId || visible.chapterId != chapterId) return
        if (readerState.visibleChapterId != chapterId) readerState = readerState.copy(visibleChapterId = chapterId)
        val visibleCatalog = (readerState.chapters as? LoadResult.Success)?.value
        val chapterNumber = visibleCatalog?.let { chapters ->
            val chapterIndex = chapters.indexOfFirst { it.id == chapterId }
            if (chapterIndex < 0) {
                null
            } else {
                chapters[chapterIndex].number?.takeIf { it > 0 } ?: (chapterIndex + 1)
            }
        }
        val existing = readerProgressStore.load(route.bookId)
        if (
            existing?.chapterId == chapterId &&
            existing.chapterTitle == chapterTitle &&
            (chapterNumber == null || existing.chapterNumber == chapterNumber)
        ) return
        readerSessionStore.save(route.bookId, chapterId)
        saveReaderProgress(
            bookId = route.bookId,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            chapterNumber = chapterNumber,
            chapterCountAtLastRead = visibleCatalog?.size?.takeIf { it > 0 },
        )
    }

    /**
     * Saves the exact same-chapter viewport locally without turning a normal scroll into repeated
     * website progress requests.  The source service only understands chapters; item/offset state
     * is deliberately private to this native reader.
     */
    fun recordReaderViewportAnchor(anchor: ReaderViewportAnchor) {
        val route = currentRoute as? AppRoute.Reader ?: return
        if (
            route.bookId != readerState.bookId ||
            anchor.chapterId <= 0L ||
            anchor.itemIndexWithinChapter < 0 ||
            anchor.itemScrollOffsetPx < 0
        ) return

        val visibleChapter = readerState.chapterContents.firstOrNull { it.chapterId == anchor.chapterId }
        if (anchor.chapterId != readerState.chapterId && visibleChapter == null) return

        val existing = readerProgressStore.load(route.bookId)
        if (
            existing?.chapterId == anchor.chapterId &&
            existing.viewportItemIndex == anchor.itemIndexWithinChapter &&
            existing.viewportItemScrollOffsetPx == anchor.itemScrollOffsetPx
        ) return

        val catalog = (readerState.chapters as? LoadResult.Success)?.value
        val chapterNumber = catalog?.let { chapters ->
            val index = chapters.indexOfFirst { it.id == anchor.chapterId }
            if (index < 0) null else chapters[index].number?.takeIf { it > 0 } ?: (index + 1)
        }
        val chapterTitle = visibleChapter?.title?.takeIf { it.isNotBlank() }
            ?: (readerState.content as? LoadResult.Success)
                ?.value
                ?.title
                ?.takeIf { anchor.chapterId == readerState.chapterId && it.isNotBlank() }
            ?: existing?.takeIf { it.chapterId == anchor.chapterId }?.chapterTitle

        readerProgressStore.save(
            bookId = route.bookId,
            chapterId = anchor.chapterId,
            chapterTitle = chapterTitle,
            bookTitle = readerProgressBookTitle(route.bookId) ?: existing?.bookTitle,
            chapterNumber = chapterNumber,
            chapterCountAtLastRead = catalog?.size?.takeIf { it > 0 },
            viewportItemIndex = anchor.itemIndexWithinChapter,
            viewportItemScrollOffsetPx = anchor.itemScrollOffsetPx,
        )
        readerProgress = readerProgressStore.load()
        recentReaderProgresses = readerProgressStore.loadRecent(limit = READER_PROGRESS_HISTORY_LIMIT)
        updateLoadedCollectionProgress()
        readerProgressRevision += 1
        if (bookDetailState.bookId == route.bookId) {
            bookDetailState = bookDetailState.copy(readerProgress = readerProgressStore.load(route.bookId))
        }
    }

    private fun clearReaderSessionWhenLeaving() {
        if (currentRoute is AppRoute.Reader) readerSessionStore.clear()
    }

    private fun saveFavoritesOptions() {
        favoritesSettingsStore.save(
            favoritesUiOptions.toPersistedFavoritesSettings(
                selectedDisplayGroupId = selectedFavoriteGroupId,
                searchQuery = bookshelfQuery
            )
        )
    }

    private fun resetFavoritesPage() {
        resetHomeGridScrollPosition()
        if (favoritesUiOptions.currentPage != 1) {
            favoritesUiOptions = favoritesUiOptions.copy(currentPage = 1)
        }
    }

    private fun runFavoritesMutation(successMessage: String, mutation: suspend () -> Unit) {
        libraryFeature.mutation(successMessage,mutation){loadHome(successMessage)}
    }

    private fun onLibraryPageLoaded() {
        syncedCollectionProgressRevision=readerProgressRevision
        favoritesUiOptions=favoritesUiOptions.copy(currentPage=homeState.favoritesPage.coerceAtLeast(1))
        saveFavoritesOptions()
    }

    private fun selectedFavoriteEntries(): List<FavoriteEntry> =
        ((homeState.favoriteEntries as? LoadResult.Success)?.value ?: emptyList())
            .filter { entry -> entry.book.id in homeState.selectedBookIds && entry.favoriteId != null }

    private fun FavoritePage.canLoadMore(): Boolean =
        totalPages?.let { page < it }
            ?: total?.let { page * pageSize < it }
            ?: (items.size >= PAGE_SIZE)

    private fun SearchPage.canLoadMore(): Boolean =
        totalPages?.let { page < it }
            ?: total?.let { page.toLong() * pageSize < it }
            ?: (items.size >= pageSize)

    private fun resetHomeGridScrollPosition() {
        if (homeGridScrollPosition != GridScrollPosition()) homeGridScrollPosition = GridScrollPosition()
    }

    private fun resetForumScrollPosition() {
        forumFeature.resetScroll()
    }

    private fun mergeBooksById(current: List<NovelCard>, next: List<NovelCard>): List<NovelCard> {
        if (next.isEmpty()) return current
        return (current + next).distinctBy { it.id }
    }

    private fun mergeFavoriteEntriesByBookId(
        current: List<FavoriteEntry>,
        next: List<FavoriteEntry>
    ): List<FavoriteEntry> {
        if (next.isEmpty()) return current
        return (current + next).distinctBy { it.book.id }
    }

    private fun MutableList<AppRoute>.replaceWith(next: List<AppRoute>) {
        if (this == next) return
        clear()
        addAll(next)
    }

    private fun <T> Result<T>.toLoadResult(label: String): LoadResult<T> =
        fold(
            onSuccess = { LoadResult.Success(it) },
            onFailure = { LoadResult.Error(apiFailureMessage(label, it)) }
        )

    override fun onCleared() {
        searchFeature.close()
        forumFeature.close()
        forumPostFeature.close()
        libraryFeature.close()
        bookFeature.close()
        messageInboxFeature.close()
        messageDetailFeature.close()
        conversationFeature.close()
        messageSettingsFeature.close()
        profileFeature.close()
        publicProfileFeature.close()
        workspaceFeature.close()
        uploadFeature.close()
        adminFeature.close()
        super.onCleared()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val READER_PROGRESS_HISTORY_LIMIT = 20
        private const val BOOK_COMMENT_PAGE_SIZE = 30
        // The mobile source renders sixty search cards per explicit page.
        private const val SEARCH_PAGE_SIZE = 60
        private const val NATIVE_DOWNLOAD_ORPHAN_AGE_SECONDS = 10 * 60L
        private const val SEARCH_TAG_SUGGESTION_LIMIT = 100
        private const val TOOLS_MESSAGE_PREVIEW_LIMIT = 6
        private val FAVORITES_SORT_FIELDS = setOf("created_at", "last_read_time", "updated_at")
        private val FAVORITES_SORT_ORDERS = setOf("asc", "desc")
    }
}

internal fun resolveUserLoadResult(
    remote: Result<UserProfile>,
    tokenProfile: UserProfile?
): LoadResult<UserProfile> = remote.fold(
    onSuccess = { LoadResult.Success(it) },
    onFailure = { failure ->
        tokenProfile?.let { LoadResult.Success(it) }
            ?: LoadResult.Error(apiFailureMessage("登录状态", failure))
    }
)

private fun PersistedSearchSettings.toSearchOptions(): SearchOptions =
    SearchOptions(
        sortBy = sortBy,
        sortOrder = sortOrder,
        scope = scope,
        matchType = matchType,
        adultFilter = adultFilter,
        source = source,
        wordCountRange = wordCountRange,
        requiredTags = requiredTags,
        blockedTags = blockedTags,
        advancedSyntaxEnabled = advancedSyntaxEnabled,
        viewMode = if (viewMode == "list") SearchViewMode.List else SearchViewMode.Grid,
        cacheEnabled = cacheEnabled
    )

private fun SearchOptions.toPersistedSearchSettings(): PersistedSearchSettings =
    PersistedSearchSettings(
        sortBy = sortBy,
        sortOrder = sortOrder,
        scope = scope,
        matchType = matchType,
        adultFilter = adultFilter,
        source = source,
        wordCountRange = wordCountRange,
        requiredTags = requiredTags,
        blockedTags = blockedTags,
        advancedSyntaxEnabled = advancedSyntaxEnabled,
        viewMode = if (viewMode == SearchViewMode.List) "list" else "grid",
        cacheEnabled = cacheEnabled
    )

private fun PersistedFavoritesSettings.toFavoritesUiOptions(): FavoritesUiOptions =
    FavoritesUiOptions(
        cacheMode = cacheMode,
        tab = if (tab == "history") FavoritesContentTab.History else FavoritesContentTab.Favorites,
        layout = if (layout == "list") FavoritesLayout.List else FavoritesLayout.Grid,
        gridColumns = com.novalpie.nativeapp.data.normalizeGridColumns(gridColumns),
        displayMode = when (displayMode) {
            "all" -> FavoritesDisplayMode.All
            "unclassified" -> FavoritesDisplayMode.Unclassified
            else -> FavoritesDisplayMode.Default
        },
        currentPage = currentPage.coerceAtLeast(1),
        sortField = sortField,
        sortOrder = sortOrder
    )

private fun FavoritesUiOptions.toPersistedFavoritesSettings(
    selectedDisplayGroupId: Long?,
    searchQuery: String
): PersistedFavoritesSettings =
    PersistedFavoritesSettings(
        cacheMode = cacheMode,
        tab = if (tab == FavoritesContentTab.History) "history" else "favorites",
        layout = if (layout == FavoritesLayout.List) "list" else "grid",
        gridColumns = com.novalpie.nativeapp.data.normalizeGridColumns(gridColumns),
        displayMode = when (displayMode) {
            FavoritesDisplayMode.Default -> "default"
            FavoritesDisplayMode.All -> "all"
            FavoritesDisplayMode.Unclassified -> "unclassified"
        },
        selectedDisplayGroupId = selectedDisplayGroupId,
        currentPage = currentPage,
        sortField = sortField,
        sortOrder = sortOrder,
        searchQuery = searchQuery
    )

internal fun favoritesCacheModeLabel(mode: FavoritesCacheMode): String = when (mode) {
    FavoritesCacheMode.None -> "不缓存收藏设置"
    FavoritesCacheMode.NoSearch -> "不缓存搜索框信息"
    FavoritesCacheMode.All -> "缓存所有内容"
}
