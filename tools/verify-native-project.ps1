param(
    [switch]$RequireApk
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Read-Required {
    param([string]$Path)
    Assert-True -Condition (Test-Path -LiteralPath $Path) -Message "Missing required file: $Path"
    return Get-Content -LiteralPath $Path -Raw
}

$settings = Read-Required (Join-Path $ProjectRoot "settings.gradle")
$rootBuild = Read-Required (Join-Path $ProjectRoot "build.gradle")
$appBuild = Read-Required (Join-Path $ProjectRoot "app\build.gradle")
$manifest = Read-Required (Join-Path $ProjectRoot "app\src\main\AndroidManifest.xml")
$mainActivity = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\MainActivity.kt")
$appSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt")
$productCopySource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\ProductCopy.kt")
$libraryPresentationSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\LibraryPresentation.kt")
$discoverPresentationSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\DiscoverPresentation.kt")
$profilePresentationSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\ProfilePresentation.kt")
$viewModelSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\NovalPieViewModel.kt")
$apiMessagesSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\ApiMessages.kt")
$requestFreshnessSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\RequestFreshness.kt")
$routeStackPolicySource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\RouteStackPolicy.kt")
$readerPresentationSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\ReaderPresentation.kt")
$readerTextSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\ui\ReaderText.kt")
$apiSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\data\NovalPieApi.kt")
$networkConfigSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\data\NetworkConfigStore.kt")
$modelSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\model\Models.kt")
$authStoreSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\data\AuthSessionStore.kt")
$readerProgressStoreSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\data\ReaderProgressStore.kt")
$readerSettingsStoreSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\data\ReaderSettingsStore.kt")
$searchHistoryStoreSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\data\SearchHistoryStore.kt")
$searchSettingsStoreSource = Read-Required (Join-Path $ProjectRoot "app\src\main\java\com\novalpie\nativeapp\data\SearchSettingsStore.kt")
$readerProgressStoreTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\data\ReaderProgressStoreTest.kt")
$searchHistoryStoreTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\data\SearchHistoryStoreTest.kt")
$searchSettingsStoreTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\data\SearchSettingsStoreTest.kt")
$apiTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\data\NovalPieApiTest.kt")
$apiFailureMessageTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\ApiFailureMessageTest.kt")
$productCopyTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\ProductCopyTest.kt")
$libraryPresentationTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\LibraryPresentationTest.kt")
$discoverPresentationTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\DiscoverPresentationTest.kt")
$profilePresentationTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\ProfilePresentationTest.kt")
$bookCoverFallbackTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\BookCoverFallbackTest.kt")
$requestFreshnessTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\RequestFreshnessTest.kt")
$routeStackPolicyTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\RouteStackPolicyTest.kt")
$readerPresentationTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\ReaderPresentationTest.kt")
$readerTextTestSource = Read-Required (Join-Path $ProjectRoot "app\src\test\java\com\novalpie\nativeapp\ui\ReaderTextTest.kt")
$mumuVerifySource = Read-Required (Join-Path $ProjectRoot "tools\verify-mumu-compose-launch.ps1")

Assert-True -Condition ($settings -match "include\s+['""]\:app['""]") -Message "Gradle settings must include only the native app module."
Assert-True -Condition ($rootBuild -notmatch "capacitor|getcapacitor|cordova") -Message "Root Gradle build must not reference Capacitor or Cordova."
Assert-True -Condition ($appBuild -match "com\.android\.application") -Message "App module must use the Android application plugin."
Assert-True -Condition ($appBuild -match "kotlin-android") -Message "App module must use Kotlin Android."
Assert-True -Condition ($appBuild -match "compose\s+true") -Message "App module must enable Jetpack Compose."
Assert-True -Condition ($appBuild -match "applicationId\s+['""]com\.novalpie\.app['""]") -Message "Native 2.0 must keep the NovalPie package id for overwrite installs."
Assert-True -Condition ($appBuild -notmatch "capacitor|getcapacitor|cordova") -Message "App Gradle build must not depend on Capacitor or Cordova."
Assert-True -Condition ($appBuild.Contains("junit:junit")) -Message "App module must include native unit-test support."
Assert-True -Condition ($appBuild.Contains("robolectric")) -Message "App module must include Robolectric for Android SharedPreferences tests."

Assert-True -Condition ($manifest -match "com\.novalpie\.nativeapp\.MainActivity") -Message "Manifest must launch the native MainActivity."
Assert-True -Condition ($manifest -notmatch "BridgeActivity|Capacitor") -Message "Manifest must not launch a Capacitor bridge activity."
Assert-True -Condition ($manifest -match "android:scheme=""novalpie""") -Message "Manifest must expose the native novalpie deep link scheme."
Assert-True -Condition ($mainActivity -match "ComponentActivity") -Message "MainActivity must be a native ComponentActivity."
Assert-True -Condition ($mainActivity -match "setContent") -Message "MainActivity must enter Compose through setContent."
Assert-True -Condition ($mainActivity -match "startUri") -Message "MainActivity must pass deep-link startUri into Compose."
Assert-True -Condition ($mainActivity -notmatch "WebView|loadUrl|file:///android_asset|appassets\.androidplatform\.net") -Message "MainActivity must not be a WebView/local-HTML launcher."

Assert-True -Condition ($appSource -match "LaunchedEffect") -Message "Compose app must consume the native deep link once."
Assert-True -Condition ($appSource.Contains("NOVALPIE_NATIVE_COMPOSE_HOME")) -Message "Default home screen must expose a UIAutomator-visible native Compose marker."
Assert-True -Condition ($mumuVerifySource.Contains("NOVALPIE_NATIVE_COMPOSE_HOME")) -Message "MuMu launch verifier must check the native Compose marker."
Assert-True -Condition ($mumuVerifySource.Contains("uiautomator dump")) -Message "MuMu launch verifier must dump the Android UI tree."
Assert-True -Condition ($mumuVerifySource.Contains("screencap")) -Message "MuMu launch verifier must capture a screenshot."
$readerBodyIndex = $appSource.IndexOf("ReaderBody(content.value, options)")
$readerCatalogIndex = $appSource.IndexOf("CatalogFilterField(catalogQuery, onCatalogQueryChange)", $readerBodyIndex)
Assert-True -Condition ($readerBodyIndex -ge 0 -and $readerCatalogIndex -gt $readerBodyIndex) -Message "Reader screen must render content before the chapter catalog."
foreach ($screenFunction in @("HomeScreen", "SearchScreen", "BookDetailScreen", "ReaderScreen", "SettingsScreen")) {
    Assert-True -Condition ($appSource.Contains("private fun $screenFunction")) -Message "Missing first-stage native screen: $screenFunction"
}

foreach ($routeTarget in @("AppRoute.Home", "AppRoute.Search", "AppRoute.Settings", "AppRoute.BookDetail", "AppRoute.Reader", "AppRoute.WebFallback")) {
    Assert-True -Condition ($appSource.Contains($routeTarget)) -Message "Compose app must route $routeTarget"
}

foreach ($proxySignal in @("proxyEnabled", "proxyHost", "proxyPort", "onSaveProxy")) {
    Assert-True -Condition ($appSource.Contains($proxySignal)) -Message "Settings screen must expose native API proxy control: $proxySignal"
}

foreach ($homeSignal in @("ContinueReadingCard", "RecentReadingSection", "recentReaderProgresses", "GroupSection", "UserSection", "filterBooks", "onBookshelfQueryChange")) {
    Assert-True -Condition ($appSource.Contains($homeSignal)) -Message "Home/bookshelf screen must include native signal: $homeSignal"
}
foreach ($librarySignal in @("LibraryOverview", "libraryOverview", "LibraryOverviewBlock", "LibraryShelfControls", "libraryFavoritesTitle")) {
    Assert-True -Condition ($appSource.Contains($librarySignal) -or $libraryPresentationSource.Contains($librarySignal)) -Message "Library screen must include native reading-client signal: $librarySignal"
}
foreach ($libraryTestSignal in @("libraryOverviewReadsLikeAReaderLibraryClient", "libraryOverviewShowsUnsignedStateWithoutDebugLanguage", "libraryShelfSectionTitlesStayCompact")) {
    Assert-True -Condition ($libraryPresentationTestSource.Contains($libraryTestSignal)) -Message "Library presentation must have unit coverage for: $libraryTestSignal"
}

foreach ($forumSignal in @("ForumFeedItem", "authorName", "tags", "pinned", "replyCount", "CompactForumBadge", "forumFeedBadges", "forumFeedMetaLine")) {
    Assert-True -Condition ($appSource.Contains($forumSignal) -or $productCopySource.Contains($forumSignal)) -Message "Forum feed must include native forum-client signal: $forumSignal"
}
foreach ($forumTestSignal in @("forumHomeUsesForumClientFeedStructure", "forumFeedCopyAvoidsUnsupportedReaderTooling")) {
    Assert-True -Condition ($productCopyTestSource.Contains($forumTestSignal)) -Message "Forum feed presentation must have unit coverage for: $forumTestSignal"
}

foreach ($homePagingSignal in @("loadMoreFavorites", "favoritesCanLoadMore", "favoritesLoadingMore", "onLoadMoreFavorites")) {
    Assert-True -Condition ($appSource.Contains($homePagingSignal) -or $viewModelSource.Contains($homePagingSignal)) -Message "Home/bookshelf pagination must include native signal: $homePagingSignal"
}

foreach ($searchSignal in @("SearchHistorySection", "searchHistory", "onUseSearchHistory", "SearchOptionSection", "ChoiceChips", "sortBy", "sortOrder", "scope", "matchType", "adultFilter")) {
    Assert-True -Condition ($appSource.Contains($searchSignal) -or $viewModelSource.Contains($searchSignal)) -Message "Search screen must include native control: $searchSignal"
}
foreach ($discoverSignal in @("DiscoverOverview", "discoverOverview", "DiscoverSearchPanel", "discoverFilterGroups", "DiscoverFilterGroup", "discoverQuickPrompts", "DiscoverIdlePanel")) {
    Assert-True -Condition ($appSource.Contains($discoverSignal) -or $discoverPresentationSource.Contains($discoverSignal)) -Message "Discover screen must include native content-client signal: $discoverSignal"
}
foreach ($discoverTestSignal in @("discoverOverviewUsesContentClientSearchLanguage", "discoverFilterGroupsMatchWebsiteSearchControls", "discoverUnsupportedReaderToolingDoesNotAppear", "discoverEmptyStateOffersSearchPromptsInsteadOfBlankSpace")) {
    Assert-True -Condition ($discoverPresentationTestSource.Contains($discoverTestSignal)) -Message "Discover presentation must have unit coverage for: $discoverTestSignal"
}

foreach ($searchPagingSignal in @("loadMoreSearch", "searchCanLoadMore", "searchLoadingMore", "onLoadMore", "LoadMoreRow")) {
    Assert-True -Condition ($appSource.Contains($searchPagingSignal) -or $viewModelSource.Contains($searchPagingSignal)) -Message "Search pagination must include native signal: $searchPagingSignal"
}

foreach ($bookDetailSignal in @("BookDetailHero", "BookDetailActionRow", "bookDetailPrimaryActions", "bookDetailFavoriteLabel", "readerProgress", "CatalogFilterField", "filterChapters", "BookCover")) {
    Assert-True -Condition ($appSource.Contains($bookDetailSignal)) -Message "Book detail/catalog must include native signal: $bookDetailSignal"
}

foreach ($readerSignal in @("ReaderToolbar", "ReaderBody", "ReaderCatalogPanel", "readerCatalogPanelTitle", "readerCloseCatalogLabel", "increaseReaderFont", "decreaseReaderFont", "cycleReaderTheme", "ReaderProgressStore", "ReaderSettingsStore")) {
    Assert-True -Condition ($appSource.Contains($readerSignal) -or $viewModelSource.Contains($readerSignal) -or $readerPresentationSource.Contains($readerSignal)) -Message "Reader must include native signal: $readerSignal"
}
Assert-True -Condition ($appSource.Contains("readerParagraphsFromContent(content.content)")) -Message "Reader body must use the shared native reader text normalizer."
Assert-True -Condition ($readerPresentationTestSource.Contains("readerCatalogPanelUsesReaderAppLanguage")) -Message "Reader presentation must cover the native catalog panel language."
foreach ($readerForbiddenSignal in @("书源", "规则", "爬取", "下载", "净化", "编辑源")) {
    Assert-True -Condition (-not $readerPresentationSource.Contains($readerForbiddenSignal)) -Message "Reader presentation must not contain mojibake/unsupported tooling: $readerForbiddenSignal"
}

foreach ($profileSignal in @("ProfileOverview", "profileOverview", "ProfileOverviewBlock", "ProfileAccountCard", "ProfileReaderCard", "ProfileConnectionCard", "profileWebActions")) {
    Assert-True -Condition ($appSource.Contains($profileSignal) -or $profilePresentationSource.Contains($profileSignal)) -Message "Profile screen must include native user-center signal: $profileSignal"
}
foreach ($profileTestSignal in @("profileOverviewUsesUserCenterLanguage", "profileOverviewShowsGuestStateCleanly", "profileSectionsStayProductFacing", "profileCopyDoesNotExposeDebugOrUnsupportedReaderTooling")) {
    Assert-True -Condition ($profilePresentationTestSource.Contains($profileTestSignal)) -Message "Profile presentation must have unit coverage for: $profileTestSignal"
}
foreach ($profileActionSignal in @("onSaveProxy", "onClearToken", "onOpenLogin", "onProxyEnabledChange", "onOpenHomeFallback", "onOpenSearchFallback")) {
    Assert-True -Condition ($appSource.Contains($profileActionSignal)) -Message "Profile screen must retain native action signal: $profileActionSignal"
}
foreach ($removedDebugSignal in @("RuntimeModeCard", "SettingsAccountCard", "SettingsReaderCard", "Package: com.novalpie.app")) {
    Assert-True -Condition (-not $appSource.Contains($removedDebugSignal)) -Message "Profile screen must not expose debug/settings shell copy: $removedDebugSignal"
}

Assert-True -Condition ($appSource.Contains("SubcomposeAsyncImage")) -Message "Native UI must render remote covers with Coil SubcomposeAsyncImage."
foreach ($coverFallbackSignal in @("bookCoverFallbackText", "BookCoverFallbackText", "loading = { BookCoverFallbackText", "error = { BookCoverFallbackText")) {
    Assert-True -Condition ($appSource.Contains($coverFallbackSignal)) -Message "Native cover rendering must include fallback signal: $coverFallbackSignal"
}
foreach ($coverFallbackTestSignal in @("coverFallbackTextUsesFirstNonBlankTitleCharacter", "coverFallbackTextUsesDefaultForBlankTitle")) {
    Assert-True -Condition ($bookCoverFallbackTestSource.Contains($coverFallbackTestSignal)) -Message "Native cover fallback must have unit coverage for: $coverFallbackTestSignal"
}
Assert-True -Condition ($appSource.Contains("WebFallbackScreen")) -Message "Compose app must retain explicit WebView fallback route."

Assert-True -Condition ($viewModelSource -match "openDeepLink") -Message "ViewModel must route native deep links to book detail or reader."
Assert-True -Condition ($viewModelSource -match "CookieManager") -Message "Native API client must reuse preserved WebView cookies when available."
Assert-True -Condition ($viewModelSource -match "NetworkConfigStore") -Message "ViewModel must load persisted native network/proxy settings."
Assert-True -Condition ($viewModelSource -match "saveProxySettings") -Message "ViewModel must save proxy settings from the native settings screen."
Assert-True -Condition ($viewModelSource -match "AuthSessionStore") -Message "ViewModel must persist native auth token state."
Assert-True -Condition ($viewModelSource -match "ReaderProgressStore") -Message "ViewModel must persist reader progress."
Assert-True -Condition ($viewModelSource -match "ReaderSettingsStore") -Message "ViewModel must persist reader settings."
Assert-True -Condition ($viewModelSource -match "SearchSettingsStore") -Message "ViewModel must persist search settings."
Assert-True -Condition ($viewModelSource -match "SearchHistoryStore") -Message "ViewModel must persist search history."
Assert-True -Condition ($viewModelSource.Contains("searchSettingsStore.load().toSearchOptions()")) -Message "Search options must initialize from persisted native settings."
Assert-True -Condition ($viewModelSource.Contains("searchHistoryStore.loadLastKeyword()")) -Message "Search keyword must initialize from persisted native history."
Assert-True -Condition ($viewModelSource.Contains("saveSearchOptions()")) -Message "Search option updates must save persisted native settings."
Assert-True -Condition ($viewModelSource.Contains("searchHistoryStore.saveKeyword")) -Message "Search execution must persist nonblank native search history."
foreach ($routeStackSignal in @("pushDistinctRoute", "replaceTopReaderRoute")) {
    Assert-True -Condition ($routeStackPolicySource.Contains($routeStackSignal)) -Message "Route stack policy must include: $routeStackSignal"
    Assert-True -Condition ($viewModelSource.Contains($routeStackSignal)) -Message "ViewModel must apply route stack policy: $routeStackSignal"
    Assert-True -Condition ($routeStackPolicyTestSource.Contains($routeStackSignal)) -Message "Route stack policy must have unit coverage for: $routeStackSignal"
}
Assert-True -Condition ($viewModelSource.Contains("routes.replaceWith(nextStack)")) -Message "ViewModel must replace route stack through a single helper for distinct navigation."
Assert-True -Condition ($routeStackPolicyTestSource.Contains("pushDistinctRouteDoesNotDuplicateCurrentTopRoute")) -Message "Route stack policy must test duplicate book-detail suppression."
Assert-True -Condition ($routeStackPolicyTestSource.Contains("replaceTopReaderRouteSkipsReloadForSameReaderChapter")) -Message "Route stack policy must test duplicate reader suppression."
Assert-True -Condition ($apiMessagesSource.Contains("fun apiFailureMessage")) -Message "Native API errors must use a shared contextual message helper."
Assert-True -Condition ($apiMessagesSource.Contains("visibleFailureLabel(label)") -and $apiMessagesSource.Contains("璇锋眰澶辫触")) -Message "Native API errors must include the API area label."
Assert-True -Condition ($viewModelSource.Contains("apiFailureMessage(label")) -Message "ViewModel toLoadResult must include the API area label in failures."
Assert-True -Condition ($apiFailureMessageTestSource.Contains("includesApiAreaLabelAndThrowableMessage")) -Message "API failure message formatting must have unit coverage."
foreach ($freshnessSignal in @("isFreshBookDetailResult", "isFreshReaderResult")) {
    Assert-True -Condition ($requestFreshnessSource.Contains($freshnessSignal)) -Message "Request freshness guard must include: $freshnessSignal"
    Assert-True -Condition ($viewModelSource.Contains($freshnessSignal)) -Message "ViewModel must apply request freshness guard: $freshnessSignal"
    Assert-True -Condition ($requestFreshnessTestSource.Contains($freshnessSignal)) -Message "Request freshness guard must have unit coverage for: $freshnessSignal"
}
Assert-True -Condition ($requestFreshnessTestSource.Contains("bookDetailResultIsFreshOnlyForCurrentBookDetailOrReaderRoute")) -Message "Book detail request freshness must have route-level coverage."
Assert-True -Condition ($requestFreshnessTestSource.Contains("readerResultIsFreshOnlyForCurrentReaderRouteAndChapter")) -Message "Reader request freshness must have route-level coverage."
foreach ($readerTextSignal in @("readerParagraphsFromContent", "Html.fromHtml", "LINE_BREAK_MARKER", "PARAGRAPH_BREAK_MARKER")) {
    Assert-True -Condition ($readerTextSource.Contains($readerTextSignal)) -Message "Reader text normalizer must include signal: $readerTextSignal"
}
foreach ($readerTextTestSignal in @("readerParagraphsDecodeHtmlEntitiesAndPreserveParagraphBreaks", "readerParagraphsIgnoreBlankMarkupButKeepPlainTextFallback", "&nbsp;", "&amp;")) {
    Assert-True -Condition ($readerTextTestSource.Contains($readerTextTestSignal)) -Message "Reader text normalizer must have unit coverage for: $readerTextTestSignal"
}

foreach ($endpoint in @("/api/search", "/api/users/me", "/api/favorites", "/api/novels/", "/api/chapters/")) {
    Assert-True -Condition ($apiSource.Contains($endpoint)) -Message "Missing native API endpoint contract: $endpoint"
}
foreach ($endpoint in @("/api/favorites/groups", "/api/favorites/status")) {
    Assert-True -Condition ($apiSource.Contains($endpoint)) -Message "Missing native first-stage API endpoint contract: $endpoint"
}
foreach ($model in @("NovelCard", "Chapter", "ReaderContent", "FavoriteGroup", "FavoriteStatus", "UserProfile", "ReaderProgress")) {
    Assert-True -Condition ($modelSource.Contains("data class $model") -or $modelSource.Contains("sealed interface $model")) -Message "Missing native model: $model"
}
Assert-True -Condition ($apiSource -match "cookieProvider") -Message "NovalPieApi must accept a cookie provider for logged API calls."
Assert-True -Condition ($apiSource -match "proxyProvider") -Message "NovalPieApi must accept a proxy provider for blocked direct connections."
foreach ($chapterAlias in @("chapter_name", "display_order", "words", "created_at")) {
    Assert-True -Condition ($apiSource.Contains($chapterAlias)) -Message "NovalPieApi must normalize chapter alias: $chapterAlias"
    Assert-True -Condition ($apiTestSource.Contains($chapterAlias)) -Message "NovalPieApiTest must cover chapter alias: $chapterAlias"
}
Assert-True -Condition ($apiTestSource.Contains("chaptersNormalizeWebsiteFieldAliases")) -Message "NovalPieApi must have request-level coverage for chapter field aliases."
Assert-True -Condition ($apiSource.Contains("IndexedChapter")) -Message "NovalPieApi must preserve original chapter index for stable sorting."
Assert-True -Condition ($apiSource.Contains(".sortedWith(compareBy<IndexedChapter>")) -Message "NovalPieApi must sort chapters by normalized website order."
Assert-True -Condition ($apiTestSource.Contains("chaptersAreSortedByWebsiteDisplayOrder")) -Message "NovalPieApi must have request-level coverage for chapter display-order sorting."
foreach ($bookAlias in @("cover_path", "synopsis", "normalizeTags", "objectStringOrNull")) {
    Assert-True -Condition ($apiSource.Contains($bookAlias)) -Message "NovalPieApi must normalize book alias/helper: $bookAlias"
    Assert-True -Condition ($apiTestSource.Contains($bookAlias) -or $bookAlias -in @("normalizeTags", "objectStringOrNull")) -Message "NovalPieApiTest must cover book alias/helper: $bookAlias"
}
Assert-True -Condition ($apiTestSource.Contains("bookDetailUnwrapsNestedNovelAndNormalizesWebsiteFieldAliases")) -Message "NovalPieApi must have request-level coverage for nested book detail field aliases."
foreach ($searchArrayAlias in @("results", "novels", "list", "records")) {
    Assert-True -Condition ($apiSource.Contains($searchArrayAlias)) -Message "NovalPieApi must extract search/list array alias: $searchArrayAlias"
}
Assert-True -Condition ($apiTestSource.Contains("searchNormalizesResultArrayAliasesAndSendsQueryParameters")) -Message "NovalPieApi must have request-level coverage for search result aliases and query parameters."
foreach ($favoritesArrayAlias in @("favorites", "books")) {
    Assert-True -Condition ($apiSource.Contains($favoritesArrayAlias)) -Message "NovalPieApi must extract bookshelf/favorites array alias: $favoritesArrayAlias"
    Assert-True -Condition ($apiTestSource.Contains($favoritesArrayAlias)) -Message "NovalPieApiTest must cover bookshelf/favorites array alias: $favoritesArrayAlias"
}
Assert-True -Condition ($apiTestSource.Contains("favoritesNormalizesFavoriteArrayAliasesAndSendsBookshelfParameters")) -Message "NovalPieApi must have request-level coverage for favorites aliases and bookshelf query parameters."
foreach ($favoriteGroupAlias in @("favorite_groups", "group_name", "book_count")) {
    Assert-True -Condition ($apiSource.Contains($favoriteGroupAlias)) -Message "NovalPieApi must normalize favorite group alias: $favoriteGroupAlias"
    Assert-True -Condition ($apiTestSource.Contains($favoriteGroupAlias)) -Message "NovalPieApiTest must cover favorite group alias: $favoriteGroupAlias"
}
Assert-True -Condition ($apiTestSource.Contains("favoriteGroupsNormalizesWebsiteGroupAliasesAndSendsPreviewParameters")) -Message "NovalPieApi must have request-level coverage for favorite group aliases and preview query parameters."
foreach ($favoriteStatusAlias in @("isFavorite", "status_text", "favorite_group")) {
    Assert-True -Condition ($apiSource.Contains($favoriteStatusAlias)) -Message "NovalPieApi must normalize favorite status alias: $favoriteStatusAlias"
    Assert-True -Condition ($apiTestSource.Contains($favoriteStatusAlias)) -Message "NovalPieApiTest must cover favorite status alias: $favoriteStatusAlias"
}
Assert-True -Condition ($apiTestSource.Contains("favoriteStatusNormalizesWebsiteStatusAliasesAndSendsParameters")) -Message "NovalPieApi must have request-level coverage for favorite status aliases and query parameters."
foreach ($userAlias in @("uid", "nickname", "user_role")) {
    Assert-True -Condition ($apiSource.Contains($userAlias)) -Message "NovalPieApi must normalize current-user alias: $userAlias"
    Assert-True -Condition ($apiTestSource.Contains($userAlias)) -Message "NovalPieApiTest must cover current-user alias: $userAlias"
}
Assert-True -Condition ($apiTestSource.Contains("currentUserNormalizesWebsiteProfileAliases")) -Message "NovalPieApi must have request-level coverage for current-user profile aliases."
foreach ($readerContentAlias in @("body_html", "bodyHtml", "chapter_name")) {
    Assert-True -Condition ($apiSource.Contains($readerContentAlias)) -Message "NovalPieApi must normalize reader content alias: $readerContentAlias"
    Assert-True -Condition ($apiTestSource.Contains($readerContentAlias)) -Message "NovalPieApiTest must cover reader content alias: $readerContentAlias"
}
Assert-True -Condition ($apiTestSource.Contains("chapterContentNormalizesWebsiteBodyAliasesAndSendsReaderParameters")) -Message "NovalPieApi must have request-level coverage for reader content aliases and query parameters."
Assert-True -Condition ($networkConfigSource -match "Proxy.Type.HTTP") -Message "Network config must produce an HTTP proxy when proxy settings are enabled."
Assert-True -Condition ($networkConfigSource -match "127.0.0.1") -Message "Network config must provide a MuMu/local-proxy default host."
Assert-True -Condition ($networkConfigSource -match "7890") -Message "Network config must provide a MuMu/local-proxy default port."
Assert-True -Condition ($authStoreSource -match "SharedPreferences") -Message "Auth session store must use Android SharedPreferences."
Assert-True -Condition ($readerProgressStoreSource -match "SharedPreferences") -Message "Reader progress store must use Android SharedPreferences."
Assert-True -Condition ($readerProgressStoreSource -match "fun\s+load\(bookId:\s*Long\)") -Message "Reader progress store must support per-book progress reads."
Assert-True -Condition ($readerProgressStoreSource -match "fun\s+loadRecent\(limit:\s*Int") -Message "Reader progress store must support native recent-reading lists."
Assert-True -Condition ($readerProgressStoreSource.Contains("bookKey(bookId")) -Message "Reader progress store must persist progress under per-book keys."
Assert-True -Condition ($readerProgressStoreSource.Contains("recent_book_ids")) -Message "Reader progress store must persist recent-reading order."
Assert-True -Condition ($readerProgressStoreTestSource.Contains("loadsProgressForRequestedBookWithoutLosingOtherBooks")) -Message "Reader progress store must test per-book progress isolation."
Assert-True -Condition ($readerProgressStoreTestSource.Contains("loadsRecentProgressesInMostRecentOrderWithLimit")) -Message "Reader progress store must test recent-reading ordering."
Assert-True -Condition ($readerSettingsStoreSource -match "SharedPreferences") -Message "Reader settings store must use Android SharedPreferences."
Assert-True -Condition ($searchHistoryStoreSource -match "SharedPreferences") -Message "Search history store must use Android SharedPreferences."
Assert-True -Condition ($searchHistoryStoreSource.Contains("MAX_HISTORY")) -Message "Search history store must bound the history size."
Assert-True -Condition ($searchHistoryStoreTestSource.Contains("savesRecentKeywordsMostRecentFirst")) -Message "Search history store must test most-recent ordering."
Assert-True -Condition ($searchHistoryStoreTestSource.Contains("savingExistingKeywordMovesItToFront")) -Message "Search history store must test duplicate promotion."
Assert-True -Condition ($searchHistoryStoreTestSource.Contains("ignoresBlankKeywordsAndLimitsHistorySize")) -Message "Search history store must test blank filtering and limit behavior."
Assert-True -Condition ($searchHistoryStoreTestSource.Contains("loadsLastKeywordFromMostRecentHistoryEntry")) -Message "Search history store must test last-keyword loading."
Assert-True -Condition ($searchSettingsStoreSource -match "SharedPreferences") -Message "Search settings store must use Android SharedPreferences."
foreach ($searchSettingKey in @("sort_by", "sort_order", "scope", "match_type", "adult_filter")) {
    Assert-True -Condition ($searchSettingsStoreSource.Contains($searchSettingKey)) -Message "Search settings store must persist key: $searchSettingKey"
}
Assert-True -Condition ($searchSettingsStoreTestSource.Contains("savesAndLoadsSearchSettings")) -Message "Search settings store must test save/load behavior."
Assert-True -Condition ($searchSettingsStoreTestSource.Contains("returnsDefaultsWhenNothingWasSaved")) -Message "Search settings store must test default settings."

$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "app\src\main") -Recurse -File -Include *.kt,*.java,*.xml,*.gradle
$badCapacitor = $sourceFiles | Select-String -Pattern "capacitor|getcapacitor|cordova" -SimpleMatch
Assert-True -Condition ($null -eq $badCapacitor) -Message "Native source must not contain Capacitor/Cordova references."

$webViewHits = $sourceFiles | Select-String -Pattern "android.webkit.WebView|WebView\(" | Select-Object -ExpandProperty Path -Unique
foreach ($hit in $webViewHits) {
    Assert-True -Condition ($hit.EndsWith("WebFallbackScreen.kt")) -Message "WebView is allowed only in WebFallbackScreen.kt, found in $hit"
}

if ($RequireApk) {
    $apk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
    Assert-True -Condition (Test-Path -LiteralPath $apk) -Message "Missing signed release APK: $apk"
}

Write-Host "Native project verification passed: $ProjectRoot"
