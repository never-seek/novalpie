package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.NovalPieApiException
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserCheckinRecord
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserContentActivityFeed
import com.novalpie.nativeapp.model.UserInventory
import com.novalpie.nativeapp.model.UserInventoryItem
import com.novalpie.nativeapp.model.UserBadge
import com.novalpie.nativeapp.model.UserQuizRewardStatus
import java.util.Calendar
import java.util.TimeZone

internal fun isAdminProfile(profile: UserProfile?): Boolean = profile?.role == "admin"

internal fun profileActivityFilterLabel(filter: ProfileActivityFilter): String = when (filter) {
    ProfileActivityFilter.All -> "全部"
    ProfileActivityFilter.Posts -> "帖子"
    ProfileActivityFilter.Comments -> "评论"
    ProfileActivityFilter.BookReviews -> "书评"
    ProfileActivityFilter.ChapterReviews -> "章评"
}

/** Keep the source ActivityTab categories local so changing a chip never refetches the feed. */
internal fun filterProfileActivities(
    activities: List<com.novalpie.nativeapp.model.UserActivity>,
    filter: ProfileActivityFilter,
): List<com.novalpie.nativeapp.model.UserActivity> = when (filter) {
    ProfileActivityFilter.All -> activities
    ProfileActivityFilter.Posts -> activities.filter { it.type == "post" }
    ProfileActivityFilter.Comments -> activities.filter { it.type == "post_comment" }
    ProfileActivityFilter.BookReviews -> activities.filter { it.type == "novel_comment" }
    ProfileActivityFilter.ChapterReviews -> activities.filter { it.type == "chapter_comment" }
}

/** Unknown remote profile fields must never be rendered as a factual zero. */
internal fun profileMetricValueLabel(value: Long?): String = value?.toString() ?: "—"

internal fun profileWebsiteFacts(
    profile: UserProfile,
    checkinStats: UserCheckinStats?
): List<String> = listOf(
    "积分 ${profile.points ?: 0}",
    "作品 ${profile.stats["novels"] ?: 0}",
    "评论 ${profile.stats["comments"] ?: 0}",
    "连续签到 ${checkinStats?.currentStreak ?: 0} 天"
)

internal fun profileAccountStatusLabels(profile: UserProfile): List<String> = buildList {
    if (profile.deleted == true) {
        add("账号已删除")
    } else if (profile.isBanned == true) {
        add(profile.banExpiresAt?.takeIf(String::isNotBlank)?.let { "账号封禁至 ${profileShortDate(it)}" } ?: "账号封禁")
        profile.banReason?.takeIf(String::isNotBlank)?.let { add("封禁原因 $it") }
    } else if (profile.isBanned == false) {
        add("账号正常")
    }
    when (profile.isAdult) {
        true -> add("成年已验证")
        false -> add("成年未验证")
        null -> Unit
    }
    if (!profile.email.isNullOrBlank()) add("邮箱已绑定")
    profile.createdAt?.takeIf(String::isNotBlank)?.let { add("注册 ${profileShortDate(it)}") }
    profile.showCheckin?.let { add(if (it) "签到公开" else "签到隐藏") }
    profile.autoCheckin?.let { add(if (it) "自动签到已开" else "自动签到未开") }
}

/** A safe empty-state fallback only when the source profile explicitly reports both counters. */
internal fun profileHasNoPublicActivities(profile: UserProfile?): Boolean =
    profile != null &&
        profile.stats.containsKey("posts") &&
        profile.stats.containsKey("comments") &&
        (profile.stats["posts"] ?: 0L) == 0L &&
        (profile.stats["comments"] ?: 0L) == 0L

/** Source `/users/{id}` can omit stats; fill only absent keys from explicit feed totals. */
internal fun profileWithContentActivityCounts(
    profile: UserProfile?,
    feed: UserContentActivityFeed?
): UserProfile? {
    if (profile == null || feed == null) return profile
    val mergedStats = profile.stats.toMutableMap()
    if ("posts" !in mergedStats) feed.postCount?.let { mergedStats["posts"] = it }
    if ("comments" !in mergedStats) feed.commentCount?.let { mergedStats["comments"] = it }
    return profile.copy(stats = mergedStats)
}

/** The public profile payload often omits its novels counter; the versioned collection is exact. */
internal fun profileWithPublicCollectionCounts(
    profile: UserProfile?,
    feed: UserContentActivityFeed?,
    novels: List<NovelCard>?,
): UserProfile? {
    val withActivityCounts = profileWithContentActivityCounts(profile, feed) ?: return null
    if (novels == null || "novels" in withActivityCounts.stats) return withActivityCounts
    return withActivityCounts.copy(
        stats = withActivityCounts.stats + ("novels" to novels.size.toLong()),
    )
}

/** Keep the public profile's visible timeline and its aggregate counters from the same feed. */
internal data class PublicProfileLoadPresentation(
    val profile: UserProfile?,
    val activities: List<UserActivity>?,
)

/**
 * A public route already has a stable user id before its hero response arrives.  The source
 * loads timeline, books, and check-in panels independently, so a failed hero must not make
 * successful secondary panels unreachable behind the error card.
 */
internal fun shouldRenderPublicProfilePanels(state: UserProfileDetailState): Boolean = state.userId > 0L

internal fun publicProfileLoadPresentation(
    profile: UserProfile?,
    feed: UserContentActivityFeed?,
    novels: List<NovelCard>?,
): PublicProfileLoadPresentation = PublicProfileLoadPresentation(
    profile = profileWithPublicCollectionCounts(profile, feed, novels),
    activities = feed?.activities,
)

/**
 * The signed-in profile must not wait for its activity, uploads, backpack, shop, and reward
 * requests.  Those requests arrive independently on the website, so the native hero is promoted
 * as soon as `/users/me` succeeds and is enriched again only when a related section arrives.
 */
internal fun currentUserProfileWithLoadedHero(
    state: ProfileState,
    result: Result<UserProfile>,
    tokenProfile: UserProfile?,
): ProfileState {
    val resolved = resolveUserLoadResult(result, tokenProfile)
    val profile = (resolved as? LoadResult.Success<UserProfile>)?.value
        ?: return state.copy(profile = resolved)
    return state
        .copy(profile = LoadResult.Success(profile))
        .withEnrichedCurrentUserHero()
        .copy(
            nameDraft = profile.name,
            bioDraft = profile.bio.orEmpty(),
            showCheckin = profile.showCheckin ?: true,
            autoCheckin = profile.autoCheckin ?: false,
        )
}

/** A late activity response changes only its own panel and optionally fills omitted profile counts. */
internal fun currentUserProfileWithLoadedActivities(
    state: ProfileState,
    result: Result<UserContentActivityFeed>,
): ProfileState = result.fold(
    onSuccess = { feed ->
        state.copy(
            activities = LoadResult.Success(feed.activities),
            activityFeed = feed,
        ).withEnrichedCurrentUserHero()
    },
    onFailure = { failure ->
        state.copy(
            activities = if (
                profileHasNoPublicActivities((state.profile as? LoadResult.Success<UserProfile>)?.value) ||
                sourceActivitiesEndpointUnavailable(failure)
            ) {
                LoadResult.Success(emptyList())
            } else {
                LoadResult.Error(apiFailureMessage("个人动态", failure))
            },
        )
    },
)

/** A late uploads response is similarly isolated from the profile hero and other panels. */
internal fun currentUserProfileWithLoadedBooks(
    state: ProfileState,
    result: Result<List<NovelCard>>,
): ProfileState = result.fold(
    onSuccess = { books ->
        state.copy(
            books = LoadResult.Success(books),
            uploadedBooks = books,
        ).withEnrichedCurrentUserHero()
    },
    onFailure = { failure ->
        state.copy(
            books = if (sourceBooksEndpointUnavailable(failure)) {
                LoadResult.Success(emptyList())
            } else {
                LoadResult.Error(apiFailureMessage("上传书籍", failure))
            },
        )
    },
)

/** Inventory only augments a displayed profile with its equipped frame/badges after it succeeds. */
internal fun currentUserProfileWithLoadedInventory(
    state: ProfileState,
    result: Result<UserInventory>,
): ProfileState = state.copy(
    inventory = result.fold(
        onSuccess = { LoadResult.Success(it) },
        onFailure = { LoadResult.Error(apiFailureMessage("背包", it)) },
    ),
).withEnrichedCurrentUserHero()

internal fun currentUserProfileWithLoadedCheckinStats(
    state: ProfileState,
    result: Result<UserCheckinStats>,
    today: String,
): ProfileState = result.fold(
    onSuccess = { source ->
        val records = (state.checkinRecords as? LoadResult.Success<List<UserCheckinRecord>>)?.value.orEmpty()
        state.copy(checkinStats = LoadResult.Success(reconcileCheckinStats(source, records, today)))
    },
    onFailure = { failure ->
        val records = (state.checkinRecords as? LoadResult.Success<List<UserCheckinRecord>>)?.value.orEmpty()
        state.copy(
            checkinStats = if (records.isNotEmpty()) {
                LoadResult.Success(reconcileCheckinStats(UserCheckinStats(), records, today))
            } else {
                LoadResult.Error(apiFailureMessage("签到统计", failure))
            },
        )
    },
)

internal fun currentUserProfileWithLoadedCheckinRecords(
    state: ProfileState,
    result: Result<List<UserCheckinRecord>>,
    today: String,
): ProfileState = result.fold(
    onSuccess = { records ->
        val source = (state.checkinStats as? LoadResult.Success<UserCheckinStats>)?.value
            ?: UserCheckinStats()
        state.copy(
            checkinRecords = LoadResult.Success(records),
            checkinStats = LoadResult.Success(reconcileCheckinStats(source, records, today)),
        )
    },
    onFailure = { failure ->
        state.copy(checkinRecords = LoadResult.Error(apiFailureMessage("签到记录", failure)))
    },
)

private fun ProfileState.withEnrichedCurrentUserHero(): ProfileState {
    val profile = (profile as? LoadResult.Success<UserProfile>)?.value ?: return this
    val inventory = (inventory as? LoadResult.Success<UserInventory>)?.value
    val enriched = profileWithEquippedCosmetics(
        profile = profileWithPublicCollectionCounts(profile, activityFeed, uploadedBooks),
        inventory = inventory,
    ) ?: profile
    return copy(profile = LoadResult.Success(enriched))
}

/** The hero can render as soon as its own source request returns. */
internal fun userProfileDetailWithLoadedProfile(
    state: UserProfileDetailState,
    result: Result<UserProfile>,
): UserProfileDetailState = state.copy(
    profile = result.fold(
        onSuccess = { profile ->
            profileWithPublicCollectionCounts(
                profile = profile,
                feed = state.activityFeed,
                novels = state.publicBooks,
            )?.let { LoadResult.Success(it) } ?: LoadResult.Success(profile)
        },
        onFailure = { failure ->
            LoadResult.Error(apiFailureMessage("用户资料", failure))
        },
    ),
)

/** Independent public-profile endpoints may be retried without blanking the rendered hero. */
internal enum class PublicUserProfilePanel {
    Activities,
    Books,
    CheckinStats,
    CheckinRecords,
    CheckinSettings,
}

internal fun userProfileDetailForPanelRetry(
    state: UserProfileDetailState,
    panel: PublicUserProfilePanel,
): UserProfileDetailState = when (panel) {
    PublicUserProfilePanel.Activities -> state.copy(activities = LoadResult.Loading)
    PublicUserProfilePanel.Books -> state.copy(books = LoadResult.Loading)
    PublicUserProfilePanel.CheckinStats -> state.copy(checkinStats = LoadResult.Loading)
    PublicUserProfilePanel.CheckinRecords -> state.copy(checkinRecords = LoadResult.Loading)
    PublicUserProfilePanel.CheckinSettings -> state.copy(checkinSettings = LoadResult.Loading)
}

/** Retrying the timeline must not replace a successfully rendered public profile hero. */
internal fun userProfileDetailForActivitiesRetry(
    state: UserProfileDetailState,
): UserProfileDetailState = userProfileDetailForPanelRetry(
    state = state,
    panel = PublicUserProfilePanel.Activities,
)

/**
 * Activity data is independent of the profile hero.  When it arrives later, refresh the profile
 * counters as a small secondary update rather than keeping the entire page behind a spinner.
 */
internal fun userProfileDetailWithLoadedActivities(
    state: UserProfileDetailState,
    result: Result<UserContentActivityFeed>,
): UserProfileDetailState = result.fold(
    onSuccess = { feed ->
        val profile = (state.profile as? LoadResult.Success<UserProfile>)?.value
        state.copy(
            profile = profileWithPublicCollectionCounts(profile, feed, state.publicBooks)
                ?.let { LoadResult.Success(it) }
                ?: state.profile,
            activities = LoadResult.Success(feed.activities),
            activityFeed = feed,
        )
    },
    onFailure = { failure ->
        state.copy(
            activities = if (
                profileHasNoPublicActivities((state.profile as? LoadResult.Success<UserProfile>)?.value) ||
                sourceActivitiesEndpointUnavailable(failure)
            ) {
                LoadResult.Success(emptyList())
            } else {
                LoadResult.Error(apiFailureMessage("用户动态", failure))
            },
        )
    },
)

/** The source collection route may be a documented placeholder; otherwise expose an explicit retry state. */
internal fun userProfileDetailWithLoadedBooks(
    state: UserProfileDetailState,
    result: Result<List<NovelCard>>,
): UserProfileDetailState = result.fold(
    onSuccess = { books ->
        val profile = (state.profile as? LoadResult.Success<UserProfile>)?.value
        state.copy(
            profile = profileWithPublicCollectionCounts(profile, state.activityFeed, books)
                ?.let { LoadResult.Success(it) }
                ?: state.profile,
            books = LoadResult.Success(books),
            publicBooks = books,
        )
    },
    onFailure = { failure ->
        state.copy(
            books = if (sourceBooksEndpointUnavailable(failure)) {
                LoadResult.Success(emptyList())
            } else {
                LoadResult.Error(apiFailureMessage("用户作品", failure))
            },
        )
    },
)

private fun <T> Result<T>.toPublicProfileLoadResult(): LoadResult<T> = fold(
    onSuccess = { LoadResult.Success(it) },
    onFailure = { LoadResult.Error(apiFailureMessage("用户资料", it)) },
)

/** The profile response is authoritative; the equipped inventory frame fills only a missing URL. */
internal fun profileWithEquippedAvatarFrame(
    profile: UserProfile?,
    inventory: UserInventory?
): UserProfile? {
    if (profile == null || !profile.avatarFrameUrl.isNullOrBlank()) return profile
    val frameUrl = inventory
        ?.items
        ?.firstOrNull { item -> item.equipped && item.isAvatarFrame() }
        ?.imageUrl
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: return profile
    return profile.copy(avatarFrameUrl = frameUrl)
}

/**
 * `/users/me` can omit cosmetic records even though the inventory reports which Badge is equipped.
 * Merge only equipped Badge records and retain profile payload fields whenever the profile already
 * contains them.
 */
internal fun profileWithEquippedBadges(
    profile: UserProfile?,
    inventory: UserInventory?,
): UserProfile? {
    if (profile == null) return null
    val equipped = inventory
        ?.items
        ?.filter { item -> item.equipped && item.isBadge() }
        ?.map { item ->
            UserBadge(
                id = item.itemId.takeIf { it > 0 } ?: item.id.takeIf { it > 0 },
                name = item.name,
                description = item.description,
                imageUrl = item.imageUrl,
                badgeHtml = item.badgeHtml,
                badgeCss = item.badgeCss,
            )
        }
        .orEmpty()
    if (equipped.isEmpty()) return profile

    val merged = linkedMapOf<String, UserBadge>()
    profile.badges.forEach { badge ->
        merged[badge.identityKey()] = badge
    }
    equipped.forEach { equippedBadge ->
        val key = equippedBadge.identityKey()
        val existing = merged[key]
        merged[key] = if (existing == null) {
            equippedBadge
        } else {
            existing.copy(
                description = existing.description ?: equippedBadge.description,
                imageUrl = existing.imageUrl ?: equippedBadge.imageUrl,
                badgeHtml = existing.badgeHtml ?: equippedBadge.badgeHtml,
                badgeCss = existing.badgeCss ?: equippedBadge.badgeCss,
            )
        }
    }
    return profile.copy(badges = merged.values.toList())
}

/** Applies both equipped cosmetic fallbacks without overriding a populated profile payload. */
internal fun profileWithEquippedCosmetics(
    profile: UserProfile?,
    inventory: UserInventory?,
): UserProfile? = profileWithEquippedBadges(profileWithEquippedAvatarFrame(profile, inventory), inventory)

private fun UserInventoryItem.isAvatarFrame(): Boolean {
    val normalizedType = type.orEmpty().trim().lowercase().replace('-', '_').replace(' ', '_')
    val normalizedSlot = slot.orEmpty().trim().lowercase().replace('-', '_').replace(' ', '_')
    return normalizedType in setOf("frame", "avatar_frame", "avatarframe") ||
        normalizedSlot in setOf("frame", "avatar_frame", "avatarframe")
}

private fun UserInventoryItem.isBadge(): Boolean {
    val normalizedType = type.orEmpty().trim().lowercase().replace('-', '_').replace(' ', '_')
    val normalizedSlot = slot.orEmpty().trim().lowercase().replace('-', '_').replace(' ', '_')
    return normalizedType in setOf("badge", "badges", "user_badge") ||
        normalizedSlot in setOf("badge", "badges", "user_badge")
}

private fun UserBadge.identityKey(): String = id?.let { "id:$it" } ?: "name:$name"

/** The source's public activities and books collection routes can be Laravel placeholders. */
internal fun sourceActivitiesEndpointUnavailable(failure: Throwable?): Boolean =
    sourceProfileCollectionEndpointUnavailable(failure, "activities")

/** See [sourceActivitiesEndpointUnavailable]; this maps only the matching books collection path. */
internal fun sourceBooksEndpointUnavailable(failure: Throwable?): Boolean =
    sourceProfileCollectionEndpointUnavailable(failure, "novels")

private fun sourceProfileCollectionEndpointUnavailable(failure: Throwable?, collection: String): Boolean {
    if (collection !in setOf("activities", "novels")) return false
    val apiFailure = generateSequence(failure) { it.cause }
        .filterIsInstance<NovalPieApiException>()
        .firstOrNull()
        ?: return false
    return apiFailure.statusCode in setOf(404, 501) &&
        apiFailure.path.matches(Regex("^/api/users/(?:\\d+|me)/${collection}$")) &&
        apiFailure.serverMessage.orEmpty().contains("not implemented", ignoreCase = true)
}

/** Stable product copy for the source's read-only reward status endpoint. */
internal fun profileQuizRewardLabel(status: UserQuizRewardStatus): String = when {
    status.claimed == true -> "奖励已领取"
    status.eligible == true -> "奖励问答可领取"
    status.eligible == false -> "暂不满足奖励条件"
    else -> "奖励状态待确认"
}

/**
 * Some source deployments return a zeroed check-in summary while still returning valid records.
 * Keep server values when present, but fill only missing zero fields from those records so the
 * account surface never claims zero days beside a visible check-in history.
 */
internal fun reconcileCheckinStats(
    source: UserCheckinStats,
    records: List<UserCheckinRecord>,
    today: String
): UserCheckinStats {
    val days = records.mapNotNull { checkinEpochDay(it.date) }.distinct().sorted()
    if (days.isEmpty()) return source

    var longest = 1
    var run = 1
    for (index in 1 until days.size) {
        if (days[index] == days[index - 1] + 1) {
            run += 1
            longest = maxOf(longest, run)
        } else {
            run = 1
        }
    }

    val todayDay = checkinEpochDay(today)
    var current = 0
    var expected = todayDay
    while (expected != null && expected in days) {
        current += 1
        expected = expected?.minus(1L)
    }

    val recordPoints = records.sumOf(UserCheckinRecord::points)
    return source.copy(
        totalDays = source.totalDays.takeIf { it > 0 } ?: days.size,
        totalPoints = source.totalPoints.takeIf { it > 0 } ?: recordPoints,
        maxStreak = maxOf(source.maxStreak, longest),
        currentStreak = source.currentStreak.takeIf { it > 0 } ?: current
    )
}

private fun checkinEpochDay(value: String): Long? {
    val parts = value.split('-').map(String::toIntOrNull)
    if (parts.size != 3 || parts.any { it == null }) return null
    return runCatching {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(parts[0]!!, parts[1]!! - 1, parts[2]!!, 0, 0, 0)
        }.timeInMillis / 86_400_000L
    }.getOrNull()
}

private fun profileShortDate(value: String): String =
    value.takeIf { it.length >= 10 && it[4] == '-' && it[7] == '-' }?.take(10) ?: value

internal data class ProfileOverview(
    val title: String,
    val subtitle: String,
    val accountName: String,
    val syncLabel: String,
    val roleLabel: String,
    val stats: List<String>
)

internal fun profileOverview(
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    readerProgress: ReaderProgress?,
    readerOptions: ReaderUiOptions,
    proxyEnabled: Boolean
): ProfileOverview {
    val header = productHeader(ProductSurface.Profile)
    val profile = (user as? LoadResult.Success)?.value
    return ProfileOverview(
        title = header.title,
        subtitle = header.subtitle,
        accountName = profile?.name ?: if (hasAuthToken) "账号已同步" else "未登录",
        syncLabel = if (hasAuthToken) "已同步" else "未同步",
        roleLabel = profileRoleLabel(profile?.role, hasAuthToken),
        stats = listOf(
            readerProgress?.let { "阅读 章节 ${it.chapterId}" } ?: "阅读 无进度",
            "字号 ${readerOptions.fontSizeSp}sp",
            "主题 ${readerOptions.theme.themeLabel()}",
            if (proxyEnabled) "连接 已启用" else "连接 未启用"
        )
    )
}

internal fun profileSectionTitles(): List<String> =
    listOf("账号", "阅读偏好", "连接设置", "网页入口")

internal fun profileAccountActions(hasAuthToken: Boolean): List<String> =
    if (hasAuthToken) {
        listOf("同步账号", "网页登录", "退出同步")
    } else {
        listOf("同步账号", "网页登录")
    }

internal fun profileWebActions(): List<String> =
    listOf("打开网站", "网页搜索")

private fun profileRoleLabel(role: String?, hasAuthToken: Boolean): String =
    when {
        role == "admin" -> "管理员"
        !role.isNullOrBlank() -> "普通用户"
        hasAuthToken -> "身份待同步"
        else -> "普通用户"
    }
