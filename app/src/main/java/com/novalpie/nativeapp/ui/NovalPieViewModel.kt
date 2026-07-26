package com.novalpie.nativeapp.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.data.AuthSessionStore
import com.novalpie.nativeapp.data.EpubParser
import com.novalpie.nativeapp.data.EpubWriter
import com.novalpie.nativeapp.data.EditorArchiveStore
import com.novalpie.nativeapp.data.EditorProcessor
import com.novalpie.nativeapp.data.NetworkConfigStore
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.PersistedSearchSettings
import com.novalpie.nativeapp.data.ProxySettings
import com.novalpie.nativeapp.data.ReaderProgressStore
import com.novalpie.nativeapp.data.ReaderSettingsStore
import com.novalpie.nativeapp.data.SearchHistoryStore
import com.novalpie.nativeapp.data.SearchSettingsStore
import com.novalpie.nativeapp.data.WorkspaceLocalStore
import com.novalpie.nativeapp.data.UploadFileSource
import com.novalpie.nativeapp.data.configureNovalPieImageLoader
import com.novalpie.nativeapp.data.decodeAuthTokenProfile
import com.novalpie.nativeapp.data.isEmulatorRuntime
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.ChapterIllustrationPage
import com.novalpie.nativeapp.model.BookEditInfo
import com.novalpie.nativeapp.model.BookEditPermissions
import com.novalpie.nativeapp.model.BookEditRequest
import com.novalpie.nativeapp.model.DirectMessage
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import com.novalpie.nativeapp.model.FavoriteGroup
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
import com.novalpie.nativeapp.model.PoliticalExamAnswers
import com.novalpie.nativeapp.model.PoliticalExamResult
import com.novalpie.nativeapp.model.PoliticalExamSession
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.SiteMessage
import com.novalpie.nativeapp.model.UploadActionResult
import com.novalpie.nativeapp.model.UploadBookRequest
import com.novalpie.nativeapp.model.UploadChapter
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserCheckinSettings
import com.novalpie.nativeapp.model.UserCheckinRecord
import com.novalpie.nativeapp.model.WorkspaceApiConfig
import com.novalpie.nativeapp.model.WorkspaceCookieConfigs
import com.novalpie.nativeapp.model.WorkspaceCookieStatus
import com.novalpie.nativeapp.model.WorkspaceHealth
import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import com.novalpie.nativeapp.model.WorkspaceTranslationJob
import com.novalpie.nativeapp.model.AdminBaseUrlRule
import com.novalpie.nativeapp.model.AdminCookieConfig
import com.novalpie.nativeapp.model.AdminKeyItem
import com.novalpie.nativeapp.model.AdminOperationLogPage
import com.novalpie.nativeapp.model.AdminOverviewStats
import com.novalpie.nativeapp.model.AdminReviewRequest
import com.novalpie.nativeapp.model.AdminReviewSettings
import com.novalpie.nativeapp.model.AdminSchedulerLogs
import com.novalpie.nativeapp.model.AdminShopItem
import com.novalpie.nativeapp.model.UserCheckinAction
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Calendar

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
    data class MessageDetail(val messageId: Long) : AppRoute()
    data class MessageConversation(val targetUserId: Long, val targetName: String?) : AppRoute()
    data class ForumPostDetail(val postId: Long) : AppRoute()
    object ForumCreate : AppRoute()
    data class BookDetail(val bookId: Long) : AppRoute()
    data class BookEditInfo(val bookId: Long) : AppRoute()
    data class BookChapters(val bookId: Long) : AppRoute()
    data class BookAppend(val bookId: Long) : AppRoute()
    data class Reader(val bookId: Long, val chapterId: Long) : AppRoute()
    data class UserProfileDetail(val userId: Long) : AppRoute()
    data class Admin(val section: AdminSection) : AppRoute()
    data class WebFallback(val url: String) : AppRoute()
}

data class HomeState(
    val user: LoadResult<UserProfile> = LoadResult.Idle,
    val groups: LoadResult<List<FavoriteGroup>> = LoadResult.Idle,
    val favorites: LoadResult<List<NovelCard>> = LoadResult.Idle,
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
    val selectedFavoriteGroupId: Long? = null
)

data class ForumState(
    val posts: LoadResult<List<ForumPost>> = LoadResult.Idle
)

data class ProfileState(
    val profile: LoadResult<UserProfile> = LoadResult.Idle,
    val checkinStats: LoadResult<UserCheckinStats> = LoadResult.Idle,
    val nameDraft: String = "",
    val bioDraft: String = "",
    val showCheckin: Boolean = true,
    val autoCheckin: Boolean = false,
    val adultBirthYearDraft: String = "",
    val saving: Boolean = false,
    val checkingIn: Boolean = false,
    val verifyingAdult: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val actionMessage: String? = null
)

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
    val selectedTab: UserProfileTab = UserProfileTab.Activities
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
    val reviewQuery: AdminReviewQuery = AdminReviewQuery(),
    val operationLogQuery: AdminOperationLogQuery = AdminOperationLogQuery(),
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
    val fileName: String? = null,
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

data class ForumPostDetailState(
    val postId: Long = 0,
    val detail: LoadResult<ForumPostDetail> = LoadResult.Idle,
    val comments: LoadResult<List<ForumComment>> = LoadResult.Idle,
    val commentDraft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
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
    val readerProgress: ReaderProgress? = null,
    val commentDraft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
    val actionMessage: String? = null,
    val actionLoading: Boolean = false
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

data class ReaderState(
    val bookId: Long = 0,
    val chapterId: Long = 0,
    val content: LoadResult<ReaderContent> = LoadResult.Idle,
    val chapters: LoadResult<List<Chapter>> = LoadResult.Idle,
    val comments: LoadResult<List<ChapterComment>> = LoadResult.Idle,
    val commentDraft: String = "",
    val replyingToCommentId: Long? = null,
    val replyingToName: String? = null,
    val actionMessage: String? = null,
    val actionLoading: Boolean = false
)

data class SearchOptions(
    val sortBy: String = "relevance",
    val sortOrder: String = "desc",
    val scope: String = "all",
    val matchType: String = "ai",
    val adultFilter: String = "all",
    val source: String = "",
    val wordCountRange: String = ""
)

data class ReaderUiOptions(
    val fontSizeSp: Int = 18,
    val theme: String = "system"
)

class NovalPieViewModel(application: Application) : AndroidViewModel(application) {
    private val networkConfigStore = NetworkConfigStore(application)
    private val authSessionStore = AuthSessionStore(application)
    private val readerProgressStore = ReaderProgressStore(application)
    private val readerSettingsStore = ReaderSettingsStore(application)
    private val searchHistoryStore = SearchHistoryStore(application)
    private val searchSettingsStore = SearchSettingsStore(application)
    private val workspaceLocalStore = WorkspaceLocalStore(application)
    private val editorArchiveStore = EditorArchiveStore(application)

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

    private val routes = mutableStateListOf<AppRoute>(AppRoute.Home)
    private var forumRequestSerial = 0L
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
    private var searchRequestSerial = 0L
    private var selectedFavoriteGroupId: Long? = null

    var currentTab by mutableStateOf(BottomTab.Collection)
        private set
    var forumState by mutableStateOf(ForumState())
        private set
    var forumPostDetailState by mutableStateOf(ForumPostDetailState())
        private set
    var forumCreateState by mutableStateOf(ForumCreateState())
        private set
    var homeState by mutableStateOf(HomeState())
        private set
    var profileState by mutableStateOf(ProfileState())
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
    var bookshelfQuery by mutableStateOf("")
        private set
    var searchKeyword by mutableStateOf(searchHistoryStore.loadLastKeyword())
        private set
    var searchHistory by mutableStateOf(searchHistoryStore.load())
        private set
    var searchOptions by mutableStateOf(searchSettingsStore.load().toSearchOptions())
        private set
    var searchResults by mutableStateOf<LoadResult<List<NovelCard>>>(LoadResult.Idle)
        private set
    var searchTags by mutableStateOf<LoadResult<List<NovelTag>>>(LoadResult.Idle)
        private set
    var searchPage by mutableStateOf(1)
        private set
    var searchCanLoadMore by mutableStateOf(false)
        private set
    var searchLoadingMore by mutableStateOf(false)
        private set

    /** See [HomeState.favoritesLoadMoreError]; search had the identical defect. */
    var searchLoadMoreError by mutableStateOf<String?>(null)
        private set
    var bookCatalogQuery by mutableStateOf("")
        private set
    var bookDetailState by mutableStateOf(BookDetailState())
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
        ReaderUiOptions(
            fontSizeSp = readerSettingsStore.loadFontSizeSp(),
            theme = readerSettingsStore.loadTheme()
        )
    )
        private set
    var readerProgress by mutableStateOf(readerProgressStore.load())
        private set
    var recentReaderProgresses by mutableStateOf(readerProgressStore.loadRecent())
        private set

    val currentRoute: AppRoute get() = routes.lastOrNull() ?: AppRoute.Home

    init {
        configureNovalPieImageLoader(application, proxySettings)
        loadForum()
        loadHome()
    }

    fun updateBookshelfQuery(value: String) {
        bookshelfQuery = value
    }

    fun selectFavoriteGroup(groupId: Long?) {
        if (selectedFavoriteGroupId == groupId) return
        selectedFavoriteGroupId = groupId
        loadHome()
    }

    fun updateSearchKeyword(value: String) {
        if (searchKeyword != value) invalidateSearchRequests()
        searchKeyword = value
    }

    fun useSearchHistory(keyword: String) {
        performSearch(keyword)
    }

    fun useSearchTag(tagName: String) {
        performSearch(tagName)
    }

    fun clearSearchHistory() {
        searchHistoryStore.clear()
        searchHistory = emptyList()
    }

    fun updateSearchSortBy(value: String) {
        if (searchOptions.sortBy != value) invalidateSearchRequests()
        searchOptions = searchOptions.copy(sortBy = value)
        saveSearchOptions()
    }

    fun updateSearchSortOrder(value: String) {
        if (searchOptions.sortOrder != value) invalidateSearchRequests()
        searchOptions = searchOptions.copy(sortOrder = value)
        saveSearchOptions()
    }

    fun updateSearchScope(value: String) {
        if (searchOptions.scope != value) invalidateSearchRequests()
        searchOptions = searchOptions.copy(scope = value)
        saveSearchOptions()
    }

    fun updateSearchMatchType(value: String) {
        if (searchOptions.matchType != value) invalidateSearchRequests()
        searchOptions = searchOptions.copy(matchType = value)
        saveSearchOptions()
    }

    fun updateSearchAdultFilter(value: String) {
        if (searchOptions.adultFilter != value) invalidateSearchRequests()
        searchOptions = searchOptions.copy(adultFilter = value)
        saveSearchOptions()
    }

    fun updateSearchSource(value: String) {
        if (searchOptions.source != value) invalidateSearchRequests()
        searchOptions = searchOptions.copy(source = value)
        saveSearchOptions()
    }

    fun updateSearchWordCountRange(value: String) {
        if (searchOptions.wordCountRange != value) invalidateSearchRequests()
        searchOptions = searchOptions.copy(wordCountRange = value)
        saveSearchOptions()
    }

    fun updateBookCatalogQuery(value: String) {
        bookCatalogQuery = value
    }

    fun updateReaderCatalogQuery(value: String) {
        readerCatalogQuery = value
    }

    fun increaseReaderFont() {
        val next = (readerUiOptions.fontSizeSp + 1).coerceAtMost(ReaderSettingsStore.MAX_FONT_SIZE_SP)
        readerSettingsStore.saveFontSizeSp(next)
        readerUiOptions = readerUiOptions.copy(fontSizeSp = next)
    }

    fun decreaseReaderFont() {
        val next = (readerUiOptions.fontSizeSp - 1).coerceAtLeast(ReaderSettingsStore.MIN_FONT_SIZE_SP)
        readerSettingsStore.saveFontSizeSp(next)
        readerUiOptions = readerUiOptions.copy(fontSizeSp = next)
    }

    fun cycleReaderTheme() {
        val next = when (readerUiOptions.theme) {
            "system" -> "sepia"
            "sepia" -> "dark"
            else -> "system"
        }
        readerSettingsStore.saveTheme(next)
        readerUiOptions = readerUiOptions.copy(theme = next)
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
            when (tab) {
                BottomTab.Collection -> loadHome()
                BottomTab.Discover -> loadSearchTags()
                BottomTab.Tools -> loadTools()
                BottomTab.Forum -> loadForum()
                BottomTab.Profile -> loadProfile()
            }
            return
        }
        
        if (tab == BottomTab.Collection) loadHome()
        if (tab == BottomTab.Forum) loadForum()
        if (tab == BottomTab.Discover) loadSearchTags()
        if (tab == BottomTab.Tools) loadTools()
        if (tab == BottomTab.Profile) loadProfile()

        routes.clear()
        routes.add(targetRoute)
        currentTab = tab
    }

    fun openSettings() {
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.Settings))
    }

    fun loadProfile() {
        val requestSerial = ++profileRequestSerial
        val tokenProfile = authToken?.let(::decodeAuthTokenProfile)
        profileState = profileState.copy(
            profile = tokenProfile?.let { LoadResult.Success(it) } ?: LoadResult.Loading,
            checkinStats = LoadResult.Loading,
            actionMessage = null
        )
        viewModelScope.launch {
            val profileResult = async { runCatching { api.currentUser() } }
            val statsResult = async { runCatching { api.currentUserCheckinStats() } }
            val resolvedProfile = resolveUserLoadResult(profileResult.await(), tokenProfile)
            val resolvedStats = statsResult.await().toLoadResult("签到统计")
            if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@launch
            val profile = (resolvedProfile as? LoadResult.Success)?.value
            profileState = ProfileState(
                profile = resolvedProfile,
                checkinStats = resolvedStats,
                nameDraft = profile?.name.orEmpty(),
                bioDraft = profile?.bio.orEmpty(),
                showCheckin = profile?.showCheckin ?: true,
                autoCheckin = profile?.autoCheckin ?: false
            )
            if (profile != null) homeState = homeState.copy(user = LoadResult.Success(profile))
        }
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
                    val profileResult = refreshedProfile.await()
                    val statsResult = refreshedStats.await()
                    if (!isFreshRequestSerial(requestSerial, profileRequestSerial)) return@onSuccess
                    val current = (profileState.profile as? LoadResult.Success)?.value
                    val resolvedProfile = resolveUserLoadResult(profileResult, current)
                    profileState = profileState.copy(
                        profile = resolvedProfile,
                        checkinStats = statsResult.toLoadResult("签到统计"),
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
            selectedTab = userProfileDetailState.selectedTab
        )
        viewModelScope.launch {
            val profile = async { runCatching { api.userProfile(userId) } }
            val activities = async { runCatching { api.userActivities(userId = userId) } }
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
            userProfileDetailState = userProfileDetailState.copy(
                userId = userId,
                profile = profileResult.toLoadResult("用户资料"),
                activities = activityResult.toLoadResult("用户动态"),
                books = booksResult.toLoadResult("用户作品"),
                checkinStats = statsResult.toLoadResult("签到统计"),
                checkinRecords = recordsResult.toLoadResult("签到记录"),
                checkinSettings = settingsResult.toLoadResult("签到设置")
            )
        }
    }

    fun selectUserProfileTab(tab: UserProfileTab) {
        userProfileDetailState = userProfileDetailState.copy(selectedTab = tab)
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

    private fun loadAdminSectionInternal(section: AdminSection, message: String?) {
        if (!isAdminProfile(currentUserProfile())) return
        val requestSerial = ++adminRequestSerial
        val reviewQuery = adminState.reviewQuery
        val operationLogQuery = adminState.operationLogQuery
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
                baseUrlRules = LoadResult.Loading,
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
                    val result = runCatching { api.adminOverview() }
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
                    val result = runCatching { api.adminKeys() }
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(keys = result.toLoadResult("Key 管理"))
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
                    val rules = async { runCatching { api.adminBaseUrlRules() } }
                    val logs = async { runCatching { api.adminSchedulerLogs() } }
                    val cookieResult = cookies.await()
                    val ruleResult = rules.await()
                    val logResult = logs.await()
                    if (!isFreshRequestSerial(requestSerial, adminRequestSerial)) return@launch
                    adminState = adminState.copy(
                        cookieConfigs = cookieResult.toLoadResult("Cookie 配置"),
                        baseUrlRules = ruleResult.toLoadResult("BaseURL 规则"),
                        schedulerLogs = logResult.toLoadResult("调度日志")
                    )
                }
                AdminSection.Shop -> {
                    val result = runCatching { api.adminShopItems() }
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
                stats = statsResult.toLoadResult("\u6d88\u606f\u7edf\u8ba1"),
                messages = messagesResult.toLoadResult("\u6d88\u606f\u5217\u8868")
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
                    onFailure = { LoadResult.Error(apiFailureMessage("\u6d88\u606f\u5217\u8868", it)) }
                ),
                pagination = pageResult.getOrNull()?.pagination ?: MessagePagination(),
                stats = statsResult.toLoadResult("\u6d88\u606f\u7edf\u8ba1")
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
                        actionMessage = apiFailureMessage("\u52a0\u8f7d\u66f4\u591a\u6d88\u606f", failure)
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
        runMessageCenterAction("\u6279\u91cf\u5df2\u8bfb") { api.markMessagesRead(ids) }
    }

    fun deleteSelectedMessages() {
        val ids = messageCenterState.selectedIds.toList()
        if (ids.isEmpty()) return
        runMessageCenterAction("\u6279\u91cf\u5220\u9664") { api.deleteMessages(ids) }
    }

    fun markAllMessagesRead() {
        runMessageCenterAction("\u5168\u90e8\u5df2\u8bfb") { api.markAllMessagesRead() }
    }

    fun toggleMessageStar(message: SiteMessage) {
        runMessageCenterAction(if (message.isStarred) "\u53d6\u6d88\u661f\u6807" else "\u6dfb\u52a0\u661f\u6807") {
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
                detail = result.toLoadResult("\u6d88\u606f\u8be6\u60c5")
            )
        }
    }

    fun markCurrentMessageRead() {
        val message = (messageDetailState.detail as? LoadResult.Success)?.value ?: return
        if (message.isRead || messageDetailState.actionLoading) return
        runMessageDetailAction("\u5df2\u6807\u8bb0\u4e3a\u5df2\u8bfb") { api.markMessageRead(message.id) }
    }

    fun toggleCurrentMessageStar() {
        val message = (messageDetailState.detail as? LoadResult.Success)?.value ?: return
        if (messageDetailState.actionLoading) return
        runMessageDetailAction(if (message.isStarred) "\u5df2\u53d6\u6d88\u661f\u6807" else "\u5df2\u6dfb\u52a0\u661f\u6807") {
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
                    actionMessage = apiFailureMessage("\u5220\u9664\u6d88\u606f", failure)
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
                messages = result.toLoadResult("\u79c1\u4fe1\u5bf9\u8bdd")
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
                        actionMessage = it.message ?: "\u79c1\u4fe1\u5df2\u53d1\u9001"
                    )
                },
                onFailure = { failure ->
                    messageConversationState.copy(
                        sending = false,
                        actionMessage = apiFailureMessage("\u53d1\u9001\u79c1\u4fe1", failure)
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
                        settings = LoadResult.Error(apiFailureMessage("\u6d88\u606f\u8bbe\u7f6e", failure)),
                        actionMessage = apiFailureMessage("\u6d88\u606f\u8bbe\u7f6e", failure)
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
                        actionMessage = it.message ?: "\u6d88\u606f\u8bbe\u7f6e\u5df2\u4fdd\u5b58"
                    )
                },
                onFailure = { failure ->
                    messageSettingsState.copy(
                        saving = false,
                        actionMessage = apiFailureMessage("\u4fdd\u5b58\u6d88\u606f\u8bbe\u7f6e", failure)
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
                        actionMessage = it.message ?: "$label\u5df2\u540c\u6b65",
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

    fun updateEditorText(value: String) {
        uploadEditorState = uploadEditorState.copy(text = value, actionMessage = null)
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
            uploadEditorState = result.fold(
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
                    uploadEditorState.copy(
                        text = loaded.text,
                        fileName = loaded.document.displayName,
                        metadata = loaded.metadata,
                        chapters = loaded.chapters,
                        selectedTab = if (loaded.chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
                        busy = false,
                        actionMessage = if (loaded.chapters.isEmpty()) "文件已加载，请配置分章规则" else "EPUB 已加载，共 ${loaded.chapters.size} 章"
                    )
                },
                onFailure = { failure ->
                    uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("打开编辑文件", failure))
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
            state.splitPattern,
            state.splitTarget,
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
                EditorSplitMode.CustomScript -> emptyList()
            }
        }
        uploadEditorState = result.fold(
            onSuccess = { chapters ->
                if (chapters.isEmpty()) state.copy(actionMessage = "没有匹配到章节标题，请调整规则")
                else state.copy(chapters = chapters, selectedTab = EditorTab.Chapters, actionMessage = "已生成 ${chapters.size} 章")
            },
            onFailure = { failure -> state.copy(actionMessage = "分章失败：${failure.message ?: "规则无效"}") }
        )
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
        val chapters = EditorProcessor.parseWebsiteIdentifiers(processedText)
        uploadEditorState = state.copy(
            text = processedText,
            chapters = if (chapters.isEmpty()) state.chapters else chapters,
            selectedTab = if (chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
            busy = false,
            actionMessage = if (chapters.isEmpty()) {
                "脚本处理完成；未发现网站章节标识，已保留处理后的文本"
            } else {
                "脚本处理完成，已生成 ${chapters.size} 章"
            }
        )
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
        uploadEditorState = result.fold(
            onSuccess = { replaced ->
                val changed = replaced != state.text
                state.copy(text = replaced, actionMessage = if (changed) "替换完成" else "未找到匹配项")
            },
            onFailure = { failure -> state.copy(actionMessage = "替换失败：${failure.message ?: "正则无效"}") }
        )
    }

    fun updateEditorChapter(index: Int, title: String, content: String) {
        if (index !in uploadEditorState.chapters.indices) return
        val next = uploadEditorState.chapters.toMutableList()
        next[index] = next[index].copy(title = title.trim().ifBlank { "第 ${index + 1} 章" }, content = content)
        uploadEditorState = uploadEditorState.copy(chapters = next, actionMessage = "章节已更新")
    }

    fun addEditorChapter() {
        val nextIndex = uploadEditorState.chapters.size
        uploadEditorState = uploadEditorState.copy(
            chapters = uploadEditorState.chapters + UploadChapter("第 ${nextIndex + 1} 章", "", nextIndex + 1),
            actionMessage = "已添加章节"
        )
    }

    fun deleteEditorChapter(index: Int) {
        if (index !in uploadEditorState.chapters.indices) return
        val next = uploadEditorState.chapters.filterIndexed { chapterIndex, _ -> chapterIndex != index }
            .mapIndexed { chapterIndex, chapter -> chapter.copy(chapterNumber = chapterIndex + 1) }
        uploadEditorState = uploadEditorState.copy(chapters = next, actionMessage = "章节已删除并重新编号")
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
        uploadEditorState = uploadEditorState.copy(
            text = archive.textContent,
            metadata = archive.metadata,
            fileName = archive.fileName,
            chapters = emptyList(),
            selectedTab = EditorTab.Text,
            actionMessage = "存档已加载，请重新生成章节目录"
        )
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
                apiConfigs = apiResult.toLoadResult("\u5de5\u4f5c\u533a API \u914d\u7f6e"),
                cookieStatus = cookieStatusResult.toLoadResult("Cookie \u72b6\u6001"),
                cookieConfigs = cookieResult.toLoadResult("Cookie \u914d\u7f6e"),
                health = healthResult.toLoadResult("\u5de5\u4f5c\u533a\u5065\u5eb7\u72b6\u6001")
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
                        actionMessage = it.message ?: "API \u914d\u7f6e\u5df2\u4fdd\u5b58"
                    )
                },
                onFailure = { failure ->
                    workspaceState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage("\u4fdd\u5b58 API \u914d\u7f6e", failure)
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
                        actionMessage = "API \u914d\u7f6e\u5df2\u5220\u9664"
                    )
                },
                onFailure = { failure ->
                    workspaceState.copy(
                        actionLoading = false,
                        actionMessage = apiFailureMessage("\u5220\u9664 API \u914d\u7f6e", failure)
                    )
                }
            )
            if (result.isSuccess) loadWorkspace()
        }
    }

    fun deleteWorkspaceServerApi(config: WorkspaceApiConfig) {
        runWorkspaceAction("API \u914d\u7f6e\u5df2\u5220\u9664") { api.deleteWorkspaceApi(config.id) }
    }

    fun saveWorkspaceCookie(draft: WorkspaceCookieDraft) {
        validateWorkspaceCookieDraft(draft)?.let { error ->
            workspaceState = workspaceState.copy(actionMessage = error)
            return
        }
        runWorkspaceAction("Cookie \u914d\u7f6e\u5df2\u4fdd\u5b58") {
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
        runWorkspaceAction("Cookie \u72b6\u6001\u5df2\u66f4\u65b0") {
            api.setWorkspaceCookieActive(config.id, !config.isActive)
        }
    }

    fun deleteWorkspaceCookie(config: com.novalpie.nativeapp.model.WorkspaceCookieConfig) {
        runWorkspaceAction("Cookie \u914d\u7f6e\u5df2\u5220\u9664") {
            api.deleteWorkspaceCookie(config.id)
        }
    }

    fun updateWorkspaceJobStatus(job: WorkspaceTranslationJob, status: String) {
        workspaceLocalStore.upsertJob(job.copy(status = status, updatedAt = System.currentTimeMillis().toString()))
        workspaceState = workspaceState.copy(
            jobs = workspaceLocalStore.loadJobs(),
            actionMessage = "\u4efb\u52a1\u72b6\u6001\u5df2\u66f4\u65b0"
        )
    }

    fun deleteWorkspaceJob(job: WorkspaceTranslationJob) {
        workspaceLocalStore.deleteJob(job.id)
        workspaceState = workspaceState.copy(
            jobs = workspaceLocalStore.loadJobs(),
            actionMessage = "\u4efb\u52a1\u5df2\u5220\u9664"
        )
    }

    private fun runWorkspaceAction(
        successMessage: String,
        action: suspend () -> com.novalpie.nativeapp.model.WorkspaceActionResult
    ) {
        if (workspaceState.actionLoading) return
        workspaceState = workspaceState.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            val result = runCatching { action() }
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

    fun loadForum() {
        val requestSerial = ++forumRequestSerial
        forumState = ForumState(posts = LoadResult.Loading)
        viewModelScope.launch {
            val result = runCatching { api.forumPosts(page = 1, limit = PAGE_SIZE) }
            if (!isFreshRequestSerial(requestSerial, forumRequestSerial)) return@launch
            forumState = ForumState(posts = result.toLoadResult("论坛"))
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
        forumPostDetailState = ForumPostDetailState(
            postId = postId,
            detail = LoadResult.Loading,
            comments = LoadResult.Loading,
            commentDraft = forumPostDetailState.takeIf { it.postId == postId }?.commentDraft.orEmpty(),
            replyingToCommentId = forumPostDetailState.takeIf { it.postId == postId }?.replyingToCommentId,
            replyingToName = forumPostDetailState.takeIf { it.postId == postId }?.replyingToName
        )
        viewModelScope.launch {
            val detail = async { runCatching { api.forumPostDetail(postId) } }
            val comments = async { runCatching { api.forumPostComments(postId = postId, page = 1, limit = PAGE_SIZE) } }
            if (currentRoute != AppRoute.ForumPostDetail(postId)) return@launch
            forumPostDetailState = forumPostDetailState.copy(
                detail = detail.await().toLoadResult(VisibleUiLabels.ForumPostDetail),
                comments = comments.await().toLoadResult(VisibleUiLabels.Comments)
            )
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
                        actionMessage = it.message ?: "\u8bc4\u8bba\u5df2\u63d0\u4ea4"
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
                        actionMessage = it.message ?: "$label \u5df2\u540c\u6b65"
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
                        actionMessage = it.message ?: "$label \u5df2\u540c\u6b65"
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
        uploadRequestSerial++
        uploadBookState = UploadBookState(existingNovelId = bookId)
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.BookAppend(bookId)))
    }

    fun openBook(bookId: Long) {
        if (bookId <= 0) return
        val currentStack = routes.toList()
        val nextStack = pushDistinctRoute(currentStack, AppRoute.BookDetail(bookId))
        if (nextStack === currentStack) return
        routes.replaceWith(nextStack)
        loadBookDetail(bookId)
    }

    fun openReader(bookId: Long, chapterId: Long) {
        if (bookId <= 0 || chapterId <= 0) return
        val next = AppRoute.Reader(bookId, chapterId)
        val currentStack = routes.toList()
        val nextStack = replaceTopReaderRoute(currentStack, next)
        if (nextStack === currentStack) return
        routes.replaceWith(nextStack)
        loadReader(bookId, chapterId)
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
        readerProgress = null
        recentReaderProgresses = emptyList()
    }

    fun openWebFallback(url: String) {
        routes.replaceWith(pushDistinctRoute(routes.toList(), AppRoute.WebFallback(url)))
    }

    fun openLoginFallback() {
        openWebFallback("https://novalpie.cc/login")
    }

    fun openDeepLink(rawUri: String) {
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return
        val isNativeScheme = uri.scheme == "novalpie" && uri.host == "app"
        val isWebsiteRoute = uri.scheme in setOf("https", "http") && uri.host == "novalpie.cc"
        if (!isNativeScheme && !isWebsiteRoute) return

        val segments = uri.pathSegments
        if (segments.firstOrNull() == "user") {
            val userId = segments.getOrNull(1)?.toLongOrNull() ?: return
            openUserProfile(userId)
            return
        }
        if (segments.firstOrNull() != "book") return

        val bookId = segments.getOrNull(1)?.toLongOrNull() ?: return
        val chapterId = segments.getOrNull(2)?.toLongOrNull()

        currentTab = BottomTab.Collection
        routes.clear()
        routes.add(AppRoute.Home)

        if (chapterId != null && chapterId > 0) {
            routes.add(AppRoute.BookDetail(bookId))
            loadBookDetail(bookId)
            routes.add(AppRoute.Reader(bookId, chapterId))
            loadReader(bookId, chapterId)
        } else {
            openBook(bookId)
        }
    }

    fun goBack(): Boolean {
        if (routes.size <= 1) return false
        routes.removeAt(routes.lastIndex)
        return true
    }

    fun loadHome() {
        val requestSerial = ++homeRequestSerial
        val tokenProfile = authToken?.let(::decodeAuthTokenProfile)
        homeState = HomeState(
            user = tokenProfile?.let { LoadResult.Success(it) } ?: LoadResult.Loading,
            groups = LoadResult.Loading,
            favorites = LoadResult.Loading,
            favoritesPage = 1,
            selectedFavoriteGroupId = selectedFavoriteGroupId
        )
        viewModelScope.launch {
            val user = async { runCatching { api.currentUser() } }
            val groups = async { runCatching { api.favoriteGroups() } }
            val favoriteGroupId = selectedFavoriteGroupId
            val favorites = async { runCatching { api.favorites(page = 1, limit = PAGE_SIZE, groupId = favoriteGroupId) } }
            val favoritesResult = favorites.await()
            if (!isFreshRequestSerial(requestSerial, homeRequestSerial)) return@launch
            homeState = HomeState(
                user = resolveUserLoadResult(user.await(), tokenProfile),
                groups = groups.await().toLoadResult(VisibleUiLabels.FavoriteGroups),
                favorites = favoritesResult.toLoadResult(VisibleUiLabels.Bookshelf),
                favoritesPage = 1,
                favoritesCanLoadMore = favoritesResult.getOrNull()?.size == PAGE_SIZE,
                selectedFavoriteGroupId = favoriteGroupId
            )
        }
    }

    fun loadMoreFavorites() {
        val currentFavorites = (homeState.favorites as? LoadResult.Success)?.value ?: return
        if (homeState.favoritesLoadingMore || !homeState.favoritesCanLoadMore) return

        val nextPage = homeState.favoritesPage + 1
        val requestSerial = homeRequestSerial
        val favoriteGroupId = homeState.selectedFavoriteGroupId
        homeState = homeState.copy(favoritesLoadingMore = true, favoritesLoadMoreError = null)
        viewModelScope.launch {
            val result = runCatching { api.favorites(page = nextPage, limit = PAGE_SIZE, groupId = favoriteGroupId) }
            if (!isFreshRequestSerial(requestSerial, homeRequestSerial)) return@launch
            homeState = result.fold(
                onSuccess = { nextItems ->
                    val merged = mergeBooksById(currentFavorites, nextItems)
                    homeState.copy(
                        favorites = LoadResult.Success(merged),
                        favoritesPage = nextPage,
                        favoritesCanLoadMore = nextItems.size == PAGE_SIZE,
                        favoritesLoadingMore = false,
                        favoritesLoadMoreError = null
                    )
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
        searchResults = LoadResult.Loading
        searchPage = 1
        searchCanLoadMore = false
        searchLoadingMore = false
        searchLoadMoreError = null
        val options = searchOptions
        val request = SearchRequestSnapshot(
            serial = requestSerial,
            keyword = keyword,
            options = options,
            page = 1
        )
        searchHistoryStore.saveKeyword(keyword)
        searchHistory = searchHistoryStore.load()
        viewModelScope.launch {
            val result = runCatching {
                api.search(
                    keyword = keyword,
                    page = 1,
                    limit = PAGE_SIZE,
                    sortBy = options.sortBy,
                    sortOrder = options.sortOrder,
                    scope = options.scope,
                    matchType = options.matchType,
                    adultFilter = options.adultFilter,
                    source = options.source,
                    minWordCount = searchMinWordCount(options.wordCountRange),
                    maxWordCount = searchMaxWordCount(options.wordCountRange)
                )
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
            searchResults = result.toLoadResult(VisibleUiLabels.Search)
            searchCanLoadMore = result.getOrNull()?.size == PAGE_SIZE
        }
    }

    fun loadMoreSearch() {
        val currentResults = (searchResults as? LoadResult.Success)?.value ?: return
        if (searchLoadingMore || !searchCanLoadMore) return

        val keyword = searchKeyword
        val options = searchOptions
        val nextPage = searchPage + 1
        val request = SearchRequestSnapshot(
            serial = searchRequestSerial,
            keyword = keyword,
            options = options,
            page = nextPage
        )
        searchLoadingMore = true
        viewModelScope.launch {
            val result = runCatching {
                api.search(
                    keyword = keyword,
                    page = nextPage,
                    limit = PAGE_SIZE,
                    sortBy = options.sortBy,
                    sortOrder = options.sortOrder,
                    scope = options.scope,
                    matchType = options.matchType,
                    adultFilter = options.adultFilter,
                    source = options.source,
                    minWordCount = searchMinWordCount(options.wordCountRange),
                    maxWordCount = searchMaxWordCount(options.wordCountRange)
                )
            }
            if (
                !isFreshSearchResult(
                    request = request,
                    activeSerial = searchRequestSerial,
                    currentKeyword = searchKeyword,
                    currentOptions = searchOptions,
                    expectedPage = nextPage
                )
            ) return@launch
            result.fold(
                onSuccess = { nextItems ->
                    searchResults = LoadResult.Success(mergeBooksById(currentResults, nextItems))
                    searchPage = nextPage
                    searchCanLoadMore = nextItems.size == PAGE_SIZE
                    searchLoadMoreError = null
                },
                onFailure = {
                    // Same reasoning as loadMoreFavorites: the results already on screen are good
                    // data, so a failed extra page must not replace them with an error card.
                    searchLoadMoreError = apiFailureMessage(VisibleUiLabels.Search, it)
                }
            )
            searchLoadingMore = false
        }
    }

    fun loadSearchTags() {
        if (searchTags is LoadResult.Loading) return
        searchTags = LoadResult.Loading
        viewModelScope.launch {
            val result = runCatching { api.tags(sort = "count", limit = 24) }
            searchTags = result.toLoadResult("标签")
        }
    }

    fun loadBookDetail(bookId: Long) {
        bookDetailState = BookDetailState(
            bookId = bookId,
            book = LoadResult.Loading,
            chapters = LoadResult.Loading,
            comments = LoadResult.Loading,
            favoriteStatus = LoadResult.Loading,
            readerProgress = readerProgressStore.load(bookId)
        )
        viewModelScope.launch {
            val book = async { runCatching { api.bookDetail(bookId) } }
            val originalCover = async { runCatching { api.bookCoverPhoto(bookId) } }
            val chapters = async { runCatching { api.chapters(bookId) } }
            val comments = async { runCatching { api.bookComments(bookId = bookId, page = 1, limit = PAGE_SIZE) } }
            val favoriteStatus = async { runCatching { api.favoriteStatus(bookId) } }
            if (!isFreshBookDetailResult(currentRoute, bookDetailState, bookId)) return@launch
            bookDetailState = BookDetailState(
                bookId = bookId,
                book = book.await().toLoadResult(VisibleUiLabels.BookDetail),
                chapters = chapters.await().toLoadResult(VisibleUiLabels.ChapterCatalog),
                comments = LoadResult.Loading,
                favoriteStatus = LoadResult.Loading,
                readerProgress = readerProgressStore.load(bookId)
            )
            val originalCoverUrl = originalCover.await().getOrNull()
            val loadedBook = (bookDetailState.book as? LoadResult.Success)?.value
            if (!originalCoverUrl.isNullOrBlank() && loadedBook != null) {
                bookDetailState = bookDetailState.copy(
                    book = LoadResult.Success(loadedBook.copy(fullCoverUrl = originalCoverUrl))
                )
            }
            if (!isFreshBookDetailResult(currentRoute, bookDetailState, bookId)) return@launch
            bookDetailState = bookDetailState.copy(
                comments = comments.await().toLoadResult("评论区"),
                favoriteStatus = favoriteStatus.await().toLoadResult("\u6536\u85cf\u72b6\u6001"),
                readerProgress = readerProgressStore.load(bookId)
            )
        }
    }

    fun loadReader(bookId: Long, chapterId: Long) {
        readerState = ReaderState(
            bookId = bookId,
            chapterId = chapterId,
            content = LoadResult.Loading,
            chapters = LoadResult.Loading,
            comments = LoadResult.Loading
        )
        viewModelScope.launch {
            val content = async { runCatching { api.chapterContent(chapterId) } }
            val chapters = async { runCatching { api.chapters(bookId) } }
            val comments = async { runCatching { api.chapterComments(bookId = bookId, chapterId = chapterId, page = 1, limit = PAGE_SIZE) } }
            val contentResult = content.await()
            val chaptersResult = chapters.await()
            val commentsResult = comments.await()
            val chapterTitle = chaptersResult.getOrNull()
                ?.firstOrNull { it.id == chapterId }
                ?.title
            if (!isFreshReaderResult(currentRoute, readerState, bookId, chapterId)) return@launch
            if (contentResult.isSuccess) {
                saveReaderProgress(bookId, chapterId, chapterTitle)
            }
            readerState = ReaderState(
                bookId = bookId,
                chapterId = chapterId,
                content = contentResult.toLoadResult("\u9605\u8bfb\u5668\u6b63\u6587"),
                chapters = chaptersResult.toLoadResult("\u9605\u8bfb\u5668\u76ee\u5f55"),
                comments = commentsResult.toLoadResult(VisibleUiLabels.ChapterComments)
            )
        }
    }

    private fun saveReaderProgress(bookId: Long, chapterId: Long, chapterTitle: String?) {
        readerProgressStore.save(bookId, chapterId, chapterTitle)
        readerProgress = readerProgressStore.load()
        recentReaderProgresses = readerProgressStore.loadRecent()
        if (bookDetailState.bookId == bookId) {
            bookDetailState = bookDetailState.copy(readerProgress = readerProgressStore.load(bookId))
        }
    }

    private fun saveSearchOptions() {
        searchSettingsStore.save(searchOptions.toPersistedSearchSettings())
    }

    private fun invalidateSearchRequests() {
        searchRequestSerial += 1
        searchCanLoadMore = false
        searchLoadingMore = false
    }

    private fun mergeBooksById(current: List<NovelCard>, next: List<NovelCard>): List<NovelCard> {
        if (next.isEmpty()) return current
        return (current + next).distinctBy { it.id }
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
        private const val TOOLS_MESSAGE_PREVIEW_LIMIT = 6
    }
}

internal fun resolveUserLoadResult(
    remote: Result<UserProfile>,
    tokenProfile: UserProfile?
): LoadResult<UserProfile> = remote.fold(
    onSuccess = { LoadResult.Success(it) },
    onFailure = { failure ->
        tokenProfile?.let { LoadResult.Success(it) }
            ?: LoadResult.Error(apiFailureMessage("\u767b\u5f55\u72b6\u6001", failure))
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
        wordCountRange = wordCountRange
    )

private fun SearchOptions.toPersistedSearchSettings(): PersistedSearchSettings =
    PersistedSearchSettings(
        sortBy = sortBy,
        sortOrder = sortOrder,
        scope = scope,
        matchType = matchType,
        adultFilter = adultFilter,
        source = source,
        wordCountRange = wordCountRange
    )
