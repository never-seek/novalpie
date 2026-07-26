package com.novalpie.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.FavoriteGroup
import com.novalpie.nativeapp.model.FavoriteStatus
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.ForumPostDetail
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.MessageStats
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.SiteMessage
import com.novalpie.nativeapp.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovalPieApp(startUri: String? = null, viewModel: NovalPieViewModel = viewModel()) {
    val route = viewModel.currentRoute

    LaunchedEffect(startUri) {
        if (!startUri.isNullOrBlank()) viewModel.openDeepLink(startUri)
    }

    BackHandler(enabled = route !is AppRoute.Forum && route !is AppRoute.Home && route !is AppRoute.Search && route !is AppRoute.Tools && route !is AppRoute.Profile) {
        viewModel.goBack()
    }

    Scaffold(
        topBar = {
            if (globalProductTopBarVisible(route)) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NovalPie", fontWeight = FontWeight.Bold)
                            Text(routeContextLabel(route, viewModel.currentTab), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    navigationIcon = {
                        if (route !is AppRoute.Forum && route !is AppRoute.Home && route !is AppRoute.Search && route !is AppRoute.Tools && route !is AppRoute.Profile) {
                            IconButton(onClick = { viewModel.goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (route is AppRoute.Forum || route is AppRoute.Home || route is AppRoute.Search || route is AppRoute.Tools || route is AppRoute.Profile) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    BottomTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = viewModel.currentTab == tab,
                            onClick = { viewModel.openTab(tab) },
                            icon = { Icon(bottomTabIcon(tab), contentDescription = bottomTabDisplayLabel(tab)) },
                            label = { Text(bottomTabDisplayLabel(tab), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                unselectedTextColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (route) {
                AppRoute.Forum -> ForumScreen(
                    posts = viewModel.forumState.posts,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onRefresh = viewModel::loadForum,
                    onOpenPost = viewModel::openForumPost,
                    onCreatePost = viewModel::openForumCreate,
                    onOpenUser = viewModel::openUserProfile,
                    onOpenLogin = viewModel::openLoginFallback,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc") }
                )

                AppRoute.ForumCreate -> ForumCreateScreen(
                    state = viewModel.forumCreateState,
                    onDraftChange = viewModel::updateForumCreateDraft,
                    onSubmit = viewModel::submitForumPost,
                    onOpenLogin = viewModel::openLoginFallback
                )

                is AppRoute.ForumPostDetail -> ForumPostDetailScreen(
                    state = viewModel.forumPostDetailState,
                    onRetry = { viewModel.loadForumPostDetail(route.postId) },
                    onDraftChange = viewModel::updateForumCommentDraft,
                    onSubmitComment = viewModel::submitForumComment,
                    onReplyComment = viewModel::replyToForumComment,
                    onCancelReply = viewModel::cancelForumReply,
                    onLike = viewModel::likeForumPost,
                    onDislike = viewModel::dislikeForumPost,
                    onEmoji = viewModel::emojiForumPost,
                    onAward = viewModel::awardForumPost,
                    onCommentLike = viewModel::likeForumComment,
                    onCommentDislike = viewModel::dislikeForumComment,
                    onCommentEmoji = viewModel::emojiForumComment,
                    onCommentAward = viewModel::awardForumComment,
                    onOpenUser = viewModel::openUserProfile,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/posts/${route.postId}") }
                )

                AppRoute.Home -> HomeScreen(
                    state = viewModel.homeState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    readerProgress = viewModel.readerProgress,
                    recentReaderProgresses = viewModel.recentReaderProgresses,
                    bookshelfQuery = viewModel.bookshelfQuery,
                    onRefresh = viewModel::loadHome,
                    onBookshelfQueryChange = viewModel::updateBookshelfQuery,
                    onFavoriteGroupSelected = viewModel::selectFavoriteGroup,
                    onOpenLogin = viewModel::openLoginFallback,
                    onContinueReading = viewModel::continueReading,
                    onClearReaderProgress = viewModel::clearReaderProgress,
                    onOpenBook = viewModel::openBook,
                    onLoadMoreFavorites = viewModel::loadMoreFavorites,
                    onOpenSearch = { viewModel.openTab(BottomTab.Discover) },
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/favorites") }
                )

                AppRoute.Search -> SearchScreen(
                    keyword = viewModel.searchKeyword,
                    searchHistory = viewModel.searchHistory,
                    options = viewModel.searchOptions,
                    results = viewModel.searchResults,
                    tags = viewModel.searchTags,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onUseSearchHistory = viewModel::useSearchHistory,
                    onClearSearchHistory = viewModel::clearSearchHistory,
                    onUseTag = viewModel::useSearchTag,
                    onRefreshTags = viewModel::loadSearchTags,
                    onSortByChange = viewModel::updateSearchSortBy,
                    onSortOrderChange = viewModel::updateSearchSortOrder,
                    onScopeChange = viewModel::updateSearchScope,
                    onMatchTypeChange = viewModel::updateSearchMatchType,
                    onAdultFilterChange = viewModel::updateSearchAdultFilter,
                    onSourceChange = viewModel::updateSearchSource,
                    onWordCountRangeChange = viewModel::updateSearchWordCountRange,
                    onSearch = { submittedKeyword -> viewModel.performSearch(submittedKeyword) },
                    searchCanLoadMore = viewModel.searchCanLoadMore,
                    searchLoadingMore = viewModel.searchLoadingMore,
                    onLoadMore = viewModel::loadMoreSearch,
                    onOpenBook = viewModel::openBook,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/search?sort_by=relevance") }
                )

                AppRoute.Tools -> ToolsScreen(
                    state = viewModel.toolsState,
                    user = viewModel.homeState.user,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onRefresh = viewModel::loadTools,
                    onOpenLogin = viewModel::openLoginFallback,
                    onOpenMessages = viewModel::openMessageCenter,
                    onOpenMessage = viewModel::openMessage,
                    onOpenRoute = { path ->
                        when (path) {
                            "/workspace" -> viewModel.openWorkspace()
                            "/upload" -> viewModel.openUploadBook()
                            "/upload-editor" -> viewModel.openUploadEditor()
                            "/political-exam" -> viewModel.openPoliticalExam()
                            else -> {
                                val adminSection = AdminSection.values().firstOrNull { it.websitePath == path }
                                if (adminSection != null) viewModel.openAdminSection(adminSection)
                                else viewModel.openWebFallback("https://novalpie.cc$path")
                            }
                        }
                    }
                )

                is AppRoute.Admin -> AdminScreen(
                    state = viewModel.adminState,
                    onRefresh = { viewModel.loadAdminSection(route.section) },
                    onSectionSelected = viewModel::openAdminSection,
                    onReviewQueryChange = viewModel::updateAdminReviewQuery,
                    onApplyReviewQuery = viewModel::applyAdminReviewQuery,
                    onResetReviewQuery = viewModel::resetAdminReviewQuery,
                    onOperationLogQueryChange = viewModel::updateAdminOperationLogQuery,
                    onApplyOperationLogQuery = viewModel::applyAdminOperationLogQuery,
                    onResetOperationLogQuery = viewModel::resetAdminOperationLogQuery,
                    onToggleReviewSetting = viewModel::toggleAdminReviewSetting,
                    onReviewAction = viewModel::adminReviewAction,
                    onUpdateKeyStatus = viewModel::updateAdminKeyStatus,
                    onDeleteKey = viewModel::deleteAdminKey,
                    onSaveCookie = viewModel::saveAdminCookieConfig,
                    onToggleCookie = viewModel::toggleAdminCookieConfig,
                    onDeleteCookie = viewModel::deleteAdminCookieConfig,
                    onSaveRule = viewModel::saveAdminBaseUrlRule,
                    onSetRuleAction = viewModel::setAdminBaseUrlRuleAction,
                    onDeleteRule = viewModel::deleteAdminBaseUrlRule,
                    onSaveShopItem = viewModel::saveAdminShopItem,
                    onToggleShopItem = viewModel::toggleAdminShopItem,
                    onDeleteShopItem = viewModel::deleteAdminShopItem
                )

                AppRoute.MessageCenter -> MessageCenterScreen(
                    state = viewModel.messageCenterState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onOpenLogin = viewModel::openLoginFallback,
                    onRefresh = viewModel::loadMessageCenter,
                    onKeywordChange = viewModel::updateMessageKeyword,
                    onSearch = viewModel::applyMessageSearch,
                    onTypeSelected = viewModel::selectMessageType,
                    onReadSelected = viewModel::selectMessageReadFilter,
                    onPrioritySelected = viewModel::selectMessagePriority,
                    onToggleSelected = viewModel::toggleMessageSelected,
                    onSelectAll = viewModel::selectAllVisibleMessages,
                    onMarkSelectedRead = viewModel::markSelectedMessagesRead,
                    onDeleteSelected = viewModel::deleteSelectedMessages,
                    onMarkAllRead = viewModel::markAllMessagesRead,
                    onToggleStar = viewModel::toggleMessageStar,
                    onOpenMessage = viewModel::openMessage,
                    onLoadMore = viewModel::loadMoreMessages,
                    onOpenSettings = viewModel::openMessageSettings
                )

                is AppRoute.MessageDetail -> MessageDetailScreen(
                    state = viewModel.messageDetailState,
                    onRetry = { viewModel.loadMessageDetail(route.messageId) },
                    onMarkRead = viewModel::markCurrentMessageRead,
                    onToggleStar = viewModel::toggleCurrentMessageStar,
                    onDelete = viewModel::deleteCurrentMessage,
                    onOpenAction = viewModel::openMessageAction,
                    onOpenConversation = viewModel::openCurrentMessageConversation
                )

                is AppRoute.MessageConversation -> MessageConversationScreen(
                    state = viewModel.messageConversationState,
                    currentUserId = (viewModel.homeState.user as? LoadResult.Success)?.value?.id,
                    onRetry = { viewModel.loadMessageConversation(route.targetUserId, route.targetName) },
                    onDraftChange = viewModel::updateMessageDraft,
                    onSend = viewModel::sendMessageDraft
                )

                AppRoute.MessageSettings -> MessageSettingsScreen(
                    state = viewModel.messageSettingsState,
                    onRetry = viewModel::loadMessageSettings,
                    onDraftChange = viewModel::updateMessageSettingsDraft,
                    onSave = viewModel::saveMessageSettings
                )

                AppRoute.Workspace -> WorkspaceScreen(
                    state = viewModel.workspaceState,
                    onRefresh = viewModel::loadWorkspace,
                    onTabSelected = viewModel::selectWorkspaceTab,
                    onSaveApi = viewModel::saveWorkspaceApi,
                    onDeleteLocalApi = viewModel::deleteWorkspaceLocalApi,
                    onDeleteServerApi = viewModel::deleteWorkspaceServerApi,
                    onSaveCookie = viewModel::saveWorkspaceCookie,
                    onToggleCookie = viewModel::toggleWorkspaceCookie,
                    onDeleteCookie = viewModel::deleteWorkspaceCookie,
                    onUpdateJobStatus = viewModel::updateWorkspaceJobStatus,
                    onDeleteJob = viewModel::deleteWorkspaceJob,
                    onOpenUpload = viewModel::openUploadBook
                )

                AppRoute.UploadBook -> UploadBookScreen(
                    state = viewModel.uploadBookState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onOpenLogin = viewModel::openLoginFallback,
                    onPickEpub = viewModel::selectUploadEpub,
                    onDraftChange = viewModel::updateUploadBookDraft,
                    onSubmit = viewModel::submitUploadBook,
                    onClear = viewModel::clearUploadBook,
                    onOpenEditor = viewModel::openUploadEditor,
                    onOpenBook = viewModel::openUploadedBook
                )

                AppRoute.UploadEditor -> UploadEditorScreen(
                    state = viewModel.uploadEditorState,
                    onTabSelected = viewModel::selectEditorTab,
                    onOpenDocument = viewModel::selectEditorDocument,
                    onEncodingChange = viewModel::updateEditorEncoding,
                    onTextChange = viewModel::updateEditorText,
                    onMetadataChange = viewModel::updateEditorMetadata,
                    onSplitModeChange = viewModel::updateEditorSplitMode,
                    onSplitPatternChange = viewModel::updateEditorSplitPattern,
                    onSplitTargetChange = viewModel::updateEditorSplitTarget,
                    onCustomScriptChange = viewModel::updateEditorCustomScript,
                    onScriptChunkedChange = viewModel::updateEditorScriptChunked,
                    onScriptChunkSizeChange = viewModel::updateEditorScriptChunkSize,
                    onCustomScriptResult = viewModel::completeEditorCustomScript,
                    onAiConfigSelected = viewModel::selectEditorAiConfig,
                    onGenerateAiRegex = viewModel::generateEditorRegexWithAi,
                    onProcessSplit = viewModel::processEditorSplit,
                    onFindChange = viewModel::updateEditorFind,
                    onReplaceChange = viewModel::updateEditorReplace,
                    onFindRegexChange = viewModel::updateEditorFindUsesRegex,
                    onReplaceAll = viewModel::replaceEditorText,
                    onUpdateChapter = viewModel::updateEditorChapter,
                    onAddChapter = viewModel::addEditorChapter,
                    onDeleteChapter = viewModel::deleteEditorChapter,
                    onArchiveNameChange = viewModel::updateEditorArchiveName,
                    onSaveArchive = viewModel::saveEditorArchive,
                    onLoadArchive = viewModel::loadEditorArchive,
                    onDeleteArchive = viewModel::deleteEditorArchive,
                    onClearArchives = viewModel::clearEditorArchives,
                    onExportEpub = viewModel::exportEditorEpub,
                    onSendToUpload = viewModel::sendEditorToUpload,
                    onClear = viewModel::clearUploadEditor
                )

                AppRoute.PoliticalExam -> PoliticalExamScreen(
                    state = viewModel.politicalExamState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onStart = viewModel::startPoliticalExam,
                    onOpenLogin = viewModel::openLoginFallback,
                    onSelectSingle = viewModel::selectPoliticalExamSingle,
                    onToggleMultiple = viewModel::togglePoliticalExamMultiple,
                    onSelectTrueFalse = viewModel::selectPoliticalExamTrueFalse,
                    onUpdateBlank = viewModel::updatePoliticalExamBlank,
                    onTick = viewModel::tickPoliticalExamTimer,
                    onSubmit = viewModel::submitPoliticalExam,
                    onReset = viewModel::resetPoliticalExam,
                    onBack = viewModel::goBack
                )

                AppRoute.Profile -> ProfileScreen(
                    state = viewModel.profileState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onRefresh = viewModel::loadProfile,
                    onOpenLogin = viewModel::openLoginFallback,
                    onNameChange = viewModel::updateProfileName,
                    onBioChange = viewModel::updateProfileBio,
                    onShowCheckinChange = viewModel::updateProfileShowCheckin,
                    onAutoCheckinChange = viewModel::updateProfileAutoCheckin,
                    onAdultBirthYearChange = viewModel::updateProfileAdultBirthYear,
                    onSave = viewModel::saveProfile,
                    onCheckin = viewModel::checkinCurrentUser,
                    onVerifyAdult = viewModel::verifyCurrentUserAdult,
                    onAvatarSelected = viewModel::uploadProfileAvatar,
                    onOpenSettings = viewModel::openSettings
                )

                is AppRoute.UserProfileDetail -> UserProfileDetailScreen(
                    state = viewModel.userProfileDetailState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onRetry = { viewModel.loadUserProfile(route.userId) },
                    onTabSelected = viewModel::selectUserProfileTab,
                    onOpenActivity = viewModel::openUserActivity,
                    onOpenBook = viewModel::openBook,
                    onMessageUser = viewModel::openMessageConversation,
                    onOpenLogin = viewModel::openLoginFallback
                )

                AppRoute.Settings -> SettingsScreen(
                    user = viewModel.profileState.profile,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    readerProgress = viewModel.readerProgress,
                    readerOptions = viewModel.readerUiOptions,
                    proxyEnabled = viewModel.proxyEnabled,
                    proxyHost = viewModel.proxyHost,
                    proxyPort = viewModel.proxyPortText,
                    proxySummary = viewModel.proxySettings.summary(),
                    onRefreshAccount = viewModel::loadHome,
                    onOpenLogin = viewModel::openLoginFallback,
                    onClearToken = viewModel::clearAuthToken,
                    onProxyEnabledChange = viewModel::updateProxyEnabled,
                    onProxyHostChange = viewModel::updateProxyHost,
                    onProxyPortChange = viewModel::updateProxyPort,
                    onSaveProxy = viewModel::saveProxySettings,
                    onOpenHomeFallback = { viewModel.openWebFallback("https://novalpie.cc") },
                    onOpenSearchFallback = { viewModel.openWebFallback("https://novalpie.cc/search?sort_by=relevance") }
                )

                is AppRoute.BookDetail -> BookDetailScreen(
                    state = viewModel.bookDetailState,
                    readerProgress = viewModel.bookDetailState.readerProgress,
                    catalogQuery = viewModel.bookCatalogQuery,
                    onCatalogQueryChange = viewModel::updateBookCatalogQuery,
                    onRetry = { viewModel.loadBookDetail(route.bookId) },
                    onOpenReader = viewModel::openReader,
                    onEditInfo = { viewModel.openBookEditInfo(route.bookId) },
                    onManageChapters = { viewModel.openBookChapters(route.bookId) },
                    onAppendChapters = { viewModel.openBookAppend(route.bookId) },
                    onCommentDraftChange = viewModel::updateBookCommentDraft,
                    onSubmitComment = viewModel::submitBookComment,
                    onReplyComment = viewModel::replyToBookComment,
                    onCancelCommentReply = viewModel::cancelBookCommentReply,
                    onCommentLike = viewModel::likeBookComment,
                    onCommentDislike = viewModel::dislikeBookComment,
                    onCommentEmoji = viewModel::emojiBookComment,
                    onCommentAward = viewModel::awardBookComment,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/book/${route.bookId}") }
                )

                is AppRoute.BookEditInfo -> BookEditInfoScreen(
                    state = viewModel.bookEditState,
                    onRetry = { viewModel.loadBookEditInfo(route.bookId) },
                    onDraftChange = viewModel::updateBookEditDraft,
                    onCoverSelected = viewModel::uploadManagedBookCover,
                    onAccessPolicyDraftChange = viewModel::updateBookAccessPolicyDraft,
                    onSaveAccessPolicy = viewModel::saveManagedBookAccessPolicy,
                    onTransferIdentifierChange = viewModel::updateBookTransferIdentifier,
                    onTransfer = viewModel::transferManagedBook,
                    onSave = viewModel::saveManagedBook
                )

                is AppRoute.BookChapters -> BookChapterManagerScreen(
                    state = viewModel.bookChapterManagerState,
                    onRetry = { viewModel.loadManagedChapters(route.bookId) },
                    onToggleSelection = viewModel::toggleManagedChapterSelection,
                    onSelectAll = viewModel::selectAllManagedChapters,
                    onMove = viewModel::moveManagedChapter,
                    onSaveOrder = viewModel::saveManagedChapterOrder,
                    onOpenEditor = viewModel::openManagedChapterEditor,
                    onUpdateEditor = viewModel::updateManagedChapterDraft,
                    onDismissEditor = viewModel::dismissManagedChapterEditor,
                    onSaveEditor = viewModel::saveManagedChapterDraft,
                    onDelete = viewModel::deleteManagedChapter,
                    onBatchDelete = viewModel::batchDeleteManagedChapters,
                    onTranslationMode = viewModel::updateManagedTranslationMode,
                    onTranslate = viewModel::translateSelectedManagedChapters,
                    onOpenIllustrations = viewModel::openManagedChapterIllustrations,
                    onDismissIllustrations = viewModel::dismissManagedChapterIllustrations,
                    onUploadIllustrations = viewModel::uploadManagedChapterIllustrations,
                    onDeleteIllustration = viewModel::deleteManagedChapterIllustration,
                    onInsertIllustrationPlaceholder = viewModel::insertChapterIllustrationPlaceholder,
                    onAppend = { viewModel.openBookAppend(route.bookId) }
                )

                is AppRoute.BookAppend -> UploadBookScreen(
                    state = viewModel.uploadBookState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onOpenLogin = viewModel::openLoginFallback,
                    onPickEpub = viewModel::selectUploadEpub,
                    onDraftChange = viewModel::updateUploadBookDraft,
                    onSubmit = viewModel::submitUploadBook,
                    onClear = viewModel::clearUploadBook,
                    onOpenEditor = viewModel::openUploadEditor,
                    onOpenBook = viewModel::openUploadedBook
                )

                is AppRoute.Reader -> ReaderScreen(
                    state = viewModel.readerState,
                    options = viewModel.readerUiOptions,
                    catalogQuery = viewModel.readerCatalogQuery,
                    onCatalogQueryChange = viewModel::updateReaderCatalogQuery,
                    onDecreaseFont = viewModel::decreaseReaderFont,
                    onIncreaseFont = viewModel::increaseReaderFont,
                    onCycleTheme = viewModel::cycleReaderTheme,
                    onRetry = { viewModel.loadReader(route.bookId, route.chapterId) },
                    onOpenReader = viewModel::openReader,
                    onBack = viewModel::goBack,
                    onCommentDraftChange = viewModel::updateReaderCommentDraft,
                    onSubmitComment = viewModel::submitReaderComment,
                    onReplyComment = viewModel::replyToReaderComment,
                    onCancelCommentReply = viewModel::cancelReaderCommentReply,
                    onCommentLike = viewModel::likeReaderComment,
                    onCommentDislike = viewModel::dislikeReaderComment,
                    onCommentEmoji = viewModel::emojiReaderComment,
                    onCommentAward = viewModel::awardReaderComment,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/book/${route.bookId}/${route.chapterId}") }
                )

                is AppRoute.WebFallback -> WebFallbackScreen(
                    url = route.url,
                    proxySettings = viewModel.proxySettings,
                    authToken = viewModel.authToken,
                    onAuthTokenCaptured = viewModel::saveCapturedAuthToken
                )
            }
        }
    }
}

private fun bottomTabIcon(tab: BottomTab): ImageVector = when (tab) {
    BottomTab.Collection -> Icons.Filled.Favorite
    BottomTab.Discover -> Icons.Filled.Search
    BottomTab.Tools -> Icons.Filled.GridView
    BottomTab.Forum -> Icons.Filled.Forum
    BottomTab.Profile -> Icons.Filled.Person
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ForumScreen(
    posts: LoadResult<List<ForumPost>>,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenPost: (Long) -> Unit,
    onCreatePost: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val header = forumHeader()
    val actions = forumPrimaryActions(hasAuthToken)
    val feedItems = when (posts) {
        is LoadResult.Success -> posts.value.map(::forumPostFeedItem).ifEmpty { forumFeedItems() }
        else -> forumFeedItems()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "NOVALPIE_NATIVE_COMPOSE_HOME" },
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(header.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        header.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (hasAuthToken) "已同步" else "未同步",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Button(onClick = onRefresh) { Text(actions[0]) } }
                item { OutlinedButton(onClick = onOpenLogin) { Text(actions[1]) } }
                item { OutlinedButton(onClick = onOpenWeb) { Text(actions[2]) } }
            }
        }
        item {
            ForumStatsStrip(feedItems)
        }
        when (posts) {
            LoadResult.Loading -> item { LoadingBlock("正在同步论坛") }
            is LoadResult.Error -> item {
                ErrorBlock(
                    message = posts.message,
                    retryLabel = "重新同步",
                    onRetry = onRefresh
                )
            }
            is LoadResult.Success -> if (posts.value.isEmpty()) {
                item { StatusText("论坛暂时没有可显示的讨论") }
            }
            LoadResult.Idle -> Unit
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(forumFeedTabs()) { tab ->
                    FilterChip(
                        selected = tab == forumFeedTabs().first(),
                        onClick = {},
                        label = { Text(tab) }
                    )
                }
            }
        }
        items(feedItems) { item ->
            ForumFeedRow(item = item, onOpenPost = onOpenPost, onOpenUser = onOpenUser)
        }
    }
    
    androidx.compose.material3.FloatingActionButton(
        onClick = onCreatePost,
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "发布帖子")
    }
}
}

@Composable
private fun ForumStatsStrip(items: List<ForumFeedItem>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ForumStat(label = "主题", value = items.size.toString())
            ForumStat(label = "回复", value = items.sumOf { it.replyCount }.toString())
            ForumStat(label = "分区", value = forumFeedTabs().size.toString())
        }
    }
}

@Composable
private fun ForumStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ForumFeedRow(
    item: ForumFeedItem,
    onOpenPost: (Long) -> Unit,
    onOpenUser: (Long) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.id > 0) { onOpenPost(item.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (item.pinned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (item.pinned) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(42.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (item.pinned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.category.take(1),
                    fontWeight = FontWeight.Bold,
                    color = if (item.pinned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(forumFeedBadges(item)) { badge ->
                        CompactForumBadge(badge)
                    }
                }
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    forumFeedMetaLine(item),
                    modifier = Modifier.clickable(enabled = item.authorId != null) {
                        item.authorId?.let(onOpenUser)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(forumFeedMetricLabels(item)) { metric ->
                        Text(
                            metric,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.replyCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("回复", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompactForumBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ForumPostDetailScreen(
    state: ForumPostDetailState,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyComment: (ForumComment) -> Unit,
    onCancelReply: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onCommentLike: (Long) -> Unit,
    onCommentDislike: (Long) -> Unit,
    onCommentEmoji: (Long) -> Unit,
    onCommentAward: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenWeb: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (val detail = state.detail) {
            LoadResult.Idle, LoadResult.Loading -> item { LoadingBlock("正在打开帖子") }
            is LoadResult.Error -> item {
                ErrorBlock(message = detail.message, retryLabel = "重试帖子", onRetry = onRetry)
            }
            is LoadResult.Success -> {
                item {
                    ForumPostHeader(
                        detail = detail.value,
                        onLike = onLike,
                        onDislike = onDislike,
                        onEmoji = onEmoji,
                        onAward = onAward,
                        onOpenUser = onOpenUser,
                        onOpenWeb = onOpenWeb
                    )
                }
                item {
                    ForumPostBody(detail.value.content.orEmpty())
                }
            }
        }

        item {
            InlineCommentComposer(
                draft = state.commentDraft,
                replyingToName = state.replyingToName,
                loading = state.actionLoading,
                message = state.actionMessage,
                onDraftChange = onDraftChange,
                onSubmit = onSubmitComment,
                onCancelReply = onCancelReply
            )
        }

        item {
            Text("评论", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        when (val comments = state.comments) {
            LoadResult.Idle, LoadResult.Loading -> item { LoadingBlock("正在同步评论") }
            is LoadResult.Error -> item { ErrorBlock(message = comments.message, retryLabel = "重试评论", onRetry = onRetry) }
            is LoadResult.Success -> {
                if (comments.value.isEmpty()) {
                    item { StatusText("还没有评论") }
                } else {
                    val threads = forumCommentThreads(comments.value)
                    item {
                        Text(
                            forumCommentThreadSummary(threads),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(threads) { thread ->
                        ForumCommentThreadBlock(
                            thread = thread,
                            onLike = onCommentLike,
                            onDislike = onCommentDislike,
                            onEmoji = onCommentEmoji,
                            onAward = onCommentAward,
                            onReply = onReplyComment,
                            onOpenUser = onOpenUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumPostHeader(
    detail: ForumPostDetail,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenWeb: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(forumFeedBadges(forumPostFeedItem(detail.post))) { badge -> CompactForumBadge(badge) }
            }
            Text(detail.post.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                forumFeedMetaLine(forumPostFeedItem(detail.post)),
                modifier = Modifier.clickable(enabled = detail.post.authorId != null) {
                    detail.post.authorId?.let(onOpenUser)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ForumActionBar(
                likeCount = detail.likeCount ?: 0,
                dislikeCount = detail.dislikeCount ?: 0,
                reactionCount = detail.reactionCount ?: 0,
                awardPoints = detail.awardPoints ?: 0,
                onLike = onLike,
                onDislike = onDislike,
                onEmoji = onEmoji,
                onAward = onAward,
                onOpenWeb = onOpenWeb
            )
        }
    }
}

@Composable
private fun ForumActionBar(
    likeCount: Int,
    dislikeCount: Int,
    reactionCount: Int,
    awardPoints: Int,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onOpenWeb: (() -> Unit)? = null
) {
    val labels = forumActionBarLabels()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ForumActionIcon(Icons.Filled.ThumbUp, "${labels[0]} $likeCount", onLike)
        ForumActionIcon(Icons.Filled.ThumbDown, "${labels[1]} $dislikeCount", onDislike)
        ForumActionIcon(Icons.Filled.EmojiEmotions, "${labels[2]} $reactionCount", onEmoji)
        ForumActionIcon(Icons.Filled.CardGiftcard, "${labels[3]} $awardPoints", onAward)
        if (onOpenWeb != null) {
            ForumActionIcon(Icons.Filled.OpenInBrowser, labels[4], onOpenWeb)
        }
    }
}

@Composable
private fun ForumActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.width(20.dp).height(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ForumPostBody(content: String) {
    val paragraphs = readerParagraphsFromContent(content)
    val links = forumContentLinks(paragraphs)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (paragraphs.isEmpty()) {
                Text("正文暂时为空", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                paragraphs.forEach { paragraph ->
                    Text(paragraph, style = MaterialTheme.typography.bodyLarge)
                }
            }
            ForumLinkPreviewRows(links)
        }
    }
}

@Composable
private fun ForumLinkPreviewRows(links: List<String>) {
    if (links.isEmpty()) return
    Text("链接预览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    links.take(4).forEach { link ->
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(
                link,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ForumCommentComposer(
    draft: String,
    replyingToName: String?,
    loading: Boolean,
    message: String?,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancelReply: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            replyingToName?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("回复 $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onCancelReply) { Text("取消") }
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                label = { Text("写评论") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(enabled = !loading && draft.isNotBlank(), onClick = onSubmit) { Text(if (loading) "发送中" else "发送") }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun InlineCommentComposer(
    draft: String,
    replyingToName: String?,
    loading: Boolean,
    message: String?,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancelReply: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        replyingToName?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("回复 $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onCancelReply) { Text("取消") }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            label = { Text("写评论") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(enabled = !loading && draft.isNotBlank(), onClick = onSubmit) {
                Text(if (loading) "发送中" else "发送")
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ForumCommentThreadBlock(
    thread: ForumCommentThread,
    onLike: (Long) -> Unit,
    onDislike: (Long) -> Unit,
    onEmoji: (Long) -> Unit,
    onAward: (Long) -> Unit,
    onReply: (ForumComment) -> Unit,
    onOpenUser: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ForumCommentRow(
            comment = thread.comment,
            onLike = { onLike(thread.comment.id) },
            onDislike = { onDislike(thread.comment.id) },
            onEmoji = { onEmoji(thread.comment.id) },
            onAward = { onAward(thread.comment.id) },
            onReply = { onReply(thread.comment) },
            onOpenUser = onOpenUser
        )
        thread.replies.forEach { reply ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(86.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Box(Modifier.weight(1f)) {
                    ForumCommentRow(
                        comment = reply,
                        onLike = { onLike(reply.id) },
                        onDislike = { onDislike(reply.id) },
                        onEmoji = { onEmoji(reply.id) },
                        onAward = { onAward(reply.id) },
                        onReply = { onReply(reply) },
                        onOpenUser = onOpenUser
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumCommentRow(
    comment: ForumComment,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onReply: () -> Unit,
    onOpenUser: (Long) -> Unit
) {
    val paragraphs = readerParagraphsFromContent(comment.content).ifEmpty { listOf(comment.content) }
    val links = forumCommentLinkPreviews(comment)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    comment.authorName ?: "匿名用户",
                    modifier = Modifier.clickable(enabled = comment.authorId != null) {
                        comment.authorId?.let(onOpenUser)
                    },
                    fontWeight = FontWeight.SemiBold
                )
                comment.createdAt?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            comment.replyToName?.let {
                Text("回复 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            paragraphs.forEach { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyMedium)
            }
            ForumLinkPreviewRows(links)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ForumActionIcon(Icons.Filled.ThumbUp, "赞 ${comment.likeCount ?: 0}", onLike)
                ForumActionIcon(Icons.Filled.ThumbDown, "踩 ${comment.dislikeCount ?: 0}", onDislike)
                ForumActionIcon(Icons.Filled.EmojiEmotions, "表情 ${comment.reactionCount ?: 0}", onEmoji)
                ForumActionIcon(Icons.Filled.CardGiftcard, "打赏 ${comment.awardPoints ?: 0}", onAward)
                ForumActionIcon(Icons.AutoMirrored.Filled.Reply, "回复", onReply)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: HomeState,
    hasAuthToken: Boolean,
    readerProgress: ReaderProgress?,
    recentReaderProgresses: List<ReaderProgress>,
    bookshelfQuery: String,
    onRefresh: () -> Unit,
    onBookshelfQueryChange: (String) -> Unit,
    onFavoriteGroupSelected: (Long?) -> Unit,
    onOpenLogin: () -> Unit,
    onContinueReading: (ReaderProgress) -> Unit,
    onClearReaderProgress: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onLoadMoreFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val favoriteCount = when (val favorites = state.favorites) {
        is LoadResult.Success -> favorites.value.size
        else -> 0
    }
    val groupCount = when (val groups = state.groups) {
        is LoadResult.Success -> groups.value.size
        else -> 0
    }
    val overview = libraryOverview(
        hasAuthToken = hasAuthToken,
        favoriteCount = favoriteCount,
        groupCount = groupCount,
        recentCount = recentReaderProgresses.size
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LibraryOverviewBlock(
                overview = overview,
                onRefresh = onRefresh,
                onOpenSearch = onOpenSearch,
                onOpenLogin = onOpenLogin,
                onOpenWeb = onOpenWeb
            )
        }
        readerProgress?.let { progress ->
            item {
                ContinueReadingCard(
                    progress = progress,
                    onContinue = { onContinueReading(progress) },
                    onClear = onClearReaderProgress
                )
            }
        }
        val secondaryRecent = recentReaderProgresses.filterNot { it == readerProgress }
        if (secondaryRecent.isNotEmpty()) {
            item {
                RecentReadingSection(
                    progresses = secondaryRecent,
                    onContinueReading = onContinueReading
                )
            }
        }
        item {
            LibraryShelfControls(
                groups = state.groups,
                selectedGroupId = state.selectedFavoriteGroupId,
                value = bookshelfQuery,
                onValueChange = onBookshelfQueryChange,
                onGroupSelected = onFavoriteGroupSelected
            )
        }
        item { Text(libraryFavoritesTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        when (val favorites = state.favorites) {
            LoadResult.Idle -> item { StatusText("等待加载书架") }
            LoadResult.Loading -> item { LoadingBlock("正在加载收藏书籍") }
            is LoadResult.Error -> item { ErrorBlock(favorites.message, retryLabel = retryActionLabel("书架"), onRetry = onRefresh) }
            is LoadResult.Success -> {
                val visibleBooks = filterBooks(favorites.value, bookshelfQuery)
                when {
                    favorites.value.isEmpty() -> item {
                        EmptyCollectionState(
                            onOpenLogin = onOpenLogin,
                            onOpenWeb = onOpenWeb
                        )
                    }
                    visibleBooks.isEmpty() -> item { StatusText("没有匹配的收藏") }
                    else -> {
                        val columns = novelGridColumnCount()
                        items(visibleBooks.chunked(columns), key = { it.joinToString { b -> b.id.toString() } }) { rowBooks ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                for (book in rowBooks) {
                                    Box(Modifier.weight(1f)) {
                                        NovelCardItem(book = book, onClick = { onOpenBook(book.id) })
                                    }
                                }
                                for (i in 0 until (columns - rowBooks.size)) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                if (favorites.value.isNotEmpty()) {
                    item {
                        LoadMoreRow(
                            canLoadMore = state.favoritesCanLoadMore,
                            loading = state.favoritesLoadingMore,
                            onLoadMore = onLoadMoreFavorites,
                            idleText = "已显示 ${visibleBooks.size} 本",
                            loadText = "加载更多收藏"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    keyword: String,
    searchHistory: List<String>,
    options: SearchOptions,
    results: LoadResult<List<NovelCard>>,
    tags: LoadResult<List<NovelTag>>,
    onKeywordChange: (String) -> Unit,
    onUseSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onUseTag: (String) -> Unit,
    onRefreshTags: () -> Unit,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit,
    onSearch: (String?) -> Unit,
    searchCanLoadMore: Boolean,
    searchLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onOpenWeb: () -> Unit
) {
    val overview = discoverOverview(results)
    val sectionOrder = discoverSectionOrder(results, searchHistory.isNotEmpty())
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sectionOrder.forEach { section ->
            when (section) {
                DiscoverSection.SearchPanel -> item {
                    DiscoverSearchPanel(
                        overview = overview,
                        keyword = keyword,
                        options = options,
                        onKeywordChange = onKeywordChange,
                        onSearch = onSearch,
                        onOpenWeb = onOpenWeb
                    )
                }
                DiscoverSection.Results -> searchResultItems(
                    results = results,
                    searchCanLoadMore = searchCanLoadMore,
                    searchLoadingMore = searchLoadingMore,
                    onSearch = { onSearch(null) },
                    onLoadMore = onLoadMore,
                    onOpenBook = onOpenBook
                )
                DiscoverSection.History -> item {
                    SearchHistorySection(
                        history = searchHistory,
                        onUseKeyword = onUseSearchHistory,
                        onClear = onClearSearchHistory
                    )
                }
                DiscoverSection.Tags -> item {
                    SearchTagSection(
                        tags = tags,
                        onUseTag = onUseTag,
                        onRefresh = onRefreshTags
                    )
                }
                DiscoverSection.Filters -> item {
                    SearchOptionSection(
                        options = options,
                        onSortByChange = onSortByChange,
                        onSortOrderChange = onSortOrderChange,
                        onScopeChange = onScopeChange,
                        onMatchTypeChange = onMatchTypeChange,
                        onAdultFilterChange = onAdultFilterChange,
                        onSourceChange = onSourceChange,
                        onWordCountRangeChange = onWordCountRangeChange
                    )
                }
                DiscoverSection.IdlePrompts -> item {
                    DiscoverIdlePanel(
                        onUsePrompt = onKeywordChange,
                        onSearch = onSearch
                    )
                }
            }
        }
    }
}

private fun LazyListScope.searchResultItems(
    results: LoadResult<List<NovelCard>>,
    searchCanLoadMore: Boolean,
    searchLoadingMore: Boolean,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenBook: (Long) -> Unit
) {
    when (results) {
        LoadResult.Idle -> Unit
        LoadResult.Loading -> item { LoadingBlock("正在请求 NovalPie 搜索") }
        is LoadResult.Error -> item { ErrorBlock(results.message, retryLabel = retryActionLabel("搜索"), onRetry = onSearch) }
        is LoadResult.Success -> {
            if (results.value.isEmpty()) {
                item { StatusText("没有找到搜索结果") }
            } else {
                val columns = novelGridColumnCount()
                items(results.value.chunked(columns), key = { it.joinToString { b -> b.id.toString() } }) { rowBooks ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        for (book in rowBooks) {
                            Box(Modifier.weight(1f)) {
                                NovelCardItem(book = book, onClick = { onOpenBook(book.id) })
                            }
                        }
                        for (i in 0 until (columns - rowBooks.size)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item {
                    LoadMoreRow(
                        canLoadMore = searchCanLoadMore,
                        loading = searchLoadingMore,
                        onLoadMore = onLoadMore,
                        idleText = "已显示 ${results.value.size} 个结果",
                        loadText = "加载更多结果"
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchTagSection(
    tags: LoadResult<List<NovelTag>>,
    onUseTag: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("热门标签", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("刷新")
                }
            }
            when (tags) {
                LoadResult.Idle -> StatusText("打开发现页后同步网站标签")
                LoadResult.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is LoadResult.Error -> ErrorBlock(tags.message, retryLabel = "重试标签", onRetry = onRefresh)
                is LoadResult.Success -> {
                    if (tags.value.isEmpty()) {
                        StatusText("暂无可显示标签")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tags.value) { tag ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onUseTag(tag.name) },
                                    label = { Text(discoverTagLabels(listOf(tag)).single()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DiscoverIdlePanel(
    onUsePrompt: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(discoverIdleMessage(), style = MaterialTheme.typography.bodyMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(discoverQuickPrompts()) { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            onUsePrompt(prompt)
                            onSearch(prompt)
                        },
                        label = { Text(prompt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverEmptyResultPanel() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("没有匹配结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "可以换一个关键词，或调整范围、匹配方式和内容筛选。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverSearchPanel(
    overview: DiscoverOverview,
    keyword: String,
    options: SearchOptions,
    onKeywordChange: (String) -> Unit,
    onSearch: (String?) -> Unit,
    onOpenWeb: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val submitSearch = {
        focusManager.clearFocus()
        onSearch(keyword)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(overview.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    overview.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        overview.statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onOpenWeb) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = "打开网页搜索")
                }
            }
        }
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(overview.hint) },
            trailingIcon = {
                IconButton(onClick = submitSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.primary)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() })
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            discoverSelectedFilterSummaries(options).forEach { summary ->
                LibraryStatPill(summary)
            }
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onUseKeyword: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("搜索历史", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClear) { Text("清空") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it }) { keyword ->
                    OutlinedButton(onClick = { onUseKeyword(keyword) }) {
                        Text(keyword, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchOptionSection(
    options: SearchOptions,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("筛选", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                discoverFilterGroups(options).forEach { group ->
                    FilterChoiceRail(
                        group = group,
                        onSelected = when (group.label) {
                            "排序" -> onSortByChange
                            "顺序" -> onSortOrderChange
                            "范围" -> onScopeChange
                            "内容" -> onAdultFilterChange
                            "字数" -> onWordCountRangeChange
                            "来源" -> onSourceChange
                            else -> onMatchTypeChange
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChoiceRail(group: DiscoverFilterGroup, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(group.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(group.choices) { choice ->
                FilterChip(
                    selected = choice.selected,
                    onClick = { onSelected(choice.value) },
                    label = { Text(choice.label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceChips(label: String, selected: String, choices: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(choices, key = { it.first }) { choice ->
                FilterChip(
                    selected = selected == choice.first,
                    onClick = { onSelected(choice.first) },
                    label = { Text(choice.second) }
                )
            }
        }
    }
}

@Composable
private fun BookDetailScreen(
    state: BookDetailState,
    readerProgress: ReaderProgress?,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyComment: (ChapterComment) -> Unit,
    onCancelCommentReply: () -> Unit,
    onCommentLike: (ChapterComment) -> Unit,
    onCommentDislike: (ChapterComment) -> Unit,
    onCommentEmoji: (ChapterComment) -> Unit,
    onCommentAward: (ChapterComment) -> Unit,
    onOpenWeb: () -> Unit
) {
    val sectionTitles = bookDetailSectionTitles()
    val firstChapter = (state.chapters as? LoadResult.Success)?.value?.firstOrNull()
    val progressForBook = readerProgress?.takeIf { it.bookId == state.bookId }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            when (val book = state.book) {
                LoadResult.Idle -> StatusText("等待加载书籍详情")
                LoadResult.Loading -> LoadingBlock("正在加载书籍详情")
                is LoadResult.Error -> ErrorBlock(book.message, retryLabel = retryActionLabel("书籍详情"), onRetry = onRetry)
                is LoadResult.Success -> BookDetailHero(
                    book = book.value,
                    favoriteStatus = state.favoriteStatus,
                    progress = progressForBook,
                    firstChapter = firstChapter,
                    onOpenReader = onOpenReader,
                    onEditInfo = onEditInfo,
                    onManageChapters = onManageChapters,
                    onAppendChapters = onAppendChapters,
                    onOpenWeb = onOpenWeb
                )
            }
        }
        item { Text(sectionTitles[2], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { CatalogFilterField(catalogQuery, onCatalogQueryChange) }
        when (val chapters = state.chapters) {
            LoadResult.Idle -> item { StatusText("等待加载章节") }
            LoadResult.Loading -> item { LoadingBlock("正在加载章节目录") }
            is LoadResult.Error -> item { ErrorBlock(chapters.message, retryLabel = retryActionLabel("章节目录"), onRetry = onRetry) }
            is LoadResult.Success -> {
                val visible = filterChapters(chapters.value, catalogQuery)
                item {
                    CatalogSummaryText(
                        catalogSummaryLabel(
                            allChapters = chapters.value,
                            visibleChapters = visible,
                            currentChapterId = readerProgress?.takeIf { it.bookId == state.bookId }?.chapterId
                        )
                    )
                }
                when {
                    chapters.value.isEmpty() -> item { StatusText("章节目录为空，可打开网页详情。") }
                    visible.isEmpty() -> item { StatusText("没有匹配的章节") }
                    else -> items(visible, key = { it.id }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            selected = isBookDetailProgressChapter(state.bookId, chapter.id, readerProgress),
                            onClick = { onOpenReader(state.bookId, chapter.id) }
                        )
                    }
                }
            }
        }
        item {
            BookCommentsSection(
                title = sectionTitles[3],
                state = state,
                comments = state.comments,
                onRetry = onRetry,
                onDraftChange = onCommentDraftChange,
                onSubmit = onSubmitComment,
                onReply = onReplyComment,
                onCancelReply = onCancelCommentReply,
                onLike = onCommentLike,
                onDislike = onCommentDislike,
                onEmoji = onCommentEmoji,
                onAward = onCommentAward,
                onOpenWeb = onOpenWeb
            )
        }
    }
}

@Composable
private fun ReaderScreen(
    state: ReaderState,
    options: ReaderUiOptions,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onCycleTheme: () -> Unit,
    onRetry: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onBack: () -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyComment: (ChapterComment) -> Unit,
    onCancelCommentReply: () -> Unit,
    onCommentLike: (ChapterComment) -> Unit,
    onCommentDislike: (ChapterComment) -> Unit,
    onCommentEmoji: (ChapterComment) -> Unit,
    onCommentAward: (ChapterComment) -> Unit,
    onOpenWeb: () -> Unit
) {
    val chapters = (state.chapters as? LoadResult.Success)?.value.orEmpty()
    val catalogVisible = remember { mutableStateOf(false) }
    val toolbarsVisible = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { toolbarsVisible.value = !toolbarsVisible.value },
            contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                when (val content = state.content) {
                    LoadResult.Idle -> StatusText("等待加载正文")
                    LoadResult.Loading -> LoadingBlock("正在加载正文")
                    is LoadResult.Error -> ErrorBlock(content.message, retryLabel = retryActionLabel("正文"), onRetry = onRetry)
                    is LoadResult.Success -> ReaderBody(content.value, options)
                }
            }
            item {
                ReaderChapterCommentsSection(
                    state = state,
                    onRetry = onRetry,
                    onDraftChange = onCommentDraftChange,
                    onSubmit = onSubmitComment,
                    onReply = onReplyComment,
                    onCancelReply = onCancelCommentReply,
                    onLike = onCommentLike,
                    onDislike = onCommentDislike,
                    onEmoji = onCommentEmoji,
                    onAward = onCommentAward,
                    onOpenWeb = onOpenWeb
                )
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = toolbarsVisible.value,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopBar(
                state = state,
                chapters = chapters,
                onBack = onBack,
                onOpenWeb = onOpenWeb
            )
        }

        if (catalogVisible.value) {
            ReaderCatalogPanel(
                state = state,
                chapters = state.chapters,
                catalogQuery = catalogQuery,
                onCatalogQueryChange = onCatalogQueryChange,
                onRetry = onRetry,
                onOpenReader = { chapterId ->
                    catalogVisible.value = false
                    toolbarsVisible.value = false
                    onOpenReader(state.bookId, chapterId)
                },
                onClose = { catalogVisible.value = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = toolbarsVisible.value && !catalogVisible.value,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderToolbar(
                state = state,
                chapters = chapters,
                options = options,
                onDecreaseFont = onDecreaseFont,
                onIncreaseFont = onIncreaseFont,
                onCycleTheme = onCycleTheme,
                onOpenReader = onOpenReader,
                onOpenWeb = onOpenWeb,
                onOpenCatalog = { catalogVisible.value = true }
            )
        }
    }
}

@Composable
private fun ReaderCatalogPanel(
    state: ReaderState,
    chapters: LoadResult<List<Chapter>>,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenReader: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 86.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(readerCatalogPanelTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text(readerCloseCatalogLabel()) }
            }
            CatalogFilterField(catalogQuery, onCatalogQueryChange)
            when (chapters) {
                LoadResult.Idle -> StatusText("等待加载目录")
                LoadResult.Loading -> LoadingBlock("正在加载目录")
                is LoadResult.Error -> ErrorBlock(chapters.message, retryLabel = retryActionLabel("章节目录"), onRetry = onRetry)
                is LoadResult.Success -> {
                    val visible = filterChapters(chapters.value, catalogQuery)
                    CatalogSummaryText(
                        catalogSummaryLabel(
                            allChapters = chapters.value,
                            visibleChapters = visible,
                            currentChapterId = state.chapterId
                        )
                    )
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (visible.isEmpty()) {
                            item { StatusText("没有匹配的章节") }
                        } else {
                            items(visible, key = { it.id }) { chapter ->
                                ChapterRow(
                                    chapter = chapter,
                                    selected = chapter.id == state.chapterId,
                                    onClick = { onOpenReader(chapter.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    state: ReaderState,
    chapters: List<Chapter>,
    onBack: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val labels = readerTopBarLabels()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(labels.back) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(labels.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    readerChapterProgressLabel(state.chapterId, chapters),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onOpenWeb) { Text(labels.web) }
        }
    }
}

@Composable
private fun ReaderHeader(state: ReaderState, chapters: List<Chapter>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(readerScreenTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        readerDebugIdentityLine(state.bookId, state.chapterId)?.let {
            Text(it, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            readerChapterProgressLabel(state.chapterId, chapters),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReaderChapterCommentsSection(
    state: ReaderState,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReply: (ChapterComment) -> Unit,
    onCancelReply: () -> Unit,
    onLike: (ChapterComment) -> Unit,
    onDislike: (ChapterComment) -> Unit,
    onEmoji: (ChapterComment) -> Unit,
    onAward: (ChapterComment) -> Unit,
    onOpenWeb: () -> Unit
) {
    val comments = state.comments
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(chapterCommentsSectionTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onOpenWeb) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = chapterCommentsFallbackLabel(), modifier = Modifier.width(18.dp).height(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(chapterCommentsFallbackLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            InlineCommentComposer(
                draft = state.commentDraft,
                replyingToName = state.replyingToName,
                loading = state.actionLoading,
                message = state.actionMessage,
                onDraftChange = onDraftChange,
                onSubmit = onSubmit,
                onCancelReply = onCancelReply
            )
            when (comments) {
                LoadResult.Idle -> StatusText("等待加载章节评论")
                LoadResult.Loading -> LoadingBlock("正在同步章节评论")
                is LoadResult.Error -> ErrorBlock(comments.message, retryLabel = retryActionLabel("章节评论"), onRetry = onRetry)
                is LoadResult.Success -> {
                    if (comments.value.isEmpty()) {
                        StatusText("还没有章节评论")
                    } else {
                        comments.value.forEach { comment ->
                            ReaderChapterCommentRow(
                                comment = comment,
                                onLike = { onLike(comment) },
                                onDislike = { onDislike(comment) },
                                onEmoji = { onEmoji(comment) },
                                onAward = { onAward(comment) },
                                onReply = { onReply(comment) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderChapterCommentRow(
    comment: ChapterComment,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onReply: () -> Unit
) {
    val paragraphs = readerParagraphsFromContent(comment.content).ifEmpty { listOf(comment.content) }
    val links = forumContentLinks(paragraphs)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(comment.authorName ?: "匿名用户", fontWeight = FontWeight.SemiBold)
                comment.createdAt?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            comment.replyToName?.let {
                Text("回复 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            paragraphs.forEach { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyMedium)
            }
            ForumLinkPreviewRows(links)
            ChapterCommentActionRow(
                comment = comment,
                onLike = onLike,
                onDislike = onDislike,
                onEmoji = onEmoji,
                onAward = onAward,
                onReply = onReply
            )
        }
    }
}

@Composable
private fun ChapterCommentActionRow(
    comment: ChapterComment,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onReply: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ForumActionIcon(Icons.Filled.ThumbUp, "赞 ${comment.likeCount ?: 0}", onLike) }
        item { ForumActionIcon(Icons.Filled.ThumbDown, "踩 ${comment.dislikeCount ?: 0}", onDislike) }
        item { ForumActionIcon(Icons.Filled.EmojiEmotions, "表情 ${comment.reactionCount ?: 0}", onEmoji) }
        item { ForumActionIcon(Icons.Filled.CardGiftcard, "打赏 ${comment.awardPoints ?: 0}", onAward) }
        item { ForumActionIcon(Icons.AutoMirrored.Filled.Reply, "回复", onReply) }
    }
}

@Composable
private fun ReaderToolbar(
    state: ReaderState,
    chapters: List<Chapter>,
    options: ReaderUiOptions,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onOpenWeb: () -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adjacent = adjacentReaderChapters(state.chapterId, chapters)
    val previous = adjacent.previous
    val next = adjacent.next
    val labels = readerToolbarLabels()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { TextButton(enabled = previous != null, onClick = { previous?.let { onOpenReader(state.bookId, it.id) } }) { Text(labels[0]) } }
            item { TextButton(onClick = onOpenCatalog) { Text(labels[1]) } }
            item { TextButton(enabled = next != null, onClick = { next?.let { onOpenReader(state.bookId, it.id) } }) { Text(labels[2]) } }
            item { Spacer(Modifier.width(8.dp)) }
            item { TextButton(onClick = onDecreaseFont) { Text(labels[3]) } }
            item { Text("${options.fontSizeSp}sp", style = MaterialTheme.typography.labelLarge) }
            item { TextButton(onClick = onIncreaseFont) { Text(labels[4]) } }
            item { Spacer(Modifier.width(8.dp)) }
            item { TextButton(onClick = onCycleTheme) { Text(options.theme.themeLabel()) } }
            item { TextButton(onClick = onOpenWeb) { Text(labels[6]) } }
        }
    }
}

@Composable
private fun ToolsScreen(
    state: ToolsState,
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenMessage: (SiteMessage) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    val profile = (user as? LoadResult.Success)?.value
    val isAdmin = isAdminProfile(profile)
    val entries = toolsEntries(isAdmin)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("\u529f\u80fd\u4e2d\u5fc3", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "\u6d88\u606f\u3001\u5de5\u4f5c\u533a\u4e0e\u7f51\u7ad9\u7ba1\u7406\u5165\u53e3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("\u5237\u65b0")
                }
            }
        }

        if (!hasAuthToken) {
            item { ToolsLoginPrompt(onOpenLogin) }
        }

        item { ToolsMessageStats(state.stats) }
        item {
            Text("\u6700\u8fd1\u6d88\u606f", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        when (val messages = state.messages) {
            LoadResult.Idle -> item { StatusText("\u6253\u5f00\u529f\u80fd\u4e2d\u5fc3\u540e\u540c\u6b65\u6d88\u606f") }
            LoadResult.Loading -> item { LoadingBlock("\u6b63\u5728\u540c\u6b65\u6d88\u606f") }
            is LoadResult.Error -> item {
                ErrorBlock(messages.message, retryLabel = "\u91cd\u8bd5\u6d88\u606f", onRetry = onRefresh)
            }
            is LoadResult.Success -> {
                if (messages.value.isEmpty()) {
                    item { StatusText("\u6682\u65e0\u6d88\u606f") }
                } else {
                    items(messages.value.take(6), key = { it.id }) { message ->
                        ToolsMessageRow(message = message, onOpenMessage = onOpenMessage)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onOpenMessages, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Forum, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("\u6253\u5f00\u5b8c\u6574\u6d88\u606f\u4e2d\u5fc3")
            }
        }

        item {
            Text("\u7f51\u7ad9\u529f\u80fd", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(entries.chunked(2), key = { row -> row.joinToString("|") { it.path } }) { rowEntries ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowEntries.forEach { entry ->
                    ToolRouteCard(
                        entry = entry,
                        onClick = { if (entry.path == "/messages") onOpenMessages() else onOpenRoute(entry.path) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowEntries.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ToolsLoginPrompt(onOpenLogin: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("\u767b\u5f55\u540e\u540c\u6b65", fontWeight = FontWeight.Bold)
                Text(
                    "\u6d88\u606f\u3001\u5de5\u4f5c\u533a\u548c\u7ba1\u7406\u529f\u80fd\u9700\u8981\u7f51\u7ad9\u8d26\u53f7",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onOpenLogin) { Text("\u767b\u5f55") }
        }
    }
}

@Composable
private fun ToolsMessageStats(stats: LoadResult<MessageStats>) {
    when (stats) {
        LoadResult.Idle -> StatusText("\u7b49\u5f85\u540c\u6b65\u6d88\u606f\u7edf\u8ba1")
        LoadResult.Loading -> LoadingBlock("\u6b63\u5728\u540c\u6b65\u6d88\u606f\u7edf\u8ba1")
        is LoadResult.Error -> ErrorBlock(stats.message)
        is LoadResult.Success -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { LibraryStatPill("\u672a\u8bfb ${stats.value.unreadCount}") }
            item { LibraryStatPill("\u5168\u90e8 ${stats.value.totalCount}") }
            item { LibraryStatPill("\u91cd\u8981 ${stats.value.importantCount}") }
            item { LibraryStatPill("7\u65e5 ${stats.value.recentSevenDaysCount}") }
            item { LibraryStatPill("\u661f\u6807 ${stats.value.starredCount}") }
        }
    }
}

@Composable
private fun ToolsMessageRow(message: SiteMessage, onOpenMessage: (SiteMessage) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onOpenMessage(message) },
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (message.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (message.isRead) FontWeight.Medium else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    messageTypeLabel(message.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            message.content?.takeIf { it.isNotBlank() }?.let { content ->
                Text(
                    content.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(message.username.orEmpty(), style = MaterialTheme.typography.labelSmall)
                Text(
                    message.createdAt?.replace('T', ' ')?.take(16).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToolRouteCard(entry: ToolEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(toolEntryIcon(entry.path), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                entry.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun toolEntryIcon(path: String): ImageVector = when (path) {
    "/messages" -> Icons.Filled.Forum
    "/workspace" -> Icons.Filled.GridView
    "/upload" -> Icons.Filled.OpenInBrowser
    "/upload-editor" -> Icons.AutoMirrored.Filled.MenuBook
    "/political-exam" -> Icons.Filled.CardGiftcard
    else -> Icons.Filled.Tune
}

@Composable
private fun SettingsScreen(
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    readerProgress: ReaderProgress?,
    readerOptions: ReaderUiOptions,
    proxyEnabled: Boolean,
    proxyHost: String,
    proxyPort: String,
    proxySummary: String,
    onRefreshAccount: () -> Unit,
    onOpenLogin: () -> Unit,
    onClearToken: () -> Unit,
    onProxyEnabledChange: (Boolean) -> Unit,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (String) -> Unit,
    onSaveProxy: () -> Unit,
    onOpenHomeFallback: () -> Unit,
    onOpenSearchFallback: () -> Unit
) {
    val overview = profileOverview(
        user = user,
        hasAuthToken = hasAuthToken,
        readerProgress = readerProgress,
        readerOptions = readerOptions,
        proxyEnabled = proxyEnabled
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProfileOverviewBlock(overview)
        }
        item { ProfileAccountCard(user, hasAuthToken, onRefreshAccount, onOpenLogin, onClearToken) }
        item { ProfileReaderCard(readerProgress, readerOptions) }
        item { ProfileConnectionCard(proxyEnabled, proxyHost, proxyPort, proxySummary, onProxyEnabledChange, onProxyHostChange, onProxyPortChange, onSaveProxy) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val actions = profileWebActions()
                item { Button(onClick = onOpenHomeFallback) { Text(actions[0]) } }
                item { OutlinedButton(onClick = onOpenSearchFallback) { Text(actions[1]) } }
            }
        }
    }
}

@Composable
private fun ProfileOverviewBlock(overview: ProfileOverview) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(overview.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        overview.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        overview.syncLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(overview.accountName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(overview.roleLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(overview.stats) { stat ->
                    LibraryStatPill(stat)
                }
            }
        }
    }
}

@Composable
private fun ProfileAccountCard(
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    onRefreshAccount: () -> Unit,
    onOpenLogin: () -> Unit,
    onClearToken: () -> Unit
) {
    val actions = profileAccountActions(hasAuthToken)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(profileSectionTitles()[0], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(accountSyncSummary(hasAuthToken))
            when (user) {
                LoadResult.Idle -> Text("等待同步账号")
                LoadResult.Loading -> LoadingBlock("正在同步账号")
                is LoadResult.Error -> Text(user.message, style = MaterialTheme.typography.bodySmall)
                is LoadResult.Success -> {
                    Text(user.value.name, fontWeight = FontWeight.Bold)
                    Text(if (isAdminProfile(user.value)) "管理员" else "普通用户")
                    val accountStatus = profileAccountStatusLabels(user.value)
                    if (accountStatus.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accountStatus) { status -> LibraryStatPill(status) }
                        }
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Button(onClick = onRefreshAccount) { Text(actions[0]) } }
                item { OutlinedButton(onClick = onOpenLogin) { Text(actions[1]) } }
                if (hasAuthToken) item { OutlinedButton(onClick = onClearToken) { Text(actions[2]) } }
            }
        }
    }
}

@Composable
private fun ProfileReaderCard(readerProgress: ReaderProgress?, readerOptions: ReaderUiOptions) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profileSectionTitles()[1], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("字号: ${readerOptions.fontSizeSp}sp")
            Text("主题: ${readerOptions.theme.themeLabel()}")
            if (readerProgress == null) {
                Text("进度: 无")
            } else {
                Text("进度: 章节 ${readerProgress.chapterId}")
                readerProgress.chapterTitle?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun ProfileConnectionCard(
    proxyEnabled: Boolean,
    proxyHost: String,
    proxyPort: String,
    proxySummary: String,
    onProxyEnabledChange: (Boolean) -> Unit,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (String) -> Unit,
    onSaveProxy: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(profileSectionTitles()[2], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("当前: $proxySummary", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = proxyEnabled, onCheckedChange = onProxyEnabledChange)
            }
            OutlinedTextField(proxyHost, onProxyHostChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("连接主机") })
            OutlinedTextField(proxyPort, onProxyPortChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("连接端口") })
            Text(
                "模拟器访问受限时可使用本机连接设置，保存后重新同步页面。",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onSaveProxy) { Text("保存连接") }
        }
    }
}

@Composable
private fun ReaderProgressHint(progress: ReaderProgress) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("本书阅读进度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("章节 ${progress.chapterId}")
            progress.chapterTitle?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun UserSection(user: LoadResult<UserProfile>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("账号状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (user) {
                LoadResult.Idle -> StatusText("等待检查登录状态")
                LoadResult.Loading -> LoadingBlock("正在检查 /api/users/me")
                is LoadResult.Error -> Text("${user.message}\n未拿到网站 Cookie 或 auth token 时这里会失败。", style = MaterialTheme.typography.bodySmall)
                is LoadResult.Success -> {
                    Text(user.value.name, fontWeight = FontWeight.Bold)
                    Text("role: ${user.value.role ?: "unknown"}")
                }
            }
        }
    }
}

@Composable
private fun LibraryOverviewBlock(
    overview: LibraryOverview,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenWeb: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(overview.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    overview.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        overview.syncLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "同步书架", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onOpenWeb) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = "打开网页收藏")
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSearch),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "搜索小说、作者或标签",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(Icons.Filled.Search, contentDescription = "进入搜索", tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (overview.syncLabel == "未同步") {
            TextButton(onClick = onOpenLogin, modifier = Modifier.align(Alignment.Start)) {
                Text("登录后同步收藏")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                overview.stats.take(3).forEachIndexed { index, stat ->
                    LibraryMetricCell(
                        modifier = Modifier.weight(1f),
                        label = when (index) {
                            0 -> "收藏"
                            1 -> "分组"
                            else -> "最近"
                        },
                        value = stat.filter(Char::isDigit)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryMetricCell(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value.ifBlank { "0" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryShelfControls(
    groups: LoadResult<List<FavoriteGroup>>,
    selectedGroupId: Long?,
    value: String,
    onValueChange: (String) -> Unit,
    onGroupSelected: (Long?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("分组与筛选", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            when (groups) {
                LoadResult.Idle -> StatusText("等待加载分组")
                LoadResult.Loading -> LoadingBlock("正在加载收藏分组")
                is LoadResult.Error -> Text(groups.message, style = MaterialTheme.typography.bodySmall)
                is LoadResult.Success -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedGroupId == null,
                                onClick = { onGroupSelected(null) },
                                label = { Text("全部") }
                            )
                        }
                        items(groups.value.take(8)) { group ->
                            FilterChip(
                                selected = group.id != null && selectedGroupId == group.id,
                                enabled = group.id != null,
                                onClick = { onGroupSelected(group.id) },
                                label = { Text("${group.name}${group.count?.let { " $it" } ?: ""}") }
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("筛选书架") }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GroupSection(
    groups: LoadResult<List<FavoriteGroup>>,
    selectedGroupId: Long?,
    onGroupSelected: (Long?) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("收藏分组", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (groups) {
                LoadResult.Idle -> StatusText("等待加载分组")
                LoadResult.Loading -> LoadingBlock("正在加载收藏分组")
                is LoadResult.Error -> Text(groups.message, style = MaterialTheme.typography.bodySmall)
                is LoadResult.Success -> {
                    if (groups.value.isEmpty()) {
                        StatusText("暂无分组")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedGroupId == null,
                                    onClick = { onGroupSelected(null) },
                                    label = { Text("全部") }
                                )
                            }
                            items(groups.value.take(8)) { group ->
                                FilterChip(
                                    selected = group.id != null && selectedGroupId == group.id,
                                    enabled = group.id != null,
                                    onClick = { onGroupSelected(group.id) },
                                    label = { Text("${group.name}${group.count?.let { " $it" } ?: ""}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(progress: ReaderProgress, onContinue: () -> Unit, onClear: () -> Unit) {
    val actions = libraryContinueActions()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(24.dp).width(24.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(libraryContinueTitle(hasProgress = true), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("章节 ${progress.chapterId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                progress.chapterTitle?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Button(onClick = onContinue) { Text(actions[0]) }
            TextButton(onClick = onClear) { Text(actions[1]) }
        }
    }
}

@Composable
private fun RecentReadingSection(
    progresses: List<ReaderProgress>,
    onContinueReading: (ReaderProgress) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(libraryContinueTitle(hasProgress = false), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(progresses.take(8)) { progress ->
                ElevatedCard(
                    modifier = Modifier
                        .width(196.dp)
                        .clickable { onContinueReading(progress) }
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("章节 ${progress.chapterId}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            progress.chapterTitle ?: "继续上次阅读",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHeaderBlock(header: ProductHeader) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(header.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            header.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String, semanticsMarker: String? = null) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (semanticsMarker == null) {
                    Modifier
                } else {
                    Modifier.semantics { contentDescription = semanticsMarker }
                }
            )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SearchResultHeader(results: LoadResult<List<NovelCard>>) {
    val text = when (results) {
        LoadResult.Idle -> "就绪"
        LoadResult.Loading -> "加载中"
        is LoadResult.Error -> "错误"
        is LoadResult.Success -> "${results.value.size} 个结果"
    }
    Text("搜索状态: $text", style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun LoadMoreRow(
    canLoadMore: Boolean,
    loading: Boolean,
    onLoadMore: () -> Unit,
    idleText: String,
    loadText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("正在加载更多", style = MaterialTheme.typography.bodySmall)
        } else if (canLoadMore) {
            OutlinedButton(onClick = onLoadMore) { Text(loadText) }
        } else {
            Text(idleText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun NovelCardItem(book: NovelCard, onClick: () -> Unit) {
    val preview = novelSearchPreview(book)
    val displayCoverUrl = novelDisplayCoverUrl(book)
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(bookCoverAspectRatio())) {
            BookCover(book.title, displayCoverUrl, previewUrl = displayCoverUrl)
        }
        Text(
            book.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            preview.authorLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        preview.originalTitleLabel?.let { originalTitle ->
            Text(
                originalTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (preview.platformLabel != null || preview.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                preview.platformLabel?.let { platform -> NovelSourcePill(platform) }
                preview.tags.forEach { tag -> NovelTagPill(tag) }
            }
        }
        if (preview.facts.isNotEmpty()) {
            Text(
                preview.facts.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NovelTagPill(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NovelSourcePill(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FavoriteStatusCard(status: LoadResult<FavoriteStatus>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("收藏状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (status) {
                LoadResult.Idle -> StatusText("等待读取收藏状态")
                LoadResult.Loading -> LoadingBlock("正在检查 /api/favorites/status")
                is LoadResult.Error -> Text(status.message, style = MaterialTheme.typography.bodySmall)
                is LoadResult.Success -> {
                    Text(if (status.value.isFavorited) "当前账号已收藏" else "当前账号未收藏", fontWeight = FontWeight.Bold)
                    status.value.groupId?.let { Text("分组 id: $it", style = MaterialTheme.typography.bodySmall) }
                    status.value.rawState?.let { Text("原始状态: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BookDetailHero(
    book: NovelCard,
    favoriteStatus: LoadResult<FavoriteStatus>,
    progress: ReaderProgress?,
    firstChapter: Chapter?,
    onOpenReader: (Long, Long) -> Unit,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val displayCoverUrl = novelDisplayCoverUrl(book)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookCover(book.title, displayCoverUrl, 100.dp, 150.dp, previewUrl = displayCoverUrl)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    novelOriginalTitleLabel(book)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BookDetailFavoriteChip(favoriteStatus)
                    progress?.chapterTitle?.takeIf { it.isNotBlank() }?.let {
                        Text("上次读到: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val facts = bookDetailFacts(book)
            if (facts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    facts.forEach { fact ->
                        BookDetailFactLabel(fact)
                    }
                }
            }
            if (book.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    book.tags.forEach { tag ->
                        NovelTagPill(tag)
                    }
                }
            }
            book.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BookDetailActionRow(
                bookId = book.id,
                progress = progress,
                firstChapter = firstChapter,
                onOpenReader = onOpenReader,
                onEditInfo = onEditInfo,
                onManageChapters = onManageChapters,
                onAppendChapters = onAppendChapters,
                onOpenWeb = onOpenWeb
            )
        }
    }
}

@Composable
private fun BookDetailFavoriteChip(status: LoadResult<FavoriteStatus>) {
    val label = when (status) {
        LoadResult.Idle, LoadResult.Loading -> bookDetailFavoriteLoadingLabel()
        is LoadResult.Error -> bookDetailFavoriteUnavailableLabel()
        is LoadResult.Success -> bookDetailFavoriteLabel(status.value.isFavorited)
    }
    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BookDetailFactLabel(label: String) {
    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookDetailActionRow(
    bookId: Long,
    progress: ReaderProgress?,
    firstChapter: Chapter?,
    onOpenReader: (Long, Long) -> Unit,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val actions = bookDetailPrimaryActions(hasProgress = progress != null)
    val continueLabel = actions.firstOrNull()
    val startLabel = if (progress != null) actions.getOrElse(1) { "开始阅读" } else actions.first()
    val webLabel = actions.last()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (progress != null) {
            Button(onClick = { onOpenReader(progress.bookId, progress.chapterId) }) {
                Text(continueLabel ?: "继续阅读")
            }
        }
        Button(
            enabled = firstChapter != null,
            onClick = { firstChapter?.let { onOpenReader(bookId, it.id) } }
        ) { Text(startLabel) }
        OutlinedButton(onClick = onOpenWeb) { Text(webLabel) }
        OutlinedButton(onClick = onEditInfo) { Text("编辑信息") }
        OutlinedButton(onClick = onManageChapters) { Text("章节管理") }
        OutlinedButton(onClick = onAppendChapters) { Text("追加章节") }
    }
}

@Composable
private fun BookCommentsSection(
    title: String,
    state: BookDetailState,
    comments: LoadResult<List<ChapterComment>>,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReply: (ChapterComment) -> Unit,
    onCancelReply: () -> Unit,
    onLike: (ChapterComment) -> Unit,
    onDislike: (ChapterComment) -> Unit,
    onEmoji: (ChapterComment) -> Unit,
    onAward: (ChapterComment) -> Unit,
    onOpenWeb: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title.ifBlank { bookCommentsSectionTitle() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onOpenWeb) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = bookCommentsFallbackLabel(), modifier = Modifier.width(18.dp).height(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(bookCommentsFallbackLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            ForumCommentComposer(
                draft = state.commentDraft,
                replyingToName = state.replyingToName,
                loading = state.actionLoading,
                message = state.actionMessage,
                onDraftChange = onDraftChange,
                onSubmit = onSubmit,
                onCancelReply = onCancelReply
            )
            when (comments) {
                LoadResult.Idle -> StatusText("等待加载评论")
                LoadResult.Loading -> LoadingBlock("正在同步评论")
                is LoadResult.Error -> ErrorBlock(comments.message, retryLabel = retryActionLabel("评论区"), onRetry = onRetry)
                is LoadResult.Success -> {
                    if (comments.value.isEmpty()) {
                        StatusText("还没有评论")
                    } else {
                        comments.value.take(6).forEach { comment ->
                            BookCommentRow(
                                comment = comment,
                                onLike = { onLike(comment) },
                                onDislike = { onDislike(comment) },
                                onEmoji = { onEmoji(comment) },
                                onAward = { onAward(comment) },
                                onReply = { onReply(comment) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCommentRow(
    comment: ChapterComment,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onReply: () -> Unit
) {
    val paragraphs = readerParagraphsFromContent(comment.content).ifEmpty { listOf(comment.content) }
    val links = forumContentLinks(paragraphs)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(comment.authorName ?: "匿名用户", fontWeight = FontWeight.SemiBold)
                comment.createdAt?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            comment.replyToName?.let {
                Text("回复 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            paragraphs.forEach { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyMedium)
            }
            ForumLinkPreviewRows(links)
            ChapterCommentActionRow(
                comment = comment,
                onLike = onLike,
                onDislike = onDislike,
                onEmoji = onEmoji,
                onAward = onAward,
                onReply = onReply
            )
        }
    }
}

@Composable
private fun BookSummary(book: NovelCard) {
    val displayCoverUrl = novelDisplayCoverUrl(book)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookCover(book.title, displayCoverUrl, 104.dp, 148.dp, previewUrl = displayCoverUrl)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val facts = bookDetailFacts(book)
                if (facts.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(facts) { fact -> AssistChip(onClick = {}, label = { Text(fact) }) }
                    }
                }
                book.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
                if (book.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(book.tags.take(8)) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCover(
    title: String,
    coverUrl: String?,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    modifier: Modifier = Modifier,
    previewUrl: String? = coverUrl
) {
    val shape = RoundedCornerShape(6.dp)
    val fallbackText = bookCoverFallbackText(title)
    var previewVisible by remember(previewUrl) { mutableStateOf(false) }
    var boxModifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
    if (width != Dp.Unspecified) boxModifier = boxModifier.width(width)
    if (height != Dp.Unspecified) boxModifier = boxModifier.height(height)
    if (width == Dp.Unspecified && height == Dp.Unspecified) boxModifier = boxModifier.fillMaxSize()
    if (!previewUrl.isNullOrBlank()) {
        boxModifier = boxModifier.combinedClickable(
            onClick = { previewVisible = true },
            onLongClick = { previewVisible = true }
        )
    }
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNullOrBlank()) {
            BookCoverFallbackText(fallbackText)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .size(1024, 1536)
                    .precision(Precision.EXACT)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { BookCoverFallbackText(fallbackText) },
                error = { BookCoverFallbackText(fallbackText) }
            )
        }
    }
    if (previewVisible && !previewUrl.isNullOrBlank()) {
        ImagePreviewDialog(imageUrl = previewUrl, title = "$title · 封面", onDismiss = { previewVisible = false })
    }
}

@Composable
private fun BookCoverFallbackText(value: String) {
    Text(
        value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

internal fun bookCoverFallbackText(title: String): String =
    title.trim().firstOrNull()?.toString() ?: "N"

internal fun bookCoverAspectRatio(): Float = 2f / 3f

internal fun novelGridColumnCount(): Int = 2

@Composable
private fun ChapterRow(chapter: Chapter, selected: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(chapter.number?.let { "#$it" } ?: "CH", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(chapter.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                chapter.wordCount?.let { Text("$it 字", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun CatalogSummaryText(value: String) {
    Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CatalogFilterField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("筛选目录") }
    )
}

@Composable
private fun ReaderBody(content: ReaderContent, options: ReaderUiOptions) {
    val palette = readerPalette(options.theme)
    val imagePlaceholders = remember(content.illustrations) {
        readerImagePlaceholdersFromIllustrations(content.illustrations)
    }
    val blocks = remember(content.content, imagePlaceholders) {
        readerBlocksFromContent(content.content, imagePlaceholders = imagePlaceholders)
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = palette.background)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content.title?.let {
                SelectionContainer {
                    Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.text)
                }
            }
            var imageOrdinal = 0
            blocks.forEach { block ->
                when (block) {
                    is ReaderContentBlock.Text -> SelectionContainer {
                        Text(
                            block.value,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = options.fontSizeSp.sp,
                            lineHeight = (options.fontSizeSp + 8).sp,
                            color = palette.text
                        )
                    }
                    is ReaderContentBlock.Image -> ReaderIllustration(
                        image = block,
                        ordinal = ++imageOrdinal,
                        palette = palette
                    )
                }
            }
            readerSourceDebugLine(content.source)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = palette.meta)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderIllustration(
    image: ReaderContentBlock.Image,
    ordinal: Int,
    palette: ReaderPalette
) {
    var previewVisible by remember(image.url) { mutableStateOf(false) }
    val label = readerIllustrationLabel(image.alt, ordinal)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { previewVisible = true }, onLongClick = { previewVisible = true }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = palette.background)
    ) {
        Column {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.url)
                    .size(2048, 2048)
                    .precision(Precision.EXACT)
                    .crossfade(true)
                    .build(),
                contentDescription = readerIllustrationContentDescription(label),
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 720.dp),
                contentScale = ContentScale.Fit,
                loading = { LoadingBlock(readerIllustrationLoadingLabel()) },
                error = { ErrorBlock(readerIllustrationErrorLabel()) }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = palette.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(readerIllustrationPreviewHint(), style = MaterialTheme.typography.labelSmall, color = palette.meta)
            }
        }
    }
    if (previewVisible) {
        ImagePreviewDialog(imageUrl = image.url, title = label, onDismiss = { previewVisible = false })
    }
}

internal fun readerIllustrationLabel(alt: String?, ordinal: Int): String =
    alt?.trim()?.takeIf { it.isNotBlank() } ?: "正文插图 ${ordinal.coerceAtLeast(1)}"

internal fun readerIllustrationContentDescription(label: String): String =
    "$label，点击或长按查看大图"

internal fun readerIllustrationPreviewHint(): String = "点击 / 长按看大图"

internal fun readerIllustrationLoadingLabel(): String = "正在加载插图"

internal fun readerIllustrationErrorLabel(): String = "插图加载失败"

private data class ReaderPalette(val background: Color, val text: Color, val meta: Color)

@Composable
private fun readerPalette(theme: String): ReaderPalette = when (theme) {
    "sepia" -> ReaderPalette(Color(0xFFF4ECD8), Color(0xFF30271B), Color(0xFF76634B))
    "dark" -> ReaderPalette(Color(0xFF111111), Color(0xFFECECEC), Color(0xFFAAAAAA))
    else -> ReaderPalette(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant)
}

internal fun String.themeLabel(): String = when (this) {
    "sepia" -> "护眼"
    "dark" -> "深色"
    else -> "系统"
}

@Composable
internal fun LoadingBlock(message: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        StatusText(message)
    }
}

@Composable
internal fun ErrorBlock(
    message: String,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                OutlinedButton(onClick = onRetry) {
                    Text(retryLabel ?: retryActionLabel(""))
                }
            }
        }
    }
}

@Composable
private fun StatusText(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LibraryStatPill(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyCollectionState(
    onOpenLogin: () -> Unit,
    onOpenWeb: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("暂无收藏", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "登录后同步网页收藏，或先打开网页确认账号状态。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Button(onClick = onOpenLogin) { Text("网页登录") } }
                item { OutlinedButton(onClick = onOpenWeb) { Text("打开网页") } }
            }
        }
    }
}

private fun filterBooks(books: List<NovelCard>, query: String): List<NovelCard> {
    val normalized = query.trim()
    if (normalized.isBlank()) return books
    return books.filter { book -> bookMatchesQuery(book, normalized) }
}

private fun filterChapters(chapters: List<Chapter>, query: String): List<Chapter> {
    val normalized = query.trim()
    if (normalized.isBlank()) return chapters
    return chapters.filter { chapter -> chapterMatchesQuery(chapter, normalized) }
}

