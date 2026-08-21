package com.novalpie.nativeapp.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.novalpie.nativeapp.data.NovelCoverLoadPriority
import com.novalpie.nativeapp.data.NovelCoverRequestSize
import com.novalpie.nativeapp.data.novalPieBookCoverRequest
import com.novalpie.nativeapp.data.novalPieBookCoverTargetSize
import com.novalpie.nativeapp.data.novalPieStaticImageRequest
import com.novalpie.nativeapp.data.preloadNovalPieBookCovers
import com.novalpie.nativeapp.ui.design.NovalPieRadius
import com.novalpie.nativeapp.ui.design.NovalPieElevation
import com.novalpie.nativeapp.ui.design.NovalPieSize
import com.novalpie.nativeapp.ui.design.NovalPieSpacing
import com.novalpie.nativeapp.ui.design.NpBookRowSkeleton
import com.novalpie.nativeapp.ui.design.NpCard
import com.novalpie.nativeapp.ui.design.NpChip
import com.novalpie.nativeapp.ui.design.NpChipRow
import com.novalpie.nativeapp.ui.design.NpChipTone
import com.novalpie.nativeapp.ui.design.NpEmptyState
import com.novalpie.nativeapp.ui.design.NpErrorState
import com.novalpie.nativeapp.ui.design.NpSearchField
import com.novalpie.nativeapp.ui.design.NpSectionHeader
import com.novalpie.nativeapp.ui.design.NpSkeleton
import com.novalpie.nativeapp.ui.design.novalPieReaderPalette
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.AppThemeMode
import com.novalpie.nativeapp.model.ChapterComment
import com.novalpie.nativeapp.model.FavoriteEntry
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
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.ReaderChapterCacheState
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.SearchPage
import com.novalpie.nativeapp.model.SiteMessage
import com.novalpie.nativeapp.model.UserBadge
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.data.ReaderTtsSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FORUM_LINK_ANNOTATION = "forum_link"
private const val FORUM_SPOILER_ANNOTATION = "forum_spoiler"
private val LocalReaderAccent = compositionLocalOf { Color(0xFF2563EB) }
// One display preference must govern every native surface that renders forum-style content.
private val LocalForumHideSpoilers = compositionLocalOf { true }
private const val SOURCE_SEARCH_SUBMISSION_SETTLE_MILLIS = 96L
// A short debounce avoids wasting bandwidth while a fling is still moving, without leaving the
// next row blank for the old 650 ms after the user stops scrolling.
private const val SEARCH_COVER_PRELOAD_SETTLE_MILLIS = 160L

/** Restore a route's grid position only when that route returns; regular scrolling stays local. */
@Composable
private fun rememberRestoredGridState(
    scrollPosition: GridScrollPosition,
    onPositionChange: (Int, Int) -> Unit,
): LazyGridState {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = scrollPosition.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = scrollPosition.firstVisibleItemScrollOffset,
    )
    LaunchedEffect(scrollPosition) {
        if (
            gridState.firstVisibleItemIndex != scrollPosition.firstVisibleItemIndex ||
            gridState.firstVisibleItemScrollOffset != scrollPosition.firstVisibleItemScrollOffset
        ) {
            gridState.scrollToItem(
                index = scrollPosition.firstVisibleItemIndex,
                scrollOffset = scrollPosition.firstVisibleItemScrollOffset,
            )
        }
    }
    DisposableEffect(gridState) {
        onDispose {
            onPositionChange(
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset,
            )
        }
    }
    return gridState
}

/** Keeps a forum feed viewport when a post detail temporarily replaces the root route. */
@Composable
private fun rememberRestoredListState(
    scrollPosition: GridScrollPosition,
    onPositionChange: (Int, Int) -> Unit,
): LazyListState {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollPosition.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = scrollPosition.firstVisibleItemScrollOffset,
    )
    LaunchedEffect(scrollPosition) {
        if (
            listState.firstVisibleItemIndex != scrollPosition.firstVisibleItemIndex ||
            listState.firstVisibleItemScrollOffset != scrollPosition.firstVisibleItemScrollOffset
        ) {
            listState.scrollToItem(
                index = scrollPosition.firstVisibleItemIndex,
                scrollOffset = scrollPosition.firstVisibleItemScrollOffset,
            )
        }
    }
    DisposableEffect(listState) {
        onDispose {
            onPositionChange(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
            )
        }
    }
    return listState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovalPieApp(
    startUri: String? = null,
    onStartUriHandled: () -> Unit = {},
    viewModel: NovalPieViewModel = viewModel(),
) {
    val route = viewModel.currentRoute

    // The deep link is consumed once and then cleared by the host activity. Keying the effect on
    // a URI that outlived the navigation meant a rotation re-ran openDeepLink and yanked the user
    // back to the linked book/chapter.
    LaunchedEffect(startUri) {
        if (!startUri.isNullOrBlank()) {
            viewModel.openDeepLink(startUri)
            onStartUriHandled()
        }
    }

    val canNavigateBack = route !is AppRoute.Forum &&
        route !is AppRoute.Home &&
        route !is AppRoute.Search &&
        route !is AppRoute.Tools &&
        route !is AppRoute.Profile
    val navigateBack: () -> Unit = {
        if (route == AppRoute.AuthCaptcha) viewModel.cancelAuthCaptcha() else viewModel.goBack()
    }
    BackHandler(enabled = canNavigateBack) {
        navigateBack()
    }

    val resolvedDarkTheme = viewModel.appThemeMode.resolvesDark(isSystemInDarkTheme())
    NovalPieTheme(darkTheme = resolvedDarkTheme) {
    CompositionLocalProvider(
        LocalChineseVariant provides viewModel.chineseVariant,
        LocalForumHideSpoilers provides forumContentHideSpoilers(),
    ) {
    Scaffold(
        // Edge-to-edge uses a dynamic IME inset on Android 15. Folding it into the same content
        // contract as status/navigation bars makes every route remeasure back to its real height
        // after the keyboard closes instead of retaining an old resize budget.
        contentWindowInsets = WindowInsets.safeDrawing.union(WindowInsets.ime),
        topBar = {
            if (globalProductTopBarVisible(route)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (canNavigateBack) {
                                routeContextLabel(route, viewModel.currentTab)
                            } else {
                                "NovalPie"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = navigateBack) {
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
                .padding(padding)
                // Child screens own their scrolling/content padding. Mark the scaffold budget as
                // consumed so a future route-level IME/system-bar modifier cannot add it twice.
                .consumeWindowInsets(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (route) {
                AppRoute.Forum -> ForumScreen(
                    state = viewModel.forumState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    scrollPosition = viewModel.forumScrollPosition,
                    onRefresh = viewModel::loadForum,
                    onSearchQueryChange = viewModel::updateForumSearchQuery,
                    onSearch = viewModel::loadForum,
                    onCategorySelected = viewModel::selectForumCategory,
                    onHideSpoilersChange = viewModel::updateForumHideSpoilers,
                    onLoadMore = viewModel::loadMoreForum,
                    onGoToPage = viewModel::goToForumPage,
                    onOpenPost = viewModel::openForumPost,
                    onOpenBook = viewModel::openBook,
                    onCreatePost = viewModel::openForumCreate,
                    onOpenUser = viewModel::openUserProfile,
                    onListScrollPositionChange = viewModel::saveForumScrollPosition
                )

                AppRoute.ForumCreate -> ForumCreateScreen(
                    state = viewModel.forumCreateState,
                    onDraftChange = viewModel::updateForumCreateDraft,
                    onSubmit = viewModel::submitForumPost,
                    onOpenLogin = viewModel::openLoginFallback
                )

                is AppRoute.ForumPostDetail -> ForumPostDetailScreen(
                    state = viewModel.forumPostDetailState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onBack = viewModel::goBack,
                    onRetry = { viewModel.loadForumPostDetail(route.postId) },
                    onDraftChange = viewModel::updateForumCommentDraft,
                    onSubmitComment = viewModel::submitForumComment,
                    onReplyComment = viewModel::replyToForumComment,
                    onCancelReply = viewModel::cancelForumReply,
                    onToggleCommentReplies = viewModel::toggleForumCommentReplies,
                    onLike = viewModel::likeForumPost,
                    onDislike = viewModel::dislikeForumPost,
                    onEmoji = viewModel::emojiForumPost,
                    onAward = viewModel::awardForumPost,
                    onCommentLike = viewModel::likeForumComment,
                    onCommentDislike = viewModel::dislikeForumComment,
                    onCommentEmoji = viewModel::emojiForumComment,
                    onCommentAward = viewModel::awardForumComment,
                    onOpenUser = viewModel::openUserProfile,
                    onOpenLogin = viewModel::openLoginFallback,
                    onOpenLink = { link ->
                        if (link.startsWith("https://novalpie.cc/", ignoreCase = true) ||
                            link.startsWith("http://novalpie.cc/", ignoreCase = true)
                        ) {
                            viewModel.openDeepLink(link)
                        } else {
                            viewModel.openWebFallback(link)
                        }
                    },
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/forum/${route.postId}") }
                )

                AppRoute.Home -> HomeScreen(
                    state = viewModel.homeState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    readerProgress = viewModel.readerProgress,
                    recentReaderProgresses = viewModel.recentReaderProgresses,
                    bookshelfQuery = viewModel.bookshelfQuery,
                    scrollPosition = viewModel.homeGridScrollPosition,
                    onRefresh = viewModel::loadHome,
                    onBookshelfQueryChange = viewModel::updateBookshelfQuery,
                    onFavoriteGroupSelected = viewModel::selectFavoriteGroup,
                      onFavoritesTabSelected = viewModel::selectFavoritesContentTab,
                      onToggleFavoritesLayout = viewModel::toggleFavoritesLayout,
                      onSelectFavoritesGridColumns = viewModel::selectFavoritesGridColumns,
                    onFavoritesDisplayModeSelected = viewModel::selectFavoritesDisplayMode,
                    onFavoritesSortUpdated = viewModel::updateFavoritesSort,
                    onToggleFavoritesSelectionMode = viewModel::toggleFavoritesSelectionMode,
                    onToggleFavoritesBookSelection = viewModel::toggleFavoritesBookSelection,
                    onLongPressFavoritesBook = viewModel::longPressFavoritesBook,
                    onCreateFavoriteGroup = viewModel::createFavoriteGroup,
                    onRenameFavoriteGroup = viewModel::renameFavoriteGroup,
                    onDeleteFavoriteGroup = viewModel::deleteFavoriteGroup,
                    onMoveSelectedFavorites = viewModel::moveSelectedFavoritesToGroup,
                    onRemoveSelectedFavorites = viewModel::removeSelectedFavorites,
                    onFavoritePinnedChange = viewModel::setFavoritePinned,
                    onDeleteSelectedHistory = viewModel::deleteSelectedReadingHistory,
                    onClearAllHistory = viewModel::clearAllReadingHistory,
                    onCycleFavoritesCacheMode = viewModel::cycleFavoritesCacheMode,
                    onClearFavoritesCache = viewModel::clearFavoritesCache,
                    onClearFavoriteImageCache = viewModel::clearFavoriteImageCache,
                    onOpenLogin = viewModel::openLoginFallback,
                    onContinueReading = viewModel::continueReading,
                    onClearReaderProgress = viewModel::clearReaderProgress,
                    onOpenBook = viewModel::openBook,
                    onGridScrollPositionChange = viewModel::saveHomeGridScrollPosition,
                    onLoadMoreFavorites = viewModel::loadMoreFavorites,
                    onOpenSearch = { viewModel.openTab(BottomTab.Discover) },
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/favorites") }
                )

                AppRoute.Search -> SearchScreen(
                    keyword = viewModel.searchKeyword,
                    searchHistory = viewModel.searchHistory,
                    options = viewModel.searchOptions,
                    viewMode = viewModel.searchOptions.viewMode,
                    results = viewModel.searchResults,
                    tags = viewModel.searchTags,
                    scrollPosition = viewModel.searchGridScrollPosition,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onUseSearchHistory = viewModel::useSearchHistory,
                    onClearSearchHistory = viewModel::clearSearchHistory,
                    onApplyRequiredTag = viewModel::applyRequiredSearchTag,
                    onApplyBlockedTag = viewModel::applyBlockedSearchTag,
                    onRemoveTag = viewModel::removeSearchTag,
                    onClearTags = viewModel::clearSearchTags,
                    onRefreshTags = viewModel::loadSearchTags,
                    onSortByChange = viewModel::updateSearchSortBy,
                    onSortOrderChange = viewModel::updateSearchSortOrder,
                    onScopeChange = viewModel::updateSearchScope,
                    onMatchTypeChange = viewModel::updateSearchMatchType,
                    onAdultFilterChange = viewModel::updateSearchAdultFilter,
                    onSourceChange = viewModel::updateSearchSource,
                    onWordCountRangeChange = viewModel::updateSearchWordCountRange,
                    onAdvancedSyntaxEnabledChange = viewModel::updateSearchAdvancedSyntaxEnabled,
                    onToggleSettingsCache = viewModel::toggleSearchSettingsCache,
                    onClearSettingsCache = viewModel::clearSearchSettingsCache,
                    onToggleViewMode = viewModel::toggleSearchViewMode,
                    onSearch = { submittedKeyword -> viewModel.performSearch(submittedKeyword) },
                    searchPage = viewModel.searchResultPage,
                    searchLoadingPage = viewModel.searchLoadingMore,
                    searchPageError = viewModel.searchLoadMoreError,
                    onGoToPage = viewModel::goToSearchPage,
                    onOpenBook = viewModel::openBook,
                    onPreviewBookCover = viewModel::previewBookCover,
                    onGridScrollPositionChange = viewModel::saveSearchGridScrollPosition,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/search?sort_by=relevance") }
                )

                AppRoute.Tools -> ToolsScreen(
                    state = viewModel.toolsState,
                    user = viewModel.homeState.user,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    appThemeMode = viewModel.appThemeMode,
                    chineseVariant = viewModel.chineseVariant,
                    onRefresh = viewModel::loadTools,
                    onToggleAppTheme = viewModel::toggleAppTheme,
                    onCycleChineseVariant = viewModel::cycleChineseVariant,
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
                    onOverviewDaysChange = viewModel::updateAdminOverviewDays,
                    onReviewQueryChange = viewModel::updateAdminReviewQuery,
                    onApplyReviewQuery = viewModel::applyAdminReviewQuery,
                    onResetReviewQuery = viewModel::resetAdminReviewQuery,
                    onApproveAllReviews = viewModel::approveAllAdminReviews,
                    onOperationLogQueryChange = viewModel::updateAdminOperationLogQuery,
                    onApplyOperationLogQuery = viewModel::applyAdminOperationLogQuery,
                    onResetOperationLogQuery = viewModel::resetAdminOperationLogQuery,
                    onOperationLogPageChange = viewModel::selectAdminOperationLogPage,
                    onShopQueryChange = viewModel::updateAdminShopQuery,
                    onApplyShopQuery = viewModel::applyAdminShopQuery,
                    onResetShopQuery = viewModel::resetAdminShopQuery,
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
                    onToggleServerApi = viewModel::toggleWorkspaceApi,
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
                    onQueueDocuments = viewModel::queueEditorDocuments,
                    onRemoveQueuedDocument = viewModel::removeQueuedEditorDocument,
                    onImportQueuedDocuments = viewModel::importQueuedEditorDocuments,
                    onEncodingChange = viewModel::updateEditorEncoding,
                    onDocumentChange = viewModel::updateEditorDocument,
                    onMetadataChange = viewModel::updateEditorMetadata,
                    onSplitModeChange = viewModel::updateEditorSplitMode,
                    onSplitPatternChange = viewModel::updateEditorSplitPattern,
                    onSplitTargetChange = viewModel::updateEditorSplitTarget,
                    onCustomScriptChange = viewModel::updateEditorCustomScript,
                    onScriptChunkedChange = viewModel::updateEditorScriptChunked,
                    onScriptChunkSizeChange = viewModel::updateEditorScriptChunkSize,
                    onApiEndpointChange = viewModel::updateEditorApiEndpoint,
                    onApiTimeoutChange = viewModel::updateEditorApiTimeout,
                    onApiMarkerModeChange = viewModel::updateEditorApiMarkerMode,
                    onBatchModeChange = viewModel::updateEditorBatchMode,
                    onBatchTargetChange = viewModel::updateEditorBatchTarget,
                    onCustomScriptResult = viewModel::completeEditorCustomScript,
                    onAiConfigSelected = viewModel::selectEditorAiConfig,
                    onGenerateAiRegex = viewModel::generateEditorRegexWithAi,
                    onProcessSplit = viewModel::processEditorSplit,
                    onFindChange = viewModel::updateEditorFind,
                    onReplaceChange = viewModel::updateEditorReplace,
                    onFindRegexChange = viewModel::updateEditorFindUsesRegex,
                    onReplaceAll = viewModel::replaceEditorText,
                    onUndo = viewModel::undoEditorDocument,
                    onRedo = viewModel::redoEditorDocument,
                    onInsertTitleMarkerAtCursor = viewModel::insertEditorTitleMarkerAtCursor,
                    onInsertContentMarkerAtCursor = viewModel::insertEditorContentMarkerAtCursor,
                    onInsertChapterAtCursor = viewModel::insertEditorChapterAtCursor,
                    onDeleteChapterAtCursor = viewModel::deleteEditorChapterAtCursor,
                    onRenumberMarkers = viewModel::renumberEditorMarkers,
                    onValidateMarkers = viewModel::validateEditorMarkers,
                    onClearMarkers = viewModel::clearEditorMarkers,
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

                is AppRoute.Auth -> AuthScreen(
                    page = route.page,
                    state = viewModel.authState,
                    onPageSelected = viewModel::switchAuthPage,
                    onLoginMethodSelected = viewModel::selectAuthLoginMethod,
                    onLoginUsernameChange = viewModel::updateAuthLoginUsername,
                    onLoginPasswordChange = viewModel::updateAuthLoginPassword,
                    onLoginEmailChange = viewModel::updateAuthLoginEmail,
                    onLoginCodeChange = viewModel::updateAuthLoginCode,
                    onRegisterEmailChange = viewModel::updateAuthRegisterEmail,
                    onRegisterCodeChange = viewModel::updateAuthRegisterCode,
                    onRegisterUsernameChange = viewModel::updateAuthRegisterUsername,
                    onRegisterPasswordChange = viewModel::updateAuthRegisterPassword,
                    onRegisterConfirmPasswordChange = viewModel::updateAuthRegisterConfirmPassword,
                    onResetEmailChange = viewModel::updateAuthResetEmail,
                    onResetPasswordChange = viewModel::updateAuthResetPassword,
                    onResetConfirmPasswordChange = viewModel::updateAuthResetConfirmPassword,
                    onSendLoginCode = viewModel::sendAuthLoginCode,
                    onSubmitLogin = viewModel::submitAuthLogin,
                    onSendRegistrationCode = viewModel::sendAuthRegistrationCode,
                    onVerifyRegistrationCode = viewModel::verifyAuthRegistrationCode,
                    onSubmitRegistration = viewModel::submitAuthRegistration,
                    onRequestPasswordReset = viewModel::requestAuthPasswordReset,
                    onSubmitPasswordReset = viewModel::submitAuthPasswordReset,
                    onOpenWebLogin = viewModel::openWebLoginFallback
                )

                AppRoute.AuthCaptcha -> AuthCaptchaScreen(
                    proxySettings = viewModel.proxySettings,
                    onToken = viewModel::completeAuthCaptcha,
                    onCancel = viewModel::cancelAuthCaptcha
                )

                AppRoute.Profile -> ProfileScreen(
                    state = viewModel.profileState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onRefresh = viewModel::loadProfile,
                    onOpenLogin = viewModel::openLoginFallback,
                    onTabSelected = viewModel::selectProfileTab,
                      bookQuery = viewModel.profileState.bookQuery,
                      onBookQueryChange = viewModel::updateProfileBookQuery,
                      onBookGridColumnsChange = viewModel::selectProfileBooksGridColumns,
                    onNameChange = viewModel::updateProfileName,
                    onBioChange = viewModel::updateProfileBio,
                    onShowCheckinChange = viewModel::updateProfileShowCheckin,
                    onAutoCheckinChange = viewModel::updateProfileAutoCheckin,
                    onAdultBirthYearChange = viewModel::updateProfileAdultBirthYear,
                    onSave = viewModel::saveProfile,
                    onCheckin = viewModel::checkinCurrentUser,
                    onVerifyAdult = viewModel::verifyCurrentUserAdult,
                    onAvatarSelected = viewModel::uploadProfileAvatar,
                    onOpenSettings = viewModel::openSettings,
                    onOpenActivity = viewModel::openUserActivity,
                    onActivityFilterSelected = viewModel::selectProfileActivityFilter,
                    onOpenBook = viewModel::openBook,
                    onPersonalizationTabSelected = viewModel::selectPersonalizationTab,
                    onPurchaseShopItem = viewModel::purchaseCurrentUserShopItem,
                    onEquipInventoryItem = viewModel::toggleCurrentUserEquipment
                )

                is AppRoute.UserProfileDetail -> UserProfileDetailScreen(
                    state = viewModel.userProfileDetailState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    onRetry = { viewModel.loadUserProfile(route.userId) },
                    onTabSelected = viewModel::selectUserProfileTab,
                    onActivityFilterSelected = viewModel::selectUserProfileActivityFilter,
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
                    appThemeMode = viewModel.appThemeMode,
                    chineseVariant = viewModel.chineseVariant,
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
                    onAppThemeModeChange = viewModel::updateAppThemeMode,
                    onChineseVariantChange = viewModel::updateChineseVariant,
                    onOpenHomeFallback = { viewModel.openWebFallback("https://novalpie.cc") },
                    onOpenSearchFallback = { viewModel.openWebFallback("https://novalpie.cc/search?sort_by=relevance") }
                )

                is AppRoute.BookDetail -> BookDetailScreen(
                    state = viewModel.bookDetailState,
                    nativeEpubDownloadState = viewModel.nativeEpubDownloadState,
                    hasAuthToken = !viewModel.authToken.isNullOrBlank(),
                    readerProgress = viewModel.bookDetailState.readerProgress,
                    catalogQuery = viewModel.bookCatalogQuery,
                    onCatalogQueryChange = viewModel::updateBookCatalogQuery,
                    onRetry = { viewModel.loadBookDetail(route.bookId) },
                    onOpenReader = viewModel::openReader,
                    onToggleFavorite = viewModel::toggleBookDetailFavorite,
                    onOpenTerminology = { viewModel.openTerminology(route.bookId) },
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
                    onOpenUser = viewModel::openUserProfile,
                    onOpenLink = { link ->
                        if (link.startsWith("https://novalpie.cc/", ignoreCase = true) ||
                            link.startsWith("http://novalpie.cc/", ignoreCase = true)
                        ) {
                            viewModel.openDeepLink(link)
                        } else {
                            viewModel.openWebFallback(link)
                        }
                    },
                    onDownloadEpub = { viewModel.downloadBookEpub(route.bookId) },
                    onDownloadTxt = { viewModel.downloadBookTxt(route.bookId) },
                    onPreviewBookCover = viewModel::previewBookCover,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/book/${route.bookId}") }
                )

                is AppRoute.Terminology -> TerminologyScreen(
                    state = viewModel.terminologyState,
                    onKeywordChange = viewModel::updateTerminologyKeyword,
                    onSearch = viewModel::searchTerminologies,
                    onRetry = { viewModel.loadTerminologies(route.bookId) },
                    onLoadMore = viewModel::loadMoreTerminologies,
                    onOpenWeb = { viewModel.openWebFallback("https://novalpie.cc/book/${route.bookId}") },
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

                is AppRoute.Reader -> ReaderRoute(route = route, viewModel = viewModel)

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
    // Compose this after the route BackHandler so a visible preview always consumes Back before
    // the underlying detail/reader route can navigate away.
    ImagePreviewHost(
        state = viewModel.imagePreviewState,
        onDismiss = viewModel::dismissImagePreview,
    )
    }
}

/** Isolates the reader's large callback surface from the root Scaffold content lambda. */
@Composable
private fun ReaderRoute(
    route: AppRoute.Reader,
    viewModel: NovalPieViewModel,
) {
    ReaderScreen(
        state = viewModel.readerState,
        options = viewModel.readerUiOptions,
        ttsSettings = viewModel.readerTtsSettings,
        catalogQuery = viewModel.readerCatalogQuery,
        onCatalogQueryChange = viewModel::updateReaderCatalogQuery,
        onDecreaseFont = viewModel::decreaseReaderFont,
        onIncreaseFont = viewModel::increaseReaderFont,
        onCycleTheme = viewModel::cycleReaderTheme,
        onReaderOptionsChange = viewModel::setReaderOptions,
        onReaderTtsSettingsChange = viewModel::updateReaderTtsSettings,
        onResetReaderOptions = viewModel::resetReaderOptions,
        onClearReaderChapterCache = viewModel::clearReaderChapterCache,
        onRetry = {
            viewModel.loadReader(
                route.bookId,
                route.chapterId,
                entryPosition = route.entryPosition,
            )
        },
        onRetryChapterComments = viewModel::refreshReaderChapterComments,
        onRetryCatalog = viewModel::refreshReaderCatalog,
        onOpenReader = viewModel::openReader,
        onOpenReaderAtPosition = { bookId, chapterId, entryPosition ->
            viewModel.openReader(bookId, chapterId, entryPosition)
        },
        onLoadNextChapter = viewModel::loadNextReaderChapter,
        onVisibleChapterChanged = viewModel::recordVisibleReaderChapter,
        onToggleFavorite = viewModel::toggleReaderFavorite,
        onBack = viewModel::goBack,
        onCommentDraftChange = viewModel::updateReaderCommentDraft,
        onSubmitComment = viewModel::submitReaderComment,
        onReplyComment = viewModel::replyToReaderComment,
        onCancelCommentReply = viewModel::cancelReaderCommentReply,
        onCommentLike = viewModel::likeReaderComment,
        onCommentDislike = viewModel::dislikeReaderComment,
        onCommentEmoji = viewModel::emojiReaderComment,
        onCommentAward = viewModel::awardReaderComment,
        onOpenUser = viewModel::openUserProfile,
        onOpenLink = { link ->
            if (
                link.startsWith("https://novalpie.cc/", ignoreCase = true) ||
                link.startsWith("http://novalpie.cc/", ignoreCase = true)
            ) {
                viewModel.openDeepLink(link)
            } else {
                viewModel.openWebFallback(link)
            }
        },
        onOpenWeb = { chapterId ->
            viewModel.openWebFallback("https://novalpie.cc/book/${route.bookId}/$chapterId")
        },
        onPreviewImage = { image, title ->
            viewModel.previewReaderImage(route.bookId, image, title)
        },
    )
}

private fun bottomTabIcon(tab: BottomTab): ImageVector = when (tab) {
    BottomTab.Collection -> Icons.Filled.Favorite
    BottomTab.Discover -> Icons.Filled.Search
    BottomTab.Tools -> Icons.Filled.GridView
    BottomTab.Forum -> Icons.Filled.Forum
    BottomTab.Profile -> Icons.Filled.Person
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ForumScreen(
    state: ForumState,
    hasAuthToken: Boolean,
    scrollPosition: GridScrollPosition,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onHideSpoilersChange: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onOpenPost: (Long) -> Unit,
    onOpenBook: (Long) -> Unit,
    onCreatePost: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onListScrollPositionChange: (Int, Int) -> Unit
) {
    val availableWidthDp = LocalConfiguration.current.screenWidthDp
    if (forumUsesListLayout(state.selectedType, availableWidthDp)) {
        ForumListScreen(
            state = state,
            hasAuthToken = hasAuthToken,
            scrollPosition = scrollPosition,
            onRefresh = onRefresh,
            onSearchQueryChange = onSearchQueryChange,
            onSearch = onSearch,
            onCategorySelected = onCategorySelected,
            onHideSpoilersChange = onHideSpoilersChange,
            onLoadMore = onLoadMore,
            onGoToPage = onGoToPage,
            onOpenPost = onOpenPost,
            onOpenBook = onOpenBook,
            onCreatePost = onCreatePost,
            onOpenUser = onOpenUser,
            onListScrollPositionChange = onListScrollPositionChange,
        )
        return
    }
    val feedItems = when (val posts = state.posts) {
        is LoadResult.Success -> posts.value.map(::forumPostFeedItem)
        else -> emptyList()
    }
    val forumGridState = rememberRestoredGridState(
        scrollPosition = scrollPosition,
        onPositionChange = onListScrollPositionChange,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(
                forumGridColumnCount(state.selectedType, availableWidthDp),
            ),
            state = forumGridState,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "NOVALPIE_NATIVE_COMPOSE_HOME" },
            contentPadding = PaddingValues(
                start = NovalPieSpacing.screenHorizontal,
                top = NovalPieSpacing.lg,
                end = NovalPieSpacing.screenHorizontal,
                bottom = NovalPieSpacing.listBottom
            ),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NpSearchField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    onSearch = { onSearch() },
                    placeholder = "搜索帖子...",
                    clearContentDescription = "清除帖子搜索"
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ForumCategorySelector(
                    selectedType = state.selectedType,
                    reviewTotal = state.reviewTotal,
                    onSelect = onCategorySelected
                )
            }
            if (state.selectedType == "review") {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ForumSpoilerToggle(
                        hideSpoilers = state.hideSpoilers,
                        onHideSpoilersChange = onHideSpoilersChange
                    )
                }
            }
            when (val posts = state.posts) {
                // Skeletons reserve the footprint of the live source cards while the request is in flight.
                LoadResult.Loading -> items(3) { NpBookRowSkeleton() }
                is LoadResult.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                    NpErrorState(
                        message = posts.message,
                        retryLabel = "重新加载",
                        onRetry = onRefresh
                    )
                }
                is LoadResult.Success -> if (posts.value.isEmpty()) {
                    val searching = state.searchQuery.isNotBlank()
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NpEmptyState(
                            title = if (searching) "未找到匹配的帖子" else "暂时没有可显示的帖子",
                            description = if (searching) "尝试其他搜索词" else "下拉刷新后再试",
                            actionLabel = if (searching) "清除搜索" else "重新加载",
                            onAction = if (searching) {
                                { onSearchQueryChange("") }
                            } else {
                                onRefresh
                            }
                        )
                    }
                }
                LoadResult.Idle -> Unit
            }
            items(feedItems, key = { it.id }) { item ->
                ForumFeedRow(
                    item = item,
                    onOpenPost = onOpenPost,
                    onOpenBook = onOpenBook,
                    onOpenUser = onOpenUser,
                    compact = state.selectedType == "review",
                    hideSpoilers = forumFeedHideSpoilers(
                        type = state.selectedType,
                        reviewFeedHideSpoilers = state.hideSpoilers,
                    ),
                )
            }
            if (state.totalPages?.let { it > 1 } == true) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ForumPaginationBar(
                        page = state.page,
                        totalPages = state.totalPages,
                        loading = state.posts is LoadResult.Loading,
                        onGoToPage = onGoToPage
                    )
                }
            } else if (state.loadingMore || state.canLoadMore || state.loadMoreError != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    when {
                        state.loadingMore -> LoadingBlock("正在加载更多帖子")
                        state.loadMoreError != null -> NpErrorState(
                            message = state.loadMoreError,
                            retryLabel = "重试加载更多",
                            onRetry = onLoadMore
                        )
                        else -> OutlinedButton(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("加载更多帖子") }
                    }
                }
            }
        }
        if (hasAuthToken) {
            androidx.compose.material3.FloatingActionButton(
                onClick = onCreatePost,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(NovalPieSpacing.lg)
                    .size(NovalPieSize.minTouchTarget),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "发布帖子")
            }
        }
    }
}

/**
 * Phone forum feeds are structurally one-dimensional. Using LazyColumn here avoids the grid
 * subcomposition/remeasure loop that can starve touch dispatch during a long review fling.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ForumListScreen(
    state: ForumState,
    hasAuthToken: Boolean,
    scrollPosition: GridScrollPosition,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onHideSpoilersChange: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onOpenPost: (Long) -> Unit,
    onOpenBook: (Long) -> Unit,
    onCreatePost: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onListScrollPositionChange: (Int, Int) -> Unit,
) {
    val feedItems = when (val posts = state.posts) {
        is LoadResult.Success -> posts.value.map(::forumPostFeedItem)
        else -> emptyList()
    }
    val forumListState = rememberRestoredListState(
        scrollPosition = scrollPosition,
        onPositionChange = onListScrollPositionChange,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = forumListState,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "NOVALPIE_NATIVE_COMPOSE_HOME" },
            contentPadding = PaddingValues(
                start = NovalPieSpacing.screenHorizontal,
                top = NovalPieSpacing.lg,
                end = NovalPieSpacing.screenHorizontal,
                bottom = NovalPieSpacing.listBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
        ) {
            item {
                NpSearchField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    onSearch = { onSearch() },
                    placeholder = "搜索帖子...",
                    clearContentDescription = "清除帖子搜索",
                )
            }
            item {
                ForumCategorySelector(
                    selectedType = state.selectedType,
                    reviewTotal = state.reviewTotal,
                    onSelect = onCategorySelected,
                )
            }
            if (state.selectedType == "review") {
                item {
                    ForumSpoilerToggle(
                        hideSpoilers = state.hideSpoilers,
                        onHideSpoilersChange = onHideSpoilersChange,
                    )
                }
            }
            when (val posts = state.posts) {
                LoadResult.Loading -> items(3) { NpBookRowSkeleton() }
                is LoadResult.Error -> item {
                    NpErrorState(
                        message = posts.message,
                        retryLabel = "重新加载",
                        onRetry = onRefresh,
                    )
                }
                is LoadResult.Success -> if (posts.value.isEmpty()) {
                    val searching = state.searchQuery.isNotBlank()
                    item {
                        NpEmptyState(
                            title = if (searching) "未找到匹配的帖子" else "暂时没有可显示的帖子",
                            description = if (searching) "尝试其他搜索词" else "下拉刷新后再试",
                            actionLabel = if (searching) "清除搜索" else "重新加载",
                            onAction = if (searching) {
                                { onSearchQueryChange("") }
                            } else {
                                onRefresh
                            },
                        )
                    }
                }
                LoadResult.Idle -> Unit
            }
            items(feedItems, key = { it.id }) { item ->
                ForumFeedRow(
                    item = item,
                    onOpenPost = onOpenPost,
                    onOpenBook = onOpenBook,
                    onOpenUser = onOpenUser,
                    compact = state.selectedType == "review",
                    hideSpoilers = forumFeedHideSpoilers(
                        type = state.selectedType,
                        reviewFeedHideSpoilers = state.hideSpoilers,
                    ),
                )
            }
            if (state.totalPages?.let { it > 1 } == true) {
                item {
                    ForumPaginationBar(
                        page = state.page,
                        totalPages = state.totalPages,
                        loading = state.posts is LoadResult.Loading,
                        onGoToPage = onGoToPage,
                    )
                }
            } else if (state.loadingMore || state.canLoadMore || state.loadMoreError != null) {
                item {
                    when {
                        state.loadingMore -> LoadingBlock("正在加载更多帖子")
                        state.loadMoreError != null -> NpErrorState(
                            message = state.loadMoreError,
                            retryLabel = "重试加载更多",
                            onRetry = onLoadMore,
                        )
                        else -> OutlinedButton(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("加载更多帖子") }
                    }
                }
            }
        }
        if (hasAuthToken) {
            androidx.compose.material3.FloatingActionButton(
                onClick = onCreatePost,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(NovalPieSpacing.lg)
                    .size(NovalPieSize.minTouchTarget),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "发布帖子")
            }
        }
    }
}

@Composable
private fun ForumCategorySelector(
    selectedType: String,
    reviewTotal: Int?,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NovalPieSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
        ) {
            forumCategorySlots(forumFeedCategories()).forEach { slot ->
                val category = slot.category
                val selected = category.type == selectedType
                Box(
                    modifier = Modifier
                        .weight(slot.weight)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(NovalPieRadius.sm))
                        .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { onSelect(category.type) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        forumFeedCategoryLabel(category, reviewTotal),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumSpoilerToggle(
    hideSpoilers: Boolean,
    onHideSpoilersChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NovalPieSize.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
    ) {
        Text(
            "显示剧透内容",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = !hideSpoilers,
            onCheckedChange = { showSpoilers -> onHideSpoilersChange(!showSpoilers) }
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ForumFeedRow(
    item: ForumFeedItem,
    onOpenPost: (Long) -> Unit,
    onOpenBook: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    compact: Boolean = false,
    hideSpoilers: Boolean = true,
) {
    val destination = forumFeedDestination(item)
    val openDestination: () -> Unit = {
        when (destination) {
            is ForumFeedDestination.Book -> onOpenBook(destination.bookId)
            is ForumFeedDestination.Post -> onOpenPost(destination.postId)
            ForumFeedDestination.None -> Unit
        }
    }
    var revealedSpoilerIndexes by remember(item.id, item.excerpt, hideSpoilers) {
        mutableStateOf(emptySet<Int>())
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.lg),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = NovalPieElevation.card)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) NovalPieSpacing.md else NovalPieSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = item.authorId != null) { item.authorId?.let(onOpenUser) },
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForumAvatar(item, size = if (compact) 32.dp else NovalPieSize.avatarMd)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)) {
                    Text(
                        item.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.authorBadges.isNotEmpty() || item.authorBadgeVisuals.isNotEmpty()) {
                        ForumAuthorBadgeRow(
                            labels = item.authorBadges,
                            visuals = item.authorBadgeVisuals,
                        )
                    }
                    Text(
                        forumShortDateLabel(item.createdAt ?: item.lastActiveLabel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.isBookReview) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .forumCardTap(
                            enabled = destination !is ForumFeedDestination.None,
                            onTap = openDestination,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCover(
                        title = item.bookTitle,
                        coverUrl = item.bookCoverUrl,
                        width = if (compact) 30.dp else 36.dp,
                        height = if (compact) 45.dp else 54.dp,
                        previewUrl = item.bookCoverUrl,
                        previewPolicy = CoverPreviewPolicy.Disabled,
                        requestSize = novalPieBookCoverTargetSize(
                            widthPx = if (compact) 30 else 36,
                            heightPx = if (compact) 45 else 54,
                        ),
                        staticImage = true,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            forumFeedTitle(item),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.pinned || item.featured) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
                            ) {
                                if (item.pinned) ForumStatusBadge(label = "缃《", color = Color(0xFFF97316))
                                if (item.featured) ForumStatusBadge(label = "绮惧崕", color = Color(0xFFEAB308))
                            }
                        }
                    }
                }
            } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .forumCardTap(
                        enabled = destination !is ForumFeedDestination.None,
                        onTap = openDestination,
                    ),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.pinned) ForumStatusBadge(label = "置顶", color = Color(0xFFF97316))
                if (item.featured) ForumStatusBadge(label = "精华", color = Color(0xFFEAB308))
            }
            }

            item.excerpt?.takeIf(String::isNotBlank)?.let { excerpt ->
                ForumFeedExcerpt(
                    content = excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (item.isBookReview) 4 else 3,
                    hideSpoilers = hideSpoilers,
                    revealedSpoilerIndexes = revealedSpoilerIndexes,
                    onOpenCard = openDestination,
                    onRevealSpoiler = { spoilerIndex ->
                        revealedSpoilerIndexes = forumRevealSpoiler(
                            hideSpoilers = hideSpoilers,
                            revealedSpoilerIndexes = revealedSpoilerIndexes,
                            spoilerIndex = spoilerIndex,
                        )
                    },
                )
            }

            val visibleTags = forumFeedTags(item)
            if (visibleTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                ) {
                    visibleTags.forEach { tag -> ForumTopicTag(tag) }
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NovalPieSize.hairline)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    if (compact) NovalPieSpacing.sm else NovalPieSpacing.md
                ),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
            ) {
                ForumMetric(Icons.Filled.Forum, "${item.replyCount} 条回复")
                if (item.isBookReview) {
                    ForumMetric(Icons.Filled.Favorite, item.likeCount.toString(), MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (item.helpfulCount > 0) ForumMetric(Icons.Filled.ThumbUp, item.helpfulCount.toString(), MaterialTheme.colorScheme.primary)
                    if (item.notHelpfulCount > 0) ForumMetric(Icons.Filled.ThumbDown, item.notHelpfulCount.toString(), MaterialTheme.colorScheme.error)
                    if (item.funnyCount > 0) ForumMetric(Icons.Filled.EmojiEmotions, item.funnyCount.toString(), Color(0xFF16A34A))
                    if (item.awardPoints > 0) ForumMetric(Icons.Filled.CardGiftcard, item.awardPoints.toString(), Color(0xFFF59E0B))
                }
                ForumMetric(Icons.Filled.Visibility, "${item.viewCount} 次浏览")
                Text(
                    forumShortDateLabel(item.createdAt ?: item.lastActiveLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ForumAvatar(item: ForumFeedItem, size: Dp = NovalPieSize.avatarMd) {
    ForumProfileAvatar(
        authorName = item.authorName,
        avatarUrl = item.authorAvatarUrl,
        avatarFrameUrl = item.authorAvatarFrameUrl,
        size = size,
    )
}

@Composable
private fun ForumProfileAvatar(
    authorName: String,
    avatarUrl: String?,
    avatarFrameUrl: String? = null,
    size: Dp = NovalPieSize.avatarMd
) {
    val context = LocalContext.current
    val avatarRequest = remember(avatarUrl, context) {
        avatarUrl?.takeIf(String::isNotBlank)?.let { url ->
            novalPieStaticImageRequest(context, url, widthPx = 160, heightPx = 160)
        }
    }
    val frameRequest = remember(avatarFrameUrl, context) {
        avatarFrameUrl?.takeIf(String::isNotBlank)?.let { url ->
            novalPieStaticImageRequest(context, url, widthPx = 160, heightPx = 160)
        }
    }
    Box(
        modifier = Modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (avatarRequest != null) {
                SubcomposeAsyncImage(
                    model = avatarRequest,
                    contentDescription = "$authorName 的头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { ForumAvatarFallback(authorName) },
                    error = { ForumAvatarFallback(authorName) }
                )
            } else {
                ForumAvatarFallback(authorName)
            }
        }
        if (frameRequest != null) {
            SubcomposeAsyncImage(
                model = frameRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer(scaleX = PROFILE_AVATAR_FRAME_SCALE, scaleY = PROFILE_AVATAR_FRAME_SCALE),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ForumAvatarFallback(authorName: String) {
    Text(
        authorName.trim().firstOrNull()?.toString() ?: "N",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ForumAuthorBadgeRow(
    labels: List<String>,
    visuals: List<UserBadge>,
) {
    val badges = forumAuthorBadgeVisuals(visuals = visuals, labels = labels)
    if (badges.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
    ) {
        badges.forEach { badge ->
            ProfileSourceBadge(
                badge = badge,
                display = ProfileBadgeDisplay.Inline,
            )
        }
    }
}

@Composable
private fun ForumStatusBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(NovalPieRadius.xs), color = color) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = NovalPieSpacing.sm, vertical = NovalPieSpacing.xs),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ForumTopicTag(label: String) {
    Surface(
        shape = RoundedCornerShape(NovalPieRadius.pill),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = NovalPieSpacing.sm, vertical = NovalPieSpacing.xs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ForumMetric(icon: ImageVector, label: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(NovalPieSize.iconSm), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ForumPostDetailScreen(
    state: ForumPostDetailState,
    hasAuthToken: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyComment: (ForumComment) -> Unit,
    onCancelReply: () -> Unit,
    onToggleCommentReplies: (Long) -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onCommentLike: (Long) -> Unit,
    onCommentDislike: (Long) -> Unit,
    onCommentEmoji: (Long) -> Unit,
    onCommentAward: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenWeb: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ForumDetailBackLink(onBack) }
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
                        onOpenLink = onOpenLink,
                        onOpenWeb = onOpenWeb
                    )
                }
            }
        }

        item {
            Text("评论", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            if (hasAuthToken) {
                InlineCommentComposer(
                    draft = state.commentDraft,
                    replyingToName = state.replyingToName,
                    loading = state.actionLoading,
                    message = state.actionMessage,
                    onDraftChange = onDraftChange,
                    onSubmit = onSubmitComment,
                    onCancelReply = onCancelReply
                )
            } else {
                ForumLoginPrompt(onOpenLogin)
            }
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
                            onOpenUser = onOpenUser,
                            expanded = state.expandedCommentIds.contains(thread.comment.id),
                            onToggleReplies = { onToggleCommentReplies(thread.comment.id) },
                            onOpenLink = onOpenLink
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ForumPostHeader(
    detail: ForumPostDetail,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenWeb: () -> Unit
) {
    val post = detail.post
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.lg),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = NovalPieElevation.card)
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = post.authorId != null) { post.authorId?.let(onOpenUser) },
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForumProfileAvatar(
                    authorName = post.authorName ?: "匿名用户",
                    avatarUrl = post.authorAvatarUrl
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)) {
                    Text(
                        post.authorName ?: "匿名用户",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.authorBadges.isNotEmpty() || post.authorBadgeVisuals.isNotEmpty()) {
                        ForumAuthorBadgeRow(
                            labels = post.authorBadges,
                            visuals = post.authorBadgeVisuals,
                        )
                    }
                    Text(
                        forumPostDateLine(post),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    post.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (post.pinned) ForumStatusBadge(label = "置顶", color = Color(0xFFF97316))
                if (post.featured) ForumStatusBadge(label = "精华", color = Color(0xFFEAB308))
            }

            if (post.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                ) {
                    post.tags.filter(String::isNotBlank).take(5).forEach { tag -> ForumTopicTag(tag) }
                }
            }

            ForumRichContent(
                content = detail.content.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                onOpenLink = onOpenLink,
                emptyLabel = "正文暂时为空"
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NovalPieSize.hairline)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
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
@OptIn(ExperimentalLayoutApi::class)
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
    ) {
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
private fun ForumDetailBackLink(onBack: () -> Unit) {
    TextButton(onClick = onBack) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回论坛",
            modifier = Modifier.size(NovalPieSize.iconMd)
        )
        Spacer(Modifier.width(NovalPieSpacing.xs))
        Text("返回论坛")
    }
}

@Composable
internal fun ForumFeedExcerpt(
    content: String,
    style: androidx.compose.ui.text.TextStyle,
    maxLines: Int,
    hideSpoilers: Boolean,
    revealedSpoilerIndexes: Set<Int>,
    onOpenCard: () -> Unit,
    onRevealSpoiler: (Int) -> Unit,
    semanticDescription: String = "内容摘要",
) {
    val paragraphCache = remember { ForumRichParagraphCache() }
    val paragraphs = paragraphCache.get(forumFeedExcerptText(content))
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val annotated = remember(paragraphs, hideSpoilers, revealedSpoilerIndexes, linkColor) {
        buildAnnotatedString {
            var spoilerIndex = 0
            paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                if (paragraphIndex > 0) append("\n\n")
                paragraph.segments.forEach { segment ->
                    when (segment) {
                        is ForumTextSegment.Plain -> append(segment.value)
                        is ForumTextSegment.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(segment.value)
                        }
                        is ForumTextSegment.Link -> withStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            )
                        ) {
                            append(segment.label)
                        }
                        is ForumTextSegment.Spoiler -> {
                            val currentSpoilerIndex = spoilerIndex++
                            pushStringAnnotation(
                                tag = FORUM_SPOILER_ANNOTATION,
                                annotation = currentSpoilerIndex.toString(),
                            )
                            if (forumSpoilerIsVisible(hideSpoilers, currentSpoilerIndex, revealedSpoilerIndexes)) {
                                append(segment.value)
                            } else {
                                withStyle(SpanStyle(color = Color.Black, background = Color.Black)) {
                                    append(segment.value)
                                }
                            }
                            pop()
                        }
                    }
                }
            }
        }
    }
    ClickableText(
        text = annotated,
        style = style.copy(color = textColor),
        modifier = Modifier.semantics { contentDescription = semanticDescription },
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onClick = { offset ->
            val spoilerIndex = forumStringAnnotationAtOffset(
                text = annotated,
                tag = FORUM_SPOILER_ANNOTATION,
                offset = offset,
            )
                ?.toIntOrNull()
            if (
                spoilerIndex != null &&
                !forumSpoilerIsVisible(hideSpoilers, spoilerIndex, revealedSpoilerIndexes)
            ) {
                onRevealSpoiler(spoilerIndex)
            } else {
                onOpenCard()
            }
        },
    )
}

/** A compact activity/feed preview with the same segment-level spoiler behavior as forum rows. */
@Composable
internal fun ForumRichExcerpt(
    content: String,
    style: TextStyle,
    maxLines: Int,
    onOpenContent: () -> Unit,
    semanticDescription: String,
) {
    val hideSpoilers = LocalForumHideSpoilers.current
    var revealedSpoilerIndexes by remember(content, hideSpoilers) {
        mutableStateOf(emptySet<Int>())
    }
    ForumFeedExcerpt(
        content = content,
        style = style,
        maxLines = maxLines,
        hideSpoilers = hideSpoilers,
        revealedSpoilerIndexes = revealedSpoilerIndexes,
        onOpenCard = onOpenContent,
        onRevealSpoiler = { spoilerIndex ->
            revealedSpoilerIndexes = forumRevealSpoiler(
                hideSpoilers = hideSpoilers,
                revealedSpoilerIndexes = revealedSpoilerIndexes,
                spoilerIndex = spoilerIndex,
            )
        },
        semanticDescription = semanticDescription,
    )
}

@Composable
private fun ForumRichContent(
    content: String,
    style: androidx.compose.ui.text.TextStyle,
    onOpenLink: (String) -> Unit,
    emptyLabel: String,
    hideSpoilers: Boolean = LocalForumHideSpoilers.current,
) {
    val paragraphCache = remember { ForumRichParagraphCache() }
    val paragraphs = paragraphCache.get(content)
    var revealedSpoilerIndexes by remember(content, hideSpoilers) {
        mutableStateOf(emptySet<Int>())
    }
    if (paragraphs.isEmpty()) {
        Text(emptyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
            var spoilerOffset = 0
            paragraphs.forEach { paragraph ->
                val paragraphSpoilerOffset = spoilerOffset
                spoilerOffset += paragraph.segments.count { it is ForumTextSegment.Spoiler }
                ForumRichParagraphText(
                    paragraph = paragraph,
                    style = style,
                    onOpenLink = onOpenLink,
                    hideSpoilers = hideSpoilers,
                    revealedSpoilerIndexes = revealedSpoilerIndexes,
                    spoilerOffset = paragraphSpoilerOffset,
                    onRevealSpoiler = { spoilerIndex ->
                        revealedSpoilerIndexes = forumRevealSpoiler(
                            hideSpoilers = hideSpoilers,
                            revealedSpoilerIndexes = revealedSpoilerIndexes,
                            spoilerIndex = spoilerIndex,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Keep navigation on the explicit title/cover row. Inline text and author controls own their own
 * pointer stream, while the list remains free to consume vertical movement.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.forumCardTap(
    enabled: Boolean,
    onTap: () -> Unit,
): Modifier {
    if (!enabled) return this
    return this
        .semantics {
            onClick(label = "打开帖子", action = { onTap(); true })
        }
        .combinedClickable(
            onClick = onTap,
            // A stationary long press is an inspection/selection gesture, not card navigation.
            onLongClick = {},
        )
}

@Composable
private fun ForumRichParagraphText(
    paragraph: ForumRichParagraph,
    style: androidx.compose.ui.text.TextStyle,
    onOpenLink: (String) -> Unit,
    hideSpoilers: Boolean,
    revealedSpoilerIndexes: Set<Int>,
    spoilerOffset: Int,
    onRevealSpoiler: (Int) -> Unit,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSurface
    var spoilerIndex = spoilerOffset
    val annotated = remember(
        paragraph,
        hideSpoilers,
        revealedSpoilerIndexes,
        spoilerOffset,
        linkColor,
        contentColor,
    ) {
        buildAnnotatedString {
            paragraph.segments.forEach { segment ->
                when (segment) {
                    is ForumTextSegment.Plain -> append(segment.value)
                    is ForumTextSegment.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(segment.value)
                    }
                    is ForumTextSegment.Link -> {
                        pushStringAnnotation(tag = FORUM_LINK_ANNOTATION, annotation = segment.url)
                        withStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(segment.label)
                        }
                        pop()
                    }
                    is ForumTextSegment.Spoiler -> {
                        val currentSpoilerIndex = spoilerIndex++
                        pushStringAnnotation(
                            tag = FORUM_SPOILER_ANNOTATION,
                            annotation = currentSpoilerIndex.toString(),
                        )
                        if (forumSpoilerIsVisible(hideSpoilers, currentSpoilerIndex, revealedSpoilerIndexes)) {
                            append(segment.value)
                        } else {
                            withStyle(SpanStyle(color = Color.Black, background = Color.Black)) {
                                append(segment.value)
                            }
                        }
                        pop()
                    }
                }
            }
        }
    }
    ClickableText(
        text = annotated,
        style = style.copy(color = contentColor),
        onClick = { offset ->
            val spoilerIndex = forumStringAnnotationAtOffset(
                text = annotated,
                tag = FORUM_SPOILER_ANNOTATION,
                offset = offset,
            )
                ?.toIntOrNull()
            if (spoilerIndex != null) {
                onRevealSpoiler(spoilerIndex)
            } else {
                forumStringAnnotationAtOffset(
                    text = annotated,
                    tag = FORUM_LINK_ANNOTATION,
                    offset = offset,
                )
                    ?.let(onOpenLink)
            }
        }
    )
}

@Composable
private fun ForumLoginPrompt(onOpenLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            Text(
                "登录后参与讨论",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onOpenLogin) { Text("登录") }
        }
    }
}

@Composable
private fun ForumLinkPreviewRows(
    links: List<String>,
    onOpenLink: (String) -> Unit = {},
) {
    if (links.isEmpty()) return
    Text("链接预览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    links.take(4).forEach { link ->
        Surface(
            modifier = Modifier.clickable { onOpenLink(link) },
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
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
    NpCard(contentPadding = NovalPieSpacing.md) {
        replyingToName?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
            ) {
                Text("回复 $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onCancelReply) { Text("取消") }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            label = { Text("写评论") }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(enabled = !loading && draft.isNotBlank(), onClick = onSubmit) { Text(if (loading) "发送中" else "发送") }
            // The result message is unweighted no more: a long failure string used to be pushed off
            // the row by the button rather than wrapping under it.
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
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
    onOpenUser: (Long) -> Unit,
    expanded: Boolean,
    onToggleReplies: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ForumCommentRow(
            comment = thread.comment,
            onLike = { onLike(thread.comment.id) },
            onDislike = { onDislike(thread.comment.id) },
            onEmoji = { onEmoji(thread.comment.id) },
            onAward = { onAward(thread.comment.id) },
            onReply = { onReply(thread.comment) },
            onOpenUser = onOpenUser,
            onOpenLink = onOpenLink
        )
        if (thread.replies.isNotEmpty()) {
            TextButton(
                onClick = onToggleReplies,
                modifier = Modifier.padding(start = NovalPieSpacing.sm)
            ) {
                Text(if (expanded) "收起 ${thread.replies.size} 条回复" else "展开 ${thread.replies.size} 条回复")
            }
            if (expanded) {
                thread.replies.forEach { reply ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = NovalPieSpacing.lg)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(NovalPieRadius.md)
                            )
                            .padding(start = NovalPieSpacing.sm)
                    ) {
                        ForumCommentRow(
                            comment = reply,
                            onLike = { onLike(reply.id) },
                            onDislike = { onDislike(reply.id) },
                            onEmoji = { onEmoji(reply.id) },
                            onAward = { onAward(reply.id) },
                            onReply = { onReply(reply) },
                            onOpenUser = onOpenUser,
                            onOpenLink = onOpenLink
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ForumCommentRow(
    comment: ForumComment,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onReply: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForumProfileAvatar(
                    authorName = comment.authorName ?: "匿名用户",
                    avatarUrl = comment.authorAvatarUrl,
                    avatarFrameUrl = comment.authorAvatarFrameUrl,
                    size = 40.dp
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = comment.authorId != null) { comment.authorId?.let(onOpenUser) },
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
                ) {
                    Text(
                        comment.authorName ?: "匿名用户",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (comment.authorBadges.isNotEmpty() || comment.authorBadgeVisuals.isNotEmpty()) {
                        ForumAuthorBadgeRow(
                            labels = comment.authorBadges,
                            visuals = comment.authorBadgeVisuals,
                        )
                    }
                    comment.createdAt?.let {
                        Text(
                            forumShortDateLabel(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            comment.replyToName?.let {
                Text("回复 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            ForumRichContent(
                content = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                onOpenLink = onOpenLink,
                emptyLabel = "评论内容暂时为空"
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
            ) {
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
    scrollPosition: GridScrollPosition,
    onRefresh: () -> Unit,
    onBookshelfQueryChange: (String) -> Unit,
    onFavoriteGroupSelected: (Long?) -> Unit,
    onFavoritesTabSelected: (FavoritesContentTab) -> Unit,
    onToggleFavoritesLayout: () -> Unit,
    onSelectFavoritesGridColumns: (Int) -> Unit,
    onFavoritesDisplayModeSelected: (FavoritesDisplayMode) -> Unit,
    onFavoritesSortUpdated: (String?, String?) -> Unit,
    onToggleFavoritesSelectionMode: () -> Unit,
    onToggleFavoritesBookSelection: (Long) -> Unit,
    onLongPressFavoritesBook: (Long) -> Unit,
    onCreateFavoriteGroup: (String) -> Unit,
    onRenameFavoriteGroup: (Long, String) -> Unit,
    onDeleteFavoriteGroup: (Long) -> Unit,
    onMoveSelectedFavorites: (Long?) -> Unit,
    onRemoveSelectedFavorites: () -> Unit,
    onFavoritePinnedChange: (FavoriteEntry, Boolean) -> Unit,
    onDeleteSelectedHistory: () -> Unit,
    onClearAllHistory: () -> Unit,
    onCycleFavoritesCacheMode: () -> Unit,
    onClearFavoritesCache: () -> Unit,
    onClearFavoriteImageCache: () -> Unit,
    onOpenLogin: () -> Unit,
    onContinueReading: (ReaderProgress) -> Unit,
    onClearReaderProgress: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onGridScrollPositionChange: (Int, Int) -> Unit,
    onLoadMoreFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val collectionGridColumnCount = state.options.gridColumns.coerceIn(2, 4)
    val collectionGridCoverHeight = searchGridCoverHeightDp(
        availableWidthDp = LocalConfiguration.current.screenWidthDp,
        columnCount = collectionGridColumnCount,
    ).dp
    val collectionColumns = if (state.options.layout == FavoritesLayout.Grid) {
        GridCells.Fixed(collectionGridColumnCount)
    } else {
        GridCells.Fixed(1)
    }
    // One LazyVerticalGrid, not a LazyColumn of hand-chunked rows. The old grid keyed each row on
    // the joined ids of the books in it, so loading one more page re-keyed every row after the
    // insertion point and threw away item reuse. The source-style column rule keeps portrait
    // phones at two readable covers while letting a genuinely wide window use more space.
    val collectionGridState = rememberRestoredGridState(
        scrollPosition = scrollPosition,
        onPositionChange = onGridScrollPositionChange,
    )
    LazyVerticalGrid(
        columns = collectionColumns,
        state = collectionGridState,
        modifier = Modifier.fillMaxSize(),
        // No extra bottom inset on top of the Scaffold's: listBottom is the whole budget.
        contentPadding = PaddingValues(
            start = NovalPieSpacing.screenHorizontal,
            end = NovalPieSpacing.screenHorizontal,
            top = NovalPieSpacing.sm,
            bottom = NovalPieSpacing.listBottom
        ),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)
    ) {
        item(key = "library-controls", span = { GridItemSpan(maxLineSpan) }) {
            FavoritesControlPanel(
                state = state,
                value = bookshelfQuery,
                onValueChange = onBookshelfQueryChange,
                onSelectTab = onFavoritesTabSelected,
                onSelectGroup = onFavoriteGroupSelected,
                onToggleLayout = onToggleFavoritesLayout,
                onSelectGridColumns = onSelectFavoritesGridColumns,
                onSelectDisplayMode = onFavoritesDisplayModeSelected,
                onUpdateSort = onFavoritesSortUpdated,
                onToggleSelectionMode = onToggleFavoritesSelectionMode,
                onCreateGroup = onCreateFavoriteGroup,
                onRenameGroup = onRenameFavoriteGroup,
                onDeleteGroup = onDeleteFavoriteGroup,
                onMoveSelectedFavorites = onMoveSelectedFavorites,
                onRemoveSelectedFavorites = onRemoveSelectedFavorites,
                onDeleteSelectedHistory = onDeleteSelectedHistory,
                onClearAllHistory = onClearAllHistory,
                onCycleCacheMode = onCycleFavoritesCacheMode,
                onClearFavoritesCache = onClearFavoritesCache,
                onClearImageCache = onClearFavoriteImageCache,
                onRetry = onRefresh
            )
        }
        item(key = "library-overview", span = { GridItemSpan(maxLineSpan) }) {
            LibraryOverviewBlock(
                overview = libraryOverview(
                    hasAuthToken = hasAuthToken,
                    favoriteCount = (state.favoriteEntries as? LoadResult.Success)?.value?.size ?: 0,
                    groupCount = (state.groups as? LoadResult.Success)?.value?.size ?: 0,
                    recentCount = (state.history as? LoadResult.Success)?.value?.size ?: 0,
                    pageCount = state.favoritesPage,
                ),
                onRefresh = onRefresh,
                onOpenSearch = onOpenSearch,
                onOpenLogin = onOpenLogin,
                onOpenWeb = onOpenWeb,
            )
        }
        readerProgress?.let { progress ->
            item(key = "collection-resume", span = { GridItemSpan(maxLineSpan) }) {
                val libraryBookTitle = buildList {
                    addAll((state.favoriteEntries as? LoadResult.Success)?.value.orEmpty().map(FavoriteEntry::book))
                    addAll((state.history as? LoadResult.Success)?.value.orEmpty().map(FavoriteEntry::book))
                    addAll((state.favorites as? LoadResult.Success)?.value.orEmpty())
                }
                    .firstOrNull { it.id == progress.bookId }
                    ?.title
                CompactFavoritesResumeRow(
                    presentation = compactFavoritesResumePresentation(progress, libraryBookTitle),
                    onContinue = { onContinueReading(progress) },
                    onClear = onClearReaderProgress
                )
            }
        }
        if (!hasAuthToken) {
            item(key = "collection-login", span = { GridItemSpan(maxLineSpan) }) {
                EmptyCollectionState(onOpenLogin = onOpenLogin, onOpenSearch = onOpenSearch)
            }
        } else {
            val collectionEntries = if (state.options.tab == FavoritesContentTab.Favorites) {
                state.favoriteEntries
            } else {
                state.history
            }
            when (val entries = collectionEntries) {
            LoadResult.Idle -> item(key = "library-favorites-idle", span = { GridItemSpan(maxLineSpan) }) {
                LibraryStatusLine("等待加载书架")
            }
            LoadResult.Loading -> item(key = "library-favorites-loading", span = { GridItemSpan(maxLineSpan) }) {
                LibraryLoadingBlock("正在加载收藏书籍")
            }
            is LoadResult.Error -> item(key = "library-favorites-error", span = { GridItemSpan(maxLineSpan) }) {
                NpErrorState(
                    message = entries.message,
                    retryLabel = retryActionLabel("书架"),
                    onRetry = onRefresh,
                    secondaryLabel = "打开网页",
                    onSecondary = { onOpenWeb() }
                )
            }
            is LoadResult.Success -> {
                val visibleEntries = visibleFavoriteEntries(
                    entries = entries.value,
                    query = bookshelfQuery,
                    displayMode = if (state.options.tab == FavoritesContentTab.Favorites) {
                        state.options.displayMode
                    } else {
                        FavoritesDisplayMode.All
                    },
                    selectedGroupId = state.selectedFavoriteGroupId
                )
                val entriesWithLocalProgress = favoriteEntriesWithLocalReaderProgress(
                    entries = visibleEntries,
                    localProgresses = recentReaderProgresses,
                )
                val visibleGroupFolders: List<FavoriteGroup> = if (
                    shouldShowFavoriteGroupFolders(state.options, state.selectedFavoriteGroupId)
                ) {
                    visibleFavoriteGroupFolders(
                        (state.groups as? LoadResult.Success)?.value.orEmpty()
                    )
                } else {
                    emptyList()
                }
                if (visibleGroupFolders.isNotEmpty()) {
                    items(
                        items = visibleGroupFolders,
                        key = { group -> "favorite-folder-${group.id ?: group.name}" },
                        // Folder summaries are a separate hierarchy level from book covers. Giving
                        // them the full row avoids a short folder card leaving a large blank area
                        // beside a tall 2:3 cover in the same grid line.
                        span = { GridItemSpan(maxLineSpan) }
                    ) { group ->
                        FavoriteGroupFolderCard(group = group) {
                            group.id?.let(onFavoriteGroupSelected)
                        }
                    }
                }
                when {
                    entries.value.isEmpty() && visibleGroupFolders.isEmpty() -> item(
                        key = "library-favorites-empty",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        if (state.options.tab == FavoritesContentTab.Favorites) {
                            NpEmptyState(
                                title = "暂无收藏",
                                description = "在作品详情页收藏后，会显示在这里。"
                            )
                        } else {
                            NpEmptyState(
                                title = "暂无阅读历史",
                                description = "打开一章正文后，阅读进度会显示在这里。"
                            )
                        }
                    }
                    shouldShowFavoriteNoMatchState(
                        visibleEntries = entriesWithLocalProgress,
                        visibleGroupFolders = visibleGroupFolders,
                        query = bookshelfQuery,
                    ) -> item(
                        key = "library-favorites-nomatch",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        NpEmptyState(
                            title = "没有匹配的收藏",
                            description = "换个关键词，或清空筛选后重新浏览书架。"
                        )
                    }
                    else -> if (state.options.layout == FavoritesLayout.Grid) {
                        items(
                            items = entriesWithLocalProgress.chunked(collectionGridColumnCount),
                            key = { row ->
                                "favorite-row-${row.joinToString(separator = ",") { it.favoriteId?.toString() ?: it.book.id.toString() }}"
                            },
                            // Compact library cards reserve identical title, author and progress
                            // slots. A direct row therefore keeps columns aligned without an
                            // intrinsic pre-measurement during a fast image-grid fling.
                            span = { GridItemSpan(maxLineSpan) },
                        ) { rowEntries ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
                                verticalAlignment = Alignment.Top,
                            ) {
                                rowEntries.forEach { entry ->
                                    val isSelected = entry.book.id in state.selectedBookIds
                                    val onEntryClick = {
                                        if (state.selectionMode) {
                                            onToggleFavoritesBookSelection(entry.book.id)
                                        } else {
                                            onOpenBook(entry.book.id)
                                        }
                                    }
                                    CompactLibraryBookCardItem(
                                        book = entry.book,
                                        presentation = compactFavoriteBookCardPresentation(entry),
                                        modifier = Modifier.weight(1f),
                                        gridCoverHeight = collectionGridCoverHeight,
                                        previewPolicy = CoverPreviewPolicy.Disabled,
                                        collectionCard = true,
                                        onClick = onEntryClick,
                                        onLongClick = { onLongPressFavoritesBook(entry.book.id) },
                                        selected = if (state.selectionMode) isSelected else entry.isPinned,
                                        selectionMode = state.selectionMode,
                                    )
                                }
                                repeat(collectionGridColumnCount - rowEntries.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(
                            items = entriesWithLocalProgress,
                            key = { entry -> "favorite-entry-${entry.favoriteId ?: entry.book.id}" }
                        ) { entry ->
                            val isSelected = entry.book.id in state.selectedBookIds
                            val onEntryClick = {
                                if (state.selectionMode) {
                                    onToggleFavoritesBookSelection(entry.book.id)
                                } else {
                                    onOpenBook(entry.book.id)
                                }
                            }
                            FavoriteListRow(
                                entry = entry,
                                selected = isSelected,
                                selecting = state.selectionMode,
                                onClick = onEntryClick,
                                onLongClick = { onLongPressFavoritesBook(entry.book.id) },
                            )
                        }
                    }
                }
                // Reported beside the control that triggered it. A failed extra page no longer
                // replaces the books already loaded.
                state.favoritesLoadMoreError?.let { loadMoreError ->
                    item(key = "library-loadmore-error", span = { GridItemSpan(maxLineSpan) }) {
                        NpErrorState(
                            message = loadMoreError,
                            retryLabel = "重试加载更多",
                            onRetry = onLoadMoreFavorites,
                        )
                    }
                }
                if (entries.value.isNotEmpty()) {
                    item(key = "library-loadmore", span = { GridItemSpan(maxLineSpan) }) {
                        LoadMoreRow(
                            canLoadMore = state.favoritesCanLoadMore,
                            loading = state.favoritesLoadingMore,
                            onLoadMore = onLoadMoreFavorites,
                            idleText = "已显示 ${entriesWithLocalProgress.size} 本",
                            loadText = if (state.options.tab == FavoritesContentTab.History) {
                                "加载更多阅读历史"
                            } else {
                                "加载更多收藏"
                            }
                        )
                    }
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
    viewMode: SearchViewMode,
    results: LoadResult<List<NovelCard>>,
    tags: LoadResult<List<NovelTag>>,
    scrollPosition: GridScrollPosition,
    onKeywordChange: (String) -> Unit,
    onUseSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onApplyRequiredTag: (String) -> Unit,
    onApplyBlockedTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onClearTags: () -> Unit,
    onRefreshTags: () -> Unit,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit,
    onAdvancedSyntaxEnabledChange: (Boolean) -> Unit,
    onToggleSettingsCache: () -> Unit,
    onClearSettingsCache: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSearch: (String?) -> Unit,
    searchPage: SearchPage?,
    searchLoadingPage: Boolean,
    searchPageError: String?,
    onGoToPage: (Int) -> Unit,
    onOpenBook: (Long) -> Unit,
    onPreviewBookCover: (NovelCard) -> Unit,
    onGridScrollPositionChange: (Int, Int) -> Unit,
    onOpenWeb: () -> Unit
) {
    val overview = discoverOverview(results)
    val searchGridState = rememberRestoredGridState(
        scrollPosition = scrollPosition,
        onPositionChange = onGridScrollPositionChange,
    )
    val searchConfiguration = LocalConfiguration.current
    val availableSearchWidthDp = searchConfiguration.screenWidthDp
    val searchColumnCount = sourceSearchGridColumnCount(availableSearchWidthDp)
    val searchColumns = GridCells.Fixed(searchColumnCount)
    // The grid width is fixed by the viewport and screen padding. Calculate its tag rail once
    // instead of subcomposing every result row through BoxWithConstraints while the user flings.
    val searchGridTagContentWidth = searchGridTagContentWidthDp(
        availableWidthDp = availableSearchWidthDp,
        columnCount = searchColumnCount,
    )
    val searchGridCoverHeight = searchGridCoverHeightDp(
        availableWidthDp = availableSearchWidthDp,
        columnCount = searchColumnCount,
    ).dp
    val sectionOrder = discoverSectionOrder(
        results = results,
        hasHistory = searchHistory.isNotEmpty(),
        advancedSyntaxEnabled = options.advancedSyntaxEnabled
    )
    val visibleBookIds by remember(searchGridState) {
        derivedStateOf {
            searchGridState.layoutInfo.visibleItemsInfo.flatMap { item ->
                when (val key = item.key) {
                    is Long -> listOf(key)
                    else -> searchGridRowBookIds(key)
                }
            }
        }
    }
    // Do not let off-screen downloads compete with the two cards visible on a fresh search.
    // The grid begins with the search panel, filter panel, and pagination header; after those
    // items have scrolled away, speculative work helps the next row without delaying the first.
    val allowScrollCoverPreload by remember(searchGridState) {
        derivedStateOf { searchGridState.firstVisibleItemIndex > 2 }
    }
    val scrollCoverPreloadUrls = when (results) {
        is LoadResult.Success -> searchCoverPreloadUrlsAfterVisible(
            books = results.value,
            visibleBookIds = visibleBookIds,
            columnCount = searchColumnCount,
            allowSpeculativePreload = allowScrollCoverPreload
        )
        else -> emptyList()
    }
    val context = LocalContext.current
    val initialCoverPreloadUrls = when (results) {
        is LoadResult.Success -> searchInitialCoverPreloadUrls(
            books = results.value,
            columnCount = searchColumnCount,
        )
        else -> emptyList()
    }
    LaunchedEffect(initialCoverPreloadUrls) {
        if (initialCoverPreloadUrls.isEmpty()) return@LaunchedEffect
        // The filter panel normally keeps result cards off-screen briefly. Warm a bounded number
        // of responsive grid rows now, so landscape does not inherit a two-card preload budget.
        val prefetches = preloadNovalPieBookCovers(
            context = context,
            urls = initialCoverPreloadUrls,
            maxPreloadCount = initialCoverPreloadUrls.size,
            // The first row is the next content the reader sees after submitting a search. It
            // shares the visible-card dispatcher so a pending warmup cannot hold it in the
            // low-priority scroll queue.
            priority = NovelCoverLoadPriority.Visible,
        )
        try {
            awaitCancellation()
        } finally {
            prefetches.forEach { it.dispose() }
        }
    }
    LaunchedEffect(scrollCoverPreloadUrls) {
        if (scrollCoverPreloadUrls.isEmpty()) return@LaunchedEffect
        // Let the current grid settle before using the proxy/CDN for off-screen covers. Keeping
        // the disposables alive lets a new scroll target cancel unfinished stale work.
        delay(SEARCH_COVER_PRELOAD_SETTLE_MILLIS)
        val prefetches = preloadNovalPieBookCovers(context, scrollCoverPreloadUrls)
        try {
            awaitCancellation()
        } finally {
            prefetches.forEach { it.dispose() }
        }
    }
    // One LazyVerticalGrid, matching the bookshelf. Results use their actual book id as the key,
    // so Compose reuses cells after pagination and the column count can adapt with the window.
    // Every non-result section remains a full-width span in the same continuous scroll.
    LazyVerticalGrid(
        columns = searchColumns,
        state = searchGridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = NovalPieSpacing.screenHorizontal,
            end = NovalPieSpacing.screenHorizontal,
            top = NovalPieSpacing.sm,
            bottom = NovalPieSpacing.listBottom
        ),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)
    ) {
        sectionOrder.forEach { section ->
            when (section) {
                DiscoverSection.SearchPanel -> item(
                    key = "discover-panel",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    DiscoverSearchPanel(
                        overview = overview,
                        keyword = keyword,
                        onKeywordChange = onKeywordChange,
                        // The text field can receive an IME action before the next composition. Submit
                        // through the ViewModel's current state instead of forwarding a captured value.
                        onSearch = { onSearch(null) },
                        advancedSyntaxEnabled = options.advancedSyntaxEnabled,
                        onAdvancedSyntaxEnabledChange = onAdvancedSyntaxEnabledChange,
                        onOpenWeb = onOpenWeb
                    )
                }
                DiscoverSection.Filters -> item(
                    key = "discover-filters",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SourceSearchFilterPanel(
                        options = options,
                        tags = tags,
                        onApplyRequiredTag = onApplyRequiredTag,
                        onApplyBlockedTag = onApplyBlockedTag,
                        onRemoveTag = onRemoveTag,
                        onClearTags = onClearTags,
                        onRefreshTags = onRefreshTags,
                        onSortByChange = onSortByChange,
                        onSortOrderChange = onSortOrderChange,
                        onScopeChange = onScopeChange,
                        onMatchTypeChange = onMatchTypeChange,
                        onAdultFilterChange = onAdultFilterChange,
                        onSourceChange = onSourceChange,
                        onWordCountRangeChange = onWordCountRangeChange,
                        onToggleSettingsCache = onToggleSettingsCache,
                        onClearSettingsCache = onClearSettingsCache
                    )
                }
                DiscoverSection.Results -> searchResultGridItems(
                    keyword = keyword,
                    results = results,
                    viewMode = viewMode,
                    gridColumnCount = searchColumnCount,
                    gridTagContentWidthDp = searchGridTagContentWidth,
                    gridCoverHeight = searchGridCoverHeight,
                    gridFontScale = searchConfiguration.fontScale,
                    searchPage = searchPage,
                    searchLoadingPage = searchLoadingPage,
                    searchPageError = searchPageError,
                    onRetrySearch = { onSearch(null) },
                    onGoToPage = onGoToPage,
                    onToggleViewMode = onToggleViewMode,
                    onOpenBook = onOpenBook,
                    onPreviewBookCover = onPreviewBookCover,
                    onOpenWeb = onOpenWeb
                )
                DiscoverSection.History -> item(
                    key = "discover-history",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SearchHistorySection(
                        history = searchHistory,
                        onUseKeyword = onUseSearchHistory,
                        onClear = onClearSearchHistory
                    )
                }
                DiscoverSection.IdlePrompts -> item(
                    key = "discover-idle",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    DiscoverIdlePanel(
                        onUsePrompt = onKeywordChange,
                        onSearch = onSearch
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.searchResultGridItems(
    keyword: String,
    results: LoadResult<List<NovelCard>>,
    viewMode: SearchViewMode,
    gridColumnCount: Int,
    gridTagContentWidthDp: Int,
    gridCoverHeight: Dp,
    gridFontScale: Float,
    searchPage: SearchPage?,
    searchLoadingPage: Boolean,
    searchPageError: String?,
    onRetrySearch: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onPreviewBookCover: (NovelCard) -> Unit,
    onOpenWeb: () -> Unit
) {
    when (results) {
        LoadResult.Idle -> Unit
        LoadResult.Loading -> item(key = "search-results-loading", span = { GridItemSpan(maxLineSpan) }) {
            LibraryLoadingBlock(if (searchLoadingPage) "正在切换搜索页" else "正在请求 NovalPie 搜索")
        }
        is LoadResult.Error -> item(key = "search-results-error", span = { GridItemSpan(maxLineSpan) }) {
            NpErrorState(
                message = results.message,
                retryLabel = if (searchPage != null) "重试此页" else retryActionLabel("搜索"),
                onRetry = { searchPage?.let { onGoToPage(it.page) } ?: onRetrySearch() },
                secondaryLabel = "打开网页",
                onSecondary = { onOpenWeb() }
            )
        }
        is LoadResult.Success -> {
            searchPage?.let { page ->
                item(key = "search-results-pagination-top", span = { GridItemSpan(maxLineSpan) }) {
                    SearchPaginationBar(
                        keyword = keyword,
                        page = page,
                        viewMode = viewMode,
                        showHeading = true,
                        loading = searchLoadingPage,
                        onGoToPage = onGoToPage,
                        onToggleViewMode = onToggleViewMode
                    )
                }
            }
            if (results.value.isEmpty()) {
                item(key = "search-results-empty", span = { GridItemSpan(maxLineSpan) }) {
                    NpEmptyState(title = "没有找到搜索结果")
                }
            } else {
                if (viewMode == SearchViewMode.Grid) {
                        items(
                            items = results.value.chunked(gridColumnCount),
                            key = { row -> searchGridRowKey(row.map(NovelCard::id)) },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { rowBooks ->
                        val rowTagLineCount = searchGridRowTagLineCount(
                            books = rowBooks,
                            availableTagWidthDp = gridTagContentWidthDp,
                            fontScale = gridFontScale,
                        )
                        val rowMetricLineCount = searchGridRowMetricLineCount(
                            books = rowBooks,
                            availableMetricWidthDp = gridTagContentWidthDp,
                            fontScale = gridFontScale,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
                            verticalAlignment = Alignment.Top,
                        ) {
                            rowBooks.forEach { book ->
                                NovelCardItem(
                                    book = book,
                                    modifier = Modifier.weight(1f),
                                    gridTagLineCount = rowTagLineCount,
                                    gridMetricLineCount = rowMetricLineCount,
                                    gridCoverHeight = gridCoverHeight,
                                    onPreview = { onPreviewBookCover(book) },
                                    onClick = { onOpenBook(book.id) },
                                )
                            }
                            repeat(gridColumnCount - rowBooks.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(
                        items = results.value,
                        key = { book -> book.id },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { book ->
                        NovelSearchListItem(
                            book = book,
                            onPreview = { onPreviewBookCover(book) },
                            onClick = { onOpenBook(book.id) },
                        )
                    }
                }
                searchPageError?.let { pageError ->
                    item(key = "search-results-loadmore-error", span = { GridItemSpan(maxLineSpan) }) {
                        NpErrorState(
                            message = pageError,
                            retryLabel = "重试此页",
                            onRetry = { searchPage?.let { onGoToPage(it.page) } },
                        )
                    }
                }
            }
            searchPage?.let { page ->
                item(key = "search-results-pagination-bottom", span = { GridItemSpan(maxLineSpan) }) {
                    SearchPaginationBar(
                        keyword = keyword,
                        page = page,
                        viewMode = viewMode,
                        showHeading = false,
                        loading = searchLoadingPage,
                        onGoToPage = onGoToPage,
                        onToggleViewMode = onToggleViewMode
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPaginationBar(
    keyword: String,
    page: SearchPage,
    viewMode: SearchViewMode,
    showHeading: Boolean,
    loading: Boolean,
    onGoToPage: (Int) -> Unit,
    onToggleViewMode: () -> Unit
) {
    val window = searchPaginationWindow(page)
    var jumpValue by remember(page.page, window.totalPages) { mutableStateOf("") }
    var jumpDialogVisible by remember { mutableStateOf(false) }
    val jumpTarget = searchPageJumpTarget(jumpValue, window.totalPages)

    if (jumpDialogVisible) {
        AlertDialog(
            onDismissRequest = { jumpDialogVisible = false },
            title = { Text("跳转页码") },
            text = {
                OutlinedTextField(
                    value = jumpValue,
                    onValueChange = { value -> jumpValue = value.filter(Char::isDigit).take(7) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("页码（共 ${window.totalPages} 页）") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (!loading && jumpTarget != null) {
                                jumpDialogVisible = false
                                onGoToPage(jumpTarget)
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !loading && jumpTarget != null,
                    onClick = {
                        jumpTarget?.let { target ->
                            jumpDialogVisible = false
                            onGoToPage(target)
                        }
                    }
                ) { Text("跳转") }
            },
            dismissButton = { TextButton(onClick = { jumpDialogVisible = false }) { Text("取消") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
    ) {
        if (showHeading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${searchResultsHeading(keyword, page)} (${searchResultsCountLabel(page)})",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onToggleViewMode) {
                    val listMode = viewMode == SearchViewMode.List
                    Icon(
                        imageVector = if (listMode) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = if (listMode) "切换到网格视图" else "切换到列表视图"
                    )
                }
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "search-page-previous") {
                SearchCompactPagerCell(
                    label = "‹",
                    contentDescription = "上一页",
                    enabled = !loading && window.previousPage != null,
                    onClick = { window.previousPage?.let(onGoToPage) }
                )
            }
            items(window.pages, key = { number -> "search-page-$number" }) { number ->
                SearchCompactPagerCell(
                    label = number.toString(),
                    contentDescription = "第 $number 页",
                    selected = number == window.currentPage,
                    enabled = !loading,
                    onClick = { onGoToPage(number) }
                )
            }
            item(key = "search-page-next") {
                SearchCompactPagerCell(
                    label = "›",
                    contentDescription = "下一页",
                    enabled = !loading && window.nextPage != null,
                    onClick = { window.nextPage?.let(onGoToPage) }
                )
            }
            item(key = "search-page-jump") {
                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clickable(enabled = !loading) { jumpDialogVisible = true },
                    shape = RoundedCornerShape(NovalPieRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = NovalPieSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
                    ) {
                        Text("共 ${window.totalPages} 页", style = MaterialTheme.typography.labelSmall)
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "跳转页码",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            if (!showHeading) {
                item(key = "search-page-view-mode") {
                    IconButton(onClick = onToggleViewMode, modifier = Modifier.size(32.dp)) {
                        val listMode = viewMode == SearchViewMode.List
                        Icon(
                            imageVector = if (listMode) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (listMode) "切换到网格视图" else "切换到列表视图",
                            modifier = Modifier.size(NovalPieSize.iconSm)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCompactPagerCell(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(NovalPieRadius.sm),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(
            NovalPieSize.hairline,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private class SearchFilterPage(val label: String)

private val searchFilterRules = SearchFilterPage("规则")
private val searchFilterTags = SearchFilterPage("标签")
private val searchFilterWordCount = SearchFilterPage("字数")
private val searchFilterPages = listOf(searchFilterRules, searchFilterTags, searchFilterWordCount)

/**
 * Mobile source-style filter rail. The website keeps the controls visually quiet: tabs and
 * utilities form one thin row, while rule selectors are short left-aligned label/select pairs.
 * Keeping every rule present here avoids trading source capability for a nicer first screen.
 */
@Composable
private fun SourceSearchFilterPanel(
    options: SearchOptions,
    tags: LoadResult<List<NovelTag>>,
    onApplyRequiredTag: (String) -> Unit,
    onApplyBlockedTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onClearTags: () -> Unit,
    onRefreshTags: () -> Unit,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit,
    onToggleSettingsCache: () -> Unit,
    onClearSettingsCache: () -> Unit
) {
    var page by remember { mutableStateOf(searchFilterRules) }
    var showUsage by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmClearCache by remember { mutableStateOf(false) }

    if (showUsage) {
        AlertDialog(
            onDismissRequest = { showUsage = false },
            title = { Text("搜索使用说明") },
            text = {
                Text(
                        "规则页可筛选搜索范围、内容筛选、来源、搜索模式、排序方式和排序方向；标签页支持包含和屏蔽标签；字数页按作品字数缩小结果。"
                )
            },
            confirmButton = { TextButton(onClick = { showUsage = false }) { Text("知道了") } }
        )
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("搜索设置") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("缓存搜索设置", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (options.cacheEnabled) "下次打开 App 时恢复当前设置" else "本次设置不会在下次启动时恢复",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = options.cacheEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled != options.cacheEnabled) onToggleSettingsCache()
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("完成") } }
        )
    }
    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            title = { Text("清除搜索设置缓存") },
            text = { Text("只清除本地筛选、标签和视图设置；搜索历史与网站数据不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearCache = false
                    onClearSettingsCache()
                }) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = { confirmClearCache = false }) { Text("取消") } }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.sm),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.none,
        border = BorderStroke(
            NovalPieSize.hairline,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = NovalPieSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchFilterTabBar(
                    selected = page,
                    onSelected = { page = it },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showUsage = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.HelpOutline,
                        contentDescription = "搜索使用说明",
                        modifier = Modifier.size(NovalPieSize.iconSm)
                    )
                }
                IconButton(onClick = { showSettings = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "搜索设置",
                        modifier = Modifier.size(NovalPieSize.iconSm)
                    )
                }
                IconButton(onClick = onToggleSettingsCache, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Block,
                        contentDescription = if (options.cacheEnabled) "不缓存搜索设置" else "缓存搜索设置",
                        tint = if (options.cacheEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(NovalPieSize.iconSm)
                    )
                }
                IconButton(onClick = { confirmClearCache = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "清除搜索设置缓存",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(NovalPieSize.iconSm)
                    )
                }
            }

            when (page) {
                searchFilterRules -> SourceSearchOptionSection(
                    options = options,
                    onSortByChange = onSortByChange,
                    onSortOrderChange = onSortOrderChange,
                    onScopeChange = onScopeChange,
                    onMatchTypeChange = onMatchTypeChange,
                    onAdultFilterChange = onAdultFilterChange,
                    onSourceChange = onSourceChange,
                )

                searchFilterTags -> SearchTagFilterSection(
                    options = options,
                    tags = tags,
                    onApplyRequiredTag = onApplyRequiredTag,
                    onApplyBlockedTag = onApplyBlockedTag,
                    onRemoveTag = onRemoveTag,
                    onClearTags = onClearTags,
                    onRefresh = onRefreshTags
                )

                searchFilterWordCount -> SearchWordCountSection(
                    options = options,
                    onWordCountRangeChange = onWordCountRangeChange
                )
            }
        }
    }
}

/**
 * Mirrors the source site's flex-wrapped rule row: phones keep one dependable selector per line,
 * while landscape/tablet space is used for paired controls instead of a large dead right column.
 */
@Composable
private fun SourceSearchOptionSection(
    options: SearchOptions,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
) {
    val primaryRules = sourcePrimarySearchFilterGroups(options)
    val scope = primaryRules[0]
    val contentFilter = primaryRules[1]
    val source = primaryRules[2]
    val matchType = primaryRules[3]
    val sortBy = primaryRules[4]
    val sortOrder = primaryRules[5]

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // A pair needs enough room for the longest source label and a usable dropdown. Below this
        // breakpoint, keeping the existing single-column row prevents small Android phones from
        // truncating the content-rating filter that exposes the adult-only option.
        if (maxWidth >= SOURCE_SEARCH_RULES_TWO_COLUMN_MIN_WIDTH_DP.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SourceSearchRulePairRow(scope, onScopeChange, contentFilter, onAdultFilterChange)
                SourceSearchRulePairRow(source, onSourceChange, matchType, onMatchTypeChange)
                SourceSearchRulePairRow(sortBy, onSortByChange, sortOrder, onSortOrderChange)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SourceSearchRuleSelectRow(scope, onScopeChange)
                SourceSearchRuleSelectRow(contentFilter, onAdultFilterChange)
                SourceSearchRuleSelectRow(source, onSourceChange)
                SourceSearchRuleSelectRow(matchType, onMatchTypeChange)
                SourceSearchRuleSelectRow(sortBy, onSortByChange)
                SourceSearchRuleSelectRow(sortOrder, onSortOrderChange, emphasized = true)
            }
        }
    }
}

/** Keeps the website's compact two-control row without hiding either source search field. */
@Composable
private fun SourceSearchRulePairRow(
    first: DiscoverFilterGroup,
    onFirstSelected: (String) -> Unit,
    second: DiscoverFilterGroup,
    onSecondSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SOURCE_SEARCH_RULE_ROW_HEIGHT_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceSearchRulePairCell(
            group = first,
            onSelected = onFirstSelected,
            modifier = Modifier.weight(1f),
        )
        SourceSearchRulePairCell(
            group = second,
            onSelected = onSecondSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SourceSearchRulePairCell(
    group: DiscoverFilterGroup,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.heightIn(min = SOURCE_SEARCH_RULE_ROW_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${group.label}：",
            modifier = Modifier.width(SOURCE_SEARCH_RULE_PAIR_LABEL_WIDTH_DP.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SourceSearchRuleSelectControl(
            group = group,
            onSelected = onSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SourceSearchRuleSelectRow(
    group: DiscoverFilterGroup,
    onSelected: (String) -> Unit,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SOURCE_SEARCH_RULE_ROW_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${group.label}：",
            modifier = Modifier.width(SOURCE_SEARCH_RULE_LABEL_WIDTH_DP.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SourceSearchRuleSelectControl(
            group = group,
            onSelected = onSelected,
            emphasized = emphasized,
            modifier = Modifier.width(
                if (emphasized) 96.dp else SOURCE_SEARCH_RULE_CONTROL_WIDTH_DP.dp
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SourceSearchRuleSelectControl(
    group: DiscoverFilterGroup,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    var expanded by remember(group.label) { mutableStateOf(false) }
    val selected = group.choices.firstOrNull { it.selected } ?: group.choices.first()
    Box(modifier = modifier) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SOURCE_SEARCH_RULE_ROW_HEIGHT_DP.dp)
                    .semantics { contentDescription = "${group.label}: ${selected.label}" }
                    .clickable { expanded = true },
                shape = RoundedCornerShape(NovalPieRadius.xs),
                color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (emphasized) null else BorderStroke(
                    NovalPieSize.hairline,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
                ) {
                    Text(
                        selected.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                group.choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.label) },
                        onClick = {
                            expanded = false
                            if (choice.value != selected.value) onSelected(choice.value)
                        }
                    )
                }
            }
    }
}

@Composable
private fun SearchFilterPanel(
    options: SearchOptions,
    tags: LoadResult<List<NovelTag>>,
    onApplyRequiredTag: (String) -> Unit,
    onApplyBlockedTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onClearTags: () -> Unit,
    onRefreshTags: () -> Unit,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit,
    onToggleSettingsCache: () -> Unit,
    onClearSettingsCache: () -> Unit
) {
    var page by remember { mutableStateOf(searchFilterRules) }
    // Keep the first search surface aligned with the mobile source: rules, tags, and word count
    // are immediately discoverable instead of being hidden behind an app-only accordion.
    var expanded by remember { mutableStateOf(SOURCE_SEARCH_FILTERS_START_EXPANDED) }
    var showUsage by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmClearCache by remember { mutableStateOf(false) }
    if (showUsage) {
        AlertDialog(
            onDismissRequest = { showUsage = false },
            title = { Text("搜索使用说明") },
            text = {
                Text(
                    "规则页可筛选范围、来源、匹配模式与排序；标签页支持包含和屏蔽标签；字数页按作品字数缩小结果。启用高级语法后可直接输入 tag:、NOT tag:、word: 等源站语法。"
                )
            },
            confirmButton = { TextButton(onClick = { showUsage = false }) { Text("知道了") } }
        )
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("搜索设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
                    Text("搜索筛选、标签和视图偏好只保存在本机，不会写入网站。")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("缓存搜索设置", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (options.cacheEnabled) "下次打开 App 时恢复当前设置" else "本次设置不会在下次启动时恢复",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = options.cacheEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled != options.cacheEnabled) onToggleSettingsCache()
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("完成") } }
        )
    }
    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            title = { Text("清除搜索设置缓存") },
            text = { Text("只会清除本机保存的筛选、标签和视图设置；搜索历史与网站数据不会受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearCache = false
                    onClearSettingsCache()
                }) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = { confirmClearCache = false }) { Text("取消") } }
        )
    }
    val selectedTagCount = options.requiredTags.size + options.blockedTags.size
    val summary = buildList {
        if (options.scope != "all") add("已限定范围")
        if (options.source.isNotBlank()) add("已限定来源")
        if (options.adultFilter != "unrestricted") add("内容筛选")
        if (options.sortBy != "relevance") add("已调整排序")
        if (selectedTagCount > 0) add("$selectedTagCount 个标签")
    }.joinToString(" · ").ifBlank { "规则、标签和字数筛选" }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.sm),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.none,
        border = BorderStroke(
            NovalPieSize.hairline,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
            ) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(NovalPieSize.iconMd),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("筛选", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .height(36.dp)
                        .semantics {
                        contentDescription = if (expanded) "收起搜索筛选" else "展开搜索筛选"
                        },
                    contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp)
                ) {
                    Text(if (expanded) "收起" else "筛选")
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(NovalPieSize.iconSm)
                    )
                }
            }
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchFilterTabBar(
                        selected = page,
                        onSelected = { page = it },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showUsage = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.HelpOutline,
                            contentDescription = "搜索使用说明",
                            modifier = Modifier.size(NovalPieSize.iconSm)
                        )
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "搜索设置",
                            modifier = Modifier.size(NovalPieSize.iconSm)
                        )
                    }
                    IconButton(
                        onClick = onToggleSettingsCache,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Block,
                            contentDescription = if (options.cacheEnabled) "不缓存搜索设置" else "缓存搜索设置",
                            tint = if (options.cacheEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(NovalPieSize.iconSm)
                        )
                    }
                    IconButton(
                        onClick = { confirmClearCache = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "清除搜索设置缓存",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(NovalPieSize.iconSm)
                        )
                    }
                }
                when (page) {
                    searchFilterRules -> SearchOptionSection(
                        options = options,
                        onSortByChange = onSortByChange,
                        onSortOrderChange = onSortOrderChange,
                        onScopeChange = onScopeChange,
                        onMatchTypeChange = onMatchTypeChange,
                        onAdultFilterChange = onAdultFilterChange,
                        onSourceChange = onSourceChange,
                        onWordCountRangeChange = onWordCountRangeChange,
                        includeWordCount = false
                    )

                    searchFilterTags -> SearchTagFilterSection(
                        options = options,
                        tags = tags,
                        onApplyRequiredTag = onApplyRequiredTag,
                        onApplyBlockedTag = onApplyBlockedTag,
                        onRemoveTag = onRemoveTag,
                        onClearTags = onClearTags,
                        onRefresh = onRefreshTags
                    )

                    searchFilterWordCount -> SearchWordCountSection(
                        options = options,
                        onWordCountRangeChange = onWordCountRangeChange
                    )
                }
            }
        }
    }
}

/** Source-like text tabs keep the mobile filter rail compact without hiding a filter page. */
@Composable
private fun SearchFilterTabBar(
    selected: SearchFilterPage,
    onSelected: (SearchFilterPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
    ) {
        searchFilterPages.forEach { candidate ->
            val isSelected = candidate == selected
            TextButton(
                onClick = { onSelected(candidate) },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        candidate.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .height(2.dp)
                            .width(24.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun LegacySearchFilterPanel(
    options: SearchOptions,
    tags: LoadResult<List<NovelTag>>,
    onApplyRequiredTag: (String) -> Unit,
    onApplyBlockedTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onClearTags: () -> Unit,
    onRefreshTags: () -> Unit,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit
) {
    var page by remember { mutableStateOf(searchFilterRules) }
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        NpChipRow {
            searchFilterPages.forEach { candidate ->
                FilterChip(
                    selected = page == candidate,
                    onClick = { page = candidate },
                    label = { Text(candidate.label) }
                )
            }
        }
        when (page) {
            searchFilterRules -> SearchOptionSection(
                options = options,
                onSortByChange = onSortByChange,
                onSortOrderChange = onSortOrderChange,
                onScopeChange = onScopeChange,
                onMatchTypeChange = onMatchTypeChange,
                onAdultFilterChange = onAdultFilterChange,
                onSourceChange = onSourceChange,
                onWordCountRangeChange = onWordCountRangeChange,
                includeWordCount = false
            )

            searchFilterTags -> SearchTagFilterSection(
                options = options,
                tags = tags,
                onApplyRequiredTag = onApplyRequiredTag,
                onApplyBlockedTag = onApplyBlockedTag,
                onRemoveTag = onRemoveTag,
                onClearTags = onClearTags,
                onRefresh = onRefreshTags
            )

            searchFilterWordCount -> SearchWordCountSection(
                options = options,
                onWordCountRangeChange = onWordCountRangeChange
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun SearchTagFilterSection(
    options: SearchOptions,
    tags: LoadResult<List<NovelTag>>,
    onApplyRequiredTag: (String) -> Unit,
    onApplyBlockedTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onClearTags: () -> Unit,
    onRefresh: () -> Unit
) {
    var mode by remember { mutableStateOf(SearchTagFilterMode.Required) }
    var tagInput by remember { mutableStateOf("") }
    var visibleTagCount by remember { mutableIntStateOf(24) }
    val hasSelectedTags = options.requiredTags.isNotEmpty() || options.blockedTags.isNotEmpty()
    val applyInput = {
        if (tagInput.isNotBlank()) {
            if (mode == SearchTagFilterMode.Required) onApplyRequiredTag(tagInput) else onApplyBlockedTag(tagInput)
            tagInput = ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
        ) {
            FilterChip(
                selected = mode == SearchTagFilterMode.Required,
                onClick = { mode = SearchTagFilterMode.Required },
                label = { Text("包含") }
            )
            FilterChip(
                selected = mode == SearchTagFilterMode.Blocked,
                onClick = { mode = SearchTagFilterMode.Blocked },
                label = { Text("屏蔽") }
            )
            Spacer(modifier = Modifier.weight(1f))
            if (hasSelectedTags) {
                TextButton(onClick = onClearTags) { Text("清除标签") }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("输入标签") },
                placeholder = { Text("可用逗号分隔多个标签") }
            )
            Button(onClick = applyInput, enabled = tagInput.isNotBlank()) {
                Text("添加")
            }
        }
        if (hasSelectedTags) {
            Text("当前筛选", style = MaterialTheme.typography.titleSmall)
            NpChipRow {
                options.requiredTags.forEach { tag ->
                    FilterChip(
                        selected = true,
                        onClick = { onRemoveTag(tag) },
                        label = { Text("包含 $tag ×") }
                    )
                }
                options.blockedTags.forEach { tag ->
                    FilterChip(
                        selected = true,
                        onClick = { onRemoveTag(tag) },
                        label = { Text("屏蔽 $tag ×") }
                    )
                }
            }
        }
        NpSectionHeader(title = "热门标签", actionLabel = "刷新", onAction = onRefresh)
        Text(
            "选择后立即按网站标签条件刷新结果。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (tags) {
            LoadResult.Idle -> LibraryStatusLine("打开发现页后同步网站标签")
            LoadResult.Loading -> NpSkeleton(height = NovalPieSize.minTouchTarget, widthFraction = 0.7f)
            is LoadResult.Error -> NpErrorState(
                message = tags.message,
                retryLabel = "重试标签",
                onRetry = onRefresh
            )

            is LoadResult.Success -> {
                if (tags.value.isEmpty()) {
                    LibraryStatusLine("暂无可显示标签")
                } else {
                    NpChipRow {
                        tags.value.take(visibleTagCount).forEach { tag ->
                            val selected = when (mode) {
                                SearchTagFilterMode.Required -> options.requiredTags.containsSearchTag(tag.name)
                                SearchTagFilterMode.Blocked -> options.blockedTags.containsSearchTag(tag.name)
                            }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (mode == SearchTagFilterMode.Required) onApplyRequiredTag(tag.name)
                                    else onApplyBlockedTag(tag.name)
                                },
                                label = { Text(discoverTagLabels(listOf(tag)).single()) }
                            )
                        }
                    }
                    if (tags.value.size > visibleTagCount) {
                        TextButton(
                            onClick = {
                                visibleTagCount = (visibleTagCount + 24).coerceAtMost(tags.value.size)
                            }
                        ) {
                            Text("显示更多热门标签")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchWordCountSection(
    options: SearchOptions,
    onWordCountRangeChange: (String) -> Unit
) {
    val group = discoverFilterGroups(options).single { it.label == "字数" }
    FilterChoiceRail(group = group, onSelected = onWordCountRangeChange)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun DiscoverIdlePanel(
    onUsePrompt: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        Text(
            discoverIdleMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NpChipRow {
            discoverQuickPrompts().forEach { prompt ->
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

@Composable
private fun DiscoverSearchPanel(
    overview: DiscoverOverview,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    advancedSyntaxEnabled: Boolean,
    onAdvancedSyntaxEnabledChange: (Boolean) -> Unit,
    onOpenWeb: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.sm),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.card,
        border = BorderStroke(
            NovalPieSize.hairline,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("搜索小说", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "高级语法模式",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SourceCompactSwitch(
                    checked = advancedSyntaxEnabled,
                    onCheckedChange = onAdvancedSyntaxEnabledChange,
                    contentDescription = "高级语法模式",
                )
                IconButton(
                    onClick = onOpenWeb,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.OpenInBrowser,
                        contentDescription = "打开网页搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(NovalPieSize.iconMd)
                    )
                }
            }
            SourceSearchInput(
                value = keyword,
                onValueChange = onKeywordChange,
                onSearch = onSearch,
                placeholder = if (advancedSyntaxEnabled) {
                    "输入高级搜索语法，例如 tag:恋爱 word:10w..50w"
                } else {
                    overview.hint
                }
            )
            if (advancedSyntaxEnabled) {
                AdvancedSearchSyntaxHint(onUseExample = onKeywordChange)
            }
        }
    }
}

/** A 36x20 visual switch in a 44x36 hit target, matching the compact source-search header. */
@Composable
private fun SourceCompactSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .semantics {
                this.contentDescription = "$contentDescription：${if (checked) "已开启" else "已关闭"}"
            }
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 36.dp, height = 20.dp),
            shape = RoundedCornerShape(NovalPieRadius.pill),
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(
                NovalPieSize.hairline,
                if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                Surface(
                    modifier = Modifier
                        .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                        .size(16.dp),
                    shape = CircleShape,
                    color = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {}
            }
        }
    }
}

/** Compact mobile source-style search field with an explicit action and IME submission. */
@Composable
private fun SourceSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var submissionPending by remember { mutableStateOf(false) }

    fun submitSearch() {
        if (submissionPending) return
        submissionPending = true
        focusManager.clearFocus()
        scope.launch {
            // Android can deliver the IME action before the preceding text commit reaches Compose.
            // A short frame-sized settle keeps the search tied to the text the user can see.
            delay(SOURCE_SEARCH_SUBMISSION_SETTLE_MILLIS)
            submissionPending = false
            onSearch()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceCompactSearchField(
            value = value,
            onValueChange = onValueChange,
            onSearch = ::submitSearch,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = ::submitSearch,
            enabled = !submissionPending,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(NovalPieRadius.xs),
            border = BorderStroke(NovalPieSize.hairline, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Text("搜索", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * Material's outlined field has a 56dp internal minimum. The source form is visually 40dp, so a
 * small native field keeps the correct baseline and touch target without clipping its placeholder.
 */
@Composable
private fun SourceCompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(NovalPieRadius.xs)
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.height(40.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            NovalPieSize.hairline,
            if (focused) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(NovalPieSize.iconSm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = "搜索关键词" },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearch()
                    },
                ),
                onTextLayout = {},
                interactionSource = null,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                },
            )
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "清除搜索关键词",
                        modifier = Modifier.size(NovalPieSize.iconSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegacyDiscoverSearchPanel(
    overview: DiscoverOverview,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: (String?) -> Unit,
    advancedSyntaxEnabled: Boolean,
    onAdvancedSyntaxEnabledChange: (Boolean) -> Unit,
    onOpenWeb: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        // One search input, the same NpSearchField the bookshelf uses, replacing the OutlinedTextField
        // with a floating label -- two adjacent tabs no longer offer two different-looking search
        // boxes. Submitting runs the search and dismisses the keyboard. The screen is named once, by
        // the top bar and the tab, so the old titleLarge 发现 heading that pushed results below the
        // fold is gone.
        NpSearchField(
            value = keyword,
            onValueChange = onKeywordChange,
            onSearch = { onSearch(keyword) },
            placeholder = if (advancedSyntaxEnabled) {
                "使用语法搜索，例如：tag:恋爱 word:10w..50w"
            } else {
                overview.hint
            },
            clearContentDescription = "清除关键词"
        )
        // Live search status and the web-search escape hatch on one compact line.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            NpChip(label = overview.statusLabel, tone = NpChipTone.Neutral)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onOpenWeb,
                modifier = Modifier.size(NovalPieSize.minTouchTarget)
            ) {
                Icon(
                    Icons.Filled.OpenInBrowser,
                    contentDescription = "打开网页搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(NovalPieSize.iconLg)
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            shape = RoundedCornerShape(NovalPieRadius.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovalPieSpacing.md, vertical = NovalPieSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("高级语法", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (advancedSyntaxEnabled) {
                            "按网站高级规则解析；普通筛选面板已隐藏。"
                        } else {
                            "启用后可直接输入标签、范围和排除条件。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = advancedSyntaxEnabled,
                    onCheckedChange = onAdvancedSyntaxEnabledChange,
                    modifier = Modifier.semantics { contentDescription = "高级语法模式" }
                )
            }
        }
        if (advancedSyntaxEnabled) {
            AdvancedSearchSyntaxHint(onUseExample = onKeywordChange)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdvancedSearchSyntaxHint(onUseExample: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val commonExamples = listOf(
        "tag:奇幻 NOT tag:后宫",
        "@title:魔法 学院 NOT 续作"
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            Text("源站高级搜索", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "点击示例填入搜索框。关键词默认 AND，NOT 用于排除。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            NpChipRow {
                commonExamples.forEach { example ->
                    AssistChip(
                        onClick = { onUseExample(example) },
                        label = { Text(example, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起完整语法" else "查看完整语法")
            }
            if (expanded) {
                NpChipRow {
                    listOf(
                        "in:author 叶轻灵",
                        "word:10w..50w",
                        "platform:novelPia type:玄幻 status:连载"
                    ).forEach { example ->
                        AssistChip(
                            onClick = { onUseExample(example) },
                            label = { Text(example, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { onUseExample("tag:(恋爱 AND 校园) OR 轻小说") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "标签表达式：tag:(恋爱 AND 校园) OR 轻小说",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "可用：in:title / in:author / in:tags、#标签、-tag:标签、adult:only、match:strict / loose / exact；标签支持 AND / OR。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SearchHistorySection(
    history: List<String>,
    onUseKeyword: (String) -> Unit,
    onClear: () -> Unit
) {
    Column {
        NpSectionHeader(title = "搜索历史", actionLabel = "清空", onAction = onClear)
        // Wrapping row of recent keywords, not a LazyRow that scrolled the oldest ones off-screen.
        NpChipRow {
            history.forEach { keyword ->
                FilterChip(
                    selected = false,
                    onClick = { onUseKeyword(keyword) },
                    label = { Text(keyword, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
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
    onWordCountRangeChange: (String) -> Unit,
    includeWordCount: Boolean = true
) {
    // `discoverFilterGroups` owns the source vocabulary. Pair controls as the mobile website's
    // wrapping flex row does, so all source options remain visible without a tall settings form.
    val groups = discoverFilterGroups(options)
    val sortBy = groups[0]
    val sortOrder = groups[1]
    val scope = groups[2]
    val adult = groups[3]
    val wordCount = groups[4]
    val source = groups[5]
    val matchType = groups[6]

    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
        SearchRuleSelectPair(scope, onScopeChange, adult, onAdultFilterChange)
        SearchRuleSelectPair(source, onSourceChange, matchType, onMatchTypeChange)
        SearchRuleSelectPair(sortBy, onSortByChange, sortOrder, onSortOrderChange)
        if (includeWordCount) {
            SearchRuleSelectRow(wordCount, onWordCountRangeChange)
        }
    }
}

@Composable
private fun SearchRuleSelectPair(
    first: DiscoverFilterGroup,
    onFirstSelected: (String) -> Unit,
    second: DiscoverFilterGroup,
    onSecondSelected: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (sourceSearchRulesUseTwoColumns(maxWidth.value.toInt())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
            ) {
                SearchRuleSelectRow(
                    group = first,
                    onSelected = onFirstSelected,
                    modifier = Modifier.weight(1f),
                    labelWidth = SOURCE_SEARCH_RULE_PAIR_LABEL_WIDTH_DP.dp,
                )
                SearchRuleSelectRow(
                    group = second,
                    onSelected = onSecondSelected,
                    modifier = Modifier.weight(1f),
                    labelWidth = SOURCE_SEARCH_RULE_PAIR_LABEL_WIDTH_DP.dp,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                SearchRuleSelectRow(
                    group = first,
                    onSelected = onFirstSelected,
                    labelWidth = SOURCE_SEARCH_RULE_LABEL_WIDTH_DP.dp,
                )
                SearchRuleSelectRow(
                    group = second,
                    onSelected = onSecondSelected,
                    labelWidth = SOURCE_SEARCH_RULE_LABEL_WIDTH_DP.dp,
                )
            }
        }
    }
}

/** Compact source selector: stacked on phone widths and paired only on wider windows. */
@Composable
private fun SearchRuleSelectRow(
    group: DiscoverFilterGroup,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelWidth: Dp? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = group.choices.firstOrNull { it.selected } ?: group.choices.first()
    val resolvedLabelWidth = labelWidth ?: SOURCE_SEARCH_RULE_LABEL_WIDTH_DP.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SOURCE_SEARCH_RULE_ROW_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
    ) {
        Text(
            "${group.label}：",
            modifier = Modifier.width(resolvedLabelWidth),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(modifier = Modifier.width(SOURCE_SEARCH_RULE_CONTROL_WIDTH_DP.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "${group.label}: ${selected.label}" }
                    .clickable { expanded = true },
                shape = RoundedCornerShape(NovalPieRadius.sm),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(NovalPieSize.hairline, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = NovalPieSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                ) {
                    Text(
                        selected.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(NovalPieSize.iconSm),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                group.choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.label) },
                        onClick = {
                            expanded = false
                            onSelected(choice.value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacySearchOptionSection(
    options: SearchOptions,
    onSortByChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAdultFilterChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onWordCountRangeChange: (String) -> Unit,
    includeWordCount: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)) {
        NpSectionHeader(title = "规则")
        // The callback is chosen by the Chinese group label, so the labels below and in
        // discoverFilterGroups must stay in lockstep -- renaming one silently rewires the screen.
        discoverFilterGroups(options)
            .filter { includeWordCount || it.label != "字数" }
            .forEach { group ->
            FilterChoiceRail(
                group = group,
                onSelected = when (group.label) {
                    "排序方式" -> onSortByChange
                    "排序方向" -> onSortOrderChange
                    "搜索范围" -> onScopeChange
                    "内容筛选" -> onAdultFilterChange
                    "字数" -> onWordCountRangeChange
                    "来源" -> onSourceChange
                    "搜索模式" -> onMatchTypeChange
                    else -> onMatchTypeChange
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterChoiceRail(group: DiscoverFilterGroup, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
        Text(
            group.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        // FlowRow, not LazyRow: this is the row that clipped "字…" through the glyph at the screen
        // edge with nothing to say more choices existed.
        NpChipRow {
            group.choices.forEach { choice ->
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

private enum class BookDetailContentTab {
    Introduction,
    Catalog,
    Comments
}

@Composable
private fun BookDetailContentTabBar(
    selected: BookDetailContentTab,
    chapterCount: Int,
    onSelected: (BookDetailContentTab) -> Unit
) {
    val tabs = listOf(
        BookDetailContentTab.Introduction to bookDetailTabLabel(
            BookDetailContentTabLabel.Introduction,
            chapterCount,
        ),
        BookDetailContentTab.Catalog to bookDetailTabLabel(
            BookDetailContentTabLabel.Catalog,
            chapterCount,
        ),
        BookDetailContentTab.Comments to bookDetailTabLabel(
            BookDetailContentTabLabel.Comments,
            chapterCount,
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = tab == selected
            TextButton(
                onClick = { onSelected(tab) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = NovalPieSpacing.xs, vertical = 0.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .height(2.dp)
                            .width(28.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookDetailScreen(
    state: BookDetailState,
    nativeEpubDownloadState: NativeEpubDownloadState,
    hasAuthToken: Boolean,
    readerProgress: ReaderProgress?,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenTerminology: () -> Unit,
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
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
    onDownloadEpub: () -> Unit,
    onDownloadTxt: () -> Unit,
    onPreviewBookCover: (NovelCard) -> Unit,
    onOpenWeb: () -> Unit
) {
    val sectionTitles = bookDetailSectionTitles()
    val loadedChapters = (state.chapters as? LoadResult.Success)?.value.orEmpty()
    val firstChapter = loadedChapters.firstOrNull()
    val sourceChapterCount = (state.book as? LoadResult.Success)?.value
        ?.let { book -> book.chapterCount ?: book.maxChapterNumber }
        ?.coerceAtLeast(0)
        ?: 0
    // A successful directory remains authoritative. If it is temporarily unavailable or empty,
    // preserve the source detail count so the tab and primary action do not misleadingly show 0.
    val chapterCount = bookDetailDisplayedChapterCount(loadedChapters, sourceChapterCount)
    val progressForBook = readerProgress?.takeIf { it.bookId == state.bookId }
    var contentTab by remember(state.bookId) { mutableStateOf(BookDetailContentTab.Introduction) }
    var catalogOrder by remember(state.bookId) {
        mutableStateOf(BookDetailCatalogOrder.Ascending)
    }
    val visibleChapters = (state.chapters as? LoadResult.Success)?.value?.let { chapters ->
        sortBookDetailChapters(filterChapters(chapters, catalogQuery), catalogOrder)
    }.orEmpty()
    val listState = rememberLazyListState()
    LaunchedEffect(state.bookId) {
        listState.scrollToItem(0)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NovalPieSpacing.screenHorizontal,
                end = NovalPieSpacing.screenHorizontal,
                top = NovalPieSpacing.sm,
                // Keep the last chapter/review above the source-style fixed action bar.
                bottom = if (state.book is LoadResult.Success) 112.dp else NovalPieSpacing.listBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)
        ) {
            item {
                when (val book = state.book) {
                    LoadResult.Idle -> LibraryStatusLine("等待加载书籍详情")
                    LoadResult.Loading -> LibraryLoadingBlock("正在加载书籍详情")
                    is LoadResult.Error -> NpErrorState(
                        message = book.message,
                        retryLabel = retryActionLabel("书籍详情"),
                        onRetry = onRetry,
                        secondaryLabel = "打开网页",
                        onSecondary = { onOpenWeb() }
                    )
                    is LoadResult.Success -> BookDetailHero(
                        book = book.value,
                        favoriteStatus = state.favoriteStatus,
                        progress = progressForBook,
                        onPreviewCover = { onPreviewBookCover(book.value) },
                    )
                }
            }
            item {
                (state.book as? LoadResult.Success)?.value?.let { book ->
                    BookDetailStatisticsPanel(book)
                }
            }
            item {
                BookDetailContentTabBar(
                    selected = contentTab,
                    chapterCount = chapterCount,
                    onSelected = {
                        contentTab = it
                    },
                )
            }
            // The source mobile page renders Introduction, Catalog, and Comments in one
            // continuous stream. The tab rail above is an anchor/navigation aid, not a filter
            // that removes the other sections (which previously made reviews appear missing).
            item {
                if (contentTab == BookDetailContentTab.Introduction) {
                    when (val book = state.book) {
                        is LoadResult.Success -> BookDetailIntroduction(
                            book = book.value,
                            canManageBook = bookManagementActionsVisible(
                                (state.managementPermissions as? LoadResult.Success)?.value
                            ),
                            onEditInfo = onEditInfo,
                            onManageChapters = onManageChapters,
                            onAppendChapters = onAppendChapters,
                        )
                        else -> Unit
                    }
                }
            }
            if (contentTab == BookDetailContentTab.Catalog) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                ) {
                    Text(
                        text = bookDetailCatalogHeading(chapterCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = catalogOrder == BookDetailCatalogOrder.Ascending,
                        onClick = { catalogOrder = BookDetailCatalogOrder.Ascending },
                        label = { Text("正序") },
                    )
                    FilterChip(
                        selected = catalogOrder == BookDetailCatalogOrder.Descending,
                        onClick = { catalogOrder = BookDetailCatalogOrder.Descending },
                        label = { Text("倒序") },
                    )
                }
            }
            item { CatalogFilterField(catalogQuery, onCatalogQueryChange) }
            when (val chapters = state.chapters) {
                LoadResult.Idle -> item { LibraryStatusLine("等待加载章节") }
                LoadResult.Loading -> item { LibraryLoadingBlock("正在加载章节目录") }
                is LoadResult.Error -> item {
                    NpErrorState(
                        message = chapters.message,
                        retryLabel = retryActionLabel("章节目录"),
                        onRetry = onRetry
                    )
                }
                is LoadResult.Success -> {
                    item {
                        CatalogSummaryText(
                            if (chapters.value.isEmpty()) {
                                "源站当前未提供可阅读章节"
                            } else {
                                catalogSummaryLabel(
                                    allChapters = chapters.value,
                                    visibleChapters = visibleChapters,
                                    currentChapterId = readerProgress
                                        ?.takeIf { it.bookId == state.bookId }
                                        ?.chapterId
                                )
                            }
                        )
                    }
                    when {
                        chapters.value.isEmpty() -> item {
                            LibraryStatusLine("源站当前暂无可阅读章节。")
                        }
                        visibleChapters.isEmpty() -> item { NpEmptyState(title = "没有匹配的章节") }
                        else -> items(visibleChapters, key = { it.id }) { chapter ->
                            ChapterRow(
                                chapter = chapter,
                                selected = isBookDetailProgressChapter(
                                    state.bookId,
                                    chapter.id,
                                    readerProgress,
                                ),
                                context = ChapterListContext.BookDetail,
                                onClick = { onOpenReader(state.bookId, chapter.id) }
                            )
                        }
                    }
                }
            }
            }
            if (contentTab == BookDetailContentTab.Comments) {
            item {
                BookCommentsSection(
                    title = sectionTitles.getOrElse(3) { "书评" },
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
                    onOpenUser = onOpenUser,
                    onOpenLink = onOpenLink,
                    onOpenWeb = onOpenWeb
                )
            }
            }
        }
        if (state.book is LoadResult.Success) {
            BookDetailBottomActionBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                bookId = state.bookId,
                bookTitle = (state.book as LoadResult.Success).value.title,
                nativeEpubDownloadState = nativeEpubDownloadState,
                chapterCount = chapterCount,
                allowDownload = (state.book as LoadResult.Success).value.allowDownload,
                favoriteStatus = state.favoriteStatus,
                favoriteLoading = state.favoriteLoading,
                progress = progressForBook,
                firstChapter = firstChapter,
                hasAuthToken = hasAuthToken,
                canManageBook = bookManagementActionsVisible(
                    (state.managementPermissions as? LoadResult.Success)?.value
                ),
                onOpenReader = onOpenReader,
                onToggleFavorite = onToggleFavorite,
                onOpenTerminology = onOpenTerminology,
                onEditInfo = onEditInfo,
                onManageChapters = onManageChapters,
                onAppendChapters = onAppendChapters,
                onDownloadEpub = onDownloadEpub,
                onDownloadTxt = onDownloadTxt,
                onOpenWeb = onOpenWeb,
            )
        }
    }
}

@Composable
private fun ReaderScreen(
    state: ReaderState,
    options: ReaderUiOptions,
    ttsSettings: ReaderTtsSettings,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onCycleTheme: () -> Unit,
    onReaderOptionsChange: (ReaderUiOptions) -> Unit,
    onReaderTtsSettingsChange: ((ReaderTtsSettings) -> ReaderTtsSettings) -> Unit,
    onResetReaderOptions: () -> Unit,
    onClearReaderChapterCache: () -> Unit,
    onRetry: () -> Unit,
    onRetryChapterComments: (Long) -> Unit,
    onRetryCatalog: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onOpenReaderAtPosition: (Long, Long, ReaderChapterEntryPosition) -> Unit,
    onLoadNextChapter: () -> Unit,
    onVisibleChapterChanged: (Long, String?) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onCommentDraftChange: (Long, String) -> Unit,
    onSubmitComment: (Long) -> Unit,
    onReplyComment: (Long, ChapterComment) -> Unit,
    onCancelCommentReply: (Long) -> Unit,
    onCommentLike: (Long, ChapterComment) -> Unit,
    onCommentDislike: (Long, ChapterComment) -> Unit,
    onCommentEmoji: (Long, ChapterComment) -> Unit,
    onCommentAward: (Long, ChapterComment) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenWeb: (Long) -> Unit,
    onPreviewImage: (ReaderContentBlock.Image, String) -> Unit,
) {
    val chapters = (state.chapters as? LoadResult.Success)?.value.orEmpty()
    val catalogVisible = remember { mutableStateOf(false) }
    val readerSettingsVisible = remember { mutableStateOf(false) }
    val readerHelpVisible = remember { mutableStateOf(false) }
    val readerNavigationVisible = remember { mutableStateOf(false) }
    val readerSettingsCategory = remember { mutableStateOf<ReaderSettingsCategory?>(null) }
    val toolbarsVisible = remember { mutableStateOf(false) }
    var pendingChapterEntryPosition by remember { mutableStateOf(ReaderChapterEntryPosition.Start) }
    val radialMenuVisible = remember { mutableStateOf(false) }
    val readerFullscreen = remember { mutableStateOf(false) }
    var lastReaderChromeTapUptime by remember { mutableLongStateOf(0L) }
    var lastReaderChromeTapXFraction by remember { mutableFloatStateOf(-1f) }
    var lastReaderChromeTapYFraction by remember { mutableFloatStateOf(-1f) }
    val listState = rememberLazyListState()
    val readerTouchSlop = LocalViewConfiguration.current.touchSlop
    val readableContent = (state.content as? LoadResult.Success)?.value
    val chapterContents = remember(state.chapterContents, readableContent, state.chapterId) {
        state.chapterContents.ifEmpty {
            readableContent?.let { listOf(ReaderChapterContent(state.chapterId, it.title, it)) }.orEmpty()
        }
    }
    val readerEndSentinelKey = remember(chapterContents) {
        readerBodyEndSentinelKey(chapterContents)
    }
    // Do not make the next append depend on one visibility transition. A catalog refresh or an
    // in-flight append can replace the observer at exactly the chapter boundary. A newly appended
    // chapter gets a new sentinel key and therefore a fresh boundary flag.
    val continuousBoundaryReached = remember(readerEndSentinelKey) { mutableStateOf(false) }
    val readerScope = rememberCoroutineScope()
    val context = LocalContext.current
    val readerView = LocalView.current
    val readerInsetsController = remember(context, readerView) {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, readerView)
        }
    }
    val isReaderFullscreen = readerFullscreen.value
    DisposableEffect(readerInsetsController, isReaderFullscreen) {
        if (isReaderFullscreen) {
            readerInsetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            readerInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            readerInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (isReaderFullscreen) {
                readerInsetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val ttsController = remember(context) { ReaderTtsController(context) }
    androidx.compose.runtime.DisposableEffect(ttsController) {
        onDispose { ttsController.shutdown() }
    }

    // D11. The reader is the app's one fully immersive route -- globalProductTopBarVisible excludes
    // it and the bottom NavigationBar only shows on tab roots -- and toolbarsVisible starts false.
    // So a chapter that failed to load rendered an error card and a comment box with no visible way
    // out at all; the only escape was the system back gesture, which is not an affordance. Chrome is
    // therefore not optional while there is nothing to read: it is forced on for Idle, Loading and
    // Error, and only becomes tap-to-toggle once a body has actually arrived.
    val hasReadableBody = state.content is LoadResult.Success
    // Continuous scroll takes precedence for legacy preferences that accidentally saved both
    // modes. ReaderSettingsStore and the settings sheet repair that state for future launches.
    val continuousScrollEnabled = options.useInfiniteScroll
    val pageTurnEnabled = options.pageTurnMode && !continuousScrollEnabled
    val chromeVisible = readerChromeVisible(hasReadableBody, toolbarsVisible.value)

    // A new chapter replaces the reader route in-place, so the LazyColumn survives by design.
    // Reset its viewport explicitly; otherwise a user who was near the end of chapter A can land
    // in the middle of chapter B before its heading and first paragraphs.
    LaunchedEffect(state.bookId, state.chapterId, state.entryPosition) {
        ttsController.stop()
        catalogVisible.value = false
        readerSettingsVisible.value = false
        readerHelpVisible.value = false
        readerNavigationVisible.value = false
        toolbarsVisible.value = false
        radialMenuVisible.value = false
        lastReaderChromeTapUptime = 0L
        lastReaderChromeTapXFraction = -1f
        lastReaderChromeTapYFraction = -1f
        continuousBoundaryReached.value = false
        pendingChapterEntryPosition = state.entryPosition
        listState.scrollToItem(0)
    }

    // The loading placeholder is initially the only LazyColumn item. Wait for the actual chapter
    // body to compose before resolving an end-of-previous-chapter request, then consume it so
    // subsequent comment/catalog updates cannot pull the reader back to the bottom again.
    LaunchedEffect(
        state.bookId,
        state.chapterId,
        state.entryPosition,
        hasReadableBody,
        readerEndSentinelKey,
    ) {
        if (!hasReadableBody || pendingChapterEntryPosition != ReaderChapterEntryPosition.End) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        listState.scrollToItem(
            readerChapterEntryScrollIndex(
                entryPosition = pendingChapterEntryPosition,
                itemCount = listState.layoutInfo.totalItemsCount,
            )
        )
        pendingChapterEntryPosition = ReaderChapterEntryPosition.Start
    }

    // Infinite scrolling appends bodies without replacing the route. Save a chapter only after
    // one of its article items becomes visible, never merely because it was prefetched.
    LaunchedEffect(listState, chapterContents) {
        snapshotFlow {
            readerFirstVisibleChapterId(
                listState.layoutInfo.visibleItemsInfo.map { item -> item.key },
            )
        }.collect { visibleChapterId ->
            if (visibleChapterId != null) {
                onVisibleChapterChanged(
                    visibleChapterId,
                    chapterContents.firstOrNull { it.chapterId == visibleChapterId }?.title,
                )
            }
        }
    }

    LaunchedEffect(
        listState,
        continuousScrollEnabled,
        // The chapter catalog commonly finishes after the first chapter body. Include it in the
        // effect keys so a trigger that happened while the catalog was still Loading is retried
        // as soon as the next-chapter candidates become available.
        chapters,
        state.chapterId,
        hasReadableBody,
        chapterContents.size,
        state.chapters,
        state.loadingNextChapter,
        state.nextChapterError,
        state.nextChapterWaitingForCatalog,
        state.nextChapterExhausted,
    ) {
        // A loading/error placeholder may occupy the first LazyColumn item. It is not an article
        // boundary, so it must never cause an adjacent chapter request before the current body is
        // readable.
        if (!readerContinuousScrollCanTrigger(continuousScrollEnabled, hasReadableBody)) {
            return@LaunchedEffect
        }
        snapshotFlow {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val bodyEndVisible = visibleItems.any { item -> item.key == readerEndSentinelKey }
            val lastVisibleItemIndex = visibleItems.maxOfOrNull { it.index } ?: -1
            val prefetchStartIndex = readerNextChapterPrefetchStartIndex(
                totalItemCount = listState.layoutInfo.totalItemsCount,
                // Every chapter's comments are now part of its own body group, before the
                // sentinel. The next chapter therefore follows body -> comments -> body.
                itemsAfterSentinel = 0,
            )
            // Begin the idempotent read shortly before the marker. This hides normal network
            // latency while retaining the marker as a fallback for very short chapters.
            bodyEndVisible || readerShouldPrefetchNextChapter(
                lastVisibleItemIndex = lastVisibleItemIndex,
                prefetchStartIndex = prefetchStartIndex,
            )
        }.collect { nextBoundaryReached ->
            // The sentinel is immediately after the article body and before the potentially
            // very large comments block. Watching LazyColumn's last item made comments block
            // continuous reading until their entire section had been traversed.
            if (nextBoundaryReached) continuousBoundaryReached.value = true
        }
    }

    LaunchedEffect(
        continuousScrollEnabled,
        hasReadableBody,
        continuousBoundaryReached.value,
        state.chapters,
        state.loadingNextChapter,
        state.nextChapterError,
        state.nextChapterWaitingForCatalog,
        state.nextChapterExhausted,
        chapterContents.size,
    ) {
        val catalogReady = state.chapters is LoadResult.Success
        if (
            readerContinuousScrollCanRequestNext(
                continuousScrollEnabled = continuousScrollEnabled,
                hasReadableBody = hasReadableBody,
                boundaryReached = continuousBoundaryReached.value,
                catalogReady = catalogReady,
                loadingNextChapter = state.loadingNextChapter,
                nextChapterError = state.nextChapterError,
                nextChapterWaitingForCatalog = state.nextChapterWaitingForCatalog,
                nextChapterExhausted = state.nextChapterExhausted,
            )
        ) {
            // Keep the boundary armed until a successful append replaces this sentinel. The
            // ViewModel can first refresh a partial catalog before it knows the next chapter;
            // consuming the signal here would leave the reader at the chapter end after that
            // refresh, because no second visibility transition is guaranteed. Loading, error and
            // confirmed-end states already gate this effect, so retaining the signal cannot spin.
            onLoadNextChapter()
        }
    }

    BackHandler(
        enabled = catalogVisible.value ||
            readerSettingsVisible.value ||
            readerHelpVisible.value ||
            readerNavigationVisible.value ||
            radialMenuVisible.value ||
            readerFullscreen.value ||
            toolbarsVisible.value,
    ) {
        when {
            readerSettingsVisible.value -> readerSettingsVisible.value = false
            catalogVisible.value -> catalogVisible.value = false
            readerHelpVisible.value -> readerHelpVisible.value = false
            readerNavigationVisible.value -> readerNavigationVisible.value = false
            readerFullscreen.value -> readerFullscreen.value = false
            radialMenuVisible.value -> radialMenuVisible.value = false
            toolbarsVisible.value -> toolbarsVisible.value = false
        }
    }
    val palette = readerPalette(options)
    val chromeLayout = readerChromeLayout()
    val sidePanelVisible = catalogVisible.value ||
        readerSettingsVisible.value ||
        readerHelpVisible.value ||
        readerNavigationVisible.value
    // The source keeps its compact title/status rails visible while reading. Only the full side
    // panel replaces them; the action rail itself remains a tap-to-toggle immersive overlay.
    val headerVisible = options.showHeader && !sidePanelVisible
    val statusVisible = options.showFooter && !sidePanelVisible
    val actionRailVisible = chromeVisible && !radialMenuVisible.value
    val pageAlpha = remember { Animatable(1f) }
    val pageOffset = remember { Animatable(0f) }

    fun openReaderPageBoundary(target: ReaderPageBoundaryTarget) {
        val adjacent = adjacentReaderChapters(state.chapterId, chapters)
        when (target) {
            ReaderPageBoundaryTarget.PreviousChapter -> adjacent.previous?.let {
                onOpenReaderAtPosition(
                    state.bookId,
                    it.id,
                    readerChapterEntryPositionForPageBoundary(target),
                )
            }
            ReaderPageBoundaryTarget.NextChapter -> adjacent.next?.let {
                onOpenReaderAtPosition(
                    state.bookId,
                    it.id,
                    readerChapterEntryPositionForPageBoundary(target),
                )
            }
            ReaderPageBoundaryTarget.None -> Unit
        }
    }

    fun turnReaderPage(direction: Int) {
        val adjacent = adjacentReaderChapters(state.chapterId, chapters)
        val alreadyAtBoundary = if (direction < 0) !listState.canScrollBackward else !listState.canScrollForward
        val immediateTarget = readerPageBoundaryTarget(
            direction = direction,
            reachedBoundary = alreadyAtBoundary,
            hasPrevious = adjacent.previous != null,
            hasNext = adjacent.next != null,
        )
        if (immediateTarget != ReaderPageBoundaryTarget.None) {
            openReaderPageBoundary(immediateTarget)
            return
        }
        val viewport = (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset)
            .toFloat()
            .coerceAtLeast(320f)
        readerScope.launch {
            val distance = viewport * 0.86f * direction
            val duration = readerPageAnimationDurationMs(options.pageTurnEffect)
            when (options.pageTurnEffect) {
                "fade" -> pageAlpha.animateTo(0.35f, tween(duration / 2))
                "cover", "slide" -> pageOffset.snapTo(-direction * viewport * 0.12f)
                "simulated" -> {
                    pageAlpha.animateTo(0.72f, tween(duration / 2))
                    pageOffset.snapTo(-direction * viewport * 0.06f)
                }
            }
            listState.animateScrollBy(distance, tween(duration))
            pageAlpha.animateTo(1f, tween(duration / 2))
            pageOffset.animateTo(0f, tween(duration / 2))
            val reachedBoundary = if (direction < 0) !listState.canScrollBackward else !listState.canScrollForward
            openReaderPageBoundary(
                readerPageBoundaryTarget(
                    direction = direction,
                    reachedBoundary = reachedBoundary,
                    hasPrevious = adjacent.previous != null,
                    hasNext = adjacent.next != null,
                )
            )
        }
    }

    fun closeReaderSidePanel() {
        catalogVisible.value = false
        readerSettingsVisible.value = false
        readerHelpVisible.value = false
        readerNavigationVisible.value = false
    }

    fun openReaderSettings(category: ReaderSettingsCategory?) {
        closeReaderSidePanel()
        readerSettingsCategory.value = category
        readerSettingsVisible.value = true
        toolbarsVisible.value = true
    }

    fun openReaderCatalog() {
        closeReaderSidePanel()
        catalogVisible.value = true
        toolbarsVisible.value = true
    }

    fun openReaderHelp() {
        closeReaderSidePanel()
        readerHelpVisible.value = true
        toolbarsVisible.value = true
    }

    fun openReaderNavigation() {
        closeReaderSidePanel()
        readerNavigationVisible.value = true
        toolbarsVisible.value = true
    }

    fun toggleReaderReadingMode() {
        val enablePageTurn = !pageTurnEnabled
        onReaderOptionsChange(
            options.copy(
                pageTurnMode = enablePageTurn,
                useInfiniteScroll = !enablePageTurn,
            ).normalizedReaderOptions(),
        )
    }

    fun performReaderTap(xFraction: Float, yFraction: Float, uptimeMillis: Long) {
        val sameTapTarget = readerChromeTapTargetsOverlap(
            previousXFraction = lastReaderChromeTapXFraction,
            previousYFraction = lastReaderChromeTapYFraction,
            currentXFraction = xFraction,
            currentYFraction = yFraction,
        )
        when (
            readerBodyTapResolution(
                controlsVisible = toolbarsVisible.value,
                lastToggleUptimeMillis = lastReaderChromeTapUptime,
                currentUptimeMillis = uptimeMillis,
                sameTapTarget = sameTapTarget,
            )
        ) {
            ReaderBodyTapResolution.KeepChromeVisible -> {
                lastReaderChromeTapUptime = uptimeMillis
                lastReaderChromeTapXFraction = xFraction
                lastReaderChromeTapYFraction = yFraction
                return
            }
            ReaderBodyTapResolution.DismissChrome -> {
                toolbarsVisible.value = false
                lastReaderChromeTapUptime = uptimeMillis
                lastReaderChromeTapXFraction = xFraction
                lastReaderChromeTapYFraction = yFraction
                return
            }
            ReaderBodyTapResolution.ApplyConfiguredTapArea -> Unit
        }
        when (readerTapActionAt(options.tapAreas.ifEmpty { defaultReaderTapAreas() }, xFraction)) {
            // Side zones are page-turn controls only in page mode. In continuous reading they must
            // not turn a slightly off-center scroll stop into a surprise jump through the chapter.
            "pagePrev" -> if (pageTurnEnabled) turnReaderPage(-1)
            "pageNext" -> if (pageTurnEnabled) turnReaderPage(1)
            "catalog" -> catalogVisible.value = true
            "sidebar" -> {
                toolbarsVisible.value = true
                lastReaderChromeTapUptime = uptimeMillis
                lastReaderChromeTapXFraction = xFraction
                lastReaderChromeTapYFraction = yFraction
            }
            else -> Unit
        }
    }

    fun toggleTts() {
        val segments = chapterContents.flatMap { chapter ->
            readerParagraphsFromContent(chapter.content.content)
        }
        ttsController.toggle(
            segments = segments,
            settings = ttsSettings,
            onSegmentChanged = { _, text ->
                if (ttsSettings.enableAutoScroll) {
                    val itemIndex = readerBodyItemIndexForText(chapterContents, options, text)
                    if (itemIndex != null) {
                        readerScope.launch {
                            listState.animateScrollToItem(itemIndex)
                        }
                    }
                }
            },
            onFinished = if (ttsSettings.enableAutoNextChapter) {
                { adjacentReaderChapters(state.chapterId, chapters).next?.let { onOpenReader(state.bookId, it.id) } }
            } else null,
        )
    }

    val ttsHighlightText = ttsController.currentSegmentText

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        palette.backgroundImageUri?.let { backgroundUri ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backgroundUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.14f },
                contentScale = ContentScale.Crop,
            )
        }
        val readerSidePanelModifier = Modifier
            .align(Alignment.CenterStart)
            .fillMaxHeight()
            .fillMaxWidth(readerSidePanelWidthFraction())
            .widthIn(max = readerSidePanelMaxWidthDp().dp)
        LazyColumn(
            state = listState,
            userScrollEnabled = !pageTurnEnabled,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = pageAlpha.value
                    translationX = pageOffset.value
                }
                .pointerInput(
                    options.tapAreas,
                    options.showRadialMenu,
                    options.radialMenuOpenMode,
                    pageTurnEnabled,
                    readerTouchSlop,
                ) {
                    // Observe the initial pointer pass without consuming it. SelectionContainer
                    // consumes a final-pass release inside article text; observing earlier keeps
                    // an ordinary short body tap available for the reader chrome while leaving
                    // LazyColumn drags and long-press text selection untouched.
                    awaitPointerEventScope {
                        var active = false
                        var moved = false
                        var downPosition = Offset.Zero
                        var downTime = 0L
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed && !active) {
                                active = true
                                moved = false
                                downPosition = change.position
                                downTime = change.uptimeMillis
                            } else if (change.pressed && active) {
                                if (readerTapExceedsTouchSlop(
                                        distancePx = (change.position - downPosition).getDistance(),
                                        touchSlopPx = readerTouchSlop,
                                    )
                                ) {
                                    moved = true
                                }
                            } else if (!change.pressed && active) {
                                val duration = change.uptimeMillis - downTime
                                val position = change.position
                                val fraction = (position.x / size.width).coerceIn(0f, 1f)
                                val verticalFraction = (position.y / size.height).coerceIn(0f, 1f)
                                val action = readerTapActionAt(
                                    options.tapAreas.ifEmpty { defaultReaderTapAreas() },
                                    fraction,
                                )
                                val isShortBodyTap = readerBodyTapIsEligible(
                                    moved = moved || readerTapExceedsTouchSlop(
                                        distancePx = (position - downPosition).getDistance(),
                                        touchSlopPx = readerTouchSlop,
                                    ),
                                    durationMillis = duration,
                                    longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis,
                                )
                                val opensRadialMenu = !toolbarsVisible.value &&
                                    readerUsesRadialMenu(options.showRadialMenu) &&
                                    options.radialMenuOpenMode == "longPress" &&
                                    action == "sidebar" &&
                                    duration >= 500L
                                if ((isShortBodyTap || opensRadialMenu) && (action != null || toolbarsVisible.value)) {
                                    if (opensRadialMenu) {
                                        radialMenuVisible.value = true
                                        toolbarsVisible.value = false
                                    } else {
                                        performReaderTap(fraction, verticalFraction, change.uptimeMillis)
                                    }
                                }
                                active = false
                            }
                        }
                    }
                },
            contentPadding = PaddingValues(
                top = (if (headerVisible) chromeLayout.headerHeightDp else 0f).dp + options.screenPaddingTopDp.dp,
                bottom = (if (statusVisible) chromeLayout.statusHeightDp else 0f).dp + options.screenPaddingBottomDp.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            when (val content = state.content) {
                    LoadResult.Idle -> item { LibraryStatusLine("等待加载正文") }
                    LoadResult.Loading -> item { LibraryLoadingBlock("正在加载正文") }
                    is LoadResult.Error -> item { NpErrorState(
                        message = content.message,
                        retryLabel = retryActionLabel("正文"),
                        onRetry = onRetry,
                        secondaryLabel = "网页正文",
                        onSecondary = { onOpenWeb(state.chapterId) }
                    ) }
                    is LoadResult.Success -> readerBodyItems(
                        contents = chapterContents,
                        options = options,
                        highlightedText = ttsHighlightText,
                        chapterCommentStates = state.chapterCommentStates,
                        fallbackCommentState = state,
                        onRetryChapterComments = onRetryChapterComments,
                        onDraftChange = onCommentDraftChange,
                        onSubmit = onSubmitComment,
                        onReply = onReplyComment,
                        onCancelReply = onCancelCommentReply,
                        onLike = onCommentLike,
                        onDislike = onCommentDislike,
                        onEmoji = onCommentEmoji,
                        onAward = onCommentAward,
                        onOpenUser = onOpenUser,
                        onOpenLink = onOpenLink,
                        onOpenWeb = onOpenWeb,
                        onPreviewImage = onPreviewImage,
                    )
            }
            if (hasReadableBody && continuousScrollEnabled) {
                item(key = readerEndSentinelKey) {
                    ReaderInfiniteScrollEnd(
                        state = state,
                        chapters = chapters,
                        chapterContents = chapterContents,
                        onRetryCatalog = onRetryCatalog,
                        onRetryNextChapter = onLoadNextChapter,
                    )
                }
            }
        }

        // The source keeps a slim title rail even while the side controls are hidden. The full
        // action surface is the right-hand rail, which appears only after a stationary body tap.
        androidx.compose.animation.AnimatedVisibility(
            visible = headerVisible,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(
                state = state,
                chapters = chapters,
                options = options,
                chromeLayout = chromeLayout,
            )
        }

        if (radialMenuVisible.value) {
            ReaderRadialMenu(
                state = state,
                chapters = chapters,
                favoriteStatus = state.favoriteStatus,
                ttsState = ttsController.state,
                showTts = options.showTts,
                onPrevious = {
                    adjacentReaderChapters(state.chapterId, chapters).previous?.let { onOpenReader(state.bookId, it.id) }
                    radialMenuVisible.value = false
                },
                onNext = {
                    adjacentReaderChapters(state.chapterId, chapters).next?.let { onOpenReader(state.bookId, it.id) }
                    radialMenuVisible.value = false
                },
                onCatalog = {
                    radialMenuVisible.value = false
                    openReaderCatalog()
                },
                onTts = {
                    toggleTts()
                    radialMenuVisible.value = false
                },
                onFavorite = {
                    onToggleFavorite()
                    radialMenuVisible.value = false
                },
                onDismiss = { radialMenuVisible.value = false },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // A panel owns the reading surface until it is dismissed. This prevents a tap that was
        // meant to close the drawer from leaking into the article and unexpectedly turning a page.
        if (sidePanelVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.08f))
                    .clickable {
                        closeReaderSidePanel()
                        toolbarsVisible.value = false
                    },
            )
        }

        if (catalogVisible.value) {
            ReaderCatalogPanel(
                state = state,
                chapters = state.chapters,
                options = options,
                catalogQuery = catalogQuery,
                onCatalogQueryChange = onCatalogQueryChange,
                onRetry = onRetryCatalog,
                onDismiss = ::closeReaderSidePanel,
                onOpenReader = { chapterId ->
                    closeReaderSidePanel()
                    toolbarsVisible.value = false
                    onOpenReader(state.bookId, chapterId)
                },
                modifier = readerSidePanelModifier,
            )
        }

        if (readerHelpVisible.value) {
            ReaderHelpPanel(
                palette = palette.sidebarPalette(),
                onDismiss = ::closeReaderSidePanel,
                modifier = readerSidePanelModifier,
            )
        }

        if (readerNavigationVisible.value) {
            ReaderNavigationPanel(
                palette = palette.sidebarPalette(),
                showFavoriteAction = options.showFavoriteButton,
                favoriteStatus = state.favoriteStatus,
                favoriteLoading = state.favoriteLoading,
                onOpenBook = {
                    closeReaderSidePanel()
                    toolbarsVisible.value = false
                    onBack()
                },
                onOpenWeb = {
                    closeReaderSidePanel()
                    toolbarsVisible.value = false
                    onOpenWeb(state.chapterId)
                },
                onToggleFavorite = onToggleFavorite,
                onDismiss = ::closeReaderSidePanel,
                modifier = readerSidePanelModifier,
            )
        }

        if (readerSettingsVisible.value) {
            ReaderSettingsSheet(
                options = options,
                palette = palette,
                initialCategory = readerSettingsCategory.value,
                onDecreaseFont = onDecreaseFont,
                onIncreaseFont = onIncreaseFont,
                onCycleTheme = onCycleTheme,
                onOptionsChange = onReaderOptionsChange,
                onReset = onResetReaderOptions,
                onClearCurrentBookCache = onClearReaderChapterCache,
                cacheClearing = state.clearingChapterCache,
                cacheMessage = state.chapterCacheActionMessage,
                ttsSettings = ttsSettings,
                ttsVoiceOptions = ttsController.voiceOptions,
                onTtsSettingsChange = onReaderTtsSettingsChange,
                onDismiss = { readerSettingsVisible.value = false },
                sidebar = true,
                modifier = readerSidePanelModifier,
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = actionRailVisible,
            enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
            exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd),
        ) {
            ReaderActionRailV2(
                state = state,
                chapters = chapters,
                options = options,
                selectedAction = when {
                    readerHelpVisible.value -> ReaderRailActionId.Help
                    catalogVisible.value -> ReaderRailActionId.Catalog
                    readerSettingsVisible.value && readerSettingsCategory.value == ReaderSettingsCategory.Theme -> ReaderRailActionId.Theme
                    readerSettingsVisible.value -> ReaderRailActionId.Settings
                    readerNavigationVisible.value -> ReaderRailActionId.Navigation
                    else -> null
                },
                fullscreen = readerFullscreen.value,
                onClose = {
                    closeReaderSidePanel()
                    radialMenuVisible.value = false
                    toolbarsVisible.value = false
                },
                onOpenHelp = ::openReaderHelp,
                onOpenCatalog = ::openReaderCatalog,
                onOpenSettings = { openReaderSettings(null) },
                onOpenTheme = { openReaderSettings(ReaderSettingsCategory.Theme) },
                onPrevious = {
                    adjacentReaderChapters(state.chapterId, chapters).previous?.let {
                        onOpenReader(state.bookId, it.id)
                    }
                },
                onNext = {
                    adjacentReaderChapters(state.chapterId, chapters).next?.let {
                        onOpenReader(state.bookId, it.id)
                    }
                },
                onToggleReadingMode = ::toggleReaderReadingMode,
                onToggleTts = ::toggleTts,
                ttsState = ttsController.state,
                onToggleFullscreen = { readerFullscreen.value = !readerFullscreen.value },
                onOpenNavigation = ::openReaderNavigation,
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = ttsController.state != ReaderTtsState.Stopped && !sidePanelVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    end = if (actionRailVisible) readerActionRailWidthDp().dp + 8.dp else 0.dp,
                    bottom = (if (statusVisible) chromeLayout.statusHeightDp else 0f).dp + 8.dp,
                ),
        ) {
            ReaderTtsFeedback(
                state = ttsController.state,
                failureMessage = ttsController.failureMessage,
                onOpenSystemTtsSettings = {
                    // The actionable error refers to the Android speech engine, not an app-local
                    // voice preference. Open the matching system surface and retain the local TTS
                    // group as a safe fallback on images that do not expose this intent.
                    runCatching {
                        context.startActivity(Intent(READER_TTS_SYSTEM_SETTINGS_ACTION))
                    }.onFailure {
                        openReaderSettings(ReaderSettingsCategory.Tts)
                    }
                },
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = statusVisible,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderStatusBar(
                state = state,
                chapters = chapters,
                options = options,
                chromeLayout = chromeLayout,
            )
        }
    }
}

@Composable
private fun ReaderCatalogPanel(
    state: ReaderState,
    chapters: LoadResult<List<Chapter>>,
    options: ReaderUiOptions,
    catalogQuery: String,
    onCatalogQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenReader: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = readerPalette(options).sidebarPalette()
    ReaderSidePanel(
        title = readerCatalogPanelTitle(),
        palette = palette,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        CatalogFilterField(catalogQuery, onCatalogQueryChange)
        when (chapters) {
            LoadResult.Idle -> LibraryStatusLine("等待加载目录")
            LoadResult.Loading -> LibraryLoadingBlock("正在加载目录")
            is LoadResult.Error -> NpErrorState(
                message = chapters.message,
                retryLabel = retryActionLabel("章节目录"),
                onRetry = onRetry
            )
            is LoadResult.Success -> {
                val visible = filterChapters(chapters.value, catalogQuery)
                if (visible.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        NpEmptyState(title = "没有匹配的章节")
                    }
                } else {
                    // The source drawer is a full-height rail. Let its chapter list own remaining
                    // space so a long catalog does not turn into an unscrollable bottom card.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
                    ) {
                        items(visible, key = { it.id }) { chapter ->
                            ReaderCatalogChapterCard(
                                chapter = chapter,
                                selected = chapter.id == state.chapterId,
                                cacheState = state.chapterCacheStates[chapter.id] ?: ReaderChapterCacheState.Missing,
                                palette = palette,
                                onClick = { onOpenReader(chapter.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderCatalogChapterCard(
    chapter: Chapter,
    selected: Boolean,
    cacheState: ReaderChapterCacheState,
    palette: ReaderPalette,
    onClick: () -> Unit,
) {
    val presentation = chapterListPresentation(chapter, ChapterListContext.ReaderCatalog)
    val cacheLabel = readerCatalogCacheLabel(cacheState)
    val cacheColor = when (cacheState) {
        ReaderChapterCacheState.Current -> Color(0xFF22C55E)
        ReaderChapterCacheState.Stale -> Color(0xFFF59E0B)
        ReaderChapterCacheState.Missing -> palette.meta.copy(alpha = 0.56f)
    }
    val dateLabel = readerCatalogUpdatedLabel(chapter.updatedAt) ?: "—"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovalPieRadius.md))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${presentation.numberLabel} ${chapter.title} $cacheLabel" },
        color = if (selected) palette.accent.copy(alpha = 0.12f) else palette.background,
        contentColor = palette.text,
        border = BorderStroke(
            NovalPieSize.hairline,
            if (selected) palette.accent else palette.meta.copy(alpha = 0.30f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = NovalPieSpacing.md, vertical = NovalPieSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
        ) {
            Text(
                text = presentation.numberLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) palette.accent else palette.meta,
            )
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    presentation.metrics.forEach { metric ->
                        if (metric.endsWith("图")) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = palette.accent,
                            )
                        }
                        Text(
                            text = metric,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.meta,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(cacheColor, CircleShape),
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.meta,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderSidePanel(
    title: String,
    palette: ReaderPalette,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = palette.sidebarBackground,
        contentColor = palette.sidebarText,
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.raised,
        border = BorderStroke(NovalPieSize.hairline, palette.sidebarText.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = NovalPieSpacing.md, vertical = NovalPieSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.size(48.dp))
            }
            content()
        }
    }
}

@Composable
private fun ReaderHelpPanel(
    palette: ReaderPalette,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReaderSidePanel(title = "帮助", palette = palette, onDismiss = onDismiss, modifier = modifier) {
        Text(
            "点击正文中央显示或关闭右侧工具栏；移动超过系统触摸阈值会保持为普通阅读滚动。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
        )
        Text(
            "目录、设置和主题都在右侧栏中。目录会在左侧抽屉打开，选中章节后会回到沉浸阅读。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
        )
        Text(
            "长按静止的正文插图可预览大图；收藏和上传书籍的封面不会触发预览，以免影响列表操作。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.sidebarText.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun ReaderNavigationPanel(
    palette: ReaderPalette,
    showFavoriteAction: Boolean,
    favoriteStatus: LoadResult<FavoriteStatus>,
    favoriteLoading: Boolean,
    onOpenBook: () -> Unit,
    onOpenWeb: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorite = (favoriteStatus as? LoadResult.Success)?.value?.isFavorited == true
    val favoriteEnabled = favoriteStatus is LoadResult.Success && !favoriteLoading
    ReaderSidePanel(title = "导航", palette = palette, onDismiss = onDismiss, modifier = modifier) {
        Text(
            "离开正文、打开网页正文和收藏操作集中在这里，避免工具栏里出现两个含义相同的网页返回键。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.meta,
        )
        readerNavigationPanelLabels(showFavoriteAction).forEach { label ->
            when (label) {
                "书本页" -> Button(
                    onClick = onOpenBook,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(label) }
                "网页正文" -> OutlinedButton(
                    onClick = onOpenWeb,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(label) }
                else -> OutlinedButton(
                    enabled = favoriteEnabled,
                    onClick = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (favorite) "已收藏" else label) }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    state: ReaderState,
    chapters: List<Chapter>,
    options: ReaderUiOptions,
    chromeLayout: ReaderChromeLayout,
    modifier: Modifier = Modifier,
) {
    val palette = readerPalette(options)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.sidebarBackground,
        contentColor = palette.sidebarText,
        tonalElevation = NovalPieElevation.none
    ) {
        Text(
            text = readerChapterProgressLabel(state.chapterId, chapters),
            modifier = Modifier
                .fillMaxWidth()
                .height(chromeLayout.headerHeightDp.dp)
                .padding(horizontal = chromeLayout.sidePaddingDp.dp),
            style = MaterialTheme.typography.labelSmall,
            color = palette.sidebarText.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun ReaderStatusBar(
    state: ReaderState,
    chapters: List<Chapter>,
    options: ReaderUiOptions,
    chromeLayout: ReaderChromeLayout,
    modifier: Modifier = Modifier,
) {
    val palette = readerPalette(options)
    fun currentClockLabel(): String =
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    var clockLabel by remember { mutableStateOf(currentClockLabel()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            clockLabel = currentClockLabel()
        }
    }
    val bookProgress = readerBookProgressFraction(state.chapterId, chapters)
    val progressLabel = String.format(java.util.Locale.US, "%.2f%%", bookProgress * 100f)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.sidebarBackground,
        contentColor = palette.sidebarText.copy(alpha = 0.72f),
        tonalElevation = NovalPieElevation.none,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chromeLayout.statusHeightDp.dp)
                .padding(horizontal = chromeLayout.sidePaddingDp.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
        ) {
            Text(
                text = clockLabel,
                style = MaterialTheme.typography.labelSmall,
                color = palette.sidebarText.copy(alpha = 0.72f),
            )
            Text(
                text = readerChapterProgressLabel(state.chapterId, chapters),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = palette.sidebarText.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (state.contentFromCache) {
                Text(
                    text = "离线缓存",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF59E0B),
                )
            }
            CircularProgressIndicator(
                progress = { bookProgress },
                modifier = Modifier.size(16.dp),
                color = palette.accent,
                strokeWidth = 2.dp,
                trackColor = palette.meta.copy(alpha = 0.18f),
            )
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.labelSmall,
                color = palette.meta,
            )
        }
    }
}

@Composable
private fun ReaderChapterCommentsSection(
    chapterId: Long,
    commentState: ReaderChapterCommentState,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReply: (ChapterComment) -> Unit,
    onCancelReply: () -> Unit,
    onLike: (ChapterComment) -> Unit,
    onDislike: (ChapterComment) -> Unit,
    onEmoji: (ChapterComment) -> Unit,
    onAward: (ChapterComment) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenWeb: () -> Unit,
) {
    val comments = commentState.comments
    var expandedThreadIds by remember(chapterId) { mutableStateOf(emptySet<Long>()) }
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        NpSectionHeader(
            title = chapterCommentsSectionTitle(),
            actionLabel = chapterCommentsFallbackLabel(),
            onAction = onOpenWeb
        )
        InlineCommentComposer(
            draft = commentState.draft,
            replyingToName = commentState.replyingToName,
            loading = commentState.actionLoading,
            message = commentState.actionMessage,
            onDraftChange = onDraftChange,
            onSubmit = onSubmit,
            onCancelReply = onCancelReply
        )
        when (comments) {
            LoadResult.Idle -> LibraryStatusLine("等待加载章节评论")
            LoadResult.Loading -> LibraryLoadingBlock("正在同步章节评论")
            is LoadResult.Error -> NpErrorState(
                message = comments.message,
                retryLabel = retryActionLabel("章节评论"),
                onRetry = onRetry
            )
            is LoadResult.Success -> {
                if (comments.value.isEmpty()) {
                    LibraryStatusLine("还没有章节评论")
                } else {
                    val threads = chapterCommentThreads(comments.value)
                    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
                        Text(
                            chapterCommentThreadSummary(threads, rootLabel = "章节评论"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        threads.forEach { thread ->
                            ChapterCommentThreadBlock(
                                thread = thread,
                                expanded = expandedThreadIds.contains(thread.comment.id),
                                onToggleReplies = {
                                    val next = expandedThreadIds.toMutableSet()
                                    if (!next.add(thread.comment.id)) next.remove(thread.comment.id)
                                    expandedThreadIds = next
                                },
                                onLike = onLike,
                                onDislike = onDislike,
                                onEmoji = onEmoji,
                                onAward = onAward,
                                onReply = onReply,
                                onOpenUser = onOpenUser,
                                onOpenLink = onOpenLink,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterCommentActionRow(
    comment: ChapterComment,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onReply: () -> Unit
) {
    // Five labelled actions in a LazyRow ran past the right edge of a narrow screen and clipped the
    // last one mid-glyph, with nothing to suggest it was there -- the same defect as the search
    // filter rail. They wrap now, so 回复 is always reachable.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
    ) {
        ForumActionIcon(Icons.Filled.ThumbUp, "赞 ${comment.likeCount ?: 0}", onLike)
        ForumActionIcon(Icons.Filled.ThumbDown, "踩 ${comment.dislikeCount ?: 0}", onDislike)
        ForumActionIcon(Icons.Filled.EmojiEmotions, "表情 ${comment.reactionCount ?: 0}", onEmoji)
        ForumActionIcon(Icons.Filled.CardGiftcard, "打赏 ${comment.awardPoints ?: 0}", onAward)
        ForumActionIcon(Icons.AutoMirrored.Filled.Reply, "回复", onReply)
    }
}

@Composable
private fun ReaderActionRailLegacy(
    state: ReaderState,
    chapters: List<Chapter>,
    options: ReaderUiOptions,
    selectedAction: String?,
    fullscreen: Boolean,
    onClose: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTheme: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onToggleTts: () -> Unit,
    ttsState: ReaderTtsState,
    onToggleFullscreen: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adjacent = adjacentReaderChapters(state.chapterId, chapters)
    val labels = readerActionRailLabels()
    val palette = readerPalette(options)
    Surface(
        modifier = modifier
            .width(readerActionRailWidthDp().dp)
            .fillMaxHeight(),
        color = palette.sidebarBackground,
        contentColor = palette.sidebarText,
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.raised,
        border = BorderStroke(NovalPieSize.hairline, palette.sidebarText.copy(alpha = 0.30f)),
    ) {
        CompositionLocalProvider(LocalReaderAccent provides palette.accent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = NovalPieSpacing.xs),
            ) {
            ReaderRailAction(
                icon = Icons.Filled.Close,
                label = labels[0],
                onClick = onClose,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.Filled.HelpOutline,
                label = labels[1],
                selected = selectedAction == labels[1],
                onClick = onOpenHelp,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = labels[2],
                selected = selectedAction == labels[2],
                onClick = onOpenCatalog,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.Filled.Settings,
                label = labels[3],
                selected = selectedAction == labels[3],
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.Filled.LightMode,
                label = labels[4],
                selected = selectedAction == labels[4],
                onClick = onOpenTheme,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.AutoMirrored.Filled.NavigateBefore,
                label = labels[5],
                enabled = adjacent.previous != null,
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.AutoMirrored.Filled.NavigateNext,
                label = labels[6],
                enabled = adjacent.next != null,
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.AutoMirrored.Filled.ViewList,
                label = readerReadingModeActionLabel(options.pageTurnMode),
                onClick = onToggleReadingMode,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = Icons.Filled.RecordVoiceOver,
                label = when (ttsState) {
                    ReaderTtsState.Speaking -> "停止"
                    ReaderTtsState.Loading -> "准备中"
                    ReaderTtsState.Error -> "听书异常"
                    ReaderTtsState.Stopped -> labels[8]
                },
                enabled = options.showTts,
                onClick = onToggleTts,
                modifier = Modifier.weight(1f),
            )
            ReaderRailAction(
                icon = if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                label = if (fullscreen) "退出全屏" else labels[9],
                onClick = onToggleFullscreen,
                modifier = Modifier.weight(1f),
            )
                ReaderRailAction(
                    icon = Icons.Filled.Navigation,
                    label = labels[10],
                    selected = selectedAction == labels[10],
                    onClick = onOpenNavigation,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Renders the source rail from one action specification so its icon, label, enabled state and
 * callback cannot drift apart. A scrollable parent preserves touch targets on short landscape
 * windows instead of distributing eleven controls into unusably small weighted cells.
 */
@Composable
private fun ReaderActionRailV2(
    state: ReaderState,
    chapters: List<Chapter>,
    options: ReaderUiOptions,
    selectedAction: ReaderRailActionId?,
    fullscreen: Boolean,
    onClose: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTheme: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onToggleTts: () -> Unit,
    ttsState: ReaderTtsState,
    onToggleFullscreen: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val adjacent = adjacentReaderChapters(state.chapterId, chapters)
    val palette = readerPalette(options)
    val scrollState = androidx.compose.foundation.rememberScrollState()
    Surface(
        modifier = modifier
            .width(readerActionRailWidthDp().dp)
            .fillMaxHeight(),
        color = palette.sidebarBackground,
        contentColor = palette.sidebarText,
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.raised,
        border = BorderStroke(NovalPieSize.hairline, palette.sidebarText.copy(alpha = 0.30f)),
    ) {
        CompositionLocalProvider(LocalReaderAccent provides palette.accent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = NovalPieSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
            ) {
                readerRailActionSpecs().forEach { spec ->
                    val icon = when (spec.id) {
                        ReaderRailActionId.Close -> Icons.Filled.Close
                        ReaderRailActionId.Help -> Icons.Filled.HelpOutline
                        ReaderRailActionId.Catalog -> Icons.AutoMirrored.Filled.MenuBook
                        ReaderRailActionId.Settings -> Icons.Filled.Settings
                        ReaderRailActionId.Theme -> Icons.Filled.LightMode
                        ReaderRailActionId.Previous -> Icons.AutoMirrored.Filled.NavigateBefore
                        ReaderRailActionId.Next -> Icons.AutoMirrored.Filled.NavigateNext
                        ReaderRailActionId.ReadingMode -> Icons.AutoMirrored.Filled.ViewList
                        ReaderRailActionId.Tts -> Icons.Filled.RecordVoiceOver
                        ReaderRailActionId.Fullscreen -> if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen
                        ReaderRailActionId.Navigation -> Icons.Filled.Navigation
                    }
                    val label = when (spec.id) {
                        ReaderRailActionId.ReadingMode -> readerReadingModeActionLabel(options.pageTurnMode)
                        ReaderRailActionId.Tts -> when (ttsState) {
                            ReaderTtsState.Speaking -> "停止"
                            ReaderTtsState.Loading -> "准备中"
                            ReaderTtsState.Error -> "听书异常"
                            ReaderTtsState.Stopped -> spec.label
                        }
                        ReaderRailActionId.Fullscreen -> if (fullscreen) "退出全屏" else spec.label
                        else -> spec.label
                    }
                    val onClick = when (spec.id) {
                        ReaderRailActionId.Close -> onClose
                        ReaderRailActionId.Help -> onOpenHelp
                        ReaderRailActionId.Catalog -> onOpenCatalog
                        ReaderRailActionId.Settings -> onOpenSettings
                        ReaderRailActionId.Theme -> onOpenTheme
                        ReaderRailActionId.Previous -> onPrevious
                        ReaderRailActionId.Next -> onNext
                        ReaderRailActionId.ReadingMode -> onToggleReadingMode
                        ReaderRailActionId.Tts -> onToggleTts
                        ReaderRailActionId.Fullscreen -> onToggleFullscreen
                        ReaderRailActionId.Navigation -> onOpenNavigation
                    }
                    ReaderRailAction(
                        icon = icon,
                        label = label,
                        selected = readerRailActionSelected(spec.id, selectedAction),
                        enabled = readerRailActionEnabled(
                            id = spec.id,
                            hasPrevious = adjacent.previous != null,
                            hasNext = adjacent.next != null,
                            showTts = options.showTts,
                        ),
                        onClick = onClick,
                        modifier = Modifier.heightIn(min = 56.dp),
                    )
                }
            }
        }
    }
}

/** One narrow, full-height target per source control keeps every action reachable without a dock. */
@Composable
private fun ReaderRailAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val accent = LocalReaderAccent.current
    val contentColor = if (selected) accent else LocalContentColor.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovalPieRadius.sm))
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else 0.38f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReaderTtsFeedback(
    state: ReaderTtsState,
    failureMessage: String?,
    onOpenSystemTtsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (state) {
        ReaderTtsState.Loading -> "正在准备听书…"
        ReaderTtsState.Speaking -> "正在听书"
        ReaderTtsState.Error -> failureMessage ?: "听书暂时不可用"
        ReaderTtsState.Stopped -> return
    }
    val isError = state == ReaderTtsState.Error
    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = if (isError) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.RecordVoiceOver,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 260.dp),
            )
            if (isError) {
                TextButton(onClick = onOpenSystemTtsSettings, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(readerTtsSystemSettingsLabel(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    options: ReaderUiOptions,
    palette: ReaderPalette,
    initialCategory: ReaderSettingsCategory?,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onCycleTheme: () -> Unit,
    onOptionsChange: (ReaderUiOptions) -> Unit,
    onReset: () -> Unit,
    onClearCurrentBookCache: () -> Unit,
    cacheClearing: Boolean,
    cacheMessage: String?,
    ttsSettings: ReaderTtsSettings,
    ttsVoiceOptions: List<ReaderTtsVoiceOption>,
    onTtsSettingsChange: ((ReaderTtsSettings) -> ReaderTtsSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sidebar: Boolean = false,
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    Surface(
        modifier = if (sidebar) {
            modifier.fillMaxHeight()
        } else {
            modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
        },
        color = palette.sidebarBackground,
        contentColor = palette.sidebarText,
        shadowElevation = NovalPieElevation.dialog,
        border = BorderStroke(NovalPieSize.hairline, palette.sidebarText.copy(alpha = 0.28f)),
        shape = if (sidebar) {
            RoundedCornerShape(0.dp)
        } else {
            RoundedCornerShape(topStart = NovalPieRadius.lg, topEnd = NovalPieRadius.lg)
        },
    ) {
        Column(
            modifier = (if (sidebar) Modifier.fillMaxHeight() else Modifier.heightIn(max = 620.dp))
                .verticalScroll(scrollState)
                .padding(horizontal = NovalPieSpacing.lg, vertical = NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (selectedCategory == null) onDismiss() else selectedCategory = null
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (selectedCategory == null) "返回" else "全部设置",
                    )
                }
                Text(
                    text = selectedCategory?.let { "${it.label}设置" } ?: "设置",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.size(48.dp))
            }
            if (selectedCategory == null) {
                ReaderSettingsOverview(
                    options = options,
                    palette = palette,
                    onSelect = { selectedCategory = it },
                )
            } else if (selectedCategory == ReaderSettingsCategory.Tts) {
                ReaderTtsSettingsControls(
                    settings = ttsSettings,
                    textColor = palette.sidebarText,
                    metaColor = palette.sidebarText.copy(alpha = 0.68f),
                    voiceOptions = ttsVoiceOptions,
                    onChange = onTtsSettingsChange,
                )
            } else {
                ReaderSettingsControls(
                    options = options,
                    category = selectedCategory!!,
                    textColor = palette.sidebarText,
                    metaColor = palette.sidebarText.copy(alpha = 0.68f),
                    onDecreaseFont = onDecreaseFont,
                    onIncreaseFont = onIncreaseFont,
                    onCycleTheme = onCycleTheme,
                    onOptionsChange = onOptionsChange,
                    onReset = onReset,
                    onClearCurrentBookCache = onClearCurrentBookCache,
                    clearingChapterCache = cacheClearing,
                    chapterCacheMessage = cacheMessage,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderSettingsOverview(
    options: ReaderUiOptions,
    palette: ReaderPalette,
    onSelect: (ReaderSettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
    ) {
        ReaderSettingsOverviewCard(
            icon = Icons.Filled.Tune,
            title = "偏好配置",
            summary = "设置会自动保存在当前设备",
            tags = listOf("自动保存"),
            palette = palette,
            onClick = { onSelect(ReaderSettingsCategory.Other) },
        )
        readerSettingsCategories().forEach { category ->
            ReaderSettingsOverviewCard(
                icon = readerSettingsCategoryIcon(category),
                title = readerSettingsOverviewTitle(category),
                summary = readerSettingsOverviewSummary(category, options),
                tags = readerSettingsOverviewTags(category, options),
                palette = palette,
                onClick = { onSelect(category) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderSettingsOverviewCard(
    icon: ImageVector,
    title: String,
    summary: String,
    tags: List<String>,
    palette: ReaderPalette,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovalPieRadius.md))
            .clickable(onClick = onClick)
            .semantics { contentDescription = title },
        color = palette.sidebarBackground,
        contentColor = palette.sidebarText,
        border = BorderStroke(NovalPieSize.hairline, palette.meta.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(NovalPieSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(NovalPieRadius.sm),
                color = palette.accent.copy(alpha = 0.10f),
                contentColor = palette.accent,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = palette.sidebarText.copy(alpha = 0.68f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tags.take(4).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(NovalPieRadius.pill),
                                color = palette.sidebarText.copy(alpha = 0.12f),
                                contentColor = palette.sidebarText,
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.sidebarText.copy(alpha = 0.72f),
            )
        }
    }
}

private fun readerSettingsOverviewTitle(category: ReaderSettingsCategory): String = when (category) {
    ReaderSettingsCategory.Replacement -> "文本替换"
    else -> "${category.label}设置"
}

private fun readerSettingsOverviewSummary(
    category: ReaderSettingsCategory,
    options: ReaderUiOptions,
): String = when (category) {
    ReaderSettingsCategory.Font -> "${options.fontSizeSp}sp · 行高${formatReaderOptionDecimal(options.lineHeight)}"
    ReaderSettingsCategory.Typography -> "首行缩进 · ${if (options.emptyLine) "保留空行" else "紧凑段落"}"
    ReaderSettingsCategory.Display -> "评论${if (options.showComments) "开" else "关"} · 插图${if (options.showImages) "开" else "关"}"
    ReaderSettingsCategory.Layout -> "${options.contentWidthDp}dp 宽 · ${if (options.pageTurnMode) "翻页模式" else "滚动模式"}"
    ReaderSettingsCategory.Replacement -> options.replaceMode.readerReplaceModeLabel()
    ReaderSettingsCategory.Theme -> readerThemeLabel(options.theme, options.customThemes)
    ReaderSettingsCategory.Tts -> "语速、声音和朗读行为"
    ReaderSettingsCategory.Other -> "缓存与重置快捷入口"
}

private fun readerSettingsOverviewTags(
    category: ReaderSettingsCategory,
    options: ReaderUiOptions,
): List<String> = when (category) {
    ReaderSettingsCategory.Font -> listOf("${options.fontSizeSp}sp", "字重${options.fontWeight}")
    ReaderSettingsCategory.Typography -> listOf(if (options.textIndent) "首行缩进" else "无缩进", if (options.emptyLine) "保留空行" else "紧凑")
    ReaderSettingsCategory.Display -> listOf(if (options.showComments) "评论开" else "评论关", if (options.showTts) "听书开" else "听书关")
    ReaderSettingsCategory.Layout -> listOf("${options.contentWidthDp}dp", if (options.useInfiniteScroll) "滚动模式" else "翻页模式")
    ReaderSettingsCategory.Replacement -> listOf(options.replaceMode.readerReplaceModeLabel())
    ReaderSettingsCategory.Theme -> listOf(readerThemeLabel(options.theme, options.customThemes))
    ReaderSettingsCategory.Tts -> listOf("听书")
    ReaderSettingsCategory.Other -> listOf("缓存/重置")
}

private fun formatReaderOptionDecimal(value: Float): String {
    val whole = value.toInt()
    return if (value == whole.toFloat()) whole.toString() else String.format(java.util.Locale.US, "%.1f", value)
}

@Composable
private fun ToolsScreen(
    state: ToolsState,
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    appThemeMode: AppThemeMode,
    chineseVariant: com.novalpie.nativeapp.model.ChineseVariant,
    onRefresh: () -> Unit,
    onToggleAppTheme: (Boolean) -> Unit,
    onCycleChineseVariant: () -> Unit,
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
                    Text("功能中心", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "消息、工作区与网站管理入口",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("刷新")
                }
            }
        }

        if (!hasAuthToken) {
            item { ToolsLoginPrompt(onOpenLogin) }
        }

        item {
            ToolsAppearanceCard(
                mode = appThemeMode,
                onToggleTheme = onToggleAppTheme
            )
        }

        item {
            ToolsChineseVariantCard(
                variant = chineseVariant,
                onCycleVariant = onCycleChineseVariant
            )
        }

        item { ToolsMessageStats(state.stats) }
        item {
            Text("最近消息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        when (val messages = state.messages) {
            LoadResult.Idle -> item { StatusText("打开功能中心后同步消息") }
            LoadResult.Loading -> item { LoadingBlock("正在同步消息") }
            is LoadResult.Error -> item {
                ErrorBlock(messages.message, retryLabel = "重试消息", onRetry = onRefresh)
            }
            is LoadResult.Success -> {
                if (messages.value.isEmpty()) {
                    item { StatusText("暂无消息") }
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
                Text("打开完整消息中心")
            }
        }

        item {
            Text("网站功能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
private fun ToolsAppearanceCard(
    mode: AppThemeMode,
    onToggleTheme: (Boolean) -> Unit
) {
    val isDark = mode.resolvesDark(isSystemInDarkTheme())
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovalPieSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)) {
                Text("外观", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${mode.displayLabel()}；与网站工具菜单的深浅色切换一致。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = { onToggleTheme(isDark) }) {
                Text(sourceThemeToggleLabel(isDark))
            }
        }
    }
}

@Composable
private fun ToolsChineseVariantCard(
    variant: com.novalpie.nativeapp.model.ChineseVariant,
    onCycleVariant: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovalPieSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)) {
                Text("文字转换", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    variant.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onCycleVariant) { Text(variant.displayLabel()) }
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
                Text("登录后同步", fontWeight = FontWeight.Bold)
                Text(
                    "消息、工作区和管理功能需要网站账号",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onOpenLogin) { Text("登录") }
        }
    }
}

@Composable
private fun ToolsMessageStats(stats: LoadResult<MessageStats>) {
    when (stats) {
        LoadResult.Idle -> StatusText("等待同步消息统计")
        LoadResult.Loading -> LoadingBlock("正在同步消息统计")
        is LoadResult.Error -> ErrorBlock(stats.message)
        is LoadResult.Success -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { LibraryStatPill("未读 ${stats.value.unreadCount}") }
            item { LibraryStatPill("全部 ${stats.value.totalCount}") }
            item { LibraryStatPill("重要 ${stats.value.importantCount}") }
            item { LibraryStatPill("7日 ${stats.value.recentSevenDaysCount}") }
            item { LibraryStatPill("星标 ${stats.value.starredCount}") }
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
    appThemeMode: AppThemeMode,
    chineseVariant: com.novalpie.nativeapp.model.ChineseVariant,
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
    onAppThemeModeChange: (AppThemeMode) -> Unit,
    onChineseVariantChange: (com.novalpie.nativeapp.model.ChineseVariant) -> Unit,
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
        item {
            AppThemeSettingsCard(
                mode = appThemeMode,
                onModeChange = onAppThemeModeChange
            )
        }
        item {
            ChineseVariantSettingsCard(
                variant = chineseVariant,
                onVariantChange = onChineseVariantChange
            )
        }
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
@OptIn(ExperimentalLayoutApi::class)
private fun AppThemeSettingsCard(
    mode: AppThemeMode,
    onModeChange: (AppThemeMode) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            Text("外观", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "网站工具菜单可直接在深色和浅色间切换；此处额外保留系统默认选项。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
            ) {
                AppThemeMode.entries.forEach { candidate ->
                    FilterChip(
                        selected = mode == candidate,
                        onClick = { onModeChange(candidate) },
                        label = { Text(candidate.displayLabel()) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ChineseVariantSettingsCard(
    variant: com.novalpie.nativeapp.model.ChineseVariant,
    onVariantChange: (com.novalpie.nativeapp.model.ChineseVariant) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            Text("文字转换", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "与网站工具菜单一致；转换只发生在本地显示，不会修改帖子、书籍或账号数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
            ) {
                com.novalpie.nativeapp.model.ChineseVariant.entries.forEach { candidate ->
                    FilterChip(
                        selected = variant == candidate,
                        onClick = { onVariantChange(candidate) },
                        label = { Text(candidate.displayLabel()) }
                    )
                }
            }
            Text(
                variant.description(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
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

/**
 * The bookshelf header.
 *
 * This block used to own the whole first screen: a title pair, a sync pill, two icon buttons, a
 * full-height search bar and a 3-cell metric card with titleLarge numbers — roughly 200dp of chrome
 * before the first book cover. The counts are metadata, not the point of the tab, so they now ride
 * in one wrapping chip row beside the sync state instead of a card of their own.
 */
@Composable
private fun LibraryOverviewBlock(
    overview: LibraryOverview,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenWeb: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    overview.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    overview.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(NovalPieSize.minTouchTarget)
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "同步书架",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(NovalPieSize.iconLg)
                )
            }
            IconButton(
                onClick = onOpenWeb,
                modifier = Modifier.size(NovalPieSize.minTouchTarget)
            ) {
                Icon(
                    Icons.Filled.OpenInBrowser,
                    contentDescription = "打开网页收藏",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(NovalPieSize.iconLg)
                )
            }
        }
        // Sync state and the three counts, coloured by meaning: an unsynced shelf is the one thing
        // here the user may need to act on, so it is the only warning-toned chip.
        NpChipRow {
            NpChip(
                label = overview.syncLabel,
                tone = if (overview.syncLabel == "已同步") NpChipTone.Status else NpChipTone.Warning
            )
            overview.stats.take(4).forEachIndexed { index, stat ->
                LibraryMetricCell(
                    label = if (index == 3) "总页数" else when (index) {
                        0 -> "收藏"
                        1 -> "分组"
                        else -> "最近"
                    },
                    value = stat.filter(Char::isDigit)
                )
            }
        }
        if (overview.syncLabel == "未同步") {
            TextButton(
                onClick = onOpenLogin,
                modifier = Modifier
                    .align(Alignment.Start)
                    .heightIn(min = NovalPieSize.minTouchTarget)
            ) {
                Text("登录后同步收藏", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        LibrarySearchEntry(onOpenSearch = onOpenSearch)
    }
}

/** One shelf count. A chip, not a titleLarge number in a card: it is metadata about the grid. */
@Composable
private fun LibraryMetricCell(label: String, value: String) {
    NpChip(label = "$label ${value.ifBlank { "0" }}", tone = NpChipTone.Neutral)
}

/** Entry point to the Discover tab. Distinct from the local 筛选书架 field below. */
@Composable
private fun LibrarySearchEntry(onOpenSearch: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NovalPieSize.minTouchTarget)
            .clickable(onClick = onOpenSearch),
        shape = RoundedCornerShape(NovalPieRadius.pill),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = NovalPieSpacing.md,
                vertical = NovalPieSpacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(NovalPieSize.iconMd)
            )
            Text(
                "搜索小说、作者或标签",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Filled.Explore,
                contentDescription = "进入搜索",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(NovalPieSize.iconMd)
            )
        }
    }
}

/** An Idle / not-yet-requested notice. Quiet, because nothing has gone wrong. */
@Composable
private fun LibraryStatusLine(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NovalPieSpacing.sm)
    )
}

/**
 * A pending shelf request. Reserves roughly the space the books will take, so the arriving grid
 * does not shove the header off the screen the way a bare progress bar did.
 */
@Composable
private fun LibraryLoadingBlock(message: String, rows: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        repeat(rows) { NpBookRowSkeleton() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun LibraryShelfControls(
    groups: LoadResult<List<FavoriteGroup>>,
    selectedGroupId: Long?,
    value: String,
    onValueChange: (String) -> Unit,
    onGroupSelected: (Long?) -> Unit,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        when (groups) {
            LoadResult.Idle -> LibraryStatusLine("等待加载分组")
            // Reserves the height a chip row will occupy, so the controls do not jump when the
            // groups arrive.
            LoadResult.Loading -> NpSkeleton(height = NovalPieSize.minTouchTarget, widthFraction = 0.6f)
            // Was a bare Text of the message: indistinguishable from ordinary copy, and with no way
            // to retry a failed group load short of refreshing the whole screen.
            is LoadResult.Error -> NpErrorState(
                message = groups.message,
                retryLabel = "重新加载分组",
                onRetry = onRetry
            )
            is LoadResult.Success -> {
                // FlowRow, not LazyRow. The group chips used to scroll horizontally and clip the
                // last one mid-glyph with no affordance suggesting more existed.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                ) {
                    FilterChip(
                        selected = selectedGroupId == null,
                        onClick = { onGroupSelected(null) },
                        label = { Text("全部") }
                    )
                    groups.value.take(8).forEach { group ->
                        FilterChip(
                            selected = group.id != null && selectedGroupId == group.id,
                            enabled = group.id != null,
                            onClick = { onGroupSelected(group.id) },
                            label = {
                                Text(
                                    "${group.name}${group.count?.let { " $it" } ?: ""}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
        NpSearchField(
            value = value,
            onValueChange = onValueChange,
            onSearch = {},
            placeholder = "筛选书架",
            clearContentDescription = "清除筛选"
        )
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
internal fun NovelCardItem(
    book: NovelCard,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    gridTagLineCount: Int? = null,
    gridMetricLineCount: Int? = null,
    gridCoverHeight: Dp? = null,
    previewPolicy: CoverPreviewPolicy = CoverPreviewPolicy.LongPressOnly,
    onPreview: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val preview = novelSearchPreview(book)
    val thumbnailCoverUrl = novelThumbnailCoverUrl(book)
    val previewCoverUrl = novelDisplayCoverUrl(book)
    val coverBadges = novelCardCoverBadges(book)
    val contentTags = novelCardContentTags(book)
    val tagAreaMinHeight = gridTagLineCount
        ?.let(::searchGridTagAreaMinHeightDp)
        ?.dp
        ?: SEARCH_GRID_TAG_MIN_AREA_HEIGHT_DP.dp
    val metricAreaMinHeight = gridMetricLineCount
        ?.let(::searchGridMetricAreaMinHeightDp)
        ?.dp
        ?: SEARCH_GRID_METRIC_MIN_AREA_HEIGHT_DP.dp
    val compactMetrics = novelCardCompactMetrics(book)
    val cardClickModifier = if (onLongClick == null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }
    Surface(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .semantics {
                contentDescription = if (selectionMode) {
                    "选择 ${book.title}"
                } else {
                    novelCardAccessibilityLabel(book)
                }
            }
            .then(cardClickModifier),
        shape = RoundedCornerShape(NovalPieRadius.md),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.card
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
            Box(
                modifier = if (gridCoverHeight == null) {
                    Modifier.fillMaxWidth().aspectRatio(bookCoverAspectRatio())
                } else {
                    Modifier.fillMaxWidth().height(gridCoverHeight)
                }
            ) {
                BookCover(
                    title = book.title,
                    coverUrl = thumbnailCoverUrl,
                    previewUrl = previewCoverUrl,
                    previewPolicy = previewPolicy,
                    onPreview = onPreview,
                )
                coverBadges.category?.let { category ->
                    NovelCoverBadge(
                        label = category,
                        modifier = Modifier.align(Alignment.TopStart).padding(NovalPieSpacing.xs),
                        containerColor = Color(0xFF15803D)
                    )
                }
                coverBadges.status?.let { status ->
                    NovelCoverBadge(
                        label = status,
                        modifier = Modifier.align(Alignment.TopEnd).padding(NovalPieSpacing.xs),
                        containerColor = Color(0xFF1D4ED8)
                    )
                }
                if (selectionMode && selected) {
                    FavoriteSelectionMarker(Modifier.align(Alignment.TopEnd))
                }
            }
            Column(
                modifier = Modifier.padding(
                    start = NovalPieSpacing.sm,
                    end = NovalPieSpacing.sm,
                    bottom = NovalPieSpacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
            ) {
                // Fixed line slots keep the tag and metric baselines equal across every card in
                // a grid row. minLines tracks the user's font scale without a fixed dp clip.
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = preview.authorLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tagAreaMinHeight)
                ) {
                    if (preview.platformLabel != null || contentTags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
                            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
                        ) {
                            preview.platformLabel?.let { platform -> NovelSourcePill(platform) }
                            contentTags.forEach { tag -> NovelTagPill(tag) }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = metricAreaMinHeight)
                ) {
                    if (compactMetrics.isNotEmpty()) {
                        NovelCompactMetricRow(compactMetrics)
                    }
                }
                if (!selectionMode && onTogglePin != null) {
                    TextButton(onClick = onTogglePin) {
                        Text(if (selected) "取消置顶" else "置顶")
                    }
                }
            }
        }
    }
}

/**
 * Collection/profile card: identity first, with only the reader progress line as an optional
 * secondary fact. Tags, source pills, counters and pin buttons belong in their tool panels, not
 * under every cover where they create large uneven blank regions.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun CompactLibraryBookCardItem(
    book: NovelCard,
    presentation: CompactLibraryBookCardPresentation,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    previewPolicy: CoverPreviewPolicy = CoverPreviewPolicy.Disabled,
    collectionCard: Boolean = false,
    gridCoverHeight: Dp? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val clickModifier = if (onLongClick == null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }
    val textSlots = compactLibraryBookCardTextSlots()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .semantics { contentDescription = "打开 ${presentation.title}，作者 ${presentation.author}" },
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
    ) {
        Box(
            modifier = if (gridCoverHeight == null) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(bookCoverAspectRatio())
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(gridCoverHeight)
            },
        ) {
            BookCover(
                title = book.title,
                coverUrl = novelThumbnailCoverUrl(book),
                previewUrl = novelDisplayCoverUrl(book),
                previewPolicy = previewPolicy,
            )
            if (selectionMode && selected) {
                FavoriteSelectionMarker(Modifier.align(Alignment.TopEnd))
            }
        }
        Text(
            text = presentation.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            minLines = textSlots.titleLines,
            maxLines = textSlots.titleLines,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = presentation.author,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = textSlots.authorLines,
            maxLines = textSlots.authorLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (compactLibraryCardReservesProgressSlot(presentation, collectionCard)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = textSlots.progressMinHeightDp.dp),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                presentation.progressLabel?.let { progress ->
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    presentation.updateLabel?.let { update ->
                        Text(
                            text = update,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Mobile source list mode: a tap-ready information row with the same cover, badges, tags and facts. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun NovelSearchListItem(
    book: NovelCard,
    onPreview: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val preview = novelSearchPreview(book)
    val thumbnailCoverUrl = novelThumbnailCoverUrl(book)
    val previewCoverUrl = novelDisplayCoverUrl(book)
    val coverBadges = novelCardCoverBadges(book)
    val contentTags = novelCardContentTags(book)
    val compactMetrics = novelCardCompactMetrics(book)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = novelCardAccessibilityLabel(book) }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(NovalPieRadius.md),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(NovalPieSize.hairline, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = NovalPieElevation.none,
        shadowElevation = NovalPieElevation.none
    ) {
        Row(
            modifier = Modifier.padding(NovalPieSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.width(84.dp).height(126.dp)) {
                BookCover(
                    title = book.title,
                    coverUrl = thumbnailCoverUrl,
                    width = 84.dp,
                    height = 126.dp,
                    previewUrl = previewCoverUrl,
                    previewPolicy = CoverPreviewPolicy.LongPressOnly,
                    onPreview = onPreview,
                )
                coverBadges.category?.let { category ->
                    NovelCoverBadge(
                        label = category,
                        modifier = Modifier.align(Alignment.TopStart).padding(NovalPieSpacing.xxs),
                        containerColor = Color(0xFF15803D)
                    )
                }
                coverBadges.status?.let { status ->
                    NovelCoverBadge(
                        label = status,
                        modifier = Modifier.align(Alignment.TopEnd).padding(NovalPieSpacing.xxs),
                        containerColor = Color(0xFF1D4ED8)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = NOVEL_CARD_METADATA_MAX_LINES,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = preview.authorLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = NOVEL_CARD_METADATA_MAX_LINES,
                    overflow = TextOverflow.Clip
                )
                if (preview.platformLabel != null || contentTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
                        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
                    ) {
                        preview.platformLabel?.let { platform -> NovelSourcePill(platform) }
                        contentTags.forEach { tag -> NovelTagPill(tag) }
                    }
                }
                if (compactMetrics.isNotEmpty()) {
                    NovelCompactMetricRow(compactMetrics)
                }
            }
        }
    }
}

@Composable
private fun NovelCoverBadge(label: String, modifier: Modifier, containerColor: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NovalPieRadius.xs),
        color = containerColor,
        contentColor = Color.White,
        tonalElevation = NovalPieElevation.raised
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = NovalPieSpacing.xs, vertical = NovalPieSpacing.xxs),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun NovelCompactMetricRow(metrics: List<NovelCardCompactMetric>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
    ) {
        metrics.forEach { metric ->
            val icon = when (metric.kind) {
                NovelCardMetricKind.Favorite -> Icons.Filled.Favorite
                NovelCardMetricKind.Read -> Icons.Filled.Visibility
                NovelCardMetricKind.WordCount -> Icons.AutoMirrored.Filled.MenuBook
            }
            Row(
                modifier = Modifier.semantics { contentDescription = metric.contentDescription },
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    metric.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun NovelTagPill(label: String) {
    Surface(
        shape = RoundedCornerShape(NovalPieRadius.xs),
        color = Color(0xFFF3E8FF),
        contentColor = Color(0xFF7E22CE)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = NovalPieSpacing.xs, vertical = NovalPieSpacing.xxs),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NovelSourcePill(label: String) {
    Surface(
        shape = RoundedCornerShape(NovalPieRadius.xs),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = NovalPieSpacing.xs, vertical = NovalPieSpacing.xxs),
            style = MaterialTheme.typography.labelSmall,
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
    onPreviewCover: () -> Unit,
) {
    // Detail still renders the source's outer/card layer. The inner layer is reserved for an
    // explicit stationary long press, matching the website's full-screen image viewer.
    val displayCoverUrl = novelThumbnailCoverUrl(book)
    val coverWidth = NovalPieSize.coverWidthHero
    NpCard {
        Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)) {
            BookCover(
                book.title,
                displayCoverUrl,
                coverWidth,
                coverWidth / NovalPieSize.coverAspectRatio,
                previewUrl = displayCoverUrl,
                // Keep image inspection explicit: the native client uses the same stationary
                // long-press gesture here as Search and reader illustrations.
                previewPolicy = bookDetailCoverPreviewPolicy(),
                onPreview = onPreviewCover,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
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
                book.author?.takeIf { it.isNotBlank() }?.let {
                    Text("作者 · $it", style = MaterialTheme.typography.bodyMedium)
                }
                book.status?.takeIf { it.isNotBlank() }?.let {
                    Text("状态 · $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BookDetailFavoriteChip(favoriteStatus)
                progress?.chapterTitle?.takeIf { it.isNotBlank() }?.let {
                    Text("上次读到: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (book.tags.isNotEmpty()) {
            NpChipRow {
                book.tags.forEach { tag -> NpChip(label = tag, tone = NpChipTone.Tag) }
            }
        }
    }
}

/** Counters are intentionally a compact glance rail rather than five oversized statistic cards. */
@Composable
private fun BookDetailStatisticsPanel(book: NovelCard) {
    val statistics = bookDetailStatistics(book)
    if (statistics.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.sm),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(NovalPieSize.hairline, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = NovalPieSpacing.sm, vertical = NovalPieSpacing.xxs)) {
            statistics.forEachIndexed { index, statistic ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f))
                BookDetailStatisticsRow(statistic = statistic)
            }
        }
    }
}

@Composable
private fun BookDetailStatisticsRow(statistic: BookDetailStatistic) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
    ) {
        Text(
            text = statistic.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = statistic.value,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BookDetailFavoriteChip(status: LoadResult<FavoriteStatus>) {
    // The colour now carries the meaning: collected is a positive status, an unavailable status is a
    // warning, everything else is neutral -- instead of every state sharing one primaryContainer pill.
    val (label, tone) = when (status) {
        LoadResult.Idle, LoadResult.Loading -> bookDetailFavoriteLoadingLabel() to NpChipTone.Neutral
        is LoadResult.Error -> bookDetailFavoriteUnavailableLabel() to NpChipTone.Warning
        is LoadResult.Success ->
            bookDetailFavoriteLabel(status.value.isFavorited) to
                if (status.value.isFavorited) NpChipTone.Status else NpChipTone.Neutral
    }
    NpChip(label = label, tone = tone)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BookDetailIntroduction(
    book: NovelCard,
    canManageBook: Boolean,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
) {
    NpCard {
        Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)) {
            Text(
                "作品简介",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
            ) {
                bookDetailFacts(book).forEach { fact ->
                    NpChip(label = fact, tone = NpChipTone.Neutral)
                }
            }
            if (book.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                ) {
                    book.tags.forEach { tag -> NpChip(label = tag, tone = NpChipTone.Tag) }
                }
            }
            Text(
                text = book.description?.trim().takeUnless { it.isNullOrBlank() }
                    ?: "源站暂未提供作品简介。",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.45f,
            )
            if (canManageBook) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NovalPieRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                ) {
                    Column(
                        modifier = Modifier.padding(NovalPieSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    ) {
                        Text(
                            "书籍管理",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                        ) {
                            OutlinedButton(
                                onClick = onEditInfo,
                                contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp),
                            ) { Text("编辑信息") }
                            OutlinedButton(
                                onClick = onManageChapters,
                                contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp),
                            ) { Text("章节管理") }
                            OutlinedButton(
                                onClick = onAppendChapters,
                                contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp),
                            ) { Text("追加章节") }
                        }
                    }
                }
            }
        }
    }
}

/** Source-style fixed actions: collection, menu, and the chapter entry point. */
@Composable
private fun BookDetailBottomActionBar(
    modifier: Modifier = Modifier,
    bookId: Long,
    bookTitle: String,
    nativeEpubDownloadState: NativeEpubDownloadState,
    chapterCount: Int,
    allowDownload: Boolean?,
    favoriteStatus: LoadResult<FavoriteStatus>,
    favoriteLoading: Boolean,
    progress: ReaderProgress?,
    firstChapter: Chapter?,
    hasAuthToken: Boolean,
    canManageBook: Boolean,
    onOpenReader: (Long, Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenTerminology: () -> Unit,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
    onDownloadEpub: () -> Unit,
    onDownloadTxt: () -> Unit,
    onOpenWeb: () -> Unit,
) {
    val context = LocalContext.current
    val isFavorited = (favoriteStatus as? LoadResult.Success)?.value?.isFavorited
    var menuExpanded by remember(bookId) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = NovalPieElevation.raised,
        shadowElevation = 4.dp,
    ) {
        Column {
            if (nativeEpubDownloadState.busy && nativeEpubDownloadState.bookId == bookId) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (nativeEpubDownloadState.message != null && nativeEpubDownloadState.bookId == bookId) {
                Text(
                    text = nativeEpubDownloadState.message.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = NovalPieSpacing.screenHorizontal,
                        end = NovalPieSpacing.screenHorizontal,
                        top = NovalPieSpacing.xs,
                    ),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovalPieSpacing.screenHorizontal, vertical = NovalPieSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            OutlinedButton(
                onClick = onToggleFavorite,
                enabled = isFavorited != null && !favoriteLoading,
                modifier = Modifier.weight(0.9f),
                contentPadding = PaddingValues(horizontal = NovalPieSpacing.xs, vertical = 0.dp),
            ) {
                if (favoriteLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(NovalPieSize.iconSm),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (isFavorited == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(NovalPieSize.iconSm),
                    )
                }
                Spacer(Modifier.width(NovalPieSpacing.xxs))
                Text(
                    when {
                        favoriteLoading -> "同步中"
                        isFavorited == true -> "已收藏"
                        isFavorited == false -> "收藏"
                        else -> "收藏"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                TextButton(
                    onClick = { menuExpanded = true },
                    contentPadding = PaddingValues(horizontal = NovalPieSpacing.xs, vertical = 0.dp),
                ) {
                    Text("菜单", maxLines = 1)
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(NovalPieSize.iconSm),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("术语表") },
                        onClick = {
                            menuExpanded = false
                            onOpenTerminology()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("分享") },
                        onClick = {
                            menuExpanded = false
                            val shareText = "$bookTitle\nhttps://novalpie.cc/book/$bookId"
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, shareText),
                                    "分享书籍",
                                ),
                            )
                        },
                    )
                    if (bookDetailAllowsNativeEpubDownload(hasAuthToken, allowDownload)) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    nativeEpubDownloadMenuLabel(
                                        isDownloading = nativeEpubDownloadState.busy &&
                                            nativeEpubDownloadState.bookId == bookId &&
                                            nativeEpubDownloadState.format == NativeBookDownloadFormat.Epub,
                                    )
                                )
                            },
                            enabled = !nativeEpubDownloadState.busy,
                            onClick = {
                                menuExpanded = false
                                onDownloadEpub()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    nativeTxtDownloadMenuLabel(
                                        isDownloading = nativeEpubDownloadState.busy &&
                                            nativeEpubDownloadState.bookId == bookId &&
                                            nativeEpubDownloadState.format == NativeBookDownloadFormat.Txt,
                                    )
                                )
                            },
                            enabled = !nativeEpubDownloadState.busy,
                            onClick = {
                                menuExpanded = false
                                onDownloadTxt()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("打开网页详情") },
                        onClick = {
                            menuExpanded = false
                            onOpenWeb()
                        },
                    )
                    if (canManageBook) {
                        DropdownMenuItem(
                            text = { Text("编辑信息") },
                            onClick = {
                                menuExpanded = false
                                onEditInfo()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("章节管理") },
                            onClick = {
                                menuExpanded = false
                                onManageChapters()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("追加章节") },
                            onClick = {
                                menuExpanded = false
                                onAppendChapters()
                            },
                        )
                    }
                }
            }
                Button(
                enabled = progress != null || firstChapter != null,
                onClick = {
                    when {
                        progress != null -> onOpenReader(progress.bookId, progress.chapterId)
                        firstChapter != null -> onOpenReader(bookId, firstChapter.id)
                    }
                },
                modifier = Modifier.weight(1.5f),
                contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp),
                ) {
                    Text(
                        if (progress != null) "继续阅读" else "立即阅读 (${chapterCount.coerceAtLeast(0)}章)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookDetailActionRow(
    bookId: Long,
    bookTitle: String,
    favoriteStatus: LoadResult<FavoriteStatus>,
    favoriteLoading: Boolean,
    progress: ReaderProgress?,
    firstChapter: Chapter?,
    hasAuthToken: Boolean,
    canManageBook: Boolean,
    onOpenReader: (Long, Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenTerminology: () -> Unit,
    onEditInfo: () -> Unit,
    onManageChapters: () -> Unit,
    onAppendChapters: () -> Unit,
    onDownloadEpub: () -> Unit,
    onDownloadTxt: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val actions = bookDetailPrimaryActions(hasProgress = progress != null)
    val continueLabel = actions.firstOrNull()
    val startLabel = if (progress != null) actions.getOrElse(1) { "开始阅读" } else actions.first()
    val webLabel = actions.last()
    val context = LocalContext.current
    val isFavorited = (favoriteStatus as? LoadResult.Success)?.value?.isFavorited
    var moreExpanded by remember(bookId) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            if (progress != null) {
                Button(
                    onClick = { onOpenReader(progress.bookId, progress.chapterId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(continueLabel ?: "继续阅读", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Button(
                enabled = firstChapter != null,
                onClick = { firstChapter?.let { onOpenReader(bookId, it.id) } },
                modifier = Modifier.weight(1f)
            ) {
                Text(startLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
        ) {
            OutlinedButton(
                onClick = onToggleFavorite,
                enabled = isFavorited != null && !favoriteLoading,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp),
            ) {
                if (favoriteLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(NovalPieSize.iconSm),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (isFavorited == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(NovalPieSize.iconSm),
                    )
                }
                Spacer(Modifier.width(NovalPieSpacing.xxs))
                Text(
                    when {
                        favoriteLoading -> "收藏同步中"
                        isFavorited == true -> "取消收藏"
                        isFavorited == false -> "收藏"
                        else -> "收藏不可用"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onOpenTerminology,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(NovalPieSize.iconSm),
                )
                Spacer(Modifier.width(NovalPieSpacing.xxs))
                Text("术语表", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                TextButton(
                    onClick = { moreExpanded = true },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = NovalPieSpacing.xs, vertical = 0.dp),
                ) {
                    Text("更多", maxLines = 1)
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(NovalPieSize.iconSm),
                    )
                }
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { moreExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("分享") },
                        onClick = {
                            moreExpanded = false
                            val shareText = "$bookTitle\nhttps://novalpie.cc/book/$bookId"
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, shareText),
                                    "分享书籍",
                                ),
                            )
                        },
                    )
                    if (bookDetailAllowsNativeEpubDownload(hasAuthToken, null)) {
                        DropdownMenuItem(
                            text = { Text(nativeEpubDownloadMenuLabel(isDownloading = false)) },
                            onClick = {
                                moreExpanded = false
                                onDownloadEpub()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(nativeTxtDownloadMenuLabel(isDownloading = false)) },
                            onClick = {
                                moreExpanded = false
                                onDownloadTxt()
                            },
                        )
                    }
                }
            }
        }
        TextButton(
            onClick = onOpenWeb,
            modifier = Modifier.align(Alignment.End),
            contentPadding = PaddingValues(horizontal = NovalPieSpacing.xs, vertical = 0.dp)
        ) {
            Icon(
                Icons.Filled.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(NovalPieSize.iconSm)
            )
            Spacer(Modifier.width(NovalPieSpacing.xxs))
            Text(webLabel)
        }
        if (canManageBook) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NovalPieRadius.sm),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            ) {
                Column(
                    modifier = Modifier.padding(NovalPieSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                ) {
                    Text(
                        "书籍管理",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                    ) {
                        OutlinedButton(
                            onClick = onEditInfo,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp)
                        ) { Text("编辑信息") }
                        OutlinedButton(
                            onClick = onManageChapters,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp)
                        ) { Text("章节管理") }
                        OutlinedButton(
                            onClick = onAppendChapters,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = NovalPieSpacing.sm, vertical = 0.dp)
                        ) { Text("追加章节") }
                    }
                }
            }
        }
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
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenWeb: () -> Unit
) {
    // A long composition box used to occupy the first screen of the \"书评\" tab, so existing
    // reviews looked missing until the user scrolled past it. Keep writing available, but lead
    // with the source reviews and reveal the editor deliberately.
    var composerVisible by remember(state.bookId) { mutableStateOf(false) }
    var expandedThreadIds by remember(state.bookId) { mutableStateOf(emptySet<Long>()) }
    LaunchedEffect(state.replyingToName) {
        if (state.replyingToName != null) composerVisible = true
    }
    val reviewSubtitle = when (comments) {
        LoadResult.Idle -> "等待同步"
        LoadResult.Loading -> "正在同步"
        is LoadResult.Error -> "同步失败"
        is LoadResult.Success -> chapterCommentThreadSummary(
            chapterCommentThreads(comments.value),
            rootLabel = "书评",
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title.ifBlank { bookCommentsSectionTitle() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    reviewSubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { composerVisible = !composerVisible }) {
                Text(if (composerVisible) "收起" else "写书评")
            }
            TextButton(
                onClick = onOpenWeb,
                modifier = Modifier.semantics { contentDescription = bookCommentsFallbackLabel() }
            ) { Text("网页") }
        }
        if (composerVisible) {
            ForumCommentComposer(
                draft = state.commentDraft,
                replyingToName = state.replyingToName,
                loading = state.actionLoading,
                message = state.actionMessage,
                onDraftChange = onDraftChange,
                onSubmit = onSubmit,
                onCancelReply = onCancelReply
            )
        }
        when (comments) {
            LoadResult.Idle -> LibraryStatusLine("等待加载书评")
            LoadResult.Loading -> LibraryLoadingBlock("正在同步书评")
            is LoadResult.Error -> NpErrorState(
                message = comments.message,
                retryLabel = retryActionLabel("书评"),
                onRetry = onRetry
            )
            is LoadResult.Success -> {
                if (comments.value.isEmpty()) {
                    LibraryStatusLine("还没有书评")
                } else {
                    val threads = chapterCommentThreads(comments.value)
                    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
                        threads.forEach { thread ->
                            ChapterCommentThreadBlock(
                                thread = thread,
                                expanded = expandedThreadIds.contains(thread.comment.id),
                                onToggleReplies = {
                                    val next = expandedThreadIds.toMutableSet()
                                    if (!next.add(thread.comment.id)) next.remove(thread.comment.id)
                                    expandedThreadIds = next
                                },
                                onLike = onLike,
                                onDislike = onDislike,
                                onEmoji = onEmoji,
                                onAward = onAward,
                                onReply = onReply,
                                onOpenUser = onOpenUser,
                                onOpenLink = onOpenLink,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One comment, on a book or on a chapter.
 *
 * There were two of these -- `BookCommentRow` and `ReaderChapterCommentRow` -- byte-identical down
 * to the whitespace, because both screens render the same `ChapterComment`. Keeping both meant every
 * fix had to be made twice or the two would drift.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterCommentCard(
    comment: ChapterComment,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onEmoji: () -> Unit,
    onAward: () -> Unit,
    onReply: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
    compact: Boolean = false,
) {
    val authorName = comment.authorName ?: "匿名用户"
    val links = forumContentLinks(listOf(comment.content))
    NpCard(contentPadding = if (compact) NovalPieSpacing.md else NovalPieSpacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.clickable(enabled = comment.authorId != null) {
                    comment.authorId?.let(onOpenUser)
                },
            ) {
                ForumProfileAvatar(
                    authorName = authorName,
                    avatarUrl = comment.authorAvatarUrl,
                    avatarFrameUrl = comment.authorAvatarFrameUrl,
                    size = if (compact) 36.dp else 40.dp,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = comment.authorId != null) { comment.authorId?.let(onOpenUser) },
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
            ) {
                Text(
                    authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comment.authorBadges.isNotEmpty() || comment.authorBadgeVisuals.isNotEmpty()) {
                    ForumAuthorBadgeRow(
                        labels = comment.authorBadges,
                        visuals = comment.authorBadgeVisuals,
                    )
                }
                comment.createdAt?.let {
                    Text(
                        forumShortDateLabel(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        comment.replyToName?.let {
            Text("回复 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        ForumRichContent(
            content = comment.content,
            style = MaterialTheme.typography.bodyMedium,
            onOpenLink = onOpenLink,
            emptyLabel = "评论内容暂时为空",
        )
        ForumLinkPreviewRows(links, onOpenLink)
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

@Composable
private fun ChapterCommentThreadBlock(
    thread: ChapterCommentThread,
    expanded: Boolean,
    onToggleReplies: () -> Unit,
    onLike: (ChapterComment) -> Unit,
    onDislike: (ChapterComment) -> Unit,
    onEmoji: (ChapterComment) -> Unit,
    onAward: (ChapterComment) -> Unit,
    onReply: (ChapterComment) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val visibleReplyCount = maxOf(thread.comment.replyCount ?: 0, thread.replies.size)
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
        ChapterCommentCard(
            comment = thread.comment,
            onLike = { onLike(thread.comment) },
            onDislike = { onDislike(thread.comment) },
            onEmoji = { onEmoji(thread.comment) },
            onAward = { onAward(thread.comment) },
            onReply = { onReply(thread.comment) },
            onOpenUser = onOpenUser,
            onOpenLink = onOpenLink,
        )
        if (thread.replies.isNotEmpty()) {
            TextButton(
                onClick = onToggleReplies,
                modifier = Modifier.padding(start = NovalPieSpacing.sm),
            ) {
                val count = visibleReplyCount.coerceAtLeast(thread.replies.size)
                Text(if (expanded) "收起 $count 条回复" else "展开 $count 条回复")
            }
            if (expanded) {
                thread.replies.forEach { reply ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = NovalPieSpacing.lg)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(NovalPieRadius.md),
                            )
                            .padding(start = NovalPieSpacing.xs),
                    ) {
                        ChapterCommentCard(
                            comment = reply,
                            onLike = { onLike(reply) },
                            onDislike = { onDislike(reply) },
                            onEmoji = { onEmoji(reply) },
                            onAward = { onAward(reply) },
                            onReply = { onReply(reply) },
                            onOpenUser = onOpenUser,
                            onOpenLink = onOpenLink,
                            compact = true,
                        )
                    }
                }
            }
        } else if (visibleReplyCount > 0) {
            Text(
                "$visibleReplyCount 条回复正在等待源站返回",
                modifier = Modifier.padding(start = NovalPieSpacing.sm),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
internal fun BookCover(
    title: String,
    coverUrl: String?,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    modifier: Modifier = Modifier,
    previewUrl: String? = coverUrl,
    previewPolicy: CoverPreviewPolicy = CoverPreviewPolicy.LongPressOnly,
    onPreview: (() -> Unit)? = null,
    requestSize: NovelCoverRequestSize? = null,
    staticImage: Boolean = false,
) {
    val shape = RoundedCornerShape(6.dp)
    val fallbackText = bookCoverFallbackText(title)
    var previewVisible by remember(previewUrl) { mutableStateOf(false) }
    var coverFailed by remember(coverUrl) { mutableStateOf(false) }
    val context = LocalContext.current
    val imageRequest = remember(coverUrl, context, requestSize, staticImage) {
        coverUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val targetSize = requestSize ?: novalPieBookCoverTargetSize(null, null)
            if (staticImage) {
                novalPieStaticImageRequest(
                    context = context,
                    url = url,
                    widthPx = targetSize.widthPx,
                    heightPx = targetSize.heightPx,
                )
            } else {
                novalPieBookCoverRequest(
                    context,
                    url,
                    targetSize = targetSize,
                )
            }
        }
    }
    var boxModifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
    if (width != Dp.Unspecified) boxModifier = boxModifier.width(width)
    if (height != Dp.Unspecified) boxModifier = boxModifier.height(height)
    if (width == Dp.Unspecified && height == Dp.Unspecified) boxModifier = boxModifier.fillMaxSize()
    if (!previewUrl.isNullOrBlank()) {
        boxModifier = when (previewPolicy) {
            CoverPreviewPolicy.Disabled -> boxModifier
            CoverPreviewPolicy.LongPressOnly -> boxModifier.longPressOnly(LocalViewConfiguration.current.touchSlop) {
                if (onPreview != null) onPreview() else previewVisible = true
            }
        }
    }
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest == null || coverFailed) {
            // Search results genuinely can have no source photo_url. Make that state explicit
            // instead of leaving a gray initial that reads as an endlessly loading image.
            BookCoverUnavailable(fallbackText)
        } else {
            // Draw the fallback underneath: unlike SubcomposeAsyncImage this keeps Lazy grids
            // on the fast composition path while preserving a useful loading/error state.
            BookCoverLoadingFallback(fallbackText)
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { coverFailed = true },
            )
        }
    }
    if (previewVisible && !previewUrl.isNullOrBlank()) {
        ImagePreviewDialog(imageUrl = previewUrl, title = "$title · 封面", onDismiss = { previewVisible = false })
    }
}

internal enum class CoverPreviewPolicy {
    Disabled,
    LongPressOnly,
}

/**
 * A cover preview is an explicit inspection gesture, never a navigation shortcut.  Detail keeps
 * the same stationary-long-press rule as Search and reader illustrations; collection and upload
 * management cards opt out entirely.
 */
internal fun bookDetailCoverPreviewPolicy(): CoverPreviewPolicy = CoverPreviewPolicy.LongPressOnly

private enum class StationaryLongPressOutcome {
    Released,
    Moved,
    Cancelled,
    Triggered,
}

/**
 * Watches a still pointer without taking ownership of ordinary taps or scrolling. The parent
 * card remains the click target; only a stationary press held past the platform long-press
 * threshold opens the image preview.
 */
private fun Modifier.longPressOnly(
    touchSlopPx: Float,
    onLongPress: () -> Unit,
): Modifier = pointerInput(touchSlopPx) {
    awaitEachGesture {
        // Observe before the ancestor card's clickable consumes the stream. This preserves the
        // parent tap while still letting movement past touch slop cancel the long press.
        val firstEvent = awaitPointerEvent(PointerEventPass.Initial)
        val firstChange = firstEvent.changes.firstOrNull()
        if (firstChange == null || !firstChange.pressed) {
            return@awaitEachGesture
        }

        val downPosition = firstChange.position
        // This is AwaitPointerEventScope.withTimeoutOrNull, not the general coroutine helper. The
        // restricted scope permits awaitPointerEvent inside the timer and keeps the event pass
        // ahead of the ancestor combinedClickable.
        val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull()
                    ?: return@withTimeoutOrNull StationaryLongPressOutcome.Cancelled
                if (!change.pressed) {
                    return@withTimeoutOrNull StationaryLongPressOutcome.Released
                }
                if (event.changes.size > 1 ||
                    (change.position - downPosition).getDistance() > touchSlopPx
                ) {
                    return@withTimeoutOrNull StationaryLongPressOutcome.Moved
                }
            }
        } ?: StationaryLongPressOutcome.Triggered

        if (outcome == StationaryLongPressOutcome.Triggered) {
            // Trigger at the platform long-press threshold, even when the finger remains still and
            // produces no move event. Consume the eventual up so the ancestor card cannot also
            // perform its ordinary click after the preview opens.
            onLongPress()
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.isEmpty() || event.changes.none { it.pressed }) {
                    event.changes.forEach { it.consume() }
                    break
                }
            }
        }
    }
}

@Composable
private fun BookCoverLoadingFallback(value: String) {
    Text(
        value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun BookCoverUnavailable(value: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.padding(NovalPieSpacing.sm).size(NovalPieSize.iconMd)
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "暂无封面",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

internal fun bookCoverFallbackText(title: String): String =
    title.trim().firstOrNull()?.toString() ?: "N"

internal fun bookCoverAspectRatio(): Float = 2f / 3f

internal fun novelGridColumnCount(): Int = 2

@Composable
private fun ChapterRow(
    chapter: Chapter,
    selected: Boolean,
    context: ChapterListContext,
    onClick: () -> Unit,
) {
    val presentation = chapterListPresentation(chapter, context)
    // Separation comes from container colour plus a hairline, like every other card in the system;
    // the chapter the reader is on gets a primary-coloured border rather than only a bold title.
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            NovalPieSize.hairline,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    chapter.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    presentation.numberLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                presentation.updatedLabel?.let { updated ->
                    Text(
                        updated,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } ?: Spacer(Modifier.weight(1f))
                presentation.metrics.forEach { metric ->
                    Text(
                        metric,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
    // The one search input, shared with the reader's catalog panel. Filters as you type, so there is
    // no submit action; clearing is one tap on the trailing icon.
    NpSearchField(
        value = value,
        onValueChange = onValueChange,
        onSearch = {},
        placeholder = "搜索章节...",
        clearContentDescription = "清除筛选"
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.readerBodyItems(
    contents: List<ReaderChapterContent>,
    options: ReaderUiOptions,
    highlightedText: String? = null,
    chapterCommentStates: Map<Long, ReaderChapterCommentState>,
    fallbackCommentState: ReaderState,
    onRetryChapterComments: (Long) -> Unit,
    onDraftChange: (Long, String) -> Unit,
    onSubmit: (Long) -> Unit,
    onReply: (Long, ChapterComment) -> Unit,
    onCancelReply: (Long) -> Unit,
    onLike: (Long, ChapterComment) -> Unit,
    onDislike: (Long, ChapterComment) -> Unit,
    onEmoji: (Long, ChapterComment) -> Unit,
    onAward: (Long, ChapterComment) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenWeb: (Long) -> Unit,
    onPreviewImage: (ReaderContentBlock.Image, String) -> Unit,
) {
    val textLayout = readerTextLayout(options)
    var imageOrdinal = 0
    contents.forEachIndexed { chapterIndex, chapter ->
        val blocks = readerBlocksForContent(chapter.content)
        val visibleBlocks = readerBlocksForDisplay(blocks, options.removeDuplicateLines)
        chapter.title?.takeIf(String::isNotBlank)?.let { title ->
            item(key = "reader-title-${chapter.chapterId}") {
                ReaderArticleItem(
                    horizontalPadding = textLayout.horizontalPaddingDp.dp,
                    contentWidthDp = textLayout.contentWidthDp,
                    // The source puts a 16dp separator margin after its article's 20dp top
                    // padding. Reproduce that first-chapter breathing room before the top rule.
                    topPadding = 36.dp,
                ) {
                    ReaderChapterHeading(title, textLayout, readerPalette(options))
                }
            }
        }
        visibleBlocks.forEachIndexed { index, block ->
            when (block) {
                is ReaderContentBlock.Text -> item(key = "reader-text-${chapter.chapterId}-$index") {
                    ReaderArticleItem(
                        horizontalPadding = textLayout.horizontalPaddingDp.dp,
                        contentWidthDp = textLayout.contentWidthDp,
                        topPadding = if (index == 0 && chapter.title.isNullOrBlank()) 20.dp else 0.dp,
                        bottomPadding = textLayout.paragraphSpacingDp.dp,
                    ) {
                        ReaderParagraph(block.value, textLayout, readerPalette(options), highlightedText)
                    }
                }
                is ReaderContentBlock.Image -> if (options.showImages) {
                    imageOrdinal += 1
                    val currentImageOrdinal = imageOrdinal
                    item(key = "reader-image-${chapter.chapterId}-${block.url}-$index") {
                        ReaderArticleItem(
                            horizontalPadding = textLayout.horizontalPaddingDp.dp,
                            contentWidthDp = textLayout.contentWidthDp,
                            topPadding = 6.dp,
                            bottomPadding = 6.dp,
                        ) {
                            ReaderIllustration(
                                image = block,
                                ordinal = currentImageOrdinal,
                                palette = readerPalette(options),
                                onPreview = {
                                    onPreviewImage(block, readerIllustrationLabel(block.alt, currentImageOrdinal))
                                },
                            )
                        }
                    }
                }
            }
        }
        item(key = "reader-chapter-finish-${chapter.chapterId}") {
            ReaderArticleItem(
                horizontalPadding = textLayout.horizontalPaddingDp.dp,
                contentWidthDp = textLayout.contentWidthDp,
                topPadding = 8.dp,
                bottomPadding = 20.dp,
            ) {
                ReaderChapterFinish(readerPalette(options))
            }
        }
        if (options.showComments) {
            item(key = "reader-chapter-comments-${chapter.chapterId}") {
                ReaderChapterCommentsSection(
                    chapterId = chapter.chapterId,
                    commentState = chapterCommentStates[chapter.chapterId]
                        ?: readerChapterCommentState(fallbackCommentState, chapter.chapterId),
                    onRetry = { onRetryChapterComments(chapter.chapterId) },
                    onDraftChange = { value -> onDraftChange(chapter.chapterId, value) },
                    onSubmit = { onSubmit(chapter.chapterId) },
                    onReply = { comment -> onReply(chapter.chapterId, comment) },
                    onCancelReply = { onCancelReply(chapter.chapterId) },
                    onLike = { comment -> onLike(chapter.chapterId, comment) },
                    onDislike = { comment -> onDislike(chapter.chapterId, comment) },
                    onEmoji = { comment -> onEmoji(chapter.chapterId, comment) },
                    onAward = { comment -> onAward(chapter.chapterId, comment) },
                    onOpenUser = onOpenUser,
                    onOpenLink = onOpenLink,
                    onOpenWeb = { onOpenWeb(chapter.chapterId) },
                )
            }
        }
    }
}

/**
 * Kept directly after the article body so comments cannot delay the next chapter request. The
 * stable LazyColumn key above observes this item entering the viewport and starts the request.
 */
@Composable
private fun ReaderInfiniteScrollEnd(
    state: ReaderState,
    chapters: List<Chapter>,
    chapterContents: List<ReaderChapterContent>,
    onRetryCatalog: () -> Unit,
    onRetryNextChapter: () -> Unit,
) {
    val loadedIds = chapterContents.map { it.chapterId }.toSet().ifEmpty { setOf(state.chapterId) }
    val next = nextReaderChapterForInfiniteScroll(state.chapterId, chapters, loadedIds)
    when {
        state.loadingNextChapter -> LoadingBlock("正在加载下一章")
        state.nextChapterError != null -> ErrorBlock(
            message = state.nextChapterError,
            retryLabel = "重试下一章",
            onRetry = onRetryNextChapter,
        )
        state.chapters is LoadResult.Idle || state.chapters is LoadResult.Loading ->
            LibraryLoadingBlock("正在同步章节目录")
        state.chapters is LoadResult.Error -> NpErrorState(
            message = (state.chapters as LoadResult.Error).message,
            retryLabel = retryActionLabel("章节目录"),
            onRetry = onRetryCatalog,
        )
        state.nextChapterWaitingForCatalog -> ErrorBlock(
            message = "章节目录暂时不完整，无法确认下一章",
            retryLabel = "重试章节目录",
            onRetry = onRetryCatalog,
        )
        // An empty catalog must not be treated as a terminal chapter: it only means the API has
        // not supplied enough data to determine the successor. The catalog state above explains
        // that condition; only the ViewModel-confirmed terminal flag means the book is finished.
        state.nextChapterExhausted ->
            LibraryStatusLine("已读完全部章节")
        next == null -> TextButton(
            onClick = onRetryNextChapter,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("继续下滑加载下一章 · 点此重试") }
        else -> TextButton(
            onClick = onRetryNextChapter,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("继续下滑自动加载 · 立即加载下一章") }
    }
}

@Composable
private fun ReaderArticleItem(
    horizontalPadding: Dp,
    contentWidthDp: Int = 800,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .widthIn(max = contentWidthDp.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(
                    start = horizontalPadding,
                    top = topPadding,
                    end = horizontalPadding,
                    bottom = bottomPadding,
                ),
        ) { content() }
    }
}

@Composable
private fun ReaderChapterHeading(
    title: String,
    textLayout: ReaderTextLayout,
    palette: ReaderPalette,
) {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Source chapter separators use a 2px top rule before the subdued heading, not a
            // heavy divider below it. Keeping it above the title produces the same calm rhythm.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(palette.text.copy(alpha = 0.1f)),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = textLayout.titleFontSizeSp.sp,
                    lineHeight = (textLayout.titleFontSizeSp * 1.4f).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = palette.meta,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(textLayout.titleBottomSpacingDp.dp))
        }
    }
}

@Composable
private fun ReaderParagraph(
    value: String,
    textLayout: ReaderTextLayout,
    palette: ReaderPalette,
    highlightedText: String? = null,
) {
    SelectionContainer {
        val displayText = readerTextWithWordSpacing(value, textLayout.wordSpacingSp)
        val highlight = highlightedText?.trim()?.takeIf(String::isNotBlank)
        val annotated = buildAnnotatedString {
            val start = highlight?.let(displayText::indexOf)?.takeIf { it >= 0 }
            if (start == null) {
                append(displayText)
            } else {
                val match = highlight.orEmpty()
                append(displayText.substring(0, start))
                withStyle(SpanStyle(background = palette.text.copy(alpha = 0.2f))) {
                    append(displayText.substring(start, start + match.length))
                }
                append(displayText.substring(start + match.length))
            }
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = textLayout.fontSizeSp.sp,
                lineHeight = textLayout.lineHeightSp.sp,
                textIndent = TextIndent(firstLine = textLayout.firstLineIndentSp.sp),
                fontFamily = readerFontFamily(textLayout.fontFamily),
                fontWeight = FontWeight(textLayout.fontWeight),
                letterSpacing = textLayout.letterSpacingSp.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
            ),
            color = palette.text,
        )
    }
}

@Composable
private fun ReaderChapterFinish(palette: ReaderPalette) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.28f).height(1.dp)
                .background(palette.text.copy(alpha = 0.16f)),
        )
        Text(
            "本章结束",
            style = MaterialTheme.typography.labelSmall,
            color = palette.meta,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderIllustration(
    image: ReaderContentBlock.Image,
    ordinal: Int,
    palette: ReaderPalette,
    onPreview: () -> Unit,
) {
    val label = readerIllustrationLabel(image.alt, ordinal)
    val touchSlop = LocalViewConfiguration.current.touchSlop
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .longPressOnly(touchSlop, onPreview),
        shape = RoundedCornerShape(8.dp),
        color = palette.text.copy(alpha = 0.035f),
        contentColor = palette.text,
        tonalElevation = NovalPieElevation.none,
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
}

internal fun readerIllustrationLabel(alt: String?, ordinal: Int): String =
    alt?.trim()?.takeIf { it.isNotBlank() } ?: "正文插图 ${ordinal.coerceAtLeast(1)}"

internal fun readerIllustrationContentDescription(label: String): String =
    "$label，长按查看大图"

internal fun readerIllustrationPreviewHint(): String = "长按看大图"

internal fun readerIllustrationLoadingLabel(): String = "正在加载插图"

internal fun readerIllustrationErrorLabel(): String = "插图加载失败"

private data class ReaderPalette(
    val background: Color,
    val text: Color,
    val meta: Color,
    val sidebarBackground: Color = background,
    val sidebarText: Color = text,
    val accent: Color = Color(0xFF2563EB),
    val backgroundImageUri: String? = null,
)

private fun ReaderPalette.sidebarPalette(): ReaderPalette = copy(
    background = sidebarBackground,
    text = sidebarText,
    meta = sidebarText.copy(alpha = 0.68f),
)

@Composable
private fun readerPalette(options: ReaderUiOptions): ReaderPalette = novalPieReaderPalette(
    theme = options.theme,
    customThemes = options.customThemes,
).let {
    ReaderPalette(
        background = it.background,
        text = it.text,
        meta = it.meta,
        sidebarBackground = it.sidebarBackground,
        sidebarText = it.sidebarText,
        accent = it.accent,
        backgroundImageUri = it.backgroundImageUri,
    )
}

private fun readerFontFamily(value: String): FontFamily = when (value) {
    "serif" -> FontFamily.Serif
    "sans" -> FontFamily.SansSerif
    "monospace" -> FontFamily.Monospace
    else -> FontFamily.Default
}

internal fun String.themeLabel(): String = when (this) {
    "light" -> "明亮"
    "sepia" -> "护眼"
    "green" -> "绿色"
    "gray" -> "灰色"
    "dark" -> "深色"
    "high_contrast" -> "高对比"
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
    onOpenSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NovalPieSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("需要登录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "登录后即可查看和管理您的收藏，记录阅读历史，享受更多个性化功能",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onOpenLogin,
            modifier = Modifier.fillMaxWidth()
        ) { Text("前往登录") }
        OutlinedButton(
            onClick = onOpenSearch,
            modifier = Modifier.fillMaxWidth()
        ) { Text("去搜索小说") }
    }
}

internal fun filterBooks(books: List<NovelCard>, query: String): List<NovelCard> {
    val normalized = query.trim()
    if (normalized.isBlank()) return books
    return books.filter { book -> bookMatchesQuery(book, normalized) }
}

private fun filterChapters(chapters: List<Chapter>, query: String): List<Chapter> {
    val normalized = query.trim()
    if (normalized.isBlank()) return chapters
    return chapters.filter { chapter -> chapterMatchesQuery(chapter, normalized) }
}
