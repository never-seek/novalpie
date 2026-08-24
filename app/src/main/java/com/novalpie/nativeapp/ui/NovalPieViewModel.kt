package com.novalpie.nativeapp.ui

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.webkit.CookieManager
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
import com.novalpie.nativeapp.data.EpubParser
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
import com.novalpie.nativeapp.data.NativeEpubExportProgress
import com.novalpie.nativeapp.data.NativeEpubMetadata
import com.novalpie.nativeapp.data.copyNativeDownloadFile
import com.novalpie.nativeapp.data.cleanupNativeEpubTempFiles
import com.novalpie.nativeapp.data.nativeEpubGenerationFile
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
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
    val history: LoadResult<List<FavoriteEntry>> = LoadResult.Idle,
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
    val bookQuery: String = "",
    val booksGridColumns: Int = 2,
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
    Inventory
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
    val actionMessage: String? = null
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

data class MessageCenterState(
    val query: MessageQuery = MessageQuery(),
    val messages: LoadResult<List<SiteMessage>> = LoadResult.Idle,
    val pagination: MessagePagination = MessagePagination(),
    val stats: LoadResult<MessageStats> = LoadResult.Idle,
    val selectedIds: Set<Long> = emptySet(),
    val loadingMore: Boolean = false,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null
)

data class MessageDetailState(
    val messageId: Long = 0,
    val detail: LoadResult<SiteMessage> = LoadResult.Idle,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null
)

data class MessageConversationState(
    val targetUserId: Long = 0,
    val targetName: String? = null,
    val messages: LoadResult<List<DirectMessage>> = LoadResult.Idle,
    val draft: String = "",
    val sending: Boolean = false,
    val actionMessage: String? = null
)

data class MessageSettingsState(
    val settings: LoadResult<MessageSettings> = LoadResult.Idle,
    val draft: MessageSettings = MessageSettings(),
    val saving: Boolean = false,
    val actionMessage: String? = null
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
    val actionMessage: String? = null
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
    val actionMessage: String? = null
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
    val expandedCommentIds: Set<Long> = emptySet(),
    val actionMessage: String? = null,
    val actionLoading: Boolean = false
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
    val favoriteStatus: LoadResult<FavoriteStatus> = LoadResult.Idle,
    val managementPermissions: LoadResult<BookEditPermissions> = LoadResult.Idle,
    val readerProgress: ReaderProgress? = null,
    val commentDraft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
    val favoriteLoading: Boolean = false,
    val actionMessage: String? = null,
    val actionLoading: Boolean = false
)

enum class NativeBookDownloadFormat {
    Epub,
    Txt,
}

data class NativeEpubDownloadState(
    val bookId: Long = 0,
    val format: NativeBookDownloadFormat? = null,
    val busy: Boolean = false,
    val progress: NativeEpubExportProgress? = null,
    val message: String? = null,
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
    val content: LoadResult<ReaderContent> = LoadResult.Idle,
    val chapterContents: List<ReaderChapterContent> = emptyList(),
    val chapters: LoadResult<List<Chapter>> = LoadResult.Idle,
    val comments: LoadResult<List<ChapterComment>> = LoadResult.Idle,
    /** Continuous reading keeps one independent comment panel state per rendered chapter. */
    val chapterCommentStates: Map<Long, ReaderChapterCommentState> = emptyMap(),
    val favoriteStatus: LoadResult<FavoriteStatus> = LoadResult.Idle,
    val favoriteLoading: Boolean = false,
    val loadingNextChapter: Boolean = false,
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
    val sortBy: String = "relevance",
    val sortOrder: String = "desc",
    val scope: String = "all",
    val matchType: String = "fuzzy_strict",
    /** Matches the live mobile source's initial `adult_filter=unrestricted` request. */
    val adultFilter: String = "unrestricted",
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
    private val appThemeSettingsStore = AppThemeSettingsStore(application)
    private val chineseVariantSettingsStore = ChineseVariantSettingsStore(application)
    private val searchHistoryStore = SearchHistoryStore(application)
    private val searchSettingsStore = SearchSettingsStore(application)
    private val favoritesSettingsStore = FavoritesSettingsStore(application)
    private val profileBooksSettingsStore = ProfileBooksSettingsStore(application)
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

    private val api: NovalPieApi = NovalPieApi(
        cookieProvider = {
            runCatching { CookieManager.getInstance().getCookie("https://novalpie.cc") }.getOrNull()
        },
        authTokenProvider = { authToken },
        proxySelectorProvider = {
            proxySettings.toProxySelector(
                emulatorRuntime = isEmulatorRuntime()
            )
        }
    )

    private val startupReaderSession: ReaderSession? = readerSessionStore.load()
    private val routes = mutableStateListOf<AppRoute>().apply {
        addAll(readerSessionRouteStack(startupReaderSession))
    }
    private var forumRequestSerial = 0L
    private var bookDetailRequestSerial = 0L
    private var terminologyRequestSerial = 0L
    private var bookEditRequestSerial = 0L
    private var bookChapterRequestSerial = 0L
    private var homeRequestSerial = 0L
    private var profileRequestSerial = 0L
    private var userProfileRequestSerial = 0L
    private var adminRequestSerial = 0L
    private var toolsRequestSerial = 0L
    private var messageCenterRequestSerial = 0L
    private var messageDetailRequestSerial = 0L
    private var messageConversationRequestSerial = 0L
    private var messageSettingsRequestSerial = 0L
    private var workspaceRequestSerial = 0L
    private var uploadRequestSerial = 0L
    private var editorRequestSerial = 0L
    private var editorProcessorRequestSerial = 0L
    private var searchRequestSerial = 0L
    private var authRequestSerial = 0L
    private var readerRequestSerial = 0L
    private var readerCatalogRequestSerial = 0L
    private var imagePreviewRequestSerial = 0L
    /** One in-flight source detail lookup prevents home refreshes from duplicating legacy repair. */
    private var readerProgressTitleLookupBookId: Long? = null
    private val initialFavoritesSettings = favoritesSettingsStore.load()
    private val initialProfileBooksSettings = profileBooksSettingsStore.load()
    private var selectedFavoriteGroupId: Long? = initialFavoritesSettings.selectedDisplayGroupId
    private var favoritesUiOptions = initialFavoritesSettings.toFavoritesUiOptions()

    var currentTab by mutableStateOf(BottomTab.Collection)
        private set
    var forumState by mutableStateOf(ForumState())
        private set
    internal var forumScrollPosition by mutableStateOf(GridScrollPosition())
        private set
    var forumPostDetailState by mutableStateOf(ForumPostDetailState())
        private set
    var forumCreateState by mutableStateOf(ForumCreateState())
        private set
    var homeState by mutableStateOf(HomeState())
        private set
    var profileState by mutableStateOf(
        ProfileState(booksGridColumns = initialProfileBooksSettings.gridColumns)
    )
        private set
    var userProfileDetailState by mutableStateOf(UserProfileDetailState())
        private set
    var adminState by mutableStateOf(AdminState())
        private set
    var toolsState by mutableStateOf(ToolsState())
        private set
    var messageCenterState by mutableStateOf(MessageCenterState())
        private set
    var messageDetailState by mutableStateOf(MessageDetailState())
        private set
    var messageConversationState by mutableStateOf(MessageConversationState())
        private set
    var messageSettingsState by mutableStateOf(MessageSettingsState())
        private set
    var workspaceState by mutableStateOf(
        WorkspaceState(
            localApis = workspaceLocalStore.loadApis(),
            jobs = workspaceLocalStore.loadJobs()
        )
    )
        private set
    var uploadBookState by mutableStateOf(UploadBookState())
        private set
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
    var searchKeyword by mutableStateOf("")
        private set
    var searchHistory by mutableStateOf(searchHistoryStore.load())
        private set
    var searchOptions by mutableStateOf(searchSettingsStore.load().toSearchOptions())
        private set
    var searchResults by mutableStateOf<LoadResult<List<NovelCard>>>(LoadResult.Idle)
        private set
    var searchTags by mutableStateOf<LoadResult<List<NovelTag>>>(LoadResult.Idle)
        private set
    var searchPage by mutableIntStateOf(1)
        private set
    /** Last successful source search envelope, used for source-style direct page navigation. */
    var searchResultPage by mutableStateOf<SearchPage?>(null)
        private set
    var searchCanLoadMore by mutableStateOf(false)
        private set
    var searchLoadingMore by mutableStateOf(false)
        private set

    /** See [HomeState.favoritesLoadMoreError]; search had the identical defect. */
    var searchLoadMoreError by mutableStateOf<String?>(null)
        private set
    internal var searchGridScrollPosition by mutableStateOf(GridScrollPosition())
        private set
    var bookCatalogQuery by mutableStateOf("")
        private set
    var bookDetailState by mutableStateOf(BookDetailState())
        private set
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

    init {
        configureNovalPieImageLoader(application, proxySettings)
        // The app opens on Collection. Loading an unseen forum feed here competes with the
        // authenticated shelf requests through the same proxy/CDN route; Forum loads on tab or
        // deep-link entry instead.
        startupReaderSession?.let { session ->
            loadReader(session.bookId, session.chapterId)
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
        if (searchKeyword == value) return
        invalidateSearchRequests()
        resetSearchGridScrollPosition()
        searchKeyword = value
        // A result grid belongs to the submitted query, not the text currently being edited.
        // Leaving prior cards below an empty or different field made it too easy to open the
        // wrong book while a request was intentionally invalidated.
        searchResults = LoadResult.Idle
        searchPage = 1
        searchResultPage = null
        searchCanLoadMore = false
        searchLoadingMore = false
        searchLoadMoreError = null
        if (value.isBlank() && currentRoute == AppRoute.Search) loadDefaultSearchResultsIfNeeded()
    }

    fun useSearchHistory(keyword: String) {
        performSearch(keyword)
    }

    fun useSearchTag(tagName: String) {
        applySearchTag(tagName, SearchTagFilterMode.Required)
    }

    fun clearSearchHistory() {
        searchHistoryStore.clear()
        searchHistory = emptyList()
    }

    /** Mirrors the source toolbar: keep the active filters, but opt in/out of retaining them locally. */
    fun toggleSearchSettingsCache() {
        val nextEnabled = !searchOptions.cacheEnabled
        searchOptions = searchOptions.copy(cacheEnabled = nextEnabled)
        searchSettingsStore.setCacheEnabled(nextEnabled)
        if (nextEnabled) saveSearchOptions()
    }

    /** Clears local search-option state only; no source API request or search history is changed. */
    fun clearSearchSettingsCache() {
        invalidateSearchRequests()
        val cacheEnabled = searchOptions.cacheEnabled
        searchSettingsStore.clearCachedSettings()
        searchOptions = SearchOptions(cacheEnabled = cacheEnabled)
        searchResults = LoadResult.Idle
        searchPage = 1
        searchResultPage = null
        searchCanLoadMore = false
        searchLoadingMore = false
        searchLoadMoreError = null
    }

    fun updateSearchSortBy(value: String) {
        val changed = searchOptions.sortBy != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(sortBy = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    fun updateSearchSortOrder(value: String) {
        val changed = searchOptions.sortOrder != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(sortOrder = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    fun updateSearchScope(value: String) {
        val changed = searchOptions.scope != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(scope = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    fun updateSearchMatchType(value: String) {
        val changed = searchOptions.matchType != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(matchType = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    fun updateSearchAdultFilter(value: String) {
        val changed = searchOptions.adultFilter != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(adultFilter = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    fun updateSearchSource(value: String) {
        val changed = searchOptions.source != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(source = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    fun updateSearchWordCountRange(value: String) {
        val changed = searchOptions.wordCountRange != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(wordCountRange = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    fun updateSearchAdvancedSyntaxEnabled(value: Boolean) {
        val changed = searchOptions.advancedSyntaxEnabled != value
        if (changed) invalidateSearchRequests()
        searchOptions = searchOptions.copy(advancedSyntaxEnabled = value)
        saveSearchOptions()
        refreshSearchAfterFilterChange(changed)
    }

    /** Source search toggles its local view mode without rerunning or invalidating the query. */
    fun toggleSearchViewMode() {
        resetSearchGridScrollPosition()
        searchOptions = searchOptions.copy(
            viewMode = if (searchOptions.viewMode == SearchViewMode.Grid) {
                SearchViewMode.List
            } else {
                SearchViewMode.Grid
            }
        )
        saveSearchOptions()
    }

    fun applyRequiredSearchTag(tagName: String) {
        applySearchTag(tagName, SearchTagFilterMode.Required)
    }

    fun applyBlockedSearchTag(tagName: String) {
        applySearchTag(tagName, SearchTagFilterMode.Blocked)
    }

    fun removeSearchTag(tagName: String) {
        val (requiredTags, blockedTags) = removeSearchTagFilter(
            requiredTags = searchOptions.requiredTags,
            blockedTags = searchOptions.blockedTags,
            tagName = tagName
        )
        updateSearchTagsAndRun(requiredTags, blockedTags)
    }

    fun clearSearchTags() {
        updateSearchTagsAndRun(emptyList(), emptyList())
    }

    private fun applySearchTag(tagName: String, mode: SearchTagFilterMode) {
        val (requiredTags, blockedTags) = toggleSearchTagFilters(
            requiredTags = searchOptions.requiredTags,
            blockedTags = searchOptions.blockedTags,
            input = tagName,
            mode = mode
        )
        updateSearchTagsAndRun(requiredTags, blockedTags)
    }

    private fun updateSearchTagsAndRun(requiredTags: List<String>, blockedTags: List<String>) {
        val next = searchOptions.copy(
            requiredTags = normalizeSearchTagList(requiredTags),
            blockedTags = normalizeSearchTagList(blockedTags)
        )
        if (next == searchOptions) return
        invalidateSearchRequests()
        searchOptions = next
        saveSearchOptions()
        performSearch()
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

    /** Appends the next source chapter to the current reader window for website-style scrolling. */
    fun loadNextReaderChapter() {
        val route = currentRoute as? AppRoute.Reader ?: return
        val state = readerState
        if (
            state.loadingNextChapter ||
            state.nextChapterExhausted ||
            state.nextChapterWaitingForCatalog
        ) return
        val chapters = (state.chapters as? LoadResult.Success)?.value
        // The initial catalog request owns the Loading state. A failed/idle catalog can be retried
        // explicitly, while a successful partial catalog is refreshed once by the branch below.
        if (chapters == null) {
            if (state.chapters is LoadResult.Idle) refreshReaderCatalog()
            return
        }
        val loadedIds = state.chapterContents.map { it.chapterId }.toSet().ifEmpty { setOf(state.chapterId) }
        // An empty/incomplete catalog is not evidence that this is the last chapter. Refresh only
        // once for this sentinel state; the refreshed result decides whether to retry or to show a
        // visible manual retry affordance.
        if (readerCatalogIsIncomplete(state.chapterId, chapters)) {
            readerState = state.copy(nextChapterWaitingForCatalog = true)
            refreshReaderCatalog()
            return
        }
        val next = nextReaderChapterForInfiniteScroll(state.chapterId, chapters, loadedIds)
        if (next == null) {
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
            loadingNextChapter = true,
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
                val existing = readerState.chapterContents
                readerState = readerState.copy(
                    chapterContents = (existing + ReaderChapterContent(next.id, next.title, content))
                        .distinctBy { it.chapterId },
                    loadingNextChapter = false,
                    nextChapterError = null,
                    nextChapterWaitingForCatalog = false,
                    nextChapterEndConfirmationRequested = false,
                    nextChapterExhausted = false,
                    chapterCacheStates = cacheStates,
                    contentFromCache = readerState.contentFromCache || servedFromCache,
                    chapterCommentStates = readerState.chapterCommentStates + (
                        next.id to ReaderChapterCommentState(comments = LoadResult.Loading)
                    ),
                )
                launch {
                    val commentsResult = chapterComments.await()
                    if (
                        requestSerial != readerRequestSerial ||
                        currentRoute != route ||
                        readerState.bookId != route.bookId ||
                        readerState.chapterId != route.chapterId
                    ) return@launch
                    val existingCommentState = readerChapterCommentState(readerState, next.id)
                    readerState = readerState.copy(
                        chapterCommentStates = readerState.chapterCommentStates + (
                            next.id to existingCommentState.copy(
                                comments = commentsResult.toLoadResult(VisibleUiLabels.ChapterComments),
                            )
                        ),
                    )
                }
            } else {
                readerState = readerState.copy(
                    loadingNextChapter = false,
                    nextChapterError = apiFailureMessage("下一章", result.exceptionOrNull() ?: IllegalStateException("缓存不可用")),
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
        configureNovalPieImageLoader(getApplication(), next)
        loadHome()
    }

    fun saveCapturedAuthToken(token: String) {
        val normalized = token.trim()
        if (normalized.isBlank() || normalized == authToken) return
        authSessionStore.saveToken(normalized)
        authToken = normalized
        loadHome()
    }

    fun clearAuthToken() {
        authSessionStore.clearToken()
        authToken = null
        profileRequestSerial++
        profileState = ProfileState()
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

        routes.clear()
        routes.add(targetRoute)
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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.Settings))
    }

    fun loadProfile() {
        val requestSerial = ++profileRequestSerial
        val tokenProfile = authToken?.let(::decodeAuthTokenProfile)
        val displayedProfile = (profileState.profile as? LoadResult.Success)?.value
        val selectedTab = profileState.selectedTab
        val activityFilter = profileState.activityFilter
        val personalizationTab = profileState.personalizationTab
        val bookQuery = profileState.bookQuery
        profileState = profileState.copy(
            // A JWT contains only identity fields. Rendering it as a full profile made a cold
            // account visit briefly claim 0 points/posts and a missing avatar before `/me`
            // arrived. Retain a real profile on refresh, otherwise show the explicit sync state.
            profile = displayedProfile?.let { LoadResult.Success(it) } ?: LoadResult.Loading,
            checkinStats = LoadResult.Loading,
            checkinRecords = LoadResult.Loading,
            activities = LoadResult.Loading,
            books = LoadResult.Loading,
            inventory = LoadResult.Loading,
            shopItems = LoadResult.Loading,
            quizReward = LoadResult.Loading,
            actionMessage = null
        )
        viewModelScope.launch {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val profileResult = async { runCatching { api.currentUser() } }
            val statsResult = async { runCatching { api.currentUserCheckinStats() } }
            val recordsResult = async {
                runCatching { api.userCheckinRecords(startDate = "$currentYear-01-01", endDate = "$currentYear-12-31") }
            }
            val activitiesResult = async {
                runCatching {
                    val ownerId = profileResult.await().getOrNull()?.id ?: tokenProfile?.id
                    requireNotNull(ownerId) { "当前账号缺少用户 ID" }
                    // Source ActivityTab opens a single 200-item window. Keeping that
                    // contract prevents an active account timeline from looking empty simply
                    // because its newest relevant entries sit after the legacy 100-item cap.
                    api.userContentActivityFeed(ownerId, limit = 200)
                }
            }
            val booksResult = async {
                runCatching {
                    api.currentUserUploadedBooks()
                }
            }
            val inventoryResult = async { runCatching { api.currentUserInventory() } }
            val shopItemsResult = async { runCatching { api.shopItems() } }
            val quizRewardResult = async { runCatching { api.currentUserQuizRewardStatus() } }
            val resolvedProfile = resolveUserLoadResult(profileResult.await(), tokenProfile)
            val statsCall = statsResult.await()
            val recordsCall = recordsResult.await()
            val resolvedRecords = recordsCall.toLoadResult("签到记录")
            val sourceStats = statsCall.getOrNull()
            val sourceRecords = recordsCall.getOrNull().orEmpty()
            val resolvedStats = if (sourceStats != null || sourceRecords.isNotEmpty()) {
                LoadResult.Success(
                    reconcileCheckinStats(
                        source = sourceStats ?: UserCheckinStats(),
                        records = sourceRecords,
                        today = String.format(Locale.US, "%tF", Calendar.getInstance())
                    )
                )
            } else {
                statsCall.toLoadResult("签到统计")
            }
            val sourceProfile = (resolvedProfile as? LoadResult.Success)?.value
            val activitiesCall = activitiesResult.await()
            val activityFeed = activitiesCall.getOrNull()
            val resolvedActivities: LoadResult<List<UserActivity>> = activityFeed?.let { feed ->
                LoadResult.Success(feed.activities)
            }
                ?: if (
                    profileHasNoPublicActivities(sourceProfile) ||
                    sourceActivitiesEndpointUnavailable(activitiesCall.exceptionOrNull())
                ) {
                    LoadResult.Success(emptyList<UserActivity>())
                }
                else LoadResult.Error(
                    activitiesCall.exceptionOrNull()?.message ?: "个人动态暂时无法加载"
                )
            val booksCall = booksResult.await()
            val resolvedBooks = booksCall.getOrNull()?.let { LoadResult.Success(it) }
                ?: if (sourceBooksEndpointUnavailable(booksCall.exceptionOrNull())) {
                    LoadResult.Success(emptyList())
                } else {
                    booksCall.toLoadResult("上传书籍")
                }
            val inventoryCall = inventoryResult.await()
            val resolvedInventory = inventoryCall.toLoadResult("背包")
            val resolvedShopItems = shopItemsResult.await().toLoadResult("商店")
            val profile = profileWithEquippedCosmetics(
                profile = profileWithPublicCollectionCounts(
                    profile = sourceProfile,
                    feed = activityFeed,
                    novels = booksCall.getOrNull(),
                ),
                inventory = inventoryCall.getOrNull()
            )
            val resolvedProfileWithFrame: LoadResult<UserProfile> = profile?.let { LoadResult.Success(it) } ?: resolvedProfile
            val resolvedQuizReward = quizRewardResult.await().toLoadResult("奖励状态")
            if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@launch
            profileState = ProfileState(
                profile = resolvedProfileWithFrame,
                checkinStats = resolvedStats,
                checkinRecords = resolvedRecords,
                activities = resolvedActivities,
                books = resolvedBooks,
                bookQuery = bookQuery,
                inventory = resolvedInventory,
                shopItems = resolvedShopItems,
                quizReward = resolvedQuizReward,
                selectedTab = selectedTab,
                activityFilter = activityFilter,
                personalizationTab = personalizationTab,
                nameDraft = profile?.name.orEmpty(),
                bioDraft = profile?.bio.orEmpty(),
                showCheckin = profile?.showCheckin ?: true,
                autoCheckin = profile?.autoCheckin ?: false
            )
            if (profile != null) homeState = homeState.copy(user = LoadResult.Success(profile))
        }
    }

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

    fun selectPersonalizationTab(tab: PersonalizationTab) {
        if (profileState.personalizationTab != tab) {
            profileState = profileState.copy(personalizationTab = tab, actionMessage = null)
        }
    }

    fun toggleCurrentUserEquipment(item: UserInventoryItem) {
        if (item.itemId <= 0 || profileState.inventoryActionInventoryId != null) return
        if (authToken.isNullOrBlank()) {
            profileState = profileState.copy(actionMessage = "请先登录后再管理装扮")
            return
        }
        val requestSerial = ++profileRequestSerial
        val action = if (item.equipped) "unequip" else "equip"
        profileState = profileState.copy(
            inventoryActionInventoryId = item.inventoryId,
            actionMessage = if (action == "equip") "正在装备…" else "正在卸下…"
        )
        viewModelScope.launch {
            val result = runCatching {
                val mutation = api.setCurrentUserEquipment(item.itemId, action)
                val inventory = api.currentUserInventory()
                val profile = api.currentUser()
                Triple(mutation, inventory, profile)
            }
            if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@launch
            result.onSuccess { (mutation, inventory, remoteProfile) ->
                val profile = profileWithEquippedCosmetics(remoteProfile, inventory) ?: remoteProfile
                profileState = profileState.copy(
                    profile = LoadResult.Success(profile),
                    inventory = LoadResult.Success(inventory),
                    inventoryActionInventoryId = null,
                    actionMessage = mutation.message
                        ?: if (mutation.success) {
                            if (action == "equip") "装备成功" else "已卸下"
                        } else {
                            "操作未完成"
                        }
                )
                homeState = homeState.copy(user = LoadResult.Success(profile))
            }.onFailure { failure ->
                profileState = profileState.copy(
                    inventoryActionInventoryId = null,
                    actionMessage = apiFailureMessage(if (action == "equip") "装备" else "卸下", failure)
                )
            }
        }
    }

    fun purchaseCurrentUserShopItem(item: ShopItem) {
        if (item.id <= 0 || profileState.shopPurchaseItemId != null) return
        if (authToken.isNullOrBlank()) {
            profileState = profileState.copy(actionMessage = "请先登录后再购买装扮")
            return
        }
        val requestSerial = ++profileRequestSerial
        profileState = profileState.copy(
            shopPurchaseItemId = item.id,
            actionMessage = "正在购买“${item.name}”…"
        )
        viewModelScope.launch {
            val result = runCatching {
                val mutation = api.purchaseShopItem(item.id)
                val inventory = api.currentUserInventory()
                val shopItems = api.shopItems()
                val profile = api.currentUser()
                PurchasedShopRefresh(
                    mutation = mutation,
                    inventory = inventory,
                    shopItems = shopItems,
                    profile = profile,
                )
            }
            if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@launch
            result.onSuccess { refreshed ->
                val profile = profileWithEquippedCosmetics(refreshed.profile, refreshed.inventory) ?: refreshed.profile
                profileState = profileState.copy(
                    profile = LoadResult.Success(profile),
                    inventory = LoadResult.Success(refreshed.inventory),
                    shopItems = LoadResult.Success(refreshed.shopItems),
                    shopPurchaseItemId = null,
                    actionMessage = refreshed.mutation.message
                        ?: if (refreshed.mutation.success) "购买成功" else "购买未完成"
                )
                homeState = homeState.copy(user = LoadResult.Success(profile))
            }.onFailure { failure ->
                profileState = profileState.copy(
                    shopPurchaseItemId = null,
                    actionMessage = apiFailureMessage("购买装扮", failure)
                )
            }
        }
    }

    private data class PurchasedShopRefresh(
        val mutation: ShopPurchaseResult,
        val inventory: UserInventory,
        val shopItems: List<ShopItem>,
        val profile: UserProfile,
    )

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

    fun updateProfileName(value: String) {
        profileState = profileState.copy(nameDraft = value, actionMessage = null)
    }

    fun updateProfileBio(value: String) {
        profileState = profileState.copy(bioDraft = value, actionMessage = null)
    }

    fun updateProfileShowCheckin(value: Boolean) {
        profileState = profileState.copy(showCheckin = value, actionMessage = null)
    }

    fun updateProfileAutoCheckin(value: Boolean) {
        profileState = profileState.copy(autoCheckin = value, actionMessage = null)
    }

    fun updateProfileAdultBirthYear(value: String) {
        profileState = profileState.copy(
            adultBirthYearDraft = value.filter(Char::isDigit).take(4),
            actionMessage = null
        )
    }

    fun saveProfile() {
        if (profileState.saving || profileState.checkingIn) return
        val profile = (profileState.profile as? LoadResult.Success)?.value
            ?: currentUserProfile()
            ?: run {
                profileState = profileState.copy(actionMessage = "请先登录后再编辑资料")
                return
            }
        val normalizedName = profileState.nameDraft.trim()
        if (normalizedName.isBlank()) {
            profileState = profileState.copy(actionMessage = "用户名不能为空")
            return
        }
        val updated = profile.copy(
            name = normalizedName,
            bio = profileState.bioDraft.trim(),
            showCheckin = profileState.showCheckin,
            autoCheckin = profileState.autoCheckin
        )
        val requestSerial = ++profileRequestSerial
        profileState = profileState.copy(saving = true, actionMessage = "正在保存资料…")
        viewModelScope.launch {
            runCatching { api.updateCurrentUser(updated) }
                .onSuccess {
                    if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@onSuccess
                    profileState = profileState.copy(
                        profile = LoadResult.Success(updated),
                        saving = false,
                        actionMessage = "资料已保存"
                    )
                    homeState = homeState.copy(user = LoadResult.Success(updated))
                }
                .onFailure { failure ->
                    if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@onFailure
                    profileState = profileState.copy(
                        saving = false,
                        actionMessage = apiFailureMessage("保存资料", failure)
                    )
                }
        }
    }

    fun checkinCurrentUser() {
        if (profileState.checkingIn || profileState.saving) return
        if (authToken.isNullOrBlank()) {
            profileState = profileState.copy(actionMessage = "请先登录后再签到")
            return
        }
        val requestSerial = ++profileRequestSerial
        profileState = profileState.copy(checkingIn = true, actionMessage = "正在签到…")
        viewModelScope.launch {
            runCatching { api.checkinCurrentUser() }
                .onSuccess { action ->
                    val refreshedProfile = async { runCatching { api.currentUser() } }
                    val refreshedStats = async { runCatching { api.currentUserCheckinStats() } }
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val refreshedRecords = async {
                        runCatching {
                            api.userCheckinRecords(
                                startDate = "$currentYear-01-01",
                                endDate = "$currentYear-12-31"
                            )
                        }
                    }
                    val refreshedQuizReward = async { runCatching { api.currentUserQuizRewardStatus() } }
                    val profileResult = refreshedProfile.await()
                    val statsResult = refreshedStats.await()
                    val recordsResult = refreshedRecords.await()
                    val quizRewardResult = refreshedQuizReward.await()
                    if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@onSuccess
                    val current = (profileState.profile as? LoadResult.Success)?.value
                    val resolvedProfile = resolveUserLoadResult(profileResult, current)
                    profileState = profileState.copy(
                        profile = resolvedProfile,
                        checkinStats = if (statsResult.getOrNull() != null || recordsResult.getOrNull().orEmpty().isNotEmpty()) {
                            LoadResult.Success(
                                reconcileCheckinStats(
                                    source = statsResult.getOrNull() ?: UserCheckinStats(),
                                    records = recordsResult.getOrNull().orEmpty(),
                                    today = String.format(Locale.US, "%tF", Calendar.getInstance())
                                )
                            )
                        } else {
                            statsResult.toLoadResult("签到统计")
                        },
                        checkinRecords = recordsResult.toLoadResult("签到记录"),
                        quizReward = quizRewardResult.toLoadResult("奖励状态"),
                        checkingIn = false,
                        actionMessage = action.message ?: if (action.success) "签到成功" else "签到未完成"
                    )
                    (resolvedProfile as? LoadResult.Success)?.value?.let { user ->
                        homeState = homeState.copy(user = LoadResult.Success(user))
                    }
                }
                .onFailure { failure ->
                    if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@onFailure
                    profileState = profileState.copy(
                        checkingIn = false,
                        actionMessage = apiFailureMessage("签到", failure)
                    )
                }
        }
    }

    fun verifyCurrentUserAdult() {
        if (profileState.verifyingAdult || profileState.saving || profileState.checkingIn) return
        if (authToken.isNullOrBlank()) {
            profileState = profileState.copy(actionMessage = "请先登录后再进行成年验证")
            return
        }
        val birthYear = profileState.adultBirthYearDraft.toIntOrNull()
        if (birthYear == null || birthYear !in 1900..Calendar.getInstance().get(Calendar.YEAR)) {
            profileState = profileState.copy(actionMessage = "请输入有效的出生年份")
            return
        }
        val requestSerial = ++profileRequestSerial
        profileState = profileState.copy(verifyingAdult = true, actionMessage = "正在提交成年验证…")
        viewModelScope.launch {
            runCatching { api.verifyCurrentUserAdult(birthYear) }
                .onSuccess { action ->
                    if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@onSuccess
                    val current = (profileState.profile as? LoadResult.Success)?.value
                    val verified = current?.copy(isAdult = action.success)
                    profileState = profileState.copy(
                        profile = verified?.let { LoadResult.Success(it) } ?: profileState.profile,
                        verifyingAdult = false,
                        actionMessage = action.message ?: if (action.success) "成年验证已完成" else "成年验证未通过"
                    )
                    verified?.let { homeState = homeState.copy(user = LoadResult.Success(it)) }
                }
                .onFailure { failure ->
                    if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@onFailure
                    profileState = profileState.copy(
                        verifyingAdult = false,
                        actionMessage = apiFailureMessage("成年验证", failure)
                    )
                }
        }
    }

    fun uploadProfileAvatar(rawUri: String) {
        if (profileState.uploadingAvatar || rawUri.isBlank()) return
        val requestSerial = ++profileRequestSerial
        profileState = profileState.copy(uploadingAvatar = true, actionMessage = "正在上传头像…")
        viewModelScope.launch {
            val result = runCatching {
                val document = readUploadDocument(rawUri)
                require(document.sizeBytes > 0L) { "头像文件为空" }
                require(document.mimeType?.startsWith("image/") == true) { "请选择图片文件" }
                api.uploadCurrentUserAvatar(uploadSource(document))
                api.currentUser()
            }
            if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@launch
            result.onSuccess { refreshed ->
                profileState = profileState.copy(
                    profile = LoadResult.Success(refreshed),
                    uploadingAvatar = false,
                    actionMessage = "头像已更新"
                )
                homeState = homeState.copy(user = LoadResult.Success(refreshed))
            }.onFailure { failure ->
                profileState = profileState.copy(
                    uploadingAvatar = false,
                    actionMessage = apiFailureMessage("上传头像", failure)
                )
            }
        }
    }

    fun openUserProfile(userId: Long) {
        if (userId <= 0) return
        val ownId = currentUserProfile()?.id
        if (ownId != null && ownId == userId) {
            currentTab = BottomTab.Profile
            routes.clear()
            routes.add(AppRoute.Profile)
            loadProfile()
            return
        }
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.UserProfileDetail(userId)))
        loadUserProfile(userId)
    }

    fun loadUserProfile(userId: Long) {
        if (userId <= 0) return
        val requestSerial = ++userProfileRequestSerial
        userProfileDetailState = UserProfileDetailState(
            userId = userId,
            profile = LoadResult.Loading,
            activities = LoadResult.Loading,
            books = LoadResult.Loading,
            checkinStats = LoadResult.Loading,
            checkinRecords = LoadResult.Loading,
            checkinSettings = LoadResult.Loading,
            selectedTab = userProfileDetailState.selectedTab,
            activityFilter = userProfileDetailState.activityFilter,
        )
        viewModelScope.launch {
            val profile = async { runCatching { api.userProfile(userId) } }
            val activities = async {
                runCatching { api.userContentActivityFeed(userId = userId, limit = 200) }
            }
            val books = async { runCatching { api.userNovels(userId = userId) } }
            val stats = async { runCatching { api.userCheckinStats(userId) } }
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val records = async {
                runCatching {
                    api.userCheckinRecords(userId, "$currentYear-01-01", "$currentYear-12-31")
                }
            }
            val settings = async { runCatching { api.userCheckinSettings(userId) } }
            val profileResult = profile.await()
            val activityResult = activities.await()
            val booksResult = books.await()
            val statsResult = stats.await()
            val recordsResult = records.await()
            val settingsResult = settings.await()
            if (!isFreshRequestSerial(requestSerial, userProfileRequestSerial)) return@launch
            val publicProfile = profileResult.getOrNull()
            val publicProfilePresentation = publicProfileLoadPresentation(
                profile = publicProfile,
                feed = activityResult.getOrNull(),
                novels = booksResult.getOrNull(),
            )
            val resolvedPublicActivities: LoadResult<List<UserActivity>> =
                publicProfilePresentation.activities?.let { LoadResult.Success(it) }
                    ?: if (
                        profileHasNoPublicActivities(publicProfile) ||
                        sourceActivitiesEndpointUnavailable(activityResult.exceptionOrNull())
                    ) {
                        LoadResult.Success(emptyList())
                    } else {
                        LoadResult.Error(activityResult.exceptionOrNull()?.message ?: "用户动态暂时无法加载")
                    }
            userProfileDetailState = userProfileDetailState.copy(
                userId = userId,
                profile = publicProfilePresentation.profile?.let { LoadResult.Success(it) }
                    ?: profileResult.toLoadResult("用户资料"),
                activities = resolvedPublicActivities,
                books = booksResult.getOrNull()?.let { LoadResult.Success(it) }
                    ?: if (sourceBooksEndpointUnavailable(booksResult.exceptionOrNull())) {
                        LoadResult.Success(emptyList())
                    } else {
                        booksResult.toLoadResult("用户作品")
                    },
                checkinStats = statsResult.toLoadResult("签到统计"),
                checkinRecords = recordsResult.toLoadResult("签到记录"),
                checkinSettings = settingsResult.toLoadResult("签到设置")
            )
        }
    }

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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.Admin(section)))
        loadAdminSection(section)
    }

    fun loadAdminSection(section: AdminSection = adminState.section) {
        loadAdminSectionInternal(section, null)
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

    private fun loadAdminSectionInternal(section: AdminSection, message: String?) {
        if (!isAdminProfile(currentUserProfile())) return
        val requestSerial = ++adminRequestSerial
        val overviewDays = adminState.overviewDays
        val reviewQuery = adminState.reviewQuery
        val operationLogQuery = adminState.operationLogQuery
        val shopQuery = adminState.shopQuery
        adminState = when (section) {
            AdminSection.Overview -> adminState.copy(
                section = section,
                overview = LoadResult.Loading,
                actionLoading = false,
                actionMessage = message
            )
            AdminSection.Review -> adminState.copy(
                section = section,
                reviewSettings = LoadResult.Loading,
                reviewRequests = LoadResult.Loading,
                actionLoading = false,
                actionMessage = message
            )
            AdminSection.Keys -> adminState.copy(
                section = section,
                keys = LoadResult.Loading,
                baseUrlRules = LoadResult.Loading,
                actionLoading = false,
                actionMessage = message
            )
            AdminSection.OperationLogs -> adminState.copy(
                section = section,
                operationLogs = LoadResult.Loading,
                actionLoading = false,
                actionMessage = message
            )
            AdminSection.Scraper -> adminState.copy(
                section = section,
                cookieConfigs = LoadResult.Loading,
                schedulerLogs = LoadResult.Loading,
                actionLoading = false,
                actionMessage = message
            )
            AdminSection.Shop -> adminState.copy(
                section = section,
                shopItems = LoadResult.Loading,
                actionLoading = false,
                actionMessage = message
            )
        }
        viewModelScope.launch {
            when (section) {
                AdminSection.Overview -> {
                    val result = runCatching { api.adminOverview(days = overviewDays) }
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(overview = result.toLoadResult("管理总览"))
                }
                AdminSection.Review -> {
                    val settings = async { runCatching { api.adminReviewSettings() } }
                    val requests = async {
                        runCatching {
                            api.adminReviewRequests(
                                type = reviewQuery.type.trim(),
                                status = reviewQuery.status.trim(),
                                keyword = reviewQuery.keyword.trim()
                            )
                        }
                    }
                    val settingsResult = settings.await()
                    val requestsResult = requests.await()
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(
                        reviewSettings = settingsResult.toLoadResult("审核设置"),
                        reviewRequests = requestsResult.toLoadResult("审核请求")
                    )
                }
                AdminSection.Keys -> {
                    val keys = async { runCatching { api.adminKeys() } }
                    val rules = async { runCatching { api.adminBaseUrlRules() } }
                    val keysResult = keys.await()
                    val rulesResult = rules.await()
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(
                        keys = keysResult.toLoadResult("Key 管理"),
                        baseUrlRules = rulesResult.toLoadResult("BaseURL 规则")
                    )
                }
                AdminSection.OperationLogs -> {
                    val result = runCatching {
                        api.adminOperationLogs(
                            page = operationLogQuery.page,
                            action = operationLogQuery.action.trim(),
                            status = operationLogQuery.status.trim(),
                            userId = operationLogQuery.userId.trim(),
                            novelId = operationLogQuery.novelId.trim(),
                            keyword = operationLogQuery.keyword.trim(),
                            startDate = operationLogQuery.startDate.trim(),
                            endDate = operationLogQuery.endDate.trim()
                        )
                    }
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(operationLogs = result.toLoadResult("操作日志"))
                }
                AdminSection.Scraper -> {
                    val cookies = async { runCatching { api.adminCookieConfigs() } }
                    val logs = async { runCatching { api.adminSchedulerLogs() } }
                    val cookieResult = cookies.await()
                    val logResult = logs.await()
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(
                        cookieConfigs = cookieResult.toLoadResult("Cookie 配置"),
                        schedulerLogs = logResult.toLoadResult("调度日志")
                    )
                }
                AdminSection.Shop -> {
                    val result = runCatching {
                        api.adminShopItems(
                            type = shopQuery.type.trim(),
                            active = shopQuery.isActive,
                            keyword = shopQuery.keyword.trim()
                        )
                    }
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(shopItems = result.toLoadResult("商店商品"))
                }
            }
        }
    }

    fun toggleAdminReviewSetting(kind: String) {
        val settings = (adminState.reviewSettings as? LoadResult.Success)?.value ?: return
        val upload = if (kind == "upload") !settings.autoApproveUpload else settings.autoApproveUpload
        val delete = if (kind == "delete") !settings.autoApproveDelete else settings.autoApproveDelete
        runAdminMutation("更新审核设置", "审核设置已更新") {
            api.adminUpdateReviewSettings(upload, delete)
        }
    }

    fun approveAllAdminReviews() {
        val query = adminState.reviewQuery
        if (query.status.isNotBlank() && query.status != "pending") return
        runAdminMutation("批量通过审核请求", "审核请求已批量处理") {
            api.adminApproveAllReviews(
                type = query.type.trim(),
                status = query.status.trim(),
                keyword = query.keyword.trim()
            )
        }
    }

    fun adminReviewAction(requestId: Long, action: String) {
        runAdminMutation("处理审核请求", if (action == "approve") "审核已通过" else "审核已拒绝") {
            api.adminReviewAction(requestId, action)
        }
    }

    fun updateAdminKeyStatus(keyId: Long, status: String) {
        runAdminMutation("更新 Key 状态", "Key 状态已更新") {
            api.adminUpdateKeyStatus(keyId, status)
        }
    }

    fun deleteAdminKey(keyId: Long) {
        runAdminMutation("删除 Key", "Key 已删除") { api.adminDeleteKey(keyId) }
    }

    fun toggleAdminCookieConfig(config: AdminCookieConfig) {
        runAdminMutation("更新 Cookie 配置", "Cookie 配置状态已更新") {
            api.adminSaveCookieConfig(config.copy(isActive = !config.isActive), cookieRaw = null)
        }
    }

    fun saveAdminCookieConfig(config: AdminCookieConfig, cookieRaw: String?) {
        runAdminMutation("保存 Cookie 配置", "Cookie 配置已保存") {
            api.adminSaveCookieConfig(config, cookieRaw)
        }
    }

    fun deleteAdminCookieConfig(configId: Long) {
        runAdminMutation("删除 Cookie 配置", "Cookie 配置已删除") {
            api.adminDeleteCookieConfig(configId)
        }
    }

    fun setAdminBaseUrlRuleAction(rule: AdminBaseUrlRule, action: String) {
        runAdminMutation("更新 BaseURL 规则", "BaseURL 规则已更新") {
            api.adminSaveBaseUrlRule(rule.copy(action = action))
        }
    }

    fun saveAdminBaseUrlRule(rule: AdminBaseUrlRule) {
        runAdminMutation("保存 BaseURL 规则", "BaseURL 规则已保存") {
            api.adminSaveBaseUrlRule(rule)
        }
    }

    fun deleteAdminBaseUrlRule(ruleId: Long) {
        runAdminMutation("删除 BaseURL 规则", "BaseURL 规则已删除") {
            api.adminDeleteBaseUrlRule(ruleId)
        }
    }

    fun toggleAdminShopItem(item: AdminShopItem) {
        runAdminMutation("更新商品状态", "商品状态已更新") {
            api.adminSaveShopItem(item.copy(isActive = !item.isActive))
        }
    }

    fun saveAdminShopItem(item: AdminShopItem) {
        runAdminMutation("保存商品", "商品已保存") { api.adminSaveShopItem(item) }
    }

    fun deleteAdminShopItem(itemId: Long) {
        runAdminMutation("删除商品", "商品已删除") { api.adminDeleteShopItem(itemId) }
    }

    private fun runAdminMutation(
        label: String,
        successMessage: String,
        block: suspend () -> UserCheckinAction
    ) {
        if (!isAdminProfile(currentUserProfile()) || adminState.actionLoading) return
        val section = adminState.section
        val requestSerial = ++adminRequestSerial
        adminState = adminState.copy(actionLoading = true, actionMessage = "$label…")
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { action ->
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@onSuccess
                    loadAdminSectionInternal(section, action.message ?: successMessage)
                }
                .onFailure { failure ->
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@onFailure
                    adminState = adminState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, failure)
                    )
                }
        }
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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.MessageCenter))
        loadMessageCenter()
    }

    fun loadMessageCenter() {
        val requestSerial = ++messageCenterRequestSerial
        val query = messageCenterState.query
        messageCenterState = messageCenterState.copy(
            messages = LoadResult.Loading,
            stats = LoadResult.Loading,
            pagination = MessagePagination(),
            selectedIds = emptySet(),
            loadingMore = false,
            actionMessage = null
        )
        viewModelScope.launch {
            val page = async { runCatching { api.messagePage(query, page = 1, pageSize = PAGE_SIZE) } }
            val stats = async { runCatching { api.messageStats() } }
            val pageResult = page.await()
            val statsResult = stats.await()
            if (!isFreshRequestSerial(requestSerial, messageCenterRequestSerial)) return@launch
            messageCenterState = messageCenterState.copy(
                messages = pageResult.fold(
                    onSuccess = { LoadResult.Success(it.items) },
                    onFailure = { LoadResult.Error(apiFailureMessage("消息列表", it)) }
                ),
                pagination = pageResult.getOrNull()?.pagination ?: MessagePagination(),
                stats = statsResult.toLoadResult("消息统计")
            )
        }
    }

    fun loadMoreMessages() {
        val current = (messageCenterState.messages as? LoadResult.Success)?.value ?: return
        val pagination = messageCenterState.pagination
        if (messageCenterState.loadingMore || pagination.page >= pagination.totalPages) return
        val requestSerial = ++messageCenterRequestSerial
        messageCenterState = messageCenterState.copy(loadingMore = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                api.messagePage(
                    query = messageCenterState.query,
                    page = pagination.page + 1,
                    pageSize = pagination.pageSize
                )
            }
            if (!isFreshRequestSerial(requestSerial, messageCenterRequestSerial)) return@launch
            messageCenterState = result.fold(
                onSuccess = { next ->
                    messageCenterState.copy(
                        messages = LoadResult.Success(mergeMessagePages(current, next.items)),
                        pagination = next.pagination,
                        loadingMore = false
                    )
                },
                onFailure = { failure ->
                    messageCenterState.copy(
                        loadingMore = false,
                        actionMessage = apiFailureMessage("加载更多消息", failure)
                    )
                }
            )
        }
    }

    fun updateMessageKeyword(value: String) {
        messageCenterState = messageCenterState.copy(
            query = messageCenterState.query.copy(keyword = value)
        )
    }

    fun applyMessageSearch() = loadMessageCenter()

    fun selectMessageType(type: Int?) {
        if (messageCenterState.query.messageType == type) return
        messageCenterState = messageCenterState.copy(query = messageCenterState.query.copy(messageType = type))
        loadMessageCenter()
    }

    fun selectMessageReadFilter(isRead: Boolean?) {
        if (messageCenterState.query.isRead == isRead) return
        messageCenterState = messageCenterState.copy(query = messageCenterState.query.copy(isRead = isRead))
        loadMessageCenter()
    }

    fun selectMessagePriority(priority: Int?) {
        if (messageCenterState.query.priority == priority) return
        messageCenterState = messageCenterState.copy(query = messageCenterState.query.copy(priority = priority))
        loadMessageCenter()
    }

    fun toggleMessageSelected(messageId: Long) {
        messageCenterState = messageCenterState.copy(
            selectedIds = toggleMessageSelection(messageCenterState.selectedIds, messageId)
        )
    }

    fun selectAllVisibleMessages(select: Boolean) {
        val ids = (messageCenterState.messages as? LoadResult.Success)?.value?.map { it.id }.orEmpty()
        messageCenterState = messageCenterState.copy(selectedIds = selectVisibleMessages(ids, select))
    }

    fun markSelectedMessagesRead() {
        val ids = messageCenterState.selectedIds.toList()
        if (ids.isEmpty()) return
        runMessageCenterAction("批量已读") { api.markMessagesRead(ids) }
    }

    fun deleteSelectedMessages() {
        val ids = messageCenterState.selectedIds.toList()
        if (ids.isEmpty()) return
        runMessageCenterAction("批量删除") { api.deleteMessages(ids) }
    }

    fun markAllMessagesRead() {
        runMessageCenterAction("全部已读") { api.markAllMessagesRead() }
    }

    fun toggleMessageStar(message: SiteMessage) {
        runMessageCenterAction(if (message.isStarred) "取消星标" else "添加星标") {
            api.starMessage(message.id, !message.isStarred)
        }
    }

    fun openMessage(message: SiteMessage) {
        if (message.type == 8) {
            val currentUserId = currentUserProfile()?.id
            val targetUserId = directMessageTargetUserId(message, currentUserId)
            if (targetUserId != null) {
                if (!message.isRead) {
                    viewModelScope.launch { runCatching { api.markMessageRead(message.id) } }
                }
                openMessageConversation(targetUserId, message.username)
                return
            }
        }
        openMessageDetail(message.id)
    }

    fun openMessageDetail(messageId: Long) {
        if (messageId <= 0) return
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.MessageDetail(messageId)))
        loadMessageDetail(messageId)
    }

    fun loadMessageDetail(messageId: Long) {
        val requestSerial = ++messageDetailRequestSerial
        messageDetailState = MessageDetailState(messageId = messageId, detail = LoadResult.Loading)
        viewModelScope.launch {
            val result = runCatching { api.messageDetail(messageId) }
            if (!isFreshRequestSerial(requestSerial, messageDetailRequestSerial)) return@launch
            messageDetailState = messageDetailState.copy(
                detail = result.toLoadResult("消息详情")
            )
        }
    }

    fun markCurrentMessageRead() {
        val message = (messageDetailState.detail as? LoadResult.Success)?.value ?: return
        if (message.isRead || messageDetailState.actionLoading) return
        runMessageDetailAction("已标记为已读") { api.markMessageRead(message.id) }
    }

    fun toggleCurrentMessageStar() {
        val message = (messageDetailState.detail as? LoadResult.Success)?.value ?: return
        if (messageDetailState.actionLoading) return
        runMessageDetailAction(if (message.isStarred) "已取消星标" else "已添加星标") {
            api.starMessage(message.id, !message.isStarred)
        }
    }

    fun deleteCurrentMessage() {
        val message = (messageDetailState.detail as? LoadResult.Success)?.value ?: return
        if (messageDetailState.actionLoading) return
        messageDetailState = messageDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { api.deleteMessage(message.id) }
            result.onSuccess {
                goBack()
                loadMessageCenter()
            }.onFailure { failure ->
                messageDetailState = messageDetailState.copy(
                    actionLoading = false,
                    actionMessage = apiFailureMessage("删除消息", failure)
                )
            }
        }
    }

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
        val segments = uri?.pathSegments.orEmpty()
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
        val route = AppRoute.MessageConversation(targetUserId, targetName)
        routes.replaceWith(pushDistinctRoute(routes.toList(), route))
        loadMessageConversation(targetUserId, targetName)
    }

    fun loadMessageConversation(targetUserId: Long, targetName: String?) {
        val requestSerial = ++messageConversationRequestSerial
        messageConversationState = MessageConversationState(
            targetUserId = targetUserId,
            targetName = targetName,
            messages = LoadResult.Loading
        )
        viewModelScope.launch {
            val result = runCatching { api.messageConversation(targetUserId) }
            if (!isFreshRequestSerial(requestSerial, messageConversationRequestSerial)) return@launch
            messageConversationState = messageConversationState.copy(
                messages = result.toLoadResult("私信对话")
            )
        }
    }

    fun updateMessageDraft(value: String) {
        messageConversationState = messageConversationState.copy(draft = value, actionMessage = null)
    }

    fun sendMessageDraft() {
        val profile = currentUserProfile() ?: return
        val currentUserId = profile.id ?: return
        val content = messageConversationState.draft.trim()
        val targetUserId = messageConversationState.targetUserId
        if (content.isBlank() || targetUserId <= 0 || messageConversationState.sending) return
        messageConversationState = messageConversationState.copy(sending = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                api.sendDirectMessage(
                    currentUserId = currentUserId,
                    targetUserId = targetUserId,
                    currentUserName = profile.name,
                    content = content
                )
            }
            messageConversationState = result.fold(
                onSuccess = {
                    messageConversationState.copy(
                        draft = "",
                        sending = false,
                        actionMessage = it.message ?: "私信已发送"
                    )
                },
                onFailure = { failure ->
                    messageConversationState.copy(
                        sending = false,
                        actionMessage = apiFailureMessage("发送私信", failure)
                    )
                }
            )
            if (result.isSuccess) loadMessageConversation(targetUserId, messageConversationState.targetName)
        }
    }

    fun openMessageSettings() {
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.MessageSettings))
        loadMessageSettings()
    }

    fun loadMessageSettings() {
        val requestSerial = ++messageSettingsRequestSerial
        messageSettingsState = MessageSettingsState(settings = LoadResult.Loading)
        viewModelScope.launch {
            val result = runCatching { api.messageSettings() }
            if (!isFreshRequestSerial(requestSerial, messageSettingsRequestSerial)) return@launch
            messageSettingsState = result.fold(
                onSuccess = { settings ->
                    MessageSettingsState(settings = LoadResult.Success(settings), draft = settings)
                },
                onFailure = { failure ->
                    MessageSettingsState(
                        settings = LoadResult.Error(apiFailureMessage("消息设置", failure)),
                        actionMessage = apiFailureMessage("消息设置", failure)
                    )
                }
            )
        }
    }

    fun updateMessageSettingsDraft(transform: (MessageSettings) -> MessageSettings) {
        messageSettingsState = messageSettingsState.copy(
            draft = transform(messageSettingsState.draft),
            actionMessage = null
        )
    }

    fun saveMessageSettings() {
        if (messageSettingsState.saving) return
        validateMessageSettings(messageSettingsState.draft)?.let { error ->
            messageSettingsState = messageSettingsState.copy(actionMessage = error)
            return
        }
        messageSettingsState = messageSettingsState.copy(saving = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { api.updateMessageSettings(messageSettingsState.draft) }
            messageSettingsState = result.fold(
                onSuccess = {
                    messageSettingsState.copy(
                        settings = LoadResult.Success(messageSettingsState.draft),
                        saving = false,
                        actionMessage = it.message ?: "消息设置已保存"
                    )
                },
                onFailure = { failure ->
                    messageSettingsState.copy(
                        saving = false,
                        actionMessage = apiFailureMessage("保存消息设置", failure)
                    )
                }
            )
        }
    }

    private fun runMessageCenterAction(
        label: String,
        action: suspend () -> com.novalpie.nativeapp.model.MessageActionResult
    ) {
        if (messageCenterState.actionLoading) return
        messageCenterState = messageCenterState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { action() }
            messageCenterState = result.fold(
                onSuccess = {
                    messageCenterState.copy(
                        actionLoading = false,
                        // Braces are required, not stylistic: Kotlin identifiers may contain CJK
                        // letters, so "$label已同步" parses as a reference to `label已同步`.
                        actionMessage = it.message ?: "${label}已同步",
                        selectedIds = emptySet()
                    )
                },
                onFailure = { failure ->
                    messageCenterState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, failure)
                    )
                }
            )
            if (result.isSuccess) loadMessageCenter()
        }
    }

    private fun runMessageDetailAction(
        successMessage: String,
        action: suspend () -> com.novalpie.nativeapp.model.MessageActionResult
    ) {
        val messageId = messageDetailState.messageId
        messageDetailState = messageDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { action() }
            messageDetailState = result.fold(
                onSuccess = { messageDetailState.copy(actionLoading = false, actionMessage = it.message ?: successMessage) },
                onFailure = { failure ->
                    messageDetailState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(successMessage, failure)
                    )
                }
            )
            if (result.isSuccess) loadMessageDetail(messageId)
        }
    }

    private fun currentUserProfile(): UserProfile? =
        (homeState.user as? LoadResult.Success)?.value ?: authToken?.let(::decodeAuthTokenProfile)

    fun openUploadBook() {
        if (uploadBookState.existingNovelId != null) uploadBookState = UploadBookState()
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.UploadBook))
    }

    fun updateUploadBookDraft(draft: UploadBookDraft) {
        uploadBookState = uploadBookState.copy(
            draft = draft.copy(chapterCount = (uploadBookState.chapters as? LoadResult.Success)?.value?.size ?: 0),
            actionMessage = null,
            submitResult = LoadResult.Idle
        )
    }

    fun selectUploadEpub(rawUri: String) {
        if (uploadBookState.processing) return
        val requestSerial = ++uploadRequestSerial
        uploadBookState = uploadBookState.copy(
            processing = true,
            progressLabel = "正在读取 EPUB 文件…",
            chapters = LoadResult.Loading,
            submitResult = LoadResult.Idle,
            actionMessage = null,
            serverFilePath = null
        )
        viewModelScope.launch {
            val result = runCatching {
                val document = readUploadDocument(rawUri)
                if (!document.displayName.endsWith(".epub", ignoreCase = true)) {
                    throw IOException("仅支持 EPUB 格式文件")
                }
                if (document.sizeBytes == 0L) throw IOException("EPUB 文件为空")
                if (!isFreshRequestSerial(requestSerial, uploadRequestSerial)) return@launch
                uploadBookState = uploadBookState.copy(
                    selectedFile = document,
                    progressLabel = if (uploadParseMode(document.sizeBytes.coerceAtLeast(0L)) == UploadParseMode.SERVER_CHUNKED) {
                        "文件超过 50 MiB，正在按 5 MiB 流式分片上传…"
                    } else {
                        "正在本机解析 EPUB 目录与章节…"
                    }
                )
                val source = uploadSource(document)
                if (uploadParseMode(document.sizeBytes.coerceAtLeast(0L)) == UploadParseMode.SERVER_CHUNKED) {
                    val path = api.uploadFileInChunks(source)
                    api.parseUploadedEpub(path).copy(epubFilePath = path)
                } else {
                    withContext(Dispatchers.IO) { EpubParser.parse(source) }
                }
            }
            if (!isFreshRequestSerial(requestSerial, uploadRequestSerial)) return@launch
            uploadBookState = result.fold(
                onSuccess = { parsed ->
                    val current = uploadBookState.draft
                    uploadBookState.copy(
                        draft = current.copy(
                            title = current.title.ifBlank { parsed.title },
                            author = current.author.ifBlank { parsed.author },
                            description = current.description.ifBlank { parsed.description },
                            language = parsed.language.ifBlank { current.language },
                            chapterCount = parsed.chapters.size
                        ),
                        chapters = LoadResult.Success(parsed.chapters),
                        serverFilePath = parsed.epubFilePath,
                        processing = false,
                        progressLabel = null,
                        actionMessage = "EPUB 解析完成，共 ${parsed.chapters.size} 章"
                    )
                },
                onFailure = { failure ->
                    uploadBookState.copy(
                        chapters = LoadResult.Error(apiFailureMessage("解析 EPUB", failure)),
                        processing = false,
                        progressLabel = null,
                        actionMessage = apiFailureMessage("解析 EPUB", failure)
                    )
                }
            )
        }
    }

    fun submitUploadBook() {
        if (uploadBookState.processing) return
        val chapters = (uploadBookState.chapters as? LoadResult.Success)?.value.orEmpty()
        val draft = uploadBookState.draft.copy(chapterCount = chapters.size)
        val appendBookId = uploadBookState.existingNovelId
        val validation = if (appendBookId != null) {
            when {
                chapters.isEmpty() -> "请先选择并解析 EPUB 文件"
                draft.submitType !in setOf("chinese", "personal", "shared") -> "提交方式无效"
                else -> null
            }
        } else {
            validateUploadBookDraft(draft)
        }
        validation?.let { error ->
            uploadBookState = uploadBookState.copy(actionMessage = error)
            return
        }
        val document = uploadBookState.selectedFile ?: run {
            uploadBookState = uploadBookState.copy(actionMessage = "请先选择 EPUB 文件")
            return
        }
        uploadBookState = uploadBookState.copy(
            processing = true,
            progressLabel = "正在安全上传书籍与 ${chapters.size} 章内容…",
            submitResult = LoadResult.Loading,
            actionMessage = null
        )
        viewModelScope.launch {
            val request = UploadBookRequest(
                title = draft.title,
                titleTranslation = draft.titleTranslation,
                authorName = draft.author,
                description = draft.description,
                language = draft.language,
                spans = draft.spans,
                isAdult = draft.isAdult,
                source = draft.source,
                sourceUrl = draft.sourceUrl,
                tags = normalizeUploadTags(draft.tagsText),
                submitType = draft.submitType,
                chapters = chapters,
                epubFilePath = uploadBookState.serverFilePath,
                coverUrl = draft.coverUrl.takeIf { it.isNotBlank() }
            )
            val result = runCatching {
                val uploaded = if (appendBookId != null) {
                    api.appendManagedChapters(
                        bookId = appendBookId,
                        submitType = draft.submitType,
                        chapters = chapters,
                        epubFilePath = uploadBookState.serverFilePath,
                        epubFile = if (uploadBookState.serverFilePath == null) uploadSource(document) else null
                    )
                } else {
                    api.uploadBook(
                        upload = request,
                        epubFile = if (request.epubFilePath == null) uploadSource(document) else null
                    )
                }
                uploaded.also { if (!it.success) error(it.message ?: "上传失败") }
            }
            uploadBookState = result.fold(
                onSuccess = { uploaded ->
                    uploadBookState.copy(
                        processing = false,
                        progressLabel = null,
                        submitResult = LoadResult.Success(uploaded),
                        actionMessage = uploaded.message ?: if (appendBookId != null) "章节追加成功" else "上传成功"
                    )
                },
                onFailure = { failure ->
                    uploadBookState.copy(
                        processing = false,
                        progressLabel = null,
                        submitResult = LoadResult.Error(apiFailureMessage("上传书籍", failure)),
                        actionMessage = apiFailureMessage("上传书籍", failure)
                    )
                }
            )
        }
    }

    fun clearUploadBook() {
        uploadRequestSerial++
        uploadBookState = UploadBookState(existingNovelId = (currentRoute as? AppRoute.BookAppend)?.bookId)
    }

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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.UploadEditor))
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
        val archive = editorArchiveStore.load(id) ?: run {
            uploadEditorState = uploadEditorState.copy(actionMessage = "存档不存在")
            return
        }
        uploadEditorState = replaceEditorDocument(uploadEditorState.copy(
            text = archive.textContent,
            metadata = archive.metadata,
            fileName = archive.fileName,
            chapters = emptyList(),
            selectedTab = EditorTab.Text,
            actionMessage = "存档已加载，请重新生成章节目录"
        ))
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
                val appendBookId = routes.asReversed().filterIsInstance<AppRoute.BookAppend>().firstOrNull()?.bookId
                uploadBookState = UploadBookState(
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
                )
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "已发送到上传页")
                val target = appendBookId?.let { AppRoute.BookAppend(it) } ?: AppRoute.UploadBook
                val withoutEditor = routes.toMutableList().apply {
                    if (lastOrNull() is AppRoute.UploadEditor) removeAt(lastIndex)
                }
                routes.replaceWith(pushDistinctRoute(withoutEditor, target))
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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.PoliticalExam))
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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.Workspace))
        loadWorkspace()
    }

    fun selectWorkspaceTab(tab: WorkspaceTab) {
        workspaceState = workspaceState.copy(selectedTab = tab, actionMessage = null)
    }

    fun loadWorkspace() {
        val requestSerial = ++workspaceRequestSerial
        workspaceState = workspaceState.copy(
            apiConfigs = LoadResult.Loading,
            cookieStatus = LoadResult.Loading,
            cookieConfigs = LoadResult.Loading,
            health = LoadResult.Loading,
            localApis = workspaceLocalStore.loadApis(),
            jobs = workspaceLocalStore.loadJobs(),
            actionMessage = null
        )
        viewModelScope.launch {
            val apis = async { runCatching { api.workspaceApiConfigs() } }
            val cookieStatus = async { runCatching { api.workspaceCookieStatus() } }
            val cookies = async { runCatching { api.workspaceCookieConfigs() } }
            val health = async { runCatching { api.workspaceHealth() } }
            val apiResult = apis.await()
            val cookieStatusResult = cookieStatus.await()
            val cookieResult = cookies.await()
            val healthResult = health.await()
            if (!isFreshRequestSerial(requestSerial, workspaceRequestSerial)) return@launch
            workspaceState = workspaceState.copy(
                apiConfigs = apiResult.toLoadResult("工作区 API 配置"),
                cookieStatus = cookieStatusResult.toLoadResult("Cookie 状态"),
                cookieConfigs = cookieResult.toLoadResult("Cookie 配置"),
                health = healthResult.toLoadResult("工作区健康状态")
            )
        }
    }

    fun saveWorkspaceApi(draft: WorkspaceApiDraft) {
        validateWorkspaceApiDraft(draft)?.let { error ->
            workspaceState = workspaceState.copy(actionMessage = error)
            return
        }
        if (workspaceState.actionLoading) return
        workspaceState = workspaceState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                val serverResult = when {
                    draft.shareToServer && draft.serverId != null -> api.updateWorkspaceApi(
                        id = draft.serverId,
                        name = draft.name,
                        model = draft.model,
                        endpoint = draft.endpoint,
                        apiKey = draft.apiKey,
                        concurrency = draft.concurrency.toInt()
                    )
                    draft.shareToServer -> api.createWorkspaceApi(
                        name = draft.name,
                        model = draft.model,
                        endpoint = draft.endpoint,
                        apiKey = draft.apiKey,
                        concurrency = draft.concurrency.toInt()
                    )
                    draft.serverId != null -> api.deleteWorkspaceApi(draft.serverId)
                    else -> com.novalpie.nativeapp.model.WorkspaceActionResult(success = true)
                }
                if (!serverResult.success) error(serverResult.message ?: "Workspace API operation failed")
                val localId = draft.id ?: System.currentTimeMillis()
                workspaceLocalStore.upsertApi(
                    WorkspaceLocalApiConfig(
                        id = localId,
                        name = draft.name.trim(),
                        model = draft.model.trim(),
                        endpoint = draft.endpoint.trim(),
                        apiKey = draft.apiKey.trim(),
                        concurrency = draft.concurrency.toInt(),
                        sharedToServer = draft.shareToServer,
                        serverId = if (draft.shareToServer) serverResult.id ?: draft.serverId else null
                    )
                )
                serverResult
            }
            workspaceState = result.fold(
                onSuccess = {
                    workspaceState.copy(
                        localApis = workspaceLocalStore.loadApis(),
                        actionLoading = false,
                        actionMessage = it.message ?: "API 配置已保存"
                    )
                },
                onFailure = { failure ->
                    workspaceState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage("保存 API 配置", failure)
                    )
                }
            )
            if (result.isSuccess) loadWorkspace()
        }
    }

    fun deleteWorkspaceLocalApi(config: WorkspaceLocalApiConfig) {
        if (workspaceState.actionLoading) return
        workspaceState = workspaceState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                config.serverId?.let { api.deleteWorkspaceApi(it) }
                workspaceLocalStore.deleteApi(config.id)
            }
            workspaceState = result.fold(
                onSuccess = {
                    workspaceState.copy(
                        localApis = workspaceLocalStore.loadApis(),
                        actionLoading = false,
                        actionMessage = "API 配置已删除"
                    )
                },
                onFailure = { failure ->
                    workspaceState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage("删除 API 配置", failure)
                    )
                }
            )
            if (result.isSuccess) loadWorkspace()
        }
    }

    fun deleteWorkspaceServerApi(config: WorkspaceApiConfig) {
        runWorkspaceAction("API 配置已删除") { api.deleteWorkspaceApi(config.id) }
    }

    fun toggleWorkspaceApi(config: WorkspaceApiConfig) {
        runWorkspaceAction("API 状态已更新") { api.toggleWorkspaceApi(config.id) }
    }

    fun saveWorkspaceCookie(draft: WorkspaceCookieDraft) {
        validateWorkspaceCookieDraft(draft)?.let { error ->
            workspaceState = workspaceState.copy(actionMessage = error)
            return
        }
        runWorkspaceAction("Cookie 配置已保存") {
            if (draft.id == null) {
                api.createWorkspaceCookie(
                    configKey = draft.configKey,
                    description = draft.description,
                    cookieRaw = draft.cookieRaw,
                    proxyIp = draft.proxyIp,
                    isActive = draft.isActive
                )
            } else {
                api.updateWorkspaceCookie(
                    id = draft.id,
                    description = draft.description,
                    cookieRaw = draft.cookieRaw.takeIf { it.isNotBlank() },
                    proxyIp = draft.proxyIp,
                    isActive = draft.isActive
                )
            }
        }
    }

    fun toggleWorkspaceCookie(config: com.novalpie.nativeapp.model.WorkspaceCookieConfig) {
        runWorkspaceAction("Cookie 状态已更新") {
            api.setWorkspaceCookieActive(config.id, !config.isActive)
        }
    }

    fun deleteWorkspaceCookie(config: com.novalpie.nativeapp.model.WorkspaceCookieConfig) {
        runWorkspaceAction("Cookie 配置已删除") {
            api.deleteWorkspaceCookie(config.id)
        }
    }

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

    private fun runWorkspaceAction(
        successMessage: String,
        action: suspend () -> com.novalpie.nativeapp.model.WorkspaceActionResult
    ) {
        if (workspaceState.actionLoading) return
        workspaceState = workspaceState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                action().also { response ->
                    if (!response.success) error(response.message ?: "工作区操作失败")
                }
            }
            workspaceState = result.fold(
                onSuccess = {
                    workspaceState.copy(
                        actionLoading = false,
                        actionMessage = it.message ?: successMessage
                    )
                },
                onFailure = { failure ->
                    workspaceState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(successMessage, failure)
                    )
                }
            )
            if (result.isSuccess) loadWorkspace()
        }
    }

    fun updateForumSearchQuery(value: String) {
        if (forumState.searchQuery == value) return
        val wasSearching = forumState.searchQuery.isNotBlank()
        ++forumRequestSerial
        forumState = forumState.copy(
            searchQuery = value,
            reviewTotal = null,
            page = 1,
            totalPages = null,
            canLoadMore = false,
            loadingMore = false,
            loadMoreError = null
        )
        // Clearing the field should restore the source feed immediately; new text is submitted
        // explicitly from the search IME action so typing does not spawn a request per character.
        if (wasSearching && value.isBlank()) loadForum()
    }

    fun selectForumCategory(type: String) {
        val normalizedType = type.trim().ifBlank { "discussion" }
        if (forumState.selectedType == normalizedType) return
        forumState = forumState.copy(
            selectedType = normalizedType,
            reviewTotal = null,
            page = 1,
            totalPages = null,
            canLoadMore = false,
            loadingMore = false,
            loadMoreError = null
        )
        resetForumScrollPosition()
        loadForum()
    }

    fun updateForumHideSpoilers(hideSpoilers: Boolean) {
        val current = forumState
        if (current.hideSpoilers == hideSpoilers) return
        forumState = if (current.selectedType == "review") {
            current.copy(
                hideSpoilers = hideSpoilers,
                page = 1,
                totalPages = null,
                canLoadMore = false,
                loadingMore = false,
                loadMoreError = null
            )
        } else {
            // Other forum surfaces only need a local redraw. The review endpoint alone uses the
            // preference as a request filter for pure-spoiler records.
            current.copy(hideSpoilers = hideSpoilers)
        }
        if (current.selectedType == "review") loadForum()
    }

    fun loadForum() = loadForumPage(forumState.page)

    /** Direct source-page navigation replaces the former load-more-only forum footer. */
    fun goToForumPage(page: Int) {
        val current = forumState
        val totalPages = current.totalPages?.coerceAtLeast(1) ?: return
        val targetPage = page.coerceIn(1, totalPages)
        if (targetPage == current.page || current.posts is LoadResult.Loading) return
        loadForumPage(targetPage)
    }

    private fun loadForumPage(targetPage: Int) {
        resetForumScrollPosition()
        val requestSerial = ++forumRequestSerial
        val request = forumState
        val requestedPage = targetPage.coerceAtLeast(1)
        forumState = request.copy(
            posts = LoadResult.Loading,
            page = requestedPage,
            canLoadMore = false,
            loadingMore = false,
            loadMoreError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                api.forumPostsPage(
                    page = requestedPage,
                    limit = PAGE_SIZE,
                    type = request.selectedType,
                    search = request.searchQuery,
                    hideSpoilers = request.hideSpoilers
                )
            }
            if (!isFreshRequestSerial(requestSerial, forumRequestSerial)) return@launch
            forumState = result.fold(
                onSuccess = { resultPage ->
                    val posts = resultPage.posts
                    val resolvedPage = resultPage.page.coerceAtLeast(1)
                    forumState.copy(
                        posts = LoadResult.Success(posts),
                        reviewTotal = if (request.selectedType == "review") resultPage.total else null,
                        page = resolvedPage,
                        totalPages = resultPage.totalPages,
                        canLoadMore = resultPage.totalPages?.let { resolvedPage < it } ?: posts.size == PAGE_SIZE,
                        loadingMore = false,
                        loadMoreError = null
                    )
                },
                onFailure = { failure ->
                    forumState.copy(
                        posts = LoadResult.Error(apiFailureMessage("论坛", failure)),
                        canLoadMore = false,
                        loadingMore = false,
                        loadMoreError = null
                    )
                }
            )
        }
    }

    fun loadMoreForum() {
        val current = forumState
        current.totalPages?.let { totalPages ->
            val nextPage = (current.page + 1).takeIf { it <= totalPages } ?: return
            goToForumPage(nextPage)
            return
        }
        val currentPosts = (current.posts as? LoadResult.Success)?.value ?: return
        if (current.loadingMore || !current.canLoadMore) return

        val requestSerial = forumRequestSerial
        val nextPage = current.page + 1
        forumState = current.copy(loadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            val result = runCatching {
                api.forumPostsPage(
                    page = nextPage,
                    limit = PAGE_SIZE,
                    type = current.selectedType,
                    search = current.searchQuery,
                    hideSpoilers = current.hideSpoilers
                )
            }
            if (!isFreshRequestSerial(requestSerial, forumRequestSerial)) return@launch
            if (forumState.selectedType != current.selectedType || forumState.searchQuery != current.searchQuery) return@launch
            forumState = result.fold(
                onSuccess = { resultPage ->
                    val nextPosts = resultPage.posts
                    forumState.copy(
                        posts = LoadResult.Success((currentPosts + nextPosts).distinctBy(ForumPost::id)),
                        reviewTotal = if (current.selectedType == "review") {
                            resultPage.total ?: current.reviewTotal
                        } else {
                            null
                        },
                        page = nextPage,
                        totalPages = resultPage.totalPages ?: current.totalPages,
                        canLoadMore = nextPosts.size == PAGE_SIZE,
                        loadingMore = false,
                        loadMoreError = null
                    )
                },
                onFailure = { failure ->
                    // A failed additional page never erases the already visible forum feed.
                    forumState.copy(
                        loadingMore = false,
                        loadMoreError = apiFailureMessage("论坛", failure)
                    )
                }
            )
        }
    }

    fun openForumPost(postId: Long) {
        if (postId <= 0) return
        val currentStack = routes.toList()
        val nextStack = pushDistinctRoute(currentStack, AppRoute.ForumPostDetail(postId))
        if (nextStack === currentStack) return
        routes.replaceWith(nextStack)
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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.ForumCreate))
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
                        routes.replaceWith(nextStack)
                        loadForumPostDetail(postId)
                    } else if (routes.lastOrNull() is AppRoute.ForumCreate) {
                        routes.removeAt(routes.lastIndex)
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

    fun loadForumPostDetail(postId: Long) {
        val previous = forumPostDetailState.takeIf { it.postId == postId }
        forumPostDetailState = ForumPostDetailState(
            postId = postId,
            detail = LoadResult.Loading,
            comments = LoadResult.Loading,
            commentDraft = previous?.commentDraft.orEmpty(),
            replyingToCommentId = previous?.replyingToCommentId,
            replyingToName = previous?.replyingToName,
            expandedCommentIds = previous?.expandedCommentIds.orEmpty()
        )
        viewModelScope.launch {
            val detail = async { runCatching { api.forumPostDetail(postId) } }
            val comments = async { runCatching { api.forumPostComments(postId = postId) } }
            val detailResult = detail.await()
            val commentsResult = comments.await()
            if (currentRoute != AppRoute.ForumPostDetail(postId)) return@launch
            forumPostDetailState = forumPostDetailState.copy(
                detail = detailResult.toLoadResult(VisibleUiLabels.ForumPostDetail),
                comments = commentsResult.toLoadResult(VisibleUiLabels.Comments)
            )
            loadForumBookReferences(
                postId = postId,
                contents = buildList {
                    detailResult.getOrNull()?.content?.let(::add)
                    commentsResult.getOrNull().orEmpty().map(ForumComment::content).forEach(::add)
                }
            )
        }
    }

    /**
     * Resolve each source book marker once per detail route. Compose receives immutable loading
     * states and therefore never starts network work during recomposition or while a comment row
     * is being flung.
     */
    private fun loadForumBookReferences(postId: Long, contents: List<String>) {
        val bookIds = forumBookReferenceIds(contents)
        if (currentRoute != AppRoute.ForumPostDetail(postId) || forumPostDetailState.postId != postId) return
        if (bookIds.isEmpty()) {
            forumPostDetailState = forumPostDetailState.copy(bookReferences = emptyMap())
            return
        }

        forumPostDetailState = forumPostDetailState.copy(
            bookReferences = bookIds.associateWith { LoadResult.Loading }
        )
        viewModelScope.launch {
            val requests = bookIds.associateWith { bookId ->
                async { runCatching { api.bookDetail(bookId) } }
            }
            val resolved = requests.mapValues { (_, request) ->
                request.await().toLoadResult("关联书籍")
            }
            if (currentRoute != AppRoute.ForumPostDetail(postId) || forumPostDetailState.postId != postId) {
                return@launch
            }
            forumPostDetailState = forumPostDetailState.copy(bookReferences = resolved)
        }
    }

    fun updateForumCommentDraft(value: String) {
        forumPostDetailState = forumPostDetailState.copy(commentDraft = value)
    }

    fun replyToForumComment(comment: ForumComment) {
        forumPostDetailState = forumPostDetailState.copy(
            replyingToCommentId = comment.id,
            replyingToName = comment.authorName,
            commentDraft = forumPostDetailState.commentDraft.ifBlank {
                comment.authorName?.let { "@$it " }.orEmpty()
            }
        )
    }

    fun cancelForumReply() {
        forumPostDetailState = forumPostDetailState.copy(replyingToCommentId = null, replyingToName = null)
    }

    fun toggleForumCommentReplies(commentId: Long) {
        if (commentId <= 0) return
        val expanded = forumPostDetailState.expandedCommentIds.toMutableSet()
        if (!expanded.add(commentId)) expanded.remove(commentId)
        forumPostDetailState = forumPostDetailState.copy(expandedCommentIds = expanded)
    }

    fun submitForumComment() {
        val postId = forumPostDetailState.postId
        val content = forumPostDetailState.commentDraft.trim()
        if (postId <= 0 || content.isBlank() || forumPostDetailState.actionLoading) return
        forumPostDetailState = forumPostDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                api.createForumComment(
                    postId = postId,
                    content = content,
                    parentCommentId = forumPostDetailState.replyingToCommentId,
                    replyToName = forumPostDetailState.replyingToName
                )
            }
            forumPostDetailState = result.fold(
                onSuccess = {
                    forumPostDetailState.copy(
                        commentDraft = "",
                        replyingToCommentId = null,
                        replyingToName = null,
                        actionLoading = false,
                        actionMessage = it.message ?: "评论已提交"
                    )
                },
                onFailure = {
                    forumPostDetailState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(VisibleUiLabels.CommentSubmit, it)
                    )
                }
            )
            loadForumPostDetail(postId)
        }
    }

    fun likeForumPost() {
        reactOnForumPost(forumPostActionLabel(ForumPostAction.Like)) { api.toggleForumPostLike(forumPostDetailState.postId) }
    }

    fun dislikeForumPost() {
        reactOnForumPost(forumPostActionLabel(ForumPostAction.Dislike)) { api.reactToForumPost(forumPostDetailState.postId, "down") }
    }

    fun emojiForumPost() {
        reactOnForumPost(forumPostActionLabel(ForumPostAction.Emoji)) { api.reactToForumPost(forumPostDetailState.postId, "emoji:heart") }
    }

    fun awardForumPost() {
        reactOnForumPost(forumPostActionLabel(ForumPostAction.Award)) { api.reactToForumPost(forumPostDetailState.postId, "award", awardPoints = 10) }
    }

    fun likeForumComment(commentId: Long) {
        reactOnForumComment(commentId, forumCommentActionLabel(ForumPostAction.Like)) { api.toggleForumCommentLike(commentId) }
    }

    fun dislikeForumComment(commentId: Long) {
        reactOnForumComment(commentId, forumCommentActionLabel(ForumPostAction.Dislike)) { api.reactToForumComment(commentId, "down") }
    }

    fun emojiForumComment(commentId: Long) {
        reactOnForumComment(commentId, forumCommentActionLabel(ForumPostAction.Emoji)) { api.reactToForumComment(commentId, "emoji:heart") }
    }

    fun awardForumComment(commentId: Long) {
        reactOnForumComment(commentId, forumCommentActionLabel(ForumPostAction.Award)) { api.reactToForumComment(commentId, "award", awardPoints = 10) }
    }

    private fun reactOnForumPost(label: String, action: suspend () -> com.novalpie.nativeapp.model.ForumActionResult) {
        if (forumPostDetailState.postId <= 0 || forumPostDetailState.actionLoading) return
        val postId = forumPostDetailState.postId
        forumPostDetailState = forumPostDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { action() }
            forumPostDetailState = result.fold(
                onSuccess = {
                    forumPostDetailState.copy(
                        actionLoading = false,
                        actionMessage = it.message ?: "$label 已同步"
                    )
                },
                onFailure = {
                    forumPostDetailState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, it)
                    )
                }
            )
            loadForumPostDetail(postId)
        }
    }

    private fun reactOnForumComment(commentId: Long, label: String, action: suspend () -> com.novalpie.nativeapp.model.ForumActionResult) {
        if (commentId <= 0 || forumPostDetailState.postId <= 0 || forumPostDetailState.actionLoading) return
        val postId = forumPostDetailState.postId
        forumPostDetailState = forumPostDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { action() }
            forumPostDetailState = result.fold(
                onSuccess = {
                    forumPostDetailState.copy(
                        actionLoading = false,
                        actionMessage = it.message ?: "$label 已同步"
                    )
                },
                onFailure = {
                    forumPostDetailState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, it)
                    )
                }
            )
            loadForumPostDetail(postId)
        }
    }

    fun updateBookCommentDraft(value: String) {
        bookDetailState = bookDetailState.copy(commentDraft = value)
    }

    fun replyToBookComment(comment: ChapterComment) {
        bookDetailState = bookDetailState.copy(
            replyingToCommentId = comment.id,
            replyingToName = comment.authorName,
            commentDraft = bookDetailState.commentDraft.ifBlank {
                comment.authorName?.let { "@$it " }.orEmpty()
            }
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
        bookDetailState = bookDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                if (replyId != null) {
                    api.createCommentReply(commentId = replyId, content = content, replyToName = replyToName)
                } else {
                    api.createBookComment(bookId = bookId, content = content)
                }
            }
            bookDetailState = result.fold(
                onSuccess = {
                    bookDetailState.copy(
                        commentDraft = "",
                        replyingToCommentId = null,
                        replyingToName = null,
                        actionLoading = false,
                        actionMessage = it.message ?: "评论已提交"
                    )
                },
                onFailure = {
                    bookDetailState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage("评论提交", it)
                    )
                }
            )
            loadBookDetail(bookId)
        }
    }

    fun likeBookComment(comment: ChapterComment) {
        reactOnBookComment(comment, "评论点赞") { api.toggleCommentLike(comment.id) }
    }

    fun dislikeBookComment(comment: ChapterComment) {
        reactOnBookComment(comment, "评论点踩") { reactToCommentOrReply(comment, "down") }
    }

    fun emojiBookComment(comment: ChapterComment) {
        reactOnBookComment(comment, "评论表情") { reactToCommentOrReply(comment, "emoji:heart") }
    }

    fun awardBookComment(comment: ChapterComment) {
        reactOnBookComment(comment, "评论打赏") { reactToCommentOrReply(comment, "award", awardPoints = 10) }
    }

    private fun reactOnBookComment(comment: ChapterComment, label: String, action: suspend () -> com.novalpie.nativeapp.model.ForumActionResult) {
        if (comment.id <= 0 || bookDetailState.bookId <= 0 || bookDetailState.actionLoading) return
        val bookId = bookDetailState.bookId
        bookDetailState = bookDetailState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { action() }
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
            loadBookDetail(bookId)
        }
    }

    fun updateReaderCommentDraft(chapterId: Long, value: String) {
        updateReaderChapterCommentState(chapterId) { it.copy(draft = value) }
    }

    fun replyToReaderComment(chapterId: Long, comment: ChapterComment) {
        updateReaderChapterCommentState(chapterId) { current ->
            current.copy(
                replyingToCommentId = comment.id,
                replyingToName = comment.authorName,
                draft = current.draft.ifBlank { comment.authorName?.let { "@$it " }.orEmpty() },
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
                result.fold(
                    onSuccess = {
                        current.copy(
                            draft = "",
                            replyingToCommentId = null,
                            replyingToName = null,
                            actionLoading = false,
                            actionMessage = it.message ?: "评论已提交",
                        )
                    },
                    onFailure = {
                        current.copy(
                            actionLoading = false,
                            actionMessage = apiFailureMessage("章节评论提交", it),
                        )
                    },
                )
            }
            if (result.isSuccess) refreshReaderChapterComments(chapterId)
        }
    }

    fun likeReaderComment(chapterId: Long, comment: ChapterComment) {
        reactOnReaderComment(chapterId, comment, "章节评论点赞") { api.toggleCommentLike(comment.id) }
    }

    fun dislikeReaderComment(chapterId: Long, comment: ChapterComment) {
        reactOnReaderComment(chapterId, comment, "章节评论点踩") { reactToCommentOrReply(comment, "down") }
    }

    fun emojiReaderComment(chapterId: Long, comment: ChapterComment) {
        reactOnReaderComment(chapterId, comment, "章节评论表情") { reactToCommentOrReply(comment, "emoji:heart") }
    }

    fun awardReaderComment(chapterId: Long, comment: ChapterComment) {
        reactOnReaderComment(chapterId, comment, "章节评论打赏") {
            reactToCommentOrReply(comment, "award", awardPoints = 10)
        }
    }

    fun refreshReaderChapterComments(chapterId: Long) {
        val state = readerState
        val route = currentRoute as? AppRoute.Reader ?: return
        val bookId = state.bookId
        if (chapterId <= 0 || bookId <= 0 || route.bookId != bookId) return
        val requestSerial = readerRequestSerial
        updateReaderChapterCommentState(chapterId) { it.copy(comments = LoadResult.Loading) }
        viewModelScope.launch {
            val result = runCatching {
                api.chapterComments(bookId = bookId, chapterId = chapterId, page = 1, limit = PAGE_SIZE)
            }
            if (!isFreshReaderCommentRequest(requestSerial, bookId)) return@launch
            val loadResult = result.toLoadResult(VisibleUiLabels.ChapterComments)
            updateReaderChapterCommentState(chapterId) { it.copy(comments = loadResult) }
            if (readerState.chapterId == chapterId) {
                readerState = readerState.copy(comments = loadResult)
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
            if (result.isSuccess) refreshReaderChapterComments(chapterId)
        }
    }

    private fun updateReaderChapterCommentState(
        chapterId: Long,
        transform: (ReaderChapterCommentState) -> ReaderChapterCommentState,
    ) {
        if (chapterId <= 0) return
        val state = readerState
        val current = readerChapterCommentState(state, chapterId)
        readerState = state.copy(
            chapterCommentStates = state.chapterCommentStates + (chapterId to transform(current)),
        )
    }

    private fun isFreshReaderCommentRequest(requestSerial: Long, bookId: Long): Boolean {
        val route = currentRoute as? AppRoute.Reader ?: return false
        return requestSerial == readerRequestSerial && route.bookId == bookId && readerState.bookId == bookId
    }

    fun updateReaderCommentDraft(value: String) {
        readerState = readerState.copy(commentDraft = value)
    }

    fun replyToReaderComment(comment: ChapterComment) {
        readerState = readerState.copy(
            replyingToCommentId = comment.id,
            replyingToName = comment.authorName,
            commentDraft = readerState.commentDraft.ifBlank {
                comment.authorName?.let { "@$it " }.orEmpty()
            }
        )
    }

    fun cancelReaderCommentReply() {
        readerState = readerState.copy(replyingToCommentId = null, replyingToName = null)
    }

    fun submitReaderComment() {
        val bookId = readerState.bookId
        val chapterId = readerState.chapterId
        val content = readerState.commentDraft.trim()
        if (bookId <= 0 || chapterId <= 0 || content.isBlank() || readerState.actionLoading) return
        val replyId = readerState.replyingToCommentId
        val replyToName = readerState.replyingToName
        readerState = readerState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                if (replyId != null) {
                    api.createCommentReply(commentId = replyId, content = content, replyToName = replyToName)
                } else {
                    api.createChapterComment(bookId = bookId, chapterId = chapterId, content = content)
                }
            }
            readerState = result.fold(
                onSuccess = {
                    readerState.copy(
                        commentDraft = "",
                        replyingToCommentId = null,
                        replyingToName = null,
                        actionLoading = false,
                        actionMessage = it.message ?: "评论已提交"
                    )
                },
                onFailure = {
                    readerState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage("章节评论提交", it)
                    )
                }
            )
            loadReader(bookId, chapterId)
        }
    }

    fun likeReaderComment(comment: ChapterComment) {
        reactOnReaderComment(comment, "章节评论点赞") { api.toggleCommentLike(comment.id) }
    }

    fun dislikeReaderComment(comment: ChapterComment) {
        reactOnReaderComment(comment, "章节评论点踩") { reactToCommentOrReply(comment, "down") }
    }

    fun emojiReaderComment(comment: ChapterComment) {
        reactOnReaderComment(comment, "章节评论表情") { reactToCommentOrReply(comment, "emoji:heart") }
    }

    fun awardReaderComment(comment: ChapterComment) {
        reactOnReaderComment(comment, "章节评论打赏") { reactToCommentOrReply(comment, "award", awardPoints = 10) }
    }

    private fun reactOnReaderComment(comment: ChapterComment, label: String, action: suspend () -> com.novalpie.nativeapp.model.ForumActionResult) {
        if (comment.id <= 0 || readerState.bookId <= 0 || readerState.chapterId <= 0 || readerState.actionLoading) return
        val bookId = readerState.bookId
        val chapterId = readerState.chapterId
        readerState = readerState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { action() }
            readerState = result.fold(
                onSuccess = {
                    readerState.copy(
                        actionLoading = false,
                        actionMessage = it.message ?: "$label 已同步"
                    )
                },
                onFailure = {
                    readerState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage(label, it)
                    )
                }
            )
            loadReader(bookId, chapterId)
        }
    }

    private suspend fun reactToCommentOrReply(
        comment: ChapterComment,
        reactionType: String,
        awardPoints: Int? = null
    ): com.novalpie.nativeapp.model.ForumActionResult {
        val parentId = comment.parentCommentId
        return if (parentId != null && parentId > 0) {
            api.reactToCommentReply(parentCommentId = parentId, replyId = comment.id, reactionType = reactionType, awardPoints = awardPoints)
        } else {
            api.reactToComment(commentId = comment.id, reactionType = reactionType, awardPoints = awardPoints)
        }
    }

    fun openBookEditInfo(bookId: Long) {
        if (bookId <= 0) return
        if (authToken.isNullOrBlank()) {
            openLoginFallback()
            return
        }
        if (!hasBookManagementAccess(bookId)) return
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.BookEditInfo(bookId)))
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
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.BookChapters(bookId)))
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
        uploadRequestSerial++
        uploadBookState = UploadBookState(existingNovelId = bookId)
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.BookAppend(bookId)))
    }

    fun openBook(bookId: Long) {
        if (bookId <= 0) return
        clearReaderSessionWhenLeaving()
        val currentStack = routes.toList()
        val nextStack = pushDistinctRoute(currentStack, AppRoute.BookDetail(bookId))
        if (nextStack === currentStack) return
        routes.replaceWith(nextStack)
        loadBookDetail(bookId)
    }

    /** Opens the source-compatible read-only glossary from a book detail route. */
    fun openTerminology(bookId: Long) {
        if (bookId <= 0) return
        val currentStack = routes.toList()
        val nextStack = pushDistinctRoute(currentStack, AppRoute.Terminology(bookId))
        if (nextStack === currentStack) return
        routes.replaceWith(nextStack)
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
        routes.replaceWith(nextStack)
        loadReader(bookId, chapterId, entryPosition = entryPosition)
    }

    fun continueReading(progress: ReaderProgress) {
        currentTab = BottomTab.Collection
        routes.clear()
        routes.add(AppRoute.Home)
        routes.add(AppRoute.BookDetail(progress.bookId))
        loadBookDetail(progress.bookId)
        routes.add(AppRoute.Reader(progress.bookId, progress.chapterId))
        loadReader(progress.bookId, progress.chapterId)
    }

    fun clearReaderProgress() {
        readerProgressStore.clear()
        readerSessionStore.clear()
        readerProgress = null
        recentReaderProgresses = emptyList()
    }

    fun openWebFallback(url: String) {
        clearReaderSessionWhenLeaving()
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.WebFallback(url)))
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
        routes.replaceWith(next)
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
            routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.AuthCaptcha))
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
                    authState = AuthState()
                    currentTab = BottomTab.Collection
                    routes.clear()
                    routes.add(AppRoute.Home)
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
        routes.clear()
        routes.add(
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
        routes.add(AppRoute.BookDetail(bookId))
        loadBookDetail(bookId)
        viewModelScope.launch {
            val permissions = runCatching { api.managedBookPermissions(bookId) }.getOrNull()
            if (!bookManagementActionsVisible(permissions)) return@launch
            if (currentRoute != AppRoute.BookDetail(bookId)) return@launch

            routes.add(route)
            when (route) {
                is AppRoute.BookEditInfo -> loadBookEditInfo(bookId)
                is AppRoute.BookChapters -> loadManagedChapters(bookId)
                is AppRoute.BookAppend -> {
                    uploadRequestSerial++
                    uploadBookState = UploadBookState(existingNovelId = bookId)
                }
                else -> Unit
            }
        }
    }

    fun goBack(): Boolean {
        if (routes.size <= 1) return false
        val leavingReader = currentRoute as? AppRoute.Reader
        routes.removeAt(routes.lastIndex)
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
        val next = GridScrollPosition.from(firstVisibleItemIndex, firstVisibleItemScrollOffset)
        if (forumScrollPosition != next) forumScrollPosition = next
    }

    fun saveSearchGridScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        val next = GridScrollPosition.from(firstVisibleItemIndex, firstVisibleItemScrollOffset)
        if (searchGridScrollPosition != next) searchGridScrollPosition = next
    }

    fun loadHome(actionMessage: String? = null) {
        val requestSerial = ++homeRequestSerial
        val tokenProfile = authToken?.let(::decodeAuthTokenProfile)
        val options = favoritesUiOptions
        val requestedPage = options.currentPage
        val requestedReaderProgressRevision = readerProgressRevision
        val retainCollectionWhileRefreshing =
            options.tab == FavoritesContentTab.Favorites &&
                collectionRefreshRequired(
                    readerProgressRevision = readerProgressRevision,
                    syncedProgressRevision = syncedCollectionProgressRevision,
                )
        val retainedFavoriteEntries = if (retainCollectionWhileRefreshing) {
            (homeState.favoriteEntries as? LoadResult.Success)?.value
                ?.let { favoriteEntriesWithLocalReaderProgress(it, recentReaderProgresses) }
        } else {
            null
        }
        homeState = HomeState(
            user = tokenProfile?.let { LoadResult.Success(it) } ?: LoadResult.Loading,
            groups = LoadResult.Loading,
            favorites = retainedFavoriteEntries?.let { LoadResult.Success(it.map(FavoriteEntry::book)) }
                ?: if (options.tab == FavoritesContentTab.Favorites) LoadResult.Loading else LoadResult.Idle,
            favoriteEntries = retainedFavoriteEntries?.let { LoadResult.Success(it) }
                ?: if (options.tab == FavoritesContentTab.Favorites) LoadResult.Loading else LoadResult.Idle,
            history = if (options.tab == FavoritesContentTab.History) LoadResult.Loading else LoadResult.Idle,
            favoritesPage = requestedPage,
            selectedFavoriteGroupId = selectedFavoriteGroupId,
            options = options,
            actionMessage = actionMessage
        )
        resolveMissingReaderProgressBookTitle()
        viewModelScope.launch {
            val favoriteGroupId = selectedFavoriteGroupId
            if (options.tab == FavoritesContentTab.Favorites) {
                // Start the visible shelf first. Profile and group chrome can resolve afterward.
                val favorites = async {
                    runCatching {
                        api.favoritePage(
                            page = requestedPage,
                            limit = PAGE_SIZE,
                            groupId = favoriteGroupId,
                            search = bookshelfQuery,
                            sortField = options.sortField,
                            sortOrder = options.sortOrder
                        )
                    }
                }
                val user = async { runCatching { api.currentUser() } }
                val groups = async { runCatching { api.favoriteGroups() } }
                val result = favorites.await()
                if (!isFreshRequestSerial(requestSerial, homeRequestSerial)) return@launch
                val page = result.getOrNull()
                if (page != null) {
                    syncedCollectionProgressRevision = maxOf(
                        syncedCollectionProgressRevision,
                        requestedReaderProgressRevision,
                    )
                }
                val entriesWithLocalProgress = page?.items?.let { entries ->
                    favoriteEntriesWithLocalReaderProgress(entries, recentReaderProgresses)
                }
                homeState = HomeState(
                    // The shelf is the primary surface. Do not make returned book cards wait for
                    // the independent profile/group requests on a slow source route.
                    user = tokenProfile?.let { LoadResult.Success(it) } ?: LoadResult.Loading,
                    groups = LoadResult.Loading,
                    favorites = entriesWithLocalProgress?.let { LoadResult.Success(it.map(FavoriteEntry::book)) }
                        ?: LoadResult.Error(
                            apiFailureMessage(
                                VisibleUiLabels.Bookshelf,
                                result.exceptionOrNull() ?: IOException("favorites request failed")
                            )
                        ),
                    favoriteEntries = entriesWithLocalProgress?.let { LoadResult.Success(it) }
                        ?: LoadResult.Error(
                            apiFailureMessage(
                                VisibleUiLabels.Bookshelf,
                                result.exceptionOrNull() ?: IOException("favorites request failed")
                            )
                        ),
                    favoritesPage = page?.page ?: requestedPage,
                    favoritesCanLoadMore = page?.canLoadMore() ?: false,
                    selectedFavoriteGroupId = favoriteGroupId,
                    options = options,
                    actionMessage = actionMessage
                )
                val resolvedUser = user.await()
                val resolvedGroups = groups.await()
                if (!isFreshRequestSerial(requestSerial, homeRequestSerial)) return@launch
                homeState = homeState.copy(
                    user = resolveUserLoadResult(resolvedUser, tokenProfile),
                    groups = resolvedGroups.toLoadResult(VisibleUiLabels.FavoriteGroups)
                )
            } else {
                val history = async { runCatching { api.readingHistoryPage(page = requestedPage, limit = PAGE_SIZE) } }
                val user = async { runCatching { api.currentUser() } }
                val groups = async { runCatching { api.favoriteGroups() } }
                val result = history.await()
                if (!isFreshRequestSerial(requestSerial, homeRequestSerial)) return@launch
                val page = result.getOrNull()
                homeState = HomeState(
                    user = tokenProfile?.let { LoadResult.Success(it) } ?: LoadResult.Loading,
                    groups = LoadResult.Loading,
                    favorites = LoadResult.Success(emptyList()),
                    history = page?.let { LoadResult.Success(it.items) }
                        ?: LoadResult.Error(
                            apiFailureMessage(
                                "阅读历史",
                                result.exceptionOrNull() ?: IOException("history request failed")
                            )
                        ),
                    favoritesPage = page?.page ?: requestedPage,
                    favoritesCanLoadMore = page?.canLoadMore() ?: false,
                    selectedFavoriteGroupId = favoriteGroupId,
                    options = options,
                    actionMessage = actionMessage
                )
                val resolvedUser = user.await()
                val resolvedGroups = groups.await()
                if (!isFreshRequestSerial(requestSerial, homeRequestSerial)) return@launch
                homeState = homeState.copy(
                    user = resolveUserLoadResult(resolvedUser, tokenProfile),
                    groups = resolvedGroups.toLoadResult(VisibleUiLabels.FavoriteGroups)
                )
            }
        }
    }

    fun loadMoreFavorites() {
        if (homeState.favoritesLoadingMore || !homeState.favoritesCanLoadMore) return

        val nextPage = homeState.favoritesPage + 1
        val requestSerial = homeRequestSerial
        val favoriteGroupId = homeState.selectedFavoriteGroupId
        val options = homeState.options
        homeState = homeState.copy(favoritesLoadingMore = true, favoritesLoadMoreError = null)
        viewModelScope.launch {
            val result = if (options.tab == FavoritesContentTab.Favorites) {
                runCatching {
                    api.favoritePage(
                        page = nextPage,
                        limit = PAGE_SIZE,
                        groupId = favoriteGroupId,
                        search = bookshelfQuery,
                        sortField = options.sortField,
                        sortOrder = options.sortOrder
                    )
                }
            } else {
                runCatching { api.readingHistoryPage(page = nextPage, limit = PAGE_SIZE) }
            }
            if (!isFreshRequestSerial(requestSerial, homeRequestSerial)) return@launch
            homeState = result.fold(
                onSuccess = { nextPageResult ->
                    if (options.tab == FavoritesContentTab.Favorites) {
                        val currentEntries = (homeState.favoriteEntries as? LoadResult.Success)?.value.orEmpty()
                        val merged = favoriteEntriesWithLocalReaderProgress(
                            entries = mergeFavoriteEntriesByBookId(currentEntries, nextPageResult.items),
                            localProgresses = recentReaderProgresses,
                        )
                        favoritesUiOptions = favoritesUiOptions.copy(currentPage = nextPageResult.page.coerceAtLeast(1))
                        saveFavoritesOptions()
                        homeState.copy(
                            favorites = LoadResult.Success(merged.map(FavoriteEntry::book)),
                            favoriteEntries = LoadResult.Success(merged),
                            favoritesPage = nextPageResult.page,
                            options = favoritesUiOptions,
                            favoritesCanLoadMore = nextPageResult.canLoadMore(),
                            favoritesLoadingMore = false,
                            favoritesLoadMoreError = null
                        )
                    } else {
                        val currentEntries = (homeState.history as? LoadResult.Success)?.value.orEmpty()
                        val merged = mergeFavoriteEntriesByBookId(currentEntries, nextPageResult.items)
                        favoritesUiOptions = favoritesUiOptions.copy(currentPage = nextPageResult.page.coerceAtLeast(1))
                        saveFavoritesOptions()
                        homeState.copy(
                            history = LoadResult.Success(merged),
                            favoritesPage = nextPageResult.page,
                            options = favoritesUiOptions,
                            favoritesCanLoadMore = nextPageResult.canLoadMore(),
                            favoritesLoadingMore = false,
                            favoritesLoadMoreError = null
                        )
                    }
                },
                onFailure = {
                    // Keep the pages already loaded; report only that this page failed. The page
                    // counter is left alone so retrying asks for the same page again.
                    homeState.copy(
                        favoritesLoadingMore = false,
                        favoritesLoadMoreError = apiFailureMessage(VisibleUiLabels.Bookshelf, it)
                    )
                }
            )
        }
    }

    fun performSearch(submittedKeyword: String? = null) {
        val keyword = searchKeywordForSubmission(searchKeyword, submittedKeyword)
        if (searchKeyword != keyword) searchKeyword = keyword
        val requestSerial = ++searchRequestSerial
        resetSearchGridScrollPosition()
        searchResults = LoadResult.Loading
        searchPage = 1
        searchResultPage = null
        searchCanLoadMore = false
        searchLoadingMore = false
        searchLoadMoreError = null
        val options = searchOptions
        val resolved = resolveSearchRequest(keyword, options)
        val request = SearchRequestSnapshot(
            serial = requestSerial,
            keyword = keyword,
            options = options,
            page = 1
        )
        if (keyword.isNotBlank()) {
            searchHistoryStore.saveKeyword(keyword)
            searchHistory = searchHistoryStore.load()
        }
        if (resolved.errors.isNotEmpty()) {
            searchResults = LoadResult.Error("高级语法：${resolved.errors.joinToString("；")}")
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                requestSearchPage(resolved = resolved, page = 1)
            }
            if (
                !isFreshSearchResult(
                    request = request,
                    activeSerial = searchRequestSerial,
                    currentKeyword = searchKeyword,
                    currentOptions = searchOptions,
                    expectedPage = 1
                )
            ) return@launch
            result.fold(
                onSuccess = { response ->
                    searchResults = LoadResult.Success(response.items)
                    searchPage = response.page
                    searchResultPage = response
                    searchCanLoadMore = response.canLoadMore()
                },
                onFailure = {
                    searchResults = LoadResult.Error(apiFailureMessage(VisibleUiLabels.Search, it))
                }
            )
        }
    }

    fun loadMoreSearch() {
        goToSearchPage(searchPage + 1)
    }

    /** Source parity: result pages replace the current grid instead of appending an endless list. */
    fun goToSearchPage(requestedPage: Int) {
        val currentPage = searchResultPage ?: return
        if (searchLoadingMore) return
        val totalPages = currentPage.totalPages
            ?: currentPage.total?.let { count ->
                ((count.toLong() + currentPage.pageSize - 1L) / currentPage.pageSize).toInt()
            }
        val targetPage = totalPages?.let { requestedPage.coerceIn(1, it.coerceAtLeast(1)) }
            ?: requestedPage.coerceAtLeast(1)
        if (targetPage == searchPage && searchResults is LoadResult.Success) return

        resetSearchGridScrollPosition()

        val keyword = searchKeyword
        val options = searchOptions
        val resolved = resolveSearchRequest(keyword, options)
        if (resolved.errors.isNotEmpty()) {
            searchLoadMoreError = "高级语法：${resolved.errors.joinToString("；")}"
            return
        }
        val requestSerial = ++searchRequestSerial
        val request = SearchRequestSnapshot(
            serial = requestSerial,
            keyword = keyword,
            options = options,
            page = targetPage
        )
        searchPage = targetPage
        // Keep the attempted page in metadata so an error-state retry targets the same source page.
        searchResultPage = currentPage.copy(page = targetPage)
        searchResults = LoadResult.Loading
        searchCanLoadMore = false
        searchLoadingMore = true
        searchLoadMoreError = null
        viewModelScope.launch {
            val result = runCatching {
                requestSearchPage(resolved = resolved, page = targetPage)
            }
            if (
                !isFreshSearchResult(
                    request = request,
                    activeSerial = searchRequestSerial,
                    currentKeyword = searchKeyword,
                    currentOptions = searchOptions,
                    expectedPage = targetPage
                )
            ) return@launch
            result.fold(
                onSuccess = { response ->
                    searchResults = LoadResult.Success(response.items)
                    searchPage = response.page
                    searchResultPage = response
                    searchCanLoadMore = response.canLoadMore()
                    searchLoadMoreError = null
                },
                onFailure = {
                    searchResults = LoadResult.Error(apiFailureMessage(VisibleUiLabels.Search, it))
                }
            )
            searchLoadingMore = false
        }
    }

    private suspend fun requestSearchPage(
        resolved: ResolvedSearchRequest,
        page: Int
    ): SearchPage = api.searchPage(
        keyword = resolved.keyword,
        page = page,
        limit = SEARCH_PAGE_SIZE,
        sortBy = resolved.sortBy,
        sortOrder = resolved.sortOrder,
        scope = resolved.scope,
        matchType = resolved.matchType,
        adultFilter = resolved.adultFilter,
        source = resolved.source,
        minWordCount = resolved.minWordCount,
        maxWordCount = resolved.maxWordCount,
        requiredTags = resolved.requiredTags,
        blockedTags = resolved.blockedTags,
        tagsAny = resolved.tagsAny,
        tagsExpression = resolved.tagsExpression,
        blockedTerms = resolved.blockedTerms,
        platform = resolved.platform,
        novelType = resolved.type,
        status = resolved.status
    )

    fun loadSearchTags() {
        if (searchTags is LoadResult.Loading) return
        searchTags = LoadResult.Loading
        viewModelScope.launch {
            val result = runCatching { api.tags(sort = "count", limit = SEARCH_TAG_SUGGESTION_LIMIT) }
            searchTags = result.toLoadResult("标签")
        }
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

    fun loadBookDetail(bookId: Long) {
        val requestSerial = ++bookDetailRequestSerial
        val shouldCheckManagementPermissions = !authToken.isNullOrBlank()
        bookDetailState = BookDetailState(
            bookId = bookId,
            book = LoadResult.Loading,
            chapters = LoadResult.Loading,
            comments = LoadResult.Loading,
            favoriteStatus = LoadResult.Loading,
            managementPermissions = if (shouldCheckManagementPermissions) {
                LoadResult.Loading
            } else {
                LoadResult.Success(BookEditPermissions())
            },
            readerProgress = readerProgressStore.load(bookId)
        )
        viewModelScope.launch {
            val book = async { runCatching { api.bookDetail(bookId) } }
            val originalCover = async { runCatching { api.bookCoverPhoto(bookId) } }
            val chapters = async { runCatching { api.chapters(bookId) } }
            // The live mobile book page requests thirty top-level reviews, including nested
            // replies, before rendering the review section.
            val comments = async {
                runCatching { api.bookComments(bookId = bookId, page = 1, limit = BOOK_COMMENT_PAGE_SIZE) }
            }
            val favoriteStatus = async { runCatching { api.favoriteStatus(bookId) } }
            val managementPermissions = if (shouldCheckManagementPermissions) {
                async { runCatching { api.managedBookPermissions(bookId) } }
            } else {
                null
            }
            fun isFresh(): Boolean =
                isFreshRequestSerial(requestSerial, bookDetailRequestSerial) &&
                    isFreshBookDetailResult(currentRoute, bookDetailState, bookId)

            // Book, catalogue, reviews, favourite state, permissions, and the full-resolution
            // cover are independent source requests. Publish each result as it arrives so a slow
            // review/permission endpoint cannot make the detail page or its book reviews look
            // empty. Every child keeps the same request-serial guard for rapid A -> B navigation.
            launch {
                val result = chapters.await()
                if (isFresh()) {
                    bookDetailState = bookDetailState.copy(
                        chapters = result.toLoadResult(VisibleUiLabels.ChapterCatalog),
                    )
                }
            }
            launch {
                val result = comments.await()
                if (isFresh()) {
                    bookDetailState = bookDetailState.copy(
                        comments = result.toLoadResult("评论区"),
                    )
                }
            }
            launch {
                val result = favoriteStatus.await()
                if (isFresh()) {
                    bookDetailState = bookDetailState.copy(
                        favoriteStatus = result.toLoadResult("收藏状态"),
                    )
                }
            }
            managementPermissions?.let { permissions ->
                launch {
                    val result = permissions.await()
                    if (isFresh()) {
                        bookDetailState = bookDetailState.copy(
                            managementPermissions = result.toLoadResult("load book management permissions"),
                        )
                    }
                }
            }
            launch {
                val coverUrl = originalCover.await().getOrNull().orEmpty()
                val sourceBook = book.await().getOrNull() ?: return@launch
                if (coverUrl.isNotBlank() && isFresh()) {
                    val currentBook = (bookDetailState.book as? LoadResult.Success)?.value ?: sourceBook
                    bookDetailState = bookDetailState.copy(
                        book = LoadResult.Success(currentBook.copy(fullCoverUrl = coverUrl)),
                    )
                }
            }

            val bookResult = book.await()
            if (!isFresh()) return@launch
            val fullCoverUrl = (bookDetailState.book as? LoadResult.Success)
                ?.value
                ?.fullCoverUrl
                ?.takeIf(String::isNotBlank)
            val resolvedBook = bookResult.toLoadResult(VisibleUiLabels.BookDetail).let { result ->
                if (result is LoadResult.Success && !fullCoverUrl.isNullOrBlank()) {
                    LoadResult.Success(result.value.copy(fullCoverUrl = fullCoverUrl))
                } else {
                    result
                }
            }
            bookDetailState = bookDetailState.copy(
                book = resolvedBook,
                readerProgress = readerProgressStore.load(bookId),
            )
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

    private data class NativeDownloadDestination(
        val displayName: String,
        val mimeType: String,
        val uri: Uri? = null,
        val file: File? = null,
    )

    /** Downloads the source-authorized EPUB into Android Downloads without opening a WebView. */
    fun downloadBookEpub(bookId: Long) {
        val book = (bookDetailState.book as? LoadResult.Success)?.value
            ?.takeIf { it.id == bookId }
        if (bookId <= 0 || nativeEpubDownloadState.busy) return
        if (authToken.isNullOrBlank()) {
            nativeEpubDownloadState = NativeEpubDownloadState(
                bookId = bookId,
                format = NativeBookDownloadFormat.Epub,
                message = "请先登录后再下载 EPUB",
            )
            return
        }
        val app = getApplication<Application>()
        cleanupNativeEpubTempFiles(app.cacheDir)
        nativeEpubDownloadState = NativeEpubDownloadState(
            bookId = bookId,
            format = NativeBookDownloadFormat.Epub,
            busy = true,
            message = "正在申请下载授权…",
        )
        viewModelScope.launch {
            var destination: NativeDownloadDestination? = null
            var epubWorkDirectory: File? = null
            var generatedEpubFile: File? = null
            // NativeEpubArchiveWriter stages several image assets at once. The producer callback
            // therefore records temporary files from multiple IO workers.
            val temporaryAssets = ConcurrentLinkedQueue<File>()
            try {
                val ticket = api.requestEpubDownload(bookId)
                nativeEpubDownloadState = nativeEpubDownloadState.copy(
                    message = "正在下载正文并整理 EPUB…",
                )
                withContext(Dispatchers.IO) {
                    val workDirectory = createNativeEpubWorkDirectory(app, bookId)
                    epubWorkDirectory = workDirectory
                    val generated = nativeEpubGenerationFile(workDirectory, bookId)
                    generatedEpubFile = generated
                    generated.outputStream().use { output ->
                        api.streamDownloadFile(ticket.fileName) { input ->
                            InputStreamReader(input, Charsets.UTF_8).use { source ->
                                NativeEpubArchiveWriter.write(
                                    output = output,
                                    metadata = NativeEpubMetadata(
                                        title = book?.title ?: "NovalPie book $bookId",
                                        author = book?.author ?: "未知作者",
                                        description = book?.description.orEmpty(),
                                        coverUrl = nativeEpubCoverUrl(book),
                                    ),
                                    source = source,
                                    stagingDirectory = epubWorkDirectory,
                                    openAsset = { url ->
                                        val temporary = File.createTempFile(
                                            "novalpie-asset-",
                                            ".bin",
                                            epubWorkDirectory,
                                        )
                                        try {
                                            var mediaType: String? = null
                                            api.streamAsset(url) { assetInput, contentType ->
                                                mediaType = contentType
                                                temporary.outputStream().buffered().use { fileOutput ->
                                                    assetInput.copyTo(fileOutput)
                                                }
                                            }
                                            temporaryAssets.add(temporary)
                                            NativeEpubAsset(
                                                mediaType = mediaType,
                                                input = temporary.inputStream(),
                                                onConsumed = {
                                                    temporary.delete()
                                                    temporaryAssets.remove(temporary)
                                                },
                                            )
                                        } catch (failure: Throwable) {
                                            temporary.delete()
                                            throw failure
                                        }
                                    },
                                    onProgress = { progress ->
                                        nativeEpubDownloadState = nativeEpubDownloadState.copy(
                                            progress = progress,
                                            message = nativeEpubProgressMessage(progress),
                                        )
                                    },
                                )
                            }
                        }
                    }
                    if (!generated.isFile || generated.length() <= 0L) {
                        throw IOException("EPUB 临时文件为空")
                    }
                    nativeEpubDownloadState = nativeEpubDownloadState.copy(
                        message = "正在写入下载目录…",
                    )
                    destination = createNativeEpubDestination(book?.title ?: "novalpie", bookId)
                    val target = destination ?: throw IOException("无法创建下载目标")
                    publishNativeEpubFile(generated, target)
                    commitNativeDownloadDestination(target)
                }
                nativeEpubDownloadState = nativeEpubDownloadState.copy(
                    busy = false,
                    message = "EPUB 已保存到下载目录：${destination?.displayName.orEmpty()}",
                )
            } catch (cancelled: CancellationException) {
                destination?.let(::discardNativeDownloadDestination)
                throw cancelled
            } catch (failure: Throwable) {
                destination?.let(::discardNativeDownloadDestination)
                nativeEpubDownloadState = nativeEpubDownloadState.copy(
                    busy = false,
                    message = apiFailureMessage("下载 EPUB", failure),
                )
            } finally {
                temporaryAssets.forEach { it.delete() }
                generatedEpubFile?.delete()
                epubWorkDirectory?.deleteRecursively()
            }
        }
    }

    /** Downloads the source-authorized TXT into Android Downloads without opening a WebView. */
    fun downloadBookTxt(bookId: Long) {
        val book = (bookDetailState.book as? LoadResult.Success)?.value
            ?.takeIf { it.id == bookId }
        if (bookId <= 0 || nativeEpubDownloadState.busy) return
        if (authToken.isNullOrBlank()) {
            nativeEpubDownloadState = NativeEpubDownloadState(
                bookId = bookId,
                format = NativeBookDownloadFormat.Txt,
                message = "请先登录后再下载 TXT",
            )
            return
        }
        nativeEpubDownloadState = NativeEpubDownloadState(
            bookId = bookId,
            format = NativeBookDownloadFormat.Txt,
            busy = true,
            message = "正在申请下载授权…",
        )
        viewModelScope.launch {
            var destination: NativeDownloadDestination? = null
            try {
                val ticket = api.requestTxtDownload(bookId)
                nativeEpubDownloadState = nativeEpubDownloadState.copy(
                    message = "正在原生保存 TXT…",
                )
                withContext(Dispatchers.IO) {
                    destination = createNativeTxtDestination(book?.title ?: "novalpie", bookId)
                    val target = destination ?: throw IOException("无法创建下载目标")
                    openNativeDownloadOutput(target).use { output ->
                        api.streamDownloadFile(ticket.fileName) { input ->
                            input.copyTo(output)
                        }
                    }
                    commitNativeDownloadDestination(target)
                }
                nativeEpubDownloadState = nativeEpubDownloadState.copy(
                    busy = false,
                    message = "TXT 已保存到下载目录：${destination?.displayName.orEmpty()}",
                )
            } catch (cancelled: CancellationException) {
                destination?.let(::discardNativeDownloadDestination)
                throw cancelled
            } catch (failure: Throwable) {
                destination?.let(::discardNativeDownloadDestination)
                nativeEpubDownloadState = nativeEpubDownloadState.copy(
                    busy = false,
                    message = apiFailureMessage("下载 TXT", failure),
                )
            }
        }
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

    private fun nativeEpubProgressMessage(progress: NativeEpubExportProgress): String {
        val chapterText = if (progress.totalChapters > 0) {
            "章节 ${progress.completedChapters}/${progress.totalChapters}"
        } else {
            "已处理 ${progress.completedChapters} 章"
        }
        val imageText = if (progress.totalImages > 0) {
            "，插图 ${progress.completedImages}/${progress.totalImages}"
        } else {
            ""
        }
        return "正在生成 EPUB：$chapterText$imageText"
    }

    /**
     * EPUB staging is deliberately kept out of cacheDir. Android may evict cache files while a
     * multi-minute, multi-gigabyte export is still reading them; a private per-job directory gives
     * the writer a stable filesystem boundary and is removed in the coroutine's finally block.
     */
    private fun createNativeEpubWorkDirectory(app: Application, bookId: Long): File {
        val root = File(app.filesDir, "novalpie-epub-work")
        if (!root.isDirectory && !root.mkdirs()) {
            throw IOException("无法创建 EPUB 临时目录")
        }
        val directory = File(root, "$bookId-${System.currentTimeMillis()}-${System.nanoTime()}")
        if (!directory.mkdirs()) {
            throw IOException("无法创建 EPUB 工作目录")
        }
        return directory
    }

    private fun createNativeEpubDestination(title: String, bookId: Long): NativeDownloadDestination =
        createNativeDownloadDestination(
            title = title,
            bookId = bookId,
            extension = "epub",
            mimeType = "application/epub+zip",
        )

    private fun createNativeTxtDestination(title: String, bookId: Long): NativeDownloadDestination =
        createNativeDownloadDestination(
            title = title,
            bookId = bookId,
            extension = "txt",
            mimeType = "text/plain",
        )

    private fun createNativeDownloadDestination(
        title: String,
        bookId: Long,
        extension: String,
        mimeType: String,
    ): NativeDownloadDestination {
        val safeTitle = title
            .replace(Regex("[^A-Za-z0-9\\p{L}\\p{N}._-]"), "_")
            .trim('_')
            .take(72)
            .ifBlank { "novalpie" }
        val displayName = "${safeTitle}_${bookId}_${System.currentTimeMillis()}.$extension"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = getApplication<Application>().contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values,
            ) ?: throw IOException("无法创建下载文件")
            return NativeDownloadDestination(
                displayName = displayName,
                mimeType = mimeType,
                uri = uri,
            )
        }
        val directory = getApplication<Application>()
            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: getApplication<Application>().cacheDir
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建下载目录")
        }
        return NativeDownloadDestination(
            displayName = displayName,
            mimeType = mimeType,
            file = File(directory, displayName),
        )
    }

    private fun openNativeDownloadOutput(destination: NativeDownloadDestination): OutputStream =
        destination.uri?.let { uri ->
            getApplication<Application>().contentResolver.openOutputStream(uri, "w")
                ?: throw IOException("无法打开下载文件")
        } ?: destination.file?.outputStream()
        ?: throw IOException("下载目标无效")

    /**
     * Publish only after NativeEpubArchiveWriter has closed and validated the complete ZIP. A
     * MediaStore/FUSE stream can stop at the 4 GiB boundary on large books; keeping ZIP assembly
     * on a private regular file avoids exposing that boundary to ZipOutputStream.
     */
    private fun publishNativeEpubFile(
        source: File,
        destination: NativeDownloadDestination,
    ) {
        val expected = source.length()
        if (expected <= 0L) throw IOException("EPUB 临时文件为空")
        destination.uri?.let { uri ->
            val resolver = getApplication<Application>().contentResolver
            resolver.openOutputStream(uri, "w")?.use { output ->
                copyNativeDownloadFile(source, output)
            } ?: throw IOException("无法打开下载文件")
            val publishedSize = resolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) null else {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index)
                }
            }
            if (publishedSize != null && publishedSize >= 0L && publishedSize != expected) {
                throw IOException("下载文件复制不完整：预期 $expected 字节，实际 $publishedSize 字节")
            }
            return
        }

        val target = destination.file ?: throw IOException("下载目标无效")
        val temporary = File(target.parentFile, ".${target.name}.part")
        try {
            temporary.delete()
            temporary.outputStream().use { output ->
                copyNativeDownloadFile(source, output)
            }
            if (temporary.length() != expected) {
                throw IOException("下载文件复制不完整：预期 $expected 字节，实际 ${temporary.length()} 字节")
            }
            if (target.exists() && !target.delete()) {
                throw IOException("无法替换下载文件")
            }
            if (!temporary.renameTo(target)) {
                throw IOException("无法发布下载文件")
            }
        } finally {
            temporary.delete()
        }
    }

    private fun commitNativeDownloadDestination(destination: NativeDownloadDestination) {
        destination.uri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getApplication<Application>().contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null,
                )
            }
            return
        }
        destination.file?.let { file ->
            MediaScannerConnection.scanFile(
                getApplication<Application>(),
                arrayOf(file.absolutePath),
                arrayOf(destination.mimeType),
                null,
            )
        }
    }

    private fun discardNativeDownloadDestination(destination: NativeDownloadDestination) {
        destination.uri?.let { uri ->
            runCatching { getApplication<Application>().contentResolver.delete(uri, null, null) }
        }
        destination.file?.let { file -> runCatching { file.delete() } }
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
    ) {
        val requestSerial = ++readerRequestSerial
        val catalogRequestSerial = ++readerCatalogRequestSerial
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
                previousState.chapterContents.filterNot { it.chapterId == chapterId } + loaded
            } else {
                listOf(loaded)
            }
        }
        val retainedBookTitle = previousState.bookTitle
            ?.trim()
            ?.takeIf { keepWindow && previousState.bookId == bookId }
        val initialBookTitle = readerBookTitle(bookId) ?: retainedBookTitle
        readerState = ReaderState(
            bookId = bookId,
            bookTitle = initialBookTitle,
            chapterId = chapterId,
            entryPosition = entryPosition,
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
                                )
                            ),
                        )
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
                saveReaderProgress(
                    bookId = bookId,
                    chapterId = chapterId,
                    chapterTitle = chapterTitle ?: contentValue?.title,
                    chapterNumber = chapterNumber,
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
    ) {
        val existing = readerProgressStore.load(bookId)
        readerProgressStore.save(
            bookId = bookId,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            bookTitle = readerProgressBookTitle(bookId) ?: existing?.bookTitle,
            chapterNumber = chapterNumber,
        )
        readerProgress = readerProgressStore.load()
        recentReaderProgresses = readerProgressStore.loadRecent(limit = READER_PROGRESS_HISTORY_LIMIT)
        updateLoadedCollectionProgress()
        readerProgressRevision += 1
        if (bookDetailState.bookId == bookId) {
            bookDetailState = bookDetailState.copy(readerProgress = readerProgressStore.load(bookId))
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
        val chapterNumber = (readerState.chapters as? LoadResult.Success)?.value?.let { chapters ->
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
        saveReaderProgress(route.bookId, chapterId, chapterTitle, chapterNumber)
    }

    private fun clearReaderSessionWhenLeaving() {
        if (currentRoute is AppRoute.Reader) readerSessionStore.clear()
    }

    private fun saveSearchOptions() {
        searchSettingsStore.save(searchOptions.toPersistedSearchSettings())
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
        homeState = homeState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            runCatching { mutation() }.fold(
                onSuccess = {
                    homeState = homeState.copy(
                        actionLoading = false,
                        selectionMode = false,
                        selectedBookIds = emptySet()
                    )
                    loadHome(successMessage)
                },
                onFailure = { failure ->
                    homeState = homeState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage("收藏操作", failure)
                    )
                }
            )
        }
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

    private fun invalidateSearchRequests() {
        searchRequestSerial += 1
        searchCanLoadMore = false
        searchLoadingMore = false
    }

    private fun refreshSearchAfterFilterChange(changed: Boolean) {
        if (searchFilterChangeShouldRefresh(currentRoute == AppRoute.Search, changed)) {
            performSearch()
        }
    }

    private fun resetHomeGridScrollPosition() {
        if (homeGridScrollPosition != GridScrollPosition()) homeGridScrollPosition = GridScrollPosition()
    }

    private fun resetForumScrollPosition() {
        if (forumScrollPosition != GridScrollPosition()) forumScrollPosition = GridScrollPosition()
    }

    private fun resetSearchGridScrollPosition() {
        if (searchGridScrollPosition != GridScrollPosition()) searchGridScrollPosition = GridScrollPosition()
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

    companion object {
        private const val PAGE_SIZE = 20
        private const val READER_PROGRESS_HISTORY_LIMIT = 20
        private const val BOOK_COMMENT_PAGE_SIZE = 30
        // The mobile source renders sixty search cards per explicit page.
        private const val SEARCH_PAGE_SIZE = 60
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
